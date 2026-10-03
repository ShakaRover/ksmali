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
import java.io.OutputStream

/**
 * A deferred output stream that is stored in memory
 */
class MemoryDeferredOutputStream constructor(bufferSize: Int = DEFAULT_BUFFER_SIZE) :
    DeferredOutputStream() {
    private val buffers = ArrayList<ByteArray>()
    private var currentBuffer: ByteArray = ByteArray(bufferSize)
    private var currentPosition = 0

    @Throws(IOException::class)
    override fun writeTo(dest: OutputStream) {
        for (buffer in buffers) {
            dest.write(buffer)
        }
        if (currentPosition > 0) {
            dest.write(currentBuffer, 0, currentPosition)
        }
        buffers.clear()
        currentPosition = 0
    }

    @Throws(IOException::class)
    override fun write(i: Int) {
        if (remaining() == 0) {
            buffers.add(currentBuffer)
            currentBuffer = ByteArray(currentBuffer.size)
            currentPosition = 0
        }
        currentBuffer[currentPosition++] = i.toByte()
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        write(bytes, 0, bytes.size)
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        var remaining = remaining()
        var written = 0
        while (length - written > 0) {
            val toWrite = minOf(remaining, (length - written))
            System.arraycopy(bytes, offset + written, currentBuffer, currentPosition, toWrite)
            written += toWrite
            currentPosition += toWrite

            remaining = remaining()
            if (remaining == 0) {
                buffers.add(currentBuffer)
                currentBuffer = ByteArray(currentBuffer.size)
                currentPosition = 0
                remaining = currentBuffer.size
            }
        }
    }

    private fun remaining(): Int {
        return currentBuffer.size - currentPosition
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 16 * 1024

        val factory: DeferredOutputStreamFactory
            get() = getFactory(DEFAULT_BUFFER_SIZE)

        fun getFactory(bufferSize: Int): DeferredOutputStreamFactory {
            return object : DeferredOutputStreamFactory {
                override fun makeDeferredOutputStream(): DeferredOutputStream {
                    return MemoryDeferredOutputStream(bufferSize)
                }
            }
        }
    }
}
