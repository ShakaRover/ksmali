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

import com.android.tools.smali.dexlib2.base.BaseAnnotationElement
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableEncodedValueFactory
import com.android.tools.smali.util.ImmutableConverter

open class ImmutableAnnotationElement(
    override val name: String,
    immutableValue: ImmutableEncodedValue
) : BaseAnnotationElement() {
    override val value: EncodedValue = immutableValue

    constructor(name: String, value: EncodedValue) :
        this(name, ImmutableEncodedValueFactory.of(value))

    companion object {
        @JvmStatic
        fun of(annotationElement: AnnotationElement): ImmutableAnnotationElement {
            if (annotationElement is ImmutableAnnotationElement) {
                return annotationElement
            }
            return ImmutableAnnotationElement(
                annotationElement.name,
                annotationElement.value
            )
        }

        @JvmStatic
        fun immutableSetOf(list: Iterable<AnnotationElement>?): Set<ImmutableAnnotationElement> {
            return CONVERTER.toSet(list)
        }

        private val CONVERTER: ImmutableConverter<ImmutableAnnotationElement, AnnotationElement> =
            object : ImmutableConverter<ImmutableAnnotationElement, AnnotationElement>() {
                override fun isImmutable(item: AnnotationElement): Boolean {
                    return item is ImmutableAnnotationElement
                }

                override fun makeImmutable(item: AnnotationElement): ImmutableAnnotationElement {
                    return of(item)
                }
            }
    }
}
