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

package com.android.tools.smali.dexlib2

import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.util.regex.Pattern

class AccessorTest {
    private val accessorMethodPattern = Pattern.compile("([a-zA-Z]*)_([a-zA-Z]*)")

    @Test
    @Throws(IOException::class)
    fun testAccessors() {
        val url = AccessorTest::class.java.classLoader.getResource("accessorTest.dex")
        Assert.assertNotNull(url)
        val f = DexFileFactory.loadDexFile(url!!.file, Opcodes.default)

        val sar = SyntheticAccessorResolver(f.opcodes, f.classes)

        var accessorTypesClass: ClassDef? = null
        var accessorsClass: ClassDef? = null

        for (classDef in f.classes) {
            val className = classDef.type

            if (className == "Lorg/jf/dexlib2/AccessorTypes;") {
                accessorTypesClass = classDef
            } else if (className == "Lorg/jf/dexlib2/AccessorTypes\$Accessors;") {
                accessorsClass = classDef
            }
        }

        Assert.assertNotNull(accessorTypesClass)
        Assert.assertNotNull(accessorsClass)

        for (method in accessorsClass!!.methods) {
            val m = accessorMethodPattern.matcher(method.name)
            if (!m.matches()) {
                continue
            }
            val type = m.group(1)
            val operation = m.group(2)

            val methodImpl = method.implementation
            Assert.assertNotNull(methodImpl)

            for (instruction in methodImpl!!.instructions) {
                val opcode = instruction.opcode
                if (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) {
                    val accessorMethod =
                        (instruction as ReferenceInstruction).reference as MethodReference

                    val accessedMember = sar.getAccessedMember(accessorMethod)

                    Assert.assertNotNull(
                        "Could not resolve accessor for ${type}_${operation}",
                        accessedMember
                    )

                    val operationType = operationTypes[operation]
                    Assert.assertEquals(operationType!!.toLong(), accessedMember!!.accessedMemberType.toLong())

                    Assert.assertEquals(
                        "${type}_val",
                        (accessedMember.accessedMember as FieldReference).name
                    )
                }
            }
        }
    }

    companion object {
        private val operationTypes: Map<String, Int>

        init {
            operationTypes = buildMap {
                put("postinc", SyntheticAccessorResolver.POSTFIX_INCREMENT)
                put("preinc", SyntheticAccessorResolver.PREFIX_INCREMENT)
                put("postdec", SyntheticAccessorResolver.POSTFIX_DECREMENT)
                put("predec", SyntheticAccessorResolver.PREFIX_DECREMENT)
                put("add", SyntheticAccessorResolver.ADD_ASSIGNMENT)
                put("sub", SyntheticAccessorResolver.SUB_ASSIGNMENT)
                put("mul", SyntheticAccessorResolver.MUL_ASSIGNMENT)
                put("div", SyntheticAccessorResolver.DIV_ASSIGNMENT)
                put("rem", SyntheticAccessorResolver.REM_ASSIGNMENT)
                put("and", SyntheticAccessorResolver.AND_ASSIGNMENT)
                put("or", SyntheticAccessorResolver.OR_ASSIGNMENT)
                put("xor", SyntheticAccessorResolver.XOR_ASSIGNMENT)
                put("shl", SyntheticAccessorResolver.SHL_ASSIGNMENT)
                put("shr", SyntheticAccessorResolver.SHR_ASSIGNMENT)
                put("ushr", SyntheticAccessorResolver.USHR_ASSIGNMENT)
            }
        }
    }
}
