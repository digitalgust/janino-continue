/*
 * Janino - An embedded Java[TM] compiler
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

// SUPPRESS CHECKSTYLE JavadocMethod|MethodName:9999

package org.codehaus.commons.compiler.tests;

import java.util.List;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Tests for the Java 7 "multi-CATCH" feature, i.e. "{@code catch (E1 | E2 e) ...}".
 */
@RunWith(Parameterized.class) public
class MultiCatchTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    MultiCatchTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    /** Both alternatives route to the one handler; exception table has one entry per alternative. */
    @Test public void
    testAlternativesRouteToSameHandler() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class MC1 {\n"
            + "    public static boolean main() {\n"
            + "        if (!\"EOF\".equals(mc(1))) return false;\n"
            + "        if (!\"INT\".equals(mc(2))) return false;\n"
            + "        if (!\"NONE\".equals(mc(0))) return false;\n"
            + "        return true;\n"
            + "    }\n"
            + "    static String mc(int i) {\n"
            + "        try { thrower(i); return \"NONE\"; }\n"
            + "        catch (java.io.IOException | InterruptedException e) { return e instanceof java.io.IOException ? \"EOF\" : \"INT\"; }\n"
            + "    }\n"
            + "    static void thrower(int i) throws java.io.IOException, InterruptedException {\n"
            + "        if (i == 1) throw new java.io.EOFException();\n"
            + "        if (i == 2) throw new InterruptedException();\n"
            + "    }\n"
            + "}\n"
        ), "MC1");
    }

    /** The declared type of the catch parameter is the LUB of the alternatives; its methods are accessible. */
    @Test public void
    testCatchParameterTypeIsLub() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class MC2 {\n"
            + "    public static boolean main() throws Exception {\n"
            + "        try { thrower(); }\n"
            + "        catch (java.io.EOFException | java.io.FileNotFoundException e) {\n"
            + "            // getMessage() is declared on the LUB \"java.io.IOException\".\n"
            + "            return e.getMessage() == null;\n"
            + "        }\n"
            + "        return false;\n"
            + "    }\n"
            + "    static void thrower() throws java.io.EOFException, java.io.FileNotFoundException { throw new java.io.EOFException(); }\n"
            + "}\n"
        ), "MC2");
    }

    /** Precise rethrow: A FINAL multi-CATCH parameter only throws its alternatives. */
    @Test public void
    testPreciseRethrowOfFinalCatchParameter() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class MC3 {\n"
            + "    public static boolean main() {\n"
            + "        try { rethrow(); return false; }\n"
            + "        catch (java.io.EOFException e) { return true; }\n"
            + "        catch (InterruptedException e) { return false; }\n"
            + "    }\n"
            + "    static void rethrow() throws java.io.EOFException, InterruptedException {\n"
            + "        try { mayThrow(); }\n"
            + "        catch (final java.io.EOFException | InterruptedException e) { throw e; }\n"
            + "    }\n"
            + "    static void mayThrow() throws java.io.EOFException, InterruptedException { throw new java.io.EOFException(); }\n"
            + "}\n"
        ), "MC3");
    }

    /**
     * Multi-CATCH parameters are implicitly FINAL (JLS8 14.20): Assigning one is a compile error, and precise
     * rethrow applies even if the parameter is not declared "final".
     */
    @Test public void
    testMultiCatchParameterIsImplicitlyFinal() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class MC4 {\n"
            + "    public static boolean main() {\n"
            + "        try { rethrow(); return false; }\n"
            + "        catch (java.io.EOFException e) { return true; }\n"
            + "        catch (InterruptedException e) { return false; }\n"
            + "    }\n"
            + "    static void rethrow() throws java.io.EOFException, InterruptedException {\n"
            + "        try { mayThrow(); }\n"
            + "        catch (java.io.EOFException | InterruptedException e) { throw e; }\n"
            + "    }\n"
            + "    static void mayThrow() throws java.io.EOFException, InterruptedException { throw new java.io.EOFException(); }\n"
            + "}\n"
        ), "MC4");
        this.assertCompilationUnitUncookable((
            ""
            + "public class MC4b {\n"
            + "    static void m() throws Exception {\n"
            + "        try { m2(); } catch (java.io.EOFException | InterruptedException e) { e = null; }\n"
            + "    }\n"
            + "    static void m2() throws java.io.EOFException, InterruptedException {}\n"
            + "}\n"
        ));
    }

    /** Alternatives related by subclassing are rejected (JLS7 14.20). */
    @Test public void
    testRelatedAlternativesRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "public class MC5 {\n"
            + "    void mc() { try { m(); } catch (java.io.IOException | java.io.EOFException e) {} }\n"
            + "    void m() {}\n"
            + "}\n"
        ));
    }

    /** Alternatives not assignable to "java.lang.Throwable" are rejected. */
    @Test public void
    testNonThrowableAlternativesRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "public class MC6 {\n"
            + "    void mc() { try { m(); } catch (String | Integer e) {} }\n"
            + "    void m() {}\n"
            + "}\n"
        ));
    }

    /** Multi-CATCH combined with a "finally" clause. */
    @Test public void
    testMultiCatchWithFinally() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class MC7 {\n"
            + "    static String log = \"\";\n"
            + "    public static boolean main() {\n"
            + "        mc();\n"
            + "        return \"CF\".equals(log);\n"
            + "    }\n"
            + "    static void mc() {\n"
            + "        try { mayThrow(); }\n"
            + "        catch (java.io.EOFException | InterruptedException e) { log += \"C\"; }\n"
            + "        finally { log += \"F\"; }\n"
            + "    }\n"
            + "    static void mayThrow() throws java.io.EOFException, InterruptedException { throw new java.io.EOFException(); }\n"
            + "}\n"
        ), "MC7");
    }
}
