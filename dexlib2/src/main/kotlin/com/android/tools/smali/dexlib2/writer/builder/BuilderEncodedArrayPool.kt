package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.EncodedArraySection
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderEncodedArrayPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    EncodedArraySection<BuilderArrayEncodedValue, BuilderEncodedValue> {
    private val internedItems: ConcurrentMap<ArrayEncodedValue, BuilderArrayEncodedValue> = ConcurrentHashMap()

    fun internArrayEncodedValue(arrayEncodedValue: ArrayEncodedValue): BuilderArrayEncodedValue {
        var builderArrayEncodedValue = internedItems[arrayEncodedValue]
        if (builderArrayEncodedValue != null) {
            return builderArrayEncodedValue
        }

        builderArrayEncodedValue = dexBuilder.internEncodedValue(arrayEncodedValue) as BuilderArrayEncodedValue
        val previous = internedItems.putIfAbsent(builderArrayEncodedValue, builderArrayEncodedValue)
        return previous ?: builderArrayEncodedValue
    }

    override fun getItemOffset(key: BuilderArrayEncodedValue): Int {
        return key.offset
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderArrayEncodedValue, Int>>
        get() = object : BuilderMapEntryCollection<BuilderArrayEncodedValue>(internedItems.values) {
            override fun getValue(key: BuilderArrayEncodedValue): Int {
                return key.offset
            }

            override fun setValue(key: BuilderArrayEncodedValue, value: Int): Int {
                val prev = key.offset
                key.offset = value
                return prev
            }
        }

    override fun getEncodedValueList(encodedArrayKey: BuilderArrayEncodedValue):
        List<@JvmWildcard BuilderEncodedValue> {
        return encodedArrayKey.elements
    }
}
