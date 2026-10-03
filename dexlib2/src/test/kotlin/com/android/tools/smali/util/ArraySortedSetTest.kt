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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Arrays
import java.util.Comparator

@RunWith(JUnit4::class)
class ArraySortedSetTest {
    @Test
    fun testOf() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), arrayOf(1, 2, 3))
        Assert.assertEquals(set.size, 3)
        Assert.assertTrue(set.contains(1))
        Assert.assertTrue(set.contains(2))
        Assert.assertTrue(set.contains(3))
        Assert.assertFalse(set.contains(4))
    }

    @Test
    fun testOfCollection() {
        val list = Arrays.asList(1, 2, 3)
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), list)
        Assert.assertEquals(set.size, 3)
        Assert.assertTrue(set.contains(1))
        Assert.assertTrue(set.contains(2))
        Assert.assertTrue(set.contains(3))
        Assert.assertFalse(set.contains(4))
    }

    @Test(expected = AssertionError::class)
    fun testOfUnsorted() {
        ArraySortedSet.of(Comparator.naturalOrder<Int>(), arrayOf(3, 1, 2))
    }

    @Test(expected = AssertionError::class)
    fun testOfCollectionUnsorted() {
        ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(3, 1, 2))
    }

    @Test
    fun testIterator() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        val it = set.iterator()
        Assert.assertEquals(it.next(), 1)
        Assert.assertEquals(it.next(), 2)
        Assert.assertEquals(it.next(), 3)
    }

    @Test
    fun testIsEmpty() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), arrayOf(1))
        Assert.assertFalse(set.isEmpty())

        val emptySet = ArraySortedSet.of(Comparator.naturalOrder<Int>(), arrayOf<Int>())
        Assert.assertTrue(emptySet.isEmpty())
    }

    @Test
    fun testToArray() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertArrayEquals(set.toArray(), arrayOf<Any?>(1, 2, 3))
    }

    @Test
    fun testToArrayWithArg() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))

        // Array smaller than set
        val small = arrayOfNulls<Int>(0)
        Assert.assertArrayEquals(set.toArray(small), arrayOf<Any?>(1, 2, 3))

        // Array exact size
        val exact = arrayOfNulls<Int>(3)
        Assert.assertArrayEquals(set.toArray(exact), arrayOf<Any?>(1, 2, 3))

        // Array larger than set
        val large = arrayOf<Int?>(9, 9, 9, 9)
        val res = set.toArray(large)
        Assert.assertSame(res, large)
        Assert.assertEquals(res[0], 1)
        Assert.assertEquals(res[1], 2)
        Assert.assertEquals(res[2], 3)
        Assert.assertNull(res[3])
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testAdd() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        set.add(4)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testRemove() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        set.remove(2)
    }

    @Test
    fun testContainsAll() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertTrue(set.containsAll(Arrays.asList(1, 2)))
        Assert.assertTrue(set.containsAll(Arrays.asList(1, 2, 3)))
        Assert.assertFalse(set.containsAll(Arrays.asList(1, 2, 3, 4)))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testAddAll() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        set.addAll(Arrays.asList(4))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testRemoveAll() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        set.removeAll(Arrays.asList(2))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testClear() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        set.clear()
    }

    @Test
    fun testComparator() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertEquals(set.comparator(), Comparator.naturalOrder<Int>())
    }

    @Test
    fun testFirst() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertEquals(set.first(), 1)
    }

    @Test
    fun testLast() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertEquals(set.last(), 3)
    }

    @Test
    fun testHashCode() {
        val set = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertEquals(set.hashCode(), 6)
    }

    @Test
    fun testEquals() {
        val set1 = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        val set2 = ArraySortedSet.of(Comparator.naturalOrder<Int>(), Arrays.asList(1, 2, 3))
        Assert.assertTrue(set1 == set2)
    }
}
