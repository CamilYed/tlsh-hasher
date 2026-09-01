package io.github.camilyed.tlsh.cli;

import java.util.Set;

/** One discoverable command in the persistent interactive menu. */
interface InteractiveAction {

  /** Returns the stable numeric key displayed beside the action. */
  String key();

  /** Returns the human-readable menu label. */
  String description();

  /** Returns optional words or letters that select the same action. */
  Set<String> aliases();

  /** Performs the action after the shell has selected it. */
  void execute();

  /** Returns whether executing this action should close the persistent shell. */
  default boolean closesShell() {
    return false;
  }

  /** Matches either the displayed key or one documented textual alias. */
  default boolean matches(final String selection) {
    return key().equals(selection) || aliases().contains(selection);
  }
}
