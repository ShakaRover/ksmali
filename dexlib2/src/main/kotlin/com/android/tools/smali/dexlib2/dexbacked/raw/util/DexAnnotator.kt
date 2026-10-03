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

package com.android.tools.smali.dexlib2.dexbacked.raw.util

import com.android.tools.smali.dexlib2.dexbacked.CDexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.AnnotationDirectoryItem
import com.android.tools.smali.dexlib2.dexbacked.raw.AnnotationItem
import com.android.tools.smali.dexlib2.dexbacked.raw.AnnotationSetItem
import com.android.tools.smali.dexlib2.dexbacked.raw.AnnotationSetRefList
import com.android.tools.smali.dexlib2.dexbacked.raw.CallSiteIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.CdexDebugOffsetTable
import com.android.tools.smali.dexlib2.dexbacked.raw.ClassDataItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ClassDefItem
import com.android.tools.smali.dexlib2.dexbacked.raw.CodeItem
import com.android.tools.smali.dexlib2.dexbacked.raw.DebugInfoItem
import com.android.tools.smali.dexlib2.dexbacked.raw.EncodedArrayItem
import com.android.tools.smali.dexlib2.dexbacked.raw.FieldIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HiddenApiClassDataItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ItemType
import com.android.tools.smali.dexlib2.dexbacked.raw.MapItem
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodHandleItem
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.SectionAnnotator
import com.android.tools.smali.dexlib2.dexbacked.raw.StringDataItem
import com.android.tools.smali.dexlib2.dexbacked.raw.StringIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeListItem
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import java.io.IOException
import java.io.Writer
import java.util.Arrays
import java.util.Comparator
import java.util.HashMap

