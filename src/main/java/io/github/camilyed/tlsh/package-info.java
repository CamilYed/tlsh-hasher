/**
 * Provides the readable, educational TLSH implementation.
 *
 * <p>The current streaming feature pipeline maintains a five-byte window, maps six local byte
 * combinations through Pearson hashing, and records their bucket frequencies in a histogram. Later
 * exercises will add the remaining TLSH digest components.
 */
package io.github.camilyed.tlsh;
