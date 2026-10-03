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

object MethodIdItem {
    const val ITEM_SIZE = 8

    const val CLASS_OFFSET = 0
    const val PROTO_OFFSET = 2
    const val NAME_OFFSET = 4

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "method_id_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val classIndex = dexFile.buffer.readUshort(out.cursor)
                out.annotate(2, "class_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, classIndex))

                val protoIndex = dexFile.buffer.readUshort(out.cursor)
                out.annotate(2, "proto_idx = %s", ProtoIdItem.getReferenceAnnotation(dexFile, protoIndex))

                val nameIndex = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "name_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, nameIndex))
            }
        }
    }

    fun asString(dexFile: DexBackedDexFile, methodIndex: Int): String {
        val methodOffset = dexFile.methodSection.getOffset(methodIndex)
        val classIndex = dexFile.buffer.readUshort(methodOffset + CLASS_OFFSET)
        val classType = dexFile.typeSection.get(classIndex)

        val protoIndex = dexFile.buffer.readUshort(methodOffset + PROTO_OFFSET)
        val protoString = ProtoIdItem.asString(dexFile, protoIndex)

        val nameIndex = dexFile.buffer.readSmallUint(methodOffset + NAME_OFFSET)
        val methodName = dexFile.stringSection.get(nameIndex)

        return "${classType}->${methodName}${protoString}"
    }

    fun getReferenceAnnotation(dexFile: DexBackedDexFile, methodIndex: Int): String {
        try {
            val methodString = asString(dexFile, methodIndex)
            return "method_id_item[${methodIndex}]: ${methodString}"
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
        return "method_id_item[${methodIndex}]"
    }

    fun getMethods(dexFile: DexBackedDexFile): Array<String> {
        val mapItem = dexFile.getMapItemForSection(ItemType.METHOD_ID_ITEM) ?: return emptyArray()

        val methodCount = mapItem.itemCount
        return Array(methodCount) { i -> asString(dexFile, i) }
    }
}
