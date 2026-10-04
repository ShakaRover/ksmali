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

// smali is assembled in a single pass: the parser rules below execute the semantic actions
// directly and build the DexBuilder contents as they recognise the source, so there is no AST
// and no second tree-grammar parse. The rule bodies mirror the historical smaliTreeWalker.g4.
parser grammar smaliParser;

options {
  tokenVocab=smaliLexer;
}

@parser::header {
package com.android.tools.smali.smali;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder;
import com.android.tools.smali.dexlib2.builder.SwitchLabelElement;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.AnnotationElement;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation;
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableCallSiteReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodHandleReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodProtoReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference;
import com.android.tools.smali.dexlib2.immutable.value.*;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.android.tools.smali.dexlib2.writer.builder.*;
import com.android.tools.smali.util.LinearSearch;
import java.util.*;
}

@parser::members {
  public static final int ERROR_CHANNEL = 100;

  public String classType;
  private boolean verboseErrors = false;
  private boolean allowOdex = false;
  private int apiLevel = 15;
  private Opcodes opcodes = Opcodes.forApi(apiLevel);
  private DexBuilder dexBuilder;

  // Replaces the ANTLR3 smali_file scope
  private boolean smaliFileHasClassSpec;
  private boolean smaliFileHasSuperSpec;
  private boolean smaliFileHasSourceSpec;
  private List<Annotation> classAnnotations = new ArrayList<Annotation>();

  // Replaces the ANTLR3 statements_and_directives scope
  private boolean statementsHasRegistersDirective;
  private List<Annotation> statementsMethodAnnotations;

  // Replaces the ANTLR3 'method' rule scope
  private boolean methodIsStatic;
  private int methodTotalRegisters;
  private int methodParameterRegisters;
  private MethodImplementationBuilder methodBuilder;
  private Token methodRegistersDirectiveStart;
  private boolean methodIsLocalsDirective;
  private Token smaliFileStartToken;

  public void setDexBuilder(DexBuilder dexBuilder) { this.dexBuilder = dexBuilder; }
  public void setVerboseErrors(boolean verboseErrors) { this.verboseErrors = verboseErrors; }
  public void setAllowOdex(boolean allowOdex) { this.allowOdex = allowOdex; }

  public void setApiLevel(int apiLevel) {
    this.opcodes = Opcodes.forApi(apiLevel);
    this.apiLevel = apiLevel;
  }

  public Opcodes getOpcodes() { return opcodes; }

  /** The symbolic name of a token type (unlike the deprecated tokenNames array, which prefers literals). */
  public static String tokenName(int type) {
    String name = VOCABULARY.getSymbolicName(type);
    if (name != null) {
      return name;
    }
    name = VOCABULARY.getLiteralName(type);
    return name != null ? name : "<INVALID>";
  }

  private Set<Annotation> buildAnnotationSet(List<Annotation> annotations) {
    HashMap<String, Annotation> annotationMap = new HashMap<String, Annotation>();
    for (Annotation anno : annotations) {
      Annotation old = annotationMap.put(anno.getType(), anno);
      if (old != null) {
        throw new SemanticException(_input, "Multiple annotations of type %s", anno.getType());
      }
    }
    return Collections.unmodifiableSet(new LinkedHashSet<Annotation>(annotationMap.values()));
  }

  private byte parseRegister_nibble(String register) throws SemanticException {
    int totalMethodRegisters = methodTotalRegisters;
    int methodParameterRegisters = this.methodParameterRegisters;

    //register should be in the format "v12"
    int val = Byte.parseByte(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<4) {
      throw new SemanticException(_input, "The maximum allowed register in this context is list of registers is v15");
    }
    //the parser wouldn't have accepted a negative register, i.e. v-1, so we don't have to check for val<0;
    return (byte)val;
  }

  //return a short, because java's byte is signed
  private short parseRegister_byte(String register) throws SemanticException {
    int totalMethodRegisters = methodTotalRegisters;
    int methodParameterRegisters = this.methodParameterRegisters;
    //register should be in the format "v123"
    int val = Short.parseShort(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<8) {
      throw new SemanticException(_input, "The maximum allowed register in this context is v255");
    }
    return (short)val;
  }

  //return an int because java's short is signed
  private int parseRegister_short(String register) throws SemanticException {
    int totalMethodRegisters = methodTotalRegisters;
    int methodParameterRegisters = this.methodParameterRegisters;
    //register should be in the format "v12345"
    int val = Integer.parseInt(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<16) {
      throw new SemanticException(_input, "The maximum allowed register in this context is v65535");
    }
    //the parser wouldn't accept a negative register, i.e. v-1, so we don't have to check for val<0;
    return val;
  }

  private void applyParameter(List<SmaliMethodParameter> params, int paramOrdinal, Token parameterDirective,
      Token reg, String pname, Set<Annotation> anns) throws SemanticException {
    SmaliMethodParameter methodParameter;
    if (reg != null) {
      final int registerNumber = parseRegister_short(reg.getText());
      int totalMethodRegisters = methodTotalRegisters;
      int methodParameterRegisters = this.methodParameterRegisters;

      if (registerNumber >= totalMethodRegisters) {
        throw new SemanticException(_input, parameterDirective, "Register %s is larger than the maximum register v%d " +
            "for this method", reg.getText(), totalMethodRegisters-1);
      }
      final int indexGuess = registerNumber - (totalMethodRegisters - methodParameterRegisters) - (methodIsStatic?0:1);

      if (indexGuess < 0) {
        throw new SemanticException(_input, parameterDirective, "Register %s is not a parameter register.",
            reg.getText());
      }

      int parameterIndex = LinearSearch.linearSearch(params, SmaliMethodParameter.COMPARATOR,
          new WithRegister() { public int getRegister() { return indexGuess; } }, indexGuess);

      if (parameterIndex < 0) {
        throw new SemanticException(_input, parameterDirective, "Register %s is the second half of a wide parameter.",
            reg.getText());
      }

      methodParameter = params.get(parameterIndex);
    } else {
      // Legacy .parameter directive with no register: parameters are positional.
      if (paramOrdinal >= params.size()) {
        throw new SemanticException(_input, parameterDirective, "No parameter exists for this parameter directive");
      }
      methodParameter = params.get(paramOrdinal);
    }

    methodParameter.setName(pname);
    if (anns != null && anns.size() > 0) {
      methodParameter.setAnnotations(anns);
    }
  }

  public String getErrorHeader(RecognitionException e) {
    return getSourceName()+"["+ e.getOffendingToken().getLine()+","+e.getOffendingToken().getCharPositionInLine()+"]";
  }

  private void throwOdexedInstructionException(String odexedInstruction)
      throws OdexedInstructionException {
    throw new OdexedInstructionException(_input, odexedInstruction);
  }
}

