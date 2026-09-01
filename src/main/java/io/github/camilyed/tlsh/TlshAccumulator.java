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
 * <p>The streaming updates retain the raw values described above. When {@link #finish()} is called,
 * the current exact length, checksum, and effective histogram counts are passed to {@link
 * TlshDigestAssembler}. The assembler calculates the compact length code, quartiles, two-bit
 * histogram levels, packed histogram code, and quartile ratios, then returns an immutable {@link
 * TlshDigest}. Distance calculation and conversion to the textual digest format remain separate
 * responsibilities.
 *
 * <p>Instances are mutable and represent the state of one input stream. A single instance should
 * not be shared between unrelated files or updated concurrently from multiple threads. Finishing
 * creates a snapshot and does not reset or close the accumulator; later bytes continue the same
 * stream and a later call to {@code finish()} reflects the extended state.
 */
final class TlshAccumulator {
  private final BucketMapper bucketMapper;
  private final Histogram featureHistogram;
  private final ChecksumAccumulator checksumAccumulator;
  private final TlshDigestAssembler digestAssembler;
  private final TlshDigestEligibilityChecker digestEligibilityChecker;
  private final SlidingWindow slidingWindow = new SlidingWindow();
  private long inputLength;

  /**
   * Creates an empty accumulator with input length and checksum initially equal to zero.
   *
   * <p>The collaborators are retained rather than copied. Consequently, callers holding the same
   * {@code featureHistogram} instance observe every bucket hit recorded by this accumulator. Code
   * holding the same {@code checksumAccumulator} observes its latest rolling value.
   *
   * @param bucketMapper mapper that records six feature hits for each full five-byte window
   * @param featureHistogram histogram that receives the resulting bucket hits
   * @param checksumAccumulator accumulator that receives the two newest bytes of every full window
   * @param digestAssembler finalization pipeline that transforms the current accumulated state into
   *     an immutable digest
   * @param digestEligibilityChecker policy that decides whether the accumulated length and feature
   *     distribution can produce a standard digest
   */
  TlshAccumulator(
      final BucketMapper bucketMapper,
      final Histogram featureHistogram,
      final ChecksumAccumulator checksumAccumulator,
      final TlshDigestAssembler digestAssembler,
      final TlshDigestEligibilityChecker digestEligibilityChecker) {
    this.bucketMapper = bucketMapper;
    this.featureHistogram = featureHistogram;
    this.checksumAccumulator = checksumAccumulator;
    this.digestAssembler = digestAssembler;
    this.digestEligibilityChecker = digestEligibilityChecker;
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
      checksumAccumulator.update(slidingWindow.byteAt(4), slidingWindow.byteAt(3));
      bucketMapper.mapWindowIntoHistogram(slidingWindow, featureHistogram);
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

  /**
   * Creates an immutable digest from the state accumulated so far.
   *
   * <p>The method passes the exact input length, current rolling checksum, and a defensive copy of
   * the 128 effective histogram counts to the digest assembler. It does not add features, change
   * the checksum, reset the sliding window, or clear any counters. Calling it repeatedly without
   * adding bytes therefore produces equal digests.
   *
   * <p>Before assembly begins, the current state must satisfy the standard whole-stream eligibility
   * policy: the supported length range and enough occupied effective histogram buckets. Component
   * validation still protects the individual encoded fields afterward.
   *
   * @return immutable snapshot of the current accumulated state in compact digest form
   * @throws IllegalStateException when the accumulated input is too short, too long, or does not
   *     occupy enough effective histogram buckets
   */
  TlshDigest finish() {
    final long[] effectiveBucketCounts = featureHistogram.effectiveBucketCounts();
    validateDigestEligibility(effectiveBucketCounts);

    return digestAssembler.assemble(
        inputLength, checksumAccumulator.value(), effectiveBucketCounts);
  }

  /**
   * Ensures that the current stream contains enough data and feature diversity for finalization.
   */
  private void validateDigestEligibility(final long[] effectiveBucketCounts) {
    if (!digestEligibilityChecker.isEligible(inputLength, effectiveBucketCounts)) {
      throw new IllegalStateException("Accumulated input is not eligible for a TLSH digest");
    }
  }
}
