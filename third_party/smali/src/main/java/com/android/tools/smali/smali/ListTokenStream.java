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

package com.android.tools.smali.smali;

import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;

import java.util.List;

/**
 * A {@link TokenStream} over a token list that is already fully materialized. The tree walker
 * consumes the flattened AST, so its tokens are known upfront; unlike {@code CommonTokenStream}
 * this does not copy them into a second buffer (and there is no lazy fetch during parsing).
 */
public final class ListTokenStream implements TokenStream {
    private final List<Token> tokens;
    private final TokenSource tokenSource;
    private int index;

    public ListTokenStream(List<Token> tokens) {
        this(tokens, null);
    }

    public ListTokenStream(List<Token> tokens, TokenSource tokenSource) {
        this.tokens = tokens;
        this.tokenSource = tokenSource;
    }

    @Override
    public Token LT(int k) {
        if (k == 0) {
            return null;
        }
        int i = k > 0 ? index + k - 1 : index + k;
        if (i < 0) {
            return null;
        }
        if (i >= tokens.size()) {
            return tokens.size() > 0 ? tokens.get(tokens.size() - 1) : null;
        }
        return tokens.get(i);
    }

    @Override
    public int LA(int i) {
        Token token = LT(i);
        return token == null ? Token.INVALID_TYPE : token.getType();
    }

    @Override
    public Token get(int i) {
        return tokens.get(i);
    }

    @Override
    public int size() {
        return tokens.size();
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public void consume() {
        if (index < tokens.size() - 1) {
            index++;
        }
    }

    @Override
    public int mark() {
        return -1;
    }

    @Override
    public void release(int marker) {
    }

    @Override
    public void seek(int newIndex) {
        if (newIndex < 0) {
            index = 0;
        } else if (newIndex >= tokens.size()) {
            index = tokens.size() - 1;
        } else {
            index = newIndex;
        }
    }

    @Override
    public TokenSource getTokenSource() {
        return tokenSource;
    }

    @Override
    public String getSourceName() {
        return tokenSource != null ? tokenSource.getSourceName() : IntStream.UNKNOWN_SOURCE_NAME;
    }

    @Override
    public String getText() {
        return getText(Interval.of(0, tokens.size() - 1));
    }

    @Override
    public String getText(Interval interval) {
        int start = interval.a;
        int stop = interval.b;
        if (start < 0 || stop < 0) {
            return "";
        }
        if (stop >= tokens.size()) {
            stop = tokens.size() - 1;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = start; i <= stop; i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                break;
            }
            buf.append(token.getText());
        }
        return buf.toString();
    }

    @Override
    public String getText(RuleContext ctx) {
        return getText(ctx.getSourceInterval());
    }

    @Override
    public String getText(Token start, Token stop) {
        if (start != null && stop != null) {
            return getText(Interval.of(start.getTokenIndex(), stop.getTokenIndex()));
        }
        return "";
    }
}
