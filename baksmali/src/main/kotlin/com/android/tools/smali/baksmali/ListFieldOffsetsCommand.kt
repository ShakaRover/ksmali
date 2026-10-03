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
import com.beust.jcommander.ParametersDelegate
import com.android.tools.smali.baksmali.formatter.BaksmaliFormatter
import com.android.tools.smali.dexlib2.analysis.ClassProto
import com.android.tools.smali.util.jcommander.ExtendedParameters
import java.io.IOException

@Parameters(commandDescription = "Lists the instance field offsets for classes in a dex file.")
@ExtendedParameters(
    commandName = "fieldoffsets",
    commandAliases = ["fieldoffset", "fo"]
)
open class ListFieldOffsetsCommand(commandAncestors: List<JCommander>) :
    DexInputCommand(commandAncestors) {

    @field:Parameter(
        names = ["-h", "-?", "--help"], help = true,
        description = "Show usage information"
    )
    private var help = false

    @field:ParametersDelegate
    private var analysisArguments = AnalysisArguments()

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
        loadDexFile(input)
        val options = this.options

        val formatter = BaksmaliFormatter()

        try {
            for (classDef in requireNotNull(dexFile).classes) {
                val classProto = requireNotNull(options.classPath).getClass(classDef) as ClassProto
                val fields = classProto.instanceFields
                val className =
                    "Class  ${formatter.getType(classDef.type)} : ${fields.size()} instance fields\n"
                System.out.write(className.toByteArray())
                for (i in 0 until fields.size()) {
                    val field =
                        "${fields.keyAt(i)}:${fields.valueAt(i).type} ${fields.valueAt(i).name}\n"
                    System.out.write(field.toByteArray())
                }
                System.out.write("\n".toByteArray())
            }
            System.out.close()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private val options: BaksmaliOptions
        get() {
            if (dexFile == null) {
                throw IllegalStateException("You must call loadDexFile first")
            }

            val options = BaksmaliOptions()

            options.apiLevel = apiLevel

            try {
                options.classPath = analysisArguments.loadClassPathForDexFile(
                    requireNotNull(inputFile?.absoluteFile?.parentFile),
                    requireNotNull(dexEntry), false)
            } catch (ex: Exception) {
                System.err.println("Error occurred while loading class path files.")
                ex.printStackTrace(System.err)
                System.exit(-1)
            }

            return options
        }
}
