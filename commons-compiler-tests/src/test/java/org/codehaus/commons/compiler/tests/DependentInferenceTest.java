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

/** Executable regressions for dependent and mutually recursive inference bounds. */
@RunWith(Parameterized.class)
public class DependentInferenceTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public DependentInferenceTest(ICompilerFactory factory) throws Exception { super(factory); }

    private void program(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        compiler.cook(source);
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("Case").getMethod("main").invoke(null));
    }

    private void reject(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        try {
            compiler.cook(source);
            Assert.fail("Must reject incompatible dependent inference bounds");
        } catch (CompileException expected) {}
    }

    @Test public void dependent_diamond() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){Object b=new B<>();return b!=null;}}"
        );
    }

    @Test public void dependent_member_default() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){Number n=new B<>().get();return n==null;}}"
        );
    }

    @Test public void dependent_write_default() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){new B<>().set(1);return true;}}"
        );
    }

    @Test public void dependent_argument() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){return new B<>(7).get().intValue()==7;}}"
        );
    }

    @Test public void dependent_target() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){B<Integer,Number>b=new B<>(7);return b.get()==7;}}"
        );
    }

    @Test public void dependent_wildcard() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){B<? extends Number,? extends Number>b=new B<>();return b.get()==null;}}"
        );
    }

    @Test public void dependent_reverse_order() throws Exception {
        this.program(
            "public class Case{static class B<U extends Number,T extends U>{T x;B(T x){this.x=x;}T get(){return x;}}public static boolean main(){return new B<>(7).get().intValue()==7;}}"
        );
    }

    @Test public void dependent_chain() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends V,V extends Number>{T get(){return null;}}public static boolean main(){Number n=new B<>().get();return n==null;}}"
        );
    }

    @Test public void mutual_diamond() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){Object b=new B<>();return b!=null;}}"
        );
    }

    @Test public void mutual_target() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){B<String,String>b=new B<>(\"ok\");return b.get().equals(\"ok\");}}"
        );
    }

    @Test public void mutual_argument() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){return new B<>(\"ok\").get().equals(\"ok\");}}"
        );
    }

    @Test public void mutual_null_write() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){new B<>().set(null);return true;}}"
        );
    }

    @Test public void mutual_member() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){try{new B<>().get().compareTo(null);return false;}catch(NullPointerException e){return true;}}}"
        );
    }

    @Test public void mutual_wildcard_target() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){B<?,?>b=new B<>();return b.get()==null;}}"
        );
    }

    @Test public void mutual_method() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){Object x=foo();return x==null;}}"
        );
    }

    @Test public void mutual_method_target() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){String x=foo();return x==null;}}"
        );
    }

    @Test public void mutual_method_member() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){try{foo().compareTo(null);return false;}catch(NullPointerException e){return true;}}}"
        );
    }

    @Test public void mutual_method_argument() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>U foo(T x){return null;}public static boolean main(){String x=foo(\"ok\");return x==null;}}"
        );
    }

    @Test public void mutual_method_reference() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}interface F{Object get();}public static boolean main(){F f=Case::foo;return f.get()==null;}}"
        );
    }

    @Test public void mutual_constructor_reference() throws Exception {
        this.program(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}interface F{B<?,?>get();}public static boolean main(){F f=B::new;return f.get()!=null;}}"
        );
    }

    @Test public void mutual_generic_constructor() throws Exception {
        this.program(
            "public class Case{static class B{<T extends Comparable<U>,U extends Comparable<T>>B(){}}public static boolean main(){return new B()!=null;}}"
        );
    }

    @Test public void dependent_method() throws Exception {
        this.program(
            "public class Case{static<T extends U,U extends Number>U foo(T x){return x;}public static boolean main(){return foo(7).intValue()==7;}}"
        );
    }

    @Test public void dependent_method_no_target() throws Exception {
        this.program(
            "public class Case{static<T extends U,U extends Number>T foo(){return null;}public static boolean main(){Number n=foo();return n==null;}}"
        );
    }

    @Test public void direct_fbound_method() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<T>>T foo(){return null;}public static boolean main(){Object x=foo();return x==null;}}"
        );
    }

    @Test public void reject_dependent_argument() throws Exception {
        this.reject(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){Object x=new B<>(\"bad\");return x!=null;}}"
        );
    }

    @Test public void reject_dependent_target() throws Exception {
        this.reject(
            "public class Case{static class B<T extends U,U extends Number>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){B<Integer,Number>x=new B<>(2.5);return x!=null;}}"
        );
    }

    @Test public void reject_mutual_write() throws Exception {
        this.reject(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){new B<>().set(\"bad\");return true;}}"
        );
    }

    @Test public void reject_mutual_arguments() throws Exception {
        this.reject(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(T t,U u){return t;}public static boolean main(){Object x=foo(\"bad\",1);return x!=null;}}"
        );
    }

    @Test public void reject_mutual_explicit() throws Exception {
        this.reject(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){Object x=Case.<String,Integer>foo();return x!=null;}}"
        );
    }

    @Test public void reject_mutual_reference() throws Exception {
        this.reject(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>U foo(T x){return null;}interface F{Integer get(String x);}public static boolean main(){F f=Case::foo;return f.get(\"bad\")==null;}}"
        );
    }

    @Test public void mutual_three_variables() throws Exception {
        this.program(
            "public class Case{static class B<T extends Comparable<U>,U extends Comparable<V>,V extends Comparable<T>>{T get(){return null;}}public static boolean main(){Object b=new B<>();return b!=null;}}"
        );
    }

    @Test public void mutual_method_number_target() throws Exception {
        this.program(
            "public class Case{static<T extends Number & Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){Number x=foo();return x==null;}}"
        );
    }

    @Test public void mutual_method_interface_target() throws Exception {
        this.program(
            "public class Case{static<T extends Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){java.io.Serializable x=foo();return x==null;}}"
        );
    }

    @Test public void fbound_method_number_target() throws Exception {
        this.program(
            "public class Case{static<T extends Number & Comparable<T>>T foo(){return null;}public static boolean main(){Number x=foo();return x==null;}}"
        );
    }

    @Test public void mutual_secondary_bound() throws Exception {
        this.program(
            "public class Case{static<T extends Number & Comparable<U>,U extends Comparable<T>>T foo(){return null;}static int which(Number n){return 1;}static int which(Object n){return 2;}public static boolean main(){return which(foo())==1;}}"
        );
    }

    @Test public void reject_mutual_distinct_captures() throws Exception {
        this.reject(
            "public class Case{static class B<T extends U,U extends Comparable<T>>{T x;B(){} B(T x){this.x=x;}T get(){return x;}void set(T x){this.x=x;}}public static boolean main(){new B<>().set(new B<>().get());return true;}}"
        );
    }

    @Test public void reject_mutual_final_target() throws Exception {
        this.reject(
            "public class Case{static<T extends Number & Comparable<U>,U extends Comparable<T>>T foo(){return null;}public static boolean main(){String x=foo();return x==null;}}"
        );
    }

    @Test public void mutual_transitive_interface_target() throws Exception {
        this.program(
            "public class Case{static<T extends U,U extends Comparable<T>>T foo(){return null;}public static boolean main(){java.io.Serializable x=foo();return x==null;}}"
        );
    }
}
