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

package com.android.tools.smali.dexlib2.dexbacked.raw

import com.android.tools.smali.dexlib2.VersionMap
import com.android.tools.smali.dexlib2.dexbacked.CDexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBuffer
import com.android.tools.smali.dexlib2.dexbacked.raw.util.DexAnnotator
import com.android.tools.smali.dexlib2.util.AnnotatedBytes
import com.android.tools.smali.util.StringUtils

class HeaderItem(private var dexFile: DexBackedDexFile) {
    val checksum: Int
        get() = dexFile.buffer.readSmallUint(CHECKSUM_OFFSET)

    val signature: ByteArray
        get() = dexFile.buffer.readByteRange(SIGNATURE_OFFSET, SIGNATURE_SIZE)

    val mapOffset: Int
        get() = dexFile.buffer.readSmallUint(MAP_OFFSET)

    val headerSize: Int
        get() = dexFile.buffer.readSmallUint(HEADER_SIZE_OFFSET)

    val stringCount: Int
        get() = dexFile.buffer.readSmallUint(STRING_COUNT_OFFSET)

    val stringOffset: Int
        get() = dexFile.buffer.readSmallUint(STRING_START_OFFSET)

    val typeCount: Int
        get() = dexFile.buffer.readSmallUint(TYPE_COUNT_OFFSET)

    val typeOffset: Int
        get() = dexFile.buffer.readSmallUint(TYPE_START_OFFSET)

    val protoCount: Int
        get() = dexFile.buffer.readSmallUint(PROTO_COUNT_OFFSET)

    val protoOffset: Int
        get() = dexFile.buffer.readSmallUint(PROTO_START_OFFSET)

    val fieldCount: Int
        get() = dexFile.buffer.readSmallUint(FIELD_COUNT_OFFSET)

    val fieldOffset: Int
        get() = dexFile.buffer.readSmallUint(FIELD_START_OFFSET)

    val methodCount: Int
        get() = dexFile.buffer.readSmallUint(METHOD_COUNT_OFFSET)

    val methodOffset: Int
        get() = dexFile.buffer.readSmallUint(METHOD_START_OFFSET)

    val classCount: Int
        get() = dexFile.buffer.readSmallUint(CLASS_COUNT_OFFSET)

    val classOffset: Int
        get() = dexFile.buffer.readSmallUint(CLASS_START_OFFSET)

