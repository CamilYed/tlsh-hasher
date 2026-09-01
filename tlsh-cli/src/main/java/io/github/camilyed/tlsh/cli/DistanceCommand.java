package io.github.camilyed.tlsh.cli;

import io.github.camilyed.tlsh.TlshDigest;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** Calculates the TLSH difference score between two canonical digest strings. */
@Command(
    name = "distance",
    description = "Compare two canonical T1 digests.",
    mixinStandardHelpOptions = true)
final class DistanceCommand implements Callable<Integer> {

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
      parent.output().println(distance);
      return TlshCli.SUCCESS;
    } catch (final IllegalArgumentException exception) {
      parent.error().println("tlsh: invalid digest: " + TlshCli.message(exception));
      return TlshCli.DATA_ERROR;
    }
  }
}
