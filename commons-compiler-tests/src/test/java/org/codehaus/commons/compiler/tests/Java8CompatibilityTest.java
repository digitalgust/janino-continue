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
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.CommonsCompilerTestSuite;
import util.TestUtil;

/** Differential regression cases for the remaining Java 8 compatibility gaps. */
@RunWith(Parameterized.class) public class Java8CompatibilityTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}") public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    public Java8CompatibilityTest(ICompilerFactory factory) throws Exception { super(factory); }
    private void program(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook(source);
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("Case").getMethod("main").invoke(null));
    }
    @Test public void parameter_annotations() throws Exception {
        this.program("import java.lang.annotation.*; public class Case {"
            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) public @interface A {int value();}"
            + "public Case(@A(2) long x){} public void m(@A(3) String a,int b){}"
            + "public static boolean main() throws Exception{return Case.class.getConstructor(long.class)"
            + ".getParameterAnnotations()[0].length==1 && ((A)Case.class.getMethod(\"m\",String.class,int.class)"
            + ".getParameterAnnotations()[0][0]).value()==3;}}");
    }
    @Test public void enclosing_super() throws Exception {
        this.program("public class Case {static class Base{int m(int... x){return x[0]+2;}}"
            + "static class Outer extends Base{int m(int... x){return 99;}class Inner{int run(){return Outer.super.m(3);}}}"
            + "public static boolean main(){return new Outer().new Inner().run()==5;}}");
    }
    @Test public void local_class_metadata() throws Exception {
        this.program("public class Case {public static boolean main(){class Local{} Class c=Local.class;"
            + "return c.getSimpleName().equals(\"Local\") && c.isLocalClass() && c.getEnclosingClass()==Case.class"
            + "&& c.getEnclosingMethod().getName().equals(\"main\");}}");
    }
    @Test public void block_lambda_overload() throws Exception {
        this.program("public class Case{interface A{String get(int n);}interface B{Object get(int n);}"
            + "static String call(A a){return \"A\";}static String call(B b){return \"B\";}"
            + "public static boolean main(){return call((int n)->{if(n==0)return \"a\";return \"b\";}).equals(\"A\");}}");
    }
    @Test public void generic_assignments_legal() throws Exception {
        this.program("import java.util.*;public class Case{public static boolean main(){List<Integer> a=new ArrayList<Integer>();"
            + "List<? extends Number> b=a;List<? super Integer> c=new ArrayList<Number>();c.add(3);"
            + "List raw=a;List<String> unchecked=raw;return b.isEmpty() && c.get(0).equals(3) && unchecked.isEmpty();}}");
    }
    @Test public void reject_generic_assignment() throws Exception {
        this.assertCompilationUnitUncookable("import java.util.*;class Case {void m(){List<String> s=new ArrayList<Integer>();}}");
    }
    @Test public void reject_generic_return() throws Exception {
        this.assertCompilationUnitUncookable("import java.util.*;class Case {List<String> m(){return new ArrayList<Integer>();}}");
    }
    @Test public void reject_generic_bounds() throws Exception {
        this.assertCompilationUnitUncookable("class Case<T extends Number>{Case<String> x;}");
    }
    @Test public void reject_generic_arity() throws Exception {
        this.assertCompilationUnitUncookable("class Case{java.util.List<String,Integer> x;}");
    }
    @Test public void reject_final_override() throws Exception {
        this.assertCompilationUnitUncookable("class Base{final void m(){}}class Case extends Base{void m(){}}");
    }
    @Test public void reject_binary_final_override() throws Exception {
        this.assertCompilationUnitUncookable("class Case{public void wait() {}}");
    }
    @Test public void reject_static_override() throws Exception {
        this.assertCompilationUnitUncookable("class Base{void m(){}}class Case extends Base{static void m(){}}");
    }
    @Test public void reject_weaker_access() throws Exception {
        this.assertCompilationUnitUncookable("class Base{public void m(){}}class Case extends Base{protected void m(){}}");
    }
    @Test public void reject_incompatible_return() throws Exception {
        this.assertCompilationUnitUncookable("class Base{Number m(){return null;}}class Case extends Base{String m(){return null;}}");
    }
    @Test public void reject_incompatible_throws() throws Exception {
        this.assertCompilationUnitUncookable("class Base{void m(){}}class Case extends Base{void m()throws Exception{}}");
    }
    @Test public void private_method_is_not_override() throws Exception {
        this.assertCompilationUnitUncookable("class Base{private void m(){}}class Case extends Base{@Override void m(){}}");
    }
    @Test public void generic_bounds_and_fields() throws Exception {
        this.program("public class Case<T extends Comparable<T>> {T value;Case(T x){value=x;}"
            + "public static boolean main(){Case<String> c=new Case<String>(\"a\");String s=c.value;return s.equals(\"a\");}}");
    }
    @Test public void reject_parameterized_bound() throws Exception {
        this.assertCompilationUnitUncookable("class Case<T extends Comparable<String>>{Case<Integer> value;}");
    }
    @Test public void reject_dependent_bound() throws Exception {
        this.assertCompilationUnitUncookable("class Case<A,B extends A>{Case<Number,String> value;}");
    }
    @Test public void generic_throws_override() throws Exception {
        this.program("public class Case{interface A<E extends Exception>{void m()throws E;}"
            + "static class B implements A<java.io.IOException>{public void m()throws java.io.IOException{}}"
            + "public static boolean main()throws Exception{new B().m();return true;}}");
    }
    @Test public void reject_generic_throws_override() throws Exception {
        this.assertCompilationUnitUncookable("interface A<E extends Exception>{void m()throws E;}"
            + "class Case implements A<java.io.IOException>{public void m()throws Exception{}}");
    }
    @Test public void reject_substituted_return_override() throws Exception {
        this.assertCompilationUnitUncookable("class A<T>{T m(){return null;}}class Case extends A<String>{Object m(){return null;}}");
    }
    @Test public void repeated_parameter_annotations() throws Exception {
        this.program("import java.lang.annotation.*;public class Case {"
            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) @Repeatable(As.class) public @interface A{int value();}"
            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) public @interface As{A[] value();}"
            + "public void m(@A(1) @A(2) long x){}public static boolean main()throws Exception{"
            + "As a=(As)Case.class.getMethod(\"m\",long.class).getParameterAnnotations()[0][0];"
            + "return a.value().length==2 && a.value()[0].value()==1 && a.value()[1].value()==2;}}");
    }
    @Test public void enum_constructor_parameter_annotations() throws Exception {
        this.program("import java.lang.annotation.*;public class Case{"
            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) public @interface A{}"
            + "enum E{X(3);E(@A long n){}}public static boolean main()throws Exception{"
            + "java.lang.annotation.Annotation[][] a=E.class.getDeclaredConstructors()[0].getParameterAnnotations();"
            + "return a[a.length-1].length==1 && a[a.length-1][0] instanceof A;}}");
    }
    @Test public void enclosing_super_generic_and_exceptions() throws Exception {
        this.program("public class Case {static class Base<T>{T value;Base(T t){value=t;}T m()throws Exception{return value;}}"
            + "static class Outer extends Base<String>{Outer(){super(\"ok\");}String m(){return \"wrong\";}"
            + "class Inner{String run()throws Exception{return Outer.super.m();}}}"
            + "public static boolean main()throws Exception{return new Outer().new Inner().run().equals(\"ok\");}}");
    }
    @Test public void block_lambda_primitive_overload() throws Exception {
        this.program("public class Case{interface A{int get(int n);}interface B{Integer get(int n);}"
            + "static int call(A a){return 1;}static int call(B b){return 2;}"
            + "public static boolean main(){return call((int n)->{if(n==0)return 1;return n;})==1;}}");
    }
    @Test public void block_lambda_local_return() throws Exception {
        this.program("public class Case{interface A{String get(int n);}interface B{Object get(int n);}"
            + "static int call(A a){return 1;}static int call(B b){return 2;}"
            + "public static boolean main(){return call((int n)->{String x=\"a\";return x;})==1;}}");
    }
    @Test public void nested_poly_expression() throws Exception {
        this.program("import java.util.*;public class Case{static <T>T id(T x){return x;}"
            + "public static boolean main(){List<String> a=id(Collections.emptyList());return a.isEmpty();}}");
    }
    @Test public void type_parameter_receiver_and_array_annotations() throws Exception {
        this.program("import java.lang.annotation.*;import java.lang.reflect.*;import java.io.Serializable;"
            + "@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE_USE,ElementType.TYPE_PARAMETER}) @interface A{}"
            + "public class Case<@A T extends @A Number> extends @A Object implements @A Serializable{"
            + "String @A [] @A [] field;public <@A U extends @A CharSequence> void m(@A Case<T> this){}"
            + "public static boolean main()throws Exception{Class c=Case.class;"
            + "AnnotatedArrayType a=(AnnotatedArrayType)c.getDeclaredField(\"field\").getAnnotatedType();"
            + "return c.getTypeParameters()[0].getAnnotations().length==1"
            + "&& c.getTypeParameters()[0].getAnnotatedBounds()[0].getAnnotations().length==1"
            + "&& c.getAnnotatedSuperclass().getAnnotations().length==1"
            + "&& c.getAnnotatedInterfaces()[0].getAnnotations().length==1"
            + "&& c.getMethod(\"m\").getAnnotatedReceiverType().getAnnotations().length==1"
            + "&& c.getMethod(\"m\").getTypeParameters()[0].getAnnotations().length==1"
            + "&& a.getAnnotations().length==1 && a.getAnnotatedGenericComponentType().getAnnotations().length==1;}}");
    }
    @Test public void explicit_generic_method_and_reference() throws Exception {
        this.program("public class Case{interface F{String get(String x);}static <Z,A>Z first(Z z,A a){return z;}"
            + "static <T>T id(T x){return x;}public static boolean main(){F f=Case::<String>id;"
            + "return Case.<String,Integer>first(\"ok\",1).equals(\"ok\") && f.get(\"yes\").equals(\"yes\");}}");
    }
    @Test public void explicit_super_method() throws Exception {
        this.program("public class Case{static class Base{<T>T id(T x){return x;}}static class Child extends Base{"
            + "String run(){return super.<String>id(\"ok\");}}public static boolean main(){return new Child().run().equals(\"ok\");}}");
    }
    @Test public void reject_explicit_argument() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static <T>T id(T x){return x;}String x=Case.<String>id(1);}");
    }
    @Test public void reject_explicit_bound() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static <T extends Number>T id(T x){return x;}Object x=Case.<String>id(\"x\");}");
    }
    @Test public void reject_explicit_arity() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static <T>T id(T x){return x;}Object x=Case.<String,Integer>id(\"x\");}");
    }
    @Test public void reject_explicit_reference_bound() throws Exception {
        this.assertCompilationUnitUncookable("class Case{interface F{String get(String x);}static <T extends Number>T id(T x){return x;}F f=Case::<String>id;}");
    }
    @Test public void reject_wrong_receiver() throws Exception {
        this.assertCompilationUnitUncookable("class Case{void m(String this){}}");
    }
    @Test public void reject_static_receiver() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static void m(Case this){}}");
    }
    @Test public void parameter_name_ending_this() throws Exception {
        this.program("public class Case{static int m(int notthis){return notthis;}public static boolean main(){return m(3)==3;}}");
    }

    @Test public void generic_constructor() throws Exception {
        this.program("public class Case{final int value; public <T extends Number>Case(T n){value=n.intValue();}"
            + "public static boolean main()throws Exception{Case a=new Case(7);Case b=new <Integer>Case(8);"
            + "return a.value==7 && b.value==8 && Case.class.getConstructor(Number.class).getTypeParameters()[0].getName().equals(\"T\");}}");
    }
    @Test public void generic_constructor_reference() throws Exception {
        this.program("public class Case{interface F{Case make(Integer n);}int value;<T extends Number>Case(T n){value=n.intValue();}"
            + "public static boolean main(){F f=Case::<Integer>new;return f.make(4).value==4;}}");
    }
    @Test public void generic_constructor_delegation() throws Exception {
        this.program("public class Case{static class Base{int value;<T extends Number>Base(T n){value=n.intValue();}}"
            + "static class Child extends Base{Child(){<Integer>super(6);}}public static boolean main(){return new Child().value==6;}}");
    }
    @Test public void generic_constructor_functional_target() throws Exception {
        this.program("public class Case{interface F<T>{T get();}String value;<T>Case(F<T> f){value=(String)f.get();}"
            + "public static boolean main(){return new <String>Case(()->\"ok\").value.equals(\"ok\");}}");
    }
    @Test public void reject_generic_constructor_bound() throws Exception {
        this.assertCompilationUnitUncookable("class Case{<T extends Number>Case(T n){}Object x=new <String>Case(\"x\");}");
    }
    @Test public void reject_generic_constructor_argument() throws Exception {
        this.assertCompilationUnitUncookable("class Case{<T>Case(T n){}Object x=new <String>Case(3);}");
    }
    @Test public void reject_generic_constructor_arity() throws Exception {
        this.assertCompilationUnitUncookable("class Case{<T>Case(T n){}Object x=new <String,Integer>Case(\"x\");}");
    }
    @Test public void reject_generic_constructor_reference() throws Exception {
        this.assertCompilationUnitUncookable("class Case{interface F{Case make(String n);}<T extends Number>Case(T n){}F f=Case::<String>new;}");
    }

    @Test public void inferred_generic_exception() throws Exception {
        this.program("public class Case{static <E extends Exception>void raise(E e)throws E{throw e;}"
            + "static <E extends Exception>void empty()throws E{}public static boolean main(){empty();"
            + "try{raise(new java.io.IOException());return false;}catch(java.io.IOException e){return true;}}}");
    }
    @Test public void reject_unhandled_generic_exception() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static <E extends Exception>void raise(E e)throws E{throw e;}"
            + "void run(){raise(new java.io.IOException());}}");
    }
    @Test public void reject_explicit_generic_exception() throws Exception {
        this.assertCompilationUnitUncookable("class Case{static <E extends Exception>void empty()throws E{}"
            + "void run(){Case.<java.io.IOException>empty();}}");
    }

    @Test public void assertion_status_and_side_effects() throws Exception {
        String source = "public class Case{static int calls;static boolean condition(){calls++;return false;}"
            + "public static int run(){try{assert condition():++calls;}catch(AssertionError e){return calls;}return calls;}}";
        for (boolean enabled : new boolean[] {false, true}) {
            org.codehaus.commons.compiler.ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
            compiler.setTargetVersion(8);
            compiler.cook(source);
            compiler.getClassLoader().setDefaultAssertionStatus(enabled);
            Class<?> type = compiler.getClassLoader().loadClass("Case");
            org.junit.Assert.assertEquals(enabled ? 2 : 0, type.getMethod("run").invoke(null));
            compiler.getClassLoader().setDefaultAssertionStatus(!enabled);
            org.junit.Assert.assertEquals(enabled ? 4 : 0, type.getMethod("run").invoke(null));
        }
    }
    @Test public void interface_assertion_status() throws Exception {
        org.codehaus.commons.compiler.ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook("interface I{default int test(){assert false;return 7;}}public class Case implements I{public static int run(){return new Case().test();}}");
        compiler.getClassLoader().setDefaultAssertionStatus(false);
        org.junit.Assert.assertEquals(7, compiler.getClassLoader().loadClass("Case").getMethod("run").invoke(null));
    }

    @Test public void type_use_is_not_a_declaration_annotation() throws Exception {
        this.program("import java.lang.annotation.*;public class Case{"
            + "@Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) public @interface A{}"
            + "@A public String field;@A public String method(){return null;}"
            + "public static boolean main()throws Exception{return Case.class.getField(\"field\").getAnnotations().length==0"
            + " && Case.class.getMethod(\"method\").getAnnotations().length==0"
            + " && Case.class.getField(\"field\").getAnnotatedType().getAnnotation(A.class)!=null"
            + " && Case.class.getMethod(\"method\").getAnnotatedReturnType().getAnnotation(A.class)!=null;}}");
    }
    @Test public void reject_invalid_annotation_target() throws Exception {
        this.assertCompilationUnitUncookable("import java.lang.annotation.*;@Target(ElementType.FIELD) @interface A{}class Case{@A void m(){}}");
    }
    @Test public void generic_variable_identity() throws Exception { this.program("public class Case<T>{T value;Case(T x){value=x;}T get(){return value;}static <U>U first(U... xs){return xs[0];}public static boolean main(){Case<String> c=new Case<String>(\"ok\");return c.get().equals(\"ok\") && first(\"yes\").equals(\"yes\");}}"); }
    @Test public void capture_helper() throws Exception { this.program("public class Case{static <T>void copy(java.util.List<T> x){x.set(0,x.get(0));}public static boolean main(){java.util.List<?> x=java.util.Arrays.asList(\"ok\");copy(x);return x.get(0).equals(\"ok\");}}"); }
    @Test public void bounded_wildcard_read_write() throws Exception { this.program("public class Case{static <T>T read(java.util.List<? extends T> xs){return xs.get(0);}public static boolean main(){java.util.List<? super String> xs=new java.util.ArrayList<Object>();xs.add(\"ok\");return read(java.util.Arrays.asList(\"ok\")).equals(xs.get(0));}}"); }
    @Test public void generic_constructor_checked_exception() throws Exception { this.program("public class Case{<E extends Exception>Case(E x)throws E{throw x;}public static boolean main(){try{new Case(new java.io.IOException());return false;}catch(java.io.IOException x){return true;}}}"); }
    @Test public void super_method_reference() throws Exception { this.program("public class Case{interface F{String get();}static class Base{String text(){return \"base\";}}static class Child extends Base{String text(){return \"child\";}F factory(){return super::text;}}public static boolean main(){return new Child().factory().get().equals(\"base\");}}"); }
    @Test public void outer_super_method_reference() throws Exception { this.program("public class Case{interface F{String get();}static class Base{String text(){return \"base\";}}static class Outer extends Base{String text(){return \"outer\";}class Inner{F factory(){return Outer.super::text;}}}public static boolean main(){Outer o=new Outer();return o.new Inner().factory().get().equals(\"base\");}}"); }
    @Test public void interface_super_method_reference() throws Exception { this.program("public class Case{interface F{String get();}interface I{default String text(){return \"base\";}}static class Child implements I{public String text(){return \"child\";}F factory(){return I.super::text;}}public static boolean main(){return new Child().factory().get().equals(\"base\");}}"); }
    @Test public void generic_super_method_reference() throws Exception { this.program("public class Case{interface F{String get(String s);}static class Base{<T>T id(T s){return s;}}static class Child extends Base{F factory(){return super::<String>id;}}public static boolean main(){return new Child().factory().get(\"ok\").equals(\"ok\");}}"); }
    @Test public void method_reference_checked_exception() throws Exception { this.program("public class Case{interface F{String get()throws java.io.IOException;}static String read()throws java.io.IOException{throw new java.io.IOException();}public static boolean main(){F f=Case::read;try{f.get();return false;}catch(java.io.IOException x){return true;}}}"); }
    @Test public void generic_array_identity() throws Exception { this.program("public class Case{static <T>T first(T[] a){return a[0];}static <T>T[] copy(T[] a){return a;}public static boolean main(){String[] a={\"ok\"};return first(copy(a)).equals(\"ok\");}}"); }
    @Test public void reject_type_variable_return() throws Exception { this.assertCompilationUnitUncookable("class Case{static <T>T f(){return \"x\";}}"); }
    @Test public void reject_type_variable_field() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{T value=\"x\";}"); }
    @Test public void reject_type_variable_invariance() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{java.util.List<T> value=new java.util.ArrayList<String>();}"); }
    @Test public void reject_impossible_wildcard_bound() throws Exception { this.assertCompilationUnitUncookable("class Case<T extends Number>{Case<? extends String> value;}"); }
    @Test public void reject_impossible_wildcard_lower() throws Exception { this.assertCompilationUnitUncookable("class Case<T extends Number>{Case<? super String> value;}"); }
    @Test public void reject_wildcard_capture_write() throws Exception { this.assertCompilationUnitUncookable("class Case{void f(java.util.List<?> x){x.set(0,x.get(0));}}"); }
    @Test public void reject_generic_method_result_assignment() throws Exception { this.assertCompilationUnitUncookable("class Case{static java.util.List<Integer> list(){return null;}java.util.List<String> value=list();}"); }
    @Test public void reject_static_class_type_variable() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{static T value;}"); }
    @Test public void reject_new_type_variable() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{Object value=new T();}"); }
    @Test public void reject_new_parameterized_array() throws Exception { this.assertCompilationUnitUncookable("class Case{Object value=new java.util.List<String>[2];}"); }
    @Test public void reject_new_wildcard_instance() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{Object value=new Case<?>();}"); }
    @Test public void reject_method_reference_checked_exception() throws Exception { this.assertCompilationUnitUncookable("class Case{interface F{String get();}static String read()throws java.io.IOException{return \"x\";}F f=Case::read;}"); }
    @Test public void reject_abstract_super_reference() throws Exception { this.assertCompilationUnitUncookable("abstract class Case{interface F{String get();}abstract static class Base{abstract String text();}static class Child extends Base{String text(){return \"x\";}F f=super::text;}}"); }
    @Test public void cast_does_not_target_generic_invocation() throws Exception { this.program("public class Case{static <T>java.util.List<T> list(){return new java.util.ArrayList<T>();}static <T>java.util.List<T> cast(){return (java.util.List<T>)(Object)Case.<Object>list();}public static boolean main(){return Case.<String>cast().isEmpty();}}"); }
    @Test public void anonymous_generic_field_identity() throws Exception { this.program("public class Case{interface F<T>{T get();}static <T>F<T> make(final T x){return new F<T>(){T value=x;public T get(){T result=this.value;return result;}};}public static boolean main(){return make(\"ok\").get().equals(\"ok\");}}"); }
    @Test public void generic_super_return_identity() throws Exception { this.program("public class Case{static class Base<T>{T value;Base(T x){value=x;}T get(){return value;}}static class Child<T>extends Base<T>{Child(T x){super(x);}T get(){return super.get();}}public static boolean main(){return new Child<String>(\"ok\").get().equals(\"ok\");}}"); }
    @Test public void generic_conditional_target() throws Exception { this.program("public class Case{static java.util.List<? extends Number> f(boolean b,java.util.List<? extends Number>a,java.util.List<Integer>c){return b?a:c;}public static boolean main(){return f(true,java.util.Arrays.asList(1),null).get(0).intValue()==1;}}"); }
    @Test public void qualified_type_annotation() throws Exception { this.program("import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface A{}public class Case{java.lang.@A String value;public static boolean main()throws Exception{return Case.class.getDeclaredField(\"value\").getAnnotatedType().getAnnotations().length==1;}}"); }
    @Test public void post_identifier_dimension_annotations() throws Exception { this.program("import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface A{}public class Case{String value @A [];public String[] f() @A []{return null;}public static boolean main()throws Exception{java.lang.reflect.AnnotatedType t=Case.class.getDeclaredField(\"value\").getAnnotatedType();return t.getAnnotations().length==1 && ((java.lang.reflect.AnnotatedArrayType)t).getAnnotatedGenericComponentType().getAnnotations().length==0 && Case.class.getMethod(\"f\").getAnnotatedReturnType().getAnnotations().length==1;}}"); }
    @Test public void varargs_type_annotations() throws Exception { this.program("import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface A{}public class Case{public static int f(@A String @A ...value){return value.length;}public static boolean main()throws Exception{java.lang.reflect.AnnotatedType t=Case.class.getMethod(\"f\",String[].class).getAnnotatedParameterTypes()[0];return t.getAnnotations().length==1 && ((java.lang.reflect.AnnotatedArrayType)t).getAnnotatedGenericComponentType().getAnnotations().length==1 && f(\"ok\")==1;}}"); }
    @Test public void nested_type_annotation() throws Exception { this.program("import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface A{}public class Case{static class Outer{class Inner{}}Outer.@A Inner value;public static boolean main()throws Exception{return Case.class.getDeclaredField(\"value\").getAnnotatedType().getAnnotations().length==1;}}"); }
    @Test public void reject_generic_array_initializer() throws Exception { this.assertCompilationUnitUncookable("class Case{Object value=new java.util.List<String>[]{};}"); }
    @Test public void reject_generic_array_shorthand() throws Exception { this.assertCompilationUnitUncookable("class Case{java.util.List<String>[] value={};}"); }
    @Test public void reifiable_wildcard_array_and_instanceof() throws Exception { this.program("public class Case{public static boolean main(){java.util.List<?>[] values=new java.util.List<?>[]{java.util.Arrays.asList(\"ok\")};Object value=values[0];return value instanceof java.util.List<?> && values[0].get(0).equals(\"ok\");}}"); }
    @Test public void reject_nonreifiable_instanceof() throws Exception { this.assertCompilationUnitUncookable("class Case{boolean f(Object x){return x instanceof java.util.List<String>;}}"); }
    @Test public void reject_type_variable_class_literal() throws Exception { this.assertCompilationUnitUncookable("class Case<T>{Class<?> value=T.class;}"); }
    @Test public void secondary_generic_bound_method() throws Exception { this.program("public class Case{static class N extends Number implements Comparable<N>{public int intValue(){return 1;}public long longValue(){return 1;}public float floatValue(){return 1;}public double doubleValue(){return 1;}public int compareTo(N x){return 0;}}static <T extends Number & Comparable<T>>int cmp(T a,T b){return a.compareTo(b);}public static boolean main(){return cmp(new N(),new N())==0;}}"); }
    @Test public void binary_generic_array_constructor() throws Exception { this.program("public class Case{public static boolean main(){Class<?>[] a={String.class};java.util.ArrayList<Class<?>> x=new java.util.ArrayList<Class<?>>(java.util.Arrays.asList(a));return x.get(0)==String.class;}}"); }
}
