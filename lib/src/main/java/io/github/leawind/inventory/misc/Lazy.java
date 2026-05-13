package io.github.leawind.inventory.misc;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

  private final Supplier<T> valueGetter;
  private volatile T value;

  public Lazy(Supplier<T> valueGetter) {
    this.valueGetter = valueGetter;
  }

  public synchronized void set(T value) {
    this.value = value;
  }

  @Override
  public T get() {
    if (value == null) {
      synchronized (this) {
        if (value == null) {
          value = valueGetter.get();
        }
      }
    }
    return value;
  }

  public synchronized void reset() {
    this.value = null;
  }

  public synchronized boolean isComputed() {
    return value != null;
  }
}
