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

package com.android.tools.smali.util

import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

/**
 * Class that takes a combined output destination and provides two
 * output writers, one of which ends up writing to the left column and
 * one which goes on the right.
 */
class TwoColumnOutput(
    private val out: Writer,
    private val leftWidth: Int,
    private val rightWidth: Int,
    private val spacer: String
) {
    /**
     * Constructs an instance.
     *
     * @param out non-null; stream to send final output to
     * @param leftWidth &gt;= 1; width of the left column, in characters
     * @param rightWidth &gt;= 1; width of the right column, in characters
     * @param spacer non-null; spacer string to sit between the two columns
     */
    constructor(out: OutputStream, leftWidth: Int, rightWidth: Int, spacer: String) :
        this(OutputStreamWriter(out), leftWidth, rightWidth, spacer)

    init {
        if (leftWidth < 1) {
            throw IllegalArgumentException("leftWidth < 1")
        }

        if (rightWidth < 1) {
            throw IllegalArgumentException("rightWidth < 1")
        }
    }

    private var leftLines: Array<String?>? = null
    private var rightLines: Array<String?>? = null

    @Throws(IOException::class)
    fun write(left: String, right: String) {
        leftLines = StringWrapper.wrapString(left, leftWidth, leftLines)
        rightLines = StringWrapper.wrapString(right, rightWidth, rightLines)
        var leftCount = leftLines!!.size
        var rightCount = rightLines!!.size

        var i = 0
        while (i < leftCount || i < rightCount) {
            var leftLine: String? = null
            var rightLine: String? = null

            if (i < leftCount) {
                leftLine = leftLines!![i]
                if (leftLine == null) {
                    leftCount = i
                }
            }

            if (i < rightCount) {
                rightLine = rightLines!![i]
                if (rightLine == null) {
                    rightCount = i
                }
            }

            if (leftLine != null || rightLine != null) {
                var written = 0
                if (leftLine != null) {
                    out.write(leftLine)
                    written = leftLine.length
                }

                val remaining = leftWidth - written
                if (remaining > 0) {
                    writeSpaces(out, remaining)
                }

                out.write(spacer)

                if (rightLine != null) {
                    out.write(rightLine)
                }

                out.write('\n'.code)
            }
            i++
        }
    }

    companion object {
        /**
         * Writes the given number of spaces to the given writer.
         *
         * @param out non-null; where to write
         * @param amt &gt;= 0; the number of spaces to write
         */
        @Throws(IOException::class)
        private fun writeSpaces(out: Writer, amt: Int) {
            var amt = amt
            while (amt > 0) {
                out.write(' '.code)
                amt--
            }
        }
    }
}
