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


package com.android.tools.smali.dexlib2.analysis.reflection.util

import java.util.Collections
import java.util.HashMap

object ReflectionUtils {
    private val primitiveMap: Map<String, String>

    init {
        val temp = HashMap<String, String>()
        temp["boolean"] = "Z"
        temp["int"] = "I"
        temp["long"] = "J"
        temp["double"] = "D"
        temp["void"] = "V"
        temp["float"] = "F"
        temp["char"] = "C"
        temp["short"] = "S"
        temp["byte"] = "B"
        primitiveMap = Collections.unmodifiableMap(temp)
    }

    private val primitiveMapInverse: Map<String, String> = getInverse()

    private fun getInverse(): Map<String, String> {
        val temp = HashMap<String, String>()
        for ((key, value) in primitiveMap) {
            temp[value] = key
        }
        return Collections.unmodifiableMap(temp)
    }

    fun javaToDexName(javaName: String): String {
        if (javaName[0] == '[') {
            return javaName.replace('.', '/')
        }

        val mapped = primitiveMap[javaName]
        if (mapped != null) {
            return mapped
        }

        return "L" + javaName.replace('.', '/') + ";"
    }

    fun dexToJavaName(dexName: String): String {
        if (dexName[0] == '[') {
            return dexName.replace('/', '.')
        }

        val mapped = primitiveMapInverse[dexName]
        if (mapped != null) {
            return mapped
        }

        return dexName.replace('/', '.').substring(1, dexName.length - 1)
    }
}
