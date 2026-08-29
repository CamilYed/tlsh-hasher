package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SlidingWindowTest {

  @Test
  void shouldBecomeFullAfterFifthByteAndShiftWhenNextByteIsAdded() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final byte[] firstFragment = {'A', 'B', 'C', 'D'};

    // when
    boolean isWindowFull = false;
    for (final byte currentByte : firstFragment) {
      isWindowFull = slidingWindow.addByte(currentByte);
    }

    // then
    assertThat(isWindowFull).isFalse();

    // when
    isWindowFull = slidingWindow.addByte((byte) 'E');

    // then
    assertThat(isWindowFull).isTrue();
    assertThat(slidingWindow.snapshot()).containsExactly(new byte[] {'A', 'B', 'C', 'D', 'E'});

    // when
    isWindowFull = slidingWindow.addByte((byte) 'F');

    // then
    assertThat(isWindowFull).isTrue();
    assertThat(slidingWindow.snapshot()).containsExactly(new byte[] {'B', 'C', 'D', 'E', 'F'});
  }

  @Test
  void shouldNotExposeInternalWindowState() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final byte[] bytes = {'A', 'B', 'C', 'D', 'E'};
    for (final byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // when
    final byte[] windowSnapshot = slidingWindow.snapshot();
    windowSnapshot[0] = 'F';

    // then
    assertThat(slidingWindow.snapshot()).containsExactly(bytes);
  }

  @Test
  void shouldReturnNoTripletsBeforeWindowIsFull() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final byte[] firstFourBytes = {'A', 'B', 'C', 'D'};

    // then
    assertThat(slidingWindow.triplets()).as("triplets before adding any bytes").isEmpty();

    for (int index = 0; index < firstFourBytes.length; index++) {
      // when
      slidingWindow.addByte(firstFourBytes[index]);

      // then
      assertThat(slidingWindow.triplets())
          .as("triplets after adding %s byte(s)", index + 1)
          .isEmpty();
    }
  }

  @Test
  void shouldReturnWindowTriplets() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final byte[] bytes = {'A', 'B', 'C', 'D', 'E'};
    for (final byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // when
    final List<byte[]> initialTriplets = slidingWindow.triplets();

    // then
    assertThat(initialTriplets).hasSize(6);
    assertThat(initialTriplets.get(0)).containsExactly(new byte[] {'A', 'B', 'E'});
    assertThat(initialTriplets.get(1)).containsExactly(new byte[] {'A', 'C', 'E'});
    assertThat(initialTriplets.get(2)).containsExactly(new byte[] {'A', 'D', 'E'});
    assertThat(initialTriplets.get(3)).containsExactly(new byte[] {'B', 'C', 'E'});
    assertThat(initialTriplets.get(4)).containsExactly(new byte[] {'B', 'D', 'E'});
    assertThat(initialTriplets.get(5)).containsExactly(new byte[] {'C', 'D', 'E'});

    // when
    slidingWindow.addByte((byte) 'F');

    // then
    final List<byte[]> shiftedTriplets = slidingWindow.triplets();
    assertThat(shiftedTriplets).hasSize(6);
    assertThat(shiftedTriplets.get(0)).containsExactly(new byte[] {'B', 'C', 'F'});
    assertThat(shiftedTriplets.get(1)).containsExactly(new byte[] {'B', 'D', 'F'});
    assertThat(shiftedTriplets.get(2)).containsExactly(new byte[] {'B', 'E', 'F'});
    assertThat(shiftedTriplets.get(3)).containsExactly(new byte[] {'C', 'D', 'F'});
    assertThat(shiftedTriplets.get(4)).containsExactly(new byte[] {'C', 'E', 'F'});
    assertThat(shiftedTriplets.get(5)).containsExactly(new byte[] {'D', 'E', 'F'});
  }

  @Test
  void shouldKeepGeneratingTripletsAfterMultipleWindowShifts() {
    // given
    final SlidingWindow slidingWindow = new SlidingWindow();
    final byte[] bytes = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};

    // when
    for (final byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // then
    assertThat(slidingWindow.snapshot()).containsExactly(new byte[] {'D', 'E', 'F', 'G', 'H'});

    final List<byte[]> triplets = slidingWindow.triplets();
    assertThat(triplets).hasSize(6);
    assertThat(triplets.get(0)).containsExactly(new byte[] {'D', 'E', 'H'});
    assertThat(triplets.get(1)).containsExactly(new byte[] {'D', 'F', 'H'});
    assertThat(triplets.get(2)).containsExactly(new byte[] {'D', 'G', 'H'});
    assertThat(triplets.get(3)).containsExactly(new byte[] {'E', 'F', 'H'});
    assertThat(triplets.get(4)).containsExactly(new byte[] {'E', 'G', 'H'});
    assertThat(triplets.get(5)).containsExactly(new byte[] {'F', 'G', 'H'});
  }
}
