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

parser grammar smaliTreeWalker;

options {
  tokenVocab=smaliParser;
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
import com.android.tools.smali.dexlib2.writer.InstructionFactory;
import com.android.tools.smali.dexlib2.writer.builder.*;
import com.android.tools.smali.util.LinearSearch;

import java.util.*;
}

@members {
  public String classType;
  private boolean verboseErrors = false;
  private int apiLevel = 15;
  private Opcodes opcodes = Opcodes.forApi(apiLevel);
  private DexBuilder dexBuilder;
  private int callSiteNameIndex = 0;

  // Replaces the ANTLR3 'method' rule scope
  private boolean methodIsStatic;
  private int methodTotalRegisters;
  private int methodParameterRegisters;
  private MethodImplementationBuilder methodBuilder;

  public void setDexBuilder(DexBuilder dexBuilder) {
      this.dexBuilder = dexBuilder;
  }

  public void setApiLevel(int apiLevel) {
      this.opcodes = Opcodes.forApi(apiLevel);
      this.apiLevel = apiLevel;
  }

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  private byte parseRegister_nibble(String register)
      throws SemanticException {
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
  private short parseRegister_byte(String register)
      throws SemanticException {
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
  private int parseRegister_short(String register)
      throws SemanticException {
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

  public String getErrorHeader(RecognitionException e) {
    return getSourceName()+"["+ e.getOffendingToken().getLine()+","+e.getOffendingToken().getCharPositionInLine()+"]";
  }
}

smali_file returns[ClassDef classDef]
  : I_CLASS_DEF (DOWN  header methods fields annotations UP)?
  {
    $classDef = dexBuilder.internClassDef($header.classType, $header.accessFlags, $header.superType,
            $header.implementsList, $header.sourceSpec, $annotations.annotationsSet, $fields.fieldsList, $methods.methodsList);
  };


header returns[String classType, int accessFlags, String superType, List<String> implementsList, String sourceSpec]
: class_spec ss=super_spec? implements_list source_spec
  {
    classType = $class_spec.type;
    $classType = classType;
    $accessFlags = $class_spec.accessFlags;
    $superType = _localctx.ss != null ? _localctx.ss.type : null;
    $implementsList = $implements_list.implementsList;
    $sourceSpec = $source_spec.sourceStr;
  };


class_spec returns[String type, int accessFlags]
  : CLASS_DESCRIPTOR access_list
  {
    $type = $CLASS_DESCRIPTOR.text;
    $accessFlags = $access_list.value;
  };

super_spec returns[String type]
  : I_SUPER (DOWN  CLASS_DESCRIPTOR UP)?
  {
    $type = $CLASS_DESCRIPTOR.text;
  };


implements_spec returns[String type]
  : I_IMPLEMENTS (DOWN  CLASS_DESCRIPTOR UP)?
  {
    $type = $CLASS_DESCRIPTOR.text;
  };

implements_list returns[List<String> implementsList]
@init { List<String> typeList; }
  : {typeList = new ArrayList<>();}
    (implements_spec {typeList.add($implements_spec.type);} )*
  {
    if (typeList.size() > 0) {
      $implementsList = typeList;
    } else {
      $implementsList = null;
    }
  };

source_spec returns[String sourceStr]
  : {$sourceStr = null;}
    I_SOURCE (DOWN  string_literal {$sourceStr = $string_literal.value;} UP)?
  | /*epsilon*/;

access_list returns[int value]
  @init
  {
    $value = 0;
  }
  : I_ACCESS_LIST (DOWN 
      (
        ACCESS_SPEC
        {
          $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue();
        }
      )* UP)?;

access_or_restriction_list returns[int value, Set<HiddenApiRestriction> hiddenApiRestrictions]
  @init
  {
    $value = 0;
    HiddenApiRestriction hiddenApiRestriction = null;
    List<HiddenApiRestriction> domainSpecificApiRestrictions = new ArrayList<>();
  }
  : I_ACCESS_OR_RESTRICTION_LIST (DOWN 
      (
        ACCESS_SPEC
        {
          $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue();
        }
        |
        HIDDENAPI_RESTRICTION
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
      )* UP)?
      {
        Set<HiddenApiRestriction> restrictions = new LinkedHashSet<>();
        if (hiddenApiRestriction != null) {
          restrictions.add(hiddenApiRestriction);
        }
        restrictions.addAll(domainSpecificApiRestrictions);
        $hiddenApiRestrictions = Collections.unmodifiableSet(restrictions);
      };

fields returns[List<BuilderField> fieldsList]
  @init {$fieldsList = new ArrayList<>();}
  : I_FIELDS (DOWN 
      (field
      {
        $fieldsList.add($field.fieldValue);
      })* UP)?;

methods returns[List<BuilderMethod> methodsList]
  @init {$methodsList = new ArrayList<>();}
  : I_METHODS (DOWN 
      (method
      {
        $methodsList.add($method.ret);
      })* UP)?;

field returns [BuilderField fieldValue]
  :I_FIELD (DOWN  SIMPLE_NAME access_or_restriction_list I_FIELD_TYPE (DOWN  nonvoid_type_descriptor UP)? field_initial_value annotations? UP)?
  {
    int accessFlags = $access_or_restriction_list.value;
    Set<HiddenApiRestriction> hiddenApiRestrictions = $access_or_restriction_list.hiddenApiRestrictions;

    if (!AccessFlags.STATIC.isSet(accessFlags) && $field_initial_value.encodedValue != null) {
        throw new SemanticException(_input, "Initial field values can only be specified for static fields.");
    }

    $fieldValue = dexBuilder.internField(classType, $SIMPLE_NAME.text, $nonvoid_type_descriptor.type, accessFlags,
            $field_initial_value.encodedValue, $annotations.annotationsSet, hiddenApiRestrictions);
  };


field_initial_value returns[EncodedValue encodedValue]
  : I_FIELD_INITIAL_VALUE (DOWN  literal UP)? {$encodedValue = $literal.encodedValue;}
  | /*epsilon*/;

literal returns[ImmutableEncodedValue encodedValue]
  : integer_literal { $encodedValue = new ImmutableIntEncodedValue($integer_literal.value); }
  | long_literal { $encodedValue = new ImmutableLongEncodedValue($long_literal.value); }
  | short_literal { $encodedValue = new ImmutableShortEncodedValue($short_literal.value); }
  | byte_literal { $encodedValue = new ImmutableByteEncodedValue($byte_literal.value); }
  | float_literal { $encodedValue = new ImmutableFloatEncodedValue($float_literal.value); }
  | double_literal { $encodedValue = new ImmutableDoubleEncodedValue($double_literal.value); }
  | char_literal { $encodedValue = new ImmutableCharEncodedValue($char_literal.value); }
  | string_literal { $encodedValue = new ImmutableStringEncodedValue($string_literal.value); }
  | bool_literal { $encodedValue = ImmutableBooleanEncodedValue.forBoolean($bool_literal.value); }
  | NULL_LITERAL { $encodedValue = ImmutableNullEncodedValue.INSTANCE; }
  | type_descriptor { $encodedValue = new ImmutableTypeEncodedValue($type_descriptor.type); }
  | array_literal { $encodedValue = new ImmutableArrayEncodedValue($array_literal.elements); }
  | subannotation { $encodedValue = new ImmutableAnnotationEncodedValue($subannotation.annotationType, $subannotation.elements); }
  | field_literal { $encodedValue = new ImmutableFieldEncodedValue($field_literal.value); }
  | method_literal { $encodedValue = new ImmutableMethodEncodedValue($method_literal.value); }
  | enum_literal { $encodedValue = new ImmutableEnumEncodedValue($enum_literal.value); }
  | method_handle_literal { $encodedValue = new ImmutableMethodHandleEncodedValue($method_handle_literal.value); }
  | method_prototype { $encodedValue = new ImmutableMethodTypeEncodedValue($method_prototype.proto); };

//everything but string
fixed_64bit_literal_number returns[Number value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { $value = $long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | double_literal { $value = Double.doubleToRawLongBits($double_literal.value); }
  | char_literal { $value = (int)$char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

fixed_64bit_literal returns[long value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { $value = $long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | double_literal { $value = Double.doubleToRawLongBits($double_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

//everything but string and double
//long is allowed, but it must fit into an int
fixed_32bit_literal returns[int value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { LiteralTools.checkInt($long_literal.value); $value = (int)$long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

array_elements returns[List<Number> elements]
  : {$elements = new ArrayList<>();}
    I_ARRAY_ELEMENTS (DOWN 
      (fixed_64bit_literal_number
      {
        $elements.add($fixed_64bit_literal_number.value);
      })* UP)?;

packed_switch_elements returns[List<Label> elements]
  @init {$elements = new ArrayList<>();}
  :
    I_PACKED_SWITCH_ELEMENTS (DOWN 
      (label_ref { $elements.add($label_ref.label); })*
     UP)?;

sparse_switch_elements returns[List<SwitchLabelElement> elements]
  @init {$elements = new ArrayList<>();}
  :
    I_SPARSE_SWITCH_ELEMENTS (DOWN 
       (fixed_32bit_literal label_ref
       {
         $elements.add(new SwitchLabelElement($fixed_32bit_literal.value, $label_ref.label));
       })*
     UP)?;

method returns[BuilderMethod ret]
  @init
  {
    methodTotalRegisters = 0;
    methodParameterRegisters = 0;
    int accessFlags = 0;
    methodIsStatic = false;
    Set<HiddenApiRestriction> hiddenApiRestrictions = null;
  }
  :
    I_METHOD (DOWN 
      method_name_and_prototype
      access_or_restriction_list
      {
        accessFlags = $access_or_restriction_list.value;
        hiddenApiRestrictions = $access_or_restriction_list.hiddenApiRestrictions;
        methodIsStatic = AccessFlags.STATIC.isSet(accessFlags);
        methodParameterRegisters =
                MethodUtil.getParameterRegisterCount($method_name_and_prototype.parameterList, methodIsStatic);
      }
      (
        (registers_directive
        {
          if ($registers_directive.isLocalsDirective) {
            methodTotalRegisters = $registers_directive.registers + methodParameterRegisters;
          } else {
            methodTotalRegisters = $registers_directive.registers;
          }

          methodBuilder = new MethodImplementationBuilder(methodTotalRegisters);

        })
        |
        /* epsilon */
        {
          methodBuilder = new MethodImplementationBuilder(0);
        }
      )
      ordered_method_items
      catches
      parameters[$method_name_and_prototype.parameterList]
      annotations
     UP)?
  {
    MethodImplementation methodImplementation = null;
    List<BuilderTryBlock> tryBlocks = $catches.tryBlocks;

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
        throw new SemanticException(_input, $I_METHOD, "A non-abstract/non-native method must have at least 1 instruction");
      }

      String methodType;
      if (isAbstract) {
        methodType = "an abstract";
      } else {
        methodType = "a native";
      }

      if ($registers_directive.start != null) {
        if ($registers_directive.isLocalsDirective) {
          throw new SemanticException(_input, $registers_directive.start, "A .locals directive is not valid in %s method", methodType);
        } else {
          throw new SemanticException(_input, $registers_directive.start, "A .registers directive is not valid in %s method", methodType);
        }
      }

      if (methodImplementation.getTryBlocks().size() > 0) {
        throw new SemanticException(_input, $I_METHOD, "try/catch blocks cannot be present in %s method", methodType);
      }

      if (methodImplementation.getDebugItems().iterator().hasNext()) {
        throw new SemanticException(_input, $I_METHOD, "debug directives cannot be present in %s method", methodType);
      }

      methodImplementation = null;
    } else {
      if (isAbstract) {
        throw new SemanticException(_input, $I_METHOD, "An abstract method cannot have any instructions");
      }
      if (isNative) {
        throw new SemanticException(_input, $I_METHOD, "A native method cannot have any instructions");
      }

      if ($registers_directive.start == null) {
        throw new SemanticException(_input, $I_METHOD, "A .registers or .locals directive must be present for a non-abstract/non-final method");
      }

      if (methodTotalRegisters < methodParameterRegisters) {
        throw new SemanticException(_input, $registers_directive.start, "This method requires at least " +
                Integer.toString(methodParameterRegisters) +
                " registers, for the method parameters");
      }
    }

    $ret = dexBuilder.internMethod(
            classType,
            $method_name_and_prototype.name,
            $method_name_and_prototype.parameterList,
            $method_name_and_prototype.returnType,
            accessFlags,
            $annotations.annotationsSet,
            hiddenApiRestrictions,
            methodImplementation);
  };

method_prototype returns[ImmutableMethodProtoReference proto]
  : I_METHOD_PROTOTYPE (DOWN  I_METHOD_RETURN_TYPE (DOWN  type_descriptor UP)? method_type_list UP)?
  {
    String returnType = $type_descriptor.type;
    List<String> parameters = $method_type_list.types;
    $proto = new ImmutableMethodProtoReference(parameters, returnType);
  };

method_name_and_prototype returns[String name, List<SmaliMethodParameter> parameterList, String returnType]
  : SIMPLE_NAME method_prototype
  {
    $name = $SIMPLE_NAME.text;
    $parameterList = new ArrayList<>();

    int paramRegister = 0;
    for (CharSequence type: $method_prototype.proto.getParameterTypes()) {
        $parameterList.add(new SmaliMethodParameter(paramRegister++, type.toString()));
        char c = type.charAt(0);
        if (c == 'D' || c == 'J') {
            paramRegister++;
        }
    }
    $returnType = $method_prototype.proto.getReturnType();
  };

method_type_list returns[List<String> types]
  @init
  {
    $types = new ArrayList<>();
  }
  : (
      nonvoid_type_descriptor
      {
        $types.add($nonvoid_type_descriptor.type);
      }
    )*;

call_site_reference returns[ImmutableCallSiteReference callSiteReference]
  :
  I_CALL_SITE_REFERENCE (DOWN  call_site_name=SIMPLE_NAME method_name=string_literal method_prototype
        call_site_extra_arguments method_reference UP)?
    {
        String callSiteName = $call_site_name.text;
        ImmutableMethodHandleReference methodHandleReference =
            new ImmutableMethodHandleReference(MethodHandleType.INVOKE_STATIC,
                $method_reference.methodReference);
        $callSiteReference = new ImmutableCallSiteReference(
            callSiteName, methodHandleReference, $method_name.value, $method_prototype.proto,
            $call_site_extra_arguments.extraArguments);
    };

method_handle_type returns[int methodHandleType]
  : t=METHOD_HANDLE_TYPE_FIELD { $methodHandleType = MethodHandleType.getMethodHandleType($t.text); }
  | t=METHOD_HANDLE_TYPE_METHOD { $methodHandleType = MethodHandleType.getMethodHandleType($t.text); }
  | t=INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE { $methodHandleType = MethodHandleType.getMethodHandleType($t.text); };

method_handle_reference returns[ImmutableMethodHandleReference methodHandle]
  : method_handle_type field_reference {
    $methodHandle = new ImmutableMethodHandleReference($method_handle_type.methodHandleType, $field_reference.fieldReference);
  }
  | method_handle_type method_reference {
    $methodHandle = new ImmutableMethodHandleReference($method_handle_type.methodHandleType, $method_reference.methodReference);
  };

method_handle_literal returns[ImmutableMethodHandleReference value]
  : I_ENCODED_METHOD_HANDLE (DOWN  method_handle_reference UP)? {
    $value = $method_handle_reference.methodHandle;
  };

method_reference returns[ImmutableMethodReference methodReference]
  : rt=reference_type_descriptor? SIMPLE_NAME method_prototype
  {
    String type;
    if (_localctx.rt == null || _localctx.rt.type == null) {
        type = classType;
    } else {
        type = _localctx.rt.type;
    }
    $methodReference = new ImmutableMethodReference(type, $SIMPLE_NAME.text,
             $method_prototype.proto.getParameterTypes(), $method_prototype.proto.getReturnType());
  };

field_reference returns[ImmutableFieldReference fieldReference]
  : rt=reference_type_descriptor? SIMPLE_NAME nonvoid_type_descriptor
  {
    String type;
    if (_localctx.rt == null || _localctx.rt.type == null) {
        type = classType;
    } else {
        type = _localctx.rt.type;
    }
    $fieldReference = new ImmutableFieldReference(type, $SIMPLE_NAME.text,
            $nonvoid_type_descriptor.type);
  };

registers_directive returns[boolean isLocalsDirective, int registers]
  : {$registers = 0;}
    ( I_REGISTERS {$isLocalsDirective = false;}
      | I_LOCALS {$isLocalsDirective = true;}
      ) (DOWN 
      short_integral_literal {$registers = $short_integral_literal.value & 0xFFFF;}
      UP)?;

label_def
  : I_LABEL (DOWN  SIMPLE_NAME UP)?
  {
    methodBuilder.addLabel($SIMPLE_NAME.text);
  };

catches returns[List<BuilderTryBlock> tryBlocks]
  @init {$tryBlocks = new ArrayList<>();}
  : I_CATCHES (DOWN  catch_directive* catchall_directive* UP)?;

catch_directive
  : I_CATCH (DOWN  nonvoid_type_descriptor from=label_ref to=label_ref using=label_ref UP)?
  {
    methodBuilder.addCatch(dexBuilder.internTypeReference($nonvoid_type_descriptor.type),
        $from.label, $to.label, $using.label);
  };

catchall_directive
  : I_CATCHALL (DOWN  from=label_ref to=label_ref using=label_ref UP)?
  {
    methodBuilder.addCatch($from.label, $to.label, $using.label);
  };

parameters[List<SmaliMethodParameter> params]
  : I_PARAMETERS (DOWN  (parameter[params])* UP)?;

parameter[List<SmaliMethodParameter> params]
  : I_PARAMETER (DOWN  REGISTER pname=string_literal? anns=annotations UP)?
    {
        final int registerNumber = parseRegister_short($REGISTER.text);
        int totalMethodRegisters = methodTotalRegisters;
        int methodParameterRegisters = this.methodParameterRegisters;

        if (registerNumber >= totalMethodRegisters) {
            throw new SemanticException(_input, $I_PARAMETER, "Register %s is larger than the maximum register v%d " +
                    "for this method", $REGISTER.text, totalMethodRegisters-1);
        }
        final int indexGuess = registerNumber - (totalMethodRegisters - methodParameterRegisters) - (methodIsStatic?0:1);

        if (indexGuess < 0) {
            throw new SemanticException(_input, $I_PARAMETER, "Register %s is not a parameter register.",
                    $REGISTER.text);
        }

        int parameterIndex = LinearSearch.linearSearch(params, SmaliMethodParameter.COMPARATOR,
            new WithRegister() { public int getRegister() { return indexGuess; } },
                indexGuess);

        if (parameterIndex < 0) {
            throw new SemanticException(_input, $I_PARAMETER, "Register %s is the second half of a wide parameter.",
                                $REGISTER.text);
        }

        SmaliMethodParameter methodParameter = params.get(parameterIndex);
        methodParameter.setName(_localctx.pname != null ? _localctx.pname.value : null);
        if (_localctx.anns != null && _localctx.anns.annotationsSet != null
                && _localctx.anns.annotationsSet.size() > 0) {
            methodParameter.setAnnotations(_localctx.anns.annotationsSet);
        }
    };

debug_directive
  : line
  | local
  | end_local
  | restart_local
  | prologue
  | epilogue
  | source;

line
  : I_LINE (DOWN  integral_literal UP)?
    {
        methodBuilder.addLineNumber($integral_literal.value);
    };

local
  : I_LOCAL (DOWN  REGISTER ((NULL_LITERAL | name=string_literal) nvtd=nonvoid_type_descriptor? signature=string_literal?)? UP)?
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addStartLocal(registerNumber,
              dexBuilder.internNullableStringReference(_localctx.name != null ? _localctx.name.value : null),
              dexBuilder.internNullableTypeReference(
                      _localctx.nvtd != null ? _localctx.nvtd.type : null),
              dexBuilder.internNullableStringReference(_localctx.signature != null ? _localctx.signature.value : null));
    };

end_local
  : I_END_LOCAL (DOWN  REGISTER UP)?
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addEndLocal(registerNumber);
    };

restart_local
  : I_RESTART_LOCAL (DOWN  REGISTER UP)?
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      methodBuilder.addRestartLocal(registerNumber);
    };

prologue
  : I_PROLOGUE
    {
      methodBuilder.addPrologue();
    };

epilogue
  : I_EPILOGUE
    {
      methodBuilder.addEpilogue();
    };

source
  : I_SOURCE (DOWN  string_literal? UP)?
    {
      methodBuilder.addSetSourceFile(dexBuilder.internNullableStringReference($string_literal.value));
    };

call_site_extra_arguments returns[List<ImmutableEncodedValue> extraArguments]
  : { $extraArguments = new ArrayList<>(); }
  I_CALL_SITE_EXTRA_ARGUMENTS (DOWN  (literal { $extraArguments.add($literal.encodedValue); })* UP)?;

ordered_method_items
  : I_ORDERED_METHOD_ITEMS (DOWN  (label_def | instruction | debug_directive)* UP)?;

label_ref returns[Label label]
  : SIMPLE_NAME { $label = methodBuilder.getLabel($SIMPLE_NAME.text); };

register_list returns[byte[] registers, byte registerCount]
  @init
  {
    $registers = new byte[5];
    $registerCount = 0;
  }
  : I_REGISTER_LIST (DOWN 
      (REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(_input, $I_REGISTER_LIST, "A list of registers can only have a maximum of 5 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      })* UP)?;

register_range returns[int startRegister, int endRegister]
  : I_REGISTER_RANGE (DOWN  (startReg=REGISTER endReg=REGISTER?)? UP)?
    {
        if ($startReg == null) {
            $startRegister = 0;
            $endRegister = -1;
        } else {
                $startRegister  = parseRegister_short($startReg.text);
                if ($endReg == null) {
                    $endRegister = $startRegister;
                } else {
                    $endRegister = parseRegister_short($endReg.text);
                }

                int registerCount = $endRegister-$startRegister+1;
                if (registerCount < 1) {
                    throw new SemanticException(_input, $I_REGISTER_RANGE, "A register range must have the lower register listed first");
                }
            }
    };

verification_error_reference returns[ImmutableReference reference]
  : CLASS_DESCRIPTOR
  {
    $reference = new ImmutableTypeReference($CLASS_DESCRIPTOR.text);
  }
  | field_reference
  {
    $reference = $field_reference.fieldReference;
  }
  | method_reference
  {
    $reference = $method_reference.methodReference;
  };

verification_error_type returns[int verificationError]
  : VERIFICATION_ERROR_TYPE
  {
    $verificationError = VerificationError.getVerificationError($VERIFICATION_ERROR_TYPE.text);
  };

instruction
  : insn_format10t
  | insn_format10x
  | insn_format11n
  | insn_format11x
  | insn_format12x
  | insn_format20bc
  | insn_format20t
  | insn_format21c_field
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
  | insn_format22c_type
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
  | insn_format3rc_call_site
  | insn_format3rc_method
  | insn_format3rc_type
  | insn_format45cc_method
  | insn_format4rcc_method
  | insn_format51l_type
  | insn_array_data_directive
  | insn_packed_switch_directive
  | insn_sparse_switch_directive;

insn_format10t
  : //e.g. goto endloop:
    I_STATEMENT_FORMAT10t (DOWN  INSTRUCTION_FORMAT10t label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10t.text);
      methodBuilder.addInstruction(new BuilderInstruction10t(opcode, $label_ref.label));
    };

insn_format10x
  : //e.g. return
    I_STATEMENT_FORMAT10x (DOWN  INSTRUCTION_FORMAT10x UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
      methodBuilder.addInstruction(new BuilderInstruction10x(opcode));
    };

insn_format11n
  : //e.g. const/4 v0, 5
    I_STATEMENT_FORMAT11n (DOWN  INSTRUCTION_FORMAT11n REGISTER short_integral_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
      byte regA = parseRegister_nibble($REGISTER.text);

      short litB = $short_integral_literal.value;
      LiteralTools.checkNibble(litB);

      methodBuilder.addInstruction(new BuilderInstruction11n(opcode, regA, litB));
    };

insn_format11x
  : //e.g. move-result-object v1
    I_STATEMENT_FORMAT11x (DOWN  INSTRUCTION_FORMAT11x REGISTER UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction11x(opcode, regA));
    };

insn_format12x
  : //e.g. move v1 v2
    I_STATEMENT_FORMAT12x (DOWN  INSTRUCTION_FORMAT12x registerA=REGISTER registerB=REGISTER UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT12x.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      methodBuilder.addInstruction(new BuilderInstruction12x(opcode, regA, regB));
    };

insn_format20bc
  : //e.g. throw-verification-error generic-error, Lsome/class;
    I_STATEMENT_FORMAT20bc (DOWN  INSTRUCTION_FORMAT20bc verification_error_type verification_error_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20bc.text);

      int verificationError = $verification_error_type.verificationError;
      ImmutableReference referencedItem = $verification_error_reference.reference;

      methodBuilder.addInstruction(new BuilderInstruction20bc(opcode, verificationError,
              dexBuilder.internReference(referencedItem)));
    };

insn_format20t
  : //e.g. goto/16 endloop:
    I_STATEMENT_FORMAT20t (DOWN  INSTRUCTION_FORMAT20t label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20t.text);
      methodBuilder.addInstruction(new BuilderInstruction20t(opcode, $label_ref.label));
    };

insn_format21c_field
  : //e.g. sget_object v0, java/lang/System/out LJava/io/PrintStream;
    I_STATEMENT_FORMAT21c_FIELD (DOWN  inst=(INSTRUCTION_FORMAT21c_FIELD | INSTRUCTION_FORMAT21c_FIELD_ODEX) REGISTER field_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($inst.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableFieldReference fieldReference = $field_reference.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format21c_method_handle
  : //e.g. const-method-handle v0, invoke-static@Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    I_STATEMENT_FORMAT21c_METHOD_HANDLE (DOWN  inst=INSTRUCTION_FORMAT21c_METHOD_HANDLE REGISTER method_handle_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($inst.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableMethodHandleReference methodHandleReference = $method_handle_reference.methodHandle;

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internMethodHandle(methodHandleReference)));
    };

insn_format21c_method_type
  : //e.g. const-method-type v0, (ILjava/lang/String;)Ljava/lang/String;
    I_STATEMENT_FORMAT21c_METHOD_TYPE (DOWN  inst=INSTRUCTION_FORMAT21c_METHOD_TYPE REGISTER method_prototype UP)?
    {
        Opcode opcode = opcodes.getOpcodeByName($inst.text);
        short regA = parseRegister_byte($REGISTER.text);

        ImmutableMethodProtoReference methodProtoReference = $method_prototype.proto;

        methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
                dexBuilder.internMethodProtoReference(methodProtoReference)));
    };

insn_format21c_string
  : //e.g. const-string v1, "Hello World!"
    I_STATEMENT_FORMAT21c_STRING (DOWN  INSTRUCTION_FORMAT21c_STRING REGISTER string_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format21c_type
  : //e.g. const-class v2, com/android/tools/smali/HelloWorld2/HelloWorld2
    I_STATEMENT_FORMAT21c_TYPE (DOWN  INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format21ih
  : //e.g. const/high16 v1, 1234
    I_STATEMENT_FORMAT21ih (DOWN  INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21ih.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction21ih(opcode, regA, litB));
    };

insn_format21lh
  : //e.g. const-wide/high16 v1, 1234
    I_STATEMENT_FORMAT21lh (DOWN  INSTRUCTION_FORMAT21lh REGISTER fixed_64bit_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21lh.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction21lh(opcode, regA, litB));
    };

insn_format21s
  : //e.g. const/16 v1, 1234
    I_STATEMENT_FORMAT21s (DOWN  INSTRUCTION_FORMAT21s REGISTER short_integral_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
      short regA = parseRegister_byte($REGISTER.text);

      short litB = $short_integral_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction21s(opcode, regA, litB));
    };

insn_format21t
  : //e.g. if-eqz v0, endloop:
    I_STATEMENT_FORMAT21t (DOWN  INSTRUCTION_FORMAT21t REGISTER label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction21t(opcode, regA, $label_ref.label));
    };

insn_format22b
  : //e.g. add-int v0, v1, 123
    I_STATEMENT_FORMAT22b (DOWN  INSTRUCTION_FORMAT22b registerA=REGISTER registerB=REGISTER short_integral_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);

      short litC = $short_integral_literal.value;
      LiteralTools.checkByte(litC);

      methodBuilder.addInstruction(new BuilderInstruction22b(opcode, regA, regB, litC));
    };

