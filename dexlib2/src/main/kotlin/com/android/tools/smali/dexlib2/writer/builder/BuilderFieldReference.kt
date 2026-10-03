package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseFieldReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderFieldReference internal constructor(
    internal val definingClassReference: BuilderTypeReference,
    internal val nameReference: BuilderStringReference,
    internal val fieldTypeReference: BuilderTypeReference,
) : BaseFieldReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX

    override val definingClass: String
        get() = definingClassReference.type

    override val name: String
        get() = nameReference.string

    override val type: String
        get() = fieldTypeReference.type
}
