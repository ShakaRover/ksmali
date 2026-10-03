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
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile.NotADexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.util.DexUtil
import com.android.tools.smali.dexlib2.util.DexUtil.InvalidFile
import com.android.tools.smali.dexlib2.util.DexUtil.UnsupportedFile
import com.android.tools.smali.util.InputStreamUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Represents a zip file that contains dex files (i.e. an apk or jar file)
 */
class ZipDexContainer @JvmOverloads constructor(
    private val zipFilePath: File,
    private val opcodes: Opcodes?,
    private val ignoreInvalid: Boolean = true
) : MultiDexContainer<DexBackedDexFile> {

    /**
     * Represents a dex entry in a zip container, including the uncompressed CRC-32 checksum
     * of the zip entry.
     */
    interface ZipDexEntry : MultiDexContainer.DexEntry<DexBackedDexFile> {
        /**
         * Gets the uncompressed CRC-32 checksum of the zip entry containing this dex file.
         *
         * @return The uncompressed CRC-32 checksum of the zip entry
         */
        val crc: Long
    }

    private var entries: MutableMap<String, ZipDexEntry>? = null

    /**
     * Gets a list of the names of dex files in this zip file.
     *
     * The returned list follows a strict deterministic order: it starts with all dex files in
     * classes.dex, then classes2.dex, etc. sequentially until classesN.dex is missing, followed by
     * any remaining dex entries in lexicographically sorted order.
     *
     * @return A list of the names of dex files in this zip file
     */
    override val dexEntryNames: List<String>
        @Throws(IOException::class)
        get() = ArrayList(getEntries().keys)

    private fun getEntries(): Map<String, ZipDexEntry> {
        entries?.let { return it }
        // LinkedHashMap is used to preserve the strict deterministic insertion ordering of entries.
        val localEntries = LinkedHashMap<String, ZipDexEntry>()
        entries = localEntries
        getZipFile().use { zipFile ->
            val validZipEntries = HashMap<String, ZipEntry>()
            val entriesEnumeration = zipFile.entries()

            while (entriesEnumeration.hasMoreElements()) {
                val entry = entriesEnumeration.nextElement()
                if (isDex(zipFile, entry)) {
                    validZipEntries[entry.name] = entry
                }
            }

            var n = 1
            while (true) {
                val targetName = if (n == 1) "classes.dex" else "classes$n.dex"
                val entry = validZipEntries.remove(targetName) ?: break
                loadDexFilesFromZipEntry(zipFile, entry)
                n++
            }

            val remainingNames = ArrayList(validZipEntries.keys)
            Collections.sort(remainingNames)
            for (remainingName in remainingNames) {
                val entry = validZipEntries[remainingName]!!
                loadDexFilesFromZipEntry(zipFile, entry)
            }

            return localEntries
        }
    }

    @Throws(IOException::class)
    private fun loadDexFilesFromZipEntry(zipFile: ZipFile, entry: ZipEntry) {
        val entryCrc = entry.crc
        zipFile.getInputStream(entry).use { inputStream ->
            val buf = InputStreamUtil.toByteArray(inputStream)
            var offset = 0
            var i = 0
            while (offset < buf.size) {
                val headerOffset = offset
                var isLast = false
                var fileSize = 0
                try {
                    val dex = DexBackedDexFile(opcodes, buf, 0, true, headerOffset)
                    isLast = dex.isDexContainerLastEntry()
                    fileSize = dex.getFileSize()
                } catch (ex: NotADexFile) {
                    if (ignoreInvalid) return
                    isLast = true
                    fileSize = buf.size - offset
                } catch (ex: InvalidFile) {
                    if (ignoreInvalid) return
                    isLast = true
                    fileSize = buf.size - offset
                } catch (ex: UnsupportedFile) {
                    if (ignoreInvalid) return
                    isLast = true
                    fileSize = buf.size - offset
                }
                val entryName = entry.name + (if (i == 0 && isLast) "" else "/$i")
                val dexEntry = object : ZipDexEntry {
                    override val entryName: String
                        get() = entryName

                    // throws InvalidFile if the dex file has an invalid header.
                    // throws UnsupportedFile if the dex file version is not supported.
                    override val dexFile: DexBackedDexFile
                        get() = DexBackedDexFile(opcodes, buf, 0, true, headerOffset)

                    override val container: MultiDexContainer<out DexBackedDexFile>
                        get() = this@ZipDexContainer

                    override val crc: Long
                        get() = entryCrc
                }
                entries!![entryName] = dexEntry
                offset += fileSize
                if (isLast) {
                    return
                }
                i++
            }
        }
    }

    /**
     * Loads a dex file from a specific named entry.
     *
     * @param entryName The name of the entry
     * @return A DexEntry, or null if there is no entry with the given name
     * @throws NotADexFile If the entry isn't a dex file
     */
    @Throws(IOException::class)
    override fun getEntry(entryName: String): ZipDexEntry? {
        return getEntries()[entryName]
    }

    val isZipFile: Boolean
        get() {
            try {
                getZipFile().use {
                    return true
                }
            } catch (ex: IOException) {
                return false
            } catch (ex: NotAZipFileException) {
                return false
            }
        }

    @Throws(IOException::class)
    protected fun isDex(zipFile: ZipFile, zipEntry: ZipEntry): Boolean {
        try {
            BufferedInputStream(zipFile.getInputStream(zipEntry)).use { inputStream ->
                DexUtil.verifyDexHeader(inputStream)
            }
        } catch (ex: NotADexFile) {
            return false
        } catch (ex: InvalidFile) {
            return false
        } catch (ex: UnsupportedFile) {
            return false
        }
        return true
    }

    @Throws(IOException::class)
    protected fun getZipFile(): ZipFile {
        try {
            return ZipFile(zipFilePath)
        } catch (ex: IOException) {
            throw NotAZipFileException()
        }
    }

    class NotAZipFileException : RuntimeException()
}
