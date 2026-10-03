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

import java.util.NoSuchElementException

abstract class AbstractIterator<T> : MutableIterator<T>, Iterable<T> {
    private var state = STATE_NOT_READY
    private var nextElement: T? = null

    protected fun endOfData(): T? {
        state = STATE_DONE
        return null
    }

    final override fun hasNext(): Boolean {
        when (state) {
            STATE_DONE -> return false
            STATE_READY -> return true
            STATE_FAILED -> throw IllegalStateException()
            else -> {}
        }
        return tryToComputeNext()
    }

    private fun tryToComputeNext(): Boolean {
        state = STATE_FAILED // temporary pessimism
        nextElement = computeNext()
        if (state != STATE_DONE) {
            state = STATE_READY
            return true
        }
        return false
    }

    final override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        state = STATE_NOT_READY
        val result = nextElement
        nextElement = null
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    final override fun iterator(): Iterator<T> {
        return this
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }

    /**
     * Computes next item for the iterator. If the end of the list has been reached, it should call
     * endOfData. endOfData has a return value of T, so you can simply `return endOfData()`
     *
     * @return The item that was read. If endOfData was called, the return value is ignored.
     */
    protected abstract fun computeNext(): T?

    private companion object {
        /** We have computed the next element and haven't returned it yet. */
        const val STATE_READY = 1

        /** We haven't yet computed or have already returned the element. */
        const val STATE_NOT_READY = 2

        /** We have reached the end of the data and are finished. */
        const val STATE_DONE = 3

        /** We've suffered an exception and are kaputt. */
        const val STATE_FAILED = 4
    }
}
