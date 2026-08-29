package io.github.camilyed.tlsh;

/**
 * Connects the streaming window, bucket mapping, and histogram stages of TLSH feature extraction.
 *
 * <p>The accumulator accepts one byte at a time and keeps only the five most recent bytes in a
 * {@link SlidingWindow}. This allows a large file to be processed as a stream without storing the
 * complete file in memory.
 *
 * <p>No feature is recorded while the window contains fewer than five bytes. Once the fifth byte
 * arrives, {@link BucketMapper} maps the full window to six bucket indices. Each index identifies
 * one counter in the shared {@link Histogram}, and that counter is incremented once.
 *
 * <p>For example, adding {@code A}, {@code B}, {@code C}, and {@code D} changes only the sliding
 * window. Adding {@code E} completes {@code [A, B, C, D, E]} and performs the following flow:
 *
 * <pre>{@code
 * [A, B, C, D, E]
 *         |
 *         v
 * six TLSH combinations: ABE, ACE, ADE, BCE, BDE, CDE
 *         |
 *         v
 * six Pearson bucket indices
 *         |
 *         v
 * increment the six corresponding histogram counters
 * }</pre>
 *
 * <p>Adding {@code F} shifts the window to {@code [B, C, D, E, F]} and records another six
 * features. If multiple features map to the same bucket, the same histogram counter is incremented
 * multiple times. The accumulator stores counts of mapped local features, not the original bytes or
 * triplets.
 *
 * <p>This class performs only feature accumulation. It does not calculate the TLSH checksum,
 * input-length encoding, quartiles, two-bit quantization, final digest, or distance.
 *
 * <p>Instances are mutable and represent the state of one input stream. A single instance should
 * not be shared between unrelated files or updated concurrently from multiple threads.
 */
final class FeatureAccumulator {
  private final BucketMapper bucketMapper;
  private final Histogram featureHistogram;
  private final SlidingWindow slidingWindow = new SlidingWindow();

  /**
   * Creates an accumulator that maps completed windows with the supplied mapper and records hits in
   * the supplied histogram.
   *
   * <p>The collaborators are retained rather than copied. Consequently, callers holding the same
   * {@code featureHistogram} instance observe every bucket hit recorded by this accumulator.
   *
   * @param bucketMapper mapper that turns each full five-byte window into six bucket indices
   * @param featureHistogram histogram that receives the resulting bucket hits
   */
  FeatureAccumulator(final BucketMapper bucketMapper, final Histogram featureHistogram) {
    this.bucketMapper = bucketMapper;
    this.featureHistogram = featureHistogram;
  }

  /**
   * Adds one byte to the current input stream and records features when the window is full.
   *
   * <p>The first four calls only fill the internal window. The fifth and every later call maps the
   * current five-byte window and increments six histogram counters.
   *
   * @param currentByte next byte from the input stream
   */
  void addByte(final byte currentByte) {
    if (slidingWindow.addByte(currentByte)) {
      final int[] bucketIndices = bucketMapper.mapWindowToBucketIndices(slidingWindow.snapshot());
      for (final int bucketIndex : bucketIndices) {
        featureHistogram.recordHit(bucketIndex);
      }
    }
  }
}