insn_format22c_field
  : //e.g. iput-object v1, v0, com/android/tools/smali/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    I_STATEMENT_FORMAT22c_FIELD (DOWN  inst=(INSTRUCTION_FORMAT22c_FIELD | INSTRUCTION_FORMAT22c_FIELD_ODEX) registerA=REGISTER registerB=REGISTER field_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($inst.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      ImmutableFieldReference fieldReference = $field_reference.fieldReference;

      methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format22c_type
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    I_STATEMENT_FORMAT22c_TYPE (DOWN  INSTRUCTION_FORMAT22c_TYPE registerA=REGISTER registerB=REGISTER nonvoid_type_descriptor UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format22s
  : //e.g. add-int/lit16 v0, v1, 12345
    I_STATEMENT_FORMAT22s (DOWN  INSTRUCTION_FORMAT22s registerA=REGISTER registerB=REGISTER short_integral_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22s.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      short litC = $short_integral_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction22s(opcode, regA, regB, litC));
    };

insn_format22t
  : //e.g. if-eq v0, v1, endloop:
    I_STATEMENT_FORMAT22t (DOWN  INSTRUCTION_FORMAT22t registerA=REGISTER registerB=REGISTER label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      methodBuilder.addInstruction(new BuilderInstruction22t(opcode, regA, regB, $label_ref.label));
    };

