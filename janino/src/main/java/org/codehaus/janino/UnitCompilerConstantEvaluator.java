
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.Numbers;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.Literal;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.TextBlock;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Visitor.ArrayInitializerOrRvalueVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;

/** Evaluates and caches compile-time constants, including known local constants. */
final class UnitCompilerConstantEvaluator {

    /** Type/name queries and source validation needed to evaluate a constant. */
    interface Context {
        void
        fakeCompile(Rvalue rv) throws CompileException;
        boolean
        mayHaveSideEffects(ArrayInitializerOrRvalue arrayInitializerOrRvalue);
        IType
        getType(Type t) throws CompileException;
        IType
        getType2(ConditionalExpression ce) throws CompileException;
        Rvalue
        toRvalueOrCompileException(Atom a) throws CompileException;
        Atom
        reclassify(AmbiguousName an) throws CompileException;
        CompileException
        compileException(Locatable locatable, String message);
    }

    private final Context context;
    private final IClassLoader iClassLoader;

    UnitCompilerConstantEvaluator(Context context, IClassLoader iClassLoader) {
        this.context = context;
        this.iClassLoader = iClassLoader;
    }


    private final Map<LocalVariable, Object> constantLocals = new java.util.IdentityHashMap<>();

    /**
     * Attempts to evaluate as a constant expression. The result is one of the following: {@link Boolean}, {@link
     * Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link Character}, {@link
     * String}, {@code null} (representing the {@code null} literal.
     * <p>
     *   This method cannot be STATIC, because the constant value may refer to a constant declaration in this
     *   compilation unit.
     * </p>
     *
     * @return {@link UnitCompiler#NOT_CONSTANT} iff the rvalue is not a constant value
     */
    @Nullable final Object
    getConstantValue(ArrayInitializerOrRvalue rv) throws CompileException {

        return rv.accept(new ArrayInitializerOrRvalueVisitor<Object, CompileException>() {

            @Override @Nullable public Object
            visitArrayInitializer(ArrayInitializer ai) { return UnitCompiler.NOT_CONSTANT; }

            @Override @Nullable public Object
            visitRvalue(Rvalue rvalue) throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue(rvalue); }
        });
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
     * @return {@link UnitCompiler#NOT_CONSTANT} iff the rvalue is not a constant value
     */
    @Nullable final Object
    getConstantValue(Rvalue rv) throws CompileException {

        if (rv.constantValue != Rvalue.CONSTANT_VALUE_UNKNOWN) return rv.constantValue;

        return (rv.constantValue = rv.accept(new RvalueVisitor<Object, CompileException>() {

            @Override @Nullable public Object
            visitLvalue(Lvalue lv) throws CompileException {
                return lv.accept(new LvalueVisitor<Object, CompileException>() {
                    @Override @Nullable public Object visitAmbiguousName(AmbiguousName an)                     throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(an);    }
                    @Override @Nullable public Object visitArrayAccessExpression(ArrayAccessExpression aae)                            { return UnitCompilerConstantEvaluator.this.getConstantValue2(aae);   }
                    @Override @Nullable public Object visitFieldAccess(FieldAccess fa)                         throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(fa);    }
                    @Override @Nullable public Object visitFieldAccessExpression(FieldAccessExpression fae)                            { return UnitCompilerConstantEvaluator.this.getConstantValue2(fae);   }
                    @Override @Nullable public Object visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae)      { return UnitCompilerConstantEvaluator.this.getConstantValue2(scfae); }
                    @Override @Nullable public Object visitLocalVariableAccess(LocalVariableAccess lva)        throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(lva);   }
                    @Override @Nullable public Object visitParenthesizedExpression(ParenthesizedExpression pe) throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(pe);    }
                });
            }

