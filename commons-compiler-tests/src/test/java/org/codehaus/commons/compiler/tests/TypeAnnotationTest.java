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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Tests for the JSR 308 "type annotations" in the supported declaration positions (FIELD, METHOD_RETURN,
 * METHOD_FORMAL_PARAMETER, THROWS), as they appear in the generated class files' "Runtime[In]visibleTypeAnnotations"
 * attributes.
 * <p>
 *   The class files are verified on the BYTE level (by parsing the attributes), because the Java-8 core reflection
 *   support for type annotations is incomplete (e.g. "Field.getAnnotatedType()" does not implement
 *   "AnnotatedParameterizedType").
 * </p>
 * <p>
 *   Notice: Type annotations in "Code"-attribute positions (local variables, casts, "instanceof", ...) are not yet
 *   implemented.
 * </p>
 */
@RunWith(Parameterized.class) public
class TypeAnnotationTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    TypeAnnotationTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    private static final String
    DECLS = (
        ""
        + "import java.lang.annotation.*;\n"
        + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface TA { String value(); }\n"
        + "public class TA1 {\n"
        + "    public java.util.List<@TA(\"f1\") String> f1;\n"
        + "    public java.util.List<@TA(\"comp\") String[]> f2;\n"
        + "    public java.util.Map<String, java.util.List<@TA(\"nested\") String>> f3;\n"
        + "    public java.util.List<? extends @TA(\"ext\") Number> f4;\n"
        + "    public java.util.List<? super @TA(\"sup\") Number> f5;\n"
        + "    public java.util.List<@TA(\"wild\") ?> f6;\n"
        + "    public @TA(\"arrcomp\") String[] f7;\n"
        + "    public @TA(\"plain\") String f8;\n"
        + "    public java.util.List<@TA(\"ret\") String> m(java.util.List<@TA(\"p0\") String> p, @TA(\"p1\") String q)"
        + " throws @TA(\"thr\") java.io.IOException {\n"
        + "        return null;\n"
        + "    }\n"
        + "}\n"
    );

    /**
     * The expected entries in the "RuntimeVisibleTypeAnnotations" attributes of class "TA1" and its members, in the
     * format "<var>member</var>|<var>targetType</var>|<var>targetIndex</var>|<var>path</var>|<var>value</var>".
     */
    private static final String[]
    EXPECTED = {
        "TA1.f1|FIELD|-1|TA0|value=f1",
        "TA1.f2|FIELD|-1|TA0,ARR|value=comp",
        "TA1.f3|FIELD|-1|TA1,TA0|value=nested",
        "TA1.f4|FIELD|-1|TA0,WILD|value=ext",
        "TA1.f5|FIELD|-1|TA0,WILD|value=sup",
        "TA1.f6|FIELD|-1|TA0|value=wild",
        "TA1.f7|FIELD|-1|ARR|value=arrcomp",
        "TA1.f8|FIELD|-1||value=plain",
        "TA1.m|METHOD_RETURN|-1|TA0|value=ret",
        "TA1.m|METHOD_FORMAL_PARAMETER|0|TA0|value=p0",
        "TA1.m|METHOD_FORMAL_PARAMETER|1||value=p1",
        "TA1.m|THROWS|0||value=thr",
    };

    @Test public void
    testDeclarationPositionTypeAnnotations() throws Exception {
        org.junit.Assume.assumeTrue(this.compilerFactory.getClass().getName().contains("janino"));  // Byte-level assertions about JANINO's output.

        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.setTargetVersion(8);
        sc.cook(DECLS);

        byte[] classFileBytes = TypeAnnotationTest.classFileBytes(sc.getClassLoader(), "TA1");

        List<String> actual   = TypeAnnotationTest.parseTypeAnnotations(classFileBytes);
        List<String> expected = new ArrayList<>(Arrays.asList(TypeAnnotationTest.EXPECTED));
        Collections.sort(expected);

        Assert.assertEquals("Wrong type annotations in generated class file", expected.toString(), actual.toString());
    }

    /** Type annotations must NOT be emitted for target versions below 8. */
    @Test public void
    testNoTypeAnnotationsForOldTargetVersion() throws Exception {
        org.junit.Assume.assumeTrue(this.compilerFactory.getClass().getName().contains("janino"));

        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.setTargetVersion(7);
        sc.cook(DECLS);

        byte[] classFileBytes = TypeAnnotationTest.classFileBytes(sc.getClassLoader(), "TA1");

        Assert.assertEquals(
            "No type annotations expected for target version 7",
            "[]",
            TypeAnnotationTest.parseTypeAnnotations(classFileBytes).toString()
        );
    }

    @Test public void testCodeTypeAnnotations() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.setTargetVersion(8);
        sc.setDebuggingInformation(false, false, false);
        sc.cook("import java.lang.annotation.*;"
            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface A{}"
            + "class R implements AutoCloseable{public void close(){}}"
            + "public class TA1 {public static boolean main() throws Exception{Object x=\"ok\";"
            + "@A String local=(@A String)x;boolean b=x instanceof @A String;Object y=new @A String();"
            + "try(@A AutoCloseable r=new R()){if(y==null)throw new RuntimeException();}"
            + "catch(@A RuntimeException e){return false;}return b && local.equals(\"ok\");}}");
        Assert.assertEquals(Boolean.TRUE, sc.getClassLoader().loadClass("TA1").getMethod("main").invoke(null));
        List<String> annotations = parseTypeAnnotations(classFileBytes(sc.getClassLoader(), "TA1"), !this.isJdk);
        for (String target : new String[] {"0x40", "0x41", "0x42", "0x43", "0x44", "0x47"}) {
            boolean found = false;
            for (String annotation : annotations) if (annotation.contains("|" + target + "|")) found = true;
            Assert.assertTrue("Missing code annotation " + target + ": " + annotations, found);
        }
    }


    @Test public void testArrayCreationAndExplicitArguments() throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook("import java.lang.annotation.*;@Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface A{}"
            + "public class TA1{<T>TA1(T x){}static <T>T id(T x){return x;}public static boolean main(){"
            + "String[][] a=new @A String @A[2] @A[];String[] b=new @A String @A[]{\"ok\"};"
            + "TA1 c=new <@A String>TA1(TA1.<@A String>id(\"ok\"));return a.length==2 && b[0].equals(\"ok\") && c!=null;}}");
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("TA1").getMethod("main").invoke(null));
        List<String> annotations = parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), "TA1"), !this.isJdk);
        for (String target : new String[] {"0x44", "0x48", "0x49"}) {
            boolean found = false;
            for (String annotation : annotations) if (annotation.contains("|" + target + "|")) found = true;
            Assert.assertTrue("Missing annotation " + target + ": " + annotations, found);
        }
    }

    @Test public void testOwnerTypeAnnotationPaths() throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.setTargetVersion(8);
        compiler.cook("import java.lang.annotation.*;@Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface A{String value();}"
            + "public class TA1{static class O<T>{class I<U>{}}public O<@A(\"outer\") String>.@A(\"inner\") I<@A(\"argument\") Integer> value;}");
        Assert.assertEquals(Arrays.asList(
            "TA1.value|FIELD|-1|INNER,TA0|value=argument", "TA1.value|FIELD|-1|INNER|value=inner", "TA1.value|FIELD|-1|TA0|value=outer"),
            parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), "TA1")));
    }

    @Test public void testRemainingCreationAndCastAnnotations() throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler(); compiler.setTargetVersion(8);
        compiler.cook("import java.lang.annotation.*;@Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface A{String value();}"
            + "public class TA1{static class N extends Number implements Runnable{public int intValue(){return 0;}public long longValue(){return 0;}"
            + "public float floatValue(){return 0;}public double doubleValue(){return 0;}public void run(){}}"
            + "static class O<T>{class I<U>{I(U x){}}}static class Base{<T>T id(T x){return x;}}"
            + "public class Child extends Base{String call(){return super.<@A(\"super\") String>id(\"ok\");}}"
            + "public static Class<?> make(){return new @A(\"anonymous\") Object(){}.getClass();}"
            + "public static boolean main(){Object x=new N();Object y=(@A(\"first\") Number & @A(\"second\") Runnable)x;"
            + "O<String> o=new O<String>();Object i=o.new @A(\"qualified\") I<Integer>(1);return y==x && i!=null;}}");
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("TA1").getMethod("main").invoke(null));
        List<String> annotations = parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), "TA1"), !this.isJdk);
        String all = annotations.toString();
        Assert.assertTrue(all, all.contains("|value=first|arg=0")); Assert.assertTrue(all, all.contains("|value=second|arg=1"));
        Assert.assertTrue(all, all.contains("|INNER|value=qualified")); Assert.assertTrue(all, all.contains("|value=anonymous"));
        Class<?> anonymous = (Class<?>) compiler.getClassLoader().loadClass("TA1").getMethod("make").invoke(null);
        List<String> parent = parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), anonymous.getName()));
        Assert.assertTrue(parent.toString(), parent.contains("TA1|0x10|65535||value=anonymous"));
        List<String> child = parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), "TA1$Child"), !this.isJdk);
        Assert.assertTrue(child.toString(), child.toString().contains("|value=super|arg=0"));
    }

    @Test public void testReferenceExpressionAnnotations() throws Exception {
        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler(); compiler.setTargetVersion(8);
        compiler.cook("import java.lang.annotation.*;"
            + "@Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)@interface A{String value();}"
            + "@Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.CLASS)@interface B{String value();}"
            + "@Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.SOURCE)@interface C{String value();}"
            + "public class TA1{interface F<T,R>{R get(T x);}static class Box<T>{T x;Box(T x){this.x=x;}T get(){return x;}}"
            + "<T>TA1(T x){}static<T>T id(T x){return x;}<T>T instance(T x){return x;}"
            + "public static boolean main(){F<String,TA1>c=@A(\"constructor\") TA1::<@A(\"ctorArg\") String>new;"
            + "F<String,String>m=@A(\"method\") TA1::<@A(\"methodArg\") String>id;"
            + "F<String,String>b=new TA1(\"x\")::<@A(\"boundArg\") String>instance;"
            + "F<Integer,String[]>a=@A(\"element\") String @A(\"array\") []::new;"
            + "F<Box<String>,String>g=Box<@A(\"qualifierArg\") String>::get;"
            + "F<String[],String[]>copy=@A(\"clone\") String[]::clone;"
            + "F<String,String>invisible=TA1::<@B(\"invisibleArg\") String>id;"
            + "F<String,String>source=@C(\"source\") TA1::id;"
            + "String[]x={\"ok\"};return c.get(\"x\")!=null&&m.get(\"ok\").equals(\"ok\")&&b.get(\"ok\").equals(\"ok\")"
            + "&&a.get(2).length==2&&g.get(new Box<>(\"ok\")).equals(\"ok\")&&copy.get(x)!=x"
            + "&&invisible.get(\"ok\").equals(\"ok\")&&source.get(\"ok\").equals(\"ok\");}}");
        Assert.assertEquals(Boolean.TRUE, compiler.getClassLoader().loadClass("TA1").getMethod("main").invoke(null));
        List<String> annotations = parseTypeAnnotations(classFileBytes(compiler.getClassLoader(), "TA1"), !this.isJdk);
        String all = annotations.toString();
        for (String[] expected : new String[][] {
            {"0x45", "", "constructor"}, {"0x4a", "", "ctorArg"}, {"0x46", "", "method"},
            {"0x4b", "", "methodArg"}, {"0x4b", "", "boundArg"}, {"0x45", "ARR", "element"},
            {"0x45", "", "array"}, {"0x46", "TA0", "qualifierArg"}, {"0x46", "ARR", "clone"},
            {"0x4b", "", "invisibleArg"}
        }) {
            int matches = 0;
            // javac 8 flattens array reference qualifier paths; Janino follows the array path in JVMS 4.7.20.2.
            String path = this.isJdk && "1.8".equals(System.getProperty("java.specification.version"))
                && (expected[2].equals("element") || expected[2].equals("clone")) ? "" : expected[1];
            for (String annotation : annotations) if (annotation.contains("|"+expected[0]+"|")
                && annotation.contains("|"+path+"|value="+expected[2])) matches++;
            Assert.assertEquals(all, 1, matches);
        }
        Assert.assertFalse(all, all.contains("value=source"));
        // Generated delegate methods must not turn source reference type arguments into invocation annotations.
        Field field = ByteArrayClassLoader.class.getDeclaredField("classes"); field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String,byte[]> classes = (Map<String,byte[]>) field.get(compiler.getClassLoader());
        List<String> every = new ArrayList<>();
        for (byte[] bytes : classes.values()) every.addAll(parseTypeAnnotations(bytes, !this.isJdk));
        for (String annotation : every) if (annotation.contains("value=ctorArg") || annotation.contains("value=methodArg")
            || annotation.contains("value=boundArg") || annotation.contains("value=invisibleArg")) {
            Assert.assertFalse(annotation, annotation.contains("|0x48|") || annotation.contains("|0x49|"));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static byte[]
    classFileBytes(ClassLoader cl, String className) throws Exception {
        Field classesField = ByteArrayClassLoader.class.getDeclaredField("classes");
        classesField.setAccessible(true);
        Map<String, byte[]> classes = (Map<String, byte[]>) classesField.get(cl);

        byte[] bytes = classes.get(className);
        if (bytes == null) {
            // The key may be the "class file name" rather than the "class name".
            bytes = classes.get(className.replace('.', '/') + ".class");
        }
        Assert.assertNotNull("Class \"" + className + "\" not found in generated classes", bytes);
        return bytes;
    }

    /**
     * Parses the "Runtime[In]visibleTypeAnnotations" attributes of the given class file and returns a sorted list of
     * canonical descriptions of the type annotation entries.
     */
    private static List<String>
    parseTypeAnnotations(byte[] b) throws IOException {
        return parseTypeAnnotations(b, false);
    }

    private static List<String> parseTypeAnnotations(byte[] b, boolean strictOffsets) throws IOException {

        List<String> result = new ArrayList<>();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
        int magic = dis.readInt();
        Assert.assertEquals(0xcafebabe, magic);
        dis.readShort(); // minor_version
        dis.readShort(); // major_version

        // Constant pool.
        int           cpCount = dis.readUnsignedShort();
        List<Integer> cpTags  = new ArrayList<>(Collections.nCopies(cpCount, 0));
        List<String>  cpUtf8s = new ArrayList<>(Collections.nCopies(cpCount, (String) null));
        {
            for (int i = 1; i < cpCount; i++) {
                int tag = dis.readUnsignedByte();
                cpTags.set(i, tag);
                switch (tag) {
                case 1: // CONSTANT_Utf8
                    cpUtf8s.set(i, dis.readUTF());
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: // 4-byte entries
                    dis.skipBytes(4);
                    break;
                case 5: case 6: // 8-byte entries which occupy two constant pool slots
                    dis.skipBytes(8);
                    i++;
                    break;
                case 7: case 8: case 16: case 19: case 20: // 2-byte entries
                    dis.skipBytes(2);
                    break;
                case 15: // MethodHandle
                    dis.skipBytes(3);
                    break;
                default:
                    throw new AssertionError("Unexpected constant pool tag " + tag);
                }
            }
        }

        dis.readUnsignedShort(); // access_flags
        dis.readUnsignedShort(); // this_class
        dis.readUnsignedShort(); // super_class
        for (int i = dis.readUnsignedShort(); i > 0; i--) dis.readUnsignedShort(); // interfaces

        // Fields, then methods, then class attributes.
        for (int section = 0; section < 3; section++) {
            int memberCount = section < 2 ? dis.readUnsignedShort() : 1;
            for (int m = 0; m < memberCount; m++) {
                String memberName = null;
                if (section < 2) {
                    dis.readUnsignedShort();                                         // access_flags
                    memberName = cpUtf8s.get(dis.readUnsignedShort());               // name_index
                    dis.readUnsignedShort();                                         // descriptor_index
                }
                String who = section == 0 ? "TA1." + memberName : section == 1 ? "TA1." + memberName : "TA1";

                int attrCount = dis.readUnsignedShort();  // attributes_count
                for (int a = 0; a < attrCount; a++) {
                    String attrName = cpUtf8s.get(dis.readUnsignedShort());
                    int    bodyLen  = dis.readInt();
                    if (attrName.equals("RuntimeVisibleTypeAnnotations") || attrName.equals("RuntimeInvisibleTypeAnnotations")) {
                        TypeAnnotationTest.parseTypeAnnotationsBody(who, dis, cpUtf8s, result);
                    } else if (attrName.equals("Code")) {
                        dis.readUnsignedShort(); // max_stack
                        int maxLocals = dis.readUnsignedShort();
                        int length = dis.readInt();
                        byte[] code = new byte[length];
                        dis.readFully(code);
                        int handlers = dis.readUnsignedShort();
                        dis.skipBytes(handlers * 8);
                        for (int n = dis.readUnsignedShort(); n > 0; n--) {
                            String nested = cpUtf8s.get(dis.readUnsignedShort());
                            int size = dis.readInt();
                            if (nested.equals("RuntimeVisibleTypeAnnotations") || nested.equals("RuntimeInvisibleTypeAnnotations")) {
                                parseTypeAnnotationsBody(who, dis, cpUtf8s, result, strictOffsets ? code : null, maxLocals, handlers);
                            } else dis.skipBytes(size);
                        }
                    } else
                    {
                        dis.skipBytes(bodyLen);
                    }
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    private static void
    parseTypeAnnotationsBody(String who, DataInputStream dis, List<String> cpUtf8s, List<String> result)
    throws IOException {
        parseTypeAnnotationsBody(who, dis, cpUtf8s, result, null, 0, 0);
    }

    private static void parseTypeAnnotationsBody(String who, DataInputStream dis, List<String> cpUtf8s, List<String> result,
        byte[] code, int maxLocals, int handlers) throws IOException {
        for (int i = dis.readUnsignedShort(); i > 0; i--) {  // num_annotations

            int targetType = dis.readUnsignedByte();
            int targetIndex;
            int typeArgumentIndex = -1;
            switch (targetType) {
            case 0x13: case 0x14: case 0x15:  // FIELD, METHOD_RETURN, METHOD_RECEIVER: no target_info
                targetIndex = -1;
                break;
            case 0x16: case 0x00: case 0x01:  // METHOD_FORMAL_PARAMETER, type parameters: u1
                targetIndex = dis.readUnsignedByte();
                break;
            case 0x40: case 0x41:
                targetIndex = dis.readUnsignedShort();
                Assert.assertTrue(targetIndex > 0);
                for (int r = 0; r < targetIndex; r++) {
                    int start = dis.readUnsignedShort(), length = dis.readUnsignedShort(), slot = dis.readUnsignedShort();
                    Assert.assertTrue("Local range must not be empty", length > 0);
                    if (code != null) {
                        Assert.assertTrue("Local range exceeds Code", start + length <= code.length);
                        Assert.assertTrue("Local slot exceeds max_locals", slot < maxLocals);
                    }
                }
                break;
            case 0x47: case 0x48: case 0x49: case 0x4a: case 0x4b:
                targetIndex = dis.readUnsignedShort();
                typeArgumentIndex = dis.readUnsignedByte();
                break;
            default:                          // 0x10, 0x11, 0x17 and all "Code"-attribute target types: u2
                targetIndex = dis.readUnsignedShort();
                break;
            }

            if (code != null) {
                if (targetType == 0x42) Assert.assertTrue("Catch annotation references missing handler", targetIndex < handlers);
                if (targetType >= 0x43) {
                    Assert.assertTrue("Annotation offset exceeds Code", targetIndex < code.length);
                    int opcode = code[targetIndex] & 255;
                    if (targetType == 0x43) Assert.assertEquals("instanceof offset", 0xc1, opcode);
                    if (targetType == 0x44) Assert.assertTrue("new offset: " + opcode, opcode == 0xbb || opcode == 0xbc || opcode == 0xbd || opcode == 0xc5);
                    if (targetType == 0x45 || targetType == 0x46 || targetType == 0x4a || targetType == 0x4b) {
                        Assert.assertTrue("Reference expression offset: " + opcode, opcode == 0xbb || opcode == 0xb8 || opcode == 0xba);
                    }
                    if (targetType == 0x47) Assert.assertEquals("cast offset", 0xc0, opcode);
                    if (targetType == 0x48) Assert.assertEquals("constructor invocation offset", 0xb7, opcode);
                    if (targetType == 0x49) Assert.assertTrue("method invocation offset: " + opcode, opcode >= 0xb6 && opcode <= 0xb9);
                }
            }
            StringBuilder path = new StringBuilder();
            for (int p = dis.readUnsignedByte(); p > 0; p--) {  // path_length, path
                int kind = dis.readUnsignedByte();
                int idx = dis.readUnsignedByte();
                if (path.length() > 0) path.append(',');
                switch (kind) {
                case 0:  path.append("ARR");            break;
                case 1:  path.append("INNER");          break;
                case 2:  path.append("WILD");           break;  // The index of kind 2 is always zero (verified against JAVAC).
                case 3:  path.append("TA").append(idx); break;
                default: throw new AssertionError("Unexpected path kind " + kind);
                }
            }

            String annotationFd = cpUtf8s.get(dis.readUnsignedShort());  // type_index

            StringBuilder evps = new StringBuilder();
            for (int e = dis.readUnsignedShort(); e > 0; e--) {  // num_element_value_pairs
                String elementName = cpUtf8s.get(dis.readUnsignedShort());
                int    tag         = dis.readUnsignedByte();
                String value;
                switch (tag) {
                case 's': value = cpUtf8s.get(dis.readUnsignedShort()); break;
                default:  throw new AssertionError("Element value tag '" + (char) tag + "' not implemented");
                }
                if (evps.length() > 0) evps.append(',');
                evps.append(elementName).append('=').append(value);
            }

            result.add(
                who
                + "|"
                + (targetType == 0x13 ? "FIELD"
                :  targetType == 0x14 ? "METHOD_RETURN"
                :  targetType == 0x16 ? "METHOD_FORMAL_PARAMETER"
                :  targetType == 0x17 ? "THROWS"
                :                      "0x" + Integer.toHexString(targetType))
                + "|"
                + targetIndex
                + "|"
                + path
                + "|"
                + evps
                + (typeArgumentIndex < 0 ? "" : "|arg=" + typeArgumentIndex)
            );
        }
    }
}
