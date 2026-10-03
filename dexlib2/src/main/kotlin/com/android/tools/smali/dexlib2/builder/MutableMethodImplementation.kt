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


package com.android.tools.smali.dexlib2.builder

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.debug.BuilderEndLocal
import com.android.tools.smali.dexlib2.builder.debug.BuilderEpilogueBegin
import com.android.tools.smali.dexlib2.builder.debug.BuilderLineNumber
import com.android.tools.smali.dexlib2.builder.debug.BuilderPrologueEnd
import com.android.tools.smali.dexlib2.builder.debug.BuilderRestartLocal
import com.android.tools.smali.dexlib2.builder.debug.BuilderSetSourceFile
import com.android.tools.smali.dexlib2.builder.debug.BuilderStartLocal
import com.android.tools.smali.dexlib2.builder.instruction.BuilderArrayPayload
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20bc
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21ih
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21lh
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22b
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22cs
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction23x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction30t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction32x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35mi
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35ms
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rmi
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rms
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction45cc
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction4rcc
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction51l
import com.android.tools.smali.dexlib2.builder.instruction.BuilderPackedSwitchPayload
import com.android.tools.smali.dexlib2.builder.instruction.BuilderSparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import com.android.tools.smali.dexlib2.iface.debug.RestartLocal
import com.android.tools.smali.dexlib2.iface.debug.SetSourceFile
import com.android.tools.smali.dexlib2.iface.debug.StartLocal
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction12x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20bc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21ih
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21lh
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22b
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22cs
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction23x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction30t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction32x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35mi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35ms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rmi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction45cc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction4rcc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction51l
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.ExceptionWithContext
import java.util.Arrays
import java.util.Collections

open class MutableMethodImplementation : MethodImplementation {
    val instructionList: ArrayList<MethodLocation> = arrayListOf(MethodLocation(null, 0, 0))

    private val mutableTryBlocks = ArrayList<BuilderTryBlock>()

    private var needsFixInstructions = true

    private var registerCountValue: Int = 0

    override val registerCount: Int
        get() = registerCountValue

    constructor(methodImplementation: MethodImplementation) {
        this.registerCountValue = methodImplementation.registerCount

        var codeAddress = 0
        var index = 0

        for (instruction in methodImplementation.instructions) {
            codeAddress += instruction.codeUnits
            index++

            instructionList.add(MethodLocation(null, codeAddress, index))
        }

        val codeAddressToIndex = IntArray(codeAddress + 1)
        Arrays.fill(codeAddressToIndex, -1)

        for (i in instructionList.indices) {
            codeAddressToIndex[instructionList[i].codeAddress] = i
        }

        val switchPayloadTasks = ArrayList<Task>()
        index = 0
        for (instruction in methodImplementation.instructions) {
            val location = instructionList[index]
            val opcode = instruction.opcode
            if (opcode == Opcode.PACKED_SWITCH_PAYLOAD || opcode == Opcode.SPARSE_SWITCH_PAYLOAD) {
                switchPayloadTasks.add(Task { convertAndSetInstruction(location, codeAddressToIndex, instruction) })
            } else {
                convertAndSetInstruction(location, codeAddressToIndex, instruction)
            }
            index++
        }

        // the switch payload instructions must be converted last, so that any switch statements that refer to them
        // have created the referring labels that we look for
        for (switchPayloadTask in switchPayloadTasks) {
            switchPayloadTask.perform()
        }

        for (debugItem in methodImplementation.debugItems) {
            val debugCodeAddress = debugItem.codeAddress
            val locationIndex = mapCodeAddressToIndex(codeAddressToIndex, debugCodeAddress)
            val debugLocation = instructionList[locationIndex]
            val builderDebugItem = convertDebugItem(debugItem)
            debugLocation.getDebugItems().add(builderDebugItem)
            builderDebugItem.location = debugLocation
        }

        for (tryBlock in methodImplementation.tryBlocks) {
            val startLabel = newLabel(codeAddressToIndex, tryBlock.startCodeAddress)
            val endLabel = newLabel(codeAddressToIndex, tryBlock.startCodeAddress + tryBlock.codeUnitCount)

            for (exceptionHandler in tryBlock.exceptionHandlers) {
                mutableTryBlocks.add(
                    BuilderTryBlock(
                        startLabel, endLabel,
                        exceptionHandler.exceptionTypeReference,
                        newLabel(codeAddressToIndex, exceptionHandler.handlerCodeAddress)
                    )
                )
            }
        }
    }

