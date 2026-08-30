package io.github.camilyed.tlsh;

/**
 * Replaces every effective histogram bucket count with one of four relative frequency levels.
 *
 * <p>A raw histogram stores exact feature-hit counts. Those counts can grow with the input size, so
 * storing all of them would make a similarity digest unnecessarily large. This class instead
 * compares each count with the three quartiles calculated for the same histogram. The comparison
 * says whether that bucket is in a relatively low, medium, high, or very high part of the
 * histogram's own distribution.
 *
 * <p>The four levels are assigned as follows:
 *
 * <pre>{@code
 * bucketCount <= Q1       -> 0
 * Q1 < bucketCount <= Q2  -> 1
 * Q2 < bucketCount <= Q3  -> 2
 * Q3 < bucketCount        -> 3
 * }</pre>
 *
 * <p>For example, with quartiles {@code Q1 = 10}, {@code Q2 = 20}, and {@code Q3 = 30}, counts
 * {@code 10}, {@code 11}, {@code 21}, and {@code 31} become {@code 0}, {@code 1}, {@code 2}, and
 * {@code 3}. A count equal to a boundary remains in the lower level.
 *
 * <p>Each result is represented here as an {@code int} for clarity, but only values {@code 0..3}
 * are produced. Such a value needs only two bits. A later stage can therefore pack four consecutive
 * levels into one byte. This class does not perform that packing and does not change the supplied
 * bucket-count array.
 */
final class HistogramQuantizer {

  private static final int EFFECTIVE_BUCKET_COUNT = 128;

  /** Creates a stateless quantizer for the 128 effective histogram buckets. */
  HistogramQuantizer() {}

  /**
   * Assigns a relative frequency level to every bucket count while preserving bucket order.
   *
   * @param bucketCounts counts from the 128 effective histogram buckets
   * @param quartiles ordered count thresholds calculated from those bucket counts
   * @return a new 128-entry array containing values from {@code 0} through {@code 3}
   * @throws IllegalArgumentException when there are not exactly 128 counts or the quartiles are not
   *     in nondecreasing order
   */
  int[] quantize(final int[] bucketCounts, final HistogramQuartiles quartiles) {
    validateBucketCount(bucketCounts);
    validateQuartileOrder(quartiles);

    final int[] result = new int[bucketCounts.length];
    final int firstQuartile = quartiles.firstQuartile();
    final int secondQuartile = quartiles.secondQuartile();
    final int thirdQuartile = quartiles.thirdQuartile();
    for (int i = 0; i < bucketCounts.length; i++) {
      final int bucketCount = bucketCounts[i];

      if (bucketCount > thirdQuartile) {
        result[i] = 3;
      } else if (bucketCount > secondQuartile) {
        result[i] = 2;
      } else if (bucketCount > firstQuartile) {
        result[i] = 1;
      }
    }

    return result;
  }

  /** Ensures that one level will be produced for every effective histogram bucket. */
  private static void validateBucketCount(final int[] bucketCounts) {
    if (bucketCounts.length != EFFECTIVE_BUCKET_COUNT) {
      throw new IllegalArgumentException("Quantization requires exactly 128 bucket counts");
    }
  }

  /** Ensures that comparisons against successive quartiles cannot move to a lower threshold. */
  private static void validateQuartileOrder(final HistogramQuartiles quartiles) {
    if (quartiles.firstQuartile() > quartiles.secondQuartile()
        || quartiles.secondQuartile() > quartiles.thirdQuartile()) {
      throw new IllegalArgumentException("Quartiles must be in nondecreasing order");
    }
  }
}
