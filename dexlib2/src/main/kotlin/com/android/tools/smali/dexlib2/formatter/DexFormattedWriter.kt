/*
 * Copyright 2021, Google LLC
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

package com.android.tools.smali.dexlib2.formatter

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.BooleanEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue
import com.android.tools.smali.dexlib2.iface.value.CharEncodedValue
import com.android.tools.smali.dexlib2.iface.value.DoubleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.EnumEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FloatEncodedValue
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue
import com.android.tools.smali.dexlib2.iface.value.LongEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ShortEncodedValue
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue
import java.io.IOException
import java.io.Writer

/**
 * This class handles formatting and writing various types of items in a dex file to a Writer.
 */
open class DexFormattedWriter(protected val writer: Writer) : Writer() {

    /**
     * Write the method descriptor for the given [MethodReference].
     */
    @Throws(IOException::class)
    open fun writeMethodDescriptor(methodReference: MethodReference) {
        writeType(methodReference.definingClass)
        writer.write("->")
        writeSimpleName(methodReference.name)
        writer.write('('.code)
        for (paramType in methodReference.parameterTypes) {
            writeType(paramType)
        }
        writer.write(')'.code)
        writeType(methodReference.returnType)
    }

    /**
     * Write the short method descriptor for the given [MethodReference].
     *
     * The short method descriptor elides the class that the field is a member of.
     */
    @Throws(IOException::class)
    open fun writeShortMethodDescriptor(methodReference: MethodReference) {
        writeSimpleName(methodReference.name)
        writer.write('('.code)
        for (paramType in methodReference.parameterTypes) {
            writeType(paramType)
        }
        writer.write(')'.code)
        writeType(methodReference.returnType)
    }

    /**
     * Write the method proto descriptor for the given [MethodProtoReference].
     */
    @Throws(IOException::class)
    open fun writeMethodProtoDescriptor(protoReference: MethodProtoReference) {
        writer.write('('.code)
        for (paramType in protoReference.parameterTypes) {
            writeType(paramType)
        }
        writer.write(')'.code)
        writeType(protoReference.returnType)
    }

    /**
     * Write the field descriptor for the given [FieldReference].
     */
    @Throws(IOException::class)
    open fun writeFieldDescriptor(fieldReference: FieldReference) {
        writeType(fieldReference.definingClass)
        writer.write("->")
        writeSimpleName(fieldReference.name)
        writer.write(':'.code)
        writeType(fieldReference.type)
    }

    /**
     * Write the short field descriptor for the given [FieldReference].
     *
     * The short field descriptor typically elides the class that the field is a member of.
     */
    @Throws(IOException::class)
    open fun writeShortFieldDescriptor(fieldReference: FieldReference) {
        writeSimpleName(fieldReference.name)
        writer.write(':'.code)
        writeType(fieldReference.type)
    }

    /**
     * Write the given [MethodHandleReference].
     */
    @Throws(IOException::class)
    open fun writeMethodHandle(methodHandleReference: MethodHandleReference) {
        writer.write(MethodHandleType.toString(methodHandleReference.methodHandleType))
        writer.write('@'.code)

        val memberReference = methodHandleReference.memberReference
        if (memberReference is MethodReference) {
            writeMethodDescriptor(memberReference)
        } else {
            writeFieldDescriptor(memberReference as FieldReference)
        }
    }

    /**
     * Write the given [CallSiteReference].
     */
    @Throws(IOException::class)
    open fun writeCallSite(callSiteReference: CallSiteReference) {
        writeSimpleName(callSiteReference.name)
        writer.write('('.code)
        writeQuotedString(callSiteReference.methodName)
        writer.write(", ")
        writeMethodProtoDescriptor(callSiteReference.methodProto)

        for (encodedValue in callSiteReference.extraArguments) {
            writer.write(", ")
            writeEncodedValue(encodedValue)
        }
        writer.write(")@")
        val methodHandle = callSiteReference.methodHandle
        if (methodHandle.methodHandleType != MethodHandleType.INVOKE_STATIC) {
            throw IllegalArgumentException("The linker method handle for a call site must be of type invoke-static")
        }
        writeMethodDescriptor(callSiteReference.methodHandle.memberReference as MethodReference)
    }

