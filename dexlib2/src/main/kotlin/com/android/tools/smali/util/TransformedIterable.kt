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

import java.util.function.Function

/**
 * An wrapping instance that will return the [TransformedIterator] of [backingIterator]
 * and [transformFunction].
 *
 * The returned iterator supports `remove()` if [backingIterator] does.
 */
class TransformedIterable<F, T>(
    private val backingIterable: Iterable<F>,
    private val transformFunction: Function<F, T>
) : Iterable<T> {
    final override fun iterator(): MutableIterator<T> {
        return TransformedIterator(backingIterable.iterator(), transformFunction)
    }

    /**
     * An iterator that will return the results of applying [transformFunction] to each
     * element of [backingIterator].
     *
     * The returned iterator supports `remove()` if [backingIterator] does.
     */
    class TransformedIterator<G, U> : MutableIterator<U> {
        private val backingIterator: Iterator<G>
        private val transformFunction: Function<G, U>

        constructor(
            backingIterable: Iterable<G>,
            transformFunction: Function<G, U>
        ) : this(backingIterable.iterator(), transformFunction)

        constructor(
            backingIterator: Iterator<G>,
            transformFunction: Function<G, U>
        ) {
            this.backingIterator = backingIterator
            this.transformFunction = transformFunction
        }

        final override fun hasNext(): Boolean {
            return backingIterator.hasNext()
        }

        final override fun next(): U {
            return transformFunction.apply(backingIterator.next())
        }

        final override fun remove() {
            (backingIterator as MutableIterator<G>).remove()
        }
    }
}