smali_file returns[ClassDef classDef]
  @init
  { smaliFileHasClassSpec = smaliFileHasSuperSpec = smaliFileHasSourceSpec = false;
    classAnnotations = new ArrayList<Annotation>();
    List<BuilderField> fieldList = new ArrayList<BuilderField>();
    List<BuilderMethod> methodList = new ArrayList<BuilderMethod>();
    String localClassType = null;
    int localAccessFlags = 0;
    String localSuperType = null;
    List<String> localImplementsList = null;
    String localSourceSpec = null;
  }
  :
  { smaliFileStartToken = _input.LT(1); }
  ( {!smaliFileHasClassSpec}? cs=class_spec
      { smaliFileHasClassSpec = true; localClassType = $cs.className; localAccessFlags = $cs.accessFlags; }
  | {!smaliFileHasSuperSpec}? ss=super_spec
      { smaliFileHasSuperSpec = true; localSuperType = $ss.type; }
  | is=implements_spec
      { if (localImplementsList == null) { localImplementsList = new ArrayList<String>(); }
        localImplementsList.add($is.type); }
  | {!smaliFileHasSourceSpec}? src=source_spec
      { smaliFileHasSourceSpec = true; localSourceSpec = $src.sourceStr; }
  | m=method { methodList.add($m.ret); }
  | f=field { fieldList.add($f.fieldValue); }
  | an=annotation { classAnnotations.add($an.annotationValue); }
  )+
  EOF
  {
    if (!smaliFileHasClassSpec) {
      throw new SemanticException(_input, _localctx.start, "The file must contain a .class directive");
    }

    if (!smaliFileHasSuperSpec) {
      if (!localClassType.equals("Ljava/lang/Object;")) {
        throw new SemanticException(_input, _localctx.start, "The file must contain a .super directive");
      }
    }

    $classDef = dexBuilder.internClassDef(localClassType, localAccessFlags, localSuperType,
            localImplementsList, localSourceSpec, buildAnnotationSet(classAnnotations), fieldList, methodList);
  };

class_spec returns[String className, int accessFlags]
  : CLASS_DIRECTIVE al=access_list CLASS_DESCRIPTOR
    { $className = $CLASS_DESCRIPTOR.text; $accessFlags = $al.value; classType = $className; };

super_spec returns[String type]
  : SUPER_DIRECTIVE CLASS_DESCRIPTOR
    { $type = $CLASS_DESCRIPTOR.text; };

implements_spec returns[String type]
  : IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR
    { $type = $CLASS_DESCRIPTOR.text; };

source_spec returns[String sourceStr]
  : SOURCE_DIRECTIVE sl=string_literal
    { $sourceStr = $sl.value; };

access_list returns[int value]
  @init { $value = 0; }
  : ( ACCESS_SPEC { $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue(); } )*;

access_or_restriction_list returns[int value, Set<HiddenApiRestriction> hiddenApiRestrictions]
  @init
  {
    $value = 0;
    HiddenApiRestriction hiddenApiRestriction = null;
    List<HiddenApiRestriction> domainSpecificApiRestrictions = new ArrayList<HiddenApiRestriction>();
  }
  : (
      ACCESS_SPEC
        { $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue(); }
    | HIDDENAPI_RESTRICTION
        {
          if (opcodes.api < 29) {
            throw new SemanticException(_input, $HIDDENAPI_RESTRICTION, "Hidden API restrictions are only supported on api 29 and above.");
          }
          HiddenApiRestriction restriction = HiddenApiRestriction.forName($HIDDENAPI_RESTRICTION.getText());
          if (restriction.isDomainSpecificApiFlag()) {
            domainSpecificApiRestrictions.add(restriction);
          } else {
            if (hiddenApiRestriction != null) {
              throw new SemanticException(_input, $HIDDENAPI_RESTRICTION, "Only one hidden api restriction may be specified.");
            }
            hiddenApiRestriction = restriction;
          }
        }
    )*
    {
      Set<HiddenApiRestriction> restrictions = new LinkedHashSet<HiddenApiRestriction>();
      if (hiddenApiRestriction != null) {
        restrictions.add(hiddenApiRestriction);
      }
      restrictions.addAll(domainSpecificApiRestrictions);
      $hiddenApiRestrictions = Collections.unmodifiableSet(restrictions);
    };

field returns[BuilderField fieldValue]
  @init { List<Annotation> annotations = new ArrayList<Annotation>(); }
  : FIELD_DIRECTIVE ar=access_or_restriction_list mn=member_name COLON nvtd=nonvoid_type_descriptor
    ( EQUAL lit=literal )?
    ( ({_input.LA(1) == ANNOTATION_DIRECTIVE}? an=annotation { annotations.add($an.annotationValue); })*
      ( END_FIELD_DIRECTIVE
      | { classAnnotations.addAll(annotations); annotations = new ArrayList<Annotation>(); }
      )
    )
    {
      if (classType == null) {
        throw new SemanticException(_input, smaliFileStartToken, "The file must contain a .class directive");
      }

      int accessFlags = $ar.value;
      Set<HiddenApiRestriction> hiddenApiRestrictions = $ar.hiddenApiRestrictions;
      EncodedValue initialValue = _localctx.lit != null ? $lit.encodedValue : null;

      if (!AccessFlags.STATIC.isSet(accessFlags) && initialValue != null) {
        throw new SemanticException(_input, "Initial field values can only be specified for static fields.");
      }

      $fieldValue = dexBuilder.internField(classType, $mn.value, $nvtd.type, accessFlags,
          initialValue, buildAnnotationSet(annotations), hiddenApiRestrictions);
    };

method returns[BuilderMethod ret]
  @init
  {
    methodTotalRegisters = 0;
    methodParameterRegisters = 0;
    int accessFlags = 0;
    methodIsStatic = false;
    Set<HiddenApiRestriction> hiddenApiRestrictions = null;
    methodBuilder = new MethodImplementationBuilder(0);
    methodRegistersDirectiveStart = null;
    methodIsLocalsDirective = false;
  }
  : METHOD_DIRECTIVE
    ar=access_or_restriction_list
    mnp=method_name_and_prototype
    {
      accessFlags = $ar.value;
      hiddenApiRestrictions = $ar.hiddenApiRestrictions;
      methodIsStatic = AccessFlags.STATIC.isSet(accessFlags);
      methodParameterRegisters = MethodUtil.getParameterRegisterCount($mnp.parameterList, methodIsStatic);
    }
    statements_and_directives[$mnp.parameterList]
    END_METHOD_DIRECTIVE
    {
      if (classType == null) {
        throw new SemanticException(_input, smaliFileStartToken, "The file must contain a .class directive");
      }

      MethodImplementation methodImplementation = null;

      boolean isAbstract = false;
      boolean isNative = false;

      if ((accessFlags & AccessFlags.ABSTRACT.getValue()) != 0) {
        isAbstract = true;
      } else if ((accessFlags & AccessFlags.NATIVE.getValue()) != 0) {
        isNative = true;
      }

      methodImplementation = methodBuilder.getMethodImplementation();

      if (!methodImplementation.getInstructions().iterator().hasNext()) {
        if (!isAbstract && !isNative) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "A non-abstract/non-native method must have at least 1 instruction");
        }

        String methodType;
        if (isAbstract) {
          methodType = "an abstract";
        } else {
          methodType = "a native";
        }

        if (methodRegistersDirectiveStart != null) {
          if (methodIsLocalsDirective) {
            throw new SemanticException(_input, methodRegistersDirectiveStart, "A .locals directive is not valid in %s method", methodType);
          } else {
            throw new SemanticException(_input, methodRegistersDirectiveStart, "A .registers directive is not valid in %s method", methodType);
          }
        }

        if (methodImplementation.getTryBlocks().size() > 0) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "try/catch blocks cannot be present in %s method", methodType);
        }

        if (methodImplementation.getDebugItems().iterator().hasNext()) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "debug directives cannot be present in %s method", methodType);
        }

        methodImplementation = null;
      } else {
        if (isAbstract) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "An abstract method cannot have any instructions");
        }
        if (isNative) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "A native method cannot have any instructions");
        }

        if (methodRegistersDirectiveStart == null) {
          throw new SemanticException(_input, $METHOD_DIRECTIVE, "A .registers or .locals directive must be present for a non-abstract/non-final method");
        }

        if (methodTotalRegisters < methodParameterRegisters) {
          throw new SemanticException(_input, methodRegistersDirectiveStart, "This method requires at least " +
              Integer.toString(methodParameterRegisters) +
              " registers, for the method parameters");
        }
      }

      $ret = dexBuilder.internMethod(
              classType,
              $mnp.name,
              $mnp.parameterList,
              $mnp.returnType,
              accessFlags,
              buildAnnotationSet(statementsMethodAnnotations),
              hiddenApiRestrictions,
              methodImplementation);
    };

