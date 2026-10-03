package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.BaseTryBlock

class BuilderTryBlock(
    override val startCodeAddress: Int,
    override val codeUnitCount: Int,
    override val exceptionHandlers: List<@JvmWildcard BuilderExceptionHandler>,
) : BaseTryBlock<BuilderExceptionHandler>()
