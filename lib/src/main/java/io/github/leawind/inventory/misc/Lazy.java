package io.github.leawind.inventory.misc;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

  private final Supplier<T> valueGetter;
  private T value;

  public Lazy(Supplier<T> valueGetter) {
    this.valueGetter = valueGetter;
  }

  public void set(T value) {
    this.value = value;
  }

  @Override
  public T get() {
    if (value == null) {
      value = valueGetter.get();
    }
    return value;
  }

  public void reset() {
    this.value = null;
  }

  public boolean isComputed() {
    return value != null;
  }
}
