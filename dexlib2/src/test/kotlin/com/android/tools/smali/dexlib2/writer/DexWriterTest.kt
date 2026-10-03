/*
 * Copyright 2014, Google LLC
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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.AnnotationVisibility
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.value.ImmutableAnnotationEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableNullEncodedValue
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.util.ArrayList

class DexWriterTest {
    @Test
    fun testAnnotationElementOrder() {
        // Elements are out of order wrt to the element name
        val elements = setOf(
            ImmutableAnnotationElement("zabaglione", ImmutableNullEncodedValue.INSTANCE),
            ImmutableAnnotationElement("blah", ImmutableNullEncodedValue.INSTANCE)
        )

        val annotation = ImmutableAnnotation(AnnotationVisibility.RUNTIME,
            "Lorg/test/anno;", elements)

        val classDef = ImmutableClassDef("Lorg/test/blah;",
            0, "Ljava/lang/Object;", null, null, setOf(annotation), null, null)

        val dataStore = MemoryDataStore()

        try {
            DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.default, setOf(classDef)))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

        val dexFile = DexBackedDexFile(Opcodes.default, dataStore.buffer)
        val dbClassDef = dexFile.classes.firstOrNull()
        Assert.assertNotNull(dbClassDef)
        val dbAnnotation = dbClassDef!!.annotations.firstOrNull()
        Assert.assertNotNull(dbAnnotation)
        val dbElements = ArrayList(dbAnnotation!!.elements)

        // Ensure that the elements were written out in sorted order
        Assert.assertEquals(2, dbElements.size)
        Assert.assertEquals("blah", dbElements[0].name)
        Assert.assertEquals("zabaglione", dbElements[1].name)
    }

    @Test
    fun testEncodedAnnotationElementOrder() {
        // Elements are out of order wrt to the element name
        val encodedElements = setOf(
            ImmutableAnnotationElement("zabaglione", ImmutableNullEncodedValue.INSTANCE),
            ImmutableAnnotationElement("blah", ImmutableNullEncodedValue.INSTANCE)
        )

        val encodedAnnotations = ImmutableAnnotationEncodedValue("Lan/encoded/annotation", encodedElements)

        val elements = setOf(
            ImmutableAnnotationElement("encoded_annotation", encodedAnnotations)
        )

        val annotation = ImmutableAnnotation(AnnotationVisibility.RUNTIME,
            "Lorg/test/anno;", elements)

        val classDef = ImmutableClassDef("Lorg/test/blah;",
            0, "Ljava/lang/Object;", null, null, setOf(annotation), null, null)

        val dataStore = MemoryDataStore()

        try {
            DexPool.writeTo(dataStore, ImmutableDexFile(Opcodes.default, setOf(classDef)))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

        val dexFile = DexBackedDexFile(Opcodes.default, dataStore.buffer)
        val dbClassDef = dexFile.classes.firstOrNull()
        Assert.assertNotNull(dbClassDef)
        val dbAnnotation = dbClassDef!!.annotations.firstOrNull()
        Assert.assertNotNull(dbAnnotation)

        val element = dbAnnotation!!.elements.firstOrNull()
        val dbAnnotationEncodedValue = element!!.value as AnnotationEncodedValue

        val dbElements = ArrayList(dbAnnotationEncodedValue.elements)

        // Ensure that the elements were written out in sorted order
        Assert.assertEquals(2, dbElements.size)
        Assert.assertEquals("blah", dbElements[0].name)
        Assert.assertEquals("zabaglione", dbElements[1].name)
    }
}
