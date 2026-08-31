package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class HistogramQuantizerTest {

  @Test
  void shouldRejectAnythingOtherThan128BucketCounts() {
    // given
    final HistogramQuantizer quantizer = new HistogramQuantizer();
    final HistogramQuartiles quartiles = new HistogramQuartiles(10, 20, 30);

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> quantizer.quantize(new long[127], quartiles));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> quantizer.quantize(new long[129], quartiles));
  }

  @Test
  void shouldRejectQuartilesInDescendingOrder() {
    // given
    final HistogramQuantizer quantizer = new HistogramQuantizer();
    final long[] bucketCounts = new long[128];

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> quantizer.quantize(bucketCounts, new HistogramQuartiles(20, 10, 30)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> quantizer.quantize(bucketCounts, new HistogramQuartiles(10, 30, 20)));
  }

  @Test
  void shouldAssignTwoBitValueUsingQuartileBoundaries() {
    // given
    final long[] bucketCounts = new long[128];
    bucketCounts[0] = 0;
    bucketCounts[1] = 10;
    bucketCounts[2] = 11;
    bucketCounts[3] = 20;
    bucketCounts[4] = 21;
    bucketCounts[5] = 30;
    bucketCounts[6] = 31;
    final HistogramQuartiles quartiles = new HistogramQuartiles(10, 20, 30);
    final HistogramQuantizer quantizer = new HistogramQuantizer();

    // when
    final int[] quantizedBucketValues = quantizer.quantize(bucketCounts, quartiles);

    // then
    assertThat(quantizedBucketValues).hasSize(128).startsWith(0, 0, 1, 1, 2, 2, 3);
  }

  @Test
  void shouldQuantizeWithoutChangingBucketCounts() {
    // given
    final long[] bucketCounts = new long[128];
    bucketCounts[0] = 5;
    bucketCounts[1] = 15;
    bucketCounts[2] = 25;
    bucketCounts[3] = 35;
    final long[] originalBucketCounts = bucketCounts.clone();
    final HistogramQuartiles quartiles = new HistogramQuartiles(10, 20, 30);
    final HistogramQuantizer quantizer = new HistogramQuantizer();

    // when
    quantizer.quantize(bucketCounts, quartiles);

    // then
    assertThat(bucketCounts).containsExactly(originalBucketCounts);
  }
}
