package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class TlshDigestAssemblerTest {

  @Test
  void shouldAssembleDigestFromAccumulatedInputStatistics() {
    // given
    final int[] effectiveBucketCounts = new int[128];
    Arrays.fill(effectiveBucketCounts, 0, 32, 3);
    Arrays.fill(effectiveBucketCounts, 32, 64, 6);
    Arrays.fill(effectiveBucketCounts, 64, 96, 10);
    Arrays.fill(effectiveBucketCounts, 96, 128, 11);

    final byte[] expectedHistogramCode = new byte[32];
    Arrays.fill(expectedHistogramCode, 8, 16, (byte) 0x55);
    Arrays.fill(expectedHistogramCode, 16, 24, (byte) 0xAA);
    Arrays.fill(expectedHistogramCode, 24, 32, (byte) 0xFF);
    final TlshDigest expectedDigest = new TlshDigest(92, 13, 0xEC, expectedHistogramCode);

    final TlshDigestAssembler assembler =
        new TlshDigestAssembler(
            new LengthEncoder(),
            new HistogramQuartileCalculator(),
            new HistogramQuantizer(),
            new HistogramCodePacker(),
            new QuartileRatioEncoder());

    // when
    final TlshDigest digest = assembler.assemble(256, 92, effectiveBucketCounts);

    // then
    assertThat(digest).isEqualTo(expectedDigest);
  }
}
