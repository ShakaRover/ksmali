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


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.analysis.util.TypeProtoUtils
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.util.TypeUtils
import com.android.tools.smali.dexlib2.util.isPackagePrivate

object AnalyzedMethodUtil {
    fun canAccess(
        type: TypeProto,
        virtualMethod: Method,
        checkPackagePrivate: Boolean,
        checkProtected: Boolean,
        checkClass: Boolean
    ): Boolean {
        if (checkPackagePrivate && isPackagePrivate(virtualMethod)) {
            val otherPackage = TypeUtils.getPackage(virtualMethod.definingClass)
            val thisPackage = TypeUtils.getPackage(type.type)
            if (otherPackage != thisPackage) {
                return false
            }
        }

        if (checkProtected && (virtualMethod.accessFlags and AccessFlags.PROTECTED.value) != 0) {
            if (!TypeProtoUtils.extendsFrom(type, virtualMethod.definingClass)) {
                return false
            }
        }

        if (checkClass) {
            val classPath = type.classPath
            val methodClassDef = classPath.getClassDef(virtualMethod.definingClass)
            if (!TypeUtils.canAccessClass(type.type, methodClassDef!!)) {
                return false
            }
        }

        return true
    }
}
