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
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.util.TypeUtils
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.StringUtils

open class ArrayProto(override val classPath: ClassPath, type: String) : TypeProto {
    val dimensions: Int

    val elementType: String

    init {
        var i = 0
        while (type[i] == '[') {
            i++
            if (i == type.length) {
                throw ExceptionWithContext("Invalid array type: %s", type)
            }
        }

        if (i == 0) {
            throw ExceptionWithContext("Invalid array type: %s", type)
        }

        dimensions = i
        elementType = type.substring(i)
    }

    override fun toString(): String = type

    override val type: String
        get() = makeArrayType(elementType, dimensions)

    override fun isInterface(): Boolean = false

    /**
     * @return The base element type of this array. E.g. This would return Ljava/lang/String; for [[Ljava/lang/String;
     */
    val immediateElementType: String get() {
        if (dimensions > 1) {
            return makeArrayType(elementType, dimensions - 1)
        }
        return elementType
    }

    override fun implementsInterface(iface: String): Boolean {
        return iface == "Ljava/lang/Cloneable;" || iface == "Ljava/io/Serializable;"
    }

    override val superclass: String?
        get() = "Ljava/lang/Object;"

    override fun getCommonSuperclass(other: TypeProto): TypeProto {
        if (other is ArrayProto) {
            if (TypeUtils.isPrimitiveType(elementType) ||
                TypeUtils.isPrimitiveType(other.elementType)
            ) {
                if (dimensions == other.dimensions &&
                    elementType == other.elementType
                ) {
                    return this
                }
                return classPath.getClass("Ljava/lang/Object;")
            }

            if (dimensions == other.dimensions) {
                val thisClass = classPath.getClass(elementType)
                val otherClass = classPath.getClass(other.elementType)
                val mergedClass = thisClass.getCommonSuperclass(otherClass)
                if (thisClass === mergedClass) {
                    return this
                }
                if (otherClass === mergedClass) {
                    return other
                }
                return classPath.getClass(makeArrayType(mergedClass.type, dimensions))
            }

            val dimensions = Math.min(this.dimensions, other.dimensions)
            return classPath.getClass(makeArrayType("Ljava/lang/Object;", dimensions))
        }

        if (other is ClassProto) {
            try {
                if (other.isInterface()) {
                    if (implementsInterface(other.type)) {
                        return other
                    }
                }
            } catch (ex: UnresolvedClassException) {
                // ignore
            }
            return classPath.getClass("Ljava/lang/Object;")
        }

        // otherwise, defer to the other class' getCommonSuperclass
        return other.getCommonSuperclass(this)
    }

    override fun getFieldByOffset(fieldOffset: Int): FieldReference? {
        if (fieldOffset == 8) {
            return ImmutableFieldReference(type, "length", "int")
        }
        return null
    }

    override fun getMethodByVtableIndex(vtableIndex: Int): Method? {
        return classPath.getClass("Ljava/lang/Object;").getMethodByVtableIndex(vtableIndex)
    }

    override fun findMethodIndexInVtable(method: MethodReference): Int {
        return classPath.getClass("Ljava/lang/Object;").findMethodIndexInVtable(method)
    }

    companion object {
        private val BRACKETS = StringUtils.repeat("[", 256)

        private fun makeArrayType(elementType: String, dimensions: Int): String {
            return BRACKETS.substring(0, dimensions) + elementType
        }
    }
}
