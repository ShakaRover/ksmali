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

package com.android.tools.smali.dexlib2

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile.NotADexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile
import com.android.tools.smali.dexlib2.dexbacked.OatFile
import com.android.tools.smali.dexlib2.dexbacked.OatFile.NotAnOatFileException
import com.android.tools.smali.dexlib2.dexbacked.OatFile.VdexProvider
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer.NotAZipFileException
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.iface.MultiDexContainer.DexEntry
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.InputStreamUtil
import com.android.tools.smali.util.StringUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Collections

object DexFileFactory {

    @Throws(IOException::class)
    fun loadDexFile(path: String, opcodes: Opcodes?): DexBackedDexFile {
        return loadDexFile(File(path), opcodes)
    }

    /**
     * Loads a dex/apk/odex/oat file.
     *
     * For oat files with multiple dex files, the first will be opened. For zip/apk files, the "classes.dex" entry
     * will be opened.
     *
     * @param file The file to open
     * @param opcodes The set of opcodes to use
     * @return A DexBackedDexFile for the given file
     *
     * @throws UnsupportedOatVersionException If file refers to an unsupported oat file
     * @throws DexFileNotFoundException If file does not exist, if file is a zip file but does not have a "classes.dex"
     * entry, or if file is an oat file that has no dex entries.
     * @throws UnsupportedFileTypeException If file is not a valid dex/zip/odex/oat file, or if the "classes.dex" entry
     * in a zip file is not a valid dex file
     */
    @Throws(IOException::class)
    fun loadDexFile(file: File, opcodes: Opcodes?): DexBackedDexFile {
        if (!file.exists()) {
            throw DexFileNotFoundException("%s does not exist", file.name)
        }

        try {
            val container = ZipDexContainer(file, opcodes)
            return DexEntryFinder(file.path, container).findEntry("classes.dex", true).dexFile
        } catch (ex: NotAZipFileException) {
            // eat it and continue
        }

        BufferedInputStream(FileInputStream(file)).use { inputStream ->
            try {
                return DexBackedDexFile.fromInputStream(opcodes, inputStream)
            } catch (ex: NotADexFile) {
                // just eat it
            }

            try {
                return DexBackedOdexFile.fromInputStream(opcodes!!, inputStream)
            } catch (ex: DexBackedOdexFile.NotAnOdexFile) {
                // just eat it
            }

            // Note: DexBackedDexFile.fromInputStream and DexBackedOdexFile.fromInputStream will reset inputStream
            // back to the same position, if they fails

            var oatFile: OatFile? = null
            try {
                oatFile = OatFile.fromInputStream(inputStream, FilenameVdexProvider(file))
            } catch (ex: NotAnOatFileException) {
                // just eat it
            }

            if (oatFile != null) {
                if (oatFile.isSupportedVersion == OatFile.UNSUPPORTED) {
                    throw UnsupportedOatVersionException(oatFile)
                }

                val oatDexFiles: List<DexBackedDexFile> = oatFile.dexFiles

                if (oatDexFiles.isEmpty()) {
                    throw DexFileNotFoundException("Oat file %s contains no dex files", file.name)
                }

                return oatDexFiles[0]
            }
        }

        throw UnsupportedFileTypeException("%s is not an apk, dex, odex or oat file.", file.path)
    }

