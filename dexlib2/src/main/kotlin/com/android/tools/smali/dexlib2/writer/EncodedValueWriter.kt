/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.BaseAnnotationElement
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.util.CollectionUtils
import java.io.IOException

abstract class EncodedValueWriter<StringKey, TypeKey, FieldRefKey : FieldReference,
    MethodRefKey : MethodReference, AnnotationElement : com.android.tools.smali.dexlib2.iface.AnnotationElement,
    ProtoRefKey, MethodHandleKey : MethodHandleReference, EncodedValue>(
    private val writer: DexDataWriter,
    private val stringSection: StringSection<StringKey, *>,
    private val typeSection: TypeSection<*, TypeKey, *>,
    private val fieldSection: FieldSection<*, *, FieldRefKey, *>,
    private val methodSection: MethodSection<*, *, *, MethodRefKey, *>,
    private val protoSection: ProtoSection<*, *, ProtoRefKey, *>,
    private val methodHandleSection: MethodHandleSection<MethodHandleKey, *, *>,
    private val annotationSection: AnnotationSection<StringKey, TypeKey, *, AnnotationElement, EncodedValue>,
) {
    @Throws(IOException::class)
    protected abstract fun writeEncodedValue(encodedValue: EncodedValue)

    @Throws(IOException::class)
    fun writeAnnotation(annotationType: TypeKey, elements: Collection<@JvmWildcard AnnotationElement>) {
        writer.writeEncodedValueHeader(ValueType.ANNOTATION, 0)
        writer.writeUleb128(typeSection.getItemIndex(annotationType))
        writer.writeUleb128(elements.size)

        val sortedElements = CollectionUtils.immutableSortedCopy(elements, BaseAnnotationElement.BY_NAME)

        for (element in sortedElements) {
            writer.writeUleb128(stringSection.getItemIndex(annotationSection.getElementName(element)))
            writeEncodedValue(annotationSection.getElementValue(element))
        }
    }

    @Throws(IOException::class)
    fun writeArray(elements: Collection<@JvmWildcard EncodedValue>) {
        writer.writeEncodedValueHeader(ValueType.ARRAY, 0)
        writer.writeUleb128(elements.size)
        for (element in elements) {
            writeEncodedValue(element)
        }
    }

    @Throws(IOException::class)
    fun writeBoolean(value: Boolean) {
        writer.writeEncodedValueHeader(ValueType.BOOLEAN, if (value) 1 else 0)
    }

    @Throws(IOException::class)
    fun writeByte(value: Byte) {
        writer.writeEncodedInt(ValueType.BYTE, value.toInt())
    }

    @Throws(IOException::class)
    fun writeChar(value: Char) {
        writer.writeEncodedUint(ValueType.CHAR, value.code)
    }

    @Throws(IOException::class)
    fun writeDouble(value: Double) {
        writer.writeEncodedDouble(ValueType.DOUBLE, value)
    }

    @Throws(IOException::class)
    fun writeEnum(value: FieldRefKey) {
        writer.writeEncodedUint(ValueType.ENUM, fieldSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeField(value: FieldRefKey) {
        writer.writeEncodedUint(ValueType.FIELD, fieldSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeFloat(value: Float) {
        writer.writeEncodedFloat(ValueType.FLOAT, value)
    }

    @Throws(IOException::class)
    fun writeInt(value: Int) {
        writer.writeEncodedInt(ValueType.INT, value)
    }

    @Throws(IOException::class)
    fun writeLong(value: Long) {
        writer.writeEncodedLong(ValueType.LONG, value)
    }

    @Throws(IOException::class)
    fun writeMethod(value: MethodRefKey) {
        writer.writeEncodedUint(ValueType.METHOD, methodSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeNull() {
        writer.write(ValueType.NULL)
    }

    @Throws(IOException::class)
    fun writeShort(value: Int) {
        writer.writeEncodedInt(ValueType.SHORT, value)
    }

    @Throws(IOException::class)
    fun writeString(value: StringKey) {
        writer.writeEncodedUint(ValueType.STRING, stringSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeType(value: TypeKey) {
        writer.writeEncodedUint(ValueType.TYPE, typeSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeMethodType(value: ProtoRefKey) {
        writer.writeEncodedUint(ValueType.METHOD_TYPE, protoSection.getItemIndex(value))
    }

    @Throws(IOException::class)
    fun writeMethodHandle(value: MethodHandleKey) {
        writer.writeEncodedUint(ValueType.METHOD_HANDLE, methodHandleSection.getItemIndex(value))
    }
}
