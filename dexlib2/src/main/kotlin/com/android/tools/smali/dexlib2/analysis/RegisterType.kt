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

import com.android.tools.smali.util.ExceptionWithContext
import java.io.IOException
import java.io.Writer

class RegisterType private constructor(
    val category: Byte,
    val type: TypeProto?
) {
    init {
        assert(
            ((category == REFERENCE || category == UNINIT_REF || category == UNINIT_THIS) && type != null) ||
                    ((category != REFERENCE && category != UNINIT_REF && category != UNINIT_THIS) && type == null)
        )
    }

    override fun toString(): String {
        return "(" + CATEGORY_NAMES[category.toInt()] + (if (type == null) "" else "," + type) + ")"
    }

    @Throws(IOException::class)
    fun writeTo(writer: Writer) {
        writer.write('('.code)
        writer.write(CATEGORY_NAMES[category.toInt()])
        if (type != null) {
            writer.write(','.code)
            writer.write(type.type)
        }
        writer.write(')'.code)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as RegisterType

        if (category != that.category) {
            return false
        }

        // These require strict reference equality. Every instance represents a unique
        // reference that can't be merged with a different one, even if they have the same type.
        if (category == UNINIT_REF || category == UNINIT_THIS) {
            return false
        }
        return type == that.type
    }

    override fun hashCode(): Int {
        var result = category.toInt()
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    fun merge(other: RegisterType): RegisterType {
        if (other == this) {
            return this
        }

        val mergedCategory = mergeTable[category.toInt()][other.category.toInt()]

        var mergedType: TypeProto? = null
        if (mergedCategory == REFERENCE) {
            val type = this.type
            if (type != null) {
                if (other.type != null) {
                    mergedType = type.getCommonSuperclass(other.type)
                } else {
                    mergedType = type
                }
            } else {
                mergedType = other.type
            }
        } else if (mergedCategory == UNINIT_REF || mergedCategory == UNINIT_THIS) {
            if (this.category == UNKNOWN) {
                return other
            }
            assert(other.category == UNKNOWN)
            return this
        }

        if (mergedType != null) {
            if (mergedType == this.type) {
                return this
            }
            if (mergedType == other.type) {
                return other
            }
        }
        return getRegisterType(mergedCategory, mergedType)
    }

    companion object {
        // The Unknown category denotes a register type that hasn't been determined yet
        const val UNKNOWN: Byte = 0
        // The Uninit category is for registers that haven't been set yet. e.g. the non-parameter registers in a method
        // start out as unint
        const val UNINIT: Byte = 1
        const val NULL: Byte = 2
        const val ONE: Byte = 3
        const val BOOLEAN: Byte = 4
        const val BYTE: Byte = 5
        const val POS_BYTE: Byte = 6
        const val SHORT: Byte = 7
        const val POS_SHORT: Byte = 8
        const val CHAR: Byte = 9
        const val INTEGER: Byte = 10
        const val FLOAT: Byte = 11
        const val LONG_LO: Byte = 12
        const val LONG_HI: Byte = 13
        const val DOUBLE_LO: Byte = 14
        const val DOUBLE_HI: Byte = 15
        // The UninitRef category is used after a new-instance operation, and before the corresponding <init> is called
        const val UNINIT_REF: Byte = 16
        // The UninitThis category is used the "this" register inside an <init> method, before the superclass' <init>
        // method is called
        const val UNINIT_THIS: Byte = 17
        const val REFERENCE: Byte = 18
        // This is used when there are multiple incoming execution paths that have incompatible register types. For
        // example if the register's type is an Integer on one incoming code path, but is a Reference type on another
        // incomming code path. There is no register type that can hold either an Integer or a Reference.
        const val CONFLICTED: Byte = 19

        val CATEGORY_NAMES: Array<String> = arrayOf(
            "Unknown",
            "Uninit",
            "Null",
            "One",
            "Boolean",
            "Byte",
            "PosByte",
            "Short",
            "PosShort",
            "Char",
            "Integer",
            "Float",
            "LongLo",
            "LongHi",
            "DoubleLo",
            "DoubleHi",
            "UninitRef",
            "UninitThis",
            "Reference",
            "Conflicted"
        )

        // this table is used when merging register types. For example, if a particular register can be either a BYTE
        // or a Char, then the "merged" type of that register would be Integer, because it is the "smallest" type can
        // could hold either type of value.
        private val mergeTable: Array<ByteArray> = arrayOf(
            byteArrayOf(UNKNOWN, UNINIT, NULL, ONE, BOOLEAN, BYTE, POS_BYTE, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, LONG_LO, LONG_HI, DOUBLE_LO, DOUBLE_HI, UNINIT_REF, UNINIT_THIS, REFERENCE, CONFLICTED),
            byteArrayOf(UNINIT, UNINIT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(NULL, CONFLICTED, NULL, BOOLEAN, BOOLEAN, BYTE, POS_BYTE, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, REFERENCE, CONFLICTED),
            byteArrayOf(ONE, CONFLICTED, BOOLEAN, ONE, BOOLEAN, BYTE, POS_BYTE, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(BOOLEAN, CONFLICTED, BOOLEAN, BOOLEAN, BOOLEAN, BYTE, POS_BYTE, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(BYTE, CONFLICTED, BYTE, BYTE, BYTE, BYTE, BYTE, SHORT, SHORT, INTEGER, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(POS_BYTE, CONFLICTED, POS_BYTE, POS_BYTE, POS_BYTE, BYTE, POS_BYTE, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(SHORT, CONFLICTED, SHORT, SHORT, SHORT, SHORT, SHORT, SHORT, SHORT, INTEGER, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(POS_SHORT, CONFLICTED, POS_SHORT, POS_SHORT, POS_SHORT, SHORT, POS_SHORT, SHORT, POS_SHORT, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(CHAR, CONFLICTED, CHAR, CHAR, CHAR, INTEGER, CHAR, INTEGER, CHAR, CHAR, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(INTEGER, CONFLICTED, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(FLOAT, CONFLICTED, FLOAT, FLOAT, FLOAT, FLOAT, FLOAT, FLOAT, FLOAT, FLOAT, INTEGER, FLOAT, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(LONG_LO, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_LO, CONFLICTED, LONG_LO, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(LONG_HI, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_HI, CONFLICTED, LONG_HI, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(DOUBLE_LO, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_LO, CONFLICTED, DOUBLE_LO, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(DOUBLE_HI, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_HI, CONFLICTED, DOUBLE_HI, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(UNINIT_REF, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
            byteArrayOf(UNINIT_THIS, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, UNINIT_THIS, CONFLICTED, CONFLICTED),
            byteArrayOf(REFERENCE, CONFLICTED, REFERENCE, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, REFERENCE, CONFLICTED),
            byteArrayOf(CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED),
        )

        val UNKNOWN_TYPE: RegisterType = RegisterType(UNKNOWN, null)

        val UNINIT_TYPE: RegisterType = RegisterType(UNINIT, null)

        val NULL_TYPE: RegisterType = RegisterType(NULL, null)

        val ONE_TYPE: RegisterType = RegisterType(ONE, null)

        val BOOLEAN_TYPE: RegisterType = RegisterType(BOOLEAN, null)

        val BYTE_TYPE: RegisterType = RegisterType(BYTE, null)

        val POS_BYTE_TYPE: RegisterType = RegisterType(POS_BYTE, null)

        val SHORT_TYPE: RegisterType = RegisterType(SHORT, null)

        val POS_SHORT_TYPE: RegisterType = RegisterType(POS_SHORT, null)

        val CHAR_TYPE: RegisterType = RegisterType(CHAR, null)

        val INTEGER_TYPE: RegisterType = RegisterType(INTEGER, null)

        val FLOAT_TYPE: RegisterType = RegisterType(FLOAT, null)

        val LONG_LO_TYPE: RegisterType = RegisterType(LONG_LO, null)

        val LONG_HI_TYPE: RegisterType = RegisterType(LONG_HI, null)

        val DOUBLE_LO_TYPE: RegisterType = RegisterType(DOUBLE_LO, null)

        val DOUBLE_HI_TYPE: RegisterType = RegisterType(DOUBLE_HI, null)

        val CONFLICTED_TYPE: RegisterType = RegisterType(CONFLICTED, null)

        fun getWideRegisterType(type: CharSequence, firstRegister: Boolean): RegisterType {
            when (type[0]) {
                'J' -> {
                    if (firstRegister) {
                        return getRegisterType(LONG_LO, null)
                    } else {
                        return getRegisterType(LONG_HI, null)
                    }
                }
                'D' -> {
                    if (firstRegister) {
                        return getRegisterType(DOUBLE_LO, null)
                    } else {
                        return getRegisterType(DOUBLE_HI, null)
                    }
                }
                else -> throw ExceptionWithContext(
                    "Cannot use this method for narrow register type: %s", type
                )
            }
        }

        fun getRegisterType(classPath: ClassPath, type: CharSequence): RegisterType {
            when (type[0]) {
                'Z' -> return BOOLEAN_TYPE
                'B' -> return BYTE_TYPE
                'S' -> return SHORT_TYPE
                'C' -> return CHAR_TYPE
                'I' -> return INTEGER_TYPE
                'F' -> return FLOAT_TYPE
                'J' -> return LONG_LO_TYPE
                'D' -> return DOUBLE_LO_TYPE
                'L', '[' -> return getRegisterType(REFERENCE, classPath.getClass(type))
                else -> throw AnalysisException("Invalid type: " + type)
            }
        }

        fun getRegisterTypeForLiteral(literalValue: Int): RegisterType {
            if (literalValue < -32768) {
                return INTEGER_TYPE
            }
            if (literalValue < -128) {
                return SHORT_TYPE
            }
            if (literalValue < 0) {
                return BYTE_TYPE
            }
            if (literalValue == 0) {
                return NULL_TYPE
            }
            if (literalValue == 1) {
                return ONE_TYPE
            }
            if (literalValue < 128) {
                return POS_BYTE_TYPE
            }
            if (literalValue < 32768) {
                return POS_SHORT_TYPE
            }
            if (literalValue < 65536) {
                return CHAR_TYPE
            }
            return INTEGER_TYPE
        }

        fun getRegisterType(category: Byte, typeProto: TypeProto?): RegisterType {
            when (category) {
                UNKNOWN -> return UNKNOWN_TYPE
                UNINIT -> return UNINIT_TYPE
                NULL -> return NULL_TYPE
                ONE -> return ONE_TYPE
                BOOLEAN -> return BOOLEAN_TYPE
                BYTE -> return BYTE_TYPE
                POS_BYTE -> return POS_BYTE_TYPE
                SHORT -> return SHORT_TYPE
                POS_SHORT -> return POS_SHORT_TYPE
                CHAR -> return CHAR_TYPE
                INTEGER -> return INTEGER_TYPE
                FLOAT -> return FLOAT_TYPE
                LONG_LO -> return LONG_LO_TYPE
                LONG_HI -> return LONG_HI_TYPE
                DOUBLE_LO -> return DOUBLE_LO_TYPE
                DOUBLE_HI -> return DOUBLE_HI_TYPE
                CONFLICTED -> return CONFLICTED_TYPE
            }

            return RegisterType(category, typeProto)
        }
    }
}
