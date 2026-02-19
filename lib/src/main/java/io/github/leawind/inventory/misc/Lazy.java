package io.github.leawind.inventory.misc;


import java.util.function.Supplier;

public class Lazy<T> {

  private final Supplier<T> supplier;
  private T value;

  public Lazy(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  public T get() {
    if (value == null) {
      value = supplier.get();
    }
    return value;
  }

  public void reset() {
    this.value = null;
  }

  public boolean isInitialized() {
    return value != null;
  }
}
