package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.writer.AnnotationSection
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderAnnotationPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    AnnotationSection<BuilderStringReference, BuilderTypeReference, BuilderAnnotation, BuilderAnnotationElement,
        BuilderEncodedValue> {
    private val internedItems: ConcurrentMap<Annotation, BuilderAnnotation> = ConcurrentHashMap()

    fun internAnnotation(annotation: Annotation): BuilderAnnotation {
        val ret = internedItems[annotation]
        if (ret != null) {
            return ret
        }

        val dexBuilderAnnotation = BuilderAnnotation(
            annotation.visibility,
            dexBuilder.typeSection.internType(annotation.type),
            dexBuilder.internAnnotationElements(annotation.elements)
        )
        val existing = internedItems.putIfAbsent(dexBuilderAnnotation, dexBuilderAnnotation)
        return existing ?: dexBuilderAnnotation
    }

    override fun getVisibility(key: BuilderAnnotation): Int {
        return key.visibility
    }

    override fun getType(key: BuilderAnnotation): BuilderTypeReference {
        return key.typeReference
    }

    override fun getElements(key: BuilderAnnotation): Collection<@JvmWildcard BuilderAnnotationElement> {
        return key.elements
    }

    override fun getElementName(element: BuilderAnnotationElement): BuilderStringReference {
        return element.nameReference
    }

    override fun getElementValue(element: BuilderAnnotationElement): BuilderEncodedValue {
        return element.valueReference
    }

    override fun getItemOffset(key: BuilderAnnotation): Int {
        return key.offset
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderAnnotation, Int>>
        get() = object : BuilderMapEntryCollection<BuilderAnnotation>(internedItems.values) {
            override fun getValue(key: BuilderAnnotation): Int {
                return key.offset
            }

            override fun setValue(key: BuilderAnnotation, value: Int): Int {
                val prev = key.offset
                key.offset = value
                return prev
            }
        }
}
