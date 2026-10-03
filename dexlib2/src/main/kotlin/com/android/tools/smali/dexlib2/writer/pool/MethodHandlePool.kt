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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.MethodHandleSection
import com.android.tools.smali.util.ExceptionWithContext

class MethodHandlePool(dexPool: DexPool) : BaseIndexPool<MethodHandleReference>(dexPool),
    MethodHandleSection<MethodHandleReference, FieldReference, MethodReference> {

    fun intern(methodHandleReference: MethodHandleReference) {
        val prev = internedItems.put(methodHandleReference, 0)
        if (prev == null) {
            when (methodHandleReference.methodHandleType) {
                MethodHandleType.STATIC_PUT,
                MethodHandleType.STATIC_GET,
                MethodHandleType.INSTANCE_PUT,
                MethodHandleType.INSTANCE_GET ->
                    dexPool.fieldSection.intern(methodHandleReference.memberReference as FieldReference)
                MethodHandleType.INVOKE_STATIC,
                MethodHandleType.INVOKE_INSTANCE,
                MethodHandleType.INVOKE_CONSTRUCTOR,
                MethodHandleType.INVOKE_DIRECT,
                MethodHandleType.INVOKE_INTERFACE ->
                    dexPool.methodSection.intern(methodHandleReference.memberReference as MethodReference)
                else -> throw ExceptionWithContext(
                    "Invalid method handle type: %d", methodHandleReference.methodHandleType)
            }
        }
    }

    override fun getFieldReference(methodHandleReference: MethodHandleReference): FieldReference {
        return methodHandleReference.memberReference as FieldReference
    }

    override fun getMethodReference(methodHandleReference: MethodHandleReference): MethodReference {
        return methodHandleReference.memberReference as MethodReference
    }
}
