package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;

/** One pair whose TLSH distance is at or below the requested maximum. */
record SimilarityMatch(int distance, Path firstPath, Path secondPath) {}