statements_and_directives[List<SmaliMethodParameter> params]
  @init
  {
    statementsHasRegistersDirective = false;
    statementsMethodAnnotations = new ArrayList<Annotation>();
    List<Catch_directiveContext> catches = new ArrayList<Catch_directiveContext>();
    List<Catchall_directiveContext> catchAlls = new ArrayList<Catchall_directiveContext>();
    int paramOrdinal = 0;
  }
  : ( ordered_method_item
    | registers_directive
    | cd=catch_directive { catches.add(_localctx.cd); }
    | cad=catchall_directive { catchAlls.add(_localctx.cad); }
    | parameter_directive[params, paramOrdinal] { paramOrdinal++; }
    | an=annotation { statementsMethodAnnotations.add($an.annotationValue); }
    )*
    {
      for (Catch_directiveContext catchCtx : catches) {
        methodBuilder.addCatch(dexBuilder.internTypeReference(catchCtx.nvtd.type),
            catchCtx.from.target, catchCtx.to.target, catchCtx.using.target);
      }
      for (Catchall_directiveContext catchallCtx : catchAlls) {
        methodBuilder.addCatch(catchallCtx.from.target, catchallCtx.to.target, catchallCtx.using.target);
      }
    };

ordered_method_item
  : label
  | instruction
  | debug_directive;

registers_directive
  : ( d=REGISTERS_DIRECTIVE rc=short_integral_literal
        { methodIsLocalsDirective = false; methodRegistersDirectiveStart = $d; methodTotalRegisters = $rc.value & 0xFFFF; }
    | d2=LOCALS_DIRECTIVE rc2=short_integral_literal
        { methodIsLocalsDirective = true; methodRegistersDirectiveStart = $d2; methodTotalRegisters = ($rc2.value & 0xFFFF) + methodParameterRegisters; }
    )
    {
      if (statementsHasRegistersDirective) {
        throw new SemanticException(_input, $d != null ? $d : $d2, "There can only be a single .registers or .locals directive in a method");
      }
      statementsHasRegistersDirective = true;
      methodBuilder = new MethodImplementationBuilder(methodTotalRegisters);
    };

method_name_and_prototype returns[String name, List<SmaliMethodParameter> parameterList, String returnType]
  : mn=member_name mp=method_prototype
    {
      $name = $mn.value;
      $parameterList = new ArrayList<SmaliMethodParameter>();

      int paramRegister = 0;
      for (CharSequence type: $mp.proto.getParameterTypes()) {
          $parameterList.add(new SmaliMethodParameter(paramRegister++, type.toString()));
          char c = type.charAt(0);
          if (c == 'D' || c == 'J') {
              paramRegister++;
          }
      }
      $returnType = $mp.proto.getReturnType();
    };

method_prototype returns[ImmutableMethodProtoReference proto]
  : OPEN_PAREN pl=param_list CLOSE_PAREN td=type_descriptor
    { $proto = new ImmutableMethodProtoReference($pl.types, $td.type); };

param_list_or_id returns[String value]
  @init { StringBuilder sb = new StringBuilder(); }
  : ( PARAM_LIST_OR_ID_PRIMITIVE_TYPE { sb.append($PARAM_LIST_OR_ID_PRIMITIVE_TYPE.text); } )+
    { $value = sb.toString(); };

param_list_or_id_primitive_type returns[String value]
  : PARAM_LIST_OR_ID_PRIMITIVE_TYPE { $value = $PARAM_LIST_OR_ID_PRIMITIVE_TYPE.text; };

param_list returns[List<String> types]
  @init { $types = new ArrayList<String>(); }
  : ( pt=param_list_or_id_primitive_type { $types.add($pt.value); } )+
  | ( nvtd=nonvoid_type_descriptor { $types.add($nvtd.type); } )*;

simple_name returns[String value]
  : SIMPLE_NAME { $value = $SIMPLE_NAME.text; }
  | ACCESS_SPEC { $value = $ACCESS_SPEC.text; }
  | HIDDENAPI_RESTRICTION { $value = $HIDDENAPI_RESTRICTION.text; }
  | VERIFICATION_ERROR_TYPE { $value = $VERIFICATION_ERROR_TYPE.text; }
  | POSITIVE_INTEGER_LITERAL { $value = $POSITIVE_INTEGER_LITERAL.text; }
  | NEGATIVE_INTEGER_LITERAL { $value = $NEGATIVE_INTEGER_LITERAL.text; }
  | FLOAT_LITERAL_OR_ID { $value = $FLOAT_LITERAL_OR_ID.text; }
  | DOUBLE_LITERAL_OR_ID { $value = $DOUBLE_LITERAL_OR_ID.text; }
  | BOOL_LITERAL { $value = $BOOL_LITERAL.text; }
  | NULL_LITERAL { $value = $NULL_LITERAL.text; }
  | REGISTER { $value = $REGISTER.text; }
  | plid=param_list_or_id { $value = $plid.value; }
  | PRIMITIVE_TYPE { $value = $PRIMITIVE_TYPE.text; }
  | VOID_TYPE { $value = $VOID_TYPE.text; }
  | ANNOTATION_VISIBILITY { $value = $ANNOTATION_VISIBILITY.text; }
  | METHOD_HANDLE_TYPE_FIELD { $value = $METHOD_HANDLE_TYPE_FIELD.text; }
  | METHOD_HANDLE_TYPE_METHOD { $value = $METHOD_HANDLE_TYPE_METHOD.text; }
  | INSTRUCTION_FORMAT10t { $value = $INSTRUCTION_FORMAT10t.text; }
  | INSTRUCTION_FORMAT10x { $value = $INSTRUCTION_FORMAT10x.text; }
  | INSTRUCTION_FORMAT10x_ODEX { $value = $INSTRUCTION_FORMAT10x_ODEX.text; }
  | INSTRUCTION_FORMAT11x { $value = $INSTRUCTION_FORMAT11x.text; }
  | INSTRUCTION_FORMAT12x_OR_ID { $value = $INSTRUCTION_FORMAT12x_OR_ID.text; }
  | INSTRUCTION_FORMAT21c_FIELD { $value = $INSTRUCTION_FORMAT21c_FIELD.text; }
  | INSTRUCTION_FORMAT21c_FIELD_ODEX { $value = $INSTRUCTION_FORMAT21c_FIELD_ODEX.text; }
  | INSTRUCTION_FORMAT21c_METHOD_HANDLE { $value = $INSTRUCTION_FORMAT21c_METHOD_HANDLE.text; }
  | INSTRUCTION_FORMAT21c_METHOD_TYPE { $value = $INSTRUCTION_FORMAT21c_METHOD_TYPE.text; }
  | INSTRUCTION_FORMAT21c_STRING { $value = $INSTRUCTION_FORMAT21c_STRING.text; }
  | INSTRUCTION_FORMAT21c_TYPE { $value = $INSTRUCTION_FORMAT21c_TYPE.text; }
  | INSTRUCTION_FORMAT21t { $value = $INSTRUCTION_FORMAT21t.text; }
  | INSTRUCTION_FORMAT22c_FIELD { $value = $INSTRUCTION_FORMAT22c_FIELD.text; }
  | INSTRUCTION_FORMAT22c_FIELD_ODEX { $value = $INSTRUCTION_FORMAT22c_FIELD_ODEX.text; }
  | INSTRUCTION_FORMAT22c_TYPE { $value = $INSTRUCTION_FORMAT22c_TYPE.text; }
  | INSTRUCTION_FORMAT22cs_FIELD { $value = $INSTRUCTION_FORMAT22cs_FIELD.text; }
  | INSTRUCTION_FORMAT22s_OR_ID { $value = $INSTRUCTION_FORMAT22s_OR_ID.text; }
  | INSTRUCTION_FORMAT22t { $value = $INSTRUCTION_FORMAT22t.text; }
  | INSTRUCTION_FORMAT23x { $value = $INSTRUCTION_FORMAT23x.text; }
  | INSTRUCTION_FORMAT31i_OR_ID { $value = $INSTRUCTION_FORMAT31i_OR_ID.text; }
  | INSTRUCTION_FORMAT31t { $value = $INSTRUCTION_FORMAT31t.text; }
  | INSTRUCTION_FORMAT35c_CALL_SITE { $value = $INSTRUCTION_FORMAT35c_CALL_SITE.text; }
  | INSTRUCTION_FORMAT35c_METHOD { $value = $INSTRUCTION_FORMAT35c_METHOD.text; }
  | INSTRUCTION_FORMAT35c_METHOD_ODEX { $value = $INSTRUCTION_FORMAT35c_METHOD_ODEX.text; }
  | INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE { $value = $INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE.text; }
  | INSTRUCTION_FORMAT35c_TYPE { $value = $INSTRUCTION_FORMAT35c_TYPE.text; }
  | INSTRUCTION_FORMAT35mi_METHOD { $value = $INSTRUCTION_FORMAT35mi_METHOD.text; }
  | INSTRUCTION_FORMAT35ms_METHOD { $value = $INSTRUCTION_FORMAT35ms_METHOD.text; }
  | INSTRUCTION_FORMAT45cc_METHOD { $value = $INSTRUCTION_FORMAT45cc_METHOD.text; }
  | INSTRUCTION_FORMAT4rcc_METHOD { $value = $INSTRUCTION_FORMAT4rcc_METHOD.text; }
  | INSTRUCTION_FORMAT51l { $value = $INSTRUCTION_FORMAT51l.text; };

