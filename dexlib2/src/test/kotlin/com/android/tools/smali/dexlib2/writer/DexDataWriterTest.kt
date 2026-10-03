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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.util.ExceptionWithContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Arrays
import java.util.Random

class DexDataWriterTest {
    private lateinit var random: Random
    private val output = NakedByteArrayOutputStream()
    private var startPosition = 0
    private lateinit var writer: DexDataWriter

    @Before
    @Throws(IOException::class)
    fun setup() {
        // use a predefined seed, so we get a deterministic result
        random = Random()
        output.reset()
        startPosition = 123
    val bufferSize = 256
        writer = DexDataWriter(output, startPosition, bufferSize)
    }

    // Note: we use int[] rather than byte[] so that we don't have to cast every value when manually constructing an
    // array.
    private fun expectData(vararg bytes: Int) {
        Assert.assertEquals(startPosition+bytes.size, writer.position)

        writer.flush()
        val writtenData = output.buffer

        for (i in 0 until bytes.size) {
            Assert.assertEquals("Values not equal at index ${i}", bytes[i].toByte(), writtenData[i])
        }
    }

    private fun expectData(bytes: ByteArray) {
        Assert.assertEquals(startPosition+bytes.size, writer.position)

        writer.flush()
        val writtenData = output.buffer

        for (i in 0 until bytes.size) {
            Assert.assertEquals("Values not equal at index ${i}", bytes[i], writtenData[i])
        }
    }

