package io.github.leawind.inventory.misc;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LazyTest {

  @Test
  void testBasicInitialization() {
    AtomicInteger counter = new AtomicInteger(0);
    Lazy<String> lazy =
        new Lazy<>(
            () -> {
              counter.incrementAndGet();
              return "test-value";
            });

    // Initially not initialized
    assertFalse(lazy.isInitialized());
    assertEquals(0, counter.get());

    // First get() should initialize
    String result1 = lazy.get();
    assertTrue(lazy.isInitialized());
    assertEquals("test-value", result1);
    assertEquals(1, counter.get());

    // Second get() should return cached value
    String result2 = lazy.get();
    assertSame(result1, result2);
    assertEquals(1, counter.get()); // Supplier should only be called once
  }

  @Test
  void testReset() {
    AtomicInteger counter = new AtomicInteger(0);
    Lazy<Integer> lazy =
        new Lazy<>(
            () -> {
              counter.incrementAndGet();
              return counter.get() * 10;
            });

    // Initialize
    assertEquals(10, lazy.get());
    assertTrue(lazy.isInitialized());
    assertEquals(1, counter.get());

    // Reset
    lazy.reset();
    assertFalse(lazy.isInitialized());

    // Get again should re-initialize
    assertEquals(20, lazy.get());
    assertTrue(lazy.isInitialized());
    assertEquals(2, counter.get());
  }

  @Test
  void testMultipleResetCycles() {
    AtomicInteger counter = new AtomicInteger(0);
    Lazy<String> lazy = new Lazy<>(() -> "value-" + counter.incrementAndGet());

    // First cycle
    assertEquals("value-1", lazy.get());
    assertTrue(lazy.isInitialized());

    lazy.reset();
    assertFalse(lazy.isInitialized());

    // Second cycle
    assertEquals("value-2", lazy.get());
    assertTrue(lazy.isInitialized());

    lazy.reset();
    assertFalse(lazy.isInitialized());

    // Third cycle
    assertEquals("value-3", lazy.get());
    assertTrue(lazy.isInitialized());

    assertEquals(3, counter.get());
  }

  @Test
  void testNullValue() {
    Lazy<String> lazy = new Lazy<>(() -> null);

    assertFalse(lazy.isInitialized());
    assertNull(lazy.get());
    assertFalse(lazy.isInitialized());

    // Subsequent calls should return same null
    assertNull(lazy.get());
  }

  @Test
  void testComplexObject() {
    Lazy<StringBuilder> lazy =
        new Lazy<>(
            () -> {
              StringBuilder sb = new StringBuilder();
              sb.append("Hello");
              return sb;
            });

    assertFalse(lazy.isInitialized());

    StringBuilder result1 = lazy.get();
    assertTrue(lazy.isInitialized());
    assertEquals("Hello", result1.toString());

    // Modify the returned object
    result1.append(" World");

    // Get again should return the same modified object
    StringBuilder result2 = lazy.get();
    assertSame(result1, result2);
    assertEquals("Hello World", result2.toString());
  }

  @Test
  void testExceptionInSupplier() {
    Lazy<String> lazy =
        new Lazy<>(
            () -> {
              throw new RuntimeException("Supplier error");
            });

    assertFalse(lazy.isInitialized());

    // First call should throw
    try {
      lazy.get();
    } catch (RuntimeException e) {
      assertEquals("Supplier error", e.getMessage());
    }

    // isInitialized should remain false since initialization failed
    assertFalse(lazy.isInitialized());

    // Subsequent calls should retry initialization
    try {
      lazy.get();
    } catch (RuntimeException e) {
      assertEquals("Supplier error", e.getMessage());
    }
    assertFalse(lazy.isInitialized());
  }
}
