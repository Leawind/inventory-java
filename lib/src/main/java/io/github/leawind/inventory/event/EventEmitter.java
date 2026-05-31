package io.github.leawind.inventory.event;

import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A generic event emitter that supports priority-ordered listener dispatch, keyed listener
 * management, and one-time subscriptions.
 *
 * <p>Listeners are executed in descending priority order. Listeners with the same key replace each
 * other. Keyless listeners accumulate.
 *
 * @param <E> The event type
 */
public class EventEmitter<E> {
  protected static final int DEFAULT_PRIORITY = 0;

  /** Sorted by priority in descending order */
  protected final List<Subscription<E>> subscriptions = new ArrayList<>();

  /**
   * Lookup map from key to subscription.
   *
   * <p>Keyless subscriptions ({@code null} key) are not included.
   */
  protected final Map<Constable, Subscription<E>> subscriptionsByKey = new HashMap<>();

  /**
   * Removes all subscribed listeners.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> clear() {
    subscriptions.clear();
    subscriptionsByKey.clear();
    return this;
  }

  /**
   * Returns whether a listener with the given key exists.
   *
   * @param key the lookup key; always returns {@code false} if {@code null}
   */
  public boolean hasKey(Constable key) {
    return subscriptionsByKey.containsKey(key);
  }

  /**
   * Returns the listener associated with the given key, or {@code null} if not found.
   *
   * @param key the lookup key
   */
  public @Nullable Listener<E> getListener(Constable key) {
    var subscription = subscriptionsByKey.get(key);
    if (subscription == null) {
      return null;
    }
    return subscription.listener;
  }

  /**
   * Adds a one-time keyless listener with default priority. The listener is automatically removed
   * after its first invocation.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Listener.NoArg<E> listener) {
    return once((Listener<E>) listener);
  }

  /**
   * Adds a one-time keyless listener with default priority. The listener is automatically removed
   * after its first invocation.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Listener.Basic<E> listener) {
    return once((Listener<E>) listener);
  }

  /**
   * Adds a one-time keyless listener with default priority. The listener is automatically removed
   * after its first invocation.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Listener<E> listener) {
    return subscribe(new Subscription<>(null, listener, DEFAULT_PRIORITY, true));
  }

  /**
   * Sets a one-time listener identified by {@code key} with default priority. Replaces any existing
   * listener with the same key.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener.NoArg<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  /**
   * Sets a one-time listener identified by {@code key} with default priority. Replaces any existing
   * listener with the same key.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener.Basic<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  /**
   * Sets a one-time listener identified by {@code key} with default priority. Replaces any existing
   * listener with the same key.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener<E> listener) {
    return subscribe(new Subscription<>(key, listener, DEFAULT_PRIORITY, true));
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener.NoArg<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener.Basic<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Constable key, Listener<E> listener, int priority) {
    return subscribe(new Subscription<>(key, listener, priority, true));
  }

  /**
   * Adds a persistent keyless listener with default priority.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener.NoArg<E> listener) {
    return on((Listener<E>) listener);
  }

  /**
   * Adds a persistent keyless listener with default priority.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener.Basic<E> listener) {
    return on((Listener<E>) listener);
  }

  /**
   * Adds a persistent keyless listener with default priority.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener<E> listener) {
    return on(listener, DEFAULT_PRIORITY);
  }

  /**
   * Adds a persistent keyless listener with the given priority.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener.NoArg<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  /**
   * Adds a persistent keyless listener with the given priority.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener.Basic<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  /**
   * Adds a persistent keyless listener with the given priority.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Listener<E> listener, int priority) {
    return subscribe(new Subscription<>(null, listener, priority, false));
  }

  /**
   * Sets a persistent listener identified by {@code key} with default priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener.NoArg<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  /**
   * Sets a persistent listener identified by {@code key} with default priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener.Basic<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  /**
   * Sets a persistent listener identified by {@code key} with default priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener<E> listener) {
    return on(key, listener, DEFAULT_PRIORITY);
  }

  /**
   * Sets a persistent listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener.NoArg<E> listener, int priority) {
    return on(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a persistent listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener.Basic<E> listener, int priority) {
    return on(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a persistent listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Constable key, Listener<E> listener, int priority) {
    if (key == null) {
      throw new IllegalArgumentException("Listener key must not be null.");
    }
    return subscribe(new Subscription<>(key, listener, priority, false));
  }

  protected EventEmitter<E> subscribe(Subscription<E> subscription) {
    // If it has a key, replace the existing one with the same key
    if (subscription.key != null) {
      if (subscriptionsByKey.containsKey(subscription.key)) {
        off(subscription.key);
      }
      subscriptionsByKey.put(subscription.key, subscription);
    }

    // Insert it at the correct position in the list
    var it = subscriptions.listIterator();
    while (it.hasNext()) {
      if (it.next().priority < subscription.priority) {
        it.previous();
        break;
      }
    }
    it.add(subscription);

    return this;
  }

  /**
   * Removes the listener associated with the given key. Does nothing if the key is not found.
   *
   * @param key the key of the listener to remove
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> off(Constable key) {
    var subscription = this.subscriptionsByKey.remove(key);
    if (subscription != null) {
      subscriptions.remove(subscription);
    }
    return this;
  }

  /**
   * Removes the first (highest-priority) occurrence of the given listener instance. Does nothing if
   * the listener is not subscribed.
   *
   * @param listener the listener to remove
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> off(Listener<E> listener) {
    var it = subscriptions.listIterator();
    while (it.hasNext()) {
      var subscription = it.next();
      if (subscription.listener == listener) {
        it.remove();
        if (subscription.key != null) {
          subscriptionsByKey.remove(subscription.key);
        }
        break;
      }
    }
    return this;
  }

  /** Emits an event with a {@code null} payload. */
  public void emit() {
    emit(null);
  }

