package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;

/** Immutable input for comparing the TLSH digests of two regular files. */
record FileComparisonRequest(Path firstPath, Path secondPath, boolean ignoreLength) {

  /** Normalizes both paths so every adapter presents the same file identity. */
  FileComparisonRequest {
    firstPath = firstPath.toAbsolutePath().normalize();
    secondPath = secondPath.toAbsolutePath().normalize();
  }
}
