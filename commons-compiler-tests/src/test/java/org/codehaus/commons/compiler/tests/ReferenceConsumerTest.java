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

/** Erased reference consumers must preserve source checks and required runtime adaptations. */
@RunWith(Parameterized.class)
public class ReferenceConsumerTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public ReferenceConsumerTest(ICompilerFactory factory) throws Exception { super(factory); }

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
            Assert.fail("Must reject invalid reference operations");
        } catch (CompileException expected) {}
    }

    @Test public void cast_object() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Object x=(Object)Case.<Integer>value();return x.toString();}}");
    }

    @Test public void reference_equality() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value()==(Object)\"ok\"?\"ok\":\"bad\";}}");
    }

    @Test public void instanceof_object() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value() instanceof Object?\"ok\":\"bad\";}}");
    }

    @Test public void string_concat() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return \"\"+Case.<Integer>value();}}");
    }

    @Test public void cast_serializable() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Serializable x=(Serializable)Case.<Integer>value();return x.toString();}}");
    }

    @Test public void cast_number_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{Number x=(Number)Case.<Integer>value();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void cast_parentheses() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Object x=(Object)((Case.<Integer>value()));return x.toString();}}");
    }

    @Test public void cast_conditional_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{try{Object x=(Object)(flag()?Case.<Integer>value():Case.<Integer>value());String unexpected = x.toString();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void cast_conditional_false_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return false;}public static String test()throws Exception{try{Object x=(Object)(flag()?Case.<Integer>value():Case.<Integer>value());String unexpected = x.toString();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void cast_conditional_constant_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{Object x=(Object)(true?Case.<Integer>value():null);String unexpected = x.toString();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void cast_intersection() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Object x=(Object & Serializable)Case.<Integer>value();return x.toString();}}");
    }

    @Test public void null_equality() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value()!=null?\"ok\":\"bad\";}}");
    }

    @Test public void null_equality_reverse() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return null!=Case.<Integer>value()?\"ok\":\"bad\";}}");
    }

    @Test public void same_boxed_references() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value()==Case.<Integer>value()?\"ok\":\"bad\";}}");
    }

    @Test public void reference_conditional_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{try{String unexpected = (flag()?Case.<Integer>value():null)==(Object)\"ok\"?\"ok\":\"bad\";return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void reference_parentheses() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return ((Case.<Integer>value()))!=(Object)\"bad\"?\"ok\":\"bad\";}}");
    }

    @Test public void instanceof_serializable() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value() instanceof Serializable?\"ok\":\"bad\";}}");
    }

    @Test public void instanceof_number() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value() instanceof Number?\"bad\":\"ok\";}}");
    }

    @Test public void instanceof_conditional_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{try{String unexpected = (flag()?Case.<Integer>value():null) instanceof Object?\"ok\":\"bad\";return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void concat_left() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return Case.<Integer>value()+\"\";}}");
    }

    @Test public void concat_long() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{String s=\"\"+Case.<Integer>value()+Case.<Integer>value()+Case.<Integer>value()+Case.<Integer>value();return \"okokokok\".equals(s)?\"ok\":\"bad\";}}");
    }

    @Test public void concat_conditional_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{try{String unexpected = \"\"+(flag()?Case.<Integer>value():null);return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void concat_parentheses() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return \"\"+((Case.<Integer>value()));}}");
    }

    @Test public void concat_compound() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{String s=\"\";s+=Case.<Integer>value();return s;}}");
    }

    @Test public void comparison_order() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static String trace=\"\";static<T>T next(String s){trace+=s;return (T)\"ok\";}public static String test()throws Exception{boolean b=Case.<Integer>next(\"a\")==Case.<Integer>next(\"b\");return b&&\"ab\".equals(trace)?\"ok\":\"bad\";}}");
    }

    @Test public void concat_order() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static String trace=\"\";static<T>T next(String s){trace+=s;return (T)\"ok\";}public static String test()throws Exception{String s=Case.<Integer>next(\"a\")+\"\"+Case.<Integer>next(\"b\");return \"okok\".equals(s)&&\"ab\".equals(trace)?\"ok\":\"bad\";}}");
    }

    @Test public void instanceof_once() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static String trace=\"\";static<T>T next(String s){trace+=s;return (T)\"ok\";}public static String test()throws Exception{boolean b=Case.<Integer>next(\"a\") instanceof Object;return b&&\"a\".equals(trace)?\"ok\":\"bad\";}}");
    }

    @Test public void cast_once() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static String trace=\"\";static<T>T next(String s){trace+=s;return (T)\"ok\";}public static String test()throws Exception{Object x=(Object)Case.<Integer>next(\"a\");return \"ok\".equals(x)&&\"a\".equals(trace)?\"ok\":\"bad\";}}");
    }

    @Test public void unbox_comparison_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{boolean b=Case.<Integer>value()==1;return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void unbox_arithmetic_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{String s=Case.<Integer>value()+1+\"\";return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void unbox_conditional_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{Object x=(Object)(true?Case.<Integer>value():1);return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void receiver_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{int x=Case.<Integer>value().intValue();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void argument_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static void take(Integer x){}public static String test()throws Exception{try{take(Case.<Integer>value());return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void assignment_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{try{Integer x=Case.<Integer>value();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void string_concat_required_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)Integer.valueOf(7);}public static String test()throws Exception{try{String s=\"\"+Case.<String>value();return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void string_concat_left_required_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)Integer.valueOf(7);}public static String test()throws Exception{try{String s=Case.<String>value()+\"\";return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void string_concat_null() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return null;}public static String test()throws Exception{return \"null\".equals(\"\"+Case.<Integer>value())?\"ok\":\"bad\";}}");
    }

    @Test public void string_concat_char_array() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)new char[]{'o','k'};}public static String test()throws Exception{String s=\"\"+Case.<Integer>value();return s.startsWith(\"[C@\")?\"ok\":\"bad\";}}");
    }

    @Test public void reject_cast_string() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Object x=(String)Case.<Integer>value();return \"bad\";}}");
    }

    @Test public void reject_comparison() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{boolean x=Case.<Integer>value()==\"ok\";return \"bad\";}}");
    }

    @Test public void reject_instanceof_string() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{boolean x=Case.<Integer>value() instanceof String;return \"bad\";}}");
    }

    @Test public void reject_cast_variable_bound() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<U extends Number>U value(){return (U)\"bad\";}public static String test()throws Exception{return \"bad\";}}");
    }

    @Test public void generic_receiver() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class B<T>{T value(){return (T)\"ok\";}}public static String test()throws Exception{return new B<Integer>().value() instanceof Object?\"ok\":\"bad\";}}");
    }

    @Test public void generic_receiver_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class B<T>{T value(){return (T)\"ok\";}}public static String test()throws Exception{Object x=(Object)new B<Integer>().value();return x.toString();}}");
    }

    @Test public void super_cast() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{String run(){Object x=(Object)super.<Integer>value();return x.toString();}}public static String test()throws Exception{return new B().run();}}");
    }

    @Test public void super_instanceof() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{String run(){return super.<Integer>value() instanceof Object?\"ok\":\"bad\";}}public static String test()throws Exception{return new B().run();}}");
    }

    @Test public void super_concat() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{String run(){return \"\"+super.<Integer>value();}}public static String test()throws Exception{return new B().run();}}");
    }

    @Test public void super_reference() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class A{<T>T value(){return (T)\"ok\";}}static class B extends A{String run(){return super.<Integer>value()==(Object)\"ok\"?\"ok\":\"bad\";}}public static String test()throws Exception{return new B().run();}}");
    }

    @Test public void erasure_number_failure() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T extends Number>T value(){return (T)(Object)\"ok\";}public static String test()throws Exception{try{boolean b=Case.<Integer>value() instanceof Object;return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void receiver_chain_required() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static class B<T>{T value(){return (T)Integer.valueOf(1);}}public static String test()throws Exception{try{boolean b=new B<String>().value().trim()==(Object)\"ok\";return \"bad\";}catch(ClassCastException e){return \"ok\";}}}");
    }

    @Test public void conditional_object_target() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{Object x=flag()?Case.<Integer>value():null;return x.toString();}}");
    }

    @Test public void conditional_object_branches() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{return \"\"+(flag()?(Object)Case.<Integer>value():null);}}");
    }

    @Test public void reject_primitive_instanceof() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{boolean b=1 instanceof Runnable;return \"bad\";}}");
    }

    @Test public void reject_primitive_null() throws Exception {
        this.reject("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{boolean b=1==null;return \"bad\";}}");
    }

    @Test public void null_instanceof() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return null instanceof Object?\"bad\":\"ok\";}}");
    }

    @Test public void numeric_conditional_object() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{Object x=flag()?Case.<Integer>value():Integer.valueOf(1);return x.toString();}}");
    }

    @Test public void boolean_conditional_object() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static boolean flag(){return true;}public static String test()throws Exception{Object x=flag()?Case.<Boolean>value():Boolean.FALSE;return x.toString();}}");
    }

    @Test public void interface_assignment() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{Serializable x=Case.<Integer>value();return x.toString();}}");
    }

    @Test public void interface_argument() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static String take(Serializable x){return x.toString();}public static String test()throws Exception{return take(Case.<Integer>value());}}");
    }

    @Test public void interface_return() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}static Serializable result(){return Case.<Integer>value();}public static String test()throws Exception{return result().toString();}}");
    }

    @Test public void number_assignment() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)Double.valueOf(2.5);}public static String test()throws Exception{Number x=Case.<Integer>value();return x.doubleValue()==2.5?\"ok\":\"bad\";}}");
    }

    @Test public void boxed_relational() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return new Integer(7)>new Byte((byte)5)?\"ok\":\"bad\";}}");
    }

    @Test public void boxed_bitwise() throws Exception {
        this.program("import java.util.*;import java.io.*;public class Case{static<T>T value(){return (T)\"ok\";}public static String test()throws Exception{return (new Long(7L)&12)==4?\"ok\":\"bad\";}}");
    }

}
