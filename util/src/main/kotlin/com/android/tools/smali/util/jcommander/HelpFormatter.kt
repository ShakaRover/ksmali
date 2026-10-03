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

package com.android.tools.smali.util.jcommander

import com.android.tools.smali.util.WrappedIndentingWriter
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterDescription
import com.beust.jcommander.Parameters
import com.google.common.base.Joiner
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import java.io.IOException
import java.io.StringWriter
import java.util.Collections
import java.util.regex.Pattern

class HelpFormatter {

    private var width = 80

    fun width(width: Int): HelpFormatter {
        this.width = width
        return this
    }

    private fun getParameterArity(param: ParameterDescription): Int {
        if (param.parameter.arity() > 0) {
            return param.parameter.arity()
        }
        val type = param.parameterized.type
        if (type == Boolean::class.javaPrimitiveType || type == Boolean::class.javaObjectType) {
            return 0
        }
        return 1
    }

    private fun getSortedParameters(jc: JCommander): List<ParameterDescription> {
        val parameters: MutableList<ParameterDescription> = Lists.newArrayList(jc.parameters)

        val pattern = Pattern.compile("^-*(.*)$")

        Collections.sort(parameters, Comparator<ParameterDescription> { o1, o2 ->
            var matcher = pattern.matcher(o1.parameter.names()[0])
            val s1: String
            if (matcher.matches()) {
                s1 = matcher.group(1)
            } else {
                throw IllegalStateException()
            }

            matcher = pattern.matcher(o2.parameter.names()[0])
            val s2: String
            if (matcher.matches()) {
                s2 = matcher.group(1)
            } else {
                throw IllegalStateException()
            }

            s1.compareTo(s2)
        })
        return parameters
    }

    fun format(vararg jc: JCommander): String {
        return format(jc.toList())
    }

