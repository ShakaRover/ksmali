/*
 * Copyright 2013, Google LLC
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
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import org.junit.Assert
import org.junit.Test

class FixGotoTest {
    @Test
    fun testFixGotoToGoto16() {
        val builder = MethodImplementationBuilder(1)

        val gotoTarget = builder.getLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10t(Opcode.GOTO, gotoTarget))

        for (i in 0 until 500) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val impl = builder.getMethodImplementation()

        val instructions = impl.instructions.toList()
        Assert.assertEquals(502, instructions.size)

        Assert.assertEquals(Opcode.GOTO_16, instructions[0].opcode)
        Assert.assertEquals(502, (instructions[0] as OffsetInstruction).codeOffset)
    }

    @Test
    fun testFixGotoToGoto32() {
        val builder = MethodImplementationBuilder(1)

        val gotoTarget = builder.getLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10t(Opcode.GOTO, gotoTarget))

        for (i in 0 until 70000) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val impl = builder.getMethodImplementation()

        val instructions = impl.instructions.toList()
        Assert.assertEquals(70002, instructions.size)

        Assert.assertEquals(Opcode.GOTO_32, instructions[0].opcode)
        Assert.assertEquals(70003, (instructions[0] as OffsetInstruction).codeOffset)
    }

    @Test
    fun testFixGoto16ToGoto32() {
        val builder = MethodImplementationBuilder(1)

        val gotoTarget = builder.getLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction20t(Opcode.GOTO_16, gotoTarget))

        for (i in 0 until 70000) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val impl = builder.getMethodImplementation()

        val instructions = impl.instructions.toList()
        Assert.assertEquals(70002, instructions.size)

        Assert.assertEquals(Opcode.GOTO_32, instructions[0].opcode)
        Assert.assertEquals(70003, (instructions[0] as OffsetInstruction).codeOffset)
    }

    @Test
    fun testFixGotoCascading() {
        val builder = MethodImplementationBuilder(1)

        val goto16Target = builder.getLabel("goto16Target")
        builder.addInstruction(BuilderInstruction20t(Opcode.GOTO_16, goto16Target))

        for (i in 0 until 1000) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        val gotoTarget = builder.getLabel("gotoTarget")
        builder.addInstruction(BuilderInstruction10t(Opcode.GOTO, gotoTarget))

        for (i in 0 until 499) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("gotoTarget")

        for (i in 0 until 31265) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("goto16Target")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val impl = builder.getMethodImplementation()

        val instructions = impl.instructions.toList()
        Assert.assertEquals(32767, instructions.size)

        Assert.assertEquals(Opcode.GOTO_32, instructions[0].opcode)
        Assert.assertEquals(32769, (instructions[0] as OffsetInstruction).codeOffset)
    }
}
