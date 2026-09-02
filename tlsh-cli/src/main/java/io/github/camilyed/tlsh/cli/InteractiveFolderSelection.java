package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;
import java.util.List;

/** A validated folder, its traversal choice, and the deterministic discovery preview. */
record InteractiveFolderSelection(
    Path directory, boolean recursive, HashInputDiscovery.Result discovery) {

  /** Returns the files that the confirmed operation will attempt to process. */
  List<HashInput> inputs() {
    return discovery.inputs();
  }

  /** Returns the exact number of unique pairs for an all-pairs similarity scan. */
  long comparisonCount() {
    return SimilarityScanUseCase.pairCount(inputs().size());
  }

  /** Sums discovered sizes for a preview without allowing arithmetic overflow. */
  long expectedBytes() {
    long total = 0L;
    for (final HashInput input : inputs()) {
      if (Long.MAX_VALUE - total < input.expectedBytes()) {
        return Long.MAX_VALUE;
      }
      total += input.expectedBytes();
    }
    return total;
  }
}
