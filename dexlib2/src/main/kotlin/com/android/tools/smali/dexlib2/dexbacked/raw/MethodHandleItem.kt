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

package com.android.tools.smali.dexlib2.dexbacked.raw

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.ExceptionWithContext

object MethodHandleItem {
    const val ITEM_SIZE = 8

    const val METHOD_HANDLE_TYPE_OFFSET = 0
    const val MEMBER_ID_OFFSET = 4

    @JvmStatic
    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override fun getItemName(): String {
                return "method_handle_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val methodHandleType = dexFile.buffer.readUshort(out.cursor)
                out.annotate(2, "type = %s", MethodHandleType.toString(methodHandleType))
                out.annotate(2, "unused")

                val fieldOrMethodId = dexFile.buffer.readUshort(out.cursor)
                val fieldOrMethodDescriptor: String = when (methodHandleType) {
                    MethodHandleType.STATIC_PUT,
                    MethodHandleType.STATIC_GET,
                    MethodHandleType.INSTANCE_PUT,
                    MethodHandleType.INSTANCE_GET ->
                        FieldIdItem.getReferenceAnnotation(dexFile, fieldOrMethodId)
                    MethodHandleType.INVOKE_STATIC,
                    MethodHandleType.INVOKE_INSTANCE,
                    MethodHandleType.INVOKE_CONSTRUCTOR,
                    MethodHandleType.INVOKE_DIRECT,
                    MethodHandleType.INVOKE_INTERFACE ->
                        MethodIdItem.getReferenceAnnotation(dexFile, fieldOrMethodId)
                    else -> throw ExceptionWithContext(
                        "Invalid method handle type: %d", methodHandleType
                    )
                }

                out.annotate(2, "field_or_method_id = %s", fieldOrMethodDescriptor)
                out.annotate(2, "unused")
            }
        }
    }
}
