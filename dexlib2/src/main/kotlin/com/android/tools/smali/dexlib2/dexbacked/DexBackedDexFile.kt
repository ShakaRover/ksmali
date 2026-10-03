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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.dexbacked.raw.CallSiteIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ClassDefItem
import com.android.tools.smali.dexlib2.dexbacked.raw.FieldIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HiddenApiClassDataItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ItemType
import com.android.tools.smali.dexlib2.dexbacked.raw.MapItem
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodHandleItem
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.StringIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeIdItem
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedCallSiteReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedFieldReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodHandleReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodProtoReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedStringReference
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedTypeReference
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeSet
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.util.DexUtil
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.util.InputStreamUtil
import java.io.IOException
import java.io.InputStream
import java.util.AbstractList

open class DexBackedDexFile internal constructor(
    opcodesArg: Opcodes?,
    buf: ByteArray,
    offset: Int,
    verifyMagic: Boolean,
    header_offset: Int
) : DexFile {
    val buffer: DexBuffer
    val dataBuffer: DexBuffer

    private lateinit var opcodesField: Opcodes

    override val opcodes: Opcodes
        get() = opcodesField

    private val fileSize: Int
    private val stringCount: Int
    private val stringStartOffset: Int
    private val typeCount: Int
    private val typeStartOffset: Int
    private val protoCount: Int
    private val protoStartOffset: Int
    private val fieldCount: Int
    private val fieldStartOffset: Int
    private val methodCount: Int
    private val methodStartOffset: Int
    private val classCount: Int
    private val classStartOffset: Int
    private val mapOffset: Int
    private var hiddenApiRestrictionsOffset = -1
    private val headerOffset: Int
    private val containerSize: Int

    init {
        if (offset < 0 || offset > buf.size) {
            throw DexUtil.InvalidFile(
                "Invalid offset ${offset} for buffer of length ${buf.size}"
            )
        }
        if (header_offset < 0 || buf.size - offset < header_offset) {
            throw DexUtil.InvalidFile(
                "Invalid header_offset ${header_offset} for buffer of length ${buf.size} at offset ${offset}"
            )
        }

        buffer = DexBuffer(buf, offset)
        dataBuffer = DexBuffer(buf, offset + baseDataOffset)

        val dexVersion = getVersion(buf, offset + header_offset, verifyMagic)

        opcodesField = if (opcodesArg == null) {
            getDefaultOpcodes(dexVersion)
        } else {
            opcodesArg
        }

        val requiredHeaderSize = if (dexVersion >= 41) 120 else 112
        if (buf.size - offset - header_offset < requiredHeaderSize) {
            throw DexUtil.InvalidFile(
                "File is too short to contain a valid dex header (size: ${buf.size - offset - header_offset}, required: ${requiredHeaderSize})"
            )
        }

        fileSize = buffer.readSmallUint(header_offset + HeaderItem.FILE_SIZE_OFFSET)
        if (fileSize < requiredHeaderSize) {
            throw DexUtil.InvalidFile(
                "Invalid file_size ${fileSize} (smaller than required header size ${requiredHeaderSize})"
            )
        }
        if (fileSize > buf.size - offset - header_offset) {
            throw DexUtil.InvalidFile(
                "Invalid file_size ${fileSize} (exceeds remaining buffer length ${buf.size - offset - header_offset})"
            )
        }
        stringCount = buffer.readSmallUint(header_offset + HeaderItem.STRING_COUNT_OFFSET)
        stringStartOffset = buffer.readSmallUint(header_offset + HeaderItem.STRING_START_OFFSET)
        typeCount = buffer.readSmallUint(header_offset + HeaderItem.TYPE_COUNT_OFFSET)
        typeStartOffset = buffer.readSmallUint(header_offset + HeaderItem.TYPE_START_OFFSET)
        protoCount = buffer.readSmallUint(header_offset + HeaderItem.PROTO_COUNT_OFFSET)
        protoStartOffset = buffer.readSmallUint(header_offset + HeaderItem.PROTO_START_OFFSET)
        fieldCount = buffer.readSmallUint(header_offset + HeaderItem.FIELD_COUNT_OFFSET)
        fieldStartOffset = buffer.readSmallUint(header_offset + HeaderItem.FIELD_START_OFFSET)
        methodCount = buffer.readSmallUint(header_offset + HeaderItem.METHOD_COUNT_OFFSET)
        methodStartOffset = buffer.readSmallUint(header_offset + HeaderItem.METHOD_START_OFFSET)
        classCount = buffer.readSmallUint(header_offset + HeaderItem.CLASS_COUNT_OFFSET)
        classStartOffset = buffer.readSmallUint(header_offset + HeaderItem.CLASS_START_OFFSET)
        mapOffset = buffer.readSmallUint(header_offset + HeaderItem.MAP_OFFSET)

        this.headerOffset = header_offset
        var container_off = 0
        if (dexVersion >= 41) {
            container_off = buffer.readSmallUint(
                header_offset + HeaderItem.HEADER_OFFSET_OFFSET
            )
            this.containerSize = buffer.readSmallUint(
                header_offset + HeaderItem.CONTAINER_SIZE_OFFSET
            )
            if (this.containerSize < header_offset + fileSize) {
                throw DexUtil.InvalidFile(
                    "DEX entry (header_offset: ${header_offset}, file_size: ${fileSize}) exceeds container_size ${containerSize}"
                )
            }
            if (this.containerSize > buf.size - offset) {
                throw DexUtil.InvalidFile(
                    "Invalid container_size ${containerSize} (exceeds buffer length ${buf.size - offset})"
                )
            }
        } else if (CDexBackedDexFile.isCdex(buf, offset + header_offset)) {
            val dataStart = buffer.readSmallUint(header_offset + HeaderItem.DATA_START_OFFSET)
            val dataSize = buffer.readSmallUint(header_offset + HeaderItem.DATA_SIZE_OFFSET)
            this.containerSize = Math.min(
                buf.size - offset, Math.max(this.fileSize, dataStart + dataSize)
            )
        } else {
            this.containerSize = this.fileSize
        }
        if (container_off != header_offset) {
            throw DexUtil.InvalidFile("Unexpected container offset in header")
        }

        if (mapOffset < 0 || mapOffset > containerSize - 4) {
            throw DexUtil.InvalidFile("Invalid mapOffset ${mapOffset}")
        }

        val dataOffset = baseDataOffset
        if (dataOffset > containerSize - 4 - mapOffset) {
            throw DexUtil.InvalidFile("Invalid mapOffset ${mapOffset}")
        }

        // Eagerly read mapSize to validate map bounds
        val mapSize = dataBuffer.readSmallUint(mapOffset)
        val remainingSize = containerSize - dataOffset - mapOffset - 4
        if (mapSize > remainingSize / MapItem.ITEM_SIZE) {
            throw DexUtil.InvalidFile("Map extends beyond container bounds")
        }
    }

    protected constructor(
        opcodes: Opcodes?,
        buf: ByteArray,
        offset: Int,
        verifyMagic: Boolean
    ) : this(opcodes, buf, offset, verifyMagic, 0)

    constructor(opcodes: Opcodes?, buf: DexBuffer) : this(opcodes, buf.buf, buf.baseOffset)

    constructor(opcodes: Opcodes?, buf: ByteArray, offset: Int) : this(
        opcodes, buf, offset, false
    )

    constructor(opcodes: Opcodes?, buf: ByteArray) : this(opcodes, buf, 0, true)

    /**
     * @return The offset that various data offsets are relative to. This is always 0 for a dex file, but may be
     * different for other related formats (e.g. cdex).
     */
    open val baseDataOffset: Int
        get() = 0

    /**
     * @return Size of single dex file (out of potentially several dex files within a container).
     */
    fun getFileSize(): Int {
        return fileSize
    }

    /**
     * @return True if this is the first entry in a DEX container (or classic DEX).
     */
    fun isDexContainerFirstEntry(): Boolean {
        return headerOffset == 0
    }

    /**
     * @return True if this is the last entry in a DEX container, ignoring trailing garbage.
     */
    fun isDexContainerLastEntry(): Boolean {
        return headerOffset + fileSize >= containerSize
    }

    protected open fun getVersion(buf: ByteArray, offset: Int, verifyMagic: Boolean): Int {
        return if (verifyMagic) {
            DexUtil.verifyDexHeader(buf, offset)
        } else {
            HeaderItem.getVersion(buf, offset)
        }
    }

    protected open fun getDefaultOpcodes(version: Int): Opcodes {
        return Opcodes.forDexVersion(version)
    }

    companion object {
        @Throws(IOException::class)
        fun fromInputStream(opcodes: Opcodes?, `is`: InputStream): DexBackedDexFile {
            DexUtil.verifyDexHeader(`is`)

            val buf = InputStreamUtil.toByteArray(`is`)
            return DexBackedDexFile(opcodes, buf, 0, false)
        }
    }

    open fun supportsOptimizedOpcodes(): Boolean {
        return false
    }

    override val classes: Set<DexBackedClassDef>
        get() = object : FixedSizeSet<DexBackedClassDef>() {
            override val size: Int
                get() = classCount

            override fun readItem(index: Int): DexBackedClassDef {
                return classSection.get(index)
            }
        }

    val stringReferences: List<DexBackedStringReference>
        get() {
            return object : AbstractList<DexBackedStringReference>() {
            override fun get(index: Int): DexBackedStringReference {
                if (index < 0 || index >= stringSection.size) {
                    throw IndexOutOfBoundsException()
                }
                return DexBackedStringReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() = stringSection.size
        }
    }

    val typeReferences: List<DexBackedTypeReference>
        get() {
            return object : AbstractList<DexBackedTypeReference>() {
            override fun get(index: Int): DexBackedTypeReference {
                if (index < 0 || index >= typeSection.size) {
                    throw IndexOutOfBoundsException()
                }
                return DexBackedTypeReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() = typeSection.size
        }
    }

    fun getReferences(referenceType: Int): List<Reference> {
        return when (referenceType) {
            ReferenceType.STRING -> stringReferences
            ReferenceType.TYPE -> typeReferences
            ReferenceType.METHOD -> methodSection
            ReferenceType.FIELD -> fieldSection
            ReferenceType.METHOD_PROTO -> methodSection
            ReferenceType.METHOD_HANDLE -> methodHandleSection
            ReferenceType.CALL_SITE -> callSiteSection
            else -> throw IllegalArgumentException(
                "Invalid reference type: ${referenceType}"
            )
        }
    }

    val mapItems: List<MapItem>
        get() {
            val mapSize = dataBuffer.readSmallUint(mapOffset)

            return object : FixedSizeList<MapItem>() {
                override val size: Int
                    get() = mapSize

                override fun readItem(index: Int): MapItem {
                    val mapItemOffset = mapOffset + 4 + index * MapItem.ITEM_SIZE
                    return MapItem(this@DexBackedDexFile, mapItemOffset)
                }
            }
        }

    fun getMapItemForSection(itemType: Int): MapItem? {
        for (mapItem in mapItems) {
            if (mapItem.type == itemType) {
                return mapItem
            }
        }
        return null
    }

    class NotADexFile : RuntimeException {
        constructor()

        constructor(cause: Throwable?) : super(cause)

        constructor(message: String?) : super(message)

        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    val stringSection: OptionalIndexedSection<String> =
        object : OptionalIndexedSection<String>() {
            override fun get(index: Int): String {
                val stringOffset = getOffset(index)
                val stringDataOffset = buffer.readSmallUint(stringOffset)
                val reader: DexReader<out DexBuffer> = dataBuffer.readerAt(stringDataOffset)
                val utf16Length = reader.readSmallUleb128()
                return reader.readString(utf16Length)
            }

            override val size: Int
                get() = stringCount

            override fun getOptional(index: Int): String? {
                if (index == -1) {
                    return null
                }
                return get(index)
            }

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid string index ${index}, not in [0, ${size})"
                    )
                }
                return stringStartOffset + index * StringIdItem.ITEM_SIZE
            }
        }

    val typeSection: OptionalIndexedSection<String> =
        object : OptionalIndexedSection<String>() {
            override fun get(index: Int): String {
                val typeOffset = getOffset(index)
                val stringIndex = buffer.readSmallUint(typeOffset)
                return stringSection.get(stringIndex)
            }

            override val size: Int
                get() = typeCount

            override fun getOptional(index: Int): String? {
                if (index == -1) {
                    return null
                }
                return get(index)
            }

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid type index ${index}, not in [0, ${size})"
                    )
                }
                return typeStartOffset + index * TypeIdItem.ITEM_SIZE
            }
        }

    val fieldSection: IndexedSection<DexBackedFieldReference> =
        object : IndexedSection<DexBackedFieldReference>() {
            override fun get(index: Int): DexBackedFieldReference {
                return DexBackedFieldReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() = fieldCount

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid field index ${index}, not in [0, ${size})"
                    )
                }

                return fieldStartOffset + index * FieldIdItem.ITEM_SIZE
            }
        }

    val methodSection: IndexedSection<DexBackedMethodReference> =
        object : IndexedSection<DexBackedMethodReference>() {
            override fun get(index: Int): DexBackedMethodReference {
                return DexBackedMethodReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() = methodCount

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid method index ${index}, not in [0, ${size})"
                    )
                }

                return methodStartOffset + index * MethodIdItem.ITEM_SIZE
            }
        }

    val protoSection: IndexedSection<DexBackedMethodProtoReference> =
        object : IndexedSection<DexBackedMethodProtoReference>() {
            override fun get(index: Int): DexBackedMethodProtoReference {
                return DexBackedMethodProtoReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() = protoCount

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid proto index ${index}, not in [0, ${size})"
                    )
                }

                return protoStartOffset + index * ProtoIdItem.ITEM_SIZE
            }
        }

    val classSection: IndexedSection<DexBackedClassDef> =
        object : IndexedSection<DexBackedClassDef>() {
            override fun get(index: Int): DexBackedClassDef {
                return DexBackedClassDef(
                    this@DexBackedDexFile, getOffset(index),
                    readHiddenApiRestrictionsOffset(index)
                )
            }

            override val size: Int
                get() = classCount

            override fun getOffset(index: Int): Int {
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid class index ${index}, not in [0, ${size})"
                    )
                }

                return classStartOffset + index * ClassDefItem.ITEM_SIZE
            }
        }

    val callSiteSection: IndexedSection<DexBackedCallSiteReference> =
        object : IndexedSection<DexBackedCallSiteReference>() {
            override fun get(index: Int): DexBackedCallSiteReference {
                return DexBackedCallSiteReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() {
                    val mapItem = getMapItemForSection(ItemType.CALL_SITE_ID_ITEM)
                        ?: return 0
                    return mapItem.itemCount
                }

            override fun getOffset(index: Int): Int {
                val mapItem = getMapItemForSection(ItemType.CALL_SITE_ID_ITEM)
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid callsite index ${index}, not in [0, ${size})"
                    )
                }
                return mapItem!!.getOffset() + index * CallSiteIdItem.ITEM_SIZE
            }
        }

    val methodHandleSection: IndexedSection<DexBackedMethodHandleReference> =
        object : IndexedSection<DexBackedMethodHandleReference>() {
            override fun get(index: Int): DexBackedMethodHandleReference {
                return DexBackedMethodHandleReference(this@DexBackedDexFile, index)
            }

            override val size: Int
                get() {
                    val mapItem = getMapItemForSection(ItemType.METHOD_HANDLE_ITEM)
                        ?: return 0
                    return mapItem.itemCount
                }

            override fun getOffset(index: Int): Int {
                val mapItem = getMapItemForSection(ItemType.METHOD_HANDLE_ITEM)
                if (index < 0 || index >= size) {
                    throw IndexOutOfBoundsException(
                        "Invalid method handle index ${index}, not in [0, ${size})"
                    )
                }
                return mapItem!!.getOffset() + index * MethodHandleItem.ITEM_SIZE
            }
        }

    open fun createMethodImplementation(
        dexFile: DexBackedDexFile,
        method: DexBackedMethod,
        codeOffset: Int
    ): DexBackedMethodImplementation {
        return DexBackedMethodImplementation(dexFile, method, codeOffset)
    }

    private fun getHiddenApiRestrictionsOffset(): Int {
        if (hiddenApiRestrictionsOffset == -1) {
            val mapItem = getMapItemForSection(ItemType.HIDDENAPI_CLASS_DATA_ITEM)
            if (mapItem != null) {
                hiddenApiRestrictionsOffset = mapItem.getOffset()
            } else {
                hiddenApiRestrictionsOffset = DexWriter.NO_OFFSET
            }
        }
        return hiddenApiRestrictionsOffset
    }

    private fun readHiddenApiRestrictionsOffset(classIndex: Int): Int {
        val restrictionsOffset = getHiddenApiRestrictionsOffset()
        if (restrictionsOffset == DexWriter.NO_OFFSET) {
            return DexWriter.NO_OFFSET
        }

        val offset = buffer.readInt(
            restrictionsOffset +
                HiddenApiClassDataItem.OFFSETS_LIST_OFFSET +
                classIndex * HiddenApiClassDataItem.OFFSET_ITEM_SIZE
        )
        if (offset == DexWriter.NO_OFFSET) {
            return DexWriter.NO_OFFSET
        }

        return restrictionsOffset + offset
    }

    abstract class OptionalIndexedSection<T> : IndexedSection<T>() {
        /**
         * @param index The index of the item, or -1 for a null item.
         * @return The value at the given index, or null if index is -1.
         * @throws IndexOutOfBoundsException if the index is out of bounds and is not -1.
         */
        abstract fun getOptional(index: Int): T?
    }

    abstract class IndexedSection<T> : AbstractList<T>() {
        /**
         * @param index The index of the item to get the offset for.
         * @return The offset from the beginning of the dex file to the specified item.
         * @throws IndexOutOfBoundsException if the index is out of bounds.
         */
        abstract fun getOffset(index: Int): Int
    }
}
