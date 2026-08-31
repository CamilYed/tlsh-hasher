package io.github.camilyed.tlsh;

/**
 * Packs 128 quantized histogram values into the 32-byte body of a similarity digest.
 *
 * <p>The preceding quantization stage represents every effective histogram bucket with one of four
 * levels: {@code 0}, {@code 1}, {@code 2}, or {@code 3}. Four alternatives need only two binary
 * digits, called bits:
 *
 * <pre>{@code
 * level 0 -> 00
 * level 1 -> 01
 * level 2 -> 10
 * level 3 -> 11
 * }</pre>
 *
 * <p>A byte contains eight bits, so one byte can hold four of these two-bit levels. This reduces
 * the 128 integer values produced by {@link HistogramQuantizer} to 32 bytes without losing any
 * information from the quantized representation.
 *
 * <p>For each group of four levels, the first level occupies the two least significant bits. The
 * following levels are shifted left by two, four, and six positions:
 *
 * <pre>{@code
 * input levels:     [0, 1, 2, 3]
 *
 * first level:       00000000
 * second level:      00000100
 * third level:       00100000
 * fourth level:      11000000
 *                    --------
 * packed byte:       11100100
 * }</pre>
 *
 * <p>The bitwise OR operator combines the four non-overlapping pairs of bits. Java performs the
 * shifts as {@code int} operations, after which the complete eight-bit value is cast to {@code
 * byte}. Java bytes are signed, so a bit pattern such as {@code 11100100} is displayed numerically
 * as {@code -28}; the stored eight bits are nevertheless exactly the required bits.
 *
 * <p>This class only packs values that have already been quantized. It does not calculate
 * quartiles, choose frequency levels, reverse the 32-byte code for text formatting, or modify the
 * supplied array.
 */
final class HistogramCodePacker {

  private static final int QUANTIZED_BUCKET_COUNT = 128;

  /** Creates a stateless packer for the 128 quantized histogram values. */
  HistogramCodePacker() {}

  /**
   * Packs every consecutive group of four two-bit values into one byte.
   *
   * @param quantizedBucketValues 128 values in histogram bucket order, each from {@code 0} through
   *     {@code 3}
   * @return a new 32-byte array containing the packed histogram code
   * @throws IllegalArgumentException when the array does not contain exactly 128 values or any
   *     value is outside {@code 0..3}
   */
  byte[] pack(final int[] quantizedBucketValues) {
    validateValueCount(quantizedBucketValues);
    validateTwoBitValues(quantizedBucketValues);

    final byte[] code = new byte[quantizedBucketValues.length / 4];

    for (int codeIndex = 0; codeIndex < code.length; codeIndex++) {
      final int firstBucketIndex = codeIndex * 4;

      final int packedValue =
          quantizedBucketValues[firstBucketIndex]
              | (quantizedBucketValues[firstBucketIndex + 1] << 2)
              | (quantizedBucketValues[firstBucketIndex + 2] << 4)
              | (quantizedBucketValues[firstBucketIndex + 3] << 6);

      code[codeIndex] = (byte) packedValue;
    }

    return code;
  }

  /** Ensures that exactly four values are available for each of the 32 output bytes. */
  private static void validateValueCount(final int[] quantizedBucketValues) {
    if (quantizedBucketValues.length != QUANTIZED_BUCKET_COUNT) {
      throw new IllegalArgumentException("Packing requires exactly 128 quantized bucket values");
    }
  }

  /** Ensures that every input value fits in its allotted pair of bits. */
  private static void validateTwoBitValues(final int[] quantizedBucketValues) {
    for (final int value : quantizedBucketValues) {
      if (value < 0 || value > 3) {
        throw new IllegalArgumentException("Every quantized bucket value must be between 0 and 3");
      }
    }
  }
}
