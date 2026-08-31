package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class TlshDigestEligibilityCheckerTest {

  @Test
  void shouldRejectAnythingOtherThan128EffectiveBucketCounts() {
    // given
    final TlshDigestEligibilityChecker checker = new TlshDigestEligibilityChecker();

    // then
    assertThatIllegalArgumentException().isThrownBy(() -> checker.isEligible(256, new long[127]));
    assertThatIllegalArgumentException().isThrownBy(() -> checker.isEligible(256, new long[129]));
  }

  @Test
  void shouldRejectInputLengthAboveMaximumSupportedRange() {
    // given
    final long[] sufficientlyDistributedBucketCounts = new long[128];
    Arrays.fill(sufficientlyDistributedBucketCounts, 0, 65, 1);
    final TlshDigestEligibilityChecker checker = new TlshDigestEligibilityChecker();

    // then
    assertThat(checker.isEligible(4_224_281_216L, sufficientlyDistributedBucketCounts)).isTrue();
    assertThat(checker.isEligible(4_224_281_217L, sufficientlyDistributedBucketCounts)).isFalse();
  }

  @Test
  void shouldRequireAtLeast256InputBytes() {
    // given
    final long[] sufficientlyDistributedBucketCounts = new long[128];
    Arrays.fill(sufficientlyDistributedBucketCounts, 0, 65, 1);
    final TlshDigestEligibilityChecker checker = new TlshDigestEligibilityChecker();

    // then
    assertThat(checker.isEligible(255, sufficientlyDistributedBucketCounts)).isFalse();
    assertThat(checker.isEligible(256, sufficientlyDistributedBucketCounts)).isTrue();
  }

  @Test
  void shouldRequireMoreThanHalfEffectiveBucketsToBeNonZero() {
    // given
    final long[] effectiveBucketCounts = new long[128];
    Arrays.fill(effectiveBucketCounts, 0, 64, 1);
    final TlshDigestEligibilityChecker checker = new TlshDigestEligibilityChecker();

    // then
    assertThat(checker.isEligible(256, effectiveBucketCounts)).isFalse();

    // when
    effectiveBucketCounts[64] = 1;

    // then
    assertThat(checker.isEligible(256, effectiveBucketCounts)).isTrue();
  }
}
