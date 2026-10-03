package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodProtoReference
import com.android.tools.smali.dexlib2.util.getShorty
import com.android.tools.smali.dexlib2.writer.ProtoSection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderProtoPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    ProtoSection<BuilderStringReference, BuilderTypeReference, BuilderMethodProtoReference, BuilderTypeList> {
    private val internedItems: ConcurrentMap<MethodProtoReference, BuilderMethodProtoReference> = ConcurrentHashMap()

    fun internMethodProto(methodProto: MethodProtoReference): BuilderMethodProtoReference {
        val ret = internedItems[methodProto]
        if (ret != null) {
            return ret
        }

        val protoReference = BuilderMethodProtoReference(
            dexBuilder.stringSection.internString(getShorty(methodProto.parameterTypes, methodProto.returnType)),
            dexBuilder.typeListSection.internTypeList(methodProto.parameterTypes),
            dexBuilder.typeSection.internType(methodProto.returnType)
        )
        val existing = internedItems.putIfAbsent(protoReference, protoReference)
        return existing ?: protoReference
    }

    fun internMethodProto(methodReference: MethodReference): BuilderMethodProtoReference {
        return internMethodProto(
            ImmutableMethodProtoReference(methodReference.parameterTypes, methodReference.returnType)
        )
    }

    override fun getShorty(key: BuilderMethodProtoReference): BuilderStringReference {
        return key.shortyReference
    }

    override fun getReturnType(key: BuilderMethodProtoReference): BuilderTypeReference {
        return key.returnTypeReference
    }

    override fun getParameters(key: BuilderMethodProtoReference): BuilderTypeList {
        return key.parameterTypesList
    }

    override fun getItemIndex(key: BuilderMethodProtoReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderMethodProtoReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderMethodProtoReference>(internedItems.values) {
            override fun getValue(key: BuilderMethodProtoReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderMethodProtoReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
