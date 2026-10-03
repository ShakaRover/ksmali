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

import java.util.Collections
import java.util.Map.Entry

/*
 * Based on guava's ImmutableRangeMap
 */
class UnmodifiableRangeMap<K : Comparable<K>, V> private constructor(
    ranges: List<Range<K>>,
    values: List<V>
) {
    private val ranges: List<Range<K>> = Collections.unmodifiableList(ranges)
    private val values: List<V> = Collections.unmodifiableList(values)

    fun get(key: K?): V? {
        if (key == null) {
            return null
        }

        val index = rangeBinarySearch(ranges, key)
        if (index == -1) {
            return null
        } else {
            val range = ranges[index]
            return if (range.contains(key)) values[index] else null
        }
    }

    fun getEntry(key: K?): Entry<Range<K>, V>? {
        if (key == null) {
            return null
        }

        val index = rangeBinarySearch(ranges, key)
        if (index == -1) {
            return null
        } else {
            val range = ranges[index]
            return if (range.contains(key)) UnmodifiableEntry(range, values[index]) else null
        }
    }
    class Builder<K : Comparable<K>, V> {
        private val entries: MutableList<Entry<Range<K>, V>> = ArrayList()

        fun put(range: Range<K>?, value: V?): Builder<K, V> {
            if (range == null || value == null) {
                throw NullPointerException("Both range and value must be non-null")
            }

            if (range.isEmpty()) {
                throw IllegalArgumentException("Ranges cannot be empty")
            }
            entries.add(UnmodifiableEntry(range, value!!))
            return this
        }

        fun build(): UnmodifiableRangeMap<K, V> {
            entries.sortWith(Comparator { e1, e2 ->
                Range.RANGE_LEX_COMPARATOR.compare(e1.key, e2.key)
            })
            val rangesList = ArrayList<Range<K>>(entries.size)
            val valuesList = ArrayList<V>(entries.size)
            for (i in entries.indices) {
                val range = entries[i].key
                if (i > 0) {
                    val prevRange = entries[i - 1].key
                    if (range.isConnected(prevRange) && !range.intersection(prevRange)!!.isEmpty()) {
                        throw IllegalArgumentException(
                            "Overlapping ranges: range " + prevRange + " overlaps with entry " + range
                        )
                    }
                }
                rangesList.add(range)
                valuesList.add(entries[i].value)
            }
            return UnmodifiableRangeMap(rangesList, valuesList)
        }
    }

    class UnmodifiableEntry<K, V>(private val key: K, private val value: V) : Entry<K, V> {
        override fun getKey(): K {
            return key
        }

        override fun getValue(): V {
            return value
        }

        override fun setValue(value: V): V {
            throw UnsupportedOperationException()
        }
    }
    companion object {
        private val EMPTY: UnmodifiableRangeMap<Int, Any> = UnmodifiableRangeMap(emptyList(), emptyList())

        @JvmStatic
        fun <K : Comparable<K>, V> builder(): Builder<K, V> {
            return Builder()
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <K : Comparable<K>, V> of(): UnmodifiableRangeMap<K, V> {
            return EMPTY as UnmodifiableRangeMap<K, V>
        }

        private fun <T : Comparable<T>> rangeBinarySearch(l: List<Range<T>>, key: T): Int {
            var low = 0
            var high = l.size - 1

            while (low <= high) {
                val mid = (low + high) ushr 1
                val midRange = l[mid]
                if (midRange.contains(key)) {
                    return mid
                }

                val cmp = if (midRange.hasLowerBound()) key.compareTo(midRange.lowerBound!!)
                else key.compareTo(midRange.upperBound!!)

                if (cmp > 0)
                    low = mid + 1
                else if (cmp < 0)
                    high = mid - 1
                else
                    return mid // key found
            }
            return -1 // key not found
        }
    }
}
