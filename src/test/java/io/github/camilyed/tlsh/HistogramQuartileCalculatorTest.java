package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class HistogramQuartileCalculatorTest {

  @Test
  void shouldPreserveBucketCountsAboveIntegerRange() {
    // given
    final long[] bucketCounts = new long[128];
    for (int bucketIndex = 0; bucketIndex < bucketCounts.length; bucketIndex++) {
      bucketCounts[bucketIndex] = 3_000_000_000L + bucketIndex;
    }
    final HistogramQuartileCalculator calculator = new HistogramQuartileCalculator();

    // when
    final HistogramQuartiles quartiles = calculator.calculate(bucketCounts);

    // then
    assertThat(quartiles.firstQuartile()).isEqualTo(3_000_000_031L);
    assertThat(quartiles.secondQuartile()).isEqualTo(3_000_000_063L);
    assertThat(quartiles.thirdQuartile()).isEqualTo(3_000_000_095L);
  }

  @Test
  void shouldRejectAnythingOtherThan128BucketCounts() {
    // given
    final HistogramQuartileCalculator calculator = new HistogramQuartileCalculator();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(new long[127]));
    assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(new long[256]));
  }

  @Test
  void shouldCalculateQuartilesWithoutChangingBucketOrder() {
    // given
    final long[] bucketCounts = new long[128];
    for (int bucketIndex = 0; bucketIndex < bucketCounts.length; bucketIndex++) {
      bucketCounts[bucketIndex] = bucketCounts.length - bucketIndex;
    }
    final long[] originalBucketCounts = bucketCounts.clone();
    final HistogramQuartileCalculator calculator = new HistogramQuartileCalculator();

    // when
    final HistogramQuartiles quartiles = calculator.calculate(bucketCounts);

    // then
    assertThat(quartiles.firstQuartile()).isEqualTo(32);
    assertThat(quartiles.secondQuartile()).isEqualTo(64);
    assertThat(quartiles.thirdQuartile()).isEqualTo(96);
    assertThat(bucketCounts).containsExactly(originalBucketCounts);
  }
}
