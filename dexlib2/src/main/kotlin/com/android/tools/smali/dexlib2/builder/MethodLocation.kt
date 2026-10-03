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

import com.android.tools.smali.dexlib2.builder.debug.BuilderEndLocal
import com.android.tools.smali.dexlib2.builder.debug.BuilderEpilogueBegin
import com.android.tools.smali.dexlib2.builder.debug.BuilderLineNumber
import com.android.tools.smali.dexlib2.builder.debug.BuilderPrologueEnd
import com.android.tools.smali.dexlib2.builder.debug.BuilderRestartLocal
import com.android.tools.smali.dexlib2.builder.debug.BuilderSetSourceFile
import com.android.tools.smali.dexlib2.builder.debug.BuilderStartLocal
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

open class MethodLocation internal constructor(
    var instruction: BuilderInstruction?,
    var codeAddress: Int,
    var index: Int
) {
    private val labels: LocatedItems<Label> = LocatedLabels()
    private val debugItems: LocatedItems<BuilderDebugItem> = LocatedDebugItems()

    fun getInstruction(): Instruction? = instruction

    fun mergeInto(nextLocation: MethodLocation) {
        labels.mergeItemsIntoNext(nextLocation, nextLocation.labels)
        debugItems.mergeItemsIntoNext(nextLocation, nextLocation.debugItems)
    }

    fun getLabels(): MutableSet<Label> {
        return labels.getModifiableItems(this)
    }

    fun addNewLabel(): Label {
        val newLabel = Label()
        getLabels().add(newLabel)
        return newLabel
    }

    fun getDebugItems(): MutableSet<BuilderDebugItem> {
        return debugItems.getModifiableItems(this)
    }

    fun addLineNumber(lineNumber: Int) {
        getDebugItems().add(BuilderLineNumber(lineNumber))
    }

    fun addStartLocal(
        registerNumber: Int,
        name: StringReference?,
        type: TypeReference?,
        signature: StringReference?
    ) {
        getDebugItems().add(BuilderStartLocal(registerNumber, name, type, signature))
    }

    fun addEndLocal(registerNumber: Int) {
        getDebugItems().add(BuilderEndLocal(registerNumber))
    }

    fun addRestartLocal(registerNumber: Int) {
        getDebugItems().add(BuilderRestartLocal(registerNumber))
    }

    fun addPrologue() {
        getDebugItems().add(BuilderPrologueEnd())
    }

    fun addEpilogue() {
        getDebugItems().add(BuilderEpilogueBegin())
    }

    fun addSetSourceFile(sourceFile: StringReference?) {
        getDebugItems().add(BuilderSetSourceFile(sourceFile))
    }
}
