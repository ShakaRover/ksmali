/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.util

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.HashMap
import java.util.regex.Pattern

/**
 * This class handles the complexities of translating a class name into a file name. i.e. dealing with case insensitive
 * file systems, windows reserved filenames, class names with extremely long package/class elements, etc.
 *
 * The types of transformations this class does include:
 * - append a '#123' style numeric suffix if 2 physical representations collide
 * - replace some number of characters in the middle with a '#' character name if an individual path element is too long
 * - append a '#' if an individual path element would otherwise be considered a reserved filename
 */
class ClassFileNameHandler {
    private val NO_VALUE = -1
    private val CASE_INSENSITIVE = 0
    private val CASE_SENSITIVE = 1
    private var forcedCaseSensitivity = NO_VALUE

    private var top: DirectoryEntry
    private var fileExtension: String
    private var modifyWindowsReservedFilenames: Boolean

    constructor(path: File, fileExtension: String) {
        this.top = DirectoryEntry(path)
        this.fileExtension = fileExtension
        this.modifyWindowsReservedFilenames = isWindows()
    }

    // for testing
    constructor(path: File, fileExtension: String, caseSensitive: Boolean,
                modifyWindowsReservedFilenames: Boolean) {
        this.top = DirectoryEntry(path)
        this.fileExtension = fileExtension
        this.forcedCaseSensitivity = if (caseSensitive) CASE_SENSITIVE else CASE_INSENSITIVE
        this.modifyWindowsReservedFilenames = modifyWindowsReservedFilenames
    }

    private fun getMaxFilenameLength(): Int {
        return MAX_FILENAME_LENGTH - NUMERIC_SUFFIX_RESERVE
    }

    @Throws(IOException::class)
    fun getUniqueFilenameForClass(className: String): File {
        //class names should be passed in the normal dalvik style, with a leading L, a trailing ;, and using
        //'/' as a separator.
        if (className[0] != 'L' || className[className.length - 1] != ';') {
            throw RuntimeException("Not a valid dalvik class name")
        }

        var packageElementCount = 1
        for (i in 1 until className.length - 1) {
            if (className[i] == '/') {
                packageElementCount++
            }
        }

        val packageElements = arrayOfNulls<String>(packageElementCount)
        var elementIndex = 0
        var elementStart = 1
        var i = 1
        while (i < className.length - 1) {
            if (className[i] == '/') {
                //if the first char after the initial L is a '/', or if there are
                //two consecutive '/'
                if (i - elementStart == 0) {
                    throw RuntimeException("Not a valid dalvik class name")
                }

                packageElements[elementIndex++] = className.substring(elementStart, i)
                elementStart = ++i
                i++
            } else {
                i++
            }
        }

        //at this point, we have added all the package elements to packageElements, but still need to add
        //the final class name. elementStart should point to the beginning of the class name

        //this will be true if the class ends in a '/', i.e. Lsome/package/className/;
        if (elementStart >= className.length - 1) {
            throw RuntimeException("Not a valid dalvik class name")
        }

        packageElements[elementIndex] = className.substring(elementStart, className.length - 1)

        return addUniqueChild(top, packageElements, 0)
    }

    @Throws(IOException::class)
    private fun addUniqueChild(parent: DirectoryEntry, packageElements: Array<String?>,
                               packageElementIndex: Int): File {
        if (packageElementIndex == packageElements.size - 1) {
            val fileEntry = FileEntry(parent, packageElements[packageElementIndex]!! + fileExtension)
            parent.addChild(fileEntry)

            val physicalName = fileEntry.physicalName

            // the physical name should be set when adding it as a child to the parent
            assert(physicalName != null)

            return File(parent.file, physicalName!!)
        } else {
            var directoryEntry = DirectoryEntry(parent, packageElements[packageElementIndex]!!)
            directoryEntry = parent.addChild(directoryEntry) as DirectoryEntry
            return addUniqueChild(directoryEntry, packageElements, packageElementIndex + 1)
        }
    }

