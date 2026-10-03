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

import java.text.DecimalFormat

object NumberUtils {
    private val canonicalFloatNaN = java.lang.Float.floatToRawIntBits(Float.NaN)
    private val maxFloat = java.lang.Float.floatToRawIntBits(Float.MAX_VALUE)
    private val piFloat = java.lang.Float.floatToRawIntBits(Math.PI.toFloat())
    private val eFloat = java.lang.Float.floatToRawIntBits(Math.E.toFloat())

    private val canonicalDoubleNaN = java.lang.Double.doubleToRawLongBits(Double.NaN)
    private val maxDouble = java.lang.Double.doubleToLongBits(Double.MAX_VALUE)
    private val piDouble = java.lang.Double.doubleToLongBits(Math.PI)
    private val eDouble = java.lang.Double.doubleToLongBits(Math.E)

    private val format = DecimalFormat("0.####################E0")

    @JvmStatic
    fun isLikelyFloat(value: Int): Boolean {
        // Check for some common named float values
        // We don't check for Float.MIN_VALUE, which has an integer representation of 1
        if (value == canonicalFloatNaN ||
            value == maxFloat ||
            value == piFloat ||
            value == eFloat
        ) {
            return true
        }

        // Check for some named integer values
        if (value == Integer.MAX_VALUE || value == Integer.MIN_VALUE) {
            return false
        }

        // Check for likely resource id
        val packageId = value shr 24
        val resourceType = (value shr 16) and 0xff
        val resourceId = value and 0xffff
        if ((packageId == 0x7f || packageId == 1) && resourceType < 0x1f && resourceId < 0xfff) {
            return false
        }

        // a non-canocical NaN is more likely to be an integer
        val floatValue = java.lang.Float.intBitsToFloat(value)
        if (java.lang.Float.isNaN(floatValue)) {
            return false
        }

        // Otherwise, whichever has a shorter scientific notation representation is more likely.
        // Integer wins the tie
        val asInt = format.format(value.toLong())
        var asFloat = format.format(floatValue.toDouble())

        // try to strip off any small imprecision near the end of the mantissa
        val decimalPoint = asFloat.indexOf('.')
        val exponent = asFloat.indexOf("E")
        val zeros = asFloat.indexOf("000")
        if (zeros > decimalPoint && zeros < exponent) {
            asFloat = asFloat.substring(0, zeros) + asFloat.substring(exponent)
        } else {
            val nines = asFloat.indexOf("999")
            if (nines > decimalPoint && nines < exponent) {
                asFloat = asFloat.substring(0, nines) + asFloat.substring(exponent)
            }
        }

        return asFloat.length < asInt.length
    }

    @JvmStatic
    fun isLikelyDouble(value: Long): Boolean {
        // Check for some common named double values
        // We don't check for Double.MIN_VALUE, which has a long representation of 1
        if (value == canonicalDoubleNaN ||
            value == maxDouble ||
            value == piDouble ||
            value == eDouble
        ) {
            return true
        }

        // Check for some named long values
        if (value == Long.MAX_VALUE || value == Long.MIN_VALUE) {
            return false
        }

        // a non-canocical NaN is more likely to be an long
        val doubleValue = java.lang.Double.longBitsToDouble(value)
        if (java.lang.Double.isNaN(doubleValue)) {
            return false
        }

        // Otherwise, whichever has a shorter scientific notation representation is more likely.
        // Long wins the tie
        val asLong = format.format(value)
        var asDouble = format.format(doubleValue)

        // try to strip off any small imprecision near the end of the mantissa
        val decimalPoint = asDouble.indexOf('.')
        val exponent = asDouble.indexOf("E")
        val zeros = asDouble.indexOf("000")
        if (zeros > decimalPoint && zeros < exponent) {
            asDouble = asDouble.substring(0, zeros) + asDouble.substring(exponent)
        } else {
            val nines = asDouble.indexOf("999")
            if (nines > decimalPoint && nines < exponent) {
                asDouble = asDouble.substring(0, nines) + asDouble.substring(exponent)
            }
        }

        return asDouble.length < asLong.length
    }
}
