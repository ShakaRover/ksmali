/*
 * Copyright 2024, Google LLC
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

package com.android.tools.smali.smali;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles every smali fixture used by the on-device integration tests and the bundled examples.
 * This exercises the full ANTLR4 parser + tree walker pipeline across the whole smali syntax.
 */
public class AssembleIntegrationTest {

    private static final int MOST_RECENT_API = 30;

    private static final String[] FIXTURE_ROOTS = {
            "../smali-integration-tests/src/test/smali",
            "../examples",
    };

    // These fixtures are not valid input for the current smali front-end: assembling them fails
    // identically with the pre-migration ANTLR3 front-end. They are excluded from this check so
    // that it only reports regressions introduced by the parser/tree-walker itself.
    private static final String[] PRE_EXISTING_UNSUPPORTED = {
            "InstructionTests/Format21h/Format21h.smali",  // requires const/high16 literals to be shifted
            "AutofixTests/GotoTest.smali",                // uses unquoted .local names
            "AnnotationTests/AnnotationTests.smali",      // uses the .parameter directive
    };

    @Test
    public void assembleAllFixtures() throws Exception {
        List<File> fixtures = new ArrayList<File>();
        for (String root : FIXTURE_ROOTS) {
            File dir = new File(root);
            if (dir.isDirectory()) {
                collectSmaliFiles(dir, fixtures);
            }
        }

        Assume.assumeFalse("smali fixture directories not present", fixtures.isEmpty());

        int assembled = 0;
        List<String> failures = new ArrayList<String>();
        for (File fixture : fixtures) {
            if (isUnsupported(fixture)) {
                continue;
            }
            String text = new String(Files.readAllBytes(fixture.toPath()), StandardCharsets.UTF_8);
            try {
                SmaliTestUtils.compileSmali(text, MOST_RECENT_API);
                assembled++;
            } catch (Throwable ex) {
                java.io.StringWriter sw = new java.io.StringWriter();
                ex.printStackTrace(new java.io.PrintWriter(sw));
                failures.add(fixture.getPath() + ":\n" + sw);
            }
        }

        Assert.assertTrue("Failed to assemble " + failures.size() + " of " + fixtures.size()
                + " smali fixtures:\n" + join(failures), failures.isEmpty());
        Assert.assertTrue(assembled > 0);
    }

    private static boolean isUnsupported(File fixture) {
        String path = fixture.getPath().replace(File.separatorChar, '/');
        for (String unsupported : PRE_EXISTING_UNSUPPORTED) {
            if (path.contains(unsupported)) {
                return true;
            }
        }
        return false;
    }

    private static void collectSmaliFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectSmaliFiles(file, out);
            } else if (file.getName().endsWith(".smali")) {
                out.add(file);
            }
        }
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("  ").append(item).append('\n');
        }
        return sb.toString();
    }
}
