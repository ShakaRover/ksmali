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

package com.android.tools.smali.dexlib2.base

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.Comparator

abstract class BaseExceptionHandler : ExceptionHandler {
    override val exceptionTypeReference: TypeReference?
        get() {
            val exceptionType = exceptionType ?: return null

            return object : BaseTypeReference() {
                override val type: String
                    get() = exceptionType
            }
        }

    override fun hashCode(): Int {
        val exceptionType = exceptionType
        val hashCode = exceptionType?.hashCode() ?: 0
        return hashCode * 31 + handlerCodeAddress
    }

    override fun equals(o: Any?): Boolean {
        if (o is ExceptionHandler) {
            return exceptionType == o.exceptionType &&
                    handlerCodeAddress == o.handlerCodeAddress
        }
        return false
    }

    override fun compareTo(o: ExceptionHandler): Int {
        val exceptionType = exceptionType
        if (exceptionType == null) {
            if (o.exceptionType != null) {
                return 1
            }
        } else {
            val otherExceptionType = o.exceptionType
            if (otherExceptionType == null) {
                return -1
            }
            val res = exceptionType.compareTo(otherExceptionType)
            if (res != 0) return res
        }
        return handlerCodeAddress.compareTo(o.handlerCodeAddress)
    }

    companion object {
        @JvmField
        val BY_EXCEPTION: Comparator<ExceptionHandler> = Comparator { o1, o2 ->
            val exceptionType1 = o1.exceptionType
            if (exceptionType1 == null) {
                if (o2.exceptionType != null) 1 else 0
            } else {
                val exceptionType2 = o2.exceptionType
                if (exceptionType2 == null) -1 else exceptionType1.compareTo(exceptionType2)
            }
        }
    }
}
