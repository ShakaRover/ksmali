/*
 * Copyright 2020, Google LLC
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

package com.android.tools.smali.dexlib2.rewriter

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.AnnotationVisibility
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.junit.Assert
import org.junit.Test

class RewriteArrayTypeTest {
    @Test
    fun testRewriteArrayTypeTest() {
        val class1: ClassDef = ImmutableClassDef("Lcls1;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null, null,
            listOf(ImmutableAnnotation(AnnotationVisibility.RUNTIME, "Lannotation;", null)),
            listOf<Field>(
                ImmutableField("Lcls1;", "field1", "I", AccessFlags.PUBLIC.value, null, null, null)
            ),
            listOf<Method>(
                ImmutableMethod("Lcls1", "method1",
                    listOf<MethodParameter>(ImmutableMethodParameter("[[[Lcls1;", null, null)), "V",
                    AccessFlags.PUBLIC.value, null, null, null)
            )
        )

        val dexFile = ImmutableDexFile(Opcodes.default, setOf(class1))

        val rewriter = DexRewriter(object : RewriterModule() {
            override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
                return object : TypeRewriter() {
                    override fun rewriteUnwrappedType(value: String): String {
                        if (value == "Lcls1;") {
                            return "Lcls2;"
                        }
                        return value
                    }
                }
            }
        })

        val rewrittenDexFile = rewriter.dexFileRewriter.rewrite(dexFile)

        val rewrittenClassDef = rewrittenDexFile.classes.first()
        val rewrittenMethodDef = rewrittenClassDef.methods.first()

        Assert.assertEquals(rewrittenClassDef.type, "Lcls2;")
        Assert.assertEquals(rewrittenMethodDef.parameterTypes[0], "[[[Lcls2;")
    }

    @Test
    fun testUnmodifiedArrayTypeTest() {
        val class1: ClassDef = ImmutableClassDef("Lcls1;", AccessFlags.PUBLIC.value, "Ljava/lang/Object;", null, null,
            listOf(ImmutableAnnotation(AnnotationVisibility.RUNTIME, "Lannotation;", null)),
            listOf<Field>(
                ImmutableField("Lcls1;", "field1", "I", AccessFlags.PUBLIC.value, null, null, null)
            ),
            listOf<Method>(
                ImmutableMethod("Lcls1", "method1",
                    listOf<MethodParameter>(ImmutableMethodParameter("[[[Lcls1;", null, null)), "V",
                    AccessFlags.PUBLIC.value, null, null, null)
            )
        )

        val dexFile = ImmutableDexFile(Opcodes.default, setOf(class1))

        val rewriter = DexRewriter(RewriterModule())

        val rewrittenDexFile = rewriter.dexFileRewriter.rewrite(dexFile)

        val rewrittenClassDef = rewrittenDexFile.classes.first()
        val rewrittenMethodDef = rewrittenClassDef.methods.first()

        Assert.assertEquals(rewrittenClassDef.type, "Lcls1;")
        Assert.assertEquals(rewrittenMethodDef.parameterTypes[0], "[[[Lcls1;")
    }
}