    private fun interface Task {
        fun perform()
    }

    constructor(registerCount: Int) {
        this.registerCountValue = registerCount
    }

    override val instructions: List<BuilderInstruction>
        get() {
            if (needsFixInstructions) {
                fixInstructions()
            }

            return object : AbstractList<BuilderInstruction>() {
                override fun get(i: Int): BuilderInstruction {
                    if (i >= size) {
                        throw IndexOutOfBoundsException()
                    }
                    if (needsFixInstructions) {
                        fixInstructions()
                    }
                    return instructionList[i].instruction!!
                }

                override val size: Int
                    get() {
                        if (needsFixInstructions) {
                            fixInstructions()
                        }
                        // don't include the last MethodLocation, which always has a null instruction
                        return instructionList.size - 1
                    }
            }
        }

    override val tryBlocks: List<BuilderTryBlock>
        get() {
            if (needsFixInstructions) {
                fixInstructions()
            }
            return Collections.unmodifiableList(mutableTryBlocks)
        }

    override val debugItems: Iterable<DebugItem>
        get() {
            if (needsFixInstructions) {
                fixInstructions()
            }

            val debugItems = ArrayList<DebugItem>()

            for (methodLocation in instructionList) {
                if (needsFixInstructions) {
                    throw IllegalStateException(
                        "This iterator was invalidated by a change to" +
                                " this MutableMethodImplementation."
                    )
                }
                debugItems.addAll(methodLocation.getDebugItems())
            }

            return Collections.unmodifiableList(debugItems)
        }

    fun addCatch(type: TypeReference?, from: Label, to: Label, handler: Label) {
        mutableTryBlocks.add(BuilderTryBlock(from, to, type, handler))
    }

    fun addCatch(type: String?, from: Label, to: Label, handler: Label) {
        mutableTryBlocks.add(BuilderTryBlock(from, to, type, handler))
    }

    fun addCatch(from: Label, to: Label, handler: Label) {
        mutableTryBlocks.add(BuilderTryBlock(from, to, handler))
    }

    fun addInstruction(index: Int, instruction: BuilderInstruction) {
        // the end check here is intentially >= rather than >, because the list always includes an "empty"
        // (null instruction) MethodLocation at the end. To add an instruction to the end of the list, the user would
        // provide the index of this empty item, which would be size() - 1.
        if (index >= instructionList.size) {
            throw IndexOutOfBoundsException()
        }

        if (index == instructionList.size - 1) {
            addInstruction(instruction)
            return
        }
        var codeAddress = instructionList[index].codeAddress
        val newLoc = MethodLocation(instruction, codeAddress, index)
        instructionList.add(index, newLoc)
        instruction.location = newLoc

        codeAddress += instruction.codeUnits

        for (i in index + 1 until instructionList.size) {
            val location = instructionList[i]
            location.index++
            location.codeAddress = codeAddress
            if (location.instruction != null) {
                codeAddress += location.instruction!!.codeUnits
            } else {
                // only the last MethodLocation should have a null instruction
                assert(i == instructionList.size - 1)
            }
        }

        needsFixInstructions = true
    }

    fun addInstruction(instruction: BuilderInstruction) {
        val last = instructionList[instructionList.size - 1]
        last.instruction = instruction
        instruction.location = last

        val nextCodeAddress = last.codeAddress + instruction.codeUnits
        instructionList.add(MethodLocation(null, nextCodeAddress, instructionList.size))

        needsFixInstructions = true
    }

    fun replaceInstruction(index: Int, replacementInstruction: BuilderInstruction) {
        if (index >= instructionList.size - 1) {
            throw IndexOutOfBoundsException()
        }

        val replaceLocation = instructionList[index]
        replacementInstruction.location = replaceLocation
        val old = replaceLocation.instruction
        assert(old != null)
        old!!.location = null
        replaceLocation.instruction = replacementInstruction

        // TODO: factor out index/address fix up loop
        var codeAddress = replaceLocation.codeAddress + replaceLocation.instruction!!.codeUnits
        for (i in index + 1 until instructionList.size) {
            val location = instructionList[i]
            location.codeAddress = codeAddress

            val instruction = location.instruction
            if (instruction != null) {
                codeAddress += instruction.codeUnits
            } else {
                assert(i == instructionList.size - 1)
            }
        }

        needsFixInstructions = true
    }

