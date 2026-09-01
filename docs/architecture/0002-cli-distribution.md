# Architecture decision 0002: CLI and native distribution

Status: JVM CLI accepted; native executable packaging is planned but deferred.

Date: 2026-09-01

## Purpose

The command-line application makes the library useful from shell scripts and provides a concrete
consumer of its public API. It lives in the non-published `tlsh-cli` module so CLI dependencies and
presentation choices do not leak into the dependency-free hashing library.

The executable keeps a small command vocabulary while supporting both human and batch workflows:

```text
tlsh
tlsh hash PATH...
tlsh hash [--recursive] [--include-hidden] [--progress=auto|always|never] PATH...
tlsh hash -
tlsh similar [--recursive] [--max-distance=N] [--max-comparisons=N] DIRECTORY
tlsh compare [--ignore-length] FIRST_FILE SECOND_FILE
tlsh distance FIRST SECOND
tlsh distance --ignore-length FIRST SECOND
```

`hash` emits one stable line per successful input:

```text
T1...  path-or-
```

`similar` emits a distance followed by two paths for every matching pair. `compare` and `distance`
emit only the decimal score. The former calculates both digests from files, while the latter accepts
existing digest strings. Human explanations belong in guided presentation, help, and diagnostics
rather than standard output, keeping successful output easy to pipe into other programs.

## Exit codes

| Code | Meaning |
| ---: | --- |
| 0 | Every requested operation succeeded. |
| 1 | Input data was invalid, ineligible for TLSH, or could not be read. |
| 2 | Command syntax or option parsing was invalid. |

Expected user errors are concise and do not print Java stack traces. Multiple files can be hashed in
one invocation; readable files still produce output when another file fails, and the final exit code
is one.

## Human and machine output

No-argument execution starts a persistent guided session only when a real terminal is attached.
Single-file hashing, folder hashing, and digest comparison are separate actions. A file workflow
therefore never asks about directory recursion, while folder mode previews scope, file count, and
combined size before starting. Paths accept quoting, the conventional `~` home prefix, and the
backslash escaping produced when dragging a path into common macOS terminals. Path prompts provide
normal line editing, history, and Tab completion for filesystem entries. After an operation, the
menu remains available until the user explicitly exits. Closing this persistent menu is itself a
successful action even when an earlier operation reported an ineligible file.

The menu delegates to the same parsed commands as explicit invocation instead of maintaining a
second hashing path. In a redirected process, no-argument execution prints help and exits so an
unattended process never waits for a prompt.

Files from a directory are sorted before hashing. Nested directories require an explicit
`--recursive` option, and symbolic directories are not followed. Hidden files and subdirectories
are skipped by default, with the skipped count included in previews and summaries;
`--include-hidden` opts into them. An explicitly named hidden file remains explicit input and is not
silently discarded. These rules make the scope visible and prevent accidental cycles. Duplicate
filesystem paths are hashed once.

Successful digests and numeric distances remain on standard output. The live progress line,
diagnostics, and batch summary use standard error. Consequently, presentation can evolve without
changing data captured by a pipe. Progress defaults to `auto`, can be forced for an IDE with
`always`, and can be disabled with `never`. Rendering is rate-limited and observes byte reads from
the existing streaming API, so it does not retain complete files in memory. When part of a batch
fails, successful digest lines are still emitted and the final diagnostic repeats failed paths and
reasons after the summary instead of leaving them hidden in the preceding output.

## CLI framework

The module uses Picocli 4.7.7 and its annotation processor. Picocli provides subcommands, generated
help, version handling, JPMS metadata, predictable parsing errors, and generated GraalVM reflection
configuration. JLine 4.3.1 owns interactive terminal details: editable input, history, filesystem
completion, terminal capability detection, and restoration of terminal state on exit. The Java FFM
terminal provider is selected at runtime on the Java 25 baseline. The library module itself remains
dependency-free. See Picocli's
[official API overview](https://picocli.info/apidocs-all/overview-summary.html) and JLine's
[LineReader documentation](https://jline.org/docs/line-reader/).

Guided menu entries implement a small Command contract containing their key, description, aliases,
and behavior. The shell renders and selects commands without knowing workflow-specific classes.
Path completion uses a Strategy selected by the active prompt: file prompts offer files and
directories, while folder prompts offer directories only. Picocli commands and JLine actions are
input adapters over shared hashing, file-comparison, and similarity-scan use cases; guided workflows
do not invoke the Picocli parser recursively.

Similarity scanning hashes each usable file once, keeps only its small immutable digest, and then
visits indices `i < j`. This avoids re-reading files and guarantees that self-pairs and reversed
duplicates are absent. Results are ordered by distance and then path. The maximum distance is
inclusive and defaults to zero. Because there are `n * (n - 1) / 2` pairs, discovery calculates the
cost before file contents are read and refuses more than 1,000,000 comparisons unless an explicit
command raises the guardrail.

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

Machine-readable JSON, clustering of overlapping similar pairs, and parallel hashing are
intentionally deferred. These features should be added from concrete workflows rather than making
the CLI large before its current contract is exercised.
