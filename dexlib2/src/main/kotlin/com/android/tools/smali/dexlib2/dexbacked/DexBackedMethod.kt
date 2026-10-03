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

package com.android.tools.smali.dexlib2.dexbacked

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeListItem
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodReference
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory.AnnotationIterator
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList
import com.android.tools.smali.dexlib2.dexbacked.util.ParameterIterator
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.util.AbstractForwardSequentialList
import java.util.Collections
import java.util.EnumSet

class DexBackedMethod(
    val dexFile: DexBackedDexFile,
    reader: DexReader<out DexBuffer>,
    val classDef: DexBackedClassDef,
    previousMethodIndex: Int,
    methodAnnotationIterator: AnnotationIterator?,
    paramaterAnnotationIterator: AnnotationIterator?,
    hiddenApiRestrictions: Int
) : BaseMethodReference(), Method {
    override val accessFlags: Int

    private val codeOffset: Int
    private val parameterAnnotationSetListOffset: Int
    private val methodAnnotationSetOffset: Int
    private val hiddenApiRestrictionFlags: Int

    val methodIndex: Int
    private val startOffset: Int

    private var methodIdItemOffset = 0
    private var protoIdItemOffset = 0
    private var parametersOffset = -1

    init {
        startOffset = reader.offset

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        val methodIndexDiff = reader.readLargeUleb128()
        this.methodIndex = methodIndexDiff + previousMethodIndex
        this.accessFlags = reader.readSmallUleb128()
        this.codeOffset = reader.readSmallUleb128()
        this.hiddenApiRestrictionFlags = hiddenApiRestrictions

        this.methodAnnotationSetOffset = methodAnnotationIterator?.seekTo(methodIndex) ?: 0
        this.parameterAnnotationSetListOffset =
            paramaterAnnotationIterator?.seekTo(methodIndex) ?: 0
    }

    constructor(
        dexFile: DexBackedDexFile,
        reader: DexReader<out DexBuffer>,
        classDef: DexBackedClassDef,
        previousMethodIndex: Int,
        hiddenApiRestrictions: Int
    ) : this(dexFile, reader, classDef, previousMethodIndex, null, null, hiddenApiRestrictions)

    override val definingClass: String
        get() = classDef.type

    override val name: String
        get() = dexFile.stringSection.get(
            dexFile.buffer.readSmallUint(getMethodIdItemOffset() + MethodIdItem.NAME_OFFSET)
        )

    override val returnType: String
        get() = dexFile.typeSection.get(
            dexFile.buffer.readSmallUint(
                getProtoIdItemOffset() + ProtoIdItem.RETURN_TYPE_OFFSET
            )
        )

    override val parameters: List<MethodParameter>
        get() {
            val parametersOffset = getParametersOffset()
            if (parametersOffset > 0) {
                val parameterTypes = this.parameterTypes

                return object : AbstractForwardSequentialList<MethodParameter>() {
                    override fun iterator(): MutableIterator<MethodParameter> {
                        return ParameterIterator(
                            parameterTypes,
                            parameterAnnotations,
                            parameterNames
                        )
                    }

                    override val size: Int
                        get() = parameterTypes.size
                }
            }
            return emptyList()
        }

    val parameterAnnotations: List<Set<DexBackedAnnotation>>
        get() = AnnotationsDirectory.getParameterAnnotations(
            dexFile, parameterAnnotationSetListOffset
        )

    val parameterNames: MutableIterator<String?>
        get() {
            val methodImpl = implementation
            if (methodImpl != null) {
                return methodImpl.getParameterNames(null)
            }
            return Collections.emptyIterator()
        }

    override val parameterTypes: List<String>
        get() {
            val parametersOffset = getParametersOffset()
            if (parametersOffset > 0) {
                val parameterCount = dexFile.dataBuffer.readSmallUint(
                    parametersOffset + TypeListItem.SIZE_OFFSET
                )
                val paramListStart = parametersOffset + TypeListItem.LIST_OFFSET
                return object : FixedSizeList<String>() {
                    override val size: Int
                        get() = parameterCount

                    override fun readItem(index: Int): String {
                        return dexFile.typeSection.get(
                            dexFile.dataBuffer.readUshort(paramListStart + 2 * index)
                        )
                    }
                }
            }
            return emptyList()
        }

    override val annotations: Set<DexBackedAnnotation>
        get() = AnnotationsDirectory.getAnnotations(dexFile, methodAnnotationSetOffset)

    override val hiddenApiRestrictions: Set<HiddenApiRestriction>
        get() {
            if (hiddenApiRestrictionFlags == DexBackedClassDef.NO_HIDDEN_API_RESTRICTIONS) {
                return emptySet()
            } else {
                return EnumSet.copyOf(HiddenApiRestriction.getAllFlags(hiddenApiRestrictionFlags))
            }
        }

    override val implementation: DexBackedMethodImplementation?
        get() {
            if (codeOffset > 0) {
                return dexFile.createMethodImplementation(dexFile, this, codeOffset)
            }
            return null
        }

    /**
     * Calculate and return the private size of a method definition.
     *
     * Calculated as: method_idx_diff + access_flags + code_off +
     * implementation size + reference size
     *
     * @return size in bytes
     */
    fun getSize(): Int {
        var size = 0

        val reader: DexReader<out DexBuffer> = dexFile.dataBuffer.readerAt(startOffset)
        reader.readLargeUleb128() //method_idx_diff
        reader.readSmallUleb128() //access_flags
        reader.readSmallUleb128() //code_off
        size += reader.offset - startOffset

        val impl = implementation
        if (impl != null) {
            size += impl.getSize()
        }

        val methodRef = DexBackedMethodReference(dexFile, methodIndex)
        size += methodRef.getSize()

        return size
    }

    private fun getMethodIdItemOffset(): Int {
        if (methodIdItemOffset == 0) {
            methodIdItemOffset = dexFile.methodSection.getOffset(methodIndex)
        }
        return methodIdItemOffset
    }

    private fun getProtoIdItemOffset(): Int {
        if (protoIdItemOffset == 0) {
            val protoIndex =
                dexFile.buffer.readUshort(getMethodIdItemOffset() + MethodIdItem.PROTO_OFFSET)
            protoIdItemOffset = dexFile.protoSection.getOffset(protoIndex)
        }
        return protoIdItemOffset
    }

    private fun getParametersOffset(): Int {
        if (parametersOffset == -1) {
            parametersOffset = dexFile.buffer.readSmallUint(
                getProtoIdItemOffset() + ProtoIdItem.PARAMETERS_OFFSET
            )
        }
        return parametersOffset
    }

    companion object {
        /**
         * Skips the reader over the specified number of encoded_method structures
         *
         * @param reader The reader to skip
         * @param count The number of encoded_method structures to skip over
         */
        fun skipMethods(reader: DexReader<out DexBuffer>, count: Int) {
            for (i in 0 until count) {
                reader.skipUleb128()
                reader.skipUleb128()
                reader.skipUleb128()
            }
        }
    }
}
