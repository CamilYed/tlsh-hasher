package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;

/** Prints an easy-to-scan batch summary and repeats every failure after successful digest lines. */
final class HashBatchReporter {

  private static final int MAXIMUM_DISPLAYED_FAILURES = 20;

  private final PrintWriter error;
  private final CliStyle style;

  HashBatchReporter(final PrintWriter error, final CliTerminal terminal) {
    this.error = error;
    style = new CliStyle(terminal.ansi());
  }

  /** Prints success compactly and expands runs with failures into a separate final report. */
  void print(final HashBatchSummary summary, final boolean summaryEnabled) {
    if (summary.failures().isEmpty()) {
      if (summaryEnabled) {
        printSuccess(summary);
      }
      return;
    }

    error.println();
    if (summaryEnabled) {
      printWarningSummary(summary);
      error.println();
    }
    printFailures(summary);
  }

  /** Prints the familiar single-line outcome when every file succeeded. */
  private void printSuccess(final HashBatchSummary summary) {
    error.println(
        style.success("✓")
            + " "
            + summary.successfulFiles()
            + pluralize(summary.successfulFiles(), " file", " files")
            + " hashed · "
            + HumanUnits.bytes(summary.processedBytes())
            + " · "
            + HumanUnits.duration(summary.elapsedNanoseconds()));
  }

  /** Puts the ratio before secondary measurements so partial success is immediately visible. */
  private void printWarningSummary(final HashBatchSummary summary) {
    final int failureCount = summary.failures().size();
    error.println(
        style.warning(
                "⚠ Completed with "
                    + failureCount
                    + pluralize(failureCount, " failed file", " failed files"))
            + System.lineSeparator()
            + "  "
            + summary.successfulFiles()
            + " of "
            + summary.attemptedFiles()
            + " hashed · "
            + HumanUnits.bytes(summary.processedBytes())
            + " · "
            + HumanUnits.duration(summary.elapsedNanoseconds()));
  }

  /** Repeats failures at the bottom where they cannot be lost among successful digest lines. */
  private void printFailures(final HashBatchSummary summary) {
    error.println(style.error("Failed files"));
    final int displayed = Math.min(summary.failures().size(), MAXIMUM_DISPLAYED_FAILURES);
    for (int index = 0; index < displayed; index++) {
      final HashFailure failure = summary.failures().get(index);
      error.println("  " + style.error("✗") + " " + failure.inputName());
      error.println("    " + failure.detail());
    }
    final int omitted = summary.failures().size() - displayed;
    if (omitted > 0) {
      error.println("  " + style.warning("… and " + omitted + " more"));
    }
  }

  /** Chooses a singular or plural noun without introducing a formatting dependency. */
  private static String pluralize(final int count, final String singular, final String plural) {
    return count == 1 ? singular : plural;
  }
}
