/*
 * Copyright 2018, Google LLC
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

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableCallSiteReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodHandleReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodProtoReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.writer.builder.BuilderCallSiteReference
import com.android.tools.smali.dexlib2.writer.builder.BuilderMethod
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.ArrayList

class CallSiteTest {
    @Test
    @Throws(IOException::class)
    fun testPoolCallSite() {
        val class1: ClassDef = ImmutableClassDef("Lcls1;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null, null,
            null, null,
            listOf<Method>(
                ImmutableMethod("Lcls1", "method1",
                    ImmutableList.of(), "V", AccessFlags.PUBLIC.value, null, null,
                    ImmutableMethodImplementation(10, ImmutableList.of(
                        ImmutableInstruction35c(Opcode.INVOKE_CUSTOM, 0, 0, 0, 0, 0, 0,
                            ImmutableCallSiteReference("call_site_1",
                                ImmutableMethodHandleReference(
                                    MethodHandleType.INVOKE_STATIC,
                                    ImmutableMethodReference("Lcls1", "loader",
                                        ImmutableList.of("Ljava/lang/invoke/Lookup;",
                                            "Ljava/lang/String;",
                                            "Ljava/lang/invoke/MethodType;"),
                                        "Ljava/lang/invoke/CallSite;")),
                                "someMethod", ImmutableMethodProtoReference(ImmutableList.of(), "V"),
                                ImmutableList.of<EncodedValue>()))
                    ), null, null))
            ))

        val tempFile = File.createTempFile("dex", ".dex")
        DexFileFactory.writeDexFile(tempFile.path,
            ImmutableDexFile(Opcodes.forArtVersion(111), ImmutableList.of(class1)))

        verifyDexFile(DexFileFactory.loadDexFile(tempFile, Opcodes.forArtVersion(111)))
    }

    @Test
    @Throws(IOException::class)
    fun testBuilderCallSite() {
        val dexBuilder = DexBuilder(Opcodes.forArtVersion(111))

        val callSite: BuilderCallSiteReference = dexBuilder.internCallSite(
            ImmutableCallSiteReference("call_site_1",
                ImmutableMethodHandleReference(
                    MethodHandleType.INVOKE_STATIC,
                    ImmutableMethodReference("Lcls1;", "loader", ImmutableList.of("Ljava/lang/invoke/Lookup;",
                        "Ljava/lang/String;",
                        "Ljava/lang/invoke/MethodType;"),
                        "Ljava/lang/invoke/CallSite;")),
                "someMethod",
                ImmutableMethodProtoReference(ImmutableList.of(), "V"), ImmutableList.of<EncodedValue>()))

        val methodImplementationBuilder = MethodImplementationBuilder(10)
        methodImplementationBuilder.addInstruction(
            BuilderInstruction35c(Opcode.INVOKE_CUSTOM, 0, 0, 0, 0, 0, 0, callSite)
        )

        val method: BuilderMethod = dexBuilder.internMethod(
            "Lcls1;", "method1", null, "V", 0, ImmutableSet.of(),
            ImmutableSet.of(), methodImplementationBuilder.getMethodImplementation()
        )
        dexBuilder.internClassDef("Lcls1;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null, null,
            ImmutableSet.of(), null,
            ImmutableList.of(method))

        val tempFile = File.createTempFile("dex", ".dex")
        dexBuilder.writeTo(FileDataStore(tempFile))

        verifyDexFile(DexFileFactory.loadDexFile(tempFile, Opcodes.forArtVersion(111)))
    }

    private fun verifyDexFile(dexFile: DexFile) {
        Assert.assertEquals(1, dexFile.classes.size)
        val cls = dexFile.classes.first()
        Assert.assertEquals("Lcls1;", cls.type)
        Assert.assertEquals(1, cls.methods.toList().size)
        val method = cls.methods.first()
        Assert.assertEquals("method1", method.name)
        Assert.assertEquals(1, method.implementation!!.instructions.toList().size)
        val instruction = method.implementation!!.instructions.toList()[0]
        Assert.assertEquals(Opcode.INVOKE_CUSTOM, instruction.opcode)
        Assert.assertTrue((instruction as Instruction35c).reference is CallSiteReference)
    }
}
