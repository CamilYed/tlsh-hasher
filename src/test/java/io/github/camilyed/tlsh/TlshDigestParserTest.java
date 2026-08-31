package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class TlshDigestParserTest {

  private static final String ENCODED_DIGEST =
      "T10511A1808D0B3106EC1B03FE20B726CA2B2C3DB4C0B3DDE768024296D2134BA0AB30E4";

  @Test
  void shouldRestoreDigestThatFormatsToIdenticalText() {
    // when
    final TlshDigest digest = TlshDigest.parse(ENCODED_DIGEST);

    // then
    assertThat(digest.encoded()).isEqualTo(ENCODED_DIGEST);
    assertThat(digest.checksum()).isEqualTo(0x50);
    assertThat(digest.lengthCode()).isEqualTo(0x11);
    assertThat(digest.quartileRatios()).isEqualTo(0xA1);
  }

  @Test
  void shouldAcceptLowercaseHexadecimalAndReturnCanonicalUppercaseText() {
    // when
    final TlshDigest digest =
        TlshDigest.parse(
            ENCODED_DIGEST.substring(0, 2) + ENCODED_DIGEST.substring(2).toLowerCase());

    // then
    assertThat(digest.encoded()).isEqualTo(ENCODED_DIGEST);
  }

  @Test
  void shouldRejectInvalidLengthPrefixAndHexadecimalContent() {
    // then
    assertThatIllegalArgumentException().isThrownBy(() -> TlshDigest.parse(ENCODED_DIGEST + "00"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TlshDigest.parse("T2" + ENCODED_DIGEST.substring(2)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TlshDigest.parse(ENCODED_DIGEST.substring(0, 71) + "Z"));
  }
}
