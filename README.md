# TLSH Hasher

[![CI](https://github.com/CamilYed/tlsh-hasher/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/tlsh-hasher/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_tlsh-hasher&metric=alert_status)](https://sonarcloud.io/summary/overall?id=CamilYed_tlsh-hasher)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_tlsh-hasher&metric=coverage)](https://sonarcloud.io/summary/overall?id=CamilYed_tlsh-hasher)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.camilyed/tlsh-hasher?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.camilyed/tlsh-hasher)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Status](https://img.shields.io/badge/status-functional%20%2F%20pre--release-orange.svg)](#status)

An educational Java implementation of TLSH (Trend Locality Sensitive Hash), developed as a home
lab for learning the algorithm from first principles and experimenting with clear Java API design.
Unlike a cryptographic hash, TLSH is designed for similarity comparisons: related inputs usually
produce a lower difference score than unrelated inputs. A score of zero means that two TLSH digests
are identical; it is not proof that the original byte sequences are identical.

The implementation favors small classes, descriptive names, tests, and beginner-friendly Javadoc.
It currently supports the standard versioned `T1` representation with 128 effective histogram
buckets and a one-byte checksum.

## Contents

- [Status](#status)
- [Requirements](#requirements)
- [Usage](#usage)
- [Command-line application](#command-line-application)
- [Complete CLI guide](docs/cli.md)
- [How it works](#how-it-works)
- [Input eligibility](#input-eligibility)
- [Compatibility](#compatibility)
- [Build and verification](#build-and-verification)
- [Performance experiments](#performance-experiments)
- [Project](#project)

## Status

Functional and compatibility-tested, but not published yet. This is currently a home-lab project,
not a claim of production maturity. It will continue to evolve as the implementation is measured,
reviewed, and extended. The public API can hash byte arrays, streams, and files, parse canonical
digests, and calculate TLSH difference scores. Publication is deliberately deferred while
compatibility, performance, and future module boundaries are explored, so API changes are still
possible.

## Requirements

- JDK 25 (the Gradle toolchain also requests Java 25)
- no globally installed Gradle; use the committed wrapper

## Usage

Hash a complete byte array:

```java
byte[] input = readBytes();
TlshDigest digest = Tlsh.hash(input);
String encoded = digest.encoded();
```

Process data incrementally without retaining the complete input:

```java
TlshHasher hasher = Tlsh.newHasher();
hasher.update(firstChunk);
hasher.update(secondChunk, offset, length);
TlshDigest digest = hasher.finish();
```

The boundaries between chunks do not affect the result. `finish()` returns an immutable snapshot;
it does not reset or close the hasher.

Parse and compare encoded digests:

```java
TlshDigest first = TlshDigest.parse(firstEncodedDigest);
TlshDigest second = TlshDigest.parse(secondEncodedDigest);

int distance = first.distanceTo(second);
int distanceWithoutLength = first.distanceToIgnoringLength(second);
```

The difference is a nonnegative score, not a percentage or probability. Smaller values indicate
greater similarity. Interpretation depends on the data and use case, so this library does not label
one universal score as "similar" or "different."

TLSH stores the input's exact byte count only as a compact approximate size range. The normal
`distanceTo` score includes a penalty when two digests represent different ranges.
`distanceToIgnoringLength` removes only that size penalty; checksum, quartile ratios, and the
local-pattern histogram are still compared.

## Command-line application

The non-published `tlsh-cli` module provides a named JPMS command-line application. Build its local
JVM distribution with:

```shell
./gradlew :tlsh-cli:installDist
```

The generated launcher is `tlsh-cli/build/install/tlsh/bin/tlsh` on Unix-like systems and
`tlsh.bat` in the same directory on Windows. The [complete CLI guide](docs/cli.md) explains every
command, option, interactive question, output channel, and exit code.

```shell
tlsh hash file.bin another-file.bin
tlsh hash samples/
tlsh hash --recursive --progress=always samples/
tlsh hash --recursive --include-hidden samples/
cat file.bin | tlsh hash -
tlsh similar samples/
tlsh similar --recursive --max-distance=100 samples/
tlsh compare first.bin second.bin
tlsh compare --ignore-length first.bin second.bin
tlsh distance T1_FIRST_DIGEST T1_SECOND_DIGEST
tlsh distance --ignore-length T1_FIRST_DIGEST T1_SECOND_DIGEST
```

Running an installed `tlsh` launcher without arguments opens a guided session when a human terminal
is attached. Single-file and folder hashing are separate actions, so a file never triggers a
question about nested directories. Paths may be relative, home-relative with `~`, quoted, pasted,
or dragged from a graphical file manager into the terminal. Path prompts also support line editing,
command history, and filesystem suggestions with Tab. Folder mode previews the number and combined
size of selected files before starting. Similar-file mode previews both the file count and the
number of unique pairs before starting. After every operation the menu remains open until the user
chooses Exit. When standard streams are redirected, no-argument execution prints help and exits
instead of waiting for input. This keeps pipes, IDE builds, and CI jobs safe.

`hash` accepts files and directories. A directory includes its immediate regular files;
`--recursive` also includes nested directories. Hidden entries are skipped during directory
discovery by default, and the summary reports how many were skipped. Use `--include-hidden` to hash
hidden files and descend into hidden directories. A hidden file supplied explicitly is always
processed. Directory files are sorted for reproducible output, and overlapping arguments are
de-duplicated. `--progress=auto` displays one live aggregate progress line only in a terminal. Use
`--progress=always` for an IDE output window or `--progress=never` for fully quiet batch execution.
Progress and batch summaries use standard error.

If one item fails, hashing continues for the remaining files. The final summary gives the successful
and attempted counts, then repeats failed paths and their reasons in a separate colored section so a
failure cannot disappear among many successful digest lines. An explicitly closed guided session
still exits successfully; an explicit `tlsh hash ...` invocation returns exit code `1` when any
requested item fails.

`compare` hashes two regular files and prints their numeric TLSH distance. `distance` performs the
same calculation from two existing canonical digests. Both accept `--ignore-length`. The guided
file comparison additionally displays the complete digests and reminds the user that the result is
neither a percentage nor proof of byte-for-byte equality.

`similar` hashes each discovered file once and compares every unique digest pair. It prints
`DISTANCE  FIRST_PATH  SECOND_PATH`, ordered by distance and then path. The default maximum distance
is zero; choose a threshold deliberately for a particular data set. Because an all-pairs scan grows
as `n * (n - 1) / 2`, the command refuses more than 1,000,000 comparisons unless
`--max-comparisons` is raised explicitly. Its terminal progress has separate `Hashing files` and
`Comparing digests` phases; `--progress=auto|always|never` follows the same policy as `hash`.

Standard output remains a stable data stream: hash output contains the canonical digest, two
spaces, and the input name; similar output contains a distance and two paths; and compare and
distance output contain only the decimal score. It can therefore be redirected or piped without
collecting progress presentation. `--no-summary`
suppresses the folder or multi-file summary as well. The current application is a JVM distribution; the
[CLI distribution decision](docs/architecture/0002-cli-distribution.md) describes the later,
separately tested path to native executables for specific platforms.

## How it works

```text
input bytes
    |
five-byte sliding windows
    |
six local triplets per full window
    |
Pearson mapping into feature buckets
    |
128 effective bucket frequencies
    |
quartiles and two-bit quantization
    |
checksum + length + quartile ratios + packed histogram
    |
72-character T1 digest
```

TLSH preserves a compact description of local-pattern frequency rather than the original bytes.
That is why nearby digests can suggest similar content, while neither equal digests nor a distance
of zero prove that the original inputs were byte-for-byte identical.

## Input eligibility

The standard API creates a digest only when:

- the input contains at least 256 bytes and no more than 4,224,281,216 bytes; and
- at least 65 of the 128 effective feature buckets are nonempty.

`finish()` throws `IllegalStateException` when these requirements are not met. The second rule
rejects low-information inputs whose repeated local patterns would not produce a meaningful
similarity digest.

Hash a file directly. The library opens and closes the file stream itself:

```java
TlshDigest digest = Tlsh.hash(path);
```

When an existing `InputStream` is supplied, the library reads it incrementally but leaves ownership
of closing it with the caller:

```java
try (InputStream input = Files.newInputStream(path)) {
  TlshDigest digest = Tlsh.hash(input);
}
```

## Compatibility

### What `T1` means

`T1` is a version prefix for the standard TLSH digest representation. It is not a similarity score
and does not mean that the original input has been stored or can be reconstructed. The
[official TLSH documentation](https://github.com/trendmicro/tlsh/blob/5.0.0/README.md) defines the
standard `T1` configuration as:

- 128 effective histogram buckets, quantized to two bits each and packed into 32 bytes; and
- a one-byte rolling checksum.

Together with the encoded length and quartile ratios, these fields form 35 binary bytes. Each byte
becomes two hexadecimal characters, giving 70 hexadecimal characters, and the `T1` prefix makes the
canonical text 72 characters long.

This library generates and parses exactly that representation. It deliberately does not currently
support the legacy 70-character form without `T1`, builds with 256 effective buckets, three-byte
checksums, or other nonstandard build configurations supported by the reference project.

### Is this a complete TLSH implementation?

For eligible inputs in the supported `T1`, 128-bucket, one-byte-checksum configuration, the hashing,
encoding, parsing, and distance algorithms are compatible with reference TLSH 5.0.0 results. In
that precise sense this is a real TLSH implementation, not an approximation inspired by TLSH.

It is not yet a drop-in replacement for every reference-library mode or API. Most importantly, this
project always applies the conservative minimum input length of 256 bytes. Reference TLSH also has
a default mode that accepts sufficiently varied inputs from 50 bytes and a conservative option that
requires 256 bytes. This project throws an exception for ineligible input instead of returning the
reference command-line/Python sentinel `TNULL`.

### Where the compatibility tests live

The tests themselves are part of this repository:

- [`TlshCompatibilityTest`](src/test/java/io/github/camilyed/tlsh/TlshCompatibilityTest.java) pins
  complete digests for deterministic inputs from 256 through 65,536 bytes;
- [`TlshDistanceTest`](src/test/java/io/github/camilyed/tlsh/TlshDistanceTest.java) pins official
  scores with and without the encoded-length contribution; and
- [`OfficialTlshCorpusCompatibilityTest`](src/test/java/io/github/camilyed/tlsh/OfficialTlshCorpusCompatibilityTest.java)
  hashes real files from the official corpus and checks their complete digests and distance scores.

The deterministic tests always run locally and in CI. A shared test helper creates both in-memory
inputs and temporary files from a fixed algorithm, so those tests need neither committed binary
fixtures nor network access. The real corpus files are intentionally not copied into this
repository. The [CI workflow](.github/workflows/ci.yml) checks out the official
TLSH repository at the immutable `5.0.0` tag into its temporary workspace and supplies those files
to `OfficialTlshCorpusCompatibilityTest`. That test is skipped locally when the fixture-directory
property is absent, while CI always configures it.

## Build and verification

```shell
./gradlew clean check
```

Tests use JUnit 6 and AssertJ. `check` also verifies Spotless formatting and strict Javadoc. Generated
API documentation is available under `build/docs/javadoc`.

To apply formatting:

```shell
./gradlew spotlessApply
```

Java sources use Google Java Format, including its two-space block indentation. To make IntelliJ
IDEA's **Reformat Code** action produce the same result, install and enable the
`google-java-format` plugin. Spotless remains the repository's formatting source of truth.

## Performance experiments

The non-published [`tlsh-benchmarks`](tlsh-benchmarks/README.md) module contains JMH benchmarks. Its
first baseline measures complete `Tlsh.hash(byte[])` operations for deterministic inputs of several
sizes:

```shell
./gradlew :tlsh-benchmarks:jmh
```

The default configuration is intentionally short and suitable for development checks, not for
publishing performance claims. The benchmark documentation describes a longer run and allocation
measurement. The first recorded experiment documents the
[2026-08-31 allocation hot-path refactor](tlsh-benchmarks/results/2026-08-31-hot-path-refactor.md).

## Project conventions

Names describe TLSH concepts rather than implementation mechanics. For example, `bucketIndex` is a
Pearson result and `bucketCounts` contains histogram frequencies.

Classes not designed for inheritance, write-once fields, parameters, and non-reassigned local
variables use `final`. This communicates intent and prevents accidental reassignment; it is not a
JIT performance guarantee.

## Project

- [CHANGELOG.md](CHANGELOG.md) records release contents.
- [CONTRIBUTING.md](CONTRIBUTING.md) explains development and release checks.
- [ROADMAP.md](ROADMAP.md) describes compatibility, benchmark, and module work planned before a
  release.
- [Module boundaries](docs/architecture/0001-module-boundaries.md) records the reviewed public API,
  target modules, and the conditions for splitting the current implementation.
- [CLI distribution](docs/architecture/0002-cli-distribution.md) defines the command contract and
  staged path from a JVM application to platform-native executables.
- [Release readiness](docs/release-readiness.md) records local artifact checks and work deliberately
  left before publication.
- Issues and design discussions are welcome in the GitHub repository.

This project is licensed under the [Apache License 2.0](LICENSE).
