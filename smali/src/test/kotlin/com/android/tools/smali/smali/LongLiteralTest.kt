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

package com.android.tools.smali.smali

import org.junit.Assert
import org.junit.Test

class LongLiteralTest {

    @Test
    fun SuccessHexTests() {
        Assert.assertTrue(parseLong("0x0L") == 0L)
        Assert.assertTrue(parseLong("0x00L") == 0L)
        Assert.assertTrue(parseLong("0x1L") == 1L)
        Assert.assertTrue(parseLong("0x1234567890123456L") == 0x1234567890123456L)
        Assert.assertTrue(parseLong("0x7fffffffffffffffL") == Long.MAX_VALUE)
        Assert.assertTrue(parseLong("0x8000000000000000L") == Long.MIN_VALUE)
        Assert.assertTrue(parseLong("0xFFFFFFFFFFFFFFFFL") == -1L)

        Assert.assertTrue(parseLong("-0x00L") == 0L)
        Assert.assertTrue(parseLong("-0x01L") == -1L)
        Assert.assertTrue(parseLong("-0x1234567890123456L") == -0x1234567890123456L)
        Assert.assertTrue(parseLong("-0x8000000000000000L") == Long.MIN_VALUE)
        Assert.assertTrue(parseLong("-0x1fffffffffffffffL") == -0x1fffffffffffffffL)
    }

    @Test(expected = NumberFormatException::class)
    fun FaileHexTest1() {
        parseLong("-0x8000000000000001")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest2() {
        parseLong("-0xFFFFFFFFFFFFFFFF")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest3() {
        parseLong("0x10000000000000000")
    }

    @Test
    fun SuccessDecTests() {
        Assert.assertTrue(parseLong("0L") == 0L)
        Assert.assertTrue(parseLong("1") == 1L)
        Assert.assertTrue(parseLong("1234567890123456789") == 1234567890123456789L)
        Assert.assertTrue(parseLong("9223372036854775807") == Long.MAX_VALUE)
        Assert.assertTrue(parseLong("9223372036854775808") == Long.MIN_VALUE)
        Assert.assertTrue(parseLong("18446744073709551615L") == -1L)

        Assert.assertTrue(parseLong("-0") == 0L)
        Assert.assertTrue(parseLong("-1") == -1L)
        Assert.assertTrue(parseLong("-1234567890123456789") == -1234567890123456789L)
        Assert.assertTrue(parseLong("-9223372036854775807") == -9223372036854775807L)
        Assert.assertTrue(parseLong("-9223372036854775808") == Long.MIN_VALUE)
    }

    @Test(expected = NumberFormatException::class)
    fun FaileDecTest1() {
        parseLong("-9223372036854775809")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest2() {
        parseLong("-18446744073709551616")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest3() {
        parseLong("18446744073709551617")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest4() {
        parseLong("18446744073709551700")
    }

    @Test
    fun SuccessOctTests() {
        Assert.assertTrue(parseLong("00") == 0L)
        Assert.assertTrue(parseLong("01") == 1L)
        Assert.assertTrue(parseLong("0123456701234567012345") == 1505851632739161317L)
        Assert.assertTrue(parseLong("0777777777777777777777") == Long.MAX_VALUE)
        Assert.assertTrue(parseLong("01000000000000000000000") == Long.MIN_VALUE)
        Assert.assertTrue(parseLong("01777777777777777777777") == -1L)

        Assert.assertTrue(parseLong("-00") == 0L)
        Assert.assertTrue(parseLong("-01") == -1L)
        Assert.assertTrue(parseLong("-0123456701234567012345") == -1505851632739161317L)
        Assert.assertTrue(parseLong("-0777777777777777777777") == -9223372036854775807L)
        Assert.assertTrue(parseLong("-01000000000000000000000") == Long.MIN_VALUE)
    }

    @Test(expected = NumberFormatException::class)
    fun FaileOctTest1() {
        parseLong("-01000000000000000000001")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest2() {
        parseLong("-01777777777777777777777")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest3() {
        parseLong("02000000000000000000000")
    }
}
