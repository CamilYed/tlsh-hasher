package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class TlshCompatibilityTest {

  @Test
  void shouldMatchOfficialTlshVersionFiveVectors() {
    // given
    final List<CompatibilityVector> vectors =
        List.of(
            new CompatibilityVector(
                256, "T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570"),
            new CompatibilityVector(
                1_000, "T10511A1808D0B3106EC1B03FE20B726CA2B2C3DB4C0B3DDE768024296D2134BA0AB30E4"),
            new CompatibilityVector(
                4_096, "T18B815EE5E8724BE24429FC3B27CA1F713ADB15A8A4584DC127D6A0960F4B504F3A1DF2"),
            new CompatibilityVector(
                65_536,
                "T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C"));
    final TlshDigestFormatter formatter = new TlshDigestFormatter();

    for (final CompatibilityVector vector : vectors) {
      final TlshAccumulator accumulator = newAccumulator();
      final byte[] input = deterministicBytes(vector.inputSize());

      // when
      for (final byte value : input) {
        accumulator.addByte(value);
      }
      final String actual = formatter.format(accumulator.finish());

      // then
      assertThat(actual).as("input size %s", vector.inputSize()).isEqualTo(vector.expectedDigest());
    }
  }

  private static TlshAccumulator newAccumulator() {
    final PearsonHash pearsonHash = new PearsonHash();
    return new TlshAccumulator(
        new BucketMapper(pearsonHash),
        new Histogram(),
        new ChecksumAccumulator(pearsonHash),
        new TlshDigestAssembler(
            new LengthEncoder(),
            new HistogramQuartileCalculator(),
            new HistogramQuantizer(),
            new HistogramCodePacker(),
            new QuartileRatioEncoder()),
        new TlshDigestEligibilityChecker());
  }

  private static byte[] deterministicBytes(final int size) {
    final byte[] input = new byte[size];
    int state = 0x6D2B79F5 ^ size;
    for (int index = 0; index < input.length; index++) {
      state ^= state << 13;
      state ^= state >>> 17;
      state ^= state << 5;
      input[index] = (byte) state;
    }
    return input;
  }

  private record CompatibilityVector(int inputSize, String expectedDigest) {}
}
