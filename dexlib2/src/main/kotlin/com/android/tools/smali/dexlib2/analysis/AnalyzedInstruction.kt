/*
 * Copyright 2012, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.ExceptionWithContext
import java.util.ArrayList
import java.util.BitSet
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.Objects
import java.util.SortedSet
import java.util.TreeSet

open class AnalyzedInstruction(
    @JvmField val methodAnalyzer: MethodAnalyzer,
    @JvmField var instruction: Instruction,
    @JvmField val instructionIndex: Int,
    registerCount: Int
) : Comparable<AnalyzedInstruction> {
    /**
     * Instructions that can pass on execution to this one during normal execution
     */
    @JvmField
    val predecessors: TreeSet<AnalyzedInstruction> = TreeSet()

    /**
     * Instructions that can execution could pass on to next during normal execution
     */
    @JvmField
    val successors: LinkedList<AnalyzedInstruction> = LinkedList()

    /**
     * This contains the register types *before* the instruction has executed
     */
    @JvmField
    val preRegisterMap: Array<RegisterType>

    /**
     * This contains the register types *after* the instruction has executed
     */
    @JvmField
    val postRegisterMap: Array<RegisterType>

    /**
     * This contains optional register type overrides for register types from predecessors
     */
    private var predecessorRegisterOverrides: MutableMap<PredecessorOverrideKey, RegisterType>? = null

    /**
     * When deodexing, we might need to deodex this instruction multiple times, when we merge in new register
     * information. When this happens, we need to restore the original (odexed) instruction, so we can deodex it again
     */
    @JvmField
    val originalInstruction: Instruction

    init {
        this.originalInstruction = instruction
        val unknown = RegisterType.getRegisterType(RegisterType.UNKNOWN, null)
        this.postRegisterMap = Array(registerCount) { unknown }
        this.preRegisterMap = Array(registerCount) { unknown }
    }

    fun getInstructionIndex(): Int {
        return instructionIndex
    }

    fun getPredecessorCount(): Int {
        return predecessors.size
    }

    fun getPredecessors(): SortedSet<AnalyzedInstruction> {
        return Collections.unmodifiableSortedSet(predecessors)
    }

    open fun getPredecessorRegisterType(predecessor: AnalyzedInstruction, registerNumber: Int): RegisterType {
        val overrides = predecessorRegisterOverrides
        if (overrides != null) {
            val overrideType = overrides[PredecessorOverrideKey(predecessor, registerNumber)]
            if (overrideType != null) {
                return overrideType
            }
        }
        return predecessor.postRegisterMap[registerNumber]
    }

    open fun addPredecessor(predecessor: AnalyzedInstruction): Boolean {
        return predecessors.add(predecessor)
    }

    fun addSuccessor(successor: AnalyzedInstruction) {
        successors.add(successor)
    }

    fun setDeodexedInstruction(instruction: Instruction) {
        assert(originalInstruction.opcode.odexOnly())
        this.instruction = instruction
    }

    fun restoreOdexedInstruction() {
        assert(originalInstruction.opcode.odexOnly())
        instruction = originalInstruction
    }

    fun getSuccessors(): List<AnalyzedInstruction> {
        return Collections.unmodifiableList(successors)
    }

    fun getInstruction(): Instruction {
        return instruction
    }

    fun getOriginalInstruction(): Instruction {
        return originalInstruction
    }

    /**
     * Is this instruction a "beginning instruction". A beginning instruction is defined to be an instruction
     * that can be the first successfully executed instruction in the method. The first instruction is always a
     * beginning instruction. If the first instruction can throw an exception, and is covered by a try block, then
     * the first instruction of any exception handler for that try block is also a beginning instruction. And likewise,
     * if any of those instructions can throw an exception and are covered by try blocks, the first instruction of the
     * corresponding exception handler is a beginning instruction, etc.
     *
     * To determine this, we simply check if the first predecessor is the fake "StartOfMethod" instruction, which has
     * an instruction index of -1.
     * @return a boolean value indicating whether this instruction is a beginning instruction
     */
    fun isBeginningInstruction(): Boolean {
        //if this instruction has no predecessors, it is either the fake "StartOfMethod" instruction or it is an
        //unreachable instruction.
        if (predecessors.size == 0) {
            return false
        }
        return predecessors.first().instructionIndex == -1
    }

    /*
     * Merges the given register type into the specified pre-instruction register, and also sets the post-instruction
     * register type accordingly if it isn't a destination register for this instruction
     * @param registerNumber Which register to set
     * @param registerType The register type
     * @returns true If the post-instruction register type was changed. This might be false if either the specified
     * register is a destination register for this instruction, or if the pre-instruction register type didn't change
     * after merging in the given register type
     */
    fun mergeRegister(
        registerNumber: Int,
        registerType: RegisterType,
        verifiedInstructions: BitSet,
        overrideFlag: Boolean
    ): Boolean {
        assert(registerNumber >= 0 && registerNumber < postRegisterMap.size)

        val oldRegisterType = preRegisterMap[registerNumber]

        val mergedRegisterType: RegisterType
        if (overrideFlag) {
            mergedRegisterType = getMergedPreRegisterTypeFromPredecessors(registerNumber)
        } else {
            mergedRegisterType = oldRegisterType.merge(registerType)
        }

        if (mergedRegisterType == oldRegisterType) {
            return false
        }

        preRegisterMap[registerNumber] = mergedRegisterType
        verifiedInstructions.clear(instructionIndex)

        if (!setsRegister(registerNumber)) {
            postRegisterMap[registerNumber] = mergedRegisterType
            return true
        }

        return false
    }

    /**
     * Iterates over the predecessors of this instruction, and merges all the post-instruction register types for the
     * given register. Any dead, unreachable, or odexed predecessor is ignored. This takes into account any overridden
     * predecessor register types
     *
     * @param registerNumber the register number
     * @return The register type resulting from merging the post-instruction register types from all predecessors
     */
    fun getMergedPreRegisterTypeFromPredecessors(registerNumber: Int): RegisterType {
        var mergedRegisterType: RegisterType? = null
        for (predecessor in predecessors) {
            val predecessorRegisterType = getPredecessorRegisterType(predecessor, registerNumber)
            if (mergedRegisterType == null) {
                mergedRegisterType = predecessorRegisterType
            } else {
                mergedRegisterType = predecessorRegisterType.merge(mergedRegisterType)
            }
        }
        if (mergedRegisterType == null) {
            // This is a start-of-method or unreachable instruction.
            throw IllegalStateException()
        }
        return mergedRegisterType
    }

    /**
     * Sets the "post-instruction" register type as indicated.
     * @param registerNumber Which register to set
     * @param registerType The "post-instruction" register type
     * @return true if the given register type is different than the existing post-instruction register type
     */
    fun setPostRegisterType(registerNumber: Int, registerType: RegisterType): Boolean {
        assert(registerNumber >= 0 && registerNumber < postRegisterMap.size)

        val oldRegisterType = postRegisterMap[registerNumber]
        if (oldRegisterType == registerType) {
            return false
        }

        postRegisterMap[registerNumber] = registerType
        return true
    }

    /**
     * Adds an override for a register type from a predecessor.
     *
     * This is used to set the register type for only one branch from a conditional jump.
     *
     * @param predecessor Which predecessor is being overridden
     * @param registerNumber The register number of the register being overridden
     * @param registerType The overridden register type
     * @param verifiedInstructions A bit vector of instructions that have been verified
     *
     * @return true if the post-instruction register type for this instruction changed as a result of this override
     */
    fun overridePredecessorRegisterType(
        predecessor: AnalyzedInstruction,
        registerNumber: Int,
        registerType: RegisterType,
        verifiedInstructions: BitSet
    ): Boolean {
        var overrides = predecessorRegisterOverrides
        if (overrides == null) {
            overrides = HashMap()
            predecessorRegisterOverrides = overrides
        }
        overrides[PredecessorOverrideKey(predecessor, registerNumber)] = registerType

        val mergedType = getMergedPreRegisterTypeFromPredecessors(registerNumber)

        if (preRegisterMap[registerNumber] == mergedType) {
            return false
        }

        preRegisterMap[registerNumber] = mergedType
        verifiedInstructions.clear(instructionIndex)

        if (!setsRegister(registerNumber)) {
            if (postRegisterMap[registerNumber] != mergedType) {
                postRegisterMap[registerNumber] = mergedType
                return true
            }
        }

        return false
    }

    fun isInvokeInit(): Boolean {
        if (!instruction.opcode.canInitializeReference()) {
            return false
        }

        val referenceInstruction = instruction as ReferenceInstruction
        val reference: Reference = referenceInstruction.reference
        if (reference is MethodReference) {
            return reference.name == "<init>"
        }

        return false
    }

    /**
     * Determines if this instruction sets the given register, or alters its type
     *
     * @param registerNumber The register to check
     * @return true if this instruction sets the given register or alters its type
     */
    fun setsRegister(registerNumber: Int): Boolean {
        // This method could be implemented by calling getSetRegisters and checking if registerNumber is in the result
        // However, this is a frequently called method, and this is a more efficient implementation, because it doesn't
        // allocate a new list, and it can potentially exit earlier

        if (isInvokeInit()) {
            // When constructing a new object, the register type will be an uninitialized reference after the
            // new-instance instruction, but becomes an initialized reference once the <init> method is called. So even
            // though invoke instructions don't normally change any registers, calling an <init> method will change the
            // type of its object register. If the uninitialized reference has been copied to other registers, they will
            // be initialized as well, so we need to check for that too
            val destinationRegister: Int
            if (instruction is FiveRegisterInstruction) {
                assert((instruction as FiveRegisterInstruction).registerCount > 0)
                destinationRegister = (instruction as FiveRegisterInstruction).registerC
            } else {
                assert(instruction is RegisterRangeInstruction)
                val rangeInstruction = instruction as RegisterRangeInstruction
                assert(rangeInstruction.registerCount > 0)
                destinationRegister = rangeInstruction.startRegister
            }

            val preInstructionDestRegisterType = getPreInstructionRegisterType(destinationRegister)
            if (preInstructionDestRegisterType.category == RegisterType.UNKNOWN) {
                // We never let an uninitialized reference propagate past an invoke-init if the object register type is
                // unknown This is because the uninitialized reference may be an alias to the reference being
                // initialized, but we can't know that until the object register's type is known
                val preInstructionRegisterType = getPreInstructionRegisterType(registerNumber)
                if (preInstructionRegisterType.category == RegisterType.UNINIT_REF ||
                    preInstructionRegisterType.category == RegisterType.UNINIT_THIS
                ) {
                    return true
                }
            }

            if (preInstructionDestRegisterType.category != RegisterType.UNINIT_REF &&
                preInstructionDestRegisterType.category != RegisterType.UNINIT_THIS
            ) {
                return false
            }

            if (registerNumber == destinationRegister) {
                return true
            }

            //check if the uninit ref has been copied to another register
            return preInstructionDestRegisterType == getPreInstructionRegisterType(registerNumber)
        }

        // On art, the optimizer will often nop out a check-cast instruction after an instance-of instruction.
        // Normally, check-cast is where the register type actually changes.
        // In order to correctly handle this case, we have to propagate the narrowed register type to the appropriate
        // branch of the following if-eqz/if-nez
        if (instructionIndex > 0 &&
            methodAnalyzer.classPath.isArt() &&
            getPredecessorCount() == 1 &&
            (instruction.opcode == Opcode.IF_EQZ || instruction.opcode == Opcode.IF_NEZ)
        ) {
            val prevInstruction = predecessors.first()
            if (prevInstruction.instruction.opcode == Opcode.INSTANCE_OF &&
                MethodAnalyzer.canPropagateTypeAfterInstanceOf(
                    prevInstruction, this, methodAnalyzer.classPath
                )
            ) {
                val instanceOfInstruction = prevInstruction.instruction as Instruction22c

                if (registerNumber == instanceOfInstruction.registerB) {
                    return true
                }

                // Additionally, there may be a move instruction just before the instance-of, in order to put the value
                // into a register that is addressable by the instance-of. In this case, we also need to propagate the
                // new register type for the original register that the value was moved from.
                // In some cases, the instance-of may have multiple predecessors. In this case, we should only do the
                // propagation if all predecessors are move-object instructions for the same source register
                // TODO: do we need to do some sort of additional check that these multiple move-object predecessors actually refer to the same value?
                if (instructionIndex > 1) {
                    var originalSourceRegister = -1

                    var newType: RegisterType? = null

                    for (prevPrevAnalyzedInstruction in prevInstruction.predecessors) {
                        val opcode = prevPrevAnalyzedInstruction.instruction.opcode
                        if (opcode == Opcode.MOVE_OBJECT || opcode == Opcode.MOVE_OBJECT_16 ||
                            opcode == Opcode.MOVE_OBJECT_FROM16
                        ) {
                            val moveInstruction =
                                prevPrevAnalyzedInstruction.instruction as TwoRegisterInstruction
                            val originalType =
                                prevPrevAnalyzedInstruction.getPostInstructionRegisterType(
                                    moveInstruction.registerB
                                )
                            if (moveInstruction.registerA != instanceOfInstruction.registerB) {
                                originalSourceRegister = -1
                                break
                            }
                            if (originalType.type == null) {
                                originalSourceRegister = -1
                                break
                            }

                            if (newType == null) {
                                newType = RegisterType.getRegisterType(
                                    methodAnalyzer.classPath,
                                    instanceOfInstruction.reference as TypeReference
                                )
                            }

                            if (MethodAnalyzer.isNotWideningConversion(originalType, newType!!)) {
                                if (originalSourceRegister != -1) {
                                    if (originalSourceRegister != moveInstruction.registerB) {
                                        originalSourceRegister = -1
                                        break
                                    }
                                } else {
                                    originalSourceRegister = moveInstruction.registerB
                                }
                            }
                        } else {
                            originalSourceRegister = -1
                            break
                        }
                    }
                    if (originalSourceRegister != -1 && registerNumber == originalSourceRegister) {
                        return true
                    }
                }
            }
        }

        if (!instruction.opcode.setsRegister()) {
            return false
        }
        val destinationRegister = getDestinationRegister()

        if (registerNumber == destinationRegister) {
            return true
        }
        if (instruction.opcode.setsWideRegister() && registerNumber == destinationRegister + 1) {
            return true
        }
        return false
    }

    fun getSetRegisters(): List<Int> {
        val setRegisters = ArrayList<Int>()

        if (instruction.opcode.setsRegister()) {
            setRegisters.add(getDestinationRegister())
        }
        if (instruction.opcode.setsWideRegister()) {
            setRegisters.add(getDestinationRegister() + 1)
        }

        if (isInvokeInit()) {
            //When constructing a new object, the register type will be an uninitialized reference after the new-instance
            //instruction, but becomes an initialized reference once the <init> method is called. So even though invoke
            //instructions don't normally change any registers, calling an <init> method will change the type of its
            //object register. If the uninitialized reference has been copied to other registers, they will be initialized
            //as well, so we need to check for that too

            val destinationRegister: Int
            if (instruction is FiveRegisterInstruction) {
                destinationRegister = (instruction as FiveRegisterInstruction).registerC
                assert((instruction as FiveRegisterInstruction).registerCount > 0)
            } else {
                assert(instruction is RegisterRangeInstruction)
                val rangeInstruction = instruction as RegisterRangeInstruction
                assert(rangeInstruction.registerCount > 0)
                destinationRegister = rangeInstruction.startRegister
            }

            val preInstructionDestRegisterType = getPreInstructionRegisterType(destinationRegister)
            if (preInstructionDestRegisterType.category == RegisterType.UNINIT_REF ||
                preInstructionDestRegisterType.category == RegisterType.UNINIT_THIS
            ) {
                setRegisters.add(destinationRegister)

                val objectRegisterType = preRegisterMap[destinationRegister]
                for (i in preRegisterMap.indices) {
                    if (i == destinationRegister) {
                        continue
                    }

                    val preInstructionRegisterType = preRegisterMap[i]

                    if (preInstructionRegisterType == objectRegisterType) {
                        setRegisters.add(i)
                    } else if (preInstructionRegisterType.category == RegisterType.UNINIT_REF ||
                        preInstructionRegisterType.category == RegisterType.UNINIT_THIS
                    ) {
                        val postInstructionRegisterType = postRegisterMap[i]
                        if (postInstructionRegisterType.category == RegisterType.UNKNOWN) {
                            setRegisters.add(i)
                        }
                    }
                }
            } else if (preInstructionDestRegisterType.category == RegisterType.UNKNOWN) {
                // We never let an uninitialized reference propagate past an invoke-init if the object register type is
                // unknown This is because the uninitialized reference may be an alias to the reference being
                // initialized, but we can't know that until the object register's type is known

                for (i in preRegisterMap.indices) {
                    val registerType = preRegisterMap[i]
                    if (registerType.category == RegisterType.UNINIT_REF ||
                        registerType.category == RegisterType.UNINIT_THIS
                    ) {
                        setRegisters.add(i)
                    }
                }
            }
        }

        // On art, the optimizer will often nop out a check-cast instruction after an instance-of instruction.
        // Normally, check-cast is where the register type actually changes.
        // In order to correctly handle this case, we have to propagate the narrowed register type to the appropriate
        // branch of the following if-eqz/if-nez
        if (instructionIndex > 0 &&
            methodAnalyzer.classPath.isArt() &&
            getPredecessorCount() == 1 &&
            (instruction.opcode == Opcode.IF_EQZ || instruction.opcode == Opcode.IF_NEZ)
        ) {
            val prevInstruction = predecessors.first()
            if (prevInstruction.instruction.opcode == Opcode.INSTANCE_OF &&
                MethodAnalyzer.canPropagateTypeAfterInstanceOf(
                    prevInstruction, this, methodAnalyzer.classPath
                )
            ) {
                val instanceOfInstruction = prevInstruction.instruction as Instruction22c
                setRegisters.add(instanceOfInstruction.registerB)

                // Additionally, there may be a move instruction just before the instance-of, in order to put the value
                // into a register that is addressable by the instance-of. In this case, we also need to propagate the
                // new register type for the original register that the value was moved from.
                // In some cases, the instance-of may have multiple predecessors. In this case, we should only do the
                // propagation if all predecessors are move-object instructions for the same source register
                // TODO: do we need to do some sort of additional check that these multiple move-object predecessors actually refer to the same value?
                if (instructionIndex > 1) {
                    var originalSourceRegister = -1

                    var newType: RegisterType? = null

                    for (prevPrevAnalyzedInstruction in prevInstruction.predecessors) {
                        val opcode = prevPrevAnalyzedInstruction.instruction.opcode
                        if (opcode == Opcode.MOVE_OBJECT || opcode == Opcode.MOVE_OBJECT_16 ||
                            opcode == Opcode.MOVE_OBJECT_FROM16
                        ) {
                            val moveInstruction =
                                prevPrevAnalyzedInstruction.instruction as TwoRegisterInstruction
                            val originalType =
                                prevPrevAnalyzedInstruction.getPostInstructionRegisterType(
                                    moveInstruction.registerB
                                )
                            if (moveInstruction.registerA != instanceOfInstruction.registerB) {
                                originalSourceRegister = -1
                                break
                            }
                            if (originalType.type == null) {
                                originalSourceRegister = -1
                                break
                            }

                            if (newType == null) {
                                newType = RegisterType.getRegisterType(
                                    methodAnalyzer.classPath,
                                    instanceOfInstruction.reference as TypeReference
                                )
                            }

                            if (MethodAnalyzer.isNotWideningConversion(originalType, newType!!)) {
                                if (originalSourceRegister != -1) {
                                    if (originalSourceRegister != moveInstruction.registerB) {
                                        originalSourceRegister = -1
                                        break
                                    }
                                } else {
                                    originalSourceRegister = moveInstruction.registerB
                                }
                            }
                        } else {
                            originalSourceRegister = -1
                            break
                        }
                    }
                    if (originalSourceRegister != -1) {
                        setRegisters.add(originalSourceRegister)
                    }
                }
            }
        }

        return setRegisters
    }

    fun getDestinationRegister(): Int {
        if (!this.instruction.opcode.setsRegister()) {
            throw ExceptionWithContext(
                "Cannot call getDestinationRegister() for an instruction that doesn't " +
                        "store a value"
            )
        }
        return (instruction as OneRegisterInstruction).registerA
    }

    fun getRegisterCount(): Int {
        return postRegisterMap.size
    }

    fun getPostInstructionRegisterType(registerNumber: Int): RegisterType {
        return postRegisterMap[registerNumber]
    }

    fun getPreInstructionRegisterType(registerNumber: Int): RegisterType {
        return preRegisterMap[registerNumber]
    }

    override fun compareTo(analyzedInstruction: AnalyzedInstruction): Int {
        if (instructionIndex < analyzedInstruction.instructionIndex) {
            return -1
        } else if (instructionIndex == analyzedInstruction.instructionIndex) {
            return 0
        } else {
            return 1
        }
    }

    private class PredecessorOverrideKey(
        @JvmField val analyzedInstruction: AnalyzedInstruction,
        @JvmField val registerNumber: Int
    ) {
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as PredecessorOverrideKey
            return registerNumber == that.registerNumber &&
                    analyzedInstruction == that.analyzedInstruction
        }

        override fun hashCode(): Int {
            return Objects.hash(analyzedInstruction, registerNumber)
        }
    }
}
