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

import java.util.Arrays
import java.util.Comparator
import java.util.NoSuchElementException
import java.util.SortedSet
import java.util.stream.Collectors

/* A sorted set implemented with an underlying array. ArraySortedSet is inmutable. */
class ArraySortedSet<T> private constructor(
    private val comparatorField: Comparator<in T>,
    private val arr: Array<Any?>
) : SortedSet<T> {
    init {
        assert(assertSorted())
    }

    private constructor(
        comparator: Comparator<in T>,
        collection: Collection<T>
    ) : this(comparator, collectionToArray(collection))

    @Suppress("UNCHECKED_CAST")
    private fun assertSorted(): Boolean {
        for (i in 1 until arr.size) {
            assert(comparatorField.compare(arr[i - 1] as T, arr[i] as T) < 0)
        }
        return true
    }

    override val size: Int
        get() = arr.size

    override fun isEmpty(): Boolean {
        return arr.size == 0
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun contains(element: T): Boolean {
        return Arrays.binarySearch(arr as Array<T>, element, comparatorField) >= 0
    }

    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<T> {
        return (Arrays.asList(*arr) as MutableList<T>).iterator()
    }

    fun toArray(): Array<Any?> {
        return arr.clone()
    }

    @Suppress("UNCHECKED_CAST")
    fun <E> toArray(a: Array<E>): Array<E> {
        if (a.size < arr.size) {
            return Arrays.copyOf(arr, arr.size, a.javaClass) as Array<E>
        }
        System.arraycopy(arr, 0, a, 0, arr.size)
        if (a.size > arr.size) {
            (a as Array<Any?>)[arr.size] = null
        }
        return a
    }
    override fun add(element: T): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: T): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (o in elements) {
            if (!contains(o)) {
                return false
            }
        }
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }
    override fun comparator(): Comparator<in T>? {
        return comparatorField
    }

    override fun subSet(fromElement: T, toElement: T): SortedSet<T> {
        throw UnsupportedOperationException()
    }

    override fun headSet(toElement: T): SortedSet<T> {
        throw UnsupportedOperationException()
    }

    override fun tailSet(fromElement: T): SortedSet<T> {
        throw UnsupportedOperationException()
    }

    @Suppress("UNCHECKED_CAST")
    override fun first(): T {
        if (arr.size == 0) {
            throw NoSuchElementException()
        }
        return arr[0] as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun last(): T {
        if (arr.size == 0) {
            throw NoSuchElementException()
        }
        return arr[arr.size - 1] as T
    }
    override fun hashCode(): Int {
        var result = 0
        for (o in arr) {
            result += o.hashCode()
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other is SortedSet<*>) {
            if (arr.size != other.size) {
                return false
            }
            return IteratorUtils.elementsEqual(iterator(), other.iterator())
        }
        if (other is Set<*>) {
            if (arr.size != other.size) {
                return false
            }
            return containsAll(other as Collection<T>)
        }
        return false
    }

    companion object {
        fun <T> of(comparator: Comparator<in T>, arr: Array<T>): ArraySortedSet<T> {
            @Suppress("UNCHECKED_CAST")
            return ArraySortedSet(comparator, arr as Array<Any?>)
        }

        fun <T> of(
            comparator: Comparator<in T>,
            collection: Collection<T>
        ): ArraySortedSet<T> {
            return ArraySortedSet(comparator, collection)
        }

        /* Copies without duplicates and sorts the given collection to create an ArraySortedSet from it */
        fun <T> copyOf(
            comparator: Comparator<in T>,
            collection: Collection<T>
        ): ArraySortedSet<T> {
            val tmp = collection.stream().distinct().sorted(comparator)
                .collect(Collectors.toList()) as List<T>
            return ArraySortedSet(comparator, collectionToArray(tmp))
        }
    }
}

private fun <T> collectionToArray(collection: Collection<T>): Array<Any?> {
    val array = arrayOfNulls<Any?>(collection.size)
    var i = 0
    for (item in collection) {
        array[i++] = item
    }
    return array
}
