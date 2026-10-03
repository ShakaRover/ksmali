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

package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.base.value.BaseAnnotationEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseArrayEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseBooleanEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseEnumEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseFieldEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseNullEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseStringEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableByteEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableCharEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableDoubleEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableFloatEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableLongEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableShortEncodedValue
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.util.ExceptionWithContext

object BuilderEncodedValues {
    interface BuilderEncodedValue : EncodedValue

    class BuilderAnnotationEncodedValue(
        internal val typeReference: BuilderTypeReference,
        override val elements: Set<@JvmWildcard BuilderAnnotationElement>,
    ) : BaseAnnotationEncodedValue(), BuilderEncodedValue {
        override val type: String
            get() = typeReference.type
    }

    class BuilderArrayEncodedValue(internal val elements: List<@JvmWildcard BuilderEncodedValue>) :
        BaseArrayEncodedValue(), BuilderEncodedValue {
        var offset = DexWriter.NO_OFFSET

        override val value: List<@JvmWildcard EncodedValue>
            get() = elements
    }

    fun defaultValueForType(type: String): BuilderEncodedValue {
        return when (type[0]) {
            'Z' -> BuilderBooleanEncodedValue.FALSE_VALUE
            'B' -> BuilderByteEncodedValue(0.toByte())
            'S' -> BuilderShortEncodedValue(0.toShort())
            'C' -> BuilderCharEncodedValue('\u0000')
            'I' -> BuilderIntEncodedValue(0)
            'J' -> BuilderLongEncodedValue(0L)
            'F' -> BuilderFloatEncodedValue(0f)
            'D' -> BuilderDoubleEncodedValue(0.0)
            'L', '[' -> BuilderNullEncodedValue.INSTANCE
            else -> throw ExceptionWithContext("Unrecognized type: %s", type)
        }
    }

    class BuilderBooleanEncodedValue private constructor(override val value: Boolean) :
        BaseBooleanEncodedValue(), BuilderEncodedValue {
        companion object {
            val TRUE_VALUE = BuilderBooleanEncodedValue(true)

            val FALSE_VALUE = BuilderBooleanEncodedValue(false)
        }
    }

    class BuilderByteEncodedValue(value: Byte) : ImmutableByteEncodedValue(value), BuilderEncodedValue

    class BuilderCharEncodedValue(value: Char) : ImmutableCharEncodedValue(value), BuilderEncodedValue

    class BuilderDoubleEncodedValue(value: Double) : ImmutableDoubleEncodedValue(value), BuilderEncodedValue

    class BuilderEnumEncodedValue(internal val enumReference: BuilderFieldReference) : BaseEnumEncodedValue(),
        BuilderEncodedValue {
        override val value: BuilderFieldReference
            get() = enumReference
    }

    class BuilderFieldEncodedValue(internal val fieldReference: BuilderFieldReference) : BaseFieldEncodedValue(),
        BuilderEncodedValue {
        override val value: BuilderFieldReference
            get() = fieldReference
    }

    class BuilderFloatEncodedValue(value: Float) : ImmutableFloatEncodedValue(value), BuilderEncodedValue

    class BuilderIntEncodedValue(value: Int) : ImmutableIntEncodedValue(value), BuilderEncodedValue

    class BuilderLongEncodedValue(value: Long) : ImmutableLongEncodedValue(value), BuilderEncodedValue

    class BuilderMethodEncodedValue(internal val methodReference: BuilderMethodReference) : BaseMethodEncodedValue(),
        BuilderEncodedValue {
        override val value: BuilderMethodReference
            get() = methodReference
    }

    class BuilderNullEncodedValue private constructor() : BaseNullEncodedValue(), BuilderEncodedValue {
        companion object {
            val INSTANCE = BuilderNullEncodedValue()
        }
    }

    class BuilderShortEncodedValue(value: Short) : ImmutableShortEncodedValue(value), BuilderEncodedValue

    class BuilderStringEncodedValue(internal val stringReference: BuilderStringReference) : BaseStringEncodedValue(),
        BuilderEncodedValue {
        override val value: String
            get() = stringReference.string
    }

    class BuilderTypeEncodedValue(internal val typeReference: BuilderTypeReference) : BaseTypeEncodedValue(),
        BuilderEncodedValue {
        override val value: String
            get() = typeReference.type
    }

    class BuilderMethodTypeEncodedValue(internal val methodProtoReference: BuilderMethodProtoReference) :
        BaseMethodTypeEncodedValue(), BuilderEncodedValue {
        override val value: BuilderMethodProtoReference
            get() = methodProtoReference
    }

    class BuilderMethodHandleEncodedValue(internal val methodHandleReference: BuilderMethodHandleReference) :
        BaseMethodHandleEncodedValue(), BuilderEncodedValue {
        override val value: BuilderMethodHandleReference
            get() = methodHandleReference
    }
}
