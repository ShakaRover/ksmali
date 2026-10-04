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

package com.android.tools.smali.smali

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The suspend entry points must be usable from inside a coroutine and must produce exactly the
 * same bytes as the blocking [assemble] wrappers, for both the list and vararg overloads.
 */
class SuspendApiTest {

    @Test
    fun listOverloadMatchesBlockingOutput() = withTempDir { dir ->
        val src = File(dir, "src").apply { mkdirs() }
        writeFixture(src)

        val blocking = File(dir, "blocking.dex")
        val suspending = File(dir, "suspending.dex")

        Assert.assertTrue(
            assemble(SmaliOptions().apply { outputDexFile = blocking.absolutePath; jobs = 4 },
                src.absolutePath)
        )
        Assert.assertTrue(runBlocking {
            assembleSuspend(
                SmaliOptions().apply { outputDexFile = suspending.absolutePath; jobs = 4 },
                listOf(src.absolutePath)
            )
        })

        Assert.assertArrayEquals("suspend and blocking output differ", blocking.readBytes(), suspending.readBytes())
    }

    @Test
    fun varargOverloadMatchesBlockingOutput() = withTempDir { dir ->
        val src = File(dir, "src").apply { mkdirs() }
        writeFixture(src)

        val blocking = File(dir, "blocking.dex")
        val suspending = File(dir, "suspending.dex")

        Assert.assertTrue(
            assemble(SmaliOptions().apply { outputDexFile = blocking.absolutePath; jobs = 2 },
                src.absolutePath)
        )
        Assert.assertTrue(runBlocking {
            assembleSuspend(
                SmaliOptions().apply { outputDexFile = suspending.absolutePath; jobs = 2 },
                src.absolutePath
            )
        })

        Assert.assertArrayEquals("suspend and blocking output differ", blocking.readBytes(), suspending.readBytes())
    }

    @Test
    fun suspendReportsFailureLikeBlocking() = withTempDir { dir ->
        val src = File(dir, "src").apply { mkdirs() }
        File(src, "Bad.smali").writeText(
            ".class public LBad;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public foo()V\n" +
                "    .registers 1\n" +
                "    return-void\n"
        )
        val suspending = File(dir, "suspend.dex")
        Assert.assertFalse(runBlocking {
            assembleSuspend(
                SmaliOptions().apply { outputDexFile = suspending.absolutePath; jobs = 1 },
                src.absolutePath
            )
        })
    }

    private fun writeFixture(root: File) {
        File(root, "Hello.smali").writeText(
            ".class public LHello;\n" +
                ".super Ljava/lang/Object;\n\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 2\n" +
                "    const-string v0, \"hello\"\n" +
                "    return-void\n" +
                ".end method\n"
        )
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("smali-suspend").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
