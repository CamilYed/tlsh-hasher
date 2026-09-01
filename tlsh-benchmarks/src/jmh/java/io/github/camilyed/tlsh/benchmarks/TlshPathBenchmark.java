package io.github.camilyed.tlsh.benchmarks;

import io.github.camilyed.tlsh.Tlsh;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures the complete public {@link Tlsh#hash(Path)} operation for repeatedly read local files.
 *
 * <p>Each JMH trial creates one deterministic file before measurement and deletes it afterward.
 * File creation and input generation are therefore excluded, while every measured invocation still
 * opens the path, reads it to the end, hashes its bytes, and closes the stream.
 *
 * <p>The same file is read during warmup and measurement. The operating system is consequently
 * expected to serve most reads from its filesystem cache. This is intentionally a warm-cache API
 * benchmark: it measures path opening, stream management, copying, and TLSH calculation together.
 * It does not claim to measure cold storage-device throughput, which would require a separate,
 * platform-specific experimental design.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TlshPathBenchmark {

  /** Number of file bytes hashed by one benchmark invocation. Assigned by JMH. */
  @Param({"256", "4096", "1048576"})
  public int inputSize;

  private Path inputPath;

  /** Creates the deterministic input file outside warmup and measurement. */
  @Setup(Level.Trial)
  public void createInputFile() throws IOException {
    inputPath = Files.createTempFile("tlsh-jmh-", ".bin");
    Files.write(inputPath, DeterministicInput.bytes(inputSize));
  }

  /**
   * Opens, hashes, and closes the prepared input path and consumes the resulting digest.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   * @throws IOException when the file cannot be opened, read, or closed
   */
  @Benchmark
  public void hashPath(final Blackhole blackhole) throws IOException {
    blackhole.consume(Tlsh.hash(inputPath));
  }

  /** Deletes the temporary input file after the trial, including when a benchmark fails. */
  @TearDown(Level.Trial)
  public void deleteInputFile() throws IOException {
    Files.deleteIfExists(inputPath);
  }
}
