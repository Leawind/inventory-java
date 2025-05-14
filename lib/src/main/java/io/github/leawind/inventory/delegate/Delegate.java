package io.github.leawind.inventory.delegate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
  private final Map<Object, Handler> key2handlerMap = new HashMap<>();

  /** Creates a unnamed delegate */
  public Delegate() {
    this("Unnamed");
  }

  public Delegate(String name) {
    this.name = name;
  }

  /** Remove all listeners */
  public Delegate<D> clear() {
    handlers.clear();
    return this;
  }

  /**
   * Add a event handler
   *
   * <p>The given handler will be inserted at the appropriate position based on its priority.
   *
   * @param handler The handler to add
   */
  protected Delegate<D> addHandler(Handler handler) {
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
   * Check if a listener with the given key exists
   *
   * @param key The key of the listener. If `null`, it always return false
   * @return true if the listener exists, false otherwise
   */
  public boolean containsListener(Object key) {
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
  public @Nullable Consumer<Event> getListener(Object key) {
    var handler = key2handlerMap.get(key);
    if (handler == null) {
      return null;
    }
    return handler.listener;
  }

  /** Add a listener that will be executed once and then removed */
  public Delegate<D> once(Consumer<Event> listener) {
    return addListener(
        (e) -> {
          listener.accept(e);
          e.removeSelf();
        });
  }

  /** Add a listener that will be executed once and then removed */
  public Delegate<D> once(Object key, Consumer<Event> listener) {
    return addListener(
        key,
        (e) -> {
          listener.accept(e);
          e.removeSelf();
        });
  }

  /** Add a new listener with {@link Delegate#DEFAULT_PRIORITY} */
  public Delegate<D> addListener(Consumer<Event> listener) {
    return addListener(listener, DEFAULT_PRIORITY);
  }

  /** Add a listener with the specified priority */
  public Delegate<D> addListener(Consumer<Event> listener, int priority) {
    return addHandler(new Handler(null, listener, priority));
  }

  /**
   * Add a listener with the specified key.
   *
   * @see Delegate#addListener(Object, Consumer,int)
   */
  public Delegate<D> addListener(Object key, Consumer<Event> listener) {
    return addListener(key, listener, DEFAULT_PRIORITY);
  }

  /**
   * Add a listener with the specified key and priority
   *
   * <p>The key is used to identify the listener and can be used to remove the listener later.
   *
   * <p>If the key is `null`, no old listener will be removed, and a new listener will be added.
   *
   * <p>If the key is not `null`, and a handler with the same key already exists, the listener will
   * be updated.
   *
   * @param key The key of the listener
   * @param listener The callback to invoke
   * @param priority The priority of the listener (higher values execute first)
   * @return self for chaining
   */
  public Delegate<D> addListener(Object key, Consumer<Event> listener, int priority) {
    if (key == null) {
      throw new IllegalArgumentException("Listener key must not be null.");
    }

    var handler = key2handlerMap.get(key);

    if (handler != null) {
      // If a handler with the same key already exists, update its listener
      handler.listener = listener;

      // Their may be some other handlers with same priority were added after this old handler was
      // added. We should make sure this new listener is executed after the old handler. So we
      // should move it right after the old handler. To do that, we remove the old handler from
      // the list and add it back later
      handlers.remove(handler);
    } else {
      // It's a new key, create a new handler and add it to the key map
      handler = new Handler(key, listener, priority);
      key2handlerMap.put(key, handler);
    }
    addHandler(handler);

    return this;
  }

  /**
   * Removes a listener by key
   *
   * @param key The key of the listener to remove
   * @return self for chaining
   */
  public Delegate<D> removeListener(Object key) {
    var handler = this.key2handlerMap.get(key);
    if (handler != null) {
      handlers.remove(handler);
    }
    return this;
  }

  /**
   * Removes the first occurrence of a listener Only removes the highest-priority instance if
   * duplicate
   *
   * @param listener The callback to remove
   * @return self for chaining
   */
  public Delegate<D> removeListener(Consumer<Event> listener) {
    var it = handlers.listIterator();
    while (it.hasNext()) {
      if (it.next().listener == listener) {
        it.remove();
        break;
      }
    }
    return this;
  }

  /**
   * Broadcasts data to all listeners Execution order: High -> Low priority
   *
   * @param data The data to broadcast
   * @throws NullPointerException if data is null
   */
  public void broadcast(D data) {
    var it = handlers.listIterator();
    while (it.hasNext()) {
      var event = new Event(data);

      it.next().listener.accept(event);

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

  /** Event execution context */
  public class Event {
    private boolean doStop = false;
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
    protected final Object key;
    protected Consumer<Event> listener;
    protected final int priority;

    /**
     * @param key The key object to identify the listener
     * @param listener The callback to invoke
     * @param priority The priority of the listener (higher values execute first)
     */
    protected Handler(Object key, Consumer<Event> listener, int priority) {
      this.key = key;
      this.listener = listener;
      this.priority = priority;
    }
  }
}
