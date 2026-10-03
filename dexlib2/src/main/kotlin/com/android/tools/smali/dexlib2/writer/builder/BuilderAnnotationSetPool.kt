package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.writer.AnnotationSetSection
import com.android.tools.smali.dexlib2.writer.DexWriter
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Collectors

class BuilderAnnotationSetPool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    AnnotationSetSection<BuilderAnnotation, BuilderAnnotationSet> {
    private val internedItems: ConcurrentMap<Set<out Annotation>, BuilderAnnotationSet> = ConcurrentHashMap()

    fun internAnnotationSet(annotations: Set<out Annotation>?): BuilderAnnotationSet {
        if (annotations == null) {
            return BuilderAnnotationSet.EMPTY
        }

        val ret = internedItems[annotations]
        if (ret != null) {
            return ret
        }

        val annotationSet = BuilderAnnotationSet(
            Collections.unmodifiableSet(
                annotations.stream()
                    .map { annotation -> dexBuilder.annotationSection.internAnnotation(annotation) }
                    .collect(Collectors.toSet())
            )
        )

        val existing = internedItems.putIfAbsent(annotationSet, annotationSet)
        return existing ?: annotationSet
    }

    override fun getAnnotations(key: BuilderAnnotationSet): Collection<@JvmWildcard BuilderAnnotation> {
        return key.annotations
    }

    override fun getNullableItemOffset(key: BuilderAnnotationSet?): Int {
        return if (key == null) DexWriter.NO_OFFSET else key.offset
    }

    override fun getItemOffset(key: BuilderAnnotationSet): Int {
        return key.offset
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderAnnotationSet, Int>>
        get() = object : BuilderMapEntryCollection<BuilderAnnotationSet>(internedItems.values) {
            override fun getValue(key: BuilderAnnotationSet): Int {
                return key.offset
            }

            override fun setValue(key: BuilderAnnotationSet, value: Int): Int {
                val prev = key.offset
                key.offset = value
                return prev
            }
        }
}