member_name returns[String value]
  : sn=simple_name { $value = $sn.value; }
  | MEMBER_NAME { $value = $MEMBER_NAME.text; };

array_descriptor returns[String type]
  : ARRAY_TYPE_PREFIX PRIMITIVE_TYPE
    { $type = $ARRAY_TYPE_PREFIX.text + $PRIMITIVE_TYPE.text; }
  | ARRAY_TYPE_PREFIX CLASS_DESCRIPTOR
    { $type = $ARRAY_TYPE_PREFIX.text + $CLASS_DESCRIPTOR.text; };

type_descriptor returns[String type]
  : VOID_TYPE { $type = "V"; }
  | nvtd=nonvoid_type_descriptor { $type = $nvtd.type; };

nonvoid_type_descriptor returns[String type]
  : PRIMITIVE_TYPE { $type = $PRIMITIVE_TYPE.text; }
  | CLASS_DESCRIPTOR { $type = $CLASS_DESCRIPTOR.text; }
  | ad=array_descriptor { $type = $ad.type; };

reference_type_descriptor returns[String type]
  : CLASS_DESCRIPTOR { $type = $CLASS_DESCRIPTOR.text; }
  | ad=array_descriptor { $type = $ad.type; };

string_literal returns[String value]
  : STRING_LITERAL
    {
      $value = $STRING_LITERAL.text;
      $value = $value.substring(1,$value.length()-1);
    };

integer_literal returns[int value]
  : il=POSITIVE_INTEGER_LITERAL { $value = LiteralTools.parseInt($il.text); }
  | il=NEGATIVE_INTEGER_LITERAL { $value = LiteralTools.parseInt($il.text); };

long_literal returns[long value]
  : ll=LONG_LITERAL { $value = LiteralTools.parseLong($ll.text); };

short_literal returns[short value]
  : sl=SHORT_LITERAL { $value = LiteralTools.parseShort($sl.text); };

byte_literal returns[byte value]
  : bl=BYTE_LITERAL { $value = LiteralTools.parseByte($bl.text); };

float_literal returns[float value]
  : fl=FLOAT_LITERAL_OR_ID { $value = LiteralTools.parseFloat($fl.text); }
  | fl2=FLOAT_LITERAL { $value = LiteralTools.parseFloat($fl2.text); };

double_literal returns[double value]
  : dl=DOUBLE_LITERAL_OR_ID { $value = LiteralTools.parseDouble($dl.text); }
  | dl2=DOUBLE_LITERAL { $value = LiteralTools.parseDouble($dl2.text); };

char_literal returns[char value]
  : cl=CHAR_LITERAL { $value = $cl.text.charAt(1); };

bool_literal returns[boolean value]
  : bl=BOOL_LITERAL { $value = Boolean.parseBoolean($bl.text); };

short_integral_literal returns[short value]
  : ll=long_literal
    {
      LiteralTools.checkShort($ll.value);
      $value = (short)$ll.value;
    }
  | il=integer_literal
    {
      LiteralTools.checkShort($il.value);
      $value = (short)$il.value;
    }
  | sl=short_literal { $value = $sl.value; }
  | cl=char_literal { $value = (short)$cl.value; }
  | bl=byte_literal { $value = $bl.value; };

integral_literal returns[int value]
  : ll=long_literal
    {
      LiteralTools.checkInt($ll.value);
      $value = (int)$ll.value;
    }
  | il=integer_literal { $value = $il.value; }
  | sl=short_literal { $value = $sl.value; }
  | bl=byte_literal { $value = $bl.value; };

// everything but string and double; long is allowed, but it must fit into an int
fixed_32bit_literal returns[int value]
  : il=integer_literal { $value = $il.value; }
  | ll=long_literal { LiteralTools.checkInt($ll.value); $value = (int)$ll.value; }
  | sl=short_literal { $value = $sl.value; }
  | bl=byte_literal { $value = $bl.value; }
  | fl=float_literal { $value = Float.floatToRawIntBits($fl.value); }
  | cl=char_literal { $value = $cl.value; }
  | bol=bool_literal { $value = $bol.value?1:0; };

// same literal set as fixed_32bit_literal, but returned as a long for const-wide/high16
fixed_wide_literal returns[long value]
  : il=integer_literal { $value = $il.value; }
  | ll=long_literal { $value = $ll.value; }
  | sl=short_literal { $value = $sl.value; }
  | bl=byte_literal { $value = $bl.value; }
  | fl=float_literal { $value = Float.floatToRawIntBits($fl.value); }
  | cl=char_literal { $value = $cl.value; }
  | bol=bool_literal { $value = $bol.value?1:0; };

// everything but string
fixed_64bit_literal returns[long value]
  : il=integer_literal { $value = $il.value; }
  | ll=long_literal { $value = $ll.value; }
  | sl=short_literal { $value = $sl.value; }
  | bl=byte_literal { $value = $bl.value; }
  | fl=float_literal { $value = Float.floatToRawIntBits($fl.value); }
  | dl=double_literal { $value = Double.doubleToRawLongBits($dl.value); }
  | cl=char_literal { $value = $cl.value; }
  | bol=bool_literal { $value = $bol.value?1:0; };

