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

import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets

/**
 * Assembles every smali fixture used by the on-device integration tests and the bundled examples.
 * This exercises the full ANTLR4 parser + tree walker pipeline across the whole smali syntax.
 *
 * The fixtures are packaged as test resources by the `processTestResources` task (the
 * `smali-integration-tests/` directory is not a Gradle project of its own any more), so the test
 * does not depend on the working directory Gradle happens to launch it from.
 */
class AssembleIntegrationTest {

    @Test
    fun assembleAllFixtures() {
        val fixtures = mutableListOf<String>()
        for (root in FIXTURE_ROOTS) {
            fixtures.addAll(collectSmaliResources(root))
        }

        Assume.assumeFalse("smali fixture resources not present", fixtures.isEmpty())

        var assembled = 0
        val failures = mutableListOf<String>()
        for (fixture in fixtures) {
            val text = readResource(fixture)
            try {
                compileSmali(text, MOST_RECENT_API)
                assembled++
            } catch (ex: Throwable) {
                val sw = StringWriter()
                ex.printStackTrace(PrintWriter(sw))
                failures.add(fixture + ":\n" + sw)
            }
        }

        Assert.assertTrue(
            "Failed to assemble " + failures.size + " of " + fixtures.size +
                " smali fixtures:\n" + join(failures), failures.isEmpty()
        )
        Assert.assertTrue(assembled > 0)
    }

    companion object {
        private const val MOST_RECENT_API = 30

        private val FIXTURE_ROOTS = arrayOf(
            "smali-integration-tests",
            "examples",
        )

        private fun readResource(name: String): String {
            val stream = AssembleIntegrationTest::class.java.classLoader.getResourceAsStream(name)
            Assert.assertNotNull("Could not load $name", stream)
            return requireNotNull(stream).use { it.readBytes().toString(StandardCharsets.UTF_8) }
        }

        private fun collectSmaliResources(root: String): List<String> {
            val url = AssembleIntegrationTest::class.java.classLoader.getResource(root)
                ?: return emptyList()
            // Test resources are exploded onto the classpath as a directory; if a future runner packs
            // them into a jar we simply skip the scan rather than fail the suite.
            if (url.protocol != "file") {
                return emptyList()
            }
            val dir = File(url.toURI())
            if (!dir.isDirectory) {
                return emptyList()
            }
            val out = mutableListOf<String>()
            collectSmaliResources(dir, dir, root, out)
            return out
        }

        private fun collectSmaliResources(dir: File, root: File, prefix: String, out: MutableList<String>) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    collectSmaliResources(file, root, prefix, out)
                } else if (file.name.endsWith(".smali")) {
                    val relative = file.relativeTo(root).path.replace(File.separatorChar, '/')
                    out.add("$prefix/$relative")
                }
            }
        }

        private fun join(items: List<String>): String = buildString {
            for (item in items) {
                append("  ").append(item).append('\n')
            }
        }
    }
}
