package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hashes two regular files and calculates their TLSH difference using one selected distance mode.
 */
final class FileComparisonUseCase {

  /** Produces both digests and their score or identifies the exact file that could not be used. */
  FileComparison execute(final FileComparisonRequest request) throws FileComparisonException {
    final TlshDigest first = hash(request.firstPath());
    final TlshDigest second = hash(request.secondPath());
    final int distance =
        request.ignoreLength() ? first.distanceToIgnoringLength(second) : first.distanceTo(second);
    return new FileComparison(
        request.firstPath(), first, request.secondPath(), second, request.ignoreLength(), distance);
  }

  /** Validates one regular file and translates expected API failures into a path-aware error. */
  private static TlshDigest hash(final Path path) throws FileComparisonException {
    if (!Files.isRegularFile(path)) {
      final String detail =
          Files.exists(path) ? "path is not a regular file" : "path does not exist";
      throw new FileComparisonException(
          path.toString(), detail, new IllegalArgumentException(detail));
    }

    long expectedBytes = -1L;
    try {
      expectedBytes = Files.size(path);
      return Tlsh.hash(path);
    } catch (final IOException
        | IllegalArgumentException
        | IllegalStateException
        | SecurityException exception) {
      throw new FileComparisonException(
          path.toString(), HashFailureDetail.explain(expectedBytes, exception), exception);
    }
  }
}
