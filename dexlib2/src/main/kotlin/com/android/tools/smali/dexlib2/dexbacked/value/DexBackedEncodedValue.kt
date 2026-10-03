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

package com.android.tools.smali.dexlib2.dexbacked.value

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableBooleanEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableByteEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableCharEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableDoubleEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableFloatEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableLongEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableNullEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableShortEncodedValue
import com.android.tools.smali.dexlib2.util.checkValueArg
import com.android.tools.smali.util.ExceptionWithContext

abstract class DexBackedEncodedValue {
    companion object {
        fun readFrom(dexFile: DexBackedDexFile, reader: DexReader<out DexBuffer>): EncodedValue {
            val startOffset = reader.offset

            try {
                val b = reader.readUbyte()
                val valueType = b and 0x1f
                val valueArg = b ushr 5

                return when (valueType) {
                    ValueType.BYTE -> {
                        checkValueArg(valueArg, 0)
                        ImmutableByteEncodedValue(reader.readByte().toByte())
                    }
                    ValueType.SHORT -> {
                        checkValueArg(valueArg, 1)
                        ImmutableShortEncodedValue(
                            reader.readSizedInt(valueArg + 1).toShort()
                        )
                    }
                    ValueType.CHAR -> {
                        checkValueArg(valueArg, 1)
                        ImmutableCharEncodedValue(
                            reader.readSizedSmallUint(valueArg + 1).toChar()
                        )
                    }
                    ValueType.INT -> {
                        checkValueArg(valueArg, 3)
                        ImmutableIntEncodedValue(reader.readSizedInt(valueArg + 1))
                    }
                    ValueType.LONG -> {
                        checkValueArg(valueArg, 7)
                        ImmutableLongEncodedValue(reader.readSizedLong(valueArg + 1))
                    }
                    ValueType.FLOAT -> {
                        checkValueArg(valueArg, 3)
                        ImmutableFloatEncodedValue(
                            Float.fromBits(reader.readSizedRightExtendedInt(valueArg + 1))
                        )
                    }
                    ValueType.DOUBLE -> {
                        checkValueArg(valueArg, 7)
                        ImmutableDoubleEncodedValue(
                            Double.fromBits(reader.readSizedRightExtendedLong(valueArg + 1))
                        )
                    }
                    ValueType.STRING -> {
                        checkValueArg(valueArg, 3)
                        DexBackedStringEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.TYPE -> {
                        checkValueArg(valueArg, 3)
                        DexBackedTypeEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.FIELD -> {
                        checkValueArg(valueArg, 3)
                        DexBackedFieldEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.METHOD -> {
                        checkValueArg(valueArg, 3)
                        DexBackedMethodEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.ENUM -> {
                        checkValueArg(valueArg, 3)
                        DexBackedEnumEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.ARRAY -> {
                        checkValueArg(valueArg, 0)
                        DexBackedArrayEncodedValue(dexFile, reader)
                    }
                    ValueType.ANNOTATION -> {
                        checkValueArg(valueArg, 0)
                        DexBackedAnnotationEncodedValue(dexFile, reader)
                    }
                    ValueType.NULL -> {
                        checkValueArg(valueArg, 0)
                        ImmutableNullEncodedValue.INSTANCE
                    }
                    ValueType.BOOLEAN -> {
                        checkValueArg(valueArg, 1)
                        ImmutableBooleanEncodedValue.forBoolean(valueArg == 1)
                    }
                    ValueType.METHOD_HANDLE -> {
                        checkValueArg(valueArg, 3)
                        DexBackedMethodHandleEncodedValue(dexFile, reader, valueArg)
                    }
                    ValueType.METHOD_TYPE -> {
                        checkValueArg(valueArg, 3)
                        DexBackedMethodTypeEncodedValue(dexFile, reader, valueArg)
                    }
                    else -> throw ExceptionWithContext(
                        "Invalid encoded_value type: 0x%x", valueType
                    )
                }
            } catch (ex: Exception) {
                throw ExceptionWithContext.withContext(
                    ex, "Error while reading encoded value at offset 0x%x", startOffset
                )
            }
        }

        fun skipFrom(reader: DexReader<out DexBuffer>) {
            val startOffset = reader.offset

            try {
                val b = reader.readUbyte()
                val valueType = b and 0x1f

                when (valueType) {
                    ValueType.BYTE -> reader.skipByte()
                    ValueType.SHORT, ValueType.CHAR, ValueType.INT, ValueType.LONG,
                    ValueType.FLOAT, ValueType.DOUBLE, ValueType.STRING, ValueType.TYPE,
                    ValueType.FIELD, ValueType.METHOD, ValueType.ENUM,
                    ValueType.METHOD_HANDLE, ValueType.METHOD_TYPE -> {
                        val valueArg = b ushr 5
                        reader.moveRelative(valueArg + 1)
                    }
                    ValueType.ARRAY -> DexBackedArrayEncodedValue.skipFrom(reader)
                    ValueType.ANNOTATION -> DexBackedAnnotationEncodedValue.skipFrom(reader)
                    ValueType.NULL, ValueType.BOOLEAN -> {
                    }
                    else -> throw ExceptionWithContext(
                        "Invalid encoded_value type: 0x%x", valueType
                    )
                }
            } catch (ex: Exception) {
                throw ExceptionWithContext.withContext(
                    ex, "Error while skipping encoded value at offset 0x%x", startOffset
                )
            }
        }
    }
}
