
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.iterator.Iterables;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AnnotationTypeDeclaration;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.DocCommentable;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FieldDeclarationOrInitializer;
import org.codehaus.janino.Java.FieldDeclarationOrInitializer;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.InnerClassDeclaration;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NamedTypeDeclaration;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Visitor.TypeDeclarationVisitor;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.DeepCopier;

/** Compiles class, interface, enum and annotation declarations and their fields and member types. */
final class UnitCompilerTypeDeclarationCompiler {

    /** Semantic operations and current compilation settings supplied by the compilation unit. */
    interface Context {
        void
        addClassFile(ClassFile cf);
        CodeContext
        getCodeContext();
        LocalVariable
        getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException;
        int
        getTargetVersion();
        LocalVariableSlot
        allocateLocalVariableSlotAndMarkAsInitialized(IType localVariableType, @Nullable String localVariableName);
        @Nullable IMethod
        findGenericOverride(IClass type, IMethod base) throws CompileException;
        boolean compatibleOverrideReturn(IMethod method, IMethod base, IClass type) throws CompileException;
        IClass[]
        rawTypesOf(IType[] iTypes);
        IClass
        getRawType(Type t) throws CompileException;
        IType[]
        getTypes(Type[] types) throws CompileException;
        IClass
        resolve(TypeDeclaration td);
        List<TypeDeclaration>
        getOuterClasses(TypeDeclaration inner);
        @Nullable String[]
        getSingleTypeImport(String name, Location location) throws CompileException;
        @Nullable Object
        constantAssignmentConversion(Locatable locatable, @Nullable Object value, IType targetType) throws CompileException;
        void
        compileError(String message, @Nullable Location location) throws CompileException;
        @Nullable CodeContext
        replaceCodeContext(@Nullable CodeContext newCodeContext);
        short
        accessFlags(Modifier[] modifiers) throws CompileException;
        Modifier[]
        accessModifiers(Location location, String... keywords);
        short
        changeAccessibility(short accessFlags, short newAccessibility);
        String functionDescriptor(FunctionDeclarator function) throws CompileException;
        boolean debugSource();
        IField newSyntheticField(IClass declaringClass, String name, IClass type);
    }

    private final Context context;
    private final IClassLoader iClassLoader;
    private final UnitCompilerSignatureEncoder signatureEncoder;
    private final UnitCompilerAnnotationCompiler annotationCompiler;
    private final UnitCompilerConstantEvaluator constantEvaluator;
    private final UnitCompilerStatementCompiler statementCompiler;
    private final UnitCompilerMethodCompiler methodCompiler;

    UnitCompilerTypeDeclarationCompiler(
        Context context,
        IClassLoader iClassLoader,
        UnitCompilerSignatureEncoder signatureEncoder,
        UnitCompilerAnnotationCompiler annotationCompiler,
        UnitCompilerConstantEvaluator constantEvaluator,
        UnitCompilerStatementCompiler statementCompiler,
        UnitCompilerMethodCompiler methodCompiler
    ) {
        this.context = context;
        this.iClassLoader = iClassLoader;
        this.signatureEncoder = signatureEncoder;
        this.annotationCompiler = annotationCompiler;
        this.constantEvaluator = constantEvaluator;
        this.statementCompiler = statementCompiler;
        this.methodCompiler = methodCompiler;
    }

