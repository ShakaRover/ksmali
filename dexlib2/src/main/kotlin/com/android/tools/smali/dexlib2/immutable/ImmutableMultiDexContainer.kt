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

package com.android.tools.smali.dexlib2.immutable

import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

open class ImmutableMultiDexContainer(
    entries: Map<String, ImmutableDexFile>
) : MultiDexContainer<ImmutableDexFile> {
    private val entries: Map<String, ImmutableDexEntry>

    init {
        val map = HashMap<String, ImmutableDexEntry>()
        for ((key, value) in entries) {
            val dexEntry = ImmutableDexEntry(key, value)
            map[dexEntry.entryName] = dexEntry
        }
        this.entries = Collections.unmodifiableMap(map)
    }

    override val dexEntryNames: List<String>
        get() = Collections.unmodifiableList(ArrayList(entries.keys))

    override fun getEntry(entryName: String): ImmutableDexEntry? {
        return entries[entryName]
    }

    open inner class ImmutableDexEntry(
        override val entryName: String,
        override val dexFile: ImmutableDexFile
    ) : MultiDexContainer.DexEntry<ImmutableDexFile> {
        override val container: MultiDexContainer<out ImmutableDexFile>
            get() = this@ImmutableMultiDexContainer
    }
}
