/*
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

parser grammar smaliParser;

options {
  tokenVocab=smaliLexer;
}

tokens {
  INTEGER_LITERAL,
  I_CLASS_DEF,
  I_SUPER,
  I_IMPLEMENTS,
  I_SOURCE,
  I_ACCESS_LIST,
  I_ACCESS_OR_RESTRICTION_LIST,
  I_METHODS,
  I_FIELDS,
  I_FIELD,
  I_FIELD_TYPE,
  I_FIELD_INITIAL_VALUE,
  I_METHOD,
  I_METHOD_PROTOTYPE,
  I_METHOD_RETURN_TYPE,
  I_REGISTERS,
  I_LOCALS,
  I_LABEL,
  I_ANNOTATIONS,
  I_ANNOTATION,
  I_ANNOTATION_ELEMENT,
  I_SUBANNOTATION,
  I_ENCODED_METHOD_HANDLE,
  I_ENCODED_FIELD,
  I_ENCODED_METHOD,
  I_ENCODED_ENUM,
  I_ENCODED_ARRAY,
  I_ARRAY_ELEMENT_SIZE,
  I_ARRAY_ELEMENTS,
  I_PACKED_SWITCH_START_KEY,
  I_PACKED_SWITCH_ELEMENTS,
  I_SPARSE_SWITCH_ELEMENTS,
  I_CATCH,
  I_CATCHALL,
  I_CATCHES,
  I_PARAMETER,
  I_PARAMETERS,
  I_PARAMETER_NOT_SPECIFIED,
  I_LINE,
  I_LOCAL,
  I_END_LOCAL,
  I_RESTART_LOCAL,
  I_PROLOGUE,
  I_EPILOGUE,
  I_ORDERED_METHOD_ITEMS,
  I_STATEMENT_FORMAT10t,
  I_STATEMENT_FORMAT10x,
  I_STATEMENT_FORMAT11n,
  I_STATEMENT_FORMAT11x,
  I_STATEMENT_FORMAT12x,
  I_STATEMENT_FORMAT20bc,
  I_STATEMENT_FORMAT20t,
  I_STATEMENT_FORMAT21c_TYPE,
  I_STATEMENT_FORMAT21c_FIELD,
  I_STATEMENT_FORMAT21c_STRING,
  I_STATEMENT_FORMAT21c_METHOD_HANDLE,
  I_STATEMENT_FORMAT21c_METHOD_TYPE,
  I_STATEMENT_FORMAT21ih,
  I_STATEMENT_FORMAT21lh,
  I_STATEMENT_FORMAT21s,
  I_STATEMENT_FORMAT21t,
  I_STATEMENT_FORMAT22b,
  I_STATEMENT_FORMAT22c_FIELD,
  I_STATEMENT_FORMAT22c_TYPE,
  I_STATEMENT_FORMAT22s,
  I_STATEMENT_FORMAT22t,
  I_STATEMENT_FORMAT22x,
  I_STATEMENT_FORMAT23x,
  I_STATEMENT_FORMAT30t,
  I_STATEMENT_FORMAT31c,
  I_STATEMENT_FORMAT31i,
  I_STATEMENT_FORMAT31t,
  I_STATEMENT_FORMAT32x,
  I_STATEMENT_FORMAT35c_CALL_SITE,
  I_STATEMENT_FORMAT35c_METHOD,
  I_STATEMENT_FORMAT35c_TYPE,
  I_STATEMENT_FORMAT3rc_CALL_SITE,
  I_STATEMENT_FORMAT3rc_METHOD,
  I_STATEMENT_FORMAT3rc_TYPE,
  I_STATEMENT_FORMAT45cc_METHOD,
  I_STATEMENT_FORMAT4rcc_METHOD,
  I_STATEMENT_FORMAT51l,
  I_STATEMENT_ARRAY_DATA,
  I_STATEMENT_PACKED_SWITCH,
  I_STATEMENT_SPARSE_SWITCH,
  I_REGISTER_RANGE,
  I_REGISTER_LIST,
  I_CALL_SITE_EXTRA_ARGUMENTS,
  I_CALL_SITE_REFERENCE,
  DOWN,
  UP
}


@parser::header {
package com.android.tools.smali.smali;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
}

@parser::members {
  public static final int ERROR_CHANNEL = 100;

  private boolean verboseErrors = false;
  private boolean allowOdex = false;
  private int apiLevel = 15;
  private Opcodes opcodes = Opcodes.forApi(apiLevel);

  // Replaces the ANTLR3 smali_file scope
  private boolean smaliFileHasClassSpec;
  private boolean smaliFileHasSuperSpec;
  private boolean smaliFileHasSourceSpec;
  private List<AstNode> classAnnotations = new ArrayList<AstNode>();

  // Replaces the ANTLR3 statements_and_directives scope
  private boolean statementsHasRegistersDirective;
  private List<AstNode> statementsMethodAnnotations;

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  public void setAllowOdex(boolean allowOdex) {
    this.allowOdex = allowOdex;
  }

  public void setApiLevel(int apiLevel) {
    this.opcodes = Opcodes.forApi(apiLevel);
    this.apiLevel = apiLevel;
  }

  public Opcodes getOpcodes() {
    return opcodes;
  }

  /** The symbolic name of a token type (unlike the deprecated tokenNames array, which prefers literals). */
  public static String tokenName(int type) {
    String name = VOCABULARY.getSymbolicName(type);
    if (name != null) {
      return name;
    }
    name = VOCABULARY.getLiteralName(type);
    return name != null ? name : "<INVALID>";
  }

  // ---- AST construction helpers -------------------------------------------

  private AstNode ast(int type, Token start, Object... parts) {
    AstNode node = AstNode.imaginary(type, start);
    addParts(node, parts);
    return node;
  }

  private AstNode flat(Object... parts) {
    AstNode node = AstNode.flat(new ArrayList<AstNode>());
    addParts(node, parts);
    return node;
  }

  private void addParts(AstNode node, Object[] parts) {
    for (Object part : parts) {
      if (part == null) {
        continue;
      }
      if (part instanceof AstNode) {
        node.addChild((AstNode) part);
      } else if (part instanceof Token) {
        node.addChild(AstNode.leaf((Token) part));
      } else if (part instanceof List) {
        for (Object item : (List<?>) part) {
          addParts(node, new Object[] { item });
        }
      } else {
        throw new IllegalArgumentException("Unsupported AST part: " + part);
      }
    }
  }

  private AstNode retype(Token src, int type) {
    return AstNode.retype(src, type);
  }

  private AstNode retypedText(int type, Token start, String text) {
    return AstNode.retypedText(type, start, text);
  }

  private String textOf(ParserRuleContext ctx) {
    return _input.getText(ctx.start, ctx.stop);
  }

  private AstNode buildTree(int type, List<AstNode> children) {
    return AstNode.node(type, null, children);
  }

  private void throwOdexedInstructionException(String odexedInstruction)
      throws OdexedInstructionException {
    throw new OdexedInstructionException(_input, odexedInstruction);
  }
}