    void
    compile(TypeDeclaration td) throws CompileException {
        this.prepareAssertions(td);
        td.accept(new TypeDeclarationVisitor<Void, CompileException>() {

            @Override @Nullable public Void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(acd);                           return null; }
            @Override @Nullable public Void visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(lcd);                           return null; }
            @Override @Nullable public Void visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)                    throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(pmcd);                          return null; }
            @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(mid);                           return null; }
            @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(pmid);                          return null; }
            @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2((InnerClassDeclaration) mcd);   return null; }
            @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2((InnerClassDeclaration) med);   return null; }
            @Override @Nullable public Void visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(pmed);                          return null; }
            @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2(pmatd);                         return null; }

            @Override @Nullable public Void visitEnumConstant(EnumConstant ec)                                         throws CompileException { UnitCompilerTypeDeclarationCompiler.this.context.compileError("Compilation of enum constant NYI",                      ec.getLocation());   return null; }
            @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws CompileException { UnitCompilerTypeDeclarationCompiler.this.compile2((InterfaceDeclaration) matd); return null; }
        });
    }

    /** Assertion status is fixed at initialization, and disabled assertions evaluate neither operand. */
    private void prepareAssertions(final TypeDeclaration declaration) throws CompileException {
        if (!(declaration instanceof Java.AbstractTypeDeclaration)) return;
        final Java.AbstractTypeDeclaration owner = (Java.AbstractTypeDeclaration) declaration;
        if (owner.assertionsDisabledField != null) return;
        final boolean[] required = { false };
        new org.codehaus.janino.util.AbstractTraverser<RuntimeException>() {
            @Override public void traverseBinaryOperation(Java.BinaryOperation operation) {
                this.traverseBinaryOperationIteratively(operation);
            }

            private final java.util.Map<Java.Scope, Java.Scope> lambdaParents = new java.util.IdentityHashMap<>();

            @Override public void traverseLambdaExpression(Java.LambdaExpression expression) {
                // Lambda blocks receive their Statement parent only when moved into a helper.
                // Visit them now without modifying those bindings, before class initialization is emitted.
                if (expression.body instanceof Java.BlockLambdaBody) {
                    Java.Block block = ((Java.BlockLambdaBody) expression.body).block;
                    this.lambdaParents.put(block, expression.getEnclosingScope());
                    this.traverseBlock(block);
                } else this.dispatchRvalue(((Java.ExpressionLambdaBody) expression.body).expression);
            }

            @Override public void traverseAssertStatement(Java.AssertStatement statement) {
                Java.Scope scope = statement.getEnclosingScope();
                while (!(scope instanceof TypeDeclaration)) {
                    scope = this.lambdaParents.containsKey(scope) ? this.lambdaParents.get(scope) : scope.getEnclosingScope();
                }
                if (scope == declaration) required[0] = true;
            }
        }.visitTypeDeclaration(declaration);
        if (!required[0]) return;
        IClass type = this.context.resolve(owner);
        String name = "janino$assertionsDisabled";
        for (int index = 0; type.getDeclaredIField(name) != null; index++) name = "janino$assertionsDisabled$" + index;
        owner.assertionsDisabledField = name;
        TypeDeclaration topLevel = declaration;
        for (Java.Scope scope = declaration.getEnclosingScope(); !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            if (scope instanceof TypeDeclaration) topLevel = (TypeDeclaration) scope;
        }
        Location location = owner.getLocation();
        Rvalue status = new Java.MethodInvocation(location, new Java.ClassLiteral(location,
            new SimpleType(location, this.context.resolve(topLevel))), "desiredAssertionStatus", new Rvalue[0]);
        FieldDeclaration flag = new FieldDeclaration(location, null,
            this.context.accessModifiers(location, "public", "static", "final"), new Java.PrimitiveType(location, Java.Primitive.BOOLEAN),
            new VariableDeclarator[] { new VariableDeclarator(location, name, 0, new Java.UnaryOperation(location, "!", status)) });
        if (owner instanceof AbstractClassDeclaration) {
            AbstractClassDeclaration clazz = (AbstractClassDeclaration) owner;
            clazz.addFieldDeclaration(flag);
            clazz.fieldDeclarationsAndInitializers.remove(flag);
            clazz.fieldDeclarationsAndInitializers.add(0, flag);
        } else if (owner instanceof InterfaceDeclaration) {
            InterfaceDeclaration interfaze = (InterfaceDeclaration) owner;
            interfaze.addConstantDeclaration(flag);
            interfaze.constantDeclarations.remove(flag);
            interfaze.constantDeclarations.add(0, flag);
        }
        type.clearIFieldCaches();
    }

    /**
     * Compiles a top-level class or enum declaration.
     */
    private void
    compile2(PackageMemberClassDeclaration pmcd) throws CompileException {
        this.checkForConflictWithSingleTypeImport(pmcd.getName(), pmcd.getLocation());
        this.checkForNameConflictWithAnotherPackageMemberTypeDeclaration(pmcd);
        this.compile2((NamedClassDeclaration) pmcd);
    }

    /**
     * Compiles a top-level interface or annotation type declaration.
     */
    private void
    compile2(PackageMemberInterfaceDeclaration pmid) throws CompileException {
        this.checkForConflictWithSingleTypeImport(pmid.getName(), pmid.getLocation());
        this.checkForNameConflictWithAnotherPackageMemberTypeDeclaration(pmid);
        this.compile2((InterfaceDeclaration) pmid);
    }

    /**
     * @see JLS8, section 7.6, "Top Level Type Declarations"
     */
    private void
    checkForNameConflictWithAnotherPackageMemberTypeDeclaration(PackageMemberTypeDeclaration pmtd)
    throws CompileException {

        CompilationUnit declaringCompilationUnit = pmtd.getDeclaringCompilationUnit();

        String                       name      = pmtd.getName();
        PackageMemberTypeDeclaration otherPmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(
            name
        );

        if (otherPmtd != null && otherPmtd != pmtd) {
            this.context.compileError(
                "Redeclaration of type \"" + name + "\", previously declared in " + otherPmtd.getLocation()
                ,
                pmtd.getLocation()
            );
        }
    }

    /**
     * @see JLS8, section 7.6, "Top Level Type Declarations"
     */
    private void
    checkForConflictWithSingleTypeImport(String name, Location location) throws CompileException {

        String[] ss = this.context.getSingleTypeImport(name, location);
        if (ss != null) {
            this.context.compileError((
                "Package member type declaration \""
                + name
                + "\" conflicts with single-type-import \""
                + Java.join(ss, ".")
                + "\""
            ), location);
        }
    }

    private void
    compile2(AbstractClassDeclaration cd) throws CompileException {
        IClass iClass = this.context.resolve(cd);
        iClass.getSuperclass();
        iClass.getInterfaces();
        if (cd instanceof NamedClassDeclaration) {
            NamedClassDeclaration named = (NamedClassDeclaration) cd;
            if (named.extendedType != null) this.context.getTypes(new Type[] { named.extendedType });
            this.context.getTypes(named.implementedTypes);
        } else if (cd instanceof AnonymousClassDeclaration) {
            this.context.getTypes(new Type[] { ((AnonymousClassDeclaration) cd).baseType });
        }

        // Check that all methods of the non-abstract class are implemented.
        if (!iClass.isAbstract()) {
            IMethod[] ms = iClass.getIMethods();
            for (IMethod base : ms) {
                if (base.isAbstract()) {
                    if ("<clinit>".equals(base.getName())) continue;
                    IClass superclass = iClass.getSuperclass();
                    // Trust the established interface contract of a concrete superclass, including binaries
                    // compiled against an older version of that interface (javac permits this evolution).
                    if (base.getDeclaringIClass().isInterface() && superclass != null && !superclass.isAbstract()
                        && base.getDeclaringIClass().isAssignableFrom(superclass)) continue;
                    IMethod override = iClass.findIMethod(base.getName(), base.getParameterTypes());
                    if (override == null || override.isAbstract()) override = this.context.findGenericOverride(iClass, base);
                    if (
                        override == null           // It wasn't overridden
                        || override.isAbstract()   // It was overridden with an abstract method
                                                   // The override does not provide a covariant return type
                        || !this.context.compatibleOverrideReturn(override, base, iClass)
                    ) {
                        this.context.compileError(
                            "Non-abstract class \"" + iClass + "\" must implement method \"" + base + "\"",
                            cd.getLocation()
                        );
                    }
                }
            }
        }

        short accessFlags = this.context.accessFlags(cd.getModifiers());
        if (cd.getEnclosingScope() instanceof InterfaceDeclaration) accessFlags |= Mod.PUBLIC | Mod.STATIC;
        if (cd instanceof PackageMemberTypeDeclaration) accessFlags |= Mod.SUPER;
        if (cd instanceof EnumDeclaration) {
            accessFlags |= Mod.ENUM;
            if (iClass.isAbstract()) accessFlags |= Mod.ABSTRACT;
        }
        if (cd instanceof AnonymousClassDeclaration && ((AnonymousClassDeclaration) cd).enumConstantBody) {
            accessFlags |= Mod.ENUM;
        }
        if (cd instanceof MemberEnumDeclaration) accessFlags |= Mod.STATIC;

        // Create "ClassFile" object.
        ClassFile cf = this.newClassFile(accessFlags, iClass, iClass.getSuperclass(), iClass.getInterfaces());

        this.signatureEncoder.addClassSignature(cd, cf);
        if (this.context.getTargetVersion() >= 8) this.annotationCompiler.compileClassTypeAnnotations(cd, cf);

        // Add class annotations with retention != SOURCE.
        this.annotationCompiler.compileDeclarationAnnotations(cd.getAnnotations(), "TYPE", cf, cf);

        if (cd instanceof LocalClassDeclaration || cd instanceof AnonymousClassDeclaration) {

            // Add an "InnerClasses" attribute entry for this anonymous class declaration on the class file (JLS8,
            // section 4.7.6, "The InnerClasses Attribute").
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short innerNameIndex      = (
                cd instanceof Java.NamedTypeDeclaration ? cf.addConstantUtf8Info(((Java.NamedTypeDeclaration) cd).getName()) : (short) 0
            );
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,  // innerClassInfoIndex
                (short) 0,            // outerClassInfoIndex
                innerNameIndex,       // innerNameIndex
                accessFlags           // innerClassAccessFlags
            ));
        } else
        if (cd.getEnclosingScope() instanceof TypeDeclaration) {

            // Add an "InnerClasses" attribute entry for this nested class declaration on the class file (JLS8,
            // section 4.7.6, "The InnerClasses Attribute").
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(
                this.context.resolve(((TypeDeclaration) cd.getEnclosingScope())).getDescriptor()
            );
            short innerNameIndex = cf.addConstantUtf8Info(((MemberTypeDeclaration) cd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,  // innerClassInfoIndex
                outerClassInfoIndex,  // outerClassInfoIndex
                innerNameIndex,       // innerNameIndex
                accessFlags           // innerClassAccessFlags
            ));
        }

        // A local or anonymous declaration is enclosed by a method, constructor or initializer.
        if (!(cd.getEnclosingScope() instanceof TypeDeclaration) && !(cd.getEnclosingScope() instanceof CompilationUnit)) {
            Java.Scope enclosing = cd.getEnclosingScope();
            while (!(enclosing instanceof TypeDeclaration)) {
                if (enclosing instanceof FunctionDeclarator) {
                    FunctionDeclarator function = (FunctionDeclarator) enclosing;
                    String descriptor = this.context.functionDescriptor(function);
                    cf.addEnclosingMethodAttribute(this.context.resolve(function.getDeclaringType()).getDescriptor(), function.name, descriptor);
                    break;
                }
                enclosing = enclosing.getEnclosingScope();
            }
            if (enclosing instanceof TypeDeclaration) cf.addEnclosingMethodAttribute(
                this.context.resolve((TypeDeclaration) enclosing).getDescriptor(), null, null);
        }

        // Set the "SourceFile" attribute (JVMS8, section 4.7.10, "The SourceFile Attribute") on this class file.
        if (this.context.debugSource()) {
            String sourceFileName;
            {
                String s = cd.getLocation().getFileName();
                if (s != null) {
                    sourceFileName = new File(s).getName();
                } else if (cd instanceof NamedTypeDeclaration) {
                    sourceFileName = ((NamedTypeDeclaration) cd).getName() + ".java";
                } else {
                    sourceFileName = "ANONYMOUS.java";
                }
            }
            cf.addSourceFileAttribute(sourceFileName);
        }

        // Add a "Deprecated" attribute (JVMS8, section 4.7.15, "The Deprecated Attribute") on this class file.
        if (cd instanceof DocCommentable && ((DocCommentable) cd).hasDeprecatedDocTag()) {
            cf.addDeprecatedAttribute();
        }

        List<BlockStatement> classInitializationStatements = new ArrayList<>();
        FieldDeclaration enumValuesField = null;

        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed = (EnumDeclaration) cd;

            // Create field and static initializer for each enum constant.
            for (EnumConstant ec : ed.getConstants()) {

                // E <constant> = new E(<ordinal>, <name> [ optional-constructor-args ]);
                VariableDeclarator variableDeclarator = new VariableDeclarator(
                    ec.getLocation(),     // location
                    ec.name,              // name
                    0,                    // brackets
                    this.enumConstantInitializer(ec, iClass) // initializer
                );

                FieldDeclaration fd = new FieldDeclaration(
                    ec.getLocation(),                                                            // location
                    ec.getDocComment(),                                                          // docComment
                    UnitCompilerTypeDeclarationCompiler.this.context.accessModifiers(ec.getLocation(), "public", "static", "final"), // modifiers
                    new SimpleType(ec.getLocation(), iClass),                                    // type
                    new VariableDeclarator[] { variableDeclarator }                              // variableDeclarators
                );
                fd.setDeclaringType(ed);

                classInitializationStatements.add(fd);

                this.addFields(fd, cf);
            }

            // Create the synthetic "ENUM$VALUES" field:
            //
            //     private static final E[] ENUM$VALUES = new E[<number-of-enum-constants>];
            Location         loc        = ed.getLocation();
            IClass           enumIClass = this.context.resolve(ed);
            FieldDeclaration fd         = new FieldDeclaration(
                loc,                                                             // location
                null,                                                            // docComment
                UnitCompilerTypeDeclarationCompiler.this.context.accessModifiers(loc, "private", "static", "final"), // modifiers
                new SimpleType(loc, enumIClass),                                 // type
                new VariableDeclarator[] {                                       // variableDeclarators)
                    new VariableDeclarator(
                        loc,           // location
                        "ENUM$VALUES", // name
                        1,             // brackets
                        new NewArray(  // initializer
                            loc,                             // location
                            new SimpleType(loc, enumIClass), // type
                            new Rvalue[] {                   // dimExprs
                                new IntegerLiteral(loc, String.valueOf(ed.getConstants().size())),
                            },
                            0                                // dims
                        )
                    )
                }
            );
            ((AbstractClassDeclaration) ed).addFieldDeclaration(fd);
            enumValuesField = fd;
            classInitializationStatements.add(fd);
        }

        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed         = (EnumDeclaration) cd;
            IClass          enumIClass = this.context.resolve(ed);

            // Initialize the elements of the synthetic "ENUM$VALUES" field:
            //
            //     E.ENUM$VALUES[0] = E.<first-constant>;
            //     E.ENUM$VALUES[1] = E.<second-constant>;
            //     ...
            int index = 0;
            for (EnumConstant ec : ed.getConstants()) {
                classInitializationStatements.add(
                    new ExpressionStatement(
                        new Assignment(
                            ec.getLocation(),          // location
                            new ArrayAccessExpression( // lhs
                                ec.getLocation(),                                             // location
                                new FieldAccessExpression(                                    // lhs
                                    ec.getLocation(),
                                    new SimpleType(ec.getLocation(), enumIClass),
                                    "ENUM$VALUES"
                                ),
                                new IntegerLiteral(ec.getLocation(), String.valueOf(index++)) // rhs
                            ),
                            "=",                       // operator
                            new FieldAccessExpression( // rhs
                                ec.getLocation(),
                                new SimpleType(ec.getLocation(), enumIClass),
                                ec.name
                            )
                        )
                    )
                );
            }
        }

        // Enum constants and their values array must exist before user code can call values() or valueOf().
        for (BlockStatement fdoi : cd.fieldDeclarationsAndInitializers) {
            if (fdoi == enumValuesField) continue;
            if (
                (fdoi instanceof FieldDeclaration && ((FieldDeclaration) fdoi).isStatic())
                || (fdoi instanceof Initializer && ((Initializer) fdoi).isStatic())
            ) classInitializationStatements.add(fdoi);
        }

        // Create class initialization method.
        this.methodCompiler.maybeCreateInitMethod(cd, cf, classInitializationStatements);

        // Generate and compile the "magic" ENUM methods "E[] values()" and "E valueOf(String)".
        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed = (EnumDeclaration) cd;

            // public static E[] values() {
            //     E[] tmp = new T[<number-of-constants>];
            //     System.arraycopy(T.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
            //     return tmp;
            // }
            Location   loc                   = ed.getLocation();
            int        numberOfEnumConstants = ed.getConstants().size();
            IClass     enumIClass            = this.context.resolve(ed);

            // tmp = new T[<number-of-constants>];
            VariableDeclarator vd = new VariableDeclarator(
                loc,          // location
                "tmp",        // name
                0,            // brackets
                new NewArray( // optionalinitializer
                    loc,                             // location
                    new SimpleType(loc, enumIClass), // type
                    new Rvalue[] {                   // dimExprs
                        new IntegerLiteral(loc, String.valueOf(numberOfEnumConstants))
                    },
                    0                                // dims
                )
            );

            // E[] tmp = new E[<number-of-constants>];
            LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
                loc,
                new Modifier[0],
                new SimpleType(loc, this.iClassLoader.getArrayIClass(enumIClass)),
                new VariableDeclarator[] { vd }
            );

            // public static E[] values() {
            //     E[] tmp = new T[<number-of-constants>];
            //     System.arraycopy(T.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
            //     return tmp;
            // }
            {
                MethodDeclarator md = new MethodDeclarator(
                    loc,                                                   // location
                    null,                                                  // docComment
                    UnitCompilerTypeDeclarationCompiler.this.context.accessModifiers(loc, "public", "static"), // modifiers
                    null,                                                  // typeParameters
                    new ArrayType(new SimpleType(loc, enumIClass)),        // type
                    "values",                                              // name
                    new FormalParameters(loc),                             // parameters
                    new Type[0],                                           // thrownExceptions
                    null,                                                  // defaultValue
                    Arrays.asList(                                         // statements

                        // E[] tmp = new E[<number-of-constants>];
                        lvds,

                        // System.arraycopy(E.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
                        new ExpressionStatement(new MethodInvocation(
                            loc,                                                          // location
                            new SimpleType(loc, this.iClassLoader.TYPE_java_lang_System), // target
                            "arraycopy",                                                  // methodName
                            new Rvalue[] {                                                // arguments
                                new FieldAccessExpression(loc, new SimpleType(loc, enumIClass), "ENUM$VALUES"), // Argument #1: E.ENUM$VALUES
                                new IntegerLiteral(loc, "0"),                                                   // Argument #2: 0
                                new LocalVariableAccess(loc, this.context.getLocalVariable(lvds, vd)),                  // Argument #3: tmp
                                new IntegerLiteral(loc, "0"),                                                   // Argument #4: 0
                                new IntegerLiteral(loc, String.valueOf(numberOfEnumConstants)),                 // Argument #5: <number-of-constants>
                            }
                        )),

                        // return tmp;
                        new ReturnStatement(loc, new LocalVariableAccess(loc, this.context.getLocalVariable(lvds, vd)))
                    )
                );
                md.setDeclaringType(ed);

                this.methodCompiler.compile(md, cf);
            }

            // public static E valueOf(String s) {
            //     return (E) Enum.valueOf(E.class, s);
            // }
            FormalParameter fp = new FormalParameter(
                loc,                                                          // location
                new Modifier[0],                                              // modifiers
                new SimpleType(loc, this.iClassLoader.TYPE_java_lang_String), // type
                "s"                                                           // name
            );
            {
                MethodDeclarator md = new MethodDeclarator(
                    loc,                                                   // location
                    null,                                                  // docComment
                    UnitCompilerTypeDeclarationCompiler.this.context.accessModifiers(loc, "public", "static"), // modifiers
                    null,                                                  // typeParameters
                    new SimpleType(loc, enumIClass),                       // type
                    "valueOf",                                             // name
                    new FormalParameters(                                  // formalParameters
                        loc,                          // location
                        new FormalParameter[] { fp }, // parameters
                        false                         // variableArity
                    ),
                    new Type[0],                                           // thrownExceptions,
                    null,                                                  // defaultValue
                    Arrays.asList(                                         // statements

                        // return (E) Enum.valueOf(E.class, s);
                        new ReturnStatement(loc, new Cast(
                            loc,                             // location
                            new SimpleType(loc, enumIClass), // targetType
                            new MethodInvocation(            // value
                                loc,                                                        // location
                                new SimpleType(loc, this.iClassLoader.TYPE_java_lang_Enum), // target
                                "valueOf",                                                  // methodName
                                new Rvalue[] {                                              // arguments
                                    new ClassLiteral(loc, new SimpleType(loc, enumIClass)),
                                    new ParameterAccess(loc, fp)
                                }
                            )
                        ))
                    )
                );

                md.setEnclosingScope(ed);

                this.methodCompiler.compile(md, cf);
            }
        }

        // Compile declared methods.
        this.methodCompiler.compileDeclaredMethods(cd, cf);

        // Compile declared constructors.
        // As a side effect of compiling methods and constructors, synthetic "class-dollar" methods (which implement
        // class literals) are generated on-the fly. We need to note how many we have here so we can compile the
        // extras.
        final int declaredMethodCount = cd.getMethodDeclarations().size();
        {
            ConstructorDeclarator[] ctords = cd.getConstructors();

            int syntheticFieldCount = cd.syntheticFields.size();
            int methodInfoCount     = cf.methodInfos.size();
            for (ConstructorDeclarator ctord : ctords) this.methodCompiler.compile(ctord, cf);

            if (syntheticFieldCount != cd.syntheticFields.size()) {

                // New synthetic fields were created while the constructors were compiled -- need to re-compile all
                // constructors!
                syntheticFieldCount = cd.syntheticFields.size();
                while (cf.methodInfos.size() > methodInfoCount) cf.methodInfos.remove(methodInfoCount);
                for (ConstructorDeclarator ctord : ctords) {

                    // Clear the cached IConstructor -- necessary because the synthetic constructor parameters have
                    // changed!
                    ctord.iConstructor = null;

                    this.methodCompiler.compile(ctord, cf);
                }

                // Re-compilation of the constructors cannot possibly create even more synthetic fields!
                assert syntheticFieldCount == cd.syntheticFields.size();
            }
        }

        // A side effect of this call may create synthetic functions to access protected parent variables.
        this.compileDeclaredMemberTypes(cd, cf);

        // Compile the aforementioned extras.
        this.methodCompiler.compileDeclaredMethods(cd, cf, declaredMethodCount);

        // For every method look for bridge methods that need to be supplied. This is used to correctly dispatch into
        // covariant return types from existing code.
        for (IMethod base : iClass.getIMethods()) {
            if (!base.isStatic() && base.getAccess() != Access.PRIVATE) {
                // A concrete inherited erased method can coexist with a source override whose
                // parameters specialize class variables. Resolve those variables before choosing
                // the target, otherwise calls through the superclass silently run its old body.
                IMethod override = this.context.findGenericOverride(iClass, base);
                if (override == null) override = iClass.findIMethod(base.getName(), base.getParameterTypes());

                // An override can differ in its erased parameters as well as its return type.
                if (
                    override != null
                    && !base.getDescriptor().equals(override.getDescriptor())
                ) {
                    try {
                        this.methodCompiler.generateBridgeMethod(cf, iClass, base, override);
                    } catch (RuntimeException re) {
                        throw new InternalCompilerException(cd.getLocation(), (
                            "Generating bridge method from \""
                            + base
                            + "\" to \""
                            + override
                            + "\": "
                            + re.getMessage()
                        ), re);
                    }
                }
            }
        }

        // Add class and instance variables as (static and non-static) fields.
        for (FieldDeclaration fd : Iterables.filterByClass(
            cd.getVariableDeclaratorsAndInitializers(),
            FieldDeclaration.class
        )) this.addFields(fd, cf);

        // Synthetic fields.
        for (IField f : cd.getSyntheticFields().values()) {
            cf.addFieldInfo(
                Mod.PACKAGE,                                         // accessFlags
                f.getName(),                                         // fieldName
                UnitCompiler.rawTypeOf(f.getType()).getDescriptor(), // fieldTypeFD
                null                                                 // constantValue
            );
        }

        // Add the generated class file to a thread-local store.
        this.context.addClassFile(cf);
    }

    /**
     * Creates and adds {@link ClassFile.FieldInfo}s to the <var>cf</var> for all fields declared by the <var>fd</var>.
     */
    private void
    addFields(FieldDeclaration fd, ClassFile cf) throws CompileException {
        for (VariableDeclarator vd : fd.variableDeclarators) {

            Type type = vd.withType(fd.type);

            IClass fieldType = this.context.getRawType(type);
            Object ocv = ( // Optional constant value
                (fieldType.isPrimitive() || "Ljava/lang/String;".equals(fieldType.getDescriptor()))
                && (fd.isFinal() || fd.getDeclaringType() instanceof InterfaceDeclaration)
                && vd.initializer instanceof Rvalue
                ? this.context.constantAssignmentConversion(
                    vd.initializer,                                 // locatable
                    this.constantEvaluator.getConstantValue((Rvalue) vd.initializer), // value
                    this.context.getRawType(type)                           // targetType
                )
                : UnitCompiler.NOT_CONSTANT
            );

            short accessFlags = this.context.accessFlags(fd.modifiers);
            if (fd.getDeclaringType() instanceof Java.AbstractTypeDeclaration
                && vd.name.equals(((Java.AbstractTypeDeclaration) fd.getDeclaringType()).assertionsDisabledField)) accessFlags |= Mod.SYNTHETIC;
            if (fd.getDeclaringType() instanceof EnumDeclaration) {
                for (EnumConstant constant : ((EnumDeclaration) fd.getDeclaringType()).getConstants()) {
                    if (constant.name.equals(vd.name)) { accessFlags |= Mod.ENUM; break; }
                }
            }

            ClassFile.FieldInfo fi;
            if (fd.isPrivate()) {

                // To make the private field accessible for enclosing types, enclosed types and types enclosed by the
                // same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                accessFlags = UnitCompilerTypeDeclarationCompiler.this.context.changeAccessibility(accessFlags, Mod.PACKAGE);

                fi = cf.addFieldInfo(
                    accessFlags,                                  // accessFlags
                    vd.name,                                      // fieldName
                    this.context.getRawType(type).getDescriptor(),        // fieldTypeFD
                    ocv == UnitCompiler.NOT_CONSTANT ? null : ocv // constantValue
                );
            } else
            {
                fi = cf.addFieldInfo(
                    (                                             // accessFlags
                        fd.getDeclaringType() instanceof InterfaceDeclaration
                        ? (short) (Mod.PUBLIC | Mod.STATIC | Mod.FINAL)
                        : accessFlags
                    ),
                    vd.name,                                      // fieldName
                    this.context.getRawType(type).getDescriptor(),        // fieldTypeFD
                    ocv == UnitCompiler.NOT_CONSTANT ? null : ocv // constantValue
                );
            }

            // Add field annotations with retention != SOURCE.
            this.annotationCompiler.compileDeclarationAnnotations(fd.getAnnotations(), "FIELD", fi, cf);

            // Add the TYPE annotations (JSR 308) which appear in the declared type.
            if (this.context.getTargetVersion() >= 8) {
                UnitCompilerAnnotationCompiler.TypeAnnotationConsumer fiTac = UnitCompilerAnnotationCompiler.typeAnnotationsOf(fi);
                this.annotationCompiler.compileTypeAnnotations(
                    type,                                        // type
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_FIELD,              // targetType
                    -1,                                          // targetIndex
                    fiTac,                                       // tac
                    cf                                           // cf
                );

                // Plus the declaration-position annotations with TYPE_USE target.
                this.annotationCompiler.compileDeclarationPositionTypeAnnotations(
                    fd.getAnnotations(),                         // annotations
                    type,                                        // declaredType
                    UnitCompilerAnnotationCompiler.TARGET_TYPE_FIELD,              // targetType
                    -1,                                          // targetIndex
                    fiTac,                                       // tac
                    cf                                           // cf
                );

                this.annotationCompiler.flushTypeAnnotations(fiTac, cf);

                // Emit a "Signature" attribute for GENERIC field types (JVMS8 4.7.9), so that reflection sees
                // the parameterized type (e.g. "List<String>") rather than the raw type.
                String fieldSignature = this.signatureEncoder.toFieldSignature(type);
                if (fieldSignature != null) {
                    fi.addAttribute(new ClassFile.SignatureAttribute(
                        cf.addConstantUtf8Info("Signature"),
                        cf.addConstantUtf8Info(fieldSignature)
                    ));
                }
            }

            // Add "Deprecated" attribute (JVMS 4.7.10).
            if (fd.hasDeprecatedDocTag()) {
                fi.addAttribute(new ClassFile.DeprecatedAttribute(cf.addConstantUtf8Info("Deprecated")));
            }
        }
    }

    private void
    compile2(AnonymousClassDeclaration acd) throws CompileException {

        // For classes that enclose surrounding scopes, trawl their field initializers looking for synthetic fields.
//        this.fakeCompileVariableDeclaratorsAndInitializers(acd);

        this.compile2((InnerClassDeclaration) acd);
    }

    /** Constant-specific bodies are anonymous subclasses, not instances of the enum's base class. */
    private Rvalue
    enumConstantInitializer(EnumConstant constant, IClass enumType) throws CompileException {
        Rvalue[] arguments = constant.arguments == null ? new Rvalue[0] : constant.arguments;
        if (
            !constant.hasClassBody
            && constant.fieldDeclarationsAndInitializers.isEmpty()
            && constant.getMethodDeclarations().isEmpty()
            && constant.getMemberTypeDeclarations().isEmpty()
            && constant.constructors.isEmpty()
        ) return new NewClassInstance(constant.getLocation(), null, enumType, arguments);

        if (!constant.constructors.isEmpty()) {
            this.context.compileError("Enum constant class body must not declare a constructor", constant.getLocation());
        }
        AnonymousClassDeclaration body = new AnonymousClassDeclaration(
            constant.getLocation(), new SimpleType(constant.getLocation(), enumType)
        );
        body.enumConstantBody = true;
        DeepCopier copier = new DeepCopier();
        for (FieldDeclarationOrInitializer field : constant.fieldDeclarationsAndInitializers) {
            body.addFieldDeclarationOrInitializer(copier.copyFieldDeclarationOrInitializer(field));
        }
        for (MethodDeclarator method : constant.getMethodDeclarations()) {
            body.addDeclaredMethod(copier.copyMethodDeclarator(method));
        }
        for (MemberTypeDeclaration member : constant.getMemberTypeDeclarations()) {
            body.addMemberTypeDeclaration(copier.copyMemberTypeDeclaration(member));
        }
        return new NewAnonymousClassInstance(constant.getLocation(), null, body, copier.copyRvalues(arguments));
    }

    private void
    compile2(LocalClassDeclaration lcd) throws CompileException {

        // For classes that enclose surrounding scopes, trawl their field initializers looking for synthetic fields.
        this.fakeCompileVariableDeclaratorsAndInitializers(lcd);

        this.compile2((InnerClassDeclaration) lcd);
    }

    private void
    compile2(InnerClassDeclaration icd) throws CompileException {

        // Define a synthetic "this$n" field if there is an enclosing instance, where "n" designates the number of
        // enclosing instances minus one.
        {
            List<TypeDeclaration> ocs     = UnitCompilerTypeDeclarationCompiler.this.context.getOuterClasses(icd);
            final int             nesting = ocs.size();
            if (nesting >= 2) {
                TypeDeclaration immediatelyEnclosingOuterClassDeclaration = (TypeDeclaration) ocs.get(1);
                icd.defineSyntheticField(this.context.newSyntheticField(
                    this.context.resolve(icd),
                    "this$" + (nesting - 2),
                    this.context.resolve(immediatelyEnclosingOuterClassDeclaration)
                ));
            }
        }

        this.compile2((AbstractClassDeclaration) icd);
    }

    private void
    fakeCompileVariableDeclaratorsAndInitializers(AbstractClassDeclaration cd) throws CompileException {

        // Initializers run with the local class's "this", not with the enclosing method's locals.
        // In particular a static enclosing method may have no slot zero at all. Use a disposable
        // context so discovering captures neither reads nor changes the enclosing method's stack map.
        CodeContext saved = this.context.replaceCodeContext(new CodeContext(this.context.getCodeContext().getClassFile()));
        try {
            this.context.getCodeContext().saveLocalVariables();
            this.context.allocateLocalVariableSlotAndMarkAsInitialized(this.context.resolve(cd), "this");
            // Compilation can create synthetic variables, so we must not use an iterator.
            List<FieldDeclarationOrInitializer> fdais = cd.fieldDeclarationsAndInitializers;
            for (int i = 0; i < fdais.size(); i++) {
                this.statementCompiler.fakeCompile((BlockStatement) fdais.get(i));
            }
        } finally {
            this.context.replaceCodeContext(saved);
        }
    }

    private void
    compile2(InterfaceDeclaration id) throws CompileException {

        final IClass iClass = this.context.resolve(id);

        // Determine extended interfaces.
        IClass[] rawInterfaces = UnitCompilerTypeDeclarationCompiler.this.context.rawTypesOf((id.interfaces = this.context.getTypes(id.extendedTypes)));

        short accessFlags = this.context.accessFlags(id.getModifiers());
        accessFlags |= Mod.INTERFACE;
        accessFlags |= Mod.ABSTRACT;
        if (id instanceof AnnotationTypeDeclaration)  accessFlags |= Mod.ANNOTATION;
        if (id instanceof MemberInterfaceDeclaration) accessFlags |= Mod.STATIC;
        if (id.getEnclosingScope() instanceof InterfaceDeclaration) accessFlags |= Mod.PUBLIC;

        ClassFile cf = this.newClassFile(
            accessFlags,
            iClass,
            this.iClassLoader.TYPE_java_lang_Object,
            rawInterfaces
        );

        this.signatureEncoder.addClassSignature(id, cf);
        if (this.context.getTargetVersion() >= 8) this.annotationCompiler.compileClassTypeAnnotations(id, cf);

        if (id instanceof MemberInterfaceDeclaration) {
            TypeDeclaration owner = ((MemberInterfaceDeclaration) id).getDeclaringType();
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                cf.addConstantClassInfo(iClass.getDescriptor()),
                cf.addConstantClassInfo(this.context.resolve(owner).getDescriptor()),
                cf.addConstantUtf8Info(id.getName()),
                (short) (accessFlags | Mod.STATIC)
            ));
        }

        // Add interface annotations with retention != SOURCE.
        this.annotationCompiler.compileDeclarationAnnotations(id.getAnnotations(), id instanceof AnnotationTypeDeclaration ? "ANNOTATION_TYPE" : "TYPE", cf, cf);

        // Set "SourceFile" attribute.
        if (this.context.debugSource()) {
            String sourceFileName;
            {
                String s = id.getLocation().getFileName();
                if (s != null) {
                    sourceFileName = new File(s).getName();
                } else {
                    sourceFileName = id.getName() + ".java";
                }
            }
            cf.addSourceFileAttribute(sourceFileName);
        }

        // Add "Deprecated" attribute (JVMS 4.7.10).
        if (id.hasDeprecatedDocTag()) cf.addDeprecatedAttribute();

        // Interface initialization method.
        if (!id.constantDeclarations.isEmpty()) {
            List<BlockStatement> statements = new ArrayList<>();
            statements.addAll(id.constantDeclarations);

            this.methodCompiler.maybeCreateInitMethod(id, cf, statements);
        }

        this.methodCompiler.compileDeclaredMethods(id, cf);

        // An interface must itself supply the erased entry point for a specialized default
        // (or covariant abstract) override, including when its implementor is compiled elsewhere.
        if (this.context.getTargetVersion() >= 8) for (IMethod base : iClass.getIMethods()) {
            if (base.isStatic() || base.getAccess() == Access.PRIVATE) continue;
            IMethod override = this.context.findGenericOverride(iClass, base);
            if (override != null && override.getDeclaringIClass() == iClass
                && !base.getDescriptor().equals(override.getDescriptor())) {
                this.methodCompiler.generateBridgeMethod(cf, iClass, base, override);
            }
        }

        // Class variables.
        for (FieldDeclaration constantDeclaration : id.constantDeclarations) this.addFields(constantDeclaration, cf);

        this.compileDeclaredMemberTypes(id, cf);

        // Add the generated class file to a thread-local store.
        this.context.addClassFile(cf);
    }

    /**
     * @param superclass {@code null} for {@link Object}, {@link Object} for interfaces
     */
    private ClassFile
    newClassFile(
        short            accessFlags,
        IClass           iClass,
        @Nullable IClass superclass,
        IClass[]         interfaces
    ) throws CompileException {
        // Class headers have no protected/private/static flags. A protected member type must be
        // public to the VM so subclasses in other packages can extend/implement it. Preserve the
        // source accessibility in InnerClasses, where the Java compiler reads it back.
        short classFlags = accessFlags;
        if (Mod.isProtectedAccess(classFlags)) classFlags = Mod.changeAccess(classFlags, Mod.PUBLIC);
        classFlags &= Mod.PUBLIC | Mod.FINAL | Mod.SUPER | Mod.INTERFACE | Mod.ABSTRACT
            | Mod.SYNTHETIC | Mod.ANNOTATION | Mod.ENUM;
        if ((classFlags & Mod.INTERFACE) == 0) classFlags |= Mod.SUPER;
        ClassFile result = new ClassFile(
            classFlags,                                             // accessFlags
            iClass.getDescriptor(),                                 // thisClassFD
            superclass != null ? superclass.getDescriptor() : null, // superclassFD
            IClass.getDescriptors(interfaces)                       // interfaceFDs
        );

        int v = this.context.getTargetVersion();

        if (v < 6) throw new CompileException("Cannot generate version " + v + " .class files", Location.NOWHERE);

        result.setVersion((short) (44 + v), ClassFile.MINOR_VERSION_JDK_1_6);

        return result;
    }

    /**
     * Compiles all of the types for this declaration
     * <p>
     *   NB: as a side effect this will fill in the synthetic field map
     * </p>
     */
    private void
    compileDeclaredMemberTypes(TypeDeclaration decl, ClassFile cf) throws CompileException {
        for (MemberTypeDeclaration mtd : decl.getMemberTypeDeclarations()) {
            this.compile(mtd);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.context.resolve(mtd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.context.resolve(decl).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(mtd.getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,                 // innerClassInfoIndex
                outerClassInfoIndex,                 // outerClassInfoIndex
                innerNameIndex,                      // innerNameIndex
                (short) (this.context.accessFlags(mtd.getModifiers())
                    | (decl instanceof InterfaceDeclaration ? Mod.PUBLIC | Mod.STATIC : 0)
                    | (mtd instanceof InterfaceDeclaration ? Mod.INTERFACE | Mod.ABSTRACT | Mod.STATIC : 0)
                    | (mtd instanceof EnumDeclaration ? Mod.ENUM | Mod.STATIC : 0)) // innerClassAccessFlags
            ));
        }
    }
}
