package io.github.leawind.inventory.objectpool;

import io.github.leawind.inventory.windowpeak.SimpleWindowPeakEstimator;
import java.util.function.Supplier;

/**
 * Object pool using a stack
 *
 * <p>~10-25% faster than {@link DequeObjectPool} in standard tests.
 *
 * <p>Configurable expansion threshold and ratio
 */
public class StackObjectPool<T> implements ObjectPool<T> {
  private final Supplier<T> factory;

  private T[] stack;
  private int size = 0;

  private final float expandThreshold;
  private final float expandRatio;

  private final float shrinkThreshold;
  private final float shrinkRatio;

  private final SimpleWindowPeakEstimator peakEstimator;

  public StackObjectPool(Supplier<T> factory) {
    this(factory, 8, 3 / 4f, 4 / 3f, 0.25f, 0.5f, 5000);
  }

  @Deprecated
  public StackObjectPool(
      Supplier<T> factory, int capacity, float expandThreshold, float expandRatio) {
    this(factory, capacity, expandThreshold, expandRatio, 0.25f, 1 / expandRatio, 5000);
  }

  private StackObjectPool(
      Supplier<T> factory,
      int capacity,
      float expandThreshold,
      float expandRatio,
      float shrinkThreshold,
      float shrinkRatio,
      int peakEstimatorWindowSize) {
    if (expandThreshold <= 0 || 1 <= expandThreshold) {
      throw new IllegalArgumentException("expandThreshold must be in (0, 1]");
    }
    if (expandRatio <= 1) {
      throw new IllegalArgumentException("expandRatio must be greater than 1");
    }
    if (shrinkThreshold <= 0 || 1 <= shrinkThreshold) {
      throw new IllegalArgumentException("shrinkThreshold must be in (0, 1]");
    }
    if (shrinkRatio <= 0 || 1 <= shrinkRatio) {
      throw new IllegalArgumentException("shrinkRatio must be in (0, 1)");
    }

    this.factory = factory;
    stack = newStack(capacity);

    this.expandThreshold = expandThreshold;
    this.expandRatio = expandRatio;

    this.shrinkThreshold = shrinkThreshold;
    this.shrinkRatio = shrinkRatio;

    this.peakEstimator = new SimpleWindowPeakEstimator(peakEstimatorWindowSize);
  }

  public void checkShrink(int now) {
    peakEstimator.record(size, now);
    if (peakEstimator.peak() < stack.length * shrinkThreshold) {
      shrink((int) Math.ceil(stack.length * shrinkRatio));
    }
  }

  private void expand(int newCapacity) {
    T[] newStack = newStack(newCapacity);
    System.arraycopy(stack, 0, newStack, 0, size);

    for (int i = size; i < newCapacity; i++) {
      newStack[i] = factory.get();
    }

    stack = newStack;
  }

  private void shrink(int newCapacity) {
    T[] newStack = newStack(newCapacity);
    System.arraycopy(stack, 0, newStack, 0, newCapacity);
    size = newCapacity;
    stack = newStack;
  }

  @Override
  public int idleCount() {
    return size;
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
    return size == 0 ? factory.get() : stack[--size];
  }

  @Override
  public void release(T obj) {
    if (size >= stack.length * expandThreshold) {
      expand((int) Math.ceil(stack.length * expandRatio));
    }

    stack[size++] = obj;
  }

  @SuppressWarnings("unchecked")
  private static <T> T[] newStack(int size) {
    return (T[]) new Object[size];
  }

  public static <T> Builder<T> builder(Supplier<T> factory) {
    return new Builder<>(factory);
  }

  public static class Builder<T> {
    private final Supplier<T> factory;
    private int capacity = 8;
    private float expandThreshold = 0.75f;
    private float expandRatio = 1.5f;
    private float shrinkThreshold = 0.25f;
    private float shrinkRatio = 0.5f;
    private int peakEstimatorWindowSize = 5000;

    public Builder(Supplier<T> factory) {
      this.factory = factory;
    }

    public Builder<T> capacity(int capacity) {
      this.capacity = capacity;
      return this;
    }

    public Builder<T> expandThreshold(float expandThreshold) {
      this.expandThreshold = expandThreshold;
      return this;
    }

    public Builder<T> expandRatio(float expandRatio) {
      this.expandRatio = expandRatio;
      return this;
    }

    public Builder<T> shrinkThreshold(float shrinkThreshold) {
      this.shrinkThreshold = shrinkThreshold;
      return this;
    }

    public Builder<T> shrinkRatio(float shrinkRatio) {
      this.shrinkRatio = shrinkRatio;
      return this;
    }

    public Builder<T> peakWindow(int windowSize) {
      this.peakEstimatorWindowSize = windowSize;
      return this;
    }

    public StackObjectPool<T> build() {
      if (factory == null) {
        throw new IllegalStateException("Factory must be provided");
      }
      return new StackObjectPool<>(
          factory,
          capacity,
          expandThreshold,
          expandRatio,
          shrinkThreshold,
          shrinkRatio,
          peakEstimatorWindowSize);
    }
  }
}
