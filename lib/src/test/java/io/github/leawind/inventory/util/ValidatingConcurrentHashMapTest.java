package io.github.leawind.inventory.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/** Tests for {@link ValidatingConcurrentHashMap}. */
public class ValidatingConcurrentHashMapTest {

  /** A concrete implementation that rejects values less than zero. */
  static class PositiveValueMap<K> extends ValidatingConcurrentHashMap<K, Integer> {

    @Override
    public void validateEntry(K key, Integer value) {
      if (value != null && value < 0) {
        throw new IllegalArgumentException("Value must be non-negative: " + value);
      }
    }

    public PositiveValueMap(Map<? extends K, ? extends Integer> m) {
      super(m);
    }

    public PositiveValueMap() {
      super();
    }
  }

  // --- Basic operations ---

  @Test
  void testPutAndGet() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    assertNull(map.put("a", 1));
    assertEquals(1, map.get("a"));
    assertEquals(1, map.put("a", 2)); // returns old value
    assertEquals(2, map.get("a"));
  }

  @Test
  void testPutAll() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 1);
    Map<String, Integer> data = new HashMap<>();
    data.put("b", 2);
    data.put("c", 3);
    map.putAll(data);
    assertEquals(3, map.size());
    assertEquals(1, map.get("a"));
    assertEquals(2, map.get("b"));
    assertEquals(3, map.get("c"));
  }

  @Test
  void testRemove() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 10);
    assertEquals(10, map.remove("a"));
    assertNull(map.get("a"));
  }

  // --- Validation: single entry rejection ---

  @Test
  void testValidationRejectsPut() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    assertThrows(IllegalArgumentException.class, () -> map.put("a", -1));
    assertFalse(map.containsKey("a"));
  }

  @Test
  void testValidationRejectsPutIfAbsent() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    assertThrows(IllegalArgumentException.class, () -> map.putIfAbsent("a", -5));
    assertNull(map.get("a"));
  }

  @Test
  void testValidationRejectsComputeIfAbsent() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    assertThrows(IllegalArgumentException.class, () -> map.computeIfAbsent("a", k -> -10));
    assertFalse(map.containsKey("a"));
  }

  @Test
  void testValidationRejectsCompute() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 5);
    assertThrows(IllegalArgumentException.class, () -> map.compute("a", (k, v) -> -1));
    // The existing entry must remain intact
    assertEquals(5, map.get("a"));
  }

  @Test
  void testValidationRejectsComputeIfPresent() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 5);
    assertThrows(IllegalArgumentException.class, () -> map.computeIfPresent("a", (k, v) -> -1));
    assertEquals(5, map.get("a"));
  }

  // --- Validation: putAll rejects an invalid combined state ---

  @Test
  void testPutAllRejectsInvalidEntry() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 10);
    Map<String, Integer> invalid = new HashMap<>();
    invalid.put("b", -1);
    invalid.put("c", 3);
    assertThrows(IllegalArgumentException.class, () -> map.putAll(invalid));
    // The original map must be untouched (atomic failure)
    assertEquals(1, map.size());
    assertTrue(map.containsKey("a"));
    assertFalse(map.containsKey("b"));
    assertFalse(map.containsKey("c"));
  }

  @Test
  void testPutAllValidatesCombinedMap() {
    // Custom validation: the map must never contain both "a" and "b" together
    ValidatingConcurrentHashMap<String, String> map =
        new ValidatingConcurrentHashMap<String, String>() {
          @Override
          public void validateMap(Map<? extends String, ? extends String> m) {
            if (m.containsKey("a") && m.containsKey("b")) {
              throw new IllegalStateException("Cannot have both a and b");
            }
          }
        };
    map.put("a", "1");
    Map<String, String> toAdd = new HashMap<>();
    toAdd.put("b", "2");
    assertThrows(IllegalStateException.class, () -> map.putAll(toAdd));
    assertEquals(1, map.size());
    assertNull(map.get("b"));
  }

  // --- Edge cases: absent or null mapping results ---

  @Test
  void testPutIfAbsentDoesNotValidateWhenPresent() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 5);
    // Should not throw even though the value is negative, because the key is present
    assertDoesNotThrow(() -> map.putIfAbsent("a", -1));
    assertEquals(5, map.get("a"));
  }

  @Test
  void testComputeIfAbsentDoesNotInvokeMappingWhenPresent() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("a", 5);
    Integer result =
        map.computeIfAbsent(
            "a",
            k -> {
              throw new RuntimeException("should not be called");
            });
    assertEquals(5, result);
  }

  @Test
  void testComputeIfAbsentMappingReturnsNull() {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    Integer result = map.computeIfAbsent("a", k -> null);
    assertNull(result);
    assertFalse(map.containsKey("a"));
  }

  // --- Constructor with map ---

  @Test
  void testConstructorWithInvalidMapThrows() {
    Map<String, Integer> invalid = new HashMap<>();
    invalid.put("x", -5);
    assertThrows(IllegalArgumentException.class, () -> new PositiveValueMap<>(invalid));
  }

  @Test
  void testConstructorWithValidMapSucceeds() {
    Map<String, Integer> valid = new HashMap<>();
    valid.put("x", 10);
    valid.put("y", 20);
    PositiveValueMap<String> map = new PositiveValueMap<>(valid);
    assertEquals(2, map.size());
    assertEquals(10, map.get("x"));
    assertEquals(20, map.get("y"));
  }

  // --- Concurrent safety ---

  @Test
  void testConcurrentPutSafety() throws InterruptedException {
    int threadCount = 10;
    int itemsPerThread = 100;
    PositiveValueMap<Integer> map = new PositiveValueMap<>();
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(
          () -> {
            try {
              for (int i = 0; i < itemsPerThread; i++) {
                map.put(threadId * itemsPerThread + i, i);
              }
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await();
    executor.shutdown();
    assertEquals(threadCount * itemsPerThread, map.size());
    // spot-check a few values
    assertEquals(50, (int) map.get(5 * itemsPerThread + 50));
  }

  @Test
  void testConcurrentPutAllAndPut() throws InterruptedException {
    PositiveValueMap<String> map = new PositiveValueMap<>();
    map.put("init", 1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    executor.submit(
        () -> {
          try {
            Map<String, Integer> data = new HashMap<>();
            data.put("a", 2);
            data.put("b", 3);
            map.putAll(data);
          } finally {
            latch.countDown();
          }
        });
    executor.submit(
        () -> {
          try {
            map.put("c", 4);
          } finally {
            latch.countDown();
          }
        });
    latch.await();
    executor.shutdown();
    // At least the initial entry plus the ones added concurrently must be present
    assertTrue(map.size() >= 3);
    assertEquals(1, (int) map.get("init"));
  }
}
