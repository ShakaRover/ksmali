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

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import com.android.tools.smali.dexlib2.iface.debug.RestartLocal
import com.android.tools.smali.dexlib2.iface.debug.SetSourceFile
import com.android.tools.smali.dexlib2.iface.debug.StartLocal
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.util.EncodedValueUtils
import com.android.tools.smali.dexlib2.writer.ClassSection
import com.android.tools.smali.dexlib2.writer.DebugWriter
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue
import com.android.tools.smali.util.AbstractForwardSequentialList
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ExceptionWithContext
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors

class BuilderClassPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    ClassSection<BuilderStringReference, BuilderTypeReference, BuilderTypeList, BuilderClassDef, BuilderField,
        BuilderMethod, BuilderAnnotationSet, BuilderArrayEncodedValue> {
    private val internedItems: ConcurrentMap<String, BuilderClassDef> = ConcurrentHashMap()

    internal fun internClass(classDef: BuilderClassDef): BuilderClassDef {
        val prev = internedItems.put(classDef.type, classDef)
        if (prev != null) {
            throw ExceptionWithContext("Class %s has already been interned", classDef.type)
        }
        return classDef
    }

    private var sortedClassesCache: List<BuilderClassDef>? = null

    override val sortedClasses: Collection<@JvmWildcard BuilderClassDef>
        get() {
            if (sortedClassesCache == null) {
                sortedClassesCache = CollectionUtils.immutableSortedCopy(
                    internedItems.values, CollectionUtils.naturalOrdering<BuilderClassDef>()
                )
            }
            return sortedClassesCache!!
        }

    override fun getClassEntryByType(key: BuilderTypeReference?):
        MutableMap.MutableEntry<@JvmWildcard BuilderClassDef, Int>? {
        if (key == null) {
            return null
        }

        val classDef = internedItems[key.type]
        if (classDef == null) {
            return null
        }

        return object : MutableMap.MutableEntry<BuilderClassDef, Int> {
            override val key: BuilderClassDef
                get() = classDef

            override val value: Int
                get() = classDef.classDefIndex

            override fun setValue(newValue: Int): Int {
                val prev = classDef.classDefIndex
                classDef.classDefIndex = newValue
                return prev
            }
        }
    }
    override fun getType(key: BuilderClassDef): BuilderTypeReference {
        return key.typeReference
    }

    override fun getAccessFlags(key: BuilderClassDef): Int {
        return key.accessFlags
    }

    override fun getSuperclass(key: BuilderClassDef): BuilderTypeReference? {
        return key.superclassReference
    }

    override fun getInterfaces(key: BuilderClassDef): BuilderTypeList {
        return key.interfacesList
    }

    override fun getSourceFile(key: BuilderClassDef): BuilderStringReference? {
        return key.sourceFileReference
    }

    override fun getStaticInitializers(key: BuilderClassDef): BuilderArrayEncodedValue? {
        return key.staticInitializers
    }

    override fun getSortedStaticFields(key: BuilderClassDef): Collection<@JvmWildcard BuilderField> {
        return key.staticFields
    }

    override fun getSortedInstanceFields(key: BuilderClassDef): Collection<@JvmWildcard BuilderField> {
        return key.instanceFields
    }

    override fun getSortedFields(key: BuilderClassDef): Collection<@JvmWildcard BuilderField> {
        return key.fields
    }

    override fun getSortedDirectMethods(key: BuilderClassDef): Collection<@JvmWildcard BuilderMethod> {
        return key.directMethods
    }

    override fun getSortedVirtualMethods(key: BuilderClassDef): Collection<@JvmWildcard BuilderMethod> {
        return key.virtualMethods
    }

    override fun getSortedMethods(key: BuilderClassDef): Collection<@JvmWildcard BuilderMethod> {
        return key.methods
    }

    override fun getFieldAccessFlags(key: BuilderField): Int {
        return key.accessFlags
    }

    override fun getMethodAccessFlags(key: BuilderMethod): Int {
        return key.accessFlags
    }

    override fun getFieldHiddenApiRestrictions(key: BuilderField): Set<HiddenApiRestriction> {
        return key.hiddenApiRestrictions
    }

    override fun getMethodHiddenApiRestrictions(key: BuilderMethod): Set<HiddenApiRestriction> {
        return key.hiddenApiRestrictions
    }
    override fun getClassAnnotations(key: BuilderClassDef): BuilderAnnotationSet? {
        if (key.annotations.isEmpty()) {
            return null
        }
        return key.annotations
    }

    override fun getFieldAnnotations(key: BuilderField): BuilderAnnotationSet? {
        if (key.annotations.isEmpty()) {
            return null
        }
        return key.annotations
    }

    override fun getMethodAnnotations(key: BuilderMethod): BuilderAnnotationSet? {
        if (key.annotations.isEmpty()) {
            return null
        }
        return key.annotations
    }

    override fun getParameterAnnotations(key: BuilderMethod): List<@JvmWildcard BuilderAnnotationSet>? {
        val parameters = key.parameters
        val hasParameterAnnotations = parameters.stream().anyMatch(HAS_PARAMETER_ANNOTATIONS)

        if (hasParameterAnnotations) {
            return object : AbstractForwardSequentialList<BuilderAnnotationSet>() {
                override fun iterator(): MutableIterator<BuilderAnnotationSet> {
                    return parameters.stream().map(PARAMETER_ANNOTATIONS).iterator()
                }

                override val size: Int
                    get() = parameters.size
            }
        }
        return null
    }

    override fun getDebugItems(key: BuilderMethod): Iterable<@JvmWildcard DebugItem>? {
        val impl = key.implementation
        if (impl == null) {
            return null
        }
        return impl.debugItems
    }

    override fun getParameterNames(key: BuilderMethod): Iterable<@JvmWildcard BuilderStringReference?>? {
        return key.parameters.stream()
            .map { builderMethodParameter -> builderMethodParameter.nameReference }
            .collect(Collectors.toList())
    }

    override fun getRegisterCount(key: BuilderMethod): Int {
        val impl = key.implementation
        if (impl == null) {
            return 0
        }
        return impl.registerCount
    }

    override fun getInstructions(key: BuilderMethod): Iterable<@JvmWildcard Instruction>? {
        val impl = key.implementation
        if (impl == null) {
            return null
        }
        return impl.instructions
    }

    override fun getTryBlocks(key: BuilderMethod): List<@JvmWildcard TryBlock<out ExceptionHandler>> {
        val impl = key.implementation
        if (impl == null) {
            return emptyList()
        }
        return impl.tryBlocks
    }

    override fun getExceptionType(handler: ExceptionHandler): BuilderTypeReference? {
        return checkTypeReference(handler.exceptionTypeReference)
    }

    override fun makeMutableMethodImplementation(key: BuilderMethod): MutableMethodImplementation {
        val impl = key.implementation
        if (impl is MutableMethodImplementation) {
            return impl
        }
        return MutableMethodImplementation(impl!!)
    }
    override fun setAnnotationDirectoryOffset(key: BuilderClassDef, offset: Int) {
        key.annotationDirectoryOffset = offset
    }

    override fun getAnnotationDirectoryOffset(key: BuilderClassDef): Int {
        return key.annotationDirectoryOffset
    }

    override fun setAnnotationSetRefListOffset(key: BuilderMethod, offset: Int) {
        key.annotationSetRefListOffset = offset
    }

    override fun getAnnotationSetRefListOffset(key: BuilderMethod): Int {
        return key.annotationSetRefListOffset
    }

    override fun setCodeItemOffset(key: BuilderMethod, offset: Int) {
        key.codeItemOffset = offset
    }

    override fun getCodeItemOffset(key: BuilderMethod): Int {
        return key.codeItemOffset
    }

    private fun checkStringReference(stringReference: StringReference?): BuilderStringReference? {
        if (stringReference == null) {
            return null
        }
        try {
            return stringReference as BuilderStringReference
        } catch (ex: ClassCastException) {
            throw IllegalStateException("Only StringReference instances returned by " +
                "DexBuilder.internStringReference or DexBuilder.internNullableStringReference may be used.")
        }
    }

    private fun checkTypeReference(typeReference: TypeReference?): BuilderTypeReference? {
        if (typeReference == null) {
            return null
        }
        try {
            return typeReference as BuilderTypeReference
        } catch (ex: ClassCastException) {
            throw IllegalStateException("Only TypeReference instances returned by " +
                "DexBuilder.internTypeReference or DexBuilder.internNullableTypeReference may be used.")
        }
    }
    @Throws(IOException::class)
    override fun writeDebugItem(writer: DebugWriter<BuilderStringReference, BuilderTypeReference>,
                                debugItem: DebugItem) {
        when (debugItem.debugItemType) {
            DebugItemType.START_LOCAL -> {
                val startLocal = debugItem as StartLocal
                writer.writeStartLocal(startLocal.codeAddress, startLocal.register,
                    checkStringReference(startLocal.nameReference),
                    checkTypeReference(startLocal.typeReference),
                    checkStringReference(startLocal.signatureReference))
            }
            DebugItemType.END_LOCAL -> {
                val endLocal = debugItem as EndLocal
                writer.writeEndLocal(endLocal.codeAddress, endLocal.register)
            }
            DebugItemType.RESTART_LOCAL -> {
                val restartLocal = debugItem as RestartLocal
                writer.writeRestartLocal(restartLocal.codeAddress, restartLocal.register)
            }
            DebugItemType.PROLOGUE_END -> {
                writer.writePrologueEnd(debugItem.codeAddress)
            }
            DebugItemType.EPILOGUE_BEGIN -> {
                writer.writeEpilogueBegin(debugItem.codeAddress)
            }
            DebugItemType.LINE_NUMBER -> {
                val lineNumber = debugItem as LineNumber
                writer.writeLineNumber(lineNumber.codeAddress, lineNumber.lineNumber)
            }
            DebugItemType.SET_SOURCE_FILE -> {
                val setSourceFile = debugItem as SetSourceFile
                writer.writeSetSourceFile(setSourceFile.codeAddress,
                    checkStringReference(setSourceFile.sourceFileReference))
            }
            else -> throw ExceptionWithContext("Unexpected debug item type: %d", debugItem.debugItemType)
        }
    }
    override fun getItemIndex(key: BuilderClassDef): Int {
        return key.classDefIndex
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderClassDef, Int>>
        get() = object : BuilderMapEntryCollection<BuilderClassDef>(internedItems.values) {
            override fun getValue(key: BuilderClassDef): Int {
                return key.classDefIndex
            }

            override fun setValue(key: BuilderClassDef, value: Int): Int {
                val prev = key.classDefIndex
                key.classDefIndex = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
    companion object {
        private val HAS_INITIALIZER = Predicate<Field> { input ->
            val encodedValue = input.initialValue
            encodedValue != null && !EncodedValueUtils.isDefaultValue(encodedValue)
        }

        private val GET_INITIAL_VALUE = Function<BuilderField, BuilderEncodedValue> { input ->
            val initialValue = input.initialValue
            if (initialValue == null) {
                BuilderEncodedValues.defaultValueForType(input.type)
            } else {
                initialValue
            }
        }

        private val HAS_PARAMETER_ANNOTATIONS = Predicate<BuilderMethodParameter> { input ->
            input.annotations.size > 0
        }

        private val PARAMETER_ANNOTATIONS = Function<BuilderMethodParameter, BuilderAnnotationSet> { input ->
            input.annotations
        }
    }
}
