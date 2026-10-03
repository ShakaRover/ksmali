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

import org.antlr.v4.runtime.RecognitionException
import org.junit.Assert
import java.io.File
import java.io.IOException

/**
 * A base test class for performing a roundtrip assembly/disassembly
 *
 * The test accepts a smali file as input, performs a smali -> dex -> smali roundtrip, and
 * verifies that the result equals a known-good output smali file.
 *
 * By default, the input and output files should be resources at [testDir]/[testName]Input.smali
 * and [testDir]/[testName]Output.smali respectively
 */
abstract class RoundtripTest {
    protected val testDir: String

    protected constructor(testDir: String) {
        this.testDir = testDir
    }

    protected constructor() {
        this.testDir = javaClass.simpleName
    }

    protected open fun getInputFilename(testName: String): String {
        return "$testDir${File.separatorChar}${testName}Input.smali"
    }

    protected open fun getOutputFilename(testName: String): String {
        return "$testDir${File.separatorChar}${testName}Output.smali"
    }

    protected fun runTest(testName: String) {
        runTest(testName, BaksmaliOptions())
    }

    protected fun runTest(testName: String, options: BaksmaliOptions) {
        try {
            // Load file from resources as a stream
            val inputFilename = getInputFilename(testName)
            val input = BaksmaliTestUtils.readResourceFully(getInputFilename(testName))
            val output: String
            if (getOutputFilename(testName) == inputFilename) {
                output = input
            } else {
                output = BaksmaliTestUtils.readResourceFully(getOutputFilename(testName))
            }

            // Run smali, baksmali, and then compare strings are equal (minus comments/whitespace)
            BaksmaliTestUtils.assertSmaliCompiledEquals(input, output, options, true)
        } catch (ex: IOException) {
            Assert.fail()
        } catch (ex: RecognitionException) {
            Assert.fail()
        }
    }
}
