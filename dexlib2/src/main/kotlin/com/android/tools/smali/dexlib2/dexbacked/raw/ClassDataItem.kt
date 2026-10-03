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

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.StringUtils

object ClassDataItem {
    @JvmStatic
    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            private var codeItemAnnotator: SectionAnnotator? = null

            override fun annotateSection(out: AnnotatedBytes) {
                codeItemAnnotator = annotator.getAnnotator(ItemType.CODE_ITEM)
                super.annotateSection(out)
            }

            override fun getItemName(): String {
                return "class_data_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val reader: DexReader<out DexBuffer> = dexFile.buffer.readerAt(out.cursor)

                val staticFieldsSize = reader.readSmallUleb128()
                out.annotateTo(reader.offset, "static_fields_size = %d", staticFieldsSize)

                val instanceFieldsSize = reader.readSmallUleb128()
                out.annotateTo(reader.offset, "instance_fields_size = %d", instanceFieldsSize)

                val directMethodsSize = reader.readSmallUleb128()
                out.annotateTo(reader.offset, "direct_methods_size = %d", directMethodsSize)

                val virtualMethodsSize = reader.readSmallUleb128()
                out.annotateTo(reader.offset, "virtual_methods_size = %d", virtualMethodsSize)

                var previousIndex = 0
                if (staticFieldsSize > 0) {
                    out.annotate(0, "static_fields:")
                    out.indent()
                    for (i in 0 until staticFieldsSize) {
                        out.annotate(0, "static_field[%d]", i)
                        out.indent()
                        previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex)
                        out.deindent()
                    }
                    out.deindent()
                }

                if (instanceFieldsSize > 0) {
                    out.annotate(0, "instance_fields:")
                    out.indent()
                    previousIndex = 0
                    for (i in 0 until instanceFieldsSize) {
                        out.annotate(0, "instance_field[%d]", i)
                        out.indent()
                        previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex)
                        out.deindent()
                    }
                    out.deindent()
                }

                if (directMethodsSize > 0) {
                    out.annotate(0, "direct_methods:")
                    out.indent()
                    previousIndex = 0
                    for (i in 0 until directMethodsSize) {
                        out.annotate(0, "direct_method[%d]", i)
                        out.indent()
                        previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex)
                        out.deindent()
                    }
                    out.deindent()
                }

                if (virtualMethodsSize > 0) {
                    out.annotate(0, "virtual_methods:")
                    out.indent()
                    previousIndex = 0
                    for (i in 0 until virtualMethodsSize) {
                        out.annotate(0, "virtual_method[%d]", i)
                        out.indent()
                        previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex)
                        out.deindent()
                    }
                    out.deindent()
                }
            }

            private fun annotateEncodedField(
                out: AnnotatedBytes,
                dexFile: DexBackedDexFile,
                reader: DexReader<out DexBuffer>,
                previousIndex: Int
            ): Int {
                // large values may be used for the index delta, which cause the cumulative index to overflow upon
                // addition, effectively allowing out of order entries.
                val indexDelta = reader.readLargeUleb128()
                val fieldIndex = previousIndex + indexDelta
                out.annotateTo(
                    reader.offset, "field_idx_diff = %d: %s", indexDelta,
                    FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex)
                )

                val accessFlags = reader.readSmallUleb128()
                out.annotateTo(
                    reader.offset, "access_flags = 0x%x: %s", accessFlags,
                    StringUtils.join(
                        AccessFlags.getAccessFlagsForField(accessFlags).asList(), "|"
                    )
                )

                return fieldIndex
            }

            private fun annotateEncodedMethod(
                out: AnnotatedBytes,
                dexFile: DexBackedDexFile,
                reader: DexReader<out DexBuffer>,
                previousIndex: Int
            ): Int {
                // large values may be used for the index delta, which cause the cumulative index to overflow upon
                // addition, effectively allowing out of order entries.
                val indexDelta = reader.readLargeUleb128()
                val methodIndex = previousIndex + indexDelta
                out.annotateTo(
                    reader.offset, "method_idx_diff = %d: %s", indexDelta,
                    MethodIdItem.getReferenceAnnotation(dexFile, methodIndex)
                )

                val accessFlags = reader.readSmallUleb128()
                out.annotateTo(
                    reader.offset, "access_flags = 0x%x: %s", accessFlags,
                    StringUtils.join(
                        AccessFlags.getAccessFlagsForField(accessFlags).asList(), "|"
                    )
                )

                val codeOffset = reader.readSmallUleb128()
                if (codeOffset == 0) {
                    out.annotateTo(reader.offset, "code_off = code_item[NO_OFFSET]")
                } else {
                    out.annotateTo(reader.offset, "code_off = code_item[0x%x]", codeOffset)
                    addCodeItemIdentity(codeOffset, MethodIdItem.asString(dexFile, methodIndex))
                }

                return methodIndex
            }

            private fun addCodeItemIdentity(codeItemOffset: Int, methodString: String) {
                if (codeItemAnnotator != null) {
                    codeItemAnnotator!!.setItemIdentity(codeItemOffset, methodString)
                }
            }
        }
    }
}
