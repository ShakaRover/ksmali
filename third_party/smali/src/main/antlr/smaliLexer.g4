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

lexer grammar smaliLexer;

tokens { INVALID_TOKEN }

@header {
package com.android.tools.smali.smali;
}

@members {
  public static final int ERROR_CHANNEL = 100;

  private int apiLevel = 15;
  private int syntaxErrors = 0;
  private boolean suppressErrors = false;
  private java.io.File sourceFile;
  private boolean descriptorTokenError = false;

  public void setApiLevel(int apiLevel) {
    this.apiLevel = apiLevel;
  }

  public void setSuppressErrors(boolean suppressErrors) {
    this.suppressErrors = suppressErrors;
  }

  public void setSourceFile(java.io.File sourceFile) {
    this.sourceFile = sourceFile;
  }

  public int getNumberOfSyntaxErrors() {
    return syntaxErrors;
  }

  @Override
  public String getSourceName() {
    if (sourceFile == null) {
      return "";
    }
    try {
      return com.android.tools.smali.util.PathUtil.getRelativeFile(
          new java.io.File("."), sourceFile).getPath();
    } catch (java.io.IOException ex) {
      return sourceFile.getAbsolutePath();
    }
  }

  private void reportInvalid(String text, String message) {
    if (!suppressErrors) {
      System.err.println(getSourceName() + "[" + getLine() + "," + getCharPositionInLine() +
          "] Error for input '" + text + "': " + message);
    }
    syntaxErrors++;
    setType(smaliLexer.INVALID_TOKEN);
    setChannel(ERROR_CHANNEL);
    setText(text);
  }

  private static boolean isHexDigit(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isUnicodeSpace(char c) {
    return c == '\u0020' || c == '\u00A0' || c == '\u1680' ||
        (c >= '\u2000' && c <= '\u200A') || c == '\u202F' || c == '\u205F' || c == '\u3000';
  }

  private String processQuotedSimpleName(String text) {
    if (text.charAt(0) == '`') {
      return text.substring(1, text.length() - 1);
    }
    return text;
  }

  private String processClassDescriptor(String text) {
    descriptorTokenError = false;
    if (text.indexOf('`') < 0) {
      return text;
    }
    StringBuilder sb = new StringBuilder();
    int i = 0;
    int len = text.length();
    while (i < len) {
      char c = text.charAt(i);
      if (c == '`') {
        int end = text.indexOf('`', i + 1);
        if (end < 0) {
          end = len;
        }
        String name = text.substring(i + 1, end);
        if (apiLevel < 30) {
          for (int j = 0; j < name.length(); j++) {
            if (isUnicodeSpace(name.charAt(j))) {
              descriptorTokenError = true;
              break;
            }
          }
        }
        sb.append(name);
        i = end + 1;
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

  private void processQuotedLiteral(String text, boolean isChar) {
    int len = text.length();
    if (text.indexOf('\\') < 0) {
      // No escapes: the literal text is already completely processed.
      char closeQuote = isChar ? '\'' : '"';
      if (len < 2 || text.charAt(len - 1) != closeQuote) {
        int end = len;
        if (end > 0 && (text.charAt(end - 1) == '\r' || text.charAt(end - 1) == '\n')) {
          end--;
        }
        reportInvalid(text.substring(0, end),
            isChar ? "Unterminated character literal" : "Unterminated string literal");
        return;
      }
      if (isChar) {
        if (len == 2) {
          reportInvalid(text, "Empty character literal");
          return;
        }
        if (len > 3) {
          reportInvalid(text, "Character literal with multiple chars");
          return;
        }
      }
      setText(text);
      return;
    }
    StringBuilder sb = new StringBuilder();
    int i = 1;
    sb.append(text.charAt(0));
    boolean closed = false;
    String error = null;
    char closeQuote = isChar ? '\'' : '"';
    while (i < len) {
      char c = text.charAt(i);
      if (c == '\\') {
        if (i + 1 >= len) {
          sb.append(c);
          i++;
          continue;
        }
        char d = text.charAt(i + 1);
        if (d == 'u') {
          int h = 0;
          while (i + 2 + h < len && h < 4 && isHexDigit(text.charAt(i + 2 + h))) {
            h++;
          }
          if (h >= 4) {
            sb.append((char) Integer.parseInt(text.substring(i + 2, i + 6), 16));
            i += 6;
          } else {
            sb.append(text, i, i + 2 + h);
            error = isChar
                ? "Invalid \\u sequence. \\u must be followed by exactly 4 hex digits"
                : "Invalid \\u sequence. \\u must be followed by 4 hex digits";
            i += 2 + h;
          }
        } else if (d == 'b') { sb.append('\b'); i += 2; }
        else if (d == 't') { sb.append('\t'); i += 2; }
        else if (d == 'n') { sb.append('\n'); i += 2; }
        else if (d == 'f') { sb.append('\f'); i += 2; }
        else if (d == 'r') { sb.append('\r'); i += 2; }
        else if (d == '\'') { sb.append('\''); i += 2; }
        else if (d == '"') { sb.append('"'); i += 2; }
        else if (d == '\\') { sb.append('\\'); i += 2; }
        else {
          sb.append(text, i, i + 2);
          error = "Invalid escape sequence " + text.substring(i, i + 2);
          i += 2;
        }
      } else if (c == closeQuote) {
        sb.append(c);
        i++;
        closed = true;
        break;
      } else if (c == '\r' || c == '\n') {
        break;
      } else {
        sb.append(c);
        i++;
      }
    }
    if (!closed) {
      reportInvalid(sb.toString(),
          isChar ? "Unterminated character literal" : "Unterminated string literal");
      return;
    }
    if (error != null) {
      reportInvalid(sb.toString(), error);
      return;
    }
    if (isChar) {
      if (sb.length() == 2) {
        reportInvalid(sb.toString(), "Empty character literal");
        return;
      }
      if (sb.length() > 3) {
        reportInvalid(sb.toString(), "Character literal with multiple chars");
        return;
      }
    }
    setText(sb.toString());
  }
}
ACCESS_SPEC
  : 'public' | 'private' | 'protected' | 'static' | 'final' | 'synchronized' | 'bridge' | 'varargs' | 'native' | 'abstract' | 'strictfp' | 'synthetic' | 'constructor' | 'declared-synchronized' | 'interface' | 'enum' | 'annotation' | 'volatile' | 'transient'
  ;

ANNOTATION_DIRECTIVE
  : '.annotation'
  ;

ANNOTATION_VISIBILITY
  : 'build' | 'runtime' | 'system'
  ;

ARRAY_DATA_DIRECTIVE
  : '.array-data'
  ;

ARROW
  : '->'
  ;

AT
  : '@'
  ;

BOOL_LITERAL
  : 'true' | 'false'
  ;

CATCHALL_DIRECTIVE
  : '.catchall'
  ;

CATCH_DIRECTIVE
  : '.catch'
  ;

CLASS_DIRECTIVE
  : '.class'
  ;

CLOSE_BRACE
  : '}'
  ;

CLOSE_PAREN
  : ')'
  ;

COLON
  : ':'
  ;

COMMA
  : ','
  ;

DOTDOT
  : '..'
  ;

END_ANNOTATION_DIRECTIVE
  : '.end annotation'
  ;

END_ARRAY_DATA_DIRECTIVE
  : '.end array-data'
  ;

END_FIELD_DIRECTIVE
  : '.end field'
  ;

END_LOCAL_DIRECTIVE
  : '.end local'
  ;

END_METHOD_DIRECTIVE
  : '.end method'
  ;

END_PACKED_SWITCH_DIRECTIVE
  : '.end packed-switch'
  ;

END_PARAMETER_DIRECTIVE
  : '.end param'
  ;

END_SPARSE_SWITCH_DIRECTIVE
  : '.end sparse-switch'
  ;

END_SUBANNOTATION_DIRECTIVE
  : '.end subannotation'
  ;

ENUM_DIRECTIVE
  : '.enum'
  ;

EPILOGUE_DIRECTIVE
  : '.epilogue'
  ;

EQUAL
  : '='
  ;

FIELD_DIRECTIVE
  : '.field'
  ;

HIDDENAPI_RESTRICTION
  : 'whitelist' | 'greylist' | 'blacklist' | 'greylist-max-o' | 'greylist-max-p' | 'greylist-max-q' | 'greylist-max-r' | 'core-platform-api' | 'test-api'
  ;

IMPLEMENTS_DIRECTIVE
  : '.implements'
  ;

INSTRUCTION_FORMAT10t
  : 'goto'
  ;

INSTRUCTION_FORMAT10x
  : 'return-void' | 'nop'
  ;

INSTRUCTION_FORMAT10x_ODEX
  : 'return-void-barrier' | 'return-void-no-barrier'
  ;

INSTRUCTION_FORMAT11n
  : 'const/4'
  ;

INSTRUCTION_FORMAT11x
  : 'move-result' | 'move-result-wide' | 'move-result-object' | 'move-exception' | 'return' | 'return-wide' | 'return-object' | 'monitor-enter' | 'monitor-exit' | 'throw'
  ;

INSTRUCTION_FORMAT12x
  : 'add-int/2addr' | 'sub-int/2addr' | 'mul-int/2addr' | 'div-int/2addr' | 'rem-int/2addr' | 'and-int/2addr' | 'or-int/2addr' | 'xor-int/2addr' | 'shl-int/2addr' | 'shr-int/2addr' | 'ushr-int/2addr' | 'add-long/2addr' | 'sub-long/2addr' | 'mul-long/2addr' | 'div-long/2addr' | 'rem-long/2addr' | 'and-long/2addr' | 'or-long/2addr' | 'xor-long/2addr' | 'shl-long/2addr' | 'shr-long/2addr' | 'ushr-long/2addr' | 'add-float/2addr' | 'sub-float/2addr' | 'mul-float/2addr' | 'div-float/2addr' | 'rem-float/2addr' | 'add-double/2addr' | 'sub-double/2addr' | 'mul-double/2addr' | 'div-double/2addr' | 'rem-double/2addr'
  ;

INSTRUCTION_FORMAT12x_OR_ID
  : 'move' | 'move-wide' | 'move-object' | 'array-length' | 'neg-int' | 'not-int' | 'neg-long' | 'not-long' | 'neg-float' | 'neg-double' | 'int-to-long' | 'int-to-float' | 'int-to-double' | 'long-to-int' | 'long-to-float' | 'long-to-double' | 'float-to-int' | 'float-to-long' | 'float-to-double' | 'double-to-int' | 'double-to-long' | 'double-to-float' | 'int-to-byte' | 'int-to-char' | 'int-to-short'
  ;

INSTRUCTION_FORMAT20bc
  : 'throw-verification-error'
  ;

INSTRUCTION_FORMAT20t
  : 'goto/16'
  ;

INSTRUCTION_FORMAT21c_FIELD
  : 'sget' | 'sget-wide' | 'sget-object' | 'sget-boolean' | 'sget-byte' | 'sget-char' | 'sget-short' | 'sput' | 'sput-wide' | 'sput-object' | 'sput-boolean' | 'sput-byte' | 'sput-char' | 'sput-short'
  ;

INSTRUCTION_FORMAT21c_FIELD_ODEX
  : 'sget-volatile' | 'sget-wide-volatile' | 'sget-object-volatile' | 'sput-volatile' | 'sput-wide-volatile' | 'sput-object-volatile'
  ;

INSTRUCTION_FORMAT21c_METHOD_HANDLE
  : 'const-method-handle'
  ;

INSTRUCTION_FORMAT21c_METHOD_TYPE
  : 'const-method-type'
  ;

INSTRUCTION_FORMAT21c_STRING
  : 'const-string'
  ;

INSTRUCTION_FORMAT21c_TYPE
  : 'check-cast' | 'new-instance' | 'const-class'
  ;

INSTRUCTION_FORMAT21ih
  : 'const/high16'
  ;

INSTRUCTION_FORMAT21lh
  : 'const-wide/high16'
  ;

INSTRUCTION_FORMAT21s
  : 'const/16' | 'const-wide/16'
  ;

INSTRUCTION_FORMAT21t
  : 'if-eqz' | 'if-nez' | 'if-ltz' | 'if-gez' | 'if-gtz' | 'if-lez'
  ;

INSTRUCTION_FORMAT22b
  : 'add-int/lit8' | 'rsub-int/lit8' | 'mul-int/lit8' | 'div-int/lit8' | 'rem-int/lit8' | 'and-int/lit8' | 'or-int/lit8' | 'xor-int/lit8' | 'shl-int/lit8' | 'shr-int/lit8' | 'ushr-int/lit8'
  ;

INSTRUCTION_FORMAT22c_FIELD
  : 'iget' | 'iget-wide' | 'iget-object' | 'iget-boolean' | 'iget-byte' | 'iget-char' | 'iget-short' | 'iput' | 'iput-wide' | 'iput-object' | 'iput-boolean' | 'iput-byte' | 'iput-char' | 'iput-short'
  ;

INSTRUCTION_FORMAT22c_FIELD_ODEX
  : 'iget-volatile' | 'iget-wide-volatile' | 'iget-object-volatile' | 'iput-volatile' | 'iput-wide-volatile' | 'iput-object-volatile'
  ;

INSTRUCTION_FORMAT22c_TYPE
  : 'instance-of' | 'new-array'
  ;

INSTRUCTION_FORMAT22cs_FIELD
  : 'iget-quick' | 'iget-wide-quick' | 'iget-object-quick' | 'iput-quick' | 'iput-wide-quick' | 'iput-object-quick' | 'iput-boolean-quick' | 'iput-byte-quick' | 'iput-char-quick' | 'iput-short-quick'
  ;

INSTRUCTION_FORMAT22s
  : 'add-int/lit16' | 'mul-int/lit16' | 'div-int/lit16' | 'rem-int/lit16' | 'and-int/lit16' | 'or-int/lit16' | 'xor-int/lit16'
  ;

INSTRUCTION_FORMAT22s_OR_ID
  : 'rsub-int'
  ;

INSTRUCTION_FORMAT22t
  : 'if-eq' | 'if-ne' | 'if-lt' | 'if-ge' | 'if-gt' | 'if-le'
  ;

INSTRUCTION_FORMAT22x
  : 'move/from16' | 'move-wide/from16' | 'move-object/from16'
  ;

INSTRUCTION_FORMAT23x
  : 'cmpl-float' | 'cmpg-float' | 'cmpl-double' | 'cmpg-double' | 'cmp-long' | 'aget' | 'aget-wide' | 'aget-object' | 'aget-boolean' | 'aget-byte' | 'aget-char' | 'aget-short' | 'aput' | 'aput-wide' | 'aput-object' | 'aput-boolean' | 'aput-byte' | 'aput-char' | 'aput-short' | 'add-int' | 'sub-int' | 'mul-int' | 'div-int' | 'rem-int' | 'and-int' | 'or-int' | 'xor-int' | 'shl-int' | 'shr-int' | 'ushr-int' | 'add-long' | 'sub-long' | 'mul-long' | 'div-long' | 'rem-long' | 'and-long' | 'or-long' | 'xor-long' | 'shl-long' | 'shr-long' | 'ushr-long' | 'add-float' | 'sub-float' | 'mul-float' | 'div-float' | 'rem-float' | 'add-double' | 'sub-double' | 'mul-double' | 'div-double' | 'rem-double'
  ;

INSTRUCTION_FORMAT30t
  : 'goto/32'
  ;

INSTRUCTION_FORMAT31c
  : 'const-string/jumbo'
  ;

INSTRUCTION_FORMAT31i
  : 'const-wide/32'
  ;

INSTRUCTION_FORMAT31i_OR_ID
  : 'const'
  ;

INSTRUCTION_FORMAT31t
  : 'fill-array-data' | 'packed-switch' | 'sparse-switch'
  ;

INSTRUCTION_FORMAT32x
  : 'move/16' | 'move-wide/16' | 'move-object/16'
  ;

INSTRUCTION_FORMAT35c_CALL_SITE
  : 'invoke-custom'
  ;

INSTRUCTION_FORMAT35c_METHOD
  : 'invoke-virtual' | 'invoke-super'
  ;

INSTRUCTION_FORMAT35c_METHOD_ODEX
  : 'invoke-direct-empty'
  ;

INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE
  : 'invoke-direct' | 'invoke-static' | 'invoke-interface'
  ;

INSTRUCTION_FORMAT35c_TYPE
  : 'filled-new-array'
  ;

INSTRUCTION_FORMAT35mi_METHOD
  : 'execute-inline'
  ;

INSTRUCTION_FORMAT35ms_METHOD
  : 'invoke-virtual-quick' | 'invoke-super-quick'
  ;

INSTRUCTION_FORMAT3rc_CALL_SITE
  : 'invoke-custom/range'
  ;

INSTRUCTION_FORMAT3rc_METHOD
  : 'invoke-virtual/range' | 'invoke-super/range' | 'invoke-direct/range' | 'invoke-static/range' | 'invoke-interface/range'
  ;

INSTRUCTION_FORMAT3rc_METHOD_ODEX
  : 'invoke-object-init/range'
  ;

INSTRUCTION_FORMAT3rc_TYPE
  : 'filled-new-array/range'
  ;

INSTRUCTION_FORMAT3rmi_METHOD
  : 'execute-inline/range'
  ;

INSTRUCTION_FORMAT3rms_METHOD
  : 'invoke-virtual-quick/range' | 'invoke-super-quick/range'
  ;

INSTRUCTION_FORMAT45cc_METHOD
  : 'invoke-polymorphic'
  ;

INSTRUCTION_FORMAT4rcc_METHOD
  : 'invoke-polymorphic/range'
  ;

INSTRUCTION_FORMAT51l
  : 'const-wide'
  ;

LINE_DIRECTIVE
  : '.line'
  ;

LOCALS_DIRECTIVE
  : '.locals'
  ;

LOCAL_DIRECTIVE
  : '.local'
  ;


METHOD_DIRECTIVE
  : '.method'
  ;

METHOD_HANDLE_TYPE_FIELD
  : 'static-put' | 'static-get' | 'instance-put' | 'instance-get'
  ;

METHOD_HANDLE_TYPE_METHOD
  : 'invoke-instance' | 'invoke-constructor'
  ;

NULL_LITERAL
  : 'null'
  ;

OPEN_BRACE
  : '{'
  ;

OPEN_PAREN
  : '('
  ;

PACKED_SWITCH_DIRECTIVE
  : '.packed-switch'
  ;

PARAMETER_DIRECTIVE
  : '.param'
  ;

PROLOGUE_DIRECTIVE
  : '.prologue'
  ;

REGISTERS_DIRECTIVE
  : '.registers'
  ;

RESTART_LOCAL_DIRECTIVE
  : '.restart local'
  ;

SOURCE_DIRECTIVE
  : '.source'
  ;

SPARSE_SWITCH_DIRECTIVE
  : '.sparse-switch'
  ;

SUBANNOTATION_DIRECTIVE
  : '.subannotation'
  ;

SUPER_DIRECTIVE
  : '.super'
  ;

VERIFICATION_ERROR_TYPE
  : 'no-error' | 'generic-error' | 'no-such-class' | 'no-such-field' | 'no-such-method' | 'illegal-class-access' | 'illegal-field-access' | 'illegal-method-access' | 'class-change-error' | 'instantiation-error'
  ;


fragment HEX_PREFIX : '0' [xX] ;
fragment HEX_DIGIT : [0-9a-fA-F] ;
fragment FEWER_HEX_DIGITS : HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? ;
fragment INTEGER1 : '0' ;
fragment INTEGER2 : [1-9] [0-9]* ;
fragment INTEGER3 : '0' [0-7]+ ;
fragment INTEGER4 : HEX_PREFIX HEX_DIGIT+ ;
fragment INTEGER : INTEGER1 | INTEGER2 | INTEGER3 | INTEGER4 ;
fragment DECIMAL_EXPONENT : [eE] '-'? [0-9]+ ;
fragment BINARY_EXPONENT : [pP] '-'? [0-9]+ ;
fragment FLOAT_OR_ID1 : '-'? [0-9]+ DECIMAL_EXPONENT ;
fragment FLOAT_OR_ID2 : '-'? HEX_PREFIX HEX_DIGIT+ BINARY_EXPONENT ;
fragment FLOAT_OR_ID3 : '-'? [iI][nN][fF][iI][nN][iI][tT][yY] ;
fragment FLOAT_OR_ID4 : [nN][aA][nN] ;
fragment FLOAT_OR_ID : FLOAT_OR_ID1 | FLOAT_OR_ID2 | FLOAT_OR_ID3 | FLOAT_OR_ID4 ;
fragment FLOAT1 : '-'? [0-9]+ '.' [0-9]* DECIMAL_EXPONENT? ;
fragment FLOAT2 : '-'? '.' [0-9]+ DECIMAL_EXPONENT? ;
fragment FLOAT3 : '-'? HEX_PREFIX HEX_DIGIT+ '.' HEX_DIGIT* BINARY_EXPONENT ;
fragment FLOAT4 : '-'? HEX_PREFIX '.' HEX_DIGIT+ BINARY_EXPONENT ;
fragment FLOAT : FLOAT1 | FLOAT2 | FLOAT3 | FLOAT4 ;
fragment HIGH_SURROGATE : [\ud800-\udbff] ;
fragment LOW_SURROGATE : [\udc00-\udfff] ;
fragment SIMPLE_NAME_CHARACTER : (HIGH_SURROGATE LOW_SURROGATE) | [A-Za-z0-9$\-_\u00a1-\u1fff\u2010-\u2027\u2030-\ud7ff\ue000-\uffef] ;
fragment UNICODE_SPACE : [\u0020\u00A0\u1680\u2000-\u200A\u202F\u205F\u3000] ;
fragment SIMPLE_NAME_RAW : SIMPLE_NAME_CHARACTER+ ;
fragment SIMPLE_NAME_QUOTED : '`' SIMPLE_NAME_CHARACTER+ '`' ;
fragment SIMPLE_NAME_QUOTED_WITH_SPACES : '`' (SIMPLE_NAME_CHARACTER | UNICODE_SPACE)+ '`' ;
fragment SIMPLE_NAME_FRAG : SIMPLE_NAME_QUOTED_WITH_SPACES | SIMPLE_NAME_QUOTED | SIMPLE_NAME_RAW ;
fragment PRIMITIVE_TYPE_CHAR : [ZBSCIJFD] ;
fragment ARRAY_PREFIX : '['+ ;
fragment CLASS_DESCRIPTOR_FRAG : 'L' (SIMPLE_NAME_FRAG '/')* SIMPLE_NAME_FRAG ';' ;
fragment TYPE_ELEM : PRIMITIVE_TYPE_CHAR | CLASS_DESCRIPTOR_FRAG | ARRAY_PREFIX (CLASS_DESCRIPTOR_FRAG | PRIMITIVE_TYPE_CHAR) ;

POSITIVE_INTEGER_LITERAL : INTEGER ;
NEGATIVE_INTEGER_LITERAL : '-' INTEGER ;
LONG_LITERAL : '-'? INTEGER [lL] ;
SHORT_LITERAL : '-'? INTEGER [sS] ;
BYTE_LITERAL : '-'? INTEGER [tT] ;
FLOAT_LITERAL_OR_ID : FLOAT_OR_ID [fF] | '-'? [0-9]+ [fF] ;
DOUBLE_LITERAL_OR_ID : FLOAT_OR_ID [dD]? | '-'? [0-9]+ [dD] ;
FLOAT_LITERAL : FLOAT [fF] ;
DOUBLE_LITERAL : FLOAT [dD]? ;

REGISTER : [vp] [0-9]+ ;
LINE_COMMENT : '#' ~[\r\n]* -> channel(HIDDEN) ;
INLINE_INDEX : 'inline@0x' HEX_DIGIT+ ;
VTABLE_INDEX : 'vtable@0x' HEX_DIGIT+ ;
FIELD_OFFSET : 'field@0x' HEX_DIGIT+ ;

STRING_LITERAL
  : '"' ( '\\' . | ~["\\\r\n] )* '"' { processQuotedLiteral(getText(), false); }
  | '"' ( '\\' . | ~["\\\r\n] )* [\r\n] { processQuotedLiteral(getText(), false); }
  | '"' ( '\\' . | ~["\\\r\n] )* EOF { processQuotedLiteral(getText(), false); }
  ;

CHAR_LITERAL
  : '\'' ( '\\' . | ~['\\\r\n] )* '\'' { processQuotedLiteral(getText(), true); }
  | '\'' ( '\\' . | ~['\\\r\n] )* [\r\n] { processQuotedLiteral(getText(), true); }
  | '\'' ( '\\' . | ~['\\\r\n] )* EOF { processQuotedLiteral(getText(), true); }
  ;

PRIMITIVE_TYPE : PRIMITIVE_TYPE_CHAR ;
VOID_TYPE : 'V' ;
CLASS_DESCRIPTOR
  : CLASS_DESCRIPTOR_FRAG
    {
      String processed = processClassDescriptor(getText());
      if (descriptorTokenError) {
        reportInvalid(processed, "spaces in class descriptors and member names are not supported " +
            "prior to API level 30/dex version 040");
      } else {
        setText(processed);
      }
    }
  ;
ARRAY_TYPE_PREFIX : ARRAY_PREFIX { pushMode(ARRAY_DESCRIPTOR); } ;
PRIMITIVE_RUN : PRIMITIVE_TYPE_CHAR PRIMITIVE_TYPE_CHAR+ { _input.seek(_tokenStartCharIndex); pushMode(PARAM_LIST_OR_ID); skip(); } ;
TYPE_RUN : TYPE_ELEM TYPE_ELEM+ { _input.seek(_tokenStartCharIndex); pushMode(PARAM_LIST); skip(); } ;
SIMPLE_NAME
  : SIMPLE_NAME_QUOTED_WITH_SPACES { setText(processQuotedSimpleName(getText())); }
  | SIMPLE_NAME_QUOTED { setText(processQuotedSimpleName(getText())); }
  | SIMPLE_NAME_RAW
  ;
MEMBER_NAME : '<' SIMPLE_NAME_RAW '>' ;
WHITE_SPACE : [ \t\r\n]+ -> channel(HIDDEN) ;

INVALID_END_DIRECTIVE : '.end ' [a-zA-Z0-9\-_]+ { reportInvalid(getText(), "Invalid directive"); } ;
INVALID_RESTART_DIRECTIVE : '.restart ' [a-zA-Z0-9\-_]+ { reportInvalid(getText(), "Invalid directive"); } ;
INVALID_END : '.end' { reportInvalid(getText(), "Invalid directive"); } ;
INVALID_RESTART : '.restart' { reportInvalid(getText(), "Invalid directive"); } ;
INVALID_DIRECTIVE : '.' [a-zA-Z\-_] [a-zA-Z0-9\-_]* { reportInvalid(getText(), "Invalid directive"); } | '.' { reportInvalid(getText(), "Invalid directive"); } ;
INVALID_TEXT : . { reportInvalid(getText(), "Invalid text"); } ;

mode ARRAY_DESCRIPTOR;

ARRAY_DESCRIPTOR_PRIMITIVE_TYPE : PRIMITIVE_TYPE_CHAR { setType(PRIMITIVE_TYPE); popMode(); } ;
ARRAY_DESCRIPTOR_CLASS_DESCRIPTOR
  : CLASS_DESCRIPTOR_FRAG
    {
      String processed = processClassDescriptor(getText());
      if (descriptorTokenError) {
        reportInvalid(processed, "spaces in class descriptors and member names are not supported " +
            "prior to API level 30/dex version 040");
      } else {
        setType(CLASS_DESCRIPTOR);
        setText(processed);
      }
      popMode();
    }
  ;
ARRAY_DESCRIPTOR_PUSHBACK : . { _input.seek(_tokenStartCharIndex); popMode(); skip(); } ;

mode PARAM_LIST_OR_ID;

PARAM_LIST_OR_ID_PRIMITIVE_TYPE : PRIMITIVE_TYPE_CHAR ;
PARAM_LIST_OR_ID_PUSHBACK : . { _input.seek(_tokenStartCharIndex); popMode(); skip(); } ;

mode PARAM_LIST;

PARAM_LIST_PRIMITIVE_TYPE : PRIMITIVE_TYPE_CHAR { setType(PRIMITIVE_TYPE); } ;
PARAM_LIST_CLASS_DESCRIPTOR
  : CLASS_DESCRIPTOR_FRAG
    {
      String processed = processClassDescriptor(getText());
      if (descriptorTokenError) {
        reportInvalid(processed, "spaces in class descriptors and member names are not supported " +
            "prior to API level 30/dex version 040");
      } else {
        setType(CLASS_DESCRIPTOR);
        setText(processed);
      }
    }
  ;
PARAM_LIST_ARRAY_TYPE_PREFIX : ARRAY_PREFIX { setType(ARRAY_TYPE_PREFIX); } ;
PARAM_LIST_PUSHBACK : . { _input.seek(_tokenStartCharIndex); popMode(); skip(); } ;
