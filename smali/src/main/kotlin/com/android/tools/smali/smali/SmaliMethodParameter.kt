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

package com.android.tools.smali.smali

import com.google.common.collect.ImmutableSet
import com.google.common.primitives.Ints
import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.iface.Annotation
import java.util.Comparator

open class SmaliMethodParameter(register: Int, type: String) : BaseMethodParameter(), WithRegister {
    @JvmField var register: Int = register
    @JvmField var type: String = type
    @JvmField var annotations: Set<Annotation> = ImmutableSet.of()
    @JvmField var name: String? = null

    override fun getRegister(): Int = register

    override fun getType(): String = type

    override fun getAnnotations(): Set<Annotation> = annotations

    override fun getName(): String? = name

    override fun getSignature(): String? = null

    companion object {
        @JvmField
        val COMPARATOR: Comparator<WithRegister> = Comparator { o1, o2 ->
            Ints.compare(o1.getRegister(), o2.getRegister())
        }
    }
}
