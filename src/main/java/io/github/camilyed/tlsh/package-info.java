/**
 * Calculates and compares readable, educational TLSH similarity digests.
 *
 * <p>{@link io.github.camilyed.tlsh.Tlsh} is the main entry point. It can hash a complete byte
 * array at once or create a {@link io.github.camilyed.tlsh.TlshHasher} for data that arrives in
 * chunks. The resulting {@link io.github.camilyed.tlsh.TlshDigest} can be encoded as text, parsed,
 * and compared with another digest.
 *
 * <p>This implementation uses the standard versioned {@code T1} representation with 128 histogram
 * buckets and a one-byte checksum. Inputs must contain at least 256 bytes and enough variation to
 * produce a meaningful similarity digest. Alternate reference-library configurations and the legacy
 * unprefixed representation are outside the current compatibility scope.
 */
package io.github.camilyed.tlsh;
