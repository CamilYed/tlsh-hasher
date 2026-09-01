package io.github.camilyed.tlsh.cli;

import java.util.List;

/** Complete outcome of a similarity scan, including partial failures and measured work. */
record SimilarityScanResult(
    List<SimilarityMatch> matches,
    List<HashFailure> failures,
    int attemptedInputs,
    int hashedFiles,
    int skippedHiddenEntries,
    long comparisons,
    long processedBytes,
    long elapsedNanoseconds) {}
