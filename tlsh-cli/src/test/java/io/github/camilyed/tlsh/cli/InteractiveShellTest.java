package io.github.camilyed.tlsh.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.tlsh.Tlsh;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InteractiveShellTest extends CliTestSupport {

  @Test
  void shouldHashOneInteractiveFileWithoutAskingAboutFolders(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path inputPath = Files.write(directory.resolve("interactive file.bin"), input);
    final ScriptedTerminal terminal =
        new ScriptedTerminal("1", '"' + inputPath.toString() + '"', "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains("TLSH · find similarity", "Hash one file", "Hash a folder", "Bye.")
        .contains(Tlsh.hash(input).encoded() + "  " + inputPath)
        .doesNotContain("Choose scope", "nested folder");
    assertThat(terminal.prompts())
        .containsExactly("Choose an action [1]: ", "File path: ", "Choose an action [1]: ");
  }

  @Test
  void shouldPreviewAndRecursivelyHashInteractiveFolder(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path direct = Files.write(directory.resolve("direct.bin"), input);
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path nested = Files.write(nestedDirectory.resolve("nested.bin"), input);
    final ScriptedTerminal terminal = new ScriptedTerminal("2", directory.toString(), "2", "", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains(
            "Hash a folder",
            "This folder and every nested folder",
            "Found 2 files · 8.0 KiB",
            direct.toString(),
            nested.toString(),
            "Bye.");
    assertThat(error()).contains("✓ 2 files hashed");
    assertThat(terminal.prompts())
        .containsExactly(
            "Choose an action [1]: ",
            "Folder path: ",
            "Choose scope [1]: ",
            "Start hashing? [Y/n]: ",
            "Choose an action [1]: ");
  }

  @Test
  void shouldCancelInteractiveHashInsteadOfAcceptingDefaultAfterCtrlC(@TempDir final Path directory)
      throws IOException {
    final Path inputPath = Files.write(directory.resolve("input.bin"), deterministicInput());
    final ScriptedTerminal terminal =
        new ScriptedTerminal("2", directory.toString(), "1", USER_INTERRUPT, "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains("Found 1 file", "Cancelled.", "Bye.")
        .doesNotContain(inputPath.toString(), Tlsh.hash(deterministicInput()).encoded());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldRejectAFileSelectedForTheFolderWorkflow(@TempDir final Path directory)
      throws IOException {
    final Path inputPath = Files.write(directory.resolve("input.bin"), deterministicInput());
    final ScriptedTerminal terminal = new ScriptedTerminal("2", inputPath.toString(), "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains("That path is a file. Choose 'Hash one file' from the menu instead.", "Bye.")
        .doesNotContain(Tlsh.hash(deterministicInput()).encoded());
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldExplainASelectionContainingOnlyHiddenFiles(@TempDir final Path directory)
      throws IOException {
    Files.write(directory.resolve(".hidden.bin"), deterministicInput());
    final ScriptedTerminal terminal = new ScriptedTerminal("2", directory.toString(), "1", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output()).contains("No visible files found · 1 hidden entry skipped.", "Bye.");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldRejectAnUnknownFolderScope(@TempDir final Path directory) throws IOException {
    Files.write(directory.resolve("input.bin"), deterministicInput());
    final ScriptedTerminal terminal =
        new ScriptedTerminal("2", directory.toString(), "unexpected", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output()).contains("Unknown scope. Choose 1 or 2.", "Bye.");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldCompareTwoFilesInteractively(@TempDir final Path directory) throws IOException {
    final byte[] firstInput = deterministicInput();
    final byte[] secondInput = deterministicInput(0xC0FFEE);
    final Path firstPath = Files.write(directory.resolve("first.bin"), firstInput);
    final Path secondPath = Files.write(directory.resolve("second.bin"), secondInput);
    final int expectedDistance = Tlsh.hash(firstInput).distanceTo(Tlsh.hash(secondInput));
    final ScriptedTerminal terminal =
        new ScriptedTerminal("3", firstPath.toString(), secondPath.toString(), "", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains(
            "Compare two files",
            "Comparison",
            "Distance  " + expectedDistance,
            "Length    included",
            firstPath.toString(),
            secondPath.toString(),
            Tlsh.hash(firstInput).encoded(),
            Tlsh.hash(secondInput).encoded(),
            "Smaller distances indicate greater similarity",
            "Bye.");
    assertThat(terminal.prompts())
        .containsExactly(
            "Choose an action [1]: ",
            "First file: ",
            "Second file: ",
            "Ignore input-length difference? [y/N]: ",
            "Choose an action [1]: ");
  }

  @Test
  void shouldPreviewAndFindSimilarFilesInteractively(@TempDir final Path directory)
      throws IOException {
    final byte[] input = deterministicInput();
    final Path first = Files.write(directory.resolve("a.bin"), input);
    final Path second = Files.write(directory.resolve("b.bin"), input);
    Files.write(directory.resolve("different.bin"), deterministicInput(0xC0FFEE));
    final ScriptedTerminal terminal =
        new ScriptedTerminal("4", directory.toString(), "1", "", "", "", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains(
            "Find similar files",
            "Found 3 files · 3 comparisons · maximum distance 0",
            "Distance 0 means the same TLSH digest, not proof of identical bytes.",
            "0  " + first + "  " + second,
            "Bye.");
    assertThat(error()).contains("✓ 3 files hashed", "3 comparisons", "1 match");
    assertThat(terminal.prompts())
        .containsExactly(
            "Choose an action [1]: ",
            "Folder path: ",
            "Choose scope [1]: ",
            "Maximum TLSH distance [0]: ",
            "Ignore input-length difference? [y/N]: ",
            "Start similarity scan? [Y/n]: ",
            "Choose an action [1]: ");
  }

  @Test
  void shouldRejectAnInvalidInteractiveSimilarityDistance(@TempDir final Path directory)
      throws IOException {
    Files.write(directory.resolve("first.bin"), deterministicInput());
    Files.write(directory.resolve("second.bin"), deterministicInput(0xC0FFEE));
    final ScriptedTerminal terminal =
        new ScriptedTerminal("4", directory.toString(), "1", "not-a-number", "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output())
        .contains("Distance must be a whole number equal to or greater than zero.", "Bye.")
        .doesNotContain("Start similarity scan?");
    assertThat(error()).isEmpty();
  }

  @Test
  void shouldExitInteractiveShellSuccessfullyAfterAnEarlierHashFailure(
      @TempDir final Path directory) throws IOException {
    final Path ineligiblePath = Files.write(directory.resolve("too-small.bin"), new byte[32]);
    final ScriptedTerminal terminal = new ScriptedTerminal("1", ineligiblePath.toString(), "5");

    final int exitCode = interactiveCli(new byte[0], terminal).execute();

    assertThat(exitCode).isZero();
    assertThat(output()).contains("Bye.");
    assertThat(error())
        .contains(
            "Failed files",
            "✗ " + ineligiblePath.toAbsolutePath(),
            "input is 32 B; TLSH requires at least 256 B");
  }

  @Test
  void shouldNormalizeHomePathAfterAnIdeReplacementCharacter() {
    final Path path = InteractivePathParser.parse("\uFFFD~");

    assertThat(path).isEqualTo(Path.of(System.getProperty("user.home")));
  }
}
