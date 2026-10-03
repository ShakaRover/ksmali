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

package com.android.tools.smali.dexlib2.writer.util

import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.immutable.ImmutableExceptionHandler
import com.android.tools.smali.dexlib2.immutable.ImmutableTryBlock
import com.google.common.collect.ImmutableList
import org.junit.Assert
import org.junit.Test

class TryListBuilderTest {
    private class TryListBuilder : com.android.tools.smali.dexlib2.writer.util.TryListBuilder<ExceptionHandler>()

    @Test
    fun testSingleCatchAll_Beginning() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler(null, 5))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(ImmutableTryBlock(0, 10,
                ImmutableList.of(ImmutableExceptionHandler(null, 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testSingleCatchAll_Middle() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler(null, 15))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(ImmutableTryBlock(5, 5,
                ImmutableList.of(ImmutableExceptionHandler(null, 15))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testSingleCatch_Beginning() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("Ljava/lang/Exception;", 5))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(ImmutableTryBlock(0, 10,
                ImmutableList.of(ImmutableExceptionHandler("Ljava/lang/Exception;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testSingleCatch_Middle() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("Ljava/lang/Exception;", 15))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(ImmutableTryBlock(5, 5,
                ImmutableList.of(ImmutableExceptionHandler("Ljava/lang/Exception;", 15))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_End_After() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(10, 20, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 10,
                        ImmutableList.of(ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(10, 10,
                        ImmutableList.of(ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_After_After() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(15, 20, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 10,
                        ImmutableList.of(ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(15, 5,
                        ImmutableList.of(ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Before_Start() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 5, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Before_Before() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 3, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 3,
                        ImmutableList.of(ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Start_End() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Start_Middle() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 5, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Middle_Middle() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(2, 7, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 2,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(2, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(7, 3,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Middle_End() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Beginning_After() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Middle_After() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(5, 15, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Before_End() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 10, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Before_Middle() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 7, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 2,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(7, 3,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Before_After() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testOverlap_Hole() {
        val tlb = TryListBuilder()

        tlb.addHandler(1, 5, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(10, 14, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 1,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(1, 4,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(10, 4,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(14, 1,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_Same() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException1;", 5))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 15,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_DifferentType() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException2;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler("LException2;", 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException2;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_DifferentAddress() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException1;", 6))
        // no exception should be thrown...
    }

    @Test
    fun testHandlerMerge_Exception_Catchall() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler(null, 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler(null, 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_Catchall_Exception() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler(null, 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler("LException1;", 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 6))),
                ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 5),
                                ImmutableExceptionHandler("LException1;", 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 6))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_Catchall_Catchall() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler(null, 5))
        tlb.addHandler(0, 15, ImmutableExceptionHandler(null, 5))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 15,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 5))))

        Assert.assertEquals(expected, tryBlocks)
    }

    @Test
    fun testHandlerMerge_Catchall_Catchall_DifferentAddress() {
        val tlb = TryListBuilder()

        tlb.addHandler(5, 10, ImmutableExceptionHandler(null, 5))
        try {
            tlb.addHandler(0, 15, ImmutableExceptionHandler(null, 6))
        } catch (ex: TryListBuilder.InvalidTryException) {
            return
    }
        Assert.fail()
    }

    @Test
    fun testHandlerMerge_MergeSame() {
        val tlb = TryListBuilder()

        tlb.addHandler(0, 15, ImmutableExceptionHandler(null, 6))
        tlb.addHandler(10, 20, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(20, 30, ImmutableExceptionHandler("LException1;", 5))
        tlb.addHandler(25, 40, ImmutableExceptionHandler(null, 6))

        val tryBlocks = tlb.getTryBlocks()

        val expected = ImmutableList.of(
                ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 6))),
                ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 6),
                                ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(15, 10,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5))),
                ImmutableTryBlock(25, 5,
                        ImmutableList.of(
                                ImmutableExceptionHandler("LException1;", 5),
                                ImmutableExceptionHandler(null, 6))),
                ImmutableTryBlock(30, 10,
                        ImmutableList.of(
                                ImmutableExceptionHandler(null, 6))))

        Assert.assertEquals(expected, tryBlocks)
    }
}
