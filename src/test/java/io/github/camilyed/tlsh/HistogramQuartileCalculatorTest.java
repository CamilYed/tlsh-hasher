package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class HistogramQuartileCalculatorTest {

  @Test
  void shouldRejectAnythingOtherThan128BucketCounts() {
    // given
    final HistogramQuartileCalculator calculator = new HistogramQuartileCalculator();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(new int[127]));
    assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(new int[256]));
  }

  @Test
  void shouldCalculateQuartilesWithoutChangingBucketOrder() {
    // given
    final int[] bucketCounts = new int[128];
    for (int bucketIndex = 0; bucketIndex < bucketCounts.length; bucketIndex++) {
      bucketCounts[bucketIndex] = bucketCounts.length - bucketIndex;
    }
    final int[] originalBucketCounts = bucketCounts.clone();
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
