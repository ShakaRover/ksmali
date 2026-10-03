package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.TypeListSection
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Collectors

class BuilderTypeListPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    TypeListSection<BuilderTypeReference, BuilderTypeList> {
    private val internedItems: ConcurrentMap<List<CharSequence>, BuilderTypeList> = ConcurrentHashMap()

    fun internTypeList(types: List<CharSequence>?): BuilderTypeList {
        if (types == null || types.size == 0) {
            return BuilderTypeList.EMPTY
        }

        val ret = internedItems[types]
        if (ret != null) {
            return ret
        }

        val typeList = BuilderTypeList(
            Collections.unmodifiableList(
                types.stream()
                    .map { type -> dexBuilder.typeSection.internType(type.toString()) }
                    .collect(Collectors.toList())
            )
        )

        val existing = internedItems.putIfAbsent(typeList, typeList)
        return existing ?: typeList
    }

    override fun getNullableItemOffset(key: BuilderTypeList?): Int {
        return if (key == null || key.size == 0) DexWriter.NO_OFFSET else key.offset
    }

    override fun getTypes(key: BuilderTypeList?): Collection<@JvmWildcard BuilderTypeReference> {
        return if (key == null) BuilderTypeList.EMPTY else key.types
    }

    override fun getItemOffset(key: BuilderTypeList): Int {
        return key.offset
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderTypeList, Int>>
        get() = object : BuilderMapEntryCollection<BuilderTypeList>(internedItems.values) {
            override fun getValue(key: BuilderTypeList): Int {
                return key.offset
            }

            override fun setValue(key: BuilderTypeList, value: Int): Int {
                val prev = key.offset
                key.offset = value
                return prev
            }
        }
}