class DexAnnotator(
    @JvmField val dexFile: DexBackedDexFile,
    width: Int
) : AnnotatedBytes(width) {
    private val annotators: MutableMap<Int, SectionAnnotator> = HashMap()

    init {
        for (mapItem in dexFile.mapItems) {
            when (mapItem.getType()) {
                ItemType.HEADER_ITEM ->
                    annotators[mapItem.getType()] = HeaderItem.makeAnnotator(this, mapItem)
                ItemType.STRING_ID_ITEM ->
                    annotators[mapItem.getType()] = StringIdItem.makeAnnotator(this, mapItem)
                ItemType.TYPE_ID_ITEM ->
                    annotators[mapItem.getType()] = TypeIdItem.makeAnnotator(this, mapItem)
                ItemType.PROTO_ID_ITEM ->
                    annotators[mapItem.getType()] = ProtoIdItem.makeAnnotator(this, mapItem)
                ItemType.FIELD_ID_ITEM ->
                    annotators[mapItem.getType()] = FieldIdItem.makeAnnotator(this, mapItem)
                ItemType.METHOD_ID_ITEM ->
                    annotators[mapItem.getType()] = MethodIdItem.makeAnnotator(this, mapItem)
                ItemType.CLASS_DEF_ITEM ->
                    annotators[mapItem.getType()] = ClassDefItem.makeAnnotator(this, mapItem)
                ItemType.MAP_LIST ->
                    annotators[mapItem.getType()] = MapItem.makeAnnotator(this, mapItem)
                ItemType.TYPE_LIST ->
                    annotators[mapItem.getType()] = TypeListItem.makeAnnotator(this, mapItem)
                ItemType.ANNOTATION_SET_REF_LIST ->
                    annotators[mapItem.getType()] =
                        AnnotationSetRefList.makeAnnotator(this, mapItem)
                ItemType.ANNOTATION_SET_ITEM ->
                    annotators[mapItem.getType()] = AnnotationSetItem.makeAnnotator(this, mapItem)
                ItemType.CLASS_DATA_ITEM ->
                    annotators[mapItem.getType()] = ClassDataItem.makeAnnotator(this, mapItem)
                ItemType.CODE_ITEM ->
                    annotators[mapItem.getType()] = CodeItem.makeAnnotator(this, mapItem)
                ItemType.STRING_DATA_ITEM ->
                    annotators[mapItem.getType()] = StringDataItem.makeAnnotator(this, mapItem)
                ItemType.DEBUG_INFO_ITEM ->
                    annotators[mapItem.getType()] = DebugInfoItem.makeAnnotator(this, mapItem)
                ItemType.ANNOTATION_ITEM ->
                    annotators[mapItem.getType()] = AnnotationItem.makeAnnotator(this, mapItem)
                ItemType.ENCODED_ARRAY_ITEM ->
                    annotators[mapItem.getType()] = EncodedArrayItem.makeAnnotator(this, mapItem)
                ItemType.ANNOTATION_DIRECTORY_ITEM ->
                    annotators[mapItem.getType()] =
                        AnnotationDirectoryItem.makeAnnotator(this, mapItem)
                ItemType.CALL_SITE_ID_ITEM ->
                    annotators[mapItem.getType()] = CallSiteIdItem.makeAnnotator(this, mapItem)
                ItemType.METHOD_HANDLE_ITEM ->
                    annotators[mapItem.getType()] = MethodHandleItem.makeAnnotator(this, mapItem)
                ItemType.HIDDENAPI_CLASS_DATA_ITEM ->
                    annotators[mapItem.getType()] =
                        HiddenApiClassDataItem.makeAnnotator(this, mapItem)
                else -> throw RuntimeException(
                    String.format("Unrecognized item type: 0x%x", mapItem.getType())
                )
            }
        }
    }

    @Throws(IOException::class)
    fun writeAnnotations(out: Writer) {
        val mapItems = dexFile.mapItems
        // sort the map items based on the order defined by sectionAnnotationOrder
        val comparator = Comparator<MapItem> { o1, o2 ->
            Integer.compare(
                sectionAnnotationOrder[o1.getType()]!!,
                sectionAnnotationOrder[o2.getType()]!!
            )
        }

        val mapItemsArray = mapItems.toTypedArray()
        Arrays.sort(mapItemsArray, comparator)

        try {
            // Need to annotate the debug info offset table first, to propagate the debug info identities
            if (dexFile is CDexBackedDexFile) {
                moveTo(dexFile.baseDataOffset + (dexFile as CDexBackedDexFile).getDebugInfoOffsetsPos())
                CdexDebugOffsetTable.annotate(this, dexFile.buffer)
            }

            for (mapItem in mapItemsArray) {
                try {
                    val annotator = annotators[mapItem.getType()]
                    annotator!!.annotateSection(this)
                } catch (ex: Exception) {
                    System.err.println(
                        String.format(
                            "There was an error while dumping the %s section",
                            ItemType.getItemTypeName(mapItem.getType())
                        )
                    )
                    ex.printStackTrace(System.err)
                }
            }
        } finally {
            writeAnnotations(out, dexFile.buffer.buf, dexFile.buffer.baseOffset)
        }
    }

    fun getAnnotator(itemType: Int): SectionAnnotator? {
        return annotators[itemType]
    }

    companion object {
        private val sectionAnnotationOrder: MutableMap<Int, Int> = HashMap()

        init {
            val sectionOrder = intArrayOf(
                ItemType.MAP_LIST,

                ItemType.HEADER_ITEM,
                ItemType.STRING_ID_ITEM,
                ItemType.TYPE_ID_ITEM,
                ItemType.PROTO_ID_ITEM,
                ItemType.FIELD_ID_ITEM,
                ItemType.METHOD_ID_ITEM,
                ItemType.CALL_SITE_ID_ITEM,
                ItemType.METHOD_HANDLE_ITEM,

                // these need to be ordered like this, so the item identities can be propagated
                ItemType.CLASS_DEF_ITEM,
                ItemType.CLASS_DATA_ITEM,
                ItemType.CODE_ITEM,
                ItemType.DEBUG_INFO_ITEM,

                ItemType.TYPE_LIST,
                ItemType.ANNOTATION_SET_REF_LIST,
                ItemType.ANNOTATION_SET_ITEM,
                ItemType.STRING_DATA_ITEM,
                ItemType.ANNOTATION_ITEM,
                ItemType.ENCODED_ARRAY_ITEM,
                ItemType.ANNOTATION_DIRECTORY_ITEM,

                ItemType.HIDDENAPI_CLASS_DATA_ITEM
            )

            for (i in sectionOrder.indices) {
                sectionAnnotationOrder[sectionOrder[i]] = i
            }
        }
    }
}
