package io.github.camilyed.tlsh;

import java.util.HexFormat;

/**
 * Converts a structured {@link TlshDigest} into its 72-character versioned text representation.
 *
 * <p>The digest already contains all calculated values. Formatting does not hash input again and
 * does not change any component; it only serializes them in the required order:
 *
 * <pre>{@code
 * T1
 * + swapped checksum byte
 * + swapped length-code byte
 * + quartile-ratio byte
 * + 32 histogram bytes in reverse byte order
 * }</pre>
 *
 * <p>{@code T1} is the two-character version prefix used by the standard TLSH representation with
 * 128 effective histogram buckets and a one-byte checksum. The prefix identifies the digest format;
 * it is not one of the calculated similarity features. Every following byte is written as two
 * uppercase hexadecimal characters, so the complete length is:
 *
 * <pre>{@code
 * 2 version characters
 * + 2 checksum characters
 * + 2 length-code characters
 * + 2 quartile-ratio characters
 * + 32 * 2 histogram characters
 * = 72 characters
 * }</pre>
 *
 * <p>A byte consists of two four-bit halves called nibbles. The checksum and length code exchange
 * those halves before they are written. For example, {@code 0xAB} becomes {@code 0xBA}. This is not
 * the same as reversing individual bits and it does not change the stored value inside {@link
 * TlshDigest}; it is only a serialization rule.
 *
 * <p>The packed quartile-ratio byte is written without swapping its nibbles because its upper and
 * lower halves already contain the first and second ratios in their textual positions.
 *
 * <p>The histogram code reverses the order of complete bytes. If its internal order starts with
 * {@code [0x00, 0x01]} and ends with {@code [0x1E, 0x1F]}, the text starts with {@code 1F1E} and
 * ends with {@code 0100}. Digits within a byte are not reversed: {@code 0x1F} remains {@code 1F},
 * not {@code F1}.
 */
final class TlshDigestFormatter {

  private static final String VERSION_PREFIX = "T1";
  private static final int ENCODED_DIGEST_LENGTH = 72;
  private static final HexFormat UPPERCASE_HEX = HexFormat.of().withUpperCase();

  /** Creates a stateless formatter for the versioned 128-bucket digest representation. */
  TlshDigestFormatter() {}

  /**
   * Serializes all digest components to uppercase hexadecimal text.
   *
   * @param digest structured digest to serialize
   * @return 72-character text beginning with {@code T1}
   */
  String format(final TlshDigest digest) {
    final StringBuilder encodedDigest = new StringBuilder(ENCODED_DIGEST_LENGTH);

    encodedDigest.append(VERSION_PREFIX);
    encodedDigest.append(toSwappedHex(digest.checksum()));
    encodedDigest.append(toSwappedHex(digest.lengthCode()));
    encodedDigest.append(toHex(digest.quartileRatios()));

    final byte[] histogramCode = digest.histogramCode();
    for (int codeIndex = histogramCode.length - 1; codeIndex >= 0; codeIndex--) {
      encodedDigest.append(UPPERCASE_HEX.toHexDigits(histogramCode[codeIndex]));
    }

    return encodedDigest.toString();
  }

  /** Converts one logical unsigned byte to hexadecimal after exchanging its two nibbles. */
  private static String toSwappedHex(final int value) {
    return toHex(swapNibbles(value));
  }

  /** Converts the lowest eight bits of a value to two uppercase hexadecimal characters. */
  private static String toHex(final int value) {
    return UPPERCASE_HEX.toHexDigits((byte) value);
  }

  /** Exchanges bits {@code 0..3} with bits {@code 4..7} without reversing either group. */
  private static int swapNibbles(final int value) {
    return ((value & 0x0F) << 4) | ((value & 0xF0) >>> 4);
  }
}
