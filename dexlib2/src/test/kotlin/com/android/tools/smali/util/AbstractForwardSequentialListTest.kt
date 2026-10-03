/*
 * Copyright 2012, Google LLC
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
import org.junit.Before
import org.junit.Test
import java.util.NoSuchElementException

class AbstractForwardSequentialListTest {
    private lateinit var list: List<Int>

    @Before
    fun setup() {
        list = object : AbstractForwardSequentialList<Int>() {
            override fun iterator(): MutableIterator<Int> {
                return object : MutableIterator<Int> {
                    private var index = 0
                    override fun hasNext(): Boolean = index < 100
                    override fun next(): Int {
                        if (!hasNext()) {
                            throw NoSuchElementException()
                        }
                        return index++
                    }
                    override fun remove() {
                        throw UnsupportedOperationException()
                    }
                }
            }
            override val size: Int
                get() = 100
        }
    }

    private fun testForwardIterationImpl(iter: ListIterator<Int>) {
        Assert.assertFalse(iter.hasPrevious())
        for (i in 0 until 100) {
            Assert.assertEquals(i.toLong(), iter.nextIndex().toLong())
            Assert.assertEquals((i - 1).toLong(), iter.previousIndex().toLong())
            Assert.assertTrue(iter.hasNext())
            Assert.assertEquals(i.toLong(), iter.next().toLong())
            Assert.assertTrue(iter.hasPrevious())
        }
        Assert.assertFalse(iter.hasNext())
        Assert.assertEquals(iter.nextIndex().toLong(), 100L)
        Assert.assertEquals(iter.previousIndex().toLong(), 99L)
    }

    @Test
    fun testForwardIteration() {
        testForwardIterationImpl(list.listIterator())
    }

    private fun testReverseIterationImpl(iter: ListIterator<Int>) {
        Assert.assertFalse(iter.hasNext())
        for (i in 99 downTo 0) {
            Assert.assertEquals((i + 1).toLong(), iter.nextIndex().toLong())
            Assert.assertEquals(i.toLong(), iter.previousIndex().toLong())
            Assert.assertTrue(iter.hasPrevious())
            Assert.assertEquals(i.toLong(), iter.previous().toLong())
            Assert.assertTrue(iter.hasNext())
        }
        Assert.assertFalse(iter.hasPrevious())
        Assert.assertEquals(0L, iter.nextIndex().toLong())
        Assert.assertEquals(-1L, iter.previousIndex().toLong())
    }

    @Test
    fun testReverseIteration() {
        testReverseIterationImpl(list.listIterator(100))
    }

    @Test
    fun testAlternatingIteration() {
        val iter = list.listIterator(50)
        for (i in 0 until 10) {
            Assert.assertTrue(iter.hasNext())
            Assert.assertTrue(iter.hasPrevious())
            Assert.assertEquals(50L, iter.nextIndex().toLong())
            Assert.assertEquals(49L, iter.previousIndex().toLong())
            Assert.assertEquals(50L, iter.next().toLong())
            Assert.assertTrue(iter.hasNext())
            Assert.assertTrue(iter.hasPrevious())
            Assert.assertEquals(51L, iter.nextIndex().toLong())
            Assert.assertEquals(50L, iter.previousIndex().toLong())
            Assert.assertEquals(50L, iter.previous().toLong())
        }
    }

    @Test
    fun testAlternatingIteration2() {
        val iter = list.listIterator(0)
        for (i in 0 until 10) {
            testForwardIterationImpl(iter)
            testReverseIterationImpl(iter)
        }
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testNegativeIndex() {
        list.listIterator(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testLargeIndex() {
        list.listIterator(101)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testLargeIndex2() {
        list.listIterator(1000000)
    }

    @Test
    fun testForwardIterationException() {
        // note: no "expected = NoSuchElementException", because we want to make sure the exception
        // occurs only during the last call to next()
        val iter = list.listIterator(0)
        for (i in 0 until 100) {
            iter.next()
        }
        try {
            iter.next()
        } catch (ex: NoSuchElementException) {
            return
        }
        Assert.fail()
    }

    @Test(expected = NoSuchElementException::class)
    fun testForwardIterationException2() {
        val iter = list.listIterator(100)
        iter.next()
    }

    @Test
    fun testReverseIterationException() {
        // note: no "expected = NoSuchElementException", because we want to make sure the exception
        // occurs only during the last call to previous()
        val iter = list.listIterator(100)
        for (i in 0 until 100) {
            iter.previous()
        }
        try {
            iter.previous()
        } catch (ex: NoSuchElementException) {
            return
        }
        Assert.fail()
    }

    @Test(expected = NoSuchElementException::class)
    fun testReverseIterationException2() {
        val iter = list.listIterator(0)
        iter.previous()
    }
}
