# TLSH benchmarks

This non-published module contains JMH benchmarks for measuring the current scalar implementation.
It depends on the root library through its public API and does not change the production module
layout.

Run the complete benchmark set:

```shell
./gradlew :tlsh-benchmarks:jmh
```

The repository defaults deliberately form a short development run: one fork, one warmup iteration,
and three measurement iterations of one second each. This is useful for catching broken benchmarks,
but it is not enough evidence for a performance claim.

For a longer local measurement of all hashing benchmarks, override the JMH options:

```shell
./gradlew :tlsh-benchmarks:jmh \
  -Pjmh.includes=TlshHashBenchmark \
  -Pjmh.forks=3 \
  -Pjmh.warmupIterations=5 \
  -Pjmh.iterations=5 \
  -Pjmh.warmupTime=3s \
  -Pjmh.measurementTime=3s \
  -Pjmh.profilers=gc
```

The `gc` profiler reports allocation information in addition to timing. JMH prints its human-
readable report to the console and writes the machine-readable result to
`tlsh-benchmarks/build/results/jmh/results.json`.

The benchmarks measure complete `Tlsh.hash(byte[])` and `Tlsh.hash(InputStream)` calls for 256-byte,
4 KiB, and 1 MiB inputs. Input generation happens once before measurement, and every result is
consumed so the JVM cannot discard the hashing work as unused. The stream benchmark resets and
reuses an in-memory `ByteArrayInputStream`: it measures the library's stream read loop and internal
buffer, not stream construction, filesystem caching, or storage performance. Compare results only
when the JDK, hardware, power mode, JMH options, and other relevant environment details are recorded
alongside them.

Sizes use binary units: 1 KiB is 1,024 bytes and 1 MiB is 1,048,576 bytes. The decimal unit 1 MB is
1,000,000 bytes, so it would not accurately describe the largest benchmark parameter.

## Recorded experiments

- [2026-08-31: removing per-window allocations](results/2026-08-31-hot-path-refactor.md) compares
  the initial readable implementation with its allocation-free byte-processing hot path.
