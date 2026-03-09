package io.github.leawind.inventory.delegate;

import java.lang.constant.Constable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Event delegation system with prioritized listener execution
 *
 * @param <E> Type of event data
 */
public class Delegate<E> {
  private static final int DEFAULT_PRIORITY = 0;

  /** Delegate name */
  public String name;

  /** Event handlers from high to low priority */
  protected final List<Handler<E>> handlers = new LinkedList<>();

  /**
   * Map: key => handler
   *
   * <p>This map is used to quickly look up a handler by its key. Handlers with `null` key are not
   * in this map
   */
  private final Map<Constable, Handler<E>> key2handlerMap = new HashMap<>();

  /** Creates an unnamed delegate */
  public Delegate() {
    this("Unnamed");
  }

  /**
   * Creates a delegate with name
   *
   * @param name Delegate name for debugging
   */
  public Delegate(String name) {
    this.name = name;
  }

  /** Remove all listeners */
  public Delegate<E> clear() {
    handlers.clear();
    key2handlerMap.clear();
    return this;
  }

  /**
   * Check if a listener with the given key exists
   *
   * @param key The key of the listener. If `null`, it always return false
   * @return true if the listener exists, false otherwise
   */
  public boolean containsListener(Constable key) {
    return key2handlerMap.containsKey(key);
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
    var handler = key2handlerMap.get(key);
    if (handler == null) {
      return null;
    }
    return handler.listener;
  }

  public Delegate<E> addOnce(Listener.Uni<E> listener) {
    return addOnce((Listener<E>) listener);
  }

  public Delegate<E> addOnce(Listener.Bi<E> listener) {
    return addOnce((Listener<E>) listener);
  }

  /** Add a listener that will be executed once and then removed */
  public Delegate<E> addOnce(Listener<E> listener) {
    return addHandler(new Handler<>(null, listener, DEFAULT_PRIORITY, true));
  }

  public Delegate<E> setOnce(Constable key, Listener.Uni<E> listener) {
    return setOnce(key, (Listener<E>) listener);
  }

