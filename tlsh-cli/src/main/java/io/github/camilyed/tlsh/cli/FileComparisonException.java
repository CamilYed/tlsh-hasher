package io.github.camilyed.tlsh.cli;

/** Expected failure to inspect or hash one named side of a file comparison. */
final class FileComparisonException extends Exception {

  private static final long serialVersionUID = 1L;

  private final String inputName;

  FileComparisonException(final String inputName, final String detail, final Exception cause) {
    super(detail, cause);
    this.inputName = inputName;
  }

  /** Returns the file whose validation or digest calculation failed. */
  String inputName() {
    return inputName;
  }
}
