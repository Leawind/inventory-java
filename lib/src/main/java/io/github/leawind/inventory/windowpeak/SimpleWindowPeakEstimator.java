package io.github.leawind.inventory.windowpeak;

public class SimpleWindowPeakEstimator implements WindowPeakEstimator {
  private final int windowMillis;

  private int peak = Integer.MIN_VALUE;
  private long peakTime = 0;

  public SimpleWindowPeakEstimator(int windowMillis) {
    this.windowMillis = windowMillis;
  }

  @Override
  public void record(int value, long now) {
    if (now - peakTime > windowMillis) {
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
