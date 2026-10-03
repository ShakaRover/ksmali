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

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import org.junit.Assert
import java.io.File
import java.io.IOException

/**
 * A base test class for performing a disassembly on a dex file and verifying the results
 *
 * The test accepts a single-class dex file as input, disassembles it, and  verifies that
 * the result equals a known-good output smali file.
 *
 * By default, the input and output files should be resources at [testDir]/[testName]Input.dex
 * and [testDir]/[testName]Output.smali respectively
 */
open class DisassemblyTest : DexTest() {

    protected fun getOutputFilename(testName: String): String {
        return "$testDir${File.separatorChar}${testName}Output.smali"
    }

    protected fun runTest(testName: String) {
        runTest(testName, BaksmaliOptions())
    }

    protected fun runTest(testName: String, options: BaksmaliOptions) {
        try {
            val inputDex: DexBackedDexFile = getInputDexFile(testName, options)
            Assert.assertEquals(1, inputDex.classSection.size)
            val inputClass: ClassDef? = inputDex.classes.firstOrNull()
            Assert.assertNotNull(inputClass)
            val input = BaksmaliTestUtils.getNormalizedSmali(inputClass!!, options, true)

            var output = BaksmaliTestUtils.readResourceFully(getOutputFilename(testName))
            output = BaksmaliTestUtils.normalizeSmali(output, true)

            // Run smali, baksmali, and then compare strings are equal (minus comments/whitespace)
            Assert.assertEquals(output, input)
        } catch (ex: IOException) {
            Assert.fail()
        }
    }
}
