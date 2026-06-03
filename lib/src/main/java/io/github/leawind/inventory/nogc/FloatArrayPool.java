package io.github.leawind.inventory.nogc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

public final class FloatArrayPool {

  private static final Comparator<float[]> POOL_COMPARATOR =
      (a, b) -> {
        if (a == b) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return Integer.compare(a.length, b.length);
      };

  private final float[][] pool;

  int size;
  private final ReentrantLock lock = new ReentrantLock();

  public FloatArrayPool(int maxCapacity) {
    this.pool = new float[maxCapacity][];
  }

  FloatArrayPool(float[][] pool, int size) {
    this.pool = pool;
    this.size = size;
  }

  FloatArrayPool(float[][] pool) {
    this.pool = pool;
    Arrays.sort(pool, POOL_COMPARATOR);
    for (int i = 0; i < pool.length; i++) {
      if (pool[i] == null) {
        size = i;
        break;
      }
    }
  }

  public float[] acquire(int minSize) {
    lock.lock();
    try {
      int index = findPopPosition(minSize);
      if (index == -1) {
        return new float[minSize];
      }

      float[] result = pool[index];
      if (index < size - 1) {
        System.arraycopy(pool, index + 1, pool, index, size - index - 1);
      }
      size--;
      return result;
    } finally {
      lock.unlock();
    }
  }

  public void release(float[] array) {
    lock.lock();
    try {
      if (size >= pool.length) {
        return;
      }

      int index = findInsertPosition(array.length);
      if (index < size) {
        System.arraycopy(pool, index, pool, index + 1, size - index);
      }
      pool[index] = array;
      size++;
    } finally {
      lock.unlock();
    }
  }

  int findPopPosition(int target) {
    if (size == 0) {
      return -1;
    }

    int left = 0;
    int right = this.size;
    while (left < right) {
      int mid = (left + right) >>> 1;
      if (pool[mid].length < target) {
        left = mid + 1;
      } else {
        right = mid;
      }
    }
    return left < this.size ? left : -1;
  }

  int findInsertPosition(int target) {
    if (this.size == 0) {
      return 0;
    }

    int left = 0;
    int right = this.size;
    while (left < right) {
      int mid = (left + right) >>> 1;
      if (pool[mid].length <= target) {
        left = mid + 1;
      } else {
        right = mid;
      }
    }
    return left;
  }
}
