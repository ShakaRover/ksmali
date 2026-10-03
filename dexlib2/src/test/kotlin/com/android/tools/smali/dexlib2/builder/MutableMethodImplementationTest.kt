/*
 * Copyright 2015, Google LLC
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
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction32x
import org.junit.Assert
import org.junit.Test

class MutableMethodImplementationTest {

    @Test
    fun testTryEndAtEndOfMethod() {
        val builder = MethodImplementationBuilder(10)

        val startLabel = builder.addLabel("start")
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction32x(Opcode.MOVE_16, 0, 0))
        val endLabel = builder.addLabel("end")

        builder.addCatch(startLabel, endLabel, startLabel)

        var methodImplementation = builder.getMethodImplementation()

        Assert.assertEquals(0, methodImplementation.tryBlocks[0].startCodeAddress)
        Assert.assertEquals(8, methodImplementation.tryBlocks[0].codeUnitCount)

        methodImplementation = MutableMethodImplementation(methodImplementation)

        Assert.assertEquals(0, methodImplementation.tryBlocks[0].startCodeAddress)
        Assert.assertEquals(8, methodImplementation.tryBlocks[0].codeUnitCount)
    }

    @Test
    fun testNewLabelByAddress() {
        val builder = MethodImplementationBuilder(10)

        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction32x(Opcode.MOVE_16, 0, 0))

        val mutableMethodImplementation =
            MutableMethodImplementation(builder.getMethodImplementation())

        mutableMethodImplementation.addCatch(
            mutableMethodImplementation.newLabelForAddress(0),
            mutableMethodImplementation.newLabelForAddress(8),
            mutableMethodImplementation.newLabelForAddress(1)
        )

        Assert.assertEquals(0, mutableMethodImplementation.tryBlocks[0].startCodeAddress)
        Assert.assertEquals(8, mutableMethodImplementation.tryBlocks[0].codeUnitCount)
        Assert.assertEquals(
            1,
            mutableMethodImplementation.tryBlocks[0].exceptionHandlers[0].handlerCodeAddress
        )
    }

    @Test
    fun testNewLabelByIndex() {
        val builder = MethodImplementationBuilder(10)

        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction10x(Opcode.NOP))
        builder.addInstruction(BuilderInstruction32x(Opcode.MOVE_16, 0, 0))

        val mutableMethodImplementation =
            MutableMethodImplementation(builder.getMethodImplementation())

        mutableMethodImplementation.addCatch(
            mutableMethodImplementation.newLabelForIndex(0),
            mutableMethodImplementation.newLabelForIndex(6),
            mutableMethodImplementation.newLabelForIndex(1)
        )

        Assert.assertEquals(0, mutableMethodImplementation.tryBlocks[0].startCodeAddress)
        Assert.assertEquals(8, mutableMethodImplementation.tryBlocks[0].codeUnitCount)
        Assert.assertEquals(
            1,
            mutableMethodImplementation.tryBlocks[0].exceptionHandlers[0].handlerCodeAddress
        )
    }
}
