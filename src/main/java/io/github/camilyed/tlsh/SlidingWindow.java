package io.github.camilyed.tlsh;

/** Maintains the five most recently added bytes for TLSH feature extraction. */
class SlidingWindow {
  private static final int WINDOW_SIZE = 5;

  private final byte[] currentWindow;
  private int currentWindowFillSize;

  /** Creates an empty window that becomes available after five bytes have been added. */
  SlidingWindow() {
    this.currentWindow = new byte[WINDOW_SIZE];
    this.currentWindowFillSize = 0;
  }

  /**
   * Adds a byte to the window, discarding the oldest byte when the window is already full.
   *
   * @param singleByte byte to add
   * @return {@code true} when the window contains five bytes; otherwise {@code false}
   */
  public boolean addByte(byte singleByte) {
    if (currentWindowFillSize < WINDOW_SIZE) {
      currentWindowFillSize++;
      currentWindow[currentWindowFillSize - 1] = singleByte;
      return currentWindowFillSize == WINDOW_SIZE;
    }
    System.arraycopy(currentWindow, 1, currentWindow, 0, WINDOW_SIZE - 1);
    currentWindow[currentWindowFillSize - 1] = singleByte;
    return true;
  }

  /**
   * Returns a defensive copy of the five-byte window.
   *
   * <p>Before the window is full, unused trailing positions contain zeroes.
   *
   * @return copy of the current window contents
   */
  public byte[] currentWindow() {
    return currentWindow.clone();
  }
}
