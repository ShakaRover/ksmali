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

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.reference.BaseMethodHandleReference
import com.android.tools.smali.dexlib2.base.reference.BaseMethodProtoReference
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.base.value.BaseFieldEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue
import com.android.tools.smali.util.ExceptionWithContext
import java.util.AbstractList
import java.util.AbstractSet

object RewriterUtils {
    fun <T> rewriteNullable(rewriter: Rewriter<T>, value: T?): T? {
        return if (value == null) null else rewriter.rewrite(value)
    }

    fun <T> rewriteSet(rewriter: Rewriter<T>, set: Set<T>): Set<T> {
        return object : AbstractSet<T>() {
            override fun iterator(): MutableIterator<T> {
                val iterator = set.iterator() as MutableIterator<T>
                return object : MutableIterator<T> {
                    override fun hasNext(): Boolean {
                        return iterator.hasNext()
                    }

                    override fun next(): T {
                        return rewriter.rewrite(iterator.next())
                    }

                    override fun remove() {
                        iterator.remove()
                    }
                }
            }

            override val size: Int
                get() = set.size
        }
    }

    fun <T> rewriteList(rewriter: Rewriter<T>, list: List<T>): List<T> {
        return object : AbstractList<T>() {
            override fun get(i: Int): T {
                return rewriter.rewrite(list[i])
            }

            override val size: Int
                get() = list.size
        }
    }

    fun <T> rewriteIterable(rewriter: Rewriter<T>, iterable: Iterable<T>): Iterable<T> {
        return object : Iterable<T> {
            override fun iterator(): MutableIterator<T> {
                val iterator = iterable.iterator() as MutableIterator<T>
                return object : MutableIterator<T> {
                    override fun hasNext(): Boolean {
                        return iterator.hasNext()
                    }

                    override fun next(): T {
                        return rewriter.rewrite(iterator.next())
                    }

                    override fun remove() {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun rewriteTypeReference(typeRewriter: Rewriter<String>, typeReference: TypeReference): TypeReference {
        return object : BaseTypeReference() {
            override val type: String
                get() = typeRewriter.rewrite(typeReference.type)
        }
    }

    fun rewriteMethodHandleReference(
        rewriters: Rewriters,
        methodHandleReference: MethodHandleReference
    ): MethodHandleReference {
        when (methodHandleReference.methodHandleType) {
            MethodHandleType.STATIC_PUT,
            MethodHandleType.STATIC_GET,
            MethodHandleType.INSTANCE_PUT,
            MethodHandleType.INSTANCE_GET ->
                return object : BaseMethodHandleReference() {
                    override val methodHandleType: Int
                        get() = methodHandleReference.methodHandleType

                    override val memberReference: Reference
                        get() = rewriters.fieldReferenceRewriter.rewrite(
                            methodHandleReference.memberReference as FieldReference
                        )
                }
            MethodHandleType.INVOKE_STATIC,
            MethodHandleType.INVOKE_INSTANCE,
            MethodHandleType.INVOKE_CONSTRUCTOR,
            MethodHandleType.INVOKE_DIRECT,
            MethodHandleType.INVOKE_INTERFACE ->
                return object : BaseMethodHandleReference() {
                    override val methodHandleType: Int
                        get() = methodHandleReference.methodHandleType

                    override val memberReference: Reference
                        get() = rewriters.methodReferenceRewriter.rewrite(
                            methodHandleReference.memberReference as MethodReference
                        )
                }
            else -> throw ExceptionWithContext(
                "Invalid method handle type: %d",
                methodHandleReference.methodHandleType
            )
        }
    }

    fun rewriteMethodProtoReference(
        typeRewriter: Rewriter<String>,
        methodProtoReference: MethodProtoReference
    ): MethodProtoReference {
        return object : BaseMethodProtoReference() {
            override val parameterTypes: List<CharSequence>
                get() = rewriteList(
                    typeRewriter,
                    methodProtoReference.parameterTypes.map { it.toString() }
                )

            override val returnType: String
                get() = typeRewriter.rewrite(methodProtoReference.returnType)
        }
    }

    fun rewriteValue(rewriters: Rewriters, encodedValue: EncodedValue): EncodedValue {
        when (encodedValue.valueType) {
            ValueType.INT,
            ValueType.FLOAT,
            ValueType.LONG,
            ValueType.DOUBLE,
            ValueType.STRING -> return encodedValue
            ValueType.METHOD_TYPE ->
                return object : BaseMethodTypeEncodedValue() {
                    override val value: MethodProtoReference
                        get() = rewriteMethodProtoReference(
                            rewriters.typeRewriter,
                            (encodedValue as MethodTypeEncodedValue).value
                        )
                }
            ValueType.METHOD_HANDLE ->
                return object : BaseMethodHandleEncodedValue() {
                    override val value: MethodHandleReference
                        get() = rewriteMethodHandleReference(
                            rewriters,
                            (encodedValue as MethodHandleEncodedValue).value
                        )
                }
            ValueType.TYPE ->
                return object : BaseTypeEncodedValue() {
                    override val value: String
                        get() = rewriters.typeRewriter.rewrite(
                            (encodedValue as TypeEncodedValue).value
                        )
                }
            ValueType.FIELD ->
                return object : BaseFieldEncodedValue() {
                    override val value: FieldReference
                        get() = rewriters.fieldReferenceRewriter.rewrite(
                            (encodedValue as FieldEncodedValue).value
                        )
                }
            ValueType.METHOD ->
                return object : BaseMethodEncodedValue() {
                    override val value: MethodReference
                        get() = rewriters.methodReferenceRewriter.rewrite(
                            (encodedValue as MethodEncodedValue).value
                        )
                }
            else -> throw ExceptionWithContext(
                "Unsupported encoded value type: %d",
                encodedValue.valueType
            )
        }
    }
}
