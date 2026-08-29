package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BucketMapperTest {

  @Test
  void shouldMapSixTripletsFromFullWindow() {
    // given
    int[] permutation = new int[256];
    for (int i = 0; i < permutation.length; i++) {
      permutation[i] = (i * 73 + 41) & 0xff;
    }

    PearsonHash pearsonHash = new PearsonHash(permutation);
    BucketMapper bucketMapper = new BucketMapper(pearsonHash);
    byte[] window = {'A', 'B', 'C', 'D', 'E'};

    // when
    int[] buckets = bucketMapper.map(window);

    // then
    assertThat(buckets).containsExactly(79, 240, 47, 115, 222, 184);
  }
}
