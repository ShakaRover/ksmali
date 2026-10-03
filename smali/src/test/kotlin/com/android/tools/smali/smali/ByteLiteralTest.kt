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

class ByteLiteralTest {

    @Test
    fun SuccessHexTests() {
        Assert.assertTrue(parseByte("0x0T").toInt() == 0x0)
        Assert.assertTrue(parseByte("0x00").toInt() == 0x0)
        Assert.assertTrue(parseByte("0x1T").toInt() == 0x1)
        Assert.assertTrue(parseByte("0x12").toInt() == 0x12)
        Assert.assertTrue(parseByte("0x7fT").toInt() == 0x7f)
        Assert.assertTrue(parseByte("0x80t").toInt() == Byte.MIN_VALUE.toInt())
        Assert.assertTrue(parseByte("0xFFt").toInt() == -1)

        Assert.assertTrue(parseByte("-0x00").toInt() == 0)
        Assert.assertTrue(parseByte("-0x01").toInt() == -1)
        Assert.assertTrue(parseByte("-0x12").toInt() == -0x12)
        Assert.assertTrue(parseByte("-0x80").toInt() == Byte.MIN_VALUE.toInt())
        Assert.assertTrue(parseByte("-0x1f").toInt() == -0x1f)
    }

    @Test(expected = NumberFormatException::class)
    fun FaileHexTest1() {
        parseByte("-0x81")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest2() {
        parseByte("-0xFF")
    }

    @Test(expected = NumberFormatException::class)
    fun FailHexTest3() {
        parseByte("0x100")
    }

    @Test
    fun SuccessDecTests() {
        Assert.assertTrue(parseByte("0").toInt() == 0)
        Assert.assertTrue(parseByte("1t").toInt() == 1)
        Assert.assertTrue(parseByte("123").toInt() == 123)
        Assert.assertTrue(parseByte("127T").toInt() == 127)
        Assert.assertTrue(parseByte("128").toInt() == Byte.MIN_VALUE.toInt())
        Assert.assertTrue(parseByte("255").toInt() == -1)

        Assert.assertTrue(parseByte("-0").toInt() == 0)
        Assert.assertTrue(parseByte("-1").toInt() == -1)
        Assert.assertTrue(parseByte("-123").toInt() == -123)
        Assert.assertTrue(parseByte("-127").toInt() == -127)
        Assert.assertTrue(parseByte("-128").toInt() == Byte.MIN_VALUE.toInt())
    }

    @Test(expected = NumberFormatException::class)
    fun FaileDecTest1() {
        parseByte("-129")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest2() {
        parseByte("-255")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest3() {
        parseByte("256")
    }

    @Test(expected = NumberFormatException::class)
    fun FailDecTest4() {
        parseByte("260")
    }

    @Test
    fun SuccessOctTests() {
        Assert.assertTrue(parseByte("00").toInt() == 0)
        Assert.assertTrue(parseByte("01").toInt() == 1)
        Assert.assertTrue(parseByte("0123t").toInt() == 83)
        Assert.assertTrue(parseByte("0177").toInt() == Byte.MAX_VALUE.toInt())
        Assert.assertTrue(parseByte("0200T").toInt() == Byte.MIN_VALUE.toInt())
        Assert.assertTrue(parseByte("0377").toInt() == -1)

        Assert.assertTrue(parseByte("-00").toInt() == 0)
        Assert.assertTrue(parseByte("-01").toInt() == -1)
        Assert.assertTrue(parseByte("-0123").toInt() == -83)
        Assert.assertTrue(parseByte("-0177").toInt() == -127)
        Assert.assertTrue(parseByte("-0200").toInt() == Byte.MIN_VALUE.toInt())
    }

    @Test(expected = NumberFormatException::class)
    fun FaileOctTest1() {
        parseByte("-0201")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest2() {
        parseByte("-0377")
    }

    @Test(expected = NumberFormatException::class)
    fun FailOctTest3() {
        parseByte("0400")
    }
}