    fun removeInstruction(index: Int) {
        if (index >= instructionList.size - 1) {
            throw IndexOutOfBoundsException()
        }

        val toRemove = instructionList[index]
        toRemove.instruction = null
        val next = instructionList[index + 1]
        toRemove.mergeInto(next)

        instructionList.removeAt(index)
        var codeAddress = toRemove.codeAddress
        for (i in index until instructionList.size) {
            val location = instructionList[i]
            location.index = i
            location.codeAddress = codeAddress

            val instruction = location.instruction
            if (instruction != null) {
                codeAddress += instruction.codeUnits
            } else {
                assert(i == instructionList.size - 1)
            }
        }

        needsFixInstructions = true
    }

    fun swapInstructions(index1: Int, index2: Int) {
        if (index1 >= instructionList.size - 1 || index2 >= instructionList.size - 1) {
            throw IndexOutOfBoundsException()
        }
        val first = instructionList[index1]
        val second = instructionList[index2]

        // only the last MethodLocation may have a null instruction
        assert(first.instruction != null)
        assert(second.instruction != null)

        first.instruction!!.location = second
        second.instruction!!.location = first

        val tmp = second.instruction
        second.instruction = first.instruction
        first.instruction = tmp

        var i1 = index1
        var i2 = index2
        if (i2 < i1) {
            val tmpIndex = i2
            i2 = i1
            i1 = tmpIndex
        }

        var codeAddress = first.codeAddress + first.instruction!!.codeUnits
        for (i in i1 + 1..i2) {
            val location = instructionList[i]
            location.codeAddress = codeAddress

            val instruction = location.instruction
            assert(instruction != null)
            codeAddress += location.instruction!!.codeUnits
        }

        needsFixInstructions = true
    }

    private fun getFirstNonNop(startIndex: Int): BuilderInstruction? {
        for (i in startIndex until instructionList.size - 1) {
            val instruction = instructionList[i].instruction
            assert(instruction != null)
            if (instruction!!.opcode != Opcode.NOP) {
                return instruction
            }
        }
        return null
    }

