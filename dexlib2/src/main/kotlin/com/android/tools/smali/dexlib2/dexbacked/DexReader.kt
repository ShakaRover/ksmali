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
import com.android.tools.smali.util.Utf8Utils

class DexReader<T : DexBuffer>(
    val dexBuf: T,
    offset: Int
) {
    var offset: Int = offset

    fun readSleb128(): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result <= 0x7f) {
            result = (result shl 25) shr 25
        } else {
            currentByteValue = buf[end++].toInt() and 0xff
            result = (result and 0x7f) or ((currentByteValue and 0x7f) shl 7)
            if (currentByteValue <= 0x7f) {
                result = (result shl 18) shr 18
            } else {
                currentByteValue = buf[end++].toInt() and 0xff
                result = result or ((currentByteValue and 0x7f) shl 14)
                if (currentByteValue <= 0x7f) {
                    result = (result shl 11) shr 11
                } else {
                    currentByteValue = buf[end++].toInt() and 0xff
                    result = result or ((currentByteValue and 0x7f) shl 21)
                    if (currentByteValue <= 0x7f) {
                        result = (result shl 4) shr 4
                    } else {
                        currentByteValue = buf[end++].toInt() and 0xff
                        if (currentByteValue > 0x7f) {
                            throw ExceptionWithContext(
                                "Invalid sleb128 integer encountered at offset 0x%x", offset
                            )
                        }
                        result = result or (currentByteValue shl 28)
                    }
                }
            }
        }

        offset = end - dexBuf.baseOffset
        return result
    }

    fun peekSleb128Size(): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result > 0x7f) {
            currentByteValue = buf[end++].toInt() and 0xff
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++].toInt() and 0xff
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++].toInt() and 0xff
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++].toInt() and 0xff
                        if (currentByteValue > 0x7f) {
                            throw ExceptionWithContext(
                                "Invalid sleb128 integer encountered at offset 0x%x", offset
                            )
                        }
                    }
                }
            }
        }

        return end - (dexBuf.baseOffset + offset)
    }

    fun readSmallUleb128(): Int {
        return readUleb128(false)
    }

    fun peekSmallUleb128Size(): Int {
        return peekUleb128Size(false)
    }

    private fun readUleb128(allowLarge: Boolean): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result > 0x7f) {
            currentByteValue = buf[end++].toInt() and 0xff
            result = (result and 0x7f) or ((currentByteValue and 0x7f) shl 7)
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++].toInt() and 0xff
                result = result or ((currentByteValue and 0x7f) shl 14)
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++].toInt() and 0xff
                    result = result or ((currentByteValue and 0x7f) shl 21)
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++].toInt()

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw ExceptionWithContext(
                                "Invalid uleb128 integer encountered at offset 0x%x", offset
                            )
                        } else if ((currentByteValue and 0xf) > 0x07) {
                            if (!allowLarge) {
                                // for non-large uleb128s, we assume most significant bit of the result will not be
                                // set, so that it can fit into a signed integer without wrapping
                                throw ExceptionWithContext(
                                    "Encountered valid uleb128 that is out of range at offset 0x%x",
                                    offset
                                )
                            }
                        }
                        result = result or (currentByteValue shl 28)
                    }
                }
            }
        }

        offset = end - dexBuf.baseOffset
        return result
    }

    private fun peekUleb128Size(allowLarge: Boolean): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result > 0x7f) {
            currentByteValue = buf[end++].toInt() and 0xff
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++].toInt() and 0xff
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++].toInt() and 0xff
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++].toInt()

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw ExceptionWithContext(
                                "Invalid uleb128 integer encountered at offset 0x%x", offset
                            )
                        } else if ((currentByteValue and 0xf) > 0x07) {
                            if (!allowLarge) {
                                // for non-large uleb128s, we assume most significant bit of the result will not be
                                // set, so that it can fit into a signed integer without wrapping
                                throw ExceptionWithContext(
                                    "Encountered valid uleb128 that is out of range at offset 0x%x",
                                    offset
                                )
                            }
                        }
                    }
                }
            }
        }

        return end - (dexBuf.baseOffset + offset)
    }

    /**
     * Reads a "large" uleb128. That is, one that may legitimately be greater than a signed int.
     *
     * The value is returned as if it were signed. i.e. a value of 0xFFFFFFFF would be returned as -1. It is up to the
     * caller to handle the value appropriately.
     */
    fun readLargeUleb128(): Int {
        return readUleb128(true)
    }

    /**
     * Reads a "big" uleb128 that can legitimately be > 2^31. The value is returned as a signed integer, with the
     * expected semantics of re-interpreting an unsigned value as a signed value.
     *
     * @return The unsigned value, reinterpreted as a signed int
     */
    fun readBigUleb128(): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result > 0x7f) {
            currentByteValue = buf[end++].toInt() and 0xff
            result = (result and 0x7f) or ((currentByteValue and 0x7f) shl 7)
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++].toInt() and 0xff
                result = result or ((currentByteValue and 0x7f) shl 14)
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++].toInt() and 0xff
                    result = result or ((currentByteValue and 0x7f) shl 21)
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++].toInt()

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw ExceptionWithContext(
                                "Invalid uleb128 integer encountered at offset 0x%x", offset
                            )
                        }
                        result = result or (currentByteValue shl 28)
                    }
                }
            }
        }

        offset = end - dexBuf.baseOffset
        return result
    }

    fun peekBigUleb128Size(): Int {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Int
        var result: Int
        val buf = dexBuf.buf

        result = buf[end++].toInt() and 0xff
        if (result > 0x7f) {
            currentByteValue = buf[end++].toInt() and 0xff
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++].toInt() and 0xff
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++].toInt() and 0xff
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++].toInt()

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw ExceptionWithContext(
                                "Invalid uleb128 integer encountered at offset 0x%x", offset
                            )
                        }
                    }
                }
            }
        }

        return end - (dexBuf.baseOffset + offset)
    }

    fun skipUleb128() {
        var end = dexBuf.baseOffset + offset
        var currentByteValue: Byte
        val buf = dexBuf.buf

        currentByteValue = buf[end++]
        if (currentByteValue < 0) { // if the MSB is set
            currentByteValue = buf[end++]
            if (currentByteValue < 0) { // if the MSB is set
                currentByteValue = buf[end++]
                if (currentByteValue < 0) { // if the MSB is set
                    currentByteValue = buf[end++]
                    if (currentByteValue < 0) { // if the MSB is set
                        currentByteValue = buf[end++]
                        if (currentByteValue < 0) {
                            throw ExceptionWithContext(
                                "Invalid uleb128 integer encountered at offset 0x%x", offset
                            )
                        }
                    }
                }
            }
        }

        offset = end - dexBuf.baseOffset
    }

    fun readSmallUint(): Int {
        val o = offset
        val result = dexBuf.readSmallUint(o)
        offset = o + 4
        return result
    }

    fun readOptionalUint(): Int {
        val o = offset
        val result = dexBuf.readOptionalUint(o)
        offset = o + 4
        return result
    }

    fun peekUshort(): Int {
        return dexBuf.readUshort(offset)
    }

    fun readUshort(): Int {
        val o = offset
        val result = dexBuf.readUshort(offset)
        offset = o + 2
        return result
    }

    fun peekUbyte(): Int {
        return dexBuf.readUbyte(offset)
    }

    fun readUbyte(): Int {
        val o = offset
        val result = dexBuf.readUbyte(offset)
        offset = o + 1
        return result
    }

    fun readLong(): Long {
        val o = offset
        val result = dexBuf.readLong(offset)
        offset = o + 8
        return result
    }

    fun readInt(): Int {
        val o = offset
        val result = dexBuf.readInt(offset)
        offset = o + 4
        return result
    }

    fun readShort(): Int {
        val o = offset
        val result = dexBuf.readShort(offset)
        offset = o + 2
        return result
    }

    fun readByte(): Int {
        val o = offset
        val result = dexBuf.readByte(offset)
        offset = o + 1
        return result
    }

    fun skipByte() {
        offset++
    }

    fun moveRelative(i: Int) {
        offset += i
    }

    fun readSmallUint(offset: Int): Int {
        return dexBuf.readSmallUint(offset)
    }

    fun readUshort(offset: Int): Int {
        return dexBuf.readUshort(offset)
    }

    fun readUbyte(offset: Int): Int {
        return dexBuf.readUbyte(offset)
    }

    fun readLong(offset: Int): Long {
        return dexBuf.readLong(offset)
    }

    fun readInt(offset: Int): Int {
        return dexBuf.readInt(offset)
    }

    fun readShort(offset: Int): Int {
        return dexBuf.readShort(offset)
    }

    fun readByte(offset: Int): Int {
        return dexBuf.readByte(offset)
    }

    fun readSizedInt(bytes: Int): Int {
        val o = dexBuf.baseOffset + offset
        val buf = dexBuf.buf

        val result: Int = when (bytes) {
            4 ->
                (buf[o].toInt() and 0xff) or
                    ((buf[o + 1].toInt() and 0xff) shl 8) or
                    ((buf[o + 2].toInt() and 0xff) shl 16) or
                    (buf[o + 3].toInt() shl 24)
            3 ->
                (buf[o].toInt() and 0xff) or
                    ((buf[o + 1].toInt() and 0xff) shl 8) or
                    (buf[o + 2].toInt() shl 16)
            2 ->
                (buf[o].toInt() and 0xff) or
                    (buf[o + 1].toInt() shl 8)
            1 -> buf[o].toInt()
            else -> throw ExceptionWithContext(
                "Invalid size %d for sized int at offset 0x%x", bytes, offset
            )
        }
        offset = o + bytes - dexBuf.baseOffset
        return result
    }

    fun readSizedSmallUint(bytes: Int): Int {
        val o = dexBuf.baseOffset + offset
        val buf = dexBuf.buf

        var result = 0
        when (bytes) {
            4 -> {
                val b = buf[o + 3]
                if (b < 0) {
                    throw ExceptionWithContext(
                        "Encountered valid sized uint that is out of range at offset 0x%x",
                        offset
                    )
                }
                result = b.toInt() shl 24
                result = result or ((buf[o + 2].toInt() and 0xff) shl 16)
                result = result or ((buf[o + 1].toInt() and 0xff) shl 8)
                result = result or (buf[o].toInt() and 0xff)
            }
            3 -> {
                result = result or ((buf[o + 2].toInt() and 0xff) shl 16)
                result = result or ((buf[o + 1].toInt() and 0xff) shl 8)
                result = result or (buf[o].toInt() and 0xff)
            }
            2 -> {
                result = result or ((buf[o + 1].toInt() and 0xff) shl 8)
                result = result or (buf[o].toInt() and 0xff)
            }
            1 -> {
                result = result or (buf[o].toInt() and 0xff)
            }
            else -> throw ExceptionWithContext(
                "Invalid size %d for sized uint at offset 0x%x", bytes, offset
            )
        }
        offset = o + bytes - dexBuf.baseOffset
        return result
    }

    fun readSizedRightExtendedInt(bytes: Int): Int {
        val o = dexBuf.baseOffset + offset
        val buf = dexBuf.buf

        val result: Int = when (bytes) {
            4 ->
                (buf[o].toInt() and 0xff) or
                    ((buf[o + 1].toInt() and 0xff) shl 8) or
                    ((buf[o + 2].toInt() and 0xff) shl 16) or
                    (buf[o + 3].toInt() shl 24)
            3 ->
                ((buf[o].toInt() and 0xff) shl 8) or
                    ((buf[o + 1].toInt() and 0xff) shl 16) or
                    (buf[o + 2].toInt() shl 24)
            2 ->
                ((buf[o].toInt() and 0xff) shl 16) or
                    (buf[o + 1].toInt() shl 24)
            1 -> buf[o].toInt() shl 24
            else -> throw ExceptionWithContext(
                "Invalid size %d for sized, right extended int at offset 0x%x", bytes, offset
            )
        }
        offset = o + bytes - dexBuf.baseOffset
        return result
    }

    fun readSizedRightExtendedLong(bytes: Int): Long {
        val o = dexBuf.baseOffset + offset
        val buf = dexBuf.buf

        val result: Long = when (bytes) {
            8 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    ((buf[o + 3].toLong() and 0xffL) shl 24) or
                    ((buf[o + 4].toLong() and 0xffL) shl 32) or
                    ((buf[o + 5].toLong() and 0xffL) shl 40) or
                    ((buf[o + 6].toLong() and 0xffL) shl 48) or
                    (buf[o + 7].toLong() shl 56)
            7 ->
                ((buf[o].toLong() and 0xffL) shl 8) or
                    ((buf[o + 1].toLong() and 0xffL) shl 16) or
                    ((buf[o + 2].toLong() and 0xffL) shl 24) or
                    ((buf[o + 3].toLong() and 0xffL) shl 32) or
                    ((buf[o + 4].toLong() and 0xffL) shl 40) or
                    ((buf[o + 5].toLong() and 0xffL) shl 48) or
                    (buf[o + 6].toLong() shl 56)
            6 ->
                ((buf[o].toLong() and 0xffL) shl 16) or
                    ((buf[o + 1].toLong() and 0xffL) shl 24) or
                    ((buf[o + 2].toLong() and 0xffL) shl 32) or
                    ((buf[o + 3].toLong() and 0xffL) shl 40) or
                    ((buf[o + 4].toLong() and 0xffL) shl 48) or
                    (buf[o + 5].toLong() shl 56)
            5 ->
                ((buf[o].toLong() and 0xffL) shl 24) or
                    ((buf[o + 1].toLong() and 0xffL) shl 32) or
                    ((buf[o + 2].toLong() and 0xffL) shl 40) or
                    ((buf[o + 3].toLong() and 0xffL) shl 48) or
                    (buf[o + 4].toLong() shl 56)
            4 ->
                ((buf[o].toLong() and 0xffL) shl 32) or
                    ((buf[o + 1].toLong() and 0xffL) shl 40) or
                    ((buf[o + 2].toLong() and 0xffL) shl 48) or
                    (buf[o + 3].toLong() shl 56)
            3 ->
                ((buf[o].toLong() and 0xffL) shl 40) or
                    ((buf[o + 1].toLong() and 0xffL) shl 48) or
                    (buf[o + 2].toLong() shl 56)
            2 ->
                ((buf[o].toLong() and 0xffL) shl 48) or
                    (buf[o + 1].toLong() shl 56)
            1 -> buf[o].toLong() shl 56
            else -> throw ExceptionWithContext(
                "Invalid size %d for sized, right extended long at offset 0x%x", bytes, offset
            )
        }
        offset = o + bytes - dexBuf.baseOffset
        return result
    }

    fun readSizedLong(bytes: Int): Long {
        val o = dexBuf.baseOffset + offset
        val buf = dexBuf.buf

        val result: Long = when (bytes) {
            8 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    ((buf[o + 3].toLong() and 0xffL) shl 24) or
                    ((buf[o + 4].toLong() and 0xffL) shl 32) or
                    ((buf[o + 5].toLong() and 0xffL) shl 40) or
                    ((buf[o + 6].toLong() and 0xffL) shl 48) or
                    (buf[o + 7].toLong() shl 56)
            7 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    ((buf[o + 3].toLong() and 0xffL) shl 24) or
                    ((buf[o + 4].toLong() and 0xffL) shl 32) or
                    ((buf[o + 5].toLong() and 0xffL) shl 40) or
                    (buf[o + 6].toLong() shl 48)
            6 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    ((buf[o + 3].toLong() and 0xffL) shl 24) or
                    ((buf[o + 4].toLong() and 0xffL) shl 32) or
                    (buf[o + 5].toLong() shl 40)
            5 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    ((buf[o + 3].toLong() and 0xffL) shl 24) or
                    (buf[o + 4].toLong() shl 32)
            4 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    ((buf[o + 2].toLong() and 0xffL) shl 16) or
                    (buf[o + 3].toLong() shl 24)
            3 ->
                (buf[o].toLong() and 0xffL) or
                    ((buf[o + 1].toLong() and 0xffL) shl 8) or
                    (buf[o + 2].toLong() shl 16)
            2 ->
                (buf[o].toLong() and 0xffL) or
                    (buf[o + 1].toLong() shl 8)
            1 -> buf[o].toLong()
            else -> throw ExceptionWithContext(
                "Invalid size %d for sized long at offset 0x%x", bytes, offset
            )
        }

        offset = o + bytes - dexBuf.baseOffset
        return result
    }

    fun readString(utf16Length: Int): String {
        val ret = IntArray(1)
        val value = Utf8Utils.utf8BytesWithUtf16LengthToString(
            dexBuf.buf, dexBuf.baseOffset + offset, utf16Length, ret
        )
        offset += ret[0]
        return value
    }

    fun peekStringLength(utf16Length: Int): Int {
        val ret = IntArray(1)
        Utf8Utils.utf8BytesWithUtf16LengthToString(
            dexBuf.buf, dexBuf.baseOffset + offset, utf16Length, ret
        )
        return ret[0]
    }
}
