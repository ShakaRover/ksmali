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

package com.android.tools.smali.baksmali

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.smali.SmaliOptions
import com.android.tools.smali.smali.assemble
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * [disassembleDexFileSuspend] must be callable from a coroutine and must produce exactly the same
 * `.smali` tree as the blocking [disassembleDexFile] wrapper, with and without a class filter.
 */
class SuspendApiTest {

    @Test
    fun suspendMatchesBlockingOutput() = withFixture { dexFile, dir ->
        val blocking = File(dir, "blocking")
        val suspending = File(dir, "suspending")

        Assert.assertTrue(disassembleDexFile(dexFile, blocking, 3, BaksmaliOptions()))
        Assert.assertTrue(runBlocking {
            disassembleDexFileSuspend(dexFile, suspending, 3, BaksmaliOptions())
        })

        assertTreesEqual(blocking, suspending)
    }

    @Test
    fun suspendWithClassFilterMatchesBlockingOutput() = withFixture { dexFile, dir ->
        val blocking = File(dir, "blocking-filtered")
        val suspending = File(dir, "suspending-filtered")
        val classes = listOf("Lpkg/A;", "Lpkg/C;")

        Assert.assertTrue(disassembleDexFile(dexFile, blocking, 2, BaksmaliOptions(), classes))
        Assert.assertTrue(runBlocking {
            disassembleDexFileSuspend(dexFile, suspending, 2, BaksmaliOptions(), classes)
        })

        assertTreesEqual(blocking, suspending)
        Assert.assertTrue(File(blocking, "pkg/A.smali").isFile)
        Assert.assertFalse(File(blocking, "pkg/B.smali").exists())
    }

    private fun assertTreesEqual(expected: File, actual: File) {
        val expectedFiles = expected.walkTopDown().filter { it.isFile }
            .associateBy { it.relativeTo(expected).path }
        val actualFiles = actual.walkTopDown().filter { it.isFile }
            .associateBy { it.relativeTo(actual).path }
        Assert.assertEquals("file set differs", expectedFiles.keys, actualFiles.keys)
        for ((path, file) in expectedFiles) {
            Assert.assertArrayEquals(
                "file $path differs",
                file.readBytes(),
                actualFiles.getValue(path).readBytes()
            )
        }
    }

    private fun withFixture(block: (com.android.tools.smali.dexlib2.iface.DexFile, File) -> Unit) {
        val dir = Files.createTempDirectory("baksmali-suspend").toFile()
        try {
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "A.smali").writeText(classFixture("Lpkg/A;", "Lpkg/B;"))
            File(src, "B.smali").writeText(classFixture("Lpkg/B;", "Lpkg/C;"))
            File(src, "C.smali").writeText(classFixture("Lpkg/C;", "Lpkg/A;"))
            val dex = File(dir, "input.dex")
            Assert.assertTrue(
                assemble(
                    SmaliOptions().apply { outputDexFile = dex.absolutePath; jobs = 3 },
                    src.absolutePath
                )
            )
            block(DexFileFactory.loadDexFile(dex, Opcodes.forApi(15)), dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun classFixture(self: String, other: String) =
        ".class public $self\n" +
            ".super Ljava/lang/Object;\n\n" +
            ".method public static main([Ljava/lang/String;)V\n" +
            "    .registers 2\n" +
            "    const-string v0, \"$self\"\n" +
            "    invoke-static {v0}, $other->main([Ljava/lang/String;)V\n" +
            "    return-void\n" +
            ".end method\n"
}
