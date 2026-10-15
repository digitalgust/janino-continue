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
import org.junit.runners.Parameterized.Parameters;
import util.CommonsCompilerTestSuite;
import util.TestUtil;

/** Resource scope, cleanup order and exception identity, checked with both compilers. */
@RunWith(Parameterized.class) public class TryWithResourcesTest extends CommonsCompilerTestSuite {
    @Parameters(name = "CompilerFactory={0}") public static List<Object[]> compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    public TryWithResourcesTest(ICompilerFactory compilerFactory) throws Exception { super(compilerFactory); }

    /** Target 8 exercises StackMapTable verification as well as runtime semantics. */
    private void assertResourceProgram(String source, String className) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook(source);
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass(className).getMethod("main").invoke(null));
    }

    @Test public void twr_labeled_cleanup() throws Exception {
        this.assertResourceProgram(
            "public class Case {static StringBuilder trace=new StringBuilder();static class R implements AutoClos"
            + "eable {int id;R(int id){this.id=id;}public void close(){trace.append('C').append(id);}}static int wo"
            + "rk(String key){int sum=0;outer:for(int i=0;i<3;i++){try(R r=new R(i)){synchronized(trace){switch(key"
            + "){case \"FB\":if(i==0)continue outer;if(i==2)break outer;sum+=10;break;case \"Ea\":sum+=100;break;defaul"
            + "t:sum++;}}}finally{trace.append('F').append(i);}}return sum;}public static String run(){int a=work(\""
            + "FB\");String first=trace.toString();trace.setLength(0);int b=work(\"Ea\");return a+\":\"+first+\":\"+b+\":\"+"
            + "trace;}public static boolean main(){return \"10:C0F0C1F1C2F2:300:C0F0C1F1C2F2\".equals(run());}}", "Case");
    }

    @Test public void twr_resource_binding_capture() throws Exception {
        this.assertResourceProgram(
            "public class Case {interface Read {String get();}\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R a=new R(\"1\");R b=new R(a.id+\"2\")){Read c=()->a.id+b.id;if(!\"112\".equals(c.get()))return false;"
            + "}return \"O1O12C12C1\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_close_before_user_catch_finally() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R a=new R(\"1\");R b=new R(\"2\",new RuntimeException(\"x\"))){t.append(\"B\");}catch(RuntimeException e"
            + "){t.append(\"H\");}finally{t.append(\"F\");}return \"O1O2BC2C1HF\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_primary_and_suppressed() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException body=new RuntimeException(\"b\"),one=new RuntimeException(\"1\"),two=new RuntimeExcepti"
            + "on(\"2\");try(R a=new R(\"1\",one);R b=new R(\"2\",two)){throw body;}catch(RuntimeException e){return e==b"
            + "ody&&e.getSuppressed().length==2&&e.getSuppressed()[0]==two&&e.getSuppressed()[1]==one&&\"O1O2C2C1\".e"
            + "quals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_close_failure_primary() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException one=new RuntimeException(\"1\"),two=new RuntimeException(\"2\");try(R a=new R(\"1\",one);"
            + "R b=new R(\"2\",two)){t.append(\"B\");}catch(RuntimeException e){return e==two&&e.getSuppressed().length"
            + "==1&&e.getSuppressed()[0]==one&&\"O1O2BC2C1\".equals(t.toString());}return false;\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_initializer_failure() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic R fail(RuntimeException e){t.append(\"I\");throw e;}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException init=new RuntimeException(\"i\"),close=new RuntimeException(\"c\");try(R a=new R(\"1\",cl"
            + "ose);R b=fail(init)){return false;}catch(RuntimeException e){return e==init&&e.getSuppressed().lengt"
            + "h==1&&e.getSuppressed()[0]==close&&\"O1IC1\".equals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_return_close_throws_once() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic int work(){try(R a=new R(\"1\",new RuntimeException(\"x\"))){synchronized(t){return 7;}}catch(Runti"
            + "meException e){t.append(\"H\");return 9;}finally{t.append(\"F\");}}\n"
            + "public static boolean main() throws Exception {\n"
            + "return work()==9&&\"O1C1HF\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_finally_overrides() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic int work(boolean fail){try(R a=new R(\"1\",fail?new RuntimeException(\"x\"):null)){return 7;}finall"
            + "y{t.append(\"F\");return 8;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "if(work(false)!=8||!\"O1C1F\".equals(t.toString()))return false;t.setLength(0);return work(true)==8&&\""
            + "O1C1F\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_null_and_initialization_order() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R a=new R(\"1\");R b=null;R c=new R(\"3\")){t.append(\"B\");}finally{t.append(\"F\");}return \"O1O3BC3C1F"
            + "\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_nested_handler_finally_throws() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException finish=new RuntimeException(\"f\");try{try(R a=new R(\"1\")){try(R b=new R(\"2\")){throw "
            + "new RuntimeException(\"b\");}catch(RuntimeException e){t.append(\"H\");throw e;}}finally{t.append(\"F\");t"
            + "hrow finish;}}catch(RuntimeException e){return e==finish&&\"O1O2C2HC1F\".equals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_finally_overrides_loop_exit() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic int work(){outer:for(int i=0;i<2;i++){try(R a=new R(\"1\")){continue outer;}finally{t.append(\"F\")"
            + ";return 5;}}return 0;}\n"
            + "public static boolean main() throws Exception {\n"
            + "return work()==5&&\"O1C1F\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_wide_return() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic long work(){try(R a=new R(\"1\")){t.append(\"B\");return 1234567890123L;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "return work()==1234567890123L&&\"O1BC1\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_self_suppression() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException same=new RuntimeException(\"s\");try(R a=new R(\"1\");R b=new R(\"2\",same)){throw same;}"
            + "catch(IllegalArgumentException e){return \"O1O2C2C1\".equals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_reject_not_autocloseable() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static class R {public void close(){}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R()){}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_reassignment() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R(\"1\")){r=new R(\"2\");}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_catch_scope() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R(\"1\")){}catch(RuntimeException e){r.close();}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_finally_scope() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R(\"1\")){}finally{r.close();}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_forward_resource() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R a=b;R b=new R(\"2\")){}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_unhandled_close() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static class R implements AutoCloseable {public void close()throws java.io.IOException{}}\n"
            + "public static boolean main() {\n"
            + "try(R r=new R()){}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_duplicate_resource() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R(\"1\");R r=new R(\"2\")){}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_abrupt_multiple_close_failures() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic boolean work(){RuntimeException one=new RuntimeException(\"1\"),two=new RuntimeException(\"2\");try"
            + "(R a=new R(\"1\",one);R b=new R(\"2\",two)){return false;}catch(RuntimeException e){return e==two&&e.get"
            + "Suppressed().length==1&&e.getSuppressed()[0]==one;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "return work()&&\"O1O2C2C1\".equals(t.toString());\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_finally_throw_after_return() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic int work(RuntimeException finish){try(R a=new R(\"1\")){return 7;}finally{t.append(\"F\");throw finish;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "RuntimeException finish=new RuntimeException(\"f\");try{work(finish);return false;}catch(RuntimeExcept"
            + "ion e){return e==finish&&\"O1C1F\".equals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_ordinary_finally_not_recaught() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic int work(){try{return 7;}catch(RuntimeException e){t.append(\"H\");return 8;}finally{t.append(\"F\""
            + ");throw new RuntimeException(\"f\");}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try{work();return false;}catch(RuntimeException e){return \"F\".equals(t.toString());}\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_checked_close_caught() throws Exception {
        this.assertResourceProgram(
            "public class Case {\n"
            + "static class R implements AutoCloseable{public void close()throws java.io.IOException{throw new java"
            + ".io.IOException(\"closed\");}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=new R()){}catch(java.io.IOException e){return \"closed\".equals(e.getMessage());}return false;\n"
            + "}\n"
            + "}\n", "Case");
    }

    @Test public void twr_reject_self_reference_shadowing_field() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}st"
            + "atic R r=new R(\"field\");\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=r){}return true;\n"
            + "}\n"
            + "}\n");
    }

    @Test public void twr_reject_self_reference() throws Exception {
        this.assertCompilationUnitUncookable(
            "public class Case {\n"
            + "static StringBuilder t=new StringBuilder();static class R implements AutoCloseable {String id;Runtim"
            + "eException fail;R(String id){this.id=id;t.append(\"O\").append(id);}R(String id,RuntimeException fail)"
            + "{this(id);this.fail=fail;}public void close(){t.append(\"C\").append(id);if(fail!=null)throw fail;}}\n"
            + "public static boolean main() throws Exception {\n"
            + "try(R r=r){}return true;\n"
            + "}\n"
            + "}\n");
    }
}
