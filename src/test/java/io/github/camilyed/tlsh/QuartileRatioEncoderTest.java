package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class QuartileRatioEncoderTest {

  @Test
  void shouldRejectZeroThirdQuartile() {
    // given
    final HistogramQuartiles quartiles = new HistogramQuartiles(0, 0, 0);
    final QuartileRatioEncoder encoder = new QuartileRatioEncoder();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode(quartiles));
  }

  @Test
  void shouldRejectNegativeQuartiles() {
    // given
    final HistogramQuartiles quartiles = new HistogramQuartiles(-1, 0, 1);
    final QuartileRatioEncoder encoder = new QuartileRatioEncoder();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode(quartiles));
  }

  @Test
  void shouldRejectQuartilesInDescendingOrder() {
    // given
    final QuartileRatioEncoder encoder = new QuartileRatioEncoder();

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> encoder.encode(new HistogramQuartiles(2, 1, 3)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> encoder.encode(new HistogramQuartiles(1, 3, 2)));
  }

  @Test
  void shouldEncodeTwoQuartileRatiosIntoOneByte() {
    // given
    final HistogramQuartiles quartiles = new HistogramQuartiles(3, 6, 10);
    final QuartileRatioEncoder encoder = new QuartileRatioEncoder();

    // when
    final byte encodedRatios = encoder.encode(quartiles);

    // then
    assertThat(Byte.toUnsignedInt(encodedRatios)).isEqualTo(0xEC);
  }
}
