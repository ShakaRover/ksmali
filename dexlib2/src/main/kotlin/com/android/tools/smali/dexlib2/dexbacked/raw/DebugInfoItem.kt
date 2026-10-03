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

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes

object DebugInfoItem {

    fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
        return object : SectionAnnotator(annotator, mapItem) {
            override fun getItemName(): String {
                return "debug_info_item"
            }

            override fun annotateItem(out: AnnotatedBytes, itemIndex: Int, itemIdentity: String?) {
                val reader: DexReader<out DexBuffer> = dexFile.buffer.readerAt(out.cursor)

                val lineStart = reader.readBigUleb128()
                out.annotateTo(reader.offset, "line_start = %d", lineStart.toLong() and 0xFFFFFFFFL)

                val parametersSize = reader.readSmallUleb128()
                out.annotateTo(reader.offset, "parameters_size = %d", parametersSize)

                if (parametersSize > 0) {
                    out.annotate(0, "parameters:")
                    out.indent()
                    for (i in 0 until parametersSize) {
                        val paramaterIndex = reader.readSmallUleb128() - 1
                        out.annotateTo(
                            reader.offset, "%s",
                            StringIdItem.getOptionalReferenceAnnotation(
                                dexFile, paramaterIndex, true
                            )
                        )
                    }
                    out.deindent()
                }

                out.annotate(0, "debug opcodes:")
                out.indent()

                var codeAddress = 0
                var lineNumber = lineStart

                loop@ while (true) {
                    val opcode = reader.readUbyte()
                    when (opcode) {
                        DebugItemType.END_SEQUENCE -> {
                            out.annotateTo(reader.offset, "DBG_END_SEQUENCE")
                            break@loop
                        }
                        DebugItemType.ADVANCE_PC -> {
                            out.annotateTo(reader.offset, "DBG_ADVANCE_PC")
                            out.indent()
                            val addressDiff = reader.readSmallUleb128()
                            codeAddress += addressDiff
                            out.annotateTo(
                                reader.offset, "addr_diff = +0x%x: 0x%x", addressDiff,
                                codeAddress
                            )
                            out.deindent()
                        }
                        DebugItemType.ADVANCE_LINE -> {
                            out.annotateTo(reader.offset, "DBG_ADVANCE_LINE")
                            out.indent()
                            val lineDiff = reader.readSleb128()
                            lineNumber += lineDiff
                            out.annotateTo(
                                reader.offset, "line_diff = +%d: %d", Math.abs(lineDiff),
                                lineNumber
                            )
                            out.deindent()
                        }
                        DebugItemType.START_LOCAL -> {
                            out.annotateTo(reader.offset, "DBG_START_LOCAL")
                            out.indent()
                            val registerNum = reader.readSmallUleb128()
                            out.annotateTo(reader.offset, "register_num = v%d", registerNum)
                            val nameIndex = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "name_idx = %s",
                                StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIndex, true)
                            )
                            val typeIndex = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "type_idx = %s",
                                TypeIdItem.getOptionalReferenceAnnotation(dexFile, typeIndex)
                            )
                            out.deindent()
                        }
                        DebugItemType.START_LOCAL_EXTENDED -> {
                            out.annotateTo(reader.offset, "DBG_START_LOCAL_EXTENDED")
                            out.indent()
                            val registerNum = reader.readSmallUleb128()
                            out.annotateTo(reader.offset, "register_num = v%d", registerNum)
                            val nameIndex = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "name_idx = %s",
                                StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIndex, true)
                            )
                            val typeIndex = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "type_idx = %s",
                                TypeIdItem.getOptionalReferenceAnnotation(dexFile, typeIndex)
                            )
                            val sigIndex = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "sig_idx = %s",
                                StringIdItem.getOptionalReferenceAnnotation(dexFile, sigIndex, true)
                            )
                            out.deindent()
                        }
                        DebugItemType.END_LOCAL -> {
                            out.annotateTo(reader.offset, "DBG_END_LOCAL")
                            out.indent()
                            val registerNum = reader.readSmallUleb128()
                            out.annotateTo(reader.offset, "register_num = v%d", registerNum)
                            out.deindent()
                        }
                        DebugItemType.RESTART_LOCAL -> {
                            out.annotateTo(reader.offset, "DBG_RESTART_LOCAL")
                            out.indent()
                            val registerNum = reader.readSmallUleb128()
                            out.annotateTo(reader.offset, "register_num = v%d", registerNum)
                            out.deindent()
                        }
                        DebugItemType.PROLOGUE_END -> {
                            out.annotateTo(reader.offset, "DBG_SET_PROLOGUE_END")
                        }
                        DebugItemType.EPILOGUE_BEGIN -> {
                            out.annotateTo(reader.offset, "DBG_SET_EPILOGUE_BEGIN")
                        }
                        DebugItemType.SET_SOURCE_FILE -> {
                            out.annotateTo(reader.offset, "DBG_SET_FILE")
                            out.indent()
                            val nameIdx = reader.readSmallUleb128() - 1
                            out.annotateTo(
                                reader.offset, "name_idx = %s",
                                StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIdx)
                            )
                            out.deindent()
                        }
                        else -> {
                            val adjusted = opcode - 0x0A
                            val addressDiff = adjusted / 15
                            val lineDiff = (adjusted % 15) - 4
                            codeAddress += addressDiff
                            lineNumber += lineDiff
                            out.annotateTo(
                                reader.offset,
                                "address_diff = +0x%x:0x%x, line_diff = +%d:%d, ",
                                addressDiff, codeAddress, lineDiff, lineNumber
                            )
                        }
                    }
                }
                out.deindent()
            }
        }
    }
}
