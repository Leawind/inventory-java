package io.github.leawind.inventory.event;

import java.util.ArrayList;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

public class SimpleEventEmitter<E> {

  private final Collection<Listener<E>> listeners;

  public SimpleEventEmitter() {
    this.listeners = new ArrayList<>();
  }

  public SimpleEventEmitter(Collection<Listener<E>> listeners) {
    this.listeners = listeners;
  }

  public SimpleEventEmitter<E> clear() {
    listeners.clear();
    return this;
  }

  public SimpleEventEmitter<E> on(Listener<E> listener) {
    listeners.add(listener);
    return this;
  }

  public SimpleEventEmitter<E> on(Listener.NoArg<E> listener) {
    listeners.add(listener);
    return this;
  }

  public void emit() {
    emit(null);
  }

  public void emit(@Nullable E event) {
    listeners.forEach(listener -> listener.on(event));
  }

  public interface Listener<E> {
    void on(E event);

    interface NoArg<E> extends Listener<E> {
      void on();

      @Override
      default void on(E event) {
        on();
      }
    }
  }
}
