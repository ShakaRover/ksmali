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

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.util.ExceptionWithContext
import java.io.IOException

class DebugWriter<StringKey : CharSequence, TypeKey : CharSequence>
internal constructor(
    private val stringSection: StringSection<StringKey, *>,
    private val typeSection: TypeSection<StringKey, TypeKey, *>,
    private val writer: DexDataWriter,
) {
    private var currentAddress = 0
    private var currentLine = 0

    internal fun reset(startLine: Int) {
        currentAddress = 0
        currentLine = startLine
    }

    @Throws(IOException::class)
    fun writeStartLocal(codeAddress: Int, register: Int, name: StringKey?, type: TypeKey?, signature: StringKey?) {
        val nameIndex = stringSection.getNullableItemIndex(name)
        val typeIndex = typeSection.getNullableItemIndex(type)
        val signatureIndex = stringSection.getNullableItemIndex(signature)

        writeAdvancePC(codeAddress)
        if (signatureIndex == DexWriter.NO_INDEX) {
            writer.write(DebugItemType.START_LOCAL)
            writer.writeUleb128(register)
            writer.writeUleb128(nameIndex + 1)
            writer.writeUleb128(typeIndex + 1)
        } else {
            writer.write(DebugItemType.START_LOCAL_EXTENDED)
            writer.writeUleb128(register)
            writer.writeUleb128(nameIndex + 1)
            writer.writeUleb128(typeIndex + 1)
            writer.writeUleb128(signatureIndex + 1)
        }
    }

    @Throws(IOException::class)
    fun writeEndLocal(codeAddress: Int, register: Int) {
        writeAdvancePC(codeAddress)
        writer.write(DebugItemType.END_LOCAL)
        writer.writeUleb128(register)
    }

    @Throws(IOException::class)
    fun writeRestartLocal(codeAddress: Int, register: Int) {
        writeAdvancePC(codeAddress)
        writer.write(DebugItemType.RESTART_LOCAL)
        writer.writeUleb128(register)
    }

    @Throws(IOException::class)
    fun writePrologueEnd(codeAddress: Int) {
        writeAdvancePC(codeAddress)
        writer.write(DebugItemType.PROLOGUE_END)
    }

    @Throws(IOException::class)
    fun writeEpilogueBegin(codeAddress: Int) {
        writeAdvancePC(codeAddress)
        writer.write(DebugItemType.EPILOGUE_BEGIN)
    }

    @Throws(IOException::class)
    fun writeLineNumber(codeAddress: Int, lineNumber: Int) {
        var lineDelta = lineNumber - currentLine
        var addressDelta = codeAddress - currentAddress

        if (addressDelta < 0) {
            throw ExceptionWithContext("debug info items must have non-decreasing code addresses")
        }
        if (lineDelta < -4 || lineDelta > 10) {
            writeAdvanceLine(lineNumber)
            lineDelta = 0
        } // no else is intentional here. we might need to advance the PC as well as the line
        if ((lineDelta < 2 && addressDelta > 16) || (lineDelta > 1 && addressDelta > 15)) {
            writeAdvancePC(codeAddress)
            addressDelta = 0
        }

        // we need to emit the special opcode even if both lineDelta and addressDelta are 0, otherwise a positions
        // entry isn't generated
        writeSpecialOpcode(lineDelta, addressDelta)
    }

    @Throws(IOException::class)
    fun writeSetSourceFile(codeAddress: Int, sourceFile: StringKey?) {
        writeAdvancePC(codeAddress)
        writer.write(DebugItemType.SET_SOURCE_FILE)
        writer.writeUleb128(stringSection.getNullableItemIndex(sourceFile) + 1)
    }

    @Throws(IOException::class)
    private fun writeAdvancePC(address: Int) {
        val addressDelta = address - currentAddress

        if (addressDelta > 0) {
            writer.write(1)
            writer.writeUleb128(addressDelta)
            currentAddress = address
        } /*else if (addressDelta < 0) {
            throw new ExceptionWithContext("debug info items must have non-decreasing code addresses");
        }*/
    }

    @Throws(IOException::class)
    private fun writeAdvanceLine(line: Int) {
        val lineDelta = line - currentLine
        if (lineDelta != 0) {
            writer.write(2)
            writer.writeSleb128(lineDelta)
            currentLine = line
        }
    }

    @Throws(IOException::class)
    private fun writeSpecialOpcode(lineDelta: Int, addressDelta: Int) {
        writer.write(FIRST_SPECIAL + (addressDelta * LINE_RANGE) + (lineDelta - LINE_BASE))
        currentLine += lineDelta
        currentAddress += addressDelta
    }

    private companion object {
        private const val LINE_BASE = -4
        private const val LINE_RANGE = 15
        private const val FIRST_SPECIAL = 0x0a
    }
}
