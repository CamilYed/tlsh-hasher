package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** Entry point for the TLSH command-line application. */
@Command(
    name = "tlsh",
    description = "Calculate and compare TLSH similarity digests.",
    mixinStandardHelpOptions = true,
    versionProvider = TlshCli.VersionProvider.class,
    subcommands = {TlshCli.HashCommand.class, TlshCli.DistanceCommand.class})
public final class TlshCli implements Callable<Integer> {

  static final int SUCCESS = 0;
  static final int DATA_ERROR = 1;

  private final InputStream input;
  private final PrintWriter output;
  private final PrintWriter error;

  /** Creates a CLI connected to the process standard streams. */
  public TlshCli() {
    this(
        System.in,
        new PrintWriter(System.out, true, StandardCharsets.UTF_8),
        new PrintWriter(System.err, true, StandardCharsets.UTF_8));
  }

  /** Creates a CLI with replaceable streams for deterministic tests. */
  TlshCli(final InputStream input, final PrintWriter output, final PrintWriter error) {
    this.input = input;
    this.output = output;
    this.error = error;
  }

  /**
   * Executes the command and terminates the process with its documented exit code.
   *
   * @param arguments command-line arguments
   */
  public static void main(final String[] arguments) {
    final int exitCode = new TlshCli().execute(arguments);
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
    return commandLine.execute(arguments);
  }

  /** Prints root help when no subcommand was supplied. */
  @Override
  public Integer call() {
    new CommandLine(this).usage(output);
    return SUCCESS;
  }

  /** Calculates canonical digests for files or standard input. */
  @Command(
      name = "hash",
      description = "Hash files. Use '-' to read standard input.",
      mixinStandardHelpOptions = true)
  static final class HashCommand implements Callable<Integer> {

    @ParentCommand private TlshCli parent;

    @Parameters(arity = "1..*", paramLabel = "FILE", description = "File path or '-' for stdin.")
    private List<String> inputs;

    /** Hashes every input, continuing after individual failures. */
    @Override
    public Integer call() {
      if (standardInputCount() > 1) {
        parent.error.println("tlsh: standard input '-' may be specified only once");
        return DATA_ERROR;
      }

      int exitCode = SUCCESS;
      for (final String inputName : inputs) {
        try {
          final TlshDigest digest = hash(inputName);
          parent.output.println(digest.encoded() + "  " + inputName);
        } catch (final IOException | IllegalArgumentException | IllegalStateException exception) {
          parent.error.println("tlsh: " + inputName + ": " + message(exception));
          exitCode = DATA_ERROR;
        }
      }
      return exitCode;
    }

    /** Selects stream or path hashing without taking ownership of standard input. */
    private TlshDigest hash(final String inputName) throws IOException {
      return "-".equals(inputName) ? Tlsh.hash(parent.input) : Tlsh.hash(Path.of(inputName));
    }

    /** Counts uses of the single process standard-input stream. */
    private long standardInputCount() {
      return inputs.stream().filter("-"::equals).count();
    }
  }

  /** Calculates the TLSH difference score between two canonical digest strings. */
  @Command(
      name = "distance",
      description = "Compare two canonical T1 digests.",
      mixinStandardHelpOptions = true)
  static final class DistanceCommand implements Callable<Integer> {

    @ParentCommand private TlshCli parent;

    @Option(
        names = "--ignore-length",
        description = "Do not include the encoded input-length difference.")
    private boolean ignoreLength;

    @Parameters(index = "0", paramLabel = "FIRST", description = "First canonical T1 digest.")
    private String firstEncodedDigest;

    @Parameters(index = "1", paramLabel = "SECOND", description = "Second canonical T1 digest.")
    private String secondEncodedDigest;

    /** Parses both digests and prints only their numeric score for scripting. */
    @Override
    public Integer call() {
      try {
        final TlshDigest first = TlshDigest.parse(firstEncodedDigest);
        final TlshDigest second = TlshDigest.parse(secondEncodedDigest);
        final int distance =
            ignoreLength ? first.distanceToIgnoringLength(second) : first.distanceTo(second);
        parent.output.println(distance);
        return SUCCESS;
      } catch (final IllegalArgumentException exception) {
        parent.error.println("tlsh: invalid digest: " + message(exception));
        return DATA_ERROR;
      }
    }
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
  private static String message(final Exception exception) {
    final String detail = exception.getMessage();
    return detail == null || detail.isBlank() ? exception.getClass().getSimpleName() : detail;
  }
}
