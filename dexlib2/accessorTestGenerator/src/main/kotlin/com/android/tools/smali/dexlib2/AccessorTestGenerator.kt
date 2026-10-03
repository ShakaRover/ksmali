/*
 * Copyright 2012, Google LLC
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

package com.android.tools.smali.dexlib2

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroupFile

class AccessorTestGenerator {
    private class UnaryOperation(val name: String)

    private class BinaryOperation(val name: String, val inputTypes: Array<String>)

    private class TypeDef(
        val name: String,
        val unaryOperations: Array<UnaryOperation>,
        val binaryOperations: Array<BinaryOperation>
    )

    companion object {
        private val unaryOperations = arrayOf(
            UnaryOperation("preinc"),
            UnaryOperation("postinc"),
            UnaryOperation("predec"),
            UnaryOperation("postdec")
        )

        private val booleanInputs = arrayOf("boolean")
        private val integralInputs = arrayOf("int", "long")
        private val allInputs = arrayOf("int", "float", "long", "double")

        private val booleanOperations = arrayOf(
            BinaryOperation("and", booleanInputs),
            BinaryOperation("or", booleanInputs),
            BinaryOperation("xor", booleanInputs)
        )

        private val floatOperations = arrayOf(
            BinaryOperation("add", allInputs),
            BinaryOperation("sub", allInputs),
            BinaryOperation("mul", allInputs),
            BinaryOperation("div", allInputs),
            BinaryOperation("rem", allInputs)
        )

        private val integralOperations = arrayOf(
            BinaryOperation("add", allInputs),
            BinaryOperation("sub", allInputs),
            BinaryOperation("mul", allInputs),
            BinaryOperation("div", allInputs),
            BinaryOperation("rem", allInputs),
            BinaryOperation("and", integralInputs),
            BinaryOperation("or", integralInputs),
            BinaryOperation("xor", integralInputs),
            BinaryOperation("shl", integralInputs),
            BinaryOperation("shr", integralInputs),
            BinaryOperation("ushr", integralInputs)
        )

        private val types = arrayOf(
            TypeDef("boolean", arrayOf(), booleanOperations),
            TypeDef("byte", unaryOperations, integralOperations),
            TypeDef("char", unaryOperations, integralOperations),
            TypeDef("short", unaryOperations, integralOperations),
            TypeDef("int", unaryOperations, integralOperations),
            TypeDef("long", unaryOperations, integralOperations),
            TypeDef("float", unaryOperations, floatOperations),
            TypeDef("double", unaryOperations, floatOperations)
        )

        @JvmStatic
        @Throws(IOException::class)
        fun main(args: Array<String>) {
            if (args.size != 1) {
                System.err.println(
                    "Usage: java com.android.tools.smali.dexlib2.AccessorTestGenerator <output_file>"
                )
            }

            val stgUrl = AccessorTestGenerator::class.java.classLoader.getResource("AccessorTest.stg")
            val stg = STGroupFile(stgUrl, "utf-8", '<', '>')
            val fileSt: ST = stg.getInstanceOf("file")
            fileSt.add("types", types)

            PrintWriter(BufferedWriter(FileWriter(args[0]))).use { w ->
                w.print(fileSt.render())
            }
        }
    }
}
