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


package com.android.tools.smali.dexlib2.analysis.util

import java.util.LinkedHashMap

/**
 * Based on android.util.LruCache.
 * A cache that holds strong references to a limited number of values. Each time
 * a value is accessed, it is moved to the head of a queue. When a value is
 * added to a full cache, the value at the end of that queue is evicted and may
 * become eligible for garbage collection.
 *
 * <p>If your cached values hold resources that need to be explicitly released,
 * override [.entryRemoved].
 *
 * <p>If a cache miss should be computed on demand for the corresponding keys,
 * override [.create]. This simplifies the calling code, allowing it to
 * assume a value will always be returned, even when there's a cache miss.
 *
 * <p>By default, the cache size is measured in the number of entries. Override
 * [.sizeOf] to size the cache in different units. For example, this cache
 * is limited to 4MiB of bitmaps:
 * <pre>   [code
 *   int cacheSize = 4 * 1024 * 1024; // 4MiB
 *   LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
 *       protected int sizeOf(String key, Bitmap value) {
 *           return value.getByteCount();
 *       }
 *   }]</pre>
 *
 * <p>This class is thread-safe. Perform multiple cache operations atomically by
 * synchronizing on the cache: <pre>   [code
 *   synchronized (cache) {
 *     if (cache.get(key) == null) {
 *         cache.put(key, value);
 *     }
 *   }]</pre>
 *
 * <p>This class does not allow null to be used as a key or value. A return
 * value of null from [.get], [.put] or [.remove] is
 * unambiguous: the key was not in the cache.
 */
open class LruCache<K, V>(maxSize: Int) {
    private val map: LinkedHashMap<K, V>

    /** Size of this cache in units. Not necessarily the number of elements. */
    private var sizeValue = 0
    private var maxSizeValue: Int

    private var putCountValue = 0
    private var createCountValue = 0
    private var evictionCountValue = 0
    private var hitCountValue = 0
    private var missCountValue = 0

    /**
     * @param maxSize for caches that do not override [.sizeOf], this is
     * the maximum number of entries in the cache. For all other caches,
     * this is the maximum sum of the sizes of the entries in this cache.
     */
    init {
        if (maxSize <= 0) {
            throw IllegalArgumentException("maxSize <= 0")
        }
        this.maxSizeValue = maxSize
        this.map = LinkedHashMap(0, 0.75f, true)
    }

    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    fun resize(maxSize: Int) {
        if (maxSize <= 0) {
            throw IllegalArgumentException("maxSize <= 0")
        }

        synchronized(this) {
            this.maxSizeValue = maxSize
        }
        trimToSize(maxSize)
    }

    /**
     * Returns the value for `key` if it exists in the cache or can be
     * created by `#create`. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    fun get(key: K): V? {
        if (key == null) {
            throw NullPointerException("key == null")
        }

        var mapValue: V? = null
        synchronized(this) {
            mapValue = map[key]
            if (mapValue != null) {
                hitCountValue++
                return mapValue
            }
            missCountValue++
        }

        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */

        val createdValue = create(key)
        if (createdValue == null) {
            return null
        }

