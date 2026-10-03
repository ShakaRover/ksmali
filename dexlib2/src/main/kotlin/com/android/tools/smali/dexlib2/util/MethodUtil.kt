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

@file:JvmName("MethodUtil")

package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.util.CharSequenceUtils
import java.util.function.Predicate

private val directMask = AccessFlags.STATIC.value or AccessFlags.PRIVATE.value or
        AccessFlags.CONSTRUCTOR.value

@JvmField
val METHOD_IS_DIRECT: Predicate<Method> = Predicate { input -> input != null && isDirect(input) }

@JvmField
val METHOD_IS_VIRTUAL: Predicate<Method> = Predicate { input -> input != null && !isDirect(input) }

fun isDirect(method: Method): Boolean {
    return (method.accessFlags and directMask) != 0
}

fun isStatic(method: Method): Boolean {
    return AccessFlags.STATIC.isSet(method.accessFlags)
}

fun isConstructor(methodReference: MethodReference): Boolean {
    return methodReference.name == "<init>"
}

fun isPackagePrivate(method: Method): Boolean {
    return (method.accessFlags and (AccessFlags.PRIVATE.value or
            AccessFlags.PROTECTED.value or
            AccessFlags.PUBLIC.value)) == 0
}

fun getParameterRegisterCount(method: Method): Int {
    return getParameterRegisterCount(method, isStatic(method))
}

fun getParameterRegisterCount(methodRef: MethodReference, isStatic: Boolean): Int {
    return getParameterRegisterCount(methodRef.parameterTypes, isStatic)
}

fun getParameterRegisterCount(
    parameterTypes: Collection<@JvmWildcard CharSequence>,
    isStatic: Boolean
): Int {
    var regCount = 0
    for (paramType in parameterTypes) {
        val firstChar = paramType[0]
        if (firstChar == 'J' || firstChar == 'D') {
            regCount += 2
        } else {
            regCount++
        }
    }
    if (!isStatic) {
        regCount++
    }
    return regCount
}

private fun getShortyType(type: CharSequence): Char {
    if (type.length > 1) {
        return 'L'
    }
    return type[0]
}

fun getShorty(params: Collection<@JvmWildcard CharSequence>, returnType: String): String {
    val sb = StringBuilder(params.size + 1)
    sb.append(getShortyType(returnType))
    for (typeRef in params) {
        sb.append(getShortyType(typeRef))
    }
    return sb.toString()
}

fun methodSignaturesMatch(a: MethodReference, b: MethodReference): Boolean {
    return (a.name == b.name &&
            a.returnType == b.returnType &&
            CharSequenceUtils.listEquals(a.parameterTypes, b.parameterTypes))
}
