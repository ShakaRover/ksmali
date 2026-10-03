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

package com.android.tools.smali.dexlib2.immutable.reference

import com.android.tools.smali.dexlib2.base.reference.BaseCallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableEncodedValueFactory
import com.android.tools.smali.util.ImmutableUtils

open class ImmutableCallSiteReference(
    override val name: String,
    methodHandle: MethodHandleReference,
    override val methodName: String,
    methodProto: MethodProtoReference,
    extraArguments: Iterable<EncodedValue>
) : BaseCallSiteReference(), ImmutableReference {
    override val methodHandle: MethodHandleReference = ImmutableMethodHandleReference.of(methodHandle)
    override val methodProto: MethodProtoReference = ImmutableMethodProtoReference.of(methodProto)
    override val extraArguments: List<EncodedValue> = ImmutableEncodedValueFactory.immutableListOf(extraArguments)

    constructor(
        name: String,
        methodHandle: ImmutableMethodHandleReference,
        methodName: String,
        methodProto: ImmutableMethodProtoReference,
        extraArguments: List<ImmutableEncodedValue>?
    ) : this(
        name,
        methodHandle as MethodHandleReference,
        methodName,
        methodProto as MethodProtoReference,
        ImmutableUtils.nullToEmptyList(extraArguments) as Iterable<EncodedValue>
    )

    companion object {
        fun of(callSiteReference: CallSiteReference): ImmutableCallSiteReference {
            if (callSiteReference is ImmutableCallSiteReference) {
                return callSiteReference
            }
            return ImmutableCallSiteReference(
                callSiteReference.name,
                callSiteReference.methodHandle,
                callSiteReference.methodName,
                callSiteReference.methodProto,
                ImmutableEncodedValueFactory.immutableListOf(callSiteReference.extraArguments)
            )
        }
    }
}
