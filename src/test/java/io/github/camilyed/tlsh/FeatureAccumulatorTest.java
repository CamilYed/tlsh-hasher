package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class FeatureAccumulatorTest {

  @Test
  void shouldAccumulateSixFeaturesWhenWindowBecomesFull() {
    // given
    final int[] permutationTable = new int[256];
    for (int i = 0; i < permutationTable.length; i++) {
      permutationTable[i] = (i * 73 + 41) & 0xff;
    }

    final BucketMapper bucketMapper = new BucketMapper(new PearsonHash(permutationTable));
    final Histogram histogram = new Histogram();
    final FeatureAccumulator featureAccumulator = new FeatureAccumulator(bucketMapper, histogram);

    // when
    final byte[] inputBytes = {'A', 'B', 'C', 'D', 'E'};
    for (final byte currentByte : inputBytes) {
      featureAccumulator.addByte(currentByte);
    }

    // then
    assertThat(histogram.hitCountAt(79)).isOne();
    assertThat(histogram.hitCountAt(240)).isOne();
    assertThat(histogram.hitCountAt(47)).isOne();
    assertThat(histogram.hitCountAt(115)).isOne();
    assertThat(histogram.hitCountAt(222)).isOne();
    assertThat(histogram.hitCountAt(184)).isOne();
  }

  @Test
  void shouldAccumulateSixFeaturesForEveryFullWindow() {
    // given
    final int[] permutationTable = new int[256];
    for (int permutationIndex = 0; permutationIndex < permutationTable.length; permutationIndex++) {
      permutationTable[permutationIndex] = (permutationIndex * 73 + 41) & 0xff;
    }

    final BucketMapper bucketMapper = new BucketMapper(new PearsonHash(permutationTable));
    final Histogram histogram = new Histogram();
    final FeatureAccumulator featureAccumulator = new FeatureAccumulator(bucketMapper, histogram);

    // when
    final byte[] firstFourBytes = {'A', 'B', 'C', 'D'};
    for (final byte currentByte : firstFourBytes) {
      featureAccumulator.addByte(currentByte);
    }

    // then
    assertThat(totalHitCount(histogram)).isZero();

    // when
    featureAccumulator.addByte((byte) 'E');

    // then
    assertThat(totalHitCount(histogram)).isEqualTo(6);

    // when
    featureAccumulator.addByte((byte) 'F');

    // then
    assertThat(totalHitCount(histogram)).isEqualTo(12);
  }

  private static int totalHitCount(final Histogram histogram) {
    int totalHitCount = 0;
    for (int bucketIndex = 0; bucketIndex < 256; bucketIndex++) {
      totalHitCount += histogram.hitCountAt(bucketIndex);
    }
    return totalHitCount;
  }
}
