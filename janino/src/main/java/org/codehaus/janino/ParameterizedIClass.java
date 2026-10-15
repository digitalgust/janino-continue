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

package org.codehaus.janino;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;



/**
 * A parameterized type like "{@code List<String>}", represented as an {@link IClass} which delegates everything to
 * the RAW class; the type ARGUMENTS are carried along only for the compiler's type analysis ("substitution of type
 * variables", see JLS8 4.5.2), while all other aspects, most importantly the FIELD DESCRIPTOR, are those of the raw
 * class (erasure). Thus this object can flow through the entire (erasure-oriented) compilation process without
 * breaking anything, while member accesses through it can replace the declared type variables with the actual type
 * arguments.
 * <p>
 *   Constructing this wrapper does not itself verify type argument bounds (JLS8 4.5.1). Method inference and
 *   method-reference compatibility perform their own bound and containment checks; general generic type checking
 *   remains incomplete.
 * </p>
 */
public
class ParameterizedIClass extends IClass implements IParameterizedType {

    /**
     * The raw type, e.g. "{@code List}" for "{@code List<String>}".
     */
    public final IClass rawType;

    /**
     * The actual type arguments, e.g. "{@code String}" for "{@code List<String>}".
     */
    public final IType[] actualTypeArguments;

    @Nullable public final IType ownerType;

    public ParameterizedIClass(IClass rawType, IType[] actualTypeArguments) { this(rawType, actualTypeArguments, null); }

    public ParameterizedIClass(IClass rawType, IType[] actualTypeArguments, @Nullable IType ownerType) {
        this.rawType             = rawType;
        this.actualTypeArguments = actualTypeArguments;
        this.ownerType           = ownerType;
    }

    // Implement "IParameterizedType".

    @Override public IType[] getActualTypeArguments() { return this.actualTypeArguments; }
    @Override public IType   getRawType()             { return this.rawType; }

    // Implement "IClass" through delegation to the raw type.

    @Override protected IAnnotation[] getIAnnotations2() throws CompileException { return this.rawType.getIAnnotations(); }
    @Override public java.util.Map<String, GenericType[]> getTypeParameterBounds() throws CompileException { return this.rawType.getTypeParameterBounds(); }
    @Override public GenericType[] getGenericSupertypes() throws CompileException { return this.rawType.getGenericSupertypes(); }

    @Override protected ITypeVariable[]  getITypeVariables2() throws CompileException        { return this.rawType.getITypeVariables(); }
    @Override @Nullable protected IClass getComponentType2()         { return this.rawType.getComponentType(); }
    @Override protected IClass[]         getDeclaredIClasses2() throws CompileException      { return this.rawType.getDeclaredIClasses(); }
    @Override protected IConstructor[]   getDeclaredIConstructors2() { return this.rawType.getDeclaredIConstructors(); }
    @Override protected IField[]         getDeclaredIFields2()       { return this.rawType.getDeclaredIFields(); }
    @Override protected IMethod[]        getDeclaredIMethods2()      { return this.rawType.getDeclaredIMethods(); }
    @Override @Nullable protected IClass getDeclaringIClass2() throws CompileException       { return this.rawType.getDeclaringIClass(); }
    @Override protected String           getDescriptor2()            { return this.rawType.getDescriptor(); }
    @Override protected IClass[]         getInterfaces2() throws CompileException            { return this.rawType.getInterfaces(); }
    @Override @Nullable protected IClass getOuterIClass2() throws CompileException           { return this.rawType.getOuterIClass(); }
    @Override @Nullable protected IClass getSuperclass2() throws CompileException            { return this.rawType.getSuperclass(); }
    @Override public boolean             isAbstract()                { return this.rawType.isAbstract(); }
    @Override public boolean             isArray()                   { return this.rawType.isArray(); }
    @Override public boolean             isFinal()                   { return this.rawType.isFinal(); }
    @Override public boolean             isEnum()                    { return this.rawType.isEnum(); }
    @Override public boolean             isInterface()               { return this.rawType.isInterface(); }
    @Override public boolean             isPrimitive()               { return false; }
    @Override public Access              getAccess()                 { return this.rawType.getAccess(); }
    @Override public boolean             isPrimitiveNumeric()        { return false; }

    @Override public String
    toString() {
        StringBuilder sb = new StringBuilder(this.rawType.toString()).append('<');
        for (int i = 0; i < this.actualTypeArguments.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(this.actualTypeArguments[i]);
        }
        return sb.append('>').toString();
    }

    @Override public boolean
    equals(@Nullable Object obj) {
        if (!(obj instanceof ParameterizedIClass)) return false;
        ParameterizedIClass that = (ParameterizedIClass) obj;
        if (that.rawType != this.rawType || !sameType(this.ownerType, that.ownerType)
            || that.actualTypeArguments.length != this.actualTypeArguments.length) return false;
        for (int i = 0; i < this.actualTypeArguments.length; i++) {
            if (!sameType(this.actualTypeArguments[i], that.actualTypeArguments[i])) return false;
        }
        return true;
    }

    private static boolean sameType(@Nullable IType a, @Nullable IType b) {
        if (a == b) return true;
        if (a instanceof IWildcardType && b instanceof IWildcardType) {
            IWildcardType aw = (IWildcardType) a, bw = (IWildcardType) b;
            return sameType(aw.getUpperBound(), bw.getUpperBound()) && sameType(aw.getLowerBound(), bw.getLowerBound());
        }
        return a != null && a.equals(b);
    }

    private static int typeHash(@Nullable IType type) {
        if (type == null) return 0;
        if (type instanceof IWildcardType) {
            IWildcardType wildcard = (IWildcardType) type;
            return 31 * typeHash(wildcard.getUpperBound()) + typeHash(wildcard.getLowerBound());
        }
        return type.hashCode();
    }

    @Override public int
    hashCode() {
        int result = 31 * this.rawType.hashCode() + typeHash(this.ownerType);
        for (IType argument : this.actualTypeArguments) result = 31 * result + typeHash(argument);
        return result;
    }
}
