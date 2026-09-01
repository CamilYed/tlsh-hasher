package io.github.camilyed.tlsh.cli;

/** Prevents an unexpectedly large all-pairs comparison from starting. */
final class SimilarityScanLimitException extends Exception {

  private static final long serialVersionUID = 1L;

  SimilarityScanLimitException(final long requiredComparisons, final long maximumComparisons) {
    super(
        "scan requires "
            + requiredComparisons
            + " comparisons, exceeding the --max-comparisons limit of "
            + maximumComparisons);
  }
}
