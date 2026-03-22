package io.github.leawind.inventory.objectpool;

public interface ObjectPool<T> {
  int idleCount();

  ObjectPool<T> ensureIdle(int minSize);

  T acquire();

  void release(T obj);
}
