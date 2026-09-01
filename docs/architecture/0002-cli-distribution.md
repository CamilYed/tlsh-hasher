# Architecture decision 0002: CLI and native distribution

Status: JVM CLI accepted; native executable packaging is planned but deferred.

Date: 2026-09-01

## Purpose

The command-line application makes the library useful from shell scripts and provides a concrete
consumer of its public API. It lives in the non-published `tlsh-cli` module so CLI dependencies and
presentation choices do not leak into the dependency-free hashing library.

The initial executable is deliberately small:

```text
tlsh hash FILE...
tlsh hash -
tlsh distance FIRST SECOND
tlsh distance --ignore-length FIRST SECOND
```

`hash` emits one stable line per successful input:

```text
T1...  path-or-
```

`distance` emits only the decimal score. Human explanations belong in help and diagnostics rather
than standard output, keeping successful output easy to pipe into other programs.

## Exit codes

| Code | Meaning |
| ---: | --- |
| 0 | Every requested operation succeeded. |
| 1 | Input data was invalid, ineligible for TLSH, or could not be read. |
| 2 | Command syntax or option parsing was invalid. |

Expected user errors are concise and do not print Java stack traces. Multiple files can be hashed in
one invocation; readable files still produce output when another file fails, and the final exit code
is one.

## CLI framework

The module uses Picocli 4.7.7 and its annotation processor. Picocli provides subcommands, generated
help, version handling, JPMS metadata, predictable parsing errors, and generated GraalVM reflection
configuration. The library module itself remains dependency-free. Picocli's module and coordinates
are documented in its [official API overview](https://picocli.info/apidocs-all/overview-summary.html).

## Distribution stages

### 1. JVM application

The Gradle Application plugin first produces launch scripts and archives containing the CLI, the
TLSH library, and Picocli. This is the portable reference distribution and remains useful even after
native binaries exist because it is easier to debug and supports every platform with JDK 25.

### 2. Native executable

GraalVM Native Image can compile Java bytecode and dependencies into a platform-specific executable.
This is different from a JVM AOT cache: a Native Image binary starts without a separately installed
JVM, while a JDK AOT cache still accelerates a JVM application.

Native compilation depends on a local platform toolchain and produces machine code for a target
platform. Release automation should therefore build and test on separate runners instead of
assuming one machine can create every binary. The intended initial matrix is:

- Linux x86-64 and arm64;
- macOS x86-64 and arm64; and
- Windows x86-64.

The exact matrix will follow available CI runners and tested demand. Linux may later offer a
mostly-static or musl-based static variant; GraalVM documents the different linking trade-offs in
its [official static executable guide](https://www.graalvm.org/dev/reference-manual/native-image/guides/build-static-executables/).

### 3. Release packaging

Native executables are not ready for distribution merely because they compile. A release also needs:

- a smoke test on the same operating system and architecture;
- SHA-256 checksums and provenance;
- archive naming that includes version, operating system, and architecture;
- code signing and macOS notarization where appropriate;
- a software bill of materials and third-party license notices; and
- installation documentation and shell-completion generation.

## Deferred commands

A file-to-file `compare` command, recursive directory processing, machine-readable JSON, threshold
decisions, and parallel hashing are intentionally deferred. Each changes output, resource use, or
failure semantics. They should be added from concrete workflows rather than making the first CLI
large before its basic contract is exercised.
