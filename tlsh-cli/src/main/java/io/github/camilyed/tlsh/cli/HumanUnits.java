package io.github.camilyed.tlsh.cli;

import java.util.Locale;

/** Formats byte counts and elapsed time compactly for terminal presentation. */
final class HumanUnits {

  private static final double KIBIBYTE = 1_024.0;
  private static final double MEBIBYTE = KIBIBYTE * 1_024.0;
  private static final double GIBIBYTE = MEBIBYTE * 1_024.0;

  private HumanUnits() {}

  /** Formats bytes using binary IEC units so the displayed unit has an exact meaning. */
  static String bytes(final long byteCount) {
    if (byteCount < KIBIBYTE) {
      return byteCount + " B";
    }
    if (byteCount < MEBIBYTE) {
      return decimal(byteCount / KIBIBYTE) + " KiB";
    }
    if (byteCount < GIBIBYTE) {
      return decimal(byteCount / MEBIBYTE) + " MiB";
    }
    return decimal(byteCount / GIBIBYTE) + " GiB";
  }

  /** Formats a duration with enough precision for both quick and long-running commands. */
  static String duration(final long nanoseconds) {
    if (nanoseconds < 1_000_000L) {
      return "<1 ms";
    }
    if (nanoseconds < 1_000_000_000L) {
      return (nanoseconds / 1_000_000L) + " ms";
    }
    final long seconds = nanoseconds / 1_000_000_000L;
    if (seconds < 60L) {
      return seconds + " s";
    }
    return (seconds / 60L) + "m " + (seconds % 60L) + "s";
  }

  /** Uses a fixed locale so output never changes commas and dots with the machine locale. */
  private static String decimal(final double value) {
    return String.format(Locale.ROOT, value < 10.0 ? "%.1f" : "%.0f", value);
  }
}