    private fun fixInstructions() {
        val payloadLocations = HashSet<MethodLocation>()

        for (location in instructionList) {
            val instruction = location.instruction
            if (instruction != null) {
                when (instruction.opcode) {
                    Opcode.SPARSE_SWITCH, Opcode.PACKED_SWITCH -> {
                        val targetLocation =
                            (instruction as BuilderOffsetInstruction).target.requireLocation()
                        var targetInstruction: BuilderInstruction? = targetLocation.instruction
                        if (targetInstruction == null) {
                            throw IllegalStateException(
                                ("Switch instruction at address/index " +
                                        "0x%x/%d points to the end of the method.").format(
                                    location.codeAddress, location.index
                                )
                            )
                        }

                        if (targetInstruction.opcode == Opcode.NOP) {
                            targetInstruction = getFirstNonNop(targetLocation.index + 1)
                        }
                        if (targetInstruction == null || targetInstruction !is BuilderSwitchPayload) {
                            throw IllegalStateException(
                                ("Switch instruction at address/index " +
                                        "0x%x/%d does not refer to a payload instruction.").format(
                                    location.codeAddress, location.index
                                )
                            )
                        }
                        if ((instruction.opcode == Opcode.PACKED_SWITCH &&
                                    targetInstruction.opcode != Opcode.PACKED_SWITCH_PAYLOAD) ||
                            (instruction.opcode == Opcode.SPARSE_SWITCH &&
                                    targetInstruction.opcode != Opcode.SPARSE_SWITCH_PAYLOAD)
                        ) {
                            throw IllegalStateException(
                                ("Switch instruction at address/index " +
                                        "0x%x/%d refers to the wrong type of payload instruction.").format(
                                    location.codeAddress, location.index
                                )
                            )
                        }

                        if (!payloadLocations.add(targetLocation)) {
                            throw IllegalStateException(
                                "Multiple switch instructions refer to the same payload. " +
                                        "This is not currently supported. Please file a bug :)"
                            )
                        }

                        targetInstruction.referrer = location
                    }
                    else -> {}
                }
            }
        }

        var madeChanges: Boolean
        do {
            madeChanges = false

            var index = 0
            while (index < instructionList.size) {
                val location = instructionList[index]
                val instruction = location.instruction
                if (instruction != null) {
                    when (instruction.opcode) {
                        Opcode.GOTO -> {
                            val offset = (instruction as BuilderOffsetInstruction).internalGetCodeOffset()
                            if (offset < Byte.MIN_VALUE || offset > Byte.MAX_VALUE) {
                                val replacement: BuilderOffsetInstruction
                                if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                    replacement = BuilderInstruction30t(
                                        Opcode.GOTO_32,
                                        (instruction as BuilderOffsetInstruction).target
                                    )
                                } else {
                                    replacement = BuilderInstruction20t(
                                        Opcode.GOTO_16,
                                        (instruction as BuilderOffsetInstruction).target
                                    )
                                }
                                replaceInstruction(location.index, replacement)
                                madeChanges = true
                            }
                        }
                        Opcode.GOTO_16 -> {
                            val offset = (instruction as BuilderOffsetInstruction).internalGetCodeOffset()
                            if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                val replacement = BuilderInstruction30t(
                                    Opcode.GOTO_32,
                                    (instruction as BuilderOffsetInstruction).target
                                )
                                replaceInstruction(location.index, replacement)
                                madeChanges = true
                            }
                        }
                        Opcode.SPARSE_SWITCH_PAYLOAD, Opcode.PACKED_SWITCH_PAYLOAD -> {
                            if ((instruction as BuilderSwitchPayload).referrer == null) {
                                // if the switch payload isn't referenced, just remove it
                                removeInstruction(index)
                                index--
                                madeChanges = true
                            } else {
                                // intentional fall-through to the ARRAY_PAYLOAD case
                                if ((location.codeAddress and 0x01) != 0) {
                                    val previousIndex = location.index - 1
                                    val previousLocation = instructionList[previousIndex]
                                    val previousInstruction = previousLocation.instruction
                                    assert(previousInstruction != null)
                                    if (previousInstruction!!.opcode == Opcode.NOP) {
                                        removeInstruction(previousIndex)
                                        index--
                                    } else {
                                        addInstruction(location.index, BuilderInstruction10x(Opcode.NOP))
                                        index++
                                    }
                                    madeChanges = true
                                }
                            }
                        }
                        Opcode.ARRAY_PAYLOAD -> {
                            if ((location.codeAddress and 0x01) != 0) {
                                val previousIndex = location.index - 1
                                val previousLocation = instructionList[previousIndex]
                                val previousInstruction = previousLocation.instruction
                                assert(previousInstruction != null)
                                if (previousInstruction!!.opcode == Opcode.NOP) {
                                    removeInstruction(previousIndex)
                                    index--
                                } else {
                                    addInstruction(location.index, BuilderInstruction10x(Opcode.NOP))
                                    index++
                                }
                                madeChanges = true
                            }
                        }
                        else -> {}
                    }
                }
                index++
            }
        } while (madeChanges)

        needsFixInstructions = false
    }

    private fun mapCodeAddressToIndex(codeAddressToIndex: IntArray, codeAddressIn: Int): Int {
        var codeAddress = codeAddressIn
        var index: Int
        while (true) {
            if (codeAddress >= codeAddressToIndex.size) {
                codeAddress = codeAddressToIndex.size - 1
            }
            index = codeAddressToIndex[codeAddress]
            if (index < 0) {
                codeAddress--
            } else {
                return index
            }
        }
    }

    private fun mapCodeAddressToIndex(codeAddress: Int): Int {
        val avgCodeUnitsPerInstruction = 1.9f

        var index = (codeAddress / avgCodeUnitsPerInstruction).toInt()
        if (index >= instructionList.size) {
            index = instructionList.size - 1
        }

        val guessedLocation = instructionList[index]

        if (guessedLocation.codeAddress == codeAddress) {
            return index
        } else if (guessedLocation.codeAddress > codeAddress) {
            do {
                index--
            } while (instructionList[index].codeAddress > codeAddress)
            return index
        } else {
            do {
                index++
            } while (index < instructionList.size && instructionList[index].codeAddress <= codeAddress)
            return index - 1
        }
    }

    fun newLabelForAddress(codeAddress: Int): Label {
        if (codeAddress < 0 || codeAddress > instructionList[instructionList.size - 1].codeAddress) {
            throw IndexOutOfBoundsException("codeAddress ${codeAddress} out of bounds")
        }
        val referent = instructionList[mapCodeAddressToIndex(codeAddress)]
        return referent.addNewLabel()
    }

    fun newLabelForIndex(instructionIndex: Int): Label {
        if (instructionIndex < 0 || instructionIndex >= instructionList.size) {
            throw IndexOutOfBoundsException(
                "instruction index ${instructionIndex} out of bounds"
            )
        }
        val referent = instructionList[instructionIndex]
        return referent.addNewLabel()
    }

    private fun newLabel(codeAddressToIndex: IntArray, codeAddress: Int): Label {
        val referent = instructionList[mapCodeAddressToIndex(codeAddressToIndex, codeAddress)]
        return referent.addNewLabel()
    }

    private class SwitchPayloadReferenceLabel : Label() {
        var switchLocation: MethodLocation? = null
    }

    fun newSwitchPayloadReferenceLabel(
        switchLocation: MethodLocation,
        codeAddressToIndex: IntArray,
        codeAddress: Int
    ): Label {
        val referent = instructionList[mapCodeAddressToIndex(codeAddressToIndex, codeAddress)]
        val label = SwitchPayloadReferenceLabel()
        label.switchLocation = switchLocation
        referent.getLabels().add(label)
        return label
    }

    private fun setInstruction(location: MethodLocation, instruction: BuilderInstruction) {
        location.instruction = instruction
        instruction.location = location
    }

    private fun convertAndSetInstruction(
        location: MethodLocation,
        codeAddressToIndex: IntArray,
        instruction: Instruction
    ) {
        when (instruction.opcode.format) {
            Format.Format10t -> {
                setInstruction(
                    location,
                    newBuilderInstruction10t(
                        location.codeAddress, codeAddressToIndex,
                        instruction as Instruction10t
                    )
                )
                return
            }
            Format.Format10x -> {
                setInstruction(location, newBuilderInstruction10x(instruction as Instruction10x))
                return
            }
            Format.Format11n -> {
                setInstruction(location, newBuilderInstruction11n(instruction as Instruction11n))
                return
            }
            Format.Format11x -> {
                setInstruction(location, newBuilderInstruction11x(instruction as Instruction11x))
                return
            }
            Format.Format12x -> {
                setInstruction(location, newBuilderInstruction12x(instruction as Instruction12x))
                return
            }
            Format.Format20bc -> {
                setInstruction(location, newBuilderInstruction20bc(instruction as Instruction20bc))
                return
            }
            Format.Format20t -> {
                setInstruction(
                    location,
                    newBuilderInstruction20t(
                        location.codeAddress, codeAddressToIndex,
                        instruction as Instruction20t
                    )
                )
                return
            }
            Format.Format21c -> {
                setInstruction(location, newBuilderInstruction21c(instruction as Instruction21c))
                return
            }
            Format.Format21ih -> {
                setInstruction(location, newBuilderInstruction21ih(instruction as Instruction21ih))
                return
            }
            Format.Format21lh -> {
                setInstruction(location, newBuilderInstruction21lh(instruction as Instruction21lh))
                return
            }
            Format.Format21s -> {
                setInstruction(location, newBuilderInstruction21s(instruction as Instruction21s))
                return
            }
            Format.Format21t -> {
                setInstruction(
                    location,
                    newBuilderInstruction21t(
                        location.codeAddress, codeAddressToIndex,
                        instruction as Instruction21t
                    )
                )
                return
            }
            Format.Format22b -> {
                setInstruction(location, newBuilderInstruction22b(instruction as Instruction22b))
                return
            }
            Format.Format22c -> {
                setInstruction(location, newBuilderInstruction22c(instruction as Instruction22c))
                return
            }
            Format.Format22cs -> {
                setInstruction(location, newBuilderInstruction22cs(instruction as Instruction22cs))
                return
            }
            Format.Format22s -> {
                setInstruction(location, newBuilderInstruction22s(instruction as Instruction22s))
                return
            }
            Format.Format22t -> {
                setInstruction(
                    location,
                    newBuilderInstruction22t(
                        location.codeAddress, codeAddressToIndex,
                        instruction as Instruction22t
                    )
                )
                return
            }
            Format.Format22x -> {
                setInstruction(location, newBuilderInstruction22x(instruction as Instruction22x))
                return
            }
            Format.Format23x -> {
                setInstruction(location, newBuilderInstruction23x(instruction as Instruction23x))
                return
            }
            Format.Format30t -> {
                setInstruction(
                    location,
                    newBuilderInstruction30t(
                        location.codeAddress, codeAddressToIndex,
                        instruction as Instruction30t
                    )
                )
                return
            }
            Format.Format31c -> {
                setInstruction(location, newBuilderInstruction31c(instruction as Instruction31c))
                return
            }
            Format.Format31i -> {
                setInstruction(location, newBuilderInstruction31i(instruction as Instruction31i))
                return
            }
            Format.Format31t -> {
                setInstruction(
                    location,
                    newBuilderInstruction31t(location, codeAddressToIndex, instruction as Instruction31t)
                )
                return
            }
            Format.Format32x -> {
                setInstruction(location, newBuilderInstruction32x(instruction as Instruction32x))
                return
            }
            Format.Format35c -> {
                setInstruction(location, newBuilderInstruction35c(instruction as Instruction35c))
                return
            }
            Format.Format35mi -> {
                setInstruction(location, newBuilderInstruction35mi(instruction as Instruction35mi))
                return
            }
            Format.Format35ms -> {
                setInstruction(location, newBuilderInstruction35ms(instruction as Instruction35ms))
                return
            }
            Format.Format3rc -> {
                setInstruction(location, newBuilderInstruction3rc(instruction as Instruction3rc))
                return
            }
            Format.Format3rmi -> {
                setInstruction(location, newBuilderInstruction3rmi(instruction as Instruction3rmi))
                return
            }
            Format.Format3rms -> {
                setInstruction(location, newBuilderInstruction3rms(instruction as Instruction3rms))
                return
            }
            Format.Format45cc -> {
                setInstruction(location, newBuilderInstruction45cc(instruction as Instruction45cc))
                return
            }
            Format.Format4rcc -> {
                setInstruction(location, newBuilderInstruction4rcc(instruction as Instruction4rcc))
                return
            }
            Format.Format51l -> {
                setInstruction(location, newBuilderInstruction51l(instruction as Instruction51l))
                return
            }
            Format.PackedSwitchPayload -> {
                setInstruction(
                    location,
                    newBuilderPackedSwitchPayload(location, codeAddressToIndex, instruction as PackedSwitchPayload)
                )
                return
            }
            Format.SparseSwitchPayload -> {
                setInstruction(
                    location,
                    newBuilderSparseSwitchPayload(location, codeAddressToIndex, instruction as SparseSwitchPayload)
                )
                return
            }
            Format.ArrayPayload -> {
                setInstruction(location, newBuilderArrayPayload(instruction as ArrayPayload))
                return
            }
            else -> throw ExceptionWithContext(
                "Instruction format %s not supported",
                instruction.opcode.format
            )
        }
    }

    private fun newBuilderInstruction10t(
        codeAddress: Int,
        codeAddressToIndex: IntArray,
        instruction: Instruction10t
    ): BuilderInstruction10t {
        return BuilderInstruction10t(
            instruction.opcode,
            newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        )
    }

    private fun newBuilderInstruction10x(instruction: Instruction10x): BuilderInstruction10x {
        return BuilderInstruction10x(instruction.opcode)
    }

    private fun newBuilderInstruction11n(instruction: Instruction11n): BuilderInstruction11n {
        return BuilderInstruction11n(
            instruction.opcode,
            instruction.registerA,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction11x(instruction: Instruction11x): BuilderInstruction11x {
        return BuilderInstruction11x(
            instruction.opcode,
            instruction.registerA
        )
    }

    private fun newBuilderInstruction12x(instruction: Instruction12x): BuilderInstruction12x {
        return BuilderInstruction12x(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB
        )
    }

    private fun newBuilderInstruction20bc(instruction: Instruction20bc): BuilderInstruction20bc {
        return BuilderInstruction20bc(
            instruction.opcode,
            instruction.verificationError,
            instruction.reference
        )
    }

    private fun newBuilderInstruction20t(
        codeAddress: Int,
        codeAddressToIndex: IntArray,
        instruction: Instruction20t
    ): BuilderInstruction20t {
        return BuilderInstruction20t(
            instruction.opcode,
            newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        )
    }

    private fun newBuilderInstruction21c(instruction: Instruction21c): BuilderInstruction21c {
        return BuilderInstruction21c(
            instruction.opcode,
            instruction.registerA,
            instruction.reference
        )
    }

    private fun newBuilderInstruction21ih(instruction: Instruction21ih): BuilderInstruction21ih {
        return BuilderInstruction21ih(
            instruction.opcode,
            instruction.registerA,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction21lh(instruction: Instruction21lh): BuilderInstruction21lh {
        return BuilderInstruction21lh(
            instruction.opcode,
            instruction.registerA,
            instruction.wideLiteral
        )
    }

    private fun newBuilderInstruction21s(instruction: Instruction21s): BuilderInstruction21s {
        return BuilderInstruction21s(
            instruction.opcode,
            instruction.registerA,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction21t(
        codeAddress: Int,
        codeAddressToIndex: IntArray,
        instruction: Instruction21t
    ): BuilderInstruction21t {
        return BuilderInstruction21t(
            instruction.opcode,
            instruction.registerA,
            newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        )
    }

    private fun newBuilderInstruction22b(instruction: Instruction22b): BuilderInstruction22b {
        return BuilderInstruction22b(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction22c(instruction: Instruction22c): BuilderInstruction22c {
        return BuilderInstruction22c(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            instruction.reference
        )
    }

    private fun newBuilderInstruction22cs(instruction: Instruction22cs): BuilderInstruction22cs {
        return BuilderInstruction22cs(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            instruction.fieldOffset
        )
    }

    private fun newBuilderInstruction22s(instruction: Instruction22s): BuilderInstruction22s {
        return BuilderInstruction22s(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction22t(
        codeAddress: Int,
        codeAddressToIndex: IntArray,
        instruction: Instruction22t
    ): BuilderInstruction22t {
        return BuilderInstruction22t(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        )
    }

    private fun newBuilderInstruction22x(instruction: Instruction22x): BuilderInstruction22x {
        return BuilderInstruction22x(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB
        )
    }

    private fun newBuilderInstruction23x(instruction: Instruction23x): BuilderInstruction23x {
        return BuilderInstruction23x(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB,
            instruction.registerC
        )
    }

    private fun newBuilderInstruction30t(
        codeAddress: Int,
        codeAddressToIndex: IntArray,
        instruction: Instruction30t
    ): BuilderInstruction30t {
        return BuilderInstruction30t(
            instruction.opcode,
            newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        )
    }

    private fun newBuilderInstruction31c(instruction: Instruction31c): BuilderInstruction31c {
        return BuilderInstruction31c(
            instruction.opcode,
            instruction.registerA,
            instruction.reference
        )
    }

    private fun newBuilderInstruction31i(instruction: Instruction31i): BuilderInstruction31i {
        return BuilderInstruction31i(
            instruction.opcode,
            instruction.registerA,
            instruction.narrowLiteral
        )
    }

    private fun newBuilderInstruction31t(
        location: MethodLocation,
        codeAddressToIndex: IntArray,
        instruction: Instruction31t
    ): BuilderInstruction31t {
        val codeAddress = location.codeAddress
        val label: Label
        if (instruction.opcode != Opcode.FILL_ARRAY_DATA) {
            // if it's a sparse switch or packed switch
            label = newSwitchPayloadReferenceLabel(
                location, codeAddressToIndex,
                codeAddress + instruction.codeOffset
            )
        } else {
            label = newLabel(codeAddressToIndex, codeAddress + instruction.codeOffset)
        }
        return BuilderInstruction31t(
            instruction.opcode,
            instruction.registerA,
            label
        )
    }

    private fun newBuilderInstruction32x(instruction: Instruction32x): BuilderInstruction32x {
        return BuilderInstruction32x(
            instruction.opcode,
            instruction.registerA,
            instruction.registerB
        )
    }

    private fun newBuilderInstruction35c(instruction: Instruction35c): BuilderInstruction35c {
        return BuilderInstruction35c(
            instruction.opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            instruction.reference
        )
    }

    private fun newBuilderInstruction35mi(instruction: Instruction35mi): BuilderInstruction35mi {
        return BuilderInstruction35mi(
            instruction.opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            instruction.inlineIndex
        )
    }

    private fun newBuilderInstruction35ms(instruction: Instruction35ms): BuilderInstruction35ms {
        return BuilderInstruction35ms(
            instruction.opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            instruction.vtableIndex
        )
    }

    private fun newBuilderInstruction3rc(instruction: Instruction3rc): BuilderInstruction3rc {
        return BuilderInstruction3rc(
            instruction.opcode,
            instruction.startRegister,
            instruction.registerCount,
            instruction.reference
        )
    }

    private fun newBuilderInstruction3rmi(instruction: Instruction3rmi): BuilderInstruction3rmi {
        return BuilderInstruction3rmi(
            instruction.opcode,
            instruction.startRegister,
            instruction.registerCount,
            instruction.inlineIndex
        )
    }

    private fun newBuilderInstruction3rms(instruction: Instruction3rms): BuilderInstruction3rms {
        return BuilderInstruction3rms(
            instruction.opcode,
            instruction.startRegister,
            instruction.registerCount,
            instruction.vtableIndex
        )
    }

    private fun newBuilderInstruction45cc(instruction: Instruction45cc): BuilderInstruction45cc {
        return BuilderInstruction45cc(
            instruction.opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            instruction.reference,
            instruction.reference2
        )
    }

    private fun newBuilderInstruction4rcc(instruction: Instruction4rcc): BuilderInstruction4rcc {
        return BuilderInstruction4rcc(
            instruction.opcode,
            instruction.startRegister,
            instruction.registerCount,
            instruction.reference,
            instruction.reference2
        )
    }

    private fun newBuilderInstruction51l(instruction: Instruction51l): BuilderInstruction51l {
        return BuilderInstruction51l(
            instruction.opcode,
            instruction.registerA,
            instruction.wideLiteral
        )
    }

    private fun findSwitchForPayload(payloadLocation: MethodLocation): MethodLocation? {
        var location = payloadLocation
        var switchLocation: MethodLocation? = null
        while (true) {
            for (label in location.getLabels()) {
                if (label is SwitchPayloadReferenceLabel) {
                    if (switchLocation != null) {
                        throw IllegalStateException(
                            "Multiple switch instructions refer to the same payload. " +
                                    "This is not currently supported. Please file a bug :)"
                        )
                    }
                    switchLocation = label.switchLocation
                }
            }

            // A switch instruction can refer to the payload instruction itself, or to a nop before the payload
            // instruction.
            // We need to search for all occurrences of a switch reference, so we can detect when multiple switch
            // statements refer to the same payload
            // TODO: confirm that it could refer to the first NOP in a series of NOPs preceding the payload
            if (location.index == 0) {
                return switchLocation
            }
            location = instructionList[location.index - 1]
            if (location.instruction == null || location.instruction!!.opcode != Opcode.NOP) {
                return switchLocation
            }
        }
    }

    private fun newBuilderPackedSwitchPayload(
        location: MethodLocation,
        codeAddressToIndex: IntArray,
        instruction: PackedSwitchPayload
    ): BuilderPackedSwitchPayload {
        val switchElements = instruction.switchElements
        if (switchElements.size == 0) {
            return BuilderPackedSwitchPayload(0, null)
        }

        val switchLocation = findSwitchForPayload(location)
        val baseAddress: Int
        if (switchLocation == null) {
            baseAddress = 0
        } else {
            baseAddress = switchLocation.codeAddress
        }

        val labels = ArrayList<Label>()
        for (element in switchElements) {
            labels.add(newLabel(codeAddressToIndex, element.offset + baseAddress))
        }

        return BuilderPackedSwitchPayload(switchElements[0].key, labels)
    }

    private fun newBuilderSparseSwitchPayload(
        location: MethodLocation,
        codeAddressToIndex: IntArray,
        instruction: SparseSwitchPayload
    ): BuilderSparseSwitchPayload {
        val switchElements = instruction.switchElements
        if (switchElements.size == 0) {
            return BuilderSparseSwitchPayload(null)
        }

        val switchLocation = findSwitchForPayload(location)
        val baseAddress: Int
        if (switchLocation == null) {
            baseAddress = 0
        } else {
            baseAddress = switchLocation.codeAddress
        }

        val labelElements = ArrayList<SwitchLabelElement>()
        for (element in switchElements) {
            labelElements.add(
                SwitchLabelElement(
                    element.key,
                    newLabel(codeAddressToIndex, element.offset + baseAddress)
                )
            )
        }

        return BuilderSparseSwitchPayload(labelElements)
    }

    private fun newBuilderArrayPayload(instruction: ArrayPayload): BuilderArrayPayload {
        return BuilderArrayPayload(instruction.elementWidth, instruction.arrayElements)
    }

    private fun convertDebugItem(debugItem: DebugItem): BuilderDebugItem {
        when (debugItem.debugItemType) {
            DebugItemType.START_LOCAL -> {
                val startLocal = debugItem as StartLocal
                return BuilderStartLocal(
                    startLocal.register, startLocal.nameReference,
                    startLocal.typeReference, startLocal.signatureReference
                )
            }
            DebugItemType.END_LOCAL -> {
                val endLocal = debugItem as EndLocal
                return BuilderEndLocal(endLocal.register)
            }
            DebugItemType.RESTART_LOCAL -> {
                val restartLocal = debugItem as RestartLocal
                return BuilderRestartLocal(restartLocal.register)
            }
            DebugItemType.PROLOGUE_END -> return BuilderPrologueEnd()
            DebugItemType.EPILOGUE_BEGIN -> return BuilderEpilogueBegin()
            DebugItemType.LINE_NUMBER -> {
                val lineNumber = debugItem as LineNumber
                return BuilderLineNumber(lineNumber.lineNumber)
            }
            DebugItemType.SET_SOURCE_FILE -> {
                val setSourceFile = debugItem as SetSourceFile
                return BuilderSetSourceFile(setSourceFile.sourceFileReference)
            }
            else -> throw ExceptionWithContext("Invalid debug item type: " + debugItem.debugItemType)
        }
    }
}
