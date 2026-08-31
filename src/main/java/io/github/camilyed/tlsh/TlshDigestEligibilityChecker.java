package io.github.camilyed.tlsh;

/**
 * Decides whether accumulated input statistics are rich enough to produce a standard digest.
 *
 * <p>A similarity digest should not be created merely because some bytes were received. Very short
 * input provides too few five-byte windows, while highly repetitive input may send nearly all local
 * features to a small part of the histogram. In either case, the compact histogram would not
 * describe enough information for a meaningful comparison.
 *
 * <p>The standard policy requires both of the following conditions:
 *
 * <ul>
 *   <li>the input length is at least 256 bytes and no greater than the largest length supported by
 *       {@link LengthEncoder}; and
 *   <li>more than half of the 128 effective histogram buckets contain at least one feature hit.
 * </ul>
 *
 * <p>“More than half” means at least 65 nonzero buckets. The checker counts different occupied
 * buckets, not the total number of hits. For example, one bucket containing 1,000 hits contributes
 * only one occupied bucket, whereas 65 buckets containing one hit each satisfy the distribution
 * requirement. This prevents a large but repetitive input from passing solely because it generated
 * many copies of the same few features.
 *
 * <p>Returning {@code false} means that the supplied accumulated state is structurally valid but
 * cannot produce a standard digest. Supplying anything other than exactly 128 counts is a
 * programming error and results in an exception instead. The input array is only read and is never
 * modified.
 *
 * <p>This checker implements only the standard 256-byte minimum. It deliberately does not expose a
 * reduced-minimum or forced mode.
 */
final class TlshDigestEligibilityChecker {

  private static final long MINIMUM_INPUT_LENGTH = 256;
  private static final int EFFECTIVE_BUCKET_COUNT = 128;
  private static final int MINIMUM_NON_ZERO_BUCKET_COUNT = EFFECTIVE_BUCKET_COUNT / 2 + 1;

  /** Creates a stateless checker for the standard digest eligibility policy. */
  TlshDigestEligibilityChecker() {}

  /**
   * Checks the supported length range and the distribution of feature hits.
   *
   * @param inputLength exact number of accumulated input bytes
   * @param effectiveBucketCounts counts for effective buckets {@code 0..127}
   * @return {@code true} when the state can produce a standard digest; otherwise {@code false}
   * @throws IllegalArgumentException when the array does not contain exactly 128 bucket counts
   */
  boolean isEligible(final long inputLength, final int[] effectiveBucketCounts) {
    validateEffectiveBucketCount(effectiveBucketCounts);

    if (inputLength < MINIMUM_INPUT_LENGTH || inputLength > LengthEncoder.MAX_INPUT_LENGTH) {
      return false;
    }

    int nonZeroBucketCount = 0;
    for (final int bucketCount : effectiveBucketCounts) {
      if (bucketCount > 0) {
        nonZeroBucketCount++;
      }
    }

    return nonZeroBucketCount >= MINIMUM_NON_ZERO_BUCKET_COUNT;
  }

  /** Ensures that eligibility is evaluated over the complete effective histogram. */
  private static void validateEffectiveBucketCount(final int[] effectiveBucketCounts) {
    if (effectiveBucketCounts.length != EFFECTIVE_BUCKET_COUNT) {
      throw new IllegalArgumentException(
          "Eligibility requires exactly 128 effective bucket counts");
    }
  }
}
