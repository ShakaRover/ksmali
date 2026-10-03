package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseStringReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderStringReference internal constructor(override val string: String) : BaseStringReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX
}
