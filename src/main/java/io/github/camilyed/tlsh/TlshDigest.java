package io.github.camilyed.tlsh;

import java.util.Arrays;
import java.util.Objects;

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
 * <p>The digest can be converted to its canonical text with {@link #encoded()}, restored with
 * {@link #parse(String)}, and compared with another digest using {@link #distanceTo(TlshDigest)}.
 * The distance is a TLSH difference score rather than a percentage: smaller scores mean that the
 * inputs are more similar.
 *
 * @param checksum rolling checksum represented as an unsigned byte value in {@code 0..255}
 * @param lengthCode encoded input-length range in {@code 0..169}
 * @param quartileRatios packed quartile-ratio byte represented as an unsigned value in {@code
 *     0..255}
 * @param histogramCode packed 32-byte histogram representation
 */
public record TlshDigest(int checksum, int lengthCode, int quartileRatios, byte[] histogramCode) {
  private static final int MAX_UNSIGNED_BYTE_VALUE = 255;
  private static final int MAX_LENGTH_CODE = 169;

  /** Number of bytes that store the 128 packed two-bit histogram levels. */
  static final int HISTOGRAM_CODE_SIZE = 32;

  /**
   * Validates every component and stores a defensive copy of the histogram code.
   *
   * @throws NullPointerException when {@code histogramCode} is {@code null}
   * @throws IllegalArgumentException when a numeric component is outside its supported range or the
   *     histogram code does not contain exactly 32 bytes
   */
  public TlshDigest {
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

  /**
   * Returns one packed histogram byte without exposing the mutable backing array.
   *
   * <p>This package-private operation is for internal algorithms that only need to read the digest.
   * Unlike {@link #histogramCode()}, it does not allocate a complete defensive copy on every call.
   * Returning one primitive byte cannot give the caller a reference through which the digest could
   * be changed.
   *
   * @param index packed histogram-byte position in {@code 0..31}
   * @return packed histogram byte at the requested position
   * @throws ArrayIndexOutOfBoundsException when {@code index} is outside {@code 0..31}
   */
  byte histogramCodeByteAt(final int index) {
    return histogramCode[index];
  }

  /**
   * Returns the canonical 72-character versioned representation of this digest.
   *
   * @return uppercase hexadecimal text beginning with {@code T1}
   */
  public String encoded() {
    return new TlshDigestFormatter().format(this);
  }

  /**
   * Parses a canonical 72-character {@code T1} representation.
   *
   * @param encodedDigest versioned hexadecimal digest text
   * @return immutable structured digest
   * @throws NullPointerException when {@code encodedDigest} is {@code null}
   * @throws IllegalArgumentException when the text has an invalid length, prefix, or hexadecimal
   *     content
   */
  public static TlshDigest parse(final String encodedDigest) {
    return new TlshDigestParser().parse(encodedDigest);
  }

  /**
   * Calculates the TLSH difference score including the encoded input lengths.
   *
   * <p>A TLSH digest does not store the exact number of bytes. It stores a compact code identifying
   * an approximate size range. Including this component means that files from different size ranges
   * receive an additional penalty even when their local-pattern histograms are similar.
   *
   * <p>The result is not a percentage or probability. Lower values indicate greater similarity, and
   * comparing a digest with itself returns zero.
   *
   * @param other digest to compare with this digest
   * @return nonnegative TLSH difference score
   * @throws NullPointerException when {@code other} is {@code null}
   */
  public int distanceTo(final TlshDigest other) {
    return distanceTo(other, true);
  }

  /**
   * Calculates the TLSH difference score without comparing the encoded input lengths.
   *
   * <p>This method ignores only the compact approximate file-size ranges. It still compares the
   * checksum, quartile ratios, and local-pattern histogram. This variant is useful when one file
   * may contain a longer or shorter version of related content and that overall size difference
   * should not raise the score. The result remains a TLSH difference score rather than a percentage
   * or probability.
   *
   * @param other digest to compare with this digest
   * @return nonnegative TLSH difference score without a length contribution
   * @throws NullPointerException when {@code other} is {@code null}
   */
  public int distanceToIgnoringLength(final TlshDigest other) {
    return distanceTo(other, false);
  }

  /**
   * Calculates the TLSH difference score with a configurable input-length contribution.
   *
   * <p>{@link #distanceTo(TlshDigest)} and {@link #distanceToIgnoringLength(TlshDigest)} are
   * clearer choices at ordinary call sites. This overload is useful when the choice is already
   * represented by a boolean configuration value.
   *
   * @param other digest to compare with this digest
   * @param includeLength whether differences between encoded input lengths should affect the score
   * @return nonnegative TLSH difference score
   * @throws NullPointerException when {@code other} is {@code null}
   */
  public int distanceTo(final TlshDigest other, final boolean includeLength) {
    return new TlshDistanceCalculator().calculate(this, other, includeLength);
  }

  /**
   * Returns the same canonical representation as {@link #encoded()}.
   *
   * @return uppercase hexadecimal text beginning with {@code T1}
   */
  @Override
  public String toString() {
    return encoded();
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
    Objects.requireNonNull(histogramCode, "histogramCode");
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
