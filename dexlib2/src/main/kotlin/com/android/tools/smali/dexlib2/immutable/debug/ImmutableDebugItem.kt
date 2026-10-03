/*
 * Copyright 2012, Google LLC
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

package com.android.tools.smali.dexlib2.immutable.debug

import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.EpilogueBegin
import com.android.tools.smali.dexlib2.iface.debug.LineNumber
import com.android.tools.smali.dexlib2.iface.debug.PrologueEnd
import com.android.tools.smali.dexlib2.iface.debug.RestartLocal
import com.android.tools.smali.dexlib2.iface.debug.SetSourceFile
import com.android.tools.smali.dexlib2.iface.debug.StartLocal
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.ImmutableConverter

abstract class ImmutableDebugItem(
    override val codeAddress: Int
) : DebugItem {
    companion object {
        fun of(debugItem: DebugItem): ImmutableDebugItem {
            if (debugItem is ImmutableDebugItem) {
                return debugItem
            }
            when (debugItem.debugItemType) {
                DebugItemType.START_LOCAL -> return ImmutableStartLocal.of(debugItem as StartLocal)
                DebugItemType.END_LOCAL -> return ImmutableEndLocal.of(debugItem as EndLocal)
                DebugItemType.RESTART_LOCAL -> return ImmutableRestartLocal.of(debugItem as RestartLocal)
                DebugItemType.PROLOGUE_END -> return ImmutablePrologueEnd.of(debugItem as PrologueEnd)
                DebugItemType.EPILOGUE_BEGIN -> return ImmutableEpilogueBegin.of(debugItem as EpilogueBegin)
                DebugItemType.SET_SOURCE_FILE -> return ImmutableSetSourceFile.of(debugItem as SetSourceFile)
                DebugItemType.LINE_NUMBER -> return ImmutableLineNumber.of(debugItem as LineNumber)
                else -> throw ExceptionWithContext(
                    "Invalid debug item type: %d", debugItem.debugItemType
                )
            }
        }

        fun immutableListOf(list: Iterable<DebugItem>?): List<ImmutableDebugItem> {
            return CONVERTER.toList(list)
        }

        private val CONVERTER: ImmutableConverter<ImmutableDebugItem, DebugItem> =
            object : ImmutableConverter<ImmutableDebugItem, DebugItem>() {
                override fun isImmutable(item: DebugItem): Boolean {
                    return item is ImmutableDebugItem
                }

                override fun makeImmutable(item: DebugItem): ImmutableDebugItem {
                    return of(item)
                }
            }
    }
}
