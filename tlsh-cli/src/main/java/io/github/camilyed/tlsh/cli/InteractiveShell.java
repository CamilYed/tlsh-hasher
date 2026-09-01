package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import java.util.Locale;

/** Owns the persistent interactive menu while specialized workflows perform individual actions. */
final class InteractiveShell {

  private final InteractivePrompter prompter;
  private final InteractiveFileHashWorkflow fileHashWorkflow;
  private final InteractiveFolderHashWorkflow folderHashWorkflow;
  private final InteractiveDistanceWorkflow distanceWorkflow;

  InteractiveShell(final TlshCli cli, final CliTerminal terminal, final PrintWriter output) {
    prompter = new InteractivePrompter(terminal, output);
    fileHashWorkflow = new InteractiveFileHashWorkflow(cli, prompter);
    folderHashWorkflow = new InteractiveFolderHashWorkflow(cli, prompter);
    distanceWorkflow = new InteractiveDistanceWorkflow(cli, prompter);
  }

  /** Keeps displaying the menu until Exit, Ctrl-D, or end of input explicitly ends the session. */
  int run() {
    printWelcome();
    while (true) {
      printMenu();
      final String answer = prompter.answer("Choose an action [1]: ");
      if (answer == null) {
        return leave();
      }

      final String selection = answer.strip().toLowerCase(Locale.ROOT);
      switch (selection) {
        case "", "1", "file", "f" -> fileHashWorkflow.run();
        case "2", "folder", "directory" -> folderHashWorkflow.run();
        case "3", "distance", "compare", "d", "c" -> distanceWorkflow.run();
        case "4", "quit", "exit", "q" -> {
          return leave();
        }
        default -> prompter.error("Unknown choice. Select 1, 2, 3, or 4.");
      }
      prompter.blankLine();
    }
  }

  /** Prints the product identity once at the beginning of the session. */
  private void printWelcome() {
    prompter.line("╭────────────────────────────────────────────╮");
    prompter.line(
        "│  " + prompter.style().heading("TLSH") + " · find similarity beyond exact bytes │");
    prompter.line("╰────────────────────────────────────────────╯");
  }

  /** Uses color only as a visual aid, keeping numeric choices usable in every terminal. */
  private void printMenu() {
    prompter.blankLine();
    printAction("1", "Hash one file");
    printAction("2", "Hash a folder");
    printAction("3", "Compare two digests");
    printAction("4", "Exit");
    prompter.blankLine();
  }

  /** Prints one aligned menu row. */
  private void printAction(final String key, final String description) {
    prompter.line("  " + prompter.style().accent(key) + "  " + description);
  }

  /** An explicitly closed interactive session is successful even if an earlier file was skipped. */
  private int leave() {
    prompter.line(prompter.style().muted("Bye."));
    return TlshCli.SUCCESS;
  }
}
