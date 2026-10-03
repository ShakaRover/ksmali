/*
 * Copyright 2014, Google LLC
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

package com.android.tools.smali.dexlib2.rewriter

import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction20bc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction45cc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction4rcc
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.ExceptionWithContext

open class InstructionRewriter(
    @JvmField protected val rewriters: Rewriters
) : Rewriter<Instruction> {
    override fun rewrite(instruction: Instruction): Instruction {
        if (instruction is ReferenceInstruction) {
            when (instruction.opcode.format) {
                Format.Format20bc -> return RewrittenInstruction20bc(instruction as Instruction20bc)
                Format.Format21c -> return RewrittenInstruction21c(instruction as Instruction21c)
                Format.Format22c -> return RewrittenInstruction22c(instruction as Instruction22c)
                Format.Format31c -> return RewrittenInstruction31c(instruction as Instruction31c)
                Format.Format35c -> return RewrittenInstruction35c(instruction as Instruction35c)
                Format.Format3rc -> return RewrittenInstruction3rc(instruction as Instruction3rc)
                Format.Format45cc -> return RewrittenInstruction45cc(instruction as Instruction45cc)
                Format.Format4rcc -> return RewrittenInstruction4rcc(instruction as Instruction4rcc)
                else -> throw IllegalArgumentException()
            }
        }
        return instruction
    }

    private fun rewriteReference(type: Int, reference: Reference): Reference {
        when (type) {
            ReferenceType.TYPE -> return RewriterUtils.rewriteTypeReference(
                rewriters.typeRewriter,
                reference as TypeReference
            )
            ReferenceType.FIELD -> return rewriters.fieldReferenceRewriter.rewrite(
                reference as FieldReference
            )
            ReferenceType.METHOD -> return rewriters.methodReferenceRewriter.rewrite(
                reference as MethodReference
            )
            ReferenceType.STRING -> return reference
            ReferenceType.METHOD_PROTO -> return RewriterUtils.rewriteMethodProtoReference(
                rewriters.typeRewriter,
                reference as MethodProtoReference
            )
            ReferenceType.METHOD_HANDLE -> return RewriterUtils.rewriteMethodHandleReference(
                rewriters,
                reference as MethodHandleReference
            )
            ReferenceType.CALL_SITE -> return rewriters.callSiteReferenceRewriter.rewrite(
                reference as CallSiteReference
            )
            else -> throw ExceptionWithContext("Invalid reference type: %d", type)
        }
    }

    protected open inner class BaseRewrittenReferenceInstruction<T : ReferenceInstruction>(
        protected val instruction: T
    ) : ReferenceInstruction {
        override val reference: Reference
            get() = rewriteReference(instruction.referenceType, instruction.reference)

        override val referenceType: Int
            get() = instruction.referenceType

        override val opcode: Opcode
            get() = instruction.opcode

        override val codeUnits: Int
            get() = instruction.codeUnits
    }

    protected inner class RewrittenInstruction20bc(
        instruction: Instruction20bc
    ) : BaseRewrittenReferenceInstruction<Instruction20bc>(instruction), Instruction20bc {
        override val verificationError: Int
            get() = instruction.verificationError
    }

    protected inner class RewrittenInstruction21c(
        instruction: Instruction21c
    ) : BaseRewrittenReferenceInstruction<Instruction21c>(instruction), Instruction21c {
        override val registerA: Int
            get() = instruction.registerA
    }

    protected inner class RewrittenInstruction22c(
        instruction: Instruction22c
    ) : BaseRewrittenReferenceInstruction<Instruction22c>(instruction), Instruction22c {
        override val registerA: Int
            get() = instruction.registerA

        override val registerB: Int
            get() = instruction.registerB
    }

    protected inner class RewrittenInstruction31c(
        instruction: Instruction31c
    ) : BaseRewrittenReferenceInstruction<Instruction31c>(instruction), Instruction31c {
        override val registerA: Int
            get() = instruction.registerA
    }

    protected inner class RewrittenInstruction35c(
        instruction: Instruction35c
    ) : BaseRewrittenReferenceInstruction<Instruction35c>(instruction), Instruction35c {
        override val registerC: Int
            get() = instruction.registerC
        override val registerD: Int
            get() = instruction.registerD
        override val registerE: Int
            get() = instruction.registerE
        override val registerF: Int
            get() = instruction.registerF
        override val registerG: Int
            get() = instruction.registerG
        override val registerCount: Int
            get() = instruction.registerCount
    }

    protected inner class RewrittenInstruction3rc(
        instruction: Instruction3rc
    ) : BaseRewrittenReferenceInstruction<Instruction3rc>(instruction), Instruction3rc {
        override val startRegister: Int
            get() = instruction.startRegister
        override val registerCount: Int
            get() = instruction.registerCount
    }

    protected open inner class BaseRewrittenDualReferenceInstruction<T : DualReferenceInstruction>(
        instruction: T
    ) : BaseRewrittenReferenceInstruction<T>(instruction), DualReferenceInstruction {
        override val reference2: Reference
            get() = rewriteReference(instruction.referenceType2, instruction.reference2)

        override val referenceType2: Int
            get() = instruction.referenceType2
    }

    protected inner class RewrittenInstruction45cc(
        instruction: Instruction45cc
    ) : BaseRewrittenDualReferenceInstruction<Instruction45cc>(instruction), Instruction45cc {
        override val registerC: Int
            get() = instruction.registerC
        override val registerD: Int
            get() = instruction.registerD
        override val registerE: Int
            get() = instruction.registerE
        override val registerF: Int
            get() = instruction.registerF
        override val registerG: Int
            get() = instruction.registerG
        override val registerCount: Int
            get() = instruction.registerCount
    }

    protected inner class RewrittenInstruction4rcc(
        instruction: Instruction4rcc
    ) : BaseRewrittenDualReferenceInstruction<Instruction4rcc>(instruction), Instruction4rcc {
        override val startRegister: Int
            get() = instruction.startRegister
        override val registerCount: Int
            get() = instruction.registerCount
    }
}
