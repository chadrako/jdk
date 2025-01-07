/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * @test ReplaceNMethodTest
 * @bug 8316684
 * @summary testing of WB::replaceNMethod()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm compiler.whitebox.ReplaceNMethodTest
 */

package compiler.whitebox;

import java.lang.reflect.Method;

import jdk.test.whitebox.code.NMethod;
import jdk.test.whitebox.WhiteBox;

import java.util.ArrayList;
import java.util.Objects;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ReplaceNMethodTest {

    public static void main(String[] args) throws Exception {
        var cmd = new ArrayList<String>();
        cmd.add("-Xbootclasspath/a:.");
        cmd.add("-Xbatch");
        cmd.add("-XX:+UnlockDiagnosticVMOptions");
        cmd.add("-XX:+PrintCompilation");
        cmd.add("-XX:+WhiteBoxAPI");
        cmd.add(ReplaceNMethodTest.ReplaceNMethod.class.getName());
        var pb = ProcessTools.createTestJavaProcessBuilder(cmd);
        var output = new OutputAnalyzer(pb.start());

        if (output.getExitValue() != 0) {
            output.reportDiagnosticSummary();
            throw new RuntimeException("Test exited with non-zero code");
        } else {
            if (!verifyOutput(output.getOutput())) {
                output.outputTo(System.out);
                throw new RuntimeException("Output was incorrect");
            }
        }
    }

    // Matches output for function being compiled. Does not match to deoptimization outputs for that function
    // Example: 4       compiler.whitebox.ReplaceNMethodTest$ReplaceNMethod::function (20 bytes)
    private static String methodCompiledRegex = "^4\\s+compiler\\.whitebox\\.ReplaceNMethodTest\\$ReplaceNMethod::function\\s+\\(\\d+\\s+bytes\\)\\\n$";

    public static boolean verifyOutput(String text) {
        int notCompiled = text.indexOf("Should not be compiled");
        if (notCompiled == -1) {
            return false;
        }

        int functionCompiled = text.indexOf("4       compiler.whitebox.ReplaceNMethodTest$ReplaceNMethod::function");
        if (functionCompiled == -1) {
            return false;
        }

        // Confirm that the function was not compiled when it was not supposed to be
        if (functionCompiled < notCompiled) {
            return false;
        }

        int isCompiled = text.indexOf("Should be compiled");
        if (isCompiled == -1) {
            return false;
        }

        // Confirm that the function was compled when it was supposed to be
        if (functionCompiled > isCompiled) {
            return false;
        }

        // Confirm that the function never gets recompiled
        String remainingOutput = text.substring(functionCompiled + 1);

        // Confirm that the function never gets recompiled
        boolean functionRecompiled = remainingOutput.matches(methodCompiledRegex);
        if (functionRecompiled) {
            return false;
        }

        return true;
    }

    public static class ReplaceNMethod {
        private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
        public static double FUNCTION_RESULT = 0;

        /** Value of {@code -XX:CompileThreshold} */
        private static final int COMPILE_THRESHOLD
            = Integer.parseInt(getVMOption("CompileThreshold", "10000"));

        protected static String getVMOption(String name) {
            Objects.requireNonNull(name);
            return Objects.toString(WHITE_BOX.getVMFlag(name), null);
        }

        protected static String getVMOption(String name, String defaultValue) {
            String result = getVMOption(name);
            return result == null ? defaultValue : result;
        }


        public static void main(String [] args) throws Exception {
            Method method = ReplaceNMethodTest.ReplaceNMethod.class.getMethod("function");
            WHITE_BOX.testSetDontInlineMethod(method, true);

            checkNotCompiled(method, false);

            System.out.println("Should not be compiled");
            callFunction();
            System.out.println("Should be compiled");

            checkCompiled(method, false);
            NMethod origNmethod = NMethod.get(method, false);

            WHITE_BOX.replaceNMethod(method, false, false);

            WHITE_BOX.fullGC();

            checkCompiled(method, false);
            NMethod newNmethod = NMethod.get(method, false);

            if (origNmethod.entry_point == newNmethod.entry_point) {
                throw new RuntimeException("Did not create new nmethod");
            }

            // Calling function again should not recompile
            callFunction();

            checkCompiled(method, false);
        }

        private static void checkNotCompiled(Method method, boolean isOsr) {
            if (WHITE_BOX.isMethodQueuedForCompilation(method)) {
                throw new RuntimeException("checkNotCompiled isMethodQueuedForCompilation");
            }
            if (WHITE_BOX.isMethodCompiled(method, isOsr)) {
                throw new RuntimeException("checkNotCompiled isMethodCompiled");
            }
            if (WHITE_BOX.getMethodCompilationLevel(method, isOsr) != 0) {
                throw new RuntimeException("checkNotCompiled getMethodCompilationLevel");
            }
        }

        private static void checkCompiled(Method method, boolean isOsr) {
            if (WHITE_BOX.isMethodQueuedForCompilation(method)) {
                throw new RuntimeException("checkCompiled isMethodQueuedForCompilation");
            }
            if (!WHITE_BOX.isMethodCompiled(method, isOsr)) {
                throw new RuntimeException("checkCompiled isMethodCompiled");
            }
            if (WHITE_BOX.getMethodCompilationLevel(method, isOsr) == 0) {
                throw new RuntimeException("checkCompiledgetMethodCompilationLevel ");
            }
        }

        private static void callFunction() {
            for (int i = 0; i < COMPILE_THRESHOLD; i++) {
                function();
            }
        }

        public static void function() {
            FUNCTION_RESULT = Math.random();
        }
    }
}
