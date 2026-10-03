package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.TypeSection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderTypePool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    TypeSection<BuilderStringReference, BuilderTypeReference, BuilderTypeReference> {
    private val internedItems: ConcurrentMap<String, BuilderTypeReference> = ConcurrentHashMap()

    fun internType(type: String): BuilderTypeReference {
        val ret = internedItems[type]
        if (ret != null) {
            return ret
        }
        val stringRef = dexBuilder.stringSection.internString(type)
        val typeReference = BuilderTypeReference(stringRef)
        val existing = internedItems.putIfAbsent(type, typeReference)
        return existing ?: typeReference
    }

    fun internNullableType(type: String?): BuilderTypeReference? {
        if (type == null) {
            return null
        }
        return internType(type)
    }

    override fun getString(key: BuilderTypeReference): BuilderStringReference {
        return key.stringReference
    }

    override fun getNullableItemIndex(key: BuilderTypeReference?): Int {
        return if (key == null) DexWriter.NO_INDEX else key.index
    }

    override fun getItemIndex(key: BuilderTypeReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderTypeReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderTypeReference>(internedItems.values) {
            override fun getValue(key: BuilderTypeReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderTypeReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
