package io.github.camilyed.tlsh;

import java.util.HexFormat;
import java.util.Objects;

/** Reconstructs a structured digest from its canonical versioned hexadecimal representation. */
final class TlshDigestParser {

  private static final String VERSION_PREFIX = "T1";
  private static final int ENCODED_DIGEST_LENGTH = 72;
  private static final int CHECKSUM_OFFSET = 2;
  private static final int LENGTH_CODE_OFFSET = 4;
  private static final int QUARTILE_RATIOS_OFFSET = 6;
  private static final int HISTOGRAM_CODE_OFFSET = 8;
  private static final int HEX_BYTE_LENGTH = 2;
  private static final int HISTOGRAM_CODE_SIZE = 32;
  private static final HexFormat HEX = HexFormat.of();

  /** Creates a stateless parser for the versioned 128-bucket digest representation. */
  TlshDigestParser() {}

  /**
   * Validates and decodes all four digest components.
   *
   * @param encodedDigest 72-character text beginning with {@code T1}
   * @return decoded immutable digest
   * @throws NullPointerException when the text is {@code null}
   * @throws IllegalArgumentException when its structure or hexadecimal content is invalid
   */
  TlshDigest parse(final String encodedDigest) {
    Objects.requireNonNull(encodedDigest, "encodedDigest");
    validateLength(encodedDigest);
    validateVersionPrefix(encodedDigest);

    final int checksum = swapNibbles(parseHexByte(encodedDigest, CHECKSUM_OFFSET));
    final int lengthCode = swapNibbles(parseHexByte(encodedDigest, LENGTH_CODE_OFFSET));
    final int quartileRatios = parseHexByte(encodedDigest, QUARTILE_RATIOS_OFFSET);
    final byte[] serializedHistogramCode =
        HEX.parseHex(encodedDigest, HISTOGRAM_CODE_OFFSET, encodedDigest.length());
    final byte[] histogramCode = reverseHistogramCode(serializedHistogramCode);

    return new TlshDigest(checksum, lengthCode, quartileRatios, histogramCode);
  }

  /** Ensures that all fixed fields and 32 histogram bytes are present. */
  private static void validateLength(final String encodedDigest) {
    if (encodedDigest.length() != ENCODED_DIGEST_LENGTH) {
      throw new IllegalArgumentException("Encoded TLSH digest must contain exactly 72 characters");
    }
  }

  /** Ensures that the text identifies the supported digest representation. */
  private static void validateVersionPrefix(final String encodedDigest) {
    if (!encodedDigest.startsWith(VERSION_PREFIX)) {
      throw new IllegalArgumentException("Encoded TLSH digest must begin with T1");
    }
  }

  /** Parses exactly two hexadecimal characters as one logical unsigned byte. */
  private static int parseHexByte(final String encodedDigest, final int offset) {
    return HexFormat.fromHexDigits(encodedDigest, offset, offset + HEX_BYTE_LENGTH);
  }

  /** Restores the internal histogram order from the reversed serialized byte order. */
  private static byte[] reverseHistogramCode(final byte[] serializedHistogramCode) {
    final byte[] histogramCode = new byte[HISTOGRAM_CODE_SIZE];
    for (int index = 0; index < histogramCode.length; index++) {
      histogramCode[index] = serializedHistogramCode[histogramCode.length - index - 1];
    }
    return histogramCode;
  }

  /** Exchanges the upper and lower four-bit halves of one logical byte. */
  private static int swapNibbles(final int value) {
    return ((value & 0x0F) << 4) | ((value & 0xF0) >>> 4);
  }
}
