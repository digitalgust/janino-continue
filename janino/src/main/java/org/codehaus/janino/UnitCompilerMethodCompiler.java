
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
import java.util.List;
import java.util.Map;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.iterator.Iterables;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IInvocable;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FieldDeclarationOrInitializer;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Located;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Visitor.FunctionDeclaratorVisitor;
import org.codehaus.janino.util.Annotatable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.ClassFileException;
import org.codehaus.janino.util.ClassFile.MethodInfo;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute;

/** Compiles methods, constructors, initializers and bridge methods using a temporary method context. */
final class UnitCompilerMethodCompiler {

    /** Semantic operations and current compilation settings supplied by the compilation unit. */
    interface Context {
        CodeContext
        getCodeContext();
        LocalVariable
        allocateLocalVariableAndMarkAsInitialized(boolean finaL, IType localVariableType);
        int
        getTargetVersion();
        LocalVariableSlot
        allocateLocalVariableSlotAndMarkAsInitialized(IType localVariableType, @Nullable String localVariableName);
        LocalVariable
        getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException;
        @Nullable IMethod
        findGenericOverride(IClass type, IMethod base) throws CompileException;
        boolean compatibleOverrideReturn(IMethod method, IMethod base) throws CompileException;

        boolean compatibleOverrideReturn(IMethod method, IMethod base, IClass type) throws CompileException;
        IClass[] inheritedThrownExceptions(IMethod method, IClass receiver) throws CompileException;
        IClass
        getRawType(Type t) throws CompileException;
        IType
        getType(Type t) throws CompileException;
        IClass
        resolve(TypeDeclaration td);
        IType
        getReturnType(FunctionDeclarator fd) throws CompileException;
        IClass.IMethod
        toIMethod(MethodDeclarator methodDeclarator);
        IClass.IInvocable
        toIInvocable(FunctionDeclarator fd);
        void
        compileError(String message) throws CompileException;
        void
        compileError(String message, @Nullable Location location) throws CompileException;
        void
        warning(String handle, String message, @Nullable Location location) throws CompileException;
        @Nullable CodeContext
        replaceCodeContext(@Nullable CodeContext newCodeContext);
        short
        accessFlags(Modifier[] modifiers) throws CompileException;
        Modifier[]
        accessModifiers(Location location, String... keywords);
        short
        changeAccessibility(short accessFlags, short newAccessibility);
        int compileErrorCount();
        boolean debugLines();
        boolean debugVars();
        boolean parameters();
        IClass[] anonymousInitializerExceptions(AnonymousClassDeclaration owner);
        boolean looksLikeTypeParameter(String name);
        IType capturedFieldType(IField field) throws CompileException;
    }

    private final Context context;
    private final IClassLoader iClassLoader;
    private final UnitCompilerSignatureEncoder signatureEncoder;
    private final UnitCompilerAnnotationCompiler annotationCompiler;
    private final UnitCompilerBytecodeEmitter bytecodeEmitter;
    private final UnitCompilerStatementCompiler statementCompiler;

    UnitCompilerMethodCompiler(
        Context context,
        IClassLoader iClassLoader,
        UnitCompilerSignatureEncoder signatureEncoder,
        UnitCompilerAnnotationCompiler annotationCompiler,
        UnitCompilerBytecodeEmitter bytecodeEmitter,
        UnitCompilerStatementCompiler statementCompiler
    ) {
        this.context = context;
        this.iClassLoader = iClassLoader;
        this.signatureEncoder = signatureEncoder;
        this.annotationCompiler = annotationCompiler;
        this.bytecodeEmitter = bytecodeEmitter;
        this.statementCompiler = statementCompiler;
    }

    /**
     * Creates class/interface initialization method iff there is any initialization code.
     *
     * @param td         The type declaration
     * @param cf         The class file into which to put the method
     * @param statements The statements for the method (possibly empty)
     */
    void
    maybeCreateInitMethod(
        TypeDeclaration      td,
        ClassFile            cf,
        List<BlockStatement> statements
    ) throws CompileException {

        // Create class/interface initialization method iff there is any initialization code.
        if (this.statementCompiler.generatesCode2(statements)) {
            MethodDeclarator md = new MethodDeclarator(
                td.getLocation(),                                                   // location
                null,                                                               // docComment
                UnitCompilerMethodCompiler.this.context.accessModifiers(td.getLocation(), "static", "public"), // modifiers
                null,                                                               // typeParameters
                new PrimitiveType(td.getLocation(), Primitive.VOID),                // type
                "<clinit>",                                                         // name
                new FormalParameters(td.getLocation()),                             // formalParameters
                new ReferenceType[0],                                               // thrownExceptions
                null,                                                               // defaultValue
                statements                                                          // statements
            );
            md.setDeclaringType(td);
            this.compile(md, cf);
        }
    }

    /**
     * Compiles all of the methods for this declaration
     * <p>
     *   NB: as a side effect this will fill in the synthetic field map
     * </p
     */
    void
    compileDeclaredMethods(TypeDeclaration typeDeclaration, ClassFile cf) throws CompileException {
        this.compileDeclaredMethods(typeDeclaration, cf, 0);
    }