insn_format22x
  : //e.g. move/from16 v1, v1234
    I_STATEMENT_FORMAT22x (DOWN  INSTRUCTION_FORMAT22x registerA=REGISTER registerB=REGISTER UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
      short regA = parseRegister_byte($registerA.text);
      int regB = parseRegister_short($registerB.text);

      methodBuilder.addInstruction(new BuilderInstruction22x(opcode, regA, regB));
    };

insn_format23x
  : //e.g. add-int v1, v2, v3
    I_STATEMENT_FORMAT23x (DOWN  INSTRUCTION_FORMAT23x registerA=REGISTER registerB=REGISTER registerC=REGISTER UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);
      short regC = parseRegister_byte($registerC.text);

      methodBuilder.addInstruction(new BuilderInstruction23x(opcode, regA, regB, regC));
    };

insn_format30t
  : //e.g. goto/32 endloop:
    I_STATEMENT_FORMAT30t (DOWN  INSTRUCTION_FORMAT30t label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT30t.text);

      methodBuilder.addInstruction(new BuilderInstruction30t(opcode, $label_ref.label));
    };

insn_format31c
  : //e.g. const-string/jumbo v1 "Hello World!"
    I_STATEMENT_FORMAT31c (DOWN  INSTRUCTION_FORMAT31c REGISTER string_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction31c(opcode, regA,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format31i
  : //e.g. const v0, 123456
    I_STATEMENT_FORMAT31i (DOWN  INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31i.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction31i(opcode, regA, litB));
    };

insn_format31t
  : //e.g. fill-array-data v0, ArrayData:
    I_STATEMENT_FORMAT31t (DOWN  INSTRUCTION_FORMAT31t REGISTER label_ref UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31t.text);

      short regA = parseRegister_byte($REGISTER.text);

      methodBuilder.addInstruction(new BuilderInstruction31t(opcode, regA, $label_ref.label));
    };

