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

package com.android.tools.smali.dexlib2.writer

import org.junit.Assert
import org.junit.Test
import java.io.IOException

class DexWriterSleb128Test {
    private val output = NakedByteArrayOutputStream()
    private var startPosition = 0
    private lateinit var writer: DexDataWriter

    @Throws(IOException::class)
    fun setup() {
        output.reset()
        startPosition = 123
        val bufferSize = 256
        writer = DexDataWriter(output, startPosition, bufferSize)
    }

    @Test
    @Throws(IOException::class)
    fun testSleb128() {
        performTest(0x0.toInt(), byteArrayOf(0x0.toByte(), 0x11.toByte()), 1)
        performTest(0x1.toInt(), byteArrayOf(0x1.toByte(), 0x11.toByte()), 1)
        performTest(0x3f.toInt(), byteArrayOf(0x3f.toByte(), 0x11.toByte()), 1)
        performTest(0xffffffc0.toInt(), byteArrayOf(0x40.toByte(), 0x11.toByte()), 1)
        performTest(0xfffffff0.toInt(), byteArrayOf(0x70.toByte(), 0x11.toByte()), 1)
        performTest(0xffffffff.toInt(), byteArrayOf(0x7f.toByte(), 0x11.toByte()), 1)

        performTest(0x80.toInt(), byteArrayOf(0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 2)
        performTest(0x100.toInt(), byteArrayOf(0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 2)
        performTest(0x800.toInt(), byteArrayOf(0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 2)
        performTest(0x1f80.toInt(), byteArrayOf(0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 2)
        performTest(0xffffe000.toInt(), byteArrayOf(0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 2)
        performTest(0xffffe080.toInt(), byteArrayOf(0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 2)
        performTest(0xfffff800.toInt(), byteArrayOf(0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 2)
        performTest(0xffffff80.toInt(), byteArrayOf(0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 2)

        performTest(0xff.toInt(), byteArrayOf(0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 2)
        performTest(0x17f.toInt(), byteArrayOf(0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 2)
        performTest(0x87f.toInt(), byteArrayOf(0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 2)
        performTest(0x1fff.toInt(), byteArrayOf(0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 2)
        performTest(0xffffe07f.toInt(), byteArrayOf(0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 2)
        performTest(0xffffe0ff.toInt(), byteArrayOf(0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 2)
        performTest(0xfffff87f.toInt(), byteArrayOf(0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 2)

        performTest(0x4000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 3)
        performTest(0x8000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 3)
        performTest(0x40000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 3)
        performTest(0xfc000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 3)
        performTest(0xfff00000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 3)
        performTest(0xfff04000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 3)
        performTest(0xfffc0000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 3)
        performTest(0xffffc000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 3)

        performTest(0x7fff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 3)
        performTest(0xbfff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 3)
        performTest(0x43fff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 3)
        performTest(0xfffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 3)
        performTest(0xfff03fff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 3)
        performTest(0xfff07fff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 3)
        performTest(0xfffc3fff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 3)

        performTest(0x200000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 4)
        performTest(0x400000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 4)
        performTest(0x2000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x10.toByte(), 0x11.toByte()), 4)
        performTest(0x7e00000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x3f.toByte(), 0x11.toByte()), 4)
        performTest(0xf8000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x40.toByte(), 0x11.toByte()), 4)
        performTest(0xf8200000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x41.toByte(), 0x11.toByte()), 4)
        performTest(0xfe000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x70.toByte(), 0x11.toByte()), 4)
        performTest(0xffe00000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x7f.toByte(), 0x11.toByte()), 4)

        performTest(0x3fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 4)
        performTest(0x5fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 4)
        performTest(0x21fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x10.toByte(), 0x11.toByte()), 4)
        performTest(0x7ffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x3f.toByte(), 0x11.toByte()), 4)
        performTest(0xf81fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x40.toByte(), 0x11.toByte()), 4)
        performTest(0xf83fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x41.toByte(), 0x11.toByte()), 4)
        performTest(0xfe1fffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x70.toByte(), 0x11.toByte()), 4)

        performTest(0x10000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x1.toByte(), 0x11.toByte()), 5)
        performTest(0x20000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x2.toByte(), 0x11.toByte()), 5)
        performTest(0x70000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x7.toByte(), 0x11.toByte()), 5)
        performTest(0x80000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x8.toByte(), 0x11.toByte()), 5)
        performTest(0xe0000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0xe.toByte(), 0x11.toByte()), 5)
        performTest(0xf0000000.toInt(), byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0xf.toByte(), 0x11.toByte()), 5)

        performTest(0x1fffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x1.toByte(), 0x11.toByte()), 5)
        performTest(0x2fffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x2.toByte(), 0x11.toByte()), 5)
        performTest(0x7fffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7.toByte(), 0x11.toByte()), 5)
        performTest(0x8fffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x8.toByte(), 0x11.toByte()), 5)
        performTest(0xefffffff.toInt(), byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xe.toByte(), 0x11.toByte()), 5)

        performTest(0x8197d2.toInt(), byteArrayOf(0xd2.toByte(), 0xaf.toByte(), 0x86.toByte(), 0x4.toByte()))
        performTest(0x3cc8eb78.toInt(), byteArrayOf(0xf8.toByte(), 0xd6.toByte(), 0xa3.toByte(), 0xe6.toByte(), 0x3.toByte()))
        performTest(0x51307f32.toInt(), byteArrayOf(0xb2.toByte(), 0xfe.toByte(), 0xc1.toByte(), 0x89.toByte(), 0x5.toByte()))
        performTest(0x8893.toInt(), byteArrayOf(0x93.toByte(), 0x91.toByte(), 0x2.toByte()))
        performTest(0x80fb.toInt(), byteArrayOf(0xfb.toByte(), 0x81.toByte(), 0x2.toByte()))
        performTest(0x3d.toInt(), byteArrayOf(0x3d.toByte()))
        performTest(0x987c.toInt(), byteArrayOf(0xfc.toByte(), 0xb0.toByte(), 0x2.toByte()))
        performTest(0x5b2478.toInt(), byteArrayOf(0xf8.toByte(), 0xc8.toByte(), 0xec.toByte(), 0x2.toByte()))
        performTest(0x65350ed9.toInt(), byteArrayOf(0xd9.toByte(), 0x9d.toByte(), 0xd4.toByte(), 0xa9.toByte(), 0x6.toByte()))
        performTest(0x3e.toInt(), byteArrayOf(0x3e.toByte()))
        performTest(0x7b1e.toInt(), byteArrayOf(0x9e.toByte(), 0xf6.toByte(), 0x1.toByte()))
        performTest(0xb5.toInt(), byteArrayOf(0xb5.toByte(), 0x1.toByte()))
        performTest(0x96.toInt(), byteArrayOf(0x96.toByte(), 0x1.toByte()))
        performTest(0xa1.toInt(), byteArrayOf(0xa1.toByte(), 0x1.toByte()))
        performTest(0x4d50a85d.toInt(), byteArrayOf(0xdd.toByte(), 0xd0.toByte(), 0xc2.toByte(), 0xea.toByte(), 0x4.toByte()))
        performTest(0xc419.toInt(), byteArrayOf(0x99.toByte(), 0x88.toByte(), 0x3.toByte()))
        performTest(0xcf34.toInt(), byteArrayOf(0xb4.toByte(), 0x9e.toByte(), 0x3.toByte()))
        performTest(0x527d.toInt(), byteArrayOf(0xfd.toByte(), 0xa4.toByte(), 0x1.toByte()))
        performTest(0x5a2894.toInt(), byteArrayOf(0x94.toByte(), 0xd1.toByte(), 0xe8.toByte(), 0x2.toByte()))
        performTest(0xa6.toInt(), byteArrayOf(0xa6.toByte(), 0x1.toByte()))
        performTest(0x3e05.toInt(), byteArrayOf(0x85.toByte(), 0xfc.toByte(), 0x0.toByte()))
        performTest(0x5f.toInt(), byteArrayOf(0xdf.toByte(), 0x0.toByte()))
        performTest(0xe2d9af.toInt(), byteArrayOf(0xaf.toByte(), 0xb3.toByte(), 0x8b.toByte(), 0x7.toByte()))
        performTest(0xa853fe14.toInt(), byteArrayOf(0x94.toByte(), 0xfc.toByte(), 0xcf.toByte(), 0xc2.toByte(), 0xa.toByte()))
        performTest(0xa853fe14.toInt(), byteArrayOf(0x94.toByte(), 0xfc.toByte(), 0xcf.toByte(), 0xc2.toByte(), 0x7a.toByte()))
        performTest(0x117de731.toInt(), byteArrayOf(0xb1.toByte(), 0xce.toByte(), 0xf7.toByte(), 0x8b.toByte(), 0x1.toByte()))
        performTest(0xb7c9.toInt(), byteArrayOf(0xc9.toByte(), 0xef.toByte(), 0x2.toByte()))
        performTest(0xb1.toInt(), byteArrayOf(0xb1.toByte(), 0x1.toByte()))
        performTest(0x4f194d.toInt(), byteArrayOf(0xcd.toByte(), 0xb2.toByte(), 0xbc.toByte(), 0x2.toByte()))
        performTest(0x8d5733.toInt(), byteArrayOf(0xb3.toByte(), 0xae.toByte(), 0xb5.toByte(), 0x4.toByte()))
        performTest(0x2824e9ae.toInt(), byteArrayOf(0xae.toByte(), 0xd3.toByte(), 0x93.toByte(), 0xc1.toByte(), 0x2.toByte()))
        performTest(0x792e.toInt(), byteArrayOf(0xae.toByte(), 0xf2.toByte(), 0x1.toByte()))
        performTest(0xadef.toInt(), byteArrayOf(0xef.toByte(), 0xdb.toByte(), 0x2.toByte()))
        performTest(0x5c.toInt(), byteArrayOf(0xdc.toByte(), 0x0.toByte()))
        performTest(0x14f9ccf8.toInt(), byteArrayOf(0xf8.toByte(), 0x99.toByte(), 0xe7.toByte(), 0xa7.toByte(), 0x1.toByte()))
        performTest(0xd1.toInt(), byteArrayOf(0xd1.toByte(), 0x1.toByte()))
        performTest(0xba787ecd.toInt(), byteArrayOf(0xcd.toByte(), 0xfd.toByte(), 0xe1.toByte(), 0xd3.toByte(), 0x7b.toByte()))
        performTest(0x4f.toInt(), byteArrayOf(0xcf.toByte(), 0x0.toByte()))
        performTest(0xfb03.toInt(), byteArrayOf(0x83.toByte(), 0xf6.toByte(), 0x3.toByte()))
        performTest(0xee3f7cd8.toInt(), byteArrayOf(0xd8.toByte(), 0xf9.toByte(), 0xfd.toByte(), 0xf1.toByte(), 0x7e.toByte()))
        performTest(0x9a6e.toInt(), byteArrayOf(0xee.toByte(), 0xb4.toByte(), 0x2.toByte()))
        performTest(0x8f0983.toInt(), byteArrayOf(0x83.toByte(), 0x93.toByte(), 0xbc.toByte(), 0x4.toByte()))
        performTest(0x3a00e01f.toInt(), byteArrayOf(0x9f.toByte(), 0xc0.toByte(), 0x83.toByte(), 0xd0.toByte(), 0x3.toByte()))
        performTest(0x7f532d93.toInt(), byteArrayOf(0x93.toByte(), 0xdb.toByte(), 0xcc.toByte(), 0xfa.toByte(), 0x7.toByte()))
        performTest(0x179d8d.toInt(), byteArrayOf(0x8d.toByte(), 0xbb.toByte(), 0xde.toByte(), 0x0.toByte()))
        performTest(0xfc5.toInt(), byteArrayOf(0xc5.toByte(), 0x1f.toByte()))
        performTest(0x11.toInt(), byteArrayOf(0x11.toByte()))
        performTest(0xc9b53e8.toInt(), byteArrayOf(0xe8.toByte(), 0xa7.toByte(), 0xed.toByte(), 0xe4.toByte(), 0x0.toByte()))
        performTest(0x97.toInt(), byteArrayOf(0x97.toByte(), 0x1.toByte()))
        performTest(0x52b3.toInt(), byteArrayOf(0xb3.toByte(), 0xa5.toByte(), 0x1.toByte()))
        performTest(0x92.toInt(), byteArrayOf(0x92.toByte(), 0x1.toByte()))
        performTest(0xd2.toInt(), byteArrayOf(0xd2.toByte(), 0x1.toByte()))
        performTest(0x13d330.toInt(), byteArrayOf(0xb0.toByte(), 0xa6.toByte(), 0xcf.toByte(), 0x0.toByte()))
        performTest(0x672f41.toInt(), byteArrayOf(0xc1.toByte(), 0xde.toByte(), 0x9c.toByte(), 0x3.toByte()))
        performTest(0xcf.toInt(), byteArrayOf(0xcf.toByte(), 0x1.toByte()))
        performTest(0x54ddb6dd.toInt(), byteArrayOf(0xdd.toByte(), 0xed.toByte(), 0xf6.toByte(), 0xa6.toByte(), 0x5.toByte()))
        performTest(0x7ebcae.toInt(), byteArrayOf(0xae.toByte(), 0xf9.toByte(), 0xfa.toByte(), 0x3.toByte()))
        performTest(0x38.toInt(), byteArrayOf(0x38.toByte()))
        performTest(0x8118f4e7.toInt(), byteArrayOf(0xe7.toByte(), 0xe9.toByte(), 0xe3.toByte(), 0x88.toByte(), 0x78.toByte()))
        performTest(0xac.toInt(), byteArrayOf(0xac.toByte(), 0x1.toByte()))
        performTest(0xab309c.toInt(), byteArrayOf(0x9c.toByte(), 0xe1.toByte(), 0xac.toByte(), 0x5.toByte()))
        performTest(0x1bf9b2.toInt(), byteArrayOf(0xb2.toByte(), 0xf3.toByte(), 0xef.toByte(), 0x0.toByte()))
        performTest(0x8b3c70.toInt(), byteArrayOf(0xf0.toByte(), 0xf8.toByte(), 0xac.toByte(), 0x4.toByte()))
        performTest(0x7774.toInt(), byteArrayOf(0xf4.toByte(), 0xee.toByte(), 0x1.toByte()))
        performTest(0x33e839.toInt(), byteArrayOf(0xb9.toByte(), 0xd0.toByte(), 0xcf.toByte(), 0x1.toByte()))
        performTest(0x84d655a0.toInt(), byteArrayOf(0xa0.toByte(), 0xab.toByte(), 0xd9.toByte(), 0xa6.toByte(), 0x78.toByte()))
        performTest(0xf3543ef3.toInt(), byteArrayOf(0xf3.toByte(), 0xfd.toByte(), 0xd0.toByte(), 0x9a.toByte(), 0x7f.toByte()))
        performTest(0x1d777e.toInt(), byteArrayOf(0xfe.toByte(), 0xee.toByte(), 0xf5.toByte(), 0x0.toByte()))
        performTest(0xf7.toInt(), byteArrayOf(0xf7.toByte(), 0x1.toByte()))
        performTest(0x2444.toInt(), byteArrayOf(0xc4.toByte(), 0xc8.toByte(), 0x0.toByte()))
        performTest(0x536b.toInt(), byteArrayOf(0xeb.toByte(), 0xa6.toByte(), 0x1.toByte()))
        performTest(0xa8.toInt(), byteArrayOf(0xa8.toByte(), 0x1.toByte()))
        performTest(0xdbfc.toInt(), byteArrayOf(0xfc.toByte(), 0xb7.toByte(), 0x3.toByte()))
        performTest(0xe66db7.toInt(), byteArrayOf(0xb7.toByte(), 0xdb.toByte(), 0x99.toByte(), 0x7.toByte()))
        performTest(0xb7ca.toInt(), byteArrayOf(0xca.toByte(), 0xef.toByte(), 0x2.toByte()))
        performTest(0xe807d0e5.toInt(), byteArrayOf(0xe5.toByte(), 0xa1.toByte(), 0x9f.toByte(), 0xc0.toByte(), 0x7e.toByte()))
        performTest(0x6a4.toInt(), byteArrayOf(0xa4.toByte(), 0xd.toByte()))
        performTest(0x64.toInt(), byteArrayOf(0xe4.toByte(), 0x0.toByte()))
        performTest(0xf3fb75.toInt(), byteArrayOf(0xf5.toByte(), 0xf6.toByte(), 0xcf.toByte(), 0x7.toByte()))
        performTest(0xb72cb6b9.toInt(), byteArrayOf(0xb9.toByte(), 0xed.toByte(), 0xb2.toByte(), 0xb9.toByte(), 0x7b.toByte()))
        performTest(0xfd.toInt(), byteArrayOf(0xfd.toByte(), 0x1.toByte()))
        performTest(0xb48b.toInt(), byteArrayOf(0x8b.toByte(), 0xe9.toByte(), 0x2.toByte()))
        performTest(0x39c3.toInt(), byteArrayOf(0xc3.toByte(), 0xf3.toByte(), 0x0.toByte()))
        performTest(0x12b8afbd.toInt(), byteArrayOf(0xbd.toByte(), 0xdf.toByte(), 0xe2.toByte(), 0x95.toByte(), 0x1.toByte()))
        performTest(0x56f149.toInt(), byteArrayOf(0xc9.toByte(), 0xe2.toByte(), 0xdb.toByte(), 0x2.toByte()))
        performTest(0xbf.toInt(), byteArrayOf(0xbf.toByte(), 0x1.toByte()))
        performTest(0x3ac72481.toInt(), byteArrayOf(0x81.toByte(), 0xc9.toByte(), 0x9c.toByte(), 0xd6.toByte(), 0x3.toByte()))
        performTest(0xb69ca721.toInt(), byteArrayOf(0xa1.toByte(), 0xce.toByte(), 0xf2.toByte(), 0xb4.toByte(), 0x7b.toByte()))
        performTest(0x2380.toInt(), byteArrayOf(0x80.toByte(), 0xc7.toByte(), 0x0.toByte()))
        performTest(0x656268.toInt(), byteArrayOf(0xe8.toByte(), 0xc4.toByte(), 0x95.toByte(), 0x3.toByte()))
        performTest(0x71.toInt(), byteArrayOf(0xf1.toByte(), 0x0.toByte()))
        performTest(0xf06425.toInt(), byteArrayOf(0xa5.toByte(), 0xc8.toByte(), 0xc1.toByte(), 0x7.toByte()))
        performTest(0xb587cb.toInt(), byteArrayOf(0xcb.toByte(), 0x8f.toByte(), 0xd6.toByte(), 0x5.toByte()))
        performTest(0x8742.toInt(), byteArrayOf(0xc2.toByte(), 0x8e.toByte(), 0x2.toByte()))
        performTest(0xc6.toInt(), byteArrayOf(0xc6.toByte(), 0x1.toByte()))
        performTest(0xee62789f.toInt(), byteArrayOf(0x9f.toByte(), 0xf1.toByte(), 0x89.toByte(), 0xf3.toByte(), 0x7e.toByte()))
        performTest(0x470a.toInt(), byteArrayOf(0x8a.toByte(), 0x8e.toByte(), 0x1.toByte()))
        performTest(0x11ef5cdc.toInt(), byteArrayOf(0xdc.toByte(), 0xb9.toByte(), 0xbd.toByte(), 0x8f.toByte(), 0x1.toByte()))
        performTest(0xc44ea9.toInt(), byteArrayOf(0xa9.toByte(), 0x9d.toByte(), 0x91.toByte(), 0x6.toByte()))
        performTest(0x94477f78.toInt(), byteArrayOf(0xf8.toByte(), 0xfe.toByte(), 0x9d.toByte(), 0xa2.toByte(), 0x79.toByte()))
        performTest(0xe47a0b4f.toInt(), byteArrayOf(0xcf.toByte(), 0x96.toByte(), 0xe8.toByte(), 0xa3.toByte(), 0x7e.toByte()))
    }

    private fun performTest(integerValue: Int, encodedValue: ByteArray) {
        performTest(integerValue, encodedValue, encodedValue.size)
    }

    private fun performTest(integerValue: Int, encodedValue: ByteArray, encodedLength: Int) {
        setup()

        writer.writeSleb128(integerValue)
        writer.flush()

        val writtenData = output.getBuffer()

        Assert.assertEquals(startPosition + encodedLength, writer.position)
        for (i in 0 until encodedLength) {
            var encoded = encodedValue[i]
            var written = writtenData[i]
            if (i == 4) {
                encoded = (encoded.toInt() and 0x0F).toByte()
                written = (written.toInt() and 0x0F).toByte()
            }
            Assert.assertEquals(String.format("Values not equal at index %d", i), encoded, written)
        }
    }
}
