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
 * Round-trip fidelity against every real dex fixture checked into the repository.
 *
 * For each dex the test proves two things:
 *  * the dex writer reaches a fixed point: disassembling the reassembled dex and reassembling it
 *    again produces the same bytes as the first reassembly, and
 *  * disassembly is idempotent: the second disassembly is byte-for-byte identical to the first.
 *
 * A small set of fixtures are known to be canonical output of this writer and must round-trip
 * byte-identically all the way back to the original dex.
 */
class FidelityRoundtripTest {

    @Test
    fun inRepoDexesHaveStableDisassembly() {
        for (resource in DEX_RESOURCES) {
            withTempDir { dir ->
                val original = BaksmaliTestUtils.readResourceBytesFully(resource)

                val dis0 = File(dir, "dis0")
                val gen0 = File(dir, "gen0.dex")
                Assert.assertTrue(
                    "$resource: first disassemble/assemble failed",
                    disassembleThenAssemble(original, dis0, gen0)
                )

                val dis1 = File(dir, "dis1")
                val gen1 = File(dir, "gen1.dex")
                Assert.assertTrue(
                    "$resource: second disassemble/assemble failed",
                    disassembleThenAssemble(gen0.readBytes(), dis1, gen1)
                )

                Assert.assertTrue(
                    "$resource: the dex writer is not a fixed point",
                    gen0.readBytes().contentEquals(gen1.readBytes())
                )

                // disassemble(assemble(disassemble(gen0))) == disassemble(gen0)
                val dis2 = File(dir, "dis2")
                Assert.assertTrue(disassemble(gen1.readBytes(), dis2))
                Assert.assertEquals(
                    "$resource: disassembly is not idempotent",
                    tree(dis1), tree(dis2)
                )

                // For every fixture except the deliberately non-canonical duplicate one, the
                // disassembly of the original dex is already canonical.
                if (resource !in NON_CANONICAL_DISASSEMBLY) {
                    Assert.assertEquals(
                        "$resource: original disassembly is not canonical",
                        tree(dis0), tree(dis1)
                    )
                }
            }
        }
    }

    @Test
    fun canonicalInRepoDexesRoundTripByteIdentical() {
        for (resource in BYTE_IDENTICAL) {
            withTempDir { dir ->
                val original = BaksmaliTestUtils.readResourceBytesFully(resource)
                val rebuilt = File(dir, "rebuilt.dex")
                Assert.assertTrue(
                    "$resource: disassemble/assemble failed",
                    disassembleThenAssemble(original, File(dir, "dis"), rebuilt)
                )
                Assert.assertArrayEquals(
                    "$resource did not round-trip byte-identically",
                    original, rebuilt.readBytes()
                )
            }
        }
    }

    private fun disassemble(bytes: ByteArray, outDir: File): Boolean {
        val dexFile = DexBackedDexFile(Opcodes.forApi(API), bytes)
        return disassembleDexFile(dexFile, outDir, 1, BaksmaliOptions().apply { apiLevel = API })
    }

    private fun disassembleThenAssemble(bytes: ByteArray, outDir: File, outDex: File): Boolean {
        if (!disassemble(bytes, outDir)) {
            return false
        }
        val smaliOptions = SmaliOptions().apply {
            apiLevel = API
            outputDexFile = outDex.absolutePath
            jobs = 1
        }
        return assemble(smaliOptions, outDir.absolutePath)
    }

    private fun tree(dir: File): Map<String, String> =
        dir.walkTopDown().filter { it.isFile }
            .associate { it.relativeTo(dir).path to it.readText() }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("smali-fidelity").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        private const val API = 15

        private val DEX_RESOURCES = listOf(
            "ConstructorTest/classes.dex",
            "DuplicateTest/classes.dex",
            "FieldGapOrderTest/FieldGapOrderInput.dex",
            "InstanceOfTest/classes.dex",
            "LocalTest/classes.dex",
            "MultipleStartInstructionsTest/classes.dex",
            "MultiSwitchTest/MultiSwitchInput.dex",
            "RegisterEqualityOnMergeTest/classes.dex",
            "UninitRefIdentityTest/classes.dex",
            "ZeroArrayPayloadWidthTest/ZeroArrayPayloadWidthTestInput.dex",
            // Bundled from dexlib2's test resources (see baksmali/build.gradle).
            "accessorTest.dex",
        )

        // These two fixtures are byte-for-byte output of this assembler/writer, so the whole
        // round-trip is expected to be lossless. The others were produced by other tools and are
        // only required to be stable from the second generation on.
        private val BYTE_IDENTICAL = setOf(
            "InstanceOfTest/classes.dex",
            "UninitRefIdentityTest/classes.dex",
        )

        // DuplicateTest/classes.dex intentionally contains two entries with the same signature. The
        // first disassembly annotates the unreachable ones with "duplicate ... ignored" comments;
        // a reassembled dex no longer contains them, so the text changes once. The writer fixed
        // point (asserted above) still holds.
        private val NON_CANONICAL_DISASSEMBLY = setOf("DuplicateTest/classes.dex")
    }
}
