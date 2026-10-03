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

package com.android.tools.smali.baksmali.formatter

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.formatter.DexFormattedWriter
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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
import com.android.tools.smali.util.IndentingWriter
import java.io.IOException
import java.io.Writer

/**
 * A specialized version of DexFormattedWriter that handles quoting
 * [simple names](https://source.android.com/devices/tech/dalvik/dex-format#simplename) containing spaces.
 */
open class BaksmaliWriter : DexFormattedWriter {

    private val classContext: String?

    @JvmField
    protected val buffer = CharArray(24)

    constructor(writer: Writer) : this(writer, null)

    /**
     * Constructs a new BaksmaliWriter
     *
     * @param writer The [IndentingWriter] to write to
     * @param classContext If provided, the class will be elided from any field/method descriptors whose containing
     *                     class match this instance's classContext.
     */
    constructor(writer: Writer, classContext: String?) : super(
        if (writer is IndentingWriter) writer else IndentingWriter(writer)) {
        this.classContext = classContext
    }

    @Throws(IOException::class)
    fun write(c: Char) {
        super.write(c.code)
    }

    @Throws(IOException::class)
    override fun writeMethodDescriptor(methodReference: MethodReference) {
        if (methodReference.definingClass == classContext) {
            writeShortMethodDescriptor(methodReference)
        } else {
            super.writeMethodDescriptor(methodReference)
        }
    }

    @Throws(IOException::class)
    override fun writeFieldDescriptor(fieldReference: FieldReference) {
        if (fieldReference.definingClass == classContext) {
            writeShortFieldDescriptor(fieldReference)
        } else {
            super.writeFieldDescriptor(fieldReference)
        }
    }

    @Throws(IOException::class)
    override fun writeClass(type: CharSequence) {
        assert(type[0] == 'L')

        write(type[0])

        val startIndex0 = 1
        var startIndex = startIndex0
        var hasSpace = false
        var i: Int
        i = startIndex0
        while (i < type.length) {
            val c = type[i]

            if (Character.getType(c) == Character.SPACE_SEPARATOR.toInt()) {
                hasSpace = true
            } else if (c == '/') {
                if (i == startIndex) {
                    throw IllegalArgumentException(
                        String.format("Invalid type string: %s", type))
                }

                writeSimpleName(type.subSequence(startIndex, i), hasSpace)
                write(type[i])
                hasSpace = false
                startIndex = i + 1
            } else if (c == ';') {
                if (i == startIndex) {
                    throw IllegalArgumentException(
                        String.format("Invalid type string: %s", type))
                }

                writeSimpleName(type.subSequence(startIndex, i), hasSpace)
                write(type[i])
                break
            }
            i++
        }

        if (i != type.length - 1 || type[i] != ';') {
            throw IllegalArgumentException(
                String.format("Invalid type string: %s", type))
        }
    }

    @Throws(IOException::class)
    public override fun writeSimpleName(simpleName: CharSequence) {
        var hasSpace = false
        for (i in 0 until simpleName.length) {
            if (Character.getType(simpleName[i]) == Character.SPACE_SEPARATOR.toInt()) {
                hasSpace = true
                break
            }
        }
        writeSimpleName(simpleName, hasSpace)
    }

    /**
     * Writes the given simple name, potentially quoting it if requested.
     *
     * The simple name will be quoted with backticks if quoted is true
     *
     * A simple name should typically be quoted if it is meant to be human readable, and it contains spaces.
     *
     * @param simpleName The simple name to write. See: https://source.android.com/devices/tech/dalvik/dex-format#simplename
     */
    @Throws(IOException::class)
    fun writeSimpleName(simpleName: CharSequence, quoted: Boolean) {
        if (quoted) {
            write('`')
        }
        writer.append(simpleName)
        if (quoted) {
            write('`')
        }
    }