    private abstract inner class FileSystemEntry(
        val parent: DirectoryEntry?,
        val logicalName: String
    ) {
        var physicalName: String? = null

        fun getNormalizedName(preserveCase: Boolean): String {
            var elementName = logicalName
            val parent = this.parent
            if (!preserveCase && parent != null && !parent.isCaseSensitive()) {
                elementName = elementName.toLowerCase()
            }

            if (modifyWindowsReservedFilenames && isReservedFileName(elementName)) {
                elementName = addSuffixBeforeExtension(elementName, "#")
            }

            val elementUtf8Length = utf8Length(elementName)
            if (elementUtf8Length > getMaxFilenameLength()) {
                elementName = shortenPathComponent(elementName, elementUtf8Length - getMaxFilenameLength())
            }
            return elementName
        }

        @Throws(IOException::class)
        fun setSuffix(suffix: Int) {
            if (suffix < 0 || suffix > 99999) {
                throw IllegalArgumentException("suffix must be in [0, 100000)")
            }

            if (this.physicalName != null) {
                throw IllegalStateException("The suffix can only be set once")
            }
            val physicalName = getPhysicalNameWithSuffix(suffix)
            val file = File(parent!!.file, physicalName).canonicalFile
            this.physicalName = file.name
            createIfNeeded()
        }

        /**
         * Actually create the (empty) file or directory, if it doesn't exist.
         */
        @Throws(IOException::class)
        protected abstract fun createIfNeeded()

        abstract fun getPhysicalNameWithSuffix(suffix: Int): String
    }

    private inner class DirectoryEntry : FileSystemEntry {
        var file: File? = null
        private var caseSensitivity = forcedCaseSensitivity

        // maps a normalized (but not suffixed) entry name to 1 or more FileSystemEntries.
        // Each FileSystemEntry associated with a normalized entry name must have a distinct
        // physical name
        private val children: Multimap<String, FileSystemEntry> = ArrayListMultimap.create()
        private val physicalToEntry: MutableMap<String, FileSystemEntry> = HashMap()
        private val lastSuffixMap: MutableMap<String, Int> = HashMap()

        constructor(path: File) : super(null, path.name) {
            file = path
            physicalName = path.name
        }

        constructor(parent: DirectoryEntry?, logicalName: String) : super(parent, logicalName)

        @Synchronized
        @Throws(IOException::class)
        fun addChild(entry: FileSystemEntry): FileSystemEntry {
            val normalizedChildName = entry.getNormalizedName(false)
            val entries = children.get(normalizedChildName)
            if (entry is DirectoryEntry) {
                for (childEntry in entries) {
                    if (childEntry.logicalName == entry.logicalName) {
                        return childEntry
                    }
                }
            }

            val lastSuffix = lastSuffixMap[normalizedChildName] ?: -1

            var suffix = lastSuffix
            while (true) {
                suffix++

                var entryPhysicalName = entry.getPhysicalNameWithSuffix(suffix)
                val entryFile = File(this.file, entryPhysicalName)
                entryPhysicalName = entryFile.canonicalFile.name

                if (!this.physicalToEntry.containsKey(entryPhysicalName)) {
                    entry.setSuffix(suffix)
                    lastSuffixMap[normalizedChildName] = suffix
                    physicalToEntry[entry.physicalName!!] = entry
                    break
                }
            }
            entries.add(entry)
            return entry
        }

        override fun getPhysicalNameWithSuffix(suffix: Int): String {
            if (suffix > 0) {
                return getNormalizedName(true) + "." + suffix
            }
            return getNormalizedName(true)
        }

        override fun createIfNeeded() {
            val physicalName = this.physicalName
            val parent = this.parent
            if (parent != null && physicalName != null) {
                val newFile = File(parent.file, physicalName).canonicalFile
                file = newFile

                // If there are 2 non-existent files with different names that collide after filesystem
                // canonicalization, getCanonicalPath() for each will return different values. But once one of the 2
                // files gets created, the other will return the same name as the one that was created.
                //
                // In order to detect these collisions, we need to ensure that the same value would be returned for any
                // future potential filename that would end up colliding. So we have to actually create the file here,
                // to force the Schrodinger filename to collapse to this particular version.
                newFile.mkdirs()
            }
        }

        fun isCaseSensitive(): Boolean {
            if (physicalName == null || file == null) {
                throw IllegalStateException("Must call setSuffix() first")
            }

            if (caseSensitivity != NO_VALUE) {
                return caseSensitivity == CASE_SENSITIVE
            }

            val path = file!!
            if (path.exists() && path.isFile) {
                if (!path.delete()) {
                    throw ExceptionWithContext("Can't delete %s to make it into a directory",
                        path.absolutePath)
                }
            }

            if (!path.exists() && !path.mkdirs()) {
                throw ExceptionWithContext("Couldn't create directory %s", path.absolutePath)
            }

            return try {
                val result = testCaseSensitivity(path)
                caseSensitivity = if (result) CASE_SENSITIVE else CASE_INSENSITIVE
                result
            } catch (ex: IOException) {
                false
            }
        }
    }

