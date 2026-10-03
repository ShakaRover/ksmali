package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.writer.FieldSection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderFieldPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    FieldSection<BuilderStringReference, BuilderTypeReference, BuilderFieldReference, BuilderField> {
    private val internedItems: ConcurrentMap<FieldReference, BuilderFieldReference> = ConcurrentHashMap()

    internal fun internField(definingClass: String, name: String, type: String): BuilderFieldReference {
        val fieldReference = ImmutableFieldReference(definingClass, name, type)
        return internField(fieldReference)
    }

    fun internField(fieldReference: FieldReference): BuilderFieldReference {
        val ret = internedItems[fieldReference]
        if (ret != null) {
            return ret
        }

        val dexPoolFieldReference = BuilderFieldReference(
            dexBuilder.typeSection.internType(fieldReference.definingClass),
            dexBuilder.stringSection.internString(fieldReference.name),
            dexBuilder.typeSection.internType(fieldReference.type)
        )
        val existing = internedItems.putIfAbsent(dexPoolFieldReference, dexPoolFieldReference)
        return existing ?: dexPoolFieldReference
    }

    override fun getDefiningClass(key: BuilderFieldReference): BuilderTypeReference {
        return key.definingClassReference
    }

    override fun getFieldType(key: BuilderFieldReference): BuilderTypeReference {
        return key.fieldTypeReference
    }

    override fun getName(key: BuilderFieldReference): BuilderStringReference {
        return key.nameReference
    }

    override fun getFieldIndex(key: BuilderField): Int {
        return key.fieldReference.index
    }

    override fun getItemIndex(key: BuilderFieldReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderFieldReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderFieldReference>(internedItems.values) {
            override fun getValue(key: BuilderFieldReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderFieldReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
