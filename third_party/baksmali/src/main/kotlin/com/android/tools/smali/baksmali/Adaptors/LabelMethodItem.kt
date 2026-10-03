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

open class LabelMethodItem(
    private val options: BaksmaliOptions,
    codeAddress: Int,
    val labelPrefix: String
) : MethodItem(codeAddress) {
    var labelSequence = 0

    override val sortOrder: Double get() = 0.0

    override fun compareTo(methodItem: MethodItem): Int {
        var result = super.compareTo(methodItem)

        if (result == 0) {
            if (methodItem is LabelMethodItem) {
                result = labelPrefix.compareTo(methodItem.labelPrefix)
            }
        }
        return result
    }

    override fun hashCode(): Int {
        //force it to call equals when two labels are at the same address
        return codeAddress
    }

    override fun equals(o: Any?): Boolean {
        if (o !is LabelMethodItem) {
            return false
        }
        return this.compareTo(o) == 0
    }

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        writer.write(':')
        writer.write(labelPrefix)
        if (options.sequentialLabels) {
            writer.writeUnsignedLongAsHex(labelSequence.toLong())
        } else {
            writer.writeUnsignedLongAsHex(labelAddress.toLong())
        }
        return true
    }

    open val labelAddress: Int get() = codeAddress
}
