/*
 * Copyright 2016, Google LLC
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

/**
 * Mutable configuration bag for the smali assembler, used by [assemble] / [assembleSuspend] and
 * the `smali assemble` command line tool.
 *
 * An instance is filled in by the caller (or by [AssembleCommand]) and then handed to
 * [assemble]; it is not immutable and is not safe to share between concurrent assemblies that
 * configure it differently.
 */
open class SmaliOptions {
    /**
     * The target API level, passed to [com.android.tools.smali.dexlib2.Opcodes.forApi] and used to
     * select the opcodes that the lexer/parser accept. Corresponds to the `-a/--api` option and
     * defaults to 15.
     */
    var apiLevel = 15

    /**
     * The dex file that [assemble] / [assembleSuspend] write their output to. Corresponds to the
     * `-o/--output` option and defaults to `out.dex`.
     */
    var outputDexFile: String = "out.dex"

    /**
     * The maximum number of smali files that are assembled in parallel. This is the `-j/--jobs`
     * option: it caps the [kotlinx.coroutines.Dispatchers.Default] dispatcher used for the
     * assembly workers, so values below 1 are treated as 1. It does not affect the bytes that are
     * written, only how quickly they are produced. Defaults to the number of available processors.
     */
    var jobs = Runtime.getRuntime().availableProcessors()

    /** Whether odexed instructions are accepted instead of rejected as unsupported. */
    var allowOdexOpcodes = false

    /** Whether the parser/lexer print their errors and stack traces while assembling. */
    var verboseErrors = false

    /**
     * Whether the lexer's token stream is dumped while assembling.
     *
     * Note: since the parser and tree-walker were merged into a single pass there is no longer an
     * AST to print, so this only dumps the lexer tokens, one line per visible token. It is intended
     * for debugging the front end, not for programmatic consumption.
     */
    var printTokens = false
}
