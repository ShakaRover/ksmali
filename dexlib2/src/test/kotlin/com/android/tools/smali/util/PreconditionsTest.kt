/*
 * Copyright 2019, Google LLC
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

import com.android.tools.smali.dexlib2.util.checkArrayPayloadElements
import org.junit.Assert
import org.junit.Test

class PreconditionsTest {

  private fun verifyArrayPayloadElementIsValid(elementWidth: Int, value: Long) {
    checkArrayPayloadElements(elementWidth, listOf(value))
  }

  private fun verifyArrayPayloadElementIsInvalid(elementWidth: Int, value: Long) {
    try {
      checkArrayPayloadElements(elementWidth, listOf(value))
      Assert.fail()
    } catch (ex: IllegalArgumentException) {
      // expected exception
    }
  }

  @Test
  fun checkArrayPayloadElements() {
    verifyArrayPayloadElementIsValid(8, Long.MAX_VALUE)
    verifyArrayPayloadElementIsValid(8, Long.MIN_VALUE)
    verifyArrayPayloadElementIsValid(4, Integer.MAX_VALUE.toLong())
    verifyArrayPayloadElementIsValid(4, Integer.MIN_VALUE.toLong())
    verifyArrayPayloadElementIsValid(2, Short.MAX_VALUE.toLong())
    verifyArrayPayloadElementIsValid(2, Short.MIN_VALUE.toLong())
    verifyArrayPayloadElementIsValid(2, Char.MAX_VALUE.code.toLong())
    verifyArrayPayloadElementIsValid(2, Char.MIN_VALUE.code.toLong())
    verifyArrayPayloadElementIsValid(1, Byte.MAX_VALUE.toLong())
    verifyArrayPayloadElementIsValid(1, Byte.MIN_VALUE.toLong())

    verifyArrayPayloadElementIsInvalid(4, Integer.MAX_VALUE.toLong() + 1)
    verifyArrayPayloadElementIsInvalid(4, Integer.MIN_VALUE.toLong() - 1)
    verifyArrayPayloadElementIsInvalid(2, Short.MIN_VALUE.toLong() - 1)
    //Since short and character have the same size, but different ranges
    // and cannot be distinguished here, the valid interval is
    //[Short.MIN_VALUE, Character.MAX_VALUE], i.e. [-32768, 65535]
    verifyArrayPayloadElementIsInvalid(2, Char.MAX_VALUE.code.toLong() + 1)
    verifyArrayPayloadElementIsInvalid(2, Short.MIN_VALUE.toLong() - 1)
    verifyArrayPayloadElementIsInvalid(1, Byte.MAX_VALUE.toLong() + 1)
    verifyArrayPayloadElementIsInvalid(1, Byte.MIN_VALUE.toLong() - 1)
  }
}
