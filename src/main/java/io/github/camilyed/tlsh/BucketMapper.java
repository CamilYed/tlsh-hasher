package io.github.camilyed.tlsh;

/**
 * Maps the six TLSH byte combinations from a full five-byte sliding window to Pearson hash bucket
 * indices.
 *
 * <p>TLSH processes input as a stream. Whenever a new byte completes a five-byte window, that byte
 * is combined with every possible pair selected from the four preceding bytes. There are six such
 * pairs because choosing two items from four gives {@code 4 choose 2 = 6}.
 *
 * <p>For a window containing {@code ABCDE}, {@code E} is the newest byte. The pairs selected from
 * {@code ABCD} are {@code AB}, {@code AC}, {@code AD}, {@code BC}, {@code BD}, and {@code CD}.
 * Appending {@code E} produces the chronological triplets {@code ABE}, {@code ACE}, {@code ADE},
 * {@code BCE}, {@code BDE}, and {@code CDE}.
 *
 * <p>Only combinations containing the newest byte are processed. For example, {@code ABC} is not
 * processed again when {@code E} arrives because it was already processed when {@code C} was the
 * newest byte.
 *
 * <p>The Pearson mapper expects the newest byte first and the other two bytes from newer to older.
 * Therefore the chronological triplet {@code ABE} is passed to Pearson as {@code E, B, A}. The full
 * mapping is:
 *
 * <pre>{@code
 * output 0: salt 13, E B A  (ABE)
 * output 1: salt  7, E C A  (ACE)
 * output 2: salt 11, E D A  (ADE)
 * output 3: salt  5, E C B  (BCE)
 * output 4: salt  3, E D B  (BDE)
 * output 5: salt  2, E D C  (CDE)
 * }</pre>
 *
 * <p>The output order follows the readable chronological order {@code ABE}, {@code ACE}, {@code
 * ADE}, {@code BCE}, {@code BDE}, and {@code CDE}. A histogram ultimately increments all six
 * returned indices, so their order does not change the final histogram as long as every mapping is
 * performed exactly once.
 *
 * <p>The salts {@code 2}, {@code 3}, {@code 5}, {@code 7}, {@code 11}, and {@code 13} are fixed
 * parts of TLSH. They give the six combinations different starting positions in the Pearson
 * permutation. They are public algorithm constants rather than random or secret cryptographic
 * salts.
 */
class BucketMapper {

  private static final TripletMapping[] MAPPINGS = {
    new TripletMapping(13, 1, 0), // ABE -> E, B, A
    new TripletMapping(7, 2, 0), // ACE -> E, C, A
    new TripletMapping(11, 3, 0), // ADE -> E, D, A
    new TripletMapping(5, 2, 1), // BCE -> E, C, B
    new TripletMapping(3, 3, 1), // BDE -> E, D, B
    new TripletMapping(2, 3, 2) // CDE -> E, D, C
  };

  private static final int NEWEST_BYTE_INDEX = 4;

  private final PearsonHash pearsonHash;

  /**
   * Creates a mapper backed by the supplied Pearson hash.
   *
   * @param pearsonHash hash used to map each byte combination to a bucket index
   */
  BucketMapper(PearsonHash pearsonHash) {
    this.pearsonHash = pearsonHash;
  }

  /**
   * Maps a full TLSH sliding window to its six bucket indices.
   *
   * <p>A new result array is created for every invocation. Modifying the returned array therefore
   * cannot affect later mappings.
   *
   * @param window exactly five bytes ordered from the oldest byte to the newest byte
   * @return six bucket indices ordered as {@code ABE}, {@code ACE}, {@code ADE}, {@code BCE},
   *     {@code BDE}, and {@code CDE}
   */
  int[] map(byte[] window) {
    int[] buckets = new int[MAPPINGS.length];

    for (int i = 0; i < MAPPINGS.length; i++) {
      TripletMapping mapping = MAPPINGS[i];

      buckets[i] =
          pearsonHash.map(
              mapping.salt(),
              window[NEWEST_BYTE_INDEX],
              window[mapping.secondIndex()],
              window[mapping.thirdIndex()]);
    }

    return buckets;
  }

  /**
   * Describes one fixed TLSH combination.
   *
   * <p>The newest byte is implicit and always comes from index {@value #NEWEST_BYTE_INDEX}. The two
   * stored indices identify the remaining bytes in the order expected by the Pearson hash.
   *
   * @param salt fixed TLSH salt for the combination
   * @param secondIndex index of the second byte passed to the Pearson hash
   * @param thirdIndex index of the third byte passed to the Pearson hash
   */
  private record TripletMapping(int salt, int secondIndex, int thirdIndex) {}
}
