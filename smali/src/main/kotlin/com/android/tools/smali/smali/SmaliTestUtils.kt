/*
 * Copyright 2014, Google LLC
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

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import java.io.IOException
import java.io.StringReader

@Throws(RecognitionException::class, IOException::class)
fun compileSmali(smaliText: String): DexBackedClassDef = compileSmali(smaliText, 15)

@Throws(RecognitionException::class, IOException::class)
fun compileSmali(smaliText: String, apiLevel: Int): DexBackedClassDef {
    val dexBuilder = DexBuilder(Opcodes.forApi(apiLevel))

    val reader = StringReader(smaliText)

    val lexer = smaliLexer(CharStreams.fromReader(reader))
    lexer.setApiLevel(apiLevel)
    val tokens = CommonTokenStream(lexer)

    val parser = smaliParser(tokens)
    parser.setBuildParseTree(false)
    parser.setVerboseErrors(true)
    parser.setAllowOdex(false)
    parser.setApiLevel(apiLevel)

    val result = parser.smali_file()

    if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
        throw RuntimeException("Error occurred while compiling text")
    }

    val t = result.n

    val treeStream = ListTokenStream(t.flatten())

    val dexGen = smaliTreeWalker(treeStream)
    dexGen.setBuildParseTree(false)
    dexGen.setErrorHandler(NoSyncErrorStrategy())
    dexGen.setApiLevel(apiLevel)
    dexGen.setVerboseErrors(true)
    dexGen.setDexBuilder(dexBuilder)
    dexGen.smali_file()

    if (dexGen.getNumberOfSyntaxErrors() > 0) {
        throw RuntimeException("Error occurred while compiling text")
    }

    val dataStore = MemoryDataStore()

    dexBuilder.writeTo(dataStore)

    val dexFile = DexBackedDexFile(Opcodes.forApi(apiLevel), dataStore.buffer)

    return requireNotNull(dexFile.classes.firstOrNull())
}
