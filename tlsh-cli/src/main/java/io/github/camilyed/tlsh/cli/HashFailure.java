package io.github.camilyed.tlsh.cli;

/** One input that could not produce a digest together with its concise user-facing reason. */
record HashFailure(String inputName, String detail) {}
