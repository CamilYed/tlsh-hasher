package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TlshApiTest {

  @Test
  void shouldHashCompleteByteArrayThroughPublicFacade() {
    // given
    final byte[] input = deterministicBytes(1_000);

    // when
    final TlshDigest digest = Tlsh.hash(input);

    // then
    assertThat(digest.encoded())
        .isEqualTo("T10511A1808D0B3106EC1B03FE20B726CA2B2C3DB4C0B3DDE768024296D2134BA0AB30E4");
    assertThat(digest.toString()).isEqualTo(digest.encoded());
  }

  @Test
  void shouldProduceSameDigestForDifferentChunkBoundaries() {
    // given
    final byte[] input = deterministicBytes(4_096);
    final TlshDigest expected = Tlsh.hash(input);
    final TlshHasher hasher = Tlsh.newHasher();

    // when
    int offset = 0;
    while (offset < input.length) {
      final int chunkLength = Math.min(37, input.length - offset);
      hasher.update(input, offset, chunkLength);
      offset += chunkLength;
    }

    // then
    assertThat(hasher.finish()).isEqualTo(expected);
  }

  @Test
  void shouldProduceSameDigestWhenBytesAreAddedIndividually() {
    // given
    final byte[] input = deterministicBytes(1_000);
    final TlshDigest expected = Tlsh.hash(input);
    final TlshHasher hasher = Tlsh.newHasher();

    // when
    for (final byte value : input) {
      hasher.update(value);
    }

    // then
    assertThat(hasher.finish()).isEqualTo(expected);
  }

  @Test
  void shouldIgnoreEmptyUpdatesAndKeepFinishAsANonDestructiveSnapshot() {
    // given
    final byte[] input = deterministicBytes(1_000);
    final TlshHasher hasher = Tlsh.newHasher();

    // when
    hasher.update(new byte[0]);
    hasher.update(input, 0, 0);
    hasher.update(input);
    final TlshDigest firstSnapshot = hasher.finish();
    final TlshDigest secondSnapshot = hasher.finish();

    // then
    assertThat(firstSnapshot).isEqualTo(Tlsh.hash(input));
    assertThat(secondSnapshot).isEqualTo(firstSnapshot);
  }

  private static byte[] deterministicBytes(final int size) {
    final byte[] input = new byte[size];
    int state = 0x6D2B79F5 ^ size;
    for (int index = 0; index < input.length; index++) {
      state ^= state << 13;
      state ^= state >>> 17;
      state ^= state << 5;
      input[index] = (byte) state;
    }
    return input;
  }
}
