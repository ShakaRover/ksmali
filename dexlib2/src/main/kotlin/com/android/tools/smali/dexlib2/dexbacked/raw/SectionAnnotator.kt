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

package com.android.tools.smali.dexlib2.dexbacked.raw

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.dexlib2.util.alignOffset
import java.util.HashMap

abstract class SectionAnnotator(
    @JvmField val annotator: DexAnnotator,
    mapItem: MapItem
) {
    @JvmField
    val dexFile: DexBackedDexFile

    @JvmField
    val itemType: Int

    @JvmField
    val sectionOffset: Int

    @JvmField
    val itemCount: Int

    protected val itemIdentities: MutableMap<Int, String> = HashMap()

    init {
        this.dexFile = annotator.dexFile
        this.itemType = mapItem.getType()

        if (mapItem.getType() >= ItemType.MAP_LIST) {
            this.sectionOffset = mapItem.getOffset() + dexFile.baseDataOffset
        } else {
            this.sectionOffset = mapItem.getOffset()
        }

        this.itemCount = mapItem.getItemCount()
    }

    abstract fun getItemName(): String

    protected abstract fun annotateItem(
        out: AnnotatedBytes,
        itemIndex: Int,
        itemIdentity: String?
    )

    /**
     * Write out annotations for this section
     *
     * @param out The AnnotatedBytes object to annotate to
     */
    open fun annotateSection(out: AnnotatedBytes) {
        out.moveTo(sectionOffset)
        annotateSectionInner(out, itemCount)
    }

    protected open fun getItemOffset(itemIndex: Int, currentOffset: Int): Int {
        return alignOffset(currentOffset, getItemAlignment())
    }

    protected fun annotateSectionInner(out: AnnotatedBytes, itemCount: Int) {
        val itemName = getItemName()
        if (itemCount > 0) {
            out.annotate(0, "")
            out.annotate(0, "-----------------------------")
            out.annotate(0, "%s section", itemName)
            out.annotate(0, "-----------------------------")
            out.annotate(0, "")

            for (i in 0 until itemCount) {
                out.moveTo(getItemOffset(i, out.cursor))

                val itemIdentity = getItemIdentity(out.cursor)
                if (itemIdentity != null) {
                    out.annotate(0, "[%d] %s: %s", i, itemName, itemIdentity)
                } else {
                    out.annotate(0, "[%d] %s", i, itemName)
                }
                out.indent()
                annotateItem(out, i, itemIdentity)
                out.deindent()
            }
        }
    }

    private fun getItemIdentity(itemOffset: Int): String? {
        return itemIdentities[itemOffset]
    }

    fun setItemIdentity(itemOffset: Int, identity: String) {
        itemIdentities[itemOffset + dexFile.baseDataOffset] = identity
    }

    open fun getItemAlignment(): Int {
        return 1
    }
}
