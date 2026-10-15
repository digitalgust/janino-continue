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
/** Regressions found compiling Elasticsearch production sources on JDK8. */
@RunWith(Parameterized.class)
public class ElasticsearchRegressionTest extends CommonsCompilerTestSuite {
    @Parameterized.Parameters(name="CompilerFactory={0}")
    public static List<Object[]> factories() throws Exception {return TestUtil.getCompilerFactoriesForParameters();}
    public ElasticsearchRegressionTest(ICompilerFactory factory) throws Exception {super(factory);}
    private void program(String source,String expected) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);c.cook(source);
        Assert.assertEquals(expected,c.getClassLoader().loadClass("Case").getMethod("test").invoke(null));
    }
    private void reject(String source) throws Exception {
        ISimpleCompiler c=this.compilerFactory.newSimpleCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        try{c.cook(source);Assert.fail("Must reject invalid lambda/inference constraints");}catch(CompileException expected){}
    }

    @Test public void nestedGenericConsumerUsesLaterTypedArgument() throws Exception {
        this.program("import java.util.function.*;public class Case{interface P<C,T>{T parse(C c);}static<V,F>BiConsumer<V,F>arg(){return(v,f)->{};}static class D<V,C>{<T>String call(BiConsumer<V,T>a,P<C,T>b){return b.parse(null).toString();}}static class Parser implements P<Void,String>{public String parse(Void c){return \"OK\";}}public static String test(){return new D<String,Void>().call(arg(),new Parser());}}","OK");
    }

    @Test public void nestedGenericConsumerUsesLaterTypedInvocation() throws Exception {
        this.program("import java.util.function.*;public class Case{interface P<C,T>{T parse(C c);}static<V,F>BiConsumer<V,F>arg(){return(v,f)->{};}static class D<V,C>{<T>String call(BiConsumer<V,T>a,P<C,T>b){return b.parse(null).toString();}}static P<Void,String>parser(){return c->\"OK\";}public static String test(){return new D<String,Void>().call(arg(),parser());}}","OK");
    }

    @Test public void nestedGenericConsumerArgumentOrderIsSymmetric() throws Exception {
        this.program("import java.util.function.*;public class Case{static<A,B>BiConsumer<A,B>arg(){return(a,b)->{};}static<T>String call(Class<T>type,BiConsumer<String,T>c){return type.getName();}public static String test(){return call(String.class,arg()).equals(\"java.lang.String\")?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void nestedGenericConstructorUsesSiblingType() throws Exception {
        this.program("import java.util.function.*;public class Case{static<A,B>BiConsumer<A,B>arg(){return(a,b)->{};}static class C<V>{String value;<T>C(BiConsumer<V,T>c,Class<T>type){value=type.getName();}}public static String test(){return new C<String>(arg(),String.class).value.equals(\"java.lang.String\")?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void nestedDiamondUsesSiblingType() throws Exception {
        this.program("import java.util.*;public class Case{static<T>String call(List<T>a,Class<T>b){a.add(b.cast(\"OK\"));return a.get(0).toString();}public static String test(){return call(new ArrayList<>(),String.class);}}","OK");
    }

    @Test public void conflictingProperSiblingTypesAreRejected() throws Exception {
        this.reject("import java.util.*;public class Case{static<T>void call(List<T>a,Class<T>b){}void run(){call(new ArrayList<Integer>(),String.class);}}");
    }
    @Test public void nestedCheckedCallbackFactoriesRetainContext() throws Exception {
        this.program("import java.util.function.*;public class Case{interface C<T,E extends Exception>{void accept(T t)throws E;}interface L<T>{void on(T t);static<T>L<T>wrap(C<T,? extends Exception>c,Consumer<Exception>f){return t->{try{c.accept(t);}catch(Exception e){f.accept(e);}};}}static class A{boolean ok(){return true;}}static class B{String value(){return \"OK\";}}static String result;static void first(L<A>l){l.on(new A());}static void second(L<B>l){l.on(new B());}static void fail(Exception e){throw new AssertionError(e);}public static String test(){first(L.wrap(a->{second(L.wrap(b->{result=b.value();},Case::fail));if(!a.ok())throw new AssertionError();},Case::fail));return result;}}","OK");
    }

    @Test public void checkedExceptionFromNestedArgumentIsStillInferred() throws Exception {
        this.program("import java.io.*;public class Case{interface C<E extends Exception>{void run()throws E;}static<E extends Exception>void run(C<E>c)throws E{c.run();}static<T>T arg()throws IOException{throw new IOException(\"OK\");}static<T>void consume(T t){}public static String test(){try{run(()->consume(arg()));throw new AssertionError();}catch(IOException e){return e.getMessage();}}}","OK");
    }

    @Test public void uncaughtCheckedExceptionFromNestedArgumentIsRejected() throws Exception {
        this.reject("import java.io.*;public class Case{interface C<E extends Exception>{void run()throws E;}static<E extends Exception>void run(C<E>c)throws E{c.run();}static<T>T arg()throws IOException{return null;}static<T>void consume(T t){}void bad(){run(()->consume(arg()));}}");
    }

    @Test public void genericSuperFieldRetainsConcreteMemberType() throws Exception {
        this.program("class Parent<T>{protected T value;Parent(T v){value=v;}}public class Case extends Parent<String>{Case(){super(\"OK\");}String read(){return super.value.substring(0);}public static String test(){return new Case().read();}}","OK");
    }

    @Test public void qualifiedGenericSuperFieldUsesEnclosingInstance() throws Exception {
        this.program("class Parent<T>{protected T value;Parent(T v){value=v;}}public class Case extends Parent<String>{Case(){super(\"OK\");}class Inner extends Parent<Integer>{Inner(){super(7);}String read(){return Case.super.value;}}public static String test(){Case c=new Case();return c.new Inner().read();}}","OK");
    }

    @Test public void wrongAssignmentToGenericSuperFieldIsRejected() throws Exception {
        this.reject("class Parent<T>{T value;}public class Case extends Parent<String>{void bad(){super.value=new Object();}}");
    }

    @Test public void inheritedInnerTypeRetainsParameterizedOwner() throws Exception {
        this.program("class Parent<T>{class Inner{T value;Inner(T v){value=v;}T get(){return value;}}}public class Case extends Parent<String>{String read(Inner i){return i.get().substring(0);}public static String test(){Case c=new Case();return c.read(c.new Inner(\"OK\"));}}","OK");
    }

    @Test public void inheritedInnerSignatureRetainsParameterizedOwner() throws Exception {
        this.program("import java.lang.reflect.*;class Parent<T>{class Inner{T get(){return null;}}}public class Case extends Parent<String>{void use(Inner i){}public static String test()throws Exception{ParameterizedType inner=(ParameterizedType)Case.class.getDeclaredMethod(\"use\",Parent.Inner.class).getGenericParameterTypes()[0];ParameterizedType owner=(ParameterizedType)inner.getOwnerType();return owner.getActualTypeArguments()[0]==String.class?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void constructorReferenceOverloadChecksArity() throws Exception {
        this.program("import java.util.function.*;public class Case{Case(){}Case(int n,String s){}static String call(Supplier<Case>s){return \"OK\";}static String call(Function<String,Supplier<Case>>f){return \"BAD\";}public static String test(){return call(Case::new);}}","OK");
    }

    @Test public void constructorReferenceOverloadChecksArgumentTypes() throws Exception {
        this.program("import java.util.function.*;public class Case{interface A{Case make(String s);}interface B{Case make(Integer n);}Case(String s){}static String call(A a){return \"OK\";}static String call(B b){return \"BAD\";}public static String test(){return call(Case::new);}}","OK");
    }

    @Test public void arrayReferenceOverloadChecksInputTypes() throws Exception {
        this.program("public class Case{interface A{String[]make(int n);}interface B{String[]make(String n);}static String call(A a){return a.make(2).length==2?\"OK\":\"BAD\";}static String call(B b){return \"BAD\";}public static String test(){return call(String[]::new);}}","OK");
    }

    @Test public void constructorReferenceRetainsGenericDiamondInference() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{static<T>List<T>call(Supplier<List<T>>s,Class<T>t){return s.get();}public static String test(){List<String>x=call(ArrayList::new,String.class);x.add(\"OK\");return x.get(0);}}","OK");
    }

    @Test public void missingConstructorReferenceIsRejected() throws Exception {
        this.reject("import java.util.function.*;public class Case{Case(int n){}Supplier<Case>s=Case::new;}");
    }

    @Test public void checkedConstructorReferenceRequiresThrowingSam() throws Exception {
        this.reject("import java.io.*;import java.util.function.*;public class Case{Case()throws IOException{}Supplier<Case>s=Case::new;}");
    }


    @Test public void nestedCallbackCaptureKeepsDeclaringBlock() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{interface C<T,E extends Exception>{void accept(T t)throws E;}interface L<T>{void on(T t);static<T>L<T>wrap(C<T,? extends Exception>c,Consumer<Exception>f){return t->{try{c.accept(t);}catch(Exception e){f.accept(e);}};}}static String result;static void first(L<String>l){l.on(\"OK\");}static void second(Map<String,String>m,L<String>l){l.on(\"key\");}static String combine(String key,Map<String,String>m,List<String>l){l.add(m.get(key));return l.get(0);}static void fail(Exception e){throw new AssertionError(e);}public static String test(){first(L.wrap(value->{Map<String,String>map=new HashMap<>();map.put(\"key\",value);second(map,L.wrap(key->{List<String>local=new ArrayList<>();result=combine(key,map,local);},Case::fail));},Case::fail));return result;}}","OK");
    }

    @Test public void supertypeEnclosingMemberTypesAreNotInherited() throws Exception {
        this.program("class Library{interface Primary{}interface Result{}}public class Case{static class Result{String value(){return \"OK\";}}class Worker implements Library.Primary{Result perform(){return new Result();}}public static String test(){Case c=new Case();return c.new Worker().perform().value();}}","OK");
    }

    @Test public void covariantSiblingStreamCanBroadenProvisionalLowerBound() throws Exception {
        this.program("import java.util.*;import java.util.stream.*;public class Case{static class Base extends RuntimeException{Base(){super(\"OK\");}}static class Sub extends Base{}static<T extends Throwable>void rethrow(List<T>errors)throws T{if(!errors.isEmpty())throw errors.get(0);}public static String test(){try{rethrow(Stream.concat(Stream.of(new Sub()),Stream.of(1).map(i->new Base())).collect(Collectors.toList()));return \"BAD\";}catch(Base e){return e.getMessage();}}}","OK");
    }


    @Test public void rawAnonymousSubclassInheritsGenericBridge() throws Exception {
        this.program("interface Value<T>{String write(T value);}interface Item<T>{String value();}abstract class Parent<T extends Item<T>>implements Value<T>{public String write(T t){return t.value();}abstract Object read();}public class Case implements Item<Case>{public String value(){return \"OK\";}public static String test(){Parent p=new Parent(){Object read(){return null;}};Value v=p;return v.write(new Case());}}","OK");
    }


    @Test public void nestedLambdaAssertionsUseLexicalClassStatus() throws Exception {
        this.program("import java.util.function.*;public class Case{static int evaluations;static boolean condition(){evaluations++;return false;}public static String test(){boolean enabled=false;assert enabled=true;Runnable outer=()->{Runnable inner=()->{assert condition():\"OK\";};inner.run();};try{outer.run();return !enabled&&evaluations==0?\"OK\":\"BAD\";}catch(AssertionError e){return enabled&&evaluations==1&&\"OK\".equals(e.getMessage())?\"OK\":\"BAD\";}}}","OK");
    }


    @Test public void anonymousSuperMethodRetainsEnclosingMethodVariables() throws Exception {
        this.program("import java.util.*;public class Case{static<K,V>Map<K,V>create(){return new HashMap<K,V>(){public V put(K k,V v){return super.put(k,v);}};}public static String test(){Map<String,String>m=create();m.put(\"key\",\"OLD\");return \"OLD\".equals(m.put(\"key\",\"OK\"))?m.get(\"key\"):\"BAD\";}}","OK");
    }


    @Test public void constructorReferenceWithCovariantReaderResult() throws Exception {
        this.program("import java.io.*;public class Case{interface Reader<T>{T read(String s)throws IOException;}static class Base{String value;}static class Sub extends Base{Sub(String s)throws IOException{value=s;}}static class Entry{Reader<?>r;<T extends Base>Entry(Class<T>t,Reader<? extends T>r){this.r=r;}}public static String test()throws Exception{Entry e=new Entry(Base.class,Sub::new);return ((Base)e.r.read(\"OK\")).value;}}","OK");
    }


    @Test public void arrayReferenceWithCovariantFactoryResult() throws Exception {
        this.program("import java.util.function.*;public class Case{public static String test(){IntFunction<? extends Object[]>f=String[]::new;return f.apply(2).length==2?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void targetDependentArgumentUsesStrictOverloadBeforeUnboxing() throws Exception {
        this.program("import java.util.*;public class Case{static<T>T value(){return (T)Arrays.asList(\"OK\");}public static String test(){Set<String>s=new HashSet<String>(value());return s.iterator().next();}}","OK");
    }

    @Test public void targetDependentArgumentChecksUnrelatedReferenceOverloads() throws Exception {
        this.reject("import java.util.*;public class Case{static<T>T value(){return null;}static void use(List<String>x){}static void use(Set<String>x){}void bad(){use(value());}}");
    }


    @Test public void comparisonWithLowerBoundedCapturedClassIsPossible() throws Exception {
        this.program("public class Case{static class Token<T>{Class<? super T>raw;Token(Class<? super T>c){raw=c;}Class<? super T>get(){return raw;}}static boolean matches(Token<?>t){return t.get()==Class.class;}public static String test(){return matches(new Token<Class>(Class.class))?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void comparisonWithIncompatibleProperLowerClassIsRejected() throws Exception {
        this.reject("public class Case{boolean bad(Class<? super Integer>c){return c==String.class;}}");
    }


    @Test public void anonymousSubclassForwardsVarargsAndFixedArray() throws Exception {
        this.program("public class Case{static class Parent{Object[]values;Parent(String s,Object...a){values=a;}int size(){return values.length;}}public static String test(){Object[]a={1,2};Parent zero=new Parent(\"x\"){};Parent many=new Parent(\"x\",1,2,3){};Parent array=new Parent(\"x\",a){};return zero.size()==0&&many.size()==3&&array.values==a?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void anonymousSubclassSelectsMoreSpecificVarargsConstructor() throws Exception {
        this.program("public class Case{static class Parent{String value;Parent(String s,Object...a){value=\"BAD\";}Parent(String s,Throwable t,Object...a){value=s+ a.length;}}public static String test(){Parent p=new Parent(\"OK\",new RuntimeException()){};return \"OK0\".equals(p.value)?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void binaryPrimitiveConstantsRetainDeclaredSourceTypes() throws Exception {
        java.util.Map<String,byte[]> classes=new java.util.HashMap<>();
        org.codehaus.commons.compiler.ICompiler c=this.compilerFactory.newCompiler();
        c.setSourceVersion(8);c.setTargetVersion(8);
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{new org.codehaus.commons.compiler.util.resource.StringResource("Flags.java","public class Flags{public static final boolean YES=true,NO=false;public static final byte B=7;public static final char C='x';public static final short S=32000;}")});
        c=this.compilerFactory.newCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        java.nio.file.Path binary=java.nio.file.Files.createTempDirectory("janino-binary-constants");
        java.nio.file.Files.write(binary.resolve("Flags.class"),classes.get("Flags.class"));
        c.setClassPath(new java.io.File[]{binary.toFile()});
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{new org.codehaus.commons.compiler.util.resource.StringResource("Case.java","public class Case{static final boolean YES=Flags.YES,NO=Flags.NO;public static String test(){return YES&&!NO&&\"x\".equals(\"\"+Flags.C)&&Flags.B==7&&Flags.S==32000?\"OK\":\"BAD\";}}")});
        ClassLoader loader=new org.codehaus.commons.compiler.util.ResourceFinderClassLoader(new org.codehaus.commons.compiler.util.resource.MapResourceFinder(classes),getClass().getClassLoader());
        Assert.assertEquals("OK",loader.loadClass("Case").getMethod("test").invoke(null));
        java.nio.file.Files.delete(binary.resolve("Flags.class"));java.nio.file.Files.delete(binary);
    }


    @Test public void inheritedErasedFBoundImplementationRemainsAnOverride() throws Exception {
        this.program("abstract class Parent<T extends Parent<T>>{abstract String read(T t);}abstract class Middle<T extends Middle<T>>extends Parent<T>{final String read(Middle t){return \"OK\";}}public class Case extends Middle<Case>{public static String test(){Parent<Case>p=new Case();return p.read(new Case());}}","OK");
    }

    @Test public void inheritedOverloadDoesNotImplementGenericAbstractMethod() throws Exception {
        this.reject("abstract class Parent<T>{abstract void read(T t);}abstract class Middle<T>extends Parent<T>{void read(String s){}}public class Case extends Middle<Integer>{}");
    }


    @Test public void inheritedErasedFBoundImplementationAcrossCompilationUnits() throws Exception {
        java.util.Map<String,byte[]> classes=new java.util.HashMap<>();
        org.codehaus.commons.compiler.ICompiler c=this.compilerFactory.newCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{
            new org.codehaus.commons.compiler.util.resource.StringResource("Parent.java","public abstract class Parent<T extends Parent<T>>{public abstract String read(T t);}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Middle.java","public abstract class Middle<T extends Middle<T>>extends Parent<T>{public final String read(Middle t){return \"OK\";}}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Case.java","public class Case extends Middle<Case>{public static String test(){Parent<Case>p=new Case();return p.read(new Case());}}")});
        ClassLoader loader=new org.codehaus.commons.compiler.util.ResourceFinderClassLoader(new org.codehaus.commons.compiler.util.resource.MapResourceFinder(classes),getClass().getClassLoader());
        Assert.assertEquals("OK",loader.loadClass("Case").getMethod("test").invoke(null));
    }


    @Test public void lambdaPreciselyRethrowsBroadCatchWithOnlyUncheckedBody() throws Exception {
        this.program("public class Case{interface C<E extends Exception>{void run()throws E;}static<E extends Exception>void run(C<E>c)throws E{c.run();}public static String test(){try{run(()->{try{throw new IllegalStateException(\"OK\");}catch(final Exception e){throw e;}});return \"BAD\";}catch(IllegalStateException e){return e.getMessage();}}}","OK");
    }

    @Test public void preciselyRethrowsSingleCatchCheckedType() throws Exception {
        this.program("import java.io.*;public class Case{interface C<E extends Exception>{void run()throws E;}static<E extends Exception>void run(C<E>c)throws E{c.run();}static void direct()throws IOException{try{throw new IOException(\"OK\");}catch(Exception e){throw e;}}public static String test(){try{run(()->{try{direct();}catch(Exception e){throw e;}});return \"BAD\";}catch(IOException e){return e.getMessage();}}}","OK");
    }

    @Test public void reassignedCatchParameterLosesPreciseRethrowType() throws Exception {
        this.reject("import java.io.*;public class Case{void bad()throws IOException{try{throw new IOException();}catch(Exception e){e=new Exception();throw e;}}}");
    }


    @Test public void varargsSamImplementationKeepsSingleArrayDimension() throws Exception {
        this.program("import java.util.*;public class Case{interface F{String call(String...a);}static String join(String...a){return Arrays.toString(a);}public static String test(){F ref=Case::join;F lambda=a->Arrays.toString(a);return \"[OK]\".equals(ref.call(\"OK\"))&&\"[A, B]\".equals(lambda.call(\"A\",\"B\"))?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void nestedWildcardRespectsImplicitDeclaredUpperBound() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{interface Provider<T extends Number>{T get();}static class Plugin{Map<String,Supplier<Provider<? extends Number>>>values(){return Collections.singletonMap(\"key\",()->()->7);}}static class Registry<T>{<P>String load(List<P>p,Function<P,Map<String,T>>f){return ((Provider)((Supplier)f.apply(p.get(0)).get(\"key\")).get()).get().toString();}}public static String test(){Registry<Supplier<Provider<?>>>r=new Registry<>();return \"7\".equals(r.load(Arrays.asList(new Plugin()),Plugin::values))?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void nestedProperWildcardMismatchStillRejectsInvariantAssignment() throws Exception {
        this.reject("import java.util.*;public class Case{void bad(List<List<String>>a){List<List<?>>b=a;}}");
    }


    @Test public void failedPrimitiveArrayOverloadDoesNotAbortReferenceArrayCandidate() throws Exception {
        this.program("import java.util.*;public class Case{static class Info{}public static String test(){Info[]a={new Info()};return Arrays.equals(a,Arrays.copyOf(a,1))?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void comparatorReferenceInfersMostSpecificCovariantReturn() throws Exception {
        this.program("import java.util.*;public class Case{interface Key{Object getKey();}static class Item implements Key{String name;Item(String s){name=s;}public String getKey(){return name;}}public static String test(){List<Item>a=new ArrayList<>(Arrays.asList(new Item(\"b\"),new Item(\"a\")));Collections.sort(a,Comparator.comparing(Item::getKey));return \"a\".equals(a.get(0).name)?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void abstractGenericOverrideRetainsNarrowerReturnDespiteSameErasure() throws Exception {
        this.program("import java.util.*;class Root{}class Leaf extends Root{String value=\"OK\";}interface Top{List<? extends Root>values();}abstract class Base<T extends Leaf>implements Top{public abstract List<T>values();String read(){return values().get(0).value;}}public class Case extends Base<Leaf>{public List<Leaf>values(){return Arrays.asList(new Leaf());}public static String test(){return new Case().read();}}","OK");
    }


    @Test public void rawInstanceFieldErasesSourceParameterizationInsideLambda() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case<T>{List<T>values=new ArrayList<>();static void fill(Consumer<? super Case>c){c.accept(new Case());}public static String test(){String[]result={null};fill(raw->{raw.values.add(\"OK\");result[0]=(String)raw.values.get(0);});return result[0];}}","OK");
    }


    @Test public void recursiveRawBoundIsIncorporatedBeforeNestedListenerTarget() throws Exception {
        this.program("import java.util.function.*;public class Case{interface R<T>{}interface L<T>{void accept(T t);}static<T>L<T>wrap(Consumer<T>c){return c::accept;}static<T extends R<T>>void run(T t,L<T>l){l.accept(t);}static String result;public static String test(){R<R>value=new R<R>(){};run(value,wrap(r->{result=r==value?\"OK\":\"BAD\";}));return result;}}","OK");
    }


    @Test public void wildcardSamGroundTypeIncludesDeclaredBound() throws Exception {
        this.program("public class Case{interface Factory<T extends Number>{T get();}interface Consumer<T extends Number>{String use(T t);}static Integer value(){return 7;}public static String test(){Factory<?>f=Case::value;Consumer<?>c=n->String.valueOf(n.intValue());return f.get().intValue()==7&&\"7\".equals(((Consumer)c).use(7))?\"OK\":\"BAD\";}}","OK");
    }


    @Test public void publicSubclassExposesPublicFieldsOfInaccessibleParent() throws Exception {
        java.util.Map<String,byte[]> classes=new java.util.HashMap<>();
        org.codehaus.commons.compiler.ICompiler c=this.compilerFactory.newCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{
            new org.codehaus.commons.compiler.util.resource.StringResource("p/Public.java","package p;class Hidden{public int value=1;public static String text=\"OLD\";}public class Public extends Hidden{}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Case.java","public class Case{public static String test(){p.Public instance=new p.Public();instance.value+=2;p.Public.text=\"OK\";return instance.value==3?p.Public.text:\"BAD\";}}")});
        ClassLoader loader=new org.codehaus.commons.compiler.util.ResourceFinderClassLoader(new org.codehaus.commons.compiler.util.resource.MapResourceFinder(classes),getClass().getClassLoader());
        Assert.assertEquals("OK",loader.loadClass("Case").getMethod("test").invoke(null));
    }


    @Test public void enhancedForVariableDoesNotShadowIterableField() throws Exception {
        this.program("import java.util.*;public class Case{List<String>values=Arrays.asList(\"O\",\"K\");int[]numbers={1,2};String read(){String s=\"\";for(String values:values)s+=values;int sum=0;for(int numbers:numbers)sum+=numbers;return sum==3?s:\"BAD\";}public static String test(){return new Case().read();}}","OK");
    }

    @Test public void enhancedForVariableCannotReferToItselfInIterable() throws Exception {
        this.reject("public class Case{void bad(){for(int n:new int[]{n}){}}}");
    }


    @Test public void mixedBooleanNumericConditionalBoxesBothBranches() throws Exception {
        this.program("import java.io.*;public class Case{static Object value(boolean b){return b?2.5d:true;}static String text(boolean b){return(b?2.5d:true).toString();}public static String test(){Serializable a=false?3:false;return value(true)instanceof Double&&value(false)instanceof Boolean&&\"2.5\".equals(text(true))&&\"true\".equals(text(false))&&a.equals(Boolean.FALSE)?\"OK\":\"BAD\";}}","OK");
    }

    @Test public void voidConditionalOperandIsRejected() throws Exception {
        this.reject("public class Case{static void nothing(){}Object bad(boolean b){return b?nothing():true;}}");
    }


    @Test public void covariantReturnUsesSecondaryTypeVariableBoundAndBridgeCast() throws Exception {
        this.program("import java.io.*;interface View{Serializable get();}class Box<T extends CharSequence & Serializable>implements View{T value;Box(T v){value=v;}public T get(){return value;}}public class Case{public static String test(){View v=new Box<String>(\"OK\");return v.get().toString();}}","OK");
    }

    @Test public void covariantReturnRequiresSecondaryBound() throws Exception {
        this.reject("import java.io.*;interface View{Serializable get();}public class Case<T extends CharSequence>implements View{public T get(){return null;}}");
    }


    @Test public void inheritedSecondaryReturnBoundAcrossCompilationUnits() throws Exception {
        java.util.Map<String,byte[]> classes=new java.util.HashMap<>();
        org.codehaus.commons.compiler.ICompiler c=this.compilerFactory.newCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{
            new org.codehaus.commons.compiler.util.resource.StringResource("View.java","public interface View{java.io.Serializable get();}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Box.java","public abstract class Box<T extends CharSequence & java.io.Serializable>implements View{T value;Box(T v){value=v;}public T get(){return value;}}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Case.java","public class Case extends Box<String>{Case(){super(\"OK\");}public static String test(){View v=new Case();return v.get().toString();}}")});
        ClassLoader loader=new org.codehaus.commons.compiler.util.ResourceFinderClassLoader(new org.codehaus.commons.compiler.util.resource.MapResourceFinder(classes),getClass().getClassLoader());
        Assert.assertEquals("OK",loader.loadClass("Case").getMethod("test").invoke(null));
    }


    @Test public void inheritedReturnIsCheckedInConcreteSubclassView() throws Exception {
        java.util.Map<String,byte[]> classes=new java.util.HashMap<>();
        org.codehaus.commons.compiler.ICompiler c=this.compilerFactory.newCompiler();c.setSourceVersion(8);c.setTargetVersion(8);
        c.setClassFileCreator(new org.codehaus.commons.compiler.util.resource.MapResourceCreator(classes));
        c.compile(new org.codehaus.commons.compiler.util.resource.Resource[]{
            new org.codehaus.commons.compiler.util.resource.StringResource("View.java","public interface View{java.io.Serializable get();}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Box.java","public abstract class Box<T>{T value;Box(T v){value=v;}public T get(){return value;}}"),
            new org.codehaus.commons.compiler.util.resource.StringResource("Case.java","public class Case extends Box<String>implements View{Case(){super(\"OK\");}public static String test(){View v=new Case();return v.get().toString();}}")});
        ClassLoader loader=new org.codehaus.commons.compiler.util.ResourceFinderClassLoader(new org.codehaus.commons.compiler.util.resource.MapResourceFinder(classes),getClass().getClassLoader());
        Assert.assertEquals("OK",loader.loadClass("Case").getMethod("test").invoke(null));
    }


    @Test public void nestedWildcardSamResultRetainsContainmentAndDeclaredBound() throws Exception {
        this.program("import java.util.*;import java.util.function.*;public class Case{interface Ends<T extends CharSequence>{Collection<T>get();}static class Holder{Ends<?>e;Holder(Ends<?>e){this.e=e;}Holder(Collection<Ends<?>>es){e=null;}}static<T>Supplier<T>singleton(Supplier<T>s){return s;}public static String test(){Supplier<Holder>s=singleton(()->new Holder(()->Arrays.asList(\"OK\")));return s.get().e.get().iterator().next().toString();}}","OK");
    }

    @Test public void nestedWildcardSamResultStillEnforcesDeclaredBound() throws Exception {
        this.reject("import java.util.*;public class Case{interface Ends<T extends CharSequence>{Collection<T>get();}Ends<?>bad=()->Arrays.asList(new Object());}");
    }


}
