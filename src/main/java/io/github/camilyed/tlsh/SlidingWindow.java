package io.github.camilyed.tlsh;

class SlidingWindow {
  private static final int WINDOW_SIZE = 5;

  private final byte[] currentWindow;
  private int currentWindowFillSize;

  SlidingWindow() {
    this.currentWindow = new byte[WINDOW_SIZE];
    this.currentWindowFillSize = 0;
  }

  public boolean addBytes(byte[] bytes) {
    for (int i = 0; i < Math.min(bytes.length, WINDOW_SIZE); i++) {
      currentWindow[i] = bytes[i];
      currentWindowFillSize = i + 1;
    }
    return currentWindowFillSize == WINDOW_SIZE;
  }

  public boolean addByte(byte singleByte) {
    if (currentWindowFillSize < WINDOW_SIZE) {
      currentWindowFillSize++;
      currentWindow[currentWindowFillSize - 1] = singleByte;
      return currentWindowFillSize == WINDOW_SIZE;
    }
    // move window
    /** [A B C D E] └───────┐ ▼ [B C D E E] */
    System.arraycopy(currentWindow, 1, currentWindow, 0, WINDOW_SIZE - 1);
    currentWindow[currentWindowFillSize - 1] = singleByte;
    return true;
  }

  public byte[] currentWindow() {
    return currentWindow;
  }
}
