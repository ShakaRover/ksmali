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

import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.HashMap

class MethodImplementationBuilder(registerCount: Int) {
    // Contains all named labels - both placed and unplaced
    private val labels = HashMap<String, Label>()

    private val impl: MutableMethodImplementation = MutableMethodImplementation(registerCount)

    private var currentLocation: MethodLocation = impl.instructionList[0]

    fun getMethodImplementation(): MethodImplementation = impl

    /**
     * Adds a new named label at the current location.
     *
     * Any previous unplaced references to a label of this name will now refer to this label/location
     *
     * @param name The name of the label to add
     * @return A LabelRef representing the label
     */
    fun addLabel(name: String): Label {
        var label = labels[name]

        if (label != null) {
            if (label.isPlaced) {
                throw IllegalArgumentException("There is already a label with that name.")
            } else {
                currentLocation.getLabels().add(label)
            }
        } else {
            label = currentLocation.addNewLabel()
            labels.put(name, label)
        }

        return label
    }

    /**
     * Get a reference to a label with the given name.
     *
     * If a label with that name has not been added yet, a new one is created, but is left
     * in an unplaced state. It is assumed that addLabel(name) will be called at a later
     * point to define the location of the label.
     *
     * @param name The name of the label to get
     * @return A LabelRef representing the label
     */
    fun getLabel(name: String): Label {
        var label = labels[name]
        if (label == null) {
            label = Label()
            labels.put(name, label)
        }
        return label
    }

    fun addCatch(type: TypeReference?, from: Label, to: Label, handler: Label) {
        impl.addCatch(type, from, to, handler)
    }

    fun addCatch(type: String?, from: Label, to: Label, handler: Label) {
        impl.addCatch(type, from, to, handler)
    }

    fun addCatch(from: Label, to: Label, handler: Label) {
        impl.addCatch(from, to, handler)
    }

    fun addLineNumber(lineNumber: Int) {
        currentLocation.addLineNumber(lineNumber)
    }

    fun addStartLocal(
        registerNumber: Int,
        name: StringReference?,
        type: TypeReference?,
        signature: StringReference?
    ) {
        currentLocation.addStartLocal(registerNumber, name, type, signature)
    }

    fun addEndLocal(registerNumber: Int) {
        currentLocation.addEndLocal(registerNumber)
    }

    fun addRestartLocal(registerNumber: Int) {
        currentLocation.addRestartLocal(registerNumber)
    }

    fun addPrologue() {
        currentLocation.addPrologue()
    }

    fun addEpilogue() {
        currentLocation.addEpilogue()
    }

    fun addSetSourceFile(sourceFile: StringReference?) {
        currentLocation.addSetSourceFile(sourceFile)
    }

    fun addInstruction(instruction: BuilderInstruction) {
        impl.addInstruction(instruction)
        currentLocation = impl.instructionList[impl.instructionList.size - 1]
    }
}
