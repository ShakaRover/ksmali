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

import com.android.tools.smali.util.getConsoleWidth
import com.android.tools.smali.util.jcommander.Command
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.android.tools.smali.util.jcommander.ExtendedParameters
import com.android.tools.smali.util.jcommander.HelpFormatter
import com.android.tools.smali.util.jcommander.getSubcommand
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.google.common.collect.Iterables

@Parameters(commandDescription = "Shows usage information")
@ExtendedParameters(
    commandName = "help",
    commandAliases = ["h"]
)
open class ListHelpCommand(commandAncestors: List<JCommander>) : Command(commandAncestors) {

    @field:Parameter(description = "If specified, show the detailed usage information for the given commands")
    @field:ExtendedParameter(argumentNames = ["commands"])
    private var commands: MutableList<String>? = null

    override fun run() {
        val cmds = commands
        if (cmds.isNullOrEmpty()) {
            System.out.println(
                HelpFormatter()
                    .width(getConsoleWidth())
                    .format(commandAncestors)
            )
        } else {
            var printedHelp = false
            val parentJc = Iterables.getLast(commandAncestors)
            for (cmd in cmds) {
                val command = getSubcommand(parentJc, cmd)
                if (command == null) {
                    System.err.println("No such command: $cmd")
                } else {
                    printedHelp = true
                    System.out.println(
                        HelpFormatter()
                            .width(getConsoleWidth())
                            .format((command.objects[0] as Command).getCommandHierarchy())
                    )
                }
            }
            if (!printedHelp) {
                System.out.println(
                    HelpFormatter()
                        .width(getConsoleWidth())
                        .format(commandAncestors)
                )
            }
        }
    }

    @Parameters(hidden = true)
    @ExtendedParameters(commandName = "hlep")
    open class ListHlepCommand(commandAncestors: List<JCommander>) : ListHelpCommand(commandAncestors)
}
