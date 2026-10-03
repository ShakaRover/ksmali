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

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.StringUtils

object ClassDefItem {
    const val ITEM_SIZE = 32

    const val CLASS_OFFSET = 0
    const val ACCESS_FLAGS_OFFSET = 4
    const val SUPERCLASS_OFFSET = 8
    const val INTERFACES_OFFSET = 12
    const val SOURCE_FILE_OFFSET = 16
    const val ANNOTATIONS_OFFSET = 20
    const val CLASS_DATA_OFFSET = 24
    const val STATIC_VALUES_OFFSET = 28

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            private var classDataAnnotator: SectionAnnotator? = null

            override fun annotateSection(out: AnnotatedBytes) {
                classDataAnnotator = annotator.getAnnotator(ItemType.CLASS_DATA_ITEM)
                super.annotateSection(out)
            }

            override fun getItemName(): String {
                return "class_def_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val classIndex = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "class_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, classIndex))

                val accessFlags = dexFile.buffer.readInt(out.cursor)
                out.annotate(
                    4, "access_flags = 0x%x: %s", accessFlags,
                    StringUtils.join(
                        AccessFlags.getAccessFlagsForClass(accessFlags).asList(), "|"
                    )
                )

                val superclassIndex = dexFile.buffer.readOptionalUint(out.cursor)
                out.annotate(
                    4, "superclass_idx = %s",
                    TypeIdItem.getOptionalReferenceAnnotation(dexFile, superclassIndex)
                )

                val interfacesOffset = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(
                    4, "interfaces_off = %s",
                    TypeListItem.getReferenceAnnotation(dexFile, interfacesOffset)
                )

                val sourceFileIdx = dexFile.buffer.readOptionalUint(out.cursor)
                out.annotate(
                    4, "source_file_idx = %s",
                    StringIdItem.getOptionalReferenceAnnotation(dexFile, sourceFileIdx)
                )

                val annotationsOffset = dexFile.buffer.readSmallUint(out.cursor)
                if (annotationsOffset == 0) {
                    out.annotate(4, "annotations_off = annotations_directory_item[NO_OFFSET]")
                } else {
                    out.annotate(
                        4, "annotations_off = annotations_directory_item[0x%x]", annotationsOffset
                    )
                }

                val classDataOffset = dexFile.buffer.readSmallUint(out.cursor)
                if (classDataOffset == 0) {
                    out.annotate(4, "class_data_off = class_data_item[NO_OFFSET]")
                } else {
                    out.annotate(4, "class_data_off = class_data_item[0x%x]", classDataOffset)
                    addClassDataIdentity(classDataOffset, dexFile.typeSection.get(classIndex))
                }

                val staticValuesOffset = dexFile.buffer.readSmallUint(out.cursor)
                if (staticValuesOffset == 0) {
                    out.annotate(4, "static_values_off = encoded_array_item[NO_OFFSET]")
                } else {
                    out.annotate(4, "static_values_off = encoded_array_item[0x%x]", staticValuesOffset)
                }
            }

            private fun addClassDataIdentity(classDataOffset: Int, classType: String) {
                if (classDataAnnotator != null) {
                    classDataAnnotator!!.setItemIdentity(classDataOffset, classType)
                }
            }
        }
    }

    fun asString(dexFile: DexBackedDexFile, classIndex: Int): String {
        val offset = dexFile.classSection.getOffset(classIndex)
        val typeIndex = dexFile.buffer.readSmallUint(offset + CLASS_OFFSET)
        return dexFile.typeSection.get(typeIndex)
    }

    fun getClasses(dexFile: DexBackedDexFile): Array<String> {
        val mapItem = dexFile.getMapItemForSection(ItemType.CLASS_DEF_ITEM) ?: return emptyArray()

        val classCount = mapItem.itemCount
        return Array(classCount) { i -> asString(dexFile, i) }
    }
}
