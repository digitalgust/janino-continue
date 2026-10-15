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
import org.codehaus.commons.compiler.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.CommonsCompilerTestSuite;
import util.TestUtil;
/** Regression coverage for array references, diamond and raw constructor inference. */
@RunWith(Parameterized.class) public class Java8RemainingTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name="CompilerFactory={0}") public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    public Java8RemainingTest(ICompilerFactory f) throws Exception { super(f); }
    private void program(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);c.cook(source);
        Assert.assertEquals(Boolean.TRUE,c.getClassLoader().loadClass("Case").getMethod("main").invoke(null));
    }
    private void reject(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        try {c.cook(source);Assert.fail("Must reject invalid parameterized reference");}catch(CompileException expected){}
    }
    @Test public void array_clone() throws Exception {this.program("public class Case{interface F{String[]get(String[]x);}public static boolean main(){F f=String[]::clone;String[]x={\"ok\"};String[]y=f.get(x);return y!=x&&y[0].equals(\"ok\");}}");}
    @Test public void primitive_clone() throws Exception {this.program("public class Case{interface F{int[]get(int[]x);}public static boolean main(){F f=int[]::clone;int[]x={3};int[]y=f.get(x);return x!=y&&y[0]==3;}}");}
    @Test public void multidimensional_clone() throws Exception {this.program("public class Case{interface F{String[][]get(String[][]x);}public static boolean main(){F f=String[][]::clone;String[][]x={{\"ok\"}};String[][]y=f.get(x);return x!=y&&x[0]==y[0];}}");}
    @Test public void generic_clone() throws Exception {this.program("import java.util.*;public class Case{interface F{List<String>[]get(List<String>[]x);}public static boolean main(){F f=List<String>[]::clone;List<String>[]x=(List<String>[])new List<?>[]{Arrays.asList(\"ok\")};return f.get(x)!=x&&f.get(x)[0].get(0).equals(\"ok\");}}");}
    @Test public void boxed_length() throws Exception {this.program("public class Case{interface F{String[]get(Integer n);}public static boolean main(){F f=String[]::new;return f.get(2).length==2;}}");}
    @Test public void generic_sam_length() throws Exception {this.program("public class Case{interface F<T,R>{R get(T n);}public static boolean main(){F<Integer,String[]>f=String[]::new;return f.get(2).length==2;}}");}
    @Test public void byte_length() throws Exception {this.program("public class Case{interface F{String[]get(Byte n);}public static boolean main(){F f=String[]::new;return f.get((byte)2).length==2;}}");}
    @Test public void char_length() throws Exception {this.program("public class Case{interface F{int[]get(Character n);}public static boolean main(){F f=int[]::new;return f.get((char)2).length==2;}}");}
    @Test public void short_length() throws Exception {this.program("public class Case{interface F{int[]get(short n);}public static boolean main(){F f=int[]::new;return f.get((short)2).length==2;}}");}
    @Test public void generic_array_length() throws Exception {this.program("import java.util.*;public class Case{interface F{List<?>[]get(Integer n);}public static boolean main(){F f=List<?>[]::new;return f.get(2).length==2;}}");}
    @Test public void null_length() throws Exception {this.program("public class Case{interface F{String[]get(Integer n);}public static boolean main(){F f=String[]::new;try{f.get(null);return false;}catch(NullPointerException expected){return true;}}}");}
    @Test public void negative_length() throws Exception {this.program("public class Case{interface F{String[]get(Integer n);}public static boolean main(){F f=String[]::new;try{f.get(-1);return false;}catch(NegativeArraySizeException expected){return true;}}}");}
    @Test public void void_array() throws Exception {this.program("public class Case{interface F{void get(Integer n);}public static boolean main(){F f=String[]::new;f.get(2);return true;}}");}
    @Test public void reject_long_length() throws Exception {this.reject("class Case{interface F{String[]get(Long n);}F f=String[]::new;}");}
    @Test public void reject_number_length() throws Exception {this.reject("class Case{interface F{String[]get(Number n);}F f=String[]::new;}");}
    @Test public void reject_boolean_length() throws Exception {this.reject("class Case{interface F{String[]get(Boolean n);}F f=String[]::new;}");}
    @Test public void reject_clone_result() throws Exception {this.reject("class Case{interface F{Integer[]get(String[]x);}F f=String[]::clone;}");}
    @Test public void diamond_target() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<String>x=new ArrayList<>(Arrays.asList(\"ok\"));return x.get(0).equals(\"ok\");}}");}
    @Test public void diamond_empty() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<String>x=new ArrayList<>();x.add(\"ok\");return x.get(0).equals(\"ok\");}}");}
    @Test public void diamond_method_argument() throws Exception {this.program("import java.util.*;public class Case{static boolean f(List<String>x){x.add(\"ok\");return x.get(0).equals(\"ok\");}public static boolean main(){return f(new ArrayList<>());}}");}
    @Test public void diamond_method_argument_collection() throws Exception {this.program("import java.util.*;public class Case{static boolean f(List<String>x){return x.get(0).equals(\"ok\");}public static boolean main(){return f(new ArrayList<>(Arrays.asList(\"ok\")));}}");}
    @Test public void diamond_nested() throws Exception {this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}public static boolean main(){B<B<String>>x=new B<>(new B<>(\"ok\"));return x.x.x.equals(\"ok\");}}");}
    @Test public void diamond_bound() throws Exception {this.program("public class Case{static class B<T extends Number>{}public static boolean main(){Object x=new B<>();return x!=null;}}");}
    @Test public void diamond_fbound_target() throws Exception {this.program("public class Case{static class B<T extends Comparable<T>>{}public static boolean main(){B<String>x=new B<>();return x!=null;}}");}
    @Test public void diamond_fbound_standalone() throws Exception {this.program("public class Case{static class B<T extends Comparable<T>>{}public static boolean main(){return new B<>()!=null;}}");}
    @Test public void diamond_shadowed_constructor() throws Exception {this.program("public class Case{static class B<T>{<T extends Number>B(T x){}}public static boolean main(){B<String>x=new B<>(1);return x!=null;}}");}
    @Test public void diamond_generic_constructor() throws Exception {this.program("public class Case{static class B<T>{T x;<U extends T>B(U x){this.x=x;}}public static boolean main(){B<Number>x=new B<>(1);return x.x.intValue()==1;}}");}
    @Test public void diamond_owner() throws Exception {this.program("public class Case{static class O<T>{class I<U extends T>{U x;I(U x){this.x=x;}}}public static boolean main(){O<Number>o=new O<>();O<Number>.I<Integer>x=o.new I<>(1);return x.x==1;}}");}
    @Test public void diamond_wildcard() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<? extends Number>x=new ArrayList<>(Arrays.asList(1));return x.get(0).intValue()==1;}}");}
    @Test public void diamond_conditional() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<String>x=true?new ArrayList<>():new LinkedList<>();x.add(\"ok\");return x.get(0).equals(\"ok\");}}");}
    @Test public void diamond_lambda_return() throws Exception {this.program("import java.util.*;public class Case{interface F{List<String>get();}public static boolean main(){F f=()->new ArrayList<>();List<String>x=f.get();x.add(\"ok\");return x.get(0).equals(\"ok\");}}");}
    @Test public void raw_constructor_valid() throws Exception {this.program("import java.util.*;public class Case{interface F{ArrayList<String>get(Collection<String>x);}public static boolean main(){F f=ArrayList::new;return f.get(Arrays.asList(\"ok\")).get(0).equals(\"ok\");}}");}
    @Test public void raw_constructor_empty() throws Exception {this.program("import java.util.*;public class Case{interface F{ArrayList<String>get();}public static boolean main(){F f=ArrayList::new;List<String>x=f.get();x.add(\"ok\");return x.get(0).equals(\"ok\");}}");}
    @Test public void raw_constructor_generic() throws Exception {this.program("public class Case{static class B<T>{T x;B(T x){this.x=x;}}interface F{B<String>get(String x);}public static boolean main(){F f=B::new;return f.get(\"ok\").x.equals(\"ok\");}}");}
    @Test public void reject_diamond_arguments() throws Exception {this.reject("import java.util.*;class Case{List<String>x=new ArrayList<>(Arrays.asList(1));}");}
    @Test public void reject_diamond_nested() throws Exception {this.reject("class Case{static class B<T>{T x;B(T x){this.x=x;}}B<B<String>>x=new B<>(new B<>(1));}");}
    @Test public void reject_diamond_bound() throws Exception {this.reject("class Case{static class B<T extends Number>{B(T x){}}B<String>x=new B<>(\"bad\");}");}
    @Test public void reject_raw_constructor() throws Exception {this.reject("import java.util.*;class Case{interface F{ArrayList<String>get(Collection<Integer>x);}F f=ArrayList::new;}");}
    @Test public void reject_raw_constructor_source() throws Exception {this.reject("class Case{static class B<T>{B(T x){}}interface F{B<String>get(Integer x);}F f=B::new;}");}
    @Test public void reject_diamond_method_argument() throws Exception {this.reject("import java.util.*;class Case{static void f(List<String>x){}void g(){f(new ArrayList<>(Arrays.asList(1)));}}");}
    @Test public void reject_diamond_explicit_constructor() throws Exception {this.reject("class Case{static class B<T>{<U extends T>B(U x){}}B<String>x=new <Integer>B<>(1);}");}
    @Test public void diamond_assignment_expression() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<String>x=null;Map<String,List<String>>m=new HashMap<>();m.put(\"k\",x=new ArrayList<>());x.add(\"ok\");return m.get(\"k\").get(0).equals(\"ok\");}}");}
    @Test public void diamond_inferred_receiver() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){return new ArrayList<>(Arrays.asList(\"ok\")).get(0).equals(\"ok\");}}");}
    @Test public void diamond_raw_argument() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){Collection raw=Arrays.asList(\"ok\");List<String>x=new ArrayList<>(raw);return x.get(0).equals(\"ok\");}}");}
    @Test public void diamond_multiple_variables() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){Map<String,Integer>x=new HashMap<>();x.put(\"ok\",3);return x.get(\"ok\")==3;}}");}
    @Test public void diamond_bound_dependency() throws Exception {this.program("public class Case{static class B<T,U extends T>{U x;B(U x){this.x=x;}}public static boolean main(){B<Number,Integer>x=new B<>(1);return x.x==1;}}");}
    @Test public void diamond_constructor_varargs() throws Exception {this.program("public class Case{static class B<T>{T x;B(T...x){this.x=x[0];}}public static boolean main(){B<String>x=new B<>(\"ok\",\"other\");return x.x.equals(\"ok\");}}");}
    @Test public void diamond_fbound_method_member() throws Exception {this.program("public class Case{static class B<T extends Comparable<T>>{T get(){return null;}}public static boolean main(){return new B<>().get()==null;}}");}
    @Test public void reject_raw_constructor_explicit_method_arguments() throws Exception {this.reject("public class Case{static class B<T>{T x;<U extends T>B(U x){this.x=x;}}interface F{B<Number>get(Integer x);}public static boolean main(){F f=B::<Integer>new;return f.get(3).x.intValue()==3;}}");}
    @Test public void raw_constructor_shadowed_type_parameter() throws Exception {this.program("public class Case{static class B<T>{<T extends Number>B(T x){}}interface F{B<String>get(Integer x);}public static boolean main(){F f=B::new;return f.get(3)!=null;}}");}
    @Test public void bound_reference_type_variable() throws Exception {this.program("public class Case{interface F<T,R>{R get(T x);}static class B{<T>T id(T x){return x;}}static<T>T f(T x){F<T,T>r=new B()::<T>id;return r.get(x);}public static boolean main(){return f(\"ok\").equals(\"ok\");}}");}
    @Test public void boxed_type_variable_array() throws Exception {this.program("public class Case{interface F<T>{String[]get(T n);}static<T extends Integer>F<T>f(){return String[]::new;}public static boolean main(){return Case.<Integer>f().get(2).length==2;}}");}
    @Test public void array_clone_object_result() throws Exception {this.program("public class Case{interface F{Object get(int[]x);}public static boolean main(){F f=int[]::clone;int[]x={2};return ((int[])f.get(x))[0]==2&&f.get(x)!=x;}}");}
    @Test public void wildcard_array_clone() throws Exception {this.program("import java.util.*;public class Case{interface F{List<?>[]get(List<?>[]x);}public static boolean main(){F f=List<?>[]::clone;List<?>[]x={Arrays.asList(\"ok\")};return f.get(x)!=x&&f.get(x)[0].get(0).equals(\"ok\");}}");}
    @Test public void reject_diamond_with_explicit_constructor_arguments() throws Exception {this.reject("class Case{static class B<T>{<U extends T>B(U x){}}B<Number>x=new <Integer>B<>(1);}");}
    @Test public void reject_diamond_assignment_expression() throws Exception {this.reject("import java.util.*;class Case{void f(){List<String>x=null;Object y=(x=new ArrayList<>(Arrays.asList(1)));}}");}
    @Test public void reject_diamond_inferred_write() throws Exception {this.reject("import java.util.*;class Case{void f(){new ArrayList<>(Arrays.asList(\"ok\")).add(1);}}");}
    @Test public void reject_raw_constructor_explicit_bound() throws Exception {this.reject("class Case{static class B<T>{<U extends T>B(U x){}}interface F{B<String>get(Integer x);}F f=B::<Integer>new;}");}
    @Test public void reject_reference_annotation_target() throws Exception {this.reject("import java.lang.annotation.*;@Target(ElementType.TYPE)@interface A{}class Case{interface F{String get();}F f=@A String::new;}");}
    @Test public void reject_reference_argument_annotation_target() throws Exception {this.reject("import java.lang.annotation.*;@Target(ElementType.METHOD)@interface A{}class Case{static<T>T id(T x){return x;}interface F{String get(String x);}F f=Case::<@A String>id;}");}
    @Test public void reject_array_explicit_type_arguments() throws Exception {
        // javac 8 accepts this extension; JLS 15.13 only allows ArrayType :: new.
        String source = "class Case{interface F{String[]get(int n);}F f=String[]::<String>new;}";
        if (this.isJdk && "1.8".equals(System.getProperty("java.specification.version"))) {
            ISimpleCompiler c = this.compilerFactory.newSimpleCompiler(); c.setSourceVersion(8); c.setTargetVersion(8); c.cook(source);
        } else this.reject(source);
    }
}
