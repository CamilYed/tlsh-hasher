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
- Verify the public API from a separate named JPMS module during every build.
- Add a non-published CLI module for hashing files or standard input and comparing encoded digests.
- Add guided no-argument CLI use, directory discovery, recursive hashing, aggregate progress, and
  batch summaries without changing script-oriented standard output.
- Separate guided file and folder workflows, preview folder batches, normalize pasted paths, and
  keep the interactive session open across operations.
- Add terminal colors, line editing, history, and Tab filesystem completion with JLine.
- Collect per-file failures into a readable final batch report with successful and attempted counts.
- Compare two files directly in command and guided modes while retaining numeric script output.
- Skip hidden directory entries by default, report the skipped count, and add `--include-hidden`.
- Document every CLI command, option, guided question, output channel, and exit code in a dedicated
  user guide, with expanded generated help text.
- Find similar files in command and guided modes with deterministic pair ordering, inclusive
  distance thresholds, single-pass hashing, partial-failure reporting, and an all-pairs cost limit.
- Show separate byte-hashing and digest-comparison progress phases during similar-file scans.

### Changed

- Remove per-window array allocations from the byte-processing hot path.
- Make distance calculation allocation-free without exposing mutable digest state.
- Split the interactive CLI into focused menu, prompt, path, workflow, and reporting components.
- Model guided menu entries as commands, select path completion as a strategy, and share hashing and
  file-comparison use cases between Picocli and JLine adapters.
- Split the CLI integration suite by workflow and share only deterministic stream, input, and
  terminal fixtures.
- Describe TLSH's encoded length component as an approximate file-size range in API documentation,
  help, and guided questions.
