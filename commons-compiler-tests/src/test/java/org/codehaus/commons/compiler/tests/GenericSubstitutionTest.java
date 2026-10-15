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
 * Tests for JANINO's "generics" support: Member accesses through explicitly parameterized types substitute the
 * declared type variables with the actual type arguments (JLS8 4.5.2), which makes the casts from the JANINO
 * documentation's "Java 5 language features" restrictions unnecessary, and gives INFERRED LAMBDA parameters their
 * types.
 * <p>
 *   Notice: JANINO does not verify bounds, variance or type argument containment; such "unchecked" code compiles
 *   like it does against raw types.
 * </p>
 * <p>
 *   These tests use the JANINO compiler factory only (not the JDK adapter), because they verify JANINO's own
 *   type-substitution implementation.
 * </p>
 */
@RunWith(Parameterized.class) public
class GenericSubstitutionTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    GenericSubstitutionTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    /** "Map<String, String>.get()" returns "String" (not the erased "Object"); the famous JANINO restriction. */
    @Test public void
    testMethodReturnTypeSubstitution() throws Exception {
        Assert.assertEquals(
            "v",
            this.run("public class GS1 { public static String m() {"
            + " java.util.Map<String,String> mm = new java.util.HashMap<String,String>();"
            + " mm.put(\"k\", \"v\");"
            + " return mm.get(\"k\");"
            + "} }")
        );
    }

    /** "List<String>.get(int)" and unboxing of "List<Integer>.get(int)". */
    @Test public void
    testListGetSubstitution() throws Exception {
        Assert.assertEquals(
            "s7",
            this.run("public class GS2 { public static String m() {"
            + " java.util.List<String> l = new java.util.ArrayList<String>(); l.add(\"s\");"
            + " java.util.List<Integer> is = new java.util.ArrayList<Integer>(); is.add(7);"
            + " return l.get(0) + is.get(0);"
            + "} }")
        );
    }

    /** Field access through a parameterized receiver, and nested parameterizations. */
    @Test public void
    testFieldAccessAndNesting() throws Exception {
        Assert.assertEquals(
            "n",
            this.run("public class GS3 {"
            + " static java.util.Iterator<String> it;"
            + " public static String m() {"
            + "     java.util.Map<String, java.util.List<String>> m2 = new java.util.HashMap<String, java.util.List<String>>();"
            + "     java.util.List<String> l = new java.util.ArrayList<String>(); l.add(\"n\");"
            + "     m2.put(\"a\", l);"
            + "     it = l.iterator();"
            + "     return m2.get(\"a\").get(0) + it.next().substring(1);"
            + " }"
            + "}")
        );
    }

    /** INFERRED LAMBDA parameters get the substituted SAM parameter types. */
    @Test public void
    testInferredLambdaParameterTypes() throws Exception {
        Assert.assertEquals(
            "4lloOK",
            this.run("public class GS4 { public static String m() {"
            + " java.util.function.Function<String,Integer> f1 = s -> s.length();"
            + " java.util.function.BiFunction<String,Integer,String> f2 = (a, b) -> a.substring(b);"
            + " java.util.function.Function<String,String> f3 = s -> { return s.toUpperCase(); };"
            + " return f1.apply(\"abcd\") + f2.apply(\"hello\", 2) + f3.apply(\"ok\");"
            + "} }")
        );
    }

    /** The diamond operator works where the target type is a parameterized type. */
    @Test public void
    testDiamondOperator() throws Exception {
        Assert.assertEquals(
            "d",
            this.run("public class GS5 { public static String m() {"
            + " java.util.Map<String,String> mm = new java.util.HashMap<>();"
            + " mm.put(\"k\", \"d\");"
            + " return mm.get(\"k\");"
            + "} }")
        );
    }

    /** The enhanced FOR statement iterates a parameterized "Iterable". */
    @Test public void
    testForEachOverParameterizedIterable() throws Exception {
        Assert.assertEquals(
            "abc",
            this.run("public class GS6 { public static String m() {"
            + " java.util.List<String> l = new java.util.ArrayList<String>();"
            + " l.add(\"a\"); l.add(\"b\"); l.add(\"c\");"
            + " StringBuilder sb = new StringBuilder();"
            + " for (String s : l) sb.append(s);"
            + " return sb.toString();"
            + "} }")
        );
    }

    /** "Optional<T>.get()" returns "T". */
    @Test public void
    testOptional() throws Exception {
        Assert.assertEquals(
            "opt",
            this.run("public class GS7 { public static String m() {"
            + " return java.util.Optional.of(\"opt\").get();"
            + "} }")
        );
    }

    // ----------------------------------------------------------------------------------------------------------------

    private Object
    run(String compilationUnit) throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(compilationUnit);
        try {
            return sc.getClassLoader().loadClass(compilationUnit.replaceAll("(?s).*class (\\w+).*", "$1")).getMethod("m").invoke(null);
        } catch (InvocationTargetException ite) {
            Throwable te = ite.getTargetException();
            throw te instanceof Exception ? (Exception) te : ite;
        }
    }
}
