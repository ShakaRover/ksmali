/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.analysis.AnalyzedInstruction
import com.android.tools.smali.dexlib2.analysis.MethodAnalyzer
import com.android.tools.smali.dexlib2.analysis.RegisterType
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.io.IOException
import java.util.BitSet

class PreInstructionRegisterInfoMethodItem(
    private val registerInfo: Int,
    private val methodAnalyzer: MethodAnalyzer,
    private val registerFormatter: RegisterFormatter,
    private val analyzedInstruction: AnalyzedInstruction,
    codeAddress: Int
) : MethodItem(codeAddress) {

    override val sortOrder: Double get() = 99.9

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        val registerCount = analyzedInstruction.registerCount
        val registers = BitSet(registerCount)
        var mergeRegisters: BitSet? = null

        if ((registerInfo and BaksmaliOptions.ALL) != 0) {
            registers.set(0, registerCount)
        } else {
            if ((registerInfo and BaksmaliOptions.ALLPRE) != 0) {
                registers.set(0, registerCount)
            } else {
                if ((registerInfo and BaksmaliOptions.ARGS) != 0) {
                    addArgsRegs(registers)
                }
                if ((registerInfo and BaksmaliOptions.MERGE) != 0) {
                    if (analyzedInstruction.isBeginningInstruction) {
                        addParamRegs(registers, registerCount)
                    }
                    mergeRegisters = BitSet(registerCount)
                    addMergeRegs(mergeRegisters, registerCount)
                } else if ((registerInfo and BaksmaliOptions.FULLMERGE) != 0 &&
                    analyzedInstruction.isBeginningInstruction) {
                    addParamRegs(registers, registerCount)
                }
            }
        }

        if ((registerInfo and BaksmaliOptions.FULLMERGE) != 0) {
            if (mergeRegisters == null) {
                mergeRegisters = BitSet(registerCount)
                addMergeRegs(mergeRegisters, registerCount)
            }
            registers.or(mergeRegisters)
        } else if (mergeRegisters != null) {
            registers.or(mergeRegisters)
            mergeRegisters = null
        }

        return writeRegisterInfo(writer, registers, mergeRegisters)
    }

    private fun addArgsRegs(registers: BitSet) {
        val instruction = analyzedInstruction.instruction
        if (instruction is RegisterRangeInstruction) {
            registers.set(
                instruction.startRegister,
                instruction.startRegister + instruction.registerCount)
        } else if (instruction is FiveRegisterInstruction) {
            val regCount = instruction.registerCount
            when (regCount) {
                5 -> {
                    registers.set(instruction.registerG)
                    registers.set(instruction.registerF)
                    registers.set(instruction.registerE)
                    registers.set(instruction.registerD)
                    registers.set(instruction.registerC)
                }
                4 -> {
                    registers.set(instruction.registerF)
                    registers.set(instruction.registerE)
                    registers.set(instruction.registerD)
                    registers.set(instruction.registerC)
                }
                3 -> {
                    registers.set(instruction.registerE)
                    registers.set(instruction.registerD)
                    registers.set(instruction.registerC)
                }
                2 -> {
                    registers.set(instruction.registerD)
                    registers.set(instruction.registerC)
                }
                1 -> registers.set(instruction.registerC)
            }
        } else if (instruction is ThreeRegisterInstruction) {
            registers.set(instruction.registerA)
            registers.set(instruction.registerB)
            registers.set(instruction.registerC)
        } else if (instruction is TwoRegisterInstruction) {
            registers.set(instruction.registerA)
            registers.set(instruction.registerB)
        } else if (instruction is OneRegisterInstruction) {
            registers.set(instruction.registerA)
        }
    }

    private fun addMergeRegs(registers: BitSet, registerCount: Int) {
        if (analyzedInstruction.predecessorCount <= 1) {
            //in the common case of an instruction that only has a single predecessor which is the previous
            //instruction, the pre-instruction registers will always match the previous instruction's
            //post-instruction registers
            return
        }

        for (registerNum in 0 until registerCount) {
            val mergedRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNum)

            for (predecessor in analyzedInstruction.predecessors) {
                val predecessorRegisterType = analyzedInstruction.getPredecessorRegisterType(
                    predecessor, registerNum)
                if (predecessorRegisterType.category != RegisterType.UNKNOWN &&
                    predecessorRegisterType != mergedRegisterType) {
                    registers.set(registerNum)
                }
            }
        }
    }

    private fun addParamRegs(registers: BitSet, registerCount: Int) {
        val parameterRegisterCount = methodAnalyzer.paramRegisterCount
        registers.set(registerCount - parameterRegisterCount, registerCount)
    }

    @Throws(IOException::class)
    private fun writeFullMerge(writer: BaksmaliWriter, registerNum: Int) {
        registerFormatter.writeTo(writer, registerNum)
        writer.write('=')
        analyzedInstruction.getPreInstructionRegisterType(registerNum).writeTo(writer)
        writer.write(":merge{")

        var first = true

        for (predecessor in analyzedInstruction.predecessors) {
            val predecessorRegisterType = analyzedInstruction.getPredecessorRegisterType(
                predecessor, registerNum)

            if (!first) {
                writer.write(',')
            }

            if (predecessor.instructionIndex == -1) {
                //the fake "StartOfMethod" instruction
                writer.write("Start:")
            } else {
                writer.write("0x")
                writer.writeUnsignedLongAsHex(methodAnalyzer.getInstructionAddress(predecessor).toLong())
                writer.write(':')
            }
            predecessorRegisterType.writeTo(writer)

            first = false
        }
        writer.write('}')
    }

    @Throws(IOException::class)
    private fun writeRegisterInfo(
        writer: BaksmaliWriter, registers: BitSet, fullMergeRegisters: BitSet?
    ): Boolean {
        var firstRegister = true
        var previousWasFullMerge = false
        var registerNum = registers.nextSetBit(0)
        if (registerNum < 0) {
            return false
        }

        writer.write('#')
        while (registerNum >= 0) {
            val fullMerge = fullMergeRegisters != null && fullMergeRegisters.get(registerNum)
            if (fullMerge) {
                if (!firstRegister) {
                    writer.write('\n')
                    writer.write('#')
                }
                writeFullMerge(writer, registerNum)
                previousWasFullMerge = true
            } else {
                if (previousWasFullMerge) {
                    writer.write('\n')
                    writer.write('#')
                    previousWasFullMerge = false
                }

                val registerType = analyzedInstruction.getPreInstructionRegisterType(registerNum)

                registerFormatter.writeTo(writer, registerNum)
                writer.write('=')

                registerType.writeTo(writer)
                writer.write(';')
            }

            firstRegister = false
            registerNum = registers.nextSetBit(registerNum + 1)
        }
        return true
    }
}
