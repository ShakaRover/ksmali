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

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.util.StringUtils
import java.io.IOException
import java.io.StringWriter
import java.io.Writer

@Deprecated("use DexFormatter instead")
fun getMethodDescriptor(methodReference: MethodReference): String {
    return getMethodDescriptor(methodReference, false)
}

@Deprecated("use DexFormatter instead")
fun getMethodDescriptor(methodReference: MethodReference, useImplicitReference: Boolean): String {
    return buildString {
        if (!useImplicitReference) {
            append(methodReference.definingClass)
            append("->")
        }
        append(methodReference.name)
        append('(')
        for (paramType in methodReference.parameterTypes) {
            append(paramType)
        }
        append(')')
        append(methodReference.returnType)
    }
}

@Deprecated("use DexFormatter instead")
fun getMethodProtoDescriptor(methodProtoReference: MethodProtoReference): String {
    val stringWriter = StringWriter()
    try {
        writeMethodProtoDescriptor(stringWriter, methodProtoReference)
    } catch (ex: IOException) {
        // IOException shouldn't happen for a StringWriter...
        throw RuntimeException(ex)
    }
    return stringWriter.toString()
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeMethodProtoDescriptor(writer: Writer, methodProtoReference: MethodProtoReference) {
    writer.write('('.code)
    for (paramType in methodProtoReference.parameterTypes) {
        writer.write(paramType.toString())
    }
    writer.write(')'.code)
    writer.write(methodProtoReference.returnType)
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeMethodDescriptor(writer: Writer, methodReference: MethodReference) {
    writeMethodDescriptor(writer, methodReference, false)
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeMethodDescriptor(writer: Writer, methodReference: MethodReference, useImplicitReference: Boolean) {
    if (!useImplicitReference) {
        writer.write(methodReference.definingClass)
        writer.write("->")
    }
    writer.write(methodReference.name)
    writer.write('('.code)
    for (paramType in methodReference.parameterTypes) {
        writer.write(paramType.toString())
    }
    writer.write(')'.code)
    writer.write(methodReference.returnType)
}

@Deprecated("use DexFormatter instead")
fun getFieldDescriptor(fieldReference: FieldReference): String {
    return getFieldDescriptor(fieldReference, false)
}

@Deprecated("use DexFormatter instead")
fun getFieldDescriptor(fieldReference: FieldReference, useImplicitReference: Boolean): String {
    return buildString {
        if (!useImplicitReference) {
            append(fieldReference.definingClass)
            append("->")
        }
        append(fieldReference.name)
        append(':')
        append(fieldReference.type)
    }
}

@Deprecated("use DexFormatter instead")
fun getShortFieldDescriptor(fieldReference: FieldReference): String {
    return buildString {
        append(fieldReference.name)
        append(':')
        append(fieldReference.type)
    }
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeFieldDescriptor(writer: Writer, fieldReference: FieldReference) {
    writeFieldDescriptor(writer, fieldReference, false)
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeFieldDescriptor(writer: Writer, fieldReference: FieldReference, implicitReference: Boolean) {
    if (!implicitReference) {
        writer.write(fieldReference.definingClass)
        writer.write("->")
    }
    writer.write(fieldReference.name)
    writer.write(':'.code)
    writer.write(fieldReference.type)
}

@Deprecated("use DexFormatter instead")
fun getMethodHandleString(methodHandleReference: MethodHandleReference): String {
    val stringWriter = StringWriter()
    try {
        writeMethodHandle(stringWriter, methodHandleReference)
    } catch (ex: IOException) {
        // IOException shouldn't happen for a StringWriter...
        throw RuntimeException(ex)
    }
    return stringWriter.toString()
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeMethodHandle(writer: Writer, methodHandleReference: MethodHandleReference) {
    writer.write(MethodHandleType.toString(methodHandleReference.methodHandleType))
    writer.write('@'.code)

    val memberReference = methodHandleReference.memberReference
    if (memberReference is MethodReference) {
        writeMethodDescriptor(writer, memberReference)
    } else {
        writeFieldDescriptor(writer, memberReference as FieldReference)
    }
}

@Deprecated("use DexFormatter instead")
fun getCallSiteString(callSiteReference: CallSiteReference): String {
    val stringWriter = StringWriter()
    try {
        writeCallSite(stringWriter, callSiteReference)
    } catch (ex: IOException) {
        // IOException shouldn't happen for a StringWriter...
        throw RuntimeException(ex)
    }
    return stringWriter.toString()
}

@Deprecated("use DexFormatter instead")
@Throws(IOException::class)
fun writeCallSite(writer: Writer, callSiteReference: CallSiteReference) {
    writer.write(callSiteReference.name)
    writer.write('('.code)
    writer.write('"'.code)
    StringUtils.writeEscapedString(writer, callSiteReference.methodName)
    writer.write('"'.code)
    writer.write(", ")
    writeMethodProtoDescriptor(writer, callSiteReference.methodProto)

    for (encodedValue in callSiteReference.extraArguments) {
        writer.write(", ")
        EncodedValueUtils.writeEncodedValue(writer, encodedValue)
    }
    writer.write(")@")
    val methodHandle = callSiteReference.methodHandle
    if (methodHandle.methodHandleType != MethodHandleType.INVOKE_STATIC) {
        throw IllegalArgumentException("The linker method handle for a call site must be of type invoke-static")
    }
    writeMethodDescriptor(writer, callSiteReference.methodHandle.memberReference as MethodReference)
}

@Deprecated("use DexFormatter instead")
fun getReferenceString(reference: Reference): String? {
    return getReferenceString(reference, null)
}

@Deprecated("use DexFormatter instead")
fun getReferenceString(reference: Reference, containingClass: String?): String? {
    if (reference is StringReference) {
        return "\"${StringUtils.escapeString(reference.string)}\""
    }
    if (reference is TypeReference) {
        return reference.type
    }
    if (reference is FieldReference) {
        val useImplicitReference = reference.definingClass == containingClass
        return getFieldDescriptor(reference, useImplicitReference)
    }
    if (reference is MethodReference) {
        val useImplicitReference = reference.definingClass == containingClass
        return getMethodDescriptor(reference, useImplicitReference)
    }
    if (reference is MethodProtoReference) {
        return getMethodProtoDescriptor(reference)
    }
    if (reference is MethodHandleReference) {
        return getMethodHandleString(reference)
    }
    if (reference is CallSiteReference) {
        return getCallSiteString(reference)
    }
    return null
}
