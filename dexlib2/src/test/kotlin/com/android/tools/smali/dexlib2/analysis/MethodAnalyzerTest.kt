/*
 * Copyright 2016, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google LLC nor the names of its
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

package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class MethodAnalyzerTest {

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingEqz_art() {
        val builder = MethodImplementationBuilder(2)

        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("not_instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.forArtVersion(56), listOf(classDef))

        val classPath = ClassPath(mutableListOf(DexClassProvider(dexFile)), true, 56)
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Lmain;", analyzedInstructions.get(2).getPreInstructionRegisterType(1).type!!.type)

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
    }

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingEqz_dalvik() {
        val builder = MethodImplementationBuilder(2)

        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("not_instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.forApi(19), listOf(classDef))

        val classPath = ClassPath(DexClassProvider(dexFile))
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(2).getPreInstructionRegisterType(1).type!!.type)

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
    }

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingNez_art() {
        val builder = MethodImplementationBuilder(2)

        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_NEZ, 0, builder.getLabel("instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.forArtVersion(56), listOf(classDef))

        val classPath = ClassPath(mutableListOf(DexClassProvider(dexFile)), true, 56)
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(2).getPreInstructionRegisterType(1).type!!.type)

        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
    }

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingNez_dalvik() {
        val builder = MethodImplementationBuilder(2)

        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_NEZ, 0, builder.getLabel("instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.getDefault(), listOf(classDef))

        val classPath = ClassPath(DexClassProvider(dexFile))
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(2).getPreInstructionRegisterType(1).type!!.type)

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
    }

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingAfterMove_art() {
        val builder = MethodImplementationBuilder(3)

        builder.addInstruction(BuilderInstruction12x(Opcode.MOVE_OBJECT, 1, 2))
        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("not_instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.forArtVersion(56), listOf(classDef))

        val classPath = ClassPath(mutableListOf(DexClassProvider(dexFile)), true, 56)
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(2).type!!.type)

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(1).type!!.type)
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(2).type!!.type)
    }

    @Test
    @Throws(IOException::class)
    fun testInstanceOfNarrowingAfterMove_dalvik() {
        val builder = MethodImplementationBuilder(3)

        builder.addInstruction(BuilderInstruction12x(Opcode.MOVE_OBJECT, 1, 2))
        builder.addInstruction(BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                ImmutableTypeReference("Lmain;")))
        builder.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")))
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        builder.addLabel("not_instance_of")
        builder.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

        val methodImplementation = builder.getMethodImplementation()

        val method = ImmutableMethod("Lmain;", "narrowing",
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.value, null, null, methodImplementation)
        val classDef = ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
                null, null, null, listOf(method))
        val dexFile = ImmutableDexFile(Opcodes.getDefault(), listOf(classDef))

        val classPath = ClassPath(DexClassProvider(dexFile))
        val methodAnalyzer = MethodAnalyzer(classPath, method, null, false)

        val analyzedInstructions = methodAnalyzer.analyzedInstructions
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(1).type!!.type)
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(2).type!!.type)

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(1).type!!.type)
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(2).type!!.type)
    }
}
