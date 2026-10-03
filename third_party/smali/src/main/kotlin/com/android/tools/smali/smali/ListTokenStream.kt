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

import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.misc.Interval

/**
 * A [TokenStream] over a token list that is already fully materialized. The tree walker consumes
 * the flattened AST, so its tokens are known upfront; unlike `CommonTokenStream` this does not copy
 * them into a second buffer (and there is no lazy fetch during parsing).
 */
class ListTokenStream(
    private val tokens: List<Token>,
    private val tokenSource: TokenSource?
) : TokenStream {
    private var index = 0

    constructor(tokens: List<Token>) : this(tokens, null)

    override fun LT(k: Int): Token? {
        if (k == 0) {
            return null
        }
        val i = if (k > 0) index + k - 1 else index + k
        if (i < 0) {
            return null
        }
        if (i >= tokens.size) {
            return if (tokens.isNotEmpty()) tokens[tokens.size - 1] else null
        }
        return tokens[i]
    }

    override fun LA(i: Int): Int {
        val token = LT(i)
        return token?.type ?: Token.INVALID_TYPE
    }

    override fun get(i: Int): Token = tokens[i]

    override fun size(): Int = tokens.size

    override fun index(): Int = index

    override fun consume() {
        if (index < tokens.size - 1) {
            index++
        }
    }

    override fun mark(): Int = -1

    override fun release(marker: Int) {
    }

    override fun seek(newIndex: Int) {
        if (newIndex < 0) {
            index = 0
        } else if (newIndex >= tokens.size) {
            index = tokens.size - 1
        } else {
            index = newIndex
        }
    }

    override fun getTokenSource(): TokenSource? = tokenSource

    override fun getSourceName(): String =
        tokenSource?.sourceName ?: IntStream.UNKNOWN_SOURCE_NAME

    override fun getText(): String = getText(Interval.of(0, tokens.size - 1))

    override fun getText(interval: Interval): String {
        val start = interval.a
        var stop = interval.b
        if (start < 0 || stop < 0) {
            return ""
        }
        if (stop >= tokens.size) {
            stop = tokens.size - 1
        }
        val buf = StringBuilder()
        for (i in start..stop) {
            val token = tokens[i]
            if (token.type == Token.EOF) {
                break
            }
            buf.append(token.text)
        }
        return buf.toString()
    }

    override fun getText(ctx: RuleContext): String = getText(ctx.sourceInterval)

    override fun getText(start: Token?, stop: Token?): String {
        if (start != null && stop != null) {
            return getText(Interval.of(start.tokenIndex, stop.tokenIndex))
        }
        return ""
    }
}
