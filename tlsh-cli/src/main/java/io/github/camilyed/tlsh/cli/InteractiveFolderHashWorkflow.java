package io.github.camilyed.tlsh.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Previews one folder batch before delegating its execution to {@link HashCommand}. */
final class InteractiveFolderHashWorkflow {

  private static final String BACK_HINT = "Leave empty to return to the menu.";

  private final TlshCli cli;
  private final InteractivePrompter prompter;

  InteractiveFolderHashWorkflow(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
  }

  /** Selects traversal scope, displays its cost, confirms, and starts the batch. */
  void run() {
    prompter.blankLine();
    prompter.heading("Hash a folder");
    printPathHints();
    final Optional<Path> selectedPath = prompter.path("Folder path: ");
    if (selectedPath.isEmpty()) {
      return;
    }

    final Path directory = selectedPath.orElseThrow();
    if (Files.isRegularFile(directory)) {
      prompter.error("That path is a file. Choose 'Hash one file' from the menu instead.");
      return;
    }
    if (!Files.isDirectory(directory)) {
      prompter.error("No directory exists at: " + directory);
      return;
    }

    final Optional<Boolean> recursive = selectScope();
    if (recursive.isEmpty()) {
      return;
    }
    final HashInputDiscovery.Result preview =
        new HashInputDiscovery().discover(List.of(directory.toString()), recursive.orElseThrow());
    if (preview.inputs().isEmpty()) {
      printPreviewFailures(preview.failures());
      return;
    }

    printPreview(preview.inputs());
    if (!prompter.yesByDefault(prompter.answer("Start hashing? [Y/n]: "))) {
      prompter.line(prompter.style().muted("Cancelled."));
      return;
    }

    final List<String> arguments = new ArrayList<>();
    arguments.add("hash");
    arguments.add("--progress=always");
    if (recursive.orElseThrow()) {
      arguments.add("--recursive");
    }
    arguments.add(directory.toAbsolutePath().toString());
    prompter.blankLine();
    cli.execute(arguments.toArray(String[]::new));
  }

  /** Shows the working directory and the three convenient ways to provide a folder. */
  private void printPathHints() {
    prompter.hint("Current directory: " + Path.of("").toAbsolutePath());
    prompter.hint("Type a path, drag a folder here, or press Tab to complete it. " + BACK_HINT);
  }

  /** Returns an empty selection for an unknown answer so no unexpectedly broad scan begins. */
  private Optional<Boolean> selectScope() {
    prompter.blankLine();
    prompter.line("  " + prompter.style().accent("1") + "  Files directly in this folder");
    prompter.line("  " + prompter.style().accent("2") + "  This folder and every nested folder");
    final String answer = prompter.answer("Choose scope [1]: ");
    if (answer == null) {
      return Optional.empty();
    }
    final String scope = answer.strip().toLowerCase(Locale.ROOT);
    if (scope.isEmpty() || "1".equals(scope) || "current".equals(scope)) {
      return Optional.of(false);
    }
    if ("2".equals(scope) || "recursive".equals(scope) || "r".equals(scope)) {
      return Optional.of(true);
    }
    prompter.error("Unknown scope. Choose 1 or 2.");
    return Optional.empty();
  }

  /** Highlights the amount of work before the user confirms it. */
  private void printPreview(final List<HashInput> inputs) {
    prompter.blankLine();
    final String fileCount = inputs.size() + pluralize(inputs.size(), " file", " files");
    prompter.line(
        "Found "
            + prompter.style().accent(fileCount)
            + " · "
            + prompter.style().accent(HumanUnits.bytes(expectedBytes(inputs))));
  }

  /** Sums already inspected file sizes for the batch preview. */
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

  /** Prints discovery diagnostics before returning to the main menu. */
  private void printPreviewFailures(final List<HashInputDiscovery.Failure> failures) {
    for (final HashInputDiscovery.Failure failure : failures) {
      prompter.error(failure.inputName() + ": " + failure.detail());
    }
  }

  /** Chooses a singular or plural noun without introducing a formatting dependency. */
  private static String pluralize(final int count, final String singular, final String plural) {
    return count == 1 ? singular : plural;
  }
}
