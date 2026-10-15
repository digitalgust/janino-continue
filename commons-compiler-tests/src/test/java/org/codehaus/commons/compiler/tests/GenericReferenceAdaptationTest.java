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

package org.codehaus.commons.compiler.tests;

import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/** Generic fields, super calls and casts must match javac at each consuming position. */
@RunWith(Parameterized.class)
public class GenericReferenceAdaptationTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public GenericReferenceAdaptationTest(ICompilerFactory factory) throws Exception { super(factory); }

    private void program(String source, String expected) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        compiler.cook(source);
        try {
            Object result = compiler.getClassLoader().loadClass("Case").getMethod("test").invoke(null);
            Assert.assertEquals(expected, result);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (!"CCE".equals(expected)) throw e;
            Assert.assertTrue(e.getCause() instanceof ClassCastException);
        }
    }

    private void reject(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        try {
            compiler.cook(source);
            Assert.fail("Must reject incompatible generic reference operations");
        } catch (CompileException expected) {}
    }

    @Test public void super_object_return() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{Object get(){return super.<Integer>value();}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void super_object_assignment() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{Object get(){Object x=super.<Integer>value();return x;}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void super_object_argument() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{String use(Object o){return o.toString();}String get(){return use(super.<Integer>value());}}public static String test()throws Exception{return new B().get();}}", "ok");
    }

    @Test public void super_discard() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{void drop(){super.<Integer>value();}}public static String test()throws Exception{new B().drop();return \"ok\";}}", "ok");
    }

    @Test public void super_serializable() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{java.io.Serializable get(){return super.<Integer>value();}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void super_object_cast_control() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{Object get(){return (Object)super.<Integer>value();}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void super_integer_control() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{Integer get(){return super.<Integer>value();}}public static String test()throws Exception{try{new B().get();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}", "ok");
    }

    @Test public void super_lambda_object() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{F get(){return ()->super.<Integer>value();}}public static String test()throws Exception{return new B().get().get().toString();}}", "CCE");
    }

    @Test public void super_lambda_void_expression() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{V get(){return ()->super.<Integer>value();}}public static String test()throws Exception{try{new B().get().run();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}", "ok");
    }

    @Test public void super_lambda_void_block() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{V get(){return ()->{super.<Integer>value();};}}public static String test()throws Exception{new B().get().run();return \"ok\";}}", "ok");
    }

    @Test public void super_method_reference_object() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{F get(){return super::<Integer>value;}}public static String test()throws Exception{return new B().get().get().toString();}}", "ok");
    }

    @Test public void super_method_reference_void() throws Exception {
        this.program("public class Case{static class A{<T>T value(){return (T)\"ok\";}}interface F{Object get();}interface V{void run();}static class B extends A{V get(){return super::<Integer>value;}}public static String test()throws Exception{new B().get().run();return \"ok\";}}", "ok");
    }

    @Test public void generic_super_object_return() throws Exception {
        this.program("public class Case{static class A<T>{T value(){return (T)\"ok\";}}static class B extends A<Integer>{Object get(){return super.value();}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void generic_super_discard() throws Exception {
        this.program("public class Case{static class A<T>{T value(){return (T)\"ok\";}}static class B extends A<Integer>{void drop(){super.value();}}public static String test()throws Exception{new B().drop();return \"ok\";}}", "ok");
    }

    @Test public void interface_super_object_return() throws Exception {
        this.program("public class Case{interface A{default<T>T value(){return (T)\"ok\";}}static class B implements A{Object get(){return A.super.<Integer>value();}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void field_object_assignment() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{Object x=box().x;return x.toString();}}", "ok");
    }

    @Test public void field_object_return() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{return read().toString();}}", "ok");
    }

    @Test public void field_object_argument() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{return use(box().x);}}", "ok");
    }

    @Test public void field_object_cast() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{Object x=(Object)box().x;return x.toString();}}", "ok");
    }

    @Test public void field_reference_equality() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{return box().x==(Object)\"ok\"?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void field_instanceof_object() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{return box().x instanceof Object?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void field_string_concat() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{return \"\"+box().x;}}", "ok");
    }

    @Test public void field_integer_control() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{try{Integer x=box().x;return \"bad\";}catch(ClassCastException e){return \"ok\";}}}", "ok");
    }

    @Test public void field_unbox_control() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static Object read(){return box().x;}static String use(Object x){return x.toString();}public static String test()throws Exception{try{int x=box().x;return \"bad\";}catch(ClassCastException e){return \"ok\";}}}", "ok");
    }

    @Test public void array_field_element_object() throws Exception {
        this.program("public class Case{static class B<T>{T[] x;}static B<Integer> box(){B b=new B();b.x=new Object[]{\"ok\"};return b;}public static String test()throws Exception{Object x=box().x[0];return x.toString();}}", "CCE");
    }

    @Test public void array_field_element_concat() throws Exception {
        this.program("public class Case{static class B<T>{T[] x;}static B<Integer> box(){B b=new B();b.x=new Object[]{\"ok\"};return b;}public static String test()throws Exception{return \"\"+box().x[0];}}", "CCE");
    }

    @Test public void array_field_object() throws Exception {
        this.program("public class Case{static class B<T>{T[] x;}static B<Integer> box(){B b=new B();b.x=new Object[]{\"ok\"};return b;}public static String test()throws Exception{Object x=box().x;return x.getClass().getName();}}", "[Ljava.lang.Object;");
    }

    @Test public void array_field_length() throws Exception {
        this.program("public class Case{static class B<T>{T[] x;}static B<Integer> box(){B b=new B();b.x=new Object[]{\"ok\"};return b;}public static String test()throws Exception{return \"n=\"+box().x.length;}}", "CCE");
    }

    @Test public void array_field_integer_control() throws Exception {
        this.program("public class Case{static class B<T>{T[] x;}static B<Integer> box(){B b=new B();b.x=new Object[]{\"ok\"};return b;}public static String test()throws Exception{try{Integer x=box().x[0];return \"bad\";}catch(ClassCastException e){return \"ok\";}}}", "ok");
    }

    @Test public void invariant_comparison_invalid() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<String>a=null;java.util.List<Integer>b=null;return a==b?\"ok\":\"bad\";}}");
    }

    @Test public void invariant_cast_invalid() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<String>a=null;java.util.List<Integer>b=(java.util.List<Integer>)a;return \"ok\";}}");
    }

    @Test public void nonreifiable_instanceof_invalid() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{Object o=null;return o instanceof java.util.List<Integer>?\"bad\":\"ok\";}}");
    }

    @Test public void wildcard_comparison_control() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<String>a=null;java.util.List<?>b=null;return a==b?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void wildcard_cast_control() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<?>a=null;java.util.List<Integer>b=(java.util.List<Integer>)a;return \"ok\";}}", "ok");
    }

    @Test public void lambda_expression_object() throws Exception {
        this.program("public class Case{static<T>T value(){return (T)\"ok\";}interface F{Object get();}interface V{void run();}static boolean flag(){return true;}public static String test()throws Exception{F f=()->Case.<Integer>value();return f.get().toString();}}", "CCE");
    }

    @Test public void lambda_block_object() throws Exception {
        this.program("public class Case{static<T>T value(){return (T)\"ok\";}interface F{Object get();}interface V{void run();}static boolean flag(){return true;}public static String test()throws Exception{F f=()->{return Case.<Integer>value();};return f.get().toString();}}", "ok");
    }

    @Test public void lambda_expression_field() throws Exception {
        this.program("public class Case{static<T>T value(){return (T)\"ok\";}interface F{Object get();}interface V{void run();}static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{F f=()->box().x;return f.get().toString();}}", "CCE");
    }

    @Test public void lambda_block_field() throws Exception {
        this.program("public class Case{static<T>T value(){return (T)\"ok\";}interface F{Object get();}interface V{void run();}static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{F f=()->{return box().x;};return f.get().toString();}}", "ok");
    }

    @Test public void ordinary_reference_object() throws Exception {
        this.program("public class Case{static<T>T value(){return (T)\"ok\";}interface F{Object get();}interface V{void run();}static boolean flag(){return true;}public static String test()throws Exception{F f=Case::<Integer>value;return f.get().toString();}}", "ok");
    }

    @Test public void field_parentheses_object() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{Object x=((box().x));return x.toString();}}", "ok");
    }

    @Test public void field_serializable() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{java.io.Serializable x=box().x;return x.toString();}}", "ok");
    }

    @Test public void field_conditional_object() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{Object x=flag()?box().x:null;return x.toString();}}", "CCE");
    }

    @Test public void field_conditional_cast() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{Object x=(Object)(flag()?box().x:null);return x.toString();}}", "CCE");
    }

    @Test public void field_conditional_concat() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{return \"\"+(flag()?box().x:null);}}", "CCE");
    }

    @Test public void field_receiver_control() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static boolean flag(){return true;}public static String test()throws Exception{return \"n=\"+box().x.intValue();}}", "CCE");
    }

    @Test public void field_order() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<String>(\"ok\");}static String trace=\"\";static B<Integer> next(String s){trace+=s;return box();}static boolean flag(){return true;}public static String test()throws Exception{String x=next(\"a\").x+\"\"+next(\"b\").x;return x.equals(\"okok\")&&trace.equals(\"ab\")?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void field_number_target() throws Exception {
        this.program("public class Case{static class B<T extends Number>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<Double>(2.0);}static boolean flag(){return true;}public static String test()throws Exception{Number x=box().x;return x.doubleValue()==2.0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void field_bounded_object() throws Exception {
        this.program("public class Case{static class B<T extends Number>{T x;B(T x){this.x=x;}}static B<Integer> box(){return (B)new B<Double>(2.0);}static boolean flag(){return true;}public static String test()throws Exception{Object x=box().x;return x.equals(2.0)?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void super_lambda_block_return() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{F get(){return ()->{return super.<Integer>value();};}}public static String test()throws Exception{return new B().get().get().toString();}}", "ok");
    }

    @Test public void super_lambda_cast() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{F get(){return ()->(Object)super.<Integer>value();}}public static String test()throws Exception{return new B().get().get().toString();}}", "ok");
    }

    @Test public void super_lambda_concat() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{F get(){return ()->\"\"+super.<Integer>value();}}public static String test()throws Exception{return new B().get().get().toString();}}", "ok");
    }

    @Test public void super_lambda_instanceof() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{F get(){return ()->super.<Integer>value() instanceof Object;}}public static String test()throws Exception{return Boolean.TRUE.equals(new B().get().get())?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void super_number_control() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{Number get(){return super.<Integer>value();}}public static String test()throws Exception{return new B().get().toString();}}", "CCE");
    }

    @Test public void super_conditional_object() throws Exception {
        this.program("public class Case{interface F{Object get();}interface V{void run();}static class A{<T>T value(){return (T)\"ok\";}}static boolean flag(){return true;}static class B extends A{Object get(){return flag()?super.<Integer>value():null;}}public static String test()throws Exception{return new B().get().toString();}}", "ok");
    }

    @Test public void reject_inherited_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.ArrayList<String>a=null;java.util.List<Integer>b=(java.util.List<Integer>)a;return \"bad\";}}");
    }

    @Test public void reject_reverse_inherited_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<String>a=null;java.util.ArrayList<Integer>b=(java.util.ArrayList<Integer>)a;return \"bad\";}}");
    }

    @Test public void reject_inherited_compare() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.ArrayList<String>a=null;java.util.List<Integer>b=null;return a==b?\"bad\":\"bad\";}}");
    }

    @Test public void reject_common_interface_cast() throws Exception {
        this.reject("public class Case{interface A extends java.util.List<String>{}interface B extends java.util.List<Integer>{}public static String test()throws Exception{A a=null;B b=(B)a;return \"bad\";}}");
    }

    @Test public void reject_nested_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<java.util.List<?>>a=null;java.util.List<java.util.List<String>>b=(java.util.List<java.util.List<String>>)a;return \"bad\";}}");
    }

    @Test public void reject_wildcard_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<? extends Integer>a=null;java.util.List<? extends String>b=(java.util.List<? extends String>)a;return \"bad\";}}");
    }

    @Test public void reject_lower_upper_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<? extends Number>a=null;java.util.List<? super String>b=(java.util.List<? super String>)a;return \"bad\";}}");
    }

    @Test public void reject_generic_array_cast() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<String>[]a=null;java.util.List<Integer>[]b=(java.util.List<Integer>[])a;return \"bad\";}}");
    }

    @Test public void reject_generic_array_compare() throws Exception {
        this.reject("public class Case{public static String test()throws Exception{java.util.List<String>[]a=null;java.util.List<Integer>[]b=null;return a==b?\"bad\":\"bad\";}}");
    }

    @Test public void reject_owner_cast() throws Exception {
        this.reject("public class Case{static class O<T>{class I{}}public static String test()throws Exception{O<String>.I a=null;O<Integer>.I b=(O<Integer>.I)a;return \"bad\";}}");
    }

    @Test public void reject_owner_compare() throws Exception {
        this.reject("public class Case{static class O<T>{class I{}}public static String test()throws Exception{O<String>.I a=null;O<Integer>.I b=null;return a==b?\"bad\":\"bad\";}}");
    }

    @Test public void wildcard_upper_overlap() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<? extends Number>a=null;java.util.List<? extends CharSequence>b=(java.util.List<? extends CharSequence>)a;return \"ok\";}}", "ok");
    }

    @Test public void wildcard_lower_overlap() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<? super Number>a=null;java.util.List<? super Integer>b=(java.util.List<? super Integer>)a;return \"ok\";}}", "ok");
    }

    @Test public void raw_cast() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List a=null;java.util.List<Integer>b=(java.util.List<Integer>)a;return \"ok\";}}", "ok");
    }

    @Test public void object_bridge_cast() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<String>a=null;java.util.List<Integer>b=(java.util.List<Integer>)(Object)a;return \"ok\";}}", "ok");
    }

    @Test public void variable_cast() throws Exception {
        this.program("public class Case{static<T,U>java.util.List<U> convert(java.util.List<T>x){return (java.util.List<U>)x;}public static String test()throws Exception{return convert(null)==null?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void wildcard_array_cast() throws Exception {
        this.program("public class Case{public static String test()throws Exception{java.util.List<?>[]a=null;java.util.List<Integer>[]b=(java.util.List<Integer>[])a;return \"ok\";}}", "ok");
    }

    @Test public void owner_wildcard_cast() throws Exception {
        this.program("public class Case{static class O<T>{class I{}}public static String test()throws Exception{O<?>.I a=null;O<Integer>.I b=(O<Integer>.I)a;return \"ok\";}}", "ok");
    }

    @Test public void reference_number_target() throws Exception {
        this.program("public class Case{interface F{Number get();}static<T>T value(){return (T)Double.valueOf(2.0);}public static String test()throws Exception{F f=Case::<Integer>value;return f.get().doubleValue()==2.0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void super_reference_number_target() throws Exception {
        this.program("public class Case{interface F{Number get();}static class A{<T>T value(){return (T)Double.valueOf(2.0);}}static class B extends A{F reference(){return super::<Integer>value;}}public static String test()throws Exception{F f=new B().reference();return f.get().doubleValue()==2.0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void reference_serializable_target() throws Exception {
        this.program("public class Case{interface F{java.io.Serializable get();}static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{F f=Case::<Integer>value;return f.get().toString();}}", "ok");
    }

    @Test public void super_reference_serializable_target() throws Exception {
        this.program("public class Case{interface F{java.io.Serializable get();}static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{F reference(){return super::<Integer>value;}}public static String test()throws Exception{F f=new B().reference();return f.get().toString();}}", "ok");
    }

    @Test public void reference_integer_control() throws Exception {
        this.program("public class Case{interface F{Integer get();}static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{F f=Case::<Integer>value;return f.get().toString();}}", "CCE");
    }

    @Test public void super_reference_integer_control() throws Exception {
        this.program("public class Case{interface F{Integer get();}static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{F reference(){return super::<Integer>value;}}public static String test()throws Exception{F f=new B().reference();return f.get().toString();}}", "CCE");
    }

    @Test public void reference_primitive_control() throws Exception {
        this.program("public class Case{interface F{int get();}static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{F f=Case::<Integer>value;return \"n=\"+f.get();}}", "CCE");
    }

    @Test public void super_reference_primitive_control() throws Exception {
        this.program("public class Case{interface F{int get();}static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{F reference(){return super::<Integer>value;}}public static String test()throws Exception{F f=new B().reference();return \"n=\"+f.get();}}", "CCE");
    }

    @Test public void reference_charsequence_target() throws Exception {
        this.program("public class Case{interface F{CharSequence get();}static<T>T value(){return (T)new StringBuilder(\"ok\");}public static String test()throws Exception{F f=Case::<String>value;return f.get().toString();}}", "ok");
    }

    @Test public void reference_generic_integer_control() throws Exception {
        this.program("public class Case{interface F<T>{T get();}static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{F<Integer> f=Case::<Integer>value;return f.get().toString();}}", "CCE");
    }

    @Test public void field_string_control() throws Exception {
        this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}static B<String> box(){return (B)new B<StringBuilder>(new StringBuilder(\"ok\"));}public static String test()throws Exception{return \"\"+box().x;}}", "CCE");
    }

    @Test public void inherited_field_object() throws Exception {
        this.program("public class Case{static class A<T>{T x;}static class B extends A<Integer>{Object get(){return x;}}public static String test()throws Exception{B b=new B();((A)b).x=\"ok\";return b.get().toString();}}", "ok");
    }

    @Test public void super_field_object() throws Exception {
        this.program("public class Case{static class A<T>{T x;}static class B extends A<Integer>{Object get(){return super.x;}}public static String test()throws Exception{B b=new B();((A)b).x=\"ok\";return b.get().toString();}}", "ok");
    }

    @Test public void super_field_reference() throws Exception {
        this.program("public class Case{static class A<T>{T x;}static class B extends A<Integer>{boolean get(){return super.x==(Object)\"ok\";}}public static String test()throws Exception{B b=new B();((A)b).x=\"ok\";return b.get()?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void reference_numeric_erased() throws Exception {
        this.program("public class Case{interface F{int get();}static<T>T value(){return (T)Double.valueOf(2.0);}public static String test()throws Exception{F f=Case::<Integer>value;return \"n=\"+f.get();}}", "n=2");
    }

    @Test public void reference_byte_to_int() throws Exception {
        this.program("public class Case{interface F{int get();}static<T>T value(){return (T)Double.valueOf(128.0);}public static String test()throws Exception{F f=Case::<Byte>value;return \"n=\"+f.get();}}", "n=128");
    }

    @Test public void reference_numeric_widening() throws Exception {
        this.program("public class Case{interface F{long get();}static<T>T value(){return (T)Double.valueOf(2.0);}public static String test()throws Exception{F f=Case::<Integer>value;return \"n=\"+f.get();}}", "n=2");
    }

    @Test public void reference_character_widening() throws Exception {
        this.program("public class Case{interface F{int get();}static<T>T value(){return (T)Character.valueOf('a');}public static String test()throws Exception{F f=Case::<Character>value;return \"n=\"+f.get();}}", "CCE");
    }

    @Test public void reference_character_identity() throws Exception {
        this.program("public class Case{interface F{char get();}static<T>T value(){return (T)Character.valueOf('a');}public static String test()throws Exception{F f=Case::<Character>value;return \"n=\"+f.get();}}", "n=a");
    }

    @Test public void super_reference_numeric_erased() throws Exception {
        this.program("public class Case{interface F{int get();}static class A{<T>T value(){return (T)Double.valueOf(2.0);}}static class B extends A{F get(){return super::<Integer>value;}}public static String test()throws Exception{return \"n=\"+new B().get().get();}}", "CCE");
    }

    @Test public void reference_input_widening() throws Exception {
        this.program("public class Case{interface F{int get(Byte x);}static<T>T value(T x){return x;}public static String test()throws Exception{F f=Case::value;return \"n=\"+f.get(Byte.valueOf((byte)2));}}", "n=2");
    }

    @Test public void super_reference_input_widening() throws Exception {
        // javac 8 emits a wrong Long cast here; javac 17/25 with source/target 8 and Janino return n=2.
        this.program("public class Case{interface F{long get(Integer x);}static class A{<T>T value(T x){return x;}}static class B extends A{F get(){return super::value;}}public static String test()throws Exception{return \"n=\"+new B().get().get(Integer.valueOf(2));}}", this.compilerFactory.getId().contains(".jdk") ? "CCE" : "n=2");
    }

    @Test public void reference_declared_character() throws Exception {
        this.program("public class Case{interface F{int get();}static Character value(){return Character.valueOf('a');}public static String test()throws Exception{F f=Case::value;return \"n=\"+f.get();}}", "n=97");
    }

}
