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

package com.android.tools.smali.dexlib2.dexbacked.value

import com.android.tools.smali.dexlib2.base.value.BaseAnnotationEncodedValue
import com.android.tools.smali.dexlib2.dexbacked.DexBackedAnnotationElement
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.dexbacked.util.VariableSizeSet
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue

class DexBackedAnnotationEncodedValue(
    @JvmField val dexFile: DexBackedDexFile,
    reader: DexReader<out DexBuffer>
) : BaseAnnotationEncodedValue(), AnnotationEncodedValue {
    override val type: String
    private val elementCount: Int
    private val elementsOffset: Int

    init {
        this.type = dexFile.typeSection.get(reader.readSmallUleb128())
        this.elementCount = reader.readSmallUleb128()
        this.elementsOffset = reader.offset
        skipElements(reader, elementCount)
    }

    override val elements: Set<DexBackedAnnotationElement>
        get() = object : VariableSizeSet<DexBackedAnnotationElement>(
            dexFile.dataBuffer, elementsOffset, elementCount
        ) {
            override fun readNextItem(
                dexReader: DexReader<out DexBuffer>,
                index: Int
            ): DexBackedAnnotationElement =
                DexBackedAnnotationElement(dexFile, dexReader)
        }

    companion object {
        @JvmStatic
        fun skipFrom(reader: DexReader<out DexBuffer>) {
            reader.skipUleb128() // type
            val elementCount = reader.readSmallUleb128()
            skipElements(reader, elementCount)
        }

        private fun skipElements(reader: DexReader<out DexBuffer>, elementCount: Int) {
            for (i in 0 until elementCount) {
                reader.skipUleb128()
                DexBackedEncodedValue.skipFrom(reader)
            }
        }
    }
}
