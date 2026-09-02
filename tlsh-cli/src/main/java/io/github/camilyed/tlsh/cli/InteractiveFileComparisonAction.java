package io.github.camilyed.tlsh.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/** Guides a human through selecting and comparing two regular files. */
final class InteractiveFileComparisonAction implements InteractiveAction {

  private final TlshCli cli;
  private final InteractivePrompter prompter;

  InteractiveFileComparisonAction(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
  }

  /** Selects two files, chooses the distance mode, and presents a descriptive comparison. */
  @Override
  public void execute() {
    prompter.blankLine();
    prompter.heading("Compare two files");
    prompter.hint("Choose two files. Leave either path empty to return to the menu.");
    final Optional<Path> first = regularFile("First file: ");
    if (first.isEmpty()) {
      return;
    }
    final Optional<Path> second = regularFile("Second file: ");
    if (second.isEmpty()) {
      return;
    }
    prompter.hint(
        "TLSH stores file size as an approximate range; a different range normally raises the score.");
    final String includeSizeAnswer =
        prompter.answer("Include approximate file-size difference in score? [Y/n]: ");
    if (includeSizeAnswer == null) {
      return;
    }
    final boolean ignoreLength = !prompter.yesByDefault(includeSizeAnswer);

    try {
      final FileComparison comparison =
          cli.compareFiles(
              new FileComparisonRequest(first.orElseThrow(), second.orElseThrow(), ignoreLength));
      print(comparison);
    } catch (final FileComparisonException exception) {
      prompter.error(exception.inputName() + ": " + TlshCli.message(exception));
    }
  }

  @Override
  public String key() {
    return "3";
  }

  @Override
  public String description() {
    return "Compare two files";
  }

  @Override
  public Set<String> aliases() {
    return Set.of("compare", "comparison", "c");
  }

  /** Rejects directories before asking for the second side of the comparison. */
  private Optional<Path> regularFile(final String prompt) {
    final Optional<Path> selected = prompter.path(prompt, PathCompletionMode.FILES_AND_DIRECTORIES);
    if (selected.isEmpty()) {
      return Optional.empty();
    }
    final Path path = selected.orElseThrow();
    if (!Files.isRegularFile(path)) {
      prompter.error("No regular file exists at: " + path);
      return Optional.empty();
    }
    return Optional.of(path);
  }

  /** Presents human context without changing the numeric contract of the explicit command. */
  private void print(final FileComparison comparison) {
    prompter.blankLine();
    prompter.heading("Comparison");
    prompter.line(
        "  Distance  " + prompter.style().accent(Integer.toString(comparison.distance())));
    prompter.line("  File size " + (comparison.ignoredLength() ? "ignored" : "included"));
    printDigest("First", comparison.firstPath(), comparison.firstDigest().encoded());
    printDigest("Second", comparison.secondPath(), comparison.secondDigest().encoded());
    prompter.hint(
        comparison.distance() == 0
            ? "The digests are identical; this does not prove byte-for-byte equality."
            : "Smaller distances indicate greater similarity; the score is not a percentage.");
  }

  /** Prints one path and its complete canonical digest on separate scan-friendly lines. */
  private void printDigest(final String label, final Path path, final String digest) {
    prompter.line("  " + label + "     " + path);
    prompter.line("            " + digest);
  }
}
