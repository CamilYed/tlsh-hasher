package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.TlshDigest;
import java.nio.file.Path;

/** Successful file comparison containing reproducible inputs, digests, mode, and numeric score. */
record FileComparison(
    Path firstPath,
    TlshDigest firstDigest,
    Path secondPath,
    TlshDigest secondDigest,
    boolean ignoredLength,
    int distance) {}
