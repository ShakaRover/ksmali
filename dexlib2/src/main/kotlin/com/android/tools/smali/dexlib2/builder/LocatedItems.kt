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


package com.android.tools.smali.dexlib2.builder

abstract class LocatedItems<T : ItemWithLocation> {
    // We end up creating and keeping around a *lot* of MethodLocation objects
    // when building a new dex file, so it's worth the trouble of lazily creating
    // the labels and debugItems lists only when they are needed
    private var items: MutableList<T>? = null

    fun getModifiableItems(newItemsLocation: MethodLocation): MutableSet<T> {
        return object : AbstractMutableSet<T>() {
            override fun iterator(): MutableIterator<T> {
                val it: MutableIterator<T> = items?.iterator()
                    ?: ArrayList<T>().iterator()

                return object : MutableIterator<T> {
                    private var currentItem: T? = null

                    override fun hasNext(): Boolean = it.hasNext()

                    override fun next(): T {
                        val item = it.next()
                        currentItem = item
                        return item
                    }

                    override fun remove() {
                        val current = currentItem
                        if (current != null) {
                            current.location = null
                        }
                        it.remove()
                    }
                }
            }

            override val size: Int
                get() = items?.size ?: 0

            override fun add(element: T): Boolean {
                if (element.isPlaced) {
                    throw IllegalArgumentException(getAddLocatedItemError())
                }
                element.location = newItemsLocation
                addItem(element)
                return true
            }
        }
    }

    private fun addItem(item: T) {
        if (items == null) {
            items = ArrayList(1)
        }
        items!!.add(item)
    }

    protected abstract fun getAddLocatedItemError(): String

    fun mergeItemsIntoNext(nextLocation: MethodLocation, otherLocatedItems: LocatedItems<T>) {
        if (otherLocatedItems === this) {
            return
        }
        val currentItems = items
        if (currentItems != null) {
            for (item in currentItems) {
                item.location = nextLocation
            }
            val mergedItems = currentItems
            val otherItems = otherLocatedItems.items
            if (otherItems != null) {
                mergedItems.addAll(otherItems)
            }
            otherLocatedItems.items = mergedItems
            items = null
        }
    }
}
