
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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.CodeContext.BasicBlock;
import org.codehaus.janino.CodeContext.Inserter;
import org.codehaus.janino.CodeContext.Offset;
import org.codehaus.janino.IClass.IConstructor;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Padder;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.ObjectVariableInfo;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.VerificationTypeInfo;

/** Emits instructions and tracks their stack effects using the currently active method context. */
final class UnitCompilerBytecodeEmitter {

    /** Access to the active method context and compilation diagnostics; no context is cached here. */
    interface Context {
        CodeContext getCodeContext();
        int getTargetVersion();
        void compileError(String message, @Nullable Location location) throws CompileException;
    }

    private final Context context;
    private final IClassLoader iClassLoader;

    UnitCompilerBytecodeEmitter(Context context, IClassLoader iClassLoader) {
        this.context = context;
        this.iClassLoader = iClassLoader;
    }


    VerificationTypeInfo
    verificationTypeInfo(@Nullable IType type) {

        if (type == null) return ClassFile.StackMapTableAttribute.NULL_VARIABLE_INFO; // TODO Is that the right thing to do?

        String fd = UnitCompiler.rawTypeOf(type).getDescriptor();
        if (
            Descriptor.BOOLEAN.equals(fd)
            || Descriptor.BYTE.equals(fd)
            || Descriptor.CHAR.equals(fd)
            || Descriptor.INT.equals(fd)
            || Descriptor.SHORT.equals(fd)
        ) return ClassFile.StackMapTableAttribute.INTEGER_VARIABLE_INFO;
        if (Descriptor.LONG.equals(fd))   return ClassFile.StackMapTableAttribute.LONG_VARIABLE_INFO;
        if (Descriptor.FLOAT.equals(fd))  return ClassFile.StackMapTableAttribute.FLOAT_VARIABLE_INFO;
        if (Descriptor.DOUBLE.equals(fd)) return ClassFile.StackMapTableAttribute.DOUBLE_VARIABLE_INFO;
        if (
            Descriptor.isClassOrInterfaceReference(fd)
            || Descriptor.isArrayReference(fd)
        ) return new ObjectVariableInfo(this.context.getCodeContext().getClassFile().addConstantClassInfo(fd), fd);

        throw new InternalCompilerException("Cannot make VerificationTypeInfo from \"" + fd + "\"");
    }

    // ============================= BYTE CODE GENERATION METHODS, IN ALPHABETICAL ORDER =============================

