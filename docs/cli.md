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
4  Find similar files
5  Exit
```

The menu remains open after each operation. Numeric choices are always accepted; the textual aliases
shown below are conveniences. An empty menu answer selects `1`.

| Choice | Meaning | Accepted aliases |
| --- | --- | --- |
| `1` | Calculate and display one file's canonical `T1` digest. | `file`, `f`, or an empty answer |
| `2` | Preview and hash the selected files in one folder. | `folder`, `directory` |
| `3` | Hash two files and calculate their TLSH distance. | `compare`, `comparison`, `c` |
| `4` | Find pairs within a selected maximum TLSH distance. | `similar`, `scan`, `find` |
| `5` | Close the guided session successfully. | `exit`, `quit`, `q` |

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
Include approximate file-size difference in score? [Y/n]:
```

TLSH does not store the exact byte count in a digest. It stores a compact code identifying an
approximate file-size range. The default answer `y` includes a penalty when the two files belong to
different size ranges. Answer `n` or `no` to ignore only that penalty and compare the checksum,
quartile ratios, and local-pattern histogram. Ignoring size can be useful for longer and shorter
versions of related content, but it is a different distance mode and should not be mixed with
size-aware scores.

The result displays both paths, both complete `T1` digests, the selected mode, and the numeric
distance. The CLI deliberately does not label one universal score as "similar" or "different".

### Find similar files

This action selects a folder and traversal scope just like **Hash a folder**, then asks:

```text
Maximum TLSH distance [0]:
Include approximate file-size difference in score? [Y/n]:
```

The maximum is inclusive: a value of `100` reports pairs whose distance is between `0` and `100`.
Zero is the cautious default and means equal TLSH digests; it still does not prove byte-for-byte
equality. The file-size question selects the same two distance modes as file comparison.

Before reading the files, the preview shows both the number of files and the number of unique pairs.
For example, 100 files require 4,950 comparisons. The interactive safety limit is 1,000,000 pairs.
A larger selection is refused with an explanation; use the explicit command and deliberately raise
its limit when that cost is acceptable. After confirmation, `Hashing files` reports byte and file
progress. `Comparing digests` then reports completed pairs and comparison throughput.

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

### `tlsh similar`

```text
tlsh similar [OPTIONS] DIRECTORY
```

The command discovers regular files using the same hidden-file, recursion, and symbolic-directory
rules as `hash`. It calculates each usable file's digest once, then compares every unique pair. A
file is never compared with itself, and `(a, b)` is not repeated as `(b, a)`.

| Option | Meaning |
| --- | --- |
| `-r`, `--recursive` | Include nested directories. Symbolic directories remain excluded. |
| `--include-hidden` | Include hidden files and descend into hidden directories. |
| `--max-distance=N` | Include scores from zero through `N`, inclusive. The default is `0`. |
| `--ignore-length` | Ignore the approximate file-size ranges stored in the digests. |
| `--max-comparisons=N` | Refuse more than `N` unique pairs. The default is `1,000,000`. |
| `--progress=auto` | Show both progress phases only when a terminal is attached. This is the default. |
| `--progress=always` | Force progress, including in an IDE output window. |
| `--progress=never` | Disable transient progress for quiet automation. |
| `-h`, `--help` | Print command help and exit. |
| `-V`, `--version` | Print the application version and exit. |

Matches are sorted by distance, first path, and second path. Each standard-output record is:

```text
DISTANCE  FIRST_PATH  SECOND_PATH
```

The summary and failed-file details use standard error. One unreadable or TLSH-ineligible file does
not discard matches among the remaining files, but it makes the command return code `1`.
Progress also uses standard error and has two phases: bytes and files while calculating digests,
then completed pairs while comparing the successfully calculated digests.

All-pairs work grows quadratically:

```text
comparisons = fileCount * (fileCount - 1) / 2
```

Thus 1,000 files require 499,500 comparisons, while 10,000 files require 49,995,000. The default
limit prevents a broad directory from silently starting an unexpectedly expensive scan. Raising it
changes only the guardrail, not the maximum distance used to select results.

### `tlsh compare`

```text
tlsh compare [--ignore-length] FIRST_FILE SECOND_FILE
```

This command hashes two regular files and prints only their decimal distance to standard output. It
is the script-oriented counterpart of the guided file comparison.

```shell
score=$(tlsh compare original.bin changed.bin)
```

`--ignore-length` ignores the approximate file-size ranges stored in the digests. Either file can
fail because it is missing, not regular, unreadable, outside the supported length range, or
insufficiently varied. The diagnostic identifies the failing path and the process exits with code
`1`.

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
| Standard output | Digest records from `hash`, pair records from `similar`, numeric scores from `compare` and `distance`, and guided presentation. |
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
parallel folder hashing, native executables, or clustering that merges overlapping similar pairs
into groups. The current `similar` command reports direct pairs within one explicit threshold.
