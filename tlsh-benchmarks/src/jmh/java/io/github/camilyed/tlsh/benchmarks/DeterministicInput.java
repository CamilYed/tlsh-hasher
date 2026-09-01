package io.github.camilyed.tlsh.benchmarks;

/** Creates repeatable, sufficiently varied bytes shared by the TLSH benchmarks. */
final class DeterministicInput {

  private DeterministicInput() {}

  /**
   * Generates the same byte sequence for every invocation with the same size.
   *
   * <p>The small xorshift generator is not cryptographically secure and is not intended to model
   * secret or truly random data. It simply supplies repeatable local byte patterns with enough
   * variation to produce a valid TLSH digest.
   *
   * @param size number of bytes to generate
   * @return new deterministic byte array of the requested size
   */
  static byte[] bytes(final int size) {
    final byte[] bytes = new byte[size];
    int state = 0x6D2B79F5;
    for (int index = 0; index < bytes.length; index++) {
      state ^= state << 13;
      state ^= state >>> 17;
      state ^= state << 5;
      bytes[index] = (byte) state;
    }
    return bytes;
  }
}
