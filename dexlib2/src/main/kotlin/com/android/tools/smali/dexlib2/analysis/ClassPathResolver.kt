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

import com.android.tools.smali.dexlib2.DexFileFactory.UnsupportedFileTypeException
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile
import com.android.tools.smali.dexlib2.dexbacked.OatFile
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.iface.MultiDexContainer.DexEntry
import com.android.tools.smali.util.StringUtils
import java.io.File
import java.io.IOException

open class ClassPathResolver @Throws(IOException::class) constructor(
    bootClassPathDirs: List<String>,
    bootClassPathEntries: List<String>?,
    extraClassPathEntries: List<String>,
    dexEntry: DexEntry<*>
) {
    private val classPathDirs: Iterable<String>

    private val pathEntryLoader: PathEntryLoader

    /**
     * Constructs a new ClassPathResolver using a specified list of bootclasspath entries
     *
     * @param bootClassPathDirs A list of directories to search for boot classpath entries. Can be empty if all boot
     *                          classpath entries are specified as local paths
     * @param bootClassPathEntries A list of boot classpath entries to load. These can either be local paths, or
     *                             device paths (e.g. "/system/framework/framework.jar"). The entry will be interpreted
     *                             first as a local path. If not found as a local path, it will be interpreted as a
     *                             partial or absolute device path, and will be searched for in bootClassPathDirs
     * @param extraClassPathEntries A list of additional classpath entries to load. Can be empty. All entries must be
     *                              local paths. Device paths are not supported.
     * @param dexEntry The dex entry containing the dex file that the classpath will be used to analyze
     * @throws IOException If any IOException occurs
     * @throws ResolveException If any classpath entries cannot be loaded for some reason
     *
     *  If null, a default bootclasspath is used,
     *                             depending on the the file type of dexFile and the api level. If empty, no boot
     *                             classpath entries will be loaded
     */
    init {
        val dexFile = dexEntry.dexFile

        this.classPathDirs = bootClassPathDirs
        this.pathEntryLoader = PathEntryLoader(dexEntry.dexFile.opcodes)

        val bootEntries = bootClassPathEntries ?: getDefaultBootClassPath(dexEntry, dexFile.opcodes.api)

        for (entry in bootEntries) {
            try {
                loadLocalOrDeviceBootClassPathEntry(entry)
            } catch (ex: PathEntryLoader.NoDexException) {
                if (entry.endsWith(".jar")) {
                    val odexEntry = entry.substring(0, entry.length - 4) + ".odex"
                    try {
                        loadLocalOrDeviceBootClassPathEntry(odexEntry)
                    } catch (ex2: PathEntryLoader.NoDexException) {
                        throw ResolveException("Neither %s nor %s contain a dex file", entry, odexEntry)
                    } catch (ex2: NotFoundException) {
                        throw ResolveException(ex)
                    }
                } else {
                    throw ResolveException(ex)
                }
            } catch (ex: NotFoundException) {
                if (entry.endsWith(".odex")) {
                    val jarEntry = entry.substring(0, entry.length - 5) + ".jar"
                    try {
                        loadLocalOrDeviceBootClassPathEntry(jarEntry)
                    } catch (ex2: PathEntryLoader.NoDexException) {
                        throw ResolveException("Neither %s nor %s contain a dex file", entry, jarEntry)
                    } catch (ex2: NotFoundException) {
                        throw ResolveException(ex)
                    }
                } else {
                    throw ResolveException(ex)
                }
            }
        }

        for (entry in extraClassPathEntries) {
            // extra classpath entries must be specified using a local path, so we don't need to do the search through
            // bootClassPathDirs
            try {
                loadLocalClassPathEntry(entry)
            } catch (ex: PathEntryLoader.NoDexException) {
                throw ResolveException(ex)
            }
        }

        val container = dexEntry.container
        for (entry in container.dexEntryNames) {
            val tempDexEntry = container.getEntry(entry)
            assert(tempDexEntry != null)
            pathEntryLoader.classProviders.add(DexClassProvider(tempDexEntry!!.dexFile))
        }
    }

    /**
     * Constructs a new ClassPathResolver using a default list of bootclasspath entries
     *
     * @param bootClassPathDirs A list of directories to search for boot classpath entries
     * @param extraClassPathEntries A list of additional classpath entries to load. Can be empty. All entries must be
     *                              local paths. Device paths are not supported.
     * @param dexEntry The dex entry containing the dex file that the classpath will be used to analyze
     * @throws IOException If any IOException occurs
     * @throws ResolveException If any classpath entries cannot be loaded for some reason
     *
     *  If null, a default bootclasspath is used,
     *                             depending on the the file type of dexFile and the api level. If empty, no boot
     *                             classpath entries will be loaded
     */
    @Throws(IOException::class)
    constructor(
        bootClassPathDirs: List<String>,
        extraClassPathEntries: List<String>,
        dexEntry: DexEntry<*>
    ) : this(bootClassPathDirs, null, extraClassPathEntries, dexEntry)

    val resolvedClassProviders: List<ClassProvider>
        get() = pathEntryLoader.getResolvedClassProviders()

    @Throws(PathEntryLoader.NoDexException::class, IOException::class)
    private fun loadLocalClassPathEntry(entry: String): Boolean {
        val entryFile = File(entry)
        if (entryFile.exists() && entryFile.isFile) {
            try {
                pathEntryLoader.loadEntry(entryFile, true)
                return true
            } catch (ex: UnsupportedFileTypeException) {
                throw ResolveException(ex, "Couldn't load classpath entry %s", entry)
            }
        }
        return false
    }

    @Throws(IOException::class, PathEntryLoader.NoDexException::class, NotFoundException::class)
    private fun loadLocalOrDeviceBootClassPathEntry(entry: String) {
        // first, see if the entry is a valid local path
        if (loadLocalClassPathEntry(entry)) {
            return
        }

        // It's not a local path, so let's try to resolve it as a device path, relative to one of the provided
        // directories
        val pathComponents = splitDevicePath(entry)

        for (directory in classPathDirs) {
            val directoryFile = File(directory)
            if (!directoryFile.exists()) {
                continue
            }

            for (i in pathComponents.indices) {
                val partialPath = StringUtils.join(
                    pathComponents.subList(i, pathComponents.size), File.separator
                )
                val entryFile = File(directoryFile, partialPath)
                if (entryFile.exists() && entryFile.isFile) {
                    pathEntryLoader.loadEntry(entryFile, true)
                    return
                }
            }
        }

        throw NotFoundException("Could not find classpath entry %s", entry)
    }

    internal class NotFoundException(message: String, vararg formatArgs: Any?) :
        Exception(String.format(message, *formatArgs))

    /**
     * An error that occurred while resolving the classpath
     */
    open class ResolveException : RuntimeException {
        constructor(message: String, vararg formatArgs: Any?) :
            super(String.format(message, *formatArgs))

        constructor(cause: Throwable) : super(cause)

        constructor(cause: Throwable, message: String, vararg formatArgs: Any?) :
            super(String.format(message, *formatArgs), cause)
    }

    companion object {
        private fun splitDevicePath(path: String): List<String> {
            return path.split("/")
        }

        /**
         * Returns the default boot class path for the given dex file and api level.
         */
        private fun getDefaultBootClassPath(dexEntry: DexEntry<*>, apiLevel: Int): List<String> {
            val container = dexEntry.container

            if (container is OatFile) {
                return bootClassPathForOat(container)
            }

            val dexFile = dexEntry.dexFile

            if (dexFile is DexBackedOdexFile) {
                return dexFile.dependencies
            }

            if (apiLevel <= 8) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar"
                )
            } else if (apiLevel <= 11) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar"
                )
            } else if (apiLevel <= 13) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar"
                )
            } else if (apiLevel <= 15) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/filterfw.jar"
                )
            } else if (apiLevel <= 17) {
                // this is correct as of api 17/4.2.2
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar"
                )
            } else if (apiLevel <= 18) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar"
                )
            } else if (apiLevel <= 19) {
                return listOf(
                    "/system/framework/core.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/framework2.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/webviewchromium.jar"
                )
            } else if (apiLevel <= 22) {
                return listOf(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/apache-xml.jar"
                )
            } else if (apiLevel <= 23) {
                return listOf(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/org.apache.http.legacy.boot.jar"
                )
            } else /*if (apiLevel <= 24)*/ {
                return listOf(
                    "/system/framework/core-oj.jar",
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/org.apache.http.legacy.boot.jar"
                )
            }
        }

        private fun bootClassPathForOat(oatFile: OatFile): List<String> {
            val bcp = oatFile.bootClassPath
            if (bcp.isEmpty()) {
                return listOf("boot.oat")
            } else {
                return replaceElementsSuffix(bcp, ".art", ".oat")
            }
        }

        private fun replaceElementsSuffix(
            bcp: MutableList<String>,
            originalSuffix: String,
            newSuffix: String
        ): List<String> {
            for (i in bcp.indices) {
                val entry = bcp[i]
                if (entry.endsWith(originalSuffix)) {
                    bcp[i] = entry.substring(0, entry.length - originalSuffix.length) + newSuffix
                }
            }
            return bcp
        }
    }
}
