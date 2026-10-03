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

import com.android.tools.smali.util.InputStreamUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * A deferred output stream that uses a file as its backing store, with a in-memory intermediate buffer.
 */
class FileDeferredOutputStream : DeferredOutputStream {
    private val backingFile: File
    private val output: NakedBufferedOutputStream
    private var writtenBytes = 0

    @Throws(FileNotFoundException::class)
    constructor(backingFile: File) : this(backingFile, DEFAULT_BUFFER_SIZE)

    @Throws(FileNotFoundException::class)
    constructor(backingFile: File, bufferSize: Int) {
        this.backingFile = backingFile
        output = NakedBufferedOutputStream(FileOutputStream(backingFile), bufferSize)
    }

    @Throws(IOException::class)
    override fun writeTo(dest: OutputStream) {
        val outBuf = output.getBuffer()
        val count = output.getCount()
        output.resetBuffer()
        output.close()

        // did we actually write something out to disk?
        if (count != writtenBytes) {
            val fis = FileInputStream(backingFile)
            InputStreamUtil.copy(fis, dest)
            backingFile.delete()
        }

        dest.write(outBuf, 0, count)
    }

    @Throws(IOException::class)
    override fun write(i: Int) {
        output.write(i)
        writtenBytes++
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        output.write(bytes)
        writtenBytes += bytes.size
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, off: Int, len: Int) {
        output.write(bytes, off, len)
        writtenBytes += len
    }

    @Throws(IOException::class)
    override fun flush() {
        output.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        output.close()
    }

    private class NakedBufferedOutputStream : BufferedOutputStream {
        constructor(outputStream: OutputStream) : super(outputStream)

        constructor(outputStream: OutputStream, size: Int) : super(outputStream, size)

        fun getCount(): Int {
            return count
        }

        fun resetBuffer() {
            count = 0
        }

        fun getBuffer(): ByteArray {
            return buf
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 4 * 1024

        fun getFactory(containingDirectory: File?): DeferredOutputStreamFactory {
            return getFactory(containingDirectory, DEFAULT_BUFFER_SIZE)
        }

        fun getFactory(containingDirectory: File?, bufferSize: Int): DeferredOutputStreamFactory {
            return object : DeferredOutputStreamFactory {
                @Throws(IOException::class)
                override fun makeDeferredOutputStream(): DeferredOutputStream {
                    val tempFile = File.createTempFile("dexlibtmp", null, containingDirectory)
                    return FileDeferredOutputStream(tempFile, bufferSize)
                }
            }
        }
    }
}
