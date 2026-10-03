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

class ShortLiteralTest {

    @Test
    fun SuccessHexTests() {
        Assert.assertTrue(parseShort("0x0").toInt() == 0x0)
        Assert.assertTrue(parseShort("0x00").toInt() == 0x0)
        Assert.assertTrue(parseShort("0x1").toInt() == 0x1)
        Assert.assertTrue(parseShort("0x1234").toInt() == 0x1234)
        Assert.assertTrue(parseShort("0x7fff").toInt() == 0x7fff)
        Assert.assertTrue(parseShort("0x8000").toInt() == Short.MIN_VALUE.toInt())
        Assert.assertTrue(parseShort("0xFFFF").toInt() == -1)

        Assert.assertTrue(parseShort("-0x00").toInt() == 0)
        Assert.assertTrue(parseShort("-0x01").toInt() == -1)
        Assert.assertTrue(parseShort("-0x1234").toInt() == -0x1234)
        Assert.assertTrue(parseShort("-0x8000").toInt() == Short.MIN_VALUE.toInt())
        Assert.assertTrue(parseShort("-0x1fff").toInt() == -0x1fff)
    }

    @Test(expected = NumberFormatException::class)
    fun FaileHexTest1() {
        parseShort("-0x8001")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest2() {
        parseShort("-0xFFFF")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest3() {
        parseShort("0x100000")
    }

    @Test
    fun SuccessDecTests() {
        Assert.assertTrue(parseShort("0").toInt() == 0)
        Assert.assertTrue(parseShort("1").toInt() == 1)
        Assert.assertTrue(parseShort("12345").toInt() == 12345)
        Assert.assertTrue(parseShort("32767").toInt() == 32767)
        Assert.assertTrue(parseShort("32768").toInt() == Short.MIN_VALUE.toInt())
        Assert.assertTrue(parseShort("65535").toInt() == -1)

        Assert.assertTrue(parseShort("-0").toInt() == 0)
        Assert.assertTrue(parseShort("-1").toInt() == -1)
        Assert.assertTrue(parseShort("-12345").toInt() == -12345)
        Assert.assertTrue(parseShort("-32767").toInt() == -32767)
        Assert.assertTrue(parseShort("-32768").toInt() == Short.MIN_VALUE.toInt())
    }

    @Test(expected = NumberFormatException::class)
    fun FaileDecTest1() {
        parseShort("-32769")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest2() {
        parseShort("-65535")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest3() {
        parseShort("65536")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest4() {
        parseShort("65600")
    }

    @Test
    fun SuccessOctTests() {
        Assert.assertTrue(parseShort("00").toInt() == 0)
        Assert.assertTrue(parseShort("01").toInt() == 1)
        Assert.assertTrue(parseShort("012345").toInt() == 5349)
        Assert.assertTrue(parseShort("077777").toInt() == Short.MAX_VALUE.toInt())
        Assert.assertTrue(parseShort("0100000").toInt() == Short.MIN_VALUE.toInt())
        Assert.assertTrue(parseShort("0177777").toInt() == -1)

        Assert.assertTrue(parseShort("-00").toInt() == 0)
        Assert.assertTrue(parseShort("-01").toInt() == -1)
        Assert.assertTrue(parseShort("-012345").toInt() == -5349)
        Assert.assertTrue(parseShort("-077777").toInt() == -32767)
        Assert.assertTrue(parseShort("-0100000").toInt() == Short.MIN_VALUE.toInt())
    }

    @Test(expected = NumberFormatException::class)
    fun FaileOctTest1() {
        parseShort("-0100001")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest2() {
        parseShort("-0177777")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest3() {
        parseShort("0200000")
    }
}
