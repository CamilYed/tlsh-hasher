# Command-line guide

The `tlsh-cli` module provides two interfaces over the same application use cases:

- a guided terminal for people who start `tlsh` without arguments; and
- explicit commands with stable output for scripts and automation.

TLSH is a similarity hash, not a cryptographic hash. A comparison returns a nonnegative distance:
smaller values indicate greater similarity. The distance is not a percentage or probability, and
even distance zero does not prove that the original files contain identical bytes.

## Build and start the application

Build the local JVM distribution:

```shell
./gradlew :tlsh-cli:installDist
```

Start the installed launcher on macOS, Linux, or another Unix-like system:

```shell
./tlsh-cli/build/install/tlsh/bin/tlsh
```

On Windows, run `tlsh-cli\build\install\tlsh\bin\tlsh.bat`.

The installed launcher should be used for the guided terminal. Gradle does not always attach a real
console to its `run` task, so `./gradlew :tlsh-cli:run` may print help instead of waiting for
questions. Explicit commands can still be run through Gradle, for example:

```shell
./gradlew :tlsh-cli:run --args="compare first.bin second.bin"
```

## Guided terminal

Starting the installed launcher without arguments opens this menu:

```text
1  Hash one file
2  Hash a folder
3  Compare two files
4  Exit
```

The menu remains open after each operation. Numeric choices are always accepted; the textual aliases
shown below are conveniences. An empty menu answer selects `1`.

| Choice | Meaning | Accepted aliases |
| --- | --- | --- |
| `1` | Calculate and display one file's canonical `T1` digest. | `file`, `f`, or an empty answer |
| `2` | Preview and hash the selected files in one folder. | `folder`, `directory` |
| `3` | Hash two files and calculate their TLSH distance. | `compare`, `comparison`, `c` |
| `4` | Close the guided session successfully. | `exit`, `quit`, `q` |

Choosing Exit returns process code `0`, even if an earlier operation reported a file failure. Each
failed operation is reported when it happens; closing the persistent menu is a separate successful
action. Ctrl-D closes the menu. Ctrl-C cancels the current question or operation and safely returns
to the menu; it is never interpreted as accepting a default answer.

### Entering paths

Every path prompt accepts:

- absolute paths such as `/Users/example/file.bin`;
- paths relative to the current working directory, such as `docs/guide.md`;
- `~` and `~/folder` for the current user's home directory;
- matching single or double quotes around a pasted path; and
- backslash-escaped spaces commonly produced by terminal drag and drop.

Press Tab to complete filesystem names. A file prompt suggests both files and directories, because
directories may be traversed while completing a longer path. A folder prompt suggests directories
only. Leave a path empty to return to the main menu.

### Hash one file

The `File path:` question selects exactly one regular file. A directory is rejected with a hint to
use the folder action. After validation, the application displays the size, a progress line, and:

```text
T1...  /absolute/path/to/file
```

A TLSH digest requires an input between 256 and 4,224,281,216 bytes and sufficient feature
diversity. An input that does not meet those requirements is explained instead of producing a
placeholder digest.

### Hash a folder

After `Folder path:`, the application asks:

```text
1  Files directly in this folder
2  This folder and every nested folder
Choose scope [1]:
```

An empty answer or `1` scans only regular files immediately inside the selected folder. `2`,
`recursive`, or `r` includes every nested directory. Symbolic directories are not followed.

The preview reports the file count, combined size, and number of hidden entries skipped. Hidden
files and hidden subdirectories are excluded from folder discovery by default. The guided mode does
not expose an advanced switch for them; use explicit `tlsh hash --include-hidden ...` when they are
deliberately required.

`Start hashing? [Y/n]:` defaults to yes. Press Enter, enter `y`, or enter `yes` to continue. Enter
anything else—normally `n` or `no`—to cancel and return to the menu.

One unreadable or ineligible file does not stop the rest of the folder. Successful digest lines are
printed first. A final `Failed files` section repeats every failed path and its reason where it
cannot be lost among many results.

### Compare two files

`First file:` and `Second file:` select two regular files. The application then asks:

