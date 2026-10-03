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

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference
import com.android.tools.smali.util.ExceptionWithContext
import org.junit.Assert
import org.junit.Test

class InstructionOffsetMapTest {
    @Test
    fun testInstructionOffsetMap() {
        val instructions: List<ImmutableInstruction> = listOf(
                /*00: 0x00*/ ImmutableInstruction10t(Opcode.GOTO, 1),
                /*01: 0x01*/ ImmutableInstruction10x(Opcode.NOP),
                /*02: 0x02*/ ImmutableInstruction11n(Opcode.CONST_4, 2, 3),
                /*03: 0x03*/ ImmutableInstruction11x(Opcode.RETURN, 4),
                /*04: 0x04*/ ImmutableInstruction12x(Opcode.ARRAY_LENGTH, 5, 6),
                /*05: 0x05*/ ImmutableInstruction20t(Opcode.GOTO_16, 7),
                /*06: 0x07*/ ImmutableInstruction21c(Opcode.CONST_STRING, 8, ImmutableStringReference("blah")),
                /*07: 0x09*/ ImmutableInstruction21ih(Opcode.CONST_HIGH16, 9, 0x10000),
                /*08: 0x0b*/ ImmutableInstruction21lh(Opcode.CONST_WIDE_HIGH16, 10, 0x1000000000000L),
                /*09: 0x0d*/ ImmutableInstruction21s(Opcode.CONST_16, 11, 12),
                /*10: 0x0f*/ ImmutableInstruction21t(Opcode.IF_EQZ, 12, 13),
                /*11: 0x11*/ ImmutableInstruction22b(Opcode.ADD_INT_LIT8, 14, 15, 16),
                /*12: 0x13*/ ImmutableInstruction22c(Opcode.INSTANCE_OF, 0, 1, ImmutableTypeReference("Ltype;")),
                /*13: 0x15*/ ImmutableInstruction22s(Opcode.ADD_INT_LIT16, 2, 3, 17),
                /*14: 0x17*/ ImmutableInstruction22t(Opcode.IF_EQ, 4, 5, 18),
                /*15: 0x19*/ ImmutableInstruction22x(Opcode.MOVE_FROM16, 19, 20),
                /*16: 0x1b*/ ImmutableInstruction23x(Opcode.AGET, 21, 22, 23),
                /*17: 0x1d*/ ImmutableInstruction30t(Opcode.GOTO_32, 24),
                /*18: 0x20*/ ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO, 25, ImmutableStringReference("this is a string")),
                /*19: 0x23*/ ImmutableInstruction31i(Opcode.CONST, 26, 27),
                /*20: 0x26*/ ImmutableInstruction31t(Opcode.FILL_ARRAY_DATA, 28, 29),
                /*21: 0x29*/ ImmutableInstruction32x(Opcode.MOVE_16, 30, 31),
                /*22: 0x2c*/ ImmutableInstruction35c(Opcode.FILLED_NEW_ARRAY, 0, 0, 0, 0, 0, 0, ImmutableTypeReference("Ltype;")),
                /*23: 0x2f*/ ImmutableInstruction3rc(Opcode.FILLED_NEW_ARRAY_RANGE, 0, 0, ImmutableTypeReference("Ltype;")),
                /*24: 0x32*/ ImmutableInstruction51l(Opcode.CONST_WIDE, 32, 33),
                /*25: 0x37*/ ImmutableInstruction10t(Opcode.GOTO, 1)
        )
        val impl = ImmutableMethodImplementation(33, instructions, null, null)
        val instructionOffsetMap = InstructionOffsetMap(instructions)
        val expectedOffsets = intArrayOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07, 0x09, 0x0b, 0x0d, 0x0f, 0x11,
            0x13, 0x15, 0x17, 0x19, 0x1b, 0x1d, 0x20, 0x23, 0x26, 0x29, 0x2c, 0x2f, 0x32, 0x37
        )
        for (i in instructions.indices) {
            Assert.assertEquals(expectedOffsets[i].toLong(), instructionOffsetMap.getInstructionCodeOffset(i).toLong())
            Assert.assertEquals(i.toLong(), instructionOffsetMap.getInstructionIndexAtCodeOffset(expectedOffsets[i], true).toLong())
            Assert.assertEquals(i.toLong(), instructionOffsetMap.getInstructionIndexAtCodeOffset(expectedOffsets[i], false).toLong())
        }
        var instructionIndex = -1
        for (codeOffset in 0..expectedOffsets[expectedOffsets.size - 1]) {
            if (codeOffset == expectedOffsets[instructionIndex + 1]) {
                instructionIndex++
            } else {
                Assert.assertEquals(instructionIndex.toLong(), instructionOffsetMap.getInstructionIndexAtCodeOffset(codeOffset, false).toLong())
                try {
                    instructionOffsetMap.getInstructionIndexAtCodeOffset(codeOffset, true)
                    Assert.fail("Exception exception didn't occur for code offset 0x%x".format(codeOffset))
                } catch (ex: ExceptionWithContext) {
                    // expected exception
                }
            }
        }
        Assert.assertEquals((expectedOffsets.size - 1).toLong(),
            instructionOffsetMap.getInstructionIndexAtCodeOffset(expectedOffsets[expectedOffsets.size - 1] + 1, false).toLong())
        Assert.assertEquals((expectedOffsets.size - 1).toLong(),
            instructionOffsetMap.getInstructionIndexAtCodeOffset(expectedOffsets[expectedOffsets.size - 1] + 10, false).toLong())
    }
}
