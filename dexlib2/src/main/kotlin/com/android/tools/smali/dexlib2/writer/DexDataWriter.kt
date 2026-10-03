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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.util.ExceptionWithContext
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream

class DexDataWriter : BufferedOutputStream {
    private var filePosition: Int
    private var tempBuf = ByteArray(8)
    private val zeroBuf = ByteArray(3)

    @JvmOverloads
    constructor(output: OutputStream, filePosition: Int, bufferSize: Int = 256 * 1024) : super(output, bufferSize) {
        this.filePosition = filePosition
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        filePosition++
        super.write(b)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        filePosition += len
        super.write(b, off, len)
    }

    @Throws(IOException::class)
    fun writeLong(value: Long) {
        writeInt(value.toInt())
        writeInt((value shr 32).toInt())
    }

    @Throws(IOException::class)
    fun writeInt(value: Int) {
        writeInt(this, value)
    }

    @Throws(IOException::class)
    fun writeShort(value: Int) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw ExceptionWithContext("Short value out of range: %d", value)
        }
        write(value)
        write(value shr 8)
    }

    @Throws(IOException::class)
    fun writeUshort(value: Int) {
        if (value < 0 || value > 0xFFFF) {
            throw ExceptionWithContext("Unsigned short value out of range: %d", value)
        }
        write(value)
        write(value shr 8)
    }

    @Throws(IOException::class)
    fun writeUbyte(value: Int) {
        if (value < 0 || value > 0xFF) {
            throw ExceptionWithContext("Unsigned byte value out of range: %d", value)
        }
        write(value)
    }

    @Throws(IOException::class)
    fun writeUleb128(value: Int) {
        writeUleb128(this, value)
    }

    @Throws(IOException::class)
    fun writeSleb128(value: Int) {
        writeSleb128(this, value)
    }

    @Throws(IOException::class)
    fun writeEncodedValueHeader(valueType: Int, valueArg: Int) {
        write(valueType or (valueArg shl 5))
    }

    @Throws(IOException::class)
    fun writeEncodedInt(valueType: Int, value: Int) {
        var value = value
        var index = 0
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = value.toByte()
                value = value shr 8
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = value.toByte()
                value = value shr 8
            }
        }
        tempBuf[index++] = value.toByte()
        writeEncodedValueHeader(valueType, index - 1)
        write(tempBuf, 0, index)
    }

    @Throws(IOException::class)
    fun writeEncodedLong(valueType: Int, value: Long) {
        var value = value
        var index = 0
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = value.toByte()
                value = value shr 8
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = value.toByte()
                value = value shr 8
            }
        }
        tempBuf[index++] = value.toByte()
        writeEncodedValueHeader(valueType, index - 1)
        write(tempBuf, 0, index)
    }

    @Throws(IOException::class)
    fun writeEncodedUint(valueType: Int, value: Int) {
        var value = value
        var index = 0
        do {
            tempBuf[index++] = value.toByte()
            value = value ushr 8
        } while (value != 0)
        writeEncodedValueHeader(valueType, index - 1)
        write(tempBuf, 0, index)
    }

    @Throws(IOException::class)
    fun writeEncodedFloat(valueType: Int, value: Float) {
        writeRightZeroExtendedInt(valueType, value.toRawBits())
    }

    @Throws(IOException::class)
    fun writeRightZeroExtendedInt(valueType: Int, value: Int) {
        var value = value
        var index = 3
        do {
            tempBuf[index--] = ((value and 0xFF000000.toInt()) ushr 24).toByte()
            value = value shl 8
        } while (value != 0)

        val firstElement = index + 1
        val encodedLength = 4 - firstElement
        writeEncodedValueHeader(valueType, encodedLength - 1)
        write(tempBuf, firstElement, encodedLength)
    }

    @Throws(IOException::class)
    fun writeEncodedDouble(valueType: Int, value: Double) {
        writeRightZeroExtendedLong(valueType, value.toRawBits())
    }

    @Throws(IOException::class)
    fun writeRightZeroExtendedLong(valueType: Int, value: Long) {
        var value = value
        var index = 7
        do {
            tempBuf[index--] = ((value and -0x100000000000000L) ushr 56).toByte()
            value = value shl 8
        } while (value != 0L)

        val firstElement = index + 1
        val encodedLength = 8 - firstElement
        writeEncodedValueHeader(valueType, encodedLength - 1)
        write(tempBuf, firstElement, encodedLength)
    }

    @Throws(IOException::class)
    fun writeString(string: String) {
        val len = string.length

        // make sure we have enough room in the temporary buffer
        if (tempBuf.size <= string.length * 3) {
            tempBuf = ByteArray(string.length * 3)
        }

        val buf = tempBuf

        var bufPos = 0
        for (i in 0 until len) {
            val c = string[i]
            if ((c != '\u0000') && (c < '\u0080')) {
                buf[bufPos++] = c.code.toByte()
            } else if (c < '\u0800') {
                buf[bufPos++] = (((c.code shr 6) and 0x1f) or 0xc0).toByte()
                buf[bufPos++] = ((c.code and 0x3f) or 0x80).toByte()
            } else {
                buf[bufPos++] = (((c.code shr 12) and 0x0f) or 0xe0).toByte()
                buf[bufPos++] = (((c.code shr 6) and 0x3f) or 0x80).toByte()
                buf[bufPos++] = ((c.code and 0x3f) or 0x80).toByte()
            }
        }
        write(buf, 0, bufPos)
    }

    @Throws(IOException::class)
    fun align() {
        val zeros = (-position) and 3
        if (zeros > 0) {
            write(zeroBuf, 0, zeros)
        }
    }

    val position: Int
        get() = filePosition

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun writeInt(out: OutputStream, value: Int) {
            out.write(value)
            out.write(value shr 8)
            out.write(value shr 16)
            out.write(value shr 24)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun writeUleb128(out: OutputStream, value: Int) {
            var value = value
            while ((value.toLong() and 0xffffffffL) > 0x7f) {
                out.write((value and 0x7f) or 0x80)
                value = value ushr 7
            }
            out.write(value)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun writeSleb128(out: OutputStream, value: Int) {
            var value = value
            if (value >= 0) {
                while (value > 0x3f) {
                    out.write((value and 0x7f) or 0x80)
                    value = value ushr 7
                }
                out.write(value and 0x7f)
            } else {
                while (value < -0x40) {
                    out.write((value and 0x7f) or 0x80)
                    value = value shr 7
                }
                out.write(value and 0x7f)
            }
        }
    }
}
