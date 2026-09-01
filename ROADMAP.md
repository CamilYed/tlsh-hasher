# Roadmap

This home lab prioritizes understanding, compatibility evidence, and measurements before release.
The order below is deliberate: module boundaries and optimized implementations should follow data,
not assumptions about where the hot path might be.

## 1. Compatibility corpus

- Run generated deterministic vectors on every build.
- In CI, fetch the official TLSH repository at the immutable `5.0.0` tag instead of copying its
  example documents into this repository.
- Verify complete `T1` digests for real official files.
- Verify official distance scores with and without the encoded-length contribution.
- Extend coverage to boundary sizes, repetitive inputs, binary data, and controlled mutations.

## 2. Performance baseline

- [x] Add a non-published `tlsh-benchmarks` JMH module.
- [x] Measure complete hashing for `byte[]` inputs.
- [x] Measure complete hashing for `InputStream` inputs.
- [x] Measure warm-cache complete hashing for `Path` inputs.
- [x] Add isolated benchmarks for digest parsing and distance calculation.
- [x] Record a full digest-operation baseline with the same controlled JMH configuration as
  hashing.
- [x] Remove defensive-copy allocation from distance calculation without exposing the digest's
  mutable internal histogram, then compare it with the recorded baseline.
- Use deterministic generated inputs for repeatable sizes and selected official corpus files for a
  realistic secondary workload.
- Record throughput, average latency, and allocation for small, medium, and large inputs.
- Store environment and JVM metadata with results; do not make performance claims from one noisy
  run or from a benchmark that does not consume its result.

## 3. Module boundaries

Keep the current scalar implementation together until the public API and benchmark baseline are
stable. Then evaluate this shape:

- [x] Inventory the current public API and assign existing responsibilities to target modules.
- [x] Record an explicit implementation-selection design without service loading.
- Validate the proposed hashing interfaces against a second working implementation prototype before
  publishing them.
- Split the physical Gradle and JPMS modules only after that validation.

```text
tlsh-core
  public digest types, parsing, distance, and implementation contracts

tlsh-scalar
  portable baseline implementation using ordinary Java

tlsh-vector
  optional experimental implementation using the JDK Vector API

tlsh-benchmarks
  non-published JMH comparisons of scalar and experimental implementations
```

Consumers should eventually depend on `tlsh-core` plus one implementation. The selection mechanism
must remain explicit and testable; the project will not introduce an SPI or service loading before
there are at least two real implementations to select between.

## 4. Release readiness

- [x] Exercise the complete public API from a separate consumer package that cannot access
  package-private implementation classes.
- [x] Compile and run a separate named JPMS consumer module against the library during `check`.
- Stabilize the public API after compatibility and benchmark work.
- Review generated JAR, sources, Javadoc, POM, signatures, and module descriptor.
- Publish only after the home-lab status and known limitations are accurately documented.
- Create the Git tag and GitHub Release only after the matching artifact is actually available.

## 5. Command-line application

- [x] Add a separate named `tlsh-cli` module with generated help and version options.
- [x] Hash one or more files or standard input with stable line-oriented output.
- [x] Compare two encoded digests with or without the length contribution.
- [x] Add a file-to-file command with a numeric standard-output contract and a descriptive guided
  presentation.
- [x] Specify inclusive threshold semantics, pair-count limits, and deterministic output ordering,
  then add bounded directory-wide similar-file search that hashes each file once.
- Build JVM application archives and launch scripts before attempting native executables.
- Add GraalVM Native Image only with tests and per-platform CI runners; do not assume that one
  runner can produce every operating-system and architecture combination.
- Add checksums, provenance, signing, and platform-specific packaging before distributing binaries.
