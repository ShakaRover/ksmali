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

package com.android.tools.smali.baksmali.Adaptors.Format

import com.android.tools.smali.baksmali.Adaptors.LabelMethodItem
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import java.io.IOException

class PackedSwitchMethodItem(
    methodDef: MethodDefinition, codeAddress: Int, instruction: PackedSwitchPayload
) : InstructionMethodItem<PackedSwitchPayload>(methodDef, codeAddress, instruction) {
    private val targets: List<PackedSwitchTarget>
    private val firstKey: Int

    // Whether this sparse switch instruction should be commented out because it is never referenced
    private var commentedOut = false

    init {
        val baseCodeAddress = methodDef.getPackedSwitchBaseAddress(codeAddress)

        val newTargets = mutableListOf<PackedSwitchTarget>()

        var first = true
        var tempFirstKey = 0
        if (baseCodeAddress >= 0) {
            for (switchElement in instruction.switchElements) {
                if (first) {
                    tempFirstKey = switchElement.key
                    first = false
                }
                val label = methodDef.labelCache.internLabel(
                    LabelMethodItem(
                        methodDef.classDef.options, baseCodeAddress + switchElement.offset, "pswitch_"))
                newTargets.add(PackedSwitchLabelTarget(label))
            }
        } else {
            commentedOut = true
            for (switchElement in instruction.switchElements) {
                if (first) {
                    tempFirstKey = switchElement.key
                    first = false
                }
                newTargets.add(PackedSwitchOffsetTarget(switchElement.offset))
            }
        }
        targets = newTargets
        firstKey = tempFirstKey
    }

    @Throws(IOException::class)
    override fun writeTo(writer0: BaksmaliWriter): Boolean {
        var writer = writer0
        if (commentedOut) {
            writer = methodDef.classDef.getCommentingWriter(writer)
        }
        writer.write(".packed-switch ")
        writer.writeEncodedValue(ImmutableIntEncodedValue(firstKey))
        writer.indent(4)
        writer.write('\n')
        var key = firstKey
        for (target in targets) {
            target.writeTargetTo(writer)
            writeCommentIfResourceId(writer, key)
            writer.write('\n')
            key++
        }
        writer.deindent(4)
        writer.write(".end packed-switch")
        return true
    }

    private abstract class PackedSwitchTarget {
        @Throws(IOException::class)
        abstract fun writeTargetTo(writer: BaksmaliWriter)
    }

    private class PackedSwitchLabelTarget(
        private val target: LabelMethodItem
    ) : PackedSwitchTarget() {
        @Throws(IOException::class)
        override fun writeTargetTo(writer: BaksmaliWriter) {
            target.writeTo(writer)
        }
    }

    private class PackedSwitchOffsetTarget(
        private val target: Int
    ) : PackedSwitchTarget() {
        @Throws(IOException::class)
        override fun writeTargetTo(writer: BaksmaliWriter) {
            if (target >= 0) {
                writer.write('+')
            }
            writer.writeSignedIntAsDec(target)
        }
    }
}
