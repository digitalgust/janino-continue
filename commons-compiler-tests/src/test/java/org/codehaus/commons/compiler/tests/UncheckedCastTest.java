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

/** Unchecked type-variable casts, bounds and erasure runtime semantics. */
@RunWith(Parameterized.class)
public class UncheckedCastTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public UncheckedCastTest(ICompilerFactory factory) throws Exception { super(factory); }

    private void program(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        compiler.cook(source);
        Assert.assertEquals("ok", compiler.getClassLoader().loadClass("Case").getMethod("test").invoke(null));
    }

    private void reject(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        try {
            compiler.cook(source);
            Assert.fail("Must reject incompatible type-variable casts");
        } catch (CompileException expected) {}
    }

    @Test public void unbounded() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<String>value();}}");
    }

    @Test public void bounded() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends CharSequence>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<String>value().toString();}}");
    }

    @Test public void intersection() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends CharSequence & Serializable>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<String>value().toString();}}");
    }

    @Test public void builder() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends CharSequence & Serializable>T value(){return (T)new StringBuilder(\"ok\");}public static String test()throws Exception{return Case.<StringBuilder>value().toString();}}");
    }

    @Test public void object_narrow() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Number>T cast(Object x){return (T)x;}public static String test()throws Exception{return Case.<Integer>cast(7).intValue()==7?\"ok\":\"bad\";}}");
    }

    @Test public void erasure_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Number>T cast(Object x){return (T)x;}public static String test()throws Exception{try{Object x=Case.<Integer>cast(\"bad\");return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void caller_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{Integer x=Case.<Integer>value();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void null_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static abstract class N extends Number implements Runnable{}static<T extends Number & Runnable>T value(){return (T)null;}public static String test()throws Exception{Object x=Case.<N>value();return x==null?\"ok\":\"bad\";}}");
    }

    @Test public void dependent() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends U,U extends CharSequence>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<String,CharSequence>value().toString();}}");
    }

    @Test public void fbound() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Comparable<T>>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<String>value();}}");
    }

    @Test public void class_variable() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class Box<T>{T value(){return (T)\"ok\";}}public static String test()throws Exception{return new Box<String>().value();}}");
    }

    @Test public void array() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T[] value(){return (T[])new String[]{\"ok\"};}public static String test()throws Exception{return Case.<String>value()[0];}}");
    }

    @Test public void array_variable() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)new String[]{\"ok\"};}public static String test()throws Exception{return Case.<String[]>value()[0];}}");
    }

    @Test public void reject_boxed_unbounded() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)7;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_boxed_number() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Number>T value(){return (T)7;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void boxed_reference() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Number>T value(){return (T)Integer.valueOf(7);}public static String test()throws Exception{return Case.<Integer>value().intValue()==7?\"ok\":\"bad\";}}");
    }

    @Test public void lambda() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{interface F<T,R>{R apply(T x);}static<T>String call(F<List<T>,String>f){return f.apply(Arrays.asList((T)\"ok\"));}public static String test()throws Exception{return call((List<String>x)->x.get(0));}}");
    }

    @Test public void intersection_runtime() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class N extends Number implements Runnable{public int intValue(){return 7;}public long longValue(){return 7;}public float floatValue(){return 7;}public double doubleValue(){return 7;}public void run(){}}static<T extends Number & Runnable>T cast(Object x){return (T)x;}public static String test()throws Exception{N n=Case.<N>cast(new N());n.run();return n.intValue()==7?\"ok\":\"bad\";}}");
    }

    @Test public void intersection_erasure_only() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static abstract class N extends Number implements Runnable{}static<T extends Number & Runnable>T cast(Object x){return (T)x;}public static String test()throws Exception{Object x=Case.<N>cast(7);return x.equals(7)?\"ok\":\"bad\";}}");
    }

    @Test public void interface_member_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static abstract class N extends Number implements Runnable{}static<T extends Number & Runnable>void use(Object x){T t=(T)x;t.run();}public static String test()throws Exception{try{Case.<N>use(7);return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void reject_number() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Number>T value(){return (T)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_interface() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Runnable>T value(){return (T)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_secondary() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Object & Runnable>T value(){return (T)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_dependent() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends U,U extends Number>T value(){return (T)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_assignment() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return \"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_generic_bound() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(List<Integer>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_array() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Number>T value(){return (T)new String[0];}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_boxed() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends CharSequence>T value(){return (T)7;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_fixed_fbound() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends Comparable<Integer>>T value(){return (T)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void wildcard_list() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(List<?>x){return (T)x;}public static String test()throws Exception{return Case.<List<String>>cast(Arrays.asList(\"ok\")).get(0);}}");
    }

    @Test public void reject_wildcard_list() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(List<? extends Number>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void generic_supertype() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(ArrayList<String>x){return (T)x;}public static String test()throws Exception{ArrayList<String>x=new ArrayList<>();x.add(\"ok\");return Case.<ArrayList<String>>cast(x).get(0);}}");
    }

    @Test public void reject_generic_supertype() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(ArrayList<Integer>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_upper_wildcard_object() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<Object>>T cast(List<? extends Number>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_lower_wildcard_integer() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<Integer>>T cast(List<? super Number>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void upper_wildcard() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends List<Integer>>T cast(List<? extends Number>x){return (T)x;}public static String test()throws Exception{return Case.<List<Integer>>cast(Arrays.asList(7)).get(0)==7?\"ok\":\"bad\";}}");
    }

    @Test public void lower_wildcard() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends List<Object>>T cast(List<? super Number>x){return (T)x;}public static String test()throws Exception{return Case.<List<Object>>cast(new ArrayList<Object>()).isEmpty()?\"ok\":\"bad\";}}");
    }

    @Test public void nested_variable() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Comparable<List<T>>>T cast(Object x){return (T)x;}public static String test()throws Exception{return \"ok\";}}");
    }

    @Test public void nested_generic() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<List<String>>>T cast(List<List<?>>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void generic_array_component() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T[] cast(Object[]x){return (T[])x;}public static String test()throws Exception{return Case.<String>cast(new String[]{\"ok\"})[0];}}");
    }

    @Test public void reject_primitive_array_component() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T[] cast(int[]x){return (T[])x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_recursive_generic_array() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T extends List<U[]>,U>T cast(List<String[]>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void raw_list() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends List<String>>T cast(List x){return (T)x;}public static String test()throws Exception{return Case.<List<String>>cast(Arrays.asList(\"ok\")).get(0);}}");
    }

    @Test public void discard_specific_result() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Object x=Case.<Integer>value();return x.equals(\"ok\")?\"ok\":\"bad\";}}");
    }

    @Test public void return_object_result() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static Object result(){return Case.<Integer>value();}public static String test()throws Exception{return result().equals(\"ok\")?\"ok\":\"bad\";}}");
    }

    @Test public void argument_object_result() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static Object take(Object x){return x;}public static String test()throws Exception{return take(Case.<Integer>value()).equals(\"ok\")?\"ok\":\"bad\";}}");
    }

    @Test public void reject_variable_argument() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<U extends Number,T extends List<U>>T cast(List<String>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_variable_supertype() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<U extends Number,T extends List<U>>T cast(ArrayList<String>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void reject_variable_nested() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<U extends Number,T extends List<U>>T cast(List<List<?>>x){return (T)x;}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void variable_argument() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<U extends Number,T extends List<U>>T cast(List<Integer>x){return (T)x;}public static String test()throws Exception{return Case.<Integer,List<Integer>>cast(Arrays.asList(7)).get(0)==7?\"ok\":\"bad\";}}");
    }

    @Test public void primitive_array_value() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)new int[]{7};}public static String test()throws Exception{return Case.<int[]>value()[0]==7?\"ok\":\"bad\";}}");
    }

    @Test public void discard_statement() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Case.<Integer>value();return \"ok\";}}");
    }

    @Test public void discard_instance_statement() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class B{<T>T value(){return (T)\"ok\";}}public static String test()throws Exception{new B().<Integer>value();return \"ok\";}}");
    }

    @Test public void void_expression_lambda_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Runnable r=()->Case.<Integer>value();try{r.run();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void discard_block_lambda() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Runnable r=()->{Case.<Integer>value();};r.run();return \"ok\";}}");
    }

    @Test public void discard_method_reference() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Runnable r=Case::<Integer>value;r.run();return \"ok\";}}");
    }

}
