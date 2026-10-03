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

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.util.FIELD_IS_INSTANCE
import com.android.tools.smali.dexlib2.util.FIELD_IS_STATIC
import com.android.tools.smali.dexlib2.util.METHOD_IS_DIRECT
import com.android.tools.smali.dexlib2.util.METHOD_IS_VIRTUAL
import com.android.tools.smali.util.ChainedIterable
import com.android.tools.smali.util.ImmutableConverter
import com.android.tools.smali.util.ImmutableUtils
import com.android.tools.smali.util.IteratorUtils
import java.util.ArrayList
import java.util.Collections
import java.util.SortedSet

open class ImmutableClassDef(
    override val type: String,
    override val accessFlags: Int,
    override val superclass: String?,
    interfaces: List<String>?,
    override val sourceFile: String?,
    annotations: Set<ImmutableAnnotation>?,
    staticFields: SortedSet<ImmutableField>?,
    instanceFields: SortedSet<ImmutableField>?,
    directMethods: SortedSet<ImmutableMethod>?,
    virtualMethods: SortedSet<ImmutableMethod>?
) : BaseTypeReference(), ClassDef {
    override val interfaces: List<String> = ImmutableUtils.nullToEmptyList(interfaces)
    override val annotations: Set<ImmutableAnnotation> = ImmutableUtils.nullToEmptySet(annotations)
    override val staticFields: SortedSet<ImmutableField> = ImmutableUtils.nullToEmptySortedSet(staticFields)
    override val instanceFields: SortedSet<ImmutableField> = ImmutableUtils.nullToEmptySortedSet(instanceFields)
    override val directMethods: SortedSet<ImmutableMethod> = ImmutableUtils.nullToEmptySortedSet(directMethods)
    override val virtualMethods: SortedSet<ImmutableMethod> = ImmutableUtils.nullToEmptySortedSet(virtualMethods)

    override val fields: Iterable<ImmutableField>
        get() = ChainedIterable(staticFields, instanceFields)

    override val methods: Iterable<ImmutableMethod>
        get() = ChainedIterable(directMethods, virtualMethods)

    constructor(
        type: String,
        accessFlags: Int,
        superclass: String?,
        interfaces: Collection<String>?,
        sourceFile: String?,
        annotations: Collection<Annotation>?,
        staticFields: Iterable<Field>?,
        instanceFields: Iterable<Field>?,
        directMethods: Iterable<Method>?,
        virtualMethods: Iterable<Method>?
    ) : this(
        type,
        accessFlags,
        superclass,
        if (interfaces == null) Collections.emptyList()
        else Collections.unmodifiableList(ArrayList(interfaces)),
        sourceFile,
        ImmutableAnnotation.immutableSetOf(annotations),
        ImmutableField.immutableSetOf(staticFields),
        ImmutableField.immutableSetOf(instanceFields),
        ImmutableMethod.immutableSetOf(directMethods),
        ImmutableMethod.immutableSetOf(virtualMethods)
    )

    constructor(
        type: String,
        accessFlags: Int,
        superclass: String?,
        interfaces: Collection<String>?,
        sourceFile: String?,
        annotations: Collection<Annotation>?,
        fields: Iterable<Field>?,
        methods: Iterable<Method>?
    ) : this(
        type,
        accessFlags,
        superclass,
        if (interfaces == null) Collections.emptyList()
        else Collections.unmodifiableList(ArrayList(interfaces)),
        sourceFile,
        ImmutableAnnotation.immutableSetOf(annotations),
        ImmutableField.immutableSetOf(
            IteratorUtils.filter(fields ?: Collections.emptyList(), FIELD_IS_STATIC)
        ),
        ImmutableField.immutableSetOf(
            IteratorUtils.filter(fields ?: Collections.emptyList(), FIELD_IS_INSTANCE)
        ),
        ImmutableMethod.immutableSetOf(
            IteratorUtils.filter(methods ?: Collections.emptyList(), METHOD_IS_DIRECT)
        ),
        ImmutableMethod.immutableSetOf(
            IteratorUtils.filter(methods ?: Collections.emptyList(), METHOD_IS_VIRTUAL)
        )
    )

    companion object {
        fun of(classDef: ClassDef): ImmutableClassDef {
            if (classDef is ImmutableClassDef) {
                return classDef
            }
            return ImmutableClassDef(
                classDef.type,
                classDef.accessFlags,
                classDef.superclass,
                classDef.interfaces,
                classDef.sourceFile,
                classDef.annotations,
                classDef.staticFields,
                classDef.instanceFields,
                classDef.directMethods,
                classDef.virtualMethods
            )
        }

        fun immutableSetOf(iterable: Iterable<ClassDef>?): Set<ImmutableClassDef> {
            return CONVERTER.toSet(iterable)
        }

        private val CONVERTER: ImmutableConverter<ImmutableClassDef, ClassDef> =
            object : ImmutableConverter<ImmutableClassDef, ClassDef>() {
                override fun isImmutable(item: ClassDef): Boolean {
                    return item is ImmutableClassDef
                }

                override fun makeImmutable(item: ClassDef): ImmutableClassDef {
                    return of(item)
                }
            }
    }
}
