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

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue

open class RewriterModule {
    open fun getDexFileRewriter(rewriters: Rewriters): Rewriter<DexFile> {
        return DexFileRewriter(rewriters)
    }

    open fun getClassDefRewriter(rewriters: Rewriters): Rewriter<ClassDef> {
        return ClassDefRewriter(rewriters)
    }

    open fun getFieldRewriter(rewriters: Rewriters): Rewriter<Field> {
        return FieldRewriter(rewriters)
    }

    open fun getMethodRewriter(rewriters: Rewriters): Rewriter<Method> {
        return MethodRewriter(rewriters)
    }

    open fun getMethodParameterRewriter(rewriters: Rewriters): Rewriter<MethodParameter> {
        return MethodParameterRewriter(rewriters)
    }

    open fun getMethodImplementationRewriter(rewriters: Rewriters): Rewriter<MethodImplementation> {
        return MethodImplementationRewriter(rewriters)
    }

    open fun getInstructionRewriter(rewriters: Rewriters): Rewriter<Instruction> {
        return InstructionRewriter(rewriters)
    }

    open fun getTryBlockRewriter(rewriters: Rewriters): Rewriter<TryBlock<out ExceptionHandler>> {
        return TryBlockRewriter(rewriters)
    }

    open fun getExceptionHandlerRewriter(rewriters: Rewriters): Rewriter<ExceptionHandler> {
        return ExceptionHandlerRewriter(rewriters)
    }

    open fun getDebugItemRewriter(rewriters: Rewriters): Rewriter<DebugItem> {
        return DebugItemRewriter(rewriters)
    }

    open fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
        return TypeRewriter()
    }

    open fun getFieldReferenceRewriter(rewriters: Rewriters): Rewriter<FieldReference> {
        return FieldReferenceRewriter(rewriters)
    }

    open fun getMethodReferenceRewriter(rewriters: Rewriters): Rewriter<MethodReference> {
        return MethodReferenceRewriter(rewriters)
    }

    open fun getCallSiteReferenceRewriter(rewriters: Rewriters): Rewriter<CallSiteReference> {
        return CallSiteReferenceRewriter(rewriters)
    }

    open fun getAnnotationRewriter(rewriters: Rewriters): Rewriter<Annotation> {
        return AnnotationRewriter(rewriters)
    }

    open fun getAnnotationElementRewriter(rewriters: Rewriters): Rewriter<AnnotationElement> {
        return AnnotationElementRewriter(rewriters)
    }

    open fun getEncodedValueRewriter(rewriters: Rewriters): Rewriter<EncodedValue> {
        return EncodedValueRewriter(rewriters)
    }
}
