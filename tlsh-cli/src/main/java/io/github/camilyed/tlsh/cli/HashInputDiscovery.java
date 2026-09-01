package io.github.camilyed.tlsh.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Expands user-supplied files and directories into a deterministic list of regular files. */
final class HashInputDiscovery {

  private static final int CURRENT_DIRECTORY_DEPTH = 1;

  /**
   * Holds usable inputs together with failures that should be reported without stopping the run.
   */
  record Result(List<HashInput> inputs, List<Failure> failures, boolean containedDirectory) {}

  /** Describes one path that could not be inspected. */
  record Failure(String inputName, String detail) {}

  /**
   * Resolves every command-line name while removing duplicates and sorting directory contents.
   *
   * <p>Directory traversal deliberately does not follow symbolic links. A symbolic link that was
   * supplied directly can still be hashed when it resolves to a regular file. Avoiding linked
   * directories prevents accidental cycles and keeps the meaning of {@code --recursive}
   * predictable.
   */
  Result discover(final List<String> inputNames, final boolean recursive) {
    final Map<Path, HashInput> uniqueInputs = new LinkedHashMap<>();
    final List<Failure> failures = new ArrayList<>();
    boolean containedDirectory = false;

    for (final String inputName : inputNames) {
      if ("-".equals(inputName)) {
        uniqueInputs.putIfAbsent(Path.of("-"), HashInput.standardInput());
        continue;
      }

      final Path path = Path.of(inputName);
      try {
        if (Files.isDirectory(path)) {
          containedDirectory = true;
          discoverDirectory(path, recursive, uniqueInputs);
        } else if (Files.isRegularFile(path)) {
          addRegularFile(path, uniqueInputs);
        } else if (!Files.exists(path)) {
          failures.add(new Failure(inputName, "path does not exist"));
        } else {
          failures.add(new Failure(inputName, "path is not a regular file or directory"));
        }
      } catch (final IOException | UncheckedIOException | SecurityException exception) {
        failures.add(new Failure(inputName, TlshCli.message(asException(exception))));
      }
    }

    return new Result(
        List.copyOf(uniqueInputs.values()), List.copyOf(failures), containedDirectory);
  }

  /** Adds files from one directory in lexical order for reproducible output. */
  private static void discoverDirectory(
      final Path directory, final boolean recursive, final Map<Path, HashInput> uniqueInputs)
      throws IOException {
    final int maximumDepth = recursive ? Integer.MAX_VALUE : CURRENT_DIRECTORY_DEPTH;
    final List<Path> regularFiles;
    try (Stream<Path> paths = Files.walk(directory, maximumDepth)) {
      regularFiles =
          paths.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList();
    }

    if (regularFiles.isEmpty()) {
      final String hint = recursive ? "" : "; use --recursive to include subdirectories";
      throw new IOException("directory contains no regular files at this depth" + hint);
    }
    for (final Path path : regularFiles) {
      addRegularFile(path, uniqueInputs);
    }
  }

  /** Reads the expected size now so aggregate byte progress has a stable denominator. */
  private static void addRegularFile(final Path path, final Map<Path, HashInput> uniqueInputs)
      throws IOException {
    final Path identity = path.toAbsolutePath().normalize();
    uniqueInputs.putIfAbsent(identity, new HashInput(path.toString(), path, Files.size(path)));
  }

  /** Normalizes checked and unchecked I/O failures for the common diagnostic formatter. */
  private static Exception asException(final Exception exception) {
    return exception instanceof UncheckedIOException unchecked ? unchecked.getCause() : exception;
  }
}
