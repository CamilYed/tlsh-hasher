package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import picocli.CommandLine.Help.Ansi;

/** Renders completed TLSH digest pairs on one transient, rate-limited terminal line. */
final class ComparisonProgress {

  private static final long MINIMUM_REDRAW_NANOS = 80_000_000L;
  private static final int BAR_WIDTH = 20;

  private final PrintWriter error;
  private final boolean enabled;
  private final long totalComparisons;
  private final long startedAt = System.nanoTime();
  private final Ansi ansi;

  private long completedComparisons;
  private long lastRedrawAt;
  private int renderedWidth;

  ComparisonProgress(
      final PrintWriter error,
      final boolean enabled,
      final long totalComparisons,
      final Ansi ansi) {
    this.error = error;
    this.enabled = enabled;
    this.totalComparisons = totalComparisons;
    this.ansi = ansi;
  }

  /** Shows the initial zero-comparison state immediately after the phase heading. */
  void start() {
    redraw(true);
  }

  /** Records one unique pair and redraws only often enough to remain inexpensive. */
  void advance() {
    completedComparisons++;
    redraw(false);
  }

  /** Forces the completed state to be rendered even when the whole phase was very fast. */
  void finish() {
    completedComparisons = totalComparisons;
    redraw(true);
  }

  /** Removes the transient line before stable matches or the final summary are printed. */
  void close() {
    if (!enabled || renderedWidth == 0) {
      return;
    }
    error.print('\r');
    error.print(" ".repeat(renderedWidth));
    error.print('\r');
    error.flush();
    renderedWidth = 0;
  }

  /** Builds a determinate bar with pair count and average comparison throughput. */
  private void redraw(final boolean forced) {
    if (!enabled) {
      return;
    }
    final long now = System.nanoTime();
    if (!forced && now - lastRedrawAt < MINIMUM_REDRAW_NANOS) {
      return;
    }
    lastRedrawAt = now;

    final int percentage =
        totalComparisons == 0L
            ? 100
            : (int) Math.min(100.0, completedComparisons * 100.0 / totalComparisons);
    final int completedCells = percentage * BAR_WIDTH / 100;
    final String bar = "█".repeat(completedCells) + "░".repeat(BAR_WIDTH - completedCells);
    final String plain =
        String.format(
            "  [%s] %3d%%  %d/%d pairs  %d pairs/s",
            bar, percentage, completedComparisons, totalComparisons, comparisonsPerSecond(now));
    final String styled = plain.replace(bar, ansi.string("@|cyan " + bar + "|@"));

    error.print('\r');
    error.print(styled);
    if (plain.length() < renderedWidth) {
      error.print(" ".repeat(renderedWidth - plain.length()));
    }
    error.flush();
    renderedWidth = plain.length();
  }

  /** Calculates the average pair rate while avoiding division by zero at startup. */
  private long comparisonsPerSecond(final long now) {
    final long elapsed = Math.max(1L, now - startedAt);
    return (long) (completedComparisons * 1_000_000_000.0 / elapsed);
  }
}
