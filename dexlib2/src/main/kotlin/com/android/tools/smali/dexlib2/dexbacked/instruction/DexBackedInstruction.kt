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

package com.android.tools.smali.dexlib2.dexbacked.instruction

import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.util.ExceptionWithContext

abstract class DexBackedInstruction(
    @JvmField val dexFile: DexBackedDexFile,
    opcode: Opcode,
    @JvmField val instructionStart: Int
) : Instruction {
    override val opcode: Opcode = opcode

    override val codeUnits: Int
        get() = opcode.format.size / 2

    companion object {
        @JvmStatic
        fun readFrom(dexFile: DexBackedDexFile, reader: DexReader<out DexBuffer>): Instruction {
            var opcodeValue = reader.peekUbyte()

            if (opcodeValue == 0) {
                opcodeValue = reader.peekUshort()
            }

            val opcode = dexFile.opcodes.getOpcodeByValue(opcodeValue)

            val instruction = buildInstruction(
                dexFile, opcode,
                reader.offset + reader.dexBuf.baseOffset -
                    dexFile.buffer.baseOffset - dexFile.baseDataOffset
            )
            reader.moveRelative(instruction.codeUnits * 2)
            return instruction
        }

        private fun buildInstruction(
            dexFile: DexBackedDexFile,
            opcode: Opcode?,
            instructionStartOffset: Int
        ): DexBackedInstruction {
            if (opcode == null) {
                return DexBackedUnknownInstruction(dexFile, instructionStartOffset)
            }
            return when (opcode.format) {
                Format.Format10t -> DexBackedInstruction10t(dexFile, opcode, instructionStartOffset)
                Format.Format10x -> DexBackedInstruction10x(dexFile, opcode, instructionStartOffset)
                Format.Format11n -> DexBackedInstruction11n(dexFile, opcode, instructionStartOffset)
                Format.Format11x -> DexBackedInstruction11x(dexFile, opcode, instructionStartOffset)
                Format.Format12x -> DexBackedInstruction12x(dexFile, opcode, instructionStartOffset)
                Format.Format20bc -> DexBackedInstruction20bc(dexFile, opcode, instructionStartOffset)
                Format.Format20t -> DexBackedInstruction20t(dexFile, opcode, instructionStartOffset)
                Format.Format21c -> DexBackedInstruction21c(dexFile, opcode, instructionStartOffset)
                Format.Format21ih -> DexBackedInstruction21ih(dexFile, opcode, instructionStartOffset)
                Format.Format21lh -> DexBackedInstruction21lh(dexFile, opcode, instructionStartOffset)
                Format.Format21s -> DexBackedInstruction21s(dexFile, opcode, instructionStartOffset)
                Format.Format21t -> DexBackedInstruction21t(dexFile, opcode, instructionStartOffset)
                Format.Format22b -> DexBackedInstruction22b(dexFile, opcode, instructionStartOffset)
                Format.Format22c -> DexBackedInstruction22c(dexFile, opcode, instructionStartOffset)
                Format.Format22cs -> DexBackedInstruction22cs(dexFile, opcode, instructionStartOffset)
                Format.Format22s -> DexBackedInstruction22s(dexFile, opcode, instructionStartOffset)
                Format.Format22t -> DexBackedInstruction22t(dexFile, opcode, instructionStartOffset)
                Format.Format22x -> DexBackedInstruction22x(dexFile, opcode, instructionStartOffset)
                Format.Format23x -> DexBackedInstruction23x(dexFile, opcode, instructionStartOffset)
                Format.Format30t -> DexBackedInstruction30t(dexFile, opcode, instructionStartOffset)
                Format.Format31c -> DexBackedInstruction31c(dexFile, opcode, instructionStartOffset)
                Format.Format31i -> DexBackedInstruction31i(dexFile, opcode, instructionStartOffset)
                Format.Format31t -> DexBackedInstruction31t(dexFile, opcode, instructionStartOffset)
                Format.Format32x -> DexBackedInstruction32x(dexFile, opcode, instructionStartOffset)
                Format.Format35c -> DexBackedInstruction35c(dexFile, opcode, instructionStartOffset)
                Format.Format35ms -> DexBackedInstruction35ms(dexFile, opcode, instructionStartOffset)
                Format.Format35mi -> DexBackedInstruction35mi(dexFile, opcode, instructionStartOffset)
                Format.Format3rc -> DexBackedInstruction3rc(dexFile, opcode, instructionStartOffset)
                Format.Format3rmi -> DexBackedInstruction3rmi(dexFile, opcode, instructionStartOffset)
                Format.Format3rms -> DexBackedInstruction3rms(dexFile, opcode, instructionStartOffset)
                Format.Format45cc -> DexBackedInstruction45cc(dexFile, opcode, instructionStartOffset)
                Format.Format4rcc -> DexBackedInstruction4rcc(dexFile, opcode, instructionStartOffset)
                Format.Format51l -> DexBackedInstruction51l(dexFile, opcode, instructionStartOffset)
                Format.PackedSwitchPayload ->
                    DexBackedPackedSwitchPayload(dexFile, instructionStartOffset)
                Format.SparseSwitchPayload ->
                    DexBackedSparseSwitchPayload(dexFile, instructionStartOffset)
                Format.ArrayPayload -> DexBackedArrayPayload(dexFile, instructionStartOffset)
                else -> throw ExceptionWithContext(
                    "Unexpected opcode format: %s", opcode.format.toString()
                )
            }
        }
    }
}
