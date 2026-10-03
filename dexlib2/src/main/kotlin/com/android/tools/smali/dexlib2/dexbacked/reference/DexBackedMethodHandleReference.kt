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

package com.android.tools.smali.dexlib2.dexbacked.reference

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.base.reference.BaseMethodHandleReference
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodHandleItem
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.Reference.InvalidReferenceException
import com.android.tools.smali.util.ExceptionWithContext

class DexBackedMethodHandleReference(
    val dexFile: DexBackedDexFile,
    val methodHandleIndex: Int
) : BaseMethodHandleReference() {
    val methodHandleOffset: Int = dexFile.methodHandleSection.getOffset(methodHandleIndex)

    override val methodHandleType: Int
        get() = dexFile.buffer.readUshort(
            methodHandleOffset + MethodHandleItem.METHOD_HANDLE_TYPE_OFFSET
        )

    override val memberReference: Reference
        get() {
            val memberIndex = dexFile.buffer.readUshort(
                methodHandleOffset + MethodHandleItem.MEMBER_ID_OFFSET
            )
            return when (methodHandleType) {
                MethodHandleType.STATIC_PUT,
                MethodHandleType.STATIC_GET,
                MethodHandleType.INSTANCE_PUT,
                MethodHandleType.INSTANCE_GET ->
                    DexBackedFieldReference(dexFile, memberIndex)
                MethodHandleType.INVOKE_STATIC,
                MethodHandleType.INVOKE_INSTANCE,
                MethodHandleType.INVOKE_CONSTRUCTOR,
                MethodHandleType.INVOKE_DIRECT,
                MethodHandleType.INVOKE_INTERFACE ->
                    DexBackedMethodReference(dexFile, memberIndex)
                else -> throw ExceptionWithContext(
                    "Invalid method handle type: %d", methodHandleType
                )
            }
        }

    @Throws(InvalidReferenceException::class)
    override fun validateReference() {
        if (methodHandleIndex < 0 || methodHandleIndex >= dexFile.methodHandleSection.size) {
            throw InvalidReferenceException("methodhandle@$methodHandleIndex")
        }

        try {
            memberReference
        } catch (ex: ExceptionWithContext) {
            throw InvalidReferenceException("methodhandle@$methodHandleIndex", ex)
        }
    }
}
