package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class DistanceCommandTest extends CliTestSupport {

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
    final int exitCode = cli(new byte[0]).execute("distance", "invalid", SECOND_DIGEST);

    assertThat(exitCode).isEqualTo(TlshCli.DATA_ERROR);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("tlsh: invalid digest:").doesNotContain("Exception", "\tat ");
  }

  @Test
  void shouldReturnUsageErrorForMissingCommandArguments() {
    final int exitCode = cli(new byte[0]).execute("distance", FIRST_DIGEST);

    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
    assertThat(output()).isEmpty();
    assertThat(error()).contains("Missing required parameter: 'SECOND'", "Usage: tlsh distance");
  }
}