smali_file returns[AstNode n]
  @init
  { smaliFileHasClassSpec = smaliFileHasSuperSpec = smaliFileHasSourceSpec = false;
    classAnnotations = new ArrayList<AstNode>();
  }
  :
  ( {!smaliFileHasClassSpec}? class_spec { smaliFileHasClassSpec = true; }
  | {!smaliFileHasSuperSpec}? ss=super_spec { smaliFileHasSuperSpec = true; }
  | impl+=implements_spec
  | {!smaliFileHasSourceSpec}? src=source_spec { smaliFileHasSourceSpec = true; }
  | m+=method
  | fld+=field
  | annotation { classAnnotations.add($annotation.n); }
  )+
  EOF
  {
    if (!smaliFileHasClassSpec) {
      throw new SemanticException(_input, _localctx.start, "The file must contain a .class directive");
    }

    if (!smaliFileHasSuperSpec) {
      if (!$class_spec.className.equals("Ljava/lang/Object;")) {
        throw new SemanticException(_input, _localctx.start, "The file must contain a .super directive");
      }
    }

    List<AstNode> implementsList = new ArrayList<AstNode>();
    for (Implements_specContext ctx : $impl) {
      implementsList.add(ctx.n);
    }
    List<AstNode> methodList = new ArrayList<AstNode>();
    for (MethodContext ctx : $m) {
      methodList.add(ctx.n);
    }
    List<AstNode> fieldList = new ArrayList<AstNode>();
    for (FieldContext ctx : $fld) {
      fieldList.add(ctx.n);
    }

    $n = ast(I_CLASS_DEF, _localctx.start,
             $class_spec.n,
             _localctx.ss != null ? $ss.n : null,
             implementsList,
             _localctx.src != null ? $src.n : null,
             ast(I_METHODS, _localctx.start, methodList),
             ast(I_FIELDS, _localctx.start, fieldList),
             buildTree(I_ANNOTATIONS, classAnnotations));
  };

class_spec returns[String className, AstNode n]
  : CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR
    { $className = $CLASS_DESCRIPTOR.text;
      $n = flat(AstNode.leaf($CLASS_DESCRIPTOR), $access_list.n); };

super_spec returns[AstNode n]
  : SUPER_DIRECTIVE CLASS_DESCRIPTOR
    { $n = ast(I_SUPER, _localctx.start, $CLASS_DESCRIPTOR); };

implements_spec returns[AstNode n]
  : IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR
    { $n = ast(I_IMPLEMENTS, _localctx.start, $CLASS_DESCRIPTOR); };

source_spec returns[AstNode n]
  : SOURCE_DIRECTIVE STRING_LITERAL
    { $n = ast(I_SOURCE, _localctx.start, $STRING_LITERAL); };

access_list returns[AstNode n]
  @init { List<AstNode> specs = new ArrayList<AstNode>(); }
  : ( ACCESS_SPEC { specs.add(AstNode.leaf($ACCESS_SPEC)); } )*
    { $n = ast(I_ACCESS_LIST, _localctx.start, specs); };

access_or_restriction returns[AstNode n]
  : ACCESS_SPEC { $n = AstNode.leaf($ACCESS_SPEC); }
  | HIDDENAPI_RESTRICTION { $n = AstNode.leaf($HIDDENAPI_RESTRICTION); };

access_or_restriction_list returns[AstNode n]
  @init { List<AstNode> items = new ArrayList<AstNode>(); }
  : ( access_or_restriction { items.add($access_or_restriction.n); } )*
    { $n = ast(I_ACCESS_OR_RESTRICTION_LIST, _localctx.start, items); };

field returns[AstNode n]
  @init { List<AstNode> annotations = new ArrayList<AstNode>(); }
  : FIELD_DIRECTIVE access_or_restriction_list member_name COLON nonvoid_type_descriptor (EQUAL lit=literal)?
    ( ({_input.LA(1) == ANNOTATION_DIRECTIVE}? annotation { annotations.add($annotation.n); })*
      ( END_FIELD_DIRECTIVE
        { $n = ast(I_FIELD, _localctx.start, $member_name.n, $access_or_restriction_list.n,
                   ast(I_FIELD_TYPE, _localctx.start, $nonvoid_type_descriptor.n),
                   _localctx.lit != null ? ast(I_FIELD_INITIAL_VALUE, _localctx.start, $lit.n) : null,
                   buildTree(I_ANNOTATIONS, annotations)); }
      | { classAnnotations.addAll(annotations); }
        { $n = ast(I_FIELD, _localctx.start, $member_name.n, $access_or_restriction_list.n,
                   ast(I_FIELD_TYPE, _localctx.start, $nonvoid_type_descriptor.n),
                   _localctx.lit != null ? ast(I_FIELD_INITIAL_VALUE, _localctx.start, $lit.n) : null,
                   buildTree(I_ANNOTATIONS, new ArrayList<AstNode>())); }
      )
    );

method returns[AstNode n]
  : METHOD_DIRECTIVE access_or_restriction_list member_name method_prototype statements_and_directives
    END_METHOD_DIRECTIVE
    { $n = ast(I_METHOD, _localctx.start, $member_name.n, $method_prototype.n,
               $access_or_restriction_list.n, $statements_and_directives.n); };

statements_and_directives returns[AstNode n]
  @init
  { statementsHasRegistersDirective = false;
    statementsMethodAnnotations = new ArrayList<AstNode>();
    List<AstNode> methodAnnotations = statementsMethodAnnotations;
    List<AstNode> orderedItems = new ArrayList<AstNode>();
    List<AstNode> catches = new ArrayList<AstNode>();
    List<AstNode> catchAlls = new ArrayList<AstNode>();
    List<AstNode> parameters = new ArrayList<AstNode>();
    AstNode registers = null;
  }
  : ( ordered_method_item { orderedItems.add($ordered_method_item.n); }
    | registers_directive { registers = $registers_directive.n; }
    | catch_directive { catches.add($catch_directive.n); }
    | catchall_directive { catchAlls.add($catchall_directive.n); }
    | parameter_directive { parameters.add($parameter_directive.n); }
    | annotation { methodAnnotations.add($annotation.n); }
    )*
    { catches.addAll(catchAlls);
      $n = flat(registers,
                ast(I_ORDERED_METHOD_ITEMS, _localctx.start, orderedItems),
                ast(I_CATCHES, _localctx.start, catches),
                ast(I_PARAMETERS, _localctx.start, parameters),
                buildTree(I_ANNOTATIONS, methodAnnotations)); };

/* Method items whose order/location is important */
ordered_method_item returns[AstNode n]
  : label { $n = $label.n; }
  | instruction { $n = $instruction.n; }
  | debug_directive { $n = $debug_directive.n; };

