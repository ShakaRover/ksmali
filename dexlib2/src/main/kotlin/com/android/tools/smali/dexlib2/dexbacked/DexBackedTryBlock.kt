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

import com.android.tools.smali.dexlib2.base.BaseTryBlock
import com.android.tools.smali.dexlib2.dexbacked.raw.CodeItem
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeList

class DexBackedTryBlock(
    @JvmField val dexFile: DexBackedDexFile,
    private val tryItemOffset: Int,
    private val handlersStartOffset: Int
) : BaseTryBlock<DexBackedExceptionHandler>() {
    override val startCodeAddress: Int
        get() = dexFile.dataBuffer.readSmallUint(
            tryItemOffset + CodeItem.TryItem.START_ADDRESS_OFFSET
        )

    override val codeUnitCount: Int
        get() = dexFile.dataBuffer.readUshort(
            tryItemOffset + CodeItem.TryItem.CODE_UNIT_COUNT_OFFSET
        )

    override val exceptionHandlers: List<DexBackedExceptionHandler>
        get() {
            val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(
                handlersStartOffset + dexFile.dataBuffer.readUshort(
                    tryItemOffset + CodeItem.TryItem.HANDLER_OFFSET
                )
            )
            val encodedSize = reader.readSleb128()

            if (encodedSize > 0) {
                //no catch-all
                return object : VariableSizeList<DexBackedTypedExceptionHandler>(
                    dexFile.dataBuffer, reader.offset, encodedSize
                ) {
                    override fun readNextItem(
                        reader: DexReader<out DexBuffer>,
                        index: Int
                    ): DexBackedTypedExceptionHandler =
                        DexBackedTypedExceptionHandler(dexFile, reader)
                }
            } else {
                //with catch-all
                val sizeWithCatchAll = (-1 * encodedSize) + 1
                return object : VariableSizeList<DexBackedExceptionHandler>(
                    dexFile.dataBuffer, reader.offset, sizeWithCatchAll
                ) {
                    override fun readNextItem(
                        dexReader: DexReader<out DexBuffer>,
                        index: Int
                    ): DexBackedExceptionHandler {
                        return if (index == sizeWithCatchAll - 1) {
                            DexBackedCatchAllExceptionHandler(dexReader)
                        } else {
                            DexBackedTypedExceptionHandler(dexFile, dexReader)
                        }
                    }
                }
            }
        }
}
