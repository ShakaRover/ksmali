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


package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.VerificationError
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

fun checkFormat(opcode: Opcode, expectedFormat: Format) {
    if (opcode.format != expectedFormat) {
        throw IllegalArgumentException(
            "Invalid opcode ${opcode.mnemonic} for ${expectedFormat.name}"
        )
    }
}

fun checkNibbleRegister(register: Int): Int {
    if ((register and 0xFFFFFFF0.toInt()) != 0) {
        throw IllegalArgumentException(
            "Invalid register: v${register}. Must be between v0 and v15, inclusive."
        )
    }
    return register
}

fun checkByteRegister(register: Int): Int {
    if ((register and 0xFFFFFF00.toInt()) != 0) {
        throw IllegalArgumentException(
            "Invalid register: v${register}. Must be between v0 and v255, inclusive."
        )
    }
    return register
}

fun checkShortRegister(register: Int): Int {
    if ((register and 0xFFFF0000.toInt()) != 0) {
        throw IllegalArgumentException(
            "Invalid register: v${register}. Must be between v0 and v65535, inclusive."
        )
    }
    return register
}

fun checkNibbleLiteral(literal: Int): Int {
    if (literal < -8 || literal > 7) {
        throw IllegalArgumentException(
            "Invalid literal value: ${literal}. Must be between -8 and 7, inclusive."
        )
    }
    return literal
}

fun checkByteLiteral(literal: Int): Int {
    if (literal < -128 || literal > 127) {
        throw IllegalArgumentException(
            "Invalid literal value: ${literal}. Must be between -128 and 127, inclusive."
        )
    }
    return literal
}

fun checkShortLiteral(literal: Int): Int {
    if (literal < -32768 || literal > 32767) {
        throw IllegalArgumentException(
            "Invalid literal value: ${literal}. Must be between -32768 and 32767, inclusive."
        )
    }
    return literal
}

fun checkIntegerHatLiteral(literal: Int): Int {
    if ((literal and 0xFFFF) != 0) {
        throw IllegalArgumentException(
            "Invalid literal value: ${literal}. Low 16 bits must be zeroed out."
        )
    }
    return literal
}

fun checkLongHatLiteral(literal: Long): Long {
    if ((literal and 0xFFFFFFFFFFFFL) != 0L) {
        throw IllegalArgumentException(
            "Invalid literal value: ${literal}. Low 48 bits must be zeroed out."
        )
    }
    return literal
}

fun checkByteCodeOffset(offset: Int): Int {
    if (offset < -128 || offset > 127) {
        throw IllegalArgumentException(
            "Invalid code offset: ${offset}. Must be between -128 and 127, inclusive."
        )
    }
    return offset
}

fun checkShortCodeOffset(offset: Int): Int {
    if (offset < -32768 || offset > 32767) {
        throw IllegalArgumentException(
            "Invalid code offset: ${offset}. Must be between -32768 and 32767, inclusive."
        )
    }
    return offset
}

fun check35cAnd45ccRegisterCount(registerCount: Int): Int {
    if (registerCount < 0 || registerCount > 5) {
        throw IllegalArgumentException(
            "Invalid register count: ${registerCount}. Must be between 0 and 5, inclusive."
        )
    }
    return registerCount
}

fun checkRegisterRangeCount(registerCount: Int): Int {
    if ((registerCount and 0xFFFFFF00.toInt()) != 0) {
        throw IllegalArgumentException(
            "Invalid register count: ${registerCount}. Must be between 0 and 255, inclusive."
        )
    }
    return registerCount
}

fun checkValueArg(valueArg: Int, maxValue: Int) {
    if (valueArg > maxValue) {
        if (maxValue == 0) {
            throw IllegalArgumentException(
                "Invalid value_arg value ${valueArg} for an encoded_value. Expecting 0"
            )
        }
        throw IllegalArgumentException(
            "Invalid value_arg value ${valueArg} for an encoded_value. Expecting 0..${maxValue}, inclusive"
        )
    }
}

