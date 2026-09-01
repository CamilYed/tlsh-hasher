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
- [ ] Measure complete hashing for `Path` inputs.
- Measure digest parsing and distance calculation separately from hashing.
- Use deterministic generated inputs for repeatable sizes and selected official corpus files for a
  realistic secondary workload.
- Record throughput, average latency, and allocation for small, medium, and large inputs.
- Store environment and JVM metadata with results; do not make performance claims from one noisy
  run or from a benchmark that does not consume its result.

## 3. Module boundaries

Keep the current scalar implementation together until the public API and benchmark baseline are
stable. Then evaluate this shape:

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

- Stabilize the public API after compatibility and benchmark work.
- Review generated JAR, sources, Javadoc, POM, signatures, and module descriptor.
- Publish only after the home-lab status and known limitations are accurately documented.
- Create the Git tag and GitHub Release only after the matching artifact is actually available.
