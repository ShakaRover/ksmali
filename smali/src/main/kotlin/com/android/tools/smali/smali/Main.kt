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

package com.android.tools.smali.smali

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.android.tools.smali.util.jcommander.Command
import com.android.tools.smali.util.jcommander.addExtendedCommand
import com.android.tools.smali.util.jcommander.ExtendedParameters
import java.io.IOException
import java.util.Properties

@ExtendedParameters(
    includeParametersInUsage = true,
    commandName = "smali",
    postfixDescription = "See smali help <command> for more information about a specific command"
)
open class Main : Command(mutableListOf()) {
    @field:Parameter(names = ["-h", "-?", "--help"], help = true,
        description = "Show usage information")
    private var help = false

    @field:Parameter(names = ["-v", "--version"], help = true,
        description = "Print the version of baksmali and then exit")
    var version = false

    private lateinit var jc: JCommander

    override fun run() {
    }

    protected override fun getJCommander(): JCommander = jc

    companion object {
        val VERSION: String = loadVersion()

        @JvmStatic
        fun main(args: Array<String>) {
            val main = Main()

            val jc = JCommander(main)
            main.jc = jc
            jc.programName = "smali"
            val commandHierarchy = main.commandHierarchy

            addExtendedCommand(jc, AssembleCommand(commandHierarchy))
            addExtendedCommand(jc, PrintTokensCommand(commandHierarchy))
            addExtendedCommand(jc, HelpCommand(commandHierarchy))
            addExtendedCommand(jc, HelpCommand.HlepCommand(commandHierarchy))

            jc.parse(*args)

            if (main.version) {
                version()
            }

            if (jc.parsedCommand == null || main.help) {
                main.usage()
                return
            }

            val command = requireNotNull(jc.commands[jc.parsedCommand]).objects[0] as Command
            command.run()
        }

        protected fun version() {
            System.out.println("smali " + VERSION + " (https://github.com/ShakaRover/ksmali)")
            System.out.println("Copyright (C) 2010 Ben Gruver (JesusFreke@JesusFreke.com)")
            System.out.println("BSD license (https://opensource.org/license/bsd-3-clause)")
            System.exit(0)
        }

        private fun loadVersion(): String {
            val propertiesStream = Main::class.java.classLoader.getResourceAsStream("smali.properties")
            var version = "[unknown version]"
            if (propertiesStream != null) {
                val properties = Properties()
                try {
                    properties.load(propertiesStream)
                    version = properties.getProperty("application.version")
                } catch (ex: IOException) {
                    // ignore
                }
            }
            return version
        }
    }
}
