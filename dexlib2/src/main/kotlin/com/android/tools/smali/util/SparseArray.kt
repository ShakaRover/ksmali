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

import java.util.Collections

/**
 * SparseArrays map integers to Objects. Unlike a normal array of Objects,
 * there can be gaps in the indices. It is intended to be more efficient
 * than using a HashMap to map Integers to Objects.
 */
class SparseArray<E> {
    private var mGarbage = false

    private var mKeys: IntArray
    private var mValues: Array<Any?>
    private var mSize: Int

    /** Creates a new SparseArray containing no mappings. */
    constructor() : this(10)

    /**
     * Creates a new SparseArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.
     */
    constructor(initialCapacity: Int) {
        mKeys = IntArray(initialCapacity)
        mValues = arrayOfNulls(initialCapacity)
        mSize = 0
    }

    /** Gets the Object mapped from the specified key, or `null` if no such mapping has been made. */
    fun get(key: Int): E? {
        return get(key, null)
    }

    /** Gets the Object mapped from the specified key, or the specified Object if no such mapping exists. */
    @Suppress("UNCHECKED_CAST")
    fun get(key: Int, valueIfKeyNotFound: E?): E? {
        val i = binarySearch(mKeys, 0, mSize, key)

        return if (i < 0 || mValues[i] === DELETED) {
            valueIfKeyNotFound
        } else {
            mValues[i] as E?
        }
    }

    /** Removes the mapping from the specified key, if there was any. */
    fun delete(key: Int) {
        val i = binarySearch(mKeys, 0, mSize, key)

        if (i >= 0) {
            if (mValues[i] !== DELETED) {
                mValues[i] = DELETED
                mGarbage = true
            }
        }
    }

    /** Alias for [delete]. */
    fun remove(key: Int) {
        delete(key)
    }

    private fun gc() {
        val n = mSize
        var o = 0
        val keys = mKeys
        val values = mValues

        for (i in 0 until n) {
            val value = values[i]

            if (value !== DELETED) {
                if (i != o) {
                    keys[o] = keys[i]
                    values[o] = value
                }

                o++
            }
        }

        mGarbage = false
        mSize = o
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there was one.
     */
    fun put(key: Int, value: E) {
        var i = binarySearch(mKeys, 0, mSize, key)

        if (i >= 0) {
            mValues[i] = value
        } else {
            i = i.inv()

            if (i < mSize && mValues[i] === DELETED) {
                mKeys[i] = key
                mValues[i] = value
                return
            }

            if (mGarbage && mSize >= mKeys.size) {
                gc()

                // Search again because indices may have changed.
                i = binarySearch(mKeys, 0, mSize, key).inv()
            }

            if (mSize >= mKeys.size) {
                val n = Math.max(mSize + 1, mKeys.size * 2)

                val nkeys = IntArray(n)
                val nvalues = arrayOfNulls<Any?>(n)

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

    /** Returns the number of key-value mappings that this SparseArray currently stores. */
    fun size(): Int {
        if (mGarbage) {
            gc()
        }

        return mSize
    }

    /**
     * Given an index in the range `0...size()-1`, returns the key from the `index`th
     * key-value mapping that this SparseArray stores.
     */
    fun keyAt(index: Int): Int {
        if (mGarbage) {
            gc()
        }

        return mKeys[index]
    }

    /**
     * Given an index in the range `0...size()-1`, returns the value from the `index`th
     * key-value mapping that this SparseArray stores.
     */
    @Suppress("UNCHECKED_CAST")
    fun valueAt(index: Int): E {
        if (mGarbage) {
            gc()
        }

        return mValues[index] as E
    }

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the `index`th
     * key-value mapping that this SparseArray stores.
     */
    fun setValueAt(index: Int, value: E) {
        if (mGarbage) {
            gc()
        }

        mValues[index] = value
    }

    /**
     * Returns the index for which [keyAt] would return the specified key, or a negative
     * number if the specified key is not mapped.
     */
    fun indexOfKey(key: Int): Int {
        if (mGarbage) {
            gc()
        }

        return binarySearch(mKeys, 0, mSize, key)
    }

    /**
     * Returns an index for which [valueAt] would return the specified value, or a negative
     * number if no keys map to the specified value.
     */
    fun indexOfValue(value: E): Int {
        if (mGarbage) {
            gc()
        }

        for (i in 0 until mSize) {
            if (mValues[i] === value) {
                return i
            }
        }

        return -1
    }

    /** Removes all key-value mappings from this SparseArray. */
    fun clear() {
        val n = mSize
        val values = mValues

        for (i in 0 until n) {
            values[i] = null
        }

        mSize = 0
        mGarbage = false
    }

    /** Puts a key/value pair into the array, optimizing for the case where the key is greater than all existing keys. */
    fun append(key: Int, value: E) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value)
            return
        }

        if (mGarbage && mSize >= mKeys.size) {
            gc()
        }

        val pos = mSize
        if (pos >= mKeys.size) {
            val n = Math.max(pos + 1, mKeys.size * 2)

            val nkeys = IntArray(n)
            val nvalues = arrayOfNulls<Any?>(n)

            System.arraycopy(mKeys, 0, nkeys, 0, mKeys.size)
            System.arraycopy(mValues, 0, nvalues, 0, mValues.size)

            mKeys = nkeys
            mValues = nvalues
        }

        mKeys[pos] = key
        mValues[pos] = value
        mSize = pos + 1
    }

    /**
     * Increases the size of the underlying storage if needed, to ensure that it can
     * hold the specified number of items without having to allocate additional memory
     * @param capacity the number of items
     */
    fun ensureCapacity(capacity: Int) {
        if (mGarbage && mSize >= mKeys.size) {
            gc()
        }

        if (mKeys.size < capacity) {
            val nkeys = IntArray(capacity)
            val nvalues = arrayOfNulls<Any?>(capacity)

            System.arraycopy(mKeys, 0, nkeys, 0, mKeys.size)
            System.arraycopy(mValues, 0, nvalues, 0, mValues.size)

            mKeys = nkeys
            mValues = nvalues
        }
    }

    companion object {
        private val DELETED = Any()

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

    /**
     * @return a read-only list of the values in this SparseArray which are in ascending order, based on their
     * associated key
     */
    @Suppress("UNCHECKED_CAST")
    fun getValues(): List<E> {
        return Collections.unmodifiableList(listOf(*(mValues as Array<E>)))
    }
}
