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
import com.beust.jcommander.validators.PositiveInteger
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver
import com.android.tools.smali.util.getConsoleWidth
import com.android.tools.smali.util.StringWrapper
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.android.tools.smali.util.jcommander.ExtendedParameters
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException

@Parameters(commandDescription = "Disassembles a dex file.")
@ExtendedParameters(
    commandName = "disassemble",
    commandAliases = ["dis", "d"]
)
open class DisassembleCommand(commandAncestors: List<JCommander>) : DexInputCommand(commandAncestors) {

    @field:Parameter(
        names = ["-h", "-?", "--help"], help = true,
        description = "Show usage information for this command."
    )
    private var help = false

    @field:ParametersDelegate
    protected var analysisArguments = AnalysisArguments()

    @field:Parameter(
        names = ["--debug-info", "--di"], arity = 1,
        description = "Whether to include debug information in the output (.local, .param, .line, etc.). True " +
            "by default, use --debug-info=false to disable."
    )
    @field:ExtendedParameter(argumentNames = ["boolean"])
    private var debugInfo = true

    @field:Parameter(
        names = ["--code-offsets", "--offsets", "--off"],
        description = "Add a comment before each instruction with it's code offset within the method."
    )
    private var codeOffsets = false

    @field:Parameter(
        names = ["--resolve-resources", "--rr"], arity = 2,
        description = "This will attempt to find any resource id references within the bytecode and add a " +
            "comment with the name of the resource being referenced. The parameter accepts 2 values:" +
            "an arbitrary resource prefix and the path to a public.xml file. For example: " +
            "--resolve-resources android.R framework/res/values/public.xml. This option can be specified " +
            "multiple times to provide resources from multiple packages."
    )
    @field:ExtendedParameter(argumentNames = ["resource prefix", "public.xml file"])
    private var resourceIdFiles: MutableList<String> = Lists.newArrayList()

    @field:Parameter(
        names = ["-j", "--jobs"],
        description = "The number of threads to use. Defaults to the number of cores available.",
        validateWith = [PositiveInteger::class]
    )
    @field:ExtendedParameter(argumentNames = ["n"])
    private var jobs = Runtime.getRuntime().availableProcessors()

    @field:Parameter(
        names = ["-l", "--use-locals"],
        description = "When disassembling, output the .locals directive with the number of non-parameter " +
            "registers instead of the .registers directive with the total number of registers."
    )
    private var localsDirective = false

    @field:Parameter(
        names = ["--accessor-comments", "--ac"], arity = 1,
        description = "Generate helper comments for synthetic accessors. True by default, use " +
            "--accessor-comments=false to disable."
    )
    @field:ExtendedParameter(argumentNames = ["boolean"])
    private var accessorComments = true

    @field:Parameter(
        names = ["--normalize-virtual-methods", "--norm", "--nvm"],
        description = "Normalize virtual method references to use the base class where the method is " +
            "originally declared."
    )
    private var normalizeVirtualMethods = false

    @field:Parameter(
        names = ["-o", "--output"],
        description = "The directory to write the disassembled files to."
    )
    @field:ExtendedParameter(argumentNames = ["dir"])
    private var outputDir = "out"

    @field:Parameter(
        names = ["--parameter-registers", "--preg", "--pr"], arity = 1,
        description = "Use the pNN syntax for registers that refer to a method parameter on method entry. True " +
            "by default, use --parameter-registers=false to disable."
    )
    @field:ExtendedParameter(argumentNames = ["boolean"])
    private var parameterRegisters = true

    @field:Parameter(
        names = ["-r", "--register-info"],
        description = "Add comments before/after each instruction with information about register types. " +
            "The value is a comma-separated list of any of ALL, ALLPRE, ALLPOST, ARGS, DEST, MERGE and " +
            "FULLMERGE. See \"baksmali help register-info\" for more information."
    )
    @field:ExtendedParameter(argumentNames = ["register info specifier"])
    private var registerInfoTypes: MutableList<String> = Lists.newArrayList()

    @field:Parameter(
        names = ["--sequential-labels", "--seq", "--sl"],
        description = "Create label names using a sequential numbering scheme per label type, rather than " +
            "using the bytecode address."
    )
    private var sequentialLabels = false

    @field:Parameter(
        names = ["--implicit-references", "--implicit", "--ir"],
        description = "Use implicit method and field references (without the class name) for methods and " +
            "fields from the current class."
    )
    private var implicitReferences = false

    @field:Parameter(
        names = ["--allow-odex-opcodes"],
        description = "Allows odex opcodes to be disassembled, even if the result won't be able to be reassembled."
    )
    private var allowOdex = false

