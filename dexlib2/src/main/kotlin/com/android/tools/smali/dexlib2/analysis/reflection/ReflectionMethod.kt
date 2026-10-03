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


package com.android.tools.smali.dexlib2.analysis.reflection

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.analysis.reflection.util.ReflectionUtils
import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import java.util.AbstractList
import java.util.Collections

open class ReflectionMethod(private val method: java.lang.reflect.Method) : BaseMethodReference(), Method {
    override val parameters: List<MethodParameter>
        get() {
            val method = this.method
            return object : AbstractList<MethodParameter>() {
                private val parameters = method.parameterTypes

                override fun get(index: Int): MethodParameter {
                    return object : BaseMethodParameter() {
                        override val annotations: Set<Annotation>
                            get() = Collections.emptySet()

                        override val name: String?
                            get() = null

                        override val type: String
                            get() = ReflectionUtils.javaToDexName(parameters[index].name)
                    }
                }

                override val size: Int
                    get() = parameters.size
            }
        }

    override val accessFlags: Int
        get() = method.modifiers

    override val annotations: Set<Annotation>
        get() = Collections.emptySet()

    override val implementation: MethodImplementation?
        get() = null

    override val definingClass: String
        get() = ReflectionUtils.javaToDexName(method.declaringClass.name)

    override val name: String
        get() = method.name

    override val parameterTypes: List<String>
        get() {
            val params = parameters
            return object : AbstractList<String>() {
                override fun get(index: Int): String {
                    return params[index].type
                }

                override val size: Int
                    get() = params.size
            }
        }

    override val returnType: String
        get() = ReflectionUtils.javaToDexName(method.returnType.name)

    override val hiddenApiRestrictions: Set<HiddenApiRestriction>
        get() = Collections.emptySet()
}
