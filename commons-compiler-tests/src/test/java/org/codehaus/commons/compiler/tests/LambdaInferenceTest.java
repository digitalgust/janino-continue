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
import org.codehaus.commons.compiler.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.CommonsCompilerTestSuite;
import util.TestUtil;
/** Executable coverage for lambda constraints, checked throws and intersection least upper bounds. */
@RunWith(Parameterized.class)
public class LambdaInferenceTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name="CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {return TestUtil.getCompilerFactoriesForParameters();}
    public LambdaInferenceTest(ICompilerFactory factory) throws Exception {super(factory);}
    private void program(String source,String expected) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);c.cook(source);
        Assert.assertEquals(expected,c.getClassLoader().loadClass("Case").getMethod("test").invoke(null));
    }
    private void reject(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        try{c.cook(source);Assert.fail("Must reject invalid lambda/inference constraints");}catch(CompileException expected){}
    }

    @Test public void lambda_result_no_target() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>T make(Supplier<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->\"ok\").length();}}","2"
        );
    }

    @Test public void lambda_block_result_no_target() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>T make(Supplier<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->{return \"ok\";}).length();}}","2"
        );
    }

    @Test public void lambda_target_control() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>T make(Supplier<T>s){return s.get();}public static String test()throws Exception{String s=make(()->\"ok\");return s;}}","ok"
        );
    }

    @Test public void lambda_map_result() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T,R>R map(T x,Function<T,R>f){return f.apply(x);}public static String test()throws Exception{return \"\"+map(\"ok\",s->s.trim()).length();}}","2"
        );
    }

    @Test public void lambda_nested_list() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T,R>R map(T x,Function<T,R>f){return f.apply(x);}public static String test()throws Exception{return \"\"+map(\"ok\",s->Collections.singletonList(s)).get(0).length();}}","2"
        );
    }

    @Test public void explicit_lambda_parameter() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>String run(Function<T,String>f){return f.apply(null);}public static String test()throws Exception{return run((String s)->\"ok\");}}","ok"
        );
    }

    @Test public void nested_diamond_lifting() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>T id(T x){return x;}public static String test()throws Exception{List<String>x=id(new ArrayList<>());x.add(\"ok\");return x.get(0);}}","ok"
        );
    }

    @Test public void nested_diamond_target_control() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}public static String test()throws Exception{List<List<String>>x=Collections.singletonList(new ArrayList<>());x.get(0).add(\"ok\");return x.get(0).get(0);}}","ok"
        );
    }

    @Test public void intersection_secondary_member() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static class A implements Runnable,AutoCloseable{public void run(){}public void close(){}}static class B implements Runnable,AutoCloseable{public void run(){}public void close(){}}static<T>T choose(T a,T b){return a;}public static String test()throws Exception{choose(new A(),new B()).close();return \"ok\";}}","ok"
        );
    }

    @Test public void intersection_primary_member() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static class A implements Runnable,AutoCloseable{public void run(){}public void close(){}}static class B implements Runnable,AutoCloseable{public void run(){}public void close(){}}static<T>T choose(T a,T b){return a;}public static String test()throws Exception{choose(new A(),new B()).run();return \"ok\";}}","ok"
        );
    }

    @Test public void lambda_throws_inference() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}public static String test()throws Exception{try{run(()->{throw new IOException(\"ok\");});return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void reference_result_control() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface Supplier<T>{T get();}interface Function<T,R>{R apply(T x);}static<T>T make(Supplier<T>s){return s.get();}static String text(){return \"ok\";}public static String test()throws Exception{return \"\"+make(Case::text).length();}}","2"
        );
    }

    @Test public void boxed_lambda_result() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->3).intValue();}}","3"
        );
    }

    @Test public void bounded_lambda_result() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T extends CharSequence>T make(S<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->\"ok\").length();}}","2"
        );
    }

    @Test public void explicit_generic_parameter() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>String run(F<List<T>,String>f){return f.apply(Collections.<T>emptyList());}public static String test()throws Exception{return run((List<String>s)->\"ok\");}}","ok"
        );
    }

    @Test public void implicit_bound_input() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T extends CharSequence,R>R call(F<T,R>f){return f.apply(null);}public static String test()throws Exception{return \"\"+call(s->s==null?2:s.length()).intValue();}}","2"
        );
    }

    @Test public void later_lambda_input() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>String call(F<T,String>f,S<T>s){return f.apply(s.get());}public static String test()throws Exception{return call(s->s.trim(),()->\" ok \");}}","ok"
        );
    }

    @Test public void conditional_lambda_results() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->System.nanoTime()>0?\"ok\":new StringBuilder(\"ok\")).length();}}","2"
        );
    }

    @Test public void block_multiple_results() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{return \"\"+make(()->{if(System.nanoTime()>0)return \"ok\";return new StringBuilder(\"ok\");}).length();}}","2"
        );
    }

    @Test public void inherited_sam() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface Base<T,R>{R apply(T x);}interface Child<T>extends Base<T,String>{}static<T>String call(Child<T>f){return f.apply(null);}public static String test()throws Exception{return call((String s)->\"ok\");}}","ok"
        );
    }

    @Test public void constructor_lambda() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static class B<T>{T x;B(S<T>s){x=s.get();}T get(){return x;}}public static String test()throws Exception{return new B<>(()->\"ok\").get();}}","ok"
        );
    }

    @Test public void constructor_lambda_receiver() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static class B<T>{T x;B(S<T>s){x=s.get();}T get(){return x;}}public static String test()throws Exception{return \"\"+new B<>(()->\"ok\").get().length();}}","2"
        );
    }

    @Test public void varargs_lambdas() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T first(S<T>...s){return s[0].get();}public static String test()throws Exception{return first(()->\"ok\",()->\"other\");}}","ok"
        );
    }

    @Test public void conditional_lambda_argument() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{return make(System.nanoTime()>0?()->\"ok\":()->\"ok\");}}","ok"
        );
    }

    @Test public void reject_lambda_bound() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T extends Number>T make(S<T>s){return s.get();}public static String test()throws Exception{Object o=make(()->\"bad\");return \"bad\";}}"
        );
    }

    @Test public void reject_lambda_target() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{String s=make(()->1);return s;}}"
        );
    }

    @Test public void reject_explicit_parameter() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T,R>R map(T t,F<T,R>f){return f.apply(t);}public static String test()throws Exception{return map(1,(String s)->s);}}"
        );
    }

    @Test public void reject_explicit_invariance() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>String call(T x,F<List<T>,String>f){return \"bad\";}public static String test()throws Exception{return call(1,(List<String>s)->s.get(0));}}"
        );
    }

    @Test public void reject_void_value() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T make(S<T>s){return s.get();}public static String test()throws Exception{Object x=make(()->System.gc());return \"bad\";}}"
        );
    }

    @Test public void method_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{run(()->fail());return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void expression_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface G<T,E extends Exception>{T get()throws E;}static<T,E extends Exception>T get(G<T,E>g)throws E{return g.get();}static String fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{return get(()->fail());}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void constructor_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static class W{W()throws IOException{throw new IOException(\"ok\");}}public static String test()throws Exception{try{run(()->new W());return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void resource_close_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static class R implements AutoCloseable{public void close()throws IOException{throw new IOException(\"ok\");}}public static String test()throws Exception{try{run(()->{try(R r=new R()){} });return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void caught_inside_lambda() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{run(()->{try{fail();}catch(IOException e){} });return \"ok\";}}","ok"
        );
    }

    @Test public void finally_suppresses() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{run(()->{try{fail();}finally{return;} });return \"ok\";}}","ok"
        );
    }

    @Test public void rethrow_lambda() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{run(()->{try{fail();}catch(IOException e){throw e;} });return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void two_checked_exceptions() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static boolean first;static class Other extends Exception{Other(){super(\"ok\");}}public static String test()throws Exception{for(int i=0;i<2;i++){first=i==0;try{run(()->{if(first)throw new IOException(\"ok\");throw new Other();});return \"bad\";}catch(Exception e){if(!\"ok\".equals(e.getMessage()))return \"bad\";}}return \"ok\";}}","ok"
        );
    }

    @Test public void reject_uncaught_checked() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface Action{void run();}static void go(Action a){a.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{go(()->fail());return \"bad\";}}"
        );
    }

    @Test public void reject_exception_bound() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends IOException>void io(Task<E>a)throws E{a.run();}static class Other extends Exception{}public static String test()throws Exception{io(()->{throw new Other();});return \"bad\";}}"
        );
    }

    @Test public void reject_unhandled_checked() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test(){run(()->fail());return \"bad\";}}"
        );
    }

    @Test public void reject_exception_only_overload() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface IO<E extends IOException>{void run()throws E;}interface Other<E extends ReflectiveOperationException>{void run()throws E;}static<E extends IOException>void go(IO<E>x){}static<E extends ReflectiveOperationException>void go(Other<E>x){}public static String test()throws Exception{go(()->{throw new IOException();});return \"bad\";}}"
        );
    }

    @Test public void reject_unreachable_catch() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static void fail()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{run(()->{try{fail();}catch(IOException e){} });return \"bad\";}catch(IOException e){return \"bad\";}}}"
        );
    }

    @Test public void lub_calls_both() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}static StringBuilder log=new StringBuilder();static class A implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static class B implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}public static String test()throws Exception{pick(new A(),new B()).run();pick(new A(),new B()).close();return log.toString();}}","rc"
        );
    }

    @Test public void lub_class_and_interfaces() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}static class Base{String value(){return \"ok\";}}static class A extends Base implements Runnable,AutoCloseable{public void run(){}public void close(){}}static class B extends Base implements Runnable,AutoCloseable{public void run(){}public void close(){}}public static String test()throws Exception{pick(new A(),new B()).close();return pick(new A(),new B()).value();}}","ok"
        );
    }

    @Test public void lub_generic_member() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}interface Box<T>{T get();}static class A implements Box<String>,Runnable{public String get(){return \"ok\";}public void run(){}}static class B implements Box<String>,Runnable{public String get(){return \"ok\";}public void run(){}}public static String test()throws Exception{return pick(new A(),new B()).get();}}","ok"
        );
    }

    @Test public void lub_different_arguments() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}interface Box<T>{T get();}static class A implements Box<String>,Runnable{public String get(){return \"ok\";}public void run(){}}static class B implements Box<StringBuilder>,Runnable{public StringBuilder get(){return new StringBuilder(\"ok\");}public void run(){}}public static String test()throws Exception{return \"\"+pick(new A(),new B()).get().length();}}","2"
        );
    }

    @Test public void lub_three_arguments() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static StringBuilder log=new StringBuilder();static class A implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static class B implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static class C implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static<T>T pick(T a,T b,T c){return c;}public static String test()throws Exception{pick(new A(),new B(),new C()).close();return log.toString();}}","c"
        );
    }

    @Test public void lub_array_component() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}public static String test()throws Exception{return \"\"+pick(new String[]{\"ok\"},new StringBuilder[]{new StringBuilder(\"ok\")})[0].length();}}","2"
        );
    }

    @Test public void lub_list_member() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}public static String test()throws Exception{return pick(new ArrayList<String>(Arrays.asList(\"ok\")),new LinkedList<String>(Arrays.asList(\"ok\"))).get(0);}}","ok"
        );
    }

    @Test public void reject_lub_write() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}public static String test()throws Exception{pick(new ArrayList<String>(),new LinkedList<String>()).add(1);return \"bad\";}}"
        );
    }

    @Test public void reject_lub_capture_write() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}public static String test()throws Exception{pick(new ArrayList<String>(),new LinkedList<Integer>()).add(\"bad\");return \"bad\";}}"
        );
    }

    @Test public void reject_lub_ambiguous() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<T>T pick(T a,T b){return a;}static StringBuilder log=new StringBuilder();static class A implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static class B implements Runnable,AutoCloseable{public void run(){log.append(\"r\");}public void close(){log.append(\"c\");}}static String use(Runnable r){return \"r\";}static String use(AutoCloseable r){return \"c\";}public static String test()throws Exception{return use(pick(new A(),new B()));}}"
        );
    }

    @Test public void bound_reference_receiver_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface Action{void run();}interface G<T,E extends Exception>{T get()throws E;}static<T,E extends Exception>T get(G<T,E>f)throws E{return f.get();}static Action receiver()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{Action a=get(()->receiver()::run);return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void resource_variable_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static class R implements AutoCloseable{public void close()throws IOException{}void fail()throws IOException{throw new IOException(\"ok\");}}public static String test()throws Exception{try{run(()->{try(R r=new R()){r.fail();}});return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void for_variable_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static class R implements AutoCloseable{public void close()throws IOException{}void fail()throws IOException{throw new IOException(\"ok\");}}public static String test()throws Exception{try{run(()->{for(R r=new R();r!=null;){r.fail();break;}});return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void foreach_variable_throws() throws Exception {
        this.program(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}static<E extends Exception>void run(Task<E>t)throws E{t.run();}static class R implements AutoCloseable{public void close()throws IOException{}void fail()throws IOException{throw new IOException(\"ok\");}}public static String test()throws Exception{try{run(()->{for(R r:new R[]{new R()}){r.fail();}});return \"bad\";}catch(IOException e){return e.getMessage();}}}","ok"
        );
    }

    @Test public void reject_probe_catch_leak() throws Exception {
        this.reject(
            "import java.util.*;import java.io.*;public class Case{interface S<T>{T get();}interface F<T,R>{R apply(T x);}interface Task<E extends Exception>{void run()throws E;}interface G{String get()throws IOException;}static String swallow(G f){try{return f.get();}catch(IOException e){return \"ok\";}}static String text()throws IOException{throw new IOException(\"ok\");}public static String test()throws Exception{try{return swallow(()->text());}catch(IOException e){return \"bad\";}}}"
        );
    }
}
