/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

@file:JvmName("LiteralTools")

package com.android.tools.smali.smali

import java.util.regex.Pattern

private val specialFloatRegex = Pattern.compile("((-)?infinityf)|(nanf)", Pattern.CASE_INSENSITIVE)
private val specialDoubleRegex = Pattern.compile("((-)?infinityd?)|(nand?)", Pattern.CASE_INSENSITIVE)

fun parseByte(byteLiteral: String?): Byte {
    if (byteLiteral == null) {
        throw NumberFormatException("string is null")
    }
    if (byteLiteral.isEmpty()) {
        throw NumberFormatException("string is blank")
    }

    val byteChars: CharArray
    if (byteLiteral.uppercase().endsWith("T")) {
        byteChars = byteLiteral.substring(0, byteLiteral.length - 1).toCharArray()
    } else {
        byteChars = byteLiteral.toCharArray()
    }

    var position = 0
    var radix = 10
    var negative = false
    if (byteChars[position] == '-') {
        position++
        negative = true
    }

    if (byteChars[position] == '0') {
        position++
        if (position == byteChars.size) {
            return 0
        } else if (byteChars[position] == 'x' || byteChars[position] == 'X') {
            radix = 16
            position++
        } else if (Character.digit(byteChars[position], 8) >= 0) {
            radix = 8
        }
    }

    var result: Byte = 0
    var shiftedResult: Byte
    var digit: Int
    val maxValue: Byte = (Byte.MAX_VALUE / (radix / 2)).toByte()

    while (position < byteChars.size) {
        digit = Character.digit(byteChars[position], radix)
        if (digit < 0) {
            throw NumberFormatException("The string contains invalid an digit - '" + byteChars[position] + "'")
        }
        shiftedResult = (result * radix).toByte()
        if (result > maxValue) {
            throw NumberFormatException("$byteLiteral cannot fit into a byte")
        }
        if (shiftedResult < 0 && shiftedResult.toInt() >= -digit) {
            throw NumberFormatException("$byteLiteral cannot fit into a byte")
        }
        result = (shiftedResult + digit).toByte()
        position++
    }

    if (negative) {
        // allow -0x80, which is = 0x80
        if (result == Byte.MIN_VALUE) {
            return result
        } else if (result < 0) {
            throw NumberFormatException("$byteLiteral cannot fit into a byte")
        }
        return (result * -1).toByte()
    } else {
        return result
    }
}

fun parseShort(shortLiteral: String?): Short {
    if (shortLiteral == null) {
        throw NumberFormatException("string is null")
    }
    if (shortLiteral.isEmpty()) {
        throw NumberFormatException("string is blank")
    }

    val shortChars: CharArray
    if (shortLiteral.uppercase().endsWith("S")) {
        shortChars = shortLiteral.substring(0, shortLiteral.length - 1).toCharArray()
    } else {
        shortChars = shortLiteral.toCharArray()
    }

    var position = 0
    var radix = 10
    var negative = false
    if (shortChars[position] == '-') {
        position++
        negative = true
    }

    if (shortChars[position] == '0') {
        position++
        if (position == shortChars.size) {
            return 0
        } else if (shortChars[position] == 'x' || shortChars[position] == 'X') {
            radix = 16
            position++
        } else if (Character.digit(shortChars[position], 8) >= 0) {
            radix = 8
        }
    }

    var result: Short = 0
    var shiftedResult: Short
    var digit: Int
    val maxValue: Short = (Short.MAX_VALUE / (radix / 2)).toShort()

    while (position < shortChars.size) {
        digit = Character.digit(shortChars[position], radix)
        if (digit < 0) {
            throw NumberFormatException("The string contains invalid an digit - '" + shortChars[position] + "'")
        }
        shiftedResult = (result * radix).toShort()
        if (result > maxValue) {
            throw NumberFormatException("$shortLiteral cannot fit into a short")
        }
        if (shiftedResult < 0 && shiftedResult.toInt() >= -digit) {
            throw NumberFormatException("$shortLiteral cannot fit into a short")
        }
        result = (shiftedResult + digit).toShort()
        position++
    }

    if (negative) {
        // allow -0x8000, which is = 0x8000
        if (result == Short.MIN_VALUE) {
            return result
        } else if (result < 0) {
            throw NumberFormatException("$shortLiteral cannot fit into a short")
        }
        return (result * -1).toShort()
    } else {
        return result
    }
}

fun parseInt(intLiteral: String?): Int {
    if (intLiteral == null) {
        throw NumberFormatException("string is null")
    }
    if (intLiteral.isEmpty()) {
        throw NumberFormatException("string is blank")
    }

    val intChars = intLiteral.toCharArray()
    var position = 0
    var radix = 10
    var negative = false
    if (intChars[position] == '-') {
        position++
        negative = true
    }

    if (intChars[position] == '0') {
        position++
        if (position == intChars.size) {
            return 0
        } else if (intChars[position] == 'x' || intChars[position] == 'X') {
            radix = 16
            position++
        } else if (Character.digit(intChars[position], 8) >= 0) {
            radix = 8
        }
    }

    var result = 0
    var shiftedResult: Int
    var digit: Int
    val maxValue = Int.MAX_VALUE / (radix / 2)

    while (position < intChars.size) {
        digit = Character.digit(intChars[position], radix)
        if (digit < 0) {
            throw NumberFormatException("The string contains an invalid digit - '" + intChars[position] + "'")
        }
        shiftedResult = result * radix
        if (result > maxValue) {
            throw NumberFormatException("$intLiteral cannot fit into an int")
        }
        if (shiftedResult < 0 && shiftedResult >= -digit) {
            throw NumberFormatException("$intLiteral cannot fit into an int")
        }
        result = shiftedResult + digit
        position++
    }

    if (negative) {
        // allow -0x80000000, which is = 0x80000000
        if (result == Int.MIN_VALUE) {
            return result
        } else if (result < 0) {
            throw NumberFormatException("$intLiteral cannot fit into an int")
        }
        return result * -1
    } else {
        return result
    }
}

