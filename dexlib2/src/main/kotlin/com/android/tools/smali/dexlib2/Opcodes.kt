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

package com.android.tools.smali.dexlib2

import com.android.tools.smali.util.UnmodifiableRangeMap
import java.util.EnumMap
import java.util.HashMap

class Opcodes private constructor(api: Int, artVersion: Int) {

    /**
     * Either the api level for dalvik opcodes, or the art version for art opcodes
     */
    @JvmField
    val api: Int

    val artVersion: Int

    private val opcodesByValue: Array<Opcode?> = arrayOfNulls(256)
    private val opcodeValues: EnumMap<Opcode, Short> = EnumMap(Opcode::class.java)
    private val opcodesByName: MutableMap<String, Opcode> = HashMap()

    init {
        if (api >= 21) {
            this.api = api
            this.artVersion = VersionMap.mapApiToArtVersion(api)
        } else if (artVersion >= 0 && artVersion < 39) {
            this.api = VersionMap.mapArtVersionToApi(artVersion)
            this.artVersion = artVersion
        } else {
            this.api = api
            this.artVersion = artVersion
        }

        val version: Int
        if (isArt) {
            version = this.artVersion
        } else {
            version = this.api
        }

        for (opcode in Opcode.values()) {
            val versionToValueMap: UnmodifiableRangeMap<Int, Short> =
                if (isArt) opcode.artVersionToValueMap else opcode.apiToValueMap

            val opcodeValue = versionToValueMap.get(version)
            if (opcodeValue != null) {
                if (!opcode.format.isPayloadFormat) {
                    opcodesByValue[opcodeValue.toInt()] = opcode
                }
                opcodeValues[opcode] = opcodeValue
                opcodesByName[opcode.mnemonic.lowercase()] = opcode
            }
        }
    }

    fun getOpcodeByName(opcodeName: String): Opcode? {
        return opcodesByName[opcodeName.lowercase()]
    }

    fun getOpcodeByValue(opcodeValue: Int): Opcode? {
        return when (opcodeValue) {
            0x100 -> Opcode.PACKED_SWITCH_PAYLOAD
            0x200 -> Opcode.SPARSE_SWITCH_PAYLOAD
            0x300 -> Opcode.ARRAY_PAYLOAD
            else -> {
                if (opcodeValue >= 0 && opcodeValue < opcodesByValue.size) {
                    opcodesByValue[opcodeValue]
                } else {
                    null
                }
            }
        }
    }

    fun getOpcodeValue(opcode: Opcode): Short? {
        return opcodeValues[opcode]
    }

    val isArt: Boolean get() {
        return artVersion != VersionMap.NO_VERSION
    }

    companion object {
        @JvmStatic
        fun forApi(api: Int): Opcodes {
            return Opcodes(api, VersionMap.NO_VERSION)
        }

        fun forArtVersion(artVersion: Int): Opcodes {
            return Opcodes(VersionMap.NO_VERSION, artVersion)
        }

        fun forDexVersion(dexVersion: Int): Opcodes {
            val api = VersionMap.mapDexVersionToApi(dexVersion)
            if (api == VersionMap.NO_VERSION) {
                throw RuntimeException("Unsupported dex version $dexVersion")
            }
            return Opcodes(api, VersionMap.NO_VERSION)
        }

        /**
         * @return a default Opcodes instance for when the exact Opcodes to use doesn't matter or isn't known
         */
        val default: Opcodes get() {
            // The last pre-art api
            return forApi(20)
        }
    }
}
