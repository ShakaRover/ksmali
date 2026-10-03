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

package com.android.tools.smali.dexlib2.dexbacked.util

import com.android.tools.smali.dexlib2.dexbacked.DexBackedAnnotation
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile

abstract class AnnotationsDirectory {
    abstract fun getFieldAnnotationCount(): Int

    abstract fun getClassAnnotations(): Set<DexBackedAnnotation>

    abstract fun getFieldAnnotationIterator(): AnnotationIterator

    abstract fun getMethodAnnotationIterator(): AnnotationIterator

    abstract fun getParameterAnnotationIterator(): AnnotationIterator

    /**
     * This provides a forward-only, skipable iteration over the field_annotation, method_annotation or
     * parameter_annotation lists in an annotations_directory_item.
     *
     * These lists associate a key, either a field or method index, with an offset to where the annotation data for
     * that field/method/parameter is stored.
     */
    interface AnnotationIterator {
        /**
         * Seeks the iterator forward, to the first item whose key is >= the requested key. If the requested key value
         * is less than that of the item that the iterator currently points to, it will not be moved forward.
         *
         * If an item with the requested key is found, the associated annotation offset is returned. Otherwise, 0 is
         * returned.
         *
         * @param key The method/field index to search for
         * @return The annotation offset associated with the requested key, or 0 if not found.
         */
        fun seekTo(key: Int): Int

        /**
         * Resets the iterator to the beginning of its list.
         */
        fun reset()

        companion object {
            val EMPTY: AnnotationIterator = object : AnnotationIterator {
                override fun seekTo(key: Int): Int = 0

                override fun reset() {
                }
            }
        }
    }

    companion object {
        val EMPTY: AnnotationsDirectory = object : AnnotationsDirectory() {
            override fun getFieldAnnotationCount(): Int = 0

            override fun getClassAnnotations(): Set<DexBackedAnnotation> = emptySet()

            override fun getFieldAnnotationIterator(): AnnotationIterator =
                AnnotationIterator.EMPTY

            override fun getMethodAnnotationIterator(): AnnotationIterator =
                AnnotationIterator.EMPTY

            override fun getParameterAnnotationIterator(): AnnotationIterator =
                AnnotationIterator.EMPTY
        }

        fun newOrEmpty(
            dexFile: DexBackedDexFile,
            directoryAnnotationsOffset: Int
        ): AnnotationsDirectory {
            if (directoryAnnotationsOffset == 0) {
                return EMPTY
            }
            return AnnotationsDirectoryImpl(dexFile, directoryAnnotationsOffset)
        }

        fun getAnnotations(
            dexFile: DexBackedDexFile,
            annotationSetOffset: Int
        ): Set<DexBackedAnnotation> {
            if (annotationSetOffset != 0) {
                val size = dexFile.dataBuffer.readSmallUint(annotationSetOffset)
                return object : FixedSizeSet<DexBackedAnnotation>() {
                    override val size: Int
                        get() = size

                    override fun readItem(index: Int): DexBackedAnnotation {
                        val annotationOffset = dexFile.dataBuffer.readSmallUint(
                            annotationSetOffset + 4 + (4 * index)
                        )
                        return DexBackedAnnotation(dexFile, annotationOffset)
                    }
                }
            }

            return emptySet()
        }

        fun getParameterAnnotations(
            dexFile: DexBackedDexFile,
            annotationSetListOffset: Int
        ): List<Set<DexBackedAnnotation>> {
            if (annotationSetListOffset > 0) {
                val size = dexFile.dataBuffer.readSmallUint(annotationSetListOffset)

                return object : FixedSizeList<Set<DexBackedAnnotation>>() {
                    override val size: Int
                        get() = size

                    override fun readItem(index: Int): Set<DexBackedAnnotation> {
                        val annotationSetOffset = dexFile.dataBuffer.readSmallUint(
                            annotationSetListOffset + 4 + index * 4
                        )
                        return getAnnotations(dexFile, annotationSetOffset)
                    }
                }
            }
            return emptyList()
        }
    }