  /**
   * Emits an event, invoking all listeners in descending priority order. Listeners marked for
   * removal (via {@link EventControl#unsubscribe()}) are removed after execution. Propagation stops
   * if {@link EventControl#stop()} is called.
   *
   * @param event the event payload; may be {@code null}
   */
  public void emit(@Nullable E event) {
    var it = subscriptions.listIterator();
    var control = new EventControl();

    while (it.hasNext()) {
      var subscription = it.next();

      control.reset();

      if (subscription.once) {
        control.unsubscribe();
      }

      subscription.listener.on(event, control);

      if (control.markedForRemoval) {
        it.remove();
      }
      if (control.shouldStop) {
        break;
      }
    }
  }

  /** Sugar method for listener declaration */
  public Listener<E> listener(Listener.Basic<E> listener) {
    return listener;
  }

  /**
   * A listener that receives an event and an optional {@link EventControl}.
   *
   * <p>Use {@link Basic} when no propagation control is needed, or {@link Listener} to call {@code
   * stop()} / {@code unsubscribe()} during handling.
   *
   * @param <E> the event type
   */
  public interface Listener<E> {
    void on(E event, EventControl control);

    interface NoArg<E> extends Listener<E> {
      void on();

      default void on(E event, EventControl control) {
        on();
      }
    }

    interface Basic<E> extends Listener<E> {

      void on(E event);

      default void on(E event, EventControl control) {
        on(event);
      }
    }
  }

  /**
   * Controls event propagation and listener lifecycle within a single emission.
   *
   * <p>Passed to {@link Listener} during {@link EventEmitter#emit}.
   */
  public static class EventControl {

    /** Whether event propagation is going to be stopped */
    protected boolean shouldStop = false;

    /** Whether the listener should be removed after execution */
    protected boolean markedForRemoval = false;

    /**
     * Stops propagation to lower-priority listeners.
     *
     * <p>Cannot be undone once called.
     */
    public void stop() {
      shouldStop = true;
    }

    /**
     * Schedules this listener for removal after the current invocation completes.
     *
     * <p>Cannot be undone once called.
     */
    public void unsubscribe() {
      markedForRemoval = true;
    }

    protected void reset() {
      shouldStop = false;
      markedForRemoval = false;
    }
  }

  protected record Subscription<E>(
      @Nullable Constable key, Listener<E> listener, int priority, boolean once) {}
}
