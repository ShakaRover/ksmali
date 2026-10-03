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
import java.io.IOException
import java.util.BitSet

class PostInstructionRegisterInfoMethodItem(
    private val registerFormatter: RegisterFormatter,
    private val analyzedInstruction: AnalyzedInstruction,
    codeAddress: Int
) : MethodItem(codeAddress) {
    override val sortOrder: Double get() = 100.1

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        val registerInfo = registerFormatter.options.registerInfo
        val registerCount = analyzedInstruction.registerCount
        val registers = BitSet(registerCount)

        if ((registerInfo and BaksmaliOptions.ALL) != 0) {
            registers.set(0, registerCount)
        } else {
            if ((registerInfo and BaksmaliOptions.ALLPOST) != 0) {
                registers.set(0, registerCount)
            } else if ((registerInfo and BaksmaliOptions.DEST) != 0) {
                addDestRegs(registers, registerCount)
            }
        }

        return writeRegisterInfo(writer, registers)
    }

    private fun addDestRegs(printPostRegister: BitSet, registerCount: Int) {
        for (registerNum in 0 until registerCount) {
            if (analyzedInstruction.getPreInstructionRegisterType(registerNum) !=
                analyzedInstruction.getPostInstructionRegisterType(registerNum)) {
                printPostRegister.set(registerNum)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeRegisterInfo(writer: BaksmaliWriter, registers: BitSet): Boolean {
        var registerNum = registers.nextSetBit(0)
        if (registerNum < 0) {
            return false
        }

        writer.write('#')
        while (registerNum >= 0) {
            val registerType =
                analyzedInstruction.getPostInstructionRegisterType(registerNum)

            registerFormatter.writeTo(writer, registerNum)
            writer.write('=')
            registerType.writeTo(writer)
            writer.write(';')
            registerNum = registers.nextSetBit(registerNum + 1)
        }
        return true
    }
}
