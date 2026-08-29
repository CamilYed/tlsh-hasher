package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class HistogramTest {

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