        synchronized(this) {
            createCountValue++
            mapValue = map.put(key, createdValue)

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue)
            } else {
                sizeValue += safeSizeOf(key, createdValue)
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue)
            return mapValue
        } else {
            trimToSize(maxSizeValue)
            return createdValue
        }
    }

    /**
     * Caches `value` for `key`. The value is moved to the head of
     * the queue.
     *
     * @return the previous value mapped by `key`.
     */
    fun put(key: K, value: V): V? {
        if (key == null || value == null) {
            throw NullPointerException("key == null || value == null")
        }

        var previous: V? = null
        synchronized(this) {
            putCountValue++
            sizeValue += safeSizeOf(key, value)
            previous = map.put(key, value)
            if (previous != null) {
                sizeValue -= safeSizeOf(key, previous)
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value)
        }

        trimToSize(maxSizeValue)
        return previous
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     * to evict even 0-sized elements.
     */
    fun trimToSize(maxSize: Int) {
        while (true) {
            var key: K? = null
            var value: V? = null
            synchronized(this) {
                if (sizeValue < 0 || (map.isEmpty() && sizeValue != 0)) {
                    throw IllegalStateException(
                        javaClass.name + ".sizeOf() is reporting inconsistent results!"
                    )
                }

                if (sizeValue <= maxSize) {
                    return
                }

                val toEvict = eldest()
                if (toEvict == null) {
                    return
                }

                key = toEvict.key
                value = toEvict.value
                map.remove(key)
                sizeValue -= safeSizeOf(key!!, value!!)
                evictionCountValue++
            }

            entryRemoved(true, key!!, value!!, null)
        }
    }

    private fun eldest(): Map.Entry<K, V>? {
        val it = map.entries.iterator()
        return if (it.hasNext()) it.next() else null
    }

    /**
     * Removes the entry for `key` if it exists.
     *
     * @return the previous value mapped by `key`.
     */
    fun remove(key: K): V? {
        if (key == null) {
            throw NullPointerException("key == null")
        }

        var previous: V? = null
        synchronized(this) {
            previous = map.remove(key)
            if (previous != null) {
                sizeValue -= safeSizeOf(key, previous)
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null)
        }

        return previous
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * [.remove], or replaced by a call to [.put]. The default
     * implementation does nothing.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted true if the entry is being removed to make space, false
     * if the removal was caused by a [.put] or a [.remove].
     * @param newValue the new value for `key`, if it exists. If non-null,
     * this removal was caused by a [.put] or a [.get]. Otherwise it was caused by
     * an eviction or a [.remove].
     */
    protected open fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {}

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * <p>If a value for `key` exists in the cache when this method
     * returns, the created value will be released with [.entryRemoved]
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls [.put] while another is creating a value for the same
     * key.
     */
    protected open fun create(key: K): V? {
        return null
    }

    private fun safeSizeOf(key: K, value: V): Int {
        val result = sizeOf(key, value)
        if (result < 0) {
            throw IllegalStateException("Negative size: $key=$value")
        }
        return result
    }

    /**
     * Returns the size of the entry for `key` and `value` in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * <p>An entry's size must not change while it is in the cache.
     */
    protected open fun sizeOf(key: K, value: V): Int {
        return 1
    }

    /**
     * Clear the cache, calling [.entryRemoved] on each removed entry.
     */
    fun evictAll() {
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    /**
     * For caches that do not override [.sizeOf], this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    @Synchronized
    fun size(): Int {
        return sizeValue
    }

    /**
     * For caches that do not override [.sizeOf], this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    @Synchronized
    fun maxSize(): Int {
        return maxSizeValue
    }

    /**
     * Returns the number of times [.get] returned a value that was
     * already present in the cache.
     */
    @Synchronized
    fun hitCount(): Int {
        return hitCountValue
    }

    /**
     * Returns the number of times [.get] returned null or required a new
     * value to be created.
     */
    @Synchronized
    fun missCount(): Int {
        return missCountValue
    }

    /**
     * Returns the number of times [.create] returned a value.
     */
    @Synchronized
    fun createCount(): Int {
        return createCountValue
    }

    /**
     * Returns the number of times [.put] was called.
     */
    @Synchronized
    fun putCount(): Int {
        return putCountValue
    }

    /**
     * Returns the number of values that have been evicted.
     */
    @Synchronized
    fun evictionCount(): Int {
        return evictionCountValue
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    @Synchronized
    fun snapshot(): Map<K, V> {
        return LinkedHashMap(map)
    }

    @Synchronized
    override fun toString(): String {
        val accesses = hitCountValue + missCountValue
        val hitPercent = if (accesses != 0) 100 * hitCountValue / accesses else 0
        return "LruCache[maxSize=${maxSizeValue},hits=${hitCountValue},misses=${missCountValue},hitRate=${hitPercent}%]"
    }
}
