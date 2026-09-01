package io.github.camilyed.tlsh.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Guides a human through repeated operations when {@code tlsh} starts without arguments. */
final class InteractiveShell {

  private static final String BACK_HINT = "Leave empty to return to the menu.";

  private final TlshCli cli;
  private final CliTerminal terminal;
  private final PrintWriter output;

  InteractiveShell(final TlshCli cli, final CliTerminal terminal, final PrintWriter output) {
    this.cli = cli;
    this.terminal = terminal;
    this.output = output;
  }

  /**
   * Keeps the terminal session alive until the user explicitly exits.
   *
   * <p>Each menu action delegates to the same Picocli commands available to scripts. The guided
   * shell therefore adds discovery and prompts without maintaining a second implementation of
   * hashing or distance calculation.
   */
  int run() {
    printWelcome();
    int sessionExitCode = TlshCli.SUCCESS;
    boolean running = true;
    while (running) {
      printMenu();
      final String selection = answer("Choose an action [1]: ").toLowerCase(Locale.ROOT);
      final int operationExitCode;
      switch (selection) {
        case "", "1", "file", "f" -> operationExitCode = runFileHash();
        case "2", "folder", "directory" -> operationExitCode = runFolderHash();
        case "3", "distance", "compare", "d", "c" -> operationExitCode = runDistance();
        case "4", "quit", "exit", "q" -> {
          operationExitCode = TlshCli.SUCCESS;
          running = false;
          output.println("Bye.");
        }
        default -> operationExitCode = invalidSelection();
      }
      sessionExitCode = Math.max(sessionExitCode, operationExitCode);
      if (running) {
        output.println();
      }
    }
    return sessionExitCode;
  }

  /** Hashes exactly one regular file and never asks directory-specific questions. */
  private int runFileHash() {
    output.println();
    output.println("Hash one file");
    output.println("Current directory: " + Path.of("").toAbsolutePath());
    output.println("Paste a path or drag a file into this terminal. " + BACK_HINT);
    final Optional<Path> selectedPath = readPath("File path: ");
    if (selectedPath.isEmpty()) {
      return TlshCli.SUCCESS;
    }

    final Path path = selectedPath.orElseThrow();
    if (Files.isDirectory(path)) {
      output.println("That path is a folder. Choose 'Hash a folder' from the menu instead.");
      return TlshCli.DATA_ERROR;
    }
    if (!Files.isRegularFile(path)) {
      output.println("No regular file exists at: " + path);
      return TlshCli.DATA_ERROR;
    }

    output.println("Hashing " + path.getFileName() + " · " + size(path));
    output.println();
    return cli.execute(
        "hash", "--progress=always", "--no-summary", path.toAbsolutePath().toString());
  }

  /** Previews a folder batch and lets the user deliberately choose traversal depth. */
  private int runFolderHash() {
    output.println();
    output.println("Hash a folder");
    output.println("Current directory: " + Path.of("").toAbsolutePath());
    output.println("Paste a path or drag a folder into this terminal. " + BACK_HINT);
    final Optional<Path> selectedPath = readPath("Folder path: ");
    if (selectedPath.isEmpty()) {
      return TlshCli.SUCCESS;
    }

    final Path directory = selectedPath.orElseThrow();
    if (Files.isRegularFile(directory)) {
      output.println("That path is a file. Choose 'Hash one file' from the menu instead.");
      return TlshCli.DATA_ERROR;
    }
    if (!Files.isDirectory(directory)) {
      output.println("No directory exists at: " + directory);
      return TlshCli.DATA_ERROR;
    }

    output.println();
    output.println("  1  Files directly in this folder");
    output.println("  2  This folder and every nested folder");
    final String scope = answer("Choose scope [1]: ").strip().toLowerCase(Locale.ROOT);
    final boolean recursive = "2".equals(scope) || "recursive".equals(scope) || "r".equals(scope);
    if (!(scope.isEmpty() || "1".equals(scope) || "current".equals(scope) || recursive)) {
      output.println("Unknown scope. Choose 1 or 2.");
      return TlshCli.DATA_ERROR;
    }

    final HashInputDiscovery.Result preview =
        new HashInputDiscovery().discover(List.of(directory.toString()), recursive);
    if (preview.inputs().isEmpty()) {
      printPreviewFailures(preview.failures());
      return TlshCli.DATA_ERROR;
    }

    output.println();
    output.println(
        "Found "
            + preview.inputs().size()
            + pluralize(preview.inputs().size(), " file", " files")
            + " · "
            + HumanUnits.bytes(expectedBytes(preview.inputs())));
    if (!yesByDefault(answer("Start hashing? [Y/n]: "))) {
      output.println("Cancelled.");
      return TlshCli.SUCCESS;
    }

    final List<String> arguments = new ArrayList<>();
    arguments.add("hash");
    arguments.add("--progress=always");
    if (recursive) {
      arguments.add("--recursive");
    }
    arguments.add(directory.toAbsolutePath().toString());
    output.println();
    return cli.execute(arguments.toArray(String[]::new));
  }

