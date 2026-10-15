
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.util.SystemProperties;
import org.codehaus.commons.compiler.util.iterator.Iterables;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.CodeContext.BasicBlock;
import org.codehaus.janino.CodeContext.Offset;
import org.codehaus.janino.IClass.IAnnotation;
import org.codehaus.janino.IClass.IConstructor;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IInvocable;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AccessModifier;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BooleanRvalue;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CatchParameter;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EnclosingScopeOfTypeDeclaration;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.InnerClassDeclaration;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.BlockLambdaBody;import org.codehaus.janino.Java.ExpressionLambdaBody;import org.codehaus.janino.Java.FormalLambdaParameters;import org.codehaus.janino.Java.IdentifierLambdaParameters;import org.codehaus.janino.Java.InferredLambdaParameters;import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.Literal;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.ModularCompilationUnit;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NamedTypeDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.Package;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.Scope;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.TextBlock;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.Wildcard;
import org.codehaus.janino.Visitor.AbstractCompilationUnitVisitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.ArrayInitializerOrRvalueVisitor;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.FunctionDeclaratorVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.Visitor.TypeVisitor;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.ClassFileException;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.VerificationTypeInfo;

/**
 * This class actually implements the Java compiler. It is associated with exactly one compilation unit which it
 * compiles.
 * Generic signatures, annotations, instruction emission, constant evaluation and statement compilation are
 * delegated to package-private components with the {@code UnitCompiler} name prefix.
 * Semantic resolution and the lifetime of the active method's {@link CodeContext} remain with this class.
 * Type declarations, methods and statements are compiled by cooperating components in this package.
 */
public
class UnitCompiler {

    private final UnitCompilerConstantEvaluator constantEvaluator;
    private final UnitCompilerStatementCompiler statementCompiler;
    private final UnitCompilerMethodCompiler methodCompiler;
    private final UnitCompilerTypeDeclarationCompiler typeDeclarationCompiler;

    private final UnitCompilerSignatureEncoder signatureEncoder;
    private final UnitCompilerAnnotationCompiler annotationCompiler;
    private final UnitCompilerBytecodeEmitter bytecodeEmitter;
    private static final Logger LOGGER = Logger.getLogger(UnitCompiler.class.getName());

    private static final int defaultTargetVersion = SystemProperties.getIntegerClassProperty(UnitCompiler.class, "defaultTargetVersion", -1);

    /**
     * This constant determines the number of operands up to which the
     * <pre>
     *      a.concat(b).concat(c)
     * </pre>
     * <p>
     *  strategy is used to implement string concatenation. For more operands, the
     * </p>
     * <pre>
     *      new StringBuilder(a).append(b).append(c).append(d).toString()
     * </pre>
     * strategy is chosen.
     * <p>
     *   <a href="http://www.tomgibara.com/janino-evaluation/string-concatenation-benchmark">A very good article from
     *   Tom Gibara</a> analyzes the impact of this decision and recommends a value of three.
     * </p>
     */
    private static final int STRING_CONCAT_LIMIT = 3;

    /**
     * Special value for the <var>orientation</var> parameter of the {@link #compileBoolean(Java.Rvalue,
     * CodeContext.Offset, boolean)} methods, indicating that the code should be generated such that execution branches
     * if the value on top of the operand stack is TRUE.
     */
    public static final boolean JUMP_IF_TRUE  = true;

    /**
     * Special value for the <var>orientation</var> parameter of the {@link #compileBoolean(Java.Rvalue,
     * CodeContext.Offset, boolean)} methods, indicating that the code should be generated such that execution branches
     * if the value on top of the operand stack is FALSE.
     */
    public static final boolean JUMP_IF_FALSE = false;

    private static final Pattern LOOKS_LIKE_TYPE_PARAMETER = Pattern.compile("\\p{javaUpperCase}+");

    private EnumSet<JaninoOption> options = EnumSet.noneOf(JaninoOption.class);

    /**
     * Java version to compile for.
     */
    private int targetVersion = -1;

    public
    UnitCompiler(AbstractCompilationUnit abstractCompilationUnit, IClassLoader iClassLoader) {
        this.abstractCompilationUnit = abstractCompilationUnit;
        this.iClassLoader            = iClassLoader;

        this.signatureEncoder = new UnitCompilerSignatureEncoder(new UnitCompilerSignatureEncoder.Context() {
            @Override public GenericType genericType(Type type) throws CompileException { return UnitCompiler.this.sourceGenericType(type); }
            @Override public IClass getRawType(Type type) throws CompileException {
                return UnitCompiler.this.getRawType(type);
            }
            @Override public IClass resolve(TypeDeclaration declaration) {
                return UnitCompiler.this.resolve(declaration);
            }
        });

        this.annotationCompiler = new UnitCompilerAnnotationCompiler(new UnitCompilerAnnotationCompiler.Context() {
            @Override public IClass getRawType(Type type) throws CompileException {
                return UnitCompiler.this.getRawType(type);
            }
            @Override @Nullable public Object getConstantValue(Rvalue value) throws CompileException {
                return UnitCompiler.this.constantEvaluator.getConstantValue(value);
            }
            @Override public Atom reclassify(AmbiguousName name) throws CompileException {
                return UnitCompiler.this.reclassify(name);
            }
            @Override @Nullable public IClass findTypeByName(Location location, String name) throws CompileException {
                return UnitCompiler.this.findTypeByName(location, name);
            }
            @Override public void compileError(String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.compileError(message, location);
            }
        }, iClassLoader);

        this.bytecodeEmitter = new UnitCompilerBytecodeEmitter(new UnitCompilerBytecodeEmitter.Context() {
            @Override public CodeContext getCodeContext() {
                return UnitCompiler.this.getCodeContext();
            }
            @Override public int getTargetVersion() {
                return UnitCompiler.this.getTargetVersion();
            }
            @Override public void compileError(String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.compileError(message, location);
            }
        }, iClassLoader);

        this.constantEvaluator = new UnitCompilerConstantEvaluator(new UnitCompilerConstantEvaluator.Context() {
            @Override public void
            fakeCompile(Rvalue rv) throws CompileException {
                UnitCompiler.this.fakeCompile(rv);
            }
            @Override public boolean
            mayHaveSideEffects(ArrayInitializerOrRvalue arrayInitializerOrRvalue) {
                return UnitCompiler.mayHaveSideEffects(arrayInitializerOrRvalue);
            }
            @Override public IType
            getType(Type t) throws CompileException {
                return UnitCompiler.this.getType(t);
            }
            @Override public IType
            getType2(ConditionalExpression ce) throws CompileException {
                return UnitCompiler.this.getType2(ce);
            }
            @Override public Rvalue
            toRvalueOrCompileException(Atom a) throws CompileException {
                return UnitCompiler.this.toRvalueOrCompileException(a);
            }
            @Override public Atom
            reclassify(AmbiguousName an) throws CompileException {
                return UnitCompiler.this.reclassify(an);
            }
            @Override public CompileException
            compileException(Locatable locatable, String message) {
                return UnitCompiler.compileException(locatable, message);
            }
        }, iClassLoader);

        this.statementCompiler = new UnitCompilerStatementCompiler(new UnitCompilerStatementCompiler.Context() {
            @Override public void
            compile(TypeDeclaration td) throws CompileException {
                UnitCompiler.this.typeDeclarationCompiler.compile(td);
            }
            @Override public CodeContext
            getCodeContext() {
                return UnitCompiler.this.getCodeContext();
            }
            @Override public LocalVariable
            allocateLocalVariable(boolean finaL, IType localVariableType) {
                return UnitCompiler.this.allocateLocalVariable(finaL, localVariableType);
            }
            @Override @Nullable public LocalClassDeclaration
            findLocalClassDeclaration(Scope s, String name) {
                return UnitCompiler.findLocalClassDeclaration(s, name);
            }
            @Override public void
            compile(ArrayInitializerOrRvalue aiorv, IType arrayType) throws CompileException {
                UnitCompiler.this.compile(aiorv, arrayType);
            }
            @Override public LocalVariable
            getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException {
                return UnitCompiler.this.getLocalVariable(lvds, vd);
            }
            @Override public LocalVariableSlot
            allocateLocalVariableSlot(IType localVariableType, @Nullable String localVariableName) {
                return UnitCompiler.this.allocateLocalVariableSlot(localVariableType, localVariableName);
            }
            @Override public LocalVariable
            getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException {
                return UnitCompiler.this.getLocalVariable(parameter, isVariableArityParameter);
            }
            @Override public LocalVariable
            getLocalVariable(CatchParameter parameter) throws CompileException {
                return UnitCompiler.this.getLocalVariable(parameter);
            }
            @Override public IClass
            lub(List<IClass> types) throws CompileException {
                return UnitCompiler.this.lub(types);
            }
            @Override public void
            fakeCompile(Rvalue rv) throws CompileException {
                UnitCompiler.this.fakeCompile(rv);
            }
            @Override public void
            compile(Rvalue rv) throws CompileException {
                UnitCompiler.this.compile(rv);
            }
            @Override public boolean
            compile2(AlternateConstructorInvocation aci) throws CompileException {
                return UnitCompiler.this.compile2(aci);
            }
            @Override public boolean
            compile2(SuperConstructorInvocation sci) throws CompileException {
                return UnitCompiler.this.compile2(sci);
            }
            @Override public void
            compileBoolean(Rvalue rv, CodeContext.Offset dst, boolean orientation) throws CompileException {
                UnitCompiler.this.compileBoolean(rv, dst, orientation);
            }
            @Override public void
            setLambdaTargetType(Rvalue rv, @Nullable IType targetType) {
                UnitCompiler.this.setLambdaTargetType(rv, targetType);
            }
            @Override @Nullable public IType
            getComponentType(IType expressionType) {
                return UnitCompiler.getComponentType(expressionType);
            }
            @Override public boolean
            isArray(IType type) {
                return UnitCompiler.isArray(type);
            }
            @Override public boolean
            isAssignableFrom(IType targetType, IType sourceType) throws CompileException {
                return UnitCompiler.isAssignableFrom(targetType, sourceType);
            }
            @Override public IType
            compileGetValue(Rvalue rv) throws CompileException {
                return UnitCompiler.this.compileGetValue(rv);
            }
            @Override public IClass
            getRawType(Type t) throws CompileException {
                return UnitCompiler.this.getRawType(t);
            }
            @Override public IType
            getType(Type t) throws CompileException {
                return UnitCompiler.this.getType(t);
            }
            @Override public IType
            getType(Rvalue rv) throws CompileException {
                return UnitCompiler.this.getType(rv);
            }
            @Override public void
            invokeConstructor(
                Locatable        locatable,
                Scope            scope,
                @Nullable Rvalue enclosingInstance,
                IType            targetType,
                Rvalue[]         arguments
            ) throws CompileException {
                UnitCompiler.this.invokeConstructor(locatable, scope, enclosingInstance, targetType, arguments);
            }
            @Override @Nullable public ArrayInitializerOrRvalue
            getNonConstantFinalInitializer(FieldDeclaration fd, VariableDeclarator vd) throws CompileException {
                return UnitCompiler.this.getNonConstantFinalInitializer(fd, vd);
            }
            @Override @Nullable public List<IType> preciseRethrowTypes(ThrowStatement statement) {
                return UnitCompiler.this.preciseRethrowTypes(statement);
            }
            @Override public void
            checkThrownException(Locatable locatable, IType type, Scope scope) throws CompileException {
                UnitCompiler.this.checkThrownException(locatable, type, scope);
            }
            @Override public IClass
            resolve(TypeDeclaration td) {
                return UnitCompiler.this.resolve(td);
            }
            @Override public IType
            getReturnType(FunctionDeclarator fd) throws CompileException {
                return UnitCompiler.this.getReturnType(fd);
            }
            @Override public IClass
            consT(Locatable locatable, @Nullable Object value) throws CompileException {
                return UnitCompiler.this.consT(locatable, value);
            }
            @Override public void
            consT(Locatable locatable, int value) {
                UnitCompiler.this.consT(locatable, value);
            }
            @Override public void
            consT(Locatable locatable, String s) {
                UnitCompiler.this.consT(locatable, s);
            }
            @Override public void
            assignmentConversion(
                Locatable        locatable,
                IType            sourceType,
                IType            targetType,
                @Nullable Object constantValue
            ) throws CompileException {
                UnitCompiler.this.assignmentConversion(locatable, sourceType, targetType, constantValue);
            }
            @Override public boolean
            tryAssignmentConversion(
                Locatable        locatable,
                IType            sourceType,
                IType            targetType,
                @Nullable Object constantValue
            ) throws CompileException {
                return UnitCompiler.this.tryAssignmentConversion(locatable, sourceType, targetType, constantValue);
            }
            @Override @Nullable public Object
            constantAssignmentConversion(Locatable locatable, @Nullable Object value, IType targetType) throws CompileException {
                return UnitCompiler.this.constantAssignmentConversion(locatable, value, targetType);
            }
            @Override public boolean
            tryNarrowingReferenceConversion(Locatable locatable, IType sourceType, IType targetType) throws CompileException {
                return UnitCompiler.this.tryNarrowingReferenceConversion(locatable, sourceType, targetType);
            }
            @Override @Nullable public IClass
            isBoxingConvertible(IType sourceType) {
                return UnitCompiler.this.isBoxingConvertible(sourceType);
            }
            @Override public void
            unboxingConversion(Locatable locatable, IType sourceType, IClass targetType) {
                UnitCompiler.this.unboxingConversion(locatable, sourceType, targetType);
            }
            @Override public void
            compileError(String message) throws CompileException {
                UnitCompiler.this.compileError(message);
            }
            @Override public void
            compileError(String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.compileError(message, location);
            }
            @Override public void
            warning(String handle, String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.warning(handle, message, location);
            }
            @Override public EnumSet<JaninoOption> options() { return UnitCompiler.this.options; }

            @Override public void
            buildLocalVariableMap(CatchClause catchClause, Map<String, LocalVariable> localVars) throws CompileException {
                UnitCompiler.this.buildLocalVariableMap(catchClause, localVars);
            }
            @Override public LocalVariable
            getLocalVariable(FormalParameter parameter) throws CompileException {
                return UnitCompiler.this.getLocalVariable(parameter);
            }
            @Override public Atom
            reclassify(AmbiguousName an) throws CompileException {
                return UnitCompiler.this.reclassify(an);
            }
            @Override public CompileException
            compileException(Locatable locatable, String message) {
                return UnitCompiler.compileException(locatable, message);
            }
            @Override public void compileCatchTypeAnnotations(Type type, Modifier[] modifiers, int index) throws CompileException {
                List<Annotation> annotations = new ArrayList<>();
                for (Modifier modifier : modifiers) if (modifier instanceof Annotation) annotations.add((Annotation) modifier);
                if (UnitCompiler.this.getTargetVersion() >= 8) UnitCompiler.this.annotationCompiler.compileCodeTypeAnnotations(
                    type, annotations.toArray(new Annotation[annotations.size()]), (byte) 0x42, UnitCompiler.this.getCodeContext(), UnitCompiler.this.getCodeContext().newOffset(), null, index);
            }
            @Override public void compileLocalTypeAnnotations(Type type, Modifier[] modifiers, byte target, LocalVariableSlot slot)
            throws CompileException {
                if (UnitCompiler.this.getTargetVersion() < 8) return;
                List<Annotation> annotations = new ArrayList<>();
                for (Modifier modifier : modifiers) if (modifier instanceof Annotation) annotations.add((Annotation) modifier);
                UnitCompiler.this.annotationCompiler.compileCodeTypeAnnotations(type, annotations.toArray(new Annotation[annotations.size()]), target,
                    UnitCompiler.this.getCodeContext(), UnitCompiler.this.getCodeContext().newOffset(), slot, 0);
            }
        }, iClassLoader, this.bytecodeEmitter, this.constantEvaluator);

        this.methodCompiler = new UnitCompilerMethodCompiler(new UnitCompilerMethodCompiler.Context() {
            @Override public IType capturedFieldType(IField field) throws CompileException {
                return field instanceof SimpleIField ? ((SimpleIField) field).compileTimeType : field.getType();
            }
            @Override public CodeContext
            getCodeContext() {
                return UnitCompiler.this.getCodeContext();
            }
            @Override public LocalVariable
            allocateLocalVariableAndMarkAsInitialized(boolean finaL, IType localVariableType) {
                return UnitCompiler.this.allocateLocalVariableAndMarkAsInitialized(finaL, localVariableType);
            }
            @Override public int
            getTargetVersion() {
                return UnitCompiler.this.getTargetVersion();
            }
            @Override public LocalVariableSlot
            allocateLocalVariableSlotAndMarkAsInitialized(IType localVariableType, @Nullable String localVariableName) {
                return UnitCompiler.this.allocateLocalVariableSlotAndMarkAsInitialized(localVariableType, localVariableName);
            }
            @Override public LocalVariable
            getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException {
                return UnitCompiler.this.getLocalVariable(parameter, isVariableArityParameter);
            }
            @Override @Nullable public IMethod
            findGenericOverride(IClass type, IMethod base) throws CompileException {
                return UnitCompiler.this.findGenericOverride(type, base);
            }
            @Override public boolean compatibleOverrideReturn(IMethod method, IMethod base) throws CompileException {
                return UnitCompiler.this.compatibleOverrideReturn(method, base);
            }
            @Override public boolean compatibleOverrideReturn(IMethod method, IMethod base, IClass type) throws CompileException {
                return UnitCompiler.this.compatibleOverrideReturn(method, base, type);
            }
            @Override public IClass[] inheritedThrownExceptions(IMethod method, IClass receiver) throws CompileException {
                GenericType[] generic = method.getGenericThrownExceptions();
                if (generic == null) return method.getThrownExceptions();
                Map<String, IType> environment = UnitCompiler.this.receiverEnvironment(receiver, method.getDeclaringIClass());
                List<IClass> result = new ArrayList<>();
                for (GenericType exception : generic) {
                    IType resolved = UnitCompiler.this.substituteGenericType(exception, environment);
                    if (resolved != null) result.add(UnitCompiler.rawTypeOf(resolved));
                    else for (IClass erased : method.getThrownExceptions()) result.add(erased);
                }
                return result.toArray(new IClass[result.size()]);
            }
            @Override public IClass
            getRawType(Type t) throws CompileException {
                return UnitCompiler.this.getRawType(t);
            }
            @Override public IType
            getType(Type t) throws CompileException {
                return UnitCompiler.this.getType(t);
            }
            @Override public IClass
            resolve(TypeDeclaration td) {
                return UnitCompiler.this.resolve(td);
            }
            @Override public IType
            getReturnType(FunctionDeclarator fd) throws CompileException {
                return UnitCompiler.this.getReturnType(fd);
            }
            @Override public IClass.IMethod
            toIMethod(MethodDeclarator methodDeclarator) {
                return UnitCompiler.this.toIMethod(methodDeclarator);
            }
            @Override public IClass.IInvocable
            toIInvocable(FunctionDeclarator fd) {
                return UnitCompiler.this.toIInvocable(fd);
            }
            @Override public void
            compileError(String message) throws CompileException {
                UnitCompiler.this.compileError(message);
            }
            @Override public void
            compileError(String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.compileError(message, location);
            }
            @Override public void
            warning(String handle, String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.warning(handle, message, location);
            }
            @Override @Nullable public CodeContext
            replaceCodeContext(@Nullable CodeContext newCodeContext) {
                return UnitCompiler.this.replaceCodeContext(newCodeContext);
            }
            @Override public short
            accessFlags(Modifier[] modifiers) throws CompileException {
                return UnitCompiler.this.accessFlags(modifiers);
            }
            @Override public Modifier[]
            accessModifiers(Location location, String... keywords) {
                return UnitCompiler.accessModifiers(location, keywords);
            }
            @Override public short
            changeAccessibility(short accessFlags, short newAccessibility) {
                return UnitCompiler.changeAccessibility(accessFlags, newAccessibility);
            }
            @Override public int compileErrorCount() { return UnitCompiler.this.compileErrorCount; }
            @Override public boolean debugLines() { return UnitCompiler.this.debugLines; }
            @Override public boolean debugVars() { return UnitCompiler.this.debugVars; }
            @Override public boolean parameters() { return UnitCompiler.this.parameters; }
            @Override public IClass[] anonymousInitializerExceptions(AnonymousClassDeclaration owner) {
                Set<IClass> exceptions = UnitCompiler.this.anonymousInitializerExceptions.get(owner);
                return exceptions == null ? new IClass[0] : exceptions.toArray(new IClass[exceptions.size()]);
            }
            @Override public boolean looksLikeTypeParameter(String name) {
                return UnitCompiler.LOOKS_LIKE_TYPE_PARAMETER.matcher(name).matches();
            }
        }, iClassLoader, this.signatureEncoder, this.annotationCompiler, this.bytecodeEmitter, this.statementCompiler);

        this.typeDeclarationCompiler = new UnitCompilerTypeDeclarationCompiler(new UnitCompilerTypeDeclarationCompiler.Context() {
            @Override public void
            addClassFile(ClassFile cf) {
                UnitCompiler.this.addClassFile(cf);
            }
            @Override public CodeContext
            getCodeContext() {
                return UnitCompiler.this.getCodeContext();
            }
            @Override public LocalVariable
            getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException {
                return UnitCompiler.this.getLocalVariable(lvds, vd);
            }
            @Override public int
            getTargetVersion() {
                return UnitCompiler.this.getTargetVersion();
            }
            @Override public LocalVariableSlot
            allocateLocalVariableSlotAndMarkAsInitialized(IType localVariableType, @Nullable String localVariableName) {
                return UnitCompiler.this.allocateLocalVariableSlotAndMarkAsInitialized(localVariableType, localVariableName);
            }
            @Override public boolean compatibleOverrideReturn(IMethod method, IMethod base, IClass type) throws CompileException {
                return UnitCompiler.this.compatibleOverrideReturn(method, base, type);
            }
            @Override @Nullable public IMethod
            findGenericOverride(IClass type, IMethod base) throws CompileException {
                return UnitCompiler.this.findGenericOverride(type, base);
            }
            @Override public IClass[]
            rawTypesOf(IType[] iTypes) {
                return UnitCompiler.rawTypesOf(iTypes);
            }
            @Override public IClass
            getRawType(Type t) throws CompileException {
                return UnitCompiler.this.getRawType(t);
            }
            @Override public IType[]
            getTypes(Type[] types) throws CompileException {
                return UnitCompiler.this.getTypes(types);
            }
            @Override public IClass
            resolve(TypeDeclaration td) {
                return UnitCompiler.this.resolve(td);
            }
            @Override public List<TypeDeclaration>
            getOuterClasses(TypeDeclaration inner) {
                return UnitCompiler.getOuterClasses(inner);
            }
            @Override @Nullable public String[]
            getSingleTypeImport(String name, Location location) throws CompileException {
                return UnitCompiler.this.getSingleTypeImport(name, location);
            }
            @Override @Nullable public Object
            constantAssignmentConversion(Locatable locatable, @Nullable Object value, IType targetType) throws CompileException {
                return UnitCompiler.this.constantAssignmentConversion(locatable, value, targetType);
            }
            @Override public void
            compileError(String message, @Nullable Location location) throws CompileException {
                UnitCompiler.this.compileError(message, location);
            }
            @Override @Nullable public CodeContext
            replaceCodeContext(@Nullable CodeContext newCodeContext) {
                return UnitCompiler.this.replaceCodeContext(newCodeContext);
            }
            @Override public short
            accessFlags(Modifier[] modifiers) throws CompileException {
                return UnitCompiler.this.accessFlags(modifiers);
            }
            @Override public Modifier[]
            accessModifiers(Location location, String... keywords) {
                return UnitCompiler.accessModifiers(location, keywords);
            }
            @Override public short
            changeAccessibility(short accessFlags, short newAccessibility) {
                return UnitCompiler.changeAccessibility(accessFlags, newAccessibility);
            }
            @Override public String functionDescriptor(FunctionDeclarator function) throws CompileException {
                return UnitCompiler.this.toIInvocable(function).getDescriptor().toString();
            }
            @Override public boolean debugSource() { return UnitCompiler.this.debugSource; }
            @Override public IField newSyntheticField(IClass declaringClass, String name, IClass type) {
                return new SimpleIField(declaringClass, name, type);
            }

        }, iClassLoader, this.signatureEncoder, this.annotationCompiler, this.constantEvaluator, this.statementCompiler, this.methodCompiler);

    }

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.options; }

    /**
     * Sets the options for all future compilations.
     */
    public UnitCompiler
    options(EnumSet<JaninoOption> options) {
        this.options = options;
        return this;
    }

    /**
     * Generates class files that target a specified release of the virtual machine, in analogy with JAVAC's {@code
     * -target} command line option.
     * By default, Java 6 .class files are generated.
     */
    public void
    setTargetVersion(int version) { this.targetVersion = version; }

    public void setParameters(boolean value) { this.parameters = value; }
    private boolean parameters;

    /**
     * @return The {@link AbstractCompilationUnit} that this {@link UnitCompiler} compiles
     */
    public AbstractCompilationUnit
    getAbstractCompilationUnit() { return this.abstractCompilationUnit; }

    /**
     * Generates a set of {@link ClassFile} objects which represent the classes and interfaces declared in the
     * compilation unit.
     *
     * @param generatedClassFiles Adds generated {@link ClassFile}s to this {@link Collection}
     */
    public void
    compileUnit(
        boolean                     debugSource,
        boolean                     debugLines,
        boolean                     debugVars,
        final Collection<ClassFile> generatedClassFiles
    ) throws CompileException {

        this.compileUnit(
            debugSource,
            debugLines,
            debugVars,
            new ClassFileConsumer() { @Override public void consume(ClassFile classFile) { generatedClassFiles.add(classFile); }
        });
    }

    /**
     * Generates a set of {@link ClassFile} objects which represent the classes and interfaces declared in the
     * compilation unit.
     *
     * @param storesClassFiles Consumes each generated {@link ClassFile}
     */
    public void
    compileUnit(boolean debugSource, boolean debugLines, boolean debugVars, ClassFileConsumer storesClassFiles)
    throws CompileException {

        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;

        if (this.storesClassFiles != null) {
            throw new IllegalStateException("\"UnitCompiler.compileUnit()\" is not reentrant");
        }
        this.storesClassFiles = storesClassFiles;
        try {

            this.abstractCompilationUnit.accept(new AbstractCompilationUnitVisitor<Void, CompileException>() {

                @Override @Nullable public Void visitCompilationUnit(CompilationUnit cu)                throws CompileException { UnitCompiler.this.compile2(cu);  return null; }
                @Override @Nullable public Void visitModularCompilationUnit(ModularCompilationUnit mcu) throws CompileException { UnitCompiler.this.compile2(mcu); return null; }
            });

            if (this.compileErrorCount > 0) {
                throw new CompileException((
                    this.compileErrorCount
                    + " error(s) while compiling unit \""
                    + this.abstractCompilationUnit.fileName
                    + "\""
                ), null);
            }
        } finally {
            this.storesClassFiles = null;
        }
    }

    public
    interface ClassFileConsumer {
        void consume(ClassFile classFile) throws IOException;
    }
    /**
     * Compiles an (ordinary, not modular) compilation unit
     */
    private void
    compile2(CompilationUnit cu) throws CompileException {

        for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            try {
                this.typeDeclarationCompiler.compile(pmtd);
            } catch (ClassFileException cfe) {
                throw new CompileException(cfe.getMessage(), pmtd.getLocation(), cfe);
            } catch (RuntimeException re) {
                throw new InternalCompilerException((
                    "Compiling \""
                    + pmtd
                    + "\" in "
                    + pmtd.getLocation()
                    + ": "
                    + re.getMessage()
                ), re);
            }
        }
    }

    private void
    compile2(ModularCompilationUnit mcu) throws CompileException {
        this.compileError("Compilation of modular compilation unit not implemented");
    }

    /**
     * Adds the given {@link ClassFile} to the result set.
     */
    private void
    addClassFile(ClassFile cf) {

        assert this.storesClassFiles != null;
        try {
            this.storesClassFiles.consume(cf);
        } catch (IOException ioe) {
            throw new InternalCompilerException(cf.getThisClassName(), ioe);
        }
    }

    private CodeContext
    getCodeContext() {
        assert this.codeContext != null;
        return this.codeContext;
    }

    private LocalVariable
    allocateLocalVariable(boolean finaL, IType localVariableType) {

        LocalVariable result = new LocalVariable(finaL, localVariableType);

        result.setSlot(this.allocateLocalVariableSlot(localVariableType, null));

        return result;
    }

    private LocalVariable
    allocateLocalVariableAndMarkAsInitialized(boolean finaL, IType localVariableType) {

        LocalVariable result = new LocalVariable(finaL, localVariableType);

        result.setSlot(this.allocateLocalVariableSlotAndMarkAsInitialized(localVariableType, null));

        return result;
    }

    /**
     * Finds a local class declared in any block enclosing the given block statement.
     */
    @Nullable private static LocalClassDeclaration
    findLocalClassDeclaration(Scope s, String name) {

        if (s instanceof CompilationUnit) return null;

        for (;;) {
            Scope es = s.getEnclosingScope();
            if (es instanceof CompilationUnit) break;
            if (
                s instanceof BlockStatement
                && (es instanceof Block || es instanceof FunctionDeclarator)
            ) {
                BlockStatement                 bs         = (BlockStatement) s;
                List<? extends BlockStatement> statements = (
                    es instanceof BlockStatement
                    ? ((Block) es).statements
                    : ((FunctionDeclarator) es).statements
                );
                if (statements != null) {
                    for (BlockStatement bs2 : statements) {
                        if (bs2 instanceof LocalClassDeclarationStatement) {
                            LocalClassDeclarationStatement lcds = ((LocalClassDeclarationStatement) bs2);
                            if (lcds.lcd.name.equals(name)) return lcds.lcd;
                        }
                        if (bs2 == bs) break;
                    }
                }
            }
            s = es;
        }

        return null;
    }

    private void
    compile(final ArrayInitializerOrRvalue aiorv, final IType arrayType) throws CompileException {

        aiorv.accept(new ArrayInitializerOrRvalueVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitArrayInitializer(ArrayInitializer ai) throws CompileException {
                UnitCompiler.this.compileGetValue(ai, arrayType);
                return null;
            }

            @Override @Nullable public Void
            visitRvalue(Rvalue rhs) throws CompileException {
                UnitCompiler.this.setLambdaTargetType(rhs, arrayType);
                UnitCompiler.this.assignmentConversion(
                    aiorv,                                  // locatable
                    UnitCompiler.this.compileGetValue(rhs), // sourceType
                    arrayType,                              // targetType
                    UnitCompiler.this.constantEvaluator.getConstantValue(rhs) // constantValue
                );
                return null;
            }
        });
    }

    /**
     * @return The {@link LocalVariable} corresponding with the local variable declaration/declarator
     */
    public LocalVariable
    getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException {

        if (vd.localVariable != null) return vd.localVariable;

        // Determine variable type.
        Type variableType = vd.withType(lvds.type);

        // Ignore "lvds.modifiers.annotations".

        return (vd.localVariable = new LocalVariable(
            lvds.isFinal(),            // finaL
            this.getType(variableType) // type
        ));
    }

    public int
    getTargetVersion() {

        if (this.targetVersion == -1) {
            this.targetVersion = UnitCompiler.getDefaultTargetVersion();
        }

        return this.targetVersion;
    }

    /**
     * @return The target version that applies iff no target version is actively configured with {@link
     *         #setTargetVersion(int)}
     */
    public static int
    getDefaultTargetVersion() {

        if (UnitCompiler.defaultTargetVersion != -1) return UnitCompiler.defaultTargetVersion;

//        // System property               Description                                    Example values
//        // ------------------------------------------------------------------------------------------------
//        // java.vm.specification.version Java Virtual Machine specification version     "1.8", "11"
//        // java.specification.version    Java Runtime Environment specification version "1.8", "11"
//        // java.version                  Java Runtime Environment version               "1.8.0_45", "11-ea"
//        // java.class.version            Java class format version number               "52.0", "55.0"
//        String jsv = System.getProperty("java.specification.version");
//        jsv = jsv.substring(jsv.indexOf('.') + 1);
//        this.targetVersion = Integer.parseInt(jsv);

        // Because the generation of the StackMapTable attribute is still experimental, we still produce
        // only Java 6 .class files by default:
        return 6;
    }

    private LocalVariableSlot
    allocateLocalVariableSlot(IType localVariableType, @Nullable String localVariableName) {

        LocalVariableSlot result = this.getCodeContext().allocateLocalVariable(
            Descriptor.size(UnitCompiler.rawTypeOf(localVariableType).getDescriptor()),
            localVariableName,
            localVariableType
        );

        return result;
    }

    private LocalVariableSlot
    allocateLocalVariableSlotAndMarkAsInitialized(IType localVariableType, @Nullable String localVariableName) {
        LocalVariableSlot result = this.allocateLocalVariableSlot(localVariableType, localVariableName);
        this.bytecodeEmitter.updateLocalVariableInCurrentStackMap(result.getSlotIndex(), this.bytecodeEmitter.verificationTypeInfo(localVariableType));
        return result;
    }


    /**
     * Adds the given <var>localVars</var> to the 'local variable map' of the given <var>catchClause</var>.
     */
    protected void
    buildLocalVariableMap(CatchClause catchClause, Map<String, LocalVariable> localVars) throws CompileException {
        Map<String, LocalVariable> vars = new HashMap<>();
        vars.putAll(localVars);
        LocalVariable lv = this.getLocalVariable(catchClause.catchParameter);
        vars.put(catchClause.catchParameter.name, lv);
        this.statementCompiler.buildLocalVariableMap(catchClause.body, vars);
    }

    /**
     * @return The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(FormalParameter parameter) throws CompileException {
        return this.getLocalVariable(parameter, false);
    }

    /**
     * @param isVariableArityParameter Whether the <var>parameter</var> is the last parameter of a 'variable arity'
     *                                 (a.k.a. 'varargs') method declaration
     * @return                         The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException {

        if (parameter.localVariable != null) return parameter.localVariable;

        assert parameter.type != null;
        IType parameterType = this.getType(parameter.type);
        if (isVariableArityParameter) {
            parameterType = this.arrayType(parameterType);
        }

        return (parameter.localVariable = new LocalVariable(parameter.isFinal(), parameterType));
    }

    /**
     * @return The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(CatchParameter parameter) throws CompileException {

        if (parameter.localVariable != null) return parameter.localVariable;

        List<IClass> types = new ArrayList<>(parameter.types.length);
        for (Type t : parameter.types) types.add(this.getRawType(t));

        // The declared type of a multi-CATCH parameter is the least upper bound of its alternatives (JLS7
        // 14.20); a multi-CATCH parameter is implicitly FINAL (JLS8 14.20), i.e. it cannot be assigned.
        // Precise rethrow of the alternatives is implemented in "compile2(ThrowStatement)".
        LocalVariable result = new LocalVariable(
            parameter.finaL || parameter.types.length > 1,
            this.lub(types)
        );
        return (parameter.localVariable = result);
    }

    /**
     * Computes the least upper bound of the given types, i.e. the most specific type that all of them are
     * assignable to.
     * <p>
     *   This implementation considers superclasses only, i.e. the result for interfaces which the types
     *   <em>additionally</em> have in common is not represented in the result (JANINO does not have intersection
     *   types); use casts to access members declared by such common interfaces.
     * </p>
     */
    private IClass
    lub(List<IClass> types) throws CompileException {

        IClass result = types.get(0);
        for (int i = 1; i < types.size(); i++) result = UnitCompiler.this.lub2(result, types.get(i));
        return result;
    }

    /**
     * @return The most specific common superclass of <var>a</var> and <var>b</var>
     */
    private IClass
    lub2(IClass a, IClass b) throws CompileException {

        if (a.isAssignableFrom(b)) return a;
        if (b.isAssignableFrom(a)) return b;

        Set<IClass> ancestorsOfA = new HashSet<>();
        for (IClass t = a; t != null; t = t.getSuperclass()) ancestorsOfA.add(t);

        for (IClass t = b; t != null; t = t.getSuperclass()) {
            if (ancestorsOfA.contains(t)) return t;
        }

        return this.iClassLoader.TYPE_java_lang_Object;
    }

    // ------------------ Rvalue.compile() ----------------

    /**
     * Called to check whether the given {@link Rvalue} compiles or not.
     */
    private void
    fakeCompile(Rvalue rv) throws CompileException {

        final Offset from = this.getCodeContext().newOffset();
        StackMap savedStackMap = this.getCodeContext().currentInserter().getStackMap();

        this.compileContext(rv);
        this.compileGet(rv);

        Offset to = this.getCodeContext().newOffset();

        this.getCodeContext().removeCode(from, to.next);
        this.getCodeContext().currentInserter().setStackMap(savedStackMap);
    }

    /**
     * Some {@link Rvalue}s compile more efficiently when their value is not needed, e.g. "i++".
     */
    private void
    compile(Rvalue rv) throws CompileException {

        rv.accept(new RvalueVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitLvalue(Lvalue lv) throws CompileException {
                lv.accept(new LvalueVisitor<Void, CompileException>() {
                    @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compile2(an);    return null; }
                    @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compile2(aae);   return null; }
                    @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compile2(fa);    return null; }
                    @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compile2(fae);   return null; }
                    @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compile2(scfae); return null; }
                    @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { UnitCompiler.this.compile2(lva);   return null; }
                    @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compile2(pe);    return null; }
                });
                return null;
            }

            @Override @Nullable public Void visitArrayLength(ArrayLength al)                                    throws CompileException { UnitCompiler.this.compile2(al);    return null; }
            @Override @Nullable public Void visitAssignment(Assignment a)                                       throws CompileException { UnitCompiler.this.compile2(a);     return null; }
            @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                              throws CompileException { UnitCompiler.this.compile2(uo);    return null; }
            @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                            throws CompileException { UnitCompiler.this.compile2(bo);    return null; }
            @Override @Nullable public Void visitCast(Cast c)                                                   throws CompileException { UnitCompiler.this.compile2(c);     return null; }
            @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                                  throws CompileException { UnitCompiler.this.compile2(cl);    return null; }
            @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)                throws CompileException { UnitCompiler.this.compile2(ce);    return null; }
            @Override @Nullable public Void visitCrement(Crement c)                                             throws CompileException { UnitCompiler.this.compile2(c);     return null; }
            @Override @Nullable public Void visitInstanceof(Instanceof io)                                      throws CompileException { UnitCompiler.this.compile2(io);    return null; }
            @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                          throws CompileException { UnitCompiler.this.compile2(mi);    return null; }
            @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { UnitCompiler.this.compile2(smi);   return null; }
            @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { UnitCompiler.this.compile2(il);    return null; }
            @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { UnitCompiler.this.compile2(fpl);   return null; }
            @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { UnitCompiler.this.compile2(bl);    return null; }
            @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { UnitCompiler.this.compile2(cl);    return null; }
            @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                                throws CompileException { UnitCompiler.this.compile2(sl);    return null; }
            @Override @Nullable public Void visitTextBlock(TextBlock tb)                                        throws CompileException { UnitCompiler.this.compile2(tb);    return null; }
            @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                    throws CompileException { UnitCompiler.this.compile2(nl);    return null; }
            @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                              throws CompileException { UnitCompiler.this.compile2(sl);    return null; }
            @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { UnitCompiler.this.compile2(naci);  return null; }
            @Override @Nullable public Void visitNewArray(NewArray na)                                          throws CompileException { UnitCompiler.this.compile2(na);    return null; }
            @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { UnitCompiler.this.compile2(nia);   return null; }
            @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                         throws CompileException { UnitCompiler.this.compile2(nci);   return null; }
            @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                            throws CompileException { UnitCompiler.this.compile2(pa);    return null; }
            @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { UnitCompiler.this.compile2(qtr);   return null; }
            @Override @Nullable public Void visitThisReference(ThisReference tr)                                throws CompileException { UnitCompiler.this.compile2(tr);    return null; }
            @Override @Nullable public Void visitLambdaExpression(LambdaExpression le)                          throws CompileException { UnitCompiler.this.compile2(le);    return null; }
            @Override @Nullable public Void visitMethodReference(MethodReference mr)                            throws CompileException { UnitCompiler.this.compile2(mr);    return null; }
            @Override @Nullable public Void visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { UnitCompiler.this.compile2(cicr);  return null; }
            @Override @Nullable public Void visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { UnitCompiler.this.compile2(acr);   return null; }
        });
    }

    private void
    compile2(Rvalue rv) throws CompileException {
        this.bytecodeEmitter.pop(rv, this.compileGetValue(rv));
    }

    private void compile2(MethodInvocation invocation) throws CompileException {
        this.bytecodeEmitter.pop(invocation, this.compileGetInvocation(invocation, !this.expressionLambdaResults.contains(invocation)));
    }

    private void compile2(SuperclassMethodInvocation invocation) throws CompileException {
        this.bytecodeEmitter.pop(invocation, this.compileSuperclassInvocation(invocation, !this.expressionLambdaResults.contains(invocation)));
    }

    private void
    compile2(Assignment a) throws CompileException {

        // "Simple" assignment ("=")?
        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileContext(a.lhs);
            this.setLambdaTargetType(a.rhs, this.getType(a.lhs));
            this.assignmentConversion(
                a,                           // locatable
                this.compileGetValue(a.rhs), // sourceType
                this.getType(a.lhs),         // targetType
                this.constantEvaluator.getConstantValue(a.rhs) // constantValue
            );
            this.compileSet(a.lhs);
            return;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".

        // Compile LHS context.
        int lhsCs = this.compileContext(a.lhs);
        // Duplicate the LHS context.
        this.bytecodeEmitter.dupn(a.lhs, lhsCs);
        // Convert RHS value to LHS type (JLS7 15.26.2).
        IType lhsType    = this.compileGet(a.lhs);
        IType resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), /* <= IMPORTANT!
            */
            a.rhs                 // rhs
        );
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
            && !this.tryBoxingConversion(a, resultType, lhsType) // Java 5
        ) this.compileError("Operand types unsuitable for \"" + a.operator + "\"", a.getLocation());
        // Assign converted RHS value to LHS.
        this.compileSet(a.lhs);
    }

    private void
    compile2(Crement c) throws CompileException {

        // Optimized crement of "int" local variable.
        {
            LocalVariable lv = this.isIntLv(c);
            if (lv != null) {
                this.bytecodeEmitter.iinc(c, lv, c.operator);
                return;
            }
        }

        // Compile operand context.
        int operandCs = this.compileContext(c.operand);
        // DUP operand context.
        this.bytecodeEmitter.dupn(c, operandCs);
        // Get operand value.
        IType type = this.compileGet(c.operand);

        {

            // Apply "unary numeric promotion".
            IClass promotedType = this.unaryNumericPromotion(c, type);
            // Crement.
            this.consT(c, promotedType, 1);
            if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.bytecodeEmitter.add(c);
            } else
            if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.bytecodeEmitter.sub(c);
            } else {
                this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
            }
            this.reverseUnaryNumericPromotion(c, promotedType, type);
        }

        // Set operand.
        this.compileSet(c.operand);
    }

    private void
    compile2(ParenthesizedExpression pe) throws CompileException {
        this.compile(pe.value);
    }

    private boolean
    compile2(AlternateConstructorInvocation aci) throws CompileException {
        ConstructorDeclarator declaringConstructor = (ConstructorDeclarator) aci.getEnclosingScope();
        IClass                declaringIClass      = this.resolve(declaringConstructor.getDeclaringClass());

        this.bytecodeEmitter.load(aci, declaringIClass, 0);
        if (declaringIClass.getOuterIClass() != null) this.bytecodeEmitter.load(aci, declaringIClass.getOuterIClass(), 1);
        this.invokeConstructor(
            aci,                  // locatable
            declaringConstructor, // scope
            (Rvalue) null,        // enclosingInstance
            declaringIClass,      // targetClass
            aci.arguments         // arguments
        );
        return true;
    }

    private boolean
    compile2(SuperConstructorInvocation sci) throws CompileException {
        ConstructorDeclarator    declaringConstructor = (ConstructorDeclarator) sci.getEnclosingScope();
        AbstractClassDeclaration declaringClass       = declaringConstructor.getDeclaringClass();
        IClass                   declaringIClass      = this.resolve(declaringClass);
        IClass                   superclass           = declaringIClass.getSuperclass();

        this.bytecodeEmitter.load(sci, declaringIClass, 0);

//        // Fix up the operand stack entry: The loaded object is still unitializied!
//        this.getCodeContext().popObjectOperand();
//        this.getCodeContext().pushUninitializedThisOperand();

        if (superclass == null) throw new CompileException("Class has no superclass", sci.getLocation());

        Rvalue enclosingInstance;
        if (sci.qualification != null) {
            enclosingInstance = sci.qualification;
        } else {
            IClass outerIClassOfSuperclass = superclass.getOuterIClass();
            if (outerIClassOfSuperclass == null) {
                enclosingInstance = null;
            } else {
                enclosingInstance = new QualifiedThisReference(
                    sci.getLocation(),                                         // location
                    new SimpleType(sci.getLocation(), outerIClassOfSuperclass) // qualification
                );
                enclosingInstance.setEnclosingScope(sci);
            }
        }
        this.invokeConstructor(
            sci,                  // locatable
            declaringConstructor, // scope
            enclosingInstance,    // enclosingInstance
            superclass,           // targetClass
            sci.arguments         // arguments
        );
        return true;
    }

    /**
     * Compiles an {@link Rvalue} and branches, depending on the value.
     * <p>
     *   Many {@link Rvalue}s compile more efficiently when their value is the condition for a branch, thus
     *   this method generally produces more efficient bytecode than {@link #compile(Rvalue)} followed by {@link
     *   #branch(Locatable, int, Offset)}.
     * </p>
     * <p>
     *   Notice that if <var>rv</var> is a constant, then either <var>dst</var> is never branched to, or it is
     *   unconditionally branched to; "Unexamined code" errors may result during bytecode validation.
     * </p>
     *
     * @param rv          The value that determines whether to branch or not
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean(Rvalue rv, final CodeContext.Offset dst, final boolean orientation) throws CompileException {

        rv.accept(new RvalueVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitLvalue(Lvalue lv) throws CompileException {
                lv.accept(new LvalueVisitor<Void, CompileException>() {
                    @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compileBoolean2(an,    dst, orientation); return null; }
                    @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compileBoolean2(aae,   dst, orientation); return null; }
                    @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compileBoolean2(fa,    dst, orientation); return null; }
                    @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compileBoolean2(fae,   dst, orientation); return null; }
                    @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compileBoolean2(scfae, dst, orientation); return null; }
                    @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { UnitCompiler.this.compileBoolean2(lva,   dst, orientation); return null; }
                    @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compileBoolean2(pe,    dst, orientation); return null; }
                });
                return null;
            }

            @Override @Nullable public Void visitArrayLength(ArrayLength al)                                    throws CompileException { UnitCompiler.this.compileBoolean2(al,   dst, orientation); return null; }
            @Override @Nullable public Void visitAssignment(Assignment a)                                       throws CompileException { UnitCompiler.this.compileBoolean2(a,    dst, orientation); return null; }
            @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                              throws CompileException { UnitCompiler.this.compileBoolean2(uo,   dst, orientation); return null; }
            @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                            throws CompileException { UnitCompiler.this.compileBoolean2(bo,   dst, orientation); return null; }
            @Override @Nullable public Void visitCast(Cast c)                                                   throws CompileException { UnitCompiler.this.compileBoolean2(c,    dst, orientation); return null; }
            @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                                  throws CompileException { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); return null; }
            @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)                throws CompileException { UnitCompiler.this.compileBoolean2(ce,   dst, orientation); return null; }
            @Override @Nullable public Void visitCrement(Crement c)                                             throws CompileException { UnitCompiler.this.compileBoolean2(c,    dst, orientation); return null; }
            @Override @Nullable public Void visitInstanceof(Instanceof io)                                      throws CompileException { UnitCompiler.this.compileBoolean2(io,   dst, orientation); return null; }
            @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                          throws CompileException { UnitCompiler.this.compileBoolean2(mi,   dst, orientation); return null; }
            @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { UnitCompiler.this.compileBoolean2(smi,  dst, orientation); return null; }
            @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { UnitCompiler.this.compileBoolean2(il,   dst, orientation); return null; }
            @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { UnitCompiler.this.compileBoolean2(fpl,  dst, orientation); return null; }
            @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { UnitCompiler.this.compileBoolean2(bl,   dst, orientation); return null; }
            @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); return null; }
            @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                                throws CompileException { UnitCompiler.this.compileBoolean2(sl,   dst, orientation); return null; }
            @Override @Nullable public Void visitTextBlock(TextBlock tb)                                        throws CompileException { UnitCompiler.this.compileBoolean2(tb,   dst, orientation); return null; }
            @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                    throws CompileException { UnitCompiler.this.compileBoolean2(nl,   dst, orientation); return null; }
            @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                              throws CompileException { UnitCompiler.this.compileBoolean2(sl,   dst, orientation); return null; }
            @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { UnitCompiler.this.compileBoolean2(naci, dst, orientation); return null; }
            @Override @Nullable public Void visitNewArray(NewArray na)                                          throws CompileException { UnitCompiler.this.compileBoolean2(na,   dst, orientation); return null; }
            @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { UnitCompiler.this.compileBoolean2(nia,  dst, orientation); return null; }
            @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                         throws CompileException { UnitCompiler.this.compileBoolean2(nci,  dst, orientation); return null; }
            @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                            throws CompileException { UnitCompiler.this.compileBoolean2(pa,   dst, orientation); return null; }
            @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { UnitCompiler.this.compileBoolean2(qtr,  dst, orientation); return null; }
            @Override @Nullable public Void visitThisReference(ThisReference tr)                                throws CompileException { UnitCompiler.this.compileBoolean2(tr,   dst, orientation); return null; }
            @Override @Nullable public Void visitLambdaExpression(LambdaExpression le)                          throws CompileException { UnitCompiler.this.compileBoolean2(le,   dst, orientation); return null; }
            @Override @Nullable public Void visitMethodReference(MethodReference mr)                            throws CompileException { UnitCompiler.this.compileBoolean2(mr,   dst, orientation); return null; }
            @Override @Nullable public Void visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { UnitCompiler.this.compileBoolean2(cicr, dst, orientation); return null; }
            @Override @Nullable public Void visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { UnitCompiler.this.compileBoolean2(acr,  dst, orientation); return null; }
        });
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(Rvalue rv, CodeContext.Offset dst, boolean orientation) throws CompileException {
        IType        type = this.compileGetValue(rv);
        IClassLoader icl  = this.iClassLoader;
        if (type == icl.TYPE_java_lang_Boolean) {
            this.unboxingConversion(rv, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
        } else
        if (type != IClass.BOOLEAN) {
            this.compileError("Not a boolean expression", rv.getLocation());
        }
        this.bytecodeEmitter.ifxx(rv, orientation == UnitCompiler.JUMP_IF_TRUE ? UnitCompilerBytecodeEmitter.NE : UnitCompilerBytecodeEmitter.EQ, dst);
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(UnaryOperation ue, CodeContext.Offset dst, boolean orientation) throws CompileException {
        if (ue.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileBoolean(ue.operand, dst, !orientation);
            return;
        }

        this.compileError("Boolean expression expected", ue.getLocation());
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(BinaryOperation bo, CodeContext.Offset dst, boolean orientation) throws CompileException {

        if (bo.operator == "|" || bo.operator == "^" || bo.operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileBoolean2((Rvalue) bo, dst, orientation);
            return;
        }

        if (bo.operator == "||" || bo.operator == "&&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsCv = this.constantEvaluator.getConstantValue(bo.lhs);
            if (lhsCv instanceof Boolean) {
                if (((Boolean) lhsCv).booleanValue() ^ bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    // "true && a", "false || a"
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        orientation
                    );
                } else {
                    // "false && a", "true || a"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        orientation
                    );
                    this.fakeCompile(bo.rhs);
                }
                return;
            }
            Object rhsCv = this.constantEvaluator.getConstantValue(bo.rhs);
            if (rhsCv instanceof Boolean) {
                if (((Boolean) rhsCv).booleanValue() ^ bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    // "a && true", "a || false"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        orientation
                    );
                } else {
                    // "a && false", "a || true"

                    // Compile the LHS ("a"), and discard the result.
                    this.bytecodeEmitter.pop(bo.lhs, this.compileGetValue(bo.lhs));

                    // Compile the RHS and branch conditionally (although the RHS is a constant). This prevents
                    // trouble with "unreachable code".
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        orientation
                    );
                }
                return;
            }

            // SUPPRESS CHECKSTYLE StringLiteralEquality
            if (bo.operator == "||" ^ orientation == UnitCompiler.JUMP_IF_FALSE) {
                this.compileBoolean(bo.lhs, dst, orientation);
                this.compileBoolean(bo.rhs, dst, orientation);
            } else {
                CodeContext.Offset end = this.getCodeContext().new BasicBlock();
                this.compileBoolean(bo.lhs, end, !orientation);
                this.compileBoolean(bo.rhs, dst, orientation);
                end.set();
            }
            return;
        }

        COMPARISON:
        {
            final int opIdx = (
                bo.operator == "==" ? UnitCompilerBytecodeEmitter.EQ : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "!=" ? UnitCompilerBytecodeEmitter.NE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "<"  ? UnitCompilerBytecodeEmitter.LT : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == ">=" ? UnitCompilerBytecodeEmitter.GE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == ">"  ? UnitCompilerBytecodeEmitter.GT : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "<=" ? UnitCompilerBytecodeEmitter.LE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                Integer.MIN_VALUE
            );
            if (opIdx == Integer.MIN_VALUE) break COMPARISON;

            // Comparison with "null".
            {
                boolean lhsIsNull = this.constantEvaluator.getConstantValue(bo.lhs) == null;
                boolean rhsIsNull = this.constantEvaluator.getConstantValue(bo.rhs) == null;

                if (lhsIsNull || rhsIsNull) {
                    if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                        this.compileError(
                            "Operator \"" + bo.operator + "\" not allowed on operand \"null\"",
                            bo.getLocation()
                        );
                    }

                    if (!lhsIsNull) {

                        // x == null
                        IType lhsType = this.getType(bo.lhs);
                        if (UnitCompiler.rawTypeOf(lhsType).isPrimitive()) {
                            this.compileError(
                                "Cannot compare primitive type \"" + lhsType.toString() + "\" with \"null\"",
                                bo.getLocation()
                            );
                        }
                        this.compileGetReferenceValue(bo.lhs);
                    } else
                    if (!rhsIsNull) {

                        // null == x
                        IType rhsType = this.getType(bo.rhs);
                        if (UnitCompiler.rawTypeOf(rhsType).isPrimitive()) {
                            this.compileError(
                                "Cannot compare \"null\" with primitive type \"" + rhsType.toString() + "\"",
                                bo.getLocation()
                            );
                        }
                        this.compileGetReferenceValue(bo.rhs);
                    } else
                    {

                        // null == null
                        this.consT(bo, (Object) null);
                    }

                    switch (orientation == UnitCompiler.JUMP_IF_FALSE ? opIdx ^ 1 : opIdx) {

                    case UnitCompilerBytecodeEmitter.EQ:
                        this.bytecodeEmitter.ifnull(bo, dst);
                        break;

                    case UnitCompilerBytecodeEmitter.NE:
                        this.bytecodeEmitter.ifnonnull(bo, dst);
                        break;

                    default:
                        throw new AssertionError(opIdx);
                    }
                    return;
                }
            }

            IType lhsType = this.getType(bo.lhs);
            IType rhsType = this.getType(bo.rhs);

            // Validate using source types; reference comparison consumes erased references, without narrowing them.
            if ((bo.operator == "==" || bo.operator == "!=") // SUPPRESS CHECKSTYLE StringLiteralEquality
                && !UnitCompiler.isPrimitive(lhsType) && !UnitCompiler.isPrimitive(rhsType)) {
                if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError("Operator \"" + bo.operator + "\" not allowed on reference operands", bo.getLocation());
                }
                if (!this.isCastReferenceConvertible(lhsType, rhsType) || !this.isCastReferenceConvertible(rhsType, lhsType)) {
                    this.compileError("Incomparable types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());
                }
                this.compileGetReferenceValue(bo.lhs);
                this.compileGetReferenceValue(bo.rhs);
                this.bytecodeEmitter.if_acmpxx(bo, orientation == UnitCompiler.JUMP_IF_FALSE ? opIdx ^ 1 : opIdx, dst);
                return;
            }

            this.compileGetValue(bo.lhs);

            // 15.20.1 Numerical comparison.
            if (
                UnitCompiler.rawTypeOf(this.getUnboxedType(lhsType)).isPrimitiveNumeric()
                && UnitCompiler.rawTypeOf(this.getUnboxedType(rhsType)).isPrimitiveNumeric()
                && !(
                    (bo.operator == "==" || bo.operator == "!=") // SUPPRESS CHECKSTYLE StringLiteralEquality
                    && !UnitCompiler.rawTypeOf(lhsType).isPrimitive()
                    && !UnitCompiler.rawTypeOf(rhsType).isPrimitive()
                )
            ) {

                IClass
                promotedType = this.binaryNumericPromotionType(bo, this.getUnboxedType(lhsType), this.getUnboxedType(rhsType));

                this.numericPromotion(bo.lhs, this.convertToPrimitiveNumericType(bo.lhs, lhsType), promotedType);
                this.compileGetValue(bo.rhs);
                this.numericPromotion(bo.rhs, this.convertToPrimitiveNumericType(bo.rhs, rhsType), promotedType);

                this.ifNumeric(bo, opIdx, dst, orientation);
                return;
            }

            // JLS7 15.21.2 Boolean Equality Operators == and !=.
            if (
                (lhsType == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                || (rhsType == IClass.BOOLEAN && this.getUnboxedType(lhsType) == IClass.BOOLEAN)
            ) {
                if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError(
                        "Operator \"" + bo.operator + "\" not allowed on boolean operands",
                        bo.getLocation()
                    );
                }
                IClassLoader icl = this.iClassLoader;

                // Unbox LHS if necessary.
                if (lhsType == icl.TYPE_java_lang_Boolean) {
                    this.unboxingConversion(bo, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                }

                this.compileGetValue(bo.rhs);

                // Unbox RHS if necessary.
                if (rhsType == icl.TYPE_java_lang_Boolean) {
                    this.unboxingConversion(bo, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                }

                this.bytecodeEmitter.if_icmpxx(bo, orientation == UnitCompiler.JUMP_IF_FALSE ? opIdx ^ 1 : opIdx, dst);
                return;
            }

            this.compileError("Cannot compare types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());
        }

        this.compileError("Boolean expression expected", bo.getLocation());
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(ParenthesizedExpression pe, CodeContext.Offset dst, boolean orientation) throws CompileException {
        this.compileBoolean(pe.value, dst, orientation);
    }

    /**
     * Generates code that determines the context of the {@link Rvalue} and puts it on the operand stack. Most
     * expressions do not have a "context", but some do. E.g. for "x[y]", the context is "x, y". The bottom line is
     * that for statements like "x[y] += 3" the context is only evaluated once.
     *
     * @return The number of operands that is pushed on the operand stack (0, 1 or 2)
     */
    private int
    compileContext(Rvalue rv) throws CompileException {

        Integer result = (Integer) rv.accept(new RvalueVisitor<Integer, CompileException>() {

            @Override @Nullable public Integer
            visitLvalue(Lvalue lv) throws CompileException {
                return (Integer) lv.accept(new LvalueVisitor<Integer, CompileException>() {
                    @Override public Integer visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.compileContext2(an);    }
                    @Override public Integer visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.compileContext2(aae);   }
                    @Override public Integer visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.compileContext2(fa);    }
                    @Override public Integer visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.compileContext2(fae);   }
                    @Override public Integer visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.compileContext2(scfae); }
                    @Override public Integer visitLocalVariableAccess(LocalVariableAccess lva)                                                   { return UnitCompiler.this.compileContext2(lva);   }
                    @Override public Integer visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.compileContext2(pe);    }
                });
            }

            @Override public Integer visitArrayLength(ArrayLength al)            throws CompileException { return UnitCompiler.this.compileContext2(al);   }
            @Override public Integer visitAssignment(Assignment a)                                       { return UnitCompiler.this.compileContext2(a);    }
            @Override public Integer visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.this.compileContext2(uo);   }
            @Override public Integer visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.this.compileContext2(bo);   }
            @Override public Integer visitCast(Cast c)                                                   { return UnitCompiler.this.compileContext2(c);    }
            @Override public Integer visitClassLiteral(ClassLiteral cl)                                  { return UnitCompiler.this.compileContext2(cl);   }
            @Override public Integer visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.this.compileContext2(ce);   }
            @Override public Integer visitCrement(Crement c)                                             { return UnitCompiler.this.compileContext2(c);    }
            @Override public Integer visitInstanceof(Instanceof io)                                      { return UnitCompiler.this.compileContext2(io);   }
            @Override public Integer visitMethodInvocation(MethodInvocation mi)                          { return UnitCompiler.this.compileContext2(mi);   }
            @Override public Integer visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return UnitCompiler.this.compileContext2(smi);  }
            @Override public Integer visitIntegerLiteral(IntegerLiteral il)                              { return UnitCompiler.this.compileContext2(il);   }
            @Override public Integer visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return UnitCompiler.this.compileContext2(fpl);  }
            @Override public Integer visitBooleanLiteral(BooleanLiteral bl)                              { return UnitCompiler.this.compileContext2(bl);   }
            @Override public Integer visitCharacterLiteral(CharacterLiteral cl)                          { return UnitCompiler.this.compileContext2(cl);   }
            @Override public Integer visitStringLiteral(StringLiteral sl)                                { return UnitCompiler.this.compileContext2(sl);   }
            @Override public Integer visitTextBlock(TextBlock tb)                                        { return UnitCompiler.this.compileContext2(tb);   }
            @Override public Integer visitNullLiteral(NullLiteral nl)                                    { return UnitCompiler.this.compileContext2(nl);   }
            @Override public Integer visitSimpleConstant(SimpleConstant sl)                              { return UnitCompiler.this.compileContext2(sl);   }
            @Override public Integer visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return UnitCompiler.this.compileContext2(naci); }
            @Override public Integer visitNewArray(NewArray na)                                          { return UnitCompiler.this.compileContext2(na);   }
            @Override public Integer visitNewInitializedArray(NewInitializedArray nia)                   { return UnitCompiler.this.compileContext2(nia);  }
            @Override public Integer visitNewClassInstance(NewClassInstance nci)                         { return UnitCompiler.this.compileContext2(nci);  }
            @Override public Integer visitParameterAccess(ParameterAccess pa)                            { return UnitCompiler.this.compileContext2(pa);   }
            @Override public Integer visitQualifiedThisReference(QualifiedThisReference qtr)             { return UnitCompiler.this.compileContext2(qtr);  }
            @Override public Integer visitThisReference(ThisReference tr)                                { return UnitCompiler.this.compileContext2(tr);   }
            @Override public Integer visitLambdaExpression(LambdaExpression le)                          { return UnitCompiler.this.compileContext2(le);   }
            @Override public Integer visitMethodReference(MethodReference mr)                            { return UnitCompiler.this.compileContext2(mr);   }
            @Override public Integer visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return UnitCompiler.this.compileContext2(cicr); }
            @Override public Integer visitArrayCreationReference(ArrayCreationReference acr)             { return UnitCompiler.this.compileContext2(acr);  }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private int
    compileContext2(Rvalue rv) { return 0; }

    private int
    compileContext2(AmbiguousName an) throws CompileException {
        return this.compileContext(this.toRvalueOrCompileException(this.reclassify(an)));
    }

    private int
    compileContext2(FieldAccess fa) throws CompileException {
        if (this.capturedConstructorParameter(fa) != null) return 0;
        if (fa.field.isStatic()) {
            Rvalue rv = fa.lhs.toRvalue();
            if (rv != null) {
                this.warning(
                    "CNSFA",
                    "Left-hand side of static field access should be a type, not an rvalue",
                    fa.lhs.getLocation()
                );
                // JLS7 15.11.1.3.1.1:
                this.bytecodeEmitter.pop(fa.lhs, this.compileGetValue(rv));
            }
            return 0;
        } else {
            this.compileGetValue(this.toRvalueOrCompileException(fa.lhs));
            return 1;
        }
    }

    private int
    compileContext2(ArrayLength al) throws CompileException {
        if (!UnitCompiler.rawTypeOf(this.compileGetValue(al.lhs)).isArray()) {
            this.compileError("Cannot determine length of non-array type", al.getLocation());
        }
        return 1;
    }

    /**
     * Array access expression; see JLS7 15.13 / JLS8+ 15.10.3.
     */
    private int
    compileContext2(ArrayAccessExpression aae) throws CompileException {

        IType lhsType = this.compileGetValue(aae.lhs);
        if (!UnitCompiler.rawTypeOf(lhsType).isArray()) {
            this.compileError(
                "Subscript not allowed on non-array type \"" + lhsType.toString() + "\"",
                aae.getLocation()
            );
        }

        IType indexType = this.compileGetValue(aae.index);
        if (this.unaryNumericPromotion(aae.index, indexType) != IClass.INT) {
            this.compileError(
                "Index expression of type \"" + indexType + "\" cannot be promoted to \"int\"",
                aae.getLocation()
            );
        }

        return 2;
    }

    private int
    compileContext2(FieldAccessExpression fae) throws CompileException {
        return this.compileContext(this.determineValue(fae));
    }

    private int
    compileContext2(SuperclassFieldAccessExpression scfae) throws CompileException {
        return this.compileContext(this.determineValue(scfae));
    }

    private int
    compileContext2(ParenthesizedExpression pe) throws CompileException {
        return this.compileContext(pe.value);
    }

    /**
     * Generates code that determines the value of the {@link Rvalue} and puts it on the operand stack. This method
     * relies on that the "context" of the {@link Rvalue} is on top of the operand stack (see {@link
     * #compileContext(Rvalue)}).
     *
     * @return The type of the {@link Rvalue}
     */
    private IType
    compileGet(Rvalue rv) throws CompileException {

        IType result = (IType) rv.accept(new RvalueVisitor<IType, CompileException>() {

            @Override @Nullable public IType
            visitLvalue(Lvalue lv) throws CompileException {
                return (IType) lv.accept(new LvalueVisitor<IType, CompileException>() {
                    @Override public IType visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.compileGet2(an);    }
                    @Override public IType visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.compileGet2(aae);   }
                    @Override public IType visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.compileGet2(fa);    }
                    @Override public IType visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.compileGet2(fae);   }
                    @Override public IType visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.compileGet2(scfae); }
                    @Override public IType visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { return UnitCompiler.this.compileGet2(lva);   }
                    @Override public IType visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.compileGet2(pe);    }
                });
            }

            @Override public IType visitArrayLength(ArrayLength al)                                                            { return UnitCompiler.this.compileGet2(al);   }
            @Override public IType visitAssignment(Assignment a)                                       throws CompileException { return UnitCompiler.this.compileGet2(a);    }
            @Override public IType visitUnaryOperation(UnaryOperation uo)                              throws CompileException { return UnitCompiler.this.compileGet2(uo);   }
            @Override public IType visitBinaryOperation(BinaryOperation bo)                            throws CompileException { return UnitCompiler.this.compileGet2(bo);   }
            @Override public IType visitCast(Cast c)                                                   throws CompileException { return UnitCompiler.this.compileGet2(c);    }
            @Override public IType visitClassLiteral(ClassLiteral cl)                                  throws CompileException { return UnitCompiler.this.compileGet2(cl);   }
            @Override public IType visitConditionalExpression(ConditionalExpression ce)                throws CompileException { return UnitCompiler.this.compileGet2(ce);   }
            @Override public IType visitCrement(Crement c)                                             throws CompileException { return UnitCompiler.this.compileGet2(c);    }
            @Override public IType visitInstanceof(Instanceof io)                                      throws CompileException { return UnitCompiler.this.compileGet2(io);   }
            @Override public IType visitMethodInvocation(MethodInvocation mi)                          throws CompileException { return UnitCompiler.this.compileGet2(mi);   }
            @Override public IType visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { return UnitCompiler.this.compileGet2(smi);  }
            @Override public IType visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { return UnitCompiler.this.compileGet2(il);   }
            @Override public IType visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { return UnitCompiler.this.compileGet2(fpl);  }
            @Override public IType visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { return UnitCompiler.this.compileGet2(bl);   }
            @Override public IType visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { return UnitCompiler.this.compileGet2(cl);   }
            @Override public IType visitStringLiteral(StringLiteral sl)                                throws CompileException { return UnitCompiler.this.compileGet2(sl);   }
            @Override public IType visitTextBlock(TextBlock tb)                                        throws CompileException { return UnitCompiler.this.compileGet2(tb);   }
            @Override public IType visitNullLiteral(NullLiteral nl)                                    throws CompileException { return UnitCompiler.this.compileGet2(nl);   }
            @Override public IType visitSimpleConstant(SimpleConstant sl)                              throws CompileException { return UnitCompiler.this.compileGet2(sl);   }
            @Override public IType visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { return UnitCompiler.this.compileGet2(naci); }
            @Override public IType visitNewArray(NewArray na)                                          throws CompileException { return UnitCompiler.this.compileGet2(na);   }
            @Override public IType visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { return UnitCompiler.this.compileGet2(nia);  }
            @Override public IType visitNewClassInstance(NewClassInstance nci)                         throws CompileException { return UnitCompiler.this.compileGet2(nci);  }
            @Override public IType visitParameterAccess(ParameterAccess pa)                            throws CompileException { return UnitCompiler.this.compileGet2(pa);   }
            @Override public IType visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { return UnitCompiler.this.compileGet2(qtr);  }
            @Override public IType visitThisReference(ThisReference tr)                                throws CompileException { return UnitCompiler.this.compileGet2(tr);   }
            @Override public IType visitLambdaExpression(LambdaExpression le)                          throws CompileException { return UnitCompiler.this.compileGet2(le);   }
            @Override public IType visitMethodReference(MethodReference mr)                            throws CompileException { return UnitCompiler.this.compileGet2(mr);   }
            @Override public IType visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { return UnitCompiler.this.compileGet2(cicr); }
            @Override public IType visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { return UnitCompiler.this.compileGet2(acr);  }
        });

        assert result != null;
        return result;
    }

    private IClass
    compileGet2(BooleanRvalue brv) throws CompileException {
        CodeContext.Offset isTrue = this.getCodeContext().new BasicBlock();
        isTrue.setStackMap(this.getCodeContext().currentInserter().getStackMap());
        this.compileBoolean(brv, isTrue, UnitCompiler.JUMP_IF_TRUE);
        this.consT(brv, 0);
        CodeContext.Offset end = this.getCodeContext().new BasicBlock();
        this.bytecodeEmitter.gotO(brv, end);
        isTrue.setBasicBlock();
        this.consT(brv, 1);
        end.set();

        return IClass.BOOLEAN;
    }

    private IType
    compileGet2(AmbiguousName an) throws CompileException {
        return this.compileResolvedReference(an, this.toRvalueOrCompileException(this.reclassify(an)));
    }

    private IType
    compileGet2(LocalVariableAccess lva) throws CompileException {
        if (this.statementCompiler.isInitializingResource(lva.localVariable)) {
            throw UnitCompiler.compileException(lva, "Resource variable might not have been initialized");
        }
        return this.bytecodeEmitter.load(lva, lva.localVariable);
    }

    private IType
    compileGet2(FieldAccess fa) throws CompileException {
        LocalVariable capturedParameter = this.capturedConstructorParameter(fa);
        if (capturedParameter != null) return this.bytecodeEmitter.load(fa, capturedParameter);
        this.bytecodeEmitter.getfield(fa, this.fieldReferenceOwner(fa), fa.field.getName(), fa.field.getType(), fa.field.isStatic());

        return this.adaptReferenceResult(fa, this.getType(fa), fa.field.getType(), this.referenceValueTargets.get(fa), false);
    }

    private IClass fieldReferenceOwner(FieldAccess access) throws CompileException {
        IClass owner = access.field.getDeclaringIClass();
        if (access.field.getAccess() == Access.PUBLIC && !this.isAccessible(owner, access.getEnclosingScope())) {
            IClass qualifier = UnitCompiler.rawTypeOf(this.getType(access.lhs));
            if (owner.isAssignableFrom(qualifier) && this.isAccessible(qualifier, access.getEnclosingScope())) return qualifier;
        }
        this.checkAccessible(access.field, access.getEnclosingScope(), access.getLocation());
        return owner;
    }

    @Nullable private LocalVariable
    capturedConstructorParameter(FieldAccess access) throws CompileException {
        if (!access.field.getName().startsWith("val$") || !(access.lhs instanceof QualifiedThisReference)) return null;
        for (Scope scope = access.getEnclosingScope();; scope = scope.getEnclosingScope()) {
            if (scope instanceof ConstructorDeclarator) {
                ConstructorDeclarator constructor = (ConstructorDeclarator) scope;
                if (access.field.getDeclaringIClass() != this.resolve(constructor.getDeclaringClass())) return null;
                // Captured values are constructor arguments. In super(...) / this(...) the
                // receiver is still uninitialized and GETFIELD on it is forbidden by the verifier.
                return (LocalVariable) constructor.syntheticParameters.get(access.field.getName());
            }
            if (scope instanceof FunctionDeclarator || scope instanceof TypeDeclaration) return null;
        }
    }

    private IClass
    compileGet2(ArrayLength al) {
        this.bytecodeEmitter.arraylength(al);
        return IClass.INT;
    }

    private IType
    compileGet2(ThisReference tr) throws CompileException {
        final IClass currentIClass = this.getIClass(tr);
        this.referenceThis(tr, currentIClass);
        return this.getType2(tr);
    }

    private IClass
    compileGet2(LambdaExpression le) throws CompileException {
        if (le.desugared == null) le.desugared = this.desugarLambda(le);
        return this.compileGet2((NewAnonymousClassInstance) le.desugared);
    }

    private IClass
    compileGet2(MethodReference mr) throws CompileException {
        if (mr.desugared == null) mr.desugared = this.desugarMethodReference(mr);
        Atom lhs = mr.lhs instanceof AmbiguousName ? this.reclassify((AmbiguousName) mr.lhs) : mr.lhs;
        if (lhs instanceof LocalVariableAccess) {
            MethodInvocation check = new MethodInvocation(mr.getLocation(), mr.lhs, "getClass", new Rvalue[0]);
            check.setEnclosingScope(mr.getEnclosingScope());
            this.bytecodeEmitter.pop(mr, this.compileGetValue(check));
        }
        if (!this.syntheticReferences.contains(mr)) this.referenceOrigins.put(mr.desugared, mr);
        return UnitCompiler.rawTypeOf(this.compileGetValue(mr.desugared));
    }

    private IClass
    compileGet2(ClassInstanceCreationReference cicr) throws CompileException {
        if (cicr.desugared == null) cicr.desugared = this.desugarConstructorReference(cicr);
        this.referenceOrigins.put(cicr.desugared, cicr);
        return this.compileGet2((NewAnonymousClassInstance) cicr.desugared);
    }

    private IClass
    compileGet2(ArrayCreationReference acr) throws CompileException {
        if (acr.desugared == null) acr.desugared = this.desugarArrayCreationReference(acr);
        this.referenceOrigins.put(acr.desugared, acr);
        return this.compileGet2((NewAnonymousClassInstance) acr.desugared);
    }

    private final Map<Type, IType> referenceTypeBindings = new java.util.IdentityHashMap<>();

    @Nullable private TypeArgument[] copyReferenceArguments(@Nullable TypeArgument[] arguments, Scope scope) throws CompileException {
        TypeArgument[] copy = new org.codehaus.janino.util.DeepCopier().copyOptionalTypeArguments(arguments);
        if (arguments != null) for (int i = 0; i < arguments.length; i++) if (arguments[i] instanceof Type) {
            UnitCompiler.setScopeIfUnset((Type) arguments[i], scope);
            this.referenceTypeBindings.put((Type) copy[i], this.getType((Type) arguments[i]));
        }
        return copy;
    }

    private final Map<Rvalue, Rvalue> referenceOrigins = new java.util.IdentityHashMap<>();
    private final Set<Rvalue> syntheticReferences = Collections.newSetFromMap(new java.util.IdentityHashMap<Rvalue, Boolean>());
    private final Set<Rvalue> referenceDelegates = Collections.newSetFromMap(new java.util.IdentityHashMap<Rvalue, Boolean>());

    private void compileReferenceAnnotations(Rvalue lowered) throws CompileException {
        Rvalue original = this.referenceOrigins.get(lowered);
        if (original == null || this.getTargetVersion() < 8) return;
        Type type = original instanceof ClassInstanceCreationReference ? ((ClassInstanceCreationReference) original).type
            : original instanceof ArrayCreationReference ? ((ArrayCreationReference) original).type
            : ((MethodReference) original).lhs instanceof Type ? (Type) ((MethodReference) original).lhs : null;
        boolean constructor = !(original instanceof MethodReference);
        CodeContext.Offset offset = this.getCodeContext().newOffset();
        if (type != null) this.annotationCompiler.compileCodeTypeAnnotations(type, new Annotation[0],
            (byte) (constructor ? 0x45 : 0x46), this.getCodeContext(), offset, null, 0);
        TypeArgument[] arguments = original instanceof MethodReference ? ((MethodReference) original).typeArguments
            : original instanceof ClassInstanceCreationReference ? ((ClassInstanceCreationReference) original).typeArguments : null;
        if (arguments != null) for (int i = 0; i < arguments.length; i++) if (arguments[i] instanceof Type) {
            this.annotationCompiler.compileCodeTypeAnnotations((Type) arguments[i], new Annotation[0],
                (byte) (constructor ? 0x4a : 0x4b), this.getCodeContext(), offset, null, i);
        }
    }

    // ------------------ Desugaring of LAMBDA expressions and method references ------------------

    private final Map<ConditionalExpression, IType> conditionalTargets = new java.util.IdentityHashMap<>();
    private final Map<Invocation, IType> invocationTargets = new java.util.IdentityHashMap<>();
    private int speculativeInvocationDepth;
    private final Map<Rvalue, IType> referenceValueTargets = new java.util.IdentityHashMap<>();
    private final Set<Rvalue> expressionLambdaResults = Collections.newSetFromMap(new java.util.IdentityHashMap<Rvalue, Boolean>());
    private final Set<Rvalue> erasedReferenceResults = Collections.newSetFromMap(new java.util.IdentityHashMap<Rvalue, Boolean>());
    private final Map<NewClassInstance, IType> creationTargets = new java.util.IdentityHashMap<>();
    private final Set<NewClassInstance> inferredReferenceCreations = Collections.newSetFromMap(new java.util.IdentityHashMap<NewClassInstance, Boolean>());

    private boolean isDiamondCreation(NewClassInstance creation) {
        if (this.inferredReferenceCreations.contains(creation)) return true;
        TypeArgument[] arguments = creation.type instanceof ReferenceType ? ((ReferenceType) creation.type).typeArguments
            : creation.type instanceof RvalueMemberType ? ((RvalueMemberType) creation.type).typeArguments : null;
        return arguments != null && arguments.length == 0;
    }

    private boolean
    isPolyExpression(Rvalue value) {
        if (value instanceof ParenthesizedExpression) return this.isPolyExpression(((ParenthesizedExpression) value).value);
        if (value instanceof ConditionalExpression) {
            return this.isPolyExpression(((ConditionalExpression) value).mhs) || this.isPolyExpression(((ConditionalExpression) value).rhs);
        }
        return value instanceof LambdaExpression || value instanceof MethodReference
            || value instanceof ClassInstanceCreationReference || value instanceof ArrayCreationReference;
    }

    /**
     * Whether the target type of a LAMBDA expression or method reference was set at one of the "target typing"
     * positions (assignment, variable initialization, return statement, cast, method invocation argument).
     */
    private void
    setLambdaTargetType(Rvalue rv, @Nullable IType targetType) {
        this.setLambdaTargetType(rv, targetType, true);
    }

    private void setLambdaTargetType(Rvalue rv, @Nullable IType targetType, boolean adaptFieldResult) {
        if (rv instanceof Lvalue) {
            if (adaptFieldResult && targetType != null) this.referenceValueTargets.put(rv, targetType);
            else this.referenceValueTargets.remove(rv);
        }
        if (rv instanceof NewInitializedArray && ((NewInitializedArray) rv).arrayIClass != null
            && targetType != null && UnitCompiler.isArray(targetType)) {
            // Varargs arrays are allocated with erased component types, but their elements must
            // still be attributed against the instantiated formal (e.g. Predicate<Class<?>>).
            this.initializerElementTargets.put(((NewInitializedArray) rv).arrayInitializer, UnitCompiler.getComponentType(targetType));
        } else
        if (rv instanceof LambdaExpression) {
            ((LambdaExpression) rv).targetIType = targetType;
        } else
        if (rv instanceof MethodReference) {
            ((MethodReference) rv).targetIType = targetType;
        } else
        if (rv instanceof ClassInstanceCreationReference) {
            ((ClassInstanceCreationReference) rv).targetIType = targetType;
        } else
        if (rv instanceof ArrayCreationReference) {
            ((ArrayCreationReference) rv).targetIType = targetType;
        } else
        if (rv instanceof ParenthesizedExpression) {
            this.setLambdaTargetType(((ParenthesizedExpression) rv).value, targetType, adaptFieldResult);
        } else
        if (rv instanceof ConditionalExpression) {
            if (targetType != null) this.conditionalTargets.put((ConditionalExpression) rv, targetType);
            this.setLambdaTargetType(((ConditionalExpression) rv).mhs, targetType, false);
            this.setLambdaTargetType(((ConditionalExpression) rv).rhs, targetType, false);
        } else
        if (rv instanceof NewClassInstance && this.isDiamondCreation((NewClassInstance) rv) && targetType != null) {
            NewClassInstance creation = (NewClassInstance) rv;
            if (!this.inferredReferenceCreations.contains(creation)) {
                IType previous = this.creationTargets.put(creation, targetType);
                if (!targetType.equals(previous)) creation.iType = null;
            }
        } else
        if (rv instanceof Invocation && targetType != null) {
            this.invocationTargets.put((Invocation) rv, targetType);
        }
    }

    private static IClass[]
    subArray(IClass[] a, int from) {
        IClass[] result = new IClass[a.length - from];
        System.arraycopy(a, from, result, 0, result.length);
        return result;
    }

    /**
     * @return Whether <var>a</var> is more specific than <var>b</var>, i.e. whether each of a's parameter types
     *         is method invocation convertible to b's (JLS8 15.12.2.5); e.g. "f(String)" is more specific than
     *         "f(Object)"
     */
    private boolean
    isMoreSpecific(IMethod a, IMethod b, int arity, boolean variableArity) throws CompileException {
        IClass[] aps = variableArity ? this.expandedReferenceParameters(a, arity) : a.getParameterTypes();
        IClass[] bps = variableArity ? this.expandedReferenceParameters(b, arity) : b.getParameterTypes();
        if (aps.length != bps.length) return false;
        for (int i = 0; i < aps.length; i++) {
            if (aps[i] == bps[i]) continue;
            if (!this.isMethodInvocationConvertible(aps[i], bps[i], false)) return false;
        }
        return true;
    }

    private IClass[]
    expandedReferenceParameters(IMethod method, int arity) throws CompileException {
        IClass[] declared = method.getParameterTypes();
        IClass[] expanded = new IClass[Math.max(arity, declared.length)];
        for (int i = 0; i < expanded.length; i++) {
            expanded[i] = i < declared.length - 1 ? declared[i] : declared[declared.length - 1].getComponentType();
        }
        return expanded;
    }

    /**
     * @return The wrapper type for a primitive type, or the type itself if it is a reference type
     */
    IClass
    wrapperOf(IClass c) {
        if (c == IClass.BOOLEAN) return this.iClassLoader.TYPE_java_lang_Boolean;
        if (c == IClass.BYTE)    return this.iClassLoader.TYPE_java_lang_Byte;
        if (c == IClass.CHAR)    return this.iClassLoader.TYPE_java_lang_Character;
        if (c == IClass.SHORT)   return this.iClassLoader.TYPE_java_lang_Short;
        if (c == IClass.INT)     return this.iClassLoader.TYPE_java_lang_Integer;
        if (c == IClass.LONG)    return this.iClassLoader.TYPE_java_lang_Long;
        if (c == IClass.FLOAT)   return this.iClassLoader.TYPE_java_lang_Float;
        if (c == IClass.DOUBLE)  return this.iClassLoader.TYPE_java_lang_Double;
        return c;
    }

    /**
     * Sets the enclosing scope of the given {@link Rvalue} (and, recursively, its subordinate rvalues and types),
     * tolerating an already-set scope.
     */
    static void
    setRvalueScopeIfUnset(Rvalue rv, Scope scope) {
        try { rv.setEnclosingScope(scope); } catch (InternalCompilerException ice) { /* Already set. */ }
    }

    /**
     * Sets the enclosing scope of the given {@link Type}, tolerating an already-set scope.
     */
    static void
    setScopeIfUnset(@Nullable Type t, Scope scope) {
        if (t == null) return;
        try { t.setEnclosingScope(scope); } catch (InternalCompilerException ice) { /* Already set. */ }
    }

    /**
     * Moves a lambda body into a uniquely named method of its lexical enclosing type and creates an
     * anonymous SAM implementation that delegates to that method. Instantiated parameter/return types,
     * permitted checked exceptions and captured generic/final locals survive the move; lexical this,
     * super and enclosing-member lookup retain their original meaning.
     */
    private Rvalue
    desugarLambda(LambdaExpression le) throws CompileException {

        IClass.IMethod sam    = this.getSam(le, le.targetIType);
        IClass[]       samPts = sam.getParameterTypes();
        IClass         samRt  = this.substitutedSamReturnType(le.targetIType, sam, sam.getReturnType());

        // Names and (explicitly given) types of the LAMBDA parameters.
        String[]         names;
        @Nullable Type[] declaredTypes;
        if (le.parameters instanceof IdentifierLambdaParameters) {
            names         = new String[] { ((IdentifierLambdaParameters) le.parameters).identifier };
            declaredTypes = new Type[] { null };
        } else
        if (le.parameters instanceof InferredLambdaParameters) {
            names         = ((InferredLambdaParameters) le.parameters).names;
            declaredTypes = new Type[names.length];
            Arrays.fill(declaredTypes, null);
        } else
        if (le.parameters instanceof FormalLambdaParameters) {
            FormalParameter[] fps = ((FormalLambdaParameters) le.parameters).formalParameters.parameters;
            names         = new String[fps.length];
            declaredTypes = new Type[fps.length];
            for (int i = 0; i < fps.length; i++) {
                names[i]         = fps[i].name;
                declaredTypes[i] = fps[i].type;
            }
        } else
        {
            throw new AssertionError(le.parameters);
        }

        if (names.length != samPts.length) {
            throw UnitCompiler.compileException(le, (
                "LAMBDA expression has " + names.length + " parameters, but the method \"" + sam
                + "\" of the target type has " + samPts.length
            ));
        }

        // Effective parameter types: explicitly declared types, or the SUBSTITUTED SAM parameter types for
        // inferred parameters.
        IType[] effectiveTypes = this.functionalParameters(le.targetIType, sam);
        for (int i = 0; i < names.length; i++) {
            Type declaredAst = declaredTypes[i];
            if (declaredAst != null) {
                UnitCompiler.setScopeIfUnset(declaredAst, le.getEnclosingScope());
                IType declaredType = this.getType(declaredAst);
                effectiveTypes[i] = declaredType;

                // Verify the declared parameter type against the instantiated SAM parameter type.
                IType substituted = this.functionalParameters(le.targetIType, sam)[i];
                if (!UnitCompiler.rawTypeOf(substituted).getDescriptor().equals(UnitCompiler.rawTypeOf(effectiveTypes[i]).getDescriptor())
                    || substituted instanceof ParameterizedIClass && effectiveTypes[i] instanceof ParameterizedIClass && !substituted.equals(effectiveTypes[i])) {
                    throw UnitCompiler.compileException(le, (
                        "Parameter #" + (i + 1) + " of the LAMBDA expression is declared as \""
                        + effectiveTypes[i] + "\", but must be \"" + substituted + "\""
                    ));
                }
            } else {
                effectiveTypes[i] = this.functionalParameters(le.targetIType, sam)[i];
            }
        }

        // Determine the enclosing TYPE declaration and whether the LAMBDA appears in a STATIC context.
        final TypeDeclaration enclosingTypeDeclaration;
        boolean               staticContext;
        {
            TypeBodyDeclaration enclosingTbd = null;
            for (Scope sc = le.getEnclosingScope(); !(sc instanceof TypeBodyDeclaration); sc = sc.getEnclosingScope()) {
                if (sc instanceof TypeDeclaration) break;
            }
            Scope sc;
            for (sc = le.getEnclosingScope(); !(sc instanceof TypeBodyDeclaration) && !(sc instanceof TypeDeclaration); sc = sc.getEnclosingScope());
            if (sc instanceof TypeBodyDeclaration) {
                enclosingTbd = (TypeBodyDeclaration) sc;
                staticContext = UnitCompiler.isStaticContext(enclosingTbd);
                for (; !(sc instanceof TypeDeclaration); sc = sc.getEnclosingScope());
            } else {
                // E.g. a LAMBDA in a field initializer.
                staticContext = true;
                for (Scope s2 = le.getEnclosingScope(); ; s2 = s2.getEnclosingScope()) {
                    // Field initializers are non-static unless the field is static; JANINO does not give us the
                    // field here, so a "this"-using LAMBDA in a non-static field initializer would be rejected;
                    // acceptable for now.
                    if (s2 instanceof TypeBodyDeclaration && UnitCompiler.isStaticContext((TypeBodyDeclaration) s2)) { break; }
                    if (s2 instanceof TypeDeclaration) break;
                }
            }
            enclosingTypeDeclaration = (TypeDeclaration) sc;
        }
        for (Scope scope = le.getEnclosingScope(); scope != enclosingTypeDeclaration; scope = scope.getEnclosingScope()) {
            if (scope instanceof ConstructorInvocation) staticContext = true;
        }

        // Moving the body into a helper must retain enclosing method type variables, including
        // variables that shadow the enclosing class' parameters. Reuse their declarations so
        // synthetic parameter types and names in the original body retain the same identity.
        TypeParameter[] lambdaTypeParameters = null;
        for (Scope scope = le.getEnclosingScope(); scope != enclosingTypeDeclaration; scope = scope.getEnclosingScope()) {
            if (scope instanceof FunctionDeclarator) {
                TypeParameter[] parameters = ((FunctionDeclarator) scope).getOptionalTypeParameters();
                if (parameters != null) {
                    lambdaTypeParameters = new org.codehaus.janino.util.DeepCopier().copyTypeParameters(parameters);
                    for (int i = 0; i < parameters.length; i++) {
                        this.sourceVariables.put(lambdaTypeParameters[i], this.sourceVariable(parameters[i], scope));
                    }
                }
                break;
            }
        }

        // Collect the candidate names used in the body, then determine which of them denote effectively-final
        // LOCAL variables of the enclosing context; these are "captured" and passed to the "lambda$" method.
        final Set<String> candidateNames = new LinkedHashSet<>();
        {
            org.codehaus.janino.util.AbstractTraverser<RuntimeException> collector = (
                new org.codehaus.janino.util.AbstractTraverser<RuntimeException>() {
                    @Override public void traverseBinaryOperation(Java.BinaryOperation operation) throws RuntimeException {
                        this.traverseBinaryOperationIteratively(operation);
                    }

                    @Override public void
                    traverseAmbiguousName(AmbiguousName an) {
                        candidateNames.add(an.identifiers[0]);  // E.g. "x" or the "x" of "x.y".
                        super.traverseAmbiguousName(an);
                    }

                    @Override public void
                    traverseLambdaExpression(LambdaExpression nested) {
                        Set<String> outer = new LinkedHashSet<>(candidateNames);
                        candidateNames.clear();
                        if (nested.body instanceof BlockLambdaBody) {
                            this.traverseBlock(((BlockLambdaBody) nested.body).block);
                        } else {
                            this.dispatchRvalue(((ExpressionLambdaBody) nested.body).expression);
                        }
                        if (nested.parameters instanceof IdentifierLambdaParameters) {
                            candidateNames.remove(((IdentifierLambdaParameters) nested.parameters).identifier);
                        } else if (nested.parameters instanceof InferredLambdaParameters) {
                            candidateNames.removeAll(Arrays.asList(((InferredLambdaParameters) nested.parameters).names));
                        } else {
                            for (FormalParameter fp : ((FormalLambdaParameters) nested.parameters).formalParameters.parameters) {
                                candidateNames.remove(fp.name);
                            }
                        }
                        candidateNames.addAll(outer);
                    }

                    @Override public void
                    traverseMethodReference(MethodReference nested) {
                        if (nested.lhs instanceof Rvalue) this.dispatchRvalue((Rvalue) nested.lhs);
                    }

                }
            );
            if (le.body instanceof BlockLambdaBody) {
                collector.traverseBlock(((BlockLambdaBody) le.body).block);
            } else
            if (le.body instanceof ExpressionLambdaBody) {
                collector.dispatchRvalue(((ExpressionLambdaBody) le.body).expression);
            }
        }
        candidateNames.removeAll(Arrays.asList(names));  // The LAMBDA parameters shadow outer names.

        final Map<String, IType> capturedLocals = new java.util.LinkedHashMap<>();
        {
            Set<String> capturedNames = new HashSet<>();
            for (String cn : candidateNames) {
                if (capturedNames.contains(cn)) continue;
                Atom a = this.reclassifyName(le.getLocation(), le.getEnclosingScope(), cn);
                if (a instanceof LocalVariableAccess) {
                    LocalVariable lv = ((LocalVariableAccess) a).localVariable;
                    if (!lv.finaL && !lv.effectivelyFinal) {
                        throw UnitCompiler.compileException(le, (
                            "Cannot access non-final local variable \"" + cn + "\" from LAMBDA"
                        ));
                    }
                    capturedLocals.put(cn, lv.type);
                    capturedNames.add(cn);
                }
            }
        }

        final Set<String> writtenNames = new HashSet<>();
        org.codehaus.janino.util.AbstractTraverser<RuntimeException> writes = new org.codehaus.janino.util.AbstractTraverser<RuntimeException>() {
            @Override public void traverseBinaryOperation(Java.BinaryOperation operation) throws RuntimeException {
                this.traverseBinaryOperationIteratively(operation);
            }

            @Override public void traverseAssignment(Assignment assignment) {
                if (assignment.lhs instanceof AmbiguousName) writtenNames.add(((AmbiguousName) assignment.lhs).identifiers[0]);
                super.traverseAssignment(assignment);
            }
            @Override public void traverseCrement(Crement crement) {
                if (crement.operand instanceof AmbiguousName) writtenNames.add(((AmbiguousName) crement.operand).identifiers[0]);
                super.traverseCrement(crement);
            }
            @Override public void traverseLambdaExpression(LambdaExpression nested) {}
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration nested) {}
        };
        if (le.body instanceof BlockLambdaBody) writes.traverseBlock(((BlockLambdaBody) le.body).block);
        else writes.dispatchRvalue(((ExpressionLambdaBody) le.body).expression);

        // Declare and add the "lambda$N" method to the ENCLOSING class; the body keeps its ORIGINAL lexical
        // bindings: Unqualified names bind to enclosing members through "this", "this"/"super" are correct.
        List<FormalParameter> lps = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            lps.add(new FormalParameter(le.getLocation(), !writtenNames.contains(names[i]) ? Java.accessModifiers(le.getLocation(), "final")
                : le.parameters instanceof FormalLambdaParameters ? ((FormalLambdaParameters) le.parameters).formalParameters.parameters[i].modifiers : new Modifier[0],
                new SimpleType(le.getLocation(), effectiveTypes[i]), names[i]));
        }
        for (Map.Entry<String, IType> cl : capturedLocals.entrySet()) {
            lps.add(new FormalParameter(le.getLocation(), Java.accessModifiers(le.getLocation(), "final"), new SimpleType(le.getLocation(), cl.getValue()), cl.getKey()));
        }

        List<BlockStatement> lambdaDollarStatements = new ArrayList<>();
        if (le.body instanceof ExpressionLambdaBody) {
            Rvalue expr = ((ExpressionLambdaBody) le.body).expression;
            // javac adapts the expression's own result before adapting it to the SAM return type.
            this.expressionLambdaResults.add(expr);
            if (samRt != IClass.VOID) {
                lambdaDollarStatements.add(new ReturnStatement(le.getLocation(), expr));
            } else
            {
                lambdaDollarStatements.add(new ExpressionStatement(expr));
            }
        } else
        if (le.body instanceof BlockLambdaBody) {
            lambdaDollarStatements.add(((BlockLambdaBody) le.body).block);
        } else
        {
            throw new AssertionError(le.body);
        }

        // Wrap the statements in a BLOCK, so that the parameters are visible to name resolution (the local
        // variable map lives on the method's block).
        {
            Block b = new Block(le.getLocation());
            b.statements.addAll(lambdaDollarStatements);
            for (BlockStatement bs : lambdaDollarStatements) bs.setEnclosingScope(b);
            lambdaDollarStatements = new ArrayList<>();
            lambdaDollarStatements.add(b);
        }

        String lambdaMethodName;
        do { lambdaMethodName = "lambda$" + enclosingTypeDeclaration.getClassName().replace('.', '_') + "$" + (++this.lambdaMethodCount); }
        while (enclosingTypeDeclaration.getMethodDeclaration(lambdaMethodName) != null);
        IClass[] lambdaThrown = sam.getThrownExceptions();
        Type[] lambdaThrows = new Type[lambdaThrown.length];
        for (int i = 0; i < lambdaThrown.length; i++) lambdaThrows[i] = this.typeForIClass(lambdaThrown[i]);
        {
            // PACKAGE-private, so that the synthesized anonymous class can invoke it without the
            // private-method rewrite (which renames the method and prepends a parameter).
            List<Modifier> mods = new ArrayList<>();
            if (staticContext) mods.addAll(Arrays.asList(Java.accessModifiers(le.getLocation(), "static")));
            if (enclosingTypeDeclaration instanceof InterfaceDeclaration) {
                mods.addAll(Arrays.asList(Java.accessModifiers(le.getLocation(), staticContext ? "public" : "default")));
            }
            MethodDeclarator lambdaMethod = new MethodDeclarator(
                le.getLocation(),                                // location
                null,                                            // docComment
                (Modifier[]) mods.toArray(new Modifier[mods.size()]),    // modifiers
                lambdaTypeParameters,                            // typeParameters
                this.typeForIClass(samRt),                       // type
                lambdaMethodName,                                // name
                new FormalParameters(le.getLocation(), (FormalParameter[]) lps.toArray(new FormalParameter[lps.size()]), false),
                lambdaThrows,                                    // thrownExceptions
                null,                                            // defaultValue
                lambdaDollarStatements                           // statements
            );
            if (enclosingTypeDeclaration instanceof AbstractTypeDeclaration) {
                ((AbstractTypeDeclaration) enclosingTypeDeclaration).addDeclaredMethod(lambdaMethod);

                // The resolved IClass may have cached the declared methods before lambda was added.
                this.resolve(enclosingTypeDeclaration).invalidateMethodCaches();
            } else
            {
                throw UnitCompiler.compileException(le, (
                    "LAMBDA expressions are not supported in this context (enclosing type is not a class)"
                ));
            }
        }

        // The anonymous class' SAM method delegates to the "lambda$N" method; it passes the ERASED parameters
        // (cast to the effective parameter types) and the captured local variables (which JANINO's inner-class
        // capture machinery passes to the anonymous class at ITS creation, i.e. with correct value semantics).
        FormalParameter[] mps = new FormalParameter[samPts.length];
        final String[]    argNames = new String[samPts.length];
        for (int i = 0; i < samPts.length; i++) {
            argNames[i] = "lambda$arg$" + i;
            mps[i] = new FormalParameter(le.getLocation(), new Modifier[0], this.typeForIClass(samPts[i]), argNames[i]);
        }
        List<Rvalue> callArgs = new ArrayList<>();
        for (int i = 0; i < samPts.length; i++) {
            IType et = effectiveTypes[i];
            if (et == samPts[i]) {
                callArgs.add(new AmbiguousName(le.getLocation(), new String[] { argNames[i] }));
            } else
            if (UnitCompiler.isPrimitive(et)) {
                callArgs.add(new AmbiguousName(le.getLocation(), new String[] { argNames[i] }));  // Unboxing/widening at the invocation.
            } else
            {
                callArgs.add(new Cast(le.getLocation(), new SimpleType(le.getLocation(), et), new AmbiguousName(le.getLocation(), new String[] { argNames[i] })));
            }
        }
        for (String cn : capturedLocals.keySet()) callArgs.add(new AmbiguousName(le.getLocation(), new String[] { cn }));
        Type helperOwner = new SimpleType(le.getLocation(), this.lexicalReceiver(this.resolve(enclosingTypeDeclaration), le.getEnclosingScope()));
        Atom helperReceiver = staticContext ? helperOwner : new QualifiedThisReference(le.getLocation(), helperOwner);
        MethodInvocation call = new MethodInvocation(le.getLocation(), helperReceiver, lambdaMethodName, (Rvalue[]) callArgs.toArray(new Rvalue[callArgs.size()]));
        IType[] helperTargets = new IType[lps.size()];
        for (int i = 0; i < helperTargets.length; i++) helperTargets[i] = this.getType(lps.get(i).type);
        this.lambdaHelperTargets.put(call, helperTargets);
        List<BlockStatement> samStatements = new ArrayList<>();
        samStatements.add(
            samRt == IClass.VOID ? new ExpressionStatement(call) : new ReturnStatement(le.getLocation(), call)
        );

        return this.implementSam(le, sam, mps, samStatements);
    }

    /** Counter for the names of the synthesized "lambda$N" methods. */
    private int lambdaMethodCount;
    private final Map<Invocation, IType[]> lambdaHelperTargets = new java.util.IdentityHashMap<>();

    /**
     * @return The substituted (instantiated) return type of the SAM, or <var>erasedReturnType</var>
     */
    private IClass
    substitutedSamReturnType(IType targetType, IClass.IMethod sam, IClass erasedReturnType) throws CompileException {
        IType type = this.functionalReturn(targetType, sam);
        return type instanceof IClass ? (IClass) type : UnitCompiler.rawTypeOf(type);
    }

    /**
     * Desugars a method reference of the forms "Type::staticMethod", "expression::method" and
     * "Type::instanceMethod" into an anonymous class implementing the single abstract method of the target type.
     */
    private Rvalue
    desugarMethodReference(MethodReference mr) throws CompileException {

        IClass.IMethod sam    = this.getSam(mr, mr.targetIType);
        IClass[]       samPts = sam.getParameterTypes();
        IClass         samRt  = sam.getReturnType();

        // The INSTANTIATED parameter types (e.g. "String" instead of the erased "Object" for a
        // "Function<String, _>" target), which are decisive for overload resolution.
        IType[] matchPts = this.functionalParameters(mr.targetIType, sam);

        String[]       args = new String[samPts.length];
        FormalParameter[] mps = new FormalParameter[samPts.length];
        for (int i = 0; i < samPts.length; i++) {
            args[i] = "mr$arg$" + i;
            mps[i]  = new FormalParameter(mr.getLocation(), new Modifier[0], this.typeForIClass(samPts[i]), args[i]);
        }

        List<BlockStatement> statements = new ArrayList<>();
        @Nullable Atom   target;         // The LHS of the generated method invocation.
        Rvalue[]         targetArguments = new Rvalue[0];
        int              firstArg;

        ReferenceBinding binding = this.resolveReference(mr, mr.targetIType);
        IMethod im = binding.method;

        // The LHS of a method reference is not scope-wired by the parser (the "Rvalue" traverser does not descend
        // into method references).
        if (mr.lhs instanceof Rvalue) UnitCompiler.setRvalueScopeIfUnset((Rvalue) mr.lhs, mr.getEnclosingScope());

        Atom lhsAtom = mr.lhs instanceof AmbiguousName ? this.reclassify((AmbiguousName) mr.lhs) : mr.lhs;
        Type lhsType = lhsAtom.toType();  // Non-null iff the LHS denotes a type.
        if (mr.superReference) {
            target = null;
            firstArg = 0;
        } else if (lhsType != null) {

            // "Type::method" -- either a STATIC method reference or an UNBOUND instance method reference.
            UnitCompiler.setScopeIfUnset(lhsType, mr.getEnclosingScope());
            IClass lhsIClass = this.getRawType(lhsType);
            if (binding.receiverArguments == 1) {
                target = new Cast(mr.getLocation(), this.typeForIClass(lhsIClass),
                    new AmbiguousName(mr.getLocation(), new String[] { args[0] }));
                firstArg = 1;
            } else {
                target = this.typeForIClass(lhsIClass);
                firstArg = 0;
            }
        } else
        {

            // "expression::method" -- BOUND instance method reference. (The receiver is the expression; the
            // referenced method's arguments are ALL SAM parameters.)
            boolean lexicalThis = mr.lhs instanceof ThisReference || mr.lhs instanceof QualifiedThisReference;
            if (!lexicalThis && (!(lhsAtom instanceof LocalVariableAccess)
                || !((LocalVariableAccess) lhsAtom).localVariable.finaL
                    && !((LocalVariableAccess) lhsAtom).localVariable.effectivelyFinal)) {
                return this.captureMethodReferenceReceiver(mr);
            }
            IClass lhsIClass = UnitCompiler.rawTypeOf(this.getType((Atom) mr.lhs));
            // The receiver is re-resolved as a fresh name in the implementing method; if it denotes an
            // effectively-final local variable of the enclosing method, JANINO's inner-class local variable capture
            // machinery passes it to the anonymous class.
            if (lexicalThis) {
                target = new QualifiedThisReference(mr.getLocation(), this.typeForIClass(lhsIClass));
            } else {
                AmbiguousName an = (AmbiguousName) mr.lhs;
                target = new AmbiguousName(mr.getLocation(), an.identifiers, an.n);
            }
            firstArg = 0;
        }

        // Restore the instantiated SAM type before ordinary invocation conversion (including unboxing
        // and primitive widening). Casting directly to the delegate type would change overload resolution.
        targetArguments = new Rvalue[matchPts.length - firstArg];
        for (int i = 0; i < targetArguments.length; i++) {
            int j = firstArg + i;
            Rvalue a = new AmbiguousName(mr.getLocation(), new String[] { args[j] });
            targetArguments[i] = samPts[j] == matchPts[j] ? a : new Cast(mr.getLocation(), new SimpleType(mr.getLocation(), matchPts[j]), a);
        }
        if (binding.variableArity) {
            IClass[] parameters = im.getParameterTypes();
            int fixed = parameters.length - 1;
            Rvalue[] tail = new Rvalue[targetArguments.length - fixed];
            System.arraycopy(targetArguments, fixed, tail, 0, tail.length);
            Rvalue[] packed = new Rvalue[parameters.length];
            System.arraycopy(targetArguments, 0, packed, 0, fixed);
            packed[fixed] = new NewInitializedArray(mr.getLocation(), parameters[fixed], new ArrayInitializer(mr.getLocation(), tail));
            targetArguments = packed;
        }
        Rvalue invocation;
        if (mr.superReference) {
            SuperclassMethodInvocation receiver = this.superReferenceReceiver(mr);
            SuperclassMethodInvocation call = new SuperclassMethodInvocation(mr.getLocation(), new SimpleType(mr.getLocation(), receiver.receiverType), mr.methodName, targetArguments);
            call.iMethod = im;
            call.receiverType = receiver.receiverType;
            call.invocationType = receiver.invocationType;
            call.typeArguments = this.copyReferenceArguments(mr.typeArguments, mr.getEnclosingScope());
            invocation = call;
        } else {
            MethodInvocation call = new MethodInvocation(mr.getLocation(), target, mr.methodName, targetArguments);
            call.iMethod = im;
            invocation = call;
        }

        this.referenceDelegates.add(invocation);
        this.invocationTargets.put((Invocation) invocation, binding.result);
        Rvalue value = invocation;
        IType result = binding.result;
        IClass samResult = this.substitutedSamReturnType(mr.targetIType, sam, samRt);
        if (!UnitCompiler.isPrimitive(result) && !UnitCompiler.isPrimitive(samResult)
            && this.genericConvertible(result, samResult, false)) result = samResult;
        if (result != im.getReturnType() && !UnitCompiler.isPrimitive(result)) {
            value = new Cast(mr.getLocation(), new SimpleType(mr.getLocation(), result), invocation);
        }
        if (!mr.superReference && !im.getReturnType().isPrimitive() && samResult.isPrimitiveNumeric()
            && samResult != IClass.CHAR && this.isUnboxingConvertible(im.getReturnType()) != IClass.CHAR) {
            // A direct reference to an erased numeric result uses Number's target primitive conversion.
            // super references go through a typed bridge and retain the bridge's result check.
            IClass number = UnitCompiler.assertNonNull(this.iClassLoader.TYPE_java_lang_Integer.getSuperclass());
            Rvalue erased = new Cast(mr.getLocation(), new SimpleType(mr.getLocation(), this.iClassLoader.TYPE_java_lang_Object), invocation);
            value = new MethodInvocation(mr.getLocation(), new Cast(mr.getLocation(), new SimpleType(mr.getLocation(), number), erased),
                samResult.toString() + "Value", new Rvalue[0]);
        }
        statements.add(samRt == IClass.VOID ? new ExpressionStatement(invocation) : new ReturnStatement(mr.getLocation(), value));

        return this.implementSam(mr, sam, mps, statements);
    }

    /** Evaluate a bound receiver once, then capture the factory's final parameter in the SAM proxy. */
    private Rvalue
    captureMethodReferenceReceiver(MethodReference reference) throws CompileException {
        Scope scope = reference.getEnclosingScope();
        while (!(scope instanceof TypeDeclaration)) scope = scope.getEnclosingScope();
        if (!(scope instanceof AbstractTypeDeclaration)) {
            throw UnitCompiler.compileException(reference, "Method reference is not enclosed by a type declaration");
        }
        AbstractTypeDeclaration owner = (AbstractTypeDeclaration) scope;
        Location location = reference.getLocation();
        IType receiverType = this.getType(reference.lhs);
        String name;
        do { name = "mr$factory$" + (++this.lambdaMethodCount); }
        while (owner.getMethodDeclaration(name) != null);
        String receiverName = "mr$receiver";
        MethodReference captured = new MethodReference(
            location, new AmbiguousName(location, new String[] { receiverName }), reference.methodName
        );
        captured.targetIType = reference.targetIType;
        captured.typeArguments = this.copyReferenceArguments(reference.typeArguments, reference.getEnclosingScope());
        this.syntheticReferences.add(captured);
        List<BlockStatement> body = new ArrayList<>();
        body.add(new ReturnStatement(location, captured));
        owner.addDeclaredMethod(new MethodDeclarator(
            location, null, Java.accessModifiers(location, "public", "static"), null,
            new SimpleType(location, reference.targetIType), name,
            new FormalParameters(location, new FormalParameter[] {
                new FormalParameter(location, Java.accessModifiers(location, "final"),
                    new SimpleType(location, receiverType), receiverName)
            }, false), new Type[0], null, body
        ));
        this.resolve(owner).invalidateMethodCaches();
        MethodInvocation creation = new MethodInvocation(location, new SimpleType(location, this.resolve(owner)),
            name, new Rvalue[] { this.toRvalueOrCompileException(reference.lhs) });
        creation.setEnclosingScope(reference.getEnclosingScope());
        return creation;
    }

    private void validateReferenceQualifier(Type type, boolean array) throws CompileException {
        while (type instanceof ArrayType) type = ((ArrayType) type).componentType;
        if (!(type instanceof ReferenceType)) return;
        for (TypeArgument[] arguments : ((ReferenceType) type).segmentTypeArguments) {
            if (arguments == null) continue;
            if (arguments.length == 0) throw UnitCompiler.compileException(type, "Diamond is not allowed in a reference qualifier");
            for (TypeArgument argument : arguments) {
                if (argument instanceof Wildcard) {
                    if (!array) throw UnitCompiler.compileException(type, "Wildcard is not allowed in a reference qualifier");
                    Type bound = ((Wildcard) argument).referenceType;
                    if (bound != null) this.validateReferenceQualifier(bound, true);
                } else if (argument instanceof Type) this.validateReferenceQualifier((Type) argument, true);
            }
        }
    }

    /** Desugars a "Type::new" constructor reference. */
    private Rvalue
    desugarConstructorReference(ClassInstanceCreationReference cicr) throws CompileException {

        UnitCompiler.setScopeIfUnset(cicr.type, cicr.getEnclosingScope());
        this.validateReferenceQualifier(cicr.type, false);
        IType ctorType = this.getType(cicr.type);
        if (!(ctorType instanceof ParameterizedIClass) && UnitCompiler.rawTypeOf(ctorType).getITypeVariables().length != 0
            && cicr.typeArguments != null) throw UnitCompiler.compileException(cicr, "Raw constructor reference cannot have explicit constructor type arguments");
        IClass.IMethod sam    = this.getSam(cicr, cicr.targetIType);
        IClass[]       samPts = sam.getParameterTypes();
        IClass         samRt  = sam.getReturnType();

        IType expectedReturn = this.functionalReturn(cicr.targetIType, sam);
        if (expectedReturn != IClass.VOID && !this.genericConvertible(ctorType, expectedReturn, false)) {
            throw UnitCompiler.compileException(cicr, "Incompatible constructor reference return type");
        }
        String[]       args = new String[samPts.length];
        FormalParameter[] mps = new FormalParameter[samPts.length];
        Rvalue[]        ctorArgs = new Rvalue[samPts.length];
        IType[] instantiatedParameters = this.functionalParameters(cicr.targetIType, sam);
        for (int i = 0; i < samPts.length; i++) {
            args[i] = "cr$arg$" + i;
            mps[i]  = new FormalParameter(cicr.getLocation(), new Modifier[0], this.typeForIClass(samPts[i]), args[i]);

            // The constructor arguments are cast to the INSTANTIATED parameter types where they differ from the
            // ERASED ones, so that overload resolution selects the right constructor (JLS8 15.13.1).
            IType subst = instantiatedParameters[i];
            ctorArgs[i] = subst == samPts[i] || UnitCompiler.isPrimitive(subst) && samPts[i].isPrimitive()
            ? new AmbiguousName(cicr.getLocation(), new String[] { args[i] })
            : new Cast(cicr.getLocation(), new SimpleType(cicr.getLocation(), subst), new AmbiguousName(cicr.getLocation(), new String[] { args[i] }));
        }

        List<BlockStatement> statements = new ArrayList<>();
        NewClassInstance creation = new NewClassInstance(
            cicr.getLocation(),             // location
            null,                           // qualification
            new SimpleType(cicr.getLocation(), ctorType), // fresh AST retaining the qualifier's arguments
            ctorArgs                        // arguments
        );
        creation.typeArguments = this.copyReferenceArguments(cicr.typeArguments, cicr.getEnclosingScope());
        this.referenceDelegates.add(creation);
        if (!(ctorType instanceof ParameterizedIClass) && UnitCompiler.rawTypeOf(ctorType).getITypeVariables().length != 0) {
            this.inferredReferenceCreations.add(creation);
            this.creationTargets.put(creation, expectedReturn);
        }
        statements.add(samRt == IClass.VOID ? new ExpressionStatement(creation) : new ReturnStatement(cicr.getLocation(), creation));

        return this.implementSam(cicr, sam, mps, statements);
    }

    /** Desugars an "ElementType[]::new" array creation reference. */
    private Rvalue
    desugarArrayCreationReference(ArrayCreationReference acr) throws CompileException {

        UnitCompiler.setScopeIfUnset(acr.type, acr.getEnclosingScope());
        UnitCompiler.setScopeIfUnset(acr.type.componentType, acr.getEnclosingScope());
        this.validateReferenceQualifier(acr.type, true);
        IType arrayType = this.getType(acr.type);
        if (!this.isReifiable(arrayType)) throw UnitCompiler.compileException(acr, "Array reference type is not reifiable");
        IType elemType = this.getType(acr.type.componentType);
        IClass.IMethod sam    = this.getSam(acr, acr.targetIType);
        IClass[]       samPts = sam.getParameterTypes();

        IType[] inputs = this.functionalParameters(acr.targetIType, sam);
        if (inputs.length != 1 || !this.genericConvertible(inputs[0], IClass.INT, true)) {
            throw UnitCompiler.compileException(acr, (
                "Array creation reference \"" + acr + "\" requires one parameter convertible to int"
            ));
        }

        IType expectedReturn = this.functionalReturn(acr.targetIType, sam);
        if (expectedReturn != IClass.VOID && !this.genericConvertible(arrayType, expectedReturn, false)) {
            throw UnitCompiler.compileException(acr, "Incompatible array reference return type");
        }
        FormalParameter[] mps = {
            new FormalParameter(acr.getLocation(), new Modifier[0], this.typeForIClass(samPts[0]), "acr$arg$0")
        };

        List<BlockStatement> statements = new ArrayList<>();
        Rvalue length = new AmbiguousName(acr.getLocation(), new String[] { "acr$arg$0" });
        if (!inputs[0].equals(samPts[0])) length = new Cast(acr.getLocation(), new SimpleType(acr.getLocation(), inputs[0]), length);
        NewArray creation = new NewArray(
            acr.getLocation(),                                                          // location
            new SimpleType(acr.getLocation(), elemType),                                // fresh AST retaining generics
            new Rvalue[] { length },                                                    // dimExprs
            0                                                                           // dims
        );
        if (sam.getReturnType() == IClass.VOID) {
            statements.add(new LocalVariableDeclarationStatement(acr.getLocation(), new Modifier[0],
                new SimpleType(acr.getLocation(), arrayType),
                new VariableDeclarator[] { new VariableDeclarator(acr.getLocation(), "acr$result", 0, creation) }));
        } else statements.add(new ReturnStatement(acr.getLocation(), creation));

        return this.implementSam(acr, sam, mps, statements);
    }

    /**
     * @return An anonymous class which implements the single abstract method <var>sam</var> with the given
     *         parameters and body statements
     * @param rv The LAMBDA expression or method reference; determines the location and the enclosing scope
     */
    private Rvalue
    implementSam(Rvalue rv, IClass.IMethod sam, FormalParameter[] mps, List<BlockStatement> statements)
    throws CompileException {

        IClass samIc = UnitCompiler.rawTypeOf(rv instanceof LambdaExpression ? ((LambdaExpression) rv).targetIType : rv instanceof MethodReference ? ((MethodReference) rv).targetIType : rv instanceof ClassInstanceCreationReference ? ((ClassInstanceCreationReference) rv).targetIType : ((ArrayCreationReference) rv).targetIType);
        if (!samIc.isInterface()) {
            throw UnitCompiler.compileException(rv, "Target type \"" + samIc + "\" is not an interface");
        }

        // The anonymous class' base type is the RAW interface.
        AnonymousClassDeclaration acd = new AnonymousClassDeclaration(rv.getLocation(), this.typeForIClass(samIc));
        acd.lambdaProxy = true;
        if (samIc instanceof IntersectionIClass) {
            acd.lambdaInterfaces = ((IntersectionIClass) samIc).bounds;
        }
        acd.setEnclosingScope(rv.getEnclosingScope());

        // Throw the exceptions that the SAM declares (plus none).
        IClass[] tes = sam.getThrownExceptions();
        Type[]   thrownExceptions = new Type[tes.length];
        for (int i = 0; i < tes.length; i++) thrownExceptions[i] = this.typeForIClass(tes[i]);

        MethodDeclarator samImplementation = new MethodDeclarator(
            rv.getLocation(),                            // location
            null,                                        // docComment
            Java.accessModifiers(rv.getLocation(), "public"), // modifiers
            null,                                        // typeParameters
            this.typeForIClass(sam.getReturnType()),     // type
            sam.getName(),                               // name
            new FormalParameters(rv.getLocation(), mps, false), // erased SAM parameters already include the array dimension
            thrownExceptions,                            // thrownExceptions
            null,                                        // defaultValue
            statements                                   // statements
        );
        samImplementation.lambdaSamImplementation = true;
        acd.addDeclaredMethod(samImplementation);

        NewAnonymousClassInstance naci = new NewAnonymousClassInstance(rv.getLocation(), null, acd, new Rvalue[0]);
        naci.setEnclosingScope(rv.getEnclosingScope());
        return naci;
    }

    /**
     * Determines the single abstract method ("SAM") of the target type of the given LAMBDA expression or method
     * reference.
     */
    private IClass.IMethod
    getSam(Rvalue rv, @Nullable IType targetType) throws CompileException {

        if (targetType == null) {
            throw UnitCompiler.compileException(rv, (
                "Cannot determine the target type of \"" + rv + "\"; use it in an assignment, a variable"
                + " initializer, a return statement, a cast or as a method invocation argument"
            ));
        }
        IClass targetIc = UnitCompiler.rawTypeOf(targetType);
        if (!targetIc.isInterface()) {
            throw UnitCompiler.compileException(rv, "Target type \"" + targetIc + "\" of \"" + rv + "\" is not an interface");
        }
        return this.getSingleAbstractMethod(rv, targetIc);
    }

    /**
     * @return The one abstract method of the given interface, including the ones inherited from superinterfaces,
     *         excluding STATIC, DEFAULT and the public methods of "java.lang.Object" (JLS9 9.8)
     */
    private IClass.IMethod
    getSingleAbstractMethod(Locatable locatable, IClass interfaze) throws CompileException {

        List<IMethod> abstractMethods = new ArrayList<>();
        this.collectAbstractMethods(interfaze, new HashSet<String>(), abstractMethods);

        if (abstractMethods.size() == 1) return abstractMethods.get(0);

        throw UnitCompiler.compileException(locatable, (
            abstractMethods.isEmpty()
            ? "Target type \"" + interfaze + "\" has no abstract methods"
            : "Target type \"" + interfaze + "\" has more than one abstract method"
        ));
    }

    private void
    collectAbstractMethods(IClass interfaze, Set<String> seenSignatures, List<IMethod> result) throws CompileException {

        AbstractTypeDeclaration source = this.sourceDeclarations.get(interfaze);
        IType receiver = source == null ? interfaze : this.lexicalReceiver(interfaze, source);

        // BFS from the interface itself outwards: A method SIGNATURE that appears at a MORE-DERIVED level
        // (as an abstract, a default or an overriding method) wins; a superinterface's method with an
        // already-seen signature is an override and does not count again.
        List<IClass> queue = new ArrayList<>(Arrays.asList(interfaze));
        while (!queue.isEmpty()) {
            IClass ic = queue.remove(0);

            for (IMethod im : ic.getDeclaredIMethods()) {

                String signature = this.functionalMethodSignature(receiver, im);
                if (im.isStatic()) continue;
                if (!seenSignatures.add(signature)) {
                    for (int i = 0; i < result.size(); i++) {
                        IMethod previous = result.get(i);
                        if (signature.equals(this.functionalMethodSignature(receiver, previous))) {
                            if (im.getDeclaringIClass().isAssignableFrom(previous.getDeclaringIClass())) continue;
                            result.set(i, this.mergeSamMethods(previous, im));
                        }
                    }
                    continue;
                }

                // STATIC and non-abstract (DEFAULT) methods do not count as the function descriptor, but they
                // DO "consume" their signature (see above).
                if (im.isStatic() || !im.isAbstract()) continue;

                // PUBLIC "java.lang.Object" methods do not count (JLS8 9.8); notice that "clone()" and
                // "finalize()" are NOT public in "java.lang.Object", i.e. an interface that redeclares them
                // IS a functional interface.
                if (UnitCompiler.isPublicObjectMethod(im)) continue;

                result.add(im);
            }

            queue.addAll(Arrays.asList(ic.getInterfaces()));
        }
    }

    private String functionalMethodSignature(IType receiver, IMethod method) throws CompileException {
        GenericType[] parameters = this.getGenericParameterTypes(method);
        if (parameters == null) return method.getName() + Arrays.toString(IClass.getDescriptors(method.getParameterTypes()));
        Map<String, IType> environment = this.receiverEnvironment(receiver, method.getDeclaringIClass());
        Set<String> variables = this.getMethodTypeParameterNames(method);
        if (variables != null) environment.keySet().removeAll(variables);
        IClass[] types = method.getParameterTypes().clone();
        for (int i = 0; i < types.length; i++) {
            IType parameter = this.substituteGenericType(parameters[i], environment);
            if (parameter != null) types[i] = UnitCompiler.rawTypeOf(parameter);
        }
        return method.getName() + Arrays.toString(IClass.getDescriptors(types));
    }

    private IMethod
    mergeSamMethods(final IMethod a, final IMethod b) throws CompileException {
        if (a.getDeclaringIClass().isAssignableFrom(b.getDeclaringIClass())) return b;
        final IMethod selected;
        if (a.getReturnType().isAssignableFrom(b.getReturnType())) selected = b;
        else if (b.getReturnType().isAssignableFrom(a.getReturnType())) selected = a;
        else throw new CompileException("Incompatible inherited functional return types", Location.NOWHERE);
        final Set<IClass> exceptions = new LinkedHashSet<>();
        for (IClass x : a.getThrownExceptions()) for (IClass y : b.getThrownExceptions()) {
            if (x.isAssignableFrom(y)) exceptions.add(y);
            else if (y.isAssignableFrom(x)) exceptions.add(x);
        }
        return selected.getDeclaringIClass().new IMethod() {
            @Override public IAnnotation[] getAnnotations() { return selected.getAnnotations(); }
            @Override public Access getAccess() { return selected.getAccess(); }
            @Override public boolean isStatic() { return false; }
            @Override public boolean isAbstract() { return true; }
            @Override public boolean isVarargs() { return selected.isVarargs(); }
            @Override public String getName() { return selected.getName(); }
            @Override public IClass getReturnType() throws CompileException { return selected.getReturnType(); }
            @Override public IClass[] getParameterTypes2() throws CompileException { return selected.getParameterTypes(); }
            @Override public IClass[] getThrownExceptions2() { return (IClass[]) exceptions.toArray(new IClass[exceptions.size()]); }
        };
    }

    private static boolean
    isPublicObjectMethod(IMethod im) throws CompileException {
        String       name  = im.getName();
        List<String> paramFds = new ArrayList<>();
        for (IClass pt : im.getParameterTypes()) paramFds.add(pt.getDescriptor());

        if ("equals".equals(name) && paramFds.size() == 1 && "Ljava/lang/Object;".equals(paramFds.get(0))) return true;
        // Notice: "clone()" and "finalize()" are PROTECTED (not public) in "java.lang.Object", so an interface
        // that redeclares them still has a function descriptor.
        if (("hashCode".equals(name) || "toString".equals(name) || "getClass".equals(name) || "notify".equals(name) || "notifyAll".equals(name)) && paramFds.isEmpty()) return true;
        if ("wait".equals(name) && (paramFds.isEmpty() || ("J".equals(paramFds.get(0)) && (paramFds.size() == 1 || paramFds.size() == 2 && "I".equals(paramFds.get(1)))))) return true;

        return false;
    }

    /**
     * @return A {@link Type} AST for the given (erased) {@link IClass}; for reference types, a fully qualified
     *         reference type
     */
    private Type
    typeForIClass(IClass c) {

        if (c.isArray()) return new ArrayType(this.typeForIClass(c.getComponentType()));

        if (c.isPrimitive()) {
            if (c == IClass.VOID)    return new PrimitiveType(Location.NOWHERE, Primitive.VOID);
            if (c == IClass.BOOLEAN) return new PrimitiveType(Location.NOWHERE, Primitive.BOOLEAN);
            if (c == IClass.BYTE)    return new PrimitiveType(Location.NOWHERE, Primitive.BYTE);
            if (c == IClass.SHORT)   return new PrimitiveType(Location.NOWHERE, Primitive.SHORT);
            if (c == IClass.CHAR)    return new PrimitiveType(Location.NOWHERE, Primitive.CHAR);
            if (c == IClass.INT)     return new PrimitiveType(Location.NOWHERE, Primitive.INT);
            if (c == IClass.LONG)    return new PrimitiveType(Location.NOWHERE, Primitive.LONG);
            if (c == IClass.FLOAT)   return new PrimitiveType(Location.NOWHERE, Primitive.FLOAT);
            if (c == IClass.DOUBLE)  return new PrimitiveType(Location.NOWHERE, Primitive.DOUBLE);
            throw new AssertionError(c);
        }

        // A "SimpleType" wraps the resolved IClass directly, i.e. no name resolution is necessary; this works
        // for top-level AND for nested classes.
        return new SimpleType(Location.NOWHERE, c);
    }

    private IType
    compileGet2(QualifiedThisReference qtr) throws CompileException {
        this.referenceThis(
            qtr,                                       // locatable
            this.getDeclaringClass(qtr),               // declaringClass
            this.getDeclaringTypeBodyDeclaration(qtr), // declaringTypeBodyDeclaration
            this.getTargetIType(qtr)                   // targetIClass
        );
        return this.getTargetIType(qtr);
    }

    private IClass
    compileGet2(ClassLiteral cl) throws CompileException {

        Type literalType = cl.type;
        while (literalType instanceof ArrayType) literalType = ((ArrayType) literalType).componentType;
        IType literalComponent = this.getType(literalType);
        if (literalComponent instanceof SourceTypeVariable || literalComponent instanceof CapturedType) {
            this.compileError("LHS of class literal must not be a type variable", cl.getLocation());
        }
        if (literalType instanceof ReferenceType) {
            ReferenceType reference = (ReferenceType) literalType;
            for (TypeArgument[] arguments : reference.segmentTypeArguments) {
                if (arguments != null) this.compileError("LHS of class literal must not be a parameterized type", cl.getLocation());
            }
        }
        IClass iClass = this.getRawType(cl.type);

        if (iClass.isPrimitive()) {

            // Primitive class literal.
            IClass wrapperIClass = (
                iClass == IClass.VOID    ? this.iClassLoader.TYPE_java_lang_Void      :
                iClass == IClass.BYTE    ? this.iClassLoader.TYPE_java_lang_Byte      :
                iClass == IClass.CHAR    ? this.iClassLoader.TYPE_java_lang_Character :
                iClass == IClass.DOUBLE  ? this.iClassLoader.TYPE_java_lang_Double    :
                iClass == IClass.FLOAT   ? this.iClassLoader.TYPE_java_lang_Float     :
                iClass == IClass.INT     ? this.iClassLoader.TYPE_java_lang_Integer   :
                iClass == IClass.LONG    ? this.iClassLoader.TYPE_java_lang_Long      :
                iClass == IClass.SHORT   ? this.iClassLoader.TYPE_java_lang_Short     :
                iClass == IClass.BOOLEAN ? this.iClassLoader.TYPE_java_lang_Boolean   :
                null
            );
            assert wrapperIClass != null;

            this.bytecodeEmitter.getfield(cl, wrapperIClass, "TYPE", this.iClassLoader.TYPE_java_lang_Class, true);
        } else {

            // Non-primitive class literal.

            this.consT(cl, iClass);
        }

        return this.getType2(cl);
    }

    private IType
    compileGet2(Assignment a) throws CompileException {

        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            // Compile LHS context.
            int lhsCs = this.compileContext(a.lhs);
            // Convert RHS value to LHS type.
            IType lhsType = this.getType(a.lhs);
            this.setLambdaTargetType(a.rhs, lhsType);
            IType rhsType = this.compileGetValue(a.rhs);
            this.assignmentConversion(a, rhsType, lhsType, this.constantEvaluator.getConstantValue(a.rhs));
            // Duplicate RHS value below LHS context.
            this.bytecodeEmitter.dupxx(a, lhsCs);
            // Set LHS.
            this.compileSet(a.lhs);

            return lhsType;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCs = this.compileContext(a.lhs);
        this.bytecodeEmitter.dupn(a, lhsCs);
        IType lhsType    = this.compileGet(a.lhs);
        IType resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), /* <= IMPORTANT! */
            a.rhs                 // rhs
        );

        // Convert the result to LHS type (JLS7 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
        ) throw new InternalCompilerException(a.getLocation(), "SNO: \"" + a.operator + "\" reconversion failed");
        this.bytecodeEmitter.dupxx(a, lhsCs);
        this.compileSet(a.lhs);
        return lhsType;
    }

    private IType
    compileGet2(ConditionalExpression ce) throws CompileException {

        IType expressionType = this.getType2(ce);
        IType target = this.conditionalTargets.get(ce);
        if (target != null && !UnitCompiler.isPrimitive(expressionType) && !UnitCompiler.isPrimitive(target)
            && this.genericConvertible(expressionType, target, false)) expressionType = target;
        // Each branch is consumed by the conditional itself, including standalone numeric/boolean conditionals.
        this.setLambdaTargetType(ce.mhs, expressionType, false);
        this.setLambdaTargetType(ce.rhs, expressionType, false);

        {
            Object lhsCv = this.constantEvaluator.getConstantValue(ce.lhs);
            if (lhsCv instanceof Boolean) {

                // LHS is a constant expression.
                if (((Boolean) lhsCv).booleanValue()) {
                    IType actual = this.compileGetValue(ce.mhs);
                    this.castConversion(ce.mhs, actual, expressionType, UnitCompiler.NOT_CONSTANT);
                } else {
                    IType actual = this.compileGetValue(ce.rhs);
                    this.castConversion(ce.rhs, actual, expressionType, UnitCompiler.NOT_CONSTANT);
                }
                return expressionType;
            }
        }

        // Non-constant LHS.
        {
            final CodeContext.Offset toEnd = this.getCodeContext().new BasicBlock();
            final CodeContext.Offset toRhs = this.getCodeContext().new BasicBlock();

            this.compileBoolean(ce.lhs, toRhs, UnitCompiler.JUMP_IF_FALSE);

            IType actualMhs = this.compileGetValue(ce.mhs);
            this.assignmentConversion(ce.mhs, actualMhs, expressionType, this.constantEvaluator.getConstantValue(ce.mhs));
            this.bytecodeEmitter.gotO(ce, toEnd);

            this.getCodeContext().currentInserter().setStackMap(null);
            toRhs.setBasicBlock();
            IType actualRhs = this.compileGetValue(ce.rhs);
            this.assignmentConversion(ce.rhs, actualRhs, expressionType, this.constantEvaluator.getConstantValue(ce.rhs));

            toEnd.set();
        }

        return expressionType;
    }

    private final Map<IType, Map<IType, IType>> commonSupertypes = new HashMap<>();

    private IType
    commonSupertype(IType t1, IType t2) throws CompileException {

        if (this.genericConvertible(t1, t2, false)) return t2;
        if (this.genericConvertible(t2, t1, false)) return t1;
        Map<IType, IType> cached = this.commonSupertypes.get(t1);
        if (cached == null) this.commonSupertypes.put(t1, cached = new HashMap<>());
        IType previous = cached.get(t2);
        if (previous != null) return previous;
        // A recursive generic hierarchy (e.g. Comparable<A>/Comparable<B>) must not expand forever.
        cached.put(t2, this.iClassLoader.TYPE_java_lang_Object);
        IType result = this.commonSupertypeUncached(t1, t2);
        cached.put(t2, result);
        return result;
    }

    private IType commonSupertypeUncached(IType t1, IType t2) throws CompileException {
        if (UnitCompiler.isArray(t1) && UnitCompiler.isArray(t2)) {
            IType a = UnitCompiler.getComponentType(t1), b = UnitCompiler.getComponentType(t2);
            if (!UnitCompiler.isPrimitive(a) && !UnitCompiler.isPrimitive(b)) return this.arrayType(this.commonSupertype(a, b));
        }
        if (UnitCompiler.rawTypeOf(t1) == UnitCompiler.rawTypeOf(t2)) {
            if (!(t1 instanceof ParameterizedIClass) || !(t2 instanceof ParameterizedIClass)) return UnitCompiler.rawTypeOf(t1);
            IType[] a = ((ParameterizedIClass) t1).actualTypeArguments, b = ((ParameterizedIClass) t2).actualTypeArguments;
            IType[] result = new IType[a.length];
            for (int i = 0; i < a.length; i++) {
                if (a[i].equals(b[i])) { result[i] = a[i]; continue; }
                IType x = a[i] instanceof IWildcardType ? ((IWildcardType) a[i]).getUpperBound() : a[i];
                IType y = b[i] instanceof IWildcardType ? ((IWildcardType) b[i]).getUpperBound() : b[i];
                result[i] = this.wildcard(true, this.commonSupertype(x, y));
            }
            IType ownerA = ((ParameterizedIClass) t1).ownerType, ownerB = ((ParameterizedIClass) t2).ownerType;
            IType owner = ownerA == null || ownerB == null ? null : this.commonSupertype(ownerA, ownerB);
            return new ParameterizedIClass(UnitCompiler.rawTypeOf(t1), result, owner);
        }
        return this.commonSupertype2(t1, t2);
    }

    private IType
    commonSupertype2(IType t1, IType t2) throws CompileException {
        Map<IClass, IType> a = new LinkedHashMap<>(), b = new LinkedHashMap<>();
        this.collectSupertypes(t1, a);
        this.collectSupertypes(t2, b);
        List<IType> bounds = new ArrayList<>();
        for (Map.Entry<IClass, IType> entry : a.entrySet()) {
            IType other = b.get(entry.getKey());
            if (other == null) continue;
            boolean redundant = false;
            for (IClass raw : a.keySet()) {
                if (raw != entry.getKey() && b.containsKey(raw) && entry.getKey().isAssignableFrom(raw)) { redundant = true; break; }
            }
            if (!redundant) bounds.add(this.commonSupertype(entry.getValue(), other));
        }
        if (bounds.isEmpty()) return this.iClassLoader.TYPE_java_lang_Object;
        if (bounds.size() == 1) return bounds.get(0);
        // Interface-only inferred intersections use Object as their JVM carrier; member access still checks
        // each declared interface. A concrete common superclass, when present, provides the carrier instead.
        CapturedType intersection = new CapturedType(this.iClassLoader.TYPE_java_lang_Object);
        intersection.intersection = true;
        intersection.bounds = bounds.toArray(new IType[bounds.size()]);
        for (IType bound : bounds) if (!UnitCompiler.rawTypeOf(bound).isInterface()) intersection.upper = bound;
        return intersection;
    }

    private void collectSupertypes(IType type, Map<IClass, IType> result) throws CompileException {
        if (type instanceof CapturedType) {
            for (IType bound : ((CapturedType) type).bounds) this.collectSupertypes(bound, result);
            return;
        }
        if (type instanceof SourceTypeVariable) {
            ReferenceType[] bounds = ((SourceTypeVariable) type).declaration.bound;
            if (bounds != null) for (ReferenceType bound : bounds) this.collectSupertypes(this.sourceBoundType(bound), result);
            else result.put(this.iClassLoader.TYPE_java_lang_Object, this.iClassLoader.TYPE_java_lang_Object);
            return;
        }
        IClass raw = UnitCompiler.rawTypeOf(type);
        if (result.containsKey(raw)) return;
        result.put(raw, type);
        Map<String, IType> environment = new HashMap<>();
        this.bindTypeVariables(type, environment);
        for (GenericType parent : raw.getGenericSupertypes()) {
            if (parent == null) continue;
            IType resolved = this.substituteGenericType(parent, environment);
            if (resolved != null) this.collectSupertypes(resolved, result);
        }
        // Raw generic receivers may not provide bindings for their generic parents.
        IClass superclass = raw.getSuperclass();
        if (superclass != null) this.collectSupertypes(superclass, result);
        for (IClass iface : raw.getInterfaces()) this.collectSupertypes(iface, result);
        if (raw.isInterface()) result.put(this.iClassLoader.TYPE_java_lang_Object, this.iClassLoader.TYPE_java_lang_Object);
    }

    @Nullable private static Byte
    isByteConstant(@Nullable Object o) {
        if (o instanceof Integer) {
            int v = (Integer) o;
            return v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE ? Byte.valueOf((byte) v) : null;
        }
        return null;
    }

    private IType
    compileGet2(Crement c) throws CompileException {

        // Optimized crement of "int" local variable.
        LocalVariable lv = this.isIntLv(c);
        if (lv != null) {
            if (!c.pre) this.bytecodeEmitter.load(c, lv);
            this.bytecodeEmitter.iinc(c, lv, c.operator);
            if (c.pre) this.bytecodeEmitter.load(c, lv);
            return IClass.INT;
        }

        // Compile operand context.
        int operandCs = this.compileContext(c.operand);
        // DUP operand context.
        this.bytecodeEmitter.dupn(c, operandCs);
        // Get operand value.
        IType type = this.compileGet(c.operand);
        // If postincrement: DUPX the operand value.
        if (!c.pre) this.bytecodeEmitter.dupxx(c, operandCs);

        {

            // Apply "unary numeric promotion".
            IClass promotedType = this.unaryNumericPromotion(c, type);
            // Crement.
            this.consT(c, promotedType, 1);
            if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.bytecodeEmitter.add(c);
            } else
            if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.bytecodeEmitter.sub(c);
            } else {
                this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
            }
            this.reverseUnaryNumericPromotion(c, promotedType, type);
        }

        // If preincrement: DUPX the cremented operand value.
        if (c.pre) this.bytecodeEmitter.dupxx(c, operandCs);
        // Set operand.
        this.compileSet(c.operand);

        return type;
    }

    private IType
    compileGet2(ArrayAccessExpression aae) throws CompileException {
        IType lhsComponentType = this.getType(aae);
        this.bytecodeEmitter.xaload(aae, lhsComponentType);
        return lhsComponentType;
    }

    private IType
    compileGet2(FieldAccessExpression fae) throws CompileException {
        return this.compileResolvedReference(fae, this.determineValue(fae));
    }

    private IType
    compileGet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        return this.compileResolvedReference(scfae, this.determineValue(scfae));
    }

    private IClass
    compileGet2(UnaryOperation uo) throws CompileException {

        if (uo.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.compileGet2((BooleanRvalue) uo);
        }

        if (uo.operator == "+") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.unaryNumericPromotion(
                uo,
                this.convertToPrimitiveNumericType(uo, this.compileGetValue(uo.operand))
            );
        }

        if (uo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            {
                Object ncv = this.constantEvaluator.getConstantValue2(uo);
                if (ncv != UnitCompiler.NOT_CONSTANT) {
                    return this.unaryNumericPromotion(uo, this.consT(uo, ncv));
                }
            }

            IClass promotedType = this.unaryNumericPromotion(
                uo,
                this.convertToPrimitiveNumericType(uo, this.compileGetValue(uo.operand))
            );
            this.bytecodeEmitter.neg(uo, promotedType);
            return promotedType;
        }

        if (uo.operator == "~") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IType operandType = this.compileGetValue(uo.operand);

            IClass promotedType = this.unaryNumericPromotion(uo, operandType);
            if (promotedType == IClass.INT) {
                this.consT(uo, -1);
                this.bytecodeEmitter.xor(uo, Opcode.IXOR);
                return IClass.INT;
            }
            if (promotedType == IClass.LONG) {
                this.consT(uo, -1L);
                this.bytecodeEmitter.xor(uo, Opcode.LXOR);
                return IClass.LONG;
            }
            this.compileError("Operator \"~\" not applicable to type \"" + promotedType + "\"", uo.getLocation());
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    private IClass
    compileGet2(Instanceof io) throws CompileException {
        IType lhsType = this.getType(io.lhs);
        IType rhsType = this.getType(io.rhs);

        if (lhsType != IClass.NULL && UnitCompiler.isPrimitive(lhsType)) {
            throw UnitCompiler.compileException(io, "instanceof requires a reference operand");
        }

        if (!this.isReifiable(rhsType)) {
            this.compileError("Cannot check against a non-reifiable type", io.getLocation());
            return IClass.BOOLEAN;
        }

        if (
            UnitCompiler.isInterface(lhsType) || UnitCompiler.isInterface(rhsType)
            // We cannot precompute the result from type information as the value might be null, but we should detect
            // when the instanceof is statically impossible.
            || UnitCompiler.isAssignableFrom(lhsType, rhsType)
            || UnitCompiler.isAssignableFrom(rhsType, lhsType)
        ) {
            this.compileGetReferenceValue(io.lhs);
            if (this.getTargetVersion() >= 8) this.annotationCompiler.compileCodeTypeAnnotations(io.rhs, new Annotation[0],
                (byte) 0x43, this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
            this.bytecodeEmitter.instanceoF(io, rhsType);
        } else {
            this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"", io.getLocation());
        }
        return IClass.BOOLEAN;
    }

    @Nullable private static IType
    getComponentType(IType expressionType) {
        if (expressionType instanceof CapturedType) return UnitCompiler.getComponentType(((CapturedType) expressionType).upper);
        if (expressionType instanceof IWildcardType) return UnitCompiler.getComponentType(((IWildcardType) expressionType).getUpperBound());
        return expressionType instanceof GenericArrayType
            ? ((GenericArrayType) expressionType).component : UnitCompiler.rawTypeOf(expressionType).getComponentType();
    }

    static boolean
    isPrimitive(IType type) { return UnitCompiler.rawTypeOf(type).isPrimitive(); }

    @Nullable private static IType
    getSuperclass(IType type) throws CompileException { return UnitCompiler.rawTypeOf(type).getSuperclass(); }

    private static boolean
    isInterface(IType type) { return UnitCompiler.rawTypeOf(type).isInterface(); }

    private static IType[]
    getInterfaces(IType t1) throws CompileException {
        IClass[] rawInterfaces = UnitCompiler.rawTypeOf(t1).getInterfaces();
        IType[] result = new IType[rawInterfaces.length];
        for (int i = 0; i < result.length; i++) result[i] = rawInterfaces[i];
        return result;
    }

    private static boolean
    isArray(IType type) { return UnitCompiler.rawTypeOf(type).isArray(); }

    private static boolean
    isAssignableFrom(IType targetType, IType sourceType) throws CompileException {
        return UnitCompiler.rawTypeOf(targetType).isAssignableFrom(UnitCompiler.rawTypeOf(sourceType));
    }

    private IType
    compileGet2(BinaryOperation bo) throws CompileException {
        if (
            bo.operator == "||"    // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "==" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "!=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "<"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == ">"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "<=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == ">=" // SUPPRESS CHECKSTYLE StringLiteralEquality
        ) {
            // Eventually calls "compileBoolean()".
            return this.compileGet2((BooleanRvalue) bo);
        }

        // If concatenation starts with the first pair, its non-String reference operand is consumed as Object.
        if (bo.operator == "+") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Iterator<Rvalue> operands = bo.unrollLeftAssociation();
            Rvalue first = operands.next();
            if (operands.hasNext()) {
                Rvalue second = operands.next();
                if (this.getType(first) == this.iClassLoader.TYPE_java_lang_String
                    || this.getType(second) == this.iClassLoader.TYPE_java_lang_String) {
                    return this.compileStringConcatenation(bo, this.compileStringOperand(first), second, operands);
                }
            }
        }

        // Implements "| ^ & * / % + - << >> >>>".
        return this.compileArithmeticOperation(
            bo,                         // locatable
            null,                       // type
            bo.unrollLeftAssociation(), // operands
            bo.operator                 // operator
        );
    }

    private IType
    compileGet2(Cast c) throws CompileException {

        // JLS7 5.5 Casting Conversion.
        IType tt = this.getType2(c);
        if (this.isPolyExpression(c.value)) this.setLambdaTargetType(c.value, tt);
        IType sourceType = this.getType(c.value);
        IType vt;
        if (!UnitCompiler.isPrimitive(sourceType) && !UnitCompiler.isPrimitive(tt)) {
            // An explicit cast checks its own target, not a substituted intermediate invocation result.
            // Keep all source-level legality checks before compiling the value with its JVM carrier.
            boolean compatible = sourceType == IClass.NULL;
            if (!compatible) {
                if (tt instanceof IntersectionIClass) {
                    compatible = true;
                    for (IClass bound : ((IntersectionIClass) tt).bounds) {
                        if (!this.isCastReferenceConvertible(sourceType, bound)) compatible = false;
                    }
                } else if (tt instanceof SourceTypeVariable || tt instanceof GenericArrayType) {
                    compatible = this.isTypeVariableCastConvertible(sourceType, tt, new HashSet<TypeParameter>());
                } else compatible = this.isCastReferenceConvertible(sourceType, tt);
            }
            if (!compatible) throw UnitCompiler.compileException(c, "Cannot cast \"" + sourceType + "\" to \"" + tt + "\"");
            vt = this.compileGetReferenceValue(c.value);
        } else vt = this.compileGetValue(c.value);
        if (this.getTargetVersion() >= 8 && !(tt instanceof IntersectionIClass)) {
            this.annotationCompiler.compileCodeTypeAnnotations(c.targetType, new Annotation[0], (byte) 0x47,
                this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
        }
        if (tt instanceof IntersectionIClass) {
            IClass[] bounds = ((IntersectionIClass) tt).bounds;
            IClass boxed = this.isBoxingConvertible(vt);
            if (boxed != null) {
                this.boxingConversion(c, vt, boxed);
                vt = boxed;
            }
            for (int i = 0; i < bounds.length; i++) {
                IClass bound = bounds[i];
                if (this.getTargetVersion() >= 8) this.annotationCompiler.compileCodeTypeAnnotations(
                    i == 0 ? c.targetType : c.additionalBounds[i - 1], new Annotation[0], (byte) 0x47,
                    this.getCodeContext(), this.getCodeContext().newOffset(), null, i);
                if (vt != IClass.NULL && !this.isCastReferenceConvertible(vt, bound)) {
                    throw UnitCompiler.compileException(c, "Incompatible intersection bound " + bound);
                }
                this.bytecodeEmitter.checkcast(c, bound);
            }
            this.bytecodeEmitter.checkcast(c, bounds[0]);
            return tt;
        }

        if (!UnitCompiler.isPrimitive(sourceType) && !UnitCompiler.isPrimitive(tt)) {
            // Source legality was checked above. Unchecked reference casts need only their erased JVM target.
            if (UnitCompiler.rawTypeOf(tt).isAssignableFrom(UnitCompiler.rawTypeOf(vt))) {
                this.getCodeContext().popOperand(UnitCompiler.rawTypeOf(vt).getDescriptor());
                this.getCodeContext().pushOperand(UnitCompiler.rawTypeOf(tt).getDescriptor());
            } else this.bytecodeEmitter.checkcast(c, tt);
            return tt;
        }

        if (this.tryCastConversion(c, vt, tt, this.constantEvaluator.getConstantValue2(c.value))) return tt;

        // JAVAC obviously also permits 'boxing conversion followed by widening reference conversion' and 'unboxing
        // conversion followed by widening primitive conversion', although these are not described in JLS7 5.5. For the
        // sake of compatibility, we implement them.
        // See also http://jira.codehaus.org/browse/JANINO-153
        {
            IClass boxedType = this.isBoxingConvertible(vt);
            if (boxedType != null && !(tt instanceof SourceTypeVariable) && this.isWideningReferenceConvertible(boxedType, tt)) {
                this.boxingConversion(c, vt, boxedType);
                return tt;
            }

            IClass unboxedType = this.isUnboxingConvertible(vt);
            if (unboxedType != null && this.isWideningPrimitiveConvertible(unboxedType, tt)) {
                this.unboxingConversion(c, vt, unboxedType);
                this.tryWideningPrimitiveConversion(c, unboxedType, tt);
                return tt;
            }
            // Casting contexts permit narrowing a reference to the target's wrapper before unboxing.
            IClass wrapper = this.isBoxingConvertible(tt);
            if (wrapper != null && this.tryNarrowingReferenceConversion(c, vt, wrapper)) {
                this.unboxingConversion(c, wrapper, UnitCompiler.rawTypeOf(tt));
                return tt;
            }
        }

        this.compileError("Cannot cast \"" + vt + "\" to \"" + tt + "\"", c.getLocation());
        return tt;
    }

    private IType
    compileGet2(ParenthesizedExpression pe) throws CompileException {
        return this.compileResolvedReference(pe, pe.value);
    }

    private IType
    compileGet2(MethodInvocation mi) throws CompileException {
        return this.compileGetInvocation(mi, false);
    }

    private IType compileGetInvocation(MethodInvocation mi, boolean discardResult) throws CompileException {
        List<MethodInvocation> chain = new ArrayList<>();
        for (MethodInvocation call = mi;;) {
            chain.add(call);
            if (!(call.target instanceof MethodInvocation)) break;
            call = (MethodInvocation) call.target;
        }
        IType result = IClass.VOID;
        for (int i = chain.size() - 1; i >= 0; i--) {
            result = this.compileMethodInvocation(chain.get(i), i < chain.size() - 1, discardResult && i == 0);
        }
        return result;
    }

    /** Emit one link of a receiver chain, preserving receiver/argument evaluation order. */
    private IType
    compileMethodInvocation(MethodInvocation mi, boolean receiverOnStack, boolean discardResult) throws CompileException {
        IClass.IMethod iMethod = mi.iMethod == null ? this.findIMethod(mi) : mi.iMethod;

        // Compute the objectref for an instance method.
        Atom ot = mi.target;
        if (receiverOnStack) {
            // A static method invoked through an expression still evaluates that expression.
            if (iMethod.isStatic()) this.bytecodeEmitter.pop(mi, this.getType(ot));
        } else if (ot == null) {

            // JLS7 6.5.7.1, 15.12.4.1.1.1
            TypeBodyDeclaration     scopeTbd;
            AbstractTypeDeclaration scopeTypeDeclaration;
            {
                Scope s;
                for (
                    s = mi.getEnclosingScope();
                    !(s instanceof TypeBodyDeclaration);
                    s = s.getEnclosingScope()
                );
                scopeTbd = (TypeBodyDeclaration) s;
                if (!(s instanceof AbstractTypeDeclaration)) s = s.getEnclosingScope();
                scopeTypeDeclaration = (AbstractTypeDeclaration) s;
            }
            if (iMethod.isStatic()) {
                this.warning(
                    "IASM",
                    "Implicit access to static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );
                // JLS7 15.12.4.1.1.1.1
                ;
            } else {
                this.warning(
                    "IANSM",
                    "Implicit access to non-static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );

                // JLS7 15.12.4.1.1.1.2
                if (UnitCompiler.isStaticContext(scopeTbd)) {
                    this.compileError(
                        "Instance method \"" + iMethod.toString() + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }

                this.referenceThis(
                    mi,                          // locatable
                    scopeTypeDeclaration,        // declaringType
                    scopeTbd,                    // declaringTypeBodyDeclaration
                    iMethod.getDeclaringIClass() // targetIClass
                );
            }
        } else {

            // 6.5.7.2
            if (this.isType(ot)) {

                // The target is a type; thus the method must be static.
                // JLS9 15.12.4.1.2:
                this.getType(this.toTypeOrCompileException(ot));
                if (!iMethod.isStatic()) {
                    this.compileError(
                        "Instance method \"" + mi.methodName + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }
            } else
            {

                // The target is an rvalue.
                Rvalue rot = this.toRvalueOrCompileException(ot);
                if (iMethod.isStatic()) {

                    // JLS9 15.12.4.1.3.1 and .4.1:
                    if (!UnitCompiler.mayHaveSideEffects(rot)) {

                        // The rvalue is guaranteed to have no side effects, so we can save the code to evaluate it
                        // and then discard the result.
                        ;
                    } else {

                        // Evaluate the target expression and then discard the result.
                        this.bytecodeEmitter.pop(ot, this.compileGetValue(rot));
                    }
                } else {

                    // JLS9 15.12.4.1.3.2 and .4.2
                    IType receiverType = this.compileGetValue(rot);
                    if (!iMethod.getDeclaringIClass().isAssignableFrom(UnitCompiler.rawTypeOf(receiverType))) {
                        this.bytecodeEmitter.checkcast(mi, iMethod.getDeclaringIClass());
                    }

                    if (this.getCodeContext().peekNullOperand()) {
                        this.compileError("Method invocation target is always null");
                        this.getCodeContext().popOperand();
                        this.getCodeContext().pushObjectOperand(iMethod.getDeclaringIClass().getDescriptor());
                    }
                }
            }
        }

        // Evaluate method parameters (JLS7 15.12.4.2).
        // If this method is vararg, rewritten all args starting from lastParamIndex to the end as if they were elements
        // of an array.
        IClass[]  parameterTypes = iMethod.getParameterTypes();
        Rvalue[]  adjustedArgs   = null;
        final int actualSize     = mi.arguments.length;
        if (iMethod.isVarargs() && mi.argumentsNeedAdjust) {
            adjustedArgs = new Rvalue[parameterTypes.length];
            Rvalue[]       lastArgs = new Rvalue[actualSize - parameterTypes.length + 1];
            final Location loc      = mi.getLocation();

            if (lastArgs.length > 0) {
                for (int i = 0, j = parameterTypes.length - 1; i < lastArgs.length; ++i, ++j) {
                    lastArgs[i] = mi.arguments[j];
                }
            }

            for (int i = parameterTypes.length - 2; i >= 0; --i) {
                adjustedArgs[i] = mi.arguments[i];
            }
            adjustedArgs[adjustedArgs.length - 1] = new NewInitializedArray(
                loc,                                       // location
                parameterTypes[parameterTypes.length - 1], // arrayIClass
                new ArrayInitializer(loc, lastArgs)        // arrayInitializer
            );
        } else {
            adjustedArgs = mi.arguments;
        }

        IType[] targetTypes = this.invocationTargetTypes(mi, iMethod);
        for (int i = 0; i < adjustedArgs.length; ++i) {
            this.setLambdaTargetType(adjustedArgs[i], targetTypes[i]);
            this.assignmentConversion(
                mi,                                    // location
                this.compileGetValue(adjustedArgs[i]), // sourceType
                parameterTypes[i],                     // targetType
                this.constantEvaluator.getConstantValue(adjustedArgs[i]) // constantValue
            );
        }
        // Invoke!
        this.compileReferenceAnnotations(mi);
        if (this.getTargetVersion() >= 8 && mi.typeArguments != null && !this.referenceDelegates.contains(mi)) {
            CodeContext.Offset offset = this.getCodeContext().newOffset();
            for (int i = 0; i < mi.typeArguments.length; i++) if (mi.typeArguments[i] instanceof Type) {
                this.annotationCompiler.compileCodeTypeAnnotations((Type) mi.typeArguments[i], new Annotation[0], (byte) 0x49,
                    this.getCodeContext(), offset, null, i);
            }
        }
        IType invocationReceiver = this.invocationReceiver(mi, iMethod.getDeclaringIClass());
        if (!this.isAccessible(iMethod, mi.getEnclosingScope(), invocationReceiver)) {
            this.checkAccessible(iMethod, mi.getEnclosingScope(), mi.getLocation());
        }
        if (!iMethod.getDeclaringIClass().isInterface() && !iMethod.isStatic() && iMethod.getAccess() == Access.PRIVATE) {

            // The original private method is retained. Its package-visible static nest accessor
            // prepends the receiver and invokes the original with invokespecial.
            this.bytecodeEmitter.invoke(
                mi,                                           // locatable
                Opcode.INVOKESTATIC,                          // opcode
                iMethod.getDeclaringIClass(),                 // declaringIClass
                iMethod.getName() + '$',                      // methodName
                iMethod.getDescriptor().prependParameter(     // methodMd
                    iMethod.getDeclaringIClass().getDescriptor()
                ),
                false                                         // useInterfaceMethodref
            );
        } else
        {
            if (!this.isAccessible(iMethod.getDeclaringIClass(), mi.getEnclosingScope()) && invocationReceiver != null) {
                IClass owner = UnitCompiler.rawTypeOf(invocationReceiver);
                this.bytecodeEmitter.invoke(mi, iMethod.isStatic() ? Opcode.INVOKESTATIC
                    : owner.isInterface() ? Opcode.INVOKEINTERFACE : Opcode.INVOKEVIRTUAL,
                    owner, iMethod.getName(), iMethod.getDescriptor(), owner.isInterface());
            } else this.bytecodeEmitter.invokeMethod(mi, iMethod);
        }

        return this.adaptReferenceResult(mi, this.substitutedReturnType(mi, iMethod), iMethod.getReturnType(),
            this.invocationTargets.get(mi), discardResult);
    }

    /**
     * @return The substituted parameter type of the single abstract method for an INFERRED LAMBDA parameter, or
     *         <var>erasedType</var> if no substitution is possible
     */
    @Nullable private IClass
    substitutedSamParameterType(IType targetType, IClass.IMethod sam, int parameterIndex, IClass erasedType)
    throws CompileException {

        IType type = this.functionalParameters(targetType, sam)[parameterIndex];
        return type instanceof IClass ? (IClass) type : UnitCompiler.rawTypeOf(type);
    }

    /**
     * @return The declared (generic) parameter types of the given method, or {@code null}
     */
    private void
    collectGenericVariables(GenericType type, Set<String> variables) {
        if (type instanceof GenericType.TypeVariable) variables.add(((GenericType.TypeVariable) type).name);
        else if (type instanceof GenericType.ArrayOf) this.collectGenericVariables(((GenericType.ArrayOf) type).componentType, variables);
        else if (type instanceof GenericType.Wildcard) this.collectGenericVariables(((GenericType.Wildcard) type).bound, variables);
        else if (type instanceof GenericType.Parameterized) {
            GenericType owner = ((GenericType.Parameterized) type).ownerType;
            if (owner != null) this.collectGenericVariables(owner, variables);
            for (GenericType argument : ((GenericType.Parameterized) type).typeArguments) this.collectGenericVariables(argument, variables);
        }
    }

    private final Map<IClass, AbstractTypeDeclaration> sourceDeclarations = new java.util.IdentityHashMap<>();
    private final Map<IMethod, MethodDeclarator> sourceMethods = new java.util.IdentityHashMap<>();

    private void bindTypeVariables(@Nullable IType type, Map<String, IType> environment) throws CompileException {
        if (!(type instanceof ParameterizedIClass)) return;
        ParameterizedIClass parameterized = (ParameterizedIClass) type;
        this.bindTypeVariables(parameterized.ownerType, environment);
        ITypeVariable[] variables = parameterized.rawType.getITypeVariables();
        if (variables.length == parameterized.actualTypeArguments.length) for (int i = 0; i < variables.length; i++) {
            environment.put(variables[i].getName(), parameterized.actualTypeArguments[i]);
        }
    }

    private void bindGenericVariables(@Nullable GenericType type, Map<String, GenericType> environment) throws CompileException {
        if (!(type instanceof GenericType.Parameterized)) return;
        GenericType.Parameterized parameterized = (GenericType.Parameterized) type;
        this.bindGenericVariables(parameterized.ownerType, environment);
        ITypeVariable[] variables = parameterized.rawType.getITypeVariables();
        if (variables.length == parameterized.typeArguments.length) for (int i = 0; i < variables.length; i++) {
            environment.put(variables[i].getName(), parameterized.typeArguments[i]);
        }
    }

    /** View a receiver as the declaration that owns an inherited generic member. */
    @Nullable private IType
    asSupertype(IType receiver, IClass declaration) throws CompileException {
        return this.asSupertype(receiver, declaration, new HashSet<IClass>());
    }

    @Nullable private IType
    asSupertype(IType receiver, IClass declaration, Set<IClass> visited) throws CompileException {
        if (receiver instanceof CapturedType) {
            for (IType bound : ((CapturedType) receiver).bounds) {
                IType found = this.asSupertype(bound, declaration, visited);
                if (found != null) return found;
            }
            return null;
        }
        if (receiver instanceof SourceTypeVariable) {
            ReferenceType[] bounds = ((SourceTypeVariable) receiver).declaration.bound;
            if (bounds != null) for (ReferenceType bound : bounds) {
                IType found = this.asSupertype(this.sourceBoundType(bound), declaration, visited);
                if (found != null) return found;
            }
            return declaration == this.iClassLoader.TYPE_java_lang_Object ? declaration : null;
        }
        IClass raw = UnitCompiler.rawTypeOf(receiver);
        if (raw == declaration) return receiver;
        if (!visited.add(raw)) return null;
        Map<String, IType> environment = new HashMap<>();
        AbstractTypeDeclaration source = this.sourceDeclarations.get(raw);
        if (source != null) for (Scope scope = source.getEnclosingScope(); !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            if (!(scope instanceof FunctionDeclarator)) continue;
            TypeParameter[] parameters = ((FunctionDeclarator) scope).getOptionalTypeParameters();
            if (parameters != null) for (TypeParameter parameter : parameters) if (!environment.containsKey(parameter.name)) {
                environment.put(parameter.name, this.sourceVariable(parameter, scope));
            }
        }
        // Local and anonymous supertypes may mention method variables that are not parameters
        // of the receiver class. Its own variables, when present, take precedence.
        for (ITypeVariable variable : raw.getITypeVariables()) environment.remove(variable.getName());
        this.bindTypeVariables(receiver, environment);
        for (GenericType parent : raw.getGenericSupertypes()) {
            if (parent == null) continue;
            IType type = this.substituteGenericType(parent, environment);
            if (type == null) continue;
            IType found = this.asSupertype(type, declaration, visited);
            if (found != null) return found;
        }
        return null;
    }

    private IType declarationReceiver(IClass type) throws CompileException {
        AbstractTypeDeclaration source = this.sourceDeclarations.get(type);
        if (source != null) return this.lexicalReceiver(type, source);
        ITypeVariable[] variables = type.getITypeVariables();
        if (variables.length == 0) return type;
        IType[] arguments = new IType[variables.length];
        Map<String, IType> environment = new HashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            CapturedType variable = new CapturedType(this.iClassLoader.TYPE_java_lang_Object);
            variable.intersection = true;
            arguments[i] = variable; environment.put(variables[i].getName(), variable);
        }
        Map<String, GenericType[]> bounds = type.getTypeParameterBounds();
        for (int i = 0; i < arguments.length; i++) {
            GenericType[] declared = bounds.get(variables[i].getName());
            List<IType> limits = new ArrayList<>();
            if (declared != null) for (GenericType bound : declared) {
                IType limit = this.substituteGenericType(bound, environment);
                if (limit != null) limits.add(limit);
            }
            if (!limits.isEmpty()) {
                CapturedType variable = (CapturedType) arguments[i];
                variable.bounds = limits.toArray(new IType[limits.size()]); variable.upper = limits.get(0);
            }
        }
        return new ParameterizedIClass(type, arguments);
    }

    private boolean compatibleOverrideReturn(IMethod method, IMethod base) throws CompileException {
        return this.compatibleOverrideReturn(method, base, method.getDeclaringIClass());
    }

    private boolean compatibleOverrideReturn(IMethod method, IMethod base, IClass type) throws CompileException {
        IClass erased = method.getReturnType(), inheritedErased = base.getReturnType();
        if (erased.isPrimitive() || inheritedErased.isPrimitive()) return erased == inheritedErased;
        IType receiver = this.declarationReceiver(type);
        GenericType own = this.getGenericReturnType(method), inherited = this.getGenericReturnType(base);
        Map<String, IType> ownEnvironment = this.receiverEnvironment(receiver, method.getDeclaringIClass());
        Map<String, IType> inheritedEnvironment = this.receiverEnvironment(receiver, base.getDeclaringIClass());
        Set<String> ownVariables = this.getMethodTypeParameterNames(method), inheritedVariables = this.getMethodTypeParameterNames(base);
        if (ownVariables != null) ownEnvironment.keySet().removeAll(ownVariables);
        if (inheritedVariables != null) inheritedEnvironment.keySet().removeAll(inheritedVariables);
        IType actual = own == null ? null : this.substituteGenericType(own, ownEnvironment);
        IType expected = inherited == null ? null : this.substituteGenericType(inherited, inheritedEnvironment);
        return actual == null || expected == null ? inheritedErased.isAssignableFrom(erased) : this.genericConvertible(actual, expected, false);
    }

    @Nullable private IMethod
    findGenericOverride(IClass type, IMethod base) throws CompileException {
        IType self = this.declarationReceiver(type);
        Map<String, IType> environment = this.receiverEnvironment(self, base.getDeclaringIClass());
        Set<String> methodVariables = this.getMethodTypeParameterNames(base);
        if (methodVariables != null) environment.keySet().removeAll(methodVariables);
        GenericType[] genericParameters = this.getGenericParameterTypes(base);
        if (genericParameters == null) return null;
        IClass[] parameters = new IClass[genericParameters.length];
        for (int i = 0; i < parameters.length; i++) {
            IType parameter = this.substituteGenericType(genericParameters[i], environment);
            if (parameter == null) {
                // A raw source superclass still supplies its generic contract's erased bridge.
                // Its lexical type retains the variable bounds that a raw child no longer has.
                IClass parent = type.getSuperclass();
                if (parent != null && base.getDeclaringIClass().isAssignableFrom(parent)) {
                    IMethod inherited = this.findGenericOverride(parent, base);
                    if (inherited != null && !inherited.isAbstract()) return inherited;
                }
                return null;
            }
            parameters[i] = UnitCompiler.rawTypeOf(parameter);
        }
        IMethod candidate = type.findIMethod(base.getName(), parameters);
        if (candidate != null && candidate.getDeclaringIClass().isInterface()) {
            for (IMethod inherited : type.getIMethods()) {
                if (inherited.isStatic() || !inherited.getDeclaringIClass().isInterface()
                    || !inherited.getName().equals(base.getName()) || !Arrays.equals(inherited.getParameterTypes(), parameters)) continue;
                if (candidate.getDeclaringIClass() != inherited.getDeclaringIClass()
                    && candidate.getDeclaringIClass().isAssignableFrom(inherited.getDeclaringIClass())) candidate = inherited;
            }
        }
        if (candidate != null && !candidate.isStatic() && (!candidate.isAbstract() || candidate.getDeclaringIClass() == type)) return candidate;
        // A source superclass may not have its erased bridge yet (Enum<E>.compareTo(E), for example).
        for (IMethod inherited : type.getIMethods()) {
            if (inherited.isStatic() || !inherited.getName().equals(base.getName())) continue;
            GenericType[] declared = this.getGenericParameterTypes(inherited);
            if (declared == null || declared.length != parameters.length) continue;
            Map<String, IType> inheritedEnvironment = this.receiverEnvironment(self, inherited.getDeclaringIClass());
            Set<String> variables = this.getMethodTypeParameterNames(inherited);
            if (variables != null) inheritedEnvironment.keySet().removeAll(variables);
            boolean matches = true;
            for (int i = 0; i < parameters.length; i++) {
                IType parameter = this.substituteGenericType(declared[i], inheritedEnvironment);
                if (parameter == null || UnitCompiler.rawTypeOf(parameter) != parameters[i]) matches = false;
            }
            if (matches && !inherited.isAbstract()) return inherited;
        }
        // An intermediate class can implement the erased specialization of its own
        // variable (Mid<T extends Mid<T>>), which remains an override in Child extends Mid<Child>.
        IClass parent = type.getSuperclass();
        if (parent != null && base.getDeclaringIClass().isAssignableFrom(parent)) {
            IMethod inherited = this.findGenericOverride(parent, base);
            if (inherited != null && !inherited.isAbstract()) return inherited;
        }
        return candidate != null && !candidate.isStatic() ? candidate : null;
    }

    private GenericType sourceGenericType(Type type) throws CompileException {
        if (type instanceof ArrayType) return new GenericType.ArrayOf(this.sourceGenericType(((ArrayType) type).componentType));
        if (type instanceof ReferenceType) return this.sourceReferenceType((ReferenceType) type, ((ReferenceType) type).identifiers.length);
        return this.genericTypeOf(this.getType(type));
    }

    private GenericType sourceTypeArgument(TypeArgument argument) throws CompileException {
        if (argument instanceof Type) return this.sourceGenericType((Type) argument);
        if (argument instanceof Wildcard) {
            Wildcard wildcard = (Wildcard) argument;
            GenericType bound = wildcard.referenceType == null ? new GenericType.Raw(this.iClassLoader.TYPE_java_lang_Object)
                : this.sourceGenericType(wildcard.referenceType);
            return new GenericType.Wildcard(wildcard.bounds != Wildcard.BOUNDS_SUPER, bound);
        }
        return GenericType.ERASED;
    }

    private GenericType sourceReferenceType(ReferenceType reference, int segments) throws CompileException {
        if (segments == 1 && reference.identifiers.length == 1 && UnitCompiler.isTypeVariableOfEnclosingDeclaration(reference)) {
            return new GenericType.TypeVariable(reference.identifiers[0]);
        }
        IClass raw = UnitCompiler.rawTypeOf(this.getReferenceType(reference.getLocation(), reference.getEnclosingScope(),
            reference.identifiers, segments, null));
        IClass outer = raw.getOuterIClass();
        GenericType owner = null;
        if (outer != null) {
            owner = segments > 1 ? this.sourceReferenceType(reference, segments - 1)
                : this.genericTypeOf(this.lexicalMemberOwner(outer, reference.getEnclosingScope()));
            if (owner instanceof GenericType.Parameterized && ((GenericType.Parameterized) owner).rawType != outer) {
                GenericType.Parameterized view = this.genericPatternSupertype((GenericType.Parameterized) owner, outer, new HashSet<IClass>());
                owner = view == null ? new GenericType.Raw(outer) : view;
            }
        }
        TypeArgument[] declared = reference.segmentTypeArguments[segments - 1];
        if (declared == null && !(owner instanceof GenericType.Parameterized)) return new GenericType.Raw(raw);
        GenericType[] arguments = new GenericType[declared == null ? 0 : declared.length];
        for (int i = 0; i < arguments.length; i++) arguments[i] = this.sourceTypeArgument(declared[i]);
        return new GenericType.Parameterized(raw, arguments, owner);
    }

    private GenericType genericTypeOf(IType type) throws CompileException {
        if (type instanceof SourceTypeVariable) return new GenericType.TypeVariable(((SourceTypeVariable) type).declaration.name);
        if (type instanceof GenericArrayType) return new GenericType.ArrayOf(this.genericTypeOf(((GenericArrayType) type).component));
        if (type instanceof IWildcardType) {
            IType lower = ((IWildcardType) type).getLowerBound();
            return new GenericType.Wildcard(lower == null, this.genericTypeOf(lower == null ? ((IWildcardType) type).getUpperBound() : lower));
        }
        if (type instanceof ParameterizedIClass) {
            ParameterizedIClass parameterized = (ParameterizedIClass) type;
            GenericType[] arguments = new GenericType[parameterized.actualTypeArguments.length];
            for (int i = 0; i < arguments.length; i++) arguments[i] = this.genericTypeOf(parameterized.actualTypeArguments[i]);
            return new GenericType.Parameterized(parameterized.rawType, arguments,
                parameterized.ownerType == null ? null : this.genericTypeOf(parameterized.ownerType));
        }
        return new GenericType.Raw(UnitCompiler.rawTypeOf(type));
    }

    private IType sourceBoundType(ReferenceType bound) throws CompileException {
        GenericType pattern = this.sourceGenericType(bound);
        Set<String> names = new HashSet<>();
        this.collectGenericVariables(pattern, names);
        Map<String, IType> environment = new HashMap<>();
        for (String name : names) {
            ReferenceType reference = new ReferenceType(bound.getLocation(), new Annotation[0], new String[] { name }, null);
            reference.setEnclosingScope(bound.getEnclosingScope());
            SourceTypeVariable variable = this.lexicalVariable(reference);
            if (variable != null) environment.put(name, variable);
        }
        IType result = this.substituteGenericType(pattern, environment);
        return result == null ? this.getHierarchyType(bound) : result;
    }

    @Nullable private GenericType.Parameterized genericPatternSupertype(GenericType.Parameterized pattern, IClass owner, Set<IClass> visited) throws CompileException {
        if (pattern.rawType == owner) return pattern;
        if (!visited.add(pattern.rawType)) return null;
        Map<String, GenericType> environment = new HashMap<>();
        this.bindGenericVariables(pattern, environment);
        for (GenericType parent : pattern.rawType.getGenericSupertypes()) {
            if (parent == null) continue;
            GenericType type = this.functionalPattern(parent, environment);
            if (type instanceof GenericType.Parameterized) {
                GenericType.Parameterized result = this.genericPatternSupertype((GenericType.Parameterized) type, owner, visited);
                if (result != null) return result;
            }
        }
        return null;
    }

    /** Candidate-local constraints; probing a reference never assigns its target or desugars its AST. */
    private final class InferenceContext {
        final Set<String> variables;
        final Set<String> explicitVariables;
        final Map<String, GenericType[]> bounds;
        @Nullable final GenericType creationPattern;
        final Map<String, IType> values;
        final Map<String, List<IType>> lowers = new HashMap<>();
        final Map<String, List<IType>> uppers = new HashMap<>();
        final Map<String, IType> equals = new HashMap<>();
        final Map<LambdaExpression, IType[]> lambdaInputs = new java.util.IdentityHashMap<>();
        final Map<LambdaExpression, List<Rvalue>> lambdaResults = new java.util.IdentityHashMap<>();
        boolean valid = true;
        @Nullable IType[] parameterTargets;

        InferenceContext(IInvocable method, @Nullable IType receiver) throws CompileException { this(method, receiver, false); }

        InferenceContext(IInvocable method, @Nullable IType receiver, boolean inferClass) throws CompileException {
            Set<String> names = UnitCompiler.this.getMethodTypeParameterNames(method);
            this.explicitVariables = names == null ? Collections.<String>emptySet() : names;
            this.variables = new LinkedHashSet<>();
            this.bounds = new LinkedHashMap<>(UnitCompiler.this.methodBounds(method));
            this.values = UnitCompiler.this.receiverEnvironment(receiver, method.getDeclaringIClass());
            if (inferClass) {
                IClass raw = method.getDeclaringIClass();
                ITypeVariable[] classVariables = raw.getITypeVariables();
                Map<String, GenericType> renames = new HashMap<>();
                GenericType[] arguments = new GenericType[classVariables.length];
                for (int i = 0; i < arguments.length; i++) {
                    String name = classVariables[i].getName();
                    String key = this.explicitVariables.contains(name) ? "janino$diamond$" + name : name;
                    this.variables.add(key); this.values.remove(name);
                    renames.put(name, arguments[i] = new GenericType.TypeVariable(key));
                }
                for (Map.Entry<String, GenericType[]> entry : raw.getTypeParameterBounds().entrySet()) {
                    GenericType[] limits = entry.getValue(), copy = new GenericType[limits.length];
                    for (int i = 0; i < copy.length; i++) copy[i] = UnitCompiler.this.functionalPattern(limits[i], renames);
                    this.bounds.put(((GenericType.TypeVariable) renames.get(entry.getKey())).name, copy);
                }
                IType owner = receiver instanceof ParameterizedIClass ? ((ParameterizedIClass) receiver).ownerType : null;
                this.creationPattern = new GenericType.Parameterized(raw, arguments, owner == null ? null : UnitCompiler.this.genericTypeOf(owner));
            } else this.creationPattern = null;
            this.variables.addAll(this.explicitVariables);
            this.values.keySet().removeAll(this.variables);
        }

        // relation: actual <: formal (0), actual = formal (1), formal <: actual (-1).
        void constrain(@Nullable GenericType formal, @Nullable IType actual, int relation) throws CompileException {
            if (formal == null || actual == null || actual == IClass.NULL || formal == GenericType.ERASED) return;
            if (formal instanceof GenericType.Wildcard) {
                GenericType.Wildcard wildcard = (GenericType.Wildcard) formal;
                if (actual instanceof IWildcardType) actual = wildcard.upperBound
                    ? ((IWildcardType) actual).getUpperBound() : ((IWildcardType) actual).getLowerBound();
                if (actual instanceof CapturedType) actual = wildcard.upperBound
                    ? ((CapturedType) actual).upper : ((CapturedType) actual).lower;
                // Return-target containment reverses the usual argument constraint:
                // Class<? extends U> <: Class<? extends Object> implies U <: Object.
                this.constrain(wildcard.bound, actual,
                    wildcard.upperBound ? (relation == -1 ? -1 : 0) : (relation == -1 ? 0 : -1));
            } else if (formal instanceof GenericType.TypeVariable) {
                String name = ((GenericType.TypeVariable) formal).name;
                if (!this.variables.contains(name)) return;
                if (UnitCompiler.isPrimitive(actual)) actual = UnitCompiler.this.wrapperOf(UnitCompiler.rawTypeOf(actual));
                if (relation == 1) {
                    IType old = this.equals.get(name);
                    if (old != null && !old.equals(actual)) this.valid = false;
                    this.equals.put(name, actual);
                } else {
                    Map<String, List<IType>> constraints = relation == 0 ? this.lowers : this.uppers;
                    List<IType> list = constraints.get(name);
                    if (list == null) constraints.put(name, list = new ArrayList<>());
                    if (!list.contains(actual)) list.add(actual);
                }
            } else if (formal instanceof GenericType.ArrayOf && UnitCompiler.isArray(actual)) {
                this.constrain(((GenericType.ArrayOf) formal).componentType, UnitCompiler.getComponentType(actual), relation);
            } else if (formal instanceof GenericType.Parameterized) {
                GenericType.Parameterized parameter = (GenericType.Parameterized) formal;
                if (relation == -1 && UnitCompiler.rawTypeOf(actual) != parameter.rawType) {
                    GenericType.Parameterized projected = UnitCompiler.this.genericPatternSupertype(parameter, UnitCompiler.rawTypeOf(actual), new HashSet<IClass>());
                    if (projected != null) { this.constrain(projected, actual, relation); return; }
                }
                IType view = UnitCompiler.this.asSupertype(actual, parameter.rawType);
                if (!(view instanceof ParameterizedIClass)) return; // Raw arguments retain unchecked compatibility.
                ParameterizedIClass actualView = (ParameterizedIClass) view;
                this.constrain(parameter.ownerType, actualView.ownerType, relation);
                IType[] arguments = actualView.actualTypeArguments;
                if (arguments.length != parameter.typeArguments.length) return;
                for (int i = 0; i < arguments.length; i++) {
                    IType argument = arguments[i];
                    if (relation == -1 && argument instanceof IWildcardType) {
                        IWildcardType wildcard = (IWildcardType) argument;
                        IType lower = wildcard.getLowerBound();
                        this.constrain(parameter.typeArguments[i], lower == null ? wildcard.getUpperBound() : lower, lower == null ? -1 : 0);
                    } else this.constrain(parameter.typeArguments[i], argument, 1);
                }
            }
        }

        Map<String, IType> solve(IInvocable method) throws CompileException {
            Map<String, GenericType[]> bounds = this.bounds;
            // Entries provisionally supplied to nested arguments are not resolved instantiations.
            this.values.keySet().removeAll(this.variables);
            this.values.putAll(this.equals);
            // Incorporate actual arguments/targets before choosing defaults. E.g. String <: T and
            // T <: Comparable<U> imply U = String; Integer <: T <: U implies Integer <: U.
            int previousSize;
            do {
                previousSize = this.constraintCount();
                for (String name : this.variables) {
                    IType value = this.equals.get(name);
                    List<IType> lower = this.lowers.get(name), upper = this.uppers.get(name);
                    if (value == null && lower != null) for (IType type : lower) {
                        value = value == null ? type : UnitCompiler.this.commonSupertype(value, type);
                    }
                    if (value == null && upper != null) for (IType type : upper) {
                        if (type != UnitCompiler.this.iClassLoader.TYPE_java_lang_Object
                            && this.canInstantiateUpper(name, type)
                            && (value == null || UnitCompiler.this.genericConvertible(type, value, false))) value = type;
                    }
                    if (value != null) this.values.put(name, value);
                }
                for (Map.Entry<String, GenericType[]> entry : bounds.entrySet()) {
                    IType value = this.values.get(entry.getKey());
                    for (GenericType bound : entry.getValue()) {
                        if (value != null) this.constrain(bound, value, 0);
                        // An instantiated upper variable also bounds its dependent lower variable.
                        if (bound instanceof GenericType.TypeVariable) {
                            IType limit = this.values.get(((GenericType.TypeVariable) bound).name);
                            if (limit != null) this.constrain(new GenericType.TypeVariable(entry.getKey()), limit, -1);
                        }
                    }
                }
            } while (this.constraintCount() != previousSize && this.valid);

            Map<String, CapturedType> recursive = new HashMap<>();
            for (String name : this.variables) this.resolve(name, new ArrayList<String>(), recursive);
            for (String name : recursive.keySet()) {
                List<IType> limits = new ArrayList<>();
                GenericType[] declared = bounds.get(name);
                if (declared != null) for (GenericType bound : declared) {
                    IType limit = UnitCompiler.this.substituteGenericType(bound, this.values);
                    if (limit != null) limits.add(limit);
                }
                List<IType> upper = this.uppers.get(name);
                if (upper != null) limits.addAll(upper);
                CapturedType capture = recursive.get(name);
                if (!limits.isEmpty()) {
                    capture.bounds = limits.toArray(new IType[limits.size()]); capture.upper = limits.get(0);
                    for (IType limit : limits) {
                        IClass a = UnitCompiler.rawTypeOf(capture.upper), b = UnitCompiler.rawTypeOf(limit);
                        if (a.isAssignableFrom(b) || a.isInterface() && !b.isInterface()
                            && b != UnitCompiler.this.iClassLoader.TYPE_java_lang_Object) capture.upper = limit;
                    }
                }
            }
            for (String name : this.variables) {
                IType result = this.values.get(name);
                List<IType> lower = this.lowers.get(name), upper = this.uppers.get(name);
                if (result == null) throw new InternalCompilerException("Unresolved inference variable " + name);
                if (recursive.containsKey(name)) {
                    IType[] limits = recursive.get(name).bounds;
                    for (int i = 0; i < limits.length; i++) for (int j = 0; j < i; j++) {
                        IClass a = UnitCompiler.rawTypeOf(limits[i]), b = UnitCompiler.rawTypeOf(limits[j]);
                        boolean related = a.isAssignableFrom(b) || b.isAssignableFrom(a);
                        if (!related && (!a.isInterface() && !b.isInterface() || a.isFinal() || b.isFinal())) this.valid = false;
                        if (a == b && limits[i] instanceof ParameterizedIClass && limits[j] instanceof ParameterizedIClass
                            && !UnitCompiler.this.genericConvertible(limits[i], limits[j], false)
                            && !UnitCompiler.this.genericConvertible(limits[j], limits[i], false)) this.valid = false;
                    }
                }
                if (!this.equals.containsKey(name) && lower == null) {
                    GenericType[] exceptions = method.getGenericThrownExceptions();
                    boolean thrown = false;
                    if (exceptions != null) for (GenericType exception : exceptions) if (exception instanceof GenericType.TypeVariable
                        && name.equals(((GenericType.TypeVariable) exception).name)) thrown = true;
                    GenericType[] declared = bounds.get(name);
                    boolean permitted = true;
                    if (declared != null) for (GenericType bound : declared) {
                        IType type = UnitCompiler.this.substituteGenericType(bound, this.values);
                        if (type == null) { permitted = false; continue; }
                        String descriptor = UnitCompiler.rawTypeOf(type).getDescriptor();
                        if (!descriptor.equals("Ljava/lang/Object;") && !descriptor.equals("Ljava/lang/Throwable;")
                            && !descriptor.equals("Ljava/lang/Exception;")) permitted = false;
                    }
                    if (thrown && permitted) result = UnitCompiler.this.iClassLoader.TYPE_java_lang_RuntimeException;
                }
                if (upper != null) for (IType limit : upper) {
                    if (UnitCompiler.this.genericConvertible(result, limit, true)) continue;
                    boolean fits = !this.equals.containsKey(name);
                    if (lower != null) for (IType type : lower) if (!UnitCompiler.this.genericConvertible(type, limit, true)) fits = false;
                    if (fits) result = limit; else this.valid = false;
                }
                if (lower != null) for (IType type : lower) {
                    if (!UnitCompiler.this.genericConvertible(type, result, true)) this.valid = false;
                }
                this.values.put(name, result);
            }
            for (Map.Entry<String, GenericType[]> entry : bounds.entrySet()) {
                IType value = this.values.get(entry.getKey());
                if (value == null) continue;
                for (GenericType bound : entry.getValue()) {
                    IType limit = UnitCompiler.this.substituteGenericType(bound, this.values);
                    if (limit != null && !UnitCompiler.this.genericConvertible(value, limit, true)) this.valid = false;
                }
            }
            return this.values;
        }

        private int constraintCount() {
            int result = this.equals.size();
            for (List<IType> types : this.lowers.values()) result += types.size();
            for (List<IType> types : this.uppers.values()) result += types.size();
            return result;
        }

        /** An upper bound need not itself satisfy the declaration: Number & Comparable<T> needs a fresh type. */
        private boolean canInstantiateUpper(String name, IType type) throws CompileException {
            return this.canInstantiateUpper(name, type, new HashSet<String>());
        }

        private boolean canInstantiateUpper(String name, IType type, Set<String> visited) throws CompileException {
            IClass raw = UnitCompiler.rawTypeOf(type);
            if (raw.isFinal()) return true; // A conflicting final target must fail the subsequent bound checks.
            if (!visited.add(name)) return true;
            GenericType[] declared = this.bounds.get(name);
            if (declared != null) for (GenericType bound : declared) {
                if (bound instanceof GenericType.TypeVariable) {
                    String dependency = ((GenericType.TypeVariable) bound).name;
                    if (this.variables.contains(dependency) && !this.canInstantiateUpper(dependency, type, visited)) return false;
                }
                IType limit = UnitCompiler.this.substituteGenericType(bound, this.values);
                IClass erased = bound instanceof GenericType.Parameterized ? ((GenericType.Parameterized) bound).rawType
                    : bound instanceof GenericType.Raw ? ((GenericType.Raw) bound).iClass : null;
                if (limit != null ? !UnitCompiler.this.genericConvertible(type, limit, false)
                    : erased != null && !erased.isAssignableFrom(raw)) return false;
            }
            return true;
        }

        /** Resolve dependencies first; a cycle needs distinct fresh variables with mutually substituted bounds. */
        private void resolve(String name, List<String> active, Map<String, CapturedType> recursive) throws CompileException {
            if (this.values.containsKey(name)) return;
            int cycle = active.indexOf(name);
            if (cycle != -1) {
                for (int i = cycle; i < active.size(); i++) {
                    String variable = active.get(i);
                    CapturedType capture = new CapturedType(UnitCompiler.this.iClassLoader.TYPE_java_lang_Object);
                    recursive.put(variable, capture); this.values.put(variable, capture);
                }
                return;
            }
            active.add(name);
            GenericType[] declared = this.bounds.get(name);
            Set<String> dependencies = new LinkedHashSet<>();
            if (declared != null) for (GenericType bound : declared) UnitCompiler.this.collectGenericVariables(bound, dependencies);
            for (String dependency : dependencies) if (this.variables.contains(dependency)) this.resolve(dependency, active, recursive);
            active.remove(active.size() - 1);
            if (this.values.containsKey(name)) return;
            IType result = declared == null || declared.length == 0 ? null
                : UnitCompiler.this.substituteGenericType(declared[0], this.values);
            if (result == null) result = UnitCompiler.this.iClassLoader.TYPE_java_lang_Object;
            List<IType> upper = this.uppers.get(name);
            if (upper != null) for (IType limit : upper) if (!UnitCompiler.this.genericConvertible(result, limit, false)) {
                CapturedType capture = new CapturedType(UnitCompiler.this.iClassLoader.TYPE_java_lang_Object);
                recursive.put(name, capture); result = capture; break;
            }
            this.values.put(name, result);
        }
    }

    private Map<String, GenericType[]> methodBounds(IInvocable method) throws CompileException {
        if (method instanceof IMethod && method.getDeclaringIClass() instanceof ClassFileIClass) {
            return ((ClassFileIClass) method.getDeclaringIClass()).getMethodTypeParameterBounds((IMethod) method);
        }
        return method.getTypeParameterBounds();
    }

    private Map<String, IType> receiverEnvironment(@Nullable IType receiver, IClass owner) throws CompileException {
        Map<String, IType> result = new HashMap<>();
        AbstractTypeDeclaration source = this.sourceDeclarations.get(owner);
        if (source != null) for (Scope scope = source.getEnclosingScope(); !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            if (!(scope instanceof FunctionDeclarator)) continue;
            TypeParameter[] parameters = ((FunctionDeclarator) scope).getOptionalTypeParameters();
            if (parameters != null) for (TypeParameter parameter : parameters) if (!result.containsKey(parameter.name)) {
                result.put(parameter.name, this.sourceVariable(parameter, scope));
            }
        }
        IType view = receiver == null ? null : this.asSupertype(receiver, owner);
        this.bindTypeVariables(view, result);
        return result;
    }

    /** Assignment of parameterized types is invariant, except for explicit wildcard containment. */
    private boolean genericConvertible(IType actual, IType target, boolean boxing) throws CompileException {
        if (actual.equals(target)) return true;
        if (target instanceof CapturedType && ((CapturedType) target).intersection) {
            for (IType bound : ((CapturedType) target).bounds) if (!this.genericConvertible(actual, bound, boxing)) return false;
            return true;
        }
        if (actual instanceof GenericArrayType && target instanceof IClass && ((IClass) target).isArray()) {
            return this.genericConvertible(((GenericArrayType) actual).component, ((IClass) target).getComponentType(), false);
        }
        if (target instanceof GenericArrayType) {
            return actual == IClass.NULL || UnitCompiler.isArray(actual)
                && this.genericConvertible(UnitCompiler.getComponentType(actual), ((GenericArrayType) target).component, boxing);
        }
        if (actual instanceof CapturedType) {
            for (IType bound : ((CapturedType) actual).bounds) if (this.genericConvertible(bound, target, boxing)) return true;
            return false;
        }
        if (target instanceof CapturedType) {
            IType lower = ((CapturedType) target).lower;
            return actual == IClass.NULL || lower != null && this.genericConvertible(actual, lower, boxing);
        }
        if (actual instanceof IWildcardType) return this.genericConvertible(((IWildcardType) actual).getUpperBound(), target, boxing);
        if (target instanceof SourceTypeVariable) {
            if (actual == IClass.NULL) return true;
            if (!(actual instanceof SourceTypeVariable)) return false;
            TypeParameter declaration = ((SourceTypeVariable) actual).declaration;
            if (declaration.bound != null) for (ReferenceType bound : declaration.bound) {
                if (this.genericConvertible(this.getType(bound), target, boxing)) return true;
            }
            return false;
        }
        if (actual instanceof SourceTypeVariable) {
            if (!(target instanceof ParameterizedIClass) && this.isMethodInvocationConvertible(UnitCompiler.rawTypeOf(actual), UnitCompiler.rawTypeOf(target), boxing)) return true;
            TypeParameter declaration = ((SourceTypeVariable) actual).declaration;
            if (declaration.bound != null) for (ReferenceType bound : declaration.bound) {
                IType type = this.sourceBoundType(bound);
                if (type != null && this.genericConvertible(type, target, boxing)) return true;
            }
            return false;
        }
        if (target instanceof IWildcardType) {
            IType lower = ((IWildcardType) target).getLowerBound();
            return actual == IClass.NULL || lower != null && this.genericConvertible(actual, lower, boxing);
        }
        if (!this.isMethodInvocationConvertible(UnitCompiler.rawTypeOf(actual), UnitCompiler.rawTypeOf(target), boxing)) return false;
        if (!(target instanceof ParameterizedIClass) || actual == IClass.NULL) return true;
        ParameterizedIClass parameter = (ParameterizedIClass) target;
        IType view = this.asSupertype(actual, parameter.rawType);
        if (!(view instanceof ParameterizedIClass)) return true;
        ParameterizedIClass actualView = (ParameterizedIClass) view;
        for (IType argument : actualView.actualTypeArguments) if (argument instanceof IWildcardType) {
            actualView = (ParameterizedIClass) this.captureParameterized(actualView);
            break;
        }
        if (parameter.ownerType != null && actualView.ownerType != null
            && !this.genericConvertible(actualView.ownerType, parameter.ownerType, false)) return false;
        IType[] arguments = actualView.actualTypeArguments;
        if (arguments.length == 0 && actualView.rawType.getITypeVariables().length > 0) return true;
        if (arguments.length != parameter.actualTypeArguments.length) return false;
        for (int i = 0; i < arguments.length; i++) {
            IType a = arguments[i], b = parameter.actualTypeArguments[i];
            if (b instanceof IWildcardType) {
                IWildcardType wildcard = (IWildcardType) b;
                IType lower = wildcard.getLowerBound();
                if (lower != null) {
                    if (a instanceof IWildcardType) a = ((IWildcardType) a).getLowerBound();
                    if (a == null || !this.genericConvertible(lower, a, boxing)
                        || !this.genericConvertible(a, wildcard.getUpperBound(), boxing)) return false;
                } else {
                    if (a instanceof IWildcardType) a = ((IWildcardType) a).getUpperBound();
                    if (!this.genericConvertible(a, wildcard.getUpperBound(), boxing)) return false;
                }
            } else if (!this.sameTypeArgument(a, b)) return false;
        }
        return true;
    }

    /** Invariant nested arguments can still have equivalent wildcard bounds from their declaration. */
    private boolean sameTypeArgument(IType a, IType b) throws CompileException {
        if (a.equals(b)) return true;
        if (!(a instanceof ParameterizedIClass) || !(b instanceof ParameterizedIClass)) return false;
        ParameterizedIClass pa = (ParameterizedIClass) a, pb = (ParameterizedIClass) b;
        if (pa.rawType != pb.rawType || pa.actualTypeArguments.length != pb.actualTypeArguments.length) return false;
        if (pa.ownerType == null ? pb.ownerType != null : pb.ownerType == null || !this.sameTypeArgument(pa.ownerType, pb.ownerType)) return false;
        ParameterizedIClass ca = null, cb = null;
        for (int i = 0; i < pa.actualTypeArguments.length; i++) {
            IType x = pa.actualTypeArguments[i], y = pb.actualTypeArguments[i];
            if (x instanceof IWildcardType && y instanceof IWildcardType) {
                IType lx = ((IWildcardType) x).getLowerBound(), ly = ((IWildcardType) y).getLowerBound();
                if (lx == null ? ly != null : ly == null || !this.sameTypeArgument(lx, ly)) return false;
                if (ca == null) { ca = (ParameterizedIClass) this.captureParameterized(pa); cb = (ParameterizedIClass) this.captureParameterized(pb); }
                IType[] ax = ((CapturedType) ca.actualTypeArguments[i]).bounds, by = ((CapturedType) cb.actualTypeArguments[i]).bounds;
                for (int direction = 0; direction < 2; direction++) {
                    for (IType required : direction == 0 ? ax : by) {
                        boolean covered = false;
                        for (IType available : direction == 0 ? by : ax) if (this.genericConvertible(available, required, false)) covered = true;
                        if (!covered) return false;
                    }
                }
            } else if (!this.sameTypeArgument(x, y)) return false;
        }
        return true;
    }

    private Map<String, IType> functionalEnvironment(IType target, IMethod sam) throws CompileException {
        Map<String, IType> environment = this.receiverEnvironment(target, sam.getDeclaringIClass());
        if (target instanceof ParameterizedIClass) {
            Map<String, IType> captured = this.receiverEnvironment(this.captureParameterized((ParameterizedIClass) target), sam.getDeclaringIClass());
            for (Map.Entry<String, IType> entry : environment.entrySet()) if (entry.getValue() instanceof IWildcardType) {
                IType value = captured.get(entry.getKey());
                if (value instanceof CapturedType) {
                    CapturedType variable = (CapturedType) value;
                    // Keep wildcard containment in nested results such as Collection<E>.
                    // Fixing E to its capture would reject a legal Collection<EndpointSubtype> lambda result.
                    entry.setValue(this.wildcard(variable.upper, variable.lower));
                }
            }
        }
        return environment;
    }

    private IType[] functionalParameters(IType target, IMethod sam) throws CompileException {
        Map<String, IType> environment = this.functionalEnvironment(target, sam);
        GenericType[] generic = this.getGenericParameterTypes(sam);
        IType[] result = new IType[sam.getParameterTypes().length];
        System.arraycopy(sam.getParameterTypes(), 0, result, 0, result.length);
        if (generic != null) for (int i = 0; i < result.length; i++) {
            IType type = this.substituteGenericType(generic[i], environment);
            if (type != null) result[i] = type instanceof CapturedType
                ? (((CapturedType) type).lower == null ? ((CapturedType) type).upper : ((CapturedType) type).lower)
                : type instanceof IWildcardType
                    ? (((IWildcardType) type).getLowerBound() == null ? ((IWildcardType) type).getUpperBound() : ((IWildcardType) type).getLowerBound()) : type;
        }
        return result;
    }

    private IType functionalReturn(IType target, IMethod sam) throws CompileException {
        GenericType generic = this.getGenericReturnType(sam);
        IType result = generic == null ? null : this.substituteGenericType(generic, this.functionalEnvironment(target, sam));
        if (result instanceof IWildcardType) result = ((IWildcardType) result).getUpperBound();
        return result == null ? sam.getReturnType() : result;
    }

    private static final class CapturedType implements IParameterizedType {
        IType upper;
        IType[] bounds;
        boolean intersection;
        @Nullable final IType lower;
        CapturedType(IType upper) { this.upper = upper; this.lower = null; this.bounds = new IType[] { upper }; }
        CapturedType(IWildcardType wildcard) {
            this.upper = wildcard.getUpperBound(); this.lower = wildcard.getLowerBound(); this.bounds = new IType[] { this.upper };
        }
        @Override public IType getRawType() { return UnitCompiler.rawTypeOf(this.upper); }
        @Override public IType[] getActualTypeArguments() { return new IType[0]; }
        @Override public String toString() { return "capture of ? extends " + UnitCompiler.rawTypeOf(this.upper); }
    }
    private final Map<Object, IType> invocationCaptures = new java.util.IdentityHashMap<>();
    private final Map<Object, IType> captureInputs = new java.util.IdentityHashMap<>();
    private final Map<Object, Object> captureOrigins = new java.util.IdentityHashMap<>();

    private IType captureInvocationReceiver(Object expression, IType receiver) throws CompileException {
        if (!(receiver instanceof ParameterizedIClass)) return receiver;
        Object origin = this.captureOrigins.get(expression);
        if (origin != null) expression = origin;
        IType input = receiver;
        IType previous = this.invocationCaptures.get(expression);
        if (previous != null && receiver.equals(this.captureInputs.get(expression))) return previous;
        IType oldInput = this.captureInputs.get(expression);
        if (previous instanceof ParameterizedIClass && oldInput instanceof ParameterizedIClass
            && ((ParameterizedIClass) previous).rawType == ((ParameterizedIClass) receiver).rawType) {
            ParameterizedIClass current = (ParameterizedIClass) receiver, old = (ParameterizedIClass) oldInput;
            IType[] captured = ((ParameterizedIClass) previous).actualTypeArguments;
            IType[] arguments = current.actualTypeArguments.clone();
            for (int i = 0; i < arguments.length && i < captured.length && i < old.actualTypeArguments.length; i++) {
                if (arguments[i] instanceof IWildcardType && captured[i] instanceof CapturedType
                    && arguments[i].equals(old.actualTypeArguments[i])) {
                    // Re-attributing another type argument does not create a new existential for this expression.
                    CapturedType capture = (CapturedType) captured[i];
                    capture.upper = ((IWildcardType) arguments[i]).getUpperBound();
                    arguments[i] = capture;
                }
            }
            receiver = new ParameterizedIClass(current.rawType, arguments, current.ownerType);
        }
        IType result = this.captureParameterized((ParameterizedIClass) receiver);
        this.captureInputs.put(expression, input);
        this.invocationCaptures.put(expression, result);
        return result;
    }

    private IType captureParameterized(ParameterizedIClass parameter) throws CompileException {
        IType owner = parameter.ownerType instanceof ParameterizedIClass
            ? this.captureParameterized((ParameterizedIClass) parameter.ownerType) : parameter.ownerType;
        IType[] arguments = new IType[parameter.actualTypeArguments.length];
        System.arraycopy(parameter.actualTypeArguments, 0, arguments, 0, arguments.length);
        boolean changed = owner != parameter.ownerType;
        for (int i = 0; i < arguments.length; i++) if (arguments[i] instanceof IWildcardType) {
            arguments[i] = new CapturedType((IWildcardType) arguments[i]); changed = true;
        }
        if (!changed) return parameter;
        ParameterizedIClass result = new ParameterizedIClass(parameter.rawType, arguments, owner);
        Map<String, IType> environment = new HashMap<>();
        this.bindTypeVariables(result, environment);
        ITypeVariable[] variables = parameter.rawType.getITypeVariables();
        Map<String, GenericType[]> declaredBounds = parameter.rawType.getTypeParameterBounds();
        for (int i = 0; i < arguments.length && i < variables.length; i++) if (arguments[i] instanceof CapturedType) {
            CapturedType capture = (CapturedType) arguments[i];
            List<IType> bounds = new ArrayList<>(); bounds.add(capture.upper);
            GenericType[] declared = declaredBounds.get(variables[i].getName());
            if (declared != null) for (GenericType bound : declared) {
                IType type = this.substituteGenericType(bound, environment);
                if (type == null) continue;
                bounds.add(type);
                IClass upper = UnitCompiler.rawTypeOf(capture.upper), limit = UnitCompiler.rawTypeOf(type);
                if (this.genericConvertible(type, capture.upper, false) || upper.isInterface() && !limit.isInterface() && limit != this.iClassLoader.TYPE_java_lang_Object) capture.upper = type;
            }
            capture.bounds = bounds.toArray(new IType[bounds.size()]);
        }
        for (IType argument : arguments) if (argument instanceof CapturedType) {
            CapturedType capture = (CapturedType) argument;
            for (IType bound : capture.bounds) {
                IClass upper = UnitCompiler.rawTypeOf(capture.upper), limit = UnitCompiler.rawTypeOf(bound);
                if (this.genericConvertible(bound, capture.upper, false) || upper.isInterface() && !limit.isInterface() && limit != this.iClassLoader.TYPE_java_lang_Object) capture.upper = bound;
            }
        }
        return result;
    }

    @Nullable private IType invocationReceiver(Invocation invocation) throws CompileException {
        if (invocation instanceof MethodInvocation) {
            Atom target = ((MethodInvocation) invocation).target;
            if (target != null) return this.captureInvocationReceiver(target, this.getType(target));
            Scope scope = invocation.getEnclosingScope();
            while (!(scope instanceof TypeDeclaration)) scope = scope.getEnclosingScope();
            return this.lexicalReceiver(this.resolve((TypeDeclaration) scope), invocation.getEnclosingScope());
        }
        SuperclassMethodInvocation scmi = (SuperclassMethodInvocation) invocation;
        return scmi.receiverType == null || scmi.invocationType == null ? null
            : this.asSupertype(this.lexicalReceiver(scmi.receiverType, scmi.getEnclosingScope()), scmi.invocationType);
    }

    @Nullable private IType invocationReceiver(Invocation invocation, IClass declaringType) throws CompileException {
        if (invocation instanceof MethodInvocation && ((MethodInvocation) invocation).target == null) {
            for (Scope scope = invocation.getEnclosingScope();; scope = scope.getEnclosingScope()) {
                if (scope instanceof TypeDeclaration) {
                    IClass lexical = this.resolve((TypeDeclaration) scope);
                    if (declaringType.isAssignableFrom(lexical)) return this.lexicalReceiver(lexical, invocation.getEnclosingScope());
                }
                if (scope instanceof CompilationUnit) break;
            }
        }
        return this.invocationReceiver(invocation);
    }

    /** Attribute a nested generic invocation against this candidate without retaining its target. */
    private IType invocationArgumentType(Rvalue argument, @Nullable IType target) throws CompileException {
        while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
        if (argument instanceof ConditionalExpression && target != null) {
            ConditionalExpression conditional = (ConditionalExpression) argument;
            IType previous = this.conditionalTargets.put(conditional, target);
            this.speculativeInvocationDepth++;
            try { return this.getType(conditional); }
            finally {
                this.speculativeInvocationDepth--;
                if (previous == null) this.conditionalTargets.remove(conditional); else this.conditionalTargets.put(conditional, previous);
            }
        }
        if (argument instanceof NewClassInstance && this.isDiamondCreation((NewClassInstance) argument) && target != null) {
            NewClassInstance creation = (NewClassInstance) argument;
            if (this.inferredReferenceCreations.contains(creation)) return this.getType(creation);
            IType previous = this.creationTargets.put(creation, target), cached = creation.iType;
            creation.iType = null;
            try { return this.getType2(creation); }
            finally {
                if (previous == null) this.creationTargets.remove(creation); else this.creationTargets.put(creation, previous);
                creation.iType = cached;
            }
        }
        if (!(argument instanceof Invocation) || target == null) return this.captureInvocationReceiver(argument, this.getType(argument));
        Invocation nested = (Invocation) argument;
        IType previous = this.invocationTargets.put(nested, target);
        IMethod oldMethod = nested instanceof MethodInvocation ? ((MethodInvocation) nested).iMethod : null;
        boolean success = false;
        this.speculativeInvocationDepth++;
        try {
            IType result = this.captureInvocationReceiver(argument, this.getType(argument));
            success = true;
            return result;
        }
        finally {
            this.speculativeInvocationDepth--;
            if (!success && nested instanceof MethodInvocation) ((MethodInvocation) nested).iMethod = oldMethod;
            if (previous == null) this.invocationTargets.remove(nested);
            else this.invocationTargets.put(nested, previous);
        }
    }

    /** Nested poly invocations must wait for proper constraints from their siblings. */
    private boolean needsInvocationTarget(Rvalue argument) throws CompileException {
        while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
        if (argument instanceof ConditionalExpression) return this.needsInvocationTarget(((ConditionalExpression) argument).mhs)
            || this.needsInvocationTarget(((ConditionalExpression) argument).rhs);
        if (argument instanceof NewClassInstance) return this.isDiamondCreation((NewClassInstance) argument);
        if (!(argument instanceof Invocation) || ((Invocation) argument).typeArguments != null) return false;
        IMethod method;
        IMethod previous = argument instanceof MethodInvocation ? ((MethodInvocation) argument).iMethod : null;
        this.speculativeInvocationDepth++;
        try {
            method = argument instanceof MethodInvocation ? this.findIMethod((MethodInvocation) argument)
                : this.findIMethod((SuperclassMethodInvocation) argument);
        } catch (CompileException ex) {
            if (argument instanceof MethodInvocation) ((MethodInvocation) argument).iMethod = previous;
            return true;
        } finally { this.speculativeInvocationDepth--; }
        Set<String> variables = this.getMethodTypeParameterNames(method);
        if (variables == null || variables.isEmpty()) return false;
        Set<String> resultVariables = new HashSet<>();
        GenericType result = this.getGenericReturnType(method);
        if (result != null) this.collectGenericVariables(result, resultVariables);
        resultVariables.retainAll(variables);
        return !resultVariables.isEmpty();
    }

    /** An invocation's constraints are shared by return inference and functional argument target typing. */
    @Nullable private GenericType[] invocationPatterns(Invocation invocation, IMethod method) throws CompileException {
        GenericType[] declared = this.getGenericParameterTypes(method);
        if (declared == null) return null;
        if (!method.isVarargs()) return declared.length == invocation.arguments.length ? declared : null;
        int fixed = declared.length - 1;
        if (invocation.arguments.length < fixed) return null;
        GenericType[] result = new GenericType[invocation.arguments.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = declared[Math.min(i, fixed)];
            if (i >= fixed) {
                Rvalue argument = invocation.arguments[i];
                boolean array = result.length == declared.length && !this.isPolyExpression(argument)
                    && this.genericConvertible(this.getType(argument), method.getParameterTypes()[fixed], false);
                if (!array) {
                    if (result[i] instanceof GenericType.ArrayOf) result[i] = ((GenericType.ArrayOf) result[i]).componentType;
                    else if (result[i] instanceof GenericType.Raw && ((GenericType.Raw) result[i]).iClass.isArray()) {
                        result[i] = new GenericType.Raw(((GenericType.Raw) result[i]).iClass.getComponentType());
                    }
                }
            }
        }
        return result;
    }

    private void explicitTypeArguments(InferenceContext context, @Nullable TypeArgument[] arguments, Scope scope) throws CompileException {
        if (arguments == null) return;
        List<IType> types = new ArrayList<>();
        for (TypeArgument argument : arguments) {
            if (!(argument instanceof Type)) { context.valid = false; continue; }
            UnitCompiler.setScopeIfUnset((Type) argument, scope);
            IType type = this.getType((Type) argument);
            if (UnitCompiler.isPrimitive(type)) context.valid = false;
            types.add(type);
        }
        if (context.explicitVariables.isEmpty()) return;
        if (types.size() != context.explicitVariables.size()) { context.valid = false; return; }
        int index = 0;
        for (String variable : context.explicitVariables) context.equals.put(variable, types.get(index++));
    }

    private IType[] constructorTargetTypes(Locatable call, IConstructor constructor, Rvalue[] arguments, Scope scope) throws CompileException {
        return this.constructorInference(call, constructor, arguments, scope, false).parameterTargets;
    }
    private InferenceContext constructorInference(Locatable call, IConstructor constructor, Rvalue[] arguments, Scope scope) throws CompileException {
        return this.constructorInference(call, constructor, arguments, scope, true);
    }
    private InferenceContext constructorInference(Locatable call, IConstructor constructor, Rvalue[] arguments, Scope scope, boolean inferThrows) throws CompileException {
        IType receiver = constructor.getDeclaringIClass();
        TypeArgument[] explicit = null;
        if (call instanceof NewClassInstance) {
            NewClassInstance creation = (NewClassInstance) call;
            receiver = creation.type == null ? creation.iType : this.getType(creation.type);
            explicit = creation.typeArguments;
        } else if (call instanceof NewAnonymousClassInstance) {
            NewAnonymousClassInstance creation = (NewAnonymousClassInstance) call;
            IType base = this.getType(creation.anonymousClassDeclaration.baseType);
            if (UnitCompiler.rawTypeOf(base) == constructor.getDeclaringIClass()) {
                receiver = base;
                explicit = creation.typeArguments;
            }
        } else if (call instanceof ConstructorInvocation) {
            explicit = ((ConstructorInvocation) call).typeArguments;
            for (Scope enclosing = scope; !(enclosing instanceof CompilationUnit); enclosing = enclosing.getEnclosingScope()) {
                if (enclosing instanceof TypeDeclaration) {
                    // Anonymous constructors forward erased synthetic parameters; the source creation
                    // has already been checked against the parameterized base type.
                    if (enclosing instanceof AnonymousClassDeclaration) break;
                    IType self = this.lexicalReceiver(this.resolve((TypeDeclaration) enclosing), scope);
                    IType view = this.asSupertype(self, constructor.getDeclaringIClass());
                    if (view != null) receiver = view;
                    break;
                }
            }
        }
        boolean diamond = call instanceof NewClassInstance && this.isDiamondCreation((NewClassInstance) call);
        InferenceContext context = new InferenceContext(constructor, receiver, diamond);
        if (diamond) context.constrain(context.creationPattern, this.creationTargets.get((NewClassInstance) call), -1);
        this.explicitTypeArguments(context, explicit, scope);
        GenericType[] declared = constructor.getGenericParameterTypes();
        IClass[] erased = constructor.getParameterTypes();
        IType[] targets = new IType[erased.length];
        System.arraycopy(erased, 0, targets, 0, erased.length);
        context.parameterTargets = targets;
        // A constructor of a raw generic class is a raw member, including parameterized
        // types that do not mention the class variable (JLS 4.8).
        boolean rawConstructor = !diamond && !(receiver instanceof ParameterizedIClass)
            && constructor.getDeclaringIClass().getITypeVariables().length != 0;
        if (rawConstructor || declared == null || declared.length != erased.length) {
            declared = new GenericType[erased.length];
            for (int i = 0; i < declared.length; i++) declared[i] = new GenericType.Raw(erased[i]);
        }
        context.values.putAll(context.equals);
        GenericType[] argumentPatterns = new GenericType[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (declared.length == 0 || i >= declared.length && !constructor.isVarargs()) break;
            GenericType pattern = declared[Math.min(i, declared.length - 1)];
            if (constructor.isVarargs() && i >= declared.length - 1
                && (arguments.length != declared.length || !this.isPolyExpression(arguments[i]) && !UnitCompiler.isArray(this.getType(arguments[i])))) {
                if (pattern instanceof GenericType.ArrayOf) pattern = ((GenericType.ArrayOf) pattern).componentType;
                else if (pattern instanceof GenericType.Raw && ((GenericType.Raw) pattern).iClass.isArray()) {
                    pattern = new GenericType.Raw(((GenericType.Raw) pattern).iClass.getComponentType());
                }
            }
            argumentPatterns[i] = pattern;
        }
        boolean[] deferred = new boolean[arguments.length];
        for (int i = 0; i < arguments.length; i++) deferred[i] = this.needsInvocationTarget(arguments[i]);
        for (int phase = 0; phase < 2; phase++) for (int i = 0; i < arguments.length; i++) {
            if (deferred[i] != (phase == 1) || argumentPatterns[i] == null || this.isPolyExpression(arguments[i])) continue;
            this.seedLambdaInference(context);
            context.constrain(argumentPatterns[i], this.invocationArgumentType(arguments[i],
                this.inferenceTarget(argumentPatterns[i], context)), 0);
        }
        this.inferReferenceArguments(arguments, argumentPatterns, context);
        this.inferLambdaArguments(arguments, argumentPatterns, context, inferThrows);
        context.solve(constructor);
        for (int i = 0; i < targets.length; i++) {
            IType type = this.substituteGenericType(declared[i], context.values);
            if (type != null) targets[i] = type;
        }
        if (!context.valid) throw UnitCompiler.compileException(call, "Incompatible generic constructor arguments");
        for (int i = 0; i < arguments.length; i++) {
            if (targets.length == 0 || i >= targets.length && !constructor.isVarargs() || this.isPolyExpression(arguments[i])) continue;
            IType target = targets[Math.min(i, targets.length - 1)];
            if (constructor.isVarargs() && i >= targets.length - 1
                && (arguments.length != targets.length || !UnitCompiler.isArray(this.getType(arguments[i])))) target = UnitCompiler.getComponentType(target);
            if (!this.genericConvertible(this.invocationArgumentType(arguments[i], target), target, true)) throw UnitCompiler.compileException(call, "Incompatible constructor argument");
        }
        return context;
    }

    private InferenceContext invocationInference(Invocation invocation, IMethod method) throws CompileException {
        return this.invocationInference(invocation, method, true);
    }

    private InferenceContext invocationInference(Invocation invocation, IMethod method, boolean inferThrows) throws CompileException {
        IType receiver = this.invocationReceiver(invocation, method.getDeclaringIClass());
        InferenceContext context = new InferenceContext(method, receiver);
        this.explicitTypeArguments(context, invocation.typeArguments, invocation.getEnclosingScope());
        GenericType[] parameters = this.invocationPatterns(invocation, method);
        if (parameters == null) return context;
        context.constrain(this.getGenericReturnType(method), this.invocationTargets.get(invocation), -1);
        // Propagate proper target constraints to nested calls before solving argument-dependent bounds.
        // Solving now would reject still-unbound F-bounds such as Enum<E> and Comparable<T>.
        context.values.putAll(context.equals);
        for (Map.Entry<String, List<IType>> constraint : context.uppers.entrySet()) {
            if (!context.values.containsKey(constraint.getKey()) && !constraint.getValue().isEmpty()) {
                context.values.put(constraint.getKey(), constraint.getValue().get(0));
            }
        }
        this.inferReferenceArguments(invocation.arguments, parameters, context, true);
        // Seed proper sibling types before attributing nested generic calls or diamonds.
        // Otherwise arg() in declare(arg(), Parser<String>) is prematurely fixed to Object.
        boolean[] deferred = new boolean[parameters.length];
        for (int i = 0; i < parameters.length; i++) deferred[i] = this.needsInvocationTarget(invocation.arguments[i]);
        for (int phase = 0; phase < 2; phase++) for (int i = 0; i < parameters.length; i++) {
            if (deferred[i] != (phase == 1)) continue;
            this.seedLambdaInference(context);
            Rvalue argument = invocation.arguments[i];
            while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
            if (this.isPolyExpression(argument)) continue;
            IType actual = this.invocationArgumentType(argument, this.inferenceTarget(parameters[i], context));
            if (argument instanceof ClassLiteral && parameters[i] instanceof GenericType.Parameterized
                && "Ljava/lang/Class;".equals(((GenericType.Parameterized) parameters[i]).rawType.getDescriptor())) {
                IType type = this.getType(((ClassLiteral) argument).type);
                type = type == IClass.VOID ? this.iClassLoader.TYPE_java_lang_Void : this.wrapperOf(UnitCompiler.rawTypeOf(type));
                actual = new ParameterizedIClass(this.iClassLoader.TYPE_java_lang_Class, new IType[] { type });
            }
            context.constrain(parameters[i], actual, 0);
        }
        context.constrain(this.getGenericReturnType(method), this.invocationTargets.get(invocation), -1);
        // Exact references contribute constraints before their target types are fully instantiated.
        this.seedLambdaInference(context);
        this.inferReferenceArguments(invocation.arguments, parameters, context);
        this.inferLambdaArguments(invocation.arguments, parameters, context, inferThrows);
        context.solve(method);
        return context;
    }

    private static final class ReferenceBinding {
        final IMethod method;
        final IType[] parameters;
        final IType result;
        final boolean variableArity;
        final int receiverArguments;
        final IClass[] exceptions;
        ReferenceBinding(IMethod method, IType[] parameters, IType result, boolean variableArity, int receiverArguments, IClass[] exceptions) {
            this.method = method; this.parameters = parameters; this.result = result;
            this.variableArity = variableArity; this.receiverArguments = receiverArguments; this.exceptions = exceptions;
        }
    }

    @Nullable private ReferenceBinding selectReferenceMethod(
        MethodReference reference, IType receiver, IType[] inputs, IType output, boolean statik, int receiverArguments
    ) throws CompileException {
        List<IMethod> methods = new ArrayList<>();
        receiver = this.captureInvocationReceiver(reference, receiver);
        this.getTypeMethods(receiver, reference.methodName, methods);
        if (UnitCompiler.rawTypeOf(receiver).isInterface() && !statik) {
            methods.addAll(Arrays.asList(this.iClassLoader.TYPE_java_lang_Object.getDeclaredIMethods(reference.methodName)));
        }
        for (int phase = 0; phase < 3; phase++) {
            List<ReferenceBinding> applicable = new ArrayList<>();
            for (IMethod method : methods) {
                if (method.isStatic() != statik || !this.isAccessible(method, reference.getEnclosingScope(), receiver)) continue;
                IClass[] erased = method.getParameterTypes();
                if (phase == 2 ? !method.isVarargs() || inputs.length < erased.length - 1 : inputs.length != erased.length) continue;
                GenericType[] generic = this.getGenericParameterTypes(method);
                InferenceContext context = new InferenceContext(method, receiver);
                this.explicitTypeArguments(context, reference.typeArguments, reference.getEnclosingScope());
                IType[] parameters = new IType[inputs.length];
                for (int i = 0; i < inputs.length; i++) {
                    GenericType formal = generic == null ? null : generic[Math.min(i, generic.length - 1)];
                    if (phase == 2 && i >= erased.length - 1 && formal instanceof GenericType.ArrayOf) formal = ((GenericType.ArrayOf) formal).componentType;
                    context.constrain(formal, inputs[i], 0);
                }
                GenericType resultType = this.getGenericReturnType(method);
                // A fixed/input-constrained reference result may unbox and widen to a primitive target.
                // Requiring its type variable to extend the target's wrapper would reject Byte -> int.
                String resultVariable = resultType instanceof GenericType.TypeVariable ? ((GenericType.TypeVariable) resultType).name : null;
                boolean inputResult = resultVariable != null && (context.equals.containsKey(resultVariable) || context.lowers.containsKey(resultVariable));
                if (output != IClass.VOID && !(UnitCompiler.isPrimitive(output) && inputResult)) context.constrain(resultType, output, -1);
                Map<String, IType> environment = context.solve(method);
                boolean matches = context.valid;
                for (int i = 0; i < inputs.length; i++) {
                    int index = Math.min(i, erased.length - 1);
                    GenericType formal = generic == null ? null : generic[index];
                    if (phase == 2 && i >= erased.length - 1 && formal instanceof GenericType.ArrayOf) formal = ((GenericType.ArrayOf) formal).componentType;
                    IType type = formal == null ? null : this.substituteGenericType(formal, environment);
                    if (type == null) type = phase == 2 && i >= erased.length - 1 ? erased[index].getComponentType() : erased[index];
                    parameters[i] = type;
                    if (!this.genericConvertible(inputs[i], type, phase != 0)) matches = false;
                }
                IType result = resultType == null ? null : this.substituteGenericType(resultType, environment);
                if (result == null) result = method.getReturnType();
                if (UnitCompiler.isArray(receiver) && "clone".equals(reference.methodName)) result = receiver;
                if (result instanceof IWildcardType) result = ((IWildcardType) result).getUpperBound();
                if (output != IClass.VOID && !this.genericConvertible(result, output, true)) matches = false;
                if (matches) applicable.add(new ReferenceBinding(method, parameters, result, phase == 2, receiverArguments, this.instantiatedExceptions(method, environment)));
            }
            if (applicable.isEmpty()) continue;
            ReferenceBinding best = null;
            for (ReferenceBinding a : applicable) {
                boolean maximal = true;
                for (ReferenceBinding b : applicable) if (this.referenceMoreSpecific(b, a) && !this.referenceMoreSpecific(a, b)) maximal = false;
                if (!maximal) continue;
                if (best != null && !this.referenceMoreSpecific(a, best)) throw UnitCompiler.compileException(reference, "Ambiguous method reference");
                if (best == null || best.method.getDeclaringIClass().isAssignableFrom(a.method.getDeclaringIClass())) best = a;
            }
            return best;
        }
        return null;
    }

    private boolean referenceMoreSpecific(ReferenceBinding a, ReferenceBinding b) throws CompileException {
        if (a.parameters.length != b.parameters.length) return false;
        if (a.variableArity && b.variableArity && a.parameters.length == 0) return this.isMoreSpecific(a.method, b.method, 0, true);
        boolean equal = true;
        for (int i = 0; i < a.parameters.length; i++) if (!this.genericConvertible(a.parameters[i], b.parameters[i], false)) return false;
        for (int i = 0; i < a.parameters.length; i++) if (!a.parameters[i].equals(b.parameters[i])) equal = false;
        if (equal && this.isMoreSpecific(b.method, a.method, a.parameters.length, a.variableArity)
            && !this.isMoreSpecific(a.method, b.method, a.parameters.length, a.variableArity)) return false;
        return true;
    }

    private IClass[] instantiatedExceptions(IInvocable method, Map<String, IType> environment) throws CompileException {
        GenericType[] types = method.getGenericThrownExceptions();
        IClass[] erased = method.getThrownExceptions();
        if (types == null || types.length != erased.length) return erased;
        IClass[] result = erased.clone();
        for (int i = 0; i < result.length; i++) {
            IType type = this.substituteGenericType(types[i], environment);
            if (type != null) result[i] = UnitCompiler.rawTypeOf(type);
        }
        return result;
    }

    /** Pure target compatibility check, also used while comparing outer overload candidates. */
    private ReferenceBinding resolveReference(MethodReference reference, IType target) throws CompileException {
        IMethod sam = this.getSam(reference, target);
        IType[] inputs = this.functionalParameters(target, sam);
        IType output = this.functionalReturn(target, sam);
        if (output instanceof IWildcardType) output = ((IWildcardType) output).getUpperBound();
        if (reference.lhs instanceof Rvalue) UnitCompiler.setRvalueScopeIfUnset((Rvalue) reference.lhs, reference.getEnclosingScope());
        Atom lhs = reference.lhs instanceof AmbiguousName ? this.reclassify((AmbiguousName) reference.lhs) : reference.lhs;
        Type type = lhs.toType();
        ReferenceBinding result;
        if (reference.superReference) {
            SuperclassMethodInvocation superReceiver = this.superReferenceReceiver(reference);
            IType lexical = this.lexicalReceiver(superReceiver.receiverType, reference.getEnclosingScope());
            IType receiver = this.asSupertype(lexical, superReceiver.invocationType);
            result = this.selectReferenceMethod(reference, receiver == null ? superReceiver.invocationType : receiver, inputs, output, false, 0);
            if (result != null && result.method.isAbstract()) throw UnitCompiler.compileException(reference, "Super reference requires a concrete method");
        } else if (type == null) {
            result = this.selectReferenceMethod(reference, this.getType(lhs), inputs, output, false, 0);
        } else {
            UnitCompiler.setScopeIfUnset(type, reference.getEnclosingScope());
            this.validateReferenceQualifier(type, type instanceof ArrayType);
            IType receiver = this.getType(type);
            ReferenceBinding statik = receiver instanceof ParameterizedIClass ? null
                : this.selectReferenceMethod(reference, receiver, inputs, output, true, 0);
            ReferenceBinding instance = null;
            if (inputs.length > 0 && this.genericConvertible(inputs[0], receiver, false)) {
                IType[] tail = new IType[inputs.length - 1];
                System.arraycopy(inputs, 1, tail, 0, tail.length);
                IType lookup = receiver instanceof ParameterizedIClass ? receiver
                    : this.asSupertype(inputs[0], UnitCompiler.rawTypeOf(receiver));
                instance = this.selectReferenceMethod(reference, lookup == null ? receiver : lookup, tail, output, false, 1);
            }
            if (statik != null && instance != null) throw UnitCompiler.compileException(reference, "Ambiguous static/unbound method reference");
            result = statik == null ? instance : statik;
        }
        if (result == null) throw UnitCompiler.compileException(reference, "Cannot resolve method reference " + reference + " against " + target);
        IClass[] permitted = this.instantiatedExceptions(sam, this.receiverEnvironment(target, sam.getDeclaringIClass()));
        for (IClass exception : result.exceptions) {
            if (this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(exception) || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(exception)) continue;
            boolean declared = false;
            for (IClass allowed : permitted) if (allowed.isAssignableFrom(exception)) declared = true;
            if (!declared) throw UnitCompiler.compileException(reference, "Method reference throws an incompatible checked exception " + exception);
        }
        return result;
    }

    /** Validate creation references against an overload candidate without lowering their AST. */
    private void checkCreationReference(Rvalue reference, IType target) throws CompileException {
        IMethod sam = this.getSam(reference, target);
        IType[] inputs = this.functionalParameters(target, sam);
        IType output = this.functionalReturn(target, sam);
        if (output instanceof IWildcardType) output = ((IWildcardType) output).getUpperBound();
        Type qualifier = reference instanceof ClassInstanceCreationReference ? ((ClassInstanceCreationReference) reference).type
            : ((ArrayCreationReference) reference).type;
        UnitCompiler.setScopeIfUnset(qualifier, reference.getEnclosingScope());
        this.validateReferenceQualifier(qualifier, reference instanceof ArrayCreationReference);
        IType type = this.getType(qualifier);
        if (reference instanceof ArrayCreationReference) {
            if (!this.isReifiable(type) || inputs.length != 1 || !this.genericConvertible(inputs[0], IClass.INT, true)
                || output != IClass.VOID && !this.genericConvertible(type, output, false)) {
                throw UnitCompiler.compileException(reference, "Incompatible array creation reference");
            }
            return;
        }
        ClassInstanceCreationReference constructorReference = (ClassInstanceCreationReference) reference;
        IClass raw = UnitCompiler.rawTypeOf(type);
        if (raw.isInterface() || raw.isAbstract()) throw UnitCompiler.compileException(reference, "Cannot instantiate abstract reference type");
        boolean inferClass = !(type instanceof ParameterizedIClass) && raw.getITypeVariables().length != 0;
        if (inferClass && constructorReference.typeArguments != null) {
            throw UnitCompiler.compileException(reference, "Raw constructor reference cannot have explicit constructor type arguments");
        }
        Rvalue[] arguments = new Rvalue[inputs.length];
        for (int i = 0; i < inputs.length; i++) arguments[i] = new LocalVariableAccess(reference.getLocation(), new LocalVariable(true, inputs[i]));
        NewClassInstance creation = new NewClassInstance(reference.getLocation(), null, new SimpleType(reference.getLocation(), type), arguments);
        creation.typeArguments = this.copyReferenceArguments(constructorReference.typeArguments, reference.getEnclosingScope());
        creation.setEnclosingScope(reference.getEnclosingScope());
        if (inferClass) {
            this.inferredReferenceCreations.add(creation);
            if (output != IClass.VOID) this.creationTargets.put(creation, output);
        }
        this.speculativeInvocationDepth++;
        try {
            IType actual = this.getType(creation);
            if (output != IClass.VOID && !this.genericConvertible(actual, output, false)) {
                throw UnitCompiler.compileException(reference, "Incompatible constructor reference return type");
            }
            IConstructor constructor = (IConstructor) this.findMostSpecificIInvocable(creation, raw.getDeclaredIConstructors(), arguments,
                reference.getEnclosingScope());
            if (!this.isAccessible(constructor, reference.getEnclosingScope())) {
                throw UnitCompiler.compileException(reference, "Inaccessible constructor reference");
            }
            InferenceContext inference = this.constructorInference(creation, constructor, arguments, reference.getEnclosingScope(), false);
            IClass[] permitted = this.instantiatedExceptions(sam, this.receiverEnvironment(target, sam.getDeclaringIClass()));
            for (IClass exception : this.instantiatedExceptions(constructor, inference.values)) {
                if (this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(exception)
                    || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(exception)) continue;
                boolean declared = false;
                for (IClass allowed : permitted) if (allowed.isAssignableFrom(exception)) declared = true;
                if (!declared) throw UnitCompiler.compileException(reference, "Constructor reference throws an incompatible checked exception");
            }
        } finally {
            this.speculativeInvocationDepth--;
            this.inferredReferenceCreations.remove(creation);
            this.creationTargets.remove(creation);
        }
    }

    private GenericType functionalPattern(GenericType type, Map<String, GenericType> patterns) {
        if (type instanceof GenericType.TypeVariable) {
            GenericType pattern = patterns.get(((GenericType.TypeVariable) type).name);
            if (pattern instanceof GenericType.Wildcard) pattern = ((GenericType.Wildcard) pattern).bound;
            return pattern == null ? type : pattern;
        }
        if (type instanceof GenericType.ArrayOf) return new GenericType.ArrayOf(this.functionalPattern(((GenericType.ArrayOf) type).componentType, patterns));
        if (type instanceof GenericType.Parameterized) {
            GenericType.Parameterized parameter = (GenericType.Parameterized) type;
            GenericType[] arguments = new GenericType[parameter.typeArguments.length];
            for (int i = 0; i < arguments.length; i++) arguments[i] = this.functionalPattern(parameter.typeArguments[i], patterns);
            return new GenericType.Parameterized(parameter.rawType, arguments,
                parameter.ownerType == null ? null : this.functionalPattern(parameter.ownerType, patterns));
        }
        return type;
    }

    private void inferReferenceArguments(Rvalue[] arguments, GenericType[] parameters, InferenceContext context)
    throws CompileException { this.inferReferenceArguments(arguments, parameters, context, false); }

    private void inferReferenceArguments(Rvalue[] arguments, GenericType[] parameters, InferenceContext context, boolean preliminary)
    throws CompileException {
        for (int i = 0; i < parameters.length; i++) {
            this.seedLambdaInference(context);
            Rvalue argument = arguments[i];
            while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
            if (!(parameters[i] instanceof GenericType.Parameterized)) continue;
            if (argument instanceof ClassInstanceCreationReference || argument instanceof ArrayCreationReference) {
                GenericType.Parameterized parameter = (GenericType.Parameterized) parameters[i];
                Type type = argument instanceof ClassInstanceCreationReference ? ((ClassInstanceCreationReference) argument).type
                    : ((ArrayCreationReference) argument).type;
                UnitCompiler.setScopeIfUnset(type, argument.getEnclosingScope());
                IMethod sam = this.getSam(argument, parameter.rawType);
                if (argument instanceof ArrayCreationReference && sam.getParameterTypes().length != 1) {
                    context.valid = false; continue;
                }
                Map<String, GenericType> patterns = new HashMap<>();
                GenericType.Parameterized view = this.genericPatternSupertype(parameter, sam.getDeclaringIClass(), new HashSet<IClass>());
                this.bindGenericVariables(view == null ? parameter : view, patterns);
                GenericType result = this.getGenericReturnType(sam);
                if (result != null && sam.getReturnType() != IClass.VOID) {
                    context.constrain(this.functionalPattern(result, patterns), this.getType(type), 0);
                }
                continue;
            }
            if (!(argument instanceof MethodReference)) continue;
            MethodReference reference = (MethodReference) argument;
            GenericType.Parameterized parameter = (GenericType.Parameterized) parameters[i];
            IMethod sam = this.getSam(reference, parameter.rawType);
            GenericType[] descriptor = this.getGenericParameterTypes(sam);
            if (descriptor == null) {
                descriptor = new GenericType[sam.getParameterTypes().length];
                for (int j = 0; j < descriptor.length; j++) descriptor[j] = new GenericType.Raw(sam.getParameterTypes()[j]);
            }
            Map<String, GenericType> patterns = new HashMap<>();
            GenericType.Parameterized view = this.genericPatternSupertype(parameter, sam.getDeclaringIClass(), new HashSet<IClass>());
            this.bindGenericVariables(view == null ? parameter : view, patterns);
            if (reference.lhs instanceof Rvalue) UnitCompiler.setRvalueScopeIfUnset((Rvalue) reference.lhs, reference.getEnclosingScope());
            Atom lhs = reference.lhs instanceof AmbiguousName ? this.reclassify((AmbiguousName) reference.lhs) : reference.lhs;
            Type type = lhs.toType();
            IType receiver;
            if (type == null) receiver = this.getType(lhs);
            else { UnitCompiler.setScopeIfUnset(type, reference.getEnclosingScope()); receiver = this.getType(type); }
            List<IMethod> methods = new ArrayList<>();
            this.getIMethods(UnitCompiler.rawTypeOf(receiver), reference.methodName, methods);
            IMethod exact = null;
            int receiverArguments = 0;
            boolean ambiguous = false;
            for (IMethod candidate : methods) {
                if (!this.isAccessible(candidate, reference.getEnclosingScope(), receiver)) continue;
                if (type == null && candidate.isStatic()) continue;
                int offset = type != null && !candidate.isStatic() ? 1 : 0;
                if (candidate.isVarargs()) {
                    if (descriptor.length >= candidate.getParameterTypes().length - 1 + offset) ambiguous = true;
                    continue;
                }
                if (candidate.getParameterTypes().length + offset != descriptor.length) continue;
                if (exact != null && !Arrays.equals(exact.getParameterTypes(), candidate.getParameterTypes())) { ambiguous = true; break; }
                if (exact == null || exact.getDeclaringIClass().isAssignableFrom(candidate.getDeclaringIClass())
                    || !candidate.getDeclaringIClass().isAssignableFrom(exact.getDeclaringIClass())
                        && exact.getReturnType().isAssignableFrom(candidate.getReturnType())) {
                    exact = candidate; receiverArguments = offset;
                }
            }
            Set<String> methodVariables = exact == null ? null : this.getMethodTypeParameterNames(exact);
            if (preliminary && (exact == null || ambiguous || methodVariables != null && !methodVariables.isEmpty())) continue;
            if (exact == null || ambiguous || methodVariables != null && !methodVariables.isEmpty()) {
                // An overloaded/varargs reference can contribute its result once descriptor inputs are proper.
                boolean proper = true;
                for (GenericType input : descriptor) if (this.substituteGenericType(this.functionalPattern(input, patterns), context.values) == null) proper = false;
                if (proper) {
                    IType target = this.inferenceTarget(parameter, context);
                    if (target != null) {
                        try {
                            ReferenceBinding binding = this.resolveReference(reference, target);
                            GenericType result = this.getGenericReturnType(sam);
                            if (result != null && binding.result != IClass.VOID) context.constrain(this.functionalPattern(result, patterns),
                                UnitCompiler.isPrimitive(binding.result) ? this.wrapperOf(UnitCompiler.rawTypeOf(binding.result)) : binding.result, 0);
                            this.inferReferenceExceptions(sam, patterns, binding.exceptions, context);
                        } catch (CompileException ignored) { /* Another argument may supply the remaining constraints. */ }
                    }
                }
                continue;
            }
            if (receiverArguments == 1 && !(receiver instanceof ParameterizedIClass)) {
                IType input = this.substituteGenericType(this.functionalPattern(descriptor[0], patterns), context.values);
                IType receiverView = input == null ? null : this.asSupertype(input, UnitCompiler.rawTypeOf(receiver));
                if (receiverView != null) receiver = receiverView;
            }
            Map<String, IType> member = this.receiverEnvironment(receiver, exact.getDeclaringIClass());
            if (receiverArguments == 1) context.constrain(this.functionalPattern(descriptor[0], patterns), receiver, -1);
            GenericType[] formal = this.getGenericParameterTypes(exact);
            for (int j = receiverArguments; j < descriptor.length; j++) {
                IType input = formal == null ? null : this.substituteGenericType(formal[j - receiverArguments], member);
                if (input == null) input = exact.getParameterTypes()[j - receiverArguments];
                context.constrain(this.functionalPattern(descriptor[j], patterns), input, -1);
            }
            GenericType result = this.getGenericReturnType(exact), samResult = this.getGenericReturnType(sam);
            IType output = result == null ? null : this.substituteGenericType(result, member);
            if (output == null && result != null && preliminary) continue;
            if (output == null) output = exact.getReturnType();
            if (samResult != null && output != IClass.VOID) context.constrain(this.functionalPattern(samResult, patterns),
                UnitCompiler.isPrimitive(output) ? this.wrapperOf(UnitCompiler.rawTypeOf(output)) : output, 0);
            this.inferReferenceExceptions(sam, patterns, this.instantiatedExceptions(exact, member), context);
        }
    }

    private void inferReferenceExceptions(IMethod sam, Map<String, GenericType> patterns, IClass[] exceptions, InferenceContext context)
    throws CompileException {
        GenericType[] declared = sam.getGenericThrownExceptions();
        if (declared == null) return;
        for (IClass exception : exceptions) {
            if (this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(exception)
                || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(exception)) continue;
            for (GenericType type : declared) context.constrain(this.functionalPattern(type, patterns), exception, 0);
        }
    }

    /** Candidate-local lambda constraints; the original body is neither targeted nor lowered while probing. */
    private void inferLambdaArguments(Rvalue[] arguments, GenericType[] patterns, InferenceContext context, boolean inferThrows)
    throws CompileException {
        int previous;
        boolean defaults = false;
        do {
            previous = context.constraintCount();
            this.seedLambdaInference(context);
            for (int i = 0; i < arguments.length && i < patterns.length; i++) {
                if (patterns[i] != null) this.inferLambdaArgument(arguments[i], patterns[i], context, inferThrows, defaults);
            }
            if (context.constraintCount() != previous) continue;
            if (defaults) break;
            defaults = true; // Other lambdas had the first opportunity to constrain implicit parameter types.
        } while (context.valid);
    }

    private void seedLambdaInference(InferenceContext context) throws CompileException {
        int previous;
        do {
            previous = context.constraintCount();
            context.values.putAll(context.equals);
            for (String name : context.variables) {
                if (context.equals.containsKey(name)) continue;
                IType type = null;
                List<IType> lower = context.lowers.get(name);
                if (lower != null) for (IType bound : lower) type = type == null ? bound : this.commonSupertype(type, bound);
                if (type != null) context.values.put(name, type);
            }
            // Incorporate declared bounds before freezing a nested call's invariant target.
            // For T extends R<T> and R<R> as an argument, the raw R instantiation is the
            // unchecked solution; passing the provisional R<R> to Listener<T> pins it too early.
            for (Map.Entry<String, GenericType[]> entry : context.bounds.entrySet()) {
                IType value = context.values.get(entry.getKey());
                if (value != null && (context.equals.containsKey(entry.getKey()) || context.lowers.containsKey(entry.getKey()))) {
                    for (GenericType bound : entry.getValue()) context.constrain(bound, value, 0);
                }
            }
        } while (context.valid && previous != context.constraintCount());
        context.values.putAll(context.equals);
    }

    private void inferLambdaArgument(
        Rvalue argument, GenericType pattern, InferenceContext context, boolean inferThrows, boolean defaults
    ) throws CompileException {
        while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
        if (argument instanceof ConditionalExpression) {
            this.inferLambdaArgument(((ConditionalExpression) argument).mhs, pattern, context, inferThrows, defaults);
            this.inferLambdaArgument(((ConditionalExpression) argument).rhs, pattern, context, inferThrows, defaults);
            return;
        }
        if (!(argument instanceof LambdaExpression) || !(pattern instanceof GenericType.Parameterized)) return;
        LambdaExpression lambda = (LambdaExpression) argument;
        GenericType.Parameterized parameter = (GenericType.Parameterized) pattern;
        IMethod sam = this.getSam(lambda, parameter.rawType);
        GenericType.Parameterized view = this.genericPatternSupertype(parameter, sam.getDeclaringIClass(), new HashSet<IClass>());
        Map<String, GenericType> environment = new HashMap<>();
        this.bindGenericVariables(view == null ? parameter : view, environment);
        GenericType[] declared = this.getGenericParameterTypes(sam);
        IType[] inputs = new IType[sam.getParameterTypes().length];
        for (int i = 0; i < inputs.length; i++) {
            GenericType input = declared == null ? new GenericType.Raw(sam.getParameterTypes()[i])
                : this.functionalPattern(declared[i], environment);
            if (lambda.parameters instanceof FormalLambdaParameters) {
                FormalParameter[] parameters = ((FormalLambdaParameters) lambda.parameters).formalParameters.parameters;
                if (parameters.length != inputs.length) { context.valid = false; return; }
                UnitCompiler.setScopeIfUnset(parameters[i].type, lambda.getEnclosingScope());
                context.constrain(input, this.getType(parameters[i].type), 1);
                this.seedLambdaInference(context);
            }
            inputs[i] = this.substituteGenericType(input, context.values);
            if (inputs[i] == null) {
                if (!defaults) return;
                Map<String, IType> provisional = new HashMap<>(context.values);
                for (String variable : context.variables) if (!provisional.containsKey(variable)) {
                    GenericType[] bounds = context.bounds.get(variable);
                    IType bound = bounds == null || bounds.length == 0 ? null : this.substituteGenericType(bounds[0], provisional);
                    if (bound == null) bound = this.iClassLoader.TYPE_java_lang_Object;
                    provisional.put(variable, bound);
                }
                inputs[i] = this.substituteGenericType(input, provisional);
                if (inputs[i] == null) return;
            }
        }
        GenericType result = this.getGenericReturnType(sam);
        if (result != null && sam.getReturnType() != IClass.VOID) {
            result = this.functionalPattern(result, environment);
            List<IType> previousProbe = this.lambdaProbeExceptions;
            this.lambdaProbeExceptions = new ArrayList<>();
            try {
                // Reuse the attributed expressions for a fixed descriptor. Fresh AST copies would
                // repeatedly manufacture distinct captures and keep the constraint loop growing.
                if (!Arrays.equals(inputs, context.lambdaInputs.get(lambda))) {
                    context.lambdaInputs.put(lambda, inputs.clone());
                    context.lambdaResults.put(lambda, this.lambdaResults(lambda, inputs));
                }
                List<Rvalue> results = context.lambdaResults.get(lambda);
                if (results != null) for (Rvalue expression : results) {
                    this.constrainLambdaResult(expression, result, context);
                }
            } finally { this.lambdaProbeExceptions = previousProbe; }
        }
        if (inferThrows) {
            GenericType[] exceptions = sam.getGenericThrownExceptions();
            if (exceptions != null && exceptions.length != 0) for (IType exception : this.lambdaExceptions(lambda, inputs)) {
                boolean covered = false;
                // Proper throws types do not need constraints on an inference variable.
                for (GenericType generic : exceptions) if (generic instanceof GenericType.Raw
                    && ((GenericType.Raw) generic).iClass.isAssignableFrom(UnitCompiler.rawTypeOf(exception))) covered = true;
                if (!covered) for (GenericType generic : exceptions) {
                    context.constrain(this.functionalPattern(generic, environment), exception, 0);
                }
            }
        }
    }

    private void constrainLambdaResult(Rvalue expression, GenericType result, InferenceContext context) throws CompileException {
        while (expression instanceof ParenthesizedExpression) expression = ((ParenthesizedExpression) expression).value;
        if (expression instanceof ConditionalExpression) {
            this.constrainLambdaResult(((ConditionalExpression) expression).mhs, result, context);
            this.constrainLambdaResult(((ConditionalExpression) expression).rhs, result, context);
            return;
        }
        context.constrain(result, this.invocationArgumentType(expression, this.substituteGenericType(result, context.values)), 0);
    }

    /** Target types for poly arguments, preserving the directly inferred method type variables. */
    private IType[]
    invocationTargetTypes(Invocation invocation, IMethod method) throws CompileException {
        return this.invocationTargetTypes(invocation, method, true);
    }

    private IType[] invocationTargetTypes(Invocation invocation, IMethod method, boolean inferThrows) throws CompileException {
        // Capture identities are not denotable in a class-file generic signature. The generated
        // lambda delegate retains the exact semantic formals instead of re-inferring their bounds.
        IType[] helperTargets = this.lambdaHelperTargets.get(invocation);
        if (helperTargets != null) return helperTargets.clone();
        IType[] targets = new IType[method.getParameterTypes().length];
        System.arraycopy(method.getParameterTypes(), 0, targets, 0, targets.length);
        GenericType[] parameters = this.getGenericParameterTypes(method);
        if (parameters == null || !method.isVarargs() && parameters.length != invocation.arguments.length) return targets;
        InferenceContext context = this.invocationInference(invocation, method, inferThrows);
        if (!context.valid) throw UnitCompiler.compileException(invocation, "Incompatible generic invocation constraints");
        GenericType[] patterns = this.invocationPatterns(invocation, method);
        if (patterns != null) for (int i = 0; i < patterns.length; i++) {
            if (this.isPolyExpression(invocation.arguments[i])) continue;
            IType target = this.substituteGenericType(patterns[i], context.values);
            IType actual = target == null ? null : this.invocationArgumentType(invocation.arguments[i], target);
            if (actual != null && !this.genericConvertible(actual, target, true)) {
                throw UnitCompiler.compileException(invocation.arguments[i], "Incompatible generic argument; actual " + actual + "; required " + target);
            }
        }
        for (int i = 0; i < parameters.length; i++) {
            IType target = i < invocation.arguments.length && this.isPolyExpression(invocation.arguments[i])
                ? this.substituteFunctionalTarget(parameters[i], context.values)
                : this.substituteGenericType(parameters[i], context.values);
            if (target != null) targets[i] = target;
        }
        return targets;
    }

    /** Ground a wildcard functional target using its bound, rather than erasing a contravariant input to Object. */
    @Nullable private IType
    substituteFunctionalTarget(GenericType type, Map<String, IType> environment) throws CompileException {
        if (!(type instanceof GenericType.Parameterized)) return this.substituteGenericType(type, environment);
        GenericType.Parameterized parameterized = (GenericType.Parameterized) type;
        IType[] arguments = new IType[parameterized.typeArguments.length];
        for (int i = 0; i < arguments.length; i++) {
            GenericType argument = parameterized.typeArguments[i];
            if (argument instanceof GenericType.Wildcard) argument = ((GenericType.Wildcard) argument).bound;
            arguments[i] = this.substituteGenericType(argument, environment);
            if (arguments[i] == null) return null;
        }
        IType owner = parameterized.ownerType == null ? null : this.substituteGenericType(parameterized.ownerType, environment);
        if (parameterized.ownerType != null && owner == null) return null;
        return new ParameterizedIClass(parameterized.rawType, arguments, owner);
    }

    @Nullable private GenericType[]
    getGenericParameterTypes(IClass.IMethod im) throws CompileException {
        GenericType[] metadata = im.getGenericParameterTypes();
        if (metadata != null) return metadata;
        MethodDeclarator source = this.sourceMethods.get(im);
        if (source != null) {
            FormalParameter[] parameters = source.formalParameters.parameters;
            GenericType[] result = new GenericType[parameters.length];
            for (int i = 0; i < result.length; i++) result[i] = this.sourceGenericType(parameters[i].type);
            return result;
        }
        IClass dc = im.getDeclaringIClass();
        if (dc instanceof ClassFileIClass) return ((ClassFileIClass) dc).getGenericParameterTypes(im);
        if (dc instanceof ReflectionIClass) return ((ReflectionIClass) dc).getGenericParameterTypes(im);
        return null;
    }

    /**
     * @return The return type of the invoked method, with the CLASS type variables replaced by the actual type
     *         arguments of a parameterized invocation target (see {@link #getType2(MethodInvocation)})
     */
    private IType
    substitutedReturnType(Invocation mi, IMethod iMethod) throws CompileException {

        if (iMethod.getDeclaringIClass() == this.iClassLoader.TYPE_java_lang_Object
            && "getClass".equals(mi.methodName) && mi.arguments.length == 0) {
            IType receiver = this.invocationReceiver(mi, iMethod.getDeclaringIClass());
            if (receiver != null) return new ParameterizedIClass(this.iClassLoader.TYPE_java_lang_Class,
                new IType[] { this.wildcard(true, UnitCompiler.rawTypeOf(receiver)) });
        }

        // Array.clone() has the array's type in the language, but returns Object in its JVM descriptor.
        if (iMethod.getDeclaringIClass().isArray() && "clone".equals(mi.methodName)) {
            IType array = this.invocationReceiver(mi);
            return array != null && UnitCompiler.isArray(array) ? array : iMethod.getDeclaringIClass();
        }

        GenericType declaredReturn = this.getGenericReturnType(iMethod);
        if (declaredReturn == null) return iMethod.getReturnType();
        if (declaredReturn instanceof GenericType.Raw) return ((GenericType.Raw) declaredReturn).iClass;

        // (1) Substitute the CLASS type variables from a PARAMETERIZED receiver.
        IType receiver = this.invocationReceiver(mi, iMethod.getDeclaringIClass());
        if (receiver != null) {
            IType tt = this.asSupertype(receiver, iMethod.getDeclaringIClass());
            if (
                tt instanceof ParameterizedIClass
                && iMethod.getDeclaringIClass() == ((ParameterizedIClass) tt).rawType
            ) {
                GenericType grt = this.getGenericReturnType(iMethod);
                if (grt != null) {
                    IType substituted = this.substituteTypeVariables(
                        grt,
                        iMethod.getDeclaringIClass(),
                        (ParameterizedIClass) tt,
                        iMethod
                    );
                    if (substituted != null) return substituted;
                }
            }
        }

        GenericType[] parameters = this.getGenericParameterTypes(iMethod);
        if (parameters != null && (iMethod.isVarargs() || parameters.length == mi.arguments.length)) {
            InferenceContext context = this.invocationInference(mi, iMethod);
            IType inferred = this.substituteGenericType(declaredReturn, context.values);
            if (inferred != null) return inferred;
        }

        GenericType genericReturn = this.getGenericReturnType(iMethod);
        if (genericReturn != null) {
            IType concreteReturn = this.substituteGenericType(genericReturn, Collections.<String, IType>emptyMap());
            if (concreteReturn != null) return concreteReturn;
        }
        return iMethod.getReturnType();
    }

    private static boolean
    isStaticContext(TypeBodyDeclaration tbd) {
        if (tbd instanceof FieldDeclaration)       return ((FieldDeclaration)       tbd).isStatic() || ((FieldDeclaration) tbd).getDeclaringType() instanceof InterfaceDeclaration;
        if (tbd instanceof MethodDeclarator)       return ((MethodDeclarator)       tbd).isStatic();
        if (tbd instanceof Initializer)            return ((Initializer)            tbd).isStatic();
        if (tbd instanceof MemberClassDeclaration) return ((MemberClassDeclaration) tbd).isStatic();

        return false;
    }

    private static boolean
    mayHaveSideEffects(ArrayInitializerOrRvalue... arrayInitializersOrRvalues) {

        for (ArrayInitializerOrRvalue aiorv : arrayInitializersOrRvalues) {
            if (UnitCompiler.mayHaveSideEffects(aiorv)) return true;
        }

        return false;
    }

    private static boolean
    mayHaveSideEffects(ArrayInitializerOrRvalue arrayInitializerOrRvalue) {
        Boolean result = (Boolean) arrayInitializerOrRvalue.accept(UnitCompiler.MAY_HAVE_SIDE_EFFECTS_VISITOR);
        assert result != null;
        return result;
    }

    private static final ArrayInitializerOrRvalueVisitor<Boolean, RuntimeException>
    MAY_HAVE_SIDE_EFFECTS_VISITOR = new ArrayInitializerOrRvalueVisitor<Boolean, RuntimeException>() {

        @Override @Nullable public Boolean visitRvalue(Rvalue rvalue)                 { return (Boolean) rvalue.accept(this.rvalueVisitor); }
        @Override @Nullable public Boolean visitArrayInitializer(ArrayInitializer ai) { return UnitCompiler.mayHaveSideEffects(ai.values);  }

        final LvalueVisitor<Boolean, RuntimeException>
        lvalueVisitor = new LvalueVisitor<Boolean, RuntimeException>() {
            @Override @Nullable public Boolean visitAmbiguousName(AmbiguousName an)                                        { return false;                                               }
            @Override @Nullable public Boolean visitArrayAccessExpression(ArrayAccessExpression aae)                       { return UnitCompiler.mayHaveSideEffects(aae.lhs, aae.index); }
            @Override @Nullable public Boolean visitFieldAccess(FieldAccess fa)                                            { return false;                                               }
            @Override @Nullable public Boolean visitFieldAccessExpression(FieldAccessExpression fae)                       { return false;                                               }
            @Override @Nullable public Boolean visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { return false;                                               }
            @Override @Nullable public Boolean visitLocalVariableAccess(LocalVariableAccess lva)                           { return false;                                               }
            @Override @Nullable public Boolean visitParenthesizedExpression(ParenthesizedExpression pe)                    { return UnitCompiler.mayHaveSideEffects(pe.value);           }
        };

        final RvalueVisitor<Boolean, RuntimeException>
        rvalueVisitor = new RvalueVisitor<Boolean, RuntimeException>() {

            @SuppressWarnings("unqualified-field-access") @Override @Nullable public Boolean
            visitLvalue(Lvalue lv) { return (Boolean) lv.accept(lvalueVisitor); }

            @Override @Nullable public Boolean visitArrayLength(ArrayLength al)                                    { return UnitCompiler.mayHaveSideEffects(al.lhs);                 }
            @Override @Nullable public Boolean visitAssignment(Assignment a)                                       { return true;                                                    }
            @Override @Nullable public Boolean visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.mayHaveSideEffects(uo.operand);             }
            @Override @Nullable public Boolean visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.mayHaveSideEffects(bo.lhs, bo.rhs);         }
            @Override @Nullable public Boolean visitCast(Cast c)                                                   { return UnitCompiler.mayHaveSideEffects(c.value);                }
            @Override @Nullable public Boolean visitClassLiteral(ClassLiteral cl)                                  { return false;                                                   }
            @Override @Nullable public Boolean visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.mayHaveSideEffects(ce.lhs, ce.mhs, ce.rhs); }
            @Override @Nullable public Boolean visitCrement(Crement c)                                             { return true;                                                    }
            @Override @Nullable public Boolean visitInstanceof(Instanceof io)                                      { return false;                                                   }
            @Override @Nullable public Boolean visitMethodInvocation(MethodInvocation mi)                          { return true;                                                    }
            @Override @Nullable public Boolean visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return true;                                                    }
            @Override @Nullable public Boolean visitIntegerLiteral(IntegerLiteral il)                              { return false;                                                   }
            @Override @Nullable public Boolean visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return false;                                                   }
            @Override @Nullable public Boolean visitBooleanLiteral(BooleanLiteral bl)                              { return false;                                                   }
            @Override @Nullable public Boolean visitCharacterLiteral(CharacterLiteral cl)                          { return false;                                                   }
            @Override @Nullable public Boolean visitStringLiteral(StringLiteral sl)                                { return false;                                                   }
            @Override @Nullable public Boolean visitTextBlock(TextBlock tb)                                        { return false;                                                   }
            @Override @Nullable public Boolean visitNullLiteral(NullLiteral nl)                                    { return false;                                                   }
            @Override @Nullable public Boolean visitSimpleConstant(SimpleConstant sl)                              { return false;                                                   }
            @Override @Nullable public Boolean visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return true;                                                    }
            @Override @Nullable public Boolean visitNewArray(NewArray na)                                          { return UnitCompiler.mayHaveSideEffects(na.dimExprs);            }
            @Override @Nullable public Boolean visitNewInitializedArray(NewInitializedArray nia)                   { return UnitCompiler.mayHaveSideEffects(nia.arrayInitializer);   }
            @Override @Nullable public Boolean visitNewClassInstance(NewClassInstance nci)                         { return true;                                                    }
            @Override @Nullable public Boolean visitParameterAccess(ParameterAccess pa)                            { return false;                                                   }
            @Override @Nullable public Boolean visitQualifiedThisReference(QualifiedThisReference qtr)             { return false;                                                   }
            @Override @Nullable public Boolean visitThisReference(ThisReference tr)                                { return false;                                                   }
            @Override @Nullable public Boolean visitLambdaExpression(LambdaExpression le)                          { return true;                                                    }
            @Override @Nullable public Boolean visitMethodReference(MethodReference mr)                            { return true;                                                    }
            @Override @Nullable public Boolean visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return true;                                                    }
            @Override @Nullable public Boolean visitArrayCreationReference(ArrayCreationReference acr)             { return false;                                                   }
        };
    };

    private IType
    compileGet2(SuperclassMethodInvocation scmi) throws CompileException {
        return this.compileSuperclassInvocation(scmi, false);
    }

    private IType compileSuperclassInvocation(SuperclassMethodInvocation scmi, boolean discardResult) throws CompileException {
        final IClass.IMethod iMethod = this.findIMethod(scmi);

        Scope s;
        for (
            s = scmi.getEnclosingScope();
            s instanceof Statement || s instanceof CatchClause;
            s = s.getEnclosingScope()
        );
        FunctionDeclarator fd = s instanceof FunctionDeclarator ? (FunctionDeclarator) s : null;
        if (fd == null) {
            this.compileError("Cannot invoke superclass method in non-method scope", scmi.getLocation());
            return IClass.INT;
        }
        if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isStatic()) {
            this.compileError("Cannot invoke superclass method in static context", scmi.getLocation());
        }
        if (scmi.receiverType != this.resolve(fd.getDeclaringType())) {
            Scope enclosing = scmi.getEnclosingScope();
            while (!(enclosing instanceof AbstractTypeDeclaration) || this.resolve((AbstractTypeDeclaration) enclosing) != scmi.receiverType) {
                enclosing = enclosing.getEnclosingScope();
            }
            AbstractTypeDeclaration owner = (AbstractTypeDeclaration) enclosing;
            Location location = scmi.getLocation();
            String name;
            do { name = "super$bridge$" + owner.getClassName().replace('.', '_') + "$" + (++this.lambdaMethodCount); } while (owner.getMethodDeclaration(name) != null);
            IClass[] types = iMethod.getParameterTypes();
            FormalParameter[] parameters = new FormalParameter[types.length];
            Rvalue[] values = new Rvalue[types.length];
            for (int i = 0; i < types.length; i++) {
                parameters[i] = new FormalParameter(location, new Modifier[0], new SimpleType(location, iMethod.isVarargs() && i == types.length - 1 ? types[i].getComponentType() : types[i]), "arg$" + i);
                values[i] = new AmbiguousName(location, new String[] { "arg$" + i });
            }
            SuperclassMethodInvocation invocation = new SuperclassMethodInvocation(location, null, iMethod.getName(), values);
            invocation.iMethod = iMethod;
            invocation.receiverType = scmi.receiverType;
            invocation.invocationType = scmi.invocationType;
            Type[] exceptions = new Type[iMethod.getThrownExceptions().length];
            for (int i = 0; i < exceptions.length; i++) exceptions[i] = new SimpleType(location, iMethod.getThrownExceptions()[i]);
            List<BlockStatement> body = new ArrayList<>();
            body.add(iMethod.getReturnType() == IClass.VOID ? new ExpressionStatement(invocation) : new ReturnStatement(location, invocation));
            owner.addDeclaredMethod(new MethodDeclarator(location, null, owner instanceof InterfaceDeclaration
                ? Java.accessModifiers(location, "public", "default") : Java.accessModifiers(location, "public", "final"), null,
                new SimpleType(location, iMethod.getReturnType()), name, new FormalParameters(location, parameters, iMethod.isVarargs()), exceptions, null, body));
            scmi.receiverType.invalidateMethodCaches();
            MethodInvocation call = new MethodInvocation(location,
                new QualifiedThisReference(location, new SimpleType(location, scmi.receiverType)), name, scmi.arguments);
            call.setEnclosingScope(scmi.getEnclosingScope());
            call.iMethod = scmi.receiverType.getDeclaredIMethods(name)[0];
            // Pack varargs exactly as for the original method, before calling the fixed-arity bridge.
            call.argumentsNeedAdjust = scmi.argumentsNeedAdjust;
            IType value = this.compileGet2(call);
            return this.adaptReferenceResult(scmi, this.substitutedReturnType(scmi, iMethod), UnitCompiler.rawTypeOf(value),
                this.invocationTargets.get(scmi), discardResult);
        }
        this.bytecodeEmitter.load(scmi, scmi.receiverType, 0);

        IClass[] parameterTypes = iMethod.getParameterTypes();
        Rvalue[] arguments = scmi.arguments;
        if (iMethod.isVarargs() && scmi.argumentsNeedAdjust) {
            int fixed = parameterTypes.length - 1;
            Rvalue[] tail = new Rvalue[arguments.length - fixed];
            System.arraycopy(arguments, fixed, tail, 0, tail.length);
            arguments = new Rvalue[parameterTypes.length];
            System.arraycopy(scmi.arguments, 0, arguments, 0, fixed);
            arguments[fixed] = new NewInitializedArray(
                scmi.getLocation(), parameterTypes[fixed], new ArrayInitializer(scmi.getLocation(), tail)
            );
        }
        IType[] targets = this.invocationTargetTypes(scmi, iMethod);
        for (int i = 0; i < arguments.length; ++i) {
            this.setLambdaTargetType(arguments[i], targets[i]);
            this.assignmentConversion(scmi, this.compileGetValue(arguments[i]), parameterTypes[i], this.constantEvaluator.getConstantValue(arguments[i]));
        }
        if (scmi.invocationType.isInterface() && this.getTargetVersion() < 8) {
            throw UnitCompiler.compileException(scmi, "Interface super calls require target version 8 or above");
        }

        if (this.getTargetVersion() >= 8 && scmi.typeArguments != null && !this.referenceDelegates.contains(scmi)) {
            CodeContext.Offset offset = this.getCodeContext().newOffset();
            for (int i = 0; i < scmi.typeArguments.length; i++) if (scmi.typeArguments[i] instanceof Type) this.annotationCompiler.compileCodeTypeAnnotations(
                (Type) scmi.typeArguments[i], new Annotation[0], (byte) 0x49, this.getCodeContext(), offset, null, i);
        }
        // Invoke!
        this.bytecodeEmitter.invoke(
            scmi,                         // locatable
            Opcode.INVOKESPECIAL,         // opcode
            scmi.invocationType,          // declaringIClass
            iMethod.getName(),            // methodName
            iMethod.getDescriptor(),      // methodMd
            scmi.invocationType.isInterface() // useInterfaceMethodref
        );

        return this.adaptReferenceResult(scmi, this.substitutedReturnType(scmi, iMethod), iMethod.getReturnType(),
            this.invocationTargets.get(scmi), discardResult);
    }

    private IType
    compileGet2(NewClassInstance nci) throws CompileException {

        IType iType;
        if (nci.iType != null) {
            iType = nci.iType;
        } else {
            assert nci.type != null;
            iType = this.getType2(nci);
        }

        if (iType instanceof SourceTypeVariable) throw UnitCompiler.compileException(nci, "Cannot instantiate a type variable");
        if (iType instanceof ParameterizedIClass) for (IType argument : ((ParameterizedIClass) iType).actualTypeArguments) {
            if (argument instanceof IWildcardType) throw UnitCompiler.compileException(nci, "Cannot instantiate a wildcard parameterization");
        }
        IClass rawType = UnitCompiler.rawTypeOf(iType);

        if (rawType.isInterface()) {
            this.compileError("Cannot instantiate \"" + iType + "\"", nci.getLocation());
        }
        this.checkAccessible(rawType, nci.getEnclosingScope(), nci.getLocation());
        if (rawType.isAbstract()) {
            this.compileError("Cannot instantiate abstract \"" + iType + "\"", nci.getLocation());
        }

        // Determine the enclosing instance for the new object.
        Rvalue enclosingInstance;
        if (nci.qualification != null) {
            if (rawType.getOuterIClass() == null) {
                this.compileError("Static member class cannot be instantiated with qualified NEW");
            }

            // Enclosing instance defined by qualification (JLS7 15.9.2.BL1.B3.B2).
            enclosingInstance = nci.qualification;
        } else {
            Scope s = nci.getEnclosingScope();

            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            TypeBodyDeclaration enclosingTypeBodyDeclaration = (TypeBodyDeclaration) s;

            TypeDeclaration enclosingTypeDeclaration = (TypeDeclaration) s.getEnclosingScope();

            if (
                !(enclosingTypeDeclaration instanceof AbstractTypeDeclaration)
                || (
                    enclosingTypeBodyDeclaration instanceof MemberClassDeclaration
                    && ((MemberClassDeclaration) enclosingTypeBodyDeclaration).isStatic()
                )
                || (
                    enclosingTypeBodyDeclaration instanceof PackageMemberClassDeclaration
                    && ((PackageMemberClassDeclaration) enclosingTypeBodyDeclaration).isStatic()
                )
            ) {

                // No enclosing instance in
                //  + interface method declaration or
                //  + static type body declaration (here: method or initializer or field declarator)
                // context (JLS7 15.9.2.BL1.B3.B1.B1).
                if (rawType.getOuterIClass() != null) {
                    this.compileError((
                        "Instantiation of \""
                        + (nci.type != null ? nci.type.toString() : String.valueOf(nci.iType))
                        + "\" requires an enclosing instance"
                    ), nci.getLocation());
                }
                enclosingInstance = null;
            } else
            {

                // Determine the type of the enclosing instance for the new object.
                IClass outerIClass = rawType.getDeclaringIClass();
                if (outerIClass == null) {

                    // No enclosing instance needed for a top-level class object.
                    enclosingInstance = null;
                } else {

                    // Find an appropriate enclosing instance for the new inner class object among the enclosing
                    // instances of the current object (JLS7 15.9.2.BL1.B3.B1.B2).
                    enclosingInstance = new QualifiedThisReference(
                        nci.getLocation(), // location
                        new SimpleType(    // qualification
                            nci.getLocation(),
                            outerIClass
                        )
                    );
                    enclosingInstance.setEnclosingScope(nci.getEnclosingScope());
                }
            }
        }

        if (this.getTargetVersion() >= 8 && nci.type != null) this.annotationCompiler.compileCodeTypeAnnotations(nci.type, new Annotation[0],
            (byte) 0x44, this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
        this.bytecodeEmitter.neW(nci, iType);
        this.bytecodeEmitter.dup(nci);
        this.invokeConstructor(
            nci,                     // l
            nci.getEnclosingScope(), // scope
            enclosingInstance,       // enclosingInstance
            iType,                   // targetClass
            nci.arguments            // arguments
        );
        this.getCodeContext().popUninitializedVariableOperand();
        this.getCodeContext().pushObjectOperand(rawType.getDescriptor());

        return iType;
    }

    private IClass
    compileGet2(NewAnonymousClassInstance naci) throws CompileException {
        AnonymousClassDeclaration acd = naci.anonymousClassDeclaration;

        // Find constructors of superclass.
        IClass sc = this.resolve(acd).getSuperclass();
        assert sc != null;

        IClass.IConstructor[] superclassIConstructors = sc.getDeclaredIConstructors();
        if (superclassIConstructors.length == 0) {
            throw new InternalCompilerException(naci.getLocation(), "SNO: Superclass has no constructors");
        }

        // Determine the most specific constructor of the superclass.
        IClass.IConstructor superclassIConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            naci,                    // locatable
            superclassIConstructors, // iInvocables
            naci.arguments,          // arguments
            acd                      // contextScope
        );

        Location loc           = naci.getLocation();
        Rvalue   qualification = naci.qualification;

        // Determine the formal parameters of the anonymous constructor.
        IClass[]         scpts = superclassIConstructor.getParameterTypes();
        boolean anonymousVarargs = superclassIConstructor.isVarargs();
        FormalParameters parameters;
        {
            List<FormalParameter> l = new ArrayList<>();

            // Pass the enclosing instance of the base class as parameter #1.
            if (qualification != null) l.add(new FormalParameter(
                loc,                                              // location
                UnitCompiler.accessModifiers(loc, "final"),       // modifiers
                new SimpleType(loc, this.getType(qualification)), // type
                "this$base"                                       // name
            ));
            for (int i = 0; i < scpts.length; ++i) l.add(new FormalParameter(
                loc,                                        // location
                UnitCompiler.accessModifiers(loc, "final"), // modifiers
                new SimpleType(loc, anonymousVarargs && i == scpts.length - 1 ? scpts[i].getComponentType() : scpts[i]), // type
                "p" + i                                     // name
            ));
            parameters = new FormalParameters(
                loc,
                (FormalParameter[]) l.toArray(new FormalParameter[l.size()]),
                anonymousVarargs
            );
        }

        // Determine the declared exceptions of the anonymous constructor.
        Type[] thrownExceptions;
        {
            IClass[] tes = superclassIConstructor.getThrownExceptions();
            thrownExceptions = new Type[tes.length];
            for (int i = 0; i < tes.length; ++i) thrownExceptions[i] = new SimpleType(loc, tes[i]);
        }

        // The anonymous constructor merely invokes the constructor of its superclass.
        int    j = 0;
        Rvalue qualificationAccess;
        if (qualification == null) {
            qualificationAccess = null;
        } else
        {
            qualificationAccess = new ParameterAccess(loc, parameters.parameters[j++]);
        }
        Rvalue[] parameterAccesses = new Rvalue[scpts.length];
        for (int i = 0; i < scpts.length; ++i) {
            parameterAccesses[i] = new ParameterAccess(loc, parameters.parameters[j++]);
        }

        // Generate the anonymous constructor for the anonymous class (JLS7 15.9.5.1).
        acd.addConstructor(new ConstructorDeclarator(
            loc,                                    // location
            null,                                   // docComment
            new Modifier[0],                        // modifiers
            parameters,                             // parameters
            thrownExceptions,                       // thrownExceptions
            new SuperConstructorInvocation(         // constructorInvocation
                loc,                            // location
                qualificationAccess,    // qualification
                parameterAccesses               // arguments
            ),
            Collections.<BlockStatement>emptyList() // statements
        ));

        // Compile the anonymous class.
        final IClass iClass = this.resolve(naci.anonymousClassDeclaration);
        try {
            this.typeDeclarationCompiler.compile(acd);

            // Instantiate the anonymous class.
            IClass anonymousIClass = this.resolve(acd);
            if (this.getTargetVersion() >= 8) this.annotationCompiler.compileCodeTypeAnnotations(acd.baseType, new Annotation[0], (byte) 0x44,
                this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
            this.compileReferenceAnnotations(naci);
            this.bytecodeEmitter.neW(naci, anonymousIClass);

            // Invoke the anonymous constructor.
            this.bytecodeEmitter.dup(naci);
            Rvalue[] arguments2;
            if (qualification == null) {
                arguments2 = naci.arguments;
            } else {
                arguments2    = new Rvalue[naci.arguments.length + 1];
                arguments2[0] = qualification;
                System.arraycopy(naci.arguments, 0, arguments2, 1, naci.arguments.length);
            }

            // Notice: The enclosing instance of the anonymous class is "this", not the qualification of the
            // NewAnonymousClassInstance.
            Scope s;
            for (s = naci.getEnclosingScope(); !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            ThisReference oei;
            if (UnitCompiler.isStaticContext((TypeBodyDeclaration) s)) {
                oei = null;
            } else
            {
                oei = new ThisReference(loc);
                oei.setEnclosingScope(naci.getEnclosingScope());
            }
            this.invokeConstructor(
                naci,                     // locatable
                naci.getEnclosingScope(), // scope
                oei,                      // enclosingInstance
                iClass,                   // targetClass
                arguments2                // arguments
            );
            this.getCodeContext().popUninitializedVariableOperand();
            this.getCodeContext().pushObjectOperand(iClass.getDescriptor());
        } finally {

            // Remove the synthetic constructor that was temporarily added. This is necessary because this NACI
            // expression (and all other expressions) are sometimes compiled more than once (see "fakeCompile()"), and
            // we'd end up with TWO synthetic constructors. See JANINO-143.
            acd.constructors.remove(acd.constructors.size() - 1);
        }
        return iClass;
    }
    private IType
    compileGet2(ParameterAccess pa) throws CompileException {
        LocalVariable lv = this.getLocalVariable(pa.formalParameter);
        this.bytecodeEmitter.load(pa, lv);
        return lv.type;
    }
    private IClass
    compileGet2(NewArray na) throws CompileException {
        if (!this.isReifiable(this.getType(na.type))) throw UnitCompiler.compileException(na, "Generic array creation is not reifiable");
        for (Rvalue dimExpr : na.dimExprs) {
            IType dimType = this.compileGetValue(dimExpr);
            if (dimType != IClass.INT && this.unaryNumericPromotion(
                na,     // locatable
                dimType // type
            ) != IClass.INT) this.compileError("Invalid array size expression type", na.getLocation());
        }

        if (this.getTargetVersion() >= 8) {
            Type annotated = na.type;
            for (int i = na.dimExprs.length + na.dims - 1; i >= 0; i--) annotated = new ArrayType(annotated,
                i < na.dimensionAnnotations.length ? na.dimensionAnnotations[i] : new Annotation[0]);
            this.annotationCompiler.compileCodeTypeAnnotations(annotated, new Annotation[0], (byte) 0x44,
                this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
        }
        return this.newArray(
            na,                   // locatable
            na.dimExprs.length,   // dimExprCount
            na.dims,              // dims
            this.getType(na.type) // componentType
        );
    }
    private boolean isReifiable(IType type) {
        if (type instanceof GenericArrayType) return this.isReifiable(((GenericArrayType) type).component);
        if (!(type instanceof ParameterizedIClass)) return type instanceof IClass;
        ParameterizedIClass parameterized = (ParameterizedIClass) type;
        if (parameterized.ownerType != null && !this.isReifiable(parameterized.ownerType)) return false;
        for (IType argument : parameterized.actualTypeArguments) {
            if (!(argument instanceof IWildcardType) || ((IWildcardType) argument).getLowerBound() != null
                || ((IWildcardType) argument).getUpperBound() != this.iClassLoader.TYPE_java_lang_Object) return false;
        }
        return true;
    }

    private IType
    compileGet2(NewInitializedArray nia) throws CompileException {

        IType at = this.getType2(nia);

        this.compileGetValue(nia.arrayInitializer, at, nia.arrayType);

        return at;
    }
    private final Map<ArrayInitializer, IType> initializerElementTargets = new java.util.IdentityHashMap<>();

    private void
    compileGetValue(ArrayInitializer ai, IType arrayType) throws CompileException {
        this.compileGetValue(ai, arrayType, null);
    }
    private void compileGetValue(ArrayInitializer ai, IType arrayType, @Nullable Type annotated) throws CompileException {

        if (!UnitCompiler.isArray(arrayType)) {
            this.compileError("Array initializer not allowed for non-array type \"" + arrayType.toString() + "\"");
        }

        IType componentType = UnitCompiler.getComponentType(arrayType);
        assert componentType != null;
        if (!this.isReifiable(componentType)) throw UnitCompiler.compileException(ai, "Generic array creation is not reifiable");

        this.consT(ai, Integer.valueOf(ai.values.length));
        if (annotated != null && this.getTargetVersion() >= 8) this.annotationCompiler.compileCodeTypeAnnotations(
            annotated, new Annotation[0], (byte) 0x44, this.getCodeContext(), this.getCodeContext().newOffset(), null, 0);
        this.newArray(
            ai,           // locatable
            1,            // dimExprCount
            0,            // dims
            componentType // componentType
        );

        for (int index = 0; index < ai.values.length; index++) {
            ArrayInitializerOrRvalue componentInitializer = ai.values[index];

            this.bytecodeEmitter.dup(componentInitializer);
            this.consT(ai, index);
            IType elementTarget = this.initializerElementTargets.get(ai);
            this.compile(componentInitializer, elementTarget == null ? componentType : elementTarget);
            this.bytecodeEmitter.arraystore(componentInitializer, componentType);
        }
    }

    private IClass
    compileGet2(Literal l) throws CompileException {
        return this.consT(l, this.constantEvaluator.getConstantValue(l));
    }
    private IClass
    compileGet2(SimpleConstant sl) throws CompileException {
        return this.consT(sl, sl.value);
    }

    /**
     * Convenience function that calls {@link #compileContext(Rvalue)} and {@link #compileGet(Rvalue)}.
     *
     * @return The type of the Rvalue
     */
    private IType
    compileGetValue(Rvalue rv) throws CompileException {
        Object cv = this.constantEvaluator.getConstantValue(rv);
        if (cv != UnitCompiler.NOT_CONSTANT) {
            this.fakeCompile(rv); // To check that, e.g., "a" compiles in "true || a".
            this.consT(rv, cv);
            return this.getType(rv);
        }

        try {
            this.compileContext(rv);
            IType result = this.compileGet(rv);
            Object origin = this.captureOrigins.get(rv);
            Object key = origin == null ? rv : origin;
            IType captured = this.invocationCaptures.get(key);
            return captured != null && result.equals(this.captureInputs.get(key)) ? captured : result;
        } catch (RuntimeException re) {
            throw new InternalCompilerException(rv.getLocation(), "Compiling \"" + rv + "\"", re);
        }
    }

    /** Consumes a reference without imposing its substituted type; receiver and argument adaptations remain intact. */
    private IType compileGetReferenceValue(Rvalue rv) throws CompileException {
        List<Rvalue> added = new ArrayList<>();
        this.markErasedReferenceResult(rv, added);
        try { return this.compileGetValue(rv); }
        finally { this.erasedReferenceResults.removeAll(added); }
    }

    private void markErasedReferenceResult(Rvalue value, List<Rvalue> added) throws CompileException {
        if (UnitCompiler.isPrimitive(this.getType(value))) return;
        if (value instanceof ParenthesizedExpression) {
            this.markErasedReferenceResult(((ParenthesizedExpression) value).value, added);
        } else if ((value instanceof Invocation || value instanceof Lvalue) && this.erasedReferenceResults.add(value)) added.add(value);
    }

    /** Pass only the result's consumer through name/field resolution, never through its receiver. */
    private IType compileResolvedReference(Rvalue original, Rvalue resolved) throws CompileException {
        IType target = this.referenceValueTargets.get(original);
        IType previous = target == null ? null : this.referenceValueTargets.put(resolved, target);
        boolean erased = this.erasedReferenceResults.contains(original) && this.erasedReferenceResults.add(resolved);
        boolean lambda = this.expressionLambdaResults.contains(original) && this.expressionLambdaResults.add(resolved);
        try { return this.compileGet(resolved); }
        finally {
            if (target != null) {
                if (previous == null) this.referenceValueTargets.remove(resolved);
                else this.referenceValueTargets.put(resolved, previous);
            }
            if (erased) this.erasedReferenceResults.remove(resolved);
            if (lambda) this.expressionLambdaResults.remove(resolved);
        }
    }

    /** Adapt an erased JVM value to the type required by this particular consumer. */
    private IType adaptReferenceResult(Rvalue expression, IType type, IClass erasedType, @Nullable IType target, boolean discard)
    throws CompileException {
        if (discard || this.erasedReferenceResults.contains(expression)) return erasedType;
        if (type != erasedType && !erasedType.isPrimitive()) {
            if (!this.expressionLambdaResults.contains(expression) && target instanceof IClass
                && !UnitCompiler.isPrimitive(target) && this.genericConvertible(type, target, false)) {
                if (UnitCompiler.rawTypeOf(target).isAssignableFrom(erasedType)) {
                    this.getCodeContext().popOperand(erasedType.getDescriptor());
                    this.getCodeContext().pushOperand(UnitCompiler.rawTypeOf(target).getDescriptor());
                } else this.bytecodeEmitter.checkcast(expression, target);
                return target;
            }
            this.bytecodeEmitter.checkcast(expression, type);
        }
        return type;
    }

    private IType compileStringOperand(Rvalue value) throws CompileException {
        IType type = this.getType(value);
        return UnitCompiler.isPrimitive(type) || UnitCompiler.rawTypeOf(type) == this.iClassLoader.TYPE_java_lang_String
            ? this.compileGetValue(value) : this.compileGetReferenceValue(value);
    }

    // -------------------- Rvalue.getConstantValue() -----------------

    /**
     * Special return value for the {@link #getConstantValue(Java.Rvalue)} method family indicating that the given
     * {@link Java.Rvalue} does not evaluate to a constant value.
     */
    public static final Object NOT_CONSTANT = IClass.NOT_CONSTANT;

    /**
     * Attempts to evaluate as a constant expression. The result is one of the following: {@link Boolean}, {@link
     * Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link Character}, {@link
     * String}, {@code null} (representing the {@code null} literal.
     * <p>
     *   This method cannot be STATIC, because the constant value may refer to a constant declaration in this
     *   compilation unit.
     * </p>
     *
     * @return {@link #NOT_CONSTANT} iff the rvalue is not a constant value
     */
    @Nullable public final Object
    getConstantValue(ArrayInitializerOrRvalue rv) throws CompileException {
        return this.constantEvaluator.getConstantValue(rv);
    }

    /**
     * Attempts to evaluate as a constant expression. The result is one of the following: {@link Boolean}, {@link
     * Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link Character}, {@link
     * String}, {@code null} (representing the {@code null} literal.
     * <p>
     *   This method cannot be STATIC, because the constant value may refer to a constant declaration in this
     *   compilation unit.
     * </p>
     *
     * @return {@link #NOT_CONSTANT} iff the rvalue is not a constant value
     */
    @Nullable public final Object
    getConstantValue(Rvalue rv) throws CompileException {
        return this.constantEvaluator.getConstantValue(rv);
    }

    // ---------------- Lvalue.compileSet() -----------------

    /**
     * Generates code that stores a value in the {@link Lvalue}. Expects the {@link Lvalue}'s context (see {@link
     * #compileContext}) and a value of the {@link Lvalue}'s type on the operand stack.
     */
    private void
    compileSet(Lvalue lv) throws CompileException {

        lv.accept(new LvalueVisitor<Void, CompileException>() {
            @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compileSet2(an);    return null; }
            @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compileSet2(aae);   return null; }
            @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compileSet2(fa);    return null; }
            @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compileSet2(fae);   return null; }
            @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compileSet2(scfae); return null; }
            @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                                                   { UnitCompiler.this.compileSet2(lva);   return null; }
            @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compileSet2(pe);    return null; }
        });
    }
    private void
    compileSet2(AmbiguousName an) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(this.reclassify(an)));
    }

    private void
    compileSet2(LocalVariableAccess lva) { this.bytecodeEmitter.store(lva, lva.localVariable); }

    private void
    compileSet2(FieldAccess fa) throws CompileException {
        this.bytecodeEmitter.putfield(fa, fa.field, this.fieldReferenceOwner(fa));
    }
    private void
    compileSet2(ArrayAccessExpression aae) throws CompileException {
        this.bytecodeEmitter.arraystore(aae, this.getType(aae));
    }
    private void
    compileSet2(FieldAccessExpression fae) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(this.determineValue(fae)));
    }
    private void
    compileSet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        this.compileSet(this.toLvalueOrCompileException(this.determineValue(scfae)));
    }
    private void
    compileSet2(ParenthesizedExpression pe) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(pe.value));
    }

    // ---------------- Atom.getType() ----------------

    /**
     * @return For a {@link Type}, the resolved {@link IClass}, for an {@link Rvalue}, the resolved {@link IClass} of
     *         the rvalue's type
     * @see #resolve(TypeDeclaration)
     */
    private IType
    getType(Atom a) throws CompileException {

        IType result = (IType) a.accept(new AtomVisitor<IType, CompileException>() {

            @Override public IType
            visitPackage(Package p) throws CompileException { return UnitCompiler.this.getType2(p); }

            @Override @Nullable public IType
            visitType(Type t) throws CompileException { return UnitCompiler.this.getType(t); }

            @Override @Nullable public IType
            visitRvalue(Rvalue rv) throws CompileException { return UnitCompiler.this.getType(rv); }

            @Override @Nullable public IType
            visitConstructorInvocation(ConstructorInvocation ci) throws CompileException {
                return UnitCompiler.this.getType2(ci);
            }
        });

        assert result != null;
        return result;
    }

    static IClass
    rawTypeOf(IType iType) {
        if (iType instanceof IWildcardType) return UnitCompiler.rawTypeOf(((IWildcardType) iType).getUpperBound());
        while (iType instanceof IParameterizedType) iType = ((IParameterizedType) iType).getRawType();
        assert iType instanceof IClass;
        return (IClass) iType;
    }

    private static IClass[]
    rawTypesOf(IType[] iTypes) {
        IClass[] result = new IClass[iTypes.length];
        for (int i = 0; i < result.length; i++) result[i] = UnitCompiler.rawTypeOf(iTypes[i]);
        return result;
    }

    private IClass
    getRawType(Type t) throws CompileException { return UnitCompiler.rawTypeOf(this.getType(t)); }

    // Resolve hierarchy erasures before validating bounds, which may themselves inspect that hierarchy.
    private IClass getHierarchyType(Type type) throws CompileException {
        if (type instanceof ReferenceType) {
            ReferenceType reference = (ReferenceType) type;
            return UnitCompiler.rawTypeOf(this.getReferenceType(reference.getLocation(), reference.getEnclosingScope(),
                reference.identifiers, reference.identifiers.length, null));
        }
        return this.getRawType(type);
    }

    private IType
    getType(Type t) throws CompileException {
        IType bound = this.referenceTypeBindings.get(t);
        if (bound != null) return bound;
        IType result = (IType) t.accept(new TypeVisitor<IType, CompileException>() {
            @Override public IType visitArrayType(ArrayType at)                throws CompileException { return UnitCompiler.this.getType2(at);  }
            @Override public IType visitPrimitiveType(PrimitiveType bt)                                { return UnitCompiler.this.getType2(bt);  }
            @Override public IType visitReferenceType(ReferenceType rt)        throws CompileException { return UnitCompiler.this.getType2(rt);  }
            @Override public IType visitRvalueMemberType(RvalueMemberType rmt) throws CompileException { return UnitCompiler.this.getType2(rmt); }
            @Override public IType visitSimpleType(SimpleType st)                                      { return UnitCompiler.this.getType2(st);  }
        });
        assert result != null;
        return result;
    }

    private IType[]
    getTypes(final Type[] types) throws CompileException {
        IType[] result = new IType[types.length];
        for (int i = 0; i < types.length; ++i) result[i] = this.getType(types[i]);
        return result;
    }

    private IType
    getType(Rvalue rv) throws CompileException {

        IType result = (IType) rv.accept(new RvalueVisitor<IType, CompileException>() {

            @Override @Nullable public IType
            visitLvalue(Lvalue lv) throws CompileException { return UnitCompiler.this.getType(lv); }

            @Override public IType visitArrayLength(ArrayLength al)                                                            { return UnitCompiler.this.getType2(al);   }
            @Override public IType visitAssignment(Assignment a)                                       throws CompileException { return UnitCompiler.this.getType2(a);    }
            @Override public IType visitUnaryOperation(UnaryOperation uo)                              throws CompileException { return UnitCompiler.this.getType2(uo);   }
            @Override public IType visitBinaryOperation(BinaryOperation bo)                            throws CompileException { return UnitCompiler.this.getType2(bo);   }
            @Override public IType visitCast(Cast c)                                                   throws CompileException { return UnitCompiler.this.getType2(c);    }
            @Override public IType visitClassLiteral(ClassLiteral cl)                                                          throws CompileException { return UnitCompiler.this.getType2(cl);   }
            @Override public IType visitConditionalExpression(ConditionalExpression ce)                throws CompileException { return UnitCompiler.this.getType2(ce);   }
            @Override public IType visitCrement(Crement c)                                             throws CompileException { return UnitCompiler.this.getType2(c);    }
            @Override public IType visitInstanceof(Instanceof io)                                                              { return UnitCompiler.this.getType2(io);   }
            @Override public IType visitMethodInvocation(MethodInvocation mi)                          throws CompileException { return UnitCompiler.this.getType2(mi);   }
            @Override public IType visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { return UnitCompiler.this.getType2(smi);  }
            @Override public IType visitIntegerLiteral(IntegerLiteral il)                                                      { return UnitCompiler.this.getType2(il);   }
            @Override public IType visitFloatingPointLiteral(FloatingPointLiteral fpl)                                         { return UnitCompiler.this.getType2(fpl);  }
            @Override public IType visitBooleanLiteral(BooleanLiteral bl)                                                      { return UnitCompiler.this.getType2(bl);   }
            @Override public IType visitCharacterLiteral(CharacterLiteral cl)                                                  { return UnitCompiler.this.getType2(cl);   }
            @Override public IType visitStringLiteral(StringLiteral sl)                                                        { return UnitCompiler.this.getType2(sl);   }
            @Override public IType visitTextBlock(TextBlock tb)                                                                { return UnitCompiler.this.getType2(tb);   }
            @Override public IType visitNullLiteral(NullLiteral nl)                                                            { return UnitCompiler.this.getType2(nl);   }
            @Override public IType visitSimpleConstant(SimpleConstant sl)                                                      { return UnitCompiler.this.getType2(sl);   }
            @Override public IType visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)                              { return UnitCompiler.this.getType2(naci); }
            @Override public IType visitNewArray(NewArray na)                                          throws CompileException { return UnitCompiler.this.getType2(na);   }
            @Override public IType visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { return UnitCompiler.this.getType2(nia);  }
            @Override public IType visitNewClassInstance(NewClassInstance nci)                         throws CompileException { return UnitCompiler.this.getType2(nci);  }
            @Override public IType visitParameterAccess(ParameterAccess pa)                            throws CompileException { return UnitCompiler.this.getType2(pa);   }
            @Override public IType visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { return UnitCompiler.this.getType2(qtr);  }
            @Override public IType visitThisReference(ThisReference tr)                                throws CompileException { return UnitCompiler.this.getType2(tr);   }
            @Override public IType visitLambdaExpression(LambdaExpression le)                          throws CompileException { return UnitCompiler.this.getType2(le);   }
            @Override public IType visitMethodReference(MethodReference mr)                            throws CompileException { return UnitCompiler.this.getType2(mr);   }
            @Override public IType visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { return UnitCompiler.this.getType2(cicr); }
            @Override public IType visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { return UnitCompiler.this.getType2(acr);  }
        });

        assert result != null;
        return result;
    }

    private IType
    getType(Lvalue lv) throws CompileException {
        IType result = (IType) lv.accept(new LvalueVisitor<IType, CompileException>() {
            @Override public IType visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.getType2(an);    }
            @Override public IType visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.getType2(aae);   }
            @Override public IType visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.getType2(fa);    }
            @Override public IType visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.getType2(fae);   }
            @Override public IType visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.getType2(scfae); }
            @Override public IType visitLocalVariableAccess(LocalVariableAccess lva)                                                   { return UnitCompiler.this.getType2(lva);   }
            @Override public IType visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.getType2(pe);    }
        });

        assert result != null;
        return result;
    }

    private IClass
    getType2(ConstructorInvocation ci) throws CompileException {
        this.compileError("Explicit constructor invocation not allowed here", ci.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    @SuppressWarnings("static-method") private IType
    getType2(SimpleType st) { return st.iType; }

    @SuppressWarnings("static-method") private IClass
    getType2(PrimitiveType bt) {
        switch (bt.primitive) {
        case VOID:    return IClass.VOID;
        case BYTE:    return IClass.BYTE;
        case SHORT:   return IClass.SHORT;
        case CHAR:    return IClass.CHAR;
        case INT:     return IClass.INT;
        case LONG:    return IClass.LONG;
        case FLOAT:   return IClass.FLOAT;
        case DOUBLE:  return IClass.DOUBLE;
        case BOOLEAN: return IClass.BOOLEAN;
        default:      throw new InternalCompilerException(bt.getLocation(), "Invalid primitive " + bt.primitive);
        }
    }
    /**
     * @return Whether any of the given type arguments is a bare, single identifier that looks like a TYPE VARIABLE
     *         (e.g. "T" in "implements Iterator<T>"); such types appear in "extends"/"implements" clauses of
     *         generic classes, where the type variable is not resolvable
     */
    /**
     * @return Whether any of the given type arguments is (or contains) a TYPE VARIABLE of the declaring class,
     *         e.g. "T" in "implements Iterator<T>"; such types are used in "extends"/"implements" clauses of
     *         generic classes, where the type variable is not resolvable. Notice that a pattern on the NAME is
     *         not sufficient (type variables may contain digits, e.g. "T2"), hence the "is not resolvable as a
     *         type" check.
     */
    private boolean
    containsTypeVariableArgument(TypeArgument[] typeArguments) {
        for (TypeArgument ta : typeArguments) {
            Type t;
            if (ta instanceof Wildcard) {

                // E.g. "Iterator<? extends T1>": The type variable is the wildcard BOUND.
                Wildcard w = (Wildcard) ta;
                if (w.referenceType == null) continue;  // "?"
                t = w.referenceType;
            } else
            {
                t = (Type) ta;
            }
            if (t instanceof ArrayType) t = ((ArrayType) t).componentType;
            if (
                t instanceof ReferenceType
                && ((ReferenceType) t).identifiers.length == 1
                && UnitCompiler.isTypeVariableOfEnclosingDeclaration((ReferenceType) t)
            ) return true;
        }
        return false;
    }

    /**
     * @return Whether the given (single-identifier) reference type is a TYPE VARIABLE declared by an ENCLOSING
     *         type or method (JLS8 6.3), e.g. "T2" in "class Foo<T1, T2> implements Iterator<T2>"
     */
    static boolean
    isTypeVariableOfEnclosingDeclaration(ReferenceType rt) {
        String id = rt.identifiers[0];

        for (Scope s = rt.getEnclosingScope(); !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof EnclosingScopeOfTypeDeclaration) {

                // E.g. the "T" of "implements Iterator<T>" of class "G<T>": The enclosing scope is a LAZY
                // wrapper AROUND "G"; check "G"'s type parameters, then continue OUTSIDE of it.
                TypeDeclaration td = ((EnclosingScopeOfTypeDeclaration) s).typeDeclaration;
                if (td instanceof NamedTypeDeclaration) {
                    TypeParameter[] tps = ((NamedTypeDeclaration) td).getOptionalTypeParameters();
                    if (tps != null) for (TypeParameter tp : tps) if (tp.name.equals(id)) return true;
                }
            }
            if (s instanceof NamedTypeDeclaration) {
                TypeParameter[] tps = ((NamedTypeDeclaration) s).getOptionalTypeParameters();
                if (tps != null) for (TypeParameter tp : tps) if (tp.name.equals(id)) return true;
            } else
            if (s instanceof FunctionDeclarator) {
                TypeParameter[] tps = ((FunctionDeclarator) s).getOptionalTypeParameters();
                if (tps != null) for (TypeParameter tp : tps) if (tp.name.equals(id)) return true;
            }
        }
        return false;
    }

    private static final class GenericArrayType implements IParameterizedType {
        final IType component;
        final IClass erasure;
        GenericArrayType(IType component, IClass erasure) { this.component = component; this.erasure = erasure; }
        @Override public IType getRawType() { return this.erasure; }
        @Override public IType[] getActualTypeArguments() { return new IType[0]; }
        @Override public String toString() { return this.component + "[]"; }
        @Override public boolean equals(@Nullable Object other) { return other instanceof GenericArrayType && this.component.equals(((GenericArrayType) other).component); }
        @Override public int hashCode() { return this.component.hashCode(); }
    }
    private IType arrayType(IType component) {
        IClass erasure = this.iClassLoader.getArrayIClass(UnitCompiler.rawTypeOf(component));
        return component instanceof IParameterizedType ? new GenericArrayType(component, erasure) : erasure;
    }

    /** A source variable has declaration identity; erasure is used only by JVM instructions and descriptors. */
    private static final class SourceTypeVariable implements IParameterizedType {
        final TypeParameter declaration;
        IClass erasure;
        SourceTypeVariable(TypeParameter declaration, IClass erasure) { this.declaration = declaration; this.erasure = erasure; }
        @Override public IType getRawType() { return this.erasure; }
        @Override public IType[] getActualTypeArguments() { return new IType[0]; }
        @Override public String toString() { return this.declaration.name; }
    }
    private final Map<TypeParameter, SourceTypeVariable> sourceVariables = new java.util.IdentityHashMap<>();

    private SourceTypeVariable sourceVariable(TypeParameter parameter, Scope scope) throws CompileException {
        SourceTypeVariable result = this.sourceVariables.get(parameter);
        if (result != null) return result;
        result = new SourceTypeVariable(parameter, this.iClassLoader.TYPE_java_lang_Object);
        this.sourceVariables.put(parameter, result);
        IClass erasure = this.iClassLoader.TYPE_java_lang_Object;
        if (parameter.bound != null && parameter.bound.length > 0) {
            ReferenceType first = parameter.bound[0];
            UnitCompiler.setScopeIfUnset(first, scope);
            erasure = UnitCompiler.rawTypeOf(this.getReferenceType(first.getLocation(), first.getEnclosingScope(), first.identifiers, first.identifiers.length, null));
        }
        result.erasure = erasure;
        return result;
    }

    @Nullable private SourceTypeVariable lexicalVariable(ReferenceType reference) throws CompileException {
        if (reference.identifiers.length != 1) return null;
        String name = reference.identifiers[0];
        boolean staticContext = false;
        for (Scope scope = reference.getEnclosingScope(); !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            TypeParameter[] parameters = null;
            Scope declaration = scope;
            if (scope instanceof EnclosingScopeOfTypeDeclaration) declaration = ((EnclosingScopeOfTypeDeclaration) scope).typeDeclaration;
            if (declaration instanceof FunctionDeclarator) parameters = ((FunctionDeclarator) declaration).getOptionalTypeParameters();
            else if (declaration instanceof NamedTypeDeclaration) parameters = ((NamedTypeDeclaration) declaration).getOptionalTypeParameters();
            if (parameters != null) for (TypeParameter parameter : parameters) if (parameter.name.equals(name)) {
                if (declaration instanceof NamedTypeDeclaration && staticContext) throw UnitCompiler.compileException(reference, "Class type variable cannot be used in static context");
                if (reference.typeArguments != null) throw UnitCompiler.compileException(reference, "Type variable cannot have type arguments");
                return this.sourceVariable(parameter, declaration);
            }
            if (declaration instanceof MethodDeclarator && ((MethodDeclarator) declaration).isStatic()
                || declaration instanceof FieldDeclaration && (((FieldDeclaration) declaration).isStatic()
                    || ((FieldDeclaration) declaration).getDeclaringType() instanceof InterfaceDeclaration)
                || declaration instanceof InterfaceDeclaration
                || declaration instanceof MemberClassDeclaration && ((MemberClassDeclaration) declaration).isStatic()) staticContext = true;
        }
        return null;
    }

    private IType lexicalReceiver(IClass raw, Scope site) throws CompileException {
        IClass outer = raw.getOuterIClass();
        IType owner = outer == null ? null : this.lexicalReceiver(outer, site);
        for (Scope scope = site; !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            if (!(scope instanceof NamedTypeDeclaration) || this.resolve((TypeDeclaration) scope) != raw) continue;
            TypeParameter[] parameters = ((NamedTypeDeclaration) scope).getOptionalTypeParameters();
            IType[] arguments = new IType[parameters == null ? 0 : parameters.length];
            for (int i = 0; i < arguments.length; i++) arguments[i] = this.sourceVariable(parameters[i], scope);
            return arguments.length == 0 && !(owner instanceof ParameterizedIClass) ? raw : new ParameterizedIClass(raw, arguments, owner);
        }
        return owner instanceof ParameterizedIClass ? new ParameterizedIClass(raw, new IType[0], owner) : raw;
    }

    /** An inherited unqualified member type carries the enclosing subclass's parent view. */
    private IType lexicalMemberOwner(IClass owner, Scope site) throws CompileException {
        for (Scope scope = site; !(scope instanceof CompilationUnit); scope = scope.getEnclosingScope()) {
            if (!(scope instanceof TypeDeclaration)) continue;
            IClass lexical = this.resolve((TypeDeclaration) scope);
            if (!owner.isAssignableFrom(lexical)) continue;
            IType view = this.asSupertype(this.lexicalReceiver(lexical, site), owner);
            if (view != null) return view;
        }
        return this.lexicalReceiver(owner, site);
    }

    private IType getType2(ReferenceType type) throws CompileException {
        SourceTypeVariable variable = this.lexicalVariable(type);
        return variable == null ? this.referenceType(type, type.identifiers.length) : variable;
    }

    private IType referenceType(ReferenceType type, int segments) throws CompileException {
        IType resolved = this.getReferenceType(type.getLocation(), type.getEnclosingScope(), type.identifiers, segments, null);
        if (resolved == null) throw UnitCompiler.compileException(type, "Reference type not found");
        IClass raw = UnitCompiler.rawTypeOf(resolved);
        IClass outer = raw.getOuterIClass();
        IType owner = null;
        if (outer != null) {
            owner = segments > 1 ? this.referenceType(type, segments - 1) : this.lexicalMemberOwner(outer, type.getEnclosingScope());
            owner = this.asSupertype(owner, outer);
            if (owner == null) throw UnitCompiler.compileException(type, "Invalid enclosing member type");
        } else if (segments > 1) {
            for (int i = 0; i < segments - 1; i++) if (type.segmentTypeArguments[i] != null) {
                throw UnitCompiler.compileException(type, "Cannot select a static member type from a parameterized owner");
            }
        }
        return this.parameterizedType(raw, owner, type.segmentTypeArguments[segments - 1], type);
    }

    private IType parameterizedType(IClass raw, @Nullable IType owner, @Nullable TypeArgument[] typeArguments, Locatable location)
    throws CompileException {
        ITypeVariable[] variables = raw.getITypeVariables();
        if (owner != null && UnitCompiler.rawTypeOf(owner).getITypeVariables().length > 0 && !(owner instanceof ParameterizedIClass)
            && typeArguments != null) throw UnitCompiler.compileException(location, "Cannot parameterize a member of a raw enclosing type");
        if (owner instanceof ParameterizedIClass && variables.length > 0 && typeArguments == null) {
            throw UnitCompiler.compileException(location, "Member of a parameterized owner requires its type arguments");
        }
        if (typeArguments == null || typeArguments.length == 0) {
            if (typeArguments != null && variables.length == 0) throw UnitCompiler.compileException(location, "Diamond requires a generic class");
            return owner instanceof ParameterizedIClass ? new ParameterizedIClass(raw, new IType[0], owner) : raw;
        }
        if (variables.length != typeArguments.length) throw UnitCompiler.compileException(location, "Wrong number of type arguments");
        IType[] arguments = new IType[typeArguments.length];
        for (int i = 0; i < arguments.length; i++) arguments[i] = typeArguments[i] instanceof Type
            ? this.getType((Type) typeArguments[i]) : this.typeArgumentToIType(typeArguments[i]);
        Map<String, IType> environment = new HashMap<>();
        this.bindTypeVariables(owner, environment);
        for (int i = 0; i < arguments.length; i++) environment.put(variables[i].getName(), arguments[i]);
        Map<String, GenericType[]> bounds = raw.getTypeParameterBounds();
        for (int i = 0; i < arguments.length; i++) {
            if (UnitCompiler.isPrimitive(arguments[i])) throw UnitCompiler.compileException(location, "Primitive type argument");
            GenericType[] declared = bounds.get(variables[i].getName());
            if (declared == null) continue;
            for (GenericType bound : declared) {
                IType expected = this.substituteGenericType(bound, environment);
                if (expected == null) continue;
                if (arguments[i] instanceof IWildcardType) {
                    IWildcardType wildcard = (IWildcardType) arguments[i];
                    IType lower = wildcard.getLowerBound();
                    if (lower != null && !this.genericConvertible(lower, expected, false)) throw UnitCompiler.compileException(location, "Wildcard lower bound violates " + expected);
                    IClass upper = UnitCompiler.rawTypeOf(wildcard.getUpperBound()), limit = UnitCompiler.rawTypeOf(expected);
                    boolean related = upper.isAssignableFrom(limit) || limit.isAssignableFrom(upper);
                    if (!related && (!upper.isInterface() && !limit.isInterface() || upper.isFinal() || limit.isFinal())) {
                        throw UnitCompiler.compileException(location, "Incompatible wildcard upper bounds");
                    }
                } else if (!this.genericConvertible(arguments[i], expected, false)) {
                    throw UnitCompiler.compileException(location, "Type argument violates bound " + expected);
                }
            }
        }
        return new ParameterizedIClass(raw, arguments, owner);
    }

    /**
     * @param typeArguments Zero-length array if type has no type variables
     * @return              The resolved {@link IClass}, or {@link IParameterizedType}, or {@code null}
     */
    @Nullable private IType
    getReferenceType(
        Location                 location,
        Scope                    scope,
        String[]                 identifiers,
        int                      n,
        @Nullable TypeArgument[] typeArguments
    ) throws CompileException {

        if (n == 1) {
            return this.getReferenceType(location, identifiers[0], typeArguments, scope);
        }

        // JLS7 6.5.5.1   Unnamed package member type name (one identifier).
        // JLS7 6.5.5.2.1 Qualified type name (two or more identifiers).
        {
            String className = Java.join(identifiers, ".", 0, n);
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return result;
        }

        // JLS7 6.5.5.2.2 referenceType '.' memberTypeName
        if (n >= 2) {
            IType enclosingType = this.getReferenceType(location, scope, identifiers, n - 1, new TypeArgument[0]);
            if (enclosingType != null) {
                String memberTypeName = identifiers[n - 1];
                IClass memberType     = this.findMemberType(enclosingType, memberTypeName, typeArguments, location);
                if (memberType == null) {
                    this.compileError(
                        "\"" + enclosingType + "\" declares no member type \"" + memberTypeName + "\"",
                        location
                    );
                    return this.iClassLoader.TYPE_java_lang_Object;
                }
                return memberType;
            }
        }

        return null;
    }

    /**
     * JLS7 6.5.5.1 Simple type name (single identifier)
     *
     * @param typeArguments Zero-length array if type is not parameterized
     * @return              The resolved {@link IClass} or {@link IParameterizedType}
     */
    private IType
    getReferenceType(
        Location                 location,
        String                   simpleTypeName,
        @Nullable TypeArgument[] typeArguments,
        Scope                    scope
    ) throws CompileException {

        if ("var".equals(simpleTypeName)) {
            this.compileError("Local variable type inference NYI", location);
            return this.iClassLoader.TYPE_java_lang_Object;
        }

        // Method declaration type parameter?
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (!(s instanceof FunctionDeclarator)) continue;
            FunctionDeclarator md = (FunctionDeclarator) s;

            TypeParameter[] typeParameters = md.getOptionalTypeParameters();
            if (typeParameters != null) {
                for (TypeParameter tp : typeParameters) {
                    if (tp.name.equals(simpleTypeName)) {
                        ReferenceType[] ob = tp.bound;
                        if (ob == null || ob.length == 0) return this.iClassLoader.TYPE_java_lang_Object;
                        // Erasure only needs the leftmost bound, without its type arguments.
                        // Resolving all bounds eagerly recurses for e.g. T extends Comparable<T>.
                        ReferenceType firstBound = ob[0];
                        return this.getReferenceType(firstBound.getLocation(), firstBound.getEnclosingScope(),
                            firstBound.identifiers, firstBound.identifiers.length, null);
                    }
                }
            }
        }

        // Type declaration type parameter?
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (!(s instanceof NamedTypeDeclaration)) continue;
            NamedTypeDeclaration ntd = (NamedTypeDeclaration) s;

            TypeParameter[] typeParameters = ntd.getOptionalTypeParameters();
            if (typeParameters != null) {
                for (TypeParameter tp : typeParameters) {
                    if (tp.name.equals(simpleTypeName)) {
                        ReferenceType[] ob = tp.bound;
                        if (ob == null || ob.length == 0) return this.iClassLoader.TYPE_java_lang_Object;
                        ReferenceType firstBound = ob[0];
                        return this.getReferenceType(firstBound.getLocation(), firstBound.getEnclosingScope(),
                            firstBound.identifiers, firstBound.identifiers.length, null);
                    }
                }
            }
        }

//        // Determine type arguments.
//        IType[] tas;
//        {
//            if (typeArguments == null) {
//                tas = new IType[0];
//            } else {
//                tas = new IType[typeArguments.length];
//                for (int i = 0; i < tas.length; i++) {
//                    final TypeArgument ta = typeArguments[i];
//                    tas[i] = this.typeArgumentToIType(ta);
//                }
//            }
//        }

        try {
            return this.getRawReferenceType(location, simpleTypeName, scope)/*.parameterize(tas)*/;
        } catch (CompileException ce) {
            throw new CompileException(ce.getMessage(), location, ce);
        }
    }

    @SuppressWarnings("unused") private IType
    typeArgumentToIType(TypeArgument ta) throws CompileException {

        if (ta instanceof ReferenceType) {
            return this.getType((ReferenceType) ta);
        } else
        if (ta instanceof Wildcard) {
            Wildcard w = (Wildcard) ta;
            final IType ub = w.bounds == Wildcard.BOUNDS_EXTENDS ? this.getType(w.referenceType) : UnitCompiler.this.iClassLoader.TYPE_java_lang_Object;
            final IType lb = w.bounds == Wildcard.BOUNDS_SUPER   ? this.getType(w.referenceType) : null;
            return this.wildcard(lb == null, lb == null ? ub : lb);
        } else
        if (ta instanceof ArrayType) {
            // TODO
            return UnitCompiler.this.iClassLoader.TYPE_java_lang_Object;
        } else
        {
            throw new AssertionError(ta.getClass() + ": " + ta);
        }
    }

    private IClass
    getRawReferenceType(Location location, String simpleTypeName, Scope scope) throws CompileException {

        // 6.5.5.1.1 Local class.
        {
            LocalClassDeclaration lcd = UnitCompiler.findLocalClassDeclaration(
                scope,
                simpleTypeName
            );
            if (lcd != null) return this.resolve(lcd);
        }

        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {

            // This type, or an enclosing type.
            if (s instanceof NamedTypeDeclaration) {
                NamedTypeDeclaration ntd = (NamedTypeDeclaration) s;
                if (ntd.getName().equals(simpleTypeName)) {
                    return this.resolve((NamedTypeDeclaration) s);
                }
            }

            // 6.5.5.1.2 Member type (of this type or an enclosing type).
            if (s instanceof TypeDeclaration) {
                IClass mt = this.findMemberType(
                    this.resolve((AbstractTypeDeclaration) s),
                    simpleTypeName,
                    null,                                      // typeArguments
                    location
                );
                if (mt != null) return mt;
            }
        }

        // 6.5.5.1.4a Single-type import.
        {
            IClass importedClass = this.importSingleType(simpleTypeName, location);
            if (importedClass != null) return importedClass;
        }

        CompilationUnit  scopeCompilationUnit;
        for (Scope s = scope;; s = s.getEnclosingScope()) {
            if (s instanceof CompilationUnit) {
                scopeCompilationUnit = (CompilationUnit) s;
                break;
            }
        }

        // 6.5.5.1.4b Type declared in same compilation unit.
        {
            PackageMemberTypeDeclaration pmtd = (
                scopeCompilationUnit.getPackageMemberTypeDeclaration(simpleTypeName)
            );
            if (pmtd != null) return this.resolve(pmtd);
        }

        // 6.5.5.1.5 Type declared in other compilation unit of same package.
        {
            PackageDeclaration opd = scopeCompilationUnit.packageDeclaration;

            String pkg       = opd == null ? null : opd.packageName;
            String className = pkg == null ? simpleTypeName : pkg + "." + simpleTypeName;
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return result;
        }

        // 6.5.5.1.6 Type-import-on-demand declaration.
        {
            IClass importedClass = this.importTypeOnDemand(simpleTypeName, location);
            if (importedClass != null) return importedClass;
        }

        // JLS7 6.5.2.BL1.B2: Type imported through single import.
        {
            IClass importedIClass = this.importSingleType(simpleTypeName, location);
            if (importedIClass != null) {
                if (!this.isAccessible(importedIClass, scope)) {
                    this.compileError("Member type \"" + simpleTypeName + "\" is not accessible", location);
                }
                return importedIClass;
            }
        }

        // JLS7 6.5.2.BL1.B2: Member type imported through single static import.
        {
            IClass importedMemberType = null;
            for (IClass mt : Iterables.filterByClass(this.importSingleStatic(simpleTypeName), IClass.class)) {

                if (importedMemberType != null && mt != importedMemberType) {
                    this.compileError(
                        "Ambiguous static member type import: \""
                        + importedMemberType.toString()
                        + "\" vs. \""
                        + mt
                        + "\""
                    );
                }
                importedMemberType = mt;
            }
            if (importedMemberType != null) return importedMemberType;
        }

        // JLS7 6.5.2.BL1.B2: Member type imported through static-import-on-demand.
        {
            Iterator<IClass>
            it = Iterables.filterByClass(this.importStaticOnDemand(simpleTypeName).iterator(), IClass.class);
            if (it.hasNext()) return (IClass) it.next();
        }

        // Unnamed package member type.
        {
            IClass result = this.findTypeByName(location, simpleTypeName);
            if (result != null) return result;
        }

        // Type argument of the enclosing anonymous class declaration?
        for (
            Scope s = scope;
            !(s instanceof CompilationUnit);
            s = s.getEnclosingScope()
        ) {
            if (!(s instanceof AnonymousClassDeclaration)) continue;
            AnonymousClassDeclaration acd = (AnonymousClassDeclaration) s;

            Type bt = acd.baseType;
            if (bt instanceof ReferenceType) {
                TypeArgument[] otas = ((ReferenceType) bt).typeArguments;
                if (otas != null) {
                    for (TypeArgument ta : otas) {
                        if (ta instanceof ReferenceType) {
                            String[] is = ((ReferenceType) ta).identifiers;
                            if (is.length == 1 && is[0].equals(simpleTypeName)) {
                                return this.iClassLoader.TYPE_java_lang_Object;
                            }
                        }
                    }
                }
            }
        }

        // 6.5.5.1.8 Give up.
        this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"", location);
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    /**
     * Imports a member class or interface, static field or static method via the compilation unit's static import
     * on-demand declarations.
     *
     * @return A list of {@link IField}s, {@link IMethod}s and/or {@link IClass}es with that <var>simpleName</var>;
     *         may be empty
     */
    private List<Object>
    importStaticOnDemand(String simpleName) throws CompileException {

        List<Object> result = new ArrayList<>();
        for (StaticImportOnDemandDeclaration siodd : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            StaticImportOnDemandDeclaration.class
        )) {

            IClass iClass = this.findTypeByFullyQualifiedName(siodd.getLocation(), siodd.identifiers);
            if (iClass == null) {
                this.compileError("Could not load \"" + Java.join(siodd.identifiers, ".") + "\"", siodd.getLocation());
                continue;
            }

            this.importStatic(iClass, simpleName, result, siodd.getLocation());
        }

        return result;
    }

    private IType
    getType2(RvalueMemberType rvmt) throws CompileException {
        IType rvt         = this.getType(rvmt.rvalue);
        IClass memberType = this.findMemberType(rvt, rvmt.identifier, null, rvmt.getLocation());
        if (memberType == null) {
            this.compileError("\"" + rvt + "\" has no member type \"" + rvmt.identifier + "\"", rvmt.getLocation());
            return this.iClassLoader.TYPE_java_lang_Object;
        }
        IClass outer = memberType.getOuterIClass();
        if (outer == null) throw UnitCompiler.compileException(rvmt, "Qualified creation requires a non-static member class");
        IType owner = this.asSupertype(rvt, outer);
        if (owner == null) throw UnitCompiler.compileException(rvmt, "Invalid enclosing instance");
        return this.parameterizedType(memberType, owner, rvmt.typeArguments, rvmt);
    }

    private IType
    getType2(ArrayType at) throws CompileException {
        return this.arrayType(this.getType(at.componentType));
    }

    private IType
    getType2(AmbiguousName an) throws CompileException {
        return this.getType(this.reclassify(an));
    }

    private IClass
    getType2(Package p) throws CompileException {
        this.compileError("Unknown variable or type \"" + p.name + "\"", p.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    @SuppressWarnings("static-method") private IType
    getType2(LocalVariableAccess lva) {
        return lva.localVariable.type;
    }

    @SuppressWarnings("static-method") private IType
    getType2(FieldAccess fa) throws CompileException {

        // Captures retain their source type for member substitution, although their JVM field
        // descriptor (and the synthetic constructor parameter) must remain erased.
        if (fa.field instanceof SimpleIField) return ((SimpleIField) fa.field).compileTimeType;

        // Substitute type variables with the actual type arguments of a PARAMETERIZED receiver (JLS8 4.5.2), e.g.
        // the type of "iter.next" is "String" if "iter" is an "Iterator<String>".
        IType receiver = fa.lhs == null ? null : this.captureInvocationReceiver(fa, this.getType(fa.lhs));
        if (receiver != null) {
            IType tt = this.asSupertype(receiver, fa.field.getDeclaringIClass());
            if (tt != null && !(tt instanceof ParameterizedIClass) && !fa.field.isStatic()
                && fa.field.getDeclaringIClass().getITypeVariables().length != 0) {
                // An instance field selected through a raw declaring type has its erased type,
                // even when the source IField retains its declaration's parameterization.
                return UnitCompiler.rawTypeOf(fa.field.getType());
            }
            if (
                tt instanceof ParameterizedIClass
                && fa.field.getDeclaringIClass() == ((ParameterizedIClass) tt).rawType
            ) {
                GenericType gft = this.getGenericFieldType(fa.field);
                if (gft != null) {
                    IType substituted = this.substituteTypeVariables(
                        gft,
                        fa.field.getDeclaringIClass(),
                        (ParameterizedIClass) tt,
                        null  // The field type contains no METHOD type parameters.
                    );
                    if (substituted != null) return substituted;
                }
            }
        }

        GenericType genericField = this.getGenericFieldType(fa.field);
        if (genericField != null) {
            IType concrete = this.substituteGenericType(genericField, this.receiverEnvironment(receiver, fa.field.getDeclaringIClass()));
            if (concrete != null) return concrete;
        }
        return fa.field.getType();
    }

    /**
     * @return The declared (generic) return type of the given method, or {@code null} if it is not generic
     */
    @Nullable private GenericType
    getGenericReturnType(IClass.IMethod im) throws CompileException {
        GenericType metadata = im.getGenericReturnType();
        if (metadata != null) return metadata;
        MethodDeclarator source = this.sourceMethods.get(im);
        if (source != null) return this.sourceGenericType(source.type);
        IClass dc = im.getDeclaringIClass();
        if (dc instanceof ClassFileIClass) return ((ClassFileIClass) dc).getGenericReturnType(im);
        if (dc instanceof ReflectionIClass) return ((ReflectionIClass) dc).getGenericReturnType(im);
        return null;
    }

    /**
     * @return The declared (generic) type of the given field, or {@code null} if it is not generic
     */
    @Nullable private GenericType
    getGenericFieldType(IClass.IField ifld) throws CompileException {
        GenericType source = ifld.getGenericType();
        if (source != null) return source;
        IClass dc = ifld.getDeclaringIClass();
        if (dc instanceof ClassFileIClass) return ((ClassFileIClass) dc).getGenericFieldType(ifld);
        if (dc instanceof ReflectionIClass) return ((ReflectionIClass) dc).getGenericFieldType(ifld);
        return null;
    }

    /**
     * Replaces the CLASS type variables in the given generic type with the actual type arguments of the given
     * parameterized type; restricted to the case where the parameterized type's raw type IS the declaring class.
     *
     * @return {@code null} if the substitution is impossible (e.g. the generic type contains METHOD type
     *         variables); the caller then falls back to the erased type
     */
    @Nullable private IType
    substituteTypeVariables(GenericType gt, IClass declaringClass, ParameterizedIClass parameterizedType, @Nullable IMethod currentMethod)
    throws CompileException {

        Map<String, IType> env = this.receiverEnvironment(parameterizedType, declaringClass);
        // The CLASS type variables of the receiver do not apply to a METHOD type parameter of the same name
        // (JLS8 6.3/8.4.4: the method's declaration shadows the class's).
        {
            Set<String> mtpn = this.getMethodTypeParameterNames(currentMethod);
            if (mtpn != null) env.keySet().removeAll(mtpn);
        }

        return this.substituteGenericType(gt, env);
    }

    /**
     * @return The names of the type parameters declared by the given method, or {@code null}
     */
    @Nullable private Set<String>
    getMethodTypeParameterNames(@Nullable IInvocable im) {
        if (im == null) return null;
        String[] metadata = im.getTypeParameterNames();
        if (metadata != null) return new LinkedHashSet<>(Arrays.asList(metadata));
        if (!(im instanceof IMethod)) return Collections.emptySet();
        MethodDeclarator source = this.sourceMethods.get(im);
        if (source != null) {
            Set<String> names = new LinkedHashSet<>();
            TypeParameter[] parameters = source.getOptionalTypeParameters();
            if (parameters != null) for (TypeParameter parameter : parameters) names.add(parameter.name);
            return names;
        }
        String[] ns;
        IClass   dc = im.getDeclaringIClass();
        if (dc instanceof ClassFileIClass) {
            ns = ((ClassFileIClass) dc).getMethodTypeParameterNames((IMethod) im);
        } else
        if (dc instanceof ReflectionIClass) {
            ns = ((ReflectionIClass) dc).getMethodTypeParameterNames((IMethod) im);
        } else
        {
            return null;
        }
        if (ns == null) return null;
        Set<String> result = new LinkedHashSet<>();
        for (String n : ns) result.add(n);
        return result;
    }

    /**
     * @return {@code null} if the type contains type variables that are not in <var>env</var> (e.g. METHOD type
     *         variables)
     */
    @Nullable private IType
    substituteGenericType(GenericType gt, Map<String, IType> env) throws CompileException {

        if (gt instanceof GenericType.Raw) {
            return ((GenericType.Raw) gt).iClass;
        }

        if (gt instanceof GenericType.TypeVariable) {
            return env.get(((GenericType.TypeVariable) gt).name);
        }

        if (gt instanceof GenericType.ArrayOf) {
            IType ct = this.substituteGenericType(((GenericType.ArrayOf) gt).componentType, env);
            return ct == null ? null : this.arrayType(ct);
        }

        if (gt instanceof GenericType.Wildcard) {
            GenericType.Wildcard wildcard = (GenericType.Wildcard) gt;
            // Reading a lower-bounded wildcard yields Object. Only functional target grounding
            // uses its lower bound, in substituteFunctionalTarget().
            return wildcard.upperBound ? this.substituteGenericType(wildcard.bound, env) : this.iClassLoader.TYPE_java_lang_Object;
        }

        if (gt instanceof GenericType.Parameterized) {
            GenericType.Parameterized gpt = (GenericType.Parameterized) gt;
            IType[] args = new IType[gpt.typeArguments.length];
            for (int i = 0; i < args.length; i++) {
                IType a = this.substituteTypeArgument(gpt.typeArguments[i], env);
                if (a == null) return null;
                args[i] = a;
            }
            IType owner = gpt.ownerType == null ? null : this.substituteGenericType(gpt.ownerType, env);
            if (gpt.ownerType != null && owner == null) return null;
            return new ParameterizedIClass(gpt.rawType, args, owner);
        }

        return null;  // E.g. "GenericType.ERASED".
    }

    @Nullable private IType substituteTypeArgument(GenericType type, Map<String, IType> environment) throws CompileException {
        if (!(type instanceof GenericType.Wildcard)) return this.substituteGenericType(type, environment);
        GenericType.Wildcard wildcard = (GenericType.Wildcard) type;
        final IType bound = this.substituteGenericType(wildcard.bound, environment);
        if (bound == null) return null;
        return this.wildcard(wildcard.upperBound, bound);
    }

    private IType wildcard(boolean upper, IType bound) {
        return this.wildcard(upper ? bound : this.iClassLoader.TYPE_java_lang_Object, upper ? null : bound);
    }

    private IType wildcard(final IType upper, @Nullable final IType lower) {
        return new IWildcardType() {
            @Override public IType getUpperBound() { return upper; }
            @Override @Nullable public IType getLowerBound() { return lower; }
            @Override public String toString() { return lower == null ? "? extends " + upper : "? super " + lower; }
            @Override public boolean equals(@Nullable Object other) {
                if (this == other) return true;
                if (!(other instanceof IWildcardType)) return false;
                IWildcardType wildcard = (IWildcardType) other;
                IType otherLower = wildcard.getLowerBound();
                return (lower == null ? otherLower == null : lower.equals(otherLower)) && upper.equals(wildcard.getUpperBound());
            }
            @Override public int hashCode() { return upper.hashCode() * 31 + (lower == null ? 0 : lower.hashCode()); }
        };
    }

    /** Keep known arguments of a nested call's target even when an unrelated variable is still unsolved. */
    @Nullable private IType inferenceTarget(GenericType type, InferenceContext context) throws CompileException {
        Map<String, IType> environment = context.values;
        IType resolved = this.substituteGenericType(type, environment);
        if (!(type instanceof GenericType.Parameterized)) return resolved;
        GenericType.Parameterized parameterized = (GenericType.Parameterized) type;
        IType[] arguments = new IType[parameterized.typeArguments.length];
        for (int i = 0; i < arguments.length; i++) {
            GenericType argument = parameterized.typeArguments[i];
            Map<String, IType> targetEnvironment = environment;
            if (argument instanceof GenericType.Wildcard && ((GenericType.Wildcard) argument).upperBound) {
                // A sibling's lower bound is provisional: Stream<Sub> must not constrain the
                // next Stream<? extends T> argument to Sub before its own result is known.
                targetEnvironment = new HashMap<>(environment);
                for (String name : context.variables) if (!context.equals.containsKey(name)) {
                    List<IType> upper = context.uppers.get(name);
                    if (upper == null || upper.isEmpty()) targetEnvironment.remove(name);
                    else targetEnvironment.put(name, upper.get(0));
                }
            }
            arguments[i] = argument instanceof GenericType.Parameterized ? this.inferenceTarget(argument, context)
                : this.substituteTypeArgument(argument, targetEnvironment);
            if (arguments[i] != null && argument instanceof GenericType.TypeVariable) {
                String name = ((GenericType.TypeVariable) argument).name;
                if (context.variables.contains(name) && !context.equals.containsKey(name) && !context.lowers.containsKey(name)) {
                    arguments[i] = this.wildcard(true, arguments[i]);
                }
            }
            if (arguments[i] == null) {
                arguments[i] = this.wildcard(true, this.iClassLoader.TYPE_java_lang_Object);
            }
        }
        IType owner = parameterized.ownerType == null ? null : this.inferenceTarget(parameterized.ownerType, context);
        return new ParameterizedIClass(parameterized.rawType, arguments, owner);
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ArrayLength al) {
        return IClass.INT;
    }

    private IType
    getType2(ThisReference tr) throws CompileException {
        return this.lexicalReceiver(this.getIClass(tr), tr.getEnclosingScope());
    }

    @SuppressWarnings("static-method") private IClass
    getType2(LambdaExpression le) throws CompileException {
        // The type of a LAMBDA expression is its target type (if it is already known).
        if (le.targetIType != null) return UnitCompiler.rawTypeOf(le.targetIType);
        throw UnitCompiler.compileException(le, "Cannot determine the target type of this LAMBDA expression");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(MethodReference mr) throws CompileException {
        if (mr.targetIType != null) return UnitCompiler.rawTypeOf(mr.targetIType);
        throw UnitCompiler.compileException(mr, "Cannot determine the target type of this method reference");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ClassInstanceCreationReference cicr) throws CompileException {
        if (cicr.targetIType != null) return UnitCompiler.rawTypeOf(cicr.targetIType);
        throw UnitCompiler.compileException(cicr, "Cannot determine the target type of this constructor reference");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ArrayCreationReference acr) throws CompileException {
        if (acr.targetIType != null) return UnitCompiler.rawTypeOf(acr.targetIType);
        throw UnitCompiler.compileException(acr, "Cannot determine the target type of this array creation reference");
    }

    private IType
    getType2(QualifiedThisReference qtr) throws CompileException {
        IType type = this.getTargetIType(qtr);
        return type instanceof IClass ? this.lexicalReceiver((IClass) type, qtr.getEnclosingScope()) : type;
    }

    private IClass
    getType2(ClassLiteral cl) throws CompileException {
        IClass type = this.getRawType(cl.type);
        type = type == IClass.VOID ? this.iClassLoader.TYPE_java_lang_Void : this.wrapperOf(type);
        return new ParameterizedIClass(this.iClassLoader.TYPE_java_lang_Class, new IType[] { type });
    }

    private IType
    getType2(Assignment a) throws CompileException { return this.getType(a.lhs); }

    private IType
    getType2(ConditionalExpression ce) throws CompileException {
        IType mhsType = this.getType(ce.mhs);
        IType rhsType = this.getType(ce.rhs);
        if (mhsType == IClass.VOID || rhsType == IClass.VOID) throw UnitCompiler.compileException(ce, "Conditional operand cannot be void");
        IType target = this.conditionalTargets.get(ce);
        if (target != null && !UnitCompiler.isPrimitive(target)
            && (mhsType == IClass.NULL || !UnitCompiler.isPrimitive(mhsType))
            && (rhsType == IClass.NULL || !UnitCompiler.isPrimitive(rhsType))
            && !(this.isConvertibleToPrimitiveNumeric(mhsType) && this.isConvertibleToPrimitiveNumeric(rhsType))
            && !(this.getUnboxedType(mhsType) == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN)) {
            if (!this.genericConvertible(this.invocationArgumentType(ce.mhs, target), target, true)
                || !this.genericConvertible(this.invocationArgumentType(ce.rhs, target), target, true)) {
                throw UnitCompiler.compileException(ce, "Conditional operand is incompatible with its target type");
            }
            return target;
        }

        if (mhsType == rhsType) {

            // JLS7 15.25, list 1, bullet 1: "b ? T : T => T"
            return mhsType;
        } else
        if (this.isUnboxingConvertible(mhsType) == rhsType) {

            // JLS7 15.25, list 1, bullet 2: "b ? Integer : int => int"
            return rhsType;
        } else
        if (this.isUnboxingConvertible(rhsType) == mhsType) {

            // JLS7 15.25, list 1, bullet 2: "b ? int : Integer => int"
            return mhsType;
        } else
        if (this.constantEvaluator.getConstantValue(ce.mhs) == null && !UnitCompiler.isPrimitive(rhsType)) {

            // JLS7 15.25, list 1, bullet 3: "b ? null : String => String"
            return rhsType;
        } else
        if (this.constantEvaluator.getConstantValue(ce.mhs) == null && this.isBoxingConvertible(rhsType) != null) {

            // Undocumented JAVAC feature: "b ? null : 7 => Integer"
            IClass result = this.isBoxingConvertible(rhsType);
            assert result != null;
            return result;
        } else
        if (!UnitCompiler.isPrimitive(mhsType) && this.constantEvaluator.getConstantValue(ce.rhs) == null) {

            // JLS7 15.25, list 1, bullet 3: "b ? String : null => String"
            return mhsType;
        } else
        if (this.isBoxingConvertible(mhsType) != null && this.constantEvaluator.getConstantValue(ce.rhs) == null) {

            // Undocumented JAVAC feature: "b ? 7 : null => Integer"
            IClass result = this.isBoxingConvertible(mhsType);
            assert result != null;
            return result;
        } else
        if (this.isConvertibleToPrimitiveNumeric(mhsType) && this.isConvertibleToPrimitiveNumeric(rhsType)) {

            // JLS7 15.25, list 1, bullet 4, bullet 1: "b ? Byte : Short => short"
            if (
                (mhsType == IClass.BYTE || mhsType == this.iClassLoader.TYPE_java_lang_Byte)
                && (rhsType == IClass.SHORT || rhsType == this.iClassLoader.TYPE_java_lang_Short)
            ) return IClass.SHORT;
            if (
                (rhsType == IClass.BYTE || rhsType == this.iClassLoader.TYPE_java_lang_Byte)
                && (mhsType == IClass.SHORT || mhsType == this.iClassLoader.TYPE_java_lang_Short)
            ) return IClass.SHORT;

            // JLS7 15.25, list 1, bullet 4, bullet 2: "b ? (byte) 1 : byte => byte"
            Object rhscv = this.constantEvaluator.getConstantValue(ce.rhs);
            if (
                (mhsType == IClass.BYTE || mhsType == IClass.SHORT || mhsType == IClass.CHAR)
                && rhscv != null
                && rhscv != UnitCompiler.NOT_CONSTANT
                && this.constantAssignmentConversion(ce.rhs, rhscv, mhsType) != null
            ) return mhsType;
            Object mhscv = this.constantEvaluator.getConstantValue(ce.mhs);
            if (
                (rhsType == IClass.BYTE || rhsType == IClass.SHORT || rhsType == IClass.CHAR)
                && mhscv != null
                && mhscv != UnitCompiler.NOT_CONSTANT
                && this.constantAssignmentConversion(ce.mhs, mhscv, rhsType) != null
            ) return rhsType;

            // JLS7 15.25, list 1, bullet 4, bullet 3: "b ? 127 : byte => byte"
            if (
                mhsType == IClass.INT
                && rhsType == IClass.BYTE
                && UnitCompiler.isByteConstant(mhscv) != null
            ) {

                // Fix up the constant to be a byte
                ce.mhs.constantValue = UnitCompiler.isByteConstant(mhscv);
                return IClass.BYTE;
            }
            if (
                rhsType == IClass.INT
                && mhsType == IClass.BYTE
                && UnitCompiler.isByteConstant(rhscv) != null
            ) {

                // Fix up the constant to be a byte
                ce.rhs.constantValue = UnitCompiler.isByteConstant(rhscv);
                return IClass.BYTE;
            }

            // JLS7 15.25, list 1, bullet 4, bullet 4: "b ? Integer : Double => double"
            return this.binaryNumericPromotionType(
                ce,
                this.getUnboxedType(mhsType),
                this.getUnboxedType(rhsType)
            );
        }

        // A conditional that is neither boolean nor numeric is a reference conditional,
        // including a boolean/numeric pair. Box both operands before computing their LUB.
        return this.commonSupertype(
            UnitCompiler.isPrimitive(mhsType) ? this.wrapperOf(UnitCompiler.rawTypeOf(mhsType)) : mhsType,
            UnitCompiler.isPrimitive(rhsType) ? this.wrapperOf(UnitCompiler.rawTypeOf(rhsType)) : rhsType);
    }

    private IType
    getType2(Crement c) throws CompileException { return this.getType(c.operand); }

    private IType
    getType2(ArrayAccessExpression aae) throws CompileException {
        IType componentType = UnitCompiler.getComponentType(this.getType(aae.lhs));
        assert componentType != null : "null component type for " + aae;
        return componentType;
    }

    private IType
    getType2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.getType(this.determineValue(fae));
    }

    private IType
    getType2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.getType(this.determineValue(scfae));
    }

    private IClass
    getType2(UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") return IClass.BOOLEAN; // SUPPRESS CHECKSTYLE StringLiteralEquality

         // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (uo.operator == "+" || uo.operator == "-" || uo.operator == "~") {
            return this.unaryNumericPromotionType(uo, this.getUnboxedType(this.getType(uo.operand)));
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return IClass.BOOLEAN;
    }

    @SuppressWarnings("static-method")
    private IClass
    getType2(Instanceof io) { return IClass.BOOLEAN; }

    private IType
    getType2(BinaryOperation bo) throws CompileException {
        if (
            // SUPPRESS CHECKSTYLE StringLiteralEquality:8
            bo.operator == "||"
            || bo.operator == "&&"
            || bo.operator == "=="
            || bo.operator == "!="
            || bo.operator == "<"
            || bo.operator == ">"
            || bo.operator == "<="
            || bo.operator == ">="
        ) return IClass.BOOLEAN;

        if (bo.operator == "|" || bo.operator == "^" || bo.operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IType lhsType = this.getType(bo.lhs);
            return (
                lhsType == IClass.BOOLEAN || lhsType == this.iClassLoader.TYPE_java_lang_Boolean
                ? IClass.BOOLEAN
                : this.binaryNumericPromotionType(bo, this.getUnboxedType(lhsType), this.getUnboxedType(this.getType(bo.rhs)))
            );
        }

        if (bo.operator == "*" || bo.operator == "/" || bo.operator == "%" || bo.operator == "+" || bo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IClassLoader icl = this.iClassLoader;

            // Unroll the operands of this binary operation.
            Iterator<Rvalue> ops = bo.unrollLeftAssociation();

            IType lhsType = this.getType((Rvalue) ops.next());

            // Check the far left operand type.
            if (bo.operator == "+" && lhsType == icl.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality
                return icl.TYPE_java_lang_String;
            }

            // Determine the expression type.
            lhsType = this.getUnboxedType(lhsType);
            do {
                IType rhsType = this.getUnboxedType(this.getType((Rvalue) ops.next()));
                if (bo.operator == "+" && rhsType == icl.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    return icl.TYPE_java_lang_String;
                }
                lhsType = this.binaryNumericPromotionType(bo, lhsType, rhsType);
            } while (ops.hasNext());

            return lhsType;
        }

        if (bo.operator == "<<"  || bo.operator == ">>"  || bo.operator == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IType lhsType = this.getType(bo.lhs);
            return this.unaryNumericPromotionType(bo, lhsType);
        }

        this.compileError("Unexpected operator \"" + bo.operator + "\"", bo.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    /**
     * @return The <var>type</var>, or, iff <var>type</var> is a primitive wrapper type, the unwrapped <var>type</var>
     */
    private IType
    getUnboxedType(IType type) {
        IClass c = this.isUnboxingConvertible(type);
        return c != null ? c : type;
    }

    private IType
    getType2(Cast c) throws CompileException {
        if (c.additionalBounds.length == 0) return this.getType(c.targetType);
        if (c.intersectionType != null) return c.intersectionType;
        IClass[] bounds = new IClass[c.additionalBounds.length + 1];
        IType first = this.getType(c.targetType);
        if (first instanceof SourceTypeVariable) throw UnitCompiler.compileException(c, "A type variable cannot be followed by intersection bounds");
        bounds[0] = first instanceof IClass ? (IClass) first : UnitCompiler.rawTypeOf(first);
        Set<String> descriptors = new HashSet<>();
        descriptors.add(bounds[0].getDescriptor());
        if (bounds[0].isPrimitive() || bounds[0].isArray()) throw UnitCompiler.compileException(c, "Invalid first intersection bound");
        for (int i = 1; i < bounds.length; i++) {
            IType additional = this.getType(c.additionalBounds[i - 1]);
            if (additional instanceof SourceTypeVariable) throw UnitCompiler.compileException(c, "Intersection requires an interface type");
            bounds[i] = additional instanceof IClass ? (IClass) additional : UnitCompiler.rawTypeOf(additional);
            if (!bounds[i].isInterface() || !descriptors.add(bounds[i].getDescriptor())) {
                throw UnitCompiler.compileException(c, "Invalid or repeated intersection interface " + bounds[i]);
            }
        }
        Map<IClass, IType> hierarchy = new HashMap<>();
        for (IClass bound : bounds) this.checkIntersectionHierarchy(c, bound, hierarchy);
        for (int i = 0; i < bounds.length; i++) for (int j = i + 1; j < bounds.length; j++) {
            IClass a = UnitCompiler.rawTypeOf(bounds[i]), b = UnitCompiler.rawTypeOf(bounds[j]);
            for (IMethod am : a.getIMethods()) for (IMethod bm : b.getIMethods()) {
                if (am.isStatic() || bm.isStatic() || am.getAccess() == Access.PRIVATE || bm.getAccess() == Access.PRIVATE
                    || !am.getName().equals(bm.getName()) || !Arrays.equals(am.getParameterTypes(), bm.getParameterTypes())) continue;
                IType ar = this.functionalReturn(bounds[i], am), br = this.functionalReturn(bounds[j], bm);
                if (!this.genericConvertible(ar, br, false) && !this.genericConvertible(br, ar, false)) {
                    throw UnitCompiler.compileException(c, "Incompatible intersection method returns");
                }
                IClass ao = am.getDeclaringIClass(), bo = bm.getDeclaringIClass();
                if ((!am.isAbstract() || !bm.isAbstract()) && ao.isInterface() && bo.isInterface()
                    && !ao.isAssignableFrom(bo) && !bo.isAssignableFrom(ao)) {
                    throw UnitCompiler.compileException(c, "Conflicting intersection default methods");
                }
            }
        }
        return c.intersectionType = new IntersectionIClass(bounds, this.iClassLoader.TYPE_java_lang_Object);
    }

    private void checkIntersectionHierarchy(Cast cast, IType type, Map<IClass, IType> hierarchy) throws CompileException {
        IClass raw = UnitCompiler.rawTypeOf(type);
        IType previous = hierarchy.get(raw);
        if (previous != null) {
            if (previous instanceof ParameterizedIClass && type instanceof ParameterizedIClass && !previous.equals(type)) {
                throw UnitCompiler.compileException(cast, "Incompatible intersection type arguments for " + raw);
            }
            return;
        }
        hierarchy.put(raw, type);
        Map<String, IType> environment = this.receiverEnvironment(type, raw);
        for (GenericType parent : raw.getGenericSupertypes()) {
            IType resolved = this.substituteGenericType(parent, environment);
            if (resolved != null) this.checkIntersectionHierarchy(cast, resolved, hierarchy);
        }
    }

    private IType
    getType2(ParenthesizedExpression pe) throws CompileException {
        return this.getType(pe.value);
    }

    private IType
    getType2(MethodInvocation mi) throws CompileException {
        // Resolve a fluent receiver chain from the inside out. Recursing through every visitor
        // for a long chain (e.g. hundreds of StringBuilder.append calls) exhausts small VM stacks.
        List<MethodInvocation> receivers = new ArrayList<>();
        for (Atom target = mi.target; target instanceof MethodInvocation; target = ((MethodInvocation) target).target) {
            MethodInvocation receiver = (MethodInvocation) target;
            if (receiver.iMethod != null) break;
            receivers.add(receiver);
        }
        for (int i = receivers.size() - 1; i >= 0; i--) this.getType2(receivers.get(i));
        IMethod iMethod = mi.iMethod != null ? mi.iMethod : (mi.iMethod = this.findIMethod(mi));

        // Substitute type variables with the actual type arguments of a PARAMETERIZED receiver (JLS8 4.5.2),
        // e.g. the return type of "list.get(0)" is "String" if "list" is a "List<String>".
        return this.substitutedReturnType(mi, iMethod);
    }

    private IType
    getType2(SuperclassMethodInvocation scmi) throws CompileException {
        return this.substitutedReturnType(scmi, this.findIMethod(scmi));
    }

    private IType
    getType2(NewClassInstance nci) throws CompileException {
        if (nci.iType != null) return nci.iType;

        assert nci.type != null;
        IType declared = this.getType(nci.type);
        if (!this.isDiamondCreation(nci)) return (nci.iType = declared);
        if (nci.typeArguments != null && !this.inferredReferenceCreations.contains(nci)) {
            throw UnitCompiler.compileException(nci, "Diamond cannot be combined with explicit constructor type arguments");
        }
        IClass raw = UnitCompiler.rawTypeOf(declared);
        IConstructor constructor = (IConstructor) this.findMostSpecificIInvocable(nci, raw.getDeclaredIConstructors(), nci.arguments, nci.getEnclosingScope());
        InferenceContext context = this.constructorInference(nci, constructor, nci.arguments, nci.getEnclosingScope());
        IType inferred = this.substituteGenericType(context.creationPattern, context.values);
        return (nci.iType = inferred == null ? declared : inferred);
    }

    private IClass
    getType2(NewAnonymousClassInstance naci) {
        return this.resolve(naci.anonymousClassDeclaration);
    }

    private IType
    getType2(ParameterAccess pa) throws CompileException {
        return this.getLocalVariable(pa.formalParameter).type;
    }

    private IClass
    getType2(NewArray na) throws CompileException {
        return UnitCompiler.this.iClassLoader.getArrayIClass(
            UnitCompiler.rawTypeOf(this.getType(na.type)),
            na.dimExprs.length + na.dims
        );
    }

    private IType
    getType2(NewInitializedArray nia) throws CompileException {
        IType at = nia.arrayType != null ? this.getType(nia.arrayType) : nia.arrayIClass;
        assert at != null;
        return at;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(IntegerLiteral il) {
        String v        = il.value;
        char   lastChar = v.charAt(v.length() - 1);
        return lastChar == 'l' || lastChar == 'L' ? IClass.LONG : IClass.INT;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(FloatingPointLiteral fpl) {
        String v        = fpl.value;
        char   lastChar = v.charAt(v.length() - 1);
        return lastChar == 'f' || lastChar == 'F' ? IClass.FLOAT : IClass.DOUBLE;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(BooleanLiteral bl) {
        return IClass.BOOLEAN;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(CharacterLiteral cl) {
        return IClass.CHAR;
    }

    private IClass
    getType2(StringLiteral sl) { return this.iClassLoader.TYPE_java_lang_String; }

    private IClass
    getType2(TextBlock tb) { return this.iClassLoader.TYPE_java_lang_String; }

    @SuppressWarnings("static-method") private IClass
    getType2(NullLiteral nl) {
        return IClass.NULL;
    }

    private IClass
    getType2(SimpleConstant sl) {
        Object v = sl.value;
        if (v instanceof Byte)      return IClass.BYTE;
        if (v instanceof Short)     return IClass.SHORT;
        if (v instanceof Integer)   return IClass.INT;
        if (v instanceof Long)      return IClass.LONG;
        if (v instanceof Float)     return IClass.FLOAT;
        if (v instanceof Double)    return IClass.DOUBLE;
        if (v instanceof Boolean)   return IClass.BOOLEAN;
        if (v instanceof Character) return IClass.CHAR;
        if (v instanceof String)    return this.iClassLoader.TYPE_java_lang_String;
        if (v == null)              return IClass.NULL;
        throw new InternalCompilerException(sl.getLocation(), "Invalid SimpleLiteral value type \"" + v.getClass() + "\"");
    }

    // ---------------- Atom.isType() ---------------

    private boolean
    isType(Atom a) throws CompileException {

        Boolean result = (Boolean) a.accept(new AtomVisitor<Boolean, CompileException>() {

            @Override public Boolean visitPackage(Package p) { return UnitCompiler.this.isType2(p); }

            @Override @Nullable public Boolean
            visitType(Type t) {

                return (Boolean) t.accept(new TypeVisitor<Boolean, RuntimeException>() {
                    @Override public Boolean visitArrayType(ArrayType at)                { return UnitCompiler.this.isType2(at);  }
                    @Override public Boolean visitPrimitiveType(PrimitiveType bt)        { return UnitCompiler.this.isType2(bt);  }
                    @Override public Boolean visitReferenceType(ReferenceType rt)        { return UnitCompiler.this.isType2(rt);  }
                    @Override public Boolean visitRvalueMemberType(RvalueMemberType rmt) { return UnitCompiler.this.isType2(rmt); }
                    @Override public Boolean visitSimpleType(SimpleType st)              { return UnitCompiler.this.isType2(st);  }
                });
            }

            @Override @Nullable public Boolean
            visitRvalue(Rvalue rv) throws CompileException {

                return (Boolean) rv.accept(new RvalueVisitor<Boolean, CompileException>() {

                    @Override @Nullable public Boolean
                    visitLvalue(Lvalue lv) throws CompileException {

                        return (Boolean) lv.accept(new LvalueVisitor<Boolean, CompileException>() {
                            @Override public Boolean visitAmbiguousName(AmbiguousName an)                throws CompileException { return UnitCompiler.this.isType2(an);    }
                            @Override public Boolean visitArrayAccessExpression(ArrayAccessExpression aae)                       { return UnitCompiler.this.isType2(aae);   }
                            @Override public Boolean visitFieldAccess(FieldAccess fa)                                            { return UnitCompiler.this.isType2(fa);    }
                            @Override public Boolean visitFieldAccessExpression(FieldAccessExpression fae)                       { return UnitCompiler.this.isType2(fae);   }
                            @Override public Boolean visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { return UnitCompiler.this.isType2(scfae); }
                            @Override public Boolean visitLocalVariableAccess(LocalVariableAccess lva)                           { return UnitCompiler.this.isType2(lva);   }
                            @Override public Boolean visitParenthesizedExpression(ParenthesizedExpression pe)                    { return UnitCompiler.this.isType2(pe);    }
                        });
                    }

                    @Override public Boolean visitArrayLength(ArrayLength al)                                    { return UnitCompiler.this.isType2(al);   }
                    @Override public Boolean visitAssignment(Assignment a)                                       { return UnitCompiler.this.isType2(a);    }
                    @Override public Boolean visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.this.isType2(uo);   }
                    @Override public Boolean visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.this.isType2(bo);   }
                    @Override public Boolean visitCast(Cast c)                                                   { return UnitCompiler.this.isType2(c);    }
                    @Override public Boolean visitClassLiteral(ClassLiteral cl)                                  { return UnitCompiler.this.isType2(cl);   }
                    @Override public Boolean visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.this.isType2(ce);   }
                    @Override public Boolean visitCrement(Crement c)                                             { return UnitCompiler.this.isType2(c);    }
                    @Override public Boolean visitInstanceof(Instanceof io)                                      { return UnitCompiler.this.isType2(io);   }
                    @Override public Boolean visitMethodInvocation(MethodInvocation mi)                          { return UnitCompiler.this.isType2(mi);   }
                    @Override public Boolean visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return UnitCompiler.this.isType2(smi);  }
                    @Override public Boolean visitIntegerLiteral(IntegerLiteral il)                              { return UnitCompiler.this.isType2(il);   }
                    @Override public Boolean visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return UnitCompiler.this.isType2(fpl);  }
                    @Override public Boolean visitBooleanLiteral(BooleanLiteral bl)                              { return UnitCompiler.this.isType2(bl);   }
                    @Override public Boolean visitCharacterLiteral(CharacterLiteral cl)                          { return UnitCompiler.this.isType2(cl);   }
                    @Override public Boolean visitStringLiteral(StringLiteral sl)                                { return UnitCompiler.this.isType2(sl);   }
                    @Override public Boolean visitTextBlock(TextBlock tb)                                        { return UnitCompiler.this.isType2(tb);   }
                    @Override public Boolean visitNullLiteral(NullLiteral nl)                                    { return UnitCompiler.this.isType2(nl);   }
                    @Override public Boolean visitSimpleConstant(SimpleConstant sl)                              { return UnitCompiler.this.isType2(sl);   }
                    @Override public Boolean visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return UnitCompiler.this.isType2(naci); }
                    @Override public Boolean visitNewArray(NewArray na)                                          { return UnitCompiler.this.isType2(na);   }
                    @Override public Boolean visitNewInitializedArray(NewInitializedArray nia)                   { return UnitCompiler.this.isType2(nia);  }
                    @Override public Boolean visitNewClassInstance(NewClassInstance nci)                         { return UnitCompiler.this.isType2(nci);  }
                    @Override public Boolean visitParameterAccess(ParameterAccess pa)                            { return UnitCompiler.this.isType2(pa);   }
                    @Override public Boolean visitQualifiedThisReference(QualifiedThisReference qtr)             { return UnitCompiler.this.isType2(qtr);  }
                    @Override public Boolean visitThisReference(ThisReference tr)                                { return UnitCompiler.this.isType2(tr);   }
                    @Override public Boolean visitLambdaExpression(LambdaExpression le)                          { return UnitCompiler.this.isType2(le);   }
                    @Override public Boolean visitMethodReference(MethodReference mr)                            { return UnitCompiler.this.isType2(mr);   }
                    @Override public Boolean visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return UnitCompiler.this.isType2(cicr); }
                    @Override public Boolean visitArrayCreationReference(ArrayCreationReference acr)             { return UnitCompiler.this.isType2(acr);  }
                });
            }

            @Override @Nullable public Boolean
            visitConstructorInvocation(ConstructorInvocation ci) { return false; }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private boolean
    isType2(Atom a) { return a instanceof Type; }

    private boolean
    isType2(AmbiguousName an) throws CompileException { return this.isType(this.reclassify(an)); }

    /**
     * Determines whether the given {@link IClass.IMember} is accessible in the given context, according to
     * JLS7 6.6.1.BL1.B4. Issues a {@link #compileError(String)} if not.
     */
    private boolean
    isAccessible(IClass.IMember member, Scope contextScope) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass declaringIClass = member.getDeclaringIClass();
        return (
            this.isAccessible(declaringIClass, contextScope)
            && this.isAccessible(declaringIClass, member.getAccess(), contextScope)
        );
    }

    private boolean isAccessible(IClass.IMember member, Scope scope, @Nullable IType receiver) throws CompileException {
        if (this.isAccessible(member, scope)) return true;
        // A public/protected method inherited by an accessible subclass is callable through that subclass,
        // even if its original declaring class is package-private (e.g. AbstractStringBuilder).
        return member instanceof IMethod && receiver != null
            && (member.getAccess() == Access.PUBLIC || member.getAccess() == Access.PROTECTED)
            && this.isAccessible(UnitCompiler.rawTypeOf(receiver), scope)
            && member.getDeclaringIClass().isAssignableFrom(UnitCompiler.rawTypeOf(receiver))
            && this.isAccessible(member.getDeclaringIClass(), member.getAccess(), scope);
    }

    /**
     * Checks whether the given {@link IClass.IMember} is accessible in the given context, according to JLS7
     * 6.6.1.BL1.B4. Issues a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass.IMember member, Scope contextScope, Location location) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass declaringIClass = member.getDeclaringIClass();
        this.checkAccessible(declaringIClass, contextScope, location);
        this.checkMemberAccessible(declaringIClass, member, contextScope, location);
    }

    /**
     * Determines whether a member (class, interface, field or method) declared in a given class is accessible from a
     * given block statement context, according to JLS7 6.6.1.4.
     */
    private boolean
    isAccessible(IClass iClassDeclaringMember, Access memberAccess, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextScope);
    }

    /**
     * Verifies that a member (class, interface, field or method) declared in a given class is accessible from a given
     * block statement context, according to JLS7 6.6.1.4. Issue a {@link #compileError(String)} if not.
     */
    private void
    checkMemberAccessible(
        IClass         iClassDeclaringMember,
        IClass.IMember member,
        Scope          contextScope,
        Location       location
    ) throws CompileException {
        String message = this.internalCheckAccessible(iClassDeclaringMember, member.getAccess(), contextScope);
        if (message != null) this.compileError(member.toString() + ": " + message, location);
    }

    /**
     * @return a descriptive text iff a member declared in that {@link IClass} with that {@link Access} is inaccessible
     */
    @Nullable private String
    internalCheckAccessible(
        IClass iClassDeclaringMember,
        Access memberAccess,
        Scope  contextScope
    ) throws CompileException {

        // At this point, memberAccess is PUBLIC, DEFAULT, PROTECTED or PRIVATE.

        // PUBLIC members are always accessible.
        if (memberAccess == Access.PUBLIC) return null;

        // At this point, the member is DEFAULT, PROTECTED or PRIVATE accessible.

        // Determine the class declaring the context.
        IClass iClassDeclaringContext = null;
        for (Scope s = contextScope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof TypeDeclaration) {
                iClassDeclaringContext = this.resolve((TypeDeclaration) s);
                break;
            }
        }

        // Access is always allowed for block statements declared in the same class as the member.
        if (iClassDeclaringContext == iClassDeclaringMember) return null;

        // Check whether the member and the context block statement are enclosed by the same top-level type.
        if (
            iClassDeclaringContext != null
            && !this.options.contains(JaninoOption.PRIVATE_MEMBERS_OF_ENCLOSING_AND_ENCLOSED_TYPES_INACCESSIBLE)
        ) {
            IClass topLevelIClassEnclosingMember = iClassDeclaringMember;
            for (IClass c = iClassDeclaringMember.getDeclaringIClass(); c != null; c = c.getDeclaringIClass()) {
                topLevelIClassEnclosingMember = c;
            }
            IClass topLevelIClassEnclosingContextBlockStatement = iClassDeclaringContext;
            for (
                IClass c = iClassDeclaringContext.getDeclaringIClass();
                c != null;
                c = c.getDeclaringIClass()
            ) topLevelIClassEnclosingContextBlockStatement = c;

            if (topLevelIClassEnclosingMember == topLevelIClassEnclosingContextBlockStatement) return null;
        }

        if (memberAccess == Access.PRIVATE) {
            return "Private member cannot be accessed from type \"" + iClassDeclaringContext + "\".";
        }

        // At this point, the member is DEFAULT or PROTECTED accessible.

        // Check whether the member and the context block statement are declared in the same package.
        if (iClassDeclaringContext != null && Descriptor.areInSamePackage(
            iClassDeclaringMember.getDescriptor(),
            iClassDeclaringContext.getDescriptor()
        )) return null;

        if (memberAccess == Access.DEFAULT) {
            return (
                "Member with \"package\" access cannot be accessed from type \""
                + iClassDeclaringContext
                + "\"."
            );
        }

        // At this point, the member is PROTECTED accessible.

        // Check whether the class declaring the context block statement is a subclass of the class declaring the
        // member or a nested class whose parent is a subclass
        {
            IClass parentClass = iClassDeclaringContext;
            do {
                assert parentClass != null;
                if (iClassDeclaringMember.isAssignableFrom(parentClass)) {
                    return null;
                }
                parentClass = parentClass.getDeclaringIClass();
            } while (parentClass != null);
        }

        return (
            "Protected member cannot be accessed from type \""
            + iClassDeclaringContext
            + "\", which is neither declared in the same package as nor is a subclass of \""
            + iClassDeclaringMember
            + "\"."
        );
    }

    /**
     * Determines whether the given {@link IClass} is accessible in the given context, according to JLS7 6.6.1.2 and
     * 6.6.1.4.
     */
    private boolean
    isAccessible(IClass type, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(type, contextScope);
    }

    /**
     * Checks whether the given {@link IClass} is accessible in the given context, according to JLS7 6.6.1.2 and
     * 6.6.1.4. Issues a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass type, Scope contextScope, Location location) throws CompileException {
        String message = this.internalCheckAccessible(type, contextScope);
        if (message != null) this.compileError(message, location);
    }

    /**
     * @return An error message, or {@code null}
     */
    @Nullable private String
    internalCheckAccessible(IClass type, Scope contextScope) throws CompileException {

        // Determine the type declaring the type.
        IClass iClassDeclaringType = type.getDeclaringIClass();

        // Check accessibility of package member type.
        if (iClassDeclaringType == null) {
            if (type.getAccess() == Access.PUBLIC) {
                return null;
            } else
            if (type.getAccess() == Access.DEFAULT) {

                // Determine the type declaring the context block statement.
                IClass iClassDeclaringContextBlockStatement;
                for (Scope s = contextScope;; s = s.getEnclosingScope()) {
                    if (s instanceof TypeDeclaration) {
                        iClassDeclaringContextBlockStatement = this.resolve((TypeDeclaration) s);
                        break;
                    }
                    if (s instanceof EnclosingScopeOfTypeDeclaration) {
                        iClassDeclaringContextBlockStatement = this.resolve(
                            ((EnclosingScopeOfTypeDeclaration) s).typeDeclaration
                        );
                        break;
                    }
                }

                // Check whether the type is accessed from within the same package.
                String packageDeclaringType = Descriptor.getPackageName(type.getDescriptor());
                String contextPackage       = Descriptor.getPackageName(iClassDeclaringContextBlockStatement.getDescriptor());
                if (
                    packageDeclaringType == null
                    ? contextPackage != null
                    : !packageDeclaringType.equals(contextPackage)
                ) return "\"" + type + "\" is inaccessible from this package";
                return null;
            } else
            {
                throw new InternalCompilerException((
                    "\"" + type + "\" has unexpected access \"" + type.getAccess() + "\""
                ));
            }
        }

        // "type" is a member type at this point.
        return this.internalCheckAccessible(iClassDeclaringType, type.getAccess(), contextScope);
    }

    private Type
    toTypeOrCompileException(Atom a) throws CompileException {
        Type result = a.toType();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not a type", a.getLocation());
            return new SimpleType(a.getLocation(), this.iClassLoader.TYPE_java_lang_Object);
        }
        return result;
    }

    private Rvalue
    toRvalueOrCompileException(final Atom a) throws CompileException {
        Rvalue result = a.toRvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an rvalue", a.getLocation());
            return new StringLiteral(a.getLocation(), "\"X\"");
        }
        return result;
    }

    private Lvalue
    toLvalueOrCompileException(final Atom a) throws CompileException {
        Lvalue result = a.toLvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an lvalue", a.getLocation());
            return new Lvalue(a.getLocation()) {

                @Override @Nullable public <R, EX extends Throwable> R
                accept(LvalueVisitor<R, EX> visitor) { return null; }

                @Override public String
                toString() { return a.toString(); }
            };
        }
        return result;
    }

    /**
     * The LHS operand of type <var>lhsType</var> is expected on the stack.
     * <p>
     *   The following operators are supported: {@code | ^ & * / % + - << >> >>>}
     * </p>
     */
    private IType
    compileArithmeticBinaryOperation(
        Locatable locatable,
        IType     lhsType,
        String    operator,
        Rvalue    rhs
    ) throws CompileException {
        return this.compileArithmeticOperation(
            locatable,
            lhsType,
            Arrays.asList(rhs).iterator(),
            operator
        );
    }

    /**
     * Executes an arithmetic operation on a sequence of <var>operands</var>. If <var>type</var> is non-{@code null},
     * then the first operand with that type is already on the stack.
     * <p>
     *   The following operators are supported: {@code | ^ & * / % + - << >> >>>}
     * </p>
     */
    private IType
    compileArithmeticOperation(
        final Locatable  locatable,
        @Nullable IType  firstOperandType,
        Iterator<Rvalue> operands,
        String           operator
    ) throws CompileException {

        // A very special case.
        if (operator == "+" && firstOperandType == this.iClassLoader.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality
            assert firstOperandType != null;
            return this.compileStringConcatenation(locatable, firstOperandType, (Rvalue) operands.next(), operands);
        }

        IType type = firstOperandType == null ? this.compileGetValue((Rvalue) operands.next()) : firstOperandType;

        // Operator which is allowed for BYTE, SHORT, INT, LONG and BOOLEAN operands?
        if (operator == "|" || operator == "^" || operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality:5

            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                IType rhsType = this.getType(operand);

                if (this.isConvertibleToPrimitiveNumeric(type) && this.isConvertibleToPrimitiveNumeric(rhsType)) {
                    IClass promotedType = this.binaryNumericPromotionType(
                        operand,
                        this.getUnboxedType(type),
                        this.getUnboxedType(rhsType)
                    );
                    if (promotedType != IClass.INT && promotedType != IClass.LONG) {
                        throw new CompileException("Invalid operand type " + promotedType, operand.getLocation());
                    }
                    this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, type), promotedType);
                    this.compileGetValue(operand);
                    this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, rhsType), promotedType);
                    this.bytecodeEmitter.andOrXor(operand, operator);
                    type = promotedType;
                } else
                if (
                    (type == IClass.BOOLEAN || this.getUnboxedType(type) == IClass.BOOLEAN)
                    && (rhsType == IClass.BOOLEAN || this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                ) {
                    IClassLoader icl = this.iClassLoader;
                    if (type == icl.TYPE_java_lang_Boolean) {
                        this.unboxingConversion(locatable, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                    }
                    this.compileGetValue(operand);
                    if (rhsType == icl.TYPE_java_lang_Boolean) {
                        this.unboxingConversion(locatable, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                    }
                    this.bytecodeEmitter.andOrXor(operand, operator);
                    type = IClass.BOOLEAN;
                } else
                {
                    this.compileError((
                        "Operator \""
                        + operator
                        + "\" not defined on types \""
                        + type
                        + "\" and \""
                        + rhsType
                        + "\""
                    ), locatable.getLocation());
                    type = IClass.INT;
                }
            }
            return type;
        }

        // Operator which is allowed for INT, LONG, FLOAT, DOUBLE and (for operator '+') STRING operands?
        if (operator == "*" || operator == "/" || operator == "%" || operator == "+" || operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality:6
            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                // String concatenation?
                if (operator == "+" && ( // SUPPRESS CHECKSTYLE StringLiteralEquality
                    type == this.iClassLoader.TYPE_java_lang_String
                    || this.getType(operand) == this.iClassLoader.TYPE_java_lang_String
                )) return this.compileStringConcatenation(locatable, type, operand, operands);

                // It's a numeric arithmetic operation.
                IType  rhsType      = this.getType(operand);
                IClass promotedType = this.binaryNumericPromotionType(
                    operand,
                    this.getUnboxedType(type),
                    this.getUnboxedType(rhsType)
                );

                this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, type), promotedType);
                this.compileGetValue(operand);
                this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, rhsType), promotedType);
                this.bytecodeEmitter.mulDivRemAddSub(operand, operator);

                type = promotedType;
            }

            return type;
        }

        // Operator which is allowed for BYTE, SHORT, INT and LONG lhs operand and BYTE, SHORT, INT or LONG rhs operand?
        if (operator == "<<" || operator == ">>" || operator == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality:4

            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                type = this.unaryNumericPromotion(operand, type);

                IType  rhsType         = this.compileGetValue(operand);
                IClass promotedRhsType = this.unaryNumericPromotion(operand, rhsType);
                if (promotedRhsType == IClass.INT) {
                    ;
                } else
                if (promotedRhsType == IClass.LONG) {
                    this.bytecodeEmitter.l2i(operand);
                } else
                {
                    this.compileError(
                        "Shift distance of type \"" + rhsType + "\" is not allowed",
                        locatable.getLocation()
                    );
                }

                this.bytecodeEmitter.shift(operand, operator);
            }

            return type;
        }

        throw new InternalCompilerException(locatable.getLocation(), "Unexpected operator \"" + operator + "\"");
    }

    /**
     * @param type          The type of the first operand, which is already on the stack
     * @param secondOperand The second operand
     * @param operands      All following operands
     */
    private IClass
    compileStringConcatenation(
        final Locatable  locatable,
        IType            type,
        final Rvalue     secondOperand,
        Iterator<Rvalue> operands
    ) throws CompileException {

        // Convert the first operand (which is already on the operand stack) to "String".
        this.stringConversion(locatable, type);

        // Compute list of operands and merge consecutive constant operands.
        List<Rvalue> tmp = new ArrayList<>();
        for (Rvalue nextOperand = secondOperand; nextOperand != null;) {
            Object cv = this.constantEvaluator.getConstantValue(nextOperand);
            if (cv == UnitCompiler.NOT_CONSTANT) {

                // Non-constant operand.
                tmp.add(nextOperand);

                nextOperand = operands.hasNext() ? (Rvalue) operands.next() : null;
            } else
            {
                // Constant operand. Check to see whether the next operand is also constant.
                if (operands.hasNext()) {
                    nextOperand = (Rvalue) operands.next();
                    Object cv2 = this.constantEvaluator.getConstantValue(nextOperand);
                    if (cv2 != UnitCompiler.NOT_CONSTANT) {
                        StringBuilder sb = new StringBuilder(String.valueOf(cv)).append(cv2);
                        for (;;) {
                            if (!operands.hasNext()) {
                                nextOperand = null;
                                break;
                            }
                            nextOperand = (Rvalue) operands.next();
                            Object cv3 = this.constantEvaluator.getConstantValue(nextOperand);
                            if (cv3 == UnitCompiler.NOT_CONSTANT) break;
                            sb.append(cv3);
                        }
                        cv = sb.toString();
                    }
                } else
                {
                    nextOperand = null;
                }

                // Break long string constants up into UTF8-able chunks.
                for (final String s : UnitCompiler.makeUtf8Able(String.valueOf(cv))) {
                    tmp.add(new SimpleConstant(locatable.getLocation(), s));
                }
            }
        }

        // At this point "tmp" contains an optimized sequence of Strings (representing constant portions) and Rvalues
        // (non-constant portions).

        if (tmp.size() <= UnitCompiler.STRING_CONCAT_LIMIT - 1) {

            // String concatenation through "a.concat(b).concat(c)".
            for (Rvalue operand : tmp) {

                // "s.concat(String.valueOf(operand))"
                UnitCompiler.this.stringConversion(operand, UnitCompiler.this.compileStringOperand(operand));
                this.bytecodeEmitter.invokeMethod(locatable, this.iClassLoader.METH_java_lang_String__concat__java_lang_String);
            }
            return this.iClassLoader.TYPE_java_lang_String;
        }

        // String concatenation through "new StringBuilder(a).append(b).append(c).append(d).toString()".
        this.bytecodeEmitter.neW(locatable, this.iClassLoader.TYPE_java_lang_StringBuilder);
        this.bytecodeEmitter.dupx(locatable);
        this.bytecodeEmitter.swap(locatable);

        this.invokeConstructor(locatable, this.iClassLoader.CTOR_java_lang_StringBuilder__java_lang_String);
        this.getCodeContext().popUninitializedVariableOperand();
        this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_STRINGBUILDER);

        for (Iterator<Rvalue> it = tmp.iterator(); it.hasNext();) {
            Rvalue operand = (Rvalue) it.next();

            // "sb.append(operand)"
            IType t = UnitCompiler.this.compileStringOperand(operand);
            this.bytecodeEmitter.invokeMethod(locatable, (
                t == IClass.BYTE    ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.SHORT   ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.INT     ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.LONG    ? this.iClassLoader.METH_java_lang_StringBuilder__append__long    :
                t == IClass.FLOAT   ? this.iClassLoader.METH_java_lang_StringBuilder__append__float   :
                t == IClass.DOUBLE  ? this.iClassLoader.METH_java_lang_StringBuilder__append__double  :
                t == IClass.CHAR    ? this.iClassLoader.METH_java_lang_StringBuilder__append__char    :
                t == IClass.BOOLEAN ? this.iClassLoader.METH_java_lang_StringBuilder__append__boolean :
                this.iClassLoader.METH_java_lang_StringBuilder__append__java_lang_Object
            ));
        }

        // "StringBuilder.toString()":
        this.bytecodeEmitter.invokeMethod(locatable, this.iClassLoader.METH_java_lang_StringBuilder__toString);

        return this.iClassLoader.TYPE_java_lang_String;
    }

    /**
     * Helper interface for string conversion.
     */
    interface Compilable { void compile() throws CompileException; }

    /**
     * Converts object of type "sourceType" to type "String" (JLS7 15.18.1.1).
     */
    private void
    stringConversion(Locatable locatable, IType sourceType) throws CompileException {

        IClass sourceClass = UnitCompiler.rawTypeOf(sourceType);

        this.bytecodeEmitter.invokeMethod(locatable, (
            sourceClass == IClass.BYTE    ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceClass == IClass.SHORT   ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceClass == IClass.INT     ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceClass == IClass.LONG    ? this.iClassLoader.METH_java_lang_String__valueOf__long :
            sourceClass == IClass.FLOAT   ? this.iClassLoader.METH_java_lang_String__valueOf__float :
            sourceClass == IClass.DOUBLE  ? this.iClassLoader.METH_java_lang_String__valueOf__double :
            sourceClass == IClass.CHAR    ? this.iClassLoader.METH_java_lang_String__valueOf__char :
            sourceClass == IClass.BOOLEAN ? this.iClassLoader.METH_java_lang_String__valueOf__boolean :
            this.iClassLoader.METH_java_lang_String__valueOf__java_lang_Object
        ));
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     *   Notice: This method is used both for explicit constructor invocation (first statement of a constructor body)
     *   and implicit constructor invocation (right after NEW).
     * </p>
     *
     * @param enclosingInstance Used if the target class is an inner class
     */
    private void
    invokeConstructor(
        Locatable        locatable,
        Scope            scope,
        @Nullable Rvalue enclosingInstance,
        IType            targetType,
        Rvalue[]         arguments
    ) throws CompileException {

        IClass rawTargetType = UnitCompiler.rawTypeOf(targetType);

        // Find constructors.
        IClass.IConstructor[] iConstructors = rawTargetType.getDeclaredIConstructors(arguments.length);
        if (iConstructors.length == 0) {
            throw UnitCompiler.compileException(locatable, "No applicable constructor for " + rawTargetType);
        }

        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            locatable,     // l
            iConstructors, // iInvocables
            arguments,     // arguments
            scope          // contextScope
        );

        // Check exceptions that the constructor may throw.
        InferenceContext constructorInference = this.constructorInference(locatable, iConstructor, arguments, scope);
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        GenericType[] genericExceptions = iConstructor.getGenericThrownExceptions();
        if (genericExceptions != null && genericExceptions.length == thrownExceptions.length) {
            thrownExceptions = thrownExceptions.clone();
            for (int i = 0; i < thrownExceptions.length; i++) {
                IType type = this.substituteGenericType(genericExceptions[i], constructorInference.values);
                if (type != null) thrownExceptions[i] = UnitCompiler.rawTypeOf(type);
            }
        }
        for (IClass te : thrownExceptions) {
            this.checkThrownException(locatable, te, scope);
        }

        // Enum constant: Pass constant name and ordinal as synthetic parameters.
        if (scope instanceof ConstructorDeclarator && rawTargetType.isEnum()) {
            ConstructorDeclarator constructor = (ConstructorDeclarator) scope;
            LocalVariable name = constructor.syntheticParameters.get("$name");
            LocalVariable ordinal = constructor.syntheticParameters.get("$ordinal");
            if (name != null && ordinal != null) {
                this.bytecodeEmitter.load(locatable, name);
                this.bytecodeEmitter.load(locatable, ordinal);
            }
        }
        ENUM_CONSTANT:
        if (
            scope instanceof FieldDeclaration
            && scope.getEnclosingScope() instanceof EnumDeclaration
        ) {

            FieldDeclaration fd = (FieldDeclaration) scope;
            EnumDeclaration  ed = (EnumDeclaration) fd.getEnclosingScope();
            if (!this.resolve(ed).isAssignableFrom(rawTargetType)) break ENUM_CONSTANT;

            if (fd.variableDeclarators.length != 1) break ENUM_CONSTANT;

            String fieldName = fd.variableDeclarators[0].name;

            int ordinal = 0;
            for (EnumConstant ec : ed.getConstants()) {
                if (fieldName.equals(ec.name)) {

                    // Now we know that this field IS an enum constant.
                    this.consT(locatable, fieldName);
                    this.consT(locatable, ordinal);
                    break ENUM_CONSTANT;
                }
                ordinal++;
            }
        }

        // Pass enclosing instance as a synthetic parameter.
        if (enclosingInstance != null) {
            IClass outerIClass = rawTargetType.getOuterIClass();
            if (outerIClass != null) {
                IType eiic = this.compileGetValue(enclosingInstance);
                if (!UnitCompiler.isAssignableFrom(outerIClass, eiic)) {
                    this.compileError(
                        "Type of enclosing instance (\"" + eiic + "\") is not assignable to \"" + outerIClass + "\"",
                        locatable.getLocation()
                    );
                }
            }
        }

        // Pass local variables to constructor as synthetic parameters.
        {
            IClass.IField[] syntheticFields = rawTargetType.getSyntheticIFields();

            // Determine enclosing function declarator and type declaration.
            TypeBodyDeclaration scopeTbd;
            TypeDeclaration     scopeTypeDeclaration;
            {
                Scope s = scope;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
                scopeTbd             = (TypeBodyDeclaration) s;
                scopeTypeDeclaration = scopeTbd.getDeclaringType();
            }

            // Interface default/static methods can also instantiate capturing anonymous classes.
            {
                AbstractClassDeclaration scopeClassDeclaration = scopeTypeDeclaration instanceof AbstractClassDeclaration
                    ? (AbstractClassDeclaration) scopeTypeDeclaration : null;
                for (IClass.IField sf : syntheticFields) {
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = scopeClassDeclaration == null ? null : (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTbd instanceof MethodDeclarator) {
                            this.bytecodeEmitter.load(locatable, this.resolve(scopeClassDeclaration), 0);
                            this.bytecodeEmitter.getfield(locatable, eisf);
                        } else
                        if (scopeTbd instanceof ConstructorDeclarator) {
                            ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) scopeTbd;
                            LocalVariable         syntheticParameter    = (
                                (LocalVariable) constructorDeclarator.syntheticParameters.get(sf.getName())
                            );
                            if (syntheticParameter == null) {
                                this.compileError((
                                    "Compiler limitation: Constructor cannot access local variable \""
                                    + sf.getName().substring(4)
                                    + "\" declared in an enclosing block because none of the methods accesses it. "
                                    + "As a workaround, declare a dummy method that accesses the local variable."
                                ), locatable.getLocation());
                                this.consT(locatable, (Object) null);
                            } else {
                                this.bytecodeEmitter.load(locatable, syntheticParameter);
                            }
                        } else
                        if (scopeTbd instanceof FieldDeclaration) {
                            this.compileError((
                                "Compiler limitation: Field initializers cannot access local variable \""
                                + sf.getName().substring(4)
                                + "\" declared in an enclosing block because none of the methods accesses it. "
                                + "As a workaround, declare a dummy method that accesses the local variable."
                            ), locatable.getLocation());
                            this.consT(locatable, (Object) null);
                        } else
                        {
                            throw new AssertionError(scopeTbd);
                        }
                    } else {
                        String        localVariableName = sf.getName().substring(4);
                        LocalVariable lv;
                        DETERMINE_LV: {
                            // The lexical map also includes switch-group locals, catch parameters and for-init
                            // variables, which cannot all be found by scanning enclosing Block.statements.
                            if (scope instanceof BlockStatement) {
                                lv = ((BlockStatement) scope).findLocalVariable(localVariableName);
                                if (lv != null) break DETERMINE_LV;
                            }
                            Scope s;

                            // Does one of the enclosing blocks declare a local variable with that name?
                            for (s = scope; s instanceof BlockStatement; s = s.getEnclosingScope()) {
                                BlockStatement       bs = (BlockStatement) s;
                                Scope                es = bs.getEnclosingScope();

                                List<? extends BlockStatement> statements;
                                if (es instanceof Block) {
                                    statements = ((Block) es).statements;
                                } else
                                if (es instanceof FunctionDeclarator) {
                                    statements = ((FunctionDeclarator) es).statements;
                                } else
                                if (es instanceof ForEachStatement) {
                                    FunctionDeclarator.FormalParameter fp = ((ForEachStatement) es).currentElement;
                                    if (fp.name.equals(localVariableName)) {
                                        lv = this.getLocalVariable(fp);
                                        break DETERMINE_LV;
                                    }
                                    continue;
                                } else
                                {
                                    continue;
                                }

                                if (statements != null) {
                                    for (BlockStatement bs2 : statements) {
                                        if (bs2 == bs) break;
                                        if (bs2 instanceof LocalVariableDeclarationStatement) {
                                            LocalVariableDeclarationStatement lvds = (
                                                (LocalVariableDeclarationStatement) bs2
                                            );
                                            for (VariableDeclarator vd : lvds.variableDeclarators) {
                                                if (vd.name.equals(localVariableName)) {
                                                    lv = this.getLocalVariable(lvds, vd);
                                                    break DETERMINE_LV;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Does the declaring function declare a parameter with that name?
                            while (!(s instanceof FunctionDeclarator)) s = s.getEnclosingScope();
                            FunctionDeclarator fd = (FunctionDeclarator) s;
                            for (FormalParameter fp : fd.formalParameters.parameters) {
                                if (fp.name.equals(localVariableName)) {
                                    lv = this.getLocalVariable(fp);
                                    break DETERMINE_LV;
                                }
                            }
                            throw new InternalCompilerException(fd.getLocation(), (
                                "SNO: Synthetic field \""
                                + sf.getName()
                                + "\" neither maps a synthetic field of an enclosing instance nor a local variable"
                            ));
                        }
                        this.bytecodeEmitter.load(locatable, lv);
                    }
                }
            }
        }

        // Preserve instantiated functional targets while emitting erased descriptors.
        IType[] constructorTargets = constructorInference.parameterTargets;
        // Evaluate constructor arguments.
        Rvalue[] adjustedArgs   = null;
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        int      actualSize     = arguments.length;
        if (iConstructor.isVarargs() && iConstructor.argsNeedAdjust()) {
            adjustedArgs = new Rvalue[parameterTypes.length];
            Rvalue[] lastArgs = new Rvalue[actualSize - parameterTypes.length + 1];
            for (int i = 0, j = parameterTypes.length - 1; i < lastArgs.length; ++i, ++j) {
                lastArgs[i] = arguments[j];
            }

            for (int i = parameterTypes.length - 2; i >= 0; --i) {
                adjustedArgs[i] = arguments[i];
            }
            Location loc = (lastArgs.length == 0 ? locatable : lastArgs[lastArgs.length - 1]).getLocation();
            adjustedArgs[adjustedArgs.length - 1] = new NewInitializedArray(
                loc,                                       // location
                parameterTypes[parameterTypes.length - 1], // arrayIClass
                new ArrayInitializer(loc, lastArgs)        // arrayInitializer
            );
            arguments = adjustedArgs;
        }

        for (int i = 0; i < arguments.length; ++i) {
            this.setLambdaTargetType(arguments[i], constructorTargets[i]);
            this.assignmentConversion(
                locatable,                          // locatable
                this.compileGetValue(arguments[i]), // sourceType
                parameterTypes[i],                  // targetType
                this.constantEvaluator.getConstantValue(arguments[i]) // constantValue
            );
        }

        TypeArgument[] explicitArguments = locatable instanceof NewClassInstance ? ((NewClassInstance) locatable).typeArguments
            : locatable instanceof ConstructorInvocation ? ((ConstructorInvocation) locatable).typeArguments : null;
        if (this.getTargetVersion() >= 8 && explicitArguments != null && !this.referenceDelegates.contains(locatable)) {
            CodeContext.Offset offset = this.getCodeContext().newOffset();
            for (int i = 0; i < explicitArguments.length; i++) if (explicitArguments[i] instanceof Type) this.annotationCompiler.compileCodeTypeAnnotations(
                (Type) explicitArguments[i], new Annotation[0], (byte) 0x48, this.getCodeContext(), offset, null, i);
        }
        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()", prepended with the synthetic parameters.
        this.invokeConstructor(locatable, iConstructor);
    }

    /**
     * @return The {@link IField}s that are declared by the <var>fieldDeclaration</var>
     */
    private IClass.IField[]
    compileFields(final FieldDeclaration fieldDeclaration) {

        IClass.IField[] result = new IClass.IField[fieldDeclaration.variableDeclarators.length];

        for (int i = 0; i < result.length; ++i) {
            VariableDeclarator vd = fieldDeclaration.variableDeclarators[i];
            result[i] = this.compileField(
                fieldDeclaration.getDeclaringType(),
                fieldDeclaration.getAnnotations(),
                fieldDeclaration.getAccess(),
                fieldDeclaration.isStatic(),
                fieldDeclaration.isFinal(),
                fieldDeclaration.type,
                vd.brackets,
                vd.name,
                vd.initializer
            );
        }

        return result;
    }

    /**
     * Compiles one variable declarator into an {@link IField}.
     * <p>
     *   Example: "b" in in the variable declaration
     * </p>
     * <pre>
     *     class Foo {
     *         &#64;Deprecated private int[] a, b[], c;
     *     }
     * </pre>
     *
     * @param declaringType      "{@code class Foo}"
     * @param type               "{@code int[]}"
     */
    private IField
    compileField(
        final TypeDeclaration                    declaringType,
        final Annotation[]                       annotations,
        final Access                             access,
        final boolean                            statiC,
        final boolean                            finaL,
        final Type                               type,
        final int                                brackets,
        final String                             name,
        @Nullable final ArrayInitializerOrRvalue initializer
    ) {

        return this.resolve(declaringType).new IField() {
            @Override public GenericType getGenericType() throws CompileException {
                GenericType result = UnitCompiler.this.sourceGenericType(type);
                for (int i = 0; i < brackets; i++) result = new GenericType.ArrayOf(result);
                return result;
            }

            @Nullable private IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() { return declaringType instanceof InterfaceDeclaration ? Access.PUBLIC : access; }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(annotations));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(declaringType.getLocation(), null, ce);
                }
            }

            // Implement "IField".

            @Override public boolean
            isStatic() { return declaringType instanceof InterfaceDeclaration || statiC; }

            @Override public IClass
            getType() throws CompileException {
                if (brackets == 0) {

                    // Preserve an explicit parameterization ("Iterator<String>"), so that member accesses
                    // through this field substitute type variables; for all other purposes (e.g. the field
                    // descriptor) the parameterized type behaves like the raw type.
                    IType t = UnitCompiler.this.getType(type);
                    return t instanceof IClass ? (IClass) t : UnitCompiler.rawTypeOf(t);
                }
                return UnitCompiler.this.iClassLoader.getArrayIClass(
                    UnitCompiler.this.getRawType(type),
                    brackets
                );
            }

            @Override public String
            getName() { return name; }

            @Override @Nullable public Object
            getConstantValue() throws CompileException {
                // Only primitive and String variables are constant variables (JLS 4.12.4).
                // Boxed final fields must retain their initializer and its boxing operation.
                IClass fieldType = this.getType();
                if ((fieldType.isPrimitive() || fieldType == UnitCompiler.this.iClassLoader.TYPE_java_lang_String)
                    && (finaL || declaringType instanceof InterfaceDeclaration) && initializer != null) {
                    Object constantInitializerValue = UnitCompiler.this.constantEvaluator.getConstantValue(initializer);
                    if (constantInitializerValue != UnitCompiler.NOT_CONSTANT) {
                        return UnitCompiler.this.constantAssignmentConversion(
                            initializer,              // locatable
                            constantInitializerValue, // value
                            this.getType()            // targetType
                        );
                    }
                }
                return UnitCompiler.NOT_CONSTANT;
            }
        };
    }

    /**
     * Determines the non-constant-final initializer of the given {@link VariableDeclarator}.
     *
     * @return {@code null} if the variable is declared without an initializer or if the initializer is
     *         constant-final
     */
    @Nullable ArrayInitializerOrRvalue
    getNonConstantFinalInitializer(FieldDeclaration fd, VariableDeclarator vd) throws CompileException {

        // Check if optional initializer exists.
        if (vd.initializer == null) return null;

        // Check if initializer is constant-final.
        if (
            fd.isStatic()
            && fd.isFinal()
            && (this.getRawType(vd.withType(fd.type)).isPrimitive()
                || this.getRawType(vd.withType(fd.type)) == this.iClassLoader.TYPE_java_lang_String)
            && vd.initializer instanceof Rvalue
            && this.constantEvaluator.getConstantValue((Rvalue) vd.initializer) != UnitCompiler.NOT_CONSTANT
        ) return null;

        return vd.initializer;
    }

    private Atom
    reclassify(AmbiguousName an) throws CompileException {

        if (an.reclassified != null) return an.reclassified;

        return (an.reclassified = this.reclassifyName(an.getLocation(), an.getEnclosingScope(), an.identifiers, an.n));
    }

    private IAnnotation[]
    toIAnnotations(Annotation[] annotations) throws CompileException {

        IAnnotation[] result = new IAnnotation[annotations.length];
        for (int i = 0; i < result.length; i++) result[i] = this.toIAnnotation(annotations[i]);

        return result;
    }

    private IAnnotation
    toIAnnotation(Annotation annotation) throws CompileException {

        IAnnotation result = (IAnnotation) annotation.accept(
            new AnnotationVisitor<IAnnotation, CompileException>() {

                @Override public IAnnotation
                visitMarkerAnnotation(final MarkerAnnotation ma) throws CompileException {
                    return this.toIAnnotation(ma.type, new ElementValuePair[0]);
                }

                @Override public IAnnotation
                visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                    return this.toIAnnotation(
                        sea.type,
                        new ElementValuePair[] { new ElementValuePair("value", sea.elementValue) }
                    );
                }

                @Override public IAnnotation
                visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                    return this.toIAnnotation(na.type, na.elementValuePairs);
                }

                private IAnnotation
                toIAnnotation(final Type type, ElementValuePair[] elementValuePairs) throws CompileException {

                    final Map<String, Object> m = new HashMap<>();
                    for (ElementValuePair evp : elementValuePairs) {
                        m.put(evp.identifier, this.toObject(evp.elementValue));
                    }

                    return new IAnnotation() {

                        @Override public Object
                        getElementValue(String name) { return m.get(name); }

                        @Override public IType
                        getAnnotationType() throws CompileException { return UnitCompiler.this.getType(type); }
                    };
                }

                /**
                 * @return A wrapped primitive value, a {@link String}, an {@link IClass} (representing a class
                 *         literal), an {@link IClass.IField} (representing an enum constant), or an {@link Object}[]
                 *         array containing any of the previously described
                 */
                private Object
                toObject(ElementValue ev) throws CompileException {

                    try {
                        Object result = ev.accept(new ElementValueVisitor<Object, CompileException>() {

                            @Override public Object
                            visitRvalue(Rvalue rv) throws CompileException {

                                if (rv instanceof AmbiguousName) {
                                    AmbiguousName an = (AmbiguousName) rv;
                                    rv = UnitCompiler.this.reclassify(an).toRvalueOrCompileException();
                                }

                                // Class literal?
                                if (rv instanceof ClassLiteral) {
                                    ClassLiteral cl = (ClassLiteral) rv;
                                    return UnitCompiler.this.getType(cl.type);
                                }

                                // Enum constant?
                                if (rv instanceof FieldAccess) {
                                    FieldAccess fa = (FieldAccess) rv;
                                    return fa.field;
                                }

                                Object result = UnitCompiler.this.constantEvaluator.getConstantValue(rv);
                                if (result == null) {
                                    UnitCompiler.this.compileError(
                                        "Null value not allowed as an element value",
                                        rv.getLocation()
                                    );
                                    return 1;
                                }
                                if (result == UnitCompiler.NOT_CONSTANT) {
                                    UnitCompiler.this.compileError(
                                        "Element value is not a constant expression",
                                        rv.getLocation()
                                    );
                                    return 1;
                                }
                                return result;
                            }

                            @Override public Object
                            visitAnnotation(Annotation a) throws CompileException {
                                return UnitCompiler.this.toIAnnotation(a);
                            }

                            @Override public Object
                            visitElementValueArrayInitializer(ElementValueArrayInitializer evai)
                            throws CompileException {
                                Object[] result = new Object[evai.elementValues.length];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = toObject(evai.elementValues[i]);
                                }
                                return result;
                            }
                        });
                        assert result != null;
                        return result;
                    } catch (/*CompileException*/ Exception ce) {
                        if (ce instanceof CompileException) throw (CompileException) ce;
                        throw new IllegalStateException(ce);
                    }
                }
            }
        );

        assert result != null;
        return result;
    }

    /**
     * Reclassifies the ambiguous name consisting of the first <var>n</var> of the <var>identifiers</var> (JLS7
     * 6.5.2.2).
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String[] identifiers, int n) throws CompileException {

        if (n == 1) return this.reclassifyName(location, scope, identifiers[0]);

        // 6.5.2.2
        Atom   lhs = this.reclassifyName(location, scope, identifiers, n - 1);
        String rhs = identifiers[n - 1];

        // 6.5.2.2.1
        UnitCompiler.LOGGER.log(Level.FINE, "lhs={0}", lhs);
        if (lhs instanceof Package) {
            String className = ((Package) lhs).name + '.' + rhs;
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return new SimpleType(location, result);

            return new Package(location, className);
        }

        // 6.5.2.2.3.2 EXPRESSION.length
        if ("length".equals(rhs) && UnitCompiler.isArray(this.getType(lhs))) {
            ArrayLength al = new ArrayLength(location, this.toRvalueOrCompileException(lhs));
            if (!(scope instanceof BlockStatement)) {
                this.compileError("\".length\" only allowed in expression context");
                return al;
            }
            al.setEnclosingScope(scope);
            return al;
        }

        IType lhsType = this.getType(lhs);

        // Notice: Don't need to check for 6.5.2.2.2.1 TYPE.METHOD and 6.5.2.2.3.1 EXPRESSION.METHOD here because that
        // has been done before.

        {
            IClass.IField field = this.findIField(UnitCompiler.rawTypeOf(lhsType), rhs, location);
            if (field != null) {
                // 6.5.2.2.2.2 TYPE.FIELD
                // 6.5.2.2.3.2 EXPRESSION.FIELD
                FieldAccess fa = new FieldAccess(
                    location,
                    lhs,
                    field
                );
                fa.setEnclosingScope(scope);
                return fa;
            }
        }

        IClass memberType = this.findMemberType(lhsType, rhs, null, location);
        if (memberType != null) return new SimpleType(location, memberType);

        this.compileError(
            "\"" + rhs + "\" is neither a method, a field, nor a member class of \"" + lhsType + "\"",
            location
        );
        return new Atom(location) {

            @Override @Nullable public <R, EX extends Throwable> R
            accept(AtomVisitor<R, EX> visitor) { return null; }

            @Override public String
            toString() { return Java.join(identifiers, "."); }
        };
    }

    /**
     * Finds the named {@link IClass} in this compilation unit, or through the {@link #iClassLoader}.
     *
     * @param className         Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return                  {@code null} iff an {@link IClass} with that name could not be loaded
     * @throws CompileException An exception was raised while loading the {@link IClass}
     */
    @Nullable private IClass
    findTypeByName(Location location, String className) throws CompileException {

        // Is the type defined in the same compilation unit?
        IClass res = this.findClass(className);
        if (res != null) return res;

        // Is the type defined on the classpath?
        try {
            return this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
        } catch (ClassNotFoundException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CompileException) throw (CompileException) cause;
            throw new CompileException(className, location, ex);
        }
    }

    /**
     * JLS7 6.5.2.1
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String identifier) throws CompileException {

        // Determine scope block statement, type body declaration, type and compilation unit.
        TypeBodyDeclaration     scopeTbd             = null;
        AbstractTypeDeclaration scopeTypeDeclaration = null;
        CompilationUnit         scopeCompilationUnit;
        {
            Scope s = scope;
            while (
                (s instanceof BlockStatement || s instanceof CatchClause)
                && !(s instanceof TypeBodyDeclaration)
            ) s = s.getEnclosingScope();
            if (s instanceof TypeBodyDeclaration) {
                scopeTbd = (TypeBodyDeclaration) s;
                s        = s.getEnclosingScope();
            }
            if (s instanceof TypeDeclaration) {
                scopeTypeDeclaration = (AbstractTypeDeclaration) s;
                s                    = s.getEnclosingScope();
            }
            while (!(s instanceof CompilationUnit)) s = s.getEnclosingScope();
            scopeCompilationUnit = (CompilationUnit) s;
        }

        // 6.5.2.1.BL1

        // 6.5.2.BL1.B1.B1.1 (JLS7: 6.5.2.BL1.B1.B1.1) / 6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2 (JLS7: 6.5.2.BL1.B1.B1.2) / 6.5.6.1.1 Parameter.
        {
            Scope s = scope;
            if (s instanceof BlockStatement) {
                BlockStatement bs = (BlockStatement) s;
                LocalVariable  lv = bs.findLocalVariable(identifier);
                if (lv != null) {
                    LocalVariableAccess lva = new LocalVariableAccess(location, lv);
                    lva.setEnclosingScope(bs);
                    return lva;
                }
                s = s.getEnclosingScope();
            }
            while (s instanceof BlockStatement || s instanceof CatchClause) s = s.getEnclosingScope();
            if (s instanceof FunctionDeclarator) {
                s = s.getEnclosingScope();
            }
            if (s instanceof InnerClassDeclaration) {
                InnerClassDeclaration icd = (InnerClassDeclaration) s; // SUPPRESS CHECKSTYLE UsageDistance

                s = s.getEnclosingScope();
                if (s instanceof AnonymousClassDeclaration) {
                    s = s.getEnclosingScope();
                } else
                if (s instanceof FieldDeclaration) {
                    s = s.getEnclosingScope().getEnclosingScope();
                }
                while (s instanceof BlockStatement) {
                    LocalVariable lv = ((BlockStatement) s).findLocalVariable(identifier);
                    if (lv != null) {
                        if (!lv.finaL && !lv.effectivelyFinal) {
                            this.compileError((
                                "Cannot access non-final local variable \""
                                + identifier
                                + "\" from inner class"
                            ), location);
                        }
                        final IType   lvType = lv.type;
                        IClass.IField iField = new SimpleIField(
                            this.resolve(icd),
                            "val$" + identifier,
                            lvType
                        );
                        icd.defineSyntheticField(iField);
                        FieldAccess fa = new FieldAccess(
                            location,                                   // location
                            new QualifiedThisReference(                 // lhs
                                location,                                   // location
                                new SimpleType(location, this.resolve(icd)) // qualification
                            ),
                            iField                                      // field
                        );
                        fa.setEnclosingScope(scope);
                        return fa;
                    }
                    s = s.getEnclosingScope();
                    while (s instanceof BlockStatement) s = s.getEnclosingScope();
                    if (!(s instanceof FunctionDeclarator)) break;
                    s = s.getEnclosingScope();
                    if (!(s instanceof InnerClassDeclaration)) break;
                    icd = (InnerClassDeclaration) s;
                    s   = s.getEnclosingScope();
                }
            }
        }

        // 6.5.2.BL1.B1.B1.3 (JLS7: 6.5.2.BL1.B1.B1.3) / 6.5.6.1.2.1 Field.
        BlockStatement enclosingBlockStatement = null;
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof BlockStatement && enclosingBlockStatement == null) {
                enclosingBlockStatement = (BlockStatement) s;
            }
            if (s instanceof TypeDeclaration) {
                final AbstractTypeDeclaration enclosingTypeDecl = (AbstractTypeDeclaration) s;
                final IClass                  etd               = this.resolve(enclosingTypeDecl);
                final IClass.IField           f                 = this.findIField(etd, identifier, location);
                if (f != null) {
                    if (f.isStatic()) {
                        this.warning("IASF", (
                            "Implicit access to static field \""
                            + identifier
                            + "\" of declaring class (better write \""
                            + f.getDeclaringIClass()
                            + '.'
                            + f.getName()
                            + "\")"
                        ), location);
                    } else
                    if (f.getDeclaringIClass() == etd) {
                        this.warning("IANSF", (
                            "Implicit access to non-static field \""
                            + identifier
                            + "\" of declaring class (better write \"this."
                            + f.getName()
                            + "\")"
                        ), location);
                    } else {
                        this.warning("IANSFEI", (
                            "Implicit access to non-static field \""
                            + identifier
                            + "\" of enclosing instance (better write \""
                            + f.getDeclaringIClass()
                            + ".this."
                            + f.getName()
                            + "\")"
                        ), location);
                    }

                    assert scopeTypeDeclaration != null;
                    SimpleType ct = new SimpleType(scopeTypeDeclaration.getLocation(), etd);

                    Atom lhs;
                    if (scopeTbd == null) {

                        // Field access in top-level type declaration context (member annotation).
                        lhs = ct;
                    } else
                    if (scopeTbd instanceof MethodDeclarator && ((MethodDeclarator) scopeTbd).isStatic()) {

                        // Field access in static method context.
                        lhs = ct;
                    } else
                    {

                        // Field access in non-static method context.
                        if (f.isStatic()) {

                            // Access to static field.
                            lhs = ct;
                        } else {

                            // Access to non-static field.
                            lhs = new QualifiedThisReference(location, ct);
                        }
                    }

                    Rvalue res = new FieldAccess(location, lhs, f);
                    res.setEnclosingScope(scope);

                    return res;
                }
            }
        }

        // JLS7 6.5.2.BL1.B2.1 Static field imported through single static import.
        for (IField f : Iterables.filterByClass(this.importSingleStatic(identifier), IField.class)) {

            if (this.isAccessible(f, scope)) {

                FieldAccess fieldAccess = new FieldAccess(
                    location,
                    new SimpleType(location, f.getDeclaringIClass()),
                    f
                );
                fieldAccess.setEnclosingScope(scope);
                return fieldAccess;
            }
        }

        // JLS7 6.5.2.BL1.B2.2 Static field imported through static-import-on-demand.
        for (IField f : Iterables.filterByClass(this.importStaticOnDemand(identifier), IField.class)) {

            if (this.isAccessible(f, scope)) {

                FieldAccess fieldAccess = new FieldAccess(
                    location,
                    new SimpleType(location, f.getDeclaringIClass()),
                    f
                );
                fieldAccess.setEnclosingScope(scope);
                return fieldAccess;
            }
        }

        // Hack: "java" MUST be a package, not a class.
        ReferenceType variableReference = new ReferenceType(location, new Annotation[0], new String[] { identifier }, null);
        variableReference.setEnclosingScope(scope);
        SourceTypeVariable variable = this.lexicalVariable(variableReference);
        if (variable != null) return new SimpleType(location, variable);
        if ("java".equals(identifier)) return new Package(location, identifier);

        // JLS7: 6.5.2.BL1.B3.1 Unnamed package class
        // JLS7: 6.5.2.BL1.B3.2 Unnamed package interface
        // JLS7: 7.4.2
        {
            IClass unnamedPackageType = this.findTypeByName(location, identifier);
            if (unnamedPackageType != null) return new SimpleType(location, unnamedPackageType);
        }

        // 6.5.2.BL1.B1.B2.1 (JLS7: 6.5.2.BL1.B3.3) Local class.
        {
            LocalClassDeclaration lcd = UnitCompiler.findLocalClassDeclaration(scope, identifier);
            if (lcd != null) return new SimpleType(location, this.resolve(lcd));
        }

        // 6.5.2.BL1.B1.B2.2 (JLS7: 6.5.2.BL1.B3.4) Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = this.findMemberType(
                this.resolve(scopeTypeDeclaration),
                identifier,
                null,                               // typeArguments
                location
            );
            if (memberType != null) return new SimpleType(location, memberType);
        }

        // 6.5.2.BL1.B1.B3.1 (JLS7: 6.5.2.BL1.B1.B4.1) Single type import.
        {
            IClass iClass = this.importSingleType(identifier, location);
            if (iClass != null) return new SimpleType(location, iClass);
        }

        // 6.5.2.BL1.B1.B3.2 (JLS7: 6.5.2.BL1.B1.B3.1) Package member class/interface declared in this compilation
        // unit.
        // Notice that JLS2 looks this up AFTER local class, member type, single type import, while JLS3 looks this up
        // BEFORE local class, member type, single type import.
        {
            PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(identifier);
            if (pmtd != null) return new SimpleType(location, this.resolve(pmtd));
        }

        // 6.5.2.BL1.B1.B4 Class or interface declared in same package.
        // Notice: Why is this missing in JLS3?
        {
            PackageDeclaration opd = scopeCompilationUnit.packageDeclaration;

            String className = opd == null ? identifier : opd.packageName + '.' + identifier;

            IClass result = this.findTypeByName(location, className);
            if (result != null) return new SimpleType(location, result);
        }

        // 6.5.2.BL1.B1.B5 (JLS7: 6.5.2.BL1.B1.B4.2), 6.5.2.BL1.B1.B6 Type-import-on-demand.
        {
            IClass importedClass = this.importTypeOnDemand(identifier, location);
            if (importedClass != null) return new SimpleType(location, importedClass);
        }

        // JLS7 6.5.2.BL1.B1.B4.3 Type imported through single static import.
        {
            Iterator<IClass>
            it = Iterables.filterByClass(this.importSingleStatic(identifier).iterator(), IClass.class);
            if (it.hasNext()) return new SimpleType(location, (IClass) it.next());
        }

        // JLS7 6.5.2.BL1.B1.B4.4 Member type imported through static-import-on-demand.
        for (IClass mt : Iterables.filterByClass(this.importStaticOnDemand(identifier), IClass.class)) {
            if (this.isAccessible(mt, scope)) return new SimpleType(location, mt);
        }

        // 6.5.2.BL1.B1.B7 Package name
        return new Package(location, identifier);
    }

    /**
     * Imports a member class, member interface, static field or static method via the compilation unit's single
     * static import declarations.
     *
     * @return A list of {@link IField}s, {@link IMethod}s and/or {@link IClass}es with that <var>simpleName</var>;
     *         may be empty
     */
    private List<Object>
    importSingleStatic(String simpleName) throws CompileException {

        List<Object> result = new ArrayList<>();
        for (SingleStaticImportDeclaration ssid : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            SingleStaticImportDeclaration.class
        )) {

            if (simpleName.equals(UnitCompiler.last(ssid.identifiers))) {

                IClass declaringIClass = this.findTypeByFullyQualifiedName(
                    ssid.getLocation(),
                    UnitCompiler.allButLast(ssid.identifiers)
                );
                if (declaringIClass != null) {
                    this.importStatic(declaringIClass, simpleName, result, ssid.getLocation());
                }
            }
        }

        return result;
    }

    /**
     * Finds all members (member classes, member interfaces, static fields and/or static methods) of the
     * <var>declaringIClass</var> with the given <var>simpleName</var> and adds them to the <var>result</var>.
     *
     * @param declaringIClass The class or interface that declares the members
     * @param result          Results ({@link IClass}es, {@link IField}s and/or {@link IMethod}s) are added to this
     *                        collection
     */
    private void
    importStatic(IClass declaringIClass, String simpleName, Collection<Object> result, Location location)
    throws CompileException {

        // Member type?
        for (IClass memberIClass : declaringIClass.findMemberType(simpleName)) {
            if (memberIClass.getDeclaringIClass() == declaringIClass) result.add(memberIClass);
        }

        // Static field?
        {
            IField iField = this.findIField(declaringIClass, simpleName, location);
            if (iField != null && iField.isStatic()) {
                result.add(iField);
            }
        }

        // Static method?
        for (IMethod iMethod : declaringIClass.getDeclaredIMethods(simpleName)) {
            if (iMethod.isStatic()) result.add(iMethod);
        }
    }

    /**
     * @return Either the {@link FieldAccess} or an {@link ArrayLength}
     */
    private Rvalue
    determineValue(FieldAccessExpression fae) throws CompileException {
        if (fae.value != null) return fae.value;

        IType lhsType = this.getType(fae.lhs);

        Rvalue value;
        if (fae.fieldName.equals("length") && UnitCompiler.isArray(lhsType)) {
            value = new ArrayLength(
                fae.getLocation(),
                this.toRvalueOrCompileException(fae.lhs)
            );
        } else {
            IClass.IField iField = this.findIField(UnitCompiler.rawTypeOf(lhsType), fae.fieldName, fae.getLocation());
            if (iField == null) {
                this.compileError(
                    "\"" + this.getType(fae.lhs).toString() + "\" has no field \"" + fae.fieldName + "\"",
                    fae.getLocation()
                );
                value = new Rvalue(fae.getLocation()) {

                    @Override @Nullable public <R, EX extends Throwable> R
                    accept(RvalueVisitor<R, EX> visitor) { return null; }

                    @Override public String
                    toString() { return "???"; }
                };
            } else {
                value = new FieldAccess(
                    fae.getLocation(),
                    fae.lhs,
                    iField
                );
            }
        }

        value.setEnclosingScope(fae.getEnclosingScope());

        return (fae.value = value);
    }

    /**
     * "super.fld", "Type.super.fld"
     */
    private Rvalue
    determineValue(SuperclassFieldAccessExpression scfae) throws CompileException {
        if (scfae.value != null) return scfae.value;

        Rvalue lhs;
        {
            Rvalue receiver = scfae.qualification == null ? new ThisReference(scfae.getLocation())
                : new QualifiedThisReference(scfae.getLocation(), scfae.qualification);
            receiver.setEnclosingScope(scfae.getEnclosingScope());
            IType type = this.getType(receiver);
            IClass superclass = UnitCompiler.rawTypeOf(type).getSuperclass();
            if (superclass == null) {
                throw new CompileException("Cannot use \"super\" on \"" + type + "\"", scfae.getLocation());
            }
            IType view = this.asSupertype(type, superclass);
            lhs = new Cast(scfae.getLocation(), new SimpleType(scfae.getLocation(), view == null ? superclass : view), receiver);
        }

        Rvalue value;

        IClass.IField iField = this.findIField(UnitCompiler.rawTypeOf(this.getType(lhs)), scfae.fieldName, scfae.getLocation());
        if (iField == null) {
            this.compileError("Class has no field \"" + scfae.fieldName + "\"", scfae.getLocation());
            value = new Rvalue(scfae.getLocation()) {

                @Override @Nullable public <R, EX extends Throwable> R
                accept(RvalueVisitor<R, EX> visitor) { return null; }

                @Override public String
                toString() { return "???"; }
            };
        } else {
            value = new FieldAccess(
                scfae.getLocation(),
                lhs,
                iField
            );
        }
        value.setEnclosingScope(scfae.getEnclosingScope());

        return (scfae.value = value);
    }

    /**
     * Finds methods of the <var>mi</var>{@code .}{@link MethodInvocation#target
     * target} named <var>mi</var>{@code .}{@link Invocation#methodName methodName},
     * examines the argument types and chooses the most specific method. Checks that only the
     * allowed exceptions are thrown.
     * <p>
     *   Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     * </p>
     *
     * @return The selected {@link IClass.IMethod}
     */
    public IClass.IMethod
    findIMethod(MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod;
        FIND_METHOD: {

            Atom ot = mi.target;
            if (ot == null) {

                // Method invocation by simple method name... method must be declared by an enclosing type declaration.
                for (Scope s = mi.getEnclosingScope(); !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
                    if (s instanceof TypeDeclaration) {
                        TypeDeclaration td = (TypeDeclaration) s;

                        // Find methods with specified name.
                        iMethod = this.findIMethod(
                            this.resolve(td),  // targetType
                            mi                 // invocation
                        );
                        if (iMethod != null) break FIND_METHOD;
                    }
                }
            } else
            {

                // Method invocation by "target": "expr.meth(arguments)" -- method must be declared by the target's
                // type.
                iMethod = this.findIMethod(
                    this.getType(ot), // targetType
                    mi                // invocable
                );
                if (iMethod != null) break FIND_METHOD;
            }

            // Static method declared through single static import?
            {
                IMethod[] candidates = (IMethod[]) Iterables.toArray(
                    Iterables.filterByClass(this.importSingleStatic(mi.methodName), IMethod.class),
                    IMethod.class
                );

                if (candidates.length > 0) {
                    iMethod = (IMethod) this.findMostSpecificIInvocable(
                        mi,
                        candidates,
                        mi.arguments,
                        mi.getEnclosingScope()
                    );
                    break FIND_METHOD;
                }
            }

            // Static method declared through static-import-on-demand?
            {
                IMethod[] candidates = (IMethod[]) Iterables.toArray(
                    Iterables.filterByClass(this.importStaticOnDemand(mi.methodName), IMethod.class),
                    IMethod.class
                );

                if (candidates.length > 0) {
                    iMethod = (IMethod) this.findMostSpecificIInvocable(
                        mi,
                        candidates,
                        mi.arguments,
                        mi.getEnclosingScope()
                    );
                    break FIND_METHOD;
                }
            }

            this.compileError((
                "A method named \""
                + mi.methodName
                + "\" is not declared in any enclosing class nor any supertype, nor through a static import"
            ), mi.getLocation());
            return this.fakeIMethod(this.iClassLoader.TYPE_java_lang_Object, mi.methodName, mi.arguments);
        }

        assert iMethod != null; // Don't know why JAVAC thinks "iMethod" could be null here!?

        mi.argumentsNeedAdjust = iMethod.isVarargs() && iMethod.argsNeedAdjust();
        this.checkThrownExceptions(mi, iMethod);
        return iMethod;
    }

    /**
     * Finds a {@link IClass.IMethod} in the given <var>targetType</var>, its superclasses or superinterfaces which is
     * applicable with the given <var>invocation</var>. If more than one such method exists, chooses the most
     * specific one (JLS7 15.11.2).
     *
     * @return {@code null} if no appropriate method could be found
     */
    private void getTypeMethods(IType target, String name, List<IMethod> methods) throws CompileException {
        if (target instanceof IWildcardType) {
            this.getTypeMethods(((IWildcardType) target).getUpperBound(), name, methods);
            return;
        }
        IClass raw = UnitCompiler.rawTypeOf(target);
        this.getIMethods(raw, name, methods);
        if (target instanceof CapturedType) {
            for (IType bound : ((CapturedType) target).bounds) this.getTypeMethods(bound, name, methods);
        } else if (target instanceof SourceTypeVariable) {
            ReferenceType[] bounds = ((SourceTypeVariable) target).declaration.bound;
            if (bounds != null) for (int i = 1; i < bounds.length; i++) this.getIMethods(UnitCompiler.rawTypeOf(this.sourceBoundType(bounds[i])), name, methods);
        }
        Set<IMethod> unique = new LinkedHashSet<>(methods);
        methods.clear(); methods.addAll(unique);
    }

    @Nullable private IClass.IMethod
    findIMethod(IType targetType, Invocation invocation) throws CompileException {

        IClass rawTargetType = UnitCompiler.rawTypeOf(targetType);

        // Get all methods.
        List<IClass.IMethod> ms = new ArrayList<>();
        this.getTypeMethods(targetType, invocation.methodName, ms);

        // Interfaces inherit the methods declared in 'Object'.
        if (rawTargetType.isInterface()) {
            IClass.IMethod[] oms = this.iClassLoader.TYPE_java_lang_Object.getDeclaredIMethods(invocation.methodName);
            for (IMethod om : oms) {
                if (!om.isStatic() && om.getAccess() == Access.PUBLIC && !ms.contains(om)) ms.add(om);
            }
        }

        if (ms.size() == 0) return null;

        // Determine arguments' types, choose the most specific method.
        return (IClass.IMethod) this.findMostSpecificIInvocable(
            invocation,                                                   // locatable
            (IClass.IMethod[]) ms.toArray(new IClass.IMethod[ms.size()]), // iInvocables
            invocation.arguments,                                         // arguments
            invocation.getEnclosingScope()                                // contextScope
        );
    }

    private IMethod
    fakeIMethod(IClass targetType, final String name, Rvalue[] arguments) throws CompileException {

        final IClass[] pts = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            Rvalue arg = arguments[i];
            pts[i] = this.isPolyExpression(arg) ? null : UnitCompiler.rawTypeOf(this.getType(arg));
        }

        return targetType.new IMethod() {
            @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
            @Override public Access        getAccess()            { return Access.PUBLIC;      }
            @Override public boolean       isStatic()             { return false;              }
            @Override public boolean       isAbstract()           { return false;              }
            @Override public IClass        getReturnType()        { return IClass.INT;         }
            @Override public String        getName()              { return name;               }
            @Override public IClass[]      getParameterTypes2()   { return pts;                }
            @Override public boolean       isVarargs()            { return false;              }
            @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
        };
    }

    /**
     * Adds all methods with the given <var>methodName</var> that are declared by the <var>type</var>, its superclasses
     * and all their superinterfaces to the result list <var>v</var>.
     */
    public void
    getIMethods(IClass type, String methodName, List<IMethod> v) throws CompileException {
        this.getIMethods(type, methodName, v, true);
    }

    private void getIMethods(IClass type, String methodName, List<IMethod> v, boolean includeStatic) throws CompileException {

        // Check methods declared by this type.
        {
            IClass.IMethod[] ims = type.getDeclaredIMethods(methodName);
            for (IMethod im : ims) if (includeStatic || !im.isStatic()) v.add(im);
        }

        // Private methods are not inherited; package methods stop being inherited at a package boundary.
        int inheritedStart = v.size();
        IClass superclass = type.getSuperclass();
        if (superclass != null) this.getIMethods(superclass, methodName, v, includeStatic);

        // Check superinterfaces.
        IClass[] interfaces = type.getInterfaces();
        for (IClass interfacE : interfaces) this.getIMethods(interfacE, methodName, v, false);
        for (int i = v.size() - 1; i >= inheritedStart; i--) {
            IMethod method = v.get(i);
            if (method.getAccess() == Access.PRIVATE || method.getAccess() == Access.DEFAULT
                && !String.valueOf(Descriptor.getPackageName(type.getDescriptor())).equals(
                    String.valueOf(Descriptor.getPackageName(method.getDeclaringIClass().getDescriptor())))) v.remove(i);
        }
    }

    /**
     * @return The {@link IClass.IMethod} that implements the <var>superclassMethodInvocation</var>
     */
    public IClass.IMethod
    findIMethod(SuperclassMethodInvocation superclassMethodInvocation) throws CompileException {
        if (superclassMethodInvocation.iMethod != null) return superclassMethodInvocation.iMethod;
        this.resolveSuperReceiver(superclassMethodInvocation);
        IClass superclass = superclassMethodInvocation.invocationType;

        IMethod iMethod = this.findIMethod(
            superclass,                // targetType
            superclassMethodInvocation // invocation
        );
        if (iMethod == null) {
            this.compileError(
                "Class \"" + superclass + "\" has no method named \"" + superclassMethodInvocation.methodName + "\"",
                superclassMethodInvocation.getLocation()
            );
            return this.fakeIMethod(
                superclass,
                superclassMethodInvocation.methodName,
                superclassMethodInvocation.arguments
            );
        }
        if (iMethod.isStatic() || iMethod.isAbstract()) {
            throw UnitCompiler.compileException(superclassMethodInvocation, "Super call requires a concrete instance method");
        }
        if (!this.isAccessible(iMethod, superclassMethodInvocation.getEnclosingScope(), superclass)) {
            this.checkAccessible(iMethod, superclassMethodInvocation.getEnclosingScope(), superclassMethodInvocation.getLocation());
        }
        this.checkThrownExceptions(superclassMethodInvocation, iMethod);
        superclassMethodInvocation.argumentsNeedAdjust = iMethod.isVarargs() && iMethod.argsNeedAdjust();
        return superclassMethodInvocation.iMethod = iMethod;
    }

    private void resolveSuperReceiver(SuperclassMethodInvocation superclassMethodInvocation) throws CompileException {
        IClass qualification = null;
        if (superclassMethodInvocation.qualification != null) {
            UnitCompiler.setScopeIfUnset(superclassMethodInvocation.qualification, superclassMethodInvocation.getEnclosingScope());
            qualification = this.getRawType(superclassMethodInvocation.qualification);
        }
        AbstractTypeDeclaration declaringClass;
        for (Scope s = superclassMethodInvocation.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) s;
                if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isStatic()) {
                    this.compileError(
                        "Superclass method cannot be invoked in static context",
                        superclassMethodInvocation.getLocation()
                    );
                }
            }
            if (s instanceof AbstractTypeDeclaration && (qualification == null || qualification.isInterface()
                || this.resolve((AbstractTypeDeclaration) s) == qualification)) {
                declaringClass = (AbstractTypeDeclaration) s;
                break;
            }
            if (s instanceof CompilationUnit) throw UnitCompiler.compileException(superclassMethodInvocation, "Invalid super qualifier");
        }

        IClass receiver = this.resolve(declaringClass);
        IClass superclass = receiver.getSuperclass();
        if (qualification != null && qualification.isInterface()) {
            boolean direct = false;
            for (IClass interfaze : receiver.getInterfaces()) {
                if (interfaze == qualification) direct = true;
                else if (qualification.isAssignableFrom(interfaze)) {
                    throw UnitCompiler.compileException(superclassMethodInvocation, "Redundant superinterface qualifier");
                }
            }
            if (!direct) throw UnitCompiler.compileException(superclassMethodInvocation, "Super qualifier must be a direct superinterface");
            superclass = qualification;
        }
        if (superclass == null) {
            throw new CompileException(
                "\"" + declaringClass + "\" has no superclass",
                superclassMethodInvocation.getLocation()
            );
        }
        superclassMethodInvocation.receiverType = receiver;
        superclassMethodInvocation.invocationType = superclass;

    }
    private SuperclassMethodInvocation superReferenceReceiver(MethodReference reference) throws CompileException {
        SuperclassMethodInvocation invocation = new SuperclassMethodInvocation(reference.getLocation(), reference.superQualification,
            reference.methodName, new Rvalue[0]);
        invocation.setEnclosingScope(reference.getEnclosingScope());
        this.resolveSuperReceiver(invocation);
        return invocation;
    }

    /** Whether overload resolution can use this reference before choosing its functional target. */
    private boolean isExactReference(MethodReference reference, IType target) throws CompileException {
        IMethod sam = this.getSam(reference, target);
        int arity = sam.getParameterTypes().length;
        if (reference.lhs instanceof Rvalue) UnitCompiler.setRvalueScopeIfUnset((Rvalue) reference.lhs, reference.getEnclosingScope());
        Atom lhs = reference.lhs instanceof AmbiguousName ? this.reclassify((AmbiguousName) reference.lhs) : reference.lhs;
        Type type = lhs.toType();
        if (type != null) UnitCompiler.setScopeIfUnset(type, reference.getEnclosingScope());
        IType receiverType = type == null ? this.getType(lhs) : this.getType(type);
        if (reference.superReference) receiverType = this.superReferenceReceiver(reference).invocationType;
        IClass receiver = UnitCompiler.rawTypeOf(receiverType);
        if (type != null && !(receiverType instanceof ParameterizedIClass) && receiver.getITypeVariables().length != 0) return false;
        List<IMethod> methods = new ArrayList<>();
        this.getIMethods(receiver, reference.methodName, methods);
        IMethod found = null;
        for (IMethod method : methods) {
            if (!this.isAccessible(method, reference.getEnclosingScope()) || type == null && method.isStatic()) continue;
            // Exactness depends on all accessible members with this name, including other arities.
            if (method.isVarargs()) return false;
            Set<String> variables = this.getMethodTypeParameterNames(method);
            if (variables != null && !variables.isEmpty() && (reference.typeArguments == null || reference.typeArguments.length != variables.size())) return false;
            if (found != null && !Arrays.equals(found.getParameterTypes(), method.getParameterTypes())) return false;
            found = method;
        }
        return found != null && found.getParameterTypes().length + (type != null && !found.isStatic() ? 1 : 0) == arity;
    }

    @Nullable private List<Rvalue> lambdaResults(final LambdaExpression lambda, final IType[] inputs) throws CompileException {
        final List<Rvalue> originals = new ArrayList<>();
        if (lambda.body instanceof ExpressionLambdaBody) {
            originals.add(((ExpressionLambdaBody) lambda.body).expression);
        } else {
            new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
                @Override public void traverseBinaryOperation(Java.BinaryOperation operation) throws CompileException {
                    this.traverseBinaryOperationIteratively(operation);
                }

                @Override public void traverseReturnStatement(ReturnStatement statement) {
                    if (statement.returnValue != null) originals.add(statement.returnValue);
                }
                @Override public void traverseLambdaExpression(LambdaExpression expression) {}
                @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
                @Override public void traverseLocalClassDeclaration(Java.LocalClassDeclaration declaration) {}
            }.traverseBlock(((Java.BlockLambdaBody) lambda.body).block);
        }
        List<Rvalue> results = new ArrayList<>();
        for (Rvalue original : originals) {
            Rvalue result = this.copyLambdaResult(lambda, inputs, original);
            if (result == null) return null;
            results.add(result);
        }
        return results;
    }

    @Nullable private Rvalue lambdaResult(final LambdaExpression lambda, IType[] inputs) throws CompileException {
        List<Rvalue> results = this.lambdaResults(lambda, inputs);
        return results != null && results.size() == 1 ? results.get(0) : null;
    }

    @Nullable private List<IType> lambdaProbeExceptions;

    /** Exceptions escaping the body, excluding nested functional bodies and catches inside this lambda. */
    private List<IType> lambdaExceptions(final LambdaExpression lambda, final IType[] inputs) throws CompileException {
        return this.lambdaExceptions(lambda, inputs, null);
    }

    private List<IType> lambdaExceptions(final LambdaExpression lambda, final IType[] inputs, @Nullable final TryStatement protectedTry)
    throws CompileException {
        final List<IType> result = new ArrayList<>();
        org.codehaus.janino.util.AbstractTraverser<CompileException> visitor = new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
            @Override public void traverseBinaryOperation(Java.BinaryOperation operation) throws CompileException {
                this.traverseBinaryOperationIteratively(operation);
            }

            private void expression(Rvalue original, boolean thrown) throws CompileException {
                List<IType> previous = UnitCompiler.this.lambdaProbeExceptions;
                List<IType> exceptions = new ArrayList<>();
                UnitCompiler.this.lambdaProbeExceptions = exceptions;
                try {
                    Rvalue copy = UnitCompiler.this.copyLambdaResult(lambda, inputs, original);
                    if (copy == null) return;
                    IType type = UnitCompiler.this.getType(copy);
                    if (thrown && type != IClass.NULL) exceptions.add(type);
                    if (copy instanceof NewClassInstance) {
                        NewClassInstance creation = (NewClassInstance) copy;
                        IClass raw = UnitCompiler.rawTypeOf(type);
                        IConstructor constructor = (IConstructor) UnitCompiler.this.findMostSpecificIInvocable(
                            creation, raw.getDeclaredIConstructors(), creation.arguments, creation.getEnclosingScope());
                        InferenceContext inference = UnitCompiler.this.constructorInference(
                            creation, constructor, creation.arguments, creation.getEnclosingScope());
                        for (IClass exception : UnitCompiler.this.instantiatedExceptions(constructor, inference.values)) exceptions.add(exception);
                    }
                } finally { UnitCompiler.this.lambdaProbeExceptions = previous; }
                for (IType exception : exceptions) this.add(lambda.body instanceof ExpressionLambdaBody
                    ? lambda.getEnclosingScope() : original.getEnclosingScope(), exception);
            }

            private void add(Scope scope, IType exception) throws CompileException {
                IClass raw = UnitCompiler.rawTypeOf(exception);
                if (UnitCompiler.this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(raw)
                    || UnitCompiler.this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(raw)) return;
                Scope child = null;
                for (; scope != null && scope != lambda.getEnclosingScope(); child = scope, scope = scope.getEnclosingScope()) {
                    if (lambda.body instanceof BlockLambdaBody && scope == ((BlockLambdaBody) lambda.body).block) break;
                    if (scope == protectedTry) break;
                    if (!(scope instanceof TryStatement)) continue;
                    TryStatement statement = (TryStatement) scope;
                    if (statement.finallY != null && child != statement.finallY
                        && !UnitCompiler.this.lambdaCanCompleteNormally(statement.finallY)) return;
                    if (child != null && child != statement.body) continue;
                    for (CatchClause caught : statement.catchClauses) for (Type type : caught.catchParameter.types) {
                        Type copy = new org.codehaus.janino.util.DeepCopier().copyType(type);
                        copy.setEnclosingScope(lambda.getEnclosingScope());
                        if (UnitCompiler.this.getRawType(copy).isAssignableFrom(raw)) return;
                    }
                }
                if (!result.contains(exception)) result.add(exception);
            }

            @Override public void traverseMethodInvocation(MethodInvocation invocation) throws CompileException {
                // Attributing the whole invocation also checks its receiver and arguments with
                // their contextual targets. Revisiting an argument alone loses those targets.
                this.expression(invocation, false);
            }
            @Override public void traverseSuperclassMethodInvocation(SuperclassMethodInvocation invocation) throws CompileException {
                this.expression(invocation, false);
            }
            @Override public void traverseNewClassInstance(NewClassInstance creation) throws CompileException {
                this.expression(creation, false);
            }
            @Override public void traverseThrowStatement(ThrowStatement statement) throws CompileException {
                Rvalue expression = statement.expression;
                while (expression instanceof ParenthesizedExpression) expression = ((ParenthesizedExpression) expression).value;
                if (expression instanceof AmbiguousName && ((AmbiguousName) expression).identifiers.length == 1) {
                    final String name = ((AmbiguousName) expression).identifiers[0];
                    for (Scope scope = statement.getEnclosingScope(); !(scope instanceof TypeDeclaration);) {
                        if (scope instanceof CatchClause && ((CatchClause) scope).catchParameter.name.equals(name)) {
                            CatchClause caught = (CatchClause) scope;
                            final boolean[] assigned = { false };
                            new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
                                @Override public void traverseAssignment(Assignment assignment) throws CompileException {
                                    if (assignment.lhs instanceof AmbiguousName && ((AmbiguousName) assignment.lhs).identifiers.length == 1
                                        && ((AmbiguousName) assignment.lhs).identifiers[0].equals(name)) assigned[0] = true;
                                    super.traverseAssignment(assignment);
                                }
                                @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
                                @Override public void traverseLocalClassDeclaration(LocalClassDeclaration declaration) {}
                            }.visitBlockStatement(caught.body);
                            if (!assigned[0]) {
                                TryStatement attempt = (TryStatement) caught.getEnclosingScope();
                                for (IType exception : UnitCompiler.this.lambdaExceptions(lambda, inputs, attempt)) {
                                    IClass raw = UnitCompiler.rawTypeOf(exception);
                                    boolean handled = false;
                                    for (CatchClause handler : attempt.catchClauses) {
                                        for (Type type : handler.catchParameter.types) {
                                            Type copy = new org.codehaus.janino.util.DeepCopier().copyType(type);
                                            copy.setEnclosingScope(lambda.getEnclosingScope());
                                            IClass limit = UnitCompiler.this.getRawType(copy);
                                            if (limit.isAssignableFrom(raw) || raw.isAssignableFrom(limit)) {
                                                if (handler == caught) this.add(statement.getEnclosingScope(), limit.isAssignableFrom(raw) ? exception : limit);
                                                if (limit.isAssignableFrom(raw)) handled = true;
                                            }
                                        }
                                        if (handler == caught || handled) break;
                                    }
                                }
                                return;
                            }
                            break;
                        }
                        if (lambda.body instanceof BlockLambdaBody && scope == ((BlockLambdaBody) lambda.body).block) break;
                        scope = UnitCompiler.this.lambdaProbeParents.containsKey(scope)
                            ? UnitCompiler.this.lambdaProbeParents.get(scope) : scope.getEnclosingScope();
                        if (scope == null) break;
                    }
                }
                this.expression(statement.expression, true); super.traverseThrowStatement(statement);
            }
            @Override public void traverseTryStatement(TryStatement statement) throws CompileException {
                for (TryStatement.Resource resource : statement.resources) if (resource instanceof TryStatement.LocalVariableDeclaratorResource) {
                    Type type = new org.codehaus.janino.util.DeepCopier().copyType(((TryStatement.LocalVariableDeclaratorResource) resource).type);
                    type.setEnclosingScope(lambda.getEnclosingScope());
                    IType receiver = UnitCompiler.this.getType(type);
                    List<IMethod> methods = new ArrayList<>();
                    UnitCompiler.this.getTypeMethods(receiver, "close", methods);
                    for (IMethod method : methods) if (method.getParameterTypes().length == 0 && !method.isStatic()) {
                        for (IClass exception : UnitCompiler.this.instantiatedExceptions(method,
                            UnitCompiler.this.receiverEnvironment(receiver, method.getDeclaringIClass()))) this.add(statement, exception);
                        break;
                    }
                }
                if (statement == protectedTry) this.visitBlockStatement(statement.body);
                else super.traverseTryStatement(statement);
            }
            @Override public void traverseLambdaExpression(LambdaExpression nested) {}
            @Override public void traverseMethodReference(MethodReference reference) throws CompileException {
                // Referencing a method does not invoke it, but evaluating a bound receiver can throw.
                if (reference.lhs instanceof Rvalue) this.dispatchRvalue((Rvalue) reference.lhs);
            }
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
            @Override public void traverseLocalClassDeclaration(Java.LocalClassDeclaration declaration) {}
        };
        if (protectedTry != null) visitor.traverseTryStatement(protectedTry);
        else if (lambda.body instanceof BlockLambdaBody) visitor.traverseBlock(((BlockLambdaBody) lambda.body).block);
        else visitor.dispatchRvalue(((ExpressionLambdaBody) lambda.body).expression);
        return result;
    }

    private boolean lambdaCanCompleteNormally(BlockStatement statement) throws CompileException {
        return this.statementCompiler.canCompleteNormallyForLambda(statement);
    }

    private final Map<Scope, Scope> lambdaProbeParents = new java.util.IdentityHashMap<>();

    private void rememberLambdaParents(final LambdaExpression root) throws CompileException {
        new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
            @Override public void traverseLambdaExpression(LambdaExpression expression) throws CompileException {
                if (expression.body instanceof BlockLambdaBody) {
                    Block block = ((BlockLambdaBody) expression.body).block;
                    UnitCompiler.this.lambdaProbeParents.put(block, expression.getEnclosingScopeOrNull());
                    this.traverseBlock(block);
                } else this.dispatchRvalue(((ExpressionLambdaBody) expression.body).expression);
            }
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
            @Override public void traverseLocalClassDeclaration(LocalClassDeclaration declaration) {}
        }.traverseLambdaExpression(root);
    }

    @Nullable private Rvalue copyLambdaResult(final LambdaExpression lambda, IType[] inputs, final Rvalue original) throws CompileException {
        // Lambda blocks have no Statement parent before lowering; remember their lexical
        // expression scopes so nested probes can reach locals in enclosing lambda bodies.
        this.rememberLambdaParents(lambda);
        Object lambdaOrigin = this.captureOrigins.get(lambda);
        if (lambdaOrigin instanceof LambdaExpression) this.rememberLambdaParents((LambdaExpression) lambdaOrigin);
        String[] names;
        if (lambda.parameters instanceof FormalLambdaParameters) {
            FormalParameter[] parameters = ((FormalLambdaParameters) lambda.parameters).formalParameters.parameters;
            names = new String[parameters.length];
            for (int i = 0; i < names.length; i++) names[i] = parameters[i].name;
        } else if (lambda.parameters instanceof IdentifierLambdaParameters) {
            names = new String[] { ((IdentifierLambdaParameters) lambda.parameters).identifier };
        } else names = ((InferredLambdaParameters) lambda.parameters).names;
        if (names.length != inputs.length) throw UnitCompiler.compileException(lambda, "Wrong lambda arity");
        final Map<String, LocalVariable> parameters = new HashMap<>();
        for (int i = 0; i < names.length; i++) parameters.put(names[i], new LocalVariable(true, inputs[i]));
        if (this.isPolyExpression(original)) return null;
        Rvalue result = new org.codehaus.janino.util.DeepCopier() {
            @Override public Lvalue copyLocalVariableAccess(LocalVariableAccess subject) {
                return new LocalVariableAccess(subject.getLocation(), subject.localVariable);
            }
            @Override public Rvalue copyRvalue(Rvalue subject) throws CompileException {
                Rvalue copy = super.copyRvalue(subject);
                Object origin = UnitCompiler.this.captureOrigins.get(subject);
                UnitCompiler.this.captureOrigins.put(copy, origin == null ? subject : origin);
                return copy;
            }
            @Override public Rvalue copyLambdaExpression(LambdaExpression subject) throws CompileException {
                Map<String, LocalVariable> shadowed = new HashMap<>();
                List<String> names = new ArrayList<>();
                if (subject.parameters instanceof FormalLambdaParameters) {
                    for (FormalParameter p : ((FormalLambdaParameters) subject.parameters).formalParameters.parameters) names.add(p.name);
                } else if (subject.parameters instanceof IdentifierLambdaParameters) names.add(((IdentifierLambdaParameters) subject.parameters).identifier);
                else names.addAll(Arrays.asList(((InferredLambdaParameters) subject.parameters).names));
                for (String name : names) if (parameters.containsKey(name)) shadowed.put(name, parameters.remove(name));
                try { return super.copyLambdaExpression(subject); }
                finally { parameters.putAll(shadowed); }
            }
            @Override public Lvalue copyAmbiguousName(AmbiguousName name) throws CompileException {
                LocalVariable parameter = parameters.get(name.identifiers[0]);
                if (parameter == null && lambda.body instanceof Java.BlockLambdaBody) {
                    Object origin = UnitCompiler.this.captureOrigins.get(name);
                    Scope scope = origin instanceof Rvalue ? ((Rvalue) origin).getEnclosingScopeOrNull() : name.getEnclosingScopeOrNull();
                    // A prior probe's copy may only be scoped to the extracted expression;
                    // its source origin still knows the block declaring captured locals.
                    if (scope == null) scope = original.getEnclosingScopeOrNull();
                    if (scope == null) scope = ((BlockLambdaBody) lambda.body).block;
                    while (scope != null && !(scope instanceof CompilationUnit) && scope != lambda.getEnclosingScope()) {
                        if (scope instanceof TryStatement) {
                            for (TryStatement.Resource resource : ((TryStatement) scope).resources) {
                                if (!(resource instanceof TryStatement.LocalVariableDeclaratorResource)) continue;
                                TryStatement.LocalVariableDeclaratorResource declaration = (TryStatement.LocalVariableDeclaratorResource) resource;
                                if (declaration.variableDeclarator.name.equals(name.identifiers[0])) parameter = new LocalVariable(true,
                                    UnitCompiler.this.lambdaLocalType(declaration.type, declaration.variableDeclarator.brackets, lambda));
                            }
                        }
                        if (scope instanceof Java.ForStatement && ((Java.ForStatement) scope).init instanceof LocalVariableDeclarationStatement) {
                            LocalVariableDeclarationStatement declaration = (LocalVariableDeclarationStatement) ((Java.ForStatement) scope).init;
                            for (VariableDeclarator variable : declaration.variableDeclarators) if (variable.name.equals(name.identifiers[0])) {
                                parameter = new LocalVariable(true, UnitCompiler.this.lambdaLocalType(declaration.type, variable.brackets, lambda));
                            }
                        }
                        if (scope instanceof Java.ForEachStatement) {
                            FormalParameter declaration = ((Java.ForEachStatement) scope).currentElement;
                            if (declaration.name.equals(name.identifiers[0])) {
                                parameter = new LocalVariable(true, UnitCompiler.this.lambdaLocalType(declaration.type, 0, lambda));
                            }
                        }
                        if (scope instanceof CatchClause) {
                            CatchParameter caught = ((CatchClause) scope).catchParameter;
                            if (caught.name.equals(name.identifiers[0])) {
                                IType type = null;
                                for (Type alternative : caught.types) {
                                    Type copy = new org.codehaus.janino.util.DeepCopier().copyType(alternative);
                                    copy.setEnclosingScope(lambda.getEnclosingScope());
                                    IType bound = UnitCompiler.this.getType(copy);
                                    type = type == null ? bound : UnitCompiler.this.commonSupertype(type, bound);
                                }
                                if (type != null) parameter = new LocalVariable(true, type);
                            }
                        }
                        if (scope instanceof Block) {
                            for (BlockStatement statement : ((Block) scope).statements) {
                                if (statement instanceof LocalVariableDeclarationStatement) {
                                    LocalVariableDeclarationStatement declaration = (LocalVariableDeclarationStatement) statement;
                                    for (VariableDeclarator variable : declaration.variableDeclarators) {
                                        if (variable.name.equals(name.identifiers[0])) {
                                            Type declared = new org.codehaus.janino.util.DeepCopier().copyType(declaration.type);
                                            declared.setEnclosingScope(lambda.getEnclosingScope());
                                            IType type = UnitCompiler.this.getType(declared);
                                            for (int i = 0; i < variable.brackets; i++) type = UnitCompiler.this.iClassLoader.getArrayIClass(UnitCompiler.rawTypeOf(type));
                                            parameter = new LocalVariable(true, type);
                                        }
                                    }
                                }
                            }
                            if (parameter != null) break;
                        }
                        if (parameter != null) break;
                        if (scope == ((Java.BlockLambdaBody) lambda.body).block) break;
                        scope = UnitCompiler.this.lambdaProbeParents.containsKey(scope)
                            ? UnitCompiler.this.lambdaProbeParents.get(scope) : scope.getEnclosingScope();
                    }
                }
                if (parameter == null) return super.copyAmbiguousName(name);
                Lvalue value = new LocalVariableAccess(name.getLocation(), parameter);
                for (int i = 1; i < name.n; i++) value = new FieldAccessExpression(name.getLocation(), value, name.identifiers[i]);
                return value;
            }
        }.copyRvalue(original);
        UnitCompiler.setRvalueScopeIfUnset(result, lambda.getEnclosingScope());
        return result;
    }

    private IType lambdaLocalType(Type original, int brackets, LambdaExpression lambda) throws CompileException {
        Type copy = new org.codehaus.janino.util.DeepCopier().copyType(original);
        copy.setEnclosingScope(lambda.getEnclosingScope());
        IType type = this.getType(copy);
        for (int i = 0; i < brackets; i++) type = this.arrayType(type);
        return type;
    }

    private boolean lambdaCompatible(LambdaExpression lambda, IType target) throws CompileException {
        List<IType> previous = this.lambdaProbeExceptions;
        this.lambdaProbeExceptions = new ArrayList<>();
        try { return this.lambdaCompatibleProbe(lambda, target); }
        finally { this.lambdaProbeExceptions = previous; }
    }

    private boolean lambdaCompatibleProbe(LambdaExpression lambda, IType target) throws CompileException {
        IMethod sam = this.getSam(lambda, target);
        IType[] inputs = this.functionalParameters(target, sam);
        if (lambda.parameters instanceof FormalLambdaParameters) {
            FormalParameter[] parameters = ((FormalLambdaParameters) lambda.parameters).formalParameters.parameters;
            if (parameters.length != inputs.length) return false;
            for (int i = 0; i < inputs.length; i++) {
                UnitCompiler.setScopeIfUnset(parameters[i].type, lambda.getEnclosingScope());
                if (!this.getType(parameters[i].type).equals(inputs[i])) return false;
            }
        } else if (lambda.parameters instanceof IdentifierLambdaParameters) {
            if (inputs.length != 1) return false;
        } else if (((InferredLambdaParameters) lambda.parameters).names.length != inputs.length) return false;
        IType output = this.functionalReturn(target, sam);
        if (lambda.body instanceof BlockLambdaBody
            && !this.statementCompiler.lambdaBlockCompatible(((BlockLambdaBody) lambda.body).block, output == IClass.VOID)) return false;
        List<Rvalue> results = this.lambdaResults(lambda, inputs);
        if (results == null) return true; // Nested poly expressions are checked with the selected target.
        if (!(lambda.body instanceof ExpressionLambdaBody) && output == IClass.VOID) return results.isEmpty();
        for (Rvalue result : results) {
            if (!this.lambdaResultCompatible(result, output)) return false;
        }
        return true;
    }

    private boolean lambdaResultCompatible(Rvalue result, IType output) throws CompileException {
        if (output == IClass.VOID) {
            return result instanceof MethodInvocation || result instanceof SuperclassMethodInvocation
                || result instanceof Assignment || result instanceof Crement || result instanceof NewClassInstance
                || result instanceof NewAnonymousClassInstance;
        }
        IType actual = this.invocationArgumentType(result, output);
        if (this.genericConvertible(actual, output, true)) return true;
        IType unboxed = this.getUnboxedType(output);
        Object constant = this.constantEvaluator.getConstantValue(result);
        return UnitCompiler.isPrimitive(actual) && constant != UnitCompiler.NOT_CONSTANT
            && this.constantAssignmentConversion(result, constant, unboxed) != null;
    }

    private boolean functionalOverloadMoreSpecific(
        IInvocable a, IInvocable b, Rvalue[] arguments, Map<IInvocable, IType[]> targets
    ) throws CompileException {
        List<IType> previous = this.lambdaProbeExceptions;
        this.lambdaProbeExceptions = new ArrayList<>();
        try { return this.functionalOverloadMoreSpecificProbe(a, b, arguments, targets); }
        finally { this.lambdaProbeExceptions = previous; }
    }

    private boolean functionalOverloadMoreSpecificProbe(
        IInvocable a, IInvocable b, Rvalue[] arguments, Map<IInvocable, IType[]> targets
    ) throws CompileException {
        if (a.isVarargs() || b.isVarargs()) return false;
        IType[] ap = targets.get(a), bp = targets.get(b);
        if (ap.length != arguments.length || bp.length != arguments.length) return false;
        boolean functional = false;
        for (int i = 0; i < arguments.length; i++) {
            if (ap[i].equals(bp[i])) continue;
            Rvalue argument = arguments[i];
            while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
            boolean reference = argument instanceof MethodReference;
            if (reference) {
                if (!this.isExactReference((MethodReference) argument, ap[i]) || !this.isExactReference((MethodReference) argument, bp[i])) return false;
            } else if (!(argument instanceof LambdaExpression) || !(((LambdaExpression) argument).parameters instanceof FormalLambdaParameters)) return false;
            IMethod as = this.getSam(argument, ap[i]), bs = this.getSam(argument, bp[i]);
            IType[] ai = this.functionalParameters(ap[i], as), bi = this.functionalParameters(bp[i], bs);
            if (!Arrays.equals(ai, bi)) return false;
            List<Rvalue> lambdaResults = reference ? null : this.lambdaResults((LambdaExpression) argument, ai);
            if (!reference && lambdaResults == null) return false;
            IType ar = this.functionalReturn(ap[i], as), br = this.functionalReturn(bp[i], bs);
            if (br == IClass.VOID) { functional = true; continue; }
            if (ar == IClass.VOID) return false;
            if (UnitCompiler.isPrimitive(ar) != UnitCompiler.isPrimitive(br)) {
                if (reference) {
                    if (UnitCompiler.isPrimitive(this.resolveReference((MethodReference) argument, ap[i]).result) != UnitCompiler.isPrimitive(ar)) return false;
                } else {
                    if (lambdaResults.isEmpty()) return false;
                    for (Rvalue result : lambdaResults) if (UnitCompiler.isPrimitive(this.getType(result)) != UnitCompiler.isPrimitive(ar)) return false;
                }
            } else if (!this.genericConvertible(ar, br, false)) return false;
            functional = true;
        }
        return functional;
    }

    /** Determines applicability and specificity, including candidate-local functional target types. */
    private IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        IInvocable[]       iInvocables,
        final Rvalue[]     arguments,
        Scope              contextScope
    ) throws CompileException {

        // Poly expressions require a functional interface; an exact reference also constrains applicability.
        List<IInvocable> candidates = new ArrayList<>();
        Map<IInvocable, IType[]> candidateTargets = new java.util.IdentityHashMap<>();
        NEXT_CANDIDATE:
        for (IInvocable candidate : new LinkedHashSet<IInvocable>(Arrays.asList(iInvocables))) {
            if (candidate instanceof IMethod) for (IInvocable other : iInvocables) {
                if (other instanceof IMethod && candidate.getDeclaringIClass() != other.getDeclaringIClass()
                    && candidate.getDeclaringIClass().isAssignableFrom(other.getDeclaringIClass())
                    && ((IMethod) candidate).isStatic() == ((IMethod) other).isStatic() && other.getAccess() != Access.PRIVATE
                    && Arrays.equals(candidate.getParameterTypes(), other.getParameterTypes())) continue NEXT_CANDIDATE;
            }
            IClass[] parameters = candidate.getParameterTypes();
            if (candidate.isVarargs() ? arguments.length < parameters.length - 1 : arguments.length != parameters.length) continue;
            IType[] targets = parameters.clone();
            if (locatable instanceof Invocation && candidate instanceof IMethod) {
                try { targets = this.invocationTargetTypes((Invocation) locatable, (IMethod) candidate, false); }
                catch (CompileException ex) {
                    continue;
                }
            }
            if (candidate instanceof IConstructor) {
                try { targets = this.constructorTargetTypes(locatable, (IConstructor) candidate, arguments, contextScope); }
                catch (CompileException ex) {
                    continue;
                }
            }
            for (int i = 0; i < arguments.length; i++) {
                if (!this.isPolyExpression(arguments[i])) continue;
                if (parameters.length == 0 || i >= parameters.length && !candidate.isVarargs()) continue NEXT_CANDIDATE;
                IType target = targets[Math.min(i, targets.length - 1)];
                if (candidate.isVarargs() && i >= targets.length - 1) target = UnitCompiler.getComponentType(target);
                IClass parameter = UnitCompiler.rawTypeOf(target);
                if (!parameter.isInterface()) continue NEXT_CANDIDATE;
                try { this.getSingleAbstractMethod(locatable, parameter); }
                catch (CompileException ex) { continue NEXT_CANDIDATE; }
                Rvalue argument = arguments[i];
                while (argument instanceof ParenthesizedExpression) argument = ((ParenthesizedExpression) argument).value;
                if (argument instanceof LambdaExpression) {
                    try { if (!this.lambdaCompatible((LambdaExpression) argument, target)) continue NEXT_CANDIDATE; }
                    catch (CompileException ex) { continue NEXT_CANDIDATE; }
                }
                if (argument instanceof MethodReference) {
                    try { this.resolveReference((MethodReference) argument, target); }
                    catch (CompileException ex) { continue NEXT_CANDIDATE; }
                }
                if (argument instanceof ClassInstanceCreationReference || argument instanceof ArrayCreationReference) {
                    try { this.checkCreationReference(argument, target); }
                    catch (CompileException ex) { continue NEXT_CANDIDATE; }
                }
            }
            candidates.add(candidate);
            candidateTargets.put(candidate, targets);
        }
        if (candidates.isEmpty()) throw UnitCompiler.compileException(locatable, "No compatible constructor/method");
        // Compare functional result specificity without changing ordinary strict/boxing/varargs phases.
        List<IInvocable> maximal = new ArrayList<>();
        for (IInvocable candidate : candidates) {
            boolean dominated = false;
            for (IInvocable other : candidates) {
                if (candidate != other && this.functionalOverloadMoreSpecific(other, candidate, arguments, candidateTargets)
                    && !this.functionalOverloadMoreSpecific(candidate, other, arguments, candidateTargets)) dominated = true;
            }
            if (!dominated) maximal.add(candidate);
        }
        iInvocables = maximal.toArray(new IInvocable[maximal.size()]);

        // Determine arguments' types. LAMBDA expressions and method references are "poly expressions" (JLS8
        // 15.12.2.1): Their applicability does not depend on their (yet unknown) type; they get a "null"
        // argument type, and the comparison below treats "null" argument types as always convertible.
        final IType[] argumentTypes = new IType[arguments.length];
        boolean[] dependent = new boolean[arguments.length];
        for (int i = 0; i < dependent.length; i++) dependent[i] = this.needsInvocationTarget(arguments[i]);
        for (int i = 0; i < arguments.length; ++i) {
            Rvalue arg = arguments[i];
            // A common fixed-arity parameter provides a target before querying a generic argument's type.
            // This covers foo(getAttachment()) without guessing between differently typed overloads.
            IType commonTarget = null;
            boolean common = true;
            for (IInvocable candidate : iInvocables) {
                IClass[] parameters = candidate.getParameterTypes();
                if (candidate.isVarargs() || parameters.length != arguments.length) { common = false; break; }
                IType candidateTarget = candidateTargets.get(candidate)[i];
                if (commonTarget == null) commonTarget = candidateTarget;
                else if (!commonTarget.equals(candidateTarget)) { common = false; break; }
            }
            if (common && commonTarget != null) this.setLambdaTargetType(arg, commonTarget);
            argumentTypes[i] = this.isPolyExpression(arg) || dependent[i] ? null : this.getType(arg);
        }

        Map<IInvocable, IType[]> candidateArguments = new java.util.IdentityHashMap<>();
        List<IInvocable> attributedCandidates = new ArrayList<>();
        for (IInvocable candidate : iInvocables) {
            IType[] actual = argumentTypes.clone(), targets = candidateTargets.get(candidate);
            try {
                for (int i = 0; i < actual.length; i++) if (dependent[i] && !this.isPolyExpression(arguments[i])) {
                    IType target = targets[Math.min(i, targets.length - 1)];
                    if (candidate.isVarargs() && i >= targets.length - 1
                        && (actual.length != targets.length || !UnitCompiler.isArray(this.getType(arguments[i])))) target = UnitCompiler.getComponentType(target);
                    actual[i] = this.invocationArgumentType(arguments[i], target);
                }
            } catch (CompileException rejected) {
                // Failure under one overload's target says nothing about the other overloads.
                continue;
            }
            candidateArguments.put(candidate, actual);
            attributedCandidates.add(candidate);
        }
        iInvocables = attributedCandidates.toArray(new IInvocable[attributedCandidates.size()]);
        if (iInvocables.length == 0) throw UnitCompiler.compileException(locatable, "No compatible constructor/method");
        final IClass[] erasedArgumentTypes = new IClass[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) erasedArgumentTypes[i] = argumentTypes[i] == null
            ? this.iClassLoader.TYPE_java_lang_Object : UnitCompiler.rawTypeOf(argumentTypes[i]);
        // Determine most specific invocable WITHOUT boxing.
        IInvocable ii = this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, false, false, contextScope, candidateArguments);
        if (ii != null) return ii;

        // Determine most specific invocable WITH boxing.
        ii = this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, true, false, contextScope, candidateArguments);
        if (ii != null) return ii;

        // Variable arity is considered only after both fixed-arity invocation phases.
        ii = this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, true, true, contextScope, candidateArguments);
        if (ii != null) return ii;

        // Report a nice compile error.
        StringBuilder sb = new StringBuilder("No applicable constructor/method found for ");
        if (argumentTypes.length == 0) {
            sb.append("zero actual parameters");
        } else {
            sb.append("actual parameters \"").append(argumentTypes[0]);
            for (int i = 1; i < argumentTypes.length; ++i) {
                sb.append(", ").append(argumentTypes[i]);
            }
            sb.append("\"");
        }
        sb.append("; candidates are: \"").append(iInvocables[0]).append('"');
        for (int i = 1; i < iInvocables.length; ++i) {
            sb.append(", \"").append(iInvocables[i]).append('"');
        }
        this.compileError(sb.toString(), locatable.getLocation());

        // Well, returning a "fake" IInvocable is a bit tricky, because the iInvocables can be of different types.
        if (iInvocables[0] instanceof IClass.IConstructor) {
            return iInvocables[0].getDeclaringIClass().new IConstructor() {
                @Override public boolean       isVarargs()            { return false;              }
                @Override public IClass[]      getParameterTypes2()   { return erasedArgumentTypes; }
                @Override public Access        getAccess()            { return Access.PUBLIC;      }
                @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
                @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
            };
        } else
        if (iInvocables[0] instanceof IClass.IMethod) {
            final String methodName = ((IClass.IMethod) iInvocables[0]).getName();
            return iInvocables[0].getDeclaringIClass().new IMethod() {

                @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                @Override public Access        getAccess()            { return Access.PUBLIC;      }
                @Override public boolean       isStatic()             { return true;               }
                @Override public boolean       isAbstract()           { return false;              }
                @Override public IClass        getReturnType()        { return IClass.INT;         }
                @Override public String        getName()              { return methodName;         }
                @Override public IClass[]      getParameterTypes2()   { return erasedArgumentTypes; }
                @Override public boolean       isVarargs()            { return false;              }
                @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
            };
        } else
        {
            return iInvocables[0];
        }
    }

    /**
     * Determines the applicable invocables and choose the most specific invocable.
     *
     * @return The maximally specific {@link IClass.IInvocable} or {@code null} if no {@link IClass.IInvocable} is
     *         applicable
     */
    @Nullable public IClass.IInvocable findMostSpecificIInvocable(
        Locatable locatable, IInvocable[] candidates, IType[] arguments, boolean boxing, Scope scope
    ) throws CompileException {
        return this.findMostSpecificIInvocable(locatable, candidates, arguments, boxing, boxing, scope);
    }

    @Nullable private IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        final IInvocable[] iInvocables,
        IType[]            argumentTypes,
        boolean            boxingPermitted,
        boolean            varargsPermitted,
        Scope              contextScope
    ) throws CompileException {
        return this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, boxingPermitted, varargsPermitted, contextScope, null);
    }

    @Nullable private IInvocable findMostSpecificIInvocable(
        Locatable locatable, IInvocable[] iInvocables, IType[] argumentTypes,
        boolean boxingPermitted, boolean varargsPermitted, Scope contextScope,
        @Nullable Map<IInvocable, IType[]> candidateArguments
    ) throws CompileException {
        if (UnitCompiler.LOGGER.isLoggable(Level.FINER)) {
            UnitCompiler.LOGGER.entering(null, "findMostSpecificIInvocable", new Object[] {
                locatable, Arrays.toString(iInvocables), Arrays.toString(argumentTypes), boxingPermitted, contextScope
            });
        }

        // Select applicable methods (15.12.2.1).
        List<IClass.IInvocable> applicableIInvocables = new ArrayList<>();
        List<IClass.IInvocable> varargApplicables     = new ArrayList<>();

        NEXT_METHOD:
        for (IClass.IInvocable ii : iInvocables) {
            IType[] actualArguments = candidateArguments == null ? argumentTypes : candidateArguments.get(ii);
            boolean argsNeedAdjust = false;

            // Ignore inaccessible invocables.
            IType receiver = locatable instanceof Invocation && ii instanceof IMethod
                ? this.invocationReceiver((Invocation) locatable, ii.getDeclaringIClass()) : null;
            if (!this.isAccessible(ii, contextScope, receiver)) continue;

            // Check parameter count.
            final IClass[] parameterTypes   = ii.getParameterTypes();
            int            formalParamCount = parameterTypes.length;
            int            nUncheckedArg    = actualArguments.length;
            final boolean  isVarargs        = ii.isVarargs();

            // Match the last formal parameter with all args starting from that index (or none).
            VARARGS:
            if (isVarargs) {

                // Decrement the count to get the index.
                formalParamCount--;
                final IClass lastParamType = parameterTypes[formalParamCount].getComponentType();
                assert lastParamType != null;

                final int lastActualArg = nUncheckedArg - 1;

                // If the two have the same argCount and the last actual arg is an array of the same type accept it
                // (e.g. "void foo(int a, double...b) VS foo(1, new double[0]").
                if (
                    formalParamCount == lastActualArg
                    && actualArguments[lastActualArg] != null
                    && this.isInvocationArgumentConvertible(
                        actualArguments[lastActualArg], parameterTypes[formalParamCount], boxingPermitted
                    )
                ) {
                    nUncheckedArg--;
                } else {
                    if (!varargsPermitted) continue NEXT_METHOD;
                    for (int idx = lastActualArg; idx >= formalParamCount; --idx) {

                        // Is method invocation conversion possible (5.3)?
                        UnitCompiler.LOGGER.log(
                            Level.FINE,
                            "{0} <=> {1}",
                            new Object[] { lastParamType, actualArguments[idx] }
                        );
                        if (!this.isInvocationArgumentConvertible(actualArguments[idx], lastParamType, boxingPermitted)) {
                            formalParamCount++;
                            break VARARGS;
                        }

                        nUncheckedArg--;
                    }
                    argsNeedAdjust = true;
                }
            }

            if (formalParamCount == nUncheckedArg) {
                for (int j = 0; j < nUncheckedArg; ++j) {
                    UnitCompiler.LOGGER.log(
                        Level.FINE,
                        "{0}: {1} <=> {2}",
                        new Object[] { j, parameterTypes[j], actualArguments[j] }
                    );

                    // Is method invocation conversion possible (5.3)? ("null" = poly expression argument;
                    // assumed applicable, see above.)
                    if (
                        actualArguments[j] != null
                        && !this.isInvocationArgumentConvertible(actualArguments[j], parameterTypes[j], boxingPermitted)
                    ) {
                        continue NEXT_METHOD;
                    }
                }

                // Applicable!
                UnitCompiler.LOGGER.fine("Applicable!");

                // Varargs has lower priority.
                if (isVarargs) ii.setArgsNeedAdjust(argsNeedAdjust);
                if (argsNeedAdjust) {
                    varargApplicables.add(ii);
                } else {
                    applicableIInvocables.add(ii);
                }
            }
        }

        // Choose the most specific invocable (15.12.2.2).
        if (applicableIInvocables.size() == 1) {
            return (IInvocable) applicableIInvocables.get(0);
        }

        // No method found by previous phase(s).
        if (applicableIInvocables.size() == 0 && !varargApplicables.isEmpty()) {
            //TODO: 15.12.2.3 (type-conversion?)

            // 15.12.2.4 : Phase 3: Identify Applicable Variable Arity Methods
            applicableIInvocables = varargApplicables;
            if (applicableIInvocables.size() == 1) {
                return (IInvocable) applicableIInvocables.get(0);
            }
        }

        if (applicableIInvocables.size() == 0) return null;

        // 15.12.2.5. Determine the "maximally specific invocables".
        List<IClass.IInvocable> maximallySpecificIInvocables = new ArrayList<>();
        for (IClass.IInvocable applicableIInvocable : applicableIInvocables) {
            int moreSpecific = 0, lessSpecific = 0;
            for (IClass.IInvocable mostSpecificIInvocable : maximallySpecificIInvocables) {
                if (applicableIInvocable.isMoreSpecificThan(mostSpecificIInvocable)) {
                    ++moreSpecific;
                } else
                if (applicableIInvocable.isLessSpecificThan(mostSpecificIInvocable)) {
                    ++lessSpecific;
                }
            }
            if (moreSpecific == maximallySpecificIInvocables.size()) {
                maximallySpecificIInvocables.clear();
                maximallySpecificIInvocables.add(applicableIInvocable);
            } else
            if (lessSpecific < maximallySpecificIInvocables.size()) {
                maximallySpecificIInvocables.add(applicableIInvocable);
            } else
            {
                ;
            }
            UnitCompiler.LOGGER.log(Level.FINE, "maximallySpecificIInvocables={0}", maximallySpecificIInvocables);
        }

        if (maximallySpecificIInvocables.size() == 1) return (IInvocable) maximallySpecificIInvocables.get(0);

        ONE_NON_ABSTRACT_INVOCABLE:
        if (maximallySpecificIInvocables.size() > 1 && iInvocables[0] instanceof IClass.IMethod) {

            // Check if all methods have the same signature (i.e. the types of all their parameters are identical) and
            // exactly one of the methods is non-abstract (JLS7 15.12.2.2.BL2.B1).
            IClass.IMethod theNonAbstractMethod = null;
            {
                Iterator<IClass.IInvocable> it                          = maximallySpecificIInvocables.iterator();
                IClass.IMethod              m                           = (IClass.IMethod) it.next();
                final IClass[]              parameterTypesOfFirstMethod = m.getParameterTypes();
                for (;;) {
                    if (!m.isAbstract() && !m.getDeclaringIClass().isInterface()) {
                        if (theNonAbstractMethod == null) {
                            theNonAbstractMethod = m;
                        } else {
                            IClass declaringIClass                     = m.getDeclaringIClass();
                            IClass theNonAbstractMethodDeclaringIClass = theNonAbstractMethod.getDeclaringIClass();
                            if (declaringIClass == theNonAbstractMethodDeclaringIClass) {
                                if (m.getReturnType() == theNonAbstractMethod.getReturnType()) {

                                    // JLS8 15.12.2.5.B9: "Otherwise, the method invocation is ambiguous, and a
                                    // compile-time error occurs."
                                    throw new InternalCompilerException(locatable.getLocation(), (
                                        "Two non-abstract methods \""
                                        + m
                                        + "\" have the same parameter types, declaring type and return type"
                                    ));
                                } else

                                // For compatibility with SCALA, also allow for boxing conversion, e.g. return types
                                // "int" and "Object" are comnpatible, and "int" is more specific. See issue #182.
//                                if (m.getReturnType().isAssignableFrom(theNonAbstractMethod.getReturnType())) {
                                if (this.isInvocationArgumentConvertible(theNonAbstractMethod.getReturnType(), m.getReturnType(), /*boxingPermitted*/ true)) {
                                    ;
                                } else

                                // For compatibility with SCALA, also allow for boxing conversion, e.g. return types
                                // "int" and "Object" are comnpatible, and "int" is more specific. See issue #182.
//                                if (theNonAbstractMethod.getReturnType().isAssignableFrom(m.getReturnType())) {
                                if (this.isInvocationArgumentConvertible(m.getReturnType(), theNonAbstractMethod.getReturnType(), /*boxingPermitted*/ true)) {
                                    theNonAbstractMethod = m;
                                } else

                                {
                                    throw new InternalCompilerException(locatable.getLocation(), "Incompatible return types");
                                }
                            } else
                            if (declaringIClass.isAssignableFrom(theNonAbstractMethodDeclaringIClass)) {
                                ;
                            } else
                            if (theNonAbstractMethodDeclaringIClass.isAssignableFrom(declaringIClass)) {
                                theNonAbstractMethod = m;
                            } else
                            {
                                this.compileError(
                                    "Ambiguous static method import: \""
                                    + theNonAbstractMethod
                                    + "\" vs. \""
                                    + m
                                    + "\""
                                );
                            }
                        }
                    }
                    if (!it.hasNext()) break;

                    m = (IClass.IMethod) it.next();
                    IClass[] pts = m.getParameterTypes();
                    for (int i = 0; i < pts.length; ++i) {
                        if (pts[i] != parameterTypesOfFirstMethod[i]) break ONE_NON_ABSTRACT_INVOCABLE;
                    }
                }
            }

            // JLS7 15.12.2.2.BL2.B1.B1
            if (theNonAbstractMethod != null) return theNonAbstractMethod;

            // JLS7 15.12.2.2.BL2.B1.B2
            // Check "that exception [te1] is declared in the THROWS clause of each of the maximally specific methods".
            Set<IClass> s = new HashSet<>();
            {
                IClass[][]                  tes = new IClass[maximallySpecificIInvocables.size()][];
                Iterator<IClass.IInvocable> it  = maximallySpecificIInvocables.iterator();
                for (int i = 0; i < tes.length; ++i) {
                    tes[i] = ((IClass.IMethod) it.next()).getThrownExceptions();
                }
                for (int i = 0; i < tes.length; ++i) {
                    EACH_EXCEPTION:
                    for (IClass te1 : tes[i]) {
                        EACH_METHOD:
                        for (int k = 0; k < tes.length; ++k) {
                            if (k == i) continue;
                            for (IClass te2 : tes[k]) {
                                if (te2.isAssignableFrom(te1)) continue EACH_METHOD;
                            }
                            continue EACH_EXCEPTION;
                        }
                        s.add(te1);
                    }
                }
            }

            // JLS7 1.12.2.5.BL2.B1.BL.B2: "... the most specific method is chosen arbitrarily among the subset of the
            // maximally specific methods that have the most specific return type".
            final IClass.IMethod im;
            {
                Iterator<IInvocable> it2                              = maximallySpecificIInvocables.iterator();
                IClass.IMethod       methodWithMostSpecificReturnType = (IMethod) it2.next();

                while (it2.hasNext()) {
                    IMethod im2 = (IMethod) it2.next();
                    if (methodWithMostSpecificReturnType.getReturnType().isAssignableFrom(im2.getReturnType())) {
                        methodWithMostSpecificReturnType = im2;
                    }
                }
                im = methodWithMostSpecificReturnType;
            }

            // Return a "dummy" method.
            final IClass[]       tes = (IClass[]) s.toArray(new IClass[s.size()]);
            return im.getDeclaringIClass().new IMethod() {

                @Override public Map<String, GenericType[]> getTypeParameterBounds() throws CompileException {
                    return UnitCompiler.this.methodBounds(im);
                }
                @Override public GenericType getGenericReturnType() throws CompileException {
                    return UnitCompiler.this.getGenericReturnType(im);
                }
                @Override public GenericType[] getGenericParameterTypes() throws CompileException {
                    return UnitCompiler.this.getGenericParameterTypes(im);
                }
                @Override public String[] getTypeParameterNames() {
                    Set<String> names = UnitCompiler.this.getMethodTypeParameterNames((IMethod) im);
                    return names == null ? null : names.toArray(new String[names.size()]);
                }

                @Override public IAnnotation[] getAnnotations()                             { return im.getAnnotations();    }
                @Override public Access        getAccess()                                  { return im.getAccess();         }
                // JLS8 15.12.2.5.B8.B2: "In this case, the most specific method is considered to be abstract"
                @Override public boolean       isAbstract()                                 { return true;                   }
                @Override public boolean       isStatic()                                   { return im.isStatic();          }
                @Override public IClass        getReturnType()      throws CompileException { return im.getReturnType();     }
                @Override public String        getName()                                    { return im.getName();           }
                @Override public IClass[]      getParameterTypes2() throws CompileException { return im.getParameterTypes(); }
                @Override public boolean       isVarargs()                                  { return im.isVarargs();         }
                @Override public IClass[]      getThrownExceptions2()                       { return tes;                    }
            };
        }

        if (!varargsPermitted) return null; // Try the next invocation phase.

        // JLS7 15.12.2.2.BL2.B2
        {
            StringBuilder sb = new StringBuilder("Invocation of constructor/method with argument type(s) \"");
            for (int i = 0; i < argumentTypes.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(argumentTypes[i] == null ? "<poly>" : argumentTypes[i] == IClass.NULL ? "null" : Descriptor.toString(UnitCompiler.rawTypeOf(argumentTypes[i]).getDescriptor()));
            }
            sb.append("\" is ambiguous: ");
            for (int i = 0; i < maximallySpecificIInvocables.size(); ++i) {
                if (i > 0) sb.append(" vs. ");
                sb.append("\"" + maximallySpecificIInvocables.get(i) + "\"");
            }
            this.compileError(sb.toString(), locatable.getLocation());
        }

        return iInvocables[0];
    }

    private static <T> T
    assertNonNull(@Nullable T subject) {
        assert subject != null;
        return subject;
    }

    /**
     * Checks if "method invocation conversion" (5.3) is possible.
     */
    private boolean isInvocationArgumentConvertible(@Nullable IType actual, IClass formal, boolean boxing) throws CompileException {
        if (actual == null) return true;
        if (actual instanceof SourceTypeVariable || actual instanceof CapturedType || actual instanceof GenericArrayType) {
            if (!formal.isPrimitive()) return this.genericConvertible(actual, formal, boxing);
        }
        return this.isMethodInvocationConvertible(UnitCompiler.rawTypeOf(actual), formal, boxing);
    }

    private boolean
    isMethodInvocationConvertible(
        IClass  sourceType,
        IClass  targetType,
        boolean boxingPermitted
    ) throws CompileException {

        // 5.3 Identity conversion.
        if (sourceType == targetType) return true;

        // 5.3 Widening primitive conversion.
        if (this.isWideningPrimitiveConvertible(sourceType, targetType)) return true;

        // 5.3 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return true;

        // JLS7 5.3 A boxing conversion (JLS7 5.1.7) optionally followed by widening reference conversion.
        if (boxingPermitted) {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                return (
                    this.isIdentityConvertible(boxedType, targetType)
                    || this.isWideningReferenceConvertible(boxedType, targetType)
                );
            }
        }

        // JLS7 5.3 An unboxing conversion (JLS7 5.1.8) optionally followed by a widening primitive conversion.
        if (boxingPermitted) {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                return (
                    this.isIdentityConvertible(unboxedType, targetType)
                    || this.isWideningPrimitiveConvertible(unboxedType, targetType)
                );
            }
        }

        // 5.3 TODO: FLOAT or DOUBLE value set conversion

        if (this.isNullConvertible(sourceType, targetType)) return true;

        return false;
    }

    /**
     * @throws CompileException if the {@link Invocation} throws exceptions that are disallowed in the given scope
     */
    private void
    checkThrownExceptions(Invocation in, IMethod iMethod) throws CompileException {
        IClass[] thrownExceptions = iMethod.getThrownExceptions();
        GenericType[] generic = iMethod.getGenericThrownExceptions();
        if (generic != null && generic.length > 0) {
            InferenceContext inference = this.invocationInference(in, iMethod);
            IClass[] substituted = new IClass[generic.length];
            for (int i = 0; i < generic.length; i++) {
                IType type = this.substituteGenericType(generic[i], inference.values);
                substituted[i] = type == null ? thrownExceptions[Math.min(i, thrownExceptions.length - 1)] : UnitCompiler.rawTypeOf(type);
            }
            thrownExceptions = substituted;
        }
        for (IClass thrownException : thrownExceptions) {
            this.checkThrownException(
                in,                    // locatable
                thrownException,       // type
                in.getEnclosingScope() // scope
            );
        }
    }

    /**
     * @throws CompileException The exception with the given <var>type</var> must not be thrown in the given
     *                          <var>scope</var>
     */
    private final Map<AnonymousClassDeclaration, Set<IClass>> anonymousInitializerExceptions = new java.util.IdentityHashMap<>();

    private final Map<CatchClause, List<IType>> caughtExceptionTypes = new java.util.IdentityHashMap<>();

    @Nullable private List<IType> preciseRethrowTypes(ThrowStatement statement) {
        Rvalue expression = statement.expression;
        while (expression instanceof ParenthesizedExpression) expression = ((ParenthesizedExpression) expression).value;
        if (expression instanceof AmbiguousName && ((AmbiguousName) expression).reclassified instanceof Rvalue) {
            expression = (Rvalue) ((AmbiguousName) expression).reclassified;
        }
        if (!(expression instanceof LocalVariableAccess)) return null;
        LocalVariable variable = ((LocalVariableAccess) expression).localVariable;
        if (!variable.finaL && !variable.effectivelyFinal) return null;
        for (Scope scope = statement.getEnclosingScope(); !(scope instanceof FunctionDeclarator); scope = scope.getEnclosingScope()) {
            if (scope instanceof CatchClause && ((CatchClause) scope).catchParameter.localVariable == variable) {
                List<IType> types = this.caughtExceptionTypes.get(scope);
                return types == null ? Collections.<IType>emptyList() : types;
            }
        }
        return null;
    }

    private void rememberCaughtException(CatchClause clause, IType type) {
        List<IType> types = this.caughtExceptionTypes.get(clause);
        if (types == null) this.caughtExceptionTypes.put(clause, types = new ArrayList<>());
        if (!types.contains(type)) types.add(type);
    }

    private void
    checkThrownException(Locatable locatable, IType type, Scope scope) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!this.iClassLoader.TYPE_java_lang_Throwable.isAssignableFrom(UnitCompiler.rawTypeOf(type))) {
            this.compileError(
                "Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"",
                locatable.getLocation()
            );
        }

        IClass rawType = UnitCompiler.rawTypeOf(type);

        // "RuntimeException" and "Error" are never checked.
        if (
            this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(rawType)
            || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(rawType)
        ) return;

        if (this.lambdaProbeExceptions != null) {
            if (!this.lambdaProbeExceptions.contains(type)) this.lambdaProbeExceptions.add(type);
            return; // Candidate attribution must not change catch reachability in the lexical enclosing method.
        }

        Scope child = null;
        for (;; child = scope, scope = scope.getEnclosingScope()) {

            // Match against enclosing "try...catch" blocks.
            if (scope instanceof TryStatement) {
                TryStatement ts = (TryStatement) scope;
                // A catch/finally body is outside its own protected region.
                if (child instanceof CatchClause || child != null && child == ts.finallY) continue;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    CatchClause cc         = (CatchClause) ts.catchClauses.get(i);
                    for (Type ct : cc.catchParameter.types) {
                        IClass caughtType = this.getRawType(ct);

                        if (caughtType.isAssignableFrom(rawType)) {

                            // This catch clause definitely catches the exception.
                            cc.reachable = true;
                            this.rememberCaughtException(cc, type);
                            return;
                        }

                        CATCH_SUBTYPE:
                        if (rawType.isAssignableFrom(caughtType)) {

                            // This catch clause catches only a subtype of the exception type.
                            for (int j = 0; j < i; ++j) {
                                for (Type ct2 : ((CatchClause) ts.catchClauses.get(j)).catchParameter.types) {
                                    if (this.getRawType(ct2).isAssignableFrom(caughtType)) {

                                        // A preceding catch clause is more general than this catch clause.
                                        break CATCH_SUBTYPE;
                                    }
                                }
                            }

                            // This catch clause catches PART OF the actual exceptions.
                            cc.reachable = true;
                            this.rememberCaughtException(cc, caughtType);
                        }
                    }
                }
            } else

            // Match against "throws" clause of declaring function.
            if (scope instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) scope;
                for (Type thrownException : fd.thrownExceptions) {
                    if (this.getRawType(thrownException).isAssignableFrom(rawType)) return;
                }
                break;
            } else

            if (scope instanceof TypeBodyDeclaration) {
                TypeBodyDeclaration declaration = (TypeBodyDeclaration) scope;
                if (declaration.getDeclaringType() instanceof AnonymousClassDeclaration
                    && (scope instanceof FieldDeclaration && !((FieldDeclaration) scope).isStatic()
                        || scope instanceof Initializer && !((Initializer) scope).isStatic())) {
                    AnonymousClassDeclaration owner = (AnonymousClassDeclaration) declaration.getDeclaringType();
                    Set<IClass> exceptions = this.anonymousInitializerExceptions.get(owner);
                    if (exceptions == null) this.anonymousInitializerExceptions.put(owner, exceptions = new LinkedHashSet<IClass>());
                    exceptions.add(rawType);
                    // The anonymous constructor propagates initializer exceptions at its creation site.
                    this.checkThrownException(locatable, type, owner.getEnclosingScope());
                    return;
                }
                break;
            }
        }

        this.compileError((
            "Thrown exception of type \""
            + type
            + "\" is neither caught by a \"try...catch\" block "
            + "nor declared in the \"throws\" clause of the declaring function"
        ), locatable.getLocation());
    }

    private IType
    getTargetIType(QualifiedThisReference qtr) throws CompileException {

        if (qtr.targetIType != null) return qtr.targetIType;

        // Determine target type.
        return (qtr.targetIType = this.getType(qtr.qualification));
    }

    /**
     * Checks whether the operand is an {@code int} local variable.
     *
     * @return {@code null} iff <var>c</var> is <em>not</em> an {@code int} local variable
     */
    @Nullable LocalVariable
    isIntLv(Crement c) throws CompileException {
        if (!(c.operand instanceof AmbiguousName)) return null;
        AmbiguousName an = (AmbiguousName) c.operand;

        Atom rec = this.reclassify(an);
        if (!(rec instanceof LocalVariableAccess)) return null;
        LocalVariableAccess lva = (LocalVariableAccess) rec;

        LocalVariable lv = lva.localVariable;
        if (lv.finaL) this.compileError("Must not increment or decrement \"final\" local variable", lva.getLocation());

        return lv.type == IClass.INT ? lv : null;
    }

    private IClass
    resolve(final TypeDeclaration td) {

        final AbstractTypeDeclaration atd = (AbstractTypeDeclaration) td;

        if (atd.resolvedType != null) return atd.resolvedType;

        atd.resolvedType = new IClass() {

            @Override public GenericType[]
            getGenericSupertypes() throws CompileException {
                List<GenericType> result = new ArrayList<>();
                if (atd instanceof EnumDeclaration) {
                    result.add(new GenericType.Parameterized(UnitCompiler.this.iClassLoader.TYPE_java_lang_Enum,
                        new GenericType[] { new GenericType.Raw(this) }));
                    for (Type iface : ((EnumDeclaration) atd).getImplementedTypes()) result.add(UnitCompiler.this.sourceGenericType(iface));
                } else if (atd instanceof AnonymousClassDeclaration) {
                    AnonymousClassDeclaration declaration = (AnonymousClassDeclaration) atd;
                    if (declaration.lambdaInterfaces == null) result.add(UnitCompiler.this.sourceGenericType(declaration.baseType));
                    else for (IClass bound : declaration.lambdaInterfaces) result.add(new GenericType.Raw(bound));
                } else if (atd instanceof NamedClassDeclaration) {
                    NamedClassDeclaration declaration = (NamedClassDeclaration) atd;
                    if (declaration.extendedType != null) result.add(UnitCompiler.this.sourceGenericType(declaration.extendedType));
                    for (Type iface : declaration.implementedTypes) result.add(UnitCompiler.this.sourceGenericType(iface));
                } else if (atd instanceof InterfaceDeclaration) {
                    for (Type iface : ((InterfaceDeclaration) atd).extendedTypes) result.add(UnitCompiler.this.sourceGenericType(iface));
                }
                return result.toArray(new GenericType[result.size()]);
            }

            @Override public Map<String, GenericType[]> getTypeParameterBounds() throws CompileException {
                Map<String, GenericType[]> result = new HashMap<>();
                if (!(atd instanceof NamedTypeDeclaration)) return result;
                TypeParameter[] parameters = ((NamedTypeDeclaration) atd).getOptionalTypeParameters();
                if (parameters == null) return result;
                for (TypeParameter parameter : parameters) {
                    ReferenceType[] declared = parameter.bound;
                    GenericType[] bounds = new GenericType[declared == null ? 0 : declared.length];
                    for (int i = 0; i < bounds.length; i++) bounds[i] = UnitCompiler.this.sourceGenericType(declared[i]);
                    result.put(parameter.name, bounds);
                }
                return result;
            }

            @Override protected ITypeVariable[]
            getITypeVariables2() throws CompileException {

                if (!(atd instanceof NamedTypeDeclaration)) return new ITypeVariable[0];
                NamedTypeDeclaration ntd = (NamedTypeDeclaration) atd;

                final Java.TypeParameter[] typeParameters = ntd.getOptionalTypeParameters();
                if (typeParameters == null) return new ITypeVariable[0];
                ITypeVariable[] result = new ITypeVariable[typeParameters.length];
                for (int i = 0; i < result.length; i++) {
                    final TypeParameter tp = typeParameters[i];

                    result[i] = new ITypeVariable() {
                        @Nullable private ITypeVariableOrIClass[] bounds;

                        @Override public String
                        getName() { return tp.name; }

                        @Override public ITypeVariableOrIClass[]
                        getBounds() throws CompileException {
                            if (this.bounds != null) return this.bounds;
                            ITypeVariableOrIClass[] resolved = new ITypeVariableOrIClass[tp.bound == null ? 0 : tp.bound.length];
                            for (int j = 0; j < resolved.length; j++) resolved[j] = UnitCompiler.rawTypeOf(UnitCompiler.this.getType(tp.bound[j]));
                            return (this.bounds = resolved);
                        }
                    };
                }
                return result;
            }

            @Override protected IClass.IMethod[]
            getDeclaredIMethods2() {
                List<IClass.IMethod> res = new ArrayList<>(atd.getMethodDeclarations().size());
                for (MethodDeclarator md : atd.getMethodDeclarations()) {
                    res.add(UnitCompiler.this.toIMethod(md));
                }

                if (td instanceof EnumDeclaration) {

                    res.add(new IMethod() {

                        @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                        @Override public Access        getAccess()            { return Access.PUBLIC;      }
                        @Override public boolean       isStatic()             { return true;               }
                        @Override public boolean       isAbstract()           { return false;              }
                        @Override public String        getName()              { return "values";           }
                        @Override public IClass[]      getParameterTypes2()   { return new IClass[0];      }
                        @Override public boolean       isVarargs()            { return false;              }
                        @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }

                        @Override public IClass
                        getReturnType() {
                            IClass rt = atd.resolvedType;
                            assert rt != null;
                            return UnitCompiler.this.iClassLoader.getArrayIClass(rt);
                        }
                    });

                    res.add(new IMethod() {

                        @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0];                                                    }
                        @Override public Access        getAccess()            { return Access.PUBLIC;                                                         }
                        @Override public boolean       isStatic()             { return true;                                                                  }
                        @Override public boolean       isAbstract()           { return false;                                                                 }
                        @Override public String        getName()              { return "valueOf";                                                             }
                        @Override public IClass[]      getParameterTypes2()   { return new IClass[] { UnitCompiler.this.iClassLoader.TYPE_java_lang_String }; }
                        @Override public boolean       isVarargs()            { return false;                                                                 }
                        @Override public IClass[]      getThrownExceptions2() { return new IClass[0];                                                         }

                        @Override public IClass
                        getReturnType() {
                            IClass rt = atd.resolvedType;
                            assert rt != null;
                            return rt;
                        }
                    });
                }

                return (IClass.IMethod[]) res.toArray(new IClass.IMethod[res.size()]);
            }

            @Nullable private IClass[] declaredClasses;

            @Override protected IClass[]
            getDeclaredIClasses2() {

                if (this.declaredClasses != null) return this.declaredClasses;

                Collection<MemberTypeDeclaration> mtds = td.getMemberTypeDeclarations();
                IClass[]                          mts  = new IClass[mtds.size()];
                int                               i    = 0;
                for (MemberTypeDeclaration mtd : mtds) {
                    mts[i++] = UnitCompiler.this.resolve(mtd);
                }
                return (this.declaredClasses = mts);
            }

            @Override @Nullable protected IClass
            getDeclaringIClass2() {
                Scope s = atd;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
                    if (s instanceof CompilationUnit) return null;
                }
                return UnitCompiler.this.resolve((AbstractTypeDeclaration) s.getEnclosingScope());
            }

            @Override @Nullable protected IClass
            getOuterIClass2() {
                AbstractTypeDeclaration oc = (AbstractTypeDeclaration) UnitCompiler.getOuterClass(atd);
                if (oc == null) return null;
                return UnitCompiler.this.resolve(oc);
            }

            @Override protected String
            getDescriptor2() { return Descriptor.fromClassName(atd.getClassName()); }

            @Override public boolean
            isArray() { return false; }

            @Override protected IClass
            getComponentType2() { throw new InternalCompilerException(td.getLocation(), "SNO: Non-array type has no component type"); }

            @Override public boolean
            isPrimitive() { return false; }

            @Override public boolean
            isPrimitiveNumeric() { return false; }

            @Override protected IConstructor[]
            getDeclaredIConstructors2() {
                if (atd instanceof AbstractClassDeclaration) {
                    AbstractClassDeclaration acd = (AbstractClassDeclaration) atd;

                    ConstructorDeclarator[] cs     = acd.getConstructors();
                    IClass.IConstructor[]   result = new IClass.IConstructor[cs.length];
                    for (int i = 0; i < cs.length; ++i) result[i] = UnitCompiler.this.toIConstructor(cs[i]);
                    return result;
                }
                return new IClass.IConstructor[0];
            }

            @Override protected IField[]
            getDeclaredIFields2() {
                if (atd instanceof AbstractClassDeclaration) {
                    AbstractClassDeclaration cd = (AbstractClassDeclaration) atd;
                    List<IClass.IField>      l  = new ArrayList<>();

                    // Determine variable declarators of type declaration.
                    for (FieldDeclaration fd : Iterables.filterByClass(
                        cd.fieldDeclarationsAndInitializers,
                        FieldDeclaration.class
                    )) l.addAll(Arrays.asList(UnitCompiler.this.compileFields(fd)));

                    if (atd instanceof EnumDeclaration) {
                        EnumDeclaration ed = (EnumDeclaration) atd;

                        for (EnumConstant ec : ed.getConstants()) {
                            l.add(UnitCompiler.this.compileField(
                                ed,                                                              // declaringType
                                new Annotation[0],                                               // annotations
                                Access.PUBLIC,                                                   // access
                                true,                                                            // statiC
                                true,                                                            // finaL
                                new SimpleType(ed.getLocation(), UnitCompiler.this.resolve(ed)), // type
                                0,                                                               // brackets
                                ec.name,                                                         // name
                                null                                                             // initializer
                            ));
                        }
                    }

                    return (IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                if (atd instanceof InterfaceDeclaration) {
                    InterfaceDeclaration id = (InterfaceDeclaration) atd;
                    List<IClass.IField>  l  = new ArrayList<>();

                    // Determine static fields.
                    for (FieldDeclaration fd : Iterables.filterByClass(
                        id.constantDeclarations,
                        FieldDeclaration.class
                    )) l.addAll(Arrays.asList(UnitCompiler.this.compileFields(fd)));
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                {
                    throw new InternalCompilerException(
                        td.getLocation(),
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override public IField[]
            getSyntheticIFields() {
                if (atd instanceof AbstractClassDeclaration) {
                    Collection<IClass.IField> c = ((AbstractClassDeclaration) atd).syntheticFields.values();
                    return (IField[]) c.toArray(new IField[c.size()]);
                }
                return new IField[0];
            }

            @Override @Nullable protected IClass
            getSuperclass2() throws CompileException {

                if (atd instanceof EnumDeclaration) return UnitCompiler.this.iClassLoader.TYPE_java_lang_Enum;

                if (atd instanceof AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getHierarchyType(((AnonymousClassDeclaration) atd).baseType);
                    return UnitCompiler.isInterface(bt) ? UnitCompiler.this.iClassLoader.TYPE_java_lang_Object : bt;
                }

                if (atd instanceof NamedClassDeclaration) {
                    if (Descriptor.JAVA_LANG_OBJECT.equals(this.getDescriptor())) return null;
                    NamedClassDeclaration ncd = (NamedClassDeclaration) atd;
                    Type                  oet = ncd.extendedType;
                    if (oet == null) return UnitCompiler.this.iClassLoader.TYPE_java_lang_Object;
                    IClass superclass = UnitCompiler.this.getHierarchyType(oet);
                    if (superclass.isInterface()) {
                        UnitCompiler.this.compileError(
                            "\"" + superclass.toString() + "\" is an interface; classes can only extend a class",
                            td.getLocation()
                        );
                    }
                    return superclass;
                }

                return null;
            }

            @Override public Access
            getAccess() {

                if (atd instanceof MemberClassDeclaration)            return ((MemberClassDeclaration)            atd).getAccess();
                if (atd instanceof PackageMemberClassDeclaration)     return ((PackageMemberClassDeclaration)     atd).getAccess();
                if (atd instanceof MemberInterfaceDeclaration)        return ((MemberInterfaceDeclaration)        atd).getAccess();
                if (atd instanceof PackageMemberInterfaceDeclaration) return ((PackageMemberInterfaceDeclaration) atd).getAccess();
                if (atd instanceof AnonymousClassDeclaration)         return Access.PUBLIC;
                if (atd instanceof LocalClassDeclaration)             return Access.PUBLIC;

                throw new InternalCompilerException(td.getLocation(), atd.getClass().getName());
            }

            @Override public boolean
            isFinal() { return atd instanceof NamedClassDeclaration && ((NamedClassDeclaration) atd).isFinal(); }

            @Override protected IClass[]
            getInterfaces2() throws CompileException {
                if (atd instanceof AnonymousClassDeclaration) {
                    if (((AnonymousClassDeclaration) atd).lambdaInterfaces != null) {
                        IClass[] result = ((AnonymousClassDeclaration) atd).lambdaInterfaces.clone();
                        for (int i = 0; i < result.length; i++) result[i] = UnitCompiler.rawTypeOf(result[i]);
                        return result;
                    }
                    IClass bt = UnitCompiler.this.getHierarchyType(((AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? new IClass[] { bt } : new IClass[0];
                } else
                if (atd instanceof NamedClassDeclaration) {
                    NamedClassDeclaration ncd = (NamedClassDeclaration) atd;
                    IClass[]              res = new IClass[ncd.implementedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getHierarchyType(ncd.implementedTypes[i]);
                        if (!res[i].isInterface()) {
                            UnitCompiler.this.compileError((
                                "\""
                                + res[i].toString()
                                + "\" is not an interface; classes can only implement interfaces"
                            ), td.getLocation());
                        }
                    }
                    return res;
                } else
                if (atd instanceof InterfaceDeclaration) {
                    InterfaceDeclaration id  = (InterfaceDeclaration) atd;
                    IClass[]             res = new IClass[id.extendedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getHierarchyType(id.extendedTypes[i]);
                        if (!res[i].isInterface()) {
                            UnitCompiler.this.compileError((
                                "\""
                                + res[i].toString()
                                + "\" is not an interface; interfaces can only extend interfaces"
                            ), td.getLocation());
                        }
                    }
                    return res;
                } else {
                    throw new InternalCompilerException(
                        td.getLocation(),
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override protected IAnnotation[]
            getIAnnotations2() throws CompileException { return UnitCompiler.this.toIAnnotations(td.getAnnotations()); }

            @Override public boolean
            isAbstract() {
                if (atd instanceof EnumDeclaration) {
                    try {
                        for (IMethod method : this.getIMethods()) {
                            if (!method.isAbstract()) continue;
                            IMethod implementation = this.findIMethod(method.getName(), method.getParameterTypes());
                            if (implementation == null || implementation.isAbstract()) {
                                implementation = UnitCompiler.this.findGenericOverride(this, method);
                            }
                            if (implementation == null || implementation.isAbstract()) return true;
                        }
                    } catch (CompileException exception) {
                        throw new InternalCompilerException(td.getLocation(), "Resolving enum abstract methods", exception);
                    }
                }
                return (
                    atd instanceof InterfaceDeclaration
                    || (atd instanceof NamedClassDeclaration && ((NamedClassDeclaration) atd).isAbstract())
                );
            }

            @Override public boolean
            isEnum() { return atd instanceof EnumDeclaration; }

            @Override public boolean
            isInterface() { return atd instanceof InterfaceDeclaration; }
        };
        this.sourceDeclarations.put(atd.resolvedType, atd);
        return atd.resolvedType;
    }

    private void
    referenceThis(
        Locatable               locatable,
        AbstractTypeDeclaration declaringType,
        TypeBodyDeclaration     declaringTypeBodyDeclaration,
        IType                   targetIType
    ) throws CompileException {
        List<TypeDeclaration> path = UnitCompiler.getOuterClasses(declaringType);

        if (UnitCompiler.isStaticContext(declaringTypeBodyDeclaration)) {
            this.compileError("No current instance available in static context", locatable.getLocation());
        }

        int j;
        TARGET_FOUND: {
            // A qualified this denotes its lexical instance, even if an inner lambda proxy
            // also implements that interface. Selecting the proxy would recurse into its SAM.
            if (locatable instanceof QualifiedThisReference) for (j = 0; j < path.size(); j++) {
                if (UnitCompiler.rawTypeOf(targetIType) == this.resolve(path.get(j))) break TARGET_FOUND;
            }
            for (j = 0; j < path.size(); ++j) {

                // Notice: JLS7 15.9.2.BL1.B3.B1.B2 seems to be wrong: Obviously, JAVAC does not only allow
                //
                //    O is the nth lexically enclosing class
                //
                // , but also
                //
                //    O is assignable from the nth lexically enclosing class
                //
                // However, this strategy bears the risk of ambiguities, because "O" may be assignable from more than
                // one enclosing class.
                if (UnitCompiler.isAssignableFrom(targetIType, this.resolve((TypeDeclaration) path.get(j)))) {
                    break TARGET_FOUND;
                }
            }
            this.compileError(
                "\"" + declaringType + "\" is not enclosed by \"" + targetIType + "\"",
                locatable.getLocation()
            );
        }

        int i;
        if (declaringTypeBodyDeclaration instanceof ConstructorDeclarator) {
            if (j == 0) {
                this.bytecodeEmitter.load(locatable, this.resolve(declaringType), 0);
                return;
            }

            ConstructorDeclarator constructorDeclarator = (
                (ConstructorDeclarator) declaringTypeBodyDeclaration
            );
            String        spn                = "this$" + (path.size() - 2);
            LocalVariable syntheticParameter = (LocalVariable) constructorDeclarator.syntheticParameters.get(spn);
            if (syntheticParameter == null) {
                throw new InternalCompilerException(
                    locatable.getLocation(),
                    "SNO: Synthetic parameter \"" + spn + "\" not found"
                );
            }
            this.bytecodeEmitter.load(locatable, syntheticParameter);
            i = 1;
        } else {
            this.bytecodeEmitter.load(locatable, this.resolve(declaringType), 0);
            i = 0;
        }
        for (; i < j; ++i) {
            final InnerClassDeclaration inner     = (InnerClassDeclaration) path.get(i);
            final TypeDeclaration       outer     = (TypeDeclaration) path.get(i + 1);

            SimpleIField sf = new SimpleIField(
                this.resolve(inner),             // declaringIClass
                "this$" + (path.size() - i - 2), // name
                this.resolve(outer)              // type
            );
            inner.defineSyntheticField(sf);
            this.bytecodeEmitter.getfield(locatable, sf);
        }
    }

    /**
     * Returns a list consisting of the given <var>inner</var> class and all its enclosing (outer) classes.
     *
     * @return {@link List} of {@link TypeDeclaration}; has length one iff <var>inner</var> has no enclosing instance
     */
    private static List<TypeDeclaration>
    getOuterClasses(TypeDeclaration inner) {
        List<TypeDeclaration> path = new ArrayList<>();
        for (TypeDeclaration ic = inner; ic != null; ic = UnitCompiler.getOuterClass(ic)) path.add(ic);
        return path;
    }

    /**
     * @return The {@link TypeDeclaration} that immediately encloses the <var>typeDeclaration</var>, or {@code null}
     */
    @Nullable static TypeDeclaration
    getOuterClass(TypeDeclaration typeDeclaration) {

        // Package member class declaration.
        if (typeDeclaration instanceof PackageMemberClassDeclaration) return null;

        // Interfaces (including annotation types) and member enums have no enclosing instance.
        if (typeDeclaration instanceof InterfaceDeclaration || typeDeclaration instanceof MemberEnumDeclaration) return null;

        // Local class declaration.
        if (typeDeclaration instanceof LocalClassDeclaration) {
            Scope s = typeDeclaration.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator) && !(s instanceof Initializer); s = s.getEnclosingScope());
            if (s instanceof MethodDeclarator && ((MethodDeclarator) s).isStatic()) return null;
            if (s instanceof Initializer      && ((Initializer)      s).isStatic()) return null;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof AbstractClassDeclaration
            ) ? immediatelyEnclosingTypeDeclaration : null;
        }

        // Member class declaration.
        if (
            typeDeclaration instanceof MemberClassDeclaration
            && ((MemberClassDeclaration) typeDeclaration).isStatic()
        ) return null;

        // Anonymous class declaration, interface declaration.
        Scope s = typeDeclaration;
        for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
            if (s instanceof ConstructorInvocation) return null;
            if (s instanceof CompilationUnit) return null;
        }

        //if (!(s instanceof ClassDeclaration)) return null;

        if (UnitCompiler.isStaticContext((TypeBodyDeclaration) s)) return null;

        return (AbstractTypeDeclaration) s.getEnclosingScope();
    }

    private IClass
    getIClass(ThisReference tr) throws CompileException {

        if (tr.iClass != null) return tr.iClass;

        // Compile error if in static function context.
        Scope s;
        for (
            s = tr.getEnclosingScope();
            s instanceof Statement || s instanceof CatchClause;
            s = s.getEnclosingScope()
        );
        if (s instanceof FunctionDeclarator) {
            FunctionDeclarator function = (FunctionDeclarator) s;
            if (function instanceof MethodDeclarator && ((MethodDeclarator) function).isStatic()) {
                this.compileError("No current instance available in static method", tr.getLocation());
            }
        }

        // Determine declaring type.
        while (!(s instanceof TypeDeclaration)) {
            s = s.getEnclosingScope();
        }
        return (tr.iClass = this.resolve((TypeDeclaration) s));
    }

    private IType
    getReturnType(FunctionDeclarator fd) throws CompileException {

        if (fd.returnType != null) return fd.returnType;

        return (fd.returnType = this.getType(fd.type));
    }

    /**
     * @return the {@link IConstructor} that implements the <var>constructorDeclarator</var>
     */
    IClass.IConstructor
    toIConstructor(final ConstructorDeclarator constructorDeclarator) {
        if (constructorDeclarator.iConstructor != null) return constructorDeclarator.iConstructor;

        constructorDeclarator.iConstructor = this.resolve(constructorDeclarator.getDeclaringType()).new IConstructor() {

            @Override public GenericType[] getGenericThrownExceptions() throws CompileException {
                GenericType[] result = new GenericType[constructorDeclarator.thrownExceptions.length];
                for (int i = 0; i < result.length; i++) result[i] = UnitCompiler.this.sourceGenericType(constructorDeclarator.thrownExceptions[i]);
                return result;
            }

            @Override public GenericType[]
            getGenericParameterTypes() throws CompileException {
                FormalParameter[] parameters = constructorDeclarator.formalParameters.parameters;
                GenericType[] result = new GenericType[parameters.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = UnitCompiler.this.sourceGenericType(parameters[i].type);
                    if (i == result.length - 1 && constructorDeclarator.formalParameters.variableArity) result[i] = new GenericType.ArrayOf(result[i]);
                }
                return result;
            }

            @Override public String[]
            getTypeParameterNames() {
                TypeParameter[] parameters = constructorDeclarator.getOptionalTypeParameters();
                if (parameters == null) return new String[0];
                String[] result = new String[parameters.length];
                for (int i = 0; i < result.length; i++) result[i] = parameters[i].name;
                return result;
            }

            @Override public Map<String, GenericType[]> getTypeParameterBounds() throws CompileException {
                Map<String, GenericType[]> result = new HashMap<>();
                TypeParameter[] parameters = constructorDeclarator.getOptionalTypeParameters();
                if (parameters != null) for (TypeParameter parameter : parameters) {
                    ReferenceType[] bounds = parameter.bound;
                    GenericType[] types = new GenericType[bounds == null ? 0 : bounds.length];
                    for (int i = 0; i < types.length; i++) types[i] = UnitCompiler.this.sourceGenericType(bounds[i]);
                    result.put(parameter.name, types);
                }
                return result;
            }

            @Nullable private IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() { return constructorDeclarator.getAccess(); }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(constructorDeclarator.getAnnotations()));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(constructorDeclarator.getLocation(), null, ce);
                }
            }

            // Implement IInvocable.

            @Override public MethodDescriptor
            getDescriptor2() throws CompileException {

                if (!(constructorDeclarator.getDeclaringClass() instanceof InnerClassDeclaration)) {
                    return super.getDescriptor2();
                }

                if (constructorDeclarator.getDeclaringClass() instanceof MemberEnumDeclaration) {
                    return super.getDescriptor2();
                }

                List<String> parameterFds = new ArrayList<>();

                if (
                    constructorDeclarator.getDeclaringClass() instanceof AnonymousClassDeclaration
                    && ((AnonymousClassDeclaration) constructorDeclarator.getDeclaringClass()).enumConstantBody
                ) {
                    parameterFds.add(Descriptor.JAVA_LANG_STRING);
                    parameterFds.add(Descriptor.INT);
                }

                // Convert enclosing instance reference into prepended constructor parameters.
                IClass outerClass = UnitCompiler.this.resolve(
                    constructorDeclarator.getDeclaringClass()
                ).getOuterIClass();
                if (outerClass != null) parameterFds.add(outerClass.getDescriptor());

                // Convert synthetic fields into prepended constructor parameters.
                for (IField sf : constructorDeclarator.getDeclaringClass().syntheticFields.values()) {
                    if (sf.getName().startsWith("val$")) parameterFds.add(sf.getType().getDescriptor());
                }

                // Process the 'normal' (declared) function parameters.
                for (IClass pt : this.getParameterTypes2()) parameterFds.add(pt.getDescriptor());

                return new MethodDescriptor(
                    Descriptor.VOID,                                                 // returnFd
                    (String[]) parameterFds.toArray(new String[parameterFds.size()]) // parameterFds
                );
            }

            @Override public boolean
            isVarargs() { return constructorDeclarator.formalParameters.variableArity; }

            @Override public IClass[]
            getParameterTypes2() throws CompileException {

                FormalParameter[] parameters = constructorDeclarator.formalParameters.parameters;
                IClass[]          res        = new IClass[parameters.length];

                for (int i = 0; i < parameters.length; ++i) {
                    IClass parameterType = UnitCompiler.this.getRawType(parameters[i].type);
                    if (i == parameters.length - 1 && constructorDeclarator.formalParameters.variableArity) {
                        parameterType = UnitCompiler.this.iClassLoader.getArrayIClass(UnitCompiler.rawTypeOf(parameterType));
                    }
                    res[i] = parameterType;
                }

                return res;
            }

            @Override public IClass[]
            getThrownExceptions2() throws CompileException {
                IClass[] res = new IClass[constructorDeclarator.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getRawType(constructorDeclarator.thrownExceptions[i]);
                }
                return res;
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder().append(
                    constructorDeclarator.getDeclaringType().getClassName()
                ).append('(');

                FormalParameter[] parameters = constructorDeclarator.formalParameters.parameters;
                for (int i = 0; i < parameters.length; ++i) {
                    if (i != 0) sb.append(", ");
                    sb.append(parameters[i].toString(
                        i == parameters.length - 1
                        && constructorDeclarator.formalParameters.variableArity
                    ));
                }
                return sb.append(')').toString();
            }
        };
        return constructorDeclarator.iConstructor;
    }

    /**
     * @return The {@link IMethod} that implements the <var>methodDeclarator</var>
     */
    public IClass.IMethod
    toIMethod(final MethodDeclarator methodDeclarator) {

        if (methodDeclarator.iMethod != null) return methodDeclarator.iMethod;

        methodDeclarator.iMethod = this.resolve(methodDeclarator.getDeclaringType()).new IMethod() {
            @Override public GenericType[] getGenericThrownExceptions() throws CompileException {
                GenericType[] result = new GenericType[methodDeclarator.thrownExceptions.length];
                for (int i = 0; i < result.length; i++) result[i] = UnitCompiler.this.sourceGenericType(methodDeclarator.thrownExceptions[i]);
                return result;
            }

            @Override public GenericType
            getGenericReturnType() throws CompileException {
                return UnitCompiler.this.sourceGenericType(methodDeclarator.type);
            }

            @Override public GenericType[]
            getGenericParameterTypes() throws CompileException {
                FormalParameter[] parameters = methodDeclarator.formalParameters.parameters;
                GenericType[] result = new GenericType[parameters.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = UnitCompiler.this.sourceGenericType(parameters[i].type);
                    if (i == result.length - 1 && methodDeclarator.formalParameters.variableArity) result[i] = new GenericType.ArrayOf(result[i]);
                }
                return result;
            }

            @Override public String[]
            getTypeParameterNames() {
                TypeParameter[] parameters = methodDeclarator.getOptionalTypeParameters();
                if (parameters == null) return new String[0];
                String[] result = new String[parameters.length];
                for (int i = 0; i < result.length; i++) result[i] = parameters[i].name;
                return result;
            }

            @Override public Map<String, GenericType[]> getTypeParameterBounds() throws CompileException {
                Map<String, GenericType[]> result = new HashMap<>();
                TypeParameter[] parameters = methodDeclarator.getOptionalTypeParameters();
                if (parameters != null) for (TypeParameter parameter : parameters) {
                    ReferenceType[] bounds = parameter.bound;
                    GenericType[] types = new GenericType[bounds == null ? 0 : bounds.length];
                    for (int i = 0; i < types.length; i++) types[i] = UnitCompiler.this.sourceGenericType(bounds[i]);
                    result.put(parameter.name, types);
                }
                return result;
            }

            @Nullable IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() {
                return (
                    methodDeclarator.getDeclaringType() instanceof InterfaceDeclaration && methodDeclarator.getAccess() == Access.DEFAULT
                    ? Access.PUBLIC
                    : methodDeclarator.getAccess()
                );
            }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(methodDeclarator.getAnnotations()));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(methodDeclarator.getLocation(), null, ce);
                }
            }

            // Implement IInvocable.

            @Override public boolean
            isVarargs() { return methodDeclarator.formalParameters.variableArity; }

            @Override public IClass[]
            getParameterTypes2() throws CompileException {
                FormalParameter[] parameters = methodDeclarator.formalParameters.parameters;
                IClass[]          res        = new IClass[parameters.length];
                for (int i = 0; i < parameters.length; ++i) {
                    IClass parameterType = UnitCompiler.this.getRawType(parameters[i].type);
                    if (i == parameters.length - 1 && methodDeclarator.formalParameters.variableArity) {
                        parameterType = UnitCompiler.this.iClassLoader.getArrayIClass(parameterType);
                    }
                    res[i] = parameterType;
                }
                return res;
            }

            @Override public IClass[]
            getThrownExceptions2() throws CompileException {
                IClass[] result = new IClass[methodDeclarator.thrownExceptions.length];
                for (int i = 0; i < result.length; i++) result[i] = UnitCompiler.this.getRawType(methodDeclarator.thrownExceptions[i]);
                return result;
            }

            @Override public boolean
            isStatic() { return methodDeclarator.isStatic(); }

            @Override public boolean isFinal() { return methodDeclarator.isFinal(); }

            @Override public boolean
            isAbstract() {
                return (
                    (
                        methodDeclarator.getDeclaringType() instanceof InterfaceDeclaration
                        && !methodDeclarator.isDefault()
                        && !methodDeclarator.isStatic()
                        && methodDeclarator.getAccess() != Access.PRIVATE
                    )
                    || methodDeclarator.isAbstract()
                );
            }

            @Override public IClass
            getReturnType() throws CompileException {
                return UnitCompiler.rawTypeOf(UnitCompiler.this.getReturnType(methodDeclarator));
            }

            @Override public String
            getName() { return methodDeclarator.name; }
        };
        this.sourceMethods.put(methodDeclarator.iMethod, methodDeclarator);
        return methodDeclarator.iMethod;
    }

    private IClass.IInvocable
    toIInvocable(final FunctionDeclarator fd) {
        IClass.IInvocable result = (IClass.IInvocable) fd.accept(
            new FunctionDeclaratorVisitor<IInvocable, RuntimeException>() {
                @Override public IInvocable visitMethodDeclarator(MethodDeclarator md)           { return UnitCompiler.this.toIMethod((MethodDeclarator) fd);           }
                @Override public IInvocable visitConstructorDeclarator(ConstructorDeclarator cd) { return UnitCompiler.this.toIConstructor((ConstructorDeclarator) fd); }
            }
        );

        assert result != null;
        return result;
    }

    /**
     * If the given name was declared in a simple type import, load that class.
     */
    @Nullable private IClass
    importSingleType(String simpleTypeName, Location location) throws CompileException {
        String[] ss = this.getSingleTypeImport(simpleTypeName, location);
        if (ss == null) return null;

        IClass iClass = this.findTypeByFullyQualifiedName(location, ss);
        if (iClass == null) {
            this.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
            return this.iClassLoader.TYPE_java_lang_Object;
        }
        return iClass;
    }

    /**
     * Checks if the given simple name was imported through a single type import.
     *
     * @param name The simple type name, e.g. {@code "Inner"}
     * @return     The fully qualified name, e.g. {@code { "pkg", "Outer", "Inner" }}, or {@code null}
     */
    @Nullable public String[]
    getSingleTypeImport(String name, Location location) throws CompileException {

        // Resolve all single type imports (if not already done).
        Map<String, String[]> stis = this.singleTypeImports;
        if (stis == null) {

            // Collect all single type import declarations.
            final List<SingleTypeImportDeclaration> stids = new ArrayList<>();
            for (ImportDeclaration id : this.abstractCompilationUnit.importDeclarations) {
                id.accept(new ImportVisitor<Void, RuntimeException>() {

                    @Override @Nullable public Void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          { stids.add(stid); return null; }
                    @Override @Nullable public Void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     { return null;                  }
                    @Override @Nullable public Void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      { return null;                  }
                    @Override @Nullable public Void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) { return null;                  }
                });
            }

            // Resolve all single type imports.
            stis = new HashMap<>();
            for (SingleTypeImportDeclaration stid : stids) {

                String[] ids        = stid.identifiers;
                String   simpleName = UnitCompiler.last(ids);

                // Check for re-import of same simple name.
                String[] prev = (String[]) stis.put(simpleName, ids);
                if (prev != null && !Arrays.equals(prev, ids)) {
                    UnitCompiler.this.compileError((
                        "Class \"" + simpleName + "\" was previously imported as "
                        + "\"" + Java.join(prev, ".") + "\", now as \"" + Java.join(ids, ".") + "\""
                    ), stid.getLocation());
                }

                if (this.findTypeByFullyQualifiedName(location, ids) == null) {
                    UnitCompiler.this.compileError(
                        "A class \"" + Java.join(ids, ".") + "\" could not be found",
                        stid.getLocation()
                    );
                }
            }

            this.singleTypeImports = stis;
        }

        return (String[]) stis.get(name);
    }

    /**
     * To be used only by {@link #getSingleTypeImport(String, Location)}; {@code null} means "not yet initialized"
     */
    @Nullable private Map<String /*simpleTypeName*/, String[] /*fullyQualifiedTypeName*/> singleTypeImports;

    /**
     * 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.<br>
     * 6.5.5.1.6 Type-import-on-demand declaration.
     *
     * @return {@code null} if the given <var>simpleTypeName</var> cannot be resolved through any of the
     *         type-import-on-demand declarations
     */
    @Nullable public IClass
    importTypeOnDemand(String simpleTypeName, Location location) throws CompileException {

        IClass importedClass = (IClass) this.onDemandImportableTypes.get(simpleTypeName);
        if (importedClass == null) {
            importedClass = this.importTypeOnDemand2(simpleTypeName, location);
            this.onDemandImportableTypes.put(simpleTypeName, importedClass);
        }

        return importedClass;
    }
    private final Map<String /*simpleTypeName*/, IClass> onDemandImportableTypes = new HashMap<>();

    /**
     * @return {@code null} if the given <var>simpleTypeName</var> cannot be resolved through any of the
     *         type-import-on-demand declarations
     */
    @Nullable private IClass
    importTypeOnDemand2(String simpleTypeName, Location location) throws CompileException {

        IClass importedClass = null;
        for (TypeImportOnDemandDeclaration tiodd : this.getTypeImportOnDemandImportDeclarations()) {

            IClass iClass = this.findTypeByFullyQualifiedName(
                location,
                UnitCompiler.concat(tiodd.identifiers, simpleTypeName)
            );
            if (iClass == null) continue;

            if (importedClass != null && importedClass != iClass) {
                this.compileError(
                    "Ambiguous class name: \"" + importedClass + "\" vs. \"" + iClass + "\"",
                    location
                );
            }
            importedClass = iClass;
        }
        if (importedClass == null) return null;
        return importedClass;
    }

    private Collection<TypeImportOnDemandDeclaration>
    getTypeImportOnDemandImportDeclarations() {

        Collection<TypeImportOnDemandDeclaration> result = new ArrayList<>();
        for (TypeImportOnDemandDeclaration tiodd : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            TypeImportOnDemandDeclaration.class
        )) result.add(tiodd);

        result.add(new TypeImportOnDemandDeclaration(Location.NOWHERE, new String[] { "java", "lang" }));

        return result;
    }

    // ============================================ BYTECODE INSTRUCTIONS ============================================

    /**
     * Pushes one value on the operand stack and pushes the respective {@link VerificationTypeInfo} operand to the
     * stack map.
     * <table border="border">
     *   <tr><th><var>value</var></th><th>Operand stack value</th><th>Verification type info</th></tr>
     *   <tr>
     *     <td>
     *       {@link Character}
     *       <br />
     *       {@link Byte}
     *       <br />
     *       {@link Short}
     *       <br />
     *       {@link Integer}
     *       <br />
     *       {@link Boolean}
     *     </td>
     *     <td>{@code int}</td>
     *     <td>{@code integer_variable_info}</td>
     *   </tr>
     *   <tr><td>{@link Float}</td><td>{@code float}</td><td>{@code float_variable_info}</td></tr>
     *   <tr><td>{@link Long}</td><td>{@code msb+lsb} of {@code long}</td><td>{@code long_variable_info}</td></tr>
     *   <tr><td>{@link Double}</td><td>{@code msb+lsb} of {@code double}</td><td>{@code double_variable_info}</td></tr>
     *   <tr><td>{@link String}</td><td>{@code ConstantStringInfo} CP index</td><td>{@code object_variable_info(String)}</td></tr>
     *   <tr><td>{@link IClass}</td><td>{@code ConstantClassInfo} CP index</td><td>{@code object_variable_info(iClass.descriptor())}</td></tr>
     *   <tr><td>{@code null}</td><td>{@code null}</td><td>{@code null_variable_info(iClass.descriptor())}</td></tr>
     * </table>
     *
     * @return The computational type of the value that was pushed, e.g. {@link IClass#INT} for {@link Byte} or {@link
     *         IClass#NULL} for {@code null}
     */
    private IClass
    consT(Locatable locatable, @Nullable Object value) throws CompileException {

        if (value instanceof Character) {
            this.consT(locatable, ((Character) value).charValue());
            return IClass.INT;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            this.consT(locatable, ((Number) value).intValue());
            return IClass.INT;
        }
        if (Boolean.TRUE.equals(value)) {
            this.consT(locatable, 1);
            return IClass.BOOLEAN;
        }
        if (Boolean.FALSE.equals(value)) {
            this.consT(locatable, 0);
            return IClass.BOOLEAN;
        }
        if (value instanceof Float) {
            this.consT(locatable, ((Float) value).floatValue());
            return IClass.FLOAT;
        }
        if (value instanceof Long) {
            this.consT(locatable, ((Long) value).longValue());
            return IClass.LONG;
        }
        if (value instanceof Double) {
            this.consT(locatable, ((Double) value).doubleValue());
            return IClass.DOUBLE;
        }

        if (value instanceof String) {
            String   s  = (String) value;
            String[] ss = UnitCompiler.makeUtf8Able(s);
            this.consT(locatable, ss[0]);
            for (int i = 1; i < ss.length; ++i) {
                this.consT(locatable, ss[i]);
                this.bytecodeEmitter.invokeMethod(locatable, this.iClassLoader.METH_java_lang_String__concat__java_lang_String);
            }
            return this.iClassLoader.TYPE_java_lang_String;
        }

        if (value instanceof IClass) {
            this.consT(locatable, (IClass) value);
            return this.iClassLoader.TYPE_java_lang_Class;
        }

        if (value == null) {
            this.bytecodeEmitter.aconstnull(locatable);
            return IClass.NULL;
        }

        throw new InternalCompilerException(locatable.getLocation(), "Unknown literal \"" + value + "\"");
    }

    /**
     * Only strings that can be UTF8-encoded into 65535 bytes can be stored as a constant string info.
     *
     * @param s The string to split into suitable chunks
     * @return  Strings that can be UTF8-encoded into 65535 bytes
     */
    private static String[]
    makeUtf8Able(String s) {
        if (s.length() < (65536 / 3)) return new String[] { s };

        int          sLength = s.length(), utfLength = 0;
        int          from    = 0;
        List<String> l       = new ArrayList<>();
        for (int i = 0;; i++) {
            if (i == sLength) {
                l.add(s.substring(from));
                break;
            }
            if (utfLength >= 65532) {
                l.add(s.substring(from, i));
                if (i + (65536 / 3) > sLength) {
                    l.add(s.substring(i));
                    break;
                }
                from      = i;
                utfLength = 0;
            }
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                ++utfLength;
            } else
            if (c > 0x07FF) {
                utfLength += 3;
            } else
            {
                utfLength += 2;
            }
        }
        return (String[]) l.toArray(new String[l.size()]);

    }

    private void
    consT(Locatable locatable, IClass t, int value) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            this.consT(locatable, value);
        } else
        if (t == IClass.LONG) {
            this.consT(locatable, (long) value);
        } else
        if (t == IClass.FLOAT) {
            this.consT(locatable, (float) value);
        } else
        if (t == IClass.DOUBLE) {
            this.consT(locatable, (double) value);
        } else
        {
            throw new AssertionError(t);
        }
    }

    private void
    consT(Locatable locatable, int value) {

        this.bytecodeEmitter.addLineNumberOffset(locatable);

        if (value >= -1 && value <= 5) {
            this.bytecodeEmitter.write(Opcode.ICONST_0 + value);
        } else
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            this.bytecodeEmitter.write(Opcode.BIPUSH);
            this.bytecodeEmitter.writeByte(value);
        } else
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            this.bytecodeEmitter.write(Opcode.SIPUSH);
            this.bytecodeEmitter.writeShort(value);
        } else {
            this.bytecodeEmitter.writeLdc(this.bytecodeEmitter.addConstantIntegerInfo(value));
        }

        this.getCodeContext().pushIntOperand();
    }

    private void
    consT(Locatable locatable, long value) {

        this.bytecodeEmitter.addLineNumberOffset(locatable);

        if (value == 0L || value == 1L) {
            this.bytecodeEmitter.write(Opcode.LCONST_0 + (int) value);
        } else
        {
            this.bytecodeEmitter.writeLdc2(this.bytecodeEmitter.addConstantLongInfo(value));
        }

        this.getCodeContext().pushLongOperand();
    }

    private void
    consT(Locatable locatable, float value) {

        this.bytecodeEmitter.addLineNumberOffset(locatable);

        if (
            Float.floatToIntBits(value) == Float.floatToIntBits(0.0F) // POSITIVE zero!
            || value == 1.0F
            || value == 2.0F
        ) {
            this.bytecodeEmitter.write(Opcode.FCONST_0 + (int) value);
        } else {
            this.bytecodeEmitter.writeLdc(this.bytecodeEmitter.addConstantFloatInfo(value));
        }

        this.getCodeContext().pushFloatOperand();
    }

    private void
    consT(Locatable locatable, double value) {

        this.bytecodeEmitter.addLineNumberOffset(locatable);

        if (
            Double.doubleToLongBits(value) == Double.doubleToLongBits(0.0D) // POSITIVE zero!
            || value == 1.0D
        ) {
            this.bytecodeEmitter.write(Opcode.DCONST_0 + (int) value);
        } else {
            this.bytecodeEmitter.writeLdc2(this.bytecodeEmitter.addConstantDoubleInfo(value));
        }

        this.getCodeContext().pushDoubleOperand();
    }

    private void
    consT(Locatable locatable, final String s) {
        this.bytecodeEmitter.addLineNumberOffset(locatable);
        this.bytecodeEmitter.writeLdc(this.bytecodeEmitter.addConstantStringInfo(s));
        this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_STRING);
    }

    private void
    consT(Locatable locatable, IClass iClass) {
        this.bytecodeEmitter.addLineNumberOffset(locatable);
        this.bytecodeEmitter.writeLdc(this.bytecodeEmitter.addConstantClassInfo(iClass));
        this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_CLASS);
    }

    private void
    castConversion(
        Locatable        locatable,
        IType            sourceType,
        IType            targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        if (!this.tryCastConversion(locatable, sourceType, targetType, constantValue)) {
            this.compileError(
                "Cast conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"",
                locatable.getLocation()
            );
        }
    }

    private boolean
    tryCastConversion(
        Locatable        locatable,
        IType            sourceType,
        IType            targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        // Casting to a type variable is unchecked; assignment to it is still constrained by declaration identity.
        // Validate every upper bound, but emit the cast to its JVM erasure (JLS 5.5, 4.6).
        if (targetType instanceof SourceTypeVariable || targetType instanceof GenericArrayType) {
            if (!this.isTypeVariableCastConvertible(sourceType, targetType, new HashSet<TypeParameter>())) return false;
            return this.tryCastConversion(locatable, sourceType, UnitCompiler.rawTypeOf(targetType), constantValue);
        }
        return (
            this.tryAssignmentConversion(locatable, sourceType, targetType, constantValue)
            || this.tryNarrowingPrimitiveConversion(locatable, sourceType, targetType)
            || this.tryNarrowingReferenceConversion(locatable, sourceType, targetType)
        );
    }

    private boolean isTypeVariableCastConvertible(IType source, IType target, Set<TypeParameter> visiting)
    throws CompileException {
        if (source == IClass.NULL) return true;
        if (UnitCompiler.isPrimitive(source)) return false;
        if (target instanceof SourceTypeVariable) {
            TypeParameter parameter = ((SourceTypeVariable) target).declaration;
            if (!visiting.add(parameter)) return true;
            try {
                if (parameter.bound != null) for (ReferenceType bound : parameter.bound) {
                    if (!this.isTypeVariableCastConvertible(source, this.sourceBoundType(bound), visiting)) return false;
                }
                return true;
            } finally { visiting.remove(parameter); }
        }
        if (target instanceof GenericArrayType && UnitCompiler.isArray(source)) {
            IType component = UnitCompiler.getComponentType(source);
            if (UnitCompiler.isPrimitive(component)) return false;
            return this.isTypeVariableCastConvertible(component, ((GenericArrayType) target).component, visiting);
        }
        return this.isCastReferenceConvertible(source, target, visiting);
    }

    /** Conflicting parameterizations of a common supertype make a reference cast impossible. */
    private boolean distinctGenericSupertypes(IType source, IType target, Set<TypeParameter> visiting) throws CompileException {
        if (source.equals(target)) return false;
        if (UnitCompiler.isArray(source) && UnitCompiler.isArray(target)) {
            return this.distinctGenericSupertypes(UnitCompiler.getComponentType(source), UnitCompiler.getComponentType(target), visiting);
        }
        Map<IClass, IType> sources = new LinkedHashMap<>(), targets = new LinkedHashMap<>();
        this.collectSupertypes(source, sources);
        this.collectSupertypes(target, targets);
        for (Map.Entry<IClass, IType> entry : targets.entrySet()) {
            IType from = sources.get(entry.getKey()), to = entry.getValue();
            if (!(from instanceof ParameterizedIClass) || !(to instanceof ParameterizedIClass)) continue;
            IType[] a = ((ParameterizedIClass) from).actualTypeArguments;
            IType[] b = ((ParameterizedIClass) to).actualTypeArguments;
            if (a.length != b.length) continue; // A raw view carries no contradictory type arguments.
            for (int i = 0; i < a.length; i++) if (this.castTypeArgumentsDistinct(a[i], b[i], visiting)) return true;
            IType ownerA = ((ParameterizedIClass) from).ownerType, ownerB = ((ParameterizedIClass) to).ownerType;
            if (ownerA != null && ownerB != null && this.distinctGenericSupertypes(ownerA, ownerB, visiting)) return true;
        }
        return false;
    }

    private boolean castTypeArgumentsDistinct(IType a, IType b, Set<TypeParameter> visiting) throws CompileException {
        if (a.equals(b) || a instanceof CapturedType || b instanceof CapturedType) return false;
        if (a instanceof SourceTypeVariable || b instanceof SourceTypeVariable) {
            if (a instanceof SourceTypeVariable && b instanceof SourceTypeVariable) {
                TypeParameter declaration = ((SourceTypeVariable) a).declaration;
                if (!visiting.add(declaration)) return false;
                try { return !this.isCastReferenceConvertible(a, b, visiting); }
                finally { visiting.remove(declaration); }
            }
            SourceTypeVariable variable = (SourceTypeVariable) (a instanceof SourceTypeVariable ? a : b);
            IType value = a instanceof SourceTypeVariable ? b : a;
            if (!visiting.add(variable.declaration)) return false;
            try {
                if (variable.declaration.bound != null) for (ReferenceType reference : variable.declaration.bound) {
                    IType bound = this.sourceBoundType(reference);
                    if (value instanceof IWildcardType) {
                        IWildcardType wildcard = (IWildcardType) value;
                        IType lower = wildcard.getLowerBound();
                        if (lower != null ? !this.genericConvertible(lower, UnitCompiler.rawTypeOf(bound), false)
                            : !this.isCastReferenceConvertible(wildcard.getUpperBound(), bound, visiting)) return true;
                    } else if (!UnitCompiler.rawTypeOf(bound).isAssignableFrom(UnitCompiler.rawTypeOf(value))
                        || !this.isTypeVariableCastConvertible(value, bound, visiting)) return true;
                }
                return false;
            } finally { visiting.remove(variable.declaration); }
        }
        if (a instanceof IWildcardType || b instanceof IWildcardType) {
            if (a instanceof IWildcardType && b instanceof IWildcardType) {
                IWildcardType wa = (IWildcardType) a, wb = (IWildcardType) b;
                IType la = wa.getLowerBound(), lb = wb.getLowerBound();
                if (la != null && lb != null) return false;
                if (la != null) return !this.genericConvertible(la, wb.getUpperBound(), false);
                if (lb != null) return !this.genericConvertible(lb, wa.getUpperBound(), false);
                return !this.isCastReferenceConvertible(wa.getUpperBound(), wb.getUpperBound(), visiting);
            }
            IWildcardType wildcard = (IWildcardType) (a instanceof IWildcardType ? a : b);
            IType value = a instanceof IWildcardType ? b : a;
            IType lower = wildcard.getLowerBound();
            IType bound = lower == null ? wildcard.getUpperBound() : lower;
            if (bound instanceof SourceTypeVariable) return !this.isTypeVariableCastConvertible(value, bound, visiting);
            if (bound instanceof CapturedType) {
                CapturedType capture = (CapturedType) bound;
                if (capture.lower != null && !this.genericConvertible(capture.lower, value, false)) return true;
                for (IType limit : capture.bounds) if (!this.isCastReferenceConvertible(value, limit, visiting)) return true;
                return false;
            }
            return lower == null
                ? !this.genericConvertible(value, wildcard.getUpperBound(), false)
                : !this.genericConvertible(lower, value, false);
        }
        if (a instanceof ParameterizedIClass && b instanceof ParameterizedIClass
            && ((ParameterizedIClass) a).rawType == ((ParameterizedIClass) b).rawType
            && (this.containsFreeTypeVariable(a) || this.containsFreeTypeVariable(b))) {
            ParameterizedIClass pa = (ParameterizedIClass) a, pb = (ParameterizedIClass) b;
            if (pa.actualTypeArguments.length != pb.actualTypeArguments.length) return false;
            for (int i = 0; i < pa.actualTypeArguments.length; i++) {
                if (this.castTypeArgumentsDistinct(pa.actualTypeArguments[i], pb.actualTypeArguments[i], visiting)) return true;
            }
            return pa.ownerType != null && pb.ownerType != null && this.castTypeArgumentsDistinct(pa.ownerType, pb.ownerType, visiting);
        }
        // Proper type arguments are invariant, including nested parameterized and array types.
        // In particular, List<?> and List<String> are distinct when used as arguments of another List.
        return true;
    }

    private boolean containsFreeTypeVariable(IType type) throws CompileException {
        if (type instanceof SourceTypeVariable || type instanceof CapturedType) return true;
        if (UnitCompiler.isArray(type)) return this.containsFreeTypeVariable(UnitCompiler.getComponentType(type));
        if (type instanceof ParameterizedIClass) {
            ParameterizedIClass parameter = (ParameterizedIClass) type;
            if (parameter.ownerType != null && this.containsFreeTypeVariable(parameter.ownerType)) return true;
            for (IType argument : parameter.actualTypeArguments) if (this.containsFreeTypeVariable(argument)) return true;
        }
        return false;
    }

    /**
     * Implements "assignment conversion" (JLS7 5.2) (more precisely: "assignment contexts".)
     */
    private void
    assignmentConversion(
        Locatable        locatable,
        IType            sourceType,
        IType            targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        if (!this.tryAssignmentConversion(locatable, sourceType, targetType, constantValue)) {
            this.compileError(
                "Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"",
                locatable.getLocation()
            );
        }
    }

    private boolean
    tryAssignmentConversion(
        Locatable        locatable,
        IType            sourceType,
        IType            targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        UnitCompiler.LOGGER.entering(
            null,
            "tryAssignmentConversion",
            new Object[] { locatable, sourceType, targetType, constantValue }
        );

        if ((targetType instanceof ParameterizedIClass || targetType instanceof SourceTypeVariable || targetType instanceof CapturedType || targetType instanceof GenericArrayType)
            && !this.genericConvertible(sourceType, targetType, true)) return false;

        if (sourceType instanceof GenericArrayType && UnitCompiler.isArray(targetType) && this.genericConvertible(sourceType, targetType, false)) {
            if (!UnitCompiler.rawTypeOf(targetType).isAssignableFrom(UnitCompiler.rawTypeOf(sourceType))) this.bytecodeEmitter.checkcast(locatable, targetType);
            return true;
        }
        // An intersection variable can widen to any bound, even when its JVM erasure is the first bound.
        if ((sourceType instanceof CapturedType || sourceType instanceof SourceTypeVariable) && !UnitCompiler.isPrimitive(targetType)
            && this.genericConvertible(sourceType, targetType, false)) {
            if (!UnitCompiler.rawTypeOf(targetType).isAssignableFrom(UnitCompiler.rawTypeOf(sourceType))) this.bytecodeEmitter.checkcast(locatable, targetType);
            return true;
        }
        // JLS7 5.1.1 Identity conversion.
        if (this.tryIdentityConversion(sourceType, targetType)) return true;

        // JLS7 5.1.2 Widening primitive conversion.
        if (this.tryWideningPrimitiveConversion(locatable, sourceType, targetType)) return true;

        // JLS7 5.1.4 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) {
            this.getCodeContext().popOperand(sourceType == IClass.NULL ? Descriptor.VOID : UnitCompiler.rawTypeOf(sourceType).getDescriptor());
            this.getCodeContext().pushOperand(UnitCompiler.rawTypeOf(targetType).getDescriptor());
            return true;
        }

        // A boxing conversion (JLS7 5.1.7) optionally followed by a widening reference conversion.
        {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                if (this.tryIdentityConversion(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    return true;
                }
                if (this.isWideningReferenceConvertible(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    this.getCodeContext().popOperand(boxedType.getDescriptor());
                    this.getCodeContext().pushOperand(UnitCompiler.rawTypeOf(targetType).getDescriptor());
                    return true;
                }
            }
        }

        // An unboxing conversion (JLS7 5.1.8) optionally followed by a widening primitive conversion.
        {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                if (this.tryIdentityConversion(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    return true;
                }
                if (this.isWideningPrimitiveConvertible(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    this.tryWideningPrimitiveConversion(locatable, unboxedType, targetType);
                    return true;
                }
            }
        }

        // Constant value narrowing primitive conversion.
        if (constantValue != UnitCompiler.NOT_CONSTANT) {
            if (this.tryConstantAssignmentConversion(
                locatable,
                constantValue, // constantValue
                targetType     // targetType
            )) return true;
        }

        if (this.tryNullConversion(sourceType, targetType)) return true;

        return false;
    }

    /**
     * Implements "assignment conversion" (JLS7 5.2) on a constant value.
     *
     * @param value Must be a {@link Boolean}, {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link
     *              Character}, {@link Long}, {@link Float}, {@link Double} or {@code null}
     * @return      A {@link Boolean}, {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Character},
     *              {@link Long}, {@link Float}, {@link Double} or {@code null} (representing the {@code null} literal)
     */
    @Nullable private Object
    constantAssignmentConversion(Locatable locatable, @Nullable Object value, IType targetType) throws CompileException {

        if (value == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

        if (targetType == IClass.BOOLEAN) {
            if (value instanceof Boolean) return value;
        } else
        if (targetType == this.iClassLoader.TYPE_java_lang_String) {
            if (value instanceof String || value == null) return value;
        } else
        if (targetType == IClass.BYTE) {
            if (value instanceof Byte) {
                return value;
            } else
            if (value instanceof Short || value instanceof Integer) {
                assert value != null;
                int x = ((Number) value).intValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) return Byte.valueOf((byte) x);
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) return Byte.valueOf((byte) x);
            }
        } else
        if (targetType == IClass.SHORT) {
            if (value instanceof Byte) {
                return Short.valueOf(((Number) value).shortValue());
            } else
            if (value instanceof Short) {
                return value;
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) return Short.valueOf((short) x);
            } else
            if (value instanceof Integer) {
                int x = ((Integer) value).intValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) return Short.valueOf((short) x);
            }
        } else
        if (targetType == IClass.CHAR) {
            if (value instanceof Character) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                assert value != null;
                int x = ((Number) value).intValue();
                if (x >= Character.MIN_VALUE && x <= Character.MAX_VALUE) return Character.valueOf((char) x);
            }
        } else
        if (targetType == IClass.INT) {
            if (value instanceof Integer) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short) {
                assert value != null;
                return Integer.valueOf(((Number) value).intValue());
            } else
            if (value instanceof Character) {
                return Integer.valueOf(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.LONG) {
            if (value instanceof Long) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                assert value != null;
                return Long.valueOf(((Number) value).longValue());
            } else
            if (value instanceof Character) {
                return Long.valueOf(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.FLOAT) {
            if (value instanceof Float) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                assert value != null;
                return Float.valueOf(((Number) value).floatValue());
            } else
            if (value instanceof Character) {
                return Float.valueOf(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.DOUBLE) {
            if (value instanceof Double) {
                return value;
            } else
            if (
                value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
            ) {
                assert value != null;
                return Double.valueOf(((Number) value).doubleValue());
            } else
            if (value instanceof Character) {
                return Double.valueOf(((Character) value).charValue());
            }
        } else
        if (value == null && !UnitCompiler.isPrimitive(targetType)) {
            return null;
        } else
        if (value instanceof String && UnitCompiler.isAssignableFrom(targetType, this.iClassLoader.TYPE_java_lang_String)) {
            return value;
        }

        if (value == null) {
            this.compileError(
                "Cannot convert 'null' to type \"" + targetType.toString() + "\"",
                locatable.getLocation()
            );
        } else
        {
            this.compileError((
                "Cannot convert constant of type \""
                + value.getClass().getName()
                + "\" to type \""
                + targetType.toString()
                + "\""
            ), locatable.getLocation());
        }
        return value;
    }

    /**
     * Implements "unary numeric promotion" (JLS7 5.6.1).
     *
     * @return The promoted type
     */
    private IClass
    unaryNumericPromotion(Locatable locatable, IType type) throws CompileException {
        type = this.convertToPrimitiveNumericType(locatable, type);

        IClass promotedType = this.unaryNumericPromotionType(locatable, type);

        this.numericPromotion(locatable, type, promotedType);
        return promotedType;
    }

    private void
    reverseUnaryNumericPromotion(Locatable locatable, IClass sourceType, IType targetType) {
        IClass unboxedType = this.isUnboxingConvertible(targetType);
        IType  pt          = unboxedType != null ? unboxedType : targetType;
        if (
            !this.tryIdentityConversion(sourceType, pt)
            && !this.tryNarrowingPrimitiveConversion(
                locatable,  // locatable
                sourceType, // sourceType
                pt          // targetType
            )
        ) throw new InternalCompilerException(locatable.getLocation(), "SNO: reverse unary numeric promotion failed");
        if (unboxedType != null) this.boxingConversion(locatable, unboxedType, targetType);
    }

    /**
     * If the given type is a primitive type, return that type. If the given type is a primitive wrapper class, unbox
     * the operand on top of the operand stack and return the primitive type. Otherwise, issue a compile error.
     */
    private IClass
    convertToPrimitiveNumericType(Locatable locatable, IType type) throws CompileException {

        if ((type instanceof IClass) && ((IClass) type).isPrimitiveNumeric()) return (IClass) type;

        IClass unboxedType = this.isUnboxingConvertible(type);
        if (unboxedType != null) {
            this.unboxingConversion(locatable, type, unboxedType);
            return unboxedType;
        }
        this.compileError(
            "Object of type \"" + type.toString() + "\" cannot be converted to a numeric type",
            locatable.getLocation()
        );
        return IClass.INT;
    }

    private void
    numericPromotion(Locatable locatable, IType sourceType, IClass targetType) {
        if (
            !this.tryIdentityConversion(sourceType, targetType)
            && !this.tryWideningPrimitiveConversion(
                locatable,  // locatable
                sourceType, // sourceType
                targetType  // targetType
            )
        ) throw new InternalCompilerException(locatable.getLocation(), "SNO: Conversion failed");
    }

    private IClass
    unaryNumericPromotionType(Locatable locatable, IType type) throws CompileException {

        if (!(type instanceof IClass) || !((IClass) type).isPrimitiveNumeric()) {
            this.compileError(
                "Unary numeric promotion not possible on non-numeric-primitive type \"" + type + "\"",
                locatable.getLocation()
            );
        }

        return (
            type == IClass.DOUBLE ? IClass.DOUBLE :
            type == IClass.FLOAT  ? IClass.FLOAT  :
            type == IClass.LONG   ? IClass.LONG   :
            IClass.INT
        );
    }

    private IClass
    binaryNumericPromotionType(Locatable locatable, IType type1, IType type2) throws CompileException {
        if (
            !(type1 instanceof IClass)
            || !((IClass) type1).isPrimitiveNumeric()
            || !(type2 instanceof IClass)
            || !((IClass) type2).isPrimitiveNumeric()
        ) {
            this.compileError(
                "Binary numeric promotion not possible on types \"" + type1 + "\" and \"" + type2 + "\"",
                locatable.getLocation()
            );
        }

        return (
            type1 == IClass.DOUBLE || type2 == IClass.DOUBLE ? IClass.DOUBLE :
            type1 == IClass.FLOAT  || type2 == IClass.FLOAT  ? IClass.FLOAT  :
            type1 == IClass.LONG   || type2 == IClass.LONG   ? IClass.LONG   :
            IClass.INT
        );
    }

    /**
     * Checks whether "identity conversion" (5.1.1) is possible.
     *
     * @return Whether the conversion is possible
     */
    @SuppressWarnings("static-method") private boolean
    isIdentityConvertible(IType sourceType, IType targetType) { return sourceType == targetType; }

    /**
     * Implements "identity conversion" (5.1.1).
     *
     * @return Whether the conversion was possible
     */
    @SuppressWarnings("static-method") private boolean
    tryIdentityConversion(IType sourceType, IType targetType) { return sourceType == targetType; }

    @SuppressWarnings("static-method") private boolean
    isWideningPrimitiveConvertible(IClass sourceType, IType targetType) {
        return UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + UnitCompiler.rawTypeOf(targetType).getDescriptor()
        ) != null;
    }

    /**
     * Implements "widening primitive conversion" (5.1.2).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryWideningPrimitiveConversion(Locatable locatable, IType sourceType, IType targetType) {

        if (sourceType instanceof IParameterizedType) return false;

        if (targetType instanceof IParameterizedType) return false;
        IClass targetClass = (IClass) targetType;

        int[] opcodes = (int[]) UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            UnitCompiler.rawTypeOf(sourceType).getDescriptor() + targetClass.getDescriptor()
        );
        if (opcodes != null) {
            this.bytecodeEmitter.addLineNumberOffset(locatable);
            for (int opcode : opcodes) this.bytecodeEmitter.write(opcode);
            this.getCodeContext().popOperand();
            this.getCodeContext().pushOperand(targetClass.getDescriptor());
            return true;
        }
        return false;
    }

    private static final Map<String /*descriptor*/, int[] /*opcodes*/>
    PRIMITIVE_WIDENING_CONVERSIONS = new HashMap<>();

    static { UnitCompiler.fillConversionMap(new Object[] {

        new int[0],
        Descriptor.BYTE  + Descriptor.SHORT,
        Descriptor.BYTE  + Descriptor.INT,
        Descriptor.SHORT + Descriptor.INT,
        Descriptor.CHAR  + Descriptor.INT,

        new int[] { Opcode.I2L },
        Descriptor.BYTE  + Descriptor.LONG,
        Descriptor.SHORT + Descriptor.LONG,
        Descriptor.CHAR  + Descriptor.LONG,
        Descriptor.INT   + Descriptor.LONG,

        new int[] { Opcode.I2F },
        Descriptor.BYTE  + Descriptor.FLOAT,
        Descriptor.SHORT + Descriptor.FLOAT,
        Descriptor.CHAR  + Descriptor.FLOAT,
        Descriptor.INT   + Descriptor.FLOAT,

        new int[] { Opcode.L2F },
        Descriptor.LONG  + Descriptor.FLOAT,

        new int[] { Opcode.I2D },
        Descriptor.BYTE  + Descriptor.DOUBLE,
        Descriptor.SHORT + Descriptor.DOUBLE,
        Descriptor.CHAR  + Descriptor.DOUBLE,
        Descriptor.INT   + Descriptor.DOUBLE,

        new int[] { Opcode.L2D },
        Descriptor.LONG  + Descriptor.DOUBLE,

        new int[] { Opcode.F2D },
        Descriptor.FLOAT + Descriptor.DOUBLE,
    }, UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS); }
    private static void
    fillConversionMap(Object[] array, Map<String /*descriptor*/, int[] /*opcodes*/> map) {
        int[] opcodes = null;
        for (Object o : array) {
            if (o instanceof int[]) {
                opcodes = (int[]) o;
            } else {
                map.put((String) o, opcodes);
            }
        }
    }

    /**
     * Checks if "widening reference conversion" (5.1.4) is possible.
     *
     * @return Whether the conversion is possible
     */
    @SuppressWarnings("static-method") private boolean
    isWideningReferenceConvertible(IType sourceType, IType targetType) throws CompileException {

        IClass sourceClass = UnitCompiler.rawTypeOf(sourceType);
        IClass targetClass = UnitCompiler.rawTypeOf(targetType);

        if (targetClass.isPrimitive() || sourceType == targetType) return false;

        // JLS11 5.1.5: "The null type is not a reference type, and so a widening reference conversion does not exist
        // from the null type to a reference type."
        if (sourceClass == IClass.NULL) return false;

        return targetClass.isAssignableFrom(sourceClass);
    }

    /**
     * Checks whether "narrowing primitive conversion" (JLS7 5.1.3) is possible.
     */
    @SuppressWarnings("static-method") private boolean
    isNarrowingPrimitiveConvertible(IType sourceType, IType targetType) {
        return UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.containsKey(
            UnitCompiler.rawTypeOf(sourceType).getDescriptor() + UnitCompiler.rawTypeOf(targetType).getDescriptor()
        );
    }

    /**
     * Implements "narrowing primitive conversion" (JLS7 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryNarrowingPrimitiveConversion(Locatable locatable, IType sourceType, IType targetType) {

        if (!(sourceType instanceof IClass) || !(targetType instanceof IClass)) return false;

        IClass sourceClass = (IClass) sourceType;
        IClass targetClass = (IClass) targetType;

        int[] opcodes = (int[]) UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.get(
            sourceClass.getDescriptor() + targetClass.getDescriptor()
        );
        if (opcodes != null) {
            this.bytecodeEmitter.addLineNumberOffset(locatable);
            for (int opcode : opcodes) this.bytecodeEmitter.write(opcode);
            this.getCodeContext().popOperand();
            this.getCodeContext().pushOperand(targetClass.getDescriptor());
            return true;
        }
        return false;
    }

    private static final Map<String /*descriptor*/, int[] /*opcodes*/>
    PRIMITIVE_NARROWING_CONVERSIONS = new HashMap<>();

    static { UnitCompiler.fillConversionMap(new Object[] {

        new int[0],
        Descriptor.BYTE + Descriptor.CHAR,
        Descriptor.SHORT + Descriptor.CHAR,
        Descriptor.CHAR + Descriptor.SHORT,

        new int[] { Opcode.I2B },
        Descriptor.SHORT + Descriptor.BYTE,
        Descriptor.CHAR + Descriptor.BYTE,
        Descriptor.INT + Descriptor.BYTE,

        new int[] { Opcode.I2S },
        Descriptor.INT + Descriptor.SHORT,

        new int[] { Opcode.I2C },
        Descriptor.INT + Descriptor.CHAR,

        new int[] { Opcode.L2I, Opcode.I2B },
        Descriptor.LONG + Descriptor.BYTE,

        new int[] { Opcode.L2I, Opcode.I2S },
        Descriptor.LONG + Descriptor.SHORT,
        Descriptor.LONG + Descriptor.CHAR,

        new int[] { Opcode.L2I },
        Descriptor.LONG + Descriptor.INT,

        new int[] { Opcode.F2I, Opcode.I2B },
        Descriptor.FLOAT + Descriptor.BYTE,

        new int[] { Opcode.F2I, Opcode.I2S },
        Descriptor.FLOAT + Descriptor.SHORT,
        Descriptor.FLOAT + Descriptor.CHAR,

        new int[] { Opcode.F2I },
        Descriptor.FLOAT + Descriptor.INT,

        new int[] { Opcode.F2L },
        Descriptor.FLOAT + Descriptor.LONG,

        new int[] { Opcode.D2I, Opcode.I2B },
        Descriptor.DOUBLE + Descriptor.BYTE,

        new int[] { Opcode.D2I, Opcode.I2S },
        Descriptor.DOUBLE + Descriptor.SHORT,
        Descriptor.DOUBLE + Descriptor.CHAR,

        new int[] { Opcode.D2I },
        Descriptor.DOUBLE + Descriptor.INT,

        new int[] { Opcode.D2L },
        Descriptor.DOUBLE + Descriptor.LONG,

        new int[] { Opcode.D2F },
        Descriptor.DOUBLE + Descriptor.FLOAT,
    }, UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS); }

    /**
     * Checks if "constant assignment conversion" (JLS7 5.2, paragraph 1) is possible.
     *
     * @param constantValue The constant value that is to be converted
     * @param targetType    The type to convert to
     */
    private boolean
    tryConstantAssignmentConversion(Locatable locatable, @Nullable Object constantValue, IType targetType) {
        UnitCompiler.LOGGER.entering(
            null,
            "tryConstantAssignmentConversion",
            new Object[] { locatable, constantValue, targetType }
        );

        int cv;
        if (constantValue instanceof Byte) {
            cv = ((Byte) constantValue).byteValue();
        } else
        if (constantValue instanceof Short) {
            cv = ((Short) constantValue).shortValue();
        } else
        if (constantValue instanceof Integer) {
            cv = ((Integer) constantValue).intValue();
        } else
        if (constantValue instanceof Character) {
            cv = ((Character) constantValue).charValue();
        } else
        {
            return false;
        }

        if (targetType == IClass.BYTE)  return cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE;
        if (targetType == IClass.SHORT) return cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE;
        if (targetType == IClass.CHAR)  return cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE;

        IClassLoader icl = this.iClassLoader;
        if (targetType == icl.TYPE_java_lang_Byte && cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.BYTE, targetType);
            return true;
        }
        if (targetType == icl.TYPE_java_lang_Short && cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.SHORT, targetType);
            return true;
        }
        if (targetType == icl.TYPE_java_lang_Character && cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.CHAR, targetType);
            return true;
        }

        return false;
    }

    /**
     * Checks whether "narrowing reference conversion" (JLS7 5.1.5) is possible.
     */
    private boolean
    isNarrowingReferenceConvertible(IType sourceType, IType targetType) throws CompileException {
        if (UnitCompiler.rawTypeOf(sourceType).isPrimitive()) return false;
        if (sourceType == targetType) return false;

        IClass rawSourceType = UnitCompiler.rawTypeOf(sourceType);
        IClass rawTargetType = UnitCompiler.rawTypeOf(targetType);

        // 5.1.5.1
        if (rawSourceType.isAssignableFrom(rawTargetType)) return true;

        // 5.1.5.2
        if (rawTargetType.isInterface() && !rawSourceType.isFinal() && !rawTargetType.isAssignableFrom(rawSourceType)) return true;

        // 5.1.5.3
        if (sourceType == this.iClassLoader.TYPE_java_lang_Object && rawTargetType.isArray()) return true;

        // 5.1.5.4
        if (sourceType == this.iClassLoader.TYPE_java_lang_Object && rawTargetType.isInterface()) return true;

        // 5.1.5.5
        if (rawSourceType.isInterface() && !rawTargetType.isFinal()) return true;

        // 5.1.5.6
        if (rawSourceType.isInterface() && rawTargetType.isFinal() && rawSourceType.isAssignableFrom(rawTargetType)) return true;

        // 5.1.5.7
        // TODO: Check for redefinition of methods with same signature but different return type.
        if (rawSourceType.isInterface() && rawTargetType.isInterface() && !rawTargetType.isAssignableFrom(rawSourceType)) {
            return true;
        }

        // 5.1.5.8
        if (rawSourceType.isArray() && rawTargetType.isArray()) {

            IType st = rawSourceType.getComponentType();
            assert st != null;

            IType tt = rawTargetType.getComponentType();
            assert tt != null;

            if (this.isNarrowingPrimitiveConvertible(st, tt) || this.isNarrowingReferenceConvertible(st, tt)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implements "narrowing reference conversion" (5.1.5).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryNarrowingReferenceConversion(Locatable locatable, IType sourceType, IType targetType) throws CompileException {

        if (!this.isNarrowingReferenceConvertible(sourceType, targetType)) return false;

        this.bytecodeEmitter.checkcast(locatable, targetType);
        return true;
    }

    /**
     * JLS7 5.5
     */
    private boolean
    isCastReferenceConvertible(IType sourceType, IType targetType) throws CompileException {
        return this.isCastReferenceConvertible(sourceType, targetType, new HashSet<TypeParameter>());
    }

    private boolean isCastReferenceConvertible(IType sourceType, IType targetType, Set<TypeParameter> visiting)
    throws CompileException {
        // A widening conversion to a proper target is already safe. In particular, self-referential
        // supertypes such as BaseStream<T, Stream<T>> must not turn Stream<T> -> Stream<? extends T>
        // into a spurious invariant-argument conflict. Free target variables still need bound checks.
        if (!this.containsFreeTypeVariable(targetType) && this.genericConvertible(sourceType, targetType, false)) return true;
        return (
            this.isIdentityConvertible(sourceType, targetType)
            || this.isWideningReferenceConvertible(sourceType, targetType)
            || this.isNarrowingReferenceConvertible(sourceType, targetType)
        ) && !this.distinctGenericSupertypes(sourceType, targetType, visiting);
    }

    /**
     * @return The boxed type or {@code null}
     */
    @Nullable private IClass
    isBoxingConvertible(IType sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == IClass.BOOLEAN) return icl.TYPE_java_lang_Boolean;
        if (sourceType == IClass.BYTE)    return icl.TYPE_java_lang_Byte;
        if (sourceType == IClass.CHAR)    return icl.TYPE_java_lang_Character;
        if (sourceType == IClass.SHORT)   return icl.TYPE_java_lang_Short;
        if (sourceType == IClass.INT)     return icl.TYPE_java_lang_Integer;
        if (sourceType == IClass.LONG)    return icl.TYPE_java_lang_Long;
        if (sourceType == IClass.FLOAT)   return icl.TYPE_java_lang_Float;
        if (sourceType == IClass.DOUBLE)  return icl.TYPE_java_lang_Double;
        return null;
    }

    private boolean
    tryBoxingConversion(Locatable locatable, IType sourceType, IType targetType) {
        if (this.isBoxingConvertible(sourceType) == targetType) {
            this.boxingConversion(locatable, sourceType, targetType);
            return true;
        }
        return false;
    }

    @SuppressWarnings("static-method") private boolean
    isNullConvertible(IType sourceType, IType targetType) {

        return (
            sourceType == IClass.NULL
            && !UnitCompiler.isPrimitive(targetType)
        );
    }

    /**
     * JLS11 5.2: "Finally, a value of the null type (the null reference is the only such value) may be assigned to any
     * reference type".
     */
    private boolean
    tryNullConversion(IType sourceType, IType targetType) {

        if (this.isNullConvertible(sourceType, targetType)) {
            this.getCodeContext().popNullOperand();
            this.getCodeContext().pushOperand(UnitCompiler.rawTypeOf(targetType).getDescriptor());
            return true;
        }

        return false;
    }

    /**
     * @param sourceType a primitive type (except VOID)
     * @param targetType the corresponding wrapper type
     */
    private void
    boxingConversion(Locatable locatable, IType sourceType, IType targetType) {

        assert targetType instanceof IClass;
        IClass targetClass = (IClass) targetType;

        assert sourceType instanceof IClass;
        IClass sourceClass = (IClass) sourceType;

        this.bytecodeEmitter.invoke(
            locatable,            // locatable
            Opcode.INVOKESTATIC,  // opcode
            targetClass,          // declaringIClass
            "valueOf",            // methodName
            new MethodDescriptor( // methodFD
                targetClass.getDescriptor(), // returnFd
                sourceClass.getDescriptor()  // parameterFds...
            ),
            false                 // useInterfaceMethodref
        );
    }

    /**
     * @return Iff <var>sourceType</var> is a primitive wrapper type, the unboxed type, otherwise {@code null}
     */
    @Nullable private IClass
    isUnboxingConvertible(IType sourceType) {
        sourceType = UnitCompiler.rawTypeOf(sourceType);
        IClassLoader icl = this.iClassLoader;
        if (sourceType == icl.TYPE_java_lang_Boolean)   return IClass.BOOLEAN;
        if (sourceType == icl.TYPE_java_lang_Byte)      return IClass.BYTE;
        if (sourceType == icl.TYPE_java_lang_Character) return IClass.CHAR;
        if (sourceType == icl.TYPE_java_lang_Short)     return IClass.SHORT;
        if (sourceType == icl.TYPE_java_lang_Integer)   return IClass.INT;
        if (sourceType == icl.TYPE_java_lang_Long)      return IClass.LONG;
        if (sourceType == icl.TYPE_java_lang_Float)     return IClass.FLOAT;
        if (sourceType == icl.TYPE_java_lang_Double)    return IClass.DOUBLE;
        return null;
    }

    /**
     * @return Whether the <var>sourceType</var> is a primitive numeric type, or a wrapper type of a primitive numeric
     *         type
     */
    private boolean
    isConvertibleToPrimitiveNumeric(IType sourceType) {

        if (sourceType instanceof IClass && ((IClass) sourceType).isPrimitiveNumeric()) return true;

        IClass unboxedType = this.isUnboxingConvertible(sourceType);
        return unboxedType != null && unboxedType.isPrimitiveNumeric();
    }

    /**
     * @param targetType a primitive type (except VOID)
     * @param sourceType the corresponding wrapper type
     */
    private void
    unboxingConversion(Locatable locatable, IType sourceType, IClass targetType) {

        sourceType = UnitCompiler.rawTypeOf(sourceType);
        // "source.intValue()"
        this.bytecodeEmitter.invoke(
            locatable,                                                       // locatable
            Opcode.INVOKEVIRTUAL,                                            // opcode
            (IClass) sourceType,                                             // declaringIClass
            targetType.toString() + "Value",                                 // methodName
            new MethodDescriptor(targetType.getDescriptor(), new String[0]), // methodMd
            false                                                            // useInterfaceMethodref
        );
    }

    /**
     * Attempts to load an {@link IClass} by fully-qualified name through {@link #iClassLoader}.
     *
     * @param identifiers       The fully qualified type name, e.g. '{@code { "pkg", "Outer", "Inner" }}'
     * @return                  {@code null} if a class with the given name could not be loaded
     * @throws CompileException The type exists, but a problem occurred when it was loaded
     */
    @Nullable private IClass
    findTypeByFullyQualifiedName(Location location, String[] identifiers) throws CompileException {

        // Try all 'flavors', i.e. 'a.b.c', 'a.b$c', 'a$b$c'.
        String className = Java.join(identifiers, ".");
        for (;;) {
            IClass iClass = UnitCompiler.this.findTypeByName(location, className);
            if (iClass != null) return iClass;

            int idx = className.lastIndexOf('.');
            if (idx == -1) break;
            className = className.substring(0, idx) + '$' + className.substring(idx + 1);
        }

        return null;
    }

    /**
     * @param opIdx       One of {@link UnitCompilerBytecodeEmitter#EQ}, {@link UnitCompilerBytecodeEmitter#NE}, {@link UnitCompilerBytecodeEmitter#LT}, {@link UnitCompilerBytecodeEmitter#GE}, {@link UnitCompilerBytecodeEmitter#GT} or {@link UnitCompilerBytecodeEmitter#LE}
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    ifNumeric(Locatable locatable, int opIdx, Offset dst, boolean orientation) {
        assert opIdx >= UnitCompilerBytecodeEmitter.EQ && opIdx <= UnitCompilerBytecodeEmitter.LE;

        VerificationTypeInfo topOperand = this.getCodeContext().peekOperand();

        if (topOperand == StackMapTableAttribute.INTEGER_VARIABLE_INFO) {
            this.bytecodeEmitter.if_icmpxx(locatable, orientation == UnitCompiler.JUMP_IF_FALSE ? opIdx ^ 1 : opIdx, dst);
        } else
        if (
            topOperand == StackMapTableAttribute.LONG_VARIABLE_INFO
            || topOperand == StackMapTableAttribute.FLOAT_VARIABLE_INFO
            || topOperand == StackMapTableAttribute.DOUBLE_VARIABLE_INFO
        ) {

            // Important: The opIdx here must be the *original* opIdx (and not the possibly negated!), because that is
            // important for floating-point comparison! See PR #145.
            this.bytecodeEmitter.cmp(locatable, opIdx);

            this.bytecodeEmitter.ifxx(locatable, orientation == UnitCompiler.JUMP_IF_FALSE ? opIdx ^ 1 : opIdx, dst);
        } else
        {
            throw new InternalCompilerException(locatable.getLocation(), "Unexpected computational type \"" + topOperand + "\"");
        }
    }

    /**
     * Finds a named field in the given {@link IClass}. Honors superclasses and interfaces. See JLS7 8.3.
     *
     * @return {@code null} if no field is found
     */
    @Nullable private IClass.IField
    findIField(IClass iClass, String name, Location location) throws CompileException {

        // Search for a field with the given name in the current class.
        IClass.IField f = iClass.getDeclaredIField(name);
        if (f != null) return f;

        // Examine superclass.
        {
            IClass superclass = iClass.getSuperclass();
            if (superclass != null) {
                f = this.findIField(superclass, name, location);
                if (f != null && !this.inheritsField(iClass, f)) f = null;
            }
        }

        // Examine interfaces.
        IClass[] ifs = iClass.getInterfaces();
        for (IClass iF : ifs) {
            IClass.IField f2 = this.findIField(iF, name, location);
            if (f2 != null && this.inheritsField(iClass, f2)) {
                if (f != null && f.getDeclaringIClass() != f2.getDeclaringIClass()) {
                    throw new CompileException((
                        "Access to field \""
                        + name
                        + "\" is ambiguous - both \""
                        + f.getDeclaringIClass()
                        + "\" and \""
                        + f2.getDeclaringIClass()
                        + "\" declare it"
                    ), location);
                }
                f = f2;
            }
        }
        return f;
    }

    private boolean inheritsField(IClass type, IField field) {
        return field.getAccess() != Access.PRIVATE && (field.getAccess() != Access.DEFAULT
            || Descriptor.areInSamePackage(type.getDescriptor(), field.getDeclaringIClass().getDescriptor()));
    }

    /**
     * Finds a named type in the given {@link IClass} or {@link IParameterizedType}. Honors superclasses, interfaces
     * and enclosing type declarations.
     *
     * @return {@code null} if no type with the given name is found
     */
    @Nullable private IClass
    findMemberType(IType iType, String name, @Nullable TypeArgument[] typeArguments, Location location)
    throws CompileException {

        IClass[] types = UnitCompiler.rawTypeOf(iType).findMemberType(name);
        if (types.length == 0) return null;
        if (types.length == 1) return types[0];

        StringBuilder sb = new StringBuilder("Type \"").append(name).append("\" is ambiguous: ").append(types[0]);
        for (int i = 1; i < types.length; ++i) sb.append(" vs. ").append(types[i].toString());
        this.compileError(sb.toString(), location);
        return types[0];
    }

    /**
     * Finds one class or interface declaration in this compilation unit and resolves it into an {@link IClass}.
     *
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return          {@code null} if a class or an interface with that name is not declared in this compilation unit
     */
    @Nullable public IClass
    findClass(String className) {

        AbstractCompilationUnit acu = this.abstractCompilationUnit;
        if (!(acu instanceof CompilationUnit)) return null;
        CompilationUnit cu = (CompilationUnit) acu;

        // Examine package name.
        PackageDeclaration opd = cu.packageDeclaration;
        if (opd != null) {
            String packageName = opd.packageName;
            if (!className.startsWith(packageName + '.')) return null;
            className = className.substring(packageName.length() + 1);
        }

        // Attempt to find the type declaration by name "as is", i.e. including any dollar signs.
        TypeDeclaration td = cu.getPackageMemberTypeDeclaration(className);
        if (td == null) {

            int idx = className.indexOf('$');
            if (idx == -1) return null;
            StringTokenizer st = new StringTokenizer(className, "$");
            td = cu.getPackageMemberTypeDeclaration(st.nextToken());
            if (td == null) return null;
            while (st.hasMoreTokens()) {
                td = td.getMemberTypeDeclaration(st.nextToken());
                if (td == null) return null;
            }
        }

        return this.resolve(td);
    }

    /**
     * Equivalent with {@link #compileError(String, Location)} with a {@code null} location argument.
     */
    private void
    compileError(String message) throws CompileException { this.compileError(message, null); }

    /**
     * Issues a compile error with the given message. This is done through the {@link ErrorHandler} that was installed
     * through {@link #setCompileErrorHandler(ErrorHandler)}. Such a handler typically throws a {@link
     * CompileException}, but it may as well decide to return normally. Consequently, the calling code must be prepared
     * that {@link #compileError(String, Location)} returns normally, and must attempt to continue compiling.
     *
     * @param message          The message to report
     * @param location The location to report
     */
    private void
    compileError(String message, @Nullable Location location) throws CompileException {
        if (this.speculativeInvocationDepth != 0) throw new CompileException(message, location);
        ++this.compileErrorCount;
        if (this.compileErrorHandler != null) {
            this.compileErrorHandler.handleError(message, location);
        } else {
            throw new CompileException(message, location);
        }
    }

    /**
     * Issues a warning with the given message an location an returns. This is done through a {@link WarningHandler}
     * that was installed through {@link #setWarningHandler(WarningHandler)}.
     * <p>
     *   The <var>handle</var> argument qualifies the warning and is typically used by the {@link WarningHandler} to
     *   suppress individual warnings.
     * </p>
     */
    private void
    warning(String handle, String message, @Nullable Location location) throws CompileException {
        if (this.warningHandler != null) {
            this.warningHandler.handleWarning(handle, message, location);
        }
    }

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application my install its own
     * (thread-local) {@link ErrorHandler}.
     * <p>
     *   Be aware that a single problem during compilation often causes a bunch of compile errors, so a good {@link
     *   ErrorHandler} counts errors and throws a {@link CompileException} when a limit is reached.
     * </p>
     * <p>
     *   If the given {@link ErrorHandler} does not throw {@link CompileException}s, then {@link
     *   #compileUnit(boolean, boolean, boolean, ClassFileConsumer)} will throw one when the compilation of the unit is
     *   finished, and errors had occurred. In other words: The {@link ErrorHandler} may throw a {@link
     *   CompileException} or not, but {@link #compileUnit(boolean, boolean, boolean, ClassFileConsumer)} will
     *   definitely throw a {@link CompileException} if one or more compile errors have occurred.
     * </p>
     *
     * @param compileErrorHandler {@code null} to restore the default behavior (throwing a {@link
     *                                    CompileException})
     */
    public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    @Nullable private CodeContext
    replaceCodeContext(@Nullable CodeContext newCodeContext) {
        CodeContext oldCodeContext = this.codeContext;
        this.codeContext = newCodeContext;
        return oldCodeContext;
    }

    private void
    invokeConstructor(Locatable locatable, IConstructor iConstructor) throws CompileException {
        this.bytecodeEmitter.invokeConstructor(locatable, iConstructor);
    }

    private TypeBodyDeclaration
    getDeclaringTypeBodyDeclaration(QualifiedThisReference qtr) throws CompileException {

        if (qtr.declaringTypeBodyDeclaration != null) return qtr.declaringTypeBodyDeclaration;

        // Compile error if in static function context.
        Scope s;
        for (
            s = qtr.getEnclosingScope();
            !(s instanceof TypeBodyDeclaration);
            s = s.getEnclosingScope()
        );
        TypeBodyDeclaration result = (TypeBodyDeclaration) s;

        if (UnitCompiler.isStaticContext(result)) {
            this.compileError("No current instance available in static method", qtr.getLocation());
        }

        // Determine declaring type.
        qtr.declaringClass = (AbstractClassDeclaration) result.getDeclaringType();

        return (qtr.declaringTypeBodyDeclaration = result);
    }

    private AbstractClassDeclaration
    getDeclaringClass(QualifiedThisReference qtr) throws CompileException {

        if (qtr.declaringClass != null) return qtr.declaringClass;

        this.getDeclaringTypeBodyDeclaration(qtr);
        assert qtr.declaringClass != null;
        return qtr.declaringClass;
    }

    private void
    referenceThis(Locatable locatable, IClass currentIClass) {
        this.bytecodeEmitter.load(
            locatable,
            currentIClass, // localVariableType
            0              // localVariableIndex
        );
    }

    /**
     * Expects <var>dimExprCount</var> values of type {@code int} on the operand stack. Creates an array of
     * <var>dimExprCount</var> {@code +} <var>dims</var> dimensions of <var>componentType</var>.
     *
     * @return The type of the created array
     */
    private IClass
    newArray(Locatable locatable, int dimExprCount, int dims, IType componentType) {

        IClass rawComponentType = UnitCompiler.rawTypeOf(componentType);

        if (dimExprCount == 1 && dims == 0 && rawComponentType.isPrimitive()) {

            // "new <primitive>[<size>]"
            this.bytecodeEmitter.newarray(locatable, componentType);
        } else
        if (dimExprCount == 1) {

            // "new <class-or-interface> [<size>]"
            // "new <anything> [<size>] []{dims}"
            this.bytecodeEmitter.anewarray(locatable, this.iClassLoader.getArrayIClass(rawComponentType, dims));
        } else
        {

            // "new <anything> []{dims}"
            // "new <anything> [<size>]{dimexprCount}"
            // "new <anything> [<size>]{dimexprCount} []{dims}"
            this.bytecodeEmitter.multianewarray(locatable, dimExprCount, dims, componentType);
        }

        return this.iClassLoader.getArrayIClass(rawComponentType, dimExprCount + dims);
    }

    /**
     * Short-hand implementation of {@link IClass.IField} that implements a non-constant, non-static,
     * package-accessible field.
     */
    public static
    class SimpleIField extends IClass.IField {

        private final String name;
        private final IClass type;
        private final IType compileTimeType;

        public
        SimpleIField(IClass declaringIClass, String name, IClass type) {
            this(declaringIClass, name, (IType) type);
        }

        public
        SimpleIField(IClass declaringIClass, String name, IType type) {
            declaringIClass.super();
            this.name = name;
            this.type = UnitCompiler.rawTypeOf(type);
            this.compileTimeType = type;
        }

        @Override public Object        getConstantValue() { return UnitCompiler.NOT_CONSTANT; }
        @Override public String        getName()          { return this.name;                 }
        @Override public IClass        getType()          { return this.type;                 }
        @Override public boolean       isStatic()         { return false;                     }
        @Override public Access        getAccess()        { return Access.DEFAULT;            }
        @Override public IAnnotation[] getAnnotations()   { return new IAnnotation[0];        }
    }

    private static String
    last(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        return sa[sa.length - 1];
    }

    private static String[]
    allButLast(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        String[] tmp = new String[sa.length - 1];
        System.arraycopy(sa, 0, tmp, 0, tmp.length);
        return tmp;
    }

    private static String[]
    concat(String[] sa, String s) {
        String[] tmp = new String[sa.length + 1];
        System.arraycopy(sa, 0, tmp, 0, sa.length);
        tmp[sa.length] = s;
        return tmp;
    }

    private static CompileException
    compileException(Locatable locatable, String message) {
        return new CompileException(message, locatable.getLocation());
    }

    private short
    accessFlags(Modifier[] modifiers) throws CompileException {
        int result = 0;
        for (Modifier m : modifiers) {
            if (m instanceof AccessModifier) {
                String kw = ((AccessModifier) m).keyword;
                if ("public".equals(kw)) {
                    result |= Mod.PUBLIC;
                } else
                if ("private".equals(kw)) {
                    result |= Mod.PRIVATE;
                } else
                if ("protected".equals(kw)) {
                    result |= Mod.PROTECTED;
                } else
                if ("static".equals(kw)) {
                    result |= Mod.STATIC;
                } else
                if ("final".equals(kw)) {
                    result |= Mod.FINAL;
                } else
                if ("synchronized".equals(kw)) {
                    result |= Mod.SYNCHRONIZED;
                } else
                if ("volatile".equals(kw)) {
                    result |= Mod.VOLATILE;
                } else
                if ("transient".equals(kw)) {
                    result |= Mod.TRANSIENT;
                } else
                if ("native".equals(kw)) {
                    result |= Mod.NATIVE;
                } else
                if ("abstract".equals(kw)) {
                    result |= Mod.ABSTRACT;
                } else
                if ("strictfp".equals(kw)) {
                    result |= Mod.STRICTFP;
                } else
                if ("default".equals(kw)) {
                    ;
                } else {
                    this.compileError("Invalid modifier \"" + kw + "\"");
                }
            }
        }
        return (short) result;
    }

    private static Modifier[]
    accessModifiers(Location location, String... keywords) {
        Modifier[] result = new Modifier[keywords.length];
        for (int i = 0; i < keywords.length; i++) {
            result[i] = new AccessModifier(keywords[i], location);
        }
        return result;
    }

    private static short
    changeAccessibility(short accessFlags, short newAccessibility) {
        return (short) ((accessFlags & ~(Mod.PUBLIC | Mod.PROTECTED | Mod.PRIVATE)) | newAccessibility);
    }

    // Used to write byte code while compiling one constructor/method.
    @Nullable private CodeContext codeContext;

    // Used for elaborate compile error handling.
    @Nullable private ErrorHandler compileErrorHandler;
    private int                    compileErrorCount;

    // Used for elaborate warning handling.
    @Nullable private WarningHandler warningHandler;

    private final AbstractCompilationUnit abstractCompilationUnit;

    private final IClassLoader iClassLoader;

    /**
     * Non-{@code null} while {@link #compileUnit(boolean, boolean, boolean, ClassFileConsumer)} is executing.
     */
    @Nullable private ClassFileConsumer storesClassFiles;

    private boolean debugSource;
    private boolean debugLines;
    private boolean debugVars;
}
