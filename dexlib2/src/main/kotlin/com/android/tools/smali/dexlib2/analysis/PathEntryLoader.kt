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


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.OatFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashSet

open class PathEntryLoader(@JvmField var opcodes: Opcodes) {
    @JvmField
    val loadedFiles: MutableSet<File> = HashSet()

    @JvmField
    val classProviders: MutableList<ClassProvider> = ArrayList()

    fun getOpcodes(): Opcodes {
        return opcodes
    }

    fun getClassProviders(): List<ClassProvider> {
        return classProviders
    }

    fun getResolvedClassProviders(): List<ClassProvider> {
        return classProviders
    }

    @Throws(IOException::class, NoDexException::class)
    fun loadEntry(entryFile: File, loadOatDependencies: Boolean) {
        if (loadedFiles.contains(entryFile)) {
            return
        }

        val container: MultiDexContainer<out DexBackedDexFile>
        try {
            container = DexFileFactory.loadDexContainer(entryFile, opcodes)
        } catch (ex: DexFileFactory.UnsupportedFileTypeException) {
            throw ClassPathResolver.ResolveException(ex)
        }

        val entryNames = container.dexEntryNames

        if (entryNames.isEmpty()) {
            throw NoDexException("%s contains no dex file", entryFile)
        }

        loadedFiles.add(entryFile)

        for (entryName in entryNames) {
            classProviders.add(DexClassProvider(container.getEntry(entryName)!!.dexFile))
        }

        if (loadOatDependencies && container is OatFile) {
            val oatDependencies = container.bootClassPath
            if (!oatDependencies.isEmpty()) {
                try {
                    loadOatDependencies(entryFile.parentFile!!, oatDependencies)
                } catch (ex: ClassPathResolver.NotFoundException) {
                    throw ClassPathResolver.ResolveException(
                        ex, "Error while loading oat file %s", entryFile
                    )
                } catch (ex: NoDexException) {
                    throw ClassPathResolver.ResolveException(
                        ex, "Error while loading dependencies for oat file %s", entryFile
                    )
                }
            }
        }
    }

    @Throws(IOException::class, NoDexException::class, ClassPathResolver.NotFoundException::class)
    private fun loadOatDependencies(directory: File, oatDependencies: List<String>) {
        // We assume that all oat dependencies are located in the same directory as the oat file
        for (oatDependency in oatDependencies) {
            val oatDependencyName = getFilenameForOatDependency(oatDependency)
            val file = File(directory, oatDependencyName)
            if (!file.exists()) {
                throw ClassPathResolver.NotFoundException(
                    "Cannot find dependency %s in %s", oatDependencyName, directory
                )
            }

            loadEntry(file, false)
        }
    }

    private fun getFilenameForOatDependency(oatDependency: String): String {
        val index = oatDependency.lastIndexOf('/')

        val dependencyLeaf = oatDependency.substring(index + 1)
        if (dependencyLeaf.endsWith(".art")) {
            return dependencyLeaf.substring(0, dependencyLeaf.length - 4) + ".oat"
        }
        return dependencyLeaf
    }

    class NoDexException(message: String, vararg formatArgs: Any?) :
        Exception(String.format(message, *formatArgs))
}
