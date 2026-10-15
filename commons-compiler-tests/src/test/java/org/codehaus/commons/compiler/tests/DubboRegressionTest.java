package org.codehaus.commons.compiler.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import org.codehaus.commons.compiler.*;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.CommonsCompilerTestSuite;
import util.TestUtil;

/** Source, binary dispatch and reflection regressions exposed by Dubbo 2.7.x. */
@RunWith(Parameterized.class)
public class DubboRegressionTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }
    public DubboRegressionTest(ICompilerFactory factory) throws Exception { super(factory); }

    private Map<String, byte[]> compile(boolean parameters, Resource... sources) throws Exception {
        ICompiler compiler = this.compilerFactory.newCompiler();
        compiler.setSourceVersion(8); compiler.setTargetVersion(8); compiler.setParameters(parameters);
        Map<String, byte[]> classes = new HashMap<>();
        compiler.setClassFileCreator(new MapResourceCreator(classes)); compiler.compile(sources);
        return classes;
    }

    private void program(String source, boolean parameters) throws Exception {
        Map<String, byte[]> classes = this.compile(parameters, new StringResource("Case.java", source));
        ClassLoader loader = new ResourceFinderClassLoader(new MapResourceFinder(classes), this.getClass().getClassLoader());
        Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
    }

    private void reject(String source) throws Exception {
        try { this.compile(false, new StringResource("Case.java", source)); Assert.fail("Must reject invalid source"); }
        catch (CompileException expected) {}
    }

    @Test public void genericDefaultDoesNotCountAsAbstractSam() throws Exception {
        this.program("interface Ordered extends Comparable<Ordered>{default int compareTo(Ordered x){return 7;}}interface Callback extends Ordered{void run()throws Throwable;}public class Case{public static String test()throws Throwable{final int[]n={0};Callback c=()->n[0]++;c.run();if(n[0]!=1||((Comparable<Ordered>)c).compareTo(c)!=7)throw new AssertionError();return \"OK\";}}", false);
    }

    @Test public void directGenericDefaultSam() throws Exception {
        this.program("interface Callback extends Comparable<Callback>{default int compareTo(Callback x){return 3;}void run();}public class Case{public static String test(){Callback c=()->{};return ((Comparable<Callback>)c).compareTo(c)==3?\"OK\":\"BAD\";}}", false);
    }

    @Test public void rejectTwoAbstractGenericSamMethods() throws Exception {
        this.reject("interface Callback extends Comparable<Callback>{void run();}public class Case{Callback c=()->{};}");
    }

    @Test public void rejectReabstractedDefaultSam() throws Exception {
        this.reject("interface Parent extends Comparable<Parent>{default int compareTo(Parent x){return 0;}}interface Callback extends Parent{int compareTo(Parent x);void run();}public class Case{Callback c=()->{};}");
    }

    @Test public void inaccessibleInheritedFieldDoesNotHideLexicalOuterField() throws Exception {
        this.program("import java.util.*;import java.lang.reflect.*;public class Case{private static final float DEFAULT_LOAD_FACTOR=0.5f;static class Cache extends LinkedHashMap<String,String>{Cache(){super(16,DEFAULT_LOAD_FACTOR);}}public static String test()throws Exception{Cache c=new Cache();c.put(\"k\",\"v\");Field f=HashMap.class.getDeclaredField(\"loadFactor\");f.setAccessible(true);if(f.getFloat(c)!=0.5f||!c.get(\"k\").equals(\"v\"))throw new AssertionError();return \"OK\";}}", false);
    }

    @Test public void privateParentFieldIsNotInherited() throws Exception {
        this.program("public class Case{private static final int x=7;static class Parent{private int x=2;}static class Child extends Parent{int value(){return x;}}public static String test(){return new Child().value()==7?\"OK\":\"BAD\";}}", false);
    }

    @Test public void parameterNamesAndFinalVarargsFlags() throws Exception {
        this.program("import java.lang.reflect.*;public class Case{public Case(final int number){}public static void method(final String name,long count,String... tags){}interface Api{void run(String input);}public static String test()throws Exception{Parameter[] p=Case.class.getMethod(\"method\",String.class,long.class,String[].class).getParameters();if(!p[0].isNamePresent()||!p[0].getName().equals(\"name\")||p[0].getModifiers()!=Modifier.FINAL||!p[1].getName().equals(\"count\")||!p[2].getName().equals(\"tags\")||!p[2].isVarArgs())throw new AssertionError();if(!Case.class.getConstructor(int.class).getParameters()[0].getName().equals(\"number\")||!Api.class.getMethod(\"run\",String.class).getParameters()[0].getName().equals(\"input\"))throw new AssertionError();return \"OK\";}}", true);
    }

    @Test public void parameterNamesAreOptIn() throws Exception {
        this.program("public class Case{public static void method(String name){}public static String test()throws Exception{return Case.class.getMethod(\"method\",String.class).getParameters()[0].isNamePresent()?\"BAD\":\"OK\";}}", false);
    }

    @Test public void syntheticConstructorParameterOffsets() throws Exception {
        this.program("import java.lang.reflect.*;public class Case{class Inner{Inner(final int count){}}enum E{A(1);E(int code){}}public static String test()throws Exception{Parameter[] i=Inner.class.getDeclaredConstructors()[0].getParameters();if(i.length!=2||!i[0].isImplicit()||!i[1].getName().equals(\"count\")||i[1].getModifiers()!=Modifier.FINAL)throw new AssertionError();Parameter[] e=E.class.getDeclaredConstructors()[0].getParameters();if(e.length!=3||!e[0].isSynthetic()||!e[1].isSynthetic()||!e[2].getName().equals(\"code\"))throw new AssertionError();return \"OK\";}}", true);
    }

    @Test public void enumAsSubclassInference() throws Exception {
        this.program("public class Case{enum Mode{OK}static Object parse(Class<?> type,String value){return Enum.valueOf(type.asSubclass(Enum.class),value);}public static String test(){return parse(Mode.class,\"OK\")==Mode.OK?\"OK\":\"BAD\";}}", false);
    }

    @Test public void parameterizedVarargsLambdaRetainsItsInputType() throws Exception {
        this.program("import java.util.function.*;public class Case{static boolean apply(Class<?> type,Predicate<Class<?>>...p){return p[0].test(type);}public static String test(){return apply(String.class,type->CharSequence.class.isAssignableFrom(type))?\"OK\":\"BAD\";}}", false);
    }

    @Test public void parameterizedVarargsConstructorLambdaRetainsItsInputType() throws Exception {
        this.program("import java.util.function.*;public class Case{static class C{boolean ok;C(Predicate<Class<?>>...p){ok=p[0].test(String.class);}}public static String test(){return new C(type->CharSequence.class.isAssignableFrom(type)).ok?\"OK\":\"BAD\";}}", false);
    }

    @Test public void genericInterfaceBridgeWorksWithJavacConsumer() throws Exception {
        Map<String, byte[]> classes = this.compile(false, new StringResource("Callback.java",
            "public interface Callback extends Comparable<Callback>{default int compareTo(Callback x){return 7;}void run();}"));
        Path root = Files.createTempDirectory("janino-default-bridge-");
        try {
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) Files.write(root.resolve(entry.getKey()), entry.getValue());
            Path consumer = root.resolve("Case.java");
            Files.write(consumer, ("public class Case implements Callback{public void run(){}public static String test(){Callback c=new Case();return ((Comparable<Callback>)c).compareTo(c)==7?\"OK\":\"BAD\";}}").getBytes("UTF-8"));
            Assert.assertEquals(0, javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-proc:none", "-classpath", root.toString(), "-d", root.toString(), consumer.toString()));
            try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, this.getClass().getClassLoader())) {
                Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
            }
        } finally {
            try (java.nio.file.DirectoryStream<Path> files = Files.newDirectoryStream(root)) {
                for (Path file : files) Files.delete(file);
            }
            Files.delete(root);
        }
    }

    @Test public void anonymousInitializerCheckedExceptionAndMetadata() throws Exception {
        this.program("import java.io.*;import java.util.*;public class Case{static int read(boolean fail)throws IOException{if(fail)throw new IOException(\"expected\");return 7;}static Object make(boolean fail)throws IOException{return new Object(){int x=read(fail);};}public static String test()throws Exception{Object o=make(false);if(!Arrays.asList(o.getClass().getDeclaredConstructors()[0].getExceptionTypes()).contains(IOException.class))throw new AssertionError();try{make(true);throw new AssertionError();}catch(IOException expected){return \"OK\";}}}", true);
    }

    @Test public void anonymousInitializerExceptionCaughtAtCreation() throws Exception {
        this.program("import java.io.*;public class Case{static int read()throws IOException{throw new IOException();}public static String test(){try{new Object(){int x=read();};throw new AssertionError();}catch(IOException expected){return \"OK\";}}}", false);
    }

    @Test public void rejectUncaughtAnonymousInitializerException() throws Exception {
        this.reject("import java.io.*;public class Case{static int read()throws IOException{return 7;}static Object make(){return new Object(){int x=read();};}}");
    }

    @Test public void staticImportIgnoresSameNamedInstanceOverloads() throws Exception {
        Map<String, byte[]> classes = this.compile(false,
            new StringResource("lib/Action.java", "package lib;public interface Action{void execute();static int execute(Action a){a.execute();return 7;}}"),
            new StringResource("Case.java", "import static lib.Action.execute;public class Case{public static String test(){final int[]n={0};return execute(()->n[0]++)==7&&n[0]==1?\"OK\":\"BAD\";}}"));
        ClassLoader loader = new ResourceFinderClassLoader(new MapResourceFinder(classes), this.getClass().getClassLoader());
        Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
    }

    @Test public void voidBlockLambdaChoosesRunnableOverCallable() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{static String choice;static <V>V write(Callable<V> c){choice=\"callable\";return null;}static void write(Runnable r){choice=\"OK\";r.run();}public static String test(){final int[]n={0};write(()->{n[0]++;n[0]++;});return n[0]==2?choice:\"BAD\";}}", false);
    }

    @Test public void throwingBlockLambdaCanProduceAValue() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{public static String test()throws Exception{Callable<String> c=()->{throw new IllegalStateException(\"OK\");};try{c.call();throw new AssertionError();}catch(IllegalStateException e){return e.getMessage();}}}", false);
    }

    @Test public void getClassCaptureSurvivesLambdaHelperDelegation() throws Exception {
        this.program("import java.lang.reflect.*;public class Case{interface F<T,R>{R apply(T value)throws Exception;}static <T,R>R execute(T t,F<T,R>f)throws Exception{return f.apply(t);}Method method()throws Exception{return execute(getClass(),cls->cls.getMethod(\"test\"));}public static String test()throws Exception{return new Case().method().getName().equals(\"test\")?\"OK\":\"BAD\";}}", false);
    }

    @Test public void sameFieldInheritedThroughDiamondIsUnambiguous() throws Exception {
        this.program("interface Root{String VALUE=\"OK\";}interface Left extends Root{}interface Right extends Root{}class Parent implements Left{}public class Case extends Parent implements Right{public static String test(){return Case.VALUE;}}", false);
    }

    @Test public void distinctInheritedFieldsRemainAmbiguous() throws Exception {
        this.reject("interface Left{int VALUE=1;}interface Right{int VALUE=1;}public class Case implements Left,Right{int x=Case.VALUE;}");
    }

    @Test public void caseInsensitiveOutputDirectoryDoesNotAliasLocalNameToClass() throws Exception {
        Path root = Files.createTempDirectory("janino-class-case-");
        Map<String, byte[]> classes = this.compile(false, new StringResource("Widget.java", "public class Widget{public int value=1;}"));
        try {
            Files.write(root.resolve("Widget.class"), classes.get("Widget.class"));
            ICompiler compiler = this.compilerFactory.newCompiler();
            compiler.setSourceVersion(8); compiler.setTargetVersion(8); compiler.setClassPath(new java.io.File[]{root.toFile()});
            classes = new HashMap<>(); compiler.setClassFileCreator(new MapResourceCreator(classes));
            compiler.compile(new Resource[]{
                new StringResource("Widget.java", "public class Widget{public int value=7;}"),
                new StringResource("Case.java", "public class Case{public static String test(){final int[]n={0};Runnable r=()->{Widget widget=new Widget();n[0]=widget.value;};r.run();return n[0]==7?\"OK\":\"BAD\";}}")});
            ClassLoader loader = new ResourceFinderClassLoader(new MapResourceFinder(classes), this.getClass().getClassLoader());
            Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
        } finally { Files.deleteIfExists(root.resolve("Widget.class")); Files.delete(root); }
    }

    @Test public void infiniteValueLambdasRemainLegal() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{public static String test(){Callable<String>a=()->{for(;;){}};Callable<String>b=()->{while(true){}};return a!=null&&b!=null?\"OK\":\"BAD\";}}", false);
    }

    @Test public void voidLoopAndBareReturnChooseRunnable() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{static String choice;static <V>V write(Callable<V> c){choice=\"BAD\";return null;}static void write(Runnable r){choice=\"OK\";r.run();}public static String test(){write(()->{for(int i=0;i<2;i++){}return;});return choice;}}", false);
    }

    @Test public void annotationValueUsesResolvedJdkConstantType() throws Exception {
        this.program("import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME)@interface A{int value();}@A(Integer.MIN_VALUE)public class Case{public static String test(){return Case.class.getAnnotation(A.class).value()==Integer.MIN_VALUE?\"OK\":\"BAD\";}}", false);
    }
    @Test public void rawConstructorErasesUnrelatedParameterizedFormals() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{static class Adapter<T>{CompletableFuture<?> future;Adapter(CompletableFuture<String> f){future=f;}}public static String test(){CompletableFuture<?> f=CompletableFuture.completedFuture(7);return new Adapter(f).future.join().equals(7)?\"OK\":\"BAD\";}}", false);
    }

    @Test public void parameterizedConstructorStillRejectsWrongArgument() throws Exception {
        this.reject("import java.util.concurrent.*;public class Case{static class Adapter<T>{Adapter(CompletableFuture<String> f){}}Object a=new Adapter<Integer>(CompletableFuture.completedFuture(7));}");
    }

    @Test public void doContinueLambdaChoosesRunnable() throws Exception {
        this.program("import java.util.concurrent.*;public class Case{static String choice;static <V>V write(Callable<V> c){choice=\"BAD\";return null;}static void write(Runnable r){choice=\"OK\";r.run();}public static String test(){write(()->{again:do{continue again;}while(false);});return choice;}}", false);
    }

    @Test public void staticImportFindsInheritedArrayField() throws Exception {
        Map<String, byte[]> classes = this.compile(false,
            new StringResource("lib/Parent.java", "package lib;public class Parent{public static final String[] VALUES={\"OK\"};}"),
            new StringResource("lib/Child.java", "package lib;public class Child extends Parent{}"),
            new StringResource("Case.java", "import static lib.Child.VALUES;public class Case{public static String test(){for(String value:VALUES)return value;return \"BAD\";}}"));
        ClassLoader loader = new ResourceFinderClassLoader(new MapResourceFinder(classes), this.getClass().getClassLoader());
        Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
    }

    @Test public void varargsConstructorConditionalUsesElementTarget() throws Exception {
        this.program("public class Case{String value;Case(int n,String...s){value=s[n];}public static String test(){boolean b=true;return new Case(1,\"unused\",b?\"OK\":\"BAD\").value;}}", false);
    }

    @Test public void genericInnerOfNonGenericOwnerHasValidSignature() throws Exception {
        this.program("import java.util.*;import java.lang.reflect.*;public class Case{class Inner<T>{}Map<String,Inner<?>> values;Inner<String> method(Inner<Integer> input){return null;}public static String test()throws Exception{ParameterizedType map=(ParameterizedType)Case.class.getDeclaredField(\"values\").getGenericType();ParameterizedType inner=(ParameterizedType)map.getActualTypeArguments()[1];if(inner.getRawType()!=Inner.class||inner.getOwnerType()!=Case.class)throw new AssertionError();Method m=Case.class.getDeclaredMethod(\"method\",Inner.class);return ((ParameterizedType)m.getGenericReturnType()).getActualTypeArguments()[0]==String.class&&((ParameterizedType)m.getGenericParameterTypes()[0]).getActualTypeArguments()[0]==Integer.class?\"OK\":\"BAD\";}}", false);
    }

}