    @field:Parameter(
        names = ["--classes"],
        description = "A comma separated list of classes. Only disassemble these classes"
    )
    @field:ExtendedParameter(argumentNames = ["classes"])
    private var classes: MutableList<String>? = null

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

        if (showDeodexWarning() && dexFile!!.supportsOptimizedOpcodes()) {
            StringWrapper.printWrappedString(
                System.err,
                "Warning: You are disassembling an odex/oat file without deodexing it. You won't be able to " +
                    "re-assemble the results unless you deodex it. See \"baksmali help deodex\"",
                getConsoleWidth()
            )
        }

        val outputDirectoryFile = File(outputDir)
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory $outputDir")
                System.exit(-1)
            }
        }

        if (analysisArguments.classPathDirectories == null ||
            analysisArguments.classPathDirectories!!.isEmpty()) {
            analysisArguments.classPathDirectories =
                Lists.newArrayList<String>(inputFile!!.absoluteFile.parent!!)
        }

        if (!disassembleDexFile(dexFile!!, outputDirectoryFile, jobs, getOptions(), classes)) {
            System.exit(-1)
        }
    }

    protected open fun needsClassPath(): Boolean {
        return registerInfoTypes.isNotEmpty() || normalizeVirtualMethods
    }

    protected open fun shouldCheckPackagePrivateAccess(): Boolean {
        return false
    }

    protected open fun showDeodexWarning(): Boolean {
        return true
    }

    protected open fun getOptions(): BaksmaliOptions {
        if (dexFile == null) {
            throw IllegalStateException("You must call loadDexFile first")
        }

        val options = BaksmaliOptions()

        if (needsClassPath()) {
            try {
                options.classPath = analysisArguments.loadClassPathForDexFile(
                    inputFile!!.absoluteFile.parentFile!!, dexEntry!!, shouldCheckPackagePrivateAccess())
            } catch (ex: Exception) {
                System.err.println("\n\nError occurred while loading class path files. Aborting.")
                ex.printStackTrace(System.err)
                System.exit(-1)
            }
        }

        if (resourceIdFiles.isNotEmpty()) {
            val resourceFiles = Maps.newHashMap<String, File>()

            assert((resourceIdFiles.size % 2) == 0)
            var i = 0
            while (i < resourceIdFiles.size) {
                val resourcePrefix = resourceIdFiles[i]
                val publicXml = resourceIdFiles[i + 1]

                val publicXmlFile = File(publicXml)

                if (!publicXmlFile.exists()) {
                    System.err.println(String.format("Can't find file: %s", publicXmlFile))
                    System.exit(-1)
                }

                resourceFiles[resourcePrefix] = publicXmlFile
                i += 2
            }

            try {
                options.loadResourceIds(resourceFiles)
            } catch (ex: IOException) {
                System.err.println("Error while loading resource files:")
                ex.printStackTrace(System.err)
                System.exit(-1)
            } catch (ex: SAXException) {
                System.err.println("Error while loading resource files:")
                ex.printStackTrace(System.err)
                System.exit(-1)
            }
        }

        options.parameterRegisters = parameterRegisters
        options.localsDirective = localsDirective
        options.sequentialLabels = sequentialLabels
        options.debugInfo = debugInfo
        options.codeOffsets = codeOffsets
        options.accessorComments = accessorComments
        options.implicitReferences = implicitReferences
        options.normalizeVirtualMethods = normalizeVirtualMethods

        options.registerInfo = 0

        for (registerInfoType in registerInfoTypes) {
            if (registerInfoType.equals("ALL", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.ALL
            } else if (registerInfoType.equals("ALLPRE", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.ALLPRE
            } else if (registerInfoType.equals("ALLPOST", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.ALLPOST
            } else if (registerInfoType.equals("ARGS", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.ARGS
            } else if (registerInfoType.equals("DEST", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.DEST
            } else if (registerInfoType.equals("MERGE", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.MERGE
            } else if (registerInfoType.equals("FULLMERGE", ignoreCase = true)) {
                options.registerInfo = options.registerInfo or BaksmaliOptions.FULLMERGE
            } else {
                System.err.println(String.format("Invalid register info type: %s", registerInfoType))
                usage()
                System.exit(-1)
            }

            if ((options.registerInfo and BaksmaliOptions.FULLMERGE) != 0) {
                options.registerInfo = options.registerInfo and BaksmaliOptions.MERGE.inv()
            }
        }

        if (accessorComments) {
            options.syntheticAccessorResolver = SyntheticAccessorResolver(
                dexFile!!.opcodes, dexFile!!.classes)
        }

        if (allowOdex) {
            options.allowOdex = true
        }

        return options
    }
}
