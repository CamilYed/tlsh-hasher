package io.github.camilyed.tlsh;

/**
 * Compresses an exact input length into the small length code stored in a similarity digest.
 *
 * <p>An exact byte count may require a {@code long}. For example, a stream may contain {@code
 * 1_000_000} bytes, and supported inputs may be larger than the maximum value of Java's {@code
 * int}. The digest cannot dedicate eight bytes to this exact number because its purpose is to
 * remain compact. Instead, it stores the number of a predefined length range.
 *
 * <p>Each entry in {@code RANGE_UPPER_BOUNDS} is the inclusive upper boundary of one range. The
 * array index is the code for that range:
 *
 * <pre>{@code
 * code 0 -> lengths 1 through 1
 * code 1 -> lengths 2 through 2
 * code 2 -> lengths 3 through 3
 * code 3 -> lengths 4 through 5
 * code 4 -> lengths 6 through 7
 * code 5 -> lengths 8 through 11
 * }</pre>
 *
 * <p>Encoding therefore means finding the first upper boundary that is greater than or equal to the
 * exact length and returning its index. For example:
 *
 * <pre>{@code
 * exact length 50  -> first matching boundary is 57  at index 9  -> code 9
 * exact length 256 -> first matching boundary is 291 at index 13 -> code 13
 * }</pre>
 *
 * <p>Many exact lengths intentionally share one code. The encoder cannot reconstruct the original
 * byte count: code {@code 9}, for instance, tells us that the input was longer than {@code 38}
 * bytes and no longer than {@code 57} bytes. This loss of precision is acceptable because a
 * similarity digest needs only a compact indication of overall size, not an archival record of the
 * exact file length.
 *
 * <p>The ranges become wider in absolute byte counts as inputs grow. A one-byte difference is
 * important for a tiny input but insignificant for a file containing hundreds of megabytes. Using
 * ranges prevents such a small absolute difference from making the length component appear
 * needlessly different for large inputs.
 *
 * <p>Length complements the feature histogram. Two inputs can contain local patterns in similar
 * proportions even when one repeats those patterns many more times and is therefore much larger.
 * Their histograms may look similar, while their length codes preserve a coarse indication of that
 * size difference. The length code alone does not measure similarity and does not make a digest
 * unique.
 *
 * <p>The boundaries are stored explicitly instead of being calculated with floating-point
 * logarithms. Values close to a boundary must always receive the same code; a fixed integer table
 * avoids platform-dependent rounding at those edges. A linear scan checks at most 170 entries and
 * is performed only when the final length value is needed, so its cost is negligible compared with
 * reading and processing the input stream.
 *
 * <p>The returned code is represented by an {@code int} for convenient Java arithmetic and array
 * indexing. Its value is limited to {@code 0..169} and therefore fits into the eight-bit length
 * field used later when the digest is assembled. This encoder accepts exact lengths from {@code 1}
 * through {@code 4_224_281_216} bytes.
 */
final class LengthEncoder {

  private static final long[] RANGE_UPPER_BOUNDS = {
    1,
    2,
    3,
    5,
    7,
    11,
    17,
    25,
    38,
    57,
    86,
    129,
    194,
    291,
    437,
    656,
    854,
    1_110,
    1_443,
    1_876,
    2_439,
    3_171,
    3_475,
    3_823,
    4_205,
    4_626,
    5_088,
    5_597,
    6_157,
    6_772,
    7_450,
    8_195,
    9_014,
    9_916,
    10_907,
    11_998,
    13_198,
    14_518,
    15_970,
    17_567,
    19_323,
    21_256,
    23_382,
    25_720,
    28_292,
    31_121,
    34_233,
    37_656,
    41_422,
    45_564,
    50_121,
    55_133,
    60_646,
    66_711,
    73_382,
    80_721,
    88_793,
    97_672,
    107_439,
    118_183,
    130_002,
    143_002,
    157_302,
    173_032,
    190_335,
    209_369,
    230_306,
    253_337,
    278_670,
    306_538,
    337_191,
    370_911,
    408_002,
    448_802,
    493_682,
    543_050,
    597_356,
    657_091,
    722_800,
    795_081,
    874_589,
    962_048,
    1_058_252,
    1_164_078,
    1_280_486,
    1_408_534,
    1_549_388,
    1_704_327,
    1_874_759,
    2_062_236,
    2_268_459,
    2_495_305,
    2_744_836,
    3_019_320,
    3_321_252,
    3_653_374,
    4_018_711,
    4_420_582,
    4_862_641,
    5_348_905,
    5_883_796,
    6_472_176,
    7_119_394,
    7_831_333,
    8_614_467,
    9_475_909,
    10_423_501,
    11_465_851,
    12_612_437,
    13_873_681,
    15_261_050,
    16_787_154,
    18_465_870,
    20_312_458,
    22_343_706,
    24_578_077,
    27_035_886,
    29_739_474,
    32_713_425,
    35_984_770,
    39_583_245,
    43_541_573,
    47_895_730,
    52_685_306,
    57_953_837,
    63_749_221,
    70_124_148,
    77_136_564,
    84_850_228,
    93_335_252,
    102_668_779,
    112_935_659,
    124_229_227,
    136_652_151,
    150_317_384,
    165_349_128,
    181_884_040,
    200_072_456,
    220_079_703,
    242_087_671,
    266_296_456,
    292_926_096,
    322_218_735,
    354_440_623,
    389_884_688,
    428_873_168,
    471_760_495,
    518_936_559,
    570_830_240,
    627_913_311,
    690_704_607,
    759_775_136,
    835_752_671,
    919_327_967,
    1_011_260_767,
    1_112_386_880,
    1_223_623_232,
    1_345_985_727,
    1_480_584_256,
    1_628_642_751,
    1_791_507_135,
    1_970_657_856,
    2_167_723_648L,
    2_384_496_256L,
    2_622_945_920L,
    2_885_240_448L,
    3_173_764_736L,
    3_491_141_248L,
    3_840_255_616L,
    4_224_281_216L
  };

  /** Largest exact byte count represented by the final predefined length range. */
  static final long MAX_INPUT_LENGTH = RANGE_UPPER_BOUNDS[RANGE_UPPER_BOUNDS.length - 1];

  /** Creates a stateless encoder for exact input lengths. */
  LengthEncoder() {}

  /**
   * Returns the predefined range containing the supplied exact byte count.
   *
   * @param inputLength exact number of bytes in the input stream
   * @return length-range code from {@code 0} through {@code 169}
   * @throws IllegalArgumentException when {@code inputLength} is not positive or exceeds {@code
   *     4_224_281_216}
   */
  int encode(final long inputLength) {
    validateInputLength(inputLength);

    for (int rangeIndex = 0; rangeIndex < RANGE_UPPER_BOUNDS.length - 1; rangeIndex++) {
      if (inputLength <= RANGE_UPPER_BOUNDS[rangeIndex]) {
        return rangeIndex;
      }
    }

    return RANGE_UPPER_BOUNDS.length - 1;
  }

  /** Ensures that the exact length can be represented by the predefined range table. */
  private static void validateInputLength(final long inputLength) {
    if (inputLength <= 0) {
      throw new IllegalArgumentException("Input length must be positive");
    }
    if (inputLength > MAX_INPUT_LENGTH) {
      throw new IllegalArgumentException("Input length must not exceed " + MAX_INPUT_LENGTH);
    }
  }
}
