package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PearsonHashTest {

  @Test
  void shouldMapThreeBytesUsingSaltAndPermutationTable() {
    // given
    int[] permutation = {3, 6, 1, 5, 7, 0, 4, 2};
    PearsonHash pearsonHash = new PearsonHash(permutation);

    // when
    int result = pearsonHash.map(2, (byte) 5, (byte) 3, (byte) 6);

    // then
    assertThat(result).isEqualTo(6);
  }
}
