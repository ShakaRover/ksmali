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
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement
import com.android.tools.smali.dexlib2.immutable.reference.*
import com.android.tools.smali.dexlib2.immutable.value.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.StringWriter

class DexFormattedWriterTest {

    private lateinit var output: StringWriter

    @Before
    fun setup() {
        output = StringWriter()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodDescriptor() {
        val writer = DexFormattedWriter(output)

        writer.writeMethodDescriptor(getMethodReference())

        Assert.assertEquals("Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteShortMethodDescriptor() {
        val writer = DexFormattedWriter(output)

        writer.writeShortMethodDescriptor(getMethodReference())

        Assert.assertEquals("methodName(Lparam1;Lparam2;)Lreturn/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodProtoDescriptor() {
        val writer = DexFormattedWriter(output)

        writer.writeMethodProtoDescriptor(getMethodProtoReference())

        Assert.assertEquals("(Lparam1;Lparam2;)Lreturn/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteFieldDescriptor() {
        val writer = DexFormattedWriter(output)

        writer.writeFieldDescriptor(getFieldReference())

        Assert.assertEquals("Ldefining/class;->fieldName:Lfield/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteShortFieldDescriptor() {
        val writer = DexFormattedWriter(output)

        writer.writeShortFieldDescriptor(getFieldReference())

        Assert.assertEquals("fieldName:Lfield/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodHandle_fieldAccess() {
        val writer = DexFormattedWriter(output)

        writer.writeMethodHandle(getMethodHandleReferenceForField())

        Assert.assertEquals("instance-get@Ldefining/class;->fieldName:Lfield/type;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodHandle_methodAccess() {
        val writer = DexFormattedWriter(output)

        writer.writeMethodHandle(getMethodHandleReferenceForMethod())

        Assert.assertEquals("invoke-instance@Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteCallsite() {
        val writer = DexFormattedWriter(output)

        writer.writeCallSite(getCallSiteReference())

        Assert.assertEquals(
                "callsiteName(\"callSiteMethodName\", " +
                        "(Lparam1;Lparam2;)Lreturn/type;, Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;)@" +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_boolean_true() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableBooleanEncodedValue.TRUE_VALUE)

        Assert.assertEquals("true", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_boolean_false() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableBooleanEncodedValue.FALSE_VALUE)

        Assert.assertEquals("false", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_byte() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableByteEncodedValue(0x12.toByte()))

        Assert.assertEquals("0x12", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_char() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableCharEncodedValue('a'))

        Assert.assertEquals("0x61", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_short() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableShortEncodedValue(0x12.toShort()))

        Assert.assertEquals("0x12", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_int() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableIntEncodedValue(0x12))

        Assert.assertEquals("0x12", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_long() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableLongEncodedValue(0x12))

        Assert.assertEquals("0x12", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_float() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableFloatEncodedValue(12.34f))

        Assert.assertEquals("12.34", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_double() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableDoubleEncodedValue(12.34))

        Assert.assertEquals("12.34", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_annotation() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableAnnotationEncodedValue(
                "Lannotation/type;",
                setOf(
                        ImmutableAnnotationElement("element1", ImmutableFieldEncodedValue(getFieldReference())),
                        ImmutableAnnotationElement("element2", ImmutableMethodEncodedValue(getMethodReference()))
                )))

        Assert.assertEquals(
                "Annotation[Lannotation/type;, " +
                        "element1=Ldefining/class;->fieldName:Lfield/type;, " +
                        "element2=Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;]",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_array() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableArrayEncodedValue(listOf(
                ImmutableFieldEncodedValue(getFieldReference()),
                ImmutableMethodEncodedValue(getMethodReference()))))

        Assert.assertEquals(
                "Array[Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;]",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_string() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableStringEncodedValue("string value\n"))

        Assert.assertEquals(
                "\"string value\\n\"",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_field() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableFieldEncodedValue(getFieldReference()))

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_enum() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableEnumEncodedValue(getFieldReference()))

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_method() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableMethodEncodedValue(getMethodReference()))

        Assert.assertEquals(
                "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_type() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableTypeEncodedValue("Ltest/type;"))

        Assert.assertEquals(
                "Ltest/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_methodType() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableMethodTypeEncodedValue(getMethodProtoReference()))

        Assert.assertEquals(
                "(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_methodHandle() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableMethodHandleEncodedValue(getMethodHandleReferenceForField()))

        Assert.assertEquals(
                "instance-get@Ldefining/class;->fieldName:Lfield/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_null() {
        val writer = DexFormattedWriter(output)

        writer.writeEncodedValue(ImmutableNullEncodedValue.INSTANCE)

        Assert.assertEquals(
                "null",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_string() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(ImmutableStringReference("string value"))

        Assert.assertEquals(
                "\"string value\"",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_type() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(ImmutableTypeReference("Ltest/type;"))

        Assert.assertEquals(
                "Ltest/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_field() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(getFieldReference())

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_method() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(getMethodReference())

        Assert.assertEquals(
                "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_methodProto() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(getMethodProtoReference())

        Assert.assertEquals(
                "(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_methodHandle() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(getMethodHandleReferenceForMethod())

        Assert.assertEquals(
                "invoke-instance@Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteReference_callSite() {
        val writer = DexFormattedWriter(output)

        writer.writeReference(getCallSiteReference())

        Assert.assertEquals(
                "callsiteName(\"callSiteMethodName\", " +
                        "(Lparam1;Lparam2;)Lreturn/type;, Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;)@" +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString())
    }

    private fun getMethodReference(): ImmutableMethodReference {
        return ImmutableMethodReference(
                "Ldefining/class;",
                "methodName",
                listOf("Lparam1;", "Lparam2;"),
                "Lreturn/type;")
    }

    private fun getMethodProtoReference(): ImmutableMethodProtoReference {
        return ImmutableMethodProtoReference(
                listOf("Lparam1;", "Lparam2;"),
                "Lreturn/type;")
    }

    private fun getFieldReference(): ImmutableFieldReference {
        return ImmutableFieldReference(
                "Ldefining/class;",
                "fieldName",
                "Lfield/type;")
    }

    private fun getMethodHandleReferenceForField(): ImmutableMethodHandleReference {
        return ImmutableMethodHandleReference(
                MethodHandleType.INSTANCE_GET,
                getFieldReference())
    }

    private fun getMethodHandleReferenceForMethod(): MethodHandleReference {
        return ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_INSTANCE,
                getMethodReference())
    }

    private fun getInvokeStaticMethodHandleReferenceForMethod(): MethodHandleReference {
        return ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_STATIC,
                getMethodReference())
    }

    private fun getCallSiteReference(): CallSiteReference {
        return ImmutableCallSiteReference(
                "callsiteName",
                getInvokeStaticMethodHandleReferenceForMethod(),
                "callSiteMethodName",
                getMethodProtoReference(),
                listOf(
                        ImmutableFieldEncodedValue(getFieldReference()),
                        ImmutableMethodEncodedValue(getMethodReference())))
    }
}
