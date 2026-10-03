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

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.SortedSet
import java.util.function.Predicate

class CollectionUtils private constructor() {

    class UsingToStringOrdering<T : Any> private constructor() : Comparator<T> {
        override fun compare(left: T, right: T): Int {
            return left.toString().compareTo(right.toString())
        }

        companion object {
            val INSTANCE: UsingToStringOrdering<Any> = UsingToStringOrdering()
        }
    }

    class NaturalOrdering<T> private constructor() : Comparator<T> {
        @Suppress("UNCHECKED_CAST")
        override fun compare(left: T, right: T): Int {
            return (left as Comparable<Any>).compareTo(right as Any)
        }

        companion object {
            val INSTANCE: NaturalOrdering<Any> = NaturalOrdering()
        }
    }

    companion object {
        fun <T> listHashCode(iterable: Iterable<T>): Int {
            var hashCode = 1
            for (item in iterable) {
                hashCode = hashCode * 31 + item.hashCode()
            }
            return hashCode
        }

        fun <T> lastIndexOf(iterable: Iterable<T>, predicate: Predicate<in T>): Int {
            var index = 0
            var lastMatchingIndex = -1
            for (item in iterable) {
                if (predicate.test(item)) {
                    lastMatchingIndex = index
                }
                index++
            }
            return lastMatchingIndex
        }
        fun <T : Comparable<T>> compareAsList(list1: Collection<out T>, list2: Collection<out T>): Int {
            var res = list1.size.compareTo(list2.size)
            if (res != 0) return res
            val elements2 = list2.iterator()
            for (element1 in list1) {
                res = element1.compareTo(elements2.next())
                if (res != 0) return res
            }
            return 0
        }

        fun <T> compareAsIterable(
            comparator: Comparator<in T>,
            it1: Iterable<out T>,
            it2: Iterable<out T>
        ): Int {
            val elements2 = it2.iterator()
            for (element1 in it1) {
                if (!elements2.hasNext()) {
                    return 1
                }
                val element2 = elements2.next()
                val res = comparator.compare(element1, element2)
                if (res != 0) return res
            }
            if (elements2.hasNext()) {
                return -1
            }
            return 0
        }

        fun <T : Comparable<T>> compareAsIterable(it1: Iterable<out T>, it2: Iterable<out T>): Int {
            val elements2 = it2.iterator()
            for (element1 in it1) {
                if (!elements2.hasNext()) {
                    return 1
                }
                val element2 = elements2.next()
                val res = element1.compareTo(element2)
                if (res != 0) return res
            }
            if (elements2.hasNext()) {
                return -1
            }
            return 0
        }

        fun <T> compareAsList(
            elementComparator: Comparator<in T>,
            list1: Collection<out T>,
            list2: Collection<out T>
        ): Int {
            var res = list1.size.compareTo(list2.size)
            if (res != 0) return res
            val elements2 = list2.iterator()
            for (element1 in list1) {
                res = elementComparator.compare(element1, elements2.next())
                if (res != 0) return res
            }
            return 0
        }

        fun <T> listComparator(elementComparator: Comparator<in T>): Comparator<Collection<out T>> {
            return Comparator { list1, list2 ->
                compareAsList(elementComparator, list1, list2)
            }
        }
        fun <T> isNaturalSortedSet(it: Iterable<out T>): Boolean {
            if (it is SortedSet<*>) {
                val comparator = it.comparator()
                return comparator == null || comparator == NaturalOrdering.INSTANCE
            }
            return false
        }

        fun <T> isSortedSet(elementComparator: Comparator<*>, it: Iterable<out T>): Boolean {
            if (it is SortedSet<*>) {
                val comparator = it.comparator()
                if (comparator == null) {
                    return elementComparator == NaturalOrdering.INSTANCE
                }
                return elementComparator == comparator
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> toNaturalSortedSet(collection: Collection<out T>): SortedSet<T> {
            if (isNaturalSortedSet(collection)) {
                return collection as SortedSet<T>
            }
            val sortedSet = ArraySortedSet.copyOf(naturalOrdering<T>(), collection)
            return Collections.unmodifiableSortedSet(sortedSet)
        }

        private fun <T> toSortedSet(
            elementComparator: Comparator<in T>,
            collection: Collection<out T>
        ): SortedSet<T> {
            if (collection is SortedSet<*>) {
                val comparator = collection.comparator()
                if (comparator != null && comparator == elementComparator) {
                    @Suppress("UNCHECKED_CAST")
                    return collection as SortedSet<T>
                }
            }

            return Collections.unmodifiableSortedSet(ArraySortedSet.copyOf(elementComparator, collection))
        }
        fun <T> setComparator(elementComparator: Comparator<in T>): Comparator<Collection<out T>> {
            return Comparator { list1, list2 ->
                compareAsSet(elementComparator, list1, list2)
            }
        }

        fun <T : Comparable<T>> compareAsSet(set1: Collection<out T>, set2: Collection<out T>): Int {
            val sortedSet1 = toNaturalSortedSet(set1)
            val sortedSet2 = toNaturalSortedSet(set2)
            return compareAsIterable(sortedSet1, sortedSet2)
        }

        fun <T> compareAsSet(
            elementComparator: Comparator<in T>,
            list1: Collection<out T>,
            list2: Collection<out T>
        ): Int {
            val set1 = toSortedSet(elementComparator, list1)
            val set2 = toSortedSet(elementComparator, list2)
            return compareAsIterable(elementComparator, set1, set2)
        }
        fun <T> immutableSortedCopy(collection: Collection<T>, comparator: Comparator<in T>): MutableList<T> {
            val copy = ArrayList(collection)
            copy.sortWith(comparator)
            return Collections.unmodifiableList(copy)
        }

        fun <T> usingToStringOrdering(): Comparator<in T> {
            @Suppress("UNCHECKED_CAST")
            return UsingToStringOrdering.INSTANCE as Comparator<in T>
        }

        fun <T> naturalOrdering(): Comparator<in T> {
            @Suppress("UNCHECKED_CAST")
            return NaturalOrdering.INSTANCE as Comparator<in T>
        }
    }
}
