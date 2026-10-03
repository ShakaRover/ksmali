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


package com.android.tools.smali.dexlib2.analysis.reflection

import com.android.tools.smali.dexlib2.analysis.reflection.util.ReflectionUtils
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import java.lang.reflect.Modifier
import java.util.Collections

/**
 * Wraps a ClassDef around a class loaded in the current VM
 *
 * Only supports the basic information exposed by ClassProto
 */
open class ReflectionClassDef(private val cls: Class<*>) : BaseTypeReference(), ClassDef {
    override val accessFlags: Int
        // the java modifiers appear to be the same as the dex access flags
        get() = cls.modifiers

    override val superclass: String?
        get() {
            if (Modifier.isInterface(cls.modifiers)) {
                return "Ljava/lang/Object;"
            }
            val superClass = cls.superclass ?: return null
            return ReflectionUtils.javaToDexName(superClass.name)
        }

    override val interfaces: List<String>
        get() = cls.interfaces.map { ReflectionUtils.javaToDexName(it.name) }

    override val sourceFile: String?
        get() = null

    override val annotations: Set<Annotation>
        get() = Collections.emptySet()

    override val staticFields: Iterable<Field>
        get() = cls.declaredFields.asSequence()
            .filter { Modifier.isStatic(it.modifiers) }
            .map { ReflectionField(it) }
            .asIterable()

    override val instanceFields: Iterable<Field>
        get() = cls.declaredFields.asSequence()
            .filter { !Modifier.isStatic(it.modifiers) }
            .map { ReflectionField(it) }
            .asIterable()

    override val fields: Set<Field>
        get() = object : AbstractSet<Field>() {
            override fun iterator(): Iterator<Field> =
                cls.declaredFields.asSequence().map { ReflectionField(it) }.iterator()

            override val size: Int
                get() = cls.declaredFields.size
        }

    override val directMethods: Iterable<Method>
        get() {
            val constructorIterator = cls.declaredConstructors.asSequence().map { ReflectionConstructor(it) }
            val methodIterator = cls.declaredMethods.asSequence()
                .filter { (it.modifiers and DIRECT_MODIFIERS) != 0 }
                .map { ReflectionMethod(it) }
            return (constructorIterator + methodIterator).asIterable()
        }

    override val virtualMethods: Iterable<Method>
        get() = cls.declaredMethods.asSequence()
            .filter { (it.modifiers and DIRECT_MODIFIERS) == 0 }
            .map { ReflectionMethod(it) }
            .asIterable()

    override val methods: Set<Method>
        get() = object : AbstractSet<Method>() {
            override fun iterator(): Iterator<Method> {
                val constructorIterator =
                    cls.declaredConstructors.asSequence().map { ReflectionConstructor(it) }
                val methodIterator = cls.declaredMethods.asSequence().map { ReflectionMethod(it) }
                return (constructorIterator + methodIterator).iterator()
            }

            override val size: Int
                get() = cls.declaredMethods.size + cls.declaredConstructors.size
        }

    override val type: String
        get() = ReflectionUtils.javaToDexName(cls.name)

    companion object {
        private val DIRECT_MODIFIERS = Modifier.PRIVATE or Modifier.STATIC
    }
}
