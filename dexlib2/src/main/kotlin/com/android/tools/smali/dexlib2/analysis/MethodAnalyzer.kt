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

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22cs
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35mi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35ms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rmi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rms
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction22c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.util.TypeUtils
import com.android.tools.smali.dexlib2.util.getParameterRegisterCount
import com.android.tools.smali.dexlib2.util.isConstructor
import com.android.tools.smali.dexlib2.util.isStatic
import com.android.tools.smali.dexlib2.writer.util.TryListBuilder
import com.android.tools.smali.util.BitSetUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.IteratorUtils
import com.android.tools.smali.util.SparseArray
import java.util.BitSet
import java.util.Collections

/**
 * The MethodAnalyzer performs several functions. It "analyzes" the instructions and infers the register types
 * for each register, it can deodex odexed instructions, and it can verify the bytecode. The analysis and verification
 * are done in two separate passes, because the analysis has to process instructions multiple times in some cases, and
 * there's no need to perform the verification multiple times, so we wait until the method is fully analyzed and then
 * verify it.
 *
 * Before calling the analyze() method, you must have initialized the ClassPath by calling
 * ClassPath.InitializeClassPath
 */
open class MethodAnalyzer(
    val classPath: ClassPath,
    private val method: Method,
    private val inlineResolver: InlineMethodResolver?,
    private val normalizeVirtualMethods: Boolean
) {
    private val methodImpl: MethodImplementation

    private val paramRegisterCountValue: Int

    // This contains all the AnalyzedInstruction instances, keyed by the code unit address of the instruction
    private val analyzedInstructionsByAddress = SparseArray<AnalyzedInstruction>(0)

    // Which instructions have been analyzed, keyed by instruction index
    private val analyzedState: BitSet

    private var analysisExceptionValue: AnalysisException? = null

    // This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    // register types for this instruction to the parameter types, in order to have them propagate to all of its
    // successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    // instruction, etc.
    private val startOfMethod: AnalyzedInstruction

    init {
        val methodImpl = method.implementation
            ?: throw IllegalArgumentException("The method has no implementation")

        this.methodImpl = methodImpl

        // Override AnalyzedInstruction and provide custom implementations of some of the methods, so that we don't
        // have to handle the case this special case of instruction being null, in the main class
        startOfMethod = object : AnalyzedInstruction(
            this, ImmutableInstruction10x(Opcode.NOP), -1, methodImpl.registerCount
        ) {
            override fun addPredecessor(predecessor: AnalyzedInstruction): Boolean {
                throw UnsupportedOperationException()
            }

            override fun getPredecessorRegisterType(
                predecessor: AnalyzedInstruction,
                registerNumber: Int
            ): RegisterType {
                throw UnsupportedOperationException()
            }
        }

        buildInstructionList()

        analyzedState = BitSet(analyzedInstructionsByAddress.size())
        paramRegisterCountValue = getParameterRegisterCount(method)
        analyze()
    }

    private fun analyze() {
        val method = this.method
        val methodImpl = this.methodImpl

        val totalRegisters = methodImpl.registerCount
        val parameterRegisters = paramRegisterCountValue

        val nonParameterRegisters = totalRegisters - parameterRegisters

        //if this isn't a static method, determine which register is the "this" register and set the type to the
        //current class
        if (!isStatic(method)) {
            val thisRegister = totalRegisters - parameterRegisters

            //if this is a constructor, then set the "this" register to an uninitialized reference of the current class
            if (isConstructor(method)) {
                setPostRegisterTypeAndPropagateChanges(
                    startOfMethod, thisRegister,
                    RegisterType.getRegisterType(
                        RegisterType.UNINIT_THIS,
                        classPath.getClass(method.definingClass)
                    )
                )
            } else {
                setPostRegisterTypeAndPropagateChanges(
                    startOfMethod, thisRegister,
                    RegisterType.getRegisterType(
                        RegisterType.REFERENCE,
                        classPath.getClass(method.definingClass)
                    )
                )
            }

            propagateParameterTypes(totalRegisters - parameterRegisters + 1)
        } else {
            propagateParameterTypes(totalRegisters - parameterRegisters)
        }

        val uninit = RegisterType.getRegisterType(RegisterType.UNINIT, null)
        for (i in 0 until nonParameterRegisters) {
            setPostRegisterTypeAndPropagateChanges(startOfMethod, i, uninit)
        }

        val instructionsToAnalyze = BitSet(analyzedInstructionsByAddress.size())

        //make sure all of the "first instructions" are marked for processing
        for (successor in startOfMethod.successors) {
            instructionsToAnalyze.set(successor.instructionIndex)
        }

        val undeodexedInstructions = BitSet(analyzedInstructionsByAddress.size())

        do {
            var didSomething = false

            while (!instructionsToAnalyze.isEmpty()) {
                var i = instructionsToAnalyze.nextSetBit(0)
                while (i >= 0) {
                    instructionsToAnalyze.clear(i)
                    if (analyzedState.get(i)) {
                        i = instructionsToAnalyze.nextSetBit(i + 1)
                        continue
                    }
                    val instructionToAnalyze = analyzedInstructionsByAddress.valueAt(i)
                    try {
                        if (instructionToAnalyze.originalInstruction.opcode.odexOnly()) {
                            //if we had deodexed an odex instruction in a previous pass, we might have more specific
                            //register information now, so let's restore the original odexed instruction and
                            //re-deodex it
                            instructionToAnalyze.restoreOdexedInstruction()
                        }

                        if (!analyzeInstruction(instructionToAnalyze)) {
                            undeodexedInstructions.set(i)
                            i = instructionsToAnalyze.nextSetBit(i + 1)
                            continue
                        } else {
                            didSomething = true
                            undeodexedInstructions.clear(i)
                        }
                    } catch (ex: AnalysisException) {
                        this.analysisExceptionValue = ex
                        val codeAddress = getInstructionAddress(instructionToAnalyze)
                        ex.codeAddress = codeAddress
                        ex.addContext("opcode: ${instructionToAnalyze.instruction.opcode.mnemonic}")
                        ex.addContext("code address: ${codeAddress}")
                        ex.addContext("method: ${method}")
                        break
                    }

                    analyzedState.set(instructionToAnalyze.instructionIndex)

                    for (successor in instructionToAnalyze.successors) {
                        instructionsToAnalyze.set(successor.instructionIndex)
                    }
                    i = instructionsToAnalyze.nextSetBit(i + 1)
                }
                if (analysisExceptionValue != null) {
                    break
                }
            }

            if (!didSomething) {
                break
            }

            if (!undeodexedInstructions.isEmpty()) {
                var i = undeodexedInstructions.nextSetBit(0)
                while (i >= 0) {
                    instructionsToAnalyze.set(i)
                    i = undeodexedInstructions.nextSetBit(i + 1)
                }
            }
        } while (true)

        //Now, go through and fix up any unresolvable odex instructions. These are usually odex instructions
        //that operate on a null register, and thus always throw an NPE. They can also be any sort of odex instruction
        //that occurs after an unresolvable odex instruction. We deodex if possible, or replace with an
        //UnresolvableOdexInstruction
        for (i in 0 until analyzedInstructionsByAddress.size()) {
            val analyzedInstruction = analyzedInstructionsByAddress.valueAt(i)

            val instruction = analyzedInstruction.instruction

            if (instruction.opcode.odexOnly()) {
                val objectRegisterNumber: Int = when (instruction.opcode.format) {
                    Format.Format10x -> {
                        analyzeOdexReturnVoid(analyzedInstruction, false)
                        continue
                    }
                    Format.Format21c, Format.Format22c -> {
                        analyzePutGetVolatile(analyzedInstruction, false)
                        continue
                    }
                    Format.Format35c -> {
                        analyzeInvokeDirectEmpty(analyzedInstruction, false)
                        continue
                    }
                    Format.Format3rc -> {
                        analyzeInvokeObjectInitRange(analyzedInstruction, false)
                        continue
                    }
                    Format.Format22cs -> (instruction as Instruction22cs).registerB
                    Format.Format35mi, Format.Format35ms -> (instruction as FiveRegisterInstruction).registerC
                    Format.Format3rmi, Format.Format3rms -> (instruction as RegisterRangeInstruction).startRegister
                    else -> continue
                }

                analyzedInstruction.setDeodexedInstruction(
                    UnresolvedOdexInstruction(instruction, objectRegisterNumber)
                )
            }
        }
    }

    private fun propagateParameterTypes(parameterStartRegister: Int) {
        var i = 0
        for (parameter in method.parameters) {
            if (TypeUtils.isWideType(parameter)) {
                setPostRegisterTypeAndPropagateChanges(
                    startOfMethod, parameterStartRegister + i++,
                    RegisterType.getWideRegisterType(parameter, true)
                )
                setPostRegisterTypeAndPropagateChanges(
                    startOfMethod, parameterStartRegister + i++,
                    RegisterType.getWideRegisterType(parameter, false)
                )
            } else {
                setPostRegisterTypeAndPropagateChanges(
                    startOfMethod, parameterStartRegister + i++,
                    RegisterType.getRegisterType(classPath, parameter)
                )
            }
        }
    }

    val analyzedInstructions: List<AnalyzedInstruction>
        get() = analyzedInstructionsByAddress.values

    val instructions: List<Instruction> get() {
        return analyzedInstructionsByAddress.values.map { it.instruction }
    }

    val analysisException: AnalysisException?
        get() = analysisExceptionValue

    val paramRegisterCount: Int
        get() = paramRegisterCountValue

    fun getInstructionAddress(instruction: AnalyzedInstruction): Int {
        return analyzedInstructionsByAddress.keyAt(instruction.instructionIndex)
    }

    private fun setDestinationRegisterTypeAndPropagateChanges(
        analyzedInstruction: AnalyzedInstruction,
        registerType: RegisterType
    ) {
        setPostRegisterTypeAndPropagateChanges(
            analyzedInstruction, analyzedInstruction.destinationRegister,
            registerType
        )
    }

    private fun propagateChanges(changedInstructions: BitSet, registerNumber: Int, override: Boolean) {
        //Using a for loop inside the while loop optimizes for the common case of the successors of an instruction
        //occurring after the instruction. Any successors that occur prior to the instruction will be picked up on
        //the next iteration of the while loop.
        //This could also be done recursively, but in large methods it would likely cause very deep recursion.
        while (!changedInstructions.isEmpty()) {
            var instructionIndex = changedInstructions.nextSetBit(0)
            while (instructionIndex >= 0) {
                changedInstructions.clear(instructionIndex)

                propagateRegisterToSuccessors(
                    analyzedInstructionsByAddress.valueAt(instructionIndex), registerNumber,
                    changedInstructions, override
                )
                instructionIndex = changedInstructions.nextSetBit(instructionIndex + 1)
            }
        }
    }

    private fun overridePredecessorRegisterTypeAndPropagateChanges(
        analyzedInstruction: AnalyzedInstruction,
        predecessor: AnalyzedInstruction,
        registerNumber: Int,
        registerType: RegisterType
    ) {
        val changedInstructions = BitSet(analyzedInstructionsByAddress.size())

        if (!analyzedInstruction.overridePredecessorRegisterType(
                predecessor, registerNumber, registerType, analyzedState
            )
        ) {
            return
        }
        changedInstructions.set(analyzedInstruction.instructionIndex)

        propagateChanges(changedInstructions, registerNumber, true)

        if (registerType.category == RegisterType.LONG_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            overridePredecessorRegisterTypeAndPropagateChanges(
                analyzedInstruction, predecessor, registerNumber + 1,
                RegisterType.LONG_HI_TYPE
            )
        } else if (registerType.category == RegisterType.DOUBLE_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            overridePredecessorRegisterTypeAndPropagateChanges(
                analyzedInstruction, predecessor, registerNumber + 1,
                RegisterType.DOUBLE_HI_TYPE
            )
        }
    }

    private fun initializeRefAndPropagateChanges(
        analyzedInstruction: AnalyzedInstruction,
        registerNumber: Int,
        registerType: RegisterType
    ) {
        val changedInstructions = BitSet(analyzedInstructionsByAddress.size())

        if (!analyzedInstruction.setPostRegisterType(registerNumber, registerType)) {
            return
        }

        propagateRegisterToSuccessors(analyzedInstruction, registerNumber, changedInstructions, false)

        propagateChanges(changedInstructions, registerNumber, false)

        if (registerType.category == RegisterType.LONG_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            setPostRegisterTypeAndPropagateChanges(
                analyzedInstruction, registerNumber + 1, RegisterType.LONG_HI_TYPE
            )
        } else if (registerType.category == RegisterType.DOUBLE_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            setPostRegisterTypeAndPropagateChanges(
                analyzedInstruction, registerNumber + 1, RegisterType.DOUBLE_HI_TYPE
            )
        }
    }

    private fun setPostRegisterTypeAndPropagateChanges(
        analyzedInstruction: AnalyzedInstruction,
        registerNumber: Int,
        registerType: RegisterType
    ) {
        val changedInstructions = BitSet(analyzedInstructionsByAddress.size())

        if (!analyzedInstruction.setPostRegisterType(registerNumber, registerType)) {
            return
        }

        propagateRegisterToSuccessors(analyzedInstruction, registerNumber, changedInstructions, false)

        propagateChanges(changedInstructions, registerNumber, false)

        if (registerType.category == RegisterType.LONG_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            setPostRegisterTypeAndPropagateChanges(
                analyzedInstruction, registerNumber + 1, RegisterType.LONG_HI_TYPE
            )
        } else if (registerType.category == RegisterType.DOUBLE_LO) {
            checkWidePair(registerNumber, analyzedInstruction)
            setPostRegisterTypeAndPropagateChanges(
                analyzedInstruction, registerNumber + 1, RegisterType.DOUBLE_HI_TYPE
            )
        }
    }

    private fun propagateRegisterToSuccessors(
        instruction: AnalyzedInstruction,
        registerNumber: Int,
        changedInstructions: BitSet,
        override: Boolean
    ) {
        val postRegisterType = instruction.getPostInstructionRegisterType(registerNumber)
        for (successor in instruction.successors) {
            if (successor.mergeRegister(registerNumber, postRegisterType, analyzedState, override)) {
                changedInstructions.set(successor.instructionIndex)
            }
        }
    }

    private fun buildInstructionList() {
        val registerCount = methodImpl.registerCount

        val instructions: List<Instruction> = Collections.unmodifiableList(
            IteratorUtils.toList(methodImpl.instructions)
        )

        analyzedInstructionsByAddress.ensureCapacity(instructions.size)

        //first, create all the instructions and populate the instructionAddresses array
        var currentCodeAddress = 0
        for (i in instructions.indices) {
            val instruction = instructions[i]
            analyzedInstructionsByAddress.append(
                currentCodeAddress,
                AnalyzedInstruction(this, instruction, i, registerCount)
            )
            assert(analyzedInstructionsByAddress.indexOfKey(currentCodeAddress) == i)
            currentCodeAddress += instruction.codeUnits
        }

        //next, populate the exceptionHandlers array. The array item for each instruction that can throw an exception
        //and is covered by a try block should be set to a list of the first instructions of each exception handler
        //for the try block covering the instruction
        var tries: List<TryBlock<out ExceptionHandler>> = methodImpl.tryBlocks
        tries = TryListBuilder.massageTryBlocks(tries)
        var triesIndex = 0
        var currentTry: TryBlock<out ExceptionHandler>? = null
        var currentExceptionHandlers: Array<AnalyzedInstruction?>? = null
        val exceptionHandlers = arrayOfNulls<Array<AnalyzedInstruction?>>(instructions.size)

        for (i in 0 until analyzedInstructionsByAddress.size()) {
            val instruction = analyzedInstructionsByAddress.valueAt(i)
            val instructionOpcode = instruction.instruction.opcode
            currentCodeAddress = getInstructionAddress(instruction)

            //check if we have gone past the end of the current try
            if (currentTry != null) {
                if (currentTry.startCodeAddress + currentTry.codeUnitCount <= currentCodeAddress) {
                    currentTry = null
                    triesIndex++
                }
            }

            //check if the next try is applicable yet
            if (currentTry == null && triesIndex < tries.size) {
                val tryBlock = tries[triesIndex]
                if (tryBlock.startCodeAddress <= currentCodeAddress) {
                    assert(tryBlock.startCodeAddress + tryBlock.codeUnitCount > currentCodeAddress)

                    currentTry = tryBlock

                    currentExceptionHandlers = buildExceptionHandlerArray(tryBlock)
                }
            }

            //if we're inside a try block, and the instruction can throw an exception, then add the exception handlers
            //for the current instruction
            if (currentTry != null && instructionOpcode.canThrow()) {
                exceptionHandlers[i] = currentExceptionHandlers
            }
        }

        //finally, populate the successors and predecessors for each instruction. We start at the fake "StartOfMethod"
        //instruction and follow the execution path. Any unreachable code won't have any predecessors or successors,
        //and no reachable code will have an unreachable predessor or successor
        assert(analyzedInstructionsByAddress.size() > 0)
        val instructionsToProcess = BitSet(instructions.size)

        addPredecessorSuccessor(
            startOfMethod, analyzedInstructionsByAddress.valueAt(0), exceptionHandlers, instructionsToProcess
        )
        while (!instructionsToProcess.isEmpty()) {
            val currentInstructionIndex = instructionsToProcess.nextSetBit(0)
            instructionsToProcess.clear(currentInstructionIndex)

            val instruction = analyzedInstructionsByAddress.valueAt(currentInstructionIndex)
            val instructionOpcode = instruction.instruction.opcode
            val instructionCodeAddress = getInstructionAddress(instruction)

            if (instruction.instruction.opcode.canContinue()) {
                if (currentInstructionIndex == analyzedInstructionsByAddress.size() - 1) {
                    throw AnalysisException("Execution can continue past the last instruction")
                }

                val nextInstruction = analyzedInstructionsByAddress.valueAt(currentInstructionIndex + 1)
                addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers, instructionsToProcess)
            }

            if (instruction.instruction is OffsetInstruction) {
                val offsetInstruction = instruction.instruction as OffsetInstruction

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    val analyzedSwitchPayload = analyzedInstructionsByAddress.get(
                        instructionCodeAddress + offsetInstruction.codeOffset
                    )
                    if (analyzedSwitchPayload == null) {
                        throw AnalysisException("Invalid switch payload offset")
                    }
                    val switchPayload = analyzedSwitchPayload.instruction as SwitchPayload

                    for (switchElement in switchPayload.switchElements) {
                        val targetInstruction = analyzedInstructionsByAddress.get(
                            instructionCodeAddress + switchElement.offset
                        )
                        if (targetInstruction == null) {
                            throw AnalysisException("Invalid switch target offset")
                        }

                        addPredecessorSuccessor(
                            instruction, targetInstruction, exceptionHandlers,
                            instructionsToProcess
                        )
                    }
                } else if (instructionOpcode != Opcode.FILL_ARRAY_DATA) {
                    val targetAddressOffset = offsetInstruction.codeOffset
                    val targetInstruction = analyzedInstructionsByAddress.get(instructionCodeAddress + targetAddressOffset)!!
                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers, instructionsToProcess)
                }
            }
        }
    }

    private fun addPredecessorSuccessor(
        predecessor: AnalyzedInstruction,
        successor: AnalyzedInstruction,
        exceptionHandlers: Array<Array<AnalyzedInstruction?>?>,
        instructionsToProcess: BitSet
    ) {
        addPredecessorSuccessor(predecessor, successor, exceptionHandlers, instructionsToProcess, false)
    }

    private fun addPredecessorSuccessor(
        predecessor: AnalyzedInstruction,
        successor: AnalyzedInstruction,
        exceptionHandlers: Array<Array<AnalyzedInstruction?>?>,
        instructionsToProcess: BitSet,
        allowMoveException: Boolean
    ) {
        if (!allowMoveException && successor.instruction.opcode == Opcode.MOVE_EXCEPTION) {
            throw AnalysisException(
                "Execution can pass from the " + predecessor.instruction.opcode.mnemonic +
                        " instruction at code address 0x" + Integer.toHexString(getInstructionAddress(predecessor)) +
                        " to the move-exception instruction at address 0x" +
                        Integer.toHexString(getInstructionAddress(successor))
            )
        }

        if (!successor.addPredecessor(predecessor)) {
            return
        }

        predecessor.addSuccessor(successor)
        instructionsToProcess.set(successor.instructionIndex)


        //if the successor can throw an instruction, then we need to add the exception handlers as additional
        //successors to the predecessor (and then apply this same logic recursively if needed)
        //Technically, we should handle the monitor-exit instruction as a special case. The exception is actually
        //thrown *after* the instruction executes, instead of "before" the instruction executes, lke for any other
        //instruction. But since it doesn't modify any registers, we can treat it like any other instruction.
        val exceptionHandlersForSuccessor = exceptionHandlers[successor.instructionIndex]
        if (exceptionHandlersForSuccessor != null) {
            //the item for this instruction in exceptionHandlersForSuccessor should only be set if this instruction
            //can throw an exception
            assert(successor.instruction.opcode.canThrow())

            for (exceptionHandler in exceptionHandlersForSuccessor) {
                addPredecessorSuccessor(
                    predecessor, exceptionHandler!!, exceptionHandlers, instructionsToProcess, true
                )
            }
        }
    }

    private fun buildExceptionHandlerArray(tryBlock: TryBlock<out ExceptionHandler>): Array<AnalyzedInstruction?> {
        val exceptionHandlers = tryBlock.exceptionHandlers

        val handlerInstructions = arrayOfNulls<AnalyzedInstruction>(exceptionHandlers.size)
        for (i in exceptionHandlers.indices) {
            handlerInstructions[i] = analyzedInstructionsByAddress.get(exceptionHandlers[i].handlerCodeAddress)
        }

        return handlerInstructions
    }

    /**
     * @return false if analyzedInstruction is an odex instruction that couldn't be deodexed, due to its
     * object register being null
     */
    private fun analyzeInstruction(analyzedInstruction: AnalyzedInstruction): Boolean {
        val instruction = analyzedInstruction.instruction

        when (instruction.opcode) {
            Opcode.NOP -> return true
            Opcode.MOVE,
            Opcode.MOVE_FROM16,
            Opcode.MOVE_16,
            Opcode.MOVE_WIDE,
            Opcode.MOVE_WIDE_FROM16,
            Opcode.MOVE_WIDE_16,
            Opcode.MOVE_OBJECT,
            Opcode.MOVE_OBJECT_FROM16,
            Opcode.MOVE_OBJECT_16 -> {
                analyzeMove(analyzedInstruction)
                return true
            }
            Opcode.MOVE_RESULT,
            Opcode.MOVE_RESULT_WIDE,
            Opcode.MOVE_RESULT_OBJECT -> {
                analyzeMoveResult(analyzedInstruction)
                return true
            }
            Opcode.MOVE_EXCEPTION -> {
                analyzeMoveException(analyzedInstruction)
                return true
            }
            Opcode.RETURN_VOID,
            Opcode.RETURN,
            Opcode.RETURN_WIDE,
            Opcode.RETURN_OBJECT -> return true
            Opcode.RETURN_VOID_BARRIER,
            Opcode.RETURN_VOID_NO_BARRIER -> {
                analyzeOdexReturnVoid(analyzedInstruction)
                return true
            }
            Opcode.CONST_4,
            Opcode.CONST_16,
            Opcode.CONST,
            Opcode.CONST_HIGH16 -> {
                analyzeConst(analyzedInstruction)
                return true
            }
            Opcode.CONST_WIDE_16,
            Opcode.CONST_WIDE_32,
            Opcode.CONST_WIDE,
            Opcode.CONST_WIDE_HIGH16 -> {
                analyzeWideConst(analyzedInstruction)
                return true
            }
            Opcode.CONST_STRING,
            Opcode.CONST_STRING_JUMBO -> {
                analyzeConstString(analyzedInstruction)
                return true
            }
            Opcode.CONST_CLASS -> {
                analyzeConstClass(analyzedInstruction)
                return true
            }
            Opcode.MONITOR_ENTER,
            Opcode.MONITOR_EXIT -> return true
            Opcode.CHECK_CAST -> {
                analyzeCheckCast(analyzedInstruction)
                return true
            }
            Opcode.INSTANCE_OF -> {
                analyzeInstanceOf(analyzedInstruction)
                return true
            }
            Opcode.ARRAY_LENGTH -> {
                analyzeArrayLength(analyzedInstruction)
                return true
            }
            Opcode.NEW_INSTANCE -> {
                analyzeNewInstance(analyzedInstruction)
                return true
            }
            Opcode.NEW_ARRAY -> {
                analyzeNewArray(analyzedInstruction)
                return true
            }
            Opcode.FILLED_NEW_ARRAY,
            Opcode.FILLED_NEW_ARRAY_RANGE -> return true
            Opcode.FILL_ARRAY_DATA -> return true
            Opcode.THROW,
            Opcode.GOTO,
            Opcode.GOTO_16,
            Opcode.GOTO_32 -> return true
            Opcode.PACKED_SWITCH,
            Opcode.SPARSE_SWITCH -> return true
            Opcode.CMPL_FLOAT,
            Opcode.CMPG_FLOAT,
            Opcode.CMPL_DOUBLE,
            Opcode.CMPG_DOUBLE,
            Opcode.CMP_LONG -> {
                analyzeFloatWideCmp(analyzedInstruction)
                return true
            }
            Opcode.IF_EQ,
            Opcode.IF_NE,
            Opcode.IF_LT,
            Opcode.IF_GE,
            Opcode.IF_GT,
            Opcode.IF_LE,
            Opcode.IF_LTZ,
            Opcode.IF_GEZ,
            Opcode.IF_GTZ,
            Opcode.IF_LEZ -> return true
            Opcode.IF_EQZ,
            Opcode.IF_NEZ -> {
                analyzeIfEqzNez(analyzedInstruction)
                return true
            }
            Opcode.AGET -> {
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.AGET_BOOLEAN -> {
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.BOOLEAN_TYPE)
                return true
            }
            Opcode.AGET_BYTE -> {
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.BYTE_TYPE)
                return true
            }
            Opcode.AGET_CHAR -> {
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.CHAR_TYPE)
                return true
            }
            Opcode.AGET_SHORT -> {
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.SHORT_TYPE)
                return true
            }
            Opcode.AGET_WIDE -> {
                analyzeAgetWide(analyzedInstruction)
                return true
            }
            Opcode.AGET_OBJECT -> {
                analyzeAgetObject(analyzedInstruction)
                return true
            }
            Opcode.APUT,
            Opcode.APUT_BOOLEAN,
            Opcode.APUT_BYTE,
            Opcode.APUT_CHAR,
            Opcode.APUT_SHORT,
            Opcode.APUT_WIDE,
            Opcode.APUT_OBJECT -> return true
            Opcode.IGET -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.IGET_BOOLEAN -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BOOLEAN_TYPE)
                return true
            }
            Opcode.IGET_BYTE -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BYTE_TYPE)
                return true
            }
            Opcode.IGET_CHAR -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.CHAR_TYPE)
                return true
            }
            Opcode.IGET_SHORT -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.SHORT_TYPE)
                return true
            }
            Opcode.IGET_WIDE,
            Opcode.IGET_OBJECT -> {
                analyzeIgetSgetWideObject(analyzedInstruction)
                return true
            }
            Opcode.IPUT,
            Opcode.IPUT_BOOLEAN,
            Opcode.IPUT_BYTE,
            Opcode.IPUT_CHAR,
            Opcode.IPUT_SHORT,
            Opcode.IPUT_WIDE,
            Opcode.IPUT_OBJECT -> return true
            Opcode.SGET -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.SGET_BOOLEAN -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BOOLEAN_TYPE)
                return true
            }
            Opcode.SGET_BYTE -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BYTE_TYPE)
                return true
            }
            Opcode.SGET_CHAR -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.CHAR_TYPE)
                return true
            }
            Opcode.SGET_SHORT -> {
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.SHORT_TYPE)
                return true
            }
            Opcode.SGET_WIDE,
            Opcode.SGET_OBJECT -> {
                analyzeIgetSgetWideObject(analyzedInstruction)
                return true
            }
            Opcode.SPUT,
            Opcode.SPUT_BOOLEAN,
            Opcode.SPUT_BYTE,
            Opcode.SPUT_CHAR,
            Opcode.SPUT_SHORT,
            Opcode.SPUT_WIDE,
            Opcode.SPUT_OBJECT -> return true
            Opcode.INVOKE_VIRTUAL -> {
                analyzeInvokeVirtual(analyzedInstruction, false)
                return true
            }
            Opcode.INVOKE_SUPER -> {
                analyzeInvokeVirtual(analyzedInstruction, false)
                return true
            }
            Opcode.INVOKE_DIRECT -> {
                analyzeInvokeDirect(analyzedInstruction)
                return true
            }
            Opcode.INVOKE_STATIC -> return true
            Opcode.INVOKE_INTERFACE -> // TODO: normalize the interface reference before analyzing this invoke-interface.
                return true
            Opcode.INVOKE_VIRTUAL_RANGE -> {
                analyzeInvokeVirtual(analyzedInstruction, true)
                return true
            }
            Opcode.INVOKE_SUPER_RANGE -> {
                analyzeInvokeVirtual(analyzedInstruction, true)
                return true
            }
            Opcode.INVOKE_DIRECT_RANGE -> {
                analyzeInvokeDirectRange(analyzedInstruction)
                return true
            }
            Opcode.INVOKE_STATIC_RANGE -> return true
            Opcode.INVOKE_INTERFACE_RANGE -> // TODO: normalize the interface reference before analyzing this invoke-interface-range.
                return true
            Opcode.NEG_INT,
            Opcode.NOT_INT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.NEG_LONG,
            Opcode.NOT_LONG -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE)
                return true
            }
            Opcode.NEG_FLOAT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE)
                return true
            }
            Opcode.NEG_DOUBLE -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE)
                return true
            }
            Opcode.INT_TO_LONG -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE)
                return true
            }
            Opcode.INT_TO_FLOAT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE)
                return true
            }
            Opcode.INT_TO_DOUBLE -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE)
                return true
            }
            Opcode.LONG_TO_INT,
            Opcode.DOUBLE_TO_INT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.LONG_TO_FLOAT,
            Opcode.DOUBLE_TO_FLOAT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE)
                return true
            }
            Opcode.LONG_TO_DOUBLE -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE)
                return true
            }
            Opcode.FLOAT_TO_INT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE)
                return true
            }
            Opcode.FLOAT_TO_LONG -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE)
                return true
            }
            Opcode.FLOAT_TO_DOUBLE -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE)
                return true
            }
            Opcode.DOUBLE_TO_LONG -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE)
                return true
            }
            Opcode.INT_TO_BYTE -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.BYTE_TYPE)
                return true
            }
            Opcode.INT_TO_CHAR -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.CHAR_TYPE)
                return true
            }
            Opcode.INT_TO_SHORT -> {
                analyzeUnaryOp(analyzedInstruction, RegisterType.SHORT_TYPE)
                return true
            }
            Opcode.ADD_INT,
            Opcode.SUB_INT,
            Opcode.MUL_INT,
            Opcode.DIV_INT,
            Opcode.REM_INT,
            Opcode.SHL_INT,
            Opcode.SHR_INT,
            Opcode.USHR_INT -> {
                analyzeBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false)
                return true
            }
            Opcode.AND_INT,
            Opcode.OR_INT,
            Opcode.XOR_INT -> {
                analyzeBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true)
                return true
            }
            Opcode.ADD_LONG,
            Opcode.SUB_LONG,
            Opcode.MUL_LONG,
            Opcode.DIV_LONG,
            Opcode.REM_LONG,
            Opcode.AND_LONG,
            Opcode.OR_LONG,
            Opcode.XOR_LONG,
            Opcode.SHL_LONG,
            Opcode.SHR_LONG,
            Opcode.USHR_LONG -> {
                analyzeBinaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE, false)
                return true
            }
            Opcode.ADD_FLOAT,
            Opcode.SUB_FLOAT,
            Opcode.MUL_FLOAT,
            Opcode.DIV_FLOAT,
            Opcode.REM_FLOAT -> {
                analyzeBinaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE, false)
                return true
            }
            Opcode.ADD_DOUBLE,
            Opcode.SUB_DOUBLE,
            Opcode.MUL_DOUBLE,
            Opcode.DIV_DOUBLE,
            Opcode.REM_DOUBLE -> {
                analyzeBinaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE, false)
                return true
            }
            Opcode.ADD_INT_2ADDR,
            Opcode.SUB_INT_2ADDR,
            Opcode.MUL_INT_2ADDR,
            Opcode.DIV_INT_2ADDR,
            Opcode.REM_INT_2ADDR,
            Opcode.SHL_INT_2ADDR,
            Opcode.SHR_INT_2ADDR,
            Opcode.USHR_INT_2ADDR -> {
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false)
                return true
            }
            Opcode.AND_INT_2ADDR,
            Opcode.OR_INT_2ADDR,
            Opcode.XOR_INT_2ADDR -> {
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true)
                return true
            }
            Opcode.ADD_LONG_2ADDR,
            Opcode.SUB_LONG_2ADDR,
            Opcode.MUL_LONG_2ADDR,
            Opcode.DIV_LONG_2ADDR,
            Opcode.REM_LONG_2ADDR,
            Opcode.AND_LONG_2ADDR,
            Opcode.OR_LONG_2ADDR,
            Opcode.XOR_LONG_2ADDR,
            Opcode.SHL_LONG_2ADDR,
            Opcode.SHR_LONG_2ADDR,
            Opcode.USHR_LONG_2ADDR -> {
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.LONG_LO_TYPE, false)
                return true
            }
            Opcode.ADD_FLOAT_2ADDR,
            Opcode.SUB_FLOAT_2ADDR,
            Opcode.MUL_FLOAT_2ADDR,
            Opcode.DIV_FLOAT_2ADDR,
            Opcode.REM_FLOAT_2ADDR -> {
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.FLOAT_TYPE, false)
                return true
            }
            Opcode.ADD_DOUBLE_2ADDR,
            Opcode.SUB_DOUBLE_2ADDR,
            Opcode.MUL_DOUBLE_2ADDR,
            Opcode.DIV_DOUBLE_2ADDR,
            Opcode.REM_DOUBLE_2ADDR -> {
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE, false)
                return true
            }
            Opcode.ADD_INT_LIT16,
            Opcode.RSUB_INT,
            Opcode.MUL_INT_LIT16,
            Opcode.DIV_INT_LIT16,
            Opcode.REM_INT_LIT16 -> {
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false)
                return true
            }
            Opcode.AND_INT_LIT16,
            Opcode.OR_INT_LIT16,
            Opcode.XOR_INT_LIT16 -> {
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true)
                return true
            }
            Opcode.ADD_INT_LIT8,
            Opcode.RSUB_INT_LIT8,
            Opcode.MUL_INT_LIT8,
            Opcode.DIV_INT_LIT8,
            Opcode.REM_INT_LIT8,
            Opcode.SHL_INT_LIT8 -> {
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false)
                return true
            }
            Opcode.AND_INT_LIT8,
            Opcode.OR_INT_LIT8,
            Opcode.XOR_INT_LIT8 -> {
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true)
                return true
            }
            Opcode.SHR_INT_LIT8 -> {
                analyzeLiteralBinaryOp(
                    analyzedInstruction, getDestTypeForLiteralShiftRight(analyzedInstruction, true),
                    false
                )
                return true
            }
            Opcode.USHR_INT_LIT8 -> {
                analyzeLiteralBinaryOp(
                    analyzedInstruction, getDestTypeForLiteralShiftRight(analyzedInstruction, false),
                    false
                )
                return true
            }

            /*odexed instructions*/
            Opcode.IGET_VOLATILE,
            Opcode.IPUT_VOLATILE,
            Opcode.SGET_VOLATILE,
            Opcode.SPUT_VOLATILE,
            Opcode.IGET_OBJECT_VOLATILE,
            Opcode.IGET_WIDE_VOLATILE,
            Opcode.IPUT_WIDE_VOLATILE,
            Opcode.SGET_WIDE_VOLATILE,
            Opcode.SPUT_WIDE_VOLATILE -> {
                analyzePutGetVolatile(analyzedInstruction)
                return true
            }
            Opcode.THROW_VERIFICATION_ERROR -> return true
            Opcode.EXECUTE_INLINE -> {
                analyzeExecuteInline(analyzedInstruction)
                return true
            }
            Opcode.EXECUTE_INLINE_RANGE -> {
                analyzeExecuteInlineRange(analyzedInstruction)
                return true
            }
            Opcode.INVOKE_DIRECT_EMPTY -> {
                analyzeInvokeDirectEmpty(analyzedInstruction)
                return true
            }
            Opcode.INVOKE_OBJECT_INIT_RANGE -> {
                analyzeInvokeObjectInitRange(analyzedInstruction)
                return true
            }
            Opcode.IGET_QUICK,
            Opcode.IGET_WIDE_QUICK,
            Opcode.IGET_OBJECT_QUICK,
            Opcode.IPUT_QUICK,
            Opcode.IPUT_WIDE_QUICK,
            Opcode.IPUT_OBJECT_QUICK,
            Opcode.IPUT_BOOLEAN_QUICK,
            Opcode.IPUT_BYTE_QUICK,
            Opcode.IPUT_CHAR_QUICK,
            Opcode.IPUT_SHORT_QUICK,
            Opcode.IGET_BOOLEAN_QUICK,
            Opcode.IGET_BYTE_QUICK,
            Opcode.IGET_CHAR_QUICK,
            Opcode.IGET_SHORT_QUICK -> return analyzeIputIgetQuick(analyzedInstruction)
            Opcode.INVOKE_VIRTUAL_QUICK -> return analyzeInvokeVirtualQuick(analyzedInstruction, false, false)
            Opcode.INVOKE_SUPER_QUICK -> return analyzeInvokeVirtualQuick(analyzedInstruction, true, false)
            Opcode.INVOKE_VIRTUAL_QUICK_RANGE -> return analyzeInvokeVirtualQuick(analyzedInstruction, false, true)
            Opcode.INVOKE_SUPER_QUICK_RANGE -> return analyzeInvokeVirtualQuick(analyzedInstruction, true, true)
            Opcode.IPUT_OBJECT_VOLATILE,
            Opcode.SGET_OBJECT_VOLATILE,
            Opcode.SPUT_OBJECT_VOLATILE -> {
                analyzePutGetVolatile(analyzedInstruction)
                return true
            }
            else -> {
                assert(false)
                return true
            }
        }
    }

    companion object {
        private val Primitive32BitCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL.toInt(),
            RegisterType.ONE.toInt(),
            RegisterType.BOOLEAN.toInt(),
            RegisterType.BYTE.toInt(),
            RegisterType.POS_BYTE.toInt(),
            RegisterType.SHORT.toInt(),
            RegisterType.POS_SHORT.toInt(),
            RegisterType.CHAR.toInt(),
            RegisterType.INTEGER.toInt(),
            RegisterType.FLOAT.toInt()
        )

        private val WideLowCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.LONG_LO.toInt(),
            RegisterType.DOUBLE_LO.toInt()
        )

        private val WideHighCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.LONG_HI.toInt(),
            RegisterType.DOUBLE_HI.toInt()
        )

        private val ReferenceOrUninitCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL.toInt(),
            RegisterType.UNINIT_REF.toInt(),
            RegisterType.UNINIT_THIS.toInt(),
            RegisterType.REFERENCE.toInt()
        )

        private val BooleanCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL.toInt(),
            RegisterType.ONE.toInt(),
            RegisterType.BOOLEAN.toInt()
        )

        fun isNotWideningConversion(originalType: RegisterType, newType: RegisterType): Boolean {
            if (originalType.type == null || newType.type == null) {
                return true
            }
            if (originalType.type.isInterface()) {
                return newType.type.implementsInterface(originalType.type.type)
            } else {
                val commonSuperclass = newType.type.getCommonSuperclass(originalType.type)
                if (commonSuperclass.type == originalType.type.type) {
                    return true
                }
                if (commonSuperclass.type == newType.type.type) {
                    return false
                }
            }
            return true
        }

        fun canPropagateTypeAfterInstanceOf(
            analyzedInstanceOfInstruction: AnalyzedInstruction,
            analyzedIfInstruction: AnalyzedInstruction,
            classPath: ClassPath
        ): Boolean {
            if (!classPath.isArt) {
                return false
            }

            val ifInstruction = analyzedIfInstruction.instruction
            if ((ifInstruction as Instruction21t).registerA == analyzedInstanceOfInstruction.destinationRegister) {
                val reference: Reference =
                    (analyzedInstanceOfInstruction.instruction as Instruction22c).reference
                val registerType = RegisterType.getRegisterType(classPath, reference as TypeReference)

                try {
                    if (registerType.type != null && !registerType.type.isInterface()) {
                        val objectRegister =
                            (analyzedInstanceOfInstruction.instruction as TwoRegisterInstruction).registerB

                        val originalType = analyzedIfInstruction.getPreInstructionRegisterType(objectRegister)

                        return isNotWideningConversion(originalType, registerType)
                    }
                } catch (ex: UnresolvedClassException) {
                    return false
                }
            }
            return false
        }

        private fun getAndCheckSourceRegister(
            analyzedInstruction: AnalyzedInstruction,
            registerNumber: Int,
            validCategories: BitSet
        ): RegisterType {
            assert(registerNumber >= 0 && registerNumber < analyzedInstruction.postRegisterMap.size)

            val registerType = analyzedInstruction.getPreInstructionRegisterType(registerNumber)

            checkRegister(registerType, registerNumber, validCategories)

            if (validCategories === WideLowCategories) {
                checkRegister(registerType, registerNumber, WideLowCategories)
                checkWidePair(registerNumber, analyzedInstruction)

                val secondRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNumber + 1)
                checkRegister(secondRegisterType, registerNumber + 1, WideHighCategories)
            }

            return registerType
        }

        private fun checkRegister(registerType: RegisterType, registerNumber: Int, validCategories: BitSet) {
            if (!validCategories.get(registerType.category.toInt())) {
                throw AnalysisException(
                    "Invalid register type ${registerType.toString()} for register v${registerNumber}."
                )
            }
        }

        private fun checkWidePair(registerNumber: Int, analyzedInstruction: AnalyzedInstruction) {
            if (registerNumber + 1 >= analyzedInstruction.postRegisterMap.size) {
                throw AnalysisException(
                    "v${registerNumber} cannot be used as the first register in a wide registerpair because it is the last register."
                )
            }
        }
    }

    private fun analyzeMove(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as TwoRegisterInstruction

        val sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType)
    }

    private fun analyzeMoveResult(analyzedInstruction: AnalyzedInstruction) {
        var previousInstruction: AnalyzedInstruction? = null
        if (analyzedInstruction.instructionIndex > 0) {
            previousInstruction = analyzedInstructionsByAddress.valueAt(analyzedInstruction.instructionIndex - 1)
        }
        if (previousInstruction == null || !previousInstruction.instruction.opcode.setsResult()) {
            throw AnalysisException(
                analyzedInstruction.instruction.opcode.mnemonic + " must occur after an " +
                        "invoke-*/fill-new-array instruction"
            )
        }

        val resultRegisterType: RegisterType
        val invokeInstruction = previousInstruction.instruction as ReferenceInstruction
        val reference = invokeInstruction.reference

        if (reference is MethodReference) {
            resultRegisterType = RegisterType.getRegisterType(classPath, reference.returnType)
        } else {
            resultRegisterType = RegisterType.getRegisterType(classPath, reference as TypeReference)
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, resultRegisterType)
    }

    private fun analyzeMoveException(analyzedInstruction: AnalyzedInstruction) {
        val instructionAddress = getInstructionAddress(analyzedInstruction)

        var exceptionType = RegisterType.UNKNOWN_TYPE

        for (tryBlock in methodImpl.tryBlocks) {
            for (handler in tryBlock.exceptionHandlers) {

                if (handler.handlerCodeAddress == instructionAddress) {
                    val type = handler.exceptionType
                    if (type == null) {
                        exceptionType = RegisterType.getRegisterType(
                            RegisterType.REFERENCE,
                            classPath.getClass("Ljava/lang/Throwable;")
                        )
                    } else {
                        exceptionType = RegisterType.getRegisterType(RegisterType.REFERENCE, classPath.getClass(type))
                            .merge(exceptionType)
                    }
                }
            }
        }

        if (exceptionType.category == RegisterType.UNKNOWN) {
            throw AnalysisException("move-exception must be the first instruction in an exception handler block")
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, exceptionType)
    }

    private fun analyzeOdexReturnVoid(analyzedInstruction: AnalyzedInstruction) {
        analyzeOdexReturnVoid(analyzedInstruction, true)
    }

    private fun analyzeOdexReturnVoid(analyzedInstruction: AnalyzedInstruction, analyzeResult: Boolean) {
        val deodexedInstruction = ImmutableInstruction10x(Opcode.RETURN_VOID)

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction)
        }
    }

    private fun analyzeConst(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as NarrowLiteralInstruction

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(
            analyzedInstruction,
            RegisterType.getRegisterTypeForLiteral(instruction.narrowLiteral)
        )
    }

    private fun analyzeWideConst(analyzedInstruction: AnalyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE)
    }

    private fun analyzeConstString(analyzedInstruction: AnalyzedInstruction) {
        val stringClass = classPath.getClass("Ljava/lang/String;")
        val stringType = RegisterType.getRegisterType(RegisterType.REFERENCE, stringClass)
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, stringType)
    }

    private fun analyzeConstClass(analyzedInstruction: AnalyzedInstruction) {
        val classClass = classPath.getClass("Ljava/lang/Class;")
        val classType = RegisterType.getRegisterType(RegisterType.REFERENCE, classClass)
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, classType)
    }

    private fun analyzeCheckCast(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as ReferenceInstruction
        val reference = instruction.reference as TypeReference
        val castRegisterType = RegisterType.getRegisterType(classPath, reference)
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, castRegisterType)
    }

    /**
     * Art uses a peephole optimization for an if-eqz or if-nez that occur immediately after an instance-of. It will
     * narrow the type if possible, and then NOP out any corresponding check-cast instruction later on
     */
    private fun analyzeIfEqzNez(analyzedInstruction: AnalyzedInstruction) {
        if (classPath.isArt) {
            val instructionIndex = analyzedInstruction.instructionIndex
            if (instructionIndex > 0) {
                if (analyzedInstruction.predecessorCount != 1) {
                    return
                }
                val prevAnalyzedInstruction = analyzedInstruction.getPredecessors().first()
                if (prevAnalyzedInstruction.instruction.opcode == Opcode.INSTANCE_OF) {

                    val fallthroughInstruction = analyzedInstructionsByAddress.valueAt(
                        analyzedInstruction.instructionIndex + 1
                    )

                    val nextAddress = getInstructionAddress(analyzedInstruction) +
                            (analyzedInstruction.instruction as Instruction21t).codeOffset
                    val branchInstruction = analyzedInstructionsByAddress.get(nextAddress)!!

                    val narrowingRegister = (prevAnalyzedInstruction.instruction as Instruction22c).registerB
                    val originalType = analyzedInstruction.getPreInstructionRegisterType(narrowingRegister)

                    val instanceOfInstruction = prevAnalyzedInstruction.instruction as Instruction22c
                    val newType = RegisterType.getRegisterType(
                        classPath,
                        instanceOfInstruction.reference as TypeReference
                    )

                    for (register in analyzedInstruction.setRegisters) {
                        if (analyzedInstruction.instruction.opcode == Opcode.IF_EQZ) {
                            overridePredecessorRegisterTypeAndPropagateChanges(
                                fallthroughInstruction,
                                analyzedInstruction, register, newType
                            )
                            overridePredecessorRegisterTypeAndPropagateChanges(
                                branchInstruction, analyzedInstruction,
                                register, originalType
                            )
                        } else {
                            overridePredecessorRegisterTypeAndPropagateChanges(
                                fallthroughInstruction,
                                analyzedInstruction, register, originalType
                            )
                            overridePredecessorRegisterTypeAndPropagateChanges(
                                branchInstruction, analyzedInstruction,
                                register, newType
                            )
                        }
                    }
                }
            }
        }
    }

    private fun analyzeInstanceOf(analyzedInstruction: AnalyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.BOOLEAN_TYPE)
    }

    private fun analyzeArrayLength(analyzedInstruction: AnalyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.INTEGER_TYPE)
    }

    private fun analyzeNewInstance(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as ReferenceInstruction

        val register = (analyzedInstruction.instruction as OneRegisterInstruction).registerA
        val destRegisterType = analyzedInstruction.getPostInstructionRegisterType(register)
        if (destRegisterType.category != RegisterType.UNKNOWN) {
            //the post-instruction destination register will only be set if we have already analyzed this instruction
            //at least once. If this is the case, then the uninit reference has already been propagated to all
            //successors and nothing else needs to be done.
            assert(destRegisterType.category == RegisterType.UNINIT_REF)
            return
        }

        val typeReference = instruction.reference as TypeReference

        val classType = RegisterType.getRegisterType(classPath, typeReference)

        setDestinationRegisterTypeAndPropagateChanges(
            analyzedInstruction,
            RegisterType.getRegisterType(RegisterType.UNINIT_REF, classType.type)
        )
    }

    private fun analyzeNewArray(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as ReferenceInstruction

        val type = instruction.reference as TypeReference
        if (type.type[0] != '[') {
            throw AnalysisException("new-array used with non-array type")
        }

        val arrayType = RegisterType.getRegisterType(classPath, type)

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType)
    }

    private fun analyzeFloatWideCmp(analyzedInstruction: AnalyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.BYTE_TYPE)
    }

    private fun analyze32BitPrimitiveAget(
        analyzedInstruction: AnalyzedInstruction,
        registerType: RegisterType
    ) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, registerType)
    }

    private fun analyzeAgetWide(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as ThreeRegisterInstruction

        val arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)
        if (arrayRegisterType.category != RegisterType.NULL) {
            if (arrayRegisterType.category != RegisterType.REFERENCE ||
                arrayRegisterType.type !is ArrayProto
            ) {
                throw AnalysisException("aget-wide used with non-array register: %s", arrayRegisterType.toString())
            }
            val arrayProto = arrayRegisterType.type

            if (arrayProto.dimensions != 1) {
                throw AnalysisException(
                    "aget-wide used with multi-dimensional array: %s",
                    arrayRegisterType.toString()
                )
            }

            val arrayBaseType = arrayProto.elementType[0]
            if (arrayBaseType == 'J') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE)
            } else if (arrayBaseType == 'D') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE)
            } else {
                throw AnalysisException("aget-wide used with narrow array: %s", arrayRegisterType)
            }
        } else {
            // If the array register is null, we can assume that the destination register was meant to be a wide type.
            // This is the same behavior as dalvik's verifier
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE)
        }
    }

    private fun analyzeAgetObject(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as ThreeRegisterInstruction

        val arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)
        if (arrayRegisterType.category != RegisterType.NULL) {
            if (arrayRegisterType.category != RegisterType.REFERENCE ||
                arrayRegisterType.type !is ArrayProto
            ) {
                throw AnalysisException(
                    "aget-object used with non-array register: %s",
                    arrayRegisterType.toString()
                )
            }

            val arrayProto = arrayRegisterType.type

            val elementType = arrayProto.immediateElementType

            setDestinationRegisterTypeAndPropagateChanges(
                analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.REFERENCE, classPath.getClass(elementType))
            )
        } else {
            // If the array register is null, we can assume that the destination register was meant to be a reference
            // type, so we set the destination to NULL. This is the same behavior as dalvik's verifier
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.NULL_TYPE)
        }
    }

    private fun analyze32BitPrimitiveIgetSget(
        analyzedInstruction: AnalyzedInstruction,
        registerType: RegisterType
    ) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, registerType)
    }

    private fun analyzeIgetSgetWideObject(analyzedInstruction: AnalyzedInstruction) {
        val referenceInstruction = analyzedInstruction.instruction as ReferenceInstruction

        val fieldReference = referenceInstruction.reference as FieldReference

        val fieldType = RegisterType.getRegisterType(classPath, fieldReference.type)
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType)
    }

    private fun analyzeInvokeDirect(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as FiveRegisterInstruction
        analyzeInvokeDirectCommon(analyzedInstruction, instruction.registerC)
    }

    private fun analyzeInvokeDirectRange(analyzedInstruction: AnalyzedInstruction) {
        val instruction = analyzedInstruction.instruction as RegisterRangeInstruction
        analyzeInvokeDirectCommon(analyzedInstruction, instruction.startRegister)
    }

    private fun analyzeInvokeDirectCommon(analyzedInstruction: AnalyzedInstruction, objectRegister: Int) {
        // This handles the case of invoking a constructor on an uninitialized reference. This propagates the
        // initialized type for the object register, and also any known aliased registers.
        //
        // In some cases, unrelated uninitialized references may not have been propagated past this instruction. This
        // happens when propagating those types and the type of object register of this instruction isn't known yet.
        // In this case, we can't determine if the uninitialized reference being propagated in an alias of the object
        // register, so we don't stop propagation.
        //
        // We check for any of these unpropagated uninitialized references here and propagate them.
        if (analyzedInstruction.isInvokeInit) {
            val uninitRef = analyzedInstruction.getPreInstructionRegisterType(objectRegister)
            if (uninitRef.category != RegisterType.UNINIT_REF && uninitRef.category != RegisterType.UNINIT_THIS) {
                assert(analyzedInstruction.setRegisters.isEmpty())
                return
            }

            val initRef = RegisterType.getRegisterType(RegisterType.REFERENCE, uninitRef.type)

            for (register in analyzedInstruction.setRegisters) {
                val registerType = analyzedInstruction.getPreInstructionRegisterType(register)

                if (registerType == uninitRef) {
                    setPostRegisterTypeAndPropagateChanges(analyzedInstruction, register, initRef)
                } else {
                    // This is unrelated uninitialized reference. propagate it as-is
                    setPostRegisterTypeAndPropagateChanges(analyzedInstruction, register, registerType)
                }
            }
        }
    }

    private fun analyzeUnaryOp(analyzedInstruction: AnalyzedInstruction, destRegisterType: RegisterType) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType)
    }

    private fun analyzeBinaryOp(
        analyzedInstruction: AnalyzedInstruction,
        destRegisterTypeIn: RegisterType,
        checkForBoolean: Boolean
    ) {
        var destRegisterType = destRegisterTypeIn
        if (checkForBoolean) {
            val instruction = analyzedInstruction.instruction as ThreeRegisterInstruction

            val source1RegisterType =
                analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)
            val source2RegisterType =
                analyzedInstruction.getPreInstructionRegisterType(instruction.registerC)

            if (BooleanCategories.get(source1RegisterType.category.toInt()) &&
                BooleanCategories.get(source2RegisterType.category.toInt())
            ) {
                destRegisterType = RegisterType.BOOLEAN_TYPE
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType)
    }

    private fun analyzeBinary2AddrOp(
        analyzedInstruction: AnalyzedInstruction,
        destRegisterTypeIn: RegisterType,
        checkForBoolean: Boolean
    ) {
        var destRegisterType = destRegisterTypeIn
        if (checkForBoolean) {
            val instruction = analyzedInstruction.instruction as TwoRegisterInstruction

            val source1RegisterType =
                analyzedInstruction.getPreInstructionRegisterType(instruction.registerA)
            val source2RegisterType =
                analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)

            if (BooleanCategories.get(source1RegisterType.category.toInt()) &&
                BooleanCategories.get(source2RegisterType.category.toInt())
            ) {
                destRegisterType = RegisterType.BOOLEAN_TYPE
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType)
    }

    private fun analyzeLiteralBinaryOp(
        analyzedInstruction: AnalyzedInstruction,
        destRegisterTypeIn: RegisterType,
        checkForBoolean: Boolean
    ) {
        var destRegisterType = destRegisterTypeIn
        if (checkForBoolean) {
            val instruction = analyzedInstruction.instruction as TwoRegisterInstruction

            val sourceRegisterType =
                analyzedInstruction.getPreInstructionRegisterType(instruction.registerB)

            if (BooleanCategories.get(sourceRegisterType.category.toInt())) {
                val literal = (analyzedInstruction.instruction as NarrowLiteralInstruction).narrowLiteral
                if (literal == 0 || literal == 1) {
                    destRegisterType = RegisterType.BOOLEAN_TYPE
                }
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType)
    }

    private fun getDestTypeForLiteralShiftRight(
        analyzedInstruction: AnalyzedInstruction,
        signedShift: Boolean
    ): RegisterType {
        val instruction = analyzedInstruction.instruction as TwoRegisterInstruction

        val sourceRegisterType = getAndCheckSourceRegister(
            analyzedInstruction, instruction.registerB,
            Primitive32BitCategories
        )
        var literalShift = (analyzedInstruction.instruction as NarrowLiteralInstruction).narrowLiteral.toLong()

        if (literalShift == 0L) {
            return sourceRegisterType
        }

        val destRegisterType: RegisterType
        if (!signedShift) {
            destRegisterType = RegisterType.INTEGER_TYPE
        } else {
            destRegisterType = sourceRegisterType
        }

        literalShift = literalShift and 0x1fL

        when (sourceRegisterType.category) {
            RegisterType.INTEGER,
            RegisterType.FLOAT -> {
                if (!signedShift) {
                    if (literalShift > 24) {
                        return RegisterType.POS_BYTE_TYPE
                    }
                    if (literalShift >= 16) {
                        return RegisterType.CHAR_TYPE
                    }
                } else {
                    if (literalShift >= 24) {
                        return RegisterType.BYTE_TYPE
                    }
                    if (literalShift >= 16) {
                        return RegisterType.SHORT_TYPE
                    }
                }
            }
            RegisterType.SHORT -> {
                if (signedShift && literalShift >= 8) {
                    return RegisterType.BYTE_TYPE
                }
            }
            RegisterType.POS_SHORT -> {
                if (literalShift >= 8) {
                    return RegisterType.POS_BYTE_TYPE
                }
            }
            RegisterType.CHAR -> {
                if (literalShift > 8) {
                    return RegisterType.POS_BYTE_TYPE
                }
            }
            RegisterType.BYTE -> {}
            RegisterType.POS_BYTE -> return RegisterType.POS_BYTE_TYPE
            RegisterType.NULL,
            RegisterType.ONE,
            RegisterType.BOOLEAN -> return RegisterType.NULL_TYPE
            else -> assert(false)
        }

        return destRegisterType
    }

    private fun analyzeExecuteInline(analyzedInstruction: AnalyzedInstruction) {
        if (inlineResolver == null) {
            throw AnalysisException("Cannot analyze an odexed instruction unless we are deodexing")
        }

        val instruction = analyzedInstruction.instruction as Instruction35mi
        val resolvedMethod = inlineResolver.resolveExecuteInline(analyzedInstruction)

        val deodexedOpcode: Opcode
        val accessFlags = resolvedMethod.accessFlags
        if (AccessFlags.STATIC.isSet(accessFlags)) {
            deodexedOpcode = Opcode.INVOKE_STATIC
        } else if (AccessFlags.PRIVATE.isSet(accessFlags)) {
            deodexedOpcode = Opcode.INVOKE_DIRECT
        } else {
            deodexedOpcode = Opcode.INVOKE_VIRTUAL
        }

        val deodexedInstruction = ImmutableInstruction35c(
            deodexedOpcode, instruction.registerCount,
            instruction.registerC, instruction.registerD, instruction.registerE,
            instruction.registerF, instruction.registerG, resolvedMethod
        )

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)
        analyzeInstruction(analyzedInstruction)
    }

    private fun analyzeExecuteInlineRange(analyzedInstruction: AnalyzedInstruction) {
        if (inlineResolver == null) {
            throw AnalysisException("Cannot analyze an odexed instruction unless we are deodexing")
        }

        val instruction = analyzedInstruction.instruction as Instruction3rmi
        val resolvedMethod = inlineResolver.resolveExecuteInline(analyzedInstruction)

        val deodexedOpcode: Opcode
        val accessFlags = resolvedMethod.accessFlags
        if (AccessFlags.STATIC.isSet(accessFlags)) {
            deodexedOpcode = Opcode.INVOKE_STATIC_RANGE
        } else if (AccessFlags.PRIVATE.isSet(accessFlags)) {
            deodexedOpcode = Opcode.INVOKE_DIRECT_RANGE
        } else {
            deodexedOpcode = Opcode.INVOKE_VIRTUAL_RANGE
        }

        val deodexedInstruction = ImmutableInstruction3rc(
            deodexedOpcode, instruction.startRegister,
            instruction.registerCount, resolvedMethod
        )

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)
        analyzeInstruction(analyzedInstruction)
    }

    private fun analyzeInvokeDirectEmpty(analyzedInstruction: AnalyzedInstruction) {
        analyzeInvokeDirectEmpty(analyzedInstruction, true)
    }

    private fun analyzeInvokeDirectEmpty(analyzedInstruction: AnalyzedInstruction, analyzeResult: Boolean) {
        val instruction = analyzedInstruction.instruction as Instruction35c

        val deodexedInstruction = ImmutableInstruction35c(
            Opcode.INVOKE_DIRECT,
            instruction.registerCount, instruction.registerC, instruction.registerD,
            instruction.registerE, instruction.registerF, instruction.registerG,
            instruction.reference
        )

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction)
        }
    }

    private fun analyzeInvokeObjectInitRange(analyzedInstruction: AnalyzedInstruction) {
        analyzeInvokeObjectInitRange(analyzedInstruction, true)
    }

    private fun analyzeInvokeObjectInitRange(analyzedInstruction: AnalyzedInstruction, analyzeResult: Boolean) {
        val instruction = analyzedInstruction.instruction as Instruction3rc

        val deodexedInstruction: Instruction

        val startRegister = instruction.startRegister
        // hack: we should be using instruction.getRegisterCount, but some tweaked versions of dalvik appear
        // to generate invoke-object-init/range instructions with an invalid register count. We know it should
        // always be 1, so just use that.
        val registerCount = 1
        if (startRegister < 16) {
            deodexedInstruction = ImmutableInstruction35c(
                Opcode.INVOKE_DIRECT,
                registerCount, startRegister, 0, 0, 0, 0, instruction.reference
            )
        } else {
            deodexedInstruction = ImmutableInstruction3rc(
                Opcode.INVOKE_DIRECT_RANGE,
                startRegister, registerCount, instruction.reference
            )
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction)
        }
    }

    private fun analyzeIputIgetQuick(analyzedInstruction: AnalyzedInstruction): Boolean {
        val instruction = analyzedInstruction.instruction as Instruction22cs

        val fieldOffset = instruction.fieldOffset
        val objectRegisterType = getAndCheckSourceRegister(
            analyzedInstruction, instruction.registerB,
            ReferenceOrUninitCategories
        )

        if (objectRegisterType.category == RegisterType.NULL) {
            return false
        }

        val objectRegisterTypeProto = objectRegisterType.type
        assert(objectRegisterTypeProto != null)

        val classTypeProto = classPath.getClass(objectRegisterTypeProto!!.type)
        var resolvedField = classTypeProto.getFieldByOffset(fieldOffset)

        if (resolvedField == null) {
            throw AnalysisException(
                "Could not resolve the field in class %s at offset %d",
                objectRegisterType.type.type, fieldOffset
            )
        }

        val thisClass = classPath.getClassDef(method.definingClass)

        if (!TypeUtils.canAccessClass(thisClass.type, classPath.getClassDef(resolvedField.definingClass))) {

            // the class is not accessible. So we start looking at objectRegisterTypeProto (which may be different
            // than resolvedField.getDefiningClass()), and walk up the class hierarchy.
            var fieldClass = classPath.getClassDef(objectRegisterTypeProto.type)
            while (!TypeUtils.canAccessClass(thisClass.type, fieldClass)) {
                val superclass = fieldClass.superclass
                    ?: throw ExceptionWithContext(
                        "Couldn't find accessible class while resolving field %s",
                        resolvedField
                    )

                fieldClass = classPath.getClassDef(superclass)
            }

            // fieldClass is now the first accessible class found. Now. we need to make sure that the field is
            // actually valid for this class
            val newResolvedField = classPath.getClass(fieldClass.type).getFieldByOffset(fieldOffset)
                ?: throw ExceptionWithContext(
                    "Couldn't find accessible class while resolving field %s",
                    resolvedField
                )
            resolvedField = ImmutableFieldReference(
                fieldClass.type, newResolvedField.name,
                newResolvedField.type
            )
        }

        val fieldType = resolvedField.type

        val opcode = classPath.fieldInstructionMapper.getAndCheckDeodexedOpcode(
            fieldType, instruction.opcode
        )

        val deodexedInstruction = ImmutableInstruction22c(
            opcode, instruction.registerA,
            instruction.registerB, resolvedField
        )
        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)

        analyzeInstruction(analyzedInstruction)

        return true
    }

    private fun analyzeInvokeVirtual(analyzedInstruction: AnalyzedInstruction, isRange: Boolean): Boolean {
        val targetMethod: MethodReference

        if (!normalizeVirtualMethods) {
            return true
        }

        if (isRange) {
            val instruction = analyzedInstruction.instruction as Instruction3rc
            targetMethod = instruction.reference as MethodReference
        } else {
            val instruction = analyzedInstruction.instruction as Instruction35c
            targetMethod = instruction.reference as MethodReference
        }

        val replacementMethod = normalizeMethodReference(targetMethod)

        if (replacementMethod == null || replacementMethod == targetMethod) {
            return true
        }

        val deodexedInstruction: Instruction
        if (isRange) {
            val instruction = analyzedInstruction.instruction as Instruction3rc
            deodexedInstruction = ImmutableInstruction3rc(
                instruction.opcode, instruction.startRegister,
                instruction.registerCount, replacementMethod
            )
        } else {
            val instruction = analyzedInstruction.instruction as Instruction35c
            deodexedInstruction = ImmutableInstruction35c(
                instruction.opcode, instruction.registerCount,
                instruction.registerC, instruction.registerD, instruction.registerE,
                instruction.registerF, instruction.registerG, replacementMethod
            )
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)
        return true
    }

    private fun analyzeInvokeVirtualQuick(
        analyzedInstruction: AnalyzedInstruction,
        isSuper: Boolean,
        isRange: Boolean
    ): Boolean {
        val methodIndex: Int
        val objectRegister: Int

        if (isRange) {
            val instruction = analyzedInstruction.instruction as Instruction3rms
            methodIndex = instruction.vtableIndex
            objectRegister = instruction.startRegister
        } else {
            val instruction = analyzedInstruction.instruction as Instruction35ms
            methodIndex = instruction.vtableIndex
            objectRegister = instruction.registerC
        }

        val objectRegisterType = getAndCheckSourceRegister(
            analyzedInstruction, objectRegister,
            ReferenceOrUninitCategories
        )
        val objectRegisterTypeProto = objectRegisterType.type

        if (objectRegisterType.category == RegisterType.NULL) {
            return false
        }

        assert(objectRegisterTypeProto != null)

        var resolvedMethod: MethodReference
        if (isSuper) {
            // invoke-super is only used for the same class that we're currently in
            val typeProto = classPath.getClass(method.definingClass)
            val superType: TypeProto

            val superclassType = typeProto.superclass
            if (superclassType != null) {
                superType = classPath.getClass(superclassType)
            } else {
                // This is either java.lang.Object, or an UnknownClassProto
                superType = typeProto
            }

            resolvedMethod = superType.getMethodByVtableIndex(methodIndex)
                ?: throw AnalysisException(
                    "Could not resolve the method in class %s at index %d",
                    objectRegisterType.type!!.type, methodIndex
                )
        } else {
            resolvedMethod = objectRegisterTypeProto!!.getMethodByVtableIndex(methodIndex)
                ?: throw AnalysisException(
                    "Could not resolve the method in class %s at index %d",
                    objectRegisterType.type.type, methodIndex
                )
        }

        // no need to check class access for invoke-super. A class can obviously access its superclass.
        val thisClass = classPath.getClassDef(method.definingClass)

        if (classPath.getClass(resolvedMethod.definingClass).isInterface()) {
            resolvedMethod = ReparentedMethodReference(resolvedMethod, objectRegisterTypeProto!!.type)
        } else if (!isSuper && !TypeUtils.canAccessClass(
                thisClass.type, classPath.getClassDef(resolvedMethod.definingClass)
            )
        ) {

            // the class is not accessible. So we start looking at objectRegisterTypeProto (which may be different
            // than resolvedMethod.getDefiningClass()), and walk up the class hierarchy.
            var methodClass = classPath.getClassDef(objectRegisterTypeProto!!.type)
            while (!TypeUtils.canAccessClass(thisClass.type, methodClass)) {
                val superclass = methodClass.superclass
                    ?: throw ExceptionWithContext(
                        "Couldn't find accessible class while resolving method %s",
                        resolvedMethod
                    )

                methodClass = classPath.getClassDef(superclass)
            }

            // methodClass is now the first accessible class found. Now. we need to make sure that the method is
            // actually valid for this class
            val newResolvedMethod = classPath.getClass(methodClass.type).getMethodByVtableIndex(methodIndex)
                ?: throw ExceptionWithContext(
                    "Couldn't find accessible class while resolving method %s",
                    resolvedMethod
                )
            resolvedMethod = newResolvedMethod
            resolvedMethod = ImmutableMethodReference(
                methodClass.type, resolvedMethod.name,
                resolvedMethod.parameterTypes, resolvedMethod.returnType
            )
        }

        if (normalizeVirtualMethods) {
            val replacementMethod = normalizeMethodReference(resolvedMethod)
            if (replacementMethod != null) {
                resolvedMethod = replacementMethod
            }
        }

        val deodexedInstruction: Instruction
        if (isRange) {
            val instruction = analyzedInstruction.instruction as Instruction3rms
            val opcode: Opcode
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER_RANGE
            } else {
                opcode = Opcode.INVOKE_VIRTUAL_RANGE
            }

            deodexedInstruction = ImmutableInstruction3rc(
                opcode, instruction.startRegister,
                instruction.registerCount, resolvedMethod
            )
        } else {
            val instruction = analyzedInstruction.instruction as Instruction35ms
            val opcode: Opcode
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER
            } else {
                opcode = Opcode.INVOKE_VIRTUAL
            }

            deodexedInstruction = ImmutableInstruction35c(
                opcode, instruction.registerCount,
                instruction.registerC, instruction.registerD, instruction.registerE,
                instruction.registerF, instruction.registerG, resolvedMethod
            )
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)
        analyzeInstruction(analyzedInstruction)

        return true
    }

    private fun analyzePutGetVolatile(analyzedInstruction: AnalyzedInstruction): Boolean {
        return analyzePutGetVolatile(analyzedInstruction, true)
    }

    private fun analyzePutGetVolatile(analyzedInstruction: AnalyzedInstruction, analyzeResult: Boolean): Boolean {
        val field = (analyzedInstruction.instruction as ReferenceInstruction).reference as FieldReference
        val fieldType = field.type

        val originalOpcode = analyzedInstruction.instruction.opcode

        val opcode = classPath.fieldInstructionMapper.getAndCheckDeodexedOpcode(
            fieldType, originalOpcode
        )

        val deodexedInstruction: Instruction

        if (originalOpcode.isStaticFieldAccessor) {
            val instruction = analyzedInstruction.instruction as OneRegisterInstruction
            deodexedInstruction = ImmutableInstruction21c(opcode, instruction.registerA, field)
        } else {
            val instruction = analyzedInstruction.instruction as TwoRegisterInstruction

            deodexedInstruction = ImmutableInstruction22c(
                opcode, instruction.registerA,
                instruction.registerB, field
            )
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction)

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction)
        }
        return true
    }

    private fun normalizeMethodReference(methodRef: MethodReference): MethodReference? {
        var typeProto = classPath.getClass(methodRef.definingClass)
        val methodIndex: Int
        try {
            methodIndex = typeProto.findMethodIndexInVtable(methodRef)
        } catch (ex: UnresolvedClassException) {
            return null
        }

        if (methodIndex < 0) {
            return null
        }

        val thisClass = classPath.getClass(method.definingClass) as ClassProto

        var replacementMethod: Method = typeProto.getMethodByVtableIndex(methodIndex)!!
        while (true) {
            val superType = typeProto.superclass
            if (superType == null) {
                break
            }
            typeProto = classPath.getClass(superType)
            val resolvedMethod = typeProto.getMethodByVtableIndex(methodIndex)
            if (resolvedMethod == null) {
                break
            }

            if (resolvedMethod != replacementMethod) {
                if (!AnalyzedMethodUtil.canAccess(thisClass, resolvedMethod, false, false, true)) {
                    continue
                }

                replacementMethod = resolvedMethod
            }
        }
        return replacementMethod
    }

    private class ReparentedMethodReference(
        private val baseReference: MethodReference,
        override val definingClass: String
    ) : BaseMethodReference() {
        override val name: String
            get() = baseReference.name

        override val parameterTypes: List<CharSequence>
            get() = baseReference.parameterTypes

        override val returnType: String
            get() = baseReference.returnType
    }
}
