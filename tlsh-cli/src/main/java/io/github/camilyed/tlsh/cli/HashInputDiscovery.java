package io.github.camilyed.tlsh.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Expands user-supplied files and directories into a deterministic list of regular files. */
final class HashInputDiscovery {

  private static final int CURRENT_DIRECTORY_DEPTH = 1;

  /**
   * Holds usable inputs together with failures that should be reported without stopping the run.
   */
  record Result(
      List<HashInput> inputs,
      List<Failure> failures,
      boolean containedDirectory,
      int skippedHiddenEntries) {}

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
  Result discover(
      final List<String> inputNames, final boolean recursive, final boolean includeHidden) {
    final Map<Path, HashInput> uniqueInputs = new LinkedHashMap<>();
    final List<Failure> failures = new ArrayList<>();
    boolean containedDirectory = false;
    int skippedHiddenEntries = 0;

    for (final String inputName : inputNames) {
      if ("-".equals(inputName)) {
        uniqueInputs.putIfAbsent(Path.of("-"), HashInput.standardInput());
        continue;
      }

      final Path path = Path.of(inputName);
      try {
        if (Files.isDirectory(path)) {
          containedDirectory = true;
          skippedHiddenEntries += discoverDirectory(path, recursive, includeHidden, uniqueInputs);
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
        List.copyOf(uniqueInputs.values()),
        List.copyOf(failures),
        containedDirectory,
        skippedHiddenEntries);
  }

  /** Adds visible files from one directory in lexical order for reproducible output. */
  private static int discoverDirectory(
      final Path directory,
      final boolean recursive,
      final boolean includeHidden,
      final Map<Path, HashInput> uniqueInputs)
      throws IOException {
    final int maximumDepth = recursive ? Integer.MAX_VALUE : CURRENT_DIRECTORY_DEPTH;
    final DirectoryCollector collector = new DirectoryCollector(directory, includeHidden);
    Files.walkFileTree(directory, java.util.Set.of(), maximumDepth, collector);
    final List<Path> regularFiles = collector.regularFiles();

    if (regularFiles.isEmpty() && collector.skippedHiddenEntries() == 0) {
      final String hint = recursive ? "" : "; use --recursive to include subdirectories";
      throw new IOException("directory contains no regular files at this depth" + hint);
    }
    for (final Path path : regularFiles) {
      addRegularFile(path, uniqueInputs);
    }
    return collector.skippedHiddenEntries();
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

  /** Collects regular files while pruning hidden subtrees before they are traversed. */
  private static final class DirectoryCollector extends SimpleFileVisitor<Path> {

    private final Path root;
    private final boolean includeHidden;
    private final List<Path> regularFiles = new ArrayList<>();
    private int skippedHiddenEntries;

    private DirectoryCollector(final Path root, final boolean includeHidden) {
      this.root = root;
      this.includeHidden = includeHidden;
    }

    @Override
    public FileVisitResult preVisitDirectory(
        final Path directory, final BasicFileAttributes attributes) throws IOException {
      if (!root.equals(directory) && hidden(directory)) {
        skippedHiddenEntries++;
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes)
        throws IOException {
      if (Files.isRegularFile(file)) {
        if (hidden(file)) {
          skippedHiddenEntries++;
        } else {
          regularFiles.add(file);
        }
      }
      return FileVisitResult.CONTINUE;
    }

    private boolean hidden(final Path path) throws IOException {
      return !includeHidden && Files.isHidden(path);
    }

    private List<Path> regularFiles() {
      regularFiles.sort(Comparator.comparing(Path::toString));
      return regularFiles;
    }

    private int skippedHiddenEntries() {
      return skippedHiddenEntries;
    }
  }
}
