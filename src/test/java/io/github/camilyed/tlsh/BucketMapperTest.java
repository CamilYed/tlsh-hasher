package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class BucketMapperTest {

  @Test
  void shouldMapSixTripletsFromFullWindow() {
    // given
    final int[] permutationTable = new int[256];
    for (int i = 0; i < permutationTable.length; i++) {
      permutationTable[i] = (i * 73 + 41) & 0xff;
    }

    final PearsonHash pearsonHash = new PearsonHash(permutationTable);
    final BucketMapper bucketMapper = new BucketMapper(pearsonHash);
    final SlidingWindow slidingWindow = fullWindow();
    final Histogram histogram = new Histogram();

    // when
    bucketMapper.mapWindowIntoHistogram(slidingWindow, histogram);

    // then
    assertThat(hitBucketIndices(histogram)).containsExactly(47, 79, 115, 184, 222, 240);
    assertThat(totalHitCount(histogram)).isEqualTo(6);
  }

  @Test
  void shouldMapWindowUsingOfficialTlshPermutation() {
    // given
    final BucketMapper bucketMapper = new BucketMapper(new PearsonHash());
    final SlidingWindow slidingWindow = fullWindow();
    final Histogram histogram = new Histogram();

    // when
    bucketMapper.mapWindowIntoHistogram(slidingWindow, histogram);

    // then
    assertThat(hitBucketIndices(histogram)).containsExactly(55, 105, 112, 181, 242, 243);
    assertThat(totalHitCount(histogram)).isEqualTo(6);
  }

  private static SlidingWindow fullWindow() {
    final SlidingWindow slidingWindow = new SlidingWindow();
    for (final byte currentByte : new byte[] {'A', 'B', 'C', 'D', 'E'}) {
      slidingWindow.addByte(currentByte);
    }
    return slidingWindow;
  }

  private static int[] hitBucketIndices(final Histogram histogram) {
    final int[] bucketIndices = new int[6];
    int resultIndex = 0;
    for (int bucketIndex = 0; bucketIndex < 256; bucketIndex++) {
      if (histogram.hitCountAt(bucketIndex) > 0) {
        bucketIndices[resultIndex] = bucketIndex;
        resultIndex++;
      }
    }
    return bucketIndices;
  }

  private static long totalHitCount(final Histogram histogram) {
    long totalHitCount = 0;
    for (int bucketIndex = 0; bucketIndex < 256; bucketIndex++) {
      totalHitCount += histogram.hitCountAt(bucketIndex);
    }
    return totalHitCount;
  }
}
