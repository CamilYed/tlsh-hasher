package io.github.camilyed.tlsh.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Guides a bounded all-pairs similarity scan without hiding its quadratic comparison cost. */
final class InteractiveSimilarityAction implements InteractiveAction {

  private final TlshCli cli;
  private final InteractivePrompter prompter;

  InteractiveSimilarityAction(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
  }

  /** Collects the directory and threshold, previews the work, and asks before starting it. */
  @Override
  public void execute() {
    prompter.blankLine();
    prompter.heading("Find similar files");
    printPathHints();
    final Optional<Path> selectedPath =
        prompter.path("Folder path: ", PathCompletionMode.DIRECTORIES_ONLY);
    if (selectedPath.isEmpty()) {
      return;
    }

    final Path directory = selectedPath.orElseThrow();
    if (!validDirectory(directory)) {
      return;
    }
    final Optional<Boolean> recursive = selectScope();
    if (recursive.isEmpty()) {
      return;
    }
    final Optional<Integer> maximumDistance = selectMaximumDistance();
    if (maximumDistance.isEmpty()) {
      return;
    }
    final String ignoreLengthAnswer = prompter.answer("Ignore input-length difference? [y/N]: ");
    if (ignoreLengthAnswer == null) {
      return;
    }

    final HashInputDiscovery.Result preview =
        new HashInputDiscovery()
            .discover(List.of(directory.toString()), recursive.orElseThrow(), false);
    if (preview.inputs().isEmpty()) {
      printEmptyPreview(preview);
      return;
    }
    final long comparisons = SimilarityScanUseCase.pairCount(preview.inputs().size());
    if (comparisons > SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS) {
      prompter.error(
          "This folder requires "
              + comparisons
              + " comparisons; the interactive safety limit is "
              + SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS
              + ". Use `tlsh similar --max-comparisons N` to raise it deliberately.");
      return;
    }

    printPreview(preview, comparisons, maximumDistance.orElseThrow());
    if (!prompter.yesByDefault(prompter.answer("Start similarity scan? [Y/n]: "))) {
      prompter.line(prompter.style().muted("Cancelled."));
      return;
    }

    prompter.blankLine();
    cli.findSimilar(
        new SimilarityScanRequest(
            directory.toAbsolutePath(),
            recursive.orElseThrow(),
            false,
            maximumDistance.orElseThrow(),
            prompter.yes(ignoreLengthAnswer),
            SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS));
  }

  @Override
  public String key() {
    return "4";
  }

  @Override
  public String description() {
    return "Find similar files";
  }

  @Override
  public Set<String> aliases() {
    return Set.of("similar", "scan", "find");
  }

  /** Rejects files and missing paths with a workflow-specific correction. */
  private boolean validDirectory(final Path directory) {
    if (Files.isRegularFile(directory)) {
      prompter.error(
          "That path is a file. Similarity scanning needs a folder with at least two files.");
      return false;
    }
    if (!Files.isDirectory(directory)) {
      prompter.error("No directory exists at: " + directory);
      return false;
    }
    return true;
  }

  /** Shows completion and working-directory hints before the path prompt. */
  private void printPathHints() {
    prompter.hint("Current directory: " + Path.of("").toAbsolutePath());
    prompter.hint(
        "Type a path, drag a folder here, or press Tab to complete it. "
            + "Leave empty to return to the menu.");
  }

  /** Selects shallow or recursive discovery while refusing unknown answers. */
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

  /** Parses a nonnegative distance; zero is intentionally the safest default. */
  private Optional<Integer> selectMaximumDistance() {
    final String answer = prompter.answer("Maximum TLSH distance [0]: ");
    if (answer == null) {
      return Optional.empty();
    }
    if (answer.isBlank()) {
      return Optional.of(0);
    }
    try {
      final int distance = Integer.parseInt(answer.strip());
      if (distance >= 0) {
        return Optional.of(distance);
      }
    } catch (final NumberFormatException exception) {
      // The common message below deliberately avoids exposing parsing internals.
    }
    prompter.error("Distance must be a whole number equal to or greater than zero.");
    return Optional.empty();
  }

  /** Displays both file-reading work and the all-pairs comparison cost. */
  private void printPreview(
      final HashInputDiscovery.Result preview, final long comparisons, final int maximumDistance) {
    prompter.blankLine();
    final int fileCount = preview.inputs().size();
    prompter.line(
        "Found "
            + prompter.style().accent(fileCount + pluralize(fileCount, " file", " files"))
            + " · "
            + prompter
                .style()
                .accent(comparisons + pluralize(comparisons, " comparison", " comparisons"))
            + " · maximum distance "
            + prompter.style().accent(Integer.toString(maximumDistance))
            + hiddenPreviewSuffix(preview.skippedHiddenEntries()));
    if (maximumDistance == 0) {
      prompter.hint("Distance 0 means the same TLSH digest, not proof of identical bytes.");
    }
  }

  /** Explains discovery failures or a folder with no visible files at the selected depth. */
  private void printEmptyPreview(final HashInputDiscovery.Result preview) {
    for (final HashInputDiscovery.Failure failure : preview.failures()) {
      prompter.error(failure.inputName() + ": " + failure.detail());
    }
    if (preview.failures().isEmpty()) {
      prompter.line(
          prompter
              .style()
              .muted(
                  "No visible files found"
                      + hiddenPreviewSuffix(preview.skippedHiddenEntries())
                      + "."));
    }
  }

  /** Exposes hidden-file filtering in the preview instead of silently omitting entries. */
  private String hiddenPreviewSuffix(final int skippedHiddenEntries) {
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

  /** Selects singular grammar for both integer and long counters. */
  private static String pluralize(final long count, final String singular, final String plural) {
    return count == 1L ? singular : plural;
  }
}
