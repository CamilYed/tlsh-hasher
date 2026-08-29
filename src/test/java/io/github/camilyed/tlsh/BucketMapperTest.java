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
    final byte[] windowBytes = {'A', 'B', 'C', 'D', 'E'};

    // when
    final int[] bucketIndices = bucketMapper.mapWindowToBucketIndices(windowBytes);

    // then
    assertThat(bucketIndices).containsExactly(79, 240, 47, 115, 222, 184);
  }

  @Test
  void shouldMapWindowUsingOfficialTlshPermutation() {
    // given
    final BucketMapper bucketMapper = new BucketMapper(new PearsonHash());
    final byte[] windowBytes = {'A', 'B', 'C', 'D', 'E'};

    // when
    final int[] bucketIndices = bucketMapper.mapWindowToBucketIndices(windowBytes);

    // then
    assertThat(bucketIndices).containsExactly(55, 242, 243, 112, 105, 181);
  }
}
