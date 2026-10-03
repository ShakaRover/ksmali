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

package com.android.tools.smali.dexlib2.dexbacked.instruction

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.util.ExceptionWithContext

class DexBackedArrayPayload(
    dexFile: DexBackedDexFile,
    instructionStart: Int
) : DexBackedInstruction(dexFile, OPCODE, instructionStart), ArrayPayload {
    override val elementWidth: Int

    @JvmField
    val elementCount: Int

    init {
        val localElementWidth =
            dexFile.dataBuffer.readUshort(instructionStart + ELEMENT_WIDTH_OFFSET)

        if (localElementWidth == 0) {
            elementWidth = 1
            elementCount = 0
        } else {
            elementWidth = localElementWidth

            elementCount =
                dexFile.dataBuffer.readSmallUint(instructionStart + ELEMENT_COUNT_OFFSET)
            if (elementWidth.toLong() * elementCount > Int.MAX_VALUE) {
                throw ExceptionWithContext(
                    "Invalid array-payload instruction: element width*count overflows"
                )
            }
        }
    }

    override val arrayElements: List<Number>
        get() {
            val elementsStart = instructionStart + ELEMENTS_OFFSET

            if (elementCount == 0) {
                return emptyList()
            }

            abstract class ReturnedList : FixedSizeList<Number>() {
                override val size: Int
                    get() = elementCount
            }

            return when (elementWidth) {
                1 -> object : ReturnedList() {
                    override fun readItem(index: Int): Number =
                        dexFile.dataBuffer.readByte(elementsStart + index)
                }
                2 -> object : ReturnedList() {
                    override fun readItem(index: Int): Number =
                        dexFile.dataBuffer.readShort(elementsStart + index * 2)
                }
                4 -> object : ReturnedList() {
                    override fun readItem(index: Int): Number =
                        dexFile.dataBuffer.readInt(elementsStart + index * 4)
                }
                8 -> object : ReturnedList() {
                    override fun readItem(index: Int): Number =
                        dexFile.dataBuffer.readLong(elementsStart + index * 8)
                }
                else -> throw ExceptionWithContext("Invalid element width: %d", elementWidth)
            }
        }

    override val codeUnits: Int
        get() = 4 + (elementWidth * elementCount + 1) / 2

    companion object {
        @JvmField
        val OPCODE: Opcode = Opcode.ARRAY_PAYLOAD

        private const val ELEMENT_WIDTH_OFFSET = 2
        private const val ELEMENT_COUNT_OFFSET = 4
        private const val ELEMENTS_OFFSET = 8
    }
}
