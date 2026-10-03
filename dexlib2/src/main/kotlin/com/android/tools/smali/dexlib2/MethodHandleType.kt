/*
 * Copyright 2018, Google LLC
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

import com.android.tools.smali.util.ExceptionWithContext
import java.util.Collections
import java.util.HashMap

object MethodHandleType {
    const val STATIC_PUT = 0
    const val STATIC_GET = 1
    const val INSTANCE_PUT = 2
    const val INSTANCE_GET = 3
    const val INVOKE_STATIC = 4
    const val INVOKE_INSTANCE = 5
    const val INVOKE_CONSTRUCTOR = 6
    const val INVOKE_DIRECT = 7
    const val INVOKE_INTERFACE = 8

    private val methodHandleTypeNames: Map<Int, String>
    private val inverse: Map<String, Int>

    init {
        val temp = HashMap<Int, String>()
        temp[STATIC_PUT] = "static-put"
        temp[STATIC_GET] = "static-get"
        temp[INSTANCE_PUT] = "instance-put"
        temp[INSTANCE_GET] = "instance-get"
        temp[INVOKE_STATIC] = "invoke-static"
        temp[INVOKE_INSTANCE] = "invoke-instance"
        temp[INVOKE_CONSTRUCTOR] = "invoke-constructor"
        temp[INVOKE_DIRECT] = "invoke-direct"
        temp[INVOKE_INTERFACE] = "invoke-interface"
        methodHandleTypeNames = Collections.unmodifiableMap(temp)

        val namesToTypes = HashMap<String, Int>()
        for (entry in methodHandleTypeNames.entries) {
            namesToTypes[entry.value] = entry.key
        }
        inverse = Collections.unmodifiableMap(namesToTypes)
    }

    @JvmStatic
    fun toString(methodHandleType: Int): String {
        val v = methodHandleTypeNames[methodHandleType]
            ?: throw InvalidMethodHandleTypeException(methodHandleType)
        return v
    }

    @JvmStatic
    fun getMethodHandleType(methodHandleType: String): Int {
        return inverse[methodHandleType]
            ?: throw ExceptionWithContext("Invalid method handle type: %s", methodHandleType)
    }

    class InvalidMethodHandleTypeException : ExceptionWithContext {
        val methodHandleType: Int

        constructor(methodHandleType: Int) : super("Invalid method handle type: %d", methodHandleType) {
            this.methodHandleType = methodHandleType
        }

        constructor(methodHandleType: Int, message: String, vararg formatArgs: Any?) :
            super(message, *formatArgs) {
            this.methodHandleType = methodHandleType
        }
    }
}
