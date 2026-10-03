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

package com.android.tools.smali.dexlib2.dexbacked.reference

import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.util.ExceptionWithContext

abstract class DexBackedReference {
    companion object {
        fun makeReference(
            dexFile: DexBackedDexFile,
            referenceType: Int,
            referenceIndex: Int
        ): Reference {
            return when (referenceType) {
                ReferenceType.STRING -> DexBackedStringReference(dexFile, referenceIndex)
                ReferenceType.TYPE -> DexBackedTypeReference(dexFile, referenceIndex)
                ReferenceType.METHOD -> DexBackedMethodReference(dexFile, referenceIndex)
                ReferenceType.FIELD -> DexBackedFieldReference(dexFile, referenceIndex)
                ReferenceType.METHOD_PROTO ->
                    DexBackedMethodProtoReference(dexFile, referenceIndex)
                ReferenceType.METHOD_HANDLE ->
                    DexBackedMethodHandleReference(dexFile, referenceIndex)
                ReferenceType.CALL_SITE -> DexBackedCallSiteReference(dexFile, referenceIndex)
                else -> throw ExceptionWithContext("Invalid reference type: %d", referenceType)
            }
        }
    }
}
