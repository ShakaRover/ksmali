/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.util.METHOD_IS_DIRECT
import com.android.tools.smali.dexlib2.util.METHOD_IS_VIRTUAL
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue
import com.android.tools.smali.util.ArraySortedSet
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.IteratorUtils
import java.util.AbstractCollection
import java.util.ArrayList
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet
import java.util.stream.Collectors

class BuilderClassDef internal constructor(
    internal val typeReference: BuilderTypeReference,
    override val accessFlags: Int,
    internal val superclassReference: BuilderTypeReference?,
    internal val interfacesList: BuilderTypeList,
    internal val sourceFileReference: BuilderStringReference?,
    override val annotations: BuilderAnnotationSet,
    staticFields: SortedSet<BuilderField>?,
    instanceFields: SortedSet<BuilderField>?,
    methods: Iterable<out BuilderMethod>?,
    internal val staticInitializers: BuilderArrayEncodedValue?,
) : BaseTypeReference(), ClassDef {
    override val staticFields: SortedSet<BuilderField>
    override val instanceFields: SortedSet<BuilderField>
    override val directMethods: SortedSet<BuilderMethod>
    override val virtualMethods: SortedSet<BuilderMethod>

    var classDefIndex = DexWriter.NO_INDEX
    var annotationDirectoryOffset = DexWriter.NO_OFFSET

    init {
        val resolvedMethods: Iterable<out BuilderMethod> = methods ?: emptyList()
        this.staticFields = staticFields
            ?: Collections.unmodifiableSortedSet(TreeSet<BuilderField>())
        this.instanceFields = instanceFields
            ?: Collections.unmodifiableSortedSet(TreeSet<BuilderField>())
        val directMethodsIterator: MutableIterator<BuilderMethod> =
            IteratorUtils.filter(resolvedMethods, METHOD_IS_DIRECT)
        val virtualMethodsIterator: MutableIterator<BuilderMethod> =
            IteratorUtils.filter(resolvedMethods, METHOD_IS_VIRTUAL)
        this.directMethods = ArraySortedSet.copyOf(
            CollectionUtils.naturalOrdering(),
            IteratorUtils.toList(directMethodsIterator)
        )
        this.virtualMethods = ArraySortedSet.copyOf(
            CollectionUtils.naturalOrdering(),
            IteratorUtils.toList(virtualMethodsIterator)
        )
    }

    override val type: String
        get() = typeReference.type

    override val superclass: String?
        get() = superclassReference?.type

    override val sourceFile: String?
        get() = sourceFileReference?.string

    override val interfaces: List<String>
        get() = interfacesList.stream().map { iface -> iface.toString() }.collect(Collectors.toList())

    override val fields: Collection<BuilderField>
        get() = object : AbstractCollection<BuilderField>() {
            override fun iterator(): MutableIterator<BuilderField> {
                val fields = ArrayList<BuilderField>(staticFields)
                fields.addAll(instanceFields)
                fields.sortWith(CollectionUtils.naturalOrdering<BuilderField>())
                return fields.iterator()
            }

            override val size: Int
                get() = staticFields.size + instanceFields.size
        }

    override val methods: Collection<BuilderMethod>
        get() = object : AbstractCollection<BuilderMethod>() {
            override fun iterator(): MutableIterator<BuilderMethod> {
                val methods = ArrayList<BuilderMethod>(directMethods)
                methods.addAll(virtualMethods)
                methods.sortWith(CollectionUtils.naturalOrdering<BuilderMethod>())
                return methods.iterator()
            }

            override val size: Int
                get() = directMethods.size + virtualMethods.size
        }
}
