package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  @Test
  void shouldHashInputStreamWithoutClosingIt() throws IOException {
    // given
    final byte[] input = deterministicBytes(4_096);
    final CloseTrackingInputStream inputStream = new CloseTrackingInputStream(input);

    // when
    final TlshDigest digest = Tlsh.hash(inputStream);

    // then
    assertThat(digest).isEqualTo(Tlsh.hash(input));
    assertThat(inputStream.wasClosed()).isFalse();
  }

  @Test
  void shouldPropagateInputStreamReadFailure() {
    // given
    final InputStream failingInput =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("read failed");
          }
        };

    // then
    assertThatIOException().isThrownBy(() -> Tlsh.hash(failingInput)).withMessage("read failed");
  }

  @Test
  void shouldHashFileWithoutLoadingItAsACompleteByteArray(@TempDir final Path temporaryDirectory)
      throws IOException {
    // given
    final byte[] input = deterministicBytes(4_096);
    final Path inputFile = temporaryDirectory.resolve("input.bin");
    Files.write(inputFile, input);

    // when
    final TlshDigest digest = Tlsh.hash(inputFile);

    // then
    assertThat(digest).isEqualTo(Tlsh.hash(input));
  }

  @Test
  void shouldPropagateFailureWhenFileDoesNotExist(@TempDir final Path temporaryDirectory) {
    // given
    final Path missingFile = temporaryDirectory.resolve("missing.bin");

    // then
    assertThatIOException().isThrownBy(() -> Tlsh.hash(missingFile));
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

  private static final class CloseTrackingInputStream extends ByteArrayInputStream {

    private boolean closed;

    private CloseTrackingInputStream(final byte[] input) {
      super(input);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }

    private boolean wasClosed() {
      return closed;
    }
  }
}
