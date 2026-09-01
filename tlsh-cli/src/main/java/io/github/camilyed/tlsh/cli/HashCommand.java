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
    description = {
      "Calculate T1 digests for files or folders. Use '-' to read standard input.",
      "Directory discovery skips hidden entries unless --include-hidden is selected."
    },
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
      description = "Include nested directories without following symbolic directories.")
  private boolean recursive;

  @Option(
      names = "--include-hidden",
      description = {
        "Include hidden files found in directories and descend into hidden directories.",
        "An explicitly named hidden file is always processed."
      })
  private boolean includeHidden;

  @Option(
      names = "--progress",
      defaultValue = "AUTO",
      paramLabel = "MODE",
      description = {
        "Progress display: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
        "AUTO requires a terminal; ALWAYS forces progress; NEVER disables it."
      })
  private ProgressMode progressMode;

  @Option(
      names = "--no-summary",
      description = "Hide the normal batch summary; failure details are still printed.")
  private boolean noSummary;

  /** Maps parsed command options onto the shared hashing use case. */
  @Override
  public Integer call() {
    return parent.hash(
        new HashBatchRequest(inputNames, recursive, includeHidden, progressMode, !noSummary));
  }
}
