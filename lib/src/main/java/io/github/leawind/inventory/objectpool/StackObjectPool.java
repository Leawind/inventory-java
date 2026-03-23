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

  private final float shrinkThreshold = 0.25f;
  private final float shrinkRatio = 0.25f;

  private SimpleWindowPeakEstimator peakEstimator = new SimpleWindowPeakEstimator(5000);

  public StackObjectPool(Supplier<T> factory) {
    this(factory, 8, 3 / 4f, 4 / 3f);
  }

  public StackObjectPool(
      Supplier<T> factory, int capacity, float expandThreshold, float expandRatio) {
    if (expandThreshold <= 0 || 1 <= expandThreshold) {
      throw new IllegalArgumentException("expandThreshold must be in (0, 1]");
    }
    if (expandRatio <= 1) {
      throw new IllegalArgumentException("expandRatio must be greater than 1");
    }

    this.factory = factory;
    stack = newStack(capacity);
    this.expandThreshold = expandThreshold;
    this.expandRatio = expandRatio;
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
}