    @Throws(IOException::class)
    override fun writeEncodedValue(encodedValue: EncodedValue) {
        when (encodedValue.valueType) {
            ValueType.BOOLEAN ->
                writeBooleanEncodedValue(encodedValue as BooleanEncodedValue)
            ValueType.BYTE ->
                writeIntegralValue((encodedValue as ByteEncodedValue).value.toLong(), 't')
            ValueType.CHAR ->
                writeCharEncodedValue(encodedValue as CharEncodedValue)
            ValueType.SHORT ->
                writeIntegralValue((encodedValue as ShortEncodedValue).value.toLong(), 's')
            ValueType.INT ->
                writeIntegralValue((encodedValue as IntEncodedValue).value.toLong(), null)
            ValueType.LONG ->
                writeIntegralValue((encodedValue as LongEncodedValue).value, 'L')
            ValueType.FLOAT ->
                writeFloatEncodedValue(encodedValue as FloatEncodedValue)
            ValueType.DOUBLE ->
                writeDoubleEncodedValue(encodedValue as DoubleEncodedValue)
            ValueType.ANNOTATION ->
                writeAnnotation(encodedValue as AnnotationEncodedValue)
            ValueType.ARRAY ->
                writeArray(encodedValue as ArrayEncodedValue)
            ValueType.STRING ->
                writeQuotedString((encodedValue as StringEncodedValue).value)
            ValueType.FIELD ->
                writeFieldDescriptor((encodedValue as FieldEncodedValue).value)
            ValueType.ENUM ->
                writeEnum(encodedValue as EnumEncodedValue)
            ValueType.METHOD ->
                writeMethodDescriptor((encodedValue as MethodEncodedValue).value)
            ValueType.TYPE ->
                writeType((encodedValue as TypeEncodedValue).value)
            ValueType.METHOD_TYPE ->
                writeMethodProtoDescriptor((encodedValue as MethodTypeEncodedValue).value)
            ValueType.METHOD_HANDLE ->
                writeMethodHandle((encodedValue as MethodHandleEncodedValue).value)
            ValueType.NULL ->
                write("null")
            else -> throw IllegalArgumentException("Unknown encoded value type")
        }
    }

    @Throws(IOException::class)
    protected fun writeBooleanEncodedValue(encodedValue: BooleanEncodedValue) {
        write(encodedValue.value.toString())
    }

    @Throws(IOException::class)
    protected fun writeIntegralValue(value: Long, suffix: Char?) {
        if (value < 0) {
            write("-0x")
            writeUnsignedLongAsHex(-value)
        } else {
            write("0x")
            writeUnsignedLongAsHex(value)
        }
        if (suffix != null) {
            write(suffix)
        }
    }

    @Throws(IOException::class)
    protected fun writeCharEncodedValue(encodedValue: CharEncodedValue) {
        val c = encodedValue.value
        if ((c >= ' ') && (c.code < 0x7f)) {
            write('\'')
            if ((c == '\'') || (c == '\"') || (c == '\\')) {
                write('\\')
            }
            write(c)
            write('\'')
            return
        } else if (c.code <= 0x7f) {
            when (c) {
                '\n' -> {
                    write("'\\n'")
                    return
                }
                '\r' -> {
                    write("'\\r'")
                    return
                }
                '\t' -> {
                    write("'\\t'")
                    return
                }
            }
        }

        write('\'')
        write("\\u")
        write(Character.forDigit(c.code shr 12, 16))
        write(Character.forDigit((c.code shr 8) and 0x0f, 16))
        write(Character.forDigit((c.code shr 4) and 0x0f, 16))
        write(Character.forDigit(c.code and 0x0f, 16))
        write('\'')
    }

    @Throws(IOException::class)
    protected fun writeFloatEncodedValue(encodedValue: FloatEncodedValue) {
        write(encodedValue.value.toString())
        write('f')
    }

    @Throws(IOException::class)
    protected fun writeDoubleEncodedValue(encodedValue: DoubleEncodedValue) {
        write(encodedValue.value.toString())
    }

    @Throws(IOException::class)
    protected fun writeEnum(encodedValue: EnumEncodedValue) {
        write(".enum ")
        writeFieldDescriptor(encodedValue.value)
    }

    /**
     * Write the given [AnnotationEncodedValue].
     */
    @Throws(IOException::class)
    override protected fun writeAnnotation(annotation: AnnotationEncodedValue) {
        write(".subannotation ")
        writeType(annotation.type)
        write('\n')

        writeAnnotationElements(annotation.elements)

        write(".end subannotation")
    }

