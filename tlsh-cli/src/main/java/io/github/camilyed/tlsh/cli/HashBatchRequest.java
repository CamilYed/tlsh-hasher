package io.github.camilyed.tlsh.cli;

import java.util.List;

/** Immutable input for the shared file-and-folder hashing use case. */
record HashBatchRequest(
    List<String> inputNames,
    boolean recursive,
    boolean includeHidden,
    ProgressMode progressMode,
    boolean summaryEnabled) {

  /** Protects the request from later modification by a command adapter. */
  HashBatchRequest {
    inputNames = List.copyOf(inputNames);
  }
}
