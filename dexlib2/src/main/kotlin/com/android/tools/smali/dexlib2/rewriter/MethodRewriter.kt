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

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter

open class MethodRewriter(
    @JvmField protected val rewriters: Rewriters
) : Rewriter<Method> {
    override fun rewrite(value: Method): Method {
        return RewrittenMethod(value)
    }

    protected inner class RewrittenMethod(
        protected var method: Method
    ) : BaseMethodReference(), Method {
        override val definingClass: String
            get() = rewriters.methodReferenceRewriter.rewrite(method).definingClass

        override val name: String
            get() = rewriters.methodReferenceRewriter.rewrite(method).name

        override val parameterTypes: List<CharSequence>
            get() = rewriters.methodReferenceRewriter.rewrite(method).parameterTypes

        override val parameters: List<MethodParameter>
            get() = RewriterUtils.rewriteList(
                rewriters.methodParameterRewriter,
                method.parameters
            )

        override val returnType: String
            get() = rewriters.methodReferenceRewriter.rewrite(method).returnType

        override val accessFlags: Int
            get() = method.accessFlags

        override val annotations: Set<Annotation>
            get() = RewriterUtils.rewriteSet(rewriters.annotationRewriter, method.annotations)

        override val hiddenApiRestrictions: Set<HiddenApiRestriction>
            get() = method.hiddenApiRestrictions

        override val implementation: MethodImplementation?
            get() = RewriterUtils.rewriteNullable(
                rewriters.methodImplementationRewriter,
                method.implementation
            )
    }
}
