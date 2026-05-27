package io.github.leawind.inventory.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A thread-safe version of {@link ValidatingHashMap} backed by a {@link ConcurrentHashMap}.
 *
 * <p>All mutating operations are synchronized to ensure that entry validation and the actual
 * modification happen atomically. Removal is assumed to never make the map invalid.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see ConcurrentHashMap
 */
public abstract class ValidatingConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

  /**
   * @see ConcurrentHashMap#ConcurrentHashMap(int, float)
   */
  public ValidatingConcurrentHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * @see ConcurrentHashMap#ConcurrentHashMap(int)
   */
  public ValidatingConcurrentHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * @see ConcurrentHashMap#ConcurrentHashMap()
   */
  public ValidatingConcurrentHashMap() {
    super();
  }

  /**
   * Creates a new map with the same mappings as the given map. The whole insertion is performed via
   * {@link #putAll(Map)} so that validation is applied and atomicity is ensured by synchronization.
   *
   * @param m the map whose mappings are to be placed in this map
   */
  public ValidatingConcurrentHashMap(Map<? extends K, ? extends V> m) {
    this();
    putAll(m);
  }

  /**
   * Validates a single entry. This method is called before an entry is inserted or updated. The
   * default implementation does nothing; subclasses are expected to override it and throw an
   * exception if the entry is considered invalid.
   *
   * <p>Should not consider the current state of this map
   *
   * @param key the key to validate
   * @param value the value to validate
   */
  public void validateEntry(K key, V value) {}

  /**
   * Validates all entries in the given map. The default implementation simply delegates to {@link
   * #validateEntry(Object, Object)} for each entry.
   *
   * <p>Should not consider the current state of `this`
   *
   * @param m the map whose entries should be validated
   */
  public void validateMap(Map<? extends K, ? extends V> m) {
    m.forEach(this::validateEntry);
  }

  @Override
  public synchronized V put(@NonNull K key, @NonNull V value) {
    validateEntry(key, value);
    return super.put(key, value);
  }

  @Override
  public synchronized void putAll(Map<? extends K, ? extends V> m) {
    HashMap<K, V> temp = new HashMap<>(this);
    temp.putAll(m);
    validateMap(temp);

    super.putAll(m);
  }

  /**
   * @see Map#putIfAbsent(Object, Object)
   */
  @Override
  public synchronized @Nullable V putIfAbsent(K key, V value) {
    V v = get(key);
    if (v == null) {
      v = put(key, value);
    }

    return v;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    validateEntry(key, newValue);
    return super.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    validateEntry(key, value);
    return super.replace(key, value);
  }

  /**
   * @see ConcurrentMap#computeIfAbsent(Object, Function)
   */
  @Override
  public synchronized @Nullable V computeIfAbsent(
      K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V v, newValue;
    return ((v = get(key)) == null
            && (newValue = mappingFunction.apply(key)) != null
            && (v = putIfAbsent(key, newValue)) == null)
        ? newValue
        : v;
  }

  /**
   * @see ConcurrentMap#computeIfPresent(Object, BiFunction)
   */
  @Override
  public synchronized @Nullable V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue;
    while ((oldValue = get(key)) != null) {
      V newValue = remappingFunction.apply(key, oldValue);
      if (newValue != null) {
        if (replace(key, oldValue, newValue)) return newValue;
      } else if (remove(key, oldValue)) return null;
    }
    return oldValue;
  }

  /**
   * @see ConcurrentMap#compute(Object, BiFunction)
   */
  @Override
  public synchronized @Nullable V compute(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);
    for (; ; ) {
      V newValue = remappingFunction.apply(key, oldValue);
      if (newValue == null) {
        if (oldValue != null || containsKey(key)) {
          if (remove(key, oldValue)) {
            return null;
          }

          oldValue = get(key);
        } else {
          return null;
        }
      } else {
        if (oldValue != null) {
          if (replace(key, oldValue, newValue)) {
            return newValue;
          }

          oldValue = get(key);
        } else {
          if ((oldValue = putIfAbsent(key, newValue)) == null) {
            return newValue;
          }
        }
      }
    }
  }
}
