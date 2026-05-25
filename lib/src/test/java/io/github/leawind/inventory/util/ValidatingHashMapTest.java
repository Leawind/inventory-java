package io.github.leawind.inventory.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import org.junit.jupiter.api.Test;

public class ValidatingHashMapTest {
  static class MyMap<K, V> extends ValidatingHashMap<K, V> {
    @Override
    public V put(K k, V v) {
      Objects.requireNonNull(k);
      return super.put(k, v);
    }
  }

  @Test
  void test() {
    assertThrows(NullPointerException.class, () -> new MyMap<>().put(null, null));
  }
}
