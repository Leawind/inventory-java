package io.github.leawind.inventory.windowpeak;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BucketedWindowPeakEstimatorTest {

  @Test
  @DisplayName("Should record and retrieve the peak value within the same bucket")
  void testBasicRecordAndPeak() {
    WindowPeakEstimator estimator = new BucketedWindowPeakEstimator(2000, 500);

    estimator.record(10, 1000);
    estimator.record(25, 1050); // Same bucket
    estimator.record(15, 1080); // Same bucket

    assertEquals(25, estimator.peak(), "Peak should be the maximum value recorded");
  }

  @Test
  @DisplayName("Should maintain peak across multiple buckets within the window")
  void testPeakAcrossBuckets() {
    // 3 buckets total (300ms window / 100ms per bucket)
    WindowPeakEstimator estimator = new BucketedWindowPeakEstimator(300, 100);

    estimator.record(50, 1000); // Bucket 0
    estimator.record(20, 1100); // Bucket 1
    estimator.record(30, 1200); // Bucket 2

    assertEquals(50, estimator.peak(), "Should retain 50 from the first bucket");
  }

  @Test
  @DisplayName("Should clear old values when time advances beyond the window")
  void testSlidingWindowExpiration() {
    // 2 buckets (200ms window / 100ms per bucket)
    WindowPeakEstimator estimator = new BucketedWindowPeakEstimator(200, 100);

    estimator.record(100, 1000); // Bucket 0
    estimator.record(50, 1100); // Bucket 1
    assertEquals(100, estimator.peak());

    // Advance to 1200: Bucket 0 (where 100 was) should be cleared and reused
    estimator.record(10, 1200);
    assertEquals(50, estimator.peak(), "The value 100 should have expired");

    // Advance to 1300: Bucket 1 (where 50 was) should be cleared
    estimator.record(5, 1300);
    assertEquals(10, estimator.peak(), "The value 50 should have expired");
  }

  @Test
  @DisplayName("Should handle a large time jump that clears all buckets")
  void testLargeTimeJump() {
    WindowPeakEstimator estimator = new BucketedWindowPeakEstimator(1000, 100);

    estimator.record(100, 1000);
    assertEquals(100, estimator.peak());

    // Jump 2 seconds (well beyond the 1s window)
    estimator.record(20, 3000);
    assertEquals(20, estimator.peak(), "All old values should be cleared after a large jump");
  }

  @ParameterizedTest
  @CsvSource({"0, 100", "100, 0", "-1, 100"})
  @DisplayName("Should throw exception for invalid constructor arguments")
  void testInvalidArguments(long window, long bucket) {
    assertThrows(
        IllegalArgumentException.class, () -> new BucketedWindowPeakEstimator(window, bucket));
  }

  @Test
  @DisplayName("Should handle multiple samples in the same timestamp")
  void testMultipleSamplesSameTime() {
    WindowPeakEstimator estimator = new BucketedWindowPeakEstimator(1000, 100);
    estimator.record(10, 1000);
    estimator.record(30, 1000);
    estimator.record(20, 1000);

    assertEquals(30, estimator.peak());
  }
}
