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

package com.android.tools.smali.dexlib2

object ValueType {
    const val BYTE = 0x00
    const val SHORT = 0x02
    const val CHAR = 0x03
    const val INT = 0x04
    const val LONG = 0x06
    const val FLOAT = 0x10
    const val DOUBLE = 0x11
    const val METHOD_TYPE = 0x15
    const val METHOD_HANDLE = 0x16
    const val STRING = 0x17
    const val TYPE = 0x18
    const val FIELD = 0x19
    const val METHOD = 0x1a
    const val ENUM = 0x1b
    const val ARRAY = 0x1c
    const val ANNOTATION = 0x1d
    const val NULL = 0x1e
    const val BOOLEAN = 0x1f

    @JvmStatic
    fun getValueTypeName(valueType: Int): String {
        return when (valueType) {
            BYTE -> "byte"
            SHORT -> "short"
            CHAR -> "char"
            INT -> "int"
            LONG -> "long"
            FLOAT -> "float"
            DOUBLE -> "double"
            METHOD_TYPE -> "method_type"
            METHOD_HANDLE -> "method_handle"
            STRING -> "string"
            TYPE -> "type"
            FIELD -> "field"
            METHOD -> "method"
            ENUM -> "enum"
            ARRAY -> "array"
            ANNOTATION -> "annotation"
            NULL -> "null"
            BOOLEAN -> "boolean"
            else -> throw IllegalArgumentException("Unknown encoded value type: $valueType")
        }
    }
}