insn_format32x
  : //e.g. move/16 v5678, v1234
    I_STATEMENT_FORMAT32x (DOWN  INSTRUCTION_FORMAT32x registerA=REGISTER registerB=REGISTER UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
      int regA = parseRegister_short($registerA.text);
      int regB = parseRegister_short($registerB.text);

      methodBuilder.addInstruction(new BuilderInstruction32x(opcode, regA, regB));
    };

insn_format35c_call_site
  : //e.g. invoke-custom {v0, v1}, call_site_name
    // OR invoke-custom {v0, v1}, {"doSomething", (LCustom;Ljava/lang/String;)Ljava/lang/String;, "just testing"}, BootstrapLinker;->normalLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
    I_STATEMENT_FORMAT35c_CALL_SITE (DOWN  INSTRUCTION_FORMAT35c_CALL_SITE register_list call_site_reference UP)?
    {
        Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_CALL_SITE.text);

        //this depends on the fact that register_list returns a byte[5]
        byte[] registers = $register_list.registers;
        byte registerCount = $register_list.registerCount;

        ImmutableCallSiteReference callSiteReference = $call_site_reference.callSiteReference;

        methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0],
                registers[1], registers[2], registers[3], registers[4], dexBuilder.internCallSite(callSiteReference)));
    };

