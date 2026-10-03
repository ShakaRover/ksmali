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

import java.util.ArrayList
import java.util.function.Predicate

object IteratorUtils {
    @JvmStatic
    fun <T : Any> getLast(iterator: Iterator<T>): T {
        while (true) {
            val current = iterator.next()
            if (!iterator.hasNext()) {
                return current
            }
        }
    }

    @JvmStatic
    fun <T : Any> filter(
        unfiltered: Iterable<T>,
        retainIfTrue: Predicate<in T>
    ): AbstractIterator<T> {
        return filter(unfiltered.iterator(), retainIfTrue)
    }

    @JvmStatic
    fun <T : Any> filter(
        unfiltered: Iterator<T>,
        retainIfTrue: Predicate<in T>
    ): AbstractIterator<T> {
        return object : AbstractIterator<T>() {
            override fun computeNext(): T? {
                while (unfiltered.hasNext()) {
                    val next = unfiltered.next()
                    if (retainIfTrue.test(next)) {
                        return next
                    }
                }
                return endOfData()
            }
        }
    }

    @JvmStatic
    fun <T : Any> toList(iterable: Iterable<T>): MutableList<T> {
        return toList(iterable.iterator())
    }

    @JvmStatic
    fun <T : Any> toList(iterator: Iterator<T>): MutableList<T> {
        val list = ArrayList<T>()
        while (iterator.hasNext()) {
            list.add(iterator.next())
        }
        return list
    }

    @JvmStatic
    fun <T : Any> addAll(collection: MutableCollection<T>, iterator: Iterator<T>) {
        while (iterator.hasNext()) {
            collection.add(iterator.next())
        }
    }

    @JvmStatic
    fun elementsEqual(iterator1: Iterator<*>, iterator2: Iterator<*>): Boolean {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false
            }
            val o1 = iterator1.next()
            val o2 = iterator2.next()
            if (o1 != o2) {
                return false
            }
        }
        return !iterator2.hasNext()
    }

    @JvmStatic
    fun size(iterable: Iterable<*>): Int {
        val iterator = iterable.iterator()
        var count = 0
        while (iterator.hasNext()) {
            count++
            iterator.next()
        }
        return count
    }
}
