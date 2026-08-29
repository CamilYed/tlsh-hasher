package io.github.camilyed.tlsh;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SlidingWindowsTest {

  /** po A, B, C, D → okno jeszcze niepełne po E → [A, B, C, D, E] po F → [B, C, D, E, F] 3. */
  @Test
  public void checkSlideWindowIsFilled() {
    // given - powinny byc bytes naprawde
    SlidingWindow slidingWindow = new SlidingWindow();
    byte[] firstFragment = {'A', 'B', 'C', 'D'};

    // when
    boolean isFull = slidingWindow.addBytes(firstFragment);

    // then
    Assertions.assertThat(isFull).isFalse();

    // when
    isFull = slidingWindow.addByte((byte) 'E');
    Assertions.assertThat(isFull).isTrue();
    // then
    byte[] currentWindow = slidingWindow.currentWindow();
    Assertions.assertThat(currentWindow).isEqualTo((new byte[] {'A', 'B', 'C', 'D', 'E'}));

    // when
    isFull = slidingWindow.addByte((byte) 'F');
    Assertions.assertThat(isFull).isTrue();
    // then
    currentWindow = slidingWindow.currentWindow();
    Assertions.assertThat(currentWindow).isEqualTo((new byte[] {'B', 'C', 'D', 'E', 'F'}));
  }

  @Test
  void shouldNotExposeInternalWindowState() {
    SlidingWindow slidingWindow = new SlidingWindow();
  }
}
