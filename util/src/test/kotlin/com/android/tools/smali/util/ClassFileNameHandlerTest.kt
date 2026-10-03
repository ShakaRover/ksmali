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

package com.android.tools.smali.util

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class ClassFileNameHandlerTest {
    private val UTF8: Charset = StandardCharsets.UTF_8

    @Test
    fun test1ByteEncodings() {
        val sb = StringBuilder()
        for (i in 0 until 100) {
            sb.append(i.toChar())
        }

        val result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 5)
        Assert.assertEquals(95, result.toByteArray(UTF8).size)
        Assert.assertEquals(95, result.length)
    }

    @Test
    fun test2ByteEncodings() {
        val sb = StringBuilder()
        for (i in 0x80 until 0x80 + 100) {
            sb.append(i.toChar())
        }

        // remove a total of 3 2-byte characters, and then add back in the 1-byte '#'
        var result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 4)
        Assert.assertEquals(200, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(195, result.toByteArray(UTF8).size)
        Assert.assertEquals(98, result.length)

        // remove a total of 3 2-byte characters, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 5)
        Assert.assertEquals(200, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(195, result.toByteArray(UTF8).size)
        Assert.assertEquals(98, result.length)
    }

    @Test
    fun test3ByteEncodings() {
        val sb = StringBuilder()
        for (i in 0x800 until 0x800 + 100) {
            sb.append(i.toChar())
        }

        // remove a total of 3 3-byte characters, and then add back in the 1-byte '#'
        var result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 6)
        Assert.assertEquals(300, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(292, result.toByteArray(UTF8).size)
        Assert.assertEquals(98, result.length)

        // remove a total of 3 3-byte characters, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 7)
        Assert.assertEquals(300, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(292, result.toByteArray(UTF8).size)
        Assert.assertEquals(98, result.length)
    }

    @Test
    fun test4ByteEncodings() {
        val sb = StringBuilder()
        for (i in 0x10000 until 0x10000 + 100) {
            sb.appendCodePoint(i)
        }

        // we remove 3 codepoints == 6 characters == 12 bytes, and then add back in the 1-byte '#'
        var result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 8)
        Assert.assertEquals(400, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(389, result.toByteArray(UTF8).size)
        Assert.assertEquals(195, result.length)

        // we remove 2 codepoints == 4 characters == 8 bytes, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 7)
        Assert.assertEquals(400, sb.toString().toByteArray(UTF8).size)
        Assert.assertEquals(393, result.toByteArray(UTF8).size)
        Assert.assertEquals(197, result.length)
    }

    @Test
    fun testMultipleLongNames() {
        val filenameFragment = "a".repeat(512)

        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali")

        // put the differentiating character in the middle, where it will get stripped out by the filename shortening
        // logic
        val file1 = handler.getUniqueFilenameForClass(
            "La/a/" + filenameFragment + "1" + filenameFragment + ";")
        checkFilename(tempDir, file1, "a", "a",
            "a".repeat(124) + "#" + "a".repeat(118) + ".smali")

        val file2 = handler.getUniqueFilenameForClass(
            "La/a/" + filenameFragment + "2" + filenameFragment + ";")
        checkFilename(tempDir, file2, "a", "a",
            "a".repeat(124) + "#" + "a".repeat(118) + ".1.smali")

        Assert.assertFalse(file1.absolutePath == file2.absolutePath)
    }

    @Test
    fun testBasicFunctionality() {
        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali")

        var file = handler.getUniqueFilenameForClass("La/b/c/d;")
        checkFilename(tempDir, file, "a", "b", "c", "d.smali")

        file = handler.getUniqueFilenameForClass("La/b/c/e;")
        checkFilename(tempDir, file, "a", "b", "c", "e.smali")

        file = handler.getUniqueFilenameForClass("La/b/d/d;")
        checkFilename(tempDir, file, "a", "b", "d", "d.smali")

        file = handler.getUniqueFilenameForClass("La/b;")
        checkFilename(tempDir, file, "a", "b.smali")

        file = handler.getUniqueFilenameForClass("Lb;")
        checkFilename(tempDir, file, "b.smali")
    }

    @Test
    fun testCaseInsensitiveFilesystem() {
        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali", false, false)

        var file = handler.getUniqueFilenameForClass("La/b/c;")
        checkFilename(tempDir, file, "a", "b", "c.smali")

        file = handler.getUniqueFilenameForClass("La/b/C;")
        checkFilename(tempDir, file, "a", "b", "C.1.smali")

        file = handler.getUniqueFilenameForClass("La/B/c;")
        checkFilename(tempDir, file, "a", "B.1", "c.smali")
    }

    @Test
    fun testCaseSensitiveFilesystem() {
        val tempDir = createTempDir().canonicalFile
        if (!testCaseSensitivity(tempDir)) {
            // Test can only be performed on case sensitive systems
            return
        }

        val handler = ClassFileNameHandler(tempDir, ".smali", true, false)

        var file = handler.getUniqueFilenameForClass("La/b/c;")
        checkFilename(tempDir, file, "a", "b", "c.smali")

        file = handler.getUniqueFilenameForClass("La/b/C;")
        checkFilename(tempDir, file, "a", "b", "C.smali")

        file = handler.getUniqueFilenameForClass("La/B/c;")
        checkFilename(tempDir, file, "a", "B", "c.smali")
    }

    @Test
    fun testWindowsReservedFilenames() {
        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali", false, true)

        var file = handler.getUniqueFilenameForClass("La/con/c;")
        checkFilename(tempDir, file, "a", "con#", "c.smali")

        file = handler.getUniqueFilenameForClass("La/Con/c;")
        checkFilename(tempDir, file, "a", "Con#.1", "c.smali")

        file = handler.getUniqueFilenameForClass("La/b/PRN;")
        checkFilename(tempDir, file, "a", "b", "PRN#.smali")

        file = handler.getUniqueFilenameForClass("La/b/prN;")
        checkFilename(tempDir, file, "a", "b", "prN#.1.smali")

        file = handler.getUniqueFilenameForClass("La/b/com0;")
        checkFilename(tempDir, file, "a", "b", "com0.smali")

        for (reservedName in arrayOf("con", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9")) {
            file = handler.getUniqueFilenameForClass("L" + reservedName + ";")
            checkFilename(tempDir, file, reservedName + "#.smali")
        }
    }

    @Test
    fun testIgnoringWindowsReservedFilenames() {
        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali", true, false)

        var file = handler.getUniqueFilenameForClass("La/con/c;")
        checkFilename(tempDir, file, "a", "con", "c.smali")

        file = handler.getUniqueFilenameForClass("La/Con/c;")
        if (testCaseSensitivity(tempDir)) {
            checkFilename(tempDir, file, "a", "Con", "c.smali")
        } else {
            checkFilename(tempDir, file, "a", "Con.1", "c.smali")
        }

        file = handler.getUniqueFilenameForClass("La/b/PRN;")
        checkFilename(tempDir, file, "a", "b", "PRN.smali")

        file = handler.getUniqueFilenameForClass("La/b/prN;")
        if (testCaseSensitivity(tempDir)) {
            checkFilename(tempDir, file, "a", "b", "prN.smali")
        } else {
            checkFilename(tempDir, file, "a", "b", "prN.1.smali")
        }

        file = handler.getUniqueFilenameForClass("La/b/com0;")
        checkFilename(tempDir, file, "a", "b", "com0.smali")

        for (reservedName in arrayOf("con", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9")) {
            file = handler.getUniqueFilenameForClass("L" + reservedName + ";")
            checkFilename(tempDir, file, reservedName + ".smali")
        }
    }

    @Test
    fun testUnicodeCollisionOnMac() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) {
            // The test is only applicable when run on a mac system
            return
        }

        val tempDir = createTempDir().canonicalFile
        val handler = ClassFileNameHandler(tempDir, ".smali", true, false)

        var file = handler.getUniqueFilenameForClass("Lε;")
        checkFilename(tempDir, file, "ε.smali")

        file = handler.getUniqueFilenameForClass("Lϵ;")
        checkFilename(tempDir, file, "ϵ.1.smali")

        file = handler.getUniqueFilenameForClass("Lε/ε;")
        checkFilename(tempDir, file, "ε", "ε.smali")

        file = handler.getUniqueFilenameForClass("Lε/ϵ;")
        checkFilename(tempDir, file, "ε", "ϵ.1.smali")

        file = handler.getUniqueFilenameForClass("Lϵ/ϵ;")
        checkFilename(tempDir, file, "ϵ.1", "ϵ.smali")

        file = handler.getUniqueFilenameForClass("Lϵ/ε;")
        checkFilename(tempDir, file, "ϵ.1", "ε.1.smali")
    }

    private fun checkFilename(base: File, file0: File, vararg elements: String) {
        var file = file0
        for (i in elements.size - 1 downTo 0) {
            Assert.assertEquals(elements[i], file.name)
            file = file.parentFile
        }
        Assert.assertEquals(base.absolutePath, file.absolutePath)
    }

    private fun createTempDir(): File = java.nio.file.Files.createTempDirectory("smali-classfilehandler").toFile()
}
