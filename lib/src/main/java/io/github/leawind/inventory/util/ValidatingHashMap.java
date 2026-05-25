package io.github.leawind.inventory.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public abstract class ValidatingHashMap<K, V> extends HashMap<K, V> {
  public ValidatingHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public ValidatingHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public ValidatingHashMap() {
    super();
  }

  public ValidatingHashMap(Map<? extends K, ? extends V> m) {
    super(m);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
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
