package io.github.camilyed.tlsh.cli;

/** Converts expected hashing exceptions into concise explanations shared by every file workflow. */
final class HashFailureDetail {

  private static final long MINIMUM_TLSH_INPUT_BYTES = 256L;
  private static final long MAXIMUM_TLSH_INPUT_BYTES = 4_224_281_216L;

  private HashFailureDetail() {}

  /** Uses a known size to distinguish length failures from insufficient feature diversity. */
  static String explain(final long expectedBytes, final Exception exception) {
    if (exception instanceof IllegalStateException) {
      if (expectedBytes >= 0L && expectedBytes < MINIMUM_TLSH_INPUT_BYTES) {
        return "input is " + HumanUnits.bytes(expectedBytes) + "; TLSH requires at least 256 B";
      }
      if (expectedBytes > MAXIMUM_TLSH_INPUT_BYTES) {
        return "input is "
            + HumanUnits.bytes(expectedBytes)
            + "; TLSH supports at most "
            + HumanUnits.bytes(MAXIMUM_TLSH_INPUT_BYTES);
      }
      return "content does not have enough feature diversity for a TLSH digest";
    }
    return TlshCli.message(exception);
  }
}
