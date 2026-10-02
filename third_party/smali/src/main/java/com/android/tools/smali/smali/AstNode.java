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

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.WritableToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lightweight AST node. The smali front-end is split into two stages: the parser builds one of
 * these trees, and then the tree walker consumes a flattened representation of it (see
 * {@link #flatten()}), mirroring the way ANTLR3's {@code CommonTreeNodeStream} handled the old
 * ANTLR3 tree grammar.
 */
public final class AstNode {
    /** A transparent list of siblings. Rules that do not build an explicit node produce one of these. */
    public static final int FLAT = -1;

    public final int type;
    public final String text;
    public final Token token;
    private final List<AstNode> children = new ArrayList<AstNode>();

    private AstNode(int type, Token token, String text) {
        this.type = type;
        this.token = token;
        this.text = text;
    }

    public static AstNode flat(List<AstNode> children) {
        AstNode node = new AstNode(FLAT, null, null);
        node.addChildren(children);
        return node;
    }

    public static AstNode flat(AstNode... children) {
        AstNode node = new AstNode(FLAT, null, null);
        for (AstNode child : children) {
            node.addChild(child);
        }
        return node;
    }

    public static AstNode leaf(Token token) {
        // Decode the token text once and cache it on the token; the tree walker later reads the
        // same token via $TOKEN.text, which would otherwise decode it from the input stream again.
        String text = token.getText();
        if (token instanceof WritableToken) {
            ((WritableToken) token).setText(text);
        }
        return new AstNode(token.getType(), token, text);
    }

    public static AstNode retype(Token src, int type) {
        return retypedText(type, src, src.getText());
    }

    public static AstNode retypedText(int type, Token src, String text) {
        CommonToken token = new CommonToken(type, text);
        if (src != null) {
            token.setLine(src.getLine());
            token.setCharPositionInLine(src.getCharPositionInLine());
            token.setStartIndex(src.getStartIndex());
            token.setStopIndex(src.getStopIndex());
        }
        return new AstNode(type, token, text);
    }

    public static AstNode imaginary(int type, Token start) {
        return new AstNode(type, makeImaginaryToken(type, start), nameFor(type));
    }

    public static AstNode node(int type, Token start, AstNode... children) {
        AstNode node = imaginary(type, start);
        for (AstNode child : children) {
            node.addChild(child);
        }
        return node;
    }

    public static AstNode node(int type, Token start, List<AstNode> children) {
        AstNode node = imaginary(type, start);
        node.addChildren(children);
        return node;
    }

    public static AstNode node(int type, String text, Token start) {
        return new AstNode(type, makeImaginaryToken(type, start), text);
    }

    /** Returns the token used for an imaginary (synthetic) node. */
    static Token makeImaginaryToken(int type, Token start) {
        CommonToken token = new CommonToken(type, nameFor(type));
        if (start != null) {
            token.setLine(start.getLine());
            token.setCharPositionInLine(start.getCharPositionInLine());
            token.setStartIndex(start.getStartIndex());
            token.setStopIndex(start.getStopIndex());
        }
        return token;
    }

    private static String nameFor(int type) {
        String name = smaliParser.VOCABULARY.getSymbolicName(type);
        return name != null ? name : Integer.toString(type);
    }

    public void addChild(AstNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void addChildren(List<AstNode> newChildren) {
        for (AstNode child : newChildren) {
            addChild(child);
        }
    }

    public List<AstNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getChildCount() {
        return children.size();
    }

    /**
     * Flattens this tree into a token list, inserting {@code DOWN}/{@code UP} markers around the
     * children of every non-leaf node. This is the exact representation that ANTLR3's
     * {@code CommonTreeNodeStream} produced.
     */
    public List<Token> flatten() {
        List<Token> tokens = new ArrayList<Token>();
        flatten(this, tokens);
        CommonToken eof = new CommonToken(Token.EOF, "<EOF>");
        eof.setTokenIndex(tokens.size());
        tokens.add(eof);
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof WritableToken) {
                ((WritableToken) token).setTokenIndex(i);
            }
        }
        return tokens;
    }

    private static void flatten(AstNode node, List<Token> out) {
        if (node.type == FLAT) {
            for (AstNode child : node.children) {
                flatten(child, out);
            }
            return;
        }
        out.add(node.token);
        if (!node.children.isEmpty()) {
            out.add(new CommonToken(smaliParser.DOWN, "<DOWN>"));
            for (AstNode child : node.children) {
                flatten(child, out);
            }
            out.add(new CommonToken(smaliParser.UP, "<UP>"));
        }
    }

    public String toStringTree() {
        StringBuilder sb = new StringBuilder();
        toStringTree(sb);
        return sb.toString();
    }

    private void toStringTree(StringBuilder sb) {
        if (type == FLAT) {
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                children.get(i).toStringTree(sb);
            }
            return;
        }
        String display = text != null ? text : nameFor(type);
        if (children.isEmpty()) {
            sb.append(display);
            return;
        }
        sb.append('(').append(display);
        for (AstNode child : children) {
            sb.append(' ');
            child.toStringTree(sb);
        }
        sb.append(')');
    }
}
