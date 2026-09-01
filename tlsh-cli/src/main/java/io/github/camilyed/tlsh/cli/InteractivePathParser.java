package io.github.camilyed.tlsh.cli;

import java.io.File;
import java.nio.file.Path;

/** Normalizes quoted, home-relative, escaped, or occasionally malformed terminal paths. */
final class InteractivePathParser {

  private InteractivePathParser() {}

  /** Converts one human-entered path into the platform filesystem representation. */
  static Path parse(final String answer) {
    String normalized = removeLeadingReplacementCharacters(answer.strip());
    normalized = stripMatchingQuotes(normalized);
    if (File.separatorChar == '/') {
      normalized = removeShellEscapes(normalized);
    }
    if ("~".equals(normalized)) {
      normalized = System.getProperty("user.home");
    } else if (normalized.startsWith("~" + File.separator)) {
      normalized =
          Path.of(System.getProperty("user.home")).resolve(normalized.substring(2)).toString();
    }
    return Path.of(normalized).normalize();
  }

  /** Removes decoder replacement markers observed before the first character in some IDE PTYs. */
  private static String removeLeadingReplacementCharacters(final String value) {
    int firstMeaningfulCharacter = 0;
    while (firstMeaningfulCharacter < value.length()
        && value.charAt(firstMeaningfulCharacter) == '\uFFFD') {
      firstMeaningfulCharacter++;
    }
    return value.substring(firstMeaningfulCharacter);
  }

  /** Removes one matching quote pair commonly produced when a path is copied from another app. */
  private static String stripMatchingQuotes(final String value) {
    if (value.length() < 2) {
      return value;
    }
    final char first = value.charAt(0);
    final char last = value.charAt(value.length() - 1);
    return (first == last && (first == '\'' || first == '"'))
        ? value.substring(1, value.length() - 1)
        : value;
  }

  /** Interprets backslash escaping produced when macOS terminals paste paths containing spaces. */
  private static String removeShellEscapes(final String value) {
    final StringBuilder result = new StringBuilder(value.length());
    boolean escaped = false;
    for (int index = 0; index < value.length(); index++) {
      final char character = value.charAt(index);
      if (escaped) {
        result.append(character);
        escaped = false;
      } else if (character == '\\') {
        escaped = true;
      } else {
        result.append(character);
      }
    }
    if (escaped) {
      result.append('\\');
    }
    return result.toString();
  }
}
