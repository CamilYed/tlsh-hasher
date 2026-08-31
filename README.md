# TLSH Hasher

[![CI](https://github.com/CamilYed/tlsh-hasher/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/tlsh-hasher/actions/workflows/ci.yml)
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
- [Installation](#installation)
- [Usage](#usage)
- [How it works](#how-it-works)
- [Input eligibility](#input-eligibility)
- [Compatibility](#compatibility)
- [Build and verification](#build-and-verification)
- [Project](#project)

## Status

Functional and compatibility-tested, but not published yet. This is currently a home-lab project,
not a claim of production maturity. It will continue to evolve as the implementation is measured,
reviewed, and extended. The public API can hash byte arrays, streams, and files, parse canonical
digests, and calculate TLSH difference scores. Version `0.1.0` is being prepared as the first public
release, so API changes are still possible.

## Requirements

- JDK 25 (the Gradle toolchain also requests Java 25)
- no globally installed Gradle; use the committed wrapper

## Installation

The planned Maven coordinates are:

```kotlin
dependencies {
    implementation("io.github.camilyed:tlsh-hasher:0.1.0")
}
```

They will become usable after the first Maven Central release. Until then, build and publish the
current source to your local Maven repository:

```shell
git clone https://github.com/CamilYed/tlsh-hasher.git
cd tlsh-hasher
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to the consuming build and use version `0.1.0-SNAPSHOT`.

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
int distanceWithoutLength = first.distanceTo(second, false);
```

The difference is a nonnegative score, not a percentage or probability. Smaller values indicate
greater similarity. Interpretation depends on the data and use case, so this library does not label
one universal score as "similar" or "different."

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

Compatibility tests pin complete digest strings and difference scores generated by the reference
TLSH 5.0.0 implementation. They cover deterministic inputs from 256 through 65,536 bytes and both
distance modes. The supported format is the 128-bucket, one-byte-checksum `T1` variant.

## Build and verification

```shell
./gradlew clean check
```

Tests use JUnit 6 and AssertJ. `check` also verifies Spotless formatting and strict Javadoc. Generated
API documentation is available under `build/docs/javadoc`.

Create the versioned JAR, source JAR, Javadoc JAR, POM, and a Maven-style staging repository under
`build/staging-deploy`:

```shell
./gradlew -PreleaseVersion=0.1.0 clean build publishAllPublicationsToLocalBuildRepository
```

The staged coordinates are `io.github.camilyed:tlsh-hasher:0.1.0`. Remote publication is not
configured yet; the build writes only inside this repository's `build` directory.

To apply formatting:

```shell
./gradlew spotlessApply
```

Java sources use Google Java Format, including its two-space block indentation. To make IntelliJ
IDEA's **Reformat Code** action produce the same result, install and enable the
`google-java-format` plugin. Spotless remains the repository's formatting source of truth.

## Project conventions

Names describe TLSH concepts rather than implementation mechanics. For example, `bucketIndex` is a
Pearson result and `bucketCounts` contains histogram frequencies.

Classes not designed for inheritance, write-once fields, parameters, and non-reassigned local
variables use `final`. This communicates intent and prevents accidental reassignment; it is not a
JIT performance guarantee.

## Project

- [CHANGELOG.md](CHANGELOG.md) records release contents.
- [CONTRIBUTING.md](CONTRIBUTING.md) explains development and release checks.
- Issues and design discussions will live in the GitHub repository after it is created.

This project is licensed under the [Apache License 2.0](LICENSE).
