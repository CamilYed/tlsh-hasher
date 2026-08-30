package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class HistogramTest {

  @Test
  void shouldReturnDefensiveCopyOfFirst128EffectiveBucketCounts() {
    // given
    final Histogram histogram = new Histogram();
    histogram.recordHit(0);
    histogram.recordHit(127);
    histogram.recordHit(127);
    histogram.recordHit(128);
    histogram.recordHit(255);

    // when
    final int[] effectiveBucketCounts = histogram.effectiveBucketCounts();

    // then
    assertThat(effectiveBucketCounts).hasSize(128);
    assertThat(effectiveBucketCounts[0]).isOne();
    assertThat(effectiveBucketCounts[127]).isEqualTo(2);

    // when
    effectiveBucketCounts[0] = 100;

    // then
    assertThat(histogram.effectiveBucketCounts()[0]).isOne();
    assertThat(histogram.hitCountAt(128)).isOne();
    assertThat(histogram.hitCountAt(255)).isOne();
  }

  @Test
  void shouldCountRepeatedBucketHits() {
    // given
    final Histogram histogram = new Histogram();

    // when
    histogram.recordHit(79);
    histogram.recordHit(79);
    histogram.recordHit(240);

    // then
    assertThat(histogram.hitCountAt(79)).isEqualTo(2);
    assertThat(histogram.hitCountAt(240)).isEqualTo(1);
    assertThat(histogram.hitCountAt(0)).isZero();
  }
}
