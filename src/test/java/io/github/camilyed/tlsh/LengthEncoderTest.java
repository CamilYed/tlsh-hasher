package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class LengthEncoderTest {

  @Test
  void shouldRejectNonPositiveInputLength() {
    // given
    final LengthEncoder lengthEncoder = new LengthEncoder();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> lengthEncoder.encode(0));
    assertThatIllegalArgumentException().isThrownBy(() -> lengthEncoder.encode(-1));
  }

  @Test
  void shouldRejectInputLengthAboveMaximumSupportedRange() {
    // given
    final LengthEncoder lengthEncoder = new LengthEncoder();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> lengthEncoder.encode(4_224_281_217L));
  }

  @Test
  void shouldEncodeExactLengthIntoIncreasinglyWideRanges() {
    // given
    final LengthEncoder lengthEncoder = new LengthEncoder();

    // then
    assertThat(lengthEncoder.encode(1)).isZero();
    assertThat(lengthEncoder.encode(2)).isEqualTo(1);
    assertThat(lengthEncoder.encode(3)).isEqualTo(2);
    assertThat(lengthEncoder.encode(4)).isEqualTo(3);
    assertThat(lengthEncoder.encode(5)).isEqualTo(3);
    assertThat(lengthEncoder.encode(6)).isEqualTo(4);
    assertThat(lengthEncoder.encode(7)).isEqualTo(4);
    assertThat(lengthEncoder.encode(8)).isEqualTo(5);
    assertThat(lengthEncoder.encode(50)).isEqualTo(9);
    assertThat(lengthEncoder.encode(256)).isEqualTo(13);
    assertThat(lengthEncoder.encode(656)).isEqualTo(15);
    assertThat(lengthEncoder.encode(657)).isEqualTo(16);
    assertThat(lengthEncoder.encode(1_000)).isEqualTo(17);
    assertThat(lengthEncoder.encode(1_110)).isEqualTo(17);
    assertThat(lengthEncoder.encode(1_111)).isEqualTo(18);
    assertThat(lengthEncoder.encode(1_000_000)).isEqualTo(82);
    assertThat(lengthEncoder.encode(4_224_281_216L)).isEqualTo(169);
  }
}
