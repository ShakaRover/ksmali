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

package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
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
import com.android.tools.smali.dexlib2.util.FIELD_IS_INSTANCE
import com.android.tools.smali.dexlib2.util.FIELD_IS_STATIC
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderAnnotationEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderBooleanEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderByteEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderCharEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderDoubleEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEnumEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderFieldEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderFloatEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderIntEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderLongEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderMethodEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderNullEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderShortEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderStringEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderTypeEncodedValue
import com.android.tools.smali.dexlib2.writer.util.StaticInitializerUtil
import com.android.tools.smali.util.ArraySortedSet
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.IteratorUtils
import java.io.IOException
import java.util.Collections
import java.util.HashSet
import java.util.SortedSet
import java.util.stream.Collectors

class DexBuilder(opcodes: Opcodes) : DexWriter<BuilderStringReference, BuilderStringReference, BuilderTypeReference,
    BuilderTypeReference, BuilderMethodProtoReference, BuilderFieldReference, BuilderMethodReference,
    BuilderClassDef, BuilderCallSiteReference, BuilderMethodHandleReference, BuilderAnnotation, BuilderAnnotationSet,
    BuilderTypeList, BuilderField, BuilderMethod, BuilderArrayEncodedValue, BuilderEncodedValue,
    BuilderAnnotationElement, BuilderStringPool, BuilderTypePool, BuilderProtoPool, BuilderFieldPool,
    BuilderMethodPool, BuilderClassPool, BuilderCallSitePool, BuilderMethodHandlePool, BuilderTypeListPool,
    BuilderAnnotationPool, BuilderAnnotationSetPool, BuilderEncodedArrayPool>(opcodes) {
    override val sectionProvider: SectionProvider get() {
        return DexBuilderSectionProvider()
    }

    fun internField(definingClass: String, name: String, type: String, accessFlags: Int,
                    initialValue: EncodedValue?, annotations: Set<Annotation>,
                    hiddenApiRestrictions: Set<HiddenApiRestriction>): BuilderField {
        return BuilderField(
            fieldSection.internField(definingClass, name, type),
            accessFlags,
            internNullableEncodedValue(initialValue),
            annotationSetSection.internAnnotationSet(annotations),
            hiddenApiRestrictions
        )
    }

    fun internMethod(definingClass: String, name: String, parameters: List<MethodParameter>?,
                     returnType: String, accessFlags: Int, annotations: Set<Annotation>,
                     hiddenApiRestrictions: Set<HiddenApiRestriction>,
                     methodImplementation: MethodImplementation?): BuilderMethod {
        val params = parameters ?: emptyList()
        return BuilderMethod(
            methodSection.internMethod(definingClass, name, params, returnType),
            internMethodParameters(params),
            accessFlags,
            annotationSetSection.internAnnotationSet(annotations),
            hiddenApiRestrictions,
            methodImplementation
        )
    }

    fun internClassDef(type: String, accessFlags: Int, superclass: String?, interfaces: MutableList<String>?,
                       sourceFile: String?, annotations: Set<Annotation>,
                       fields: Iterable<BuilderField>?,
                       methods: Iterable<BuilderMethod>?): BuilderClassDef {
        val interfacesList: List<String>
        if (interfaces == null) {
            interfacesList = emptyList()
        } else {
            val interfacesCopy = HashSet(interfaces)
            val interfaceIterator = interfaces.iterator()
            while (interfaceIterator.hasNext()) {
                val iface = interfaceIterator.next()
                if (!interfacesCopy.contains(iface)) {
                    interfaceIterator.remove()
                } else {
                    interfacesCopy.remove(iface)
                }
            }
            interfacesList = interfaces
        }

        var staticFields: SortedSet<BuilderField>? = null
        var instanceFields: SortedSet<BuilderField>? = null
        var internedStaticInitializers: BuilderArrayEncodedValue? = null
        if (fields != null) {
            val staticFieldsIterator: MutableIterator<BuilderField> =
                IteratorUtils.filter(fields, FIELD_IS_STATIC)
            val instanceFieldsIterator: MutableIterator<BuilderField> =
                IteratorUtils.filter(fields, FIELD_IS_INSTANCE)
            staticFields = ArraySortedSet.copyOf(
                CollectionUtils.naturalOrdering(),
                IteratorUtils.toList(staticFieldsIterator)
            )
            instanceFields = ArraySortedSet.copyOf(
                CollectionUtils.naturalOrdering(),
                IteratorUtils.toList(instanceFieldsIterator)
            )
            val staticInitializers = StaticInitializerUtil.getStaticInitializers(staticFields)
            if (staticInitializers != null) {
                internedStaticInitializers = encodedArraySection.internArrayEncodedValue(staticInitializers)
            }
        }

        return classSection.internClass(
            BuilderClassDef(
                typeSection.internType(type),
                accessFlags,
                typeSection.internNullableType(superclass),
                typeListSection.internTypeList(interfacesList),
                stringSection.internNullableString(sourceFile),
                annotationSetSection.internAnnotationSet(annotations),
                staticFields,
                instanceFields,
                methods,
                internedStaticInitializers
            )
        )
    }
    fun internCallSite(callSiteReference: CallSiteReference): BuilderCallSiteReference {
        return callSiteSection.internCallSite(callSiteReference)
    }

    fun internMethodHandle(methodHandleReference: MethodHandleReference): BuilderMethodHandleReference {
        return methodHandleSection.internMethodHandle(methodHandleReference)
    }

    fun internStringReference(string: String): BuilderStringReference {
        return stringSection.internString(string)
    }

    fun internNullableStringReference(string: String?): BuilderStringReference? {
        if (string != null) {
            return internStringReference(string)
        }
        return null
    }

    fun internTypeReference(type: String): BuilderTypeReference {
        return typeSection.internType(type)
    }

    fun internNullableTypeReference(type: String?): BuilderTypeReference? {
        if (type != null) {
            return internTypeReference(type)
        }
        return null
    }

    fun internFieldReference(field: FieldReference): BuilderFieldReference {
        return fieldSection.internField(field)
    }

    fun internMethodReference(method: MethodReference): BuilderMethodReference {
        return methodSection.internMethod(method)
    }

    fun internMethodProtoReference(methodProto: MethodProtoReference): BuilderMethodProtoReference {
        return protoSection.internMethodProto(methodProto)
    }

    fun internReference(reference: Reference): BuilderReference {
        if (reference is StringReference) {
            return internStringReference(reference.string)
        }
        if (reference is TypeReference) {
            return internTypeReference(reference.type)
        }
        if (reference is MethodReference) {
            return internMethodReference(reference)
        }
        if (reference is FieldReference) {
            return internFieldReference(reference)
        }
        if (reference is MethodProtoReference) {
            return internMethodProtoReference(reference)
        }
        if (reference is CallSiteReference) {
            return internCallSite(reference)
        }
        if (reference is MethodHandleReference) {
            return internMethodHandle(reference)
        }
        throw IllegalArgumentException("Could not determine type of reference")
    }
    private fun internMethodParameters(methodParameters: List<MethodParameter>?): List<BuilderMethodParameter> {
        if (methodParameters == null) {
            return emptyList()
        }
        return Collections.unmodifiableList(
            methodParameters.stream().map { methodParam -> internMethodParameter(methodParam) }
                .collect(Collectors.toList())
        )
    }

    private fun internMethodParameter(methodParameter: MethodParameter): BuilderMethodParameter {
        return BuilderMethodParameter(
            typeSection.internType(methodParameter.type),
            stringSection.internNullableString(methodParameter.name),
            annotationSetSection.internAnnotationSet(methodParameter.annotations)
        )
    }
    @Throws(IOException::class)
    override fun writeEncodedValue(writer: InternalEncodedValueWriter, encodedValue: BuilderEncodedValue) {
        when (encodedValue.valueType) {
            ValueType.ANNOTATION -> {
                val annotationEncodedValue = encodedValue as BuilderAnnotationEncodedValue
                writer.writeAnnotation(annotationEncodedValue.typeReference, annotationEncodedValue.elements)
            }
            ValueType.ARRAY -> {
                val arrayEncodedValue = encodedValue as BuilderArrayEncodedValue
                writer.writeArray(arrayEncodedValue.elements)
            }
            ValueType.BOOLEAN -> writer.writeBoolean((encodedValue as BooleanEncodedValue).value)
            ValueType.BYTE -> writer.writeByte((encodedValue as ByteEncodedValue).value)
            ValueType.CHAR -> writer.writeChar((encodedValue as CharEncodedValue).value)
            ValueType.DOUBLE -> writer.writeDouble((encodedValue as DoubleEncodedValue).value)
            ValueType.ENUM -> writer.writeEnum((encodedValue as BuilderEnumEncodedValue).value)
            ValueType.FIELD -> writer.writeField((encodedValue as BuilderFieldEncodedValue).fieldReference)
            ValueType.FLOAT -> writer.writeFloat((encodedValue as FloatEncodedValue).value)
            ValueType.INT -> writer.writeInt((encodedValue as IntEncodedValue).value)
            ValueType.LONG -> writer.writeLong((encodedValue as LongEncodedValue).value)
            ValueType.METHOD -> writer.writeMethod((encodedValue as BuilderMethodEncodedValue).methodReference)
            ValueType.NULL -> writer.writeNull()
            ValueType.SHORT -> writer.writeShort((encodedValue as ShortEncodedValue).value.toInt())
            ValueType.STRING -> writer.writeString((encodedValue as BuilderStringEncodedValue).stringReference)
            ValueType.TYPE -> writer.writeType((encodedValue as BuilderTypeEncodedValue).typeReference)
            ValueType.METHOD_TYPE ->
                writer.writeMethodType((encodedValue as BuilderMethodTypeEncodedValue).methodProtoReference)
            ValueType.METHOD_HANDLE ->
                writer.writeMethodHandle((encodedValue as BuilderMethodHandleEncodedValue).methodHandleReference)
            else -> throw ExceptionWithContext("Unrecognized value type: %d", encodedValue.valueType)
        }
    }
    internal fun internAnnotationElements(elements: Set<AnnotationElement>):
        Set<@JvmWildcard BuilderAnnotationElement> {
        return Collections.unmodifiableSet(
            elements.stream().map { annotationElement -> internAnnotationElement(annotationElement) }
                .collect(Collectors.toSet())
        )
    }

    private fun internAnnotationElement(annotationElement: AnnotationElement): BuilderAnnotationElement {
        return BuilderAnnotationElement(
            stringSection.internString(annotationElement.name),
            internEncodedValue(annotationElement.value)
        )
    }
    internal fun internNullableEncodedValue(encodedValue: EncodedValue?): BuilderEncodedValue? {
        if (encodedValue == null) {
            return null
        }
        return internEncodedValue(encodedValue)
    }

    internal fun internEncodedValue(encodedValue: EncodedValue): BuilderEncodedValue {
        return when (encodedValue.valueType) {
            ValueType.ANNOTATION ->
                internAnnotationEncodedValue(encodedValue as AnnotationEncodedValue)
            ValueType.ARRAY ->
                internArrayEncodedValue(encodedValue as ArrayEncodedValue)
            ValueType.BOOLEAN -> {
                val value = (encodedValue as BooleanEncodedValue).value
                if (value) BuilderBooleanEncodedValue.TRUE_VALUE else BuilderBooleanEncodedValue.FALSE_VALUE
            }
            ValueType.BYTE -> BuilderByteEncodedValue((encodedValue as ByteEncodedValue).value)
            ValueType.CHAR -> BuilderCharEncodedValue((encodedValue as CharEncodedValue).value)
            ValueType.DOUBLE -> BuilderDoubleEncodedValue((encodedValue as DoubleEncodedValue).value)
            ValueType.ENUM -> internEnumEncodedValue(encodedValue as EnumEncodedValue)
            ValueType.FIELD -> internFieldEncodedValue(encodedValue as FieldEncodedValue)
            ValueType.FLOAT -> BuilderFloatEncodedValue((encodedValue as FloatEncodedValue).value)
            ValueType.INT -> BuilderIntEncodedValue((encodedValue as IntEncodedValue).value)
            ValueType.LONG -> BuilderLongEncodedValue((encodedValue as LongEncodedValue).value)
            ValueType.METHOD -> internMethodEncodedValue(encodedValue as MethodEncodedValue)
            ValueType.NULL -> BuilderNullEncodedValue.INSTANCE
            ValueType.SHORT -> BuilderShortEncodedValue((encodedValue as ShortEncodedValue).value)
            ValueType.STRING -> internStringEncodedValue(encodedValue as StringEncodedValue)
            ValueType.TYPE -> internTypeEncodedValue(encodedValue as TypeEncodedValue)
            ValueType.METHOD_TYPE -> internMethodTypeEncodedValue(encodedValue as MethodTypeEncodedValue)
            ValueType.METHOD_HANDLE -> internMethodHandleEncodedValue(encodedValue as MethodHandleEncodedValue)
            else -> throw ExceptionWithContext("Unexpected encoded value type: %d", encodedValue.valueType)
        }
    }
    private fun internAnnotationEncodedValue(value: AnnotationEncodedValue): BuilderAnnotationEncodedValue {
        return BuilderAnnotationEncodedValue(
            typeSection.internType(value.type),
            internAnnotationElements(value.elements)
        )
    }

    private fun internArrayEncodedValue(value: ArrayEncodedValue): BuilderArrayEncodedValue {
        return BuilderArrayEncodedValue(
            Collections.unmodifiableList(
                value.value.stream().map { encodedVal -> internEncodedValue(encodedVal) }
                    .collect(Collectors.toList())
            )
        )
    }

    private fun internEnumEncodedValue(value: EnumEncodedValue): BuilderEnumEncodedValue {
        return BuilderEnumEncodedValue(fieldSection.internField(value.value))
    }

    private fun internFieldEncodedValue(value: FieldEncodedValue): BuilderFieldEncodedValue {
        return BuilderFieldEncodedValue(fieldSection.internField(value.value))
    }

    private fun internMethodEncodedValue(value: MethodEncodedValue): BuilderMethodEncodedValue {
        return BuilderMethodEncodedValue(methodSection.internMethod(value.value))
    }

    private fun internStringEncodedValue(string: StringEncodedValue): BuilderStringEncodedValue {
        return BuilderStringEncodedValue(stringSection.internString(string.value))
    }

    private fun internTypeEncodedValue(type: TypeEncodedValue): BuilderTypeEncodedValue {
        return BuilderTypeEncodedValue(typeSection.internType(type.value))
    }

    private fun internMethodTypeEncodedValue(methodType: MethodTypeEncodedValue): BuilderMethodTypeEncodedValue {
        return BuilderMethodTypeEncodedValue(protoSection.internMethodProto(methodType.value))
    }

    private fun internMethodHandleEncodedValue(methodHandle: MethodHandleEncodedValue): BuilderMethodHandleEncodedValue {
        return BuilderMethodHandleEncodedValue(methodHandleSection.internMethodHandle(methodHandle.value))
    }
    protected inner class DexBuilderSectionProvider : SectionProvider() {
        override val stringSection: BuilderStringPool get() {
            return BuilderStringPool()
        }

        override val typeSection: BuilderTypePool get() {
            return BuilderTypePool(this@DexBuilder)
        }

        override val protoSection: BuilderProtoPool get() {
            return BuilderProtoPool(this@DexBuilder)
        }

        override val fieldSection: BuilderFieldPool get() {
            return BuilderFieldPool(this@DexBuilder)
        }

        override val methodSection: BuilderMethodPool get() {
            return BuilderMethodPool(this@DexBuilder)
        }

        override val classSection: BuilderClassPool get() {
            return BuilderClassPool(this@DexBuilder)
        }

        override val callSiteSection: BuilderCallSitePool get() {
            return BuilderCallSitePool(this@DexBuilder)
        }

        override val methodHandleSection: BuilderMethodHandlePool get() {
            return BuilderMethodHandlePool(this@DexBuilder)
        }

        override val typeListSection: BuilderTypeListPool get() {
            return BuilderTypeListPool(this@DexBuilder)
        }

        override val annotationSection: BuilderAnnotationPool get() {
            return BuilderAnnotationPool(this@DexBuilder)
        }

        override val annotationSetSection: BuilderAnnotationSetPool get() {
            return BuilderAnnotationSetPool(this@DexBuilder)
        }

        override val encodedArraySection: BuilderEncodedArrayPool get() {
            return BuilderEncodedArrayPool(this@DexBuilder)
        }
    }
}
