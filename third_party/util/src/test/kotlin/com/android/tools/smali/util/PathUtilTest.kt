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

package com.android.tools.smali.util

import org.junit.Assert
import org.junit.Test
import java.io.File

class PathUtilTest {

    @Test
    fun pathUtilTest1() {
        val roots = File.listRoots()

        if (roots.size > 1) {
            val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
                File.separatorChar + "test.txt")
            val relativePath = File(roots[1].toString() + "some" + File.separatorChar + "dir" +
                File.separatorChar + "test.txt")

            val path = getRelativeFileInternal(basePath, relativePath)

            Assert.assertEquals(path, relativePath.path)
        }
    }

    @Test
    fun pathUtilTest2() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "test.txt")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "test.txt")

        val path = getRelativeFileInternal(basePath, relativePath)

        /*the "internal" version of the method in PathUtil doesn't handle the case when the "leaf" of the base path
        is a file, this is handled by the two public wrappers. Since we're not calling them, the correct return is
        a single dot"*/
        Assert.assertEquals(path, ".")
    }

    @Test
    fun pathUtilTest3() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".")
    }

    @Test
    fun pathUtilTest4() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".")
    }

    @Test
    fun pathUtilTest5() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".")
    }

    @Test
    fun pathUtilTest6() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".")
    }

    @Test
    fun pathUtilTest7() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir")
    }

    @Test
    fun pathUtilTest8() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir")
    }

    @Test
    fun pathUtilTest9() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir")
    }

    @Test
    fun pathUtilTest10() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir")
    }

    @Test
    fun pathUtilTest11() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest12() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2" + File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest13() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2" + File.separatorChar)

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest14() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest15() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir3")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".." + File.separatorChar + "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest16() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some2" + File.separatorChar + "dir3")
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".." + File.separatorChar + ".." + File.separatorChar + "some" +
            File.separatorChar + "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest17() {
        val roots = File.listRoots()

        val basePath = File(roots[0].path)
        val relativePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "some" + File.separatorChar + "dir" + File.separatorChar + "dir2")
    }

    @Test
    fun pathUtilTest18() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir")
        val relativePath = File(roots[0].toString() + "some")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, "..")
    }

    @Test
    fun pathUtilTest19() {
        val roots = File.listRoots()

        val basePath = File(roots[0].toString() + "some" + File.separatorChar + "dir" +
            File.separatorChar + "dir2")
        val relativePath = File(roots[0].toString() + "some")

        val path = getRelativeFileInternal(basePath, relativePath)

        Assert.assertEquals(path, ".." + File.separatorChar + "..")
    }
}