insn_format35c_method
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    I_STATEMENT_FORMAT35c_METHOD (DOWN  INSTRUCTION_FORMAT35c_METHOD register_list method_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internMethodReference(methodReference)));
    };

insn_format35c_type
  : //e.g. filled-new-array {v0,v1}, I
    I_STATEMENT_FORMAT35c_TYPE (DOWN  INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_TYPE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;

      methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format3rc_call_site
  : //e.g. invoke-custom/range {v0 .. v1}, call_site_name
    // OR invoke-custom/range {v0 .. v1}, {"doSomething", (LCustom;Ljava/lang/String;)Ljava/lang/String;, "just testing"}, BootstrapLinker;->normalLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
    I_STATEMENT_FORMAT3rc_CALL_SITE (DOWN  INSTRUCTION_FORMAT3rc_CALL_SITE register_range call_site_reference UP)?
    {
        Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_CALL_SITE.text);
        int startRegister = $register_range.startRegister;
        int endRegister = $register_range.endRegister;

        int registerCount = endRegister - startRegister + 1;

        ImmutableCallSiteReference callSiteReference = $call_site_reference.callSiteReference;

        methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
                dexBuilder.internCallSite(callSiteReference)));
    };

insn_format3rc_method
  : //e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    I_STATEMENT_FORMAT3rc_METHOD (DOWN  INSTRUCTION_FORMAT3rc_METHOD register_range method_reference UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internMethodReference(methodReference)));
    };

