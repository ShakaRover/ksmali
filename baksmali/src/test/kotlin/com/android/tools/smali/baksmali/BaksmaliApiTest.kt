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

import com.android.tools.smali.smali.SmaliOptions
import com.android.tools.smali.smali.assemble
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Public API and CLI-option coverage for the baksmali disassembler: [BaksmaliOptions] defaults, the
 * `disassemble` command's argument parsing and [BaksmaliOptions.loadResourceIds].
 */
class BaksmaliApiTest {

    @Test
    fun baksmaliOptionsDefaults() {
        val options = BaksmaliOptions()
        Assert.assertEquals(15, options.apiLevel)
        Assert.assertTrue(options.parameterRegisters)
        Assert.assertFalse(options.localsDirective)
        Assert.assertFalse(options.sequentialLabels)
        Assert.assertTrue(options.debugInfo)
        Assert.assertFalse(options.codeOffsets)
        Assert.assertTrue(options.accessorComments)
        Assert.assertFalse(options.allowOdex)
        Assert.assertFalse(options.deodex)
        Assert.assertFalse(options.implicitReferences)
        Assert.assertFalse(options.normalizeVirtualMethods)
        Assert.assertEquals(0, options.registerInfo)
        Assert.assertTrue(options.resourceIds.isEmpty())
    }

    @Test
    fun disassembleCommandParsesOptions() {
        withSingleClassDex { dex ->
            val command = ExposedDisassembleCommand()
            JCommander(command).parse(
                "--api", "29",
                "-j", "3",
                "-o", "custom-out",
                "--sequential-labels",
                "--use-locals",
                "--parameter-registers", "false",
                "--debug-info", "false",
                "--code-offsets",
                dex.absolutePath
            )
            command.load(dex.absolutePath)

            Assert.assertEquals(29, command.apiLevel)
            val options = command.options
            Assert.assertTrue(options.sequentialLabels)
            Assert.assertTrue(options.localsDirective)
            Assert.assertFalse(options.parameterRegisters)
            Assert.assertFalse(options.debugInfo)
            Assert.assertTrue(options.codeOffsets)
        }
    }

    @Test
    fun disassembleCommandRejectsInvalidJobCounts() {
        Assert.assertThrows(ParameterException::class.java) {
            JCommander(ExposedDisassembleCommand()).parse("-j", "-1")
        }
        Assert.assertThrows(ParameterException::class.java) {
            JCommander(ExposedDisassembleCommand()).parse("--jobs", "not-a-number")
        }
    }

    @Test
    fun loadResourceIdsQualifiesNamesWithThePrefix() {
        withTempDir { dir ->
            val publicXml = File(dir, "public.xml")
            publicXml.writeText(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<resources>\n" +
                    "    <public type=\"string\" name=\"app.name\" id=\"0x7f010000\"/>\n" +
                    "    <public type=\"id\" name=\"root\" id=\"0x7f020000\"/>\n" +
                    "</resources>\n"
            )

            val options = BaksmaliOptions()
            options.loadResourceIds(mapOf("android" to publicXml))

            Assert.assertEquals("android.string.app_name", options.resourceIds[0x7f010000])
            Assert.assertEquals("android.id.root", options.resourceIds[0x7f020000])
        }
    }

    private fun withSingleClassDex(block: (File) -> Unit) {
        withTempDir { dir ->
            val src = File(dir, "src").apply { mkdirs() }
            File(src, "Branch.smali").writeText(BRANCH)
            val dex = File(dir, "classes.dex")
            Assert.assertTrue(assemble(smaliOptions(dex), src.absolutePath))
            block(dex)
        }
    }

    private fun smaliOptions(output: File) = SmaliOptions().apply {
        apiLevel = API
        outputDexFile = output.absolutePath
        jobs = 1
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("baksmali-api").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private class ExposedDisassembleCommand : DisassembleCommand(emptyList()) {
        public override val options: BaksmaliOptions get() = super.options
        public fun load(input: String) = loadDexFile(input)
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
    }
}
