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

package com.android.tools.smali.baksmali

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.util.ClassFileNameHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

fun disassembleDexFile(dexFile: DexFile, outputDir: File, jobs: Int, options: BaksmaliOptions): Boolean =
    disassembleDexFile(dexFile, outputDir, jobs, options, null)

fun disassembleDexFile(
    dexFile: DexFile, outputDir: File, jobs: Int, options: BaksmaliOptions,
    classes: List<String>?
): Boolean {
    // sort the classes, so that if we're on a case-insensitive file system and need to handle
    // classes with file name collisions, then we'll use the same name for each class, if the dex
    // file goes through multiple baksmali/smali cycles for some reason. If a class with a
    // colliding name is added or removed, the filenames may still change of course
    val classDefs = dexFile.classes.sortedBy { it.type }

    val fileNameHandler = ClassFileNameHandler(outputDir, ".smali")
    val classSet = classes?.toHashSet()

    // Disassemble each class on a dispatcher that honours the requested job count.
    // supervisorScope + runCatching makes sure one failing class does not cancel the others (same
    // behaviour as the old thread-pool implementation, which waited for every task).
    val results = runBlocking {
        supervisorScope {
            classDefs
                .filter { classSet == null || it.type in classSet }
                .map { classDef ->
                    async(disassemblyDispatcher(jobs)) {
                        runCatching { disassembleClass(classDef, fileNameHandler, options) }
                    }
                }
                .awaitAll()
        }
    }

    results.firstOrNull { it.isFailure }?.let { throw RuntimeException(it.exceptionOrNull()) }

    return results.all { it.getOrDefault(false) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun disassemblyDispatcher(jobs: Int): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(jobs.coerceAtLeast(1))

private fun disassembleClass(
    classDef: ClassDef, fileNameHandler: ClassFileNameHandler, options: BaksmaliOptions
): Boolean {
    /**
     * The path for the disassembly file is based on the package name
     * The class descriptor will look something like:
     * Ljava/lang/Object;
     * Where the there is leading 'L' and a trailing ';', and the parts of the
     * package name are separated by '/'
     */
    val classDescriptor = classDef.type

    //validate that the descriptor is formatted like we expect
    if (classDescriptor[0] != 'L' ||
        classDescriptor[classDescriptor.length - 1] != ';') {
        System.err.println("Unrecognized class descriptor - $classDescriptor - skipping class")
        return false
    }

    val smaliFile: File
    try {
        smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor)
    } catch (ex: IOException) {
        System.err.println("\n\nError occurred while creating file for class $classDescriptor")
        ex.printStackTrace()
        return false
    }

    //create and initialize the top level string template
    val classDefinition = ClassDefinition(options, classDef)

    //write the disassembly
    var writer: BaksmaliWriter? = null
    try {
        val smaliParent = smaliFile.parentFile
        if (!smaliParent.exists()) {
            if (!smaliParent.mkdirs()) {
                // check again, it's likely it was created in a different thread
                if (!smaliParent.exists()) {
                    System.err.println("Unable to create directory ${smaliParent.toString()} - skipping class")
                    return false
                }
            }
        }

        if (!smaliFile.exists()) {
            if (!smaliFile.createNewFile()) {
                System.err.println("Unable to create file ${smaliFile.toString()} - skipping class")
                return false
            }
        }

        val bufWriter = BufferedWriter(OutputStreamWriter(
            FileOutputStream(smaliFile), StandardCharsets.UTF_8))

        writer = BaksmaliWriter(
            bufWriter,
            if (options.implicitReferences) classDef.type else null)
        classDefinition.writeTo(writer)
    } catch (ex: Exception) {
        System.err.println("\n\nError occurred while disassembling class ${classDescriptor.replace('/', '.')} - skipping class")
        ex.printStackTrace()
        // noinspection ResultOfMethodCallIgnored
        smaliFile.delete()
        return false
    } finally {
        if (writer != null) {
            try {
                writer.close()
            } catch (ex: Throwable) {
                System.err.println("\n\nError occurred while closing file ${smaliFile.toString()}")
                ex.printStackTrace()
            }
        }
    }
    return true
}
