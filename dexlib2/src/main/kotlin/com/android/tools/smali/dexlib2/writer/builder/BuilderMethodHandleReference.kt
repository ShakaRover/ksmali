package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseMethodHandleReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderMethodHandleReference(
    override val methodHandleType: Int,
    override val memberReference: BuilderReference,
) : BaseMethodHandleReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX
}
