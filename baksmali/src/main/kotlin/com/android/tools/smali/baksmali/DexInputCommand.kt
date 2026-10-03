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
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.util.jcommander.Command
import com.android.tools.smali.util.jcommander.ExtendedParameter
import java.io.File
import java.io.IOException

/**
 * This class implements common functionality for commands that need to load a dex file based on
 * command line input
 */
abstract class DexInputCommand(commandAncestors: List<JCommander>) : Command(commandAncestors) {

    @field:Parameter(
        names = ["-a", "--api"],
        description = "The numeric api level of the file being disassembled."
    )
    @field:ExtendedParameter(argumentNames = ["api"])
    var apiLevel = -1

    @field:Parameter(
        description = "A dex/apk/oat/odex file. For apk or oat files that contain multiple dex " +
            "files, you can specify the specific entry to use as if the apk/oat file was a directory. " +
            "e.g. \"app.apk/classes2.dex\". For more information, see \"baksmali help input\"."
    )
    @field:ExtendedParameter(argumentNames = ["file"])
    protected var inputList: MutableList<String> = mutableListOf()

    protected var inputFile: File? = null
    protected var inputEntry: String? = null
    protected var dexEntry: MultiDexContainer.DexEntry<out DexBackedDexFile>? = null
    protected var dexFile: DexBackedDexFile? = null

    /**
     * Parses a dex file input from the user and loads the given dex file.
     *
     * In some cases, the input file can contain multiple dex files. If this is the case, you can refer to a specific
     * dex file with a slash, followed by the entry name, optionally in quotes.
     *
     * If the entry name is enclosed in quotes, then it will strip the first and last quote and look for an entry with
     * exactly that name. Otherwise, it will perform a partial filename match against the entry to find any candidates.
     * If there is a single matching candidate, it will be used. Otherwise, an error will be generated.
     *
     * For example, to refer to the "/system/framework/framework.jar:classes2.dex" entry within the
     * "framework/arm/framework.oat" oat file, you could use any of:
     *
     * framework/arm/framework.oat/"/system/framework/framework.jar:classes2.dex"
     * framework/arm/framework.oat/system/framework/framework.jar:classes2.dex
     * framework/arm/framework.oat/framework/framework.jar:classes2.dex
     * framework/arm/framework.oat/framework.jar:classes2.dex
     * framework/arm/framework.oat/classes2.dex
     *
     * The last option is the easiest, but only works if the oat file doesn't contain another entry with the
     * "classes2.dex" name. e.g. "/system/framework/blah.jar:classes2.dex"
     *
     * It's technically possible (although unlikely) for an oat file to contain 2 entries like:
     * /system/framework/framework.jar:classes2.dex
     * system/framework/framework.jar:classes2.dex
     *
     * In this case, the "framework/arm/framework.oat/system/framework/framework.jar:classes2.dex" syntax will generate
     * an error because both entries match the partial entry name. Instead, you could use the following for the
     * first and second entry respectively:
     *
     * framework/arm/framework.oat/"/system/framework/framework.jar:classes2.dex"
     * framework/arm/framework.oat/"system/framework/framework.jar:classes2.dex"
     *
     * @param input The name of a dex, apk, odex or oat file/entry.
     */
    protected fun loadDexFile(input: String) {
        var file: File? = File(input)

        while (file != null && !file.exists()) {
            file = file.parentFile
        }

        val foundFile = file
        if (foundFile == null || !foundFile.exists() || foundFile.isDirectory) {
            System.err.println("Can't find file: $input")
            System.exit(1)
            return
        }

        inputFile = foundFile

        var dexEntryName: String? = null
        if (foundFile.path.length < input.length) {
            dexEntryName = input.substring(foundFile.path.length + 1)
        }

        var opcodes: Opcodes? = null
        if (apiLevel != -1) {
            opcodes = Opcodes.forApi(apiLevel)
        }

        if (!dexEntryName.isNullOrEmpty()) {
            var exactMatch = false
            var entryName = requireNotNull(dexEntryName)
            if (entryName.length > 2 && entryName[0] == '"' && entryName[entryName.length - 1] == '"') {
                entryName = entryName.substring(1, entryName.length - 1)
                exactMatch = true
            }

            inputEntry = entryName

            try {
                val entry = DexFileFactory.loadDexEntry(foundFile, entryName, exactMatch, opcodes)
                dexEntry = entry
                dexFile = entry.dexFile
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        } else {
            try {
                val container = DexFileFactory.loadDexContainer(foundFile, opcodes)

                if (container.dexEntryNames.size == 1) {
                    val entry = requireNotNull(container.getEntry(container.dexEntryNames[0]))
                    dexEntry = entry
                    dexFile = entry.dexFile
                } else if (container.dexEntryNames.size > 1) {
                    var entry = container.getEntry("classes.dex")
                    if (entry == null) {
                        entry = container.getEntry(container.dexEntryNames[0])
                    }
                    entry = requireNotNull(entry)
                    dexEntry = entry
                    dexFile = entry.dexFile
                } else {
                    throw RuntimeException("\"$input\" has no dex files")
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }
    }
}
