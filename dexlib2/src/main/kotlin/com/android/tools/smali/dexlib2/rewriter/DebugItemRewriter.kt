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

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.LocalInfo
import com.android.tools.smali.dexlib2.iface.debug.RestartLocal
import com.android.tools.smali.dexlib2.iface.debug.StartLocal
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

open class DebugItemRewriter(
    @JvmField protected val rewriters: Rewriters
) : Rewriter<DebugItem> {
    override fun rewrite(value: DebugItem): DebugItem {
        when (value.debugItemType) {
            DebugItemType.START_LOCAL -> return RewrittenStartLocal(value as StartLocal)
            DebugItemType.END_LOCAL -> return RewrittenEndLocal(value as EndLocal)
            DebugItemType.RESTART_LOCAL -> return RewrittenRestartLocal(value as RestartLocal)
            else -> return value
        }
    }

    protected open inner class BaseRewrittenLocalInfoDebugItem<T>(
        protected var debugItem: T
    ) : DebugItem, LocalInfo where T : DebugItem, T : LocalInfo {
        override val debugItemType: Int
            get() = debugItem.debugItemType

        override val codeAddress: Int
            get() = debugItem.codeAddress

        override val name: String?
            get() = debugItem.name

        override val type: String?
            get() = RewriterUtils.rewriteNullable(rewriters.typeRewriter, debugItem.type)

        override val signature: String?
            get() = debugItem.signature
    }

    protected inner class RewrittenStartLocal(
        debugItem: StartLocal
    ) : BaseRewrittenLocalInfoDebugItem<StartLocal>(debugItem), StartLocal {
        override val register: Int
            get() = debugItem.register

        override val nameReference: StringReference?
            get() = debugItem.nameReference

        override val typeReference: TypeReference?
            get() {
                val typeReference = debugItem.typeReference ?: return null
                return RewriterUtils.rewriteTypeReference(rewriters.typeRewriter, typeReference)
            }

        override val signatureReference: StringReference?
            get() = debugItem.signatureReference
    }

    protected inner class RewrittenEndLocal(
        debugItem: EndLocal
    ) : BaseRewrittenLocalInfoDebugItem<EndLocal>(debugItem), EndLocal {
        override val register: Int
            get() = debugItem.register
    }

    protected inner class RewrittenRestartLocal(
        debugItem: RestartLocal
    ) : BaseRewrittenLocalInfoDebugItem<RestartLocal>(debugItem), RestartLocal {
        override val register: Int
            get() = debugItem.register
    }
}
