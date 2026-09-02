package io.github.camilyed.tlsh.cli;

import java.util.Optional;
import java.util.Set;

/** Guides a bounded all-pairs similarity scan without hiding its quadratic comparison cost. */
final class InteractiveSimilarityAction implements InteractiveAction {

  private final TlshCli cli;
  private final InteractivePrompter prompter;
  private final InteractiveFolderSelector folderSelector;

  InteractiveSimilarityAction(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
    folderSelector = new InteractiveFolderSelector(prompter);
  }

  /** Collects the directory and threshold, previews the work, and asks before starting it. */
  @Override
  public void execute() {
    prompter.blankLine();
    prompter.heading("Find similar files");
    final Optional<InteractiveFolderSelection> selected =
        folderSelector.select(
            "That path is a file. Similarity scanning needs a folder with at least two files.");
    if (selected.isEmpty()) {
      return;
    }

    final Optional<SimilarityPreferences> preferences = selectPreferences();
    if (preferences.isEmpty()) {
      return;
    }

    final InteractiveFolderSelection selection = selected.orElseThrow();
    if (!withinSafetyLimit(selection)) {
      return;
    }
    final SimilarityPreferences chosenPreferences = preferences.orElseThrow();
    printPreview(selection, chosenPreferences.maximumDistance());
    if (!prompter.yesByDefault(prompter.answer("Start similarity scan? [Y/n]: "))) {
      prompter.line(prompter.style().muted("Cancelled."));
      return;
    }

    prompter.blankLine();
    cli.findSimilar(toRequest(selection, chosenPreferences));
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

  /** Collects the pair-selection threshold and the desired distance mode. */
  private Optional<SimilarityPreferences> selectPreferences() {
    final Optional<Integer> maximumDistance = selectMaximumDistance();
    if (maximumDistance.isEmpty()) {
      return Optional.empty();
    }
    prompter.hint(
        "TLSH stores file size as an approximate range; a different range normally raises the score.");
    final String includeSizeAnswer =
        prompter.answer("Include approximate file-size difference in score? [Y/n]: ");
    if (includeSizeAnswer == null) {
      return Optional.empty();
    }
    return Optional.of(
        new SimilarityPreferences(
            maximumDistance.orElseThrow(), !prompter.yesByDefault(includeSizeAnswer)));
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

  /** Refuses a broad scan unless the explicit command raises the comparison guardrail. */
  private boolean withinSafetyLimit(final InteractiveFolderSelection selection) {
    final long comparisons = selection.comparisonCount();
    if (comparisons <= SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS) {
      return true;
    }
    prompter.error(
        "This folder requires "
            + comparisons
            + " comparisons; the interactive safety limit is "
            + SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS
            + ". Use `tlsh similar --max-comparisons N` to raise it deliberately.");
    return false;
  }

  /** Displays both file-reading work and the all-pairs comparison cost. */
  private void printPreview(final InteractiveFolderSelection selection, final int maximumDistance) {
    prompter.blankLine();
    final int fileCount = selection.inputs().size();
    final long comparisons = selection.comparisonCount();
    prompter.line(
        "Found "
            + prompter.style().accent(fileCount + pluralize(fileCount, " file", " files"))
            + " · "
            + prompter
                .style()
                .accent(comparisons + pluralize(comparisons, " comparison", " comparisons"))
            + " · maximum distance "
            + prompter.style().accent(Integer.toString(maximumDistance))
            + folderSelector.hiddenPreviewSuffix(selection));
    if (maximumDistance == 0) {
      prompter.hint("Distance 0 means the same TLSH digest, not proof of identical bytes.");
    }
  }

  /** Converts guided choices into the same application request used by Picocli. */
  private static SimilarityScanRequest toRequest(
      final InteractiveFolderSelection selection, final SimilarityPreferences preferences) {
    return new SimilarityScanRequest(
        selection.directory().toAbsolutePath(),
        selection.recursive(),
        false,
        preferences.maximumDistance(),
        preferences.ignoreLength(),
        SimilarCommand.DEFAULT_MAXIMUM_COMPARISONS,
        ProgressMode.ALWAYS);
  }

  /** Chooses singular or plural wording for a counter. */
  private static String pluralize(final long count, final String singular, final String plural) {
    return count == 1L ? singular : plural;
  }

  /** Immutable answers that control which pairs qualify as a match. */
  private record SimilarityPreferences(int maximumDistance, boolean ignoreLength) {}
}
