package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseCallSiteReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderMethodTypeEncodedValue

class BuilderCallSiteReference(
    override val name: String,
    internal val encodedCallSite: BuilderArrayEncodedValue,
) : BaseCallSiteReference(), BuilderReference {
    override var index = DexWriter.NO_INDEX

    override val methodHandle: BuilderMethodHandleReference
        get() = (encodedCallSite.elements[0] as BuilderMethodHandleEncodedValue).value

    override val methodName: String
        get() = (encodedCallSite.elements[1] as StringEncodedValue).value

    override val methodProto: BuilderMethodProtoReference
        get() = (encodedCallSite.elements[2] as BuilderMethodTypeEncodedValue).value

    override val extraArguments: List<@JvmWildcard BuilderEncodedValue>
        get() {
            if (encodedCallSite.elements.size <= 3) {
                return emptyList()
            }
            return encodedCallSite.elements.subList(3, encodedCallSite.elements.size)
        }
}
