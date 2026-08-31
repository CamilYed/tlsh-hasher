package io.github.camilyed.tlsh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Convenient entry point for one-shot and incremental TLSH calculation. */
public final class Tlsh {

  private static final int STREAM_BUFFER_SIZE = 8_192;

  private Tlsh() {}

  /**
   * Calculates a digest for one complete byte array.
   *
   * @param input complete input bytes
   * @return immutable digest
   * @throws NullPointerException when {@code input} is {@code null}
   * @throws IllegalStateException when the input does not satisfy the standard length and feature
   *     diversity requirements
   */
  public static TlshDigest hash(final byte[] input) {
    Objects.requireNonNull(input, "input");
    final TlshHasher hasher = newHasher();
    hasher.update(input);
    return hasher.finish();
  }

  /**
   * Calculates a digest while reading bytes incrementally from a stream.
   *
   * <p>The complete input is never retained in memory. This method reads at most one 8 KiB chunk at
   * a time and feeds it into the same streaming state used by {@link TlshHasher}. The caller owns
   * the supplied stream: this method reads it to the end but deliberately does not close it.
   *
   * @param input stream containing all bytes to hash
   * @return immutable digest
   * @throws NullPointerException when {@code input} is {@code null}
   * @throws IOException when bytes cannot be read from the stream
   * @throws IllegalStateException when the input does not satisfy the standard length and feature
   *     diversity requirements
   */
  public static TlshDigest hash(final InputStream input) throws IOException {
    Objects.requireNonNull(input, "input");
    final TlshHasher hasher = newHasher();
    final byte[] buffer = new byte[STREAM_BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = input.read(buffer)) != -1) {
      hasher.update(buffer, 0, bytesRead);
    }
    return hasher.finish();
  }

  /**
   * Calculates a digest for the contents of one filesystem path.
   *
   * <p>This is the convenient file-oriented counterpart of {@link #hash(InputStream)}. It opens the
   * file as a stream, processes it incrementally, and closes that internally created stream before
   * returning. The complete file is not loaded into memory.
   *
   * @param path path of the file to hash
   * @return immutable digest
   * @throws NullPointerException when {@code path} is {@code null}
   * @throws IOException when the file cannot be opened, read, or closed
   * @throws IllegalStateException when the input does not satisfy the standard length and feature
   *     diversity requirements
   */
  public static TlshDigest hash(final Path path) throws IOException {
    Objects.requireNonNull(path, "path");
    try (InputStream input = Files.newInputStream(path)) {
      return hash(input);
    }
  }

  /**
   * Creates an empty streaming hasher using the standard 128-bucket, one-byte-checksum format.
   *
   * @return new independent hasher
   */
  public static TlshHasher newHasher() {
    final PearsonHash pearsonHash = new PearsonHash();
    final TlshDigestAssembler digestAssembler =
        new TlshDigestAssembler(
            new LengthEncoder(),
            new HistogramQuartileCalculator(),
            new HistogramQuantizer(),
            new HistogramCodePacker(),
            new QuartileRatioEncoder());
    final TlshAccumulator accumulator =
        new TlshAccumulator(
            new BucketMapper(pearsonHash),
            new Histogram(),
            new ChecksumAccumulator(pearsonHash),
            digestAssembler,
            new TlshDigestEligibilityChecker());
    return new TlshHasher(accumulator);
  }
}
