package io.github.leawind.inventory.objectpool;

import java.util.ArrayDeque;
import java.util.function.Supplier;

/**
 * Object pool using a double-ended queue
 *
 * <p>~10-25% slower than {@link StackObjectPool} in standard tests.
 */
public class DequeObjectPool<T> implements ObjectPool<T> {
  private final Supplier<T> factory;

  private final ArrayDeque<T> queue;

  public DequeObjectPool(Supplier<T> factory) {
    this(factory, 8);
  }

  public DequeObjectPool(Supplier<T> factory, int capacity) {
    queue = new ArrayDeque<>(capacity);
    this.factory = factory;
  }

  @Override
  public int idleCount() {
    return queue.size();
  }

  @Override
  public ObjectPool<T> ensureIdle(int minSize) {
    while (idleCount() < minSize) {
      release(factory.get());
    }
    return this;
  }

  @Override
  public T acquire() {
    var obj = queue.poll();
    if (obj == null) {
      obj = factory.get();
    }
    return obj;
  }

  @Override
  public void release(T obj) {
    queue.add(obj);
  }
}
