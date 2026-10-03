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

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.Arrays
import java.util.Queue

/**
 * Utility methods for working with [InputStream]. Based on guava ByteStreams.
 */
object InputStreamUtil {

    private const val BUFFER_SIZE = 8192

    /** Max array length on JVM. */
    private const val MAX_ARRAY_LEN = Integer.MAX_VALUE - 8

    /** Large enough to never need to expand, given the geometric progression of buffer sizes. */
    private const val TO_BYTE_ARRAY_DEQUE_SIZE = 20

    /**
     * Reads all bytes from an input stream into a byte array. Does not close the stream.
     *
     * @param `in` the input stream to read from
     * @return a byte array containing all the bytes from the stream
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    @Throws(IOException::class)
    fun toByteArray(`in`: InputStream): ByteArray {
        var totalLen = 0
        val bufs = ArrayDeque<ByteArray>(TO_BYTE_ARRAY_DEQUE_SIZE)

        // Roughly size to match what has been read already. Some file systems, such as procfs,
        // return 0 as their length. These files are very small, so it's wasteful to allocate an
        // 8KB buffer.
        val initialBufferSize = Math.min(BUFFER_SIZE, Math.max(128, Integer.highestOneBit(totalLen) * 2))
        // Starting with an 8k buffer, double the size of each successive buffer. Smaller buffers
        // quadruple in size until they reach 8k, to minimize the number of small reads for longer
        // streams. Buffers are retained in a deque so that there's no copying between buffers while
        // reading and so all of the bytes in each new allocated buffer are available for reading
        // from the stream.
        var bufSize = initialBufferSize
        while (totalLen < MAX_ARRAY_LEN) {
            val buf = ByteArray(Math.min(bufSize, MAX_ARRAY_LEN - totalLen))
            bufs.add(buf)
            var off = 0
            while (off < buf.size) {
                // always OK to fill buf; its size plus the rest of bufs is never more than
                // MAX_ARRAY_LEN
                val r = `in`.read(buf, off, buf.size - off)
                if (r == -1) {
                    return combineBuffers(bufs, totalLen)
                }
                off += r
                totalLen += r
            }
            bufSize = saturatedMultiply(bufSize, if (bufSize < 4096) 4 else 2)
        }

        // read MAX_ARRAY_LEN bytes without seeing end of stream
        if (`in`.read() == -1) {
            // oh, there's the end of the stream
            return combineBuffers(bufs, MAX_ARRAY_LEN)
        } else {
            throw OutOfMemoryError("input is too large to fit in a byte array")
        }
    }

    private fun saturatedMultiply(a: Int, b: Int): Int {
        val value = a.toLong() * b
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE
        }
        return value.toInt()
    }

    private fun combineBuffers(bufs: Queue<ByteArray>, totalLen: Int): ByteArray {
        if (bufs.isEmpty()) {
            return ByteArray(0)
        }
        var result = bufs.remove()
        if (result.size == totalLen) {
            return result
        }
        var remaining = totalLen - result.size
        result = Arrays.copyOf(result, totalLen)
        while (remaining > 0) {
            val buf = bufs.remove()
            val bytesToCopy = Math.min(remaining, buf.size)
            val resultOffset = totalLen - remaining
            System.arraycopy(buf, 0, result, resultOffset, bytesToCopy)
            remaining -= bytesToCopy
        }
        return result
    }

    /**
     * Discards `n` bytes of data from the input stream. This method will block until the full
     * amount has been skipped. Does not close the stream.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun skipFully(`in`: InputStream, n: Long) {
        val skipped = skipUpTo(`in`, n)
        if (skipped < n) {
            throw EOFException(
                "reached end of stream after skipping $skipped bytes; $n bytes expected"
            )
        }
    }

    /**
     * Discards up to `n` bytes of data from the input stream. This method will block until
     * either the full amount has been skipped or until the end of the stream is reached, whichever
     * happens first. Returns the total number of bytes skipped.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun skipUpTo(`in`: InputStream, n: Long): Long {
        var totalSkipped = 0L
        // A buffer is allocated if skipSafely does not skip any bytes.
        var buf: ByteArray? = null

        while (totalSkipped < n) {
            val remaining = n - totalSkipped
            var skipped = skipSafely(`in`, remaining)

            if (skipped == 0L) {
                // Do a buffered read since skipSafely could return 0 repeatedly, for example if
                // in.available() always returns 0 (the default).
                val skip = Math.min(remaining, BUFFER_SIZE.toLong()).toInt()
                if (buf == null) {
                    // Allocate a buffer bounded by the maximum size that can be requested.
                    buf = ByteArray(skip)
                }
                skipped = `in`.read(buf, 0, skip).toLong()
                if (skipped == -1L) {
                    // Reached EOF
                    break
                }
            }

            totalSkipped += skipped
        }

        return totalSkipped
    }

    /**
     * Attempts to skip up to `n` bytes from the given input stream, but not more than
     * `in.available()` bytes.
     */
    @Throws(IOException::class)
    private fun skipSafely(`in`: InputStream, n: Long): Long {
        val available = `in`.available()
        return if (available == 0) 0L else `in`.skip(Math.min(available.toLong(), n))
    }

    /**
     * Attempts to read enough bytes from the stream to fill the given byte array.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun readFully(`in`: InputStream, b: ByteArray) {
        val read = read(`in`, b, 0, b.size)
        if (read != b.size) {
            throw EOFException(
                "reached end of stream after reading $read bytes; ${b.size} bytes expected"
            )
        }
    }

    /**
     * Reads some bytes from an input stream and stores them into the buffer array `b`.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun read(`in`: InputStream, b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || off + len > b.size) {
            throw IndexOutOfBoundsException("trying to read invalid offset/length range")
        }

        var total = 0
        while (total < len) {
            val result = `in`.read(b, off + total, len - total)
            if (result == -1) {
                break
            }
            total += result
        }
        return total
    }

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either
     * stream.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copy(from: InputStream, to: OutputStream): Long {
        val buf = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val r = from.read(buf)
            if (r == -1) {
                break
            }
            to.write(buf, 0, r)
            total += r
        }
        return total
    }
}
