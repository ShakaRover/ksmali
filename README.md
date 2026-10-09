### About

smali/baksmali is an assembler/disassembler for the dex format used by dalvik, Android's Java VM implementation. The syntax is loosely based on Jasmin's/dedexer's syntax, and supports the full functionality of the dex format (annotations, debug info, line info, etc.)

**NOTE**: This repository is [ShakaRover/ksmali](https://github.com/ShakaRover/ksmali), a fork of [google/smali](https://github.com/google/smali). The latter was forked from https://github.com/JesusFreke/smali for patches needed by Google, as the original repository was no longer maintained; that fork changed the namespace from `org.jf` to `com.android.tools.smali`, and this fork keeps that namespace so the code stays source- and binary-compatible with the 3.0.x artifacts. The 3.0.x artifacts are Google's releases, published on [Google Maven](https://maven.google.com) under the following coordinates:

* [`com.android.tools.smali:smali:<version>`](https://maven.google.com/web/index.html?q=smali#com.android.tools.smali:smali)
* [`com.android.tools.smali:smali-dexlib2:<version>`](https://maven.google.com/web/index.html?q=smali-dexlib2#com.android.tools.smali:smali-dexlib2)
* [`com.android.tools.smali:smali-baksmali:<version>`](https://maven.google.com/web/index.html?q=smali-baksmali#com.android.tools.smali:smali)
* [`com.android.tools.smali:smali-util:<version>`](https://maven.google.com/web/index.html?q=smali-util#com.android.tools.smali:smali-util)

After the fork the first version released was 3.0.0, which was version 2.5.2 from the original repo with a few patches and the namespace change. 3.0.10 is the last version Google released, and is where this repository picks up: the 4.x line was the ANTLR4 + Kotlin rewrite, and the current release is 5.0.0 (Kotlin-only, no Guava, single-pass front end — see the implementation notes below). The artifacts of this fork are **not** published to Google Maven; build them from source as described under [Building and testing](#building-and-testing).

#### Support
- [github Issue tracker](https://github.com/ShakaRover/ksmali/issues) - For any bugs/issues/feature requests

#### Some useful links for getting started with smali

- [Official dex bytecode reference](https://source.android.com/devices/tech/dalvik/dalvik-bytecode.html)
- [Registers wiki page](https://github.com/JesusFreke/smali/wiki/Registers)
- [Types, Methods and Fields wiki page](https://github.com/JesusFreke/smali/wiki/TypesMethodsAndFields)
- [Official dex format reference](https://source.android.com/devices/tech/dalvik/dex-format.html)

### Building and testing

All building and testing should be done using a version of OpenJDK 11. Newer OpenJDK versions are currently not supported due to issues with some of the tools used in the build process.

#### Implementation notes (this fork)

* **Kotlin only.** Every module is Kotlin; there are no Java sources left. The only Java in the
  tree is the ANTLR-generated lexer/parser, which is produced at build time from the grammars
  under `third_party/smali/src/main/antlr/`.
* **Single-pass front end.** The smali front end is one ANTLR4 grammar (`smaliParser.g4`). Earlier
  versions built an explicit AST and ran a second grammar (`smaliTreeWalker.g4`) over a flattened
  node stream; that walker and its AST were removed, and the semantic actions now run directly in
  the parser rules. As a result, `SmaliOptions.printTokens` still dumps the lexer tokens but no
  longer dumps an AST `toStringTree()` (there is no AST any more).
* **Coroutines.** Parallel assembly/disassembly uses `kotlinx.coroutines`: the work is fanned out
  with `supervisorScope { async(Dispatchers.Default.limitedParallelism(jobs)) { ... } }`. The
  `-j/--jobs` option caps the worker count (values below 1 are treated as 1) and only affects
  throughput, never the bytes. `Smali.assemble`/`Baksmali.disassembleDexFile` are blocking
  wrappers around `assembleSuspend`/`disassembleDexFileSuspend`, which callers that already run
  in a coroutine should call directly (the two produce byte-identical output).

#### Building
```
./gradlew assemble
```
#### Command Line Version

To run the `smali` and `baksmali` tools from the command line build the fat
jars. A fat jar is named `<tool>-<version>-fat.jar` after `version` in
`build.gradle` (no git hash, e.g. `smali-5.0.0-fat.jar`) and can be invoked with
`java -jar`.
```
./gradlew :smali:fatJar :baksmali:fatJar --offline -x proguard
java -jar smali/build/libs/smali-x.y.z-fat.jar
```

#### Testing

To execute all tests run
```
./gradlew test --offline
```

The build is self-contained and works offline once the dependencies are in the Gradle cache; the
same is true for the fat jars above. `clean test --offline --rerun-tasks` forces a full rerun.

#### Testing Maven Release
Push a release version to your local maven repository (add
`-Dmaven.repo.local=<dir>` to override the default local maven repository
location)
```
./gradlew release publishToMavenLocal
```

### Releasing

This section describes the release process of the upstream google/smali repository and is kept for reference: it applies to the 3.0.x artifacts on [Google Maven](https://maven.google.com), not to this fork's own releases. Releasing here means bumping `version` in `build.gradle`, committing, and pushing a tag for that commit: the [`Release` workflow](.github/workflows/release.yml) then checks that the tag equals `version`, runs the tests, builds the fat jars and creates a GitHub release with them attached. For an existing tag (the workflow file is not in older tags) run it manually:

```
gh workflow run release.yml -f tag=5.0.0
```

Building release versions and releasing to [Google Maven](https://maven.google.com) use Google infrastructure and support scripts maintained as part of the [R8](https://r8.googlesource.com/r8/) repository. The tasks below can only be performed by Google employees.

#### Prepare and build a release version
To prepare a release update `build.gradle` with the next release version and commit that.
Then create a tag for that commit with the version.
```
git tag <version> <commit>
git push origin <version>
```
Release versions can then be built by the Google R8 team using:
```
tools/trigger.py --smali=<version> --release
```
in the R8 repository.

The status of the build on the bot is at https://ci.chromium.org/p/r8/builders/ci/smali.

#### Releasing to Google Maven

When a release version has been built on the bot, it can be released to [Google Maven](https://maven.google.com) by running 
```
tools/release_smali.py --version=<version>
```
in the R8 repository. This kick off an internal Google approval process to finalize the release.
