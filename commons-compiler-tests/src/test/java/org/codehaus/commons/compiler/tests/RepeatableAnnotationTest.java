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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Tests for the Java 8 "repeating annotations" feature (JLS8 9.7.5).
 */
@RunWith(Parameterized.class) public
class RepeatableAnnotationTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    RepeatableAnnotationTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    private static final String
    DECLS = (
        ""
        + "import java.lang.annotation.*;\n"
        + "@Retention(RetentionPolicy.RUNTIME) @Repeatable(Tasks.class) @interface Task { String value(); }\n"
        + "@Retention(RetentionPolicy.RUNTIME) @interface Tasks { Task[] value(); }\n"
    );

    /**
     * Repeated annotations on a class are compiled as one container annotation, which the Java 8 reflection
     * API expands.
     */
    @Test public void
    testRepeatedAnnotationsOnClass() throws Exception {
        ClassLoader cl = this.cook(DECLS + "@Task(\"one\") @Task(\"two\") @Task(\"three\") public class RA1 {}");
        Class<?>    c  = cl.loadClass("RA1");

        Class<? extends java.lang.annotation.Annotation> task  = ann(cl, "Task");
        Class<? extends java.lang.annotation.Annotation> tasks = ann(cl, "Tasks");

        // The class file contains only the container annotation.
        Assert.assertEquals(1, c.getDeclaredAnnotations().length);
        Assert.assertNotNull(c.getAnnotation(tasks));

        // "getAnnotationsByType()" expands the container.
        java.lang.annotation.Annotation[] byType = c.getAnnotationsByType(task);
        Assert.assertEquals(3, byType.length);
    }

    /** Repeated annotations on methods and fields. */
    @Test public void
    testRepeatedAnnotationsOnMethodAndField() throws Exception {
        ClassLoader cl = this.cook((
            DECLS
            + "public class RA2 {\n"
            + "    @Task(\"m1\") @Task(\"m2\") public void method() {}\n"
            + "    @Task(\"f1\") @Task(\"f2\") public int field;\n"
            + "}\n"
        ));
        Class<?> c = cl.loadClass("RA2");
        Class<? extends java.lang.annotation.Annotation> task = ann(cl, "Task");
        Assert.assertEquals(2, c.getMethod("method").getAnnotationsByType(task).length);
        Assert.assertEquals(2, c.getField("field").getAnnotationsByType(task).length);
    }

    /** A SINGLE annotation of a repeatable type is not wrapped in a container. */
    @Test public void
    testSingleAnnotationOfRepeatableTypeIsNotWrapped() throws Exception {
        ClassLoader cl = this.cook(DECLS + "@Task(\"only\") public class RA3 {}");
        Class<?>    c  = cl.loadClass("RA3");
        Assert.assertEquals(1, c.getDeclaredAnnotations().length);
        Assert.assertNotNull(c.getAnnotation(ann(cl, "Task")));
        Assert.assertNull(c.getAnnotation(ann(cl, "Tasks")));
    }

    /** Duplicate annotations of a NON-repeatable annotation type are still an error. */
    @Test public void
    testDuplicateNonRepeatableAnnotationRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "import java.lang.annotation.*;\n"
            + "@interface Single {}\n"
            + "@Single @Single public class RA4 {}\n"
        ));
    }

    /** The containing annotation type must declare an "annotationType[] value()" element. */
    @Test public void
    testInvalidContainingAnnotationTypeRejected() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "import java.lang.annotation.*;\n"
            + "@Repeatable(InvalidContainer.class) @interface Repeated2 { int value(); }\n"
            + "@interface InvalidContainer { int value(); }\n"
            + "@Repeated2(1) @Repeated2(2) public class RA5 {}\n"
        ));
    }

    /** A RUNTIME-retention annotation needs a RUNTIME-retention containing annotation type. */
    @Test public void
    testRuntimeRetentionContainerRequired() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "import java.lang.annotation.*;\n"
            + "@Retention(RetentionPolicy.RUNTIME) @Repeatable(ClassRetentionContainer.class)\n"
            + "@interface Repeated3 { int value(); }\n"
            + "@interface ClassRetentionContainer { Repeated3[] value(); }\n"
            + "@Repeated3(1) @Repeated3(2) public class RA6 {}\n"
        ));
    }

    // ----------------------------------------------------------------------------------------------------------------

    private ClassLoader
    cook(String compilationUnit) throws Exception {
        org.codehaus.commons.compiler.ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(compilationUnit);
        return sc.getClassLoader();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends java.lang.annotation.Annotation>
    ann(ClassLoader cl, String className) throws ClassNotFoundException {
        return (Class<? extends java.lang.annotation.Annotation>) cl.loadClass(className);
    }
}
