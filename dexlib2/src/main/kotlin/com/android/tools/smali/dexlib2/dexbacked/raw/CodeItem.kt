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

package com.android.tools.smali.dexlib2.dexbacked.raw

import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.VerificationError
import com.android.tools.smali.dexlib2.dexbacked.CDexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.instruction.DexBackedInstruction
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.FieldOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.InlineIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VerificationErrorInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VtableIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.NumberUtils
import com.android.tools.smali.util.StringUtils
import java.util.ArrayList

object CodeItem {
    const val REGISTERS_OFFSET = 0
    const val INS_OFFSET = 2
    const val OUTS_OFFSET = 4
    const val TRIES_SIZE_OFFSET = 6
    const val DEBUG_INFO_OFFSET = 8
    const val INSTRUCTION_COUNT_OFFSET = 12
    const val INSTRUCTION_START_OFFSET = 16

    @JvmField
    var CDEX_TRIES_SIZE_SHIFT = 0

    @JvmField
    var CDEX_OUTS_COUNT_SHIFT = 4

    @JvmField
    var CDEX_INS_COUNT_SHIFT = 8

    @JvmField
    var CDEX_REGISTER_COUNT_SHIFT = 12

    @JvmField
    var CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET = 2

    @JvmField
    var CDEX_INSTRUCTIONS_SIZE_SHIFT = 5

    @JvmField
    var CDEX_PREHEADER_FLAGS_MASK = 0x1f

    @JvmField
    var CDEX_PREHEADER_FLAG_REGISTER_COUNT = 1 shl 0

    @JvmField
    var CDEX_PREHEADER_FLAG_INS_COUNT = 1 shl 1

    @JvmField
    var CDEX_PREHEADER_FLAG_OUTS_COUNT = 1 shl 2

    @JvmField
    var CDEX_PREHEADER_FLAG_TRIES_COUNT = 1 shl 3

    @JvmField
    var CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE = 1 shl 4

    object TryItem {
        const val ITEM_SIZE = 8

        const val START_ADDRESS_OFFSET = 0
        const val CODE_UNIT_COUNT_OFFSET = 4
        const val HANDLER_OFFSET = 6
    }

