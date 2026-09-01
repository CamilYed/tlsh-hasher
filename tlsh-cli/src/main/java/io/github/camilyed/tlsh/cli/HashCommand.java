package io.github.camilyed.tlsh.cli;

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
      names = "--include-hidden",
      description = "Include hidden files and descend into hidden directories.")
  private boolean includeHidden;

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

  /** Maps parsed command options onto the shared hashing use case. */
  @Override
  public Integer call() {
    return parent.hash(
        new HashBatchRequest(inputNames, recursive, includeHidden, progressMode, !noSummary));
  }
}
