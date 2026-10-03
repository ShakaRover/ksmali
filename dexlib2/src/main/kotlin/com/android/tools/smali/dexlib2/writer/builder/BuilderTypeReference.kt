package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderTypeReference internal constructor(internal val stringReference: BuilderStringReference) :
    BaseTypeReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX

    override val type: String
        get() = stringReference.string
}
