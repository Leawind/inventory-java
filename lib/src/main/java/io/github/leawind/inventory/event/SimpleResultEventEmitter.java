package io.github.leawind.inventory.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class SimpleResultEventEmitter<E, R> {

  private final Collection<Listener<E, R>> listeners;

  public SimpleResultEventEmitter() {
    this.listeners = new ArrayList<>();
  }

  public SimpleResultEventEmitter(Collection<Listener<E, R>> listeners) {
    this.listeners = listeners;
  }

  public SimpleResultEventEmitter<E, R> clear() {
    listeners.clear();
    return this;
  }

  public SimpleResultEventEmitter<E, R> on(Listener<E, R> listener) {
    listeners.add(listener);
    return this;
  }

  public SimpleResultEventEmitter<E, R> on(Listener.NoArg<E, R> listener) {
    listeners.add(listener);
    return this;
  }

  public void emit() {
    emit(null);
  }

  public Stream<R> emit(@Nullable E event) {
    return listeners.stream().map(listener -> listener.on(event));
  }

  public interface Listener<E, R> {
    R on(E event);

    interface NoArg<E, R> extends Listener<E, R> {
      R on();

      @Override
      default R on(E event) {
        return on();
      }
    }
  }
}
