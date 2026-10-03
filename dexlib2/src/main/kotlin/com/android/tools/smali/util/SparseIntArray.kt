/*
 * Copyright 2013, Google LLC
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

/**
 * SparseIntArrays map integers to integers. Unlike a normal array of integers,
 * there can be gaps in the indices. It is intended to be more efficient
 * than using a HashMap to map Integers to Integers.
 */
class SparseIntArray {
    private var mKeys: IntArray
    private var mValues: IntArray
    private var mSize: Int

    /** Creates a new SparseIntArray containing no mappings. */
    constructor() : this(10)

    /**
     * Creates a new SparseIntArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.
     */
    constructor(initialCapacity: Int) {
        mKeys = IntArray(initialCapacity)
        mValues = IntArray(initialCapacity)
        mSize = 0
    }

    /** Gets the int mapped from the specified key, or `0` if no such mapping has been made. */
    fun get(key: Int): Int {
        return get(key, 0)
    }

    /** Gets the int mapped from the specified key, or the specified value if no such mapping exists. */
    fun get(key: Int, valueIfKeyNotFound: Int): Int {
        val i = binarySearch(mKeys, 0, mSize, key)

        return if (i < 0) {
            valueIfKeyNotFound
        } else {
            mValues[i]
        }
    }

    /** Gets the int mapped from the specified key, or the closest key that is less than the specified key. */
    fun getClosestSmaller(key: Int): Int {
        var i = binarySearch(mKeys, 0, mSize, key)

        if (i < 0) {
            i = i.inv()
            if (i > 0) {
                i--
            }
            return mValues[i]
        } else {
            return mValues[i]
        }
    }
    /** Removes the mapping from the specified key, if there was any. */
    fun delete(key: Int) {
        val i = binarySearch(mKeys, 0, mSize, key)

        if (i >= 0) {
            removeAt(i)
        }
    }

    /** Removes the mapping at the given index. */
    fun removeAt(index: Int) {
        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1))
        System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1))
        mSize--
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there was one.
     */
    fun put(key: Int, value: Int) {
        var i = binarySearch(mKeys, 0, mSize, key)

        if (i >= 0) {
            mValues[i] = value
        } else {
            i = i.inv()

            if (mSize >= mKeys.size) {
                val n = Math.max(mSize + 1, mKeys.size * 2)

                val nkeys = IntArray(n)
                val nvalues = IntArray(n)

                System.arraycopy(mKeys, 0, nkeys, 0, mKeys.size)
                System.arraycopy(mValues, 0, nvalues, 0, mValues.size)

                mKeys = nkeys
                mValues = nvalues
            }

            if (mSize - i != 0) {
                System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i)
                System.arraycopy(mValues, i, mValues, i + 1, mSize - i)
            }

            mKeys[i] = key
            mValues[i] = value
            mSize++
        }
    }
    /** Returns the number of key-value mappings that this SparseIntArray currently stores. */
    fun size(): Int {
        return mSize
    }

    /**
     * Given an index in the range `0...size()-1`, returns the key from the `index`th
     * key-value mapping that this SparseIntArray stores.
     */
    fun keyAt(index: Int): Int {
        return mKeys[index]
    }

    /**
     * Given an index in the range `0...size()-1`, returns the value from the `index`th
     * key-value mapping that this SparseIntArray stores.
     */
    fun valueAt(index: Int): Int {
        return mValues[index]
    }

    /**
     * Returns the index for which [keyAt] would return the specified key, or a negative
     * number if the specified key is not mapped.
     */
    fun indexOfKey(key: Int): Int {
        return binarySearch(mKeys, 0, mSize, key)
    }

    /**
     * Returns an index for which [valueAt] would return the specified value, or a negative
     * number if no keys map to the specified value.
     */
    fun indexOfValue(value: Int): Int {
        for (i in 0 until mSize) {
            if (mValues[i] == value) {
                return i
            }
        }

        return -1
    }

    /** Removes all key-value mappings from this SparseIntArray. */
    fun clear() {
        mSize = 0
    }

    /** Puts a key/value pair into the array, optimizing for the case where the key is greater than all existing keys. */
    fun append(key: Int, value: Int) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value)
            return
        }

        val pos = mSize
        if (pos >= mKeys.size) {
            val n = Math.max(pos + 1, mKeys.size * 2)

            val nkeys = IntArray(n)
            val nvalues = IntArray(n)

            System.arraycopy(mKeys, 0, nkeys, 0, mKeys.size)
            System.arraycopy(mValues, 0, nvalues, 0, mValues.size)

            mKeys = nkeys
            mValues = nvalues
        }

        mKeys[pos] = key
        mValues[pos] = value
        mSize = pos + 1
    }

    companion object {
        @JvmStatic
        private fun binarySearch(a: IntArray, start: Int, len: Int, key: Int): Int {
            var high = start + len
            var low = start - 1
            var guess: Int

            while (high - low > 1) {
                guess = (high + low) / 2

                if (a[guess] < key) {
                    low = guess
                } else {
                    high = guess
                }
            }

            return if (high == start + len) {
                (start + len).inv()
            } else if (a[high] == key) {
                high
            } else {
                high.inv()
            }
        }
    }
}
