package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.BaseMethodParameter

class BuilderMethodParameter(
    internal val typeReference: BuilderTypeReference,
    internal val nameReference: BuilderStringReference?,
    override val annotations: BuilderAnnotationSet,
) : BaseMethodParameter() {
    override val type: String
        get() = typeReference.type

    override val name: String?
        get() = nameReference?.string
}
