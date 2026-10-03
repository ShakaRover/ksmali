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

class MapItem(
    private val dexFile: DexBackedDexFile,
    private val offset: Int
) {
    fun getType(): Int {
        return dexFile.dataBuffer.readUshort(offset + TYPE_OFFSET)
    }

    fun getName(): String {
        return ItemType.getItemTypeName(getType())
    }

    fun getItemCount(): Int {
        return dexFile.dataBuffer.readSmallUint(offset + SIZE_OFFSET)
    }

    fun getOffset(): Int {
        return dexFile.dataBuffer.readSmallUint(offset + OFFSET_OFFSET)
    }

    companion object {
        const val ITEM_SIZE = 12

        const val TYPE_OFFSET = 0
        const val SIZE_OFFSET = 4
        const val OFFSET_OFFSET = 8

        @JvmStatic
        fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
            return object : SectionAnnotator(annotator, mapItem) {
                override fun getItemName(): String {
                    return "map_item"
                }

                override fun annotateItem(
                    out: AnnotatedBytes,
                    itemIndex: Int,
                    itemIdentity: String?
                ) {
                    val itemType = dexFile.buffer.readUshort(out.cursor)
                    out.annotate(2, "type = 0x%x: %s", itemType, ItemType.getItemTypeName(itemType))

                    out.annotate(2, "unused")

                    val size = dexFile.buffer.readSmallUint(out.cursor)
                    out.annotate(4, "size = %d", size)

                    val offset = dexFile.buffer.readSmallUint(out.cursor)
                    out.annotate(4, "offset = 0x%x", offset)
                }

                override fun annotateSection(out: AnnotatedBytes) {
                    out.moveTo(sectionOffset)
                    val mapItemCount = dexFile.buffer.readSmallUint(out.cursor)
                    out.annotate(4, "size = %d", mapItemCount)

                    super.annotateSectionInner(out, mapItemCount)
                }
            }
        }
    }
}
