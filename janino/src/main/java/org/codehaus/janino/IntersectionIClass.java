/* Janino - An embedded Java compiler. Licensed under the Apache License, Version 2.0. */
package org.codehaus.janino;

import java.util.Arrays;
import org.codehaus.commons.nullanalysis.Nullable;

/** A compile-time intersection; its descriptor is the erasure of its leftmost bound. */
final class IntersectionIClass extends IClass {
    final IClass[] bounds;
    private final IClass objectType;

    IntersectionIClass(IClass[] bounds, IClass objectType) {
        this.bounds = bounds.clone();
        this.objectType = objectType;
    }
    @Override public GenericType[] getGenericSupertypes() {
        GenericType[] result = new GenericType[this.bounds.length];
        for (int i = 0; i < result.length; i++) result[i] = new GenericType.Raw(this.bounds[i]);
        return result;
    }
    @Override protected ITypeVariable[] getITypeVariables2() { return new ITypeVariable[0]; }
    @Override @Nullable protected IClass getComponentType2() { return null; }
    @Override protected IClass[] getDeclaredIClasses2() { return new IClass[0]; }
    @Override protected IConstructor[] getDeclaredIConstructors2() { return new IConstructor[0]; }
    @Override protected IField[] getDeclaredIFields2() { return new IField[0]; }
    @Override protected IMethod[] getDeclaredIMethods2() { return new IMethod[0]; }
    @Override @Nullable protected IClass getDeclaringIClass2() { return null; }
    @Override protected String getDescriptor2() { return this.bounds[0].getDescriptor(); }
    @Override protected IClass[] getInterfaces2() {
        IClass[] result = this.bounds[0].isInterface() ? this.bounds.clone() : Arrays.copyOfRange(this.bounds, 1, this.bounds.length);
        for (int i = 0; i < result.length; i++) result[i] = erased(result[i]);
        return result;
    }
    @Override @Nullable protected IClass getOuterIClass2() { return null; }
    @Override protected IClass getSuperclass2() { return this.bounds[0].isInterface() ? this.objectType : erased(this.bounds[0]); }
    private static IClass erased(IClass type) {
        return type instanceof ParameterizedIClass ? ((ParameterizedIClass) type).rawType : type;
    }
    @Override public boolean isAbstract() { return true; }
    @Override public boolean isArray() { return false; }
    @Override public boolean isFinal() { return false; }
    @Override public boolean isEnum() { return false; }
    @Override public boolean isInterface() { return this.bounds[0].isInterface(); }
    @Override public boolean isPrimitive() { return false; }
    @Override public Access getAccess() { return Access.PUBLIC; }
    @Override public boolean isPrimitiveNumeric() { return false; }
    @Override public boolean equals(@Nullable Object other) {
        return other instanceof IntersectionIClass && Arrays.equals(this.bounds, ((IntersectionIClass) other).bounds);
    }
    @Override public int hashCode() { return Arrays.hashCode(this.bounds); }
    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        for (IClass bound : this.bounds) { if (result.length() != 0) result.append(" & "); result.append(bound); }
        return result.toString();
    }
}
