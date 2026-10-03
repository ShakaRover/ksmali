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
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction10x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction12x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20bc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21ih
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21lh
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22b
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22cs
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22s
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction23x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction30t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction32x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35mi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35ms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rmi
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rms
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction45cc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction4rcc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction51l
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.UnknownInstruction
import com.android.tools.smali.dexlib2.util.*
import com.android.tools.smali.util.ImmutableConverter

abstract class ImmutableInstruction(opcode: Opcode) : Instruction {
    override val opcode: Opcode = opcode

    init {
        checkFormat(opcode, format)
    }

    abstract val format: Format

    override val codeUnits: Int
        get() = format.size / 2

    companion object {
        @JvmStatic
        fun of(instruction: Instruction): ImmutableInstruction {
            if (instruction is ImmutableInstruction) {
                return instruction
            }

            when (instruction.opcode.format) {
                Format.Format10t -> return ImmutableInstruction10t.of(instruction as Instruction10t)
                Format.Format10x -> {
                    if (instruction is UnknownInstruction) {
                        return ImmutableUnknownInstruction.of(instruction)
                    }
                    return ImmutableInstruction10x.of(instruction as Instruction10x)
                }
                Format.Format11n -> return ImmutableInstruction11n.of(instruction as Instruction11n)
                Format.Format11x -> return ImmutableInstruction11x.of(instruction as Instruction11x)
                Format.Format12x -> return ImmutableInstruction12x.of(instruction as Instruction12x)
                Format.Format20bc -> return ImmutableInstruction20bc.of(instruction as Instruction20bc)
                Format.Format20t -> return ImmutableInstruction20t.of(instruction as Instruction20t)
                Format.Format21c -> return ImmutableInstruction21c.of(instruction as Instruction21c)
                Format.Format21ih -> return ImmutableInstruction21ih.of(instruction as Instruction21ih)
                Format.Format21lh -> return ImmutableInstruction21lh.of(instruction as Instruction21lh)
                Format.Format21s -> return ImmutableInstruction21s.of(instruction as Instruction21s)
                Format.Format21t -> return ImmutableInstruction21t.of(instruction as Instruction21t)
                Format.Format22b -> return ImmutableInstruction22b.of(instruction as Instruction22b)
                Format.Format22c -> return ImmutableInstruction22c.of(instruction as Instruction22c)
                Format.Format22cs -> return ImmutableInstruction22cs.of(instruction as Instruction22cs)
                Format.Format22s -> return ImmutableInstruction22s.of(instruction as Instruction22s)
                Format.Format22t -> return ImmutableInstruction22t.of(instruction as Instruction22t)
                Format.Format22x -> return ImmutableInstruction22x.of(instruction as Instruction22x)
                Format.Format23x -> return ImmutableInstruction23x.of(instruction as Instruction23x)
                Format.Format30t -> return ImmutableInstruction30t.of(instruction as Instruction30t)
                Format.Format31c -> return ImmutableInstruction31c.of(instruction as Instruction31c)
                Format.Format31i -> return ImmutableInstruction31i.of(instruction as Instruction31i)
                Format.Format31t -> return ImmutableInstruction31t.of(instruction as Instruction31t)
                Format.Format32x -> return ImmutableInstruction32x.of(instruction as Instruction32x)
                Format.Format35c -> return ImmutableInstruction35c.of(instruction as Instruction35c)
                Format.Format35mi -> return ImmutableInstruction35mi.of(instruction as Instruction35mi)
                Format.Format35ms -> return ImmutableInstruction35ms.of(instruction as Instruction35ms)
                Format.Format3rc -> return ImmutableInstruction3rc.of(instruction as Instruction3rc)
                Format.Format3rmi -> return ImmutableInstruction3rmi.of(instruction as Instruction3rmi)
                Format.Format3rms -> return ImmutableInstruction3rms.of(instruction as Instruction3rms)
                Format.Format45cc -> return ImmutableInstruction45cc.of(instruction as Instruction45cc)
                Format.Format4rcc -> return ImmutableInstruction4rcc.of(instruction as Instruction4rcc)
                Format.Format51l -> return ImmutableInstruction51l.of(instruction as Instruction51l)
                Format.PackedSwitchPayload -> return ImmutablePackedSwitchPayload.of(
                    instruction as PackedSwitchPayload
                )
                Format.SparseSwitchPayload -> return ImmutableSparseSwitchPayload.of(
                    instruction as SparseSwitchPayload
                )
                Format.ArrayPayload -> return ImmutableArrayPayload.of(instruction as ArrayPayload)
                else -> throw RuntimeException("Unexpected instruction type")
            }
        }

        @JvmStatic
        fun immutableListOf(list: Iterable<Instruction>?): List<ImmutableInstruction> {
            return CONVERTER.toList(list)
        }

        private val CONVERTER: ImmutableConverter<ImmutableInstruction, Instruction> =
            object : ImmutableConverter<ImmutableInstruction, Instruction>() {
                override fun isImmutable(item: Instruction): Boolean {
                    return item is ImmutableInstruction
                }

                override fun makeImmutable(item: Instruction): ImmutableInstruction {
                    return of(item)
                }
            }
    }
}
