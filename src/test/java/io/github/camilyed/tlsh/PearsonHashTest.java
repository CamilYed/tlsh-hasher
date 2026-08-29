package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PearsonHashTest {

  @Test
  void shouldMapThreeBytesUsingSaltAndPermutationTable() {
    // given
    int[] permutation = new int[256];
    for (int i = 0; i < permutation.length; i++) {
      permutation[i] = i;
    }

    int[] firstEightValues = {3, 6, 1, 5, 7, 0, 4, 2};
    System.arraycopy(firstEightValues, 0, permutation, 0, firstEightValues.length);
    PearsonHash pearsonHash = new PearsonHash(permutation);

    // when
    int result = pearsonHash.map(2, (byte) 5, (byte) 3, (byte) 6);

    // then
    assertThat(result).isEqualTo(6);
  }

  @Test
  void shouldRejectPermutationWithInvalidSize() {
    // given
    int[] invalidPermutation = new int[255];

    // when / then
    assertThatThrownBy(() -> new PearsonHash(invalidPermutation))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Permutation must contain exactly 256 values");
  }

  @Test
  @Disabled
  void shouldRejectSaltOutsideByteRange() {
    // given
    int[] permutation = new int[256];
    for (int i = 0; i < permutation.length; i++) {
      permutation[i] = i;
    }
    PearsonHash pearsonHash = new PearsonHash(permutation);

    // when / then
    assertThatThrownBy(() -> pearsonHash.map(-1, (byte) 0, (byte) 0, (byte) 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Salt must be between 0 and 255");

    assertThatThrownBy(() -> pearsonHash.map(256, (byte) 0, (byte) 0, (byte) 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Salt must be between 0 and 255");
  }
}
