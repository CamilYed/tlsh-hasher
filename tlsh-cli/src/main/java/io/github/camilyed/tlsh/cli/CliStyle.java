package io.github.camilyed.tlsh.cli;

import picocli.CommandLine.Help.Ansi;

/** Applies a small semantic color palette while leaving redirected output free of escape codes. */
final class CliStyle {

  private static final String RESET = "\u001B[0m";

  private final boolean enabled;

  CliStyle(final Ansi ansi) {
    enabled = ansi.enabled();
  }

  /** Highlights product and workflow headings. */
  String heading(final String text) {
    return decorate("1;36", text);
  }

  /** Highlights menu keys, counters, progress-related values, and prompts. */
  String accent(final String text) {
    return decorate("36", text);
  }

  /** Marks a completed operation that had no failures. */
  String success(final String text) {
    return decorate("32", text);
  }

  /** Marks a completed operation that needs the user's attention. */
  String warning(final String text) {
    return decorate("33", text);
  }

  /** Marks invalid input and failed files. */
  String error(final String text) {
    return decorate("31", text);
  }

  /** De-emphasizes hints without hiding them. */
  String muted(final String text) {
    return decorate("2", text);
  }

  /** Wraps arbitrary user-facing text without interpreting it as markup. */
  private String decorate(final String code, final String text) {
    return enabled ? "\u001B[" + code + "m" + text + RESET : text;
  }
}
