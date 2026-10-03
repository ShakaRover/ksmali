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
import java.io.IOException
import java.io.StringWriter
import java.io.Writer

/**
 * This class handles formatting and getting strings for various types of items in a dex file.
 */
open class DexFormatter {

    /**
     * Gets a [DexFormattedWriter] for writing formatted strings to a [Writer], with the same settings as this Formatter.
     *
     * @param writer The [Writer] that the [DexFormattedWriter] will write to.
     */
    open fun getWriter(writer: Writer): DexFormattedWriter {
        return DexFormattedWriter(writer)
    }

    fun getMethodDescriptor(methodReference: MethodReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeMethodDescriptor(methodReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getShortMethodDescriptor(methodReference: MethodReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeShortMethodDescriptor(methodReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getMethodProtoDescriptor(protoReference: MethodProtoReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeMethodProtoDescriptor(protoReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getFieldDescriptor(fieldReference: FieldReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeFieldDescriptor(fieldReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getShortFieldDescriptor(fieldReference: FieldReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeShortFieldDescriptor(fieldReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getMethodHandle(methodHandleReference: MethodHandleReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeMethodHandle(methodHandleReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getCallSite(callSiteReference: CallSiteReference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeCallSite(callSiteReference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getType(type: CharSequence): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeType(type)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getQuotedString(string: CharSequence): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeQuotedString(string)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getEncodedValue(encodedValue: EncodedValue): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeEncodedValue(encodedValue)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    fun getReference(reference: Reference): String {
        val writer = StringWriter()
        try {
            getWriter(writer).writeReference(reference)
        } catch (e: IOException) {
            throw AssertionError("Unexpected IOException")
        }
        return writer.toString()
    }

    companion object {
        val INSTANCE: DexFormatter = DexFormatter()
    }
}
