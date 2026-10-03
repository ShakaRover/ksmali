/*
 * Copyright 2014, Google LLC
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

package com.android.tools.smali.dexlib2.rewriter

import com.android.tools.smali.dexlib2.base.reference.BaseCallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue

open class CallSiteReferenceRewriter(
    protected val rewriters: Rewriters
) : Rewriter<CallSiteReference> {
    override fun rewrite(value: CallSiteReference): CallSiteReference {
        return RewrittenCallSiteReference(value)
    }

    protected inner class RewrittenCallSiteReference(
        protected var callSiteReference: CallSiteReference
    ) : BaseCallSiteReference() {
        override val name: String
            get() = callSiteReference.name

        override val methodHandle: MethodHandleReference
            get() = RewriterUtils.rewriteMethodHandleReference(
                rewriters,
                callSiteReference.methodHandle
            )

        override val methodName: String
            get() = callSiteReference.methodName

        override val methodProto: MethodProtoReference
            get() = RewriterUtils.rewriteMethodProtoReference(
                rewriters.typeRewriter,
                callSiteReference.methodProto
            )

        override val extraArguments: List<EncodedValue>
            get() = callSiteReference.extraArguments.map {
                RewriterUtils.rewriteValue(rewriters, it)
            }
    }
}
