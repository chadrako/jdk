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

package org.openjdk.bench.vm.compiler;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import jdk.test.whitebox.WhiteBox;

/*
 * This benchmark is used to check performance when the code cache is sparse.
 *
 * We use C2 compiler to compile multiple methods (method0 through method3)
 * and place each compiled method in its own code region of fixed size.
 * CodeCache becomes sparse when code regions are not fully filled.
 *
 * The callMethods() method directly calls these methods, stressing
 * branch prediction across a sparse code cache.
 *
 * The benchmark parameters are active method count and code region size.
 */

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+WhiteBoxAPI",
    "-Xbootclasspath/a:lib-test/wb.jar",
    "-XX:CompileCommand=compileonly,org.openjdk.bench.vm.compiler.NewSparseCodeCache::callMethods",
    "-XX:CompileCommand=dontinline,org.openjdk.bench.vm.compiler.NewSparseCodeCache::callMethods",
    "-XX:CompileCommand=compileonly,org.openjdk.bench.vm.compiler.NewSparseCodeCache::method*",
    "-XX:CompileCommand=dontinline,org.openjdk.bench.vm.compiler.NewSparseCodeCache::method*",
    "-XX:-UseCodeCacheFlushing",
    "-XX:-TieredCompilation",
    "-XX:+SegmentedCodeCache",
    "-XX:ReservedCodeCacheSize=512m",
    "-XX:InitialCodeCacheSize=512m",
    "-XX:+PrintCodeCache"
})
public class NewSparseCodeCache {

    private static final int C2_LEVEL = 4;
    private static final int DUMMY_BLOB_SIZE = 1024 * 1024;
    private static final int LARGE_GAP_SIZE = 128 * 1024 * 1024; // 128MB gap before callMethods()
    private static final int BLOB_TYPE = 0; // BlobType.MethodNonProfiled.id
    private static final int CALL_METHODS_WARMUP_ITERATIONS = 20_000;
    private static final int BENCHMARK_WARMUP_ITERATIONS = 1;

    private static WhiteBox WB;

    @Param({"128"})
    public int activeMethodCount;

    @Param({"2097152"})
    public int codeRegionSize;

    // Methods to be compiled 2MB apart
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method0() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method1() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method2() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method3() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method4() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method5() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method6() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method7() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method8() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method9() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method10() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method11() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method12() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method13() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method14() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method15() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method16() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method17() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method18() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method19() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method20() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method21() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method22() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method23() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method24() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method25() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method26() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method27() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method28() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method29() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method30() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method31() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method32() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method33() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method34() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method35() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method36() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method37() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method38() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method39() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method40() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method41() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method42() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method43() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method44() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method45() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method46() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method47() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method48() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method49() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method50() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method51() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method52() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method53() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method54() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method55() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method56() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method57() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method58() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method59() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method60() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method61() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method62() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method63() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method64() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method65() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method66() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method67() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method68() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method69() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method70() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method71() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method72() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method73() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method74() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method75() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method76() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method77() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method78() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method79() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method80() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method81() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method82() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method83() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method84() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method85() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method86() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method87() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method88() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method89() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method90() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method91() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method92() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method93() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method94() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method95() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method96() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method97() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method98() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method99() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method100() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method101() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method102() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method103() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method104() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method105() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method106() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method107() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method108() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method109() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method110() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method111() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method112() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method113() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method114() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method115() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method116() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method117() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method118() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method119() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method120() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method121() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method122() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method123() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method124() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method125() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method126() {}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void method127() {}

    private static void initWhiteBox() {
        WB = WhiteBox.getWhiteBox();
    }

    private void compileWithC2(Method method) throws Exception {
        WB.enqueueMethodForCompilation(method, C2_LEVEL);
        while (WB.isMethodQueuedForCompilation(method)) {
            Thread.onSpinWait();
        }
        if (WB.getMethodCompilationLevel(method) != C2_LEVEL) {
            throw new IllegalStateException("Method " + method + " is not compiled by C2.");
        }
    }

    private void compileMethodWithSpacing(String methodName) throws Exception {
        Method method = NewSparseCodeCache.class.getDeclaredMethod(methodName);

        WB.markMethodProfiled(method);
        WB.testSetDontInlineMethod(method, true);

        compileWithC2(method);

        WB.lockCompilation();
        WB.allocateCodeBlob(codeRegionSize, BLOB_TYPE);
        WB.unlockCompilation();
    }

    private void generateCode() throws Exception {
        if ((codeRegionSize & (codeRegionSize - 1)) != 0) {
            throw new IllegalArgumentException("codeRegionSize = " + codeRegionSize
                + ". 'codeRegionSize' must be a power of 2.");
        }

        // Step 1: Compile method0-method3 with codeRegionSize gaps after each
        for (int i = 0; i < activeMethodCount; i++) {
            compileMethodWithSpacing("method" + i);
        }

        // Step 2: Allocate 128MB gap before callMethods()
        WB.lockCompilation();
        WB.allocateCodeBlob(LARGE_GAP_SIZE, BLOB_TYPE);
        WB.unlockCompilation();

        // Step 3: Compile callMethods()
        compileCallMethods();

        // Step 4: Fill remaining code cache with dummy blobs
        WB.lockCompilation();
        while (true) {
            long blob = WB.allocateCodeBlob(DUMMY_BLOB_SIZE, BLOB_TYPE);
            if (blob == 0) {
                break;
            }
        }
        WB.unlockCompilation();
    }