    void
    aconstnull(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.ACONST_NULL);
        this.context.getCodeContext().pushNullOperand();
    }

    void
    add(Locatable locatable) { this.mulDivRemAddSub(locatable, "+"); }

    void
    andOrXor(Locatable locatable, String operator) {

        VerificationTypeInfo operand2 = this.context.getCodeContext().popIntOrLongOperand();
        VerificationTypeInfo operand1 = this.context.getCodeContext().popIntOrLongOperand();

        assert operand1 == operand2;

        final int opcode = (
            operator == "&" ? Opcode.IAND :
            operator == "|" ? Opcode.IOR  :
            operator == "^" ? Opcode.IXOR :
            Integer.MAX_VALUE
        ) + (operand1 == StackMapTableAttribute.LONG_VARIABLE_INFO ? 1 : 0);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.context.getCodeContext().pushOperand(operand1);
    }

    void
    anewarray(Locatable locatable, IClass componentType) {
        IClass arrayType = this.iClassLoader.getArrayIClass(componentType);

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().popIntOperand();
        this.write(Opcode.ANEWARRAY);
        this.writeConstantClassInfo(componentType);
        this.context.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    void
    arraylength(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        try {
            this.context.getCodeContext().popObjectOperand();
            this.write(Opcode.ARRAYLENGTH);
            this.context.getCodeContext().pushIntOperand();
        } catch (AssertionError ae) {
            throw new InternalCompilerException(locatable.getLocation(), null, ae);
        }
    }

    void
    arraystore(Locatable locatable, IType lhsComponentType) {
        this.addLineNumberOffset(locatable);

        this.context.getCodeContext().popOperand();
        this.context.getCodeContext().popOperand();
        this.context.getCodeContext().popOperand();
        this.write(Opcode.IASTORE + UnitCompilerBytecodeEmitter.ilfdabcs(UnitCompiler.rawTypeOf(lhsComponentType)));
    }

    void
    athrow(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.ATHROW);

        this.context.getCodeContext().currentInserter().setStackMap(null);
    }

    void
    checkcast(Locatable locatable, IType targetType) {

        IClass rawTargetType = UnitCompiler.rawTypeOf(targetType);

        this.addLineNumberOffset(locatable);
        this.write(Opcode.CHECKCAST);
        this.writeConstantClassInfo(rawTargetType);
        this.context.getCodeContext().popOperand();
        this.context.getCodeContext().pushObjectOperand(rawTargetType.getDescriptor());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    void
    cmp(Locatable locatable, int opIdx) {
        assert opIdx >= UnitCompilerBytecodeEmitter.EQ && opIdx <= UnitCompilerBytecodeEmitter.LE;

        VerificationTypeInfo operand2 = this.context.getCodeContext().currentInserter().getStackMap().peekOperand();
        this.context.getCodeContext().popOperand();
        VerificationTypeInfo operand1 = this.context.getCodeContext().currentInserter().getStackMap().peekOperand();
        this.context.getCodeContext().popOperand();

        if (operand1 == StackMapTableAttribute.LONG_VARIABLE_INFO && operand2 == StackMapTableAttribute.LONG_VARIABLE_INFO) {
            this.write(Opcode.LCMP);
        } else
        if (operand1 == StackMapTableAttribute.FLOAT_VARIABLE_INFO && operand2 == StackMapTableAttribute.FLOAT_VARIABLE_INFO) {
            this.write(opIdx == UnitCompilerBytecodeEmitter.GE || opIdx == UnitCompilerBytecodeEmitter.GT ? Opcode.FCMPL : Opcode.FCMPG);
        } else
        if (operand1 == StackMapTableAttribute.DOUBLE_VARIABLE_INFO && operand2 == StackMapTableAttribute.DOUBLE_VARIABLE_INFO) {
            this.write(opIdx == UnitCompilerBytecodeEmitter.GE || opIdx == UnitCompilerBytecodeEmitter.GT ? Opcode.DCMPL : Opcode.DCMPG);
        } else
        {
            throw new AssertionError(operand1 + " and " + operand2);
        }
        this.context.getCodeContext().pushIntOperand();
    }

    /**
     * Duplicates the top operand: ... a => ... a a
     */
    void
    dup(Locatable locatable) {

        VerificationTypeInfo topOperand = this.context.getCodeContext().peekOperand();

        this.addLineNumberOffset(locatable);
        this.write(topOperand.category() == 1 ? Opcode.DUP : Opcode.DUP2);

        this.context.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Duplicates the top two operands: ... a b => ... a b a b.
     * This works iff both the top operand <em>and</em> the top-but-one operand have size 1.
     */
    private void
    dup2(Locatable locatable) {

        this.addLineNumberOffset(locatable);

        VerificationTypeInfo topOperand = this.context.getCodeContext().popOperand();
        assert topOperand.category() == 1;
        VerificationTypeInfo topButOneOperand = this.context.getCodeContext().popOperand();
        assert topButOneOperand.category() == 1;
        this.write(Opcode.DUP2);
        this.context.getCodeContext().pushOperand(topButOneOperand);
        this.context.getCodeContext().pushOperand(topOperand);
        this.context.getCodeContext().pushOperand(topButOneOperand);
        this.context.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Duplicates the top <var>n</var> operands.
     * <dl>
     *   <dt>n == 0</dt><dd>(nothing)</dd>
     *   <dt>n == 1</dt><dd>... a => ... a a</dd>
     *   <dt>n == 2</dt><dd>... a b => ... a b a b</dd>
     * </dl>
     */
    void
    dupn(Locatable locatable, int n) {

        switch (n) {
        case 0:  ;                     break;
        case 1:  this.dup(locatable);  break;
        case 2:  this.dup2(locatable); break;
        default: throw new AssertionError(n);
        }
    }

    /**
     * Copies the top operand one position down: b a => a b a
     */
    void
    dupx(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.context.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.context.getCodeContext().popOperand();

        this.addLineNumberOffset(locatable);
        this.write((
            topOperand.category() == 1
            ? (topButOneOperand.category() == 1 ? Opcode.DUP_X1  : Opcode.DUP_X2)
            : (topButOneOperand.category() == 1 ? Opcode.DUP2_X1 : Opcode.DUP2_X2)
        ));

        this.context.getCodeContext().pushOperand(topOperand);
        this.context.getCodeContext().pushOperand(topButOneOperand);
        this.context.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Copies the top operand <em>two</em> positions down: c b a => a c b a.
     * This works iff the top-but-one and top-but-two operands <em>both</em> have size 1.
     */
    private void
    dupx2(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.context.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.context.getCodeContext().popOperand();
        VerificationTypeInfo topButTwoOperand = this.context.getCodeContext().popOperand();

        assert topButOneOperand.category() == 1;
        assert topButTwoOperand.category() == 1;

        this.addLineNumberOffset(locatable);
        this.write(topOperand.category() == 1 ? Opcode.DUP_X2 : Opcode.DUP2_X2);

        this.context.getCodeContext().pushOperand(topOperand);
        this.context.getCodeContext().pushOperand(topButTwoOperand);
        this.context.getCodeContext().pushOperand(topButOneOperand);
        this.context.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Copies the top operand <em>positions</em> down.
     * <dl>
     *   <dt>n == 0</dt><dd>... a => ... a a</dd>
     *   <dt>n == 1</dt><dd>... b a => ... a b a</dd>
     *   <dt>n == 2</dt><dd>... c b a => ... a c b a</dd>
     * </dl>
     */
    void
    dupxx(Locatable locatable, int positions) {
        switch (positions) {
        case 0:  this.dup(locatable);   break;
        case 1:  this.dupx(locatable);  break;
        case 2:  this.dupx2(locatable); break;
        default: throw new AssertionError(positions);
        }
    }

    void
    getfield(Locatable locatable, IClass.IField iField) throws CompileException {
        this.getfield(
            locatable,                   // locatable
            iField.getDeclaringIClass(), // declaringIClass
            iField.getName(),            // fieldName
            iField.getType(),            // fieldType
            iField.isStatic()            // statiC
        );
    }

    void
    getfield(Locatable locatable, IClass declaringIClass, String fieldName, IClass fieldType, boolean statiC) {

        this.addLineNumberOffset(locatable);
        if (statiC) {
            this.write(Opcode.GETSTATIC);
        } else {
            this.write(Opcode.GETFIELD);
            this.context.getCodeContext().popOperand();
        }
        this.writeConstantFieldrefInfo(
            declaringIClass, // iClass
            fieldName,       // fieldName
            fieldType        // fieldType
        );
        this.context.getCodeContext().pushOperand(fieldType.getDescriptor());
    }

    /**
         * @param opcode One of IF* and GOTO
         */
        void
        gotO(Locatable locatable, CodeContext.Offset dst) {
            assert dst instanceof CodeContext.BasicBlock;
            this.context.getCodeContext().writeBranch(Opcode.GOTO, dst);
            this.context.getCodeContext().currentInserter().setStackMap(null);
        }

    /**
     * @param opIdx {@link #EQ} or {@link #NE}
     */
    void
    if_acmpxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx == UnitCompilerBytecodeEmitter.EQ || opIdx == UnitCompilerBytecodeEmitter.NE : opIdx;

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().writeBranch(Opcode.IF_ACMPEQ + opIdx, dst);
        this.context.getCodeContext().popReferenceOperand();
        this.context.getCodeContext().popReferenceOperand();
        // Every incoming edge contributes, including multiple forward short-circuit branches.
        dst.mergeStackMap(this.context.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    void
    if_icmpxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx >= UnitCompilerBytecodeEmitter.EQ && opIdx <= UnitCompilerBytecodeEmitter.LE;

        assert dst instanceof BasicBlock;

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().writeBranch(Opcode.IF_ICMPEQ + opIdx, dst);
        this.context.getCodeContext().popIntOperand();
        this.context.getCodeContext().popIntOperand();
        // Every incoming edge contributes, including multiple forward short-circuit branches.
        dst.mergeStackMap(this.context.getCodeContext().currentInserter().getStackMap());
    }

    static final int EQ = 0;

    static final int NE = 1;

    static final int LT = 2;

    static final int GE = 3;

    static final int GT = 4;

    static final int LE = 5;

    void
    ifnonnull(Locatable locatable, CodeContext.Offset dst) {
        this.context.getCodeContext().writeBranch(Opcode.IFNONNULL, dst);
        this.context.getCodeContext().popReferenceOperand();
        // Every incoming edge contributes, including multiple forward short-circuit branches.
        dst.mergeStackMap(this.context.getCodeContext().currentInserter().getStackMap());
    }

    void
    ifnull(Locatable locatable, CodeContext.Offset dst) {
        this.context.getCodeContext().writeBranch(Opcode.IFNULL, dst);
        this.context.getCodeContext().popReferenceOperand();
        // Every incoming edge contributes, including multiple forward short-circuit branches.
        dst.mergeStackMap(this.context.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    void
    ifxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx >= UnitCompilerBytecodeEmitter.EQ && opIdx <= UnitCompilerBytecodeEmitter.LE;

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().writeBranch(Opcode.IFEQ + opIdx, dst);
        this.context.getCodeContext().popIntOperand();
        // Every incoming edge contributes, including multiple forward short-circuit branches.
        dst.mergeStackMap(this.context.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param operator Must be either "++" or "--", as an @link {@link String#intern() interned} string
     */
    void
    iinc(Locatable locatable, LocalVariable lv, String operator) {
        this.addLineNumberOffset(locatable);
        if (lv.getSlotIndex() > 255) {
            this.write(Opcode.WIDE);
            this.write(Opcode.IINC);
            this.writeShort(lv.getSlotIndex());
            this.writeShort(operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        } else {
            this.write(Opcode.IINC);
            this.writeByte(lv.getSlotIndex());
            this.writeByte(operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        }
    }

    void
    instanceoF(Locatable locatable, IType rhsType) {
        this.addLineNumberOffset(locatable);

        this.context.getCodeContext().popReferenceOperand();
        this.write(Opcode.INSTANCEOF);
        this.writeConstantClassInfo(UnitCompiler.rawTypeOf(rhsType));
        this.context.getCodeContext().pushIntOperand();
    }

    /**
     * Expects the target object and the arguments on the operand stack.
     */
    void
    invoke(
        Locatable        locatable,
        int              opcode,
        IClass           declaringIClass,
        String           methodName,
        MethodDescriptor methodDescriptor,
        boolean          useInterfaceMethodRef
    ) {

        this.addLineNumberOffset(locatable);

        // Pop the arguments off the operand stack.
        for (int i = methodDescriptor.parameterFds.length - 1; i >= 0; i--) {
            this.context.getCodeContext().popOperandAssignableTo(methodDescriptor.parameterFds[i]);
        }

        // Pop the target object off the operand stack.
        if (opcode == Opcode.INVOKEINTERFACE || opcode == Opcode.INVOKESPECIAL || opcode == Opcode.INVOKEVIRTUAL) {
            this.context.getCodeContext().popObjectOrUninitializedOrUninitializedThisOperand();
        }

        // Generate the instruction.
        this.write(opcode);
        if (useInterfaceMethodRef) {
            this.writeConstantInterfaceMethodrefInfo(declaringIClass, methodName, methodDescriptor);
        } else {
            this.writeConstantMethodrefInfo(declaringIClass, methodName, methodDescriptor);
        }

        switch (opcode) {

        case Opcode.INVOKEDYNAMIC:
            this.writeByte(0);
            this.writeByte(0);
            break;

        case Opcode.INVOKEINTERFACE:
            int count = 1;
            for (String pfd : methodDescriptor.parameterFds) count += Descriptor.size(pfd);
            this.writeByte(count);
            this.writeByte(0);
            break;

        case Opcode.INVOKESPECIAL:
        case Opcode.INVOKESTATIC:
        case Opcode.INVOKEVIRTUAL:
            ;
            break;

        default:
            throw new AssertionError(opcode);
        }

        if (!methodDescriptor.returnFd.equals(Descriptor.VOID)) {
            this.context.getCodeContext().pushOperand(methodDescriptor.returnFd);
        }
    }

    void
    l2i(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.context.getCodeContext().popLongOperand();
        this.write(Opcode.L2I);
        this.context.getCodeContext().pushIntOperand();

    }

    // Load the value of a local variable onto the stack and return its type.
    IType
    load(Locatable locatable, LocalVariable localVariable) {
        this.load(locatable, localVariable.type, localVariable.getSlotIndex());
        return localVariable.type;
    }

    void
    load(Locatable locatable, IType localVariableType, int localVariableIndex) {
        assert localVariableIndex >= 0 && localVariableIndex <= 65535;

        this.addLineNumberOffset(locatable);

        IClass rawClass = UnitCompiler.rawTypeOf(localVariableType);

        if (localVariableIndex <= 3) {
            this.write(Opcode.ILOAD_0 + 4 * UnitCompilerBytecodeEmitter.ilfda(rawClass) + localVariableIndex);
        } else
        if (localVariableIndex <= 255) {
            this.write(Opcode.ILOAD + UnitCompilerBytecodeEmitter.ilfda(rawClass));
            this.write(localVariableIndex);
        } else
        {
            this.write(Opcode.WIDE);
            this.write(Opcode.ILOAD + UnitCompilerBytecodeEmitter.ilfda(rawClass));
            this.writeUnsignedShort(localVariableIndex);
        }

        VerificationTypeInfo vti = this.getLocalVariableTypeInfo((short) localVariableIndex);
        this.context.getCodeContext().pushOperand(vti);
    }

    void
    lookupswitch(
        Locatable                  locatable,
        SortedMap<Integer, Offset> caseLabelMap,
        Offset                     defaultLabelOffset
    ) {
        CodeContext.Offset switchOffset = this.context.getCodeContext().newOffset();

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().popIntOperand();
        StackMap smAtCase = this.context.getCodeContext().currentInserter().getStackMap();
        this.write(Opcode.LOOKUPSWITCH);                                            // lookupswitch
        new Padder(this.context.getCodeContext()).set();                                    // 0-3 byte pad
        defaultLabelOffset.setStackMap(smAtCase);
        this.writeOffset(switchOffset, defaultLabelOffset);                         // defaultbyte1-4
        this.writeInt(caseLabelMap.size());                                         // npairs1-4
        for (Map.Entry<Integer, CodeContext.Offset> me : caseLabelMap.entrySet()) {
            Integer match = (Integer) me.getKey();
            CodeContext.Offset offset = (CodeContext.Offset) me.getValue();

            offset.setStackMap(smAtCase);
            this.writeInt(match);                       // match
            this.writeOffset(switchOffset, offset);     // offset
        }
    }

    void
    monitorenter(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.context.getCodeContext().popReferenceOperand();
        this.write(Opcode.MONITORENTER);
    }

    void
    monitorexit(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.context.getCodeContext().popReferenceOperand();
        this.write(Opcode.MONITOREXIT);
    }

    /**
     * @param operator One of {@code * / % + -}
     */
    void
    mulDivRemAddSub(Locatable locatable, String operator) {

        VerificationTypeInfo operand2 = this.context.getCodeContext().popOperand();
        VerificationTypeInfo operand1 = this.context.getCodeContext().popOperand();
        assert operand1 == operand2 : operand1 + " vs. " + operand2;

        final int opcode = (
            operator == "*"   ? Opcode.IMUL :
            operator == "/"   ? Opcode.IDIV :
            operator == "%"   ? Opcode.IREM :
            operator == "+"   ? Opcode.IADD :
            operator == "-"   ? Opcode.ISUB :
            Integer.MAX_VALUE
        ) + UnitCompilerBytecodeEmitter.ilfd(operand1);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.context.getCodeContext().pushOperand(operand1);
    }

    void
    multianewarray(Locatable locatable, int dimExprCount, int dims, IType componentType) {

        IClass arrayType = this.iClassLoader.getArrayIClass(
            UnitCompiler.rawTypeOf(componentType),
            dimExprCount + dims
        );

        this.addLineNumberOffset(locatable);
        for (int i = 0; i < dimExprCount; i++) this.context.getCodeContext().popIntOperand();
        this.write(Opcode.MULTIANEWARRAY);
        this.writeConstantClassInfo(arrayType);
        this.writeByte(dimExprCount);
        this.context.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    /**
     * @param operandType One of BYTE, CHAR, INT, SHORT, LONG, BOOLEAN, LONG, FLOAT, DOUBLE
     */
    void
    neg(Locatable locatable, IClass operandType) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.INEG + UnitCompilerBytecodeEmitter.ilfd(operandType));
    }

    void
    neW(Locatable locatable, IType iType) {
        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().pushUninitializedOperand();
        this.write(Opcode.NEW);
        this.writeConstantClassInfo(UnitCompiler.rawTypeOf(iType));
    }

    void
    newarray(Locatable locatable, IType componentType) {
        IClass arrayType = this.iClassLoader.getArrayIClass(UnitCompiler.rawTypeOf(componentType));

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().popIntOperand();
        this.write(Opcode.NEWARRAY);
        this.writeByte((
            componentType == IClass.BOOLEAN ? 4 :
            componentType == IClass.CHAR    ? 5 :
            componentType == IClass.FLOAT   ? 6 :
            componentType == IClass.DOUBLE  ? 7 :
            componentType == IClass.BYTE    ? 8 :
            componentType == IClass.SHORT   ? 9 :
            componentType == IClass.INT     ? 10 :
            componentType == IClass.LONG    ? 11 : -1
        ));
        this.context.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    void
    pop(Locatable locatable, IType type) {

        if (type == IClass.VOID) return;

        this.addLineNumberOffset(locatable);
        this.write(type == IClass.LONG || type == IClass.DOUBLE ? Opcode.POP2 : Opcode.POP);
        this.context.getCodeContext().popOperand(UnitCompiler.rawTypeOf(type).getDescriptor());
    }

    void
    putfield(Locatable locatable, IField iField) throws CompileException {
        this.putfield(locatable, iField, iField.getDeclaringIClass());
    }

    void putfield(Locatable locatable, IField iField, IClass owner) throws CompileException {
        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().popOperand();
        if (iField.isStatic()) {
            this.write(Opcode.PUTSTATIC);
        } else {
            this.write(Opcode.PUTFIELD);
            this.context.getCodeContext().popOperand();
        }
        this.writeConstantFieldrefInfo(
            owner,                     // iClass
            iField.getName(),            // fieldName
            iField.getType()             // fieldType
        );
    }

    void
    returN(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.RETURN);
        this.context.getCodeContext().currentInserter().setStackMap(null);
    }

    /**
     * @param operator One of {@code << >> >>>}
     */
    void
    shift(Locatable locatable, String operator) {

        this.context.getCodeContext().popIntOperand();
        VerificationTypeInfo operand1 = this.context.getCodeContext().popIntOrLongOperand();

        final int iopcode = (
            operator == "<<"  ? Opcode.ISHL  :
            operator == ">>"  ? Opcode.ISHR  :
            operator == ">>>" ? Opcode.IUSHR :
            Integer.MAX_VALUE
        );

        int opcode = iopcode + UnitCompilerBytecodeEmitter.il(operand1);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.context.getCodeContext().pushOperand(operand1);
    }

    /**
     * Assigns the top operand to the given local variable.
     */
    void
    store(Locatable locatable, LocalVariable localVariable) {
        this.store(
            locatable,                   // locatable
            localVariable.type,          // lvType
            localVariable.getSlotIndex() // lvIndex
        );
    }

    /**
     * @param lvIndex (two slots for LONG and DOUBLE local variables)
     */
    void
    store(Locatable locatable, IType lvType, short lvIndex) {
        this.addLineNumberOffset(locatable);

        if (lvIndex <= 3) {
            this.write(Opcode.ISTORE_0 + 4 * UnitCompilerBytecodeEmitter.ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            this.write(Opcode.ISTORE + UnitCompilerBytecodeEmitter.ilfda(lvType));
            this.write(lvIndex);
        } else
        {
            this.write(Opcode.WIDE);
            this.write(Opcode.ISTORE + UnitCompilerBytecodeEmitter.ilfda(lvType));
            this.writeUnsignedShort(lvIndex);
        }
        this.context.getCodeContext().popOperand();

        this.updateLocalVariableInCurrentStackMap(lvIndex, this.verificationTypeInfo(lvType));
    }

    void
    sub(Locatable locatable) { this.mulDivRemAddSub(locatable, "-"); }

    void
    swap(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.context.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.context.getCodeContext().popOperand();

        this.addLineNumberOffset(locatable);
        this.write(Opcode.SWAP);

        this.context.getCodeContext().pushOperand(topOperand);
        this.context.getCodeContext().pushOperand(topButOneOperand);
    }

    void
    tableswitch(
        Locatable                              locatable,
        SortedMap<Integer, CodeContext.Offset> caseLabelMap,
        Offset                                 defaultLabelOffset
    ) {
        assert defaultLabelOffset instanceof CodeContext.BasicBlock;

        CodeContext.Offset switchOffset = this.context.getCodeContext().newOffset();

        final int low  = (Integer) caseLabelMap.firstKey();
        final int high = (Integer) caseLabelMap.lastKey();

        this.addLineNumberOffset(locatable);
        this.context.getCodeContext().popIntOperand();
        StackMap smAtCase = this.context.getCodeContext().currentInserter().getStackMap();
        this.write(Opcode.TABLESWITCH);
        new Padder(this.context.getCodeContext()).set();
        defaultLabelOffset.setStackMap(smAtCase);
        this.writeOffset(switchOffset, defaultLabelOffset);
        this.writeInt(low);
        this.writeInt(high);

        int cur = low;
        for (Map.Entry<Integer, CodeContext.Offset> me : caseLabelMap.entrySet()) {
            int                caseLabelValue  = (Integer) me.getKey();
            CodeContext.Offset caseLabelOffset = (CodeContext.Offset) me.getValue();

            assert caseLabelOffset instanceof BasicBlock;

            caseLabelOffset.setStackMap(smAtCase);
            while (cur < caseLabelValue) {
                this.writeOffset(switchOffset, defaultLabelOffset);
                ++cur;
            }
            this.writeOffset(switchOffset, caseLabelOffset);
            ++cur;
        }

    }

    void
    xaload(Locatable locatable, IType componentType) {
        this.addLineNumberOffset(locatable);

        IClass rawComponentType = UnitCompiler.rawTypeOf(componentType);

        this.context.getCodeContext().popIntOperand();
        this.context.getCodeContext().popReferenceOperand();
        this.write(Opcode.IALOAD + UnitCompilerBytecodeEmitter.ilfdabcs(rawComponentType));
        this.context.getCodeContext().pushOperand(rawComponentType.getDescriptor());
    }

    void
    xor(Locatable locatable, int opcode) {
        if (opcode != Opcode.IXOR && opcode != Opcode.LXOR) throw new AssertionError(opcode);
        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.context.getCodeContext().popOperand();
    }

    void
    xreturn(Locatable locatable, IType returnType) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.IRETURN + UnitCompilerBytecodeEmitter.ilfda(returnType));
        this.context.getCodeContext().currentInserter().setStackMap(null);
    }

    /**
     * @param t One of BYTE, CHAR, INT, SHORT, LONG, BOOLEAN, LONG, FLOAT, DOUBLE
     */
    private static int
    ilfd(final IType t) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            return 0;
        }
        if (t == IClass.LONG)   return 1;
        if (t == IClass.FLOAT)  return 2;
        if (t == IClass.DOUBLE) return 3;
        throw new InternalCompilerException("Unexpected type \"" + t + "\"");
    }

    private static int
    ilfd(VerificationTypeInfo vti) {
        if (vti == StackMapTableAttribute.INTEGER_VARIABLE_INFO) return 0;
        if (vti == StackMapTableAttribute.LONG_VARIABLE_INFO)    return 1;
        if (vti == StackMapTableAttribute.FLOAT_VARIABLE_INFO)   return 2;
        if (vti == StackMapTableAttribute.DOUBLE_VARIABLE_INFO)  return 3;
        throw new InternalCompilerException("Unexpected type \"" + vti + "\"");
    }

    private static int
    ilfda(IType t) { return !UnitCompiler.isPrimitive(t) ? 4 : UnitCompilerBytecodeEmitter.ilfd(t); }

    private static int
    il(VerificationTypeInfo vti) {

        if (vti == StackMapTableAttribute.INTEGER_VARIABLE_INFO)  return 0;
        if (vti == StackMapTableAttribute.LONG_VARIABLE_INFO)     return 1;

        throw new AssertionError(vti);
    }

    private static int
    ilfdabcs(IClass t) {
        if (t == IClass.INT)     return 0;
        if (t == IClass.LONG)    return 1;
        if (t == IClass.FLOAT)   return 2;
        if (t == IClass.DOUBLE)  return 3;
        if (!t.isPrimitive())    return 4;
        if (t == IClass.BOOLEAN) return 5;
        if (t == IClass.BYTE)    return 5;
        if (t == IClass.CHAR)    return 6;
        if (t == IClass.SHORT)   return 7;
        throw new InternalCompilerException("Unexpected type \"" + t + "\"");
    }

    void
    addLineNumberOffset(Locatable locatable) {
        this.context.getCodeContext().addLineNumberOffset(locatable.getLocation().getLineNumber());
    }

    void
    write(int v) { this.context.getCodeContext().write((byte) v); }

    void
    writeByte(int v) {
        if (v > Byte.MAX_VALUE - Byte.MIN_VALUE) {
            throw new InternalCompilerException("Byte value out of legal range");
        }
        this.context.getCodeContext().write((byte) v);
    }
    void
    writeShort(int v) {
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
            throw new InternalCompilerException("Short value out of legal range");
        }
        this.context.getCodeContext().write((byte) (v >> 8), (byte) v);
    }
    private void
    writeUnsignedShort(int v) {
        if (v < 0 || v > 65535) {
            throw new InternalCompilerException("Unsigned short value out of legal range");
        }
        this.context.getCodeContext().write((byte) (v >> 8), (byte) v);
    }
    private void
    writeInt(int v) {
        this.context.getCodeContext().write((byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v);
    }

    void
    writeLdc(short constantPoolIndex) {
        if (constantPoolIndex >= 0 && constantPoolIndex <= 255) {
            this.write(Opcode.LDC);
            this.write(constantPoolIndex);
        } else {
            this.write(Opcode.LDC_W);
            this.writeShort(constantPoolIndex);
        }
    }

    void
    writeLdc2(short constantPoolIndex) {
        this.write(Opcode.LDC2_W);
        this.context.getCodeContext().writeShort(constantPoolIndex);
    }

    /**
     * Invokes the <var>iMethod</var>; assumes that {@code this} (unless <var>iMethod</var> is static) and the correct
     * number and types of arguments are on the operand stack.
     */
    void
    invokeMethod(Locatable locatable, IMethod iMethod) throws CompileException {

        if (this.context.getTargetVersion() < 8 && iMethod.isStatic() && iMethod.getDeclaringIClass().isInterface()) {
            this.context.compileError(
                "Invocation of static interface methods only available for target version 8+",
                locatable.getLocation()
            );
        }

        int opcode = (
            iMethod.isStatic()                         ? Opcode.INVOKESTATIC :
            iMethod.getDeclaringIClass().isInterface() ? Opcode.INVOKEINTERFACE :
            Opcode.INVOKEVIRTUAL
        );

        boolean useInterfaceMethodref = iMethod.getDeclaringIClass().isInterface();

        this.invoke(
            locatable,                    // locatable
            opcode,                       // opcode
            iMethod.getDeclaringIClass(), // declaringIClass
            iMethod.getName(),            // methodName
            iMethod.getDescriptor(),      // methodMd
            useInterfaceMethodref         // useInterfaceMethodref
        );
    }

    /**
     * Invokes the <var>iConstructor</var>; assumes that {@code this} and the correct number and types of arguments are
     * on the operand stack.
     */
    void
    invokeConstructor(Locatable locatable, IConstructor iConstructor) throws CompileException {
        this.invoke(
            locatable,                         // locatable
            Opcode.INVOKESPECIAL,              // opcode
            iConstructor.getDeclaringIClass(), // declaringIClass
            "<init>",                          // methodName
            iConstructor.getDescriptor(),      // methodMd
            false                              // useInterfaceMethodref
        );
    }

    private void
    writeOffset(CodeContext.Offset src, final CodeContext.Offset dst) {
        this.context.getCodeContext().writeOffset(src, dst);
    }

    // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

    short
    addConstantStringInfo(String value) {
        return this.context.getCodeContext().getClassFile().addConstantStringInfo(value);
    }
    short
    addConstantIntegerInfo(int value) {
        return this.context.getCodeContext().getClassFile().addConstantIntegerInfo(value);
    }
    short
    addConstantLongInfo(long value) {
        return this.context.getCodeContext().getClassFile().addConstantLongInfo(value);
    }
    short
    addConstantFloatInfo(float value) {
        return this.context.getCodeContext().getClassFile().addConstantFloatInfo(value);
    }
    short
    addConstantDoubleInfo(double value) {
        return this.context.getCodeContext().getClassFile().addConstantDoubleInfo(value);
    }
    short
    addConstantClassInfo(IClass iClass) {
        return this.context.getCodeContext().getClassFile().addConstantClassInfo(iClass.getDescriptor());
    }
    private short
    addConstantFieldrefInfo(IClass iClass, String fieldName, IClass fieldType) {
        return this.context.getCodeContext().getClassFile().addConstantFieldrefInfo(iClass.getDescriptor(), fieldName, fieldType.getDescriptor());
    }
    private short
    addConstantMethodrefInfo(IClass iClass, String methodName, String methodFd) {
        return this.context.getCodeContext().getClassFile().addConstantMethodrefInfo(iClass.getDescriptor(), methodName, methodFd);
    }
    private short
    addConstantInterfaceMethodrefInfo(IClass iClass, String methodName, String methodFd) {
        return this.context.getCodeContext().getClassFile().addConstantInterfaceMethodrefInfo(iClass.getDescriptor(), methodName, methodFd);
    }

/* UNUSED
    private void writeConstantIntegerInfo(int value) {
        this.context.getCodeContext().writeShort(-1, this.addConstantIntegerInfo(value));
    }
*/
    private void
    writeConstantClassInfo(IClass iClass) {
        this.writeShort(this.addConstantClassInfo(iClass));
    }
    private void
    writeConstantFieldrefInfo(IClass iClass, String fieldName, IClass fieldType) {
        this.writeShort(this.addConstantFieldrefInfo(iClass, fieldName, fieldType));
    }
    private void
    writeConstantMethodrefInfo(IClass iClass, String methodName, MethodDescriptor methodMd) {
        this.writeShort(this.addConstantMethodrefInfo(iClass, methodName, methodMd.toString()));
    }
    private void
    writeConstantInterfaceMethodrefInfo(IClass iClass, String methodName, MethodDescriptor methodMd) {
        this.writeShort(this.addConstantInterfaceMethodrefInfo(iClass, methodName, methodMd.toString()));
    }

    /**
     * @param lvIndex (two slots for LONG and DOUBLE local variables)
     */
    private VerificationTypeInfo
    getLocalVariableTypeInfo(short lvIndex) {

        StackMap cism = this.context.getCodeContext().currentInserter().getStackMap();
        assert cism != null;

        int nextLvIndex = 0;
        for (VerificationTypeInfo vti : cism.locals()) {
            if (nextLvIndex == lvIndex) return vti;
            nextLvIndex += vti.category();
        }

        throw new InternalCompilerException("Invalid local variable index " + lvIndex);
    }

    void
    updateLocalVariableInCurrentStackMap(short lvIndex, VerificationTypeInfo vti) {

        final Inserter         ci     = this.context.getCodeContext().currentInserter();
        VerificationTypeInfo[] locals = ci.getStackMap().locals();

        int nextLvIndex = 0;
        for (int i = 0; i < locals.length; i++) {
            VerificationTypeInfo vti2 = locals[i];

            if (nextLvIndex == lvIndex) {

                if (vti.equals(vti2)) { // Replace VTI with equal VTI?
                    return;
                }

                if (vti2.category() == vti.category()) { // Replace VTI with VTI of same category?
                    locals[i] = vti;
                } else
                if (vti2.category() == 1 && vti.category() == 2) { // Replace two category 1 VTIs with one category 2 VTI?
                    assert locals[i + 1].category() == 1;
                    locals[i] = vti;
                    System.arraycopy(locals, i + 2, locals, i + 1, locals.length - i - 2);
                    locals = (VerificationTypeInfo[]) Arrays.copyOf(locals, locals.length - 1);
                } else
                if (vti2.category() == 2 && vti.category() == 1) { // Replace one category 2 VTI with two category 1 VTIs?
                    locals = (VerificationTypeInfo[]) Arrays.copyOf(locals, locals.length + 1);
                    System.arraycopy(locals, i + 1, locals, i + 2, locals.length - i - 2);
                    locals[i] = vti;
                    locals[i + 1] = StackMapTableAttribute.TOP_VARIABLE_INFO;
                } else
                {
                    throw new AssertionError(vti2.category() + " vs. " + vti.category());
                }

                ci.setStackMap(new StackMap(locals, ci.getStackMap().operands()));
                return;
            }
            nextLvIndex += vti2.category();
        }

        assert nextLvIndex <= lvIndex;

        while (nextLvIndex < lvIndex) {
            ci.setStackMap(ci.getStackMap().pushLocal(StackMapTableAttribute.TOP_VARIABLE_INFO));
            nextLvIndex++;
        }

        ci.setStackMap(ci.getStackMap().pushLocal(vti));
    }
}
