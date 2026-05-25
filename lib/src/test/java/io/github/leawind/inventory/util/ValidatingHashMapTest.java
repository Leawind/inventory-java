package io.github.leawind.inventory.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class ValidatingHashMapTest {

  static class NonNullValueMap extends ValidatingHashMap<String, Integer> {
    @Override
    public void validateEntry(String k, Integer v) {
      Objects.requireNonNull(v);
    }
  }

  static class UniqueMap<K, V> extends ValidatingHashMap<K, V> {

    @Override
    public void validateMap(Map<? extends K, ? extends V> m) {
      super.validateMap(m);

      if (m.values().stream().distinct().count() != m.size()) {
        throw new IllegalArgumentException("Not unique");
      }
    }
  }

  @Test
  void test1() {
    assertThrows(NullPointerException.class, () -> new NonNullValueMap().put("Steve", null));
  }

  @Test
  void test2() {
    Map<String, Integer> map = new HashMap<>();
    map.put("a", null);
    map.put("b", 123);

    assertThrows(NullPointerException.class, () -> new NonNullValueMap().putAll(map));
  }

  @Test
  void test3() {
    Map<String, Integer> map = new HashMap<>();
    map.put("a", 123);
    map.put("b", 123);
    map.put("c", 789);

    assertThrows(
        IllegalArgumentException.class, () -> new UniqueMap<String, Integer>().putAll(map));
  }
}
