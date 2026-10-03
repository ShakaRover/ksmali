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

package com.android.tools.smali.dexlib2.immutable.instruction

import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35mi
import com.android.tools.smali.dexlib2.util.*

open class ImmutableInstruction35mi(
    opcode: Opcode,
    registerCount: Int,
    registerC: Int,
    registerD: Int,
    registerE: Int,
    registerF: Int,
    registerG: Int,
    inlineIndex: Int
) : ImmutableInstruction(opcode), Instruction35mi {
    override val registerCount: Int = check35cAnd45ccRegisterCount(registerCount)
    override val registerC: Int = if (registerCount > 0) checkNibbleRegister(registerC) else 0
    override val registerD: Int = if (registerCount > 1) checkNibbleRegister(registerD) else 0
    override val registerE: Int = if (registerCount > 2) checkNibbleRegister(registerE) else 0
    override val registerF: Int = if (registerCount > 3) checkNibbleRegister(registerF) else 0
    override val registerG: Int = if (registerCount > 4) checkNibbleRegister(registerG) else 0
    override val inlineIndex: Int = checkInlineIndex(inlineIndex)

    override val format: Format
        get() = FORMAT

    companion object {
        @JvmField
        val FORMAT: Format = Format.Format35mi

        @JvmStatic
        fun of(instruction: Instruction35mi): ImmutableInstruction35mi {
            if (instruction is ImmutableInstruction35mi) {
                return instruction
            }
            return ImmutableInstruction35mi(instruction.opcode, instruction.registerCount, instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF, instruction.registerG, instruction.inlineIndex)
        }
    }
}
