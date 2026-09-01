package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CompareFilesCommandTest extends CliTestSupport {

  @Test
  void shouldCompareTwoFilesUsingBothDistanceModes(@TempDir final Path directory)
      throws IOException {
    final byte[] firstInput = deterministicInput();
    final byte[] secondInput = deterministicInput(0xC0FFEE);
    final Path firstPath = Files.write(directory.resolve("first.bin"), firstInput);
    final Path secondPath = Files.write(directory.resolve("second.bin"), secondInput);
    final int distance = Tlsh.hash(firstInput).distanceTo(Tlsh.hash(secondInput));
    final int distanceIgnoringLength =
        Tlsh.hash(firstInput).distanceToIgnoringLength(Tlsh.hash(secondInput));

    final int includingLengthExitCode =
        cli(new byte[0]).execute("compare", firstPath.toString(), secondPath.toString());

    assertThat(includingLengthExitCode).isZero();
    assertThat(output()).isEqualTo(distance + System.lineSeparator());
    assertThat(error()).isEmpty();

    createOutputStreams();
    final int ignoringLengthExitCode =
        cli(new byte[0])
            .execute("compare", "--ignore-length", firstPath.toString(), secondPath.toString());

    assertThat(ignoringLengthExitCode).isZero();
    assertThat(output()).isEqualTo(distanceIgnoringLength + System.lineSeparator());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldIdentifyAnIneligibleFileDuringComparison(@TempDir final Path directory)
      throws IOException {
    final Path validPath = Files.write(directory.resolve("valid.bin"), deterministicInput());
    final Path ineligiblePath = Files.write(directory.resolve("small.bin"), new byte[32]);

    final int exitCode =
        cli(new byte[0]).execute("compare", validPath.toString(), ineligiblePath.toString());

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error())
        .contains(
            "tlsh: " + ineligiblePath.toAbsolutePath(),
            "input is 32 B; TLSH requires at least 256 B")
        .doesNotContain("Exception", "\tat ");
  }
}