fun parseLong(longLiteral: String?): Long {
    if (longLiteral == null) {
        throw NumberFormatException("string is null")
    }
    if (longLiteral.isEmpty()) {
        throw NumberFormatException("string is blank")
    }

    val longChars: CharArray
    if (longLiteral.uppercase().endsWith("L")) {
        longChars = longLiteral.substring(0, longLiteral.length - 1).toCharArray()
    } else {
        longChars = longLiteral.toCharArray()
    }

    var position = 0
    var radix = 10
    var negative = false
    if (longChars[position] == '-') {
        position++
        negative = true
    }

    if (longChars[position] == '0') {
        position++
        if (position == longChars.size) {
            return 0
        } else if (longChars[position] == 'x' || longChars[position] == 'X') {
            radix = 16
            position++
        } else if (Character.digit(longChars[position], 8) >= 0) {
            radix = 8
        }
    }

    var result = 0L
    var shiftedResult: Long
    var digit: Int
    val maxValue = Long.MAX_VALUE / (radix / 2)

    while (position < longChars.size) {
        digit = Character.digit(longChars[position], radix)
        if (digit < 0) {
            throw NumberFormatException("The string contains an invalid digit - '" + longChars[position] + "'")
        }
        shiftedResult = result * radix
        if (result > maxValue) {
            throw NumberFormatException("$longLiteral cannot fit into a long")
        }
        if (shiftedResult < 0 && shiftedResult >= -digit) {
            throw NumberFormatException("$longLiteral cannot fit into a long")
        }
        result = shiftedResult + digit
        position++
    }

    if (negative) {
        // allow -0x8000000000000000, which is = 0x8000000000000000
        if (result == Long.MIN_VALUE) {
            return result
        } else if (result < 0) {
            throw NumberFormatException("$longLiteral cannot fit into a long")
        }
        return result * -1
    } else {
        return result
    }
}

fun parseFloat(floatString: String): Float {
    val m = specialFloatRegex.matcher(floatString)
    if (m.matches()) {
        // got an infinity
        if (m.start(1) != -1) {
            return if (m.start(2) != -1) {
                Float.NEGATIVE_INFINITY
            } else {
                Float.POSITIVE_INFINITY
            }
        } else {
            return Float.NaN
        }
    }
    return java.lang.Float.parseFloat(floatString)
}

fun parseDouble(doubleString: String): Double {
    val m = specialDoubleRegex.matcher(doubleString)
    if (m.matches()) {
        // got an infinity
        if (m.start(1) != -1) {
            return if (m.start(2) != -1) {
                Double.NEGATIVE_INFINITY
            } else {
                Double.POSITIVE_INFINITY
            }
        } else {
            return Double.NaN
        }
    }
    return java.lang.Double.parseDouble(doubleString)
}

fun longToBytes(value: Long): ByteArray {
    val bytes = ByteArray(8)

    var v = value
    var i = 0
    while (v != 0L) {
        bytes[i] = v.toByte()
        v = v ushr 8
        i++
    }
    return bytes
}

fun intToBytes(value: Int): ByteArray {
    val bytes = ByteArray(4)

    var v = value
    var i = 0
    while (v != 0) {
        bytes[i] = v.toByte()
        v = v ushr 8
        i++
    }
    return bytes
}

fun shortToBytes(value: Short): ByteArray {
    val bytes = ByteArray(2)

    bytes[0] = value.toByte()
    bytes[1] = (value.toInt() ushr 8).toByte()
    return bytes
}

fun floatToBytes(value: Float): ByteArray = intToBytes(value.toRawBits())

fun doubleToBytes(value: Double): ByteArray = longToBytes(value.toRawBits())

fun charToBytes(value: Char): ByteArray = shortToBytes(value.code.toShort())

fun boolToBytes(value: Boolean): ByteArray {
    return if (value) {
        byteArrayOf(0x01)
    } else {
        byteArrayOf(0x00)
    }
}

fun checkInt(value: Long) {
    if (value > 0xFFFFFFFFL || value < -0x80000000L) {
        throw NumberFormatException(value.toString() + " cannot fit into an int")
    }
}

fun checkShort(value: Long) {
    if (value > 0xFFFFL || value < -0x8000L) {
        throw NumberFormatException(value.toString() + " cannot fit into a short")
    }
}

fun checkByte(value: Long) {
    if (value > 0xFFL || value < -0x80L) {
        throw NumberFormatException(value.toString() + " cannot fit into a byte")
    }
}

fun checkNibble(value: Long) {
    if (value > 0x0FL || value < -0x08L) {
        throw NumberFormatException(value.toString() + " cannot fit into a nibble")
    }
}
