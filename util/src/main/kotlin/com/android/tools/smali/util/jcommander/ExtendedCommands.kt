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

@file:JvmName("ExtendedCommands")

package com.android.tools.smali.util.jcommander

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterDescription
import com.beust.jcommander.Parameters
import java.lang.reflect.Field

/**
 * Utilities related to "extended" commands - JCommander commands with additional information
 */
private fun getExtendedParameters(command: Any): ExtendedParameters {
    val anno = command.javaClass.getAnnotation(ExtendedParameters::class.java)
    if (anno == null) {
        throw IllegalStateException(
            "All extended commands should have an ExtendedParameters annotation: " +
                command.javaClass.canonicalName
        )
    }
    return anno
}

fun commandName(jc: JCommander): String = getExtendedParameters(jc.objects[0]).commandName

fun commandName(command: Any): String = getExtendedParameters(command).commandName

fun commandAliases(jc: JCommander): Array<String> = commandAliases(jc.objects[0])

fun commandAliases(command: Any): Array<String> = getExtendedParameters(command).commandAliases

fun includeParametersInUsage(jc: JCommander): Boolean =
    includeParametersInUsage(jc.objects[0])

fun includeParametersInUsage(command: Any): Boolean =
    getExtendedParameters(command).includeParametersInUsage

fun postfixDescription(jc: JCommander): String = postfixDescription(jc.objects[0])

fun postfixDescription(command: Any): String =
    getExtendedParameters(command).postfixDescription

fun addExtendedCommand(jc: JCommander, command: Command) {
    jc.addCommand(commandName(command), command, *commandAliases(command))
    command.setupCommandInternal(command.getJCommanderInternal())
}

fun parameterArgumentNames(parameterDescription: ParameterDescription): Array<String> {
    val parameterized = parameterDescription.parameterized

    var cls: Class<*> = parameterDescription.`object`.javaClass
    var field: Field? = null
    while (cls != Any::class.java) {
        try {
            field = cls.getDeclaredField(parameterized.name)
        } catch (ex: NoSuchFieldException) {
            cls = cls.superclass
            continue
        }
        break
    }

    assert(field != null)
    val extendedParameter = field!!.getAnnotation(ExtendedParameter::class.java)
    if (extendedParameter != null) {
        return extendedParameter.argumentNames
    }

    return emptyArray()
}

fun getSubcommand(jc: JCommander, commandName: String): JCommander? {
    return if (jc.commands.containsKey(commandName)) {
        jc.commands[commandName]
    } else {
        for (command in jc.commands.values) {
            for (alias in commandAliases(command)) {
                if (commandName == alias) {
                    return command
                }
            }
        }
        null
    }
}

fun getCommandDescription(jc: JCommander): String? {
    val parameters = jc.objects[0].javaClass.getAnnotation(Parameters::class.java)
    if (parameters == null) {
        return null
    }
    return parameters.commandDescription
}
