package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TlshDigestFormatterTest {

  @Test
  void shouldFormatDigestUsingVersionedUppercaseHexadecimalRepresentation() {
    // given
    final byte[] histogramCode = new byte[32];
    for (int codeIndex = 0; codeIndex < histogramCode.length; codeIndex++) {
      histogramCode[codeIndex] = (byte) codeIndex;
    }
    final TlshDigest digest = new TlshDigest(0xAB, 0x2D, 0xEC, histogramCode);
    final TlshDigestFormatter formatter = new TlshDigestFormatter();

    // when
    final String encodedDigest = formatter.format(digest);

    // then
    assertThat(encodedDigest)
        .isEqualTo("T1BAD2EC1F1E1D1C1B1A19181716151413121110" + "0F0E0D0C0B0A09080706050403020100");
  }
}
