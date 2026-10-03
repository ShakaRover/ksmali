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

package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.Hex
import com.android.tools.smali.util.StringUtils
import com.android.tools.smali.util.TwoColumnOutput
import java.io.IOException
import java.io.Writer
import java.util.TreeMap

/**
 * Collects/presents a set of textual annotations, each associated with a range of bytes or a specific point
 * between bytes.
 *
 * Point annotations cannot occur within the middle of a range annotation, only at the endpoints, or some other area
 * with no range annotation.
 *
 * Multiple point annotations can be defined for a given point. They will be printed in insertion order.
 *
 * Only a single range annotation may exist for any given range of bytes. Range annotations may not overlap.
 */
open class AnnotatedBytes(private val outputWidth: Int) {
    /**
     * This defines the bytes ranges and their associated range and point annotations.
     *
     * A range is defined by 2 consecutive keys in the map. The first key is the inclusive start point, the second key
     * is the exclusive end point. The range annotation for a range is associated with the first key for that range.
     * The point annotations for a point are associated with the key at that point.
     */
    private val annotatations: TreeMap<Int, AnnotationEndpoint> = TreeMap()

    var cursor: Int = 0
        private set
    private var indentLevel: Int = 0

    /**
     * &gt;= 8 (if used); the number of bytes of hex output to use
     * in annotations
     */
    private var hexCols: Int = 8

    private var startLimit: Int = -1
    private var endLimit: Int = -1

    /**
     * Moves the cursor to a new location
     *
     * @param offset The offset to move to
     */
    fun moveTo(offset: Int) {
        cursor = offset
    }

    /**
     * Moves the cursor forward or backward by some amount
     *
     * @param offset The amount to move the cursor
     */
    fun moveBy(offset: Int) {
        cursor += offset
    }

    fun annotateTo(offset: Int, msg: String, vararg formatArgs: Any?) {
        annotate(offset - cursor, msg, *formatArgs)
    }

    /**
     * Add an annotation of the given length at the current location.
     *
     * The location
     *
     *
     * @param length the length of data being annotated
     * @param msg the annotation message
     * @param formatArgs format arguments to pass to String.format
     */
    fun annotate(length: Int, msg: String, vararg formatArgs: Any?) {
        if (startLimit != -1 && endLimit != -1 && (cursor < startLimit || cursor >= endLimit)) {
            throw ExceptionWithContext("Annotating outside the parent bounds")
        }

        val formattedMsg: String
        if (formatArgs.isNotEmpty()) {
            formattedMsg = String.format(msg, *formatArgs)
        } else {
            formattedMsg = msg
        }
        val exclusiveEndOffset = cursor + length

        var endPoint: AnnotationEndpoint? = null

        // Do we have an endpoint at the beginning of this annotation already?
        var startPoint = annotatations[cursor]
        if (startPoint == null) {
            // Nope. We need to check that we're not in the middle of an existing range annotation.
            val previousEntry = annotatations.lowerEntry(cursor)
            if (previousEntry != null) {
                val previousAnnotations = previousEntry.value
                val previousRangeAnnotation = previousAnnotations.rangeAnnotation
                if (previousRangeAnnotation != null) {
                    throw ExceptionWithContext(
                        "Cannot add annotation %s, due to existing annotation %s",
                        formatAnnotation(cursor, cursor + length, formattedMsg),
                        formatAnnotation(previousEntry.key, previousRangeAnnotation.annotation)
                    )
                }
            }
        } else if (length > 0) {
            val existingRangeAnnotation = startPoint.rangeAnnotation
            if (existingRangeAnnotation != null) {
                throw ExceptionWithContext(
                    "Cannot add annotation %s, due to existing annotation %s",
                    formatAnnotation(cursor, cursor + length, formattedMsg),
                    formatAnnotation(cursor, existingRangeAnnotation.annotation)
                )
            }
        }

        if (length > 0) {
            // Ensure that there is no later annotation that would intersect with this one
            val nextEntry = annotatations.higherEntry(cursor)
            if (nextEntry != null) {
                val nextKey = nextEntry.key
                if (nextKey < exclusiveEndOffset) {
                    // there is an endpoint that would intersect with this annotation. Find one of the annotations
                    // associated with the endpoint, to print in the error message
                    val nextEndpoint = nextEntry.value
                    val nextRangeAnnotation = nextEndpoint.rangeAnnotation
                    if (nextRangeAnnotation != null) {
                        throw ExceptionWithContext(
                            "Cannot add annotation %s, due to existing annotation %s",
                            formatAnnotation(cursor, cursor + length, formattedMsg),
                            formatAnnotation(nextKey, nextRangeAnnotation.annotation)
                        )
                    }
                    if (nextEndpoint.pointAnnotations.size > 0) {
                        throw ExceptionWithContext(
                            "Cannot add annotation %s, due to existing annotation %s",
                            formatAnnotation(cursor, cursor + length, formattedMsg),
                            formatAnnotation(
                                nextKey, nextKey,
                                nextEndpoint.pointAnnotations[0].annotation
                            )
                        )
                    }
                    // There are no annotations on this endpoint. This "shouldn't" happen. We can still throw an exception.
                    throw ExceptionWithContext(
                        "Cannot add annotation %s, due to existing annotation endpoint at %d",
                        formatAnnotation(cursor, cursor + length, formattedMsg),
                        nextKey
                    )
                }

                if (nextKey == exclusiveEndOffset) {
                    // the next endpoint matches the end of the annotation we are adding
                    endPoint = nextEntry.value
                }
            }
        }

        // Now, actually add the annotation
        // If startPoint is null, we need to create a new one and add it to annotations. Otherwise, we just need to add
        // the annotation to the existing AnnotationEndpoint
        // the range annotation
        if (startPoint == null) {
            startPoint = AnnotationEndpoint()
            annotatations[cursor] = startPoint
        }
        if (length == 0) {
            startPoint.pointAnnotations.add(AnnotationItem(indentLevel, formattedMsg))
        } else {
            startPoint.rangeAnnotation = AnnotationItem(indentLevel, formattedMsg)

            // If endPoint is null, we need to create a new, empty one and add it to annotations
            if (endPoint == null) {
                endPoint = AnnotationEndpoint()
                annotatations[exclusiveEndOffset] = endPoint
            }
        }

        cursor += length
    }

