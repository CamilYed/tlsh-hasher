/** Provides the executable TLSH command-line application. */
module io.github.camilyed.tlsh.cli {
  requires info.picocli;
  requires io.github.camilyed.tlsh;

  opens io.github.camilyed.tlsh.cli to
      info.picocli;
}
