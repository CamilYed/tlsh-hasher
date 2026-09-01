package io.github.camilyed.tlsh.cli;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

/** Counts bytes as they are read and reports deltas without buffering the complete input. */
final class CountingInputStream extends FilterInputStream {

  private final LongConsumer byteListener;
  private long bytesRead;

  CountingInputStream(final InputStream input, final LongConsumer byteListener) {
    super(input);
    this.byteListener = byteListener;
  }

  @Override
  public int read() throws IOException {
    final int value = super.read();
    if (value != -1) {
      recordRead(1L);
    }
    return value;
  }

  @Override
  public int read(final byte[] bytes, final int offset, final int length) throws IOException {
    final int count = super.read(bytes, offset, length);
    if (count > 0) {
      recordRead(count);
    }
    return count;
  }

  /** Returns the number of bytes successfully obtained from the wrapped stream. */
  long bytesRead() {
    return bytesRead;
  }

  /** Updates both the local count and aggregate progress using a delta. */
  private void recordRead(final long count) {
    bytesRead += count;
    byteListener.accept(count);
  }
}
