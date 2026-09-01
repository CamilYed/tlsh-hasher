package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Guides a human through common commands when {@code tlsh} starts without arguments. */
final class InteractiveShell {

  private final TlshCli cli;
  private final CliTerminal terminal;
  private final PrintWriter output;

  InteractiveShell(final TlshCli cli, final CliTerminal terminal, final PrintWriter output) {
    this.cli = cli;
    this.terminal = terminal;
    this.output = output;
  }

  /**
   * Displays one focused menu and delegates the selected operation to the normal command parser.
   */
  int run() {
    printWelcome();
    final String selection = answer("Choose an action [1]: ").toLowerCase(Locale.ROOT);
    return switch (selection) {
      case "", "1", "hash", "h" -> runHash();
      case "2", "distance", "compare", "d", "c" -> runDistance();
      case "3", "quit", "exit", "q" -> leave();
      default -> invalidSelection();
    };
  }

  /** Collects one or more paths and optional recursive traversal before invoking {@code hash}. */
  private int runHash() {
    final String pathLine = answer("File or folder path: ");
    if (pathLine.isBlank()) {
      output.println("Nothing selected.");
      return TlshCli.SUCCESS;
    }

    final boolean recursive = yes(answer("Include nested folders? [y/N]: "));
    final List<String> arguments = new ArrayList<>();
    arguments.add("hash");
    arguments.add("--progress=always");
    if (recursive) {
      arguments.add("--recursive");
    }
    arguments.add(pathLine.strip());
    output.println();
    return cli.execute(arguments.toArray(String[]::new));
  }

  /** Collects two digests and the distance mode before invoking {@code distance}. */
  private int runDistance() {
    final String first = answer("First T1 digest: ").strip();
    final String second = answer("Second T1 digest: ").strip();
    final boolean ignoreLength = yes(answer("Ignore input-length difference? [y/N]: "));

    final List<String> arguments = new ArrayList<>();
    arguments.add("distance");
    if (ignoreLength) {
      arguments.add("--ignore-length");
    }
    arguments.add(first);
    arguments.add(second);
    output.println();
    return cli.execute(arguments.toArray(String[]::new));
  }

  /** Prints the product identity and a deliberately small choice set. */
  private void printWelcome() {
    output.println("╭────────────────────────────────────────────╮");
    output.println(
        "│  "
            + terminal.ansi().string("@|bold,cyan TLSH|@")
            + " · find similarity beyond exact bytes │");
    output.println("╰────────────────────────────────────────────╯");
    output.println();
    output.println("  1  Hash a file or folder");
    output.println("  2  Compare two digests");
    output.println("  3  Exit");
    output.println();
    output.flush();
  }

  /** Reads one answer and treats an end-of-input signal like an empty response. */
  private String answer(final String prompt) {
    final String answer = terminal.readLine(prompt);
    return answer == null ? "" : answer;
  }

  /** Recognizes short and full affirmative answers without depending on locale. */
  private static boolean yes(final String answer) {
    final String normalized = answer.strip().toLowerCase(Locale.ROOT);
    return "y".equals(normalized) || "yes".equals(normalized);
  }

  /** Leaves a clean final line when the user explicitly exits. */
  private int leave() {
    output.println("Bye.");
    return TlshCli.SUCCESS;
  }

  /** Rejects unknown choices instead of silently selecting a destructive or expensive operation. */
  private int invalidSelection() {
    output.println("Unknown choice. Run tlsh again and select 1, 2, or 3.");
    return TlshCli.DATA_ERROR;
  }
}
