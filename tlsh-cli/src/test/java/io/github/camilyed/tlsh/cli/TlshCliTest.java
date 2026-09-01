package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

final class TlshCliTest {

  private static final String FIRST_DIGEST =
      "T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570";
  private static final String SECOND_DIGEST =
      "T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C";

  private ByteArrayOutputStream outputBytes;
  private ByteArrayOutputStream errorBytes;

  @BeforeEach
  void createOutputStreams() {
    outputBytes = new ByteArrayOutputStream();
    errorBytes = new ByteArrayOutputStream();
  }

  @Test
  void shouldDisplayRootHelpWhenNoCommandIsSupplied() {
    // when
    final int exitCode = cli(new byte[0]).execute();

    // then
    assertThat(exitCode).isZero();
    assertThat(output()).contains("Usage: tlsh", "hash", "compare", "distance");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldDisplayBuildVersion() {
    // when
    final int exitCode = cli(new byte[0]).execute("--version");

    // then
    assertThat(exitCode).isZero();
    assertThat(output()).containsPattern("tlsh (0\\.1\\.0-SNAPSHOT|development)");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldHashFileUsingStableLineOrientedOutput(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("input.bin"), input);

    // when
    final int exitCode = cli(new byte[0]).execute("hash", inputPath.toString());

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + inputPath + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldHashStandardInputWithoutClosingOrNamingTemporaryData() {
    // given
    final byte[] input = deterministicInput();

    // when
    final int exitCode = cli(input).execute("hash", "-");

    // then
    assertThat(exitCode).isZero();
    assertThat(output()).isEqualTo(Tlsh.hash(input).encoded() + "  -" + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldRejectStandardInputSpecifiedMoreThanOnce() {
    // when
    final int exitCode = cli(deterministicInput()).execute("hash", "-", "-");

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("standard input '-' may be specified only once");
  }

  @Test
  void shouldContinueAfterMissingFileAndReturnDataError(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path missingPath = directory.resolve("missing.bin");
    final Path validPath = Files.write(directory.resolve("valid.bin"), input);

    // when
    final int exitCode =
        cli(new byte[0]).execute("hash", missingPath.toString(), validPath.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + validPath + System.lineSeparator());
    assertThat(error())
        .contains(
            "Completed with 1 failed file",
            "1 of 2 hashed",
            "Failed files",
            "✗ " + missingPath,
            "path does not exist")
        .doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldCalculateBothDistanceModes() {
    final TlshCli includingLength = cli(new byte[0]);
    assertThat(includingLength.execute("distance", FIRST_DIGEST, SECOND_DIGEST)).isZero();
    assertThat(output()).isEqualTo("766" + System.lineSeparator());

    createOutputStreams();
    final TlshCli excludingLength = cli(new byte[0]);
    assertThat(excludingLength.execute("distance", "--ignore-length", FIRST_DIGEST, SECOND_DIGEST))
        .isZero();
    assertThat(output()).isEqualTo("286" + System.lineSeparator());
  }

  @Test
  void shouldCompareTwoFilesUsingBothDistanceModes(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] firstInput = deterministicInput();
    final byte[] secondInput = deterministicInput(0xC0FFEE);
    final Path firstPath = Files.write(directory.resolve("first.bin"), firstInput);
    final Path secondPath = Files.write(directory.resolve("second.bin"), secondInput);
    final int distance = Tlsh.hash(firstInput).distanceTo(Tlsh.hash(secondInput));
    final int distanceIgnoringLength =
        Tlsh.hash(firstInput).distanceToIgnoringLength(Tlsh.hash(secondInput));

    // when
    final int includingLengthExitCode =
        cli(new byte[0]).execute("compare", firstPath.toString(), secondPath.toString());

    // then
    assertThat(includingLengthExitCode).isZero();
    assertThat(output()).isEqualTo(distance + System.lineSeparator());
    assertThat(error()).isEmpty();

    createOutputStreams();

    // when
    final int ignoringLengthExitCode =
        cli(new byte[0])
            .execute("compare", "--ignore-length", firstPath.toString(), secondPath.toString());

    // then
    assertThat(ignoringLengthExitCode).isZero();
    assertThat(output()).isEqualTo(distanceIgnoringLength + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldIdentifyAnIneligibleFileDuringComparison(@TempDir final Path directory)
      throws IOException {
    // given
    final Path validPath = Files.write(directory.resolve("valid.bin"), deterministicInput());
    final Path ineligiblePath = Files.write(directory.resolve("small.bin"), new byte[32]);

    // when
    final int exitCode =
        cli(new byte[0]).execute("compare", validPath.toString(), ineligiblePath.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains(
            "tlsh: " + ineligiblePath.toAbsolutePath(),
            "input is 32 B; TLSH requires at least 256 B")
        .doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldReturnDataErrorWithoutStackTraceForInvalidDigest() {
    // when
    final int exitCode = cli(new byte[0]).execute("distance", "invalid", SECOND_DIGEST);

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("tlsh: invalid digest:").doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldReturnUsageErrorForMissingCommandArguments() {
    // when
    final int exitCode = cli(new byte[0]).execute("distance", FIRST_DIGEST);

    // then
    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("Missing required parameter: 'SECOND'", "Usage: tlsh distance");
  }

  @Test
  void shouldHashDirectoryFilesInDeterministicOrder(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path second = Files.write(directory.resolve("b.bin"), input);
    final Path first = Files.write(directory.resolve("a.bin"), input);

    // when
    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

    // then
    assertThat(exitCode).isZero();
    assertThat(output().lines())
        .containsExactly(
            Tlsh.hash(input).encoded() + "  " + first, Tlsh.hash(input).encoded() + "  " + second);
    assertThat(error()).contains("✓ 2 files hashed", "8.0 KiB");
  }

  @Test
  void shouldSkipHiddenDirectoryEntriesByDefault(@TempDir final Path directory) throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hidden = Files.write(directory.resolve(".localized"), new byte[0]);

    // when
    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + visible + System.lineSeparator())
        .doesNotContain(hidden.toString());
    assertThat(error()).contains("✓ 1 file hashed", "1 hidden entry skipped");
  }

  @Test
  void shouldIncludeHiddenDirectoryEntriesWhenRequested(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hidden = Files.write(directory.resolve(".localized"), new byte[0]);

    // when
    final int exitCode =
        cli(new byte[0])
            .execute("hash", "--progress=never", "--include-hidden", directory.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).contains(visible.toString()).doesNotContain(hidden.toString());
    assertThat(error())
        .contains(
            "Completed with 1 failed file",
            "✗ " + hidden,
            "input is 0 B; TLSH requires at least 256 B")
        .doesNotContain("hidden entry skipped");
  }

  @Test
  void shouldPruneHiddenDirectoriesUnlessTheyAreIncluded(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hiddenDirectory = Files.createDirectory(directory.resolve(".private"));
    final Path hidden = Files.write(hiddenDirectory.resolve("hidden.bin"), input);

    // when
    final int defaultExitCode =
        cli(new byte[0]).execute("hash", "--progress=never", "--recursive", directory.toString());

    // then
    assertThat(defaultExitCode).isZero();
    assertThat(output()).contains(visible.toString()).doesNotContain(hidden.toString());
    assertThat(error()).contains("1 hidden entry skipped");

    createOutputStreams();

    // when
    final int includedExitCode =
        cli(new byte[0])
            .execute(
                "hash",
                "--progress=never",
                "--recursive",
                "--include-hidden",
                directory.toString());

    // then
    assertThat(includedExitCode).isZero();
    assertThat(output()).contains(visible.toString(), hidden.toString());
    assertThat(error()).doesNotContain("hidden entry skipped");
  }

  @Test
  void shouldAlwaysHashAnExplicitlySelectedHiddenFile(@TempDir final Path directory)
      throws IOException {
    // given
    final Path hidden = Files.write(directory.resolve(".selected"), new byte[0]);

    // when
    final int exitCode = cli(new byte[0]).execute("hash", hidden.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(error()).contains("✗ " + hidden, "input is 0 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldEnterNestedDirectoriesOnlyWhenRecursiveIsRequested(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path direct = Files.write(directory.resolve("direct.bin"), input);
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path nested = Files.write(nestedDirectory.resolve("nested.bin"), input);

    // when
    final int shallowExitCode =
        cli(new byte[0]).execute("hash", "--progress=never", "--no-summary", directory.toString());

    // then
    assertThat(shallowExitCode).isZero();
    assertThat(output()).contains(direct.toString()).doesNotContain(nested.toString());

    createOutputStreams();

    // when
    final int recursiveExitCode =
        cli(new byte[0])
            .execute(
                "hash", "--progress=never", "--no-summary", "--recursive", directory.toString());

    // then
    assertThat(recursiveExitCode).isZero();
    assertThat(output()).contains(direct.toString(), nested.toString());
  }

  @Test
  void shouldKeepProgressOnErrorStreamAndDigestOnOutput(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("input.bin"), input);

    // when
    final int exitCode =
        cli(new byte[0]).execute("hash", "--progress=always", "--no-summary", inputPath.toString());

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + inputPath + System.lineSeparator())
        .doesNotContain("%", "█", "░");
    assertThat(error()).contains("100%", "4.0 KiB", inputPath.getFileName().toString());
  }

  @Test
  void shouldHashOneInteractiveFileWithoutAskingAboutFolders(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("interactive file.bin"), input);
    final ScriptedTerminal terminal =
        new ScriptedTerminal("1", '"' + inputPath.toString() + '"', "4");

    // when
    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .contains("TLSH · find similarity", "Hash one file", "Hash a folder", "Bye.")
        .contains(Tlsh.hash(input).encoded() + "  " + inputPath)
        .doesNotContain("Choose scope", "nested folder");
    assertThat(terminal.prompts())
        .containsExactly("Choose an action [1]: ", "File path: ", "Choose an action [1]: ");
  }

  @Test
  void shouldPreviewAndRecursivelyHashInteractiveFolder(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path direct = Files.write(directory.resolve("direct.bin"), input);
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path nested = Files.write(nestedDirectory.resolve("nested.bin"), input);
    final ScriptedTerminal terminal = new ScriptedTerminal("2", directory.toString(), "2", "", "4");

    // when
    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .contains(
            "Hash a folder",
            "This folder and every nested folder",
            "Found 2 files · 8.0 KiB",
            direct.toString(),
            nested.toString(),
            "Bye.");
    assertThat(error()).contains("✓ 2 files hashed");
    assertThat(terminal.prompts())
        .containsExactly(
            "Choose an action [1]: ",
            "Folder path: ",
            "Choose scope [1]: ",
            "Start hashing? [Y/n]: ",
            "Choose an action [1]: ");
  }

  @Test
  void shouldCompareTwoFilesInteractively(@TempDir final Path directory) throws IOException {
    // given
    final byte[] firstInput = deterministicInput();
    final byte[] secondInput = deterministicInput(0xC0FFEE);
    final Path firstPath = Files.write(directory.resolve("first.bin"), firstInput);
    final Path secondPath = Files.write(directory.resolve("second.bin"), secondInput);
    final int expectedDistance = Tlsh.hash(firstInput).distanceTo(Tlsh.hash(secondInput));
    final ScriptedTerminal terminal =
        new ScriptedTerminal("3", firstPath.toString(), secondPath.toString(), "", "4");

    // when
    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    // then
    assertThat(exitCode).isZero();
    assertThat(output())
        .contains(
            "Compare two files",
            "Comparison",
            "Distance  " + expectedDistance,
            "Length    included",
            firstPath.toString(),
            secondPath.toString(),
            Tlsh.hash(firstInput).encoded(),
            Tlsh.hash(secondInput).encoded(),
            "Smaller distances indicate greater similarity",
            "Bye.");
    assertThat(terminal.prompts())
        .containsExactly(
            "Choose an action [1]: ",
            "First file: ",
            "Second file: ",
            "Ignore input-length difference? [y/N]: ",
            "Choose an action [1]: ");
  }

  @Test
  void shouldExplainHowToIncludeFilesFromNestedOnlyDirectory(@TempDir final Path directory)
      throws IOException {
    // given
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    Files.write(nestedDirectory.resolve("input.bin"), deterministicInput());

    // when
    final int exitCode = cli(new byte[0]).execute("hash", directory.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains(
            "Completed with 1 failed file",
            "0 of 1 hashed",
            "use --recursive to include subdirectories");
  }

  @Test
  void shouldListFailedFilesAfterSuccessfulFolderResults(@TempDir final Path directory)
      throws IOException {
    // given
    final byte[] input = deterministicInput();
    final Path validPath = Files.write(directory.resolve("valid.bin"), input);
    final Path ineligiblePath = Files.write(directory.resolve("too-small.bin"), new byte[32]);

    // when
    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

    // then
    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + validPath + System.lineSeparator());
    assertThat(error())
        .contains(
            "Completed with 1 failed file",
            "1 of 2 hashed",
            "Failed files",
            "✗ " + ineligiblePath,
            "input is 32 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldExitInteractiveShellSuccessfullyAfterAnEarlierHashFailure(
      @TempDir final Path directory) throws IOException {
    // given
    final Path ineligiblePath = Files.write(directory.resolve("too-small.bin"), new byte[32]);
    final ScriptedTerminal terminal = new ScriptedTerminal("1", ineligiblePath.toString(), "4");

    // when
    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    // then
    assertThat(exitCode).isZero();
    assertThat(output()).contains("Bye.");
    assertThat(error())
        .contains(
            "Failed files",
            "✗ " + ineligiblePath.toAbsolutePath(),
            "input is 32 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldNormalizeHomePathAfterAnIdeReplacementCharacter() {
    // when
    final Path path = InteractivePathParser.parse("\uFFFD~");

    // then
    assertThat(path).isEqualTo(Path.of(System.getProperty("user.home")));
  }

  private TlshCli cli(final byte[] input) {
    return new TlshCli(
        new ByteArrayInputStream(input),
        new PrintWriter(outputBytes, true, StandardCharsets.UTF_8),
        new PrintWriter(errorBytes, true, StandardCharsets.UTF_8));
  }

  private TlshCli interactiveCli(final byte[] input, final CliTerminal terminal) {
    return new TlshCli(
        new ByteArrayInputStream(input),
        new PrintWriter(outputBytes, true, StandardCharsets.UTF_8),
        new PrintWriter(errorBytes, true, StandardCharsets.UTF_8),
        terminal);
  }

  private String output() {
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  private String error() {
    return errorBytes.toString(StandardCharsets.UTF_8);
  }

  private static byte[] deterministicInput() {
    return deterministicInput(0x5EEDL);
  }

  private static byte[] deterministicInput(final long seed) {
    final byte[] input = new byte[4_096];
    new Random(seed).nextBytes(input);
    return input;
  }

  /** Supplies deterministic answers without claiming that redirected byte input is a console. */
  private static final class ScriptedTerminal implements CliTerminal {

    private final Queue<String> answers;
    private final List<String> prompts = new java.util.ArrayList<>();

    private ScriptedTerminal(final String... answers) {
      this.answers = new ArrayDeque<>(List.of(answers));
    }

    @Override
    public boolean interactive() {
      return true;
    }

    @Override
    public CommandLine.Help.Ansi ansi() {
      return CommandLine.Help.Ansi.OFF;
    }

    @Override
    public String readLine(final String prompt) {
      prompts.add(prompt);
      return answers.remove();
    }

    private List<String> prompts() {
      return List.copyOf(prompts);
    }
  }
}
