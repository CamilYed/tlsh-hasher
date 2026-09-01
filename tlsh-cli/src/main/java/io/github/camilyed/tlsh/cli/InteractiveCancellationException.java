package io.github.camilyed.tlsh.cli;

/** Internal control signal that returns a Ctrl-C-interrupted operation to the guided menu. */
final class InteractiveCancellationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
}
