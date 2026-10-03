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

import java.util.AbstractSequentialList
import java.util.NoSuchElementException

abstract class AbstractForwardSequentialList<T> : AbstractSequentialList<T>() {

    private fun iterator(index: Int): MutableIterator<T> {
        if (index < 0) {
            throw NoSuchElementException()
        }

        val it = iterator()
        for (i in 0 until index) {
            it.next()
        }
        return it
    }

    abstract override fun iterator(): MutableIterator<T>

    override fun listIterator(initialIndex: Int): MutableListIterator<T> {

        val initialIterator: MutableIterator<T>
        try {
            initialIterator = iterator(initialIndex)
        } catch (ex: NoSuchElementException) {
            throw IndexOutOfBoundsException()
        }

        return object : AbstractListIterator<T>() {
            private var index = initialIndex - 1
            private var forwardIterator: MutableIterator<T>? = initialIterator

            private fun getForwardIterator(): MutableIterator<T> {
                if (forwardIterator == null) {
                    try {
                        forwardIterator = iterator(index + 1)
                    } catch (ex: IndexOutOfBoundsException) {
                        throw NoSuchElementException()
                    }
                }
                return forwardIterator!!
            }

            override fun hasNext(): Boolean {
                return getForwardIterator().hasNext()
            }

            override fun hasPrevious(): Boolean {
                return index >= 0
            }

            override fun next(): T {
                val ret = getForwardIterator().next()
                index++
                return ret
            }

            override fun nextIndex(): Int {
                return index + 1
            }

            override fun previous(): T {
                forwardIterator = null
                try {
                    return iterator(index--).next()
                } catch (ex: IndexOutOfBoundsException) {
                    throw NoSuchElementException()
                }
            }

            override fun previousIndex(): Int {
                return index
            }
        }
    }

    override fun listIterator(): MutableListIterator<T> {
        return listIterator(0)
    }
}
