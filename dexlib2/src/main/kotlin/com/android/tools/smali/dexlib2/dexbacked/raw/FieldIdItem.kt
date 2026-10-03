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

object FieldIdItem {
    const val ITEM_SIZE = 8

    const val CLASS_OFFSET = 0
    const val TYPE_OFFSET = 2
    const val NAME_OFFSET = 4

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "field_id_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val classIndex = dexFile.buffer.readUshort(out.cursor)
                out.annotate(2, "class_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, classIndex))

                val typeIndex = dexFile.buffer.readUshort(out.cursor)
                out.annotate(2, "return_type_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, typeIndex))

                val nameIndex = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "name_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, nameIndex))
            }
        }
    }

    fun asString(dexFile: DexBackedDexFile, fieldIndex: Int): String {
        val fieldOffset = dexFile.fieldSection.getOffset(fieldIndex)
        val classIndex = dexFile.buffer.readUshort(fieldOffset + CLASS_OFFSET)
        val classType = dexFile.typeSection.get(classIndex)

        val typeIndex = dexFile.buffer.readUshort(fieldOffset + TYPE_OFFSET)
        val fieldType = dexFile.typeSection.get(typeIndex)

        val nameIndex = dexFile.buffer.readSmallUint(fieldOffset + NAME_OFFSET)
        val fieldName = dexFile.stringSection.get(nameIndex)

        return "${classType}->${fieldName}:${fieldType}"
    }

    fun getReferenceAnnotation(dexFile: DexBackedDexFile, fieldIndex: Int): String {
        try {
            val fieldString = asString(dexFile, fieldIndex)
            return "field_id_item[${fieldIndex}]: ${fieldString}"
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
        return "field_id_item[${fieldIndex}]"
    }

    fun getFields(dexFile: DexBackedDexFile): Array<String> {
        val mapItem = dexFile.getMapItemForSection(ItemType.FIELD_ID_ITEM) ?: return emptyArray()

        val fieldCount = mapItem.itemCount
        return Array(fieldCount) { i -> asString(dexFile, i) }
    }
}