fixed_64bit_literal_number returns[Number value]
  : il=integer_literal { $value = $il.value; }
  | ll=long_literal { $value = $ll.value; }
  | sl=short_literal { $value = $sl.value; }
  | bl=byte_literal { $value = $bl.value; }
  | fl=float_literal { $value = Float.floatToRawIntBits($fl.value); }
  | dl=double_literal { $value = Double.doubleToRawLongBits($dl.value); }
  | cl=char_literal { $value = (int)$cl.value; }
  | bol=bool_literal { $value = $bol.value?1:0; };

parsed_integer_literal returns[int value]
  : il=integer_literal { $value = $il.value; };

array_literal returns[List<EncodedValue> elements]
  @init { $elements = new ArrayList<EncodedValue>(); }
  : OPEN_BRACE ( l=literal { $elements.add($l.encodedValue); }
      ( COMMA l2=literal { $elements.add($l2.encodedValue); } )* )? CLOSE_BRACE;

literal returns[ImmutableEncodedValue encodedValue]
  : il=integer_literal { $encodedValue = new ImmutableIntEncodedValue($il.value); }
  | ll=long_literal { $encodedValue = new ImmutableLongEncodedValue($ll.value); }
  | sl=short_literal { $encodedValue = new ImmutableShortEncodedValue($sl.value); }
  | bl=byte_literal { $encodedValue = new ImmutableByteEncodedValue($bl.value); }
  | fl=float_literal { $encodedValue = new ImmutableFloatEncodedValue($fl.value); }
  | dl=double_literal { $encodedValue = new ImmutableDoubleEncodedValue($dl.value); }
  | cl=char_literal { $encodedValue = new ImmutableCharEncodedValue($cl.value); }
  | stl=string_literal { $encodedValue = new ImmutableStringEncodedValue($stl.value); }
  | bol=bool_literal { $encodedValue = ImmutableBooleanEncodedValue.forBoolean($bol.value); }
  | NULL_LITERAL { $encodedValue = ImmutableNullEncodedValue.INSTANCE; }
  | al=array_literal { $encodedValue = new ImmutableArrayEncodedValue($al.elements); }
  | sa=subannotation { $encodedValue = new ImmutableAnnotationEncodedValue($sa.annotationType, $sa.elements); }
  | tfml=type_field_method_literal { $encodedValue = $tfml.encodedValue; }
  | el=enum_literal { $encodedValue = new ImmutableEnumEncodedValue($el.value); }
  | mhl=method_handle_literal { $encodedValue = new ImmutableMethodHandleEncodedValue($mhl.value); }
  | mp=method_prototype { $encodedValue = new ImmutableMethodTypeEncodedValue($mp.proto); };

annotation_element returns[AnnotationElement element]
  : sn=simple_name EQUAL lit=literal
    { $element = new ImmutableAnnotationElement($sn.value, $lit.encodedValue); };

annotation returns[Annotation annotationValue]
  : ANNOTATION_DIRECTIVE av=ANNOTATION_VISIBILITY cd=CLASS_DESCRIPTOR an+=annotation_element* END_ANNOTATION_DIRECTIVE
    {
      int visibility = AnnotationVisibility.getVisibility($av.text);
      List<AnnotationElement> elements = new ArrayList<AnnotationElement>();
      for (Annotation_elementContext ctx : $an) {
        elements.add(ctx.element);
      }
      $annotationValue = new ImmutableAnnotation(visibility, $cd.text, elements);
    };

subannotation returns[String annotationType, List<AnnotationElement> elements]
  @init { List<AnnotationElement> elems = new ArrayList<AnnotationElement>(); }
  : SUBANNOTATION_DIRECTIVE cd=CLASS_DESCRIPTOR ( ae=annotation_element { elems.add($ae.element); } )* END_SUBANNOTATION_DIRECTIVE
    { $annotationType = $cd.text; $elements = elems; };

enum_literal returns[ImmutableFieldReference value]
  : ENUM_DIRECTIVE fr=field_reference
    { $value = $fr.fieldReference; };

type_field_method_literal returns[ImmutableEncodedValue encodedValue]
  : rtd=reference_type_descriptor
      { $encodedValue = new ImmutableTypeEncodedValue($rtd.type); }
  | fr=field_reference
      { $encodedValue = new ImmutableFieldEncodedValue($fr.fieldReference); }
  | mr=method_reference
      { $encodedValue = new ImmutableMethodEncodedValue($mr.methodReference); }
  | PRIMITIVE_TYPE
      { $encodedValue = new ImmutableTypeEncodedValue($PRIMITIVE_TYPE.text); }
  | VOID_TYPE
      { $encodedValue = new ImmutableTypeEncodedValue("V"); };

field_reference returns[ImmutableFieldReference fieldReference]
  : (rt=reference_type_descriptor ARROW)? mn=member_name COLON nvtd=nonvoid_type_descriptor
    {
      String type;
      if (_localctx.rt == null || $rt.type == null) {
        type = classType;
      } else {
        type = $rt.type;
      }
      $fieldReference = new ImmutableFieldReference(type, $mn.value, $nvtd.type);
    };

method_reference returns[ImmutableMethodReference methodReference]
  : (rt=reference_type_descriptor ARROW)? mn=member_name mp=method_prototype
    {
      String type;
      if (_localctx.rt == null || $rt.type == null) {
        type = classType;
      } else {
        type = $rt.type;
      }
      $methodReference = new ImmutableMethodReference(type, $mn.value,
          $mp.proto.getParameterTypes(), $mp.proto.getReturnType());
    };

method_handle_reference returns[ImmutableMethodHandleReference methodHandle]
  : METHOD_HANDLE_TYPE_FIELD AT fr=field_reference
      { $methodHandle = new ImmutableMethodHandleReference(
          MethodHandleType.getMethodHandleType($METHOD_HANDLE_TYPE_FIELD.text), $fr.fieldReference); }
  | METHOD_HANDLE_TYPE_METHOD AT mr=method_reference
      { $methodHandle = new ImmutableMethodHandleReference(
          MethodHandleType.getMethodHandleType($METHOD_HANDLE_TYPE_METHOD.text), $mr.methodReference); }
  | INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE AT mr=method_reference
      { $methodHandle = new ImmutableMethodHandleReference(
          MethodHandleType.getMethodHandleType($INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE.text), $mr.methodReference); };

method_handle_literal returns[ImmutableMethodHandleReference value]
  : mhr=method_handle_reference { $value = $mhr.methodHandle; };

call_site_reference returns[ImmutableCallSiteReference callSiteReference]
  @init { List<ImmutableEncodedValue> extraArguments = new ArrayList<ImmutableEncodedValue>(); }
  : sn=simple_name OPEN_PAREN mn=string_literal COMMA mp=method_prototype
    ( COMMA lit=literal { extraArguments.add($lit.encodedValue); } )*
    CLOSE_PAREN AT mr=method_reference
    {
      ImmutableMethodHandleReference methodHandleReference =
          new ImmutableMethodHandleReference(MethodHandleType.INVOKE_STATIC, $mr.methodReference);
      $callSiteReference = new ImmutableCallSiteReference(
          $sn.value, methodHandleReference, $mn.value, $mp.proto, extraArguments);
    };

verification_error_reference returns[ImmutableReference reference]
  : cd=CLASS_DESCRIPTOR { $reference = new ImmutableTypeReference($cd.text); }
  | fr=field_reference { $reference = $fr.fieldReference; }
  | mr=method_reference { $reference = $mr.methodReference; };

