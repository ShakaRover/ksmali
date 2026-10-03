/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

/**
 * Utilities for formatting numbers as hexadecimal.
 */
object Hex {
    /** Formats a `long` as an 8-byte unsigned hex value. */
    fun u8(v: Long): String {
        val result = CharArray(16)
        var v = v
        for (i in 0 until 16) {
            result[15 - i] = Character.forDigit((v and 0x0fL).toInt(), 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 4-byte unsigned hex value. */
    fun u4(v: Int): String {
        val result = CharArray(8)
        var v = v
        for (i in 0 until 8) {
            result[7 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 3-byte unsigned hex value. */
    fun u3(v: Int): String {
        val result = CharArray(6)
        var v = v
        for (i in 0 until 6) {
            result[5 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 2-byte unsigned hex value. */
    fun u2(v: Int): String {
        val result = CharArray(4)
        var v = v
        for (i in 0 until 4) {
            result[3 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /**
     * Formats an `int` as either a 2-byte unsigned hex value (if the value is small enough) or a
     * 4-byte unsigned hex value (if not).
     */
    fun u2or4(v: Int): String {
        return if (v == v.toChar().code) {
            u2(v)
        } else {
            u4(v)
        }
    }

    /** Formats an `int` as a 1-byte unsigned hex value. */
    fun u1(v: Int): String {
        val result = CharArray(2)
        var v = v
        for (i in 0 until 2) {
            result[1 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 4-bit unsigned hex nibble. */
    fun uNibble(v: Int): String {
        val result = CharArray(1)
        result[0] = Character.forDigit(v and 0x0f, 16)
        return String(result)
    }

    /** Formats a `long` as an 8-byte signed hex value. */
    fun s8(v: Long): String {
        val result = CharArray(17)
        var v = v
        if (v < 0) {
            result[0] = '-'
            v = -v
        } else {
            result[0] = '+'
        }
        for (i in 0 until 16) {
            result[16 - i] = Character.forDigit((v and 0x0fL).toInt(), 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 4-byte signed hex value. */
    fun s4(v: Int): String {
        val result = CharArray(9)
        var v = v
        if (v < 0) {
            result[0] = '-'
            v = -v
        } else {
            result[0] = '+'
        }
        for (i in 0 until 8) {
            result[8 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 2-byte signed hex value. */
    fun s2(v: Int): String {
        val result = CharArray(5)
        var v = v
        if (v < 0) {
            result[0] = '-'
            v = -v
        } else {
            result[0] = '+'
        }
        for (i in 0 until 4) {
            result[4 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /** Formats an `int` as a 1-byte signed hex value. */
    fun s1(v: Int): String {
        val result = CharArray(3)
        var v = v
        if (v < 0) {
            result[0] = '-'
            v = -v
        } else {
            result[0] = '+'
        }
        for (i in 0 until 2) {
            result[2 - i] = Character.forDigit(v and 0x0f, 16)
            v = v shr 4
        }
        return String(result)
    }

    /**
     * Formats a hex dump of a portion of a `byte[]`. The result is always newline-terminated,
     * unless the passed-in length was zero, in which case the result is always the empty string.
     */
    fun dump(
        arr: ByteArray,
        offset: Int,
        length: Int,
        outOffset: Int,
        bpl: Int,
        addressLength: Int
    ): String {
        var offset = offset
        var outOffset = outOffset
        var length = length
        val end = offset + length

        // twos-complement math trick: ((x < 0) || (y < 0)) <=> ((x|y) < 0)
        if (((offset or length or end) < 0) || (end > arr.size)) {
            throw IndexOutOfBoundsException(
                "arr.length " + arr.size + "; " + offset + "..!" + end
            )
        }

        if (outOffset < 0) {
            throw IllegalArgumentException("outOffset < 0")
        }

        if (length == 0) {
            return ""
        }

        val sb = StringBuffer(length * 4 + 6)
        var col = 0

        while (length > 0) {
            if (col == 0) {
                val astr: String
                when (addressLength) {
                    2 -> astr = u1(outOffset)
                    4 -> astr = u2(outOffset)
                    6 -> astr = u3(outOffset)
                    else -> astr = u4(outOffset)
                }
                sb.append(astr)
                sb.append(": ")
            } else if ((col and 1) == 0) {
                sb.append(' ')
            }
            sb.append(u1(arr[offset].toInt()))
            outOffset++
            offset++
            col++
            if (col == bpl) {
                sb.append('\n')
                col = 0
            }
            length--
        }

        if (col != 0) {
            sb.append('\n')
        }

        return sb.toString()
    }
}
