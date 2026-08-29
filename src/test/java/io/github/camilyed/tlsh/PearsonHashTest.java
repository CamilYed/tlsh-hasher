package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class PearsonHashTest {

  @Test
  void shouldMapThreeBytesUsingSaltAndPermutationTable() {
    // given
    final int[] permutationTable = new int[256];
    for (int i = 0; i < permutationTable.length; i++) {
      permutationTable[i] = i;
    }

    final int[] firstEightValues = {3, 6, 1, 5, 7, 0, 4, 2};
    System.arraycopy(firstEightValues, 0, permutationTable, 0, firstEightValues.length);
    final PearsonHash pearsonHash = new PearsonHash(permutationTable);

    // when
    final int bucketIndex = pearsonHash.mapToBucketIndex(2, (byte) 5, (byte) 3, (byte) 6);

    // then
    assertThat(bucketIndex).isEqualTo(6);
  }

  @Test
  void shouldRejectPermutationWithInvalidSize() {
    // given
    final int[] invalidPermutationTable = new int[255];

    // when / then
    assertThatThrownBy(() -> new PearsonHash(invalidPermutationTable))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Permutation must contain exactly 256 values");
  }

  @Test
  void shouldRejectSaltOutsideByteRange() {
    // given
    final int[] permutationTable = new int[256];
    for (int i = 0; i < permutationTable.length; i++) {
      permutationTable[i] = i;
    }
    final PearsonHash pearsonHash = new PearsonHash(permutationTable);

    // when / then
    assertThatThrownBy(() -> pearsonHash.mapToBucketIndex(-1, (byte) 0, (byte) 0, (byte) 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Salt must be between 0 and 255");

    assertThatThrownBy(() -> pearsonHash.mapToBucketIndex(256, (byte) 0, (byte) 0, (byte) 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Salt must be between 0 and 255");
  }
}
