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
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import org.junit.Assert
import org.junit.Test

class FixOffsetsTest {
    @Test
    fun testFixOffsets() {
        val builder = MethodImplementationBuilder(1)

        val firstGotoTarget = builder.getLabel("firstGotoTarget")
        builder.addInstruction(BuilderInstruction10t(Opcode.GOTO, firstGotoTarget))

        builder.addLineNumber(1)

        for (i in 0 until 250) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLabel("tryStart")

        builder.addLineNumber(2)

        for (i in 0 until 250) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLineNumber(3)

        val secondGotoTarget = builder.getLabel("secondGotoTarget")
        builder.addInstruction(BuilderInstruction10t(Opcode.GOTO, secondGotoTarget))

        builder.addLineNumber(4)
        builder.addLabel("handler")

        for (i in 0 until 500) {
            builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        }

        builder.addLineNumber(5)

        builder.addLabel("tryEnd")

        builder.addLabel("firstGotoTarget")
        builder.addLabel("secondGotoTarget")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val tryStart = builder.getLabel("tryStart")
        val tryEnd = builder.getLabel("tryEnd")
        val handler = builder.getLabel("handler")

        builder.addCatch(tryStart, tryEnd, handler)

        val impl = builder.getMethodImplementation()

        val instructions = impl.instructions.toList()
        Assert.assertEquals(1003, instructions.size)

        Assert.assertEquals(Opcode.GOTO_16, instructions[0].opcode)
        Assert.assertEquals(1004, (instructions[0] as OffsetInstruction).codeOffset)

        Assert.assertEquals(Opcode.GOTO_16, instructions[501].opcode)
        Assert.assertEquals(502, (instructions[501] as OffsetInstruction).codeOffset)

        val exceptionHandlers = impl.tryBlocks

        Assert.assertEquals(1, exceptionHandlers.size)
        Assert.assertEquals(252, exceptionHandlers[0].startCodeAddress)
        Assert.assertEquals(752, exceptionHandlers[0].codeUnitCount)

        Assert.assertEquals(1, exceptionHandlers[0].exceptionHandlers.size)

        val exceptionHandler = exceptionHandlers[0].exceptionHandlers[0]
        Assert.assertEquals(504, exceptionHandler.handlerCodeAddress)

        val debugItems = impl.debugItems.toList()

        Assert.assertEquals(5, debugItems.size)

        Assert.assertEquals(1, (debugItems[0] as LineNumber).lineNumber)
        Assert.assertEquals(2, debugItems[0].codeAddress)

        Assert.assertEquals(2, (debugItems[1] as LineNumber).lineNumber)
        Assert.assertEquals(252, debugItems[1].codeAddress)

        Assert.assertEquals(3, (debugItems[2] as LineNumber).lineNumber)
        Assert.assertEquals(502, debugItems[2].codeAddress)

        Assert.assertEquals(4, (debugItems[3] as LineNumber).lineNumber)
        Assert.assertEquals(504, debugItems[3].codeAddress)

        Assert.assertEquals(5, (debugItems[4] as LineNumber).lineNumber)
        Assert.assertEquals(1004, debugItems[4].codeAddress)
    }
}
