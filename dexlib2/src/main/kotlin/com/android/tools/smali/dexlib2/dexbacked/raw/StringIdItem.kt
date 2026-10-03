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
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.StringUtils

object StringIdItem {
    const val ITEM_SIZE = 4

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "string_id_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val stringDataOffset = dexFile.buffer.readSmallUint(out.cursor)
                try {
                    val stringValue = dexFile.stringSection.get(itemIndex)
                    out.annotate(
                        4, "string_data_item[0x%x]: \"%s\"", stringDataOffset,
                        StringUtils.escapeString(stringValue)
                    )
                    return
                } catch (ex: Exception) {
                    System.err.print("Error while resolving string value at index: ")
                    System.err.print(itemIndex)
                    ex.printStackTrace(System.err)
                }

                out.annotate(4, "string_id_item[0x%x]", stringDataOffset)
            }
        }
    }

    fun getReferenceAnnotation(dexFile: DexBackedDexFile, stringIndex: Int): String {
        return getReferenceAnnotation(dexFile, stringIndex, false)
    }

    fun getReferenceAnnotation(
        dexFile: DexBackedDexFile,
        stringIndex: Int,
        quote: Boolean
    ): String {
        try {
            var string = dexFile.stringSection.get(stringIndex)
            if (quote) {
                string = "\"${StringUtils.escapeString(string)}\""
            }
            return "string_id_item[${stringIndex}]: ${string}"
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
        return "string_id_item[${stringIndex}]"
    }

    fun getOptionalReferenceAnnotation(dexFile: DexBackedDexFile, stringIndex: Int): String {
        return getOptionalReferenceAnnotation(dexFile, stringIndex, false)
    }

    fun getOptionalReferenceAnnotation(
        dexFile: DexBackedDexFile,
        stringIndex: Int,
        quote: Boolean
    ): String {
        if (stringIndex == -1) {
            return "string_id_item[NO_INDEX]"
        }
        return getReferenceAnnotation(dexFile, stringIndex, quote)
    }
}
