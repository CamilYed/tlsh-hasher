package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class TlshDigestTest {

  @Test
  void shouldCompareDigestsByAllComponentValues() {
    // given
    final byte[] firstHistogramCode = new byte[32];
    firstHistogramCode[0] = (byte) 0xE4;
    final byte[] secondHistogramCode = firstHistogramCode.clone();
    final byte[] differentHistogramCode = firstHistogramCode.clone();
    differentHistogramCode[0] = (byte) 0x1B;

    // when
    final TlshDigest first = new TlshDigest(92, 15, 0xEC, firstHistogramCode);
    final TlshDigest equal = new TlshDigest(92, 15, 0xEC, secondHistogramCode);
    final TlshDigest different = new TlshDigest(92, 15, 0xEC, differentHistogramCode);

    // then
    assertThat(first).isEqualTo(equal).hasSameHashCodeAs(equal).isNotEqualTo(different);
  }

  @Test
  void shouldRejectChecksumOutsideUnsignedByteRange() {
    // given
    final byte[] histogramCode = new byte[32];

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(-1, 15, 0xEC, histogramCode));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(256, 15, 0xEC, histogramCode));
  }

  @Test
  void shouldRejectLengthCodeOutsideSupportedRange() {
    // given
    final byte[] histogramCode = new byte[32];

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, -1, 0xEC, histogramCode));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, 170, 0xEC, histogramCode));
  }

  @Test
  void shouldRejectQuartileRatiosOutsideUnsignedByteRange() {
    // given
    final byte[] histogramCode = new byte[32];

    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, 15, -1, histogramCode));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, 15, 256, histogramCode));
  }

  @Test
  void shouldRejectHistogramCodeWithInvalidSize() {
    // then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, 15, 0xEC, new byte[31]));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TlshDigest(92, 15, 0xEC, new byte[33]));
  }

  @Test
  void shouldPreserveComponentsWithoutExposingHistogramCodeArray() {
    // given
    final byte[] histogramCode = new byte[32];
    histogramCode[0] = (byte) 0xE4;
    final TlshDigest digest = new TlshDigest(92, 15, 0xEC, histogramCode);

    // when
    histogramCode[0] = 0;
    final byte[] returnedHistogramCode = digest.histogramCode();
    returnedHistogramCode[0] = 0;

    // then
    assertThat(digest.checksum()).isEqualTo(92);
    assertThat(digest.lengthCode()).isEqualTo(15);
    assertThat(digest.quartileRatios()).isEqualTo(0xEC);
    assertThat(digest.histogramCode()).hasSize(32).startsWith((byte) 0xE4);
  }
}
