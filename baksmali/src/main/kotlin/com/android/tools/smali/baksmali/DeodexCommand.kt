/*
 * Copyright 2016, Google LLC
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

package com.android.tools.smali.baksmali

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import com.android.tools.smali.baksmali.AnalysisArguments.CheckPackagePrivateArgument
import com.android.tools.smali.dexlib2.analysis.CustomInlineMethodResolver
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.android.tools.smali.util.jcommander.ExtendedParameters
import java.io.File
import java.io.IOException

@Parameters(commandDescription = "Deodexes an odex/oat file")
@ExtendedParameters(
    commandName = "deodex",
    commandAliases = ["de", "x"]
)
open class DeodexCommand(commandAncestors: List<JCommander>) : DisassembleCommand(commandAncestors) {

    @field:ParametersDelegate
    protected var checkPackagePrivateArgument = CheckPackagePrivateArgument()

    @field:Parameter(
        names = ["--inline-table", "--inline", "--it"],
        description = "Specify a file containing a custom inline method table to use. See the " +
            "\"deodexerant\" tool in the smali github repository to dump the inline method table from a " +
            "device that uses dalvik."
    )
    @field:ExtendedParameter(argumentNames = ["file"])
    private var inlineTable: String? = null

    override val options: BaksmaliOptions
        get() {
            val options = super.options

            options.deodex = true

            if (dexFile is DexBackedOdexFile) {
                if (inlineTable == null) {
                    options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(
                        (dexFile as DexBackedOdexFile).odexVersion)
                } else {
                    val inlineTableFile = File(requireNotNull(inlineTable))
                    if (!inlineTableFile.exists()) {
                        System.err.println("Could not find file: $inlineTable")
                        System.exit(-1)
                    }
                    try {
                        options.inlineResolver = CustomInlineMethodResolver(
                            requireNotNull(options.classPath), inlineTableFile)
                    } catch (ex: IOException) {
                        System.err.println("Error while reading file: $inlineTableFile")
                        ex.printStackTrace(System.err)
                        System.exit(-1)
                    }
                }
            }

            return options
        }

    override fun shouldCheckPackagePrivateAccess(): Boolean {
        return checkPackagePrivateArgument.checkPackagePrivateAccess
    }

    override fun needsClassPath(): Boolean {
        return true
    }

    override fun showDeodexWarning(): Boolean {
        return false
    }
}
