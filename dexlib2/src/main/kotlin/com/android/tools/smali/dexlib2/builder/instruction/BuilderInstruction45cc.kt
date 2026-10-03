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


package com.android.tools.smali.dexlib2.builder.instruction

import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction45cc
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.util.check35cAnd45ccRegisterCount
import com.android.tools.smali.dexlib2.util.checkNibbleRegister

open class BuilderInstruction45cc(
    opcode: Opcode,
    registerCount: Int,
    registerC: Int,
    registerD: Int,
    registerE: Int,
    registerF: Int,
    registerG: Int,
    override val reference: Reference,
    override val reference2: Reference
) : BuilderInstruction(opcode), Instruction45cc {
    override val registerCount: Int = check35cAnd45ccRegisterCount(registerCount)
    override val registerC: Int = if (registerCount > 0) checkNibbleRegister(registerC) else 0
    override val registerD: Int = if (registerCount > 1) checkNibbleRegister(registerD) else 0
    override val registerE: Int = if (registerCount > 2) checkNibbleRegister(registerE) else 0
    override val registerF: Int = if (registerCount > 3) checkNibbleRegister(registerF) else 0
    override val registerG: Int = if (registerCount > 4) checkNibbleRegister(registerG) else 0

    override val referenceType: Int
        get() = opcode.referenceType

    override val referenceType2: Int
        get() = opcode.referenceType2

    override val format: Format
        get() = FORMAT

    companion object {
        val FORMAT: Format = Format.Format45cc
    }
}
