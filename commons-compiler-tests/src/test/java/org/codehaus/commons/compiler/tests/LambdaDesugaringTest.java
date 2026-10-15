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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Tests for the LAMBDA expressions and method references, which JANINO compiles through "desugaring" into anonymous
 * classes (JLS9 15.27, 15.13).
 * <p>
 *   The generated classes behave like JAVAC-generated LAMBDAs in all tested cases; the known deviations are:
 * </p>
 * <ul>
 *   <li>Bound method references support local variables and {@code this}; complex receiver expressions must first
 *       be assigned to a local variable</li>
 *   <li>Overload resolution and generic inference implement a subset of the Java rules</li>
 * </ul>
 */
@RunWith(Parameterized.class) public
class LambdaDesugaringTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    LambdaDesugaringTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    /**
     * One method with the most relevant LAMBDA/method reference forms; the results are compared with the results
     * that JAVAC produces for the same code.
     */
    @Test public void
    testLambdaAndMethodReferenceForms() throws Exception {

        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook((
            ""
            + "public class LD1 {\n"
            + "    static int adder(int a, int b) { return a + b; }\n"
            + "    public static String main() throws Exception {\n"
            + "        final StringBuilder sb = new StringBuilder();\n"
            + "        java.util.function.Supplier<String> s1 = () -> \"hello\";\n"                          // Expression body.
            + "        sb.append(s1.get()).append(';');\n"
            + "        sb.append(apply((Integer x) -> x * 2, 21)).append(';');\n"                             // Explicitly typed parameter, invocation-argument position, GENERIC SAM.
            + "        java.util.function.Supplier<Integer> s2 = () -> { int r = 0; for (int i = 1; i <= 3; i++) r += i; return r; };\n"  // Block body.
            + "        sb.append(s2.get()).append(';');\n"
            + "        final int base = 100;\n"
            + "        java.util.function.Supplier<Integer> s3 = () -> base + 6;\n"                           // Effectively final (declared final) capture.
            + "        sb.append(s3.get()).append(';');\n"
            + "        java.util.function.BiFunction<Integer,Integer,Integer> f1 = LD1::adder;\n"             // STATIC method reference.
            + "        sb.append((Integer) f1.apply(1, 2)).append(';');\n"
            + "        java.util.function.Function<String,Integer> f2 = String::length;\n"                    // UNBOUND instance method reference.
            + "        sb.append(f2.apply(\"abcd\")).append(';');\n"
            + "        final String prefix = \"P-\";\n"
            + "        java.util.function.Supplier<String> f3 = prefix::trim;\n"                              // BOUND instance method reference.
            + "        sb.append(f3.get()).append(';');\n"
            + "        java.util.function.Supplier<StringBuilder> f4 = StringBuilder::new;\n"                 // Constructor reference.
            + "        sb.append(((StringBuilder) f4.get()).append(\"sb\")).append(';');\n"
            + "        java.util.function.IntFunction<String[]> f5 = String[]::new;\n"                        // Array creation reference.
            + "        sb.append(((String[]) f5.apply(7)).length).append(';');\n"
            + "        sb.append(runIt(() -> sb.append(\"run\"))).append(';');\n"                             // void SAM in invocation-argument position, capturing.
            + "        java.util.function.Consumer<String> c = (String msg) -> sb.append(msg);\n"             // void SAM, explicitly typed parameter.
            + "        c.accept(\"consumed\");\n"
            + "        return sb.toString();\n"
            + "    }\n"
            + "    static <T, R> R apply(java.util.function.Function<T,R> f, T t) { return f.apply(t); }\n"
            + "    static String runIt(Runnable r) { r.run(); return \"ok\"; }\n"
            + "}\n"
        ));

        Assert.assertEquals(
            "hello;42;6;106;3;4;P-;sb;7;runok;consumed",
            LambdaDesugaringTest.invoke(sc.getClassLoader().loadClass("LD1"), "main")
        );
    }

    /** LAMBDAs in the RETURN statement position. */
    @Test public void
    testLambdaInReturnStatement() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook((
            ""
            + "public class LD2 {\n"
            + "    public static String main() {\n"
            + "        java.util.function.Supplier<String> f1 = LD2.returnLambda();\n"
            + "        java.util.function.Supplier<String> f2 = LD2.returnMethodRef();\n"
            + "        return String.valueOf(f1.get()) + String.valueOf(f2.get());\n"
            + "    }\n"
            + "    static java.util.function.Supplier<String> returnLambda() { return () -> \"lambda;\"; }\n"
            + "    static java.util.function.Supplier<String> returnMethodRef() { final String s = \"mr\"; return s::intern; }\n"  // "String.intern()" is a no-arg String-returning method.
            + "}\n"
        ));
        Assert.assertEquals("lambda;mr", LambdaDesugaringTest.invoke(sc.getClassLoader().loadClass("LD2"), "main"));
    }

    /** A LAMBDA in a CAST expression gets the cast type as its target type. */
    @Test public void
    testLambdaInCast() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook((
            ""
            + "public class LD3 {\n"
            + "    interface StrSupplier { String get(); }\n"
            + "    public static String main() {\n"
            + "        Object o = (StrSupplier) () -> \"cast\";\n"
            + "        return ((StrSupplier) o).get();\n"
            + "    }\n"
            + "}\n"
        ));
        Assert.assertEquals("cast", LambdaDesugaringTest.invoke(sc.getClassLoader().loadClass("LD3"), "main"));
    }
    /** The unqualified "this" in a LAMBDA body binds to the ENCLOSING instance (JLS8 15.27.2). */
    @Test public void
    testBareThisInLambdaBodyBindsToEnclosingInstance() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook((
            ""
            + "public class LD4 {\n"
            + "    public String toString() { return \"enclosing\"; }\n"
            + "    public java.util.function.Supplier<String> f = () -> this.toString();\n"
            + "    public static String main() { return new LD4().f.get(); }\n"
            + "}\n"
        ));
        org.junit.Assert.assertEquals("enclosing", sc.getClassLoader().loadClass("LD4").getMethod("main").invoke(null));
    }

    /** A target type which is not a functional interface is rejected. */
    @Test public void
    testNonFunctionalTargetTypeRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "public class LD5 {\n"
            + "    public String f = () -> \"x\";\n"
            + "}\n"
        ));
    }

    /** Wrong LAMBDA parameter arity is rejected. */
    @Test public void
    testWrongArityRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "public class LD6 {\n"
            + "    public java.util.function.Supplier<String> f = (a, b) -> \"x\";\n"
            + "}\n"
        ));
    }

    /** Effectively final locals can be captured; a subsequent write makes capture invalid. */
    @Test public void
    testEffectivelyFinalCapture() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        String source = (
            ""
            + "public class LD7 {\n"
            + "    public static String main() {\n"
            + "        int base = 1;\n"
            + "        java.util.function.Supplier<Integer> f = () -> base;\n"
            + "        return String.valueOf(f.get());\n"
            + "    }\n"
            + "}\n"
        );
        sc.cook(source);
        Assert.assertEquals("1", LambdaDesugaringTest.invoke(sc.getClassLoader().loadClass("LD7"), "main"));
        this.assertCompilationUnitUncookable(source.replace("return String.valueOf", "base++; return String.valueOf"));
        this.assertCompilationUnitUncookable(source.replace("return String.valueOf", "base = 2; return String.valueOf"));
    }

    // ----------------------------------------------------------------------------------------------------------------

    private static Object
    invoke(Class<?> c, String methodName) throws Exception {
        try {
            return c.getMethod(methodName).invoke(null);
        } catch (InvocationTargetException ite) {
            Throwable te = ite.getTargetException();
            throw te instanceof Exception ? (Exception) te : ite;
        }
    }
}
