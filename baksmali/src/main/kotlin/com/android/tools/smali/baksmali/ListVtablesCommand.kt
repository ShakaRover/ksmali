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
import com.android.tools.smali.baksmali.AnalysisArguments.CheckPackagePrivateArgument
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.analysis.ClassProto
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.android.tools.smali.util.jcommander.ExtendedParameters
import java.io.IOException

@Parameters(commandDescription = "Lists the virtual method tables for classes in a dex file.")
@ExtendedParameters(
    commandName = "vtables",
    commandAliases = ["vtable", "v"]
)
open class ListVtablesCommand(commandAncestors: List<JCommander>) :
    DexInputCommand(commandAncestors) {

    @field:Parameter(
        names = ["-h", "-?", "--help"], help = true,
        description = "Show usage information"
    )
    private var help = false

    @field:ParametersDelegate
    private var analysisArguments = AnalysisArguments()

    @field:ParametersDelegate
    private var checkPackagePrivateArgument = CheckPackagePrivateArgument()

    @field:Parameter(
        names = ["--classes"],
        description = "A comma separated list of classes. Only print the vtable for these classes"
    )
    @field:ExtendedParameter(argumentNames = ["classes"])
    private var classes: MutableList<String>? = null

    @field:Parameter(
        names = ["--override-oat-version"],
        description = "Uses a classpath for the given oat version, regardless of the actual oat version. This " +
            "can be used, e.g. to list vtables from a dex file, as if they were in an oat file of the given " +
            "version."
    )
    private var oatVersion = 0

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

        val options = getOptions() ?: return

        try {
            val classList = classes
            if (classList != null && !classList.isEmpty()) {
                for (cls in classList) {
                    listClassVtable(options.classPath!!.getClass(cls) as ClassProto)
                }
                return
            }

            for (classDef in dexFile!!.classes) {
                if (!AccessFlags.INTERFACE.isSet(classDef.accessFlags)) {
                    listClassVtable(options.classPath!!.getClass(classDef) as ClassProto)
                }
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    @Throws(IOException::class)
    private fun listClassVtable(classProto: ClassProto) {
        val methods: List<Method> = classProto.vtable
        val className = "Class " + classProto.type + " extends " + classProto.superclass +
            " : " + methods.size + " methods\n"
        System.out.write(className.toByteArray())
        for (i in methods.indices) {
            val method = methods[i]

            var methodString = i.toString() + ":" + method.definingClass + "->" + method.name + "("
            for (parameter in method.parameterTypes) {
                methodString += parameter
            }
            methodString += ")" + method.returnType + "\n"
            System.out.write(methodString.toByteArray())
        }
        System.out.write("\n".toByteArray())
    }

    protected fun getOptions(): BaksmaliOptions? {
        if (dexFile == null) {
            throw IllegalStateException("You must call loadDexFile first")
        }

        val options = BaksmaliOptions()

        options.apiLevel = apiLevel

        try {
            options.classPath = analysisArguments.loadClassPathForDexFile(
                inputFile!!.absoluteFile.parentFile!!,
                dexEntry!!, checkPackagePrivateArgument.checkPackagePrivateAccess, oatVersion)
        } catch (ex: Exception) {
            System.err.println("Error occurred while loading class path files.")
            ex.printStackTrace(System.err)
            return null
        }

        return options
    }
}
