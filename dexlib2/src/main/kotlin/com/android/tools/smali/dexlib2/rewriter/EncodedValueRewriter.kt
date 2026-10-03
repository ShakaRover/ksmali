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

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.value.BaseAnnotationEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseArrayEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseEnumEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseFieldEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.EnumEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue

open class EncodedValueRewriter(
    @JvmField protected val rewriters: Rewriters
) : Rewriter<EncodedValue> {
    override fun rewrite(encodedValue: EncodedValue): EncodedValue {
        when (encodedValue.valueType) {
            ValueType.TYPE -> return RewrittenTypeEncodedValue(encodedValue as TypeEncodedValue)
            ValueType.FIELD -> return RewrittenFieldEncodedValue(encodedValue as FieldEncodedValue)
            ValueType.METHOD -> return RewrittenMethodEncodedValue(encodedValue as MethodEncodedValue)
            ValueType.ENUM -> return RewrittenEnumEncodedValue(encodedValue as EnumEncodedValue)
            ValueType.ARRAY -> return RewrittenArrayEncodedValue(encodedValue as ArrayEncodedValue)
            ValueType.ANNOTATION -> return RewrittenAnnotationEncodedValue(
                encodedValue as AnnotationEncodedValue
            )
            else -> return encodedValue
        }
    }

    protected inner class RewrittenTypeEncodedValue(
        protected var typeEncodedValue: TypeEncodedValue
    ) : BaseTypeEncodedValue() {
        override val value: String
            get() = rewriters.typeRewriter.rewrite(typeEncodedValue.value)
    }

    protected inner class RewrittenFieldEncodedValue(
        protected var fieldEncodedValue: FieldEncodedValue
    ) : BaseFieldEncodedValue() {
        override val value: FieldReference
            get() = rewriters.fieldReferenceRewriter.rewrite(fieldEncodedValue.value)
    }

    protected inner class RewrittenEnumEncodedValue(
        protected var enumEncodedValue: EnumEncodedValue
    ) : BaseEnumEncodedValue() {
        override val value: FieldReference
            get() = rewriters.fieldReferenceRewriter.rewrite(enumEncodedValue.value)
    }

    protected inner class RewrittenMethodEncodedValue(
        protected var methodEncodedValue: MethodEncodedValue
    ) : BaseMethodEncodedValue() {
        override val value: MethodReference
            get() = rewriters.methodReferenceRewriter.rewrite(methodEncodedValue.value)
    }

    protected inner class RewrittenArrayEncodedValue(
        protected var arrayEncodedValue: ArrayEncodedValue
    ) : BaseArrayEncodedValue() {
        override val value: List<EncodedValue>
            get() = RewriterUtils.rewriteList(
                rewriters.encodedValueRewriter,
                arrayEncodedValue.value
            )
    }

    protected inner class RewrittenAnnotationEncodedValue(
        protected var annotationEncodedValue: AnnotationEncodedValue
    ) : BaseAnnotationEncodedValue() {
        override val type: String
            get() = rewriters.typeRewriter.rewrite(annotationEncodedValue.type)

        override val elements: Set<AnnotationElement>
            get() = RewriterUtils.rewriteSet(
                rewriters.annotationElementRewriter,
                annotationEncodedValue.elements
            )
    }
}
