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

package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.util.IteratorUtils
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

class SyntheticAccessorResolver(
    opcodes: Opcodes,
    classDefs: Iterable<@JvmWildcard ClassDef>
) {
    private val syntheticAccessorFSM: SyntheticAccessorFSM = SyntheticAccessorFSM(opcodes)
    private val classDefMap: Map<String, ClassDef>
    private val resolvedAccessors: MutableMap<MethodReference, AccessedMember> = ConcurrentHashMap()

    init {
        val map = HashMap<String, ClassDef>()

        for (classDef in classDefs) {
            map[classDef.type] = classDef
        }

        classDefMap = Collections.unmodifiableMap(map)
    }

    fun getAccessedMember(methodReference: MethodReference): AccessedMember? {
        val accessedMember = resolvedAccessors[methodReference]
        if (accessedMember != null) {
            return accessedMember
        }

        val type = methodReference.definingClass
        val classDef = classDefMap[type] ?: return null

        var matchedMethod: Method? = null
        var matchedMethodImpl: MethodImplementation? = null
        for (method in classDef.methods) {
            val methodImpl = method.implementation
            if (methodImpl != null) {
                if (methodReferenceEquals(method, methodReference)) {
                    matchedMethod = method
                    matchedMethodImpl = methodImpl
                    break
                }
            }
        }

        if (matchedMethod == null) {
            return null
        }

        //A synthetic accessor will be marked synthetic
        if (!AccessFlags.SYNTHETIC.isSet(matchedMethod.accessFlags)) {
            return null
        }

        val instructions: List<Instruction> = Collections.unmodifiableList(
            IteratorUtils.toList(matchedMethodImpl!!.instructions)
        )

        val accessType = syntheticAccessorFSM.test(instructions)

        if (accessType >= 0) {
            val member = AccessedMember(
                accessType, (instructions[0] as ReferenceInstruction).reference
            )
            resolvedAccessors[methodReference] = member
            return member
        }
        return null
    }

    class AccessedMember(
        val accessedMemberType: Int,
        val accessedMember: Reference
    )

    private fun methodReferenceEquals(ref1: MethodReference, ref2: MethodReference): Boolean {
        // we already know the containing class matches
        return ref1.name == ref2.name &&
                ref1.returnType == ref2.returnType &&
                ref1.parameterTypes == ref2.parameterTypes
    }

    companion object {
        const val METHOD = 0
        const val GETTER = 1
        const val SETTER = 2
        const val POSTFIX_INCREMENT = 3
        const val PREFIX_INCREMENT = 4
        const val POSTFIX_DECREMENT = 5
        const val PREFIX_DECREMENT = 6
        const val ADD_ASSIGNMENT = 7
        const val SUB_ASSIGNMENT = 8
        const val MUL_ASSIGNMENT = 9
        const val DIV_ASSIGNMENT = 10
        const val REM_ASSIGNMENT = 11
        const val AND_ASSIGNMENT = 12
        const val OR_ASSIGNMENT = 13
        const val XOR_ASSIGNMENT = 14
        const val SHL_ASSIGNMENT = 15
        const val SHR_ASSIGNMENT = 16
        const val USHR_ASSIGNMENT = 17

        fun looksLikeSyntheticAccessor(methodName: String): Boolean {
            return methodName.startsWith("access\$")
        }
    }
}
