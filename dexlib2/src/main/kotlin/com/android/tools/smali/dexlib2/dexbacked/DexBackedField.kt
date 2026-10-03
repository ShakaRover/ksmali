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
import com.android.tools.smali.dexlib2.base.reference.BaseFieldReference
import com.android.tools.smali.dexlib2.dexbacked.raw.FieldIdItem
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedFieldReference
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory.AnnotationIterator
import com.android.tools.smali.dexlib2.dexbacked.util.EncodedArrayItemIterator
import com.android.tools.smali.dexlib2.dexbacked.value.DexBackedEncodedValue
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import java.util.EnumSet

class DexBackedField(
    val dexFile: DexBackedDexFile,
    reader: DexReader<out DexBuffer>,
    val classDef: ClassDef,
    previousFieldIndex: Int,
    staticInitialValueIterator: EncodedArrayItemIterator?,
    annotationIterator: AnnotationIterator,
    hiddenApiRestrictions: Int
) : BaseFieldReference(), Field {
    override val accessFlags: Int
    override val initialValue: EncodedValue?

    val annotationSetOffset: Int

    val fieldIndex: Int
    private val startOffset: Int
    private val initialValueOffset: Int
    private val hiddenApiRestrictionFlags: Int

    private var fieldIdItemOffset = 0

    init {
        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        startOffset = reader.offset
        val fieldIndexDiff = reader.readLargeUleb128()
        this.fieldIndex = fieldIndexDiff + previousFieldIndex
        this.accessFlags = reader.readSmallUleb128()

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex)
        if (staticInitialValueIterator != null) {
            initialValueOffset = staticInitialValueIterator.getReaderOffset()
            this.initialValue = staticInitialValueIterator.getNextOrNull()
        } else {
            initialValueOffset = 0
            this.initialValue = null
        }
        this.hiddenApiRestrictionFlags = hiddenApiRestrictions
    }

    constructor(
        dexFile: DexBackedDexFile,
        reader: DexReader<out DexBuffer>,
        classDef: DexBackedClassDef,
        previousFieldIndex: Int,
        annotationIterator: AnnotationIterator,
        hiddenApiRestrictions: Int
    ) : this(
        dexFile, reader, classDef, previousFieldIndex, null, annotationIterator,
        hiddenApiRestrictions
    )

    override val name: String
        get() = dexFile.stringSection.get(
            dexFile.buffer.readSmallUint(getFieldIdItemOffset() + FieldIdItem.NAME_OFFSET)
        )

    override val type: String
        get() = dexFile.typeSection.get(
            dexFile.buffer.readUshort(getFieldIdItemOffset() + FieldIdItem.TYPE_OFFSET)
        )

    override val definingClass: String
        get() = classDef.type

    override val annotations: Set<DexBackedAnnotation>
        get() = AnnotationsDirectory.getAnnotations(dexFile, annotationSetOffset)

    override val hiddenApiRestrictions: Set<HiddenApiRestriction>
        get() {
            if (hiddenApiRestrictionFlags == DexBackedClassDef.NO_HIDDEN_API_RESTRICTIONS) {
                return emptySet()
            } else {
                return EnumSet.copyOf(HiddenApiRestriction.getAllFlags(hiddenApiRestrictionFlags))
            }
        }

    /**
     * Calculate and return the private size of a field definition.
     *
     * Calculated as: field_idx_diff + access_flags + annotations overhead +
     * initial value size + field reference size
     *
     * @return size in bytes
     */
    fun getSize(): Int {
        var size = 0
        val reader: DexReader<out DexBuffer> = dexFile.buffer.readerAt(startOffset)
        reader.readLargeUleb128() //field_idx_diff
        reader.readSmallUleb128() //access_flags
        size += reader.offset - startOffset

        val annotations = this.annotations
        if (annotations.isNotEmpty()) {
            size += 2 * 4 //2 * uint overhead from field_annotation
        }

        if (initialValueOffset > 0) {
            reader.offset = initialValueOffset
            if (initialValue != null) {
                DexBackedEncodedValue.skipFrom(reader)
                size += reader.offset - initialValueOffset
            }
        }

        val fieldRef = DexBackedFieldReference(dexFile, fieldIndex)
        size += fieldRef.getSize()

        return size
    }

    private fun getFieldIdItemOffset(): Int {
        if (fieldIdItemOffset == 0) {
            fieldIdItemOffset = dexFile.fieldSection.getOffset(fieldIndex)
        }
        return fieldIdItemOffset
    }

    companion object {
        /**
         * Skips the reader over the specified number of encoded_field structures
         *
         * @param reader The reader to skip
         * @param count The number of encoded_field structures to skip over
         */
        fun skipFields(reader: DexReader<out DexBuffer>, count: Int) {
            for (i in 0 until count) {
                reader.skipUleb128()
                reader.skipUleb128()
            }
        }
    }
}
