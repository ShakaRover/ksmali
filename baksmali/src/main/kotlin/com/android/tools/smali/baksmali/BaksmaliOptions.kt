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

package com.android.tools.smali.baksmali

import com.android.tools.smali.dexlib2.analysis.ClassPath
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver
import com.android.tools.smali.dexlib2.util.SyntheticAccessorResolver
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.IOException
import javax.xml.XMLConstants
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

/**
 * Mutable configuration bag for the baksmali disassembler, used by [disassembleDexFile] /
 * [disassembleDexFileSuspend] and the `baksmali` command line commands.
 *
 * An instance is filled in by the caller (or by [DisassembleCommand]) and then handed to
 * [disassembleDexFile]; it is not immutable and is not safe to share between concurrent
 * disassemblies that configure it differently.
 *
 * The worker count is deliberately not part of this class: it is the `jobs` parameter of
 * [disassembleDexFile], which corresponds to the `-j/--jobs` option of
 * `baksmali disassemble`. Values below 1 are treated as 1, and it only affects throughput, never
 * the bytes written.
 */
open class BaksmaliOptions {
    /**
     * The API level used by the listing commands (`list vtables`, `list field-offsets`, ...) to
     * build their [com.android.tools.smali.dexlib2.analysis.ClassPath]. It is not consulted by
     * [disassembleDexFile], which uses the opcodes carried by the [com.android.tools.smali.dexlib2.iface.DexFile];
     * the `baksmali` command line exposes its `-a/--api` option on the input command instead.
     */
    var apiLevel = 15

    var parameterRegisters = true
    var localsDirective = false
    var sequentialLabels = false
    var debugInfo = true
    var codeOffsets = false
    var accessorComments = true
    var allowOdex = false
    var deodex = false
    var implicitReferences = false
    var normalizeVirtualMethods = false

    var registerInfo = 0

    var resourceIds: MutableMap<Int, String> = mutableMapOf()
    var inlineResolver: InlineMethodResolver? = null
    var classPath: ClassPath? = null
    var syntheticAccessorResolver: SyntheticAccessorResolver? = null

    /**
     * Load the resource ids from a set of public.xml files.
     *
     * @param resourceFiles A map of resource prefixes -&gt; public.xml files
     */
    @Throws(SAXException::class, IOException::class)
    fun loadResourceIds(resourceFiles: Map<String, File>) {
        for ((prefix, resourceFile) in resourceFiles) {
            try {
                val parserFactory = SAXParserFactory.newInstance()
                parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                val parser = parserFactory.newSAXParser()

                parser.parse(resourceFile, object : DefaultHandler() {
                    @Throws(SAXException::class)
                    override fun startElement(
                        uri: String?, localName: String?, qName: String?, attr: Attributes
                    ) {
                        if (qName == "public") {
                            val resourceType = attr.getValue("type")
                            val resourceName = attr.getValue("name").replace('.', '_')
                            val resourceId = Integer.decode(attr.getValue("id"))
                            val qualifiedResourceName = "$prefix.$resourceType.$resourceName"
                            resourceIds[resourceId] = qualifiedResourceName
                        }
                    }
                })
            } catch (ex: ParserConfigurationException) {
                throw RuntimeException(ex)
            }
        }
    }

    companion object {
        // register info values
        const val ALL = 1
        const val ALLPRE = 2
        const val ALLPOST = 4
        const val ARGS = 8
        const val DEST = 16
        const val MERGE = 32
        const val FULLMERGE = 64
    }
}
