package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;

/** Coordinates separate byte-hashing and digest-comparison progress phases for one scan. */
final class SimilarityScanProgress implements AutoCloseable {

  private final ProgressMode mode;
  private final CliTerminal terminal;
  private final PrintWriter error;
  private final boolean enabled;
  private final CliStyle style;

  private HashProgress hashing;
  private ComparisonProgress comparing;

  private SimilarityScanProgress(
      final ProgressMode mode, final CliTerminal terminal, final PrintWriter error) {
    this.mode = mode;
    this.terminal = terminal;
    this.error = error;
    enabled = mode == ProgressMode.ALWAYS || (mode == ProgressMode.AUTO && terminal.interactive());
    style = new CliStyle(terminal.ansi());
  }

  /** Creates an active or silent observer according to the option and terminal capability. */
  static SimilarityScanProgress create(
      final ProgressMode mode, final CliTerminal terminal, final PrintWriter error) {
    return new SimilarityScanProgress(mode, terminal, error);
  }

  /** Starts byte-oriented progress after discovery provides stable file and size totals. */
  void startHashing(final long totalBytes, final int totalFiles) {
    if (enabled) {
      error.println(style.heading("Hashing files"));
    }
    hashing = HashProgress.create(mode, terminal, error, totalBytes, totalFiles);
  }

  /** Selects the source name and ordinal currently being hashed. */
  void startFile(final int fileNumber, final String displayName) {
    hashing.startFile(fileNumber, displayName);
  }

  /** Records bytes read from the current file. */
  void advanceBytes(final long byteCount) {
    hashing.advance(byteCount);
  }

  /** Forces the last byte count for the current file to be visible. */
  void finishFile() {
    hashing.finishFile();
  }

  /** Switches from byte progress to the exact number of usable digest pairs. */
  void startComparing(final long totalComparisons) {
    hashing.close();
    if (enabled) {
      error.println(style.heading("Comparing digests"));
    }
    comparing = new ComparisonProgress(error, enabled, totalComparisons, terminal.ansi());
    comparing.start();
  }

  /** Records one completed unique digest comparison. */
  void advanceComparison() {
    comparing.advance();
  }

  /** Forces the completed comparison state before final records are rendered. */
  void finishComparing() {
    comparing.finish();
    comparing.close();
  }

  /** Returns actual bytes read, including bytes from a file that ultimately failed. */
  long processedBytes() {
    return hashing == null ? 0L : hashing.processedBytes();
  }

  /** Clears whichever transient phase line exists, including after an exception. */
  @Override
  public void close() {
    if (hashing != null) {
      hashing.close();
    }
    if (comparing != null) {
      comparing.close();
    }
  }
}
