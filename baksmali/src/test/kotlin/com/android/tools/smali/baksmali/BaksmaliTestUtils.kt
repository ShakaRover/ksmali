/*
 * Copyright 2015, Google LLC
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

import com.google.common.io.ByteStreams
import org.antlr.v4.runtime.RecognitionException
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.smali.compileSmali
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class BaksmaliTestUtils {

    @Test
    fun testStripComments() {
        Assert.assertEquals("", stripComments("#world"))
        Assert.assertEquals("hello", stripComments("hello#world"))
        Assert.assertEquals("multi\nline", stripComments("multi#hello world\nline#world"))
    }

    @Test
    fun testNormalizeWhitespace() {
        Assert.assertEquals("", normalizeWhitespace(" "))
        Assert.assertEquals("hello", normalizeWhitespace("hello "))
        Assert.assertEquals("hello", normalizeWhitespace(" hello"))
        Assert.assertEquals("hello", normalizeWhitespace(" hello "))
        Assert.assertEquals("hello\nworld", normalizeWhitespace("hello \n \n world"))
    }

    companion object {
        private val newline = System.getProperty("line.separator")

        @JvmStatic
        @Throws(IOException::class, RecognitionException::class)
        fun assertSmaliCompiledEquals(
            source: String, expected: String, options: BaksmaliOptions, stripComments: Boolean
        ) {
            val classDef = compileSmali(source, options.apiLevel)

            // Remove unnecessary whitespace and optionally strip all comments from smali file
            val normalizedActual = getNormalizedSmali(classDef, options, stripComments)
            val normalizedExpected = normalizeSmali(expected, stripComments)

            // Assert that normalized strings are now equal
            Assert.assertEquals(normalizedExpected, normalizedActual)
        }

        @JvmStatic
        @Throws(IOException::class, RecognitionException::class)
        fun assertSmaliCompiledEquals(source: String, expected: String, options: BaksmaliOptions) {
            assertSmaliCompiledEquals(source, expected, options, false)
        }

        @JvmStatic
        @Throws(IOException::class, RecognitionException::class)
        fun assertSmaliCompiledEquals(source: String, expected: String) {
            val options = BaksmaliOptions()
            assertSmaliCompiledEquals(source, expected, options)
        }

        @JvmStatic
        fun normalizeSmali(smaliText0: String, stripComments: Boolean): String {
            var smaliText = smaliText0
            if (stripComments) {
                smaliText = stripComments(smaliText)
            }
            return normalizeWhitespace(smaliText)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getNormalizedSmali(
            classDef: ClassDef, options: BaksmaliOptions, stripComments: Boolean
        ): String {
            val stringWriter = StringWriter()
            val writer = BaksmaliWriter(
                stringWriter,
                if (options.implicitReferences) classDef.type else null)
            val classDefinition = ClassDefinition(options, classDef)
            classDefinition.writeTo(writer)
            writer.close()
            return normalizeSmali(stringWriter.toString(), stripComments)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readResourceBytesFully(fileName: String): ByteArray {
            val smaliStream = RoundtripTest::class.java.classLoader
                .getResourceAsStream(fileName)
            if (smaliStream == null) {
                org.junit.Assert.fail("Could not load $fileName")
            }

            return ByteStreams.toByteArray(smaliStream)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readResourceFully(fileName: String): String {
            return readResourceFully(fileName, StandardCharsets.UTF_8)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readResourceFully(fileName: String, encoding: Charset): String {
            return String(readResourceBytesFully(fileName), encoding)
        }

        @JvmStatic
        fun normalizeNewlines(source: String): String {
            return normalizeNewlines(source, newline)
        }

        @JvmStatic
        fun normalizeNewlines(source: String, newlineValue: String): String {
            return source.replace("\r", "").replace("\n", newlineValue)
        }

        @JvmStatic
        fun normalizeWhitespace(source0: String): String {
            // Go to native system new lines so that ^/$ work correctly
            var source = normalizeNewlines(source0)

            // Remove all suffix/prefix whitespace
            var pattern = Pattern.compile("((^[ \t]+)|([ \t]+$))", Pattern.MULTILINE)
            var matcher = pattern.matcher(source)
            source = matcher.replaceAll("")

            // Remove all empty lines
            pattern = Pattern.compile("^\r?\n?", Pattern.MULTILINE)
            matcher = pattern.matcher(source)
            source = matcher.replaceAll("")

            // Remove a trailing new line, if present
            pattern = Pattern.compile("\r?\n?$")
            matcher = pattern.matcher(source)
            source = matcher.replaceAll("")

            // Go back to unix-style \n newlines
            source = normalizeNewlines(source, "\n")
            return source
        }

        @JvmStatic
        fun stripComments(source: String): String {
            val pattern = Pattern.compile("#(.*)")
            val matcher = pattern.matcher(source)
            return matcher.replaceAll("")
        }
    }
}
