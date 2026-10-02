#!/bin/bash
#
# Regenerates the ANTLR4 parser (smaliParser.java) and tree walker (smaliTreeWalker.java)
# from the .g4 grammars, and copies them next to the other smali sources so that they can be
# checked into trees (e.g. AOSP) that do not run ANTLR themselves.
#
# Generation itself is delegated to the gradle build, which downloads and runs the ANTLR4 tool.
# The script can be run from anywhere (it does not depend on the current working directory).
#
# After making any changes to the parser or tree walker grammars, re-run this script and check in
# the generated source file(s).

set -euo pipefail

# Get the location of this script used to find locations of other things in the tree.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

GENERATED_DIR="$SCRIPT_DIR/build/generated-src/antlr/main/com/android/tools/smali/smali"
OUTPUT_DIR="$SCRIPT_DIR/src/main/java/com/android/tools/smali/smali"

cd "$REPO_DIR"
./gradlew :smali:generateGrammarSource

mkdir -p "$OUTPUT_DIR"
for file in smaliParser.java smaliTreeWalker.java; do
  echo "Copying $file -> $OUTPUT_DIR"
  cp "$GENERATED_DIR/$file" "$OUTPUT_DIR/$file"
  # delete trailing whitespace to make gerrit happy
  sed -i 's/[ ]*$//' "$OUTPUT_DIR/$file"
done

echo "DONE"