    @JvmStatic
    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return if (annotator.dexFile is CDexBackedDexFile) {
            makeAnnotatorForCDex(annotator, mapItem)
        } else {
            makeAnnotatorForDex(annotator, mapItem)
        }
    }

    private fun makeAnnotatorForDex(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return CodeItemAnnotator(annotator, mapItem)
    }

    private fun makeAnnotatorForCDex(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : CodeItemAnnotator(annotator, mapItem) {
            private lateinit var sortedItems: MutableList<Int>

            override fun annotateSection(out: AnnotatedBytes) {
                sortedItems = ArrayList(itemIdentities.keys)
                sortedItems.sort()

                out.moveTo(sectionOffset)
                annotateSectionInner(out, itemIdentities.size)
            }

            override fun getItemOffset(itemIndex: Int, currentOffset: Int): Int {
                return sortedItems[itemIndex]
            }

            override fun annotatePreInstructionFields(
                out: AnnotatedBytes,
                reader: DexReader<out DexBuffer>,
                itemIdentity: String?
            ): PreInstructionInfo {
                val sizeFields = reader.readUshort()

                val triesCount = (sizeFields shr CDEX_TRIES_SIZE_SHIFT) and 0xf
                val outsCount = (sizeFields shr CDEX_OUTS_COUNT_SHIFT) and 0xf
                val insCount = (sizeFields shr CDEX_INS_COUNT_SHIFT) and 0xf
                val registerCount = (sizeFields shr CDEX_REGISTER_COUNT_SHIFT) and 0xf

                val startOffset = out.cursor

                out.annotate(2, "tries_size = %d", triesCount)
                out.annotate(0, "outs_size = %d", outsCount)
                out.annotate(0, "ins_size = %d", insCount)
                out.annotate(0, "registers_size = %d", registerCount)

                val instructionsSizeAndPreheaderFlags = reader.readUshort()

                val instructionsSize =
                    instructionsSizeAndPreheaderFlags shr CDEX_INSTRUCTIONS_SIZE_SHIFT

                out.annotate(2, "insns_size = %d", instructionsSize)

                val instructionsStartOffset = out.cursor
                var preheaderOffset = startOffset

                var totalTriesCount = triesCount
                var totalInstructionsSize = instructionsSize

                if ((instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAGS_MASK) != 0) {
                    var preheaderCount = Integer.bitCount(
                        instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAGS_MASK
                    )
                    if ((instructionsSizeAndPreheaderFlags and
                            CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0
                    ) {
                        // The instructions size preheader is 2 shorts
                        preheaderCount++
                    }

                    out.moveTo(startOffset - 2 * preheaderCount)
                    out.deindent()
                    out.annotate(0, "[preheader for next code_item]")
                    out.indent()
                    out.moveTo(instructionsStartOffset)
                }

                if ((instructionsSizeAndPreheaderFlags and
                        CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0
                ) {
                    out.annotate(0, "insns_size_preheader_flag=1")
                    preheaderOffset -= 2
                    reader.offset = preheaderOffset
                    var extraInstructionsSize = reader.readUshort()
                    preheaderOffset -= 2
                    reader.offset = preheaderOffset
                    extraInstructionsSize += reader.readUshort()

                    out.moveTo(preheaderOffset)
                    totalInstructionsSize += extraInstructionsSize
                    out.annotate(
                        2, "insns_size = %d + %d = %d",
                        instructionsSize, extraInstructionsSize,
                        instructionsSize + extraInstructionsSize
                    )
                    out.moveTo(instructionsStartOffset)
                }

                if ((instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
                    out.annotate(0, "registers_size_preheader_flag=1")
                    preheaderOffset -= 2
                    out.moveTo(preheaderOffset)
                    reader.offset = preheaderOffset
                    val extraRegisterCount = reader.readUshort()
                    out.annotate(
                        2, "registers_size = %d + %d = %d",
                        registerCount, extraRegisterCount, registerCount + extraRegisterCount
                    )
                    out.moveTo(instructionsStartOffset)
                }
                if ((instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAG_INS_COUNT) != 0) {
                    out.annotate(0, "ins_size_preheader_flag=1")
                    preheaderOffset -= 2
                    out.moveTo(preheaderOffset)
                    reader.offset = preheaderOffset
                    val extraInsCount = reader.readUshort()
                    out.annotate(
                        2, "ins_size = %d + %d = %d",
                        insCount, extraInsCount, insCount + extraInsCount
                    )
                    out.moveTo(instructionsStartOffset)
                }
                if ((instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAG_OUTS_COUNT) != 0) {
                    out.annotate(0, "outs_size_preheader_flag=1")
                    preheaderOffset -= 2
                    out.moveTo(preheaderOffset)
                    reader.offset = preheaderOffset
                    val extraOutsCount = reader.readUshort()
                    out.annotate(
                        2, "outs_size = %d + %d = %d",
                        outsCount, extraOutsCount, outsCount + extraOutsCount
                    )
                    out.moveTo(instructionsStartOffset)
                }
                if ((instructionsSizeAndPreheaderFlags and CDEX_PREHEADER_FLAG_TRIES_COUNT) != 0) {
                    out.annotate(0, "tries_size_preheader_flag=1")
                    preheaderOffset -= 2
                    out.moveTo(preheaderOffset)
                    reader.offset = preheaderOffset
                    val extraTriesCount = reader.readUshort()
                    totalTriesCount += extraTriesCount
                    out.annotate(
                        2, "tries_size = %d + %d = %d",
                        triesCount, extraTriesCount, triesCount + extraTriesCount
                    )
                    out.moveTo(instructionsStartOffset)
                }

                reader.offset = instructionsStartOffset

                return PreInstructionInfo(totalTriesCount, totalInstructionsSize)
            }
        }
    }

    private open class CodeItemAnnotator(
        annotator: DexAnnotator,
        mapItem: MapItem
    ) : SectionAnnotator(annotator, mapItem) {
        private var debugInfoAnnotator: SectionAnnotator? = null

        override fun getItemName(): String {
            return "code_item"
        }

        override fun getItemAlignment(): Int {
            return 4
        }

        protected inner class PreInstructionInfo(
            @JvmField var triesCount: Int,
            @JvmField var instructionSize: Int
        )

        protected open fun annotatePreInstructionFields(
            out: AnnotatedBytes,
            reader: DexReader<out DexBuffer>,
            itemIdentity: String?
        ): PreInstructionInfo {
            val registers = reader.readUshort()
            out.annotate(2, "registers_size = %d", registers)

            val inSize = reader.readUshort()
            out.annotate(2, "ins_size = %d", inSize)

            val outSize = reader.readUshort()
            out.annotate(2, "outs_size = %d", outSize)

            val triesCount = reader.readUshort()
            out.annotate(2, "tries_size = %d", triesCount)

            val debugInfoOffset = reader.readInt()
            out.annotate(4, "debug_info_off = 0x%x", debugInfoOffset)

            if (debugInfoOffset > 0) {
                addDebugInfoIdentity(debugInfoOffset, itemIdentity)
            }

            val instructionSize = reader.readSmallUint()
            out.annotate(4, "insns_size = 0x%x", instructionSize)

            return PreInstructionInfo(triesCount, instructionSize)
        }

        protected fun annotateInstructions(
            out: AnnotatedBytes,
            reader: DexReader<out DexBuffer>,
            instructionSize: Int
        ) {
            out.annotate(0, "instructions:")
            out.indent()

            out.setLimit(out.cursor, out.cursor + instructionSize * 2)

            val end = reader.offset + instructionSize * 2
            try {
                while (reader.offset < end) {
                    val instruction = DexBackedInstruction.readFrom(dexFile, reader)

                    // if we read past the end of the instruction list
                    if (reader.offset > end) {
                        out.annotateTo(end, "truncated instruction")
                        reader.offset = end
                    } else {
                        when (instruction.opcode.format) {
                            Format.Format10x -> annotateInstruction10x(out, instruction)
                            Format.Format35c ->
                                annotateInstruction35c(out, instruction as Instruction35c)
                            Format.Format3rc ->
                                annotateInstruction3rc(out, instruction as Instruction3rc)
                            Format.ArrayPayload ->
                                annotateArrayPayload(out, instruction as ArrayPayload)
                            Format.PackedSwitchPayload ->
                                annotatePackedSwitchPayload(
                                    out, instruction as PackedSwitchPayload
                                )
                            Format.SparseSwitchPayload ->
                                annotateSparseSwitchPayload(
                                    out, instruction as SparseSwitchPayload
                                )
                            else -> annotateDefaultInstruction(out, instruction)
                        }
                    }

                    assert(reader.offset == out.cursor)
                }
            } catch (ex: ExceptionWithContext) {
                ex.printStackTrace(System.err)
                out.annotate(0, "annotation error: %s", ex.message)
                out.moveTo(end)
                reader.offset = end
            } finally {
                out.clearLimit()
                out.deindent()
            }
        }

        protected fun annotatePostInstructionFields(
            out: AnnotatedBytes,
            reader: DexReader<out DexBuffer>,
            triesCount: Int
        ) {
            if (triesCount > 0) {
                if ((reader.offset % 4) != 0) {
                    reader.readUshort()
                    out.annotate(2, "padding")
                }

                out.annotate(0, "try_items:")
                out.indent()
                try {
                    for (i in 0 until triesCount) {
                        out.annotate(0, "try_item[%d]:", i)
                        out.indent()
                        try {
                            val startAddr = reader.readSmallUint()
                            out.annotate(4, "start_addr = 0x%x", startAddr)

                            val instructionCount = reader.readUshort()
                            out.annotate(2, "insn_count = 0x%x", instructionCount)

                            val handlerOffset = reader.readUshort()
                            out.annotate(2, "handler_off = 0x%x", handlerOffset)
                        } finally {
                            out.deindent()
                        }
                    }
                } finally {
                    out.deindent()
                }

                val handlerListCount = reader.readSmallUleb128()
                out.annotate(0, "encoded_catch_handler_list:")
                out.annotateTo(reader.offset, "size = %d", handlerListCount)
                out.indent()
                try {
                    for (i in 0 until handlerListCount) {
                        out.annotate(0, "encoded_catch_handler[%d]", i)
                        out.indent()
                        try {
                            var handlerCount = reader.readSleb128()
                            out.annotateTo(reader.offset, "size = %d", handlerCount)
                            val hasCatchAll = handlerCount <= 0
                            handlerCount = Math.abs(handlerCount)
                            if (handlerCount != 0) {
                                out.annotate(0, "handlers:")
                                out.indent()
                                try {
                                    for (j in 0 until handlerCount) {
                                        out.annotate(0, "encoded_type_addr_pair[%d]", i)
                                        out.indent()
                                        try {
                                            val typeIndex = reader.readSmallUleb128()
                                            out.annotateTo(
                                                reader.offset,
                                                TypeIdItem.getReferenceAnnotation(
                                                    dexFile, typeIndex
                                                )
                                            )

                                            val handlerAddress = reader.readSmallUleb128()
                                            out.annotateTo(
                                                reader.offset, "addr = 0x%x", handlerAddress
                                            )
                                        } finally {
                                            out.deindent()
                                        }
                                    }
                                } finally {
                                    out.deindent()
                                }
                            }
                            if (hasCatchAll) {
                                val catchAllAddress = reader.readSmallUleb128()
                                out.annotateTo(
                                    reader.offset, "catch_all_addr = 0x%x", catchAllAddress
                                )
                            }
                        } finally {
                            out.deindent()
                        }
                    }
                } finally {
                    out.deindent()
                }
            }
        }

        override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
            try {
                val reader: DexReader<out DexBuffer> = dexFile.buffer.readerAt(out.cursor)

                val info = annotatePreInstructionFields(out, reader, itemIdentity)
                annotateInstructions(out, reader, info.instructionSize)
                annotatePostInstructionFields(out, reader, info.triesCount)
            } catch (ex: ExceptionWithContext) {
                out.annotate(0, "annotation error: %s", ex.message)
            }
        }

        private fun formatRegister(registerNum: Int): String {
            return String.format("v%d", registerNum)
        }

        private fun annotateInstruction10x(out: AnnotatedBytes, instruction: Instruction) {
            out.annotate(2, instruction.opcode.name)
        }

        private fun annotateInstruction35c(
            out: AnnotatedBytes,
            instruction: Instruction35c
        ) {
            val args = ArrayList<String>()

            val registerCount = instruction.registerCount
            if (registerCount == 1) {
                args.add(formatRegister(instruction.registerC))
            } else if (registerCount == 2) {
                args.add(formatRegister(instruction.registerC))
                args.add(formatRegister(instruction.registerD))
            } else if (registerCount == 3) {
                args.add(formatRegister(instruction.registerC))
                args.add(formatRegister(instruction.registerD))
                args.add(formatRegister(instruction.registerE))
            } else if (registerCount == 4) {
                args.add(formatRegister(instruction.registerC))
                args.add(formatRegister(instruction.registerD))
                args.add(formatRegister(instruction.registerE))
                args.add(formatRegister(instruction.registerF))
            } else if (registerCount == 5) {
                args.add(formatRegister(instruction.registerC))
                args.add(formatRegister(instruction.registerD))
                args.add(formatRegister(instruction.registerE))
                args.add(formatRegister(instruction.registerF))
                args.add(formatRegister(instruction.registerG))
            }

            out.annotate(
                6, String.format(
                    "%s {%s}, %s",
                    instruction.opcode.name, StringUtils.join(args, ", "), instruction.reference
                )
            )
        }

        private fun annotateInstruction3rc(
            out: AnnotatedBytes,
            instruction: Instruction3rc
        ) {
            val startRegister = instruction.startRegister
            val endRegister = startRegister + instruction.registerCount - 1
            out.annotate(
                6, String.format(
                    "%s {%s .. %s}, %s",
                    instruction.opcode.name, formatRegister(startRegister),
                    formatRegister(endRegister),
                    instruction.reference
                )
            )
        }

        private fun annotateDefaultInstruction(
            out: AnnotatedBytes,
            instruction: Instruction
        ) {
            val args = ArrayList<String>()

            if (instruction is OneRegisterInstruction) {
                args.add(formatRegister(instruction.registerA))
                if (instruction is TwoRegisterInstruction) {
                    args.add(formatRegister(instruction.registerB))
                    if (instruction is ThreeRegisterInstruction) {
                        args.add(formatRegister(instruction.registerC))
                    }
                }
            } else if (instruction is VerificationErrorInstruction) {
                val verificationError = VerificationError.getVerificationErrorName(
                    instruction.verificationError
                )
                if (verificationError != null) {
                    args.add(verificationError)
                } else {
                    args.add("invalid verification error type")
                }
            }

            if (instruction is ReferenceInstruction) {
                val reference = instruction.reference

                val referenceString: String =
                    if (instruction.referenceType == ReferenceType.STRING) {
                        DexFormatter.INSTANCE.getQuotedString(reference as StringReference)
                    } else {
                        instruction.reference.toString()
                    }

                args.add(referenceString)
            } else if (instruction is OffsetInstruction) {
                val offset = instruction.codeOffset
                val sign = if (offset >= 0) "+" else "-"
                args.add(String.format("%s0x%x", sign, Math.abs(offset)))
            } else if (instruction is NarrowLiteralInstruction) {
                val value = instruction.narrowLiteral
                if (NumberUtils.isLikelyFloat(value)) {
                    args.add(String.format("%d # %f", value, Float.fromBits(value)))
                } else {
                    args.add(String.format("%d", value))
                }
            } else if (instruction is WideLiteralInstruction) {
                val value = instruction.wideLiteral
                if (NumberUtils.isLikelyDouble(value)) {
                    args.add(String.format("%d # %f", value, Double.fromBits(value)))
                } else {
                    args.add(String.format("%d", value))
                }
            } else if (instruction is FieldOffsetInstruction) {
                val fieldOffset = instruction.fieldOffset
                args.add(String.format("field@0x%x", fieldOffset))
            } else if (instruction is VtableIndexInstruction) {
                val vtableIndex = instruction.vtableIndex
                args.add(String.format("vtable@%d", vtableIndex))
            } else if (instruction is InlineIndexInstruction) {
                val inlineIndex = instruction.inlineIndex
                args.add(String.format("inline@%d", inlineIndex))
            }

            out.annotate(
                instruction.codeUnits * 2, "%s %s",
                instruction.opcode.name, StringUtils.join(args, ", ")
            )
        }

        private fun annotateArrayPayload(
            out: AnnotatedBytes,
            instruction: ArrayPayload
        ) {
            val elements = instruction.arrayElements
            val elementWidth = instruction.elementWidth

            out.annotate(2, instruction.opcode.name)
            out.indent()
            out.annotate(2, "element_width = %d", elementWidth)
            out.annotate(4, "size = %d", elements.size)
            if (elements.size > 0) {
                out.annotate(0, "elements:")
            }
            out.indent()
            if (elements.size > 0) {
                for (i in elements.indices) {
                    if (elementWidth == 8) {
                        val value = elements[i].toLong()
                        if (NumberUtils.isLikelyDouble(value)) {
                            out.annotate(
                                elementWidth, "element[%d] = %d # %f", i, value,
                                Double.fromBits(value)
                            )
                        } else {
                            out.annotate(elementWidth, "element[%d] = %d", i, value)
                        }
                    } else {
                        val value = elements[i].toInt()
                        if (NumberUtils.isLikelyFloat(value)) {
                            out.annotate(
                                elementWidth, "element[%d] = %d # %f", i, value,
                                Float.fromBits(value)
                            )
                        } else {
                            out.annotate(elementWidth, "element[%d] = %d", i, value)
                        }
                    }
                }
            }
            if (out.cursor % 2 != 0) {
                out.annotate(1, "padding")
            }
            out.deindent()
            out.deindent()
        }

        private fun annotatePackedSwitchPayload(
            out: AnnotatedBytes,
            instruction: PackedSwitchPayload
        ) {
            val elements = instruction.switchElements

            out.annotate(2, instruction.opcode.name)
            out.indent()

            out.annotate(2, "size = %d", elements.size)
            if (elements.size == 0) {
                out.annotate(4, "first_key")
            } else {
                out.annotate(4, "first_key = %d", elements[0].key)
                out.annotate(0, "targets:")
                out.indent()
                for (i in elements.indices) {
                    out.annotate(4, "target[%d] = %d", i, elements[i].offset)
                }
                out.deindent()
            }
            out.deindent()
        }

        private fun annotateSparseSwitchPayload(
            out: AnnotatedBytes,
            instruction: SparseSwitchPayload
        ) {
            val elements = instruction.switchElements

            out.annotate(2, instruction.opcode.name)
            out.indent()
            out.annotate(2, "size = %d", elements.size)
            if (elements.size > 0) {
                out.annotate(0, "keys:")
                out.indent()
                for (i in elements.indices) {
                    out.annotate(4, "key[%d] = %d", i, elements[i].key)
                }
                out.deindent()
                out.annotate(0, "targets:")
                out.indent()
                for (i in elements.indices) {
                    out.annotate(4, "target[%d] = %d", i, elements[i].offset)
                }
                out.deindent()
            }
            out.deindent()
        }

        private fun addDebugInfoIdentity(debugInfoOffset: Int, methodString: String?) {
            val annotator = debugInfoAnnotator ?: return
            if (methodString != null) {
                annotator.setItemIdentity(debugInfoOffset, methodString)
            }
        }
    }
}
