package io.github.camilyed.tlsh.cli;

import java.util.List;

/** Immutable facts required to render the outcome of one file or folder hashing run. */
record HashBatchSummary(
    int successfulFiles, List<HashFailure> failures, long processedBytes, long elapsedNanoseconds) {

  /** Returns every attempted item, including paths that failed during discovery. */
  int attemptedFiles() {
    return successfulFiles + failures.size();
  }
}
