/*
 * Copyright 2019, Google LLC
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

package com.android.tools.smali.baksmali

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.Adaptors.Format.InstructionMethodItem
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.baksmali.Adaptors.RegisterFormatter
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.base.reference.BaseStringReference
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.Reference
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.StringWriter

class InstructionMethodItemTest {

    @Test
    @Throws(IOException::class)
    fun testInvalidReference() {

        val instruction = object : Instruction21c {
            override val registerA: Int = 0

            override val reference: Reference
                get() = object : BaseStringReference() {
                    override fun validateReference() {
                        throw Reference.InvalidReferenceException("blahblahblah")
                    }

                    override val string: String
                        get() = throw RuntimeException("invalid reference")
                }

            override val referenceType: Int = ReferenceType.STRING

            override val opcode: Opcode = Opcode.CONST_STRING

            override val codeUnits: Int = Format.Format21c.size / 2
        }

        val methodImplementation = object : MethodImplementation {
            override val registerCount: Int = 1

            override val instructions: Iterable<Instruction> = listOf(instruction)

            override val tryBlocks: List<TryBlock<ExceptionHandler>> = listOf()

            override val debugItems: Iterable<DebugItem> = listOf()
        }

        val method: Method = TestMethod(methodImplementation)

        val classDefinition = ClassDefinition(BaksmaliOptions(), TestClassDef())

        val methodDefinition = MethodDefinition(classDefinition, method, methodImplementation)
        methodDefinition.registerFormatter = RegisterFormatter(BaksmaliOptions(), 1, 0)

        val methodItem = InstructionMethodItem<Instruction21c>(methodDefinition, 0, instruction)

        val stringWriter = StringWriter()
        val writer = BaksmaliWriter(stringWriter)
        methodItem.writeTo(writer)

        Assert.assertEquals(
            "#Invalid reference" + System.lineSeparator() +
                "#const-string v0, blahblahblah" + System.lineSeparator() + "nop",
            stringWriter.toString())
    }

    private class TestMethod(
        private val methodImplementation: MethodImplementation
    ) : BaseMethodReference(), Method {

        override val parameters: List<MethodParameter> = listOf()

        override val accessFlags: Int = 0

        override val annotations: Set<Annotation> = setOf()

        override val implementation: MethodImplementation? = methodImplementation

        override val definingClass: String = "Ltest;"

        override val name: String = "test"

        override val parameterTypes: List<CharSequence> = listOf()

        override val returnType: String = "V"

        override val hiddenApiRestrictions: Set<HiddenApiRestriction> = setOf()
    }

    private class TestClassDef : BaseTypeReference(), ClassDef {
        override val accessFlags: Int = 0

        override val superclass: String? = "Ljava/lang/Object;"

        override val interfaces: List<String> = listOf()

        override val sourceFile: String? = null

        override val annotations: Set<Annotation> = setOf()

        override val staticFields: Iterable<Field> = listOf()

        override val instanceFields: Iterable<Field> = listOf()

        override val fields: Iterable<Field> = listOf()

        override val directMethods: Iterable<Method> = listOf()

        override val virtualMethods: Iterable<Method> = listOf()

        override val methods: Iterable<Method> = listOf()

        override val type: String = "Ltest;"
    }
}
