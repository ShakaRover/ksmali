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

open class DexBuffer constructor(
    val buf: ByteArray,
    val baseOffset: Int = 0
) {
    fun readSmallUint(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        val result = (buf[o].toInt() and 0xff) or
            ((buf[o + 1].toInt() and 0xff) shl 8) or
            ((buf[o + 2].toInt() and 0xff) shl 16) or
            (buf[o + 3].toInt() shl 24)
        if (result < 0) {
            throw ExceptionWithContext(
                "Encountered small uint that is out of range at offset 0x%x", o
            )
        }
        return result
    }

    fun readOptionalUint(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        val result = (buf[o].toInt() and 0xff) or
            ((buf[o + 1].toInt() and 0xff) shl 8) or
            ((buf[o + 2].toInt() and 0xff) shl 16) or
            (buf[o + 3].toInt() shl 24)
        if (result < -1) {
            throw ExceptionWithContext(
                "Encountered optional uint that is out of range at offset 0x%x", o
            )
        }
        return result
    }

    fun readUshort(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        return (buf[o].toInt() and 0xff) or
            ((buf[o + 1].toInt() and 0xff) shl 8)
    }

    fun readUbyte(offset: Int): Int {
        return buf[offset + baseOffset].toInt() and 0xff
    }

    fun readLong(offset: Int): Long {
        val buf = this.buf
        val o = offset + baseOffset
        return (buf[o].toLong() and 0xffL) or
            ((buf[o + 1].toLong() and 0xffL) shl 8) or
            ((buf[o + 2].toLong() and 0xffL) shl 16) or
            ((buf[o + 3].toLong() and 0xffL) shl 24) or
            ((buf[o + 4].toLong() and 0xffL) shl 32) or
            ((buf[o + 5].toLong() and 0xffL) shl 40) or
            ((buf[o + 6].toLong() and 0xffL) shl 48) or
            (buf[o + 7].toLong() shl 56)
    }

    fun readLongAsSmallUint(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        val result = (buf[o].toLong() and 0xffL) or
            ((buf[o + 1].toLong() and 0xffL) shl 8) or
            ((buf[o + 2].toLong() and 0xffL) shl 16) or
            ((buf[o + 3].toLong() and 0xffL) shl 24) or
            ((buf[o + 4].toLong() and 0xffL) shl 32) or
            ((buf[o + 5].toLong() and 0xffL) shl 40) or
            ((buf[o + 6].toLong() and 0xffL) shl 48) or
            (buf[o + 7].toLong() shl 56)
        if (result < 0 || result > Int.MAX_VALUE) {
            throw ExceptionWithContext("Encountered out-of-range ulong at offset 0x%x", o)
        }
        return result.toInt()
    }

    fun readInt(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        return (buf[o].toInt() and 0xff) or
            ((buf[o + 1].toInt() and 0xff) shl 8) or
            ((buf[o + 2].toInt() and 0xff) shl 16) or
            (buf[o + 3].toInt() shl 24)
    }

    fun readShort(offset: Int): Int {
        val buf = this.buf
        val o = offset + baseOffset
        return (buf[o].toInt() and 0xff) or
            (buf[o + 1].toInt() shl 8)
    }

    fun readByte(offset: Int): Int {
        return buf[baseOffset + offset].toInt()
    }

    fun readByteRange(start: Int, length: Int): ByteArray {
        return buf.copyOfRange(baseOffset + start, baseOffset + start + length)
    }

    fun readerAt(offset: Int): DexReader<DexBuffer> {
        return DexReader(this, offset)
    }
}
