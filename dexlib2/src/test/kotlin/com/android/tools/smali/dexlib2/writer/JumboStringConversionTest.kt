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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class JumboStringConversionTest {
    @Test
    @Throws(IOException::class)
    fun testJumboStringConversion() {
        val dexBuilder = DexBuilder(Opcodes.getDefault())

        val methodBuilder = MethodImplementationBuilder(1)
        for (i in 0 until 66000) {
            methodBuilder.addInstruction(
                BuilderInstruction21c(Opcode.CONST_STRING, 0,
                    dexBuilder.internStringReference("%08d".format(i)))
            )
        }
        methodBuilder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        dexBuilder.internClassDef(
            "Ltest;",
            0,
            "Ljava/lang/Object;",
            null,
            null,
            ImmutableSet.of<Annotation>(),
            null,
            ImmutableList.of(
                dexBuilder.internMethod(
                    "Ltest;",
                    "test",
                    null,
                    "V",
                    0,
                    ImmutableSet.of<Annotation>(),
                    ImmutableSet.of(),
                    methodBuilder.getMethodImplementation()
                )
            )
        )

        val dexStore = MemoryDataStore()
        dexBuilder.writeTo(dexStore)

        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexStore.buffer)

        val classDef = dexFile.classes.firstOrNull()
        Assert.assertNotNull(classDef)

        val method = classDef!!.methods.firstOrNull()
        Assert.assertNotNull(method)

        val impl = method!!.implementation
        Assert.assertNotNull(impl)

        val instructions = impl!!.instructions.toList()
        Assert.assertEquals(66001, instructions.size)

        for (i in 0 until 65536) {
            Assert.assertEquals(Opcode.CONST_STRING, instructions[i].opcode)
            Assert.assertEquals("%08d".format(i),
                (instructions[i] as ReferenceInstruction).reference.let { it as StringReference }.string)
        }
        for (i in 65536 until 66000) {
            Assert.assertEquals(Opcode.CONST_STRING_JUMBO, instructions[i].opcode)
            Assert.assertEquals("%08d".format(i),
                (instructions[i] as ReferenceInstruction).reference.let { it as StringReference }.string)
        }
        Assert.assertEquals(Opcode.RETURN_VOID, instructions[66000].opcode)
    }

    @Test
    @Throws(IOException::class)
    fun testJumboStringConversion_NonMethodBuilder() {
        val dexBuilder = DexBuilder(Opcodes.getDefault())

        val instructions = mutableListOf<Instruction>()
        for (i in 0 until 66000) {
            val ref = dexBuilder.internStringReference("%08d".format(i))

            instructions.add(object : Instruction21c {
                override val registerA: Int
                    get() = 0

                override val reference: Reference
                    get() = ref

                override val referenceType: Int
                    get() = ReferenceType.STRING

                override val opcode: Opcode
                    get() = Opcode.CONST_STRING

                override val codeUnits: Int
                    get() = opcode.format.size / 2
            })
        }
        instructions.add(ImmutableInstruction10x(Opcode.RETURN_VOID))

        val methodImpl = object : MethodImplementation {
            override val registerCount: Int
                get() = 1

            override val instructions: Iterable<Instruction>
                get() = instructions

            override val tryBlocks: List<TryBlock<out ExceptionHandler>>
                get() = ImmutableList.of()

            override val debugItems: Iterable<DebugItem>
                get() = ImmutableList.of()
        }

        dexBuilder.internClassDef(
            "Ltest;",
            0,
            "Ljava/lang/Object;",
            null,
            null,
            ImmutableSet.of<Annotation>(),
            null,
            ImmutableList.of(
                dexBuilder.internMethod(
                    "Ltest;",
                    "test",
                    null,
                    "V",
                    0,
                    ImmutableSet.of<Annotation>(),
                    ImmutableSet.of(),
                    methodImpl
                )
            )
        )

        val dexStore = MemoryDataStore()
        dexBuilder.writeTo(dexStore)

        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexStore.buffer)

        val classDef = dexFile.classes.firstOrNull()
        Assert.assertNotNull(classDef)

        val method = classDef!!.methods.firstOrNull()
        Assert.assertNotNull(method)

        val impl = method!!.implementation
        Assert.assertNotNull(impl)

        val actualInstructions = impl!!.instructions.toList()
        Assert.assertEquals(66001, actualInstructions.size)

        for (i in 0 until 65536) {
            Assert.assertEquals(Opcode.CONST_STRING, actualInstructions[i].opcode)
            Assert.assertEquals("%08d".format(i),
                (actualInstructions[i] as ReferenceInstruction).reference.let { it as StringReference }.string)
        }
        for (i in 65536 until 66000) {
            Assert.assertEquals(Opcode.CONST_STRING_JUMBO, actualInstructions[i].opcode)
            Assert.assertEquals("%08d".format(i),
                (actualInstructions[i] as ReferenceInstruction).reference.let { it as StringReference }.string)
        }
        Assert.assertEquals(Opcode.RETURN_VOID, actualInstructions[66000].opcode)
    }
}
