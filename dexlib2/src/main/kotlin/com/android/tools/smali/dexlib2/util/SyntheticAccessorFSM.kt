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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

class SyntheticAccessorFSM(private val opcodes: Opcodes) {

    fun test(instructions: List<@JvmWildcard Instruction>): Int {
        var accessorType = -1
        var cs: Int
        var p = 0
        val pe = instructions.size

        // one of the math type constants representing the type of math operation being performed
        var mathOp = -1

        // for increments an decrements, the type of value the math operation is on
        var mathType = -1

        // for increments and decrements, the value of the constant that is used
        var constantValue = 0L

        // The source register for the put instruction
        var putRegister = -1
        // The return register;
        var returnRegister = -1

        cs = SyntheticAccessorFSM_start

        var _klen: Int
        var _trans = 0
        var _acts: Int
        var _nacts: Int
        var _keys: Int
        var _goto_targ = 0

        gotoLoop@ while (true) {
            if (_goto_targ == 4 || _goto_targ == 5) {
                break@gotoLoop
            }
            if (_goto_targ == 0) {
                if (p == pe) {
                    _goto_targ = 4
                    continue@gotoLoop
                }
                if (cs == 0) {
                    _goto_targ = 5
                    continue@gotoLoop
                }
            }

            _match@ do {
                _keys = _SyntheticAccessorFSM_key_offsets[cs]
                _trans = _SyntheticAccessorFSM_index_offsets[cs]
                _klen = _SyntheticAccessorFSM_single_lengths[cs]
                if (_klen > 0) {
                    var _lower = _keys
                    var _upper = _keys + _klen - 1
                    while (true) {
                        if (_upper < _lower) {
                            break
                        }

                        val _mid = _lower + ((_upper - _lower) shr 1)
                        val opcodeValue = opcodes.getOpcodeValue(instructions[p].opcode)!!.toInt()
                        if (opcodeValue < _SyntheticAccessorFSM_trans_keys[_mid]) {
                            _upper = _mid - 1
                        } else if (opcodeValue > _SyntheticAccessorFSM_trans_keys[_mid]) {
                            _lower = _mid + 1
                        } else {
                            _trans += (_mid - _keys)
                            break@_match
                        }
                    }
                    _keys += _klen
                    _trans += _klen
                }

                _klen = _SyntheticAccessorFSM_range_lengths[cs]
                if (_klen > 0) {
                    var _lower = _keys
                    var _upper = _keys + (_klen shl 1) - 2
                    while (true) {
                        if (_upper < _lower) {
                            break
                        }

                        val _mid = _lower + (((_upper - _lower) shr 1) and 1.inv())
                        val opcodeValue = opcodes.getOpcodeValue(instructions[p].opcode)!!.toInt()
                        if (opcodeValue < _SyntheticAccessorFSM_trans_keys[_mid]) {
                            _upper = _mid - 2
                        } else if (opcodeValue > _SyntheticAccessorFSM_trans_keys[_mid + 1]) {
                            _lower = _mid + 2
                        } else {
                            _trans += ((_mid - _keys) shr 1)
                            break@_match
                        }
                    }
                    _trans += _klen
                }
            } while (false)

            _trans = _SyntheticAccessorFSM_indicies[_trans]
            cs = _SyntheticAccessorFSM_trans_targs[_trans]

            if (_SyntheticAccessorFSM_trans_actions[_trans] != 0) {
                _acts = _SyntheticAccessorFSM_trans_actions[_trans]
                _nacts = _SyntheticAccessorFSM_actions[_acts++]
                while (_nacts-- > 0) {
                    when (_SyntheticAccessorFSM_actions[_acts++]) {
                        0 -> {
                            putRegister = (instructions[p] as OneRegisterInstruction).registerA
                        }
                        1 -> {
                            constantValue = (instructions[p] as WideLiteralInstruction).wideLiteral
                        }
                        2 -> {
                            mathType = INT
                            mathOp = ADD
                            constantValue = (instructions[p] as WideLiteralInstruction).wideLiteral
                        }
                        3 -> {
                            mathType = INT
                        }
                        4 -> {
                            mathType = LONG
                        }
                        5 -> {
                            mathType = FLOAT
                        }
                        6 -> {
                            mathType = DOUBLE
                        }
                        7 -> {
                            mathOp = ADD
                        }
                        8 -> {
                            mathType = INT
                        }
                        9 -> {
                            mathType = LONG
                        }
                        10 -> {
                            mathType = FLOAT
                        }
                        11 -> {
                            mathType = DOUBLE
                        }
                        12 -> {
                            mathOp = SUB
                        }
                        13 -> {
                            mathOp = MUL
                        }
                        14 -> {
                            mathOp = DIV
                        }
                        15 -> {
                            mathOp = REM
                        }
                        16 -> {
                            mathOp = AND
                        }
                        17 -> {
                            mathOp = OR
                        }
                        18 -> {
                            mathOp = XOR
                        }
                        19 -> {
                            mathOp = SHL
                        }
                        20 -> {
                            mathOp = SHR
                        }
                        21 -> {
                            mathOp = USHR
                        }
                        22 -> {
                            returnRegister = (instructions[p] as OneRegisterInstruction).registerA
                        }
                        23 -> {
                            accessorType = SyntheticAccessorResolver.GETTER
                            p += 1
                            _goto_targ = 5
                            continue@gotoLoop
                        }
                        24 -> {
                            accessorType = SyntheticAccessorResolver.SETTER
                            p += 1
                            _goto_targ = 5
                            continue@gotoLoop
                        }
                        25 -> {
                            accessorType = SyntheticAccessorResolver.METHOD
                            p += 1
                            _goto_targ = 5
                            continue@gotoLoop
                        }
                        26 -> {
                            accessorType = getIncrementType(
                                mathOp, mathType, constantValue, putRegister, returnRegister
                            )
                        }
                        27 -> {
                            accessorType = getIncrementType(
                                mathOp, mathType, constantValue, putRegister, returnRegister
                            )
                        }
                        28 -> {
                            accessorType = mathOp
                            p += 1
                            _goto_targ = 5
                            continue@gotoLoop
                        }
                    }
                }
            }

            if (cs == 0) {
                _goto_targ = 5
                continue@gotoLoop
            }
            if (++p != pe) {
                _goto_targ = 1
                continue@gotoLoop
            }
            break@gotoLoop
        }

        return accessorType
    }

