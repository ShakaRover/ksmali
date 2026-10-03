/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.Adaptors.Debug.DebugMethodItem
import com.android.tools.smali.baksmali.Adaptors.Format.makeInstructionFormatMethodItem
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.analysis.AnalysisException
import com.android.tools.smali.dexlib2.analysis.MethodAnalyzer
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction31t
import com.android.tools.smali.dexlib2.util.InstructionOffsetMap
import com.android.tools.smali.dexlib2.util.InstructionOffsetMap.InvalidInstructionOffset
import com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver
import com.android.tools.smali.dexlib2.util.TypeUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.SparseIntArray
import java.io.IOException

class MethodDefinition(
    val classDef: ClassDefinition,
    val method: Method,
    val methodImpl: MethodImplementation
) {
    val instructions: List<Instruction> = methodImpl.instructions.toList()
    val effectiveInstructions: MutableList<Instruction> = instructions.toMutableList()
    val methodParameters: List<MethodParameter> = method.parameters.toList()
    var registerFormatter: RegisterFormatter? = null

    val labelCache: LabelCache = LabelCache()

    private val packedSwitchMap = SparseIntArray(0)
    private val sparseSwitchMap = SparseIntArray(0)
    private val instructionOffsetMap = InstructionOffsetMap(instructions)

    init {
        try {
            //TODO: what about try/catch blocks inside the dead code? those will need to be commented out too. ugh.

            var endOffset = instructionOffsetMap.getInstructionCodeOffset(instructions.size - 1) +
                instructions[instructions.size - 1].codeUnits

            for (i in 0 until instructions.size) {
                val instruction = instructions[i]

                val opcode = instruction.opcode
                if (opcode == Opcode.PACKED_SWITCH) {
                    var valid = true
                    val codeOffset = instructionOffsetMap.getInstructionCodeOffset(i)
                    var targetOffset = codeOffset + (instruction as OffsetInstruction).codeOffset
                    try {
                        targetOffset = findPayloadOffset(targetOffset, Opcode.PACKED_SWITCH_PAYLOAD)
                    } catch (ex: InvalidSwitchPayload) {
                        valid = false
                    }
                    if (valid) {
                        if (packedSwitchMap.get(targetOffset, -1) != -1) {
                            val payloadInstruction =
                                findSwitchPayload(targetOffset, Opcode.PACKED_SWITCH_PAYLOAD)
                            targetOffset = endOffset
                            effectiveInstructions.set(i, ImmutableInstruction31t(opcode,
                                (instruction as Instruction31t).registerA, targetOffset - codeOffset))
                            effectiveInstructions.add(payloadInstruction)
                            endOffset += payloadInstruction.codeUnits
                        }
                        packedSwitchMap.append(targetOffset, codeOffset)
                    }
                } else if (opcode == Opcode.SPARSE_SWITCH) {
                    var valid = true
                    val codeOffset = instructionOffsetMap.getInstructionCodeOffset(i)
                    var targetOffset = codeOffset + (instruction as OffsetInstruction).codeOffset
                    try {
                        targetOffset = findPayloadOffset(targetOffset, Opcode.SPARSE_SWITCH_PAYLOAD)
                    } catch (ex: InvalidSwitchPayload) {
                        valid = false
                        // The offset to the payload instruction was invalid. Nothing to do, except that we won't
                        // add this instruction to the map.
                    }
                    if (valid) {
                        if (sparseSwitchMap.get(targetOffset, -1) != -1) {
                            val payloadInstruction =
                                findSwitchPayload(targetOffset, Opcode.SPARSE_SWITCH_PAYLOAD)
                            targetOffset = endOffset
                            effectiveInstructions.set(i, ImmutableInstruction31t(opcode,
                                (instruction as Instruction31t).registerA, targetOffset - codeOffset))
                            effectiveInstructions.add(payloadInstruction)
                            endOffset += payloadInstruction.codeUnits
                        }
                        sparseSwitchMap.append(targetOffset, codeOffset)
                    }
                }
            }
        } catch (ex: Exception) {
            val methodString: String
            try {
                methodString = classDef.formatter.getMethodDescriptor(method)
            } catch (ex2: Exception) {
                throw ExceptionWithContext.withContext(ex, "Error while processing method")
            }
            throw ExceptionWithContext.withContext(ex, "Error while processing method %s", methodString)
        }
    }

    companion object {
        @Throws(IOException::class)
        fun writeEmptyMethodTo(writer: BaksmaliWriter, method: Method, classDef: ClassDefinition) {
            writer.write(".method ")
            writeAccessFlagsAndRestrictions(writer, method.accessFlags, method.hiddenApiRestrictions)
            writer.write(method.name)
            writer.write("(")
            val methodParameters = method.parameters.toList()
            for (parameter in methodParameters) {
                writer.writeType(parameter.type)
            }
            writer.write(")")
            writer.write(method.returnType)
            writer.write('\n')

            writer.indent(4)
            writeParameters(classDef, writer, method, methodParameters)

            writeTo(writer, method.annotations)

            writer.deindent(4)
            writer.write(".end method\n")
        }

        @Throws(IOException::class)
        private fun writeAccessFlagsAndRestrictions(
            writer: BaksmaliWriter, accessFlags: Int, hiddenApiRestrictions: Set<HiddenApiRestriction>
        ) {
            for (accessFlag in AccessFlags.getAccessFlagsForMethod(accessFlags)) {
                writer.write(accessFlag.toString())
                writer.write(' ')
            }
            for (hiddenApiRestriction in hiddenApiRestrictions) {
                writer.write(hiddenApiRestriction.toString())
                writer.write(' ')
            }
        }

        @Throws(IOException::class)
        private fun writeParameters(
            classDef: ClassDefinition, writer: BaksmaliWriter, method: Method,
            parameters: List<MethodParameter>
        ) {
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            var registerNumber = if (isStatic) 0 else 1

            for (parameter in parameters) {
                val parameterType = parameter.type
                val parameterName = parameter.name
                val annotations = parameter.annotations
                if ((classDef.options.debugInfo && parameterName != null) || annotations.size != 0) {
                    writer.write(".param p")
                    writer.writeSignedIntAsDec(registerNumber)

                    if (parameterName != null && classDef.options.debugInfo) {
                        writer.write(", ")
                        writer.writeQuotedString(parameterName)
                    }
                    writer.write("    # ")

                    writer.writeType(parameterType)
                    writer.write("\n")
                    if (annotations.size > 0) {
                        writer.indent(4)
                        writeTo(writer, annotations)
                        writer.deindent(4)
                        writer.write(".end param\n")
                    }
                }

                registerNumber++
                if (TypeUtils.isWideType(parameterType)) {
                    registerNumber++
                }
            }
        }
    }

    @Throws(IOException::class)
    fun writeTo(writer: BaksmaliWriter) {
        var parameterRegisterCount = 0
        if (!AccessFlags.STATIC.isSet(method.accessFlags)) {
            parameterRegisterCount++
        }

        writer.write(".method ")
        writeAccessFlagsAndRestrictions(writer, method.accessFlags, method.hiddenApiRestrictions)
        writer.writeSimpleName(method.name)
        writer.write("(")
        for (parameter in methodParameters) {
            val type = parameter.type
            writer.writeType(type)
            parameterRegisterCount++
            if (TypeUtils.isWideType(type)) {
                parameterRegisterCount++
            }
        }
        writer.write(")")
        writer.writeType(method.returnType)
        writer.write('\n')

        writer.indent(4)
        if (classDef.options.localsDirective) {
            writer.write(".locals ")
            writer.writeSignedIntAsDec(methodImpl.registerCount - parameterRegisterCount)
        } else {
            writer.write(".registers ")
            writer.writeSignedIntAsDec(methodImpl.registerCount)
        }
        writer.write('\n')
        writeParameters(classDef, writer, method, methodParameters)

        if (registerFormatter == null) {
            registerFormatter = RegisterFormatter(
                classDef.options, methodImpl.registerCount, parameterRegisterCount)
        }

        writeTo(writer, method.annotations)

        writer.write('\n')

        val methodItems = getMethodItems()
        for (methodItem in methodItems) {
            if (methodItem.writeTo(writer)) {
                writer.write('\n')
            }
        }
        writer.deindent(4)
        writer.write(".end method\n")
    }

    fun findSwitchPayload(targetOffset: Int, type: Opcode): Instruction {
        val targetIndex: Int
        try {
            targetIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(targetOffset)
        } catch (ex: InvalidInstructionOffset) {
            throw InvalidSwitchPayload(targetOffset)
        }

        //TODO: does dalvik let you pad with multiple nops?
        //TODO: does dalvik let a switch instruction point to a non-payload instruction?

        var instruction = instructions[targetIndex]
        if (instruction.opcode != type) {
            // maybe it's pointing to a NOP padding instruction. Look at the next instruction
            if (instruction.opcode == Opcode.NOP) {
                val nextIndex = targetIndex + 1
                if (nextIndex < instructions.size) {
                    instruction = instructions[nextIndex]
                    if (instruction.opcode == type) {
                        return instruction
                    }
                }
            }
            throw InvalidSwitchPayload(targetOffset)
        } else {
            return instruction
        }
    }

    fun findPayloadOffset(targetOffset: Int, type: Opcode): Int {
        val targetIndex: Int
        try {
            targetIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(targetOffset)
        } catch (ex: InvalidInstructionOffset) {
            throw InvalidSwitchPayload(targetOffset)
        }

        //TODO: does dalvik let you pad with multiple nops?
        //TODO: does dalvik let a switch instruction point to a non-payload instruction?

        var instruction = instructions[targetIndex]
        if (instruction.opcode != type) {
            // maybe it's pointing to a NOP padding instruction. Look at the next instruction
            if (instruction.opcode == Opcode.NOP) {
                val nextIndex = targetIndex + 1
                if (nextIndex < instructions.size) {
                    instruction = instructions[nextIndex]
                    if (instruction.opcode == type) {
                        return instructionOffsetMap.getInstructionCodeOffset(nextIndex)
                    }
                }
            }
            throw InvalidSwitchPayload(targetOffset)
        } else {
            return targetOffset
        }
    }

    fun getPackedSwitchBaseAddress(packedSwitchPayloadCodeOffset: Int): Int {
        return packedSwitchMap.get(packedSwitchPayloadCodeOffset, -1)
    }

    fun getSparseSwitchBaseAddress(sparseSwitchPayloadCodeOffset: Int): Int {
        return sparseSwitchMap.get(sparseSwitchPayloadCodeOffset, -1)
    }

    private fun getMethodItems(): List<MethodItem> {
        val methodItems = mutableListOf<MethodItem>()

        if ((classDef.options.registerInfo != 0) || classDef.options.normalizeVirtualMethods ||
            (classDef.options.deodex && needsAnalyzed())) {
            addAnalyzedInstructionMethodItems(methodItems)
        } else {
            addInstructionMethodItems(methodItems)
        }

        addTries(methodItems)
        if (classDef.options.debugInfo) {
            addDebugInfo(methodItems)
        }

        if (classDef.options.sequentialLabels) {
            setLabelSequentialNumbers()
        }

        for (labelMethodItem in labelCache.labels) {
            methodItems.add(labelMethodItem)
        }

        methodItems.sort()

        return methodItems
    }

    private fun needsAnalyzed(): Boolean {
        for (instruction in methodImpl.instructions) {
            if (instruction.opcode.odexOnly()) {
                return true
            }
        }
        return false
    }

    private fun addInstructionMethodItems(methodItems: MutableList<MethodItem>) {
        var currentCodeAddress = 0

        for (i in 0 until effectiveInstructions.size) {
            val instruction = effectiveInstructions[i]

            val methodItem = makeInstructionFormatMethodItem(this, currentCodeAddress, instruction)

            methodItems.add(methodItem)

            if (i != effectiveInstructions.size - 1) {
                methodItems.add(BlankMethodItem(currentCodeAddress))
            }

            if (classDef.options.codeOffsets) {
                methodItems.add(object : MethodItem(currentCodeAddress) {
                    override val sortOrder: Double get() = -1000.0

                    @Throws(IOException::class)
                    override fun writeTo(writer: BaksmaliWriter): Boolean {
                        writer.write("#@")
                        writer.writeUnsignedLongAsHex(codeAddress.toLong() and 0xFFFFFFFFL)
                        return true
                    }
                })
            }

            if (classDef.options.accessorComments && classDef.options.syntheticAccessorResolver != null &&
                (instruction is ReferenceInstruction)) {
                val opcode = instruction.opcode

                if (opcode.referenceType == ReferenceType.METHOD) {
                    val methodReference = instruction.reference as MethodReference

                    try {
                        methodReference.validateReference()

                        if (SyntheticAccessorResolver.looksLikeSyntheticAccessor(methodReference.name)) {
                            val accessedMember = requireNotNull(classDef.options.syntheticAccessorResolver)
                                .getAccessedMember(methodReference)
                            if (accessedMember != null) {
                                methodItems.add(SyntheticAccessCommentMethodItem(
                                    classDef, accessedMember, currentCodeAddress))
                            }
                        }
                    } catch (e: Reference.InvalidReferenceException) {
                        // Just ignore for now. We'll deal with it when processing the instruction
                    }
                }
            }

            currentCodeAddress += instruction.codeUnits
        }
    }

    private fun addAnalyzedInstructionMethodItems(methodItems: MutableList<MethodItem>) {
        val methodAnalyzer = MethodAnalyzer(requireNotNull(classDef.options.classPath), method,
            classDef.options.inlineResolver, classDef.options.normalizeVirtualMethods)

        val analysisException = methodAnalyzer.analysisException
        if (analysisException != null) {
            // TODO: need to keep track of whether any errors occurred, so we can exit with a non-zero result
            methodItems.add(CommentMethodItem(
                "AnalysisException: ${analysisException.message}",
                analysisException.codeAddress, Integer.MIN_VALUE.toDouble()))
            analysisException.printStackTrace(System.err)
        }

        val analyzedInstructions = methodAnalyzer.analyzedInstructions

        var currentCodeAddress = 0
        for (i in 0 until analyzedInstructions.size) {
            val instruction = analyzedInstructions[i]

            val methodItem = makeInstructionFormatMethodItem(
                this, currentCodeAddress, instruction.instruction)

            methodItems.add(methodItem)

            if (instruction.instruction.opcode.format == Format.UnresolvedOdexInstruction) {
                methodItems.add(CommentedOutMethodItem(
                    makeInstructionFormatMethodItem(
                        this, currentCodeAddress, instruction.originalInstruction)))
            }

            if (i != analyzedInstructions.size - 1) {
                methodItems.add(BlankMethodItem(currentCodeAddress))
            }

            if (classDef.options.codeOffsets) {
                methodItems.add(object : MethodItem(currentCodeAddress) {
                    override val sortOrder: Double get() = -1000.0

                    @Throws(IOException::class)
                    override fun writeTo(writer: BaksmaliWriter): Boolean {
                        writer.write("#@")
                        writer.writeUnsignedLongAsHex(codeAddress.toLong() and 0xFFFFFFFFL)
                        return true
                    }
                })
            }

            if (classDef.options.registerInfo != 0 &&
                !instruction.instruction.opcode.format.isPayloadFormat) {
                methodItems.add(
                    PreInstructionRegisterInfoMethodItem(classDef.options.registerInfo,
                        methodAnalyzer, requireNotNull(registerFormatter), instruction, currentCodeAddress))

                methodItems.add(
                    PostInstructionRegisterInfoMethodItem(requireNotNull(registerFormatter), instruction, currentCodeAddress))
            }

            currentCodeAddress += instruction.instruction.codeUnits
        }
    }

    private fun addTries(methodItems: MutableList<MethodItem>) {
        val tryBlocks = methodImpl.tryBlocks
        if (tryBlocks.size == 0) {
            return
        }

        val lastInstructionAddress = instructionOffsetMap.getInstructionCodeOffset(instructions.size - 1)
        val codeSize = lastInstructionAddress + instructions[instructions.size - 1].codeUnits

        for (tryBlock in tryBlocks) {
            val startAddress = tryBlock.startCodeAddress
            val endAddress = startAddress + tryBlock.codeUnitCount

            if (startAddress >= codeSize) {
                throw RuntimeException("Try start offset $startAddress is past the end of the code block.")
            }
            // Note: not >=. endAddress == codeSize is valid, when the try covers the last instruction
            if (endAddress > codeSize) {
                throw RuntimeException("Try end offset $endAddress is past the end of the code block.")
            }

            /**
             * The end address points to the address immediately after the end of the last
             * instruction that the try block covers. We want the .catch directive and end_try
             * label to be associated with the last covered instruction, so we need to get
             * the address for that instruction
             */

            val lastCoveredIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(endAddress - 1, false)
            val lastCoveredAddress = instructionOffsetMap.getInstructionCodeOffset(lastCoveredIndex)

            for (handler in tryBlock.exceptionHandlers) {
                val handlerAddress = handler.handlerCodeAddress
                if (handlerAddress >= codeSize) {
                    throw ExceptionWithContext(
                        "Exception handler offset %d is past the end of the code block.", handlerAddress)
                }

                //use the address from the last covered instruction
                val catchMethodItem = CatchMethodItem(classDef.options, labelCache, lastCoveredAddress,
                    handler.exceptionType, startAddress, endAddress, handlerAddress)
                methodItems.add(catchMethodItem)
            }
        }
    }

    private fun addDebugInfo(methodItems: MutableList<MethodItem>) {
        for (debugItem in methodImpl.debugItems) {
            methodItems.add(DebugMethodItem.build(classDef, requireNotNull(registerFormatter), debugItem))
        }
    }

    private fun setLabelSequentialNumbers() {
        val nextLabelSequenceByType = mutableMapOf<String, Int>()
        val sortedLabels = labelCache.labels.toMutableList()

        //sort the labels by their location in the method
        sortedLabels.sort()

        for (labelMethodItem in sortedLabels) {
            val labelSequence = nextLabelSequenceByType[labelMethodItem.labelPrefix] ?: 0
            labelMethodItem.labelSequence = labelSequence
            nextLabelSequenceByType[labelMethodItem.labelPrefix] = labelSequence + 1
        }
    }

    class LabelCache {
        private val labelsMap = mutableMapOf<LabelMethodItem, LabelMethodItem>()

        val labels: Collection<LabelMethodItem>
            get() = labelsMap.values

        fun internLabel(labelMethodItem: LabelMethodItem): LabelMethodItem {
            val internedLabelMethodItem = labelsMap[labelMethodItem]
            if (internedLabelMethodItem != null) {
                return internedLabelMethodItem
            }
            labelsMap[labelMethodItem] = labelMethodItem
            return labelMethodItem
        }
    }

    class InvalidSwitchPayload(val payloadOffset: Int) :
        ExceptionWithContext("No switch payload at offset: %d", payloadOffset)
}

