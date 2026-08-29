package io.github.camilyed.tlsh;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains the five most recently added bytes for TLSH feature extraction.
 *
 * <p>For example, after adding {@code A}, {@code B}, {@code C}, {@code D}, and {@code E}, the
 * window contains {@code [A, B, C, D, E]}. Adding {@code F} discards {@code A} and changes the
 * window to {@code [B, C, D, E, F]}.
 */
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
   * <p>For example, adding {@code A}, {@code B}, {@code C}, and {@code D} returns {@code false}
   * after each call because the window is incomplete. Adding {@code E} returns {@code true} and
   * produces {@code [A, B, C, D, E]}. Adding {@code F} also returns {@code true} and shifts the
   * window to {@code [B, C, D, E, F]}.
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
   * <p>For example, after adding {@code A}, {@code B}, {@code C}, {@code D}, and {@code E}, this
   * method returns {@code [A, B, C, D, E]}. After adding {@code F}, it returns {@code [B, C, D, E,
   * F]}. Modifying a returned array does not modify the window.
   *
   * <p>Before the window is full, unused trailing positions contain zeroes.
   *
   * @return copy of the current window contents
   */
  public byte[] currentWindow() {
    return currentWindow.clone();
  }

  /**
   * Returns the six triplets anchored at the newest byte of a full window.
   *
   * <p>Each triplet combines the newest byte with one of the six possible pairs selected from the
   * four preceding bytes. The triplets are returned in pair order. For example:
   *
   * <pre>{@code
   * window: [A, B, C, D, E]
   *
   * triplets:
   * [A, B, E]
   * [A, C, E]
   * [A, D, E]
   * [B, C, E]
   * [B, D, E]
   * [C, D, E]
   * }</pre>
   *
   * <p>Returns an empty list until the window contains five bytes. Each invocation returns an
   * independent result.
   *
   * @return current window triplets, or an empty list when the window is incomplete
   */
  public List<byte[]> triplets() {
    return generateTriplets();
  }

  private List<byte[]> generateTriplets() {
    List<byte[]> triplets = new ArrayList<>(6);
    if (currentWindowFillSize == WINDOW_SIZE) {
      for (int i = 0; i <= 2; i++) {
        for (int j = i + 1; j <= 3; j++) {
          byte[] triplet = new byte[3];
          triplet[0] = currentWindow[i];
          triplet[1] = currentWindow[j];
          triplet[2] = currentWindow[WINDOW_SIZE - 1];
          triplets.add(triplet);
        }
      }
    }
    return triplets;
  }
}
