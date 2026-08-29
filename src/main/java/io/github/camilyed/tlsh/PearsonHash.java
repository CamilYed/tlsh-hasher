package io.github.camilyed.tlsh;

/**
 * Maps one salted sequence of three bytes to a number between {@code 0} and {@code 255} using
 * Pearson hashing.
 *
 * <p>The result is logically an unsigned eight-bit value. Java has no unsigned {@code byte}, so the
 * method returns it as an {@code int}; only the {@code 0..255} part of the {@code int} range is
 * used. Returning {@code short} would not remove the need for conversion because Java {@code short}
 * is also signed, and Java performs bitwise operations using {@code int} values.
 *
 * <p>This class does not produce a complete TLSH digest. It produces one bucket index for one of
 * the six byte combinations extracted from a five-byte sliding window. A later histogram step
 * increments the bucket at that index.
 *
 * <p>Pearson hashing uses a table containing a permutation of all numbers from {@code 0} to {@code
 * 255}. A permutation contains every number exactly once, but in a shuffled order. Looking up a
 * value in this table after every XOR operation makes the result depend on the order of the input
 * bytes.
 *
 * <p>The mapping is calculated as follows, where {@code T} is the permutation table and {@code ^}
 * means bitwise XOR:
 *
 * <pre>{@code
 * h = T[salt]
 * h = T[h ^ unsigned(firstByte)]
 * h = T[h ^ unsigned(secondByte)]
 * h = T[h ^ unsigned(thirdByte)]
 * }</pre>
 *
 * <p>XOR compares two values bit by bit. A result bit is {@code 1} when the corresponding input
 * bits differ and {@code 0} when they are equal. For example, {@code 5 ^ 3} is {@code 6}:
 *
 * <pre>{@code
 * 0101  (5)
 * 0011  (3)
 * ----
 * 0110  (6)
 * }</pre>
 *
 * <p>For example, suppose the beginning of a test permutation is {@code [3, 6, 1, 5, 7, 0, 4, 2]}.
 * Mapping salt {@code 2} and bytes {@code 5}, {@code 3}, and {@code 6} gives:
 *
 * <pre>{@code
 * h = T[2]     = 1
 * h = T[1 ^ 5] = T[4] = 7
 * h = T[7 ^ 3] = T[4] = 7
 * h = T[7 ^ 6] = T[1] = 6
 * }</pre>
 *
 * <p>Java bytes are signed and have values from {@code -128} to {@code 127}, while TLSH treats the
 * same eight bits as an unsigned value from {@code 0} to {@code 255}. Widening a negative {@code
 * byte} to {@code short} or {@code int} preserves the negative value, so widening alone is not an
 * unsigned conversion:
 *
 * <pre>{@code
 * byte stored = (byte) 240;             // stored is -16 in Java
 * short widenedToShort = stored;        // still -16
 * int widenedToInt = stored;            // still -16
 * int unsignedValue =
 *     Byte.toUnsignedInt(stored);       // 240
 * }</pre>
 *
 * <p>The conversion is required before XOR. Otherwise Java sign-extends a negative byte to a
 * negative 32-bit integer, which can produce a negative permutation index. An {@code int} is used
 * for the converted value because {@link Byte#toUnsignedInt(byte)} returns {@code int}, array
 * indices naturally use {@code int}, and Java promotes {@code byte} and {@code short} operands to
 * {@code int} during bitwise operations.
 *
 * <p>The salt is a fixed TLSH input that distinguishes one window combination from another. It is
 * not random, secret, or cryptographically secure. Pearson hashing and TLSH are similarity
 * mechanisms, not cryptographic hashes.
 */
class PearsonHash {
  private final int[] permutation;

  /**
   * Creates a Pearson mapper with the supplied 256-entry permutation table.
   *
   * <p>The table is copied, so changing the caller's array after construction cannot change future
   * mapping results.
   *
   * @param permutation table expected to contain each value from {@code 0} to {@code 255} exactly
   *     once
   * @throws IllegalArgumentException when the table does not contain exactly 256 entries
   */
  PearsonHash(int[] permutation) {
    if (permutation.length != 256) {
      throw new IllegalArgumentException("Permutation must contain exactly 256 values");
    }
    this.permutation = permutation.clone();
  }

  /**
   * Maps a salt and three ordered bytes to one histogram bucket index.
   *
   * <p>Byte order is significant. In general, mapping {@code E, B, A} does not produce the same
   * result as mapping {@code A, B, E}.
   *
   * @param salt starting permutation index in the range {@code 0..255}
   * @param firstByte first byte mixed into the Pearson state
   * @param secondByte second byte mixed into the Pearson state
   * @param thirdByte third byte mixed into the Pearson state
   * @return bucket index in the range {@code 0..255}
   * @throws IllegalArgumentException when {@code salt} is outside the range {@code 0..255}
   */
  int map(int salt, byte firstByte, byte secondByte, byte thirdByte) {
    if (salt < 0 || salt >= permutation.length) {
      throw new IllegalArgumentException("Salt must be between 0 and 255");
    }
    int unsignedFirst = unsigned(firstByte);
    int h = permutation[salt];
    h = permutation[h ^ unsignedFirst];
    h = permutation[h ^ unsigned(secondByte)];
    h = permutation[h ^ unsigned(thirdByte)];
    return h;
  }

  /** Converts Java's signed byte representation to the unsigned value expected by TLSH. */
  private static int unsigned(byte value) {
    return Byte.toUnsignedInt(value);
  }
}
