package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import java.util.List;
import picocli.CommandLine.Help.Ansi;

/** Renders one reusable progress line instead of emitting a new log line for every read chunk. */
final class HashProgress {

  private static final long MINIMUM_REDRAW_NANOS = 80_000_000L;
  private static final int BAR_WIDTH = 20;
  private static final List<String> SPINNER =
      List.of("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏");

  private final PrintWriter error;
  private final boolean enabled;
  private final long totalBytes;
  private final int totalFiles;
  private final long startedAt;
  private final Ansi ansi;

  private long processedBytes;
  private long lastRedrawAt;
  private int currentFile;
  private String currentName = "";
  private int renderedWidth;

  private HashProgress(
      final PrintWriter error,
      final boolean enabled,
      final long totalBytes,
      final int totalFiles,
      final Ansi ansi) {
    this.error = error;
    this.enabled = enabled;
    this.totalBytes = totalBytes;
    this.totalFiles = totalFiles;
    this.startedAt = System.nanoTime();
    this.ansi = ansi;
  }

  /** Chooses an active or silent renderer from the option and detected terminal capability. */
  static HashProgress create(
      final ProgressMode mode,
      final CliTerminal terminal,
      final PrintWriter error,
      final long totalBytes,
      final int totalFiles) {
    final boolean enabled =
        mode == ProgressMode.ALWAYS || (mode == ProgressMode.AUTO && terminal.interactive());
    return new HashProgress(error, enabled, totalBytes, totalFiles, terminal.ansi());
  }

  /** Selects the file name and file ordinal displayed beside the aggregate byte progress. */
  void startFile(final int fileNumber, final String displayName) {
    currentFile = fileNumber;
    currentName = abbreviate(displayName);
    redraw(true);
  }

  /** Adds newly read bytes and redraws at a bounded rate to keep hashing overhead low. */
  void advance(final long byteCount) {
    processedBytes += byteCount;
    redraw(false);
  }

  /** Forces the last state for a file to become visible even when it completed between redraws. */
  void finishFile() {
    redraw(true);
  }

  /** Removes the transient line before stable digest output is printed to the same terminal. */
  void clearLine() {
    if (!enabled || renderedWidth == 0) {
      return;
    }
    error.print('\r');
    error.print(" ".repeat(renderedWidth));
    error.print('\r');
    error.flush();
    renderedWidth = 0;
  }

  /** Finishes terminal presentation without leaving a partially drawn line behind. */
  void close() {
    clearLine();
  }

  /** Returns actual bytes read, including standard input whose size was initially unknown. */
  long processedBytes() {
    return processedBytes;
  }

  /** Builds and prints either a determinate bar or an indeterminate spinner. */
  private void redraw(final boolean forced) {
    if (!enabled) {
      return;
    }
    final long now = System.nanoTime();
    if (!forced && now - lastRedrawAt < MINIMUM_REDRAW_NANOS) {
      return;
    }
    lastRedrawAt = now;

    final ProgressLine line = totalBytes >= 0L ? determinateLine(now) : indeterminateLine(now);
    error.print('\r');
    error.print(line.styled());
    if (line.visibleWidth() < renderedWidth) {
      error.print(" ".repeat(renderedWidth - line.visibleWidth()));
    }
    error.flush();
    renderedWidth = line.visibleWidth();
  }

  /** Renders aggregate bytes, percentage, file ordinal, current name, and current throughput. */
  private ProgressLine determinateLine(final long now) {
    final int percentage =
        totalBytes == 0L ? 100 : (int) Math.min(100.0, processedBytes * 100.0 / totalBytes);
    final int completedCells = percentage * BAR_WIDTH / 100;
    final String bar = "█".repeat(completedCells) + "░".repeat(BAR_WIDTH - completedCells);
    final String plain =
        String.format(
            "  [%s] %3d%%  %s/%s  %d/%d  %s  %s/s",
            bar,
            percentage,
            HumanUnits.bytes(processedBytes),
            HumanUnits.bytes(totalBytes),
            currentFile,
            totalFiles,
            currentName,
            HumanUnits.bytes(bytesPerSecond(now)));
    final String styled = plain.replace(bar, ansi.string("@|cyan " + bar + "|@"));
    return new ProgressLine(styled, plain.length());
  }

  /** Renders a spinner when standard input prevents knowing the final byte count in advance. */
  private ProgressLine indeterminateLine(final long now) {
    final int spinnerIndex = (int) ((now / MINIMUM_REDRAW_NANOS) % SPINNER.size());
    final String spinner = SPINNER.get(spinnerIndex);
    final String plain =
        String.format(
            "  %s  %s  %d/%d  %s  %s/s",
            spinner,
            HumanUnits.bytes(processedBytes),
            currentFile,
            totalFiles,
            currentName,
            HumanUnits.bytes(bytesPerSecond(now)));
    final String styled = plain.replace(spinner, ansi.string("@|cyan " + spinner + "|@"));
    return new ProgressLine(styled, plain.length());
  }

  /** Calculates average throughput while avoiding division by zero at startup. */
  private long bytesPerSecond(final long now) {
    final long elapsed = Math.max(1L, now - startedAt);
    return (long) (processedBytes * 1_000_000_000.0 / elapsed);
  }

  /** Keeps very long paths from pushing useful progress information off a narrow terminal. */
  private static String abbreviate(final String displayName) {
    final int maximumLength = 32;
    if (displayName.length() <= maximumLength) {
      return displayName;
    }
    return "…" + displayName.substring(displayName.length() - maximumLength + 1);
  }

  /** Keeps terminal escape sequences separate from the number of occupied columns. */
  private record ProgressLine(String styled, int visibleWidth) {}
}
