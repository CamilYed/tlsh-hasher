package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SlidingWindowTest {

  @Test
  void shouldBecomeFullAfterFifthByteAndShiftWhenNextByteIsAdded() {
    // given
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] firstFragment = {'A', 'B', 'C', 'D'};

    // when
    boolean isFull = false;
    for (byte currentByte : firstFragment) {
      isFull = slidingWindow.addByte(currentByte);
    }

    // then
    assertThat(isFull).isFalse();

    // when
    isFull = slidingWindow.addByte((byte) 'E');

    // then
    assertThat(isFull).isTrue();
    assertThat(slidingWindow.currentWindow()).containsExactly(new byte[] {'A', 'B', 'C', 'D', 'E'});

    // when
    isFull = slidingWindow.addByte((byte) 'F');

    // then
    assertThat(isFull).isTrue();
    assertThat(slidingWindow.currentWindow()).containsExactly(new byte[] {'B', 'C', 'D', 'E', 'F'});
  }

  @Test
  void shouldNotExposeInternalWindowState() {
    // given
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] bytes = {'A', 'B', 'C', 'D', 'E'};
    for (byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // when
    byte[] returnedWindow = slidingWindow.currentWindow();
    returnedWindow[0] = 'F';

    // then
    assertThat(slidingWindow.currentWindow()).containsExactly(bytes);
  }

  @Test
  void shouldReturnNoTripletsBeforeWindowIsFull() {
    // given
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] firstFourBytes = {'A', 'B', 'C', 'D'};

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
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] bytes = {'A', 'B', 'C', 'D', 'E'};
    for (byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // when
    List<byte[]> triplets = slidingWindow.triplets();

    // then
    assertThat(triplets).hasSize(6);
    assertThat(triplets.get(0)).containsExactly(new byte[] {'A', 'B', 'E'});
    assertThat(triplets.get(1)).containsExactly(new byte[] {'A', 'C', 'E'});
    assertThat(triplets.get(2)).containsExactly(new byte[] {'A', 'D', 'E'});
    assertThat(triplets.get(3)).containsExactly(new byte[] {'B', 'C', 'E'});
    assertThat(triplets.get(4)).containsExactly(new byte[] {'B', 'D', 'E'});
    assertThat(triplets.get(5)).containsExactly(new byte[] {'C', 'D', 'E'});

    // when
    slidingWindow.addByte((byte) 'F');

    // then
    triplets = slidingWindow.triplets();
    assertThat(triplets).hasSize(6);
    assertThat(triplets.get(0)).containsExactly(new byte[] {'B', 'C', 'F'});
    assertThat(triplets.get(1)).containsExactly(new byte[] {'B', 'D', 'F'});
    assertThat(triplets.get(2)).containsExactly(new byte[] {'B', 'E', 'F'});
    assertThat(triplets.get(3)).containsExactly(new byte[] {'C', 'D', 'F'});
    assertThat(triplets.get(4)).containsExactly(new byte[] {'C', 'E', 'F'});
    assertThat(triplets.get(5)).containsExactly(new byte[] {'D', 'E', 'F'});
  }

  @Test
  void shouldKeepGeneratingTripletsAfterMultipleWindowShifts() {
    // given
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] bytes = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};

    // when
    for (byte currentByte : bytes) {
      slidingWindow.addByte(currentByte);
    }

    // then
    assertThat(slidingWindow.currentWindow()).containsExactly(new byte[] {'D', 'E', 'F', 'G', 'H'});

    List<byte[]> triplets = slidingWindow.triplets();
    assertThat(triplets).hasSize(6);
    assertThat(triplets.get(0)).containsExactly(new byte[] {'D', 'E', 'H'});
    assertThat(triplets.get(1)).containsExactly(new byte[] {'D', 'F', 'H'});
    assertThat(triplets.get(2)).containsExactly(new byte[] {'D', 'G', 'H'});
    assertThat(triplets.get(3)).containsExactly(new byte[] {'E', 'F', 'H'});
    assertThat(triplets.get(4)).containsExactly(new byte[] {'E', 'G', 'H'});
    assertThat(triplets.get(5)).containsExactly(new byte[] {'F', 'G', 'H'});
  }
}