    /**
     * Write the given type.
     */
    @Throws(IOException::class)
    open fun writeType(type: CharSequence) {
        for (i in type.indices) {
            val c = type[i]
            if (c == 'L') {
                writeClass(type.subSequence(i, type.length))
                return
            } else if (c == '[') {
                writer.write(c.code)
            } else if (c == 'Z' ||
                c == 'B' ||
                c == 'S' ||
                c == 'C' ||
                c == 'I' ||
                c == 'J' ||
                c == 'F' ||
                c == 'D' ||
                c == 'V') {
                writer.write(c.code)

                if (i != type.length - 1) {
                    throw IllegalArgumentException(
                        "Invalid type string: ${type}"
                    )
                }
                return
            } else {
                throw IllegalArgumentException(
                    "Invalid type string: ${type}"
                )
            }
        }

        // Any valid type would have returned from within the loop.
        throw IllegalArgumentException(
            "Invalid type string: ${type}"
        )
    }

    @Throws(IOException::class)
    protected open fun writeClass(type: CharSequence) {
        assert(type[0] == 'L')

        writer.write(type[0].code)

        var startIndex = 1
        var i = startIndex
        while (i < type.length) {
            val c = type[i]

            if (c == '/') {
                if (i == startIndex) {
                    throw IllegalArgumentException(
                        "Invalid type string: ${type}"
                    )
                }

                writeSimpleName(type.subSequence(startIndex, i))
                writer.write(type[i].code)
                startIndex = i + 1
            } else if (c == ';') {
                if (i == startIndex) {
                    throw IllegalArgumentException(
                        "Invalid type string: ${type}"
                    )
                }

                writeSimpleName(type.subSequence(startIndex, i))
                writer.write(type[i].code)
                break
            }
            i++
        }

        if (i != type.length - 1 || type[i] != ';') {
            throw IllegalArgumentException(
                "Invalid type string: ${type}"
            )
        }
    }

    /**
     * Writes the given simple name.
     *
     * @param simpleName The [simple name](https://source.android.com/devices/tech/dalvik/dex-format#simplename)
     *                   to write.
     */
    @Throws(IOException::class)
    protected open fun writeSimpleName(simpleName: CharSequence) {
        writer.append(simpleName)
    }

    /**
     * Write the given quoted string.
     *
     * This includes the beginning and ending quotation marks, and the string value is be escaped as necessary.
     */
    @Throws(IOException::class)
    open fun writeQuotedString(charSequence: CharSequence) {
        writer.write('"'.code)

        val string = charSequence.toString()
        for (i in string.indices) {
            val c = string[i]

            if (c >= ' ' && c.code < 0x7f) {
                if (c == '\'' || c == '"' || c == '\\') {
                    writer.write('\\'.code)
                }
                writer.write(c.code)
                continue
            } else if (c.code <= 0x7f) {
                if (c == '\n') {
                    writer.write("\\n"); continue
                }
                if (c == '\r') {
                    writer.write("\\r"); continue
                }
                if (c == '\t') {
                    writer.write("\\t"); continue
                }
            }

            writer.write("\\u")
            writer.write(Character.forDigit(c.code shr 12, 16).code)
            writer.write(Character.forDigit((c.code shr 8) and 0x0f, 16).code)
            writer.write(Character.forDigit((c.code shr 4) and 0x0f, 16).code)
            writer.write(Character.forDigit(c.code and 0x0f, 16).code)
        }

        writer.write('"'.code)
    }