    private fun getIncrementType(
        mathOp: Int,
        mathType: Int,
        constantValue: Long,
        putRegister: Int,
        returnRegister: Int
    ): Int {
        val isPrefix = putRegister == returnRegister
        var negativeConstant = false

        when (mathType) {
            INT, LONG -> {
                if (constantValue == 1L) {
                    negativeConstant = false
                } else if (constantValue == -1L) {
                    negativeConstant = true
                } else {
                    return -1
                }
            }
            FLOAT -> {
                val value = java.lang.Float.intBitsToFloat(constantValue.toInt())
                if (value == 1f) {
                    negativeConstant = false
                } else if (value == -1f) {
                    negativeConstant = true
                } else {
                    return -1
                }
            }
            DOUBLE -> {
                val value = java.lang.Double.longBitsToDouble(constantValue)
                if (value == 1.0) {
                    negativeConstant = false
                } else if (value == -1.0) {
                    negativeConstant = true
                } else {
                    return -1
                }
            }
        }

        val isAdd = ((mathOp == ADD) && !negativeConstant) ||
            ((mathOp == SUB) && negativeConstant)

        return if (isPrefix) {
            if (isAdd) {
                SyntheticAccessorResolver.PREFIX_INCREMENT
            } else {
                SyntheticAccessorResolver.PREFIX_DECREMENT
            }
        } else {
            if (isAdd) {
                SyntheticAccessorResolver.POSTFIX_INCREMENT
            } else {
                SyntheticAccessorResolver.POSTFIX_DECREMENT
            }
        }
    }

