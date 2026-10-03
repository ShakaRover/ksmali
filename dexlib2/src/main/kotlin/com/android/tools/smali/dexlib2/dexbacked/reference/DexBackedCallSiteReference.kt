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

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.reference.BaseCallSiteReference
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.util.EncodedArrayItemIterator
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.Reference.InvalidReferenceException
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.util.ExceptionWithContext
import java.util.ArrayList

class DexBackedCallSiteReference(
    @JvmField val dexFile: DexBackedDexFile,
    @JvmField val callSiteIndex: Int
) : BaseCallSiteReference() {
    @JvmField
    val callSiteIdOffset: Int = dexFile.callSiteSection.getOffset(callSiteIndex)

    private var callSiteOffset: Int = -1

    override val name: String
        get() = String.format("call_site_%d", callSiteIndex)

    override val methodHandle: MethodHandleReference
        get() {
            val iter = callSiteIterator
            if (iter.getItemCount() < 3) {
                throw ExceptionWithContext(
                    "Invalid call site item: must contain at least 3 entries."
                )
            }

            val encodedValue = callSiteIterator.getNextOrNull()
            assert(encodedValue != null)
            if (encodedValue!!.valueType != ValueType.METHOD_HANDLE) {
                throw ExceptionWithContext(
                    "Invalid encoded value type (%d) for the first item in call site %d",
                    encodedValue.valueType, callSiteIndex
                )
            }
            return (encodedValue as MethodHandleEncodedValue).value
        }

    override val methodName: String
        get() {
            val iter = callSiteIterator
            if (iter.getItemCount() < 3) {
                throw ExceptionWithContext(
                    "Invalid call site item: must contain at least 3 entries."
                )
            }

            iter.skipNext()
            val encodedValue = iter.getNextOrNull()
            assert(encodedValue != null)
            if (encodedValue!!.valueType != ValueType.STRING) {
                throw ExceptionWithContext(
                    "Invalid encoded value type (%d) for the second item in call site %d",
                    encodedValue.valueType, callSiteIndex
                )
            }
            return (encodedValue as StringEncodedValue).value
        }

    override val methodProto: MethodProtoReference
        get() {
            val iter = callSiteIterator
            if (iter.getItemCount() < 3) {
                throw ExceptionWithContext(
                    "Invalid call site item: must contain at least 3 entries."
                )
            }

            iter.skipNext()
            iter.skipNext()
            val encodedValue = iter.getNextOrNull()
            assert(encodedValue != null)
            if (encodedValue!!.valueType != ValueType.METHOD_TYPE) {
                throw ExceptionWithContext(
                    "Invalid encoded value type (%d) for the second item in call site %d",
                    encodedValue.valueType, callSiteIndex
                )
            }
            return (encodedValue as MethodTypeEncodedValue).value
        }

    override val extraArguments: List<EncodedValue>
        get() {
            val values = ArrayList<EncodedValue>()

            val iter = callSiteIterator
            if (iter.getItemCount() < 3) {
                throw ExceptionWithContext(
                    "Invalid call site item: must contain at least 3 entries."
                )
            }
            if (iter.getItemCount() == 3) {
                return values
            }

            iter.skipNext()
            iter.skipNext()
            iter.skipNext()

            var item = iter.getNextOrNull()
            while (item != null) {
                values.add(item)
                item = iter.getNextOrNull()
            }
            return values
        }

    private val callSiteIterator: EncodedArrayItemIterator
        get() = EncodedArrayItemIterator.newOrEmpty(dexFile, getCallSiteOffset())

    private fun getCallSiteOffset(): Int {
        if (callSiteOffset < 0) {
            callSiteOffset = dexFile.buffer.readSmallUint(callSiteIdOffset)
        }
        return callSiteOffset
    }

    @Throws(InvalidReferenceException::class)
    override fun validateReference() {
        if (callSiteIndex < 0 || callSiteIndex >= dexFile.callSiteSection.size) {
            throw InvalidReferenceException("callsite@$callSiteIndex")
        }
    }
}
