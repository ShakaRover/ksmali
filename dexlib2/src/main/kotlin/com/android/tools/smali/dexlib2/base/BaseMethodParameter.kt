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

package com.android.tools.smali.dexlib2.base

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

abstract class BaseMethodParameter : BaseTypeReference(), MethodParameter {
    override val signature: String?
        get() {
            var signatureAnnotation: Annotation? = null
            for (annotation in annotations) {
                if (annotation.type == "Ldalvik/annotation/Signature;") {
                    signatureAnnotation = annotation
                    break
                }
            }
            val annotation = signatureAnnotation ?: return null

            var signatureValues: ArrayEncodedValue? = null
            for (annotationElement in annotation.elements) {
                if (annotationElement.name == "value") {
                    val encodedValue = annotationElement.value
                    if (encodedValue.valueType != ValueType.ARRAY) {
                        return null
                    }
                    signatureValues = encodedValue as ArrayEncodedValue
                    break
                }
            }
            val values = signatureValues ?: return null

            return buildString {
                for (signatureValue in values.value) {
                    if (signatureValue.valueType != ValueType.STRING) {
                        return null
                    }
                    append((signatureValue as StringEncodedValue).value)
                }
            }
        }
}
