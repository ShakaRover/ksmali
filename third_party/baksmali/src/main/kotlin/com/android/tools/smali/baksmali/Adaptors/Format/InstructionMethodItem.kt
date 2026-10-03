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

import com.android.tools.smali.baksmali.Adaptors.MethodItem
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.VerificationError
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FieldOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.InlineIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VtableIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20bc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31t
import com.android.tools.smali.dexlib2.iface.instruction.formats.UnknownInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.NumberUtils
import java.io.IOException
import java.util.Map

open class InstructionMethodItem<T : Instruction>(
    @JvmField protected val methodDef: MethodDefinition,
    codeAddress: Int,
    @JvmField protected val instruction: T
) : MethodItem(codeAddress) {

    override fun getSortOrder(): Double {
        //instructions should appear after everything except an "end try" label and .catch directive
        return 100.0
    }

    private fun isAllowedOdex(opcode: Opcode): Boolean {
        val options = methodDef.classDef.options
        if (options.allowOdex) {
            return true
        }

        if (methodDef.classDef.options.apiLevel >= 14) {
            return false
        }

        return opcode.isVolatileFieldAccessor || opcode == Opcode.THROW_VERIFICATION_ERROR
    }

    private fun interface Writable {
        fun write()
    }

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        val opcode = instruction.opcode
        var verificationErrorName: String? = null
        var referenceWritable: Writable? = null
        var referenceWritable2: Writable? = null

        var commentOutInstruction = false

        if (instruction is Instruction20bc) {
            val verificationError = instruction.verificationError
            verificationErrorName = VerificationError.getVerificationErrorName(verificationError)
            if (verificationErrorName == null) {
                writer.write("#was invalid verification error type: ")
                writer.writeSignedIntAsDec(verificationError)
                writer.write("\n")
                verificationErrorName = "generic-error"
            }
        }

        if (instruction is ReferenceInstruction) {
            val reference = instruction.reference

            try {
                reference.validateReference()
                referenceWritable = Writable { writer.writeReference(reference) }
            } catch (ex: Reference.InvalidReferenceException) {
                commentOutInstruction = true
                writer.write("#")
                writer.write(ex.message!!)
                writer.write("\n")
                referenceWritable = Writable { writer.write(ex.invalidReferenceRepresentation) }
            }

            if (instruction is DualReferenceInstruction) {
                try {
                    val reference2 = instruction.reference2
                    reference2.validateReference()
                    referenceWritable2 = Writable { writer.writeReference(reference2) }
                } catch (ex: Reference.InvalidReferenceException) {
                    commentOutInstruction = true
                    writer.write("#")
                    writer.write(ex.message!!)
                    writer.write("\n")
                    // Note: the original Java implementation assigns to referenceWritable here.
                    referenceWritable = Writable { writer.write(ex.invalidReferenceRepresentation) }
                }
            }
        }

        if (instruction is Instruction31t) {
            var validPayload = true

            when (instruction.opcode) {
                Opcode.PACKED_SWITCH -> {
                    val baseAddress = methodDef.getPackedSwitchBaseAddress(
                        this.codeAddress + instruction.codeOffset)
                    if (baseAddress == -1) {
                        validPayload = false
                    }
                }
                Opcode.SPARSE_SWITCH -> {
                    val baseAddress = methodDef.getSparseSwitchBaseAddress(
                        this.codeAddress + instruction.codeOffset)
                    if (baseAddress == -1) {
                        validPayload = false
                    }
                }
                Opcode.FILL_ARRAY_DATA -> {
                    try {
                        methodDef.findPayloadOffset(
                            this.codeAddress + instruction.codeOffset, Opcode.ARRAY_PAYLOAD)
                    } catch (ex: MethodDefinition.InvalidSwitchPayload) {
                        validPayload = false
                    }
                }
                else -> throw ExceptionWithContext("Invalid 31t opcode: %s", instruction.opcode)
            }

            if (!validPayload) {
                writer.write("#invalid payload reference\n")
                commentOutInstruction = true
            }
        }

        if (opcode.odexOnly()) {
            if (!isAllowedOdex(opcode)) {
                writer.write("#disallowed odex opcode\n")
                commentOutInstruction = true
            }
        }

        if (commentOutInstruction) {
            writer.write("#")
        }

        when (instruction.opcode.format) {
            Format.Format10t, Format.Format20t, Format.Format30t -> {
                writeOpcode(writer)
                writer.write(' ')
                writeTargetLabel(writer)
            }
            Format.Format10x -> {
                if (instruction is UnknownInstruction) {
                    writer.write("#unknown opcode: 0x")
                    writer.writeUnsignedLongAsHex(instruction.originalOpcode.toLong())
                    writer.write('\n')
                }
                writeOpcode(writer)
            }
            Format.Format11n -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeLiteral(writer)
            }
            Format.Format11x -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
            }
            Format.Format12x -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
            }
            Format.Format20bc -> {
                writeOpcode(writer)
                writer.write(' ')
                writer.write(verificationErrorName!!)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
            }
            Format.Format21c, Format.Format31c -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                referenceWritable!!.write()
            }
            Format.Format21ih, Format.Format21lh, Format.Format21s, Format.Format31i, Format.Format51l -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeLiteral(writer)
                if (instruction.opcode.setsWideRegister()) {
                    writeCommentIfLikelyDouble(writer)
                } else {
                    val isResourceId = writeCommentIfResourceId(writer)
                    if (!isResourceId) writeCommentIfLikelyFloat(writer)
                }
            }
            Format.Format21t, Format.Format31t -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeTargetLabel(writer)
            }
            Format.Format22b, Format.Format22s -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
                writer.write(", ")
                writeLiteral(writer)
            }
            Format.Format22c -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
            }
            Format.Format22cs -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
                writer.write(", ")
                writeFieldOffset(writer)
            }
            Format.Format22t -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
                writer.write(", ")
                writeTargetLabel(writer)
            }
            Format.Format22x, Format.Format32x -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
            }
            Format.Format23x -> {
                writeOpcode(writer)
                writer.write(' ')
                writeFirstRegister(writer)
                writer.write(", ")
                writeSecondRegister(writer)
                writer.write(", ")
                writeThirdRegister(writer)
            }
            Format.Format35c -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRegisters(writer)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
            }
            Format.Format35mi -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRegisters(writer)
                writer.write(", ")
                writeInlineIndex(writer)
            }
            Format.Format35ms -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRegisters(writer)
                writer.write(", ")
                writeVtableIndex(writer)
            }
            Format.Format3rc -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRangeRegisters(writer)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
            }
            Format.Format3rmi -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRangeRegisters(writer)
                writer.write(", ")
                writeInlineIndex(writer)
            }
            Format.Format3rms -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRangeRegisters(writer)
                writer.write(", ")
                writeVtableIndex(writer)
            }
            Format.Format45cc -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRegisters(writer)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
                writer.write(", ")
                assert(referenceWritable2 != null)
                referenceWritable2!!.write()
            }
            Format.Format4rcc -> {
                writeOpcode(writer)
                writer.write(' ')
                writeInvokeRangeRegisters(writer)
                writer.write(", ")
                assert(referenceWritable != null)
                referenceWritable!!.write()
                writer.write(", ")
                assert(referenceWritable2 != null)
                referenceWritable2!!.write()
            }
            else -> {
                assert(false)
                return false
            }
        }

        if (commentOutInstruction) {
            writer.write("\nnop")
        }

        return true
    }

    @Throws(IOException::class)
    protected fun writeOpcode(writer: BaksmaliWriter) {
        writer.write(instruction.opcode.name)
    }

    @Throws(IOException::class)
    protected open fun writeTargetLabel(writer: BaksmaliWriter) {
        //this method is overridden by OffsetInstructionMethodItem, and should only be called for the formats that
        //have a target
        throw RuntimeException()
    }

    @Throws(IOException::class)
    protected fun writeRegister(writer: BaksmaliWriter, registerNumber: Int) {
        methodDef.registerFormatter!!.writeTo(writer, registerNumber)
    }

    @Throws(IOException::class)
    protected fun writeFirstRegister(writer: BaksmaliWriter) {
        writeRegister(writer, (instruction as OneRegisterInstruction).registerA)
    }

    @Throws(IOException::class)
    protected fun writeSecondRegister(writer: BaksmaliWriter) {
        writeRegister(writer, (instruction as TwoRegisterInstruction).registerB)
    }

    @Throws(IOException::class)
    protected fun writeThirdRegister(writer: BaksmaliWriter) {
        writeRegister(writer, (instruction as ThreeRegisterInstruction).registerC)
    }

    @Throws(IOException::class)
    protected fun writeInvokeRegisters(writer: BaksmaliWriter) {
        val fiveRegInstruction = this.instruction as FiveRegisterInstruction
        val regCount = fiveRegInstruction.registerCount

        writer.write('{')
        when (regCount) {
            1 -> writeRegister(writer, fiveRegInstruction.registerC)
            2 -> {
                writeRegister(writer, fiveRegInstruction.registerC)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerD)
            }
            3 -> {
                writeRegister(writer, fiveRegInstruction.registerC)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerD)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerE)
            }
            4 -> {
                writeRegister(writer, fiveRegInstruction.registerC)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerD)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerE)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerF)
            }
            5 -> {
                writeRegister(writer, fiveRegInstruction.registerC)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerD)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerE)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerF)
                writer.write(", ")
                writeRegister(writer, fiveRegInstruction.registerG)
            }
        }
        writer.write('}')
    }

    @Throws(IOException::class)
    protected fun writeInvokeRangeRegisters(writer: BaksmaliWriter) {
        val rangeInstruction = this.instruction as RegisterRangeInstruction

        val regCount = rangeInstruction.registerCount
        if (regCount == 0) {
            writer.write("{}")
        } else {
            val startRegister = rangeInstruction.startRegister
            methodDef.registerFormatter!!.writeRegisterRange(
                writer, startRegister, startRegister + regCount - 1)
        }
    }

    @Throws(IOException::class)
    protected fun writeLiteral(writer: BaksmaliWriter) {
        writer.writeSignedIntOrLongTo((instruction as WideLiteralInstruction).wideLiteral)
    }

    @Throws(IOException::class)
    protected fun writeCommentIfLikelyFloat(writer: BaksmaliWriter) {
        writeCommentIfLikelyFloat(writer, (instruction as NarrowLiteralInstruction).narrowLiteral)
    }

    @Throws(IOException::class)
    protected fun writeCommentIfLikelyFloat(writer: BaksmaliWriter, value: Int) {
        if (NumberUtils.isLikelyFloat(value)) {
            writer.write("    # ")
            val fval = Float.fromBits(value)
            if (fval == Float.POSITIVE_INFINITY)
                writer.write("Float.POSITIVE_INFINITY")
            else if (fval == Float.NEGATIVE_INFINITY)
                writer.write("Float.NEGATIVE_INFINITY")
            else if (fval.isNaN())
                writer.write("Float.NaN")
            else if (fval == Float.MAX_VALUE)
                writer.write("Float.MAX_VALUE")
            else if (fval == Math.PI.toFloat())
                writer.write("(float)Math.PI")
            else if (fval == Math.E.toFloat())
                writer.write("(float)Math.E")
            else {
                writer.write(fval.toString())
                writer.write('f')
            }
        }
    }

    @Throws(IOException::class)
    protected fun writeCommentIfLikelyDouble(writer: BaksmaliWriter) {
        writeCommentIfLikelyDouble(writer, (instruction as WideLiteralInstruction).wideLiteral)
    }

    @Throws(IOException::class)
    protected fun writeCommentIfLikelyDouble(writer: BaksmaliWriter, value: Long) {
        if (NumberUtils.isLikelyDouble(value)) {
            writer.write("    # ")
            val dval = Double.fromBits(value)
            if (dval == Double.POSITIVE_INFINITY)
                writer.write("Double.POSITIVE_INFINITY")
            else if (dval == Double.NEGATIVE_INFINITY)
                writer.write("Double.NEGATIVE_INFINITY")
            else if (dval.isNaN())
                writer.write("Double.NaN")
            else if (dval == Double.MAX_VALUE)
                writer.write("Double.MAX_VALUE")
            else if (dval == Math.PI)
                writer.write("Math.PI")
            else if (dval == Math.E)
                writer.write("Math.E")
            else
                writer.write(dval.toString())
        }
    }

    @Throws(IOException::class)
    protected fun writeCommentIfResourceId(writer: BaksmaliWriter): Boolean {
        return writeCommentIfResourceId(writer, (instruction as NarrowLiteralInstruction).narrowLiteral)
    }

    @Throws(IOException::class)
    protected fun writeCommentIfResourceId(writer: BaksmaliWriter, value: Int): Boolean {
        val resourceIds = methodDef.classDef.options.resourceIds
        val resource = resourceIds[value]
        if (resource != null) {
            writer.write("    # ")
            writer.write(resource)
            return true
        }
        return false
    }

    @Throws(IOException::class)
    protected fun writeFieldOffset(writer: BaksmaliWriter) {
        writer.write("field@0x")
        writer.writeUnsignedLongAsHex((instruction as FieldOffsetInstruction).fieldOffset.toLong())
    }

    @Throws(IOException::class)
    protected fun writeInlineIndex(writer: BaksmaliWriter) {
        writer.write("inline@")
        writer.writeSignedIntAsDec((instruction as InlineIndexInstruction).inlineIndex)
    }

    @Throws(IOException::class)
    protected fun writeVtableIndex(writer: BaksmaliWriter) {
        writer.write("vtable@")
        writer.writeSignedIntAsDec((instruction as VtableIndexInstruction).vtableIndex)
    }
}
