package io.github.camilyed.tlsh.cli;

import java.nio.file.Path;

/** One concrete stream or regular file selected from the user's command-line inputs. */
record HashInput(String displayName, Path path, long expectedBytes) {

  private static final long UNKNOWN_SIZE = -1L;

  /** Creates the special input that reads bytes from process standard input. */
  static HashInput standardInput() {
    return new HashInput("-", null, UNKNOWN_SIZE);
  }

  /** Returns whether this input represents standard input instead of a filesystem path. */
  boolean isStandardInput() {
    return path == null;
  }
}
