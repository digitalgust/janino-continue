/*
 * Janino - An embedded Java compiler
 * Licensed under the Apache License, Version 2.0.
 */
package org.codehaus.commons.compiler.tests;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.TestUtil;

/** Same-source semantic and rejection regressions, executed with both Janino and javac. */
@RunWith(Parameterized.class) public class FeatureUpgradeRegressionTest {
    public static class Box<T> { public T value; public Box(T value) { this.value = value; } }
    public static <T> T first(T a, T b) { return a; }
    public static <T extends Number & Comparable<T>> T boundedIdentity(T value) { return value; }
    public static class Shadow<T> { public <T> T id(T value) { return value; } }

    public interface NumberA { Number get(); }
    public interface NumberB { Integer get(); }
    public interface NumberC extends NumberA, NumberB {}
    public static class G<T> {
        public T value;
        public G(T value) { this.value = value; }
        public T ignore(T ignored) { return this.value; }
    }

    private final ICompilerFactory factory;
    private final String source, expected, reason;

    public FeatureUpgradeRegressionTest(ICompilerFactory factory, String name, String source, String expected, String reason) {
        this.factory = factory; this.source = source; this.expected = expected; this.reason = reason;
    }

    @Parameterized.Parameters(name = "CompilerFactory={0}, Case={1}") public static List<Object[]> cases() throws Exception {
        List<Object[]> result = new ArrayList<Object[]>();
        for (Object[] factory : TestUtil.getCompilerFactoriesForParameters()) {
            for (String[] example : EXAMPLES) {
                result.add(new Object[] { factory[0], example[0], example[1], example[2], example[3] });
            }
        }
        return result;
    }

    @Test public void semantics() throws Exception {
        ISimpleCompiler compiler = this.factory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        try {
            compiler.cook(this.source);
        } catch (CompileException error) {
            if (this.expected != null) throw error;
            if (this.factory.getId().equals("org.codehaus.janino") && !this.reason.isEmpty()) {
                Assert.assertTrue(error.toString(), error.toString().contains(this.reason));
            }
            return;
        }
        Assert.assertNotNull("Invalid source was accepted", this.expected);
        Object value = compiler.getClassLoader().loadClass("Case").getMethod("run").invoke(null);
        Assert.assertEquals(this.expected, String.valueOf(value));
    }

