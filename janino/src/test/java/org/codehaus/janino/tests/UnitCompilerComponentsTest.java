package org.codehaus.janino.tests;

import java.lang.annotation.Annotation;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;

import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.Unparser;
import org.codehaus.janino.util.DeepCopier;
import org.junit.Assert;
import org.junit.Test;

/** Integration coverage for the state boundaries of the extracted compiler components. */
public class UnitCompilerComponentsTest {

    @Test public void rootObjectHasNeitherSuperclassNorSelfReferencingSignature() throws Exception {
        org.codehaus.janino.Compiler compiler = new org.codehaus.janino.Compiler();
        org.codehaus.commons.compiler.util.resource.MapResourceCreator output = new org.codehaus.commons.compiler.util.resource.MapResourceCreator();
        compiler.setClassFileCreator(output);
        compiler.setClassFileFinder(org.codehaus.commons.compiler.util.resource.ResourceFinder.EMPTY_RESOURCE_FINDER);
        compiler.setTargetVersion(8);
        compiler.compile(new org.codehaus.commons.compiler.util.resource.Resource[] {
            new org.codehaus.commons.compiler.util.resource.StringResource("java/lang/Object.java", "package java.lang;public class Object{}")
        });
        org.codehaus.janino.util.ClassFile root = new org.codehaus.janino.util.ClassFile(
            new java.io.ByteArrayInputStream(output.getMap().get("java/lang/Object.class"))
        );
        Assert.assertEquals(0, root.superclass);
        Assert.assertNull(root.getSignatureAttribute());
    }

    @Test public void longBinaryScopeWalkRetainsEveryNodeAndRejectsRebinding() throws Exception {
        org.codehaus.commons.compiler.Location location = org.codehaus.commons.compiler.Location.NOWHERE;
        org.codehaus.janino.Java.Rvalue expression = new org.codehaus.janino.Java.IntegerLiteral(location, "1");
        for (int i = 0; i < 4000; i++) expression = new org.codehaus.janino.Java.BinaryOperation(location,
            expression, i % 2 == 0 ? "+" : "-", new org.codehaus.janino.Java.IntegerLiteral(location, "1"));
        org.codehaus.janino.Java.Block scope = new org.codehaus.janino.Java.Block(location);
        expression.setEnclosingScope(scope);
        expression.setEnclosingScope(scope);
        org.codehaus.janino.Java.Rvalue value = expression;
        while (value instanceof org.codehaus.janino.Java.BinaryOperation) {
            org.codehaus.janino.Java.BinaryOperation binary = (org.codehaus.janino.Java.BinaryOperation) value;
            Assert.assertSame(scope, binary.getEnclosingScope());
            Assert.assertSame(scope, binary.rhs.getEnclosingScope());
            value = binary.lhs;
        }
        Assert.assertSame(scope, value.getEnclosingScope());
        try {
            expression.setEnclosingScope(new org.codehaus.janino.Java.Block(location));
            Assert.fail("Must reject assigning another scope");
        } catch (org.codehaus.commons.compiler.InternalCompilerException expected) {}
        StringBuilder source = new StringBuilder("public class Case{public static String run(){assert true;return \"start\"");
        for (int i = 0; i < 700; i++) source.append("+\"x\"");
        source.append(";}}");
        Assert.assertEquals(705, ((String) compile(source.toString()).getMethod("run").invoke(null)).length());
    }

    @Test public void explicitPlatformSourceRebindsBinarySignaturesAndArrayObjectMethods() throws Exception {
        org.codehaus.janino.Compiler compiler = new org.codehaus.janino.Compiler();
        org.codehaus.commons.compiler.util.resource.MapResourceCreator output = new org.codehaus.commons.compiler.util.resource.MapResourceCreator();
        compiler.setClassFileCreator(output);
        compiler.setClassFileFinder(org.codehaus.commons.compiler.util.resource.ResourceFinder.EMPTY_RESOURCE_FINDER);
        compiler.setTargetVersion(8);
        compiler.compile(new org.codehaus.commons.compiler.util.resource.Resource[] {
            new org.codehaus.commons.compiler.util.resource.StringResource("java/io/File.java", "package java.io;public class File{public File(String path){}}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Case.java", "public class Case{void run()throws Exception{java.io.File f=new java.io.File(\"x\");new java.io.FileInputStream(f);String[]a={};synchronized(a){a.wait(1);}}}")
        });
        Assert.assertTrue(output.getMap().containsKey("java/io/File.class"));
        Assert.assertTrue(output.getMap().containsKey("Case.class"));
        // Reusing the compiler must return to ordinary platform delegation after replacing a platform input.
        compiler.compile(new org.codehaus.commons.compiler.util.resource.Resource[] {
            new org.codehaus.commons.compiler.util.resource.StringResource("Other.java", "public class Other{String run(){return new java.io.File(\"x\").getPath();}}")
        });
        Assert.assertTrue(output.getMap().containsKey("Other.class"));
    }