    @Throws(IOException::class)
    fun writeAnnotationElements(annotationElements: Collection<AnnotationElement>) {
        indent(4)
        for (annotationElement in annotationElements) {
            writeSimpleName(annotationElement.name)
            write(" = ")
            writeEncodedValue(annotationElement.value)
            write('\n')
        }
        deindent(4)
    }

    /**
     * Write the given [ArrayEncodedValue].
     */
    @Throws(IOException::class)
    override protected fun writeArray(array: ArrayEncodedValue) {
        write('{')
        val values = array.value
        if (values.size == 0) {
            write('}')
            return
        }

        write('\n')
        indent(4)
        var first = true
        for (encodedValue in values) {
            if (!first) {
                write(",\n")
            }
            first = false

            writeEncodedValue(encodedValue)
        }
        deindent(4)
        write("\n}")
    }

    @Throws(IOException::class)
    override fun writeCallSite(callSiteReference: CallSiteReference) {
        writeSimpleName(callSiteReference.name)
        write('(')
        writeQuotedString(callSiteReference.methodName)
        write(", ")
        writeMethodProtoDescriptor(callSiteReference.methodProto)

        for (encodedValue in callSiteReference.extraArguments) {
            write(", ")
            writeEncodedValue(encodedValue)
        }

        write(")@")
        val methodHandle = callSiteReference.methodHandle
        if (methodHandle.methodHandleType != MethodHandleType.INVOKE_STATIC) {
            throw IllegalArgumentException("The linker method handle for a call site must be of type invoke-static")
        }
        writeMethodDescriptor(callSiteReference.methodHandle.memberReference as MethodReference)
    }

    fun indentingWriter(): IndentingWriter {
        return writer as IndentingWriter
    }

    @Throws(IOException::class)
    fun writeUnsignedLongAsHex(value0: Long) {
        var value = value0
        var bufferIndex = 23
        do {
            val digit = (value and 15).toInt()
            if (digit < 10) {
                buffer[bufferIndex--] = '0' + digit
            } else {
                buffer[bufferIndex--] = 'a' + (digit - 10)
            }

            value = value ushr 4
        } while (value != 0L)

        bufferIndex++

        write(buffer, bufferIndex, 24 - bufferIndex)
    }

    @Throws(IOException::class)
    fun writeSignedLongAsDec(value0: Long) {
        var value = value0
        var bufferIndex = 23

        if (value < 0) {
            write('-')
        }

        do {
            val digit = Math.abs(value % 10)
            buffer[bufferIndex--] = '0' + digit.toInt()

            value = value / 10
        } while (value != 0L)

        bufferIndex++

        write(buffer, bufferIndex, 24 - bufferIndex)
    }

    @Throws(IOException::class)
    fun writeSignedIntAsDec(value0: Int) {
        var value = value0
        var bufferIndex = 15

        if (value < 0) {
            write('-')
        }

        do {
            val digit = Math.abs(value % 10)
            buffer[bufferIndex--] = '0' + digit

            value = value / 10
        } while (value != 0)

        bufferIndex++

        write(buffer, bufferIndex, 16 - bufferIndex)
    }

    @Throws(IOException::class)
    fun writeUnsignedIntAsDec(value: Int) {
        if (value < 0) {
            writeSignedLongAsDec(value.toLong() and 0xFFFFFFFFL)
        } else {
            writeSignedIntAsDec(value)
        }
    }

    @Throws(IOException::class)
    fun writeSignedIntOrLongTo(value: Long) {
        if (value < 0) {
            write("-0x")
            writeUnsignedLongAsHex(-value)
            if (value < Integer.MIN_VALUE.toLong()) {
                write('L')
            }
        } else {
            write("0x")
            writeUnsignedLongAsHex(value)
            if (value > Integer.MAX_VALUE.toLong()) {
                write('L')
            }
        }
    }

    fun indent(indentAmount: Int) {
        (writer as IndentingWriter).indent(indentAmount)
    }

    fun deindent(indentAmount: Int) {
        (writer as IndentingWriter).deindent(indentAmount)
    }
}
