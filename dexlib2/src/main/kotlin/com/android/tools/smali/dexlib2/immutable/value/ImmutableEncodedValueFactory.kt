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

package com.android.tools.smali.dexlib2.immutable.value

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.BooleanEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue
import com.android.tools.smali.dexlib2.iface.value.CharEncodedValue
import com.android.tools.smali.dexlib2.iface.value.DoubleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.EnumEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FloatEncodedValue
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue
import com.android.tools.smali.dexlib2.iface.value.LongEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ShortEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.ImmutableConverter

object ImmutableEncodedValueFactory {
    fun of(encodedValue: EncodedValue): ImmutableEncodedValue {
        when (encodedValue.valueType) {
            ValueType.BYTE -> return ImmutableByteEncodedValue.of(encodedValue as ByteEncodedValue)
            ValueType.SHORT -> return ImmutableShortEncodedValue.of(encodedValue as ShortEncodedValue)
            ValueType.CHAR -> return ImmutableCharEncodedValue.of(encodedValue as CharEncodedValue)
            ValueType.INT -> return ImmutableIntEncodedValue.of(encodedValue as IntEncodedValue)
            ValueType.LONG -> return ImmutableLongEncodedValue.of(encodedValue as LongEncodedValue)
            ValueType.FLOAT -> return ImmutableFloatEncodedValue.of(encodedValue as FloatEncodedValue)
            ValueType.DOUBLE -> return ImmutableDoubleEncodedValue.of(encodedValue as DoubleEncodedValue)
            ValueType.STRING -> return ImmutableStringEncodedValue.of(encodedValue as StringEncodedValue)
            ValueType.TYPE -> return ImmutableTypeEncodedValue.of(encodedValue as TypeEncodedValue)
            ValueType.FIELD -> return ImmutableFieldEncodedValue.of(encodedValue as FieldEncodedValue)
            ValueType.METHOD -> return ImmutableMethodEncodedValue.of(encodedValue as MethodEncodedValue)
            ValueType.ENUM -> return ImmutableEnumEncodedValue.of(encodedValue as EnumEncodedValue)
            ValueType.ARRAY -> return ImmutableArrayEncodedValue.of(encodedValue as ArrayEncodedValue)
            ValueType.ANNOTATION -> return ImmutableAnnotationEncodedValue.of(
                encodedValue as AnnotationEncodedValue
            )
            ValueType.NULL -> return ImmutableNullEncodedValue.INSTANCE
            ValueType.BOOLEAN -> return ImmutableBooleanEncodedValue.of(encodedValue as BooleanEncodedValue)
            ValueType.METHOD_HANDLE -> return ImmutableMethodHandleEncodedValue.of(
                encodedValue as MethodHandleEncodedValue
            )
            ValueType.METHOD_TYPE -> return ImmutableMethodTypeEncodedValue.of(
                encodedValue as MethodTypeEncodedValue
            )
            else -> throw IllegalArgumentException("Invalid value type.")
        }
    }

    fun defaultValueForType(type: String): EncodedValue {
        when (type[0]) {
            'Z' -> return ImmutableBooleanEncodedValue.FALSE_VALUE
            'B' -> return ImmutableByteEncodedValue(0.toByte())
            'S' -> return ImmutableShortEncodedValue(0.toShort())
            'C' -> return ImmutableCharEncodedValue(0.toChar())
            'I' -> return ImmutableIntEncodedValue(0)
            'J' -> return ImmutableLongEncodedValue(0L)
            'F' -> return ImmutableFloatEncodedValue(0f)
            'D' -> return ImmutableDoubleEncodedValue(0.0)
            'L', '[' -> return ImmutableNullEncodedValue.INSTANCE
            else -> throw ExceptionWithContext("Unrecognized type: %s", type)
        }
    }

    fun ofNullable(encodedValue: EncodedValue?): ImmutableEncodedValue? {
        if (encodedValue == null) {
            return null
        }
        return of(encodedValue)
    }

    fun immutableListOf(list: Iterable<EncodedValue>?): List<ImmutableEncodedValue> {
        return CONVERTER.toList(list)
    }

    private val CONVERTER: ImmutableConverter<ImmutableEncodedValue, EncodedValue> =
        object : ImmutableConverter<ImmutableEncodedValue, EncodedValue>() {
            override fun isImmutable(item: EncodedValue): Boolean {
                return item is ImmutableEncodedValue
            }

            override fun makeImmutable(item: EncodedValue): ImmutableEncodedValue {
                return of(item)
            }
        }
}