    fun format(commandHierarchy: List<JCommander>): String {
        try {
            val stringWriter = StringWriter()
            val writer = WrappedIndentingWriter(stringWriter, width - 5, width)

            val leafJc = Iterables.getLast(commandHierarchy)

            writer.write("usage:")
            writer.indent(2)

            for (jc in commandHierarchy) {
                writer.write(" ")
                writer.write(commandName(jc))
            }

            if (includeParametersInUsage(leafJc)) {
                for (param in leafJc.parameters) {
                    if (!param.parameter.hidden()) {
                        writer.write(" [")
                        writer.write(param.parameter.parameter.names[0])
                        writer.write("]")
                    }
                }
            } else {
                if (leafJc.parameters.isNotEmpty()) {
                    writer.write(" [<options>]")
                }
            }

            if (leafJc.commands.isNotEmpty()) {
                writer.write(" [<command [<args>]]")
            }

            if (leafJc.mainParameter != null) {
                val argumentNames = parameterArgumentNames(leafJc.mainParameterValue)
                if (argumentNames.isEmpty()) {
                    writer.write(" <args>")
                } else {
                    val argumentName = argumentNames[0]
                    val writeAngleBrackets = !argumentName.startsWith("<") &&
                        !argumentName.startsWith("[")
                    writer.write(" ")
                    if (writeAngleBrackets) {
                        writer.write("<")
                    }
                    writer.write(argumentNames[0])
                    if (writeAngleBrackets) {
                        writer.write(">")
                    }
                }
            }

            writer.deindent(2)

            val commandDescription = getCommandDescription(leafJc)
            if (commandDescription != null) {
                writer.write("\n")
                writer.write(commandDescription)
            }

            if (leafJc.parameters.isNotEmpty() || leafJc.mainParameter != null) {
                writer.write("\n\nOptions:")
                writer.indent(2)
                for (param in getSortedParameters(leafJc)) {
                    if (!param.parameter.hidden()) {
                        writer.write("\n")
                        writer.indent(4)
                        if (param.names.isNotEmpty()) {
                            writer.write(Joiner.on(',').join(param.parameter.names().toList()))
                        }
                        if (getParameterArity(param) > 0) {
                            val argumentNames = parameterArgumentNames(param)
                            for (i in 0 until getParameterArity(param)) {
                                writer.write(" ")
                                if (i < argumentNames.size) {
                                    writer.write("<")
                                    writer.write(argumentNames[i])
                                    writer.write(">")
                                } else {
                                    writer.write("<arg>")
                                }
                            }
                        }
                        if (!param.description.isNullOrEmpty()) {
                            writer.write(" - ")
                            writer.write(param.description)
                        }
                        val defaultValue0 = param.default
                        if (defaultValue0 != null) {
                            var defaultValue: String? = null
                            val type = param.parameterized.type
                            if (type == Boolean::class.javaObjectType ||
                                type == Boolean::class.javaPrimitiveType) {
                                if (defaultValue0 as Boolean) {
                                    defaultValue = "True"
                                }
                            } else if (List::class.java.isAssignableFrom(type)) {
                                if ((defaultValue0 as List<*>).isNotEmpty()) {
                                    defaultValue = defaultValue0.toString()
                                }
                            } else {
                                defaultValue = defaultValue0.toString()
                            }
                            if (defaultValue != null) {
                                writer.write(" (default: ")
                                writer.write(defaultValue)
                                writer.write(")")
                            }
                        }
                        writer.deindent(4)
                    }
                }

                if (leafJc.mainParameter != null) {
                    val argumentNames = parameterArgumentNames(leafJc.mainParameterValue)
                    writer.write("\n")
                    writer.indent(4)
                    if (argumentNames.isNotEmpty()) {
                        writer.write("<")
                        writer.write(argumentNames[0])
                        writer.write(">")
                    } else {
                        writer.write("<args>")
                    }

                    if (leafJc.mainParameterDescription != null) {
                        writer.write(" - ")
                        writer.write(leafJc.mainParameterDescription)
                    }
                    writer.deindent(4)
                }
                writer.deindent(2)
            }

            if (leafJc.commands.isNotEmpty()) {
                writer.write("\n\nCommands:")
                writer.indent(2)

                val entryList: MutableList<Map.Entry<String, JCommander>> =
                    Lists.newArrayList(leafJc.commands.entries)
                Collections.sort(entryList, Comparator<Map.Entry<String, JCommander>> { o1, o2 ->
                    o1.key.compareTo(o2.key)
                })

                for (entry in entryList) {
                    val commandName = entry.key
                    val command = entry.value

                    val arg = command.objects[0]
                    val parametersAnno = arg.javaClass.getAnnotation(Parameters::class.java)
                    if (!parametersAnno.hidden) {
                        writer.write("\n")
                        writer.indent(4)
                        writer.write(commandName)
                        val aliases = getCommandAliases(command)
                        if (aliases.isNotEmpty()) {
                            writer.write("(")
                            writer.write(Joiner.on(',').join(aliases))
                            writer.write(")")
                        }

                        val commandDesc = leafJc.usageFormatter.getCommandDescription(commandName)
                        if (commandDesc != null) {
                            writer.write(" - ")
                            writer.write(commandDesc)
                        }
                        writer.deindent(4)
                    }
                }
                writer.deindent(2)
            }

            val postfixDescription = getPostfixDescription(leafJc)
            if (postfixDescription.isNotEmpty()) {
                writer.write("\n\n")
                writer.write(postfixDescription)
            }

            writer.flush()

            return stringWriter.buffer.toString()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    companion object {

        private fun getExtendedParameters(jc: JCommander): ExtendedParameters {
            val anno = jc.objects[0].javaClass.getAnnotation(ExtendedParameters::class.java)
            if (anno == null) {
                throw IllegalStateException("All commands should have an ExtendedParameters annotation")
            }
            return anno
        }

        private fun getCommandAliases(jc: JCommander): List<String> =
            getExtendedParameters(jc).commandAliases.toList()

        private fun includeParametersInUsage(jc: JCommander): Boolean =
            getExtendedParameters(jc).includeParametersInUsage

        private fun getPostfixDescription(jc: JCommander): String =
            getExtendedParameters(jc).postfixDescription
    }
}
