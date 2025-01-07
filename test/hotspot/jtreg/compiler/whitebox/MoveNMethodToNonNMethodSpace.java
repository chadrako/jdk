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
 * @test MoveNMethodToNonNMethodSpace
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
 * @run main/othervm -Xbootclasspath/a:. -Xbatch -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.whitebox.MoveNMethodToNonNMethodSpace
 */
package compiler.whitebox;

import java.lang.reflect.Method;

import jdk.test.whitebox.code.NMethod;
import jdk.test.whitebox.WhiteBox;

import java.util.ArrayList;
import java.util.Objects;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class MoveNMethodToNonNMethodSpace {

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
        Method method = MoveNMethodToNonNMethodSpace.class.getMethod("function", int.class);
        WHITE_BOX.testSetDontInlineMethod(method, true);

        checkNotCompiled(method, false);

        callFunction();

        checkCompiled(method, false);
        NMethod origNmethod = NMethod.get(method, false);

        WHITE_BOX.replaceNMethod(method, false, 2);

        WHITE_BOX.fullGC();

        checkCompiled(method, false);
        NMethod newNmethod = NMethod.get(method, false);

        if (origNmethod.code_blob_type == newNmethod.code_blob_type) {
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
            function(i);
        }
    }

    public static void function(int n) {
        double result = 0.0;
        for (int i = 1; i <= n; i++) {
            result += Math.sin(i) * Math.cos(i) / (Math.sqrt(i) + 1);
            result = recursiveComputation(result, i % 5); // Introduces recursion
        }

        FUNCTION_RESULT = result;
    }

    private static double recursiveComputation(double value, int depth) {
        if (depth == 0) return value;
        return Math.tan(value) / (1 + recursiveComputation(value / 2, depth - 1));
    }
}
