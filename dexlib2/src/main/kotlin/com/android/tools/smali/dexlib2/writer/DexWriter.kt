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

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.base.BaseAnnotation
import com.android.tools.smali.dexlib2.base.BaseAnnotationElement
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.dexbacked.raw.CallSiteIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ClassDefItem
import com.android.tools.smali.dexlib2.dexbacked.raw.FieldIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HiddenApiClassDataItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ItemType
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodHandleItem
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.StringIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeIdItem
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VariableRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.getParameterRegisterCount
import com.android.tools.smali.dexlib2.util.isInvokePolymorphic
import com.android.tools.smali.dexlib2.util.isInvokeStatic
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction12x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20bc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21ih
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21lh
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22b
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22cs
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction23x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction30t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction32x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35mi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35ms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rmi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction45cc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction4rcc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction51l
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import com.android.tools.smali.dexlib2.writer.io.DeferredOutputStream
import com.android.tools.smali.dexlib2.writer.io.DeferredOutputStreamFactory
import com.android.tools.smali.dexlib2.writer.io.DexDataStore
import com.android.tools.smali.dexlib2.writer.io.MemoryDeferredOutputStream
import com.android.tools.smali.dexlib2.writer.util.TryListBuilder
import com.android.tools.smali.util.ChainedIterable
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.IteratorUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.zip.Adler32

