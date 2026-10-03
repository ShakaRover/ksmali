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

package com.android.tools.smali.dexlib2.immutable.instruction

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.writer.InstructionFactory

object ImmutableInstructionFactory : InstructionFactory<Reference> {
    override fun makeInstruction10t(opcode: Opcode, codeOffset: Int): ImmutableInstruction10t =
        ImmutableInstruction10t(opcode, codeOffset)

    override fun makeInstruction10x(opcode: Opcode): ImmutableInstruction10x =
        ImmutableInstruction10x(opcode)

    override fun makeInstruction11n(opcode: Opcode, registerA: Int, literal: Int): ImmutableInstruction11n =
        ImmutableInstruction11n(opcode, registerA, literal)

    override fun makeInstruction11x(opcode: Opcode, registerA: Int): ImmutableInstruction11x =
        ImmutableInstruction11x(opcode, registerA)

    override fun makeInstruction12x(opcode: Opcode, registerA: Int, registerB: Int): ImmutableInstruction12x =
        ImmutableInstruction12x(opcode, registerA, registerB)

    override fun makeInstruction20bc(
        opcode: Opcode,
        verificationError: Int,
        reference: Reference
    ): ImmutableInstruction20bc = ImmutableInstruction20bc(opcode, verificationError, reference)

    override fun makeInstruction20t(opcode: Opcode, codeOffset: Int): ImmutableInstruction20t =
        ImmutableInstruction20t(opcode, codeOffset)

    override fun makeInstruction21c(opcode: Opcode, registerA: Int, reference: Reference): ImmutableInstruction21c =
        ImmutableInstruction21c(opcode, registerA, reference)

    override fun makeInstruction21ih(opcode: Opcode, registerA: Int, literal: Int): ImmutableInstruction21ih =
        ImmutableInstruction21ih(opcode, registerA, literal)

    override fun makeInstruction21lh(opcode: Opcode, registerA: Int, literal: Long): ImmutableInstruction21lh =
        ImmutableInstruction21lh(opcode, registerA, literal)

    override fun makeInstruction21s(opcode: Opcode, registerA: Int, literal: Int): ImmutableInstruction21s =
        ImmutableInstruction21s(opcode, registerA, literal)

    override fun makeInstruction21t(opcode: Opcode, registerA: Int, codeOffset: Int): ImmutableInstruction21t =
        ImmutableInstruction21t(opcode, registerA, codeOffset)

    override fun makeInstruction22b(
        opcode: Opcode,
        registerA: Int,
        registerB: Int,
        literal: Int
    ): ImmutableInstruction22b = ImmutableInstruction22b(opcode, registerA, registerB, literal)

    override fun makeInstruction22c(
        opcode: Opcode,
        registerA: Int,
        registerB: Int,
        reference: Reference
    ): ImmutableInstruction22c = ImmutableInstruction22c(opcode, registerA, registerB, reference)

    override fun makeInstruction22s(
        opcode: Opcode,
        registerA: Int,
        registerB: Int,
        literal: Int
    ): ImmutableInstruction22s = ImmutableInstruction22s(opcode, registerA, registerB, literal)

    override fun makeInstruction22t(
        opcode: Opcode,
        registerA: Int,
        registerB: Int,
        codeOffset: Int
    ): ImmutableInstruction22t = ImmutableInstruction22t(opcode, registerA, registerB, codeOffset)

    override fun makeInstruction22x(opcode: Opcode, registerA: Int, registerB: Int): ImmutableInstruction22x =
        ImmutableInstruction22x(opcode, registerA, registerB)

    override fun makeInstruction23x(
        opcode: Opcode,
        registerA: Int,
        registerB: Int,
        registerC: Int
    ): ImmutableInstruction23x = ImmutableInstruction23x(opcode, registerA, registerB, registerC)

    override fun makeInstruction30t(opcode: Opcode, codeOffset: Int): ImmutableInstruction30t =
        ImmutableInstruction30t(opcode, codeOffset)

    override fun makeInstruction31c(opcode: Opcode, registerA: Int, reference: Reference): ImmutableInstruction31c =
        ImmutableInstruction31c(opcode, registerA, reference)

    override fun makeInstruction31i(opcode: Opcode, registerA: Int, literal: Int): ImmutableInstruction31i =
        ImmutableInstruction31i(opcode, registerA, literal)

    override fun makeInstruction31t(opcode: Opcode, registerA: Int, codeOffset: Int): ImmutableInstruction31t =
        ImmutableInstruction31t(opcode, registerA, codeOffset)

    override fun makeInstruction32x(opcode: Opcode, registerA: Int, registerB: Int): ImmutableInstruction32x =
        ImmutableInstruction32x(opcode, registerA, registerB)

    override fun makeInstruction35c(
        opcode: Opcode,
        registerCount: Int,
        registerC: Int,
        registerD: Int,
        registerE: Int,
        registerF: Int,
        registerG: Int,
        reference: Reference
    ): ImmutableInstruction35c =
        ImmutableInstruction35c(opcode, registerCount, registerC, registerD, registerE, registerF, registerG, reference)

    override fun makeInstruction3rc(
        opcode: Opcode,
        startRegister: Int,
        registerCount: Int,
        reference: Reference
    ): ImmutableInstruction3rc = ImmutableInstruction3rc(opcode, startRegister, registerCount, reference)

    override fun makeInstruction51l(opcode: Opcode, registerA: Int, literal: Long): ImmutableInstruction51l =
        ImmutableInstruction51l(opcode, registerA, literal)

    override fun makeSparseSwitchPayload(
        switchElements: List<SwitchElement>?
    ): ImmutableSparseSwitchPayload = ImmutableSparseSwitchPayload(switchElements)

    override fun makePackedSwitchPayload(
        switchElements: List<SwitchElement>?
    ): ImmutablePackedSwitchPayload = ImmutablePackedSwitchPayload(switchElements)

    override fun makeArrayPayload(elementWidth: Int, arrayElements: List<Number>?): ImmutableArrayPayload =
        ImmutableArrayPayload(elementWidth, arrayElements)
}
