package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;

/**
 * Renders deterministic pair records on standard output and human diagnostics on standard error.
 */
final class SimilarityScanReporter {

  private static final int MAXIMUM_DISPLAYED_FAILURES = 20;

  private final PrintWriter output;
  private final PrintWriter error;
  private final CliStyle style;

  SimilarityScanReporter(
      final PrintWriter output, final PrintWriter error, final CliTerminal terminal) {
    this.output = output;
    this.error = error;
    style = new CliStyle(terminal.ansi());
  }

  /** Prints one stable line per match followed by a concise summary and any failures. */
  void print(final SimilarityScanResult result) {
    for (final SimilarityMatch match : result.matches()) {
      output.println(match.distance() + "  " + match.firstPath() + "  " + match.secondPath());
    }

    error.println(
        outcomeSymbol(result)
            + " "
            + hashedCount(result)
            + " · "
            + result.comparisons()
            + pluralize(result.comparisons(), " comparison", " comparisons")
            + " · "
            + result.matches().size()
            + pluralize(result.matches().size(), " match", " matches")
            + " · "
            + HumanUnits.bytes(result.processedBytes())
            + " · "
            + HumanUnits.duration(result.elapsedNanoseconds())
            + hiddenSuffix(result));
    if (!result.failures().isEmpty()) {
      printFailures(result);
    }
  }

  /** Uses success or warning color without changing the text contract consumed by scripts. */
  private String outcomeSymbol(final SimilarityScanResult result) {
    return result.failures().isEmpty() ? style.success("✓") : style.warning("⚠");
  }

  /** Makes partial success explicit instead of showing only the successful side of the ratio. */
  private static String hashedCount(final SimilarityScanResult result) {
    if (result.failures().isEmpty()) {
      return result.hashedFiles()
          + pluralize(result.hashedFiles(), " file hashed", " files hashed");
    }
    return result.hashedFiles() + " of " + result.attemptedInputs() + " files hashed";
  }

  /** Repeats failed inputs after the summary, bounded so a damaged directory stays readable. */
  private void printFailures(final SimilarityScanResult result) {
    error.println();
    error.println(style.error("Failed files"));
    final int displayed = Math.min(result.failures().size(), MAXIMUM_DISPLAYED_FAILURES);
    for (int index = 0; index < displayed; index++) {
      final HashFailure failure = result.failures().get(index);
      error.println("  " + style.error("✗") + " " + failure.inputName());
      error.println("    " + failure.detail());
    }
    final int omitted = result.failures().size() - displayed;
    if (omitted > 0) {
      error.println("  " + style.warning("… and " + omitted + " more"));
    }
  }

  /** Makes hidden-file filtering visible without treating the default policy as an error. */
  private String hiddenSuffix(final SimilarityScanResult result) {
    final int skipped = result.skippedHiddenEntries();
    return skipped == 0
        ? ""
        : " · "
            + style.muted(
                skipped + pluralize(skipped, " hidden entry skipped", " hidden entries skipped"));
  }

  /** Selects singular grammar for both integer and long counters. */
  private static String pluralize(final long count, final String singular, final String plural) {
    return count == 1L ? singular : plural;
  }
}
