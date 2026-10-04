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

import org.junit.Test

/**
 * Regression tests for three front-end gaps that were fixed together: the `const/high16` "hat"
 * literal form, unquoted `.local` names and the `.parameter` directive aliases. Each has an
 * accepted and a rejected case.
 */
class SmaliFrontEndRegressionTest {

    @Test
    fun constHigh16AcceptsHatAndShiftedForms() {
        assertSmaliAssembles(body("const/high16 v0, 0x3000"))
        assertSmaliAssembles(body("const/high16 v0, 0x30000000"))
        assertSmaliAssembles(body("const/high16 v0, -0x3000"))
        assertSmaliAssembles(wideBody("const-wide/high16 v0, 0x4000"))
        assertSmaliAssembles(wideBody("const-wide/high16 v0, 0x4000000000000000L"))
        assertSmaliAssembles(wideBody("const-wide/high16 v0, -0x4000"))
    }

    @Test
    fun constHigh16RejectsLiteralsThatAreNotValidHats() {
        assertSmaliThrows(
            body("const/high16 v0, 0x10001"),
            IllegalArgumentException::class.java, "Low 16 bits must be zeroed out"
        )
        assertSmaliThrows(
            wideBody("const-wide/high16 v0, 0x1000000000001"),
            NumberFormatException::class.java, "cannot fit into an int"
        )
    }

    @Test
    fun localAcceptsQuotedUnquotedAndNullNames() {
        assertSmaliAssembles(debug(".local v0, var1:I"))
        assertSmaliAssembles(debug(".local v0, \"var 1\":I, \"sig\""))
        assertSmaliAssembles(debug(".local v0, null:I"))
    }

    @Test
    fun localRejectsMalformedNames() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .locals 1",
                "    .local v0, var1",
                "    return-void",
                ".end method"
            ),
            6, "mismatched input"
        )
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .locals 1",
                "    .local v0, var.1:I",
                "    return-void",
                ".end method"
            ),
            5, "extraneous input"
        )
    }

    @Test
    fun parameterAcceptsRegisterLegacyAndAliasForms() {
        assertSmaliAssembles(withParam(".parameter p1, \"x\""))
        assertSmaliAssembles(withParam(".parameter p1\n    .end parameter"))
        assertSmaliAssembles(withParam(".parameter \"x\""))
        assertSmaliAssembles(withParam(".param \"x\""))
        assertSmaliAssembles(withParam(".param p1, \"x\""))
    }

    @Test
    fun parameterWithNoMatchingParameterIsRejected() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    .parameter \"x\"",
                "    return-void",
                ".end method"
            ),
            5, "No parameter exists for this parameter directive"
        )
    }

    private fun body(instruction: String) = smali(
        ".class public LBad;",
        ".super Ljava/lang/Object;",
        ".method public foo()V",
        "    .registers 1",
        "    $instruction",
        "    return-void",
        ".end method"
    )

    private fun wideBody(instruction: String) = smali(
        ".class public LBad;",
        ".super Ljava/lang/Object;",
        ".method public foo()V",
        "    .registers 2",
        "    $instruction",
        "    return-void",
        ".end method"
    )

    private fun debug(directive: String) = smali(
        ".class public LBad;",
        ".super Ljava/lang/Object;",
        ".method public foo()V",
        "    .locals 1",
        "    $directive",
        "    return-void",
        ".end method"
    )

    private fun withParam(parameter: String) = smali(
        ".class public LBad;",
        ".super Ljava/lang/Object;",
        ".method public foo(I)V",
        "    .registers 2",
        "    $parameter",
        "    return-void",
        ".end method"
    )

    private fun smali(vararg lines: String) = lines.joinToString("\n") + "\n"
}
