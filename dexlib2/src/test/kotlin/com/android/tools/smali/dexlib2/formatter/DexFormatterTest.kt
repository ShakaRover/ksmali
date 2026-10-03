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

import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.IOException
import java.io.Writer

class DexFormatterTest {

    @Test
    @Throws(IOException::class)
    fun testGetMethodReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("method descriptor", formatter.getMethodDescriptor(mock(MethodReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetShortMethodReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("short method descriptor", formatter.getShortMethodDescriptor(mock(MethodReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetMethodProtoReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("method proto descriptor", formatter.getMethodProtoDescriptor(mock(MethodProtoReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetFieldReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("field descriptor", formatter.getFieldDescriptor(mock(FieldReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetShortFieldReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("short field descriptor", formatter.getShortFieldDescriptor(mock(FieldReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetMethodHandle() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("method handle", formatter.getMethodHandle(mock(MethodHandleReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetCallSite() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("call site", formatter.getCallSite(mock(CallSiteReference::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testGetType() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("type", formatter.getType("mock type"))
    }

    @Test
    @Throws(IOException::class)
    fun testGetQuotedString() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("quoted string", formatter.getQuotedString("mock string"))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedValue() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("encoded value", formatter.getEncodedValue(mock(EncodedValue::class.java)))
    }

    @Test
    @Throws(IOException::class)
    fun testReference() {
        val formatter = TestDexFormatter()
        Assert.assertEquals("reference", formatter.getReference(mock(Reference::class.java)))
    }

    private class TestDexFormatter : DexFormatter() {
        override fun getWriter(writer: Writer): DexFormattedWriter {
            return object : DexFormattedWriter(writer) {
                @Throws(IOException::class)
                override fun writeMethodDescriptor(methodReference: MethodReference) {
                    writer.write("method descriptor")
                }

                @Throws(IOException::class)
                override fun writeShortMethodDescriptor(methodReference: MethodReference) {
                    writer.write("short method descriptor")
                }

                @Throws(IOException::class)
                override fun writeMethodProtoDescriptor(protoReference: MethodProtoReference) {
                    writer.write("method proto descriptor")
                }

                @Throws(IOException::class)
                override fun writeFieldDescriptor(fieldReference: FieldReference) {
                    writer.write("field descriptor")
                }

                @Throws(IOException::class)
                override fun writeShortFieldDescriptor(fieldReference: FieldReference) {
                    writer.write("short field descriptor")
                }

                @Throws(IOException::class)
                override fun writeMethodHandle(methodHandleReference: MethodHandleReference) {
                    writer.write("method handle")
                }

                @Throws(IOException::class)
                override fun writeCallSite(callSiteReference: CallSiteReference) {
                    writer.write("call site")
                }

                @Throws(IOException::class)
                override fun writeType(type: CharSequence) {
                    writer.write("type")
                }

                @Throws(IOException::class)
                override fun writeQuotedString(charSequence: CharSequence) {
                    writer.write("quoted string")
                }

                @Throws(IOException::class)
                override fun writeEncodedValue(encodedValue: EncodedValue) {
                    writer.write("encoded value")
                }

                @Throws(IOException::class)
                override fun writeReference(reference: Reference) {
                    writer.write("reference")
                }
            }
        }
    }
}
