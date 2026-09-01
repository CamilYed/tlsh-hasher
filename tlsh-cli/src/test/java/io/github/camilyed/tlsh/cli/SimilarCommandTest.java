package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SimilarCommandTest extends CliTestSupport {

  @Test
  void shouldFindEqualDigestsByDefaultInDeterministicPathOrder(@TempDir final Path directory)
      throws IOException {
    final byte[] sharedInput = deterministicInput();
    final Path second = Files.write(directory.resolve("b.bin"), sharedInput);
    Files.write(directory.resolve("different.bin"), deterministicInput(0xC0FFEE));
    final Path first = Files.write(directory.resolve("a.bin"), sharedInput);

    final int exitCode = cli(new byte[0]).execute("similar", directory.toString());

    assertThat(exitCode).isZero();
    assertThat(output()).isEqualTo("0  " + first + "  " + second + System.lineSeparator());
    assertThat(error()).contains("✓ 3 files hashed", "3 comparisons", "1 match", "12 KiB");
  }

  @Test
  void shouldIncludePairsAtTheMaximumDistance(@TempDir final Path directory) throws IOException {
    final byte[] firstInput = deterministicInput();
    final byte[] secondInput = deterministicInput(0xC0FFEE);
    final Path first = Files.write(directory.resolve("first.bin"), firstInput);
    final Path second = Files.write(directory.resolve("second.bin"), secondInput);
    final int distance = Tlsh.hash(firstInput).distanceTo(Tlsh.hash(secondInput));

    final int exitCode =
        cli(new byte[0]).execute("similar", "--max-distance=" + distance, directory.toString());

    assertThat(exitCode).isZero();
    assertThat(output())
        .isEqualTo(distance + "  " + first + "  " + second + System.lineSeparator());
    assertThat(error()).contains("1 comparison", "1 match");
  }

  @Test
  void shouldRefuseTooManyPairsBeforeHashing(@TempDir final Path directory) throws IOException {
    Files.write(directory.resolve("a.bin"), deterministicInput());
    Files.write(directory.resolve("b.bin"), deterministicInput());
    Files.write(directory.resolve("c.bin"), deterministicInput());

    final int exitCode =
        cli(new byte[0]).execute("similar", "--max-comparisons=2", directory.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains("scan requires 3 comparisons", "--max-comparisons limit of 2")
        .doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldContinueAfterAnIneligibleFile(@TempDir final Path directory) throws IOException {
    final byte[] input = deterministicInput();
    final Path first = Files.write(directory.resolve("a.bin"), input);
    final Path second = Files.write(directory.resolve("b.bin"), input);
    final Path ineligible = Files.write(directory.resolve("small.bin"), new byte[32]);

    final int exitCode = cli(new byte[0]).execute("similar", directory.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEqualTo("0  " + first + "  " + second + System.lineSeparator());
    assertThat(error())
        .contains(
            "⚠ 2 of 3 files hashed",
            "1 comparison",
            "Failed files",
            "✗ " + ineligible,
            "input is 32 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldSkipHiddenAndNestedEntriesUnlessRequested(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path visible = Files.write(directory.resolve("visible.bin"), input);
    final Path hidden = Files.write(directory.resolve(".hidden.bin"), input);
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path nested = Files.write(nestedDirectory.resolve("nested.bin"), input);

    final int defaultExitCode = cli(new byte[0]).execute("similar", directory.toString());

    assertThat(defaultExitCode).isZero();
    assertThat(output()).isEmpty();
    assertThat(error()).contains("1 file hashed", "0 comparisons", "1 hidden entry skipped");

    createOutputStreams();
    final int expandedExitCode =
        cli(new byte[0])
            .execute("similar", "--recursive", "--include-hidden", directory.toString());

    assertThat(expandedExitCode).isZero();
    assertThat(output())
        .contains(
            "0  " + hidden + "  " + nested,
            "0  " + hidden + "  " + visible,
            "0  " + nested + "  " + visible);
    assertThat(error()).contains("3 files hashed", "3 comparisons", "3 matches");
  }

  @Test
  void shouldRejectNegativeLimits(@TempDir final Path directory) {
    final int distanceExitCode =
        cli(new byte[0]).execute("similar", "--max-distance=-1", directory.toString());

    assertThat(distanceExitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(error()).contains("--max-distance must be zero or greater");

    createOutputStreams();
    final int comparisonsExitCode =
        cli(new byte[0]).execute("similar", "--max-comparisons=-1", directory.toString());

    assertThat(comparisonsExitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(error()).contains("--max-comparisons must be zero or greater");
  }

  @Test
  void shouldIdentifyAMissingDirectory(@TempDir final Path directory) {
    final Path missing = directory.resolve("missing");

    final int exitCode = cli(new byte[0]).execute("similar", missing.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains("⚠ 0 of 1 files hashed", "Failed files", "✗ " + missing, "path does not exist")
        .doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldRejectAFileInsteadOfTreatingItAsAOneFileScan(@TempDir final Path directory)
      throws IOException {
    final Path file = Files.write(directory.resolve("input.bin"), deterministicInput());

    final int exitCode = cli(new byte[0]).execute("similar", file.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains("tlsh: path is a file; similar requires a directory")
        .doesNotContain("Exception", "\tat ");
  }
}
