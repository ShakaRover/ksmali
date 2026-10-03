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

object ProtoIdItem {
    const val ITEM_SIZE = 12

    const val SHORTY_OFFSET = 0
    const val RETURN_TYPE_OFFSET = 4
    const val PARAMETERS_OFFSET = 8

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "proto_id_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val shortyIndex = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "shorty_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, shortyIndex))

                val returnTypeIndex = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "return_type_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, returnTypeIndex))

                val parametersOffset = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "parameters_off = %s", TypeListItem.getReferenceAnnotation(dexFile, parametersOffset))
            }
        }
    }

    fun getReferenceAnnotation(dexFile: DexBackedDexFile, protoIndex: Int): String {
        try {
            val protoString = asString(dexFile, protoIndex)
            return "proto_id_item[${protoIndex}]: ${protoString}"
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
        return "proto_id_item[${protoIndex}]"
    }

    fun asString(dexFile: DexBackedDexFile, protoIndex: Int): String {
        val offset = dexFile.protoSection.getOffset(protoIndex)

        val parametersOffset = dexFile.buffer.readSmallUint(offset + PARAMETERS_OFFSET)
        val returnTypeIndex = dexFile.buffer.readSmallUint(offset + RETURN_TYPE_OFFSET)
        val returnType = dexFile.typeSection.get(returnTypeIndex)

        return buildString {
            append("(")
            append(TypeListItem.asString(dexFile, parametersOffset))
            append(")")
            append(returnType)
        }
    }

    fun getProtos(dexFile: DexBackedDexFile): Array<String> {
        val mapItem = dexFile.getMapItemForSection(ItemType.PROTO_ID_ITEM) ?: return emptyArray()

        val protoCount = mapItem.itemCount
        return Array(protoCount) { i -> asString(dexFile, i) }
    }
}
