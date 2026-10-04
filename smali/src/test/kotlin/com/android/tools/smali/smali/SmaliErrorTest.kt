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
 * Error paths of the smali front-end. Each test pins the *position* the front-end reports as well
 * as the diagnostic text, so a regression that loses the line/column is caught too.
 *
 * Note: `.end class` does not exist in the smali grammar (`.class`/`.super` are not delimited), so
 * the "missing directive" cases here are the `.class`/`.super` requirements enforced by the parser.
 */
class SmaliErrorTest {

    @Test
    fun missingEndMethodReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    return-void"
            ),
            6, "missing '.end method'"
        )
    }

    @Test
    fun unknownDirectiveReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    return-void",
                ".endmethod"
            ),
            6, "Invalid directive"
        )
    }

    @Test
    fun strayTokenReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    @@@",
                "    return-void",
                ".end method"
            ),
            5, "mismatched input"
        )
    }

    @Test
    fun missingClassDirectiveReportsPosition() {
        assertSmaliRejected(
            smali(
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    return-void",
                ".end method"
            ),
            1, "The file must contain a .class directive"
        )
    }

    @Test
    fun missingSuperDirectiveReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".method public foo()V",
                "    .registers 1",
                "    return-void",
                ".end method"
            ),
            1, "The file must contain a .super directive"
        )
    }

    @Test
    fun duplicateRegistersDirectiveReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    .registers 1",
                "    .registers 2",
                "    return-void",
                ".end method"
            ),
            5, "There can only be a single .registers or .locals directive"
        )
    }

    @Test
    fun missingRegistersDirectiveReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo()V",
                "    return-void",
                ".end method"
            ),
            3, "A .registers or .locals directive must be present"
        )
    }

    @Test
    fun registerCountBelowParameterCountReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public static foo(I)V",
                "    .registers 0",
                "    return-void",
                ".end method"
            ),
            4, "This method requires at least 1 registers"
        )
    }

    @Test
    fun parameterRegisterOutOfRangeReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo(I)V",
                "    .registers 2",
                "    .parameter p99",
                "    return-void",
                ".end method"
            ),
            5, "larger than the maximum register"
        )
    }

    @Test
    fun parameterRegisterThatIsNotAParameterReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo(I)V",
                "    .registers 2",
                "    .parameter v0",
                "    return-void",
                ".end method"
            ),
            5, "is not a parameter register"
        )
    }

    @Test
    fun parameterRegisterForSecondHalfOfWideParameterReportsPosition() {
        assertSmaliRejected(
            smali(
                ".class public LBad;",
                ".super Ljava/lang/Object;",
                ".method public foo(J)V",
                "    .registers 3",
                "    .parameter p2",
                "    return-void",
                ".end method"
            ),
            5, "second half of a wide parameter"
        )
    }

    @Test
    fun invalidLiteralsReportTheRejectedValue() {
        assertSmaliThrows(
            body("const/4 v0, 0x100"),
            NumberFormatException::class.java, "cannot fit into a nibble"
        )
        assertSmaliThrows(
            body("const/16 v0, 0x10000"),
            NumberFormatException::class.java, "cannot fit into a short"
        )
        assertSmaliThrows(
            body("const v0, 0x100000000"),
            NumberFormatException::class.java, "cannot fit into an int"
        )
        assertSmaliThrows(
            body("const/4 v0, 9"),
            IllegalArgumentException::class.java, "Must be between -8 and 7"
        )
    }

    private fun body(instruction: String): String = smali(
        ".class public LBad;",
        ".super Ljava/lang/Object;",
        ".method public foo()V",
        "    .registers 1",
        "    $instruction",
        "    return-void",
        ".end method"
    )


    private fun smali(vararg lines: String): String = lines.joinToString("\n") + "\n"
}
