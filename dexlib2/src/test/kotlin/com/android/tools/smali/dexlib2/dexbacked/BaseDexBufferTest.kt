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

import com.android.tools.smali.util.ExceptionWithContext
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

class BaseDexBufferTest {
    @Test
    fun testReadSmallUintSuccess() {
        var dexBuf = DexBuffer(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        Assert.assertEquals(0x44332211, dexBuf.readSmallUint(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        Assert.assertEquals(0, dexBuf.readSmallUint(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f))
        Assert.assertEquals(0x7fffffff, dexBuf.readSmallUint(0))
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadSmallUintTooLarge1() {
        val dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadSmallUintTooLarge2() {
        val dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x80.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadSmallUintTooLarge3() {
        val dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test
    fun testReadOptionalUintSuccess() {
        var dexBuf = DexBuffer(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        Assert.assertEquals(0x44332211, dexBuf.readSmallUint(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        Assert.assertEquals(0, dexBuf.readSmallUint(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f))
        Assert.assertEquals(0x7fffffff, dexBuf.readSmallUint(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        Assert.assertEquals(-1, dexBuf.readOptionalUint(0))
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadOptionalUintTooLarge1() {
        val dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadOptionalUintTooLarge2() {
        val dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x80.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test(expected = ExceptionWithContext::class)
    fun testReadOptionalUintTooLarge3() {
        val dexBuf = DexBuffer(byteArrayOf(0xfe.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        dexBuf.readSmallUint(0)
    }

    @Test
    fun testReadUshort() {
        var dexBuf = DexBuffer(byteArrayOf(0x11, 0x22))
        Assert.assertEquals(dexBuf.readUshort(0), 0x2211)

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00))
        Assert.assertEquals(dexBuf.readUshort(0), 0)

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte()))
        Assert.assertEquals(dexBuf.readUshort(0), 0xffff)

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x80.toByte()))
        Assert.assertEquals(dexBuf.readUshort(0), 0x8000)

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0x7f))
        Assert.assertEquals(dexBuf.readUshort(0), 0x7fff)
    }

    @Test
    fun testReadUbyte() {
        val buf = ByteArray(1)
        val dexBuf = DexBuffer(buf)

        for (i in 0..0xff) {
            buf[0] = i.toByte()
            Assert.assertEquals(i, dexBuf.readUbyte(0))
        }
    }

    @Test
    fun testReadLong() {
        var dexBuf = DexBuffer(byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77))
        Assert.assertEquals(0x7766554433221100L, dexBuf.readLong(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        Assert.assertEquals(0L, dexBuf.readLong(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f))
        Assert.assertEquals(Long.MAX_VALUE, dexBuf.readLong(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80.toByte()))
        Assert.assertEquals(Long.MIN_VALUE, dexBuf.readLong(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x80.toByte()))
        Assert.assertEquals(0x80ffffffffffffffuL.toLong(), dexBuf.readLong(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        Assert.assertEquals(-1L, dexBuf.readLong(0))
    }

    @Test
    fun testReadInt() {
        var dexBuf = DexBuffer(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        Assert.assertEquals(0x44332211, dexBuf.readInt(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        Assert.assertEquals(0, dexBuf.readInt(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f))
        Assert.assertEquals(Integer.MAX_VALUE, dexBuf.readInt(0))

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte()))
        Assert.assertEquals(Integer.MIN_VALUE, dexBuf.readInt(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x80.toByte()))
        Assert.assertEquals(0x80ffffff.toInt(), dexBuf.readInt(0))

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        Assert.assertEquals(-1, dexBuf.readInt(0))
    }

    @Test
    fun testReadShort() {
        var dexBuf = DexBuffer(byteArrayOf(0x11, 0x22))
        Assert.assertEquals(dexBuf.readShort(0), 0x2211)

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x00))
        Assert.assertEquals(dexBuf.readShort(0), 0)

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0xff.toByte()))
        Assert.assertEquals(dexBuf.readShort(0), -1)

        dexBuf = DexBuffer(byteArrayOf(0x00, 0x80.toByte()))
        Assert.assertEquals(dexBuf.readShort(0), Short.MIN_VALUE.toInt())

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0x7f))
        Assert.assertEquals(dexBuf.readShort(0), 0x7fff)

        dexBuf = DexBuffer(byteArrayOf(0xff.toByte(), 0x80.toByte()))
        Assert.assertEquals(dexBuf.readShort(0), 0xffff80ff.toInt())
    }

    @Test
    fun testReadByte() {
        val buf = ByteArray(1)
        val dexBuf = DexBuffer(buf)

        for (i in 0..0xff) {
            buf[0] = i.toByte()
            Assert.assertEquals(i.toByte().toLong(), dexBuf.readByte(0).toLong())
        }
    }

    @Test
    fun testReadRandom() {
        val r = Random(1234567890)
        val byteBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        val buf = ByteArray(4)
        val dexBuf = DexBuffer(buf)

        for (i in 0 until 10000) {
            val value = r.nextInt()
            byteBuf.putInt(0, value)
            byteBuf.position(0)
            byteBuf.get(buf)

            val expectException = value < 0
            try {
                val returnedVal = dexBuf.readSmallUint(0)
                Assert.assertFalse("Didn't throw an exception for value: %x".format(value), expectException)
                Assert.assertEquals(value, returnedVal)
            } catch (ex: Exception) {
                Assert.assertTrue("Threw an exception for value: %x".format(value), expectException)
            }

            Assert.assertEquals(value, dexBuf.readInt(0))

            Assert.assertEquals(value and 0xFFFF, dexBuf.readUshort(0))
            Assert.assertEquals((value shr 8) and 0xFFFF, dexBuf.readUshort(1))
            Assert.assertEquals((value shr 16) and 0xFFFF, dexBuf.readUshort(2))

            Assert.assertEquals(value.toShort().toInt(), dexBuf.readShort(0))
            Assert.assertEquals((value shr 8).toShort().toInt(), dexBuf.readShort(1))
            Assert.assertEquals((value shr 16).toShort().toInt(), dexBuf.readShort(2))
        }
    }

    @Test
    fun testReadLongRandom() {
        val r = Random(1234567890)
        val byteBuf = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
        val buf = ByteArray(8)
        val dexBuf = DexBuffer(buf)

        for (i in 0 until 10000) {
            val value = r.nextInt().toLong()
            byteBuf.putLong(0, value)
            byteBuf.position(0)
            byteBuf.get(buf)

            Assert.assertEquals(value, dexBuf.readLong(0))
        }
    }
}
