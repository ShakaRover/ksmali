/*
 * Copyright 2012, Google LLC
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

package com.android.tools.smali.dexlib2

import com.android.tools.smali.util.Range
import com.android.tools.smali.util.UnmodifiableRangeMap

enum class Opcode(
    val apiToValueMap: UnmodifiableRangeMap<Int, Short>,
    val artVersionToValueMap: UnmodifiableRangeMap<Int, Short>,
    val mnemonic: String,
    val referenceType: Int,
    val referenceType2: Int,
    val format: Format,
    val flags: Int
) {
    NOP(0x00, "nop", ReferenceType.NONE, Format.Format10x, FLAG_CAN_CONTINUE),
    MOVE(0x01, "move", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_FROM16(0x02, "move/from16", ReferenceType.NONE, Format.Format22x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_16(0x03, "move/16", ReferenceType.NONE, Format.Format32x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_WIDE(0x04, "move-wide", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MOVE_WIDE_FROM16(0x05, "move-wide/from16", ReferenceType.NONE, Format.Format22x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MOVE_WIDE_16(0x06, "move-wide/16", ReferenceType.NONE, Format.Format32x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MOVE_OBJECT(0x07, "move-object", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_OBJECT_FROM16(0x08, "move-object/from16", ReferenceType.NONE, Format.Format22x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_OBJECT_16(0x09, "move-object/16", ReferenceType.NONE, Format.Format32x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_RESULT(0x0a, "move-result", ReferenceType.NONE, Format.Format11x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_RESULT_WIDE(0x0b, "move-result-wide", ReferenceType.NONE, Format.Format11x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MOVE_RESULT_OBJECT(0x0c, "move-result-object", ReferenceType.NONE, Format.Format11x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MOVE_EXCEPTION(0x0d, "move-exception", ReferenceType.NONE, Format.Format11x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    RETURN_VOID(0x0e, "return-void", ReferenceType.NONE, Format.Format10x),
    RETURN(0x0f, "return", ReferenceType.NONE, Format.Format11x),
    RETURN_WIDE(0x10, "return-wide", ReferenceType.NONE, Format.Format11x),
    RETURN_OBJECT(0x11, "return-object", ReferenceType.NONE, Format.Format11x),
    CONST_4(0x12, "const/4", ReferenceType.NONE, Format.Format11n, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_16(0x13, "const/16", ReferenceType.NONE, Format.Format21s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST(0x14, "const", ReferenceType.NONE, Format.Format31i, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_HIGH16(0x15, "const/high16", ReferenceType.NONE, Format.Format21ih, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_WIDE_16(0x16, "const-wide/16", ReferenceType.NONE, Format.Format21s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    CONST_WIDE_32(0x17, "const-wide/32", ReferenceType.NONE, Format.Format31i, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    CONST_WIDE(0x18, "const-wide", ReferenceType.NONE, Format.Format51l, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    CONST_WIDE_HIGH16(0x19, "const-wide/high16", ReferenceType.NONE, Format.Format21lh, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    CONST_STRING(0x1a, "const-string", ReferenceType.STRING, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_STRING_JUMBO(0x1b, "const-string/jumbo", ReferenceType.STRING, Format.Format31c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_CLASS(0x1c, "const-class", ReferenceType.TYPE, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MONITOR_ENTER(0x1d, "monitor-enter", ReferenceType.NONE, Format.Format11x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    MONITOR_EXIT(0x1e, "monitor-exit", ReferenceType.NONE, Format.Format11x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    CHECK_CAST(0x1f, "check-cast", ReferenceType.TYPE, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    INSTANCE_OF(0x20, "instance-of", ReferenceType.TYPE, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ARRAY_LENGTH(0x21, "array-length", ReferenceType.NONE, Format.Format12x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    NEW_INSTANCE(0x22, "new-instance", ReferenceType.TYPE, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    NEW_ARRAY(0x23, "new-array", ReferenceType.TYPE, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    FILLED_NEW_ARRAY(0x24, "filled-new-array", ReferenceType.TYPE, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    FILLED_NEW_ARRAY_RANGE(0x25, "filled-new-array/range", ReferenceType.TYPE, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    FILL_ARRAY_DATA(0x26, "fill-array-data", ReferenceType.NONE, Format.Format31t, FLAG_CAN_CONTINUE),
    THROW(0x27, "throw", ReferenceType.NONE, Format.Format11x, FLAG_CAN_THROW),
    GOTO(0x28, "goto", ReferenceType.NONE, Format.Format10t),
    GOTO_16(0x29, "goto/16", ReferenceType.NONE, Format.Format20t),
    GOTO_32(0x2a, "goto/32", ReferenceType.NONE, Format.Format30t),
    PACKED_SWITCH(0x2b, "packed-switch", ReferenceType.NONE, Format.Format31t, FLAG_CAN_CONTINUE),
    SPARSE_SWITCH(0x2c, "sparse-switch", ReferenceType.NONE, Format.Format31t, FLAG_CAN_CONTINUE),
    CMPL_FLOAT(0x2d, "cmpl-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CMPG_FLOAT(0x2e, "cmpg-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CMPL_DOUBLE(0x2f, "cmpl-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CMPG_DOUBLE(0x30, "cmpg-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CMP_LONG(0x31, "cmp-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IF_EQ(0x32, "if-eq", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_NE(0x33, "if-ne", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_LT(0x34, "if-lt", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_GE(0x35, "if-ge", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_GT(0x36, "if-gt", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_LE(0x37, "if-le", ReferenceType.NONE, Format.Format22t, FLAG_CAN_CONTINUE),
    IF_EQZ(0x38, "if-eqz", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    IF_NEZ(0x39, "if-nez", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    IF_LTZ(0x3a, "if-ltz", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    IF_GEZ(0x3b, "if-gez", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    IF_GTZ(0x3c, "if-gtz", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    IF_LEZ(0x3d, "if-lez", ReferenceType.NONE, Format.Format21t, FLAG_CAN_CONTINUE),
    AGET(0x44, "aget", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AGET_WIDE(0x45, "aget-wide", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    AGET_OBJECT(0x46, "aget-object", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AGET_BOOLEAN(0x47, "aget-boolean", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AGET_BYTE(0x48, "aget-byte", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AGET_CHAR(0x49, "aget-char", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AGET_SHORT(0x4a, "aget-short", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    APUT(0x4b, "aput", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_WIDE(0x4c, "aput-wide", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_OBJECT(0x4d, "aput-object", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_BOOLEAN(0x4e, "aput-boolean", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_BYTE(0x4f, "aput-byte", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_CHAR(0x50, "aput-char", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    APUT_SHORT(0x51, "aput-short", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IGET(0x52, "iget", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_WIDE(0x53, "iget-wide", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    IGET_OBJECT(0x54, "iget-object", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_BOOLEAN(0x55, "iget-boolean", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_BYTE(0x56, "iget-byte", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_CHAR(0x57, "iget-char", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_SHORT(0x58, "iget-short", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IPUT(0x59, "iput", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_WIDE(0x5a, "iput-wide", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_OBJECT(0x5b, "iput-object", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_BOOLEAN(0x5c, "iput-boolean", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_BYTE(0x5d, "iput-byte", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_CHAR(0x5e, "iput-char", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_SHORT(0x5f, "iput-short", ReferenceType.FIELD, Format.Format22c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    SGET(0x60, "sget", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_WIDE(0x61, "sget-wide", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_OBJECT(0x62, "sget-object", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_BOOLEAN(0x63, "sget-boolean", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_BYTE(0x64, "sget-byte", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_CHAR(0x65, "sget-char", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SGET_SHORT(0x66, "sget-short", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT(0x67, "sput", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_WIDE(0x68, "sput-wide", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_OBJECT(0x69, "sput-object", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_BOOLEAN(0x6a, "sput-boolean", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_BYTE(0x6b, "sput-byte", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_CHAR(0x6c, "sput-char", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_SHORT(0x6d, "sput-short", ReferenceType.FIELD, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    INVOKE_VIRTUAL(0x6e, "invoke-virtual", ReferenceType.METHOD, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_SUPER(0x6f, "invoke-super", ReferenceType.METHOD, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_DIRECT(0x70, "invoke-direct", ReferenceType.METHOD, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT or FLAG_CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC(0x71, "invoke-static", ReferenceType.METHOD, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_INTERFACE(0x72, "invoke-interface", ReferenceType.METHOD, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_VIRTUAL_RANGE(0x74, "invoke-virtual/range", ReferenceType.METHOD, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_SUPER_RANGE(0x75, "invoke-super/range", ReferenceType.METHOD, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_DIRECT_RANGE(0x76, "invoke-direct/range", ReferenceType.METHOD, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT or FLAG_CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC_RANGE(0x77, "invoke-static/range", ReferenceType.METHOD, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_INTERFACE_RANGE(0x78, "invoke-interface/range", ReferenceType.METHOD, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    NEG_INT(0x7b, "neg-int", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    NOT_INT(0x7c, "not-int", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    NEG_LONG(0x7d, "neg-long", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    NOT_LONG(0x7e, "not-long", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    NEG_FLOAT(0x7f, "neg-float", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    NEG_DOUBLE(0x80, "neg-double", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    INT_TO_LONG(0x81, "int-to-long", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    INT_TO_FLOAT(0x82, "int-to-float", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    INT_TO_DOUBLE(0x83, "int-to-double", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    LONG_TO_INT(0x84, "long-to-int", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    LONG_TO_FLOAT(0x85, "long-to-float", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    LONG_TO_DOUBLE(0x86, "long-to-double", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    FLOAT_TO_INT(0x87, "float-to-int", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    FLOAT_TO_LONG(0x88, "float-to-long", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    FLOAT_TO_DOUBLE(0x89, "float-to-double", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DOUBLE_TO_INT(0x8a, "double-to-int", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DOUBLE_TO_LONG(0x8b, "double-to-long", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DOUBLE_TO_FLOAT(0x8c, "double-to-float", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    INT_TO_BYTE(0x8d, "int-to-byte", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    INT_TO_CHAR(0x8e, "int-to-char", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    INT_TO_SHORT(0x8f, "int-to-short", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_INT(0x90, "add-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SUB_INT(0x91, "sub-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_INT(0x92, "mul-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_INT(0x93, "div-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_INT(0x94, "rem-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AND_INT(0x95, "and-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    OR_INT(0x96, "or-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    XOR_INT(0x97, "xor-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHL_INT(0x98, "shl-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHR_INT(0x99, "shr-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    USHR_INT(0x9a, "ushr-int", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_LONG(0x9b, "add-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SUB_LONG(0x9c, "sub-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MUL_LONG(0x9d, "mul-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DIV_LONG(0x9e, "div-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    REM_LONG(0x9f, "rem-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    AND_LONG(0xa0, "and-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    OR_LONG(0xa1, "or-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    XOR_LONG(0xa2, "xor-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SHL_LONG(0xa3, "shl-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SHR_LONG(0xa4, "shr-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    USHR_LONG(0xa5, "ushr-long", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    ADD_FLOAT(0xa6, "add-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SUB_FLOAT(0xa7, "sub-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_FLOAT(0xa8, "mul-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_FLOAT(0xa9, "div-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_FLOAT(0xaa, "rem-float", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_DOUBLE(0xab, "add-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SUB_DOUBLE(0xac, "sub-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MUL_DOUBLE(0xad, "mul-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DIV_DOUBLE(0xae, "div-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    REM_DOUBLE(0xaf, "rem-double", ReferenceType.NONE, Format.Format23x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    ADD_INT_2ADDR(0xb0, "add-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SUB_INT_2ADDR(0xb1, "sub-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_INT_2ADDR(0xb2, "mul-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_INT_2ADDR(0xb3, "div-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_INT_2ADDR(0xb4, "rem-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AND_INT_2ADDR(0xb5, "and-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    OR_INT_2ADDR(0xb6, "or-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    XOR_INT_2ADDR(0xb7, "xor-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHL_INT_2ADDR(0xb8, "shl-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHR_INT_2ADDR(0xb9, "shr-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    USHR_INT_2ADDR(0xba, "ushr-int/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_LONG_2ADDR(0xbb, "add-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SUB_LONG_2ADDR(0xbc, "sub-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MUL_LONG_2ADDR(0xbd, "mul-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DIV_LONG_2ADDR(0xbe, "div-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    REM_LONG_2ADDR(0xbf, "rem-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    AND_LONG_2ADDR(0xc0, "and-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    OR_LONG_2ADDR(0xc1, "or-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    XOR_LONG_2ADDR(0xc2, "xor-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SHL_LONG_2ADDR(0xc3, "shl-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SHR_LONG_2ADDR(0xc4, "shr-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    USHR_LONG_2ADDR(0xc5, "ushr-long/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    ADD_FLOAT_2ADDR(0xc6, "add-float/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SUB_FLOAT_2ADDR(0xc7, "sub-float/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_FLOAT_2ADDR(0xc8, "mul-float/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_FLOAT_2ADDR(0xc9, "div-float/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_FLOAT_2ADDR(0xca, "rem-float/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_DOUBLE_2ADDR(0xcb, "add-double/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    SUB_DOUBLE_2ADDR(0xcc, "sub-double/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    MUL_DOUBLE_2ADDR(0xcd, "mul-double/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    DIV_DOUBLE_2ADDR(0xce, "div-double/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    REM_DOUBLE_2ADDR(0xcf, "rem-double/2addr", ReferenceType.NONE, Format.Format12x, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    ADD_INT_LIT16(0xd0, "add-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    RSUB_INT(0xd1, "rsub-int", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_INT_LIT16(0xd2, "mul-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_INT_LIT16(0xd3, "div-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_INT_LIT16(0xd4, "rem-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AND_INT_LIT16(0xd5, "and-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    OR_INT_LIT16(0xd6, "or-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    XOR_INT_LIT16(0xd7, "xor-int/lit16", ReferenceType.NONE, Format.Format22s, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    ADD_INT_LIT8(0xd8, "add-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    RSUB_INT_LIT8(0xd9, "rsub-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    MUL_INT_LIT8(0xda, "mul-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    DIV_INT_LIT8(0xdb, "div-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    REM_INT_LIT8(0xdc, "rem-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    AND_INT_LIT8(0xdd, "and-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    OR_INT_LIT8(0xde, "or-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    XOR_INT_LIT8(0xdf, "xor-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHL_INT_LIT8(0xe0, "shl-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    SHR_INT_LIT8(0xe1, "shr-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    USHR_INT_LIT8(0xe2, "ushr-int/lit8", ReferenceType.NONE, Format.Format22b, FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),

    IGET_VOLATILE(firstApi(0xe3, 9), "iget-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IPUT_VOLATILE(firstApi(0xe4, 9), "iput-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    SGET_VOLATILE(firstApi(0xe5, 9), "sget-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_VOLATILE(firstApi(0xe6, 9), "sput-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),
    IGET_OBJECT_VOLATILE(firstApi(0xe7, 9), "iget-object-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_WIDE_VOLATILE(firstApi(0xe8, 9), "iget-wide-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    IPUT_WIDE_VOLATILE(firstApi(0xe9, 9), "iput-wide-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    SGET_WIDE_VOLATILE(firstApi(0xea, 9), "sget-wide-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_WIDE_VOLATILE(firstApi(0xeb, 9), "sput-wide-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),

    THROW_VERIFICATION_ERROR(firstApi(0xed, 5), "throw-verification-error", ReferenceType.NONE, Format.Format20bc, FLAG_ODEX_ONLY or FLAG_CAN_THROW),
    EXECUTE_INLINE(allApis(0xee), "execute-inline", ReferenceType.NONE,  Format.Format35mi, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    EXECUTE_INLINE_RANGE(firstApi(0xef, 8), "execute-inline/range", ReferenceType.NONE,  Format.Format3rmi,  FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_DIRECT_EMPTY(lastApi(0xf0, 13), "invoke-direct-empty", ReferenceType.METHOD,  Format.Format35c, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT or FLAG_CAN_INITIALIZE_REFERENCE),
    INVOKE_OBJECT_INIT_RANGE(firstApi(0xf0, 14), "invoke-object-init/range", ReferenceType.METHOD,  Format.Format3rc, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT or FLAG_CAN_INITIALIZE_REFERENCE),
    RETURN_VOID_BARRIER(combine(firstApi(0xf1, 11), lastArtVersion(0x73, 59)), "return-void-barrier", ReferenceType.NONE, Format.Format10x, FLAG_ODEX_ONLY),
    RETURN_VOID_NO_BARRIER(firstArtVersion(0x73, 60), "return-void-no-barrier", ReferenceType.NONE, Format.Format10x, FLAG_ODEX_ONLY),
    IGET_QUICK(combine(allApis(0xf2), allArtVersions(0xe3)), "iget-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_WIDE_QUICK(combine(allApis(0xf3), allArtVersions(0xe4)), "iget-wide-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_SETS_WIDE_REGISTER),
    IGET_OBJECT_QUICK(combine(allApis(0xf4), allArtVersions(0xe5)), "iget-object-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IPUT_QUICK(combine(allApis(0xf5), allArtVersions(0xe6)), "iput-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_WIDE_QUICK(combine(allApis(0xf6), allArtVersions(0xe7)), "iput-wide-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_OBJECT_QUICK(combine(allApis(0xf7), allArtVersions(0xe8)), "iput-object-quick", ReferenceType.NONE,  Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    IPUT_BOOLEAN_QUICK(allArtVersions(0xeb), "iput-boolean-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_QUICK_FIELD_ACCESSOR),
    IPUT_BYTE_QUICK(allArtVersions(0xec), "iput-byte-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_QUICK_FIELD_ACCESSOR),
    IPUT_CHAR_QUICK(allArtVersions(0xed), "iput-char-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_QUICK_FIELD_ACCESSOR),
    IPUT_SHORT_QUICK(allArtVersions(0xee), "iput-short-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_QUICK_FIELD_ACCESSOR),
    IGET_BOOLEAN_QUICK(allArtVersions(0xef), "iget-boolean-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_BYTE_QUICK(allArtVersions(0xf0), "iget-byte-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_CHAR_QUICK(allArtVersions(0xf1), "iget-char-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    IGET_SHORT_QUICK(allArtVersions(0xf2), "iget-short-quick", ReferenceType.NONE, Format.Format22cs, FLAG_ODEX_ONLY or FLAG_QUICK_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    
    INVOKE_VIRTUAL_QUICK(combine(allApis(0xf8), allArtVersions(0xe9)), "invoke-virtual-quick", ReferenceType.NONE,  Format.Format35ms, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_VIRTUAL_QUICK_RANGE(combine(allApis(0xf9), allArtVersions(0xea)), "invoke-virtual-quick/range", ReferenceType.NONE,  Format.Format3rms, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_SUPER_QUICK(lastApi(0xfa, 25), "invoke-super-quick", ReferenceType.NONE,  Format.Format35ms, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_SUPER_QUICK_RANGE(lastApi(0xfb, 25), "invoke-super-quick/range", ReferenceType.NONE,  Format.Format3rms, FLAG_ODEX_ONLY or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),

    IPUT_OBJECT_VOLATILE(firstApi(0xfc, 9), "iput-object-volatile", ReferenceType.FIELD, Format.Format22c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE),
    SGET_OBJECT_VOLATILE(firstApi(0xfd, 9), "sget-object-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER or FLAG_STATIC_FIELD_ACCESSOR),
    SPUT_OBJECT_VOLATILE(betweenApi(0xfe, 9, 19), "sput-object-volatile", ReferenceType.FIELD, Format.Format21c, FLAG_ODEX_ONLY or FLAG_VOLATILE_FIELD_ACCESSOR or FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_STATIC_FIELD_ACCESSOR),

    PACKED_SWITCH_PAYLOAD(0x100, "packed-switch-payload", ReferenceType.NONE, Format.PackedSwitchPayload, 0),
    SPARSE_SWITCH_PAYLOAD(0x200, "sparse-switch-payload", ReferenceType.NONE, Format.SparseSwitchPayload, 0),
    ARRAY_PAYLOAD(0x300, "array-payload", ReferenceType.NONE, Format.ArrayPayload, 0),

    INVOKE_POLYMORPHIC(firstArtVersion(0xfa, 87), "invoke-polymorphic", ReferenceType.METHOD, ReferenceType.METHOD_PROTO, Format.Format45cc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_POLYMORPHIC_RANGE(firstArtVersion(0xfb, 87), "invoke-polymorphic/range", ReferenceType.METHOD, ReferenceType.METHOD_PROTO, Format.Format4rcc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),

    INVOKE_CUSTOM(firstArtVersion(0xfc, 111), "invoke-custom", ReferenceType.CALL_SITE, Format.Format35c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),
    INVOKE_CUSTOM_RANGE(firstArtVersion(0xfd, 111), "invoke-custom/range", ReferenceType.CALL_SITE, Format.Format3rc, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_RESULT),

    CONST_METHOD_HANDLE(firstArtVersion(0xfe, 134), "const-method-handle", ReferenceType.METHOD_HANDLE, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER),
    CONST_METHOD_TYPE(firstArtVersion(0xff, 134), "const-method-type", ReferenceType.METHOD_PROTO, Format.Format21c, FLAG_CAN_THROW or FLAG_CAN_CONTINUE or FLAG_SETS_REGISTER);

    constructor(opcodeValue: Int, mnemonic: String, referenceType: Int, format: Format) :
        this(allVersions(opcodeValue), mnemonic, referenceType, -1, format, 0)

    constructor(opcodeValue: Int, mnemonic: String, referenceType: Int, format: Format, flags: Int) :
        this(allVersions(opcodeValue), mnemonic, referenceType, -1, format, flags)

    constructor(
        versionConstraints: List<VersionConstraint>, mnemonic: String, referenceType: Int,
        format: Format, flags: Int
    ) : this(versionConstraints, mnemonic, referenceType, -1, format, flags)

    constructor(
        versionConstraints: List<VersionConstraint>, mnemonic: String, referenceType: Int,
        referenceType2: Int, format: Format, flags: Int
    ) : this(
        buildApiToValueMap(versionConstraints), buildArtVersionToValueMap(versionConstraints),
        mnemonic, referenceType, referenceType2, format, flags
    )

    fun canThrow(): Boolean {
        return (flags and CAN_THROW) != 0
    }

    fun odexOnly(): Boolean {
        return (flags and ODEX_ONLY) != 0
    }

    fun canContinue(): Boolean {
        return (flags and CAN_CONTINUE) != 0
    }

    fun setsResult(): Boolean {
        return (flags and SETS_RESULT) != 0
    }

    fun setsRegister(): Boolean {
        return (flags and SETS_REGISTER) != 0
    }

    fun setsWideRegister(): Boolean {
        return (flags and SETS_WIDE_REGISTER) != 0
    }

    fun isQuickFieldaccessor(): Boolean {
        return (flags and QUICK_FIELD_ACCESSOR) != 0
    }

    fun isVolatileFieldAccessor(): Boolean {
        return (flags and VOLATILE_FIELD_ACCESSOR) != 0
    }

    fun isStaticFieldAccessor(): Boolean {
        return (flags and STATIC_FIELD_ACCESSOR) != 0
    }

    fun isJumboOpcode(): Boolean {
        return (flags and JUMBO_OPCODE) != 0
    }

    fun canInitializeReference(): Boolean {
        return (flags and CAN_INITIALIZE_REFERENCE) != 0
    }

    companion object {
        //if the instruction can throw an exception
        const val CAN_THROW = FLAG_CAN_THROW

        //if the instruction is an odex only instruction
        const val ODEX_ONLY = FLAG_ODEX_ONLY

        //if execution can continue to the next instruction
        const val CAN_CONTINUE = FLAG_CAN_CONTINUE

        //if the instruction sets the "hidden" result register
        const val SETS_RESULT = FLAG_SETS_RESULT

        //if the instruction sets the value of it's first register
        const val SETS_REGISTER = FLAG_SETS_REGISTER

        //if the instruction sets the value of it's first register to a wide type
        const val SETS_WIDE_REGISTER = FLAG_SETS_WIDE_REGISTER

        //if the instruction is an iget-quick/iput-quick instruction
        const val QUICK_FIELD_ACCESSOR = FLAG_QUICK_FIELD_ACCESSOR

        //if the instruction is a *get-volatile/*put-volatile instruction
        const val VOLATILE_FIELD_ACCESSOR = FLAG_VOLATILE_FIELD_ACCESSOR

        //if the instruction is a static sget-*/sput-*instruction
        const val STATIC_FIELD_ACCESSOR = FLAG_STATIC_FIELD_ACCESSOR

        //if the instruction is a jumbo instruction
        const val JUMBO_OPCODE = FLAG_JUMBO_OPCODE

        //if the instruction can initialize an uninitialized object reference
        const val CAN_INITIALIZE_REFERENCE = FLAG_CAN_INITIALIZE_REFERENCE
    }
}

