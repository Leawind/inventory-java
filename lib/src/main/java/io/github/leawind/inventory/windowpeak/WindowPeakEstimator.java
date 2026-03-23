package io.github.leawind.inventory.windowpeak;

public interface WindowPeakEstimator {
  void record(int value, int now);

  int peak();
}
