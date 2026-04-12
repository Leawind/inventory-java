package io.github.leawind.inventory.misc;

public interface UncheckedCloseable extends AutoCloseable {
  @Override
  void close();
}
