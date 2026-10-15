
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

import java.util.List;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.EnclosingScopeOfTypeDeclaration;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.Wildcard;
import org.codehaus.janino.util.ClassFile;

/** Encodes generic declaration signatures; semantic type resolution stays with the compilation unit. */
final class UnitCompilerSignatureEncoder {

    /** The semantic operations required by this component. */
    interface Context {
        IClass getRawType(Type type) throws CompileException;
        GenericType genericType(Type type) throws CompileException;
        IClass resolve(TypeDeclaration declaration);
    }

    private final Context context;

    UnitCompilerSignatureEncoder(Context context) {
        this.context = context;
    }


    /** Emit declarations together with references to class type variables (JVMS 4.7.9.1). */
    void
    addClassSignature(AbstractTypeDeclaration declaration, ClassFile cf) throws CompileException {
        // Object has no superclass; a self-referencing Signature makes binary readers infer a cycle.
        if (cf.superclass == 0) return;
        TypeParameter[] parameters = declaration.getOptionalTypeParameters();
        StringBuilder signature = new StringBuilder();
        if (parameters != null && parameters.length > 0) {
            signature.append('<');
            for (TypeParameter parameter : parameters) {
                signature.append(parameter.name).append(':');
                if (parameter.bound == null || parameter.bound.length == 0) {
                    signature.append("Ljava/lang/Object;");
                } else {
                    for (int i = 0; i < parameter.bound.length; i++) {
                        ReferenceType bound = parameter.bound[i];
                        UnitCompiler.setScopeIfUnset(bound, new EnclosingScopeOfTypeDeclaration(declaration));
                        if (i > 0 || this.context.getRawType(bound).isInterface()) signature.append(':');
                        signature.append(this.toTypeArgumentSignature(bound));
                    }
                }
            }
            signature.append('>');
        }
        Type superclass = declaration instanceof NamedClassDeclaration ? ((NamedClassDeclaration) declaration).extendedType : null;
        if (declaration instanceof EnumDeclaration) {
            signature.append("Ljava/lang/Enum<").append(this.context.resolve(declaration).getDescriptor()).append(">;");
        } else {
            signature.append(superclass == null ? "Ljava/lang/Object;" : this.toTypeArgumentSignature(superclass));
        }
        Type[] interfaces = declaration instanceof NamedClassDeclaration ? ((NamedClassDeclaration) declaration).implementedTypes
            : declaration instanceof InterfaceDeclaration ? ((InterfaceDeclaration) declaration).extendedTypes : new Type[0];
        for (Type interfaze : interfaces) signature.append(this.toTypeArgumentSignature(interfaze));
        // Anonymous classes have an implicit parent that is not represented by the declaration below.
        if (declaration instanceof NamedClassDeclaration || declaration instanceof InterfaceDeclaration) {
            cf.addSignatureAttribute(signature.toString());
        }
    }

    /** @return The field signature (JVMS 4.7.9.1), or {@code null} if the type is not generic */
    @Nullable String
    toFieldSignature(Type t) throws CompileException {

        if (t instanceof ArrayType) {
            String cts = this.toFieldSignature(((ArrayType) t).componentType);
            return cts == null ? null : "[" + cts;
        }
        if (!(t instanceof ReferenceType)) return null;
        GenericType type = this.context.genericType(t);
        return type instanceof GenericType.Raw ? null : this.genericSignature(type);
    }

    private String genericSignature(GenericType type) throws CompileException {
        if (type instanceof GenericType.Raw) return ((GenericType.Raw) type).iClass.getDescriptor();
        if (type instanceof GenericType.TypeVariable) return "T" + ((GenericType.TypeVariable) type).name + ";";
        if (type instanceof GenericType.ArrayOf) return "[" + this.genericSignature(((GenericType.ArrayOf) type).componentType);
        if (type instanceof GenericType.Wildcard) {
            GenericType.Wildcard wildcard = (GenericType.Wildcard) type;
            String bound = this.genericSignature(wildcard.bound);
            return wildcard.upperBound && bound.equals("Ljava/lang/Object;") ? "*" : (wildcard.upperBound ? "+" : "-") + bound;
        }
        GenericType.Parameterized parameterized = (GenericType.Parameterized) type;
        String descriptor = parameterized.rawType.getDescriptor();
        StringBuilder result;
        if (parameterized.ownerType != null && parameterized.rawType.getDeclaringIClass() != null) {
            String owner = this.genericSignature(parameterized.ownerType);
            result = new StringBuilder(owner.substring(0, owner.length() - 1));
            IClass declaring = parameterized.rawType.getDeclaringIClass();
            // Like javac, use a binary '$' name when the owner has no type arguments;
            // JDK8 signature readers reject a dotted unparameterized owner (Outer.Inner<T>).
            result.append(owner.indexOf('<') >= 0 ? '.' : '$')
                .append(descriptor.substring(declaring.getDescriptor().length(), descriptor.length() - 1));
        } else result = new StringBuilder(descriptor.substring(0, descriptor.length() - 1));
        if (parameterized.typeArguments.length > 0) {
            result.append('<');
            for (GenericType argument : parameterized.typeArguments) result.append(this.genericSignature(argument));
            result.append('>');
        }
        return result.append(';').toString();
    }

