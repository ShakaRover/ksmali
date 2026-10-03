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
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Regression guard for the parallel assembly/disassembly paths: the output must not depend on
 * the `-j/--jobs` value, nor on the interleaving of the workers.
 *
 * The fixture is deliberately shaped to touch the shared state of the parallel paths: several
 * packages/classes (per-directory filename maps), synthetic `access$` accessors that
 * [com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver] must match (its `DexBackedClassDef`
 * lazy caches are shared between workers), and runtime annotations and string constants (the
 * `Builder*Pool` concurrent maps).
 */
class ParallelDeterminismTest {

    @Test
    fun assembleIsDeterministicAcrossJobCounts() {
        withFixture { src ->
            val outputs = mutableListOf<ByteArray>()
            for (jobs in JOBS + List(REPEATS_OF_8) { 8 }) {
                val out = File(src.parentFile, "asm-j$jobs-${outputs.size}.dex")
                val options = SmaliOptions().apply {
                    apiLevel = 15
                    outputDexFile = out.absolutePath
                    this.jobs = jobs
                }
                Assert.assertTrue("assemble failed for -j$jobs", assemble(options, src.absolutePath))
                outputs.add(out.readBytes())
            }
            for (i in 1 until outputs.size) {
                Assert.assertArrayEquals(
                    "assemble output differs between runs (run 0 vs run $i)",
                    outputs[0], outputs[i]
                )
            }
        }
    }

    @Test
    fun disassembleIsDeterministicAcrossJobCounts() {
        withFixture { src ->
            val dexFile = File(src.parentFile, "input.dex")
            val asmOptions = SmaliOptions().apply {
                apiLevel = 15
                outputDexFile = dexFile.absolutePath
                jobs = 8
            }
            Assert.assertTrue(assemble(asmOptions, src.absolutePath))
            val loaded = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(15))

            var reference: Map<String, ByteArray>? = null
            for (jobs in JOBS + List(REPEATS_OF_8) { 8 }) {
                val outDir = File(src.parentFile, "dis-j$jobs")
                Assert.assertTrue(
                    "disassemble failed for -j$jobs",
                    disassembleDexFile(loaded, outDir, jobs, BaksmaliOptions())
                )
                val tree = snapshot(outDir)
                val ref = reference
                if (ref == null) {
                    reference = tree
                } else {
                    Assert.assertEquals("file set differs for -j$jobs", ref.keys, tree.keys)
                    for ((path, bytes) in ref) {
                        Assert.assertTrue(
                            "file $path differs for -j$jobs",
                            bytes.contentEquals(tree.getValue(path))
                        )
                    }
                }
            }
        }
    }

    private fun snapshot(dir: File): Map<String, ByteArray> =
        dir.walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(dir).path to it.readBytes() }

    private fun withFixture(block: (File) -> Unit) {
        val root = Files.createTempDirectory("smali-parallel-det").toFile()
        try {
            val src = File(root, "src")
            writeFixtures(src)
            block(src)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun writeFixtures(root: File) {
        val pkg = File(root, "pkg")
        for (p in 0 until PACKAGE_COUNT) {
            File(pkg, "p$p").mkdirs()
        }

        File(pkg, "Anno.smali").writeText(
            ".class public interface abstract annotation Lpkg/Anno;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n"
        )

        for (i in 0 until CLASS_COUNT) {
            val p = i % PACKAGE_COUNT
            val self = "Lpkg/p$p/C$i;"
            val prevIndex = (i - 1 + CLASS_COUNT) % CLASS_COUNT
            val prevPkg = prevIndex % PACKAGE_COUNT
            val prev = "Lpkg/p$prevPkg/C$prevIndex;"
            val body = buildString {
                append(".class public $self\n")
                append(".super Ljava/lang/Object;\n")
                append("\n.annotation runtime Lpkg/Anno;\n    value = \"class-$i\"\n.end annotation\n")
                append("\n.field private x:I\n")
                append("\n.field public static final S:Ljava/lang/String; = \"string-value-$i\"\n")
                append(
                    "\n.method public constructor <init>()V\n    .locals 0\n" +
                        "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n    return-void\n.end method\n"
                )
                append(
                    "\n.method public static synthetic access\$000($self)I\n    .locals 1\n" +
                        "    iget v0, p0, $self->x:I\n    return v0\n.end method\n"
                )
                if (i > 0) {
                    append(
                        "\n.method public readPrev($prev)I\n    .locals 1\n" +
                            "    invoke-static {p1}, $prev->access\$000($prev)I\n" +
                            "    move-result v0\n    return v0\n.end method\n"
                    )
                }
            }
            File(pkg, "p$p/C$i.smali").writeText(body)
        }
    }

    private companion object {
        private const val PACKAGE_COUNT = 3
        private const val CLASS_COUNT = 24
        private const val REPEATS_OF_8 = 4
        private val JOBS = listOf(1, 2, 8)
    }
}
