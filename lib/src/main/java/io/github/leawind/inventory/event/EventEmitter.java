package io.github.leawind.inventory.event;

import java.lang.constant.Constable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class EventEmitter<E> {
  protected static final int DEFAULT_PRIORITY = 0;

  /** Sorted by priority in descending order */
  protected final List<Subscription<E>> subscriptions = new LinkedList<>();

  /**
   * Map: key => subscription
   *
   * <p>This map is used to look up a subscription by its key. Subscriptions with `null` key are not
   * in this map
   */
  protected final Map<Constable, Subscription<E>> subscriptionsByKey = new HashMap<>();

  public EventEmitter() {}

  /** Remove all listeners */
  public EventEmitter<E> clear() {
    subscriptions.clear();
    subscriptionsByKey.clear();
    return this;
  }

  /**
   * Check if a listener with the given key exists
   *
   * @param key The key of the listener. If `null`, it always return false
   * @return true if the listener exists, false otherwise
   */
  public boolean hasKey(Constable key) {
    return subscriptionsByKey.containsKey(key);
  }

  /**
   * Retrieves the listener associated with the given key
   *
   * <p>If the given key is `null`, it returns `null`
   *
   * @param key The key of the listener
   * @return The listener if found, or `null` otherwise
   */
  public @Nullable Listener<E> getListener(Constable key) {
    var subscription = subscriptionsByKey.get(key);
    if (subscription == null) {
      return null;
    }
    return subscription.listener;
  }

  public EventEmitter<E> once(Listener.Simple<E> listener) {
    return once((Listener<E>) listener);
  }

  public EventEmitter<E> once(Listener.Controlled<E> listener) {
    return once((Listener<E>) listener);
  }

  /** Add a listener that will be executed once and then removed */
  public EventEmitter<E> once(Listener<E> listener) {
    return subscribe(new Subscription<>(null, listener, DEFAULT_PRIORITY, true));
  }

  public EventEmitter<E> once(Constable key, Listener.Simple<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  public EventEmitter<E> once(Constable key, Listener.Controlled<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public EventEmitter<E> once(Constable key, Listener<E> listener) {
    return subscribe(new Subscription<>(key, listener, DEFAULT_PRIORITY, true));
  }

  public EventEmitter<E> once(Constable key, Listener.Simple<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  public EventEmitter<E> once(Constable key, Listener.Controlled<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public EventEmitter<E> once(Constable key, Listener<E> listener, int priority) {
    return subscribe(new Subscription<>(key, listener, priority, true));
  }

  public EventEmitter<E> on(Listener.Simple<E> listener) {
    return on((Listener<E>) listener);
  }

  public EventEmitter<E> on(Listener.Controlled<E> listener) {
    return on((Listener<E>) listener);
  }

  /** Add a new listener with {@link EventEmitter#DEFAULT_PRIORITY} */
  public EventEmitter<E> on(Listener<E> listener) {
    return on(listener, DEFAULT_PRIORITY);
  }

  public EventEmitter<E> on(Listener.Simple<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  public EventEmitter<E> on(Listener.Controlled<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  /** Add a listener with the specified priority */
  public EventEmitter<E> on(Listener<E> listener, int priority) {
    return subscribe(new Subscription<>(null, listener, priority, false));
  }

  public EventEmitter<E> on(Constable key, Listener.Simple<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  public EventEmitter<E> on(Constable key, Listener.Controlled<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  /** Set a listener with a key (replaces existing if key exists), use default priority */
  public EventEmitter<E> on(Constable key, Listener<E> listener) {
    return on(key, listener, DEFAULT_PRIORITY);
  }

  public EventEmitter<E> on(Constable key, Listener.Simple<E> listener, int priority) {
    return on(key, (Listener<E>) listener, priority);
  }

  public EventEmitter<E> on(Constable key, Listener.Controlled<E> listener, int priority) {
    return on(key, (Listener<E>) listener, priority);
  }

  /**
   * Set a listener with a key (replaces existing if key exists)
   *
   * @param key Unique key for the listener
   * @param listener The listener function
   * @param priority Priority of the listener, higher executes first
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
   * Remove listeners by key
   *
   * <p>If the key doesn't exist, it does nothing
   *
   * @param key The key of the listener to remove.
   */
  public EventEmitter<E> off(Constable key) {
    var subscription = this.subscriptionsByKey.remove(key);
    if (subscription != null) {
      subscriptions.remove(subscription);
    }
    return this;
  }

  /**
   * Removes first occurrence of listener
   *
   * <p>Only removes highest-priority instance if duplicate
   *
   * @param listener The listener to remove.
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

  public void emit() {
    emit(null);
  }

  /**
   * Emits an event
   *
   * <p>Execution order: High -> Low priority
   *
   * @param event The event object
   */
  public void emit(@Nullable E event) {
    var it = subscriptions.listIterator();
    while (it.hasNext()) {
      var subscription = it.next();
      var control = new EventControl();

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

  /** Syntax sugar for listener declaration */
  public Listener<E> listener(Listener.Simple<E> listener) {
    return listener;
  }

  public Listener<E> listener(Listener.Controlled<E> listener) {
    return listener;
  }

  public sealed interface Listener<E> permits Listener.Simple, Listener.Controlled {
    void on(E event);

    void on(E event, EventControl control);

    non-sealed interface Simple<E> extends Listener<E> {
      default void on(E event, EventControl control) {
        on(event);
      }
    }

    non-sealed interface Controlled<E> extends Listener<E> {
      default void on(E event) {
        on(event, new EventControl());
      }
    }
  }

  public static class EventControl {

    /** Whether event propagation is going to be stopped */
    protected boolean shouldStop = false;

    /** Whether the listener should be removed after execution */
    protected boolean markedForRemoval = false;

    /**
     * Stops event propagation
     *
     * <ul>
     *   <li>It can be invoked multiple times in a listener
     *   <li>The effect can't be canceled once invoked
     * </ul>
     */
    public void stop() {
      shouldStop = true;
    }

    /**
     * Schedules self-removal after execution
     *
     * <ul>
     *   <li>It can be invoked multiple times in a listener
     *   <li>The effect can't be canceled once invoked
     * </ul>
     */
    public void unsubscribe() {
      markedForRemoval = true;
    }
  }

  protected record Subscription<E>(
      @Nullable Constable key, Listener<E> listener, int priority, boolean once) {}
}
