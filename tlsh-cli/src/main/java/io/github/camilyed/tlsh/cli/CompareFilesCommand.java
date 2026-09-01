package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** Calculates a TLSH difference score directly from two regular files. */
@Command(
    name = "compare",
    description = "Hash and compare two files.",
    mixinStandardHelpOptions = true)
final class CompareFilesCommand implements Callable<Integer> {

  @ParentCommand private TlshCli parent;

  @Option(
      names = "--ignore-length",
      description = "Do not include the encoded input-length difference.")
  private boolean ignoreLength;

  @Parameters(index = "0", paramLabel = "FIRST", description = "First regular file.")
  private Path firstPath;

  @Parameters(index = "1", paramLabel = "SECOND", description = "Second regular file.")
  private Path secondPath;

  /** Prints only the numeric score so command output remains easy to compose in scripts. */
  @Override
  public Integer call() {
    try {
      final FileComparison comparison =
          parent.compareFiles(new FileComparisonRequest(firstPath, secondPath, ignoreLength));
      parent.output().println(comparison.distance());
      return TlshCli.SUCCESS;
    } catch (final FileComparisonException exception) {
      parent.error().println("tlsh: " + exception.inputName() + ": " + TlshCli.message(exception));
      return TlshCli.DATA_ERROR;
    }
  }
}
