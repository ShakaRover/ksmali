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


package com.android.tools.smali.dexlib2.builder

import com.android.tools.smali.dexlib2.base.BaseTryBlock
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

open class BuilderTryBlock : BaseTryBlock<BuilderExceptionHandler> {
    // We only ever have one exception handler per try block. They are later merged as needed in TryListBuilder
    val exceptionHandler: BuilderExceptionHandler

    val start: Label

    // The end location is exclusive, it should point to the codeAddress of the instruction immediately after the last
    // covered instruction.
    val end: Label

    constructor(start: Label, end: Label, exceptionType: String?, handler: Label) : super() {
        this.start = start
        this.end = end
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(exceptionType, handler)
    }

    constructor(start: Label, end: Label, exceptionType: TypeReference?, handler: Label) : super() {
        this.start = start
        this.end = end
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(exceptionType, handler)
    }

    constructor(start: Label, end: Label, handler: Label) : super() {
        this.start = start
        this.end = end
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(handler)
    }

    override val startCodeAddress: Int
        get() = start.codeAddress

    override val codeUnitCount: Int
        get() = end.codeAddress - start.codeAddress

    override val exceptionHandlers: List<BuilderExceptionHandler>
        get() = listOf(exceptionHandler)
}
