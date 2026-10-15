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

/** Constant-specific subclasses must preserve enum identity, construction and virtual dispatch. */
@RunWith(Parameterized.class) public class EnumConstantBodyTest extends CommonsCompilerTestSuite {
    @Parameters(name = "CompilerFactory={0}") public static List<Object[]> compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    public EnumConstantBodyTest(ICompilerFactory compilerFactory) throws Exception { super(compilerFactory); }

    private void assertEnumProgram(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook(source);
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("Case").getMethod("main").invoke(null));
    }

    @Test public void enum_body_virtual_dispatch() throws Exception {
        this.assertEnumProgram(
            "public class Case {interface Read{int get();}enum E implements Read {PLAIN(7),"
            + "SPECIAL(9){public int get(){return super.get()+1;}public String toString(){return \"special\";}};"
            + "final int number;E(int n){number=n;}public int get(){return number;}}"
            + "public static boolean main(){E value=E.SPECIAL;Read read=value;Read reference=value::get;"
            + "return E.PLAIN.get()==7 && value.get()==10 && read.get()==10 && reference.get()==10"
            + "&& value.ordinal()==1 && value.name().equals(\"SPECIAL\") && value.toString().equals(\"special\")"
            + "&& E.valueOf(\"SPECIAL\")==value && E.values()[1]==value && value.getClass()!=E.class"
            + "&& value.getDeclaringClass()==E.class;}}"
        );
    }

    @Test public void enum_body_abstract_methods() throws Exception {
        this.assertEnumProgram(
            "public class Case {enum E {PLUS{int apply(int x){return x+1;}},"
            + "MINUS{int apply(int x){return x-1;}};abstract int apply(int x);}"
            + "public static boolean main(){return E.PLUS.apply(4)==5 && E.MINUS.apply(4)==3"
            + "&& E.PLUS.name().equals(\"PLUS\") && E.MINUS.ordinal()==1;}}"
        );
    }

    @Test public void enum_body_initializers_and_constructor_chain() throws Exception {
        this.assertEnumProgram(
            "public class Case {static StringBuilder trace=new StringBuilder();enum E {"
            + "A(2L){int extra=(int)base+3;{extra++;trace.append(\"IA\");}long value(){return super.value()+extra;}},"
            + "B(3L,4){int extra=4;{trace.append(\"IB\");}long value(){return super.value()+extra;}};"
            + "final long base;E(long n){this(n,1);}E(long n,int more){base=n+more;trace.append('K').append(name());}"
            + "long value(){return base;}}public static boolean main(){return E.A.value()==10L && E.B.value()==11L"
            + "&& trace.toString().equals(\"KAIAKBIB\");}}"
        );
    }

    @Test public void enum_body_varargs_and_empty_body() throws Exception {
        this.assertEnumProgram(
            "public class Case {enum E {A(1,2){int value(){return sum+10;}},B(){},"
            + "C(new int[]{4,5}){int value(){return sum+20;}};final int sum;"
            + "E(int... ns){int n=0;for(int v:ns)n+=v;sum=n;}int value(){return sum;}}"
            + "public static boolean main(){return E.A.value()==13 && E.B.value()==0 && E.C.value()==29"
            + "&& E.B.getClass()!=E.class && E.B.ordinal()==1;}}"
        );
    }

    @Test public void enum_body_generic_bridge_and_nested_capture() throws Exception {
        this.assertEnumProgram(
            "public class Case {interface Get<T>{T get();}interface Read{int get();}enum E implements Get<Number> {"
            + "A(3){class Helper {int value(){return number+1;}}public Integer get(){"
            + "Read read=()->new Helper().value();return read.get();}},"
            + "B(8){public Integer get(){return number+2;}};final int number;E(int n){number=n;}"
            + "public Number get(){return -1;}}public static boolean main(){Get<Number> a=E.A,b=E.B;"
            + "return a.get().intValue()==4 && b.get().intValue()==10 && E.A.get().intValue()==4;}}"
        );
    }

    @Test public void enum_body_virtual_call_during_base_construction() throws Exception {
        this.assertEnumProgram(
            "public class Case {enum E {A{int initialized=7;int get(){return initialized;}},B;"
            + "final int observed;E(){observed=get();}int get(){return 9;}}"
            + "public static boolean main(){return E.A.observed==0 && E.A.get()==7 && E.B.observed==9;}}"
        );
    }

    @Test public void enum_body_inherited_abstract_methods() throws Exception {
        this.assertEnumProgram(
            "public class Case {interface Read{int read();}enum E implements Read {"
            + "A{public int read(){return 1;}},B{public int read(){return 2;}};}"
            + "public static boolean main(){Read a=E.A,b=E.B;return a.read()==1 && b.read()==2;}}"
        );
    }

    @Test public void enum_body_values_before_user_static_initialization() throws Exception {
        this.assertEnumProgram(
            "public class Case {enum E {A(7){int get(){return number+1;}},B(9);"
            + "final int number;E(int n){number=n;}int get(){return number;}"
            + "static final E[] snapshot=values();static final java.util.Map<String,E> map=new java.util.HashMap<>();"
            + "static int sum;static {for(E e:values()){sum+=e.get();map.put(e.name(),e);}}"
            + "static final E selected=valueOf(\"B\");}public static boolean main(){"
            + "return E.sum==17 && E.snapshot[0]==E.A && E.map.get(\"B\")==E.B && E.selected==E.B;}}"
        );
    }

    @Test public void enum_body_reject_missing_abstract_implementation() throws Exception {
        this.assertCompilationUnitUncookable("enum E {A{},B{int get(){return 2;}};abstract int get();}");
    }

    @Test public void enum_body_reject_plain_abstract_constant() throws Exception {
        this.assertCompilationUnitUncookable("enum E {A,B{int get(){return 2;}};abstract int get();}");
    }

    @Test public void enum_body_reject_bad_override_annotation() throws Exception {
        this.assertCompilationUnitUncookable("enum E {A{@Override int missing(){return 2;}};int get(){return 1;}}");
    }
}
