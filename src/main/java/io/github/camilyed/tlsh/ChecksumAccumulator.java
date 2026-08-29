package io.github.camilyed.tlsh;

/**
 * Maintains the small rolling checksum that accompanies the feature histogram in a TLSH digest.
 *
 * <p>The histogram and the checksum describe different properties of the input. The histogram
 * counts how often local byte patterns occur. Counts deliberately lose the exact order in which
 * those patterns were observed: recording a hit in bucket {@code 10} and then bucket {@code 20}
 * produces the same final counts as recording bucket {@code 20} and then bucket {@code 10}.
 *
 * <p>The checksum adds a small amount of order-sensitive state. It starts at zero and is replaced
 * whenever another full sliding window is processed. The new value depends on three inputs:
 *
 * <ol>
 *   <li>the newest byte in the window,
 *   <li>the byte immediately preceding it, and
 *   <li>the checksum calculated for all previously processed windows.
 * </ol>
 *
 * <p>Feeding the previous checksum back into the next calculation creates a chain:
 *
 * <pre>{@code
 * initial checksum = 0
 *
 * window ABCDE: checksum = Pearson(0, E, D, 0)  = 92
 * window BCDEF: checksum = Pearson(0, F, E, 92) = 96
 * }</pre>
 *
 * <p>The second update contains the result of the first update, so changing an earlier byte can
 * influence later checksum values. This is why the class is an accumulator rather than a stateless
 * function. One instance represents one input stream and must not be reused for an unrelated file.
 *
 * <p>The fixed salt {@code 0} selects the checksum mapping in {@link PearsonHash}. The accumulator
 * stores its current value as an {@code int} in the range {@code 0..255}, because Java has no
 * unsigned {@code byte}. Pearson hashing accepts a {@code byte}, so the previous value is cast back
 * to {@code byte} before it is mixed. The cast preserves the same eight bits; {@link PearsonHash}
 * converts those bits back to their unsigned table index.
 *
 * <p>This class does not decide when a five-byte window is full. Its caller must invoke {@link
 * #update(byte, byte)} once for the fifth byte and once for every later byte. It also does not
 * calculate a cryptographic checksum. There are only 256 possible results, so collisions are
 * expected and the value must not be used to verify security or file integrity.
 */
final class ChecksumAccumulator {

  private static final int CHECKSUM_SALT = 0;

  private final PearsonHash pearsonHash;
  private int checksumValue;

  /**
   * Creates an accumulator whose initial checksum is zero.
   *
   * @param pearsonHash mapper used to calculate every successive checksum value
   */
  ChecksumAccumulator(final PearsonHash pearsonHash) {
    this.pearsonHash = pearsonHash;
    this.checksumValue = 0;
  }

  /**
   * Replaces the current checksum using the newest adjacent byte pair and the previous checksum.
   *
   * <p>The previous value is read before the newly calculated value is assigned. Consequently,
   * every update extends the existing checksum chain rather than starting a new calculation.
   *
   * @param newestByte newest byte in the current full sliding window
   * @param previousByte byte immediately preceding {@code newestByte} in that window
   */
  void update(final byte newestByte, final byte previousByte) {
    final int previousChecksum = checksumValue;
    checksumValue =
        pearsonHash.mapToBucketIndex(
            CHECKSUM_SALT, newestByte, previousByte, (byte) previousChecksum);
  }

  /**
   * Returns the checksum accumulated for the current input stream.
   *
   * @return {@code 0} before the first update; otherwise the latest value in the range {@code
   *     0..255}
   */
  int value() {
    return checksumValue;
  }
}
