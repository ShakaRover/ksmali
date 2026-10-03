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

package com.android.tools.smali.baksmali

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.analysis.ClassPath
import com.android.tools.smali.dexlib2.iface.DexFile
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.net.URISyntaxException
import java.net.URL

class AnalysisTest {

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun ConstructorTest() {
        runTest("ConstructorTest", true)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun RegisterEqualityOnMergeTest() {
        runTest("RegisterEqualityOnMergeTest", true)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun UninitRefIdentityTest() {
        runTest("UninitRefIdentityTest", true)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun InstanceOfTest() {
        runTest("InstanceOfTest", true, true)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun MultipleStartInstructionsTest() {
        runTest("MultipleStartInstructionsTest", true)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun DuplicateTest() {
        runTest("DuplicateTest", false)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun LocalTest() {
        runTest("LocalTest", false)
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun runTest(test: String, registerInfo: Boolean) {
        runTest(test, registerInfo, false)
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun runTest(test: String, registerInfo: Boolean, isArt: Boolean) {
        val dexFilePath = "$test${File.separatorChar}classes.dex"

        val dexFile: DexFile = DexFileFactory.loadDexFile(findResource(dexFilePath), Opcodes.getDefault())

        val options = BaksmaliOptions()
        if (registerInfo) {
            options.registerInfo = BaksmaliOptions.ALL or BaksmaliOptions.FULLMERGE
            if (isArt) {
                options.classPath = ClassPath(mutableListOf(), true, 56)
            } else {
                options.classPath = ClassPath()
            }
        }
        options.implicitReferences = false

        for (classDef in dexFile.classes) {
            val stringWriter = StringWriter()
            val writer = BaksmaliWriter(stringWriter)
            val classDefinition = ClassDefinition(options, classDef)
            classDefinition.writeTo(writer)
            writer.close()

            val className = classDef.type
            val smaliPath =
                "$test${File.separatorChar}${className.substring(1, className.length - 1)}.smali"
            val smaliContents = readResource(smaliPath)

            Assert.assertEquals(
                BaksmaliTestUtils.normalizeWhitespace(smaliContents),
                BaksmaliTestUtils.normalizeWhitespace(stringWriter.toString()))
        }
    }

    @Throws(URISyntaxException::class)
    private fun findResource(resource: String): File {
        val resUrl: URL = Resources.getResource(resource)
        return File(resUrl.toURI())
    }

    @Throws(URISyntaxException::class, IOException::class)
    private fun readResource(resource: String): String {
        val url: URL = Resources.getResource(resource)
        return Resources.toString(url, Charsets.UTF_8)
    }
}
