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
    assertThat(output()).contains("Usage: tlsh", "hash", "distance");
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
    assertThat(error()).contains("tlsh: " + missingPath + ":").doesNotContain("Exception", "\tat ");
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

  private TlshCli cli(final byte[] input) {
    return new TlshCli(
        new ByteArrayInputStream(input),
        new PrintWriter(outputBytes, true, StandardCharsets.UTF_8),
        new PrintWriter(errorBytes, true, StandardCharsets.UTF_8));
  }

  private String output() {
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  private String error() {
    return errorBytes.toString(StandardCharsets.UTF_8);
  }

  private static byte[] deterministicInput() {
    final byte[] input = new byte[4_096];
    new Random(0x5EEDL).nextBytes(input);
    return input;
  }
}