    /**
     * Loads a dex entry from a container format (zip/oat)
     *
     * This has two modes of operation, depending on the exactMatch parameter. When exactMatch is true, it will only
     * load an entry whose name exactly matches that provided by the dexEntry parameter.
     *
     * When exactMatch is false, then it will search for any entry that dexEntry is a path suffix of. "path suffix"
     * meaning all the path components in dexEntry must fully match the corresponding path components in the entry name,
     * but some path components at the beginning of entry name can be missing.
     *
     * For example, if an oat file contains a "/system/framework/framework.jar:classes2.dex" entry, then the following
     * will match (not an exhaustive list):
     *
     * "/system/framework/framework.jar:classes2.dex"
     * "system/framework/framework.jar:classes2.dex"
     * "framework/framework.jar:classes2.dex"
     * "framework.jar:classes2.dex"
     * "classes2.dex"
     *
     * Note that partial path components specifically don't match. So something like "work/framework.jar:classes2.dex"
     * would not match.
     *
     * If dexEntry contains an initial slash, it will be ignored for purposes of this suffix match -- but not when
     * performing an exact match.
     *
     * If multiple entries match the given dexEntry, a MultipleMatchingDexEntriesException will be thrown
     *
     * @param file The container file. This must be either a zip (apk) file or an oat file.
     * @param dexEntry The name of the entry to load. This can either be the exact entry name, if exactMatch is true,
     *                 or it can be a path suffix.
     * @param exactMatch If true, dexE
     * @param opcodes The set of opcodes to use
     * @return A DexBackedDexFile for the given entry
     *
     * @throws UnsupportedOatVersionException If file refers to an unsupported oat file
     * @throws DexFileNotFoundException If the file does not exist, or if no matching entry could be found
     * @throws UnsupportedFileTypeException If file is not a valid zip/oat file, or if the matching entry is not a
     * valid dex file
     * @throws MultipleMatchingDexEntriesException If multiple entries match the given dexEntry
     */
    @Throws(IOException::class)
    fun loadDexEntry(
        file: File,
        dexEntry: String,
        exactMatch: Boolean,
        opcodes: Opcodes?
    ): DexEntry<out DexBackedDexFile> {
        if (!file.exists()) {
            throw DexFileNotFoundException("Container file %s does not exist", file.name)
        }

        try {
            val container = ZipDexContainer(file, opcodes)
            return DexEntryFinder(file.path, container).findEntry(dexEntry, exactMatch)
        } catch (ex: NotAZipFileException) {
            // eat it and continue
        }

        BufferedInputStream(FileInputStream(file)).use { inputStream ->
            var oatFile: OatFile? = null
            try {
                oatFile = OatFile.fromInputStream(inputStream, FilenameVdexProvider(file))
            } catch (ex: NotAnOatFileException) {
                // just eat it
            }

            if (oatFile != null) {
                if (oatFile.isSupportedVersion == OatFile.UNSUPPORTED) {
                    throw UnsupportedOatVersionException(oatFile)
                }

                val oatDexFiles: List<DexFile> = oatFile.dexFiles

                if (oatDexFiles.isEmpty()) {
                    throw DexFileNotFoundException("Oat file %s contains no dex files", file.name)
                }

                return DexEntryFinder(file.path, oatFile).findEntry(dexEntry, exactMatch)
            }
        }

        throw UnsupportedFileTypeException("%s is not an apk or oat file.", file.path)
    }

    /**
     * Loads a file containing 1 or more dex files
     *
     * If the given file is a dex or odex file, it will return a MultiDexContainer containing that single entry.
     * Otherwise, for an oat or zip file, it will return an OatFile or ZipDexContainer respectively.
     *
     * @param file The file to open
     * @param opcodes The set of opcodes to use
     * @return A MultiDexContainer
     * @throws DexFileNotFoundException If the given file does not exist
     * @throws UnsupportedFileTypeException If the given file is not a valid dex/zip/odex/oat file
     */
    @Throws(IOException::class)
    fun loadDexContainer(file: File, opcodes: Opcodes?): MultiDexContainer<out DexBackedDexFile> {
        if (!file.exists()) {
            throw DexFileNotFoundException("%s does not exist", file.name)
        }

        val zipDexContainer = ZipDexContainer(file, opcodes)
        if (zipDexContainer.isZipFile) {
            return zipDexContainer
        }

        BufferedInputStream(FileInputStream(file)).use { inputStream ->
            try {
                val dexFile = DexBackedDexFile.fromInputStream(opcodes, inputStream)
                return SingletonMultiDexContainer(file.path, dexFile)
            } catch (ex: NotADexFile) {
                // just eat it
            }

            try {
                val odexFile = DexBackedOdexFile.fromInputStream(opcodes!!, inputStream)
                return SingletonMultiDexContainer(file.path, odexFile)
            } catch (ex: DexBackedOdexFile.NotAnOdexFile) {
                // just eat it
            }

            // Note: DexBackedDexFile.fromInputStream and DexBackedOdexFile.fromInputStream will reset inputStream
            // back to the same position, if they fails

            var oatFile: OatFile? = null
            try {
                oatFile = OatFile.fromInputStream(inputStream, FilenameVdexProvider(file))
            } catch (ex: NotAnOatFileException) {
                // just eat it
            }

            if (oatFile != null) {
                // TODO: support loading earlier oat files here, even though they cannot be deodexed.
                if (oatFile.isSupportedVersion == OatFile.UNSUPPORTED) {
                    throw UnsupportedOatVersionException(oatFile)
                }
                return oatFile
            }
        }

        throw UnsupportedFileTypeException("%s is not an apk, dex, odex or oat file.", file.path)
    }

