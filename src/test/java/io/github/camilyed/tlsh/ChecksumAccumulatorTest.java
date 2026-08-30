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

  @Test
  void shouldUpdateChecksumOnlyAfterSlidingWindowIsFull() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final ChecksumAccumulator checksumAccumulator = new ChecksumAccumulator(new PearsonHash());

    // when
    final byte[] bytesBeforeFullWindow = {'A', 'B', 'C', 'D'};
    for (final byte currentByte : bytesBeforeFullWindow) {
      final boolean windowIsFull = slidingWindow.addByte(currentByte);

      // then
      assertThat(windowIsFull).isFalse();
    }

    // then
    assertThat(checksumAccumulator.value()).isZero();

    // when
    final boolean firstWindowIsFull = slidingWindow.addByte((byte) 'E');
    final byte[] firstFullWindow = slidingWindow.snapshot();
    checksumAccumulator.update(firstFullWindow[4], firstFullWindow[3]);

    // then
    assertThat(firstWindowIsFull).isTrue();
    assertThat(firstFullWindow).containsExactly('A', 'B', 'C', 'D', 'E');
    assertThat(checksumAccumulator.value()).isEqualTo(92);

    // when
    final boolean secondWindowIsFull = slidingWindow.addByte((byte) 'F');
    final byte[] secondFullWindow = slidingWindow.snapshot();
    checksumAccumulator.update(secondFullWindow[4], secondFullWindow[3]);

    // then
    assertThat(secondWindowIsFull).isTrue();
    assertThat(secondFullWindow).containsExactly('B', 'C', 'D', 'E', 'F');
    assertThat(checksumAccumulator.value()).isEqualTo(96);
  }
}
