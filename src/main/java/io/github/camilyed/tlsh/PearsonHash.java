package io.github.camilyed.tlsh;

/**
 * Converts three ordered bytes into the index of one counter in a 256-counter histogram.
 *
 * <p>The problem solved by this class starts with a large difference in scale. Three bytes can form
 * {@code 256 * 256 * 256}, or 16,777,216, different sequences, but the histogram has only 256
 * counters. It is therefore impossible to give every sequence its own counter. Many different
 * sequences must intentionally share the same counter. This sharing is called a collision.
 *
 * <p>A useful mapping should spread commonly occurring sequences across the histogram instead of
 * sending most of them to a small group of counters. It should also use every input byte and care
 * about their order. Simple formulas do not meet these requirements well. For example, adding the
 * bytes would give {@code A + B + E == E + B + A}, so two differently ordered features would always
 * select the same counter. Selecting a counter from only one byte would completely ignore the other
 * two bytes.
 *
 * <p>Pearson hashing addresses this problem by carrying a small intermediate value called the hash
 * state. Each input byte changes that state. The changed state becomes the starting point for the
 * next byte, so the final result depends on the complete ordered sequence rather than on one byte
 * considered in isolation.
 *
 * <p>The state is mixed with a permutation table containing every number from {@code 0} to {@code
 * 255} exactly once, in a shuffled order. The table turns a simple intermediate index into a less
 * predictable next state. Without that shuffle, repeated XOR operations would preserve too much
 * structure from the input and would not mix the bytes sufficiently.
 *
 * <p>The mapping is deterministic: the same permutation, salt, and three bytes always select the
 * same counter. It does not guarantee that different sequences select different counters—this is
 * impossible with only 256 results—but it aims to distribute those unavoidable collisions rather
 * than concentrate them in a few places.
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
 * <p>The {@code salt} chooses the initial state. It lets the caller map different kinds of
 * three-byte features through the same permutation without giving all of them the same starting
 * point. A salt is not random data, a password, or a security mechanism; it is simply another
 * small, fixed input to the mapping.
 *
 * <p>For example, suppose a shortened demonstration table begins with {@code [3, 6, 1, 5, 7, 0, 4,
 * 2]}. Mapping salt {@code 2} and bytes {@code 5}, {@code 3}, and {@code 6} changes the state as
 * follows:
 *
 * <pre>{@code
 * h = T[2]     = 1
 * h = T[1 ^ 5] = T[4] = 7
 * h = T[7 ^ 3] = T[4] = 7
 * h = T[7 ^ 6] = T[1] = 6
 * }</pre>
 *
 * <p>The final state {@code 6} is the selected histogram index. The histogram increments counter
 * {@code 6}; it does not store the three original bytes inside that counter.
 *
 * <p>Java bytes are signed and have values from {@code -128} to {@code 127}, while every possible
 * eight-bit pattern must address a table position from {@code 0} to {@code 255}. Widening a
 * negative {@code byte} to {@code short} or {@code int} preserves the negative value, so widening
 * alone is not an unsigned conversion:
 *
 * <pre>{@code
 * final byte stored = (byte) 240;             // stored is -16 in Java
 * final short widenedToShort = stored;        // still -16
 * final int widenedToInt = stored;            // still -16
 * final int unsignedValue =
 *     Byte.toUnsignedInt(stored);       // 240
 * }</pre>
 *
 * <p>The conversion is required before XOR. Otherwise Java sign-extends a negative byte to a
 * negative 32-bit integer, which can produce a negative permutation index. An {@code int} is used
 * for both the table index and the returned result because Java has no unsigned {@code byte}, array
 * indices naturally use {@code int}, and Java promotes {@code byte} and {@code short} operands to
 * {@code int} during bitwise operations. Although the method returns {@code int}, its result is
 * always limited to {@code 0..255}.
 *
 * <p>This class performs only the small mapping described above. It does not store histogram
 * counts, process the sliding window, or produce a complete similarity digest. Pearson hashing is
 * also not a cryptographic hash: its small result makes collisions expected and unsuitable for
 * security decisions.
 */
final class PearsonHash {
  private final int[] permutationTable;

  /** Creates a Pearson mapper using the permutation selected for this similarity algorithm. */
  PearsonHash() {
    this(TlshPearsonPermutation.copy());
  }

  /**
   * Creates a Pearson mapper with the supplied 256-entry permutation table.
   *
   * <p>The table is copied, so changing the caller's array after construction cannot change future
   * mapping results.
   *
   * @param permutationTable table expected to contain each value from {@code 0} to {@code 255}
   *     exactly once
   * @throws IllegalArgumentException when the table does not contain exactly 256 entries
   */
  PearsonHash(final int[] permutationTable) {
    validatePermutationTableSize(permutationTable);
    this.permutationTable = permutationTable.clone();
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
  int mapToBucketIndex(
      final int salt, final byte firstByte, final byte secondByte, final byte thirdByte) {
    validateSalt(salt);
    final int unsignedFirstByte = toUnsignedInt(firstByte);
    int hashState = permutationTable[salt];
    hashState = permutationTable[hashState ^ unsignedFirstByte];
    hashState = permutationTable[hashState ^ toUnsignedInt(secondByte)];
    hashState = permutationTable[hashState ^ toUnsignedInt(thirdByte)];
    return hashState;
  }

  /** Ensures that every possible byte value has one position in the permutation table. */
  private static void validatePermutationTableSize(final int[] permutationTable) {
    if (permutationTable.length != 256) {
      throw new IllegalArgumentException("Permutation must contain exactly 256 values");
    }
  }

  /** Ensures that the starting value can address the 256-entry permutation table. */
  private void validateSalt(final int salt) {
    if (salt < 0 || salt >= permutationTable.length) {
      throw new IllegalArgumentException("Salt must be between 0 and 255");
    }
  }

  /** Converts Java's signed byte representation to the unsigned value expected by TLSH. */
  private static int toUnsignedInt(final byte value) {
    return Byte.toUnsignedInt(value);
  }
}