    companion object {
        const val ITEM_SIZE = 0x70
        const val MAGIC_SIZE = 8

        private val MAGIC_VALUE = byteArrayOf(0x64, 0x65, 0x78, 0x0a, 0, 0, 0, 0)

        const val LITTLE_ENDIAN_TAG = 0x12345678
        const val BIG_ENDIAN_TAG = 0x78563412

        const val CHECKSUM_OFFSET = 8

        // this is the start of the checksumed data
        const val CHECKSUM_DATA_START_OFFSET = 12
        const val SIGNATURE_OFFSET = 12
        const val SIGNATURE_SIZE = 20

        // this is the start of the sha-1 hashed data
        const val SIGNATURE_DATA_START_OFFSET = 32
        const val FILE_SIZE_OFFSET = 32

        const val HEADER_SIZE_OFFSET = 36

        const val ENDIAN_TAG_OFFSET = 40

        const val MAP_OFFSET = 52

        const val STRING_COUNT_OFFSET = 56
        const val STRING_START_OFFSET = 60

        const val TYPE_COUNT_OFFSET = 64
        const val TYPE_START_OFFSET = 68

        const val PROTO_COUNT_OFFSET = 72
        const val PROTO_START_OFFSET = 76

        const val FIELD_COUNT_OFFSET = 80
        const val FIELD_START_OFFSET = 84

        const val METHOD_COUNT_OFFSET = 88
        const val METHOD_START_OFFSET = 92

        const val CLASS_COUNT_OFFSET = 96
        const val CLASS_START_OFFSET = 100

        const val DATA_SIZE_OFFSET = 104
        const val DATA_START_OFFSET = 108

        const val CONTAINER_SIZE_OFFSET = 112
        const val HEADER_OFFSET_OFFSET = 116

        fun makeAnnotator(annotator: DexAnnotator, mapItem: MapItem): SectionAnnotator {
            return object : SectionAnnotator(annotator, mapItem) {
                override val itemName: String get() {
                    return "header_item"
                }

                override fun annotateItem(
                    out: AnnotatedBytes,
                    itemIndex: Int,
                    itemIdentity: String?
                ) {
                    val startOffset = out.cursor
                    val headerSize: Int

                    val magic = buildString {
                        for (i in 0 until 8) {
                            append(
                                dexFile.buffer.readUbyte(startOffset + i).toChar()
                            )
                        }
                    }

                    out.annotate(
                        8, "magic: %s", StringUtils.escapeString(magic)
                    )
                    out.annotate(4, "checksum")
                    out.annotate(20, "signature")
                    out.annotate(4, "file_size: %d", dexFile.buffer.readInt(out.cursor))

                    headerSize = dexFile.buffer.readInt(out.cursor)
                    out.annotate(4, "header_size: %d", headerSize)

                    val endianTag = dexFile.buffer.readInt(out.cursor)
                    out.annotate(
                        4, "endian_tag: 0x%x (%s)", endianTag, getEndianText(endianTag)
                    )

                    out.annotate(4, "link_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "link_offset: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "map_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "string_ids_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "string_ids_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "type_ids_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "type_ids_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "proto_ids_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "proto_ids_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "field_ids_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "field_ids_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "method_ids_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "method_ids_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "class_defs_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "class_defs_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    out.annotate(4, "data_size: %d", dexFile.buffer.readInt(out.cursor))
                    out.annotate(4, "data_off: 0x%x", dexFile.buffer.readInt(out.cursor))

                    if (annotator.dexFile is CDexBackedDexFile) {
                        CdexHeaderItem.annotateCdexHeaderFields(out, dexFile.buffer)
                    }

                    if (headerSize > ITEM_SIZE) {
                        out.annotateTo(headerSize, "header padding")
                    }
                }
            }
        }

        private fun getEndianText(endianTag: Int): String {
            if (endianTag == LITTLE_ENDIAN_TAG) {
                return "Little Endian"
            }
            if (endianTag == BIG_ENDIAN_TAG) {
                return "Big Endian"
            }
            return "Invalid"
        }

        /**
         * Get the highest magic number supported by Android for this api level.
         * @return The dex file magic number
         */
        fun getMagicForApi(api: Int): ByteArray {
            return getMagicForDexVersion(VersionMap.mapApiToDexVersion(api))
        }

        fun getMagicForDexVersion(dexVersion: Int): ByteArray {
            val magic = MAGIC_VALUE.clone()

            var version = dexVersion
            if (version < 0 || version > 999) {
                throw IllegalArgumentException("dexVersion must be within [0, 999]")
            }

            for (i in 6 downTo 4) {
                val digit = version % 10
                magic[i] = ('0'.code + digit).toByte()
                version /= 10
            }

            return magic
        }

        /**
         * Verifies the magic value at the beginning of a dex file
         *
         * @param buf A byte array containing at least the first 8 bytes of a dex file
         * @param offset The offset within the buffer to the beginning of the dex header
         * @return True if the magic value is valid
         */
        fun verifyMagic(buf: ByteArray, offset: Int): Boolean {
            if (offset < 0 || offset > buf.size || buf.size - offset < MAGIC_SIZE) {
                return false
            }

            for (i in 0 until 4) {
                if (buf[offset + i] != MAGIC_VALUE[i]) {
                    return false
                }
            }
            for (i in 4 until 7) {
                if (buf[offset + i] < '0'.code ||
                    buf[offset + i] > '9'.code
                ) {
                    return false
                }
            }
            if (buf[offset + 7] != MAGIC_VALUE[7]) {
                return false
            }

            return true
        }

        /**
         * Gets the dex version from a dex header
         *
         * @param buf A byte array containing at least the first 7 bytes of a dex file
         * @param offset The offset within the buffer to the beginning of the dex header
         * @return The dex version if the header is valid or -1 if the header is invalid
         */
        fun getVersion(buf: ByteArray, offset: Int): Int {
            if (offset < 0 || offset > buf.size || buf.size - offset < MAGIC_SIZE) {
                throw DexBackedDexFile.NotADexFile("File is too short")
            }
            if (!verifyMagic(buf, offset)) {
                return -1
            }

            return getVersionUnchecked(buf, offset)
        }

        private fun getVersionUnchecked(buf: ByteArray, offset: Int): Int {
            var version = (buf[offset + 4] - '0'.code) * 100
            version += (buf[offset + 5] - '0'.code) * 10
            version += buf[offset + 6] - '0'.code

            return version
        }

        fun isSupportedDexVersion(version: Int): Boolean {
            return VersionMap.mapDexVersionToApi(version) != VersionMap.NO_VERSION
        }

        fun getEndian(buf: ByteArray, offset: Int): Int {
            val bdb = DexBuffer(buf)
            return bdb.readInt(offset + ENDIAN_TAG_OFFSET)
        }
    }
}