```text
Ignore input-length difference? [y/N]:
```

TLSH normally includes a contribution derived from the files' approximate encoded lengths. The
default answer `n` keeps that contribution. Answer `y` or `yes` to compare only the remaining digest
features. Ignoring length can be useful when content patterns matter more than the size change, but
it is a different distance mode and should not be mixed with length-aware scores.

The result displays both paths, both complete `T1` digests, the selected mode, and the numeric
distance. The CLI deliberately does not label one universal score as "similar" or "different".

## Explicit commands

Run `tlsh --help` for the command list and `tlsh COMMAND --help` for generated option help.

### `tlsh hash`

```text
tlsh hash [OPTIONS] PATH...
```

`PATH` may be a regular file, a directory, or `-` for standard input. Multiple paths may be mixed in
one invocation. Overlapping filesystem paths are de-duplicated, directory results are sorted for
reproducible output, and standard input may appear only once.

| Option | Meaning |
| --- | --- |
| `-r`, `--recursive` | Include nested directories. Symbolic directories remain excluded. |
| `--include-hidden` | Include hidden files discovered in directories and descend into hidden directories. An explicitly named hidden file is always processed without this option. |
| `--progress=auto` | Show live progress only when a terminal is attached. This is the default. |
| `--progress=always` | Force progress, including in an IDE output window. |
| `--progress=never` | Disable live progress for quiet automation. |
| `--no-summary` | Suppress the normal batch summary. Failure details are still printed. |
| `-h`, `--help` | Print command help and exit. |
| `-V`, `--version` | Print the application version and exit. |

Examples:

```shell
tlsh hash report.pdf
tlsh hash first.bin second.bin
tlsh hash downloads/
tlsh hash --recursive project/
tlsh hash --recursive --include-hidden project/
cat report.pdf | tlsh hash -
```

Each successful input produces one standard-output line containing the digest, two spaces, and the
input name. Progress, summaries, and errors use standard error, so digest output can be redirected
without collecting presentation text:

```shell
tlsh hash --progress=never samples/ > digests.txt
```

### `tlsh compare`

```text
tlsh compare [--ignore-length] FIRST_FILE SECOND_FILE
```

This command hashes two regular files and prints only their decimal distance to standard output. It
is the script-oriented counterpart of the guided file comparison.

```shell
score=$(tlsh compare original.bin changed.bin)
```

`--ignore-length` removes the encoded input-length contribution. Either file can fail because it is
missing, not regular, unreadable, outside the supported length range, or insufficiently varied. The
diagnostic identifies the failing path and the process exits with code `1`.

### `tlsh distance`

```text
tlsh distance [--ignore-length] FIRST_DIGEST SECOND_DIGEST
```

This command calculates a score from two existing canonical 72-character `T1` digest strings. It
does not open or hash files. Like `compare`, it prints only the decimal distance and supports the
same `--ignore-length` mode.

Use `compare` when two files are available; use `distance` when their digests have already been
stored or received from another compatible TLSH implementation.

## Output channels

| Channel | Content |
| --- | --- |
| Standard output | Digest records from `hash`, numeric scores from `compare` and `distance`, and guided presentation. |
| Standard error | Explicit-command progress, summaries, and diagnostics. |

Colors are enabled only for a human terminal. Redirected output does not contain ANSI escape codes.

## Exit codes

| Code | Meaning |
| ---: | --- |
| `0` | The explicit operation succeeded, help/version was requested, or the guided menu was closed normally. |
| `1` | Input was missing, unreadable, invalid, or ineligible for TLSH. A batch may still have produced successful digest lines. |
| `2` | Command syntax was invalid, such as a missing required argument or unknown option. |

## Current limits

The CLI supports the standard 128-bucket, one-byte-checksum `T1` representation described in the
main [compatibility documentation](../README.md#compatibility). It does not yet provide JSON output,
parallel folder hashing, native executables, or directory-wide similar-file grouping. Similar-file
search needs an explicit threshold and pair-count policy because a naive scan compares
`n * (n - 1) / 2` file pairs.
