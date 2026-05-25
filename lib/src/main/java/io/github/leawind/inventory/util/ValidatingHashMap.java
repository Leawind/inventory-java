package io.github.leawind.inventory.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A HashMap that validates the entries before adding them.
 *
 * <p>It assumes removing any entry would not make it invalid.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Map
 * @see HashMap
 */
public abstract class ValidatingHashMap<K, V> extends HashMap<K, V> {

  /**
   * @see HashMap#HashMap(int, float)
   */
  public ValidatingHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * @see HashMap#HashMap(int)
   */
  public ValidatingHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * @see HashMap#HashMap()
   */
  public ValidatingHashMap() {
    super();
  }

  /**
   * @see HashMap#HashMap(Map)
   */
  public ValidatingHashMap(Map<? extends K, ? extends V> m) {
    super(m);
    validateMap(m);
  }

  public void validateEntry(K key, V value) {}

  public void validateMap(Map<? extends K, ? extends V> m) {
    m.forEach(this::validateEntry);
  }

  @Override
  public V put(K key, V value) {
    validateEntry(key, value);
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    HashMap<K, V> tempMap = new HashMap<>(this);
    tempMap.putAll(m);
    validateMap(tempMap);

    super.putAll(m);
  }

  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    V v = get(key);
    if (v == null) {
      v = put(key, value);
    }

    return v;
  }

  @Override
  public @Nullable V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V v;
    if ((v = get(key)) == null) {
      V newValue;
      if ((newValue = mappingFunction.apply(key)) != null) {
        put(key, newValue);
        return newValue;
      }
    }

    return v;
  }

  @Override
  public @Nullable V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue;
    if ((oldValue = get(key)) != null) {
      V newValue = remappingFunction.apply(key, oldValue);
      if (newValue != null) {
        put(key, newValue);
        return newValue;
      } else {
        remove(key);
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public @Nullable V compute(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);

    V newValue = remappingFunction.apply(key, oldValue);
    if (newValue == null) {
      if (oldValue != null || containsKey(key)) {
        remove(key);
      }
      return null;
    } else {
      put(key, newValue);
      return newValue;
    }
  }
}
