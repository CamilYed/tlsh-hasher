package io.github.camilyed.tlsh;

import java.util.Objects;

/**
 * Calculates the weighted difference between two compatible structured TLSH digests.
 *
 * <p>The score combines four kinds of evidence. A checksum mismatch adds one point. Encoded length
 * and quartile-ratio differences use circular distances because their fixed-width values wrap
 * around. The 32-byte histogram code is unpacked conceptually into 128 two-bit levels and compares
 * corresponding levels in their original bucket positions.
 *
 * <p>This is a distance-like similarity score, not a percentage and not a probability. A lower
 * score means the digests are more similar. Identical digests score zero, while very different
 * digests can score well above 1,000.
 */
final class TlshDistanceCalculator {

  private static final int BYTE_RANGE = 256;
  private static final int NIBBLE_RANGE = 16;
  private static final int LARGE_DIFFERENCE_WEIGHT = 12;
  private static final int TWO_BIT_VALUE_MASK = 0b11;
  private static final int TWO_BIT_VALUES_PER_BYTE = 4;

  /** Creates a stateless calculator for the standard digest structure. */
  TlshDistanceCalculator() {}

  /**
   * Calculates all weighted component differences.
   *
   * @param first first digest
   * @param second second digest
   * @param includeLength whether the length-code difference contributes to the score
   * @return nonnegative difference score
   */
  int calculate(final TlshDigest first, final TlshDigest second, final boolean includeLength) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");

    int difference = 0;
    if (includeLength) {
      final int lengthDifference =
          modularDifference(first.lengthCode(), second.lengthCode(), BYTE_RANGE);
      difference += lengthPenalty(lengthDifference);
    }

    final int firstQ1Ratio = (first.quartileRatios() >>> 4) & 0x0F;
    final int firstQ2Ratio = first.quartileRatios() & 0x0F;
    final int secondQ1Ratio = (second.quartileRatios() >>> 4) & 0x0F;
    final int secondQ2Ratio = second.quartileRatios() & 0x0F;
    difference += quartilePenalty(modularDifference(firstQ1Ratio, secondQ1Ratio, NIBBLE_RANGE));
    difference += quartilePenalty(modularDifference(firstQ2Ratio, secondQ2Ratio, NIBBLE_RANGE));

    if (first.checksum() != second.checksum()) {
      difference += 1;
    }

    difference += histogramDistance(first.histogramCode(), second.histogramCode());
    return difference;
  }

  /** Returns the shorter route between two positions in a wrapping numeric range. */
  private static int modularDifference(final int first, final int second, final int range) {
    final int directDifference = Math.abs(first - second);
    final int wrappedDifference = range - directDifference;
    return Math.min(directDifference, wrappedDifference);
  }

  /** Applies the stronger global-length penalty after a difference of one range step. */
  private static int lengthPenalty(final int difference) {
    if (difference == 0) {
      return 0;
    }
    if (difference == 1) {
      return 1;
    }
    return difference * LARGE_DIFFERENCE_WEIGHT;
  }

  /** Applies the quartile-ratio penalty, discounting the first range step. */
  private static int quartilePenalty(final int difference) {
    if (difference <= 1) {
      return difference;
    }
    return (difference - 1) * LARGE_DIFFERENCE_WEIGHT;
  }

  /** Sums corresponding two-bit level differences across all 32 packed bytes. */
  private static int histogramDistance(final byte[] firstCode, final byte[] secondCode) {
    int difference = 0;
    for (int byteIndex = 0; byteIndex < firstCode.length; byteIndex++) {
      final int firstByte = Byte.toUnsignedInt(firstCode[byteIndex]);
      final int secondByte = Byte.toUnsignedInt(secondCode[byteIndex]);
      for (int pairIndex = 0; pairIndex < TWO_BIT_VALUES_PER_BYTE; pairIndex++) {
        final int shift = pairIndex * 2;
        final int firstValue = (firstByte >>> shift) & TWO_BIT_VALUE_MASK;
        final int secondValue = (secondByte >>> shift) & TWO_BIT_VALUE_MASK;
        difference += twoBitDifference(firstValue, secondValue);
      }
    }
    return difference;
  }

  /** Penalizes opposite two-bit extremes more strongly than ordinary numeric distance. */
  private static int twoBitDifference(final int first, final int second) {
    final int difference = Math.abs(first - second);
    return difference == 3 ? 6 : difference;
  }
}
