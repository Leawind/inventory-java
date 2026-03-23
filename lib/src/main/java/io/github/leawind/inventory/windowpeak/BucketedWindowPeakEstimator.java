package io.github.leawind.inventory.windowpeak;

import java.util.Arrays;

public class BucketedWindowPeakEstimator implements WindowPeakEstimator {

  private final int[] buckets;
  private final int bucketSize;

  private int currentBucket = 0;
  private int lastTickTime = 0;

  public BucketedWindowPeakEstimator(int windowSize, int bucketSize) {
    if (windowSize <= 0 || bucketSize <= 0) {
      throw new IllegalArgumentException("windowSize and bucketSize must be > 0");
    }
    int count = (int) Math.ceil((double) windowSize / bucketSize);
    if (count <= 0) {
      throw new IllegalArgumentException("bucket count must be > 0");
    }

    this.bucketSize = bucketSize;
    this.buckets = new int[count];
  }

  @Override
  public void record(int sample, int now) {
    advance(now);

    // store peak value
    if (sample > buckets[currentBucket]) {
      buckets[currentBucket] = sample;
    }
  }

  @Override
  public int peak() {
    int max = 0;
    for (int v : buckets) {
      if (v > max) max = v;
    }
    return max;
  }

  private void advance(long now) {
    long elapsed = now - lastTickTime;
    if (elapsed < bucketSize) {
      return;
    }

    int steps = (int) (elapsed / bucketSize);
    int move = Math.min(steps, buckets.length);

    for (int i = 0; i < move; i++) {
      currentBucket = (currentBucket + 1) % buckets.length;
      // clear outdated bucket
      buckets[currentBucket] = 0;
    }

    lastTickTime += steps * bucketSize;
  }

  @Override
  public String toString() {
    return "BucketedWindowPeakEstimator{"
        + "buckets="
        + Arrays.toString(buckets)
        + ", currentBucket="
        + currentBucket
        + '}';
  }
}
