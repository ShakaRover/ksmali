/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.MethodSection

class MethodPool(dexPool: DexPool) : BaseIndexPool<MethodReference>(dexPool),
    MethodSection<CharSequence, CharSequence, MethodProtoReference, MethodReference, PoolMethod> {

    fun intern(method: MethodReference) {
        val prev = internedItems.put(method, 0)
        if (prev == null) {
            dexPool.typeSection.intern(method.definingClass)
            dexPool.protoSection.intern(PoolMethodProto(method))
            dexPool.stringSection.intern(method.name)
        }
    }

    override fun getMethodReference(key: PoolMethod): MethodReference {
        return key
    }

    override fun getDefiningClass(key: MethodReference): CharSequence {
        return key.definingClass
    }

    override fun getPrototype(key: MethodReference): MethodProtoReference {
        return PoolMethodProto(key)
    }

    override fun getPrototype(key: PoolMethod): MethodProtoReference {
        return PoolMethodProto(key)
    }

    override fun getName(key: MethodReference): CharSequence {
        return key.name
    }

    override fun getMethodIndex(key: PoolMethod): Int {
        return getItemIndex(key)
    }
}
