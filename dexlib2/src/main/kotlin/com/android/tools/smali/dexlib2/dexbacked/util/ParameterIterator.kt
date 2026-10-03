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

package com.android.tools.smali.dexlib2.dexbacked.util

import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.MethodParameter

class ParameterIterator(
    types: List<CharSequence>,
    annotations: List<Set<Annotation>>,
    names: Iterator<String?>
) : MutableIterator<MethodParameter> {
    private val parameterTypes: Iterator<CharSequence> = types.iterator()
    private val parameterAnnotations: Iterator<Set<Annotation>> = annotations.iterator()
    private val parameterNames: Iterator<String?> = names

    override fun hasNext(): Boolean {
        return parameterTypes.hasNext()
    }

    override fun next(): MethodParameter {
        val type: String = parameterTypes.next().toString()
        val annotations: Set<Annotation>
        val name: String?

        if (parameterAnnotations.hasNext()) {
            annotations = parameterAnnotations.next()
        } else {
            annotations = emptySet()
        }

        if (parameterNames.hasNext()) {
            name = parameterNames.next()
        } else {
            name = null
        }

        return object : BaseMethodParameter() {
            override val annotations: Set<Annotation>
                get() = annotations as Set<Annotation>

            override val name: String?
                get() = name

            override val type: String
                get() = type
        }
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }
}
