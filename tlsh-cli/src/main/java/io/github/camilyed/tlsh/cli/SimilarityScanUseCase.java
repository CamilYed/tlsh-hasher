package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Hashes each discovered file once and compares every unique pair within a bounded scan. */
final class SimilarityScanUseCase {

  private final SimilarityScanProgress progress;

  SimilarityScanUseCase(final SimilarityScanProgress progress) {
    this.progress = progress;
  }

  /**
   * Finds pairs whose TLSH distance does not exceed the requested threshold.
   *
   * <p>For {@code n} files there are {@code n * (n - 1) / 2} unique pairs. The method checks this
   * number before reading file contents, so accidentally selecting a very large directory cannot
   * silently start millions of comparisons. Each usable file is then hashed once; the stored
   * digests, rather than the file contents, are compared with one another.
   */
  SimilarityScanResult execute(final SimilarityScanRequest request)
      throws SimilarityScanLimitException {
    validate(request);
    final long startedAt = System.nanoTime();
    final HashInputDiscovery.Result discovery =
        new HashInputDiscovery()
            .discover(
                List.of(request.directory().toString()),
                request.recursive(),
                request.includeHidden());
    final long plannedComparisons = pairCount(discovery.inputs().size());
    if (plannedComparisons > request.maximumComparisons()) {
      throw new SimilarityScanLimitException(plannedComparisons, request.maximumComparisons());
    }

    final List<HashFailure> failures = discoveryFailures(discovery.failures());
    final List<HashedFile> hashedFiles = new ArrayList<>(discovery.inputs().size());
    progress.startHashing(expectedBytes(discovery.inputs()), discovery.inputs().size());
    for (int index = 0; index < discovery.inputs().size(); index++) {
      final HashInput input = discovery.inputs().get(index);
      progress.startFile(index + 1, input.displayName());
      try {
        hashedFiles.add(new HashedFile(input.path(), hash(input)));
      } catch (final IOException
          | IllegalArgumentException
          | IllegalStateException
          | SecurityException exception) {
        failures.add(
            new HashFailure(
                input.displayName(), HashFailureDetail.explain(input.expectedBytes(), exception)));
      } finally {
        progress.finishFile();
      }
    }

    final long comparisons = pairCount(hashedFiles.size());
    progress.startComparing(comparisons);
    final List<SimilarityMatch> matches = findMatches(hashedFiles, request, progress);
    progress.finishComparing();
    return new SimilarityScanResult(
        List.copyOf(matches),
        List.copyOf(failures),
        discovery.inputs().size() + discovery.failures().size(),
        hashedFiles.size(),
        discovery.skippedHiddenEntries(),
        comparisons,
        progress.processedBytes(),
        System.nanoTime() - startedAt);
  }

  /** Compares positions {@code i < j}, so a file is never compared with itself or twice. */
  private static List<SimilarityMatch> findMatches(
      final List<HashedFile> files,
      final SimilarityScanRequest request,
      final SimilarityScanProgress progress) {
    final List<SimilarityMatch> matches = new ArrayList<>();
    for (int firstIndex = 0; firstIndex < files.size(); firstIndex++) {
      final HashedFile first = files.get(firstIndex);
      for (int secondIndex = firstIndex + 1; secondIndex < files.size(); secondIndex++) {
        final HashedFile second = files.get(secondIndex);
        final int distance = distance(first.digest(), second.digest(), request.ignoreLength());
        if (distance <= request.maximumDistance()) {
          matches.add(new SimilarityMatch(distance, first.path(), second.path()));
        }
        progress.advanceComparison();
      }
    }
    matches.sort(
        Comparator.comparingInt(SimilarityMatch::distance)
            .thenComparing(match -> match.firstPath().toString())
            .thenComparing(match -> match.secondPath().toString()));
    return matches;
  }

  /** Chooses the same two distance modes exposed by the single-pair comparison commands. */
  private static int distance(
      final TlshDigest first, final TlshDigest second, final boolean ignoreLength) {
    return ignoreLength ? first.distanceToIgnoringLength(second) : first.distanceTo(second);
  }

  /** Calculates a triangular number in {@code long}, safely covering every Java list size. */
  static long pairCount(final int fileCount) {
    return fileCount * (fileCount - 1L) / 2L;
  }

  /** Streams one file through the hasher while reporting actual byte reads. */
  private TlshDigest hash(final HashInput input) throws IOException {
    try (InputStream file = Files.newInputStream(input.path());
        CountingInputStream counting = new CountingInputStream(file, progress::advanceBytes)) {
      return Tlsh.hash(counting);
    }
  }

  /** Sums discovered file sizes without allowing an extreme directory to overflow. */
  private static long expectedBytes(final List<HashInput> inputs) {
    long total = 0L;
    for (final HashInput input : inputs) {
      if (Long.MAX_VALUE - total < input.expectedBytes()) {
        return Long.MAX_VALUE;
      }
      total += input.expectedBytes();
    }
    return total;
  }

  /** Rejects nonsensical limits before discovering or reading files. */
  private static void validate(final SimilarityScanRequest request) {
    if (request.maximumDistance() < 0) {
      throw new IllegalArgumentException("maximum distance must not be negative");
    }
    if (request.maximumComparisons() < 0L) {
      throw new IllegalArgumentException("maximum comparisons must not be negative");
    }
    if (Files.isRegularFile(request.directory())) {
      throw new IllegalArgumentException("path is a file; similar requires a directory");
    }
  }

  /** Converts discovery diagnostics into the common path-and-detail representation. */
  private static List<HashFailure> discoveryFailures(
      final List<HashInputDiscovery.Failure> discoveryFailures) {
    final List<HashFailure> failures = new ArrayList<>(discoveryFailures.size());
    for (final HashInputDiscovery.Failure failure : discoveryFailures) {
      failures.add(new HashFailure(failure.inputName(), failure.detail()));
    }
    return failures;
  }

  /** Keeps a source path beside the digest calculated from it. */
  private record HashedFile(Path path, TlshDigest digest) {}
}