    /**
     * Writes a DexFile out to disk
     *
     * @param path The path to write the dex file to
     * @param dexFile a DexFile to write
     */
    @Throws(IOException::class)
    fun writeDexFile(path: String, dexFile: DexFile) {
        DexPool.writeTo(path, dexFile)
    }

    class DexFileNotFoundException : ExceptionWithContext {
        constructor(message: String?, vararg formatArgs: Any?) : super(message, *formatArgs)

        constructor(cause: Throwable, message: String?, vararg formatArgs: Any?) :
            super(cause, message, *formatArgs)
    }

    class UnsupportedOatVersionException(val oatFile: OatFile) :
        ExceptionWithContext("Unsupported oat version: %d", oatFile.oatVersion)

    class MultipleMatchingDexEntriesException(message: String, vararg formatArgs: Any?) :
        ExceptionWithContext(String.format(message, *formatArgs))

    class UnsupportedFileTypeException(message: String, vararg formatArgs: Any?) :
        ExceptionWithContext(String.format(message, *formatArgs))

    /**
     * Matches two entries fully, ignoring any initial slash, if any
     */
    private fun fullEntryMatch(entry0: String, targetEntry0: String): Boolean {
        var entry = entry0
        var targetEntry = targetEntry0
        if (entry == targetEntry) {
            return true
        }

        if (entry[0] == '/') {
            entry = entry.substring(1)
        }

        if (targetEntry[0] == '/') {
            targetEntry = targetEntry.substring(1)
        }

        return entry == targetEntry
    }

    /**
     * Performs a partial match against entry and targetEntry.
     *
     * This is considered a partial match if targetEntry is a suffix of entry, and if the suffix starts
     * on a path "part" (ignoring the initial separator, if any). '/' and ':' and '!' are considered separators for
     * this.
     *
     * So entry="/blah/blah/something.dex" and targetEntry="lah/something.dex" shouldn't match, but
     * both targetEntry="blah/something.dex" and "/blah/something.dex" should match.
     */
    private fun partialEntryMatch(entry: String, targetEntry: String): Boolean {
        if (entry == targetEntry) {
            return true
        }

        if (!entry.endsWith(targetEntry)) {
            return false
        }

        // Make sure the first matching part is a full entry. We don't want to match "/blah/blah/something.dex" with
        // "lah/something.dex", but both "/blah/something.dex" and "blah/something.dex" should match
        val precedingChar = entry[entry.length - targetEntry.length - 1]
        val firstTargetChar = targetEntry[0]
        // This is a device path, so we should always use the linux separator '/', rather than the current platform's
        // separator
        return firstTargetChar == ':' || firstTargetChar == '/' || firstTargetChar == '!' ||
                precedingChar == ':' || precedingChar == '/' || precedingChar == '!'
    }