    /**
     * Write the given [EncodedValue].
     */
    @Throws(IOException::class)
    open fun writeEncodedValue(encodedValue: EncodedValue) {
        when (encodedValue.valueType) {
            ValueType.BOOLEAN ->
                writer.write(java.lang.Boolean.toString((encodedValue as BooleanEncodedValue).value))
            ValueType.BYTE ->
                writer.write("0x%x".format((encodedValue as ByteEncodedValue).value))
            ValueType.CHAR ->
                writer.write("0x%x".format((encodedValue as CharEncodedValue).value.code))
            ValueType.SHORT ->
                writer.write("0x%x".format((encodedValue as ShortEncodedValue).value))
            ValueType.INT ->
                writer.write("0x%x".format((encodedValue as IntEncodedValue).value))
            ValueType.LONG ->
                writer.write("0x%x".format((encodedValue as LongEncodedValue).value))
            ValueType.FLOAT ->
                writer.write(java.lang.Float.toString((encodedValue as FloatEncodedValue).value))
            ValueType.DOUBLE ->
                writer.write(java.lang.Double.toString((encodedValue as DoubleEncodedValue).value))
            ValueType.ANNOTATION ->
                writeAnnotation(encodedValue as AnnotationEncodedValue)
            ValueType.ARRAY ->
                writeArray(encodedValue as ArrayEncodedValue)
            ValueType.STRING ->
                writeQuotedString((encodedValue as StringEncodedValue).value)
            ValueType.FIELD ->
                writeFieldDescriptor((encodedValue as FieldEncodedValue).value)
            ValueType.ENUM ->
                writeFieldDescriptor((encodedValue as EnumEncodedValue).value)
            ValueType.METHOD ->
                writeMethodDescriptor((encodedValue as MethodEncodedValue).value)
            ValueType.TYPE ->
                writeType((encodedValue as TypeEncodedValue).value)
            ValueType.METHOD_TYPE ->
                writeMethodProtoDescriptor((encodedValue as MethodTypeEncodedValue).value)
            ValueType.METHOD_HANDLE ->
                writeMethodHandle((encodedValue as MethodHandleEncodedValue).value)
            ValueType.NULL ->
                writer.write("null")
            else ->
                throw IllegalArgumentException("Unknown encoded value type")
        }
    }

    /**
     * Write the given [AnnotationEncodedValue].
     */
    @Throws(IOException::class)
    protected open fun writeAnnotation(annotation: AnnotationEncodedValue) {
        writer.write("Annotation[")
        writeType(annotation.type)

        val elements: Set<AnnotationElement> = annotation.elements
        for (element in elements) {
            writer.write(", ")
            writeSimpleName(element.name)
            writer.write('='.code)
            writeEncodedValue(element.value)
        }

        writer.write(']'.code)
    }

    /**
     * Write the given [ArrayEncodedValue].
     */
    @Throws(IOException::class)
    protected open fun writeArray(array: ArrayEncodedValue) {
        writer.write("Array[")

        var first = true
        for (element in array.value) {
            if (first) {
                first = false
            } else {
                writer.write(", ")
            }
            writeEncodedValue(element)
        }

        writer.write(']'.code)
    }

    /**
     * Write the given [Reference].
     */
    @Throws(IOException::class)
    open fun writeReference(reference: Reference) {
        when (reference) {
            is StringReference -> writeQuotedString(reference)
            is TypeReference -> writeType(reference)
            is FieldReference -> writeFieldDescriptor(reference)
            is MethodReference -> writeMethodDescriptor(reference)
            is MethodProtoReference -> writeMethodProtoDescriptor(reference)
            is MethodHandleReference -> writeMethodHandle(reference)
            is CallSiteReference -> writeCallSite(reference)
            else -> throw IllegalArgumentException(
                "Not a known reference type: ${reference.javaClass}"
            )
        }
    }

    @Throws(IOException::class)
    override fun write(c: Int) {
        writer.write(c)
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray) {
        writer.write(cbuf)
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        writer.write(cbuf, off, len)
    }

    @Throws(IOException::class)
    override fun write(str: String) {
        writer.write(str)
    }

    @Throws(IOException::class)
    override fun write(str: String, off: Int, len: Int) {
        writer.write(str, off, len)
    }

    @Throws(IOException::class)
    override fun append(csq: CharSequence?): Writer {
        return writer.append(csq)
    }

    @Throws(IOException::class)
    override fun append(csq: CharSequence?, start: Int, end: Int): Writer {
        return writer.append(csq, start, end)
    }

    @Throws(IOException::class)
    override fun append(c: Char): Writer {
        return writer.append(c)
    }

    @Throws(IOException::class)
    override fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        writer.close()
    }
}
