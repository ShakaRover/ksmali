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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.smali.SmaliOptions
import com.android.tools.smali.smali.assemble
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Losslessness on the assembler's *own* output: `assemble(smali)` is the input to the round-trip,
 * so the writer is expected to reach exactly the same bytes after a baksmali/smali cycle.
 */
class AssemblerOutputRoundtripTest {

    @Test
    fun assemblerOutputRoundTripsByteIdentical() {
        for ((name, source) in SOURCES) {
            withSource(name, source) { srcDir, workDir ->
                val first = File(workDir, "$name-first.dex")
                Assert.assertTrue(
                    "$name: initial assembly failed",
                    assemble(options(first), srcDir.absolutePath)
                )

                val disDir = File(workDir, "$name-dis")
                disassemble(first.readBytes(), disDir)

                val second = File(workDir, "$name-second.dex")
                Assert.assertTrue(
                    "$name: reassembly failed",
                    assemble(options(second), disDir.absolutePath)
                )

                Assert.assertArrayEquals(
                    "$name: assemble(disassemble(x)) != x",
                    first.readBytes(), second.readBytes()
                )

                val disDir2 = File(workDir, "$name-dis2")
                disassemble(second.readBytes(), disDir2)
                Assert.assertEquals(
                    "$name: disassembly is not idempotent",
                    tree(disDir), tree(disDir2)
                )
            }
        }
    }

    private fun disassemble(bytes: ByteArray, outDir: File) {
        val dexFile = DexBackedDexFile(Opcodes.forApi(API), bytes)
        val options = BaksmaliOptions().apply { apiLevel = API }
        Assert.assertTrue(
            "disassembly of ${outDir.name} failed",
            disassembleDexFile(dexFile, outDir, 1, options)
        )
    }

    private fun options(output: File) = SmaliOptions().apply {
        apiLevel = API
        outputDexFile = output.absolutePath
        jobs = 1
    }

    private fun tree(dir: File): Map<String, String> =
        dir.walkTopDown().filter { it.isFile }
            .associate { it.relativeTo(dir).path to it.readText() }

    private fun withSource(name: String, source: String, block: (File, File) -> Unit) {
        val root = Files.createTempDirectory("smali-roundtrip").toFile()
        try {
            val srcDir = File(root, "src").apply { mkdirs() }
            File(srcDir, "$name.smali").writeText(source)
            block(srcDir, root)
        } finally {
            root.deleteRecursively()
        }
    }

    private companion object {
        private const val API = 30

        private val SOURCES = linkedMapOf(
            "Rich" to """
                .class public LRich;
                .super Ljava/lang/Object;
                .source "Rich.java"

                .annotation runtime LRich; value = "self"
                .end annotation

                .field public static final CONST:I = 0x7f070000
                .field public static name:Ljava/lang/String; = "hello"

                .method public constructor <init>()V
                    .registers 1
                    .line 3
                    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                    return-void
                .end method

                .method public static compute(IJ)I
                    .registers 6
                    .param p0, "arg0"
                    .line 10
                    .local v0, "tmp":I
                    const/high16 v0, 0x3000
                    const-wide/high16 v1, 0x4000
                    if-eqz p0, :zero
                    add-int v0, p0, p0
                    goto :done
                    :zero
                    const/4 v0, 0x0
                    :done
                    return v0
                .end method
            """.trimIndent() + "\n",

            "High16" to """
                .class public LHigh16;
                .super Ljava/lang/Object;

                .method public static wide()J
                    .registers 2
                    const-wide/high16 v0, 0x4000000000000000L
                    return-wide v0
                .end method
            """.trimIndent() + "\n",
        )
    }
}
