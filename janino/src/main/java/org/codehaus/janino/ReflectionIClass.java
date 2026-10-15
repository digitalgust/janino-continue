
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Wraps a {@link java.lang.Class} in an {@link org.codehaus.janino.IClass}.
 */
class ReflectionIClass extends IClass {

    private final Class<?>     clazz;
    private final IClassLoader iClassLoader;

    /**
     * @param iClassLoader Required to load other {@link IClass}es on {@code get...()}
     */
    ReflectionIClass(Class<?> clazz, IClassLoader iClassLoader) {
        this.clazz        = clazz;
        clazz.getTypeParameters();
        this.iClassLoader = iClassLoader;
    }

    @Override public ITypeVariable[]
    getITypeVariables2() {

        final TypeVariable<?>[] tps = this.clazz.getTypeParameters();

        ITypeVariable[] result = new ITypeVariable[tps.length];
        for (int i = 0; i < result.length; i++) result[i] = this.typeVariableToITypeVariable(tps[i]);

        return result;
    }

    @Override public GenericType[]
    getGenericSupertypes() {
        List<GenericType> result = new ArrayList<>();
        java.lang.reflect.Type superclass = this.clazz.getGenericSuperclass();
        if (superclass != null) result.add(superclass instanceof Class
            ? this.classToRawType((Class<?>) superclass) : this.typeToGenericType(superclass));
        for (java.lang.reflect.Type iface : this.clazz.getGenericInterfaces()) {
            result.add(iface instanceof Class ? this.classToRawType((Class<?>) iface) : this.typeToGenericType(iface));
        }
        return result.toArray(new GenericType[result.size()]);
    }

    @Override public Map<String, GenericType[]> getTypeParameterBounds() {
        Map<String, GenericType[]> result = new HashMap<>();
        for (TypeVariable<?> variable : this.clazz.getTypeParameters()) {
            Type[] bounds = variable.getBounds();
            GenericType[] types = new GenericType[bounds.length];
            for (int i = 0; i < types.length; i++) types[i] = bounds[i] instanceof Class
                ? this.classToRawType((Class<?>) bounds[i]) : this.typeToGenericType(bounds[i]);
            result.put(variable.getName(), types);
        }
        return result;
    }

    private ITypeVariable
    typeVariableToITypeVariable(final TypeVariable<?> tv) {

        return new ITypeVariable() {

            @Override public String
            getName() { return tv.getName(); }

            @Override public ITypeVariableOrIClass[]
            getBounds() throws CompileException {
                IType[] tmp = ReflectionIClass.this.typesToITypes(tv.getBounds());
                return (ITypeVariableOrIClass[]) Arrays.copyOf(tmp, tmp.length, ITypeVariableOrIClass[].class);
            }
        };
    }

    private IType[]
    typesToITypes(Type[] types) throws CompileException {
        IType[] result = new IType[types.length];
        for (int i = 0; i < result.length; i++) result[i] = this.typeToIType(types[i]);
        return result;
    }

    private IType
    typeToIType(Type type) throws CompileException {
        if (type instanceof Class) {
            IClass iClass;
            try {
                iClass = this.iClassLoader.loadIClass(Descriptor.fromClassName(((Class<?>) type).getName()));
            } catch (ClassNotFoundException cnfe) {
                throw new CompileException("Loading \"" + type + "\"", null, cnfe);
            }
            if (iClass == null) throw new CompileException("Could not load \"" + type + "\"", null);
            return iClass;
        } else
        if (type instanceof GenericArrayType) {
            return this.iClassLoader.getArrayIClass(UnitCompiler.rawTypeOf(this.typeToIType(((GenericArrayType) type).getGenericComponentType())));
        } else
        if (type instanceof ParameterizedType) {
            return this.typeToIType(((ParameterizedType) type).getRawType());
        } else
        if (type instanceof TypeVariable) {
            return this.typeToIType(((TypeVariable<?>) type).getBounds()[0]);
        } else
        if (type instanceof WildcardType) {
            return this.typeToIType(((WildcardType) type).getUpperBounds()[0]);
        } else
        {
            throw new AssertionError(type.getClass());
        }
    }

