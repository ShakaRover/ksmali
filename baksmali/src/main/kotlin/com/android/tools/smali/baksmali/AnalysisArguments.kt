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

package com.android.tools.smali.baksmali

import com.android.tools.smali.dexlib2.VersionMap
import com.android.tools.smali.dexlib2.analysis.ClassPath
import com.android.tools.smali.dexlib2.analysis.ClassPathResolver
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.OatFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.util.jcommander.ColonParameterSplitter
import com.android.tools.smali.util.jcommander.ExtendedParameter
import com.beust.jcommander.Parameter
import java.io.File
import java.io.IOException

open class AnalysisArguments {
    @field:Parameter(
        names = ["-b", "--bootclasspath", "--bcp"],
        description = "A colon separated list of the files to include in the bootclasspath when analyzing the " +
            "dex file. If not specified, baksmali will attempt to choose an " +
            "appropriate default. When analyzing oat files, this can simply be the path to the device's " +
            "boot.oat file. A single empty string can be used to specify that an empty bootclasspath should " +
            "be used. (e.g. --bootclasspath \"\") See baksmali help classpath for more information.",
        splitter = ColonParameterSplitter::class
    )
    @field:ExtendedParameter(argumentNames = ["classpath"])
    var bootClassPath: List<String>? = null

    @field:Parameter(
        names = ["-c", "--classpath", "--cp"],
        description = "A colon separated list of additional files to include in the classpath when analyzing the " +
            "dex file. These will be added to the classpath after any bootclasspath entries.",
        splitter = ColonParameterSplitter::class
    )
    @field:ExtendedParameter(argumentNames = ["classpath"])
    var classPath: List<String> = mutableListOf()

    @field:Parameter(
        names = ["-d", "--classpath-dir", "--cpd", "--dir"],
        description = "A directory to search for classpath files. This option can be used multiple times to " +
            "specify multiple directories to search. They will be searched in the order they are provided."
    )
    @field:ExtendedParameter(argumentNames = ["dir"])
    var classPathDirectories: List<String>? = null

    class CheckPackagePrivateArgument {
        @field:Parameter(
            names = ["--check-package-private-access", "--package-private", "--checkpp", "--pp"],
            description = "Use the package-private access check when calculating vtable indexes. This is enabled " +
                "by default for oat files. For odex files, this is only needed for odexes from 4.2.0. It " +
                "was reverted in 4.2.1."
        )
        var checkPackagePrivateAccess = false
    }

    @Throws(IOException::class)
    fun loadClassPathForDexFile(
        dexFileDir: File,
        dexEntry: MultiDexContainer.DexEntry<out DexBackedDexFile>,
        checkPackagePrivateAccess: Boolean
    ): ClassPath {
        return loadClassPathForDexFile(
            dexFileDir, dexEntry, checkPackagePrivateAccess, ClassPath.NOT_SPECIFIED)
    }

    @Throws(IOException::class)
    fun loadClassPathForDexFile(
        dexFileDir: File,
        dexEntry: MultiDexContainer.DexEntry<out DexBackedDexFile>,
        checkPackagePrivateAccessArg: Boolean,
        oatVersionArg: Int
    ): ClassPath {
        var checkPackagePrivateAccess = checkPackagePrivateAccessArg
        var oatVersion = oatVersionArg

        val resolver: ClassPathResolver

        val container = dexEntry.container

        if (oatVersion == ClassPath.NOT_SPECIFIED) {
            if (container is OatFile) {
                checkPackagePrivateAccess = true
                oatVersion = container.oatVersion
            } else {
                oatVersion = VersionMap.mapApiToArtVersion(dexEntry.dexFile.opcodes.api)
            }
        } else {
            // this should always be true for ART
            checkPackagePrivateAccess = true
        }

        if (classPathDirectories.isNullOrEmpty()) {
            classPathDirectories = mutableListOf(dexFileDir.path)
        }

        val filteredClassPathDirectories = mutableListOf<String>()
        classPathDirectories?.forEach { dir ->
            val file = File(dir)
            if (!file.exists()) {
                System.err.println("Warning: directory $dir does not exist. Ignoring.")
            } else if (!file.isDirectory) {
                System.err.println("Warning: $dir is not a directory. Ignoring.")
            } else {
                filteredClassPathDirectories.add(dir)
            }
        }

        val bootClassPath = this.bootClassPath
        if (bootClassPath == null) {
            // TODO: obtain the api from the Opcodes object associated with the dexFile instead of
            // defaulting it; the oat version -> api mapping required for that is not complete yet.
            resolver = ClassPathResolver(filteredClassPathDirectories, classPath, dexEntry)
        } else if (bootClassPath.size == 1 && bootClassPath[0].isEmpty()) {
            // --bootclasspath "" is a special case, denoting that no bootclasspath should be used
            resolver = ClassPathResolver(
                emptyList(), emptyList(), classPath, dexEntry)
        } else {
            resolver = ClassPathResolver(filteredClassPathDirectories, bootClassPath, classPath, dexEntry)
        }

        if (oatVersion == 0 && container is OatFile) {
            oatVersion = container.oatVersion
        }
        return ClassPath(resolver.resolvedClassProviders, checkPackagePrivateAccess, oatVersion)
    }
}