private const val FLAG_CAN_THROW = 0x1
private const val FLAG_ODEX_ONLY = 0x2
private const val FLAG_CAN_CONTINUE = 0x4
private const val FLAG_SETS_RESULT = 0x8
private const val FLAG_SETS_REGISTER = 0x10
private const val FLAG_SETS_WIDE_REGISTER = 0x20
private const val FLAG_QUICK_FIELD_ACCESSOR = 0x40
private const val FLAG_VOLATILE_FIELD_ACCESSOR = 0x80
private const val FLAG_STATIC_FIELD_ACCESSOR = 0x100
private const val FLAG_JUMBO_OPCODE = 0x200
private const val FLAG_CAN_INITIALIZE_REFERENCE = 0x400

private class VersionConstraint(
    val apiRange: Range<Int>,
    val artVersionRange: Range<Int>,
    val opcodeValue: Int
)

private fun buildApiToValueMap(versionConstraints: List<VersionConstraint>): UnmodifiableRangeMap<Int, Short> {
    val apiToValueBuilder = UnmodifiableRangeMap.builder<Int, Short>()
    for (versionConstraint in versionConstraints) {
        if (!versionConstraint.apiRange.isEmpty()) {
            apiToValueBuilder.put(versionConstraint.apiRange, versionConstraint.opcodeValue.toShort())
        }
    }
    return apiToValueBuilder.build()
}