    private void compileCallMethods() throws Exception {
        // Warmup callMethods before compilation
        for (int i = 0; i < CALL_METHODS_WARMUP_ITERATIONS; i++) {
            callMethods();
        }

        Method method = NewSparseCodeCache.class.getDeclaredMethod("callMethods");
        WB.markMethodProfiled(method);
        compileWithC2(method);
        WB.testSetDontInlineMethod(method, true);
    }

    @Setup(Level.Trial)
    public void setupCodeCache() throws Exception {
        initWhiteBox();
        generateCode();
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @SuppressWarnings("fallthrough")  // Intentional fallthrough to call all active methods
    private void callMethods() {
        switch (activeMethodCount) {
            case 128:
                method127();
            case 127:
                method126();
            case 126:
                method125();
            case 125:
                method124();
            case 124:
                method123();
            case 123:
                method122();
            case 122:
                method121();
            case 121:
                method120();
            case 120:
                method119();
            case 119:
                method118();
            case 118:
                method117();
            case 117:
                method116();
            case 116:
                method115();
            case 115:
                method114();
            case 114:
                method113();
            case 113:
                method112();
            case 112:
                method111();
            case 111:
                method110();
            case 110:
                method109();
            case 109:
                method108();
            case 108:
                method107();
            case 107:
                method106();
            case 106:
                method105();
            case 105:
                method104();
            case 104:
                method103();
            case 103:
                method102();
            case 102:
                method101();
            case 101:
                method100();
            case 100:
                method99();
            case 99:
                method98();
            case 98:
                method97();
            case 97:
                method96();
            case 96:
                method95();
            case 95:
                method94();
            case 94:
                method93();
            case 93:
                method92();
            case 92:
                method91();
            case 91:
                method90();
            case 90:
                method89();
            case 89:
                method88();
            case 88:
                method87();
            case 87:
                method86();
            case 86:
                method85();
            case 85:
                method84();
            case 84:
                method83();
            case 83:
                method82();
            case 82:
                method81();
            case 81:
                method80();
            case 80:
                method79();
            case 79:
                method78();
            case 78:
                method77();
            case 77:
                method76();
            case 76:
                method75();
            case 75:
                method74();
            case 74:
                method73();
            case 73:
                method72();
            case 72:
                method71();
            case 71:
                method70();
            case 70:
                method69();
            case 69:
                method68();
            case 68:
                method67();
            case 67:
                method66();
            case 66:
                method65();
            case 65:
                method64();
            case 64:
                method63();
            case 63:
                method62();
            case 62:
                method61();
            case 61:
                method60();
            case 60:
                method59();
            case 59:
                method58();
            case 58:
                method57();
            case 57:
                method56();
            case 56:
                method55();
            case 55:
                method54();
            case 54:
                method53();
            case 53:
                method52();
            case 52:
                method51();
            case 51:
                method50();
            case 50:
                method49();
            case 49:
                method48();
            case 48:
                method47();
            case 47:
                method46();
            case 46:
                method45();
            case 45:
                method44();
            case 44:
                method43();
            case 43:
                method42();
            case 42:
                method41();
            case 41:
                method40();
            case 40:
                method39();
            case 39:
                method38();
            case 38:
                method37();
            case 37:
                method36();
            case 36:
                method35();
            case 35:
                method34();
            case 34:
                method33();
            case 33:
                method32();
            case 32:
                method31();
            case 31:
                method30();
            case 30:
                method29();
            case 29:
                method28();
            case 28:
                method27();
            case 27:
                method26();
            case 26:
                method25();
            case 25:
                method24();
            case 24:
                method23();
            case 23:
                method22();
            case 22:
                method21();
            case 21:
                method20();
            case 20:
                method19();
            case 19:
                method18();
            case 18:
                method17();
            case 17:
                method16();
            case 16:
                method15();
            case 15:
                method14();
            case 14:
                method13();
            case 13:
                method12();
            case 12:
                method11();
            case 11:
                method10();
            case 10:
                method9();
            case 9:
                method8();
            case 8:
                method7();
            case 7:
                method6();
            case 6:
                method5();
            case 5:
                method4();
            case 4:
                method3();
            case 3:
                method2();
            case 2:
                method1();
            case 1:
                method0();
        }
    }

    @Benchmark
    @Warmup(iterations = BENCHMARK_WARMUP_ITERATIONS)
    public void benchmarkCallMethods() {
        callMethods();
    }
}