verification_error_type returns[int verificationError]
  : vt=VERIFICATION_ERROR_TYPE
    { $verificationError = VerificationError.getVerificationError($vt.text); };

label
  : COLON sn=simple_name
    { methodBuilder.addLabel($sn.value); };

label_ref returns[Label target]
  : COLON sn=simple_name
    { $target = methodBuilder.getLabel($sn.value); };

register_list returns[byte[] registers, byte registerCount]
  @init
  {
    $registers = new byte[5];
    $registerCount = 0;
  }
  : ( REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(_input, $REGISTER, "A list of registers can only have a maximum of 5 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      }
      ( COMMA REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(_input, $REGISTER, "A list of registers can only have a maximum of 5 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      } )*
    )?;

register_range returns[int startRegister, int endRegister]
  : ( st=REGISTER ( DOTDOT en=REGISTER )? )?
    {
      if (_localctx.st == null) {
        $startRegister = 0;
        $endRegister = -1;
      } else {
        $startRegister = parseRegister_short($st.text);
        if (_localctx.en == null) {
          $endRegister = $startRegister;
        } else {
          $endRegister = parseRegister_short($en.text);
        }

        int registerCount = $endRegister-$startRegister+1;
        if (registerCount < 1) {
          throw new SemanticException(_input, $st, "A register range must have the lower register listed first");
        }
      }
    };

catch_directive
  : CATCH_DIRECTIVE nvtd=nonvoid_type_descriptor OPEN_BRACE from=label_ref DOTDOT to=label_ref CLOSE_BRACE using=label_ref;

catchall_directive
  : CATCHALL_DIRECTIVE OPEN_BRACE from=label_ref DOTDOT to=label_ref CLOSE_BRACE using=label_ref;

parameter_directive[List<SmaliMethodParameter> params, int paramOrdinal]
  @init { List<Annotation> annotations = new ArrayList<Annotation>(); }
  : PARAMETER_DIRECTIVE
    ( reg=REGISTER (COMMA name=string_literal)?
    | name=string_literal
    )?
    ( {_input.LA(1) == ANNOTATION_DIRECTIVE}? an=annotation { annotations.add($an.annotationValue); } )*
    ( END_PARAMETER_DIRECTIVE
        { applyParameter(params, paramOrdinal, $PARAMETER_DIRECTIVE, _localctx.reg,
              _localctx.name != null ? $name.value : null, buildAnnotationSet(annotations)); }
    | { statementsMethodAnnotations.addAll(annotations); }
        { applyParameter(params, paramOrdinal, $PARAMETER_DIRECTIVE, _localctx.reg,
              _localctx.name != null ? $name.value : null, buildAnnotationSet(new ArrayList<Annotation>())); }
    );

debug_directive
  : line_directive
  | local_directive
  | end_local_directive
  | restart_local_directive
  | prologue_directive
  | epilogue_directive
  | source_directive;

line_directive
  : LINE_DIRECTIVE il=integral_literal
    { methodBuilder.addLineNumber($il.value); };

local_directive
  : LOCAL_DIRECTIVE REGISTER
    ( COMMA ln=local_name COLON ( VOID_TYPE | nvtd=nonvoid_type_descriptor )
      ( COMMA sig=string_literal )? )?
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addStartLocal(registerNumber,
              dexBuilder.internNullableStringReference(_localctx.ln != null ? $ln.value : null),
              dexBuilder.internNullableTypeReference(
                      _localctx.nvtd != null ? $nvtd.type : null),
              dexBuilder.internNullableStringReference(_localctx.sig != null ? $sig.value : null));
    };

local_name returns[String value]
  : NULL_LITERAL { $value = null; }
  | sl=string_literal { $value = $sl.value; }
  // An unquoted local name is lexed as a SIMPLE_NAME. Use its raw text, matching the way the
  // historical tree walker stripped the quotes off the normalised STRING_LITERAL node.
  | sn=SIMPLE_NAME { $value = $sn.text; };

end_local_directive
  : END_LOCAL_DIRECTIVE REGISTER
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addEndLocal(registerNumber);
    };

restart_local_directive
  : RESTART_LOCAL_DIRECTIVE REGISTER
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addRestartLocal(registerNumber);
    };

prologue_directive
  : PROLOGUE_DIRECTIVE
    { methodBuilder.addPrologue(); };

epilogue_directive
  : EPILOGUE_DIRECTIVE
    { methodBuilder.addEpilogue(); };

source_directive
  : SOURCE_DIRECTIVE sl=string_literal?
    { methodBuilder.addSetSourceFile(dexBuilder.internNullableStringReference(_localctx.sl != null ? $sl.value : null)); };

instruction_format12x returns[String opcodeName]
  : t=INSTRUCTION_FORMAT12x { $opcodeName = $t.text; }
  | t=INSTRUCTION_FORMAT12x_OR_ID { $opcodeName = $t.text; };

instruction_format22s returns[String opcodeName]
  : t=INSTRUCTION_FORMAT22s { $opcodeName = $t.text; }
  | t=INSTRUCTION_FORMAT22s_OR_ID { $opcodeName = $t.text; };

instruction_format31i returns[String opcodeName]
  : t=INSTRUCTION_FORMAT31i { $opcodeName = $t.text; }
  | t=INSTRUCTION_FORMAT31i_OR_ID { $opcodeName = $t.text; };

instruction_format35c_method returns[String opcodeName]
  : t=INSTRUCTION_FORMAT35c_METHOD { $opcodeName = $t.text; }
  | t=INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE { $opcodeName = $t.text; };

instruction
  : insn_format10t
  | insn_format10x
  | insn_format10x_odex
  | insn_format11n
  | insn_format11x
  | insn_format12x
  | insn_format20bc
  | insn_format20t
  | insn_format21c_field
  | insn_format21c_field_odex
  | insn_format21c_method_handle
  | insn_format21c_method_type
  | insn_format21c_string
  | insn_format21c_type
  | insn_format21ih
  | insn_format21lh
  | insn_format21s
  | insn_format21t
  | insn_format22b
  | insn_format22c_field
  | insn_format22c_field_odex
  | insn_format22c_type
  | insn_format22cs_field
  | insn_format22s
  | insn_format22t
  | insn_format22x
  | insn_format23x
  | insn_format30t
  | insn_format31c
  | insn_format31i
  | insn_format31t
  | insn_format32x
  | insn_format35c_call_site
  | insn_format35c_method
  | insn_format35c_type
  | insn_format35c_method_odex
  | insn_format35mi_method
  | insn_format35ms_method
  | insn_format3rc_call_site
  | insn_format3rc_method
  | insn_format3rc_method_odex
  | insn_format3rc_type
  | insn_format3rmi_method
  | insn_format3rms_method
  | insn_format45cc_method
  | insn_format4rcc_method
  | insn_format51l
  | insn_array_data_directive
  | insn_packed_switch_directive
  | insn_sparse_switch_directive;

insn_format10t
  : INSTRUCTION_FORMAT10t lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10t.text);
      methodBuilder.addInstruction(new BuilderInstruction10t(opcode, $lr.target));
    };

insn_format10x
  : INSTRUCTION_FORMAT10x
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
      methodBuilder.addInstruction(new BuilderInstruction10x(opcode));
    };

insn_format10x_odex
  : INSTRUCTION_FORMAT10x_ODEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT10x_ODEX.text); };

insn_format11n
  : INSTRUCTION_FORMAT11n REGISTER COMMA sl=short_integral_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
      byte regA = parseRegister_nibble($REGISTER.text);

      short litB = $sl.value;
      LiteralTools.checkNibble(litB);

      methodBuilder.addInstruction(new BuilderInstruction11n(opcode, regA, litB));
    };

