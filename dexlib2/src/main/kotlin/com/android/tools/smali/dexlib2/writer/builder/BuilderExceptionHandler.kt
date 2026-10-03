package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.BaseExceptionHandler

class BuilderExceptionHandler internal constructor(
    override val exceptionTypeReference: BuilderTypeReference?,
    override val handlerCodeAddress: Int,
) : BaseExceptionHandler() {
    override val exceptionType: String?
        get() = exceptionTypeReference?.type
}
