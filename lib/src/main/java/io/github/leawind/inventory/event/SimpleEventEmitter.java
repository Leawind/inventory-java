package io.github.leawind.inventory.event;

import org.jspecify.annotations.Nullable;

public class SimpleEventEmitter<E> {
  protected boolean isOnce;
  protected @Nullable Listener<E> listener;

  public SimpleEventEmitter<E> clear() {
    listener = null;
    return this;
  }

  public boolean hasListener() {
    return listener != null;
  }

  public @Nullable Listener<E> getListener() {
    return listener;
  }

  public SimpleEventEmitter<E> once(Listener.NoArg<E> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  public SimpleEventEmitter<E> once(Listener<E> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  public SimpleEventEmitter<E> on(Listener.NoArg<E> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  public SimpleEventEmitter<E> on(Listener<E> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  public SimpleEventEmitter<E> off() {
    listener = null;
    return this;
  }

  public Listener<E> listener(Listener.NoArg<E> listener) {
    return listener;
  }

  public void emit() {
    emit(null);
  }

  public void emit(@Nullable E event) {
    if (listener != null) {
      listener.on(event);
      if (isOnce) {
        off();
      }
    }
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
