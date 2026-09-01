package io.github.camilyed.tlsh.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;

abstract class CliTestSupport {

  protected static final String USER_INTERRUPT = "<CTRL-C>";

  protected static final String FIRST_DIGEST =
      "T10DD02B90854AAA04F465B9B15D0B64FF6F34600FA39C06A138C13534752B9A6517C570";
  protected static final String SECOND_DIGEST =
      "T1645302DC621C945B92FD3244647EBF17E3FA0877E4D40DA2C4CA5B5B90139E2DDA818C";

  private ByteArrayOutputStream outputBytes;
  private ByteArrayOutputStream errorBytes;

  @BeforeEach
  final void createOutputStreams() {
    outputBytes = new ByteArrayOutputStream();
    errorBytes = new ByteArrayOutputStream();
  }

  protected final TlshCli cli(final byte[] input) {
    return new TlshCli(
        new ByteArrayInputStream(input),
        new PrintWriter(outputBytes, true, StandardCharsets.UTF_8),
        new PrintWriter(errorBytes, true, StandardCharsets.UTF_8));
  }

  protected final TlshCli interactiveCli(final byte[] input, final CliTerminal terminal) {
    return new TlshCli(
        new ByteArrayInputStream(input),
        new PrintWriter(outputBytes, true, StandardCharsets.UTF_8),
        new PrintWriter(errorBytes, true, StandardCharsets.UTF_8),
        terminal);
  }

  protected final String output() {
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  protected final String normalizedOutput() {
    return output().replaceAll("\\s+", " ");
  }

  protected final String error() {
    return errorBytes.toString(StandardCharsets.UTF_8);
  }

  protected static byte[] deterministicInput() {
    return deterministicInput(0x5EEDL);
  }

  protected static byte[] deterministicInput(final long seed) {
    final byte[] input = new byte[4_096];
    new Random(seed).nextBytes(input);
    return input;
  }

  /** Supplies deterministic answers without claiming that redirected byte input is a console. */
  protected static final class ScriptedTerminal implements CliTerminal {

    private final Queue<String> answers;
    private final List<String> prompts = new ArrayList<>();

    protected ScriptedTerminal(final String... answers) {
      this.answers = new ArrayDeque<>(List.of(answers));
    }

    @Override
    public boolean interactive() {
      return true;
    }

    @Override
    public CommandLine.Help.Ansi ansi() {
      return CommandLine.Help.Ansi.OFF;
    }

    @Override
    public String readLine(final String prompt) {
      prompts.add(prompt);
      final String answer = answers.remove();
      if (USER_INTERRUPT.equals(answer)) {
        throw new InteractiveCancellationException();
      }
      return answer;
    }

    protected List<String> prompts() {
      return List.copyOf(prompts);
    }
  }
}
