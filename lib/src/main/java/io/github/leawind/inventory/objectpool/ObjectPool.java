package io.github.leawind.inventory.objectpool;

/**
 * An interface for an object pool that manages reusable objects of type {@code T}.
 *
 * <p>This interface is designed with a performance-first philosophy. Implementations are typically
 * lightweight and may deliberately avoid additional safety guarantees (such as thread-safety or
 * automatic state management) in order to minimize overhead.
 *
 * <p><strong>Usage considerations:</strong>
 *
 * <ul>
 *   <li><strong>Thread-safety:</strong> Unless explicitly documented by an implementation, object
 *       pools are <em>not</em> thread-safe. External synchronization is required for concurrent
 *       use.
 *   <li><strong>Object state:</strong> Objects obtained from the pool may contain residual state
 *       from previous use. Callers are responsible for fully initializing or resetting objects
 *       after {@link #acquire()} and before reuse.
 *   <li><strong>Lifecycle discipline:</strong> Every acquired object should eventually be returned
 *       via {@link #release(Object)}. Failure to do so may lead to increased allocation or resource
 *       pressure.
 *   <li><strong>Capacity management:</strong> Pools may grow over time and are not required to
 *       shrink. Callers should use {@link #ensureIdle(int)} or other mechanisms (if provided) to
 *       control pool size according to workload characteristics.
 *   <li><strong>Correctness vs performance trade-off:</strong> This abstraction favors low overhead
 *       over defensive checks. Misuse (e.g., double release, use-after-release, or sharing pooled
 *       objects across threads without coordination) may result in undefined behavior.
 * </ul>
 *
 * @param <T> the type of objects managed by this pool
 */
public interface ObjectPool<T> {

  /**
   * Returns the number of idle objects currently available in the pool.
   *
   * @return the count of idle objects
   */
  int idleCount();

  /**
   * Ensures that the pool has at least the specified minimum number of idle objects.
   *
   * <p>This method may create new objects eagerly to satisfy the requested capacity. It is
   * typically used for pre-warming the pool to avoid allocation spikes during critical execution
   * paths.
   *
   * @param minSize the minimum number of idle objects to maintain in the pool
   * @return this object pool instance for method chaining
   */
  ObjectPool<T> ensureIdle(int minSize);

  /**
   * Acquires an object from the pool.
   *
   * <p>If no idle objects are available, a new object may be created depending on the
   * implementation.
   *
   * <p><strong>Note:</strong> The returned object is not guaranteed to be in a clean or default
   * state. Callers must initialize or reset it before use.
   *
   * @return an object from the pool (never {@code null})
   */
  T acquire();

  /**
   * Releases an object back to the pool, making it available for future reuse.
   *
   * <p><strong>Important:</strong>
   *
   * <ul>
   *   <li>The object must not be used after being released.
   *   <li>The object should not be released more than once.
   * </ul>
   *
   * <p>Violating these constraints results in undefined behavior.
   *
   * @param obj the object to return to the pool
   */
  void release(T obj);
}