insn_format3rc_type
  : //e.g. filled-new-array/range {v0..v6} I
    I_STATEMENT_FORMAT3rc_TYPE (DOWN  INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_TYPE.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;

      methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format45cc_method
  : //e.g. invoke-polymorphic {v0, v1}, java/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (I)J
    I_STATEMENT_FORMAT45cc_METHOD (DOWN  INSTRUCTION_FORMAT45cc_METHOD register_list method_reference method_prototype UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT45cc_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction45cc(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4],
              dexBuilder.internMethodReference(methodReference),
              dexBuilder.internMethodProtoReference($method_prototype.proto)));
    };

insn_format4rcc_method
  : //e.g. invoke-polymorphic {v0..v1}, java/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (I)J
    I_STATEMENT_FORMAT4rcc_METHOD (DOWN  INSTRUCTION_FORMAT4rcc_METHOD register_range method_reference method_prototype UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT4rcc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      methodBuilder.addInstruction(new BuilderInstruction4rcc(opcode, startRegister, registerCount,
              dexBuilder.internMethodReference(methodReference),
              dexBuilder.internMethodProtoReference($method_prototype.proto)));
    };

insn_format51l_type
  : //e.g. const-wide v0, 5000000000L
    I_STATEMENT_FORMAT51l (DOWN  INSTRUCTION_FORMAT51l REGISTER fixed_64bit_literal UP)?
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      methodBuilder.addInstruction(new BuilderInstruction51l(opcode, regA, litB));
    };

