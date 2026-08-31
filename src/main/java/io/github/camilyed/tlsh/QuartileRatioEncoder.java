package io.github.camilyed.tlsh;

/**
 * Encodes two proportions between histogram quartiles into one byte.
 *
 * <p>The histogram code records whether individual bucket counts are low or high relative to the
 * quartile boundaries. The proportions encoded here preserve an additional description of the
 * distribution's shape: how large the first and second quartiles are compared with the third
 * quartile. Using the third quartile as the shared reference also removes the absolute scale of the
 * counts. For example, quartiles {@code [3, 6, 10]} and {@code [30, 60, 100]} produce the same
 * proportions.
 *
 * <p>Each proportion is first converted to a whole-number percentage:
 *
 * <pre>{@code
 * first percentage  = Q1 * 100 / Q3
 * second percentage = Q2 * 100 / Q3
 * }</pre>
 *
 * <p>Integer division discards any fractional part. Only the four least significant bits of each
 * percentage are retained with {@code & 0x0F}. Four bits are called a nibble and can represent
 * values {@code 0..15}. This means the stored nibble is the percentage modulo 16, not the complete
 * percentage from {@code 0} through {@code 100}.
 *
 * <p>The first-quartile nibble occupies the upper half of the byte and the second-quartile nibble
 * occupies the lower half. For {@code Q1 = 3}, {@code Q2 = 6}, and {@code Q3 = 10}:
 *
 * <pre>{@code
 * Q1 / Q3 = 30% -> 30 & 0x0F = 14 -> hexadecimal E
 * Q2 / Q3 = 60% -> 60 & 0x0F = 12 -> hexadecimal C
 *
 * upper nibble E + lower nibble C -> byte 0xEC
 * }</pre>
 *
 * <p>The third quartile must be greater than zero because it is the divisor. A zero value also
 * indicates that the histogram contains too little useful variation for these proportions to be
 * meaningful.
 */
final class QuartileRatioEncoder {

  /** Creates a stateless encoder for histogram quartile proportions. */
  QuartileRatioEncoder() {}

  /**
   * Converts two quartile percentages to nibbles and combines them in one byte.
   *
   * @param quartiles nonnegative thresholds ordered as {@code Q1 <= Q2 <= Q3}, with a positive
   *     {@code Q3}
   * @return byte containing the first-quartile ratio in its upper nibble and the second-quartile
   *     ratio in its lower nibble
   * @throws IllegalArgumentException when a quartile is negative, the quartiles are not in
   *     nondecreasing order, or the third quartile is zero
   */
  byte encode(final HistogramQuartiles quartiles) {
    validateNonNegativeQuartiles(quartiles);
    validateQuartileOrder(quartiles);
    validateThirdQuartile(quartiles);
    final int firstRatio = percentage(quartiles.firstQuartile(), quartiles.thirdQuartile()) & 0x0F;
    final int secondRatio =
        percentage(quartiles.secondQuartile(), quartiles.thirdQuartile()) & 0x0F;

    return (byte) ((firstRatio << 4) | secondRatio);
  }

  /** Calculates a whole-number percentage relative to the third quartile. */
  private static int percentage(final long quartile, final long thirdQuartile) {
    return (int) ((quartile * 100) / thirdQuartile);
  }

  /** Ensures that every quartile can represent a histogram bucket count. */
  private static void validateNonNegativeQuartiles(final HistogramQuartiles quartiles) {
    if (quartiles.firstQuartile() < 0
        || quartiles.secondQuartile() < 0
        || quartiles.thirdQuartile() < 0) {
      throw new IllegalArgumentException("Quartiles must not be negative");
    }
  }

  /** Ensures that each successive quartile is at least as large as the preceding one. */
  private static void validateQuartileOrder(final HistogramQuartiles quartiles) {
    if (quartiles.firstQuartile() > quartiles.secondQuartile()
        || quartiles.secondQuartile() > quartiles.thirdQuartile()) {
      throw new IllegalArgumentException("Quartiles must be in nondecreasing order");
    }
  }

  /** Ensures that the common denominator is positive and describes useful histogram variation. */
  private static void validateThirdQuartile(final HistogramQuartiles quartiles) {
    if (quartiles.thirdQuartile() == 0) {
      throw new IllegalArgumentException("Third quartile must be greater than zero");
    }
  }
}
