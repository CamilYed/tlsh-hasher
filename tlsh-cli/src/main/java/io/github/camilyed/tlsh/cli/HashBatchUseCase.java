package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Coordinates discovery, streaming hashing, progress, output, and the final batch report. */
final class HashBatchUseCase {

  private final InputStream standardInput;
  private final PrintWriter output;
  private final PrintWriter error;
  private final CliTerminal terminal;

  HashBatchUseCase(
      final InputStream standardInput,
      final PrintWriter output,
      final PrintWriter error,
      final CliTerminal terminal) {
    this.standardInput = standardInput;
    this.output = output;
    this.error = error;
    this.terminal = terminal;
  }

  /** Executes one complete hashing request while continuing after individual file failures. */
  int execute(final HashBatchRequest request) {
    if (standardInputCount(request.inputNames()) > 1) {
      error.println("tlsh: standard input '-' may be specified only once");
      return TlshCli.DATA_ERROR;
    }

    final long startedAt = System.nanoTime();
    final HashInputDiscovery.Result discovery =
        new HashInputDiscovery()
            .discover(request.inputNames(), request.recursive(), request.includeHidden());
    final List<HashFailure> failures = discoveryFailures(discovery.failures());
    final HashProgress progress = createProgress(request, discovery.inputs());

    int successfulFiles = 0;
    for (int index = 0; index < discovery.inputs().size(); index++) {
      final HashInput input = discovery.inputs().get(index);
      progress.startFile(index + 1, input.displayName());
      try {
        final TlshDigest digest = hash(input, progress);
        progress.finishFile();
        progress.clearLine();
        output.println(digest.encoded() + "  " + input.displayName());
        successfulFiles++;
      } catch (final IOException | IllegalArgumentException | IllegalStateException exception) {
        progress.clearLine();
        failures.add(
            new HashFailure(
                input.displayName(), HashFailureDetail.explain(input.expectedBytes(), exception)));
      }
    }
    progress.close();

    final HashBatchSummary summary =
        new HashBatchSummary(
            successfulFiles,
            List.copyOf(failures),
            discovery.skippedHiddenEntries(),
            progress.processedBytes(),
            System.nanoTime() - startedAt);
    new HashBatchReporter(error, terminal)
        .print(summary, shouldPrintSummary(request, discovery, failures.size()));
    return failures.isEmpty() ? TlshCli.SUCCESS : TlshCli.DATA_ERROR;
  }

  /** Configures one presentation observer after discovery reveals the total amount of work. */
  private HashProgress createProgress(
      final HashBatchRequest request, final List<HashInput> inputs) {
    return HashProgress.create(
        request.progressMode(), terminal, error, expectedBytes(inputs), inputs.size());
  }

  /** Opens files locally while leaving ownership of process standard input with the caller. */
  private TlshDigest hash(final HashInput input, final HashProgress progress) throws IOException {
    if (input.isStandardInput()) {
      final CountingInputStream counting =
          new CountingInputStream(standardInput, progress::advance);
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
      if (input.expectedBytes() < 0L || Long.MAX_VALUE - total < input.expectedBytes()) {
        return -1L;
      }
      total += input.expectedBytes();
    }
    return total;
  }

  /** Counts uses of the single process standard-input stream before duplicate removal. */
  private static long standardInputCount(final List<String> inputNames) {
    return inputNames.stream().filter("-"::equals).count();
  }

  /** Converts path-discovery failures into the common final-report representation. */
  private static List<HashFailure> discoveryFailures(
      final List<HashInputDiscovery.Failure> discoveryFailures) {
    final List<HashFailure> failures = new ArrayList<>(discoveryFailures.size());
    for (final HashInputDiscovery.Failure failure : discoveryFailures) {
      failures.add(new HashFailure(failure.inputName(), failure.detail()));
    }
    return failures;
  }

  /** Keeps single-file output minimal while summarizing genuinely batch-oriented work. */
  private static boolean shouldPrintSummary(
      final HashBatchRequest request,
      final HashInputDiscovery.Result discovery,
      final int failureCount) {
    return request.summaryEnabled()
        && (discovery.containedDirectory() || discovery.inputs().size() > 1 || failureCount > 0);
  }
}
