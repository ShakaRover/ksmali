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
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.StringWriter

class InstructionMethodItemTest {

    @Test
    @Throws(IOException::class)
    fun testInvalidReference() {

        val instruction = object : Instruction21c {
            override fun getRegisterA(): Int {
                return 0
            }

            override fun getReference(): Reference {
                return object : BaseStringReference() {
                    override fun validateReference() {
                        throw Reference.InvalidReferenceException("blahblahblah")
                    }

                    override fun getString(): String {
                        throw RuntimeException("invalid reference")
                    }
                }
            }

            override fun getReferenceType(): Int {
                return ReferenceType.STRING
            }

            override fun getOpcode(): Opcode {
                return Opcode.CONST_STRING
            }

            override fun getCodeUnits(): Int {
                return Format.Format21c.size / 2
            }
        }

        val methodImplementation = object : MethodImplementation {
            override fun getRegisterCount(): Int {
                return 1
            }

            override fun getInstructions(): Iterable<Instruction> {
                return ImmutableList.of(instruction)
            }

            override fun getTryBlocks(): List<TryBlock<ExceptionHandler>> {
                return ImmutableList.of()
            }

            override fun getDebugItems(): Iterable<DebugItem> {
                return ImmutableList.of()
            }
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

        override fun getParameters(): List<MethodParameter> {
            return ImmutableList.of()
        }

        override fun getAccessFlags(): Int {
            return 0
        }

        override fun getAnnotations(): Set<Annotation> {
            return ImmutableSet.of()
        }

        override fun getImplementation(): MethodImplementation? {
            return methodImplementation
        }

        override fun getDefiningClass(): String {
            return "Ltest;"
        }

        override fun getName(): String {
            return "test"
        }

        override fun getParameterTypes(): List<CharSequence> {
            return ImmutableList.of()
        }

        override fun getReturnType(): String {
            return "V"
        }

        override fun getHiddenApiRestrictions(): Set<HiddenApiRestriction> {
            return ImmutableSet.of()
        }
    }

    private class TestClassDef : BaseTypeReference(), ClassDef {
        override fun getAccessFlags(): Int {
            return 0
        }

        override fun getSuperclass(): String? {
            return "Ljava/lang/Object;"
        }

        override fun getInterfaces(): List<String> {
            return ImmutableList.of()
        }

        override fun getSourceFile(): String? {
            return null
        }

        override fun getAnnotations(): Set<Annotation> {
            return ImmutableSet.of()
        }

        override fun getStaticFields(): Iterable<Field> {
            return ImmutableList.of()
        }

        override fun getInstanceFields(): Iterable<Field> {
            return ImmutableList.of()
        }

        override fun getFields(): Iterable<Field> {
            return ImmutableList.of()
        }

        override fun getDirectMethods(): Iterable<Method> {
            return ImmutableList.of()
        }

        override fun getVirtualMethods(): Iterable<Method> {
            return ImmutableList.of()
        }

        override fun getMethods(): Iterable<Method> {
            return ImmutableList.of()
        }

        override fun getType(): String {
            return "Ltest;"
        }
    }
}
