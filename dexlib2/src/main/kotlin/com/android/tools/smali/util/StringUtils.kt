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

package com.android.tools.smali.util

import java.io.IOException
import java.io.Writer

object StringUtils {

    /**
     * @deprecated Use @see
     * com.android.tools.smali.baksmali.formatter.BaksmaliWriter}#writeCharEncodedValue()
     */
    @Deprecated("Use BaksmaliWriter#writeCharEncodedValue()")
    @JvmStatic
    @Throws(IOException::class)
    fun writeEscapedChar(writer: Writer, c: Char) {
        var c = c
        if ((c >= ' ') && (c < 0x7f.toChar())) {
            if ((c == '\'') || (c == '\"') || (c == '\\')) {
                writer.write('\\'.code)
            }
            writer.write(c.code)
            return
        } else if (c <= 0x7f.toChar()) {
            when (c) {
                '\n' -> {
                    writer.write("\\n")
                    return
                }
                '\r' -> {
                    writer.write("\\r")
                    return
                }
                '\t' -> {
                    writer.write("\\t")
                    return
                }
            }
        }

        writer.write("\\u")
        writer.write(Character.forDigit(c.code shr 12, 16).code)
        writer.write(Character.forDigit((c.code shr 8) and 0x0f, 16).code)
        writer.write(Character.forDigit((c.code shr 4) and 0x0f, 16).code)
        writer.write(Character.forDigit(c.code and 0x0f, 16).code)
    }

    /**
     * @deprecated Use [DexFormattedWriter.writeQuotedString]
     */
    @Deprecated("Use DexFormattedWriter#writeQuotedString(CharSequence)")
    @JvmStatic
    @Throws(IOException::class)
    fun writeEscapedString(writer: Writer, value: String) {
        for (i in value.indices) {
            val c = value[i]

            if ((c >= ' ') && (c < 0x7f.toChar())) {
                if ((c == '\'') || (c == '\"') || (c == '\\')) {
                    writer.write('\\'.code)
                }
                writer.write(c.code)
                continue
            } else if (c <= 0x7f.toChar()) {
                when (c) {
                    '\n' -> {
                        writer.write("\\n")
                        continue
                    }
                    '\r' -> {
                        writer.write("\\r")
                        continue
                    }
                    '\t' -> {
                        writer.write("\\t")
                        continue
                    }
                }
            }

            writer.write("\\u")
            writer.write(Character.forDigit(c.code shr 12, 16).code)
            writer.write(Character.forDigit((c.code shr 8) and 0x0f, 16).code)
            writer.write(Character.forDigit((c.code shr 4) and 0x0f, 16).code)
            writer.write(Character.forDigit(c.code and 0x0f, 16).code)
        }
    }

    @JvmStatic
    fun escapeString(value: String): String {
        val len = value.length
        val sb = StringBuilder(len * 3 / 2)

        for (i in 0 until len) {
            val c = value[i]

            if ((c >= ' ') && (c < 0x7f.toChar())) {
                if ((c == '\'') || (c == '\"') || (c == '\\')) {
                    sb.append('\\')
                }
                sb.append(c)
                continue
            } else if (c <= 0x7f.toChar()) {
                when (c) {
                    '\n' -> {
                        sb.append("\\n")
                        continue
                    }
                    '\r' -> {
                        sb.append("\\r")
                        continue
                    }
                    '\t' -> {
                        sb.append("\\t")
                        continue
                    }
                }
            }

            sb.append("\\u")
            sb.append(Character.forDigit(c.code shr 12, 16))
            sb.append(Character.forDigit((c.code shr 8) and 0x0f, 16))
            sb.append(Character.forDigit((c.code shr 4) and 0x0f, 16))
            sb.append(Character.forDigit(c.code and 0x0f, 16))
        }

        return sb.toString()
    }

    @JvmStatic
    fun join(parts: Collection<*>, separator: String): String {
        val builder = StringBuilder()
        val it = parts.iterator()
        if (it.hasNext()) {
            builder.append(it.next())
        }
        while (it.hasNext()) {
            builder.append(separator)
            builder.append(it.next())
        }
        return builder.toString()
    }

    // Base on the repeat method in guava Strings, of the same signature.
    @JvmStatic
    fun repeat(string: String?, count: Int): String {
        if (string == null) {
            throw NullPointerException("string == null")
        }

        if (count <= 1) {
            if (count < 0) {
                throw IllegalArgumentException("invalid count: $count")
            }
            return if (count == 0) "" else string
        }

        val len = string.length
        val longSize = len.toLong() * count.toLong()
        val size = longSize.toInt()
        if (size.toLong() != longSize) {
            throw ArrayIndexOutOfBoundsException("Required array size too large: $longSize")
        }

        val array = CharArray(size)
        string.toCharArray(array, 0, 0, len)
        var n = len
        while (n < size - n) {
            System.arraycopy(array, 0, array, n, n)
            n = n shl 1
        }
        System.arraycopy(array, 0, array, n, size - n)
        return String(array)
    }
}
