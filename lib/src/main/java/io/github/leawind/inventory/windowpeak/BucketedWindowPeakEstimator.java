package io.github.leawind.inventory.windowpeak;

import java.util.Arrays;

public class BucketedWindowPeakEstimator implements WindowPeakEstimator {

  private final int[] buckets;
  private final long bucketDurationMillis;

  private int currentBucket = 0;
  private long lastTickTime = 0;

  public BucketedWindowPeakEstimator(long windowMillis, long bucketDurationMillis) {
    if (windowMillis <= 0 || bucketDurationMillis <= 0) {
      throw new IllegalArgumentException("windowMillis and bucketDurationMillis must be > 0");
    }
    int count = (int) Math.ceil((double) windowMillis / bucketDurationMillis);
    if (count <= 0) {
      throw new IllegalArgumentException("bucket count must be > 0");
    }

    this.bucketDurationMillis = bucketDurationMillis;
    this.buckets = new int[count];
  }

  @Override
  public void record(int sample, long now) {
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
    if (elapsed < bucketDurationMillis) {
      return;
    }

    int steps = (int) (elapsed / bucketDurationMillis);
    int move = Math.min(steps, buckets.length);

    for (int i = 0; i < move; i++) {
      currentBucket = (currentBucket + 1) % buckets.length;
      // clear outdated bucket
      buckets[currentBucket] = 0;
    }

    lastTickTime += (long) steps * bucketDurationMillis;
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
