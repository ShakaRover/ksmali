/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import java.io.IOException

class CatchMethodItem(
    options: BaksmaliOptions,
    labelCache: MethodDefinition.LabelCache,
    codeAddress: Int,
    private val exceptionType: String?,
    startAddress: Int,
    endAddress: Int,
    handlerAddress: Int
) : MethodItem(codeAddress) {
    private val tryStartLabel: LabelMethodItem
    private val tryEndLabel: LabelMethodItem
    private val handlerLabel: LabelMethodItem

    init {
        tryStartLabel = labelCache.internLabel(LabelMethodItem(options, startAddress, "try_start_"))

        //use the address from the last covered instruction, but make the label
        //name refer to the address of the next instruction
        tryEndLabel = labelCache.internLabel(EndTryLabelMethodItem(options, codeAddress, endAddress))

        handlerLabel = if (exceptionType == null) {
            labelCache.internLabel(LabelMethodItem(options, handlerAddress, "catchall_"))
        } else {
            labelCache.internLabel(LabelMethodItem(options, handlerAddress, "catch_"))
        }
    }

    fun getTryStartLabel(): LabelMethodItem {
        return tryStartLabel
    }

    fun getTryEndLabel(): LabelMethodItem {
        return tryEndLabel
    }

    fun getHandlerLabel(): LabelMethodItem {
        return handlerLabel
    }

    override fun getSortOrder(): Double {
        //sort after instruction and end_try label
        return 102.0
    }

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        if (exceptionType == null) {
            writer.write(".catchall")
        } else {
            writer.write(".catch ")
            writer.write(exceptionType)
        }
        writer.write(" {")
        tryStartLabel.writeTo(writer)
        writer.write(" .. ")
        tryEndLabel.writeTo(writer)
        writer.write("} ")
        handlerLabel.writeTo(writer)
        return true
    }
}
