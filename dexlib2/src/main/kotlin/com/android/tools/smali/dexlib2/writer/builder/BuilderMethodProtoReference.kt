package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseMethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.writer.DexWriter

class BuilderMethodProtoReference(
    internal val shortyReference: BuilderStringReference,
    internal val parameterTypesList: BuilderTypeList,
    internal val returnTypeReference: BuilderTypeReference,
) : BaseMethodProtoReference(), MethodProtoReference, BuilderReference {
    override var index = DexWriter.NO_INDEX

    override val parameterTypes: List<@JvmWildcard CharSequence>
        get() = parameterTypesList

    override val returnType: String
        get() = returnTypeReference.type
}
