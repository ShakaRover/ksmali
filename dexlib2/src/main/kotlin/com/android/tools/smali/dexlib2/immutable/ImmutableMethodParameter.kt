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

package com.android.tools.smali.dexlib2.immutable

import com.android.tools.smali.dexlib2.base.BaseMethodParameter

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.util.ImmutableConverter

open class ImmutableMethodParameter(
    override val type: String,
    annotations: Set<Annotation>?,
    override val name: String?
) : BaseMethodParameter() {
    override val annotations: Set<Annotation> = ImmutableAnnotation.immutableSetOf(annotations)

    // TODO: iterate over the annotations to get the signature
    override val signature: String?
        get() = null

    companion object {
        fun of(methodParameter: MethodParameter): ImmutableMethodParameter {
            if (methodParameter is ImmutableMethodParameter) {
                return methodParameter
            }
            return ImmutableMethodParameter(
                methodParameter.type,
                methodParameter.annotations,
                methodParameter.name
            )
        }

        fun immutableListOf(list: Iterable<MethodParameter>?): List<ImmutableMethodParameter> {
            return CONVERTER.toList(list)
        }

        private val CONVERTER: ImmutableConverter<ImmutableMethodParameter, MethodParameter> =
            object : ImmutableConverter<ImmutableMethodParameter, MethodParameter>() {
                override fun isImmutable(item: MethodParameter): Boolean {
                    return item is ImmutableMethodParameter
                }

                override fun makeImmutable(item: MethodParameter): ImmutableMethodParameter {
                    return of(item)
                }
            }
    }
}