registers_directive returns[AstNode n]
  : (
      directive=REGISTERS_DIRECTIVE regCount=integral_literal
        { $n = ast(I_REGISTERS, $directive, $regCount.n); }
    | directive=LOCALS_DIRECTIVE regCount2=integral_literal
        { $n = ast(I_LOCALS, $directive, $regCount2.n); }
    )
    {
      if (statementsHasRegistersDirective) {
        throw new SemanticException(_input, $directive, "There can only be a single .registers or .locals directive in a method");
      }
      statementsHasRegistersDirective = true;
    };

param_list_or_id returns[AstNode n]
  @init { List<AstNode> items = new ArrayList<AstNode>(); }
  : ( PARAM_LIST_OR_ID_PRIMITIVE_TYPE { items.add(AstNode.leaf($PARAM_LIST_OR_ID_PRIMITIVE_TYPE)); } )+
    { $n = flat(items); };

/*identifiers are much more general than most languages. Any of the below can either be
the indicated type OR an identifier, depending on the context*/
simple_name returns[AstNode n]
  : SIMPLE_NAME { $n = AstNode.leaf($SIMPLE_NAME); }
  | ACCESS_SPEC { $n = retype($ACCESS_SPEC, SIMPLE_NAME); }
  | HIDDENAPI_RESTRICTION { $n = retype($HIDDENAPI_RESTRICTION, SIMPLE_NAME); }
  | VERIFICATION_ERROR_TYPE { $n = retype($VERIFICATION_ERROR_TYPE, SIMPLE_NAME); }
  | POSITIVE_INTEGER_LITERAL { $n = retype($POSITIVE_INTEGER_LITERAL, SIMPLE_NAME); }
  | NEGATIVE_INTEGER_LITERAL { $n = retype($NEGATIVE_INTEGER_LITERAL, SIMPLE_NAME); }
  | FLOAT_LITERAL_OR_ID { $n = retype($FLOAT_LITERAL_OR_ID, SIMPLE_NAME); }
  | DOUBLE_LITERAL_OR_ID { $n = retype($DOUBLE_LITERAL_OR_ID, SIMPLE_NAME); }
  | BOOL_LITERAL { $n = retype($BOOL_LITERAL, SIMPLE_NAME); }
  | NULL_LITERAL { $n = retype($NULL_LITERAL, SIMPLE_NAME); }
  | REGISTER { $n = retype($REGISTER, SIMPLE_NAME); }
  | plid=param_list_or_id { $n = retypedText(SIMPLE_NAME, _localctx.start, textOf(_localctx.plid)); }
  | PRIMITIVE_TYPE { $n = retype($PRIMITIVE_TYPE, SIMPLE_NAME); }
  | VOID_TYPE { $n = retype($VOID_TYPE, SIMPLE_NAME); }
  | ANNOTATION_VISIBILITY { $n = retype($ANNOTATION_VISIBILITY, SIMPLE_NAME); }
  | METHOD_HANDLE_TYPE_FIELD { $n = AstNode.leaf($METHOD_HANDLE_TYPE_FIELD); }
  | METHOD_HANDLE_TYPE_METHOD { $n = AstNode.leaf($METHOD_HANDLE_TYPE_METHOD); }
  | INSTRUCTION_FORMAT10t { $n = retype($INSTRUCTION_FORMAT10t, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT10x { $n = retype($INSTRUCTION_FORMAT10x, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT10x_ODEX { $n = retype($INSTRUCTION_FORMAT10x_ODEX, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT11x { $n = retype($INSTRUCTION_FORMAT11x, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT12x_OR_ID { $n = retype($INSTRUCTION_FORMAT12x_OR_ID, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_FIELD { $n = retype($INSTRUCTION_FORMAT21c_FIELD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_FIELD_ODEX { $n = retype($INSTRUCTION_FORMAT21c_FIELD_ODEX, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_METHOD_HANDLE { $n = retype($INSTRUCTION_FORMAT21c_METHOD_HANDLE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_METHOD_TYPE { $n = retype($INSTRUCTION_FORMAT21c_METHOD_TYPE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_STRING { $n = retype($INSTRUCTION_FORMAT21c_STRING, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21c_TYPE { $n = retype($INSTRUCTION_FORMAT21c_TYPE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT21t { $n = retype($INSTRUCTION_FORMAT21t, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22c_FIELD { $n = retype($INSTRUCTION_FORMAT22c_FIELD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22c_FIELD_ODEX { $n = retype($INSTRUCTION_FORMAT22c_FIELD_ODEX, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22c_TYPE { $n = retype($INSTRUCTION_FORMAT22c_TYPE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22cs_FIELD { $n = retype($INSTRUCTION_FORMAT22cs_FIELD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22s_OR_ID { $n = retype($INSTRUCTION_FORMAT22s_OR_ID, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT22t { $n = retype($INSTRUCTION_FORMAT22t, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT23x { $n = retype($INSTRUCTION_FORMAT23x, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT31i_OR_ID { $n = retype($INSTRUCTION_FORMAT31i_OR_ID, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT31t { $n = retype($INSTRUCTION_FORMAT31t, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35c_CALL_SITE { $n = retype($INSTRUCTION_FORMAT35c_CALL_SITE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35c_METHOD { $n = retype($INSTRUCTION_FORMAT35c_METHOD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35c_METHOD_ODEX { $n = retype($INSTRUCTION_FORMAT35c_METHOD_ODEX, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE { $n = retype($INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35c_TYPE { $n = retype($INSTRUCTION_FORMAT35c_TYPE, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35mi_METHOD { $n = retype($INSTRUCTION_FORMAT35mi_METHOD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT35ms_METHOD { $n = retype($INSTRUCTION_FORMAT35ms_METHOD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT45cc_METHOD { $n = retype($INSTRUCTION_FORMAT45cc_METHOD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT4rcc_METHOD { $n = retype($INSTRUCTION_FORMAT4rcc_METHOD, SIMPLE_NAME); }
  | INSTRUCTION_FORMAT51l { $n = retype($INSTRUCTION_FORMAT51l, SIMPLE_NAME); };

member_name returns[AstNode n]
  : simple_name { $n = $simple_name.n; }
  | MEMBER_NAME { $n = retype($MEMBER_NAME, SIMPLE_NAME); };

method_prototype returns[AstNode n]
  : OPEN_PAREN param_list CLOSE_PAREN type_descriptor
    { $n = ast(I_METHOD_PROTOTYPE, _localctx.start,
               ast(I_METHOD_RETURN_TYPE, _localctx.start, $type_descriptor.n),
               $param_list.n); };

param_list_or_id_primitive_type returns[AstNode n]
  : PARAM_LIST_OR_ID_PRIMITIVE_TYPE { $n = retype($PARAM_LIST_OR_ID_PRIMITIVE_TYPE, PRIMITIVE_TYPE); };

param_list returns[AstNode n]
  @init { List<AstNode> items = new ArrayList<AstNode>(); }
  : ( param_list_or_id_primitive_type { items.add($param_list_or_id_primitive_type.n); } )+
      { $n = flat(items); }
  | ( nonvoid_type_descriptor { items.add($nonvoid_type_descriptor.n); } )*
      { $n = flat(items); };

array_descriptor returns[AstNode n]
  : ARRAY_TYPE_PREFIX PRIMITIVE_TYPE
    { $n = flat(AstNode.leaf($ARRAY_TYPE_PREFIX), AstNode.leaf($PRIMITIVE_TYPE)); }
  | ARRAY_TYPE_PREFIX CLASS_DESCRIPTOR
    { $n = flat(AstNode.leaf($ARRAY_TYPE_PREFIX), AstNode.leaf($CLASS_DESCRIPTOR)); };

type_descriptor returns[AstNode n]
  : VOID_TYPE { $n = AstNode.leaf($VOID_TYPE); }
  | PRIMITIVE_TYPE { $n = AstNode.leaf($PRIMITIVE_TYPE); }
  | CLASS_DESCRIPTOR { $n = AstNode.leaf($CLASS_DESCRIPTOR); }
  | array_descriptor { $n = $array_descriptor.n; };

nonvoid_type_descriptor returns[AstNode n]
  : PRIMITIVE_TYPE { $n = AstNode.leaf($PRIMITIVE_TYPE); }
  | CLASS_DESCRIPTOR { $n = AstNode.leaf($CLASS_DESCRIPTOR); }
  | array_descriptor { $n = $array_descriptor.n; };

reference_type_descriptor returns[AstNode n]
  : CLASS_DESCRIPTOR { $n = AstNode.leaf($CLASS_DESCRIPTOR); }
  | array_descriptor { $n = $array_descriptor.n; };

integer_literal returns[AstNode n]
  : POSITIVE_INTEGER_LITERAL { $n = retype($POSITIVE_INTEGER_LITERAL, INTEGER_LITERAL); }
  | NEGATIVE_INTEGER_LITERAL { $n = retype($NEGATIVE_INTEGER_LITERAL, INTEGER_LITERAL); };

float_literal returns[AstNode n]
  : FLOAT_LITERAL_OR_ID { $n = retype($FLOAT_LITERAL_OR_ID, FLOAT_LITERAL); }
  | FLOAT_LITERAL { $n = AstNode.leaf($FLOAT_LITERAL); };

double_literal returns[AstNode n]
  : DOUBLE_LITERAL_OR_ID { $n = retype($DOUBLE_LITERAL_OR_ID, DOUBLE_LITERAL); }
  | DOUBLE_LITERAL { $n = AstNode.leaf($DOUBLE_LITERAL); };

literal returns[AstNode n]
  : LONG_LITERAL { $n = AstNode.leaf($LONG_LITERAL); }
  | integer_literal { $n = $integer_literal.n; }
  | SHORT_LITERAL { $n = AstNode.leaf($SHORT_LITERAL); }
  | BYTE_LITERAL { $n = AstNode.leaf($BYTE_LITERAL); }
  | float_literal { $n = $float_literal.n; }
  | double_literal { $n = $double_literal.n; }
  | CHAR_LITERAL { $n = AstNode.leaf($CHAR_LITERAL); }
  | STRING_LITERAL { $n = AstNode.leaf($STRING_LITERAL); }
  | BOOL_LITERAL { $n = AstNode.leaf($BOOL_LITERAL); }
  | NULL_LITERAL { $n = AstNode.leaf($NULL_LITERAL); }
  | array_literal { $n = $array_literal.n; }
  | subannotation { $n = $subannotation.n; }
  | type_field_method_literal { $n = $type_field_method_literal.n; }
  | enum_literal { $n = $enum_literal.n; }
  | method_handle_literal { $n = $method_handle_literal.n; }
  | method_prototype { $n = $method_prototype.n; };

parsed_integer_literal returns[int value, AstNode n]
  : il=integer_literal
    { $value = LiteralTools.parseInt(textOf(_localctx.il)); $n = $il.n; };

integral_literal returns[AstNode n]
  : LONG_LITERAL { $n = AstNode.leaf($LONG_LITERAL); }
  | integer_literal { $n = $integer_literal.n; }
  | SHORT_LITERAL { $n = AstNode.leaf($SHORT_LITERAL); }
  | CHAR_LITERAL { $n = AstNode.leaf($CHAR_LITERAL); }
  | BYTE_LITERAL { $n = AstNode.leaf($BYTE_LITERAL); };

fixed_32bit_literal returns[AstNode n]
  : LONG_LITERAL { $n = AstNode.leaf($LONG_LITERAL); }
  | integer_literal { $n = $integer_literal.n; }
  | SHORT_LITERAL { $n = AstNode.leaf($SHORT_LITERAL); }
  | BYTE_LITERAL { $n = AstNode.leaf($BYTE_LITERAL); }
  | float_literal { $n = $float_literal.n; }
  | CHAR_LITERAL { $n = AstNode.leaf($CHAR_LITERAL); }
  | BOOL_LITERAL { $n = AstNode.leaf($BOOL_LITERAL); };

fixed_literal returns[AstNode n]
  : integer_literal { $n = $integer_literal.n; }
  | LONG_LITERAL { $n = AstNode.leaf($LONG_LITERAL); }
  | SHORT_LITERAL { $n = AstNode.leaf($SHORT_LITERAL); }
  | BYTE_LITERAL { $n = AstNode.leaf($BYTE_LITERAL); }
  | float_literal { $n = $float_literal.n; }
  | double_literal { $n = $double_literal.n; }
  | CHAR_LITERAL { $n = AstNode.leaf($CHAR_LITERAL); }
  | BOOL_LITERAL { $n = AstNode.leaf($BOOL_LITERAL); };

array_literal returns[AstNode n]
  @init { List<AstNode> literals = new ArrayList<AstNode>(); }
  : OPEN_BRACE ( literal { literals.add($literal.n); } ( COMMA literal { literals.add($literal.n); } )* )? CLOSE_BRACE
    { $n = ast(I_ENCODED_ARRAY, _localctx.start, literals); };

annotation_element returns[AstNode n]
  : simple_name EQUAL literal
    { $n = ast(I_ANNOTATION_ELEMENT, _localctx.start, $simple_name.n, $literal.n); };

annotation returns[AstNode n]
  : ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY CLASS_DESCRIPTOR els+=annotation_element* END_ANNOTATION_DIRECTIVE
    { List<AstNode> elements = new ArrayList<AstNode>();
      for (Annotation_elementContext ctx : $els) {
        elements.add(ctx.n);
      }
      $n = ast(I_ANNOTATION, _localctx.start, AstNode.leaf($ANNOTATION_VISIBILITY),
               ast(I_SUBANNOTATION, _localctx.start, $CLASS_DESCRIPTOR, elements)); };

subannotation returns[AstNode n]
  : SUBANNOTATION_DIRECTIVE CLASS_DESCRIPTOR els+=annotation_element* END_SUBANNOTATION_DIRECTIVE
    { List<AstNode> elements = new ArrayList<AstNode>();
      for (Annotation_elementContext ctx : $els) {
        elements.add(ctx.n);
      }
      $n = ast(I_SUBANNOTATION, _localctx.start, $CLASS_DESCRIPTOR, elements); };

enum_literal returns[AstNode n]
  : ENUM_DIRECTIVE field_reference
    { $n = ast(I_ENCODED_ENUM, _localctx.start, $field_reference.n); };

type_field_method_literal returns[AstNode n]
  : reference_type_descriptor { $n = $reference_type_descriptor.n; }
  | ( (rt=reference_type_descriptor ARROW)?
      ( member_name COLON nonvoid_type_descriptor
          { $n = ast(I_ENCODED_FIELD, _localctx.start,
                     _localctx.rt != null ? $rt.n : null,
                     $member_name.n, $nonvoid_type_descriptor.n); }
      | member_name method_prototype
          { $n = ast(I_ENCODED_METHOD, _localctx.start,
                     _localctx.rt != null ? $rt.n : null,
                     $member_name.n, $method_prototype.n); }
      )
    )
  | PRIMITIVE_TYPE { $n = AstNode.leaf($PRIMITIVE_TYPE); }
  | VOID_TYPE { $n = AstNode.leaf($VOID_TYPE); };

call_site_reference returns[AstNode n]
  @init { List<AstNode> extraArguments = new ArrayList<AstNode>(); }
  : simple_name OPEN_PAREN STRING_LITERAL COMMA method_prototype (COMMA literal { extraArguments.add($literal.n); })* CLOSE_PAREN AT method_reference
    { $n = ast(I_CALL_SITE_REFERENCE, _localctx.start, $simple_name.n, $STRING_LITERAL, $method_prototype.n,
               ast(I_CALL_SITE_EXTRA_ARGUMENTS, _localctx.start, extraArguments), $method_reference.n); };

method_handle_reference returns[AstNode n]
  : METHOD_HANDLE_TYPE_FIELD AT field_reference
      { $n = flat(AstNode.leaf($METHOD_HANDLE_TYPE_FIELD), $field_reference.n); }
  | METHOD_HANDLE_TYPE_METHOD AT method_reference
      { $n = flat(AstNode.leaf($METHOD_HANDLE_TYPE_METHOD), $method_reference.n); }
  | INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE AT method_reference
      { $n = flat(AstNode.leaf($INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE), $method_reference.n); };

method_handle_literal returns[AstNode n]
  : method_handle_reference
    { $n = ast(I_ENCODED_METHOD_HANDLE, _localctx.start, $method_handle_reference.n); };

method_reference returns[AstNode n]
  : (rt=reference_type_descriptor ARROW)? member_name method_prototype
    { $n = flat(_localctx.rt != null ? $rt.n : null,
                $member_name.n, $method_prototype.n); };

field_reference returns[AstNode n]
  : (rt=reference_type_descriptor ARROW)? member_name COLON nonvoid_type_descriptor
    { $n = flat(_localctx.rt != null ? $rt.n : null,
                $member_name.n, $nonvoid_type_descriptor.n); };

label returns[AstNode n]
  : COLON simple_name { $n = ast(I_LABEL, $COLON, $simple_name.n); };

label_ref returns[AstNode n]
  : COLON simple_name { $n = $simple_name.n; };

register_list returns[AstNode n]
  @init { List<AstNode> registers = new ArrayList<AstNode>(); }
  : REGISTER { registers.add(AstNode.leaf($REGISTER)); } (COMMA REGISTER { registers.add(AstNode.leaf($REGISTER)); })*
      { $n = ast(I_REGISTER_LIST, _localctx.start, registers); }
  | { $n = ast(I_REGISTER_LIST, _localctx.start); };

register_range returns[AstNode n]
  : (startreg=REGISTER (DOTDOT endreg=REGISTER)?)?
    { $n = ast(I_REGISTER_RANGE, _localctx.start, $startreg, $endreg); };

verification_error_reference returns[AstNode n]
  : CLASS_DESCRIPTOR { $n = AstNode.leaf($CLASS_DESCRIPTOR); }
  | field_reference { $n = $field_reference.n; }
  | method_reference { $n = $method_reference.n; };

catch_directive returns[AstNode n]
  : CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE from=label_ref DOTDOT to=label_ref CLOSE_BRACE using=label_ref
    { $n = ast(I_CATCH, _localctx.start, $nonvoid_type_descriptor.n, $from.n, $to.n, $using.n); };

catchall_directive returns[AstNode n]
  : CATCHALL_DIRECTIVE OPEN_BRACE from=label_ref DOTDOT to=label_ref CLOSE_BRACE using=label_ref
    { $n = ast(I_CATCHALL, _localctx.start, $from.n, $to.n, $using.n); };

parameter_directive returns[AstNode n]
  @init { List<AstNode> annotations = new ArrayList<AstNode>(); }
  : PARAMETER_DIRECTIVE REGISTER (COMMA STRING_LITERAL)?
    ({_input.LA(1) == ANNOTATION_DIRECTIVE}? annotation { annotations.add($annotation.n); })*
    ( END_PARAMETER_DIRECTIVE
      { $n = ast(I_PARAMETER, _localctx.start, AstNode.leaf($REGISTER), $STRING_LITERAL,
                 buildTree(I_ANNOTATIONS, annotations)); }
    | { statementsMethodAnnotations.addAll(annotations); }
      { $n = ast(I_PARAMETER, _localctx.start, AstNode.leaf($REGISTER), $STRING_LITERAL,
                 buildTree(I_ANNOTATIONS, new ArrayList<AstNode>())); }
    );

debug_directive returns[AstNode n]
  : line_directive { $n = $line_directive.n; }
  | local_directive { $n = $local_directive.n; }
  | end_local_directive { $n = $end_local_directive.n; }
  | restart_local_directive { $n = $restart_local_directive.n; }
  | prologue_directive { $n = $prologue_directive.n; }
  | epilogue_directive { $n = $epilogue_directive.n; }
  | source_directive { $n = $source_directive.n; };

line_directive returns[AstNode n]
  : LINE_DIRECTIVE integral_literal
    { $n = ast(I_LINE, _localctx.start, $integral_literal.n); };

local_directive returns[AstNode n]
  : LOCAL_DIRECTIVE REGISTER (COMMA (NULL_LITERAL | name=STRING_LITERAL) COLON (VOID_TYPE | nvtd=nonvoid_type_descriptor)
                              (COMMA signature=STRING_LITERAL)? )?
    { $n = ast(I_LOCAL, _localctx.start, AstNode.leaf($REGISTER),
               $NULL_LITERAL != null ? AstNode.leaf($NULL_LITERAL) : null,
               $name,
               _localctx.nvtd != null ? $nvtd.n : null,
               $signature); };

end_local_directive returns[AstNode n]
  : END_LOCAL_DIRECTIVE REGISTER
    { $n = ast(I_END_LOCAL, _localctx.start, $REGISTER); };

restart_local_directive returns[AstNode n]
  : RESTART_LOCAL_DIRECTIVE REGISTER
    { $n = ast(I_RESTART_LOCAL, _localctx.start, $REGISTER); };

prologue_directive returns[AstNode n]
  : PROLOGUE_DIRECTIVE
    { $n = ast(I_PROLOGUE, _localctx.start); };

epilogue_directive returns[AstNode n]
  : EPILOGUE_DIRECTIVE
    { $n = ast(I_EPILOGUE, _localctx.start); };

source_directive returns[AstNode n]
  : SOURCE_DIRECTIVE STRING_LITERAL?
    { $n = ast(I_SOURCE, _localctx.start, $STRING_LITERAL); };

instruction_format12x returns[AstNode n]
  : INSTRUCTION_FORMAT12x { $n = AstNode.leaf($INSTRUCTION_FORMAT12x); }
  | INSTRUCTION_FORMAT12x_OR_ID { $n = retype($INSTRUCTION_FORMAT12x_OR_ID, INSTRUCTION_FORMAT12x); };

instruction_format22s returns[AstNode n]
  : INSTRUCTION_FORMAT22s { $n = AstNode.leaf($INSTRUCTION_FORMAT22s); }
  | INSTRUCTION_FORMAT22s_OR_ID { $n = retype($INSTRUCTION_FORMAT22s_OR_ID, INSTRUCTION_FORMAT22s); };

instruction_format31i returns[AstNode n]
  : INSTRUCTION_FORMAT31i { $n = AstNode.leaf($INSTRUCTION_FORMAT31i); }
  | INSTRUCTION_FORMAT31i_OR_ID { $n = retype($INSTRUCTION_FORMAT31i_OR_ID, INSTRUCTION_FORMAT31i); };

instruction_format35c_method returns[AstNode n]
  : INSTRUCTION_FORMAT35c_METHOD { $n = AstNode.leaf($INSTRUCTION_FORMAT35c_METHOD); }
  | INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE { $n = retype($INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE, INSTRUCTION_FORMAT35c_METHOD); };

instruction returns[AstNode n]
  : insn_format10t { $n = $insn_format10t.n; }
  | insn_format10x { $n = $insn_format10x.n; }
  | insn_format10x_odex { $n = $insn_format10x_odex.n; }
  | insn_format11n { $n = $insn_format11n.n; }
  | insn_format11x { $n = $insn_format11x.n; }
  | insn_format12x { $n = $insn_format12x.n; }
  | insn_format20bc { $n = $insn_format20bc.n; }
  | insn_format20t { $n = $insn_format20t.n; }
  | insn_format21c_field { $n = $insn_format21c_field.n; }
  | insn_format21c_field_odex { $n = $insn_format21c_field_odex.n; }
  | insn_format21c_method_handle { $n = $insn_format21c_method_handle.n; }
  | insn_format21c_method_type { $n = $insn_format21c_method_type.n; }
  | insn_format21c_string { $n = $insn_format21c_string.n; }
  | insn_format21c_type { $n = $insn_format21c_type.n; }
  | insn_format21ih { $n = $insn_format21ih.n; }
  | insn_format21lh { $n = $insn_format21lh.n; }
  | insn_format21s { $n = $insn_format21s.n; }
  | insn_format21t { $n = $insn_format21t.n; }
  | insn_format22b { $n = $insn_format22b.n; }
  | insn_format22c_field { $n = $insn_format22c_field.n; }
  | insn_format22c_field_odex { $n = $insn_format22c_field_odex.n; }
  | insn_format22c_type { $n = $insn_format22c_type.n; }
  | insn_format22cs_field { $n = $insn_format22cs_field.n; }
  | insn_format22s { $n = $insn_format22s.n; }
  | insn_format22t { $n = $insn_format22t.n; }
  | insn_format22x { $n = $insn_format22x.n; }
  | insn_format23x { $n = $insn_format23x.n; }
  | insn_format30t { $n = $insn_format30t.n; }
  | insn_format31c { $n = $insn_format31c.n; }
  | insn_format31i { $n = $insn_format31i.n; }
  | insn_format31t { $n = $insn_format31t.n; }
  | insn_format32x { $n = $insn_format32x.n; }
  | insn_format35c_call_site { $n = $insn_format35c_call_site.n; }
  | insn_format35c_method { $n = $insn_format35c_method.n; }
  | insn_format35c_type { $n = $insn_format35c_type.n; }
  | insn_format35c_method_odex { $n = $insn_format35c_method_odex.n; }
  | insn_format35mi_method { $n = $insn_format35mi_method.n; }
  | insn_format35ms_method { $n = $insn_format35ms_method.n; }
  | insn_format3rc_call_site { $n = $insn_format3rc_call_site.n; }
  | insn_format3rc_method { $n = $insn_format3rc_method.n; }
  | insn_format3rc_method_odex { $n = $insn_format3rc_method_odex.n; }
  | insn_format3rc_type { $n = $insn_format3rc_type.n; }
  | insn_format3rmi_method { $n = $insn_format3rmi_method.n; }
  | insn_format3rms_method { $n = $insn_format3rms_method.n; }
  | insn_format45cc_method { $n = $insn_format45cc_method.n; }
  | insn_format4rcc_method { $n = $insn_format4rcc_method.n; }
  | insn_format51l { $n = $insn_format51l.n; }
  | insn_array_data_directive { $n = $insn_array_data_directive.n; }
  | insn_packed_switch_directive { $n = $insn_packed_switch_directive.n; }
  | insn_sparse_switch_directive { $n = $insn_sparse_switch_directive.n; };

insn_format10t returns[AstNode n]
  : INSTRUCTION_FORMAT10t label_ref
    { $n = ast(I_STATEMENT_FORMAT10t, _localctx.start, $INSTRUCTION_FORMAT10t, $label_ref.n); };

insn_format10x returns[AstNode n]
  : INSTRUCTION_FORMAT10x
    { $n = ast(I_STATEMENT_FORMAT10x, _localctx.start, $INSTRUCTION_FORMAT10x); };

insn_format10x_odex returns[AstNode n]
  : INSTRUCTION_FORMAT10x_ODEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT10x_ODEX.text); };

insn_format11n returns[AstNode n]
  : INSTRUCTION_FORMAT11n REGISTER COMMA integral_literal
    { $n = ast(I_STATEMENT_FORMAT11n, _localctx.start, $INSTRUCTION_FORMAT11n, $REGISTER, $integral_literal.n); };

insn_format11x returns[AstNode n]
  : INSTRUCTION_FORMAT11x REGISTER
    { $n = ast(I_STATEMENT_FORMAT11x, _localctx.start, $INSTRUCTION_FORMAT11x, $REGISTER); };

insn_format12x returns[AstNode n]
  : instruction_format12x r1=REGISTER COMMA r2=REGISTER
    { $n = ast(I_STATEMENT_FORMAT12x, _localctx.start, $instruction_format12x.n, $r1, $r2); };

insn_format20bc returns[AstNode n]
  : INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT20bc.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT20bc.text);
      }
      $n = ast(I_STATEMENT_FORMAT20bc, _localctx.start, $INSTRUCTION_FORMAT20bc, $VERIFICATION_ERROR_TYPE, $verification_error_reference.n);
    };

insn_format20t returns[AstNode n]
  : INSTRUCTION_FORMAT20t label_ref
    { $n = ast(I_STATEMENT_FORMAT20t, _localctx.start, $INSTRUCTION_FORMAT20t, $label_ref.n); };

insn_format21c_field returns[AstNode n]
  : INSTRUCTION_FORMAT21c_FIELD r1=REGISTER COMMA field_reference
    { $n = ast(I_STATEMENT_FORMAT21c_FIELD, _localctx.start, $INSTRUCTION_FORMAT21c_FIELD, $r1, $field_reference.n); };

insn_format21c_field_odex returns[AstNode n]
  : INSTRUCTION_FORMAT21c_FIELD_ODEX r1=REGISTER COMMA field_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT21c_FIELD_ODEX.text);
      }
      $n = ast(I_STATEMENT_FORMAT21c_FIELD, _localctx.start, $INSTRUCTION_FORMAT21c_FIELD_ODEX, $r1, $field_reference.n);
    };

insn_format21c_method_handle returns[AstNode n]
  : INSTRUCTION_FORMAT21c_METHOD_HANDLE r1=REGISTER COMMA method_handle_reference
    { $n = ast(I_STATEMENT_FORMAT21c_METHOD_HANDLE, _localctx.start, $INSTRUCTION_FORMAT21c_METHOD_HANDLE, $r1, $method_handle_reference.n); };

insn_format21c_method_type returns[AstNode n]
  : INSTRUCTION_FORMAT21c_METHOD_TYPE r1=REGISTER COMMA method_prototype
    { $n = ast(I_STATEMENT_FORMAT21c_METHOD_TYPE, _localctx.start, $INSTRUCTION_FORMAT21c_METHOD_TYPE, $r1, $method_prototype.n); };

insn_format21c_string returns[AstNode n]
  : INSTRUCTION_FORMAT21c_STRING r1=REGISTER COMMA STRING_LITERAL
    { $n = ast(I_STATEMENT_FORMAT21c_STRING, _localctx.start, $INSTRUCTION_FORMAT21c_STRING, $r1, $STRING_LITERAL); };

insn_format21c_type returns[AstNode n]
  : INSTRUCTION_FORMAT21c_TYPE r1=REGISTER COMMA nonvoid_type_descriptor
    { $n = ast(I_STATEMENT_FORMAT21c_TYPE, _localctx.start, $INSTRUCTION_FORMAT21c_TYPE, $r1, $nonvoid_type_descriptor.n); };

insn_format21ih returns[AstNode n]
  : INSTRUCTION_FORMAT21ih r1=REGISTER COMMA fixed_32bit_literal
    { $n = ast(I_STATEMENT_FORMAT21ih, _localctx.start, $INSTRUCTION_FORMAT21ih, $r1, $fixed_32bit_literal.n); };

insn_format21lh returns[AstNode n]
  : INSTRUCTION_FORMAT21lh r1=REGISTER COMMA fixed_32bit_literal
    { $n = ast(I_STATEMENT_FORMAT21lh, _localctx.start, $INSTRUCTION_FORMAT21lh, $r1, $fixed_32bit_literal.n); };

insn_format21s returns[AstNode n]
  : INSTRUCTION_FORMAT21s r1=REGISTER COMMA integral_literal
    { $n = ast(I_STATEMENT_FORMAT21s, _localctx.start, $INSTRUCTION_FORMAT21s, $r1, $integral_literal.n); };

insn_format21t returns[AstNode n]
  : INSTRUCTION_FORMAT21t r1=REGISTER COMMA label_ref
    { $n = ast(I_STATEMENT_FORMAT21t, _localctx.start, $INSTRUCTION_FORMAT21t, $r1, $label_ref.n); };

insn_format22b returns[AstNode n]
  : INSTRUCTION_FORMAT22b r1=REGISTER COMMA r2=REGISTER COMMA integral_literal
    { $n = ast(I_STATEMENT_FORMAT22b, _localctx.start, $INSTRUCTION_FORMAT22b, $r1, $r2, $integral_literal.n); };

insn_format22c_field returns[AstNode n]
  : INSTRUCTION_FORMAT22c_FIELD r1=REGISTER COMMA r2=REGISTER COMMA field_reference
    { $n = ast(I_STATEMENT_FORMAT22c_FIELD, _localctx.start, $INSTRUCTION_FORMAT22c_FIELD, $r1, $r2, $field_reference.n); };

insn_format22c_field_odex returns[AstNode n]
  : INSTRUCTION_FORMAT22c_FIELD_ODEX r1=REGISTER COMMA r2=REGISTER COMMA field_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT22c_FIELD_ODEX.text);
      }
      $n = ast(I_STATEMENT_FORMAT22c_FIELD, _localctx.start, $INSTRUCTION_FORMAT22c_FIELD_ODEX, $r1, $r2, $field_reference.n);
    };

insn_format22c_type returns[AstNode n]
  : INSTRUCTION_FORMAT22c_TYPE r1=REGISTER COMMA r2=REGISTER COMMA nonvoid_type_descriptor
    { $n = ast(I_STATEMENT_FORMAT22c_TYPE, _localctx.start, $INSTRUCTION_FORMAT22c_TYPE, $r1, $r2, $nonvoid_type_descriptor.n); };

insn_format22cs_field returns[AstNode n]
  : INSTRUCTION_FORMAT22cs_FIELD r1=REGISTER COMMA r2=REGISTER COMMA FIELD_OFFSET
    { throwOdexedInstructionException($INSTRUCTION_FORMAT22cs_FIELD.text); };

insn_format22s returns[AstNode n]
  : instruction_format22s r1=REGISTER COMMA r2=REGISTER COMMA integral_literal
    { $n = ast(I_STATEMENT_FORMAT22s, _localctx.start, $instruction_format22s.n, $r1, $r2, $integral_literal.n); };

insn_format22t returns[AstNode n]
  : INSTRUCTION_FORMAT22t r1=REGISTER COMMA r2=REGISTER COMMA label_ref
    { $n = ast(I_STATEMENT_FORMAT22t, _localctx.start, $INSTRUCTION_FORMAT22t, $r1, $r2, $label_ref.n); };

insn_format22x returns[AstNode n]
  : INSTRUCTION_FORMAT22x r1=REGISTER COMMA r2=REGISTER
    { $n = ast(I_STATEMENT_FORMAT22x, _localctx.start, $INSTRUCTION_FORMAT22x, $r1, $r2); };

insn_format23x returns[AstNode n]
  : INSTRUCTION_FORMAT23x r1=REGISTER COMMA r2=REGISTER COMMA r3=REGISTER
    { $n = ast(I_STATEMENT_FORMAT23x, _localctx.start, $INSTRUCTION_FORMAT23x, $r1, $r2, $r3); };

insn_format30t returns[AstNode n]
  : INSTRUCTION_FORMAT30t label_ref
    { $n = ast(I_STATEMENT_FORMAT30t, _localctx.start, $INSTRUCTION_FORMAT30t, $label_ref.n); };

insn_format31c returns[AstNode n]
  : INSTRUCTION_FORMAT31c r1=REGISTER COMMA STRING_LITERAL
    { $n = ast(I_STATEMENT_FORMAT31c, _localctx.start, $INSTRUCTION_FORMAT31c, $r1, $STRING_LITERAL); };

insn_format31i returns[AstNode n]
  : instruction_format31i r1=REGISTER COMMA fixed_32bit_literal
    { $n = ast(I_STATEMENT_FORMAT31i, _localctx.start, $instruction_format31i.n, $r1, $fixed_32bit_literal.n); };

insn_format31t returns[AstNode n]
  : INSTRUCTION_FORMAT31t r1=REGISTER COMMA label_ref
    { $n = ast(I_STATEMENT_FORMAT31t, _localctx.start, $INSTRUCTION_FORMAT31t, $r1, $label_ref.n); };

insn_format32x returns[AstNode n]
  : INSTRUCTION_FORMAT32x r1=REGISTER COMMA r2=REGISTER
    { $n = ast(I_STATEMENT_FORMAT32x, _localctx.start, $INSTRUCTION_FORMAT32x, $r1, $r2); };

insn_format35c_call_site returns[AstNode n]
  : INSTRUCTION_FORMAT35c_CALL_SITE OPEN_BRACE register_list CLOSE_BRACE COMMA call_site_reference
    { $n = ast(I_STATEMENT_FORMAT35c_CALL_SITE, _localctx.start, $INSTRUCTION_FORMAT35c_CALL_SITE, $register_list.n, $call_site_reference.n); };

insn_format35c_method returns[AstNode n]
  : instruction_format35c_method OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
    { $n = ast(I_STATEMENT_FORMAT35c_METHOD, _localctx.start, $instruction_format35c_method.n, $register_list.n, $method_reference.n); };

insn_format35c_type returns[AstNode n]
  : INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor
    { $n = ast(I_STATEMENT_FORMAT35c_TYPE, _localctx.start, $INSTRUCTION_FORMAT35c_TYPE, $register_list.n, $nonvoid_type_descriptor.n); };

insn_format35c_method_odex returns[AstNode n]
  : INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35c_METHOD_ODEX.text); };

insn_format35mi_method returns[AstNode n]
  : INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35mi_METHOD.text); };

insn_format35ms_method returns[AstNode n]
  : INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35ms_METHOD.text); };

insn_format3rc_call_site returns[AstNode n]
  : INSTRUCTION_FORMAT3rc_CALL_SITE OPEN_BRACE register_range CLOSE_BRACE COMMA call_site_reference
    { $n = ast(I_STATEMENT_FORMAT3rc_CALL_SITE, _localctx.start, $INSTRUCTION_FORMAT3rc_CALL_SITE, $register_range.n, $call_site_reference.n); };

insn_format3rc_method returns[AstNode n]
  : INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA method_reference
    { $n = ast(I_STATEMENT_FORMAT3rc_METHOD, _localctx.start, $INSTRUCTION_FORMAT3rc_METHOD, $register_range.n, $method_reference.n); };

insn_format3rc_method_odex returns[AstNode n]
  : INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rc_METHOD_ODEX.text); };

insn_format3rc_type returns[AstNode n]
  : INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor
    { $n = ast(I_STATEMENT_FORMAT3rc_TYPE, _localctx.start, $INSTRUCTION_FORMAT3rc_TYPE, $register_range.n, $nonvoid_type_descriptor.n); };

insn_format3rmi_method returns[AstNode n]
  : INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rmi_METHOD.text); };

insn_format3rms_method returns[AstNode n]
  : INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rms_METHOD.text); };

insn_format45cc_method returns[AstNode n]
  : INSTRUCTION_FORMAT45cc_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference COMMA method_prototype
    { $n = ast(I_STATEMENT_FORMAT45cc_METHOD, _localctx.start, $INSTRUCTION_FORMAT45cc_METHOD, $register_list.n, $method_reference.n, $method_prototype.n); };

insn_format4rcc_method returns[AstNode n]
  : INSTRUCTION_FORMAT4rcc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA method_reference COMMA method_prototype
    { $n = ast(I_STATEMENT_FORMAT4rcc_METHOD, _localctx.start, $INSTRUCTION_FORMAT4rcc_METHOD, $register_range.n, $method_reference.n, $method_prototype.n); };

insn_format51l returns[AstNode n]
  : INSTRUCTION_FORMAT51l r1=REGISTER COMMA fixed_literal
    { $n = ast(I_STATEMENT_FORMAT51l, _localctx.start, $INSTRUCTION_FORMAT51l, $r1, $fixed_literal.n); };

insn_array_data_directive returns[AstNode n]
  @init { List<AstNode> elements = new ArrayList<AstNode>(); }
  : ARRAY_DATA_DIRECTIVE parsed_integer_literal
    {
        int elementWidth = $parsed_integer_literal.value;
        if (elementWidth != 4 && elementWidth != 8 && elementWidth != 1 && elementWidth != 2) {
            throw new SemanticException(_input, _localctx.start, "Invalid element width: %d. Must be 1, 2, 4 or 8", elementWidth);
        }
    }
    ( fixed_literal { elements.add($fixed_literal.n); } )* END_ARRAY_DATA_DIRECTIVE
    { $n = ast(I_STATEMENT_ARRAY_DATA, _localctx.start,
               ast(I_ARRAY_ELEMENT_SIZE, _localctx.start, $parsed_integer_literal.n),
               ast(I_ARRAY_ELEMENTS, _localctx.start, elements)); };

insn_packed_switch_directive returns[AstNode n]
  @init { List<AstNode> labels = new ArrayList<AstNode>(); }
  : PACKED_SWITCH_DIRECTIVE fixed_32bit_literal ( label_ref { labels.add($label_ref.n); } )* END_PACKED_SWITCH_DIRECTIVE
    { $n = ast(I_STATEMENT_PACKED_SWITCH, _localctx.start,
               ast(I_PACKED_SWITCH_START_KEY, _localctx.start, $fixed_32bit_literal.n),
               ast(I_PACKED_SWITCH_ELEMENTS, _localctx.start, labels)); };

insn_sparse_switch_directive returns[AstNode n]
  @init { List<AstNode> items = new ArrayList<AstNode>(); }
  : SPARSE_SWITCH_DIRECTIVE ( fixed_32bit_literal ARROW label_ref
      { items.add($fixed_32bit_literal.n); items.add($label_ref.n); } )* END_SPARSE_SWITCH_DIRECTIVE
    { $n = ast(I_STATEMENT_SPARSE_SWITCH, _localctx.start,
               ast(I_SPARSE_SWITCH_ELEMENTS, _localctx.start, items)); };
