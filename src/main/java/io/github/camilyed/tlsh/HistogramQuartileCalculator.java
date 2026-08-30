package io.github.camilyed.tlsh;

import java.util.Arrays;

/**
 * Finds three frequency thresholds that divide 128 histogram bucket counts into four groups.
 *
 * <p>The input is not a list of bytes and it is not a list of bucket indices. It is a list of
 * bucket heights: each value says how many local features were recorded in one histogram bucket.
 * For example, {@code [2, 9, 3, 20]} describes four buckets containing two, nine, three, and twenty
 * feature hits.
 *
 * <p>Quartiles describe the distribution of those heights. They answer questions such as “what
 * bucket count marks the lowest quarter of the distribution?” In statistical language, the first,
 * second, and third quartiles correspond to the 25th, 50th, and 75th percentiles. The second
 * quartile is also called the median.
 *
 * <p>This calculator first sorts the 128 counts from lowest to highest. Because array indices begin
 * at zero, four equally sized groups of 32 values end at these positions:
 *
 * <pre>{@code
 * positions  0..31  -> first quarter  -> Q1 is sortedBucketCounts[31]
 * positions 32..63  -> second quarter -> Q2 is sortedBucketCounts[63]
 * positions 64..95  -> third quarter  -> Q3 is sortedBucketCounts[95]
 * positions 96..127 -> fourth quarter
 * }</pre>
 *
 * <p>The values at those positions are returned directly. They are not averaged with neighboring
 * values and they are not the position numbers themselves. For a sorted sequence {@code 1..128},
 * the selected thresholds are therefore {@code 32}, {@code 64}, and {@code 96}.
 *
 * <p>Sorting is performed on a defensive copy. The original histogram order associates each count
 * with a specific Pearson bucket and is needed later. A later quantization stage compares every
 * count in that original order with the three thresholds:
 *
 * <pre>{@code
 * count <= Q1       -> group 0
 * Q1 < count <= Q2  -> group 1
 * Q2 < count <= Q3  -> group 2
 * Q3 < count        -> group 3
 * }</pre>
 *
 * <p>Repeated counts are valid, so two or all three returned quartiles may be equal. A third
 * quartile of zero indicates that too few histogram buckets received hits; deciding whether that
 * makes the complete digest invalid belongs to a later validation stage.
 */
final class HistogramQuartileCalculator {

  private static final int QUARTILE_BUCKET_COUNT = 128;

  /** Creates a stateless calculator for a 128-bucket histogram distribution. */
  HistogramQuartileCalculator() {}

  /**
   * Calculates the three quartile thresholds without changing the supplied bucket order.
   *
   * @param bucketCounts counts from the 128 effective histogram buckets
   * @return first, second, and third quartile values in nondecreasing order
   * @throws IllegalArgumentException when the array does not contain exactly 128 counts
   */
  HistogramQuartiles calculate(final int[] bucketCounts) {
    if (bucketCounts.length != QUARTILE_BUCKET_COUNT) {
      throw new IllegalArgumentException("Quartiles require exactly 128 bucket counts");
    }

    final int[] sortedBucketCounts = bucketCounts.clone();
    Arrays.sort(sortedBucketCounts);

    final int quartileSize = sortedBucketCounts.length / 4;

    return new HistogramQuartiles(
        sortedBucketCounts[quartileSize - 1],
        sortedBucketCounts[quartileSize * 2 - 1],
        sortedBucketCounts[quartileSize * 3 - 1]);
  }
}
