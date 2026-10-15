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

/** Regressions exposed by compiling a complete Java runtime and the compiler together. */
@RunWith(Parameterized.class)
public class RuntimeCompilerCycleTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public RuntimeCompilerCycleTest(ICompilerFactory factory) throws Exception { super(factory); }

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

    @Test public void wildcard_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static <T>T[] copy(T[] x,Class<? extends T[]> c){return Arrays.copyOf(x,x.length,c);} public static String test()throws Exception{return copy(new String[]{\"ok\"},String[].class)[0];}}", "ok");
    }

    @Test public void wildcard_primitive_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static Class<? extends int[]> type(){return int[].class;} public static String test()throws Exception{return type().getComponentType()==int.class?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void wildcard_multidimensional_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static Class<? extends String[][]> type(){return String[][].class;} public static String test()throws Exception{return type()==String[][].class?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void wildcard_super_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static void take(Class<? super String[]> c){} public static String test()throws Exception{take(Object[].class);take(Object.class);return \"ok\";}}", "ok");
    }

    @Test public void f_bound_source() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static class A<T extends A<T>>{} static class B extends A<B>{} public static String test()throws Exception{return new B()!=null?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void enum_comparable_and_interface() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface F<T>{T get();}enum E implements F<String>{A;public String get(){return \"ok\";}} public static String test()throws Exception{Comparable<E> c=E.A;return c.compareTo(E.A)==0?E.A.get():\"bad\";}}", "ok");
    }

    @Test public void generic_array_clone() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static <T>T[] copy(T[]a){return a.clone();} public static String test()throws Exception{return copy(new String[]{\"ok\"})[0];}}", "ok");
    }

    @Test public void compound_local_and_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static int calls;static int index(){calls++;return 0;} static class A{long n=8;} public static String test()throws Exception{int n=256;int[]a={16};long[]b={8};A v=new A();int z=(n>>=4)+(a[index()]>>=2);long y=(b[index()]>>=1)+(v.n>>=1);return n==16&&a[0]==4&&b[0]==4&&v.n==4&&calls==2&&z==20&&y==8?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void for_condition_assignment() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{String e;String[]a={\"a\",\"b\",null};int n=0;for(int i=0;(e=a[i])!=null;i++){n+=e.length();}return n==2?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void for_condition_continue_break() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{String e;String[]a={\"a\",\"b\",\"c\",null};String s=\"\";for(int i=0;(e=a[i])!=null;i++){if(i==0)continue;s+=e;if(i==2)break;}return s;}}", "bc");
    }

    @Test public void for_condition_short_circuit() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{String e;String[]a={\"a\",\"b\"};int n=0;for(int i=0;i<a.length&&(e=a[i])!=null;i++){n+=e.length();}return n==2?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void inner_outer_generic_call() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static class A<E>{E e;A(E e){this.e=e;}E get(int i){return e;}class I{E next(){return get(0);}}E value(){return new I().next();}} public static String test()throws Exception{return new A<String>(\"ok\").value();}}", "ok");
    }

    @Test public void captured_array_component() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static <T,U>T[] copy(U[] a,Class<? extends T[]> c){return Arrays.copyOfRange(a,0,a.length,c);} public static String test()throws Exception{return copy(new Object[]{\"ok\"},String[].class)[0];}}", "ok");
    }

    @Test public void unchecked_subclass_cast() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static class I<T> implements Iterable<T>{public Iterator<T>iterator(){return Collections.<T>emptyList().iterator();}}static I<Object>empty=new I<Object>();static <T>Iterable<T> empty(){return (Iterable<T>)empty;} public static String test()throws Exception{return !Case.<String>empty().iterator().hasNext()?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void lambda_shadowing_generic_method() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static class A<K,V>{static <K,V>Consumer<Map.Entry<K,V>> wrap(Consumer<? super Map.Entry<K,V>>a){return e->a.accept(new AbstractMap.SimpleEntry<>(e));}} public static String test()throws Exception{StringBuilder s=new StringBuilder();Consumer<Map.Entry<String,Integer>> c=A.wrap(e->s.append(e.getKey()).append(e.getValue()));c.accept(new AbstractMap.SimpleEntry<>(\"ok\",1));return s.toString();}}", "ok1");
    }

    @Test public void lambda_class_type_variable() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static class A<E>{E value;A(E x){value=x;}E check(E x){return x;}UnaryOperator<E>wrap(UnaryOperator<E>op){return e->check(op.apply(e));}} public static String test()throws Exception{return new A<String>(\"x\").wrap(x->x+\"k\").apply(\"o\");}}", "ok");
    }

    @Test public void lambda_f_bound_intersection() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static <T,U extends Comparable<? super U>> Comparator<T> cmp(Function<? super T,? extends U>f){return (Comparator<T>&Serializable)(a,b)->f.apply(a).compareTo(f.apply(b));} public static String test()throws Exception{return cmp((String s)->s).compare(\"a\",\"b\")<0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void lambda_default_generic_intersection() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface C<T>{int cmp(T a,T b);default C<T>then(C<? super T>other){return (C<T>&Serializable)(a,b)->{int n=cmp(a,b);return n!=0?n:other.cmp(a,b);};}} public static String test()throws Exception{C<String>a=(x,y)->0;return a.then((x,y)->x.compareTo(y)).cmp(\"a\",\"b\")<0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void lambda_helper_inherited_collision() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface Parent{default int lambda$1(String x){return 9;}}interface Child extends Parent{static IntUnaryOperator op(){return x->x+1;}} public static String test()throws Exception{return Child.op().applyAsInt(1)==2?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void generic_inherited_parameter_bridge() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface F{boolean take(String s);}static class A<C>{public boolean take(C c){return c.equals(\"ok\");}}static class B extends A<String> implements F{} public static String test()throws Exception{F f=new B();return f.take(\"ok\")?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void generic_inherited_return_bridge() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface F{String get();}static class A<C>{C c;A(C c){this.c=c;}public C get(){return c;}}static class B extends A<String> implements F{B(){super(\"ok\");}} public static String test()throws Exception{F f=new B();return f.get();}}", "ok");
    }

    @Test public void generic_abstract_override() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface A<T>{boolean test(T t);}interface B extends A<String>{@Override boolean test(String t);}static class C implements B{public boolean test(String s){return true;}} public static String test()throws Exception{return new C().test(\"ok\")?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void private_inherited_name() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static boolean eq(Object a,Object b){return a.equals(b);}static class A{private static boolean eq(Object a,Object b){return false;}}static class B extends A{boolean same(){return eq(\"ok\",\"ok\");}} public static String test()throws Exception{return new B().same()?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void varargs_fixed_array_preferred() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static String f(String s,Object...a){return \"bad\";}static String f(Locale l,String s,Object...a){return \"ok\";} public static String test()throws Exception{return f(null,\"x\",new Object[0]);}}", "ok");
    }

    @Test public void varargs_boxing_phase() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static String f(int...a){return \"bad\";}static String f(Integer i){return \"ok\";} public static String test()throws Exception{return f(1);}}", "ok");
    }

    @Test public void varargs_null_array() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static String f(Object...a){return a==null?\"ok\":\"bad\";} public static String test()throws Exception{return f((Object[])null);}}", "ok");
    }

    @Test public void conditional_assignment() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{String[]a;int n=(a=new String[]{\"ok\"})==null?0:a.length;return n==1?a[0]:\"bad\";}}", "ok");
    }

    @Test public void conditional_both_assignment_branches() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{String[]a;boolean b=false;int n=b?(a=new String[]{\"bad\"}).length:(a=new String[]{\"ok\"}).length;return n==1?a[0]:\"bad\";}}", "ok");
    }

    @Test public void capture_iterator_filter() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface P<T>{boolean test(T t);}static <T>Iterable<T>filter(final Iterable<? extends T>d,final P<? super T>p){return new Iterable<T>(){public Iterator<T>iterator(){return Case.filter(d.iterator(),p);}};}static <T>Iterator<T>filter(Iterator<? extends T>d,P<? super T>p){return (Iterator<T>)d;} public static String test()throws Exception{return filter(Arrays.asList(\"ok\"),x->true).iterator().next();}}", "ok");
    }

    @Test public void class_literal_type_and_wildcard_equality() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static <T>boolean eq(Class<? super T>c){return c==Enum.class;}static <T>boolean upper(Class<? extends T>c){return c==String.class;} public static String test()throws Exception{Class<String>c=String.class;return c==String.class&&Case.<String>upper(c)&&Case.<Enum>eq(Enum.class)?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void for_zero_iterations() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{ public static String test()throws Exception{int n=0;for(int i=0;Boolean.FALSE.booleanValue();i++){n++;}return n==0?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void qualified_interface_this() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{interface C{int n();default C next(){return new C(){public int n(){return C.this.n()+1;}};}} public static String test()throws Exception{C c=new C(){public int n(){return 1;}};return c.next().n()==2?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void local_class_names_distinct() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{static Object a(){class Local implements Runnable{public void run(){}}return new Local();}static Object b(){class Local implements Comparable<String>{public int compareTo(String x){return 1;}}return new Local();} public static String test()throws Exception{Object a=a(),b=b();return a instanceof Runnable&&b instanceof Comparable&&a.getClass()!=b.getClass()?\"ok\":\"bad\";}}", "ok");
    }

    @Test public void private_native_name_and_descriptor() throws Exception {
        this.program("import java.util.*; import java.util.function.*; import java.io.*; public class Case{private native int value(int x); public static String test()throws Exception{java.lang.reflect.Method m=Case.class.getDeclaredMethod(\"value\",int.class);try{new Case().value(1);return \"bad\";}catch(UnsatisfiedLinkError expected){return java.lang.reflect.Modifier.isNative(m.getModifiers())&&!java.lang.reflect.Modifier.isStatic(m.getModifiers())?\"ok\":\"bad\";}}}", "ok");
    }

    @Test public void reject_class_literal_assignment() throws Exception {
        this.reject("class Case{Class<String> c=Integer.class;}");
    }

    @Test public void reject_class_literal_comparison() throws Exception {
        this.reject("class Case{boolean f(Class<String> c){return c==Integer.class;}}");
    }

    @Test public void reject_primitive_bound() throws Exception {
        this.reject("public class Case{Class<? extends int> c;}");
    }

    @Test public void reject_wrong_inherited_implementation() throws Exception {
        this.reject("interface F{boolean take(String s);}class A<C>{public boolean take(C c){return true;}}class Case extends A<Integer> implements F{}");
    }

    @Test public void reject_distinct_generic_cast() throws Exception {
        this.reject("import java.util.*;class Case{void f(List<String>x){List<Integer>y=(List<Integer>)x;}}");
    }

    @Test public void reject_shadowed_lambda_type() throws Exception {
        this.reject("import java.util.function.*;class Case<T>{static <T>Consumer<T>f(Consumer<String>c){return x->c.accept(x);}}");
    }

    @Test public void reject_wrong_array_target() throws Exception {
        this.reject("class Case{Class<? extends String[]> c=int[].class;}");
    }
}
