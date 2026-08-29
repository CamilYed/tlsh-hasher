# TLSH Hasher

An educational Java implementation of TLSH, developed incrementally from an understanding of the
algorithm rather than by translating an existing implementation.

The project currently contains only its build foundation. The TLSH implementation will be added in
small, test-driven learning exercises.

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
