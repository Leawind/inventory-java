package io.github.leawind.inventory.event;

import io.github.leawind.inventory.event.impl.EventEmitterImpl;

public interface EventEmitter<E, K>
    extends DependencyRegistry<K, BiListener<E, EventEmitter.Control>> {

  interface Control {
    void stop();

    void unregister();
  }

  interface Owned<E, K>
      extends EventEmitter<E, K>, DependencyRegistry.Owned<K, BiListener<E, EventEmitter.Control>> {
    void emit(E event);
  }

  // region on

  default void on(K key, BiListener<E, Control> listener) {
    register(key, listener);
  }

  default void on(K key, BiListener.NoArg<E, Control> listener) {
    on(key, (BiListener<E, Control>) listener);
  }

  default void on(K key, BiListener.OneArg<E, Control> listener) {
    on(key, (BiListener<E, Control>) listener);
  }

  // endregion

  // region once

  default void once(K key, BiListener<E, Control> listener) {
    register(
        key,
        (event, ctrl) -> {
          listener.on(event, ctrl);
          ctrl.unregister();
        });
  }

  default void once(K key, BiListener.NoArg<E, Control> listener) {
    once(key, (BiListener<E, Control>) listener);
  }

  default void once(K key, BiListener.OneArg<E, Control> listener) {
    once(key, (BiListener<E, Control>) listener);
  }

  // endregion

  static <E, K> Owned<E, K> create() {
    return new EventEmitterImpl<>();
  }
}
