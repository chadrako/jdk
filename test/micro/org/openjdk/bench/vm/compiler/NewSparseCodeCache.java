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
import java.util.Random;

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

import org.openjdk.bench.util.InMemoryJavaCompiler;

import jdk.test.whitebox.WhiteBox;
import jdk.test.whitebox.code.NMethod;

/*
 * This benchmark is used to check performance when the code cache is sparse.
 *
 * We use C2 compiler to compile the same Java method multiple times
 * to produce as many code as needed.
 * These compiled methods represent the active methods in the code cache.
 * Each compiled method is placed in its own code region of fixed size.
 * CodeCache becomes sparse when code regions are not fully filled.
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
    "-XX:CompileCommand=compileonly,A::sum",
    "-XX:CompileCommand=dontinline,A::sum",
    "-XX:-UseCodeCacheFlushing",
    "-XX:-TieredCompilation",
    "-XX:+SegmentedCodeCache",
    "-XX:ReservedCodeCacheSize=512m",
    "-XX:InitialCodeCacheSize=512m",
    "-XX:+UseSerialGC",
    "-XX:+PrintCodeCache"
})
public class NewSparseCodeCache {

    private static final int C2_LEVEL = 4;
    private static final int DUMMY_BLOB_SIZE = 1024 * 1024;
    private static final int DUMMY_BLOB_COUNT = 128;

    static byte[] num1;
    static byte[] num2;

    @State(Scope.Thread)
    public static class ThreadState {
        byte[] result;

        @Setup
        public void setup() {
            result = new byte[num1.length + 1];
        }
    }

    private static Object WB;

    @Param({"1", "2", "4", "8", "16", "32", "64"})
    public int activeMethodCount;

    @Param({"2097152"})
    public int codeRegionSize;

    private TestMethod[] methods = {};

    private static byte[] genNum(Random random, int digitCount) {
        byte[] num = new byte[digitCount];
        int d;
        do {
            d = random.nextInt(10);
        } while (d == 0);

        num[0] = (byte)d;
        for (int i = 1; i < digitCount; ++i) {
            num[i] = (byte)random.nextInt(10);
        }
        return num;
    }

    private static void initWhiteBox() {
        WB = WhiteBox.getWhiteBox();
    }

    private static void initNums() {
        final long seed = 8374592837465123L;
        Random random = new Random(seed);

        final int digitCount = 40;
        num1 = genNum(random, digitCount);
        num2 = genNum(random, digitCount);
    }

    private static WhiteBox getWhiteBox() {
        return (WhiteBox)WB;
    }

    private static final class TestMethod {
        private static final String CLASS_NAME = "A";
        private static final String METHOD_TO_COMPILE = "sum";
        private static final String JAVA_CODE = """
        public class A {

            public static void sum(byte[] n1, byte[] n2, byte[] out) {
                System.currentTimeMillis();
            }
        }""";

        private static final byte[] BYTE_CODE;

        static {
            BYTE_CODE = InMemoryJavaCompiler.compile(CLASS_NAME, JAVA_CODE);
        }

        private final Method method;

        private static ClassLoader createClassLoaderFor() {
            return new ClassLoader() {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    if (!name.equals(CLASS_NAME)) {
                        return super.loadClass(name);
                    }

                    return defineClass(name, BYTE_CODE, 0, BYTE_CODE.length);
                }
            };
        }

        public TestMethod() throws Exception {
            var cl = createClassLoaderFor().loadClass(CLASS_NAME);
            method = cl.getMethod(METHOD_TO_COMPILE, byte[].class, byte[].class, byte[].class);
            getWhiteBox().testSetDontInlineMethod(method, true);
        }

        public void profile(byte[] num1, byte[] num2, byte[] result) throws Exception {
            method.invoke(null, num1, num2, result);
            getWhiteBox().markMethodProfiled(method);
        }

        public void invoke(byte[] num1, byte[] num2, byte[] result) throws Exception {
            method.invoke(null, num1, num2, result);
        }

        public void compileWithC2() throws Exception {
            getWhiteBox().enqueueMethodForCompilation(method, C2_LEVEL);
            while (getWhiteBox().isMethodQueuedForCompilation(method)) {
                Thread.onSpinWait();
            }
            if (getWhiteBox().getMethodCompilationLevel(method) != C2_LEVEL) {
                throw new IllegalStateException("Method " + method + " is not compiled by C2.");
            }
        }

        public NMethod getNMethod() {
            return NMethod.get(method, false);
        }
    }

    private void allocateDummyBlobs(int count, int size, int codeBlobType) {
        getWhiteBox().lockCompilation();
        for (int i = 0; i < count; i++) {
            var dummyBlob = getWhiteBox().allocateCodeBlob(size, codeBlobType);
            if (dummyBlob == 0) {
                throw new IllegalStateException("Failed to allocate dummy blob.");
            }
        }
        getWhiteBox().unlockCompilation();
    }

    private void generateCode() throws Exception {
        initNums();

        if ((codeRegionSize & (codeRegionSize - 1)) != 0) {
            throw new IllegalArgumentException("codeRegionSize = " + codeRegionSize
                + ". 'codeRegionSize' must be a power of 2.");
        }

        byte[] result = new byte[num1.length + 1];
        methods = new TestMethod[activeMethodCount];

        for (int i = 0; i < activeMethodCount; i++) {
            methods[i] = new TestMethod();
            methods[i].profile(num1, num2, result);
            methods[i].compileWithC2();

            getWhiteBox().lockCompilation();
            NMethod nmethod = methods[i].getNMethod();
            long regionStart = nmethod.address & ~(codeRegionSize - 1);
            long regionEnd = regionStart + codeRegionSize;
            long remainingSpaceInRegion = regionEnd - nmethod.address - nmethod.size;

            if (remainingSpaceInRegion > 0) {
                getWhiteBox().allocateCodeBlob(
                    (int)remainingSpaceInRegion,
                    nmethod.code_blob_type.id);
            }
            getWhiteBox().unlockCompilation();
        }

        getWhiteBox().lockCompilation();
        NMethod lastNMethod = methods[activeMethodCount - 1].getNMethod();

        while (true) {
            long blob = getWhiteBox().allocateCodeBlob(DUMMY_BLOB_SIZE, lastNMethod.code_blob_type.id);
            if (blob == 0) {
                break;
            }
        }
        getWhiteBox().unlockCompilation();
    }

    @Setup(Level.Trial)
    public void setupCodeCache() throws Exception {
        initWhiteBox();
        generateCode();
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void callMethods(ThreadState s) throws Exception {
        for (var m : methods) {
            m.invoke(num1, num2, s.result);
        }
    }

    @Benchmark
    @Warmup(iterations = 100)
    public void runMethodsWithReflection(ThreadState s) throws Exception {
        callMethods(s);
    }
}
