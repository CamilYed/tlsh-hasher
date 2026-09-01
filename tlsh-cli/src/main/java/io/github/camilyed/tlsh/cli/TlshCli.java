package io.github.camilyed.tlsh.cli;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Entry point for the interactive and script-friendly TLSH command-line application. */
@Command(
    name = "tlsh",
    header = {
      "@|bold,cyan TLSH|@  similarity hashing for files",
      "@|faint Find related content even when the bytes are not identical.|@"
    },
    description = "Hash files and folders, compare files, or compare canonical TLSH digests.",
    synopsisHeading = "%n@|bold Usage:|@ ",
    commandListHeading = "%n@|bold Commands:|@%n",
    optionListHeading = "%n@|bold Options:|@%n",
    mixinStandardHelpOptions = true,
    versionProvider = TlshCli.VersionProvider.class,
    subcommands = {HashCommand.class, CompareFilesCommand.class, DistanceCommand.class})
public final class TlshCli implements Callable<Integer>, AutoCloseable {

  static final int SUCCESS = 0;
  static final int DATA_ERROR = 1;

  private final InputStream input;
  private final PrintWriter output;
  private final PrintWriter error;
  private final CliTerminal terminal;

  @Spec private CommandSpec commandSpec;

  /** Creates a CLI connected to the process standard streams. */
  public TlshCli() {
    this(
        System.in,
        new PrintWriter(System.out, true, StandardCharsets.UTF_8),
        new PrintWriter(System.err, true, StandardCharsets.UTF_8),
        CliTerminal.system());
  }

  /** Creates a CLI with replaceable streams for deterministic tests. */
  TlshCli(final InputStream input, final PrintWriter output, final PrintWriter error) {
    this(input, output, error, CliTerminal.nonInteractive());
  }

  /** Creates a CLI with replaceable streams and terminal behavior for deterministic tests. */
  TlshCli(
      final InputStream input,
      final PrintWriter output,
      final PrintWriter error,
      final CliTerminal terminal) {
    this.input = input;
    this.output = output;
    this.error = error;
    this.terminal = terminal;
  }

  /**
   * Executes the command and terminates the process with its documented exit code.
   *
   * @param arguments command-line arguments
   */
  static void main(final String[] arguments) {
    final int exitCode;
    try (TlshCli cli = new TlshCli()) {
      exitCode = cli.execute(arguments);
    }
    if (exitCode != SUCCESS) {
      System.exit(exitCode);
    }
  }

  /**
   * Parses and executes arguments without terminating the JVM.
   *
   * @param arguments command-line arguments
   * @return zero for success, one for invalid input data or I/O, and two for invalid command syntax
   */
  int execute(final String... arguments) {
    final CommandLine commandLine = new CommandLine(this);
    commandLine.setOut(output);
    commandLine.setErr(error);
    commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(terminal.ansi()));
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    return commandLine.execute(arguments);
  }

  /** Starts the guided terminal when possible and otherwise prints nonblocking root help. */
  @Override
  public Integer call() {
    if (terminal.interactive()) {
      return new InteractiveShell(this, terminal, output).run();
    }
    commandSpec.commandLine().usage(output);
    return SUCCESS;
  }

  /** Supplies the application version from the CLI JAR manifest. */
  public static final class VersionProvider implements CommandLine.IVersionProvider {

    /** Creates the provider used reflectively by Picocli. */
    public VersionProvider() {}

    /** Returns a useful development label when classes are not running from a packaged JAR. */
    @Override
    public String[] getVersion() {
      final String implementationVersion = TlshCli.class.getPackage().getImplementationVersion();
      final String version =
          implementationVersion == null ? moduleVersion() : implementationVersion;
      return new String[] {"tlsh " + version};
    }

    /** Reads the version embedded by Gradle in the named module descriptor. */
    private static String moduleVersion() {
      final ModuleDescriptor descriptor = TlshCli.class.getModule().getDescriptor();
      return descriptor == null ? "development" : descriptor.rawVersion().orElse("development");
    }
  }

  /** Produces a nonempty one-line message for expected user-facing failures. */
  static String message(final Exception exception) {
    final String detail = exception.getMessage();
    return detail == null || detail.isBlank() ? exception.getClass().getSimpleName() : detail;
  }

  InputStream input() {
    return input;
  }

  PrintWriter output() {
    return output;
  }

  PrintWriter error() {
    return error;
  }

  CliTerminal terminal() {
    return terminal;
  }

  /** Runs the hashing use case without re-entering the Picocli argument parser. */
  int hash(final HashBatchRequest request) {
    return new HashBatchUseCase(input, output, error, terminal).execute(request);
  }

  /** Compares two files through the shared use case used by both CLI adapters. */
  FileComparison compareFiles(final FileComparisonRequest request) throws FileComparisonException {
    return new FileComparisonUseCase().execute(request);
  }

  /** Restores terminal state after an interactive session. */
  @Override
  public void close() {
    terminal.close();
  }
}
