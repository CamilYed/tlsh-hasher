package io.github.camilyed.tlsh.cli;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Previews one folder batch before invoking the shared hashing use case. */
final class InteractiveFolderHashAction implements InteractiveAction {

  private final TlshCli cli;
  private final InteractivePrompter prompter;
  private final InteractiveFolderSelector folderSelector;

  InteractiveFolderHashAction(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
    folderSelector = new InteractiveFolderSelector(prompter);
  }

  /** Selects traversal scope, displays its cost, confirms, and starts the batch. */
  @Override
  public void execute() {
    prompter.blankLine();
    prompter.heading("Hash a folder");
    final Optional<InteractiveFolderSelection> selected =
        folderSelector.select("That path is a file. Choose 'Hash one file' from the menu instead.");
    if (selected.isEmpty()) {
      return;
    }

    final InteractiveFolderSelection selection = selected.orElseThrow();
    printPreview(selection);
    if (!prompter.yesByDefault(prompter.answer("Start hashing? [Y/n]: "))) {
      prompter.line(prompter.style().muted("Cancelled."));
      return;
    }

    prompter.blankLine();
    cli.hash(
        new HashBatchRequest(
            List.of(selection.directory().toAbsolutePath().toString()),
            selection.recursive(),
            false,
            ProgressMode.ALWAYS,
            true));
  }

  @Override
  public String key() {
    return "2";
  }

  @Override
  public String description() {
    return "Hash a folder";
  }

  @Override
  public Set<String> aliases() {
    return Set.of("folder", "directory");
  }

  /** Highlights the amount of file data before the user confirms the operation. */
  private void printPreview(final InteractiveFolderSelection selection) {
    prompter.blankLine();
    final int fileCount = selection.inputs().size();
    prompter.line(
        "Found "
            + prompter.style().accent(fileCount + pluralize(fileCount, " file", " files"))
            + " · "
            + prompter.style().accent(HumanUnits.bytes(selection.expectedBytes()))
            + folderSelector.hiddenPreviewSuffix(selection));
  }

  /** Chooses singular or plural wording for a counter. */
  private static String pluralize(final long count, final String singular, final String plural) {
    return count == 1L ? singular : plural;
  }
}
