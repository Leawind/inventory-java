package io.github.leawind.inventory.delegate;

import java.lang.constant.Constable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Event delegation system with prioritized listener execution
 *
 * @param <D> Type of event data
 */
public class Delegate<D> {
  private static final int DEFAULT_PRIORITY = 0;

  /** Delegate name */
  public String name;

  /** Event handlers from high to low priority */
  protected final List<Handler> handlers = new LinkedList<>();

  /**
   * Map: key => handler
   *
   * <p>This map is used to quickly look up a handler by its key. Handlers with `null` key are not
   * in this map
   */
  private final Map<Constable, Handler> key2handlerMap = new HashMap<>();

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
  public Delegate<D> clear() {
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
  public @Nullable Consumer<Event> getListener(Constable key) {
    var handler = key2handlerMap.get(key);
    if (handler == null) {
      return null;
    }
    return handler.listener;
  }

  /** Add a listener that will be executed once and then removed */
  public Delegate<D> addOnce(Consumer<Event> listener) {
    return addHandler(new Handler(null, listener, DEFAULT_PRIORITY, true));
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public Delegate<D> setOnce(Constable key, Consumer<Event> listener) {
    return addHandler(new Handler(key, listener, DEFAULT_PRIORITY, true));
  }

  /** Set a one-time listener with a key (replaces existing if key exists) */
  public Delegate<D> setOnce(Constable key, Consumer<Event> listener, int priority) {
    return addHandler(new Handler(key, listener, priority, true));
  }

  /** Add a new listener with {@link Delegate#DEFAULT_PRIORITY} */
  public Delegate<D> addListener(Consumer<Event> listener) {
    return addListener(listener, DEFAULT_PRIORITY);
  }

  /** Add a listener with the specified priority */
  public Delegate<D> addListener(Consumer<Event> listener, int priority) {
    return addHandler(new Handler(null, listener, priority, false));
  }

  /**
   * Set a listener with a key (replaces existing if key exists), use default priority
   *
   * @see Delegate#setListener(Constable, Consumer,int)
   */
  public Delegate<D> setListener(Constable key, Consumer<Event> listener) {
    return setListener(key, listener, DEFAULT_PRIORITY);
  }

  /**
   * Set a listener with a key (replaces existing if key exists)
   *
   * @param key Unique key for the listener
   * @param listener The listener function
   * @param priority Priority of the listener, higher executes first
   */
  public Delegate<D> setListener(Constable key, Consumer<Event> listener, int priority) {
    if (key == null) {
      throw new IllegalArgumentException("Listener key must not be null.");
    }
    return addHandler(new Handler(key, listener, priority, false));
  }

  /**
   * Internal method to add a handler to the list in the correct position based on priority
   *
   * <p>If key is specified in given handler, it replaces the existing handler with the same key
   *
   * @param handler The handler to add
   */
  protected Delegate<D> addHandler(Handler handler) {
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
  public Delegate<D> removeListener(Constable key) {
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
  public Delegate<D> removeListener(Consumer<Event> listener) {
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

  /**
   * Broadcasts data to listeners
   *
   * <p>Execution order: High -> Low priority
   *
   * @param data The data to broadcast
   */
  public void broadcast(D data) {
    var it = handlers.listIterator();
    while (it.hasNext()) {
      var handler = it.next();
      var event = new Event(data);

      if (handler.once) {
        event.removeSelf();
      }

      handler.listener.accept(event);

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
  public Consumer<Event> listener(Consumer<Event> listener) {
    return listener;
  }

  /** Event object passed to delegate listeners */
  public class Event {

    /** Whether event propagation is going to be stopped */
    private boolean doStop = false;

    /** Whether the listener should be removed after execution */
    private boolean doRemoveSelf = false;

    /** The data associated with the event */
    public final D data;

    protected Event(D data) {
      this.data = data;
    }

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

  /** Event handler */
  protected class Handler {
    protected final @Nullable Constable key;
    protected Consumer<Event> listener;
    protected final int priority;
    protected final boolean once;

    /**
     * @param key Optional unique key for the handler
     * @param listener The listener function to be called
     * @param priority Priority of the handler (higher executes first)
     * @param once Whether the handler is one-time
     * @throws NullPointerException if listener is null
     */
    protected Handler(@Nullable Constable key, Consumer<Event> listener, int priority, boolean once)
        throws NullPointerException {
      this.key = key;
      this.listener = Objects.requireNonNull(listener);
      this.priority = priority;
      this.once = once;
    }
  }
}
