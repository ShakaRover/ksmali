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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.raw.OdexHeaderItem
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeList
import com.android.tools.smali.dexlib2.util.DexUtil
import com.android.tools.smali.util.InputStreamUtil
import java.io.IOException
import java.io.InputStream

class DexBackedOdexFile(
    opcodes: Opcodes,
    private val odexBuf: ByteArray,
    dexBuf: ByteArray
) : DexBackedDexFile(opcodes, dexBuf) {

    override fun supportsOptimizedOpcodes(): Boolean {
        return true
    }

    val dependencies: List<String>
        get() {
            val dexOffset = OdexHeaderItem.getDexOffset(odexBuf)
            val dependencyOffset = OdexHeaderItem.getDependenciesOffset(odexBuf) - dexOffset

            val fromStartBuffer = DexBuffer(buffer.buf, 0)
            val dependencyCount = fromStartBuffer.readInt(dependencyOffset + DEPENDENCY_COUNT_OFFSET)

            return object : VariableSizeList<String>(
                dataBuffer, dependencyOffset + DEPENDENCY_START_OFFSET, dependencyCount
            ) {
                override fun readNextItem(reader: DexReader<out DexBuffer>, index: Int): String {
                    val length = reader.readInt()
                    val offset = reader.offset
                    reader.moveRelative(length + 20)
                    return String(fromStartBuffer.buf, offset, length - 1, Charsets.US_ASCII)
                }
            }
        }

    val odexVersion: Int
        get() {
            return OdexHeaderItem.getVersion(odexBuf, 0)
        }

    class NotAnOdexFile : RuntimeException {
        constructor()

        constructor(cause: Throwable?) : super(cause)

        constructor(message: String?) : super(message)

        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    companion object {
        private const val DEPENDENCY_COUNT_OFFSET = 12
        private const val DEPENDENCY_START_OFFSET = 16

        @Throws(IOException::class)
        fun fromInputStream(opcodes: Opcodes, `is`: InputStream): DexBackedOdexFile {
            DexUtil.verifyOdexHeader(`is`)

            `is`.reset()
            val odexBuf = ByteArray(OdexHeaderItem.ITEM_SIZE)
            InputStreamUtil.readFully(`is`, odexBuf)
            val dexOffset = OdexHeaderItem.getDexOffset(odexBuf)
            if (dexOffset > OdexHeaderItem.ITEM_SIZE) {
                InputStreamUtil.skipFully(`is`, (dexOffset - OdexHeaderItem.ITEM_SIZE).toLong())
            }

            val dexBuf = InputStreamUtil.toByteArray(`is`)

            return DexBackedOdexFile(opcodes, odexBuf, dexBuf)
        }
    }
}
