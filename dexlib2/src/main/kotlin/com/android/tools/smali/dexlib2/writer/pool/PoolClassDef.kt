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

package com.android.tools.smali.dexlib2.writer.pool

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.util.ArraySortedSet
import com.android.tools.smali.util.CollectionUtils
import com.android.tools.smali.util.IteratorUtils
import java.util.AbstractCollection
import java.util.ArrayList
import java.util.Collections
import java.util.SortedSet
import java.util.stream.Collectors

class PoolClassDef(val classDef: ClassDef) : BaseTypeReference(), ClassDef {
    val interfacesKey: TypeListPool.Key<List<String>> = TypeListPool.Key(
        Collections.unmodifiableList(ArrayList(classDef.interfaces))
    )
    override val staticFields: SortedSet<Field> = ArraySortedSet.copyOf(
        CollectionUtils.naturalOrdering(),
        IteratorUtils.toList(classDef.staticFields)
    )
    override val instanceFields: SortedSet<Field> = ArraySortedSet.copyOf(
        CollectionUtils.naturalOrdering(),
        IteratorUtils.toList(classDef.instanceFields)
    )
    override val directMethods: SortedSet<PoolMethod> = ArraySortedSet.copyOf(
        CollectionUtils.naturalOrdering(),
        IteratorUtils.toList(classDef.directMethods).stream().map(PoolMethod.TRANSFORM)
            .collect(Collectors.toList())
    )
    override val virtualMethods: SortedSet<PoolMethod> = ArraySortedSet.copyOf(
        CollectionUtils.naturalOrdering(),
        IteratorUtils.toList(classDef.virtualMethods).stream().map(PoolMethod.TRANSFORM)
            .collect(Collectors.toList())
    )

    var classDefIndex = DexWriter.NO_INDEX
    var annotationDirectoryOffset = DexWriter.NO_OFFSET

    override val type: String
        get() = classDef.type

    override val accessFlags: Int
        get() = classDef.accessFlags

    override val superclass: String?
        get() = classDef.superclass

    override val interfaces: List<String>
        get() = interfacesKey.types

    override val sourceFile: String?
        get() = classDef.sourceFile

    override val annotations: Set<@JvmWildcard Annotation>
        get() = classDef.annotations

    override val fields: Collection<Field>
        get() = object : AbstractCollection<Field>() {
            override fun iterator(): MutableIterator<Field> {
                val fields = ArrayList<Field>(staticFields)
                fields.addAll(instanceFields)
                fields.sortWith(CollectionUtils.naturalOrdering<Field>())
                return fields.iterator()
            }

            override val size: Int
                get() = staticFields.size + instanceFields.size
        }

    override val methods: Collection<PoolMethod>
        get() = object : AbstractCollection<PoolMethod>() {
            override fun iterator(): MutableIterator<PoolMethod> {
                val methods = ArrayList<PoolMethod>(directMethods)
                methods.addAll(virtualMethods)
                methods.sortWith(CollectionUtils.naturalOrdering<PoolMethod>())
                return methods.iterator()
            }

            override val size: Int
                get() = directMethods.size + virtualMethods.size
        }
}
