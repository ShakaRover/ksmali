/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package com.android.tools.smali.util

/**
 * Constants of type `CONSTANT_Utf8_info`.
 */
object Utf8Utils {
    /**
     * Converts a string into its Java-style UTF-8 form. Java-style UTF-8
     * differs from normal UTF-8 in the handling of character '\0' and
     * surrogate pairs.
     *
     * @param string non-null; the string to convert
     * @return non-null; the UTF-8 bytes for it
     */
    @JvmStatic
    fun stringToUtf8Bytes(string: String): ByteArray {
        val len = string.length
        val bytes = ByteArray(len * 3) // Avoid having to reallocate.
        var outAt = 0

        for (i in 0 until len) {
            val c = string[i]
            if ((c != '\u0000') && (c < 0x80.toChar())) {
                bytes[outAt] = c.code.toByte()
                outAt++
            } else if (c < 0x800.toChar()) {
                bytes[outAt] = (((c.code shr 6) and 0x1f) or 0xc0).toByte()
                bytes[outAt + 1] = ((c.code and 0x3f) or 0x80).toByte()
                outAt += 2
            } else {
                bytes[outAt] = (((c.code shr 12) and 0x0f) or 0xe0).toByte()
                bytes[outAt + 1] = (((c.code shr 6) and 0x3f) or 0x80).toByte()
                bytes[outAt + 2] = ((c.code and 0x3f) or 0x80).toByte()
                outAt += 3
            }
        }

        val result = ByteArray(outAt)
        System.arraycopy(bytes, 0, result, 0, outAt)
        return result
    }

    private val localBuffer: ThreadLocal<CharArray> = object : ThreadLocal<CharArray>() {
        override fun initialValue(): CharArray {
            // A reasonably sized initial value
            return CharArray(256)
        }
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param length the length of the utf8 string to convert, not including any null-terminator
     * @return non-null; the converted string
     */
    @JvmStatic
    fun utf8BytesToString(bytes: ByteArray, start: Int, length: Int): String {
        var chars = localBuffer.get()
        if (chars == null || chars.size < length) {
            chars = CharArray(length)
            localBuffer.set(chars)
        }
        var outAt = 0

        var at = start
        var remaining = length
        while (remaining > 0) {
            val v0 = bytes[at].toInt() and 0xFF
            val out: Char
            when (v0 shr 4) {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> {
                    // 0XXXXXXX -- single-byte encoding
                    remaining--
                    if (v0 == 0) {
                        // A single zero byte is illegal.
                        return throwBadUtf8(v0, at)
                    }
                    out = v0.toChar()
                    at++
                }
                0x0c, 0x0d -> {
                    // 110XXXXX -- two-byte encoding
                    remaining -= 2
                    if (remaining < 0) {
                        return throwBadUtf8(v0, at)
                    }
                    val v1 = bytes[at + 1].toInt() and 0xFF
                    if ((v1 and 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1)
                    }
                    val value = ((v0 and 0x1f) shl 6) or (v1 and 0x3f)
                    if ((value != 0) && (value < 0x80)) {
                        // This should have been represented with one-byte encoding.
                        return throwBadUtf8(v1, at + 1)
                    }
                    out = value.toChar()
                    at += 2
                }
                0x0e -> {
                    // 1110XXXX -- three-byte encoding
                    remaining -= 3
                    if (remaining < 0) {
                        return throwBadUtf8(v0, at)
                    }
                    val v1 = bytes[at + 1].toInt() and 0xFF
                    if ((v1 and 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1)
                    }
                    val v2 = bytes[at + 2].toInt() and 0xFF
                    if ((v2 and 0xc0) != 0x80) {
                        return throwBadUtf8(v2, at + 2)
                    }
                    val value = ((v0 and 0x0f) shl 12) or ((v1 and 0x3f) shl 6) or (v2 and 0x3f)
                    if (value < 0x800) {
                        // This should have been represented with one- or two-byte encoding.
                        return throwBadUtf8(v2, at + 2)
                    }
                    out = value.toChar()
                    at += 3
                }
                else -> {
                    // 10XXXXXX, 1111XXXX -- illegal
                    return throwBadUtf8(v0, at)
                }
            }
            chars[outAt] = out
            outAt++
        }

        return String(chars, 0, outAt)
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param utf16Length the number of utf16 characters in the string to decode
     * @return non-null; the converted string
     */
    @JvmStatic
    fun utf8BytesWithUtf16LengthToString(bytes: ByteArray, start: Int, utf16Length: Int): String {
        return utf8BytesWithUtf16LengthToString(bytes, start, utf16Length, null)
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param utf16Length the number of utf16 characters in the string to decode
     * @param readLength If non-null, the first element will contain the number of bytes read
     * @return non-null; the converted string
     */
    @JvmStatic
    fun utf8BytesWithUtf16LengthToString(
        bytes: ByteArray,
        start: Int,
        utf16Length: Int,
        readLength: IntArray?
    ): String {
        var chars = localBuffer.get()
        if (chars == null || chars.size < utf16Length) {
            chars = CharArray(utf16Length)
            localBuffer.set(chars)
        }
        var outAt = 0

        var at = start
        var remaining = utf16Length
        while (remaining > 0) {
            val v0 = bytes[at].toInt() and 0xFF
            val out: Char
            when (v0 shr 4) {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> {
                    // 0XXXXXXX -- single-byte encoding
                    if (v0 == 0) {
                        // A single zero byte is illegal.
                        return throwBadUtf8(v0, at)
                    }
                    out = v0.toChar()
                    at++
                }
                0x0c, 0x0d -> {
                    // 110XXXXX -- two-byte encoding
                    val v1 = bytes[at + 1].toInt() and 0xFF
                    if ((v1 and 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1)
                    }
                    val value = ((v0 and 0x1f) shl 6) or (v1 and 0x3f)
                    if ((value != 0) && (value < 0x80)) {
                        // This should have been represented with one-byte encoding.
                        return throwBadUtf8(v1, at + 1)
                    }
                    out = value.toChar()
                    at += 2
                }
                0x0e -> {
                    // 1110XXXX -- three-byte encoding
                    val v1 = bytes[at + 1].toInt() and 0xFF
                    if ((v1 and 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1)
                    }
                    val v2 = bytes[at + 2].toInt() and 0xFF
                    if ((v2 and 0xc0) != 0x80) {
                        return throwBadUtf8(v2, at + 2)
                    }
                    val value = ((v0 and 0x0f) shl 12) or ((v1 and 0x3f) shl 6) or (v2 and 0x3f)
                    if (value < 0x800) {
                        // This should have been represented with one- or two-byte encoding.
                        return throwBadUtf8(v2, at + 2)
                    }
                    out = value.toChar()
                    at += 3
                }
                else -> {
                    // 10XXXXXX, 1111XXXX -- illegal
                    return throwBadUtf8(v0, at)
                }
            }
            chars[outAt] = out
            outAt++
            remaining--
        }

        if (readLength != null && readLength.size > 0) {
            readLength[0] = at - start
        }
        return String(chars, 0, outAt)
    }

    /**
     * Helper for [utf8BytesToString], which throws the right exception for a bogus utf-8 byte.
     *
     * @param value the byte value
     * @param offset the file offset
     * @return never
     * @throws IllegalArgumentException always thrown
     */
    private fun throwBadUtf8(value: Int, offset: Int): String {
        throw IllegalArgumentException(
            "bad utf-8 byte " + Hex.u1(value) + " at offset " + Hex.u4(offset)
        )
    }
}
