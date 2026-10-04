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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.util.StringUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.TreeSet

/**
 * Assemble the specified files, using the given options.
 *
 * This is the blocking entry point. It is a thin wrapper around [assembleSuspend] via
 * [runBlocking], kept for Java callers and for existing Kotlin callers that are not already in a
 * coroutine (the command line tool uses it). A caller that is already inside a coroutine should
 * call [assembleSuspend] directly instead of nesting another event loop.
 *
 * @param options a [SmaliOptions] object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun assemble(options: SmaliOptions, vararg input: String): Boolean =
    assemble(options, input.toList())

/**
 * Assemble the specified files, using the given options.
 *
 * Blocking variant; see [assemble] and [assembleSuspend].
 *
 * @param options a [SmaliOptions] object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun assemble(options: SmaliOptions, input: List<String>): Boolean =
    runBlocking { assembleSuspend(options, input) }

/**
 * Assemble the specified files, using the given options, without blocking the calling thread.
 *
 * This is the coroutine-friendly entry point: it may suspend, so a caller that is already inside
 * a coroutine can drive assembly directly rather than nesting [runBlocking]. The parallel work
 * still runs on [Dispatchers.Default] limited to [SmaliOptions.jobs], exactly like the blocking
 * [assemble] variant, so the two produce byte-identical output.
 *
 * @param options a [SmaliOptions] object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
suspend fun assembleSuspend(options: SmaliOptions, vararg input: String): Boolean =
    assembleSuspend(options, input.toList())

/**
 * Assemble the specified files, using the given options, without blocking the calling thread.
 *
 * Suspend variant of [assemble]; the [options] and [input] parameters have the same semantics as
 * there, and the dex written to [SmaliOptions.outputDexFile] is identical.
 *
 * @param options a [SmaliOptions] object with the options to run smali with
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
suspend fun assembleSuspend(options: SmaliOptions, input: List<String>): Boolean {
    val filesToProcess = collectSmaliFiles(input)
    val dexBuilder = DexBuilder(Opcodes.forApi(options.apiLevel))

    // Assemble every file on a dispatcher that honours the requested job count. supervisorScope +
    // runCatching makes sure one failing file does not cancel the others (same behaviour as the
    // old thread-pool implementation, which waited for every task).
    val results = supervisorScope {
        filesToProcess
            .map { file ->
                async(assemblyDispatcher(options.jobs)) {
                    runCatching { assembleSmaliFile(file, dexBuilder, options) }
                }
            }
            .awaitAll()
    }

    results.firstOrNull { it.isFailure }?.let { throw RuntimeException(it.exceptionOrNull()) }

    if (results.any { !it.getOrDefault(false) }) {
        return false
    }

    dexBuilder.writeTo(FileDataStore(File(options.outputDexFile)))

    return true
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun assemblyDispatcher(jobs: Int): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(jobs.coerceAtLeast(1))

private fun collectSmaliFiles(input: List<String>): List<File> {
    val filesToProcess = TreeSet<File>()

    for (path in input) {
        val argFile = File(path)
        require(argFile.exists()) { "Cannot find file or directory \"$path\"" }
        when {
            argFile.isDirectory -> getSmaliFilesInDir(argFile, filesToProcess)
            argFile.isFile -> filesToProcess.add(argFile)
        }
    }

    return filesToProcess.toList()
}

/**
 * Prints the lexical tokens for the given files.
 *
 * This dumps the lexer tokens, one line per visible token. Since the smali parser and tree walker
 * were merged into a single pass there is no AST node stream to print any more, so unlike the old
 * two-pass front end this no longer dumps an AST `toStringTree()`.
 *
 * @param options a [SmaliOptions] object with the options to use
 * @param input The files/directories to process
 * @return true if assembly completed with no errors, or false if errors were encountered
 */
@Throws(IOException::class)
fun printTokens(options: SmaliOptions, input: List<String>): Boolean {
    val errors = collectSmaliFiles(input).map { file ->
        try {
            printTokensForSingleFile(file, options)
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }.any { !it }

    return !errors
}

private fun getSmaliFilesInDir(dir: File, smaliFiles: MutableSet<File>) {
    dir.listFiles()?.forEach { file ->
        when {
            file.isDirectory -> getSmaliFilesInDir(file, smaliFiles)
            file.name.endsWith(".smali") -> smaliFiles.add(file)
        }
    }
}

private fun assembleSmaliFile(smaliFile: File, dexBuilder: DexBuilder, options: SmaliOptions): Boolean =
    FileInputStream(smaliFile).use { fis ->
        val lexer = smaliLexer(CharStreams.fromReader(InputStreamReader(fis, StandardCharsets.UTF_8))).apply {
            setApiLevel(options.apiLevel)
            setSourceFile(smaliFile)
        }
        val tokenStream = CommonTokenStream(lexer)

        if (options.printTokens) {
            tokenStream.tokens.forEach { token ->
                if (token.channel != Token.HIDDEN_CHANNEL) {
                    val name = if (token.type == Token.EOF) "EOF" else smaliParser.tokenName(token.type)
                    println("$name: ${token.text}")
                }
            }
            System.out.flush()
        }

        val parser = smaliParser(tokenStream).apply {
            setBuildParseTree(false)
            setVerboseErrors(options.verboseErrors)
            setAllowOdex(options.allowOdexOpcodes)
            setApiLevel(options.apiLevel)
            setDexBuilder(dexBuilder)
        }

        try {
            parser.smali_file()
        } catch (ex: RuntimeException) {
            if (options.verboseErrors) {
                ex.printStackTrace(System.err)
            }
            return@use false
        }

        parser.getNumberOfSyntaxErrors() == 0 && lexer.getNumberOfSyntaxErrors() == 0
    }

private fun printTokensForSingleFile(smaliFile: File, options: SmaliOptions): Boolean =
    FileInputStream(smaliFile).use { fis ->
        val lexer = smaliLexer(CharStreams.fromReader(InputStreamReader(fis, StandardCharsets.UTF_8))).apply {
            setApiLevel(options.apiLevel)
            setSourceFile(smaliFile)
        }
        val tokenStream = CommonTokenStream(lexer)
        tokenStream.fill()

        tokenStream.tokens.forEach { token ->
            if (token.channel != Token.HIDDEN_CHANNEL) {
                val name = if (token.type == Token.EOF) "EOF" else smaliParser.tokenName(token.type)
                println("$name(\"${StringUtils.escapeString(token.text)}\")")
            }
        }
        System.out.flush()

        lexer.getNumberOfSyntaxErrors() == 0
    }
