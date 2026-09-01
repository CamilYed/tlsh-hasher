package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class HashCommandTest extends CliTestSupport {

  @Test
  void shouldHashFileUsingStableLineOrientedOutput(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("input.bin"), input);

    final int exitCode = cli(new byte[0]).execute("hash", inputPath.toString());

    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + inputPath + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldHashStandardInputWithoutClosingOrNamingTemporaryData() {
    final byte[] input = deterministicInput();

    final int exitCode = cli(input).execute("hash", "-");

    assertThat(exitCode).isZero();
    assertThat(output()).isEqualTo(Tlsh.hash(input).encoded() + "  -" + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldRejectStandardInputSpecifiedMoreThanOnce() {
    final int exitCode = cli(deterministicInput()).execute("hash", "-", "-");

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("standard input '-' may be specified only once");
  }

  @Test
  void shouldContinueAfterMissingFileAndReturnDataError(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path missingPath = directory.resolve("missing.bin");
    final Path validPath = Files.write(directory.resolve("valid.bin"), input);

    final int exitCode =
        cli(new byte[0]).execute("hash", missingPath.toString(), validPath.toString());

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
  void shouldHashDirectoryFilesInDeterministicOrder(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path second = Files.write(directory.resolve("b.bin"), input);
    final Path first = Files.write(directory.resolve("a.bin"), input);

    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

    assertThat(exitCode).isZero();
    assertThat(output().lines())
        .containsExactly(
            Tlsh.hash(input).encoded() + "  " + first, Tlsh.hash(input).encoded() + "  " + second);
    assertThat(error()).contains("✓ 2 files hashed", "8.0 KiB");
  }

  @Test
  void shouldSkipHiddenDirectoryEntriesByDefault(@TempDir final Path directory) throws IOException {
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hidden = Files.write(directory.resolve(".localized"), new byte[0]);

    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + visible + System.lineSeparator())
        .doesNotContain(hidden.toString());
    assertThat(error()).contains("✓ 1 file hashed", "1 hidden entry skipped");
  }

  @Test
  void shouldIncludeHiddenDirectoryEntriesWhenRequested(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hidden = Files.write(directory.resolve(".localized"), new byte[0]);

    final int exitCode =
        cli(new byte[0])
            .execute("hash", "--progress=never", "--include-hidden", directory.toString());

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
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hiddenDirectory = Files.createDirectory(directory.resolve(".private"));
    final Path hidden = Files.write(hiddenDirectory.resolve("hidden.bin"), input);

    final int defaultExitCode =
        cli(new byte[0]).execute("hash", "--progress=never", "--recursive", directory.toString());

    assertThat(defaultExitCode).isZero();
    assertThat(output()).contains(visible.toString()).doesNotContain(hidden.toString());
    assertThat(error()).contains("1 hidden entry skipped");

    createOutputStreams();
    final int includedExitCode =
        cli(new byte[0])
            .execute(
                "hash",
                "--progress=never",
                "--recursive",
                "--include-hidden",
                directory.toString());

    assertThat(includedExitCode).isZero();
    assertThat(output()).contains(visible.toString(), hidden.toString());
    assertThat(error()).doesNotContain("hidden entry skipped");
  }

  @Test
  void shouldAlwaysHashAnExplicitlySelectedHiddenFile(@TempDir final Path directory)
      throws IOException {
    final Path hidden = Files.write(directory.resolve(".selected"), new byte[0]);

    final int exitCode = cli(new byte[0]).execute("hash", hidden.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(error()).contains("✗ " + hidden, "input is 0 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldEnterNestedDirectoriesOnlyWhenRecursiveIsRequested(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path direct = Files.write(directory.resolve("direct.bin"), input);
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path nested = Files.write(nestedDirectory.resolve("nested.bin"), input);

    final int shallowExitCode =
        cli(new byte[0]).execute("hash", "--progress=never", "--no-summary", directory.toString());

    assertThat(shallowExitCode).isZero();
    assertThat(output()).contains(direct.toString()).doesNotContain(nested.toString());

    createOutputStreams();
    final int recursiveExitCode =
        cli(new byte[0])
            .execute(
                "hash", "--progress=never", "--no-summary", "--recursive", directory.toString());

    assertThat(recursiveExitCode).isZero();
    assertThat(output()).contains(direct.toString(), nested.toString());
  }

  @Test
  void shouldKeepProgressOnErrorStreamAndDigestOnOutput(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("input.bin"), input);

    final int exitCode =
        cli(new byte[0]).execute("hash", "--progress=always", "--no-summary", inputPath.toString());

    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(Tlsh.hash(input).encoded() + "  " + inputPath + System.lineSeparator())
        .doesNotContain("%", "█", "░");
    assertThat(error()).contains("100%", "4.0 KiB", inputPath.getFileName().toString());
  }

  @Test
  void shouldExplainHowToIncludeFilesFromNestedOnlyDirectory(@TempDir final Path directory)
      throws IOException {
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    Files.write(nestedDirectory.resolve("input.bin"), deterministicInput());

    final int exitCode = cli(new byte[0]).execute("hash", directory.toString());

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
    final byte[] input = deterministicInput();
    final Path validPath = Files.write(directory.resolve("valid.bin"), input);
    final Path ineligiblePath = Files.write(directory.resolve("too-small.bin"), new byte[32]);

    final int exitCode = cli(new byte[0]).execute("hash", "--progress=never", directory.toString());

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
}
