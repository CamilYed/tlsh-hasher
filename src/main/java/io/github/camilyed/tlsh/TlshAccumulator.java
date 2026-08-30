package io.github.camilyed.tlsh;

/**
 * Coordinates input length, the streaming window, feature histogram, and rolling checksum for one
 * input stream.
 *
 * <p>The accumulator accepts one byte at a time and keeps only the five most recent bytes in a
 * {@link SlidingWindow}. This allows a large file to be processed as a stream without storing the
 * complete file in memory.
 *
 * <p>The raw input length is counted independently of the sliding window. It starts at zero and is
 * increased for every byte, including the first four bytes that cannot yet produce a complete
 * window. A {@code long} is used so inputs larger than the approximately 2 GB limit of {@code int}
 * can be counted correctly. This raw count is not yet the compact length value stored in a final
 * digest; a later stage transforms it into that encoded representation.
 *
 * <p>No feature or checksum update is recorded while the window contains fewer than five bytes.
 * Once the fifth byte arrives, the completed window is used in two parallel calculations:
 *
 * <ul>
 *   <li>{@link BucketMapper} maps six local byte combinations to histogram indices, and
 *   <li>{@link ChecksumAccumulator} mixes the two newest bytes with its previous value.
 * </ul>
 *
 * <p>For example, adding {@code A}, {@code B}, {@code C}, and {@code D} changes only the sliding
 * window. Adding {@code E} completes {@code [A, B, C, D, E]} and performs the following flow:
 *
 * <pre>{@code
 * [A, B, C, D, E]
 *         |
 *         +----------------------------+
 *         |                            |
 *         v                            v
 * six TLSH combinations: ABE, ACE, ADE, BCE, BDE, CDE
 *                                      checksum inputs: E, D, previous checksum
 *         |                            |
 *         v                            v
 * six Pearson bucket indices
 *                                      next rolling checksum
 *         |
 *         v
 * increment the six corresponding histogram counters
 * }</pre>
 *
 * <p>For the first full window, {@code ABCDE}, the checksum calculation uses {@code E}, {@code D},
 * and the initial checksum {@code 0}. Adding {@code F} shifts the window to {@code [B, C, D, E,
 * F]}, records another six histogram features, and extends the checksum using {@code F}, {@code E},
 * and the checksum produced for the preceding window.
 *
 * <p>If multiple features map to the same bucket, the same histogram counter is incremented
 * multiple times. The accumulator stores counts and checksum state, not the original input bytes or
 * every generated triplet.
 *
 * <p>This class does not yet calculate the compact input-length encoding, quartiles, two-bit
 * quantization, the final digest, or distance between digests.
 *
 * <p>Instances are mutable and represent the state of one input stream. A single instance should
 * not be shared between unrelated files or updated concurrently from multiple threads.
 */
final class TlshAccumulator {
  private final BucketMapper bucketMapper;
  private final Histogram featureHistogram;
  private final ChecksumAccumulator checksumAccumulator;
  private final SlidingWindow slidingWindow = new SlidingWindow();
  private long inputLength;

  /**
   * Creates an empty accumulator with input length and checksum initially equal to zero.
   *
   * <p>The collaborators are retained rather than copied. Consequently, callers holding the same
   * {@code featureHistogram} instance observe every bucket hit recorded by this accumulator. Code
   * holding the same {@code checksumAccumulator} observes its latest rolling value.
   *
   * @param bucketMapper mapper that turns each full five-byte window into six bucket indices
   * @param featureHistogram histogram that receives the resulting bucket hits
   * @param checksumAccumulator accumulator that receives the two newest bytes of every full window
   */
  TlshAccumulator(
      final BucketMapper bucketMapper,
      final Histogram featureHistogram,
      final ChecksumAccumulator checksumAccumulator) {
    this.bucketMapper = bucketMapper;
    this.featureHistogram = featureHistogram;
    this.checksumAccumulator = checksumAccumulator;
    this.inputLength = 0;
  }

  /**
   * Adds one byte to the current input stream and processes every resulting full window.
   *
   * <p>Every call increases the input length. The first four calls otherwise only fill the internal
   * window. The fifth and every later call maps the current five-byte window, increments six
   * histogram counters, and updates the rolling checksum.
   *
   * @param currentByte next byte from the input stream
   */
  void addByte(final byte currentByte) {
    inputLength += 1;
    if (slidingWindow.addByte(currentByte)) {
      final byte[] fullWindow = slidingWindow.snapshot();
      final int newestByteIndex = fullWindow.length - 1;
      final int previousByteIndex = newestByteIndex - 1;
      checksumAccumulator.update(fullWindow[newestByteIndex], fullWindow[previousByteIndex]);
      final int[] bucketIndices = bucketMapper.mapWindowToBucketIndices(fullWindow);
      for (final int bucketIndex : bucketIndices) {
        featureHistogram.recordHit(bucketIndex);
      }
    }
  }

  /**
   * Returns the number of bytes added to this accumulator.
   *
   * <p>This is the exact raw byte count, not the compact length code stored in a finished digest.
   *
   * @return number of accepted input bytes, starting at zero
   */
  long inputLength() {
    return inputLength;
  }
}
