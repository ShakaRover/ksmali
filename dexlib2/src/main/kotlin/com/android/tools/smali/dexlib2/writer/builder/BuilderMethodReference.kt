package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderMethodReference internal constructor(
    internal val definingClassReference: BuilderTypeReference,
    internal val nameReference: BuilderStringReference,
    internal val protoReference: BuilderMethodProtoReference,
) : BaseMethodReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX

    override val definingClass: String
        get() = definingClassReference.type

    override val name: String
        get() = nameReference.string

    override val parameterTypes: BuilderTypeList
        get() = protoReference.parameterTypesList

    override val returnType: String
        get() = protoReference.returnTypeReference.type
}
