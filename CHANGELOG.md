# Changelog

All notable changes to this project are documented here. The format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow Semantic Versioning.

## [Unreleased]

### Added

- Calculate standard 128-bucket, one-byte-checksum `T1` digests.
- Accept complete byte arrays, incremental chunks, input streams, and filesystem paths.
- Parse and format canonical 72-character digest strings.
- Compare digests with or without the encoded-length contribution.
- Match pinned TLSH 5.0.0 digest and distance compatibility vectors.
- Publish a named Java module with source and Javadoc artifacts.
- Add non-published JMH benchmarks for byte-array, input-stream, and warm-cache path hashing.
- Add isolated JMH benchmarks for digest parsing and distance calculation.
- Add a named `distanceToIgnoringLength` API and an external-package consumer smoke test.

### Changed

- Remove per-window array allocations from the byte-processing hot path.
- Make distance calculation allocation-free without exposing mutable digest state.
