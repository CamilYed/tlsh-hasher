package io.github.camilyed.tlsh;

/**
 * Counts how often TLSH features are mapped to each Pearson hash bucket.
 *
 * <p>A histogram is a collection of counters. Each counter represents one possible result of the
 * Pearson hash. The logical Pearson result is an unsigned eight-bit value, so there are 256
 * possible bucket indices: {@code 0} through {@code 255}. Java has no unsigned {@code byte} type,
 * so {@link PearsonHash#map(int, byte, byte, byte)} returns that value as an {@code int}. The Java
 * type can represent a much larger range, but this algorithm uses only its values from {@code 0}
 * through {@code 255}.
 *
 * <p>Whenever a five-byte sliding window is full, {@link BucketMapper} produces six bucket indices.
 * The counter at each returned index is incremented. For example, if a mapper produces bucket
 * {@code 79} twice and bucket {@code 240} once, this histogram stores:
 *
 * <pre>{@code
 * bucket 79  -> 2
 * bucket 240 -> 1
 * every untouched bucket -> 0
 * }</pre>
 *
 * <p>Two different byte combinations may map to the same bucket. This is called a collision and is
 * expected: the existing counter is incremented again instead of replacing its value. Over an
 * entire input, the counters describe how frequently different local byte patterns occur. Similar
 * files are expected to produce similar distributions even when they are not byte-for-byte equal.
 *
 * <p>An {@code int[]} is used because every valid index is known in advance and the range is dense.
 * Array elements start at zero, provide direct constant-time access, and avoid the hashing and
 * object overhead of a {@code Map<Integer, Integer>}.
 *
 * <p>This 256-counter accumulator is an intermediate TLSH structure, not the final digest. A later
 * stage derives quartiles and compact two-bit values from the effective histogram buckets. Bucket
 * indices must not be reduced with modulo 128 while collecting features because that would merge
 * different Pearson results and change the TLSH algorithm.
 */
class Histogram {

  private static final int BUCKET_COUNT = 256;
  private final int[] histogram;

  /** Creates an empty histogram with all 256 counters initialized to zero. */
  public Histogram() {
    this.histogram = new int[BUCKET_COUNT];
  }

  /**
   * Records one occurrence of the specified bucket.
   *
   * @param bucketIndex Pearson hash result in the range {@code 0..255}
   * @throws IndexOutOfBoundsException when the index is outside the range {@code 0..255}
   */
  void increment(int bucketIndex) {
    histogram[bucketIndex]++;
  }

  /**
   * Returns the number of occurrences recorded for one bucket.
   *
   * @param bucketIndex bucket index in the range {@code 0..255}
   * @return number of times the bucket has been incremented
   * @throws IndexOutOfBoundsException when the index is outside the range {@code 0..255}
   */
  int countAt(int bucketIndex) {
    return histogram[bucketIndex];
  }
}
