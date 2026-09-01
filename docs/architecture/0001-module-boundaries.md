# Architecture decision 0001: module boundaries

Status: accepted as a target design; physical module extraction is deferred.

Date: 2026-09-01

## Context

The project currently publishes no artifacts and keeps the complete scalar TLSH implementation in
one Java module. This is intentionally simple while compatibility, the public API, and performance
are being established. A future optimized implementation may use a different byte-processing
strategy while producing exactly the same standard `T1` digest.

A module boundary is an API promise, not merely a directory layout. Splitting too early would force
the project to publish implementation interfaces before a second implementation had proved which
operations those interfaces actually need. Conversely, leaving every concern together forever
would make an optional optimized implementation difficult to select and test independently.

## Current public API

Only three classes are public:

| Type | User responsibility | Current implementation coupling |
| --- | --- | --- |
| `TlshDigest` | Holds, parses, encodes, and compares an immutable `T1` digest. | Parsing, formatting, and distance helpers are package-private. |
| `TlshHasher` | Accepts incremental bytes and creates digest snapshots. | It directly owns the scalar `TlshAccumulator`. |
| `Tlsh` | Provides one-shot hashing and constructs a `TlshHasher`. | It assembles every scalar implementation component itself. |

All other classes are package-private. They describe Pearson mapping, the sliding window, histogram
accumulation, length encoding, quartiles, quantization, checksum calculation, digest assembly,
parsing, formatting, and distance calculation.

The current API is convenient for one implementation, but `Tlsh` and `TlshHasher` cannot move into
a neutral core unchanged: both directly depend on scalar implementation classes.

## Decision

Keep the current physical `tlsh-hasher` module until a second working implementation prototype
exists. In the meantime, treat the following layout as the reviewed target rather than immediately
creating empty modules and speculative interfaces.

```text
application
    |
    v
tlsh-core <-------------------+
    ^                         |
    |                         |
tlsh-scalar              tlsh-vector
portable implementation  optional optimized implementation

tlsh-benchmarks depends on core and every implementation being compared
```

### `tlsh-core`

This module owns format semantics that every correct implementation must share:

- the immutable `TlshDigest` value;
- canonical `T1` parsing and formatting;
- difference-score calculation;
- input-independent public contracts for starting and updating a hash calculation; and
- shared validation that is part of the supported digest format.

Its likely JPMS name is `io.github.camilyed.tlsh.core`, exporting the
`io.github.camilyed.tlsh` package. Parser, formatter, and distance helpers remain internal rather
than becoming public merely because they move into a separate artifact.

### `tlsh-scalar`

This module owns the current portable implementation:

- sliding-window and triplet extraction;
- Pearson mapping and checksum accumulation;
- histogram storage, quartiles, quantization, and packing;
- length encoding and digest eligibility;
- digest assembly; and
- the concrete incremental hashing state.

Its likely JPMS name is `io.github.camilyed.tlsh.scalar`. Public selection code belongs in a distinct
`io.github.camilyed.tlsh.scalar` package; internal algorithm classes belong in an unexported package.
This avoids a split package, because JPMS does not allow the same package to be supplied by both core
and scalar modules.

### Optional optimized implementation

An optimized module must implement the same hashing contract and pass the complete compatibility
suite. It may change internal data layout or use optional JDK APIs, but it must not duplicate or
redefine digest parsing, formatting, or distance semantics. It earns a separate module only when it
contains a real implementation with measured value, not an empty placeholder.

## Explicit implementation selection

Selection remains visible in application code. The intended shape is similar to:

```java
TlshAlgorithm algorithm = ScalarTlsh.create();
TlshDigest digest = algorithm.hash(input);

TlshHasher hasher = algorithm.newHasher();
hasher.update(firstChunk);
hasher.update(secondChunk);
TlshDigest streamedDigest = hasher.finish();
```

The names `TlshAlgorithm` and `ScalarTlsh` are provisional until validated by a second
implementation. The important decision is the direction of dependency: core declares the neutral
contract, an implementation supplies it, and the application chooses that implementation.

The first release will not use `ServiceLoader`, classpath scanning, reflection, or a hidden global
default. Those mechanisms make selection dependent on runtime packaging and make it harder to see
which implementation a test or benchmark exercises. A scalar artifact may depend transitively on
core for Gradle and declare `requires transitive` for JPMS, so ordinary users need not manually
repeat a core dependency merely to access returned digest types.

## Existing class assignment

| Target | Existing classes or responsibilities |
| --- | --- |
| Core public | `TlshDigest`; future neutral hashing and incremental-hasher contracts |
| Core internal | `TlshDigestParser`, `TlshDigestFormatter`, `TlshDistanceCalculator` |
| Scalar public | future explicit scalar factory replacing the construction role of `Tlsh` |
| Scalar internal | `TlshAccumulator`, `SlidingWindow`, `BucketMapper`, `PearsonHash`, `TlshPearsonPermutation`, `ChecksumAccumulator`, `Histogram`, `HistogramQuartileCalculator`, `HistogramQuartiles`, `HistogramQuantizer`, `HistogramCodePacker`, `LengthEncoder`, `QuartileRatioEncoder`, `TlshDigestAssembler`, `TlshDigestEligibilityChecker`, and the concrete state currently inside `TlshHasher` |
| Benchmarks | `TlshHashBenchmark`, `TlshPathBenchmark`, `TlshDigestBenchmark`, and benchmark-only input generation |

The current `Tlsh` facade crosses the future boundary because it combines implementation-neutral
convenience methods with scalar construction. The current concrete `TlshHasher` crosses it because
its public update API is neutral but its field and constructor are scalar. These two types require
the most careful migration and are the reason a mechanical source move is not yet appropriate.

## Validation gates before extraction

Physical modules can be created when all of the following are true:

1. A second implementation prototype calculates the pinned deterministic and official corpus
   digests exactly.
2. Both implementations can be exercised through one proposed neutral contract without exposing
   scalar-only concepts such as buckets, triplets, or Pearson salts.
3. Benchmarks demonstrate a reason to retain the second implementation.
4. One-shot `byte[]`, `InputStream`, `Path`, and incremental usage remain convenient.
5. The module-path integration test proves that an application can explicitly select each
   implementation without split packages or reflection.
6. Sources, Javadoc, POM metadata, and JPMS descriptors are correct for every publishable artifact.

## Consequences

The project avoids extra artifacts and premature abstractions today. A future split will involve a
deliberate pre-release API change around construction rather than pretending that the current static
facade is implementation-neutral. Digest text and distance behavior can remain stable because they
already belong conceptually to core.

Consumers will be able to see which implementation they selected, tests will instantiate that same
choice explicitly, and benchmarks will compare implementations through a shared public contract.
The cost is that module extraction waits for evidence from a real second implementation instead of
being completed immediately.
