package io.github.camilyed.tlsh;

/**
 * Holds the three bucket-count thresholds that divide a histogram distribution into quarters.
 *
 * <p>These values are histogram heights, not bucket indices. For example, {@code firstQuartile =
 * 12} means that the lowest quarter of sorted bucket counts ends at a count of {@code 12}. The
 * values are ordered so that {@code firstQuartile <= secondQuartile <= thirdQuartile}; equal values
 * are possible when multiple buckets have the same count.
 *
 * @param firstQuartile upper count threshold for the lowest quarter of buckets
 * @param secondQuartile upper count threshold for the lower half of buckets; also the median
 * @param thirdQuartile upper count threshold for the lowest three quarters of buckets
 */
record HistogramQuartiles(long firstQuartile, long secondQuartile, long thirdQuartile) {}
