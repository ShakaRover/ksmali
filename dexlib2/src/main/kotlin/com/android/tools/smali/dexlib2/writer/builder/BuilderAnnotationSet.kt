package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.writer.DexWriter
import java.util.AbstractSet

class BuilderAnnotationSet(internal val annotations: Set<BuilderAnnotation>) : AbstractSet<BuilderAnnotation>() {
    var offset = DexWriter.NO_OFFSET

    override fun iterator(): MutableIterator<BuilderAnnotation> {
        return annotations.iterator() as MutableIterator<BuilderAnnotation>
    }

    override val size: Int
        get() = annotations.size

    companion object {
        val EMPTY = BuilderAnnotationSet(emptySet())
    }
}
