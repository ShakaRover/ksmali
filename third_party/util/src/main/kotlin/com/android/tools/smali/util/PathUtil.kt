/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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


@file:JvmName("PathUtil")

package com.android.tools.smali.util

import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.CharBuffer
import java.util.ArrayList

@Throws(IOException::class)
fun getRelativeFile(baseFile0: File, fileToRelativize: File): File {
    var baseFile = baseFile0
    if (baseFile.isFile) {
        baseFile = baseFile.parentFile!!
    }

    return File(getRelativeFileInternal(baseFile.canonicalFile, fileToRelativize.canonicalFile))
}

fun getRelativeFileInternal(canonicalBaseFile: File, canonicalFileToRelativize: File): String {
    val basePath = getPathComponents(canonicalBaseFile)
    val pathToRelativize = getPathComponents(canonicalFileToRelativize)

    //if the roots aren't the same (i.e. different drives on a windows machine), we can't construct a relative
    //path from one to the other, so just return the canonical file
    if (basePath[0] != pathToRelativize[0]) {
        return canonicalFileToRelativize.path
    }

    return buildString {
        var commonDirs = 1
        while (commonDirs < basePath.size && commonDirs < pathToRelativize.size) {
            if (basePath[commonDirs] != pathToRelativize[commonDirs]) {
                break
            }
            commonDirs++
        }

        var first = true
        for (i in commonDirs until basePath.size) {
            if (!first) {
                append(File.separatorChar)
            } else {
                first = false
            }

            append("..")
        }

        first = true
        for (i in commonDirs until pathToRelativize.size) {
            if (first) {
                if (length != 0) {
                    append(File.separatorChar)
                }
                first = false
            } else {
                append(File.separatorChar)
            }

            append(pathToRelativize[i])
        }

        if (length == 0) {
            return "."
        }
    }
}

private fun getPathComponents(file0: File): List<String> {
    val path = ArrayList<String>()

    var file: File? = file0
    while (file != null) {
        val parentFile = file.parentFile

        if (parentFile == null) {
            path.add(file.path)
        } else {
            path.add(file.name)
        }

        file = parentFile
    }

    return path.reversed()
}

@Throws(IOException::class)
fun testCaseSensitivity(path: File): Boolean {
    var num = 1
    var f: File
    var f2: File
    do {
        f = File(path, "test.$num")
        f2 = File(path, "TEST." + num)
        num++
    } while (f.exists() || f2.exists())

    try {
        try {
            val writer = FileWriter(f)
            writer.write("test")
            writer.flush()
            writer.close()
        } catch (ex: IOException) {
            try {
                f.delete()
            } catch (ex2: Exception) {
            }
            throw ex
        }

        if (f2.exists()) {
            return false
        }

        if (f2.createNewFile()) {
            return true
        }

        //the above 2 tests should catch almost all cases. But maybe there was a failure while creating f2
        //that isn't related to case sensitivity. Let's see if we can open the file we just created using
        //f2
        try {
            val buf = CharBuffer.allocate(32)
            val reader = FileReader(f2)

            while (reader.read(buf) != -1 && buf.length < 4) {
            }
            if (buf.length == 4 && buf.toString() == "test") {
                return false
            } else {
                //we probably shouldn't get here. If the filesystem was case-sensetive, creating a new
                //FileReader should have thrown a FileNotFoundException. Otherwise, we should have opened
                //the file and read in the string "test". It's remotely possible that someone else modified
                //the file after we created it. Let's be safe and return false here as well
                assert(false)
                return false
            }
        } catch (ex: FileNotFoundException) {
            return true
        }
    } finally {
        try {
            f.delete()
        } catch (ex: Exception) {
        }
        try {
            f2.delete()
        } catch (ex: Exception) {
        }
    }
}
