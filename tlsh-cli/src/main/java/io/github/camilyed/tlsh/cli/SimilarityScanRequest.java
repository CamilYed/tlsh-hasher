package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;

/** Describes one bounded search for similar files inside a directory. */
record SimilarityScanRequest(
    Path directory,
    boolean recursive,
    boolean includeHidden,
    int maximumDistance,
    boolean ignoreLength,
    long maximumComparisons) {}
