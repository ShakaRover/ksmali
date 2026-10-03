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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
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
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.io.DexDataStore
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.util.ExceptionWithContext
import java.io.File
import java.io.IOException

class DexPool(opcodes: Opcodes) : DexWriter<CharSequence, StringReference, CharSequence, TypeReference,
    MethodProtoReference, FieldReference, MethodReference, PoolClassDef,
    CallSiteReference, MethodHandleReference, Annotation, Set<Annotation>,
    TypeListPool.Key<out Collection<CharSequence>>, Field, PoolMethod,
    ArrayEncodedValue, EncodedValue, AnnotationElement, StringPool, TypePool, ProtoPool, FieldPool, MethodPool,
    ClassPool, CallSitePool, MethodHandlePool, TypeListPool, AnnotationPool, AnnotationSetPool, EncodedArrayPool>(
    opcodes) {
    private val sections: Array<BasePool<*, *>> = arrayOf(
        stringSection,
        typeSection,
        protoSection,
        fieldSection,
        methodSection,
        classSection,
        callSiteSection,
        methodHandleSection,

        typeListSection,
        annotationSection,
        annotationSetSection,
        encodedArraySection,
    )
    override val sectionProvider: SectionProvider get() {
        return DexPoolSectionProvider()
    }

    @Throws(IOException::class)
    override fun writeEncodedValue(writer: InternalEncodedValueWriter, encodedValue: EncodedValue) {
        when (encodedValue.valueType) {
            ValueType.ANNOTATION -> {
                val annotationEncodedValue = encodedValue as AnnotationEncodedValue
                writer.writeAnnotation(annotationEncodedValue.type, annotationEncodedValue.elements)
            }
            ValueType.ARRAY -> {
                val arrayEncodedValue = encodedValue as ArrayEncodedValue
                writer.writeArray(arrayEncodedValue.value)
            }
            ValueType.BOOLEAN -> writer.writeBoolean((encodedValue as BooleanEncodedValue).value)
            ValueType.BYTE -> writer.writeByte((encodedValue as ByteEncodedValue).value)
            ValueType.CHAR -> writer.writeChar((encodedValue as CharEncodedValue).value)
            ValueType.DOUBLE -> writer.writeDouble((encodedValue as DoubleEncodedValue).value)
            ValueType.ENUM -> writer.writeEnum((encodedValue as EnumEncodedValue).value)
            ValueType.FIELD -> writer.writeField((encodedValue as FieldEncodedValue).value)
            ValueType.FLOAT -> writer.writeFloat((encodedValue as FloatEncodedValue).value)
            ValueType.INT -> writer.writeInt((encodedValue as IntEncodedValue).value)
            ValueType.LONG -> writer.writeLong((encodedValue as LongEncodedValue).value)
            ValueType.METHOD -> writer.writeMethod((encodedValue as MethodEncodedValue).value)
            ValueType.NULL -> writer.writeNull()
            ValueType.SHORT -> writer.writeShort((encodedValue as ShortEncodedValue).value.toInt())
            ValueType.STRING -> writer.writeString((encodedValue as StringEncodedValue).value)
            ValueType.TYPE -> writer.writeType((encodedValue as TypeEncodedValue).value)
            ValueType.METHOD_TYPE -> writer.writeMethodType((encodedValue as MethodTypeEncodedValue).value)
            ValueType.METHOD_HANDLE -> writer.writeMethodHandle((encodedValue as MethodHandleEncodedValue).value)
            else -> throw ExceptionWithContext("Unrecognized value type: %d", encodedValue.valueType)
        }
    }
    internal fun internEncodedValue(encodedValue: EncodedValue) {
        when (encodedValue.valueType) {
            ValueType.ANNOTATION -> {
                val annotationEncodedValue = encodedValue as AnnotationEncodedValue
                typeSection.intern(annotationEncodedValue.type)
                for (element in annotationEncodedValue.elements) {
                    stringSection.intern(element.name)
                    internEncodedValue(element.value)
                }
            }
            ValueType.ARRAY -> for (element in (encodedValue as ArrayEncodedValue).value) {
                internEncodedValue(element)
            }
            ValueType.STRING -> stringSection.intern((encodedValue as StringEncodedValue).value)
            ValueType.TYPE -> typeSection.intern((encodedValue as TypeEncodedValue).value)
            ValueType.ENUM -> fieldSection.intern((encodedValue as EnumEncodedValue).value)
            ValueType.FIELD -> fieldSection.intern((encodedValue as FieldEncodedValue).value)
            ValueType.METHOD -> methodSection.intern((encodedValue as MethodEncodedValue).value)
            ValueType.METHOD_HANDLE ->
                methodHandleSection.intern((encodedValue as MethodHandleEncodedValue).value)
            ValueType.METHOD_TYPE -> protoSection.intern((encodedValue as MethodTypeEncodedValue).value)
        }
    }

    /**
     * Interns a class into this DexPool
     * @param classDef The class to intern
     */
    fun internClass(classDef: ClassDef) {
        classSection.intern(classDef)
    }

    /**
     * Creates a marked state that can be returned to by calling reset()
     *
     * This is useful to rollback the last added class if it causes a method/field/type overflow
     */
    fun mark() {
        for (section in sections) {
            section.mark()
        }
    }

    /**
     * Resets to the last marked state
     *
     * This is useful to rollback the last added class if it causes a method/field/type overflow
     */
    fun reset() {
        for (section in sections) {
            section.reset()
        }
    }
    protected inner class DexPoolSectionProvider : SectionProvider() {
        override val stringSection: StringPool get() {
            return StringPool(this@DexPool)
        }

        override val typeSection: TypePool get() {
            return TypePool(this@DexPool)
        }

        override val protoSection: ProtoPool get() {
            return ProtoPool(this@DexPool)
        }

        override val fieldSection: FieldPool get() {
            return FieldPool(this@DexPool)
        }

        override val methodSection: MethodPool get() {
            return MethodPool(this@DexPool)
        }

        override val classSection: ClassPool get() {
            return ClassPool(this@DexPool)
        }

        override val callSiteSection: CallSitePool get() {
            return CallSitePool(this@DexPool)
        }

        override val methodHandleSection: MethodHandlePool get() {
            return MethodHandlePool(this@DexPool)
        }

        override val typeListSection: TypeListPool get() {
            return TypeListPool(this@DexPool)
        }

        override val annotationSection: AnnotationPool get() {
            return AnnotationPool(this@DexPool)
        }

        override val annotationSetSection: AnnotationSetPool get() {
            return AnnotationSetPool(this@DexPool)
        }

        override val encodedArraySection: EncodedArrayPool get() {
            return EncodedArrayPool(this@DexPool)
        }
    }
    companion object {
        @Throws(IOException::class)
        fun writeTo(dataStore: DexDataStore, input: DexFile) {
            val dexPool = DexPool(input.opcodes)
            for (classDef in input.classes) {
                dexPool.internClass(classDef)
            }
            dexPool.writeTo(dataStore)
        }

        @Throws(IOException::class)
        fun writeTo(path: String, input: DexFile) {
            val dexPool = DexPool(input.opcodes)
            for (classDef in input.classes) {
                dexPool.internClass(classDef)
            }
            dexPool.writeTo(FileDataStore(File(path)))
        }
    }
}
