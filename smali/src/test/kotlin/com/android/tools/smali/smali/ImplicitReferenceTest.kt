/*
 * Copyright 2016, Google LLC
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

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue
import org.junit.Assert
import org.junit.Test

/**
 * Tests for method/field references that use an implicit type
 */
class ImplicitReferenceTest {
    @Test
    fun testImplicitMethodReference() {
        val classDef: ClassDef = compileSmali("" +
            ".class public LHelloWorld;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public static main([Ljava/lang/String;)V\n" +
            "    .registers 1\n" +
            "    invoke-static {p0}, toString()V\n" +
            "    invoke-static {p0}, V()V\n" +
            "    invoke-static {p0}, I()V\n" +
            "    return-void\n" +
            ".end method")

        var mainMethod: Method? = null
        for (method in classDef.methods) {
            if (method.name == "main") {
                mainMethod = method
            }
        }
        Assert.assertNotNull(mainMethod)

        val methodImpl = mainMethod!!.implementation
        Assert.assertNotNull(methodImpl)

        val instructions: List<Instruction> = Lists.newArrayList(methodImpl!!.instructions)

        var instruction = instructions[0] as Instruction35c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.opcode)
        var method = instruction.reference as MethodReference
        Assert.assertEquals(classDef.type, method.definingClass)
        Assert.assertEquals("toString", method.name)

        instruction = instructions[1] as Instruction35c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.opcode)
        method = instruction.reference as MethodReference
        Assert.assertEquals(classDef.type, method.definingClass)
        Assert.assertEquals("V", method.name)

        instruction = instructions[2] as Instruction35c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.opcode)
        method = instruction.reference as MethodReference
        Assert.assertEquals(classDef.type, method.definingClass)
        Assert.assertEquals("I", method.name)
    }

    @Test
    fun testImplicitMethodLiteral() {
        val classDef: ClassDef = compileSmali("" +
            ".class public LHelloWorld;\n" +
            ".super Ljava/lang/Object;\n" +
            ".field public static field1:Ljava/lang/reflect/Method; = toString()V\n" +
            ".field public static field2:Ljava/lang/reflect/Method; = V()V\n" +
            ".field public static field3:Ljava/lang/reflect/Method; = I()V\n" +
            ".field public static field4:Ljava/lang/Class; = I")

        val fields = Maps.newHashMap<String, Field>()
        for (field in classDef.fields) {
            fields[field.name] = field
        }

        var field = fields["field1"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.METHOD, field!!.initialValue!!.valueType)
        var methodEncodedValue = field!!.initialValue as MethodEncodedValue
        Assert.assertEquals(classDef.type, methodEncodedValue.value.definingClass)
        Assert.assertEquals("toString", methodEncodedValue.value.name)

        field = fields["field2"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.METHOD, field!!.initialValue!!.valueType)
        methodEncodedValue = field!!.initialValue as MethodEncodedValue
        Assert.assertEquals(classDef.type, methodEncodedValue.value.definingClass)
        Assert.assertEquals("V", methodEncodedValue.value.name)

        field = fields["field3"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.METHOD, field!!.initialValue!!.valueType)
        methodEncodedValue = field!!.initialValue as MethodEncodedValue
        Assert.assertEquals(classDef.type, methodEncodedValue.value.definingClass)
        Assert.assertEquals("I", methodEncodedValue.value.name)

        field = fields["field4"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.TYPE, field!!.initialValue!!.valueType)
        val typeEncodedValue = field!!.initialValue as TypeEncodedValue
        Assert.assertEquals("I", typeEncodedValue.value)
    }

    @Test
    fun testImplicitFieldReference() {
        val classDef: ClassDef = compileSmali("" +
            ".class public LHelloWorld;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public static main([Ljava/lang/String;)V\n" +
            "    .registers 1\n" +
            "    sget-object v0, someField:I\n" +
            "    sget-object v0, V:I\n" +
            "    sget-object v0, I:I\n" +
            "    return-void\n" +
            ".end method")

        var mainMethod: Method? = null
        for (method in classDef.methods) {
            if (method.name == "main") {
                mainMethod = method
            }
        }
        Assert.assertNotNull(mainMethod)

        val methodImpl = mainMethod!!.implementation
        Assert.assertNotNull(methodImpl)

        val instructions: List<Instruction> = Lists.newArrayList(methodImpl!!.instructions)

        var instruction = instructions[0] as Instruction21c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.opcode)
        var field = instruction.reference as FieldReference
        Assert.assertEquals(classDef.type, field.definingClass)
        Assert.assertEquals("someField", field.name)

        instruction = instructions[1] as Instruction21c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.opcode)
        field = instruction.reference as FieldReference
        Assert.assertEquals(classDef.type, field.definingClass)
        Assert.assertEquals("V", field.name)

        instruction = instructions[2] as Instruction21c
        Assert.assertNotNull(instruction)
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.opcode)
        field = instruction.reference as FieldReference
        Assert.assertEquals(classDef.type, field.definingClass)
        Assert.assertEquals("I", field.name)
    }

    @Test
    fun testImplicitFieldLiteral() {
        val classDef: ClassDef = compileSmali("" +
            ".class public LHelloWorld;\n" +
            ".super Ljava/lang/Object;\n" +
            ".field public static field1:Ljava/lang/reflect/Field; = someField:I\n" +
            ".field public static field2:Ljava/lang/reflect/Field; = V:I\n" +
            ".field public static field3:Ljava/lang/reflect/Field; = I:I\n")

        val fields = Maps.newHashMap<String, Field>()
        for (field in classDef.fields) {
            fields[field.name] = field
        }

        var field = fields["field1"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.FIELD, field!!.initialValue!!.valueType)
        var fieldEncodedValue = field!!.initialValue as FieldEncodedValue
        Assert.assertEquals(classDef.type, fieldEncodedValue.value.definingClass)
        Assert.assertEquals("someField", fieldEncodedValue.value.name)

        field = fields["field2"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.FIELD, field!!.initialValue!!.valueType)
        fieldEncodedValue = field!!.initialValue as FieldEncodedValue
        Assert.assertEquals(classDef.type, fieldEncodedValue.value.definingClass)
        Assert.assertEquals("V", fieldEncodedValue.value.name)

        field = fields["field3"]
        Assert.assertNotNull(field)
        Assert.assertNotNull(field!!.initialValue)
        Assert.assertEquals(ValueType.FIELD, field!!.initialValue!!.valueType)
        fieldEncodedValue = field!!.initialValue as FieldEncodedValue
        Assert.assertEquals(classDef.type, fieldEncodedValue.value.definingClass)
        Assert.assertEquals("I", fieldEncodedValue.value.name)
    }
}
