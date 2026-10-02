/*
 * The string related lexical rules are derived from rules from the
 * Java 1.6 grammar which can be found here:
 * http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
 *
 * Specifically, these rules:
 *
 * HEX_PREFIX, HEX_DIGIT, ESCAPE_SEQUENCE, STRING_LITERAL, BASE_STRING_LITERAL
 *
 * These rules were originally copyrighted by Terence Parr, and are used here in
 * accordance with the following license
 *
 * [The "BSD licence"]
 * Copyright (c) 2007-2008 Terence Parr
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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * The remainder of this grammar is released by me (Ben Gruver) under the
 * following license:
 *
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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

grammar expectedTokensTestGrammar;

@header {
package com.android.tools.smali.smali;

import java.util.Collections;
import java.util.List;
}

@parser::members {
	public static class ExpectedToken {
		public final String tokenName;
		public final String tokenText;

		public ExpectedToken(String tokenName, String tokenText) {
			this.tokenName = tokenName;
			this.tokenText = tokenText;
		}

		public ExpectedToken(String tokenName) {
			this.tokenName = tokenName;
			this.tokenText = null;
		}
	}

	private final ArrayList<ExpectedToken> expectedTokens = new ArrayList<ExpectedToken>();

	public List<ExpectedToken> getExpectedTokenList() {
		return Collections.unmodifiableList(expectedTokens);
	}

	private static String unescapeStringLiteral(String text) {
		StringBuilder sb = new StringBuilder();
		int length = text.length();
		for (int i = 1; i < length - 1; i++) {
			char c = text.charAt(i);
			if (c != '\\') {
				sb.append(c);
				continue;
			}
			char next = text.charAt(++i);
			switch (next) {
				case 'b': sb.append('\b'); break;
				case 't': sb.append('\t'); break;
				case 'n': sb.append('\n'); break;
				case 'f': sb.append('\f'); break;
				case 'r': sb.append('\r'); break;
				case '"': sb.append('"'); break;
				case '\'': sb.append('\''); break;
				case '\\': sb.append('\\'); break;
				case 'u': {
					sb.append((char)Integer.parseInt(text.substring(i + 1, i + 5), 16));
					i += 4;
					break;
				}
				default: sb.append(next); break;
			}
		}
		return sb.toString();
	}
}

fragment HEX_DIGIT
	:	('0'..'9')|('A'..'F')|('a'..'f');

fragment HEX_DIGITS
	:	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;

fragment ESCAPE_SEQUENCE
	:	'\\'
		(
			'b'
		|	't'
		|	'n'
		|	'f'
		|	'r'
		|	'"'
		|	'\''
		|	'\\'
		|	'u' HEX_DIGITS
		);

STRING_LITERAL
	:	'"'
		(	ESCAPE_SEQUENCE
		|	~( '\\' | '"' | '\r' | '\n' )
		)*
		'"';

TOKEN_NAME
	:	(('a'..'z')|('A' .. 'Z')|'_'|('0'..'9'))+;

WHITE_SPACE
	:	(' '|'\t'|'\n'|'\r')+ -> channel(HIDDEN);

top	:	token* EOF;

token	:	TOKEN_NAME ( '(' STRING_LITERAL ')' )
		{
			expectedTokens.add(new ExpectedToken($TOKEN_NAME.getText(), unescapeStringLiteral($STRING_LITERAL.getText())));
		} |
		TOKEN_NAME
		{
			expectedTokens.add(new ExpectedToken($TOKEN_NAME.getText()));
		};
