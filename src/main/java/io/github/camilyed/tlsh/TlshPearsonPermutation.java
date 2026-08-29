package io.github.camilyed.tlsh;

/**
 * Stores the fixed lookup table used to mix bytes during Pearson hashing.
 *
 * <p>The table solves a mixing problem. A hash state and the next input byte are combined with XOR,
 * but XOR alone is too regular. For example, XOR is reversible and does not care about order when
 * values are combined without any additional transformation:
 *
 * <pre>{@code
 * A ^ B ^ E == E ^ B ^ A
 * }</pre>
 *
 * <p>Pearson hashing breaks that simple pattern by using the XOR result as an index into this
 * shuffled table. The value found at that index becomes the next hash state. The following byte is
 * then combined with this already transformed state. Because a table lookup happens after every
 * byte, changing a byte or changing the order of the bytes changes the path through the table and
 * usually produces a different final bucket index.
 *
 * <p>The table has 256 entries because one byte has 256 possible bit patterns. Its indices and its
 * values therefore both cover {@code 0..255}. Every value occurs exactly once. A table with this
 * property is called a permutation:
 *
 * <pre>{@code
 * original values: 0, 1, 2, 3, ..., 255
 * permutation:     the same values, each used once, in a shuffled order
 * }</pre>
 *
 * <p>Uniqueness matters. If a value were missing, a lookup could never produce that state. If a
 * value were repeated, two different table positions would immediately collapse into the same state
 * more often than necessary. Merely using the identity order {@code [0, 1, 2, ...]} would also be
 * insufficient: looking up index {@code n} would return {@code n}, so the table would not transform
 * or mix the XOR result at all.
 *
 * <p>The ordering is fixed rather than shuffled separately for every input. This makes hashing
 * deterministic: the same salt and bytes always follow the same table positions and produce the
 * same bucket index. Changing even one entry would define a different mapping and could change the
 * bucket selected for many byte sequences. The table is an algorithm constant, not input data, a
 * password, or a secret key.
 *
 * <p>The array itself is mutable in Java. Declaring its reference {@code final} prevents replacing
 * the whole array, but it does not prevent code from changing an element such as {@code VALUES[0]}.
 * The internal array is therefore never returned directly. {@link #copy()} creates a separate array
 * so a caller can use or modify its copy without changing the table stored by this class.
 */
final class TlshPearsonPermutation {

  private static final int[] VALUES = {
    1, 87, 49, 12, 176, 178, 102, 166, 121, 193, 6, 84, 249, 230, 44, 163,
    14, 197, 213, 181, 161, 85, 218, 80, 64, 239, 24, 226, 236, 142, 38, 200,
    110, 177, 104, 103, 141, 253, 255, 50, 77, 101, 81, 18, 45, 96, 31, 222,
    25, 107, 190, 70, 86, 237, 240, 34, 72, 242, 20, 214, 244, 227, 149, 235,
    97, 234, 57, 22, 60, 250, 82, 175, 208, 5, 127, 199, 111, 62, 135, 248,
    174, 169, 211, 58, 66, 154, 106, 195, 245, 171, 17, 187, 182, 179, 0, 243,
    132, 56, 148, 75, 128, 133, 158, 100, 130, 126, 91, 13, 153, 246, 216, 219,
    119, 68, 223, 78, 83, 88, 201, 99, 122, 11, 92, 32, 136, 114, 52, 10,
    138, 30, 48, 183, 156, 35, 61, 26, 143, 74, 251, 94, 129, 162, 63, 152,
    170, 7, 115, 167, 241, 206, 3, 150, 55, 59, 151, 220, 90, 53, 23, 131,
    125, 173, 15, 238, 79, 95, 89, 16, 105, 137, 225, 224, 217, 160, 37, 123,
    118, 73, 2, 157, 46, 116, 9, 145, 134, 228, 207, 212, 202, 215, 69, 229,
    27, 188, 67, 124, 168, 252, 42, 4, 29, 108, 21, 247, 19, 205, 39, 203,
    233, 40, 186, 147, 198, 192, 155, 33, 164, 191, 98, 204, 165, 180, 117, 76,
    140, 36, 210, 172, 41, 54, 159, 8, 185, 232, 113, 196, 231, 47, 146, 120,
    51, 65, 28, 144, 254, 221, 93, 189, 194, 139, 112, 43, 71, 109, 184, 209
  };

  private TlshPearsonPermutation() {}

  /**
   * Returns a separate array containing the fixed Pearson permutation.
   *
   * <p>Changing the returned array does not change later copies or the mapping used by newly
   * created {@link PearsonHash} instances.
   *
   * @return a new array containing all 256 permutation values
   */
  static int[] copy() {
    return VALUES.clone();
  }
}
