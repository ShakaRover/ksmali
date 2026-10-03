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

object ItemType {
    const val HEADER_ITEM = 0x0000
    const val STRING_ID_ITEM = 0x0001
    const val TYPE_ID_ITEM = 0x0002
    const val PROTO_ID_ITEM = 0x0003
    const val FIELD_ID_ITEM = 0x0004
    const val METHOD_ID_ITEM = 0x0005
    const val CLASS_DEF_ITEM = 0x0006
    const val CALL_SITE_ID_ITEM = 0x0007
    const val METHOD_HANDLE_ITEM = 0x0008
    const val MAP_LIST = 0x1000
    const val TYPE_LIST = 0x1001
    const val ANNOTATION_SET_REF_LIST = 0x1002
    const val ANNOTATION_SET_ITEM = 0x1003
    const val CLASS_DATA_ITEM = 0x2000
    const val CODE_ITEM = 0x2001
    const val STRING_DATA_ITEM = 0x2002
    const val DEBUG_INFO_ITEM = 0x2003
    const val ANNOTATION_ITEM = 0x2004
    const val ENCODED_ARRAY_ITEM = 0x2005
    const val ANNOTATION_DIRECTORY_ITEM = 0x2006
    const val HIDDENAPI_CLASS_DATA_ITEM = 0xF000

    fun getItemTypeName(itemType: Int): String {
        return when (itemType) {
            HEADER_ITEM -> "header_item"
            STRING_ID_ITEM -> "string_id_item"
            TYPE_ID_ITEM -> "type_id_item"
            PROTO_ID_ITEM -> "proto_id_item"
            FIELD_ID_ITEM -> "field_id_item"
            METHOD_ID_ITEM -> "method_id_item"
            CLASS_DEF_ITEM -> "class_def_item"
            CALL_SITE_ID_ITEM -> "call_site_id_item"
            METHOD_HANDLE_ITEM -> "method_handle_item"
            MAP_LIST -> "map_list"
            TYPE_LIST -> "type_list"
            ANNOTATION_SET_REF_LIST -> "annotation_set_ref_list"
            ANNOTATION_SET_ITEM -> "annotation_set_item"
            CLASS_DATA_ITEM -> "class_data_item"
            CODE_ITEM -> "code_item"
            STRING_DATA_ITEM -> "string_data_item"
            DEBUG_INFO_ITEM -> "debug_info_item"
            ANNOTATION_ITEM -> "annotation_item"
            ENCODED_ARRAY_ITEM -> "encoded_array_item"
            ANNOTATION_DIRECTORY_ITEM -> "annotation_directory_item"
            HIDDENAPI_CLASS_DATA_ITEM -> "hiddenapi_class_data_item"
            else -> "unknown dex item type"
        }
    }
}
