
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java;
import org.codehaus.janino.CodeContext.BasicBlock;
import org.codehaus.janino.CodeContext.Inserter;
import org.codehaus.janino.CodeContext.Offset;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.AssertStatement;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.BreakableStatement;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CatchParameter;
import org.codehaus.janino.Java.ContinuableStatement;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Scope;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SwitchStatement.SwitchBlockStatementGroup;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.TryStatement.LocalVariableDeclaratorResource;
import org.codehaus.janino.Java.TryStatement.VariableAccessResource;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.TryStatementResourceVisitor;
import org.codehaus.janino.util.DeepCopier;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.ParenthesizedExpression;

/** Compiles block statements and their control flow using the active method context. */
final class UnitCompilerStatementCompiler {

    /** JLS 15.27.2 block shape, checked before an overloaded functional target is selected. */
    boolean lambdaBlockCompatible(Block block, final boolean voidResult) throws CompileException {
        final boolean[] compatible = { true };
        new org.codehaus.janino.util.AbstractTraverser<RuntimeException>() {
            @Override public void traverseReturnStatement(ReturnStatement statement) {
                if ((statement.returnValue == null) != voidResult) compatible[0] = false;
            }
            @Override public void traverseLambdaExpression(LambdaExpression expression) {}
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
            @Override public void traverseLocalClassDeclaration(LocalClassDeclaration declaration) {}
        }.traverseBlock(block);
        return compatible[0] && (voidResult || !this.canCompleteNormallyForLambda(block));
    }

    boolean canCompleteNormallyForLambda(BlockStatement statement) throws CompileException {
        if (statement instanceof ReturnStatement || statement instanceof ThrowStatement
            || statement instanceof BreakStatement || statement instanceof ContinueStatement) return false;
        if (statement instanceof Block) {
            for (BlockStatement child : ((Block) statement).statements) if (!this.canCompleteNormallyForLambda(child)) return false;
        } else if (statement instanceof IfStatement) {
            IfStatement conditional = (IfStatement) statement;
            return conditional.elseStatement == null || this.canCompleteNormallyForLambda(conditional.thenStatement)
                || this.canCompleteNormallyForLambda(conditional.elseStatement);
        } else if (statement instanceof WhileStatement) {
            return !Boolean.TRUE.equals(this.constantEvaluator.getConstantValue(((WhileStatement) statement).condition))
                || this.hasBreakTarget((BreakableStatement) statement);
        } else if (statement instanceof ForStatement) {
            Rvalue condition = ((ForStatement) statement).condition;
            return condition != null && !Boolean.TRUE.equals(this.constantEvaluator.getConstantValue(condition))
                || this.hasBreakTarget((BreakableStatement) statement);
        } else if (statement instanceof DoStatement) {
            DoStatement loop = (DoStatement) statement;
            return this.hasBreakTarget(loop) || (this.canCompleteNormallyForLambda(loop.body) || this.hasContinueTarget(loop))
                && !Boolean.TRUE.equals(this.constantEvaluator.getConstantValue(loop.condition));
        } else if (statement instanceof SynchronizedStatement) {
            return this.canCompleteNormallyForLambda(((SynchronizedStatement) statement).body);
        } else if (statement instanceof LabeledStatement) {
            LabeledStatement labeled = (LabeledStatement) statement;
            return this.canCompleteNormallyForLambda(labeled.body) || this.hasBreakTarget(labeled);
        } else if (statement instanceof TryStatement) {
            TryStatement attempt = (TryStatement) statement;
            if (attempt.finallY != null && !this.canCompleteNormallyForLambda(attempt.finallY)) return false;
            if (this.canCompleteNormallyForLambda(attempt.body)) return true;
            for (CatchClause caught : attempt.catchClauses) if (this.canCompleteNormallyForLambda(caught.body)) return true;
            return false;
        } else if (statement instanceof SwitchStatement) {
            SwitchStatement selection = (SwitchStatement) statement;
            boolean hasDefault = false;
            for (SwitchBlockStatementGroup group : selection.sbsgs) if (group.hasDefaultLabel) hasDefault = true;
            if (!hasDefault || this.hasBreakTarget(selection) || selection.sbsgs.isEmpty()) return true;
            List<BlockStatement> last = selection.sbsgs.get(selection.sbsgs.size() - 1).blockStatements;
            for (BlockStatement child : last) if (!this.canCompleteNormallyForLambda(child)) return false;
        }
        return true;
    }

