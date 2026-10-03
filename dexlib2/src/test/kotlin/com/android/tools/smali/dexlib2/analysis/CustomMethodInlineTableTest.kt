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

package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMultiDexContainer
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35mi
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class CustomMethodInlineTableTest {
    @Test
    @Throws(IOException::class)
    fun testCustomMethodInlineTable_Virtual() {
        val instructions: List<ImmutableInstruction> = mutableListOf(
            ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
            ImmutableInstruction10x(Opcode.RETURN_VOID)
        )

        val methodImpl = ImmutableMethodImplementation(1, instructions, null, null)
        val method = ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.PUBLIC.value, null,
            null, methodImpl)

        val classDef: ClassDef = ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
            null, null, null, null, null, listOf(method))

        val dexFile = ImmutableDexFile(Opcodes.default, listOf(classDef))

        val container = ImmutableMultiDexContainer(mapOf("classes.dex" to dexFile))

        val resolver = ClassPathResolver(listOf<String>(),
            listOf<String>(), listOf<String>(), container.getEntry("classes.dex")!!)
        val classPath = ClassPath(resolver.resolvedClassProviders, false, ClassPath.NOT_ART)

        val inlineMethodResolver = CustomInlineMethodResolver(classPath, "Lblah;->blah()V")
        val methodAnalyzer = MethodAnalyzer(classPath, method, inlineMethodResolver, false)

        val deodexedInstruction = methodAnalyzer.instructions[0]
        Assert.assertEquals(Opcode.INVOKE_VIRTUAL, deodexedInstruction.opcode)

        val methodReference = (deodexedInstruction as Instruction35c).reference as MethodReference
        Assert.assertEquals(method, methodReference)
    }

    @Test
    @Throws(IOException::class)
    fun testCustomMethodInlineTable_Static() {
        val instructions: List<ImmutableInstruction> = mutableListOf(
            ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
            ImmutableInstruction10x(Opcode.RETURN_VOID)
        )

        val methodImpl = ImmutableMethodImplementation(1, instructions, null, null)
        val method = ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.STATIC.value, null,
            null, methodImpl)

        val classDef: ClassDef = ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
            null, null, null, null, listOf(method), null)

        val dexFile = ImmutableDexFile(Opcodes.default, listOf(classDef))

        val container = ImmutableMultiDexContainer(mapOf("classes.dex" to dexFile))

        val resolver = ClassPathResolver(listOf<String>(),
            listOf<String>(), listOf<String>(), container.getEntry("classes.dex")!!)
        val classPath = ClassPath(resolver.resolvedClassProviders, false, ClassPath.NOT_ART)

        val inlineMethodResolver = CustomInlineMethodResolver(classPath, "Lblah;->blah()V")
        val methodAnalyzer = MethodAnalyzer(classPath, method, inlineMethodResolver, false)

        val deodexedInstruction = methodAnalyzer.instructions[0]
        Assert.assertEquals(Opcode.INVOKE_STATIC, deodexedInstruction.opcode)

        val methodReference = (deodexedInstruction as Instruction35c).reference as MethodReference
        Assert.assertEquals(method, methodReference)
    }

    @Test
    @Throws(IOException::class)
    fun testCustomMethodInlineTable_Direct() {
        val instructions: List<ImmutableInstruction> = mutableListOf(
            ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
            ImmutableInstruction10x(Opcode.RETURN_VOID)
        )

        val methodImpl = ImmutableMethodImplementation(1, instructions, null, null)
        val method = ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.PRIVATE.value, null,
            null, methodImpl)

        val classDef: ClassDef = ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null,
            null, null, null, null, listOf(method), null)

        val dexFile = ImmutableDexFile(Opcodes.default, listOf(classDef))

        val container = ImmutableMultiDexContainer(mapOf("classes.dex" to dexFile))

        val resolver = ClassPathResolver(listOf<String>(),
            listOf<String>(), listOf<String>(), container.getEntry("classes.dex")!!)
        val classPath = ClassPath(resolver.resolvedClassProviders, false, ClassPath.NOT_ART)

        val inlineMethodResolver = CustomInlineMethodResolver(classPath, "Lblah;->blah()V")
        val methodAnalyzer = MethodAnalyzer(classPath, method, inlineMethodResolver, false)

        val deodexedInstruction = methodAnalyzer.instructions[0]
        Assert.assertEquals(Opcode.INVOKE_DIRECT, deodexedInstruction.opcode)

        val methodReference = (deodexedInstruction as Instruction35c).reference as MethodReference
        Assert.assertEquals(method, methodReference)
    }
}
