package io.github.leawind.inventory.objectpool;

/**
 * An interface for an object pool that manages reusable objects of type T. Object pools are used to
 * reduce the overhead of creating and destroying objects by reusing them.
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
   * Ensures that the pool has at least the specified minimum number of idle objects. If the current
   * number of idle objects is less than the specified minimum, new objects may be created and added
   * to the pool.
   *
   * @param minSize the minimum number of idle objects to maintain in the pool
   * @return this object pool instance for method chaining
   */
  ObjectPool<T> ensureIdle(int minSize);

  /**
   * Acquires an object from the pool. The object is removed from the pool and should be returned to
   * the pool using {@link #release(Object)} when no longer needed.
   *
   * @return an object from the pool, or null if no objects are available
   */
  T acquire();

  /**
   * Releases an object back to the pool, making it available for future use.
   *
   * <p>Note: Releasing an object that is already idle (not currently acquired) results in undefined
   * behavior.
   *
   * @param obj the object to return to the pool
   */
  void release(T obj);
}
