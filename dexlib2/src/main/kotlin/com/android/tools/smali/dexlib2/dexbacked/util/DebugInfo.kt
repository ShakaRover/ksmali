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

package com.android.tools.smali.dexlib2.dexbacked.util

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.DebugItemType
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedMethod
import com.android.tools.smali.dexlib2.dexbacked.DexBackedMethodImplementation
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.DexReader
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.debug.EndLocal
import com.android.tools.smali.dexlib2.iface.debug.LocalInfo
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableEndLocal
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableEpilogueBegin
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableLineNumber
import com.android.tools.smali.dexlib2.immutable.debug.ImmutablePrologueEnd
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableRestartLocal
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableSetSourceFile
import com.android.tools.smali.dexlib2.immutable.debug.ImmutableStartLocal
import com.android.tools.smali.util.IteratorUtils
import java.util.Arrays
import java.util.Collections

abstract class DebugInfo : Iterable<DebugItem> {
    /**
     * Gets an iterator that yields the parameter names from the debug_info_item
     *
     * @param reader Optional. If provided, the reader must be positioned at the debug_info_item.parameters_size
     *               field, and will
     * @return An iterator that yields the parameter names as strings
     */
    abstract fun getParameterNames(reader: DexReader<out DexBuffer>?): MutableIterator<String?>

    /**
     * Calculate and return the private size of debuginfo.
     *
     * @return size in bytes
     */
    abstract fun getSize(): Int

    private class EmptyDebugInfo : DebugInfo() {
        override fun iterator(): MutableIterator<DebugItem> = Collections.emptyIterator()

        override fun getParameterNames(
            reader: DexReader<out DexBuffer>?
        ): MutableIterator<String?> = Collections.emptyIterator()

        override fun getSize(): Int = 0

        companion object {
            val INSTANCE = EmptyDebugInfo()
        }
    }

