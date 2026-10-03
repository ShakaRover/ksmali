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

class NumberUtilsTest {
    @Test
    fun isLikelyFloatTest() {
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1.23f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1.0f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Float.NaN)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Float.NEGATIVE_INFINITY)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Float.POSITIVE_INFINITY)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1e-30f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1000f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(-1f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(-5f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(1.3333f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(4.5f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(.1f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(50000f)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Float.MAX_VALUE)))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Math.PI.toFloat())))
        Assert.assertTrue(NumberUtils.isLikelyFloat(java.lang.Float.floatToRawIntBits(Math.E.toFloat())))
        Assert.assertTrue(NumberUtils.isLikelyFloat(2139095039))
        Assert.assertFalse(NumberUtils.isLikelyFloat(0))
        Assert.assertFalse(NumberUtils.isLikelyFloat(1))
        Assert.assertFalse(NumberUtils.isLikelyFloat(10))
        Assert.assertFalse(NumberUtils.isLikelyFloat(100))
        Assert.assertFalse(NumberUtils.isLikelyFloat(1000))
        Assert.assertFalse(NumberUtils.isLikelyFloat(1024))
        Assert.assertFalse(NumberUtils.isLikelyFloat(1234))
        Assert.assertFalse(NumberUtils.isLikelyFloat(-5))
        Assert.assertFalse(NumberUtils.isLikelyFloat(-13))
        Assert.assertFalse(NumberUtils.isLikelyFloat(-123))
        Assert.assertFalse(NumberUtils.isLikelyFloat(20000000))
        Assert.assertFalse(NumberUtils.isLikelyFloat(2000000000))
        Assert.assertFalse(NumberUtils.isLikelyFloat(-2000000000))
        Assert.assertFalse(NumberUtils.isLikelyFloat(Integer.MAX_VALUE))
        Assert.assertFalse(NumberUtils.isLikelyFloat(Integer.MIN_VALUE))
        Assert.assertFalse(NumberUtils.isLikelyFloat(Short.MIN_VALUE.toInt()))
        Assert.assertFalse(NumberUtils.isLikelyFloat(Short.MAX_VALUE.toInt()))
    }

    @Test
    fun isLikelyDoubleTest() {
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1.23f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1.0f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Double.NaN)))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY)))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Double.POSITIVE_INFINITY)))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1e-30f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1000f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(-1f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(-5f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1.3333f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(1.33333f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(4.5f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(.1f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(50000f.toDouble())))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Double.MAX_VALUE)))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Math.PI)))
        Assert.assertTrue(NumberUtils.isLikelyDouble(java.lang.Double.doubleToRawLongBits(Math.E)))
        Assert.assertFalse(NumberUtils.isLikelyDouble(0L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(1L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(10L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(100L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(1000L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(1024L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(1234L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(-5L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(-13L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(-123L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(20000000L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(2000000000L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(-2000000000L))
        Assert.assertFalse(NumberUtils.isLikelyDouble(Integer.MAX_VALUE.toLong()))
        Assert.assertFalse(NumberUtils.isLikelyDouble(Integer.MIN_VALUE.toLong()))
        Assert.assertFalse(NumberUtils.isLikelyDouble(Short.MIN_VALUE.toLong()))
        Assert.assertFalse(NumberUtils.isLikelyDouble(Short.MAX_VALUE.toLong()))
    }
}