    @Override protected IConstructor[]
    getDeclaredIConstructors2() {
        Constructor<?>[] constructors = this.clazz.getDeclaredConstructors();
        IConstructor[]   result       = new IConstructor[constructors.length];
        for (int i = 0; i < constructors.length; ++i) {
            result[i] = new ReflectionIConstructor(constructors[i]);
        }
        return result;
    }

    @Override protected IMethod[]
    getDeclaredIMethods2() {
        Method[] methods  = this.clazz.getDeclaredMethods();

        if (methods.length == 0 && this.clazz.isArray()) {

            // Arrays have ONE single method: "Object clone()".
            return new IMethod[] { new IMethod() {
                @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                @Override public Access        getAccess()            { return Access.PUBLIC;      }
                @Override public boolean       isStatic()             { return false;              }
                @Override public boolean       isAbstract()           { return false;              }
                @Override public String        getName()              { return "clone";            }
                @Override public IClass[]      getParameterTypes2()   { return new IClass[0];      }
                @Override public boolean       isVarargs()            { return false;              }
                @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }

                @Override public IClass
                getReturnType() { return ReflectionIClass.this.iClassLoader.TYPE_java_lang_Object; }
            } };
        }

        return this.methodsToIMethods(methods);
    }

    @Override protected IField[]
    getDeclaredIFields2() { return this.fieldsToIFields(this.clazz.getDeclaredFields()); }

    @Override protected IClass[]
    getDeclaredIClasses2() { return this.classesToIClasses(this.clazz.getDeclaredClasses()); }

    @Override @Nullable protected IClass
    getDeclaringIClass2() {

        Class<?> declaringClass = this.clazz.getDeclaringClass();
        return declaringClass == null ? null : this.classToIClass(declaringClass);
    }

    @Override @Nullable protected IClass
    getOuterIClass2() throws CompileException {

        if (Modifier.isStatic(this.clazz.getModifiers())) return null;

        return this.getDeclaringIClass();
    }

    @Override @Nullable protected IClass
    getSuperclass2() {

        Class<?> superclass = this.clazz.getSuperclass();
        return superclass == null ? null : this.classToIClass(superclass);
    }

    @Override @Nullable protected IClass
    getComponentType2() {
        Class<?> componentType = this.clazz.getComponentType();
        return componentType == null ? null : this.classToIClass(componentType);
    }

    @Override protected IClass[] getInterfaces2() { return this.classesToIClasses(this.clazz.getInterfaces()); }
    @Override protected String   getDescriptor2() { return Descriptor.fromClassName(this.clazz.getName());     }

    @Override public Access  getAccess()   { return ReflectionIClass.modifiers2Access(this.clazz.getModifiers()); }
    @Override public boolean isFinal()     { return Modifier.isFinal(this.clazz.getModifiers());                  }
    @Override public boolean isEnum()      { return this.clazz.isEnum();                                          }
    @Override public boolean isInterface() { return this.clazz.isInterface();                                     }
    @Override public boolean isAbstract()  { return Modifier.isAbstract(this.clazz.getModifiers());               }
    @Override public boolean isArray()     { return this.clazz.isArray();                                         }

    @Override public boolean
    isPrimitive() { return this.clazz.isPrimitive(); }

    @Override public boolean
    isPrimitiveNumeric() {
        return (
            this.clazz == byte.class
            || this.clazz == short.class
            || this.clazz == int.class
            || this.clazz == long.class
            || this.clazz == char.class
            || this.clazz == float.class
            || this.clazz == double.class
        );
    }

    @Override public IAnnotation[]
    getIAnnotations2() throws CompileException {

        Annotation[] as = this.clazz.getAnnotations();
        if (as.length == 0) return IClass.NO_ANNOTATIONS;

        IAnnotation[] result = new IAnnotation[as.length];
        for (int i = 0; i < as.length; i++) {
            final Annotation a = as[i];

            // Get annotation type IClass.
            final Class<? extends Annotation> annotationType = a.annotationType();
            final IClass                      annotationTypeIClass;
            try {
                annotationTypeIClass = ReflectionIClass.this.iClassLoader.loadIClass(
                    Descriptor.fromClassName(annotationType.getName())
                );
            } catch (ClassNotFoundException cnfe) {
                throw new CompileException(
                    "Loading annotation type",
                    null, // location
                    cnfe
                );
            }
            if (annotationTypeIClass == null) {
                throw new CompileException("Could not load \"" + annotationType.getName() + "\"", null);
            }

            result[i] = new IAnnotation() {

                @Override public IClass
                getAnnotationType() { return annotationTypeIClass; }

                @Override public Object
                getElementValue(String name) throws CompileException {
                    try {
                        Object v = a.getClass().getMethod(name).invoke(a);

                        if (!Enum.class.isAssignableFrom(v.getClass())) return v;

                        Class<?> enumClass = v.getClass();

                        String enumConstantName = (String) enumClass.getMethod("name").invoke(v);

                        IClass enumIClass = ReflectionIClass.this.classToIClass(enumClass);

                        IField enumConstField = enumIClass.getDeclaredIField(enumConstantName);
                        if (enumConstField == null) {
                            throw new CompileException((
                                "Enum \""
                                + enumIClass
                                + "\" has no constant \""
                                + enumConstantName
                                + ""
                            ), null);
                        }
                        return enumConstField;
                    } catch (NoSuchMethodException e) {
                        throw new CompileException(
                            "Annotation \"" + annotationType.getName() + "\" has no element \"" + name + "\"",
                            null
                        );
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }

                @Override public String
                toString() { return "@" + annotationTypeIClass; }
            };
        }

        return result;
    }

    /**
     * @return The underlying {@link Class java.lang.Class}
     */
    public Class<?>
    getClazz() { return this.clazz; }

    /**
     * @return E.g. {@code "int"}, {@code "int[][]"}, {@code "pkg1.pkg2.Outer$Inner[]"}
     */
    @Override public String
    toString() {
        int      brackets = 0;
        Class<?> c        = this.clazz;
        while (c.isArray()) {
            ++brackets;
            c = c.getComponentType();
        }
        String s = c.getName();

        // Must not strip "java.lang.", because some pieces of code rely on "toString()" returning the COMPLETE
        // qualified name.
//        if (s.startsWith("java.lang.")) s = s.substring(10);

        while (brackets-- > 0) s += "[]";
        return s;
    }

    private
    class ReflectionIConstructor extends IConstructor {

        ReflectionIConstructor(Constructor<?> constructor) { this.constructor = constructor; }

        @Override public Map<String, GenericType[]> getTypeParameterBounds() {
            Map<String, GenericType[]> result = new HashMap<>();
            for (java.lang.reflect.TypeVariable<?> variable : this.constructor.getTypeParameters()) {
                java.lang.reflect.Type[] bounds = variable.getBounds();
                GenericType[] types = new GenericType[bounds.length];
                for (int i = 0; i < bounds.length; i++) types[i] = bounds[i] instanceof Class
                    ? ReflectionIClass.this.classToRawType((Class<?>) bounds[i]) : ReflectionIClass.this.typeToGenericType(bounds[i]);
                result.put(variable.getName(), types);
            }
            return result;
        }

        @Override public GenericType[] getGenericThrownExceptions() {
            java.lang.reflect.Type[] exceptions = this.constructor.getGenericExceptionTypes();
            GenericType[] result = new GenericType[exceptions.length];
            for (int i = 0; i < result.length; i++) result[i] = exceptions[i] instanceof Class
                ? ReflectionIClass.this.classToRawType((Class<?>) exceptions[i]) : ReflectionIClass.this.typeToGenericType(exceptions[i]);
            return result;
        }

        @Override public String[] getTypeParameterNames() {
            java.lang.reflect.TypeVariable<?>[] variables = this.constructor.getTypeParameters();
            String[] result = new String[variables.length];
            for (int i = 0; i < result.length; i++) result[i] = variables[i].getName();
            return result;
        }
        @Override public GenericType[] getGenericParameterTypes() throws CompileException {
            java.lang.reflect.Type[] parameters = this.constructor.getGenericParameterTypes();
            GenericType[] result = new GenericType[parameters.length];
            for (int i = 0; i < result.length; i++) result[i] = parameters[i] instanceof Class
                ? ReflectionIClass.this.classToRawType((Class<?>) parameters[i]) : ReflectionIClass.this.typeToGenericType(parameters[i]);
            int count = this.getParameterTypes().length;
            if (result.length > count) result = java.util.Arrays.copyOfRange(result, result.length - count, result.length);
            return result;
        }
        // Implement IMember.
        @Override public Access
        getAccess() { return ReflectionIClass.modifiers2Access(this.constructor.getModifiers()); }

        @Override public IAnnotation[]
        getAnnotations() { return new IAnnotation[0]; }

        @Override public boolean
        isVarargs() {
            // TRANSIENT is identical with VARARGS.
            return Modifier.isTransient(this.constructor.getModifiers());
        }

        // Implement "IConstructor".
        @Override public IClass[]
        getParameterTypes2() throws CompileException {
            IClass[] parameterTypes = ReflectionIClass.this.classesToIClasses(this.constructor.getParameterTypes());

            // The JAVADOC of java.lang.reflect.Constructor does not document it, but "getParameterTypes()" includes
            // the synthetic "enclosing instance" parameter.
            IClass outerClass = ReflectionIClass.this.getOuterIClass();
            if (outerClass != null) {
                if (parameterTypes.length < 1) {
                    throw new CompileException(
                        "Constructor \"" + this.constructor + "\" lacks synthetic enclosing instance parameter",
                        null
                    );
                }
                if (parameterTypes[0] != outerClass) {
                    throw new CompileException((
                        "Enclosing instance parameter of constructor \""
                        + this.constructor
                        + "\" has wrong type -- \""
                        + parameterTypes[0]
                        + "\" vs. \""
                        + outerClass
                        + "\""
                    ), null);
                }
                IClass[] tmp = new IClass[parameterTypes.length - 1];
                System.arraycopy(parameterTypes, 1, tmp, 0, tmp.length);
                parameterTypes = tmp;
            }

            return parameterTypes;
        }

        @Override public MethodDescriptor
        getDescriptor2() {
            Class<?>[] parameterTypes       = this.constructor.getParameterTypes();
            String[]   parameterDescriptors = new String[parameterTypes.length];
            for (int i = 0; i < parameterDescriptors.length; ++i) {
                parameterDescriptors[i] = Descriptor.fromClassName(parameterTypes[i].getName());
            }
            return new MethodDescriptor(Descriptor.VOID, parameterDescriptors);
        }

        @Override public IClass[]
        getThrownExceptions2() {
            return ReflectionIClass.this.classesToIClasses(this.constructor.getExceptionTypes());
        }

        final Constructor<?> constructor;
    }

    public
    class ReflectionIMethod extends IMethod {

        @Override public Map<String, GenericType[]> getTypeParameterBounds() {
            Map<String, GenericType[]> result = new HashMap<>();
            for (java.lang.reflect.TypeVariable<?> variable : this.method.getTypeParameters()) {
                java.lang.reflect.Type[] bounds = variable.getBounds();
                GenericType[] types = new GenericType[bounds.length];
                for (int i = 0; i < bounds.length; i++) types[i] = bounds[i] instanceof Class
                    ? ReflectionIClass.this.classToRawType((Class<?>) bounds[i]) : ReflectionIClass.this.typeToGenericType(bounds[i]);
                result.put(variable.getName(), types);
            }
            return result;
        }

        @Override public GenericType[] getGenericThrownExceptions() {
            java.lang.reflect.Type[] exceptions = this.method.getGenericExceptionTypes();
            GenericType[] result = new GenericType[exceptions.length];
            for (int i = 0; i < result.length; i++) result[i] = exceptions[i] instanceof Class
                ? ReflectionIClass.this.classToRawType((Class<?>) exceptions[i]) : ReflectionIClass.this.typeToGenericType(exceptions[i]);
            return result;
        }

        ReflectionIMethod(Method method) {
            this.method = method;

            GenericType grt = ReflectionIClass.this.typeToGenericType(method.getGenericReturnType());
            if (grt != null && grt != GenericType.ERASED) ReflectionIClass.this.genericReturnTypes.put(this, grt);

            if (method.getTypeParameters().length > 0) {
                String[] ns = new String[method.getTypeParameters().length];
                for (int i = 0; i < ns.length; i++) ns[i] = method.getTypeParameters()[i].getName();
                ReflectionIClass.this.methodTypeParameterNames.put(this, ns);
            }

            List<GenericType> gpts = new ArrayList<>();
            for (java.lang.reflect.Type pt : method.getGenericParameterTypes()) {
                GenericType gpt = pt instanceof Class ? ReflectionIClass.this.classToRawType((Class<?>) pt)
                    : ReflectionIClass.this.typeToGenericType(pt);
                if (gpt == GenericType.ERASED) { gpts = null; break; }
                gpts.add(gpt);
            }
            if (gpts != null) {
                ReflectionIClass.this.genericParameterTypes.put(this, gpts.toArray(new GenericType[gpts.size()]));
            }
        }

        // Implement IMember.
        @Override public Access
        getAccess() { return ReflectionIClass.modifiers2Access(this.method.getModifiers()); }

        @Override public IAnnotation[]
        getAnnotations() { return new IAnnotation[0]; }

        // Implement "IMethod".
        @Override public String
        getName() { return this.method.getName(); }

        @Override public boolean
        isVarargs() {

            // VARARGS is identical with TRANSIENT.
            return Modifier.isTransient(this.method.getModifiers());
        }

        @Override public IClass[]
        getParameterTypes2() { return ReflectionIClass.this.classesToIClasses(this.method.getParameterTypes()); }

        @Override public boolean
        isStatic() { return Modifier.isStatic(this.method.getModifiers()); }

        @Override public boolean isFinal() { return Modifier.isFinal(this.method.getModifiers()); }

        @Override public boolean
        isAbstract() { return Modifier.isAbstract(this.method.getModifiers()); }

        @Override public IClass
        getReturnType() { return ReflectionIClass.this.classToIClass(this.method.getReturnType()); }

        @Override public IClass[]
        getThrownExceptions2() { return ReflectionIClass.this.classesToIClasses(this.method.getExceptionTypes()); }

        private final Method method;
    }

    private
    class ReflectionIField extends IField {

        ReflectionIField(Field field) {
            this.field = field;

            GenericType gft = ReflectionIClass.this.typeToGenericType(field.getGenericType());
            if (gft != null && gft != GenericType.ERASED) ReflectionIClass.this.genericFieldTypes.put(this, gft);
        }

        // Implement IMember.
        @Override public Access
        getAccess() { return ReflectionIClass.modifiers2Access(this.field.getModifiers()); }

        @Override public IAnnotation[]
        getAnnotations() { return new IAnnotation[0]; }

        // Implement "IField".

        @Override public String
        getName() { return this.field.getName(); }

        @Override public boolean
        isStatic() { return Modifier.isStatic(this.field.getModifiers()); }

        @Override public IClass
        getType() { return ReflectionIClass.this.classToIClass(this.field.getType()); }

        @Override public String
        toString() {
            return Descriptor.toString(this.getDeclaringIClass().getDescriptor()) + "." + this.getName();
        }

        /**
         * This implementation of {@link IClass.IField#getConstantValue()} is not completely correct:
         * <ul>
         *   <li>
         *     It treats non-static fields as non-constant
         *   </li>
         *   <li>
         *     Even fields with a <em>non-constant</em> initializer are identified as constant. (The value of that
         *     field may be different in a different JVM instance -- the classical example is {@link
         *     java.io.File#separator}.)
         *   </li>
         * </ul>
         * <p>
         *   Notice that enum constants are <em>not</em> constant expression (despite the similarity of names).
         * </p>
         */
        @Override public Object
        getConstantValue() throws CompileException {
            int      mod   = this.field.getModifiers();
            Class<?> clazz = this.field.getType();
            if (
                Modifier.isStatic(mod)
                && Modifier.isFinal(mod)
                && (clazz.isPrimitive() || clazz == String.class)
            ) {
                try {
                    return this.field.get(null);
                } catch (IllegalAccessException ex) {
                    throw new CompileException( // SUPPRESS CHECKSTYLE AvoidHidingCause
                        "Field \"" + this.field.getName() + "\" is not accessible",
                        (Location) null
                    );
                }
            }
            return IClass.NOT_CONSTANT;
        }

        final Field field;
    }

    /**
     * Loads {@link Class} through {@link IClassLoader} to ensure unique {@link IClass}es.
     */
    private IClass
    classToIClass(Class<?> c) {
        IClass iClass;
        try {
            iClass = this.iClassLoader.loadIClass(Descriptor.fromClassName(c.getName()));
        } catch (ClassNotFoundException ex) {
            throw new InternalCompilerException("Loading IClass \"" + c.getName() + "\": " + ex);
        }
        if (iClass == null) {
            throw new InternalCompilerException((
                "Cannot load class \"" + c.getName() + "\" through the given ClassLoader"
            ));
        }
        return iClass;
    }

    /**
     * @see #classToIClass(Class)
     */
    private IClass[]
    classesToIClasses(Class<?>[] cs) {

        IClass[] result = new IClass[cs.length];
        for (int i = 0; i < cs.length; ++i) result[i] = this.classToIClass(cs[i]);

        return result;
    }

    private IMethod[]
    methodsToIMethods(Method[] methods) {

        List<IMethod> result = new ArrayList<IMethod>();
        for (Method method : methods) {
            if (!method.isBridge()) result.add(new ReflectionIMethod(method));
        }
        return result.toArray(new IMethod[0]);
    }

    private IField[]
    fieldsToIFields(Field[] fields) {

        IField[] result = new IField[fields.length];
        for (int i = 0; i < fields.length; ++i) result[i] = new ReflectionIField(fields[i]);

        return result;
    }

    /**
     * @return The declared (generic) return type of the given method, or {@code null} if it is not generic
     */
    public GenericType
    getGenericReturnType(IMethod im) { return this.genericReturnTypes.get(im); }

    /**
     * @return The declared (generic) type of the given field, or {@code null} if it is not generic
     */
    public GenericType
    getGenericFieldType(IField ifld) { return this.genericFieldTypes.get(ifld); }

    /**
     * @return The declared (generic) parameter types of the given method, or {@code null}
     */
    public GenericType[]
    getGenericParameterTypes(IMethod im) { return this.genericParameterTypes.get(im); }

    /**
     * @return The names of the type parameters declared by the given method, or {@code null}
     */
    public String[]
    getMethodTypeParameterNames(IMethod im) { return this.methodTypeParameterNames.get(im); }

    /**
     * Converts a "java.lang.reflect.Type" into a {@link GenericType}; returns {@code null} for non-generic types
     * and {@link GenericType#ERASED} for unrepresentable types.
     */
    GenericType
    typeToGenericType(java.lang.reflect.Type t) {

        if (t instanceof Class) {

            // Not generic, or a raw type used as a type argument; needs no substitution.
            return null;
        }
        if (t instanceof java.lang.reflect.TypeVariable) {
            return new GenericType.TypeVariable(((java.lang.reflect.TypeVariable<?>) t).getName());
        }
        if (t instanceof java.lang.reflect.GenericArrayType) {
            GenericType ct = this.typeToGenericType(((java.lang.reflect.GenericArrayType) t).getGenericComponentType());
            return ct == null || ct == GenericType.ERASED ? GenericType.ERASED : new GenericType.ArrayOf(ct);
        }
        if (t instanceof java.lang.reflect.WildcardType) {
            java.lang.reflect.WildcardType wildcard = (java.lang.reflect.WildcardType) t;
            java.lang.reflect.Type[] bounds = wildcard.getLowerBounds();
            boolean upper = bounds.length == 0;
            if (upper) bounds = wildcard.getUpperBounds();
            if (bounds.length != 1) return GenericType.ERASED;
            GenericType bound = bounds[0] instanceof Class
                ? this.classToRawType((Class<?>) bounds[0]) : this.typeToGenericType(bounds[0]);
            return bound == null || bound == GenericType.ERASED ? GenericType.ERASED : new GenericType.Wildcard(upper, bound);
        }
        if (t instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) t;
            java.lang.reflect.Type raw = pt.getRawType();
            if (!(raw instanceof Class)) return GenericType.ERASED;

            IClass rawIClass;
            try {
                rawIClass = this.iClassLoader.loadIClass(Descriptor.fromClassName(((Class<?>) raw).getName()));
            } catch (ClassNotFoundException cnfe) {
                return GenericType.ERASED;
            }

            java.lang.reflect.Type[] ts = pt.getActualTypeArguments();
            GenericType[] args = new GenericType[ts.length];
            for (int i = 0; i < ts.length; i++) {
                java.lang.reflect.Type arg = ts[i];

                if (arg instanceof Class) {
                    args[i] = this.classToRawType((Class<?>) arg);
                    if (args[i] == null) return GenericType.ERASED;
                    continue;
                }

                if (arg instanceof java.lang.reflect.WildcardType) {
                    args[i] = this.typeToGenericType(arg);
                    if (args[i] == null || args[i] == GenericType.ERASED) return GenericType.ERASED;
                    continue;
                }

                GenericType a = this.typeToGenericType(arg);
                if (a == null || a == GenericType.ERASED) return GenericType.ERASED;
                args[i] = a;
            }
            GenericType owner = pt.getOwnerType() == null || java.lang.reflect.Modifier.isStatic(((Class<?>) raw).getModifiers()) ? null : this.typeToGenericType(pt.getOwnerType());
            if (owner == GenericType.ERASED) return GenericType.ERASED;
            return new GenericType.Parameterized(rawIClass, args, owner);
        }
        return GenericType.ERASED;
    }

    @Nullable private GenericType.Raw
    classToRawType(Class<?> c) {
        try {
            return new GenericType.Raw(this.iClassLoader.loadIClass(Descriptor.fromClassName(c.getName())));
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    private final Map<IMethod, GenericType>
    genericReturnTypes = new HashMap<>();

    private final Map<IField, GenericType>
    genericFieldTypes = new HashMap<>();

    private final Map<IMethod, GenericType[]>
    genericParameterTypes = new HashMap<>();

    private final Map<IMethod, String[]>
    methodTypeParameterNames = new HashMap<>();

    private static Access
    modifiers2Access(int modifiers) {
        return (
            Modifier.isPrivate(modifiers)   ? Access.PRIVATE   :
            Modifier.isProtected(modifiers) ? Access.PROTECTED :
            Modifier.isPublic(modifiers)    ? Access.PUBLIC    :
            Access.DEFAULT
        );
    }
}
