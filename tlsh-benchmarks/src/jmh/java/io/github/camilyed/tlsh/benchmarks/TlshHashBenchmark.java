package io.github.camilyed.tlsh.benchmarks;

import io.github.camilyed.tlsh.Tlsh;
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
 * Measures the complete public {@link Tlsh#hash(byte[])} operation for several input sizes.
 *
 * <p>The input is generated once before measurements begin. Its construction therefore does not
 * contribute to the reported hashing time. A deterministic generator gives every run the same
 * bytes, while still producing enough different local patterns for a valid TLSH digest.
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

  /** Creates the deterministic input outside the measured part of the benchmark. */
  @Setup(Level.Trial)
  public void createInput() {
    input = deterministicBytes(inputSize);
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

  private static byte[] deterministicBytes(final int size) {
    final byte[] bytes = new byte[size];
    int state = 0x6D2B79F5;
    for (int index = 0; index < bytes.length; index++) {
      state ^= state << 13;
      state ^= state >>> 17;
      state ^= state << 5;
      bytes[index] = (byte) state;
    }
    return bytes;
  }
}
