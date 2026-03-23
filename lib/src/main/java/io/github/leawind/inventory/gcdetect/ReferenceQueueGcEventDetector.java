package io.github.leawind.inventory.gcdetect;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A lightweight GC event detector based on {@link ReferenceQueue}.
 *
 * <p>This implementation uses a weakly reachable sentinel object. When the JVM performs GC and
 * reclaims the sentinel, its associated {@link WeakReference} is enqueued into a {@link
 * ReferenceQueue}. The detector polls this queue to observe GC events.
 *
 * <p>Characteristics:
 *
 * <ul>
 *   <li>Lock-free and allocation-free on steady-state polling path
 *   <li>No background threads
 *   <li>Best-effort detection (depends on GC behavior)
 *   <li>Multiple GC events between polls may be coalesced into a single {@code true}
 * </ul>
 *
 * <p>Thread-safety: this implementation is not strictly thread-safe. If used concurrently, external
 * synchronization may be required depending on the use case.
 */
public final class ReferenceQueueGcEventDetector implements GcEventDetector {

  private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
  private WeakReference<Object> signal;

  public ReferenceQueueGcEventDetector() {
    resetSignal();
  }

  @Override
  public boolean poll() {
    boolean observed = false;

    // Drain the queue to coalesce multiple GC events into one "true"
    Reference<?> ref;
    while ((ref = queue.poll()) != null) {
      observed = true;
    }

    if (observed) {
      // Re-register a new sentinel so future GC events can be observed
      resetSignal();
    }

    return observed;
  }

  private void resetSignal() {
    // The sentinel object is intentionally not stored strongly anywhere else
    // so it becomes eligible for GC immediately after this method returns.
    Object sentinel = new Object();
    signal = new WeakReference<>(sentinel, queue);
  }
}
