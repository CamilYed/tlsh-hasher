package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HistogramTest {

  @Test
  void shouldCountRepeatedBucketHits() {
    // given
    Histogram histogram = new Histogram();

    // when
    histogram.increment(79);
    histogram.increment(79);
    histogram.increment(240);

    // then
    assertThat(histogram.countAt(79)).isEqualTo(2);
    assertThat(histogram.countAt(240)).isEqualTo(1);
    assertThat(histogram.countAt(0)).isZero();
  }
}
