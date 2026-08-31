package io.github.camilyed.tlsh;

import java.util.Objects;

/** Convenient entry point for one-shot and incremental TLSH calculation. */
public final class Tlsh {

  private Tlsh() {}

  /**
   * Calculates a digest for one complete byte array.
   *
   * @param input complete input bytes
   * @return immutable digest
   * @throws NullPointerException when {@code input} is {@code null}
   * @throws IllegalStateException when the input does not satisfy the standard length and feature
   *     diversity requirements
   */
  public static TlshDigest hash(final byte[] input) {
    Objects.requireNonNull(input, "input");
    final TlshHasher hasher = newHasher();
    hasher.update(input);
    return hasher.finish();
  }

  /**
   * Creates an empty streaming hasher using the standard 128-bucket, one-byte-checksum format.
   *
   * @return new independent hasher
   */
  public static TlshHasher newHasher() {
    final PearsonHash pearsonHash = new PearsonHash();
    final TlshDigestAssembler digestAssembler =
        new TlshDigestAssembler(
            new LengthEncoder(),
            new HistogramQuartileCalculator(),
            new HistogramQuantizer(),
            new HistogramCodePacker(),
            new QuartileRatioEncoder());
    final TlshAccumulator accumulator =
        new TlshAccumulator(
            new BucketMapper(pearsonHash),
            new Histogram(),
            new ChecksumAccumulator(pearsonHash),
            digestAssembler,
            new TlshDigestEligibilityChecker());
    return new TlshHasher(accumulator);
  }
}
