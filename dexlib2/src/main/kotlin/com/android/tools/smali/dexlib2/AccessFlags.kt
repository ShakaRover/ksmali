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

package com.android.tools.smali.dexlib2

import java.util.HashMap

enum class AccessFlags(
    val value: Int,
    private val accessFlagName: String,
    private val validForClass: Boolean,
    private val validForMethod: Boolean,
    private val validForField: Boolean
) {
    PUBLIC(0x1, "public", true, true, true),
    PRIVATE(0x2, "private", true, true, true),
    PROTECTED(0x4, "protected", true, true, true),
    STATIC(0x8, "static", true, true, true),
    FINAL(0x10, "final", true, true, true),
    SYNCHRONIZED(0x20, "synchronized", false, true, false),
    VOLATILE(0x40, "volatile", false, false, true),
    BRIDGE(0x40, "bridge", false, true, false),
    TRANSIENT(0x80, "transient", false, false, true),
    VARARGS(0x80, "varargs", false, true, false),
    NATIVE(0x100, "native", false, true, false),
    INTERFACE(0x200, "interface", true, false, false),
    ABSTRACT(0x400, "abstract", true, true, false),
    STRICTFP(0x800, "strictfp", false, true, false),
    SYNTHETIC(0x1000, "synthetic", true, true, true),
    ANNOTATION(0x2000, "annotation", true, false, false),
    ENUM(0x4000, "enum", true, false, true),
    CONSTRUCTOR(0x10000, "constructor", false, true, false),
    DECLARED_SYNCHRONIZED(0x20000, "declared-synchronized", false, true, false);

    fun isSet(accessFlags: Int): Boolean {
        return (value and accessFlags) != 0
    }

    override fun toString(): String {
        return accessFlagName
    }

    companion object {
        //cache the array of all AccessFlags, because .values() allocates a new array for every call
        private val allFlags: Array<AccessFlags> = values()

        private val accessFlagsByName: MutableMap<String, AccessFlags> = HashMap()

        init {
            for (accessFlag in allFlags) {
                accessFlagsByName[accessFlag.accessFlagName] = accessFlag
            }
        }

        @JvmStatic
        fun getAccessFlagsForClass(accessFlagValue: Int): Array<AccessFlags> {
            var size = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForClass && (accessFlagValue and accessFlag.value) != 0) {
                    size++
                }
            }

            val accessFlags = arrayOfNulls<AccessFlags>(size)
            var accessFlagsPosition = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForClass && (accessFlagValue and accessFlag.value) != 0) {
                    accessFlags[accessFlagsPosition++] = accessFlag
                }
            }
            return accessFlags.requireNoNulls()
        }

        private fun formatAccessFlags(accessFlags: Array<AccessFlags>): String {
            var size = 0
            for (accessFlag in accessFlags) {
                size += accessFlag.toString().length + 1
            }

            val sb = StringBuilder(size)
            for (accessFlag in accessFlags) {
                sb.append(accessFlag.toString())
                sb.append(" ")
            }
            if (accessFlags.isNotEmpty()) {
                sb.delete(sb.length - 1, sb.length)
            }
            return sb.toString()
        }

        @JvmStatic
        fun formatAccessFlagsForClass(accessFlagValue: Int): String {
            return formatAccessFlags(getAccessFlagsForClass(accessFlagValue))
        }

        @JvmStatic
        fun getAccessFlagsForMethod(accessFlagValue: Int): Array<AccessFlags> {
            var size = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForMethod && (accessFlagValue and accessFlag.value) != 0) {
                    size++
                }
            }

            val accessFlags = arrayOfNulls<AccessFlags>(size)
            var accessFlagsPosition = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForMethod && (accessFlagValue and accessFlag.value) != 0) {
                    accessFlags[accessFlagsPosition++] = accessFlag
                }
            }
            return accessFlags.requireNoNulls()
        }

        @JvmStatic
        fun formatAccessFlagsForMethod(accessFlagValue: Int): String {
            return formatAccessFlags(getAccessFlagsForMethod(accessFlagValue))
        }

        @JvmStatic
        fun getAccessFlagsForField(accessFlagValue: Int): Array<AccessFlags> {
            var size = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForField && (accessFlagValue and accessFlag.value) != 0) {
                    size++
                }
            }

            val accessFlags = arrayOfNulls<AccessFlags>(size)
            var accessFlagsPosition = 0
            for (accessFlag in allFlags) {
                if (accessFlag.validForField && (accessFlagValue and accessFlag.value) != 0) {
                    accessFlags[accessFlagsPosition++] = accessFlag
                }
            }
            return accessFlags.requireNoNulls()
        }

        @JvmStatic
        fun formatAccessFlagsForField(accessFlagValue: Int): String {
            return formatAccessFlags(getAccessFlagsForField(accessFlagValue))
        }

        @JvmStatic
        fun getAccessFlag(accessFlag: String): AccessFlags? {
            return accessFlagsByName[accessFlag]
        }
    }
}
