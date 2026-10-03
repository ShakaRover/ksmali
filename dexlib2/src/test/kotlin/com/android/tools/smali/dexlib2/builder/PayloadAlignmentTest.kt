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

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderArrayPayload
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderPackedSwitchPayload
import com.android.tools.smali.dexlib2.builder.instruction.BuilderSparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import org.junit.Assert
import org.junit.Test

class PayloadAlignmentTest {

    @Test
    fun testPayloadAlignmentRemoveNop() {
        val implBuilder = MethodImplementationBuilder(10)

        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        implBuilder.addInstruction(BuilderArrayPayload(4, null))

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        Assert.assertEquals(instructions.size, 1)

        val instruction = instructions[0]

        Assert.assertEquals(instruction.opcode, Opcode.ARRAY_PAYLOAD)
    }

    @Test
    fun testPayloadAlignmentAddNop() {
        val implBuilder = MethodImplementationBuilder(10)

        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderArrayPayload(4, null))

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        Assert.assertEquals(instructions.size, 3)

        var instruction = instructions[0]
        Assert.assertEquals(instruction.opcode, Opcode.MOVE)

        instruction = instructions[1]
        Assert.assertEquals(instruction.opcode, Opcode.NOP)

        instruction = instructions[2]
        Assert.assertEquals(instruction.opcode, Opcode.ARRAY_PAYLOAD)
    }

    @Test
    fun testPayloadAlignmentRemoveNopWithReferent() {
        val implBuilder = MethodImplementationBuilder(10)

        val label = implBuilder.getLabel("array_payload")
        implBuilder.addInstruction(BuilderInstruction31t(Opcode.FILL_ARRAY_DATA, 0, label))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        implBuilder.addLabel("array_payload")
        implBuilder.addInstruction(BuilderArrayPayload(4, null))

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        checkInstructions(
            instructions,
            arrayOf(Opcode.FILL_ARRAY_DATA, Opcode.MOVE, Opcode.MOVE, Opcode.MOVE, Opcode.ARRAY_PAYLOAD)
        )

        val referent = instructions[0] as Instruction31t
        Assert.assertEquals(6, referent.codeOffset)
    }

    @Test
    fun testPayloadAlignmentAddNopWithReferent() {
        val implBuilder = MethodImplementationBuilder(10)

        val label = implBuilder.getLabel("array_payload")
        implBuilder.addInstruction(BuilderInstruction31t(Opcode.FILL_ARRAY_DATA, 0, label))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addInstruction(BuilderInstruction12x(Opcode.MOVE, 0, 0))
        implBuilder.addLabel("array_payload")
        implBuilder.addInstruction(BuilderArrayPayload(4, null))

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        checkInstructions(
            instructions,
            arrayOf(
                Opcode.FILL_ARRAY_DATA, Opcode.MOVE, Opcode.MOVE, Opcode.MOVE, Opcode.MOVE,
                Opcode.NOP, Opcode.ARRAY_PAYLOAD
            )
        )

        val referent = instructions[0] as Instruction31t
        Assert.assertEquals(8, referent.codeOffset)
    }

    companion object {
        private fun checkInstructions(instructions: List<Instruction>, expectedOpcodes: Array<Opcode>) {
            Assert.assertEquals(expectedOpcodes.size, instructions.size)

            for (i in expectedOpcodes.indices) {
                Assert.assertEquals(instructions[i].opcode, expectedOpcodes[i])
            }
        }
    }

    @Test
    fun testPackedSwitchAlignment() {
        val implBuilder = MethodImplementationBuilder(10)

        implBuilder.addLabel("switch_target_1")
        implBuilder.addInstruction(BuilderInstruction10t(Opcode.GOTO, implBuilder.getLabel("goto_target")))

        implBuilder.addLabel("switch_payload")
        implBuilder.addInstruction(
            BuilderPackedSwitchPayload(
                0,
                listOf(
                    implBuilder.getLabel("switch_target_1"),
                    implBuilder.getLabel("switch_target_2"),
                    implBuilder.getLabel("switch_target_3")
                )
            )
        )

        implBuilder.addLabel("goto_target")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addLabel("switch_target_2")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addLabel("switch_target_3")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addInstruction(
            BuilderInstruction31t(Opcode.PACKED_SWITCH, 0, implBuilder.getLabel("switch_payload"))
        )

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        checkInstructions(
            instructions,
            arrayOf(
                Opcode.GOTO, Opcode.NOP, Opcode.PACKED_SWITCH_PAYLOAD, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.PACKED_SWITCH
            )
        )

        val gotoInstruction = instructions[0] as OffsetInstruction
        Assert.assertEquals(12, gotoInstruction.codeOffset)

        val payload = instructions[2] as PackedSwitchPayload
        Assert.assertEquals(3, payload.switchElements.size)
        Assert.assertEquals(-16, payload.switchElements[0].offset)
        Assert.assertEquals(-2, payload.switchElements[1].offset)
        Assert.assertEquals(-1, payload.switchElements[2].offset)

        val referent = instructions[7] as OffsetInstruction
        Assert.assertEquals(-14, referent.codeOffset)
    }

    @Test
    fun testSparseSwitchAlignment() {
        val implBuilder = MethodImplementationBuilder(10)

        implBuilder.addLabel("switch_target_1")
        implBuilder.addInstruction(BuilderInstruction10t(Opcode.GOTO, implBuilder.getLabel("goto_target")))

        implBuilder.addLabel("switch_payload")
        implBuilder.addInstruction(
            BuilderSparseSwitchPayload(
                listOf(
                    SwitchLabelElement(0, implBuilder.getLabel("switch_target_1")),
                    SwitchLabelElement(5, implBuilder.getLabel("switch_target_2")),
                    SwitchLabelElement(10, implBuilder.getLabel("switch_target_3"))
                )
            )
        )

        implBuilder.addLabel("goto_target")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addLabel("switch_target_2")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addLabel("switch_target_3")
        implBuilder.addInstruction(BuilderInstruction10x(Opcode.NOP))

        implBuilder.addInstruction(
            BuilderInstruction31t(Opcode.SPARSE_SWITCH, 0, implBuilder.getLabel("switch_payload"))
        )

        val instructions = implBuilder.getMethodImplementation().instructions.toList()

        checkInstructions(
            instructions,
            arrayOf(
                Opcode.GOTO, Opcode.NOP, Opcode.SPARSE_SWITCH_PAYLOAD, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.SPARSE_SWITCH
            )
        )

        val gotoInstruction = instructions[0] as OffsetInstruction
        Assert.assertEquals(16, gotoInstruction.codeOffset)

        val payload = instructions[2] as SparseSwitchPayload
        Assert.assertEquals(3, payload.switchElements.size)
        Assert.assertEquals(-20, payload.switchElements[0].offset)
        Assert.assertEquals(-2, payload.switchElements[1].offset)
        Assert.assertEquals(-1, payload.switchElements[2].offset)

        val referent = instructions[7] as OffsetInstruction
        Assert.assertEquals(-18, referent.codeOffset)
    }
}
