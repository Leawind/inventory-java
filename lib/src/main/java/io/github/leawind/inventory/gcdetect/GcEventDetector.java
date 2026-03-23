package io.github.leawind.inventory.gcdetect;

public interface GcEventDetector {
  /**
   * Returns true if at least one GC event has been observed since the last call. Each GC is
   * reported at most once.
   */
  boolean poll();
}
