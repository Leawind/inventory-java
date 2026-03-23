package io.github.leawind.inventory.windowpeak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleWindowPeakEstimatorTest {

  @Test
  @DisplayName("Should initialize with MIN_VALUE")
  void testInitialState() {
    SimpleWindowPeakEstimator estimator = new SimpleWindowPeakEstimator(1000);
    assertEquals(Integer.MIN_VALUE, estimator.peak(), "Initial peak should be MIN_VALUE");
  }

  @Test
  @DisplayName("Should update peak when a larger value is recorded within the window")
  void testUpdatePeakWithinWindow() {
    SimpleWindowPeakEstimator estimator = new SimpleWindowPeakEstimator(1000);

    estimator.record(10, 1000);
    estimator.record(50, 1500); // 500ms later, within 1000ms window
    estimator.record(30, 1800); // Smaller value, should be ignored

    assertEquals(50, estimator.peak());
  }

  @Test
  @DisplayName("Should reset peak when time exceeds the window duration")
  void testResetPeakAfterWindow() {
    SimpleWindowPeakEstimator estimator = new SimpleWindowPeakEstimator(1000);

    estimator.record(100, 1000);
    assertEquals(100, estimator.peak());

    // Record a smaller value but after the window has passed (1000 + 1001)
    estimator.record(20, 2001);

    assertEquals(
        20, estimator.peak(), "Peak should be reset to the new value after window expiration");
  }

  @Test
  @DisplayName("Should update peak time even if the value is the same but later")
  void testUpdatePeakTime() {
    SimpleWindowPeakEstimator estimator = new SimpleWindowPeakEstimator(1000);

    estimator.record(100, 1000); // Peak is 100 at T=1000
    estimator.record(100, 1500); // Peak is still 100, but time updated to T=1500

    // This should NOT expire at T=2200 because peakTime was moved to 1500
    estimator.record(50, 2200);
    assertEquals(100, estimator.peak());

    // This SHOULD expire at T=2501 (1500 + 1001)
    estimator.record(10, 2501);
    assertEquals(10, estimator.peak());
  }

  @Test
  @DisplayName("Should handle continuous window sliding")
  void testContinuousSliding() {
    SimpleWindowPeakEstimator estimator = new SimpleWindowPeakEstimator(100);

    estimator.record(10, 100);
    estimator.record(20, 150);
    estimator.record(30, 200);

    assertEquals(30, estimator.peak());

    // At T=301, the window from the last peak (T=200) hasn't expired
    estimator.record(5, 250);
    assertEquals(30, estimator.peak());

    // At T=301 (200 + 101), the peak 30 expires
    estimator.record(1, 301);
    assertEquals(1, estimator.peak());
  }
}
