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

import com.android.tools.smali.dexlib2.Opcode
import java.util.HashMap

open class OdexedFieldInstructionMapper(isArt: Boolean) {
    private class FieldOpcode(
        val type: Char,
        val isStatic: Boolean,
        val normalOpcode: Opcode,
        val quickOpcode: Opcode?,
        val volatileOpcode: Opcode?
    ) {
        constructor(type: Char, normalOpcode: Opcode, quickOpcode: Opcode?, volatileOpcode: Opcode?) :
            this(type, false, normalOpcode, quickOpcode, volatileOpcode)

        constructor(type: Char, isStatic: Boolean, normalOpcode: Opcode, volatileOpcode: Opcode?) :
            this(type, isStatic, normalOpcode, null, volatileOpcode)

        constructor(type: Char, normalOpcode: Opcode, quickOpcode: Opcode?) :
            this(type, false, normalOpcode, quickOpcode, null)
    }

    private val opcodeMap = Array(2) { Array(2) { arrayOfNulls<FieldOpcode>(10) } }

    private val opcodeValueTypeMap: MutableMap<Opcode, Int> = HashMap(30)

    init {
        val opcodes = if (isArt) artFieldOpcodes else dalvikFieldOpcodes

        for (fieldOpcode in opcodes) {
            val getOrPut = if (isGet(fieldOpcode.normalOpcode)) GET else PUT
            val staticOrInstance = if (isStatic(fieldOpcode.normalOpcode)) STATIC else INSTANCE
            opcodeMap[getOrPut][staticOrInstance][getTypeIndex(fieldOpcode.type)] = fieldOpcode

            val quickOpcode = fieldOpcode.quickOpcode
            if (quickOpcode != null) {
                opcodeValueTypeMap[quickOpcode] = getValueType(fieldOpcode.type)
            }
            val volatileOpcode = fieldOpcode.volatileOpcode
            if (volatileOpcode != null) {
                opcodeValueTypeMap[volatileOpcode] = getValueType(fieldOpcode.type)
            }
        }
    }

    fun getAndCheckDeodexedOpcode(fieldType: String, odexedOpcode: Opcode): Opcode {
        val getOrPut = if (isGet(odexedOpcode)) GET else PUT
        val staticOrInstance = if (isStatic(odexedOpcode)) STATIC else INSTANCE
        val fieldOpcode = opcodeMap[getOrPut][staticOrInstance][getTypeIndex(fieldType[0])]!!

        if (!isCompatible(odexedOpcode, fieldOpcode.type)) {
            throw AnalysisException(
                "Incorrect field type \"${fieldType}\" for ${odexedOpcode.mnemonic}"
            )
        }

        return fieldOpcode.normalOpcode
    }

    private fun isCompatible(opcode: Opcode, type: Char): Boolean {
        val valueType = opcodeValueTypeMap[opcode] ?: throw RuntimeException("Unexpected opcode: " + opcode.mnemonic)
        return valueType == getValueType(type)
    }

