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

package com.android.tools.smali.dexlib2.writer.io

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Arrays

class MemoryDataStore constructor(initialCapacity: Int = 0) : DexDataStore {
    private var buf: ByteArray = ByteArray(initialCapacity)
    var size: Int = 0
        private set

    val buffer: ByteArray
        get() = buf

    val data: ByteArray
        get() = Arrays.copyOf(buf, size)

    override fun outputAt(offset: Int): OutputStream {
        if (offset < 0) throw IllegalArgumentException()
        return object : OutputStream() {
            private var position = offset

            @Throws(IOException::class)
            override fun write(b: Int) {
                growBufferIfNeeded(position + 1)
                buf[position++] = b.toByte()
            }

            @Throws(IOException::class)
            override fun write(b: ByteArray) {
                growBufferIfNeeded(position + b.size)
                System.arraycopy(b, 0, buf, position, b.size)
                position += b.size
            }

            @Throws(IOException::class)
            override fun write(b: ByteArray, off: Int, len: Int) {
                growBufferIfNeeded(position + len)
                System.arraycopy(b, off, buf, position, len)
                position += len
            }
        }
    }

    private fun growBufferIfNeeded(minSize: Int) {
        if (minSize > size) {
            if (minSize > buf.size) {
                val newSize = getNewBufferSize(buf.size, minSize)
                if (newSize < minSize) throw IndexOutOfBoundsException()
                buf = Arrays.copyOf(buf, newSize)
            }
            size = minSize
        }
    }

    protected fun getNewBufferSize(currentSize: Int, newMinSize: Int): Int {
        val MIN_GROWTH_STEP = 256 * 1024
        return maxOf(newMinSize + (newMinSize shr 2), currentSize + MIN_GROWTH_STEP)
    }

    override fun readAt(offset: Int): InputStream {
        if (offset < 0) throw IllegalArgumentException()
        return object : InputStream() {
            private var position = offset
            private var mark = offset

            @Throws(IOException::class)
            override fun read(): Int {
                if (position >= size) {
                    return -1
                }
                return buf[position++].toInt()
            }

            @Throws(IOException::class)
            override fun read(b: ByteArray): Int {
                val readLength = minOf(b.size, size - position)
                if (readLength <= 0) {
                    if (position >= size) {
                        return -1
                    }
                    return 0
                }
                System.arraycopy(buf, position, b, 0, readLength)
                position += readLength
                return readLength
            }

            @Throws(IOException::class)
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val readLength = minOf(len, size - position)
                if (readLength <= 0) {
                    if (position >= size) {
                        return -1
                    }
                    return 0
                }
                System.arraycopy(buf, position, b, off, readLength)
                position += readLength
                return readLength
            }

            @Throws(IOException::class)
            override fun skip(n: Long): Long {
                val skipLength = maxOf(0, minOf(n, (size - position).toLong())).toInt()
                position += skipLength
                return skipLength.toLong()
            }

            @Throws(IOException::class)
            override fun available(): Int {
                return maxOf(0, size - position)
            }

            override fun mark(i: Int) {
                mark = position
            }

            @Throws(IOException::class)
            override fun reset() {
                position = mark
            }

            override fun markSupported(): Boolean {
                return true
            }
        }
    }

    @Throws(IOException::class)
    override fun close() {
        // no-op
    }
}
