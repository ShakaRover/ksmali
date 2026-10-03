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

package com.android.tools.smali.dexlib2

import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.ExceptionWithContext

object ReferenceType {
    const val STRING = 0
    const val TYPE = 1
    const val FIELD = 2
    const val METHOD = 3
    const val METHOD_PROTO = 4
    const val CALL_SITE = 5
    const val METHOD_HANDLE = 6
    const val NONE = 7

    @JvmStatic
    fun getReferenceType(reference: Reference): Int {
        return when (reference) {
            is StringReference -> STRING
            is TypeReference -> TYPE
            is FieldReference -> FIELD
            is MethodReference -> METHOD
            is MethodProtoReference -> METHOD_PROTO
            is CallSiteReference -> CALL_SITE
            is MethodHandleReference -> METHOD_HANDLE
            else -> throw IllegalStateException("Invalid reference")
        }
    }

    /**
     * Validate a specific reference type. Note that the NONE placeholder is specifically not considered valid here.
     *
     * @throws InvalidReferenceTypeException
     */
    @JvmStatic
    fun validateReferenceType(referenceType: Int) {
        if (referenceType < 0 || referenceType > 4) {
            throw InvalidReferenceTypeException(referenceType)
        }
    }

    class InvalidReferenceTypeException : ExceptionWithContext {
        val referenceType: Int

        constructor(referenceType: Int) : super("Invalid reference type: %d", referenceType) {
            this.referenceType = referenceType
        }

        constructor(referenceType: Int, message: String, vararg formatArgs: Any?) :
            super(message, *formatArgs) {
            this.referenceType = referenceType
        }
    }
}
