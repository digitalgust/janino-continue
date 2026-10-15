
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2019 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved. // CHECKSTYLE:OFF CHECKSTYLE:ON
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.IClass.IAnnotation;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.Wildcard;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.util.Annotatable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.MethodInfo;

/** Checks and encodes annotations, owning the groups flushed after each declaration. */
final class UnitCompilerAnnotationCompiler {

    /** The semantic operations required by this component. */
    interface Context {
        IClass getRawType(Type type) throws CompileException;
        @Nullable Object getConstantValue(Rvalue value) throws CompileException;
        Atom reclassify(AmbiguousName name) throws CompileException;
        @Nullable IClass findTypeByName(Location location, String name) throws CompileException;
        void compileError(String message, @Nullable Location location) throws CompileException;
    }

    private final Context context;
    private final IClassLoader iClassLoader;

    UnitCompilerAnnotationCompiler(Context context, IClassLoader iClassLoader) {
        this.context = context;
        this.iClassLoader = iClassLoader;
    }


    /**
     * Converts and adds the <var>annotations</var> to the <var>target</var>.
     * <p>
     *   Multiple annotations of a repeatable annotation type (JLS8 9.7.5) are converted into one "container"
     *   annotation.
     * </p>
     */
    void compileDeclarationAnnotations(Annotation[] annotations, String kind, Annotatable target, ClassFile cf) throws CompileException {
        List<Annotation> selected = new ArrayList<>();
        for (Annotation annotation : annotations) {
            boolean declaration = this.isTargeted(annotation, kind, true)
                || kind.equals("ANNOTATION_TYPE") && this.isTargeted(annotation, "TYPE", true)
                || (kind.equals("TYPE") || kind.equals("ANNOTATION_TYPE")) && this.isTypeUseTargeted(annotation);
            if (declaration) selected.add(annotation);
            else if (!this.isTypeUseTargeted(annotation)) this.context.compileError("Annotation is not applicable to " + kind, annotation.getLocation());
        }
        this.compileAnnotations(selected.toArray(new Annotation[selected.size()]), target, cf);
    }

    void
    compileAnnotations(Annotation[] annotations, Annotatable target, final ClassFile cf) throws CompileException {

        // Group the annotations by annotation type, so that repeated annotations can be compiled as "contained"
        // annotations (JLS8 9.7.5).
        Map<IClass, List<Annotation>> grouped = new LinkedHashMap<>();
        for (Annotation a : annotations) {
            IClass       at = this.context.getRawType(a.getType());
            List<Annotation> l  = grouped.get(at);
            if (l == null) grouped.put(at, l = new ArrayList<>());
            l.add(a);
        }

        for (Map.Entry<IClass, List<Annotation>> e : grouped.entrySet()) {

            final IClass        annotationIClass      = e.getKey();
            final List<Annotation> annotationsOfOneType = e.getValue();
            final IAnnotation[] annotationAnnotations = annotationIClass.getIAnnotations();

            if (annotationsOfOneType.size() > 1) {

                // Find the "containing annotation type" through the "@Repeatable" meta-annotation.
                IClass containerIClass = this.getRepeatableContainerIClass(annotationIClass);
                if (containerIClass == null) {
                    this.context.compileError((
                        "Duplicate annotation \""
                        + annotationIClass
                        + "\" (its type is not \"@Repeatable\")"
                    ), annotationsOfOneType.get(1).getType().getLocation());
                    continue;
                }

                // Verify that the containing annotation type has a "value()" element with type "annotationType[]".
                {
                    IMethod[] vms = containerIClass.getDeclaredIMethods("value");
                    IClass    rt  = vms.length == 1 ? vms[0].getReturnType() : null;
                    if (rt == null || !rt.isArray() || rt.getComponentType() != annotationIClass) {
                        this.context.compileError((
                            "Containing annotation type \""
                            + containerIClass
                            + "\" must declare an element \""
                            + annotationIClass
                            + "[] value()\""
                        ), annotationsOfOneType.get(1).getType().getLocation());
                        continue;
                    }
                }

                if (this.isSourceRetained(annotationAnnotations)) continue;  // Not stored in the class file.

                // If the repeatable annotation is RUNTIME-retention, then the containing annotation must also be
                // RUNTIME-retention.
                boolean annotationRuntimeVisible = this.isRuntimeRetained(annotationAnnotations);
                if (annotationRuntimeVisible && !this.isRuntimeRetained(containerIClass.getIAnnotations())) {
                    this.context.compileError((
                        "Containing annotation type \""
                        + containerIClass
                        + "\" must have RUNTIME retention"
                    ), annotationsOfOneType.get(1).getType().getLocation());
                    continue;
                }

                // An explicitly written container annotation plus repeated annotations of the element type is
                // an error (JLS8 9.7.5).
                for (Annotation other : annotations) {
                    if (this.context.getRawType(other.getType()) == containerIClass) {
                        this.context.compileError((
                            "Container annotation \"" + containerIClass + "\" must not be written explicitly"
                            + " if \"" + annotationIClass + "\" is repeated"
                        ), other.getType().getLocation());
                        break;
                    }
                }

                // Compile the repeated annotations as one container annotation.
                ClassFile.ElementValue[] values = new ClassFile.ElementValue[annotationsOfOneType.size()];
                for (int i = 0; i < values.length; i++) {
                    Annotation a = annotationsOfOneType.get(i);
                    values[i] = new ClassFile.Annotation(
                        cf.addConstantUtf8Info(annotationIClass.getDescriptor()),
                        this.compileAnnotationElementValues(a, annotationIClass, cf)
                    );
                }
                Map<Short, ClassFile.ElementValue> containerEvps = new HashMap<>();
                containerEvps.put(cf.addConstantUtf8Info("value"), new ClassFile.ArrayElementValue(values));

                target.addAnnotationsAttributeEntry(
                    this.isRuntimeRetained(containerIClass.getIAnnotations()), // runtimeVisible
                    containerIClass.getDescriptor(),                           // fieldDescriptor
                    containerEvps                                           // elementValuePairs
                );
                continue;
            }

            final Annotation a = annotationsOfOneType.get(0);
            final Type       annotationType = a.getType();

            // Determine the retention.
            boolean runtimeVisible = false;
            boolean sourceRetention = false;
            {
                String retention = this.retention(annotationAnnotations);
                if ("SOURCE".equals(retention)) {
                    sourceRetention = true;  // SOURCE-retention annotations are not stored in the class file.
                } else
                if ("RUNTIME".equals(retention)) {
                    runtimeVisible = true;
                } else
                if (retention != null && !"CLASS".equals(retention)) {
                    throw new AssertionError(retention);
                }
            }

            if (!sourceRetention) {
                target.addAnnotationsAttributeEntry(
                    runtimeVisible,
                    annotationIClass.getDescriptor(),
                    this.compileAnnotationElementValues(a, annotationIClass, cf)
                );
            }
        }
    }