            @Override @Nullable public Object visitArrayLength(ArrayLength al)                                             { return UnitCompilerConstantEvaluator.this.getConstantValue2(al);   }
            @Override @Nullable public Object visitAssignment(Assignment a)                                                { return UnitCompilerConstantEvaluator.this.getConstantValue2(a);    }
            @Override @Nullable public Object visitUnaryOperation(UnaryOperation uo)               throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(uo);   }
            @Override @Nullable public Object visitBinaryOperation(BinaryOperation bo)             throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(bo);   }
            @Override @Nullable public Object visitCast(Cast c)                                    throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(c);    }
            @Override @Nullable public Object visitClassLiteral(ClassLiteral cl)                                           { return UnitCompilerConstantEvaluator.this.getConstantValue2(cl);   }
            @Override @Nullable public Object visitConditionalExpression(ConditionalExpression ce) throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(ce);   }
            @Override @Nullable public Object visitCrement(Crement c)                                                      { return UnitCompilerConstantEvaluator.this.getConstantValue2(c);    }
            @Override @Nullable public Object visitInstanceof(Instanceof io)                                               { return UnitCompilerConstantEvaluator.this.getConstantValue2(io);   }
            @Override @Nullable public Object visitMethodInvocation(MethodInvocation mi)                                   { return UnitCompilerConstantEvaluator.this.getConstantValue2(mi);   }
            @Override @Nullable public Object visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)              { return UnitCompilerConstantEvaluator.this.getConstantValue2(smi);  }
            @Override @Nullable public Object visitIntegerLiteral(IntegerLiteral il)               throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(il);   }
            @Override @Nullable public Object visitFloatingPointLiteral(FloatingPointLiteral fpl)  throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(fpl);  }
            @Override @Nullable public Object visitBooleanLiteral(BooleanLiteral bl)                                       { return UnitCompilerConstantEvaluator.this.getConstantValue2(bl);   }
            @Override @Nullable public Object visitCharacterLiteral(CharacterLiteral cl)           throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(cl);   }
            @Override @Nullable public Object visitStringLiteral(StringLiteral sl)                 throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(sl);   }
            @Override @Nullable public Object visitTextBlock(TextBlock tb)                         throws CompileException { return UnitCompilerConstantEvaluator.this.getConstantValue2(tb);   }
            @Override @Nullable public Object visitNullLiteral(NullLiteral nl)                                             { return UnitCompilerConstantEvaluator.this.getConstantValue2(nl);   }
            @Override @Nullable public Object visitSimpleConstant(SimpleConstant sl)                                       { return UnitCompilerConstantEvaluator.this.getConstantValue2(sl);   }
            @Override @Nullable public Object visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)               { return UnitCompilerConstantEvaluator.this.getConstantValue2(naci); }
            @Override @Nullable public Object visitNewArray(NewArray na)                                                   { return UnitCompilerConstantEvaluator.this.getConstantValue2(na);   }
            @Override @Nullable public Object visitNewInitializedArray(NewInitializedArray nia)                            { return UnitCompilerConstantEvaluator.this.getConstantValue2(nia);  }
            @Override @Nullable public Object visitNewClassInstance(NewClassInstance nci)                                  { return UnitCompilerConstantEvaluator.this.getConstantValue2(nci);  }
            @Override @Nullable public Object visitParameterAccess(ParameterAccess pa)                                     { return UnitCompilerConstantEvaluator.this.getConstantValue2(pa);   }
            @Override @Nullable public Object visitQualifiedThisReference(QualifiedThisReference qtr)                      { return UnitCompilerConstantEvaluator.this.getConstantValue2(qtr);  }
            @Override @Nullable public Object visitThisReference(ThisReference tr)                                         { return UnitCompilerConstantEvaluator.this.getConstantValue2(tr);   }
            @Override @Nullable public Object visitLambdaExpression(LambdaExpression le)                                   { return UnitCompilerConstantEvaluator.this.getConstantValue2(le);   }
            @Override @Nullable public Object visitMethodReference(MethodReference mr)                                     { return UnitCompilerConstantEvaluator.this.getConstantValue2(mr);   }
            @Override @Nullable public Object visitInstanceCreationReference(ClassInstanceCreationReference cicr)          { return UnitCompilerConstantEvaluator.this.getConstantValue2(cicr); }
            @Override @Nullable public Object visitArrayCreationReference(ArrayCreationReference acr)                      { return UnitCompilerConstantEvaluator.this.getConstantValue2(acr);  }
        }));
    }

    @SuppressWarnings("static-method")
    @Nullable Object
    getConstantValue2(Rvalue rv) { return UnitCompiler.NOT_CONSTANT; }

    @Nullable private Object
    getConstantValue2(AmbiguousName an) throws CompileException {
        return this.getConstantValue(this.context.toRvalueOrCompileException(this.context.reclassify(an)));
    }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(FieldAccess fa) throws CompileException {
        return fa.field.getConstantValue();
    }

    @Nullable Object
    getConstantValue2(UnaryOperation uo) throws CompileException {
        if (uo.operator == "+") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.getConstantValue(uo.operand);
        }
        if (uo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            // Handle the super special cases "-2147483648" and "-9223372036854775808L" (JLS9 3.10.1).
            if (uo.operand instanceof IntegerLiteral) {
                String v = ((Literal) uo.operand).value;

                if (UnitCompilerConstantEvaluator.TWO_E_31_INTEGER.matcher(v).matches()) return Integer.valueOf(Integer.MIN_VALUE);
                if (UnitCompilerConstantEvaluator.TWO_E_63_LONG.matcher(v).matches())    return Long.valueOf(Long.MIN_VALUE);
            }

            Object cv = this.getConstantValue(uo.operand);

            if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

            // SUPPRESS CHECKSTYLE DOT__SELECTOR|L_PAREN__METH_INVOCATION:6
            if (cv instanceof Byte)    return Byte   .valueOf((byte)  -((Byte)    cv));
            if (cv instanceof Short)   return Short  .valueOf((short) -((Short)   cv));
            if (cv instanceof Integer) return Integer.valueOf(        -((Integer) cv));
            if (cv instanceof Long)    return Long   .valueOf(        -((Long)    cv));
            if (cv instanceof Float)   return Float  .valueOf(        -((Float)   cv));
            if (cv instanceof Double)  return Double .valueOf(        -((Double)  cv));

            return UnitCompiler.NOT_CONSTANT;
        }

        if (uo.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object cv = this.getConstantValue(uo.operand);
            return (
                cv == Boolean.TRUE  ? Boolean.FALSE :
                cv == Boolean.FALSE ? Boolean.TRUE  :
                UnitCompiler.NOT_CONSTANT
            );
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    /**
     * 2147483648 is the special value that can <em>not</em> be stored in an INT, but <em>its negated value</em>
     * (-2147483648) can.
     */
    private static final Pattern
    TWO_E_31_INTEGER = Pattern.compile("2_*1_*4_*7_*4_*8_*3_*6_*4_*8");

    /**
     * 9223372036854775808 is the special value that can <em>not</em> be stored in a LONG, but <em>its negated
     * value</em> (-9223372036854775808) can.
     */
    private static final Pattern
    TWO_E_63_LONG = Pattern.compile("9_*2_*2_*3_*3_*7_*2_*0_*3_*6_*8_*5_*4_*7_*7_*5_*8_*0_*8[lL]");

    @Nullable private Object
    getConstantValue2(ConditionalExpression ce) throws CompileException {

        Object lhsCv = this.getConstantValue(ce.lhs);
        if (!(lhsCv instanceof Boolean)) return UnitCompiler.NOT_CONSTANT;

        IType ceType = this.context.getType2(ce);

        if (!UnitCompiler.isPrimitive(ceType) && ceType != this.iClassLoader.TYPE_java_lang_String) return UnitCompiler.NOT_CONSTANT;

        if (((Boolean) lhsCv).booleanValue()) {
            this.context.fakeCompile(ce.rhs);
            return this.getConstantValue(ce.mhs);
        } else {
            this.context.fakeCompile(ce.mhs);
            return this.getConstantValue(ce.rhs);
        }
    }

    @Nullable private Object
    getConstantValue2(BinaryOperation bo) throws CompileException {

        if ("<<".equals(bo.operator) || ">>".equals(bo.operator) || ">>>".equals(bo.operator)) {
            Object lhs = this.getConstantValue(bo.lhs), rhs = this.getConstantValue(bo.rhs);
            if (lhs instanceof Character) lhs = Integer.valueOf(((Character) lhs).charValue());
            if (rhs instanceof Character) rhs = Integer.valueOf(((Character) rhs).charValue());
            if (!(lhs instanceof Byte || lhs instanceof Short || lhs instanceof Integer || lhs instanceof Long)
                || !(rhs instanceof Byte || rhs instanceof Short || rhs instanceof Integer || rhs instanceof Long)) {
                return UnitCompiler.NOT_CONSTANT;
            }
            int distance = ((Number) rhs).intValue();
            if (lhs instanceof Long) {
                long value = ((Long) lhs).longValue();
                return Long.valueOf("<<".equals(bo.operator) ? value << distance
                    : ">>".equals(bo.operator) ? value >> distance : value >>> distance);
            }
            int value = ((Number) lhs).intValue();
            return Integer.valueOf("<<".equals(bo.operator) ? value << distance
                : ">>".equals(bo.operator) ? value >> distance : value >>> distance);
        }

        // "|", "^", "&", "*", "/", "%", "+", "-", "==", "!=".
        if (
            // SUPPRESS CHECKSTYLE StringLiteralEquality:10
            bo.operator == "|"
            || bo.operator == "^"
            || bo.operator == "&"
            || bo.operator == "*"
            || bo.operator == "/"
            || bo.operator == "%"
            || bo.operator == "+"
            || bo.operator == "-"
            || bo.operator == "=="
            || bo.operator == "!="
        ) {

            // Unroll the constant operands.
            List<Object> cvs = new ArrayList<>();
            for (Iterator<Rvalue> it = bo.unrollLeftAssociation(); it.hasNext();) {
                Object cv = this.getConstantValue((Rvalue) it.next());
                if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;
                cvs.add(cv);
            }

            // Compute the constant value of the unrolled binary operation.
            Iterator<Object> it  = cvs.iterator();
            Object           lhs = it.next();
            while (it.hasNext()) {
                if (lhs == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

                Object rhs = it.next();

                // String concatenation?
                // SUPPRESS CHECKSTYLE StringLiteralEquality
                if (bo.operator == "+" && (lhs instanceof String || rhs instanceof String)) {
                    StringBuilder sb = new StringBuilder(lhs.toString()).append(rhs);
                    while (it.hasNext()) sb.append(it.next().toString());
                    return sb.toString();
                }

                if (lhs instanceof Number && rhs instanceof Number) {
                    try {
                        if (lhs instanceof Double || rhs instanceof Double) {
                            double lhsD = ((Number) lhs).doubleValue();
                            double rhsD = ((Number) rhs).doubleValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:7
                                bo.operator == "*" ? Double.valueOf(lhsD * rhsD) :
                                bo.operator == "/" ? Double.valueOf(lhsD / rhsD) :
                                bo.operator == "%" ? Double.valueOf(lhsD % rhsD) :
                                bo.operator == "+" ? Double.valueOf(lhsD + rhsD) :
                                bo.operator == "-" ? Double.valueOf(lhsD - rhsD) :
                                bo.operator == "==" ? Boolean.valueOf(lhsD == rhsD) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsD != rhsD) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (lhs instanceof Float || rhs instanceof Float) {
                            float lhsF = ((Number) lhs).floatValue();
                            float rhsF = ((Number) rhs).floatValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:7
                                bo.operator == "*" ? Float.valueOf(lhsF * rhsF) :
                                bo.operator == "/" ? Float.valueOf(lhsF / rhsF) :
                                bo.operator == "%" ? Float.valueOf(lhsF % rhsF) :
                                bo.operator == "+" ? Float.valueOf(lhsF + rhsF) :
                                bo.operator == "-" ? Float.valueOf(lhsF - rhsF) :
                                bo.operator == "==" ? Boolean.valueOf(lhsF == rhsF) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsF != rhsF) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (lhs instanceof Long || rhs instanceof Long) {
                            long lhsL = ((Number) lhs).longValue();
                            long rhsL = ((Number) rhs).longValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:10
                                bo.operator == "|" ? Long.valueOf(lhsL | rhsL) :
                                bo.operator == "^" ? Long.valueOf(lhsL ^ rhsL) :
                                bo.operator == "&" ? Long.valueOf(lhsL & rhsL) :
                                bo.operator == "*" ? Long.valueOf(lhsL * rhsL) :
                                bo.operator == "/" ? Long.valueOf(lhsL / rhsL) :
                                bo.operator == "%" ? Long.valueOf(lhsL % rhsL) :
                                bo.operator == "+" ? Long.valueOf(lhsL + rhsL) :
                                bo.operator == "-" ? Long.valueOf(lhsL - rhsL) :
                                bo.operator == "==" ? Boolean.valueOf(lhsL == rhsL) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsL != rhsL) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (
                            lhs instanceof Integer || lhs instanceof Byte || lhs instanceof Short
                            || rhs instanceof Integer || lhs instanceof Byte || lhs instanceof Short
                        ) {
                            int lhsI = ((Number) lhs).intValue();
                            int rhsI = ((Number) rhs).intValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:10
                                bo.operator == "|" ? Integer.valueOf(lhsI | rhsI) :
                                bo.operator == "^" ? Integer.valueOf(lhsI ^ rhsI) :
                                bo.operator == "&" ? Integer.valueOf(lhsI & rhsI) :
                                bo.operator == "*" ? Integer.valueOf(lhsI * rhsI) :
                                bo.operator == "/" ? Integer.valueOf(lhsI / rhsI) :
                                bo.operator == "%" ? Integer.valueOf(lhsI % rhsI) :
                                bo.operator == "+" ? Integer.valueOf(lhsI + rhsI) :
                                bo.operator == "-" ? Integer.valueOf(lhsI - rhsI) :
                                bo.operator == "==" ? Boolean.valueOf(lhsI == rhsI) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsI != rhsI) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                    } catch (ArithmeticException ae) {

                        // Most likely a divide by zero or modulo by zero. Guess we can't make this expression into a
                        // constant.
                        return UnitCompiler.NOT_CONSTANT;
                    }
                    throw new IllegalStateException();
                }

                if (lhs instanceof Character && rhs instanceof Character) {
                    char lhsC = ((Character) lhs).charValue();
                    char rhsC = ((Character) rhs).charValue();
                    lhs = (
                        bo.operator == "==" ? Boolean.valueOf(lhsC == rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        bo.operator == "!=" ? Boolean.valueOf(lhsC != rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        UnitCompiler.NOT_CONSTANT
                    );
                    continue;
                }

                if (lhs == null || rhs == null) {
                    lhs = (
                        bo.operator == "==" ? Boolean.valueOf(lhs == rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        bo.operator == "!=" ? Boolean.valueOf(lhs != rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        UnitCompiler.NOT_CONSTANT
                    );
                    continue;
                }

                return UnitCompiler.NOT_CONSTANT;
            }
            return lhs;
        }

        // "&&" and "||" with constant LHS operand.
        if (bo.operator == "&&" || bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsValue = this.getConstantValue(bo.lhs);
            if (lhsValue instanceof Boolean) {
                boolean lhsBv = ((Boolean) lhsValue).booleanValue();
                return (
                    bo.operator == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
                    ? (lhsBv ? this.getConstantValue(bo.rhs) : Boolean.FALSE)
                    : (lhsBv ? Boolean.TRUE : this.getConstantValue(bo.rhs))
                );
            }
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    private Object
    getConstantValue2(Cast c) throws CompileException {
        Object cv = this.getConstantValue(c.value);
        if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

        if (cv instanceof Number) {
            IType tt = this.context.getType(c.targetType);
            if (tt == IClass.BYTE)   return Byte.valueOf(((Number) cv).byteValue());
            if (tt == IClass.SHORT)  return Short.valueOf(((Number) cv).shortValue());
            if (tt == IClass.INT)    return Integer.valueOf(((Number) cv).intValue());
            if (tt == IClass.LONG)   return Long.valueOf(((Number) cv).longValue());
            if (tt == IClass.FLOAT)  return Float.valueOf(((Number) cv).floatValue());
            if (tt == IClass.DOUBLE) return Double.valueOf(((Number) cv).doubleValue());
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    @Nullable private Object
    getConstantValue2(ParenthesizedExpression pe) throws CompileException {
        return this.getConstantValue(pe.value);
    }

    private @Nullable Object
    getConstantValue2(LocalVariableAccess lva) throws CompileException {

        Object constant = this.constantLocals.get(lva.localVariable);
        if (constant != null) return constant;

        // An optimization for the (very special case)
        //
        //    void method()         // Any method declarator
        //        ...
        //        boolean x = true; // Any local variable name allowed; also value "false"
        //        if (x) {          // The condition expression must be exactly like this
        //              ...
        //
        if (lva.getEnclosingScope() instanceof IfStatement) {
            IfStatement is = (IfStatement) lva.getEnclosingScope();

            if (is.condition instanceof AmbiguousName) {
                Atom ra = ((AmbiguousName) is.condition).reclassified;

                if (ra instanceof LocalVariableAccess) {

                    // It's an IF statement with a LocalVariableAccess expression.
                    LocalVariable lv = ((LocalVariableAccess) ra).localVariable;

                    // Get all statements of the immediately enclosing block, method or constructor.
                    List<? extends BlockStatement> ss = (
                        is.getEnclosingScope() instanceof FunctionDeclarator
                        ? ((FunctionDeclarator) is.getEnclosingScope()).statements
                        : is.getEnclosingScope() instanceof Block
                        ? ((Block) is.getEnclosingScope()).statements
                        : null
                    );
                    if (ss != null) {
                        int isi = ss.indexOf(is);

                        // Check all local variable declaration statements *before* the IF statement.
                        boolean haveSideEffects = false;
                        for (int i = isi - 1; i >= 0; i--) {
                            BlockStatement bs = (BlockStatement) ss.get(i);
                            if (!(bs instanceof LocalVariableDeclarationStatement)) break;
                            LocalVariableDeclarationStatement lvds = (LocalVariableDeclarationStatement) bs;

                            // Check all variable declarators (int a = 7, b = 8, c = 9, ...)
                            for (int j = lvds.variableDeclarators.length - 1; j >= 0; j--) {
                                VariableDeclarator       vd = lvds.variableDeclarators[j];
                                ArrayInitializerOrRvalue lvi = vd.initializer;
                                if (vd.localVariable == lv) {
                                    if (!lvds.isFinal() && haveSideEffects) {
                                        // The non-final LV may be modified by intervening VD initializers.
                                        return UnitCompiler.NOT_CONSTANT;
                                    }
                                    // The LV is FINAL and thus cannot be modified by intervening VD initializers.
                                    return this.getConstantValue(lvi);
                                }
                                if (lvi != null) {
                                    haveSideEffects |= UnitCompilerConstantEvaluator.this.context.mayHaveSideEffects(lvi);
                                }
                            }
                        }
                    }
                }
            }
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    /**
     * @return An {@link Integer} or a {@link Long}
     */
    @SuppressWarnings("static-method") private Object
    getConstantValue2(IntegerLiteral il) throws CompileException {

        String v = il.value.toLowerCase();

        // Remove underscores in integer literal (JLS8, section 3.10.1).
        for (;;) {
            int ui = v.indexOf('_');
            if (ui == -1) break;
            v = v.substring(0, ui) + v.substring(ui + 1);
        }

        int     radix;
        boolean signed;

        // HexIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0x")) {
            radix  = 16;
            signed = false;
            v      = v.substring(2);
        } else

        // BinaryIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0b")) {
            radix  = 2;
            signed = false;
            v      = v.substring(2);
        } else

        // OctalIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0") && !"0".equals(v) && !"0l".equals(v)) {
            radix  = 8;
            signed = false;
            v      = v.substring(1);
        } else

        // Must be a DecimalIntegerLiteral (JLS8, section 3.10.1).
        {
            radix  = 10;
            signed = true;
        }

        try {

            if (v.endsWith("l")) {
                v = v.substring(0, v.length() - 1);
                return signed ? Long.parseLong(v, radix)   : Numbers.parseUnsignedLong(v, radix);
            } else {
                return signed ? Integer.parseInt(v, radix) : Numbers.parseUnsignedInt(v, radix);
            }
        } catch (NumberFormatException e) {
            // SUPPRESS CHECKSTYLE AvoidHidingCause
            throw UnitCompilerConstantEvaluator.this.context.compileException(il, "Invalid integer literal \"" + il.value + "\"");
        }
    }

    /**
     * @return A {@link Float} or a {@link Double}
     */
    @SuppressWarnings("static-method") private Object
    getConstantValue2(FloatingPointLiteral fpl) throws CompileException {

        String v = fpl.value;

        // Remove underscores in floating point literal.
        for (;;) {
            int ui = v.indexOf('_');
            if (ui == -1) break;
            v = v.substring(0, ui) + v.substring(ui + 1);
        }

        char lastChar = v.charAt(v.length() - 1);
        if (lastChar == 'f' || lastChar == 'F') {
            v = v.substring(0, v.length() - 1);

            float fv;
            try {
                fv = Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new InternalCompilerException(fpl.getLocation(), "SNO: parsing float literal \"" + v + "\": " + e.getMessage(), e);
            }
            if (Float.isInfinite(fv)) {
                throw UnitCompilerConstantEvaluator.this.context.compileException(fpl, "Value of float literal \"" + v + "\" is out of range");
            }
            if (Float.isNaN(fv)) {
                throw new InternalCompilerException(fpl.getLocation(), "SNO: parsing float literal \"" + v + "\" results in NaN");
            }

            // Zero exponents do not make a zero significand nonzero; hexadecimal digits count as well.
            if (fv == 0.0F && nonzeroSignificand(v)) {
                throw this.context.compileException(fpl, "Literal \"" + v + "\" is too small to be represented as a float");
            }

            return Float.valueOf(fv);
        }

        if (lastChar == 'd' || lastChar == 'D') v = v.substring(0, v.length() - 1);

        double dv;
        try {
            dv = Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new InternalCompilerException(fpl.getLocation(), "SNO: parsing double literal \"" + v + "\": " + e.getMessage(), e);
        }
        if (Double.isInfinite(dv)) {
            throw UnitCompilerConstantEvaluator.this.context.compileException(fpl, "Value of double literal \"" + v + "\" is out of range");
        }
        if (Double.isNaN(dv)) {
            throw new InternalCompilerException(fpl.getLocation(), "SNO: parsing double literal \"" + v + "\" results is NaN");
        }

        // Zero exponents do not make a zero significand nonzero; hexadecimal digits count as well.
        if (dv == 0.0F && nonzeroSignificand(v)) {
            throw this.context.compileException(fpl, "Literal \"" + v + "\" is too small to be represented as a double");
        }

        return Double.valueOf(dv);
    }

    private static boolean nonzeroSignificand(String literal) {
        boolean hex = literal.startsWith("0x") || literal.startsWith("0X");
        for (int i = hex ? 2 : 0; i < literal.length(); i++) {
            char ch = literal.charAt(i);
            if (hex ? ch == 'p' || ch == 'P' : ch == 'e' || ch == 'E') break;
            if (ch >= '1' && ch <= '9' || hex && (ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F')) return true;
        }
        return false;
    }

    @SuppressWarnings("static-method") private boolean
    getConstantValue2(BooleanLiteral bl) {
        if (bl.value == "true")  return true;  // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (bl.value == "false") return false; // SUPPRESS CHECKSTYLE StringLiteralEquality
        throw new InternalCompilerException(bl.getLocation(), bl.value);
    }

    @SuppressWarnings("static-method") private char
    getConstantValue2(CharacterLiteral cl) throws CompileException {

        String v = cl.value;

        // Strip opening and closing single quotes.
        v = v.substring(1, v.length() - 1);

        // Decode escape sequences like "\n" and "\0377".
        v = UnitCompilerConstantEvaluator.unescape(v, cl.getLocation());

        if (v.isEmpty()) throw new CompileException("Empty character literal", cl.getLocation());

        if (v.length() > 1) throw new CompileException("Invalid character literal " + cl.value, cl.getLocation());

        return Character.valueOf(v.charAt(0));
    }

    @SuppressWarnings("static-method") private String
    getConstantValue2(StringLiteral sl) throws CompileException {

        String v = sl.value;

        // Strip opening and closing double quotes.
        v = v.substring(1, v.length() - 1);

        // Decode escape sequences like "\n" and "\0377".
        v = UnitCompilerConstantEvaluator.unescape(v, sl.getLocation());

        return v;
    }

    @SuppressWarnings("static-method") private String
    getConstantValue2(TextBlock tb) throws CompileException {

        // Calculate the minimum number of leading spaces for all text block lines.
        int minLeadingSpace = Integer.MAX_VALUE;
        try {
            BufferedReader br = new BufferedReader(new StringReader(tb.value));
            br.readLine(); // Skip triple-quote, space, line-break.
            for (;;) {
                String l = br.readLine();
                if (l == null) break;
                assert l != null : "String \"" + tb.value + "\": Trailing \"\"\" missing";
                for (int i = 0; i < minLeadingSpace; i++) {
                    if (i >= l.length() || !Character.isWhitespace(l.charAt(i))) {
                        minLeadingSpace = i;
                        break;
                    }
                }
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

        // Process the text block's value again and compose the represented string.
        StringBuilder v = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new StringReader(tb.value));
            br.readLine(); // Skip leading triple-quote, space, line-break.
            for (;;) {

                // Read input line.
                String l = br.readLine();
                assert l != null; // The scanner ensures that the text block value has the trailing """.

                // Strip leading space.
                l = l.substring(minLeadingSpace);

                boolean endsWithTripleQuote = false;
                if (l.endsWith("\"\"\"")) {

                    // Strip trailing triple-quote.
                    l = l.substring(0, l.length() - 3);
                    endsWithTripleQuote = true;
                }

                // Strip trailing space.
                for (int i = l.length() - 1;; i--) {
                    if (i == -1) {
                        l = "";
                        break;
                    }
                    if (!Character.isWhitespace(l.charAt(i))) {
                        l = l.substring(0, i + 1);
                        break;
                    }
                }

                // Unescape escaped characters.
                l = UnitCompilerConstantEvaluator.unescape(l, tb.getLocation());

                // Append input line.
                v.append(l);

                if (endsWithTripleQuote) break;

                // Append ONE LF char.
                v.append('\n');
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

        return v.toString();
    }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(NullLiteral nl) { return null; }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(SimpleConstant sl) { return sl.value; }

    /**
     * Decodes any escape sequences like {@code \n}, or {@code \377}, but <em>not</em> {@code &#92;uxxxx}.
     *
     * @return                           <var>s</var>, with all escape sequences replaced with their literal values
     * @throws CompileException          <var>s</var> contains an invalid escape sequence
     * @throws IndexOutOfBoundsException <var>s</var>ends with a backslash
     * @see                              JLS8, section 3.10.6, "Escape Sequences for Character and String Literals"
     */
    private static String
    unescape(String s, @Nullable Location location) throws CompileException {

        // Find the first backslash.
        int i = s.indexOf('\\');

        if (i == -1) {

            // Subject string contains no backslash and thus no escape sequences; so return the original string.
            return s;
        }

        StringBuilder sb = new StringBuilder().append(s, 0, i);

        while (i < s.length()) {

            char c = s.charAt(i++);
            if (c != '\\') {
                sb.append(c);
                continue;
            }

            c = s.charAt(i++);
            {
                int idx = "btnfr\"'\\".indexOf(c);
                if (idx != -1) {
                    sb.append("\b\t\n\f\r\"'\\".charAt(idx));
                    continue;
                }
            }

            // Must be an an OctalEscape (JLS8, section 3.10.6).
            int x = Character.digit(c, 8);
            if (x == -1) throw new CompileException("Invalid escape sequence \"\\" + c + "\"", location);

            if (i < s.length()) {
                c = s.charAt(i);
                int secondDigit = Character.digit(c, 8);
                if (secondDigit != -1) {
                    x = 8 * x + secondDigit;
                    i++;
                    if (i < s.length() && x <= 037) {
                        c = s.charAt(i);
                        int thirdDigit = Character.digit(c, 8);
                        if (thirdDigit != -1) {
                            x = 8 * x + thirdDigit;
                            i++;
                        }
                    }
                }
            }
            sb.append((char) x);
        }

        return sb.toString();
    }

    void rememberLocalConstant(LocalVariable variable, @Nullable Object value) {
        this.constantLocals.put(variable, value);
    }

}
