package io.github.camilyed.tlsh.cli;

/** Controls whether a live progress line is displayed while bytes are being hashed. */
enum ProgressMode {
  /** Displays progress only when a human terminal is attached. */
  AUTO,

  /** Displays progress even in an IDE output window or redirected stream. */
  ALWAYS,

  /** Never displays progress. */
  NEVER
}
