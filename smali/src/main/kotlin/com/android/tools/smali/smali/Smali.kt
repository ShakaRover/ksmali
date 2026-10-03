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

@file:JvmName("Smali")

package com.android.tools.smali.smali

import com.google.common.collect.Lists
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.util.StringUtils
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Assemble the specified files, using the given options
 *
 * @param options a SmaliOptions object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun assemble(options: SmaliOptions, vararg input: String): Boolean =
    assemble(options, Arrays.asList(*input))

/**
 * Assemble the specified files, using the given options
 *
 * @param options a SmaliOptions object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun assemble(options: SmaliOptions, input: List<String>): Boolean {
    val filesToProcessSet = TreeSet<File>()

    for (fileToProcess in input) {
        val argFile = File(fileToProcess)

        if (!argFile.exists()) {
            throw IllegalArgumentException("Cannot find file or directory \"$fileToProcess\"")
        }

        if (argFile.isDirectory) {
            getSmaliFilesInDir(argFile, filesToProcessSet)
        } else if (argFile.isFile) {
            filesToProcessSet.add(argFile)
        }
    }

    var errors = false

    val dexBuilder = DexBuilder(Opcodes.forApi(options.apiLevel))

    val executor = Executors.newFixedThreadPool(options.jobs)
    val tasks = Lists.newArrayList<Future<Boolean>>()

    for (file in filesToProcessSet) {
        tasks.add(executor.submit(Callable<Boolean> { assembleSmaliFile(file, dexBuilder, options) }))
    }

    for (task in tasks) {
        while (true) {
            try {
                try {
                    if (!task.get()) {
                        errors = true
                    }
                } catch (ex: ExecutionException) {
                    throw RuntimeException(ex)
                }
            } catch (ex: InterruptedException) {
                continue
            }
            break
        }
    }

    executor.shutdown()

    if (errors) {
        return false
    }

    dexBuilder.writeTo(FileDataStore(File(options.outputDexFile)))

    return true
}

/**
 * Prints the lexical tokens for the given files.
 *
 * @param options a SmaliOptions object with the options to use
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun printTokens(options: SmaliOptions, input: List<String>): Boolean {
    val filesToProcessSet = TreeSet<File>()

    for (fileToProcess in input) {
        val argFile = File(fileToProcess)

        if (!argFile.exists()) {
            throw IllegalArgumentException("Cannot find file or directory \"$fileToProcess\"")
        }

        if (argFile.isDirectory) {
            getSmaliFilesInDir(argFile, filesToProcessSet)
        } else if (argFile.isFile) {
            filesToProcessSet.add(argFile)
        }
    }

    var errors = false

    for (file in filesToProcessSet) {
        try {
            if (!printTokensForSingleFile(file, options)) {
                errors = true
            }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    if (errors) {
        return false
    }

    return true
}

private fun getSmaliFilesInDir(dir: File, smaliFiles: MutableSet<File>) {
    val files = dir.listFiles()
    if (files != null) {
        for (file in files) {
            if (file.isDirectory) {
                getSmaliFilesInDir(file, smaliFiles)
            } else if (file.name.endsWith(".smali")) {
                smaliFiles.add(file)
            }
        }
    }
}

private fun assembleSmaliFile(smaliFile: File, dexBuilder: DexBuilder, options: SmaliOptions): Boolean {
    return FileInputStream(smaliFile).use { fis ->
        val reader = InputStreamReader(fis, StandardCharsets.UTF_8)

        val lexer = smaliLexer(CharStreams.fromReader(reader))
        lexer.setApiLevel(options.apiLevel)
        lexer.setSourceFile(smaliFile)
        val tokens = CommonTokenStream(lexer)

        if (options.printTokens) {
            tokens.getTokens()

            for (i in 0 until tokens.size()) {
                val token = tokens.get(i)
                if (token.channel == Token.HIDDEN_CHANNEL) {
                    continue
                }

                val tokenName = if (token.type == Token.EOF) {
                    "EOF"
                } else {
                    smaliParser.tokenName(token.type)
                }
                System.out.println("$tokenName: ${token.text}")
            }

            System.out.flush()
        }

        val parser = smaliParser(tokens)
        parser.setBuildParseTree(false)
        parser.setVerboseErrors(options.verboseErrors)
        parser.setAllowOdex(options.allowOdexOpcodes)
        parser.setApiLevel(options.apiLevel)

        val result = parser.smali_file()

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
            return@use false
        }

        val t = result.n

        if (options.printTokens) {
            System.out.println(t.toStringTree())
        }

        val treeStream = ListTokenStream(t.flatten())

        val dexGen = smaliTreeWalker(treeStream)
        dexGen.setBuildParseTree(false)
        dexGen.setErrorHandler(NoSyncErrorStrategy())
        dexGen.setApiLevel(options.apiLevel)
        dexGen.setVerboseErrors(options.verboseErrors)
        dexGen.setDexBuilder(dexBuilder)
        try {
            dexGen.smali_file()
        } catch (ex: RuntimeException) {
            if (options.verboseErrors) {
                ex.printStackTrace(System.err)
            }
            return@use false
        }

        dexGen.getNumberOfSyntaxErrors() == 0
    }
}

private fun printTokensForSingleFile(smaliFile: File, options: SmaliOptions): Boolean {
    return FileInputStream(smaliFile).use { fis ->
        val reader = InputStreamReader(fis, StandardCharsets.UTF_8)

        val lexer = smaliLexer(CharStreams.fromReader(reader))
        lexer.setApiLevel(options.apiLevel)
        lexer.setSourceFile(smaliFile)
        val tokens = CommonTokenStream(lexer)
        tokens.fill()

        for (i in 0 until tokens.size()) {
            val token = tokens.get(i)
            if (token.channel == Token.HIDDEN_CHANNEL) {
                continue
            }

            val tokenName = if (token.type == Token.EOF) {
                "EOF"
            } else {
                smaliParser.tokenName(token.type)
            }
            System.out.println("$tokenName(\"${StringUtils.escapeString(token.text)}\")")
        }
        System.out.flush()

        lexer.getNumberOfSyntaxErrors() == 0
    }
}
