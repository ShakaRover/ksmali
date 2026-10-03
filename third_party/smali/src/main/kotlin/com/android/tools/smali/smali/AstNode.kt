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

import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.WritableToken
import java.util.ArrayList
import java.util.Collections

/**
 * A lightweight AST node. The smali front-end is split into two stages: the parser builds one of
 * these trees, and then the tree walker consumes a flattened representation of it (see [flatten]),
 * mirroring the way ANTLR3's `CommonTreeNodeStream` handled the old ANTLR3 tree grammar.
 */
class AstNode private constructor(
    @JvmField val type: Int,
    @JvmField val token: Token?,
    @JvmField val text: String?
) {
    private val children = ArrayList<AstNode>()

    fun addChild(child: AstNode?) {
        if (child != null) {
            children.add(child)
        }
    }

    fun addChildren(newChildren: List<AstNode>) {
        for (child in newChildren) {
            addChild(child)
        }
    }

    fun getChildren(): List<AstNode> = Collections.unmodifiableList(children)

    fun getChildCount(): Int = children.size

    /**
     * Flattens this tree into a token list, inserting `DOWN`/`UP` markers around the children of
     * every non-leaf node. This is the exact representation that ANTLR3's `CommonTreeNodeStream`
     * produced.
     */
    fun flatten(): List<Token> {
        val tokens = ArrayList<Token>()
        flatten(this, tokens)
        val eof = CommonToken(Token.EOF, "<EOF>")
        eof.tokenIndex = tokens.size
        tokens.add(eof)
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token is WritableToken) {
                token.tokenIndex = i
            }
        }
        return tokens
    }

    fun toStringTree(): String {
        val sb = StringBuilder()
        toStringTree(sb)
        return sb.toString()
    }

    private fun toStringTree(sb: StringBuilder) {
        if (type == FLAT) {
            for (i in children.indices) {
                if (i > 0) {
                    sb.append(' ')
                }
                children[i].toStringTree(sb)
            }
            return
        }
        val display = text ?: nameFor(type)
        if (children.isEmpty()) {
            sb.append(display)
            return
        }
        sb.append('(').append(display)
        for (child in children) {
            sb.append(' ')
            child.toStringTree(sb)
        }
        sb.append(')')
    }

    companion object {
        /** A transparent list of siblings. Rules that do not build an explicit node produce one of these. */
        const val FLAT = -1

        @JvmStatic
        fun flat(children: List<AstNode>): AstNode {
            val node = AstNode(FLAT, null, null)
            node.addChildren(children)
            return node
        }

        @JvmStatic
        fun flat(vararg children: AstNode): AstNode {
            val node = AstNode(FLAT, null, null)
            for (child in children) {
                node.addChild(child)
            }
            return node
        }

        @JvmStatic
        fun leaf(token: Token): AstNode {
            // Decode the token text once and cache it on the token; the tree walker later reads the
            // same token via $TOKEN.text, which would otherwise decode it from the input stream again.
            val text = token.text
            if (token is WritableToken) {
                token.text = text
            }
            return AstNode(token.type, token, text)
        }

        @JvmStatic
        fun retype(src: Token, type: Int): AstNode = retypedText(type, src, src.text)

        @JvmStatic
        fun retypedText(type: Int, src: Token?, text: String): AstNode {
            val token = CommonToken(type, text)
            if (src != null) {
                token.line = src.line
                token.charPositionInLine = src.charPositionInLine
                token.startIndex = src.startIndex
                token.stopIndex = src.stopIndex
            }
            return AstNode(type, token, text)
        }

        @JvmStatic
        fun imaginary(type: Int, start: Token?): AstNode =
            AstNode(type, makeImaginaryToken(type, start), nameFor(type))

        @JvmStatic
        fun node(type: Int, start: Token?, vararg children: AstNode): AstNode {
            val node = imaginary(type, start)
            for (child in children) {
                node.addChild(child)
            }
            return node
        }

        @JvmStatic
        fun node(type: Int, start: Token?, children: List<AstNode>): AstNode {
            val node = imaginary(type, start)
            node.addChildren(children)
            return node
        }

        @JvmStatic
        fun node(type: Int, text: String, start: Token?): AstNode =
            AstNode(type, makeImaginaryToken(type, start), text)

        /** Returns the token used for an imaginary (synthetic) node. */
        private fun makeImaginaryToken(type: Int, start: Token?): Token {
            val token = CommonToken(type, nameFor(type))
            if (start != null) {
                token.line = start.line
                token.charPositionInLine = start.charPositionInLine
                token.startIndex = start.startIndex
                token.stopIndex = start.stopIndex
            }
            return token
        }

        private fun nameFor(type: Int): String {
            val name = smaliParser.VOCABULARY.getSymbolicName(type)
            return name ?: Integer.toString(type)
        }

        private fun flatten(node: AstNode, out: MutableList<Token>) {
            if (node.type == FLAT) {
                for (child in node.children) {
                    flatten(child, out)
                }
                return
            }
            out.add(node.token!!)
            if (node.children.isNotEmpty()) {
                out.add(CommonToken(smaliParser.DOWN, "<DOWN>"))
                for (child in node.children) {
                    flatten(child, out)
                }
                out.add(CommonToken(smaliParser.UP, "<UP>"))
            }
        }
    }
}
