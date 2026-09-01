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
final class SlidingWindow {
  private static final int WINDOW_SIZE = 5;

  private final byte[] windowBytes;
  private int filledByteCount;

  /** Creates an empty window that becomes available after five bytes have been added. */
  SlidingWindow() {
    this.windowBytes = new byte[WINDOW_SIZE];
    this.filledByteCount = 0;
  }

  /**
   * Adds a byte to the window, discarding the oldest byte when the window is already full.
   *
   * <p>For example, adding {@code A}, {@code B}, {@code C}, and {@code D} returns {@code false}
   * after each call because the window is incomplete. Adding {@code E} returns {@code true} and
   * produces {@code [A, B, C, D, E]}. Adding {@code F} also returns {@code true} and shifts the
   * window to {@code [B, C, D, E, F]}.
   *
   * @param nextByte byte to add
   * @return {@code true} when the window contains five bytes; otherwise {@code false}
   */
  boolean addByte(final byte nextByte) {
    if (filledByteCount < WINDOW_SIZE) {
      filledByteCount++;
      windowBytes[filledByteCount - 1] = nextByte;
      return filledByteCount == WINDOW_SIZE;
    }
    System.arraycopy(windowBytes, 1, windowBytes, 0, WINDOW_SIZE - 1);
    windowBytes[filledByteCount - 1] = nextByte;
    return true;
  }

  /**
   * Returns a snapshot of the five-byte window as a defensive copy.
   *
   * <p>For example, after adding {@code A}, {@code B}, {@code C}, {@code D}, and {@code E}, this
   * method returns {@code [A, B, C, D, E]}. After adding {@code F}, it returns {@code [B, C, D, E,
   * F]}. Modifying a returned array does not modify the window.
   *
   * <p>Before the window is full, unused trailing positions contain zeroes.
   *
   * @return copy of the current window contents
   */
  byte[] snapshot() {
    return windowBytes.clone();
  }

  /**
   * Reads one position without creating a copy of the complete window.
   *
   * <p>This package-private operation exists for the allocation-sensitive hashing path. Internal
   * algorithm stages know the fixed five-byte layout and only read the returned value; callers
   * outside this package receive snapshots instead. Reading a {@code byte} cannot expose the
   * backing array or allow its contents to be changed.
   *
   * @param index window position from {@code 0}, the oldest byte, through {@code 4}, the newest
   *     byte
   * @return byte currently stored at the requested position
   * @throws IndexOutOfBoundsException when {@code index} is outside the five-byte window
   */
  byte byteAt(final int index) {
    return windowBytes[index];
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
  List<byte[]> triplets() {
    return createTriplets();
  }

  private List<byte[]> createTriplets() {
    final List<byte[]> triplets = new ArrayList<>(6);
    if (filledByteCount == WINDOW_SIZE) {
      for (int firstOlderByteIndex = 0; firstOlderByteIndex <= 2; firstOlderByteIndex++) {
        for (int secondOlderByteIndex = firstOlderByteIndex + 1;
            secondOlderByteIndex <= 3;
            secondOlderByteIndex++) {
          final byte[] triplet = new byte[3];
          triplet[0] = windowBytes[firstOlderByteIndex];
          triplet[1] = windowBytes[secondOlderByteIndex];
          triplet[2] = windowBytes[WINDOW_SIZE - 1];
          triplets.add(triplet);
        }
      }
    }
    return triplets;
  }
}
