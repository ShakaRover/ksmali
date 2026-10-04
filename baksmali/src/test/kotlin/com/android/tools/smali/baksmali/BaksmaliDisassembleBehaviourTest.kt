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
 * Behavioural coverage for [disassembleDexFile]: label naming, output-directory creation and the
 * class filter. These exercise the parts of `DisassembleCommand` that are not reachable through
 * plain option parsing.
 */
class BaksmaliDisassembleBehaviourTest {

    @Test
    fun sequentialLabelsRenameBranchLabels() {
        withDex(mapOf("Branch.smali" to BRANCH)) { dex ->
            withTempDir { root ->
                val normal = File(root, "normal")
                val sequential = File(root, "sequential")
                Assert.assertTrue(disassemble(dex, normal, BaksmaliOptions()))
                Assert.assertTrue(
                    disassemble(dex, sequential, BaksmaliOptions().apply { sequentialLabels = true })
                )

                val normalText = File(normal, "Branch.smali").readText()
                val sequentialText = File(sequential, "Branch.smali").readText()
                Assert.assertTrue("expected address label in $normalText", normalText.contains(":cond_4"))
                Assert.assertTrue("expected sequential label in $sequentialText", sequentialText.contains(":cond_0"))
                Assert.assertNotEquals(normalText, sequentialText)
            }
        }
    }

    @Test
    fun disassembleCreatesNestedOutputDirectories() {
        withDex(mapOf("Nested.smali" to NESTED)) { dex ->
            withTempDir { root ->
                val out = File(root, "does/not/exist/yet")
                Assert.assertFalse(out.exists())
                Assert.assertTrue(disassemble(dex, out, BaksmaliOptions()))
                Assert.assertTrue(out.isDirectory)
                Assert.assertTrue(File(out, "com/example/Nested.smali").isFile)
            }
        }
    }

    @Test
    fun disassembleClassesFilterWritesOnlyRequestedClasses() {
        withDex(mapOf("One.smali" to simpleClass("LOne;"), "Two.smali" to simpleClass("LTwo;"))) { dex ->
            withTempDir { root ->
                val out = File(root, "filtered")
                val loaded = load(dex)
                Assert.assertTrue(
                    disassembleDexFile(loaded, out, 1, BaksmaliOptions(), listOf("LTwo;"))
                )
                Assert.assertFalse(File(out, "One.smali").exists())
                Assert.assertTrue(File(out, "Two.smali").isFile)
            }
        }
    }

    @Test
    fun disassembleAcceptsZeroJobs() {
        withDex(mapOf("Branch.smali" to BRANCH)) { dex ->
            withTempDir { root ->
                val out = File(root, "zero-jobs")
                val options = BaksmaliOptions()
                Assert.assertTrue(disassembleDexFile(load(dex), out, 0, options))
                Assert.assertTrue(File(out, "Branch.smali").isFile)
            }
        }
    }

    private fun disassemble(dex: File, outDir: File, options: BaksmaliOptions): Boolean =
        disassembleDexFile(load(dex), outDir, 1, options)

    private fun load(dex: File): DexBackedDexFile =
        DexBackedDexFile(Opcodes.forApi(API), dex.readBytes())

    private fun withDex(files: Map<String, String>, block: (File) -> Unit) {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            for ((name, source) in files) {
                File(src, name).writeText(source)
            }
            val dex = File(dir, "classes.dex")
            val options = SmaliOptions().apply {
                apiLevel = API
                outputDexFile = dex.absolutePath
                jobs = 1
            }
            Assert.assertTrue("fixture assembly failed", assemble(options, src.absolutePath))
            block(dex)
        }
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("baksmali-behaviour").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        private const val API = 15

        private val BRANCH = """
            .class public LBranch;
            .super Ljava/lang/Object;

            .method public static f(I)I
                .registers 2
                if-eqz p0, :zero
                const/4 v0, 0x1
                return v0
                :zero
                const/4 v0, 0x0
                return v0
            .end method
        """.trimIndent() + "\n"

        private val NESTED = """
            .class public Lcom/example/Nested;
            .super Ljava/lang/Object;

            .method public static f()V
                .registers 0
                return-void
            .end method
        """.trimIndent() + "\n"

        private fun simpleClass(type: String) = """
            .class public $type
            .super Ljava/lang/Object;

            .method public static f()V
                .registers 0
                return-void
            .end method
        """.trimIndent() + "\n"
    }
}
