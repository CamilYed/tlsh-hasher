package io.github.camilyed.tlsh;

import java.util.Arrays;

/**
 * Holds the four calculated components of one compact similarity digest.
 *
 * <p>This record represents the structured binary result of the algorithm before it is converted to
 * a human-readable hexadecimal string. Its components describe different properties of the input:
 *
 * <ul>
 *   <li>{@code checksum} is the order-sensitive rolling value calculated from consecutive bytes;
 *   <li>{@code lengthCode} identifies the predefined range containing the exact input length;
 *   <li>{@code quartileRatios} contains the packed {@code Q1/Q3} and {@code Q2/Q3} nibbles; and
 *   <li>{@code histogramCode} contains the 128 quantized histogram levels packed into 32 bytes.
 * </ul>
 *
 * <p>The checksum, length code, and packed ratios are stored as {@code int} values even though each
 * eventually occupies one byte. Java bytes are signed and have the numeric range {@code -128..127};
 * using {@code int} lets these logical unsigned values be read naturally as {@code 0..255}. The
 * length code uses only {@code 0..169}, the codes currently defined by {@link LengthEncoder}.
 *
 * <p>A Java record is immutable only with respect to its component references. A {@code byte[]}
 * remains mutable, so this record copies the histogram code when it is created and whenever it is
 * returned. Neither the constructor's caller nor an accessor's caller can therefore change the
 * stored digest afterward.
 *
 * <p>Record-generated equality would compare two arrays by identity rather than by their byte
 * contents. This record overrides {@link #equals(Object)} and {@link #hashCode()} so independently
 * created digests with identical components are equal and work correctly as map keys or set
 * members.
 *
 * <p>This type does not calculate the four components and does not define their textual order,
 * hexadecimal representation, or version prefix. Those responsibilities belong to later assembly
 * and formatting stages.
 *
 * @param checksum rolling checksum represented as an unsigned byte value in {@code 0..255}
 * @param lengthCode encoded input-length range in {@code 0..169}
 * @param quartileRatios packed quartile-ratio byte represented as an unsigned value in {@code
 *     0..255}
 * @param histogramCode packed 32-byte histogram representation
 */
record TlshDigest(int checksum, int lengthCode, int quartileRatios, byte[] histogramCode) {
  private static final int MAX_UNSIGNED_BYTE_VALUE = 255;
  private static final int MAX_LENGTH_CODE = 169;
  private static final int HISTOGRAM_CODE_SIZE = 32;

  /**
   * Validates every component and stores a defensive copy of the histogram code.
   *
   * @throws IllegalArgumentException when a numeric component is outside its supported range or the
   *     histogram code does not contain exactly 32 bytes
   */
  TlshDigest {
    validateUnsignedByte(checksum, "Checksum");
    validateLengthCode(lengthCode);
    validateUnsignedByte(quartileRatios, "Quartile ratios");
    validateHistogramCodeSize(histogramCode);

    histogramCode = histogramCode.clone();
  }

  /**
   * Returns the packed histogram without exposing the mutable array stored by this digest.
   *
   * @return a new copy of the 32-byte histogram code
   */
  @Override
  public byte[] histogramCode() {
    return histogramCode.clone();
  }

  /** Ensures that a logical unsigned byte can be stored without losing information. */
  private static void validateUnsignedByte(final int value, final String componentName) {
    if (value < 0 || value > MAX_UNSIGNED_BYTE_VALUE) {
      throw new IllegalArgumentException(componentName + " must be between 0 and 255");
    }
  }

  /** Ensures that the value identifies one of the ranges defined by the length encoder. */
  private static void validateLengthCode(final int lengthCode) {
    if (lengthCode < 0 || lengthCode > MAX_LENGTH_CODE) {
      throw new IllegalArgumentException("Length code must be between 0 and 169");
    }
  }

  /** Ensures that the code contains four packed histogram levels per output byte. */
  private static void validateHistogramCodeSize(final byte[] histogramCode) {
    if (histogramCode.length != HISTOGRAM_CODE_SIZE) {
      throw new IllegalArgumentException("Histogram code must contain exactly 32 bytes");
    }
  }

  /**
   * Compares all scalar components and the contents of the packed histogram array.
   *
   * @param other object to compare with this digest
   * @return {@code true} when the other object is a digest with identical component values
   */
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TlshDigest that)) {
      return false;
    }
    return checksum == that.checksum
        && lengthCode == that.lengthCode
        && quartileRatios == that.quartileRatios
        && Arrays.equals(histogramCode, that.histogramCode);
  }

  /**
   * Calculates a content-based hash consistent with {@link #equals(Object)}.
   *
   * @return hash code derived from all four digest components
   */
  @Override
  public int hashCode() {
    int result = Integer.hashCode(checksum);
    result = 31 * result + Integer.hashCode(lengthCode);
    result = 31 * result + Integer.hashCode(quartileRatios);
    result = 31 * result + Arrays.hashCode(histogramCode);
    return result;
  }
}
