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

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

/**
 * Public API and CLI-option coverage for the smali assembler: [SmaliOptions] defaults, the
 * `assemble` command's argument parsing and the output-file behaviour of [assemble].
 */
class SmaliApiTest {

    @Test
    fun smaliOptionsDefaults() {
        val options = SmaliOptions()
        Assert.assertEquals(15, options.apiLevel)
        Assert.assertEquals("out.dex", options.outputDexFile)
        Assert.assertEquals(Runtime.getRuntime().availableProcessors(), options.jobs)
        Assert.assertFalse(options.allowOdexOpcodes)
        Assert.assertFalse(options.verboseErrors)
        Assert.assertFalse(options.printTokens)
    }

    @Test
    fun assembleCommandParsesJobCount() {
        Assert.assertEquals(3, parse("-j", "3").options.jobs)
        Assert.assertEquals(5, parse("--jobs", "5").options.jobs)
        Assert.assertEquals(
            Runtime.getRuntime().availableProcessors(),
            ExposedAssembleCommand().options.jobs
        )
        Assert.assertEquals(0, parse("-j", "0").options.jobs)
    }

    @Test
    fun assembleCommandRejectsInvalidJobCounts() {
        Assert.assertThrows(ParameterException::class.java) { parse("-j", "-1") }
        Assert.assertThrows(ParameterException::class.java) { parse("--jobs", "not-a-number") }
    }

    @Test
    fun assembleCommandParsesApiOutputAndFlags() {
        val options = parse(
            "--api", "29",
            "-o", "custom.dex",
            "--verbose",
            "--allow-odex-opcodes"
        ).options
        Assert.assertEquals(29, options.apiLevel)
        Assert.assertEquals("custom.dex", options.outputDexFile)
        Assert.assertTrue(options.verboseErrors)
        Assert.assertTrue(options.allowOdexOpcodes)
    }

    @Test
    fun assembleWritesDexToRequestedOutput() {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "Hello.smali").writeText(HELLO)

            val out = File(dir, "out.dex")
            val options = SmaliOptions().apply {
                outputDexFile = out.absolutePath
                jobs = 1
            }
            Assert.assertTrue(assemble(options, src.absolutePath))
            Assert.assertTrue("Expected $out to be written", out.isFile)

            val dexFile = DexBackedDexFile(Opcodes.forApi(15), out.readBytes())
            Assert.assertEquals(listOf("LHello;"), dexFile.classes.map { it.type })
        }
    }

    @Test
    fun assembleStillRunsWithZeroJobs() {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "Hello.smali").writeText(HELLO)
            val options = SmaliOptions().apply {
                outputDexFile = File(dir, "zero-jobs.dex").absolutePath
                jobs = 0
            }
            Assert.assertTrue(assemble(options, src.absolutePath))
        }
    }

    @Test
    fun assembleDoesNotCreateMissingOutputParentDirectory() {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "Hello.smali").writeText(HELLO)
            val options = SmaliOptions().apply {
                outputDexFile = File(dir, "missing/out.dex").absolutePath
                jobs = 1
            }
            Assert.assertThrows(FileNotFoundException::class.java) {
                assemble(options, src.absolutePath)
            }
        }
    }

    @Test
    fun assembleMissingInputIsRejected() {
        withTempDir { dir ->
            val options = SmaliOptions().apply { outputDexFile = File(dir, "out.dex").absolutePath }
            val ex = Assert.assertThrows(IllegalArgumentException::class.java) {
                assemble(options, File(dir, "does-not-exist").absolutePath)
            }
            Assert.assertTrue(ex.message.orEmpty().contains("Cannot find file or directory"))
        }
    }

    @Test
    fun assembleReturnsFalseForMalformedSource() {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "Bad.smali").writeText(
                ".class public LBad;\n" +
                    ".super Ljava/lang/Object;\n" +
                    ".method public foo()V\n" +
                    "    .registers 1\n" +
                    "    return-void\n"
            )
            val options = SmaliOptions().apply {
                outputDexFile = File(dir, "bad.dex").absolutePath
                jobs = 1
            }
            Assert.assertFalse(assemble(options, src.absolutePath))
        }
    }

    private fun parse(vararg args: String): ExposedAssembleCommand {
        val command = ExposedAssembleCommand()
        JCommander(command).parse(*args)
        return command
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("smali-api").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private class ExposedAssembleCommand : AssembleCommand(emptyList()) {
        public override val options: SmaliOptions get() = super.options
    }

    private companion object {
        private val HELLO = """
            .class public LHello;
            .super Ljava/lang/Object;

            .method public static main([Ljava/lang/String;)V
                .registers 2
                const-string v0, "hello"
                return-void
            .end method
        """.trimIndent() + "\n"
    }
}
