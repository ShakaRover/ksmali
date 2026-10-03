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

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.value.DexBackedEncodedValue
import com.android.tools.smali.dexlib2.util.AnnotatedBytes

object EncodedValue {
    fun annotateEncodedValue(
        dexFile: DexBackedDexFile,
        out: AnnotatedBytes,
        reader: DexReader<out DexBuffer>
    ) {
        val valueArgType = reader.readUbyte()

        val valueArg = valueArgType ushr 5
        val valueType = valueArgType and 0x1f

        when (valueType) {
            ValueType.BYTE,
            ValueType.SHORT,
            ValueType.CHAR,
            ValueType.INT,
            ValueType.LONG,
            ValueType.FLOAT,
            ValueType.DOUBLE,
            ValueType.METHOD_TYPE,
            ValueType.METHOD_HANDLE,
            ValueType.STRING,
            ValueType.TYPE,
            ValueType.FIELD,
            ValueType.METHOD,
            ValueType.ENUM -> {
                out.annotate(
                    1, "valueArg = %d, valueType = 0x%x: %s", valueArg, valueType,
                    ValueType.getValueTypeName(valueType)
                )
                reader.offset = reader.offset - 1
                out.annotate(valueArg + 1, "value = %s", asString(dexFile, reader))
            }
            ValueType.ARRAY -> {
                out.annotate(1, "valueArg = %d, valueType = 0x%x: array", valueArg, valueType)
                annotateEncodedArray(dexFile, out, reader)
            }
            ValueType.ANNOTATION -> {
                out.annotate(1, "valueArg = %d, valueType = 0x%x: annotation", valueArg, valueType)
                annotateEncodedAnnotation(dexFile, out, reader)
            }
            ValueType.NULL -> {
                out.annotate(1, "valueArg = %d, valueType = 0x%x: null", valueArg, valueType)
            }
            ValueType.BOOLEAN -> {
                out.annotate(
                    1, "valueArg = %d, valueType = 0x%x: boolean, value=%s", valueArg, valueType,
                    valueArg == 1
                )
            }
            else -> throw IllegalArgumentException(
                "Invalid encoded value type 0x%x at offset 0x%x".format(valueType, reader.offset)
            )
        }
    }

    fun annotateEncodedAnnotation(
        dexFile: DexBackedDexFile,
        out: AnnotatedBytes,
        reader: DexReader<out DexBuffer>
    ) {
        assert(out.cursor == reader.offset)

        val typeIndex = reader.readSmallUleb128()
        out.annotateTo(reader.offset, TypeIdItem.getReferenceAnnotation(dexFile, typeIndex))

        val size = reader.readSmallUleb128()
        out.annotateTo(reader.offset, "size: %d", size)

        for (i in 0 until size) {
            out.annotate(0, "element[%d]", i)
            out.indent()

            val nameIndex = reader.readSmallUleb128()
            out.annotateTo(
                reader.offset, "name = %s",
                StringIdItem.getReferenceAnnotation(dexFile, nameIndex)
            )

            annotateEncodedValue(dexFile, out, reader)

            out.deindent()
        }
    }

    fun annotateEncodedArray(
        dexFile: DexBackedDexFile,
        out: AnnotatedBytes,
        reader: DexReader<out DexBuffer>
    ) {
        assert(out.cursor == reader.offset)

        val size = reader.readSmallUleb128()
        out.annotateTo(reader.offset, "size: %d", size)

        for (i in 0 until size) {
            out.annotate(0, "element[%d]", i)
            out.indent()

            annotateEncodedValue(dexFile, out, reader)

            out.deindent()
        }
    }

    fun asString(dexFile: DexBackedDexFile, reader: DexReader<out DexBuffer>): String {
        val valueArgType = reader.readUbyte()

        val valueArg = valueArgType ushr 5
        val valueType = valueArgType and 0x1f

        return when (valueType) {
            ValueType.BYTE -> {
                val intValue = reader.readByte()
                "0x%x".format(intValue)
            }
            ValueType.SHORT -> {
                val intValue = reader.readSizedInt(valueArg + 1)
                "0x%x".format(intValue)
            }
            ValueType.CHAR -> {
                val intValue = reader.readSizedSmallUint(valueArg + 1)
                "0x%x".format(intValue)
            }
            ValueType.INT -> {
                val intValue = reader.readSizedInt(valueArg + 1)
                "0x%x".format(intValue)
            }
            ValueType.LONG -> {
                val longValue = reader.readSizedLong(valueArg + 1)
                "0x%x".format(longValue)
            }
            ValueType.FLOAT -> {
                val floatValue = Float.fromBits(reader.readSizedRightExtendedInt(valueArg + 1))
                "%f".format(floatValue)
            }
            ValueType.DOUBLE -> {
                val doubleValue = Double.fromBits(reader.readSizedRightExtendedLong(valueArg + 1))
                "%f".format(doubleValue)
            }
            ValueType.METHOD_TYPE -> {
                val protoIndex = reader.readSizedSmallUint(valueArg + 1)
                ProtoIdItem.getReferenceAnnotation(dexFile, protoIndex)
            }
            ValueType.STRING -> {
                val stringIndex = reader.readSizedSmallUint(valueArg + 1)
                StringIdItem.getReferenceAnnotation(dexFile, stringIndex, true)
            }
            ValueType.TYPE -> {
                val typeIndex = reader.readSizedSmallUint(valueArg + 1)
                TypeIdItem.getReferenceAnnotation(dexFile, typeIndex)
            }
            ValueType.FIELD -> {
                val fieldIndex = reader.readSizedSmallUint(valueArg + 1)
                FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex)
            }
            ValueType.METHOD -> {
                val methodIndex = reader.readSizedSmallUint(valueArg + 1)
                MethodIdItem.getReferenceAnnotation(dexFile, methodIndex)
            }
            ValueType.ENUM -> {
                val fieldIndex = reader.readSizedSmallUint(valueArg + 1)
                FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex)
            }
            ValueType.ARRAY,
            ValueType.ANNOTATION,
            ValueType.METHOD_HANDLE -> {
                reader.offset = reader.offset - 1
                DexBackedEncodedValue.readFrom(dexFile, reader).toString()
            }
            ValueType.NULL -> "null"
            ValueType.BOOLEAN -> java.lang.Boolean.toString(valueArg == 1)
            else -> throw IllegalArgumentException(
                "Invalid encoded value type 0x%x at offset 0x%x".format(valueType, reader.offset)
            )
        }
    }
}
