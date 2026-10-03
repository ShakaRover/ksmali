/*
 * Copyright 2012, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.dexlib2.immutable

import com.android.tools.smali.dexlib2.base.BaseTryBlock
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.util.ImmutableConverter

open class ImmutableTryBlock(
    override val startCodeAddress: Int,
    override val codeUnitCount: Int,
    exceptionHandlers: List<ExceptionHandler>?
) : BaseTryBlock<ImmutableExceptionHandler>() {
    override val exceptionHandlers: List<ImmutableExceptionHandler> =
        ImmutableExceptionHandler.immutableListOf(exceptionHandlers)

    companion object {
        fun of(tryBlock: TryBlock<out ExceptionHandler>): ImmutableTryBlock {
            if (tryBlock is ImmutableTryBlock) {
                return tryBlock
            }
            return ImmutableTryBlock(
                tryBlock.startCodeAddress,
                tryBlock.codeUnitCount,
                tryBlock.exceptionHandlers
            )
        }

        fun immutableListOf(list: List<TryBlock<out ExceptionHandler>>?): List<ImmutableTryBlock> {
            return CONVERTER.toList(list)
        }

        private val CONVERTER:
            ImmutableConverter<ImmutableTryBlock, TryBlock<out ExceptionHandler>> =
            object : ImmutableConverter<ImmutableTryBlock, TryBlock<out ExceptionHandler>>() {
                override fun isImmutable(item: TryBlock<out ExceptionHandler>): Boolean {
                    return item is ImmutableTryBlock
                }

                override fun makeImmutable(item: TryBlock<out ExceptionHandler>): ImmutableTryBlock {
                    return of(item)
                }
            }
    }
}
