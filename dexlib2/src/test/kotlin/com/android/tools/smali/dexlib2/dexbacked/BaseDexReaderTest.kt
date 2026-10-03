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

class BaseDexReaderTest {
    @Test
    fun testSizedInt() {
        performSizedIntTest(0.toInt(), byteArrayOf(0x00.toByte()))
        performSizedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte()))
        performSizedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedIntTest(1.toInt(), byteArrayOf(0x01.toByte()))
        performSizedIntTest(1.toInt(), byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedIntTest(0x40.toInt(), byteArrayOf(0x40.toByte()))
        performSizedIntTest(0x7f.toInt(), byteArrayOf(0x7f.toByte()))
        performSizedIntTest(0xffffff80.toInt(), byteArrayOf(0x80.toByte()))
        performSizedIntTest(-1.toInt(), byteArrayOf(0xff.toByte()))

        performSizedIntTest(0x100.toInt(), byteArrayOf(0x00.toByte(), 0x01.toByte()))
        performSizedIntTest(0x110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte()))
        performSizedIntTest(0x7f01.toInt(), byteArrayOf(0x01.toByte(), 0x7f.toByte()))
        performSizedIntTest(0xffff8001.toInt(), byteArrayOf(0x01.toByte(), 0x80.toByte()))
        performSizedIntTest(0xffffff10.toInt(), byteArrayOf(0x10.toByte(), 0xff.toByte()))

        performSizedIntTest(0x11001.toInt(), byteArrayOf(0x01.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedIntTest(0x7f0110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte(), 0x7f.toByte()))
        performSizedIntTest(0xff801001.toInt(), byteArrayOf(0x01.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedIntTest(0xffff0110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte(), 0xff.toByte()))

        performSizedIntTest(0x1003002.toInt(), byteArrayOf(0x02.toByte(), 0x30.toByte(), 0x00.toByte(), 0x01.toByte()))
        performSizedIntTest(0x7f110230.toInt(), byteArrayOf(0x30.toByte(), 0x02.toByte(), 0x11.toByte(), 0x7f.toByte()))
        performSizedIntTest(0x80112233.toInt(), byteArrayOf(0x33.toByte(), 0x22.toByte(), 0x11.toByte(), 0x80.toByte()))
        performSizedIntTest(-1.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
    }

    private fun performSizedIntTest(expectedValue: Int, buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSizedInt(buf.size))
    }

    @Test
    fun testSizedIntFailure() {
        // wrong size
        performSizedIntFailureTest(byteArrayOf())
        performSizedIntFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedIntFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedIntFailureTest(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()))
    }

    private fun performSizedIntFailureTest(buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        try {
            reader.readSizedInt(buf.size)
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected
    }
    }

    @Test
    fun testSizedSmallUint() {
        performSizedSmallUintTest(0.toInt(), byteArrayOf(0x00.toByte()))
        performSizedSmallUintTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintTest(1.toInt(), byteArrayOf(0x01.toByte()))
        performSizedSmallUintTest(1.toInt(), byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintTest(0x40.toInt(), byteArrayOf(0x40.toByte()))
        performSizedSmallUintTest(0x7f.toInt(), byteArrayOf(0x7f.toByte()))
        performSizedSmallUintTest(0x80.toInt(), byteArrayOf(0x80.toByte()))
        performSizedSmallUintTest(0xff.toInt(), byteArrayOf(0xff.toByte()))

        performSizedSmallUintTest(0x100.toInt(), byteArrayOf(0x00.toByte(), 0x01.toByte()))
        performSizedSmallUintTest(0x110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte()))
        performSizedSmallUintTest(0x7f01.toInt(), byteArrayOf(0x01.toByte(), 0x7f.toByte()))
        performSizedSmallUintTest(0x8001.toInt(), byteArrayOf(0x01.toByte(), 0x80.toByte()))
        performSizedSmallUintTest(0xff10.toInt(), byteArrayOf(0x10.toByte(), 0xff.toByte()))

        performSizedSmallUintTest(0x11001.toInt(), byteArrayOf(0x01.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedSmallUintTest(0x7f0110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte(), 0x7f.toByte()))
        performSizedSmallUintTest(0x801001.toInt(), byteArrayOf(0x01.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedSmallUintTest(0xff0110.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte(), 0xff.toByte()))

        performSizedSmallUintTest(0x1003002.toInt(), byteArrayOf(0x02.toByte(), 0x30.toByte(), 0x00.toByte(), 0x01.toByte()))
        performSizedSmallUintTest(0x7f110230.toInt(), byteArrayOf(0x30.toByte(), 0x02.toByte(), 0x11.toByte(), 0x7f.toByte()))
        performSizedSmallUintTest(Integer.MAX_VALUE, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))
    }

    private fun performSizedSmallUintTest(expectedValue: Int, buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSizedSmallUint(buf.size))
    }

    @Test
    fun testSizedSmallUintFailure() {
        // wrong size
        performSizedSmallUintFailureTest(byteArrayOf())
        performSizedSmallUintFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedSmallUintFailureTest(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()))

        // MSB set
        performSizedSmallUintFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x80.toByte()))
        performSizedSmallUintFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
    }

    private fun performSizedSmallUintFailureTest(buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        try {
            reader.readSizedSmallUint(buf.size)
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected
    }
    }

    @Test
    fun testSizedRightExtendedInt() {
        performSizedRightExtendedIntTest(0.toInt(), byteArrayOf(0x00.toByte()))
        performSizedRightExtendedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))

        performSizedRightExtendedIntTest(0x01000000.toInt(), byteArrayOf(0x01.toByte()))
        performSizedRightExtendedIntTest(0x7f000000.toInt(), byteArrayOf(0x7f.toByte()))
        performSizedRightExtendedIntTest(0x80000000.toInt(), byteArrayOf(0x80.toByte()))
        performSizedRightExtendedIntTest(0xf0000000.toInt(), byteArrayOf(0xf0.toByte()))
        performSizedRightExtendedIntTest(0xff000000.toInt(), byteArrayOf(0xff.toByte()))

        performSizedRightExtendedIntTest(0x010000.toInt(), byteArrayOf(0x01.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0x01100000.toInt(), byteArrayOf(0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedIntTest(0x7f100000.toInt(), byteArrayOf(0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedIntTest(0x80100000.toInt(), byteArrayOf(0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedIntTest(0xf0100000.toInt(), byteArrayOf(0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedIntTest(0xff100000.toInt(), byteArrayOf(0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedIntTest(0xff000000.toInt(), byteArrayOf(0x00.toByte(), 0xff.toByte()))

        performSizedRightExtendedIntTest(0x0100.toInt(), byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0x01101000.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedIntTest(0x7f101000.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedIntTest(0x80101000.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedIntTest(0xf0101000.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedIntTest(0xff101000.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedIntTest(0xff000000.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xff.toByte()))

        performSizedRightExtendedIntTest(0x01.toInt(), byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0x80.toInt(), byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0xff.toInt(), byteArrayOf(0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntTest(0x01101010.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedIntTest(0x7f101010.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedIntTest(0x80101010.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedIntTest(0xf0101010.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedIntTest(0xff101010.toInt(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedIntTest(0xff000000.toInt(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
    }

    private fun performSizedRightExtendedIntTest(expectedValue: Int, buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSizedRightExtendedInt(buf.size))
    }

    @Test
    fun testSizedRightExtendedIntFailure() {
        // wrong size
        performSizedRightExtendedIntFailureTest(byteArrayOf())
        performSizedRightExtendedIntFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedIntFailureTest(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()))
    }

    private fun performSizedRightExtendedIntFailureTest(buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        try {
            reader.readSizedRightExtendedInt(buf.size)
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected
    }
    }

    @Test
    fun testSizedRightExtendedLong() {
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))

        performSizedRightExtendedLongTest(0x0100000000000000L, byteArrayOf(0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f00000000000000L, byteArrayOf(0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8000000000000000uL.toLong(), byteArrayOf(0x80.toByte()))
        performSizedRightExtendedLongTest(0xf000000000000000uL.toLong(), byteArrayOf(0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0xff.toByte()))

        performSizedRightExtendedLongTest(0x01000000000000L, byteArrayOf(0x01.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110000000000000L, byteArrayOf(0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10000000000000L, byteArrayOf(0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010000000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010000000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10000000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fff000000000000L, byteArrayOf(0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x010000000000L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110100000000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10100000000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010100000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010100000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10100000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fffff0000000000L, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x0100000000L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110101000000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10101000000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010101000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010101000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10101000000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fffffff00000000L, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x01000000L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110101010000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10101010000000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010101010000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010101010000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10101010000000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fffffffff000000L, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x010000L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110101010100000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10101010100000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010101010100000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010101010100000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10101010100000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fffffffffff0000L, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x0100L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110101010101000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10101010101000L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010101010101000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010101010101000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10101010101000uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0x7fffffffffffff00L, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedRightExtendedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongTest(0x0110101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedRightExtendedLongTest(0x7f10101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(0x8010101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(0xf010101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedRightExtendedLongTest(0xff10101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedRightExtendedLongTest(Long.MAX_VALUE, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))
        performSizedRightExtendedLongTest(Long.MIN_VALUE, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x80.toByte()))
        performSizedRightExtendedLongTest(-1.toLong(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
    }

    private fun performSizedRightExtendedLongTest(expectedValue: Long, buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSizedRightExtendedLong(buf.size))
    }

    @Test
    fun testSizedRightExtendedLongFailure() {
        // wrong size
        performSizedRightExtendedLongFailureTest(byteArrayOf())
        performSizedRightExtendedLongFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedRightExtendedLongFailureTest(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), 0x89.toByte(), 0x90.toByte(), 0x01.toByte()))
    }

    private fun performSizedRightExtendedLongFailureTest(buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        try {
            reader.readSizedRightExtendedLong(buf.size)
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected
    }
    }

    @Test
    fun testSizedLong() {
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte()))
        performSizedLongTest(0x7fL, byteArrayOf(0x7f.toByte()))
        performSizedLongTest(0xffffffffffffff80uL.toLong(), byteArrayOf(0x80.toByte()))
        performSizedLongTest(0xfffffffffffffff0uL.toLong(), byteArrayOf(0xf0.toByte()))
        performSizedLongTest(0xffffffffffffffffuL.toLong(), byteArrayOf(0xff.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte()))
        performSizedLongTest(0x0110L, byteArrayOf(0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f10L, byteArrayOf(0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xffffffffffff8010uL.toLong(), byteArrayOf(0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfffffffffffff010uL.toLong(), byteArrayOf(0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffffffffffffff10uL.toLong(), byteArrayOf(0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffffffffffffff00uL.toLong(), byteArrayOf(0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffL, byteArrayOf(0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x011010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f1010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xffffffffff801010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfffffffffff01010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffffffffffff1010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffffffffffff0000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffffL, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x01101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xffffffff80101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfffffffff0101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffffffffff101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffffffffff000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffffffL, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01.toLong(), byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x0110101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f10101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xffffff8010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfffffff010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffffffff10101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffffffff00000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffffffffL, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x011010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f1010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xffff801010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfffff01010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffffff1010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffffff0000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffffffffffL, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x01101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0xff80101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xfff0101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xffff101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xffff000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(0x7fffffffffffffL, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))

        performSizedLongTest(0x01L, byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongTest(0x0110101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x01.toByte()))
        performSizedLongTest(0x7f10101010101010L, byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x7f.toByte()))
        performSizedLongTest(0x8010101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x80.toByte()))
        performSizedLongTest(0xf010101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xf0.toByte()))
        performSizedLongTest(0xff10101010101010uL.toLong(), byteArrayOf(0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0x10.toByte(), 0xff.toByte()))
        performSizedLongTest(0xff00000000000000uL.toLong(), byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte()))
        performSizedLongTest(Long.MAX_VALUE, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte()))
        performSizedLongTest(Long.MIN_VALUE, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x80.toByte()))
        performSizedLongTest(-1.toLong(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
    }

    private fun performSizedLongTest(expectedValue: Long, buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSizedLong(buf.size))
    }

    @Test
    fun testSizedLongFailure() {
        // wrong size
        performSizedLongFailureTest(byteArrayOf())
        performSizedLongFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongFailureTest(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        performSizedLongFailureTest(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), 0x89.toByte(), 0x90.toByte(), 0x01.toByte()))
    }

    private fun performSizedLongFailureTest(buf: ByteArray) {
        val dexBuf = DexBuffer(buf)
        val reader = dexBuf.readerAt(0)
        try {
            reader.readSizedLong(buf.size)
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected
    }
    }
}
