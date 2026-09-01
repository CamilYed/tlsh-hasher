package io.github.camilyed.tlsh.cli;

import java.util.ArrayList;
import java.util.List;

/** Collects two digest strings and delegates their comparison to {@link DistanceCommand}. */
final class InteractiveDistanceWorkflow {

  private final TlshCli cli;
  private final InteractivePrompter prompter;

  InteractiveDistanceWorkflow(final TlshCli cli, final InteractivePrompter prompter) {
    this.cli = cli;
    this.prompter = prompter;
  }

  /** Returns to the menu when either digest is left empty. */
  void run() {
    prompter.blankLine();
    prompter.heading("Compare two digests");
    prompter.hint("Leave either digest empty to return to the menu.");
    final String first = prompter.answer("First T1 digest: ");
    if (first == null || first.isBlank()) {
      return;
    }
    final String second = prompter.answer("Second T1 digest: ");
    if (second == null || second.isBlank()) {
      return;
    }
    final boolean ignoreLength =
        prompter.yes(prompter.answer("Ignore input-length difference? [y/N]: "));

    final List<String> arguments = new ArrayList<>();
    arguments.add("distance");
    if (ignoreLength) {
      arguments.add("--ignore-length");
    }
    arguments.add(first.strip());
    arguments.add(second.strip());
    prompter.blankLine();
    cli.execute(arguments.toArray(String[]::new));
  }
}