    private static final String[][] EXAMPLES = {
        {
            "java8_completion_varargs_array_element",
            "public class Case {static String use(float[]... xs){return xs.length==1 && xs[0][0]==1?\"ok\":\"bad\";}public static String run(){return use(new float[]{1});}}",
            "ok", ""
        },
        {
            "java8_completion_generic_primitive_array_varargs",
            "import java.util.*;public class Case {public static String run(){List<int[]> xs=Arrays.asList(new int[]{1});return xs.size()==1 && xs.get(0)[0]==1?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_nested_generic_target",
            "import java.util.*;public class Case {interface F {List<String> get();}static String use(F f){return f.get().isEmpty()?\"ok\":\"bad\";}public static String run(){return use(()->Collections.emptyList());}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_nested_reference_target",
            "public class Case {interface F {String get();}interface G {F get();}static String text(){return \"ok\";}static String use(G f){return f.get().get();}public static String run(){return use(()->Case::text);}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_identifier_argument",
            "public class Case {interface F {String get(String s);}static String use(F f){return f.get(\"ok\");}public static String run(){return use(s->s);}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_identifier_ambiguous",
            "public class Case {interface A {String get(String s);}interface B {Object get(String s);}static String pick(A f){return \"a\";}static String pick(B f){return \"b\";}public static String run(){return pick(s->s);}}",
            null, ""
        },
        {
            "java8_completion_intersection_boxing_comparable",
            "public class Case {public static String run(){Number n=(Number & Comparable<Integer>)1;return ((Comparable<Integer>)n).compareTo(1)==0?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_nested_wildcard_identity",
            "import java.util.*;public class Case {static String use(Set<Class<?>> xs){return xs.isEmpty()?\"ok\":\"bad\";}public static String run(){Set<Class<?>> xs=new HashSet<>();return use(xs);}}",
            "ok", ""
        },
        {
            "java8_completion_nested_wildcard_invariant",
            "import java.util.*;public class Case {static String use(List<Class<?>> xs){return \"bad\";}public static String run(){List<Class<String>> xs=new ArrayList<>();return use(xs);}}",
            null, ""
        },
        {
            "java8_completion_intersection_null",
            "public class Case {interface A {}interface B {}public static String run(){Object x=(A & B)null;return x==null?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_boxing",
            "public class Case {public static String run(){Number n=(Number & java.io.Serializable)1;return n.intValue()==1?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_class_bound",
            "public class Case {interface M {}static class C implements M {}public static String run(){C c=(C & M)new C();return c instanceof M?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_final_null",
            "public class Case {interface M {}public static String run(){Object c=(String & M)null;return c==null?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_shared_default",
            "public class Case {interface P {default String get(){return \"ok\";}}interface A extends P {}interface B extends P {}static class C implements A,B {}public static String run(){P c=(A & B)new C();return c.get();}}",
            "ok", ""
        },
        {
            "java8_completion_generic_nested_target",
            "import java.util.*;public class Case {static String use(Map<String,Integer> x){return x.isEmpty()?\"ok\":\"bad\";}public static String run(){return use(Collections.emptyMap());}}",
            "ok", ""
        },
        {
            "java8_completion_generic_nested_wrong_return",
            "import java.util.*;public class Case {static List<Integer> make(){return new ArrayList<Integer>();}static String use(List<String> x){return \"bad\";}public static String run(){return use(make());}}",
            null, ""
        },
        {
            "java8_completion_generic_array_nested_overload",
            "import java.util.*;public class Case {interface M {}public static String run(){M[] xs=new M[0];return Arrays.equals(xs,Arrays.copyOf(xs,0))?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_generic_reflection_varargs",
            "public class Case {public static String run()throws Exception{return String.valueOf(Boolean.class.getMethod(\"valueOf\",boolean.class).invoke(null,true)).equals(\"true\")?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_generic_plain_varargs",
            "public class Case {public static String run(){return String.format(\"%s%s\",\"o\",\"k\");}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_lambda",
            "interface I {default String value(){return \"ok\";}}public class Case implements I {interface F {String get();}public String value(){F f=()->I.super.value();return f.get();}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_poly_argument",
            "interface I {interface F {String get();}default String value(F f){return f.get();}}public class Case implements I {public String value(){return I.super.value(()->\"ok\");}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_varargs_array",
            "interface I {default String value(String... xs){return xs[0];}}public class Case implements I {public String value(){return I.super.value(new String[]{\"ok\"});}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_varargs_empty",
            "interface I {default String value(String... xs){return xs.length==0?\"ok\":\"bad\";}}public class Case implements I {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_checked",
            "interface I {default String value()throws Exception{return \"ok\";}}public class Case implements I {public String value()throws Exception{return I.super.value();}public static String run()throws Exception{return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_unhandled",
            "interface I {default String value()throws Exception{return \"ok\";}}public class Case implements I {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            null, ""
        },
        {
            "java8_completion_interface_super_wrong_generic",
            "interface I<T> {default T value(T t){return t;}}public class Case implements I<String> {public Object value(){return I.super.value(1);}public static Object run(){return new Case().value();}}",
            null, ""
        },
        {
            "java8_completion_intersection_generic_conflict",
            "public class Case {interface F<T> {}interface A extends F<String>{}interface B extends F<Integer>{}public static String run(){Object f=(A & B)null;return \"bad\";}}",
            null, ""
        },
        {
            "java8_completion_intersection_default_conflict",
            "public class Case {interface A {default String get(){return \"a\";}}interface B {default String get(){return \"b\";}}public static String run(){Object f=(A & B)null;return \"bad\";}}",
            null, ""
        },
        {
            "java8_completion_intersection_return_conflict_cast",
            "public class Case {interface A {String get();}interface B {Integer get();}public static String run(){Object f=(A & B)null;return \"bad\";}}",
            null, ""
        },
        {
            "java8_completion_intersection_covariant",
            "public class Case {interface A {Object get();}interface B {String get();}public static String run(){B f=(A & B)()->\"ok\";return f.get();}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_reference_preference",
            "public class Case {interface A {int get();}interface B {Integer get();}static String pick(A f){return \"bad\";}static String pick(B f){return \"ok\";}public static String run(){return pick(()->Integer.valueOf(1));}}",
            "ok", ""
        },
        {
            "java8_completion_generic_super_bad_write",
            "import java.util.*;public class Case {public static String run(){List<? super Integer> xs=new ArrayList<Number>();xs.add(\"bad\");return \"bad\";}}",
            null, ""
        },
        {
            "java8_completion_generic_invariant_argument",
            "import java.util.*;public class Case {static String pick(List<String> xs){return \"bad\";}public static String run(){return pick(new ArrayList<Integer>());}}",
            null, ""
        },
        {
            "java8_completion_interface_static_default",
            "interface I {default String value(){return \"ok\";}static String text(){return \"!\";}}public class Case implements I {public static String run(){return new Case().value()+I.text();}}",
            "ok!", ""
        },
        {
            "java8_completion_interface_static_not_inherited_class",
            "interface I {static String text(){return \"ok\";}}public class Case implements I {public static String run(){return Case.text();}}",
            null, ""
        },
        {
            "java8_completion_interface_static_not_inherited_interface",
            "interface I {static String text(){return \"ok\";}}interface J extends I {}public class Case {public static String run(){return J.text();}}",
            null, ""
        },
        {
            "java8_completion_interface_static_reference",
            "interface I {static String text(){return \"ok\";}}public class Case implements I {interface F {String get();}public static String run(){F f=I::text;return f.get();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super",
            "interface I {default String value(){return \"ok\";}}public class Case implements I {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_inherited",
            "interface I {default String value(){return \"ok\";}}interface J extends I {}public class Case implements J {public String value(){return J.super.value();}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_interface_body",
            "interface I {default String value(){return \"ok\";}}interface J extends I {default String value(){return I.super.value();}}public class Case implements J {public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_varargs",
            "interface I {default String value(String... xs){return xs[0]+xs[1];}}public class Case implements I {public String value(){return I.super.value(\"o\",\"k\");}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_generic",
            "interface I<T> {default T value(T t){return t;}}public class Case implements I<String> {public String value(){return I.super.value(\"ok\");}public static String run(){return new Case().value();}}",
            "ok", ""
        },
        {
            "java8_completion_interface_super_abstract",
            "interface I {String value();}public class Case implements I {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            null, ""
        },
        {
            "java8_completion_interface_super_static",
            "interface I {static String value(){return \"ok\";}}public class Case implements I {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            null, ""
        },
        {
            "java8_completion_interface_super_redundant",
            "interface I {default String value(){return \"ok\";}}interface J extends I {}public class Case implements I,J {public String value(){return I.super.value();}public static String run(){return new Case().value();}}",
            null, ""
        },
        {
            "java8_completion_intersection_lambda",
            "public class Case {interface F {String get();}interface M {}public static String run(){F f=(F & M)()->\"ok\";return (f instanceof M)?f.get():\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_marker_first",
            "public class Case {interface F {String get();}interface M {}interface N {}public static String run(){F f=(M & F & N)()->\"ok\";return (f instanceof M && f instanceof N)?f.get():\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_reference",
            "public class Case {interface F {String get();}interface M {}static String text(){return \"ok\";}public static String run(){F f=(F & M)Case::text;return (f instanceof M)?f.get():\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_generic_lambda",
            "public class Case {interface F<T> {T get();}interface M {}public static String run(){F<String> f=(F<String> & M)()->\"ok\";return (f instanceof M)?f.get():\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_single_parameter",
            "public class Case {interface F {String get(String x);}interface M {}public static String run(){F f=(F & M)x->x;return (f instanceof M)?f.get(\"ok\"):\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_runtime_check",
            "public class Case {interface F {String get();}interface M {}static class C implements F {public String get(){return \"bad\";}}public static String run(){try{F f=(F & M)new C();return f.get();}catch(ClassCastException e){return \"ok\";}}}",
            "ok", ""
        },
        {
            "java8_completion_intersection_duplicate",
            "public class Case {interface F {String get();}public static String run(){F f=(F & F)()->\"ok\";return f.get();}}",
            null, ""
        },
        {
            "java8_completion_intersection_conflicting_returns",
            "public class Case {interface F {String get();}interface M {Integer get();}public static String run(){F f=(F & M)()->\"ok\";return f.get();}}",
            null, ""
        },
        {
            "java8_completion_intersection_bitwise_control",
            "public class Case {public static String run(){int a=3,b=1;return (a & b)==1?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_result_overload",
            "public class Case {interface A {String get(String s);}interface B {Object get(String s);}static String pick(A f){return \"ok\";}static String pick(B f){return \"bad\";}public static String run(){return pick((String s)->s);}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_result_reject_candidate",
            "public class Case {interface A {String get(String s);}interface B {Object get(String s);}static String pick(A f){return \"bad\";}static String pick(B f){return \"ok\";}public static String run(){return pick((String s)->new Object());}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_explicit_parameter_filter",
            "public class Case {interface A {String get(String s);}interface B {String get(Integer s);}static String pick(A f){return f.get(\"ok\");}static String pick(B f){return \"bad\";}public static String run(){return pick((String s)->s);}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_primitive_preference",
            "public class Case {interface A {int get();}interface B {Integer get();}static String pick(A f){return \"ok\";}static String pick(B f){return \"bad\";}public static String run(){return pick(()->1);}}",
            "ok", ""
        },
        {
            "java8_completion_lambda_constant_narrow",
            "public class Case {interface F {byte get();}public static String run(){F f=()->1;return f.get()==1?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_generic_wrong_write",
            "import java.util.*;public class Case {public static String run(){List<String> xs=new ArrayList<String>();xs.add(1);return xs.get(0);}}",
            null, ""
        },
        {
            "java8_completion_generic_safe_write",
            "import java.util.*;public class Case {public static String run(){List<String> xs=new ArrayList<String>();xs.add(\"ok\");return xs.get(0);}}",
            "ok", ""
        },
        {
            "java8_completion_generic_extends_write",
            "import java.util.*;public class Case {public static String run(){List<? extends Number> xs=new ArrayList<Integer>();xs.add(1);return \"bad\";}}",
            null, ""
        },
        {
            "java8_completion_generic_super_write",
            "import java.util.*;public class Case {public static String run(){List<? super Integer> xs=new ArrayList<Number>();xs.add(1);return xs.get(0).equals(1)?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_generic_raw_write",
            "import java.util.*;public class Case {public static String run(){List xs=new ArrayList();xs.add(1);return xs.get(0).equals(1)?\"ok\":\"bad\";}}",
            "ok", ""
        },
        {
            "java8_completion_generic_varargs",
            "public class Case {static <T>T first(T...xs){return xs[0];}public static String run(){return first(\"ok\",\"bad\");}}",
            "ok", ""
        },
        {
            "reference_generic_collection_argument",
            "import java.util.*;import java.util.function.*;public class Case {static String text(){return \"ok\";}"
            + "public static String run(){List<Supplier<String>> fs=new ArrayList<Supplier<String>>();"
            + "fs.add(Case::text);return fs.get(0).get();}}",
            "ok", ""
        },
        {
            "lambda_generic_collection_argument",
            "import java.util.*;public class Case {public static String run(){final StringBuilder b=new StringBuilder();"
            + "List<Runnable> fs=new ArrayList<Runnable>();fs.add(()->b.append(\"ok\"));fs.get(0).run();return b.toString();}}",
            "ok", ""
        },
        {
            "generic_ref_number_bound_ok",
            "import java.util.*;import java.util.function.*;public class Case {static <T extends Number>T id(T x){retu"
            + "rn x;}public static Object run(){Function<Integer,Integer> f=Case::id;return f.apply(7);}}",
            "7", ""
        },
        {
            "generic_ref_number_bound_bad",
            "import java.util.*;import java.util.function.*;public class Case {static <T extends Number>T id(T x){retu"
            + "rn x;}public static Object run(){Function<String,String> f=Case::id;return f.apply(\"x\");}}",
            null, ""
        },
        {
            "generic_ref_f_bound_ok",
            "import java.util.*;import java.util.function.*;public class Case {static <T extends Comparable<T>>T id(T "
            + "x){return x;}public static Object run(){Function<String,String> f=Case::id;return f.apply(\"ok\");}}",
            "ok", ""
        },
        {
            "generic_ref_invariant_bad",
            "import java.util.*;import java.util.function.*;public class Case {static <T>List<T> id(List<T> x){return "
            + "x;}public static Object run(){Function<List<String>,List<Integer>> f=Case::id;return f.apply(Arrays.asLis"
            + "t(\"x\"));}}",
            null, ""
        },
        {
            "generic_ref_return_only",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T empty(){return null;}public"
            + " static String run(){Supplier<String> f=Case::empty;return String.valueOf(f.get());}}",
            "null", ""
        },
        {
            "generic_ref_shadow_class",
            "import java.util.*;import java.util.function.*;public class Case {static class Box<T>{<T>T id(T x){return"
            + " x;}}public static Object run(){Box<String> b=new Box<String>();Function<Integer,Integer> f=b::id;return "
            + "f.apply(7);}}",
            "7", ""
        },
        {
            "generic_ref_binary_bound_ok",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){Function<Int"
            + "eger,Integer> f=org.codehaus.commons.compiler.tests.FeatureUpgradeRegressionTest::boundedIdentity;return "
            + "f.apply(7);}}",
            "7", ""
        },
        {
            "generic_ref_binary_intersection_bad",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){Function<jav"
            + "a.util.concurrent.atomic.AtomicInteger,java.util.concurrent.atomic.AtomicInteger> f=org.codehaus.commons."
            + "compiler.tests.FeatureUpgradeRegressionTest::boundedIdentity;return f.apply(new java.util.concurrent.atom"
            + "ic.AtomicInteger(1));}}",
            null, ""
        },
        {
            "reference_super_write_ok",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List<? super"
            + " Number> xs=new ArrayList<Number>();Consumer<Integer> f=xs::add;f.accept(7);return xs.get(0);}}",
            "7", ""
        },
        {
            "reference_super_write_bad",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List<? super"
            + " Number> xs=new ArrayList<Number>();Consumer<Object> f=xs::add;f.accept(\"bad\");return xs.get(0);}}",
            null, ""
        },
        {
            "reference_extends_write_bad",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List<? exten"
            + "ds Number> xs=new ArrayList<Integer>();Consumer<Integer> f=xs::add;f.accept(7);return xs.get(0);}}",
            null, ""
        },
        {
            "reference_extends_read_ok",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List<? exten"
            + "ds Number> xs=Arrays.asList(7);IntFunction<Number> f=xs::get;return f.apply(0);}}",
            "7", ""
        },
        {
            "reference_wildcard_write_bad",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List<?> xs=n"
            + "ew ArrayList<String>();Consumer<Object> f=xs::add;f.accept(\"bad\");return xs.get(0);}}",
            null, ""
        },
        {
            "reference_inherited_write_bad",
            "import java.util.*;import java.util.function.*;public class Case {static class Names extends ArrayList<St"
            + "ring>{}public static Object run(){Names xs=new Names();Consumer<Integer> f=xs::add;f.accept(7);return xs."
            + "get(0);}}",
            null, ""
        },
        {
            "reference_inherited_write_ok",
            "import java.util.*;import java.util.function.*;public class Case {static class Names extends ArrayList<St"
            + "ring>{}public static Object run(){Names xs=new Names();Consumer<String> f=xs::add;f.accept(\"ok\");return x"
            + "s.get(0);}}",
            "ok", ""
        },
        {
            "reference_raw_write_ok",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){List xs=new "
            + "ArrayList();Consumer<Integer> f=xs::add;f.accept(7);return xs.get(0);}}",
            "7", ""
        },
        {
            "reference_wildcard_function_target",
            "import java.util.*;import java.util.function.*;public class Case {public static Object run(){Function<? s"
            + "uper String,? extends Integer> f=String::length;return f.apply(\"hello\");}}",
            "5", ""
        },
        {
            "reference_overload_genuinely_ambiguous",
            "import java.util.*;import java.util.function.*;public class Case {interface A{String get(String x);}inter"
            + "face B{String get(String x);}static String pick(A f){return \"A\";}static String pick(B f){return \"B\";}publ"
            + "ic static Object run(){return pick(String::trim);}}",
            null, ""
        },
        {
            "reference_overload_inexact_ambiguous",
            "import java.util.*;import java.util.function.*;public class Case {interface A{String get(String x);}inter"
            + "face B{String get(Integer x);}static String f(String x){return \"s\";}static String f(Integer x){return \"i\""
            + ";}static String pick(A f){return \"A\";}static String pick(B f){return \"B\";}public static Object run(){retu"
            + "rn pick(Case::f);}}",
            null, ""
        },
        {
            "reference_overload_inexact_other_arity",
            "import java.util.function.*;public class Case {interface P{int get(String x);}"
            + "interface B{Integer get(String x);}static String pick(P f){return \"P\";}"
            + "static String pick(B f){return \"B\";}public static Object run(){return pick(Integer::parseInt);}}",
            null, ""
        },
        {
            "reference_overload_primitive_result",
            "import java.util.*;import java.util.function.*;public class Case {static int number(String x){return 1;}interface P{int get(String x);}interfac"
            + "e B{Integer get(String x);}static String pick(P f){return \"P\";}static String pick(B f){return \"B\";}public"
            + " static Object run(){return pick(Case::number);}}",
            "P", ""
        },
        {
            "reference_overload_value_void",
            "import java.util.*;import java.util.function.*;public class Case {static String pick(Consumer<String> f){"
            + "return \"void\";}static String pick(Function<String,String> f){return \"value\";}public static Object run(){r"
            + "eturn pick(String::trim);}}",
            "value", ""
        },
        {
            "reference_generic_overload_preference",
            "import java.util.*;import java.util.function.*;public class Case {static String id(String x){return \"stri"
            + "ng\";}static <T>T id(T x){return x;}public static String run(){Function<String,String> f=Case::id;return f"
            + ".apply(\"generic\");}}",
            "string", ""
        },
        {
            "generic_constraints_class_variable",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T make(Class<T> c)throws Exce"
            + "ption{return c.newInstance();}public static String run()throws Exception{Class<StringBuilder> c=StringBui"
            + "lder.class;return make(c).append(\"ok\").toString();}}",
            "ok", ""
        },
        {
            "generic_constraints_class_and_subtype",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T first(Class<T> type,T x){re"
            + "turn x;}public static String run(){CharSequence x=first(CharSequence.class,\"ok\");return x.toString();}}",
            "ok", ""
        },
        {
            "generic_constraints_invariant_conflict",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T first(Class<T> type,T x){re"
            + "turn x;}public static Object run(){return first(Integer.class,\"bad\");}}",
            null, ""
        },
        {
            "reference_boundary_exact_reference_outer_overload",
            "import java.util.*;import java.util.function.*;public class Case {interface S{int get(String x);}interfac"
            + "e N{int get(Integer x);}static String pick(S f){return \"S\";}static String pick(N f){return \"N\";}public st"
            + "atic String run(){return pick(String::length);}}",
            "S", ""
        },
        {
            "reference_boundary_return_specific_outer_overload",
            "import java.util.*;import java.util.function.*;public class Case {interface S{String get();}interface O{O"
            + "bject get();}static String text(){return \"ok\";}static String pick(S f){return \"S\";}static String pick(O f"
            + "){return \"O\";}public static String run(){return pick(Case::text);}}",
            "S", ""
        },
        {
            "reference_boundary_return_specific_cast",
            "import java.util.*;import java.util.function.*;public class Case {interface S{String get();}interface O{O"
            + "bject get();}static String text(){return \"ok\";}static String pick(S f){return \"S\";}static String pick(O f"
            + "){return \"O\";}public static String run(){return pick((S)Case::text);}}",
            "S", ""
        },
        {
            "reference_boundary_generic_referenced_method",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T id(T x){return x;}public st"
            + "atic String run(){Function<String,String> f=Case::id;return f.apply(\"ok\");}}",
            "ok", ""
        },
        {
            "reference_boundary_static_reference_inference",
            "import java.util.*;import java.util.function.*;public class Case {static <T,R>R map(T t,Function<T,R> f){"
            + "return f.apply(t);}static String show(Integer i){return \"n\"+i;}public static String run(){return map(3,Ca"
            + "se::show).toUpperCase();}}",
            "N3", ""
        },
        {
            "reference_boundary_static_reference_typed_local",
            "import java.util.*;import java.util.function.*;public class Case {static <T,R>R map(T t,Function<T,R> f){"
            + "return f.apply(t);}static String show(Integer i){return \"n\"+i;}public static String run(){Function<Intege"
            + "r,String> f=Case::show;return map(3,f).toUpperCase();}}",
            "N3", ""
        },
        {
            "reference_boundary_binary_reference_inference",
            "import java.util.*;import java.util.function.*;public class Case {static <A,B,R>BiFunction<A,B,R> ground("
            + "BiFunction<A,B,R> f){return f;}public static String run(){BiFunction<String,Integer,String> f=ground(Stri"
            + "ng::substring);return f.apply(\"hello\",2);}}",
            "llo", ""
        },
        {
            "reference_boundary_binary_reference_typed_local",
            "import java.util.*;import java.util.function.*;public class Case {public static String run(){BiFunction<S"
            + "tring,Integer,String> f=String::substring;return f.apply(\"hello\",2);}}",
            "llo", ""
        },
        {
            "reference_boundary_wildcard_constraint_merge",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T read(List<? extends T> xs){"
            + "return xs.get(0);}public static String run(){String s=read(Arrays.asList(\"ok\"));return s;}}",
            "ok", ""
        },
        {
            "reference_boundary_different_constraints_merge",
            "import java.util.*;import java.util.function.*;public class Case {static <T>T first(T a,T b){return a;}pu"
            + "blic static String run(){CharSequence s=first(\"ok\",new StringBuilder(\"b\"));return s.toString();}}",
            "ok", ""
        },
        {
            "reference_boundary_generic_receiver_wrong_write",
            "import java.util.*;import java.util.function.*;public class Case {public static String run(){List<String>"
            + " xs=new ArrayList<String>();Consumer<Integer> f=xs::add;f.accept(1);return xs.get(0);}}",
            null, ""
        },
        {
            "generic_class_exception_only_method_signature",
            "public class Case {static class G<E extends Exception>{public void fail()throws E{}}"
            + "public static String run()throws Exception{return G.class.getMethod(\"fail\")"
            + ".getGenericExceptionTypes()[0].getTypeName();}}",
            "E", ""
        },
        {
            "method_self_referential_interface_bound_signature",
            "public class Case {public static <T extends Comparable<T>> T id(T v){return v;}"
            + "public static String run()throws Exception{return Case.class.getMethod(\"id\",Comparable.class)"
            + ".getTypeParameters()[0].getBounds()[0].getTypeName();}}",
            "java.lang.Comparable<T>", ""
        },
        {
            "method_type_parameter_signature_bounds_and_varargs",
            "public class Case {public static <T extends Number & Comparable<T>> T id(T v,String... rest){return v;}"
            + "public static String run()throws Exception{java.lang.reflect.Method m=Case.class.getMethod(\"id\",Number.class,String[].class);"
            + "return m.getTypeParameters().length+\":\"+m.getTypeParameters()[0].getBounds()[1].getTypeName()"
            + "+\":\"+m.getGenericReturnType().getTypeName()+\":\"+m.getGenericParameterTypes()[0].getTypeName();}}",
            "1:java.lang.Comparable<T>:T:T", ""
        },
        {
            "generic_method_exception_signature",
            "public class Case {public static <E extends Exception> void fail(E e)throws E{throw e;}"
            + "public static String run()throws Exception{return Case.class.getMethod(\"fail\",Exception.class)"
            + ".getGenericExceptionTypes()[0].getTypeName();}}",
            "E", ""
        },
        {
            "member_interface_implicit_static_and_public_metadata",
            "public class Case {interface Outer {interface Inner {}}public static String run(){"
            + "return java.lang.reflect.Modifier.isStatic(Outer.class.getModifiers())+\":\""
            + "+java.lang.reflect.Modifier.isPublic(Outer.Inner.class.getModifiers())+\":\""
            + "+(Outer.Inner.class.getDeclaringClass()==Outer.class);}}",
            "true:true:true", ""
        },
        {
            "enum_implicit_generic_superclass_signature",
            "public class Case {enum E {A}public static String run()throws Exception{return E.class.getGenericSuperclass().getTypeName()"
            + "+\":\"+java.lang.reflect.Modifier.isStatic(E.class.getModifiers())+\":\"+E.class.getField(\"A\").isEnumConstant();}}",
            "java.lang.Enum<Case$E>:true:true", ""
        },
        {
            "captured_local_super_constructor_argument",
            "public class Case {static class B {String value;B(String s){value=s;}}"
            + "public static String run(){final String v=new String(\"ok\");"
            + "class C extends B {C(){super(v);}String get(){return value+v;}}return new C().get();}}",
            "okok", ""
        },
        {
            "captured_wide_local_this_constructor_argument",
            "public class Case {static class B {long value;B(long n){value=n;}}"
            + "public static long run(){final long v=Long.parseLong(\"42\");"
            + "class C extends B {C(){this(v);}C(long n){super(n);}long get(){return value+v;}}return new C().get();}}",
            "84", ""
        },
        {
            "do_loop_first_entry_uninitialized_local",
            "public class Case {public static String run(){String s;int n=0;"
            + "do{s=\"v\"+(++n);}while(n<2);return s;}}",
            "v2", ""
        },
        {
            "do_loop_backedge_reference_and_wide_locals",
            "public class Case {public static String run(){String s;long v;int n=0;"
            + "do{s=\"v\";v=++n;}while(v<2L);return s+v;}}",
            "v2", ""
        },
        {
            "final_local_constant_case_labels",
            "public class Case {interface I{int A=1<<3,B=1;}public static String run(){"
            + "final int bits=I.A|I.B;final char c='/';final String s=\"o\"+\"k\";switch(9){"
            + "case bits:switch(\"ok\"){case s:return \"\"+c;default:return \"bad\";}default:return \"bad\";}}}",
            "/", ""
        },
        {
            "mutable_local_not_constant_case_label",
            "public class Case {public static int run(){int bits=9;switch(9){case bits:return 1;default:return 0;}}}",
            null, "Value of 'case' label"
        },
        {
            "boxed_final_local_not_constant_case_label",
            "public class Case {public static int run(){final Integer bits=9;switch(9){case bits:return 1;default:return 0;}}}",
            null, "Value of 'case' label"
        },
        {
            "switch_selector_assignment_local",
            "public class Case {public static String run(){char c;switch(c=\"Z\".charAt(0)){"
            + "case 'Z':case 'C':return \"\"+c;default:return \"bad\";}}}",
            "Z", ""
        },
        {
            "switch_string_selector_assignment_local",
            "public class Case {public static String run(){String s;switch(s=new String(\"ok\")){"
            + "case \"ok\":return s;default:return \"bad\";}}}",
            "ok", ""
        },
        {
            "interface_fields_implicitly_constant",
            "public class Case {interface I{int VALUE=1<<3;char SEP='/';}public static String run(){"
            + "switch(8){case I.VALUE:return \"\"+I.SEP;default:return \"bad\";}}}",
            "/", ""
        },
        {
            "generic_parameterized_argument_return",
            "public class Case {static <T>T read(java.util.List<T> a){return a.get(0);}"
            + "public static String run(){java.util.List<String> a=java.util.Collections.singletonList(\"ok\");return read(a);}}",
            "ok", ""
        },
        {
            "generic_class_variable_argument_return",
            "public class Case {static <T>T make(Class<T> c)throws Exception{return c.newInstance();}"
            + "public static String run()throws Exception{Class<StringBuilder> c=StringBuilder.class;"
            + "return make(c).append(\"ok\").toString();}}",
            "ok", ""
        },
        {
            "interface_member_class_implicitly_static",
            "public class Case {interface I{final class N{int n;N(int n){this.n=n;}}}"
            + "public static String run(){int m=I.N.class.getModifiers();return new I.N(7).n+\":\""
            + "+java.lang.reflect.Modifier.isPublic(m)+\":\"+java.lang.reflect.Modifier.isStatic(m);}}",
            "7:true:true", ""
        },
        {
            "functional_constructor_overload_filter",
            "public class Case {static class C{String s;C(int n){s=\"bad\";}C(Runnable r){s=\"ok\";r.run();}}"
            + "public static String run(){return new C(()->{}).s;}}",
            "ok", ""
        },
        {
            "shift_constant_labels_and_distance_mask",
            "public class Case {static final int A=1<<3,B=-1>>>30;static final long C=1L<<65;static final char D='/';"
            + "public static String run(){switch(8){case A:return B+\":\"+C+\":\"+D;default:return \"bad\";}}}",
            "3:2:/", ""
        },
        {
            "shift_constant_signed_and_unsigned",
            "public class Case {static final int A=-8>>1,B=1<<33;static final long C=-1L>>>63;"
            + "public static String run(){return A+\":\"+B+\":\"+C;}}",
            "-4:2:1", ""
        },
        {
            "while_short_circuit_assignment_true_edge",
            "public class Case {public static int run(){int off=0,n;"
            + "while(off<3&&(n=1)>=0){off+=n;}return off;}}",
            "3", ""
        },
        {
            "while_or_assignment_true_edge",
            "public class Case {public static int run(){int off=0;long n;"
            + "while((n=off+1)>4||off<3){off=(int)n;}return off;}}",
            "3", ""
        },
        {
            "array_comparator_method_type_inference",
            "public class Case {static class P{int n;P(int n){this.n=n;}}public static String run(){"
            + "P[] a={new P(2),new P(1)};java.util.Arrays.sort(a,(x,y)->x.n-y.n);"
            + "return a[0].n+\":\"+a[1].n;}}",
            "1:2", ""
        },
        {
            "bound_expression_captured_once",
            "public class Case {static String s=\"first\";static int count;static String make(){count++;return s;}"
            + "public static String run(){java.util.function.Supplier<String> f=make()::toString;"
            + "s=\"second\";return f.get()+\":\"+count;}}",
            "first:1", ""
        },
        {
            "bound_field_value_at_creation",
            "public class Case {static class H{String s=\"first\";}static H h=new H();public static String run(){"
            + "java.util.function.Supplier<String> f=h.s::toString;h.s=\"second\";h=new H();return f.get();}}",
            "first", ""
        },
        {
            "bound_mutable_local_value_at_creation",
            "public class Case {public static String run(){String s=\"first\";"
            + "java.util.function.Supplier<String> f=s::toString;s=\"second\";return f.get();}}",
            "first", ""
        },
        {
            "bound_expression_null_evaluation_order",
            "public class Case {static StringBuilder b=new StringBuilder();static String make(){b.append(\"R\");return null;}"
            + "static int arg(){b.append(\"A\");return 1;}static void take(java.util.function.Supplier<String> f,int a){}"
            + "public static String run(){try{take(make()::toString,arg());}catch(NullPointerException e){b.append(\"N\");}"
            + "return b.toString();}}",
            "RN", ""
        },
        {
            "wildcard_upper_bound_read",
            "public class Case {static String read(java.util.Iterator<? extends String> i){return i.next();}"
            + "public static String run(){return read(java.util.Collections.singletonList(\"ok\").iterator());}}",
            "ok", ""
        },
        {
            "wildcard_lower_bound_read",
            "public class Case {public static Object run(){java.util.List<? super String> a="
            + "new java.util.ArrayList<Object>();a.add(\"ok\");Object b=a.get(0);return b;}}",
            "ok", ""
        },
        {
            "wildcard_unbounded_read",
            "public class Case {public static Object run(){java.util.List<?> a="
            + "java.util.Collections.singletonList(\"ok\");Object b=a.get(0);return b;}}",
            "ok", ""
        },
        {
            "wildcard_lower_bound_not_string",
            "public class Case {static String read(java.util.List<? super String> a){return a.get(0);}"
            + "public static Object run(){return null;}}",
            null, "Assignment conversion not possible"
        },
        {
            "array_clone_typed_shallow_copy",
            "public class Case {public static String run(){float[] a={1,2};float[] b=a.clone();b[0]=7;"
            + "String[][] c={{\"ok\"}};String[][] d=c.clone();"
            + "return a[0]+\":\"+b[0]+\":\"+(c!=d)+\":\"+(c[0]==d[0]);}}",
            "1.0:7.0:true:true", ""
        },
        {
            "nested_reflective_varargs",
            "public class Case {public static String run()throws Exception{"
            + "java.lang.reflect.Method value=String.class.getMethod(\"toString\");"
            + "return (String)String.class.getMethod(\"concat\",String.class)"
            + ".invoke(value.invoke(\"a\"),new Object[]{\"b\"});}}",
            "ab", ""
        },
        {
            "shared_varargs_fixed_and_expanded",
            "public class Case {static Case make(Object... a){return new Case();}"
            + "String finish(Object... a){return a.length+\":\"+a[0];}"
            + "public static String run(){return make().finish(new Object[]{\"ok\"});}}",
            "1:ok", ""
        },
        {
            "receiver_chain_static_order",
            "public class Case {static StringBuilder trace=new StringBuilder();static Case make(){trace.append(\"M\");re"
            + "turn null;}static int arg(){trace.append(\"A\");return 1;}static Case next(int n){trace.append(\"S\");return "
            + "new Case();}Case last(int n){trace.append(\"I\");return this;}public static String run(){make().next(arg())"
            + ".last(arg());return trace.toString();}}",
            "MASAI", ""
        },
        {
            "receiver_chain_null_order",
            "public class Case {static StringBuilder trace=new StringBuilder();static Case make(){trace.append(\"M\");re"
            + "turn null;}static int arg(){trace.append(\"A\");return 1;}Case last(int n){trace.append(\"wrong\");return thi"
            + "s;}public static String run(){try{make().last(arg());}catch(NullPointerException e){trace.append(\"N\");}re"
            + "turn trace.toString();}}",
            "MAN", ""
        },
        {
            "receiver_chain_generic",
            "public class Case {public static String run(){return java.util.Collections.singletonList(\"ok\").iterator()"
            + ".next().toUpperCase();}}",
            "OK", ""
        },
        {
            "local_initializer_wide_capture",
            "public class Case {public static long run(){final long n=1234567890123L;class A{long value=n;A(){this(7);"
            + "}A(int x){value+=x;}}return new A().value;}}",
            "1234567890130", ""
        },
        {
            "return_through_finally_catch",
            "public class Case {static String go(){try{return \"saved\";}finally{try{throw new Exception();}catch(Except"
            + "ion e){}}}public static String run(){return go();}}",
            "saved", ""
        },
        {
            "return_long_through_finally_catch",
            "public class Case {static long go(){try{return 1234567890123L;}finally{try{throw new Exception();}catch(E"
            + "xception e){}}}public static long run(){return go();}}",
            "1234567890123", ""
        },
        {
            "return_double_through_finally_catch",
            "public class Case {static double go(){try{return 2.5;}finally{try{throw new Exception();}catch(Exception "
            + "e){}}}public static double run(){return go();}}",
            "2.5", ""
        },
        {
            "return_finally_override",
            "public class Case {static int go(){try{return 1;}finally{try{throw new Exception();}catch(Exception e){re"
            + "turn 2;}}}public static int run(){return go();}}",
            "2", ""
        },
        {
            "return_finally_mutates_local",
            "public class Case {static int go(){int n=1;try{return n++;}finally{try{n=9;}catch(RuntimeException e){n=1"
            + "0;}}}public static int run(){return go();}}",
            "1", ""
        },
        {
            "return_finally_rethrows",
            "public class Case {static String go(){try{return \"wrong\";}finally{throw new IllegalStateException(\"expect"
            + "ed\");}}public static String run(){try{return go();}catch(IllegalStateException e){return e.getMessage();}"
            + "}}",
            "expected", ""
        },
        {
            "long_receiver_chain",
            "public class Case {public static int run(){return new StringBuilder().append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").appen"
            + "d(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").ap"
            + "pend(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\")"
            + ".append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\"x\").append(\""
            + "x\").append(\"x\").append(\"x\").append(\"x\").length();}}",
            "260", ""
        },
        {
            "primitive_class_parenthesized_expression",
            "public class Case {public static String run(){return \"\"+(int.class==Integer.TYPE)+\":\"+(int[].class.getCom"
            + "ponentType()==int.class);}}",
            "true:true", ""
        },
        {
            "primitive_cast_unary",
            "public class Case {public static String run(){int n=3;int[] a=(int[])new int[]{7};return (int)-n+\":\"+(int"
            + ")+n+\":\"+(int)~n+\":\"+a[0];}}",
            "-3:3:-4:7", ""
        },
        {
            "reference_conflicting_receiver_rejected",
            "public class Case {public static class Pear{public String getName(){return \"ok\";}}static <T,R> void take("
            + "T x,T y,java.util.function.Function<? super T,? extends R> f){}public static String run(){take(new Pear()"
            + ",new Object(),Pear::getName);return \"wrong\";}}",
            null, ""
        },
        {
            "class_literal_parenthesized",
            "public class Case {static <T> T make(Class<T> c){return null;}static String pick(Object x){return \"object"
            + "\";}static String pick(String x){return \"string\";}public static String run(){return pick(make(((String.cla"
            + "ss))));}}",
            "string", ""
        },
        {
            "class_literal_annotation_parenthesized",
            "public class Case {@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) public @"
            + "interface A{String value();}@A(\"ok\") public static class Marked{}public static String run(){return Marked"
            + ".class.getAnnotation(((A.class))).value();}}",
            "ok", ""
        },
        {
            "class_literal_all_wrappers",
            "public class Case {static <T> T make(Class<T> c){return null;}static String pick(Object x){return \"wrong\""
            + ";}static String pick(Boolean x){return \"Z\";}static String pick(Byte x){return \"B\";}static String pick(Sho"
            + "rt x){return \"S\";}static String pick(Character x){return \"C\";}static String pick(Integer x){return \"I\";}s"
            + "tatic String pick(Long x){return \"J\";}static String pick(Float x){return \"F\";}static String pick(Double x"
            + "){return \"D\";}static String pick(Void x){return \"V\";}public static String run(){return pick(make(boolean."
            + "class))+pick(make(byte.class))+pick(make(short.class))+pick(make(char.class))+pick(make(int.class))+pick("
            + "make(long.class))+pick(make(float.class))+pick(make(double.class))+pick(make(void.class));}}",
            "ZBSCIJFDV", ""
        },
        {
            "class_literal_primitive_box",
            "public class Case {static <T> T value(Class<T> c,T x){return x;}public static int run(){return value((int"
            + ".class),7).intValue();}}",
            "7", ""
        },
        {
            "class_literal_void_box",
            "public class Case {static <T> T value(Class<T> c,T x){return x;}public static String run(){Void v=value(("
            + "void.class),null);return v==null?\"void\":\"wrong\";}}",
            "void", ""
        },
        {
            "class_literal_array_overload",
            "public class Case {static <T> T make(Class<T> c){return null;}static String pick(Object x){return \"wrong\""
            + ";}static String pick(String[] x){return \"ref\";}static String pick(int[] x){return \"primitive\";}public sta"
            + "tic String run(){return pick(make((String[].class)))+\":\"+pick(make((int[].class)));}}",
            "ref:primitive", ""
        },
        {
            "local_class_field_initializer",
            "public class Case {public static String run(){class A {Object lock=new Object();int count=3;{count+=2;}in"
            + "t value(){return this.count;}}A a=new A();return a.value()+\":\"+(a.lock!=null);}}",
            "5:true", ""
        },
        {
            "local_class_enclosing_instance",
            "public class Case {int base=7;String go(final int n){class A{int value=base+n;}return \"\"+new A().value;}p"
            + "ublic static String run(){return new Case().go(4);}}",
            "11", ""
        },
        {
            "local_class_initializer_capture_only",
            "public class Case {public static String run(){final String name=\"ok\";class A{String value=name;String get"
            + "(){return value;}}return new A().get();}}",
            "ok", ""
        },
        {
            "local_class_delegating_constructor",
            "public class Case {public static String run(){final int n=4;class A{int value=n;A(){this(3);}A(int x){val"
            + "ue+=x;}}return \"\"+new A().value;}}",
            "7", ""
        },
        {
            "local_class_initializer_once",
            "public class Case {static int count;static int next(){return ++count;}public static String run(){class A{"
            + "int value=next();A(){this(0);}A(int ignored){}}A a=new A();A b=new A();return a.value+\":\"+b.value+\":\"+cou"
            + "nt;}}",
            "1:2:2", ""
        },
        {
            "anonymous_capture_parameterized",
            "public class Case {public static int run(){final java.util.concurrent.atomic.AtomicReference<Integer> ar="
            + "new java.util.concurrent.atomic.AtomicReference<Integer>(0);Runnable r=new Runnable(){public void run(){a"
            + "r.set(ar.get()+1);}};r.run();r.run();return ar.get();}}",
            "2", ""
        },
        {
            "local_capture_parameterized",
            "public class Case {public static String run(){final java.util.List<String> values=java.util.Arrays.asList"
            + "(\"ok\");class A{String get(){return values.get(0).toUpperCase();}}return new A().get();}}",
            "OK", ""
        },
        {
            "nested_capture_parameterized",
            "public class Case {public static String run(){final java.util.List<String> values=java.util.Arrays.asList"
            + "(\"ok\");java.util.concurrent.Callable<String> f=new java.util.concurrent.Callable<String>(){public String "
            + "call()throws Exception{java.util.concurrent.Callable<String> g=new java.util.concurrent.Callable<String>("
            + "){public String call(){return values.get(0).toUpperCase();}};return g.call();}};try{return f.call();}catc"
            + "h(Exception e){throw new RuntimeException(e);}}}",
            "OK", ""
        },
        {
            "comparing_unbound_reference",
            "public class Case {public static class Pear{String name;Pear(String n){name=n;}public String getName(){re"
            + "turn name;}}public static String run(){Pear[] a={new Pear(\"B\"),new Pear(\"A\")};java.util.Arrays.sort(a,jav"
            + "a.util.Comparator.comparing(Pear::getName));return a[0].name+\":\"+a[1].name;}}",
            "A:B", ""
        },
        {
            "comparing_primitive_reference",
            "public class Case {public static class Pear{int size;Pear(int n){size=n;}public int getSize(){return size"
            + ";}}public static String run(){Pear[] a={new Pear(2),new Pear(1)};java.util.Arrays.sort(a,java.util.Compar"
            + "ator.comparing(Pear::getSize));return a[0].size+\":\"+a[1].size;}}",
            "1:2", ""
        },
        {
            "source_wildcard_reference",
            "public class Case {public static class Pear{public String getName(){return \"ok\";}}static <T,R> java.util."
            + "function.Function<T,R> ground(java.util.function.Function<? super T,? extends R> f){return (java.util.fun"
            + "ction.Function<T,R>)f;}public static String run(){return ground(Pear::getName).apply(new Pear()).toUpperC"
            + "ase();}}",
            "OK", ""
        },
        {
            "reference_known_receiver_rejected",
            "public class Case {public static class Pear{public String getName(){return \"ok\";}}static <T> void take(T "
            + "x,java.util.function.Function<? super T,?> f){}public static String run(){take(new Object(),Pear::getName"
            + ");return \"wrong\";}}",
            null, ""
        },
        {
            "reference_object_receiver_rejected",
            "public class Case {public static class Pear{public String getName(){return \"ok\";}}public static String ru"
            + "n(){java.util.function.Function<Object,String> f=Pear::getName;return f.apply(new Object());}}",
            null, ""
        },
        {
            "wildcard_super_read_object",
            "public class Case {static java.util.List<? super String> values(){java.util.List<Object> list=new java.ut"
            + "il.ArrayList<Object>();list.add(123);return list;}static String pick(Object x){return \"object\";}static St"
            + "ring pick(String x){return \"string\";}public static String run(){return pick(values().get(0));}}",
            "object", ""
        },
        {
            "wildcard_extends_read_bound",
            "public class Case {static java.util.List<? extends CharSequence> values(){return java.util.Arrays.asList("
            + "\"hello\");}public static int run(){return values().get(0).length();}}",
            "5", ""
        },
        {
            "single_catch_mutable",
            "public class Case {public static Object run(){try{throw new IllegalArgumentException(\"first\");}catch(Runt"
            + "imeException e){e=new IllegalArgumentException(\"second\");return e.getMessage();}}}",
            "second", ""
        },
        {
            "blank_final_branch",
            "public class Case {public static Object run(){final int n;if(System.nanoTime()>0){n=1;}else{n=2;}return n"
            + ";}}",
            "1", ""
        },
        {
            "final_assigned_init",
            "public class Case {public static Object run(){final Object x=new Object();x=new Object();return x;}}",
            null, "final"
        },
        {
            "final_assignment_expression",
            "public class Case {public static Object run(){final int x=1;return x=2;}}",
            null, "final"
        },
        {
            "catch_assignment_expression",
            "public class Case {public static Object run(){try{throw new IllegalArgumentException();}catch(IllegalArgu"
            + "mentException|IllegalStateException e){return (e=null);}}}",
            null, "final"
        },
        {
            "lambda_checked_throw",
            "public class Case {public static Object run()throws Exception{java.util.concurrent.Callable<String> f=()-"
            + ">{throw new java.io.IOException(\"expected\");};try{return f.call();}catch(java.io.IOException e){return e."
            + "getMessage();}}}",
            "expected", ""
        },
        {
            "lambda_capture_simple",
            "public class Case {public static Object run(){final int n=7;java.util.function.IntSupplier f=()->n+1;retu"
            + "rn f.getAsInt();}}",
            "8", ""
        },
        {
            "lambda_capture_block",
            "public class Case {public static Object run(){final int n=7;java.util.function.IntSupplier f=()->{int y=2"
            + ";return n+y;};return f.getAsInt();}}",
            "9", ""
        },
        {
            "lambda_nested_capture",
            "public class Case {interface Factory{java.util.function.IntSupplier make();}public static Object run(){fi"
            + "nal int n=7;Factory f=()->()->n;return f.make().getAsInt();}}",
            "7", ""
        },
        {
            "lambda_nested_methodref_capture",
            "public class Case {interface Factory{java.util.function.Supplier<String> make();}public static Object run"
            + "(){final String s=\"ok\";Factory f=()->s::trim;return f.make().get();}}",
            "ok", ""
        },
        {
            "lambda_capture_generic",
            "public class Case {public static Object run(){final java.util.List<String> xs=new java.util.ArrayList<Str"
            + "ing>();xs.add(\"ok\");java.util.function.Supplier<String> f=()->xs.get(0).toUpperCase();return f.get();}}",
            "OK", ""
        },
        {
            "lambda_local_anon_capture",
            "public class Case {public static Object run(){final int x=7;java.util.function.IntSupplier f=()->new java"
            + ".util.function.IntSupplier(){public int getAsInt(){return x;}}.getAsInt();return f.getAsInt();}}",
            "7", ""
        },
        {
            "lambda_synthetic_collision",
            "public class Case {static int lambda$1(){return 99;}public static Object run(){java.util.function.IntSupp"
            + "lier f=()->1;return f.getAsInt();}}",
            "1", ""
        },
        {
            "lambda_interface_context",
            "public class Case {interface Factory{static java.util.function.IntSupplier make(){return ()->7;}}public s"
            + "tatic Object run(){return Factory.make().getAsInt();}}",
            "7", ""
        },
        {
            "lambda_wrong_param_narrow",
            "public class Case {public static Object run(){java.util.function.Function<Number,String> f=(Integer n)->n"
            + ".toString();return f.apply(Double.valueOf(2));}}",
            null, ""
        },
        {
            "lambda_wrong_param_wide",
            "public class Case {public static Object run(){java.util.function.Function<String,String> f=(Object s)->s."
            + "toString();return f.apply(\"ok\");}}",
            null, ""
        },
        {
            "lambda_wrong_primitive_param",
            "public class Case {public static Object run(){java.util.function.IntUnaryOperator f=(long x)->1;return f."
            + "applyAsInt(2);}}",
            null, ""
        },
        {
            "methodref_generic_object",
            "public class Case {static String f(Object x){return \"Object\";}static String f(String x){return \"String\";}"
            + "public static Object run(){java.util.function.Function<Object,String> f=Case::f;return f.apply(new Object"
            + "());}}",
            "Object", ""
        },
        {
            "methodref_ambiguous",
            "public class Case {static String f(java.io.Serializable x){return \"S\";}static String f(CharSequence x){re"
            + "turn \"C\";}public static Object run(){java.util.function.Function<String,String> f=Case::f;return f.apply("
            + "\"x\");}}",
            null, ""
        },
        {
            "methodref_phase",
            "public class Case {static String f(long x){return \"long\";}static String f(Integer x){return \"Integer\";}pu"
            + "blic static Object run(){java.util.function.IntFunction<String> f=Case::f;return f.apply(1);}}",
            "long", ""
        },
        {
            "methodref_unbox_widen",
            "public class Case {static long f(long x){return x;}public static Object run(){java.util.function.Function"
            + "<Integer,Long> f=Case::f;return f.apply(2);}}",
            "2", ""
        },
        {
            "methodref_wrong_receiver",
            "public class Case {public static Object run(){java.util.function.Function<Integer,Integer> f=String::leng"
            + "th;return f.apply(2);}}",
            null, ""
        },
        {
            "sam_inherited_covariant",
            "public class Case {public static Object run(){org.codehaus.commons.compiler.tests.FeatureUpgradeRegressio"
            + "nTest.NumberC s=()->Integer.valueOf(7);return s.get();}}",
            "7", ""
        },
        {
            "signature_diamond",
            "public class Case {java.util.List<String> xs=new java.util.ArrayList<>();public static Object run(){retur"
            + "n \"ok\";}}",
            "ok", ""
        },
        {
            "signature_typevariable",
            "public class Case {static class G<T>{public T field;}public static Object run()throws Exception{return G."
            + "class.getField(\"field\").getGenericType().getTypeName();}}",
            "T", ""
        },
        {
            "generic_lowercase",
            "public class Case {static class G<t> implements java.util.Iterator<t>{public boolean hasNext(){return fal"
            + "se;}public t next(){return null;}public void remove(){}}public static Object run(){return \"ok\";}}",
            "ok", ""
        },
        {
            "generic_nested_super",
            "public class Case {static class G<T> implements java.util.function.Supplier<java.util.List<T>>{public jav"
            + "a.util.List<T> get(){return null;}}public static Object run(){return \"ok\";}}",
            "ok", ""
        },
        {
            "type_container_collision_arg",
            "import java.lang.annotation.*;import java.util.*;@Retention(RetentionPolicy.RUNTIME)@Target(ElementType.T"
            + "YPE_USE)@Repeatable(As.class)@interface A{} @Retention(RetentionPolicy.RUNTIME)@Target(ElementType.TYPE_U"
            + "SE)@interface As{A[] value();}public class Case{public List<@A @A @As({}) String> xs;public static Object"
            + " run()throws Exception{return ((java.lang.reflect.AnnotatedParameterizedType)Case.class.getField(\"xs\").ge"
            + "tAnnotatedType()).getAnnotatedActualTypeArguments()[0].getAnnotations().length;}}",
            null, ""
        },
        {
            "type_signature_nested",
            "import java.lang.annotation.*;import java.util.*;@Retention(RetentionPolicy.RUNTIME)@Target(ElementType.T"
            + "YPE_USE)@interface A{}public class Case{public Map<String,List<@A String>> xs;public static Object run()t"
            + "hrows Exception{java.lang.reflect.AnnotatedParameterizedType m=(java.lang.reflect.AnnotatedParameterizedT"
            + "ype)Case.class.getField(\"xs\").getAnnotatedType();java.lang.reflect.AnnotatedParameterizedType l=(java.lan"
            + "g.reflect.AnnotatedParameterizedType)m.getAnnotatedActualTypeArguments()[1];return l.getAnnotatedActualTy"
            + "peArguments()[0].getAnnotations().length;}}",
            "1", ""
        },
        {
            "generic_raw_class_variable",
            "public class Case {public static Object run(){org.codehaus.commons.compiler.tests.FeatureUpgradeRegressio"
            + "nTest.G raw=new org.codehaus.commons.compiler.tests.FeatureUpgradeRegressionTest.G(Integer.valueOf(7));re"
            + "turn raw.ignore(\"x\");}}",
            "7", ""
        },
        {
            "final_try_abrupt_catch",
            "public class Case {static void f(boolean c){final int x;try{if(c){x=1;throw new RuntimeException();}}catc"
            + "h(RuntimeException e){x=2;}}public static Object run(){f(true);return 0;}}",
            null, "final"
        },
        {
            "final_try_abrupt_normal",
            "public class Case {static int f(boolean c){final int x;try{if(c){x=1;return x;}}finally{}x=2;return x;}pu"
            + "blic static Object run(){return f(true)+f(false);}}",
            null, "final"
        },
        {
            "final_finally_break",
            "public class Case {public static Object run(){final int x;outer:{try{break outer;}finally{x=1;}}x=2;retur"
            + "n x;}}",
            null, "final"
        },
        {
            "final_label_continue",
            "public class Case {public static Object run(){final int x;outer:for(int i=0;i<3;i++){x=i;continue outer;}"
            + "return 0;}}",
            null, "final"
        },
        {
            "final_initializer_assignment",
            "public class Case {public static Object run(){final int x=(x=1);return x;}}",
            null, "final"
        },
        {
            "methodref_zero_varargs",
            "public class Case {static String f(String... a){return \"String\";}static String f(Object... a){return \"Obj"
            + "ect\";}public static Object run(){java.util.function.Supplier<String> f=Case::f;return f.get();}}",
            "String", ""
        },
        {
            "methodref_box_before_varargs",
            "public class Case {static String f(Integer x){return \"Integer\";}static String f(int... x){return \"array\";"
            + "}public static Object run(){java.util.function.IntFunction<String> f=Case::f;return f.apply(1);}}",
            "Integer", ""
        },
        {
            "lambda_wrong_generic_parameter",
            "public class Case {public static Object run(){java.util.function.Function<java.util.List<String>,String> "
            + "f=(java.util.List<Integer> xs)->xs.get(0).toString();return f.apply(java.util.Arrays.asList(\"a\"));}}",
            null, ""
        },
        {
            "lambda_generic_parameter",
            "public class Case {public static Object run(){java.util.function.Function<java.util.List<String>,String> "
            + "f=xs->xs.get(0).toUpperCase();return f.apply(java.util.Arrays.asList(\"a\"));}}",
            "A", ""
        },
        {
            "final_branch_abrupt",
            "public class Case {static int f(boolean c){final int x;if(c){x=1;return x;}else{x=2;}return x;}public sta"
            + "tic Object run(){return f(true)+f(false);}}",
            "3", "final"
        },
        {
            "final_branch_reassign",
            "public class Case {public static Object run(){final int x;if(System.nanoTime()>0)x=1;x=2;return x;}}",
            null, "final"
        },
        {
            "final_nested_expression",
            "public class Case {public static Object run(){final int x;return (x=1)+2;}}",
            "3", "final"
        },
        {
            "final_double_expression",
            "public class Case {public static Object run(){final int x;return (x=1)+(x=2);}}",
            null, "final"
        },
        {
            "final_conditional",
            "public class Case {static int f(boolean c){final int x;int y=c?(x=1):(x=2);return x+y;}public static Obje"
            + "ct run(){return f(true)+f(false);}}",
            "6", "final"
        },
        {
            "final_switch",
            "public class Case {static int f(int i){final int x;switch(i){case 1:x=1;break;default:x=2;}return x;}publ"
            + "ic static Object run(){return f(1)+f(2);}}",
            "3", "final"
        },
        {
            "final_switch_fallthrough",
            "public class Case {static int f(int i){final int x;switch(i){case 1:x=1;default:x=2;}return x;}public sta"
            + "tic Object run(){return f(1);}}",
            null, "final"
        },
        {
            "final_loop_local",
            "public class Case {public static Object run(){int n=0;for(int i=0;i<3;i++){final int x;x=i;n+=x;}return n"
            + ";}}",
            "3", "final"
        },
        {
            "final_loop_repeat",
            "public class Case {public static Object run(){final int x;for(int i=0;i<3;i++){x=i;}return 0;}}",
            null, "final"
        },
        {
            "final_loop_break",
            "public class Case {public static Object run(){final int x;while(true){x=7;break;}return x;}}",
            "7", "final"
        },
        {
            "final_loop_continue",
            "public class Case {public static Object run(){final int x;for(int i=0;i<3;i++){x=i;continue;}return 0;}}",
            null, "final"
        },
        {
            "final_do_once",
            "public class Case {public static Object run(){final int x;do{x=3;}while(false);return x;}}",
            "3", "final"
        },
        {
            "final_try_catch",
            "public class Case {public static Object run(){final int x;try{x=3;throw new RuntimeException();}catch(Run"
            + "timeException e){x=4;}return x;}}",
            null, "final"
        },
        {
            "final_try_finally",
            "public class Case {public static Object run(){final int x;try{x=3;}finally{x=4;}return x;}}",
            null, "final"
        },
        {
            "final_formal",
            "public class Case {static int f(final int x){return x=2;}public static Object run(){return f(1);}}",
            null, "final"
        },
        {
            "final_single_catch",
            "public class Case {public static Object run(){try{throw new RuntimeException();}catch(final RuntimeExcept"
            + "ion e){return e=null;}}}",
            null, "final"
        },
        {
            "lambda_parameter_mutable",
            "public class Case {public static Object run(){java.util.function.IntUnaryOperator f=x->++x;return f.apply"
            + "AsInt(1);}}",
            "2", ""
        },
        {
            "lambda_nested_parameter",
            "public class Case {public static Object run(){java.util.function.Function<String,java.util.function.Suppl"
            + "ier<String>> f=x->()->x;return f.apply(\"ok\").get();}}",
            "ok", ""
        },
        {
            "lambda_interface_default",
            "public class Case {interface F{default java.util.function.IntSupplier make(){return ()->7;}}public static"
            + " Object run(){return new F(){}.make().getAsInt();}}",
            "7", ""
        },
        {
            "methodref_return_wrong",
            "public class Case {static Integer f(){return 1;}public static Object run(){java.util.function.Supplier<St"
            + "ring> f=Case::f;return f.get();}}",
            null, ""
        },
        {
            "constructor_return_wrong",
            "public class Case {public static Object run(){java.util.function.Supplier<String> f=StringBuilder::new;re"
            + "turn f.get();}}",
            null, ""
        },
        {
            "array_return_wrong",
            "public class Case {public static Object run(){java.util.function.IntFunction<String> f=String[]::new;retu"
            + "rn f.apply(1);}}",
            null, ""
        },
        {
            "methodref_varargs",
            "public class Case {static String f(String... a){return a[0]+a[1];}public static Object run(){java.util.fu"
            + "nction.BiFunction<String,String,String> f=Case::f;return f.apply(\"a\",\"b\");}}",
            "ab", ""
        },
        {
            "sam_throws_intersection",
            "public class Case {interface A{Object get()throws java.io.IOException;}interface B{String get()throws jav"
            + "a.sql.SQLException;}interface C extends A,B{}public static Object run()throws Exception{C c=()->{throw ne"
            + "w java.sql.SQLException();};return c.get();}}",
            null, ""
        },
        {
            "generic_signature_bounds",
            "public class Case {static class G<t extends Number & Comparable<t>>{public t field;}public static Object "
            + "run()throws Exception{return G.class.getTypeParameters()[0].getBounds()[1].getTypeName();}}",
            "java.lang.Comparable<t>", ""
        },
        {
            "generic_method_nested_constraint",
            "public class Case {static <T>T[] cat(T a,T[] b,Class<T> c){return (T[])java.lang.reflect.Array.newInstanc"
            + "e(c,1);}public static Object run(){return cat(\"a\",new Class[0],Object.class).getClass().getComponentType("
            + ").getName();}}",
            "java.lang.Object", ""
        },
        {
            "bound_null_timing",
            "public class Case {public static String run() { final String s=null; try { java.util.function.Supplier<St"
            + "ring> f=s::toString; return \"created\"; } catch(NullPointerException e) { return \"NPE\"; } }}",
            "NPE", ""
        },
        {
            "lambda_object_lookup",
            "public class Case {public String toString(){return \"outer\";} public String go(){ java.util.function.Suppl"
            + "ier<String> f=()->toString(); return f.get();} public static String run(){return new Case().go();}}",
            "outer", ""
        },
        {
            "lambda_sam_lookup",
            "public class Case {public String get(){return \"outer\";} public String go(){java.util.function.Supplier<St"
            + "ring> f=()->get(); return f.get();} public static String run(){return new Case().go();}}",
            "outer", ""
        },
        {
            "field_cast",
            "public class Case {public static String run(){org.codehaus.commons.compiler.tests.FeatureUpgradeRegressio"
            + "nTest.Box<String> b=new org.codehaus.commons.compiler.tests.FeatureUpgradeRegressionTest.Box<String>(\"ok\""
            + "); return b.value;}}",
            "ok", ""
        },
        {
            "method_infer_repeated",
            "public class Case {public static Object run(){return org.codehaus.commons.compiler.tests.FeatureUpgradeRe"
            + "gressionTest.first(\"x\",Integer.valueOf(1));}}",
            "x", ""
        },
        {
            "method_infer_box",
            "public class Case {public static Object run(){return java.util.Optional.of(1).get();}}",
            "1", ""
        },
        {
            "method_shadow",
            "public class Case {public static Object run(){org.codehaus.commons.compiler.tests.FeatureUpgradeRegressio"
            + "nTest.Shadow<String> s=new org.codehaus.commons.compiler.tests.FeatureUpgradeRegressionTest.Shadow<String"
            + ">(); return s.id(Integer.valueOf(1));}}",
            "1", ""
        },
        {
            "lambda_wrong_return",
            "public class Case {public static Object run(){java.util.function.Supplier<String> s=()->Integer.valueOf(1"
            + "); return s.get();}}",
            null, ""
        },
        {
            "lambda_wrong_param",
            "public class Case {public static Object run(){java.util.function.Function<String,String> f=(Integer x)->x"
            + ".toString(); return f.apply(\"x\");}}",
            null, ""
        },
        {
            "source_retention",
            "public class Case {@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE) public @i"
            + "nterface A{} @A public static String run(){return \"ok\";}}",
            "ok", ""
        },
        {
            "source_repeat",
            "public class Case {@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE) @java.lan"
            + "g.annotation.Repeatable(As.class) public @interface A{} @java.lang.annotation.Retention(java.lang.annotat"
            + "ion.RetentionPolicy.SOURCE) public @interface As{A[] value();} @A @A public static String run(){return \"o"
            + "k\";}}",
            "ok", ""
        },
        {
            "repeat_container_collision",
            "public class Case {@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @java.la"
            + "ng.annotation.Repeatable(As.class) public @interface A{} @java.lang.annotation.Retention(java.lang.annota"
            + "tion.RetentionPolicy.RUNTIME) public @interface As{A[] value();} @A @A @As({}) public static String run()"
            + "{return \"ok\";}}",
            null, ""
        },
        {
            "finally_normal",
            "public class Case {public static String run(){StringBuilder b=new StringBuilder(); try {throw new Illegal"
            + "ArgumentException();} catch(IllegalArgumentException e){b.append(\"C\");} finally{b.append(\"F\");} return b."
            + "toString();}}",
            "CF", ""
        },
        {
            "finally_return",
            "public class Case {static StringBuilder b=new StringBuilder(); public static String run(){go();return b.t"
            + "oString();} static int go(){try {throw new IllegalArgumentException();} catch(IllegalArgumentException e)"
            + "{b.append(\"C\");return 1;} finally{b.append(\"F\");}}}",
            "CF", ""
        },
        {
            "type_repeat",
            "public class Case {@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @java.la"
            + "ng.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @java.lang.annotation.Repeatable(As.class"
            + ") public @interface A{} @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @ja"
            + "va.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) public @interface As{A[] value();} p"
            + "ublic @A @A String field; public static Object run()throws Exception{return Case.class.getField(\"field\")."
            + "getAnnotatedType().getAnnotationsByType(A.class).length;}}",
            "2", ""
        },
        {
            "generic_field_receiver",
            "public class Case {public static String run(){org.codehaus.commons.compiler.tests.FeatureUpgradeRegressio"
            + "nTest.Box<String> b=new org.codehaus.commons.compiler.tests.FeatureUpgradeRegressionTest.Box<String>(\"ok\""
            + ");return b.value.toUpperCase();}}",
            "OK", ""
        },
        {
            "lambda_super",
            "public class Case {public Boolean go(){java.util.function.Supplier<Boolean> s=()->super.equals(Case.this)"
            + ";return s.get();} public static Boolean run(){return new Case().go();}}",
            "true", ""
        },
        {
            "lambda_nested",
            "public class Case {public static String run(){java.util.function.Function<String,java.util.function.Suppl"
            + "ier<String>> f=x->()->x;return f.apply(\"ok\").get();}}",
            "ok", ""
        },
        {
            "final_assert_expression",
            "public class Case {public static Object run(){final int x=1;assert (x=2)>0;return x;}}",
            null, "final"
        },
        {
            "final_initializer_block",
            "public class Case {static {final int x=1;int y=(x=2);}public static Object run(){return 0;}}",
            null, "final"
        },
        {
            "methodref_static_unbound_ambiguous",
            "public class Case {static String f(Case c){return \"s\";}String f(){return \"i\";}public static Object run(){"
            + "java.util.function.Function<Case,String> f=Case::f;return f.apply(new Case());}}",
            null, ""
        },
        {
            "constructor_void",
            "public class Case {public static Object run(){Runnable r=StringBuilder::new;r.run();return \"ok\";}}",
            "ok", ""
        },
        {
            "array_void",
            "public class Case {public static Object run(){java.util.function.IntConsumer r=String[]::new;r.accept(1);"
            + "return \"ok\";}}",
            "ok", ""
        }
    };
}
