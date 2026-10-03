/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:JvmName("FieldDefinition")

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.value.EncodedValue
import com.android.tools.smali.dexlib2.util.EncodedValueUtils
import java.io.IOException

@Throws(IOException::class)
fun writeTo(writer: BaksmaliWriter, field: Field, setInStaticConstructor: Boolean) {
    var initialValue: EncodedValue? = field.initialValue
    val accessFlags = field.accessFlags

    if (setInStaticConstructor &&
        AccessFlags.STATIC.isSet(accessFlags) &&
        AccessFlags.FINAL.isSet(accessFlags) &&
        initialValue != null) {
        if (!EncodedValueUtils.isDefaultValue(initialValue)) {
            writer.write("# The value of this static final field might be set in the static constructor\n")
        } else {
            // don't write out the default initial value for static final fields that get set in the static
            // constructor
            initialValue = null
        }
    }

    writer.write(".field ")
    writeAccessFlagsAndRestrictions(writer, field.accessFlags, field.hiddenApiRestrictions)
    writer.writeSimpleName(field.name)
    writer.write(':')
    writer.writeType(field.type)

    if (initialValue != null) {
        writer.write(" = ")
        writer.writeEncodedValue(initialValue)
    }

    writer.write('\n')

    val annotations = field.annotations
    if (annotations.size > 0) {
        writer.indent(4)

        writeTo(writer, annotations)
        writer.deindent(4)
        writer.write(".end field\n")
    }
}

@Throws(IOException::class)
private fun writeAccessFlagsAndRestrictions(
    writer: BaksmaliWriter, accessFlags: Int, hiddenApiRestrictions: Set<HiddenApiRestriction>
) {
    for (accessFlag in AccessFlags.getAccessFlagsForField(accessFlags)) {
        writer.write(accessFlag.toString())
        writer.write(' ')
    }
    for (hiddenApiRestriction in hiddenApiRestrictions) {
        writer.write(hiddenApiRestriction.toString())
        writer.write(' ')
    }
}
