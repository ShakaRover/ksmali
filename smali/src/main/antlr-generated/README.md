# Generated ANTLR4 sources

This directory is the checked-in home for the ANTLR4-generated Java sources used by the
`android-smali` Soong target (`smali/Android.bp`).

ANTLR 4.13.2 has no Kotlin target, so `smaliLexer.java`, `smaliParser.java` and
`smaliTreeWalker.java` are generated as Java from the `.g4` grammars in
`third_party/smali/src/main/antlr/`. The rest of the smali module is Kotlin.

Because AOSP/Soong cannot run Gradle (and the ANTLR4 tool is not shipped in the platform),
the generated Java has to be checked in here. It is intentionally kept out of the Gradle
source sets (`src/main/kotlin`, `src/main/java`) so the `smali` module stays Kotlin-only.

Regenerate and install after changing a grammar:

```sh
bash smali/run_antlr.sh --install
git add smali/src/main/antlr-generated
```

`run_antlr.sh` without `--install` only regenerates the files under
`smali/build/generated-src/antlr/main/` and leaves this directory untouched.
