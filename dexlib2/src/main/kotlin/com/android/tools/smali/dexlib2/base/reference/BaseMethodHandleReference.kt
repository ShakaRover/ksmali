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

package com.android.tools.smali.dexlib2.base.reference

import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

abstract class BaseMethodHandleReference : BaseReference(), MethodHandleReference {
    override fun hashCode(): Int {
        var hashCode = methodHandleType
        hashCode = hashCode * 31 + memberReference.hashCode()
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other != null && other is MethodHandleReference) {
            return methodHandleType == other.methodHandleType &&
                    memberReference == other.memberReference
        }
        return false
    }

    override fun compareTo(other: MethodHandleReference): Int {
        val res = methodHandleType.compareTo(other.methodHandleType)
        if (res != 0) return res

        val reference = memberReference
        if (reference is FieldReference) {
            // "This should never happen", but if it does, we'll arbitrarily say a field reference compares less than
            // a method reference
            if (other.memberReference !is FieldReference) {
                return -1
            }
            return reference.compareTo(other.memberReference as FieldReference)
        } else {
            if (other.memberReference !is MethodReference) {
                return 1
            }
            return (reference as MethodReference).compareTo(other.memberReference as MethodReference)
        }
    }

    override fun toString(): String {
        return DexFormatter.INSTANCE.getMethodHandle(this)
    }
}
