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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.TypeListSection
import com.android.tools.smali.dexlib2.writer.pool.TypeListPool.Key

class TypeListPool(dexPool: DexPool) :
    BaseNullableOffsetPool<Key<out Collection<CharSequence>>>(dexPool),
    TypeListSection<CharSequence, Key<out Collection<CharSequence>>> {

    fun intern(types: Collection<CharSequence>) {
        if (types.size > 0) {
            val key = Key<Collection<CharSequence>>(types)
            val prev = internedItems.put(key, 0)
            if (prev == null) {
                for (type in types) {
                    dexPool.typeSection.intern(type)
                }
            }
        }
    }

    override fun getTypes(key: Key<out Collection<CharSequence>>?): Collection<CharSequence> {
        if (key == null) {
            return emptyList()
        }
        return key.types
    }

    override fun getNullableItemOffset(key: Key<out Collection<CharSequence>>?): Int {
        if (key == null || key.types.size == 0) {
            return DexWriter.NO_OFFSET
        } else {
            return super.getNullableItemOffset(key)
        }
    }

    class Key<TypeCollection : Collection<CharSequence>>(val types: TypeCollection) :
        Comparable<Key<out Collection<CharSequence>>> {
        override fun hashCode(): Int {
            var hashCode = 1
            for (type in types) {
                hashCode = hashCode * 31 + type.toString().hashCode()
            }
            return hashCode
        }

        override fun equals(other: Any?): Boolean {
            if (other is Key<*>) {
                val otherKey = other
                if (types.size != otherKey.types.size) {
                    return false
                }
                val otherTypes = otherKey.types.iterator()
                for (type in types) {
                    if (type.toString() != otherTypes.next().toString()) {
                        return false
                    }
                }
                return true
            }
            return false
        }

        override fun toString(): String {
            return buildString {
                for (type in types) {
                    append(type.toString())
                }
            }
        }

        override fun compareTo(other: Key<out Collection<CharSequence>>): Int {
            val otherIterator = other.types.iterator()
            for (type in types) {
                if (!otherIterator.hasNext()) {
                    return 1
                }
                val comparison = type.toString().compareTo(otherIterator.next().toString())
                if (comparison != 0) {
                    return comparison
                }
            }
            if (otherIterator.hasNext()) {
                return -1
            }
            return 0
        }
    }
}
