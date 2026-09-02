package io.github.camilyed.tlsh.cli;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Owns the persistent interactive menu while specialized workflows perform individual actions. */
final class InteractiveShell {

  private final InteractivePrompter prompter;
  private final List<InteractiveAction> actions;

  InteractiveShell(final TlshCli cli, final CliTerminal terminal, final PrintWriter output) {
    prompter = new InteractivePrompter(terminal, output);
    actions =
        List.of(
            new InteractiveFileHashAction(cli, prompter),
            new InteractiveFolderHashAction(cli, prompter),
            new InteractiveFileComparisonAction(cli, prompter),
            new InteractiveSimilarityAction(cli, prompter),
            new InteractiveExitAction(prompter));
  }

  /** Keeps displaying the menu until Exit, Ctrl-D, or end of input explicitly ends the session. */
  int run() {
    printWelcome();
    while (true) {
      try {
        printMenu();
        final String answer = prompter.answer("Choose an action [1]: ");
        if (answer == null) {
          return closeFromEndOfInput();
        }

        final String selection = answer.strip().toLowerCase(Locale.ROOT);
        final Optional<InteractiveAction> selectedAction = findAction(selection);
        if (selectedAction.isEmpty()) {
          prompter.error("Unknown choice. Select one of the displayed actions.");
        } else {
          final InteractiveAction action = selectedAction.orElseThrow();
          action.execute();
          if (action.closesShell()) {
            return TlshCli.SUCCESS;
          }
        }
      } catch (final InteractiveCancellationException _) {
        prompter.line(prompter.style().muted("Cancelled."));
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
    for (final InteractiveAction action : actions) {
      printAction(action.key(), action.description());
    }
    prompter.blankLine();
  }

  /** Prints one aligned menu row. */
  private void printAction(final String key, final String description) {
    prompter.line("  " + prompter.style().accent(key) + "  " + description);
  }

  /** Finds one command without teaching the shell about action-specific aliases. */
  private Optional<InteractiveAction> findAction(final String selection) {
    return actions.stream().filter(action -> action.matches(selection)).findFirst();
  }

  /** Treats Ctrl-D as the same successful outcome as the explicit Exit command. */
  private int closeFromEndOfInput() {
    prompter.line(prompter.style().muted("Bye."));
    return TlshCli.SUCCESS;
  }
}