    /**
     * Compiles methods for this declaration starting at <var>startPos</var>.
     *
     * @param startPos Starting parameter to fill in
     */
    void
    compileDeclaredMethods(TypeDeclaration typeDeclaration, ClassFile cf, int startPos) throws CompileException {

        // Notice that as a side effect of compiling methods, synthetic "class-dollar" methods (which implement class
        // literals) are generated on-the fly. Hence, we must not use an Iterator here.

        for (int i = startPos; i < typeDeclaration.getMethodDeclarations().size(); ++i) {
            MethodDeclarator md = (MethodDeclarator) typeDeclaration.getMethodDeclarations().get(i);

            IMethod m                     = this.context.toIMethod(md);
            this.checkInheritedMethods(m, m.getDeclaringIClass(), md);
            boolean overrides             = !m.isStatic() && this.overridesMethodFromSupertype(m, m.getDeclaringIClass());
            boolean hasOverrideAnnotation = this.hasAnnotation(md, this.iClassLoader.TYPE_java_lang_Override);
            if (overrides && !hasOverrideAnnotation && !(typeDeclaration instanceof InterfaceDeclaration)) {
                this.context.warning("MO", "Missing @Override", md.getLocation());
            } else
            if (!overrides && hasOverrideAnnotation) {
                this.context.compileError("Method does not override a method declared in a supertype", md.getLocation());
            }

            this.compile(md, cf);
        }
    }

    private boolean
    hasAnnotation(FunctionDeclarator fd, IClass annotationType) throws CompileException {
        for (Annotation a : Iterables.filterByClass(fd.getModifiers(), Annotation.class)) {
            if (this.context.getType(a.getType()) == annotationType) return true;
        }
        return false;
    }

    private boolean
    overridesMethodFromSupertype(IMethod m, IClass type) throws CompileException {

        // Check whether it overrides a method declared in the superclass (or any of its supertypes).
        {
            IClass superclass = type.getSuperclass();
            if (superclass != null && this.overridesMethod(m, superclass)) return true;
        }

        // Check whether it overrides a method declared in an interface (or any of its superinterfaces).
        IClass[] ifs = type.getInterfaces();
        for (IClass i : ifs) {
            if (this.overridesMethod(m, i)) return true;
        }

        // Special handling for interfaces that don't extend other interfaces: JLS7 dictates that these stem from
        // 'Object', but 'getSuperclass()' returns NULL for interfaces.
        if (ifs.length == 0 && type.isInterface()) {
            return this.overridesMethod(m, this.iClassLoader.TYPE_java_lang_Object);
        }

        return false;
    }

    /**
     * @return Whether <var>method</var> overrides a method of <var>type</var> or any of its supertypes
     */
    private boolean
    overridesMethod(IMethod method, IClass type) throws CompileException {

        // Check whether it overrides a method declared in THIS type.
        IMethod[] ms = type.getDeclaredIMethods(method.getName());
        for (IMethod m : ms) {
            if (!this.isInherited(m, method.getDeclaringIClass()) || m.isStatic()) continue;
            if (Arrays.equals(method.getParameterTypes(), m.getParameterTypes())) return true;
            if (this.context.findGenericOverride(method.getDeclaringIClass(), m) == method) return true;
        }

        // Check whether it overrides a method declared in a supertype.
        return this.overridesMethodFromSupertype(method, type);
    }

    private boolean isInherited(IMethod method, IClass child) {
        return method.getAccess() != Access.PRIVATE
            && (!method.getDeclaringIClass().isInterface() || !method.isStatic())
            && (method.getAccess() != Access.DEFAULT
                || String.valueOf(Descriptor.getPackageName(method.getDeclaringIClass().getDescriptor())).equals(
                    String.valueOf(Descriptor.getPackageName(child.getDescriptor()))));
    }

    private static int accessLevel(Access access) {
        return access == Access.PUBLIC ? 3 : access == Access.PROTECTED ? 2 : access == Access.DEFAULT ? 1 : 0;
    }

    private void checkInheritedMethods(IMethod method, IClass type, MethodDeclarator declaration) throws CompileException {
        List<IClass> parents = new ArrayList<>(Arrays.asList(type.getInterfaces()));
        if (type.getSuperclass() != null) parents.add(type.getSuperclass());
        for (IClass parent : parents) {
            for (IMethod base : parent.getDeclaredIMethods(method.getName())) {
                if (!this.isInherited(base, method.getDeclaringIClass())) continue;
                if (!Arrays.equals(method.getParameterTypes(), base.getParameterTypes())
                    && this.context.findGenericOverride(method.getDeclaringIClass(), base) != method) continue;
                if (base.isFinal()) this.context.compileError("Cannot override final method " + base, declaration.getLocation());
                if (method.isStatic() != base.isStatic()) this.context.compileError(
                    "Static and instance methods cannot override or hide one another", declaration.getLocation());
                if (accessLevel(method.getAccess()) < accessLevel(base.getAccess())) this.context.compileError(
                    "Overriding method cannot reduce access", declaration.getLocation());
                IClass result = method.getReturnType(), inherited = base.getReturnType();
                if (declaration.lambdaSamImplementation ? result != inherited && (result.isPrimitive() || inherited.isPrimitive()
                    || !inherited.isAssignableFrom(result)) : !this.context.compatibleOverrideReturn(method, base)) this.context.compileError(
                        "incompatible overriding return type", declaration.getLocation());
                for (IClass thrown : method.getThrownExceptions()) {
                    if (this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(thrown)
                        || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(thrown)) continue;
                    boolean allowed = false;
                    for (IClass permitted : this.context.inheritedThrownExceptions(base, method.getDeclaringIClass())) if (permitted.isAssignableFrom(thrown)) allowed = true;
                    if (!allowed) this.context.compileError("Overriding method adds checked exception " + thrown, declaration.getLocation());
                }
            }
            this.checkInheritedMethods(method, parent, declaration);
        }
    }