insn_array_data_directive
  : //e.g. .array-data 4 1000000 .end array-data
    I_STATEMENT_ARRAY_DATA (DOWN  I_ARRAY_ELEMENT_SIZE (DOWN  short_integral_literal UP)? array_elements UP)?
    {
      int elementWidth = $short_integral_literal.value;
      List<Number> elements = $array_elements.elements;

      methodBuilder.addInstruction(new BuilderArrayPayload(elementWidth, $array_elements.elements));
    };

insn_packed_switch_directive
  :
    I_STATEMENT_PACKED_SWITCH (DOWN  I_PACKED_SWITCH_START_KEY (DOWN  fixed_32bit_literal UP)? packed_switch_elements UP)?
      {
        int startKey = $fixed_32bit_literal.value;
        methodBuilder.addInstruction(new BuilderPackedSwitchPayload(startKey,
            $packed_switch_elements.elements));
      };

insn_sparse_switch_directive
  :
    I_STATEMENT_SPARSE_SWITCH (DOWN  sparse_switch_elements UP)?
    {
      methodBuilder.addInstruction(new BuilderSparseSwitchPayload($sparse_switch_elements.elements));
    };

array_descriptor returns [String type]
  : ARRAY_TYPE_PREFIX ( PRIMITIVE_TYPE { $type = $ARRAY_TYPE_PREFIX.text + $PRIMITIVE_TYPE.text; }
                      | CLASS_DESCRIPTOR { $type = $ARRAY_TYPE_PREFIX.text + $CLASS_DESCRIPTOR.text; });

