package io.github.camilyed.tlsh.cli;

import java.io.Console;
import java.util.Objects;
import picocli.CommandLine.Help.Ansi;

/** Describes capabilities that differ between a human terminal and redirected process streams. */
interface CliTerminal {

  /** Returns whether it is safe to ask the user questions and wait for answers. */
  boolean interactive();

  /** Selects Picocli's ANSI policy for help text and other terminal presentation. */
  Ansi ansi();

  /** Displays a prompt and reads one answer from a human terminal. */
  String readLine(String prompt);

  /** Creates the terminal adapter used by the real command-line process. */
  static CliTerminal system() {
    final Console console = System.console();
    return console == null ? nonInteractive() : new SystemCliTerminal(console);
  }

  /** Creates a safe adapter for tests, pipes, IDE output windows, and background processes. */
  static CliTerminal nonInteractive() {
    return NonInteractiveCliTerminal.INSTANCE;
  }

  /** Uses {@link Console} so prompts do not accidentally consume bytes intended for hashing. */
  final class SystemCliTerminal implements CliTerminal {

    private final Console console;

    private SystemCliTerminal(final Console console) {
      this.console = Objects.requireNonNull(console, "console");
    }

    @Override
    public boolean interactive() {
      return true;
    }

    @Override
    public Ansi ansi() {
      return Ansi.AUTO;
    }

    @Override
    public String readLine(final String prompt) {
      return console.readLine("%s", prompt);
    }
  }

  /** Never blocks while no real console is attached. */
  enum NonInteractiveCliTerminal implements CliTerminal {
    INSTANCE;

    @Override
    public boolean interactive() {
      return false;
    }

    @Override
    public Ansi ansi() {
      return Ansi.OFF;
    }

    @Override
    public String readLine(final String prompt) {
      throw new IllegalStateException("a non-interactive terminal cannot read answers");
    }
  }
}
