package io.github.camilyed.tlsh.cli;

import picocli.CommandLine.Help.Ansi;

/** Describes capabilities that differ between a human terminal and redirected process streams. */
interface CliTerminal extends AutoCloseable {

  /** Returns whether it is safe to ask the user questions and wait for answers. */
  boolean interactive();

  /** Selects Picocli's ANSI policy for help text and other terminal presentation. */
  Ansi ansi();

  /** Displays a prompt and reads one answer with normal line editing but no path suggestions. */
  String readLine(String prompt);

  /** Displays a prompt with filesystem completion enabled for the Tab key. */
  default String readPath(final String prompt) {
    return readLine(prompt);
  }

  /** Creates the line-editing terminal used by the real command-line process. */
  static CliTerminal system() {
    return JLineCliTerminal.create();
  }

  /** Creates a safe adapter for tests, pipes, IDE output windows, and background processes. */
  static CliTerminal nonInteractive() {
    return NonInteractiveCliTerminal.INSTANCE;
  }

  /** Allows callers to close every terminal implementation uniformly. */
  @Override
  default void close() {}

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
