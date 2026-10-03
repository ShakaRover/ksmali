/*
 * Copyright 2016, Google LLC
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

import com.android.tools.smali.dexlib2.DexFileFactory.DexEntryFinder
import com.android.tools.smali.dexlib2.DexFileFactory.DexFileNotFoundException
import com.android.tools.smali.dexlib2.DexFileFactory.MultipleMatchingDexEntriesException
import com.android.tools.smali.dexlib2.DexFileFactory.UnsupportedFileTypeException
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.IOException

class DexEntryFinderTest {

    @Test
    @Throws(Exception::class)
    fun testNormalStuff() {
        val entries = HashMap<String, DexBackedDexFile>()
        val dexFile1 = mock(DexBackedDexFile::class.java)
        entries["/system/framework/framework.jar"] = dexFile1
        val dexFile2 = mock(DexBackedDexFile::class.java)
        entries["/system/framework/framework.jar:classes2.dex"] = dexFile2
        val testFinder = DexEntryFinder("blah.oat", TestMultiDexContainer(entries))

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true).dexFile)

        assertEntryNotFound(testFinder, "system/framework/framework.jar", true)
        assertEntryNotFound(testFinder, "/framework/framework.jar", true)
        assertEntryNotFound(testFinder, "framework/framework.jar", true)
        assertEntryNotFound(testFinder, "/framework.jar", true)
        assertEntryNotFound(testFinder, "framework.jar", true)

        Assert.assertEquals(dexFile1, testFinder.findEntry("system/framework/framework.jar", false).dexFile)
        Assert.assertEquals(dexFile1, testFinder.findEntry("/framework/framework.jar", false).dexFile)
        Assert.assertEquals(dexFile1, testFinder.findEntry("framework/framework.jar", false).dexFile)
        Assert.assertEquals(dexFile1, testFinder.findEntry("/framework.jar", false).dexFile)
        Assert.assertEquals(dexFile1, testFinder.findEntry("framework.jar", false).dexFile)

        assertEntryNotFound(testFinder, "ystem/framework/framework.jar", false)
        assertEntryNotFound(testFinder, "ssystem/framework/framework.jar", false)
        assertEntryNotFound(testFinder, "ramework/framework.jar", false)
        assertEntryNotFound(testFinder, "ramework.jar", false)
        assertEntryNotFound(testFinder, "framework", false)

        Assert.assertEquals(dexFile2,
            testFinder.findEntry("/system/framework/framework.jar:classes2.dex", true).dexFile)

        assertEntryNotFound(testFinder, "system/framework/framework.jar:classes2.dex", true)
        assertEntryNotFound(testFinder, "framework.jar:classes2.dex", true)
        assertEntryNotFound(testFinder, "classes2.dex", true)

        Assert.assertEquals(dexFile2,
            testFinder.findEntry("system/framework/framework.jar:classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2,
            testFinder.findEntry("/framework/framework.jar:classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework/framework.jar:classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework.jar:classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework.jar:classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry(":classes2.dex", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("classes2.dex", false).dexFile)

        assertEntryNotFound(testFinder, "ystem/framework/framework.jar:classes2.dex", false)
        assertEntryNotFound(testFinder, "ramework.jar:classes2.dex", false)
        assertEntryNotFound(testFinder, "lasses2.dex", false)
        assertEntryNotFound(testFinder, "classes2", false)
    }
    @Test
    @Throws(Exception::class)
    fun testSimilarEntries() {
        val entries = HashMap<String, DexBackedDexFile>()
        val dexFile1 = mock(DexBackedDexFile::class.java)
        entries["/system/framework/framework.jar"] = dexFile1
        val dexFile2 = mock(DexBackedDexFile::class.java)
        entries["system/framework/framework.jar"] = dexFile2
        val testFinder = DexEntryFinder("blah.oat", TestMultiDexContainer(entries))

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("system/framework/framework.jar", true).dexFile)

        assertMultipleMatchingEntries(testFinder, "/system/framework/framework.jar")
        assertMultipleMatchingEntries(testFinder, "system/framework/framework.jar")
        assertMultipleMatchingEntries(testFinder, "/framework/framework.jar")
        assertMultipleMatchingEntries(testFinder, "framework/framework.jar")
        assertMultipleMatchingEntries(testFinder, "/framework.jar")
        assertMultipleMatchingEntries(testFinder, "framework.jar")
    }

    @Test
    @Throws(Exception::class)
    fun testMatchingSuffix() {
        val entries = HashMap<String, DexBackedDexFile>()
        val dexFile1 = mock(DexBackedDexFile::class.java)
        entries["/system/framework/framework.jar"] = dexFile1
        val dexFile2 = mock(DexBackedDexFile::class.java)
        entries["/framework/framework.jar"] = dexFile2
        val testFinder = DexEntryFinder("blah.oat", TestMultiDexContainer(entries))

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework/framework.jar", true).dexFile)

        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework/framework.jar", false).dexFile)
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework/framework.jar", false).dexFile)

        assertMultipleMatchingEntries(testFinder, "/framework.jar")
        assertMultipleMatchingEntries(testFinder, "framework.jar")
    }

    @Test
    @Throws(Exception::class)
    fun testNonDexEntries() {
        val entries = HashMap<String, DexBackedDexFile?>()
        val dexFile1 = mock(DexBackedDexFile::class.java)
        entries["classes.dex"] = dexFile1
        entries["/blah/classes.dex"] = null
        val testFinder = DexEntryFinder("blah.oat", TestMultiDexContainer(entries))

        Assert.assertEquals(dexFile1, testFinder.findEntry("classes.dex", true).dexFile)
        Assert.assertEquals(dexFile1, testFinder.findEntry("classes.dex", false).dexFile)

        assertUnsupportedFileType(testFinder, "/blah/classes.dex", true)
        assertDexFileNotFound(testFinder, "/blah/classes.dex", false)
    }

    @Throws(IOException::class)
    private fun assertEntryNotFound(finder: DexEntryFinder, entry: String, exactMatch: Boolean) {
        try {
            finder.findEntry(entry, exactMatch)
            Assert.fail()
        } catch (ex: DexFileNotFoundException) {
            // expected exception
        }
    }

    @Throws(IOException::class)
    private fun assertMultipleMatchingEntries(finder: DexEntryFinder, entry: String) {
        try {
            finder.findEntry(entry, false)
            Assert.fail()
        } catch (ex: MultipleMatchingDexEntriesException) {
            // expected exception
        }
    }

    @Throws(IOException::class)
    private fun assertUnsupportedFileType(finder: DexEntryFinder, entry: String, exactMatch: Boolean) {
        try {
            finder.findEntry(entry, exactMatch)
            Assert.fail()
        } catch (ex: UnsupportedFileTypeException) {
            // expected exception
        }
    }

    @Throws(IOException::class)
    private fun assertDexFileNotFound(finder: DexEntryFinder, entry: String, exactMatch: Boolean) {
        try {
            finder.findEntry(entry, exactMatch)
            Assert.fail()
        } catch (ex: DexFileNotFoundException) {
            // expected exception
        }
    }
    class TestMultiDexContainer(
        private val entries: Map<String, DexBackedDexFile?>
    ) : MultiDexContainer<DexBackedDexFile> {
        @get:Throws(IOException::class)
        override val dexEntryNames: List<String>
            get() {
                val entryNames = ArrayList<String>()

                for (entry in entries.entries) {
                    if (entry.value != null) {
                        entryNames.add(entry.key)
                    }
                }

                return entryNames
            }

        @Throws(IOException::class)
        override fun getEntry(entryName: String): MultiDexContainer.DexEntry<DexBackedDexFile>? {
            if (entries.containsKey(entryName)) {
                val dexFile = entries[entryName]
                if (dexFile == null) {
                    throw DexBackedDexFile.NotADexFile()
                }

                return object : MultiDexContainer.DexEntry<DexBackedDexFile> {
                    override val entryName: String
                        get() = "classes.dex"

                    override val dexFile: DexBackedDexFile
                        get() = dexFile

                    override val container: MultiDexContainer<DexBackedDexFile>
                        get() = this@TestMultiDexContainer
                }
            }
            return null
        }
    }
}