abstract class DexWriter<
    StringKey : CharSequence, StringRef : StringReference, TypeKey : CharSequence,
    TypeRef : TypeReference, ProtoRefKey : MethodProtoReference,
    FieldRefKey : FieldReference, MethodRefKey : MethodReference,
    ClassKey : Comparable<ClassKey>,
    CallSiteKey : CallSiteReference,
    MethodHandleKey : MethodHandleReference,
    AnnotationKey : Annotation, AnnotationSetKey,
    TypeListKey, FieldKey, MethodKey : Any,
    EncodedArrayKey, EncodedValue,
    AnnotationElement : com.android.tools.smali.dexlib2.iface.AnnotationElement,
    StringSectionType : StringSection<StringKey, StringRef>,
    TypeSectionType : TypeSection<StringKey, TypeKey, TypeRef>,
    ProtoSectionType : ProtoSection<StringKey, TypeKey, ProtoRefKey, TypeListKey>,
    FieldSectionType : FieldSection<StringKey, TypeKey, FieldRefKey, FieldKey>,
    MethodSectionType : MethodSection<StringKey, TypeKey, ProtoRefKey, MethodRefKey, MethodKey>,
    ClassSectionType : ClassSection<StringKey, TypeKey, TypeListKey, ClassKey, FieldKey, MethodKey,
        AnnotationSetKey, EncodedArrayKey>,
    CallSiteSectionType : CallSiteSection<CallSiteKey, EncodedArrayKey>,
    MethodHandleSectionType : MethodHandleSection<MethodHandleKey, FieldRefKey, MethodRefKey>,
    TypeListSectionType : TypeListSection<TypeKey, TypeListKey>,
    AnnotationSectionType : AnnotationSection<StringKey, TypeKey, AnnotationKey, AnnotationElement, EncodedValue>,
    AnnotationSetSectionType : AnnotationSetSection<AnnotationKey, AnnotationSetKey>,
    EncodedArraySectionType : EncodedArraySection<EncodedArrayKey, EncodedValue>> {
    protected val opcodes: Opcodes

    protected var stringIndexSectionOffset = NO_OFFSET
    protected var typeSectionOffset = NO_OFFSET
    protected var protoSectionOffset = NO_OFFSET
    protected var fieldSectionOffset = NO_OFFSET
    protected var methodSectionOffset = NO_OFFSET
    protected var classIndexSectionOffset = NO_OFFSET
    protected var callSiteSectionOffset = NO_OFFSET
    protected var methodHandleSectionOffset = NO_OFFSET

    protected var stringDataSectionOffset = NO_OFFSET
    protected var classDataSectionOffset = NO_OFFSET
    protected var typeListSectionOffset = NO_OFFSET
    protected var encodedArraySectionOffset = NO_OFFSET
    protected var annotationSectionOffset = NO_OFFSET
    protected var annotationSetSectionOffset = NO_OFFSET
    protected var annotationSetRefSectionOffset = NO_OFFSET
    protected var annotationDirectorySectionOffset = NO_OFFSET
    protected var debugSectionOffset = NO_OFFSET
    protected var codeSectionOffset = NO_OFFSET
    protected var hiddenApiRestrictionsOffset = NO_OFFSET
    protected var mapSectionOffset = NO_OFFSET

    protected var hasHiddenApiRestrictions = false

    protected var numAnnotationSetRefItems = 0
    protected var numAnnotationDirectoryItems = 0
    protected var numDebugInfoItems = 0
    protected var numCodeItemItems = 0
    protected var numClassDataItems = 0

    // The sections defined here must be kept in sync with these section arrays:
    // - DexWriter.overflowableSections
    // - DexPool.sections

    val stringSection: StringSectionType
    val typeSection: TypeSectionType
    val protoSection: ProtoSectionType
    val fieldSection: FieldSectionType
    val methodSection: MethodSectionType
    val classSection: ClassSectionType
    val callSiteSection: CallSiteSectionType
    val methodHandleSection: MethodHandleSectionType

    val typeListSection: TypeListSectionType
    val annotationSection: AnnotationSectionType
    val annotationSetSection: AnnotationSetSectionType
    val encodedArraySection: EncodedArraySectionType

    private val overflowableSections: Array<IndexSection<*>>

    private val debugInfoCaches = HashMap<DebugInfoCache, Int>()
    protected constructor(opcodes: Opcodes) {
        this.opcodes = opcodes

        val provider = sectionProvider
        this.stringSection = provider.stringSection
        this.typeSection = provider.typeSection
        this.protoSection = provider.protoSection
        this.fieldSection = provider.fieldSection
        this.methodSection = provider.methodSection
        this.classSection = provider.classSection
        this.callSiteSection = provider.callSiteSection
        this.methodHandleSection = provider.methodHandleSection
        this.typeListSection = provider.typeListSection
        this.annotationSection = provider.annotationSection
        this.annotationSetSection = provider.annotationSetSection
        this.encodedArraySection = provider.encodedArraySection

        overflowableSections = arrayOf(
            //stringSection,            // supports jumbo indexes
            typeSection,
            protoSection,
            fieldSection,
            methodSection,
            //classSection,             // redundant check: cannot be larger than typeSection
            callSiteSection,
            methodHandleSection,
        )
    }

    protected abstract val sectionProvider: SectionProvider

    @Throws(IOException::class)
    protected abstract fun writeEncodedValue(writer: InternalEncodedValueWriter, encodedValue: EncodedValue)
    private val callSiteComparator = Comparator<MutableMap.MutableEntry<out CallSiteKey, Int>> { o1, o2 ->
        val offset1 = encodedArraySection.getItemOffset(callSiteSection.getEncodedCallSite(o1.key))
        val offset2 = encodedArraySection.getItemOffset(callSiteSection.getEncodedCallSite(o2.key))
        val res = offset1.compareTo(offset2)
        if (res != 0) res else o1.key.name.compareTo(o2.key.name)
    }

    protected inner class InternalEncodedValueWriter internal constructor(writer: DexDataWriter) :
        EncodedValueWriter<StringKey, TypeKey, FieldRefKey, MethodRefKey, AnnotationElement, ProtoRefKey,
            MethodHandleKey, EncodedValue>(
            writer, stringSection, typeSection, fieldSection, methodSection, protoSection, methodHandleSection,
            annotationSection
        ) {
        @Throws(IOException::class)
        override fun writeEncodedValue(encodedValue: EncodedValue) {
            this@DexWriter.writeEncodedValue(this, encodedValue)
        }
    }
    private val dataSectionOffset: Int get() {
        return HeaderItem.ITEM_SIZE +
            stringSection.itemCount * StringIdItem.ITEM_SIZE +
            typeSection.itemCount * TypeIdItem.ITEM_SIZE +
            protoSection.itemCount * ProtoIdItem.ITEM_SIZE +
            fieldSection.itemCount * FieldIdItem.ITEM_SIZE +
            methodSection.itemCount * MethodIdItem.ITEM_SIZE +
            classSection.itemCount * ClassDefItem.ITEM_SIZE +
            callSiteSection.itemCount * CallSiteIdItem.ITEM_SIZE +
            methodHandleSection.itemCount * MethodHandleItem.ITEM_SIZE
    }

    val methodReferences: List<String> get() {
        val methodReferences = ArrayList<String>()
        for (methodReference in methodSection.items) {
            methodReferences.add(DexFormatter.INSTANCE.getMethodDescriptor(methodReference.key))
        }
        return methodReferences
    }

    val fieldReferences: List<String> get() {
        val fieldReferences = ArrayList<String>()
        for (fieldReference in fieldSection.items) {
            fieldReferences.add(DexFormatter.INSTANCE.getFieldDescriptor(fieldReference.key))
        }
        return fieldReferences
    }

    val typeReferences: List<String> get() {
        val classReferences = ArrayList<String>()
        for (typeReference in typeSection.items) {
            classReferences.add(typeReference.key.toString())
        }
        return classReferences
    }

    /**
     * Checks whether any of the size-sensitive constant pools have overflowed and have more than 64Ki entries.
     *
     * Note that even if this returns true, it may still be possible to successfully write the dex file, if the
     * overflowed items are not referenced anywhere that uses a 16-bit index.
     *
     * @return true if any of the size-sensitive constant pools have overflowed
     */
    fun hasOverflowed(): Boolean {
        return hasOverflowed(MAX_POOL_SIZE)
    }

    /**
     * Checks whether any of the size-sensitive constant pools have more than the supplied maximum number of entries.
     *
     * @param maxPoolSize the maximum number of entries allowed in any of the size-sensitive constant pools
     * @return true if any of the size-sensitive constant pools have overflowed the supplied size limit
     */
    fun hasOverflowed(maxPoolSize: Int): Boolean {
        for (section in overflowableSections) {
            if (section.itemCount > maxPoolSize) return true
        }
        return false
    }

    @Throws(IOException::class)
    fun writeTo(dest: DexDataStore, tempFactory: DeferredOutputStreamFactory = MemoryDeferredOutputStream.factory) {
        try {
            outputAt(dest, 0).use { headerWriter ->
                outputAt(dest, HeaderItem.ITEM_SIZE).use { indexWriter ->
                    outputAt(dest, dataSectionOffset).use { offsetWriter ->
                        writeStrings(indexWriter, offsetWriter)
                        writeTypes(indexWriter)
                        writeTypeLists(offsetWriter)
                        writeProtos(indexWriter)
                        writeFields(indexWriter)
                        writeMethods(indexWriter)

                        // encoded arrays depend on method handles..
                        outputAt(dest, indexWriter.position +
                                classSection.itemCount * ClassDefItem.ITEM_SIZE +
                                callSiteSection.itemCount * CallSiteIdItem.ITEM_SIZE).use { methodHandleWriter ->
                            writeMethodHandles(methodHandleWriter)
                        }

                        // call sites depend on encoded arrays..
                        writeEncodedArrays(offsetWriter)

                        // class defs depend on method handles and call sites..
                        outputAt(dest, indexWriter.position +
                                classSection.itemCount * ClassDefItem.ITEM_SIZE).use { callSiteWriter ->
                            writeCallSites(callSiteWriter)
                        }

                        writeAnnotations(offsetWriter)
                        writeAnnotationSets(offsetWriter)
                        writeAnnotationSetRefs(offsetWriter)
                        writeAnnotationDirectories(offsetWriter)
                        writeDebugAndCodeItems(offsetWriter, tempFactory.makeDeferredOutputStream())
                        writeClasses(dest, indexWriter, offsetWriter)

                        writeMapItem(offsetWriter)
                        writeHeader(headerWriter, dataSectionOffset, offsetWriter.position)
                    }
                }
            }
            updateSignature(dest)
            updateChecksum(dest)
        } finally {
            dest.close()
        }
    }

    @Throws(IOException::class)
    private fun updateSignature(dataStore: DexDataStore) {
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("SHA-1")
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        }

        val buffer = ByteArray(4 * 1024)
        val input = dataStore.readAt(HeaderItem.SIGNATURE_DATA_START_OFFSET)
        var bytesRead = input.read(buffer)
        while (bytesRead >= 0) {
            md.update(buffer, 0, bytesRead)
            bytesRead = input.read(buffer)
        }

        val signature = md.digest()
        if (signature.size != HeaderItem.SIGNATURE_SIZE) {
            throw RuntimeException("unexpected digest write: " + signature.size + " bytes")
        }

        // write signature
        val output = dataStore.outputAt(HeaderItem.SIGNATURE_OFFSET)
        output.write(signature)
        output.close()
    }

    @Throws(IOException::class)
    private fun updateChecksum(dataStore: DexDataStore) {
        val a32 = Adler32()

        val buffer = ByteArray(4 * 1024)
        val input = dataStore.readAt(HeaderItem.CHECKSUM_DATA_START_OFFSET)
        var bytesRead = input.read(buffer)
        while (bytesRead >= 0) {
            a32.update(buffer, 0, bytesRead)
            bytesRead = input.read(buffer)
        }

        // write checksum, utilizing logic in DexWriter to write the integer value properly
        val output = dataStore.outputAt(HeaderItem.CHECKSUM_OFFSET)
        DexDataWriter.writeInt(output, a32.value.toInt())
        output.close()
    }
    @Throws(IOException::class)
    private fun writeStrings(indexWriter: DexDataWriter, offsetWriter: DexDataWriter) {
        stringIndexSectionOffset = indexWriter.position
        stringDataSectionOffset = offsetWriter.position
        var index = 0
        val stringEntries = ArrayList<MutableMap.MutableEntry<out StringKey, Int>>(stringSection.items)
        stringEntries.sortWith(toStringKeyComparator)

        for (entry in stringEntries) {
            entry.setValue(index++)
            indexWriter.writeInt(offsetWriter.position)
            val stringValue = entry.key.toString()
            offsetWriter.writeUleb128(stringValue.length)
            offsetWriter.writeString(stringValue)
            offsetWriter.write(0)
        }
    }

    @Throws(IOException::class)
    private fun writeTypes(writer: DexDataWriter) {
        typeSectionOffset = writer.position
        var index = 0

        val typeEntries = ArrayList<MutableMap.MutableEntry<out TypeKey, Int>>(typeSection.items)
        typeEntries.sortWith(toStringKeyComparator)

        for (entry in typeEntries) {
            entry.setValue(index++)
            writer.writeInt(stringSection.getItemIndex(typeSection.getString(entry.key)))
        }
    }

    @Throws(IOException::class)
    private fun writeProtos(writer: DexDataWriter) {
        protoSectionOffset = writer.position
        var index = 0

        val protoEntries = ArrayList<MutableMap.MutableEntry<out ProtoRefKey, Int>>(protoSection.items)
        protoEntries.sortWith(comparableKeyComparator<ProtoRefKey>())

        for (entry in protoEntries) {
            entry.setValue(index++)
            val key = entry.key
            writer.writeInt(stringSection.getItemIndex(protoSection.getShorty(key)))
            writer.writeInt(typeSection.getItemIndex(protoSection.getReturnType(key)))
            writer.writeInt(typeListSection.getNullableItemOffset(protoSection.getParameters(key)))
        }
    }

    @Throws(IOException::class)
    private fun writeFields(writer: DexDataWriter) {
        fieldSectionOffset = writer.position
        var index = 0

        val fieldEntries = ArrayList<MutableMap.MutableEntry<out FieldRefKey, Int>>(fieldSection.items)
        fieldEntries.sortWith(comparableKeyComparator<FieldRefKey>())

        for (entry in fieldEntries) {
            entry.setValue(index++)
            val key = entry.key
            writer.writeUshort(typeSection.getItemIndex(fieldSection.getDefiningClass(key)))
            writer.writeUshort(typeSection.getItemIndex(fieldSection.getFieldType(key)))
            writer.writeInt(stringSection.getItemIndex(fieldSection.getName(key)))
        }
    }

    @Throws(IOException::class)
    private fun writeMethods(writer: DexDataWriter) {
        methodSectionOffset = writer.position
        var index = 0

        val methodEntries = ArrayList<MutableMap.MutableEntry<out MethodRefKey, Int>>(methodSection.items)
        methodEntries.sortWith(comparableKeyComparator<MethodRefKey>())

        for (entry in methodEntries) {
            entry.setValue(index++)
            val key = entry.key
            writer.writeUshort(typeSection.getItemIndex(methodSection.getDefiningClass(key)))
            writer.writeUshort(protoSection.getItemIndex(methodSection.getPrototype(key)))
            writer.writeInt(stringSection.getItemIndex(methodSection.getName(key)))
        }
    }

    @Throws(IOException::class)
    private fun writeClasses(dataStore: DexDataStore, indexWriter: DexDataWriter, offsetWriter: DexDataWriter) {
        classIndexSectionOffset = indexWriter.position
        classDataSectionOffset = offsetWriter.position

        val classEntriesKeySorted = ArrayList<MutableMap.MutableEntry<out ClassKey, Int>>(classSection.items)
        classEntriesKeySorted.sortWith(comparableKeyComparator<ClassKey>())

        var index = 0
        for (key in classEntriesKeySorted) {
            index = writeClass(indexWriter, offsetWriter, index, key)
        }

        if (!shouldWriteHiddenApiRestrictions()) {
            return
        }

        offsetWriter.align()
        hiddenApiRestrictionsOffset = offsetWriter.position

        val classEntriesValueSorted = ArrayList<MutableMap.MutableEntry<out ClassKey, Int>>(classSection.items)
        classEntriesValueSorted.sortWith(comparableValueComparator<Int>())
        val restrictionsWriter = RestrictionsWriter(dataStore, offsetWriter, classEntriesValueSorted.size)

        try {
            for (classEntry in classEntriesValueSorted) {
                for (fieldKey in classSection.getSortedStaticFields(classEntry.key)) {
                    restrictionsWriter.writeRestriction(classSection.getFieldHiddenApiRestrictions(fieldKey))
                }
                for (fieldKey in classSection.getSortedInstanceFields(classEntry.key)) {
                    restrictionsWriter.writeRestriction(classSection.getFieldHiddenApiRestrictions(fieldKey))
                }
                for (methodKey in classSection.getSortedDirectMethods(classEntry.key)) {
                    restrictionsWriter.writeRestriction(classSection.getMethodHiddenApiRestrictions(methodKey))
                }
                for (methodKey in classSection.getSortedVirtualMethods(classEntry.key)) {
                    restrictionsWriter.writeRestriction(classSection.getMethodHiddenApiRestrictions(methodKey))
                }
                restrictionsWriter.finishClass()
            }
        } finally {
            restrictionsWriter.close()
        }
    }

    private fun shouldWriteHiddenApiRestrictions(): Boolean {
        return hasHiddenApiRestrictions && opcodes.api >= 29
    }

    private class RestrictionsWriter(
        private val dataStore: DexDataStore,
        private val offsetWriter: DexDataWriter,
        numClasses: Int,
    ) {
        private val startOffset: Int = offsetWriter.position
        private val restrictionsWriter: DexDataWriter = offsetWriter
        private val offsetsWriter: DexDataWriter

        private var writeRestrictionsForClass = false
        private var pendingBlankEntries = 0

        init {
            val offsetsSize = numClasses * HiddenApiClassDataItem.OFFSET_ITEM_SIZE

            // We don't know the size yet, so skip over it
            restrictionsWriter.writeInt(0)

            offsetsWriter = outputAt(dataStore, restrictionsWriter.position)

            // Skip over the offsets
            for (i in 0 until offsetsSize) {
                restrictionsWriter.write(0)
            }
            restrictionsWriter.flush()
        }

        @Throws(IOException::class)
        fun finishClass() {
            if (!writeRestrictionsForClass) {
                // Normally the offset gets written when the first non-blank restriction gets written. If only blank
                // restrictions were added, nothing actually gets written, and we write out a blank offset here.
                offsetsWriter.writeInt(0)
            }

            writeRestrictionsForClass = false
            pendingBlankEntries = 0
        }

        @Throws(IOException::class)
        private fun addBlankEntry() {
            if (writeRestrictionsForClass) {
                restrictionsWriter.writeUleb128(HiddenApiRestriction.WHITELIST.value)
            } else {
                pendingBlankEntries++
            }
        }

        @Throws(IOException::class)
        fun writeRestriction(hiddenApiRestrictions: Set<HiddenApiRestriction>) {
            if (hiddenApiRestrictions.isEmpty()) {
                addBlankEntry()
                return
            }

            if (!writeRestrictionsForClass) {
                writeRestrictionsForClass = true
                offsetsWriter.writeInt(restrictionsWriter.position - startOffset)

                for (i in 0 until pendingBlankEntries) {
                    restrictionsWriter.writeUleb128(HiddenApiRestriction.WHITELIST.value)
                }
                pendingBlankEntries = 0
            }
            restrictionsWriter.writeUleb128(HiddenApiRestriction.combineFlags(hiddenApiRestrictions))
        }

        @Throws(IOException::class)
        fun close() {
            offsetsWriter.close()
            outputAt(dataStore, startOffset).use { writer ->
                writer.writeInt(restrictionsWriter.position - startOffset)
            }
        }
    }
    /**
     * Writes out the class_def_item and class_data_item for the given class.
     *
     * This will recursively write out any unwritten superclass/interface before writing the class itself, as per the
     * dex specification.
     *
     * @return the index for the next class to be written
     */
    @Throws(IOException::class)
    private fun writeClass(indexWriter: DexDataWriter, offsetWriter: DexDataWriter,
                           nextIndex: Int, entry: MutableMap.MutableEntry<out ClassKey, Int>?): Int {
        if (entry == null) {
            // class does not exist in this dex file, cannot write it
            return nextIndex
        }

        if (entry.value != NO_INDEX) {
            // class has already been written, no need to write it
            return nextIndex
        }

        val key = entry.key

        // set a bogus index, to make sure we don't recurse and double-write it
        entry.setValue(0)

        // first, try to write the superclass
        val superEntry = classSection.getClassEntryByType(classSection.getSuperclass(key))
        var result = writeClass(indexWriter, offsetWriter, nextIndex, superEntry)

        // then, try to write interfaces
        for (interfaceTypeKey in typeListSection.getTypes(classSection.getInterfaces(key))) {
            val interfaceEntry = classSection.getClassEntryByType(interfaceTypeKey)
            result = writeClass(indexWriter, offsetWriter, result, interfaceEntry)
        }

        // now set the index for real
        entry.setValue(result++)

        // and finally, write the class itself
        // first, the class_def_item
        indexWriter.writeInt(typeSection.getItemIndex(classSection.getType(key)))
        indexWriter.writeInt(classSection.getAccessFlags(key))
        indexWriter.writeInt(typeSection.getNullableItemIndex(classSection.getSuperclass(key)))
        indexWriter.writeInt(typeListSection.getNullableItemOffset(classSection.getInterfaces(key)))
        indexWriter.writeInt(stringSection.getNullableItemIndex(classSection.getSourceFile(key)))
        indexWriter.writeInt(classSection.getAnnotationDirectoryOffset(key))

        val staticFields = classSection.getSortedStaticFields(key)
        val instanceFields = classSection.getSortedInstanceFields(key)
        val directMethods = classSection.getSortedDirectMethods(key)
        val virtualMethods = classSection.getSortedVirtualMethods(key)
        val classHasData = staticFields.size > 0 ||
                instanceFields.size > 0 ||
                directMethods.size > 0 ||
                virtualMethods.size > 0

        if (classHasData) {
            indexWriter.writeInt(offsetWriter.position)
        } else {
            indexWriter.writeInt(NO_OFFSET)
        }

        val staticInitializers = classSection.getStaticInitializers(key)
        if (staticInitializers != null) {
            indexWriter.writeInt(encodedArraySection.getItemOffset(staticInitializers))
        } else {
            indexWriter.writeInt(NO_OFFSET)
        }

        // now write the class_data_item
        if (classHasData) {
            numClassDataItems++

            offsetWriter.writeUleb128(staticFields.size)
            offsetWriter.writeUleb128(instanceFields.size)
            offsetWriter.writeUleb128(directMethods.size)
            offsetWriter.writeUleb128(virtualMethods.size)

            writeEncodedFields(offsetWriter, staticFields)
            writeEncodedFields(offsetWriter, instanceFields)
            writeEncodedMethods(offsetWriter, directMethods)
            writeEncodedMethods(offsetWriter, virtualMethods)
        }

        return result
    }

    @Throws(IOException::class)
    private fun writeCallSites(writer: DexDataWriter) {
        callSiteSectionOffset = writer.position

        val callSiteEntries = ArrayList<MutableMap.MutableEntry<out CallSiteKey, Int>>(callSiteSection.items)
        callSiteEntries.sortWith(callSiteComparator)

        var index = 0
        for (callSite in callSiteEntries) {
            callSite.setValue(index++)
            writer.writeInt(encodedArraySection.getItemOffset(callSiteSection.getEncodedCallSite(callSite.key)))
        }
    }

    @Throws(IOException::class)
    private fun writeMethodHandles(writer: DexDataWriter) {
        methodHandleSectionOffset = writer.position

        val methodHandleEntries =
            ArrayList<MutableMap.MutableEntry<out MethodHandleKey, Int>>(methodHandleSection.items)
        methodHandleEntries.sortWith(comparableKeyComparator<MethodHandleKey>())

        var index = 0
        for (entry in methodHandleEntries) {
            entry.setValue(index++)
            val methodHandleReference = entry.key
            writer.writeUshort(methodHandleReference.methodHandleType)
            writer.writeUshort(0)
            val memberIndex = when (methodHandleReference.methodHandleType) {
                MethodHandleType.STATIC_PUT,
                MethodHandleType.STATIC_GET,
                MethodHandleType.INSTANCE_PUT,
                MethodHandleType.INSTANCE_GET ->
                    fieldSection.getItemIndex(methodHandleSection.getFieldReference(methodHandleReference))
                MethodHandleType.INVOKE_STATIC,
                MethodHandleType.INVOKE_INSTANCE,
                MethodHandleType.INVOKE_CONSTRUCTOR,
                MethodHandleType.INVOKE_DIRECT,
                MethodHandleType.INVOKE_INTERFACE ->
                    methodSection.getItemIndex(methodHandleSection.getMethodReference(methodHandleReference))
                else -> throw ExceptionWithContext("Invalid method handle type: %d",
                    methodHandleReference.methodHandleType)
            }

            writer.writeUshort(memberIndex)
            writer.writeUshort(0)
        }
    }

    @Throws(IOException::class)
    private fun writeEncodedFields(writer: DexDataWriter, fields: Collection<@JvmWildcard FieldKey>) {
        var prevIndex = 0
        for (key in fields) {
            val index = fieldSection.getFieldIndex(key)
            if (classSection.getFieldHiddenApiRestrictions(key).isNotEmpty()) {
                hasHiddenApiRestrictions = true
            }
            writer.writeUleb128(index - prevIndex)
            writer.writeUleb128(classSection.getFieldAccessFlags(key))
            prevIndex = index
        }
    }

    @Throws(IOException::class)
    private fun writeEncodedMethods(writer: DexDataWriter, methods: Collection<@JvmWildcard MethodKey>) {
        var prevIndex = 0
        for (key in methods) {
            val index = methodSection.getMethodIndex(key)
            if (classSection.getMethodHiddenApiRestrictions(key).isNotEmpty()) {
                hasHiddenApiRestrictions = true
            }
            writer.writeUleb128(index - prevIndex)
            writer.writeUleb128(classSection.getMethodAccessFlags(key))
            writer.writeUleb128(classSection.getCodeItemOffset(key))
            prevIndex = index
        }
    }

    @Throws(IOException::class)
    private fun writeTypeLists(writer: DexDataWriter) {
        writer.align()
        typeListSectionOffset = writer.position
        val typeListEntries = ArrayList<MutableMap.MutableEntry<out TypeListKey, Int>>(typeListSection.items)
        typeListEntries.sortWith(Comparator<MutableMap.MutableEntry<out TypeListKey, Int>> { o1, o2 ->
            CollectionUtils.compareAsIterable(
                CollectionUtils.usingToStringOrdering(),
                typeListSection.getTypes(o1.key),
                typeListSection.getTypes(o2.key)
            )
        })
        for (entry in typeListEntries) {
            writer.align()
            entry.setValue(writer.position)

            val types = typeListSection.getTypes(entry.key)
            writer.writeInt(types.size)
            for (typeKey in types) {
                writer.writeUshort(typeSection.getItemIndex(typeKey))
            }
        }
    }

    @Throws(IOException::class)
    private fun writeEncodedArrays(writer: DexDataWriter) {
        val encodedValueWriter = InternalEncodedValueWriter(writer)
        encodedArraySectionOffset = writer.position
        val encodedArrayEntries =
            ArrayList<MutableMap.MutableEntry<out EncodedArrayKey, Int>>(encodedArraySection.items)
        encodedArrayEntries.sortWith(Comparator<MutableMap.MutableEntry<out EncodedArrayKey, Int>> { o1, o2 ->
            val list1 = encodedArraySection.getEncodedValueList(o1.key)
            val list2 = encodedArraySection.getEncodedValueList(o2.key)
            CollectionUtils.compareAsIterable(
                Comparator<EncodedValue> { e1, e2 ->
                    @Suppress("UNCHECKED_CAST")
                    (e1 as Comparable<Any>).compareTo(e2 as Any)
                },
                list1, list2
            )
        })

        for (entry in encodedArrayEntries) {
            entry.setValue(writer.position)
            val encodedArray = encodedArraySection.getEncodedValueList(entry.key)
            writer.writeUleb128(encodedArray.size)
            for (value in encodedArray) {
                writeEncodedValue(encodedValueWriter, value)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAnnotations(writer: DexDataWriter) {
        val encodedValueWriter = InternalEncodedValueWriter(writer)

        annotationSectionOffset = writer.position
        val annotationEntries = ArrayList<MutableMap.MutableEntry<out AnnotationKey, Int>>(annotationSection.items)
        annotationEntries.sortWith(comparableKeyComparator<AnnotationKey>())
        for (entry in annotationEntries) {
            entry.setValue(writer.position)

            val key = entry.key

            writer.writeUbyte(annotationSection.getVisibility(key))
            writer.writeUleb128(typeSection.getItemIndex(annotationSection.getType(key)))

            val elements = CollectionUtils.immutableSortedCopy(
                annotationSection.getElements(key), BaseAnnotationElement.BY_NAME
            )

            writer.writeUleb128(elements.size)

            for (element in elements) {
                writer.writeUleb128(stringSection.getItemIndex(annotationSection.getElementName(element)))
                writeEncodedValue(encodedValueWriter, annotationSection.getElementValue(element))
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAnnotationSets(writer: DexDataWriter) {
        writer.align()
        annotationSetSectionOffset = writer.position
        if (shouldCreateEmptyAnnotationSet()) {
            writer.writeInt(0)
        }
        val annotationSetEntries =
            ArrayList<MutableMap.MutableEntry<out AnnotationSetKey, Int>>(annotationSetSection.items)
        annotationSetEntries.sortWith(Comparator<MutableMap.MutableEntry<out AnnotationSetKey, Int>> { o1, o2 ->
            CollectionUtils.compareAsSet<Annotation>(
                annotationSetSection.getAnnotations(o1.key),
                annotationSetSection.getAnnotations(o2.key)
            )
        })
        for (entry in annotationSetEntries) {
            val annotations = CollectionUtils.immutableSortedCopy(
                annotationSetSection.getAnnotations(entry.key), BaseAnnotation.BY_TYPE
            )

            writer.align()
            entry.setValue(writer.position)
            writer.writeInt(annotations.size)
            for (annotationKey in annotations) {
                writer.writeInt(annotationSection.getItemOffset(annotationKey))
            }
        }
    }
    @Throws(IOException::class)
    private fun writeAnnotationSetRefs(writer: DexDataWriter) {
        writer.align()
        annotationSetRefSectionOffset = writer.position
        val internedItems = HashMap<List<@JvmWildcard AnnotationSetKey>, Int>()

        for (classKey in classSection.sortedClasses) {
            for (methodKey in classSection.getSortedMethods(classKey)) {
                val parameterAnnotations = classSection.getParameterAnnotations(methodKey)
                if (parameterAnnotations != null) {
                    val prev = internedItems[parameterAnnotations]
                    if (prev != null) {
                        classSection.setAnnotationSetRefListOffset(methodKey, prev)
                    } else {
                        writer.align()
                        val position = writer.position
                        classSection.setAnnotationSetRefListOffset(methodKey, position)
                        internedItems[parameterAnnotations] = position

                        numAnnotationSetRefItems++

                        writer.writeInt(parameterAnnotations.size)
                        for (annotationSetKey in parameterAnnotations) {
                            if (annotationSetSection.getAnnotations(annotationSetKey).size > 0) {
                                writer.writeInt(annotationSetSection.getItemOffset(annotationSetKey))
                            } else if (shouldCreateEmptyAnnotationSet()) {
                                writer.writeInt(annotationSetSectionOffset)
                            } else {
                                writer.writeInt(NO_OFFSET)
                            }
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAnnotationDirectories(writer: DexDataWriter) {
        writer.align()
        annotationDirectorySectionOffset = writer.position
        val internedItems = HashMap<AnnotationSetKey, Int>()

        var tempBuffer = ByteBuffer.allocate(65536)
        tempBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (key in classSection.sortedClasses) {
            // first, we write the field/method/parameter items to a temporary buffer, so that we can get a count
            // of each type, and determine if we even need to write an annotation directory for this class

            val fields = classSection.getSortedFields(key)
            val methods = classSection.getSortedMethods(key)

            // this is how much space we'll need if every field and method has annotations.
            val maxSize = fields.size * 8 + methods.size * 16
            if (maxSize > tempBuffer.capacity()) {
                tempBuffer = ByteBuffer.allocate(maxSize)
                tempBuffer.order(ByteOrder.LITTLE_ENDIAN)
            }

            tempBuffer.clear()

            var fieldAnnotations = 0
            var methodAnnotations = 0
            var parameterAnnotations = 0

            for (field in fields) {
                val fieldAnnotationsKey = classSection.getFieldAnnotations(field)
                if (fieldAnnotationsKey != null) {
                    fieldAnnotations++
                    tempBuffer.putInt(fieldSection.getFieldIndex(field))
                    tempBuffer.putInt(annotationSetSection.getItemOffset(fieldAnnotationsKey))
                }
            }

            for (method in methods) {
                val methodAnnotationsKey = classSection.getMethodAnnotations(method)
                if (methodAnnotationsKey != null) {
                    methodAnnotations++
                    tempBuffer.putInt(methodSection.getMethodIndex(method))
                    tempBuffer.putInt(annotationSetSection.getItemOffset(methodAnnotationsKey))
                }
            }

            for (method in methods) {
                val offset = classSection.getAnnotationSetRefListOffset(method)
                if (offset != NO_OFFSET) {
                    parameterAnnotations++
                    tempBuffer.putInt(methodSection.getMethodIndex(method))
                    tempBuffer.putInt(offset)
                }
            }

            // now, we finally know how many field/method/parameter annotations were written to the temp buffer

            val classAnnotationKey = classSection.getClassAnnotations(key)
            if (fieldAnnotations == 0 && methodAnnotations == 0 && parameterAnnotations == 0) {
                if (classAnnotationKey != null) {
                    // This is an internable directory. Let's see if we've already written one like it
                    val directoryOffset = internedItems[classAnnotationKey]
                    if (directoryOffset != null) {
                        classSection.setAnnotationDirectoryOffset(key, directoryOffset)
                        continue
                    } else {
                        internedItems[classAnnotationKey] = writer.position
                    }
                } else {
                    continue
                }
            }

            // yep, we need to write it out
            numAnnotationDirectoryItems++
            classSection.setAnnotationDirectoryOffset(key, writer.position)

            writer.writeInt(annotationSetSection.getNullableItemOffset(classAnnotationKey))
            writer.writeInt(fieldAnnotations)
            writer.writeInt(methodAnnotations)
            writer.writeInt(parameterAnnotations)
            writer.write(tempBuffer.array(), 0, tempBuffer.position())
        }
    }

    private class CodeItemOffset<MethodKey>(val method: MethodKey, val codeOffset: Int)

    @Throws(IOException::class)
    private fun writeDebugAndCodeItems(offsetWriter: DexDataWriter, temp: DeferredOutputStream) {
        val ehBuf = ByteArrayOutputStream()
        debugSectionOffset = offsetWriter.position

        val codeWriter = DexDataWriter(temp, 0)

        val codeOffsets = ArrayList<CodeItemOffset<MethodKey>>()

        for (classKey in classSection.sortedClasses) {
            val directMethods = classSection.getSortedDirectMethods(classKey)
            val virtualMethods = classSection.getSortedVirtualMethods(classKey)

            val methods: Iterable<MethodKey> = ChainedIterable(
                directMethods, virtualMethods
            )

            for (methodKey in methods) {
                var tryBlocks: List<TryBlock<out ExceptionHandler>> = classSection.getTryBlocks(methodKey)
                var instructions: Iterable<Instruction>? = classSection.getInstructions(methodKey)
                var debugItems: Iterable<DebugItem>? = classSection.getDebugItems(methodKey)

                val instructionIterable = instructions
                if (instructionIterable != null && stringSection.hasJumboIndexes) {
                    var needsFix = false
                    for (instruction in instructionIterable) {
                        if (instruction.opcode == Opcode.CONST_STRING) {
                            @Suppress("UNCHECKED_CAST")
                            val stringRef = (instruction as ReferenceInstruction).reference as StringRef
                            if (stringSection.getItemIndex(stringRef) >= 65536) {
                                needsFix = true
                                break
                            }
                        }
                    }

                    if (needsFix) {
                        val mutableMethodImplementation =
                            classSection.makeMutableMethodImplementation(methodKey)
                        fixInstructions(mutableMethodImplementation)

                        instructions = mutableMethodImplementation.instructions
                        tryBlocks = mutableMethodImplementation.tryBlocks
                        debugItems = mutableMethodImplementation.debugItems
                    }
                }

                val debugItemOffset = writeDebugItem(
                    offsetWriter, classSection.getParameterNames(methodKey), debugItems
                )
                val codeItemOffset: Int
                try {
                    codeItemOffset = writeCodeItem(
                        codeWriter, ehBuf, methodKey, tryBlocks, instructions, debugItemOffset
                    )
                } catch (ex: RuntimeException) {
                    throw ExceptionWithContext(ex, "Exception occurred while writing code_item for method %s",
                        methodSection.getMethodReference(methodKey))
                }

                if (codeItemOffset != -1) {
                    codeOffsets.add(CodeItemOffset(methodKey, codeItemOffset))
                }
            }
        }

        offsetWriter.align()
        codeSectionOffset = offsetWriter.position

        codeWriter.close()
        temp.writeTo(offsetWriter)
        temp.close()

        for (codeOffset in codeOffsets) {
            classSection.setCodeItemOffset(codeOffset.method, codeSectionOffset + codeOffset.codeOffset)
        }
    }
    private fun fixInstructions(methodImplementation: MutableMethodImplementation) {
        val instructions = methodImplementation.instructions

        for (i in instructions.indices) {
            val instruction = instructions[i]

            if (instruction.opcode == Opcode.CONST_STRING) {
                @Suppress("UNCHECKED_CAST")
                val stringRef = (instruction as ReferenceInstruction).reference as StringRef
                if (stringSection.getItemIndex(stringRef) >= 65536) {
                    methodImplementation.replaceInstruction(i, BuilderInstruction31c(
                        Opcode.CONST_STRING_JUMBO,
                        (instruction as OneRegisterInstruction).registerA,
                        instruction.reference
                    ))
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun writeDebugItem(writer: DexDataWriter,
                               parameterNames: Iterable<StringKey?>?,
                               debugItems: Iterable<DebugItem>?): Int {
        var parameterCount = 0
        var lastNamedParameterIndex = -1
        if (parameterNames != null) {
            parameterCount = IteratorUtils.size(parameterNames)
            var index = 0
            for (parameterName in parameterNames) {
                if (parameterName != null) {
                    lastNamedParameterIndex = index
                }
                index++
            }
        }

        if (lastNamedParameterIndex == -1 && (debugItems == null || !debugItems.iterator().hasNext())) {
            return NO_OFFSET
        }

        val debugItemOffset = writer.position
        var startingLineNumber = 0

        if (debugItems != null) {
            for (debugItem in debugItems) {
                if (debugItem is LineNumber) {
                    startingLineNumber = debugItem.lineNumber
                    break
                }
            }
        }

        val tempByteOutput = ByteArrayOutputStream()
        val tempDataWriter = DexDataWriter(tempByteOutput, 0, 64)
        val tempDebugWriter = DebugWriter(stringSection, typeSection, tempDataWriter)

        tempDataWriter.writeUleb128(startingLineNumber)

        tempDataWriter.writeUleb128(parameterCount)
        if (parameterNames != null) {
            var index = 0
            for (parameterName in parameterNames) {
                if (index == parameterCount) {
                    break
                }
                index++
                tempDataWriter.writeUleb128(stringSection.getNullableItemIndex(parameterName) + 1)
            }
        }

        if (debugItems != null) {
            tempDebugWriter.reset(startingLineNumber)

            for (debugItem in debugItems) {
                classSection.writeDebugItem(tempDebugWriter, debugItem)
            }
        }
        // write an END_SEQUENCE opcode, to end the debug item
        tempDataWriter.write(0)

        tempDataWriter.flush()
        val debugInfo = tempByteOutput.toByteArray()
        val wrapBytes = DebugInfoCache(debugInfo)
        val cacheBytes = debugInfoCaches.getOrDefault(wrapBytes, -1)
        if (cacheBytes >= 0) {
            return cacheBytes
        } else {
            writer.write(debugInfo)
            debugInfoCaches[wrapBytes] = debugItemOffset
            numDebugInfoItems++
            return debugItemOffset
        }
    }

    @Throws(IOException::class)
    private fun writeCodeItem(writer: DexDataWriter, ehBuf: ByteArrayOutputStream, methodKey: MethodKey,
                              tryBlocks: List<TryBlock<out ExceptionHandler>>,
                              instructions: Iterable<Instruction>?,
                              debugItemOffset: Int): Int {
        if (instructions == null && debugItemOffset == NO_OFFSET) {
            return -1
        }

        numCodeItemItems++

        writer.align()

        val codeItemOffset = writer.position

        writer.writeUshort(classSection.getRegisterCount(methodKey))

        val isStatic = AccessFlags.STATIC.isSet(classSection.getMethodAccessFlags(methodKey))
        val parameters = typeListSection.getTypes(protoSection.getParameters(methodSection.getPrototype(methodKey)))

        writer.writeUshort(getParameterRegisterCount(parameters, isStatic))

        if (instructions != null) {
            val massagedTryBlocks = TryListBuilder.massageTryBlocks(tryBlocks)

            var outParamCount = 0
            var codeUnitCount = 0
            for (instruction in instructions) {
                codeUnitCount += instruction.codeUnits
                var paramCount = 0
                when (instruction.opcode.referenceType) {
                    ReferenceType.METHOD -> {
                        val refInsn = instruction as ReferenceInstruction
                        val methodRef = refInsn.reference as MethodReference
                        val opcode = instruction.opcode
                        if (isInvokePolymorphic(opcode)) {
                            paramCount = (instruction as VariableRegisterInstruction).registerCount
                        } else {
                            paramCount = getParameterRegisterCount(methodRef, isInvokeStatic(opcode))
                        }
                    }
                    ReferenceType.CALL_SITE ->
                        paramCount = (instruction as VariableRegisterInstruction).registerCount
                }
                if (paramCount > outParamCount) {
                    outParamCount = paramCount
                }
            }

            writer.writeUshort(outParamCount)
            writer.writeUshort(massagedTryBlocks.size)
            writer.writeInt(debugItemOffset)

            val instructionWriter = InstructionWriter.makeInstructionWriter(
                opcodes, writer, stringSection, typeSection, fieldSection, methodSection, protoSection,
                methodHandleSection, callSiteSection
            )

            writer.writeInt(codeUnitCount)
            var codeOffset = 0
            for (instruction in instructions) {
                try {
                    when (instruction.opcode.format) {
                        Format.Format10t -> instructionWriter.write(instruction as Instruction10t)
                        Format.Format10x -> instructionWriter.write(instruction as Instruction10x)
                        Format.Format11n -> instructionWriter.write(instruction as Instruction11n)
                        Format.Format11x -> instructionWriter.write(instruction as Instruction11x)
                        Format.Format12x -> instructionWriter.write(instruction as Instruction12x)
                        Format.Format20bc -> instructionWriter.write(instruction as Instruction20bc)
                        Format.Format20t -> instructionWriter.write(instruction as Instruction20t)
                        Format.Format21c -> instructionWriter.write(instruction as Instruction21c)
                        Format.Format21ih -> instructionWriter.write(instruction as Instruction21ih)
                        Format.Format21lh -> instructionWriter.write(instruction as Instruction21lh)
                        Format.Format21s -> instructionWriter.write(instruction as Instruction21s)
                        Format.Format21t -> instructionWriter.write(instruction as Instruction21t)
                        Format.Format22b -> instructionWriter.write(instruction as Instruction22b)
                        Format.Format22c -> instructionWriter.write(instruction as Instruction22c)
                        Format.Format22cs -> instructionWriter.write(instruction as Instruction22cs)
                        Format.Format22s -> instructionWriter.write(instruction as Instruction22s)
                        Format.Format22t -> instructionWriter.write(instruction as Instruction22t)
                        Format.Format22x -> instructionWriter.write(instruction as Instruction22x)
                        Format.Format23x -> instructionWriter.write(instruction as Instruction23x)
                        Format.Format30t -> instructionWriter.write(instruction as Instruction30t)
                        Format.Format31c -> instructionWriter.write(instruction as Instruction31c)
                        Format.Format31i -> instructionWriter.write(instruction as Instruction31i)
                        Format.Format31t -> instructionWriter.write(instruction as Instruction31t)
                        Format.Format32x -> instructionWriter.write(instruction as Instruction32x)
                        Format.Format35c -> instructionWriter.write(instruction as Instruction35c)
                        Format.Format35mi -> instructionWriter.write(instruction as Instruction35mi)
                        Format.Format35ms -> instructionWriter.write(instruction as Instruction35ms)
                        Format.Format3rc -> instructionWriter.write(instruction as Instruction3rc)
                        Format.Format3rmi -> instructionWriter.write(instruction as Instruction3rmi)
                        Format.Format3rms -> instructionWriter.write(instruction as Instruction3rms)
                        Format.Format45cc -> instructionWriter.write(instruction as Instruction45cc)
                        Format.Format4rcc -> instructionWriter.write(instruction as Instruction4rcc)
                        Format.Format51l -> instructionWriter.write(instruction as Instruction51l)
                        Format.ArrayPayload -> instructionWriter.write(instruction as ArrayPayload)
                        Format.PackedSwitchPayload -> instructionWriter.write(instruction as PackedSwitchPayload)
                        Format.SparseSwitchPayload -> instructionWriter.write(instruction as SparseSwitchPayload)
                        else -> throw ExceptionWithContext("Unsupported instruction format: %s",
                            instruction.opcode.format)
                    }
                } catch (ex: RuntimeException) {
                    throw ExceptionWithContext(ex, "Error while writing instruction at code offset 0x%x", codeOffset)
                }
                codeOffset += instruction.codeUnits
            }

            if (massagedTryBlocks.size > 0) {
                writer.align()

                // filter out unique lists of exception handlers
                val exceptionHandlerOffsetMap = HashMap<List<ExceptionHandler>, Int>()
                for (tryBlock in massagedTryBlocks) {
                    exceptionHandlerOffsetMap[tryBlock.exceptionHandlers] = 0
                }
                DexDataWriter.writeUleb128(ehBuf, exceptionHandlerOffsetMap.size)

                for (tryBlock in massagedTryBlocks) {
                    val startAddress = tryBlock.startCodeAddress
                    val endAddress = startAddress + tryBlock.codeUnitCount

                    val tbCodeUnitCount = endAddress - startAddress

                    writer.writeInt(startAddress)
                    writer.writeUshort(tbCodeUnitCount)

                    if (tryBlock.exceptionHandlers.size == 0) {
                        throw ExceptionWithContext("No exception handlers for the try block!")
                    }

                    var offset = exceptionHandlerOffsetMap.getValue(tryBlock.exceptionHandlers)
                    if (offset != 0) {
                        // exception handler has already been written out, just use it
                        writer.writeUshort(offset)
                    } else {
                        // if offset has not been set yet, we are about to write out a new exception handler
                        offset = ehBuf.size()
                        writer.writeUshort(offset)
                        exceptionHandlerOffsetMap[tryBlock.exceptionHandlers] = offset

                        // check if the last exception handler is a catch-all and adjust the size accordingly
                        var ehSize = tryBlock.exceptionHandlers.size
                        val ehLast = tryBlock.exceptionHandlers[ehSize - 1]
                        if (ehLast.exceptionType == null) {
                            ehSize = ehSize * (-1) + 1
                        }

                        // now let's layout the exception handlers, assuming that catch-all is always last
                        DexDataWriter.writeSleb128(ehBuf, ehSize)
                        for (eh in tryBlock.exceptionHandlers) {
                            val exceptionTypeKey = classSection.getExceptionType(eh)

                            val codeAddress = eh.handlerCodeAddress

                            if (exceptionTypeKey != null) {
                                //regular exception handling
                                DexDataWriter.writeUleb128(ehBuf, typeSection.getItemIndex(exceptionTypeKey))
                                DexDataWriter.writeUleb128(ehBuf, codeAddress)
                            } else {
                                //catch-all
                                DexDataWriter.writeUleb128(ehBuf, codeAddress)
                            }
                        }
                    }
                }

                if (ehBuf.size() > 0) {
                    ehBuf.writeTo(writer)
                    ehBuf.reset()
                }
            }
        } else {
            // no instructions, all we have is the debug item offset
            writer.writeUshort(0)
            writer.writeUshort(0)
            writer.writeInt(debugItemOffset)
            writer.writeInt(0)
        }

        return codeItemOffset
    }
    private fun calcNumItems(): Int {
        var numItems = 0

        // header item
        numItems++

        if (stringSection.items.size > 0) {
            numItems += 2 // index and data
        }
        if (typeSection.items.size > 0) {
            numItems++
        }
        if (protoSection.items.size > 0) {
            numItems++
        }
        if (fieldSection.items.size > 0) {
            numItems++
        }
        if (methodSection.items.size > 0) {
            numItems++
        }
        if (callSiteSection.items.size > 0) {
            numItems++
        }
        if (methodHandleSection.items.size > 0) {
            numItems++
        }
        if (typeListSection.items.size > 0) {
            numItems++
        }
        if (encodedArraySection.items.size > 0) {
            numItems++
        }
        if (annotationSection.items.size > 0) {
            numItems++
        }
        if (annotationSetSection.items.size > 0 || shouldCreateEmptyAnnotationSet()) {
            numItems++
        }
        if (numAnnotationSetRefItems > 0) {
            numItems++
        }
        if (numAnnotationDirectoryItems > 0) {
            numItems++
        }
        if (numDebugInfoItems > 0) {
            numItems++
        }
        if (numCodeItemItems > 0) {
            numItems++
        }
        if (classSection.items.size > 0) {
            numItems++
        }
        if (numClassDataItems > 0) {
            numItems++
        }
        if (shouldWriteHiddenApiRestrictions()) {
            numItems++
        }
        // map item itself
        numItems++

        return numItems
    }

    @Throws(IOException::class)
    private fun writeMapItem(writer: DexDataWriter) {
        writer.align()
        mapSectionOffset = writer.position
        val numItems = calcNumItems()

        writer.writeInt(numItems)

        // index section
        writeMapItem(writer, ItemType.HEADER_ITEM, 1, 0)
        writeMapItem(writer, ItemType.STRING_ID_ITEM, stringSection.items.size, stringIndexSectionOffset)
        writeMapItem(writer, ItemType.TYPE_ID_ITEM, typeSection.items.size, typeSectionOffset)
        writeMapItem(writer, ItemType.PROTO_ID_ITEM, protoSection.items.size, protoSectionOffset)
        writeMapItem(writer, ItemType.FIELD_ID_ITEM, fieldSection.items.size, fieldSectionOffset)
        writeMapItem(writer, ItemType.METHOD_ID_ITEM, methodSection.items.size, methodSectionOffset)
        writeMapItem(writer, ItemType.CLASS_DEF_ITEM, classSection.items.size, classIndexSectionOffset)
        writeMapItem(writer, ItemType.CALL_SITE_ID_ITEM, callSiteSection.items.size, callSiteSectionOffset)
        writeMapItem(writer, ItemType.METHOD_HANDLE_ITEM, methodHandleSection.items.size,
            methodHandleSectionOffset)

        // data section
        writeMapItem(writer, ItemType.STRING_DATA_ITEM, stringSection.items.size, stringDataSectionOffset)
        writeMapItem(writer, ItemType.TYPE_LIST, typeListSection.items.size, typeListSectionOffset)
        writeMapItem(writer, ItemType.ENCODED_ARRAY_ITEM, encodedArraySection.items.size,
            encodedArraySectionOffset)
        writeMapItem(writer, ItemType.ANNOTATION_ITEM, annotationSection.items.size, annotationSectionOffset)
        writeMapItem(writer, ItemType.ANNOTATION_SET_ITEM,
            annotationSetSection.items.size + (if (shouldCreateEmptyAnnotationSet()) 1 else 0),
            annotationSetSectionOffset)
        writeMapItem(writer, ItemType.ANNOTATION_SET_REF_LIST, numAnnotationSetRefItems, annotationSetRefSectionOffset)
        writeMapItem(writer, ItemType.ANNOTATION_DIRECTORY_ITEM, numAnnotationDirectoryItems,
            annotationDirectorySectionOffset)
        writeMapItem(writer, ItemType.DEBUG_INFO_ITEM, numDebugInfoItems, debugSectionOffset)
        writeMapItem(writer, ItemType.CODE_ITEM, numCodeItemItems, codeSectionOffset)
        writeMapItem(writer, ItemType.CLASS_DATA_ITEM, numClassDataItems, classDataSectionOffset)

        if (shouldWriteHiddenApiRestrictions()) {
            writeMapItem(writer, ItemType.HIDDENAPI_CLASS_DATA_ITEM, 1, hiddenApiRestrictionsOffset)
        }

        writeMapItem(writer, ItemType.MAP_LIST, 1, mapSectionOffset)
    }

    @Throws(IOException::class)
    private fun writeMapItem(writer: DexDataWriter, type: Int, size: Int, offset: Int) {
        if (size > 0) {
            writer.writeUshort(type)
            writer.writeUshort(0)
            writer.writeInt(size)
            writer.writeInt(offset)
        }
    }
    @Throws(IOException::class)
    private fun writeHeader(writer: DexDataWriter, dataOffset: Int, fileSize: Int) {
        // Write the appropriate header.
        writer.write(HeaderItem.getMagicForApi(opcodes.api))

        // checksum placeholder
        writer.writeInt(0)

        // signature placeholder
        writer.write(ByteArray(20))

        writer.writeInt(fileSize)
        writer.writeInt(HeaderItem.ITEM_SIZE)
        writer.writeInt(HeaderItem.LITTLE_ENDIAN_TAG)

        // link
        writer.writeInt(0)
        writer.writeInt(0)

        // map
        writer.writeInt(mapSectionOffset)

        // index sections

        writeSectionInfo(writer, stringSection.items.size, stringIndexSectionOffset)
        writeSectionInfo(writer, typeSection.items.size, typeSectionOffset)
        writeSectionInfo(writer, protoSection.items.size, protoSectionOffset)
        writeSectionInfo(writer, fieldSection.items.size, fieldSectionOffset)
        writeSectionInfo(writer, methodSection.items.size, methodSectionOffset)
        writeSectionInfo(writer, classSection.items.size, classIndexSectionOffset)

        // data section
        writer.writeInt(fileSize - dataOffset)
        writer.writeInt(dataOffset)
    }

    @Throws(IOException::class)
    private fun writeSectionInfo(writer: DexDataWriter, numItems: Int, offset: Int) {
        writer.writeInt(numItems)
        if (numItems > 0) {
            writer.writeInt(offset)
        } else {
            writer.writeInt(0)
        }
    }

    private fun shouldCreateEmptyAnnotationSet(): Boolean {
        // Workaround for a crash in Dalvik VM before Jelly Bean MR1 (4.2)
        // which is triggered by NO_OFFSET in parameter annotation list.
        // (https://code.google.com/p/android/issues/detail?id=35304)
        return opcodes.api < 17
    }

    abstract inner class SectionProvider {
        abstract val stringSection: StringSectionType
        abstract val typeSection: TypeSectionType
        abstract val protoSection: ProtoSectionType
        abstract val fieldSection: FieldSectionType
        abstract val methodSection: MethodSectionType
        abstract val classSection: ClassSectionType
        abstract val callSiteSection: CallSiteSectionType
        abstract val methodHandleSection: MethodHandleSectionType
        abstract val typeListSection: TypeListSectionType
        abstract val annotationSection: AnnotationSectionType
        abstract val annotationSetSection: AnnotationSetSectionType
        abstract val encodedArraySection: EncodedArraySectionType
    }
    companion object {
        const val NO_INDEX = -1
        const val NO_OFFSET = 0

        const val MAX_POOL_SIZE = (1 shl 16)

        private val toStringKeyComparator = Comparator<MutableMap.MutableEntry<*, *>> { o1, o2 ->
            o1.key.toString().compareTo(o2.key.toString())
        }

        private fun <T : Comparable<T>> comparableKeyComparator(): Comparator<MutableMap.MutableEntry<out T, *>> {
            return Comparator { o1, o2 -> o1.key.compareTo(o2.key) }
        }

        private fun <T : Comparable<T>> comparableValueComparator(): Comparator<MutableMap.MutableEntry<*, out T>> {
            return Comparator { o1, o2 -> o1.value.compareTo(o2.value) }
        }

        @Throws(IOException::class)
        private fun outputAt(dataStore: DexDataStore, filePosition: Int): DexDataWriter {
            return DexDataWriter(dataStore.outputAt(filePosition), filePosition)
        }
    }
}
