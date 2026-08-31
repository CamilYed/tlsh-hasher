package io.github.camilyed.tlsh;

import java.util.Objects;

/**
 * Incrementally calculates one TLSH digest from a sequence of byte chunks.
 *
 * <p>This is the streaming public API. It accepts individual bytes, complete arrays, or selected
 * array ranges without retaining the complete input. All update methods feed the same underlying
 * state, so splitting identical bytes into different chunk sizes produces the same digest.
 *
 * <p>An instance represents one logical input stream and is mutable. It must not be updated
 * concurrently from multiple threads. Calling {@link #finish()} creates an immutable snapshot but
 * does not reset or close the hasher; subsequent updates continue the same stream.
 */
public final class TlshHasher {

  private final TlshAccumulator accumulator;

  /**
   * Creates a public streaming wrapper around one internal accumulator.
   *
   * @param accumulator mutable state used to process the incoming bytes
   */
  TlshHasher(final TlshAccumulator accumulator) {
    this.accumulator = accumulator;
  }

  /**
   * Adds one byte to the current input stream.
   *
   * @param value next byte
   */
  public void update(final byte value) {
    accumulator.addByte(value);
  }

  /**
   * Adds every byte from an array to the current input stream.
   *
   * @param input bytes to add
   * @throws NullPointerException when {@code input} is {@code null}
   */
  public void update(final byte[] input) {
    Objects.requireNonNull(input, "input");
    update(input, 0, input.length);
  }

  /**
   * Adds a selected contiguous range from an array.
   *
   * @param input array containing the bytes
   * @param offset index of the first byte to add
   * @param length number of bytes to add
   * @throws NullPointerException when {@code input} is {@code null}
   * @throws IndexOutOfBoundsException when the selected range lies outside the array
   */
  public void update(final byte[] input, final int offset, final int length) {
    Objects.requireNonNull(input, "input");
    Objects.checkFromIndexSize(offset, length, input.length);
    final int endIndex = offset + length;
    for (int inputIndex = offset; inputIndex < endIndex; inputIndex++) {
      accumulator.addByte(input[inputIndex]);
    }
  }

  /**
   * Creates a digest from the bytes accumulated so far without resetting this hasher.
   *
   * @return immutable TLSH digest
   * @throws IllegalStateException when the input does not satisfy the standard length and feature
   *     diversity requirements
   */
  public TlshDigest finish() {
    return accumulator.finish();
  }
}
