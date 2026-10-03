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

package com.android.tools.smali.dexlib2.iface.value

/**
 * This class represents an encoded byte value.
 */
interface ByteEncodedValue : EncodedValue {
    /**
     * Gets the byte value.
     *
     * @return the byte value
     */
    val value: Byte

    /**
     * Returns a hashcode for this EncodedByteValue.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getValue();
     * }</pre>
     *
     * @return The hash code value for this EncodedByteValue
     */
    override fun hashCode(): Int

    /**
     * Compares this ByteEncodedValue to another ByteEncodedValue for equality.
     *
     * This ByteEncodedValue is equal to another ByteEncodedValue if the values returned by getValue() are equal.
     *
     * @param other The object to be compared for equality with this ByteEncodedValue
     * @return true if the specified object is equal to this ByteEncodedValue
     */
    override fun equals(other: Any?): Boolean

    /**
     * Compare this ByteEncodedValue to another EncodedValue.
     *
     * The comparison is first done on the return values of getValueType(). If the other value is another
     * ByteEncodedValue, the return values of getValue() are compared.
     *
     * @param other The EncodedValue to compare with this ByteEncodedValue
     * @return An integer representing the result of the comparison
     */
    override fun compareTo(other: EncodedValue): Int
}
