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

package com.android.tools.smali.dexlib2.dexbacked.reference

import com.android.tools.smali.dexlib2.base.reference.BaseMethodProtoReference
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeListItem
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList
import com.android.tools.smali.dexlib2.iface.reference.Reference.InvalidReferenceException

class DexBackedMethodProtoReference(
    @JvmField val dexFile: DexBackedDexFile,
    private val protoIndex: Int
) : BaseMethodProtoReference() {
    override val parameterTypes: List<String>
        get() {
            val parametersOffset = dexFile.buffer.readSmallUint(
                dexFile.protoSection.getOffset(protoIndex) + ProtoIdItem.PARAMETERS_OFFSET
            )
            if (parametersOffset > 0) {
                val parameterCount =
                    dexFile.dataBuffer.readSmallUint(parametersOffset + TypeListItem.SIZE_OFFSET)
                val paramListStart = parametersOffset + TypeListItem.LIST_OFFSET
                return object : FixedSizeList<String>() {
                    override val size: Int
                        get() = parameterCount

                    override fun readItem(index: Int): String =
                        dexFile.typeSection.get(
                            dexFile.dataBuffer.readUshort(paramListStart + 2 * index)
                        )
                }
            }
            return emptyList()
        }

    override val returnType: String
        get() = dexFile.typeSection.get(
            dexFile.buffer.readSmallUint(
                dexFile.protoSection.getOffset(protoIndex) + ProtoIdItem.RETURN_TYPE_OFFSET
            )
        )

    /**
     * Calculate and return the private size of a method proto.
     *
     * Calculated as: shorty_idx + return_type_idx + parameters_off + type_list size
     *
     * @return size in bytes
     */
    fun getSize(): Int {
        var size = ProtoIdItem.ITEM_SIZE //3 * uint
        val parameters = parameterTypes
        if (parameters.isNotEmpty()) {
            size += 4 + parameters.size * 2 //uint + size * ushort for type_idxs
        }
        return size
    }

    @Throws(InvalidReferenceException::class)
    override fun validateReference() {
        if (protoIndex < 0 || protoIndex >= dexFile.protoSection.size) {
            throw InvalidReferenceException("proto@$protoIndex")
        }
    }
}
