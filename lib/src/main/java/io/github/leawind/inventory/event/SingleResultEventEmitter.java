package io.github.leawind.inventory.event;

import org.jspecify.annotations.Nullable;

/**
 * A simple event emitter that supports both one-time and persistent listeners.
 *
 * @param <E> the event type
 * @param <R> the result type of listener
 */
public class SingleResultEventEmitter<E, R> {
  protected boolean isOnce;
  protected @Nullable Listener<E, R> listener;

  /**
   * Clears the current listener.
   *
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> clear() {
    listener = null;
    return this;
  }

  /**
   * Checks if a listener is registered.
   *
   * @return true if a listener exists, false otherwise
   */
  public boolean hasListener() {
    return listener != null;
  }

  /**
   * Gets the current listener.
   *
   * @return the current listener, or null if none
   */
  public @Nullable Listener<E, R> getListener() {
    return listener;
  }

  /**
   * Registers a one-time listener that will be called once and then removed.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> once(Listener.NoArg<E, R> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a one-time listener that will be called once and then removed.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> once(Listener<E, R> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a persistent listener that will be called on every emit.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> on(Listener.NoArg<E, R> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a persistent listener that will be called on every emit.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> on(Listener<E, R> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  /**
   * Removes the current listener.
   *
   * @return this emitter for method chaining
   */
  public SingleResultEventEmitter<E, R> off() {
    listener = null;
    return this;
  }

  /**
   * Wraps a no-arg listener.
   *
   * @param listener the listener to wrap
   * @return the wrapped listener
   */
  public Listener<E, R> listener(Listener.NoArg<E, R> listener) {
    return listener;
  }

  /** Emits an event with null payload. */
  public R emit() {
    return emit(null);
  }

  /**
   * Emits an event to the registered listener. If the listener was registered with once(), it will
   * be removed after emission.
   *
   * @param event the event payload, may be null
   */
  public R emit(@Nullable E event) {
    if (listener != null) {
      var result = listener.on(event);
      if (isOnce) {
        off();
      }
      return result;
    }
    return null;
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
