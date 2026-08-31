package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class HistogramCodePackerTest {

  @Test
  void shouldRejectAnythingOtherThan128QuantizedBucketValues() {
    // given
    final HistogramCodePacker packer = new HistogramCodePacker();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> packer.pack(new int[127]));
    assertThatIllegalArgumentException().isThrownBy(() -> packer.pack(new int[129]));
  }

  @Test
  void shouldRejectValuesOutsideTwoBitRange() {
    // given
    final int[] quantizedBucketValues = new int[128];
    final HistogramCodePacker packer = new HistogramCodePacker();

    // then
    quantizedBucketValues[0] = -1;
    assertThatIllegalArgumentException().isThrownBy(() -> packer.pack(quantizedBucketValues));

    quantizedBucketValues[0] = 4;
    assertThatIllegalArgumentException().isThrownBy(() -> packer.pack(quantizedBucketValues));
  }

  @Test
  void shouldPackFourTwoBitValuesIntoEachByte() {
    // given
    final int[] quantizedBucketValues = new int[128];
    quantizedBucketValues[0] = 0;
    quantizedBucketValues[1] = 1;
    quantizedBucketValues[2] = 2;
    quantizedBucketValues[3] = 3;
    quantizedBucketValues[4] = 3;
    quantizedBucketValues[5] = 2;
    quantizedBucketValues[6] = 1;
    quantizedBucketValues[7] = 0;
    final byte[] expectedCode = new byte[32];
    expectedCode[0] = (byte) 0b1110_0100;
    expectedCode[1] = (byte) 0b0001_1011;
    final HistogramCodePacker packer = new HistogramCodePacker();

    // when
    final byte[] code = packer.pack(quantizedBucketValues);

    // then
    assertThat(code).containsExactly(expectedCode);
  }

  @Test
  void shouldPackWithoutChangingQuantizedBucketValues() {
    // given
    final int[] quantizedBucketValues = new int[128];
    quantizedBucketValues[0] = 3;
    quantizedBucketValues[1] = 2;
    quantizedBucketValues[2] = 1;
    final int[] originalValues = quantizedBucketValues.clone();
    final HistogramCodePacker packer = new HistogramCodePacker();

    // when
    packer.pack(quantizedBucketValues);

    // then
    assertThat(quantizedBucketValues).containsExactly(originalValues);
  }
}
