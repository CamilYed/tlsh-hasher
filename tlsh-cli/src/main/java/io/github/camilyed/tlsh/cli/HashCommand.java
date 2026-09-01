package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** Calculates canonical TLSH digests for files, directories, or standard input. */
@Command(
    name = "hash",
    description = "Hash files or folders. Use '-' to read standard input.",
    mixinStandardHelpOptions = true)
final class HashCommand implements Callable<Integer> {

  @ParentCommand private TlshCli parent;

  @Parameters(
      arity = "1..*",
      paramLabel = "PATH",
      description = "File, directory, or '-' for stdin.")
  private List<String> inputNames;

  @Option(
      names = {"-r", "--recursive"},
      description = "Include files in all nested subdirectories.")
  private boolean recursive;

  @Option(
      names = "--progress",
      defaultValue = "AUTO",
      paramLabel = "MODE",
      description = "Progress display: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
  private ProgressMode progressMode;

  @Option(
      names = "--no-summary",
      description = "Do not print the final folder or multi-file summary.")
  private boolean noSummary;

  /** Discovers and hashes every input while continuing after individual failures. */
  @Override
  public Integer call() {
    if (standardInputCount() > 1) {
      parent.error().println("tlsh: standard input '-' may be specified only once");
      return TlshCli.DATA_ERROR;
    }

    final long startedAt = System.nanoTime();
    final HashInputDiscovery.Result discovery =
        new HashInputDiscovery().discover(inputNames, recursive);
    printDiscoveryFailures(discovery.failures());

    final long expectedBytes = expectedBytes(discovery.inputs());
    final HashProgress progress =
        HashProgress.create(
            progressMode,
            parent.terminal(),
            parent.error(),
            expectedBytes,
            discovery.inputs().size());

    int successfulFiles = 0;
    int hashingFailures = 0;
    for (int index = 0; index < discovery.inputs().size(); index++) {
      final HashInput input = discovery.inputs().get(index);
      progress.startFile(index + 1, input.displayName());
      try {
        final TlshDigest digest = hash(input, progress);
        progress.finishFile();
        progress.clearLine();
        parent.output().println(digest.encoded() + "  " + input.displayName());
        successfulFiles++;
      } catch (final IOException | IllegalArgumentException | IllegalStateException exception) {
        progress.clearLine();
        printFailure(input.displayName(), exception);
        hashingFailures++;
      }
    }
    progress.close();

    final int failureCount = discovery.failures().size() + hashingFailures;
    if (shouldPrintSummary(discovery, failureCount)) {
      printSummary(
          successfulFiles, failureCount, progress.processedBytes(), System.nanoTime() - startedAt);
    }
    return failureCount == 0 ? TlshCli.SUCCESS : TlshCli.DATA_ERROR;
  }

  /** Opens files locally while leaving ownership of process standard input with the caller. */
  private TlshDigest hash(final HashInput input, final HashProgress progress) throws IOException {
    if (input.isStandardInput()) {
      final CountingInputStream counting =
          new CountingInputStream(parent.input(), progress::advance);
      return Tlsh.hash(counting);
    }
    try (InputStream file = Files.newInputStream(input.path());
        CountingInputStream counting = new CountingInputStream(file, progress::advance)) {
      return Tlsh.hash(counting);
    }
  }

  /** Sums known file sizes or selects an indeterminate display when stdin is present. */
  private static long expectedBytes(final List<HashInput> inputs) {
    long total = 0L;
    for (final HashInput input : inputs) {
      if (input.expectedBytes() < 0L) {
        return -1L;
      }
      if (Long.MAX_VALUE - total < input.expectedBytes()) {
        return -1L;
      }
      total += input.expectedBytes();
    }
    return total;
  }

  /** Counts uses of the single process standard-input stream before duplicate removal. */
  private long standardInputCount() {
    return inputNames.stream().filter("-"::equals).count();
  }

  /** Prints path-discovery failures using the same concise diagnostics as hashing failures. */
  private void printDiscoveryFailures(final List<HashInputDiscovery.Failure> failures) {
    for (final HashInputDiscovery.Failure failure : failures) {
      parent.error().println("tlsh: " + failure.inputName() + ": " + failure.detail());
    }
  }

  /** Prints an expected user-facing failure without a Java exception class or stack trace. */
  private void printFailure(final String inputName, final Exception exception) {
    parent.error().println("tlsh: " + inputName + ": " + TlshCli.message(exception));
  }

  /** Keeps single-file script output minimal while summarizing genuinely batch-oriented work. */
  private boolean shouldPrintSummary(
      final HashInputDiscovery.Result discovery, final int failureCount) {
    return !noSummary
        && (discovery.containedDirectory() || discovery.inputs().size() > 1 || failureCount > 0);
  }

  /** Prints a compact batch outcome to stderr so stdout remains a digest data stream. */
  private void printSummary(
      final int successfulFiles,
      final int failedFiles,
      final long processedBytes,
      final long elapsedNanoseconds) {
    final String result =
        failedFiles == 0
            ? parent.terminal().ansi().string("@|green ✓|@")
            : parent.terminal().ansi().string("@|yellow !|@");
    final String failures = failedFiles == 0 ? "" : " · " + failedFiles + " failed";
    parent
        .error()
        .println(
            result
                + " "
                + successfulFiles
                + pluralize(successfulFiles, " file", " files")
                + " hashed · "
                + HumanUnits.bytes(processedBytes)
                + " · "
                + HumanUnits.duration(elapsedNanoseconds)
                + failures);
  }

  /** Chooses a singular or plural noun without introducing a formatting dependency. */
  private static String pluralize(final int count, final String singular, final String plural) {
    return count == 1 ? singular : plural;
  }
}
