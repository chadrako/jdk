/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifdef COMPILER2

#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "compiler/compilerDefinitions.inline.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/hotCodeGrouper.hpp"
#include "runtime/hotCodeSampler.hpp"
#include "runtime/java.hpp"
#include "runtime/javaThread.inline.hpp"

// Initalize static variables
bool      HotCodeGrouper::_is_initialized = false;
int       HotCodeGrouper::_new_c2_nmethods_count = 0;
int       HotCodeGrouper::_total_c2_nmethods_count = 0;

HotCodeGrouper::HotCodeGrouper() : JavaThread(thread_entry) {}

void HotCodeGrouper::initialize() {
  EXCEPTION_MARK;

  assert(HotCodeHeap, "HotCodeGrouper requires HotCodeHeap enabled");
  assert(CompilerConfig::is_c2_enabled(), "HotCodeGrouper requires C2 enabled");
  assert(NMethodRelocation, "HotCodeGrouper requires NMethodRelocation enabled");
  assert(HotCodeHeapSize > 0, "HotCodeHeapSize must be non-zero to use HotCodeGrouper");
  assert(CodeCache::get_code_heap(CodeBlobType::MethodHot) != nullptr, "Hot code heap not found");

  Handle thread_oop = JavaThread::create_system_thread_object("HotCodeGrouperThread", CHECK);
  HotCodeGrouper* thread = new HotCodeGrouper();
  JavaThread::vm_exit_on_osthread_failure(thread);
  JavaThread::start_internal_daemon(THREAD, thread, thread_oop, NormPriority);

  _is_initialized = true;
}

bool HotCodeGrouper::is_nmethod_count_steady() {
  MutexLocker ml_CodeCache_lock(CodeCache_lock, Mutex::_no_safepoint_check_flag);

  if (_total_c2_nmethods_count <= 0) {
    log_trace(hotcodegrouper)("C2 nmethod count not steady. Total C2 nmethods %d <= 0", _total_c2_nmethods_count);
    return false;
  }

  const double ratio_new = (double)_new_c2_nmethods_count / _total_c2_nmethods_count;
  bool is_steady_nmethod_count = ratio_new < HotCodeSteadyThreshold;

  log_info(hotcodegrouper)("C2 nmethod count %s", is_steady_nmethod_count ? "steady" : "not steady");
  log_trace(hotcodegrouper)("\t- New: %d. Total: %d. Ratio: %f. Threshold: %f", _new_c2_nmethods_count, _total_c2_nmethods_count, ratio_new, HotCodeSteadyThreshold);

  _new_c2_nmethods_count = 0;

  return is_steady_nmethod_count;
}

void HotCodeGrouper::thread_entry(JavaThread* thread, TRAPS) {
  // Initial sleep to allow JVM to warm up
  thread->sleep(HotCodeStartupDelaySeconds * 1000);

  while (true) {
    ResourceMark rm;

    // Sample application and group hot nmethods if nmethod count is steady
    if (is_nmethod_count_steady()) {
      ThreadSampler sampler;
      sampler.do_sampling(thread);
      do_grouping(sampler);
    }

    thread->sleep(HotCodeIntervalSeconds * 1000);
  }
}

void HotCodeGrouper::do_grouping(ThreadSampler& sampler) {
  while (sampler.has_candidates()) {

    double ratio_from_hot = sampler.get_hot_sample_ratio();
    log_trace(hotcodegrouper)("Ratio of samples from hot code heap: %f", ratio_from_hot);
    if (ratio_from_hot > HotCodeSampleRatio) {
      log_info(hotcodegrouper)("Ratio of samples from hot nmethods (%f) over threshold (%f). Done grouping", ratio_from_hot, HotCodeSampleRatio);
      break;
    }

    nmethod* candidate = sampler.get_candidate();

    MutexLocker ml_Compile_lock(Compile_lock);
    MutexLocker ml_CompiledIC_lock(CompiledIC_lock, Mutex::_no_safepoint_check_flag);
    MutexLocker ml_CodeCache_lock(CodeCache_lock, Mutex::_no_safepoint_check_flag);

    do_relocation(sampler, candidate, HotCodeCalleeLevel);
  }
}

void HotCodeGrouper::do_relocation(ThreadSampler& sampler, void* candidate, int callee_level) {
  if (candidate == nullptr) {
    return;
  }

  // Verify that address still points to CodeBlob
  CodeBlob* blob = CodeCache::find_blob(candidate);
  if (blob == nullptr) {
    return;
  }

  // Verify that blob is nmethod
  nmethod* nm = blob->as_nmethod_or_null();
  if (nm == nullptr) {
    return;
  }

  // The candidate may have been recompiled or already relocated.
  // Retrieve the latest nmethod from the Method
  nm = nm->method()->code();

  // Verify the nmethod is stil valid for relocation
  if (nm == nullptr || !nm->is_in_use() || !nm->is_compiled_by_c2()) {
    return;
  }

  // Verify code heap has space
  if (CodeCache::get_code_heap(CodeBlobType::MethodHot)->unallocated_capacity() < (size_t)nm->size()) {
    log_info(hotcodegrouper)("Not enough space in HotCodeHeap (%zd bytes) to relocate nm (%d bytes). Bailing out",
      CodeCache::get_code_heap(CodeBlobType::MethodHot)->unallocated_capacity(), nm->size());
    return;
  }

  // Perform relocation
  if (CodeCache::get_code_blob_type(nm) != CodeBlobType::MethodHot) {
    CompiledICLocker ic_locker(nm);
    if (nm->relocate(CodeBlobType::MethodHot) != nullptr) {
      sampler.update_sample_count(nm);
    }
  }

  if (callee_level > 0) {
    // Loop over relocations to relocate callees
    RelocIterator relocIter(nm);
    while (relocIter.next()) {
      // Check is a call
      Relocation* reloc = relocIter.reloc();
      if(!reloc->is_call()) {
        continue;
      }

      // Find the call destination address
      address dest = ((CallRelocation*) reloc)->destination();

      // Recursively relocate callees
      do_relocation(sampler, dest, callee_level - 1);
    }
  }
}

void HotCodeGrouper::unregister_nmethod(nmethod* nm) {
  assert_lock_strong(CodeCache_lock);
  if (!_is_initialized) {
    return;
  }

  if (!nm->is_compiled_by_c2()) {
    return;
  }

  if (CodeCache::get_code_blob_type(nm) == CodeBlobType::MethodHot) {
    // Nmethods in the hot code heap do not count towards total C2 nmethods.
    return;
  }

  // CodeCache_lock is held, so we can safely decrement the count.
  _total_c2_nmethods_count--;
}

void HotCodeGrouper::register_nmethod(nmethod* nm) {
  assert_lock_strong(CodeCache_lock);
  if (!_is_initialized) {
    return;
  }

  if (!nm->is_compiled_by_c2()) {
    return; // Only C2 nmethods are relocated to HotCodeHeap.
  }

  if (CodeCache::get_code_blob_type(nm) == CodeBlobType::MethodHot) {
    // Nmethods in the hot code heap do not count towards total C2 nmethods.
    return;
  }

  // CodeCache_lock is held, so we can safely increment the count.
  _new_c2_nmethods_count++;
  _total_c2_nmethods_count++;
}
#endif // COMPILER2
