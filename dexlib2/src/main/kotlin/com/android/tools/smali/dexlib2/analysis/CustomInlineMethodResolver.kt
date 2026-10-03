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


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.InlineIndexInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.util.ParamUtil
import com.android.tools.smali.util.InputStreamUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.regex.Pattern

open class CustomInlineMethodResolver(private val classPath: ClassPath, inlineTable: String) :
    InlineMethodResolver() {
    private val inlineMethods: Array<Method>

    init {
        val reader = StringReader(inlineTable)
        val lines = ArrayList<String>()

        val br = BufferedReader(reader)

        try {
            var line = br.readLine()

            while (line != null) {
                if (line.length > 0) {
                    lines.add(line)
                }

                line = br.readLine()
            }
        } catch (ex: IOException) {
            throw RuntimeException("Error while parsing inline table", ex)
        }

        inlineMethods = Array(lines.size) { parseAndResolveInlineMethod(lines[it]) }
    }

    @Throws(IOException::class)
    constructor(classPath: ClassPath, inlineTable: File) : this(
        classPath,
        String(
            InputStreamUtil.toByteArray(FileInputStream(inlineTable)),
            StandardCharsets.UTF_8
        )
    )

    override fun resolveExecuteInline(analyzedInstruction: AnalyzedInstruction): Method {
        val instruction = analyzedInstruction.instruction as InlineIndexInstruction
        val methodIndex = instruction.inlineIndex

        if (methodIndex < 0 || methodIndex >= inlineMethods.size) {
            throw RuntimeException("Invalid method index: $methodIndex")
        }
        return inlineMethods[methodIndex]
    }

    private fun parseAndResolveInlineMethod(inlineMethod: String): Method {
        val m = longMethodPattern.matcher(inlineMethod)
        if (!m.matches()) {
            assert(false)
            throw RuntimeException("Invalid method descriptor: $inlineMethod")
        }

        val className = m.group(1)
        val methodName = m.group(2)
        val methodParams: Iterable<ImmutableMethodParameter> = ParamUtil.parseParamString(m.group(3))
        val methodRet = m.group(4)
        val methodRef = ImmutableMethodReference(className, methodName, methodParams, methodRet)

        var accessFlags = 0

        var resolved = false
        val typeProto = classPath.getClass(className)
        if (typeProto is ClassProto) {
            val classDef: ClassDef = typeProto.classDef
            for (method in classDef.methods) {
                if (method == methodRef) {
                    resolved = true
                    accessFlags = method.accessFlags
                    break
                }
            }
        }

        if (!resolved) {
            throw RuntimeException("Cannot resolve inline method: $inlineMethod")
        }

        return ImmutableMethod(className, methodName, methodParams, methodRet, accessFlags, null, null, null)
    }

    companion object {
        private val longMethodPattern = Pattern.compile("(L[^;]+;)->([^(]+)\\(([^)]*)\\)(.+)")
    }
}
