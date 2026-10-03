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

import com.android.tools.smali.smali.expectedTokensTestGrammarParser.ExpectedToken
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException

class LexerTest {
    @Test
    fun DirectiveTest() {
        runTest("DirectiveTest")
    }

    @Test
    fun ByteLiteralTest() {
        runTest("ByteLiteralTest")
    }

    @Test
    fun ShortLiteralTest() {
        runTest("ShortLiteralTest")
    }

    @Test
    fun IntegerLiteralTest() {
        runTest("IntegerLiteralTest")
    }

    @Test
    fun LongLiteralTest() {
        runTest("LongLiteralTest")
    }

    @Test
    fun FloatLiteralTest() {
        runTest("FloatLiteralTest")
    }

    @Test
    fun CharLiteralTest() {
        runTest("CharLiteralTest")
    }

    @Test
    fun StringLiteralTest() {
        runTest("StringLiteralTest")
    }

    @Test
    fun MiscTest() {
        runTest("MiscTest")
    }

    @Test
    fun CommentTest() {
        runTest("CommentTest", false)
    }

    @Test
    fun InstructionTest() {
        runTest("InstructionTest", true)
    }

    @Test
    fun TypeAndIdentifierTest() {
        runTest("TypeAndIdentifierTest")
    }

    @Test
    fun TypeAndIdentifierTest_api29() {
        runTest("TypeAndIdentifierTest_api29", 29)
    }

    @Test
    fun SymbolTest() {
        runTest("SymbolTest", false)
    }

    @Test
    fun RealSmaliFileTest() {
        runTest("RealSmaliFileTest", true)
    }

    fun runTest(test: String) {
        runTest(test, true, MOST_RECENT_API)
    }

    fun runTest(test: String, discardHiddenTokens: Boolean) {
        runTest(test, discardHiddenTokens, MOST_RECENT_API)
    }

    fun runTest(test: String, apiLevel: Int) {
        runTest(test, true, apiLevel)
    }

    fun runTest(test: String, discardHiddenTokens: Boolean, apiLevel: Int) {
        val smaliFile = "LexerTest${File.separatorChar}$test.smali"
        val tokensFile = "LexerTest${File.separatorChar}$test.tokens"

        val expectedTokensLexer: expectedTokensTestGrammarLexer
        try {
            expectedTokensLexer = expectedTokensTestGrammarLexer(
                CharStreams.fromStream(
                    LexerTest::class.java.classLoader.getResourceAsStream(tokensFile)!!
                )
            )
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

        val expectedTokensStream = CommonTokenStream(expectedTokensLexer)

        val expectedTokensParser = expectedTokensTestGrammarParser(expectedTokensStream)
        try {
            expectedTokensParser.top()
        } catch (ex: RecognitionException) {
            throw RuntimeException(ex)
        }

        val expectedTokens: List<ExpectedToken> = expectedTokensParser.expectedTokenList

        val smaliStream = LexerTest::class.java.classLoader.getResourceAsStream(smaliFile)
        if (smaliStream == null) {
            Assert.fail("Could not load $smaliFile")
        }
        val lexer: smaliLexer
        try {
            lexer = smaliLexer(CharStreams.fromStream(requireNotNull(smaliStream)))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
        lexer.setApiLevel(apiLevel)
        lexer.setSourceFile(File(test + ".smali"))
        lexer.setSuppressErrors(true)

        val tokenStream = CommonTokenStream(lexer)
        tokenStream.fill()
        val tokens: List<Token> = tokenStream.tokens

        var expectedTokenIndex = 0
        for (i in 0 until tokens.size - 1) {
            val token = tokens[i] as CommonToken

            if (discardHiddenTokens && token.channel == Token.HIDDEN_CHANNEL) {
                continue
            }

            if (expectedTokenIndex >= expectedTokens.size) {
                Assert.fail("Too many tokens")
            }

            if (token.type == smaliParser.INVALID_TOKEN) {
                Assert.assertTrue(
                    "Encountered an INVALID_TOKEN not on the error channel",
                    token.channel == smaliParser.ERROR_CHANNEL
                )
            }

            val expectedToken = expectedTokens[expectedTokenIndex++]
            if (!tokenTypesByName.containsKey(expectedToken.tokenName)) {
                Assert.fail("Unknown token: " + expectedToken.tokenName)
            }
            val expectedTokenType = tokenTypesByName[expectedToken.tokenName]!!

            if (token.type != expectedTokenType) {
                Assert.fail(
                    "Invalid token at index %d. Expecting %s, got %s(%s)".format(
                        expectedTokenIndex - 1, expectedToken.tokenName, getTokenName(token.type), token.text
                    )
                )
            }

            if (expectedToken.tokenText != null) {
                if (expectedToken.tokenText != token.text) {
                    Assert.fail(
                        "Invalid token text at index %d. Expecting text \"%s\", got \"%s\"".format(
                            expectedTokenIndex - 1, expectedToken.tokenText, token.text
                        )
                    )
                }
            }
        }

        if (expectedTokenIndex < expectedTokens.size) {
            Assert.fail(
                "Not enough tokens. Expecting %d tokens, but got %d".format(
                    expectedTokens.size, expectedTokenIndex
                )
            )
        }
    }

    companion object {
        private val tokenTypesByName: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
            for (i in 0..smaliParser.VOCABULARY.maxTokenType) {
                this[smaliParser.tokenName(i)] = i
            }
        }

        private const val MOST_RECENT_API = 10000

        private fun getTokenName(tokenType: Int): String {
            return smaliParser.tokenName(tokenType)
        }
    }
}