fun checkFieldOffset(fieldOffset: Int): Int {
    if (fieldOffset < 0 || fieldOffset > 65535) {
        throw IllegalArgumentException(
            "Invalid field offset: 0x%x. Must be between 0x0000 and 0xFFFF inclusive".format(fieldOffset)
        )
    }
    return fieldOffset
}

fun checkVtableIndex(vtableIndex: Int): Int {
    if (vtableIndex < 0 || vtableIndex > 65535) {
        throw IllegalArgumentException(
            "Invalid vtable index: ${vtableIndex}. Must be between 0 and 65535, inclusive"
        )
    }
    return vtableIndex
}

fun checkInlineIndex(inlineIndex: Int): Int {
    if (inlineIndex < 0 || inlineIndex > 65535) {
        throw IllegalArgumentException(
            "Invalid inline index: ${inlineIndex}. Must be between 0 and 65535, inclusive"
        )
    }
    return inlineIndex
}

fun checkVerificationError(verificationError: Int): Int {
    if (!VerificationError.isValidVerificationError(verificationError)) {
        throw IllegalArgumentException(
            "Invalid verification error value: ${verificationError}. Must be between 1 and 9, inclusive"
        )
    }
    return verificationError
}

fun <C : Collection<@JvmWildcard SwitchElement>> checkSequentialOrderedKeys(elements: C): C {
    var previousKey: Int? = null
    for (element in elements) {
        val key = element.key
        if (previousKey != null && previousKey + 1 != key) {
            throw IllegalArgumentException("SwitchElement set is not sequential and ordered")
        }

        previousKey = key
    }

    return elements
}

fun checkArrayPayloadElementWidth(elementWidth: Int): Int {
    when (elementWidth) {
        1, 2, 4, 8 -> return elementWidth
        else -> throw IllegalArgumentException("Not a valid element width: ${elementWidth}")
    }
}

fun <L : List<@JvmWildcard Number>> checkArrayPayloadElements(elementWidth: Int, elements: L): L {
    val maxValue: Long
    val minValue: Long
    if (elementWidth == 2) {
        //Could be a short or character.
        //Characters are unsigned, Shorts are signed.
        //Short.MAX_VALUE = 32767
        //Short.MIN_VALUE = -32768
        //(int) Character.MAX_VALUE = 65535
        //(int) Character.MIN_VALUE = 0
        //See https://docs.oracle.com/javase/specs/jvms/se6/html/Overview.doc.html for details
        //As such, we use the combined interval range of both.
        maxValue = Character.MAX_VALUE.code.toLong()
        minValue = Short.MIN_VALUE.toLong()
    } else {
        //Two's complement.
        maxValue = (1L shl ((8 * elementWidth) - 1)) - 1
        minValue = -maxValue - 1
    }
    for (element in elements) {
        if (element.toLong() < minValue || element.toLong() > maxValue) {
            throw IllegalArgumentException(
                "${element.toLong()} does not fit into a ${elementWidth}-byte signed integer"
            )
        }
    }

    return elements
}

fun <T : Reference> checkReference(referenceType: Int, reference: T): T {
    when (referenceType) {
        ReferenceType.STRING ->
            if (reference !is StringReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a string reference")
            }
        ReferenceType.TYPE ->
            if (reference !is TypeReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a type reference")
            }
        ReferenceType.FIELD ->
            if (reference !is FieldReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a field reference")
            }
        ReferenceType.METHOD ->
            if (reference !is MethodReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a method reference")
            }
        ReferenceType.METHOD_PROTO ->
            if (reference !is MethodProtoReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a method proto reference")
            }
        ReferenceType.METHOD_HANDLE ->
            if (reference !is MethodHandleReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a method handle reference")
            }
        ReferenceType.CALL_SITE ->
            if (reference !is CallSiteReference) {
                throw IllegalArgumentException("Invalid reference type, expecting a call site reference")
            }
        else ->
            throw IllegalArgumentException("Not a valid reference type: ${referenceType}")
    }
    return reference
}
