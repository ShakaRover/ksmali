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

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.util.ChainedIterable

open class ClassDefRewriter(
    @JvmField protected val rewriters: Rewriters
) : Rewriter<ClassDef> {
    override fun rewrite(classDef: ClassDef): ClassDef {
        return RewrittenClassDef(classDef)
    }

    protected inner class RewrittenClassDef(
        protected var classDef: ClassDef
    ) : BaseTypeReference(), ClassDef {
        override val type: String
            get() = rewriters.typeRewriter.rewrite(classDef.type)

        override val accessFlags: Int
            get() = classDef.accessFlags

        override val superclass: String?
            get() = RewriterUtils.rewriteNullable(
                rewriters.typeRewriter,
                classDef.superclass
            )

        override val interfaces: List<String>
            get() = RewriterUtils.rewriteList(rewriters.typeRewriter, classDef.interfaces)

        override val sourceFile: String?
            get() = classDef.sourceFile

        override val annotations: Set<Annotation>
            get() = RewriterUtils.rewriteSet(rewriters.annotationRewriter, classDef.annotations)

        override val staticFields: Iterable<Field>
            get() = RewriterUtils.rewriteIterable(
                rewriters.fieldRewriter,
                classDef.staticFields
            )

        override val instanceFields: Iterable<Field>
            get() = RewriterUtils.rewriteIterable(
                rewriters.fieldRewriter,
                classDef.instanceFields
            )

        override val fields: Iterable<Field>
            get() = ChainedIterable<Field>(staticFields, instanceFields)

        override val directMethods: Iterable<Method>
            get() = RewriterUtils.rewriteIterable(
                rewriters.methodRewriter,
                classDef.directMethods
            )

        override val virtualMethods: Iterable<Method>
            get() = RewriterUtils.rewriteIterable(
                rewriters.methodRewriter,
                classDef.virtualMethods
            )

        override val methods: Iterable<Method>
            get() = ChainedIterable<Method>(directMethods, virtualMethods)
    }
}
