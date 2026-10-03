/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.baksmali.Adaptors.Format

import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.analysis.UnresolvedOdexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload

fun makeInstructionFormatMethodItem(
    methodDef: MethodDefinition, codeAddress: Int, instruction: Instruction
): InstructionMethodItem<out Instruction> {

    if (instruction is OffsetInstruction) {
        return OffsetInstructionFormatMethodItem(
            methodDef.classDef.options, methodDef, codeAddress, instruction)
    }

    if (instruction is UnresolvedOdexInstruction) {
        return UnresolvedOdexInstructionMethodItem(methodDef, codeAddress, instruction)
    }

    when (instruction.opcode.format) {
        Format.ArrayPayload ->
            return ArrayDataMethodItem(methodDef, codeAddress, instruction as ArrayPayload)
        Format.PackedSwitchPayload ->
            return PackedSwitchMethodItem(methodDef, codeAddress, instruction as PackedSwitchPayload)
        Format.SparseSwitchPayload ->
            return SparseSwitchMethodItem(methodDef, codeAddress, instruction as SparseSwitchPayload)
        else ->
            return InstructionMethodItem(methodDef, codeAddress, instruction)
    }
}
