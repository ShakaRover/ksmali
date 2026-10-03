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

package com.android.tools.smali.dexlib2.util

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
import com.android.tools.smali.util.StringUtils
import java.io.IOException
import java.io.Writer

object EncodedValueUtils {
    @JvmStatic
    fun isDefaultValue(encodedValue: EncodedValue): Boolean {
        when (encodedValue.valueType) {
            ValueType.BOOLEAN -> return !(encodedValue as BooleanEncodedValue).value
            ValueType.BYTE -> return (encodedValue as ByteEncodedValue).value.toInt() == 0
            ValueType.CHAR -> return (encodedValue as CharEncodedValue).value.code == 0
            ValueType.DOUBLE -> return (encodedValue as DoubleEncodedValue).value == 0.0
            ValueType.FLOAT -> return (encodedValue as FloatEncodedValue).value == 0f
            ValueType.INT -> return (encodedValue as IntEncodedValue).value == 0
            ValueType.LONG -> return (encodedValue as LongEncodedValue).value == 0L
            ValueType.NULL -> return true
            ValueType.SHORT -> return (encodedValue as ShortEncodedValue).value.toInt() == 0
        }
        return false
    }

    /**
     * @deprecated use [DexFormatter] instead.
     */
    @Deprecated("use DexFormatter instead")
    @JvmStatic
    @Throws(IOException::class)
    fun writeEncodedValue(writer: Writer, encodedValue: EncodedValue) {
        when (encodedValue.valueType) {
            ValueType.BOOLEAN ->
                writer.write(java.lang.Boolean.toString((encodedValue as BooleanEncodedValue).value))
            ValueType.BYTE ->
                writer.write(java.lang.Byte.toString((encodedValue as ByteEncodedValue).value))
            ValueType.CHAR ->
                writer.write(Integer.toString((encodedValue as CharEncodedValue).value.code))
            ValueType.SHORT ->
                writer.write(java.lang.Short.toString((encodedValue as ShortEncodedValue).value))
            ValueType.INT ->
                writer.write(Integer.toString((encodedValue as IntEncodedValue).value))
            ValueType.LONG ->
                writer.write(java.lang.Long.toString((encodedValue as LongEncodedValue).value))
            ValueType.FLOAT ->
                writer.write(java.lang.Float.toString((encodedValue as FloatEncodedValue).value))
            ValueType.DOUBLE ->
                writer.write(java.lang.Double.toString((encodedValue as DoubleEncodedValue).value))
            ValueType.ANNOTATION ->
                writeAnnotation(writer, encodedValue as AnnotationEncodedValue)
            ValueType.ARRAY ->
                writeArray(writer, encodedValue as ArrayEncodedValue)
            ValueType.STRING -> {
                writer.write('"'.code)
                StringUtils.writeEscapedString(writer, (encodedValue as StringEncodedValue).value)
                writer.write('"'.code)
            }
            ValueType.FIELD ->
                writeFieldDescriptor(writer, (encodedValue as FieldEncodedValue).value)
            ValueType.ENUM ->
                writeFieldDescriptor(writer, (encodedValue as EnumEncodedValue).value)
            ValueType.METHOD ->
                writeMethodDescriptor(writer, (encodedValue as MethodEncodedValue).value)
            ValueType.TYPE ->
                writer.write((encodedValue as TypeEncodedValue).value)
            ValueType.METHOD_TYPE ->
                writeMethodProtoDescriptor(writer, (encodedValue as MethodTypeEncodedValue).value)
            ValueType.METHOD_HANDLE ->
                writeMethodHandle(writer, (encodedValue as MethodHandleEncodedValue).value)
            ValueType.NULL -> writer.write("null")
            else -> throw IllegalArgumentException("Unknown encoded value type")
        }
    }

    private fun writeAnnotation(writer: Writer, annotation: AnnotationEncodedValue) {
        writer.write("Annotation[")
        writer.write(annotation.type)

        val elements = annotation.elements
        for (element in elements) {
            writer.write(", ")
            writer.write(element.name)
            writer.write('='.code)
            writeEncodedValue(writer, element.value)
        }

        writer.write(']'.code)
    }

    private fun writeArray(writer: Writer, array: ArrayEncodedValue) {
        writer.write("Array[")

        var first = true
        for (element in array.value) {
            if (first) {
                first = false
            } else {
                writer.write(", ")
            }
            writeEncodedValue(writer, element)
        }

        writer.write(']'.code)
    }
}
