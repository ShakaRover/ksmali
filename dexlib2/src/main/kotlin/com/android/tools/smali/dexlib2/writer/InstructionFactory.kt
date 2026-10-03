/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.reference.Reference

interface InstructionFactory<Ref : Reference> {
    fun makeInstruction10t(opcode: Opcode, codeOffset: Int): Instruction
    fun makeInstruction10x(opcode: Opcode): Instruction
    fun makeInstruction11n(opcode: Opcode, registerA: Int, literal: Int): Instruction
    fun makeInstruction11x(opcode: Opcode, registerA: Int): Instruction
    fun makeInstruction12x(opcode: Opcode, registerA: Int, registerB: Int): Instruction
    fun makeInstruction20bc(opcode: Opcode, verificationError: Int, reference: Ref): Instruction
    fun makeInstruction20t(opcode: Opcode, codeOffset: Int): Instruction
    fun makeInstruction21c(opcode: Opcode, registerA: Int, reference: Ref): Instruction
    fun makeInstruction21ih(opcode: Opcode, registerA: Int, literal: Int): Instruction
    fun makeInstruction21lh(opcode: Opcode, registerA: Int, literal: Long): Instruction
    fun makeInstruction21s(opcode: Opcode, registerA: Int, literal: Int): Instruction
    fun makeInstruction21t(opcode: Opcode, registerA: Int, codeOffset: Int): Instruction
    fun makeInstruction22b(opcode: Opcode, registerA: Int, registerB: Int, literal: Int): Instruction
    fun makeInstruction22c(opcode: Opcode, registerA: Int, registerB: Int, reference: Ref): Instruction
    fun makeInstruction22s(opcode: Opcode, registerA: Int, registerB: Int, literal: Int): Instruction
    fun makeInstruction22t(opcode: Opcode, registerA: Int, registerB: Int, codeOffset: Int): Instruction
    fun makeInstruction22x(opcode: Opcode, registerA: Int, registerB: Int): Instruction
    fun makeInstruction23x(opcode: Opcode, registerA: Int, registerB: Int, registerC: Int): Instruction
    fun makeInstruction30t(opcode: Opcode, codeOffset: Int): Instruction
    fun makeInstruction31c(opcode: Opcode, registerA: Int, reference: Ref): Instruction
    fun makeInstruction31i(opcode: Opcode, registerA: Int, literal: Int): Instruction
    fun makeInstruction31t(opcode: Opcode, registerA: Int, codeOffset: Int): Instruction
    fun makeInstruction32x(opcode: Opcode, registerA: Int, registerB: Int): Instruction
    fun makeInstruction35c(opcode: Opcode, registerCount: Int, registerC: Int, registerD: Int, registerE: Int,
        registerF: Int, registerG: Int, reference: Ref): Instruction
    fun makeInstruction3rc(opcode: Opcode, startRegister: Int, registerCount: Int, reference: Ref): Instruction
    fun makeInstruction51l(opcode: Opcode, registerA: Int, literal: Long): Instruction
    fun makeSparseSwitchPayload(switchElements: List<@JvmWildcard SwitchElement>?): Instruction
    fun makePackedSwitchPayload(switchElements: List<@JvmWildcard SwitchElement>?): Instruction
    fun makeArrayPayload(elementWidth: Int, arrayElements: List<Number>?): Instruction
}
