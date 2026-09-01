package io.github.camilyed.tlsh.cli;

import java.util.Set;

/** Ends a guided session successfully without inheriting an earlier operation's exit code. */
final class InteractiveExitAction implements InteractiveAction {

  private final InteractivePrompter prompter;

  InteractiveExitAction(final InteractivePrompter prompter) {
    this.prompter = prompter;
  }

  @Override
  public String key() {
    return "5";
  }

  @Override
  public String description() {
    return "Exit";
  }

  @Override
  public Set<String> aliases() {
    return Set.of("quit", "exit", "q");
  }

  @Override
  public void execute() {
    prompter.line(prompter.style().muted("Bye."));
  }

  @Override
  public boolean closesShell() {
    return true;
  }
}
