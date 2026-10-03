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

package com.android.tools.smali.dexlib2.immutable

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.ImmutableConverter
import com.android.tools.smali.util.ImmutableUtils
import java.util.Collections
import java.util.HashSet
import java.util.SortedSet

open class ImmutableMethod(
    override val definingClass: String,
    override val name: String,
    parameters: List<ImmutableMethodParameter>?,
    override val returnType: String,
    override val accessFlags: Int,
    annotations: Set<ImmutableAnnotation>?,
    hiddenApiRestrictions: Set<HiddenApiRestriction>?,
    override val implementation: ImmutableMethodImplementation?
) : BaseMethodReference(), Method {
    override val parameters: List<ImmutableMethodParameter> = ImmutableUtils.nullToEmptyList(parameters)

    override val parameterTypes: List<CharSequence>
        get() = parameters

    override val annotations: Set<ImmutableAnnotation> = ImmutableUtils.nullToEmptySet(annotations)

    override val hiddenApiRestrictions: Set<HiddenApiRestriction> =
        ImmutableUtils.nullToEmptySet(hiddenApiRestrictions)

    constructor(
        definingClass: String,
        name: String,
        parameters: Iterable<MethodParameter>?,
        returnType: String,
        accessFlags: Int,
        annotations: Set<Annotation>?,
        hiddenApiRestrictions: Set<HiddenApiRestriction>?,
        methodImplementation: MethodImplementation?
    ) : this(
        definingClass,
        name,
        ImmutableMethodParameter.immutableListOf(parameters),
        returnType,
        accessFlags,
        ImmutableAnnotation.immutableSetOf(annotations),
        if (hiddenApiRestrictions == null) Collections.emptySet<HiddenApiRestriction>()
        else Collections.unmodifiableSet(HashSet(hiddenApiRestrictions)),
        ImmutableMethodImplementation.of(methodImplementation)
    )

    companion object {
        fun of(method: Method): ImmutableMethod {
            if (method is ImmutableMethod) {
                return method
            }
            return ImmutableMethod(
                method.definingClass,
                method.name,
                method.parameters,
                method.returnType,
                method.accessFlags,
                method.annotations,
                method.hiddenApiRestrictions,
                method.implementation
            )
        }

        fun immutableSetOf(list: Iterable<Method>?): SortedSet<ImmutableMethod> {
            return CONVERTER.toSortedSet(CollectionUtils.naturalOrdering(), list)
        }

        private val CONVERTER: ImmutableConverter<ImmutableMethod, Method> =
            object : ImmutableConverter<ImmutableMethod, Method>() {
                override fun isImmutable(item: Method): Boolean {
                    return item is ImmutableMethod
                }

                override fun makeImmutable(item: Method): ImmutableMethod {
                    return of(item)
                }
            }
    }
}
