package io.github.camilyed.tlsh.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import org.jline.builtins.Completers.DirectoriesCompleter;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Help.Ansi;

/** Adapts JLine editing, history, and path completion to the small CLI terminal interface. */
final class JLineCliTerminal implements CliTerminal {

  private static final int MAXIMUM_LISTED_COMPLETIONS = 50;

  private final Terminal terminal;
  private final LineReader reader;
  private final PathCompleter pathCompleter;

  private JLineCliTerminal(
      final Terminal terminal, final LineReader reader, final PathCompleter pathCompleter) {
    this.terminal = terminal;
    this.reader = reader;
    this.pathCompleter = pathCompleter;
  }

  /**
   * Opens the process terminal or returns the non-interactive adapter when streams are redirected.
   *
   * <p>The explicit {@link Console} check prevents CI, pipes, and Gradle's delegated process from
   * becoming an accidental interactive session. JLine takes over only after the JVM confirms that a
   * real console is attached.
   */
  static CliTerminal create() {
    if (System.console() == null) {
      return CliTerminal.nonInteractive();
    }
    try {
      final Terminal terminal = TerminalBuilder.builder().system(true).build();
      final PathCompleter pathCompleter = new PathCompleter();
      final LineReader reader =
          LineReaderBuilder.builder()
              .appName("tlsh")
              .terminal(terminal)
              .completer(pathCompleter)
              .option(LineReader.Option.CASE_INSENSITIVE, true)
              .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
              .option(LineReader.Option.MENU_COMPLETE, true)
              .variable(LineReader.DISABLE_COMPLETION, true)
              .variable(LineReader.LIST_MAX, MAXIMUM_LISTED_COMPLETIONS)
              .build();
      reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference(LineReader.COMPLETE_WORD), "\t");
      return new JLineCliTerminal(terminal, reader, pathCompleter);
    } catch (final IOException | RuntimeException _) {
      return CliTerminal.nonInteractive();
    }
  }

  @Override
  public boolean interactive() {
    return true;
  }

  @Override
  public Ansi ansi() {
    return Ansi.ON;
  }

  @Override
  public String readLine(final String prompt) {
    pathCompleter.disable();
    reader.setVariable(LineReader.DISABLE_COMPLETION, true);
    return read(prompt);
  }

  @Override
  public String readPath(final String prompt, final PathCompletionMode completionMode) {
    pathCompleter.enable(completionMode);
    reader.setVariable(LineReader.DISABLE_COMPLETION, false);
    try {
      return read(prompt);
    } finally {
      reader.setVariable(LineReader.DISABLE_COMPLETION, true);
      pathCompleter.disable();
    }
  }

  /** Distinguishes Ctrl-C cancellation from Ctrl-D end-of-input for the guided shell. */
  private String read(final String prompt) {
    try {
      return reader.readLine(prompt);
    } catch (final UserInterruptException _) {
      throw new InteractiveCancellationException();
    } catch (final EndOfFileException _) {
      return null;
    }
  }

  /** Restores terminal attributes and releases the provider-specific terminal implementation. */
  @Override
  public void close() {
    try {
      terminal.close();
    } catch (final IOException _) {
      // Closing the process terminal is best-effort; command results have already been produced.
    }
  }

  /** Selects the completion algorithm requested by the currently active path prompt. */
  private static final class PathCompleter implements Completer {

    private final Completer filesAndDirectories = new FileNameCompleter();
    private final Completer directoriesOnly = new DirectoriesCompleter(Path.of(""));
    private PathCompletionMode mode;

    @Override
    public void complete(
        final LineReader reader,
        final ParsedLine line,
        final java.util.List<Candidate> candidates) {
      if (mode == PathCompletionMode.FILES_AND_DIRECTORIES) {
        filesAndDirectories.complete(reader, line, candidates);
      } else if (mode == PathCompletionMode.DIRECTORIES_ONLY) {
        directoriesOnly.complete(reader, line, candidates);
      }
    }

    private void enable(final PathCompletionMode completionMode) {
      mode = completionMode;
    }

    private void disable() {
      mode = null;
    }
  }
}
