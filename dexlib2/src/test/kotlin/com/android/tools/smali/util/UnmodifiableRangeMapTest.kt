/*
 * Copyright 2024, Google LLC
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

class UnmodifiableRangeMapTest {

  @Test
  fun testEmpty() {
    val map = UnmodifiableRangeMap.of<Int, String>()
    Assert.assertNull(map.get(0))
    Assert.assertNull(map.getEntry(0))
  }

  @Test
  fun testBuilder() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(4, 6), "b")
        .build()
    Assert.assertEquals("a", rangeMap.get(2))
    Assert.assertEquals("b", rangeMap.get(5))
    Assert.assertNull(rangeMap.get(0))
    Assert.assertNull(rangeMap.get(7))
    Assert.assertNull(rangeMap.get(null))
  }

  @Test(expected = IllegalArgumentException::class)
  fun testBuilderEmptyRange() {
    UnmodifiableRangeMap.builder<Int, String>().put(Range.open(1, 1), "a").build()
  }

  @Test(expected = IllegalArgumentException::class)
  fun testBuilderOverlappingRanges() {
    UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(2, 4), "b")
        .build()
  }

  @Test(expected = IllegalArgumentException::class)
  fun testBuilderEdgeOverlappingRanges() {
    UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(3, 7), "b")
        .build()
  }

  @Test
  fun testGet_oddSizeMap() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(4, 6), "b")
        .put(Range.closed(11, 20), "c")
        .build()
    Assert.assertEquals("a", rangeMap.get(2))
    Assert.assertEquals("c", rangeMap.get(15))
    Assert.assertNull(rangeMap.get(0))
    Assert.assertNull(rangeMap.get(7))
  }

  @Test
  fun testGet_evenSizeMap() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(4, 6), "b")
        .put(Range.closed(11, 20), "c")
        .put(Range.closed(21, 30), "d")
        .build()
    Assert.assertEquals("a", rangeMap.get(2))
    Assert.assertEquals("b", rangeMap.get(6))
    Assert.assertEquals("c", rangeMap.get(15))
    Assert.assertEquals("d", rangeMap.get(23))
    Assert.assertNull(rangeMap.get(0))
    Assert.assertNull(rangeMap.get(7))
  }

  @Test
  fun testEntry() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.closed(1, 3), "a")
        .put(Range.closed(4, 6), "b")
        .put(Range.closed(10, 15), "c")
        .build()
    val entry = rangeMap.getEntry(2)!!
    Assert.assertEquals(Range.closed(1, 3), entry.key)
    Assert.assertEquals("a", entry.value)
    Assert.assertNull(rangeMap.getEntry(null))
  }

  @Test
  fun testRanges_AtMostAtLeast() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.atMost(3), "a")
        .put(Range.closed(10, 15), "b")
        .put(Range.atLeast(25), "c")
        .build()
    Assert.assertEquals("a", rangeMap.get(2))
    Assert.assertNull(rangeMap.get(5))
    Assert.assertEquals("b", rangeMap.get(12))
    Assert.assertEquals("c", rangeMap.get(30))
    Assert.assertNull(rangeMap.get(null))
  }

  @Test
  fun testRanges_openBounds() {
    val rangeMap = UnmodifiableRangeMap.builder<Int, String>()
        .put(Range.openClosed(1, 3), "a")
        .put(Range.open(10, 15), "b")
        .put(Range.closedOpen(25, 30), "c")
        .build()

    Assert.assertNull(rangeMap.get(0))
    Assert.assertNull(rangeMap.get(1))
    Assert.assertEquals("a", rangeMap.get(2))
    Assert.assertEquals("b", rangeMap.get(12))
    Assert.assertNull(rangeMap.get(5))
    Assert.assertNull(rangeMap.get(15))
    Assert.assertEquals("c", rangeMap.get(25))
    Assert.assertEquals("c", rangeMap.get(25))
  }
}
