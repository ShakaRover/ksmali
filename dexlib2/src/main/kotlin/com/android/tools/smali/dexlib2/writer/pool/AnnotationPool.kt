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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.writer.AnnotationSection

class AnnotationPool(dexPool: DexPool) : BaseOffsetPool<Annotation>(dexPool),
    AnnotationSection<CharSequence, CharSequence, Annotation, AnnotationElement, EncodedValue> {

    fun intern(annotation: Annotation) {
        val prev = internedItems.put(annotation, 0)
        if (prev == null) {
            dexPool.typeSection.intern(annotation.type)
            for (element in annotation.elements) {
                dexPool.stringSection.intern(element.name)
                dexPool.internEncodedValue(element.value)
            }
        }
    }

    override fun getVisibility(key: Annotation): Int {
        return key.visibility
    }

    override fun getType(key: Annotation): CharSequence {
        return key.type
    }

    override fun getElements(key: Annotation): Collection<@JvmWildcard AnnotationElement> {
        return key.elements
    }

    override fun getElementName(element: AnnotationElement): CharSequence {
        return element.name
    }

    override fun getElementValue(element: AnnotationElement): EncodedValue {
        return element.value
    }
}
