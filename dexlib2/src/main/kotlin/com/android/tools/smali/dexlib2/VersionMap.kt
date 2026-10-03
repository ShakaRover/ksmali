/*
 * Copyright 2015, Google LLC
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

package com.android.tools.smali.dexlib2

object VersionMap {
    const val NO_VERSION = -1

    fun mapDexVersionToApi(dexVersion: Int): Int {
        return when (dexVersion) {
            35 -> 23
            37 -> 25
            38 -> 27
            39 -> 29
            40 -> 34
            41 -> 35
            else -> NO_VERSION
        }
    }

    fun mapApiToDexVersion(api: Int): Int {
        if (api <= 23) {  // Android M/6
            return 35
        }
        return when (api) {
            24, 25 -> 37   // Android N/7 / Android N/7.1
            26, 27 -> 38   // Android O/8 / Android O/8.1
            28, 29 -> 39   // Android P/9 / Android Q/10
            30, 31, 32, 33, 34 -> 40  // Android R..U
            35 -> 41       // Android V/15
            else -> NO_VERSION
        }
    }

    fun mapArtVersionToApi(artVersion: Int): Int {
        if (artVersion >= 244) {
            return 35
        }
        if (artVersion >= 230) {
            return 34
        }
        if (artVersion >= 225) {
            return 33
        }
        if (artVersion >= 199) {
            return 32
        }
        if (artVersion >= 183) {
            return 30
        }
        if (artVersion >= 170) {
            return 29
        }
        if (artVersion >= 138) {
            return 28
        }
        if (artVersion >= 131) {
            return 27
        }
        if (artVersion >= 124) {
            return 26
        }
        if (artVersion >= 79) {
            return 24
        }
        if (artVersion >= 64) {
            return 23
        }
        if (artVersion >= 45) {
            return 22
        }
        if (artVersion >= 39) {
            return 21
        }
        return 19
    }

    fun mapApiToArtVersion(api: Int): Int {
        if (api < 19) {
            return NO_VERSION
        }

        return when (api) {
            19, 20 -> 7
            21 -> 39
            22 -> 45
            23 -> 64
            24, 25 -> 79
            26 -> 124
            27 -> 131
            28 -> 138
            29 -> 170
            30 -> 183
            31, 32 -> 199
            33 -> 225
            34 -> 230
            35 -> 244
            // 254 is the current version in the master branch of AOSP as of 2025-01-19
            else -> 254
        }
    }
}
