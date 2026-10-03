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

import com.android.tools.smali.baksmali.Adaptors.LabelMethodItem
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import java.io.IOException

class OffsetInstructionFormatMethodItem(
    options: BaksmaliOptions, methodDef: MethodDefinition, codeAddress: Int, instruction: OffsetInstruction
) : InstructionMethodItem<OffsetInstruction>(methodDef, codeAddress, instruction) {

    protected val label: LabelMethodItem =
        methodDef.labelCache.internLabel(
            LabelMethodItem(options, codeAddress + instruction.codeOffset, labelPrefix))

    @Throws(IOException::class)
    override fun writeTargetLabel(writer: BaksmaliWriter) {
        label.writeTo(writer)
    }

    private val labelPrefix: String
        get() {
            val opcode = instruction.opcode
            return when (opcode.format) {
                Format.Format10t, Format.Format20t, Format.Format30t -> "goto_"
                Format.Format21t, Format.Format22t -> "cond_"
                Format.Format31t -> {
                    if (opcode == Opcode.FILL_ARRAY_DATA) {
                        "array_"
                    } else if (opcode == Opcode.PACKED_SWITCH) {
                        "pswitch_data_"
                    } else {
                        // Opcode.SPARSE_SWITCH;
                        "sswitch_data_"
                    }
                }
                else -> {
                    assert(false)
                    "goto_"
                }
            }
        }
}
