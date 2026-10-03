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

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes

object AnnotationItem {
    const val VISIBILITY_OFFSET = 0
    const val ANNOTATION_OFFSET = 1

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "annotation_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val visibility = dexFile.buffer.readUbyte(out.cursor)
                out.annotate(1, "visibility = %d: %s", visibility, getAnnotationVisibility(visibility))

                val reader: DexReader<out DexBuffer> = dexFile.buffer.readerAt(out.cursor)

                EncodedValue.annotateEncodedAnnotation(dexFile, out, reader)
            }
        }
    }

    private fun getAnnotationVisibility(visibility: Int): String {
        return when (visibility) {
            0 -> "build"
            1 -> "runtime"
            2 -> "system"
            else -> "invalid visibility"
        }
    }

    fun getReferenceAnnotation(dexFile: DexBackedDexFile, annotationItemOffset: Int): String {
        try {
            val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(annotationItemOffset)
            reader.readUbyte()
            val typeIndex = reader.readSmallUleb128()
            val annotationType = dexFile.typeSection.get(typeIndex)
            return "annotation_item[0x%x]: %s".format(annotationItemOffset, annotationType)
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
        return "annotation_item[0x%x]".format(annotationItemOffset)
    }
}
