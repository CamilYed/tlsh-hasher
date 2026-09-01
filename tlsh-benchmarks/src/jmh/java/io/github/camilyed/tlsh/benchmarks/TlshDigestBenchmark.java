package io.github.camilyed.tlsh.benchmarks;

import io.github.camilyed.tlsh.TlshDigest;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures operations on already calculated TLSH digests separately from input hashing.
 *
 * <p>Parsing converts one canonical 72-character {@code T1} string into its structured components.
 * Distance calculation compares two structures prepared before measurement. Keeping these
 * operations separate prevents file reading and histogram construction from hiding their much
 * smaller costs.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TlshDigestBenchmark {

  private static final String FIRST_ENCODED_DIGEST =
      "T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570";
  private static final String SECOND_ENCODED_DIGEST =
      "T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C";

  private TlshDigest firstDigest;
  private TlshDigest secondDigest;

  /** Parses the comparison inputs before timing begins. */
  @Setup(Level.Trial)
  public void createDigests() {
    firstDigest = TlshDigest.parse(FIRST_ENCODED_DIGEST);
    secondDigest = TlshDigest.parse(SECOND_ENCODED_DIGEST);
  }

  /**
   * Parses one complete canonical digest and consumes the resulting value.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   */
  @Benchmark
  public void parseDigest(final Blackhole blackhole) {
    blackhole.consume(TlshDigest.parse(FIRST_ENCODED_DIGEST));
  }

  /**
   * Compares two prepared digests with the encoded-length contribution enabled.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   */
  @Benchmark
  public void distanceIncludingLength(final Blackhole blackhole) {
    blackhole.consume(firstDigest.distanceTo(secondDigest));
  }

  /**
   * Compares two prepared digests while ignoring their encoded-length difference.
   *
   * @param blackhole JMH result consumer that prevents dead-code elimination
   */
  @Benchmark
  public void distanceExcludingLength(final Blackhole blackhole) {
    blackhole.consume(firstDigest.distanceTo(secondDigest, false));
  }
}
