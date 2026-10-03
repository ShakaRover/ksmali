/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
@file:JvmName("ConsoleUtil")

package com.android.tools.smali.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Attempt to find the width of the console. If it can't get the width, return a default of 80
 * @return The current console width
 */
fun getConsoleWidth(): Int {
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
        try {
            return attemptMode()
        } catch (ex: Exception) {
        }
    } else {
        try {
            return attemptStty()
        } catch (ex: Exception) {
        }
    }

    return 80
}

private fun attemptStty(): Int {
    val output = attemptCommand(arrayOf("sh", "-c", "stty size < /dev/tty")) ?: return 80

    val vals = output.split(Regex(" "))
    if (vals.size < 2) {
        return 80
    }
    return vals[1].toInt()
}

private fun attemptMode(): Int {
    val output = attemptCommand(arrayOf("mode", "con")) ?: return 80

    val pattern = Pattern.compile("Columns:[ \t]*(\\d+)")
    val m = pattern.matcher(output)
    if (!m.find()) {
        return 80
    }

    return m.group(1).toInt()
}

private fun attemptCommand(command: Array<String>): String? {
    var buffer: StringBuffer? = null

    try {
        val p = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(p.inputStream))

        var line = reader.readLine()
        while (line != null) {
            if (buffer == null) {
                buffer = StringBuffer()
            }
            buffer!!.append(line)
            line = reader.readLine()
        }

        return buffer?.toString()
    } catch (ex: Exception) {
        return null
    }
}
