package io.github.camilyed.tlsh;

/**
 * Transforms accumulated stream statistics into an immutable {@link TlshDigest}.
 *
 * <p>Byte accumulation and digest finalization are separate phases. While bytes arrive, {@link
 * TlshAccumulator} maintains only the exact input length, rolling checksum, and feature histogram.
 * Those values are useful for efficient streaming but are not yet the compact digest. This class
 * coordinates the stateless transformations that produce the four final components.
 *
 * <p>Assembly follows this data flow:
 *
 * <pre>{@code
 * exact input length -----------------------------> encoded length range
 * rolling checksum -------------------------------> unchanged checksum
 *
 * 128 effective histogram counts
 *          |
 *          +--> quartile calculation -------------> Q1, Q2, Q3
 *                         |                              |
 *                         |                              +--> packed ratio byte
 *                         v
 *               count quantization
 *                         |
 *                         v
 *                128 levels in 0..3
 *                         |
 *                         v
 *                32-byte histogram code
 * }</pre>
 *
 * <p>Quartile calculation sorts a defensive copy, while quantization continues to use the original
 * bucket order. Preserving that order matters because every position still represents a particular
 * Pearson bucket. The supplied histogram array is not modified during assembly.
 *
 * <p>The packed quartile ratios are returned by {@link QuartileRatioEncoder} as a signed Java
 * {@code byte}. They are converted back to an unsigned {@code int} in {@code 0..255} before the
 * digest is created. For example, the bit pattern {@code 0xEC} appears as {@code -20} in a Java
 * {@code byte}, but the digest stores its logical value {@code 236}.
 *
 * <p>This assembler does not decide whether enough input data or enough distinct histogram buckets
 * were observed to create a meaningful similarity digest. That whole-stream eligibility rule is a
 * separate stage. Component-level validation is delegated to the collaborating encoders and to
 * {@link TlshDigest}.
 *
 * <p>The assembler itself stores no per-input mutable state. The injected collaborators make every
 * transformation explicit and keep this class focused on ordering the finalization pipeline.
 */
final class TlshDigestAssembler {

  private final LengthEncoder lengthEncoder;
  private final HistogramQuartileCalculator quartileCalculator;
  private final HistogramQuantizer histogramQuantizer;
  private final HistogramCodePacker histogramCodePacker;
  private final QuartileRatioEncoder quartileRatioEncoder;

  /**
   * Creates an assembler from the stateless transformations used during finalization.
   *
   * @param lengthEncoder converts an exact byte count to its compact range code
   * @param quartileCalculator derives three thresholds from the effective histogram
   * @param histogramQuantizer replaces each bucket count with a level from {@code 0} through {@code
   *     3}
   * @param histogramCodePacker packs four quantized levels into each output byte
   * @param quartileRatioEncoder packs the two relative quartile proportions into one byte
   */
  TlshDigestAssembler(
      final LengthEncoder lengthEncoder,
      final HistogramQuartileCalculator quartileCalculator,
      final HistogramQuantizer histogramQuantizer,
      final HistogramCodePacker histogramCodePacker,
      final QuartileRatioEncoder quartileRatioEncoder) {
    this.lengthEncoder = lengthEncoder;
    this.quartileCalculator = quartileCalculator;
    this.histogramQuantizer = histogramQuantizer;
    this.histogramCodePacker = histogramCodePacker;
    this.quartileRatioEncoder = quartileRatioEncoder;
  }

  /**
   * Calculates every compact component and returns them as one structured digest.
   *
   * @param inputLength exact number of bytes accumulated for the input
   * @param checksum rolling checksum in the unsigned-byte range {@code 0..255}
   * @param effectiveBucketCounts counts for effective histogram buckets {@code 0..127}, in their
   *     original bucket order
   * @return immutable digest containing the checksum, encoded length, ratios, and packed histogram
   * @throws IllegalArgumentException when any supplied value cannot be represented by the component
   *     encoders or by the resulting digest
   */
  TlshDigest assemble(
      final long inputLength, final int checksum, final int[] effectiveBucketCounts) {
    final int lengthCode = lengthEncoder.encode(inputLength);

    final HistogramQuartiles quartiles = quartileCalculator.calculate(effectiveBucketCounts);

    final int[] quantizedBucketValues =
        histogramQuantizer.quantize(effectiveBucketCounts, quartiles);

    final byte[] histogramCode = histogramCodePacker.pack(quantizedBucketValues);

    final int quartileRatios = Byte.toUnsignedInt(quartileRatioEncoder.encode(quartiles));

    return new TlshDigest(checksum, lengthCode, quartileRatios, histogramCode);
  }
}
