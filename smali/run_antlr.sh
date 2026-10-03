#!/bin/bash
#
# Regenerates the ANTLR4 lexer (smaliLexer.java), parser (smaliParser.java) and tree walker
# (smaliTreeWalker.java) from the .g4 grammars.
#
# ANTLR 4.13.2 has no Kotlin target, so the generated sources are Java. The smali module is
# Kotlin-only, so the generated Java is deliberately NOT written into src/main/kotlin (nor into
# the removed src/main/java). Instead:
#
#   * By default this script only runs the Gradle generation task. The generated Java stays in
#     smali/build/generated-src/antlr/main/... , a build directory that is never checked in.
#
#   * With --install it additionally copies the generated Java into the clearly marked,
#     non-source directory consumed by smali/Android.bp in AOSP:
#
#         smali/src/main/antlr-generated/com/android/tools/smali/smali/
#
#     AOSP/Soong cannot run Gradle or the ANTLR4 tool, so the generated Java has to be checked
#     in there. After changing a grammar, run:
#
#         bash smali/run_antlr.sh --install
#         git add smali/src/main/antlr-generated
#
# The script can be run from anywhere (it does not depend on the current working directory).

set -euo pipefail

INSTALL=false
if [ "${1:-}" = "--install" ]; then
  INSTALL=true
elif [ "$#" -gt 0 ]; then
  echo "Usage: $0 [--install]" >&2
  exit 2
fi

# Get the location of this script used to find locations of other things in the tree.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

GENERATED_DIR="$SCRIPT_DIR/build/generated-src/antlr/main/com/android/tools/smali/smali"
INSTALL_DIR="$SCRIPT_DIR/src/main/antlr-generated/com/android/tools/smali/smali"
GENERATED_FILES=(smaliLexer.java smaliParser.java smaliTreeWalker.java)

cd "$REPO_DIR"
./gradlew :smali:generateGrammarSource

for file in "${GENERATED_FILES[@]}"; do
  if [ ! -f "$GENERATED_DIR/$file" ]; then
    echo "ERROR: expected generated file not found: $GENERATED_DIR/$file" >&2
    exit 1
  fi
done

echo "Generated ANTLR sources: $GENERATED_DIR"

if [ "$INSTALL" = true ]; then
  mkdir -p "$INSTALL_DIR"
  for file in "${GENERATED_FILES[@]}"; do
    echo "Copying $file -> $INSTALL_DIR"
    cp "$GENERATED_DIR/$file" "$INSTALL_DIR/$file"
    # delete trailing whitespace to make gerrit happy
    sed -i 's/[ ]*$//' "$INSTALL_DIR/$file"
  done
  echo "DONE (remember to check in smali/src/main/antlr-generated/)"
else
  echo "Not copying into the source tree (the default keeps the tree Java-free)."
  echo "Run 'bash smali/run_antlr.sh --install' to update"
  echo "smali/src/main/antlr-generated/ for the AOSP/Soong build."
  echo "DONE"
fi
