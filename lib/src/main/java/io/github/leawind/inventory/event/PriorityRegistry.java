package io.github.leawind.inventory.event;

import io.github.leawind.inventory.type.UnsafeTypeUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public class PriorityRegistry<T> {
  protected final List<Entry<T>> list = new ArrayList<>();

  protected final Map<Object, Entry<T>> byKey;

  public PriorityRegistry() {
    this(new HashMap<>());
  }

  public PriorityRegistry(Map<Object, ?> map) {
    if (!map.isEmpty()) {
      throw new IllegalArgumentException("map must be empty");
    }
    this.byKey = UnsafeTypeUtils.forceCast(map);
  }

  public PriorityRegistry<T> clear() {
    list.clear();
    byKey.clear();
    return this;
  }

  public boolean hasKey(Object key) {
    return byKey.containsKey(key);
  }

  public @Nullable T get(Object key) {
    Entry<T> entry = byKey.get(key);
    if (entry == null) {
      return null;
    }
    return entry.value();
  }

  public void removeKey(Object key) {
    Entry<T> entry = byKey.remove(key);
    if (entry != null) {
      list.remove(entry);
    }
  }

  public void removeValue(T value) {
    ListIterator<Entry<T>> it = list.listIterator();
    while (it.hasNext()) {
      Entry<T> entry = it.next();
      if (entry.value() == value) {
        it.remove();
        Object key = entry.getKey();
        if (key != null) {
          byKey.remove(key);
        }
        break;
      }
    }
  }

  public void removeEntry(Entry<T> entry) {
    Object key = entry.getKey();
    if (key != null) {
      byKey.remove(key);
    }
  }

  public void removeIf(Predicate<Entry<T>> predicate) {
    list.removeIf(predicate);
    byKey.values().removeIf(predicate);
  }

  protected PriorityRegistry<T> add(Entry<T> entry) {
    // Clean up any dead-key entries first
    cleanupDeadKeyEntries();

    // If it has a key, replace the existing one with the same key
    Object key = entry.getKey();
    if (key != null) {
      if (byKey.containsKey(key)) {
        removeKey(key);
      }
      byKey.put(key, entry);
    }

    // Insert it at the correct position in the list
    ListIterator<Entry<T>> it = list.listIterator();
    while (it.hasNext()) {
      if (it.next().priority < entry.priority) {
        it.previous();
        break;
      }
    }
    it.add(entry);

    return this;
  }

  public ListIterator<Entry<T>> listIterator() {
    return list.listIterator();
  }

  private void cleanupDeadKeyEntries() {
    // Remove from list
    list.removeIf(sub -> sub.keyRef != null && sub.getKey() == null);

    // Remove from map (entries with collected keys)
    byKey
        .entrySet()
        .removeIf(entry -> entry.getValue().keyRef != null && entry.getValue().getKey() == null);
  }

  public record Entry<T>(@Nullable WeakReference<Object> keyRef, int priority, T value) {
    @Nullable Object getKey() {
      return keyRef == null ? null : keyRef.get();
    }
  }
}
