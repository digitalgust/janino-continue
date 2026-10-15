package org.codehaus.commons.compiler.tests;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.CommonsCompilerTestSuite;
import util.TestUtil;

/** Java 8 regressions exposed by Commons Lang and Spring Boot source builds. */
@RunWith(Parameterized.class)
public class ExternalProjectsRegressionTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name = "CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }
    public ExternalProjectsRegressionTest(ICompilerFactory factory) throws Exception { super(factory); }

    private void program(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8); compiler.setTargetVersion(8); compiler.cook(source);
        Assert.assertEquals("OK", compiler.getClassLoader().loadClass("Case").getMethod("test").invoke(null));
    }

    private void reject(String source) throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setSourceVersion(8); compiler.setTargetVersion(8);
        try { compiler.cook(source); Assert.fail("Must reject incompatible Java 8 source"); }
        catch (CompileException expected) {}
    }

    @Test public void finalLambdaParameters() throws Exception {
        this.program("import java.util.function.*; public class Case { public static String test(){IntUnaryOperator f=(final int x)->x+1; BiFunction<String,String,String> g=(final String x,final String y)->x+y; if(f.applyAsInt(2)!=3||!g.apply(\"a\",\"b\").equals(\"ab\"))throw new AssertionError();return \"OK\";} }");
    }

    @Test public void interfaceAndAnnotationEnums() throws Exception {
        this.program("public class Case { interface Banner {enum Mode{OFF,CONSOLE,LOG}} @interface A{enum Kind{ONE,TWO}} public static String test(){if(Banner.Mode.OFF.ordinal()!=0||A.Kind.TWO.ordinal()!=1)throw new AssertionError();return \"OK\";} }");
    }

    @Test public void inheritedMemberEnum() throws Exception {
        this.program("class Base{enum Kind{A}} class Sub extends Base{} public class Case{public static String test(){if(Sub.Kind.A!=Base.Kind.A)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void boxedFinalFieldInitializers() throws Exception {
        this.program("public class Case {static final Double D=-0d;static final Float F=-0f;static final Integer I=42;static final Object O=7;final Double instance=-0d; public static String test(){if(Double.doubleToRawLongBits(D)!=Long.MIN_VALUE||Float.floatToRawIntBits(F)!=Integer.MIN_VALUE||I.intValue()!=42||!O.equals(7)||Double.doubleToRawLongBits(new Case().instance)!=Long.MIN_VALUE)throw new AssertionError();return \"OK\";} }");
    }

    @Test public void nestedCollectorTargets() throws Exception {
        this.program("import java.util.*;import java.util.stream.*;public class Case{static List<String> f(List<String>x){return Collections.unmodifiableList(x.stream().map(String::trim).collect(Collectors.toList()));}public static String test(){List<String>x=f(Arrays.asList(\" x \"));if(!x.get(0).equals(\"x\"))throw new AssertionError();try{x.add(\"y\");throw new AssertionError();}catch(UnsupportedOperationException expected){}return \"OK\";}}");
    }

    @Test public void comparatorReferenceChain() throws Exception {
        this.program("import java.util.*;public class Case{static class Item{final String name;Item(String n){name=n;}boolean deprecated(){return false;}String name(){return name;}}static final Comparator<Item>C=Comparator.comparing(Item::deprecated).thenComparing(Item::name);public static String test(){if(C.compare(new Item(\"a\"),new Item(\"b\"))>=0)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void rawQualifierRetainsSamReceiverArguments() throws Exception {
        this.program("import java.util.*;public class Case{static String f(Map<String,String>x){return x.entrySet().stream().filter(e->e.getKey().equals(\"x\")).findFirst().map(Map.Entry::getValue).orElse(null);}public static String test(){if(!\"y\".equals(f(Collections.singletonMap(\"x\",\"y\")))||f(Collections.singletonMap(\"z\",\"y\"))!=null)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void rejectIncompatibleCollectorResult() throws Exception {
        this.reject("import java.util.*;import java.util.stream.*;public class Case{static List<String> f(){return Arrays.asList(1,2).stream().collect(Collectors.toList());}}");
    }

    @Test public void rejectIncompatibleOptionalReferenceResult() throws Exception {
        this.reject("import java.util.*;public class Case{static String f(Map<String,Integer>x){return x.entrySet().stream().findFirst().map(Map.Entry::getValue).orElse(null);}}");
    }

    @Test public void distinctWildcardCapturesRemainDistinct() throws Exception {
        this.reject("import java.util.*;public class Case{static void f(List<? extends Number>a,List<? extends Number>b){a.set(0,b.get(0));}}");
    }

    @Test public void enumArgumentsDoNotLeakIntoNestedConstructors() throws Exception {
        this.program("import java.util.function.*;public class Case{enum E{A(()->\"a\",new Object()),B(()->\"b\",new Object());final Supplier<String> f;final Object o;E(Supplier<String> f,Object o){this.f=f;this.o=o;}}public static String test(){if(!E.A.f.get().equals(\"a\")||!E.B.f.get().equals(\"b\")||E.A.o==E.B.o)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void lambdaHelpersDoNotCollideAcrossInheritance() throws Exception {
        this.program("import java.util.function.*;class Parent{static Supplier<String> a=()->\"parent\";}public class Case extends Parent{Supplier<String>b=()->\"child\";public static String test(){if(!a.get().equals(\"parent\")||!new Case().b.get().equals(\"child\"))throw new AssertionError();return \"OK\";}}");
    }

    @Test public void lambdaInSuperConstructorArgument() throws Exception {
        this.program("import java.util.function.*;class Parent{final String s;Parent(Supplier<String> s){this.s=s.get();}}public class Case extends Parent{Case(){super(()->\"OK\");}public static String test(){return new Case().s;}}");
    }

    @Test public void publicMethodOnInaccessibleJdkSuperclass() throws Exception {
        this.program("import java.util.function.*;public class Case{public static String test(){StringBuilder b=new StringBuilder(\"abc\");IntFunction<String> f=b::substring;if(!b.substring(1).equals(\"bc\")||!f.apply(2).equals(\"c\"))throw new AssertionError();return \"OK\";}}");
    }

    @Test public void getClassUsesReceiverErasure() throws Exception {
        this.program("class Parent{Class<? extends Parent> type(){return getClass();}}public class Case extends Parent{public static String test(){if(new Case().type()!=Case.class)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void typeVariableQualifierAndParameterPreserveBounds() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{enum E{A}static <T extends Enum<T>>String name(T t){Function<T,String> f=T::name;return f.apply(t);}public static String test(){if(!name(E.A).equals(\"A\"))throw new AssertionError();return \"OK\";}}");
    }

    @Test public void arrayVarargsAndInexactReference() throws Exception {
        this.program("import java.util.*;import java.util.stream.*;public class Case{public static String test(){String[] a={\"a\",\"b\"};List<String>x=Stream.of(a).collect(Collectors.toList());List<String>y=Stream.of(a,a).flatMap(Stream::of).collect(Collectors.toList());if(x.size()!=2||y.size()!=4||!y.get(3).equals(\"b\"))throw new AssertionError();return \"OK\";}}");
    }

    @Test public void genericComparatorAndIdentityCollector() throws Exception {
        this.program("import java.util.*;import java.util.function.*;import java.util.stream.*;public class Case{static <E,K>Map<K,E> map(Stream<E>s,Function<E,K>f){return s.collect(Collectors.toMap(f::apply,Function.identity()));}static <S,E>E[] sorted(S s,Function<S,E[]> f,Function<E,String>key){E[]r=f.apply(s);Arrays.sort(r,Comparator.comparing(key));return r;}public static String test(){String[] r=sorted(\"\",s->new String[]{\"b\",\"a\"},s->s);if(!r[0].equals(\"a\")||!map(Stream.of(\"aa\"),String::length).get(2).equals(\"aa\"))throw new AssertionError();return \"OK\";}}");
    }

    @Test public void declaredWildcardUpperBoundInArray() throws Exception {
        this.program("public class Case{interface F<E extends Throwable>{void run()throws E;}static void use(F<? extends Throwable>... fs)throws Throwable{for(F<?> f:fs)f.run();}public static String test()throws Throwable{final int[]n={0};F<?>[] fs={()->n[0]++};use(fs);if(n[0]!=1)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void diamondConstructorReferencesWithInheritedSam() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{static class C<A,R>{final Supplier<A> s;final BinaryOperator<A>b;final Function<A,R>f;C(Supplier<A>s,BinaryOperator<A>b,Function<A,R>f){this.s=s;this.b=b;this.f=f;}}static List<String>merge(List<String>a,List<String>b){a.addAll(b);return a;}public static String test(){C<List<String>,Integer> c=new C<>(ArrayList::new,Case::merge,List::size);List<String>a=c.s.get();a.add(\"x\");if(c.f.apply(c.b.apply(a,Arrays.asList(\"y\")))!=2)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void nestedLambdaUsesEnclosingBlockLocal() throws Exception {
        this.program("import java.util.*;import java.util.stream.*;public class Case{public static String test(){List<String>r=Stream.of(\"a\").flatMap(s->{String prefix=s+\"!\";return Stream.of(\"b\").map(t->prefix+t);}).collect(Collectors.toList());return r.get(0).equals(\"a!b\")?\"OK\":\"BAD\";}}");
    }

    @Test public void comparatorReferenceConstrainsNestedNaturalOrder() throws Exception {
        this.program("import java.util.*;public class Case{static class Item{final Date date;final int priority;Item(Date d,int p){date=d;priority=p;}Date date(){return date;}int priority(){return priority;}}static final Comparator<Item>C=Comparator.comparing(Item::date,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Comparator.comparingInt(Item::priority).reversed());public static String test(){Date d=new Date(0);if(C.compare(new Item(d,2),new Item(d,1))>=0||C.compare(new Item(null,2),new Item(d,1))<=0)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void castStreamCaptureToFreeVariable() throws Exception {
        this.program("import java.util.*;import java.util.stream.*;public class Case{static <E>Stream<E> f(Class<? super E>c,Stream<?>s){return (Stream<E>)s.filter(c::isInstance);}public static String test(){List<String>r=Case.<String>f(String.class,Stream.of(1,\"x\")).collect(Collectors.toList());return r.equals(Arrays.asList(\"x\"))?\"OK\":\"BAD\";}}");
    }

    @Test public void targetedConditionalGenericInvocation() throws Exception {
        this.program("import java.util.*;public class Case{static Map<String,Integer> f(boolean b){return b?Collections.emptyMap():Collections.singletonMap(\"x\",1);}public static String test(){if(!f(true).isEmpty()||f(false).get(\"x\")!=1)throw new AssertionError();return \"OK\";}}");
    }

    @Test public void covariantInheritedDefaultBridge() throws Exception {
        this.program("interface A{default Object get(){return \"wrong\";}}interface B extends A{default String get(){return \"OK\";}}public class Case implements B{public static String test(){A a=new Case();B b=new Case();if(!a.get().equals(b.get()))throw new AssertionError();return (String)a.get();}}");
    }

    @Test public void duplicateStaticImport() throws Exception {
        this.program("import static java.util.Arrays.asList;import static java.util.Arrays.asList;public class Case{public static String test(){return asList(\"OK\").get(0);}}");
    }

    @Test public void rejectNestedInvariantWildcardCast() throws Exception {
        this.reject("import java.util.*;public class Case{static List<List<String>> f(List<List<?>>x){return (List<List<String>>)x;}}");
    }

    @Test public void interfaceConstantsRemainUsableAcrossCompilations() throws Exception {
        this.binaryProgram(new Resource[] {new StringResource("Delimiter.java",
            "import java.lang.annotation.*;@Retention(RetentionPolicy.RUNTIME) public @interface Delimiter { String NONE=\"\"; String value(); }")}, null,
            "@Delimiter(Delimiter.NONE) public class Case { public static String test() { return Case.class.getAnnotation(Delimiter.class).value().isEmpty()?\"OK\":\"BAD\"; } }");
    }

    @Test public void unusedBinaryMembersMayReferenceAbsentOptionalDependency() throws Exception {
        this.binaryProgram(new Resource[] {
            new StringResource("Missing.java", "public class Missing {}"),
            new StringResource("Api.java", "public class Api {public Api(){}public Api(Missing x){}public Missing unused(){return null;}public String value(){return \"OK\";}}")
        }, "Missing.class", "public class Case {public static String test(){return new Api().value();}}");
    }

    @Test public void protectedMemberTypesWorkAcrossPackagesAndBinaries() throws Exception {
        this.binaryProgram(new Resource[] {new StringResource("lib/Parent.java",
            "package lib;public class Parent {protected static class Base {public Base(){}public String value(){return \"OK\";}}protected interface Handler {String value();}}")}, null,
            "public class Case extends lib.Parent {static class Child extends Base implements Handler {}public static String test(){return new Child().value();}}");
    }

    @Test public void concreteGenericSuperclassOverrideNeedsBridge() throws Exception {
        this.binaryProgram(new Resource[] {new StringResource("Parent.java",
            "public class Parent<T,R>{public R apply(T value){return null;}}")}, null,
            "public class Case extends Parent<String,Integer>{public Integer apply(String s){return s.length();}public static String test(){Parent<String,Integer> p=new Case();return p.apply(\"abc\")==3?\"OK\":\"BAD\";}}");
    }

    @Test public void unusedBinaryFieldMayReferenceAbsentOptionalDependency() throws Exception {
        this.binaryProgram(new Resource[] {
            new StringResource("Missing.java", "public class Missing {}"),
            new StringResource("Api.java", "public class Api {private Missing logger;public static final String VALUE=\"OK\";}")
        }, "Missing.class", "public class Case {public static String test(){return Api.VALUE;}}");
    }

    private void binaryProgram(Resource[] library, String omit, String source) throws Exception {
        Path root = Files.createTempDirectory("janino-external-binary-");
        Map<String, byte[]> classes = new HashMap<>();
        ICompiler first = this.compilerFactory.newCompiler();
        first.setSourceVersion(8); first.setTargetVersion(8);
        first.setClassFileCreator(new MapResourceCreator(classes)); first.compile(library);
        try {
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                if (entry.getKey().equals(omit)) continue;
                Path path = root.resolve(entry.getKey());
                Files.createDirectories(path.getParent()); Files.write(path, entry.getValue());
            }
            ICompiler second = this.compilerFactory.newCompiler();
            second.setSourceVersion(8); second.setTargetVersion(8); second.setClassPath(new File[]{root.toFile()});
            second.setClassFileCreator(new MapResourceCreator(classes));
            second.compile(new Resource[]{new StringResource("Case.java", source)});
            if (omit != null) classes.remove(omit);
            ClassLoader loader = new ResourceFinderClassLoader(new MapResourceFinder(classes), this.getClass().getClassLoader());
            Assert.assertEquals("OK", loader.loadClass("Case").getMethod("test").invoke(null));
        } finally {
            for (String name : classes.keySet()) Files.deleteIfExists(root.resolve(name));
            for (String name : classes.keySet()) {
                for (Path parent = root.resolve(name).getParent(); !parent.equals(root); parent = parent.getParent()) {
                    Files.deleteIfExists(parent);
                }
            }
            Files.delete(root);
        }
    }
}
