package io.github.camilyed.tlsh.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import io.github.camilyed.tlsh.TlshDigest;
import io.github.camilyed.tlsh.TlshHasher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the exported API without package access to any implementation classes. */
final class PublicApiConsumerTest {

  @Test
  void shouldSupportCompleteConsumerWorkflow(@TempDir final Path temporaryDirectory)
      throws IOException {
    // given
    final byte[] input = new byte[4_096];
    new Random(0x5EEDL).nextBytes(input);
    final Path inputPath = temporaryDirectory.resolve("consumer-input.bin");
    Files.write(inputPath, input);

    // when
    final TlshDigest fromArray = Tlsh.hash(input);
    final TlshDigest fromStream = Tlsh.hash(new ByteArrayInputStream(input));
    final TlshDigest fromPath = Tlsh.hash(inputPath);
    final TlshHasher incrementalHasher = Tlsh.newHasher();
    incrementalHasher.update(input, 0, 1_000);
    incrementalHasher.update(input, 1_000, input.length - 1_000);
    final TlshDigest fromChunks = incrementalHasher.finish();
    final TlshDigest parsed = TlshDigest.parse(fromArray.encoded());

    // then
    assertThat(fromStream).isEqualTo(fromArray);
    assertThat(fromPath).isEqualTo(fromArray);
    assertThat(fromChunks).isEqualTo(fromArray);
    assertThat(parsed).isEqualTo(fromArray);
    assertThat(parsed.distanceTo(fromArray)).isZero();
    assertThat(parsed.distanceToIgnoringLength(fromArray)).isZero();
  }
}
