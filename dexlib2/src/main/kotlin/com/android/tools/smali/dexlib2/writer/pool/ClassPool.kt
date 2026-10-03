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

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import com.android.tools.smali.dexlib2.iface.debug.RestartLocal
import com.android.tools.smali.dexlib2.iface.debug.SetSourceFile
import com.android.tools.smali.dexlib2.iface.debug.StartLocal
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.writer.ClassSection
import com.android.tools.smali.dexlib2.writer.DebugWriter
import com.android.tools.smali.dexlib2.writer.util.StaticInitializerUtil
import com.android.tools.smali.util.AbstractForwardSequentialList
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.TransformedIterable
import com.android.tools.smali.util.TransformedIterable.TransformedIterator
import java.io.IOException
import java.util.AbstractCollection
import java.util.HashSet
import java.util.function.Function
import java.util.function.Predicate

class ClassPool(dexPool: DexPool) : BasePool<String, PoolClassDef>(dexPool),
    ClassSection<CharSequence, CharSequence, TypeListPool.Key<out Collection<out CharSequence>>, PoolClassDef,
        Field, PoolMethod, Set<out Annotation>, ArrayEncodedValue> {

    fun intern(classDef: ClassDef) {
        val poolClassDef = PoolClassDef(classDef)

        val prev = internedItems.put(poolClassDef.type, poolClassDef)
        if (prev != null) {
            throw ExceptionWithContext("Class %s has already been interned", poolClassDef.type)
        }

        dexPool.typeSection.intern(poolClassDef.type)
        dexPool.typeSection.internNullable(poolClassDef.superclass)
        dexPool.typeListSection.intern(poolClassDef.interfaces)
        dexPool.stringSection.internNullable(poolClassDef.sourceFile)

        val fields = HashSet<String>()
        for (field in poolClassDef.fields) {
            val fieldDescriptor = DexFormatter.INSTANCE.getShortFieldDescriptor(field)
            if (!fields.add(fieldDescriptor)) {
                throw ExceptionWithContext("Multiple definitions for field %s->%s",
                    poolClassDef.type, fieldDescriptor)
            }
            dexPool.fieldSection.intern(field)

            val initialValue = field.initialValue
            if (initialValue != null) {
                dexPool.internEncodedValue(initialValue)
            }

            dexPool.annotationSetSection.intern(field.annotations)

            // NOTE: preserved from the original Java implementation (this interning is performed inside the loop)
            val staticInitializers = getStaticInitializers(poolClassDef)
            if (staticInitializers != null) {
                dexPool.encodedArraySection.intern(staticInitializers)
            }
        }

        val methods = HashSet<String>()
        for (method in poolClassDef.methods) {
            val methodDescriptor = DexFormatter.INSTANCE.getShortMethodDescriptor(method)
            if (!methods.add(methodDescriptor)) {
                throw ExceptionWithContext("Multiple definitions for method %s->%s",
                    poolClassDef.type, methodDescriptor)
            }
            dexPool.methodSection.intern(method)
            internCode(method)
            internDebug(method)
            dexPool.annotationSetSection.intern(method.annotations)

            for (parameter in method.parameters) {
                dexPool.annotationSetSection.intern(parameter.annotations)
            }
        }

        dexPool.annotationSetSection.intern(poolClassDef.annotations)
    }

    private fun internCode(method: Method) {
        // this also handles parameter names, which aren't directly tied to the MethodImplementation, even though the debug items are
        var hasInstruction = false

        val methodImpl = method.implementation
        if (methodImpl != null) {
            for (instruction in methodImpl.instructions) {
                hasInstruction = true
                if (instruction is ReferenceInstruction) {
                    internReference(instruction.reference, instruction.referenceType)
                }
                if (instruction is DualReferenceInstruction) {
                    internReference(instruction.reference2, instruction.referenceType2)
                }
            }

            val tryBlocks = methodImpl.tryBlocks
            if (!hasInstruction && tryBlocks.size > 0) {
                throw ExceptionWithContext("Method %s has no instructions, but has try blocks.", method)
            }

            for (tryBlock in methodImpl.tryBlocks) {
                for (handler in tryBlock.exceptionHandlers) {
                    dexPool.typeSection.internNullable(handler.exceptionType)
                }
            }
        }
    }

    private fun internReference(reference: Reference, referenceType: Int) {
        when (referenceType) {
            ReferenceType.STRING ->
                dexPool.stringSection.intern(reference as StringReference)
            ReferenceType.TYPE ->
                dexPool.typeSection.intern((reference as TypeReference).type)
            ReferenceType.FIELD ->
                dexPool.fieldSection.intern(reference as FieldReference)
            ReferenceType.METHOD ->
                dexPool.methodSection.intern(reference as MethodReference)
            ReferenceType.METHOD_PROTO ->
                dexPool.protoSection.intern(reference as MethodProtoReference)
            ReferenceType.METHOD_HANDLE ->
                dexPool.methodHandleSection.intern(reference as MethodHandleReference)
            ReferenceType.CALL_SITE ->
                dexPool.callSiteSection.intern(reference as CallSiteReference)
            else -> throw ExceptionWithContext("Unrecognized reference type: %d", referenceType)
        }
    }

    private fun internDebug(method: Method) {
        for (param in method.parameters) {
            val paramName = param.name
            if (paramName != null) {
                dexPool.stringSection.intern(paramName)
            }
        }

        val methodImpl = method.implementation
        if (methodImpl != null) {
            for (debugItem in methodImpl.debugItems) {
                when (debugItem.debugItemType) {
                    DebugItemType.START_LOCAL -> {
                        val startLocal = debugItem as StartLocal
                        dexPool.stringSection.internNullable(startLocal.name)
                        dexPool.typeSection.internNullable(startLocal.type)
                        dexPool.stringSection.internNullable(startLocal.signature)
                    }
                    DebugItemType.SET_SOURCE_FILE ->
                        dexPool.stringSection.internNullable((debugItem as SetSourceFile).sourceFile)
                }
            }
        }
    }

    private var sortedClassesCache: List<PoolClassDef>? = null

    override val sortedClasses: Collection<@JvmWildcard PoolClassDef>
        get() {
            if (sortedClassesCache == null) {
                sortedClassesCache = CollectionUtils.immutableSortedCopy(
                    internedItems.values, CollectionUtils.usingToStringOrdering<PoolClassDef>()
                )
            }
            return sortedClassesCache!!
        }

    override fun getClassEntryByType(key: CharSequence?): MutableMap.MutableEntry<@JvmWildcard PoolClassDef, Int>? {
        if (key == null) {
            return null
        }

        val classDef = internedItems[key.toString()]
        if (classDef == null) {
            return null
        }

        return object : MutableMap.MutableEntry<PoolClassDef, Int> {
            override val key: PoolClassDef
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

    override fun getType(key: PoolClassDef): CharSequence {
        return key.type
    }

    override fun getAccessFlags(key: PoolClassDef): Int {
        return key.accessFlags
    }

    override fun getSuperclass(key: PoolClassDef): CharSequence? {
        return key.superclass
    }

    override fun getInterfaces(key: PoolClassDef): TypeListPool.Key<List<String>> {
        return key.interfacesKey
    }

    override fun getSourceFile(key: PoolClassDef): CharSequence? {
        return key.sourceFile
    }

    override fun getStaticInitializers(key: PoolClassDef): ArrayEncodedValue? {
        return StaticInitializerUtil.getStaticInitializers(key.staticFields)
    }

    override fun getSortedStaticFields(key: PoolClassDef): Collection<@JvmWildcard Field> {
        return key.staticFields
    }

    override fun getSortedInstanceFields(key: PoolClassDef): Collection<@JvmWildcard Field> {
        return key.instanceFields
    }

    override fun getSortedFields(key: PoolClassDef): Collection<@JvmWildcard Field> {
        return key.fields
    }

    override fun getSortedDirectMethods(key: PoolClassDef): Collection<PoolMethod> {
        return key.directMethods
    }

    override fun getSortedVirtualMethods(key: PoolClassDef): Collection<PoolMethod> {
        return key.virtualMethods
    }

    override fun getSortedMethods(key: PoolClassDef): Collection<@JvmWildcard PoolMethod> {
        return key.methods
    }

    override fun getFieldAccessFlags(key: Field): Int {
        return key.accessFlags
    }

    override fun getMethodAccessFlags(key: PoolMethod): Int {
        return key.accessFlags
    }

    override fun getFieldHiddenApiRestrictions(key: Field): Set<HiddenApiRestriction> {
        return key.hiddenApiRestrictions
    }

    override fun getMethodHiddenApiRestrictions(key: PoolMethod): Set<HiddenApiRestriction> {
        return key.hiddenApiRestrictions
    }

    override fun getClassAnnotations(key: PoolClassDef): Set<out Annotation>? {
        val annotations = key.annotations
        if (annotations.size == 0) {
            return null
        }
        return annotations
    }

    override fun getFieldAnnotations(key: Field): Set<out Annotation>? {
        val annotations = key.annotations
        if (annotations.size == 0) {
            return null
        }
        return annotations
    }

    override fun getMethodAnnotations(key: PoolMethod): Set<out Annotation>? {
        val annotations = key.annotations
        if (annotations.size == 0) {
            return null
        }
        return annotations
    }

    override fun getParameterAnnotations(key: PoolMethod): List<@JvmWildcard Set<out Annotation>>? {
        val parameters = key.parameters
        val hasParameterAnnotations = parameters.stream().anyMatch(HAS_PARAMETER_ANNOTATIONS)

        if (hasParameterAnnotations) {
            return object : AbstractForwardSequentialList<Set<out Annotation>>() {
                override fun iterator(): MutableIterator<Set<out Annotation>> {
                    return TransformedIterator(parameters.iterator(), PARAMETER_ANNOTATIONS)
                }

                override val size: Int
                    get() = parameters.size
            }
        }
        return null
    }

    override fun getDebugItems(key: PoolMethod): Iterable<@JvmWildcard DebugItem>? {
        val impl = key.implementation
        if (impl != null) {
            return impl.debugItems
        }
        return null
    }

    override fun getParameterNames(key: PoolMethod): Iterable<@JvmWildcard CharSequence>? {
        return TransformedIterable(key.parameters, Function<MethodParameter, CharSequence> { input -> input.name })
    }

    override fun getRegisterCount(key: PoolMethod): Int {
        val impl = key.implementation
        if (impl != null) {
            return impl.registerCount
        }
        return 0
    }

    override fun getInstructions(key: PoolMethod): Iterable<@JvmWildcard Instruction>? {
        val impl = key.implementation
        if (impl != null) {
            return impl.instructions
        }
        return null
    }

    override fun getTryBlocks(key: PoolMethod): List<@JvmWildcard TryBlock<out ExceptionHandler>> {
        val impl = key.implementation
        if (impl != null) {
            return impl.tryBlocks
        }
        return emptyList()
    }

    override fun getExceptionType(handler: ExceptionHandler): CharSequence? {
        return handler.exceptionType
    }

    override fun makeMutableMethodImplementation(key: PoolMethod): MutableMethodImplementation {
        return MutableMethodImplementation(key.implementation!!)
    }

    override fun setAnnotationDirectoryOffset(key: PoolClassDef, offset: Int) {
        key.annotationDirectoryOffset = offset
    }

    override fun getAnnotationDirectoryOffset(key: PoolClassDef): Int {
        return key.annotationDirectoryOffset
    }

    override fun setAnnotationSetRefListOffset(key: PoolMethod, offset: Int) {
        key.annotationSetRefListOffset = offset
    }

    override fun getAnnotationSetRefListOffset(key: PoolMethod): Int {
        return key.annotationSetRefListOffset
    }

    override fun setCodeItemOffset(key: PoolMethod, offset: Int) {
        key.codeItemOffset = offset
    }

    override fun getCodeItemOffset(key: PoolMethod): Int {
        return key.codeItemOffset
    }

    @Throws(IOException::class)
    override fun writeDebugItem(writer: DebugWriter<CharSequence, CharSequence>, debugItem: DebugItem) {
        when (debugItem.debugItemType) {
            DebugItemType.START_LOCAL -> {
                val startLocal = debugItem as StartLocal
                writer.writeStartLocal(startLocal.codeAddress, startLocal.register, startLocal.name,
                    startLocal.type, startLocal.signature)
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
                writer.writeSetSourceFile(setSourceFile.codeAddress, setSourceFile.sourceFile)
                // NOTE: this reproduces the original Java switch fall-through into the default case
                throw ExceptionWithContext("Unexpected debug item type: %d", debugItem.debugItemType)
            }
            else -> throw ExceptionWithContext("Unexpected debug item type: %d", debugItem.debugItemType)
        }
    }

    override fun getItemIndex(key: PoolClassDef): Int {
        return key.classDefIndex
    }

    override val items: Collection<@JvmWildcard MutableMap.MutableEntry<@JvmWildcard PoolClassDef, Int>>
        get() {
            class MapEntry(override val key: PoolClassDef) : MutableMap.MutableEntry<PoolClassDef, Int> {
                override val value: Int
                    get() = key.classDefIndex

                override fun setValue(newValue: Int): Int {
                    val prev = key.classDefIndex
                    key.classDefIndex = newValue
                    return prev
                }
            }

            return object : AbstractCollection<MutableMap.MutableEntry<PoolClassDef, Int>>() {
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<PoolClassDef, Int>> {
                    return object : MutableIterator<MutableMap.MutableEntry<PoolClassDef, Int>> {
                        private val iter = internedItems.values.iterator()

                        override fun hasNext(): Boolean {
                            return iter.hasNext()
                        }

                        override fun next(): MutableMap.MutableEntry<PoolClassDef, Int> {
                            return MapEntry(iter.next())
                        }

                        override fun remove() {
                            throw UnsupportedOperationException()
                        }
                    }
                }

                override val size: Int
                    get() = internedItems.size
            }
        }

    companion object {
        private val HAS_PARAMETER_ANNOTATIONS = Predicate<MethodParameter> { input ->
            input.annotations.size > 0
        }

        private val PARAMETER_ANNOTATIONS = Function<MethodParameter, Set<out Annotation>> { input ->
            input.annotations
        }
    }
}
