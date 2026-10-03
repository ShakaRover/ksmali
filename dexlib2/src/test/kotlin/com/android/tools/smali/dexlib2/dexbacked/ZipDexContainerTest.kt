/*
 * Copyright 2026, Google LLC
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
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(JUnit4::class)
class ZipDexContainerTest {
    @Test
    @Throws(Exception::class)
    fun testZipDexEntryCrc() {
        val classDef = makeClassDef()

        val dataStore = MemoryDataStore()
        DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.getDefault(), setOf(classDef)))
        val dexBytes = dataStore.data

        val crc32 = CRC32()
        crc32.update(dexBytes)
        val expectedCrc = crc32.value

        val tempZip = File.createTempFile("test", ".apk")
        tempZip.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            val entry = ZipEntry("classes.dex")
            zos.putNextEntry(entry)
            zos.write(dexBytes)
            zos.closeEntry()
        }

        val container = ZipDexContainer(tempZip, Opcodes.getDefault())
        val entries = container.dexEntryNames
        Assert.assertEquals(1, entries.size)
        Assert.assertEquals("classes.dex", entries[0])

        val entry = container.getEntry("classes.dex")
        Assert.assertNotNull(entry)
        Assert.assertEquals("classes.dex", entry!!.entryName)
        Assert.assertEquals(expectedCrc, entry.crc)
    }

    @Test
    @Throws(Exception::class)
    fun testZipDexEntryDeterministicOrder() {
        val classDef = makeClassDef()

        val dataStore = MemoryDataStore()
        DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.getDefault(), setOf(classDef)))
        val dexBytes = dataStore.data

        val tempZip = File.createTempFile("test_order", ".apk")
        tempZip.deleteOnExit()

        // Write entries out of order: z.dex, classes2.dex, classes4.dex, classes.dex, a.dex
        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            for (name in arrayOf("z.dex", "classes2.dex", "classes4.dex", "classes.dex", "a.dex")) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(dexBytes)
                zos.closeEntry()
            }
        }

        val container = ZipDexContainer(tempZip, Opcodes.getDefault())
        val entries = container.dexEntryNames

        // Expected order: classes.dex, classes2.dex (classes3 missing), then a, classes4, z.dex
        val expectedOrder = arrayOf("classes.dex", "classes2.dex", "a.dex", "classes4.dex", "z.dex")
        Assert.assertEquals(expectedOrder.size, entries.size)

        for (i in expectedOrder.indices) {
            Assert.assertEquals(expectedOrder[i], entries[i])
            val entry = container.getEntry(expectedOrder[i])
            Assert.assertNotNull(entry)
        }
    }
    @Test
    @Throws(Exception::class)
    fun testZipDexContainerContainerDex() {
        val classDef = makeClassDef()

        val dataStore = MemoryDataStore()
        DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.getDefault(), setOf(classDef)))
        val dexBytes = dataStore.data

        val containerBytes = ByteArray(dexBytes.size * 2)
        System.arraycopy(dexBytes, 0, containerBytes, 0, dexBytes.size)
        System.arraycopy(dexBytes, 0, containerBytes, dexBytes.size, dexBytes.size)

        for (headerOffset in intArrayOf(0, dexBytes.size)) {
            containerBytes[headerOffset + 4] = '0'.code.toByte()
            containerBytes[headerOffset + 5] = '4'.code.toByte()
            containerBytes[headerOffset + 6] = '1'.code.toByte()
            writeInt(containerBytes, headerOffset + HeaderItem.CONTAINER_SIZE_OFFSET, containerBytes.size)
            writeInt(containerBytes, headerOffset + HeaderItem.HEADER_OFFSET_OFFSET, headerOffset)
        }

        val tempZip = File.createTempFile("test_container", ".apk")
        tempZip.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            val entry = ZipEntry("classes.dex")
            zos.putNextEntry(entry)
            zos.write(containerBytes)
            zos.closeEntry()
        }

        val container = ZipDexContainer(tempZip, Opcodes.forDexVersion(41))
        val entries = container.dexEntryNames
        Assert.assertEquals(2, entries.size)
        Assert.assertEquals("classes.dex/0", entries[0])
        Assert.assertEquals("classes.dex/1", entries[1])

        Assert.assertNotNull(container.getEntry("classes.dex/0"))
        Assert.assertNotNull(container.getEntry("classes.dex/1"))
    }

    @Test
    @Throws(Exception::class)
    fun testZipDexContainerIgnoreTrailingGarbage() {
        val classDef = makeClassDef()

        val dataStore = MemoryDataStore()
        DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.getDefault(), setOf(classDef)))
        val dexBytes = dataStore.data

        val containerBytesWithGarbage = ByteArray(dexBytes.size * 2 + 50)
        System.arraycopy(dexBytes, 0, containerBytesWithGarbage, 0, dexBytes.size)
        System.arraycopy(dexBytes, 0, containerBytesWithGarbage, dexBytes.size, dexBytes.size)

        for (headerOffset in intArrayOf(0, dexBytes.size)) {
            containerBytesWithGarbage[headerOffset + 4] = '0'.code.toByte()
            containerBytesWithGarbage[headerOffset + 5] = '4'.code.toByte()
            containerBytesWithGarbage[headerOffset + 6] = '1'.code.toByte()
            writeInt(containerBytesWithGarbage,
                headerOffset + HeaderItem.CONTAINER_SIZE_OFFSET, dexBytes.size * 2)
            writeInt(containerBytesWithGarbage,
                headerOffset + HeaderItem.HEADER_OFFSET_OFFSET, headerOffset)
        }

        val tempZip = File.createTempFile("test_garbage", ".apk")
        tempZip.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            val entry = ZipEntry("classes.dex")
            zos.putNextEntry(entry)
            zos.write(containerBytesWithGarbage)
            zos.closeEntry()
        }

        val container = ZipDexContainer(tempZip, Opcodes.forDexVersion(41))
        val entries = container.dexEntryNames
        Assert.assertEquals(2, entries.size)
        Assert.assertEquals("classes.dex/0", entries[0])
        Assert.assertEquals("classes.dex/1", entries[1])
    }
    @Test
    @Throws(Exception::class)
    fun testZipDexContainerSingleEntryContainerDex() {
        val classDef = makeClassDef()

        val dataStore = MemoryDataStore()
        DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.getDefault(), setOf(classDef)))
        val dexBytes = dataStore.data

        val containerBytes = ByteArray(dexBytes.size)
        System.arraycopy(dexBytes, 0, containerBytes, 0, dexBytes.size)

        containerBytes[4] = '0'.code.toByte()
        containerBytes[5] = '4'.code.toByte()
        containerBytes[6] = '1'.code.toByte()
        writeInt(containerBytes, HeaderItem.CONTAINER_SIZE_OFFSET, containerBytes.size)
        writeInt(containerBytes, HeaderItem.HEADER_OFFSET_OFFSET, 0)

        val tempZip = File.createTempFile("test_single_container", ".apk")
        tempZip.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            val entry = ZipEntry("classes.dex")
            zos.putNextEntry(entry)
            zos.write(containerBytes)
            zos.closeEntry()
        }

        val container = ZipDexContainer(tempZip, Opcodes.forDexVersion(41))
        val entries = container.dexEntryNames
        Assert.assertEquals(1, entries.size)
        Assert.assertEquals("classes.dex", entries[0])
        Assert.assertNotNull(container.getEntry("classes.dex"))
    }

    companion object {
        private fun writeInt(buf: ByteArray, offset: Int, value: Int) {
            buf[offset] = value.toByte()
            buf[offset + 1] = (value shr 8).toByte()
            buf[offset + 2] = (value shr 16).toByte()
            buf[offset + 3] = (value shr 24).toByte()
        }
    }

    private fun makeClassDef(): ImmutableClassDef {
        return ImmutableClassDef("Lorg/test/blah;",
            0, "Ljava/lang/Object;", null, null, setOf(), null, null)
    }
}
