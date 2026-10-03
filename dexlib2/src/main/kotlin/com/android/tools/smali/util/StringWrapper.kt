/*
 * Copyright 2018, Google LLC
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

import java.io.PrintStream
import java.text.BreakIterator

object StringWrapper {
    /**
     * Splits the given string into lines of maximum width maxWidth. The splitting is done using the current locale's
     * rules for splitting lines.
     *
     * @param string The string to split
     * @param maxWidth The maximum length of any line
     * @return An iterable of Strings containing the wrapped lines
     */
    @JvmStatic
    fun wrapStringOnBreaks(string: String, maxWidth: Int): Iterable<String> {
        // TODO: should we strip any trailing newlines?
        val breakIterator = BreakIterator.getLineInstance()
        breakIterator.setText(string)

        return object : Iterable<String> {
            override fun iterator(): MutableIterator<String> {
                return object : MutableIterator<String> {
                    private var currentLineStart = 0
                    private var nextLineSet = false
                    private var nextLine: String? = null

                    override fun hasNext(): Boolean {
                        if (!nextLineSet) {
                            calculateNext()
                        }
                        return nextLine != null
                    }

                    private fun calculateNext() {
                        var lineEnd = currentLineStart
                        while (true) {
                            lineEnd = breakIterator.following(lineEnd)
                            if (lineEnd == BreakIterator.DONE) {
                                lineEnd = breakIterator.last()
                                if (lineEnd <= currentLineStart) {
                                    nextLine = null
                                    nextLineSet = true
                                    return
                                }
                                break
                            }

                            if (lineEnd - currentLineStart > maxWidth) {
                                lineEnd = breakIterator.preceding(lineEnd)
                                if (lineEnd <= currentLineStart) {
                                    lineEnd = currentLineStart + maxWidth
                                }
                                break
                            }

                            if (string[lineEnd - 1] == '\n') {
                                nextLine = string.substring(currentLineStart, lineEnd - 1)
                                nextLineSet = true
                                currentLineStart = lineEnd
                                return
                            }
                        }
                        nextLine = string.substring(currentLineStart, lineEnd)
                        nextLineSet = true
                        currentLineStart = lineEnd
                    }

                    override fun next(): String {
                        val ret = nextLine
                        nextLine = null
                        nextLineSet = false
                        return ret!!
                    }

                    override fun remove() {
                        throw UnsupportedOperationException()
                    }
                }
            }
        }
    }

    /**
     * Splits the given string into lines using on any embedded newlines, and wrapping the text as needed to conform to
     * the given maximum line width.
     *
     * This uses and assumes unix-style newlines
     *
     * @param str The string to split
     * @param maxWidth The maximum length of any line
     * @param output If given, try to use this array as the return value. If there are more values than will fit
     *               into the array, a new array will be allocated and returned, while the given array will be filled
     *               with as many lines as would fit.
     * @return The split lines from the original, as an array of Strings. The returned array may be larger than the
     *         number of lines. If this is the case, the end of the split lines will be denoted by a null entry in the
     *         array. If there is no null entry, then the size of the array exactly matches the number of lines.
     *         The returned lines will not contain an ending newline
     */
    @JvmStatic
    fun wrapString(str: String, maxWidth: Int, output: Array<String?>?): Array<String?> {
        var result: Array<String?> = output ?: arrayOfNulls((str.length / maxWidth * 1.5 + 1).toInt())

        var lineStart = 0
        var arrayIndex = 0
        var i = 0
        while (i < str.length) {
            val c = str[i]

            if (c == '\n') {
                result = addString(result, str.substring(lineStart, i), arrayIndex++)
                lineStart = i + 1
            } else if (i - lineStart == maxWidth) {
                result = addString(result, str.substring(lineStart, i), arrayIndex++)
                lineStart = i
            }
            i++
        }
        if (lineStart != i || i == 0) {
            result = addString(result, str.substring(lineStart), arrayIndex++, result.size + 1)
        }

        if (arrayIndex < result.size) {
            result[arrayIndex] = null
        }
        return result
    }

    private fun addString(arr: Array<String?>, str: String, index: Int): Array<String?> {
        var arr = arr
        if (index >= arr.size) {
            arr = enlargeArray(arr, Math.ceil((arr.size + 1) * 1.5).toInt())
        }

        arr[index] = str
        return arr
    }

    private fun addString(
        arr: Array<String?>,
        str: String,
        index: Int,
        newLength: Int
    ): Array<String?> {
        var arr = arr
        if (index >= arr.size) {
            arr = enlargeArray(arr, newLength)
        }

        arr[index] = str
        return arr
    }

    private fun enlargeArray(arr: Array<String?>, newLength: Int): Array<String?> {
        val newArr = arrayOfNulls<String>(newLength)
        System.arraycopy(arr, 0, newArr, 0, arr.size)
        return newArr
    }

    @JvmStatic
    fun printWrappedString(stream: PrintStream, string: String, maxWidth: Int) {
        for (str in wrapStringOnBreaks(string, maxWidth)) {
            stream.println(str)
        }
    }
}
