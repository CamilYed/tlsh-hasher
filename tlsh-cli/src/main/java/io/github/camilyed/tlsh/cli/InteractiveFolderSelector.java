package io.github.camilyed.tlsh.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Collects and validates the folder scope shared by interactive folder-based workflows. */
final class InteractiveFolderSelector {

  private static final String BACK_HINT = "Leave empty to return to the menu.";

  private final InteractivePrompter prompter;

  InteractiveFolderSelector(final InteractivePrompter prompter) {
    this.prompter = prompter;
  }

  /**
   * Reads one folder and traversal scope, then discovers the exact visible files once for preview.
   *
   * <p>An empty result represents cancellation, invalid input, or an empty selection. The method
   * already explains invalid and empty selections to the user, so callers can return directly.
   */
  Optional<InteractiveFolderSelection> select(final String fileInsteadOfFolderMessage) {
    printPathHints();
    final Optional<Path> selectedPath =
        prompter.path("Folder path: ", PathCompletionMode.DIRECTORIES_ONLY);
    if (selectedPath.isEmpty()) {
      return Optional.empty();
    }

    final Path directory = selectedPath.orElseThrow();
    if (!validDirectory(directory, fileInsteadOfFolderMessage)) {
      return Optional.empty();
    }
    final Optional<Boolean> recursive = selectScope();
    if (recursive.isEmpty()) {
      return Optional.empty();
    }

    final boolean includeNestedDirectories = recursive.orElseThrow();
    final HashInputDiscovery.Result discovery =
        new HashInputDiscovery()
            .discover(List.of(directory.toString()), includeNestedDirectories, false);
    if (discovery.inputs().isEmpty()) {
      printEmptyPreview(discovery);
      return Optional.empty();
    }
    return Optional.of(
        new InteractiveFolderSelection(directory, includeNestedDirectories, discovery));
  }

  /** Formats the visible hidden-entry policy consistently in every folder preview. */
  String hiddenPreviewSuffix(final InteractiveFolderSelection selection) {
    final int skippedHiddenEntries = selection.discovery().skippedHiddenEntries();
    return skippedHiddenEntries == 0
        ? ""
        : " · "
            + prompter
                .style()
                .muted(
                    skippedHiddenEntries
                        + pluralize(
                            skippedHiddenEntries,
                            " hidden entry skipped",
                            " hidden entries skipped"));
  }

  /** Shows the working directory and the three convenient ways to provide a folder. */
  private void printPathHints() {
    prompter.hint("Current directory: " + Path.of("").toAbsolutePath());
    prompter.hint("Type a path, drag a folder here, or press Tab to complete it. " + BACK_HINT);
  }

  /** Distinguishes a mistaken file selection from a missing or non-directory path. */
  private boolean validDirectory(final Path directory, final String fileInsteadOfFolderMessage) {
    if (Files.isRegularFile(directory)) {
      prompter.error(fileInsteadOfFolderMessage);
      return false;
    }
    if (!Files.isDirectory(directory)) {
      prompter.error("No directory exists at: " + directory);
      return false;
    }
    return true;
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

  /** Explains either a discovery failure or an intentionally empty visible selection. */
  private void printEmptyPreview(final HashInputDiscovery.Result discovery) {
    for (final HashInputDiscovery.Failure failure : discovery.failures()) {
      prompter.error(failure.inputName() + ": " + failure.detail());
    }
    if (discovery.failures().isEmpty()) {
      final int skippedHiddenEntries = discovery.skippedHiddenEntries();
      final String hiddenDescription =
          skippedHiddenEntries == 0
              ? ""
              : " · "
                  + skippedHiddenEntries
                  + pluralize(
                      skippedHiddenEntries, " hidden entry skipped", " hidden entries skipped");
      prompter.line(prompter.style().muted("No visible files found" + hiddenDescription + "."));
    }
  }

  /** Chooses singular or plural wording for a counter. */
  private static String pluralize(final long count, final String singular, final String plural) {
    return count == 1L ? singular : plural;
  }
}