    companion object {
        // math type constants
        const val ADD = SyntheticAccessorResolver.ADD_ASSIGNMENT
        const val SUB = SyntheticAccessorResolver.SUB_ASSIGNMENT
        const val MUL = SyntheticAccessorResolver.MUL_ASSIGNMENT
        const val DIV = SyntheticAccessorResolver.DIV_ASSIGNMENT
        const val REM = SyntheticAccessorResolver.REM_ASSIGNMENT
        const val AND = SyntheticAccessorResolver.AND_ASSIGNMENT
        const val OR = SyntheticAccessorResolver.OR_ASSIGNMENT
        const val XOR = SyntheticAccessorResolver.XOR_ASSIGNMENT
        const val SHL = SyntheticAccessorResolver.SHL_ASSIGNMENT
        const val SHR = SyntheticAccessorResolver.SHR_ASSIGNMENT
        const val USHR = SyntheticAccessorResolver.USHR_ASSIGNMENT

        const val INT = 0
        const val LONG = 1
        const val FLOAT = 2
        const val DOUBLE = 3

        const val POSITIVE_ONE = 1
        const val NEGATIVE_ONE = -1
        const val OTHER = 0

        const val SyntheticAccessorFSM_start = 1
        const val SyntheticAccessorFSM_first_final = 17
        const val SyntheticAccessorFSM_error = 0
        const val SyntheticAccessorFSM_en_main = 1

        private val _SyntheticAccessorFSM_actions: IntArray = intArrayOf(
            0, 1, 0, 1, 1, 1, 2, 1, 13, 1, 14, 1, 15, 1, 16, 1, 17, 1, 18, 1, 19, 1, 20, 1, 21, 1, 25, 2, 3, 7, 2, 4, 7, 2, 5, 7, 2, 6, 7, 2, 8, 12, 2, 9, 12, 2, 10, 12, 2, 11, 12, 2, 22, 23, 2, 22, 24, 2, 22, 25, 2, 22, 26, 2, 22, 27, 2, 22, 28
        )

        private val _SyntheticAccessorFSM_key_offsets: IntArray = intArrayOf(
            0, 0, 12, 82, 98, 102, 104, 166, 172, 174, 180, 184, 190, 192, 196, 198, 201, 203
        )

        private val _SyntheticAccessorFSM_trans_keys: IntArray = intArrayOf(
            82, 88, 89, 95, 96, 102, 103, 109, 110, 114, 116, 120, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 177, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 202, 203, 204, 206, 207, 208, 216, 15, 17, 18, 25, 129, 143, 144, 176, 178, 205, 144, 145, 155, 156, 166, 167, 171, 172, 176, 177, 187, 188, 198, 199, 203, 204, 89, 95, 103, 109, 15, 17, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 177, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 202, 203, 204, 206, 207, 144, 176, 178, 205, 89, 95, 103, 109, 129, 143, 15, 17, 89, 95, 103, 109, 129, 143, 89, 95, 103, 109, 89, 95, 103, 109, 129, 143, 15, 17, 89, 95, 103, 109, 15, 17, 14, 10, 12, 15, 17, 0
        )

        private val _SyntheticAccessorFSM_single_lengths: IntArray = intArrayOf(
            0, 0, 60, 16, 0, 0, 58, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0
        )

        private val _SyntheticAccessorFSM_range_lengths: IntArray = intArrayOf(
            0, 6, 5, 0, 2, 1, 2, 3, 1, 3, 2, 3, 1, 2, 1, 1, 1, 0
        )

        private val _SyntheticAccessorFSM_index_offsets: IntArray = intArrayOf(
            0, 0, 7, 73, 90, 93, 95, 156, 160, 162, 166, 169, 173, 175, 178, 180, 183, 185
        )

        private val _SyntheticAccessorFSM_indicies: IntArray = intArrayOf(
            0, 2, 0, 2, 3, 3, 1, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 9, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 9, 10, 11, 22, 23, 9, 10, 11, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 10, 11, 22, 23, 10, 11, 24, 24, 4, 5, 6, 7, 9, 1, 25, 26, 27, 28, 29, 30, 31, 32, 25, 26, 27, 28, 29, 30, 31, 32, 1, 33, 33, 1, 34, 1, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 9, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 9, 10, 11, 22, 23, 9, 10, 11, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 10, 11, 22, 23, 10, 11, 7, 9, 1, 35, 35, 36, 1, 37, 1, 35, 35, 38, 1, 35, 35, 1, 39, 39, 40, 1, 41, 1, 39, 39, 1, 42, 1, 44, 43, 1, 45, 1, 1, 0
        )

        private val _SyntheticAccessorFSM_trans_targs: IntArray = intArrayOf(
            2, 0, 14, 15, 17, 3, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 11, 4, 4, 4, 4, 4, 4, 4, 4, 5, 17, 8, 9, 17, 10, 12, 13, 17, 17, 16, 17, 17
        )

        private val _SyntheticAccessorFSM_trans_actions: IntArray = intArrayOf(
            0, 0, 1, 0, 51, 3, 0, 27, 39, 7, 9, 11, 13, 15, 17, 19, 21, 23, 30, 42, 33, 45, 36, 48, 5, 27, 39, 30, 42, 33, 45, 36, 48, 1, 63, 1, 0, 66, 0, 1, 0, 60, 54, 0, 25, 57
        )

    }
}

