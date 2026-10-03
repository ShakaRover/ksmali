/*
 * [The "BSD licence"]
 * Copyright (c) 2011 Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver
import com.android.tools.smali.util.ExceptionWithContext
import java.io.IOException

class SyntheticAccessCommentMethodItem(
    private val classDef: ClassDefinition,
    private val accessedMember: SyntheticAccessorResolver.AccessedMember,
    codeAddress: Int
) : MethodItem(codeAddress) {
    override fun getSortOrder(): Double {
        //just before the pre-instruction register information, if any
        return 99.8
    }

    @Throws(IOException::class)
    override fun writeTo(writer: BaksmaliWriter): Boolean {
        writer.write("# ")
        when (accessedMember.accessedMemberType) {
            SyntheticAccessorResolver.METHOD -> writer.write("invokes: ")
            SyntheticAccessorResolver.GETTER -> writer.write("getter for: ")
            SyntheticAccessorResolver.SETTER -> writer.write("setter for: ")
            SyntheticAccessorResolver.PREFIX_INCREMENT -> writer.write("++operator for: ")
            SyntheticAccessorResolver.POSTFIX_INCREMENT -> writer.write("operator++ for: ")
            SyntheticAccessorResolver.PREFIX_DECREMENT -> writer.write("--operator for: ")
            SyntheticAccessorResolver.POSTFIX_DECREMENT -> writer.write("operator-- for: ")
            SyntheticAccessorResolver.ADD_ASSIGNMENT -> writer.write("+= operator for: ")
            SyntheticAccessorResolver.SUB_ASSIGNMENT -> writer.write("-= operator for: ")
            SyntheticAccessorResolver.MUL_ASSIGNMENT -> writer.write("*= operator for: ")
            SyntheticAccessorResolver.DIV_ASSIGNMENT -> writer.write("/= operator for: ")
            SyntheticAccessorResolver.REM_ASSIGNMENT -> writer.write("%= operator for: ")
            SyntheticAccessorResolver.AND_ASSIGNMENT -> writer.write("&= operator for: ")
            SyntheticAccessorResolver.OR_ASSIGNMENT -> writer.write("|= operator for: ")
            SyntheticAccessorResolver.XOR_ASSIGNMENT -> writer.write("^= operator for: ")
            SyntheticAccessorResolver.SHL_ASSIGNMENT -> writer.write("<<= operator for: ")
            SyntheticAccessorResolver.SHR_ASSIGNMENT -> writer.write(">>= operator for: ")
            SyntheticAccessorResolver.USHR_ASSIGNMENT -> writer.write(">>>= operator for: ")
            else -> throw ExceptionWithContext("Unknown access type: %d", accessedMember.accessedMemberType)
        }

        writer.writeReference(accessedMember.accessedMember)
        return true
    }
}
