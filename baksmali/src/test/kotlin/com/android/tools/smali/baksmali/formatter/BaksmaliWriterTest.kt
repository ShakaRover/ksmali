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

import com.android.tools.smali.dexlib2.immutable.reference.ImmutableCallSiteReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodHandleReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodProtoReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.value.ImmutableAnnotationEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableArrayEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableCharEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableEnumEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableFieldEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableMethodEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableTypeEncodedValue
import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.StringWriter

class BaksmaliWriterTest {

    private lateinit var output: StringWriter

    @Before
    fun setup() {
        output = StringWriter()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodDescriptor_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeMethodDescriptor(methodReferenceWithSpaces)

        Assert.assertEquals(
            "Ldefining/class/`with spaces`;->`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)" +
                "Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteShortMethodDescriptor_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeShortMethodDescriptor(methodReferenceWithSpaces)

        Assert.assertEquals(
            "`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)" +
                "Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodProtoDescriptor_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeMethodProtoDescriptor(methodProtoReferenceWithSpaces)

        Assert.assertEquals(
            "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteFieldDescriptor_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeFieldDescriptor(fieldReferenceWithSpaces)

        Assert.assertEquals(
            "Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteShortFieldDescriptor_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeShortFieldDescriptor(fieldReferenceWithSpaces)

        Assert.assertEquals(
            "`fieldName with spaces`:Lfield/`type with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodHandle_fieldAccess_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeMethodHandle(methodHandleReferenceForFieldWithSpaces)

        Assert.assertEquals(
            "instance-get@Ldefining/class/`with spaces`;->`fieldName with spaces`:" +
                "Lfield/`type with spaces`;", output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteMethodHandle_methodAccess_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeMethodHandle(methodHandleReferenceForMethodWithSpaces)

        Assert.assertEquals(
            "invoke-instance@Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteCallsite_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeCallSite(ImmutableCallSiteReference(
            "callsiteName with spaces",
            invokeStaticMethodHandleReferenceForMethodWithSpaces,
            "callSiteMethodName with spaces",
            methodProtoReferenceWithSpaces,
            listOf(
                ImmutableFieldEncodedValue(fieldReferenceWithSpaces),
                ImmutableMethodEncodedValue(methodReferenceWithSpaces))))

        Assert.assertEquals(
            "`callsiteName with spaces`(\"callSiteMethodName with spaces\", " +
                "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;, " +
                "Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;, " +
                "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;)@" +
                "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_annotation_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableAnnotationEncodedValue(
            "Lannotation/type with spaces;",
            setOf(
                ImmutableAnnotationElement(
                    "element with spaces 1",
                    ImmutableFieldEncodedValue(fieldReferenceWithSpaces)),
                ImmutableAnnotationElement(
                    "element with spaces 2",
                    ImmutableMethodEncodedValue(methodReferenceWithSpaces))
            )))

        Assert.assertEquals(
            ".subannotation Lannotation/`type with spaces`;" + System.lineSeparator() +
                "    `element with spaces 1` = Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;" + System.lineSeparator() +
                "    `element with spaces 2` = Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;" + System.lineSeparator() +
                ".end subannotation",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_array_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableArrayEncodedValue(listOf(
            ImmutableFieldEncodedValue(fieldReferenceWithSpaces),
            ImmutableMethodEncodedValue(methodReferenceWithSpaces))))

        Assert.assertEquals(
            "{" + System.lineSeparator() +
                "    Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;," + System.lineSeparator() +
                "    Ldefining/class/`with spaces`;->`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;" + System.lineSeparator() +
                "}",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_field_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableFieldEncodedValue(fieldReferenceWithSpaces))

        Assert.assertEquals(
            "Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_enum_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableEnumEncodedValue(fieldReferenceWithSpaces))

        Assert.assertEquals(
            ".enum Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_method_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableMethodEncodedValue(methodReferenceWithSpaces))

        Assert.assertEquals(
            "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_type_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableTypeEncodedValue("Ltest/type with spaces;"))

        Assert.assertEquals(
            "Ltest/`type with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_methodType_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableMethodTypeEncodedValue(methodProtoReferenceWithSpaces))

        Assert.assertEquals(
            "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_methodHandle_withSpaces() {
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(
            ImmutableMethodHandleEncodedValue(methodHandleReferenceForMethodWithSpaces))

        Assert.assertEquals(
            "invoke-instance@Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
            output.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testWriteEncodedValue_char_normal() {
        Assert.assertEquals("'&'", performWriteChar('&'))
        Assert.assertEquals("'\\\\'", performWriteChar('\\'))
        Assert.assertEquals("'\\n'", performWriteChar('\n'))
        Assert.assertEquals("'\\u7777'", performWriteChar('\u7777'))
    }

    @Throws(IOException::class)
    private fun performWriteChar(c: Char): String {
        output = StringWriter()
        val writer = BaksmaliWriter(output)

        writer.writeEncodedValue(ImmutableCharEncodedValue(c))
        writer.close()
        return output.toString()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteUnsignedLongAsHex() {
        Assert.assertEquals("ffffffffffffffff", performWriteUnsignedLongAsHex(-1))
        Assert.assertEquals("7fffffff", performWriteUnsignedLongAsHex(Integer.MAX_VALUE.toLong()))
        Assert.assertEquals("ffffffff80000000", performWriteUnsignedLongAsHex(Integer.MIN_VALUE.toLong()))
        Assert.assertEquals("0", performWriteUnsignedLongAsHex(0))
        Assert.assertEquals("1", performWriteUnsignedLongAsHex(1))

        Assert.assertEquals("80000000", performWriteUnsignedLongAsHex(Integer.MAX_VALUE.toLong() + 1))
        Assert.assertEquals("ffffffff7fffffff", performWriteUnsignedLongAsHex(Integer.MIN_VALUE.toLong() - 1))

        Assert.assertEquals("7fffffffffffffff", performWriteUnsignedLongAsHex(Long.MAX_VALUE))
        Assert.assertEquals("8000000000000000", performWriteUnsignedLongAsHex(Long.MIN_VALUE))
    }

    @Throws(IOException::class)
    private fun performWriteUnsignedLongAsHex(value: Long): String {
        output = StringWriter()
        val writer = BaksmaliWriter(output)

        writer.writeUnsignedLongAsHex(value)
        writer.close()
        return output.toString()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteSignedLongAsDec() {
        Assert.assertEquals("-1", performWriteSignedLongAsDec(-1))
        Assert.assertEquals("2147483647", performWriteSignedLongAsDec(Integer.MAX_VALUE.toLong()))
        Assert.assertEquals("-2147483648", performWriteSignedLongAsDec(Integer.MIN_VALUE.toLong()))
        Assert.assertEquals("0", performWriteSignedLongAsDec(0))
        Assert.assertEquals("1", performWriteSignedLongAsDec(1))

        Assert.assertEquals("2147483648", performWriteSignedLongAsDec(Integer.MAX_VALUE.toLong() + 1))
        Assert.assertEquals("-2147483649", performWriteSignedLongAsDec(Integer.MIN_VALUE.toLong() - 1))

        Assert.assertEquals("9223372036854775807", performWriteSignedLongAsDec(Long.MAX_VALUE))
        Assert.assertEquals("-9223372036854775808", performWriteSignedLongAsDec(Long.MIN_VALUE))
    }

    @Throws(IOException::class)
    private fun performWriteSignedLongAsDec(value: Long): String {
        output = StringWriter()
        val writer = BaksmaliWriter(output)

        writer.writeSignedLongAsDec(value)
        writer.close()
        return output.toString()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteSignedIntAsDec() {
        Assert.assertEquals("-1", performWriteSignedIntAsDec(-1))
        Assert.assertEquals("2147483647", performWriteSignedIntAsDec(Integer.MAX_VALUE))
        Assert.assertEquals("-2147483648", performWriteSignedIntAsDec(Integer.MIN_VALUE))
        Assert.assertEquals("0", performWriteSignedIntAsDec(0))
        Assert.assertEquals("1", performWriteSignedIntAsDec(1))
    }

    @Throws(IOException::class)
    private fun performWriteSignedIntAsDec(value: Int): String {
        output = StringWriter()
        val writer = BaksmaliWriter(output)

        writer.writeSignedIntAsDec(value)
        writer.close()
        return output.toString()
    }

    @Test
    @Throws(IOException::class)
    fun testWriteUnsignedIntAsDec() {
        Assert.assertEquals("4294967295", performWriteUnsignedIntAsDec(-1))
        Assert.assertEquals("2147483647", performWriteUnsignedIntAsDec(Integer.MAX_VALUE))
        Assert.assertEquals("2147483648", performWriteUnsignedIntAsDec(Integer.MIN_VALUE))
        Assert.assertEquals("0", performWriteUnsignedIntAsDec(0))
        Assert.assertEquals("1", performWriteUnsignedIntAsDec(1))
    }

    @Throws(IOException::class)
    private fun performWriteUnsignedIntAsDec(value: Int): String {
        output = StringWriter()
        val writer = BaksmaliWriter(output)

        writer.writeUnsignedIntAsDec(value)
        writer.close()
        return output.toString()
    }

    private val methodReferenceWithSpaces: ImmutableMethodReference get() {
        return ImmutableMethodReference(
            "Ldefining/class/with spaces;",
            "methodName with spaces",
            listOf("Lparam with spaces 1;", "Lparam with spaces 2;"),
            "Lreturn/type/with spaces;")
    }

    private val methodProtoReferenceWithSpaces: ImmutableMethodProtoReference get() {
        return ImmutableMethodProtoReference(
            listOf("Lparam with spaces 1;", "Lparam with spaces 2;"),
            "Lreturn/type/with spaces;")
    }

    private val fieldReferenceWithSpaces: ImmutableFieldReference get() {
        return ImmutableFieldReference(
            "Ldefining/class/with spaces;",
            "fieldName with spaces",
            "Lfield/type with spaces;")
    }

    private val methodHandleReferenceForFieldWithSpaces: MethodHandleReference get() {
        return ImmutableMethodHandleReference(
            MethodHandleType.INSTANCE_GET,
            fieldReferenceWithSpaces)
    }

    private val methodHandleReferenceForMethodWithSpaces: ImmutableMethodHandleReference get() {
        return ImmutableMethodHandleReference(
            MethodHandleType.INVOKE_INSTANCE,
            methodReferenceWithSpaces)
    }

    private val invokeStaticMethodHandleReferenceForMethodWithSpaces: MethodHandleReference get() {
        return ImmutableMethodHandleReference(
            MethodHandleType.INVOKE_STATIC,
            methodReferenceWithSpaces)
    }
}
