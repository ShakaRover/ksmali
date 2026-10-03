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

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.value.DexBackedEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue

abstract class EncodedArrayItemIterator {
    abstract val nextOrNull: EncodedValue?

    abstract fun skipNext()

    abstract val readerOffset: Int

    abstract val itemCount: Int

    private class EncodedArrayItemIteratorImpl(
        dexFile: DexBackedDexFile,
        offset: Int
    ) : EncodedArrayItemIterator() {
        private val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(offset)
        private val dexFile: DexBackedDexFile = dexFile
        private val size: Int = reader.readSmallUleb128()
        private var index = 0

        override val nextOrNull: EncodedValue? get() {
            if (index < size) {
                index++
                return DexBackedEncodedValue.readFrom(dexFile, reader)
            }
            return null
        }

        override fun skipNext() {
            if (index < size) {
                index++
                DexBackedEncodedValue.skipFrom(reader)
            }
        }

        override val readerOffset: Int get() {
            return reader.offset
        }

        override val itemCount: Int get() {
            return size
        }
    }

    companion object {
        val EMPTY: EncodedArrayItemIterator = object : EncodedArrayItemIterator() {
            override val nextOrNull: EncodedValue? get() = null

            override fun skipNext() {
            }

            override val readerOffset: Int get() = 0

            override val itemCount: Int get() = 0
        }

        fun newOrEmpty(dexFile: DexBackedDexFile, offset: Int): EncodedArrayItemIterator {
            if (offset == 0) {
                return EMPTY
            }
            return EncodedArrayItemIteratorImpl(dexFile, offset)
        }
    }
}