    @Test public void wildcardArrayBoundsSurviveCopyAndUnparse() throws Exception {
        String source = "public class Case<T>{Class<? extends T[]> a;Class<? super String[][]> b;"
            + "Class<? extends int[]> c;public static boolean run(){return true;}}";
        AbstractCompilationUnit unit = new Parser(new Scanner(null, new StringReader(source))).parseAbstractCompilationUnit();
        AbstractCompilationUnit copy = new DeepCopier().copyAbstractCompilationUnit(unit);
        StringWriter output = new StringWriter();
        Unparser unparser = new Unparser(output);
        unparser.unparseAbstractCompilationUnit(copy);
        unparser.close();
        Class<?> type = compile(output.toString());
        ParameterizedType a = (ParameterizedType) type.getDeclaredField("a").getGenericType();
        java.lang.reflect.WildcardType upper = (java.lang.reflect.WildcardType) a.getActualTypeArguments()[0];
        Assert.assertTrue(upper.getUpperBounds()[0] instanceof GenericArrayType);
        Assert.assertEquals(type.getTypeParameters()[0], ((GenericArrayType) upper.getUpperBounds()[0]).getGenericComponentType());
        ParameterizedType b = (ParameterizedType) type.getDeclaredField("b").getGenericType();
        Assert.assertEquals(String[][].class, ((java.lang.reflect.WildcardType) b.getActualTypeArguments()[0]).getLowerBounds()[0]);
        ParameterizedType c = (ParameterizedType) type.getDeclaredField("c").getGenericType();
        Assert.assertEquals(int[].class, ((java.lang.reflect.WildcardType) c.getActualTypeArguments()[0]).getUpperBounds()[0]);
    }

    @Test public void enumConstantBodiesSurviveCopyAndUnparse() throws Exception {
        String source = "public class Case {enum E {A{},B{int seed=3;class Helper{int get(){return seed+1;}}"
            + "int get(){return new Helper().get();}};int get(){return 1;}}"
            + "public static boolean run(){return E.A.getClass()!=E.class && E.B.get()==4;}}";
        AbstractCompilationUnit unit = new Parser(new Scanner(null, new StringReader(source))).parseAbstractCompilationUnit();
        AbstractCompilationUnit copy = new DeepCopier().copyAbstractCompilationUnit(unit);
        StringWriter output = new StringWriter();
        Unparser unparser = new Unparser(output);
        unparser.unparseAbstractCompilationUnit(copy);
        unparser.close();
        Class<?> type = compile(output.toString());
        Assert.assertEquals(Boolean.TRUE, type.getMethod("run").invoke(null));
    }

    @Test public void nestedTypesRestoreWideMethodAndResourceContexts() throws Exception {
        Class<?> type = compile(
            "public class Case {static StringBuilder trace=new StringBuilder();int seed;"
            + "interface Score<T>{T get();default int extra(){return 2;}static int offset(){return 3;}}"
            + "enum Mode {ONE(7),TWO(10);"
            + "final int number;Mode(int n){number=n;}int weight(){return number;}}"
            + "static class R implements AutoCloseable{public void close(){trace.append('C');}}"
            + "Case(){this(5);}Case(int n){seed=n;trace.append('K');}"
            + "long work(final long n){final long captured=n;"
            + "class Local implements Score<Long>{public Long get(){return seed+captured;}}"
            + "final Score<Long> score=new Local();try(R r=new R()){"
            + "Score<Long> anon=new Score<Long>(){public Long get(){return score.get()+Mode.TWO.weight();}};"
            + "return anon.get()+score.extra()+Score.offset();}}"
            + "public static String run(){return new Case().work(11L)+\":\"+trace+\":\"+Mode.ONE.weight();}}"
        );
        Assert.assertEquals("31:KC:7", type.getMethod("run").invoke(null));
    }

    @Test public void foldedConstantsRemainAvailableToStatementCompilation() throws Exception {
        Class<?> type = compile(
            "public class Case {public static String run(){final int limit=(int)(long)3;"
            + "long low=-9223372036854775808L;int minimum=-2147483648;String text=\"a\\n\\\"\";int sum=0;"
            + "for(int i=0;i<limit;i++){final int increment=1;sum+=increment;}"
            + "return sum+\":\"+((low<0 && minimum<0)?'Y':'N')+\":\"+text.length()+\":\"+(byte)(127+1);}}"
        );
        Assert.assertEquals("3:Y:3:-128", type.getMethod("run").invoke(null));
    }

