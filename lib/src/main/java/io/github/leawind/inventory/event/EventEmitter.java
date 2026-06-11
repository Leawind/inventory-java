package io.github.leawind.inventory.event;

import io.github.leawind.inventory.type.UnsafeTypeUtils;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
  private static final int DEFAULT_PRIORITY = 0;

  private final EventControl control = new EventControl();

  private Entry<E>[] snapshot = createEmptyArray();

  private boolean dirty = false;

  private final Map<Object, Entry<E>> byKey;

  private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

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
   * @param map the map instance to use for key → entry lookup
   */
  public EventEmitter(Map<Object, ?> map) {
    if (!map.isEmpty()) {
      throw new IllegalArgumentException("map must be empty");
    }
    this.byKey = UnsafeTypeUtils.forceCast(map);
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
  @SuppressWarnings("unchecked")
  public void emit(@Nullable E event) {
    {
      Object polled;
      while ((polled = refQueue.poll()) != null) {
        ((Entry<E>) polled).markRemoved();
        this.dirty = true;
      }
    }

    if (this.dirty) {
      rebuildSnapshot();
    }

    boolean needsRebuild = false;
    Entry<E>[] currentSnapshot = this.snapshot;
    for (int i = 0, len = currentSnapshot.length; i < len; i++) {
      Entry<E> entry = currentSnapshot[i];
      if (entry.isRemoved()) {
        continue;
      }

      this.control.reset();
      Subscription<E> sub = entry.subscription();

      if (sub.once) {
        entry.markRemoved();
        needsRebuild = true;
      }

      sub.listener.on(event, this.control);

      if (this.control.markedForRemoval) {
        entry.markRemoved();
        needsRebuild = true;
      }
      if (this.control.shouldStop) {
        break;
      }
    }

    if (needsRebuild) {
      rebuildSnapshot();
    }
  }

  // region on

  public EventEmitter<E> on(Listener.NoArg<E> listener) {
    return on((Listener<E>) listener);
  }

  public EventEmitter<E> on(Listener.Basic<E> listener) {
    return on((Listener<E>) listener);
  }

  public EventEmitter<E> on(Listener<E> listener) {
    return on(listener, DEFAULT_PRIORITY);
  }

  public EventEmitter<E> on(Listener.NoArg<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  public EventEmitter<E> on(Listener.Basic<E> listener, int priority) {
    return on((Listener<E>) listener, priority);
  }

  public EventEmitter<E> on(Listener<E> listener, int priority) {
    return subscribe(null, priority, listener, false);
  }

  public EventEmitter<E> on(Object key, Listener.NoArg<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  public EventEmitter<E> on(Object key, Listener.Basic<E> listener) {
    return on(key, (Listener<E>) listener);
  }

  public EventEmitter<E> on(Object key, Listener<E> listener) {
    return on(key, listener, DEFAULT_PRIORITY);
  }

  public EventEmitter<E> on(Object key, Listener.NoArg<E> listener, int priority) {
    return on(key, (Listener<E>) listener, priority);
  }

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
    return subscribe(key, priority, listener, false);
  }

  // endregion

  // region once

  public EventEmitter<E> once(Listener.NoArg<E> listener) {
    return once((Listener<E>) listener);
  }

  public EventEmitter<E> once(Listener.Basic<E> listener) {
    return once((Listener<E>) listener);
  }

  public EventEmitter<E> once(Listener<E> listener) {
    return subscribe(null, DEFAULT_PRIORITY, listener, true);
  }

  public EventEmitter<E> once(Object key, Listener.NoArg<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  public EventEmitter<E> once(Object key, Listener.Basic<E> listener) {
    return once(key, (Listener<E>) listener);
  }

  public EventEmitter<E> once(Object key, Listener<E> listener) {
    return once(key, listener, DEFAULT_PRIORITY);
  }

  public EventEmitter<E> once(Object key, Listener.NoArg<E> listener, int priority) {
    return once(key, (Listener<E>) listener, priority);
  }

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
    return subscribe(key, priority, listener, true);
  }

  // endregion

  private EventEmitter<E> subscribe(
      @Nullable Object key, int priority, Listener<E> listener, boolean once) {
    if (key != null) {
      Entry<E> old = byKey.remove(key);
      if (old != null) {
        old.markRemoved();
      }
    }

    Subscription<E> sub = new Subscription<>(listener, once);
    Entry<E> entry;
    if (key != null) {
      entry = new KeyedEntry<>(key, priority, sub, refQueue);
      byKey.put(key, entry);
    } else {
      entry = new KeylessEntry<>(priority, sub);
    }

    snapshot = appendEntry(snapshot, entry);
    dirty = true;
    return this;
  }

  /**
   * Removes the listener associated with the given key. Does nothing if the key is not found.
   *
   * @param key the key of the listener to remove
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> off(Object key) {
    Entry<E> entry = byKey.remove(key);
    if (entry != null) {
      entry.markRemoved();
      dirty = true;
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
    Entry<E>[] currentSnapshot = this.snapshot;
    for (int i = 0, len = currentSnapshot.length; i < len; i++) {
      Entry<E> entry = currentSnapshot[i];
      if (!entry.isRemoved() && entry.subscription().listener == listener) {
        entry.markRemoved();
        dirty = true;
        break;
      }
    }
    return this;
  }

  /**
   * Removes all subscribed listeners.
   *
   * @return this emitter (for chaining)
   */
  public EventEmitter<E> clear() {
    snapshot = createEmptyArray();
    byKey.clear();
    dirty = false;
    return this;
  }

  /**
   * Returns whether a listener with the given key exists.
   *
   * @param key the lookup key; always returns {@code false} if {@code null}
   */
  public boolean hasKey(Object key) {
    Entry<E> entry = byKey.get(key);
    return entry != null && !entry.isRemoved();
  }

  /**
   * Returns the listener associated with the given key, or {@code null} if not found.
   *
   * @param key the lookup key
   */
  public @Nullable Listener<E> getListener(Object key) {
    Entry<E> entry = byKey.get(key);
    if (entry == null || entry.isRemoved()) {
      return null;
    }
    return entry.subscription().listener;
  }

  /** Sugar method for listener declaration */
  public Listener<E> listener(Listener.Basic<E> listener) {
    return listener;
  }

  @SuppressWarnings("unchecked")
  private Entry<E>[] appendEntry(Entry<E>[] old, Entry<E> newEntry) {
    Entry<E>[] result = Arrays.copyOf(old, old.length + 1);
    result[old.length] = newEntry;
    return result;
  }

  @SuppressWarnings("unchecked")
  private void rebuildSnapshot() {
    Entry<E>[] oldSnapshot = this.snapshot;
    ArrayList<Entry<E>> valid = new ArrayList<>(oldSnapshot.length);
    for (int i = 0, len = oldSnapshot.length; i < len; i++) {
      Entry<E> entry = oldSnapshot[i];
      if (!entry.isRemoved()) {
        valid.add(entry);
      }
    }
    valid.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    this.snapshot = valid.toArray(createEmptyArray());
    this.dirty = false;

    Iterator<Map.Entry<Object, Entry<E>>> it = byKey.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getValue().isRemoved()) {
        it.remove();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <E> Entry<E>[] createEmptyArray() {
    return new Entry[0];
  }

  private interface Entry<E> {
    int priority();

    Subscription<E> subscription();

    boolean isRemoved();

    void markRemoved();
  }

  private static final class KeyedEntry<E> extends WeakReference<Object> implements Entry<E> {
    private final int priority;
    private final Subscription<E> subscription;
    private boolean removed;

    KeyedEntry(Object key, int priority, Subscription<E> sub, ReferenceQueue<Object> queue) {
      super(key, queue);
      this.priority = priority;
      this.subscription = sub;
    }

    @Override
    public int priority() {
      return priority;
    }

    @Override
    public Subscription<E> subscription() {
      return subscription;
    }

    @Override
    public boolean isRemoved() {
      return removed;
    }

    @Override
    public void markRemoved() {
      removed = true;
    }
  }

  private static final class KeylessEntry<E> implements Entry<E> {
    private final int priority;
    private final Subscription<E> subscription;
    private boolean removed;

    KeylessEntry(int priority, Subscription<E> sub) {
      this.priority = priority;
      this.subscription = sub;
    }

    @Override
    public int priority() {
      return priority;
    }

    @Override
    public Subscription<E> subscription() {
      return subscription;
    }

    @Override
    public boolean isRemoved() {
      return removed;
    }

    @Override
    public void markRemoved() {
      removed = true;
    }
  }

  protected static final class Subscription<E> {
    final Listener<E> listener;
    final boolean once;

    Subscription(Listener<E> listener, boolean once) {
      this.listener = listener;
      this.once = once;
    }
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
}
