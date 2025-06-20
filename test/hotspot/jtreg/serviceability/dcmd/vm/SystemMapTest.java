/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat, Inc. and/or its affiliates.
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

import org.testng.annotations.Test;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test id=normal
 * @summary Test of diagnostic command System.map
 * @library /test/lib
 * @requires (vm.gc != "Z") & (os.family == "linux" | os.family == "windows" | os.family == "mac")
 * @comment ASAN changes the memory map dump slightly, but the test has rather strict requirements
 * @requires !vm.asan
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+UsePerfData SystemMapTest
 */

/*
 * @test id=zgc
 * @bug 8346717
 * @summary Test of diagnostic command System.map using ZGC
 * @library /test/lib
 * @requires vm.gc.Z & (os.family == "linux" | os.family == "windows" | os.family == "mac")
 * @comment ASAN changes the memory map dump slightly, but the test has rather strict requirements
 * @requires !vm.asan
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+UsePerfData -XX:+UseZGC SystemMapTest
 */
public class SystemMapTest extends SystemMapTestBase {
    public void run(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("System.map");
        boolean NMTOff = output.contains("NMT is disabled");
        for (String s: shouldMatchUnconditionally()) {
            output.shouldMatch(s);
        }
        if (!NMTOff) { // expect VM annotations if NMT is on
            for (String s: shouldMatchIfNMTIsEnabled()) {
                output.shouldMatch(s);
            }
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
