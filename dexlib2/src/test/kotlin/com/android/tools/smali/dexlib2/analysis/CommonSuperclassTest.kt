/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.google.common.collect.ImmutableSet
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class CommonSuperclassTest {
    // object tree:
    // object
    //   one
    //     onetwo
    //       onetwothree
    //     onethree
    // five (undefined class)
    //   fivetwo
    //     fivetwothree
    //   fivethree

    private val oldClassPath: ClassPath
    private val newClassPath: ClassPath


    init {
        val classes = ImmutableSet.of(
                TestUtils.makeClassDef("Ljava/lang/Object;", null),
                TestUtils.makeClassDef("Ltest/one;", "Ljava/lang/Object;"),
                TestUtils.makeClassDef("Ltest/two;", "Ljava/lang/Object;"),
                TestUtils.makeClassDef("Ltest/onetwo;", "Ltest/one;"),
                TestUtils.makeClassDef("Ltest/onetwothree;", "Ltest/onetwo;"),
                TestUtils.makeClassDef("Ltest/onethree;", "Ltest/one;"),
                TestUtils.makeClassDef("Ltest/fivetwo;", "Ltest/five;"),
                TestUtils.makeClassDef("Ltest/fivetwothree;", "Ltest/fivetwo;"),
                TestUtils.makeClassDef("Ltest/fivethree;", "Ltest/five;"),
                TestUtils.makeInterfaceDef("Ljava/lang/Cloneable;"),
                TestUtils.makeInterfaceDef("Ljava/io/Serializable;"),

                // basic class and interface
                TestUtils.makeClassDef("Liface/classiface1;", "Ljava/lang/Object;", "Liface/iface1;"),
                TestUtils.makeInterfaceDef("Liface/iface1;"),

                // a more complex interface tree
                TestUtils.makeInterfaceDef("Liface/base1;"),
                // implements undefined interface
                TestUtils.makeInterfaceDef("Liface/sub1;", "Liface/base1;", "Liface/base2;"),
                // this implements sub1, so that its interfaces can't be fully resolved either
                TestUtils.makeInterfaceDef("Liface/sub2;", "Liface/base1;", "Liface/sub1;"),
                TestUtils.makeInterfaceDef("Liface/sub3;", "Liface/base1;"),
                TestUtils.makeInterfaceDef("Liface/sub4;", "Liface/base1;", "Liface/sub3;"),
                TestUtils.makeClassDef("Liface/classsub1;", "Ljava/lang/Object;", "Liface/sub1;"),
                TestUtils.makeClassDef("Liface/classsub2;", "Ljava/lang/Object;", "Liface/sub2;"),
                TestUtils.makeClassDef("Liface/classsub3;", "Ljava/lang/Object;", "Liface/sub3;",
                        "Liface/base;"),
                TestUtils.makeClassDef("Liface/classsub4;", "Ljava/lang/Object;", "Liface/sub3;",
                        "Liface/sub4;"),
                TestUtils.makeClassDef("Liface/classsubsub4;", "Liface/classsub4;"),
                TestUtils.makeClassDef("Liface/classsub1234;", "Ljava/lang/Object;", "Liface/sub1;",
                        "Liface/sub2;", "Liface/sub3;", "Liface/sub4;"))

        oldClassPath = ClassPath(DexClassProvider(ImmutableDexFile(Opcodes.getDefault(), classes)))
        newClassPath = ClassPath(mutableListOf(DexClassProvider(
                ImmutableDexFile(Opcodes.forArtVersion(72), classes))), true, 72)
    }

    fun superclassTest(classPath: ClassPath, commonSuperclass: String,
                               type1: String, type2: String) {
        val commonSuperclassProto = classPath.getClass(commonSuperclass)
        val type1Proto = classPath.getClass(type1)
        val type2Proto = classPath.getClass(type2)

        Assert.assertSame(commonSuperclassProto, type1Proto.getCommonSuperclass(type2Proto))
        Assert.assertSame(commonSuperclassProto, type2Proto.getCommonSuperclass(type1Proto))
    }

    fun superclassTest(commonSuperclass: String, type1: String, type2: String) {
        superclassTest(oldClassPath, commonSuperclass, type1, type2)
        superclassTest(newClassPath, commonSuperclass, type1, type2)
    }

    @Test
    @Throws(IOException::class)
    fun testGetCommonSuperclass() {
        val objectType = "Ljava/lang/Object;"
        val unknown = "Ujava/lang/Object;"
        val one = "Ltest/one;"
        val two = "Ltest/two;"
        val onetwo = "Ltest/onetwo;"
        val onetwothree = "Ltest/onetwothree;"
        val onethree = "Ltest/onethree;"
        val five = "Ltest/five;"
        val fivetwo = "Ltest/fivetwo;"
        val fivetwothree = "Ltest/fivetwothree;"
        val fivethree = "Ltest/fivethree;"

        // same objectType        
        superclassTest(objectType, objectType, objectType)
        superclassTest(unknown, unknown, unknown)
        superclassTest(one, one, one)
        superclassTest(onetwo, onetwo, onetwo)
        superclassTest(onetwothree, onetwothree, onetwothree)
        superclassTest(onethree, onethree, onethree)
        superclassTest(five, five, five)
        superclassTest(fivetwo, fivetwo, fivetwo)
        superclassTest(fivetwothree, fivetwothree, fivetwothree)
        superclassTest(fivethree, fivethree, fivethree)
        
        // same value, but different objectType
        Assert.assertEquals(
                onetwo,
                oldClassPath.getClass(onetwo).getCommonSuperclass(ClassProto(oldClassPath, onetwo)).type)

        Assert.assertEquals(
                onetwo,
                newClassPath.getClass(onetwo).getCommonSuperclass(ClassProto(newClassPath, onetwo)).type)

        // other objectType is superclass
        superclassTest(objectType, objectType, one)

        // other objectType is superclass two levels up
        superclassTest(objectType, objectType, onetwo)

        // unknown and non-objectType class
        superclassTest(unknown, one, unknown)

        // unknown and objectType class
        superclassTest(objectType, objectType, unknown)

        // siblings
        superclassTest(one, onetwo, onethree)

        // nephew
        superclassTest(one, onethree, onetwothree)

        // unrelated
        superclassTest(objectType, one, two)

        // undefined superclass and objectType
        superclassTest(objectType, fivetwo, objectType)

        // undefined class and unrelated type
        superclassTest(unknown, one, five)

        // undefined superclass and unrelated type
        superclassTest(unknown, one, fivetwo)

        // undefined ancestor and unrelated type
        superclassTest(unknown, one, fivetwothree)

        // undefined class and direct subclass
        superclassTest(five, five, fivetwo)

        // undefined class and descendent
        superclassTest(five, five, fivetwothree)

        // undefined superclass and direct subclass
        superclassTest(fivetwo, fivetwo, fivetwothree)

        // siblings with undefined superclass
        superclassTest(five, fivetwo, fivethree)

        // undefined superclass and nephew
        superclassTest(five, fivethree, fivetwothree)
    }

    @Test
    fun testGetCommonSuperclass_interfaces() {
        val classiface1 = "Liface/classiface1;"
        val iface1 = "Liface/iface1;"
        val base1 = "Liface/base1;"
        val base2 = "Liface/base2;"
        val sub1 = "Liface/sub1;"
        val sub2 = "Liface/sub2;"
        val sub3 = "Liface/sub3;"
        val sub4 = "Liface/sub4;"
        val classsub1 = "Liface/classsub1;"
        val classsub2 = "Liface/classsub2;"
        val classsub3 = "Liface/classsub3;"
        val classsub4 = "Liface/classsub4;"
        val classsubsub4 = "Liface/classsubsub4;"
        val classsub1234 = "Liface/classsub1234;"
        val objectType = "Ljava/lang/Object;"
        val unknown = "Ujava/lang/Object;"

        superclassTest(iface1, classiface1, iface1)

        superclassTest(base1, base1, base1)
        superclassTest(base1, base1, sub1)
        superclassTest(base1, base1, classsub1)
        superclassTest(base1, base1, sub2)
        superclassTest(base1, base1, classsub2)
        superclassTest(base1, base1, sub3)
        superclassTest(base1, base1, classsub3)
        superclassTest(base1, base1, sub4)
        superclassTest(base1, base1, classsub4)
        superclassTest(base1, base1, classsubsub4)
        superclassTest(base1, base1, classsub1234)

        superclassTest(objectType, sub3, iface1)
        superclassTest(unknown, sub2, iface1)
        superclassTest(unknown, sub1, iface1)

        superclassTest(base2, base2, sub1)
        superclassTest(base2, base2, classsub1)
        superclassTest(base2, base2, sub2)
        superclassTest(base2, base2, classsub2)
        superclassTest(base2, base2, classsub1234)

        superclassTest(unknown, iface1, classsub1234)

        superclassTest(sub1, sub1, classsub1)

        superclassTest(sub2, sub2, classsub2)
        superclassTest(sub1, sub1, classsub2)

        superclassTest(sub3, sub3, classsub3)

        superclassTest(sub4, sub4, classsub4)
        superclassTest(sub3, sub3, classsub4)
        superclassTest(objectType, sub2, classsub4)
        superclassTest(objectType, sub1, classsub4)

        superclassTest(sub1, sub2, sub1)

        superclassTest(sub1, sub1, classsub1234)
        superclassTest(sub2, sub2, classsub1234)
        superclassTest(sub3, sub3, classsub1234)
        superclassTest(sub4, sub4, classsub1234)

        superclassTest(unknown, sub3, classsub1)
        superclassTest(unknown, sub4, classsub1)
        superclassTest(unknown, sub3, classsub2)
        superclassTest(unknown, sub4, classsub2)

        superclassTest(unknown, sub4, base2)
        superclassTest(unknown, classsub4, base2)
    }

    @Test
    @Throws(IOException::class)
    fun testGetCommonSuperclass_arrays() {
        val objectType = "Ljava/lang/Object;"
        val one = "Ltest/one;"
        val unknown = "Ujava/lang/Object;"

        val cloneable = "Ljava/lang/Cloneable;"
        val serializable = "Ljava/io/Serializable;"

        val object1 = "[Ljava/lang/Object;"
        val one1 = "[Ltest/one;"
        val one2 = "[[Ltest/one;"
        val two1 = "[Ltest/two;"
        val onetwo1 = "[Ltest/onetwo;"
        val onetwo2 = "[[Ltest/onetwo;"
        val onethree1 = "[Ltest/onethree;"
        val onethree2 = "[[Ltest/onethree;"
        val five = "Ltest/five;"
        val five1 = "[Ltest/five;"
        val unknown1 = "[Ujava/lang/Object;"

        val int1 = "[I"
        val int2 = "[[I"
        val float1 = "[F"

        superclassTest(one1, one1, one1)
        superclassTest(object1, object1, one1)
        superclassTest(one1, onetwo1, onethree1)
        superclassTest(one1, one1, onethree1)
        superclassTest(object1, one1, two1)

        superclassTest(one2, one2, one2)
        superclassTest(one2, one2, onetwo2)
        superclassTest(one2, onetwo2, onethree2)
        superclassTest(object1, one1, one2)
        superclassTest(object1, two1, one2)

        superclassTest(unknown1, five1, one1)
        superclassTest(object1, five1, one2)

        superclassTest(unknown1, one1, unknown1)

        superclassTest(objectType, one1, one)
        superclassTest(objectType, object1, one)
        superclassTest(objectType, onetwo1, one)
        superclassTest(objectType, five1, one)
        superclassTest(objectType, one2, one)

        superclassTest(objectType, one1, unknown)
        superclassTest(objectType, unknown1, unknown)

        superclassTest(cloneable, one1, cloneable)
        superclassTest(serializable, one1, serializable)

        superclassTest(objectType, one1, five)

        superclassTest(int1, int1, int1)
        superclassTest(objectType, int1, float1)
        superclassTest(objectType, int1, int2)
    }
}
