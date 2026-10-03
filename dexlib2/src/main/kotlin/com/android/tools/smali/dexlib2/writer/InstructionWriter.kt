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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
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
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ExceptionWithContext
import java.io.IOException
import java.util.Comparator

class InstructionWriter<StringRef : StringReference, TypeRef : TypeReference, FieldRefKey : FieldReference,
    MethodRefKey : MethodReference, ProtoRefKey : MethodProtoReference, MethodHandleKey : MethodHandleReference,
    CallSiteKey : CallSiteReference>
internal constructor(
    private val opcodes: Opcodes,
    private val writer: DexDataWriter,
    private val stringSection: StringSection<*, StringRef>,
    private val typeSection: TypeSection<*, *, TypeRef>,
    private val fieldSection: FieldSection<*, *, FieldRefKey, *>,
    private val methodSection: MethodSection<*, *, *, MethodRefKey, *>,
    private val protoSection: ProtoSection<*, *, ProtoRefKey, *>,
    private val methodHandleSection: MethodHandleSection<MethodHandleKey, *, *>,
    private val callSiteSection: CallSiteSection<CallSiteKey, *>,
) {
    private fun getOpcodeValue(opcode: Opcode): Short {
        val value = opcodes.getOpcodeValue(opcode)
        if (value == null) {
            throw ExceptionWithContext("Instruction %s is invalid for api %d", opcode.mnemonic, opcodes.api)
        }
        return value
    }

    fun write(instruction: Instruction10t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction10x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(0)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction11n) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.narrowLiteral))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction11x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction12x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.registerB))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction20bc) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.verificationError)
            writer.writeUshort(getReferenceIndex(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction20t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(0)
            writer.writeShort(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction21c) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeUshort(getReferenceIndex(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction21ih) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeShort(instruction.hatLiteral.toInt())
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction21lh) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeShort(instruction.hatLiteral.toInt())
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction21s) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeShort(instruction.narrowLiteral)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction21t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeShort(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }


    fun write(instruction: Instruction22b) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.write(instruction.registerB)
            writer.write(instruction.narrowLiteral)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction22c) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.registerB))
            writer.writeUshort(getReferenceIndex(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction22cs) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.registerB))
            writer.writeUshort(instruction.fieldOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction22s) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.registerB))
            writer.writeShort(instruction.narrowLiteral)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction22t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerA, instruction.registerB))
            writer.writeShort(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction22x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeUshort(instruction.registerB)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction23x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.write(instruction.registerB)
            writer.write(instruction.registerC)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction30t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(0)
            writer.writeInt(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction31c) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeInt(getReferenceIndex(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction31i) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeInt(instruction.narrowLiteral)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction31t) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeInt(instruction.codeOffset)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction32x) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(0)
            writer.writeUshort(instruction.registerA)
            writer.writeUshort(instruction.registerB)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction35c) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerG, instruction.registerCount))
            writer.writeUshort(getReferenceIndex(instruction))
            writer.write(packNibbles(instruction.registerC, instruction.registerD))
            writer.write(packNibbles(instruction.registerE, instruction.registerF))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction35mi) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerG, instruction.registerCount))
            writer.writeUshort(instruction.inlineIndex)
            writer.write(packNibbles(instruction.registerC, instruction.registerD))
            writer.write(packNibbles(instruction.registerE, instruction.registerF))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction35ms) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerG, instruction.registerCount))
            writer.writeUshort(instruction.vtableIndex)
            writer.write(packNibbles(instruction.registerC, instruction.registerD))
            writer.write(packNibbles(instruction.registerE, instruction.registerF))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction3rc) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerCount)
            writer.writeUshort(getReferenceIndex(instruction))
            writer.writeUshort(instruction.startRegister)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction3rmi) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerCount)
            writer.writeUshort(instruction.inlineIndex)
            writer.writeUshort(instruction.startRegister)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction3rms) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerCount)
            writer.writeUshort(instruction.vtableIndex)
            writer.writeUshort(instruction.startRegister)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction45cc) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(packNibbles(instruction.registerG, instruction.registerCount))
            writer.writeUshort(getReferenceIndex(instruction))
            writer.write(packNibbles(instruction.registerC, instruction.registerD))
            writer.write(packNibbles(instruction.registerE, instruction.registerF))
            writer.writeUshort(getReference2Index(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction4rcc) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerCount)
            writer.writeUshort(getReferenceIndex(instruction))
            writer.writeUshort(instruction.startRegister)
            writer.writeUshort(getReference2Index(instruction))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: Instruction51l) {
        try {
            writer.write(getOpcodeValue(instruction.opcode).toInt())
            writer.write(instruction.registerA)
            writer.writeLong(instruction.wideLiteral)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: ArrayPayload) {
        try {
            writer.writeUshort(getOpcodeValue(instruction.opcode).toInt())
            writer.writeUshort(instruction.elementWidth)
            val elements = instruction.arrayElements
            writer.writeInt(elements.size)
            when (instruction.elementWidth) {
                1 -> for (element in elements) {
                    writer.write(element.toByte().toInt())
                }
                2 -> for (element in elements) {
                    writer.writeShort(element.toShort().toInt())
                }
                4 -> for (element in elements) {
                    writer.writeInt(element.toInt())
                }
                8 -> for (element in elements) {
                    writer.writeLong(element.toLong())
                }
            }
            if ((writer.position and 1) != 0) {
                writer.write(0)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun write(instruction: SparseSwitchPayload) {
        try {
            writer.writeUbyte(0)
            writer.writeUbyte(getOpcodeValue(instruction.opcode).toInt() shr 8)
            val elements = CollectionUtils.immutableSortedCopy(
                instruction.switchElements, switchElementComparator
            )

            writer.writeUshort(elements.size)
            for (element in elements) {
                writer.writeInt(element.key)
            }
            for (element in elements) {
                writer.writeInt(element.offset)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private val switchElementComparator = Comparator<SwitchElement> { element1, element2 ->
        element1.key.compareTo(element2.key)
    }

    fun write(instruction: PackedSwitchPayload) {
        try {
            writer.writeUbyte(0)
            writer.writeUbyte(getOpcodeValue(instruction.opcode).toInt() shr 8)
            val elements = instruction.switchElements
            writer.writeUshort(elements.size)
            if (elements.size == 0) {
                writer.writeInt(0)
            } else {
                writer.writeInt(elements[0].key)
                for (element in elements) {
                    writer.writeInt(element.offset)
                }
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }


    private fun packNibbles(a: Int, b: Int): Int {
        return (b shl 4) or a
    }

    private fun getReferenceIndex(referenceInstruction: ReferenceInstruction): Int {
        return getReferenceIndex(referenceInstruction.referenceType, referenceInstruction.reference)
    }

    private fun getReference2Index(referenceInstruction: DualReferenceInstruction): Int {
        return getReferenceIndex(referenceInstruction.referenceType2, referenceInstruction.reference2)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getReferenceIndex(referenceType: Int, reference: Reference): Int {
        return when (referenceType) {
            ReferenceType.FIELD -> fieldSection.getItemIndex(reference as FieldRefKey)
            ReferenceType.METHOD -> methodSection.getItemIndex(reference as MethodRefKey)
            ReferenceType.STRING -> stringSection.getItemIndex(reference as StringRef)
            ReferenceType.TYPE -> typeSection.getItemIndex(reference as TypeRef)
            ReferenceType.METHOD_PROTO -> protoSection.getItemIndex(reference as ProtoRefKey)
            ReferenceType.METHOD_HANDLE -> methodHandleSection.getItemIndex(reference as MethodHandleKey)
            ReferenceType.CALL_SITE -> callSiteSection.getItemIndex(reference as CallSiteKey)
            else -> throw ExceptionWithContext("Unknown reference type: %d", referenceType)
        }
    }

    companion object {
        internal fun <StringRef : StringReference, TypeRef : TypeReference, FieldRefKey : FieldReference,
            MethodRefKey : MethodReference, ProtoRefKey : MethodProtoReference,
            MethodHandleKey : MethodHandleReference, CallSiteKey : CallSiteReference>
            makeInstructionWriter(
                opcodes: Opcodes,
                writer: DexDataWriter,
                stringSection: StringSection<*, StringRef>,
                typeSection: TypeSection<*, *, TypeRef>,
                fieldSection: FieldSection<*, *, FieldRefKey, *>,
                methodSection: MethodSection<*, *, *, MethodRefKey, *>,
                protoSection: ProtoSection<*, *, ProtoRefKey, *>,
                methodHandleSection: MethodHandleSection<MethodHandleKey, *, *>,
                callSiteSection: CallSiteSection<CallSiteKey, *>,
            ): InstructionWriter<StringRef, TypeRef, FieldRefKey, MethodRefKey, ProtoRefKey, MethodHandleKey, CallSiteKey> {
            return InstructionWriter(
                opcodes, writer, stringSection, typeSection, fieldSection, methodSection, protoSection,
                methodHandleSection, callSiteSection
            )
        }
    }
}
