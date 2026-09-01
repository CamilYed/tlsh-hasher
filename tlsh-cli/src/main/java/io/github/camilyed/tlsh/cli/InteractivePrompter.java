package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Centralizes prompts, path completion, confirmation rules, and semantic terminal styling. */
final class InteractivePrompter {

  private final CliTerminal terminal;
  private final PrintWriter output;
  private final CliStyle style;

  InteractivePrompter(final CliTerminal terminal, final PrintWriter output) {
    this.terminal = terminal;
    this.output = output;
    style = new CliStyle(terminal.ansi());
  }

  /** Reads a normal menu or confirmation answer. A {@code null} result represents Ctrl-D. */
  String answer(final String prompt) {
    return terminal.readLine(style.accent(prompt));
  }

  /** Reads a filesystem path with JLine completion enabled for the Tab key. */
  Optional<Path> path(final String prompt) {
    final String answer = terminal.readPath(style.accent(prompt));
    if (answer == null || answer.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(InteractivePathParser.parse(answer));
    } catch (final InvalidPathException exception) {
      error("That is not a valid filesystem path: " + TlshCli.message(exception));
      return Optional.empty();
    }
  }

  /** Recognizes explicit affirmative answers. */
  boolean yes(final String answer) {
    if (answer == null) {
      return false;
    }
    final String normalized = answer.strip().toLowerCase(Locale.ROOT);
    return "y".equals(normalized) || "yes".equals(normalized);
  }

  /** Accepts an empty answer as confirmation while recognizing explicit negative answers. */
  boolean yesByDefault(final String answer) {
    return answer != null && (answer.isBlank() || yes(answer));
  }

  /** Prints an ordinary line. */
  void line(final String text) {
    output.println(text);
  }

  /** Prints a blank separator line. */
  void blankLine() {
    output.println();
  }

  /** Prints a workflow heading. */
  void heading(final String text) {
    line(style.heading(text));
  }

  /** Prints a low-emphasis usage hint. */
  void hint(final String text) {
    line(style.muted(text));
  }

  /** Prints invalid input in the semantic error color. */
  void error(final String text) {
    line(style.error("✗ " + text));
  }

  /** Exposes the common palette for composed menu and preview lines. */
  CliStyle style() {
    return style;
  }
}
