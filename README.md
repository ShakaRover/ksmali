### About

smali/baksmali is an assembler/disassembler for the dex format used by dalvik, Android's Java VM implementation. The syntax is loosely based on Jasmin's/dedexer's syntax, and supports the full functionality of the dex format (annotations, debug info, line info, etc.)

**NOTE**: This repository is [ShakaRover/ksmali](https://github.com/ShakaRover/ksmali), a fork of [google/smali](https://github.com/google/smali). The latter was forked from https://github.com/JesusFreke/smali for patches needed by Google, as the original repository was no longer maintained; that fork changed the Java package namespace from `org.jf` to `com.android.tools.smali`, and this fork keeps that namespace, so the type names match the 3.0.x artifacts (the Kotlin rewrite did move the top-level entry points — see [Using the libraries](#using-the-libraries)). The 3.0.x artifacts are Google's releases, published on [Google Maven](https://maven.google.com) under the following coordinates:

* [`com.android.tools.smali:smali:<version>`](https://maven.google.com/web/index.html?q=smali#com.android.tools.smali:smali)
* [`com.android.tools.smali:smali-dexlib2:<version>`](https://maven.google.com/web/index.html?q=smali-dexlib2#com.android.tools.smali:smali-dexlib2)
* [`com.android.tools.smali:smali-baksmali:<version>`](https://maven.google.com/web/index.html?q=smali-baksmali#com.android.tools.smali:smali)
* [`com.android.tools.smali:smali-util:<version>`](https://maven.google.com/web/index.html?q=smali-util#com.android.tools.smali:smali-util)

After the fork the first version released was 3.0.0, which was version 2.5.2 from the original repo with a few patches and the namespace change. 3.0.10 is the last version Google released, and is where this repository picks up: the 4.x line was the ANTLR4 + Kotlin rewrite, and the current release is 5.0.0 (Kotlin-only, no Guava, single-pass front end — see the implementation notes below). The artifacts of this fork are **not** on Google Maven: the libraries are on Maven Central as `io.github.shakarover.ksmali:<artifact>:<version>` (see [Using the libraries](#using-the-libraries)), and the ready-to-run CLI fat jars are attached to the [GitHub releases](https://github.com/ShakaRover/ksmali/releases).

#### Using the libraries

The four libraries are on Maven Central under the `io.github.shakarover.ksmali` group. Maven Central is a default repository of both Gradle and Maven, so no repository configuration and no credentials are needed:

| Artifact | Contents |
| --- | --- |
| `io.github.shakarover.ksmali:smali-dexlib2` | reading, modifying and writing dex files |
| `io.github.shakarover.ksmali:smali-baksmali` | disassembler; depends on dexlib2 and util |
| `io.github.shakarover.ksmali:smali` | assembler; depends on dexlib2 and util |
| `io.github.shakarover.ksmali:smali-util` | shared utilities |

```groovy
dependencies {
    implementation 'io.github.shakarover.ksmali:smali-baksmali:5.0.0'
}
```

```xml
<dependency>
  <groupId>io.github.shakarover.ksmali</groupId>
  <artifactId>smali-baksmali</artifactId>
  <version>5.0.0</version>
</dependency>
```

The Java package names are unchanged (`com.android.tools.smali.*`), but the Kotlin rewrite moved the
top-level entry points, so Java code written against the 3.0.x artifacts needs a small edit:

| 3.0.x | 5.x |
| --- | --- |
| `Smali.assemble(options, input)` | `SmaliKt.assemble(options, input)` |
| `Baksmali.disassembleDexFile(dexFile, dir, jobs, options)` | `BaksmaliKt.disassembleDexFile(dexFile, dir, jobs, options)` |
| `DexFileFactory.loadDexFile(file, opcodes)` | `DexFileFactory.INSTANCE.loadDexFile(file, opcodes)` |
| `Opcodes.forApi(api)` | unchanged |

Kotlin callers import the top-level functions directly, e.g. `com.android.tools.smali.baksmali.disassembleDexFile`;
callers that already run in a coroutine should use `assembleSuspend`/`disassembleDexFileSuspend`. Disassembling a dex file from Java, checked against the published `smali-baksmali:5.0.0`:

```java
import com.android.tools.smali.baksmali.BaksmaliKt;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.DexFile;

import java.io.File;

DexFile dexFile = DexFileFactory.INSTANCE.loadDexFile(new File("classes.dex"), Opcodes.forApi(20));
boolean ok = BaksmaliKt.disassembleDexFile(dexFile, new File("out"), 1, new BaksmaliOptions());
```

#### Support
- [github Issue tracker](https://github.com/ShakaRover/ksmali/issues) - For any bugs/issues/feature requests

#### Some useful links for getting started with smali

- [Official dex bytecode reference](https://source.android.com/devices/tech/dalvik/dalvik-bytecode.html)
- [Registers wiki page](https://github.com/JesusFreke/smali/wiki/Registers)
- [Types, Methods and Fields wiki page](https://github.com/JesusFreke/smali/wiki/TypesMethodsAndFields)
- [Official dex format reference](https://source.android.com/devices/tech/dalvik/dex-format.html)

### Building and testing

Building and testing works on OpenJDK 11 or newer; 11, 17 and 21 are what this fork is checked with
(470 tests pass on each). The sources are compiled against the Java 8 API
(`-Xjdk-release=1.8`, see `build.gradle`), so the libraries and the fat jars still run on a Java 8 JVM.

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

Ready-to-run fat jars are also attached to every [release](https://github.com/ShakaRover/ksmali/releases),
so building them is only needed for a local checkout.

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
location). This writes to the local repository only — remote publishing happens in CI, see below.
```
./gradlew release publishToMavenLocal
```

#### Publishing to Maven Central

The libraries are published to Maven Central (no authentication needed to consume them there)
through Sonatype's [Portal OSSRH Staging API](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/),
which the built-in `maven-publish` plugin uploads to directly. **Publishing only happens inside
GitHub Actions**: `build.gradle` registers the remote repository only when `GITHUB_ACTIONS=true` and
credentials are present, so a local `./gradlew publish` never touches a remote repository.

One-time setup, all on <https://central.sonatype.com>:

* the namespace `io.github.shakarover` verified (signing in with GitHub grants `io.github.<username>`
  automatically),
* a Central Portal *User Token* (Account → Generate User Token),
* a GPG signing key — Maven Central rejects unsigned artifacts.

They are stored as the `CENTRAL_USER`, `CENTRAL_TOKEN`, `SIGNING_KEY` (ASCII armored private key) and
`SIGNING_PASSWORD` repository secrets, and the [`Release` workflow](.github/workflows/release.yml)
uses them for every tag:

1. upload the signed artifacts —
   `./gradlew publish -PcentralUser=… -PcentralToken=… -PsigningKey=… -PsigningPassword=…`;
2. tell the staging service to push the deployment to the Portal (this call must come from the same
   IP as the upload, hence the same job):
   ```
   curl -X POST -H "Authorization: Bearer $(printf '%s:%s' "$USER" "$TOKEN" | base64 -w0)" \
       'https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/io.github.shakarover?publishing_type=automatic'
   ```
   `publishing_type=automatic` releases the deployment once validation passes; with `user_managed`
   (default) it waits for you at <https://central.sonatype.com/publishing> instead;
3. wait until the artifacts are anonymously fetchable from `repo1.maven.org` — that is the real gate,
   because the two steps above are `continue-on-error` (re-running an already published tag cannot
   succeed, Central versions are immutable).

### Releasing

This section describes the release process of the upstream google/smali repository and is kept for reference: it applies to the 3.0.x artifacts on [Google Maven](https://maven.google.com), not to this fork's own releases. Releasing here means bumping `version` in `build.gradle`, committing, and pushing a tag for that commit: the [`Release` workflow](.github/workflows/release.yml) then checks that the tag equals `version`, runs the tests, builds the fat jars, publishes the libraries to Maven Central (see above) and creates a GitHub release with the fat jars attached. For an existing tag (the workflow file is not in older tags) run it manually:

```
gh workflow run release.yml -f tag=5.0.0
```

For a tag whose tree predates the publish configuration (5.0.0 is one, its library sources are identical to the current ones) build from the current branch instead:

```
gh workflow run release.yml -f tag=5.0.0 -f checkout=main
```

The tag/version check still applies to whatever is checked out, so this cannot publish a version that does not match the tag.

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