    @Test public void labeledExitsRunExplicitCleanupAndFinallyExactlyOnce() throws Exception {
        Class<?> type = compile(
            "public class Case {static StringBuilder trace=new StringBuilder();"
            + "static class R implements AutoCloseable {int id;R(int id){this.id=id;}"
            + "public void close(){trace.append('C').append(id);}}"
            + "static int work(String key){int sum=0;outer:for(int i=0;i<3;i++){R r=new R(i);try{"
            + "synchronized(trace){switch(key){case \"FB\":if(i==0)continue outer;"
            + "if(i==2)break outer;sum+=10;break;case \"Ea\":sum+=100;break;default:sum++;}}"
            + "}finally{r.close();trace.append('F').append(i);}}return sum;}"
            + "public static String run(){int a=work(\"FB\");String first=trace.toString();trace.setLength(0);"
            + "int b=work(\"Ea\");return a+\":\"+first+\":\"+b+\":\"+trace;}}"
        );
        Assert.assertEquals("10:C0F0C1F1C2F2:300:C0F0C1F1C2F2", type.getMethod("run").invoke(null));
    }

    @Test public void methodContextsSurviveInitializersBridgesAndLambdaCompilation() throws Exception {
        Class<?> type = compile(
            "public class Case {"
            + "static long initial; static {initial=11L;} int seed=3; {seed+=2;} public Case(){seed+=2;}"
            + "interface F {long apply(int n);}"
            + "static abstract class Cell<T>{public abstract T get();}"
            + "static class StringCell extends Cell<String>{public String get(){return \"ok\";}}"
            + "private long value(int n){long total=seed;for(int i=0;i<n;i++)total+=i;"
            + "try{return total;}finally{seed++;}}"
            + "F build(int extra){return n->value(n)+extra;}"
            + "double scale(double a){double b=a+0.5;return b*2;}"
            + "public static String run(){Case c=new Case();F f=c.build(4);"
            + "return f.apply(3)+\":\"+c.seed+\":\"+c.scale(1.5)+\":\""
            + "+((Cell)new StringCell()).get()+\":\"+initial;}}"
        );
        Assert.assertEquals("14:8:4.0:ok:11", type.getMethod("run").invoke(null));
    }

    @Test public void declarationAnnotationsAndGenericSignaturesRemainSeparate() throws Exception {
        Class<?> type = compile(
            "import java.lang.annotation.*; import java.util.List;"
            + "public class Case<T extends Number & Comparable<T>> {"
            + "@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD})"
            + "@Repeatable(Marks.class) public @interface Mark {int value();}"
            + "@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD})"
            + "public @interface Marks {Mark[] value();}"
            + "@Mark(1) @Mark(2) public List<T> first; @Mark(3) @Mark(4) public List<String> second;"
            + "@Mark(5) @Mark(6) public <E extends CharSequence> E choose(E... values)"
            + "{return values[0];}}"
        );
        Assert.assertEquals(2, type.getTypeParameters()[0].getBounds().length);
        ParameterizedType first = (ParameterizedType) type.getField("first").getGenericType();
        Assert.assertEquals(type.getTypeParameters()[0], first.getActualTypeArguments()[0]);
        ParameterizedType second = (ParameterizedType) type.getField("second").getGenericType();
        Assert.assertEquals(String.class, second.getActualTypeArguments()[0]);
        assertMarks(type.getField("first").getDeclaredAnnotations(), 1, 2);
        assertMarks(type.getField("second").getDeclaredAnnotations(), 3, 4);
        Method method = type.getMethod("choose", CharSequence[].class);
        TypeVariable<Method> variable = method.getTypeParameters()[0];
        Assert.assertEquals(CharSequence.class, variable.getBounds()[0]);
        Assert.assertEquals(variable, method.getGenericReturnType());
        Assert.assertEquals(variable, ((GenericArrayType) method.getGenericParameterTypes()[0]).getGenericComponentType());
        Assert.assertTrue(method.isVarArgs());
        assertMarks(method.getDeclaredAnnotations(), 5, 6);
        Assert.assertEquals("ok", method.invoke(type.newInstance(), new Object[] { new CharSequence[] { "ok" } }));
    }

    private static Class<?> compile(String source) throws Exception {
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        compiler.cook(source);
        return compiler.getClassLoader().loadClass("Case");
    }

    private static void assertMarks(Annotation[] annotations, int first, int second) throws Exception {
        Assert.assertEquals(1, annotations.length);
        Annotation[] marks = (Annotation[]) annotations[0].annotationType().getMethod("value").invoke(annotations[0]);
        Assert.assertEquals(2, marks.length);
        Assert.assertEquals(first, marks[0].annotationType().getMethod("value").invoke(marks[0]));
        Assert.assertEquals(second, marks[1].annotationType().getMethod("value").invoke(marks[1]));
    }
}
