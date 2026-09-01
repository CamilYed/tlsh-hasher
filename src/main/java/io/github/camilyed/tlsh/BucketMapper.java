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
 * <p>There are three related orders to keep separate:
 *
 * <ol>
 *   <li>The window order is oldest to newest: {@code A, B, C, D, E}.
 *   <li>The feature name is chronological: for example, {@code ABE}.
 *   <li>The Pearson input order is newest to oldest: the same feature is passed as {@code E, B, A}.
 * </ol>
 *
 * <p>Only combinations containing the newest byte are processed. For example, {@code ABC} is not
 * processed again when {@code E} arrives because it was already processed when {@code C} was the
 * newest byte.
 *
 * <p>The Pearson mapper expects the newest byte first and the other two bytes from newer to older.
 * Therefore the chronological triplet {@code ABE} is passed to Pearson as {@code E, B, A}. Each
 * resulting index is recorded directly in the supplied histogram. The full mapping is:
 *
 * <pre>{@code
 * hit 0: salt 13, E B A  (ABE)
 * hit 1: salt  7, E C A  (ACE)
 * hit 2: salt 11, E D A  (ADE)
 * hit 3: salt  5, E C B  (BCE)
 * hit 4: salt  3, E D B  (BDE)
 * hit 5: salt  2, E D C  (CDE)
 * }</pre>
 *
 * <p>For the first combination, the mapper reads {@code E} from index {@code 4}, {@code B} from
 * index {@code 1}, and {@code A} from index {@code 0}:
 *
 * <pre>{@code
 * final int bucketIndex = pearsonHash.mapToBucketIndex(
 *     13, slidingWindow.byteAt(4), slidingWindow.byteAt(1), slidingWindow.byteAt(0));
 * histogram.recordHit(bucketIndex);
 * }</pre>
 *
 * <p>The Pearson permutation determines the numeric value of {@code bucketIndex}. The mapper does
 * not store the triplet itself. It increments the histogram counter that represents that local byte
 * feature.
 *
 * <p>The processing order follows the readable chronological order {@code ABE}, {@code ACE}, {@code
 * ADE}, {@code BCE}, {@code BDE}, and {@code CDE}. Their order does not change the final histogram
 * as long as every mapping is performed exactly once.
 *
 * <p>The mapper reads individual bytes from the window and immediately records each result. It does
 * not create a five-byte snapshot or a six-element result array for every input byte. This matters
 * for large inputs because, after the first four bytes, every new byte produces another full
 * window.
 *
 * <p>The salts {@code 2}, {@code 3}, {@code 5}, {@code 7}, {@code 11}, and {@code 13} are fixed
 * parts of TLSH. They give the six combinations different starting positions in the Pearson
 * permutation. They are public algorithm constants rather than random or secret cryptographic
 * salts.
 */
final class BucketMapper {

  private static final TripletMapping[] TRIPLET_MAPPINGS = {
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
  BucketMapper(final PearsonHash pearsonHash) {
    this.pearsonHash = pearsonHash;
  }

  /**
   * Maps the six features from a full TLSH sliding window directly into a histogram.
   *
   * <p>The caller invokes this method only after the window reports that it is full. No triplet,
   * window snapshot, or bucket-index array is created: each Pearson result immediately increments
   * its corresponding histogram counter. If two results have the same index, that counter is
   * incremented twice, as required by the algorithm.
   *
   * @param slidingWindow full five-byte window ordered from oldest to newest
   * @param featureHistogram histogram that receives exactly six feature hits
   */
  void mapWindowIntoHistogram(final SlidingWindow slidingWindow, final Histogram featureHistogram) {
    for (final TripletMapping mapping : TRIPLET_MAPPINGS) {
      final int bucketIndex =
          pearsonHash.mapToBucketIndex(
              mapping.salt(),
              slidingWindow.byteAt(NEWEST_BYTE_INDEX),
              slidingWindow.byteAt(mapping.secondByteIndex()),
              slidingWindow.byteAt(mapping.thirdByteIndex()));
      featureHistogram.recordHit(bucketIndex);
    }
  }

  /**
   * Describes one fixed TLSH combination.
   *
   * <p>The newest byte is implicit and always comes from index {@value #NEWEST_BYTE_INDEX}. The two
   * stored indices identify the remaining bytes in the order expected by the Pearson hash.
   *
   * @param salt fixed TLSH salt for the combination
   * @param secondByteIndex index of the second byte passed to the Pearson hash
   * @param thirdByteIndex index of the third byte passed to the Pearson hash
   */
  private record TripletMapping(int salt, int secondByteIndex, int thirdByteIndex) {}
}