    /**
     * Signature of a method/constructor declaration: "(" + parameter type signatures + ")"
     * + return type signature ("V" for constructors), or {@code null} iff no parameter and
     * no return type is generic. Non-generic types are represented by their erased
     * descriptors; method type parameters precede the parameter and return signatures.
     */
    @Nullable String
    toMethodSignature(FunctionDeclarator fd) throws CompileException {

        TypeParameter[] typeParameters = fd.getOptionalTypeParameters();

        FormalParameter[] fps = fd.formalParameters.parameters;
        String[]         paramSigs = new String[fps.length];
        boolean          anyGeneric = typeParameters != null && typeParameters.length > 0;
        for (int i = 0; i < fps.length; i++) {
            paramSigs[i] = this.toFieldSignature(fps[i].type);
            if (paramSigs[i] != null) anyGeneric = true;
        }
        String returnSig = null;
        if (fd instanceof MethodDeclarator) {
            returnSig = this.toFieldSignature(((MethodDeclarator) fd).type);
            if (returnSig != null) anyGeneric = true;
        }
        for (Type thrown : fd.thrownExceptions) {
            if (this.toFieldSignature(thrown) != null) anyGeneric = true;
        }
        if (!anyGeneric) return null;

        StringBuilder sb = new StringBuilder();
        if (typeParameters != null && typeParameters.length > 0) {
            sb.append('<');
            for (TypeParameter parameter : typeParameters) {
                sb.append(parameter.name).append(':');
                if (parameter.bound == null || parameter.bound.length == 0) {
                    sb.append("Ljava/lang/Object;");
                } else {
                    for (int i = 0; i < parameter.bound.length; i++) {
                        ReferenceType bound = parameter.bound[i];
                        UnitCompiler.setScopeIfUnset(bound, fd);
                        boolean variable = bound.identifiers.length == 1
                            && UnitCompiler.isTypeVariableOfEnclosingDeclaration(bound);
                        if (i > 0 || !variable && this.context.getRawType(bound).isInterface()) sb.append(':');
                        sb.append(this.toTypeArgumentSignature(bound));
                    }
                }
            }
            sb.append('>');
        }
        sb.append('(');
        for (int i = 0; i < fps.length; i++) {
            String s = paramSigs[i];
            // The AST stores the element type of the final varargs parameter.
            if (i == fps.length - 1 && fd.formalParameters.variableArity) sb.append('[');
            sb.append(s != null ? s : this.context.getRawType(fps[i].type).getDescriptor());
        }
        sb.append(')');
        if (fd instanceof MethodDeclarator) {
            sb.append(returnSig != null ? returnSig : this.context.getRawType(((MethodDeclarator) fd).type).getDescriptor());
        } else {
            sb.append('V');
        }
        for (Type thrown : fd.thrownExceptions) sb.append('^').append(this.toTypeArgumentSignature(thrown));
        return sb.toString();
    }

    private String
    toTypeArgumentSignature(Type t) throws CompileException {

        if (t instanceof ArrayType) {
            String cts = this.toFieldSignature(((ArrayType) t).componentType);
            return cts == null ? this.context.getRawType(t).getDescriptor() : "[" + cts;
        }
        if (t instanceof ReferenceType) {
            String fs = this.toFieldSignature((ReferenceType) t);
            return fs != null ? fs : this.context.getRawType(t).getDescriptor();
        }
        return this.context.getRawType(t).getDescriptor();  // Primitive or plain class.
    }
}
