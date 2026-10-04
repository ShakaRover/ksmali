/*
 * Copyright 2024, Google LLC
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
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.junit.Assert

/** A single diagnostic reported by the smali front-end, with the position ANTLR attached to it. */
data class SmaliFrontEndError(val line: Int, val column: Int, val message: String)

/** Result of running a source string through the same pipeline as [assemble]. */
data class SmaliFrontEndResult(val thrown: Throwable?, val errors: List<SmaliFrontEndError>) {
    val rejected: Boolean get() = thrown != null || errors.isNotEmpty()
}

/**
 * Runs [source] through the same lexer/parser/tree-walker pipeline [assemble] uses, collecting every
 * diagnostic instead of printing it. Attaching an explicit listener is the only way to observe the
 * line/column of a diagnostic: [compileSmali] collapses syntax errors to a generic
 * `RuntimeException` and the tree-walker error handler otherwise prints to stderr.
 *
 * `thrown` is non-null when an exception escapes the pipeline (e.g. a range check in the dex builder),
 * mirroring what [compileSmali] would propagate.
 */
@Synchronized
fun runSmaliFrontEnd(source: String, apiLevel: Int = 15): SmaliFrontEndResult {
    // The lexer reports invalid directives itself (see smaliLexer.g4) instead of through an ANTLR
    // listener, so capture stderr to recover their line/column as well.
    val errBuffer = java.io.ByteArrayOutputStream()
    val savedErr = System.err
    System.setErr(java.io.PrintStream(errBuffer, true, "UTF-8"))
    val result = try {
        runSmaliFrontEndPipeline(source, apiLevel)
    } finally {
        System.setErr(savedErr)
    }
    val lexerErrors = parseLexerErrors(errBuffer.toString("UTF-8"))
    return if (lexerErrors.isEmpty()) {
        result
    } else {
        SmaliFrontEndResult(result.thrown, result.errors + lexerErrors)
    }
}

private val LEXER_ERROR = Regex("""\[(\d+),(\d+)] Error for input '[^']*': (.*)""")

private fun parseLexerErrors(stderr: String): List<SmaliFrontEndError> =
    stderr.lineSequence().mapNotNull { line ->
        val match = LEXER_ERROR.find(line) ?: return@mapNotNull null
        SmaliFrontEndError(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3])
    }.toList()

private fun runSmaliFrontEndPipeline(source: String, apiLevel: Int): SmaliFrontEndResult {
    val errors = mutableListOf<SmaliFrontEndError>()
    val listener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            errors.add(SmaliFrontEndError(line, charPositionInLine, msg ?: ""))
        }
    }

    val lexer = smaliLexer(CharStreams.fromString(source)).apply {
        setApiLevel(apiLevel)
        removeErrorListeners()
        addErrorListener(listener)
    }
    val parser = smaliParser(CommonTokenStream(lexer)).apply {
        setBuildParseTree(false)
        setVerboseErrors(true)
        setAllowOdex(false)
        setApiLevel(apiLevel)
        setDexBuilder(DexBuilder(Opcodes.forApi(apiLevel)))
        removeErrorListeners()
        addErrorListener(listener)
    }

    var thrown: Throwable? = null
    try {
        parser.smali_file()
    } catch (ex: Throwable) {
        thrown = ex
    }
    if (thrown == null && (parser.numberOfSyntaxErrors > 0 || lexer.numberOfSyntaxErrors > 0)) {
        thrown = RuntimeException("syntax errors")
    }
    return SmaliFrontEndResult(thrown, errors)
}

/** Asserts that [source] is accepted by the front-end. */
fun assertSmaliAssembles(source: String, apiLevel: Int = 15) {
    val result = runSmaliFrontEnd(source, apiLevel)
    Assert.assertFalse(
        "Expected source to assemble, but got thrown=${
            result.thrown?.let { it.javaClass.simpleName + ": " + it.message }
        } errors=${result.errors}",
        result.rejected
    )
}

/**
 * Asserts that [source] is rejected and that a diagnostic on [line] contains [messagePart]. Both
 * values are intentionally just "reported", not exact, so this stays robust against ANTLR wording.
 */
fun assertSmaliRejected(source: String, line: Int, messagePart: String, apiLevel: Int = 15) {
    val result = runSmaliFrontEnd(source, apiLevel)
    Assert.assertTrue(
        "Expected source to be rejected, but it assembled cleanly",
        result.rejected
    )
    val match = result.errors.firstOrNull { it.line == line && it.message.contains(messagePart) }
    if (match == null) {
        val thrown = result.thrown
        Assert.fail(
            "No diagnostic on line $line containing '$messagePart'.\n" +
                "thrown=${thrown?.javaClass?.name}: ${thrown?.message}\n" +
                "errors=${result.errors}"
        )
    }
    Assert.assertTrue("Column must be reported", requireNotNull(match).column >= 0)
}

/** Asserts that the front-end throws [expectedType] whose message contains [messagePart]. */
fun assertSmaliThrows(
    source: String,
    expectedType: Class<out Throwable>,
    messagePart: String,
    apiLevel: Int = 15
) {
    val result = runSmaliFrontEnd(source, apiLevel)
    val thrown = result.thrown
    Assert.assertNotNull("Expected ${expectedType.simpleName}, but nothing was thrown. errors=${result.errors}", thrown)
    Assert.assertTrue(
        "Expected ${expectedType.simpleName} but got ${thrown!!.javaClass.name}: ${thrown.message}",
        expectedType.isInstance(thrown)
    )
    Assert.assertTrue(
        "Expected message to contain '$messagePart' but was '${thrown.message}'",
        thrown.message.orEmpty().contains(messagePart)
    )
}
