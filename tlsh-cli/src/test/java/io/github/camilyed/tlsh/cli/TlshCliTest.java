package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TlshCliTest extends CliTestSupport {

  @Test
  void shouldDisplayRootHelpWhenNoCommandIsSupplied() {
    final int exitCode = cli(new byte[0]).execute();

    assertThat(exitCode).isZero();
    assertThat(output()).contains("Usage: tlsh", "hash", "similar", "compare", "distance");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldDisplayBuildVersion() {
    final int exitCode = cli(new byte[0]).execute("--version");

    assertThat(exitCode).isZero();
    assertThat(output()).containsPattern("tlsh (0\\.1\\.0-SNAPSHOT|development)");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldExplainHashDiscoveryAndPresentationOptions() {
    final int exitCode = cli(new byte[0]).execute("hash", "--help");

    assertThat(exitCode).isZero();
    assertThat(normalizedOutput())
        .contains(
            "Calculate T1 digests",
            "skips hidden entries",
            "explicitly named hidden file is always processed",
            "ALWAYS forces progress",
            "failure details are still printed");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldExplainTheDifferenceBetweenFileAndDigestComparison() {
    final int compareExitCode = cli(new byte[0]).execute("compare", "--help");

    assertThat(compareExitCode).isZero();
    assertThat(normalizedOutput())
        .contains(
            "Hash two regular files",
            "score is not a percentage",
            "without the approximate input-length contribution");

    createOutputStreams();
    final int distanceExitCode = cli(new byte[0]).execute("distance", "--help");

    assertThat(distanceExitCode).isZero();
    assertThat(normalizedOutput())
        .contains(
            "two existing canonical T1 digests",
            "does not open files",
            "use compare when file paths are available");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldExplainSimilarityThresholdAndCostControls() {
    final int exitCode = cli(new byte[0]).execute("similar", "--help");

    assertThat(exitCode).isZero();
    assertThat(normalizedOutput())
        .contains(
            "Each file is hashed once",
            "Distance 0 means equal TLSH digests",
            "--max-distance=N",
            "--max-comparisons=N",
            "ALWAYS forces progress");
    assertThat(error()).isEmpty();
  }
}
