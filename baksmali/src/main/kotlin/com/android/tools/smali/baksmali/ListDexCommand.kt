/*
 * Copyright 2016, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google LLC nor the names of its
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

package com.android.tools.smali.baksmali

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.util.jcommander.Command
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.android.tools.smali.util.jcommander.ExtendedParameters
import java.io.File
import java.io.IOException

@Parameters(commandDescription = "Lists the dex files in an apk/oat file.")
@ExtendedParameters(
    commandName = "dex",
    commandAliases = ["d"]
)
open class ListDexCommand(commandAncestors: List<JCommander>) : Command(commandAncestors) {

    @field:Parameter(
        names = ["-h", "-?", "--help"], help = true,
        description = "Show usage information"
    )
    private var help = false

    @field:Parameter(description = "An apk or oat file.")
    @field:ExtendedParameter(argumentNames = ["file"])
    private var inputList: MutableList<String> = mutableListOf()

    override fun run() {
        if (help || inputList.isEmpty()) {
            usage()
            return
        }

        if (inputList.size > 1) {
            System.err.println("Too many files specified")
            usage()
            return
        }

        val input = inputList[0]
        val file = File(input)

        if (!file.exists()) {
            System.err.println("Could not find the file: $input")
            System.exit(-1)
        }

        val entries: List<String>
        try {
            val container = DexFileFactory.loadDexContainer(file, Opcodes.getDefault())
            entries = container.dexEntryNames
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

        for (entry in entries) {
            System.out.println(entry)
        }
    }
}
