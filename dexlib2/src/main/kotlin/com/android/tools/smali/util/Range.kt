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

import java.util.Comparator

/*
 * Represents a Range of values of type C. It can have open bounds if the range doesn't include the
 * bound value. It can also be unbounded on the lower or upper side. Simplified version of a guava
 * Range.
 */
class Range<C : Comparable<C>> private constructor(
    val lowerBound: C?,
    val upperBound: C?,
    private val lowerOpen: Boolean,
    private val upperOpen: Boolean,
    private val allValues: Boolean
) {
    fun isEmpty(): Boolean {
        return !allValues && lowerBound == upperBound && (lowerOpen || upperOpen)
    }

    fun openLowerBound(): Boolean {
        return lowerOpen
    }

    fun openUpperBound(): Boolean {
        return upperOpen
    }

    fun hasAllValues(): Boolean {
        return allValues
    }

    fun hasLowerBound(): Boolean {
        return lowerBound != null
    }

    fun hasUpperBound(): Boolean {
        return upperBound != null
    }

    /* Returns true if value is included in the Range */
    fun contains(value: C?): Boolean {
        if (value == null) {
            return false
        }
        if (allValues) {
            return true
        }

        if (lowerBound != null) {
            if (lowerOpen && value.compareTo(lowerBound) == 0) {
                return false
            }
            if (value.compareTo(lowerBound) < 0) {
                return false
            }
        }
        if (upperBound != null) {
            if (upperOpen && value.compareTo(upperBound) == 0) {
                return false
            }
            if (value.compareTo(upperBound) > 0) {
                return false
            }
        }
        return true
    }
    /*
     * Returns true if there exists a (possibly empty) range which is enclosed by both this range
     * and other.
     */
    fun isConnected(other: Range<C>): Boolean {
        return (!hasLowerBound() || !other.hasUpperBound()
            || lowerBound!!.compareTo(other.upperBound!!) <= 0)
            && (!hasUpperBound() || !other.hasLowerBound()
                || other.lowerBound!!.compareTo(upperBound!!) <= 0)
    }

    /* Returns the maximal range enclosed by both this range and other, if such a range exists. */
    fun intersection(other: Range<C>): Range<C>? {
        if (!isConnected(other)) {
            return null
        }

        // select the max of the lowerBounds. If they're equal,
        // choose the range that has an open lower bound
        val lowerBoundRange: Range<C>
        if (!hasLowerBound() || !other.hasLowerBound()) {
            lowerBoundRange = if (hasLowerBound()) this else other
        } else if (lowerBound == other.lowerBound) {
            lowerBoundRange = if (lowerOpen) this else other
        } else {
            lowerBoundRange = if (lowerBound!!.compareTo(other.lowerBound!!) > 0) this else other
        }

        // and the min of the upperBounds, or the open upper bound if they're equal
        val upperBoundRange: Range<C>
        if (!hasUpperBound() || !other.hasUpperBound()) {
            upperBoundRange = if (hasUpperBound()) this else other
        } else if (upperBound == other.upperBound) {
            upperBoundRange = if (upperOpen) this else other
        } else {
            upperBoundRange = if (upperBound!!.compareTo(other.upperBound!!) < 0) this else other
        }

        return Range(
            lowerBoundRange.lowerBound,
            upperBoundRange.upperBound,
            lowerBoundRange.openLowerBound(),
            upperBoundRange.openUpperBound(),
            false
        )
    }
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Range<*>) {
            return false
        }

        if (allValues != other.hasAllValues()) {
            return false
        }

        return lowerBound == other.lowerBound
            && upperBound == other.upperBound
            && lowerOpen == other.openLowerBound()
            && upperOpen == other.openUpperBound()
    }

    override fun toString(): String {
        if (allValues) {
            return "[*]"
        }
        var sb = ""
        sb += if (lowerOpen) "(" else "["
        sb += lowerBound
        sb += ", "
        sb += upperBound
        sb += if (upperOpen) ")" else "]"
        return sb
    }

    companion object {
        val RANGE_LEX_COMPARATOR: Comparator<Range<*>> = Comparator { left, right ->
            compareLexicographically(left, right)
        }

        @Suppress("UNCHECKED_CAST")
        private fun compareLexicographically(left: Range<*>, right: Range<*>): Int {
            var cmp = 0
            if (!left.hasLowerBound() && right.hasLowerBound()) {
                return -1
            } else if (!right.hasLowerBound() && left.hasLowerBound()) {
                return 1
            } else if (left.hasLowerBound() && right.hasLowerBound()) {
                cmp = (left.lowerBound as Comparable<Any>).compareTo(right.lowerBound as Any)
            }

            if (cmp != 0) {
                return cmp
            }
            if (!left.hasUpperBound() && right.hasUpperBound()) {
                return 1
            } else if (!right.hasUpperBound() && left.hasUpperBound()) {
                return -1
            } else if (left.hasUpperBound() && right.hasUpperBound()) {
                cmp = (left.upperBound as Comparable<Any>).compareTo(right.upperBound as Any)
            }
            return cmp
        }

        fun <C : Comparable<C>> closed(lowerBound: C, upperBound: C): Range<C> {
            if (lowerBound.compareTo(upperBound) > 0) {
                throw IllegalArgumentException("lowerBound must be <= upperBound")
            }
            return Range(lowerBound, upperBound, false, false, false)
        }

        fun <C : Comparable<C>> open(lowerBound: C, upperBound: C): Range<C> {
            if (lowerBound.compareTo(upperBound) > 0) {
                throw IllegalArgumentException("lowerBound must be <= upperBound")
            }
            return Range(lowerBound, upperBound, true, true, false)
        }
        fun <C : Comparable<C>> openClosed(lowerBound: C, upperBound: C): Range<C> {
            if (lowerBound.compareTo(upperBound) > 0) {
                throw IllegalArgumentException("lowerBound must be <= upperBound")
            }
            return Range(lowerBound, upperBound, true, false, false)
        }

        fun <C : Comparable<C>> closedOpen(lowerBound: C, upperBound: C): Range<C> {
            if (lowerBound.compareTo(upperBound) > 0) {
                throw IllegalArgumentException("lowerBound must be <= upperBound")
            }
            return Range(lowerBound, upperBound, false, true, false)
        }

        fun <C : Comparable<C>> atLeast(lowerBound: C): Range<C> {
            return Range(lowerBound, null, false, false, false)
        }

        fun <C : Comparable<C>> atMost(upperBound: C): Range<C> {
            return Range(null, upperBound, false, false, false)
        }

        fun <C : Comparable<C>> allValues(): Range<C> {
            return Range(null, null, false, false, true)
        }
    }
}