    void compileParameterAnnotations(Java.FunctionDeclarator fd, final MethodInfo mi, final ClassFile cf)
    throws CompileException {
        final int count = new MethodDescriptor(mi.getDescriptor()).parameterFds.length;
        final int offset = count - fd.formalParameters.parameters.length;
        final Map<Boolean, ClassFile.ParameterAnnotationsAttribute> attributes = new HashMap<>();
        for (int i = 0; i < fd.formalParameters.parameters.length; i++) {
            final int index = offset + i;
            List<Annotation> declarations = new ArrayList<>();
            for (Java.Modifier modifier : fd.formalParameters.parameters[i].modifiers) {
                if (modifier instanceof Annotation && this.isTargeted((Annotation) modifier, "PARAMETER", true)) {
                    declarations.add((Annotation) modifier);
                }
            }
            this.compileAnnotations(declarations.toArray(new Annotation[declarations.size()]), new Annotatable() {
                @Override public ClassFile.Annotation[] getAnnotations(boolean visible) { return new ClassFile.Annotation[0]; }
                @Override public void addAnnotationsAttributeEntry(boolean visible, String descriptor, Map<Short, ClassFile.ElementValue> pairs) {
                    ClassFile.ParameterAnnotationsAttribute attribute = attributes.get(visible);
                    if (attribute == null) {
                        attribute = new ClassFile.ParameterAnnotationsAttribute(cf.addConstantUtf8Info(
                            visible ? "RuntimeVisibleParameterAnnotations" : "RuntimeInvisibleParameterAnnotations"), count);
                        attributes.put(visible, attribute);
                        mi.addAttribute(attribute);
                    }
                    attribute.getAnnotations(index).add(new ClassFile.Annotation(cf.addConstantUtf8Info(descriptor), pairs));
                }
            }, cf);
        }
    }

    /**
     * @return Whether the given meta-annotations (of an annotation type) include RUNTIME retention
     */
    @Nullable private String
    retention(IAnnotation[] metaAnnotations) throws CompileException {
        for (IAnnotation aa : metaAnnotations) {
            if (UnitCompiler.rawTypeOf(aa.getAnnotationType()) != this.iClassLoader.TYPE_java_lang_annotation_Retention) continue;
            Object v = aa.getElementValue("value");
            if (v instanceof IField) return ((IField) v).getName();
            if (v instanceof Enum)   return ((Enum<?>) v).name();
        }
        return null;
    }

    private boolean
    isSourceRetained(IAnnotation[] metaAnnotations) throws CompileException {
        return "SOURCE".equals(this.retention(metaAnnotations));
    }

