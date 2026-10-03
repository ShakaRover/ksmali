/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.util

import java.io.IOException
import java.io.Writer

open class IndentingWriter(protected val writer: Writer) : Writer() {
    protected val buffer = CharArray(24)
    protected var indentLevel = 0
    private var beginningOfLine = true

    @Throws(IOException::class)
    protected open fun writeIndent() {
        for (i in 0 until indentLevel) {
            writer.write(' '.code)
        }
    }

    @Throws(IOException::class)
    override fun write(chr: Int) {
        if (chr == '\n'.code) {
            writer.write(newLine)
            beginningOfLine = true
        } else {
            if (beginningOfLine) {
                writeIndent()
            }
            beginningOfLine = false
            writer.write(chr)
        }
    }

    /** Writes out a block of text that contains no newlines */
    @Throws(IOException::class)
    private fun writeLine(chars: CharArray, start: Int, len: Int) {
        if (beginningOfLine && len > 0) {
            writeIndent()
            beginningOfLine = false
        }
        writer.write(chars, start, len)
    }

    /** Writes out a block of text that contains no newlines */
    @Throws(IOException::class)
    private fun writeLine(str: String, start: Int, len: Int) {
        if (beginningOfLine && len > 0) {
            writeIndent()
            beginningOfLine = false
        }
        writer.write(str, start, len)
    }

    @Throws(IOException::class)
    override fun write(chars: CharArray) {
        write(chars, 0, chars.size)
    }

    @Throws(IOException::class)
    override fun write(chars: CharArray, start: Int, len: Int) {
        val end = start + len
        var pos = start
        var start = start
        while (pos < end) {
            if (chars[pos] == '\n') {
                writeLine(chars, start, pos - start)

                writer.write(newLine)
                beginningOfLine = true
                pos++
                start = pos
            } else {
                pos++
            }
        }
        writeLine(chars, start, pos - start)
    }

    @Throws(IOException::class)
    override fun write(s: String) {
        write(s, 0, s.length)
    }

    @Throws(IOException::class)
    override fun write(str: String, start: Int, len: Int) {
        val end = start + len
        var start = start
        while (start < end) {
            val pos = str.indexOf('\n', start)
            if (pos == -1 || pos >= end) {
                writeLine(str, start, end - start)
                return
            } else {
                writeLine(str, start, pos - start)
                writer.write(newLine)
                beginningOfLine = true
                start = pos + 1
            }
        }
    }

    @Throws(IOException::class)
    override fun append(charSequence: CharSequence?): Writer {
        write(charSequence.toString())
        return this
    }

    @Throws(IOException::class)
    override fun append(charSequence: CharSequence?, start: Int, len: Int): Writer {
        write(charSequence!!.subSequence(start, len).toString())
        return this
    }

    @Throws(IOException::class)
    override fun append(c: Char): Writer {
        write(c.code)
        return this
    }

    @Throws(IOException::class)
    override fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        writer.close()
    }

    fun indent(indentAmount: Int) {
        indentLevel += indentAmount
        if (indentLevel < 0) {
            indentLevel = 0
        }
    }

    fun deindent(indentAmount: Int) {
        indentLevel -= indentAmount
        if (indentLevel < 0) {
            indentLevel = 0
        }
    }

    companion object {
        private val newLine = System.getProperty("line.separator")
    }
}