    private boolean hasContinueTarget(final ContinuableStatement target) throws CompileException {
        final boolean[] found = { false };
        new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
            @Override public void traverseContinueStatement(ContinueStatement statement) throws CompileException {
                Scope child = statement;
                for (Scope scope = statement.getEnclosingScope(); scope instanceof Statement || scope instanceof CatchClause;
                    child = scope, scope = scope.getEnclosingScope()) {
                    if (scope instanceof TryStatement && ((TryStatement) scope).finallY != child
                        && ((TryStatement) scope).finallY != null
                        && !UnitCompilerStatementCompiler.this.canCompleteNormallyForLambda(((TryStatement) scope).finallY)) return;
                    if (statement.label == null && scope instanceof ContinuableStatement) {
                        if (scope == target) found[0] = true;
                        return;
                    }
                    if (statement.label != null && scope instanceof LabeledStatement
                        && statement.label.equals(((LabeledStatement) scope).label)) {
                        BlockStatement body = ((LabeledStatement) scope).body;
                        while (body instanceof LabeledStatement) body = ((LabeledStatement) body).body;
                        if (body == target) found[0] = true;
                        return;
                    }
                }
            }
            @Override public void traverseLambdaExpression(LambdaExpression expression) {}
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
            @Override public void traverseLocalClassDeclaration(LocalClassDeclaration declaration) {}
        }.visitBlockStatement(target);
        return found[0];
    }

    private boolean hasBreakTarget(final BreakableStatement target) throws CompileException {
        final boolean[] found = { false };
        new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
            @Override public void traverseBreakStatement(BreakStatement statement) throws CompileException {
                Scope child = statement;
                for (Scope scope = statement.getEnclosingScope(); scope instanceof Statement || scope instanceof CatchClause;
                    child = scope, scope = scope.getEnclosingScope()) {
                    if (scope instanceof TryStatement && ((TryStatement) scope).finallY != child
                        && ((TryStatement) scope).finallY != null
                        && !UnitCompilerStatementCompiler.this.canCompleteNormallyForLambda(((TryStatement) scope).finallY)) return;
                    if (scope instanceof BreakableStatement && (statement.label == null ? !(scope instanceof LabeledStatement)
                        : scope instanceof LabeledStatement && statement.label.equals(((LabeledStatement) scope).label))) {
                        if (scope == target) found[0] = true;
                        return;
                    }
                }
            }
            @Override public void traverseLambdaExpression(LambdaExpression expression) {}
            @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration declaration) {}
            @Override public void traverseLocalClassDeclaration(LocalClassDeclaration declaration) {}
        }.visitBlockStatement(target);
        return found[0];
    }

    /** Semantic operations needed by statement compilation; method state stays with the compilation unit. */
    interface Context {
        void
        compile(TypeDeclaration td) throws CompileException;
        CodeContext
        getCodeContext();
        LocalVariable
        allocateLocalVariable(boolean finaL, IType localVariableType);
        @Nullable LocalClassDeclaration
        findLocalClassDeclaration(Scope s, String name);
        void
        compile(ArrayInitializerOrRvalue aiorv, IType arrayType) throws CompileException;
        LocalVariable
        getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException;
        LocalVariableSlot
        allocateLocalVariableSlot(IType localVariableType, @Nullable String localVariableName);
        LocalVariable
        getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException;
        LocalVariable
        getLocalVariable(CatchParameter parameter) throws CompileException;
        IClass
        lub(List<IClass> types) throws CompileException;
        void
        fakeCompile(Rvalue rv) throws CompileException;
        void
        compile(Rvalue rv) throws CompileException;
        boolean
        compile2(AlternateConstructorInvocation aci) throws CompileException;
        boolean
        compile2(SuperConstructorInvocation sci) throws CompileException;
        void
        compileBoolean(Rvalue rv, CodeContext.Offset dst, boolean orientation) throws CompileException;
        void
        setLambdaTargetType(Rvalue rv, @Nullable IType targetType);
        @Nullable IType
        getComponentType(IType expressionType);
        boolean
        isArray(IType type);
        boolean
        isAssignableFrom(IType targetType, IType sourceType) throws CompileException;
        IType
        compileGetValue(Rvalue rv) throws CompileException;
        IClass
        getRawType(Type t) throws CompileException;
        IType
        getType(Type t) throws CompileException;
        IType
        getType(Rvalue rv) throws CompileException;
        void
        invokeConstructor(
            Locatable        locatable,
            Scope            scope,
            @Nullable Rvalue enclosingInstance,
            IType            targetType,
            Rvalue[]         arguments
        ) throws CompileException;
        @Nullable ArrayInitializerOrRvalue
        getNonConstantFinalInitializer(FieldDeclaration fd, VariableDeclarator vd) throws CompileException;
        void
        checkThrownException(Locatable locatable, IType type, Scope scope) throws CompileException;

        @Nullable List<IType> preciseRethrowTypes(ThrowStatement statement);
        IClass
        resolve(TypeDeclaration td);
        IType
        getReturnType(FunctionDeclarator fd) throws CompileException;
        IClass
        consT(Locatable locatable, @Nullable Object value) throws CompileException;
        void
        consT(Locatable locatable, int value);
        void
        consT(Locatable locatable, String s);
        void
        assignmentConversion(
            Locatable        locatable,
            IType            sourceType,
            IType            targetType,
            @Nullable Object constantValue
        ) throws CompileException;
        boolean
        tryAssignmentConversion(
            Locatable        locatable,
            IType            sourceType,
            IType            targetType,
            @Nullable Object constantValue
        ) throws CompileException;
        @Nullable Object
        constantAssignmentConversion(Locatable locatable, @Nullable Object value, IType targetType) throws CompileException;
        boolean
        tryNarrowingReferenceConversion(Locatable locatable, IType sourceType, IType targetType) throws CompileException;
        @Nullable IClass
        isBoxingConvertible(IType sourceType);
        void
        unboxingConversion(Locatable locatable, IType sourceType, IClass targetType);
        void
        compileError(String message) throws CompileException;
        void compileCatchTypeAnnotations(Type type, Java.Modifier[] modifiers, int index) throws CompileException;
        void compileLocalTypeAnnotations(Type type, Java.Modifier[] modifiers, byte target, LocalVariableSlot slot) throws CompileException;
        void
        compileError(String message, @Nullable Location location) throws CompileException;
        void
        warning(String handle, String message, @Nullable Location location) throws CompileException;
        EnumSet<JaninoOption> options();
        void
        buildLocalVariableMap(CatchClause catchClause, Map<String, LocalVariable> localVars) throws CompileException;
        LocalVariable
        getLocalVariable(FormalParameter parameter) throws CompileException;
        Atom
        reclassify(AmbiguousName an) throws CompileException;
        CompileException
        compileException(Locatable locatable, String message);
    }

    private static final class CleanupExclusion {
        final CodeContext.Offset start;
        @Nullable CodeContext.Offset end;
        CleanupExclusion(CodeContext.Offset start) { this.start = start; }
    }

    private static final class ProtectedRegion {
        final BlockStatement owner;
        final CodeContext codeContext;
        final boolean resource;
        final List<CleanupExclusion> exclusions = new ArrayList<>();
        ProtectedRegion(BlockStatement owner, CodeContext codeContext, boolean resource) {
            this.owner = owner;
            this.codeContext = codeContext;
            this.resource = resource;
        }
    }

    private static final class ResourceCleanup {
        final ProtectedRegion region;
        final BlockStatement body;
        ResourceCleanup(TryStatement owner, BlockStatement body, CodeContext codeContext) {
            this.region = new ProtectedRegion(owner, codeContext, true);
            this.body = body;
        }
    }

    private final Set<LocalVariable> initializingResources = new HashSet<>();

    boolean isInitializingResource(LocalVariable variable) { return this.initializingResources.contains(variable); }

    private final List<ProtectedRegion> protectedRegions = new ArrayList<>();
    private final List<ResourceCleanup> resourceCleanups = new ArrayList<>();
    @Nullable private List<CleanupExclusion> abruptCleanupExclusions;

    private void excludeCleanup(ProtectedRegion region) {
        CleanupExclusion exclusion = new CleanupExclusion(this.context.getCodeContext().newOffset());
        region.exclusions.add(exclusion);
        assert this.abruptCleanupExclusions != null;
        this.abruptCleanupExclusions.add(exclusion);
    }

    /** Inline cleanup must not be caught by the region being exited, nor close that resource again. */
    private void addExceptionTableEntries(
        ProtectedRegion region, CodeContext.Offset start, CodeContext.Offset end,
        CodeContext.Offset handler, @Nullable String catchType
    ) {
        List<CleanupExclusion> exclusions = new ArrayList<>(region.exclusions);
        Collections.sort(exclusions, new Comparator<CleanupExclusion>() {
            @Override public int compare(CleanupExclusion a, CleanupExclusion b) {
                return Integer.compare(a.start.offset, b.start.offset);
            }
        });
        CodeContext.Offset cursor = start;
        for (CleanupExclusion exclusion : exclusions) {
            CodeContext.Offset excludedEnd = exclusion.end;
            assert excludedEnd != null;
            if (excludedEnd.offset <= cursor.offset || exclusion.start.offset >= end.offset) continue;
            if (cursor.offset < exclusion.start.offset) {
                region.codeContext.addExceptionTableEntry(cursor, exclusion.start, handler, catchType);
            }
            cursor = excludedEnd.offset < end.offset ? excludedEnd : end;
        }
        if (cursor.offset < end.offset) region.codeContext.addExceptionTableEntry(cursor, end, handler, catchType);
    }

    private final Context context;
    private final IClassLoader iClassLoader;
    private final UnitCompilerBytecodeEmitter bytecodeEmitter;
    private final UnitCompilerConstantEvaluator constantEvaluator;

    UnitCompilerStatementCompiler(
        Context context,
        IClassLoader iClassLoader,
        UnitCompilerBytecodeEmitter bytecodeEmitter,
        UnitCompilerConstantEvaluator constantEvaluator
    ) {
        this.context = context;
        this.iClassLoader = iClassLoader;
        this.bytecodeEmitter = bytecodeEmitter;
        this.constantEvaluator = constantEvaluator;
    }


    /**
     * @return Whether this statement can complete normally (JLS7 14.1)
     */
    boolean
    compile(BlockStatement bs) throws CompileException {

        try {
            Boolean result = (Boolean) bs.accept(new BlockStatementVisitor<Boolean, CompileException>() {
                @Override public Boolean visitInitializer(Initializer i)                                                throws CompileException { return UnitCompilerStatementCompiler.this.compile2(i);    }
                @Override public Boolean visitFieldDeclaration(FieldDeclaration fd)                                     throws CompileException { return UnitCompilerStatementCompiler.this.compile2(fd);   }
                @Override public Boolean visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ls);   }
                @Override public Boolean visitBlock(Block b)                                                            throws CompileException { return UnitCompilerStatementCompiler.this.compile2(b);    }
                @Override public Boolean visitExpressionStatement(ExpressionStatement es)                               throws CompileException { return UnitCompilerStatementCompiler.this.compile2(es);   }
                @Override public Boolean visitIfStatement(IfStatement is)                                               throws CompileException { return UnitCompilerStatementCompiler.this.compile2(is);   }
                @Override public Boolean visitForStatement(ForStatement fs)                                             throws CompileException { return UnitCompilerStatementCompiler.this.compile2(fs);   }
                @Override public Boolean visitForEachStatement(ForEachStatement fes)                                    throws CompileException { return UnitCompilerStatementCompiler.this.compile2(fes);  }
                @Override public Boolean visitWhileStatement(WhileStatement ws)                                         throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ws);   }
                @Override public Boolean visitTryStatement(TryStatement ts)                                             throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ts);   }
                @Override public Boolean visitSwitchStatement(SwitchStatement ss)                                       throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ss);   }
                @Override public Boolean visitSynchronizedStatement(SynchronizedStatement ss)                           throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ss);   }
                @Override public Boolean visitDoStatement(DoStatement ds)                                               throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ds);   }
                @Override public Boolean visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return UnitCompilerStatementCompiler.this.compile2(lvds); }
                @Override public Boolean visitReturnStatement(ReturnStatement rs)                                       throws CompileException { return UnitCompilerStatementCompiler.this.compile2(rs);   }
                @Override public Boolean visitThrowStatement(ThrowStatement ts)                                         throws CompileException { return UnitCompilerStatementCompiler.this.compile2(ts);   }
                @Override public Boolean visitBreakStatement(BreakStatement bs)                                         throws CompileException { return UnitCompilerStatementCompiler.this.compile2(bs);   }
                @Override public Boolean visitContinueStatement(ContinueStatement cs)                                   throws CompileException { return UnitCompilerStatementCompiler.this.compile2(cs);   }
                @Override public Boolean visitAssertStatement(AssertStatement as)                                       throws CompileException { return UnitCompilerStatementCompiler.this.compile2(as);   }
                @Override public Boolean visitEmptyStatement(EmptyStatement es)                                                                 { return UnitCompilerStatementCompiler.this.compile2(es);   }
                @Override public Boolean visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws CompileException { return UnitCompilerStatementCompiler.this.compile2(lcds); }
                @Override public Boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws CompileException { return UnitCompilerStatementCompiler.this.context.compile2(aci);  }
                @Override public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws CompileException { return UnitCompilerStatementCompiler.this.context.compile2(sci);  }
            });

            assert result != null;
            return result;
        } catch (RuntimeException re) {
            throw new RuntimeException(bs.getLocation().toString(), re);
        }
    }

    /**
     * Called to check whether the given {@link BlockStatement} compiles or not.
     * Updates the stack map of the current inserter.
     *
     * @return Whether the block statement can complete normally
     */
    boolean
    fakeCompile(BlockStatement bs) throws CompileException {

        Offset from = this.context.getCodeContext().newOffset();

        boolean ccn = this.compile(bs);

        Offset to = this.context.getCodeContext().newOffset();

        this.context.getCodeContext().removeCode(from, to);

        return ccn;
    }

    private boolean
    compile2(Initializer i) throws CompileException {

        this.buildLocalVariableMap(i.block, new HashMap<String, LocalVariable>());
        this.analyzeFinalLocals(i.block);

        return this.compile(i.block);
    }

    private boolean
    compile2(Block b) throws CompileException {
        this.context.getCodeContext().saveLocalVariables();
        try {
            return this.compileStatements(b.statements);
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    boolean
    compileStatements(List<? extends BlockStatement> statements) throws CompileException {
        boolean previousStatementCanCompleteNormally = true;
        for (BlockStatement bs : statements) {
            if (!previousStatementCanCompleteNormally && this.generatesCode(bs)) {
                this.context.compileError("Statement is unreachable", bs.getLocation());
                break;
            }
            try {
                previousStatementCanCompleteNormally = this.compile(bs);
            } catch (RuntimeException re) {
                throw new RuntimeException(bs.getLocation().toString(), re);
            } catch (AssertionError ae) {
                throw new InternalCompilerException(bs.getLocation(), null, ae);
            }
        }
        return previousStatementCanCompleteNormally;
    }

    private boolean
    compile2(DoStatement ds) throws CompileException {
        Object cvc = this.constantEvaluator.getConstantValue(ds.condition);
        if (cvc != UnitCompiler.NOT_CONSTANT) {
            if (Boolean.TRUE.equals(cvc)) {
                this.context.warning("DSTC", (
                    "Condition of DO statement is always TRUE; "
                    + "the proper way of declaring an unconditional loop is \"for (;;)\""
                ), ds.getLocation());
                return this.compileUnconditionalLoop(ds, ds.body, null);
            } else
            {
                this.context.warning("DSNR", "DO statement never repeats", ds.getLocation());
            }
        }

        final CodeContext.Offset bodyOffset = this.context.getCodeContext().newBasicBlock();

        // Compile body.
        assert ds.whereToBreak    == null;
        assert ds.whereToContinue == null;

        this.context.getCodeContext().saveLocalVariables();
        try {

            if (!this.compile(ds.body) && ds.whereToContinue == null) {
                this.context.warning("DSNTC", "\"do\" statement never tests its condition", ds.getLocation());

                Offset wtb = ds.whereToBreak;
                if (wtb == null) return false;

                wtb.set();

                return true;
            }

            Offset wtc = ds.whereToContinue;
            if (wtc != null) wtc.set();

            // Compile condition.
            if (Boolean.FALSE.equals(cvc)) this.context.fakeCompile(ds.condition);
            else this.context.compileBoolean(ds.condition, bodyOffset, UnitCompiler.JUMP_IF_TRUE);

            Offset wtb = ds.whereToBreak;
            if (wtb != null) wtb.set();

            return true;
        } finally {
            ds.whereToBreak   = null;
            ds.whereToContinue = null;
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    private boolean
    compile2(ForStatement fs) throws CompileException {

        this.context.getCodeContext().saveLocalVariables();
        try {
            BlockStatement oi = fs.init;
            Rvalue[]       ou = fs.update;
            Rvalue         oc = fs.condition;

            // Compile initializer.
            if (oi != null) this.compile(oi);

            if (oc == null) {
                return this.compileUnconditionalLoop(fs, fs.body, ou);
            }

            Object cvc = this.constantEvaluator.getConstantValue(oc);
            if (cvc != UnitCompiler.NOT_CONSTANT) {
                if (Boolean.TRUE.equals(cvc)) {
                    this.context.warning("FSTC", (
                        "Condition of FOR statement is always TRUE; "
                        + "the proper way of declaring an unconditional loop is \"for (;;)\""
                    ), fs.getLocation());
                    return this.compileUnconditionalLoop(fs, fs.body, ou);
                } else
                {
                    this.context.warning("FSNR", "FOR statement never repeats", fs.getLocation());
                }
            }

            // Compile the first condition before its body: assignments in the condition initialize body locals.
            CodeContext.Offset toCondition = this.context.getCodeContext().newBasicBlock();
            StackMap beforeCondition = this.context.getCodeContext().currentInserter().getStackMap();
            CodeContext.Offset end = this.context.getCodeContext().new BasicBlock();
            fs.whereToBreak = end;
            this.context.compileBoolean(oc, end, UnitCompiler.JUMP_IF_FALSE);
            if (this.context.getCodeContext().currentInserter().getStackMap() == null) {
                this.context.getCodeContext().currentInserter().setStackMap(beforeCondition);
            }

            // Compile body.
            fs.whereToContinue = null;
            this.context.getCodeContext().newBasicBlock();
            boolean                  bodyCcn    = this.compile(fs.body);
            boolean repeats = bodyCcn || fs.whereToContinue != null;
            if (fs.whereToContinue != null) fs.whereToContinue.set();

            // Compile update.
            if (ou != null) {
                if (!bodyCcn && fs.whereToContinue == null) {
                    this.context.warning("FUUR", "For update is unreachable", fs.getLocation());
                } else
                {
                    for (Rvalue rv : ou) this.context.compile(rv);
                }
            }
            fs.whereToContinue = null;

            if (repeats) this.bytecodeEmitter.gotO(fs, toCondition);
            end.set();
            fs.whereToBreak = null;
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }

        if (fs.whereToBreak != null) {
            fs.whereToBreak.set();
            fs.whereToBreak = null;
        }

        return true;
    }

    private boolean
    compile2(ForEachStatement fes) throws CompileException {
        IType expressionType = this.context.getType(fes.expression);

        if (UnitCompilerStatementCompiler.this.context.isArray(expressionType)) {
            this.context.getCodeContext().saveLocalVariables();
            try {

                StackMap beforeStatement = this.context.getCodeContext().currentInserter().getStackMap();

                // Allocate the local variable for the current element.
                LocalVariable elementLv = this.context.getLocalVariable(fes.currentElement, false);
                elementLv.setSlot(this.context.allocateLocalVariableSlot(elementLv.type, fes.currentElement.name));

                // Compile initializer.
                this.context.compileGetValue(fes.expression);
                short expressionLv = this.context.getCodeContext().allocateLocalVariable((short) 1);
                this.bytecodeEmitter.store(fes.expression, expressionType, expressionLv);

                this.context.consT(fes, 0);
                LocalVariable indexLv = this.context.allocateLocalVariable(false /*finaL*/, IClass.INT);
                this.bytecodeEmitter.store(fes, indexLv);

                StackMap beforeBody = this.context.getCodeContext().currentInserter().getStackMap();

                CodeContext.Offset toCondition = this.context.getCodeContext().new BasicBlock();
                this.bytecodeEmitter.gotO(fes, toCondition);

                // Get the next array element.
                this.context.getCodeContext().currentInserter().setStackMap(beforeBody);
                fes.whereToContinue = null;
                final CodeContext.Offset bodyOffset = this.context.getCodeContext().newBasicBlock();

                this.bytecodeEmitter.load(fes, expressionType, expressionLv);
                this.bytecodeEmitter.load(fes, indexLv);
                IType componentType = UnitCompilerStatementCompiler.this.context.getComponentType(expressionType);
                assert componentType != null;
                this.bytecodeEmitter.xaload(fes.currentElement, componentType);
                this.context.assignmentConversion(fes.currentElement, componentType, elementLv.type, null);
                this.bytecodeEmitter.store(fes, elementLv);

                // Compile the body.
                boolean bodyCcn = this.compile(fes.body);
                if (fes.whereToContinue != null) fes.whereToContinue.set();

                // Compile update.
                if (!bodyCcn && fes.whereToContinue == null) {
                    this.context.warning("FUUR", "For update is unreachable", fes.getLocation());
                } else {
                    this.bytecodeEmitter.iinc(fes, indexLv, "++");
                }
                fes.whereToContinue = null;

                // Compile condition.
                toCondition.set();
                this.bytecodeEmitter.load(fes, indexLv);
                this.bytecodeEmitter.load(fes, expressionType, expressionLv);
                this.bytecodeEmitter.arraylength(fes);
                this.bytecodeEmitter.if_icmpxx(fes, UnitCompilerBytecodeEmitter.LT, bodyOffset);

                this.context.getCodeContext().currentInserter().setStackMap(beforeStatement);
            } finally {
                this.context.getCodeContext().restoreLocalVariables();
            }

            if (fes.whereToBreak != null) {
                fes.whereToBreak.set();
                fes.whereToBreak = null;
            }
        } else
        if (UnitCompilerStatementCompiler.this.context.isAssignableFrom(this.iClassLoader.TYPE_java_lang_Iterable, expressionType)) {
            this.context.getCodeContext().saveLocalVariables();
            try {

                StackMap beforeStatement = this.context.getCodeContext().currentInserter().getStackMap();

                // Compile initializer.
                this.context.compileGetValue(fes.expression);
                this.bytecodeEmitter.invokeMethod(fes.expression, this.iClassLoader.METH_java_lang_Iterable__iterator);
                LocalVariable iteratorLv = this.context.allocateLocalVariable(false /*finaL*/, this.iClassLoader.TYPE_java_util_Iterator);
                this.bytecodeEmitter.store(fes, iteratorLv);

                CodeContext.Offset toCondition = this.context.getCodeContext().new BasicBlock();
                StackMap smBeforeBody = this.context.getCodeContext().currentInserter().getStackMap();
                this.bytecodeEmitter.gotO(fes, toCondition);

                // Compile the body.
                this.context.getCodeContext().currentInserter().setStackMap(smBeforeBody);

                // Allocate the local variable for the current element.
                LocalVariable elementLv = this.context.getLocalVariable(fes.currentElement, false);
                elementLv.setSlot(this.context.allocateLocalVariableSlot(elementLv.type, fes.currentElement.name));

                fes.whereToContinue = null;
                final CodeContext.Offset bodyOffset = this.context.getCodeContext().newBasicBlock();

                this.bytecodeEmitter.load(fes, iteratorLv);
                this.bytecodeEmitter.invokeMethod(fes.expression, this.iClassLoader.METH_java_util_Iterator__next);

                IClass boxedType = this.context.isBoxingConvertible(elementLv.type);
                if (boxedType != null) {

                    // Fix for Issue #155.
                    this.bytecodeEmitter.checkcast(fes.currentElement, boxedType);
                    this.context.unboxingConversion(fes.currentElement, boxedType, (IClass) elementLv.type);
                } else
                if (this.context.tryAssignmentConversion(
                    fes.currentElement,
                    this.iClassLoader.TYPE_java_lang_Object,
                    elementLv.type,
                    null
                )) {
                    ;
                } else
                if (this.context.tryNarrowingReferenceConversion(
                    fes.currentElement,
                    this.iClassLoader.TYPE_java_lang_Object,
                    elementLv.type
                )) {
                    ;
                } else
                {
                    throw new InternalCompilerException(fes.getLocation(), "Don't know how to convert to " + elementLv.type);
                }

                this.bytecodeEmitter.store(fes, elementLv);

                boolean bodyCcn = this.compile(fes.body);
                if (fes.whereToContinue != null) fes.whereToContinue.set();

                // Compile update.
                if (!bodyCcn && fes.whereToContinue == null) {
                    this.context.warning("FUUR", "For update is unreachable", fes.getLocation());
                }
                fes.whereToContinue = null;

                // Compile condition.
                toCondition.set();
                this.bytecodeEmitter.load(fes, iteratorLv);
                this.bytecodeEmitter.invokeMethod(fes.expression, this.iClassLoader.METH_java_util_Iterator__hasNext);
                this.bytecodeEmitter.ifxx(fes, UnitCompilerBytecodeEmitter.NE, bodyOffset);

                this.context.getCodeContext().currentInserter().setStackMap(beforeStatement);
            } finally {
                this.context.getCodeContext().restoreLocalVariables();
            }

            if (fes.whereToBreak != null) {
                fes.whereToBreak.set();
                fes.whereToBreak = null;
            }
        } else
        {
            this.context.compileError("Cannot iterate over \"" + expressionType + "\"", fes.expression.getLocation());
        }
        return true;
    }

    private boolean
    compile2(WhileStatement ws) throws CompileException {

        // Check for special case: WHILE with constant condition.
        Object cvc = this.constantEvaluator.getConstantValue(ws.condition);
        if (cvc != UnitCompiler.NOT_CONSTANT) {
            if (Boolean.TRUE.equals(cvc)) {
                this.context.warning("WSTC", (
                    "Condition of WHILE statement is always TRUE; "
                    + "the proper way of declaring an unconditional loop is \"for (;;)\""
                ), ws.getLocation());
                return this.compileUnconditionalLoop(ws, ws.body, null);
            } else
            {
                this.context.warning("WSNR", "WHILE statement never repeats", ws.getLocation());
            }
        }

        assert ws.whereToBreak    == null;
        assert ws.whereToContinue == null;

        // GOTO condition.
        final CodeContext.Offset bodyOffset;
        final Inserter           bodyInserter;
        {
            Offset wtc = this.context.getCodeContext().new BasicBlock();
            this.bytecodeEmitter.gotO(ws, wtc);
            bodyOffset = this.context.getCodeContext().newBasicBlock();
            bodyInserter = this.context.getCodeContext().newInserter();
            wtc.set();
        }

        // Compile the condition first.
        // This is necessary because the condition may modify the stack map, e.g.
        //     String line;
        //     while ((line = bufferedReader.readLine()) != null) {
        //         System.out.println(line);
        //     }
        this.context.compileBoolean(ws.condition, bodyOffset, UnitCompiler.JUMP_IF_TRUE);

        // Now compile the body.
        this.context.getCodeContext().saveLocalVariables();
        try {

            try {
                // The body is reached on the TRUE edge. The false edge may skip assignments
                // on the RHS of &&/|| and therefore has a different local-variable map.
                StackMap smBeforeBody = bodyOffset.getStackMap();
                this.context.getCodeContext().pushInserter(bodyInserter);
                this.context.getCodeContext().currentInserter().setStackMap(smBeforeBody);

                if (!this.compile(ws.body) && ws.whereToContinue == null) {
                    this.context.warning("DSNTC", "\"where\" statement never repeats", ws.getLocation());
                }

                Offset wtc = ws.whereToContinue;
                if (wtc != null) wtc.set();
            } finally {
                this.context.getCodeContext().popInserter();
            }

            Offset wtb = ws.whereToBreak;
            if (wtb != null) wtb.set();

            return true;
        } finally {
            ws.whereToBreak    = null;
            ws.whereToContinue = null;
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    private boolean
    compileUnconditionalLoop(ContinuableStatement cs, BlockStatement body, @Nullable Rvalue[] update)
    throws CompileException {

        if (update != null) return this.compileUnconditionalLoopWithUpdate(cs, body, update);

        assert cs.whereToBreak    == null;
        assert cs.whereToContinue == null;

        this.context.getCodeContext().saveLocalVariables();
        try {

            // Compile body.
            Offset wtc = (cs.whereToContinue = this.context.getCodeContext().newBasicBlock());
            if (this.compile(body)) this.bytecodeEmitter.gotO(cs, wtc);

            Offset wtb = cs.whereToBreak;
            if (wtb == null) return false;

            wtb.set();

            return true;
        } finally {
            cs.whereToBreak    = null;
            cs.whereToContinue = null;
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    private boolean
    compileUnconditionalLoopWithUpdate(ContinuableStatement cs, BlockStatement body, Rvalue[] update)
    throws CompileException {

        // Compile body.
        cs.whereToContinue = null;
        final CodeContext.Offset bodyOffset = this.context.getCodeContext().newBasicBlock();
        boolean                  bodyCcn    = this.compile(body);

        // Compile the "update".
        if (cs.whereToContinue != null) cs.whereToContinue.set();
        if (!bodyCcn && cs.whereToContinue == null) {
            this.context.warning("LUUR", "Loop update is unreachable", update[0].getLocation());
        } else
        {
            for (Rvalue rv : update) this.context.compile(rv);
            this.bytecodeEmitter.gotO(cs, bodyOffset);
            this.context.getCodeContext().currentInserter().setStackMap(null);
        }
        cs.whereToContinue = null;

        Offset wtb = cs.whereToBreak;
        if (wtb == null) return false;
        wtb.set();

        cs.whereToBreak = null;

        return true;
    }

    private boolean
    compile2(LabeledStatement ls) throws CompileException {

        assert ls.whereToBreak == null;

        this.context.getCodeContext().saveLocalVariables();
        try {

            boolean canCompleteNormally = this.compile(ls.body);

            Offset wtb = ls.whereToBreak;
            if (wtb == null) return canCompleteNormally;

            wtb.set();

            return true;
        } finally {
            ls.whereToBreak = null;
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    private enum SwitchKind { INT, ENUM, STRING }

    private boolean
    compile2(SwitchStatement ss) throws CompileException {

        SwitchKind kind;
        short      ssvLvIndex = -1; // Only relevant if kind == STRING.

        // Determine SWITCH kind.
        IType switchExpressionType = this.context.getType(ss.condition);
        if (this.iClassLoader.TYPE_java_lang_String == switchExpressionType) {
            kind = SwitchKind.STRING;
        } else
        if (UnitCompilerStatementCompiler.this.context.isAssignableFrom(this.iClassLoader.TYPE_java_lang_Enum, switchExpressionType)) {
            kind = SwitchKind.ENUM;
        } else
        {
            kind = SwitchKind.INT;
        }

        // Prepare the map of case labels to code offsets.
        TreeMap<Integer, CodeContext.Offset> caseLabelMap       = new TreeMap<>();
        CodeContext.Offset                   defaultLabelOffset = null;
        CodeContext.Offset[]                 sbsgOffsets        = new CodeContext.Offset[ss.sbsgs.size()];
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i] = this.context.getCodeContext().new BasicBlock();
            for (Rvalue caseLabel : sbsg.caseLabels) {

                Integer civ;

                switch (kind) {

                case ENUM:
                    CIV: {
                        if (!(caseLabel instanceof AmbiguousName)) {
                            this.context.compileError("Case label must be an enum constant", caseLabel.getLocation());
                            civ = 99;
                            break;
                        }
                        String[] identifiers = ((AmbiguousName) caseLabel).identifiers;
                        if (identifiers.length != 1) {
                            this.context.compileError("Case label must be a plain enum constant", caseLabel.getLocation());
                            civ = 99;
                            break;
                        }
                        String constantName = identifiers[0];

                        int ordinal = 0;
                        for (IField f : UnitCompiler.rawTypeOf(switchExpressionType).getDeclaredIFields()) {
                            if (f.getAccess() != Access.PUBLIC || !f.isStatic()) continue;
                            if (f.getName().equals(constantName)) {
                                civ = ordinal;
                                break CIV;
                            }
                            ordinal++;
                        }

                        this.context.compileError("Unknown enum constant \"" + constantName + "\"", caseLabel.getLocation());
                        civ = 99;
                    }

                    // Store in case label map.
                    if (caseLabelMap.containsKey(civ)) {
                        this.context.compileError("Duplicate \"case\" switch label value", caseLabel.getLocation());
                    }
                    caseLabelMap.put(civ, sbsgOffsets[i]);
                    break;

                case INT:
                    {
                        // Verify that case label value is a constant.
                        Object cv = this.constantEvaluator.getConstantValue(caseLabel);
                        if (cv == UnitCompiler.NOT_CONSTANT) {
                            this.context.compileError(
                                "Value of 'case' label does not pose a constant value",
                                caseLabel.getLocation()
                            );
                            civ = 99;
                            break;
                        }

                        // Convert char, byte, short, int to "Integer".
                        if (cv instanceof Integer) {
                            civ = (Integer) cv;
                        } else
                        if (cv instanceof Number) {
                            civ = Integer.valueOf(((Number) cv).intValue());
                        } else
                        if (cv instanceof Character) {
                            civ = Integer.valueOf(((Character) cv).charValue());
                        } else {
                            this.context.compileError(
                                "Value of case label must be a char, byte, short or int constant",
                                caseLabel.getLocation()
                            );
                            civ = Integer.valueOf(99);
                        }
                    }

                    // Store in case label map.
                    if (caseLabelMap.containsKey(civ)) {
                        this.context.compileError("Duplicate \"case\" switch label value", caseLabel.getLocation());
                    }
                    caseLabelMap.put(civ, sbsgOffsets[i]);
                    break;

                case STRING:
                    {

                        // Verify that the case label value is a string constant.
                        Object cv = this.constantEvaluator.getConstantValue(caseLabel);
                        if (!(cv instanceof String)) {
                            this.context.compileError(
                                "Value of 'case' label is not a string constant",
                                caseLabel.getLocation()
                            );
                            civ = 99;
                            break;
                        }

                        // Use the string constant's hash code as the SWITCH key.
                        civ = cv.hashCode();
                    }

                    // Store in case label map.
                    if (!caseLabelMap.containsKey(civ)) {
                        caseLabelMap.put(civ, this.context.getCodeContext().new BasicBlock());
                    }
                    break;

                default:
                    throw new AssertionError(kind);
                }
            }

            if (sbsg.hasDefaultLabel) {
                if (defaultLabelOffset != null) {
                    this.context.compileError("Duplicate \"default\" switch label", sbsg.getLocation());
                }
                defaultLabelOffset = sbsgOffsets[i];
            }
        }
        if (defaultLabelOffset == null) defaultLabelOffset = this.getWhereToBreak(ss);

        // Compute the condition.
        this.context.compileGetValue(ss.condition);

        // Postprocess the condition.
        switch (kind) {

        case STRING:

            // Store the string value in a (hidden) local variable, because after we do the SWITCH
            // on the string's hash code, we need to check for string equality with the CASE
            // labels.
            this.bytecodeEmitter.dup(ss);
            ssvLvIndex = this.context.getCodeContext().allocateLocalVariable((short) 1);
            this.bytecodeEmitter.store(
                ss,                                      // locatable
                this.iClassLoader.TYPE_java_lang_String, // lvType
                ssvLvIndex                               // lvIndex
            );
            this.bytecodeEmitter.invokeMethod(ss, this.iClassLoader.METH_java_lang_String__hashCode);
            break;

        case ENUM:
            this.bytecodeEmitter.invokeMethod(ss, this.iClassLoader.METH_java_lang_Enum__ordinal);
            break;

        case INT:
            this.context.assignmentConversion(
                ss,                   // locatable
                switchExpressionType, // sourceType
                IClass.INT,           // targetType
                null                  // constantValue
            );
            break;

        default:
            throw new AssertionError(kind);
        }

        // Generate TABLESWITCH or LOOKUPSWITCH instruction.
        if (caseLabelMap.isEmpty()) {
            // Special case: SWITCH statement without CASE labels (but maybe a DEFAULT label).
            this.bytecodeEmitter.pop(ss, IClass.INT);
        } else
        if (
            (Integer) caseLabelMap.firstKey() + caseLabelMap.size() // Beware of INT overflow!
            >= (Integer) caseLabelMap.lastKey() - caseLabelMap.size()
        ) {

            // The case label values are strictly consecutive or almost consecutive (at most 50%
            // 'gaps'), so let's use a TABLESWITCH.

            this.bytecodeEmitter.tableswitch(ss, caseLabelMap, defaultLabelOffset);
        } else
        {

            // The case label values are not 'consecutive enough', so use a LOOKUPSWITCH.
            this.bytecodeEmitter.lookupswitch(ss, caseLabelMap, defaultLabelOffset);
        }

        // Case entries see assignments performed by the selector, e.g. switch (c = read()).
        StackMap smBeforeSwitch = this.context.getCodeContext().currentInserter().getStackMap();

        if (kind == SwitchKind.STRING) {

            // For STRING SWITCH, we must generate extra code that checks for string equality --
            // the strings' hash codes are not globally unique (as, e.g. MD5).
            StackMap smBeforeSbsg = this.context.getCodeContext().currentInserter().getStackMap();
            for (Entry<Integer, CodeContext.Offset> e : caseLabelMap.entrySet()) {
                final Integer            caseHashCode = (Integer) e.getKey();
                final CodeContext.Offset offset       = (CodeContext.Offset) e.getValue();

                offset.set();

                Set<String> caseLabelValues = new HashSet<>();
                for (int i = 0; i < ss.sbsgs.size(); i++) {
                    SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);

                    this.context.getCodeContext().currentInserter().setStackMap(smBeforeSbsg);
                    for (Rvalue caseLabel : sbsg.caseLabels) {

                        String cv = (String) this.constantEvaluator.getConstantValue(caseLabel);
                        assert cv != null;

                        if (!caseLabelValues.add(cv)) {
                            this.context.compileError(
                                "Duplicate case label \"" + cv + "\"",
                                caseLabel.getLocation()
                            );
                        }

                        if (cv.hashCode() != caseHashCode) continue;

                        this.bytecodeEmitter.load(sbsg, this.iClassLoader.TYPE_java_lang_String, ssvLvIndex);
                        this.context.consT(caseLabel, cv);
                        this.bytecodeEmitter.invokeMethod(caseLabel, this.iClassLoader.METH_java_lang_String__equals__java_lang_Object);
                        this.bytecodeEmitter.ifxx(sbsg, UnitCompilerBytecodeEmitter.NE, sbsgOffsets[i]);
                    }
                }

                this.bytecodeEmitter.gotO(ss, defaultLabelOffset);
            }
        }

        // Compile statement groups.
        boolean canCompleteNormally = true;
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i].set();
            this.context.getCodeContext().currentInserter().setStackMap(smBeforeSwitch);
            canCompleteNormally = true;
            for (BlockStatement bs : sbsg.blockStatements) {
                if (!canCompleteNormally) {
                    this.context.compileError("Statement is unreachable", bs.getLocation());
                    break;
                }
                canCompleteNormally = this.compile(bs);
            }
        }

        Offset wtb = ss.whereToBreak;
        if (wtb == null) return canCompleteNormally;

        if (!canCompleteNormally) this.context.getCodeContext().currentInserter().setStackMap(wtb.getStackMap());
        wtb.set();

        ss.whereToBreak = null;

        return true;
    }

    private boolean
    compile2(BreakStatement bs) throws CompileException {

        // Find the broken statement.
        BreakableStatement brokenStatement = null;
        if (bs.label == null) {

            // Unlabeled BREAK:
            for (
                Scope s = bs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof BreakableStatement && !(s instanceof LabeledStatement)) {
                    brokenStatement = (BreakableStatement) s;
                    break;
                }
            }
            if (brokenStatement == null) {
                this.context.compileError("\"break\" statement is not enclosed by a breakable statement", bs.getLocation());
                return false;
            }
        } else {

            // Labeled BREAK:
            for (
                Scope s = bs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof LabeledStatement) {
                    LabeledStatement ls = (LabeledStatement) s;
                    if (ls.label.equals(bs.label)) {
                        brokenStatement = ls;
                        break;
                    }
                }
            }
            if (brokenStatement == null) {
                this.context.compileError((
                    "Statement \"break "
                    + bs.label
                    + "\" is not enclosed by a breakable statement with label \""
                    + bs.label
                    + "\""
                ), bs.getLocation());
                return false;
            }
        }

        this.leaveStatements(
            bs.getEnclosingScope(),             // from
            brokenStatement.getEnclosingScope() // to
        );
        if (this.context.getCodeContext().currentInserter().getStackMap() != null) {
            this.bytecodeEmitter.gotO(bs, this.getWhereToBreak(brokenStatement));
        }
        return false;
    }

    private boolean
    compile2(ContinueStatement cs) throws CompileException {

        // Find the continued statement.
        ContinuableStatement continuedStatement = null;
        if (cs.label == null) {

            // Unlabeled CONTINUE:
            for (
                Scope s = cs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof ContinuableStatement) {
                    continuedStatement = (ContinuableStatement) s;
                    break;
                }
            }
            if (continuedStatement == null) {
                this.context.compileError(
                    "\"continue\" statement is not enclosed by a continuable statement",
                    cs.getLocation()
                );
                return false;
            }
        } else {

            // Labeled CONTINUE:
            for (
                Scope s = cs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof LabeledStatement) {
                    LabeledStatement ls = (LabeledStatement) s;
                    if (ls.label.equals(cs.label)) {
                        Statement st = ls.body;
                        while (st instanceof LabeledStatement) st = ((LabeledStatement) st).body;
                        if (!(st instanceof ContinuableStatement)) {
                            this.context.compileError("Labeled statement is not continuable", st.getLocation());
                            return false;
                        }
                        continuedStatement = (ContinuableStatement) st;
                        break;
                    }
                }
            }
            if (continuedStatement == null) {
                this.context.compileError((
                    "Statement \"continue "
                    + cs.label
                    + "\" is not enclosed by a continuable statement with label \""
                    + cs.label
                    + "\""
                ), cs.getLocation());
                return false;
            }
        }

        this.leaveStatements(
            cs.getEnclosingScope(),                // from
            continuedStatement.getEnclosingScope() // to
        );

        if (this.context.getCodeContext().currentInserter().getStackMap() != null) {
            Offset wtc = continuedStatement.whereToContinue;
            if (wtc == null) wtc = continuedStatement.whereToContinue = this.context.getCodeContext().new BasicBlock();
            this.bytecodeEmitter.gotO(cs, wtc);
        }

        return false;
    }

    private boolean
    compile2(AssertStatement as) throws CompileException {

        // assert expression1;
        //   if (!expression1) throw new AssertionError();
        // assert expression1 : expression2;
        //   if (!expression1) throw new AssertionError(expression2);
        CodeContext.Offset end = this.context.getCodeContext().new BasicBlock();
        try {
            Scope scope = as.getEnclosingScope();
            while (!(scope instanceof Java.AbstractTypeDeclaration)) scope = scope.getEnclosingScope();
            Java.AbstractTypeDeclaration owner = (Java.AbstractTypeDeclaration) scope;
            if (owner.assertionsDisabledField == null) throw new InternalCompilerException("Assertion flag was not prepared");
            Rvalue disabled = new FieldAccessExpression(as.getLocation(), new SimpleType(as.getLocation(),
                this.context.resolve(owner)), owner.assertionsDisabledField);
            disabled.setEnclosingScope(as);
            this.context.compileBoolean(disabled, end, UnitCompiler.JUMP_IF_TRUE);
            this.context.compileBoolean(as.expression1, end, UnitCompiler.JUMP_IF_TRUE);

            this.bytecodeEmitter.neW(as, this.iClassLoader.TYPE_java_lang_AssertionError);
            this.bytecodeEmitter.dup(as);

            Rvalue[] arguments = (
                as.expression2 == null
                ? new Rvalue[0]
                : new Rvalue[] { as.expression2 }
            );
            this.context.invokeConstructor(
                as,                                              // locatable
                as,                                              // scope
                null,                                            // enclosingInstance
                this.iClassLoader.TYPE_java_lang_AssertionError, // targetClass
                arguments                                        // arguments
            );
            this.context.getCodeContext().popUninitializedVariableOperand();
            this.context.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_ASSERTIONERROR);
            this.bytecodeEmitter.athrow(as);
        } finally {
            end.setBasicBlock();
        }
        return true;
    }

    @SuppressWarnings("static-method") private boolean
    compile2(EmptyStatement es) { return true; }

    private boolean
    compile2(ExpressionStatement ee) throws CompileException {

        try {
            this.context.compile(ee.rvalue);
        } catch (InternalCompilerException ice) {
            throw new InternalCompilerException(ee.rvalue.getLocation(), null, ice);
        }

        return true;
    }

    private boolean
    compile2(FieldDeclaration fd) throws CompileException {
        final IClass declaringIClass = this.context.resolve(fd.getDeclaringType());
        for (VariableDeclarator vd : fd.variableDeclarators) {

            ArrayInitializerOrRvalue initializer = this.context.getNonConstantFinalInitializer(fd, vd);
            if (initializer == null) continue;

            // TODO: Compile annotations on fields.
//            assert fd.modifiers.annotations.length == 0 : fd.getLocation();

            try {

                this.bytecodeEmitter.addLineNumberOffset(vd);

                if (!declaringIClass.isInterface() && !fd.isStatic()) this.bytecodeEmitter.load(vd, declaringIClass, 0);

                IType fieldType = this.context.getType(vd.withType(fd.type));
                this.context.compile(initializer, fieldType);

                // No need to check accessibility here.
                ;

                // TODO: Compile annotations on fields.
//                assert fd.modifiers.annotations.length == 0;

                IField iField = declaringIClass.getDeclaredIField(vd.name);
                assert iField != null : fd.getDeclaringType() + " has no field " + vd.name;
                this.bytecodeEmitter.putfield(fd, iField);
            } catch (InternalCompilerException ice) {
                throw new InternalCompilerException(initializer.getLocation(), null, ice);
            }
        }
        return true;
    }

    private boolean
    compile2(IfStatement is) throws CompileException {
        final BlockStatement ts = is.thenStatement;
        final BlockStatement es = is.elseStatement != null ? is.elseStatement : new EmptyStatement(ts.getLocation());

        Object cv = this.constantEvaluator.getConstantValue(is.condition);
        if (cv instanceof Boolean) {

            // Constant condition.
            this.context.fakeCompile(is.condition);
            BlockStatement seeingStatement, blindStatement;
            if (((Boolean) cv).booleanValue()) {
                seeingStatement = ts;
                blindStatement  = es;
            } else {
                seeingStatement = es;
                blindStatement  = ts;
            }

            // Compile the seeing statement.
            final CodeContext.Inserter ins                     = this.context.getCodeContext().newInserter();
            StackMap                   smBeforeSeeingStatement = this.context.getCodeContext().currentInserter().getStackMap();
            boolean                    ssccn                   = this.compile(seeingStatement);
            Offset                     afterSeeingStatement    = this.context.getCodeContext().newOffset();

            // Fake-compile the blind statement.
            this.context.getCodeContext().currentInserter().setStackMap(smBeforeSeeingStatement);
            boolean bsccn = this.fakeCompile(blindStatement);
            afterSeeingStatement.setStackMap(); // Merge stack maps after seeing and after blind statements.

            if (ssccn) return true;
            if (!bsccn) return false;

            // Hm... the "seeing statement" cannot complete normally, but the "blind statement" can. Things are getting
            // complicated here! The robust solution is to compile the constant-condition-IF statement as a
            // non-constant-condition-IF statement. As an optimization, iff the IF-statement is enclosed ONLY by blocks,
            // then the remaining bytecode can be written to a "fake" code context, i.e. be thrown away.

            // Compile constant-condition-IF statement as non-constant-condition-IF statement.
            CodeContext.Offset off = this.context.getCodeContext().newBasicBlock();

            this.context.getCodeContext().pushInserter(ins);
            try {
                this.context.consT(is, Boolean.FALSE);
                this.bytecodeEmitter.ifxx(is, UnitCompilerBytecodeEmitter.NE, off);
            } finally {
                this.context.getCodeContext().popInserter();
            }

            // Now return "ccn=true" so that the following statements are not reported as "unreachable".
            return true;
        }

        // Non-constant condition.
        if (this.generatesCode(ts)) {
            if (this.generatesCode(es)) {

                // if (<expression>) <then-statement> else <else-statement>
                CodeContext.Offset eso = this.context.getCodeContext().new BasicBlock();
                CodeContext.Offset end = this.context.getCodeContext().new BasicBlock();
                this.context.compileBoolean(is.condition, eso, UnitCompiler.JUMP_IF_FALSE);

                // Compile "then" statement.
                boolean tsccn = this.compile(ts);
                if (tsccn) {
                    this.bytecodeEmitter.gotO(is, end);
                }

                // Compile "else" statment.
                eso.setBasicBlock();
                boolean esccn = this.compile(es);

                if (!tsccn && !esccn) return false;


                end.setBasicBlock();
                return tsccn || esccn;
            } else {

                // if (<expression>) <then-statement> else ;
                CodeContext.Offset end = this.context.getCodeContext().new BasicBlock();
                this.context.compileBoolean(is.condition, end, UnitCompiler.JUMP_IF_FALSE);
                this.compile(ts);
                end.setBasicBlock();
                return true;
            }
        } else {
            if (this.generatesCode(es)) {

                // if (<expression>) ; else <else-statement>
                CodeContext.Offset end = this.context.getCodeContext().new BasicBlock();
                this.context.compileBoolean(is.condition, end, UnitCompiler.JUMP_IF_TRUE);
                this.compile(es);
                end.setBasicBlock();
                return true;
            } else {

                // if (<expression>) ; else ;
                IType conditionType = this.context.compileGetValue(is.condition);
                if (conditionType != IClass.BOOLEAN) this.context.compileError("Not a boolean expression", is.getLocation());
                this.bytecodeEmitter.pop(is, conditionType);
                return true;
            }
        }
    }

    private boolean
    compile2(LocalClassDeclarationStatement lcds) throws CompileException {

        // Check for redefinition.
        LocalClassDeclaration otherLcd = UnitCompilerStatementCompiler.this.context.findLocalClassDeclaration(lcds, lcds.lcd.name);
        if (otherLcd != null && otherLcd != lcds.lcd) {
            this.context.compileError(
                "Redeclaration of local class \""
                + lcds.lcd.name
                + "\"; previously declared in "
                + otherLcd.getLocation()
            );
        }

        this.context.compile(lcds.lcd);
        return true;
    }

    private boolean
    compile2(LocalVariableDeclarationStatement lvds) throws CompileException {

        // Ignore annotations here.

        for (VariableDeclarator vd : lvds.variableDeclarators) {

            try {
                LocalVariable lv = this.context.getLocalVariable(lvds, vd);
                lv.setSlot(this.context.allocateLocalVariableSlot(lv.type, vd.name));

                ArrayInitializerOrRvalue initializer = vd.initializer;
                if (initializer != null) {
                    this.context.compile(initializer, lv.type);
                    this.bytecodeEmitter.store(lvds, lv);
                    if (lv.finaL && (UnitCompiler.isPrimitive(lv.type)
                        || UnitCompiler.rawTypeOf(lv.type) == this.iClassLoader.TYPE_java_lang_String)) {
                        Object constant = this.constantEvaluator.getConstantValue(initializer);
                        if (constant != null && constant != UnitCompiler.NOT_CONSTANT) {
                            this.constantEvaluator.rememberLocalConstant(lv, this.context.constantAssignmentConversion(initializer, constant, lv.type));
                        }
                    }
                }
                this.context.compileLocalTypeAnnotations(vd.withType(lvds.type), lvds.modifiers, (byte) 0x40, lv.slot);
            } catch (RuntimeException re) {
                throw new RuntimeException(vd.getLocation().toString(), re);
            }
        }
        return true;
    }

    private boolean
    compile2(ReturnStatement rs) throws CompileException {

        // Determine enclosing block, function and compilation Unit.
        FunctionDeclarator enclosingFunction = null;
        {
            Scope s = rs.getEnclosingScope();
            while (s instanceof Statement || s instanceof CatchClause) s = s.getEnclosingScope();
            enclosingFunction = (FunctionDeclarator) s;
        }

        Rvalue orv = rs.returnValue;

        IType returnType = this.context.getReturnType(enclosingFunction);
        if (returnType == IClass.VOID) {
            if (orv != null) this.context.compileError("Method must not return a value", rs.getLocation());
            this.leaveStatements(
                rs.getEnclosingScope(), // from
                enclosingFunction       // to
            );

            if (this.context.getCodeContext().currentInserter().getStackMap() != null) this.bytecodeEmitter.returN(rs);
            return false;
        }

        if (orv == null) {
            this.context.compileError("Method must return a value", rs.getLocation());
            return false;
        }
        this.context.setLambdaTargetType(orv, returnType);
        IType type = this.context.compileGetValue(orv);
        this.context.assignmentConversion(
            rs,                        // locatable
            type,                      // sourceType
            returnType,                // targetType
            this.constantEvaluator.getConstantValue(orv) // constantValue
        );

        // FINALLY may contain a TRY/CATCH of its own. A handler starts with only the exception
        // on its operand stack, so keep the pending return value in a local across cleanup.
        this.context.getCodeContext().saveLocalVariables();
        try {
            LocalVariable result = null;
            Scope previous = null;
            for (Scope scope = rs.getEnclosingScope(); scope != enclosingFunction; scope = scope.getEnclosingScope()) {
                if (scope instanceof TryStatement
                    && (((TryStatement) scope).finallY != null || !((TryStatement) scope).resources.isEmpty())
                    && ((TryStatement) scope).finallY != previous) {
                    result = this.context.allocateLocalVariable(true, returnType);
                    this.bytecodeEmitter.store(rs, result);
                    break;
                }
                previous = scope;
            }
            this.leaveStatements(
                rs.getEnclosingScope(), // from
                enclosingFunction       // to
            );
            if (this.context.getCodeContext().currentInserter().getStackMap() != null) {
                if (result != null) this.bytecodeEmitter.load(rs, result);
                this.bytecodeEmitter.xreturn(rs, returnType);
            }
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }
        return false;
    }

    private boolean
    compile2(SynchronizedStatement ss) throws CompileException {

        // Evaluate monitor object expression.
        if (!UnitCompilerStatementCompiler.this.context.isAssignableFrom(this.iClassLoader.TYPE_java_lang_Object, this.context.compileGetValue(ss.expression))) {
            this.context.compileError(
                "Monitor object of \"synchronized\" statement is not a subclass of \"Object\"",
                ss.getLocation()
            );
        }

        this.context.getCodeContext().saveLocalVariables();
        boolean canCompleteNormally = false;
        try {

            // Allocate a local variable for the monitor object.
            ss.monitorLvIndex = this.context.getCodeContext().allocateLocalVariable((short) 1);

            // Store the monitor object.
            this.bytecodeEmitter.dup(ss);
            this.bytecodeEmitter.store(ss, this.iClassLoader.TYPE_java_lang_Object, ss.monitorLvIndex);

            // Create lock on the monitor object.
            this.bytecodeEmitter.monitorenter(ss);

            // Compile the statement body.
            final CodeContext.Offset monitorExitOffset = this.context.getCodeContext().new BasicBlock();
            final CodeContext.Offset beginningOfBody   = this.context.getCodeContext().newOffset();
            StackMap smBeforeBody = this.context.getCodeContext().currentInserter().getStackMap();
            ProtectedRegion region = new ProtectedRegion(ss, this.context.getCodeContext(), false);
            this.protectedRegions.add(region);
            try {
                canCompleteNormally = this.compile(ss.body);
            } finally {
                this.protectedRegions.remove(region);
            }
            if (canCompleteNormally) {
                this.bytecodeEmitter.gotO(ss, monitorExitOffset);
            }

            StackMap save = this.context.getCodeContext().currentInserter().getStackMap();
            try {
                this.context.getCodeContext().currentInserter().setStackMap(smBeforeBody);
                this.context.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_THROWABLE);

                // Generate the exception handler.
                CodeContext.Offset here = this.context.getCodeContext().newBasicBlock();
                this.addExceptionTableEntries(region, beginningOfBody, here, here, null);
                this.leave(ss);
                this.bytecodeEmitter.athrow(ss);
            } finally {
                this.context.getCodeContext().currentInserter().setStackMap(save);
            }

            // Unlock monitor object.
            if (canCompleteNormally) {
                monitorExitOffset.set();
                this.leave(ss);
            }
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }

        return canCompleteNormally;
    }

    private boolean
    compile2(ThrowStatement ts) throws CompileException {
        IType expressionType = this.context.compileGetValue(ts.expression);

        // Final or effectively final catch parameters retain the exceptions caught from the protected body.
        List<IType> preciseRethrownTypes = this.context.preciseRethrowTypes(ts);
        if (preciseRethrownTypes == null) {
            this.context.checkThrownException(
                ts,                    // locatable
                expressionType,        // type
                ts.getEnclosingScope() // scope
            );
        } else {
            for (IType thrownType : preciseRethrownTypes) {
                this.context.checkThrownException(
                    ts,          // locatable
                    thrownType,  // type
                    ts.getEnclosingScope() // scope
                );
            }
        }
        this.bytecodeEmitter.athrow(ts);
        return false;
    }

    /**
     * Interface for delayed code generation.
     */
    interface Compilable2 { boolean compile() throws CompileException; }

    private boolean
    compile2(final TryStatement ts) throws CompileException {

        return this.compileTryCatchFinally(ts, new Compilable2() {
            @Override public boolean compile() throws CompileException {
                return UnitCompilerStatementCompiler.this.compileResources(ts, 0);
            }
        }, ts.finallY);
    }

    /**
     * Generates code for a TRY statement with (possibly zero) resources and an (optional) FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileResources(final TryStatement ts, final int resourceIndex) throws CompileException {
        if (resourceIndex == ts.resources.size()) return this.compile(ts.body);
        TryStatement.Resource firstResource = ts.resources.get(resourceIndex);

        final Location loc = firstResource.getLocation();
        final IClass   tt  = this.iClassLoader.TYPE_java_lang_Throwable;

        this.context.getCodeContext().saveLocalVariables();
        try {

            LocalVariable
            identifier = (LocalVariable) firstResource.accept(
                new TryStatementResourceVisitor<LocalVariable, CompileException>() {

                    @Override @Nullable public LocalVariable
                    visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws CompileException {

                        // final {VariableModifierNoFinal} R Identifier = Expression
                        LocalVariable result = lvdr.variableDeclarator.localVariable;
                        assert result != null;
                        result.setSlot(UnitCompilerStatementCompiler.this.context.allocateLocalVariableSlot(result.type, lvdr.variableDeclarator.name));
                        ArrayInitializerOrRvalue initializer = lvdr.variableDeclarator.initializer;
                        assert initializer != null;

                        // Fresh scope: an initializer sees earlier resources, but never later resources.
                        Block scope = new Block(loc);
                        scope.setEnclosingScope(ts);
                        Map<String, LocalVariable> locals = new HashMap<>(ts.localVariables);
                        for (int i = 0; i < resourceIndex; i++) {
                            TryStatement.Resource prior = ts.resources.get(i);
                            if (prior instanceof LocalVariableDeclaratorResource) {
                                VariableDeclarator vd = ((LocalVariableDeclaratorResource) prior).variableDeclarator;
                                locals.put(vd.name, vd.localVariable);
                            }
                        }
                        locals.put(lvdr.variableDeclarator.name, result);
                        scope.localVariables = locals;
                        initializer = new DeepCopier().copyArrayInitializerOrRvalue(initializer);
                        initializer.setEnclosingScope(scope);
                        UnitCompilerStatementCompiler.this.initializingResources.add(result);
                        try {
                            UnitCompilerStatementCompiler.this.context.compile(initializer, result.type);
                        } finally {
                            UnitCompilerStatementCompiler.this.initializingResources.remove(result);
                        }
                        UnitCompilerStatementCompiler.this.bytecodeEmitter.store(ts, result);
                        UnitCompilerStatementCompiler.this.context.compileLocalTypeAnnotations(lvdr.type, lvdr.modifiers, (byte) 0x41, result.slot);

                        return result;
                    }

                    @Override @Nullable public LocalVariable
                    visitVariableAccessResource(VariableAccessResource var) throws CompileException {

                        // Expression
                        if (
                            !UnitCompilerStatementCompiler.this.context.options().contains(
                                JaninoOption.EXPRESSIONS_IN_TRY_WITH_RESOURCES_ALLOWED
                            )
                            && !(var.variableAccess instanceof AmbiguousName)
                            && !(var.variableAccess instanceof FieldAccessExpression)
                            && !(var.variableAccess instanceof SuperclassFieldAccessExpression)
                        ) {
                            throw new CompileException(
                                var.variableAccess.getClass().getSimpleName() + " rvalue not allowed as a resource",
                                var.getLocation()
                            );
                        }
                        LocalVariable result = UnitCompilerStatementCompiler.this.context.allocateLocalVariable(
                            true,                                                 // finaL
                            UnitCompilerStatementCompiler.this.context.compileGetValue(var.variableAccess) // localVariableType
                        );
                        UnitCompilerStatementCompiler.this.bytecodeEmitter.store(ts, result);
                        return result;
                    }
                }
            );
            assert identifier != null;
            try {
                IClass autoCloseable = this.iClassLoader.loadIClass("Ljava/lang/AutoCloseable;");
                if (autoCloseable == null || !autoCloseable.isAssignableFrom(UnitCompiler.rawTypeOf(identifier.type))) {
                    this.context.compileError("Resource type must implement java.lang.AutoCloseable", loc);
                }
            } catch (ClassNotFoundException ex) {
                throw new CompileException("Cannot load java.lang.AutoCloseable", loc, ex);
            }

            // Throwable #primaryExc = null;
            LocalVariable primaryExc = this.context.allocateLocalVariable(true /*finaL*/, tt);
            this.context.consT(ts, (Object) null);
            this.bytecodeEmitter.store(ts, primaryExc);

            CatchParameter suppressedException = new CatchParameter(
                loc,                                    // location
                false,                                  // finaL
                new Type[] { new SimpleType(loc, tt) }, // types
                "___"                                   // name
            );

            // Generate the FINALLY clause for the TRY-with-resources statement; see JLS9 14.20.3.1.
            // if (Identifier != null) {
            //     if (#primaryExc != null) {
            //         try {
            //             Identifier.close();
            //         } catch (Throwable #suppressedExc) {
            //             // Only iff Java >= 7:
            //             #primaryExc.addSuppressed(#suppressedExc);
            //         }
            //     } else {
            //         Identifier.close();
            //     }
            // }
            BlockStatement afterClose = (
                this.iClassLoader.METH_java_lang_Throwable__addSuppressed == null
                ? new EmptyStatement(loc)
                : new ExpressionStatement(
                    new MethodInvocation(
                        loc,                                      // location
                        new LocalVariableAccess(loc, primaryExc), // target
                        "addSuppressed",                          // methodName
                        new Rvalue[] {                            // arguments
                            new LocalVariableAccess(loc, this.context.getLocalVariable(suppressedException)),
                        }
                    )
                )
            );
            BlockStatement f = new IfStatement(
                loc,                 // location
                new BinaryOperation( // condition
                    loc,                                      // location
                    new LocalVariableAccess(loc, identifier), // lhs
                    "!=",                                     // operator
                    new NullLiteral(loc)                      // rhs
                ),
                new IfStatement(     // thenStatement
                    loc,                         // location
                    new BinaryOperation(         // condition
                        loc,                                      // location
                        new LocalVariableAccess(loc, primaryExc), // lhs
                        "!=",                                     // operator
                        new NullLiteral(loc)                      // rhs
                    ),
                    new TryStatement(            // thenStatement
                        loc,                                       // location
                        new ExpressionStatement(                   // body
                            new MethodInvocation(loc, new LocalVariableAccess(loc, identifier), "close", new Rvalue[0])
                        ),
                        Collections.singletonList(new CatchClause( // catchClauses
                            loc,                   // location
                            suppressedException,   // caughtException
                            afterClose             // body
                        ))
                    ),
                    new ExpressionStatement(     // elseStatement
                        new MethodInvocation(loc, new LocalVariableAccess(loc, identifier), "close", new Rvalue[0])
                    )
                )
            );
            f.setEnclosingScope(ts);

            // Each resource owns one protected region. User catches and finally surround ALL resources.
            ResourceCleanup cleanup = new ResourceCleanup(ts, f, this.context.getCodeContext());
            this.resourceCleanups.add(cleanup);
            this.protectedRegions.add(cleanup.region);
            CodeContext.Offset start = this.context.getCodeContext().newOffset();
            StackMap before = this.context.getCodeContext().currentInserter().getStackMap();
            boolean normal;
            try {
                normal = this.compileResources(ts, resourceIndex + 1);
            } finally {
                this.protectedRegions.remove(cleanup.region);
                this.resourceCleanups.remove(cleanup);
            }
            CodeContext.Offset end = this.context.getCodeContext().newOffset();
            StackMap after = this.context.getCodeContext().currentInserter().getStackMap();
            CodeContext.Offset done = this.context.getCodeContext().new BasicBlock();
            if (normal) this.bytecodeEmitter.gotO(ts, done);
            if (start.offset != end.offset) {
                this.context.getCodeContext().currentInserter().setStackMap(before);
                this.context.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_THROWABLE);
                CodeContext.Offset handler = this.context.getCodeContext().newBasicBlock();
                this.addExceptionTableEntries(cleanup.region, start, end, handler, null);
                this.bytecodeEmitter.store(ts, primaryExc);
                this.compile(f);
                this.bytecodeEmitter.load(ts, primaryExc);
                this.bytecodeEmitter.athrow(ts);
            }
            this.context.getCodeContext().currentInserter().setStackMap(after);
            done.set();
            if (normal) this.compile(f);
            return normal;
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    /**
     * Generates code for a TRY statement without resources, but with an (optional) FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileTryCatchFinally(
        final TryStatement             ts,
        final Compilable2              compileBody,
        @Nullable final BlockStatement finallY
    ) throws CompileException {

        if (finallY == null) {
            final CodeContext.Offset beginningOfBody = this.context.getCodeContext().newOffset();
            final CodeContext.Offset afterStatement  = this.context.getCodeContext().new BasicBlock();

            boolean canCompleteNormally = this.compileTryCatch(ts, compileBody, beginningOfBody, afterStatement);
            afterStatement.set();
            return canCompleteNormally;
        }

        // Compile a TRY statement *with* a FINALLY clause.

        final CodeContext.Offset afterStatement = this.context.getCodeContext().new BasicBlock();
        boolean                  canCompleteNormally;

        this.context.getCodeContext().saveLocalVariables();
        try {

            StackMap smBeforeBody = this.context.getCodeContext().currentInserter().getStackMap();

            final CodeContext.Offset beginningOfBody = this.context.getCodeContext().newOffset();
            ProtectedRegion region = new ProtectedRegion(ts, this.context.getCodeContext(), false);
            this.protectedRegions.add(region);
            try {
                canCompleteNormally = this.compileTryCatch(ts, compileBody, beginningOfBody, afterStatement);
            } finally {
                this.protectedRegions.remove(region);
            }

            StackMap smAfterBody = this.context.getCodeContext().currentInserter().getStackMap();

            // Generate the "catch (any) {" clause that invokes the FINALLY subroutine.
            this.context.getCodeContext().saveLocalVariables();
            try {

                this.context.getCodeContext().currentInserter().setStackMap(smBeforeBody);

                // Push the exception on the operand stack.
                this.context.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_THROWABLE);

                CodeContext.Offset here = this.context.getCodeContext().newBasicBlock();
                this.addExceptionTableEntries(region, beginningOfBody, here, here, null);

                // Save the exception object in an anonymous local variable.
                short evi = this.context.getCodeContext().allocateLocalVariable((short) 1);
                this.bytecodeEmitter.store(
                    finallY,                                    // locatable
                    this.iClassLoader.TYPE_java_lang_Throwable, // lvType
                    evi                                         // lvIndex
                );

                if (this.compile(finallY)) {

                    this.bytecodeEmitter.load(
                        finallY,                                    // locatable
                        this.iClassLoader.TYPE_java_lang_Throwable, // type
                        evi                                         // index
                    );
                    this.bytecodeEmitter.athrow(finallY);
                }

                this.context.getCodeContext().currentInserter().setStackMap(smAfterBody);
            } finally {

                // The exception object local variable allocated above MUST NOT BE RELEASED until after the FINALLY
                // block is compiled, for otherwise you get
                //   java.lang.VerifyError: ... Accessing value from uninitialized register 7
                this.context.getCodeContext().restoreLocalVariables();
            }
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }

        afterStatement.set();

        if (canCompleteNormally) canCompleteNormally = UnitCompilerStatementCompiler.this.compile(finallY);

        return canCompleteNormally;
    }

    /**
     * Generates code for a TRY statement without resources and without a FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileTryCatch(
        TryStatement             tryStatement,
        Compilable2              compileBody,
        final CodeContext.Offset beginningOfBody,
        final CodeContext.Offset afterStatement
    ) throws CompileException {

        // Initialize all catch clauses as "unreachable" only to check later that they ARE indeed reachable.
        for (CatchClause catchClause : tryStatement.catchClauses) {
            catchClause.reachable = false;
            for (Type t : catchClause.catchParameter.types) {
                IType caughtExceptionType = this.context.getType(t);
                catchClause.reachable |= (
                    // Superclass or subclass of "java.lang.Error"?
                    UnitCompilerStatementCompiler.this.context.isAssignableFrom(this.iClassLoader.TYPE_java_lang_Error, caughtExceptionType)
                    || UnitCompilerStatementCompiler.this.context.isAssignableFrom(caughtExceptionType, this.iClassLoader.TYPE_java_lang_Error)
                    // Superclass or subclass of "java.lang.RuntimeException"?
                    || UnitCompilerStatementCompiler.this.context.isAssignableFrom(this.iClassLoader.TYPE_java_lang_RuntimeException, caughtExceptionType)
                    || UnitCompilerStatementCompiler.this.context.isAssignableFrom(caughtExceptionType, this.iClassLoader.TYPE_java_lang_RuntimeException)
                );
            }
        }

        StackMap smBeforeBody = this.context.getCodeContext().currentInserter().getStackMap();

        ProtectedRegion region = new ProtectedRegion(tryStatement, this.context.getCodeContext(), false);
        this.protectedRegions.add(region);
        boolean bodyCcn;
        try {
            bodyCcn = compileBody.compile();
        } finally {
            this.protectedRegions.remove(region);
        }

        CodeContext.Offset afterBody = this.context.getCodeContext().newOffset();

        StackMap smAfterBody = this.context.getCodeContext().currentInserter().getStackMap();

        if (bodyCcn) {
            this.bytecodeEmitter.gotO(tryStatement, afterStatement);
        }

        boolean catchCcn = false; // "At least one catch clause can complete normally"
        if (beginningOfBody.offset != afterBody.offset) { // Avoid zero-length exception table entries.
            for (int i = 0; i < tryStatement.catchClauses.size(); ++i) {
                this.context.getCodeContext().currentInserter().setStackMap(smBeforeBody);

                CatchClause catchClause = (CatchClause) tryStatement.catchClauses.get(i);
                boolean clauseCcn;
                this.context.getCodeContext().saveLocalVariables();
                try {

                    // Determine the type of each CATCH parameter alternative and the least upper bound of them,
                    // which is the declared type of the CATCH parameter (JLS7 14.20).
                    List<IClass> caughtExceptionTypes = new ArrayList<>();
                    for (Type t : catchClause.catchParameter.types) {
                        IClass cet = this.context.getRawType(t);

                        if (!this.iClassLoader.TYPE_java_lang_Throwable.isAssignableFrom(cet)) {
                            this.context.compileError((
                                "Caught exception type \""
                                + cet
                                + "\" is not assignable to \"Throwable\""
                            ), t.getLocation());
                        }

                        // Alternatives in a multi-CATCH must be disjoint (JLS7 14.20).
                        for (IClass prev : caughtExceptionTypes) {
                            if (prev.isAssignableFrom(cet) || cet.isAssignableFrom(prev)) {
                                this.context.compileError((
                                    "Alternatives in a multi-CATCH are related by subclassing: \""
                                    + cet
                                    + "\", \""
                                    + prev
                                    + "\""
                                ), catchClause.catchParameter.getLocation());
                            }
                        }

                        caughtExceptionTypes.add(cet);
                    }
                    IClass caughtExceptionType = this.context.lub(caughtExceptionTypes);

                    // Verify that the CATCH clause is reachable.
                    if (!catchClause.reachable) {
                        this.context.compileError("Catch clause is unreachable", catchClause.getLocation());
                    }

                    // Push the exception on the operand stack.
                    this.context.getCodeContext().pushObjectOperand(caughtExceptionType.getDescriptor());

                    // Allocate the "exception variable".
                    LocalVariableSlot
                    exceptionVarSlot = this.context.allocateLocalVariableSlot(caughtExceptionType, catchClause.catchParameter.name);

                    // Kludge: Treat the exception variable like a local variable of the catch clause body.
                    this.context.getLocalVariable(catchClause.catchParameter).setSlot(exceptionVarSlot);

                    // One exception table entry per alternative, all pointing to the same handler.
                    {
                        CodeContext.Offset handlerPc = this.context.getCodeContext().newBasicBlock();
                        for (int alternative = 0; alternative < caughtExceptionTypes.size(); alternative++) {
                            IClass cet = caughtExceptionTypes.get(alternative);
                            int firstEntry = this.context.getCodeContext().exceptionTableEntryCount();
                            this.addExceptionTableEntries(region, beginningOfBody, afterBody, handlerPc, cet.getDescriptor());
                            for (int entry = firstEntry; entry < this.context.getCodeContext().exceptionTableEntryCount(); entry++) {
                                this.context.compileCatchTypeAnnotations(catchClause.catchParameter.types[alternative],
                                    alternative == 0 ? catchClause.catchParameter.modifiers : new Java.Modifier[0], entry);
                            }
                        }
                    }
                    this.bytecodeEmitter.store(
                        catchClause,                    // locatable
                        caughtExceptionType,            // lvType
                        exceptionVarSlot.getSlotIndex() // lvIndex
                    );

                    clauseCcn = this.compile(catchClause.body);
                } finally {
                    this.context.getCodeContext().restoreLocalVariables();
                }
                if (clauseCcn) {
                    // Merge after releasing the catch parameter's slot; it is out of scope at the join and
                    // may be reused for a different type by a subsequent try statement.
                    catchCcn = true;
                    afterStatement.setStackMap();
                    this.bytecodeEmitter.gotO(catchClause, afterStatement);
                    // The common FINALLY clause, if any, is compiled at "afterStatement".
                }
            }
        }

        this.context.getCodeContext().currentInserter().setStackMap(smAfterBody);

        return bodyCcn | catchCcn;
    }

    // ------------ BlockStatement.generatesCode() -------------

    /**
     * Checks whether invocation of {@link #compile(BlockStatement)} would generate more than zero code bytes.
     */
    private boolean
    generatesCode(BlockStatement bs) throws CompileException {

        Boolean result = (Boolean) bs.accept(new BlockStatementVisitor<Boolean, CompileException>() {
            @Override public Boolean visitInitializer(Initializer i)                        throws CompileException { return UnitCompilerStatementCompiler.this.generatesCode2(i);    }
            @Override public Boolean visitFieldDeclaration(FieldDeclaration fd)             throws CompileException { return UnitCompilerStatementCompiler.this.generatesCode2(fd);   }
            @Override public Boolean visitLabeledStatement(LabeledStatement ls)                                     { return UnitCompilerStatementCompiler.this.generatesCode2(ls);   }
            @Override public Boolean visitBlock(Block b)                                    throws CompileException { return UnitCompilerStatementCompiler.this.generatesCode2(b);    }
            @Override public Boolean visitExpressionStatement(ExpressionStatement es)                               { return UnitCompilerStatementCompiler.this.generatesCode2(es);   }
            @Override public Boolean visitIfStatement(IfStatement is)                                               { return UnitCompilerStatementCompiler.this.generatesCode2(is);   }
            @Override public Boolean visitForStatement(ForStatement fs)                                             { return UnitCompilerStatementCompiler.this.generatesCode2(fs);   }
            @Override public Boolean visitForEachStatement(ForEachStatement fes)                                    { return UnitCompilerStatementCompiler.this.generatesCode2(fes);  }
            @Override public Boolean visitWhileStatement(WhileStatement ws)                                         { return UnitCompilerStatementCompiler.this.generatesCode2(ws);   }
            @Override public Boolean visitTryStatement(TryStatement ts)                                             { return UnitCompilerStatementCompiler.this.generatesCode2(ts);   }
            @Override public Boolean visitSwitchStatement(SwitchStatement ss)                                       { return UnitCompilerStatementCompiler.this.generatesCode2(ss);   }
            @Override public Boolean visitSynchronizedStatement(SynchronizedStatement ss)                           { return UnitCompilerStatementCompiler.this.generatesCode2(ss);   }
            @Override public Boolean visitDoStatement(DoStatement ds)                                               { return UnitCompilerStatementCompiler.this.generatesCode2(ds);   }
            @Override public Boolean visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { return UnitCompilerStatementCompiler.this.generatesCode2(lvds); }
            @Override public Boolean visitReturnStatement(ReturnStatement rs)                                       { return UnitCompilerStatementCompiler.this.generatesCode2(rs);   }
            @Override public Boolean visitThrowStatement(ThrowStatement ts)                                         { return UnitCompilerStatementCompiler.this.generatesCode2(ts);   }
            @Override public Boolean visitBreakStatement(BreakStatement bs)                                         { return UnitCompilerStatementCompiler.this.generatesCode2(bs);   }
            @Override public Boolean visitContinueStatement(ContinueStatement cs)                                   { return UnitCompilerStatementCompiler.this.generatesCode2(cs);   }
            @Override public Boolean visitAssertStatement(AssertStatement as)                                       { return UnitCompilerStatementCompiler.this.generatesCode2(as);   }
            @Override public Boolean visitEmptyStatement(EmptyStatement es)                                         { return UnitCompilerStatementCompiler.this.generatesCode2(es);   }
            @Override public Boolean visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { return UnitCompilerStatementCompiler.this.generatesCode2(lcds); }
            @Override public Boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { return UnitCompilerStatementCompiler.this.generatesCode2(aci);  }
            @Override public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { return UnitCompilerStatementCompiler.this.generatesCode2(sci);  }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(BlockStatement bs) { return true; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(AssertStatement as) { return true; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(EmptyStatement es) { return false; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(LocalClassDeclarationStatement lcds) { return false; }

    private boolean
    generatesCode2(Initializer i) throws CompileException { return this.generatesCode(i.block); }

    boolean
    generatesCode2(List<BlockStatement> l) throws CompileException {
        for (BlockStatement bs : l) if (this.generatesCode(bs)) return true;
        return false;
    }

    private boolean
    generatesCode2(Block b) throws CompileException { return this.generatesCode2(b.statements); }

    private boolean
    generatesCode2(FieldDeclaration fd) throws CompileException {
        // Code is only generated if at least one of the declared variables has a non-constant-final initializer.
        for (VariableDeclarator vd : fd.variableDeclarators) {
            if (this.context.getNonConstantFinalInitializer(fd, vd) != null) return true;
        }
        return false;
    }

    // ------------ BlockStatement.leave() -------------

    /**
     * Cleans up the statement context. This is currently relevant for "{@code try ... catch ... finally}" statements
     * (execute {@code finally} clause) and {@code synchronized} statements (monitorexit).
     * <p>
     *   Statements like {@code return}, {@code break}, {@code continue} must call this method for all the statements
     *   they terminate.
     * </p>
     */
    private void
    leave(BlockStatement bs) throws CompileException {
        BlockStatementVisitor<Void, CompileException> bsv = new BlockStatementVisitor<Void, CompileException>() {
            @Override @Nullable public Void visitInitializer(Initializer i)                                                { UnitCompilerStatementCompiler.this.leave2(i);    return null; }
            @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                     { UnitCompilerStatementCompiler.this.leave2(fd);   return null; }
            @Override @Nullable public Void visitLabeledStatement(LabeledStatement ls)                                     { UnitCompilerStatementCompiler.this.leave2(ls);   return null; }
            @Override @Nullable public Void visitBlock(Block b)                                                            { UnitCompilerStatementCompiler.this.leave2(b);    return null; }
            @Override @Nullable public Void visitExpressionStatement(ExpressionStatement es)                               { UnitCompilerStatementCompiler.this.leave2(es);   return null; }
            @Override @Nullable public Void visitIfStatement(IfStatement is)                                               { UnitCompilerStatementCompiler.this.leave2(is);   return null; }
            @Override @Nullable public Void visitForStatement(ForStatement fs)                                             { UnitCompilerStatementCompiler.this.leave2(fs);   return null; }
            @Override @Nullable public Void visitForEachStatement(ForEachStatement fes)                                    { UnitCompilerStatementCompiler.this.leave2(fes);  return null; }
            @Override @Nullable public Void visitWhileStatement(WhileStatement ws)                                         { UnitCompilerStatementCompiler.this.leave2(ws);   return null; }
            @Override @Nullable public Void visitTryStatement(TryStatement ts) throws CompileException                     { UnitCompilerStatementCompiler.this.leave2(ts);   return null; }
            @Override @Nullable public Void visitSwitchStatement(SwitchStatement ss)                                       { UnitCompilerStatementCompiler.this.leave2(ss);   return null; }
            @Override @Nullable public Void visitSynchronizedStatement(SynchronizedStatement ss)                           { UnitCompilerStatementCompiler.this.leave2(ss);   return null; }
            @Override @Nullable public Void visitDoStatement(DoStatement ds)                                               { UnitCompilerStatementCompiler.this.leave2(ds);   return null; }
            @Override @Nullable public Void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { UnitCompilerStatementCompiler.this.leave2(lvds); return null; }
            @Override @Nullable public Void visitReturnStatement(ReturnStatement rs)                                       { UnitCompilerStatementCompiler.this.leave2(rs);   return null; }
            @Override @Nullable public Void visitThrowStatement(ThrowStatement ts)                                         { UnitCompilerStatementCompiler.this.leave2(ts);   return null; }
            @Override @Nullable public Void visitBreakStatement(BreakStatement bs)                                         { UnitCompilerStatementCompiler.this.leave2(bs);   return null; }
            @Override @Nullable public Void visitContinueStatement(ContinueStatement cs)                                   { UnitCompilerStatementCompiler.this.leave2(cs);   return null; }
            @Override @Nullable public Void visitAssertStatement(AssertStatement as)                                       { UnitCompilerStatementCompiler.this.leave2(as);   return null; }
            @Override @Nullable public Void visitEmptyStatement(EmptyStatement es)                                         { UnitCompilerStatementCompiler.this.leave2(es);   return null; }
            @Override @Nullable public Void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { UnitCompilerStatementCompiler.this.leave2(lcds); return null; }
            @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { UnitCompilerStatementCompiler.this.leave2(aci);  return null; }
            @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { UnitCompilerStatementCompiler.this.leave2(sci);  return null; }
        };
        bs.accept(bsv);
    }

    private void
    leave2(BlockStatement bs) {}

    private void
    leave2(SynchronizedStatement ss) {
        for (ProtectedRegion region : this.protectedRegions) {
            if (region.owner == ss && region.codeContext == this.context.getCodeContext()) this.excludeCleanup(region);
        }
        this.bytecodeEmitter.load(ss, this.iClassLoader.TYPE_java_lang_Object, ss.monitorLvIndex);
        this.bytecodeEmitter.monitorexit(ss);
    }

    private void
    leave2(TryStatement ts) throws CompileException {
        // Inline resource cleanup from innermost to outermost, before the user's finally.
        for (int i = this.resourceCleanups.size() - 1; i >= 0; --i) {
            ResourceCleanup cleanup = this.resourceCleanups.get(i);
            if (cleanup.region.owner == ts && cleanup.region.codeContext == this.context.getCodeContext()) {
                this.excludeCleanup(cleanup.region);
                this.compile(cleanup.body);
                if (this.context.getCodeContext().currentInserter().getStackMap() == null) return;
            }
        }
        Block f = ts.finallY;
        if (f == null) return;
        for (ProtectedRegion region : this.protectedRegions) {
            if (region.owner == ts && !region.resource && region.codeContext == this.context.getCodeContext()) {
                this.excludeCleanup(region);
            }
        }
        this.context.getCodeContext().saveLocalVariables();
        try {
            this.compile(f);
        } finally {
            this.context.getCodeContext().restoreLocalVariables();
        }
    }

    /**
     * Statements that jump out of blocks ({@code return}, {@code break}, {@code continue}) must call this method to
     * make sure that the {@code finally} clauses of all {@code try ... catch} and {@code synchronized} statements are
     * executed.
     */
    private void
    leaveStatements(Scope from, Scope to) throws CompileException {
        List<CleanupExclusion> saved = this.abruptCleanupExclusions;
        List<CleanupExclusion> exclusions = new ArrayList<>();
        this.abruptCleanupExclusions = exclusions;
        try {
            Scope prev = null;
            for (Scope s = from; s != to; s = s.getEnclosingScope()) {
                if (s instanceof BlockStatement && !(s instanceof TryStatement && ((TryStatement) s).finallY == prev)) {
                    this.leave((BlockStatement) s);
                    if (this.context.getCodeContext().currentInserter().getStackMap() == null) break;
                }
                prev = s;
            }
        } finally {
            CodeContext.Offset end = this.context.getCodeContext().newOffset();
            for (CleanupExclusion exclusion : exclusions) exclusion.end = end;
            this.abruptCleanupExclusions = saved;
        }
    }
/* UNUSED
    private void writeConstantStringInfo(String value) {
        this.getCodeContext().writeShort(-1, this.bytecodeEmitter.addConstantStringInfo(value));
    }
    private void
    writeConstantLongInfo(long value) {
        this.getCodeContext().writeShort(this.bytecodeEmitter.addConstantLongInfo(value));
    }
    private void writeConstantFloatInfo(float value) {
        this.getCodeContext().writeShort(-1, this.bytecodeEmitter.addConstantFloatInfo(value));
    }
    private void
    writeConstantDoubleInfo(double value) {
        this.getCodeContext().writeShort(this.bytecodeEmitter.addConstantDoubleInfo(value));
    }
*/

    private CodeContext.Offset
    getWhereToBreak(BreakableStatement bs) {

        Offset wtb = bs.whereToBreak;
        if (wtb != null) {

            StackMap saved = this.context.getCodeContext().currentInserter().getStackMap();
            wtb.setStackMap();
            this.context.getCodeContext().currentInserter().setStackMap(saved);

            return wtb;
        }

        wtb = this.context.getCodeContext().new BasicBlock();
        wtb.setStackMap(this.context.getCodeContext().currentInserter().getStackMap());
        return (bs.whereToBreak = wtb);
    }

    /**
     * Computes and fills in the 'local variable map' for the given <var>blockStatement</var>.
     */
    Map<String, LocalVariable>
    buildLocalVariableMap(BlockStatement blockStatement, final Map<String, LocalVariable> localVars)
    throws CompileException {

        Map<String, LocalVariable> result = (Map<String, LocalVariable>) blockStatement.accept(
            new BlockStatementVisitor<Map<String, LocalVariable>, CompileException>() {

                // Basic statements that use the default handlers.
                @Override public Map<String, LocalVariable> visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)  { UnitCompilerStatementCompiler.buildLocalVariableMap(aci, localVars);  return localVars; }
                @Override public Map<String, LocalVariable> visitBreakStatement(BreakStatement bs)                                   { UnitCompilerStatementCompiler.buildLocalVariableMap(bs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitContinueStatement(ContinueStatement cs)                             { UnitCompilerStatementCompiler.buildLocalVariableMap(cs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitAssertStatement(AssertStatement as)                                 { UnitCompilerStatementCompiler.buildLocalVariableMap(as, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitEmptyStatement(EmptyStatement es)                                   { UnitCompilerStatementCompiler.buildLocalVariableMap(es, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitExpressionStatement(ExpressionStatement es)                         { UnitCompilerStatementCompiler.buildLocalVariableMap(es, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitFieldDeclaration(FieldDeclaration fd)                               { UnitCompilerStatementCompiler.buildLocalVariableMap(fd, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitReturnStatement(ReturnStatement rs)                                 { UnitCompilerStatementCompiler.buildLocalVariableMap(rs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitSuperConstructorInvocation(SuperConstructorInvocation sci)          { UnitCompilerStatementCompiler.buildLocalVariableMap(sci, localVars);  return localVars; }
                @Override public Map<String, LocalVariable> visitThrowStatement(ThrowStatement ts)                                   { UnitCompilerStatementCompiler.buildLocalVariableMap(ts, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) { UnitCompilerStatementCompiler.buildLocalVariableMap(lcds, localVars); return localVars; }

                // More complicated statements with specialized handlers, but don't add new variables in this scope.
                @Override public Map<String, LocalVariable> visitBlock(Block b)                                  throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(b,   localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitDoStatement(DoStatement ds)                     throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(ds,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitForStatement(ForStatement fs)                   throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(fs,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitForEachStatement(ForEachStatement fes)          throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(fes, localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitIfStatement(IfStatement is)                     throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(is,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitInitializer(Initializer i)                      throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(i,   localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitSwitchStatement(SwitchStatement ss)             throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(ss,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitSynchronizedStatement(SynchronizedStatement ss) throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(ss,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitTryStatement(TryStatement ts)                   throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(ts,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitWhileStatement(WhileStatement ws)               throws CompileException { UnitCompilerStatementCompiler.this.buildLocalVariableMap(ws,  localVars); return localVars; }

                // More complicated statements with specialized handlers, that can add variables in this scope.
                @Override public Map<String, LocalVariable> visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return UnitCompilerStatementCompiler.this.buildLocalVariableMap(ls,   localVars); }
                @Override public Map<String, LocalVariable> visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return UnitCompilerStatementCompiler.this.buildLocalVariableMap(lvds, localVars); }
            }
        );

        assert result != null;
        return result;
    }

    private static Map<String, LocalVariable>
    buildLocalVariableMap(Statement s, final Map<String, LocalVariable> localVars) {
        return (s.localVariables = localVars);
    }

    static Map<String, LocalVariable>
    buildLocalVariableMap(ConstructorInvocation ci, final Map<String, LocalVariable> localVars) {
        return (ci.localVariables = localVars);
    }

    void
    buildLocalVariableMap(Block block, Map<String, LocalVariable> localVars) throws CompileException {
        block.localVariables = localVars;
        for (BlockStatement bs : block.statements) localVars = this.buildLocalVariableMap(bs, localVars);
    }

    private void
    buildLocalVariableMap(DoStatement ds, final Map<String, LocalVariable> localVars) throws CompileException {
        ds.localVariables = localVars;
        this.buildLocalVariableMap(ds.body, localVars);
    }

    private void
    buildLocalVariableMap(ForStatement fs, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> inner = localVars;
        if (fs.init != null) {
            inner = this.buildLocalVariableMap(fs.init, localVars);
        }
        fs.localVariables = inner;
        this.buildLocalVariableMap(fs.body, inner);
    }

    private void
    buildLocalVariableMap(ForEachStatement fes, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> vars = new HashMap<>();
        vars.putAll(localVars);
        LocalVariable elementLv = this.context.getLocalVariable(fes.currentElement, false);
        vars.put(fes.currentElement.name, elementLv);
        // The iteration variable is in scope only in the body, not in the iterable expression.
        fes.localVariables = localVars;
        this.buildLocalVariableMap(fes.body, vars);
    }

    private void
    buildLocalVariableMap(IfStatement is, final Map<String, LocalVariable> localVars) throws CompileException {
        is.localVariables = localVars;
        this.buildLocalVariableMap(is.thenStatement, localVars);
        if (is.elseStatement != null) {
            this.buildLocalVariableMap(is.elseStatement, localVars);
        }
    }

    private void
    buildLocalVariableMap(Initializer i, final Map<String, LocalVariable> localVars) throws CompileException {
        this.buildLocalVariableMap(i.block, localVars);
    }

    private void
    buildLocalVariableMap(SwitchStatement ss, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ss.localVariables = localVars;
        Map<String, LocalVariable> vars = localVars;
        for (SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (BlockStatement bs : sbsg.blockStatements) vars = this.buildLocalVariableMap(bs, vars);
        }
    }

    private void
    buildLocalVariableMap(SynchronizedStatement ss, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ss.localVariables = localVars;
        this.buildLocalVariableMap(ss.body, localVars);
    }

    private void
    buildLocalVariableMap(TryStatement ts, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ts.localVariables = localVars;
        Map<String, LocalVariable> resourceVars = new HashMap<>(localVars);
        for (TryStatement.Resource resource : ts.resources) {
            if (resource instanceof LocalVariableDeclaratorResource) {
                LocalVariableDeclaratorResource declaration = (LocalVariableDeclaratorResource) resource;
                VariableDeclarator vd = declaration.variableDeclarator;
                LocalVariable lv = vd.localVariable;
                if (lv == null) vd.localVariable = lv = new LocalVariable(true, this.context.getType(declaration.type));
                if (resourceVars.put(vd.name, lv) != null) {
                    this.context.compileError("Redefinition of resource variable \"" + vd.name + "\"", vd.getLocation());
                }
            }
        }
        this.buildLocalVariableMap(ts.body, resourceVars);
        for (CatchClause cc : ts.catchClauses) this.context.buildLocalVariableMap(cc, localVars);
        if (ts.finallY != null) {
            this.buildLocalVariableMap(ts.finallY, localVars);
        }
    }

    private void
    buildLocalVariableMap(WhileStatement ws, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ws.localVariables = localVars;
        this.buildLocalVariableMap(ws.body, localVars);
    }

    private Map<String, LocalVariable>
    buildLocalVariableMap(LabeledStatement ls, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ls.localVariables = localVars;
        return this.buildLocalVariableMap((BlockStatement) ls.body, localVars);
    }

    private Map<String, LocalVariable>
    buildLocalVariableMap(LocalVariableDeclarationStatement lvds, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> newVars = new HashMap<>();
        newVars.putAll(localVars);
        for (VariableDeclarator vd : lvds.variableDeclarators) {
            LocalVariable      lv = this.context.getLocalVariable(lvds, vd);
            if (newVars.put(vd.name, lv) != null) {
                this.context.compileError("Redefinition of local variable \"" + vd.name + "\" ", vd.getLocation());
            }
        }
        lvds.localVariables = newVars;
        return newVars;
    }

    /**
     * Definite unassignment for final locals. Keep this separate from bytecode emission: fake compilation,
     * finally inlining and mutually exclusive branches must not count as additional source assignments.
     */
    private final class FinalLocalAnalyzer {
        private final Set<LocalVariable> assigned = new HashSet<>();
        private final List<FinalExit> exits = new ArrayList<>();
        private final List<Set<LocalVariable>> protectedWrites = new ArrayList<>();

        private final class FinalExit {
            final BlockStatement target;
            final Set<LocalVariable> breaks = new HashSet<>(), continues = new HashSet<>();
            boolean hasBreak, hasContinue;
            int breakCount, continueCount;
            FinalExit(BlockStatement target) { this.target = target; }
        }

        void analyze(FunctionDeclarator fd) throws CompileException {
            for (FormalParameter fp : fd.formalParameters.parameters) this.assigned.add(UnitCompilerStatementCompiler.this.context.getLocalVariable(fp));
            if (fd instanceof ConstructorDeclarator) {
                ConstructorInvocation invocation = ((ConstructorDeclarator) fd).constructorInvocation;
                if (invocation != null) for (Rvalue argument : invocation.arguments) this.expression(argument);
            }
            if (fd.statements != null) this.statements(fd.statements);
        }

        boolean statements(List<? extends BlockStatement> statements) throws CompileException {
            boolean normal = true;
            for (BlockStatement statement : statements) {
                if (!normal) break; // Ordinary compilation diagnoses unreachable statements.
                normal = this.statement(statement);
            }
            return normal;
        }

        void restore(Set<LocalVariable> state) { this.assigned.clear(); this.assigned.addAll(state); }

        boolean statement(BlockStatement statement) throws CompileException {
            if (statement instanceof Block) return this.statements(((Block) statement).statements);
            if (statement instanceof LocalVariableDeclarationStatement) {
                LocalVariableDeclarationStatement declaration = (LocalVariableDeclarationStatement) statement;
                for (VariableDeclarator vd : declaration.variableDeclarators) {
                    LocalVariable lv = UnitCompilerStatementCompiler.this.context.getLocalVariable(declaration, vd);
                    this.assigned.remove(lv); // A new execution of a declaration creates a new variable.
                    this.initializer(vd.initializer);
                    if (vd.initializer != null) {
                        if (lv.finaL && this.assigned.contains(lv)) throw UnitCompilerStatementCompiler.this.context.compileException(vd, "Cannot assign a value to final local variable twice in its initializer");
                        if (this.assigned.contains(lv)) lv.effectivelyFinal = false;
                        this.assigned.add(lv);
                    }
                }
            } else if (statement instanceof ExpressionStatement) {
                this.expression(((ExpressionStatement) statement).rvalue);
            } else if (statement instanceof AssertStatement) {
                AssertStatement assertion = (AssertStatement) statement;
                this.expression(assertion.expression1);
                Set<LocalVariable> afterCondition = new HashSet<>(this.assigned);
                this.expression(assertion.expression2);
                this.restore(afterCondition); // The detail expression is followed by an abrupt AssertionError.

            } else if (statement instanceof IfStatement) {
                IfStatement branch = (IfStatement) statement;
                this.expression(branch.condition);
                Set<LocalVariable> before = new HashSet<>(this.assigned);
                boolean thenNormal = this.statement(branch.thenStatement);
                Set<LocalVariable> afterThen = new HashSet<>(this.assigned);
                this.restore(before);
                boolean elseNormal = branch.elseStatement == null || this.statement(branch.elseStatement);
                if (!elseNormal) this.assigned.clear();
                if (thenNormal) this.assigned.addAll(afterThen);
                return thenNormal || elseNormal;
            } else if (statement instanceof ReturnStatement) {
                this.expression(((ReturnStatement) statement).returnValue);
                return false;
            } else if (statement instanceof ThrowStatement) {
                this.expression(((ThrowStatement) statement).expression);
                return false;
            } else if (statement instanceof SynchronizedStatement) {
                SynchronizedStatement sync = (SynchronizedStatement) statement;
                this.expression(sync.expression);
                return this.statement(sync.body);
            } else if (statement instanceof LabeledStatement) {
                FinalExit exit = new FinalExit(statement);
                this.exits.add(exit);
                boolean normal = this.statement(((LabeledStatement) statement).body);
                this.exits.remove(this.exits.size() - 1);
                if (!normal) this.assigned.clear();
                this.assigned.addAll(exit.breaks);
                return normal || exit.hasBreak;
            } else if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {
                boolean continuing = statement instanceof ContinueStatement;
                String label = continuing ? ((ContinueStatement) statement).label : ((BreakStatement) statement).label;
                for (int i = this.exits.size() - 1; i >= 0; i--) {
                    FinalExit exit = this.exits.get(i);
                    if (label == null ? exit.target instanceof ContinuableStatement || !continuing && exit.target instanceof SwitchStatement
                        : exit.target instanceof LabeledStatement && label.equals(((LabeledStatement) exit.target).label)) {
                        if (continuing && exit.target instanceof LabeledStatement) {
                            Statement body = ((LabeledStatement) exit.target).body;
                            for (FinalExit loopExit : this.exits) if (loopExit.target == body) { exit = loopExit; break; }
                        }
                        if (continuing) { exit.continues.addAll(this.assigned); exit.hasContinue = true; exit.continueCount++; }
                        else { exit.breaks.addAll(this.assigned); exit.hasBreak = true; exit.breakCount++; }
                        break;
                    }
                }
                return false;
            } else if (statement instanceof ContinuableStatement) {
                ContinuableStatement loop = (ContinuableStatement) statement;
                Rvalue condition = null;
                Rvalue[] update = null;
                if (loop instanceof ForStatement) {
                    ForStatement fs = (ForStatement) loop;
                    if (fs.init != null) this.statement(fs.init);
                    condition = fs.condition; update = fs.update;
                } else if (loop instanceof WhileStatement) condition = ((WhileStatement) loop).condition;
                else if (loop instanceof DoStatement) condition = ((DoStatement) loop).condition;
                else if (loop instanceof ForEachStatement) {
                    ForEachStatement fes = (ForEachStatement) loop;
                    this.expression(fes.expression);
                    this.assigned.add(UnitCompilerStatementCompiler.this.context.getLocalVariable(fes.currentElement));
                }
                boolean postTest = loop instanceof DoStatement;
                if (!postTest) this.expression(condition);
                Set<LocalVariable> before = new HashSet<>(this.assigned);
                FinalExit exit = new FinalExit(statement);
                this.exits.add(exit);
                boolean normal = this.statement(loop.body);
                if (!normal) this.assigned.clear();
                this.assigned.addAll(exit.continues);
                if (normal || exit.hasContinue) {
                    if (update != null) for (Rvalue rv : update) this.expression(rv);
                    this.expression(condition);
                    if (condition == null || !Boolean.FALSE.equals(UnitCompilerStatementCompiler.this.constantEvaluator.getConstantValue(condition))) {
                        // One additional traversal reaches the fixed point for may-assigned variables and
                        // diagnoses a final assigned again along a loop back edge.
                        this.statement(loop.body);
                        if (update != null) for (Rvalue rv : update) this.expression(rv);
                    }
                }
                this.exits.remove(this.exits.size() - 1);
                if (!postTest) this.assigned.addAll(before);
                this.assigned.addAll(exit.breaks);
                return condition != null && !Boolean.TRUE.equals(UnitCompilerStatementCompiler.this.constantEvaluator.getConstantValue(condition)) || exit.hasBreak
                    || loop instanceof ForEachStatement;
            } else if (statement instanceof SwitchStatement) {
                SwitchStatement sw = (SwitchStatement) statement;
                this.expression(sw.condition);
                Set<LocalVariable> before = new HashSet<>(this.assigned), falling = new HashSet<>();
                FinalExit exit = new FinalExit(statement); this.exits.add(exit);
                boolean normal = false, hasDefault = false;
                for (SwitchStatement.SwitchBlockStatementGroup group : sw.sbsgs) {
                    this.restore(before); this.assigned.addAll(falling);
                    normal = this.statements(group.blockStatements);
                    falling.clear(); if (normal) falling.addAll(this.assigned);
                    hasDefault |= group.hasDefaultLabel;
                }
                this.exits.remove(this.exits.size() - 1);
                this.restore(falling); this.assigned.addAll(exit.breaks);
                if (!hasDefault) this.assigned.addAll(before);
                return normal || exit.hasBreak || !hasDefault;
            } else if (statement instanceof TryStatement) {
                TryStatement tr = (TryStatement) statement;
                for (TryStatement.Resource resource : tr.resources) {
                    if (resource instanceof TryStatement.LocalVariableDeclaratorResource) {
                        VariableDeclarator vd = ((LocalVariableDeclaratorResource) resource).variableDeclarator;
                        this.initializer(vd.initializer);
                        this.assigned.add(vd.localVariable);
                    }
                }
                int[] breaksBefore = new int[this.exits.size()], continuesBefore = new int[this.exits.size()];
                for (int i = 0; i < this.exits.size(); i++) {
                    breaksBefore[i] = this.exits.get(i).breakCount;
                    continuesBefore[i] = this.exits.get(i).continueCount;
                }
                Set<LocalVariable> before = new HashSet<>(this.assigned), writes = new HashSet<>();
                this.protectedWrites.add(writes);
                boolean normal = this.statement(tr.body);
                Set<LocalVariable> after = normal ? new HashSet<>(this.assigned) : new HashSet<LocalVariable>();
                Set<LocalVariable> catchEntry = new HashSet<>(writes); catchEntry.addAll(before);
                for (CatchClause cc : tr.catchClauses) {
                    this.restore(catchEntry); this.assigned.add(UnitCompilerStatementCompiler.this.context.getLocalVariable(cc.catchParameter));
                    boolean catchNormal = this.statement(cc.body);
                    if (catchNormal) after.addAll(this.assigned);
                    normal |= catchNormal;
                }
                this.protectedWrites.remove(this.protectedWrites.size() - 1);
                after.addAll(writes); // JLS 16: DU after try also accounts for abrupt exits from its protected blocks.
                this.restore(after);
                if (tr.finallY != null) {
                    this.assigned.addAll(before); this.assigned.addAll(writes);
                    Set<LocalVariable> finallyWrites = new HashSet<>();
                    this.protectedWrites.add(finallyWrites);
                    normal &= this.statement(tr.finallY);
                    this.protectedWrites.remove(this.protectedWrites.size() - 1);
                    this.restore(after); this.assigned.addAll(finallyWrites);
                    for (int i = 0; i < this.exits.size(); i++) {
                        FinalExit exit = this.exits.get(i);
                        if (exit.breakCount > breaksBefore[i]) exit.breaks.addAll(finallyWrites);
                        if (exit.continueCount > continuesBefore[i]) exit.continues.addAll(finallyWrites);
                    }
                }
                return normal;
            }
            return true;
        }

        void initializer(@Nullable ArrayInitializerOrRvalue value) throws CompileException {
            if (value instanceof Rvalue) this.expression((Rvalue) value);
            else if (value instanceof ArrayInitializer) {
                for (ArrayInitializerOrRvalue element : ((ArrayInitializer) value).values) this.initializer(element);
            }
        }

        void expression(@Nullable Rvalue value) throws CompileException {
            if (value == null) return;
            new org.codehaus.janino.util.AbstractTraverser<CompileException>() {
                @Override public void traverseBinaryOperation(Java.BinaryOperation operation) throws CompileException {
                    this.traverseBinaryOperationIteratively(operation);
                }

                @Override public void traverseLambdaExpression(LambdaExpression le) {} // Checked in its own generated method.
                @Override public void traverseMethodReference(MethodReference mr) {}
                @Override public void traverseAnonymousClassDeclaration(AnonymousClassDeclaration acd) {}
                @Override public void traverseAssignment(Assignment assignment) throws CompileException {
                    FinalLocalAnalyzer.this.expression(assignment.lhs);
                    FinalLocalAnalyzer.this.expression(assignment.rhs);
                    Atom lhs = assignment.lhs;
                    while (lhs instanceof ParenthesizedExpression) lhs = ((ParenthesizedExpression) lhs).value;
                    if (lhs instanceof AmbiguousName) lhs = UnitCompilerStatementCompiler.this.context.reclassify((AmbiguousName) lhs);
                    if (lhs instanceof LocalVariableAccess) {
                        LocalVariable lv = ((LocalVariableAccess) lhs).localVariable;
                        if (lv.finaL && (assigned.contains(lv) || !"=".equals(assignment.operator))) {
                            throw UnitCompilerStatementCompiler.this.context.compileException(assignment, "Cannot assign a value to final local variable");
                        }
                        if (assigned.contains(lv) || !"=".equals(assignment.operator)) lv.effectivelyFinal = false;
                        assigned.add(lv);
                        for (Set<LocalVariable> writes : protectedWrites) writes.add(lv);
                    }
                }
                @Override public void traverseCrement(Crement crement) throws CompileException {
                    FinalLocalAnalyzer.this.expression(crement.operand);
                    Atom operand = crement.operand;
                    while (operand instanceof ParenthesizedExpression) operand = ((ParenthesizedExpression) operand).value;
                    if (operand instanceof AmbiguousName) operand = UnitCompilerStatementCompiler.this.context.reclassify((AmbiguousName) operand);
                    if (operand instanceof LocalVariableAccess) {
                        LocalVariable lv = ((LocalVariableAccess) operand).localVariable;
                        if (lv.finaL) throw UnitCompilerStatementCompiler.this.context.compileException(crement, "Cannot modify final local variable");
                        lv.effectivelyFinal = false;
                        assigned.add(lv);
                        for (Set<LocalVariable> writes : protectedWrites) writes.add(lv);
                    }
                }
                @Override public void traverseConditionalExpression(ConditionalExpression conditional) throws CompileException {
                    FinalLocalAnalyzer.this.expression(conditional.lhs);
                    Set<LocalVariable> before = new HashSet<>(assigned);
                    FinalLocalAnalyzer.this.expression(conditional.mhs);
                    Set<LocalVariable> after = new HashSet<>(assigned);
                    restore(before);
                    FinalLocalAnalyzer.this.expression(conditional.rhs);
                    assigned.addAll(after);
                }
            }.dispatchRvalue(value);
        }
    }

    void analyzeFinalLocals(Block block) throws CompileException {
        new FinalLocalAnalyzer().statement(block);
    }

    void analyzeFinalLocals(FunctionDeclarator function) throws CompileException {
        new FinalLocalAnalyzer().analyze(function);
    }
}
