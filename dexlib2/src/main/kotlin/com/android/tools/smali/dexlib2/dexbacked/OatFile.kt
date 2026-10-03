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

import com.android.tools.smali.dexlib2.DexFileFactory.DexFileNotFoundException
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.util.DexUtil
import com.android.tools.smali.util.AbstractForwardSequentialList
import com.android.tools.smali.util.InputStreamUtil
import com.android.tools.smali.util.TransformedIterable.TransformedIterator
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.AbstractList
import java.util.Arrays
import java.util.function.Function

class OatFile @JvmOverloads constructor(
    buf: ByteArray,
    vdexProviderArg: VdexProvider? = null
) : DexBuffer(buf), MultiDexContainer<DexBackedDexFile> {
    private val is64bit: Boolean
    private val oatHeader: OatHeader
    private val opcodes: Opcodes
    private val vdexProvider: VdexProvider?

    init {
        if (buf.size < MIN_ELF_HEADER_SIZE) {
            throw NotAnOatFileException()
        }

        verifyMagic(buf)

        if (buf[4] == 1.toByte()) {
            is64bit = false
        } else if (buf[4] == 2.toByte()) {
            is64bit = true
        } else {
            throw InvalidOatFileException(String.format("Invalid word-size value: %x", buf[5]))
        }

        var localOatHeader: OatHeader? = null
        val symbolTable = symbolTable
        for (symbol in symbolTable.symbols) {
            if (symbol.name == "oatdata") {
                localOatHeader = OatHeader(symbol.fileOffset)
                break
            }
        }

        if (localOatHeader == null) {
            throw InvalidOatFileException("Oat file has no oatdata symbol")
        }
        this.oatHeader = localOatHeader

        if (!oatHeader.isValid) {
            throw InvalidOatFileException("Invalid oat magic value")
        }

        this.opcodes = Opcodes.forArtVersion(oatHeader.version)
        this.vdexProvider = vdexProviderArg
    }

    val oatVersion: Int
        get() = oatHeader.version

    fun isSupportedVersion(): Int {
        val version = oatVersion
        if (version < MIN_OAT_VERSION) {
            return UNSUPPORTED
        }
        if (version <= MAX_OAT_VERSION) {
            return SUPPORTED
        }
        return UNKNOWN
    }

    val bootClassPath: MutableList<String>
        get() {
            if (oatVersion < 75) {
                return mutableListOf<String>()
            }
            val bcp = oatHeader.getKeyValue("bootclasspath") ?: return mutableListOf<String>()
            return bcp.split(":").toMutableList()
        }

    val dexFiles: List<DexBackedDexFile>
        get() {
            return object : AbstractForwardSequentialList<DexBackedDexFile>() {
                override val size: Int
                    get() = DexEntryIterator().getSize()

                override fun iterator(): MutableIterator<DexBackedDexFile> {
                    return TransformedIterator(
                        DexEntryIterator(),
                        Function<OatDexEntry?, DexBackedDexFile> { dexEntry ->
                            dexEntry!!.dexFile
                        }
                    )
                }
            }
        }

    @get:Throws(IOException::class)
    override val dexEntryNames: List<String>
        get() = object : AbstractForwardSequentialList<String>() {
            override val size: Int
                get() = DexEntryIterator().getSize()

            override fun iterator(): MutableIterator<String> {
                return TransformedIterator(
                    DexEntryIterator(),
                    Function<OatDexEntry?, String> { dexEntry -> dexEntry!!.entryName }
                )
            }
        }

    @Throws(IOException::class)
    override fun getEntry(entryName: String): OatDexEntry? {
        val iterator = DexEntryIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry != null && entry.entryName == entryName) {
                return entry
            }
        }
        return null
    }

    inner class OatDexFile(buf: ByteArray, offset: Int) : DexBackedDexFile(opcodes, buf, offset) {
        override fun supportsOptimizedOpcodes(): Boolean {
            return true
        }
    }

    inner class OatCDexFile(buf: ByteArray, offset: Int) : CDexBackedDexFile(opcodes, buf, offset) {
        override fun supportsOptimizedOpcodes(): Boolean {
            return true
        }
    }

    private inner class OatHeader(offset: Int) {
        @JvmField
        val headerOffset: Int = offset

        private val keyValueStoreOffset: Int

        init {
            val version = version
            keyValueStoreOffset = when {
                version >= 225 -> 17 * 4
                version >= 190 -> 16 * 4
                version >= 180 -> 15 * 4
                version >= 170 -> 14 * 4
                version >= 166 -> 16 * 4
                version >= 162 -> 17 * 4
                version >= 127 -> 19 * 4
                else -> 18 * 4
            }
        }

        val isValid: Boolean
            get() {
                for (i in OAT_MAGIC.indices) {
                    if (buf[headerOffset + i] != OAT_MAGIC[i]) {
                        return false
                    }
                }

                for (i in 4 until 7) {
                    if (buf[headerOffset + i] < '0'.code.toByte() ||
                        buf[headerOffset + i] > '9'.code.toByte()
                    ) {
                        return false
                    }
                }

                return buf[headerOffset + 7] == 0.toByte()
            }

        val version: Int
            get() = Integer.valueOf(String(buf, headerOffset + 4, 3))

        val dexFileCount: Int
            get() = readSmallUint(headerOffset + 20)

        val keyValueStoreSize: Int
            get() {
                if (version < MIN_OAT_VERSION) {
                    throw IllegalStateException("Unsupported oat version")
                }
                val fieldOffset = keyValueStoreOffset - 4
                return readSmallUint(headerOffset + fieldOffset)
            }

        val headerSize: Int
            get() {
                if (version < MIN_OAT_VERSION) {
                    throw IllegalStateException("Unsupported oat version")
                }
                return keyValueStoreOffset + keyValueStoreSize
            }

        fun getKeyValue(key: String): String? {
            val size = keyValueStoreSize

            var offset = headerOffset + keyValueStoreOffset
            val endOffset = offset + size

            while (offset < endOffset) {
                val keyStartOffset = offset
                while (offset < endOffset && buf[offset] != 0.toByte()) {
                    offset++
                }
                if (offset >= endOffset) {
                    throw InvalidOatFileException("Oat file contains truncated key value store")
                }
                val keyEndOffset = offset

                val k = String(buf, keyStartOffset, keyEndOffset - keyStartOffset)
                val valueStartOffset = ++offset
                while (offset < endOffset && buf[offset] != 0.toByte()) {
                    offset++
                }
                if (offset >= endOffset) {
                    throw InvalidOatFileException("Oat file contains truncated key value store")
                }

                if (k == key) {
                    return String(buf, valueStartOffset, offset - valueStartOffset)
                }

                offset++ // Move past the null terminator of the value
            }
            return null
        }

        val dexListStart: Int
            get() = if (version >= 127) {
                headerOffset + readSmallUint(headerOffset + (6 * 4))
            } else {
                headerOffset + headerSize
            }
    }

    private val sections: List<SectionHeader>
        get() {
            val offset: Int
            val entrySize: Int
            val entryCount: Int
            if (is64bit) {
                offset = readLongAsSmallUint(40)
                entrySize = readUshort(58)
                entryCount = readUshort(60)
            } else {
                offset = readSmallUint(32)
                entrySize = readUshort(46)
                entryCount = readUshort(48)
            }

            if (offset + (entrySize * entryCount) > buf.size) {
                throw InvalidOatFileException(
                    "The ELF section headers extend past the end of the file"
                )
            }

            return object : AbstractList<SectionHeader>() {
                override val size: Int
                    get() = entryCount

                override fun get(index: Int): SectionHeader {
                    if (index < 0 || index >= entryCount) {
                        throw IndexOutOfBoundsException()
                    }
                    return if (is64bit) {
                        SectionHeader64Bit(offset + (index * entrySize))
                    } else {
                        SectionHeader32Bit(offset + (index * entrySize))
                    }
                }
            }
        }

    private val symbolTable: SymbolTable
        get() {
            for (header in sections) {
                if (header.type == TYPE_DYNAMIC_SYMBOL_TABLE) {
                    return SymbolTable(header)
                }
            }
            throw InvalidOatFileException("Oat file has no symbol table")
        }

    private val sectionNameStringTable: StringTable
        get() {
            val index = readUshort(50)
            if (index == 0) {
                throw InvalidOatFileException("There is no section name string table")
            }

            try {
                return StringTable(sections[index])
            } catch (ex: IndexOutOfBoundsException) {
                throw InvalidOatFileException(
                    "The section index for the section name string table is invalid"
                )
            }
        }

    abstract inner class SectionHeader(@JvmField protected val headerOffset: Int) {
        abstract val address: Long
        abstract val offset: Int
        abstract val size: Int
        abstract val link: Int
        abstract val entrySize: Int

        val name: String
            get() = sectionNameStringTable.getString(readSmallUint(headerOffset))

        val type: Int
            get() = readInt(headerOffset + 4)
    }

    private inner class SectionHeader32Bit(headerOffset: Int) : SectionHeader(headerOffset) {
        override val address: Long
            get() = readInt(headerOffset + 12).toLong() and 0xFFFFFFFFL

        override val offset: Int
            get() = readSmallUint(headerOffset + 16)

        override val size: Int
            get() = readSmallUint(headerOffset + 20)

        override val link: Int
            get() = readSmallUint(headerOffset + 24)

        override val entrySize: Int
            get() = readSmallUint(headerOffset + 36)
    }

    private inner class SectionHeader64Bit(headerOffset: Int) : SectionHeader(headerOffset) {
        override val address: Long
            get() = readLong(headerOffset + 16)

        override val offset: Int
            get() = readLongAsSmallUint(headerOffset + 24)

        override val size: Int
            get() = readLongAsSmallUint(headerOffset + 32)

        override val link: Int
            get() = readSmallUint(headerOffset + 40)

        override val entrySize: Int
            get() = readLongAsSmallUint(headerOffset + 56)
    }

    inner class SymbolTable(header: SectionHeader) {
        private val stringTable: StringTable
        private val offset: Int
        private val entryCount: Int
        private val entrySize: Int

        init {
            try {
                this.stringTable = StringTable(sections[header.link])
            } catch (ex: IndexOutOfBoundsException) {
                throw InvalidOatFileException("String table section index is invalid")
            }
            this.offset = header.offset
            this.entrySize = header.entrySize
            this.entryCount = header.size / entrySize

            if (offset + entryCount * entrySize > buf.size) {
                throw InvalidOatFileException("Symbol table extends past end of file")
            }
        }

        val symbols: List<Symbol>
            get() = object : AbstractList<Symbol>() {
                override val size: Int
                    get() = entryCount

                override fun get(index: Int): Symbol {
                    if (index < 0 || index >= entryCount) {
                        throw IndexOutOfBoundsException()
                    }
                    return if (is64bit) {
                        Symbol64(offset + index * entrySize)
                    } else {
                        Symbol32(offset + index * entrySize)
                    }
                }
            }

        abstract inner class Symbol(offset: Int) {
            protected val symbolOffset: Int = offset

            abstract val name: String
            abstract val value: Long
            abstract val symbolSize: Int
            abstract val sectionIndex: Int

            val fileOffset: Int
                get() {
                    val sectionHeader: SectionHeader
                    try {
                        sectionHeader = sections[sectionIndex]
                    } catch (ex: IndexOutOfBoundsException) {
                        throw InvalidOatFileException(
                            "Section index for symbol is out of bounds"
                        )
                    }

                    val sectionAddress = sectionHeader.address
                    val sectionOffset = sectionHeader.offset
                    val sectionSize = sectionHeader.size

                    val symbolAddress = value

                    if (symbolAddress < sectionAddress ||
                        symbolAddress >= sectionAddress + sectionSize
                    ) {
                        throw InvalidOatFileException(
                            "symbol address lies outside it's associated section"
                        )
                    }

                    val fileOffset = (sectionOffset + (value - sectionAddress))
                    assert(fileOffset <= Int.MAX_VALUE)
                    return fileOffset.toInt()
                }
        }

        inner class Symbol32(offset: Int) : Symbol(offset) {
            override val name: String
                get() = stringTable.getString(readSmallUint(symbolOffset))

            override val value: Long
                get() = readSmallUint(symbolOffset + 4).toLong()

            override val symbolSize: Int
                get() = readSmallUint(symbolOffset + 8)

            override val sectionIndex: Int
                get() = readUshort(symbolOffset + 14)
        }

        inner class Symbol64(offset: Int) : Symbol(offset) {
            override val name: String
                get() = stringTable.getString(readSmallUint(symbolOffset))

            override val value: Long
                get() = readLong(symbolOffset + 8)

            override val symbolSize: Int
                get() = readLongAsSmallUint(symbolOffset + 16)

            override val sectionIndex: Int
                get() = readUshort(symbolOffset + 6)
        }
    }

    private inner class StringTable(header: SectionHeader) {
        private val offset: Int = header.offset
        private val size: Int = header.size

        init {
            if (offset + size > buf.size) {
                throw InvalidOatFileException("String table extends past end of file")
            }
        }

        fun getString(index: Int): String {
            if (index >= size) {
                throw InvalidOatFileException("String index is out of bounds")
            }

            val start = offset + index
            var end = start
            while (buf[end] != 0.toByte()) {
                end++
                if (end >= offset + size) {
                    throw InvalidOatFileException("String extends past end of string table")
                }
            }

            return String(buf, start, end - start, Charset.forName("US-ASCII"))
        }
    }

    inner class OatDexEntry(
        override val entryName: String,
        private val dexBuf: ByteArray,
        private val dexOffset: Int
    ) : MultiDexContainer.DexEntry<DexBackedDexFile> {
        override val dexFile: DexBackedDexFile
            get() {
                if (CDexBackedDexFile.isCdex(dexBuf, dexOffset)) {
                    return OatCDexFile(dexBuf, dexOffset)
                } else {
                    try {
                        DexUtil.verifyDexHeader(dexBuf, dexOffset)
                    } catch (ex: DexBackedDexFile.NotADexFile) {
                        if (oatVersion >= 87) {
                            throw DexFileNotFoundException(
                                ex,
                                "Could not locate the embedded dex file %s. Is the vdex file missing?",
                                entryName
                            )
                        } else {
                            throw DexFileNotFoundException(
                                ex,
                                "The embedded dex file %s does not appear to be a valid dex file.",
                                entryName
                            )
                        }
                    }
                    return OatDexFile(dexBuf, dexOffset)
                }
            }

        override val container: MultiDexContainer<out DexBackedDexFile>
            get() = this@OatFile
    }

    private inner class DexEntryIterator : MutableIterator<OatDexEntry?> {
        var index = 0
        var offset = oatHeader.dexListStart

        override fun hasNext(): Boolean {
            return index < oatHeader.dexFileCount
        }

        override fun next(): OatDexEntry? {
            while (hasNext()) {
                val filenameLength = readSmallUint(offset)
                offset += 4

                // TODO: what is the correct character encoding?
                val filename = String(buf, offset, filenameLength, Charset.forName("US-ASCII"))
                offset += filenameLength

                if (oatVersion >= 233) {
                    offset += 8 // Dex File magic
                }

                offset += 4 // checksum

                if (oatVersion >= 232) {
                    offset += 20 // Dex File SHA1
                }

                var dexOffset = readSmallUint(offset)
                offset += 4

                val dexBuf: ByteArray
                if (oatVersion >= 87 && vdexProvider != null && vdexProvider.getVdex() != null) {
                    dexBuf = vdexProvider.getVdex()!!
                } else {
                    dexBuf = buf
                    dexOffset += oatHeader.headerOffset
                }

                if (oatVersion >= 75) {
                    offset += 4 // offset to class offsets table
                }
                if (oatVersion >= 73) {
                    offset += 4 // lookup table offset
                }
                if (oatVersion >= 131) {
                    offset += 4 // dex sections layout offset
                }
                if (oatVersion >= 127) {
                    offset += 4 // method bss mapping offset
                }
                if (oatVersion >= 135) {
                    offset += 8 // type bss mapping and string bss mapping offsets
                }
                if (oatVersion >= 186) {
                    offset += 4 // public_type_bss_mapping_offset_
                    offset += 4 // package_type_bss_mapping_offset_
                }
                if (oatVersion >= 241) {
                    offset += 4 // method_type_bss_mapping_offset_
                }
                if (oatVersion < 75) {
                    // prior to 75, the class offsets are included here directly
                    val classCount = readSmallUint(dexOffset + HeaderItem.CLASS_COUNT_OFFSET)
                    offset += 4 * classCount
                }

                index++

                if (oatVersion >= 138 && dexOffset == 0) {
                    // An offset of 0 indicates that the dex file remains in the apk. So we treat it as not a part of
                    // the oat file.
                    continue
                }
                return OatDexEntry(filename, dexBuf, dexOffset)
            }
            return null
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }

        /**
         * Returns the number of elements remaining in `iterator`. The iterator will be left
         * exhausted: its `hasNext()` method will return `false`.
         */
        fun getSize(): Int {
            var count = 0
            while (hasNext()) {
                if (next() != null) {
                    count++
                }
            }
            return count
        }
    }

    class InvalidOatFileException(message: String) : RuntimeException(message)

    class NotAnOatFileException : RuntimeException()

    interface VdexProvider {
        fun getVdex(): ByteArray?
    }

    companion object {
        private val ELF_MAGIC = byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
        private val OAT_MAGIC = byteArrayOf('o'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), '\n'.code.toByte())
        private const val MIN_ELF_HEADER_SIZE = 52

        // These are the "known working" versions that I have manually inspected the source for.
        // Later version may or may not work, depending on what changed.
        private const val MIN_OAT_VERSION = 56
        private const val MAX_OAT_VERSION = 254

        private const val TYPE_DYNAMIC_SYMBOL_TABLE = 11

        const val UNSUPPORTED = 0
        const val SUPPORTED = 1
        const val UNKNOWN = 2

        @JvmStatic
        private fun verifyMagic(buf: ByteArray) {
            for (i in ELF_MAGIC.indices) {
                if (buf[i] != ELF_MAGIC[i]) {
                    throw NotAnOatFileException()
                }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun fromInputStream(`is`: InputStream): OatFile {
            return fromInputStream(`is`, null)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun fromInputStream(`is`: InputStream, vdexProvider: VdexProvider?): OatFile {
            if (!`is`.markSupported()) {
                throw IllegalArgumentException("InputStream must support mark")
            }
            `is`.mark(4)
            val partialHeader = ByteArray(4)
            try {
                InputStreamUtil.readFully(`is`, partialHeader)
            } catch (ex: EOFException) {
                throw NotAnOatFileException()
            } finally {
                `is`.reset()
            }

            verifyMagic(partialHeader)

            `is`.reset()

            val buf = InputStreamUtil.toByteArray(`is`)
            return OatFile(buf, vdexProvider)
        }
    }
}
