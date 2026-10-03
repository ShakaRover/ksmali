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

package com.android.tools.smali.dexlib2.dexbacked.raw

import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes

object AnnotationDirectoryItem {
    const val CLASS_ANNOTATIONS_OFFSET = 0
    const val FIELD_SIZE_OFFSET = 4
    const val ANNOTATED_METHOD_SIZE_OFFSET = 8
    const val ANNOTATED_PARAMETERS_SIZE = 12

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "annotation_directory_item"
            }

            override val itemAlignment: Int get() {
                return 4
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val classAnnotationsOffset = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(
                    4, "class_annotations_off = %s",
                    AnnotationSetItem.getReferenceAnnotation(dexFile, classAnnotationsOffset)
                )

                val fieldsSize = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "fields_size = %d", fieldsSize)

                val annotatedMethodsSize = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "annotated_methods_size = %d", annotatedMethodsSize)

                val annotatedParameterSize = dexFile.buffer.readSmallUint(out.cursor)
                out.annotate(4, "annotated_parameters_size = %d", annotatedParameterSize)

                if (fieldsSize > 0) {
                    out.annotate(0, "field_annotations:")
                    out.indent()
                    for (i in 0 until fieldsSize) {
                        out.annotate(0, "field_annotation[%d]", i)
                        out.indent()
                        val fieldIndex = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(4, "%s", FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex))
                        val annotationOffset = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(
                            4, "%s",
                            AnnotationSetItem.getReferenceAnnotation(dexFile, annotationOffset)
                        )
                        out.deindent()
                    }
                    out.deindent()
                }

                if (annotatedMethodsSize > 0) {
                    out.annotate(0, "method_annotations:")
                    out.indent()
                    for (i in 0 until annotatedMethodsSize) {
                        out.annotate(0, "method_annotation[%d]", i)
                        out.indent()
                        val methodIndex = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(4, "%s", MethodIdItem.getReferenceAnnotation(dexFile, methodIndex))
                        val annotationOffset = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(
                            4, "%s",
                            AnnotationSetItem.getReferenceAnnotation(dexFile, annotationOffset)
                        )
                        out.deindent()
                    }
                    out.deindent()
                }

                if (annotatedParameterSize > 0) {
                    out.annotate(0, "parameter_annotations:")
                    out.indent()
                    for (i in 0 until annotatedParameterSize) {
                        out.annotate(0, "parameter_annotation[%d]", i)
                        out.indent()
                        val methodIndex = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(4, "%s", MethodIdItem.getReferenceAnnotation(dexFile, methodIndex))
                        val annotationOffset = dexFile.buffer.readSmallUint(out.cursor)
                        out.annotate(
                            4, "%s",
                            AnnotationSetRefList.getReferenceAnnotation(dexFile, annotationOffset)
                        )
                        out.deindent()
                    }
                    out.deindent()
                }
            }
        }
    }
}
