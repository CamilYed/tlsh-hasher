package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TlshDistanceTest {

  private static final TlshDigest DIGEST_256 =
      TlshDigest.parse("T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570");
  private static final TlshDigest DIGEST_1_000 =
      TlshDigest.parse("T10511A1808D0B3106EC1B03FE20B726CA2B2C3DB4C0B3DDE768024296D2134BA0AB30E4");
  private static final TlshDigest DIGEST_4_096 =
      TlshDigest.parse("T18B815EE5E8724BE24429FC3B27CA1F713ADB15A8A4584DC127D6A0960F4B504F3A1DF2");
  private static final TlshDigest DIGEST_65_536 =
      TlshDigest.parse("T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C");

  @Test
  void shouldReturnZeroForSameDigestAndBeSymmetric() {
    assertThat(DIGEST_256.distanceTo(DIGEST_256)).isZero();
    assertThat(DIGEST_256.distanceTo(DIGEST_1_000)).isEqualTo(DIGEST_1_000.distanceTo(DIGEST_256));
  }

  @Test
  void shouldMatchOfficialDifferenceScores() {
    assertThat(DIGEST_256.distanceTo(DIGEST_1_000)).isEqualTo(391);
    assertThat(DIGEST_256.distanceTo(DIGEST_4_096)).isEqualTo(379);
    assertThat(DIGEST_256.distanceTo(DIGEST_65_536)).isEqualTo(766);
    assertThat(DIGEST_1_000.distanceTo(DIGEST_4_096)).isEqualTo(394);
    assertThat(DIGEST_1_000.distanceTo(DIGEST_65_536)).isEqualTo(700);
    assertThat(DIGEST_4_096.distanceTo(DIGEST_65_536)).isEqualTo(664);
  }

  @Test
  void shouldMatchOfficialDifferenceScoresWithoutLength() {
    assertThat(DIGEST_256.distanceTo(DIGEST_1_000, false)).isEqualTo(343);
    assertThat(DIGEST_256.distanceTo(DIGEST_4_096, false)).isEqualTo(247);
    assertThat(DIGEST_256.distanceTo(DIGEST_65_536, false)).isEqualTo(286);
    assertThat(DIGEST_1_000.distanceTo(DIGEST_4_096, false)).isEqualTo(310);
    assertThat(DIGEST_1_000.distanceTo(DIGEST_65_536, false)).isEqualTo(268);
    assertThat(DIGEST_4_096.distanceTo(DIGEST_65_536, false)).isEqualTo(316);
  }
}
