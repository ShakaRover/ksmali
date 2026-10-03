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

package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.util.ExceptionWithContext
import java.util.Arrays

class InstructionOffsetMap(instructions: List<@JvmWildcard Instruction>) {
    private val instructionCodeOffsets: IntArray = IntArray(instructions.size)

    init {
        var codeOffset = 0
        for (i in instructions.indices) {
            instructionCodeOffsets[i] = codeOffset
            codeOffset += instructions[i].codeUnits
        }
    }

    fun getInstructionIndexAtCodeOffset(codeOffset: Int): Int {
        return getInstructionIndexAtCodeOffset(codeOffset, true)
    }

    fun getInstructionIndexAtCodeOffset(codeOffset: Int, exact: Boolean): Int {
        val index = Arrays.binarySearch(instructionCodeOffsets, codeOffset)
        if (index < 0) {
            if (exact) {
                throw InvalidInstructionOffset(codeOffset)
            } else {
                // This calculation would be incorrect if index was -1 (i.e. insertion point of 0). Luckily, we can
                // ignore this case, because codeOffset will always be non-negative, and the code offset of the first
                // instruction will always be 0.
                return index.inv() - 1
            }
        }
        return index
    }

    fun getInstructionCodeOffset(index: Int): Int {
        if (index < 0 || index >= instructionCodeOffsets.size) {
            throw InvalidInstructionIndex(index)
        }
        return instructionCodeOffsets[index]
    }

    class InvalidInstructionOffset(val instructionOffset: Int) :
        ExceptionWithContext("No instruction at offset %d", instructionOffset)

    class InvalidInstructionIndex(val instructionIndex: Int) :
        ExceptionWithContext("Instruction index out of bounds: %d", instructionIndex)
}
