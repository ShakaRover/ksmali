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

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes

object HiddenApiClassDataItem {
    const val SIZE_OFFSET = 0x0
    const val OFFSETS_LIST_OFFSET = 0x4

    const val OFFSET_ITEM_SIZE = 0x4

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override val itemName: String get() {
                return "hiddenapi_class_data_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val startOffset = out.cursor

                out.annotate(4, "size = 0x%x", dexFile.dataBuffer.readSmallUint(out.cursor))

                var index = 0
                for (classDef in dexFile.classes) {
                    out.annotate(0, "[%d] %s", index, classDef)
                    out.indent()

                    val offset = dexFile.dataBuffer.readSmallUint(out.cursor)
                    if (offset == 0) {
                        out.annotate(4, "offset = 0x%x", offset)
                    } else {
                        out.annotate(
                            4, "offset = 0x%x (absolute offset: 0x%x)", offset,
                            startOffset + offset
                        )
                    }

                    val nextOffset = out.cursor
                    if (offset > 0) {
                        out.deindent()

                        out.moveTo(startOffset + offset)

                        val reader: DexReader<out DexBuffer> =
                            dexFile.buffer.readerAt(out.cursor)

                        for (field in classDef.staticFields) {
                            out.annotate(0, "%s:", field)
                            out.indent()
                            val restrictions = reader.readSmallUleb128()
                            out.annotateTo(
                                reader.offset, "restriction = 0x%x: %s",
                                restrictions,
                                HiddenApiRestriction.formatHiddenRestrictions(restrictions)
                            )
                            out.deindent()
                        }
                        for (field in classDef.instanceFields) {
                            out.annotate(0, "%s:", field)
                            out.indent()
                            val restrictions = reader.readSmallUleb128()
                            out.annotateTo(
                                reader.offset, "restriction = 0x%x: %s",
                                restrictions,
                                HiddenApiRestriction.formatHiddenRestrictions(restrictions)
                            )
                            out.deindent()
                        }
                        for (method in classDef.directMethods) {
                            out.annotate(0, "%s:", method)
                            out.indent()
                            val restrictions = reader.readSmallUleb128()
                            out.annotateTo(
                                reader.offset, "restriction = 0x%x: %s",
                                restrictions,
                                HiddenApiRestriction.formatHiddenRestrictions(restrictions)
                            )
                            out.deindent()
                        }
                        for (method in classDef.virtualMethods) {
                            out.annotate(0, "%s:", method)
                            out.indent()
                            val restrictions = reader.readSmallUleb128()
                            out.annotateTo(
                                reader.offset, "restriction = 0x%x: %s",
                                restrictions,
                                HiddenApiRestriction.formatHiddenRestrictions(restrictions)
                            )
                            out.deindent()
                        }

                        out.indent()
                    }

                    out.moveTo(nextOffset)

                    out.deindent()

                    index++
                }
            }
        }
    }
}