    private class DebugInfoImpl(
        val dexFile: DexBackedDexFile,
        private val debugInfoOffset: Int,
        private val methodImpl: DexBackedMethodImplementation
    ) : DebugInfo() {
        override fun iterator(): VariableSizeLookaheadIterator<DebugItem> {
            val reader = dexFile.dataBuffer.readerAt(debugInfoOffset)
            val lineNumberStart = reader.readBigUleb128()
            val registerCount = methodImpl.registerCount
            // Debug information can have events for addresses past the instructions.
            // They have no relevance for the method in question and are excluded from the iterator.
            val lastInstructionAddress =
                methodImpl.instructionsSize -
                    IteratorUtils.getLast(methodImpl.instructions.iterator()).codeUnits

            //TODO: does dalvik allow references to invalid registers?
            val locals = arrayOfNulls<LocalInfo>(registerCount)
            Arrays.fill(locals, EMPTY_LOCAL_INFO)

            val method: DexBackedMethod = methodImpl.method

            // Create a MethodParameter iterator that uses our DexReader instance to read the parameter names.
            // After we have finished iterating over the parameters, reader will "point to" the beginning of the
            // debug instructions
            val parameterIterator: Iterator<MethodParameter> = ParameterIterator(
                method.parameterTypes,
                method.parameterAnnotations,
                getParameterNames(reader)
            )

            // first, we grab all the parameters and temporarily store them at the beginning of locals,
            // disregarding any wide types
            var parameterIndex = 0
            if (!AccessFlags.STATIC.isSet(methodImpl.method.accessFlags)) {
                // add the local info for the "this" parameter
                locals[parameterIndex++] = object : LocalInfo {
                    override val name: String
                        get() = "this"

                    override val type: String
                        get() = methodImpl.method.definingClass

                    override val signature: String?
                        get() = null
                }
            }
            while (parameterIterator.hasNext()) {
                locals[parameterIndex++] = parameterIterator.next()
            }

            if (parameterIndex < registerCount) {
                // now, we push the parameter locals back to their appropriate register, starting from the end
                var localIndex = registerCount - 1
                while (--parameterIndex > -1) {
                    val currentLocal = locals[parameterIndex]!!
                    val type = currentLocal.type
                    if (type != null && (type == "J" || type == "D")) {
                        localIndex--
                        if (localIndex == parameterIndex) {
                            // there's no more room to push, the remaining registers are already in the correct place
                            break
                        }
                    }
                    locals[localIndex] = currentLocal
                    locals[parameterIndex] = EMPTY_LOCAL_INFO
                    localIndex--
                }
            }

            return object : VariableSizeLookaheadIterator<DebugItem>(
                dexFile.dataBuffer, reader.offset
            ) {
                private var codeAddress = 0
                private var lineNumber = lineNumberStart

                override fun readNextItem(reader: DexReader<out DexBuffer>): DebugItem? {
                    while (codeAddress <= lastInstructionAddress) {
                        val next = reader.readUbyte()
                        when (next) {
                            DebugItemType.END_SEQUENCE -> return endOfData()
                            DebugItemType.ADVANCE_PC -> {
                                val addressDiff = reader.readSmallUleb128()
                                codeAddress += addressDiff
                            }
                            DebugItemType.ADVANCE_LINE -> {
                                val lineDiff = reader.readSleb128()
                                lineNumber += lineDiff
                            }
                            DebugItemType.START_LOCAL -> {
                                val register = reader.readSmallUleb128()
                                val name = dexFile.stringSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                val type = dexFile.typeSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                val startLocal =
                                    ImmutableStartLocal(codeAddress, register, name, type, null)
                                if (register >= 0 && register < locals.size) {
                                    locals[register] = startLocal
                                }
                                return startLocal
                            }
                            DebugItemType.START_LOCAL_EXTENDED -> {
                                val register = reader.readSmallUleb128()
                                val name = dexFile.stringSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                val type = dexFile.typeSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                val signature = dexFile.stringSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                val startLocal = ImmutableStartLocal(
                                    codeAddress, register, name, type, signature
                                )
                                if (register >= 0 && register < locals.size) {
                                    locals[register] = startLocal
                                }
                                return startLocal
                            }
                            DebugItemType.END_LOCAL -> {
                                val register = reader.readSmallUleb128()

                                var replaceLocalInTable = true
                                var localInfo: LocalInfo
                                if (register >= 0 && register < locals.size) {
                                    localInfo = locals[register]!!
                                } else {
                                    localInfo = EMPTY_LOCAL_INFO
                                    replaceLocalInTable = false
                                }

                                if (localInfo is EndLocal) {
                                    localInfo = EMPTY_LOCAL_INFO
                                    // don't replace the local info in locals. The new EndLocal won't have any info at all,
                                    // and we dont want to wipe out what's there, so that it is available for a subsequent
                                    // RestartLocal
                                    replaceLocalInTable = false
                                }
                                val endLocal = ImmutableEndLocal(
                                    codeAddress, register, localInfo.name,
                                    localInfo.type, localInfo.signature
                                )
                                if (replaceLocalInTable) {
                                    locals[register] = endLocal
                                }
                                return endLocal
                            }
                            DebugItemType.RESTART_LOCAL -> {
                                val register = reader.readSmallUleb128()
                                val localInfo: LocalInfo
                                if (register >= 0 && register < locals.size) {
                                    localInfo = locals[register]!!
                                } else {
                                    localInfo = EMPTY_LOCAL_INFO
                                }
                                val restartLocal = ImmutableRestartLocal(
                                    codeAddress, register, localInfo.name,
                                    localInfo.type, localInfo.signature
                                )
                                if (register >= 0 && register < locals.size) {
                                    locals[register] = restartLocal
                                }
                                return restartLocal
                            }
                            DebugItemType.PROLOGUE_END -> {
                                return ImmutablePrologueEnd(codeAddress)
                            }
                            DebugItemType.EPILOGUE_BEGIN -> {
                                return ImmutableEpilogueBegin(codeAddress)
                            }
                            DebugItemType.SET_SOURCE_FILE -> {
                                val sourceFile = dexFile.stringSection.getOptional(
                                    reader.readSmallUleb128() - 1
                                )
                                return ImmutableSetSourceFile(codeAddress, sourceFile)
                            }
                            else -> {
                                val adjusted = next - 0x0A
                                codeAddress += adjusted / 15
                                lineNumber += (adjusted % 15) - 4
                                if (codeAddress > lastInstructionAddress) {
                                    return endOfData()
                                }
                                return ImmutableLineNumber(codeAddress, lineNumber)
                            }
                        }
                    }
                    return endOfData()
                }
            }
        }
        override fun getParameterNames(
            reader: DexReader<out DexBuffer>?
        ): VariableSizeIterator<String?> {
            var r = reader
            if (r == null) {
                r = dexFile.dataBuffer.readerAt(debugInfoOffset)
                r.skipUleb128()
            }
            //TODO: make sure dalvik doesn't allow more parameter names than we have parameters
            val parameterNameCount = r.readSmallUleb128()
            return object : VariableSizeIterator<String?>(r, parameterNameCount) {
                override fun readNextItem(
                    reader: DexReader<out DexBuffer>,
                    index: Int
                ): String? = dexFile.stringSection.getOptional(reader.readSmallUleb128() - 1)
            }
        }

        override fun getSize(): Int {
            val iter = iterator()
            while (iter.hasNext()) {
                iter.next()
            }
            return iter.getReaderOffset() - debugInfoOffset
        }

        companion object {
            private val EMPTY_LOCAL_INFO: LocalInfo = object : LocalInfo {
                override val name: String?
                    get() = null

                override val type: String?
                    get() = null

                override val signature: String?
                    get() = null
            }
        }
    }

    companion object {
        fun newOrEmpty(
            dexFile: DexBackedDexFile,
            debugInfoOffset: Int,
            methodImpl: DexBackedMethodImplementation
        ): DebugInfo {
            if (debugInfoOffset == 0) {
                return EmptyDebugInfo.INSTANCE
            }
            return DebugInfoImpl(dexFile, debugInfoOffset, methodImpl)
        }
    }
}
