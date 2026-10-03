/*
 * Copyright 2016, Google LLC
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

import java.io.FilterWriter
import java.io.IOException
import java.io.Writer

class WrappedIndentingWriter(
    out: Writer,
    private val maxIndent: Int,
    private val maxWidth: Int
) : FilterWriter(out) {

    private var currentIndent = 0
    private val line = StringBuilder()

    @Throws(IOException::class)
    private fun writeIndent() {
        for (i in 0 until getIndent()) {
            write(' '.code)
        }
    }

    private fun getIndent(): Int {
        if (currentIndent < 0) {
            return 0
        }
        if (currentIndent > maxIndent) {
            return maxIndent
        }
        return currentIndent
    }

    fun indent(indent: Int) {
        currentIndent += indent
    }

    fun deindent(indent: Int) {
        currentIndent -= indent
    }

    @Throws(IOException::class)
    private fun wrapLine() {
        val wrapped = IteratorUtils.toList(
            StringWrapper.wrapStringOnBreaks(line.toString(), maxWidth)
        )
        out.write(wrapped[0], 0, wrapped[0].length)
        out.write('\n'.code)

        line.replace(0, line.length, "")
        writeIndent()
        for (i in 1 until wrapped.size) {
            if (i > 1) {
                write('\n'.code)
            }
            write(wrapped[i])
        }
    }

    @Throws(IOException::class)
    override fun write(c: Int) {
        if (c == '\n'.code) {
            out.write(line.toString())
            out.write(c)
            line.replace(0, line.length, "")
            writeIndent()
        } else {
            line.append(c.toChar())
            if (line.length > maxWidth) {
                wrapLine()
            }
        }
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        for (i in 0 until len) {
            write(cbuf[i + off].code)
        }
    }

    @Throws(IOException::class)
    override fun write(str: String, off: Int, len: Int) {
        for (i in 0 until len) {
            write(str[off + i].code)
        }
    }

    @Throws(IOException::class)
    override fun flush() {
        out.write(line.toString())
        line.replace(0, line.length, "")
    }
}