    private class AnnotationsDirectoryImpl(
        val dexFile: DexBackedDexFile,
        private val directoryOffset: Int
    ) : AnnotationsDirectory() {
        override fun getFieldAnnotationCount(): Int {
            return dexFile.dataBuffer.readSmallUint(directoryOffset + FIELD_COUNT_OFFSET)
        }

        fun getMethodAnnotationCount(): Int {
            return dexFile.dataBuffer.readSmallUint(directoryOffset + METHOD_COUNT_OFFSET)
        }

        fun getParameterAnnotationCount(): Int {
            return dexFile.dataBuffer.readSmallUint(directoryOffset + PARAMETER_COUNT_OFFSET)
        }

        override fun getClassAnnotations(): Set<DexBackedAnnotation> {
            return getAnnotations(dexFile, dexFile.dataBuffer.readSmallUint(directoryOffset))
        }

        override fun getFieldAnnotationIterator(): AnnotationIterator {
            val fieldAnnotationCount = getFieldAnnotationCount()
            if (fieldAnnotationCount == 0) {
                return AnnotationIterator.EMPTY
            }
            return AnnotationIteratorImpl(
                directoryOffset + ANNOTATIONS_START_OFFSET, fieldAnnotationCount
            )
        }

        override fun getMethodAnnotationIterator(): AnnotationIterator {
            val methodCount = getMethodAnnotationCount()
            if (methodCount == 0) {
                return AnnotationIterator.EMPTY
            }
            val fieldCount = getFieldAnnotationCount()
            val methodAnnotationsOffset = directoryOffset + ANNOTATIONS_START_OFFSET +
                fieldCount * FIELD_ANNOTATION_SIZE
            return AnnotationIteratorImpl(methodAnnotationsOffset, methodCount)
        }

        override fun getParameterAnnotationIterator(): AnnotationIterator {
            val parameterAnnotationCount = getParameterAnnotationCount()
            if (parameterAnnotationCount == 0) {
                return AnnotationIterator.EMPTY
            }
            val fieldCount = getFieldAnnotationCount()
            val methodCount = getMethodAnnotationCount()
            val parameterAnnotationsOffset = directoryOffset + ANNOTATIONS_START_OFFSET +
                fieldCount * FIELD_ANNOTATION_SIZE +
                methodCount * METHOD_ANNOTATION_SIZE
            return AnnotationIteratorImpl(parameterAnnotationsOffset, parameterAnnotationCount)
        }

        private inner class AnnotationIteratorImpl(
            private val startOffset: Int,
            private val size: Int
        ) : AnnotationIterator {
            private var currentIndex: Int
            private var currentItemIndex: Int

            init {
                this.currentItemIndex = dexFile.dataBuffer.readSmallUint(startOffset)
                this.currentIndex = 0
            }

            override fun seekTo(itemIndex: Int): Int {
                while (currentItemIndex < itemIndex && (currentIndex + 1) < size) {
                    currentIndex++
                    currentItemIndex =
                        dexFile.dataBuffer.readSmallUint(startOffset + (currentIndex * 8))
                }

                if (currentItemIndex == itemIndex) {
                    return dexFile.dataBuffer.readSmallUint(startOffset + (currentIndex * 8) + 4)
                }
                return 0
            }

            override fun reset() {
                this.currentItemIndex = dexFile.dataBuffer.readSmallUint(startOffset)
                this.currentIndex = 0
            }
        }

        companion object {
            private const val FIELD_COUNT_OFFSET = 4
            private const val METHOD_COUNT_OFFSET = 8
            private const val PARAMETER_COUNT_OFFSET = 12
            private const val ANNOTATIONS_START_OFFSET = 16

            /** The size of a field_annotation structure */
            private const val FIELD_ANNOTATION_SIZE = 8

            /** The size of a method_annotation structure */
            private const val METHOD_ANNOTATION_SIZE = 8
        }
    }
}
