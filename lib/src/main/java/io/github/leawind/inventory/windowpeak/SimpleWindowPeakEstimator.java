package io.github.leawind.inventory.windowpeak;

public class SimpleWindowPeakEstimator implements WindowPeakEstimator {
  private final int windowSize;

  private int peak = Integer.MIN_VALUE;
  private long peakTime = 0;

  public SimpleWindowPeakEstimator(int windowSize) {
    this.windowSize = windowSize;
  }

  @Override
  public void record(int value, int now) {
    if (now - peakTime > windowSize) {
      peak = value;
      peakTime = now;
    } else if (value >= peak) {
      peak = value;
      peakTime = now;
    }
  }

  @Override
  public int peak() {
    return peak;
  }
}
