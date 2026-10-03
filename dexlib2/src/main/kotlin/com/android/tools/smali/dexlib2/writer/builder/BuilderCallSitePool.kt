package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.writer.CallSiteSection
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.util.CallSiteUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderCallSitePool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    CallSiteSection<BuilderCallSiteReference, BuilderArrayEncodedValue> {
    private val internedItems: ConcurrentMap<CallSiteReference, BuilderCallSiteReference> = ConcurrentHashMap()

    fun internCallSite(callSiteReference: CallSiteReference): BuilderCallSiteReference {
        var internedCallSite = internedItems[callSiteReference]
        if (internedCallSite != null) {
            return internedCallSite
        }
        val encodedCallSite = dexBuilder.encodedArraySection.internArrayEncodedValue(
            CallSiteUtil.getEncodedCallSite(callSiteReference)
        )
        internedCallSite = BuilderCallSiteReference(callSiteReference.name, encodedCallSite)
        val existing = internedItems.putIfAbsent(internedCallSite, internedCallSite)
        return existing ?: internedCallSite
    }

    override fun getEncodedCallSite(callSiteReference: BuilderCallSiteReference): BuilderArrayEncodedValue {
        return callSiteReference.encodedCallSite
    }

    override fun getItemIndex(key: BuilderCallSiteReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderCallSiteReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderCallSiteReference>(internedItems.values) {
            override fun getValue(key: BuilderCallSiteReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderCallSiteReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