private fun buildArtVersionToValueMap(versionConstraints: List<VersionConstraint>): UnmodifiableRangeMap<Int, Short> {
    val artVersionToValueBuilder = UnmodifiableRangeMap.builder<Int, Short>()
    for (versionConstraint in versionConstraints) {
        if (!versionConstraint.artVersionRange.isEmpty()) {
            artVersionToValueBuilder.put(
                versionConstraint.artVersionRange, versionConstraint.opcodeValue.toShort()
            )
        }
    }
    return artVersionToValueBuilder.build()
}

private fun firstApi(opcodeValue: Int, api: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.atLeast(api), Range.openClosed(0, 0), opcodeValue))
}

private fun lastApi(opcodeValue: Int, api: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.atMost(api), Range.openClosed(0, 0), opcodeValue))
}

private fun betweenApi(opcodeValue: Int, minApi: Int, maxApi: Int): List<VersionConstraint> {
    return listOf(
        VersionConstraint(Range.closed(minApi, maxApi), Range.openClosed(0, 0), opcodeValue)
    )
}

private fun firstArtVersion(opcodeValue: Int, artVersion: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.openClosed(0, 0), Range.atLeast(artVersion), opcodeValue))
}

private fun lastArtVersion(opcodeValue: Int, artVersion: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.openClosed(0, 0), Range.atMost(artVersion), opcodeValue))
}

private fun allVersions(opcodeValue: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.allValues(), Range.allValues(), opcodeValue))
}

private fun allApis(opcodeValue: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.allValues(), Range.openClosed(0, 0), opcodeValue))
}

private fun allArtVersions(opcodeValue: Int): List<VersionConstraint> {
    return listOf(VersionConstraint(Range.openClosed(0, 0), Range.allValues(), opcodeValue))
}

private fun combine(vararg versionConstraints: List<VersionConstraint>): List<VersionConstraint> {
    val combinedList = ArrayList<VersionConstraint>()
    for (versionConstraintList in versionConstraints) {
        combinedList.addAll(versionConstraintList)
    }
    return combinedList
}