nonvoid_type_descriptor returns [String type]
  : (PRIMITIVE_TYPE { $type = $PRIMITIVE_TYPE.text; }
  | CLASS_DESCRIPTOR { $type = $CLASS_DESCRIPTOR.text; }
  | array_descriptor { $type = $array_descriptor.type; })
  ;

reference_type_descriptor returns [String type]
  : (CLASS_DESCRIPTOR { $type = $CLASS_DESCRIPTOR.text; }
  | array_descriptor { $type = $array_descriptor.type; })
  ;

type_descriptor returns [String type]
  : VOID_TYPE {$type = "V";}
  | nonvoid_type_descriptor {$type = $nonvoid_type_descriptor.type;}
  ;

short_integral_literal returns[short value]
  : long_literal
    {
      LiteralTools.checkShort($long_literal.value);
      $value = (short)$long_literal.value;
    }
  | integer_literal
    {
      LiteralTools.checkShort($integer_literal.value);
      $value = (short)$integer_literal.value;
    }
  | short_literal {$value = $short_literal.value;}
  | char_literal {$value = (short)$char_literal.value;}
  | byte_literal {$value = $byte_literal.value;};

integral_literal returns[int value]
  : long_literal
    {
      LiteralTools.checkInt($long_literal.value);
      $value = (int)$long_literal.value;
    }
  | integer_literal {$value = $integer_literal.value;}
  | short_literal {$value = $short_literal.value;}
  | byte_literal {$value = $byte_literal.value;};


integer_literal returns[int value]
  : INTEGER_LITERAL { $value = LiteralTools.parseInt($INTEGER_LITERAL.text); };

long_literal returns[long value]
  : LONG_LITERAL { $value = LiteralTools.parseLong($LONG_LITERAL.text); };

short_literal returns[short value]
  : SHORT_LITERAL { $value = LiteralTools.parseShort($SHORT_LITERAL.text); };

byte_literal returns[byte value]
  : BYTE_LITERAL { $value = LiteralTools.parseByte($BYTE_LITERAL.text); };

float_literal returns[float value]
  : FLOAT_LITERAL { $value = LiteralTools.parseFloat($FLOAT_LITERAL.text); };

double_literal returns[double value]
  : DOUBLE_LITERAL { $value = LiteralTools.parseDouble($DOUBLE_LITERAL.text); };

char_literal returns[char value]
  : CHAR_LITERAL { $value = $CHAR_LITERAL.text.charAt(1); };

string_literal returns[String value]
  : STRING_LITERAL
    {
      $value = $STRING_LITERAL.text;
      $value = $value.substring(1,$value.length()-1);
    };

bool_literal returns[boolean value]
  : BOOL_LITERAL { $value = Boolean.parseBoolean($BOOL_LITERAL.text); };

array_literal returns[List<EncodedValue> elements]
  : {$elements = new ArrayList<>();}
    I_ENCODED_ARRAY (DOWN  (literal {$elements.add($literal.encodedValue);})* UP)?;

annotations returns[Set<Annotation> annotationsSet]
  : {HashMap<String, Annotation> annotationMap = new HashMap<>();}
    I_ANNOTATIONS (DOWN  (annotation
    {
        Annotation anno = $annotation.annotationValue;
        Annotation old = annotationMap.put(anno.getType(), anno);
        if (old != null) {
            throw new SemanticException(_input, "Multiple annotations of type %s", anno.getType());
        }
    })* UP)?
    {
        $annotationsSet = Collections.unmodifiableSet(new LinkedHashSet<>(annotationMap.values()));
    };

annotation returns[Annotation annotationValue]
  : I_ANNOTATION (DOWN  ANNOTATION_VISIBILITY subannotation UP)?
    {
      int visibility = AnnotationVisibility.getVisibility($ANNOTATION_VISIBILITY.text);
      $annotationValue = new ImmutableAnnotation(visibility, $subannotation.annotationType, $subannotation.elements);
    };

annotation_element returns[AnnotationElement element]
  : I_ANNOTATION_ELEMENT (DOWN  SIMPLE_NAME literal UP)?
    {
      $element = new ImmutableAnnotationElement($SIMPLE_NAME.text, $literal.encodedValue);
    };

subannotation returns[String annotationType, List<AnnotationElement> elements]
  : {ArrayList<AnnotationElement> elements = new ArrayList<>();}
    I_SUBANNOTATION (DOWN 
        CLASS_DESCRIPTOR
        (annotation_element
        {
           elements.add($annotation_element.element);
        })*
      UP)?
    {
      $annotationType = $CLASS_DESCRIPTOR.text;
      $elements = elements;
    };

field_literal returns[ImmutableFieldReference value]
  : I_ENCODED_FIELD (DOWN  field_reference UP)?
    {
      $value = $field_reference.fieldReference;
    };

method_literal returns[ImmutableMethodReference value]
  : I_ENCODED_METHOD (DOWN  method_reference UP)?
    {
      $value = $method_reference.methodReference;
    };

enum_literal returns[ImmutableFieldReference value]
  : I_ENCODED_ENUM (DOWN  field_reference UP)?
    {
      $value = $field_reference.fieldReference;
    };
