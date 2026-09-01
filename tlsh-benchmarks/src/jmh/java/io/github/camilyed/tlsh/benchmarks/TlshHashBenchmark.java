package io.github.camilyed.tlsh.benchmarks;

import io.github.camilyed.tlsh.Tlsh;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures complete public TLSH hashing operations for several input sizes.
 *
 * <p>The input is generated once before measurements begin. Its construction therefore does not
 * contribute to the reported hashing time. A deterministic generator gives every run the same
 * bytes, while still producing enough different local patterns for a valid TLSH digest. Both
 * benchmarks process identical content, which makes the overhead of the two input APIs easier to
 * compare.
 *
 * <p>JMH creates a separate instance of this state for each benchmark thread. The input size is a
 * parameter, so JMH reports an independent result for every value listed on {@link #inputSize}. The
 * result is passed to a {@link Blackhole}; otherwise the JVM could notice that nobody uses the
 * digest and remove work whose cost the benchmark is intended to measure.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TlshHashBenchmark {

  /** Number of bytes hashed by one benchmark invocation. Assigned by JMH. */
  @Param({"256", "4096", "1048576"})
  public int inputSize;

  private byte[] input;
  private ByteArrayInputStream inputStream;

  /** Creates the deterministic input and reusable stream outside the measured benchmark methods. */
  @Setup(Level.Trial)
  public void createInput() {
    input = DeterministicInput.bytes(inputSize);
    inputStream = new ByteArrayInputStream(input);
  }

  /**
   * Hashes one complete byte array and consumes the resulting immutable digest.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   */
  @Benchmark
  public void hashByteArray(final Blackhole blackhole) {
    blackhole.consume(Tlsh.hash(input));
  }

  /**
   * Hashes the same content through the public stream API and consumes the resulting digest.
   *
   * <p>The in-memory stream is created during trial setup and reset to position zero before every
   * invocation. Reusing it keeps stream construction outside the result while still measuring the
   * read loop and internal buffer allocated by {@link Tlsh#hash(java.io.InputStream)}. The method
   * does not measure filesystem or storage-device performance.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   * @throws IOException when the hashing API cannot read the stream
   */
  @Benchmark
  public void hashInputStream(final Blackhole blackhole) throws IOException {
    inputStream.reset();
    blackhole.consume(Tlsh.hash(inputStream));
  }
}
