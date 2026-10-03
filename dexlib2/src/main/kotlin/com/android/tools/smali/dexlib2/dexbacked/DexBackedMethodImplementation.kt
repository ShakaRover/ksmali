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

package com.android.tools.smali.dexlib2.dexbacked

import com.android.tools.smali.dexlib2.dexbacked.instruction.DexBackedInstruction
import com.android.tools.smali.dexlib2.dexbacked.raw.CodeItem
import com.android.tools.smali.dexlib2.dexbacked.util.DebugInfo
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeListIterator
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeLookaheadIterator
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.util.alignOffset
import com.android.tools.smali.util.ExceptionWithContext
import java.util.Collections

open class DexBackedMethodImplementation internal constructor(
    val dexFile: DexBackedDexFile,
    val method: DexBackedMethod,
    protected val codeOffset: Int
) : MethodImplementation {
    override val registerCount: Int
        get() = dexFile.dataBuffer.readUshort(codeOffset)

    open val instructionsSize: Int
        get() = dexFile.dataBuffer.readSmallUint(codeOffset + CodeItem.INSTRUCTION_COUNT_OFFSET)

    protected open val instructionsStartOffset: Int
        get() = codeOffset + CodeItem.INSTRUCTION_START_OFFSET

    override val instructions: Iterable<Instruction>
        get() {
            // instructionsSize is the number of 16-bit code units in the instruction list, not the number of instructions
            val instructionsSize = this.instructionsSize

            val instructionsStartOffset = this.instructionsStartOffset
            val endOffset = instructionsStartOffset + (instructionsSize * 2)
            return object : Iterable<Instruction> {
                override fun iterator(): MutableIterator<Instruction> {
                    return object : VariableSizeLookaheadIterator<Instruction>(
                        dexFile.dataBuffer, instructionsStartOffset
                    ) {
                        override fun readNextItem(reader: DexReader<out DexBuffer>): Instruction? {
                            if (reader.offset >= endOffset) {
                                return endOfData()
                            }

                            val instruction = DexBackedInstruction.readFrom(dexFile, reader)

                            // Does the instruction extend past the end of the method?
                            val offset = reader.offset
                            if (offset > endOffset || offset < 0) {
                                throw ExceptionWithContext(
                                    "The last instruction in method %s is truncated", method
                                )
                            }
                            return instruction
                        }
                    }
                }
            }
        }

    protected open val triesSize: Int
        get() = dexFile.dataBuffer.readUshort(codeOffset + CodeItem.TRIES_SIZE_OFFSET)

    override val tryBlocks: List<DexBackedTryBlock>
        get() {
            if (triesSize > 0) {
                val instructionsSize = this.instructionsSize
                val triesStartOffset = alignOffset(
                    instructionsStartOffset + (instructionsSize * 2), 4
                )
                val handlersStartOffset =
                    triesStartOffset + triesSize * CodeItem.TryItem.ITEM_SIZE

                return object : FixedSizeList<DexBackedTryBlock>() {
                    override val size: Int
                        get() = triesSize

                    override fun readItem(index: Int): DexBackedTryBlock {
                        return DexBackedTryBlock(
                            dexFile,
                            triesStartOffset + index * CodeItem.TryItem.ITEM_SIZE,
                            handlersStartOffset
                        )
                    }
                }
            }
            return emptyList()
        }

    open val debugOffset: Int
        get() = dexFile.dataBuffer.readInt(codeOffset + CodeItem.DEBUG_INFO_OFFSET)

    private fun getDebugInfo(): DebugInfo {
        val debugOffset = this.debugOffset

        if (debugOffset == -1 || debugOffset == 0) {
            return DebugInfo.newOrEmpty(dexFile, 0, this)
        }
        if (debugOffset < 0) {
            System.err.println("${method}: Invalid debug offset")
            return DebugInfo.newOrEmpty(dexFile, 0, this)
        }
        if ((debugOffset + dexFile.baseDataOffset) >= dexFile.buffer.buf.size) {
            System.err.println("${method}: Invalid debug offset")
            return DebugInfo.newOrEmpty(dexFile, 0, this)
        }
        return DebugInfo.newOrEmpty(dexFile, debugOffset, this)
    }

    override val debugItems: Iterable<DebugItem>
        get() = getDebugInfo()

    fun getParameterNames(dexReader: DexReader<out DexBuffer>?): MutableIterator<String?> {
        return getDebugInfo().getParameterNames(dexReader)
    }

    /**
     * Calculate and return the private size of a method implementation.
     *
     * Calculated as: instructions size + try-catch size
     *
     * <p>Note: debug info can be shared among multiple methods so it is not
     * included in the method size.
     *
     * @return size in bytes
     */
    fun getSize(): Int {
        //set last offset just before bytecode instructions (after insns_size)
        var lastOffset = instructionsStartOffset

        //set code_item ending offset to the end of instructions list (insns_size * ushort)
        lastOffset += instructionsSize * 2

        //read any exception handlers and move code_item offset to the end
        for (tryBlock in tryBlocks) {
            val tryHandlerIter = tryBlock.exceptionHandlers.iterator()
            while (tryHandlerIter.hasNext()) {
                tryHandlerIter.next()
            }
            lastOffset = (tryHandlerIter as VariableSizeListIterator<*>).getReaderOffset()
        }

        //method impl size = code_item size
        return lastOffset - codeOffset
    }
}
