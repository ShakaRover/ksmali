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

package com.android.tools.smali.dexlib2.dexbacked

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.dexbacked.raw.ClassDefItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeIdItem
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory
import com.android.tools.smali.dexlib2.dexbacked.util.EncodedArrayItemIterator
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeListIterator
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeLookaheadIterator
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.util.ChainedIterable
import java.util.AbstractList

class DexBackedClassDef(
    @JvmField val dexFile: DexBackedDexFile,
    private val classDefOffset: Int,
    hiddenApiRestrictionsOffset: Int
) : BaseTypeReference(), ClassDef {
    private val hiddenApiRestrictionsReader: HiddenApiRestrictionsReader?

    private val staticFieldsOffset: Int
    private var instanceFieldsOffset = 0
    private var directMethodsOffset = 0
    private var virtualMethodsOffset = 0

    private val staticFieldCount: Int
    private val instanceFieldCount: Int
    private val directMethodCount: Int
    private val virtualMethodCount: Int

    private var annotationsDirectory: AnnotationsDirectory? = null

    init {
        val classDataOffset =
            dexFile.buffer.readSmallUint(classDefOffset + ClassDefItem.CLASS_DATA_OFFSET)
        if (classDataOffset == 0) {
            staticFieldsOffset = -1
            staticFieldCount = 0
            instanceFieldCount = 0
            directMethodCount = 0
            virtualMethodCount = 0
        } else {
            val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(classDataOffset)
            staticFieldCount = reader.readSmallUleb128()
            instanceFieldCount = reader.readSmallUleb128()
            directMethodCount = reader.readSmallUleb128()
            virtualMethodCount = reader.readSmallUleb128()
            staticFieldsOffset = reader.offset
        }

        if (hiddenApiRestrictionsOffset != DexWriter.NO_OFFSET) {
            hiddenApiRestrictionsReader = HiddenApiRestrictionsReader(hiddenApiRestrictionsOffset)
        } else {
            hiddenApiRestrictionsReader = null
        }
    }

    override val type: String
        get() = dexFile.typeSection.get(
            dexFile.buffer.readSmallUint(classDefOffset + ClassDefItem.CLASS_OFFSET)
        )

    override val superclass: String?
        get() = dexFile.typeSection.getOptional(
            dexFile.buffer.readOptionalUint(
                classDefOffset + ClassDefItem.SUPERCLASS_OFFSET
            )
        )

    override val accessFlags: Int
        get() = dexFile.buffer.readSmallUint(
            classDefOffset + ClassDefItem.ACCESS_FLAGS_OFFSET
        )

    override val sourceFile: String?
        get() = dexFile.stringSection.getOptional(
            dexFile.buffer.readOptionalUint(
                classDefOffset + ClassDefItem.SOURCE_FILE_OFFSET
            )
        )

    override val interfaces: List<String>
        get() {
            val interfacesOffset =
                dexFile.buffer.readSmallUint(classDefOffset + ClassDefItem.INTERFACES_OFFSET)
            if (interfacesOffset > 0) {
                val size = dexFile.dataBuffer.readSmallUint(interfacesOffset)
                return object : AbstractList<String>() {
                    override fun get(index: Int): String {
                        return dexFile.typeSection.get(
                            dexFile.dataBuffer.readUshort(interfacesOffset + 4 + (2 * index))
                        )
                    }

                    override val size: Int
                        get() = size
                }
            }
            return emptyList()
        }

    override val annotations: Set<DexBackedAnnotation>
        get() = getAnnotationsDirectory().getClassAnnotations()

    override val staticFields: Iterable<DexBackedField>
        get() = getStaticFields(true)

    fun getStaticFields(skipDuplicates: Boolean): Iterable<DexBackedField> {
        if (staticFieldCount > 0) {
            val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(staticFieldsOffset)

            val annotationsDirectory = getAnnotationsDirectory()
            val staticInitialValuesOffset = dexFile.buffer.readSmallUint(
                classDefOffset + ClassDefItem.STATIC_VALUES_OFFSET
            )
            val fieldsStartOffset = reader.offset

            val hiddenApiRestrictionIterator: MutableIterator<Int>? =
                hiddenApiRestrictionsReader?.getRestrictionsForStaticFields()

            return object : Iterable<DexBackedField> {
                override fun iterator(): MutableIterator<DexBackedField> {
                    val annotationIterator = annotationsDirectory.getFieldAnnotationIterator()
                    val staticInitialValueIterator =
                        EncodedArrayItemIterator.newOrEmpty(dexFile, staticInitialValuesOffset)

                    return object : VariableSizeLookaheadIterator<DexBackedField>(
                        dexFile.dataBuffer, fieldsStartOffset
                    ) {
                        private var count = 0
                        private var previousField: FieldReference? = null
                        private var previousIndex = 0

                        override fun readNextItem(
                            reader: DexReader<out DexBuffer>
                        ): DexBackedField? {
                            while (true) {
                                if (++count > staticFieldCount) {
                                    instanceFieldsOffset = reader.offset
                                    return endOfData()
                                }

                                var hiddenApiRestrictions = NO_HIDDEN_API_RESTRICTIONS
                                if (hiddenApiRestrictionIterator != null) {
                                    hiddenApiRestrictions = hiddenApiRestrictionIterator.next()
                                }

                                val item = DexBackedField(
                                    dexFile, reader, this@DexBackedClassDef,
                                    previousIndex, staticInitialValueIterator, annotationIterator,
                                    hiddenApiRestrictions
                                )
                                val currentField = previousField
                                val nextField = ImmutableFieldReference.of(item)

                                previousField = nextField
                                previousIndex = item.fieldIndex

                                if (skipDuplicates && currentField != null &&
                                    currentField == nextField
                                ) {
                                    continue
                                }

                                return item
                            }
                        }
                    }
                }
            }
        } else {
            instanceFieldsOffset = staticFieldsOffset
            return emptySet()
        }
    }

    override val instanceFields: Iterable<DexBackedField>
        get() = getInstanceFields(true)

    fun getInstanceFields(skipDuplicates: Boolean): Iterable<DexBackedField> {
        if (instanceFieldCount > 0) {
            val reader: DexReader<out DexBuffer> =
                dexFile.dataBuffer.readerAt(getInstanceFieldsOffset())

            val annotationsDirectory = getAnnotationsDirectory()
            val fieldsStartOffset = reader.offset

            val hiddenApiRestrictionIterator: MutableIterator<Int>? =
                hiddenApiRestrictionsReader?.getRestrictionsForInstanceFields()

            return object : Iterable<DexBackedField> {
                override fun iterator(): MutableIterator<DexBackedField> {
                    val annotationIterator = annotationsDirectory.getFieldAnnotationIterator()

                    return object : VariableSizeLookaheadIterator<DexBackedField>(
                        dexFile.dataBuffer, fieldsStartOffset
                    ) {
                        private var count = 0
                        private var previousField: FieldReference? = null
                        private var previousIndex = 0

                        override fun readNextItem(
                            reader: DexReader<out DexBuffer>
                        ): DexBackedField? {
                            while (true) {
                                if (++count > instanceFieldCount) {
                                    directMethodsOffset = reader.offset
                                    return endOfData()
                                }

                                var hiddenApiRestrictions = NO_HIDDEN_API_RESTRICTIONS
                                if (hiddenApiRestrictionIterator != null) {
                                    hiddenApiRestrictions = hiddenApiRestrictionIterator.next()
                                }

                                val item = DexBackedField(
                                    dexFile, reader, this@DexBackedClassDef,
                                    previousIndex, annotationIterator, hiddenApiRestrictions
                                )
                                val currentField = previousField
                                val nextField = ImmutableFieldReference.of(item)

                                previousField = nextField
                                previousIndex = item.fieldIndex

                                if (skipDuplicates && currentField != null &&
                                    currentField == nextField
                                ) {
                                    continue
                                }

                                return item
                            }
                        }
                    }
                }
            }
        } else {
            if (instanceFieldsOffset > 0) {
                directMethodsOffset = instanceFieldsOffset
            }
            return emptySet()
        }
    }

    override val fields: Iterable<DexBackedField>
        get() = ChainedIterable(staticFields, instanceFields)

    override val directMethods: Iterable<DexBackedMethod>
        get() = getDirectMethods(true)

    fun getDirectMethods(skipDuplicates: Boolean): Iterable<DexBackedMethod> {
        if (directMethodCount > 0) {
            val reader: DexReader<out DexBuffer> =
                dexFile.dataBuffer.readerAt(getDirectMethodsOffset())

            val annotationsDirectory = getAnnotationsDirectory()
            val methodsStartOffset = reader.offset

            val hiddenApiRestrictionIterator: MutableIterator<Int>? =
                hiddenApiRestrictionsReader?.getRestrictionsForDirectMethods()

            return object : Iterable<DexBackedMethod> {
                override fun iterator(): MutableIterator<DexBackedMethod> {
                    val methodAnnotationIterator = annotationsDirectory.getMethodAnnotationIterator()
                    val parameterAnnotationIterator =
                        annotationsDirectory.getParameterAnnotationIterator()

                    return object : VariableSizeLookaheadIterator<DexBackedMethod>(
                        dexFile.dataBuffer, methodsStartOffset
                    ) {
                        private var count = 0
                        private var previousMethod: MethodReference? = null
                        private var previousIndex = 0

                        override fun readNextItem(
                            reader: DexReader<out DexBuffer>
                        ): DexBackedMethod? {
                            while (true) {
                                if (++count > directMethodCount) {
                                    virtualMethodsOffset = reader.offset
                                    return endOfData()
                                }

                                var hiddenApiRestrictions = NO_HIDDEN_API_RESTRICTIONS
                                if (hiddenApiRestrictionIterator != null) {
                                    hiddenApiRestrictions = hiddenApiRestrictionIterator.next()
                                }

                                val item = DexBackedMethod(
                                    dexFile, reader, this@DexBackedClassDef,
                                    previousIndex, methodAnnotationIterator,
                                    parameterAnnotationIterator, hiddenApiRestrictions
                                )
                                val currentMethod = previousMethod
                                val nextMethod = ImmutableMethodReference.of(item)

                                previousMethod = nextMethod
                                previousIndex = item.methodIndex

                                if (skipDuplicates && currentMethod != null &&
                                    currentMethod == nextMethod
                                ) {
                                    continue
                                }
                                return item
                            }
                        }
                    }
                }
            }
        } else {
            if (directMethodsOffset > 0) {
                virtualMethodsOffset = directMethodsOffset
            }
            return emptySet()
        }
    }

    fun getVirtualMethods(skipDuplicates: Boolean): Iterable<DexBackedMethod> {
        if (virtualMethodCount > 0) {
            val reader: DexReader<out DexBuffer> =
                dexFile.dataBuffer.readerAt(getVirtualMethodsOffset())

            val annotationsDirectory = getAnnotationsDirectory()
            val methodsStartOffset = reader.offset

            val hiddenApiRestrictionIterator: MutableIterator<Int>? =
                hiddenApiRestrictionsReader?.getRestrictionsForVirtualMethods()

            return object : Iterable<DexBackedMethod> {
                override fun iterator(): MutableIterator<DexBackedMethod> {
                    val methodAnnotationIterator = annotationsDirectory.getMethodAnnotationIterator()
                    val parameterAnnotationIterator =
                        annotationsDirectory.getParameterAnnotationIterator()

                    return object : VariableSizeLookaheadIterator<DexBackedMethod>(
                        dexFile.dataBuffer, methodsStartOffset
                    ) {
                        private var count = 0
                        private var previousMethod: MethodReference? = null
                        private var previousIndex = 0

                        override fun readNextItem(
                            reader: DexReader<out DexBuffer>
                        ): DexBackedMethod? {
                            while (true) {
                                if (++count > virtualMethodCount) {
                                    return endOfData()
                                }

                                var hiddenApiRestrictions = NO_HIDDEN_API_RESTRICTIONS
                                if (hiddenApiRestrictionIterator != null) {
                                    hiddenApiRestrictions = hiddenApiRestrictionIterator.next()
                                }

                                val item = DexBackedMethod(
                                    dexFile, reader, this@DexBackedClassDef,
                                    previousIndex, methodAnnotationIterator,
                                    parameterAnnotationIterator, hiddenApiRestrictions
                                )
                                val currentMethod = previousMethod
                                val nextMethod = ImmutableMethodReference.of(item)

                                previousMethod = nextMethod
                                previousIndex = item.methodIndex

                                if (skipDuplicates && currentMethod != null &&
                                    currentMethod == nextMethod
                                ) {
                                    continue
                                }
                                return item
                            }
                        }
                    }
                }
            }
        } else {
            return emptySet()
        }
    }

    override val virtualMethods: Iterable<DexBackedMethod>
        get() = getVirtualMethods(true)

    override val methods: Iterable<DexBackedMethod>
        get() = ChainedIterable(directMethods, virtualMethods)

    private fun getAnnotationsDirectory(): AnnotationsDirectory {
        if (annotationsDirectory == null) {
            val annotationsDirectoryOffset =
                dexFile.buffer.readSmallUint(classDefOffset + ClassDefItem.ANNOTATIONS_OFFSET)
            annotationsDirectory =
                AnnotationsDirectory.newOrEmpty(dexFile, annotationsDirectoryOffset)
        }
        return annotationsDirectory!!
    }

    private fun getInstanceFieldsOffset(): Int {
        if (instanceFieldsOffset > 0) {
            return instanceFieldsOffset
        }
        val reader: DexReader<out DexBuffer> =
            dexFile.dataBuffer.readerAt(staticFieldsOffset)
        DexBackedField.skipFields(reader, staticFieldCount)
        instanceFieldsOffset = reader.offset
        return instanceFieldsOffset
    }

    private fun getDirectMethodsOffset(): Int {
        if (directMethodsOffset > 0) {
            return directMethodsOffset
        }
        val reader: DexReader<out DexBuffer> =
            dexFile.dataBuffer.readerAt(getInstanceFieldsOffset())
        DexBackedField.skipFields(reader, instanceFieldCount)
        directMethodsOffset = reader.offset
        return directMethodsOffset
    }

    private fun getVirtualMethodsOffset(): Int {
        if (virtualMethodsOffset > 0) {
            return virtualMethodsOffset
        }
        val reader: DexReader<out DexBuffer> =
            dexFile.dataBuffer.readerAt(getDirectMethodsOffset())
        DexBackedMethod.skipMethods(reader, directMethodCount)
        virtualMethodsOffset = reader.offset
        return virtualMethodsOffset
    }

    /**
     * Calculate and return the private size of a class definition.
     *
     * Calculated as: class_def_item size + type_id size + interfaces type_list +
     * annotations_directory_item overhead + class_data_item + static values overhead +
     * methods size + fields size
     *
     * @return size in bytes
     */
    fun getSize(): Int {
        var size = 8 * 4 //class_def_item has 8 uint fields in dex files
        size += TypeIdItem.ITEM_SIZE //type_ids size

        //add interface list size if any
        val interfacesLength = interfaces.size
        if (interfacesLength > 0) {
            //add size of the type_list
            size += 4 //uint for size
            size += interfacesLength * 2 //ushort per type_item
        }

        //annotations directory size if it exists
        val directory = getAnnotationsDirectory()
        if (AnnotationsDirectory.EMPTY != directory) {
            size += 4 * 4 //4 uints in annotations_directory_item
            val classAnnotations = directory.getClassAnnotations()
            if (classAnnotations.isNotEmpty()) {
                size += 4 //uint for size
                size += classAnnotations.size * 4 //uint per annotation_off
                //TODO: should we add annotation_item size? what if it's shared?
            }
        }

        //static values and/or metadata
        val staticInitialValuesOffset = dexFile.buffer.readSmallUint(
            classDefOffset + ClassDefItem.STATIC_VALUES_OFFSET
        )
        if (staticInitialValuesOffset != 0) {
            val reader: DexReader<out DexBuffer> =
                dexFile.dataBuffer.readerAt(staticInitialValuesOffset)
            size += reader.peekSmallUleb128Size() //encoded_array size field
        }

        //class_data_item
        val classDataOffset = dexFile.buffer.readSmallUint(
            classDefOffset + ClassDefItem.CLASS_DATA_OFFSET
        )
        if (classDataOffset > 0) {
            val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(classDataOffset)
            reader.readSmallUleb128() //staticFieldCount
            reader.readSmallUleb128() //instanceFieldCount
            reader.readSmallUleb128() //directMethodCount
            reader.readSmallUleb128() //virtualMethodCount
            size += reader.offset - classDataOffset
        }

        for (dexBackedField in fields) {
            size += dexBackedField.getSize()
        }

        for (dexBackedMethod in methods) {
            size += dexBackedMethod.getSize()
        }
        return size
    }

    private inner class HiddenApiRestrictionsReader(private val startOffset: Int) {
        private var instanceFieldsStartOffset = 0
        private var directMethodsStartOffset = 0
        private var virtualMethodsStartOffset = 0

        fun getRestrictionsForStaticFields(): VariableSizeListIterator<Int> {
            return object : VariableSizeListIterator<Int>(
                dexFile.dataBuffer, startOffset, staticFieldCount
            ) {
                override fun readNextItem(
                    reader: DexReader<out DexBuffer>,
                    index: Int
                ): Int = reader.readSmallUleb128()

                override fun next(): Int {
                    if (nextIndex() == staticFieldCount) {
                        instanceFieldsStartOffset = getReaderOffset()
                    }
                    return super.next()
                }
            }
        }

        private fun getInstanceFieldsStartOffset(): Int {
            if (instanceFieldsStartOffset == DexWriter.NO_OFFSET) {
                val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(startOffset)
                for (i in 0 until staticFieldCount) {
                    reader.readSmallUleb128()
                }
                instanceFieldsStartOffset = reader.offset
            }
            return instanceFieldsStartOffset
        }

        fun getRestrictionsForInstanceFields(): VariableSizeListIterator<Int> {
            return object : VariableSizeListIterator<Int>(
                dexFile.dataBuffer, getInstanceFieldsStartOffset(), instanceFieldCount
            ) {
                override fun readNextItem(
                    reader: DexReader<out DexBuffer>,
                    index: Int
                ): Int = reader.readSmallUleb128()

                override fun next(): Int {
                    if (nextIndex() == instanceFieldCount) {
                        directMethodsStartOffset = getReaderOffset()
                    }
                    return super.next()
                }
            }
        }

        private fun getDirectMethodsStartOffset(): Int {
            if (directMethodsStartOffset == DexWriter.NO_OFFSET) {
                val reader: DexReader<out DexBuffer> =
                    dexFile.dataBuffer.readerAt(getInstanceFieldsStartOffset())
                for (i in 0 until instanceFieldCount) {
                    reader.readSmallUleb128()
                }
                directMethodsStartOffset = reader.offset
            }
            return directMethodsStartOffset
        }

        fun getRestrictionsForDirectMethods(): VariableSizeListIterator<Int> {
            return object : VariableSizeListIterator<Int>(
                dexFile.dataBuffer, getDirectMethodsStartOffset(), directMethodCount
            ) {
                override fun readNextItem(
                    reader: DexReader<out DexBuffer>,
                    index: Int
                ): Int = reader.readSmallUleb128()

                override fun next(): Int {
                    if (nextIndex() == directMethodCount) {
                        virtualMethodsStartOffset = getReaderOffset()
                    }
                    return super.next()
                }
            }
        }

        private fun getVirtualMethodsStartOffset(): Int {
            if (virtualMethodsStartOffset == DexWriter.NO_OFFSET) {
                val reader: DexReader<out DexBuffer> =
                    dexFile.dataBuffer.readerAt(getDirectMethodsStartOffset())
                for (i in 0 until directMethodCount) {
                    reader.readSmallUleb128()
                }
                virtualMethodsStartOffset = reader.offset
            }
            return virtualMethodsStartOffset
        }

        fun getRestrictionsForVirtualMethods(): VariableSizeListIterator<Int> {
            return object : VariableSizeListIterator<Int>(
                dexFile.dataBuffer, getVirtualMethodsStartOffset(), virtualMethodCount
            ) {
                override fun readNextItem(
                    reader: DexReader<out DexBuffer>,
                    index: Int
                ): Int = reader.readSmallUleb128()
            }
        }
    }

    companion object {
        internal const val NO_HIDDEN_API_RESTRICTIONS = 7
    }
}
