package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.writer.DexWriter
import java.util.AbstractList

class BuilderTypeList(internal val types: List<out BuilderTypeReference>) : AbstractList<BuilderTypeReference>() {
    var offset = DexWriter.NO_OFFSET

    override fun get(index: Int): BuilderTypeReference {
        return types[index]
    }

    override val size: Int
        get() = types.size

    companion object {
        @JvmField
        val EMPTY = BuilderTypeList(emptyList())
    }
}