    private inner class FileEntry : FileSystemEntry {
        constructor(parent: DirectoryEntry?, logicalName: String) : super(parent, logicalName)

        override fun getPhysicalNameWithSuffix(suffix: Int): String {
            if (suffix > 0) {
                return addSuffixBeforeExtension(getNormalizedName(true), "." + suffix)
            }
            return getNormalizedName(true)
        }

        override fun createIfNeeded() {
            val physicalName = this.physicalName
            val parent = this.parent
            if (parent != null && physicalName != null) {
                val file = File(parent.file, physicalName).canonicalFile

                // If there are 2 non-existent files with different names that collide after filesystem
                // canonicalization, getCanonicalPath() for each will return different values. But once one of the 2
                // files gets created, the other will return the same name as the one that was created.
                //
                // In order to detect these collisions, we need to ensure that the same value would be returned for any
                // future potential filename that would end up colliding. So we have to actually create the file here,
                // to force the Schrodinger filename to collapse to this particular version.
                file.createNewFile()
            }
        }
    }

    companion object {
        private const val MAX_FILENAME_LENGTH = 255
        // How many characters to reserve in the physical filename for numeric suffixes
        // Dex files can currently only have 64k classes, so 5 digits plus 1 for an '#' should
        // be sufficient to handle the case when every class has a conflicting name
        private const val NUMERIC_SUFFIX_RESERVE = 6

        /**
         * Shortens an individual file/directory name, removing the necessary number of code points
         * from the middle of the string such that the utf-8 encoding of the string is at least
         * bytesToRemove bytes shorter than the original.
         *
         * The removed codePoints in the middle of the string will be replaced with a # character.
         */
        @JvmStatic
        fun shortenPathComponent(pathComponent: String, bytesToRemove0: Int): String {
            // We replace the removed part with a #, so we need to remove 1 extra char
            var bytesToRemove = bytesToRemove0
            bytesToRemove++

            val codePoints = try {
                val intBuffer: IntBuffer =
                    ByteBuffer.wrap(pathComponent.toByteArray(Charsets.UTF_32BE)).asIntBuffer()
                val arr = IntArray(intBuffer.limit())
                intBuffer.get(arr)
                arr
            } catch (ex: UnsupportedEncodingException) {
                throw RuntimeException(ex)
            }

            val midPoint = codePoints.size / 2

            var firstEnd = midPoint // exclusive
            var secondStart = midPoint + 1 // inclusive
            var bytesRemoved = utf8Length(codePoints[midPoint])

            // if we have an even number of codepoints, start by removing both middle characters,
            // unless just removing the first already removes enough bytes
            if (((codePoints.size % 2) == 0) && bytesRemoved < bytesToRemove) {
                bytesRemoved += utf8Length(codePoints[secondStart])
                secondStart++
            }

            while ((bytesRemoved < bytesToRemove) &&
                (firstEnd > 0 || secondStart < codePoints.size)) {
                if (firstEnd > 0) {
                    firstEnd--
                    bytesRemoved += utf8Length(codePoints[firstEnd])
                }

                if (bytesRemoved < bytesToRemove && secondStart < codePoints.size) {
                    bytesRemoved += utf8Length(codePoints[secondStart])
                    secondStart++
                }
            }

            val sb = StringBuilder()
            for (i in 0 until firstEnd) {
                sb.appendCodePoint(codePoints[i])
            }
            sb.append('#')
            for (i in secondStart until codePoints.size) {
                sb.appendCodePoint(codePoints[i])
            }

            return sb.toString()
        }

        private fun utf8Length(str: String): Int {
            var utf8Length = 0
            var i = 0
            while (i < str.length) {
                val c = str.codePointAt(i)
                utf8Length += utf8Length(c)
                i += Character.charCount(c)
            }
            return utf8Length
        }

        private fun utf8Length(codePoint: Int): Int {
            return if (codePoint < 0x80) {
                1
            } else if (codePoint < 0x800) {
                2
            } else if (codePoint < 0x10000) {
                3
            } else {
                4
            }
        }

        private fun isWindows(): Boolean {
            return System.getProperty("os.name").startsWith("Windows")
        }

        private val reservedFileNameRegex = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$", Pattern.CASE_INSENSITIVE
        )

        private fun isReservedFileName(className: String): Boolean {
            return reservedFileNameRegex.matcher(className).matches()
        }

        private fun addSuffixBeforeExtension(pathElement: String, suffix: String): String {
            val extensionStart = pathElement.lastIndexOf('.')

            val newName = StringBuilder(pathElement.length + suffix.length + 1)
            if (extensionStart < 0) {
                newName.append(pathElement)
                newName.append(suffix)
            } else {
                newName.append(pathElement.subSequence(0, extensionStart))
                newName.append(suffix)
                newName.append(pathElement.subSequence(extensionStart, pathElement.length))
            }
            return newName.toString()
        }
    }
}