insn_format11x
  : INSTRUCTION_FORMAT11x REGISTER
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction11x(opcode, regA));
    };

insn_format12x
  : if12=instruction_format12x r1=REGISTER COMMA r2=REGISTER
    {
      Opcode opcode = opcodes.getOpcodeByName($if12.opcodeName);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);

      methodBuilder.addInstruction(new BuilderInstruction12x(opcode, regA, regB));
    };

insn_format20bc
  : INSTRUCTION_FORMAT20bc vet=verification_error_type COMMA ver=verification_error_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT20bc.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT20bc.text);
      }
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20bc.text);
      int verificationError = $vet.verificationError;
      ImmutableReference referencedItem = $ver.reference;

      methodBuilder.addInstruction(new BuilderInstruction20bc(opcode, verificationError,
              dexBuilder.internReference(referencedItem)));
    };

insn_format20t
  : INSTRUCTION_FORMAT20t lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20t.text);
      methodBuilder.addInstruction(new BuilderInstruction20t(opcode, $lr.target));
    };

insn_format21c_field
  : INSTRUCTION_FORMAT21c_FIELD r1=REGISTER COMMA fr=field_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD.text);
      short regA = parseRegister_byte($r1.text);
      ImmutableFieldReference fieldReference = $fr.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format21c_field_odex
  : INSTRUCTION_FORMAT21c_FIELD_ODEX r1=REGISTER COMMA fr=field_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT21c_FIELD_ODEX.text);
      }
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD_ODEX.text);
      short regA = parseRegister_byte($r1.text);
      ImmutableFieldReference fieldReference = $fr.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format21c_method_handle
  : INSTRUCTION_FORMAT21c_METHOD_HANDLE r1=REGISTER COMMA mhr=method_handle_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_METHOD_HANDLE.text);
      short regA = parseRegister_byte($r1.text);
      ImmutableMethodHandleReference methodHandleReference = $mhr.methodHandle;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internMethodHandle(methodHandleReference)));
    };

insn_format21c_method_type
  : INSTRUCTION_FORMAT21c_METHOD_TYPE r1=REGISTER COMMA mp=method_prototype
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_METHOD_TYPE.text);
      short regA = parseRegister_byte($r1.text);
      ImmutableMethodProtoReference methodProtoReference = $mp.proto;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internMethodProtoReference(methodProtoReference)));
    };

insn_format21c_string
  : INSTRUCTION_FORMAT21c_STRING r1=REGISTER COMMA sl=string_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
      short regA = parseRegister_byte($r1.text);

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internStringReference($sl.value)));
    };

insn_format21c_type
  : INSTRUCTION_FORMAT21c_TYPE r1=REGISTER COMMA nvtd=nonvoid_type_descriptor
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
      short regA = parseRegister_byte($r1.text);

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internTypeReference($nvtd.type)));
    };

insn_format21ih
  : INSTRUCTION_FORMAT21ih r1=REGISTER COMMA f32=fixed_32bit_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21ih.text);
      short regA = parseRegister_byte($r1.text);

      // A const/high16 literal is the high 16 bits of the value (e.g. 0x3000 means
      // 0x30000000), but baksmali emits the already-shifted full value. Accept both by only
      // shifting a literal that actually looks like a 16-bit hat. Anything else is left to the
      // builder's checkIntegerHatLiteral, which reports the same error as before.
      int litB = $f32.value;
      if ((litB & 0xFFFF) != 0 && litB >= Short.MIN_VALUE && litB <= 0xFFFF) {
        litB = litB << 16;
      }

      methodBuilder.addInstruction(new BuilderInstruction21ih(opcode, regA, litB));
    };

insn_format21lh
  : INSTRUCTION_FORMAT21lh r1=REGISTER COMMA fw=fixed_wide_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21lh.text);
      short regA = parseRegister_byte($r1.text);

      // A const-wide/high16 literal is the high 16 bits of the value, but baksmali emits the
      // already-shifted full value. Accept both, as for format 21ih above.
      long litB = $fw.value;
      if ((litB & 0xFFFFFFFFFFFFL) != 0L && litB >= Short.MIN_VALUE && litB <= 0xFFFF) {
        litB = litB << 48;
      }

      methodBuilder.addInstruction(new BuilderInstruction21lh(opcode, regA, litB));
    };

insn_format21s
  : INSTRUCTION_FORMAT21s r1=REGISTER COMMA sl=short_integral_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
      short regA = parseRegister_byte($r1.text);

      short litB = $sl.value;

      methodBuilder.addInstruction(new BuilderInstruction21s(opcode, regA, litB));
    };

insn_format21t
  : INSTRUCTION_FORMAT21t r1=REGISTER COMMA lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
      short regA = parseRegister_byte($r1.text);

      methodBuilder.addInstruction(new BuilderInstruction21t(opcode, regA, $lr.target));
    };

insn_format22b
  : INSTRUCTION_FORMAT22b r1=REGISTER COMMA r2=REGISTER COMMA sl=short_integral_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
      short regA = parseRegister_byte($r1.text);
      short regB = parseRegister_byte($r2.text);

      short litC = $sl.value;
      LiteralTools.checkByte(litC);

      methodBuilder.addInstruction(new BuilderInstruction22b(opcode, regA, regB, litC));
    };

insn_format22c_field
  : INSTRUCTION_FORMAT22c_FIELD r1=REGISTER COMMA r2=REGISTER COMMA fr=field_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD.text);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);
      ImmutableFieldReference fieldReference = $fr.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format22c_field_odex
  : INSTRUCTION_FORMAT22c_FIELD_ODEX r1=REGISTER COMMA r2=REGISTER COMMA fr=field_reference
    {
      if (!allowOdex || opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException($INSTRUCTION_FORMAT22c_FIELD_ODEX.text);
      }
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD_ODEX.text);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);
      ImmutableFieldReference fieldReference = $fr.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format22c_type
  : INSTRUCTION_FORMAT22c_TYPE r1=REGISTER COMMA r2=REGISTER COMMA nvtd=nonvoid_type_descriptor
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);

      methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internTypeReference($nvtd.type)));
    };

insn_format22cs_field
  : INSTRUCTION_FORMAT22cs_FIELD r1=REGISTER COMMA r2=REGISTER COMMA FIELD_OFFSET
    { throwOdexedInstructionException($INSTRUCTION_FORMAT22cs_FIELD.text); };

insn_format22s
  : if22s=instruction_format22s r1=REGISTER COMMA r2=REGISTER COMMA sl=short_integral_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($if22s.opcodeName);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);

      short litC = $sl.value;

      methodBuilder.addInstruction(new BuilderInstruction22s(opcode, regA, regB, litC));
    };

insn_format22t
  : INSTRUCTION_FORMAT22t r1=REGISTER COMMA r2=REGISTER COMMA lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
      byte regA = parseRegister_nibble($r1.text);
      byte regB = parseRegister_nibble($r2.text);

      methodBuilder.addInstruction(new BuilderInstruction22t(opcode, regA, regB, $lr.target));
    };

insn_format22x
  : INSTRUCTION_FORMAT22x r1=REGISTER COMMA r2=REGISTER
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
      short regA = parseRegister_byte($r1.text);
      int regB = parseRegister_short($r2.text);

      methodBuilder.addInstruction(new BuilderInstruction22x(opcode, regA, regB));
    };

