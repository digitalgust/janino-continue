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
/** Parameterized qualifiers must preserve their type arguments through reference resolution. */
@RunWith(Parameterized.class) public class ParameterizedMethodReferenceTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name="CompilerFactory={0}") public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    public ParameterizedMethodReferenceTest(ICompilerFactory f) throws Exception { super(f); }
    private void program(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);c.cook(source);
        Assert.assertEquals(Boolean.TRUE,c.getClassLoader().loadClass("Case").getMethod("main").invoke(null));
    }
    private void reject(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        try {c.cook(source);Assert.fail("Must reject invalid parameterized reference");}catch(CompileException expected){}
    }
    @Test public void list_size() throws Exception {this.program("import java.util.*;public class Case{interface F{int get(List<String>x);}public static boolean main(){F f=List<String>::size;return f.get(Arrays.asList(\"a\",\"b\"))==2;}}");}
    @Test public void list_get() throws Exception {this.program("import java.util.*;public class Case{interface F{String get(List<String>x,int i);}public static boolean main(){F f=List<String>::get;return f.get(Arrays.asList(\"ok\"),0).equals(\"ok\");}}");}
    @Test public void nested_arguments() throws Exception {this.program("import java.util.*;public class Case{interface F{List<String> get(List<List<String>>x,int i);}public static boolean main(){F f=List<List<String>>::get;return f.get(Arrays.asList(Arrays.asList(\"ok\")),0).get(0).equals(\"ok\");}}");}
    @Test public void map_arguments() throws Exception {this.program("import java.util.*;public class Case{interface F{List<Integer>get(Map<String,List<Integer>>x,String k);}public static boolean main(){F f=Map<String,List<Integer>>::get;Map<String,List<Integer>>m=new HashMap<>();m.put(\"k\",Arrays.asList(3));return f.get(m,\"k\").get(0)==3;}}");}
    @Test public void fully_qualified() throws Exception {this.program("public class Case{interface F{int get(java.util.List<String>x);}public static boolean main(){F f=java.util.List<String>::size;return f.get(java.util.Arrays.asList(\"a\"))==1;}}");}
    @Test public void owner_chain() throws Exception {this.program("public class Case{static class O<T>{class I<U>{T value;I(T t){value=t;}T get(){return value;}}}interface F{String get(O<String>.I<Integer>x);}public static boolean main(){F f=O<String>.I<Integer>::get;return f.get(new O<String>().new I<Integer>(\"ok\")).equals(\"ok\");}}");}
    @Test public void owner_no_inner_arguments() throws Exception {this.program("public class Case{static class O<T>{class I{T get(){return null;}}}interface F{String get(O<String>.I x);}public static boolean main(){F f=O<String>.I::get;return f.get(new O<String>().new I())==null;}}");}
    @Test public void inherited_member() throws Exception {this.program("public class Case{static class B<T>{T get(){return null;}}static class C<T>extends B<T>{}interface F{String get(C<String>x);}public static boolean main(){F f=C<String>::get;return f.get(new C<String>())==null;}}");}
    @Test public void constructor() throws Exception {this.program("import java.util.*;public class Case{interface F{ArrayList<String>get();}public static boolean main(){F f=ArrayList<String>::new;ArrayList<String>x=f.get();x.add(\"ok\");return x.get(0).equals(\"ok\");}}");}
    @Test public void generic_method() throws Exception {this.program("public class Case{static class B<T>{<U extends T>U id(U x){return x;}}interface F{Integer get(B<Number>b,Integer x);}public static boolean main(){F f=B<Number>::<Integer>id;return f.get(new B<Number>(),3)==3;}}");}
    @Test public void generic_constructor() throws Exception {this.program("public class Case{static class B<T>{T x;<U extends T>B(U x){this.x=x;}}interface F{B<Number>get(Integer x);}public static boolean main(){F f=B<Number>::<Integer>new;return f.get(3).x.intValue()==3;}}");}
    @Test public void reject_wildcard_read() throws Exception {this.reject("import java.util.*;public class Case{interface F{Number get(List<? extends Number>x,int i);}public static boolean main(){F f=List<? extends Number>::get;return f.get(Arrays.asList(3),0).intValue()==3;}}");}
    @Test public void reject_wildcard_lower_write() throws Exception {this.reject("import java.util.*;public class Case{interface F{boolean get(List<? super String>x,String v);}public static boolean main(){F f=List<? super String>::add;List<Object>x=new ArrayList<>();return f.get(x,\"ok\")&&x.get(0).equals(\"ok\");}}");}
    @Test public void overloaded_target() throws Exception {this.program("import java.util.*;public class Case{interface A{String get(List<String>x,int i);}interface B{String get(List<String>x,String i);}static String call(A f){return f.get(Arrays.asList(\"ok\"),0);}static String call(B f){return \"bad\";}public static boolean main(){return call(List<String>::get).equals(\"ok\");}}");}
    @Test public void annotated_arguments() throws Exception {this.program("import java.util.*;import java.lang.annotation.*;@Target(ElementType.TYPE_USE)@interface A{} public class Case{interface F{int get(List<String>x);}public static boolean main(){F f=List<@A String>::size;return f.get(Arrays.asList(\"ok\"))==1;}}");}
    @Test public void annotated_owner() throws Exception {this.program("import java.lang.annotation.*;@Target(ElementType.TYPE_USE)@interface A{} public class Case{static class O<T>{class I<U>{T get(){return null;}}}interface F{String get(O<String>.I<Integer>x);}public static boolean main(){F f=O<@A String>.I<@A Integer>::get;return f.get(new O<String>().new I<Integer>())==null;}}");}
    @Test public void leading_annotation() throws Exception {this.program("import java.util.*;import java.lang.annotation.*;@Target(ElementType.TYPE_USE)@interface A{} public class Case{interface F{int get(List<String>x);}public static boolean main(){F f=@A List<String>::size;return f.get(Arrays.asList(\"ok\"))==1;}}");}
    @Test public void reifiable_array() throws Exception {this.program("import java.util.*;public class Case{interface F{List<?>[]get(int n);}public static boolean main(){F f=List<?>[]::new;return f.get(2).length==2;}}");}
    @Test public void relational_shift_rollback() throws Exception {this.program("public class Case{public static boolean main(){int a=1,b=9,c=2;return a<b>>c&&a<b>>>c&&!(a>b<<c)&&a<(b>>c)+1;}}");}
    @Test public void relational_generic_rollback() throws Exception {this.program("import java.util.*;public class Case{public static boolean main(){List<String>x=new ArrayList<>();int a=1,b=2;return a<b&&b>a&&x.isEmpty();}}");}
    @Test public void reject_receiver_arguments() throws Exception {this.reject("import java.util.*;class Case{interface F{int get(List<Integer>x);}F f=List<String>::size;}");}
    @Test public void reject_result() throws Exception {this.reject("import java.util.*;class Case{interface F{Integer get(List<String>x,int i);}F f=List<String>::get;}");}
    @Test public void reject_write() throws Exception {this.reject("import java.util.*;class Case{interface F{boolean get(List<String>x,Integer i);}F f=List<String>::add;}");}
    @Test public void reject_wildcard_write() throws Exception {this.reject("import java.util.*;class Case{interface F{boolean get(List<String>x,String i);}F f=List<?>::add;}");}
    @Test public void reject_static_parameterized() throws Exception {this.reject("class Case{static class B<T>{static int size(){return 1;}}interface F{int get();}F f=B<String>::size;}");}
    @Test public void reject_owner_receiver() throws Exception {this.reject("class Case{static class O<T>{class I<U>{T get(){return null;}}}interface F{Integer get(O<Integer>.I<Integer>x);}F f=O<String>.I<Integer>::get;}");}
    @Test public void reject_undeclared_subclass_method() throws Exception {this.reject("class Case{static class B<T>{}static class C extends B<String>{int m(){return 1;}}interface F{int get(C x);}F f=B<String>::m;}");}
    @Test public void reject_nonreifiable_array() throws Exception {this.reject("import java.util.*;class Case{interface F{List<String>[]get(int n);}F f=List<String>[]::new;}");}
    @Test public void reject_generic_constructor_bound() throws Exception {this.reject("class Case{static class B<T>{<U extends T>B(U x){}}interface F{B<String>get(Integer x);}F f=B<String>::<Integer>new;}");}
    @Test public void reject_generic_method_bound() throws Exception {this.reject("class Case{static class B<T>{<U extends T>U id(U x){return x;}}interface F{Integer get(B<String>b,Integer x);}F f=B<String>::<Integer>id;}");}
    @Test public void reject_bad_type_arguments() throws Exception {this.reject("import java.util.*;class Case{interface F{int get(List<String>x);}F f=List<String,Integer>::size;}");}
    @Test public void reject_diamond_reference() throws Exception {this.reject("import java.util.*;class Case{interface F{int get(List<String>x);}F f=List<>::size;}");}
    @Test public void nested_wildcard_argument() throws Exception {this.program("import java.util.*;public class Case{interface F{List<?>get(List<List<?>>x,int i);}public static boolean main(){F f=List<List<?>>::get;List<List<?>>x=new ArrayList<>();x.add(Arrays.asList(\"ok\"));return f.get(x,0).get(0).equals(\"ok\");}}");}
    @Test public void source_type_variable() throws Exception {this.program("import java.util.*;public class Case{interface F<T>{T get(List<T>x,int i);}static <T>F<T>make(){return List<T>::get;}public static boolean main(){return Case.<String>make().get(Arrays.asList(\"ok\"),0).equals(\"ok\");}}");}
    @Test public void raw_reference_infers_receiver() throws Exception {this.program("import java.util.*;public class Case{interface F{String get(List<String>x,int i);}public static boolean main(){F f=List::get;return f.get(Arrays.asList(\"ok\"),0).equals(\"ok\");}}");}
    @Test public void subtype_receiver() throws Exception {this.program("import java.util.*;public class Case{interface F{String get(ArrayList<String>x,int i);}public static boolean main(){F f=List<String>::get;ArrayList<String>x=new ArrayList<>();x.add(\"ok\");return f.get(x,0).equals(\"ok\");}}");}
    @Test public void constructor_collection() throws Exception {this.program("import java.util.*;public class Case{interface F{ArrayList<String>get(Collection<String>x);}public static boolean main(){F f=ArrayList<String>::new;return f.get(Arrays.asList(\"ok\")).get(0).equals(\"ok\");}}");}
    @Test public void multidimensional_array() throws Exception {this.program("import java.util.*;public class Case{interface F{List<?>[][]get(int n);}public static boolean main(){F f=List<?>[][]::new;return f.get(2).length==2&&f.get(1)[0]==null;}}");}
    @Test public void void_constructor() throws Exception {this.program("import java.util.*;public class Case{interface F{void get();}public static boolean main(){F f=ArrayList<String>::new;f.get();return true;}}");}
    @Test public void nested_static_generic() throws Exception {this.program("public class Case{static class B<T>{T get(){return null;}}interface F{String get(B<String>x);}public static boolean main(){F f=Case.B<String>::get;return f.get(new B<String>())==null;}}");}
    @Test public void reject_constructor_return_arguments() throws Exception {this.reject("import java.util.*;class Case{interface F{ArrayList<Integer>get();}F f=ArrayList<String>::new;}");}
    @Test public void reject_constructor_argument() throws Exception {this.reject("import java.util.*;class Case{interface F{ArrayList<String>get(Collection<Integer>x);}F f=ArrayList<String>::new;}");}
    @Test public void reject_wildcard_constructor() throws Exception {this.reject("import java.util.*;class Case{interface F{ArrayList<?>get();}F f=ArrayList<?>::new;}");}
    @Test public void reject_raw_subclass_member() throws Exception {this.reject("class Case{static class B{}static class C extends B{int m(){return 1;}}interface F{int get(C x);}F f=B::m;}");}
    @Test public void reject_owner_wildcard() throws Exception {this.reject("class Case{static class O<T>{class I<U>{int get(){return 1;}}}interface F{int get(O<String>.I<Integer>x);}F f=O<?>.I<Integer>::get;}");}
    @Test public void reject_constructor_diamond() throws Exception {this.reject("import java.util.*;class Case{interface F{ArrayList<String>get();}F f=ArrayList<>::new;}");}
    @Test public void reject_array_result_arguments() throws Exception {this.reject("import java.util.*;class Case{interface F{List<String>[]get(int n);}F f=List<?>[]::new;}");}
    @Test public void reject_nested_diamond() throws Exception {this.reject("import java.util.*;class Case{interface F{int get(List<List<String>>x);}F f=List<List<>>::size;}");}
    @Test public void inner_constructor() throws Exception {this.program("public class Case{static class O<T>{T value;O(T v){value=v;}class I<U>{T get(){return value;}}interface F<X>{O<X>.I<Integer>get();}F<T>factory(){return O<T>.I<Integer>::new;}}public static boolean main(){return new O<String>(\"ok\").factory().get().get().equals(\"ok\");}}");}
}
