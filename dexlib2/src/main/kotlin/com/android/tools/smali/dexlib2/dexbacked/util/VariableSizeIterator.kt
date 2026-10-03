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

package com.android.tools.smali.dexlib2.dexbacked.util

import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import java.util.NoSuchElementException

abstract class VariableSizeIterator<T> : MutableIterator<T> {
    private val reader: DexReader<out DexBuffer>
    protected val size: Int

    private var index = 0

    protected constructor(buffer: DexBuffer, offset: Int, size: Int) {
        this.reader = buffer.readerAt(offset)
        this.size = size
    }

    protected constructor(reader: DexReader<out DexBuffer>, size: Int) {
        this.reader = reader
        this.size = size
    }

    /**
     * Reads the next item from reader.
     *
     * @param reader The `DexReader` to read the next item from
     * @param index The index of the item being read. This is guaranteed to be less than `size`
     * @return The item that was read
     */
    protected abstract fun readNextItem(reader: DexReader<out DexBuffer>, index: Int): T

    fun getReaderOffset(): Int {
        return reader.offset
    }

    override fun hasNext(): Boolean {
        return index < size
    }

    override fun next(): T {
        if (index >= size) {
            throw NoSuchElementException()
        }
        return readNextItem(reader, index++)
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }
}
