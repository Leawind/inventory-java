package io.github.leawind.inventory.event;

import io.github.leawind.inventory.event.impl.SimpleEventEmitterImpl;
import java.util.Collection;

public interface SimpleEventEmitter<E> {
  static <E> Owned<E> create() {
    return new SimpleEventEmitterImpl<>();
  }

  static <E> Owned<E> create(Collection<Listener<E>> listeners) {
    return new SimpleEventEmitterImpl<>(listeners);
  }

  void on(Listener<E> listener);

  void on(Listener.NoArg<E> listener);

  interface Owned<E> extends SimpleEventEmitter<E> {

    void clear();

    void emit();

    void emit(E event);
  }

  interface Listener<E2> {
    void on(E2 event);

    interface NoArg<E> extends Listener<E> {
      void on();

      @Override
      default void on(E event) {
        on();
      }
    }
  }
}