    class DexEntryFinder(
        private val filename: String,
        private val dexContainer: MultiDexContainer<out DexBackedDexFile>
    ) {
        @Throws(IOException::class)
        fun findEntry(
            targetEntry: String, exactMatch: Boolean
        ): DexEntry<out DexBackedDexFile> {
            if (exactMatch) {
                try {
                    val entry = dexContainer.getEntry(targetEntry)
                        ?: throw DexFileNotFoundException(
                            "Could not find entry %s in %s.", targetEntry, filename
                        )
                    return entry
                } catch (ex: NotADexFile) {
                    throw UnsupportedFileTypeException(
                        "Entry %s in %s is not a dex file", targetEntry, filename
                    )
                }
            }

            // find all full and partial matches
            val fullMatches = ArrayList<String>()
            val fullEntries = ArrayList<DexEntry<out DexBackedDexFile>>()
            val partialMatches = ArrayList<String>()
            val partialEntries = ArrayList<DexEntry<out DexBackedDexFile>>()
            for (entry in dexContainer.dexEntryNames) {
                if (fullEntryMatch(entry, targetEntry)) {
                    // We want to grab all full matches, regardless of whether they're actually a dex file.
                    fullMatches.add(entry)
                    val dexEntry = dexContainer.getEntry(entry)!!
                    fullEntries.add(dexEntry)
                } else if (partialEntryMatch(entry, targetEntry)) {
                    partialMatches.add(entry)
                    val dexEntry = dexContainer.getEntry(entry)!!
                    partialEntries.add(dexEntry)
                }
            }

            // full matches always take priority
            if (fullEntries.size == 1) {
                try {
                    val dexEntry = fullEntries[0]
                    return dexEntry
                } catch (ex: NotADexFile) {
                    throw UnsupportedFileTypeException(
                        "Entry %s in %s is not a dex file",
                        fullMatches[0], filename
                    )
                }
            }
            if (fullEntries.size > 1) {
                // This should be quite rare. This would only happen if an oat file has two entries that differ
                // only by an initial path separator. e.g. "/blah/blah.dex" and "blah/blah.dex"
                throw MultipleMatchingDexEntriesException(
                    "Multiple entries in ${filename} match ${targetEntry}: ${StringUtils.join(fullMatches, ", ")}"
                )
            }

            if (partialEntries.isEmpty()) {
                throw DexFileNotFoundException(
                    "Could not find a dex entry in %s matching %s",
                    filename, targetEntry
                )
            }
            if (partialEntries.size > 1) {
                throw MultipleMatchingDexEntriesException(
                    "Multiple dex entries in ${filename} match ${targetEntry}: ${StringUtils.join(partialMatches, ", ")}"
                )
            }
            return partialEntries[0]
        }
    }

    private class SingletonMultiDexContainer(
        private val entryName: String,
        private val dexFile: DexBackedDexFile
    ) : MultiDexContainer<DexBackedDexFile> {

        override val dexEntryNames: List<String>
            get() = Collections.singletonList(entryName)

        override fun getEntry(entryName: String): DexEntry<DexBackedDexFile>? {
            if (entryName == this.entryName) {
                val name = entryName
                return object : DexEntry<DexBackedDexFile> {
                    override val entryName: String
                        get() = name

                    override val dexFile: DexBackedDexFile
                        get() = this@SingletonMultiDexContainer.dexFile

                    override val container: MultiDexContainer<out DexBackedDexFile>
                        get() = this@SingletonMultiDexContainer
                }
            }
            return null
        }
    }

    class FilenameVdexProvider(oatFile: File) : VdexProvider {
        private val vdexFile: File

        private var buf: ByteArray? = null
        private var loadedVdex = false

        init {
            val oatParent = oatFile.absoluteFile.parentFile
            val baseName = getNameWithoutExtension(oatFile.absolutePath)
            vdexFile = File(oatParent, baseName + ".vdex")
        }

        override fun getVdex(): ByteArray? {
            if (!loadedVdex) {
                var candidateFile = vdexFile

                if (!candidateFile.exists()) {
                    // On api 28, for framework files, the vdex file in the architecture-specific directory is just a
                    // symlink to a common vdex file in the framework directory. When loop-mounting a system image, that
                    // symlink won't resolve because it uses an absolute path. As a workaround, we'll just search upward
                    // one directory to see if it's there.
                    val parentDirectory = candidateFile.parentFile.parentFile
                    if (parentDirectory != null) {
                        candidateFile = File(parentDirectory, vdexFile.name)
                    }
                }

                if (candidateFile.exists()) {
                    try {
                        buf = InputStreamUtil.toByteArray(FileInputStream(candidateFile))
                    } catch (e: FileNotFoundException) {
                        buf = null
                    } catch (ex: IOException) {
                        throw RuntimeException(ex)
                    }
                }
                loadedVdex = true
            }

            return buf
        }

        companion object {
            fun getNameWithoutExtension(file: String): String {
                val fileName = File(file).name
                val dotIndex = fileName.lastIndexOf('.')
                return if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
            }
        }
    }
}
