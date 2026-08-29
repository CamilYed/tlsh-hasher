package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ChecksumAccumulatorTest {

  @Test
  void shouldAccumulateChecksumAcrossConsecutiveWindows() {
    // given
    final ChecksumAccumulator checksumAccumulator = new ChecksumAccumulator(new PearsonHash());

    // then
    assertThat(checksumAccumulator.value()).isZero();

    // when
    checksumAccumulator.update((byte) 'E', (byte) 'D');

    // then
    assertThat(checksumAccumulator.value()).isEqualTo(92);

    // when
    checksumAccumulator.update((byte) 'F', (byte) 'E');

    // then
    assertThat(checksumAccumulator.value()).isEqualTo(96);
  }
}
