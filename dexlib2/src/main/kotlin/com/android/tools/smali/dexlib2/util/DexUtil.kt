/*
 * Copyright 2016, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google LLC nor the names of its
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

package com.android.tools.smali.dexlib2.util

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile.NotADexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile.NotAnOdexFile
import com.android.tools.smali.dexlib2.dexbacked.raw.CdexHeaderItem
import com.android.tools.smali.dexlib2.dexbacked.raw.HeaderItem
import com.android.tools.smali.dexlib2.dexbacked.raw.OdexHeaderItem
import com.android.tools.smali.util.InputStreamUtil
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

object DexUtil {

    /**
     * Reads in the dex header from the given input stream and verifies that it is valid and a supported version
     *
     * The inputStream must support mark(), and will be reset to initial position upon exiting the method
     *
     * @param inputStream An input stream that is positioned at a dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a dex file
     * @throws InvalidFile If the header appears to be a dex file, but is not valid for some reason
     * @throws UnsupportedFile If the dex header is valid, but uses unsupported functionality
     */
    @JvmStatic
    @Throws(IOException::class)
    fun verifyDexHeader(inputStream: InputStream): Int {
        if (!inputStream.markSupported()) {
            throw IllegalArgumentException("InputStream must support mark")
        }
        inputStream.mark(44)
        val partialHeader = ByteArray(44)
        try {
            InputStreamUtil.readFully(inputStream, partialHeader)
        } catch (ex: EOFException) {
            throw NotADexFile("File is too short")
        } finally {
            inputStream.reset()
        }

        return verifyDexHeader(partialHeader, 0)
    }

    /**
     * Verifies that the dex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 44 bytes of a dex file
     * @param offset The offset within the array to the dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a dex file
     * @throws InvalidFile If the header appears to be a dex file, but is not valid for some reason
     * @throws UnsupportedFile If the dex header is valid, but uses unsupported functionality
     */
    @JvmStatic
    fun verifyDexHeader(buf: ByteArray, offset: Int): Int {
        if (offset < 0 || offset > buf.size || buf.size - offset < HeaderItem.MAGIC_SIZE) {
            throw NotADexFile("File is too short")
        }
        val dexVersion = HeaderItem.getVersion(buf, offset)
        if (dexVersion == -1) {
            val sb = StringBuilder("Not a valid dex magic value:")
            for (i in 0 until HeaderItem.MAGIC_SIZE) {
                sb.append(String.format(" %02x", buf[offset + i]))
            }
            throw NotADexFile(sb.toString())
        }

        if (!HeaderItem.isSupportedDexVersion(dexVersion)) {
            throw UnsupportedFile(
                String.format("Dex version %03d is not supported", dexVersion)
            )
        }

        if (buf.size - offset < 44) {
            throw NotADexFile("File is too short")
        }

        val endian = HeaderItem.getEndian(buf, offset)
        if (endian == HeaderItem.BIG_ENDIAN_TAG) {
            throw UnsupportedFile("Big endian dex files are not supported")
        }

        if (endian != HeaderItem.LITTLE_ENDIAN_TAG) {
            throw InvalidFile(String.format("Invalid endian tag: 0x%x", endian))
        }

        return dexVersion
    }

    /**
     * Verifies that the cdex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 44 bytes of a cdex file
     * @param offset The offset within the array to the dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a cdex file
     * @throws InvalidFile If the header appears to be a cdex file, but is not valid for some reason
     * @throws UnsupportedFile If the cdex header is valid, but uses unsupported functionality
     */
    @JvmStatic
    fun verifyCdexHeader(buf: ByteArray, offset: Int): Int {
        val cdexVersion = CdexHeaderItem.getVersion(buf, offset)
        if (cdexVersion == -1) {
            val sb = StringBuilder("Not a valid cdex magic value:")
            for (i in 0 until 8) {
                sb.append(String.format(" %02x", buf[offset + i]))
            }
            throw NotADexFile(sb.toString())
        }

        if (!CdexHeaderItem.isSupportedCdexVersion(cdexVersion)) {
            throw UnsupportedFile(String.format("Dex version %03d is not supported", cdexVersion))
        }

        val endian = HeaderItem.getEndian(buf, offset)
        if (endian == HeaderItem.BIG_ENDIAN_TAG) {
            throw UnsupportedFile("Big endian dex files are not supported")
        }

        if (endian != HeaderItem.LITTLE_ENDIAN_TAG) {
            throw InvalidFile(String.format("Invalid endian tag: 0x%x", endian))
        }

        return cdexVersion
    }

    /**
     * Reads in the odex header from the given input stream and verifies that it is valid and a supported version
     *
     * The inputStream must support mark(), and will be reset to initial position upon exiting the method
     *
     * @param inputStream An input stream that is positioned at an odex header
     * @throws NotAnOdexFile If the file is not an odex file
     * @throws UnsupportedFile If the odex header is valid, but is an unsupported version
     */
    @JvmStatic
    @Throws(IOException::class)
    fun verifyOdexHeader(inputStream: InputStream) {
        if (!inputStream.markSupported()) {
            throw IllegalArgumentException("InputStream must support mark")
        }
        inputStream.mark(8)
        val partialHeader = ByteArray(8)
        try {
            InputStreamUtil.readFully(inputStream, partialHeader)
        } catch (ex: EOFException) {
            throw NotAnOdexFile("File is too short")
        } finally {
            inputStream.reset()
        }

        verifyOdexHeader(partialHeader, 0)
    }

    /**
     * Verifies that the odex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 8 bytes of an odex file
     * @param offset The offset within the array to the odex header
     * @throws NotAnOdexFile If the file is not an odex file
     * @throws UnsupportedFile If the odex header is valid, but uses unsupported functionality
     */
    @JvmStatic
    fun verifyOdexHeader(buf: ByteArray, offset: Int) {
        val odexVersion = OdexHeaderItem.getVersion(buf, offset)
        if (odexVersion == -1) {
            val sb = StringBuilder("Not a valid odex magic value:")
            for (i in 0 until 8) {
                sb.append(String.format(" %02x", buf[i]))
            }
            throw NotAnOdexFile(sb.toString())
        }

        if (!OdexHeaderItem.isSupportedOdexVersion(odexVersion)) {
            throw UnsupportedFile(String.format("Odex version %03d is not supported", odexVersion))
        }
    }

    class InvalidFile : RuntimeException {
        constructor()

        constructor(message: String) : super(message)

        constructor(message: String, cause: Throwable) : super(message, cause)

        constructor(cause: Throwable) : super(cause)
    }

    class UnsupportedFile : RuntimeException {
        constructor()

        constructor(message: String) : super(message)

        constructor(message: String, cause: Throwable) : super(message, cause)

        constructor(cause: Throwable) : super(cause)
    }
}