    companion object {
        private const val GET = 0
        private const val PUT = 1

        private const val INSTANCE = 0
        private const val STATIC = 1

        private const val PRIMITIVE = 0
        private const val WIDE = 1
        private const val REFERENCE = 2

        private val dalvikFieldOpcodes = arrayOf(
            FieldOpcode('Z', Opcode.IGET_BOOLEAN, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('B', Opcode.IGET_BYTE, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('S', Opcode.IGET_SHORT, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('C', Opcode.IGET_CHAR, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('I', Opcode.IGET, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('F', Opcode.IGET, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            FieldOpcode('J', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK, Opcode.IGET_WIDE_VOLATILE),
            FieldOpcode('D', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK, Opcode.IGET_WIDE_VOLATILE),
            FieldOpcode('L', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK, Opcode.IGET_OBJECT_VOLATILE),
            FieldOpcode('[', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK, Opcode.IGET_OBJECT_VOLATILE),

            FieldOpcode('Z', Opcode.IPUT_BOOLEAN, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('B', Opcode.IPUT_BYTE, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('S', Opcode.IPUT_SHORT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('C', Opcode.IPUT_CHAR, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('I', Opcode.IPUT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('F', Opcode.IPUT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            FieldOpcode('J', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK, Opcode.IPUT_WIDE_VOLATILE),
            FieldOpcode('D', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK, Opcode.IPUT_WIDE_VOLATILE),
            FieldOpcode('L', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK, Opcode.IPUT_OBJECT_VOLATILE),
            FieldOpcode('[', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK, Opcode.IPUT_OBJECT_VOLATILE),

            FieldOpcode('Z', true, Opcode.SPUT_BOOLEAN, Opcode.SPUT_VOLATILE),
            FieldOpcode('B', true, Opcode.SPUT_BYTE, Opcode.SPUT_VOLATILE),
            FieldOpcode('S', true, Opcode.SPUT_SHORT, Opcode.SPUT_VOLATILE),
            FieldOpcode('C', true, Opcode.SPUT_CHAR, Opcode.SPUT_VOLATILE),
            FieldOpcode('I', true, Opcode.SPUT, Opcode.SPUT_VOLATILE),
            FieldOpcode('F', true, Opcode.SPUT, Opcode.SPUT_VOLATILE),
            FieldOpcode('J', true, Opcode.SPUT_WIDE, Opcode.SPUT_WIDE_VOLATILE),
            FieldOpcode('D', true, Opcode.SPUT_WIDE, Opcode.SPUT_WIDE_VOLATILE),
            FieldOpcode('L', true, Opcode.SPUT_OBJECT, Opcode.SPUT_OBJECT_VOLATILE),
            FieldOpcode('[', true, Opcode.SPUT_OBJECT, Opcode.SPUT_OBJECT_VOLATILE),

            FieldOpcode('Z', true, Opcode.SGET_BOOLEAN, Opcode.SGET_VOLATILE),
            FieldOpcode('B', true, Opcode.SGET_BYTE, Opcode.SGET_VOLATILE),
            FieldOpcode('S', true, Opcode.SGET_SHORT, Opcode.SGET_VOLATILE),
            FieldOpcode('C', true, Opcode.SGET_CHAR, Opcode.SGET_VOLATILE),
            FieldOpcode('I', true, Opcode.SGET, Opcode.SGET_VOLATILE),
            FieldOpcode('F', true, Opcode.SGET, Opcode.SGET_VOLATILE),
            FieldOpcode('J', true, Opcode.SGET_WIDE, Opcode.SGET_WIDE_VOLATILE),
            FieldOpcode('D', true, Opcode.SGET_WIDE, Opcode.SGET_WIDE_VOLATILE),
            FieldOpcode('L', true, Opcode.SGET_OBJECT, Opcode.SGET_OBJECT_VOLATILE),
            FieldOpcode('[', true, Opcode.SGET_OBJECT, Opcode.SGET_OBJECT_VOLATILE)
        )

        private val artFieldOpcodes = arrayOf(
            FieldOpcode('Z', Opcode.IGET_BOOLEAN, Opcode.IGET_BOOLEAN_QUICK),
            FieldOpcode('B', Opcode.IGET_BYTE, Opcode.IGET_BYTE_QUICK),
            FieldOpcode('S', Opcode.IGET_SHORT, Opcode.IGET_SHORT_QUICK),
            FieldOpcode('C', Opcode.IGET_CHAR, Opcode.IGET_CHAR_QUICK),
            FieldOpcode('I', Opcode.IGET, Opcode.IGET_QUICK),
            FieldOpcode('F', Opcode.IGET, Opcode.IGET_QUICK),
            FieldOpcode('J', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK),
            FieldOpcode('D', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK),
            FieldOpcode('L', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK),
            FieldOpcode('[', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK),

            FieldOpcode('Z', Opcode.IPUT_BOOLEAN, Opcode.IPUT_BOOLEAN_QUICK),
            FieldOpcode('B', Opcode.IPUT_BYTE, Opcode.IPUT_BYTE_QUICK),
            FieldOpcode('S', Opcode.IPUT_SHORT, Opcode.IPUT_SHORT_QUICK),
            FieldOpcode('C', Opcode.IPUT_CHAR, Opcode.IPUT_CHAR_QUICK),
            FieldOpcode('I', Opcode.IPUT, Opcode.IPUT_QUICK),
            FieldOpcode('F', Opcode.IPUT, Opcode.IPUT_QUICK),
            FieldOpcode('J', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK),
            FieldOpcode('D', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK),
            FieldOpcode('L', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK),
            FieldOpcode('[', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK)
        )

        private fun getValueType(type: Char): Int {
            return when (type) {
                'Z', 'B', 'S', 'C', 'I', 'F' -> PRIMITIVE
                'J', 'D' -> WIDE
                'L', '[' -> REFERENCE
                else -> throw RuntimeException("Unknown type ${type}: ")
            }
        }

        private fun getTypeIndex(type: Char): Int {
            return when (type) {
                'Z' -> 0
                'B' -> 1
                'S' -> 2
                'C' -> 3
                'I' -> 4
                'F' -> 5
                'J' -> 6
                'D' -> 7
                'L' -> 8
                '[' -> 9
                else -> throw RuntimeException("Unknown type ${type}: ")
            }
        }

        private fun isGet(opcode: Opcode): Boolean {
            return (opcode.flags and Opcode.SETS_REGISTER) != 0
        }

        private fun isStatic(opcode: Opcode): Boolean {
            return (opcode.flags and Opcode.STATIC_FIELD_ACCESSOR) != 0
        }
    }
}