    private fun formatAnnotation(offset: Int, annotationMsg: String): String {
        val endOffset = annotatations.higherKey(offset)
        return formatAnnotation(offset, endOffset, annotationMsg)
    }

    private fun formatAnnotation(offset: Int, endOffset: Int?, annotationMsg: String): String {
        return if (endOffset != null) {
            String.format("[0x%x, 0x%x) \"%s\"", offset, endOffset, annotationMsg)
        } else {
            String.format("[0x%x, ) \"%s\"", offset, annotationMsg)
        }
    }

    fun indent() {
        indentLevel++
    }

    fun deindent() {
        indentLevel--
        if (indentLevel < 0) {
            indentLevel = 0
        }
    }

    private class AnnotationEndpoint {
        /** Annotations that are associated with a specific point between bytes */
        val pointAnnotations: MutableList<AnnotationItem> = ArrayList()

        /** Annotations that are associated with a range of bytes */
        var rangeAnnotation: AnnotationItem? = null
    }

    private class AnnotationItem(val indentLevel: Int, val annotation: String)

    /**
     * @return The width of the right side containing the annotations
     */
    val annotationWidth: Int
        get() {
            val leftWidth = 8 + (hexCols * 2) + (hexCols / 2)
            return outputWidth - leftWidth
        }

    /**
     * Writes the annotated content of this instance to the given writer.
     *
     * @param out non-null; where to write to
     */
    @Throws(IOException::class)
    fun writeAnnotations(out: Writer, data: ByteArray, offset: Int) {
        val rightWidth = annotationWidth
        val leftWidth = outputWidth - rightWidth - 1

        val padding = StringUtils.repeat(" ", 1000)

        val twoc = TwoColumnOutput(out, leftWidth, rightWidth, "|")

        val keys = annotatations.keys.toTypedArray()
        val values = annotatations.values.toTypedArray()

        for (i in 0 until keys.size - 1) {
            val rangeStart = keys[i]
            val rangeEnd = keys[i + 1]

            val annotations = values[i]

            for (pointAnnotation in annotations.pointAnnotations) {
                val paddingSub = padding.substring(0, pointAnnotation.indentLevel * 2)
                twoc.write("", paddingSub + pointAnnotation.annotation)
            }

            var right: String
            val rangeAnnotation = annotations.rangeAnnotation
            if (rangeAnnotation != null) {
                right = padding.substring(0, rangeAnnotation.indentLevel * 2)
                right += rangeAnnotation.annotation
            } else {
                right = ""
            }

            val left = Hex.dump(
                data, rangeStart + offset, rangeEnd - rangeStart, rangeStart + offset, hexCols, 6
            )

            twoc.write(left, right)
        }

        val lastKey = keys[keys.size - 1]
        if (lastKey < data.size) {
            val left = Hex.dump(
                data, lastKey + offset, (data.size - offset) - lastKey, lastKey + offset, hexCols, 6
            )
            twoc.write(left, "")
        }
    }

    fun setLimit(start: Int, end: Int) {
        startLimit = start
        endLimit = end
    }

    fun clearLimit() {
        startLimit = -1
        endLimit = -1
    }
}
