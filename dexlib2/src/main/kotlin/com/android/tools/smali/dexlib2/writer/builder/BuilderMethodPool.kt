package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.MethodSection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderMethodPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    MethodSection<BuilderStringReference, BuilderTypeReference, BuilderMethodProtoReference,
        BuilderMethodReference, BuilderMethod> {
    private val internedItems: ConcurrentMap<MethodReference, BuilderMethodReference> = ConcurrentHashMap()

    fun internMethod(methodReference: MethodReference): BuilderMethodReference {
        val ret = internedItems[methodReference]
        if (ret != null) {
            return ret
        }

        val dexPoolMethodReference = BuilderMethodReference(
            dexBuilder.typeSection.internType(methodReference.definingClass),
            dexBuilder.stringSection.internString(methodReference.name),
            dexBuilder.protoSection.internMethodProto(methodReference)
        )
        val existing = internedItems.putIfAbsent(dexPoolMethodReference, dexPoolMethodReference)
        return existing ?: dexPoolMethodReference
    }

    fun internMethod(definingClass: String, name: String, parameters: List<CharSequence>,
                     returnType: String): BuilderMethodReference {
        return internMethod(MethodKey(definingClass, name, parameters, returnType))
    }

    override fun getMethodReference(key: BuilderMethod): BuilderMethodReference {
        return key.methodReference
    }

    override fun getDefiningClass(key: BuilderMethodReference): BuilderTypeReference {
        return key.definingClassReference
    }

    override fun getPrototype(key: BuilderMethodReference): BuilderMethodProtoReference {
        return key.protoReference
    }

    override fun getPrototype(key: BuilderMethod): BuilderMethodProtoReference {
        return key.methodReference.protoReference
    }

    override fun getName(key: BuilderMethodReference): BuilderStringReference {
        return key.nameReference
    }

    override fun getMethodIndex(key: BuilderMethod): Int {
        return key.methodReference.index
    }

    override fun getItemIndex(key: BuilderMethodReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderMethodReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderMethodReference>(internedItems.values) {
            override fun getValue(key: BuilderMethodReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderMethodReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size

    private class MethodKey(
        override val definingClass: String,
        override val name: String,
        override val parameterTypes: List<@JvmWildcard CharSequence>,
        override val returnType: String,
    ) : BaseMethodReference(), MethodReference
}
