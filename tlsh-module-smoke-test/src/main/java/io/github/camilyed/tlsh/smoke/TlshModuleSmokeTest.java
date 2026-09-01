package io.github.camilyed.tlsh.smoke;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import io.github.camilyed.tlsh.TlshHasher;
import java.util.Random;

/** Runs a small consumer workflow from a separate named Java module. */
public final class TlshModuleSmokeTest {

  private static final String FIRST_ENCODED_DIGEST =
      "T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570";
  private static final String SECOND_ENCODED_DIGEST =
      "T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C";

  private TlshModuleSmokeTest() {}

  /**
   * Verifies digest parsing, comparison, one-shot hashing, and incremental hashing.
   *
   * @param arguments ignored command-line arguments
   */
  public static void main(final String[] arguments) {
    final TlshDigest first = TlshDigest.parse(FIRST_ENCODED_DIGEST);
    final TlshDigest second = TlshDigest.parse(SECOND_ENCODED_DIGEST);
    require(first.distanceTo(second) == 766, "distance including length");
    require(first.distanceToIgnoringLength(second) == 286, "distance excluding length");

    final byte[] input = new byte[4_096];
    new Random(0x5EEDL).nextBytes(input);
    final TlshDigest completeDigest = Tlsh.hash(input);
    final TlshHasher hasher = Tlsh.newHasher();
    hasher.update(input, 0, 1_000);
    hasher.update(input, 1_000, input.length - 1_000);
    require(hasher.finish().equals(completeDigest), "incremental hash");
  }

  /** Throws a descriptive failure instead of relying on optionally disabled Java assertions. */
  private static void require(final boolean condition, final String operation) {
    if (!condition) {
      throw new IllegalStateException("Named-module smoke test failed: " + operation);
    }
  }
}