    @Test
    @Throws(IOException::class)
    fun testWriteByte() {
        val arr = ByteArray(257)
        for (i in 0 until 256) {
            arr[i] = i.toByte()
            writer.write(i)
        }
        arr[256] = 0x80.toByte()
        writer.write(0x180)

        expectData(arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteByteArray() {
        val arr = ByteArray(345)
        random.nextBytes(arr)
        writer.write(arr)

        expectData(arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteByteArrayWithLengthAndOffset() {
        val arr = ByteArray(345)
        random.nextBytes(arr)
        writer.write(arr, 10, 300)

        expectData(Arrays.copyOfRange(arr, 10, 310))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteLong() {
        writer.writeLong(0x1122334455667788L)
        writer.writeLong(-0x1122334455667788L)

        expectData(0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
                   0x78, 0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteInt() {
        writer.writeInt(0x11223344)
        writer.writeInt(-0x11223344)

        expectData(0x44, 0x33, 0x22, 0x11,
                   0xBC, 0xCC, 0xDD, 0xEE)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteShort() {
        writer.writeShort(0)
        writer.writeShort(0x1122)
        writer.writeShort(-0x1122)
        writer.writeShort(0x7FFF)
        writer.writeShort(-0x8000)

        expectData(0x00, 0x00,
                   0x22, 0x11,
                   0xDE, 0xEE,
                   0xFF, 0x7F,
                   0x00, 0x80)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteShortOutOfBounds() {
        writer.writeShort(0x8000)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteShortOutOfBounds2() {
        writer.writeShort(-0x8001)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteUshort() {
        writer.writeUshort(0)
        writer.writeUshort(0x1122)
        writer.writeUshort(0x8899)
        writer.writeUshort(0xFFFF)

        expectData(0x00, 0x00,
                   0x22, 0x11,
                   0x99, 0x88,
                   0xFF, 0xFF)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteUshortOutOfBounds() {
        writer.writeUshort(-1)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteUshortOutOfBounds2() {
        writer.writeUshort(0x10000)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteUbyte() {
        writer.writeUbyte(0)
        writer.writeUbyte(1)
        writer.writeUbyte(0x12)
        writer.writeUbyte(0xFF)

        expectData(0x00, 0x01, 0x12, 0xFF)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteUbyteOutOfBounds() {
        writer.writeUbyte(-1)
    }

    @Test(expected = ExceptionWithContext::class)
    @Throws(IOException::class)
    fun testWriteUbyteOutOfBounds2() {
        writer.writeUbyte(256)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValueHeader() {
        writer.writeEncodedValueHeader(0x2, 0x1)

        expectData(0x22)
    }

    private fun testWriteEncodedIntHelper(integerValue: Int, vararg encodedValue: Int) {
        setup()
        writer.writeEncodedInt(ValueType.INT, integerValue)

        val arr = IntArray(encodedValue.size+1)
        arr[0] = ValueType.INT or ((encodedValue.size - 1) shl 5)
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.size)
        expectData(*arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedInt() {
        testWriteEncodedIntHelper(0x00.toInt(), 0x00)
        testWriteEncodedIntHelper(0x40.toInt(), 0x40)
        testWriteEncodedIntHelper(0x7f.toInt(), 0x7f)
        testWriteEncodedIntHelper(0xff.toInt(), 0xff, 0x00)
        testWriteEncodedIntHelper(0xffff80.toInt(), 0x80, 0xff, 0xff, 0x00)
        testWriteEncodedIntHelper(0xffffff80.toInt(), 0x80)
        testWriteEncodedIntHelper(0xffffffff.toInt(), 0xff)
        testWriteEncodedIntHelper(0x100.toInt(), 0x00, 0x01)
        testWriteEncodedIntHelper(0x7fff.toInt(), 0xff, 0x7f)
        testWriteEncodedIntHelper(0x8000.toInt(), 0x00, 0x80, 0x00)
        testWriteEncodedIntHelper(0xffff8000.toInt(), 0x00, 0x80)
        testWriteEncodedIntHelper(0x10000.toInt(), 0x00, 0x00, 0x01)
        testWriteEncodedIntHelper(0x10203.toInt(), 0x03, 0x02, 0x01)
        testWriteEncodedIntHelper(0x810203.toInt(), 0x03, 0x02, 0x81, 0x00)
        testWriteEncodedIntHelper(0xff810203.toInt(), 0x03, 0x02, 0x81)
        testWriteEncodedIntHelper(0x1000000.toInt(), 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedIntHelper(0x1020304.toInt(), 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedIntHelper(0x7fffffff.toInt(), 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedIntHelper(0x80000000.toInt(), 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedIntHelper(0x80000001.toInt(), 0x01, 0x00, 0x00, 0x80)
    }

    private fun testWriteEncodedUintHelper(integerValue: Int, vararg encodedValue: Int) {
        setup()
        writer.writeEncodedUint(ValueType.METHOD, integerValue)

        val arr = IntArray(encodedValue.size+1)
        arr[0] = ValueType.METHOD or ((encodedValue.size - 1) shl 5)
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.size)
        expectData(*arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedUint() {
        testWriteEncodedUintHelper(0x00.toInt(), 0x00)
        testWriteEncodedUintHelper(0x01.toInt(), 0x01)
        testWriteEncodedUintHelper(0x40.toInt(), 0x40)
        testWriteEncodedUintHelper(0x7f.toInt(), 0x7f)
        testWriteEncodedUintHelper(0x80.toInt(), 0x80)
        testWriteEncodedUintHelper(0x81.toInt(), 0x81)
        testWriteEncodedUintHelper(0xff.toInt(), 0xff)
        testWriteEncodedUintHelper(0x100.toInt(), 0x00, 0x01)
        testWriteEncodedUintHelper(0x180.toInt(), 0x80, 0x01)
        testWriteEncodedUintHelper(0x8080.toInt(), 0x80, 0x80)
        testWriteEncodedUintHelper(0x1234.toInt(), 0x34, 0x12)
        testWriteEncodedUintHelper(0x1000.toInt(), 0x00, 0x10)
        testWriteEncodedUintHelper(0x8000.toInt(), 0x00, 0x80)
        testWriteEncodedUintHelper(0xff00.toInt(), 0x00, 0xff)
        testWriteEncodedUintHelper(0xffff.toInt(), 0xff, 0xff)
        testWriteEncodedUintHelper(0x10000.toInt(), 0x00, 0x00, 0x01)
        testWriteEncodedUintHelper(0x1ffff.toInt(), 0xff, 0xff, 0x01)
        testWriteEncodedUintHelper(0x80ffff.toInt(), 0xff, 0xff, 0x80)
        testWriteEncodedUintHelper(0xffffff.toInt(), 0xff, 0xff, 0xff)
        testWriteEncodedUintHelper(0x1000000.toInt(), 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedUintHelper(0x1020304.toInt(), 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedUintHelper(0x80000000.toInt(), 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedUintHelper(0x80ffffff.toInt(), 0xff, 0xff, 0xff, 0x80)
        testWriteEncodedUintHelper(0xffffffff.toInt(), 0xff, 0xff, 0xff, 0xff)
    }

    private fun testWriteEncodedLongHelper(longValue: Long, vararg encodedValue: Int) {
        setup()
        writer.writeEncodedLong(ValueType.LONG, longValue)

        val arr = IntArray(encodedValue.size+1)
        arr[0] = ValueType.LONG or ((encodedValue.size - 1) shl 5)
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.size)
        expectData(*arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedLong() {
        testWriteEncodedLongHelper(0x00L, 0x00)
        testWriteEncodedLongHelper(0x40L, 0x40)
        testWriteEncodedLongHelper(0x7fL, 0x7f)
        testWriteEncodedLongHelper(0xffL, 0xff, 0x00)
        testWriteEncodedLongHelper(0xffffffffffffff80uL.toLong(), 0x80)
        testWriteEncodedLongHelper(0xffffffffffffffffuL.toLong(), 0xff)

        testWriteEncodedLongHelper(0x100L, 0x00, 0x01)
        testWriteEncodedLongHelper(0x7fffL, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x8000L, 0x00, 0x80, 0x00)
        testWriteEncodedLongHelper(0xffffffffffff8000uL.toLong(), 0x00, 0x80)

        testWriteEncodedLongHelper(0x10000L, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x10203L, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x810203L, 0x03, 0x02, 0x81, 0x00)
        testWriteEncodedLongHelper(0xffffffffff810203uL.toLong(), 0x03, 0x02, 0x81)

        testWriteEncodedLongHelper(0x1000000L, 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x1020304L, 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x7fffffffL, 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x80000000L, 0x00, 0x00, 0x00, 0x80, 0x00)
        testWriteEncodedLongHelper(0xffffffff80000000uL.toLong(), 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0xffffffff80000001uL.toLong(), 0x01, 0x00, 0x00, 0x80)

        testWriteEncodedLongHelper(0x100000000L, 0x00, 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x102030405L, 0x05, 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x7fffffffffL, 0xff, 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x8000000000L, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00)
        testWriteEncodedLongHelper(0xffffff8000000000uL.toLong(), 0x00, 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0xffffff8000000001uL.toLong(), 0x01, 0x00, 0x00, 0x00, 0x80)

        testWriteEncodedLongHelper(0x10000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x10203040506L, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x7fffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x800000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00)
        testWriteEncodedLongHelper(0xffff800000000000uL.toLong(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0xffff800000000001uL.toLong(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x80)

        testWriteEncodedLongHelper(0x1000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x1020304050607L, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x7fffffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x80000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00)
        testWriteEncodedLongHelper(0xff80000000000000uL.toLong(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0xff80000000000001uL.toLong(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80)

        testWriteEncodedLongHelper(0x100000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
        testWriteEncodedLongHelper(0x102030405060708L, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01)
        testWriteEncodedLongHelper(0x7fffffffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)
        testWriteEncodedLongHelper(0x8000000000000000uL.toLong(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0x8000000000000001uL.toLong(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80)
        testWriteEncodedLongHelper(0xfeffffffffffffffuL.toLong(), 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xfe)

        testWriteEncodedLongHelper(0x123456789ABCDEF0L, 0xF0, 0xDE, 0xBC, 0x9A, 0x78, 0x56, 0x34, 0x12)
    }

    private fun testWriteRightZeroExtendedIntHelper(intValue: Int, vararg encodedValue: Int) {
        setup()
        writer.writeRightZeroExtendedInt(ValueType.FLOAT, intValue)

        val arr = IntArray(encodedValue.size+1)
        arr[0] = ValueType.FLOAT or ((encodedValue.size - 1) shl 5)
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.size)
        expectData(*arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteRightZeroExtendedInt() {
        testWriteRightZeroExtendedIntHelper(0.toInt(), 0x00)

        testWriteRightZeroExtendedIntHelper(0x01000000.toInt(), 0x01)
        testWriteRightZeroExtendedIntHelper(0x7f000000.toInt(), 0x7f)
        testWriteRightZeroExtendedIntHelper(0x80000000.toInt(), 0x80)
        testWriteRightZeroExtendedIntHelper(0xf0000000.toInt(), 0xf0)
        testWriteRightZeroExtendedIntHelper(0xff000000.toInt(), 0xff)

        testWriteRightZeroExtendedIntHelper(0x010000.toInt(), 0x01, 0x00)
        testWriteRightZeroExtendedIntHelper(0x01100000.toInt(), 0x10, 0x01)
        testWriteRightZeroExtendedIntHelper(0x7f100000.toInt(), 0x10, 0x7f)
        testWriteRightZeroExtendedIntHelper(0x80100000.toInt(), 0x10, 0x80)
        testWriteRightZeroExtendedIntHelper(0xf0100000.toInt(), 0x10, 0xf0)
        testWriteRightZeroExtendedIntHelper(0xff100000.toInt(), 0x10, 0xff)
        testWriteRightZeroExtendedIntHelper(0xff000000.toInt(), 0xff)

        testWriteRightZeroExtendedIntHelper(0x0100.toInt(), 0x01, 0x00, 0x00)
        testWriteRightZeroExtendedIntHelper(0x01101000.toInt(), 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedIntHelper(0x7f101000.toInt(), 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedIntHelper(0x80101000.toInt(), 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedIntHelper(0xf0101000.toInt(), 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedIntHelper(0xff101000.toInt(), 0x10, 0x10, 0xff)

        testWriteRightZeroExtendedIntHelper(0x01.toInt(), 0x01, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedIntHelper(0x80.toInt(), 0x80, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedIntHelper(0xff.toInt(), 0xff, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedIntHelper(0x01101010.toInt(), 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedIntHelper(0x7f101010.toInt(), 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedIntHelper(0x80101010.toInt(), 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedIntHelper(0xf0101010.toInt(), 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedIntHelper(0xff101010.toInt(), 0x10, 0x10, 0x10, 0xff)
    }

    private fun testWriteRightZeroExtendedLongHelper(longValue: Long, vararg encodedValue: Int) {
        setup()
        writer.writeRightZeroExtendedLong(ValueType.DOUBLE, longValue)

        val arr = IntArray(encodedValue.size+1)
        arr[0] = ValueType.DOUBLE or ((encodedValue.size - 1) shl 5)
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.size)
        expectData(*arr)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteRightZeroExtendedLong() {
        testWriteRightZeroExtendedLongHelper(0, 0x00)

        testWriteRightZeroExtendedLongHelper(0x0100000000000000L, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f00000000000000L, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8000000000000000uL.toLong(), 0x80)
        testWriteRightZeroExtendedLongHelper(0xf000000000000000uL.toLong(), 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff00000000000000uL.toLong(), 0xff)

        testWriteRightZeroExtendedLongHelper(0x01000000000000L, 0x01, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110000000000000L, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10000000000000L, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010000000000000uL.toLong(), 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010000000000000uL.toLong(), 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10000000000000uL.toLong(), 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fff000000000000L, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x010000000000L, 0x01, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110100000000000L, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10100000000000L, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010100000000000uL.toLong(), 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010100000000000uL.toLong(), 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10100000000000uL.toLong(), 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fffff0000000000L, 0xff, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x0100000000L, 0x01, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110101000000000L, 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10101000000000L, 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010101000000000uL.toLong(), 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010101000000000uL.toLong(), 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10101000000000uL.toLong(), 0x10, 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fffffff00000000L, 0xff, 0xff, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x01000000L, 0x01, 0x00, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110101010000000L, 0x10, 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10101010000000L, 0x10, 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010101010000000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010101010000000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10101010000000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fffffffff000000L, 0xff, 0xff, 0xff, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x010000L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010101010100000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010101010100000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10101010100000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fffffffffff0000L, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x0100L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010101010101000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010101010101000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10101010101000uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(0x7fffffffffffff00L, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)

        testWriteRightZeroExtendedLongHelper(0x01L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        testWriteRightZeroExtendedLongHelper(0x0110101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01)
        testWriteRightZeroExtendedLongHelper(0x7f10101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f)
        testWriteRightZeroExtendedLongHelper(0x8010101010101010uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x80)
        testWriteRightZeroExtendedLongHelper(0xf010101010101010uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0)
        testWriteRightZeroExtendedLongHelper(0xff10101010101010uL.toLong(), 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xff)
        testWriteRightZeroExtendedLongHelper(Long.MAX_VALUE, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)
        testWriteRightZeroExtendedLongHelper(Long.MIN_VALUE, 0x80)
        testWriteRightZeroExtendedLongHelper(-1, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff)
    }

    private fun testWriteStringHelper(stringValue: String, vararg encodedValue: Int) {
        setup()

        writer.writeString(stringValue)

        expectData(*encodedValue)
    }

    @Test
    @Throws(IOException::class)
    fun testWriteString() {
        testWriteStringHelper(String(charArrayOf(0x00.toChar())), 0xc0, 0x80)
        testWriteStringHelper(String(charArrayOf(0x01.toChar())), 0x01)
        testWriteStringHelper(String(charArrayOf(0x40.toChar())), 0x40)
        testWriteStringHelper(String(charArrayOf(0x7f.toChar())), 0x7f)
        testWriteStringHelper(String(charArrayOf(0x80.toChar())), 0xc2, 0x80)
        testWriteStringHelper(String(charArrayOf(0x81.toChar())), 0xc2, 0x81)
        testWriteStringHelper(String(charArrayOf(0x100.toChar())), 0xc4, 0x80)
        testWriteStringHelper(String(charArrayOf(0x7ff.toChar())), 0xdf, 0xbf)
        testWriteStringHelper(String(charArrayOf(0x800.toChar())), 0xe0, 0xa0, 0x80)
        testWriteStringHelper(String(charArrayOf(0x801.toChar())), 0xe0, 0xa0, 0x81)
        testWriteStringHelper(String(charArrayOf(0x1000.toChar())), 0xe1, 0x80, 0x80)
        testWriteStringHelper(String(charArrayOf(0x7fff.toChar())), 0xe7, 0xbf, 0xbf)
        testWriteStringHelper(String(charArrayOf(0x8000.toChar())), 0xe8, 0x80, 0x80)
        testWriteStringHelper(String(charArrayOf(0x8001.toChar())), 0xe8, 0x80, 0x81)
        testWriteStringHelper(String(charArrayOf(0xffff.toChar())), 0xef, 0xbf, 0xbf)
    }

    @Test
    @Throws(IOException::class)
    fun testAlign() {
        // create a new writer so we can start at file position 0
        startPosition = 0
        writer = DexDataWriter(output, startPosition, 256)

        writer.align()
        writer.write(1)
        writer.align()
        writer.align()

        writer.write(1)
        writer.write(2)
        writer.align()

        writer.write(1)
        writer.write(2)
        writer.write(3)
        writer.align()
        writer.align()

        writer.write(1)
        writer.write(2)
        writer.write(3)
        writer.write(4)
        writer.align()
        writer.align()
        writer.align()
        writer.align()

        writer.write(1)
        writer.align()

        expectData(0x01, 0x00, 0x00, 0x00,
                   0x01, 0x02, 0x00, 0x00,
                   0x01, 0x02, 0x03, 0x00,
                   0x01, 0x02, 0x03, 0x04,
                   0x01, 0x00, 0x00, 0x00)
    }
}