    /**
     * Generates and compiles a bridge method with signature <var>base</var> that delegates to <var>override</var>.
     */
    void
    generateBridgeMethod(ClassFile cf, IClass declaringIClass, IMethod base, IMethod override) throws CompileException {

        if (
            !this.context.compatibleOverrideReturn(override, base, declaringIClass)
            && base.getReturnType() != override.getReturnType()
        ) {
            this.context.compileError(
                "The return type of \""
                + override
                + "\" is incompatible with that of \""
                + base
                + "\""
            );
            return;
        }

        ClassFile.MethodInfo mi = cf.addMethodInfo(
            (short) (Mod.PUBLIC | Mod.SYNTHETIC | Mod.BRIDGE), // accessFlags
            base.getName(),
            base.getDescriptor()
        );

        // Add "Exceptions" attribute (JVMS 4.7.4).
        IClass[] thrownExceptions = base.getThrownExceptions();
        if (thrownExceptions.length > 0) {
            final short eani    = cf.addConstantUtf8Info("Exceptions");
            short[]     tecciis = new short[thrownExceptions.length];
            for (int i = 0; i < thrownExceptions.length; ++i) {
                tecciis[i] = cf.addConstantClassInfo(thrownExceptions[i].getDescriptor());
            }
            mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, tecciis));
        }

        final CodeContext codeContext      = new CodeContext(mi.getClassFile());
        final CodeContext savedCodeContext = this.context.replaceCodeContext(codeContext);
        try {
            codeContext.saveLocalVariables();

            // Allocate all our local variables.

            this.context.allocateLocalVariableSlotAndMarkAsInitialized(override.getDeclaringIClass(), "this");

            IClass[]            paramTypes = base.getParameterTypes();
            IClass[]            overrideTypes = override.getParameterTypes();
            LocalVariableSlot[] locals     = new LocalVariableSlot[paramTypes.length];
            for (int i = 0; i < paramTypes.length; ++i) {
                locals[i] = this.context.allocateLocalVariableSlotAndMarkAsInitialized(paramTypes[i], "param" + i);
            }

            this.bytecodeEmitter.load(Located.NOWHERE, declaringIClass, 0);
            for (int i = 0; i < locals.length; i++) {
                LocalVariableSlot l = locals[i];
                this.bytecodeEmitter.load(Located.NOWHERE, l.getType(), l.getSlotIndex());
                if (paramTypes[i] != overrideTypes[i]) this.bytecodeEmitter.checkcast(Located.NOWHERE, overrideTypes[i]);
            }
            this.bytecodeEmitter.invokeMethod(Located.NOWHERE, override);
            if (base.getReturnType() != IClass.VOID && !base.getReturnType().isAssignableFrom(override.getReturnType())) {
                this.bytecodeEmitter.checkcast(Located.NOWHERE, base.getReturnType());
            }
            if (base.getReturnType() == IClass.VOID) {
                this.bytecodeEmitter.returN(Located.NOWHERE);
            } else {
                this.bytecodeEmitter.xreturn(Located.NOWHERE, base.getReturnType());
            }

        } finally {
            this.context.replaceCodeContext(savedCodeContext);
        }

        // Generate Code attribute and add to the MethodInfo.
        mi.addAttribute(codeContext.newCodeAttribute(
            1 + override.getParameterTypes().length, // initialLocalsCount
            false,                                   // debugLines
            false                                    // debugVars
        ));
    }

    void
    compile(FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        try {
            this.compile2(fd, classFile);
            if (fd instanceof MethodDeclarator && fd.getAccess() == Access.PRIVATE
                && !((MethodDeclarator) fd).isStatic() && !(fd.getDeclaringType() instanceof InterfaceDeclaration)) {
                this.generatePrivateAccessor(classFile, this.context.toIMethod((MethodDeclarator) fd));
            }
        } catch (ClassFileException cfe) {
            throw new ClassFileException("Compiling \"" + fd + "\": " + cfe.getMessage(), cfe);
        } catch (RuntimeException re) {
            throw new InternalCompilerException(fd.getLocation(), "Compiling \"" + fd + "\"", re);
        }
    }

    /** A static nest accessor preserves private dispatch without renaming the original method. */
    private void generatePrivateAccessor(ClassFile cf, IMethod method) throws CompileException {
        IClass owner = method.getDeclaringIClass();
        ClassFile.MethodInfo mi = cf.addMethodInfo((short) (Mod.STATIC | Mod.SYNTHETIC), method.getName() + '$',
            method.getDescriptor().prependParameter(owner.getDescriptor()));
        CodeContext code = new CodeContext(cf);
        CodeContext previous = this.context.replaceCodeContext(code);
        IClass[] parameters = method.getParameterTypes();
        try {
            code.saveLocalVariables();
            this.context.allocateLocalVariableSlotAndMarkAsInitialized(owner, "receiver");
            LocalVariableSlot[] locals = new LocalVariableSlot[parameters.length];
            for (int i = 0; i < parameters.length; i++) locals[i] = this.context.allocateLocalVariableSlotAndMarkAsInitialized(parameters[i], "arg" + i);
            this.bytecodeEmitter.load(Located.NOWHERE, owner, 0);
            for (LocalVariableSlot local : locals) this.bytecodeEmitter.load(Located.NOWHERE, local.getType(), local.getSlotIndex());
            this.bytecodeEmitter.invoke(Located.NOWHERE, Opcode.INVOKESPECIAL, owner, method.getName(), method.getDescriptor(), false);
            if (method.getReturnType() == IClass.VOID) this.bytecodeEmitter.returN(Located.NOWHERE);
            else this.bytecodeEmitter.xreturn(Located.NOWHERE, method.getReturnType());
        } finally { this.context.replaceCodeContext(previous); }
        mi.addAttribute(code.newCodeAttribute(1 + parameters.length, false, false));
    }

    private void addMethodParameters(FunctionDeclarator fd, ClassFile.MethodInfo mi, ClassFile cf) throws CompileException {
        int count = new MethodDescriptor(mi.getDescriptor()).parameterFds.length;
        if (count == 0) return;
        final short[] names = new short[count], flags = new short[count];
        FormalParameter[] declared = fd.formalParameters.parameters;
        int offset = count - declared.length;
        for (int i = 0; i < offset; i++) flags[i] = Mod.SYNTHETIC;
        if (fd instanceof ConstructorDeclarator) {
            ConstructorDeclarator constructor = (ConstructorDeclarator) fd;
            TypeDeclaration owner = fd.getDeclaringType();
            int index = 0;
            if (owner instanceof EnumDeclaration || owner instanceof AnonymousClassDeclaration
                && ((AnonymousClassDeclaration) owner).enumConstantBody) {
                names[index++] = cf.addConstantUtf8Info("$enum$name");
                names[index++] = cf.addConstantUtf8Info("$enum$ordinal");
            }
            IClass outer = this.context.resolve(owner).getOuterIClass();
            if (outer != null && index < offset) {
                names[index] = cf.addConstantUtf8Info("this$0");
                flags[index++] = (short) (Mod.FINAL | 0x8000); // mandated enclosing instance
            }
            for (IField field : constructor.getDeclaringClass().syntheticFields.values()) {
                if (index >= offset) break;
                if (!field.getName().startsWith("val$")) continue;
                names[index] = cf.addConstantUtf8Info(field.getName());
                flags[index++] = (short) (Mod.FINAL | Mod.SYNTHETIC);
            }
        }
        for (int i = 0; i < declared.length; i++) {
            names[offset + i] = cf.addConstantUtf8Info(declared[i].name);
            flags[offset + i] = declared[i].isFinal() ? Mod.FINAL : 0;
        }
        mi.addAttribute(new ClassFile.AttributeInfo(cf.addConstantUtf8Info("MethodParameters")) {
            @Override protected void storeBody(java.io.DataOutputStream out) throws java.io.IOException {
                out.writeByte(names.length);
                for (int i = 0; i < names.length; i++) { out.writeShort(names[i]); out.writeShort(flags[i]); }
            }
        });
    }

    private void
    compile2(FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        ClassFile.MethodInfo mi;

        if (this.context.getTargetVersion() < 8 && fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isDefault()) {
            this.context.compileError((
                ""
                + "Default interface methods only available for target version 8+. "
                + "Either use \"setTargetVersion(8)\", or \"-DdefaultTargetVersion=8\"."
            ), fd.getLocation());
        }

        if (fd.getAccess() == Access.PRIVATE) {
            if (
                fd instanceof MethodDeclarator
                && !((MethodDeclarator) fd).isStatic()
                && !(fd.getDeclaringType() instanceof InterfaceDeclaration)
            ) {

                // Preserve the original method, including its native ABI and reflective signature.
                short accessFlags = this.context.accessFlags(fd.getModifiers());
                if (fd.formalParameters.variableArity) accessFlags |= Mod.VARARGS;
                mi = UnitCompilerMethodCompiler.addMethodInfo(fd, classFile, accessFlags, fd.name,
                    this.context.toIMethod((MethodDeclarator) fd).getDescriptor());
            } else
            {

                // TODO: Compile annotations on functions.
//                assert fd.modifiers.annotations.length == 0 : "NYI";

                short accessFlags = this.context.accessFlags(fd.getModifiers());

                // To make the static private class method or private constructor invocable for enclosing types,
                // enclosed types and types enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                if (!(fd.getDeclaringType() instanceof InterfaceDeclaration)) {
                    accessFlags = UnitCompilerMethodCompiler.this.context.changeAccessibility(accessFlags, Mod.PACKAGE);
                }

                if (fd.formalParameters.variableArity) accessFlags |= Mod.VARARGS;

                mi = UnitCompilerMethodCompiler.addMethodInfo(
                    fd,                                   // locatable
                    classFile,                            // classFile
                    accessFlags,                          // accessFlags
                    fd.name,                              // methodName
                    this.context.toIInvocable(fd).getDescriptor() // methodMD
                );
            }
        } else {

            // Non-PRIVATE function.

            short accessFlags = this.context.accessFlags(fd.getModifiers());

            if (fd.formalParameters.variableArity) accessFlags |= Mod.VARARGS;

            if ("<clinit>".equals(fd.name)) {
                accessFlags = Mod.STATIC;
            } else
            if (fd.getDeclaringType() instanceof InterfaceDeclaration) {

                accessFlags |= Mod.PUBLIC;

                if (Mod.isStatic(accessFlags)) {
                    if (this.context.getTargetVersion() < 8) {
                        this.context.compileError("Static interface methods only available for target version 8+", fd.getLocation());
                    }
                } else
                if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isDefault()) {
                    if (this.context.getTargetVersion() < 8) {
                        this.context.compileError("Default methods only available for target version 8+", fd.getLocation());
                    }
                } else
                {
                    accessFlags |= Mod.ABSTRACT;
                }
            }

            mi = UnitCompilerMethodCompiler.addMethodInfo(
                fd,                                   // locatable
                classFile,                            // classFile
                accessFlags,                          // accessFlags
                fd.name,                              // methodName
                this.context.toIInvocable(fd).getDescriptor() // methodMD
            );
        }

        // The original descriptor now always matches the source declaration.
        if (this.context.parameters() && this.context.getTargetVersion() >= 8 && !"<clinit>".equals(fd.name)) {
            this.addMethodParameters(fd, mi, classFile);
        }
        String methodSignature = this.signatureEncoder.toMethodSignature(fd);
        if (methodSignature != null) {
            mi.addAttribute(new ClassFile.SignatureAttribute(classFile.addConstantUtf8Info("Signature"), classFile.addConstantUtf8Info(methodSignature)));
        }

        // Add method annotations with retention != SOURCE.
        this.annotationCompiler.compileDeclarationAnnotations(fd.getAnnotations(), fd instanceof ConstructorDeclarator ? "CONSTRUCTOR" : "METHOD", mi, classFile);
        this.annotationCompiler.compileParameterAnnotations(fd, mi, classFile);

        if (fd.formalParameters.receiverParameter != null) {
                FormalParameter receiver = fd.formalParameters.receiverParameter;
                if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isStatic()) this.context.compileError(
                    "Static methods cannot declare a receiver", receiver.getLocation());
                IClass expected = this.context.resolve(fd.getDeclaringType());
                if (fd instanceof ConstructorDeclarator) expected = expected.getOuterIClass();
                if (expected == null || this.context.getRawType(receiver.type) != expected
                    || fd instanceof MethodDeclarator && !receiver.name.equals("this")
                    || fd instanceof ConstructorDeclarator && receiver.name.equals("this")) this.context.compileError(
                        "Receiver parameter must denote the enclosing instance", receiver.getLocation());
        }

        // Add the TYPE annotations (JSR 308) of the return type, the parameter types and the "throws" clause.
        if (this.context.getTargetVersion() >= 8) {

            UnitCompilerAnnotationCompiler.TypeAnnotationConsumer tac = UnitCompilerAnnotationCompiler.typeAnnotationsOf(mi);
            this.annotationCompiler.compileTypeParameters(fd.getOptionalTypeParameters(), fd, true, tac, classFile);

            if (fd.formalParameters.receiverParameter != null) {
                FormalParameter receiver = fd.formalParameters.receiverParameter;
                this.annotationCompiler.compileTypeAnnotations(receiver.type, (byte) 0x15, -1, tac, classFile);
                List<Annotation> annotations = new ArrayList<>();
                for (Modifier modifier : receiver.modifiers) if (modifier instanceof Annotation) annotations.add((Annotation) modifier);
                this.annotationCompiler.compileDeclarationPositionTypeAnnotations(annotations.toArray(new Annotation[annotations.size()]),
                    receiver.type, (byte) 0x15, -1, tac, classFile);
            }

            if (fd instanceof MethodDeclarator) {
                this.annotationCompiler.compileTypeAnnotations(
                    fd.type,                                   // type
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_METHOD_RETURN,    // targetType
                    -1,                                        // targetIndex
                    tac,                                       // tac
                    classFile                                  // cf
                );

                // Plus the declaration-position annotations with TYPE_USE target.
                this.annotationCompiler.compileDeclarationPositionTypeAnnotations(
                    ((Java.Annotatable) fd).getAnnotations(), // annotations (cast disambiguates for SELF-compilation)
                    fd.type,                                   // declaredType
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_METHOD_RETURN,    // targetType
                    -1,                                        // targetIndex
                    tac,                                       // tac
                    classFile                                  // cf
                );
            } else {
                this.annotationCompiler.compileDeclarationPositionTypeAnnotations(fd.getAnnotations(),
                    new Java.SimpleType(fd.getLocation(), this.context.resolve(fd.getDeclaringType())),
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_METHOD_RETURN, -1, tac, classFile);
            }

            {
                FormalParameter[] fps = fd.formalParameters.parameters;
                int parameterOffset = new org.codehaus.janino.MethodDescriptor(mi.getDescriptor()).parameterFds.length - fps.length;
                for (int i = 0; i < fps.length; i++) {
                    Type parameterType = fd.formalParameters.variableArity && i == fps.length - 1
                        ? new Java.ArrayType(fps[i].type, fps[i].variableArityAnnotations) : fps[i].type;
                    this.annotationCompiler.compileTypeAnnotations(
                        parameterType,                                  // type
                        UnitCompilerAnnotationCompiler.TARGET_TYPE_METHOD_FORMAL_PARAMETER, // targetType
                        i + parameterOffset,                          // targetIndex
                        tac,                                          // tac
                        classFile                                     // cf
                    );

                    // Plus the declaration-position annotations with TYPE_USE target.
                    List<Annotation> pms = new ArrayList<>();
                    for (Modifier m : fps[i].modifiers) {
                        if (m instanceof Annotation) pms.add((Annotation) m);
                    }
                    this.annotationCompiler.compileDeclarationPositionTypeAnnotations(
                        (Annotation[]) pms.toArray(new Annotation[pms.size()]),  // annotations
                        parameterType,                                  // declaredType
                        UnitCompilerAnnotationCompiler.TARGET_TYPE_METHOD_FORMAL_PARAMETER, // targetType
                        i + parameterOffset,                          // targetIndex
                        tac,                                          // tac
                        classFile                                     // cf
                    );
                }
            }

            for (int i = 0; i < fd.thrownExceptions.length; i++) {
                this.annotationCompiler.compileTypeAnnotations(
                    fd.thrownExceptions[i],           // type
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_THROWS,  // targetType
                    i,                                // targetIndex
                    tac,                              // tac
                    classFile                         // cf
                );
            }

            this.annotationCompiler.flushTypeAnnotations(tac, classFile);
        }

        // Add "Exceptions" attribute (JVMS 4.7.4).
        {
            if (fd.thrownExceptions.length > 0 && !(fd instanceof ConstructorDeclarator
                && fd.getDeclaringType() instanceof AnonymousClassDeclaration)) {
                final short eani    = classFile.addConstantUtf8Info("Exceptions");
                List<Short> tecciis = new ArrayList<>(); // new short[fd.thrownExceptions.length];
                for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                    final Type te = fd.thrownExceptions[i];
                    tecciis.add(classFile.addConstantClassInfo(this.context.getRawType(te).getDescriptor()));
                }

                short[] sa = new short[tecciis.size()];
                for (int i = 0; i < tecciis.size(); i++) sa[i] = (Short) tecciis.get(i);

                mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, sa));
            }
        }

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (fd.hasDeprecatedDocTag()) {
            mi.addAttribute(new ClassFile.DeprecatedAttribute(classFile.addConstantUtf8Info("Deprecated")));
        }

        // Add "AnnotationDefault" attribute (JVMS8 4.7.22)
        if (fd instanceof MethodDeclarator) {
            ElementValue defaultValue = ((MethodDeclarator) fd).defaultValue;
            if (defaultValue != null) {
                mi.addAttribute(
                    new ClassFile.AnnotationDefaultAttribute(
                        classFile.addConstantUtf8Info("AnnotationDefault"),
                        UnitCompilerMethodCompiler.this.annotationCompiler.compileElementValue(defaultValue, classFile, fd.type instanceof ArrayType)
                    )
                );
            }
        }

        if (fd.getDeclaringType() instanceof InterfaceDeclaration) {
            MethodDeclarator md = (MethodDeclarator) fd;

            if (md.getAccess() == Access.PRIVATE && this.context.getTargetVersion() < 9) {
                this.context.compileError("Private interface methods only available for target version 9+", fd.getLocation());
                return;
            }

            if (md.isStrictfp() && !md.isDefault() && !md.isStatic()) {
                this.context.compileError(
                    "Modifier strictfp only allowed for interface default methods and static interface methods",
                    fd.getLocation()
                );
                return;
            }
        }

        if (
            (
                fd.getDeclaringType() instanceof InterfaceDeclaration
                && !((MethodDeclarator) fd).isStatic()
                && ((MethodDeclarator) fd).getAccess() != Access.PRIVATE
            )
            || (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isAbstract())
            || (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isNative())
        ) {
            if (((MethodDeclarator) fd).isDefault()) {
                if (!(fd.getDeclaringType() instanceof InterfaceDeclaration)) {
                    this.context.compileError("Only interface method declarations may have the \"default\" modifier", fd.getLocation());
                } else
                if (((MethodDeclarator) fd).isStatic()) {
                    this.context.compileError("Static interface method declarations must not have the \"default\" modifier", fd.getLocation());
                } else
                if (fd.statements == null) {
                    this.context.compileError("Default method declarations must have a body", fd.getLocation());
                }
            } else {
                if (fd.statements != null) this.context.compileError("Method must not declare a body", fd.getLocation());
                return;
            }
        }

        // Create CodeContext.
        final CodeContext codeContext = new CodeContext(mi.getClassFile());

        CodeContext savedCodeContext = this.context.replaceCodeContext(codeContext);
        try {
            this.context.getCodeContext().saveLocalVariables();

            if (fd instanceof MethodDeclarator) {
                MethodDeclarator md = (MethodDeclarator) fd;
                if (!md.isStatic()) {

                    // Define special parameter "this".
                    this.context.allocateLocalVariableSlotAndMarkAsInitialized(this.context.resolve(fd.getDeclaringType()), "this");
                }
            }

            if (fd instanceof ConstructorDeclarator) {

                // Define special parameter "unititialized this".
                {
                    IClass rawThisType = this.context.resolve(fd.getDeclaringType());
                    LocalVariableSlot result = this.context.getCodeContext().allocateLocalVariable(
                        (short) 1,
                        "this",
                        rawThisType
                    );
                    this.bytecodeEmitter.updateLocalVariableInCurrentStackMap(
                        result.getSlotIndex(),
                        StackMapTableAttribute.UNINITIALIZED_THIS_VARIABLE_INFO
                    );
                }

                ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) fd;

                if (
                    fd.getDeclaringType() instanceof EnumDeclaration
                    || fd.getDeclaringType() instanceof AnonymousClassDeclaration
                    && ((AnonymousClassDeclaration) fd.getDeclaringType()).enumConstantBody
                ) {

                    // Define special constructor parameters "String $name" and "int $ordinal" for enums.
                    LocalVariable lv1 = this.context.allocateLocalVariableAndMarkAsInitialized(true /*finaL*/, this.iClassLoader.TYPE_java_lang_String);
                    constructorDeclarator.syntheticParameters.put("$name", lv1);

                    LocalVariable lv2 = this.context.allocateLocalVariableAndMarkAsInitialized(true /*finaL*/, IClass.INT);
                    constructorDeclarator.syntheticParameters.put("$ordinal", lv2);
                }

                // Define synthetic parameters for inner classes ("this$...", "val$...").
                for (IField sf : constructorDeclarator.getDeclaringClass().syntheticFields.values()) {

                    LocalVariable lv = this.context.allocateLocalVariableAndMarkAsInitialized(true /*finaL*/, this.context.capturedFieldType(sf));
                    constructorDeclarator.syntheticParameters.put(sf.getName(), lv);
                }
            }

            this.buildLocalVariableMap(fd);
            this.statementCompiler.analyzeFinalLocals(fd);
            this.context.getCodeContext().newOffset();

            // Compile the constructor preamble.
            if (fd instanceof ConstructorDeclarator) {
                ConstructorDeclarator cd = (ConstructorDeclarator) fd;
                ConstructorInvocation ci = cd.constructorInvocation;
                if (ci != null) {
                    if (ci instanceof SuperConstructorInvocation) {
                        this.assignSyntheticParametersToSyntheticFields(cd);
                    }
                    this.statementCompiler.compile(ci);

                    // Object initialization is complete; change the verification type info of "this" from
                    // "uninitializedThis" to "object".
                    this.bytecodeEmitter.updateLocalVariableInCurrentStackMap((short) 0, this.bytecodeEmitter.verificationTypeInfo(this.context.resolve(fd.getDeclaringType())));

                    if (ci instanceof SuperConstructorInvocation) {
                        this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                    }
                } else {

                    // Determine qualification for superconstructor invocation.
                    IClass superclass = this.context.resolve(cd.getDeclaringClass()).getSuperclass();
                    if (superclass == null && !Descriptor.JAVA_LANG_OBJECT.equals(this.context.resolve(cd.getDeclaringClass()).getDescriptor())) {
                        throw new CompileException("\"" + cd + "\" has no superclass", cd.getLocation());
                    }

                    if (superclass != null) {
                        IClass                 outerClassOfSuperclass = superclass.getOuterIClass();
                        QualifiedThisReference qualification          = null;
                        if (outerClassOfSuperclass != null) {
                            qualification = new QualifiedThisReference(
                                cd.getLocation(),                                        // location
                                new SimpleType(cd.getLocation(), outerClassOfSuperclass) // qualification
                            );
                        }

                        // Initialize "this.this$0" and friends.
                        this.assignSyntheticParametersToSyntheticFields(cd);

                        // Invoke the superconstructor.
                        Rvalue[] arguments;
                        if (fd.getDeclaringType() instanceof EnumDeclaration) {

                            LocalVariableAccess nameAccess = new LocalVariableAccess(
                                cd.getLocation(),
                                (LocalVariable) cd.syntheticParameters.get("$name")
                            );
                            assert nameAccess != null;

                            LocalVariableAccess ordinalAccess = new LocalVariableAccess(
                                cd.getLocation(),
                                (LocalVariable) cd.syntheticParameters.get("$ordinal")
                            );
                            assert ordinalAccess != null;

                            arguments = new Rvalue[] { nameAccess, ordinalAccess };
                        } else {
                            arguments = new Rvalue[0];
                        }
                        SuperConstructorInvocation sci = new SuperConstructorInvocation(
                            cd.getLocation(),  // location
                            qualification,     // qualification
                            arguments          // arguments
                        );
                        sci.setEnclosingScope(fd);
                        this.statementCompiler.compile(sci);
                    }

                    // Object initialization is complete; change the verification type info of "this" from
                    // "uninitializedThis" to "object".
                    this.bytecodeEmitter.updateLocalVariableInCurrentStackMap((short) 0, this.bytecodeEmitter.verificationTypeInfo(this.context.resolve(fd.getDeclaringType())));

                    // Initialize "this.x = y".
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                }
            }

            // Compile the function body.
            List<? extends BlockStatement> oss = fd.statements;
            if (oss == null) {
                this.context.compileError("Method must have a body", fd.getLocation());
                return;
            }
            if (this.statementCompiler.compileStatements(oss)) {
                if (this.context.getReturnType(fd) != IClass.VOID) {
                    this.context.compileError("Method must return a value", fd.getLocation());
                }
                this.bytecodeEmitter.returN(fd);
            }
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
            this.context.replaceCodeContext(savedCodeContext);
        }

        // Don't continue code attribute generation if we had compile errors.
        if (this.context.compileErrorCount() > 0) return;

        if (fd instanceof ConstructorDeclarator && fd.getDeclaringType() instanceof AnonymousClassDeclaration) {
            java.util.Set<IClass> exceptions = new java.util.LinkedHashSet<>();
            for (Type type : fd.thrownExceptions) exceptions.add(this.context.getRawType(type));
            exceptions.addAll(Arrays.asList(this.context.anonymousInitializerExceptions((AnonymousClassDeclaration) fd.getDeclaringType())));
            if (!exceptions.isEmpty()) {
                short[] indices = new short[exceptions.size()]; int index = 0;
                for (IClass type : exceptions) indices[index++] = classFile.addConstantClassInfo(type.getDescriptor());
                mi.addAttribute(new ClassFile.ExceptionsAttribute(classFile.addConstantUtf8Info("Exceptions"), indices));
            }
        }

        // Fix up and reallocate as needed.
        codeContext.fixUpAndRelocate();
        if (this.context.debugVars()) {
            UnitCompilerMethodCompiler.makeLocalVariableNames(codeContext, mi);
        }

        // Add the code context as a code attribute to the MethodInfo.
        boolean hasThis = (Boolean) fd.accept(new FunctionDeclaratorVisitor<Boolean, RuntimeException>() {

            @Override @Nullable public Boolean
            visitConstructorDeclarator(ConstructorDeclarator cd) { return true; }

            @Override @Nullable public Boolean
            visitMethodDeclarator(MethodDeclarator md) { return !md.isStatic(); }
        });

        try {
            mi.addAttribute(codeContext.newCodeAttribute((
                (hasThis ? 1 : 0)
                + (fd instanceof ConstructorDeclarator ? ((ConstructorDeclarator) fd).syntheticParameters.size() : 0)
                + fd.formalParameters.parameters.length
            ), this.context.debugLines(), this.context.debugVars()));
        } catch (Error e) {
            throw new InternalCompilerException(fd.getLocation(), null, e);
        }
    }

    private static MethodInfo
    addMethodInfo(
        Locatable        locatable,
        ClassFile        classFile,
        short            accessFlags,
        String           methodName,
        MethodDescriptor methodMd
    ) throws CompileException {

        MethodInfo mi = classFile.addMethodInfo(accessFlags, methodName, methodMd);

        for (MethodInfo mi2 : classFile.methodInfos) {
            if (
                mi2 != mi
                && mi.getName().equals(mi2.getName())
                && mi.getDescriptor().contentEquals(mi2.getDescriptor())
            ) {
                throw new CompileException("Redeclaration of \"" + locatable + "\" with same signature", locatable.getLocation());
            }
        }

        return mi;
    }

    /**
     * Makes the variable name and class name Constant Pool names used by local variables.
     */
    private static void
    makeLocalVariableNames(final CodeContext cc, final ClassFile.MethodInfo mi) {
        ClassFile cf = mi.getClassFile();

        cf.addConstantUtf8Info("LocalVariableTable");
        for (LocalVariableSlot slot : cc.getAllLocalVars()) {

            String localVariableName = slot.getName();
            if (localVariableName != null) {
                String typeName = UnitCompiler.rawTypeOf(slot.getType()).getDescriptor();

                cf.addConstantUtf8Info(typeName);
                cf.addConstantUtf8Info(localVariableName);
            }
        }
    }

    private void
    buildLocalVariableMap(FunctionDeclarator fd) throws CompileException {
        Map<String, LocalVariable> localVars = new HashMap<>();

        // Add function parameters.
        for (int i = 0; i < fd.formalParameters.parameters.length; ++i) {
            FormalParameter fp = fd.formalParameters.parameters[i];
            LocalVariable   lv = this.context.getLocalVariable(
                fp,
                i == fd.formalParameters.parameters.length - 1 && fd.formalParameters.variableArity
            );
            lv.setSlot(this.context.allocateLocalVariableSlotAndMarkAsInitialized(lv.type, fp.name));

            if (localVars.put(fp.name, lv) != null) {
                this.context.compileError("Redefinition of parameter \"" + fp.name + "\"", fd.getLocation());
            }
        }

        fd.localVariables = localVars;
        if (fd instanceof ConstructorDeclarator) {
            ConstructorDeclarator cd = (ConstructorDeclarator) fd;
            ConstructorInvocation ci = cd.constructorInvocation;
            if (ci != null) {
                UnitCompilerStatementCompiler.buildLocalVariableMap(ci, localVars);
            }
        }
        if (fd.statements != null) {
            for (BlockStatement bs : fd.statements) localVars = this.statementCompiler.buildLocalVariableMap(bs, localVars);
        }
    }

    /**
     * Copies the values of the synthetic parameters of this constructor ("this$..." and "val$...") to the synthetic
     * fields of the object ("this$..." and "val$...").
     */
    void
    assignSyntheticParametersToSyntheticFields(ConstructorDeclarator cd) throws CompileException {
        for (IClass.IField sf : cd.getDeclaringClass().syntheticFields.values()) {
            LocalVariable syntheticParameter = (LocalVariable) cd.syntheticParameters.get(sf.getName());
            if (syntheticParameter == null) {
                throw new InternalCompilerException(cd.getLocation(), (
                    "SNO: Synthetic parameter for synthetic field \""
                    + sf.getName()
                    + "\" not found"
                ));
            }
            ExpressionStatement es = new ExpressionStatement(new Assignment(
                cd.getLocation(),                    // location
                new FieldAccess(                     // lhs
                    cd.getLocation(),                    // location
                    new ThisReference(cd.getLocation()), // lhs
                    sf                                   // field
                ),
                "=",                                 // operator
                new LocalVariableAccess(             // rhs
                    cd.getLocation(),                    // location
                    syntheticParameter                   // localVariable
                )
            ));
            es.setEnclosingScope(cd);
            this.statementCompiler.compile(es);
        }
    }

    /**
     * Compiles the instance variable initializers and the instance initializers in their lexical order.
     */
    void
    initializeInstanceVariablesAndInvokeInstanceInitializers(ConstructorDeclarator cd) throws CompileException {

        // Compilation of block statements can create synthetic variables, so we must not use an iterator.
        List<FieldDeclarationOrInitializer> vdais = cd.getDeclaringClass().fieldDeclarationsAndInitializers;
        for (int i = 0; i < vdais.size(); i++) {
            BlockStatement bs = (BlockStatement) vdais.get(i);

            if (bs instanceof Initializer      && ((Initializer)      bs).isStatic()) continue;
            if (bs instanceof FieldDeclaration && ((FieldDeclaration) bs).isStatic()) continue;

            if (!this.statementCompiler.compile(bs)) {
                this.context.compileError(
                    "Instance variable declarator or instance initializer does not complete normally",
                    bs.getLocation()
                );
            }
        }
    }
}
