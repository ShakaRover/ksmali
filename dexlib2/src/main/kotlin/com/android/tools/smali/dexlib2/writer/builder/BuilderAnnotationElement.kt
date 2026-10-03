package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.BaseAnnotationElement
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue

class BuilderAnnotationElement(
    internal val nameReference: BuilderStringReference,
    internal val valueReference: BuilderEncodedValue,
) : BaseAnnotationElement() {
    override val name: String
        get() = nameReference.string

    override val value: EncodedValue
        get() = valueReference
}
