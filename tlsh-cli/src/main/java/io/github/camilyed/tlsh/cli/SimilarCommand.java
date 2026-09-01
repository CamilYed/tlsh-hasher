package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** Finds related files by comparing every unique TLSH digest pair in one directory. */
@Command(
    name = "similar",
    description = {
      "Find file pairs whose TLSH distance is at most --max-distance.",
      "Each file is hashed once. Results are ordered by distance, then path.",
      "Distance 0 means equal TLSH digests, not proof of byte-for-byte equality."
    },
    mixinStandardHelpOptions = true)
final class SimilarCommand implements Callable<Integer> {

  static final long DEFAULT_MAXIMUM_COMPARISONS = 1_000_000L;

  @ParentCommand private TlshCli parent;

  @Parameters(index = "0", paramLabel = "DIRECTORY", description = "Directory to scan.")
  private Path directory;

  @Option(
      names = {"-r", "--recursive"},
      description = "Include nested directories without following symbolic directories.")
  private boolean recursive;

  @Option(
      names = "--include-hidden",
      description = "Include hidden files and descend into hidden directories.")
  private boolean includeHidden;

  @Option(
      names = "--max-distance",
      defaultValue = "0",
      paramLabel = "N",
      description = "Include distances from 0 through N (default: ${DEFAULT-VALUE}).")
  private int maximumDistance;

  @Option(
      names = "--ignore-length",
      description = "Exclude the approximate input-length contribution from each distance.")
  private boolean ignoreLength;

  @Option(
      names = "--max-comparisons",
      defaultValue = "1000000",
      paramLabel = "N",
      description = {
        "Refuse a scan requiring more than N unique pairs",
        "(default: ${DEFAULT-VALUE}). Raise deliberately for very large folders."
      })
  private long maximumComparisons;

  /** Validates numeric bounds and delegates the scan to the shared application use case. */
  @Override
  public Integer call() {
    if (maximumDistance < 0) {
      return reject("--max-distance must be zero or greater");
    }
    if (maximumComparisons < 0L) {
      return reject("--max-comparisons must be zero or greater");
    }

    return parent.findSimilar(
        new SimilarityScanRequest(
            directory,
            recursive,
            includeHidden,
            maximumDistance,
            ignoreLength,
            maximumComparisons));
  }

  /** Reports an expected option error without exposing an implementation stack trace. */
  private int reject(final String detail) {
    parent.error().println("tlsh: " + detail);
    return TlshCli.DATA_ERROR;
  }
}