  public Delegate<E> setOnce(Constable key, Listener.Bi<E> listener) {
    return setOnce(key, (Listener<E>) listener);
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public Delegate<E> setOnce(Constable key, Listener<E> listener) {
    return addHandler(new Handler<>(key, listener, DEFAULT_PRIORITY, true));
  }

  public Delegate<E> setOnce(Constable key, Listener.Uni<E> listener, int priority) {
    return setOnce(key, (Listener<E>) listener, priority);
  }

  public Delegate<E> setOnce(Constable key, Listener.Bi<E> listener, int priority) {
    return setOnce(key, (Listener<E>) listener, priority);
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public Delegate<E> setOnce(Constable key, Listener<E> listener, int priority) {
    return addHandler(new Handler<>(key, listener, priority, true));
  }

  public Delegate<E> addListener(Listener.Uni<E> listener) {
    return addListener((Listener<E>) listener);
  }

  public Delegate<E> addListener(Listener.Bi<E> listener) {
    return addListener((Listener<E>) listener);
  }

  /** Add a new listener with {@link Delegate#DEFAULT_PRIORITY} */
  public Delegate<E> addListener(Listener<E> listener) {
    return addListener(listener, DEFAULT_PRIORITY);
  }

  public Delegate<E> addListener(Listener.Uni<E> listener, int priority) {
    return addListener((Listener<E>) listener, priority);
  }

  public Delegate<E> addListener(Listener.Bi<E> listener, int priority) {
    return addListener((Listener<E>) listener, priority);
  }

  /** Add a listener with the specified priority */
  public Delegate<E> addListener(Listener<E> listener, int priority) {
    return addHandler(new Handler<>(null, listener, priority, false));
  }

  public Delegate<E> setListener(Constable key, Listener.Uni<E> listener) {
    return setListener(key, (Listener<E>) listener);
  }

  public Delegate<E> setListener(Constable key, Listener.Bi<E> listener) {
    return setListener(key, (Listener<E>) listener);
  }

  /** Set a listener with a key (replaces existing if key exists), use default priority */
  public Delegate<E> setListener(Constable key, Listener<E> listener) {
    return setListener(key, listener, DEFAULT_PRIORITY);
  }

  public Delegate<E> setListener(Constable key, Listener.Uni<E> listener, int priority) {
    return setListener(key, (Listener<E>) listener, priority);
  }

  public Delegate<E> setListener(Constable key, Listener.Bi<E> listener, int priority) {
    return setListener(key, (Listener<E>) listener, priority);
  }

  /**
   * Set a listener with a key (replaces existing if key exists)
   *
   * @param key Unique key for the listener
   * @param listener The listener function
   * @param priority Priority of the listener, higher executes first
   */
  public Delegate<E> setListener(Constable key, Listener<E> listener, int priority) {
    if (key == null) {
      throw new IllegalArgumentException("Listener key must not be null.");
    }
    return addHandler(new Handler<>(key, listener, priority, false));
  }

  /**
   * Internal method to add a handler to the list in the correct position based on priority
   *
   * <p>If key is specified in given handler, it replaces the existing handler with the same key
   *
   * @param handler The handler to add
   */
  protected Delegate<E> addHandler(Handler<E> handler) {
    // If the handler has a key, replace the existing handler with the same key
    if (handler.key != null) {
      if (key2handlerMap.containsKey(handler.key)) {
        removeListener(handler.key);
      }
      key2handlerMap.put(handler.key, handler);
    }

    // Insert the handler at the correct position in the list
    var it = handlers.listIterator();
    while (it.hasNext()) {
      if (it.next().priority < handler.priority) {
        it.previous();
        break;
      }
    }
    it.add(handler);

    return this;
  }

  /**
   * Remove listeners by key
   *
   * <p>If the key doesn't exist, it does nothing
   *
   * @param key The key of the listener to remove.
   */
  public Delegate<E> removeListener(Constable key) {
    var handler = this.key2handlerMap.remove(key);
    if (handler != null) {
      handlers.remove(handler);
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
  public Delegate<E> removeListener(Listener<E> listener) {
    var it = handlers.listIterator();
    while (it.hasNext()) {
      var handler = it.next();
      if (handler.listener == listener) {
        it.remove();
        if (handler.key != null) {
          key2handlerMap.remove(handler.key);
        }
        break;
      }
    }
    return this;
  }

  public void broadcast() {
    broadcast(null);
  }

  /**
   * Broadcasts data to listeners
   *
   * <p>Execution order: High -> Low priority
   *
   * @param data The data to broadcast
   */
  public void broadcast(@Nullable E data) {
    var it = handlers.listIterator();
    while (it.hasNext()) {
      var handler = it.next();
      var event = new EventControl();

      if (handler.once) {
        event.removeSelf();
      }

      handler.listener.on(data, event);

      if (event.doRemoveSelf) {
        it.remove();
      }
      if (event.doStop) {
        break;
      }
    }
  }

  /**
   * Syntax sugar for listener declaration
   *
   * <p>Example:
   *
   * <pre>
   * {@code var listener = delegate.listener(e -> s.append(e.data));}
   * </pre>
   *
   * <p>equals to:
   *
   * <pre>
   * {@code Consumer<Delegate.Event<String>> listener = e -> s.append(e.data);}
   * </pre>
   */
  public Listener<E> listener(Listener.Uni<E> listener) {
    return listener;
  }

  public Listener<E> listener(Listener.Bi<E> listener) {
    return listener;
  }

  public sealed interface Listener<E> permits Listener.Uni, Listener.Bi {
    void on(E event);

    void on(E event, EventControl ctrl);

    non-sealed interface Uni<E> extends Listener<E> {
      default void on(E event, EventControl ctrl) {
        on(event);
      }
    }

    non-sealed interface Bi<E> extends Listener<E> {
      default void on(E event) {
        on(event, new EventControl());
      }
    }
  }

  /** Event object passed to delegate listeners */
  public static class EventControl {

    /** Whether event propagation is going to be stopped */
    private boolean doStop = false;

    /** Whether the listener should be removed after execution */
    private boolean doRemoveSelf = false;

    /**
     * Stops event propagation
     *
     * <ul>
     *   <li>It can be invoked multiple times in a listener
     *   <li>The effect can't be canceled once invoked
     * </ul>
     */
    public void stop() {
      doStop = true;
    }

    /**
     * Schedules self-removal after execution
     *
     * <ul>
     *   <li>It can be invoked multiple times in a listener
     *   <li>The effect can't be canceled once invoked
     * </ul>
     */
    public void removeSelf() {
      doRemoveSelf = true;
    }
  }

  protected record Handler<E>(
      @Nullable Constable key, Listener<E> listener, int priority, boolean once) {}
}