  /** Collects two digests and the distance mode before invoking {@code distance}. */
  private int runDistance() {
    output.println();
    output.println("Compare two digests");
    output.println(BACK_HINT);
    final String first = answer("First T1 digest: ").strip();
    if (first.isEmpty()) {
      return TlshCli.SUCCESS;
    }
    final String second = answer("Second T1 digest: ").strip();
    if (second.isEmpty()) {
      return TlshCli.SUCCESS;
    }
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

  /** Prints the product identity once at the beginning of the persistent session. */
  private void printWelcome() {
    output.println("╭────────────────────────────────────────────╮");
    output.println(
        "│  "
            + terminal.ansi().string("@|bold,cyan TLSH|@")
            + " · find similarity beyond exact bytes │");
    output.println("╰────────────────────────────────────────────╯");
    output.flush();
  }

  /** Separates file and folder workflows so irrelevant questions are never displayed. */
  private void printMenu() {
    output.println();
    output.println("  1  Hash one file");
    output.println("  2  Hash a folder");
    output.println("  3  Compare two digests");
    output.println("  4  Exit");
    output.println();
    output.flush();
  }

  /** Reads and normalizes a pasted, quoted, home-relative, or drag-and-drop path. */
  private Optional<Path> readPath(final String prompt) {
    final String answer = answer(prompt).strip();
    if (answer.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(normalizePath(answer));
    } catch (final InvalidPathException exception) {
      output.println("That is not a valid filesystem path: " + TlshCli.message(exception));
      return Optional.empty();
    }
  }

  /** Removes terminal quoting and expands the conventional tilde home-directory prefix. */
  private static Path normalizePath(final String answer) {
    String normalized = stripMatchingQuotes(answer.strip());
    if (File.separatorChar == '/') {
      normalized = removeShellEscapes(normalized);
    }
    if ("~".equals(normalized)) {
      normalized = System.getProperty("user.home");
    } else if (normalized.startsWith("~" + File.separator)) {
      normalized =
          Path.of(System.getProperty("user.home")).resolve(normalized.substring(2)).toString();
    }
    return Path.of(normalized).normalize();
  }

  /** Removes one matching pair commonly produced by copying a path from another application. */
  private static String stripMatchingQuotes(final String value) {
    if (value.length() < 2) {
      return value;
    }
    final char first = value.charAt(0);
    final char last = value.charAt(value.length() - 1);
    return (first == last && (first == '\'' || first == '"'))
        ? value.substring(1, value.length() - 1)
        : value;
  }

  /** Interprets the backslash escaping used when macOS terminals paste paths containing spaces. */
  private static String removeShellEscapes(final String value) {
    final StringBuilder result = new StringBuilder(value.length());
    boolean escaped = false;
    for (int index = 0; index < value.length(); index++) {
      final char character = value.charAt(index);
      if (escaped) {
        result.append(character);
        escaped = false;
      } else if (character == '\\') {
        escaped = true;
      } else {
        result.append(character);
      }
    }
    if (escaped) {
      result.append('\\');
    }
    return result.toString();
  }

  /** Reads one answer and treats an end-of-input signal like an empty response. */
  private String answer(final String prompt) {
    final String answer = terminal.readLine(prompt);
    return answer == null ? "" : answer;
  }

  /** Recognizes explicit affirmative answers. */
  private static boolean yes(final String answer) {
    final String normalized = answer.strip().toLowerCase(Locale.ROOT);
    return "y".equals(normalized) || "yes".equals(normalized);
  }

  /** Accepts an empty answer as confirmation while recognizing explicit negative answers. */
  private static boolean yesByDefault(final String answer) {
    final String normalized = answer.strip().toLowerCase(Locale.ROOT);
    return normalized.isEmpty() || "y".equals(normalized) || "yes".equals(normalized);
  }

  /** Sums the already inspected file sizes for the preview shown before work starts. */
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

  /** Formats a best-effort file size without turning preview metadata into a fatal operation. */
  private static String size(final Path path) {
    try {
      return HumanUnits.bytes(Files.size(path));
    } catch (final IOException exception) {
      return "size unavailable";
    }
  }

  /** Prints discovery diagnostics before returning to the main menu. */
  private void printPreviewFailures(final List<HashInputDiscovery.Failure> failures) {
    for (final HashInputDiscovery.Failure failure : failures) {
      output.println(failure.inputName() + ": " + failure.detail());
    }
  }

  /** Rejects unknown menu choices while keeping the current session alive. */
  private int invalidSelection() {
    output.println("Unknown choice. Select 1, 2, 3, or 4.");
    return TlshCli.DATA_ERROR;
  }

  /** Chooses a singular or plural noun without introducing a formatting dependency. */
  private static String pluralize(final int count, final String singular, final String plural) {
    return count == 1 ? singular : plural;
  }
}
