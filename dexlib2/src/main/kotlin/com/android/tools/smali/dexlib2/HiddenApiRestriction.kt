/*
 * Copyright 2020, Google LLC
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

import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.StringJoiner

enum class HiddenApiRestriction(
    val value: Int,
    private val flagName: String,
    val isDomainSpecificApiFlag: Boolean
) {
    WHITELIST(0, "whitelist", false),
    GREYLIST(1, "greylist", false),
    BLACKLIST(2, "blacklist", false),
    GREYLIST_MAX_O(3, "greylist-max-o", false),
    GREYLIST_MAX_P(4, "greylist-max-p", false),
    GREYLIST_MAX_Q(5, "greylist-max-q", false),
    GREYLIST_MAX_R(6, "greylist-max-r", false),
    CORE_PLATFORM_API(8, "core-platform-api", true),
    TEST_API(16, "test-api", true);

    override fun toString(): String {
        return flagName
    }

    fun isSet(value: Int): Boolean {
        return if (isDomainSpecificApiFlag) {
            (value and this.value) != 0
        } else {
            (value and HIDDENAPI_FLAG_MASK) == this.value
        }
    }

    companion object {
        private const val HIDDENAPI_FLAG_MASK = 0x7

        private val hiddenApiFlags = arrayOf(
            WHITELIST,
            GREYLIST,
            BLACKLIST,
            GREYLIST_MAX_O,
            GREYLIST_MAX_P,
            GREYLIST_MAX_Q,
            GREYLIST_MAX_R
        )

        private val domainSpecificApiFlags = arrayOf(
            CORE_PLATFORM_API,
            TEST_API
        )

        private val hiddenApiRestrictionsByName: MutableMap<String, HiddenApiRestriction> = HashMap()

        init {
            for (hiddenApiRestriction in values()) {
                hiddenApiRestrictionsByName[hiddenApiRestriction.toString()] = hiddenApiRestriction
            }
        }

        fun getAllFlags(value: Int): Set<HiddenApiRestriction> {
            val normalRestriction = hiddenApiFlags[value and HIDDENAPI_FLAG_MASK]
            val restrictionSet = HashSet<HiddenApiRestriction>()

            val domainSpecificPart = value and HIDDENAPI_FLAG_MASK.inv()
            if (domainSpecificPart == 0) {
                restrictionSet.add(normalRestriction)
                return Collections.unmodifiableSet(restrictionSet)
            }
            restrictionSet.add(normalRestriction)
            for (domainSpecificApiFlag in domainSpecificApiFlags) {
                if (domainSpecificApiFlag.isSet(value)) {
                    restrictionSet.add(domainSpecificApiFlag)
                }
            }
            return Collections.unmodifiableSet(restrictionSet)
        }

        fun formatHiddenRestrictions(value: Int): String {
            val joiner = StringJoiner("|")
            for (hiddenApiRestriction in getAllFlags(value)) {
                joiner.add(hiddenApiRestriction.toString())
            }
            return joiner.toString()
        }

        fun combineFlags(flags: Iterable<HiddenApiRestriction>): Int {
            var gotHiddenApiFlag = false

            var value = 0

            for (flag in flags) {
                if (flag.isDomainSpecificApiFlag) {
                    value += flag.value
                } else {
                    if (gotHiddenApiFlag) {
                        throw IllegalArgumentException(
                            "Cannot combine multiple flags for hidden api restrictions"
                        )
                    }
                    gotHiddenApiFlag = true
                    value += flag.value
                }
            }

            return value
        }

        @JvmStatic
        fun forName(name: String): HiddenApiRestriction? {
            return hiddenApiRestrictionsByName[name]
        }
    }
}
