package io.github.leawind.inventory.windowpeak;

public interface WindowPeakEstimator {
  void record(int value, long now);

  default void record(int sample) {
    record(sample, System.currentTimeMillis());
  }

  int peak();
}
