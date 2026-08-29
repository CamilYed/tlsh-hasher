# TLSH Hasher

An educational Java implementation of TLSH, developed incrementally from an understanding of the
algorithm rather than by translating an existing implementation.

The project is developed in small, test-driven learning exercises. Its current readable pipeline
contains a sliding window, Pearson bucket mapping, a feature histogram, and streaming feature
accumulation.

## Requirements

- JDK 25 (the Gradle toolchain also requests Java 25)
- no globally installed Gradle; use the committed wrapper

## Build

```shell
./gradlew clean check
```

Tests use JUnit 6 on the JUnit Platform. `check` also verifies formatting and builds strict Javadoc.
API documentation is available after the build under `build/docs/javadoc`.

## IntelliJ IDEA formatting

Java sources use Google Java Format, including its two-space block indentation. For IntelliJ IDEA's
**Reformat Code** action to produce the same result as Spotless, install and enable the
`google-java-format` plugin. The repository's `.editorconfig` keeps the built-in editor settings
aligned for indentation, line endings, and trailing whitespace.

The Gradle formatter remains the source of truth:

```shell
./gradlew spotlessApply
```

## Java style

Names should describe TLSH domain meaning rather than implementation mechanics. For example, use
`bucketIndex` for a Pearson result and `bucketCounts` for histogram values.

Use `final` for classes that are not designed for inheritance, object fields that are assigned only
once, method and constructor parameters, and local variables that are not reassigned. A `final`
array reference cannot be replaced, but its elements remain mutable. Do not add redundant `final`
modifiers to methods declared inside a `final` class.

This convention communicates intent and prevents accidental reassignment. It is not treated as a
performance guarantee: the JVM JIT compiler performs its own runtime analysis and can optimize
code regardless of whether local variables are declared `final`.
