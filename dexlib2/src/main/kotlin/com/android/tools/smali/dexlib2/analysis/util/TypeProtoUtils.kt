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


package com.android.tools.smali.dexlib2.analysis.util

import com.android.tools.smali.dexlib2.analysis.TypeProto
import com.android.tools.smali.dexlib2.analysis.UnresolvedClassException
import java.util.NoSuchElementException

object TypeProtoUtils {
    /**
     * Get the chain of superclasses of the given class. The first element will be the immediate superclass followed by
     * it's superclass, etc. up to java.lang.Object.
     *
     * Returns an empty iterable if called on java.lang.Object or a primitive.
     *
     * If any class in the superclass chain can't be resolved, the iterable will return Ujava/lang/Object; to represent
     * the unknown class.
     *
     * @return An iterable containing the superclasses of this class.
     */
    fun getSuperclassChain(typeProto: TypeProto): Iterable<TypeProto> {
        return object : Iterable<TypeProto> {
            override fun iterator(): Iterator<TypeProto> {
                return object : Iterator<TypeProto> {
                    private var type: TypeProto? = getSuperclassAsTypeProto(typeProto)

                    override fun hasNext(): Boolean = type != null

                    override fun next(): TypeProto {
                        val type = this.type
                        if (type == null) {
                            throw NoSuchElementException()
                        }

                        this.type = getSuperclassAsTypeProto(type)
                        return type
                    }
                }
            }
        }
    }

    fun getSuperclassAsTypeProto(type: TypeProto): TypeProto? {
        try {
            val next = type.superclass
            if (next != null) {
                return type.classPath.getClass(next)
            } else {
                return null
            }
        } catch (ex: UnresolvedClassException) {
            return type.classPath.getUnknownClass()
        }
    }

    fun extendsFrom(candidate: TypeProto, possibleSuper: String): Boolean {
        if (candidate.type == possibleSuper) {
            return true
        }
        for (superProto in getSuperclassChain(candidate)) {
            if (superProto.type == possibleSuper) {
                return true
            }
        }
        return false
    }
}
