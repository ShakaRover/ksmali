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

import com.android.tools.smali.dexlib2.dexbacked.CDexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator

/**
 * This table maps a method index to the offset to the debug_info_item associated with that method.
 *
 * There are 2 sections in the table. Each section is 32-bit aligned.
 *
 * The first section is arranged into chunks of data. Each chunk represents 16 method indices, starting at 0.
 *
 * The first two bytes of a chunk contain a 16-bit bitmask, encoded as a big-endian unsigned short. The least
 * significant bit corresponds with the first index in the chunk of 16 method indexes, with each bit being the next
 * index.
 *
 * Next, there are a series of uleb128 values, one for each set bit in the bitmask. Nothing is written for bits that are
 * not set. The first uleb128 value is associated with the least significant set bit in the mask.
 *
 * Each uleb128 value is the offset from the start of the data section to the debug_info_item that is associated with
 * the method having that method_index. It is encoded as the difference in offset from the previously encoded offset,
 * with the first offset in that chunk being relative to the first debug_info_item (e.g. the debug_info_base field in
 * the cdex header).
 *
 * It's worth noting that the offsets in each chunk of 16 are not necessarily in order. If a later offset is smaller
 * than an earlier offset, the uleb128 is a large value that, when added to the previous offset, causes integer
 * overflow, and wraps back around to the lower offset. Or, to put it another way, the uleb128 should actually be
 * interpreted as a signed number, even though it's encoded in a format that is nominally for unsigned values.
 *
 * The second part of the table contains a simple list of 32-bit offsets to each chunk. The offsets are relative to
 * the beginning of the debug offset table.
 */
object CdexDebugOffsetTable {
    @JvmStatic
    fun annotate(annotator: DexAnnotator, buffer: DexBuffer) {
        val reader: DexReader<out DexBuffer> = buffer.readerAt(annotator.cursor)

        val debugInfoAnnotator = annotator.getAnnotator(ItemType.DEBUG_INFO_ITEM)

        val methodCount = annotator.dexFile.methodSection.size

        var methodIndex = 0
        while (methodIndex < methodCount) {
            annotator.annotate(
                0, "Offset chuck for methods %d-%d", methodIndex,
                Math.min(methodIndex + 16, methodCount)
            )
            annotator.indent()

            var bitmask = reader.readUbyte() shl 8
            bitmask = bitmask or reader.readUbyte()
            val sb = StringBuilder()
            for (i in 0 until 16) {
                sb.append((bitmask shr i) and 1)
            }
            annotator.annotate(2, "bitmask: 0b%s", sb.reverse())

            var debugOffset = (annotator.dexFile as CDexBackedDexFile).getDebugInfoBase()
            for (i in 0 until 16) {
                if ((bitmask and 1) != 0) {
                    val offsetDelta = reader.readBigUleb128()

                    debugOffset += offsetDelta

                    annotator.annotateTo(
                        reader.offset, "[method_id: %d]: offset_delta: %d  (offset=0x%x)",
                        methodIndex + i,
                        offsetDelta, debugOffset
                    )

                    debugInfoAnnotator!!.setItemIdentity(
                        debugOffset,
                        annotator.dexFile.methodSection.get(methodIndex + i).toString()
                    )
                }

                bitmask = bitmask shr 1
            }

            annotator.deindent()
            methodIndex += 16
        }
    }
}
