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

package com.android.tools.smali.dexlib2.writer

import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.io.IOException

interface ClassSection<StringKey : CharSequence, TypeKey : CharSequence, TypeListKey, ClassKey,
    FieldKey, MethodKey, AnnotationSetKey, EncodedArrayKey> : IndexSection<ClassKey> {
    val sortedClasses: Collection<@JvmWildcard ClassKey>

    fun getClassEntryByType(key: TypeKey?): MutableMap.MutableEntry<@JvmWildcard ClassKey, Int>?

    fun getType(key: ClassKey): TypeKey
    fun getAccessFlags(key: ClassKey): Int
    fun getSuperclass(key: ClassKey): TypeKey?
    fun getInterfaces(key: ClassKey): TypeListKey?
    fun getSourceFile(key: ClassKey): StringKey?
    fun getStaticInitializers(key: ClassKey): EncodedArrayKey?

    fun getSortedStaticFields(key: ClassKey): Collection<@JvmWildcard FieldKey>
    fun getSortedInstanceFields(key: ClassKey): Collection<@JvmWildcard FieldKey>
    fun getSortedFields(key: ClassKey): Collection<@JvmWildcard FieldKey>
    fun getSortedDirectMethods(key: ClassKey): Collection<@JvmWildcard MethodKey>
    fun getSortedVirtualMethods(key: ClassKey): Collection<@JvmWildcard MethodKey>
    fun getSortedMethods(key: ClassKey): Collection<@JvmWildcard MethodKey>

    fun getFieldAccessFlags(key: FieldKey): Int
    fun getMethodAccessFlags(key: MethodKey): Int

    fun getFieldHiddenApiRestrictions(key: FieldKey): Set<HiddenApiRestriction>
    fun getMethodHiddenApiRestrictions(key: MethodKey): Set<HiddenApiRestriction>

    fun getClassAnnotations(key: ClassKey): AnnotationSetKey?
    fun getFieldAnnotations(key: FieldKey): AnnotationSetKey?
    fun getMethodAnnotations(key: MethodKey): AnnotationSetKey?
    fun getParameterAnnotations(key: MethodKey): List<@JvmWildcard AnnotationSetKey>?

    fun getDebugItems(key: MethodKey): Iterable<@JvmWildcard DebugItem>?
    fun getParameterNames(key: MethodKey): Iterable<@JvmWildcard StringKey?>?

    fun getRegisterCount(key: MethodKey): Int
    fun getInstructions(key: MethodKey): Iterable<@JvmWildcard Instruction>?
    fun getTryBlocks(key: MethodKey): List<@JvmWildcard TryBlock<out ExceptionHandler>>
    fun getExceptionType(handler: ExceptionHandler): TypeKey?
    fun makeMutableMethodImplementation(key: MethodKey): MutableMethodImplementation

    fun setAnnotationDirectoryOffset(key: ClassKey, offset: Int)
    fun getAnnotationDirectoryOffset(key: ClassKey): Int

    fun setAnnotationSetRefListOffset(key: MethodKey, offset: Int)
    fun getAnnotationSetRefListOffset(key: MethodKey): Int

    fun setCodeItemOffset(key: MethodKey, offset: Int)
    fun getCodeItemOffset(key: MethodKey): Int

    @Throws(IOException::class)
    fun writeDebugItem(writer: DebugWriter<StringKey, TypeKey>, debugItem: DebugItem)
}
