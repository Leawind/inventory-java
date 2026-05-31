package io.github.leawind.inventory.event;

import io.github.leawind.inventory.type.UnsafeTypeUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A generic event emitter that supports priority-ordered listener dispatch, keyed listener
 * management, and one-time subscriptions.
 *
 * <p>Listeners are executed in descending priority order. Listeners with the same key replace each
 * other. Keyless listeners accumulate.
 *
 * <p><strong>Memory management note:</strong> When a non-null key is provided, it is held via a
 * {@link WeakReference}. This allows the key object to be garbage collected when no longer strongly
 * reachable elsewhere. Once a key is collected, its associated subscription is automatically
 * removed during the next {@link #emit()} call.
 *
 * <p><strong>Key equality:</strong> If using a custom map with reference-based equality (e.g.,
 * Guava's {@code MapMaker().weakKeys()}), ensure that {@code on(key, ...)} and {@code off(key)} use
 * the exact same key instance (reference equality {@code ==}).
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
   *
   * <p>This map may be customized via constructor to support weak keys, concurrent access, etc.
   */
  protected final Map<Object, Subscription<E>> subscriptionsByKey;

  /** Creates an EventEmitter with default HashMap for key-based lookup. */
  public EventEmitter() {
    this(new HashMap<>());
  }

  /**
   * Creates an EventEmitter with a custom map for key-based subscription lookup.
   *
   * <p><strong>Requirements for the custom map:</strong>
   *
   * <ul>
   *   <li>Must be empty upon construction
   *   <li>Should support {@code null} keys only if you intend to use keyless listeners (though
   *       keyless subscriptions are not stored in this map)
   *   <li>Should be thread-safe if {@code EventEmitter} is accessed concurrently (this class itself
   *       is not thread-safe)
   *   <li>If using reference-based equality (e.g., Guava's {@code weakKeys()}), ensure {@code
   *       on(key, ...)} and {@code off(key)} use the same key instance
   * </ul>
   *
   * <p><strong>Example with Guava weak keys:</strong>
   *
   * <pre>{@code
   * Map<Object, Subscription<?>> weakKeyMap = new MapMaker()
   *     .weakKeys()
   *     .makeMap();
   * EventEmitter<MyEvent> emitter = new EventEmitter<>(weakKeyMap);
   * }</pre>
   *
   * @param subscriptionsMap the map instance to use for key → subscription lookup
   */
  public EventEmitter(Map<Object, ?> subscriptionsMap) {
    if (!subscriptionsMap.isEmpty()) {
      throw new IllegalArgumentException("subscriptionsMap must be empty");
    }
    this.subscriptionsByKey = UnsafeTypeUtils.forceCast(subscriptionsMap);
  }

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
  public boolean hasKey(Object key) {
    return subscriptionsByKey.containsKey(key);
  }

  /**
   * Returns the listener associated with the given key, or {@code null} if not found.
   *
   * @param key the lookup key
   */
  public @Nullable Listener<E> getListener(Object key) {
    Subscription<E> subscription = subscriptionsByKey.get(key);
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
  public EventEmitter<E> once(Object key, Listener.NoArg<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  /**
   * Sets a one-time listener identified by {@code key} with default priority. Replaces any existing
   * listener with the same key.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Object key, Listener.Basic<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  /**
   * Sets a one-time listener identified by {@code key} with default priority. Replaces any existing
   * listener with the same key.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Object key, Listener<E> listener) {
    return subscribe(new Subscription<>(key, listener, DEFAULT_PRIORITY, true));
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Object key, Listener.NoArg<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Object key, Listener.Basic<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  /**
   * Sets a one-time listener identified by {@code key} with the given priority. Replaces any
   * existing listener with the same key.
   *
   * @param priority higher value executes first
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> once(Object key, Listener<E> listener, int priority) {
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
  public EventEmitter<E> on(Object key, Listener.NoArg<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  /**
   * Sets a persistent listener identified by {@code key} with default priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Object key, Listener.Basic<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  /**
   * Sets a persistent listener identified by {@code key} with default priority. Replaces any
   * existing listener with the same key.
   *
   * @param key unique, non-null identifier for the listener
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> on(Object key, Listener<E> listener) {
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
  public EventEmitter<E> on(Object key, Listener.NoArg<E> listener, int priority) {
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
  public EventEmitter<E> on(Object key, Listener.Basic<E> listener, int priority) {
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
  public EventEmitter<E> on(Object key, Listener<E> listener, int priority) {
    if (key == null) {
      throw new IllegalArgumentException("Listener key must not be null.");
    }
    return subscribe(new Subscription<>(key, listener, priority, false));
  }

  protected EventEmitter<E> subscribe(Subscription<E> subscription) {
    // Clean up any dead-key subscriptions first
    cleanupDeadKeySubscriptions();

    // If it has a key, replace the existing one with the same key
    Object key = subscription.getKey();
    if (key != null) {
      if (subscriptionsByKey.containsKey(key)) {
        off(key);
      }
      subscriptionsByKey.put(key, subscription);
    }

    // Insert it at the correct position in the list
    ListIterator<Subscription<E>> it = subscriptions.listIterator();
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
  public EventEmitter<E> off(Object key) {
    Subscription<E> subscription = this.subscriptionsByKey.remove(key);
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
    ListIterator<Subscription<E>> it = subscriptions.listIterator();
    while (it.hasNext()) {
      Subscription<E> subscription = it.next();
      if (subscription.listener == listener) {
        it.remove();
        Object key = subscription.getKey();
        if (key != null) {
          subscriptionsByKey.remove(key);
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
   * <p>Subscriptions whose weak key has been garbage collected are automatically removed before
   * dispatch begins.
   *
   * @param event the event payload; may be {@code null}
   */
  public void emit(@Nullable E event) {
    // Clean up subscriptions with collected keys before dispatch
    cleanupDeadKeySubscriptions();

    ListIterator<Subscription<E>> it = subscriptions.listIterator();
    EventControl control = new EventControl();

    while (it.hasNext()) {
      Subscription<E> subscription = it.next();

      // Skip if key was collected between cleanup and now (defensive)
      if (subscription.keyRef != null && subscription.getKey() == null) {
        it.remove();
        continue;
      }

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

  private void cleanupDeadKeySubscriptions() {
    // Remove from list
    subscriptions.removeIf(sub -> sub.keyRef != null && sub.getKey() == null);

    // Remove from map (entries with collected keys)
    subscriptionsByKey
        .entrySet()
        .removeIf(entry -> entry.getValue().keyRef != null && entry.getValue().getKey() == null);
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

  protected static final class Subscription<E> {
    /**
     * Weak reference to the key, or null for keyless subscriptions. The key itself is not strongly
     * held by the Subscription.
     */
    @Nullable final WeakReference<Object> keyRef;

    final Listener<E> listener;
    final int priority;
    final boolean once;

    /**
     * @param key the key object; if non-null, will be held weakly
     */
    Subscription(@Nullable Object key, Listener<E> listener, int priority, boolean once) {
      this.keyRef = (key == null) ? null : new WeakReference<>(key);
      this.listener = listener;
      this.priority = priority;
      this.once = once;
    }

    /** Returns the key if still reachable, or null if collected / keyless. */
    @Nullable Object getKey() {
      return keyRef == null ? null : keyRef.get();
    }
  }
}
