package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.BaseAnnotation
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderAnnotation(
    override val visibility: Int,
    internal val typeReference: BuilderTypeReference,
    override val elements: Set<@JvmWildcard BuilderAnnotationElement>,
) : BaseAnnotation() {
    var offset = DexWriter.NO_OFFSET

    override val type: String
        get() = typeReference.type
}
