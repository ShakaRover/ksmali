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


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.analysis.reflection.ReflectionClassDef
import com.android.tools.smali.dexlib2.analysis.util.LruCache
import com.android.tools.smali.dexlib2.analysis.util.MemoizingSupplier
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.util.IteratorUtils
import java.io.IOException
import java.io.Serializable
import java.util.Collections
import java.util.HashSet
import java.util.function.Supplier

open class ClassPath(
    providers: Iterable<ClassProvider>,
    private val checkPackagePrivateAccess: Boolean,
    val oatVersion: Int
) {
    val unknownClass: TypeProto = UnknownClassProto(this)

    private val loadedClasses: LruCache<String, TypeProto> = object : LruCache<String, TypeProto>(30000) {
        override fun create(key: String): TypeProto {
            if (key[0] == '[') {
                return ArrayProto(this@ClassPath, key)
            } else {
                return ClassProto(this@ClassPath, key)
            }
        }
    }

    private var classProviders: MutableList<ClassProvider>

    private val fieldInstructionMapperSupplier: Supplier<OdexedFieldInstructionMapper> =
        MemoizingSupplier.memoize(Supplier { OdexedFieldInstructionMapper(isArt) })

    init {
        // add fallbacks for certain special classes that must be present
        loadedClasses.put(unknownClass.type, unknownClass)

        loadPrimitiveType("Z")
        loadPrimitiveType("B")
        loadPrimitiveType("S")
        loadPrimitiveType("C")
        loadPrimitiveType("I")
        loadPrimitiveType("J")
        loadPrimitiveType("F")
        loadPrimitiveType("D")
        loadPrimitiveType("L")

        classProviders = IteratorUtils.toList(providers)
        classProviders.add(basicClasses)
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders A varargs array of ClassProviders. When loading a class, these providers will be searched
     *                       in order
     */
    @Throws(IOException::class)
    constructor(vararg classProviders: ClassProvider) :
        this(listOf(*classProviders), false, NOT_ART)

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders An iterable of ClassProviders. When loading a class, these providers will be searched in
     *                       order
     */
    @Throws(IOException::class)
    constructor(classProviders: Iterable<ClassProvider>) :
        this(classProviders, false, NOT_ART)

    private fun loadPrimitiveType(type: String) {
        loadedClasses.put(type, PrimitiveProto(this, type))
    }

    val isArt: Boolean get() {
        return oatVersion != NOT_ART
    }

    fun getClass(type: CharSequence): TypeProto {
        return loadedClasses.get(type.toString())!!
    }

    fun getClassDef(type: String): ClassDef {
        for (provider in classProviders) {
            val classDef = provider.getClassDef(type)
            if (classDef != null) {
                return classDef
            }
        }
        throw UnresolvedClassException("Could not resolve class %s", type)
    }

    fun shouldCheckPackagePrivateAccess(): Boolean {
        return checkPackagePrivateAccess
    }

    val fieldInstructionMapper: OdexedFieldInstructionMapper get() {
        return fieldInstructionMapperSupplier.get()
    }

    companion object {
        val NOT_ART: Int = -1

        val NOT_SPECIFIED: Int = -2

        private val basicClasses: ClassProvider get() {
            // fallbacks for some special classes that we assume are present
            return DexClassProvider(
                ImmutableDexFile(
                    Opcodes.default,
                    Collections.unmodifiableSet(
                        HashSet(
                            listOf(
                                ReflectionClassDef(Class::class.java),
                                ReflectionClassDef(Cloneable::class.java),
                                ReflectionClassDef(Object::class.java),
                                ReflectionClassDef(Serializable::class.java),
                                ReflectionClassDef(String::class.java),
                                ReflectionClassDef(Throwable::class.java)
                            )
                        )
                    )
                )
            )
        }
    }
}
