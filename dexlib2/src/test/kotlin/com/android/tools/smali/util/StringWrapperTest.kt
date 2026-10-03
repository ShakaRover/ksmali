/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.util

import org.junit.Assert
import org.junit.Test

class StringWrapperTest {
    @Test
    fun testWrapStringByWords() {
        validateResult2(arrayOf("abc", "abcdef", "abcdef"),
                "abc\nabcdefabcdef", 6)

        validateResult2(arrayOf("abc", "abcdef", " ", "abcdef"),
                "abc\nabcdef abcdef", 6)

        validateResult2(arrayOf("abc", "abcde ", "fabcde", "f"),
                "abc\nabcde fabcdef", 6)

        validateResult2(arrayOf("abc def ghi ", "kjl mon pqr ", "stu vwx yz"),
                "abc def ghi kjl mon pqr stu vwx yz", 14)

        validateResult2(arrayOf("abcdefg", "hikjlmo", "npqrstu", "vwxyz"),
                "abcdefghikjlmonpqrstuvwxyz", 7)

        validateResult2(arrayOf("abc", "defhig"),
                "abc\ndefhig", 20)
    }

    @Test
    fun testWrapString() {
        validateResult(
                arrayOf("abc", "abcdef", "abcdef"),
                StringWrapper.wrapString("abc\nabcdefabcdef", 6, null))

        validateResult(
                arrayOf("abc"),
                StringWrapper.wrapString("abc", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("abc"),
                StringWrapper.wrapString("abc", 6, arrayOfNulls(0)))

        validateResult(
                arrayOf("abc"),
                StringWrapper.wrapString("abc", 6, arrayOfNulls(1)))

        validateResult(
                arrayOf(""),
                StringWrapper.wrapString("", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("abcdef"),
                StringWrapper.wrapString("abcdef", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("abcdef", "abcdef"),
                StringWrapper.wrapString("abcdef\nabcdef", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("abc", "", "def"),
                StringWrapper.wrapString("abc\n\ndef", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("", "abcdef"),
                StringWrapper.wrapString("\nabcdef", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("", "", "abcdef"),
                StringWrapper.wrapString("\n\nabcdef", 6, arrayOfNulls(3)))

        validateResult(
                arrayOf("", "", "abcdef"),
                StringWrapper.wrapString("\n\nabcdef", 6, arrayOfNulls(4)))

        validateResult(
                arrayOf("", "", "abcdef", ""),
                StringWrapper.wrapString("\n\nabcdef\n\n", 6, arrayOfNulls(4)))

        validateResult(
                arrayOf("", "", "abcdef", "a", ""),
                StringWrapper.wrapString("\n\nabcdefa\n\n", 6, arrayOfNulls(4)))

        validateResult(
                arrayOf("", "", "abcdef", "a", ""),
                StringWrapper.wrapString("\n\nabcdefa\n\n", 6, arrayOfNulls(0)))

        validateResult(
                arrayOf("", "", "abcdef", "a", ""),
                StringWrapper.wrapString("\n\nabcdefa\n\n", 6, arrayOfNulls(5)))

        validateResult(
                arrayOf("", "", "a", "b", "c", "d", "e", "f", "a", ""),
                StringWrapper.wrapString("\n\nabcdefa\n\n", 1, arrayOfNulls(5)))
    }

    companion object {
        @JvmStatic
        fun validateResult(expected: Array<String>, actual: Array<String?>) {
            Assert.assertTrue(actual.size >= expected.size)

            for (i in actual.indices) {
                if (actual[i] == null) {
                    Assert.assertTrue(i == expected.size)
                    return
                }
                Assert.assertTrue(i < expected.size)
                Assert.assertEquals(expected[i], actual[i])
            }
        }

        @JvmStatic
        fun validateResult2(expected: Array<String>, textToWrap: String, maxWidth: Int) {
            val result = StringWrapper.wrapStringOnBreaks(textToWrap, maxWidth).toList()

            Assert.assertEquals(expected.size, result.size)
            for (i in result.indices) {
                Assert.assertTrue(i < expected.size)
                Assert.assertEquals(expected[i], result[i])
            }
        }
    }
}
