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

package com.android.tools.smali.dexlib2.dexbacked

import com.android.tools.smali.util.ExceptionWithContext
import org.junit.Assert
import org.junit.Test

class BaseDexReaderLeb128Test {
    @Test
    fun testUleb128() {
        performTest(0x0, byteArrayOf(0x0.toByte(), 0x11.toByte()), 1)
        performTest(0x1, byteArrayOf(0x1.toByte(), 0x11.toByte()), 1)
        performTest(0x3f, byteArrayOf(0x3f.toByte(), 0x11.toByte()), 1)
        performTest(0x40, byteArrayOf(0x40.toByte(), 0x11.toByte()), 1)
        performTest(0x70, byteArrayOf(0x70.toByte(), 0x11.toByte()), 1)
        performTest(0x7f, byteArrayOf(0x7f.toByte(), 0x11.toByte()), 1)

        performTest(0x80, byteArrayOf(0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 2)
        performTest(0x100, byteArrayOf(0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 2)
        performTest(0x800, byteArrayOf(0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 2)
        performTest(0x1f80, byteArrayOf(0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 2)
        performTest(0x2000, byteArrayOf(0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 2)
        performTest(0x2080, byteArrayOf(0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 2)
        performTest(0x3800, byteArrayOf(0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 2)
        performTest(0x3f80, byteArrayOf(0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 2)

        performTest(0xff, byteArrayOf(0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 2)
        performTest(0x17f, byteArrayOf(0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 2)
        performTest(0x87f, byteArrayOf(0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 2)
        performTest(0x1fff, byteArrayOf(0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 2)
        performTest(0x207f, byteArrayOf(0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 2)
        performTest(0x20ff, byteArrayOf(0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 2)
        performTest(0x387f, byteArrayOf(0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 2)
        performTest(0x3fff, byteArrayOf(0xff.toByte(), 0x7f.toByte(), 0x11.toByte()), 2)

        performTest(0x4000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 3)
        performTest(0x8000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 3)
        performTest(0x40000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 3)
        performTest(0xfc000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 3)
        performTest(0x100000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 3)
        performTest(0x104000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 3)
        performTest(0x1c0000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 3)
        performTest(0x1fc000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 3)

        performTest(0x7fff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 3)
        performTest(0xbfff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 3)
        performTest(0x43fff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 3)
        performTest(0xfffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 3)
        performTest(0x103fff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 3)
        performTest(0x107fff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 3)
        performTest(0x1c3fff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 3)
        performTest(0x1fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7f.toByte(), 0x11.toByte()), 3)

        performTest(0x200000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 4)
        performTest(0x400000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 4)
        performTest(0x2000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 4)
        performTest(0x7e00000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 4)
        performTest(0x8000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 4)
        performTest(0x8200000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 4)
        performTest(0xe000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 4)
        performTest(0xfe00000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 4)

        performTest(0x3fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 4)
        performTest(0x5fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 4)
        performTest(0x21fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 4)
        performTest(0x7ffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 4)
        performTest(0x81fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 4)
        performTest(0x83fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 4)
        performTest(0xe1fffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 4)
        performTest(0xfffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(), 0x11.toByte()), 4)

        performTest(0x10000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 5)
        performTest(0x20000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 5)
        performTest(0x70000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x7.toByte(), 0x11.toByte()), 5)
        performTest(0x70000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x17.toByte(), 0x11.toByte()), 5)
        performTest(0x70000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x47.toByte(), 0x11.toByte()), 5)
        performTest(0x70000000, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x77.toByte(), 0x11.toByte()), 5)

        performTest(0x1fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 5)
        performTest(0x2fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 5)
        performTest(0x7fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7.toByte(), 0x11.toByte()), 5)
        performTest(0x7fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x17.toByte(), 0x11.toByte()), 5)
        performTest(0x7fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x47.toByte(), 0x11.toByte()), 5)
        performTest(0x7fffffff, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x77.toByte(), 0x11.toByte()), 5)

        performTest(0xcc, byteArrayOf(0xcc.toByte(), 0x1.toByte()))
        performTest(0x3b67, byteArrayOf(0xe7.toByte(), 0x76.toByte()))
        performTest(0x1b857589, byteArrayOf(0x89.toByte(), 0xeb.toByte(), 0x95.toByte(), 0xdc.toByte(), 0x1.toByte()))
        performTest(0x375d82e5, byteArrayOf(0xe5.toByte(), 0x85.toByte(), 0xf6.toByte(), 0xba.toByte(), 0x3.toByte()))
        performTest(0x5524da90, byteArrayOf(0x90.toByte(), 0xb5.toByte(), 0x93.toByte(), 0xa9.toByte(), 0x5.toByte()))
        performTest(0x35, byteArrayOf(0x35.toByte()))
        performTest(0xd7, byteArrayOf(0xd7.toByte(), 0x1.toByte()))
        performTest(0x63, byteArrayOf(0x63.toByte()))
        performTest(0x22cb5b, byteArrayOf(0xdb.toByte(), 0x96.toByte(), 0x8b.toByte(), 0x1.toByte()))
        performTest(0x585e, byteArrayOf(0xde.toByte(), 0xb0.toByte(), 0x1.toByte()))
        performTest(0x5d62a965, byteArrayOf(0xe5.toByte(), 0xd2.toByte(), 0x8a.toByte(), 0xeb.toByte(), 0x5.toByte()))
        performTest(0x6af172db, byteArrayOf(0xdb.toByte(), 0xe5.toByte(), 0xc5.toByte(), 0xd7.toByte(), 0x6.toByte()))
        performTest(0xe, byteArrayOf(0xe.toByte()))
        performTest(0xb75f7a, byteArrayOf(0xfa.toByte(), 0xbe.toByte(), 0xdd.toByte(), 0x5.toByte()))
        performTest(0x8604, byteArrayOf(0x84.toByte(), 0x8c.toByte(), 0x2.toByte()))
        performTest(0x31624026, byteArrayOf(0xa6.toByte(), 0x80.toByte(), 0x89.toByte(), 0x8b.toByte(), 0x3.toByte()))
        performTest(0x8d, byteArrayOf(0x8d.toByte(), 0x1.toByte()))
        performTest(0xc0, byteArrayOf(0xc0.toByte(), 0x1.toByte()))
        performTest(0xd7618cb, byteArrayOf(0xcb.toByte(), 0xb1.toByte(), 0xd8.toByte(), 0x6b.toByte()))
        performTest(0xff, byteArrayOf(0xff.toByte(), 0x1.toByte()))
        performTest(0x5c923e42, byteArrayOf(0xc2.toByte(), 0xfc.toByte(), 0xc8.toByte(), 0xe4.toByte(), 0x5.toByte()))
        performTest(0x91, byteArrayOf(0x91.toByte(), 0x1.toByte()))
        performTest(0xbe0f97, byteArrayOf(0x97.toByte(), 0x9f.toByte(), 0xf8.toByte(), 0x5.toByte()))
        performTest(0x88bc786, byteArrayOf(0x86.toByte(), 0x8f.toByte(), 0xaf.toByte(), 0x44.toByte()))
        performTest(0x8caa9a, byteArrayOf(0x9a.toByte(), 0xd5.toByte(), 0xb2.toByte(), 0x4.toByte()))
        performTest(0x4aee, byteArrayOf(0xee.toByte(), 0x95.toByte(), 0x1.toByte()))
        performTest(0x438c86, byteArrayOf(0x86.toByte(), 0x99.toByte(), 0x8e.toByte(), 0x2.toByte()))
        performTest(0xc0, byteArrayOf(0xc0.toByte(), 0x1.toByte()))
        performTest(0xb486, byteArrayOf(0x86.toByte(), 0xe9.toByte(), 0x2.toByte()))
        performTest(0x83fd, byteArrayOf(0xfd.toByte(), 0x87.toByte(), 0x2.toByte()))
        performTest(0x7b, byteArrayOf(0x7b.toByte()))
        performTest(0x1dc84e14, byteArrayOf(0x94.toByte(), 0x9c.toByte(), 0xa1.toByte(), 0xee.toByte(), 0x1.toByte()))
        performTest(0x2dfc, byteArrayOf(0xfc.toByte(), 0x5b.toByte()))
        performTest(0x88, byteArrayOf(0x88.toByte(), 0x1.toByte()))
        performTest(0x919e, byteArrayOf(0x9e.toByte(), 0xa3.toByte(), 0x2.toByte()))
        performTest(0x2fcf, byteArrayOf(0xcf.toByte(), 0x5f.toByte()))
        performTest(0xf00674, byteArrayOf(0xf4.toByte(), 0x8c.toByte(), 0xc0.toByte(), 0x7.toByte()))
        performTest(0xed5f7d, byteArrayOf(0xfd.toByte(), 0xbe.toByte(), 0xb5.toByte(), 0x7.toByte()))
        performTest(0xdbd9, byteArrayOf(0xd9.toByte(), 0xb7.toByte(), 0x3.toByte()))
        performTest(0xa1, byteArrayOf(0xa1.toByte(), 0x1.toByte()))
        performTest(0xf6f76c, byteArrayOf(0xec.toByte(), 0xee.toByte(), 0xdb.toByte(), 0x7.toByte()))
        performTest(0x1eed6f, byteArrayOf(0xef.toByte(), 0xda.toByte(), 0x7b.toByte()))
        performTest(0x95c, byteArrayOf(0xdc.toByte(), 0x12.toByte()))
        performTest(0x1e, byteArrayOf(0x1e.toByte()))
        performTest(0xe5, byteArrayOf(0xe5.toByte(), 0x1.toByte()))
        performTest(0x2f2f13, byteArrayOf(0x93.toByte(), 0xde.toByte(), 0xbc.toByte(), 0x1.toByte()))
        performTest(0x19, byteArrayOf(0x19.toByte()))
        performTest(0x3f, byteArrayOf(0x3f.toByte()))
        performTest(0x75e3, byteArrayOf(0xe3.toByte(), 0xeb.toByte(), 0x1.toByte()))
        performTest(0x67a4c4, byteArrayOf(0xc4.toByte(), 0xc9.toByte(), 0x9e.toByte(), 0x3.toByte()))
        performTest(0xb948, byteArrayOf(0xc8.toByte(), 0xf2.toByte(), 0x2.toByte()))
        performTest(0x34b1c9de, byteArrayOf(0xde.toByte(), 0x93.toByte(), 0xc7.toByte(), 0xa5.toByte(), 0x3.toByte()))
        performTest(0x58f0, byteArrayOf(0xf0.toByte(), 0xb1.toByte(), 0x1.toByte()))
        performTest(0x0, byteArrayOf(0x0.toByte()))
        performTest(0x9ab3e5, byteArrayOf(0xe5.toByte(), 0xe7.toByte(), 0xea.toByte(), 0x4.toByte()))
        performTest(0x4c4a8a3d, byteArrayOf(0xbd.toByte(), 0x94.toByte(), 0xaa.toByte(), 0xe2.toByte(), 0x4.toByte()))
        performTest(0x99, byteArrayOf(0x99.toByte(), 0x1.toByte()))
        performTest(0x1a67e9, byteArrayOf(0xe9.toByte(), 0xcf.toByte(), 0x69.toByte()))
        performTest(0x5ddb2d, byteArrayOf(0xad.toByte(), 0xb6.toByte(), 0xf7.toByte(), 0x2.toByte()))
        performTest(0xeccb680, byteArrayOf(0x80.toByte(), 0xed.toByte(), 0xb2.toByte(), 0x76.toByte()))
        performTest(0x6910bbf0, byteArrayOf(0xf0.toByte(), 0xf7.toByte(), 0xc2.toByte(), 0xc8.toByte(), 0x6.toByte()))
        performTest(0xc5, byteArrayOf(0xc5.toByte(), 0x1.toByte()))
        performTest(0xdd7225, byteArrayOf(0xa5.toByte(), 0xe4.toByte(), 0xf5.toByte(), 0x6.toByte()))
        performTest(0x4561ea2e, byteArrayOf(0xae.toByte(), 0xd4.toByte(), 0x87.toByte(), 0xab.toByte(), 0x4.toByte()))
        performTest(0x7f4f08, byteArrayOf(0x88.toByte(), 0x9e.toByte(), 0xfd.toByte(), 0x3.toByte()))
        performTest(0x197f, byteArrayOf(0xff.toByte(), 0x32.toByte()))
        performTest(0xb8ad13, byteArrayOf(0x93.toByte(), 0xda.toByte(), 0xe2.toByte(), 0x5.toByte()))
        performTest(0x3c8d5db4, byteArrayOf(0xb4.toByte(), 0xbb.toByte(), 0xb5.toByte(), 0xe4.toByte(), 0x3.toByte()))
        performTest(0x7e4bdf7d, byteArrayOf(0xfd.toByte(), 0xbe.toByte(), 0xaf.toByte(), 0xf2.toByte(), 0x7.toByte()))
        performTest(0x1e8e23, byteArrayOf(0xa3.toByte(), 0x9c.toByte(), 0x7a.toByte()))
        performTest(0x1602, byteArrayOf(0x82.toByte(), 0x2c.toByte()))
        performTest(0xe2, byteArrayOf(0xe2.toByte(), 0x1.toByte()))
        performTest(0x38e9, byteArrayOf(0xe9.toByte(), 0x71.toByte()))
        performTest(0xbf8665, byteArrayOf(0xe5.toByte(), 0x8c.toByte(), 0xfe.toByte(), 0x5.toByte()))
        performTest(0x43, byteArrayOf(0x43.toByte()))
        performTest(0xc9d96c, byteArrayOf(0xec.toByte(), 0xb2.toByte(), 0xa7.toByte(), 0x6.toByte()))
        performTest(0x4bd170, byteArrayOf(0xf0.toByte(), 0xa2.toByte(), 0xaf.toByte(), 0x2.toByte()))
        performTest(0x86c11b, byteArrayOf(0x9b.toByte(), 0x82.toByte(), 0x9b.toByte(), 0x4.toByte()))
        performTest(0x1a2611e7, byteArrayOf(0xe7.toByte(), 0xa3.toByte(), 0x98.toByte(), 0xd1.toByte(), 0x1.toByte()))
        performTest(0xff2f6a, byteArrayOf(0xea.toByte(), 0xde.toByte(), 0xfc.toByte(), 0x7.toByte()))
        performTest(0x6f051635, byteArrayOf(0xb5.toByte(), 0xac.toByte(), 0x94.toByte(), 0xf8.toByte(), 0x6.toByte()))
        performTest(0x75bf, byteArrayOf(0xbf.toByte(), 0xeb.toByte(), 0x1.toByte()))
        performTest(0xe8ce45, byteArrayOf(0xc5.toByte(), 0x9c.toByte(), 0xa3.toByte(), 0x7.toByte()))
        performTest(0x2946a1d8, byteArrayOf(0xd8.toByte(), 0xc3.toByte(), 0x9a.toByte(), 0xca.toByte(), 0x2.toByte()))
        performTest(0xe2, byteArrayOf(0xe2.toByte(), 0x1.toByte()))
        performTest(0x44ee, byteArrayOf(0xee.toByte(), 0x89.toByte(), 0x1.toByte()))
        performTest(0x447a, byteArrayOf(0xfa.toByte(), 0x88.toByte(), 0x1.toByte()))
        performTest(0x917, byteArrayOf(0x97.toByte(), 0x12.toByte()))
        performTest(0x25, byteArrayOf(0x25.toByte()))
        performTest(0x52c2b8eb, byteArrayOf(0xeb.toByte(), 0xf1.toByte(), 0x8a.toByte(), 0x96.toByte(), 0x5.toByte()))
        performTest(0x17dabee4, byteArrayOf(0xe4.toByte(), 0xfd.toByte(), 0xea.toByte(), 0xbe.toByte(), 0x1.toByte()))
        performTest(0x9d6a, byteArrayOf(0xea.toByte(), 0xba.toByte(), 0x2.toByte()))
        performTest(0xc4b12d, byteArrayOf(0xad.toByte(), 0xe2.toByte(), 0x92.toByte(), 0x6.toByte()))
        performTest(0xc9561d, byteArrayOf(0x9d.toByte(), 0xac.toByte(), 0xa5.toByte(), 0x6.toByte()))
        performTest(0x88a7, byteArrayOf(0xa7.toByte(), 0x91.toByte(), 0x2.toByte()))
        performTest(0x527d8f7a, byteArrayOf(0xfa.toByte(), 0x9e.toByte(), 0xf6.toByte(), 0x93.toByte(), 0x5.toByte()))
        performTest(0x2c31, byteArrayOf(0xb1.toByte(), 0x58.toByte()))
        performTest(0x3b8c, byteArrayOf(0x8c.toByte(), 0x77.toByte()))
        performTest(0xc228, byteArrayOf(0xa8.toByte(), 0x84.toByte(), 0x3.toByte()))
        performTest(0xd730d3, byteArrayOf(0xd3.toByte(), 0xe1.toByte(), 0xdc.toByte(), 0x6.toByte()))
    }

    @Test
    fun testUleb128Failure() {
        // result doesn't fit into a signed int
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x8.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x8.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x9.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xa.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xb.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xc.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xd.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xe.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xf.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x18.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x29.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x7a.toByte(), 0x11.toByte()))

        // MSB of last byte is set
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x81.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0xa0.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0xf0.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0xff.toByte(), 0x11.toByte()))
        performFailureTest(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x11.toByte()))
    }


    private fun performTest(expectedValue: Int, buf: ByteArray) {
        performTest(expectedValue, buf, buf.size)
    }

    private fun performTest(expectedValue: Int, buf: ByteArray, expectedLength: Int) {
        var dexBuf = DexBuffer(buf)
        var reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedValue, reader.readSmallUleb128())
        Assert.assertEquals(expectedLength, reader.offset)

        reader = dexBuf.readerAt(0)
        reader.skipUleb128()
        Assert.assertEquals(expectedLength, reader.offset)

        reader = dexBuf.readerAt(0)
        Assert.assertEquals(expectedLength, reader.peekSmallUleb128Size())
    }

    private fun performFailureTest(buf: ByteArray) {
        var dexBuf = DexBuffer(buf)
        var reader = dexBuf.readerAt(0)
        try {
            reader.peekSmallUleb128Size()
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected exception
    }

        try {
            reader.readSmallUleb128()
            Assert.fail()
        } catch (ex: ExceptionWithContext) {
            // expected exception
    }
    }
}
