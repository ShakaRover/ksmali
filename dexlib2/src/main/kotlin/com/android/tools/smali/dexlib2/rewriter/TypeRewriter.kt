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

package com.android.tools.smali.dexlib2.rewriter

open class TypeRewriter : Rewriter<String> {
    override fun rewrite(value: String): String {
        if (value.length > 0 && value[0] == '[') {
            var dimensions = 0
            while (value[dimensions] == '[') {
                dimensions++
            }
            val unwrappedType = value.substring(dimensions)
            val rewrittenType = rewriteUnwrappedType(unwrappedType)
            // instance equality, to avoid a value comparison in the common case of the type being unmodified
            if (unwrappedType !== rewrittenType) {
                return buildString(dimensions + rewrittenType.length) {
                    append(value, 0, dimensions)
                    append(rewrittenType)
                }
            }
            return value
        } else {
            return rewriteUnwrappedType(value)
        }
    }

    /**
     * This is called by the default rewrite implementation with the unwrapped type.
     *
     * For array types, the unwrapped type is the type with the array specifiers removed. And there is no difference
     * for non-array types.
     *
     * @param value The unwrapped type
     * @return The modified version of the unwrapped type. This will be re-array-ified if the original wrapped type was
     * an array.
     */
    protected open fun rewriteUnwrappedType(value: String): String {
        return value
    }
}