insn_format23x
  : INSTRUCTION_FORMAT23x r1=REGISTER COMMA r2=REGISTER COMMA r3=REGISTER
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
      short regA = parseRegister_byte($r1.text);
      short regB = parseRegister_byte($r2.text);
      short regC = parseRegister_byte($r3.text);

      methodBuilder.addInstruction(new BuilderInstruction23x(opcode, regA, regB, regC));
    };

insn_format30t
  : INSTRUCTION_FORMAT30t lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT30t.text);
      methodBuilder.addInstruction(new BuilderInstruction30t(opcode, $lr.target));
    };

insn_format31c
  : INSTRUCTION_FORMAT31c r1=REGISTER COMMA sl=string_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
      short regA = parseRegister_byte($r1.text);

      methodBuilder.addInstruction(new BuilderInstruction31c(opcode, regA,
              dexBuilder.internStringReference($sl.value)));
    };

insn_format31i
  : if31i=instruction_format31i r1=REGISTER COMMA f32=fixed_32bit_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($if31i.opcodeName);
      short regA = parseRegister_byte($r1.text);
      int litB = $f32.value;

      methodBuilder.addInstruction(new BuilderInstruction31i(opcode, regA, litB));
    };

insn_format31t
  : INSTRUCTION_FORMAT31t r1=REGISTER COMMA lr=label_ref
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31t.text);
      short regA = parseRegister_byte($r1.text);

      methodBuilder.addInstruction(new BuilderInstruction31t(opcode, regA, $lr.target));
    };

insn_format32x
  : INSTRUCTION_FORMAT32x r1=REGISTER COMMA r2=REGISTER
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
      int regA = parseRegister_short($r1.text);
      int regB = parseRegister_short($r2.text);

      methodBuilder.addInstruction(new BuilderInstruction32x(opcode, regA, regB));
    };

insn_format35c_call_site
  : INSTRUCTION_FORMAT35c_CALL_SITE OPEN_BRACE rl=register_list CLOSE_BRACE COMMA csr=call_site_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_CALL_SITE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $rl.registers;
      byte registerCount = $rl.registerCount;

      ImmutableCallSiteReference callSiteReference = $csr.callSiteReference;

      methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0],
              registers[1], registers[2], registers[3], registers[4], dexBuilder.internCallSite(callSiteReference)));
    };

insn_format35c_method
  : if35c=instruction_format35c_method OPEN_BRACE rl=register_list CLOSE_BRACE COMMA mr=method_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($if35c.opcodeName);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $rl.registers;
      byte registerCount = $rl.registerCount;

      ImmutableMethodReference methodReference = $mr.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internMethodReference(methodReference)));
    };

insn_format35c_type
  : INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE rl=register_list CLOSE_BRACE COMMA nvtd=nonvoid_type_descriptor
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_TYPE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $rl.registers;
      byte registerCount = $rl.registerCount;

      methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internTypeReference($nvtd.type)));
    };

insn_format35c_method_odex
  : INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35c_METHOD_ODEX.text); };

insn_format35mi_method
  : INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35mi_METHOD.text); };

insn_format35ms_method
  : INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT35ms_METHOD.text); };

insn_format3rc_call_site
  : INSTRUCTION_FORMAT3rc_CALL_SITE OPEN_BRACE rr=register_range CLOSE_BRACE COMMA csr=call_site_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_CALL_SITE.text);
      int startRegister = $rr.startRegister;
      int endRegister = $rr.endRegister;

      int registerCount = endRegister - startRegister + 1;

      ImmutableCallSiteReference callSiteReference = $csr.callSiteReference;

      methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internCallSite(callSiteReference)));
    };

insn_format3rc_method
  : INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE rr=register_range CLOSE_BRACE COMMA mr=method_reference
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
      int startRegister = $rr.startRegister;
      int endRegister = $rr.endRegister;

      int registerCount = endRegister-startRegister+1;

      ImmutableMethodReference methodReference = $mr.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internMethodReference(methodReference)));
    };

insn_format3rc_method_odex
  : INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rc_METHOD_ODEX.text); };

insn_format3rc_type
  : INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE rr=register_range CLOSE_BRACE COMMA nvtd=nonvoid_type_descriptor
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_TYPE.text);
      int startRegister = $rr.startRegister;
      int endRegister = $rr.endRegister;

      int registerCount = endRegister-startRegister+1;

      methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internTypeReference($nvtd.type)));
    };

insn_format3rmi_method
  : INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rmi_METHOD.text); };

insn_format3rms_method
  : INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX
    { throwOdexedInstructionException($INSTRUCTION_FORMAT3rms_METHOD.text); };

insn_format45cc_method
  : INSTRUCTION_FORMAT45cc_METHOD OPEN_BRACE rl=register_list CLOSE_BRACE COMMA mr=method_reference COMMA mp=method_prototype
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT45cc_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $rl.registers;
      byte registerCount = $rl.registerCount;

      ImmutableMethodReference methodReference = $mr.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction45cc(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4],
              dexBuilder.internMethodReference(methodReference),
              dexBuilder.internMethodProtoReference($mp.proto)));
    };

insn_format4rcc_method
  : INSTRUCTION_FORMAT4rcc_METHOD OPEN_BRACE rr=register_range CLOSE_BRACE COMMA mr=method_reference COMMA mp=method_prototype
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT4rcc_METHOD.text);

      int startRegister = $rr.startRegister;
      int endRegister = $rr.endRegister;

      int registerCount = endRegister-startRegister+1;

      ImmutableMethodReference methodReference = $mr.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction4rcc(opcode, startRegister, registerCount,
              dexBuilder.internMethodReference(methodReference),
              dexBuilder.internMethodProtoReference($mp.proto)));
    };

insn_format51l
  : INSTRUCTION_FORMAT51l r1=REGISTER COMMA f64=fixed_64bit_literal
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
      short regA = parseRegister_byte($r1.text);

      long litB = $f64.value;

      methodBuilder.addInstruction(new BuilderInstruction51l(opcode, regA, litB));
    };

insn_array_data_directive
  @init { List<Number> elements = new ArrayList<Number>(); }
  : ARRAY_DATA_DIRECTIVE pil=parsed_integer_literal
    {
        int elementWidth = $pil.value;
        if (elementWidth != 4 && elementWidth != 8 && elementWidth != 1 && elementWidth != 2) {
            throw new SemanticException(_input, _localctx.start, "Invalid element width: %d. Must be 1, 2, 4 or 8", elementWidth);
        }
    }
    ( fln=fixed_64bit_literal_number { elements.add($fln.value); } )* END_ARRAY_DATA_DIRECTIVE
    {
      methodBuilder.addInstruction(new BuilderArrayPayload(elementWidth, elements));
    };

insn_packed_switch_directive
  @init { List<Label> labels = new ArrayList<Label>(); }
  : PACKED_SWITCH_DIRECTIVE f32=fixed_32bit_literal ( lr=label_ref { labels.add($lr.target); } )* END_PACKED_SWITCH_DIRECTIVE
    {
      int startKey = $f32.value;
      methodBuilder.addInstruction(new BuilderPackedSwitchPayload(startKey, labels));
    };

insn_sparse_switch_directive
  @init { List<SwitchLabelElement> items = new ArrayList<SwitchLabelElement>(); }
  : SPARSE_SWITCH_DIRECTIVE ( f32=fixed_32bit_literal ARROW lr=label_ref
      { items.add(new SwitchLabelElement($f32.value, $lr.target)); } )* END_SPARSE_SWITCH_DIRECTIVE
    {
      methodBuilder.addInstruction(new BuilderSparseSwitchPayload(items));
    };