    private boolean
    isRuntimeRetained(IAnnotation[] metaAnnotations) throws CompileException {
        return "RUNTIME".equals(this.retention(metaAnnotations));
    }

    /**
     * @return The containing annotation type of the given repeatable annotation type, or {@code null} if the
     *         annotation type is <em>not</em> meta-annotated with "{@code @Repeatable}", or if the annotation type
     *         is invalid
     */
    @Nullable private IClass
    getRepeatableContainerIClass(IClass annotationIClass) throws CompileException {

        // Notice: "java.lang.annotation.Repeatable" does not exist on Java-7-and-earlier JVMs, hence it cannot be
        // eagerly loaded in "IClassLoader.postConstruct()".
        IClass repeatableIClass;
        try {
            repeatableIClass = this.iClassLoader.loadIClass("Ljava/lang/annotation/Repeatable;");
        } catch (ClassNotFoundException cnfe) {
            return null;
        }

        for (IAnnotation ma : annotationIClass.getIAnnotations()) {
            if (UnitCompiler.rawTypeOf(ma.getAnnotationType()) != repeatableIClass) continue;

            Object v = ma.getElementValue("value");

            // Notice: "ReflectionIClass" reports a "java.lang.Class" object for class-literal element values,
            // while "ClassFileIClass" reports an "IClass".
            if (v instanceof IClass) return (IClass) v;
            if (v instanceof Class) {
                try {
                    return this.iClassLoader.loadIClass(Descriptor.fromClassName(((Class<?>) v).getName()));
                } catch (ClassNotFoundException cnfe) {
                    return null;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * Compiles the element-value-pairs of one annotation.
     */
    private Map<Short, ClassFile.ElementValue>
    compileAnnotationElementValues(final Annotation a, final IClass annotationIClass, final ClassFile cf)
    throws CompileException {

        final Map<Short, ClassFile.ElementValue> evps = new HashMap<>();
        a.accept(new AnnotationVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                IMethod[] definitions = annotationIClass.getDeclaredIMethods("value");
                assert definitions.length == 1;
                boolean isArray = definitions[0].getReturnType().isArray();
                evps.put(
                    cf.addConstantUtf8Info("value"),
                    UnitCompilerAnnotationCompiler.this.compileElementValue(sea.elementValue, cf, isArray)
                );
                return null;
            }

            @Override @Nullable public Void
            visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                for (ElementValuePair evp : na.elementValuePairs) {
                    IMethod[] definitions = annotationIClass.getDeclaredIMethods(evp.identifier);
                    assert definitions.length == 1;
                    boolean isArray = definitions[0].getReturnType().isArray();
                    evps.put(
                        cf.addConstantUtf8Info(evp.identifier),
                        UnitCompilerAnnotationCompiler.this.compileElementValue(evp.elementValue, cf, isArray)
                    );
                }
                return null;
            }

            @Override @Nullable public Void
            visitMarkerAnnotation(MarkerAnnotation ma) {
                ;
                return null;
            }
        });
        return evps;
    }

    // ------------------ JSR 308 TYPE annotations (declaration positions) ------------------

    /** Target type "FIELD", see JVMS8 4.7.20-A. */
    static final byte TARGET_TYPE_FIELD                   = 0x13;

    /** Target type "METHOD_RETURN", see JVMS8 4.7.20-A. */
    static final byte TARGET_TYPE_METHOD_RETURN           = 0x14;

    /** Target type "METHOD_FORMAL_PARAMETER", see JVMS8 4.7.20-A. */
    static final byte TARGET_TYPE_METHOD_FORMAL_PARAMETER = 0x16;

    /** Target type "THROWS", see JVMS8 4.7.20-A. */
    static final byte TARGET_TYPE_THROWS                  = 0x17;

    /** Accepts the entries of a "Runtime[In]visibleTypeAnnotations" attribute. */
    interface TypeAnnotationConsumer {

        /**
         * @param runtimeVisible Whether the type annotation is RUNTIME- or CLASS-retention
         */
        void
        accept(boolean runtimeVisible, ClassFile.TypeAnnotation typeAnnotation);
    }

    static UnitCompilerAnnotationCompiler.TypeAnnotationConsumer
    typeAnnotationsOf(final ClassFile cf) {
        return new TypeAnnotationConsumer() {
            @Override public void
            accept(boolean runtimeVisible, ClassFile.TypeAnnotation ta) {
                cf.addTypeAnnotationsAttributeEntry(runtimeVisible, ta);
            }
        };
    }

    static UnitCompilerAnnotationCompiler.TypeAnnotationConsumer
    typeAnnotationsOf(final ClassFile.FieldInfo fi) {
        return new TypeAnnotationConsumer() {
            @Override public void
            accept(boolean runtimeVisible, ClassFile.TypeAnnotation ta) {
                fi.addTypeAnnotationsAttributeEntry(runtimeVisible, ta);
            }
        };
    }

    static UnitCompilerAnnotationCompiler.TypeAnnotationConsumer
    typeAnnotationsOf(final ClassFile.MethodInfo mi) {
        return new TypeAnnotationConsumer() {
            @Override public void
            accept(boolean runtimeVisible, ClassFile.TypeAnnotation ta) {
                mi.addTypeAnnotationsAttributeEntry(runtimeVisible, ta);
            }
        };
    }

    /**
     * Compiles the TYPE annotations (JSR 308) which appear in <var>type</var> and in its type arguments as entries
     * of a "Runtime[In]visibleTypeAnnotations" attribute.
     *
     * @param targetType  One of 0x13 (FIELD), 0x14 (METHOD_RETURN), 0x16 (METHOD_FORMAL_PARAMETER), 0x17 (THROWS)
     * @param targetIndex The formal parameter index (0x16) or the "throws" type index (0x17), or -1 otherwise
     */
    void
    compileTypeAnnotations(
        Type                       type,
        byte                       targetType,
        int                        targetIndex,
        UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac,
        ClassFile                  cf
    ) throws CompileException {
        this.compileTypeAnnotations2(type, targetType, targetIndex, new byte[0], tac, cf);
    }

    void compileTypeParameters(@Nullable Java.TypeParameter[] parameters, Java.Scope scope, boolean method,
        TypeAnnotationConsumer consumer, ClassFile cf) throws CompileException {
        if (parameters == null) return;
        for (int i = 0; i < parameters.length; i++) {
            Java.TypeParameter parameter = parameters[i];
            for (Annotation annotation : parameter.annotations) {
                UnitCompiler.setScopeIfUnset(annotation.getType(), scope);
                this.addTypeAnnotationEntry(annotation, (byte) (method ? 0x01 : 0x00), i, new byte[0], consumer, cf);
            }
            if (parameter.bound == null) continue;
            int offset = parameter.bound.length > 0 && this.context.getRawType(parameter.bound[0]).isInterface() ? 1 : 0;
            for (int j = 0; j < parameter.bound.length; j++) {
                this.compileTypeAnnotations(parameter.bound[j], (byte) (method ? 0x12 : 0x11), (i << 8) | (j + offset), consumer, cf);
            }
        }
    }

    void compileClassTypeAnnotations(Java.AbstractTypeDeclaration declaration, ClassFile cf) throws CompileException {
        TypeAnnotationConsumer consumer = typeAnnotationsOf(cf);
        if (declaration instanceof Java.NamedTypeDeclaration) this.compileTypeParameters(
            ((Java.NamedTypeDeclaration) declaration).getOptionalTypeParameters(), declaration, false, consumer, cf);
        Type superclass = null;
        Type[] interfaces = new Type[0];
        if (declaration instanceof Java.NamedClassDeclaration) {
            superclass = ((Java.NamedClassDeclaration) declaration).extendedType;
            interfaces = ((Java.NamedClassDeclaration) declaration).implementedTypes;
        } else if (declaration instanceof Java.InterfaceDeclaration) {
            interfaces = ((Java.InterfaceDeclaration) declaration).extendedTypes;
        } else if (declaration instanceof Java.AnonymousClassDeclaration) {
            Type base = ((Java.AnonymousClassDeclaration) declaration).baseType;
            if (this.context.getRawType(base).isInterface()) interfaces = new Type[] { base }; else superclass = base;
        }
        if (superclass != null) this.compileTypeAnnotations(superclass, (byte) 0x10, 65535, consumer, cf);
        for (int i = 0; i < interfaces.length; i++) this.compileTypeAnnotations(interfaces[i], (byte) 0x10, i, consumer, cf);
        this.flushTypeAnnotations(consumer, cf);
    }

    void compileCodeTypeAnnotations(Type type, Annotation[] declarations, final byte target,
        final CodeContext code, final CodeContext.Offset offset, final Java.LocalVariableSlot local, final int argument)
    throws CompileException {
        TypeAnnotationConsumer consumer = new TypeAnnotationConsumer() {
            @Override public void accept(boolean visible, ClassFile.TypeAnnotation annotation) {
                code.addCodeTypeAnnotation(visible, annotation, offset, local, argument);
            }
        };
        this.compileTypeAnnotations(type, target, 0, consumer, code.getClassFile());
        this.compileDeclarationPositionTypeAnnotations(declarations, type, target, 0, consumer, code.getClassFile());
        this.flushTypeAnnotations(consumer, code.getClassFile());
    }

    /**
     * @param path The accumulated "target_path", as pairs of (kind, index)
     */
    private void
    compileTypeAnnotations2(
        Type                           type,
        byte                           targetType,
        int                            targetIndex,
        byte[]                         path,
        UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac,
        ClassFile                      cf
    ) throws CompileException {

        if (type instanceof ArrayType) {
            for (Annotation annotation : ((ArrayType) type).annotations) this.addTypeAnnotationEntry(annotation, targetType, targetIndex, path, tac, cf);
            byte[] p = UnitCompilerAnnotationCompiler.concatPath(path, (byte) 0, (byte) 0);  // One array dimension deeper.
            this.compileTypeAnnotations2(((ArrayType) type).componentType, targetType, targetIndex, p, tac, cf);
            return;
        }

        if (type instanceof Java.PrimitiveType) {
            for (Annotation annotation : ((Java.PrimitiveType) type).annotations) this.addTypeAnnotationEntry(annotation, targetType, targetIndex, path, tac, cf);
            return;
        }
        if (type instanceof Java.RvalueMemberType) {
            Java.RvalueMemberType member = (Java.RvalueMemberType) type;
            IClass owner = this.context.getRawType(member);
            while ((owner = owner.getOuterIClass()) != null) path = UnitCompilerAnnotationCompiler.concatPath(path, (byte) 1, (byte) 0);
            for (Annotation annotation : member.annotations) this.addTypeAnnotationEntry(annotation, targetType, targetIndex, path, tac, cf);
            this.compileTypeArguments(member.typeArguments, targetType, targetIndex, path, tac, cf);
            return;
        }
        if (!(type instanceof ReferenceType)) return;

        ReferenceType rt = (ReferenceType) type;

        int depth = 0;
        IClass owner = this.context.getRawType(rt);
        while ((owner = owner.getOuterIClass()) != null) depth++;
        int first = rt.identifiers.length - depth - 1;
        byte[] firstPath = path;
        for (int i = first; i < 0; i++) firstPath = UnitCompilerAnnotationCompiler.concatPath(firstPath, (byte) 1, (byte) 0);
        for (Annotation a : rt.annotations) this.addTypeAnnotationEntry(a, targetType, targetIndex, firstPath, tac, cf);
        byte[] segmentPath = firstPath;
        for (int i = 0; i < rt.identifiers.length; i++) {
            if (i < first && rt.segmentAnnotations[i].length > 0) this.context.compileError("Cannot annotate a package or a static type qualifier", rt.getLocation());
            if (i > Math.max(0, first)) segmentPath = UnitCompilerAnnotationCompiler.concatPath(segmentPath, (byte) 1, (byte) 0);
            for (Annotation annotation : rt.segmentAnnotations[i]) this.addTypeAnnotationEntry(annotation, targetType, targetIndex, segmentPath, tac, cf);
            this.compileTypeArguments(rt.segmentTypeArguments[i], targetType, targetIndex, segmentPath, tac, cf);
        }
    }

    private void compileTypeArguments(@Nullable TypeArgument[] arguments, byte targetType, int targetIndex, byte[] path,
        TypeAnnotationConsumer tac, ClassFile cf) throws CompileException {
        if (arguments == null) return;
        for (int i = 0; i < arguments.length; i++) {
            byte[]       p  = UnitCompilerAnnotationCompiler.concatPath(path, (byte) 3, (byte) i);  // Type argument #i.
            TypeArgument ta = arguments[i];

            if (ta instanceof Wildcard) {
                Wildcard w = (Wildcard) ta;

                // Annotations on the wildcard itself.
                for (Annotation a : w.annotations) {
                    this.addTypeAnnotationEntry(a, targetType, targetIndex, p, tac, cf);
                }

                // Annotations on the wildcard BOUND (and deeper); the WILDCARD path step (kind 2) precedes them;
                // its index is always zero (JLS11 4.7.20.2; verified against JAVAC output).
                if (w.referenceType != null) {
                    this.compileTypeAnnotations2(
                        w.referenceType,                     // type
                        targetType,                          // targetType
                        targetIndex,                         // targetIndex
                        UnitCompilerAnnotationCompiler.concatPath(p, (byte) 2, (byte) 0),
                        tac,                                 // tac
                        cf                                   // cf
                    );
                }
            } else
            if (ta instanceof Type) {
                this.compileTypeAnnotations2((Type) ta, targetType, targetIndex, p, tac, cf);
            }
        }
    }

    private static byte[]
    concatPath(byte[] path, byte kind, byte index) {
        byte[] result = new byte[path.length + 2];
        System.arraycopy(path, 0, result, 0, path.length);
        result[path.length]     = kind;
        result[path.length + 1] = index;
        return result;
    }

    /**
     * Compiles the DECLARATION-position annotations (JLS8 9.7.2/.3/.4, e.g. "@TA String f;") whose annotation type
     * is "@Target"ed with TYPE_USE as TYPE annotations (JSR 308); such annotations apply to the ELEMENT type of the
     * declared type, i.e. the path descends one step for each array dimension.
     * <p>
     *   Notice: An annotation type WITHOUT "@Target" is, per JLS8 9.6.4.1, applicable in all positions, but JAVAC
     *   does not convert such declaration-position annotations to type annotations, and neither do we.
     * </p>
     */
    void
    compileDeclarationPositionTypeAnnotations(
        Annotation[]                    annotations,
        Type                            declaredType,
        byte                            targetType,
        int                             targetIndex,
        UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac,
        ClassFile                       cf
    ) throws CompileException {

        int  dims = 0;
        for (Type t = declaredType; t instanceof ArrayType; t = ((ArrayType) t).componentType) dims++;

        byte[] path = new byte[2 * dims];  // One (ARRAY, 0) pair per dimension.
        if (targetType == TARGET_TYPE_METHOD_RETURN && declaredType instanceof Java.SimpleType) {
            IClass inner = this.context.getRawType(declaredType);
            while (inner.getDeclaringIClass() != null && (inner = inner.getOuterIClass()) != null) {
                path = UnitCompilerAnnotationCompiler.concatPath(path, (byte) 1, (byte) 0);
            }
        }
        for (Annotation a : annotations) {
            if (!this.isTypeUseTargeted(a)) continue;
            this.addTypeAnnotationEntry(a, targetType, targetIndex, path, tac, cf);
        }
    }

    /**
     * @return Whether the type of the given annotation is meta-annotated with an explicit "@Target" that includes
     *         TYPE_USE
     */
    private boolean
    isTypeUseTargeted(Annotation a) throws CompileException { return this.isTargeted(a, "TYPE_USE", false); }

    private boolean isTargeted(Annotation a, String target, boolean withoutTarget) throws CompileException {

        IClass targetIClass;
        try {
            targetIClass = this.iClassLoader.loadIClass("Ljava/lang/annotation/Target;");
        } catch (ClassNotFoundException cnfe) {
            return false;
        }

        IClass annotationIClass = this.context.getRawType(a.getType());
        for (IAnnotation ma : annotationIClass.getIAnnotations()) {
            if (ma.getAnnotationType() != targetIClass) continue;

            // Notice: An array element with a single value is not necessarily wrapped in an Object[].
            Object v = ma.getElementValue("value");

            // Notice: "ReflectionIClass" reports raw "Enum" objects for enum array element values, while
            // "ClassFileIClass" reports "IField"s.
            Object[] vs = v instanceof Object[] ? (Object[]) v : new Object[] { v };
            for (Object e : vs) {
                String n = (
                    e instanceof IField ? ((IField) e).getName() :
                    e instanceof Enum   ? ((Enum<?>) e).name()    :
                    null
                );
                if (target.equals(n)) return true;
            }
            return false;
        }
        return withoutTarget;
    }

    private void
    addTypeAnnotationEntry(
        Annotation                       a,
        byte                             targetType,
        int                              targetIndex,
        byte[]                           path,
        UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac,
        ClassFile                        cf
    ) throws CompileException {

        boolean parameter = targetType == 0x00 || targetType == 0x01;
        if (!(parameter && this.isTargeted(a, "TYPE_PARAMETER", false)) && !this.isTypeUseTargeted(a)) {
            this.context.compileError("Annotation is not applicable to " + (parameter ? "TYPE_PARAMETER" : "TYPE_USE"), a.getLocation());
        }
        IClass annotationIClass = this.context.getRawType(a.getType());

        // Determine the retention; SOURCE-retention annotations are not stored in the class file.
        IAnnotation[] mas = annotationIClass.getIAnnotations();
        if (this.isSourceRetained(mas)) return;

        // Buffer the entry; repeated annotations of the same type at the same position are combined into one
        // container annotation by "flushTypeAnnotations()" (JLS8 9.7.5).
        String key = targetType + ":" + targetIndex + ":" + java.util.Arrays.toString(path);
        List<Object[]> group = this.typeAnnotationGroups.get(key + "@" + System.identityHashCode(annotationIClass));
        if (group == null) {
            group = new ArrayList<>();
            this.typeAnnotationGroups.put(key + "@" + System.identityHashCode(annotationIClass), group);
        }
        group.add(new Object[] {
            a,
            Byte.valueOf(targetType),
            Integer.valueOf(targetIndex),
            path,
            Boolean.valueOf(this.isRuntimeRetained(mas))
        });
        this.typeAnnotationGroupOrder.add(key + "@" + System.identityHashCode(annotationIClass));
    }

    /** { group key => buffered entries }; consumed and cleared by {@link #flushTypeAnnotations}. */
    private final Map<String, List<Object[]>> typeAnnotationGroups = new java.util.LinkedHashMap<>();

    /** The order in which the groups were created. */
    private final java.util.LinkedHashSet<String> typeAnnotationGroupOrder = new java.util.LinkedHashSet<>();

    /**
     * Emits the buffered type annotation entries; must be called after each declaration (field, method) has
     * been processed.
     */
    void
    flushTypeAnnotations(UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac, ClassFile cf) throws CompileException {

        for (String k : this.typeAnnotationGroupOrder) {
            List<Object[]> group = this.typeAnnotationGroups.get(k);
            assert group != null && !group.isEmpty();

            Object[] first = group.get(0);
            Annotation a     = (Annotation) first[0];
            byte       tt    = ((Byte) first[1]).byteValue();
            int        ti    = ((Integer) first[2]).intValue();
            byte[]     path  = (byte[]) first[3];
            boolean    rv    = ((Boolean) first[4]).booleanValue();

            if (group.size() == 1) {
                IClass annotationIClass = this.context.getRawType(a.getType());
                tac.accept(rv, new ClassFile.TypeAnnotation(tt, ti, path, new ClassFile.Annotation(
                    cf.addConstantUtf8Info(annotationIClass.getDescriptor()),
                    this.compileAnnotationElementValues(a, annotationIClass, cf)
                )));
                continue;
            }

            // Two or more annotations of the same type at the same position: Compile them as one CONTAINER
            // annotation (JLS8 9.7.5), iff the annotation type is @Repeatable; otherwise a compile error.
            IClass annotationIClass = this.context.getRawType(a.getType());
            IClass containerIClass  = this.getRepeatableContainerIClass(annotationIClass);
            if (containerIClass == null) {
                this.context.compileError("Duplicate type annotation \"" + annotationIClass + "\"", a.getType().getLocation());
                continue;
            }

            for (List<Object[]> other : this.typeAnnotationGroups.values()) {
                Object[] entry = other.get(0);
                if (((Byte) entry[1]).byteValue() == tt && ((Integer) entry[2]).intValue() == ti
                    && Arrays.equals((byte[]) entry[3], path)
                    && this.context.getRawType(((Annotation) entry[0]).getType()) == containerIClass) {
                    this.context.compileError("Repeated type annotation conflicts with its explicit container", a.getType().getLocation());
                }
            }

            ClassFile.ElementValue[] values = new ClassFile.ElementValue[group.size()];
            for (int i = 0; i < values.length; i++) {
                Annotation ga  = (Annotation) group.get(i)[0];
                IClass     gic = this.context.getRawType(ga.getType());
                values[i] = new ClassFile.Annotation(
                    cf.addConstantUtf8Info(gic.getDescriptor()),
                    this.compileAnnotationElementValues(ga, gic, cf)
                );
            }
            Map<Short, ClassFile.ElementValue> containerEvps = new HashMap<>();
            containerEvps.put(cf.addConstantUtf8Info("value"), new ClassFile.ArrayElementValue(values));

            boolean containerRv = this.isRuntimeRetained(containerIClass.getIAnnotations());
            tac.accept(containerRv, new ClassFile.TypeAnnotation(
                tt,
                ti,
                path,
                new ClassFile.Annotation(cf.addConstantUtf8Info(containerIClass.getDescriptor()), containerEvps)
            ));
        }
        this.typeAnnotationGroups.clear();
        this.typeAnnotationGroupOrder.clear();
    }

    ClassFile.ElementValue
    compileElementValue(ElementValue elementValue, final ClassFile cf, boolean compileAsArray) throws CompileException {

        ClassFile.ElementValue
        result = (ClassFile.ElementValue) elementValue.accept(
            new ElementValueVisitor<ClassFile.ElementValue, CompileException>() {

                @Override public ClassFile.ElementValue
                visitRvalue(Rvalue rv) throws CompileException {

                    // Enum constant?
                    ENUM_CONSTANT:
                    if (rv instanceof AmbiguousName) {

                        Rvalue enumConstant = UnitCompilerAnnotationCompiler.this.context.reclassify((AmbiguousName) rv).toRvalue();
                        if (!(enumConstant instanceof FieldAccess)) break ENUM_CONSTANT; // Not a field access.
                        FieldAccess enumConstantFieldAccess = (FieldAccess) enumConstant;

                        Type enumType = enumConstantFieldAccess.lhs.toType();
                        if (enumType == null) break ENUM_CONSTANT; // LHS is not a type.

                        // Reclassification already resolved this type (including java.lang/imports).
                        // SimpleType.toString() is a display name, not a fully qualified lookup key.
                        IClass enumIClass = UnitCompilerAnnotationCompiler.this.context.getRawType(enumType);

                        if (enumIClass.getSuperclass() == UnitCompilerAnnotationCompiler.this.iClassLoader.TYPE_java_lang_Enum) {
                            return new ClassFile.EnumConstValue(
                                cf.addConstantUtf8Info(enumIClass.getDescriptor()),             // typeNameIndex
                                cf.addConstantUtf8Info(enumConstantFieldAccess.field.getName()) // constNameIndex
                            );
                        }

                        // We have a constant, but it is not an ENUM constant, so fall through.
                    }

                    // Class literal?
                    if (rv instanceof ClassLiteral) {
                        return new ClassFile.ClassElementValue(cf.addConstantUtf8Info(
                            UnitCompilerAnnotationCompiler.this.context.getRawType(((ClassLiteral) rv).type).getDescriptor()
                        ));
                    }

                    // Constant value?
                    Object cv = UnitCompilerAnnotationCompiler.this.context.getConstantValue(rv);

                    if (cv == UnitCompiler.NOT_CONSTANT) {
                        throw new CompileException(
                            "\"" + rv + "\" is not a constant expression",
                            rv.getLocation()
                        );
                    }

                    if (cv == null) {
                        throw new CompileException(
                            "Null literal not allowed as element value",
                            rv.getLocation()
                        );
                    }

                    if (cv instanceof Boolean)   { return new ClassFile.BooleanElementValue(cf.addConstantIntegerInfo((Boolean) cv ? 1 : 0)); }
                    if (cv instanceof Byte)      { return new ClassFile.ByteElementValue(cf.addConstantIntegerInfo((Byte) cv));               }
                    if (cv instanceof Short)     { return new ClassFile.ShortElementValue(cf.addConstantIntegerInfo((Short) cv));             }
                    if (cv instanceof Integer)   { return new ClassFile.IntElementValue(cf.addConstantIntegerInfo((Integer) cv));             }
                    if (cv instanceof Long)      { return new ClassFile.LongElementValue(cf.addConstantLongInfo((Long) cv));                  }
                    if (cv instanceof Float)     { return new ClassFile.FloatElementValue(cf.addConstantFloatInfo((Float) cv));               }
                    if (cv instanceof Double)    { return new ClassFile.DoubleElementValue(cf.addConstantDoubleInfo((Double) cv));            }
                    if (cv instanceof Character) { return new ClassFile.CharElementValue(cf.addConstantIntegerInfo((Character) cv));          }
                    if (cv instanceof String)    { return new ClassFile.StringElementValue(cf.addConstantUtf8Info((String) cv));              }

                    throw new AssertionError(cv);
                }

                @Override public ClassFile.ElementValue
                visitAnnotation(Annotation a) throws CompileException {

                    final IClass annotationIClass    = UnitCompilerAnnotationCompiler.this.context.getRawType(a.getType());
                    short        annotationTypeIndex = cf.addConstantUtf8Info(annotationIClass.getDescriptor());

                    final Map<Short, ClassFile.ElementValue> evps = new HashMap<>();
                    a.accept(new AnnotationVisitor<Void, CompileException>() {

                        @Override @Nullable public Void
                        visitMarkerAnnotation(MarkerAnnotation ma) {
                            ;
                            return null;
                        }

                        @Override @Nullable public Void
                        visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                            IMethod[] definitions = annotationIClass.getDeclaredIMethods("value");
                            assert definitions.length == 1;
                            boolean expectArray = definitions[0].getReturnType().isArray();
                            evps.put(
                                cf.addConstantUtf8Info("value"),
                                UnitCompilerAnnotationCompiler.this.compileElementValue(sea.elementValue, cf, expectArray)
                            );
                            return null;
                        }

                        @Override @Nullable public Void
                        visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                            for (ElementValuePair evp : na.elementValuePairs) {
                                IMethod[] definitions = annotationIClass.getDeclaredIMethods(evp.identifier);
                                assert definitions.length == 1;
                                boolean expectArray = Descriptor.isArrayReference(definitions[0].getDescriptor().returnFd);
                                evps.put(
                                    cf.addConstantUtf8Info(evp.identifier),
                                    UnitCompilerAnnotationCompiler.this.compileElementValue(evp.elementValue, cf, expectArray)
                                );
                            }
                            return null;
                        }
                    });
                    return new ClassFile.Annotation(annotationTypeIndex, evps);
                }

                @Override public ClassFile.ElementValue
                visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws CompileException {
                    ClassFile.ElementValue[]
                    evs = new ClassFile.ElementValue[evai.elementValues.length];

                    for (int i = 0; i < evai.elementValues.length; i++) {
                        evs[i] = UnitCompilerAnnotationCompiler.this.compileElementValue(evai.elementValues[i], cf, false);
                    }
                    return new ClassFile.ArrayElementValue(evs);
                }
            }
        );

        if (compileAsArray && result instanceof ClassFile.ConstantElementValue) {
            ClassFile.ElementValue[] arrayValues = new ClassFile.ElementValue[1];
            arrayValues[0] = result;
            result = new ClassFile.ArrayElementValue(arrayValues);
        }

        assert result != null;
        return result;
    }
}
