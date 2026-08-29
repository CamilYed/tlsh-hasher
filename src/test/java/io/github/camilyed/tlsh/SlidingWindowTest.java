package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;

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
}
