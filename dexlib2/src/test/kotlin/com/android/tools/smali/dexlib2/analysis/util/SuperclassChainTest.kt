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

package com.android.tools.smali.dexlib2.analysis.util

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.analysis.ClassPath
import com.android.tools.smali.dexlib2.analysis.DexClassProvider
import com.android.tools.smali.dexlib2.analysis.TestUtils
import com.android.tools.smali.dexlib2.analysis.TypeProto
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class SuperclassChainTest {

    @Test
    @Throws(IOException::class)
    fun testGetSuperclassChain() {
        val objectClassDef = TestUtils.makeClassDef("Ljava/lang/Object;", null)
        val oneClassDef = TestUtils.makeClassDef("Ltest/one;", "Ljava/lang/Object;")
        val twoClassDef = TestUtils.makeClassDef("Ltest/two;", "Ltest/one;")
        val threeClassDef = TestUtils.makeClassDef("Ltest/three;", "Ltest/two;")

        val classes = ImmutableSet.of<ClassDef>(
            objectClassDef, oneClassDef, twoClassDef, threeClassDef
        )

        val classPath = ClassPath(DexClassProvider(ImmutableDexFile(Opcodes.getDefault(), classes)))

        val objectClassProto = classPath.getClass("Ljava/lang/Object;")
        val oneClassProto = classPath.getClass("Ltest/one;")
        val twoClassProto = classPath.getClass("Ltest/two;")
        val threeClassProto = classPath.getClass("Ltest/three;")

        Assert.assertEquals(
            ImmutableList.of<TypeProto>(),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(objectClassProto))
        )

        Assert.assertEquals(
            ImmutableList.of(objectClassProto),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(oneClassProto))
        )

        Assert.assertEquals(
            ImmutableList.of(oneClassProto, objectClassProto),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(twoClassProto))
        )

        Assert.assertEquals(
            ImmutableList.of(twoClassProto, oneClassProto, objectClassProto),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(threeClassProto))
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetSuperclassChain_Unresolved() {
        // Ltest/one; isn't defined

        val twoClassDef = TestUtils.makeClassDef("Ltest/two;", "Ltest/one;")
        val threeClassDef = TestUtils.makeClassDef("Ltest/three;", "Ltest/two;")
        val classes = ImmutableSet.of<ClassDef>(twoClassDef, threeClassDef)
        val classPath = ClassPath(DexClassProvider(ImmutableDexFile(Opcodes.getDefault(), classes)))

        val unknownClassProto = classPath.getUnknownClass()
        val oneClassProto = classPath.getClass("Ltest/one;")
        val twoClassProto = classPath.getClass("Ltest/two;")
        val threeClassProto = classPath.getClass("Ltest/three;")

        Assert.assertEquals(
            ImmutableList.of(oneClassProto, unknownClassProto),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(twoClassProto))
        )

        Assert.assertEquals(
            ImmutableList.of(twoClassProto, oneClassProto, unknownClassProto),
            ImmutableList.copyOf(TypeProtoUtils.getSuperclassChain(threeClassProto))
        )
    }
}
