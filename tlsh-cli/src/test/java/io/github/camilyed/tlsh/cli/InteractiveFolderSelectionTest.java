package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class InteractiveFolderSelectionTest {

  @Test
  void shouldDescribeTheAmountOfWorkWithoutOverflowingByteCounts() {
    final List<HashInput> inputs =
        List.of(
            new HashInput("a", Path.of("a"), Long.MAX_VALUE),
            new HashInput("b", Path.of("b"), 1L),
            new HashInput("c", Path.of("c"), 2L));
    final HashInputDiscovery.Result discovery =
        new HashInputDiscovery.Result(inputs, List.of(), true, 0);
    final InteractiveFolderSelection selection =
        new InteractiveFolderSelection(Path.of("."), true, discovery);

    assertThat(selection.inputs()).containsExactlyElementsOf(inputs);
    assertThat(selection.comparisonCount()).isEqualTo(3L);
    assertThat(selection.expectedBytes()).isEqualTo(Long.MAX_VALUE);
  }
}
