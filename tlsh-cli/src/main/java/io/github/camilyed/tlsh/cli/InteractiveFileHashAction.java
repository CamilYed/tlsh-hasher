package io.github.camilyed.tlsh.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Collects and validates exactly one file before invoking the shared hashing use case. */
final class InteractiveFileHashAction implements InteractiveAction {

  private static final String BACK_HINT = "Leave empty to return to the menu.";

  private final TlshCli cli;
  private final InteractivePrompter prompter;

  InteractiveFileHashAction(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
  }

  /** Hashes one selected regular file and never displays folder-specific questions. */
  public void execute() {
    prompter.blankLine();
    prompter.heading("Hash one file");
    printPathHints("file");
    final Optional<Path> selectedPath =
        prompter.path("File path: ", PathCompletionMode.FILES_AND_DIRECTORIES);
    if (selectedPath.isEmpty()) {
      return;
    }

    final Path path = selectedPath.orElseThrow();
    if (Files.isDirectory(path)) {
      prompter.error("That path is a folder. Choose 'Hash a folder' from the menu instead.");
      return;
    }
    if (!Files.isRegularFile(path)) {
      prompter.error("No regular file exists at: " + path);
      return;
    }

    prompter.line(
        "Hashing " + prompter.style().accent(path.getFileName().toString()) + " · " + size(path));
    prompter.blankLine();
    cli.hash(
        new HashBatchRequest(
            List.of(path.toAbsolutePath().toString()), false, false, ProgressMode.ALWAYS, false));
  }

  @Override
  public String key() {
    return "1";
  }

  @Override
  public String description() {
    return "Hash one file";
  }

  @Override
  public Set<String> aliases() {
    return Set.of("", "file", "f");
  }

  /** Explains both relative paths and the newly available completion key. */
  private void printPathHints(final String kind) {
    prompter.hint("Current directory: " + Path.of("").toAbsolutePath());
    prompter.hint(
        "Type a path, drag a " + kind + " here, or press Tab to complete it. " + BACK_HINT);
  }

  /** Formats a best-effort file size without turning preview metadata into a fatal operation. */
  private static String size(final Path path) {
    try {
      return HumanUnits.bytes(Files.size(path));
    } catch (final IOException exception) {
      return "size unavailable";
    }
  }
}
