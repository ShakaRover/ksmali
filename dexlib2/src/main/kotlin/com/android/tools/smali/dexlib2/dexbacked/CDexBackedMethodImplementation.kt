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

package com.android.tools.smali.dexlib2.dexbacked

import com.android.tools.smali.dexlib2.dexbacked.raw.CodeItem

class CDexBackedMethodImplementation(
    dexFile: DexBackedDexFile,
    method: DexBackedMethod,
    codeOffset: Int
) : DexBackedMethodImplementation(dexFile, method, codeOffset) {

    val insCount: Int
        get() {
            var insCount = (dexFile.dataBuffer.readUshort(codeOffset) shr
                CodeItem.CDEX_INS_COUNT_SHIFT) and 0xf

            if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_INS_COUNT) != 0) {
                var preheaderCount = 1

                if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                    preheaderCount += 2
                }
                if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
                    preheaderCount++
                }
                insCount += dexFile.dataBuffer.readUshort(codeOffset - 2 * preheaderCount)
            }
            return insCount
        }

    override val registerCount: Int
        get() {
            var registerCount = (dexFile.dataBuffer.readUshort(codeOffset) shr
                CodeItem.CDEX_REGISTER_COUNT_SHIFT) and 0xf

            registerCount += insCount
            if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
                var preheaderCount = 1
                if ((preheaderFlags and
                        CodeItem.CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) > 0
                ) {
                    preheaderCount += 2
                }
                registerCount += dexFile.dataBuffer.readUshort(codeOffset - 2 * preheaderCount)
            }
            return registerCount
        }

    override val instructionsSize: Int
        get() {
            var instructionsSize = dexFile.dataBuffer.readUshort(
                codeOffset + CodeItem.CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET
            ) shr CodeItem.CDEX_INSTRUCTIONS_SIZE_SHIFT

            if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                instructionsSize += dexFile.dataBuffer.readUshort(codeOffset - 2)
                instructionsSize += dexFile.dataBuffer.readUshort(codeOffset - 4) shl 16
            }
            return instructionsSize
        }

    override val instructionsStartOffset: Int
        get() = codeOffset + 4

    private val preheaderFlags: Int
        get() = dexFile.dataBuffer.readUshort(
            codeOffset + CodeItem.CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET
        ) and CodeItem.CDEX_PREHEADER_FLAGS_MASK

    override val triesSize: Int
        get() {
            var triesCount = (dexFile.dataBuffer.readUshort(codeOffset) shr
                CodeItem.CDEX_TRIES_SIZE_SHIFT) and 0xf
            if ((preheaderFlags and CodeItem.CDEX_PREHEADER_FLAG_TRIES_COUNT) != 0) {
                var preheaderCount = Integer.bitCount(preheaderFlags)
                if ((preheaderFlags and
                        CodeItem.CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0
                ) {
                    // The instructions size preheader is 2 shorts
                    preheaderCount++
                }
                triesCount += dexFile.dataBuffer.readUshort(codeOffset - 2 * preheaderCount)
            }
            return triesCount
        }

    override val debugOffset: Int
        get() {
        val cdexFile = dexFile as CDexBackedDexFile

        val debugTableItemOffset = (method.methodIndex / 16) * 4
        val bitIndex = method.methodIndex % 16

        val debugInfoOffsetsPos = cdexFile.debugInfoOffsetsPos
        val debugTableOffset = debugInfoOffsetsPos + cdexFile.debugInfoOffsetsTableOffset

        val debugOffsetsOffset =
            cdexFile.dataBuffer.readSmallUint(debugTableOffset + debugTableItemOffset)

        val reader: DexReader<out DexBuffer> =
            cdexFile.dataBuffer.readerAt(debugInfoOffsetsPos + debugOffsetsOffset)

        var bitMask = reader.readUbyte() shl 8
        bitMask += reader.readUbyte()

        if ((bitMask and (1 shl bitIndex)) == 0) {
            return 0
        }

        val offsetCount = Integer.bitCount(bitMask and (0xFFFF shr (16 - bitIndex)))
        var baseDebugOffset = cdexFile.debugInfoBase
        for (i in 0 until offsetCount) {
            baseDebugOffset += reader.readBigUleb128()
        }
        baseDebugOffset += reader.readBigUleb128()
        return baseDebugOffset
    }
}
