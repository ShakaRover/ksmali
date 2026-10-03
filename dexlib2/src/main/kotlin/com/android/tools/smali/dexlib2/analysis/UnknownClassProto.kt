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

import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

open class UnknownClassProto(override val classPath: ClassPath) : TypeProto {
    override fun toString(): String = "Ujava/lang/Object;"

    override val superclass: String?
        get() = null

    override fun isInterface(): Boolean = false

    override fun implementsInterface(iface: String): Boolean = false

    override fun getCommonSuperclass(other: TypeProto): TypeProto {
        if (other.type == "Ljava/lang/Object;") {
            return other
        }
        if (other is ArrayProto) {
            // if it's an array class, it's safe to assume this unknown class isn't related, and so
            // java.lang.Object is the only possible superclass
            return classPath.getClass("Ljava/lang/Object;")
        }
        return this
    }

    override val type: String
        // use the otherwise used U prefix for an unknown/unresolvable class
        get() = "Ujava/lang/Object;"

    override fun getFieldByOffset(fieldOffset: Int): FieldReference? {
        return classPath.getClass("Ljava/lang/Object;").getFieldByOffset(fieldOffset)
    }

    override fun getMethodByVtableIndex(vtableIndex: Int): Method? {
        return classPath.getClass("Ljava/lang/Object;").getMethodByVtableIndex(vtableIndex)
    }

    override fun findMethodIndexInVtable(method: MethodReference): Int {
        return classPath.getClass("Ljava/lang/Object;").findMethodIndexInVtable(method)
    }
}
