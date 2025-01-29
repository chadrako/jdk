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
 * @run main/othervm -Xbootclasspath/a:. -Xmixed -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   compiler.whitebox.replaceNMethodTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import jdk.test.whitebox.code.BlobType;
import jdk.test.whitebox.code.NMethod;

public class ReplaceNMethodTest extends CompilerWhiteBoxTest {
    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(ReplaceNMethodTest::new, args);
    }

    private ReplaceNMethodTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    @Override
    protected void test() throws Exception {
        checkNotCompiled();

        compile();
        checkCompiled();

        WHITE_BOX.replaceNMethod(method, testCase.isOsr());

        WHITE_BOX.fullGC();

        checkCompiled();
    }
}
