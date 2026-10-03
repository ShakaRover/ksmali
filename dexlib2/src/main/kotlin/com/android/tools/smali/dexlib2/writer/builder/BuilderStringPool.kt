package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.StringSection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderStringPool : StringSection<BuilderStringReference, BuilderStringReference> {
    private val internedItems: ConcurrentMap<String, BuilderStringReference> = ConcurrentHashMap()

    fun internString(string: String): BuilderStringReference {
        val ret = internedItems[string]
        if (ret != null) {
            return ret
        }
        val stringReference = BuilderStringReference(string)
        val existing = internedItems.putIfAbsent(string, stringReference)
        return existing ?: stringReference
    }

    fun internNullableString(string: String?): BuilderStringReference? {
        if (string == null) {
            return null
        }
        return internString(string)
    }

    override fun getNullableItemIndex(key: BuilderStringReference?): Int {
        return if (key == null) DexWriter.NO_INDEX else key.index
    }

    override fun getItemIndex(key: BuilderStringReference): Int {
        return key.index
    }

    override val hasJumboIndexes: Boolean
        get() = internedItems.size > 65536

    override val items: Collection<MutableMap.MutableEntry<BuilderStringReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderStringReference>(internedItems.values) {
            override fun getValue(key: BuilderStringReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderStringReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
